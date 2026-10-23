using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Security.Principal;
using System.Text;
using Microsoft.Win32;

namespace DbxInstaller
{
    // Is the profile folder the installer expects for the service account (C:\Users\<name>, see
    // InstallActions.AccountHome) free, or what is in the way? Used by the Check button on the Service
    // Account page (with "Clean up…" and "PowerShell commands"), and by the "Create user profile" step,
    // which stops BEFORE Windows creates the profile somewhere else.
    //
    // Windows names a new profile folder after the account, but when C:\Users\<name> is taken — the
    // profile of a deleted or of a local account with the same name, or just a folder — it silently
    // uses <name>.<DOMAIN> or <name>.000 instead, and the install directories don't match any more.
    internal static class ProfileCheck
    {
        internal enum CleanupKind { None, DeleteProfile, DeleteLocalAccount, DeleteFolder }

        // One thing in the way. Text: what it is and what it causes. Blocks: the profile would not end up
        // in the expected folder. Kind/What: what "Clean up…" deletes (None = only the administrator can
        // sort it out), and the line the confirmation shows for it. Sid/Name/Path: what Cleanup works on.
        internal sealed record Finding(string Text, bool Blocks, CleanupKind Kind, string What,
                                       string Sid = "", string Name = "", string Path = "");

        // What Analyze needs from the system — ReadSnapshot fills it from Windows, tests make it up.
        internal sealed record Snapshot(
            string? AccountSid,                        // null: the account doesn't exist (yet)
            Dictionary<string, string> Profiles,       // ProfileList: SID -> profile folder
            HashSet<string> LoadedSids,                // profiles in use (hive loaded under HKEY_USERS)
            string? LocalDomainSid,                    // this computer's account domain: local SIDs are <this>-<RID>
            string? LocalTwinSid,                      // a LOCAL account with the same name, when a domain account was entered
            Func<string, bool> FolderExists,
            Func<string, string?> AccountOfSid,        // SID -> "DOMAIN\name"; null = no such account any more
            Func<string, string> FolderInfo);          // folder -> "3.1 MB in 120 files — contains DbxTune data: .dbxtune"

        // "CORP\user" and "user@corp" are domain accounts; "user", ".\user" and "THISPC\user" are local.
        public static bool IsDomainName(string account)
        {
            if (account.Contains('@')) return true;
            if (!account.Contains('\\')) return false;
            string prefix = account.Split('\\', 2)[0];
            return prefix != "." && !prefix.Equals(Environment.MachineName, StringComparison.OrdinalIgnoreCase);
        }

        public static List<Finding> Analyze(string account, string expected, Snapshot s)
        {
            var findings = new List<Finding>();
            // A domain account that can't be found: the Check already says so; C:\Users\<typo> means nothing.
            if (s.AccountSid == null && IsDomainName(account)) return findings;

            static string Norm(string p) => p.TrimEnd('\\');
            bool Same(string a, string b) => string.Equals(Norm(a), Norm(b), StringComparison.OrdinalIgnoreCase);
            bool IsLocalSid(string sid) => s.LocalDomainSid != null && sid.StartsWith(s.LocalDomainSid + "-", StringComparison.OrdinalIgnoreCase);
            string elsewhere = $"Windows would create the new profile in another folder (like {expected}.000), and the install stops there.";

            // 1. This account's own profile: in another folder, or registered without its folder.
            string? own = null;
            if (s.AccountSid != null && s.Profiles.TryGetValue(s.AccountSid, out own))
            {
                bool inUse = s.LoadedSids.Contains(s.AccountSid);
                string busy = inUse ? " It is in use (the account is logged on, or a service runs as it): log it off / stop that service, then press Check again." : "";
                if (!Same(own, expected))
                    findings.Add(new Finding(
                        $"'{account}' already has a profile, but in another folder: {own} (expected: {expected}).{busy}",
                        true, inUse ? CleanupKind.None : CleanupKind.DeleteProfile,
                        $"The profile of '{account}': {own}  ({s.FolderInfo(own)})", s.AccountSid, account, own));
                else if (!s.FolderExists(Norm(own)))
                    findings.Add(new Finding(
                        $"'{account}' has a profile registered at {own}, but the folder is missing — Windows can't use it.{busy}",
                        true, inUse ? CleanupKind.None : CleanupKind.DeleteProfile,
                        $"The profile registration of '{account}' (its folder {own} is missing)", s.AccountSid, account, own));
            }

            // 2. The expected folder: someone else's profile, or a folder that is nobody's profile.
            var owner = s.Profiles.FirstOrDefault(p => p.Key != s.AccountSid && Same(p.Value, expected));
            if (owner.Key != null)
            {
                string sid   = owner.Key;
                string? name = s.AccountOfSid(sid);
                string info  = s.FolderInfo(expected);
                if (s.LoadedSids.Contains(sid))
                    findings.Add(new Finding(
                        $"{expected} is the profile of '{name ?? sid}', and it is in use — the installer can't use or remove it.",
                        true, CleanupKind.None, ""));
                else if (sid == s.LocalTwinSid)
                    findings.Add(new Finding(
                        $"{expected} is the profile of the LOCAL account '{name}' (left over from an install with a local account?). {elsewhere}",
                        true, CleanupKind.DeleteLocalAccount,
                        $"The local account '{name}' and its profile {expected}  ({info})", sid, Path.GetFileName(expected), expected));
                else if (name == null && IsLocalSid(sid))
                    findings.Add(new Finding(
                        $"{expected} is the left-over profile of a LOCAL account that no longer exists ({sid}). {elsewhere}",
                        true, CleanupKind.DeleteProfile,
                        $"The left-over profile {expected} of a deleted local account  ({info})", sid, "", expected));
                else if (name == null)
                    findings.Add(new Finding(
                        $"{expected} is the profile of an account that can't be looked up ({sid}): it was deleted, or its domain " +
                        "can't be reached. The installer doesn't remove it — see the PowerShell commands.",
                        true, CleanupKind.None, ""));
                else
                    findings.Add(new Finding(
                        $"{expected} is the profile of another account, '{name}'. The installer doesn't touch it: use another " +
                        "account name, or ask the administrator.",
                        true, CleanupKind.None, ""));
            }
            else if (s.FolderExists(expected) && !(own != null && Same(own, expected)))
                findings.Add(new Finding(
                    $"The folder {expected} exists, but it isn't the profile of any account (left over from an earlier install?). {elsewhere}",
                    true, CleanupKind.DeleteFolder, $"The folder {expected}  ({s.FolderInfo(expected)})", "", "", expected));

            // 3. A local account with the same name as the domain account (not in the way, just left over).
            if (s.LocalTwinSid != null && owner.Key != s.LocalTwinSid)
            {
                string name   = Path.GetFileName(expected);
                bool   inUse  = s.LoadedSids.Contains(s.LocalTwinSid);
                bool   hasDir = s.Profiles.TryGetValue(s.LocalTwinSid, out var twinPath);
                findings.Add(new Finding(
                    $"There is also a LOCAL account '{name}' on this computer. This install doesn't use it — it can be removed." +
                    (inUse ? " (It is in use right now.)" : ""),
                    false, inUse ? CleanupKind.None : CleanupKind.DeleteLocalAccount,
                    hasDir ? $"The local account '{name}' and its profile {twinPath}  ({s.FolderInfo(twinPath!)})"
                           : $"The local account '{name}' (it has no profile)",
                    s.LocalTwinSid, name, hasDir ? twinPath! : ""));
            }
            return findings;
        }

        // The Windows side of Analyze. Reads only; works without admin rights (folder sizes may then be partial).
        public static Snapshot ReadSnapshot(string account, string expected)
        {
            static string? SidOf(string name)
            {
                try { return LsaPrivileges.GetSidString(name); } catch { return null; }
            }

            var profiles = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
            using (var list = Registry.LocalMachine.OpenSubKey(@"SOFTWARE\Microsoft\Windows NT\CurrentVersion\ProfileList"))
                foreach (string sid in list?.GetSubKeyNames() ?? Array.Empty<string>())
                {
                    if (sid.EndsWith(".bak", StringComparison.OrdinalIgnoreCase)) continue;   // Windows' backup of a broken profile
                    using var key = list!.OpenSubKey(sid);
                    if (key?.GetValue("ProfileImagePath") is string path && path.Length > 0)
                        profiles[sid] = Environment.ExpandEnvironmentVariables(path).TrimEnd('\\');
                }

            string? localDomain = LsaPrivileges.GetLocalAccountDomainSid();
            string? twin = null;
            if (IsDomainName(account) && localDomain != null)
            {
                string? sid = SidOf($"{Environment.MachineName}\\{Path.GetFileName(expected)}");
                if (sid != null && sid.StartsWith(localDomain + "-", StringComparison.OrdinalIgnoreCase)) twin = sid;
            }

            return new Snapshot(
                SidOf(account), profiles,
                new HashSet<string>(Registry.Users.GetSubKeyNames(), StringComparer.OrdinalIgnoreCase),
                localDomain, twin,
                Directory.Exists,
                sid =>
                {
                    try { return new SecurityIdentifier(sid).Translate(typeof(NTAccount)).Value; }
                    catch (IdentityNotMappedException) { return null; }               // no such account (deleted)
                    catch (Exception) { return $"(account {sid}, can't be looked up now)"; }   // e.g. no DC: not "deleted"
                },
                FolderInfo);
        }

        // Size and file count (at most ~3 seconds of counting), and whether it holds DbxTune data.
        private static string FolderInfo(string path)
        {
            if (!Directory.Exists(path)) return "the folder is missing";
            long bytes = 0;
            int  files = 0;
            bool complete = true;
            var  watch = Stopwatch.StartNew();
            try
            {
                var options = new EnumerationOptions
                {
                    RecurseSubdirectories = true,
                    IgnoreInaccessible    = true,
                    AttributesToSkip      = FileAttributes.ReparsePoint,   // profile junctions, the '0' version junction
                };
                foreach (var file in new DirectoryInfo(path).EnumerateFiles("*", options))
                {
                    bytes += file.Length;
                    if (++files % 500 == 0 && watch.ElapsedMilliseconds > 3000) { complete = false; break; }
                }
            }
            catch { complete = false; }

            string size = bytes >= 1L << 30 ? $"{bytes / (double)(1L << 30):0.0} GB"
                        : bytes >= 1L << 20 ? $"{bytes / (double)(1L << 20):0.0} MB"
                        : $"{(bytes + 1023) / 1024} KB";
            var dbx = new[] { ".dbxtune", "dbxtune_sw" }.Where(d => Directory.Exists(Path.Combine(path, d))).ToList();
            return $"{(complete ? "" : "at least ")}{size} in {files} files" +
                   (dbx.Count > 0 ? $" — contains DbxTune data: {string.Join(", ", dbx)}" : "");
        }

        // Deletes what the findings' Kind says: profiles first (DeleteProfile needs only the SID), then
        // local accounts (their user rights and profile first), then plain folders. Every item is tried;
        // returns false when any of them failed (the log says which).
        public static bool Cleanup(IEnumerable<Finding> findings, InstallLog log)
        {
            var list = findings.ToList();
            bool ok = true;
            void Try(string what, Action action)
            {
                try { action(); }
                catch (Exception ex) { ok = false; log($"FAILED: {what}: {ex.Message}"); }
            }

            foreach (var f in list.Where(f => f.Kind == CleanupKind.DeleteProfile))
                Try($"delete the profile {f.Path}", () =>
                {
                    LsaPrivileges.DeleteUserProfile(f.Sid);
                    log($"Deleted the profile {f.Path}.");
                    RemoveEmptyParent(f.Path, log);
                });

            foreach (var f in list.Where(f => f.Kind == CleanupKind.DeleteLocalAccount))
                Try($"delete the local account '{f.Name}'", () =>
                {
                    string qualified = $"{Environment.MachineName}\\{f.Name}";
                    UninstallActions.RemoveUserRights(qualified, msg => log(msg.Trim()));   // no orphaned SIDs left in the user rights
                    if (f.Path.Length > 0)
                    {
                        LsaPrivileges.DeleteUserProfile(f.Sid);
                        log($"Deleted the profile {f.Path}.");
                    }
                    var (done, output) = InstallActions.Run("net", $"user \"{f.Name}\" /delete");
                    if (!done) throw new Exception($"net user \"{f.Name}\" /delete: {output.Trim()}");
                    log($"Deleted the local account '{f.Name}'.");
                });

            foreach (var f in list.Where(f => f.Kind == CleanupKind.DeleteFolder))
                Try($"delete the folder {f.Path}", () =>
                {
                    if (!UninstallActions.TryRemoveDir(f.Path, "left-over", msg => log(msg.Trim())))
                        throw new Exception("some files could not be removed (see above)");
                });
            return ok;
        }

        // C:\Users\DCSEQ\name (the old CreateProfile bug) leaves C:\Users\DCSEQ behind, empty.
        private static void RemoveEmptyParent(string profilePath, InstallLog log)
        {
            string? parent = Path.GetDirectoryName(profilePath.TrimEnd('\\'));
            string  root   = Path.GetDirectoryName(InstallActions.AccountHome("x")) ?? @"C:\Users";
            if (parent == null || Path.GetDirectoryName(parent)?.Equals(root, StringComparison.OrdinalIgnoreCase) != true) return;
            try
            {
                if (Directory.Exists(parent) && !Directory.EnumerateFileSystemEntries(parent).Any())
                {
                    Directory.Delete(parent);
                    log($"Deleted the empty folder {parent}.");
                }
            }
            catch { /* not important */ }
        }

        // Commands for the administrator, filled in with the account's name and what Check found.
        public static string PowerShellCommands(string account, string expected, List<Finding> findings)
        {
            string name = Path.GetFileName(expected);
            var sb = new StringBuilder();
            sb.AppendLine("# Run in PowerShell as administrator.");
            sb.AppendLine($"# 1. Profiles whose folder starts with '{name}': which account, in use (Loaded)?");
            sb.AppendLine($"Get-CimInstance Win32_UserProfile | Where-Object LocalPath -like '{expected}*' |");
            sb.AppendLine("    Select-Object LocalPath, SID, Loaded, LastUseTime,");
            sb.AppendLine("    @{n='Account';e={try{([Security.Principal.SecurityIdentifier]$_.SID).Translate([Security.Principal.NTAccount]).Value}catch{'(deleted account)'}}}");
            sb.AppendLine();
            sb.AppendLine($"# 2. A LOCAL account named '{name}'? And the local groups it is in");
            sb.AppendLine($"Get-LocalUser -Name '{name}' -ErrorAction SilentlyContinue | Select-Object Name, SID, Enabled, LastLogon");
            sb.AppendLine($"Get-LocalGroup | Where-Object {{ Get-LocalGroupMember $_ -ErrorAction SilentlyContinue | Where-Object Name -like '*\\{name}' }} | Select-Object Name");
            sb.AppendLine();
            sb.AppendLine($"# 3. User rights granted to '{account}' in the local policy");
            sb.AppendLine("secedit /export /areas USER_RIGHTS /cfg $env:TEMP\\rights.inf | Out-Null");
            sb.AppendLine($"$sid = ([Security.Principal.NTAccount]'{account}').Translate([Security.Principal.SecurityIdentifier]).Value");
            sb.AppendLine("Select-String -Path $env:TEMP\\rights.inf -Pattern $sid | ForEach-Object { $_.Line.Split('=')[0].Trim() }");
            sb.AppendLine();
            sb.AppendLine("# 4. Delete ONE profile (its folder + registry entry) — only when Loaded is False");
            var profilePaths = findings.Where(f => (f.Kind is CleanupKind.DeleteProfile or CleanupKind.DeleteLocalAccount) && f.Path.Length > 0)
                                       .Select(f => f.Path).Distinct(StringComparer.OrdinalIgnoreCase).ToList();
            foreach (var p in profilePaths.DefaultIfEmpty(expected))
                sb.AppendLine($"Get-CimInstance Win32_UserProfile | Where-Object LocalPath -eq '{p}' | Remove-CimInstance");
            sb.AppendLine();
            sb.AppendLine($"# 5. Delete the LOCAL account '{name}' (after its profile). Never do this for a domain account.");
            sb.AppendLine($"Remove-LocalUser -Name '{name}'");
            sb.AppendLine();
            sb.AppendLine("# 6. A folder that is no profile at all: look at it first, then delete it");
            sb.AppendLine($"Get-ChildItem '{expected}' -Force | Select-Object Name, LastWriteTime");
            sb.AppendLine($"Remove-Item '{expected}' -Recurse -Force");
            return sb.ToString().TrimEnd();
        }
    }
}

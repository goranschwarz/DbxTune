using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using Microsoft.Win32;

namespace DbxInstaller
{
    internal static class UninstallActions
    {
        // ── Removal steps (used by the Uninstalling/Progress wizard page) ─────

        public static void KillClient(InstallLog log)
        {
            var procs = Process.GetProcessesByName("DbxStarterClient");
            if (procs.Length == 0) { log("  DbxStarterClient is not running."); return; }

            log($"  DbxStarterClient is running ({procs.Length} process(es)) — closing…");
            foreach (var p in procs)
            {
                try { p.Kill(); p.WaitForExit(3000); log($"    PID {p.Id} terminated."); }
                catch (Exception ex) { log($"    Warning: could not kill PID {p.Id}: {ex.Message}"); }
                finally { p.Dispose(); }
            }
        }

        public static void StopService(InstallLog log)
        {
            const string svcName = "DbxStarterService";
            var (exists, queryOut) = InstallActions.Run("sc", $"query {svcName}");
            if (!exists) { log($"  {svcName} is not installed — nothing to stop."); return; }
            if (!queryOut.Contains("RUNNING", StringComparison.OrdinalIgnoreCase))
            {
                log($"  {svcName} is installed but not running — no action needed.");
                return;
            }

            log($"  {svcName} is running — stopping…");
            var (_, stopOut) = InstallActions.Run("sc", $"stop {svcName}");
            if (!string.IsNullOrWhiteSpace(stopOut)) log($"  {stopOut}");

            // The service can take up to ~180s to gracefully stop every monitored DBMS server
            // process (each gets an HTTP shutdown request and is waited on individually — see
            // DbxStarterService.StopAsync). A short fixed sleep here used to let directory
            // removal start while a monitored process was still exiting, leaving its log file
            // locked and that one versioned directory behind forever — this waits for real.
            bool stopped = InstallActions.WaitForServiceStopped(svcName, InstallActions.ServiceStopTimeoutMs, log);
            if (!stopped)
                throw new Exception($"{svcName} did not stop within {InstallActions.ServiceStopTimeoutMs / 1000}s — " +
                    "it (or a monitored server process) may still be running and holding files open. " +
                    "Close it manually and re-run removal, or the software directory may fail to delete.");

            log($"  {svcName} stopped — OK.");
        }

        // Deletes the local account and its profile directory. Only called for local
        // accounts (domain/gMSA accounts are left alone — they're managed by AD).
        // Returns false if the profile directory could not be fully removed.
        public static bool RemoveUserAccount(string account, string? profileDir, InstallLog log)
        {
            log($"  Deleting user account '{account}'…");
            var (ok, output) = InstallActions.Run("net", $"user \"{account}\" /delete");
            log(ok ? "  User account deleted." : $"  Warning: {output.Trim()}");

            if (profileDir == null) return true;
            System.Threading.Thread.Sleep(800);

            if (!Directory.Exists(profileDir))
            {
                log($"  Profile directory cleaned up automatically: {profileDir}");
                return true;
            }

            log($"  Profile directory still present: {profileDir}");
            log("  Attempting forced removal…");
            return TryRemoveDir(profileDir, "profile", log);
        }

        // Attempts to delete a directory tree: single-shot delete, then falls back to
        // deleting children item-by-item (some may be locked, others won't be), then a
        // final delete attempt. On persistent failure, reports which process(es) are
        // holding it open, via a 3-pass lock scan (exe path, Restart Manager, handle table).
        // Returns false if the directory still exists afterwards.
        public static bool TryRemoveDir(string dir, string label, InstallLog log)
        {
            if (!Directory.Exists(dir)) { log($"  Already removed: {dir}"); return true; }

            log($"  Removing {label}: {dir}");
            try
            {
                Directory.Delete(dir, recursive: true);
                log($"  Removed: {dir}");
                return true;
            }
            catch (Exception ex)
            {
                log($"  Could not remove in one shot: {ex.Message}");
                log("  Trying item by item…");
            }

            try { foreach (var f in Directory.EnumerateFiles(dir)) try { File.Delete(f); } catch { } } catch { }
            try
            {
                foreach (var sub in Directory.EnumerateDirectories(dir))
                {
                    try { Directory.Delete(sub, recursive: true); log($"    Removed: {sub}"); }
                    catch (Exception ex) { log($"    Could not remove {Path.GetFileName(sub)}: {ex.Message}"); }
                }
            }
            catch { }
            try { Directory.Delete(dir, recursive: true); } catch { }

            if (Directory.Exists(dir))
            {
                log($"  Still exists — manual cleanup required: {dir}");
                var (lockers, diag) = FindLockingProcesses(dir);
                if (lockers.Count > 0)
                {
                    log("  Processes holding this directory open:");
                    foreach (var (pid, name) in lockers) log($"    PID {pid,-6}  {name}");
                    log("  Close those processes and run uninstall again.");
                }
                else
                {
                    log($"  ({diag})");
                    log("  (A reboot will release any remaining handles.)");
                }
                return false;
            }

            log($"  Removed: {dir}");
            return true;
        }

        // Reports which running processes are holding a directory open, via three passes:
        //  1. Executable path — catches processes whose exe lives inside the directory
        //     (via QueryFullProcessImageName, needs only PROCESS_QUERY_LIMITED_INFORMATION).
        //  2. Windows Restart Manager — catches open file handles under the directory.
        //  3. Full handle-table enumeration (NtQuerySystemInformation) — catches processes
        //     holding the directory object itself open (e.g. as their working directory).
        // Returns the locking processes plus a short diagnostic string (non-empty only when
        // the list came back empty, to explain why nothing was found).
        private static (List<(int Pid, string Name)> Lockers, string Diag) FindLockingProcesses(string dir)
        {
            var seen = new HashSet<int>();
            var results = new List<(int, string)>();
            void Add(int pid)
            {
                if (!seen.Add(pid)) return;
                string name = "";
                try { name = Process.GetProcessById(pid).ProcessName; } catch { }
                results.Add((pid, name));
            }

            // SeDebugPrivilege lets us open processes belonging to other user accounts.
            IDisposable? dbgPriv = null;
            try { dbgPriv = LsaPrivileges.EnablePrivileges(LsaPrivileges.SeDebugPrivilege); }
            catch { /* not fatal — best-effort */ }

            try
            {
                string dirSep = dir.TrimEnd(Path.DirectorySeparatorChar) + Path.DirectorySeparatorChar;

                // Pass 1 — executable path.
                foreach (var proc in Process.GetProcesses())
                {
                    try
                    {
                        IntPtr h = NativeMethods.OpenProcess(0x1000 /*PROCESS_QUERY_LIMITED_INFORMATION*/, false, proc.Id);
                        if (h != IntPtr.Zero)
                        {
                            try
                            {
                                var sb = new StringBuilder(1024);
                                uint sz = (uint)sb.Capacity;
                                if (NativeMethods.QueryFullProcessImageName(h, 0, sb, ref sz))
                                {
                                    string exePath = sb.ToString(0, (int)sz);
                                    if (exePath.StartsWith(dirSep, StringComparison.OrdinalIgnoreCase)) Add(proc.Id);
                                }
                            }
                            finally { NativeMethods.CloseHandle(h); }
                        }
                    }
                    catch { }
                    finally { proc.Dispose(); }
                }

                // Pass 2 — Restart Manager.
                string[] files;
                try { files = Directory.GetFiles(dir, "*", SearchOption.AllDirectories); }
                catch { files = Array.Empty<string>(); }

                if (files.Length > 0)
                {
                    string key = Guid.NewGuid().ToString("N")[..32];
                    if (NativeMethods.RmStartSession(out uint session, 0, key) == 0)
                    {
                        try
                        {
                            const int batch = 64;
                            for (int i = 0; i < files.Length; i += batch)
                            {
                                var slice = files.Skip(i).Take(batch).ToArray();
                                NativeMethods.RmRegisterResources(session, (uint)slice.Length, slice, 0, null, 0, null);
                            }
                            uint needed = 0, count = 0, reboot = 0;
                            NativeMethods.RmGetList(session, out needed, ref count, null, ref reboot);
                            if (needed > 0)
                            {
                                count = needed;
                                var infos = new NativeMethods.RM_PROCESS_INFO[count];
                                if (NativeMethods.RmGetList(session, out needed, ref count, infos, ref reboot) == 0)
                                    for (int i = 0; i < (int)count; i++) Add(infos[i].Process.dwProcessId);
                            }
                        }
                        finally { NativeMethods.RmEndSession(session); }
                    }
                }

                // Pass 3 — full handle-table enumeration.
                int handlesScanned = 0, openFailed = 0;
                HandleEnumPids(dir, Add, ref handlesScanned, ref openFailed);

                string diag = results.Count > 0 ? "" :
                    $"Scanned {handlesScanned:N0} handles; {openFailed:N0} processes were inaccessible.";
                return (results, diag);
            }
            finally { dbgPriv?.Dispose(); }
        }

        // Enumerate every kernel handle in the system (SystemExtendedHandleInformation) and match
        // by duplicating each candidate handle into our process and querying its object name.
        private static void HandleEnumPids(string dir, Action<int> add, ref int handlesScanned, ref int openFailed)
        {
            string? ntDir = DosToNtPath(dir);
            if (ntDir == null) return;
            string ntPrefix = ntDir.TrimEnd('\\') + "\\";

            const int SystemExtendedHandleInformation = 0x40;
            int bufLen = 0x100000;
            IntPtr buf = IntPtr.Zero;
            try
            {
                while (true)
                {
                    buf = Marshal.AllocHGlobal(bufLen);
                    int st = NativeMethods.NtQuerySystemInformation(SystemExtendedHandleInformation, buf, bufLen, out int needed);
                    if (st == 0) break;
                    Marshal.FreeHGlobal(buf); buf = IntPtr.Zero;
                    if (st == unchecked((int)0xC0000004)) { bufLen = needed + 0x10000; continue; } // STATUS_INFO_LENGTH_MISMATCH
                    return;
                }

                long count   = Marshal.ReadIntPtr(buf).ToInt64();
                IntPtr start = buf + IntPtr.Size * 2; // skip NumberOfHandles + Reserved
                int eSize    = Marshal.SizeOf<NativeMethods.SYSTEM_HANDLE_TABLE_ENTRY_INFO_EX>();
                int ourPid   = Process.GetCurrentProcess().Id;
                IntPtr ourProc = NativeMethods.GetCurrentProcess();
                var matchedPids = new HashSet<int>();
                var deadline = DateTime.UtcNow.AddSeconds(8);

                for (long i = 0; i < count; i++)
                {
                    if (DateTime.UtcNow > deadline) break;

                    var e = Marshal.PtrToStructure<NativeMethods.SYSTEM_HANDLE_TABLE_ENTRY_INFO_EX>(start + (nint)(i * eSize));
                    int pid = (int)e.UniqueProcessId;
                    if (pid == ourPid || matchedPids.Contains(pid)) continue;

                    handlesScanned++;
                    IntPtr srcProc = NativeMethods.OpenProcess(0x0040 /*PROCESS_DUP_HANDLE*/, false, pid);
                    if (srcProc == IntPtr.Zero) { openFailed++; continue; }

                    IntPtr dup = IntPtr.Zero;
                    try
                    {
                        if (!NativeMethods.DuplicateHandle(srcProc, e.HandleValue, ourProc, out dup, 0, false, 2 /*DUPLICATE_SAME_ACCESS*/))
                            continue;

                        string? name = QueryHandleNameSafe(dup, 100);
                        if (name == null) continue;

                        if (name.Equals(ntDir, StringComparison.OrdinalIgnoreCase) ||
                            name.StartsWith(ntPrefix, StringComparison.OrdinalIgnoreCase))
                        {
                            matchedPids.Add(pid);
                            add(pid);
                        }
                    }
                    finally
                    {
                        if (dup != IntPtr.Zero) NativeMethods.CloseHandle(dup);
                        NativeMethods.CloseHandle(srcProc);
                    }
                }
            }
            finally { if (buf != IntPtr.Zero) Marshal.FreeHGlobal(buf); }
        }

        // Translate a DOS path (C:\foo\bar) to an NT device path (\Device\HarddiskVolume3\foo\bar).
        private static string? DosToNtPath(string dosPath)
        {
            if (dosPath.Length < 2 || dosPath[1] != ':') return null;
            string drive = dosPath[..2];
            string rest  = dosPath[2..];
            var sb = new StringBuilder(1024);
            if (NativeMethods.QueryDosDevice(drive, sb, sb.Capacity) == 0) return null;
            string device = sb.ToString();
            int nullIdx = device.IndexOf('\0');
            if (nullIdx >= 0) device = device[..nullIdx];
            return device + rest;
        }

        // Calls NtQueryObject(ObjectNameInformation) from a background thread and gives up if it
        // doesn't complete within timeoutMs — querying some handle types can hang indefinitely.
        private static string? QueryHandleNameSafe(IntPtr handle, int timeoutMs)
        {
            string? result = null;
            var t = new System.Threading.Thread(() =>
            {
                const int ObjectNameInformation = 1;
                int size = 512;
                IntPtr buf = Marshal.AllocHGlobal(size);
                try
                {
                    int st = NativeMethods.NtQueryObject(handle, ObjectNameInformation, buf, size, out int needed);
                    if (st == unchecked((int)0xC0000023)) // STATUS_BUFFER_TOO_SMALL
                    {
                        Marshal.FreeHGlobal(buf);
                        size = needed;
                        buf = Marshal.AllocHGlobal(size);
                        st = NativeMethods.NtQueryObject(handle, ObjectNameInformation, buf, size, out _);
                    }
                    if (st != 0) return;
                    ushort len = (ushort)Marshal.ReadInt16(buf);
                    IntPtr strPtr = Marshal.ReadIntPtr(buf, IntPtr.Size == 8 ? 8 : 4);
                    if (len > 0 && strPtr != IntPtr.Zero) result = Marshal.PtrToStringUni(strPtr, len / 2);
                }
                catch { }
                finally { Marshal.FreeHGlobal(buf); }
            }) { IsBackground = true };
            t.Start();
            t.Join(timeoutMs);
            return result;
        }

        // Checks that each removed component is actually gone; returns any that remain.
        public static List<string> Verify(string? account, string? profileDir, string? installDir,
            IEnumerable<string> dataDirs, bool checkedAccount, bool checkedData, bool checkedSw)
        {
            var remaining = new List<string>();

            var (svcExists, _) = InstallActions.Run("sc", "query DbxStarterService");
            if (svcExists) remaining.Add("Service registration: DbxStarterService");

            if (checkedAccount && account != null)
            {
                var (acctExists, _) = InstallActions.Run("net", $"user \"{account}\"");
                if (acctExists) remaining.Add($"User account: {account}");
                if (!string.IsNullOrEmpty(profileDir) && Directory.Exists(profileDir))
                    remaining.Add($"Profile directory: {profileDir}");
            }

            if (checkedData)
                foreach (var d in dataDirs)
                    if (Directory.Exists(d)) remaining.Add($"Data directory: {d}");

            if (checkedSw && !string.IsNullOrEmpty(installDir) && Directory.Exists(installDir))
                remaining.Add($"Software directory: {installDir}");

            return remaining;
        }

        // ── P/Invoke: Restart Manager + NT handle enumeration ─────────────────

        private static class NativeMethods
        {
            [StructLayout(LayoutKind.Sequential)]
            public struct RM_UNIQUE_PROCESS
            {
                public int dwProcessId;
                public System.Runtime.InteropServices.ComTypes.FILETIME ProcessStartTime;
            }

            [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
            public struct RM_PROCESS_INFO
            {
                public RM_UNIQUE_PROCESS Process;
                [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 256)] public string strAppName;
                [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 64)]  public string strServiceShortName;
                public int  ApplicationType;
                public uint AppStatus;
                public uint TSSessionId;
                [MarshalAs(UnmanagedType.Bool)] public bool bRestartable;
            }

            [DllImport("rstrtmgr.dll", CharSet = CharSet.Unicode)]
            public static extern int RmStartSession(out uint pSessionHandle, int dwSessionFlags, string strSessionKey);

            [DllImport("rstrtmgr.dll")]
            public static extern int RmEndSession(uint pSessionHandle);

            [DllImport("rstrtmgr.dll", CharSet = CharSet.Unicode)]
            public static extern int RmRegisterResources(uint pSessionHandle,
                uint nFiles, string[] rgsFilenames,
                uint nApplications, RM_UNIQUE_PROCESS[]? rgApplications,
                uint nServices, string[]? rgsServiceNames);

            [DllImport("rstrtmgr.dll")]
            public static extern int RmGetList(uint dwSessionHandle,
                out uint pnProcInfoNeeded, ref uint pnProcInfo,
                [In, Out] RM_PROCESS_INFO[]? rgAffectedApps, ref uint lpdwRebootReasons);

            // SystemExtendedHandleInformation (class 0x40) entry — 64-bit layout
            [StructLayout(LayoutKind.Sequential)]
            public struct SYSTEM_HANDLE_TABLE_ENTRY_INFO_EX
            {
                public IntPtr Object;
                public IntPtr UniqueProcessId;
                public IntPtr HandleValue;
                public uint   GrantedAccess;
                public ushort CreatorBackTraceIndex;
                public ushort ObjectTypeIndex;
                public uint   HandleAttributes;
                public uint   Reserved;
            }

            [DllImport("ntdll.dll")]
            public static extern int NtQuerySystemInformation(int systemInformationClass,
                IntPtr systemInformation, int systemInformationLength, out int returnLength);

            [DllImport("ntdll.dll")]
            public static extern int NtQueryObject(IntPtr handle, int objectInformationClass,
                IntPtr objectInformation, int objectInformationLength, out int returnLength);

            [DllImport("kernel32.dll", SetLastError = true)]
            public static extern bool DuplicateHandle(IntPtr hSourceProcess, IntPtr hSourceHandle,
                IntPtr hTargetProcess, out IntPtr lpTargetHandle, uint dwDesiredAccess, bool bInheritHandle, uint dwOptions);

            [DllImport("kernel32.dll", SetLastError = true)]
            public static extern IntPtr OpenProcess(uint dwDesiredAccess, bool bInheritHandle, int dwProcessId);

            [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
            public static extern bool QueryFullProcessImageName(IntPtr hProcess, uint dwFlags, StringBuilder lpExeName, ref uint lpdwSize);

            [DllImport("kernel32.dll")]
            public static extern bool CloseHandle(IntPtr hObject);

            [DllImport("kernel32.dll")]
            public static extern IntPtr GetCurrentProcess();

            [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
            public static extern uint QueryDosDevice(string lpDeviceName, StringBuilder lpTargetPath, int ucchMax);
        }

        // ── Discovery helpers ──────────────────────────────────────────────────

        // Parse SERVICE_START_NAME from "sc qc" output to discover the service account.
        internal static string? GetServiceAccount()
        {
            var (ok, output) = InstallActions.Run("sc", "qc DbxStarterService");
            if (!ok || string.IsNullOrEmpty(output)) return null;

            foreach (var line in output.Split('\n'))
            {
                int idx = line.IndexOf("SERVICE_START_NAME", StringComparison.OrdinalIgnoreCase);
                if (idx < 0) continue;
                int colon = line.IndexOf(':', idx);
                if (colon < 0) continue;
                string account = line[(colon + 1)..].Trim();
                // Local accounts appear as ".\username" — strip the ".\"
                if (account.StartsWith(@".\")) account = account[2..];
                return account;
            }
            return null;
        }

        // Look up the profile directory from the registry ProfileList (authoritative)
        // and fall back to deriving it from the account name.
        internal static string? GetProfileDir(string account)
        {
            // Registry approach — works for both local and domain accounts
            try
            {
                string sid = LsaPrivileges.GetSidString(account);
                using var key = Registry.LocalMachine.OpenSubKey(
                    $@"SOFTWARE\Microsoft\Windows NT\CurrentVersion\ProfileList\{sid}");
                if (key?.GetValue("ProfileImagePath") is string path)
                {
                    string expanded = Environment.ExpandEnvironmentVariables(path);
                    if (Directory.Exists(expanded)) return expanded;
                }
            }
            catch { /* fall through to name-based guess */ }

            // Name-based fallback (covers the case where the account has been deleted
            // but the profile directory still exists)
            string name = account.Contains('\\') ? account.Split('\\')[1]
                        : account.Contains('@')   ? account.Split('@')[0]
                        : account.TrimEnd('$');
            string derived = Path.Combine(@"C:\Users", name);
            return Directory.Exists(derived) ? derived : null;
        }

        // Build a de-duplicated list of DbxTune data directories to offer for removal.
        // Sub-directories of already-listed parents are omitted (they go with the parent).
        internal static List<string> BuildDataDirList(Dictionary<string, string> envVars)
        {
            var candidates = new[]
            {
                envVars.GetValueOrDefault("DBXTUNE_USER_HOME"),
                envVars.GetValueOrDefault("DBXTUNE_CENTRAL_SAVE_DIR"),
                envVars.GetValueOrDefault("DBXTUNE_CENTRAL_LOG_DIR"),
                envVars.GetValueOrDefault("DBXTUNE_CENTRAL_CONF_DIR"),
                envVars.GetValueOrDefault("DBXTUNE_CENTRAL_INFO_DIR"),
                envVars.GetValueOrDefault("DBXTUNE_CENTRAL_REPORTS_DIR"),
            };

            var result = new List<string>();
            foreach (var d in candidates)
            {
                if (string.IsNullOrWhiteSpace(d) || !Directory.Exists(d)) continue;
                // Skip if a parent is already in the list (will be deleted with it)
                if (result.Any(existing => d.StartsWith(
                        existing + Path.DirectorySeparatorChar,
                        StringComparison.OrdinalIgnoreCase)))
                    continue;
                result.Add(d);
            }
            return result;
        }
    }
}

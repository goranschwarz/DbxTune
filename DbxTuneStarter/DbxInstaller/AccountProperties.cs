using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;

namespace DbxInstaller
{
    // Reads the settings of the service account that would make it fail later — WITHOUT logging on as it
    // (so nothing counts toward a lockout): disabled, locked out, expired, password expired / must change,
    // password can expire, a "Log On To" list without this computer, member of "Protected Users".
    // Uses NetUserGetInfo / NetUserGetGroups — for a domain account against a domain controller of its
    // domain, for a local account on this computer. A gMSA is not a user object: nothing to read.
    internal static class AccountProperties
    {
        // Problem texts (Serious = the account can't log on now), or null when it can't be read.
        internal sealed record Finding(string Text, bool Serious);

        public static List<Finding>? Read(string account)
        {
            if (account.EndsWith('$')) return null;
            string? domain = null, user = account;
            if (account.Contains('\\')) { var p = account.Split('\\', 2); domain = p[0]; user = p[1]; }
            else if (account.Contains('@')) { var p = account.Split('@', 2); user = p[0]; domain = p[1]; }
            if (domain == "." || string.Equals(domain, Environment.MachineName, StringComparison.OrdinalIgnoreCase)) domain = null;

            string? server = null;
            if (domain != null)
            {
                if (DsGetDcName(null, domain, IntPtr.Zero, null, DS_RETURN_FLAT_NAME_OR_DNS, out IntPtr dci) != 0) return null;
                try { server = Marshal.PtrToStructure<DOMAIN_CONTROLLER_INFO>(dci).DomainControllerName; }
                finally { NetApiBufferFree(dci); }
            }

            if (NetUserGetInfo(server, user, 3, out IntPtr buf) != 0) return null;
            USER_INFO_3 info;
            try { info = Marshal.PtrToStructure<USER_INFO_3>(buf); }
            finally { NetApiBufferFree(buf); }

            var list = new List<Finding>();
            if ((info.usri3_flags & UF_ACCOUNTDISABLE) != 0)
                list.Add(new("The account is DISABLED — enable it (AD: Account tab; local: net user <name> /active:yes).", true));
            if ((info.usri3_flags & UF_LOCKOUT) != 0)
                list.Add(new("The account is LOCKED OUT (too many wrong passwords) — unlock it (AD: Account tab, 'Unlock account').", true));
            if (info.usri3_acct_expires != TIMEQ_FOREVER &&
                DateTimeOffset.FromUnixTimeSeconds(info.usri3_acct_expires) <= DateTimeOffset.UtcNow)
                list.Add(new($"The account EXPIRED on {DateTimeOffset.FromUnixTimeSeconds(info.usri3_acct_expires).LocalDateTime:yyyy-MM-dd} — extend 'Account expires'.", true));
            else if (info.usri3_acct_expires != TIMEQ_FOREVER)
                list.Add(new($"The account expires on {DateTimeOffset.FromUnixTimeSeconds(info.usri3_acct_expires).LocalDateTime:yyyy-MM-dd} — the service stops working then.", false));
            if (info.usri3_password_expired != 0)
                list.Add(new("The password has EXPIRED, or 'User must change password at next logon' is set — change it once.", true));
            if ((info.usri3_flags & UF_DONT_EXPIRE_PASSWD) == 0)
                list.Add(new("'Password never expires' is not set — DbxStarterService stops when the password expires " +
                             "(then set the new one with the installer's 'Upgrade / Repair').", false));
            string ws = info.usri3_workstations ?? "";
            if (ws.Length > 0 && !Array.Exists(ws.Split(','), w => w.Trim().Equals(Environment.MachineName, StringComparison.OrdinalIgnoreCase)))
                list.Add(new($"Its 'Log On To' list ({ws}) does not include this computer ({Environment.MachineName}) — add it.", true));

            // "Protected Users" (RID 525 of the domain) — direct membership; not meant for service accounts
            if (domain != null && NetUserGetGroups(server, user, 0, out IntPtr groups, MAX_PREFERRED_LENGTH, out int read, out _) == 0)
            {
                try
                {
                    for (int i = 0; i < read; i++)
                    {
                        string? g = Marshal.PtrToStringUni(Marshal.ReadIntPtr(groups + i * IntPtr.Size));
                        if (g == null) continue;
                        try
                        {
                            string sid = LsaPrivileges.GetSidString($"{domain}\\{g}");
                            if (sid.EndsWith("-525"))
                                list.Add(new("The account is in 'Protected Users' — that group is not meant for service accounts; remove it.", false));
                        }
                        catch { /* group name not resolvable — skip */ }
                    }
                }
                finally { NetApiBufferFree(groups); }
            }
            return list;
        }

        // For a LOCAL account the installer just created: 'Password never expires' — with Windows' default
        // password policy (maximum age 42 days) DbxStarterService would otherwise stop starting after 42 days.
        // Throws on failure.
        public static void SetLocalPasswordNeverExpires(string user)
        {
            int rc = NetUserGetInfo(null, user, 1, out IntPtr buf);   // level 1008 exists only for NetUserSetInfo
            if (rc != 0) throw new Exception($"NetUserGetInfo failed (error {rc})");
            uint flags;
            try { flags = Marshal.PtrToStructure<USER_INFO_1>(buf).usri1_flags; }
            finally { NetApiBufferFree(buf); }

            IntPtr info = Marshal.AllocHGlobal(4);
            try
            {
                Marshal.WriteInt32(info, (int)(flags | UF_DONT_EXPIRE_PASSWD));
                rc = NetUserSetInfo(null, user, 1008, info, out _);
                if (rc != 0) throw new Exception($"NetUserSetInfo failed (error {rc})");
            }
            finally { Marshal.FreeHGlobal(info); }
        }

        [DllImport("netapi32.dll", CharSet = CharSet.Unicode)]
        private static extern int NetUserSetInfo(string? servername, string username, int level, IntPtr buf, out int parmErr);

        private const uint UF_ACCOUNTDISABLE     = 0x0002;
        private const uint UF_LOCKOUT            = 0x0010;
        private const uint UF_DONT_EXPIRE_PASSWD = 0x10000;
        private const uint TIMEQ_FOREVER         = 0xFFFFFFFF;
        private const uint DS_RETURN_FLAT_NAME_OR_DNS = 0;   // either form is accepted as DomainName
        private const int  MAX_PREFERRED_LENGTH  = -1;

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
        private struct USER_INFO_1
        {
            public string? usri1_name, usri1_password;
            public uint usri1_password_age, usri1_priv;
            public string? usri1_home_dir, usri1_comment;
            public uint usri1_flags;
            public string? usri1_script_path;
        }

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
        private struct USER_INFO_3
        {
            public string? usri3_name, usri3_password;
            public uint usri3_password_age, usri3_priv;
            public string? usri3_home_dir, usri3_comment;
            public uint usri3_flags;
            public string? usri3_script_path;
            public uint usri3_auth_flags;
            public string? usri3_full_name, usri3_usr_comment, usri3_parms, usri3_workstations;
            public uint usri3_last_logon, usri3_last_logoff, usri3_acct_expires, usri3_max_storage, usri3_units_per_week;
            public IntPtr usri3_logon_hours;
            public uint usri3_bad_pw_count, usri3_num_logons;
            public string? usri3_logon_server;
            public uint usri3_country_code, usri3_code_page, usri3_user_id, usri3_primary_group_id;
            public string? usri3_profile, usri3_home_dir_drive;
            public uint usri3_password_expired;
        }

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
        private struct DOMAIN_CONTROLLER_INFO
        {
            public string? DomainControllerName, DomainControllerAddress;
            public uint DomainControllerAddressType;
            public Guid DomainGuid;
            public string? DomainName, DnsForestName;
            public uint Flags;
            public string? DcSiteName, ClientSiteName;
        }

        [DllImport("netapi32.dll", CharSet = CharSet.Unicode)]
        private static extern int NetUserGetInfo(string? servername, string username, int level, out IntPtr bufptr);

        [DllImport("netapi32.dll", CharSet = CharSet.Unicode)]
        private static extern int NetUserGetGroups(string? servername, string username, int level, out IntPtr bufptr,
                                                   int prefmaxlen, out int entriesread, out int totalentries);

        [DllImport("netapi32.dll", CharSet = CharSet.Unicode)]
        private static extern int DsGetDcName(string? computerName, string domainName, IntPtr domainGuid, string? siteName,
                                              uint flags, out IntPtr domainControllerInfo);

        [DllImport("netapi32.dll")]
        private static extern int NetApiBufferFree(IntPtr buffer);
    }
}

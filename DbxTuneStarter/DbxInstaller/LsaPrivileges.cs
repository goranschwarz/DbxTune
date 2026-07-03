using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;
using Microsoft.Win32;

namespace DbxInstaller
{
    internal static class LsaPrivileges
    {
        public const string SeCreateSymbolicLinkPrivilege = "SeCreateSymbolicLinkPrivilege";
        public const string SeServiceLogonRight           = "SeServiceLogonRight";
        public const string SeDebugPrivilege              = "SeDebugPrivilege";

        public static void Grant(string accountName, string privilege)
        {
            byte[] sid     = LookupSid(accountName);
            var    objAttr = new LSA_OBJECT_ATTRIBUTES
                             { Length = (uint)Marshal.SizeOf<LSA_OBJECT_ATTRIBUTES>() };
            var emptyName  = default(LSA_UNICODE_STRING);

            uint status = LsaOpenPolicy(ref emptyName, ref objAttr, POLICY_ALL_ACCESS, out IntPtr policy);
            ThrowOnError(status, "LsaOpenPolicy");

            IntPtr sidPtr  = IntPtr.Zero;
            IntPtr privPtr = IntPtr.Zero;
            try
            {
                sidPtr  = Marshal.AllocHGlobal(sid.Length);
                Marshal.Copy(sid, 0, sidPtr, sid.Length);

                privPtr = Marshal.StringToHGlobalUni(privilege);
                var right = new LSA_UNICODE_STRING
                {
                    Buffer        = privPtr,
                    Length        = (ushort)(privilege.Length * 2),
                    MaximumLength = (ushort)((privilege.Length + 1) * 2)
                };

                status = LsaAddAccountRights(policy, sidPtr, new[] { right }, 1);
                ThrowOnError(status, "LsaAddAccountRights");
            }
            finally
            {
                if (sidPtr  != IntPtr.Zero) Marshal.FreeHGlobal(sidPtr);
                if (privPtr != IntPtr.Zero) Marshal.FreeHGlobal(privPtr);
                LsaClose(policy);
            }
        }

        // Reads the plaintext password Windows Service Control Manager stored for a service's
        // logon account. "sc config <svc> obj= ... password= ..." saves it as an LSA secret named
        // "_SC_<ServiceName>" (HKLM\SECURITY\Policy\Secrets\_SC_<ServiceName>), which SCM itself
        // decrypts every time the service starts. Any admin-elevated process can read it the same
        // way — this lets the Upgrade wizard flow avoid re-prompting for a password it already has.
        // Returns null if the secret doesn't exist or can't be read (e.g. gMSA-managed accounts,
        // which have no stored password here).
        public static string? GetServicePassword(string serviceName)
        {
            var objAttr  = new LSA_OBJECT_ATTRIBUTES { Length = (uint)Marshal.SizeOf<LSA_OBJECT_ATTRIBUTES>() };
            var emptyName = default(LSA_UNICODE_STRING);

            if (LsaOpenPolicy(ref emptyName, ref objAttr, POLICY_ALL_ACCESS, out IntPtr policy) != 0)
                return null;

            IntPtr secretNamePtr = IntPtr.Zero;
            IntPtr secretHandle  = IntPtr.Zero;
            IntPtr currentValue  = IntPtr.Zero;
            try
            {
                string secretName = $"_SC_{serviceName}";
                secretNamePtr = Marshal.StringToHGlobalUni(secretName);
                var secretNameLsa = new LSA_UNICODE_STRING
                {
                    Buffer        = secretNamePtr,
                    Length        = (ushort)(secretName.Length * 2),
                    MaximumLength = (ushort)((secretName.Length + 1) * 2),
                };

                if (LsaOpenSecret(policy, ref secretNameLsa, SECRET_QUERY_VALUE, out secretHandle) != 0)
                    return null;

                if (LsaQuerySecret(secretHandle, ref currentValue, IntPtr.Zero, IntPtr.Zero, IntPtr.Zero) != 0
                    || currentValue == IntPtr.Zero)
                    return null;

                var value = Marshal.PtrToStructure<LSA_UNICODE_STRING>(currentValue);
                if (value.Buffer == IntPtr.Zero || value.Length == 0) return null;
                return Marshal.PtrToStringUni(value.Buffer, value.Length / 2);
            }
            catch { return null; }
            finally
            {
                if (currentValue  != IntPtr.Zero) LsaFreeMemory(currentValue);
                if (secretHandle  != IntPtr.Zero) LsaClose(secretHandle);
                if (secretNamePtr != IntPtr.Zero) Marshal.FreeHGlobal(secretNamePtr);
                LsaClose(policy);
            }
        }

        public static string GetSidString(string accountName)
        {
            byte[] sid = LookupSid(accountName);
            IntPtr ptr = IntPtr.Zero;
            try
            {
                if (!ConvertSidToStringSid(sid, out ptr))
                    throw new Exception($"ConvertSidToStringSid failed (Win32 error {Marshal.GetLastWin32Error()})");
                return Marshal.PtrToStringUni(ptr)!;
            }
            finally
            {
                if (ptr != IntPtr.Zero) LocalFree(ptr);
            }
        }

        private static byte[] LookupSid(string accountName)
        {
            int sidSize = 0, domainSize = 0;
            LookupAccountName(null, accountName, null, ref sidSize, null, ref domainSize, out _);

            byte[] sid    = new byte[sidSize];
            var    domain = new StringBuilder(domainSize);
            if (!LookupAccountName(null, accountName, sid, ref sidSize, domain, ref domainSize, out _))
                throw new Exception(
                    $"Account '{accountName}' not found (Win32 error {Marshal.GetLastWin32Error()})");
            return sid;
        }

        private static void ThrowOnError(uint status, string context)
        {
            if (status != 0)
                throw new Exception($"{context} failed — NTSTATUS 0x{status:X8}");
        }

        // ── P/Invoke ──────────────────────────────────────────────────────────

        private const uint POLICY_ALL_ACCESS = 0x00F0FFF;

        [StructLayout(LayoutKind.Sequential)]
        private struct LSA_UNICODE_STRING
        {
            public ushort Length;
            public ushort MaximumLength;
            public IntPtr Buffer;
        }

        [StructLayout(LayoutKind.Sequential)]
        private struct LSA_OBJECT_ATTRIBUTES
        {
            public uint   Length;
            public IntPtr RootDirectory;
            public IntPtr ObjectName;
            public uint   Attributes;
            public IntPtr SecurityDescriptor;
            public IntPtr SecurityQualityOfService;
        }

        [DllImport("advapi32.dll", SetLastError = true)]
        private static extern bool LookupAccountName(
            string? systemName, string accountName,
            byte[]? sid, ref int cbSid,
            StringBuilder? domainName, ref int cchDomain,
            out int sidUse);

        [DllImport("advapi32.dll")]
        private static extern uint LsaOpenPolicy(
            ref LSA_UNICODE_STRING systemName,
            ref LSA_OBJECT_ATTRIBUTES objectAttributes,
            uint desiredAccess,
            out IntPtr policyHandle);

        [DllImport("advapi32.dll")]
        private static extern uint LsaAddAccountRights(
            IntPtr policyHandle,
            IntPtr accountSid,
            LSA_UNICODE_STRING[] userRights,
            int countOfRights);

        [DllImport("advapi32.dll")]
        private static extern uint LsaClose(IntPtr objectHandle);

        private const uint SECRET_QUERY_VALUE = 0x0002;

        [DllImport("advapi32.dll", CharSet = CharSet.Unicode)]
        private static extern uint LsaOpenSecret(
            IntPtr policyHandle,
            ref LSA_UNICODE_STRING secretName,
            uint desiredAccess,
            out IntPtr secretHandle);

        [DllImport("advapi32.dll")]
        private static extern uint LsaQuerySecret(
            IntPtr secretHandle,
            ref IntPtr currentValue,
            IntPtr currentValueSetTime,
            IntPtr oldValue,
            IntPtr oldValueSetTime);

        [DllImport("advapi32.dll")]
        private static extern uint LsaFreeMemory(IntPtr buffer);

        [DllImport("advapi32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
        private static extern bool ConvertSidToStringSid(byte[] sid, out IntPtr stringSid);

        [DllImport("kernel32.dll")]
        private static extern IntPtr LocalFree(IntPtr mem);

        // ── profile creation ──────────────────────────────────────────────────

        // Creates the Windows user profile directory + NTUSER.DAT for the given account.
        // Safe to call when the profile already exists — returns true in both cases.
        public static bool EnsureUserProfile(string accountName, out string profilePath)
        {
            profilePath = "";
            string sid = GetSidString(accountName);

            var path = new StringBuilder(260);
            int  hr  = CreateProfile(sid, accountName, path, (uint)path.Capacity);

            const int S_OK                   = 0;
            const int ERROR_ALREADY_EXISTS   = unchecked((int)0x800700B7);

            if (hr == S_OK || hr == ERROR_ALREADY_EXISTS)
            {
                profilePath = hr == S_OK ? path.ToString() : GetExistingProfilePath(sid);
                return true;
            }
            throw new Exception($"CreateProfile failed — HRESULT 0x{hr:X8}");
        }

        private static string GetExistingProfilePath(string sid)
        {
            const string key = @"SOFTWARE\Microsoft\Windows NT\CurrentVersion\ProfileList\";
            using var k = Microsoft.Win32.Registry.LocalMachine.OpenSubKey(key + sid);
            return k?.GetValue("ProfileImagePath") as string ?? "";
        }

        [DllImport("userenv.dll", CharSet = CharSet.Unicode)]
        private static extern int CreateProfile(string pszUserSid, string pszUserName,
            StringBuilder pszProfilePath, uint cchProfilePath);

        // ── token privilege helpers ───────────────────────────────────────────

        // Enable one or more privileges in the current process token.
        // Returns an action that restores (disables) them when disposed.
        public static IDisposable EnablePrivileges(params string[] names)
        {
            OpenProcessToken(GetCurrentProcess(),
                TOKEN_ADJUST_PRIVILEGES | TOKEN_QUERY, out IntPtr token);

            var previous = new List<(LUID luid, uint attrs)>();
            foreach (var name in names)
            {
                if (!LookupPrivilegeValue(null, name, out LUID luid)) continue;

                var tp = new TOKEN_PRIVILEGES
                {
                    PrivilegeCount = 1,
                    Privileges     = new LUID_AND_ATTRIBUTES[1],
                };
                tp.Privileges[0] = new LUID_AND_ATTRIBUTES { Luid = luid, Attributes = SE_PRIVILEGE_ENABLED };
                int size = Marshal.SizeOf<TOKEN_PRIVILEGES>();
                AdjustTokenPrivileges(token, false, ref tp, size, out TOKEN_PRIVILEGES prev, out _);
                previous.Add((luid, prev.PrivilegeCount > 0 ? prev.Privileges[0].Attributes : 0));
            }

            return new PrivilegeRestorer(token, previous);
        }

        private sealed class PrivilegeRestorer : IDisposable
        {
            private readonly IntPtr _token;
            private readonly List<(LUID luid, uint attrs)> _previous;
            public PrivilegeRestorer(IntPtr token, List<(LUID, uint)> previous)
                { _token = token; _previous = previous; }

            public void Dispose()
            {
                foreach (var (luid, attrs) in _previous)
                {
                    var tp = new TOKEN_PRIVILEGES
                    {
                        PrivilegeCount = 1,
                        Privileges     = new LUID_AND_ATTRIBUTES[1],
                    };
                    tp.Privileges[0] = new LUID_AND_ATTRIBUTES { Luid = luid, Attributes = attrs };
                    int size = Marshal.SizeOf<TOKEN_PRIVILEGES>();
                    AdjustTokenPrivileges(_token, false, ref tp, size, out _, out _);
                }
                CloseHandle(_token);
            }
        }

        private const uint TOKEN_QUERY            = 0x0008;
        private const uint TOKEN_ADJUST_PRIVILEGES = 0x0020;
        private const uint SE_PRIVILEGE_ENABLED    = 0x0002;

        [StructLayout(LayoutKind.Sequential)]
        private struct LUID { public uint LowPart; public int HighPart; }

        [StructLayout(LayoutKind.Sequential)]
        private struct LUID_AND_ATTRIBUTES { public LUID Luid; public uint Attributes; }

        [StructLayout(LayoutKind.Sequential)]
        private struct TOKEN_PRIVILEGES
        {
            public uint PrivilegeCount;
            [MarshalAs(UnmanagedType.ByValArray, SizeConst = 1)]
            public LUID_AND_ATTRIBUTES[] Privileges;
        }

        [DllImport("kernel32.dll")] private static extern IntPtr GetCurrentProcess();
        [DllImport("kernel32.dll")] private static extern bool   CloseHandle(IntPtr h);

        [DllImport("advapi32.dll", SetLastError = true)]
        private static extern bool OpenProcessToken(IntPtr process, uint access, out IntPtr token);

        [DllImport("advapi32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
        private static extern bool LookupPrivilegeValue(string? system, string name, out LUID luid);

        [DllImport("advapi32.dll", SetLastError = true)]
        private static extern bool AdjustTokenPrivileges(IntPtr token, bool disableAll,
            ref TOKEN_PRIVILEGES newState, int bufLen,
            out TOKEN_PRIVILEGES previousState, out int returnLen);
    }
}

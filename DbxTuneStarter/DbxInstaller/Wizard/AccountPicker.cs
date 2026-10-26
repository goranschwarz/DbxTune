using System;
using System.Runtime.InteropServices;
using System.Runtime.InteropServices.ComTypes;
using System.Security.Principal;

namespace DbxInstaller.Wizard
{
    // The standard Windows "Select User" dialog (the object picker, IDsObjectPicker — the one from
    // Local Users and Groups or file permissions), for the service account. Looks in this computer and,
    // when joined, the domain (and the rest of the forest); users and (group) managed service accounts.
    // Returns the account as the installer writes it: "name" for a local account, "DOMAIN\name" for a
    // domain account, "DOMAIN\name$" for a gMSA — or null when cancelled.
    internal static class AccountPicker
    {
        public static string? Pick(IntPtr owner, bool domainJoined)
        {
            var picker = (IDsObjectPicker)new DSObjectPicker();
            IntPtr attrName = Marshal.StringToHGlobalUni("objectSid");
            IntPtr attrs    = Marshal.AllocHGlobal(IntPtr.Size);
            IntPtr scopes   = IntPtr.Zero;
            try
            {
                Marshal.WriteIntPtr(attrs, attrName);
                var scopeList = new[]
                {
                    // This computer: local users
                    new DSOP_SCOPE_INIT_INFO
                    {
                        cbSize = (uint)Marshal.SizeOf<DSOP_SCOPE_INIT_INFO>(),
                        flType = DSOP_SCOPE_TYPE_TARGET_COMPUTER,
                        flScope = (domainJoined ? 0 : DSOP_SCOPE_FLAG_STARTING_SCOPE) | DSOP_SCOPE_FLAG_DEFAULT_FILTER_USERS,
                        FilterFlags = new DSOP_FILTER_FLAGS { flDownlevel = DSOP_DOWNLEVEL_FILTER_USERS },
                    },
                    // The joined domain, the other domains of the forest, the global catalog: users + gMSA
                    new DSOP_SCOPE_INIT_INFO
                    {
                        cbSize = (uint)Marshal.SizeOf<DSOP_SCOPE_INIT_INFO>(),
                        flType = DSOP_SCOPE_TYPE_UPLEVEL_JOINED_DOMAIN | DSOP_SCOPE_TYPE_DOWNLEVEL_JOINED_DOMAIN |
                                 DSOP_SCOPE_TYPE_ENTERPRISE_DOMAIN | DSOP_SCOPE_TYPE_GLOBAL_CATALOG,
                        flScope = (domainJoined ? DSOP_SCOPE_FLAG_STARTING_SCOPE : 0) |
                                  DSOP_SCOPE_FLAG_DEFAULT_FILTER_USERS | DSOP_SCOPE_FLAG_DEFAULT_FILTER_SERVICE_ACCOUNTS,
                        FilterFlags = new DSOP_FILTER_FLAGS
                        {
                            Uplevel = new DSOP_UPLEVEL_FILTER_FLAGS { flBothModes = DSOP_FILTER_USERS | DSOP_FILTER_SERVICE_ACCOUNTS },
                            flDownlevel = DSOP_DOWNLEVEL_FILTER_USERS,
                        },
                    },
                };
                int size = Marshal.SizeOf<DSOP_SCOPE_INIT_INFO>();
                scopes = Marshal.AllocHGlobal(size * scopeList.Length);
                for (int i = 0; i < scopeList.Length; i++) Marshal.StructureToPtr(scopeList[i], scopes + i * size, false);

                var init = new DSOP_INIT_INFO
                {
                    cbSize = (uint)Marshal.SizeOf<DSOP_INIT_INFO>(),
                    pwzTargetComputer = null,          // this computer
                    cDsScopeInfos = (uint)scopeList.Length,
                    aDsScopeInfos = scopes,
                    flOptions = 0,                     // one account
                    cAttributesToFetch = 1,
                    apwzAttributeNames = attrs,
                };
                Marshal.ThrowExceptionForHR(picker.Initialize(ref init));

                int hr = picker.InvokeDialog(owner, out IDataObject? data);
                if (hr != 0 || data == null) return null;      // S_FALSE = cancelled
                return ReadSelection(data);
            }
            finally
            {
                if (scopes != IntPtr.Zero) Marshal.FreeHGlobal(scopes);
                Marshal.FreeHGlobal(attrs);
                Marshal.FreeHGlobal(attrName);
                Marshal.ReleaseComObject(picker);
            }
        }

        private static string? ReadSelection(IDataObject data)
        {
            var format = new FORMATETC
            {
                cfFormat = (short)RegisterClipboardFormat("CFSTR_DSOP_DS_SELECTION_LIST"),
                dwAspect = DVASPECT.DVASPECT_CONTENT,
                lindex   = -1,
                tymed    = TYMED.TYMED_HGLOBAL,
            };
            data.GetData(ref format, out STGMEDIUM medium);
            IntPtr list = GlobalLock(medium.unionmember);
            try
            {
                if (Marshal.ReadInt32(list) < 1) return null;    // DS_SELECTION_LIST.cItems
                // DS_SELECTION_LIST = { ULONG cItems; ULONG cFetchedAttributes; DS_SELECTION aDsSelection[] }
                var sel = Marshal.PtrToStructure<DS_SELECTION>(list + 8);

                // The SID gives the exact "DOMAIN\name" (or "COMPUTER\name" for a local account)
                if (sel.pvarFetchedAttributes != IntPtr.Zero &&
                    Marshal.GetObjectForNativeVariant(sel.pvarFetchedAttributes) is byte[] sidBytes)
                {
                    string name = new SecurityIdentifier(sidBytes, 0).Translate(typeof(NTAccount)).Value;
                    string prefix = name.Split('\\')[0];
                    return prefix.Equals(Environment.MachineName, StringComparison.OrdinalIgnoreCase) ? name.Split('\\', 2)[1] : name;
                }
                // No SID: "WinNT://DOMAIN/name" (downlevel), else just the name
                string path = sel.pwzADsPath ?? "";
                if (path.StartsWith("WinNT://", StringComparison.OrdinalIgnoreCase))
                {
                    var parts = path.Substring(8).Split('/');
                    if (parts.Length >= 2)
                        return parts[0].Equals(Environment.MachineName, StringComparison.OrdinalIgnoreCase) ? parts[^1] : $"{parts[0]}\\{parts[^1]}";
                }
                return sel.pwzName;
            }
            finally
            {
                GlobalUnlock(medium.unionmember);
                ReleaseStgMedium(ref medium);
            }
        }

        // ── objsel.h ──────────────────────────────────────────────────────────
        private const uint DSOP_SCOPE_TYPE_TARGET_COMPUTER          = 0x0001;
        private const uint DSOP_SCOPE_TYPE_UPLEVEL_JOINED_DOMAIN    = 0x0002;
        private const uint DSOP_SCOPE_TYPE_DOWNLEVEL_JOINED_DOMAIN  = 0x0004;
        private const uint DSOP_SCOPE_TYPE_ENTERPRISE_DOMAIN        = 0x0008;
        private const uint DSOP_SCOPE_TYPE_GLOBAL_CATALOG           = 0x0010;
        private const uint DSOP_SCOPE_FLAG_STARTING_SCOPE           = 0x0001;
        private const uint DSOP_SCOPE_FLAG_DEFAULT_FILTER_USERS     = 0x0040;
        private const uint DSOP_SCOPE_FLAG_DEFAULT_FILTER_SERVICE_ACCOUNTS = 0x0400;
        private const uint DSOP_FILTER_USERS                        = 0x0002;
        private const uint DSOP_FILTER_SERVICE_ACCOUNTS             = 0x1000;
        private const uint DSOP_DOWNLEVEL_FILTER_USERS              = 0x80000001;

        [StructLayout(LayoutKind.Sequential)]
        private struct DSOP_UPLEVEL_FILTER_FLAGS { public uint flBothModes, flMixedModeOnly, flNativeModeOnly; }

        [StructLayout(LayoutKind.Sequential)]
        private struct DSOP_FILTER_FLAGS { public DSOP_UPLEVEL_FILTER_FLAGS Uplevel; public uint flDownlevel; }

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
        private struct DSOP_SCOPE_INIT_INFO
        {
            public uint cbSize, flType, flScope;
            public DSOP_FILTER_FLAGS FilterFlags;
            public string? pwzDcName, pwzADsPath;
            public int hr;
        }

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
        private struct DSOP_INIT_INFO
        {
            public uint cbSize;
            public string? pwzTargetComputer;
            public uint cDsScopeInfos;
            public IntPtr aDsScopeInfos;
            public uint flOptions;
            public uint cAttributesToFetch;
            public IntPtr apwzAttributeNames;
        }

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
        private struct DS_SELECTION
        {
            public string? pwzName, pwzADsPath, pwzClass, pwzUPN;
            public IntPtr pvarFetchedAttributes;
            public uint flScopeType;
        }

        [ComImport, Guid("17D6CCD8-3B7B-11D2-B9E0-00C04FD8DBF7")]
        private class DSObjectPicker { }

        [ComImport, Guid("0C87E64E-3B7A-11D2-B9E0-00C04FD8DBF7"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
        private interface IDsObjectPicker
        {
            [PreserveSig] int Initialize(ref DSOP_INIT_INFO pInitInfo);
            [PreserveSig] int InvokeDialog(IntPtr hwndParent, out IDataObject? ppdoSelections);
        }

        [DllImport("user32.dll", CharSet = CharSet.Unicode)] private static extern uint RegisterClipboardFormat(string format);
        [DllImport("kernel32.dll")] private static extern IntPtr GlobalLock(IntPtr hMem);
        [DllImport("kernel32.dll")] private static extern bool GlobalUnlock(IntPtr hMem);
        [DllImport("ole32.dll")] private static extern void ReleaseStgMedium(ref STGMEDIUM medium);
    }
}

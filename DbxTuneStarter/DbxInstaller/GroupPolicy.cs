using System;
using System.Collections.Generic;
using System.Linq;
using System.Management;

namespace DbxInstaller
{
    // Reads which "User Rights Assignment" and "Restricted Groups" settings domain Group Policy
    // applies to this computer, from the Resultant Set of Policy (RSoP) data Windows logs in WMI
    // (root\rsop\computer — the same data rsop.msc shows). A user right or a local group that a GPO
    // sets is REPLACED by the GPO's list at every policy refresh (reboot, gpupdate, and at least
    // every 16 hours), so whatever the installer granted locally for it is lost then.
    internal static class GroupPolicy
    {
        // Name    = the user right ("SeServiceLogonRight"), or the restricted group as the GPO names it
        // Members = the accounts the GPO lists, as it lists them: usually names ("Administrators",
        //           "CORP\DbxTune Admins", "NT SERVICE\ALL SERVICES"), sometimes SIDs ("*S-1-5-32-544")
        // GpoName = the GPO's display name (what rsop.msc shows as "Source GPO")
        internal sealed record Setting(string Name, string[] Members, string GpoName);

        public static bool IsDomainJoined()
        {
            using var searcher = new ManagementObjectSearcher("SELECT PartOfDomain FROM Win32_ComputerSystem");
            using var results  = searcher.Get();
            foreach (ManagementBaseObject o in results)
                using (o) return o["PartOfDomain"] is bool b && b;
            return false;
        }

        // Only the winning setting (precedence 1) of each right/group, and only from domain GPOs.
        // Throws when the RSoP data can't be read (not elevated, RSoP logging turned off by policy, …).
        public static (List<Setting> UserRights, List<Setting> RestrictedGroups) ReadComputerSettings()
        {
            var scope = new ManagementScope(@"\\.\root\rsop\computer");
            scope.Connect();

            // RSOP_GPO.id is the GPO's AD path ("CN={GUID},CN=Policies,CN=System,DC=…") — the same
            // value the settings carry in GPOID. The local policy shows up as "LocalGPO"; the
            // installer's own grants live there, so it must not count as "set by Group Policy".
            var gpoNames = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
            foreach (var o in Query(scope, "SELECT id, name FROM RSOP_GPO"))
                if (o["id"] is string id)
                    gpoNames[id] = o["name"] is string n && n.Length > 0 ? n : id;

            bool IsDomainGpo(string gpoId) =>
                gpoId.Length > 0
                && !gpoId.Equals("LocalGPO", StringComparison.OrdinalIgnoreCase)
                && !(gpoNames.TryGetValue(gpoId, out var name) && name.Equals("Local Group Policy", StringComparison.OrdinalIgnoreCase));

            string GpoName(string gpoId) => gpoNames.TryGetValue(gpoId, out var name) ? name : gpoId;

            var userRights = new List<Setting>();
            foreach (var o in Query(scope, "SELECT UserRight, AccountList, GPOID FROM RSOP_UserPrivilegeRight WHERE precedence = 1"))
            {
                string gpoId = o["GPOID"] as string ?? "";
                if (o["UserRight"] is string right && IsDomainGpo(gpoId))
                    userRights.Add(new Setting(right, ToMembers(o["AccountList"]), GpoName(gpoId)));
            }

            var restrictedGroups = new List<Setting>();
            foreach (var o in Query(scope, "SELECT GroupName, Members, GPOID FROM RSOP_RestrictedGroup WHERE precedence = 1"))
            {
                string gpoId = o["GPOID"] as string ?? "";
                string[] members = ToMembers(o["Members"]);
                // No members at all can mean "remove everyone" or "only 'This group is a member of'
                // is configured" — RSoP doesn't tell the two apart, so don't guess (no false alarm).
                if (o["GroupName"] is string group && IsDomainGpo(gpoId) && members.Length > 0)
                    restrictedGroups.Add(new Setting(group, members, GpoName(gpoId)));
            }

            return (userRights, restrictedGroups);
        }

        // A class that isn't registered in root\rsop\computer means its policy extension never
        // logged anything on this machine, i.e. no GPO sets any of it — same as no instances.
        private static List<ManagementBaseObject> Query(ManagementScope scope, string wql)
        {
            try
            {
                using var searcher = new ManagementObjectSearcher(scope, new ObjectQuery(wql));
                return searcher.Get().Cast<ManagementBaseObject>().ToList();
            }
            catch (ManagementException ex) when (ex.ErrorCode == ManagementStatus.InvalidClass)
            {
                return new List<ManagementBaseObject>();
            }
        }

        private static string[] ToMembers(object? value) => value switch
        {
            string[] list => list.Where(s => !string.IsNullOrWhiteSpace(s)).Select(s => s.Trim()).ToArray(),
            string one    => one.Split(new[] { ',', ';' }, StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries),
            _             => Array.Empty<string>(),
        };
    }
}

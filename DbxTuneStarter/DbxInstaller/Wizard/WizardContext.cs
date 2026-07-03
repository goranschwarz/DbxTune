using System.Collections.Generic;

namespace DbxInstaller.Wizard
{
    // Shared state threaded through every wizard page. Only WizardPage.OnLeave should write
    // to Config; only OnEnter should read from it — avoids the reentrancy hazard the old
    // InstallerForm._updatingPaths flag used to guard against.
    internal sealed class WizardContext
    {
        public InstallConfig Config { get; }
        public InstallLog Log { get; set; } = _ => { };

        // Populated by the Welcome page's existing-install detection; read by Upgrade/Uninstall pages.
        public bool ExistingInstallDetected { get; set; }
        public string? DetectedServiceAccount { get; set; }
        public string? DetectedProfileDir { get; set; }
        public string? DetectedInstallDir { get; set; }
        public bool DetectedIsLocalAccount { get; set; }
        public Dictionary<string, string> DetectedEnvVars { get; set; } = new();
        public List<string> DetectedDataDirs { get; set; } = new();
        public List<string> DetectedExternalDirs { get; set; } = new();

        // Removal Options page choices, consumed by the Uninstalling (Progress) page.
        public bool RemoveAccount { get; set; }
        public bool RemoveData { get; set; }
        public bool RemoveSoftware { get; set; }

        // Config file paths reported by each DBMS's own "--cfgWizard" run (DbmsConfigWizardPage) —
        // read by DbxCentralWebConfigPage to offer reusing any mail settings already configured there.
        public List<string> DbmsConfigFiles { get; set; } = new();

        // Mail suffixes ("smtp.hostname"/"to"/"from") already auto-filled into DBX_CENTRAL.conf
        // from a DBMS config file this wizard run — DBX_CENTRAL.conf ships with its own active
        // (non-commented) default values, so "is it currently unset" can't be used to decide
        // whether to apply the DBMS-detected value; this flag ensures it's applied exactly once
        // per suffix instead, without repeatedly overwriting a later manual edit on a revisit.
        public HashSet<string> AutoFilledMailSuffixes { get; } = new();

        // Set by whichever Progress page (Install/Upgrade/Uninstall) opens its log file —
        // read by the corresponding Finish page.
        public string? LogFilePath { get; set; }

        public WizardContext(InstallConfig config) => Config = config;
    }
}

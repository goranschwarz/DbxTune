using System.Collections.Generic;
using DbxInstaller.Pages;
using DbxInstaller.Uninstall;
using DbxInstaller.Wizard;

namespace DbxInstaller
{
    // Single window for every wizard flow. Starts at Welcome (unless launched with --remove,
    // which skips straight to the Remove page group); the rest of the sequence is appended
    // once the chosen InstallMode is known — see OnLeavingPage.
    internal sealed class MainWizardWindow : WizardWindow
    {
        public MainWizardWindow(InstallConfig config, bool skipWelcome) : base(config)
        {
            if (skipWelcome)
            {
                WelcomePage.DetectExistingInstall(Context);
                Pages.AddRange(RemovePages());
            }
            else
            {
                Pages.Add(new WelcomePage());
            }
            Start();
        }

        protected override void OnLeavingPage(WizardPage page)
        {
            if (page is WelcomePage)
            {
                // Going Back to Welcome and picking a different mode re-fires this — discard
                // whatever group was appended on a previous pass through here first, otherwise
                // the new group piles up after the old one instead of replacing it (the old
                // pages stay reachable via Next and the new ones become permanently stranded
                // at the tail of the list).
                if (Pages.Count > 1) Pages.RemoveRange(1, Pages.Count - 1);
                Pages.AddRange(Context.Config.Mode switch
                {
                    InstallMode.Upgrade => UpgradePages(),
                    InstallMode.Remove  => RemovePages(),
                    _                   => InstallPages(),
                });
            }
        }

        private static IEnumerable<WizardPage> InstallPages() => new WizardPage[]
        {
            new PrerequisitesPage(),
            new PackageSourcePage(),
            new ServiceAccountPage(),
            new InstallLocationPage(),
            new DbxStarterWebConfigPage(),
            new DbmsSelectionPage(),
            new ReadyToInstallPage(),
            new InstallProgressPage(),
            new DbmsConfigWizardPage(),
            new OsMonitoringPage(),
            new DbxCentralWebConfigPage(),
            new FirewallStartServicePage(),
            new FinishPage(),
        };

        private static IEnumerable<WizardPage> UpgradePages() => new WizardPage[]
        {
            new PrerequisitesPage(),
            new PackageSourcePage(),
            new ReadyToUpgradePage(),
            new UpgradeProgressPage(),
            new FinishPage(),
        };

        private static IEnumerable<WizardPage> RemovePages() => new WizardPage[]
        {
            new DiscoveryPage(),
            new RemovalOptionsPage(),
            new ReadyToUninstallPage(),
            new UninstallProgressPage(),
            new UninstallFinishPage(),
        };
    }
}

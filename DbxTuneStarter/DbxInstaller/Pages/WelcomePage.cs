using System.Collections.Generic;
using System.Windows;
using System.Windows.Controls;
using DbxInstaller.Wizard;

namespace DbxInstaller.Pages
{
    internal sealed class WelcomePage : WizardPage
    {
        public override string StepTitle => "Welcome";

        private readonly RadioButton _rbInstall = new() { GroupName = "mode", IsChecked = true, Margin = new Thickness(0, 8, 0, 4) };
        private readonly RadioButton _rbUpgrade = new() { GroupName = "mode", Margin = new Thickness(0, 8, 0, 4) };
        private readonly RadioButton _rbRemove  = new() { GroupName = "mode", Margin = new Thickness(0, 8, 0, 4) };

        private bool _detected;

        public WelcomePage()
        {
            _rbInstall.Content = "Install — set up DbxTune on this machine";
            _rbUpgrade.Content = "Upgrade — update an existing DbxTune installation";
            _rbRemove.Content  = "Remove — uninstall DbxTune from this machine";

            var stack = PageHelpers.Stack(
                PageHelpers.Title("DbxTune Setup Wizard"),
                PageHelpers.Subtitle("This wizard installs, upgrades, or removes DbxTune and the DbxStarter service " +
                                     "on this machine. Choose what you'd like to do, then click Next."),
                _rbInstall, _rbUpgrade, _rbRemove);
            Content = stack;
        }

        public override void OnEnter(WizardContext ctx)
        {
            if (_detected) return;
            _detected = true;
            DetectExistingInstall(ctx);

            _rbUpgrade.IsEnabled = ctx.ExistingInstallDetected;
            _rbRemove.IsEnabled  = ctx.ExistingInstallDetected;
            if (!ctx.ExistingInstallDetected) _rbInstall.IsChecked = true;
        }

        // Shared with MainWizardWindow's --remove fast path, which skips the Welcome page's UI
        // entirely but still needs this same detection to populate the Discovery/RemovalOptions pages.
        public static void DetectExistingInstall(WizardContext ctx)
        {
            string? account = UninstallActions.GetServiceAccount();
            string? accountToScan = account;
            if (accountToScan == null && System.IO.Directory.Exists(@"C:\Users\dbxtune"))
                accountToScan = "dbxtune";

            if (accountToScan == null) return;

            ctx.DetectedServiceAccount = accountToScan;
            ctx.DetectedIsLocalAccount = !accountToScan.Contains('\\') && !accountToScan.Contains('@') && !accountToScan.EndsWith('$');
            ctx.DetectedProfileDir     = UninstallActions.GetProfileDir(accountToScan);
            if (account != null) ctx.DetectedEnvVars = InstallActions.ReadUserEnvVars(account);

            string? dbxHome = ctx.DetectedEnvVars.GetValueOrDefault("DBXTUNE_HOME");
            ctx.DetectedInstallDir = !string.IsNullOrEmpty(dbxHome)
                ? System.IO.Path.GetDirectoryName(dbxHome.TrimEnd('\\', '/'))
                : null;

            ctx.DetectedDataDirs     = UninstallActions.BuildDataDirList(ctx.DetectedEnvVars);
            ctx.DetectedExternalDirs = ctx.DetectedDataDirs.FindAll(d =>
                ctx.DetectedProfileDir == null ||
                !d.StartsWith(ctx.DetectedProfileDir + System.IO.Path.DirectorySeparatorChar,
                    System.StringComparison.OrdinalIgnoreCase));

            ctx.ExistingInstallDetected = true;
        }

        public override void OnLeave(WizardContext ctx)
        {
            ctx.Config.Mode = _rbUpgrade.IsChecked == true ? InstallMode.Upgrade
                            : _rbRemove.IsChecked  == true ? InstallMode.Remove
                            : InstallMode.Install;
        }
    }
}

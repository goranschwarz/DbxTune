using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Threading;
using DbxInstaller.Wizard;

namespace DbxInstaller.Pages
{
    // Recaps the detected existing install (read on the Welcome page) and populates the
    // Config fields that Upgrade mode skips asking for (Service Account / Install Location /
    // DBMS Selection pages aren't part of the Upgrade flow).
    internal sealed class ReadyToUpgradePage : WizardPage
    {
        public override string StepTitle => "Ready to Upgrade / Repair";

        private readonly TextBox _summary = new()
        {
            Style = (Style)Application.Current.Resources["SelectableText"],
            FontFamily = new System.Windows.Media.FontFamily("Consolas"),
        };
        private readonly PasswordBox _txtPassword = new() { Margin = new Thickness(0, 4, 0, 4) };
        private readonly Button _btnCheck = new() { Content = "Check" };
        private readonly TextBlock _lblPasswordStatus = new() { TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 0, 0, 12) };

        // The profile folder of the account (checked when the page opens, and by Check), with "Clean up…".
        private readonly ProfileCheckPanel _profile = new("upgrade / repair");

        private bool _isGmsa;
        private string _account = "dbxtune";

        public ReadyToUpgradePage()
        {
            _btnCheck.Click += async (_, _) =>
            {
                _lblPasswordStatus.Text = "Checking…";
                _lblPasswordStatus.SetResourceReference(TextBlock.ForegroundProperty, "FgDimBrush");
                _btnCheck.IsEnabled = false;
                // LogonUser against a domain controller can take a noticeable moment — let the
                // "Checking…" text actually paint before the blocking call runs.
                await Dispatcher.Yield(DispatcherPriority.Background);

                var (outcome, message) = InstallActions.TryVerifyPassword(_account, _txtPassword.Password, upgrade: true);
                _lblPasswordStatus.Text = message;
                string brush = outcome switch
                {
                    InstallActions.VerifyOutcome.Verified => "GreenBrush",
                    InstallActions.VerifyOutcome.Info     => "OrangeBrush",
                    _                                      => "RedBrush",
                };
                _lblPasswordStatus.SetResourceReference(TextBlock.ForegroundProperty, brush);

                await _profile.CheckAsync(_account);
                _btnCheck.IsEnabled = true;
            };

            // What the steps on the next page do (UpgradeProgressPage.UpgradeStepNames) — keep in line.
            var does = PageHelpers.Stack(
                PageHelpers.Dim("1.  The service account, set up again: the account (a missing local account is created again), its profile " +
                                "folder, the user rights Log on as a service, Allow log on locally, Create symbolic links and Debug programs, " +
                                "the local groups Performance Log Users, Performance Monitor Users and Remote Desktop Users — then a logon " +
                                "test and the Group Policy check. A problem stops it here, before the service is stopped."),
                PageHelpers.Dim("2.  The software: stop the service, install the package from the previous page (a newer version, or the " +
                                "same one to repair the files), the DBXTUNE_* variables, initialize the DbxTune home."),
                PageHelpers.Dim("3.  The service: its account and password set again (sc config), start/stop rights, then restart."),
                PageHelpers.Dim("Kept as they are: your configuration and recordings, the service's web settings, the firewall rules, " +
                                "the DBMS configuration."));
            foreach (TextBlock line in does.Children) line.Margin = new Thickness(0, 0, 0, 3);

            Content = new ScrollViewer
            {
                VerticalScrollBarVisibility = ScrollBarVisibility.Auto,
                Content = PageHelpers.Stack(
                    PageHelpers.Title("Ready to Upgrade / Repair"),
                    PageHelpers.Subtitle("Updates the existing DbxTune installation below, or repairs it. Service account, install " +
                                          "location and DBMS selection are unchanged."),
                    _summary,
                    PageHelpers.SectionHeader("What Upgrade / Repair does"),
                    does,
                    PageHelpers.SectionHeader("Service Account Password"),
                    PageHelpers.Row("Password", _txtPassword, _btnCheck),
                    _lblPasswordStatus,
                    PageHelpers.SectionHeader("Service Account Profile"),
                    _profile),
            };
        }

        public override void OnEnter(WizardContext ctx)
        {
            string account = ctx.DetectedServiceAccount ?? "dbxtune";
            _account = account;
            ctx.Config.ServiceAccount = account;
            ctx.Config.InstallDir     = ctx.DetectedInstallDir ?? Path.Combine(InstallActions.AccountHome(account), "dbxtune_sw");
            ctx.Config.InitCommand    = $"\"{Path.Combine(ctx.Config.InstallDir, "0", "bin", "dbxcentral.bat")}\" --createAppDir";

            var env = ctx.DetectedEnvVars;
            ctx.Config.DbxUserHome   = env.GetValueOrDefault("DBXTUNE_USER_HOME", "");
            ctx.Config.DbxSaveDir    = env.GetValueOrDefault("DBXTUNE_CENTRAL_SAVE_DIR", "");
            ctx.Config.DbxReportsDir = env.GetValueOrDefault("DBXTUNE_CENTRAL_REPORTS_DIR", "");
            ctx.Config.DbxLogDir     = env.GetValueOrDefault("DBXTUNE_CENTRAL_LOG_DIR", "");
            ctx.Config.DbxConfDir    = env.GetValueOrDefault("DBXTUNE_CENTRAL_CONF_DIR", "");
            ctx.Config.DbxInfoDir    = env.GetValueOrDefault("DBXTUNE_CENTRAL_INFO_DIR", "");

            _summary.Text =
                $"Service account   : {ctx.Config.ServiceAccount}\n" +
                $"Install directory : {ctx.Config.InstallDir}\n" +
                $"DbxUserHome       : {ctx.Config.DbxUserHome}\n" +
                $"Package source    : {(ctx.Config.DownloadZip ? ctx.Config.ZipUrl : ctx.Config.ZipLocalPath)}\n\n" +
                "Need to change the account, location, or DBMS selection instead? Cancel and choose Install.";

            // Repair step 1 starts here: show right away whether the profile folder is in order (read only).
            _ = _profile.CheckAsync(account);

            // Extraction/upgrade steps (extract package, init DbxTune home, etc.) run as the
            // service account, so we need its password. Rather than re-asking, try reading it
            // straight from the LSA secret Windows Service Control Manager already stores for
            // DbxStarterService ("sc config ... password=" writes it there) — the same secret
            // SCM itself decrypts every time the service starts.
            _isGmsa = account.TrimEnd().EndsWith('$');
            if (_isGmsa)
            {
                _lblPasswordStatus.Text = "gMSA account — Active Directory manages the password automatically; no password needed.";
                _txtPassword.IsEnabled = false;
                _btnCheck.IsEnabled = false;
                SetNextEnabled(true);
                return;
            }

            string? extracted = LsaPrivileges.GetServicePassword("DbxStarterService");
            if (!string.IsNullOrEmpty(extracted))
            {
                _txtPassword.Password = extracted;
                _lblPasswordStatus.Text = "Password auto-detected from the DbxStarterService configuration.";
                _lblPasswordStatus.SetResourceReference(TextBlock.ForegroundProperty, "GreenBrush");
            }
            else
            {
                _lblPasswordStatus.Text = "Could not auto-detect the password from the service configuration — enter it manually.";
                _lblPasswordStatus.SetResourceReference(TextBlock.ForegroundProperty, "OrangeBrush");
            }
        }

        public override void OnLeave(WizardContext ctx)
        {
            ctx.Config.Password = _txtPassword.Password;
        }

        public override Task<WizardValidationResult> ValidateBeforeNextAsync(WizardContext ctx) =>
            Task.FromResult(!_isGmsa && string.IsNullOrEmpty(_txtPassword.Password)
                ? WizardValidationResult.Fail("Service account password is required to upgrade (couldn't auto-detect it).")
                : WizardValidationResult.Ok());
    }
}

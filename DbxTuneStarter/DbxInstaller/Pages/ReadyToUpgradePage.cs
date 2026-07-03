using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using DbxInstaller.Wizard;

namespace DbxInstaller.Pages
{
    // Recaps the detected existing install (read on the Welcome page) and populates the
    // Config fields that Upgrade mode skips asking for (Service Account / Install Location /
    // DBMS Selection pages aren't part of the Upgrade flow).
    internal sealed class ReadyToUpgradePage : WizardPage
    {
        public override string StepTitle => "Ready to Upgrade";

        private readonly TextBox _summary = new()
        {
            Style = (Style)Application.Current.Resources["SelectableText"],
            FontFamily = new System.Windows.Media.FontFamily("Consolas"),
        };
        private readonly PasswordBox _txtPassword = new() { Margin = new Thickness(0, 4, 0, 4) };
        private readonly TextBlock _lblPasswordStatus = new() { TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 0, 0, 12) };

        private bool _isGmsa;

        public ReadyToUpgradePage()
        {
            Content = PageHelpers.Stack(
                PageHelpers.Title("Ready to Upgrade"),
                PageHelpers.Subtitle("This will fetch a new package and update the existing DbxTune installation below. " +
                                      "Service account, install location, and DBMS selection are unchanged."),
                _summary,
                PageHelpers.SectionHeader("Service Account Password"),
                PageHelpers.Row("Password", _txtPassword),
                _lblPasswordStatus);
        }

        public override void OnEnter(WizardContext ctx)
        {
            string account = ctx.DetectedServiceAccount ?? "dbxtune";
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

using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Threading;
using DbxInstaller.Wizard;

namespace DbxInstaller.Pages
{
    internal sealed class ServiceAccountPage : WizardPage
    {
        public override string StepTitle => "Service Account";

        private readonly TextBox _txtAccount = PageHelpers.TextBox("dbxtune");
        private readonly PasswordBox _txtPassword = new() { Margin = new Thickness(0, 2, 0, 2) };
        private readonly Button _btnCheck = new() { Content = "Check" };
        private readonly TextBlock _lblCheckStatus = new() { Margin = new Thickness(0, 4, 0, 0), TextWrapping = TextWrapping.Wrap };
        private readonly TextBlock _lblGmsaNote = new() { TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 8, 0, 0) };

        public ServiceAccountPage()
        {
            _txtAccount.TextChanged += (_, _) => UpdateGmsaNote();
            _btnCheck.Click += async (_, _) =>
            {
                _lblCheckStatus.Text = "Checking…";
                _lblCheckStatus.SetResourceReference(TextBlock.ForegroundProperty, "FgDimBrush");
                _btnCheck.IsEnabled = false;
                // LogonUser against a domain controller can take a noticeable moment — let the
                // "Checking…" text actually paint before the blocking call runs.
                await Dispatcher.Yield(DispatcherPriority.Background);

                var (outcome, message) = InstallActions.TryVerifyPassword(_txtAccount.Text.Trim(), _txtPassword.Password);
                _lblCheckStatus.Text = message;
                string brush = outcome switch
                {
                    InstallActions.VerifyOutcome.Verified => "GreenBrush",
                    InstallActions.VerifyOutcome.Info     => "OrangeBrush",
                    _                                      => "RedBrush",
                };
                _lblCheckStatus.SetResourceReference(TextBlock.ForegroundProperty, brush);
                _btnCheck.IsEnabled = true;
            };

            Content = PageHelpers.Stack(
                PageHelpers.Title("Service Account"),
                PageHelpers.Subtitle("DbxTune and DbxStarterService run under this Windows account. Use a local account name, " +
                                      "a domain account (DOMAIN\\user), or a gMSA (account name ending with '$')."),
                PageHelpers.Row("Account name", _txtAccount),
                PageHelpers.Row("Password", _txtPassword, _btnCheck),
                _lblCheckStatus,
                _lblGmsaNote);

            UpdateGmsaNote();
        }

        private bool IsGmsa => _txtAccount.Text.Trim().EndsWith('$');

        private void UpdateGmsaNote()
        {
            _txtPassword.IsEnabled = !IsGmsa;
            _btnCheck.IsEnabled = !IsGmsa;
            _lblGmsaNote.Text = IsGmsa
                ? "gMSA account detected — Active Directory manages the password automatically; leave the password blank."
                : "";
        }

        public override void OnLeave(WizardContext ctx)
        {
            ctx.Config.ServiceAccount = _txtAccount.Text.Trim();
            ctx.Config.Password       = _txtPassword.Password;
        }

        public override Task<WizardValidationResult> ValidateBeforeNextAsync(WizardContext ctx)
        {
            if (string.IsNullOrWhiteSpace(_txtAccount.Text))
                return Task.FromResult(WizardValidationResult.Fail("Service account name is required."));
            if (!IsGmsa && string.IsNullOrWhiteSpace(_txtPassword.Password))
                return Task.FromResult(WizardValidationResult.Fail(
                    "Password is required for local and domain accounts.\nLeave blank only for gMSA accounts (name ending with '$')."));
            return Task.FromResult(WizardValidationResult.Ok());
        }
    }
}

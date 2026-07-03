using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using DbxInstaller.Wizard;

namespace DbxInstaller.Uninstall
{
    internal sealed class RemovalOptionsPage : WizardPage
    {
        public override string StepTitle => "Removal Options";

        private readonly CheckBox _chkUser = new() { Content = "Delete User Account and Profile", Margin = new Thickness(0, 6, 0, 6) };
        private readonly CheckBox _chkData = new() { Content = "Delete dbxtune user directory", Margin = new Thickness(0, 6, 0, 6) };
        private readonly CheckBox _chkSw   = new() { Content = "Delete software directory", Margin = new Thickness(0, 6, 0, 6) };
        private readonly TextBlock _warning = new()
        {
            Text = "Deleting the account, profile, or directories cannot be undone.",
            TextWrapping = TextWrapping.Wrap,
            Margin = new Thickness(0, 12, 0, 0),
        };

        public RemovalOptionsPage()
        {
            _warning.SetResourceReference(TextBlock.ForegroundProperty, "OrangeBrush");
            Content = PageHelpers.Stack(
                PageHelpers.Title("Removal Options"),
                PageHelpers.Subtitle("The service, its registration, and firewall rules are always removed. Choose what else to remove."),
                _chkUser, _chkData, _chkSw, _warning);
        }

        public override void OnEnter(WizardContext ctx)
        {
            _chkUser.IsChecked = false;   // never auto-checked — user must opt in explicitly
            _chkUser.IsEnabled = ctx.DetectedIsLocalAccount;
            _chkData.IsChecked = ctx.DetectedDataDirs.Count > 0;
            _chkSw.IsChecked   = ctx.DetectedInstallDir != null && System.IO.Directory.Exists(ctx.DetectedInstallDir);
        }

        public override void OnLeave(WizardContext ctx)
        {
            ctx.RemoveAccount  = _chkUser.IsChecked == true;
            ctx.RemoveData     = _chkData.IsChecked == true;
            ctx.RemoveSoftware = _chkSw.IsChecked == true;
        }

        public override Task<WizardValidationResult> ValidateBeforeNextAsync(WizardContext ctx) =>
            Task.FromResult(WizardValidationResult.Ok());
    }
}

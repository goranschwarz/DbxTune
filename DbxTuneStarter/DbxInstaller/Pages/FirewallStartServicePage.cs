using System.Linq;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using DbxInstaller.Wizard;

namespace DbxInstaller.Pages
{
    internal sealed class FirewallStartServicePage : WizardPage
    {
        public override string StepTitle => "Firewall + Start Service";

        private readonly TextBlock _firewallStatus = new() { TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 0, 0, 8) };
        private readonly TextBox _firewallLog = new()
        {
            Style = (Style)Application.Current.Resources["SelectableText"],
            Margin = new Thickness(0, 0, 0, 16),
            FontFamily = new System.Windows.Media.FontFamily("Consolas"),
        };
        private readonly CheckBox _chkStart  = new() { Content = "Start DbxStarterService now", IsChecked = true };
        private readonly CheckBox _chkLaunch = new() { Content = "Launch DbxStarterClient now", IsChecked = true };
        private readonly TextBox _resultLog = new()
        {
            Style = (Style)Application.Current.Resources["SelectableText"],
            Margin = new Thickness(0, 12, 0, 0),
            FontFamily = new System.Windows.Media.FontFamily("Consolas"),
        };

        private bool _firewallDone;

        public FirewallStartServicePage()
        {
            Content = PageHelpers.Stack(
                PageHelpers.Title("Firewall + Start Service"),
                PageHelpers.Subtitle("Windows Firewall rules are added automatically. Choose whether to start the service now."),
                _firewallStatus,
                _firewallLog,
                _chkStart, _chkLaunch,
                _resultLog);
        }

        public override void OnEnter(WizardContext ctx)
        {
            if (_firewallDone) return;
            _firewallDone = true;
            _firewallStatus.Text = "Configuring firewall rules…";

            var step = InstallActions.Steps.First(s => s.Name == "Configure firewall");
            var lines = new System.Text.StringBuilder();
            _ = step.Run!(ctx.Config, msg => lines.AppendLine(msg)).ContinueWith(t =>
            {
                Dispatcher.Invoke(() =>
                {
                    _firewallStatus.Text = t.Result ? "Firewall rules configured — see below for what was added or skipped." : "Firewall configuration reported a problem — see below.";
                    _firewallStatus.SetResourceReference(TextBlock.ForegroundProperty, t.Result ? "GreenBrush" : "OrangeBrush");
                    _firewallLog.Text = lines.ToString().TrimEnd();
                });
            });
        }

        public override async Task<WizardValidationResult> ValidateBeforeNextAsync(WizardContext ctx)
        {
            var lines = new System.Text.StringBuilder();
            await InstallActions.StartServiceCore(ctx.Config, msg => lines.AppendLine(msg),
                _chkStart.IsChecked == true, _chkLaunch.IsChecked == true);
            _resultLog.Text = lines.ToString();
            return WizardValidationResult.Ok();
        }
    }
}

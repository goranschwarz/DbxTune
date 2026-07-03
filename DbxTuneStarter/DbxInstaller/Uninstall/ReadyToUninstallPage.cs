using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using DbxInstaller.Wizard;

namespace DbxInstaller.Uninstall
{
    internal sealed class ReadyToUninstallPage : WizardPage
    {
        public override string StepTitle => "Ready to Uninstall";

        private readonly TextBox _summary = new()
        {
            Style = (Style)Application.Current.Resources["SelectableText"],
            FontFamily = new System.Windows.Media.FontFamily("Consolas"),
        };
        private readonly TextBlock _warning = new() { Text = "This cannot be undone.", Margin = new Thickness(0, 12, 0, 0), FontWeight = FontWeights.SemiBold };

        public ReadyToUninstallPage()
        {
            _warning.SetResourceReference(TextBlock.ForegroundProperty, "RedBrush");
            Content = PageHelpers.Stack(
                PageHelpers.Title("Ready to Uninstall"),
                PageHelpers.Subtitle("The following actions will be performed:"),
                _summary, _warning);
        }

        public override void OnEnter(WizardContext ctx)
        {
            var lines = new System.Text.StringBuilder();
            lines.AppendLine("- Stop DbxStarterClient (if running)");
            lines.AppendLine("- Stop DbxStarterService (if running)");
            lines.AppendLine("- Delete DbxStarterService registration");
            lines.AppendLine("- Remove DbxTune firewall rules");
            if (ctx.RemoveAccount)
                lines.AppendLine($"- Delete user account '{ctx.DetectedServiceAccount}' and profile directory");
            if (ctx.RemoveData && ctx.DetectedDataDirs.Count > 0)
            {
                lines.AppendLine("- Delete data directories:");
                foreach (var d in ctx.DetectedDataDirs) lines.AppendLine($"    {d}");
            }
            if (ctx.RemoveSoftware && ctx.DetectedInstallDir != null)
                lines.AppendLine($"- Delete software directory: {ctx.DetectedInstallDir}");
            _summary.Text = lines.ToString();
        }

        public override Task<WizardValidationResult> ValidateBeforeNextAsync(WizardContext ctx) =>
            Task.FromResult(WizardValidationResult.Ok());
    }
}

using System.Collections.Generic;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using DbxInstaller.Wizard;

namespace DbxInstaller.Uninstall
{
    // Read-only recap of what the Welcome page's detection already found (GetServiceAccount,
    // GetProfileDir, ReadUserEnvVars, BuildDataDirList) — mirrors UninstallForm.Discover()'s info labels.
    internal sealed class DiscoveryPage : WizardPage
    {
        public override string StepTitle => "Discovery";

        private readonly TextBox _summary = new()
        {
            Style = (Style)Application.Current.Resources["SelectableText"],
            FontFamily = new System.Windows.Media.FontFamily("Consolas"),
        };

        public DiscoveryPage()
        {
            Content = PageHelpers.Stack(
                PageHelpers.Title("Discovery"),
                PageHelpers.Subtitle("What this wizard found on this machine."),
                _summary);
        }

        public override void OnEnter(WizardContext ctx)
        {
            if (!ctx.ExistingInstallDetected)
            {
                _summary.Text = "No existing DbxTune installation was found on this machine.";
                SetNextEnabled(true);
                return;
            }

            _summary.Text =
                $"Service account   : {ctx.DetectedServiceAccount}\n" +
                $"Profile directory : {ctx.DetectedProfileDir ?? "(not found)"}\n" +
                $"Install directory : {ctx.DetectedInstallDir ?? "(not found)"}\n" +
                $"User home         : {ctx.DetectedEnvVars.GetValueOrDefault("DBXTUNE_USER_HOME", "(not found)")}\n" +
                $"Data directories  : {(ctx.DetectedDataDirs.Count == 0 ? "(none found)" : ctx.DetectedDataDirs.Count + " found")}";
            SetNextEnabled(true);
        }

        public override Task<WizardValidationResult> ValidateBeforeNextAsync(WizardContext ctx) =>
            Task.FromResult(WizardValidationResult.Ok());
    }
}

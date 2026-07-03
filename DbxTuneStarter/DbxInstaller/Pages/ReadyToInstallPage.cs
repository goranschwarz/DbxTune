using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using DbxInstaller.Wizard;

namespace DbxInstaller.Pages
{
    internal sealed class ReadyToInstallPage : WizardPage
    {
        public override string StepTitle => "Ready to Install";

        private readonly TextBox _summary = new()
        {
            Style = (Style)Application.Current.Resources["SelectableText"],
            FontFamily = new System.Windows.Media.FontFamily("Consolas"),
        };

        private readonly TextBlock _afterInstallNote = PageHelpers.Dim(
            "After installation finishes, the wizard will walk you through the remaining configuration steps — " +
            "connecting your selected database servers, OS monitoring setup, DbxCentral web settings, and " +
            "configuring the firewall and starting the service.");

        public ReadyToInstallPage()
        {
            _afterInstallNote.Margin = new Thickness(0, 16, 0, 0);

            Content = PageHelpers.Stack(
                PageHelpers.Title("Ready to Install"),
                PageHelpers.Subtitle("Review the settings below, then click Next to begin. Use Back to change anything."),
                _summary,
                _afterInstallNote);
        }

        public override void OnEnter(WizardContext ctx)
        {
            var c = ctx.Config;
            _summary.Text =
                $"Service account   : {c.ServiceAccount}\n" +
                $"Install directory : {c.InstallDir}\n" +
                $"DbxUserHome       : {c.DbxUserHome}\n" +
                $"Package source    : {(c.DownloadZip ? c.ZipUrl : c.ZipLocalPath)}\n" +
                $"Java executable   : {c.JavaExe}\n" +
                $"Web UI            : {(c.WebPort < 0 ? "disabled" : $"port {c.WebPort}, bind {c.WebBind}")}\n" +
                $"Manage Central    : {c.ManageDbxCentral}\n" +
                $"DBMS selected     : {(c.SelectedDbms.Count > 0 ? string.Join(", ", c.SelectedDbms) : "(none)")}";
        }

        public override Task<WizardValidationResult> ValidateBeforeNextAsync(WizardContext ctx)
        {
            // Final cheap re-check — port may have gone stale since the DbxStarter Web UI Config page.
            if (ctx.Config.WebPort >= 0)
            {
                var status = InstallActions.GetPortStatus(ctx.Config.WebPort, ctx.Config.WebBind);
                if (status == InstallActions.PortStatus.InUseByOther)
                    return Task.FromResult(WizardValidationResult.Fail(
                        $"Port {ctx.Config.WebPort} is now in use by another process — go back to DbxStarter Web UI Config and change it."));
            }
            return Task.FromResult(WizardValidationResult.Ok());
        }
    }
}

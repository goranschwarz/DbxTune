using System.IO;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using DbxInstaller.Wizard;
using Microsoft.Win32;

namespace DbxInstaller.Pages
{
    internal sealed class PackageSourcePage : WizardPage
    {
        public override string StepTitle => "Package Source";

        private readonly RadioButton _rbDownload = new() { GroupName = "pkg", IsChecked = true, Content = "Download the latest release" };
        private readonly RadioButton _rbLocal     = new() { GroupName = "pkg", Content = "Use a local ZIP file", Margin = new Thickness(0, 16, 0, 4) };
        private readonly TextBox _txtUrl   = PageHelpers.TextBox("https://sourceforge.net/projects/asetune/files/latest/download");
        private readonly TextBox _txtLocal = PageHelpers.TextBox();
        private readonly Button  _btnBrowse = new() { Content = "Browse…" };
        private readonly TextBlock _lblStatus = new() { Margin = new Thickness(0, 8, 0, 0), TextWrapping = TextWrapping.Wrap };

        public PackageSourcePage()
        {
            _rbDownload.Checked += (_, _) => UpdateVisibility();
            _rbLocal.Checked    += (_, _) => UpdateVisibility();
            _btnBrowse.Click    += (_, _) =>
            {
                var dlg = new OpenFileDialog { Filter = "ZIP files (*.zip)|*.zip|All files (*.*)|*.*" };
                if (dlg.ShowDialog() == true) _txtLocal.Text = dlg.FileName;
            };

            Content = PageHelpers.Stack(
                PageHelpers.Title("Package Source"),
                PageHelpers.Subtitle("Choose where the DbxTune software package comes from. This can be changed later by re-running the wizard."),
                _rbDownload,
                PageHelpers.Row("Download URL", _txtUrl),
                _rbLocal,
                PageHelpers.Row("Local ZIP path", _txtLocal, _btnBrowse),
                _lblStatus);

            UpdateVisibility();
        }

        private void UpdateVisibility()
        {
            bool dl = _rbDownload.IsChecked == true;
            _txtUrl.IsEnabled    = dl;
            _txtLocal.IsEnabled  = !dl;
            _btnBrowse.IsEnabled = !dl;
            _lblStatus.Text = "";
        }

        public override void OnLeave(WizardContext ctx)
        {
            ctx.Config.DownloadZip  = _rbDownload.IsChecked == true;
            ctx.Config.ZipUrl       = _txtUrl.Text.Trim();
            ctx.Config.ZipLocalPath = _txtLocal.Text.Trim();
        }

        public override Task<WizardValidationResult> ValidateBeforeNextAsync(WizardContext ctx)
        {
            if (_rbDownload.IsChecked == true)
            {
                if (string.IsNullOrWhiteSpace(_txtUrl.Text))
                    return Task.FromResult(WizardValidationResult.Fail("Download URL is required."));
            }
            else
            {
                if (!File.Exists(_txtLocal.Text.Trim()))
                    return Task.FromResult(WizardValidationResult.Fail("Local ZIP file not found."));
            }
            return Task.FromResult(WizardValidationResult.Ok());
        }
    }
}

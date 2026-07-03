using System;
using System.IO;
using System.Text.Json;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using DbxInstaller.Wizard;

namespace DbxInstaller.Pages
{
    internal sealed class DbxStarterWebConfigPage : WizardPage
    {
        public override string StepTitle => "DbxStarter Web UI Config";

        private readonly CheckBox _chkWebUi  = new() { Content = "Enable DbxStarter web UI", IsChecked = true };
        private readonly TextBox  _txtPort   = PageHelpers.TextBox("8055");
        private readonly RadioButton _rbLocal = new() { GroupName = "bind", IsChecked = true, Content = "localhost only" };
        private readonly RadioButton _rbAny   = new() { GroupName = "bind", Content = "all network interfaces" };
        private readonly TextBlock _lblStatus = new() { Margin = new Thickness(0, 8, 0, 0) };

        private string? _lastConfDir;

        public DbxStarterWebConfigPage()
        {
            _chkWebUi.Checked   += (_, _) => { UpdateEnabled(); CheckPort(); };
            _chkWebUi.Unchecked += (_, _) => { UpdateEnabled(); CheckPort(); };
            _txtPort.TextChanged += (_, _) => CheckPort();
            _rbLocal.Checked += (_, _) => CheckPort();
            _rbAny.Checked   += (_, _) => CheckPort();

            Content = PageHelpers.Stack(
                PageHelpers.Title("DbxStarter Web UI Config"),
                PageHelpers.Subtitle("DbxStarterService can host a small web UI for viewing live logs and managing DbxTune Central."),
                _chkWebUi,
                PageHelpers.Row("Port", _txtPort),
                _rbLocal, _rbAny,
                _lblStatus);

            UpdateEnabled();
        }

        private void UpdateEnabled()
        {
            bool on = _chkWebUi.IsChecked == true;
            _txtPort.IsEnabled = on;
            _rbLocal.IsEnabled = on;
            _rbAny.IsEnabled   = on;
        }

        public override void OnEnter(WizardContext ctx)
        {
            string confDir = ctx.Config.DbxConfDir;
            if (confDir == _lastConfDir) { CheckPort(); return; }
            _lastConfDir = confDir;
            TryLoadExistingWebSettings(confDir);
            CheckPort();
        }

        private void TryLoadExistingWebSettings(string confDir)
        {
            if (string.IsNullOrWhiteSpace(confDir)) return;
            string jsonPath = Path.Combine(confDir, "DbxStarterService.json");
            if (!File.Exists(jsonPath)) return;

            try
            {
                using var doc = JsonDocument.Parse(File.ReadAllText(jsonPath));
                if (!doc.RootElement.TryGetProperty("DbxStarter", out var section)) return;

                if (section.TryGetProperty("WebPort", out var portEl) && portEl.TryGetInt32(out int port))
                {
                    if (port < 0) { _chkWebUi.IsChecked = false; }
                    else { _chkWebUi.IsChecked = true; _txtPort.Text = port.ToString(); }
                }
                if (section.TryGetProperty("WebBind", out var bindEl))
                {
                    string bind = bindEl.GetString() ?? "localhost";
                    bool isAny  = bind.Equals("*", StringComparison.OrdinalIgnoreCase) || bind.Equals("any", StringComparison.OrdinalIgnoreCase);
                    _rbLocal.IsChecked = !isAny;
                    _rbAny.IsChecked   = isAny;
                }
            }
            catch { /* ignore missing/malformed file */ }
            UpdateEnabled();
        }

        private bool _portOk = true;

        private void CheckPort()
        {
            if (_chkWebUi.IsChecked != true)
            {
                _lblStatus.Text = "";
                _portOk = true;
                SetNextEnabled(true);
                return;
            }
            if (!int.TryParse(_txtPort.Text.Trim(), out int port))
            {
                _lblStatus.Text = "Enter a valid port number.";
                _lblStatus.SetResourceReference(TextBlock.ForegroundProperty, "RedBrush");
                _portOk = false;
                SetNextEnabled(false);
                return;
            }

            string webBind = _rbLocal.IsChecked == true ? "localhost" : "*";
            var status = InstallActions.GetPortStatus(port, webBind);
            switch (status)
            {
                case InstallActions.PortStatus.Free:
                    _lblStatus.Text = "Port is free.";
                    _lblStatus.SetResourceReference(TextBlock.ForegroundProperty, "GreenBrush");
                    _portOk = true;
                    break;
                case InstallActions.PortStatus.OwnedByDbxStarter:
                    _lblStatus.Text = "Held by DbxStarterService — it will be stopped and restarted during install.";
                    _lblStatus.SetResourceReference(TextBlock.ForegroundProperty, "OrangeBrush");
                    _portOk = true;
                    break;
                default:
                    _lblStatus.Text = $"Port {port} is already in use by another process — choose a different port.";
                    _lblStatus.SetResourceReference(TextBlock.ForegroundProperty, "RedBrush");
                    _portOk = false;
                    break;
            }
            SetNextEnabled(_portOk);
        }

        public override void OnLeave(WizardContext ctx)
        {
            ctx.Config.WebPort = _chkWebUi.IsChecked == true && int.TryParse(_txtPort.Text.Trim(), out int p) ? p : -1;
            ctx.Config.WebBind = _rbLocal.IsChecked == true ? "localhost" : "*";
        }

        public override Task<WizardValidationResult> ValidateBeforeNextAsync(WizardContext ctx)
        {
            CheckPort();
            return Task.FromResult(_portOk ? WizardValidationResult.Ok() : WizardValidationResult.Fail("Resolve the port conflict before continuing."));
        }
    }
}

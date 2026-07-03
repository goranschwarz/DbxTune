using System;
using System.Windows;
using System.Windows.Controls;
using DbxInstaller.Wizard;

namespace DbxInstaller.Pages
{
    internal sealed class FinishPage : WizardPage
    {
        public override string StepTitle => "Finish";
        public override bool IsFinishPage => true;

        private readonly StackPanel _panel = new();
        private TextBox? _details;

        public FinishPage()
        {
            Content = _panel;
            _panel.Children.Add(PageHelpers.Title("Setup Complete"));
            _panel.Children.Add(PageHelpers.Subtitle("DbxTune has been installed."));
        }

        public override void OnEnter(WizardContext ctx)
        {
            string exePath = Environment.ProcessPath ?? "DbxInstaller.exe";
            var lines = new System.Text.StringBuilder();

            if (ctx.Config.WebPort >= 0)
            {
                bool anyNet = ctx.Config.WebBind.Equals("*", StringComparison.OrdinalIgnoreCase);
                string host = anyNet ? Environment.MachineName : "localhost";
                lines.AppendLine($"Web UI: http://{host}:{ctx.Config.WebPort}");
            }
            else
            {
                lines.AppendLine("Web UI is disabled.");
            }

            if (!string.IsNullOrEmpty(ctx.LogFilePath))
            {
                lines.AppendLine();
                lines.AppendLine($"Log file: {ctx.LogFilePath}");
            }

            lines.AppendLine();
            lines.AppendLine($"To uninstall, run as Administrator:  \"{exePath}\" --remove");
            lines.AppendLine("(or re-run this wizard and choose Remove on the Welcome page)");

            if (_details == null)
            {
                _details = PageHelpers.SelectableText(lines.ToString());
                _panel.Children.Add(_details);
            }
            else
            {
                _details.Text = lines.ToString();
            }
        }
    }
}

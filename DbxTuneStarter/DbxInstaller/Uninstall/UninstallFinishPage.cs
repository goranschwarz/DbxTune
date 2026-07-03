using System.Windows;
using System.Windows.Controls;
using DbxInstaller.Wizard;

namespace DbxInstaller.Uninstall
{
    internal sealed class UninstallFinishPage : WizardPage
    {
        public override string StepTitle => "Finish";
        public override bool IsFinishPage => true;

        private readonly StackPanel _panel = new();
        private TextBox? _logPath;

        public UninstallFinishPage()
        {
            Content = _panel;
            _panel.Children.Add(PageHelpers.Title("Uninstall Complete"));
            _panel.Children.Add(PageHelpers.Subtitle("DbxTune has been removed from this machine. Review the log for any items that could not be deleted."));
        }

        public override void OnEnter(WizardContext ctx)
        {
            if (string.IsNullOrEmpty(ctx.LogFilePath)) return;

            if (_logPath == null)
            {
                _logPath = PageHelpers.SelectableText("Log file: " + ctx.LogFilePath);
                _logPath.Margin = new Thickness(0, 12, 0, 0);
                _panel.Children.Add(_logPath);
            }
            else
            {
                _logPath.Text = "Log file: " + ctx.LogFilePath;
            }
        }
    }
}

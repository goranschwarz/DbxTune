using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Controls;
using DbxInstaller.Wizard;

namespace DbxInstaller.Pages
{
    internal sealed class DbmsSelectionPage : WizardPage
    {
        public override string StepTitle => "DBMS Selection";

        private readonly Dictionary<string, CheckBox> _checks = new();

        public DbmsSelectionPage()
        {
            var stack = new StackPanel();
            stack.Children.Add(PageHelpers.Title("Database Servers"));
            stack.Children.Add(PageHelpers.Subtitle(
                "Select the database server types you want to monitor. You'll configure connection details " +
                "for each one after the software is installed."));

            foreach (var profile in InstallActions.DbmsProfiles)
            {
                var chk = new CheckBox { Content = profile.DisplayName, ToolTip = profile.Tooltip, Margin = new System.Windows.Thickness(0, 2, 0, 2) };
                _checks[profile.DisplayName] = chk;
                stack.Children.Add(chk);
            }
            Content = stack;
        }

        public override void OnLeave(WizardContext ctx)
        {
            ctx.Config.SelectedDbms = _checks.Where(kv => kv.Value.IsChecked == true).Select(kv => kv.Key).ToList();
        }

        public override void OnEnter(WizardContext ctx)
        {
            foreach (var (name, chk) in _checks)
                chk.IsChecked = ctx.Config.SelectedDbms.Contains(name);
        }

        public override Task<WizardValidationResult> ValidateBeforeNextAsync(WizardContext ctx) =>
            Task.FromResult(WizardValidationResult.Ok());
    }
}

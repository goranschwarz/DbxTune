using System.IO;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using DbxInstaller.Wizard;

namespace DbxInstaller.Pages
{
    internal sealed class InstallLocationPage : WizardPage
    {
        public override string StepTitle => "Install Location";

        private readonly TextBox _txtInstallDir    = PageHelpers.TextBox();
        private readonly TextBox _txtDbxUserHome   = PageHelpers.TextBox();
        private readonly TextBox _txtDbxSaveDir    = PageHelpers.TextBox();
        private readonly TextBox _txtDbxReportsDir = PageHelpers.TextBox();
        private readonly TextBox _txtDbxLogDir     = PageHelpers.TextBox();
        private readonly TextBox _txtDbxConfDir    = PageHelpers.TextBox();
        private readonly TextBox _txtDbxInfoDir    = PageHelpers.TextBox();

        private string _initCommand = "";
        private string? _lastAccount;

        public InstallLocationPage()
        {
            _txtDbxUserHome.TextChanged += (_, _) => RederiveSubDirs();

            // All directory rows share one Grid (fixed label/field/button/hint columns) instead
            // of each PageHelpers.Row being its own independent Grid — otherwise each row's Auto
            // "button + env var hint" column sizes to that row's own (very differently-lengthed)
            // hint text, which shrinks or grows that row's field column and leaves the "..."
            // buttons misaligned from row to row.
            var grid = new Grid();
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(160) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });

            int row = 0;
            AddHeaderRow(grid, ref row, PageHelpers.SectionHeader("Software"));
            AddDirRow(grid, ref row, "Install directory", _txtInstallDir, @"DBXTUNE_HOME = this path\0");
            AddHeaderRow(grid, ref row, PageHelpers.SectionHeader("DbxTune Directories (DBXTUNE_*)"));
            AddDirRow(grid, ref row, "User home",   _txtDbxUserHome,   "DBXTUNE_USER_HOME");
            AddDirRow(grid, ref row, "Save dir",    _txtDbxSaveDir,    "DBXTUNE_CENTRAL_SAVE_DIR");
            AddDirRow(grid, ref row, "Reports dir", _txtDbxReportsDir, "DBXTUNE_CENTRAL_REPORTS_DIR");
            AddDirRow(grid, ref row, "Log dir",     _txtDbxLogDir,     "DBXTUNE_CENTRAL_LOG_DIR");
            AddDirRow(grid, ref row, "Conf dir",    _txtDbxConfDir,    "DBXTUNE_CENTRAL_CONF_DIR");
            AddDirRow(grid, ref row, "Info dir",    _txtDbxInfoDir,    "DBXTUNE_CENTRAL_INFO_DIR");

            Content = PageHelpers.Stack(
                PageHelpers.Title("Install Location"),
                PageHelpers.Subtitle("Where DbxTune is installed and where its data lives. These default from the " +
                                      "service account name and are pre-filled from an existing installation, if found."),
                grid);
        }

        private static void AddHeaderRow(Grid grid, ref int row, TextBlock header)
        {
            grid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            Grid.SetRow(header, row);
            Grid.SetColumn(header, 0);
            Grid.SetColumnSpan(header, 4);
            grid.Children.Add(header);
            row++;
        }

        private static void AddDirRow(Grid grid, ref int row, string label, TextBox target, string envVarName)
        {
            grid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });

            var lbl = new TextBlock { Text = label, VerticalAlignment = VerticalAlignment.Center, Margin = new Thickness(0, 4, 0, 4) };
            Grid.SetRow(lbl, row);
            Grid.SetColumn(lbl, 0);
            grid.Children.Add(lbl);

            target.Margin = new Thickness(0, 4, 0, 4);
            Grid.SetRow(target, row);
            Grid.SetColumn(target, 1);
            grid.Children.Add(target);

            var btn = PageHelpers.BrowseFolderButton(target);
            btn.Margin = new Thickness(6, 4, 0, 4);
            btn.VerticalAlignment = VerticalAlignment.Center;
            Grid.SetRow(btn, row);
            Grid.SetColumn(btn, 2);
            grid.Children.Add(btn);

            var hint = EnvVarHint(envVarName);
            Grid.SetRow(hint, row);
            Grid.SetColumn(hint, 3);
            grid.Children.Add(hint);

            row++;
        }

        private static TextBlock EnvVarHint(string name) => new()
        {
            Text = name,
            Style = (Style)Application.Current.Resources["DimText"],
            VerticalAlignment = VerticalAlignment.Center,
            Margin = new Thickness(8, 4, 0, 4),
        };

        private void RederiveSubDirs()
        {
            string home = _txtDbxUserHome.Text.Trim();
            if (string.IsNullOrEmpty(home)) return;
            string dbxBase = Path.Combine(home, "dbxc");
            _txtDbxSaveDir.Text    = Path.Combine(dbxBase, "data");
            _txtDbxReportsDir.Text = Path.Combine(dbxBase, "reports");
            _txtDbxLogDir.Text     = Path.Combine(dbxBase, "log");
            _txtDbxConfDir.Text    = Path.Combine(dbxBase, "conf");
            _txtDbxInfoDir.Text    = Path.Combine(dbxBase, "info");
        }

        public override void OnEnter(WizardContext ctx)
        {
            string account = ctx.Config.ServiceAccount;
            if (account == _lastAccount) return;   // account unchanged since last visit — keep user edits
            _lastAccount = account;

            var existing = InstallActions.ReadUserEnvVars(account);
            string home  = InstallActions.AccountHome(account);
            string swDir = Path.Combine(home, "dbxtune_sw");
            _initCommand = $"\"{Path.Combine(swDir, "0", "bin", "dbxcentral.bat")}\" --createAppDir";

            _txtInstallDir.Text = swDir;

            string dbxHome = existing.TryGetValue("DBXTUNE_USER_HOME", out var h) && !string.IsNullOrEmpty(h)
                ? h : Path.Combine(home, ".dbxtune");
            _txtDbxUserHome.Text = dbxHome;   // triggers RederiveSubDirs with the base defaults

            string dbxBase = Path.Combine(dbxHome, "dbxc");
            SetIfPresent(_txtDbxSaveDir,    existing, "DBXTUNE_CENTRAL_SAVE_DIR",    Path.Combine(dbxBase, "data"));
            SetIfPresent(_txtDbxReportsDir, existing, "DBXTUNE_CENTRAL_REPORTS_DIR", Path.Combine(dbxBase, "reports"));
            SetIfPresent(_txtDbxLogDir,     existing, "DBXTUNE_CENTRAL_LOG_DIR",     Path.Combine(dbxBase, "log"));
            SetIfPresent(_txtDbxConfDir,    existing, "DBXTUNE_CENTRAL_CONF_DIR",    Path.Combine(dbxBase, "conf"));
            SetIfPresent(_txtDbxInfoDir,    existing, "DBXTUNE_CENTRAL_INFO_DIR",    Path.Combine(dbxBase, "info"));
        }

        private static void SetIfPresent(TextBox box,
            System.Collections.Generic.Dictionary<string, string> hive, string key, string fallback) =>
            box.Text = hive.TryGetValue(key, out var v) && !string.IsNullOrEmpty(v) ? v : fallback;

        public override void OnLeave(WizardContext ctx)
        {
            ctx.Config.InstallDir    = _txtInstallDir.Text.Trim();
            ctx.Config.InitCommand   = _initCommand;
            ctx.Config.DbxUserHome   = _txtDbxUserHome.Text.Trim();
            ctx.Config.DbxSaveDir    = _txtDbxSaveDir.Text.Trim();
            ctx.Config.DbxReportsDir = _txtDbxReportsDir.Text.Trim();
            ctx.Config.DbxLogDir     = _txtDbxLogDir.Text.Trim();
            ctx.Config.DbxConfDir    = _txtDbxConfDir.Text.Trim();
            ctx.Config.DbxInfoDir    = _txtDbxInfoDir.Text.Trim();
        }

        public override Task<WizardValidationResult> ValidateBeforeNextAsync(WizardContext ctx) =>
            Task.FromResult(string.IsNullOrWhiteSpace(_txtInstallDir.Text)
                ? WizardValidationResult.Fail("Install directory is required.")
                : WizardValidationResult.Ok());
    }
}

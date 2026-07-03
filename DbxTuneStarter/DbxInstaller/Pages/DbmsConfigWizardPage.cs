using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using DbxInstaller.Wizard;

namespace DbxInstaller.Pages
{
    // Replaces the old ConfigureDbmsServers modal Prompt()/Notepad flow with inline editing —
    // the page itself owns Next/Back per sub-step; the wizard's own Next only becomes
    // enabled once every selected DBMS has been walked through.
    internal sealed class DbmsConfigWizardPage : WizardPage
    {
        public override string StepTitle => "DBMS Configuration";

        private List<DbmsProfile> _profiles = new();
        private InstallConfig? _config;
        private WizardContext? _ctx;
        private int _profileIndex;
        private int _subStep; // 1..4
        private readonly ContentControl _stepHost = new();
        private readonly TextBlock _progressLabel = new() { Margin = new Thickness(0, 0, 0, 12) };

        public DbmsConfigWizardPage()
        {
            var root = new Grid();
            root.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            root.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });

            var header = PageHelpers.Stack(
                PageHelpers.Title("DBMS Configuration"),
                PageHelpers.Subtitle("Configure a connection for each selected database server."),
                _progressLabel);
            Grid.SetRow(header, 0);
            root.Children.Add(header);

            Grid.SetRow(_stepHost, 1);
            root.Children.Add(_stepHost);

            Content = root;
        }

        public override void OnEnter(WizardContext ctx)
        {
            _config = ctx.Config;
            _ctx = ctx;
            _profiles = InstallActions.GetSelectedDbmsProfiles(ctx.Config);
            _profileIndex = 0;
            _subStep = 1;
            RenderStep();
        }

        public override void OnLeave(WizardContext ctx) { }

        private bool AllDone => _profileIndex >= _profiles.Count;
        private bool AtVeryFirstSubStep => _profileIndex == 0 && _subStep == 1;

        private void Advance()
        {
            _subStep++;
            if (_subStep > 4) { _subStep = 1; _profileIndex++; }
            RenderStep();
        }

        // Internal Back — steps back within this page's own sub-step sequence (cfgWizard →
        // start script → SERVER_LIST → setup instructions, and across DBMS profiles). This is
        // separate from the wizard's own "< Back" button, which only moves between whole pages.
        private void GoBack()
        {
            if (AtVeryFirstSubStep) return;
            _subStep--;
            if (_subStep < 1) { _profileIndex--; _subStep = 4; }
            RenderStep();
        }

        private void RenderStep()
        {
            if (_profiles.Count == 0)
            {
                _progressLabel.Text = "No DBMS servers were selected — nothing to configure.";
                _stepHost.Content = null;
                SetNextEnabled(true);
                return;
            }

            if (AllDone)
            {
                _progressLabel.Text = $"All {_profiles.Count} selected server(s) configured.";
                _stepHost.Content = null;
                SetNextEnabled(true);
                return;
            }

            SetNextEnabled(false);
            var profile = _profiles[_profileIndex];
            _progressLabel.Text = $"[{_profileIndex + 1}/{_profiles.Count}] {profile.DisplayName} — step {_subStep}/4";

            _stepHost.Content = _subStep switch
            {
                1 => BuildCfgWizardStep(profile),
                2 => BuildFileEditStep(profile.DisplayName, "Collector start script",
                        InstallActions.GetStartScriptPath(profile, _config!),
                        "This start script is called for every server of this DBMS type.\n" +
                        "It can be customized to local settings and behaviour, for example:\n" +
                        "  • JVM memory usage\n" +
                        "  • Override which OS/DBMS user to use for monitoring\n" +
                        "  • Specify DBMS Configuration file (from the previous step)\n" +
                        "  • Note: Set 'dbmsUser=integratedSecurity' to use Windows Authentication (for SQL Server)\n" +
                        "  • etc."),
                3 => BuildFileEditStep(profile.DisplayName, "SERVER_LIST",
                        InstallActions.GetServerListPath(_config!),
                        "SERVER_LIST contains the list of all DBMS servers this installation should monitor:\n" +
                        "  • At the end of each server row, the previous start_<dbms>tune script is called — " +
                        "or a full command line describing exactly how to start that DBMS monitoring instance.\n" +
                        "  • With '#FORMAT; GROUP/LABEL;' entries you can also describe how the DbxCentral " +
                        "landing page groups and labels these servers.\n\n" +
                        "Add the server entry from the wizard so the collector knows which server to monitor.",
                        // SERVER_LIST has no file extension to infer a lexer from, but it's a
                        // key=value/#-comment format — the "props" lexer at least highlights comments.
                        lexerOverride: "props"),
                4 => BuildSetupInstructionsStep(profile),
                _ => null,
            };
        }

        // Every sub-step (other than the cfgWizard one, see below) is laid out as: Auto
        // (info/buttons) | Star (editor/viewer, fills the rest of the page) | Auto (Save row) |
        // Auto (Back/Next row) so the editor always uses the full remaining height.
        private static Grid NewStepGrid(params RowDefinition[] extraRows)
        {
            var grid = new Grid();
            grid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            grid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
            foreach (var r in extraRows) grid.RowDefinitions.Add(r);
            return grid;
        }

        private UIElement BuildCfgWizardStep(DbmsProfile profile)
        {
            string tuneBat = InstallActions.GetTuneWizardBatPath(profile, _config!);

            if (!File.Exists(tuneBat))
            {
                var missing = new StackPanel();
                missing.Children.Add(PageHelpers.SelectableDim($"Not found: {tuneBat} — run the wizard again after DbxTune is extracted."));
                missing.Children.Add(BuildNextRow().Row);
                return missing;
            }

            var grid = new Grid();
            var editorRow = new RowDefinition { Height = GridLength.Auto }; // toggled to Star once the editor is opened below
            grid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            grid.RowDefinitions.Add(editorRow);
            grid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });

            var lblResult = PageHelpers.SelectableText("");
            lblResult.Margin = new Thickness(0, 8, 0, 8);
            var actionRow = new StackPanel { Orientation = Orientation.Horizontal };
            var btnRun    = new Button { Content = "Run Configuration Wizard" };
            actionRow.Children.Add(btnRun);

            var top = PageHelpers.Stack(actionRow, lblResult);
            Grid.SetRow(top, 0);
            grid.Children.Add(top);

            var editorHost = new ContentControl { Margin = new Thickness(0, 0, 0, 8) };
            Grid.SetRow(editorHost, 1);
            grid.Children.Add(editorHost);

            var (nextRow, btnNext) = BuildNextRow();
            Grid.SetRow(nextRow, 2);
            grid.Children.Add(nextRow);

            void OpenEditor(string cfgFile)
            {
                // The editor opens automatically as soon as the wizard reports a config file. An
                // unsaved edit disables this sub-step's own Next (below) until Saved or Reloaded,
                // so an in-progress edit can't be silently lost.
                editorHost.Content = BuildInlineEditor(cfgFile, dirty => btnNext.IsEnabled = !dirty);
                editorRow.Height = new GridLength(1, GridUnitType.Star);
            }

            void CloseEditor()
            {
                editorHost.Content = null;
                editorRow.Height = GridLength.Auto;
                btnNext.IsEnabled = true; // no editor left to have unsaved changes in
            }

            btnRun.Click += (_, _) =>
            {
                string cfgFile = InstallActions.RunDbmsCfgWizard(profile, _config!, _ => { });
                CloseEditor();
                if (string.IsNullOrEmpty(cfgFile) || !File.Exists(cfgFile))
                {
                    lblResult.Text = "Wizard closed. (No config file path was reported.)";
                }
                else
                {
                    Clipboard.SetText(cfgFile);
                    lblResult.Text = $"Config file: {cfgFile}\nCopied to clipboard — The content of above file.";
                    if (_ctx != null && !_ctx.DbmsConfigFiles.Contains(cfgFile)) _ctx.DbmsConfigFiles.Add(cfgFile);
                    OpenEditor(cfgFile);
                }
            };

            return grid;
        }

        private UIElement BuildFileEditStep(string dbmsName, string label, string path, string hint, string? lexerOverride = null)
        {
            if (!File.Exists(path))
            {
                var missing = new StackPanel();
                missing.Children.Add(PageHelpers.SelectableDim($"Not found: {path} — skipping."));
                missing.Children.Add(BuildNextRow().Row);
                return missing;
            }

            var grid = NewStepGrid(new RowDefinition { Height = GridLength.Auto }, new RowDefinition { Height = GridLength.Auto });

            var lblPath = PageHelpers.SelectableDim($"{label}: {path}");
            lblPath.Margin = new Thickness(0, 0, 0, 6);
            var top = PageHelpers.Stack(lblPath, PageHelpers.Dim(hint));
            Grid.SetRow(top, 0);
            grid.Children.Add(top);

            var (editorElement, sci) = ScintillaHost.Create(File.ReadAllText(path), path, lexerOverride: lexerOverride);
            editorElement.Margin = new Thickness(0, 8, 0, 8);
            Grid.SetRow(editorElement, 1);
            grid.Children.Add(editorElement);

            var (nextRow, btnNext) = BuildNextRow();

            // Unsaved edits disable this sub-step's own Next until Saved or Reloaded.
            var saveRow = ScintillaHost.BuildSaveBar(sci, path,
                onDirtyChanged: dirty => btnNext.IsEnabled = !dirty,
                includeExternalButton: false);
            ((FrameworkElement)saveRow).Margin = new Thickness(0, 0, 0, 8);
            Grid.SetRow(saveRow, 2);
            grid.Children.Add(saveRow);

            Grid.SetRow(nextRow, 3);
            grid.Children.Add(nextRow);
            return grid;
        }

        // A reusable "load file into a Scintilla editor + Save/Reload row" panel, used by the
        // cfgWizard sub-step's auto-opened editor. Sized to fill whatever row it's placed in.
        private UIElement BuildInlineEditor(string path, Action<bool> onDirtyChanged)
        {
            var grid = new Grid { Margin = new Thickness(0, 8, 0, 0), MinHeight = 260 };
            grid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
            grid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });

            var (editorElement, sci) = ScintillaHost.Create(File.ReadAllText(path), path);
            editorElement.Margin = new Thickness(0, 0, 0, 8);
            Grid.SetRow(editorElement, 0);
            grid.Children.Add(editorElement);

            var saveRow = ScintillaHost.BuildSaveBar(sci, path, onDirtyChanged, includeExternalButton: false);
            Grid.SetRow(saveRow, 1);
            grid.Children.Add(saveRow);
            return grid;
        }

        private UIElement BuildSetupInstructionsStep(DbmsProfile profile)
        {
            var grid = NewStepGrid(new RowDefinition { Height = GridLength.Auto });

            var lblNote = new TextBlock
            {
                Text = "NOTE: You need to manually connect to the DBMS you want to monitor and execute the SQL " +
                       "statements below yourself — this wizard does not run them for you.",
                FontWeight = FontWeights.Bold,
                TextWrapping = TextWrapping.Wrap,
                Margin = new Thickness(0, 0, 0, 8),
            };
            lblNote.SetResourceReference(TextBlock.ForegroundProperty, "OrangeBrush");

            var top = PageHelpers.Stack(
                PageHelpers.Dim("SQL to run inside the database to create a monitoring user with the required permissions."),
                lblNote);
            Grid.SetRow(top, 0);
            grid.Children.Add(top);

            string instructions = InstallActions.GetDbSetupInstructionsText(profile);
            var (viewerElement, _) = ScintillaHost.Create(instructions, filePath: null, readOnly: true, lexerOverride: "sql");
            viewerElement.Margin = new Thickness(0, 8, 0, 8);
            Grid.SetRow(viewerElement, 1);
            grid.Children.Add(viewerElement);

            var bottomRow = new StackPanel { Orientation = Orientation.Horizontal, Margin = new Thickness(0, 0, 0, 8) };
            var btnBack = new Button { Content = "< Back", Margin = new Thickness(0, 0, 8, 0), IsEnabled = !AtVeryFirstSubStep };
            btnBack.Click += (_, _) => GoBack();
            var btnNext = new Button { Content = "Next", Style = (Style)Application.Current.Resources["PrimaryButton"] };
            btnNext.Click += (_, _) => Advance();
            bottomRow.Children.Add(btnBack);
            bottomRow.Children.Add(btnNext);
            Grid.SetRow(bottomRow, 2);
            grid.Children.Add(bottomRow);
            return grid;
        }

        // Returns the Next button too, so a sub-step with an inline editor can disable it while
        // the editor has unsaved changes.
        private (UIElement Row, Button Next) BuildNextRow()
        {
            var row = new StackPanel { Orientation = Orientation.Horizontal, Margin = new Thickness(0, 8, 0, 0) };
            var btnBack = new Button { Content = "< Back", Margin = new Thickness(0, 0, 8, 0), IsEnabled = !AtVeryFirstSubStep };
            var btnNext = new Button { Content = "Next", Style = (Style)Application.Current.Resources["PrimaryButton"] };
            btnBack.Click += (_, _) => GoBack();
            btnNext.Click += (_, _) => Advance();
            row.Children.Add(btnBack);
            row.Children.Add(btnNext);
            return (row, btnNext);
        }
    }
}

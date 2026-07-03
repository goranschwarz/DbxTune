using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Threading;
using DbxInstaller.Wizard;

namespace DbxInstaller.Pages
{
    // Runs the silent, background install steps (everything except DBMS configuration and
    // firewall/start-service, which are their own interactive pages later in the sequence).
    internal sealed class InstallProgressPage : WizardPage, IBusyPage
    {
        public override string StepTitle => "Installing";
        public bool IsRunning { get; private set; }

        private readonly StackPanel _railPanel = new();
        private readonly Dictionary<string, StepRowView> _rows = new();
        private readonly LogBox _log = new();
        private readonly Button _btnRetry = new() { Content = "Retry", Visibility = Visibility.Collapsed, HorizontalAlignment = HorizontalAlignment.Left, Margin = new Thickness(0, 8, 0, 0) };
        private readonly Button _btnCancelInstall = new() { Content = "Cancel Install", Visibility = Visibility.Collapsed, HorizontalAlignment = HorizontalAlignment.Left, Margin = new Thickness(0, 8, 0, 0) };

        private bool _succeeded;
        private StreamWriter? _logFile;
        private string _logFilePath = "";
        private CancellationTokenSource? _cts;

        public InstallProgressPage()
        {
            var actionSteps = InstallActions.Steps
                .Where(s => s.Kind == StepKind.Action && InstallActions.ProgressStepNames.Contains(s.Name))
                .ToList();
            foreach (var step in actionSteps)
            {
                var row = new StepRowView(step.Name);
                _rows[step.Name] = row;
                _railPanel.Children.Add(row.Panel);
            }

            _btnRetry.Click += async (_, _) => await RunAsync(_lastCtx!);
            _btnCancelInstall.Click += (_, _) =>
            {
                // Cancels the in-flight download immediately; other (synchronous) steps can't be
                // interrupted mid-flight, so this stops the loop before the next step starts instead.
                _btnCancelInstall.IsEnabled = false;
                _btnCancelInstall.Content = "Cancelling…";
                _cts?.Cancel();
            };

            var grid = new Grid();
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(260) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

            var left = new StackPanel();
            left.Children.Add(PageHelpers.Title("Installing"));
            left.Children.Add(PageHelpers.Subtitle("This may take a few minutes."));
            left.Children.Add(_railPanel);
            left.Children.Add(_btnRetry);
            left.Children.Add(_btnCancelInstall);
            Grid.SetColumn(left, 0);
            grid.Children.Add(left);

            _log.Control.Margin = new Thickness(16, 0, 0, 0);
            Grid.SetColumn(_log.Control, 1);
            grid.Children.Add(_log.Control);

            Content = grid;
        }

        public override void OnEnter(WizardContext ctx)
        {
            if (IsRunning || _succeeded) return;
            _ = RunAsync(ctx);
        }

        private WizardContext? _lastCtx;

        private async Task RunAsync(WizardContext ctx)
        {
            _lastCtx = ctx;
            IsRunning = true;
            _btnRetry.Visibility = Visibility.Collapsed;
            _btnCancelInstall.Visibility = Visibility.Visible;
            _btnCancelInstall.IsEnabled = true;
            _btnCancelInstall.Content = "Cancel Install";
            SetNextEnabled(false);
            foreach (var row in _rows.Values) row.Reset();
            _log.Clear();
            ctx.Config.InitCommandHasWarnings = false;
            ctx.Config.InitCommandWarningDetail = "";

            _cts = new CancellationTokenSource();
            ctx.Config.CancelToken = _cts.Token;

            (_logFile, _logFilePath) = InstallActions.OpenLogFile(InstallMode.Install);
            ctx.LogFilePath = _logFilePath;
            WriteLogFileHeader(ctx.Config);
            _log.AppendLine("Log file: " + _logFilePath, "FgDimBrush");
            _logFile?.WriteLine("Log file: " + _logFilePath);

            var actionSteps = InstallActions.Steps
                .Where(s => s.Kind == StepKind.Action && InstallActions.ProgressStepNames.Contains(s.Name))
                .ToList();

            bool allOk = true;
            bool cancelled = false;
            foreach (var step in actionSteps)
            {
                if (_cts.Token.IsCancellationRequested) { cancelled = true; break; }

                var row = _rows[step.Name];
                row.SetRunning();
                AppendLog("\n> " + step.Name, "YellowBrush");
                // Give WPF's render pass a chance to run — most steps complete synchronously,
                // so without this the UI never repaints between steps until the whole loop is done.
                await Dispatcher.Yield(DispatcherPriority.Background);

                bool ok = false;
                try { ok = await step.Run!(ctx.Config, msg => AppendLog("  " + msg, "FgBrush")); }
                catch (OperationCanceledException) { row.Reset(); AppendLog("  Cancelled by user.", "OrangeBrush"); cancelled = true; break; }
                catch (Exception ex) { AppendLog("  ERROR: " + ex.Message, "RedBrush"); }

                if (cancelled) break;

                if (ok) { row.SetDone(); AppendLog("  Done", "GreenBrush"); }
                else { row.SetFailed(); AppendLog("  Failed — installation stopped.", "RedBrush"); allOk = false; break; }

                await Dispatcher.Yield(DispatcherPriority.Background);
            }

            bool initWarnings = allOk && !cancelled && ctx.Config.InitCommandHasWarnings;

            if (cancelled)
                AppendLog("\nInstallation cancelled — change settings below and click Retry when ready.", "OrangeBrush");
            else
                AppendLog(allOk ? "\nInstallation complete." : "\nInstallation stopped — fix the error and retry.",
                    allOk ? "GreenBrush" : "RedBrush");

            if (initWarnings)
            {
                string bar = new string('=', 70);
                AppendLog("\n" + bar, "RedBrush");
                AppendLog("  WARNING: possible error(s) found while initializing the DbxTune home directory", "RedBrush");
                AppendLog(bar, "RedBrush");
                AppendLog(ctx.Config.InitCommandWarningDetail, "RedBrush");
                AppendLog(bar, "RedBrush");
                AppendLog("  Review the lines above (full details are in the 'Initialize DbxTune home' step earlier " +
                          "in this log). Click Next to continue anyway, or Retry to run the install again.", "RedBrush");
            }

            _logFile?.Flush();
            _logFile?.Close();
            _logFile = null;

            IsRunning = false;
            _succeeded = allOk && !cancelled;
            SetNextEnabled(_succeeded);
            _btnCancelInstall.Visibility = Visibility.Collapsed;
            _btnRetry.Visibility = (_succeeded && !initWarnings) ? Visibility.Collapsed : Visibility.Visible;
        }

        private void WriteLogFileHeader(InstallConfig c)
        {
            if (_logFile == null) return;
            string bar = new string('=', 70);
            _logFile.WriteLine(bar);
            _logFile.WriteLine("DbxTune Installer — Installation Log");
            _logFile.WriteLine(bar);
            _logFile.WriteLine($"Date    : {DateTime.Now:yyyy-MM-dd HH:mm:ss}");
            _logFile.WriteLine($"Machine : {Environment.MachineName}");
            _logFile.WriteLine($"User    : {Environment.UserDomainName}\\{Environment.UserName}");
            _logFile.WriteLine();
            _logFile.WriteLine($"  Service account : {c.ServiceAccount}");
            _logFile.WriteLine($"  Install dir     : {c.InstallDir}");
            _logFile.WriteLine($"  DbxUserHome     : {c.DbxUserHome}");
            _logFile.WriteLine($"  Package         : {(c.DownloadZip ? c.ZipUrl : c.ZipLocalPath)}");
            _logFile.WriteLine($"  Java            : {c.JavaExe}");
            _logFile.WriteLine($"  Web UI port     : {(c.WebPort < 0 ? "disabled" : c.WebPort.ToString())}");
            if (c.SelectedDbms.Count > 0) _logFile.WriteLine($"  DBMS configured : {string.Join(", ", c.SelectedDbms)}");
            _logFile.WriteLine(bar);
            _logFile.Flush();
        }

        private void AppendLog(string text, string brushKey)
        {
            _log.AppendLine(text, brushKey);
            string normalized = text.Replace("\r\n", "\n").Replace("\r", "\n");
            foreach (var line in normalized.Split('\n')) _logFile?.WriteLine(line);
        }

        public override bool CanGoBack(WizardContext ctx) => !IsRunning;
        public override bool CanCancel(WizardContext ctx) => !IsRunning;

        public override Task<WizardValidationResult> ValidateBeforeNextAsync(WizardContext ctx) =>
            Task.FromResult(_succeeded ? WizardValidationResult.Ok() : WizardValidationResult.Fail("Fix the failed step and click Retry before continuing."));
    }

    // Small ○/◉/●  status indicator row, shared visual language with the original StepRow.
    internal sealed class StepRowView
    {
        public StackPanel Panel { get; } = new() { Orientation = Orientation.Horizontal, Margin = new Thickness(0, 2, 0, 2) };
        private readonly TextBlock _dot;
        private readonly TextBlock _name;

        public StepRowView(string name)
        {
            _dot = new TextBlock { Width = 20 };
            _name = new TextBlock { Text = name, TextWrapping = TextWrapping.Wrap };
            Panel.Children.Add(_dot);
            Panel.Children.Add(_name);
            Reset();
        }

        public void Reset()      => Set("○", "FgDimBrush", "FgBrush");
        public void SetRunning() => Set("◉", "YellowBrush", "FgBrush");
        public void SetDone()    => Set("●", "GreenBrush", "FgBrush");
        public void SetFailed()  => Set("●", "RedBrush", "FgBrush");

        private void Set(string dot, string dotBrush, string nameBrush)
        {
            _dot.Text = dot;
            _dot.SetResourceReference(TextBlock.ForegroundProperty, dotBrush);
            _name.SetResourceReference(TextBlock.ForegroundProperty, nameBrush);
        }
    }
}

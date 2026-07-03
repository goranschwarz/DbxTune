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
    internal sealed class UpgradeProgressPage : WizardPage, IBusyPage
    {
        public override string StepTitle => "Upgrading";
        public bool IsRunning { get; private set; }

        private static readonly string[] UpgradeStepNames =
        {
            "Stop service if running", "Locate package", "Create directory structure",
            "Extract package", "Set DbxTune directories", "Initialize DbxTune home",
            "Verify DbxTune directories",
        };

        private readonly StackPanel _railPanel = new();
        private readonly Dictionary<string, StepRowView> _rows = new();
        private readonly LogBox _log = new();
        private readonly Button _btnRetry = new() { Content = "Retry", Visibility = Visibility.Collapsed, HorizontalAlignment = HorizontalAlignment.Left, Margin = new Thickness(0, 8, 0, 0) };
        private readonly Button _btnCancelInstall = new() { Content = "Cancel Upgrade", Visibility = Visibility.Collapsed, HorizontalAlignment = HorizontalAlignment.Left, Margin = new Thickness(0, 8, 0, 0) };

        private bool _succeeded;
        private WizardContext? _lastCtx;
        private CancellationTokenSource? _cts;
        private StreamWriter? _logFile;

        public UpgradeProgressPage()
        {
            foreach (var name in UpgradeStepNames.Append("Restart service"))
            {
                var row = new StepRowView(name);
                _rows[name] = row;
                _railPanel.Children.Add(row.Panel);
            }
            _btnRetry.Click += async (_, _) => await RunAsync(_lastCtx!);
            _btnCancelInstall.Click += (_, _) =>
            {
                _btnCancelInstall.IsEnabled = false;
                _btnCancelInstall.Content = "Cancelling…";
                _cts?.Cancel();
            };

            var grid = new Grid();
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(260) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

            var left = new StackPanel();
            left.Children.Add(PageHelpers.Title("Upgrading"));
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

        private async Task RunAsync(WizardContext ctx)
        {
            _lastCtx = ctx;
            IsRunning = true;
            _btnRetry.Visibility = Visibility.Collapsed;
            _btnCancelInstall.Visibility = Visibility.Visible;
            _btnCancelInstall.IsEnabled = true;
            _btnCancelInstall.Content = "Cancel Upgrade";
            SetNextEnabled(false);
            foreach (var row in _rows.Values) row.Reset();
            _log.Clear();

            _cts = new CancellationTokenSource();
            ctx.Config.CancelToken = _cts.Token;

            (_logFile, string logFilePath) = InstallActions.OpenLogFile(InstallMode.Upgrade);
            ctx.LogFilePath = logFilePath;
            AppendLog("Log file: " + logFilePath, "FgDimBrush");

            var steps = InstallActions.Steps.Where(s => s.Kind == StepKind.Action && UpgradeStepNames.Contains(s.Name)).ToList();

            bool allOk = true;
            bool cancelled = false;
            foreach (var step in steps)
            {
                if (_cts.Token.IsCancellationRequested) { cancelled = true; break; }

                var row = _rows[step.Name];
                row.SetRunning();
                AppendLog("\n> " + step.Name, "YellowBrush");
                await Dispatcher.Yield(DispatcherPriority.Background);

                bool ok = false;
                try { ok = await step.Run!(ctx.Config, msg => AppendLog("  " + msg, "FgBrush")); }
                catch (OperationCanceledException) { row.Reset(); AppendLog("  Cancelled by user.", "OrangeBrush"); cancelled = true; break; }
                catch (Exception ex) { AppendLog("  ERROR: " + ex.Message, "RedBrush"); }

                if (cancelled) break;

                if (ok) { row.SetDone(); AppendLog("  Done", "GreenBrush"); } else { row.SetFailed(); allOk = false; break; }

                await Dispatcher.Yield(DispatcherPriority.Background);
            }

            if (allOk && !cancelled)
            {
                var row = _rows["Restart service"];
                row.SetRunning();
                AppendLog("\n> Restart service", "YellowBrush");
                await Dispatcher.Yield(DispatcherPriority.Background);
                try
                {
                    await InstallActions.StartServiceCore(ctx.Config, msg => AppendLog("  " + msg, "FgBrush"), startService: true, launchClient: false);
                    row.SetDone();
                    AppendLog("  Done", "GreenBrush");
                }
                catch (Exception ex) { AppendLog("  ERROR: " + ex.Message, "RedBrush"); row.SetFailed(); allOk = false; }
            }

            if (cancelled)
                AppendLog("\nUpgrade cancelled — change settings below and click Retry when ready.", "OrangeBrush");
            else
                AppendLog(allOk ? "\nUpgrade complete." : "\nUpgrade stopped — fix the error and retry.", allOk ? "GreenBrush" : "RedBrush");

            _logFile?.Flush();
            _logFile?.Close();
            _logFile = null;

            IsRunning = false;
            _succeeded = allOk && !cancelled;
            SetNextEnabled(_succeeded);
            _btnCancelInstall.Visibility = Visibility.Collapsed;
            _btnRetry.Visibility = _succeeded ? Visibility.Collapsed : Visibility.Visible;
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
}

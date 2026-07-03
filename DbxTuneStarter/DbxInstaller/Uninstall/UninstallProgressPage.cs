using System;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using DbxInstaller.Pages;
using DbxInstaller.Wizard;

namespace DbxInstaller.Uninstall
{
    // Best-effort removal: unlike install's fail-fast semantics, a failed step here is logged
    // and the remaining steps still run (matches the original UninstallForm.DoUninstall behavior).
    internal sealed class UninstallProgressPage : WizardPage, IBusyPage
    {
        public override string StepTitle => "Uninstalling";
        public bool IsRunning { get; private set; }

        private readonly StackPanel _railPanel = new();
        private readonly LogBox _log = new();
        private readonly Button _btnRetry = new() { Content = "Retry", Visibility = Visibility.Collapsed, HorizontalAlignment = HorizontalAlignment.Left, Margin = new Thickness(0, 8, 0, 0) };
        private bool _succeeded;
        private WizardContext? _lastCtx;
        private StreamWriter? _logFile;

        public UninstallProgressPage()
        {
            _btnRetry.Click += async (_, _) => await RunAsync(_lastCtx!);

            var grid = new Grid();
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(260) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

            var left = new StackPanel();
            left.Children.Add(PageHelpers.Title("Uninstalling"));
            left.Children.Add(_railPanel);
            left.Children.Add(_btnRetry);
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
            SetNextEnabled(false);
            _railPanel.Children.Clear();
            _log.Clear();

            (_logFile, string logFilePath) = InstallActions.OpenLogFile(InstallMode.Remove);
            ctx.LogFilePath = logFilePath;
            Log("Log file: " + logFilePath, "FgDimBrush");

            var rowStop     = AddRow("Stop client + service");
            var rowSvc      = AddRow("Delete service registration");
            var rowFw       = AddRow("Remove firewall rules");
            var rowUser     = ctx.RemoveAccount ? AddRow("Delete User Account and Profile") : null;
            var rowData     = ctx.RemoveData    ? AddRow("Delete user data directories") : null;
            var rowSw       = ctx.RemoveSoftware ? AddRow("Delete software directory") : null;
            var rowVerify   = AddRow("Verify removal");

            bool allOk = true;

            await Task.Run(() =>
            {
                RunStep(rowStop, "Stop client + service", () =>
                {
                    UninstallActions.KillClient(msg => Log(msg));
                    UninstallActions.StopService(msg => Log(msg));
                    return true;
                });

                RunStep(rowSvc, "Delete service registration", () =>
                {
                    var (_, output) = InstallActions.Run("sc", "delete DbxStarterService");
                    Log(string.IsNullOrWhiteSpace(output) ? "  Service registration removed." : $"  {output.Trim()}");
                    return true;
                });

                RunStep(rowFw, "Remove firewall rules", () => { InstallActions.RemoveFirewallRules(msg => Log(msg)); return true; });

                if (rowUser != null && ctx.DetectedServiceAccount != null)
                    if (!RunStep(rowUser, "Delete User Account and Profile",
                        () => UninstallActions.RemoveUserAccount(ctx.DetectedServiceAccount, ctx.DetectedProfileDir, msg => Log(msg))))
                        allOk = false;

                if (rowData != null)
                    if (!RunStep(rowData, "Delete user data directories", () =>
                    {
                        var allDirs = ctx.DetectedDataDirs.Concat(ctx.DetectedExternalDirs).Distinct().ToList();
                        if (allDirs.Count == 0) { Log("  Nothing to remove."); return true; }
                        bool ok = true;
                        foreach (var d in allDirs)
                            if (!UninstallActions.TryRemoveDir(d, "user", msg => Log(msg))) ok = false;
                        return ok;
                    }))
                        allOk = false;

                if (rowSw != null && ctx.DetectedInstallDir != null)
                    if (!RunStep(rowSw, "Delete software directory",
                        () => UninstallActions.TryRemoveDir(ctx.DetectedInstallDir, "software", msg => Log(msg))))
                        allOk = false;

                if (!RunStep(rowVerify, "Verify removal", () =>
                {
                    var remaining = UninstallActions.Verify(ctx.DetectedServiceAccount, ctx.DetectedProfileDir, ctx.DetectedInstallDir,
                        ctx.DetectedDataDirs.Concat(ctx.DetectedExternalDirs), ctx.RemoveAccount, ctx.RemoveData, ctx.RemoveSoftware);
                    if (remaining.Count == 0) { Log("  Everything selected for removal is gone."); return true; }
                    foreach (var r in remaining) Log($"  Still present: {r}", "RedBrush");
                    return false;
                }))
                    allOk = false;
            });

            Log(allOk ? "\nUninstall complete." : "\nUninstall finished with warnings — see the red entries above. Click Retry to try again.",
                allOk ? "GreenBrush" : "RedBrush");

            _logFile?.Flush();
            _logFile?.Close();
            _logFile = null;

            IsRunning = false;
            _succeeded = allOk;
            SetNextEnabled(true);
            _btnRetry.Visibility = _succeeded ? Visibility.Collapsed : Visibility.Visible;
        }

        private StepRowView AddRow(string name)
        {
            var row = new StepRowView(name);
            Dispatcher.Invoke(() => _railPanel.Children.Add(row.Panel));
            return row;
        }

        // Best-effort: exceptions (and steps that return false) are logged in red, but
        // execution continues with the next step. Returns false on failure so callers can
        // roll it into the page's overall Retry-eligibility.
        private bool RunStep(StepRowView row, string name, Func<bool> body)
        {
            Dispatcher.Invoke(row.SetRunning);
            Log("\n> " + name, "YellowBrush");
            try
            {
                bool ok = body();
                if (ok) { Dispatcher.Invoke(row.SetDone); Log("  Done", "GreenBrush"); }
                else { Dispatcher.Invoke(row.SetFailed); Log("  Not fully completed — see above.", "RedBrush"); }
                return ok;
            }
            catch (Exception ex)
            {
                Log($"  ERROR: {ex.Message}", "RedBrush");
                Dispatcher.Invoke(row.SetFailed);
                return false;
            }
        }

        private void Log(string text, string brushKey = "FgBrush")
        {
            Dispatcher.Invoke(() => _log.AppendLine(text, brushKey));
            string normalized = text.Replace("\r\n", "\n").Replace("\r", "\n");
            foreach (var line in normalized.Split('\n')) _logFile?.WriteLine(line);
        }

        public override bool CanGoBack(WizardContext ctx) => !IsRunning;
        public override bool CanCancel(WizardContext ctx) => !IsRunning;

        public override Task<WizardValidationResult> ValidateBeforeNextAsync(WizardContext ctx) =>
            Task.FromResult(WizardValidationResult.Ok());
    }
}

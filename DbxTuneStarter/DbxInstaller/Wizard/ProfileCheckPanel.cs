using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;

namespace DbxInstaller.Wizard
{
    // The profile-folder check of a service account (ProfileCheck) as a block of a wizard page: what
    // is in the way of C:\Users\<name>, a "Clean up…" button for the left-overs the installer may
    // delete (after a confirmation that lists them), and "PowerShell commands" for the administrator.
    // Used by the Service Account page (Install) and the Ready to Upgrade / Repair page.
    internal sealed class ProfileCheckPanel : StackPanel
    {
        private readonly TextBlock _lblProfile    = new() { TextWrapping = TextWrapping.Wrap };
        private readonly Button    _btnCleanup    = new() { Content = "Clean up…", Visibility = Visibility.Collapsed, Margin = new Thickness(0, 0, 8, 0) };
        private readonly Button    _btnPowerShell = new() { Content = "PowerShell commands", Visibility = Visibility.Collapsed, Margin = new Thickness(0, 0, 8, 0) };
        private readonly Button    _btnCopy       = new() { Content = "Copy", Visibility = Visibility.Collapsed };
        private readonly TextBox   _txtPowerShell = new() { FontFamily = new System.Windows.Media.FontFamily("Consolas") };
        private readonly Border    _powerShellBox = new() { Padding = new Thickness(10, 6, 10, 6), Margin = new Thickness(0, 6, 0, 0), Visibility = Visibility.Collapsed };
        private List<ProfileCheck.Finding> _findings = new();
        private string _account = "", _expected = "";
        private bool   _notFound;            // a domain account that doesn't resolve: nothing was checked
        private readonly string _flow;       // "install" / "upgrade / repair": what would stop

        public ProfileCheckPanel(string flow)
        {
            _flow = flow;
            Margin = new Thickness(0, 12, 0, 0);
            _txtPowerShell.Style = (Style)Application.Current.Resources["SelectableText"];
            _powerShellBox.Child = _txtPowerShell;
            _powerShellBox.SetResourceReference(Border.BackgroundProperty, "Bg2Brush");

            _btnCleanup.Click    += async (_, _) => await CleanupAsync();
            _btnPowerShell.Click += (_, _) =>
            {
                bool show = _powerShellBox.Visibility != Visibility.Visible;
                _powerShellBox.Visibility = show ? Visibility.Visible : Visibility.Collapsed;
                _btnCopy.Visibility       = show ? Visibility.Visible : Visibility.Collapsed;
            };
            _btnCopy.Click += (_, _) => { try { Clipboard.SetText(_txtPowerShell.Text); } catch { /* clipboard busy */ } };

            var buttons = new StackPanel { Orientation = Orientation.Horizontal, Margin = new Thickness(0, 8, 0, 0) };
            buttons.Children.Add(_btnCleanup);
            buttons.Children.Add(_btnPowerShell);
            buttons.Children.Add(_btnCopy);
            Children.Add(_lblProfile);
            Children.Add(buttons);
            Children.Add(_powerShellBox);
        }

        // The findings belong to the account that was checked: call this when the account name changes,
        // so "Clean up…" is never offered for another account than the one shown.
        public void Clear()
        {
            _findings = new();
            _lblProfile.Inlines.Clear();
            _btnCleanup.Visibility = _btnPowerShell.Visibility = _btnCopy.Visibility = _powerShellBox.Visibility = Visibility.Collapsed;
        }

        public Task CheckAsync(string account) => CheckAsync(account, Array.Empty<string>());

        // cleanupLog: the lines of a clean-up just done, shown first.
        private async Task CheckAsync(string account, IReadOnlyList<string> cleanupLog)
        {
            // Looking up an account in a domain can take a few seconds: say so meanwhile.
            _lblProfile.Inlines.Clear();
            var checking = new Run("Checking the profile folder…");
            checking.SetResourceReference(TextElement.ForegroundProperty, "FgDimBrush");
            _lblProfile.Inlines.Add(checking);
            _btnCleanup.Visibility = Visibility.Collapsed;

            string expected = InstallActions.AccountHome(account);
            List<ProfileCheck.Finding> findings;
            bool   notFound = false;
            string error = "";
            try
            {
                (findings, notFound) = await Task.Run(() =>
                {
                    var snapshot = ProfileCheck.ReadSnapshot(account, expected);
                    return (ProfileCheck.Analyze(account, expected, snapshot), snapshot.AccountSid == null && ProfileCheck.IsDomainName(account));
                });
            }
            catch (Exception ex) { findings = new(); error = ex.Message; }

            _findings = findings;
            _notFound = notFound;
            _account  = account;
            _expected = expected;
            ShowFindings(cleanupLog, error);
        }

        private void ShowFindings(IReadOnlyList<string> cleanupLog, string error)
        {
            Run Line(string text, string brush, bool bold = false)
            {
                var run = new Run(text) { FontWeight = bold ? FontWeights.SemiBold : FontWeights.Normal };
                run.SetResourceReference(TextElement.ForegroundProperty, brush);
                return run;
            }
            void Add(Run run) { if (_lblProfile.Inlines.Count > 0) _lblProfile.Inlines.Add(new LineBreak()); _lblProfile.Inlines.Add(run); }

            _lblProfile.Inlines.Clear();
            if (cleanupLog.Count > 0)
            {
                Add(Line("Clean up:", "FgBrush", bold: true));
                foreach (var line in cleanupLog) Add(Line("  " + line, line.StartsWith("FAILED") ? "RedBrush" : "FgBrush"));
            }

            bool blocks = _findings.Any(f => f.Blocks);
            if (error.Length > 0)
                Add(Line($"Profile folder {_expected}: could not check it ({error}).", "OrangeBrush"));
            else if (_notFound)
                Add(Line($"Profile folder: not checked — the account '{_account}' was not found.", "FgDimBrush"));
            else if (_findings.Count == 0)
                Add(Line($"Profile folder {_expected}: OK — free, or already the profile of this account.", "GreenBrush"));
            else
            {
                Add(Line(blocks ? $"Profile folder {_expected}: something is in the way — the {_flow} would stop at 'Create user profile':"
                                : $"Profile folder {_expected}: OK, but note:",
                         blocks ? "OrangeBrush" : "FgBrush", bold: true));
                foreach (var f in _findings) Add(Line("  •  " + f.Text, f.Blocks ? "OrangeBrush" : "FgDimBrush"));
                if (_findings.Any(f => f.Kind != ProfileCheck.CleanupKind.None))
                    Add(Line("'Clean up…' deletes the left-overs listed above, after asking. 'PowerShell commands' shows how to look at, " +
                             "or do, it yourself.", "FgDimBrush"));
            }

            _btnCleanup.Visibility    = _findings.Any(f => f.Kind != ProfileCheck.CleanupKind.None) ? Visibility.Visible : Visibility.Collapsed;
            _btnPowerShell.Visibility = Visibility.Visible;
            _txtPowerShell.Text       = ProfileCheck.PowerShellCommands(_account, _expected, _findings);
        }

        // Asks with the exact list of what gets deleted, deletes it, then checks again.
        private async Task CleanupAsync()
        {
            var items = _findings.Where(f => f.Kind != ProfileCheck.CleanupKind.None).ToList();
            if (items.Count == 0) return;

            string text = "These will be deleted:\n\n" + string.Join("\n\n", items.Select(f => "•  " + f.What)) + "\n\n";
            if (items.Any(f => f.What.Contains("DbxTune data")))
                text += "WARNING: some of this contains DbxTune data (recordings, configuration, software).\n\n";
            text += "This can't be undone. Delete them?";
            var answer = MessageBox.Show(Window.GetWindow(this), text, "DbxTune Setup — Clean up",
                                         MessageBoxButton.YesNo, MessageBoxImage.Warning, MessageBoxResult.No);
            if (answer != MessageBoxResult.Yes) return;

            _btnCleanup.IsEnabled = false;
            var log = new List<string>();
            await Task.Run(() => ProfileCheck.Cleanup(items, msg => { lock (log) log.Add(msg); }));
            await CheckAsync(_account, log);
            _btnCleanup.IsEnabled = true;
        }
    }
}

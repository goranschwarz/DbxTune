using System;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Threading;
using DbxInstaller.Wizard;

namespace DbxInstaller.Pages
{
    internal sealed class ServiceAccountPage : WizardPage
    {
        public override string StepTitle => "Service Account";

        private readonly TextBox _txtAccount = PageHelpers.TextBox(InstallerSettings.ServiceAccount ?? "dbxtune");   // last run's account
        private readonly PasswordBox _txtPassword = new() { Margin = new Thickness(0, 2, 0, 2) };
        private readonly Button _btnCheck = new() { Content = "Check" };
        private readonly TextBlock _lblCheckStatus = new() { Margin = new Thickness(0, 4, 0, 0), TextWrapping = TextWrapping.Wrap };
        private readonly TextBlock _lblGmsaNote = new() { TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 8, 0, 0) };

        // Shown above "Account name" when this computer is joined to an AD domain: recommends a
        // domain account, which can log in to SQL Server with Windows authentication (no database
        // password to store), and says so when the name entered is a local account.
        private readonly string? _domain    = GroupPolicy.JoinedDomain();   // NetBIOS name, null = not joined
        private readonly string  _dnsDomain = GroupPolicy.DnsDomain();
        private readonly TextBlock _domainText = new() { TextWrapping = TextWrapping.Wrap };
        private readonly Border    _domainNotice = new()
        {
            BorderThickness = new Thickness(3, 0, 0, 0),
            Padding         = new Thickness(10, 6, 10, 6),
            Margin          = new Thickness(0, 4, 0, 10),
        };

        // Profile check, run by Check after the password: what is in the way of C:\Users\<name>, with
        // "Clean up…" and "PowerShell commands".
        private readonly ProfileCheckPanel _profile = new("install");

        // Leaving "Account name" runs the checks that DON'T log on (does the account exist, the profile
        // folder) — a logon with an empty or half-typed password would count toward the account's lockout.
        // The password test stays on Check; until it ran for the current name + password, _lblUrge asks
        // for it, and Next asks once more.
        private readonly TextBlock _lblUrge = new() { TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 2, 0, 0),
            Text = "Press Check to test the password before you continue — it logs on once as the account." };
        private string _autoCheckedAccount = "";
        private (string Account, string Password, InstallActions.VerifyOutcome Outcome, string Message)? _lastCheck;

        public ServiceAccountPage()
        {
            _domainNotice.Child = _domainText;
            _domainNotice.SetResourceReference(Border.BackgroundProperty,  "Bg2Brush");
            _domainNotice.SetResourceReference(Border.BorderBrushProperty, "FgHdrBrush");

            _lblUrge.SetResourceReference(TextBlock.ForegroundProperty, "OrangeBrush");
            _txtAccount.TextChanged += (_, _) =>
            {
                UpdateGmsaNote();
                UpdateDomainNotice();
                _profile.Clear();   // its findings belong to the account that was checked
                UpdateUrge();
            };
            _txtPassword.PasswordChanged += (_, _) => UpdateUrge();
            _txtAccount.LostFocus += async (_, _) => await AutoCheckAccountAsync();
            _btnCheck.Click += async (_, _) =>
            {
                _lblCheckStatus.Text = "Checking…";
                _lblCheckStatus.SetResourceReference(TextBlock.ForegroundProperty, "FgDimBrush");
                _btnCheck.IsEnabled = false;
                // LogonUser against a domain controller can take a noticeable moment — let the
                // "Checking…" text actually paint before the blocking call runs.
                await Dispatcher.Yield(DispatcherPriority.Background);

                var (outcome, message) = InstallActions.TryVerifyPassword(_txtAccount.Text.Trim(), _txtPassword.Password);
                _lastCheck = (_txtAccount.Text.Trim(), _txtPassword.Password, outcome, message);
                _autoCheckedAccount = _txtAccount.Text.Trim();
                UpdateUrge();
                _lblCheckStatus.Text = message;
                string brush = outcome switch
                {
                    InstallActions.VerifyOutcome.Verified => "GreenBrush",
                    InstallActions.VerifyOutcome.Info     => "OrangeBrush",
                    _                                      => "RedBrush",
                };
                _lblCheckStatus.SetResourceReference(TextBlock.ForegroundProperty, brush);

                await _profile.CheckAsync(_txtAccount.Text.Trim());
                _btnCheck.IsEnabled = true;
            };

            // Check can print a lot (1385 text, profile findings, commands): let the page scroll.
            Content = new ScrollViewer
            {
                VerticalScrollBarVisibility = ScrollBarVisibility.Auto,
                Content = PageHelpers.Stack(
                    PageHelpers.Title("Service Account"),
                    PageHelpers.Subtitle("DbxTune and DbxStarterService run under this Windows account. Use a local account name, " +
                                          "a domain account (DOMAIN\\user), or a gMSA (account name ending with '$')."),
                    _domainNotice,
                    PageHelpers.Row("Account name", _txtAccount),
                    PageHelpers.Row("Password", _txtPassword, _btnCheck),
                    _lblUrge,
                    _lblCheckStatus,
                    _lblGmsaNote,
                    _profile),
            };

            UpdateGmsaNote();
            UpdateDomainNotice();
            UpdateUrge();
        }

        private bool IsGmsa => _txtAccount.Text.Trim().EndsWith('$');

        // Check ran for exactly what is in the fields now (a gMSA has no password to check).
        private bool CheckedCurrent => IsGmsa || (_lastCheck is { } last && last.Account == _txtAccount.Text.Trim() && last.Password == _txtPassword.Password);

        private void UpdateUrge() =>
            _lblUrge.Visibility = CheckedCurrent || _txtAccount.Text.Trim().Length == 0 ? Visibility.Collapsed : Visibility.Visible;

        // Leaving "Account name": does the account exist, and the profile folder — no logon. Only when the
        // name changed since the last check, so tabbing through the fields doesn't check again.
        private async Task AutoCheckAccountAsync()
        {
            string account = _txtAccount.Text.Trim();
            if (account.Length == 0 || account == _autoCheckedAccount) return;
            _autoCheckedAccount = account;

            _lblCheckStatus.Text = "Looking up the account…";
            _lblCheckStatus.SetResourceReference(TextBlock.ForegroundProperty, "FgDimBrush");
            bool exists = await Task.Run(() => { try { LsaPrivileges.GetSidString(account); return true; } catch { return false; } });
            // Changed again meanwhile, or Check was pressed (clicking it also leaves this field): its result wins
            if (_txtAccount.Text.Trim() != account || _lastCheck?.Account == account) return;

            (string text, string brush) = exists
                ? ($"Account '{account}' found.", "FgDimBrush")
                : ProfileCheck.IsDomainName(account)
                    ? ($"Account '{account}' was not found — check the spelling (DOMAIN\\user or user@domain.com) and that a domain " +
                       "controller is reachable. The installer creates only local accounts given as a plain name (e.g. dbxtune).", "RedBrush")
                    : ($"Account '{account}' doesn't exist yet — the install creates it as a local account.", "FgDimBrush");
            _lblCheckStatus.Text = text;
            _lblCheckStatus.SetResourceReference(TextBlock.ForegroundProperty, brush);
            await _profile.CheckAsync(account);
        }

        private void UpdateDomainNotice()
        {
            _domainNotice.Visibility = _domain == null ? Visibility.Collapsed : Visibility.Visible;
            if (_domain == null) return;

            // The example uses the name typed so far: "dbxtune" -> CORP\dbxtune.
            string account  = _txtAccount.Text.Trim();
            string prefix   = account.Contains('\\') ? account.Split('\\', 2)[0] : "";
            string userPart = (account.Contains('\\') ? account.Split('\\', 2)[1] : account.Split('@')[0]).TrimEnd('$');
            string example  = $"{_domain}\\{(userPart.Length > 0 ? userPart : "svc_dbxtune")}";
            // A plain name, .\name or THISCOMPUTER\name is a local account
            bool local = account.Length > 0 && !account.Contains('@')
                         && (prefix.Length == 0 || prefix == "." || prefix.Equals(Environment.MachineName, StringComparison.OrdinalIgnoreCase));
            string dns = _dnsDomain.Length > 0 && !_dnsDomain.Equals(_domain, StringComparison.OrdinalIgnoreCase) ? $" ({_dnsDomain})" : "";

            _domainText.Inlines.Clear();
            var title = new Run($"This computer is joined to the Active Directory domain {_domain}{dns}.") { FontWeight = FontWeights.SemiBold };
            title.SetResourceReference(TextElement.ForegroundProperty, "FgHdrBrush");
            _domainText.Inlines.Add(title);
            _domainText.Inlines.Add(new LineBreak());

            var body = new Run($"A domain account is recommended, for example {example} (or a gMSA: {example}$). DbxTune can then " +
                               "connect to SQL Server with Windows authentication, without a database password stored in DbxTune, and the " +
                               "same account works on all your servers. The installer doesn't create domain accounts: ask your AD " +
                               "administrator for one.");
            body.SetResourceReference(TextElement.ForegroundProperty, "FgBrush");
            _domainText.Inlines.Add(body);

            if (local)
            {
                _domainText.Inlines.Add(new LineBreak());
                var warn = new Run($"'{account}' is a local account of this computer — it can't use Windows authentication to a " +
                                   "SQL Server on another computer.");
                warn.SetResourceReference(TextElement.ForegroundProperty, "OrangeBrush");
                _domainText.Inlines.Add(warn);
            }
        }

        // A gMSA has no password to enter or verify; Check still checks its profile folder.
        private void UpdateGmsaNote()
        {
            _txtPassword.IsEnabled = !IsGmsa;
            _lblGmsaNote.Text = IsGmsa
                ? "gMSA account detected — Active Directory manages the password automatically; leave the password blank."
                : "";
        }

        public override void OnLeave(WizardContext ctx)
        {
            ctx.Config.ServiceAccount = _txtAccount.Text.Trim();
            ctx.Config.Password       = _txtPassword.Password;
            InstallerSettings.RememberServiceAccount(ctx.Config.ServiceAccount);   // never the password
        }

        public override Task<WizardValidationResult> ValidateBeforeNextAsync(WizardContext ctx)
        {
            if (string.IsNullOrWhiteSpace(_txtAccount.Text))
                return Task.FromResult(WizardValidationResult.Fail("Service account name is required."));
            if (!IsGmsa && string.IsNullOrWhiteSpace(_txtPassword.Password))
                return Task.FromResult(WizardValidationResult.Fail(
                    "Password is required for local and domain accounts.\nLeave blank only for gMSA accounts (name ending with '$')."));

            // Not checked (or the last Check failed): ask once — a wrong password or a blocked account would
            // otherwise only show up during the install.
            string? question = !CheckedCurrent
                ? $"You haven't pressed Check for '{_txtAccount.Text.Trim()}' with this password.\n\n" +
                  "A wrong password or a blocked account is then found only during the install."
                : _lastCheck is { Outcome: InstallActions.VerifyOutcome.Failed } failed
                    ? $"The last Check failed:\n\n{failed.Message}"
                    : null;
            if (question != null &&
                MessageBox.Show(Window.GetWindow(this), question + "\n\nContinue anyway?", "DbxTune Setup — Service Account",
                                MessageBoxButton.YesNo, MessageBoxImage.Question, MessageBoxResult.No) != MessageBoxResult.Yes)
                return Task.FromResult(WizardValidationResult.Fail("Press Check to test the account and the password."));
            return Task.FromResult(WizardValidationResult.Ok());
        }
    }
}

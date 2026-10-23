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

        public ServiceAccountPage()
        {
            _domainNotice.Child = _domainText;
            _domainNotice.SetResourceReference(Border.BackgroundProperty,  "Bg2Brush");
            _domainNotice.SetResourceReference(Border.BorderBrushProperty, "FgHdrBrush");

            _txtAccount.TextChanged += (_, _) =>
            {
                UpdateGmsaNote();
                UpdateDomainNotice();
                _profile.Clear();   // its findings belong to the account that was checked
            };
            _btnCheck.Click += async (_, _) =>
            {
                _lblCheckStatus.Text = "Checking…";
                _lblCheckStatus.SetResourceReference(TextBlock.ForegroundProperty, "FgDimBrush");
                _btnCheck.IsEnabled = false;
                // LogonUser against a domain controller can take a noticeable moment — let the
                // "Checking…" text actually paint before the blocking call runs.
                await Dispatcher.Yield(DispatcherPriority.Background);

                var (outcome, message) = InstallActions.TryVerifyPassword(_txtAccount.Text.Trim(), _txtPassword.Password);
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
                    _lblCheckStatus,
                    _lblGmsaNote,
                    _profile),
            };

            UpdateGmsaNote();
            UpdateDomainNotice();
        }

        private bool IsGmsa => _txtAccount.Text.Trim().EndsWith('$');

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
            return Task.FromResult(WizardValidationResult.Ok());
        }
    }
}

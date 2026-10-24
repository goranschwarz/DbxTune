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

        // The three kinds of account, how to write the name, and when to use which. The kind follows from
        // the name, so the matching line is highlighted while typing (UpdateAccountTypes).
        private readonly Grid _accountTypes = new() { Margin = new Thickness(0, 0, 0, 10) };

        // Profile check, run by Check after the password: what is in the way of C:\Users\<name>, with
        // "Clean up…" and "PowerShell commands".
        private readonly ProfileCheckPanel _profile = new("install");

        // Leaving "Account name" runs the checks that DON'T log on (does the account exist, the profile
        // folder) — a logon with an empty or half-typed password would count toward the account's lockout.
        // The password test stays on Check. Next stays disabled until Check passed for exactly what is in
        // the fields, and nothing blocks the profile folder — the hint beside Next says what is missing.
        private string _autoCheckedAccount = "";
        private (string Account, string Password, InstallActions.VerifyOutcome Outcome, string Message)? _lastCheck;

        public ServiceAccountPage()
        {
            _domainNotice.Child = _domainText;
            _domainNotice.SetResourceReference(Border.BackgroundProperty,  "Bg2Brush");
            _domainNotice.SetResourceReference(Border.BorderBrushProperty, "FgHdrBrush");

            _txtAccount.TextChanged += (_, _) =>
            {
                UpdateGmsaNote();
                UpdateAccountTypes();
                UpdateDomainNotice();
                _profile.Clear();   // its findings belong to the account that was checked
                UpdateNext();
            };
            _txtPassword.PasswordChanged += (_, _) => UpdateNext();
            _profile.Changed += (_, _) => UpdateNext();
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
                UpdateNext();
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
                    PageHelpers.Subtitle("DbxTune and DbxStarterService run under this Windows account. It can be one of these — " +
                                          "the one that matches the name you enter is highlighted:"),
                    _accountTypes,
                    _domainNotice,
                    PageHelpers.Row("Account name", _txtAccount),
                    PageHelpers.Row("Password", _txtPassword, _btnCheck),
                    _lblCheckStatus,
                    _lblGmsaNote,
                    _profile),
            };

            UpdateGmsaNote();
            UpdateAccountTypes();
            UpdateDomainNotice();
            UpdateNext();
        }

        // What the AD administrator should set up for the service account, so it works the first time
        // (each point is something the installer, or DbxTune, otherwise runs into later). Uses the
        // domain account typed in the field, else DOMAIN\svc_dbxtune.
        // withPowerShell: the copied version also has the commands (the tooltip only says so).
        private string AdAdminRequest(bool withPowerShell = false)
        {
            string typed  = _txtAccount.Text.Trim();
            string domain = _domain ?? "DOMAIN";
            string acct   = ProfileCheck.IsDomainName(typed) && !typed.EndsWith('$') ? typed : $"{domain}\\svc_dbxtune";
            string server = Environment.MachineName;
            string sam    = acct.Contains('\\') ? acct.Split('\\', 2)[1] : acct.Split('@')[0];
            string dns    = _dnsDomain.Length > 0 ? _dnsDomain : "domain.com";
            const string ura = "\n       Computer Configuration > Policies > Windows Settings > Security Settings > Local Policies > User Rights Assignment";
            string text =
                $"Request for the AD administrator: a service account for DbxTune on {server}\n\n" +
                $"Please create a domain user account for the DbxTune monitoring service, e.g. {acct}:\n" +
                "  1. A normal domain user. No Domain Admins, no administrator rights needed.\n" +
                "  2. Password never expires — otherwise DbxStarterService stops when it expires.\n" +
                "     (If it must change: tell us, the new password is set with the installer's 'Upgrade / Repair'.)\n" +
                $"  3. 'Log On To' (Account tab): all computers, or include {server}.\n" +
                "  4. Not a member of 'Protected Users' (that group is not meant for service accounts).\n" +
                $"  5. Group Policy that applies to {server}, under:{ura}\n" +
                "     - 'Log on as a service': include the account (or a group it is in), if a GPO sets that right.\n" +
                "     - Not in 'Deny log on as a service'.\n" +
                "     - Preferably 'Allow log on locally' too, and not in 'Deny log on locally' / 'Deny log on through Remote\n" +
                "       Desktop Services' — so DbxStarterClient and Remote Desktop can run as the account. (The install works\n" +
                "       without it, then those run as an administrator.)\n" +
                "     - If a GPO sets 'Create symbolic links', 'Debug programs', or the members of the local groups\n" +
                "       'Performance Log Users', 'Performance Monitor Users', 'Remote Desktop Users' (Restricted Groups):\n" +
                "       include the account there too. The installer sets these locally, and a GPO would remove them again.\n" +
                $"  Or instead of 1-2: a gMSA ({domain}\\{sam}$) that {server} may use; points 3-5 apply to it too.\n\n" +
                "And on each SQL Server to monitor (by the DBA) — the full list is in the SQL Server setup text of the installer:\n" +
                $"  CREATE LOGIN [{acct}] FROM WINDOWS;\n" +
                $"  GRANT VIEW SERVER STATE, VIEW ANY DEFINITION, CONNECT ANY DATABASE TO [{acct}];\n" +
                "\nTo also monitor the Windows OS of the SQL Server hosts, the account needs on each of them: an SSH server, the local\n" +
                "groups 'Performance Log Users' and 'OpenSSH Users', and 'Debug programs' — see the installer's 'OS Monitoring' page.\n";
            if (!withPowerShell)
                return text + "\n('Copy a request for your AD administrator' copies this text, with the PowerShell commands.)";

            return text +
                "\nPowerShell (ActiveDirectory module) for points 1-4:\n" +
                $"  New-ADUser -Name '{sam}' -SamAccountName '{sam}' -UserPrincipalName '{sam}@{dns}' `\n" +
                "      -AccountPassword (Read-Host -AsSecureString 'Password') -Enabled $true `\n" +
                $"      -PasswordNeverExpires $true -CannotChangePassword $true -Description 'DbxTune monitoring service on {server}'\n" +
                $"  # 'Log On To' = all computers is the default; to restrict it to this server:\n" +
                $"  #   Set-ADUser '{sam}' -LogonWorkstations '{server}'\n" +
                $"  Get-ADPrincipalGroupMembership '{sam}' | Select-Object Name   # no 'Protected Users', no 'Deny ...' groups for this server\n" +
                "\nPowerShell for the gMSA alternative (needs a KDS root key in the domain: Get-KdsRootKey):\n" +
                $"  New-ADServiceAccount -Name '{sam}' -DNSHostName '{sam}.{dns}' -PrincipalsAllowedToRetrieveManagedPassword '{server}$'\n" +
                $"  # then on {server}:  Install-ADServiceAccount '{sam}';  Test-ADServiceAccount '{sam}'\n" +
                "\nPoint 5 (User Rights in a GPO) has no PowerShell cmdlet: use gpmc.msc. To see what applies to the server:\n" +
                $"  Get-GPResultantSetOfPolicy -Computer '{server}' -ReportType Html -Path $env:TEMP\\rsop.html\n";
        }

        private void UpdateAccountTypes()
        {
            string account = _txtAccount.Text.Trim();
            int current = account.Length == 0 ? -1 : IsGmsa ? 2 : ProfileCheck.IsDomainName(account) ? 1 : 0;
            var types = new (string Name, string Format, string When)[]
            {
                ("Local account", "dbxtune",
                 "Created by the installer on this computer. The simplest choice; but it can't use Windows authentication to a " +
                 "SQL Server on another computer."),
                ("Active Directory (domain) account", "DOMAIN\\user  or  user@domain.com",
                 "Must already exist in AD (ask your AD administrator). Recommended on a domain server: Windows authentication to " +
                 "SQL Server, no database password stored in DbxTune."),
                ("gMSA (group Managed Service Account)", "DOMAIN\\name$  (no password)",
                 "AD changes its password by itself. The AD administrator creates it and allows this server to use it."),
            };

            // One row per kind: marker | "Name   format" over an indented "when to use" line.
            _accountTypes.Children.Clear();
            _accountTypes.RowDefinitions.Clear();
            if (_accountTypes.ColumnDefinitions.Count == 0)
            {
                _accountTypes.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(22) });
                _accountTypes.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            }
            for (int i = 0; i < types.Length; i++)
            {
                bool on = i == current;
                string fg = on ? "FgHdrBrush" : "FgBrush";
                _accountTypes.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });

                var marker = new TextBlock { Text = on ? "►" : "•", Margin = new Thickness(4, 2, 0, 0) };
                marker.SetResourceReference(TextBlock.ForegroundProperty, fg);
                Grid.SetRow(marker, i);
                _accountTypes.Children.Add(marker);

                var head = new TextBlock { TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 2, 0, 0) };
                var name = new Run(types[i].Name) { FontWeight = on ? FontWeights.SemiBold : FontWeights.Normal };
                var format = new Run("      " + types[i].Format) { FontFamily = new System.Windows.Media.FontFamily("Consolas") };
                name.SetResourceReference(TextElement.ForegroundProperty, fg);
                format.SetResourceReference(TextElement.ForegroundProperty, fg);
                head.Inlines.Add(name);
                head.Inlines.Add(format);
                var when = new TextBlock { Text = types[i].When, TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 0, 0, 4) };
                when.SetResourceReference(TextBlock.ForegroundProperty, "FgDimBrush");
                var cell = new StackPanel { Background = System.Windows.Media.Brushes.Transparent };   // the whole cell shows the tooltip
                cell.Children.Add(head);
                cell.Children.Add(when);

                // AD account: everything to ask the AD administrator for, in one go — as tooltip and copyable.
                string tip = i switch
                {
                    0 => "Local account\n\nThe installer creates it (net user) with the password you enter, and gives it the rights\n" +
                         "DbxTune needs on this computer. Good for a test or a stand-alone server.\n" +
                         "It can't log in to a SQL Server on another computer with Windows authentication:\n" +
                         "DbxTune then uses a SQL Server login with a password.",
                    1 => AdAdminRequest(),
                    _ => "gMSA (group Managed Service Account)\n\nNo password to enter or to keep up to date: AD changes it by itself.\n" +
                         "The AD administrator creates it (New-ADServiceAccount) with this server in\n" +
                         "-PrincipalsAllowedToRetrieveManagedPassword, and installs it here (Install-ADServiceAccount).\n" +
                         "Since a gMSA can't log on interactively, the installer runs its set-up steps as you, for the account,\n" +
                         "and DbxStarterClient runs as you. The points in the AD account tooltip apply to a gMSA too.",
                };
                cell.ToolTip = new TextBlock { Text = tip, MaxWidth = 760, TextWrapping = TextWrapping.Wrap };
                ToolTipService.SetShowDuration(cell, 120_000);
                if (i == 1)
                {
                    var copy = new Hyperlink(new Run("Copy a request for your AD administrator (everything to set up, in one go)"));
                    var copied = new Run("");
                    copied.SetResourceReference(TextElement.ForegroundProperty, "GreenBrush");
                    copy.Click += (_, _) =>
                    {
                        try { Clipboard.SetText(AdAdminRequest(withPowerShell: true)); copied.Text = "   Copied (text + PowerShell) — paste it into a mail or a ticket."; }
                        catch { copied.Text = "   (the clipboard is busy — try again)"; }
                    };
                    var link = new TextBlock { Margin = new Thickness(0, 0, 0, 4) };
                    link.Inlines.Add(copy);
                    link.Inlines.Add(copied);
                    cell.Children.Add(link);
                }
                Grid.SetRow(cell, i);
                Grid.SetColumn(cell, 1);
                _accountTypes.Children.Add(cell);
            }
        }

        private bool IsGmsa => _txtAccount.Text.Trim().EndsWith('$');

        // Check ran for exactly what is in the fields now (a gMSA has no password to check).
        private bool CheckedCurrent => IsGmsa || (_lastCheck is { } last && last.Account == _txtAccount.Text.Trim() && last.Password == _txtPassword.Password);

        // Next only when Check passed for what is in the fields and nothing blocks the profile folder;
        // otherwise the hint beside Next says what to do first.
        private void UpdateNext()
        {
            string account = _txtAccount.Text.Trim();
            string? todo =
                account.Length == 0
                    ? "Enter the account name."
                : !IsGmsa && _txtPassword.Password.Length == 0
                    ? "Enter the password, then press Check."
                : !CheckedCurrent
                    ? "Press Check to test the account and the password before you continue."
                : !IsGmsa && _lastCheck is { Outcome: InstallActions.VerifyOutcome.Failed }
                    ? "The Check failed (see the red text) — correct the account name or the password, then press Check again."
                : _profile.CheckedAccount == account && _profile.Blocks
                    ? "Something is in the way of the profile folder (see above): use 'Clean up…', or have it fixed and press Check again."
                : null;
            SetNextEnabled(todo == null, todo ?? "");
        }

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

            var body = new Run($"So a domain account (or a gMSA) is the better choice here, for example {example} — ask your AD " +
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
            // The Check rules themselves are enforced by Next being disabled (UpdateNext).
            return Task.FromResult(WizardValidationResult.Ok());
        }
    }
}

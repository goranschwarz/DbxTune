using System;
using System.IO;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using DbxInstaller.Wizard;

namespace DbxInstaller.Pages
{
    // Informational — links to the prep guides for setting up a dedicated OS user so DbxTune
    // can collect CPU/disk/memory metrics from the monitored host. Purely informational, no
    // config fields; shown after DBMS Configuration since that's when the user has just
    // finished setting up connections for each selected server and OS-level prep is the
    // natural next step before firewall/service start.
    //
    // For Windows hosts it also lists what each monitored host needs (DbxTune logs in over SSH and
    // runs typeperf / PowerShell there — see README_dbxcentral_2_add_sql-server.md), and copies those
    // instructions, with PowerShell, for the hosts' administrator.
    internal sealed class OsMonitoringPage : WizardPage
    {
        public override string StepTitle => "OS Monitoring";

        private readonly TextBlock _windowsNeeds = new() { TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 4, 0, 4) };
        private readonly Run _copied = new("");
        private string _osUser = @"DOMAIN\dbxtune";
        private string _dbxHome = @"<DbxTune install dir>\0";

        public OsMonitoringPage()
        {
            _copied.SetResourceReference(TextElement.ForegroundProperty, "GreenBrush");
            var copy = new Hyperlink(new Run("Copy the instructions for the administrator of the SQL Server hosts (text + PowerShell)"));
            copy.Click += (_, _) =>
            {
                try { Clipboard.SetText(WindowsHostRequest(_osUser, _dbxHome)); _copied.Text = "   Copied — paste it into a mail or a ticket."; }
                catch { _copied.Text = "   (the clipboard is busy — try again)"; }
            };
            var copyLine = new TextBlock { Margin = new Thickness(0, 4, 0, 0) };
            copyLine.Inlines.Add(copy);
            copyLine.Inlines.Add(_copied);

            Content = new ScrollViewer
            {
                VerticalScrollBarVisibility = ScrollBarVisibility.Auto,
                Content = PageHelpers.Stack(
                    PageHelpers.Title("OS Monitoring Setup (Recommended)"),
                    PageHelpers.Subtitle("A dedicated OS user lets DbxTune collect CPU / disk / memory metrics from the monitored host."),
                    PageHelpers.Subtitle("This is especially useful for spotting resource usage from processes outside the database " +
                                         "engine itself — for example a misconfigured antivirus scanner or an application server " +
                                         "competing for memory — that would otherwise be invisible to monitoring that only looks at " +
                                         "the DBMS process.\n\nFollow the preparation guide for your database type:"),
                    PageHelpers.Link("→  SQL Server on Windows",
                        "https://github.com/goranschwarz/DbxTune/blob/master/README_dbxcentral_2_add_sql-server.md#for-sql-server-on-windows-preparations-to-monitor-os-step-1----recomended"),
                    PageHelpers.Link("→  Sybase ASE",
                        "https://github.com/goranschwarz/DbxTune/blob/master/README_dbxcentral_1_add_sybase.md#prepare-step-4-recomended---create-a-dedicated-os-user-for-monitoring"),
                    PageHelpers.Link("→  PostgreSQL",
                        "https://github.com/goranschwarz/DbxTune/blob/master/README_dbxcentral_3_add_postgres.md#prepare-step-2-recomended---create-a-dedicated-os-user-for-monitoring"),
                    PageHelpers.SectionHeader("Windows hosts (SQL Server on Windows): what each monitored host needs"),
                    _windowsNeeds,
                    copyLine),
            };
        }

        public override void OnEnter(WizardContext ctx)
        {
            // The OS user: the service's AD account if it is one (works on every host); a local or gMSA
            // service account can't log in to other hosts over SSH, so then suggest a dedicated one.
            string acct = ctx.Config.ServiceAccount;
            bool adUser = ProfileCheck.IsDomainName(acct) && !acct.EndsWith('$');
            string domain = GroupPolicy.JoinedDomain() ?? "DOMAIN";
            _osUser  = adUser ? acct : $"{domain}\\svc_dbxtune_os";
            _dbxHome = string.IsNullOrEmpty(ctx.Config.InstallDir) ? _dbxHome : Path.Combine(ctx.Config.InstallDir, "0");

            _windowsNeeds.Inlines.Clear();
            void Line(string text, string brush = "FgBrush")
            {
                if (_windowsNeeds.Inlines.Count > 0) _windowsNeeds.Inlines.Add(new LineBreak());
                var run = new Run(text);
                run.SetResourceReference(TextElement.ForegroundProperty, brush);
                _windowsNeeds.Inlines.Add(run);
            }
            Line("DbxTune logs in to each host over SSH and runs typeperf (CPU, disks, memory, network) and PowerShell " +
                 "(processes, disk space) there. On each SQL Server host:", "FgDimBrush");
            Line("  1.  An SSH server: OpenSSH Server (part of Windows Server 2019 and later; older: the Win32-OpenSSH MSI), " +
                 "service 'sshd' started automatically.");
            Line("  2.  TCP port 22 open from this DbxTune server to the host.");
            Line($"  3.  A user to log in with: {_osUser}" +
                 (adUser ? " (the service account — an AD account works on every host)." :
                           " — an AD account (works on every host; the service account is local or a gMSA, which can't log in over SSH)," +
                           " or a local user on each host."));
            Line("  4.  That user in the local groups 'Performance Log Users' (typeperf) and 'OpenSSH Users' (if the group " +
                 "exists — newer Windows lets only that group log in over SSH).");
            Line("  5.  The user right 'Debug programs' — without it 'OS Top Process' shows only the user's own processes. " +
                 $"Script: {Path.Combine(_dbxHome, "resources", "dbxcentral", "scripts", "fix_SeDebugPrivilede.ps1")}");
            Line("  If a GPO sets 'Debug programs' or these local groups on the hosts, add the user there (a local change is " +
                 "removed again). SSH refused with the right password? A 'Deny …' user right in a GPO is the usual cause " +
                 "(rsop.msc on that host).", "FgDimBrush");
        }

        // The same for the hosts' administrator, with PowerShell — copied to the clipboard.
        internal static string WindowsHostRequest(string osUser, string dbxHome)
        {
            string script = Path.Combine(dbxHome, "resources", "dbxcentral", "scripts", "fix_SeDebugPrivilede.ps1");
            return
                "Request: let DbxTune monitor the Windows OS of the SQL Server hosts\n\n" +
                $"DbxTune ({Environment.MachineName}) logs in to each SQL Server host over SSH as '{osUser}' and runs typeperf\n" +
                "(CPU, disks, memory, network) and PowerShell (processes, disk space). On EACH SQL Server host, please:\n" +
                "  1. Install the OpenSSH Server (part of Windows Server 2019 and later; older versions: the Win32-OpenSSH MSI from\n" +
                "     https://github.com/PowerShell/Win32-OpenSSH/releases), with the service 'sshd' started automatically.\n" +
                $"  2. Open TCP port 22 from {Environment.MachineName} to the host.\n" +
                $"  3. Let '{osUser}' log in (an AD account; or create a local user with the same name on each host).\n" +
                $"  4. Add '{osUser}' to the local groups 'Performance Log Users' and 'OpenSSH Users' (if that group exists).\n" +
                $"  5. Grant '{osUser}' the user right 'Debug programs' (secpol.msc > Local Policies > User Rights Assignment),\n" +
                "     so DbxTune sees the CPU of all processes, not only its own.\n" +
                "  If a GPO sets 'Debug programs' or these local groups on the hosts, add the user there instead — a local change\n" +
                "  is removed at the next policy refresh. If SSH refuses the login with the right password, a 'Deny ...' user right\n" +
                "  in a GPO is the usual cause (rsop.msc on that host).\n\n" +
                "PowerShell, on each SQL Server host (as administrator):\n" +
                "  Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0          # Windows Server 2019 and later\n" +
                "  Set-Service sshd -StartupType Automatic; Start-Service sshd\n" +
                "  Get-NetFirewallRule -Name *OpenSSH* | Select-Object Name, Enabled, Direction   # the install adds a TCP 22 rule\n" +
                $"  Add-LocalGroupMember -Group 'Performance Log Users' -Member '{osUser}'\n" +
                $"  if (Get-LocalGroup 'OpenSSH Users' -ErrorAction SilentlyContinue) {{ Add-LocalGroupMember -Group 'OpenSSH Users' -Member '{osUser}' }}\n" +
                $"  .\\fix_SeDebugPrivilede.ps1 -Username '{osUser}'   # 'Debug programs' — copy it from {script}\n\n" +
                $"Test, from {Environment.MachineName}:\n" +
                $"  ssh -l '{osUser}' <sql-server-host> \"typeperf -sc 1 \\\"\\Processor(_Total)\\% Processor Time\\\"\"\n" +
                $"  ssh -l '{osUser}' <sql-server-host> \"powershell -Command Get-Process | Select-Object -First 5\"\n";
        }

        public override Task<WizardValidationResult> ValidateBeforeNextAsync(WizardContext ctx) =>
            Task.FromResult(WizardValidationResult.Ok());
    }
}

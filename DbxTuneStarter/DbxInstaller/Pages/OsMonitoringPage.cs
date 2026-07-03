using System.Threading.Tasks;
using DbxInstaller.Wizard;

namespace DbxInstaller.Pages
{
    // Informational — links to the prep guides for setting up a dedicated OS user so DbxTune
    // can collect CPU/disk/memory metrics from the monitored host. Purely informational, no
    // config fields; shown after DBMS Configuration since that's when the user has just
    // finished setting up connections for each selected server and OS-level prep is the
    // natural next step before firewall/service start.
    internal sealed class OsMonitoringPage : WizardPage
    {
        public override string StepTitle => "OS Monitoring";

        public OsMonitoringPage()
        {
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
                    "https://github.com/goranschwarz/DbxTune/blob/master/README_dbxcentral_3_add_postgres.md#prepare-step-2-recomended---create-a-dedicated-os-user-for-monitoring"));
        }

        public override Task<WizardValidationResult> ValidateBeforeNextAsync(WizardContext ctx) =>
            Task.FromResult(WizardValidationResult.Ok());
    }
}

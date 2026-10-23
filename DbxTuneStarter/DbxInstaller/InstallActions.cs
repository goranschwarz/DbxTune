using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Net.Sockets;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Security;
using System.Security.Principal;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Microsoft.Win32;

namespace DbxInstaller
{
    internal enum InstallMode { Install, Upgrade, Remove }

    internal class InstallConfig
    {
        // Wizard entry-point selection (Welcome page)
        public InstallMode Mode { get; set; } = InstallMode.Install;

        // Java
        public string JavaExe        { get; set; } = "java";

        // Package
        public bool   DownloadZip    { get; set; } = true;
        public string ZipUrl         { get; set; } = "https://sourceforge.net/projects/asetune/files/latest/download";
        public string ZipLocalPath   { get; set; } = "";

        // Service account
        public string ServiceAccount { get; set; } = "dbxtune";
        public string Password       { get; set; } = "";
        public string InstallDir     { get; set; } = @"C:\Users\dbxtune\dbxtune_sw";


        // Command to run after extraction to initialise ~/.dbxtune structure
        public string InitCommand    { get; set; } = "";

        // DbxTune environment variables
        public string DbxUserHome   { get; set; } = "";
        public string DbxSaveDir    { get; set; } = "";
        public string DbxReportsDir { get; set; } = "";
        public string DbxLogDir     { get; set; } = "";
        public string DbxConfDir    { get; set; } = "";
        public string DbxInfoDir    { get; set; } = "";

        // DBMS servers the user wants to configure
        public List<string> SelectedDbms { get; set; } = new();

        // DbxStarterService web UI settings (written to appsettings.json next to the service exe)
        // WebPort < 0  →  HTTP server is not started at all
        // WebBind      →  "localhost" (loopback only) or "*" (all network interfaces)
        public bool   ManageDbxCentral { get; set; } = true;
        public int    WebPort { get; set; } = 8055;
        public string WebBind { get; set; } = "localhost";

        // Resolved at runtime by installer steps
        public string ResolvedZipPath { get; set; } = "";
        public string VersionDir      { get; set; } = ""; // e.g. "dbxtune-3.12.1"; set by ExtractPackage
        public string ProfilePath     { get; set; } = ""; // actual profile dir, e.g. C:\Users\dbxtune.000; set by CreateUserProfile

        // Set by RunInitCommand ("Initialize DbxTune home") when the init command exits 0 but its
        // output still contains " - ERROR " log lines or a Java stack trace — a "silent failure"
        // that wouldn't otherwise stop the install. Read by InstallProgressPage to show a big
        // warning after "Installation complete" so it isn't missed.
        public bool   InitCommandHasWarnings { get; set; }
        public string InitCommandWarningDetail { get; set; } = "";

        // Set by CheckLogonRights ("Check logon rights") when a domain Group Policy sets one of the
        // rights/local groups the installer set up, without this account — the local setting is
        // replaced at the next policy refresh. Empty = nothing to warn about. Shown as a big
        // warning after "Installation complete" / "Upgrade complete".
        public string PolicyWarningDetail { get; set; } = "";

        // Set by CheckLogonRights when a 'Deny log on locally' policy blocks the account's interactive
        // logon while 'Log on as a service' works: the steps that would run AS the account (init
        // command, DBMS config wizard, impersonated file creation, launching the client) then run as
        // the installing admin instead — with the account's folders, and icacls for the account —
        // the same way as for a gMSA (RunAsAdmin).
        public bool NoInteractiveLogon { get; set; }

        // Set by the Progress page before running steps. Only the download in LocatePackage
        // actually observes this — it's the one step whose long-running work can be aborted
        // mid-flight; everything else is a quick synchronous OS call that runs to completion
        // once started, so cancellation there just means "stop before the next step".
        public System.Threading.CancellationToken CancelToken { get; set; }
    }

    internal delegate void InstallLog(string message);

    internal enum StepKind { Header, Action }

    internal sealed record StepDef(
        string Name,
        StepKind Kind,
        string Description = "",
        Func<InstallConfig, InstallLog, Task<bool>>? Run = null);

    internal sealed record DbmsProfile(
        string DisplayName,   // e.g. "SQL Server"
        string Tooltip,       // hover description
        string TuneExe,       // e.g. "sqlservertune" (no extension)
        string StartScript,   // e.g. "start_sqlservertune.bat"
        string SetupFile);    // embedded resource filename in DbmsSetup\

    internal static class InstallActions
    {
        // Companion programs bundled with the installer.
        // Add new entries here as more tools are created (DbxUpgrade, etc.).
        private static readonly string[] CompanionExeNames =
        {
            "DbxStarterService",
            "DbxStarterClient",
        };

        // Per-DBMS metadata. SetupFile = embedded resource in DbmsSetup\ opened in Notepad.
        public static readonly DbmsProfile[] DbmsProfiles =
        {
            new("SQL Server",       "Microsoft SQL Server",                  "sqlservertune", "start_sqlservertune.bat", "sqlserver.txt"),
            new("PostgreSQL",       "PostgreSQL open-source RDBMS",          "postgrestune",  "start_postgrestune.bat",  "postgres.txt"),
            new("Sybase ASE",       "SAP / Sybase Adaptive Server Enterprise","asetune",      "start_asetune.bat",       "ase.txt"),
            new("Sybase RepServer", "Sybase Replication Server",             "rstune",        "start_rstune.bat",        "repserver.txt"),
            new("MySQL / MariaDB",  "MySQL or MariaDB database server",      "mysqltune",     "start_mysqltune.bat",     "mysql.txt"),
            new("Oracle",           "Oracle Database",                       "oracletune",    "start_oracletune.bat",    "oracle.txt"),
            new("DB2",              "IBM Db2 database server",               "db2tune",       "start_db2tune.bat",       "db2.txt"),
            new("Sybase IQ",        "SAP / Sybase IQ analytics server",      "iqtune",        "start_iqtune.bat",        "iq.txt"),
            new("SAP HANA",         "SAP HANA in-memory database",           "hanatune",      "start_hanatune.bat",      "hana.txt"),
            new("Sybase RepAgentX", "Sybase RepAgent Extended",              "raxtune",       "start_raxtune.bat",       "raxtune.txt"),
        };

        // Derived string list kept for backward compatibility with UI code.
        public static readonly string[] DbmsTypes =
            DbmsProfiles.Select(p => p.DisplayName).ToArray();



        public static readonly IReadOnlyList<StepDef> Steps = new StepDef[]
        {
            new("System Requirements",                 StepKind.Header),
            new("Check web UI port",                   StepKind.Action,
                "Verifies that port <WebPort> is not already bound by another process.\n\n" +
                "Attempts to temporarily listen on <WebBind>:<WebPort> and releases\n" +
                "the socket immediately — if another process already owns the port,\n" +
                "the step fails and reports the conflicting PID and process name.\n\n" +
                "Skipped when the web UI is disabled (WebPort = −1).",
                CheckWebPort),
            new("Check Java 17+",                      StepKind.Action,
                "Runs: <JavaExe> -version\n\n" +
                "Java is located in this priority order:\n" +
                "  1. %DBXTUNE_JAVA_HOME%\\bin\\java.exe\n" +
                "  2. %JAVA_HOME%\\bin\\java.exe\n" +
                "  3. java  (system PATH)\n\n" +
                "The detected path is shown in the Java executable field above.\n" +
                "Requires Java 17 or later.\n\n" +
                "Download: https://docs.microsoft.com/java/openjdk/download",
                CheckJava),

            new("Service Account",                     StepKind.Header),
            new("Create / verify service account",     StepKind.Action,
                "Local account  →  net user \"<account>\" <password> /add\n" +
                "                   Home directory: C:\\Users\\<account>\n" +
                "Domain / gMSA  →  Skipped — account must already exist in Active Directory.\n" +
                "If the account already exists: verifies the supplied password is correct.",
                CreateServiceAccount),
            new("Create user profile",                 StepKind.Action,
                "Ensures C:\\Users\\<account>\\ and NTUSER.DAT exist (via CreateProfile API).\n" +
                "Required so that RunAs, user environment variables and the registry hive work correctly.\n" +
                "Checks first that C:\\Users\\<account> is free (or already this account's profile) — a left-over\n" +
                "profile, local account or folder stops the step; 'Check' on the Service Account page can clean it up.",
                CreateUserProfile),
            new("Grant SeCreateSymbolicLinkPrivilege", StepKind.Action,
                "Writes to Local Security Policy via LsaAddAccountRights.\n" +
                "Lets the service account create the '0' version junction:\n" +
                "  <InstallDir>\\0  →  <InstallDir>\\<version>",
                GrantSymlinkPrivilege),
            new("Grant SeServiceLogonRight",           StepKind.Action,
                "Writes to Local Security Policy via LsaAddAccountRights.\n" +
                "Required for:  sc config DbxStarterService obj= \".\\<account>\"",
                GrantServiceLogon),
            new("Grant SeInteractiveLogonRight",       StepKind.Action,
                "Writes to Local Security Policy via LsaAddAccountRights  (\"Allow log on locally\").\n" +
                "Required because the installer runs 'Initialize DbxTune home' and the DBMS config\n" +
                "wizard as the service account, and starts DbxStarterClient as that account.\n" +
                "Skipped for gMSA accounts (they can't log on interactively).",
                GrantInteractiveLogon),
            new("Add to local groups",                 StepKind.Action,
                "net localgroup \"Performance Log Users\" <account> /add\n" +
                "  → Enables typeperf, PDH data-collector sets, performance counter logging\n\n" +
                "net localgroup \"Performance Monitor Users\" <account> /add\n" +
                "  → Enables real-time WMI / PerfMon counter queries\n\n" +
                "net localgroup \"Remote Desktop Users\" <account> /add\n" +
                "  → Allows the service account to log in via Remote Desktop (RDP)\n" +
                "  → Useful for troubleshooting DbxTune directly as the service user",
                AddPerformanceGroups),
            new("Grant SeDebugPrivilege",              StepKind.Action,
                "Writes to Local Security Policy via LsaAddAccountRights.\n" +
                "Enables OpenProcess() on any PID including SYSTEM-owned processes.\n" +
                "Needed when DbxTune reads performance counters directly from database process handles.",
                GrantDebugPrivilege),
            new("Check logon rights",                  StepKind.Action,
                "1. Logs on as the service account (service logon + interactive logon) to prove that the\n" +
                "   password and the rights granted above work NOW. Stops the install with the exact cause\n" +
                "   (wrong password, 'Deny log on ...' policy, disabled/locked account, 'Log On To' list, …).\n" +
                "   Skipped for gMSA accounts (no password).\n\n" +
                "2. Domain-joined machines: reads the Group Policy result (RSoP, same data as rsop.msc).\n" +
                "   A user right or local group that a domain GPO sets is REPLACED at every policy refresh,\n" +
                "   so the local grants above are lost then. If a GPO sets one of them without this account,\n" +
                "   a WARNING is shown after the install, listing what to ask the AD administrator for.",
                CheckLogonRights),

            new("DbxTune Package",                     StepKind.Header),
            new("Stop service if running",             StepKind.Action,
                "1. Kill all DbxStarterClient.exe processes (prevents file locks)\n" +
                "2. sc stop DbxStarterService\n" +
                "Ensures no files in the install directory are locked during extraction.",
                StopServiceIfRunning),
            new("Locate package",                      StepKind.Action,
                "Download mode  →  Downloads URL to  %TEMP%\\DbxTune-latest.zip\n" +
                "Local mode     →  Verifies the specified ZIP file exists and is readable",
                LocatePackage),
            new("Create directory structure",          StepKind.Action,
                "Creates the following directories as the service account (native ownership):\n" +
                "  <InstallDir>                              ← software root\n" +
                "  <DbxUserHome>                            ← DBXTUNE_USER_HOME\n" +
                "  <DbxUserHome>\\dbxc\\data                 ← DBXTUNE_CENTRAL_SAVE_DIR\n" +
                "  <DbxUserHome>\\dbxc\\reports              ← DBXTUNE_CENTRAL_REPORTS_DIR\n" +
                "  <DbxUserHome>\\dbxc\\log                  ← DBXTUNE_CENTRAL_LOG_DIR\n" +
                "  <DbxUserHome>\\dbxc\\conf                 ← DBXTUNE_CENTRAL_CONF_DIR\n" +
                "  <DbxUserHome>\\dbxc\\info                 ← DBXTUNE_CENTRAL_INFO_DIR",
                CreateDirectories),
            new("Extract package",                     StepKind.Action,
                "Extracts the DbxTune ZIP into a versioned subdirectory:\n" +
                "  <InstallDir>\\<version>   ← all ZIP contents, owned by service account\n" +
                "  <InstallDir>\\0           ← junction (mklink /J) pointing to <version>\n\n" +
                "Extraction runs impersonated as the service account so files are natively owned.\n" +
                "The '0' junction is created as admin (no symlink privilege required for /J).",
                ExtractPackage),
            new("Set DbxTune directories",             StepKind.Action,
                "Writes user environment variables to HKU\\<SID>\\Environment for '<account>':\n" +
                "  DBXTUNE_HOME                = <InstallDir>\\0\n" +
                "  DBXTUNE_USER_HOME           = <DbxUserHome>\n" +
                "  DBXTUNE_CENTRAL_SAVE_DIR    = <DbxUserHome>\\dbxc\\data\n" +
                "  DBXTUNE_CENTRAL_REPORTS_DIR = <DbxUserHome>\\dbxc\\reports\n" +
                "  DBXTUNE_CENTRAL_LOG_DIR     = <DbxUserHome>\\dbxc\\log\n" +
                "  DBXTUNE_CENTRAL_CONF_DIR    = <DbxUserHome>\\dbxc\\conf\n" +
                "  DBXTUNE_CENTRAL_INFO_DIR    = <DbxUserHome>\\dbxc\\info",
                SetDbxEnvVars),
            new("Initialize DbxTune home",             StepKind.Action,
                "Runs as '<account>':\n" +
                "  <InstallDir>\\0\\bin\\dbxcentral.bat --createAppDir\n\n" +
                "Populates <DbxUserHome> with DBXTUNE.env.bat, default config files,\n" +
                "and collector start scripts under <DbxUserHome>\\dbxc\\bin\\.",
                RunInitCommand),
            new("Verify DbxTune directories",          StepKind.Action,
                "Checks that all DBXTUNE_CENTRAL_* directories exist; creates any that are missing.\n" +
                "Also reads  <DbxUserHome>\\DBXTUNE.env.bat  and verifies that the\n" +
                "built-in DBXTUNE_HOME default matches  <InstallDir>\\0.",
                VerifyDbxDirs),

            new("DbxStarter Companion",                StepKind.Header),
            new("Copy DbxStarter executables",         StepKind.Action,
                "Copies into  <InstallDir>\\win\\bin\\\n" +
                "  DbxStarterService.exe   ← Windows service that manages DbxTune processes\n" +
                "  DbxStarterClient.exe    ← GUI client for managing the service\n\n" +
                "Source: installer's own directory, or sibling project build output.",
                CopyExecutables),
            new("Register Windows service",            StepKind.Action,
                "sc create DbxStarterService\n" +
                "    binPath= \"<InstallDir>\\win\\bin\\DbxStarterService.exe\"\n" +
                "    start=   auto\n\n" +
                "Registers DbxStarterService to start automatically with Windows.",
                RegisterService),
            new("Write service config",                StepKind.Action,
                "Writes  <DbxConfDir>\\DbxStarterService.json  with web UI settings:\n\n" +
                "  WebPort          = <WebPort>   (−1 disables the HTTP server)\n" +
                "  WebBind          = <WebBind>   (localhost = loopback only, * = all interfaces)\n" +
                "  ManageDbxCentral = true        (set false if this node runs collectors only)\n\n" +
                "Fresh install  — file is created from installer values.\n" +
                "Re-install     — existing keys are preserved; WebPort and WebBind always\n" +
                "                 reflect the current installer UI choices.\n\n" +
                "Also writes the config directory path into the service registry so\n" +
                "DbxStarterService.exe can locate its config file after every upgrade:\n" +
                "  HKLM\\SYSTEM\\CurrentControlSet\\Services\\DbxStarterService\\Parameters\\ConfigDir\n" +
                "    = <DbxConfDir>",
                WriteServiceConfig),
            new("Configure service account",           StepKind.Action,
                "sc config DbxStarterService obj= \".\\<account>\" password= ***\n\n" +
                "Sets the service to run under the service account.\n" +
                "Configures failure recovery: restart/60s → restart/60s → reboot/60s.",
                ConfigureServiceAccount),

            new("Finish",                              StepKind.Header),
            new("Configure firewall",                  StepKind.Action,
                "Opens Windows Firewall inbound rules for DbxTune services.\n\n" +
                "DbxTune Central (Jetty HTTP server)\n" +
                "  Reads port from <DbxConfDir>\\DBX_CENTRAL.conf\n" +
                "    key: DbxTuneCentral.web.http.port.windows  (default: 80)\n" +
                "  A rule is always added — Central is a monitoring server intended for network access.\n\n" +
                "DbxTune Central HTTPS\n" +
                "  key: DbxTuneCentral.web.https.port.windows  (default: 443)\n" +
                "  Only added when the key is present in DBX_CENTRAL.conf (opt-in — requires SSL setup).\n\n" +
                "DbxStarterService Web UI\n" +
                "  Port: <WebPort>  (default 8055)\n" +
                "  Only added when WebBind = * (all interfaces); skipped for localhost.\n\n" +
                "Rules are named with the 'DbxTune - ' prefix so the uninstaller can\n" +
                "remove them by name without affecting unrelated rules.",
                ConfigureFirewall),
        };

        // Names of the Action steps executed automatically on the Installing/Upgrading (Progress)
        // page. "Configure firewall" is deliberately excluded — it runs on the Firewall + Start
        // Service page instead, alongside the (now interactive) start-service/launch-client choice.
        public static readonly string[] ProgressStepNames = Steps
            .Where(s => s.Kind == StepKind.Action && s.Name != "Configure firewall")
            .Select(s => s.Name)
            .ToArray();

        // ── steps ─────────────────────────────────────────────────────────────

        public enum PortStatus
        {
            Free,               // port is not bound — safe to use
            OwnedByDbxStarter,  // port is held by DbxStarterService — OK, it will be stopped
            InUseByOther,       // port is held by an unrelated process — block install
        }

        // Fast check: TcpListener (~0 ms) + process-name lookup (~2 ms).
        // Safe to call on every keystroke / arrow-key press in the UI.
        public static PortStatus GetPortStatus(int port, string webBind)
        {
            bool anyNet = webBind.Equals("*",   StringComparison.OrdinalIgnoreCase)
                       || webBind.Equals("any", StringComparison.OrdinalIgnoreCase);
            var address = anyNet ? IPAddress.Any : IPAddress.Loopback;

            TcpListener? listener = null;
            bool portFree;
            try
            {
                listener = new TcpListener(address, port);
                listener.Start();
                listener.Stop();
                portFree = true;
            }
            catch (SocketException) { portFree = false; }
            finally { try { listener?.Stop(); } catch { } }

            if (portFree) return PortStatus.Free;

            // Port is occupied — check if it is our own service
            try
            {
                if (Process.GetProcessesByName("DbxStarterService").Length > 0)
                    return PortStatus.OwnedByDbxStarter;
            }
            catch { /* can't enumerate processes — fall through */ }

            return PortStatus.InUseByOther;
        }

        // Full check — same as GetPortStatus but also does netstat owner lookup on conflict.
        // Returns (free: true, owner: null) if the port is available or held by DbxStarterService.
        // Returns (free: false, owner: "PID 1234 (nginx)") if held by an unrelated process.
        public static (bool Free, string? Owner) CheckPortAvailable(int port, string webBind)
        {
            var status = GetPortStatus(port, webBind);
            if (status == PortStatus.Free)               return (true,  null);
            if (status == PortStatus.OwnedByDbxStarter)  return (true,  null);   // our service — not a real conflict
            // InUseByOther — do the slow netstat lookup for a useful error message
            string? owner = FindPortOwner(port);
            return (false, owner);
        }

        // Plain free/occupied check for a TCP port bound on all interfaces — used for DbxCentral's
        // own HTTP port (typically 80). Deliberately doesn't reuse GetPortStatus: its
        // "OwnedByDbxStarter" branch only checks whether a DbxStarterService process exists at
        // all, not whether it's actually bound to this specific port. Here we apply the same
        // "our own process holding it isn't a real conflict" idea, but for the right process:
        // DbxCentral runs as a Java process, so LikelyDbxCentral is true when the actual port
        // owner (identified via netstat, same as FindPortOwner elsewhere) looks like java/javaw —
        // e.g. because Central is already running from a previous install.
        public static (bool Free, string? Owner, bool LikelyDbxCentral) CheckLocalPortAvailable(int port)
        {
            TcpListener? listener = null;
            bool free;
            try
            {
                listener = new TcpListener(IPAddress.Any, port);
                listener.Start();
                listener.Stop();
                free = true;
            }
            catch (SocketException) { free = false; }
            finally { try { listener?.Stop(); } catch { } }

            if (free) return (true, null, false);

            string? owner = FindPortOwner(port);
            bool likelyDbxCentral = owner != null &&
                (owner.Contains("(java)", StringComparison.OrdinalIgnoreCase) ||
                 owner.Contains("(javaw)", StringComparison.OrdinalIgnoreCase));
            return (false, owner, likelyDbxCentral);
        }

        // Identify the process listening on a given TCP port using netstat -ano.
        private static string? FindPortOwner(int port)
        {
            try
            {
                var (_, output) = Run("netstat", "-ano -p TCP");
                if (string.IsNullOrEmpty(output)) return null;

                // Match lines like:  TCP  0.0.0.0:8055  0.0.0.0:0  LISTENING  1234
                var pattern = new Regex(
                    $@":\b{Regex.Escape(port.ToString())}\b\s+\S+\s+LISTENING\s+(\d+)",
                    RegexOptions.IgnoreCase);

                var match = pattern.Match(output);
                if (!match.Success) return null;

                int pid = int.Parse(match.Groups[1].Value);
                try
                {
                    string name = Process.GetProcessById(pid).ProcessName;
                    return $"PID {pid} ({name})";
                }
                catch
                {
                    return $"PID {pid}";
                }
            }
            catch { return null; }
        }

        private static Task<bool> CheckWebPort(InstallConfig c, InstallLog log)
        {
            if (c.WebPort < 0)
            {
                log("Web UI disabled — port check skipped.");
                return Task.FromResult(true);
            }

            bool anyNet = c.WebBind.Equals("*",   StringComparison.OrdinalIgnoreCase)
                       || c.WebBind.Equals("any", StringComparison.OrdinalIgnoreCase);
            string bindLabel = anyNet ? "0.0.0.0" : "localhost";

            log($"Checking port {c.WebPort} on {bindLabel}…");

            switch (GetPortStatus(c.WebPort, c.WebBind))
            {
                case PortStatus.Free:
                    log($"  Port {c.WebPort} is free.");
                    return Task.FromResult(true);

                case PortStatus.OwnedByDbxStarter:
                    log($"  Port {c.WebPort} is held by DbxStarterService — it will be stopped in a later step.");
                    return Task.FromResult(true);

                default: // InUseByOther
                    string? owner = FindPortOwner(c.WebPort);
                    string  who   = owner != null ? $" — held by {owner}" : "";
                    throw new Exception(
                        $"Port {c.WebPort} is already in use on {bindLabel}{who}.\n" +
                        "Options:\n" +
                        $"  • Change the Web UI port in the installer (currently {c.WebPort})\n" +
                        $"  • Stop the conflicting process{(owner != null ? $" ({owner})" : "")}\n" +
                        "  • Disable the Web UI entirely");
            }
        }

        // Try DBXTUNE_JAVA_HOME, then JAVA_HOME, then system PATH. Returns (exe path, source label).
        public static (string exe, string source) FindJavaExe()
        {
            var candidates = new[]
            {
                ("DBXTUNE_JAVA_HOME", Environment.GetEnvironmentVariable("DBXTUNE_JAVA_HOME")),
                ("JAVA_HOME",         Environment.GetEnvironmentVariable("JAVA_HOME")),
            };
            foreach (var (varName, home) in candidates)
            {
                if (string.IsNullOrEmpty(home)) continue;
                string path = Path.Combine(home, "bin", "java.exe");
                if (File.Exists(path)) return (path, $"%{varName}%\\bin");
            }
            return ("java", "system PATH");
        }

        // Returns true if Microsoft.AspNetCore.App 10.x is present in the dotnet shared runtime folder.
        // DbxStarterService requires this runtime; DbxInstaller itself only needs the Desktop runtime.
        public static bool IsAspNetCore10Installed()
        {
            var roots = new[]
            {
                Environment.GetEnvironmentVariable("DOTNET_ROOT"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "dotnet"),
            };

            foreach (var root in roots)
            {
                if (string.IsNullOrEmpty(root)) continue;
                var dir = Path.Combine(root, "shared", "Microsoft.AspNetCore.App");
                if (!Directory.Exists(dir)) continue;
                if (Directory.EnumerateDirectories(dir)
                        .Any(d => Path.GetFileName(d).StartsWith("10.", StringComparison.Ordinal)))
                    return true;
            }
            return false;
        }

        private static Task<bool> CheckJava(InstallConfig c, InstallLog log)
        {
            log($"Running: {c.JavaExe} -version");
            var (_, output) = Run(c.JavaExe, "-version");
            if (string.IsNullOrEmpty(output))
                throw new Exception(
                    "Java not found.\n\n" +
                    "Install Java 17 or later, then re-run the installer.\n\n" +
                    "Recommended: Microsoft Build of OpenJDK\n" +
                    "  https://docs.microsoft.com/java/openjdk/download\n\n" +
                    "Or browse to an existing java.exe using the field above.");

            log(output);

            var match = Regex.Match(output, @"version ""(?:1\.)?(\d+)");
            if (!match.Success)
                throw new Exception("Could not parse Java version from output.");

            int major = int.Parse(match.Groups[1].Value);
            if (major < 17)
                throw new Exception($"Java {major} detected — Java 17 or later is required. Please upgrade.");

            log($"Java {major} — OK.");
            return Task.FromResult(true);
        }

        // DbxStarterService.StopAsync's own wait-for-monitored-processes budget is 180s
        // (see DbxStarterService.cs) — give it a little headroom beyond that.
        internal const int ServiceStopTimeoutMs = 200_000;

        private static Task<bool> StopServiceIfRunning(InstallConfig c, InstallLog log)
        {
            // ── Stop DbxStarterClient (GUI) ───────────────────────────────────────
            var clientProcs = System.Diagnostics.Process.GetProcessesByName("DbxStarterClient");
            if (clientProcs.Length > 0)
            {
                log($"  DbxStarterClient is running ({clientProcs.Length} process(es)) — closing…");
                foreach (var p in clientProcs)
                {
                    try
                    {
                        p.Kill();
                        p.WaitForExit(3000);
                        log($"    PID {p.Id} terminated.");
                    }
                    catch (Exception ex) { log($"    Warning: could not kill PID {p.Id}: {ex.Message}"); }
                    finally { p.Dispose(); }
                }
            }
            else
            {
                log("  DbxStarterClient is not running.");
            }

            // ── Stop DbxStarterService (Windows service) ──────────────────────────
            const string svcName = "DbxStarterService";

            var (exists, queryOut) = Run("sc", $"query {svcName}");
            if (!exists)
            {
                log($"  {svcName} is not installed — nothing to stop.");
                return Task.FromResult(true);
            }

            bool running = queryOut.Contains("RUNNING", StringComparison.OrdinalIgnoreCase);
            if (!running)
            {
                log($"  {svcName} is installed but not running — no action needed.");
                return Task.FromResult(true);
            }

            log($"  {svcName} is running — stopping before file operations…");
            var (_, stopOut) = Run("sc", $"stop {svcName}");
            if (!string.IsNullOrWhiteSpace(stopOut)) log($"  {stopOut}");

            bool stopped = WaitForServiceStopped(svcName, ServiceStopTimeoutMs, log);
            if (!stopped)
            {
                log($"  ERROR: {svcName} did not stop within {ServiceStopTimeoutMs / 1000}s — " +
                    "it (or a monitored server process it manages) is still running. Aborting " +
                    "before touching any files, since extracting/repointing the version link now " +
                    "could orphan a still-running process against the old version directory.");
                return Task.FromResult(false);
            }

            log("  Service stopped — OK.");
            return Task.FromResult(true);
        }

        // Polls `sc query <svcName>` until it reports a non-transitional state (stopped,
        // uninstalled, etc.) or the timeout elapses.
        //
        // DbxStarterService.StopAsync gracefully stops every monitored DBMS server process
        // (via an HTTP shutdown request each) before it reports itself stopped, and that can
        // legitimately take up to ~180s for a slow-to-exit process. A short fixed sleep here
        // was the root cause of a nasty bug: if extraction/repoint (during upgrade) or file
        // removal (during uninstall) proceeded before the service — and the processes it
        // manages — had actually finished exiting, a still-running old-version process would
        // keep a file open under the versioned directory indefinitely, silently blocking that
        // one directory from ever being deleted, upgrade after upgrade.
        internal static bool WaitForServiceStopped(string svcName, int timeoutMs, InstallLog log)
        {
            var sw = System.Diagnostics.Stopwatch.StartNew();
            while (true)
            {
                var (exists, queryOut) = Run("sc", $"query {svcName}");
                if (!exists) return true; // uninstalled mid-wait — nothing left to wait for

                bool stillGoing = queryOut.Contains("RUNNING", StringComparison.OrdinalIgnoreCase)
                                || queryOut.Contains("STOP_PENDING", StringComparison.OrdinalIgnoreCase);
                if (!stillGoing) return true;

                if (sw.ElapsedMilliseconds >= timeoutMs) return false;

                log($"  Still stopping… ({sw.Elapsed:mm\\:ss} elapsed, waiting up to {timeoutMs / 1000}s)");
                System.Threading.Thread.Sleep(2000);
            }
        }

        // Files every DbxTune package has at the top level of its ZIP — ExtractPackage unpacks the ZIP
        // into <InstallDir>\<version>, and the rest of the install uses <InstallDir>\0\bin\dbxcentral.bat.
        private static readonly string[] DbxTunePackageFiles = { "bin/dbxcentral.bat", "bin/dbxtune.bat", "lib/dbxtune.jar" };

        // null when the ZIP is a DbxTune package, else why not — e.g. the DbxTuneStarter ZIP (this
        // installer, the service and the client), which does not contain the DbxTune software.
        // Reads only the ZIP's directory (fast, also for a big ZIP).
        public static string? CheckDbxTunePackage(string zipPath)
        {
            List<string> names;
            try
            {
                using var zip = ZipFile.OpenRead(zipPath);
                names = zip.Entries.Select(e => e.FullName.Replace('\\', '/')).ToList();
            }
            catch (Exception ex)
            {
                return $"'{Path.GetFileName(zipPath)}' is not a readable ZIP file ({ex.Message}) — a broken download, or a web page " +
                       "saved instead of the ZIP?";
            }

            const string expectedName = "the DbxTune software package (dbxtune_<date>.zip, from https://sourceforge.net/projects/asetune/files/)";
            bool Has(string name) => names.Any(n => n.Equals(name, StringComparison.OrdinalIgnoreCase));
            if (DbxTunePackageFiles.All(Has)) return null;

            if (names.Any(n => Path.GetFileName(n).Equals("DbxInstaller.exe", StringComparison.OrdinalIgnoreCase)))
                return $"'{Path.GetFileName(zipPath)}' is the DbxTuneStarter package (DbxInstaller, DbxStarterService, DbxStarterClient) — " +
                       $"it does not contain the DbxTune software. Choose {expectedName}.";

            // The right files, but one folder down (a re-zipped package: "dbxtune/bin/dbxcentral.bat")
            string? nested = names.Select(n => n.Split('/')[0]).Distinct(StringComparer.OrdinalIgnoreCase)
                .FirstOrDefault(top => DbxTunePackageFiles.All(f => Has($"{top}/{f}")));
            if (nested != null)
                return $"'{Path.GetFileName(zipPath)}' has the DbxTune files inside the folder '{nested}/', but the installer needs them " +
                       $"at the top of the ZIP (bin/, lib/, …). Use {expectedName} as downloaded, not re-zipped.";

            return $"'{Path.GetFileName(zipPath)}' is not a DbxTune package: it doesn't have " +
                   $"{string.Join(", ", DbxTunePackageFiles.Where(f => !Has(f)))}. Choose {expectedName}.";
        }

        private static async Task<bool> LocatePackage(InstallConfig c, InstallLog log)
        {
            if (!c.DownloadZip)
            {
                if (!File.Exists(c.ZipLocalPath))
                    throw new Exception($"ZIP file not found: {c.ZipLocalPath}");
                c.ResolvedZipPath = c.ZipLocalPath;
                log($"Using local file: {c.ZipLocalPath}");
                log($"  Size: {new FileInfo(c.ZipLocalPath).Length / 1024:N0} KB");
                if (CheckDbxTunePackage(c.ZipLocalPath) is string notLocal) throw new Exception(notLocal);
                log("  It is a DbxTune package (bin/dbxcentral.bat, bin/dbxtune.bat, lib/dbxtune.jar) — OK.");
                return true;
            }

            string dest = Path.Combine(Path.GetTempPath(), "DbxTune-latest.zip");
            log($"Downloading: {c.ZipUrl}");
            log($"         to: {dest}");

            int lastPct = -1;
            await DownloadFileAsync(c.ZipUrl, dest, pct =>
            {
                if (pct == lastPct) return;
                lastPct = pct;
                log(pct >= 0 ? $"  {pct}%…" : "  Downloading…");
            }, c.CancelToken);

            c.ResolvedZipPath = dest;
            log($"Download complete — {new FileInfo(dest).Length / 1024:N0} KB");
            // A wrong URL can download an HTML page or another ZIP: check before anything is unpacked
            if (CheckDbxTunePackage(dest) is string notDownloaded)
                throw new Exception($"The download from {c.ZipUrl} is not usable: {notDownloaded}");
            log("  It is a DbxTune package (bin/dbxcentral.bat, bin/dbxtune.bat, lib/dbxtune.jar) — OK.");
            return true;
        }

        // gMSA accounts end with '$' (e.g. CORP\svc_dbx$). AD manages their password automatically.
        private static bool IsGmsa(string account) => account.TrimEnd().EndsWith('$');

        private static bool IsDomain(string account) =>
            account.Contains('\\') || account.Contains('@') || IsGmsa(account);

        private static Task<bool> CreateServiceAccount(InstallConfig c, InstallLog log)
        {
            if (IsDomain(c.ServiceAccount))
            {
                string kind = IsGmsa(c.ServiceAccount) ? "gMSA account" : "Domain account";
                log($"{kind} detected: '{c.ServiceAccount}'");
                log("Skipping local account creation — account must already exist in Active Directory.");
                if (IsGmsa(c.ServiceAccount))
                    log("gMSA: AD manages the password automatically; no password required here.");
                log("Privilege grants in the next steps will still be applied.");
                return Task.FromResult(true);
            }

            // Check if local account already exists
            var (exists, _) = Run("net", $"user \"{c.ServiceAccount}\"");
            if (exists)
            {
                log($"Account '{c.ServiceAccount}' already exists — skipping creation.");
                VerifyPassword(c, log);   // throws if wrong password
                log("Privilege grants will still be applied in the next steps.");
                return Task.FromResult(true);
            }

            log($"Creating local account '{c.ServiceAccount}'…");
            var (ok, output) = Run("net",
                $"user \"{c.ServiceAccount}\" \"{c.Password}\" /add " +
                "/comment:\"DbxTune user account\"");
            log(output);
            if (!ok) throw new Exception($"Failed to create account '{c.ServiceAccount}'.");
            return Task.FromResult(true);
        }

        // Derive the Windows home directory for a service account name.
        public static string AccountHome(string account)
        {
            string name = account.Contains('\\') ? account.Split('\\')[1]
                        : account.Contains('@')   ? account.Split('@')[0]
                        : account.TrimEnd('$');
            return Path.Combine(@"C:\Users", name);
        }

        private static Task<bool> CreateUserProfile(InstallConfig c, InstallLog log)
        {
            string expected = AccountHome(c.ServiceAccount);
            const string howToFix =
                "Click Back to the Service Account page, press Check and then 'Clean up…' (or use the PowerShell " +
                "commands in this log), then click Next until the install runs again.";

            // What is in the way of C:\Users\<name> — checked BEFORE creating the profile, which would
            // otherwise land in another folder (C:\Users\<name>.000) and be one more leftover.
            var findings = ProfileCheck.Analyze(c.ServiceAccount, expected, ProfileCheck.ReadSnapshot(c.ServiceAccount, expected));
            if (findings.Any(f => f.Blocks))
            {
                foreach (var f in findings) log($"- {f.Text}");
                log("PowerShell commands for the administrator:");
                foreach (var line in ProfileCheck.PowerShellCommands(c.ServiceAccount, expected, findings).Split('\n'))
                    log("    " + line.TrimEnd('\r'));
                throw new Exception($"The profile folder {expected} can't be used for '{c.ServiceAccount}' (see above). {howToFix}");
            }

            log($"Ensuring Windows profile exists for '{c.ServiceAccount}'…");
            // The folder name only: CreateProfile takes it as a path piece ("DOMAIN\user" became C:\Users\DOMAIN\user)
            LsaPrivileges.EnsureUserProfile(c.ServiceAccount, Path.GetFileName(expected), out string path);
            log($"Profile path: {path}");

            if (!path.TrimEnd('\\').Equals(expected, StringComparison.OrdinalIgnoreCase))
            {
                log("PowerShell commands for the administrator:");
                foreach (var line in ProfileCheck.PowerShellCommands(c.ServiceAccount, expected, new List<ProfileCheck.Finding>()).Split('\n'))
                    log("    " + line.TrimEnd('\r'));
                throw new Exception($"Windows created the profile of '{c.ServiceAccount}' at '{path}' instead of '{expected}': " +
                                    $"something else uses that folder name. {howToFix}");
            }

            c.ProfilePath = path;
            log("Profile path matches expected location — OK.");
            return Task.FromResult(true);
        }

        private static Task<bool> GrantSymlinkPrivilege(InstallConfig c, InstallLog log)
        {
            log($"Granting SeCreateSymbolicLinkPrivilege to '{c.ServiceAccount}'…");
            LsaPrivileges.Grant(c.ServiceAccount, LsaPrivileges.SeCreateSymbolicLinkPrivilege);
            log("Done.");
            return Task.FromResult(true);
        }

        private static Task<bool> GrantServiceLogon(InstallConfig c, InstallLog log)
        {
            log($"Granting SeServiceLogonRight to '{c.ServiceAccount}'…");
            LsaPrivileges.Grant(c.ServiceAccount, LsaPrivileges.SeServiceLogonRight);
            log("Done.");
            return Task.FromResult(true);
        }

        // "Allow log on locally". A domain account normally has it through the local Users group, but
        // hardened servers (CIS baseline, or a GPO that lists only Administrators) don't — and then
        // everything the installer runs AS the account fails with Win32 error 1385 (RunAs,
        // impersonation), and DbxStarterClient can't be started as the account either.
        private static Task<bool> GrantInteractiveLogon(InstallConfig c, InstallLog log)
        {
            if (IsGmsa(c.ServiceAccount))
            {
                log("gMSA account — can't log on interactively (AD manages the password), skipping.");
                return Task.FromResult(true);
            }
            log($"Granting SeInteractiveLogonRight (\"Allow log on locally\") to '{c.ServiceAccount}'…");
            LsaPrivileges.Grant(c.ServiceAccount, LsaPrivileges.SeInteractiveLogonRight);
            log("Done.");
            return Task.FromResult(true);
        }

        private static Task<bool> AddPerformanceGroups(InstallConfig c, InstallLog log)
        {
            // "Performance Log Users"    — typeperf, PDH data-collector sets, performance counter logging
            // "Performance Monitor Users" — real-time WMI / PerfMon counter queries (read-only subset)
            var groups = new[]
            {
                "Performance Log Users",
                "Performance Monitor Users",
                "Remote Desktop Users",
            };

            foreach (var group in groups)
            {
                log($"Adding '{c.ServiceAccount}' to '{group}'…");
                var (ok, output) = Run("net", $"localgroup \"{group}\" \"{c.ServiceAccount}\" /add");

                if (ok)
                {
                    log($"  Added.");
                }
                else if (output.Contains("already a member", StringComparison.OrdinalIgnoreCase) ||
                         output.Contains("1378",              StringComparison.Ordinal))  // ERROR_MEMBER_IN_ALIAS
                {
                    log($"  Already a member — OK.");
                }
                else
                {
                    // Non-fatal: log the warning and continue; the service may still work
                    // (e.g. if the group is managed via domain policy).
                    log($"  Warning: {output.Trim()}");
                }
            }

            return Task.FromResult(true);
        }

        private static Task<bool> GrantDebugPrivilege(InstallConfig c, InstallLog log)
        {
            // SeDebugPrivilege lets the process call OpenProcess() on any running process,
            // including SYSTEM-owned and other-user-owned ones, bypassing the normal ACL check.
            // DbxTune may need this when it reads performance data directly from database process
            // memory rather than via JDBC queries (e.g. to sample raw SQL Server or Sybase ASE
            // process internals).  It is NOT needed just to list or enumerate processes.
            log($"Granting SeDebugPrivilege to '{c.ServiceAccount}'…");
            log("  (enables OpenProcess() on any PID — needed for direct process-memory sampling)");
            LsaPrivileges.Grant(c.ServiceAccount, LsaPrivileges.SeDebugPrivilege);
            log("  Done.");
            return Task.FromResult(true);
        }

        // What the installer sets up for the service account, and what breaks when a domain Group
        // Policy takes it away again — used by "Check logon rights" (CheckLogonRights, BuildPolicyWarning).
        // NotForGmsa: a gMSA can't log on interactively or with Remote Desktop, so those don't apply to it.
        private static readonly (string Right, string Name, string Consequence, bool NotForGmsa)[] PolicyAllowRights =
        {
            (LsaPrivileges.SeServiceLogonRight,           "Log on as a service",   "DbxStarterService fails to start (error 1069)", false),
            (LsaPrivileges.SeInteractiveLogonRight,       "Allow log on locally",  "the account can no longer log on to this machine (error 1385), e.g. to run DbxStarterClient as the account", true),
            ("SeRemoteInteractiveLogonRight",             "Allow log on through Remote Desktop Services", "the account can't log on with Remote Desktop", true),
            (LsaPrivileges.SeCreateSymbolicLinkPrivilege, "Create symbolic links", "the service account can no longer create symbolic links", false),
            (LsaPrivileges.SeDebugPrivilege,              "Debug programs",        "DbxTune can no longer open other accounts' processes (direct process sampling)", false),
        };
        private static readonly (string Right, string Name, string Consequence, bool NotForGmsa)[] PolicyDenyRights =
        {
            ("SeDenyServiceLogonRight",           "Deny log on as a service",                    "DbxStarterService can't start (error 1069)", false),
            ("SeDenyInteractiveLogonRight",       "Deny log on locally",                         "the account can't log on to this machine (error 1385)", true),
            ("SeDenyRemoteInteractiveLogonRight", "Deny log on through Remote Desktop Services", "the account can't log on with Remote Desktop", true),
        };
        // The local groups AddPerformanceGroups puts the account in — by well-known SID, so the check
        // doesn't depend on the OS language.
        private static readonly (string Sid, string Name, string Consequence, bool NotForGmsa)[] PolicyLocalGroups =
        {
            ("S-1-5-32-559", "Performance Log Users",     "typeperf / performance counter logging may fail", false),
            ("S-1-5-32-558", "Performance Monitor Users", "real-time WMI / PerfMon counter queries may fail", false),
            ("S-1-5-32-555", "Remote Desktop Users",      "the account can't log on with Remote Desktop", true),
        };

        // "Check logon rights" — runs after all grants. 1) Logs on as the account to prove the password
        // and the rights work NOW, and stops the install with the exact cause if not. 2) On a domain
        // member, reads which user rights / local groups a domain GPO sets: a GPO setting REPLACES the
        // local one at every policy refresh, so the grants above are lost later — that becomes the
        // warning shown after the install (c.PolicyWarningDetail).
        private static Task<bool> CheckLogonRights(InstallConfig c, InstallLog log)
        {
            string account = c.ServiceAccount;
            bool   gmsa    = IsGmsa(account);
            c.PolicyWarningDetail = "";

            // ── 1. Group Policy result — best effort, only feeds the messages and the warning ──
            List<GroupPolicy.Setting>? gpoRights = null, gpoGroups = null;
            string rsopProblem = "";
            try
            {
                if (!GroupPolicy.IsDomainJoined())
                    log("This computer is not joined to a domain — no domain Group Policy can replace the local settings.");
                else
                {
                    (gpoRights, gpoGroups) = GroupPolicy.ReadComputerSettings();
                    log($"Group Policy result (RSoP): domain GPOs set {gpoRights.Count} user right(s) and {gpoGroups.Count} restricted group(s) on this computer.");
                    var relevant = PolicyAllowRights.Select(r => r.Right).Concat(PolicyDenyRights.Select(r => r.Right)).ToList();
                    foreach (var s in gpoRights.Where(s => relevant.Contains(s.Name, StringComparer.OrdinalIgnoreCase)))
                        log($"  {s.Name}  (GPO \"{s.GpoName}\"): {string.Join(", ", s.Members)}");
                    foreach (var s in gpoGroups)
                        log($"  Restricted group '{s.Name}'  (GPO \"{s.GpoName}\"): {string.Join(", ", s.Members)}");
                    log("  (Group Policy Preferences 'Local Users and Groups' can also change local group members — not checked.)");
                }
            }
            catch (Exception ex)
            {
                rsopProblem = ex.Message;
                log($"Warning: could not read the Group Policy result (RSoP): {ex.Message}");
            }

            // ── 2. Logon tests ──
            // sids = the account + every group it is in, from the logon token. Used to tell whether a
            // GPO's account list includes the account (directly or through a group).
            var sids = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            try { sids.Add(LsaPrivileges.GetSidString(account)); } catch { /* the grant steps above already resolved it */ }
            bool haveGroups = false;
            var notes = new List<string>();
            c.NoInteractiveLogon = false;

            var sidCache = new Dictionary<string, string?>(StringComparer.OrdinalIgnoreCase);
            string? LookupSid(string name)
            {
                if (sidCache.TryGetValue(name, out var cached)) return cached;
                string? sid = null;
                try { sid = LsaPrivileges.GetSidString(name); } catch { /* unresolvable — compared by name */ }
                return sidCache[name] = sid;
            }

            // Returns true when the logon worked; throws (stops the install) on a fatal logon error.
            bool TestLogon(int logonType, string what, string allowRight, string allowName,
                           string denyRight, string denyName, string neededFor)
            {
                int err = LogonAs(account, c.Password, logonType, sids);
                if (err == 0) { log($"Logon test '{what}': OK"); return true; }

                // Interactive logon denied, but the service logon worked (haveGroups): the service is fine,
                // so carry on and run the as-the-account steps as the admin (c.NoInteractiveLogon) — the
                // warning at the end says what the AD administrator must change to allow it.
                if (err == ERROR_LOGON_TYPE_NOT_GRANTED && logonType == LOGON32_LOGON_INTERACTIVE && haveGroups)
                {
                    c.NoInteractiveLogon = true;
                    log($"Warning: '{account}' may not log on locally on this computer (Win32 error 1385) — a '{denyName}' policy " +
                        "applies. Continuing: the steps that run as the account run as you instead, with the account's folders " +
                        "(see the warning at the end for what to ask the AD administrator for).");
                    return false;
                }

                if (err == ERROR_LOGON_TYPE_NOT_GRANTED)
                {
                    // 1385 comes only after Windows accepted the password: a right is missing or denied.
                    bool grantedLocally = false;
                    try { grantedLocally = LsaPrivileges.GetAccountRights(account).Contains(allowRight, StringComparer.OrdinalIgnoreCase); }
                    catch { /* message below covers both cases */ }

                    var msg = new StringBuilder();
                    msg.AppendLine($"'{account}' is not allowed to {what} on this computer (Win32 error 1385). {neededFor}");
                    msg.AppendLine(grantedLocally
                        ? $"'{allowName}' IS granted to the account in the local security policy, so a '{denyName}' setting applies to the account or to a group it is a member of."
                        : $"'{allowName}' is not granted to the account in the local security policy (a domain GPO may have just replaced the local grant), or a '{denyName}' setting applies.");
                    var gpoLines = (gpoRights ?? new List<GroupPolicy.Setting>())
                        .Where(s => s.Name.Equals(denyRight, StringComparison.OrdinalIgnoreCase) || s.Name.Equals(allowRight, StringComparison.OrdinalIgnoreCase))
                        .Select(s => $"  GPO \"{s.GpoName}\" sets '{(s.Name.Equals(denyRight, StringComparison.OrdinalIgnoreCase) ? denyName : allowName)}' to: {string.Join(", ", s.Members)}")
                        .ToList();
                    foreach (var line in gpoLines) msg.AppendLine(line);
                    if (gpoLines.Count == 0)
                        msg.AppendLine($"  To see which policy sets '{denyName}': rsop.msc > Computer Configuration > Windows Settings > " +
                                       "Security Settings > Local Policies > User Rights Assignment (column 'Source GPO').");
                    msg.Append($"Ask your AD administrator to allow it for '{account}' on this server (remove the account or its group " +
                               $"from '{denyName}', and include it in '{allowName}' if a GPO sets that), then click Retry.");
                    throw new Exception(msg.ToString());
                }

                if (FatalLogonErrors.Contains(err))
                    throw new Exception($"Logon test '{what}' as '{account}' failed: {DescribeLogonError(err)}");

                // Not a verdict on the account (no domain controller reachable, trust problem, …) —
                // don't block the install on what may be temporary, but say so at the end.
                string note = $"Could not test '{what}' as '{account}': {DescribeLogonError(err)}";
                log("Warning: " + note + " Continuing.");
                notes.Add(note);
                return false;
            }

            if (gmsa)
                log("gMSA account — no password, so the logon tests are skipped (DbxStarterService shows at start whether 'Log on as a service' works).");
            else
            {
                // Service logon first: its token isn't filtered by UAC, so it has every group.
                // A wrong password fails here — one bad attempt, not one per test.
                haveGroups |= TestLogon(LOGON32_LOGON_SERVICE, "log on as a service",
                    LsaPrivileges.SeServiceLogonRight, "Log on as a service",
                    "SeDenyServiceLogonRight", "Deny log on as a service",
                    "DbxStarterService runs as this account and can't start without it.");
                haveGroups |= TestLogon(LOGON32_LOGON_INTERACTIVE, "log on locally",
                    LsaPrivileges.SeInteractiveLogonRight, "Allow log on locally",
                    "SeDenyInteractiveLogonRight", "Deny log on locally",
                    "The installer runs 'Initialize DbxTune home' and the DBMS config wizard as this account, and starts DbxStarterClient as it.");
            }

            // ── 3. The warning: GPO settings that will undo (or block) the local setup ──
            string rest = BuildPolicyWarning(account, gmsa, sids, haveGroups, gpoRights, gpoGroups, rsopProblem, notes, LookupSid,
                                             skipInteractive: c.NoInteractiveLogon);
            string interactive = c.NoInteractiveLogon
                ? InteractiveDeniedText(account, c.Mode == InstallMode.Upgrade ? "upgrade / repair" : "install", sids, gpoRights, LookupSid)
                : "";
            c.PolicyWarningDetail = string.Join("\n\n", new[] { interactive, rest }.Where(s => s.Length > 0));

            if (c.PolicyWarningDetail.Length > 0)
                log("WARNING: see the Group Policy warning at the end of this log.");
            else if (gpoRights != null)
                log("No domain Group Policy setting undoes the local setup — OK.");
            return Task.FromResult(true);
        }

        // The first entry of a GPO's account list that is the account itself or a group it is in (by
        // SID, from the logon token: nested groups too), or null. A GPO lists accounts by name
        // ("CORP\DbxTune Admins") or by SID ("*S-1-5-32-544"); a name that can't be resolved is
        // compared with the account name.
        internal static string? FindGpoMember(string[] members, HashSet<string> sids, string account, Func<string, string?> lookupSid) =>
            members.FirstOrDefault(member =>
            {
                string m   = member.Trim().TrimStart('*');
                string? sid = m.StartsWith("S-1-", StringComparison.OrdinalIgnoreCase) ? m : lookupSid(m);
                return sid != null ? sids.Contains(sid) : m.Equals(account, StringComparison.OrdinalIgnoreCase);
            });

        // The warning when a 'Deny log on locally' policy blocks the account (c.NoInteractiveLogon): what
        // the install did instead, and exactly what the AD administrator must change — the deny group the
        // account is in (found through the logon token's groups) and the 'Allow log on locally' GPO.
        internal static string InteractiveDeniedText(string account, string flow, HashSet<string> sids,
            List<GroupPolicy.Setting>? gpoRights, Func<string, string?> lookupSid)
        {
            var deny  = gpoRights?.FirstOrDefault(s => s.Name.Equals("SeDenyInteractiveLogonRight", StringComparison.OrdinalIgnoreCase));
            var allow = gpoRights?.FirstOrDefault(s => s.Name.Equals(LsaPrivileges.SeInteractiveLogonRight, StringComparison.OrdinalIgnoreCase));
            string? group = deny == null ? null : FindGpoMember(deny.Members, sids, account, lookupSid);
            string me = $"{Environment.UserDomainName}\\{Environment.UserName}";
            const string where = "Computer Configuration > Policies > Windows Settings > Security Settings >\n" +
                                 "     Local Policies > User Rights Assignment";

            var sb = new StringBuilder();
            sb.AppendLine($"'{account}' may NOT log on locally on this computer:");
            sb.AppendLine(deny != null && group != null ? $"it is in '{group}', which GPO \"{deny.GpoName}\" lists under 'Deny log on locally'."
                        : deny != null ? $"GPO \"{deny.GpoName}\" denies it through one of the account's groups ('Deny log on locally')."
                        : "a 'Deny log on locally' setting applies to it.");
            sb.AppendLine($"The {flow} went on without that:");
            sb.AppendLine($"  - 'Initialize DbxTune home' and the DBMS config wizard ran as you ({me}), with the account's folders,");
            sb.AppendLine($"    and '{account}' got full control of them (icacls).");
            sb.AppendLine($"  - DbxStarterClient can't run as '{account}': start it as yourself (it controls the service the same way),");
            sb.AppendLine("    or use the DbxStarter Web UI.");
            sb.AppendLine("  - DbxStarterService is not affected: 'Log on as a service' works.");
            sb.AppendLine();
            sb.AppendLine($"To let '{account}' log on locally (for DbxStarterClient or Remote Desktop as the account), ask your AD administrator:");
            if (deny != null && group != null)
                sb.AppendLine($"  1. Take '{account}' out of the group '{group}', or exclude this server ({Environment.MachineName}) from the");
            else
                sb.AppendLine($"  1. Find the group of '{account}' that 'Deny log on locally' lists (rsop.msc on this server, or 'whoami /groups'),");
            if (deny != null && group == null)
                sb.AppendLine($"     GPO \"{deny.GpoName}\" lists: {string.Join(", ", deny.Members)}. Take the account out of it, or exclude this server from the");
            sb.AppendLine(deny != null ? $"     'Deny log on locally' setting of GPO \"{deny.GpoName}\"." : "     and take the account out of it.");
            sb.AppendLine($"     In gpmc.msc: {where}");
            if (allow != null && FindGpoMember(allow.Members, sids, account, lookupSid) == null)
            {
                sb.AppendLine($"  2. Add '{account}' (or a group it is in) to 'Allow log on locally' in GPO \"{allow.GpoName}\" (it lists only:");
                sb.AppendLine($"     {string.Join(", ", allow.Members)}) — otherwise the installer's local grant is removed at the next policy refresh.");
            }
            sb.AppendLine("Then run this installer again and choose 'Upgrade / Repair'.");

            // The same as PowerShell. Group membership: ActiveDirectory module (RSAT). A User Rights
            // Assignment inside a GPO has no PowerShell cmdlet — that part stays in gpmc.msc.
            string sam      = account.Contains('\\') ? account.Split('\\', 2)[1] : account.Split('@')[0];
            string groupSam = group == null ? "<group>" : group.Contains('\\') ? group.Split('\\', 2)[1] : group;
            sb.AppendLine();
            sb.AppendLine("PowerShell for the AD administrator (ActiveDirectory module):");
            sb.AppendLine($"    # The groups '{sam}' is in (look for the one 'Deny log on locally' lists)");
            sb.AppendLine($"    Get-ADPrincipalGroupMembership '{sam}' | Select-Object Name | Sort-Object Name");
            sb.AppendLine($"    # Take the account out of the deny group");
            sb.AppendLine($"    Remove-ADGroupMember -Identity '{groupSam}' -Members '{sam}' -Confirm:$false");
            if (deny != null)
            {
                sb.AppendLine("    # The GPO's settings (User Rights Assignment), to look at");
                sb.AppendLine($"    Get-GPOReport -Name '{deny.GpoName}' -ReportType Html -Path $env:TEMP\\gpo.html");
            }
            sb.AppendLine("    # (Changing 'Allow/Deny log on locally' INSIDE a GPO has no cmdlet: use gpmc.msc, see above.)");
            sb.AppendLine("PowerShell on this server, afterwards (as administrator):");
            sb.AppendLine("    gpupdate /force");
            sb.AppendLine("    gpresult /scope computer /h $env:TEMP\\rsop.html; Start-Process $env:TEMP\\rsop.html   # 'Source GPO' per user right");
            return sb.ToString().TrimEnd();
        }

        // The text of the "Check logon rights" warning, or "" when there is nothing to warn about.
        // No system calls except through lookupSid, so it can be tested with made-up GPO data.
        //   sids       : the account's SID + the SIDs of every group it is in (from a logon token)
        //   haveGroups : false when no logon token was available — sids then holds only the account
        //   gpoRights/gpoGroups : the domain GPO settings, null when not read (not domain-joined, or rsopProblem)
        //   lookupSid  : account/group name → SID string, null when it can't be resolved
        //   skipInteractive : the interactive-logon rights are explained by InteractiveDeniedText already
        internal static string BuildPolicyWarning(string account, bool gmsa, HashSet<string> sids, bool haveGroups,
            List<GroupPolicy.Setting>? gpoRights, List<GroupPolicy.Setting>? gpoGroups, string rsopProblem,
            List<string> notes, Func<string, string?> lookupSid, bool skipInteractive = false)
        {
            var known = new HashSet<string>(sids, StringComparer.OrdinalIgnoreCase);
            if (!haveGroups)
            {
                // Without a logon token only these are certain: they apply to every logged-on account.
                known.Add("S-1-1-0");    // Everyone
                known.Add("S-1-5-11");   // Authenticated Users
            }

            string? SidOf(string member)
            {
                string m = member.Trim().TrimStart('*');
                return m.StartsWith("S-1-", StringComparison.OrdinalIgnoreCase) ? m : lookupSid(m);
            }
            string? FindAccount(string[] members) => FindGpoMember(members, known, account, lookupSid);
            bool Skip(string right) => skipInteractive && right is "SeInteractiveLogonRight" or "SeDenyInteractiveLogonRight";

            var items = new List<string>();
            bool anyDeny = false;
            if (gpoRights != null && gpoGroups != null)
            {
                foreach (var r in PolicyAllowRights.Where(r => !(gmsa && r.NotForGmsa) && !Skip(r.Right)))
                {
                    var s = gpoRights.FirstOrDefault(s => s.Name.Equals(r.Right, StringComparison.OrdinalIgnoreCase));
                    if (s != null && FindAccount(s.Members) == null)
                        items.Add($"- {r.Name}\n    GPO \"{s.GpoName}\" sets it to: {string.Join(", ", s.Members)}\n    -> {r.Consequence}");
                }
                foreach (var r in PolicyDenyRights.Where(r => !(gmsa && r.NotForGmsa) && !Skip(r.Right)))
                {
                    var s = gpoRights.FirstOrDefault(s => s.Name.Equals(r.Right, StringComparison.OrdinalIgnoreCase));
                    if (s != null && FindAccount(s.Members) is string match)
                    {
                        anyDeny = true;
                        items.Add($"- {r.Name}\n    GPO \"{s.GpoName}\" includes '{match}'\n    -> {r.Consequence}");
                    }
                }
                foreach (var g in PolicyLocalGroups.Where(g => !(gmsa && g.NotForGmsa)))
                {
                    var s = gpoGroups.FirstOrDefault(s => string.Equals(SidOf(s.Name), g.Sid, StringComparison.OrdinalIgnoreCase));
                    if (s != null && FindAccount(s.Members) == null)
                        items.Add($"- Local group '{g.Name}' (Restricted Groups)\n    GPO \"{s.GpoName}\" sets its members to: {string.Join(", ", s.Members)}\n" +
                                  $"    -> Windows removes the account from the group: {g.Consequence}");
                }
            }

            var warning = new StringBuilder();
            if (items.Count > 0)
            {
                warning.AppendLine("A domain Group Policy sets the following for this computer, which undoes or blocks what the");
                warning.AppendLine($"installer set up for '{account}'. Group Policy REPLACES the local setting at the next policy");
                warning.AppendLine("refresh (reboot, gpupdate, or within 16 hours):");
                warning.AppendLine();
                foreach (var item in items) warning.AppendLine(item);
                warning.AppendLine();
                if (!haveGroups)
                    warning.AppendLine("(The account's group memberships could not be read, so a GPO that includes it through a group is listed too.)");
                warning.AppendLine($"Ask your AD administrator to include '{account}' (or a group it is a member of) in these settings" +
                                   (anyDeny ? ",\nand to take it out of the 'Deny ...' ones," : "") + " for this server.");
                warning.AppendLine("Group Policy editor: Computer Configuration > Policies > Windows Settings > Security Settings >");
                warning.AppendLine("Local Policies > User Rights Assignment (and > Restricted Groups for the local groups).");
            }
            if (rsopProblem.Length > 0)
            {
                if (warning.Length > 0) warning.AppendLine();
                warning.AppendLine($"Could not read the Group Policy result on this computer ({rsopProblem}).");
                warning.AppendLine($"So it's unknown whether a domain GPO will replace the rights granted to '{account}'. Check with rsop.msc");
                warning.AppendLine("(Computer Configuration > Windows Settings > Security Settings > Local Policies > User Rights Assignment)");
                warning.AppendLine("that 'Log on as a service' and 'Allow log on locally' include the account, if a GPO sets them.");
            }
            if (notes.Count > 0)
            {
                if (warning.Length > 0) warning.AppendLine();
                foreach (var note in notes) warning.AppendLine(note);
                warning.AppendLine("Make sure DbxStarterService starts, once the cause is fixed.");
            }
            return warning.ToString().TrimEnd();
        }

        private static Task<bool> CreateDirectories(InstallConfig c, InstallLog log)
        {
            // Run under impersonation so the install directory is natively owned by the
            // service account — no ACL fixup needed afterwards.
            // Falls back to admin + icacls for gMSA (no interactive logon possible).
            bool needIcacls;
            using (var imp = UserImpersonation.TryCreate(c, log))
            {
                needIcacls = (imp == null);
                Directory.CreateDirectory(c.InstallDir);
                log($"  {c.InstallDir}");
            }   // RevertToSelf here

            if (needIcacls)
            {
                // gMSA / logon-failure fallback: grant the service account full control with
                // inheritance so extracted files and future sub-directories are accessible.
                log($"Granting '{c.ServiceAccount}' full control on: {c.InstallDir}");
                var (ok, output) = Run("icacls",
                    $"\"{c.InstallDir}\" /grant \"{c.ServiceAccount}:(OI)(CI)F\" /T /Q");
                if (!string.IsNullOrWhiteSpace(output)) log($"  {output}");
                if (!ok) log("  Warning: icacls grant failed — service account may lack execute rights.");
            }

            return Task.FromResult(true);
        }

        private static Task<bool> ExtractPackage(InstallConfig c, InstallLog log)
        {
            if (string.IsNullOrEmpty(c.ResolvedZipPath))
                throw new Exception("No ZIP path — did the Locate step succeed?");

            // Versioned directory name = ZIP filename without extension
            c.VersionDir = Path.GetFileNameWithoutExtension(c.ResolvedZipPath);
            string extractTo = Path.Combine(c.InstallDir, c.VersionDir);

            log($"Extracting: {c.ResolvedZipPath}");
            log($"        to: {extractTo}");

            // Open the ZIP as admin first (the file may be in admin's %TEMP%,
            // inaccessible to the service account), then extract under impersonation
            // so every created directory and file is owned by the service account.
            using var zip = ZipFile.OpenRead(c.ResolvedZipPath);

            using (var imp = UserImpersonation.TryCreate(c, log))
            {
                Directory.CreateDirectory(extractTo);

                int total = zip.Entries.Count, done = 0;
                foreach (var entry in zip.Entries)
                {
                    string dest = Path.GetFullPath(Path.Combine(extractTo, entry.FullName));
                    if (!dest.StartsWith(extractTo, StringComparison.OrdinalIgnoreCase))
                        throw new Exception($"ZIP path traversal attempt: {entry.FullName}");

                    if (entry.FullName.EndsWith('/') || entry.FullName.EndsWith('\\'))
                        Directory.CreateDirectory(dest);
                    else
                    {
                        Directory.CreateDirectory(Path.GetDirectoryName(dest)!);
                        entry.ExtractToFile(dest, overwrite: true);
                    }
                    done++;
                    if (done % 100 == 0 || done == total)
                        log($"  {done} / {total} files…");
                }
                log($"Extraction complete — {total} entries.");
            }   // RevertToSelf here — back to admin for junction creation

            // Sanity check of what landed on disk, before the '0' junction points at it
            var missing = DbxTunePackageFiles.Where(f => !File.Exists(Path.Combine(extractTo, f.Replace('/', '\\')))).ToList();
            if (missing.Count > 0)
                throw new Exception($"The unpacked package in {extractTo} is not complete — missing: {string.Join(", ", missing)}. " +
                                    "The ZIP may be damaged, or a virus scanner removed files: download it again, then click Retry.");
            log("Unpacked files checked (bin/dbxcentral.bat, bin/dbxtune.bat, lib/dbxtune.jar) — OK.");

            // Create / update the '0' junction pointing to the versioned directory.
            // Directory junctions (/J) don't require SeCreateSymbolicLinkPrivilege, so
            // the admin can create it even though the target is owned by the service account.
            string link = Path.Combine(c.InstallDir, "0");
            if (Directory.Exists(link))
            {
                log($"Updating existing junction: {link}");
                Directory.Delete(link); // removes the junction, not the target
            }
            log($"Creating junction: {link}  →  {extractTo}");
            var (ok, output) = Run("cmd.exe", $"/c mklink /J \"{link}\" \"{extractTo}\"");
            log(output);
            if (!ok) throw new Exception("Failed to create directory junction '0'.");

            return Task.FromResult(true);
        }

        private static Task<bool> RunInitCommand(InstallConfig c, InstallLog log)
        {
            if (string.IsNullOrWhiteSpace(c.InitCommand))
            {
                log("No init command configured — skipping.");
                log($"Tip: run the DbxTune setup manually from: {c.InstallDir}");
                return Task.FromResult(true);
            }

            log(RunAsAdmin(c) ? $"Running for '{c.ServiceAccount}': {c.InitCommand}" : $"Running as '{c.ServiceAccount}': {c.InitCommand}");
            var (ok, output) = RunAs("cmd.exe", $"/c {c.InitCommand}", c, log);
            log(output);
            if (!ok) throw new Exception("Init command failed.");

            // The command can exit 0 while still having logged an internal error (e.g. a Java
            // stack trace) — that wouldn't otherwise stop the install, so flag it for a big
            // warning after "Installation complete" instead of letting it slip by silently.
            var (hasErrors, detail) = FindJavaErrors(output);
            if (hasErrors)
            {
                c.InitCommandHasWarnings = true;
                c.InitCommandWarningDetail = detail;
                log("  Warning: possible error(s) detected in the output above — see the summary after install completes.");
            }
            return Task.FromResult(true);
        }

        // Scans command output for signs of an internal error that didn't necessarily fail the
        // process's exit code: a " - ERROR " log line (the common log4j-style pattern DbxTune's
        // own tools use) or a Java stack trace ("Exception ... \n\tat ..."/"Caused by: ...").
        // Returns up to a handful of the most relevant lines as a short summary.
        private static (bool Found, string Detail) FindJavaErrors(string output)
        {
            if (string.IsNullOrEmpty(output)) return (false, "");

            var lines = output.Replace("\r\n", "\n").Split('\n');
            var flagged = new List<string>();
            var stackFrame = new Regex(@"^\s*at\s+[\w.$]+\(", RegexOptions.IgnoreCase);

            for (int i = 0; i < lines.Length; i++)
            {
                string line = lines[i];
                bool isErrorLog   = line.Contains(" - ERROR ", StringComparison.Ordinal);
                bool isStackFrame = stackFrame.IsMatch(line);
                bool isCausedBy   = line.TrimStart().StartsWith("Caused by:", StringComparison.Ordinal);
                bool isException  = Regex.IsMatch(line, @"\b[\w.$]+Exception\b");

                if (isErrorLog || isStackFrame || isCausedBy || (isException && i + 1 < lines.Length && stackFrame.IsMatch(lines[i + 1])))
                    flagged.Add(line.TrimEnd());

                if (flagged.Count >= 20) break; // enough for a summary — the full output is already in the log above
            }

            return (flagged.Count > 0, string.Join("\n", flagged));
        }

        private static Task<bool> VerifyDbxDirs(InstallConfig c, InstallLog log)
        {
            var dirs = new[]
            {
                ("DBXTUNE_USER_HOME",           c.DbxUserHome),
                ("DBXTUNE_CENTRAL_SAVE_DIR",    c.DbxSaveDir),
                ("DBXTUNE_CENTRAL_REPORTS_DIR", c.DbxReportsDir),
                ("DBXTUNE_CENTRAL_LOG_DIR",     c.DbxLogDir),
                ("DBXTUNE_CENTRAL_CONF_DIR",    c.DbxConfDir),
                ("DBXTUNE_CENTRAL_INFO_DIR",    c.DbxInfoDir),
            };

            int maxNameLen = dirs.Max(d => d.Item1.Length);

            bool created = false;
            foreach (var (name, path) in dirs)
            {
                if (string.IsNullOrWhiteSpace(path)) continue;
                string padded = name.PadRight(maxNameLen);
                if (Directory.Exists(path))
                {
                    log($"  OK       {padded} = {path}");
                }
                else
                {
                    Directory.CreateDirectory(path);
                    log($"  Created  {padded} = {path}");
                    created = true;
                }
            }
            // Created here as the admin: inside the account's profile it inherits the account's access,
            // but a directory placed elsewhere (e.g. D:\dbxtune\data) would not — grant it explicitly.
            if (created) GrantAccountDbxDirs(c, log);

            // Verify that DBXTUNE.env.bat's built-in fallback path matches the install dir.
            // When DBXTUNE_HOME is not set in the environment, the bat file auto-detects it
            // from %USERPROFILE% — make sure that auto-detected path matches our install dir.
            CheckEnvBatDefault(c, log);

            return Task.FromResult(true);
        }

        // Reads $HOME/.dbxtune/DBXTUNE.env.bat and verifies that the default DBXTUNE_HOME
        // value (used when the env var is not already defined) points to InstallDir\0.
        private static void CheckEnvBatDefault(InstallConfig c, InstallLog log)
        {
            if (string.IsNullOrWhiteSpace(c.DbxUserHome)) return;

            string batPath = Path.Combine(c.DbxUserHome, "DBXTUNE.env.bat");
            if (!File.Exists(batPath))
            {
                log($"  DBXTUNE.env.bat: not found at {batPath}");
                log("    (will be created by the init command — re-run this step afterwards if needed)");
                return;
            }

            string? rawDefault = ParseEnvBatDefault(batPath);
            if (rawDefault == null)
            {
                log($"  DBXTUNE.env.bat: could not parse default DBXTUNE_HOME value — verify manually");
                log($"    File: {batPath}");
                return;
            }

            // Strip surrounding quotes — bat files often use  set "VAR=value"  which leaves a
            // trailing " after the marker slice (and possibly a leading one too).
            rawDefault = rawDefault.Trim('"');

            // Expand %USERPROFILE% using the service account's profile directory,
            // NOT the current admin's %USERPROFILE% (which would be a different path).
            string profileDir = !string.IsNullOrEmpty(c.ProfilePath)
                ? c.ProfilePath
                : AccountHome(c.ServiceAccount);

            string expanded = rawDefault
                .Replace("%USERPROFILE%", profileDir, StringComparison.OrdinalIgnoreCase)
                .TrimEnd('\\', '/');

            // Expected: InstallDir\0  (the '0' junction that always points to the current version)
            string expected = Path.Combine(c.InstallDir, "0").TrimEnd('\\', '/');

            if (expanded.Equals(expected, StringComparison.OrdinalIgnoreCase))
            {
                log($"  DBXTUNE.env.bat: default DBXTUNE_HOME = {expanded}  ✓");
            }
            else
            {
                log($"  WARNING: DBXTUNE.env.bat default DBXTUNE_HOME does not match the install directory!");
                log($"    Bat file default (expanded) : {expanded}");
                log($"    Expected                    : {expected}");
                log($"    The DBXTUNE_HOME registry value (set in 'Set DbxTune directories') takes");
                log($"    precedence, so the service will start correctly.  However, if that registry");
                log($"    value is ever removed, DbxTune will launch with the wrong DBXTUNE_HOME.");
                log($"    Fix: rename the install directory to match, or update DBXTUNE.env.bat manually.");
            }
        }

        // Parse the default (fallback) value for DBXTUNE_HOME from a DBXTUNE.env.bat file.
        // Returns the raw unexpanded value (e.g. "%USERPROFILE%\dbxtune\0"), or null if not found.
        //
        // Recognises two common patterns:
        //   Single-line: if not defined DBXTUNE_HOME set DBXTUNE_HOME=<value>
        //   Block form:  if not defined DBXTUNE_HOME (
        //                    set DBXTUNE_HOME=<value>
        //                )
        private static string? ParseEnvBatDefault(string batPath)
        {
            const string setMarker = "DBXTUNE_HOME=";
            try
            {
                bool insideGuardBlock = false;

                foreach (string raw in File.ReadLines(batPath))
                {
                    string line = raw.Trim();

                    // Skip blank lines and comments
                    if (line.Length == 0
                        || line.StartsWith("::", StringComparison.Ordinal)
                        || line.StartsWith("REM ", StringComparison.OrdinalIgnoreCase)
                        || line.Equals("REM", StringComparison.OrdinalIgnoreCase))
                        continue;

                    // Detect guard condition forms:
                    //   if not defined DBXTUNE_HOME ...
                    //   if "%DBXTUNE_HOME%"=="" ...   /   if "%DBXTUNE_HOME%" == "" ...
                    bool isGuard =
                        line.IndexOf("DBXTUNE_HOME", StringComparison.OrdinalIgnoreCase) >= 0 &&
                        (line.IndexOf("not defined",  StringComparison.OrdinalIgnoreCase) >= 0 ||
                         line.IndexOf("==\"\"",        StringComparison.OrdinalIgnoreCase) >= 0 ||
                         line.IndexOf("== \"\"",       StringComparison.OrdinalIgnoreCase) >= 0);

                    if (isGuard)
                    {
                        // Single-line: contains both the guard and the set
                        int setIdx = line.IndexOf(setMarker, StringComparison.OrdinalIgnoreCase);
                        if (setIdx >= 0)
                            return line[(setIdx + setMarker.Length)..].Trim();

                        // Start of a block — the set will be on the next non-blank line
                        insideGuardBlock = true;
                        continue;
                    }

                    if (insideGuardBlock)
                    {
                        // End of the block
                        if (line == ")" || line == ")^") { insideGuardBlock = false; continue; }

                        int setIdx = line.IndexOf(setMarker, StringComparison.OrdinalIgnoreCase);
                        if (setIdx >= 0)
                            return line[(setIdx + setMarker.Length)..].Trim();
                    }
                }
            }
            catch { /* file read errors reported by caller */ }

            return null;
        }

        // The account can't run processes interactively: a gMSA (no password), or a 'Deny log on locally'
        // policy (c.NoInteractiveLogon). Then the as-the-account steps run as the installing admin.
        private static bool RunAsAdmin(InstallConfig c) => IsGmsa(c.ServiceAccount) || c.NoInteractiveLogon;

        private static string RunAsAdminReason(InstallConfig c) => IsGmsa(c.ServiceAccount)
            ? "gMSA account — no interactive logon"
            : $"'{c.ServiceAccount}' may not log on locally (Group Policy)";

        // A process started as the installing admin, but pointed at the SERVICE ACCOUNT's folders: its
        // profile (USERPROFILE / HOMEDRIVE / HOMEPATH, read by dbxtune.bat), the DBXTUNE_* directories,
        // and Java's user.home (JAVA_TOOL_OPTIONS — DbxTune's createAppDir writes to user.home). Without
        // this, "Initialize DbxTune home" would fill the ADMIN's profile.
        private static ProcessStartInfo AdminPsiForAccount(string exe, string args, InstallConfig c)
        {
            var psi = new ProcessStartInfo(exe, args) { UseShellExecute = false };
            string profile = !string.IsNullOrEmpty(c.ProfilePath) ? c.ProfilePath : AccountHome(c.ServiceAccount);
            string drive   = Path.GetPathRoot(profile)?.TrimEnd('\\') ?? "C:";
            psi.Environment["USERPROFILE"] = profile;
            psi.Environment["HOMEDRIVE"]   = drive;
            psi.Environment["HOMEPATH"]    = profile.Substring(drive.Length);
            psi.Environment["DBXTUNE_HOME"] = Path.Combine(c.InstallDir, "0");
            foreach (var (name, value) in new[]
            {
                ("DBXTUNE_USER_HOME", c.DbxUserHome), ("DBXTUNE_CENTRAL_SAVE_DIR", c.DbxSaveDir), ("DBXTUNE_CENTRAL_REPORTS_DIR", c.DbxReportsDir),
                ("DBXTUNE_CENTRAL_LOG_DIR", c.DbxLogDir), ("DBXTUNE_CENTRAL_CONF_DIR", c.DbxConfDir), ("DBXTUNE_CENTRAL_INFO_DIR", c.DbxInfoDir),
            })
                if (!string.IsNullOrWhiteSpace(value)) psi.Environment[name] = value;
            string userHome = profile.Contains(' ') ? $"\"-Duser.home={profile}\"" : $"-Duser.home={profile}";
            psi.Environment["JAVA_TOOL_OPTIONS"] = ((psi.Environment.TryGetValue("JAVA_TOOL_OPTIONS", out var jto) ? jto + " " : "") + userHome).Trim();
            return psi;
        }

        // After something ran as the admin for the account: give the account full control of its DbxTune
        // directories (the files are owned by the admin). Inside its profile it has that already through
        // inheritance — this also covers directories elsewhere.
        private static void GrantAccountDbxDirs(InstallConfig c, InstallLog log)
        {
            var dirs = new[] { c.DbxUserHome, c.DbxSaveDir, c.DbxReportsDir, c.DbxLogDir, c.DbxConfDir, c.DbxInfoDir }
                .Where(d => !string.IsNullOrWhiteSpace(d) && Directory.Exists(d)).Distinct(StringComparer.OrdinalIgnoreCase).ToList();
            // A directory inside another one in the list is covered by the inherited grant
            foreach (var dir in dirs.Where(d => !dirs.Any(o => !o.Equals(d, StringComparison.OrdinalIgnoreCase) &&
                                                                d.StartsWith(o.TrimEnd('\\') + "\\", StringComparison.OrdinalIgnoreCase))))
            {
                log($"  Granting '{c.ServiceAccount}' full control on: {dir}");
                var (ok, output) = Run("icacls", $"\"{dir}\" /grant \"{c.ServiceAccount}:(OI)(CI)F\" /T /Q");
                if (!ok) log($"  Warning: icacls failed: {output.Trim()}");
            }
        }

        // Run a process as the service account with its user profile loaded — or, when the account
        // can't log on interactively (RunAsAdmin), as the admin with the account's folders.
        private static (bool ok, string output) RunAs(string exe, string args, InstallConfig c, InstallLog log)
        {
            if (RunAsAdmin(c))
            {
                log($"  ({RunAsAdminReason(c)}: running as you, {Environment.UserDomainName}\\{Environment.UserName}, with the account's folders)");
                try
                {
                    var psi = AdminPsiForAccount(exe, args, c);
                    psi.RedirectStandardOutput = true;
                    psi.RedirectStandardError  = true;
                    psi.CreateNoWindow         = true;
                    using var p = Process.Start(psi)!;
                    var stdout = p.StandardOutput.ReadToEndAsync();
                    var stderr = p.StandardError.ReadToEndAsync();
                    p.WaitForExit();
                    return (p.ExitCode == 0, (stdout.Result + stderr.Result).Trim());
                }
                catch (Exception ex) { return (false, ex.Message); }
                finally { GrantAccountDbxDirs(c, log); }
            }

            try
            {
                var psi = new ProcessStartInfo(exe, args)
                {
                    RedirectStandardOutput = true,
                    RedirectStandardError  = true,
                    UseShellExecute        = false,
                    CreateNoWindow         = true,
                    LoadUserProfile        = true,   // loads NTUSER.DAT so user env vars are active
                };

                if (c.ServiceAccount.Contains('\\'))
                {
                    var parts  = c.ServiceAccount.Split('\\', 2);
                    psi.Domain   = parts[0];
                    psi.UserName = parts[1];
                }
                else
                {
                    psi.Domain   = ".";              // local machine (also works for UPN user@domain)
                    psi.UserName = c.ServiceAccount;
                }

                var secure = new SecureString();
                foreach (char ch in c.Password) secure.AppendChar(ch);
                secure.MakeReadOnly();
                psi.Password = secure;

                using var p = Process.Start(psi)!;
                string combined = (p.StandardOutput.ReadToEnd() + p.StandardError.ReadToEnd()).Trim();
                p.WaitForExit();
                return (p.ExitCode == 0, combined);
            }
            catch (System.ComponentModel.Win32Exception ex)
            {
                // Process.Start with UserName/Password logs on as the account first — say why that failed
                return (false, $"Could not start '{exe}' as '{c.ServiceAccount}': {DescribeLogonError(ex.NativeErrorCode)}");
            }
            catch (Exception ex)
            {
                return (false, ex.Message);
            }
        }

        private static Task<bool> SetDbxEnvVars(InstallConfig c, InstallLog log)
        {
            // DBXTUNE_HOME = the versioned junction (InstallDir\0), e.g. C:\Users\dbxtune\dbxtune\0
            // DBXTUNE.env.bat auto-detects this as %USERPROFILE%\dbxtune\0, but only if the
            // folder name matches. Setting it explicitly here avoids any naming mismatch.
            string dbxtuneHome = Path.Combine(c.InstallDir, "0");

            var vars = new[]
            {
                ("DBXTUNE_HOME",                dbxtuneHome),
                ("DBXTUNE_USER_HOME",           c.DbxUserHome),
                ("DBXTUNE_CENTRAL_SAVE_DIR",    c.DbxSaveDir),
                ("DBXTUNE_CENTRAL_REPORTS_DIR", c.DbxReportsDir),
                ("DBXTUNE_CENTRAL_LOG_DIR",     c.DbxLogDir),
                ("DBXTUNE_CENTRAL_CONF_DIR",    c.DbxConfDir),
                ("DBXTUNE_CENTRAL_INFO_DIR",    c.DbxInfoDir),
            };

            var toSet = new List<(string name, string value)>();
            foreach (var (name, value) in vars)
            {
                if (string.IsNullOrWhiteSpace(value)) { log($"  Skipping {name} (empty)"); continue; }
                toSet.Add((name, value));
            }

            if (toSet.Count == 0) { log("  All fields empty — no environment variables set."); return Task.FromResult(true); }

            // Write only to the service account's user hive (HKU\<SID>\Environment).
            // The service loads its profile at startup so user env vars are sufficient.
            log($"Setting user environment variables for '{c.ServiceAccount}'…");
            string sid       = LsaPrivileges.GetSidString(c.ServiceAccount);
            bool   wasLoaded = SetUserEnvVars(sid, c.ProfilePath, toSet, log);
            if (!wasLoaded)
                throw new Exception("Profile hive not found — cannot write user environment variables.");

            return Task.FromResult(true);
        }

        // Writes values to HKU\<sid>\Environment, loading NTUSER.DAT if the hive is not already mounted.
        // Returns false when the profile directory / NTUSER.DAT could not be found.
        private static bool SetUserEnvVars(string sid, string profilePath,
            List<(string name, string value)> vars, InstallLog log)
        {
            // Temp key names used when we load the hive ourselves.
            // We also clean these up in case a crashed previous run left them stale.
            const string tempKey  = "__DbxInstaller_tmp__";
            const string tempKey2 = "__DbxInstaller_read__";
            string ntuser = Path.Combine(profilePath, "NTUSER.DAT");

            // Privileges must stay enabled until AFTER RegUnLoadKey — declare at method scope.
            using var priv = LsaPrivileges.EnablePrivileges("SeRestorePrivilege", "SeBackupPrivilege");

            // ── 1. Check if hive is already accessible in HKU ────────────────────
            // Enumerate subkeys — more reliable than OpenSubKey when key ACLs block direct open.
            using var hkuBase = RegistryKey.OpenBaseKey(RegistryHive.Users, RegistryView.Default);
            string? mountedKey = hkuBase.GetSubKeyNames()
                .FirstOrDefault(k => k.Equals(sid, StringComparison.OrdinalIgnoreCase));

            if (mountedKey != null)
            {
                log("  (Hive already mounted in HKU — writing directly)");
                WriteEnvVarsNative(mountedKey, vars, log);
                return true;
            }

            // ── 2. Hive not in HKU — load it ourselves ───────────────────────────
            if (!File.Exists(ntuser)) return false;

            // Unload any stale temp keys left by previous runs (ReadUserEnvVars uses
            // __DbxInstaller_read__; SetUserEnvVars uses __DbxInstaller_tmp__).
            // RegUnLoadKey requires SeRestorePrivilege — hence the outer using above.
            RegUnLoadKey(HkeyUsers, tempKey);
            RegUnLoadKey(HkeyUsers, tempKey2);

            int rc = RegLoadKey(HkeyUsers, tempKey, ntuser);
            if (rc != 0) throw new Exception($"RegLoadKey failed (error {rc})");

            try   { WriteEnvVarsNative(tempKey, vars, log); }
            finally { RegUnLoadKey(HkeyUsers, tempKey); }   // priv still active here

            return true;
        }

        // Write environment variables into HKU\<subKey>\Environment using raw P/Invoke.
        // Creates the Environment key if it does not yet exist.
        private static void WriteEnvVarsNative(string subKey, List<(string name, string value)> vars, InstallLog log)
        {
            string envPath = $@"{subKey}\Environment";
            int rc = RegOpenKeyEx(HkeyUsers, envPath, 0, KEY_SET_VALUE, out IntPtr hKey);
            if (rc != 0)
                rc = RegCreateKeyEx(HkeyUsers, envPath, 0, null, 0, KEY_SET_VALUE,
                                    IntPtr.Zero, out hKey, out _);
            if (rc != 0) throw new Exception($"Cannot open/create HKU\\{envPath} (Win32 error {rc})");

            try
            {
                foreach (var (name, value) in vars)
                {
                    byte[] data = System.Text.Encoding.Unicode.GetBytes(value + '\0');
                    rc = RegSetValueEx(hKey, name, 0, REG_EXPAND_SZ, data, data.Length);
                    if (rc != 0) throw new Exception($"RegSetValueEx for '{name}' failed (Win32 error {rc})");
                    log($"  [User] {name} = {value}");
                }
            }
            finally { RegCloseKey(hKey); }
        }

        // Read DBXTUNE_* environment variables from the service account's user hive.
        // Returns an empty dictionary if the account doesn't exist, has no profile, or any error occurs.
        // Never touches the current user's environment — always reads from HKU\<SID>\Environment.
        public static Dictionary<string, string> ReadUserEnvVars(string accountName)
        {
            var result = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
            try
            {
                string sid         = LsaPrivileges.GetSidString(accountName);
                string profilePath = GetProfilePath(accountName, sid);
                if (string.IsNullOrEmpty(profilePath)) return result;

                const string tempKey = "__DbxInstaller_read__";
                string ntuser        = Path.Combine(profilePath, "NTUSER.DAT");
                bool   hiveLoaded    = false;

                try
                {
                    // Privileges must stay enabled until AFTER RegUnLoadKey in the finally.
                    using var priv = LsaPrivileges.EnablePrivileges("SeRestorePrivilege", "SeBackupPrivilege");

                    // Enumerate HKU subkeys to find the mounted key (more reliable than OpenSubKey).
                    using var hkuBase = RegistryKey.OpenBaseKey(RegistryHive.Users, RegistryView.Default);
                    string? mountedKey = hkuBase.GetSubKeyNames()
                        .FirstOrDefault(k => k.Equals(sid, StringComparison.OrdinalIgnoreCase));

                    string subKey;
                    if (mountedKey != null)
                    {
                        subKey = mountedKey;
                    }
                    else
                    {
                        if (!File.Exists(ntuser)) return result;
                        RegUnLoadKey(HkeyUsers, tempKey);   // clean up stale load if any (priv active)
                        int rc = RegLoadKey(HkeyUsers, tempKey, ntuser);
                        if (rc != 0) return result;         // can't load — return empty silently
                        hiveLoaded = true;
                        subKey = tempKey;
                    }

                    using var env = Registry.Users.OpenSubKey($@"{subKey}\Environment");
                    if (env != null)
                    {
                        foreach (string name in env.GetValueNames())
                            if (name.StartsWith("DBXTUNE_", StringComparison.OrdinalIgnoreCase))
                                result[name] = env.GetValue(name)?.ToString() ?? "";
                    }
                }
                finally
                {
                    if (hiveLoaded) RegUnLoadKey(HkeyUsers, tempKey);
                }
            }
            catch { /* account doesn't exist or access denied — return empty */ }
            return result;
        }

        // Look up the profile path for an account, first from the registry ProfileList,
        // then falling back to the derived C:\Users\<name> path.
        private static string GetProfilePath(string accountName, string sid)
        {
            const string key = @"SOFTWARE\Microsoft\Windows NT\CurrentVersion\ProfileList\";
            using var k = Registry.LocalMachine.OpenSubKey(key + sid);
            if (k?.GetValue("ProfileImagePath") is string path && Directory.Exists(path))
                return path;
            string derived = AccountHome(accountName);
            return Directory.Exists(derived) ? derived : "";
        }

        // 0x80000003 sign-extended — the predefined HKEY_USERS handle
        private static readonly IntPtr HkeyUsers = new IntPtr(unchecked((int)0x80000003));

        private const uint KEY_READ      = 0x20019;
        private const uint KEY_SET_VALUE = 0x00002;
        private const uint REG_EXPAND_SZ = 2;

        [DllImport("advapi32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
        private static extern int RegLoadKey(IntPtr hKey, string subKey, string file);

        [DllImport("advapi32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
        private static extern int RegUnLoadKey(IntPtr hKey, string subKey);

        [DllImport("advapi32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
        private static extern int RegOpenKeyEx(IntPtr hKey, string subKey, int ulOptions,
            uint samDesired, out IntPtr phkResult);

        [DllImport("advapi32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
        private static extern int RegCreateKeyEx(IntPtr hKey, string subKey, int reserved,
            string? lpClass, int dwOptions, uint samDesired, IntPtr lpSecurityAttributes,
            out IntPtr phkResult, out int lpdwDisposition);

        [DllImport("advapi32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
        private static extern int RegSetValueEx(IntPtr hKey, string lpValueName, int reserved,
            uint dwType, byte[] lpData, int cbData);

        [DllImport("advapi32.dll", SetLastError = false)]
        private static extern int RegCloseKey(IntPtr hKey);

        private static Task<bool> CopyExecutables(InstallConfig c, InstallLog log)
        {
            string binDir = Path.Combine(c.InstallDir, "win", "bin");
            Directory.CreateDirectory(binDir);

            var missing = new List<string>();
            foreach (var name in CompanionExeNames)
            {
                string? srcDir = FindCompanionDir(name);
                if (srcDir == null) { log($"  {name}: not found"); missing.Add(name); continue; }
                log($"  {name}: copying from {srcDir}");
                CopyExeDir(Path.Combine(srcDir, name + ".exe"), binDir, log, name);
            }
            if (missing.Count > 0)
                throw new Exception(
                    $"Could not locate: {string.Join(", ", missing)}.\n" +
                    "Place the built executables alongside DbxInstaller.exe or in a sibling directory.");

            return Task.FromResult(true);
        }

        // Locate a companion program's build output directory.
        // Searches: (1) installer's own directory, (2) one level up + sibling sub-folder,
        //           (3) sibling project Debug/Release build outputs (dev layout).
        private static string? FindCompanionDir(string exeName)
        {
            string installerDir = AppDomain.CurrentDomain.BaseDirectory;

            // 1 — same folder as the installer (deployment package)
            if (File.Exists(Path.Combine(installerDir, exeName + ".exe")))
                return installerDir;

            // 2 — one level up from the installer, then a sibling sub-folder named after the exe
            //     Handles a flat deployment like:
            //       parent\
            //         DbxInstaller\DbxInstaller.exe
            //         DbxStarterService\DbxStarterService.exe
            string? parentDir = Path.GetDirectoryName(installerDir.TrimEnd(Path.DirectorySeparatorChar));
            if (parentDir != null)
            {
                if (File.Exists(Path.Combine(parentDir, exeName + ".exe")))
                    return parentDir;
                string siblingDir = Path.Combine(parentDir, exeName);
                if (File.Exists(Path.Combine(siblingDir, exeName + ".exe")))
                    return siblingDir;
            }

            // 3 — sibling project build output (development layout: walk up to solution root)
            // BaseDirectory ends with a separator, so trim it first so GetDirectoryName moves a real level each step.
            string? solutionRoot = installerDir.TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar);
            for (int i = 0; i < 4 && solutionRoot != null; i++)
                solutionRoot = Path.GetDirectoryName(solutionRoot);
            if (solutionRoot == null) return null;

            foreach (var config in new[] { "Release", "Debug" })   // prefer Release
            {
                string binDir = Path.Combine(solutionRoot, exeName, "bin", config);
                if (!Directory.Exists(binDir)) continue;

                // Prefer the regular build output — it always has the runtimes/ subfolder
                // copied by NuGet package targets (e.g. Scintilla5.NET native DLLs).
                // A framework-dependent 'dotnet publish' (no -r RID) omits runtimes/ because
                // it doesn't know the target platform, so we only fall back to publish/ if the
                // regular output isn't found OR if publish/ also contains a runtimes/ folder.
                foreach (var tfm in Directory.GetDirectories(binDir))
                    if (File.Exists(Path.Combine(tfm, exeName + ".exe"))) return tfm;

                // Fall back to published output — valid when published with a specific RID
                // (e.g. dotnet publish -r win-x64) which does include runtimes/ alongside.
                foreach (var tfm in Directory.GetDirectories(binDir))
                {
                    string publishDir = Path.Combine(tfm, "publish");
                    if (File.Exists(Path.Combine(publishDir, exeName + ".exe"))) return publishDir;
                }
            }
            return null;
        }

        // Copy all files (and runtimes/ subfolder) from the directory that contains srcExe into destDir.
        private static void CopyExeDir(string srcExe, string destDir, InstallLog log, string exeName = "")
        {
            if (string.IsNullOrEmpty(srcExe)) return;

            string srcDir = Path.GetDirectoryName(srcExe) ?? "";
            if (!Directory.Exists(srcDir))
                throw new Exception($"Source directory not found: {srcDir}");

            log($"  Copying from: {srcDir}");
            int count = 0;
            foreach (string srcFile in Directory.GetFiles(srcDir))
            {
                string dest = Path.Combine(destDir, Path.GetFileName(srcFile));
                File.Copy(srcFile, dest, overwrite: true);
                count++;
            }

            // Copy runtimes/ subfolder if present (native platform binaries needed by some .NET packages)
            string runtimes = Path.Combine(srcDir, "runtimes");
            if (Directory.Exists(runtimes))
            {
                foreach (string srcFile in Directory.GetFiles(runtimes, "*", SearchOption.AllDirectories))
                {
                    string rel  = Path.GetRelativePath(srcDir, srcFile);
                    string dest = Path.Combine(destDir, rel);
                    Directory.CreateDirectory(Path.GetDirectoryName(dest)!);
                    File.Copy(srcFile, dest, overwrite: true);
                    count++;
                }
            }

            if (count == 0)
                throw new Exception(
                    $"No files found in {srcDir}" +
                    (string.IsNullOrEmpty(exeName) ? "" : $" — build {exeName} first."));

            log($"  {count} files copied → {destDir}");
        }

        private static Task<bool> RegisterService(InstallConfig c, InstallLog log)
        {
            string exePath = Path.Combine(c.InstallDir, "win", "bin", "DbxStarterService.exe");
            if (!File.Exists(exePath))
                throw new Exception(
                    $"DbxStarterService.exe not found at: {exePath}\n" +
                    "The Copy step should have placed it there — check that CopyExecutables succeeded.");

            Run("sc", "stop DbxStarterService");
            Run("sc", "delete DbxStarterService");

            log($"Registering: {exePath}");
            var (ok, output) = Run("sc",
                $"create DbxStarterService binPath= \"{exePath}\" " +
                "start= auto DisplayName= \"DbxStarter Service\"");
            log(output);
            if (!ok) throw new Exception("sc create failed.");

            Run("sc", "description DbxStarterService \"Manages and monitors DbxTune server processes\"");
            return Task.FromResult(true);
        }

        // Writes DbxStarterService.json to DBXTUNE_CENTRAL_CONF_DIR and records the path
        // in the Windows service registry so DbxStarterService.exe can find it after any upgrade.
        //
        // Merge strategy:
        //   Fresh install  → write {WebPort, WebBind} from installer UI.
        //   Re-install     → read existing file, preserve unknown keys, override WebPort and WebBind.
        // Reads a single bool key from DbxStarterService.json's "DbxStarter" section — used by
        // the DbxCentral Web Config page to show the current on-disk "ManageDbxCentral" value
        // (that key is written during install by WriteServiceConfig, before this page is shown).
        public static bool ReadServiceConfigBool(string confDir, string key, bool defaultValue)
        {
            string path = Path.Combine(confDir, "DbxStarterService.json");
            if (!File.Exists(path)) return defaultValue;
            try
            {
                using var doc = JsonDocument.Parse(File.ReadAllText(path));
                if (doc.RootElement.TryGetProperty("DbxStarter", out var section) &&
                    section.TryGetProperty(key, out var el))
                    return el.GetBoolean();
            }
            catch { /* malformed/missing — fall back to default */ }
            return defaultValue;
        }

        // Writes a single bool key into DbxStarterService.json's "DbxStarter" section, preserving
        // every other existing key (WebPort, WebBind, etc.) — same merge approach as WriteServiceConfig.
        public static void WriteServiceConfigBool(string confDir, string key, bool value)
        {
            string path = Path.Combine(confDir, "DbxStarterService.json");
            var section = new JsonObject();
            if (File.Exists(path))
            {
                try
                {
                    var existingRoot = JsonNode.Parse(File.ReadAllText(path));
                    if (existingRoot?["DbxStarter"] is JsonObject existingSection)
                        foreach (var kvp in existingSection)
                            section[kvp.Key] = kvp.Value?.DeepClone();
                }
                catch { /* malformed — start fresh, matches WriteServiceConfig's own fallback */ }
            }
            section[key] = value;

            var root = new JsonObject { ["DbxStarter"] = section };
            string json = root.ToJsonString(new JsonSerializerOptions { WriteIndented = true }) + "\n";
            Directory.CreateDirectory(Path.GetDirectoryName(path)!);
            File.WriteAllText(path, json, System.Text.Encoding.UTF8);
        }

        private static Task<bool> WriteServiceConfig(InstallConfig c, InstallLog log)
        {
            string configPath = Path.Combine(c.DbxConfDir, "DbxStarterService.json");
            log($"Config file: {configPath}");

            // Build the new DbxStarter section, starting with any keys that already exist.
            var section = new JsonObject();
            if (File.Exists(configPath))
            {
                try
                {
                    var existingRoot = JsonNode.Parse(File.ReadAllText(configPath));
                    if (existingRoot?["DbxStarter"] is JsonObject existingSection)
                    {
                        foreach (var kvp in existingSection)
                            section[kvp.Key] = kvp.Value?.DeepClone();
                        log($"  Found existing config — merging {existingSection.Count} existing key(s).");
                    }
                }
                catch (Exception ex)
                {
                    log($"  Warning: could not parse existing config ({ex.Message}) — starting fresh.");
                }
            }
            else
            {
                log("  No existing config — creating from installer values.");
            }

            // Installer-chosen values always override whatever was in the file.
            string bindValue = string.IsNullOrWhiteSpace(c.WebBind) ? "localhost" : c.WebBind;
            section["WebPort"]          = c.WebPort;
            section["WebBind"]          = bindValue;
            section["ManageDbxCentral"] = c.ManageDbxCentral;

            var root = new JsonObject { ["DbxStarter"] = section };
            string json = root.ToJsonString(new JsonSerializerOptions { WriteIndented = true }) + "\n";

            Directory.CreateDirectory(Path.GetDirectoryName(configPath)!);
            File.WriteAllText(configPath, json, System.Text.Encoding.UTF8);

            if (c.WebPort < 0)
                log("  Written  (web UI disabled — WebPort = -1)");
            else
                log($"  Written  (WebPort = {c.WebPort}, WebBind = {bindValue})");

            // Record the conf dir in the service registry Parameters key so
            // DbxStarterService.exe can locate DbxStarterService.json after future upgrades.
            const string regPath = @"SYSTEM\CurrentControlSet\Services\DbxStarterService\Parameters";
            try
            {
                using var key = Registry.LocalMachine.CreateSubKey(regPath, writable: true);
                key?.SetValue("ConfigDir", c.DbxConfDir, RegistryValueKind.String);
                log($"  Registry ConfigDir set: HKLM\\{regPath}\\ConfigDir = {c.DbxConfDir}");
            }
            catch (Exception ex)
            {
                log($"  Warning: could not write ConfigDir to registry: {ex.Message}");
                log("  The service will fall back to the exe directory for its config file.");
            }

            return Task.FromResult(true);
        }

        private static Task<bool> ConfigureServiceAccount(InstallConfig c, InstallLog log)
        {
            var (exists, _) = Run("sc", "query DbxStarterService");
            if (!exists)
                throw new Exception(
                    "DbxStarterService is not registered — the Register step must succeed before configuring the account.");

            // sc config expects:
            //   local account  → obj= ".\username"      password= "secret"
            //   domain account → obj= "DOMAIN\username" password= "secret"
            //   gMSA           → obj= "DOMAIN\name$"    password= ""  (AD manages it)
            string objArg = IsDomain(c.ServiceAccount)
                ? c.ServiceAccount                  // domain\user or DOMAIN\svc$
                : $".\\{c.ServiceAccount}";         // local

            string pwdArg = IsGmsa(c.ServiceAccount) ? "" : c.Password;

            log($"Service will run as: {objArg}");
            if (IsGmsa(c.ServiceAccount))
                log("gMSA — password managed by Active Directory, passing empty string to sc.");

            var (ok, output) = Run("sc",
                $"config DbxStarterService obj= \"{objArg}\" password= \"{pwdArg}\"");
            log(output);
            if (!ok) throw new Exception("Failed to configure service account.");

            Run("sc", "failure DbxStarterService reset= 86400 actions= restart/60000/restart/60000/reboot/60000");
            log("Failure recovery: restart → restart → reboot.");

            // Grant the service account rights to start/stop/query the service.
            // By default only Administrators can manage services; we must add an explicit
            // ACE to the service DACL so the dbxtune user can control its own service.
            GrantServiceControlRights(c, log);

            return Task.FromResult(true);
        }

        // Appends an ACE to DbxStarterService's DACL that grants the service account:
        //   CC = SERVICE_QUERY_CONFIG
        //   LC = SERVICE_QUERY_STATUS
        //   RP = SERVICE_START
        //   WP = SERVICE_STOP
        private static void GrantServiceControlRights(InstallConfig c, InstallLog log)
        {
            log($"Granting start/stop rights on DbxStarterService to '{c.ServiceAccount}'…");
            try
            {
                string sid = LsaPrivileges.GetSidString(c.ServiceAccount);
                string ace = $"(A;;CCLCRPWP;;;{sid})";

                var (_, raw) = Run("sc", "sdshow DbxStarterService");

                // sc sdshow outputs a blank line before the SDDL on most Windows versions.
                // Find the first non-empty line that looks like an SDDL string.
                string sddl = (raw ?? "")
                    .Split('\n', StringSplitOptions.RemoveEmptyEntries)
                    .Select(l => l.Trim())
                    .FirstOrDefault(l => l.StartsWith("D:", StringComparison.Ordinal)
                                     || l.StartsWith("O:", StringComparison.Ordinal)
                                     || l.StartsWith("G:", StringComparison.Ordinal))
                    ?? "";

                if (string.IsNullOrEmpty(sddl))
                {
                    log($"  Warning: could not parse SDDL from sc sdshow output — skipping.");
                    log($"  Raw output: {raw?.Trim()}");
                    return;
                }

                log($"  Current SDDL: {sddl}");

                // Avoid duplicating the ACE on re-install
                if (sddl.Contains(sid, StringComparison.OrdinalIgnoreCase))
                {
                    log("  ACE already present — skipping.");
                    return;
                }

                // Insert the allow ACE at the start of the D:() DACL section.
                // SDDL layout: [O:...][G:...]D:[flags](aces...)[S:[flags](aces...)]
                // We must insert inside D:(...), NOT after S:(...).
                int dPos = sddl.IndexOf("D:", StringComparison.Ordinal);
                if (dPos < 0)
                {
                    log("  Warning: D: section not found in SDDL — cannot modify.");
                    return;
                }

                // Skip over any DACL flags (e.g. "D:PAI") to reach the first '('
                int insertAt = sddl.IndexOf('(', dPos + 2);
                string newSddl = insertAt >= 0
                    ? sddl[..insertAt] + ace + sddl[insertAt..]   // insert before first ACE
                    : sddl[..(dPos + 2)] + ace + sddl[(dPos + 2)..]; // empty DACL

                log($"  New SDDL:     {newSddl}");

                // sc sdset does NOT accept quotes around the SDDL descriptor.
                var (ok, setOut) = Run("sc", $"sdset DbxStarterService {newSddl}");
                if (!string.IsNullOrWhiteSpace(setOut)) log($"  {setOut.Trim()}");
                log(ok ? "  Done." : "  Warning: sc sdset returned a non-zero exit code.");
            }
            catch (Exception ex)
            {
                log($"  Warning: could not grant service control rights: {ex.Message}");
                log("  The service account will not be able to start/stop the service without admin rights.");
            }
        }

        // Runs (optionally) starts the service and/or launches the GUI client — the decisions are
        // made by the caller (a wizard page checkbox) rather than via a MessageBox prompt here.
        internal static Task<bool> StartServiceCore(InstallConfig c, InstallLog log, bool startService, bool launchClient, bool launchWebUi = false, bool launchCentralUi = false)
        {
            const string svcName = "DbxStarterService";
            int          webPort = c.WebPort;

            if (startService)
            {
                log($"Starting {svcName}…");
                var (ok, output) = Run("sc", $"start {svcName}");
                if (!string.IsNullOrWhiteSpace(output)) log($"  {output}");
                if (ok) log($"  Service started.");
                else    log($"  Warning: sc start returned a non-zero exit code — the service may already be running.");
            }
            else
            {
                log($"Service start skipped — start it manually with:  sc start {svcName}");
            }

            log("");
            string clientExe = Path.Combine(c.InstallDir, "win", "bin", "DbxStarterClient.exe");
            if (launchClient)
            {
                if (File.Exists(clientExe))
                {
                    log(RunAsAdmin(c)
                        ? $"Launching DbxStarterClient as you ({RunAsAdminReason(c)})…"
                        : $"Launching DbxStarterClient as '{c.ServiceAccount}'…");
                    string? launchError = LaunchAsUser(clientExe, "", c);
                    log(launchError == null ? "  Client launched." : $"  WARNING: could not start DbxStarterClient: {launchError}");
                }
                else
                {
                    log($"  WARNING: DbxStarterClient.exe not found at: {clientExe}");
                }
            }
            else
            {
                log($"Client launch skipped — run manually: {clientExe}");
            }

            log("");
            if (webPort < 0)
            {
                log("  Web server is disabled (WebPort = -1) — no web UI available.");
            }
            else
            {
                bool anyNet  = c.WebBind.Equals("*",   StringComparison.OrdinalIgnoreCase)
                            || c.WebBind.Equals("any", StringComparison.OrdinalIgnoreCase);
                string host  = anyNet ? Environment.MachineName : "localhost";
                string url   = $"http://{host}:{webPort}";
                log($"  DbxStarter web UI is available at:");
                log($"  ➜  {url}");
                if (anyNet)
                    log("  (Also reachable from other machines on the network.)");
                log("");
                log("  (The web UI shows live log output and lets you open log files.)");

                if (launchWebUi)
                {
                    if (startService)
                    {
                        try
                        {
                            Process.Start(new ProcessStartInfo(url) { UseShellExecute = true });
                            log("  Opened in default browser.");
                        }
                        catch (Exception ex) { log($"  Could not open browser: {ex.Message}"); }
                    }
                    else
                    {
                        log("  Browser launch skipped — service was not started.");
                    }
                }
            }

            log("");
            if (!c.ManageDbxCentral)
            {
                log("  DbxTune Central is not managed on this node (collector-only) — no landing page to open.");
            }
            else
            {
                // Same file/key/default ConfigureFirewall reads to open the matching firewall rule.
                string centralConfFile = Path.Combine(c.DbxConfDir, "DBX_CENTRAL.conf");
                int    centralPort     = ReadJavaPropInt(centralConfFile, "DbxTuneCentral.web.http.port.windows", 80);
                string centralUrl      = $"http://localhost:{centralPort}/";
                log($"  DbxTune Central landing page:");
                log($"  ➜  {centralUrl}");

                if (launchCentralUi)
                {
                    if (startService)
                    {
                        try
                        {
                            Process.Start(new ProcessStartInfo(centralUrl) { UseShellExecute = true });
                            log("  Opened in default browser.");
                        }
                        catch (Exception ex) { log($"  Could not open browser: {ex.Message}"); }
                    }
                    else
                    {
                        log("  Browser launch skipped — service was not started.");
                    }
                }
            }

            return Task.FromResult(true);
        }

        // ── DBMS configuration — decomposed into plain data-in/data-out steps ───
        // driven interactively by DbmsConfigWizardPage. No dialogs are shown here;
        // the wizard page owns all Next/Skip and inline-editing UI.

        // Look up the DbmsProfile records for the selected DBMS display names, in order.
        public static List<DbmsProfile> GetSelectedDbmsProfiles(InstallConfig c) =>
            c.SelectedDbms
                .Select(name => DbmsProfiles.FirstOrDefault(p => p.DisplayName == name))
                .Where(p => p != null)
                .Select(p => p!)
                .ToList();

        public static string GetTuneWizardBatPath(DbmsProfile profile, InstallConfig c) =>
            Path.Combine(c.InstallDir, "0", "bin", profile.TuneExe + ".bat");

        public static string GetStartScriptPath(DbmsProfile profile, InstallConfig c) =>
            Path.Combine(c.DbxUserHome, "dbxc", "bin", profile.StartScript);

        public static string GetServerListPath(InstallConfig c) =>
            Path.Combine(c.DbxConfDir, "SERVER_LIST");

        // Runs "<tuneExe>.bat --cfgWizard" as the service account and returns the wizard's
        // reported config file path (parsed from "WIZARD_CONFIG_FILE: <path>" in its output),
        // or "" if the wizard didn't report one. Caller decides what to do with the result
        // (e.g. copy to clipboard via System.Windows.Clipboard on the WPF side).
        public static string RunDbmsCfgWizard(DbmsProfile profile, InstallConfig c, InstallLog log)
        {
            string tuneBat = GetTuneWizardBatPath(profile, c);
            log($"Running as '{c.ServiceAccount}': {tuneBat} --cfgWizard");
            string wizardOut = RunInteractiveAsUser(tuneBat, "--cfgWizard", c, log);
            log("Wizard closed.");

            var cfgMatch = Regex.Match(wizardOut, @"WIZARD_CONFIG_FILE:\s*(.+)", RegexOptions.Multiline);
            if (!cfgMatch.Success) return "";
            string cfgFile = cfgMatch.Groups[1].Value.Trim();
            log($"Config file: {cfgFile}");
            return cfgFile;
        }

        // Reads the embedded DB-user setup-instructions resource for a DBMS profile as plain text
        // for inline (read-only) display in the wizard.
        public static string GetDbSetupInstructionsText(DbmsProfile profile)
        {
            string resName = $"DbxInstaller.DbmsSetup.{profile.SetupFile}";
            try
            {
                var asm = Assembly.GetExecutingAssembly();
                using var src = asm.GetManifestResourceStream(resName);
                if (src == null) return $"(Embedded setup file not found: {resName})";
                using var reader = new StreamReader(src, System.Text.Encoding.UTF8);
                return reader.ReadToEnd();
            }
            catch (Exception ex) { return $"(Could not read setup instructions: {ex.Message})"; }
        }

        // Run an interactive/GUI process as service user, wait for it to exit, and return its stdout+stderr.
        // stdout is captured so we can parse wizard output (e.g. WIZARD_CONFIG_FILE); the GUI still appears normally.
        private static string RunInteractiveAsUser(string exe, string args, InstallConfig c, InstallLog log)
        {
            if (RunAsAdmin(c))
            {
                log($"  ({RunAsAdminReason(c)}: running as you, with the account's folders)");
                try
                {
                    var psi = AdminPsiForAccount(exe, args, c);
                    psi.RedirectStandardOutput = true;   // the wizard reports its config file on stdout
                    psi.RedirectStandardError  = true;
                    using var p = Process.Start(psi)!;
                    var stdout = p.StandardOutput.ReadToEndAsync();
                    var stderr = p.StandardError.ReadToEndAsync();
                    p.WaitForExit();
                    return stdout.Result + stderr.Result;
                }
                catch (Exception ex) { log($"    Warning: {ex.Message}"); return ""; }
                finally { GrantAccountDbxDirs(c, log); }
            }
            try
            {
                var psi = BuildUserPsi(exe, args, c);
                psi.UseShellExecute        = false;
                psi.RedirectStandardOutput = true;
                psi.RedirectStandardError  = true;
                using var p = Process.Start(psi)!;
                // Read asynchronously to avoid blocking the GUI and prevent buffer deadlock
                var stdoutTask = p.StandardOutput.ReadToEndAsync();
                var stderrTask = p.StandardError.ReadToEndAsync();
                p.WaitForExit();
                return stdoutTask.Result + stderrTask.Result;
            }
            catch (System.ComponentModel.Win32Exception ex) { log($"    Warning: could not start it as '{c.ServiceAccount}': {DescribeLogonError(ex.NativeErrorCode)}"); return ""; }
            catch (Exception ex) { log($"    Warning: {ex.Message}"); return ""; }
        }

        // Launch a process as service user without waiting (fire-and-forget). Best effort: returns
        // null when it started, else why not (the caller logs it — the install goes on either way).
        private static string? LaunchAsUser(string exe, string args, InstallConfig c)
        {
            try
            {
                if (RunAsAdmin(c))   // the client talks to the service over its pipe, which allows any user
                {
                    Process.Start(new ProcessStartInfo(exe, args) { UseShellExecute = true });
                    return null;
                }
                var psi = BuildUserPsi(exe, args, c);
                psi.UseShellExecute = false;
                Process.Start(psi);
                return null;
            }
            catch (System.ComponentModel.Win32Exception ex) { return DescribeLogonError(ex.NativeErrorCode); }
            catch (Exception ex) { return ex.Message; }
        }

        // Build a ProcessStartInfo with service-account credentials (no stdout redirect).
        private static ProcessStartInfo BuildUserPsi(string exe, string args, InstallConfig c)
        {
            var psi = new ProcessStartInfo(exe, args)
            {
                RedirectStandardOutput = false,
                RedirectStandardError  = false,
                UseShellExecute        = false,
                LoadUserProfile        = true,
            };

            if (c.ServiceAccount.Contains('\\'))
            {
                var parts  = c.ServiceAccount.Split('\\', 2);
                psi.Domain   = parts[0];
                psi.UserName = parts[1];
            }
            else
            {
                psi.Domain   = ".";
                psi.UserName = c.ServiceAccount;
            }

            var secure = new SecureString();
            foreach (char ch in c.Password) secure.AppendChar(ch);
            secure.MakeReadOnly();
            psi.Password = secure;
            return psi;
        }

        // ── firewall ──────────────────────────────────────────────────────────

        // Consistent rule names — uninstaller removes by these names.
        public const string FwRuleCentralHttp  = "DbxTune - Central HTTP";
        public const string FwRuleCentralHttps = "DbxTune - Central HTTPS";
        public const string FwRuleStarterWeb   = "DbxTune - Starter Web UI";

        private static Task<bool> ConfigureFirewall(InstallConfig c, InstallLog log)
        {
            string confFile = Path.Combine(c.DbxConfDir, "DBX_CENTRAL.conf");

            // HTTP — always open; default 80 on Windows
            int httpPort = ReadJavaPropInt(confFile,
                "DbxTuneCentral.web.http.port.windows", 80);
            AddFirewallRule(FwRuleCentralHttp, httpPort, log);

            // HTTPS — opt-in: only when the key is explicitly set in the conf file
            int? httpsPort = ReadJavaPropIntOrNull(confFile,
                "DbxTuneCentral.web.https.port.windows");
            if (httpsPort.HasValue)
                AddFirewallRule(FwRuleCentralHttps, httpsPort.Value, log);
            else
                log("  DbxCentral HTTPS key not set in DBX_CENTRAL.conf — skipping port 443 rule.");

            // DbxStarterService web UI — only when binding to all interfaces
            bool allInterfaces = c.WebPort > 0 &&
                                 !c.WebBind.Equals("localhost", StringComparison.OrdinalIgnoreCase);
            if (allInterfaces)
                AddFirewallRule(FwRuleStarterWeb, c.WebPort, log);
            else
                log($"  DbxStarterService web UI is localhost-only — no firewall rule needed.");

            return Task.FromResult(true);
        }

        private static void AddFirewallRule(string name, int port, InstallLog log)
        {
            // Delete first so re-install is idempotent (no duplicate entries)
            Run("netsh", $"advfirewall firewall delete rule name=\"{name}\"");

            var (ok, output) = Run("netsh",
                $"advfirewall firewall add rule " +
                $"name=\"{name}\" dir=in action=allow protocol=TCP localport={port} " +
                $"description=\"Managed by DbxInstaller — do not edit manually\"");

            if (ok || output.Contains("Ok", StringComparison.OrdinalIgnoreCase))
                log($"  ✓ Firewall rule '{name}'  →  TCP inbound port {port}");
            else
                log($"  Warning: could not add rule '{name}' (port {port}): {output.Trim()}");
        }

        // Removes all DbxTune firewall rules. Called by the uninstaller.
        public static void RemoveFirewallRules(Action<string> log)
        {
            foreach (var name in new[] { FwRuleCentralHttp, FwRuleCentralHttps, FwRuleStarterWeb })
            {
                var (ok, output) = Run("netsh",
                    $"advfirewall firewall delete rule name=\"{name}\"");
                if (ok || output.Contains("Deleted", StringComparison.OrdinalIgnoreCase))
                    log($"  ✓ Removed firewall rule '{name}'");
                else if (output.Contains("No rules", StringComparison.OrdinalIgnoreCase))
                    log($"  '{name}' — not present, skipping.");
                else
                    log($"  Warning: {output.Trim()}");
            }
        }

        // Read a single integer value from a Java .properties file.
        // Returns defaultValue when the file is missing or the key is not found.
        public static int ReadJavaPropInt(string path, string key, int defaultValue) =>
            ReadJavaPropIntOrNull(path, key) ?? defaultValue;

        // Writes a single key's value into a Java .properties file, preserving every other line
        // (comments, other keys, ordering):
        //   - An active "key = value" line is updated in place.
        //   - A commented-out line for the same key (e.g. "#key = value") is uncommented and
        //     updated in place, rather than left disabled while a second active line is added.
        //   - If the key doesn't appear at all, a new line is inserted at the TOP of the file
        //     (not appended at the end) so newly-added settings are easy to spot.
        public static void WriteJavaPropInt(string path, string key, int value) => WriteJavaProp(path, key, value.ToString());

        // Same algorithm as WriteJavaPropInt, for free-text values (e.g. mail settings).
        public static void WriteJavaProp(string path, string key, string value)
        {
            var lines = File.Exists(path) ? File.ReadAllLines(path).ToList() : new List<string>();

            int activeIndex = -1, commentedIndex = -1;
            for (int i = 0; i < lines.Count; i++)
            {
                string trimmed = lines[i].TrimStart();
                bool isComment = trimmed.Length > 0 && (trimmed[0] == '#' || trimmed[0] == '!');
                string body = isComment ? trimmed.TrimStart('#', '!').TrimStart() : trimmed;

                int eq = body.IndexOf('=');
                if (eq < 0 || !body[..eq].Trim().Equals(key, StringComparison.OrdinalIgnoreCase)) continue;

                if (!isComment) { activeIndex = i; break; }  // an active entry always wins outright
                if (commentedIndex < 0) commentedIndex = i;  // remember the first commented match
            }

            if (activeIndex >= 0)
            {
                int eq = lines[activeIndex].IndexOf('=');
                lines[activeIndex] = $"{lines[activeIndex][..(eq + 1)]} {value}";
            }
            else if (commentedIndex >= 0)
            {
                // Strip only the leading #/! marker(s) (keep indentation and the rest of the line
                // as-is), then set the value the same way as an active line.
                string uncommented = Regex.Replace(lines[commentedIndex], @"^(\s*)[#!]+\s?", "$1");
                int eq = uncommented.IndexOf('=');
                lines[commentedIndex] = $"{uncommented[..(eq + 1)]} {value}";
            }
            else
            {
                lines.Insert(0, $"{key} = {value}");
            }

            File.WriteAllLines(path, lines, System.Text.Encoding.UTF8);
        }

        // Searches one or more DBMS collector .properties files (as reported by
        // RunDbmsCfgWizard) for mail settings already configured there, so the DbxCentral Web
        // Config page can offer to reuse them instead of asking the user to re-enter the same
        // SMTP host/to/from a second time. Recognizes the generic "mail.*" key plus any
        // writer-specific "*ToMail.*" key (e.g. AlarmWriterToMail, ReportSenderToMail, or a
        // custom writer name) — matches the key patterns DbxTune's own collectors use.
        // Active (uncommented) matches win; a commented-out value is only used if nothing
        // active was found in any of the files.
        public static (string? Hostname, string? To, string? From) FindDbmsMailSettings(IEnumerable<string> cfgFilePaths)
        {
            var paths = cfgFilePaths.Where(p => !string.IsNullOrEmpty(p)).Distinct().ToList();
            return (
                FindMailProp(paths, "smtp.hostname"),
                FindMailProp(paths, "to"),
                FindMailProp(paths, "from"));
        }

        private static string? FindMailProp(List<string> filePaths, string suffix)
        {
            var keyPattern = MailKeyRegex(suffix);
            string? commentedFallback = null;
            foreach (var path in filePaths)
            {
                if (!File.Exists(path)) continue;
                foreach (var rawLine in File.ReadLines(path))
                {
                    string trimmed = rawLine.TrimStart();
                    bool isComment = trimmed.Length > 0 && (trimmed[0] == '#' || trimmed[0] == '!');
                    string body = isComment ? trimmed.TrimStart('#', '!').TrimStart() : trimmed;

                    int eq = body.IndexOf('=');
                    if (eq < 0 || !keyPattern.IsMatch(body[..eq].Trim())) continue;

                    string val = body[(eq + 1)..].Trim();
                    if (val.Length == 0) continue;
                    if (!isComment) return val;
                    commentedFallback ??= val;
                }
            }
            return commentedFallback;
        }

        private static Regex MailKeyRegex(string suffix) =>
            new(@"^(?:mail|\w*ToMail)\." + Regex.Escape(suffix) + "$", RegexOptions.IgnoreCase);

        // Reads the value of whichever mail key is currently active in a Java .properties file
        // for the given suffix (e.g. "smtp.hostname") — "mail.*" or any writer-specific "*ToMail.*"
        // key. Commented-out entries don't count as a "current" value.
        public static string? ReadMailProp(string path, string suffix)
        {
            if (!File.Exists(path)) return null;
            var keyPattern = MailKeyRegex(suffix);
            foreach (var rawLine in File.ReadLines(path))
            {
                var line = rawLine.Trim();
                if (line.Length == 0 || line[0] == '#' || line[0] == '!') continue;
                int eq = line.IndexOf('=');
                if (eq < 0 || !keyPattern.IsMatch(line[..eq].Trim())) continue;
                string val = line[(eq + 1)..].Trim();
                if (val.Length > 0) return val;
            }
            return null;
        }

        // Sets a mail value for the given suffix in a Java .properties file. Unlike
        // WriteJavaProp (which targets one fixed key), this updates EVERY distinct key already
        // present in the file that matches the pattern — e.g. if both "mail.to" and a commented
        // "AlarmWriterToMail.to" exist, both get the new value (each via WriteJavaProp's own
        // active/commented/insert algorithm, so an existing commented-only key is uncommented
        // rather than duplicated). If no matching key exists at all, a new "mail.<suffix>" line
        // is inserted at the top of the file.
        public static void WriteMailProp(string path, string suffix, string value)
        {
            var keys = FindMatchingKeys(path, MailKeyRegex(suffix));
            if (keys.Count == 0) { WriteJavaProp(path, "mail." + suffix, value); return; }
            foreach (var key in keys) WriteJavaProp(path, key, value);
        }

        // Distinct key names (active or commented) in a Java .properties file matching pattern.
        private static List<string> FindMatchingKeys(string path, Regex pattern)
        {
            var keys = new List<string>();
            if (!File.Exists(path)) return keys;
            foreach (var rawLine in File.ReadLines(path))
            {
                string trimmed = rawLine.TrimStart();
                bool isComment = trimmed.Length > 0 && (trimmed[0] == '#' || trimmed[0] == '!');
                string body = isComment ? trimmed.TrimStart('#', '!').TrimStart() : trimmed;
                int eq = body.IndexOf('=');
                if (eq < 0) continue;
                string key = body[..eq].Trim();
                if (pattern.IsMatch(key) && !keys.Contains(key, StringComparer.OrdinalIgnoreCase)) keys.Add(key);
            }
            return keys;
        }

        // Returns null when the file is missing or the key is absent (as opposed to set to 0).
        private static int? ReadJavaPropIntOrNull(string path, string key)
        {
            if (!File.Exists(path)) return null;
            try
            {
                foreach (var rawLine in File.ReadLines(path))
                {
                    var line = rawLine.Trim();
                    if (line.Length == 0 || line[0] == '#' || line[0] == '!') continue;
                    int eq = line.IndexOf('=');
                    if (eq < 0) continue;
                    if (!line[..eq].Trim().Equals(key, StringComparison.OrdinalIgnoreCase)) continue;
                    string val = line[(eq + 1)..].Trim();
                    if (int.TryParse(val, out int n)) return n;
                }
            }
            catch { /* unreadable file — treat as absent */ }
            return null;
        }

        // ── download ──────────────────────────────────────────────────────────

        private static async Task DownloadFileAsync(string url, string destPath, Action<int> onProgress, System.Threading.CancellationToken cancelToken)
        {
            using var handler = new HttpClientHandler { AllowAutoRedirect = true, MaxAutomaticRedirections = 10 };
            using var client  = new HttpClient(handler);
            client.Timeout = TimeSpan.FromMinutes(30);
            client.DefaultRequestHeaders.Add("User-Agent", "DbxInstaller/1.0");

            using var response = await client.GetAsync(url, HttpCompletionOption.ResponseHeadersRead, cancelToken);
            response.EnsureSuccessStatusCode();

            long? total   = response.Content.Headers.ContentLength;
            using var src = await response.Content.ReadAsStreamAsync(cancelToken);
            using var dst = File.Create(destPath);

            byte[] buf     = new byte[65_536];
            long   fetched = 0;
            int    lastPct = -1, read;

            while ((read = await src.ReadAsync(buf, cancelToken)) > 0)
            {
                await dst.WriteAsync(buf.AsMemory(0, read), cancelToken);
                fetched += read;
                int pct = total.HasValue ? (int)(fetched * 100 / total.Value) : -1;
                if (pct != lastPct && (pct < 0 || pct % 5 == 0))
                {
                    onProgress(pct);
                    lastPct = pct;
                }
            }
        }

        // ── helpers ───────────────────────────────────────────────────────────

        private static void CopyFile(string src, string destDir, InstallLog log)
        {
            if (string.IsNullOrEmpty(src)) return;
            if (!File.Exists(src)) { log($"  NOT FOUND — skipping: {src}"); return; }
            string dest = Path.Combine(destDir, Path.GetFileName(src));
            File.Copy(src, dest, overwrite: true);
            log($"  {Path.GetFileName(src)} → {dest}");
        }

        // Verify that the supplied password is correct for an existing account — throws on
        // failure, for use inside an install step (CreateServiceAccount) where that should abort
        // the step. Thin wrapper around TryVerifyPassword, which the "Check" button on the
        // Service Account page uses directly for its non-throwing, interactive version.
        private static void VerifyPassword(InstallConfig c, InstallLog log)
        {
            log($"  Verifying password for '{c.ServiceAccount}'…");
            var (outcome, message) = TryVerifyPassword(c.ServiceAccount, c.Password);
            if (outcome == VerifyOutcome.Failed) throw new Exception(message);
            log($"  {message}");
        }

        // Verified: the password is correct. Info: not a problem for the install — either nothing
        // could be checked (gMSA, or a local account that doesn't exist yet and will be created
        // during install), or the password is correct but the account may not log on locally yet
        // (1385 — the install grants that right). The Service Account page shows this differently
        // from a real failure. Failed: a genuine problem (wrong password, a domain account that
        // can't be found, a disabled/locked account, or the logon check itself errored).
        public enum VerifyOutcome { Verified, Info, Failed }

        // Attempts an interactive logon with the given account/password to verify the password is
        // correct. Works for local accounts, domain accounts (DOMAIN\user or user@domain), and
        // reports gMSA accounts as unverifiable (AD manages their password — no interactive logon
        // is possible, so this isn't a failure, just "nothing to check").
        // upgrade: called from the Upgrade flow (its 1385 text names what an upgrade grants).
        public static (VerifyOutcome Outcome, string Message) TryVerifyPassword(string account, string password, bool upgrade = false)
        {
            if (IsGmsa(account))
                return (VerifyOutcome.Info, "gMSA — password is managed by Active Directory; nothing to verify here.");

            // Windows deliberately reports "wrong password" and "no such local account" as the
            // same ERROR_LOGON_FAILURE from LogonUser (anti-enumeration behavior), so a missing
            // local account would otherwise show as "Incorrect password" — check existence
            // ourselves first via LookupAccountName (resolves local and domain accounts alike).
            // The installer only creates LOCAL accounts (a plain name) — a DOMAIN\user or user@domain
            // account that can't be found is a typo or an unreachable domain controller, and the
            // install would fail on it later, so that one is an error, not "will be created".
            bool domainAccount = IsDomain(account);
            string notFound = domainAccount
                ? $"Account '{account}' was not found — check the spelling (DOMAIN\\user or user@domain.com) and that a " +
                  "domain controller is reachable. The installer creates only local accounts given as a plain name (e.g. dbxtune)."
                : $"Account '{account}' does not exist yet — it will be created during install, so there's nothing to verify against.";
            try { LsaPrivileges.GetSidString(account); }
            catch { return (domainAccount ? VerifyOutcome.Failed : VerifyOutcome.Info, notFound); }

            int err = LogonAs(account, password, LOGON32_LOGON_INTERACTIVE, sids: null);
            return err switch
            {
                0                            => (VerifyOutcome.Verified, "Password verified — OK."),
                ERROR_LOGON_FAILURE          => (VerifyOutcome.Failed, $"Incorrect password for '{account}'."),
                // Windows checks the logon TYPE only after it has accepted the password.
                ERROR_LOGON_TYPE_NOT_GRANTED => (VerifyOutcome.Info, LogonTypeNotGrantedMessage(account, upgrade)),
                // Belt-and-suspenders: the existence pre-check above should already catch this,
                // but a domain account can still hit this path (e.g. trust/connectivity quirks).
                ERROR_NO_SUCH_USER           => (domainAccount ? VerifyOutcome.Failed : VerifyOutcome.Info, notFound),
                _                            => (VerifyOutcome.Failed, $"Logon check failed: {DescribeLogonError(err)}"),
            };
        }

        // The Check button's text for 1385 — an Info, not an error: the password is right, and the
        // rights the logon still lacks are granted by the install / upgrade-repair itself, AFTER this
        // page. Keep the lists in line with the "Grant ..." / "Add to local groups" steps (the upgrade
        // runs all of them too: UpgradeProgressPage.UpgradeStepNames).
        internal static string LogonTypeNotGrantedMessage(string account, bool upgrade)
        {
            string intro =
                $"Password is correct, but '{account}' is not allowed to log on locally on this computer yet " +
                "(Win32 error 1385, ERROR_LOGON_TYPE_NOT_GRANTED — usually set by a Group Policy).\n\n";
            string grants =
                $"This is OK — you can continue. The {(upgrade ? "upgrade / repair" : "install")} sets these up for the account, " +
                "in the local security policy:\n" +
                "  •  User rights:  Log on as a service,  Allow log on locally,  Create symbolic links,  Debug programs\n" +
                "  •  Local groups:  Performance Log Users,  Performance Monitor Users,  Remote Desktop Users\n";
            string retest =
                "\nThen it tests the logon again. If a 'Deny log on locally' policy applies, it goes on without it (it runs those steps " +
                "as you, for the account) and says at the end which group and policy to ask your AD administrator about.\n\n";
            string domainPush =
                "Note: a domain Group Policy can overwrite these local settings at its next refresh (reboot, gpupdate, " +
                "or within 16 hours). The installer checks for that, and if it will happen it shows a warning at the end " +
                $"of the {(upgrade ? "upgrade / repair" : "install")} with what to ask your AD administrator for.";
            return intro + grants + retest + domainPush;
        }

        // Logs on as the account with the given logon type and closes the token again. Returns 0 on
        // success, else the Win32 error. When sids isn't null, the account's SID and every group SID
        // in the logon token are added to it — the token holds the membership Windows itself uses
        // for user rights: nested domain groups, local groups, and well-known ones (Authenticated Users).
        private static int LogonAs(string account, string password, int logonType, HashSet<string>? sids)
        {
            var (domain, username) = SplitAccount(account);
            if (!LogonUser(username, domain, password, logonType, LOGON32_PROVIDER_DEFAULT, out IntPtr token))
                return Marshal.GetLastWin32Error();
            try
            {
                if (sids != null)
                {
                    using var identity = new WindowsIdentity(token);
                    if (identity.User != null) sids.Add(identity.User.Value);
                    if (identity.Groups != null)
                        foreach (var group in identity.Groups) sids.Add(group.Value);
                }
                return 0;
            }
            finally { CloseHandle(token); }
        }

        // LogonUser's (domain, username): "DOMAIN\user" → ("DOMAIN", "user"), UPN "user@domain" →
        // ("", "user@domain") — the full UPN as username, empty domain —, plain "user" → (".", "user").
        private static (string Domain, string Username) SplitAccount(string account)
        {
            if (account.Contains('\\'))
            {
                var parts = account.Split('\\', 2);
                return (parts[0], parts[1]);
            }
            return account.Contains('@') ? ("", account) : (".", account);
        }

        private static string Win32Message(int err) => new System.ComponentModel.Win32Exception(err).Message;

        // A logon error in words: "<Windows text> (<ERROR_NAME>, Win32 error N). <what it means for the
        // service account, and what to do>". The errors listed are the ones LogonUser / RunAs give for
        // an account that can't be used as configured; any other code gets the Windows text only.
        internal static string DescribeLogonError(int err)
        {
            const string trustHint = "This computer's trust relationship with the domain (its computer account / secure channel) " +
                                     "is broken. Ask the AD administrator; 'Test-ComputerSecureChannel -Repair' (PowerShell, " +
                                     "run with domain admin credentials) usually fixes it.";
            var (name, hint) = err switch
            {
                1317 => ("ERROR_NO_SUCH_USER", "The account doesn't exist — check the spelling of the account name."),
                1326 => ("ERROR_LOGON_FAILURE", "Check the password (Caps Lock, keyboard layout). Note that every failed attempt counts " +
                                                "toward the account's lockout threshold."),
                1327 => ("ERROR_ACCOUNT_RESTRICTION", "A policy restriction blocks this logon — typically an empty password (Windows allows " +
                                                      "blank passwords only at the console). Give the account a password."),
                1328 => ("ERROR_INVALID_LOGON_HOURS", "The account's 'Logon Hours' in Active Directory don't allow a logon right now. " +
                                                      "A service account must be allowed to log on at all hours."),
                1329 => ("ERROR_INVALID_WORKSTATION", "The account's 'Log On To' list in Active Directory (Account tab) doesn't include " +
                                                      $"this computer ({Environment.MachineName}). Ask the AD administrator to add it."),
                1330 => ("ERROR_PASSWORD_EXPIRED", "The password you entered is correct, but too old. " +
                                                   "Set a new password — in Active Directory Users and Computers, or with 'net user <name> *' " +
                                                   "for a local account — and enter the new one here. For a service account, consider " +
                                                   "'Password never expires'; otherwise DbxStarterService stops working every time it expires."),
                1331 => ("ERROR_ACCOUNT_DISABLED", "The account is disabled. Enable it in Active Directory Users and Computers, or with " +
                                                   "'net user <name> /active:yes' for a local account."),
                1311 => ("ERROR_NO_LOGON_SERVERS", "No domain controller could be reached to check the account. Check the network and " +
                                                   "DNS connection to the domain, then try again."),
                1355 => ("ERROR_NO_SUCH_DOMAIN", "The domain part of the account name is unknown, or none of its domain controllers could " +
                                                 "be contacted. Use the NetBIOS domain name (CORP\\svc_dbx) or the UPN (svc_dbx@corp.com)."),
                1385 => ("ERROR_LOGON_TYPE_NOT_GRANTED", "The password is correct, but the account lacks the right for this kind of logon " +
                                                         "('Log on as a service' / 'Allow log on locally'), or a 'Deny log on ...' policy applies."),
                1398 => ("ERROR_TIME_SKEW", "This computer's clock differs too much from the domain controller's (Kerberos allows 5 " +
                                            "minutes). Fix the time synchronization, e.g. 'w32tm /resync'."),
                1787 => ("ERROR_NO_TRUST_SAM_ACCOUNT", trustHint),
                1789 => ("ERROR_TRUSTED_RELATIONSHIP_FAILURE", trustHint),
                1792 => ("ERROR_NETLOGON_NOT_STARTED", "The Netlogon service isn't running on this computer. Start it (services.msc), " +
                                                       "then try again."),
                1793 => ("ERROR_ACCOUNT_EXPIRED", "The account's 'Account expires' date in Active Directory has passed. Ask the AD " +
                                                  "administrator to extend it."),
                1907 => ("ERROR_PASSWORD_MUST_CHANGE", "'User must change password at next logon' is set for the account. Change the " +
                                                       "password once (or ask the AD administrator to clear that flag), then enter the new one here."),
                1909 => ("ERROR_ACCOUNT_LOCKED_OUT", "Too many wrong passwords were tried. Wait until the lockout ends, or ask the AD " +
                                                     "administrator to unlock the account — and check that no other service or scheduled " +
                                                     "task still uses an old password for it."),
                _    => ((string?)null, (string?)null),
            };
            string text = Win32Message(err).TrimEnd(' ', '.');
            return name == null
                ? $"{text} (Win32 error {err})."
                : $"{text} ({name}, Win32 error {err}). {hint}";
        }

        private const int LOGON32_LOGON_INTERACTIVE    = 2;
        private const int LOGON32_LOGON_SERVICE        = 5;
        private const int LOGON32_PROVIDER_DEFAULT     = 0;
        private const int ERROR_LOGON_FAILURE          = 1326;
        private const int ERROR_NO_SUCH_USER           = 1317;
        private const int ERROR_INVALID_WORKSTATION    = 1329;
        private const int ERROR_LOGON_TYPE_NOT_GRANTED = 1385;

        // Logon errors that mean the account can't be used as configured — the install stops on
        // these. Anything else (no domain controller reachable, trust problems, …) may be temporary.
        private static readonly HashSet<int> FatalLogonErrors = new()
        {
            ERROR_LOGON_FAILURE,            // 1326 wrong password
            1327,                           // ERROR_ACCOUNT_RESTRICTION (e.g. blank password not allowed)
            1328,                           // ERROR_INVALID_LOGON_HOURS
            ERROR_INVALID_WORKSTATION,      // 1329 "Log On To" list in AD doesn't include this computer
            1330,                           // ERROR_PASSWORD_EXPIRED
            1331,                           // ERROR_ACCOUNT_DISABLED
            ERROR_LOGON_TYPE_NOT_GRANTED,   // 1385 missing right / Deny policy
            1793,                           // ERROR_ACCOUNT_EXPIRED
            1907,                           // ERROR_PASSWORD_MUST_CHANGE
            1909,                           // ERROR_ACCOUNT_LOCKED_OUT
        };

        [DllImport("advapi32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
        private static extern bool LogonUser(
            string username, string domain, string password,
            int logonType, int logonProvider, out IntPtr token);

        [DllImport("advapi32.dll", SetLastError = true)]
        private static extern bool ImpersonateLoggedOnUser(IntPtr hToken);

        [DllImport("advapi32.dll", SetLastError = true)]
        private static extern bool RevertToSelf();

        [DllImport("kernel32.dll", SetLastError = true)]
        private static extern bool CloseHandle(IntPtr handle);

        // Impersonates the service account for the duration of a using block so that
        // file-system operations (Directory.Create, File.Write, etc.) are performed as
        // that user — giving the service account native ownership of the created files.
        //
        // Returns null when impersonation is impossible (gMSA, logon failure, etc.) so
        // the caller can fall back to running as admin + applying icacls.
        private sealed class UserImpersonation : IDisposable
        {
            private readonly IntPtr _token;
            private UserImpersonation(IntPtr token) { _token = token; }

            public static UserImpersonation? TryCreate(InstallConfig c, InstallLog log)
            {
                if (RunAsAdmin(c))
                {
                    log($"  ({RunAsAdminReason(c)}: impersonation not possible; files owned by admin, icacls will grant access)");
                    return null;
                }

                var (domain, username) = SplitAccount(c.ServiceAccount);
                bool ok = LogonUser(username, domain, c.Password,
                    LOGON32_LOGON_INTERACTIVE, LOGON32_PROVIDER_DEFAULT, out IntPtr token);
                if (!ok)
                {
                    int err = Marshal.GetLastWin32Error();
                    log($"  Warning: LogonUser failed: {DescribeLogonError(err)} — files will be owned by admin, icacls will grant access.");
                    return null;
                }

                if (!ImpersonateLoggedOnUser(token))
                {
                    int err = Marshal.GetLastWin32Error();
                    CloseHandle(token);
                    log($"  Warning: ImpersonateLoggedOnUser failed (Win32 error {err}) — files will be owned by admin, icacls will grant access.");
                    return null;
                }

                log($"  Impersonating '{c.ServiceAccount}' — files will be owned by the service account.");
                return new UserImpersonation(token);
            }

            public void Dispose()
            {
                RevertToSelf();
                CloseHandle(_token);
            }
        }

        // Opens a fresh timestamped log file for the given mode — shared by the Install/Upgrade/
        // Uninstall progress pages so all three end up with the same naming scheme:
        // DbxInstaller_{install|upgrade|remove}_yyyy-MM-dd_HHmmss.log. Falls back to a temp-dir
        // path, then a no-op writer, if the installer's own directory isn't writable.
        internal static (StreamWriter Writer, string Path) OpenLogFile(InstallMode mode)
        {
            string kind = mode switch
            {
                InstallMode.Install => "install",
                InstallMode.Upgrade => "upgrade",
                InstallMode.Remove  => "remove",
                _ => "install",
            };
            string ts = DateTime.Now.ToString("yyyy-MM-dd_HHmmss");
            string name = $"DbxInstaller_{kind}_{ts}.log";
            foreach (var dir in new[] { AppDomain.CurrentDomain.BaseDirectory, Path.GetTempPath() })
            {
                try
                {
                    string path = Path.Combine(dir, name);
                    return (new StreamWriter(path, append: false, System.Text.Encoding.UTF8), path);
                }
                catch { }
            }
            return (StreamWriter.Null, "(log unavailable)");
        }

        internal static (bool ok, string output) Run(string exe, string args)
        {
            try
            {
                var psi = new ProcessStartInfo(exe, args)
                {
                    RedirectStandardOutput = true,
                    RedirectStandardError  = true,
                    UseShellExecute        = false,
                    CreateNoWindow         = true,
                };
                using var p = Process.Start(psi)!;
                string combined = (p.StandardOutput.ReadToEnd() + p.StandardError.ReadToEnd()).Trim();
                p.WaitForExit();
                return (p.ExitCode == 0, combined);
            }
            catch (Exception ex)
            {
                return (false, ex.Message);
            }
        }
    }
}

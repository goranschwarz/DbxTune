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
                "Required so that RunAs, user environment variables and the registry hive work correctly.",
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

        private static async Task<bool> LocatePackage(InstallConfig c, InstallLog log)
        {
            if (!c.DownloadZip)
            {
                if (!File.Exists(c.ZipLocalPath))
                    throw new Exception($"ZIP file not found: {c.ZipLocalPath}");
                c.ResolvedZipPath = c.ZipLocalPath;
                log($"Using local file: {c.ZipLocalPath}");
                log($"  Size: {new FileInfo(c.ZipLocalPath).Length / 1024:N0} KB");
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
            log($"Ensuring Windows profile exists for '{c.ServiceAccount}'…");
            LsaPrivileges.EnsureUserProfile(c.ServiceAccount, out string path);
            log($"Profile path: {path}");

            string expected = AccountHome(c.ServiceAccount);
            if (!path.Equals(expected, StringComparison.OrdinalIgnoreCase))
                throw new Exception(
                    $"Profile was created at '{path}' instead of the expected '{expected}'.\n" +
                    $"This usually means a leftover account or profile directory is using that name.\n" +
                    $"Delete the stale profile ('{path}') and the user account, then re-run the installer.");

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

            log($"Running as '{c.ServiceAccount}': {c.InitCommand}");
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
                }
            }

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

        // Run a process as the service account with its user profile loaded.
        // gMSA accounts cannot be impersonated interactively — falls back to current user.
        private static (bool ok, string output) RunAs(string exe, string args, InstallConfig c, InstallLog log)
        {
            if (IsGmsa(c.ServiceAccount))
            {
                log("  (gMSA account — interactive impersonation not supported, running as current admin user)");
                return Run(exe, args);
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
        internal static Task<bool> StartServiceCore(InstallConfig c, InstallLog log, bool startService, bool launchClient)
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
                    log($"Launching DbxStarterClient as '{c.ServiceAccount}'…");
                    LaunchAsUser(clientExe, "", c);
                    log("  Client launched.");
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
                log($"  DbxStarter web UI is available at:");
                log($"  ➜  http://{host}:{webPort}");
                if (anyNet)
                    log("  (Also reachable from other machines on the network.)");
                log("");
                log("  (The web UI shows live log output and lets you open log files.)");
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
            if (IsGmsa(c.ServiceAccount))
            {
                log("  (gMSA — running under current admin user)");
                using var p = Process.Start(new ProcessStartInfo(exe, args) { UseShellExecute = true })!;
                p.WaitForExit();
                return "";
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
            catch (Exception ex) { log($"    Warning: {ex.Message}"); return ""; }
        }

        // Launch a process as service user without waiting (fire-and-forget).
        private static void LaunchAsUser(string exe, string args, InstallConfig c)
        {
            if (IsGmsa(c.ServiceAccount))
            {
                Process.Start(new ProcessStartInfo(exe, args) { UseShellExecute = true });
                return;
            }
            try
            {
                var psi = BuildUserPsi(exe, args, c);
                psi.UseShellExecute = false;
                Process.Start(psi);
            }
            catch { /* best effort */ }
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

        // Verified: the password is correct. Info: nothing was actually checked, but that's
        // expected/not a problem (gMSA, or an account that doesn't exist yet and will be created
        // during install) — the Service Account page shows this differently from a real failure.
        // Failed: a genuine problem (wrong password for an existing account, or the logon check
        // itself errored).
        public enum VerifyOutcome { Verified, Info, Failed }

        // Attempts an interactive logon with the given account/password to verify the password is
        // correct. Works for local accounts, domain accounts (DOMAIN\user or user@domain), and
        // reports gMSA accounts as unverifiable (AD manages their password — no interactive logon
        // is possible, so this isn't a failure, just "nothing to check").
        public static (VerifyOutcome Outcome, string Message) TryVerifyPassword(string account, string password)
        {
            if (IsGmsa(account))
                return (VerifyOutcome.Info, "gMSA — password is managed by Active Directory; nothing to verify here.");

            // Windows deliberately reports "wrong password" and "no such local account" as the
            // same ERROR_LOGON_FAILURE from LogonUser (anti-enumeration behavior), so a missing
            // local account would otherwise show as "Incorrect password" — check existence
            // ourselves first via LookupAccountName (resolves local and domain accounts alike).
            try { LsaPrivileges.GetSidString(account); }
            catch
            {
                return (VerifyOutcome.Info,
                    $"Account '{account}' does not exist yet — it will be created during install, so there's nothing to verify against.");
            }

            string domain, username;
            if (account.Contains('\\'))
            {
                var parts = account.Split('\\', 2);
                domain   = parts[0];
                username = parts[1];
            }
            else if (account.Contains('@'))
            {
                // UPN form — pass full UPN as username, empty domain
                domain   = "";
                username = account;
            }
            else
            {
                domain   = ".";          // local machine
                username = account;
            }

            bool ok = LogonUser(username, domain, password,
                LOGON32_LOGON_INTERACTIVE, LOGON32_PROVIDER_DEFAULT, out IntPtr token);
            if (ok)
            {
                CloseHandle(token);
                return (VerifyOutcome.Verified, "Password verified — OK.");
            }

            int err = Marshal.GetLastWin32Error();
            return err switch
            {
                ERROR_LOGON_FAILURE => (VerifyOutcome.Failed, $"Incorrect password for '{account}'."),
                // Belt-and-suspenders: the existence pre-check above should already catch this,
                // but a domain account can still hit this path (e.g. trust/connectivity quirks).
                ERROR_NO_SUCH_USER  => (VerifyOutcome.Info, $"Account '{account}' does not exist yet — it will be created during install, so there's nothing to verify against."),
                _                   => (VerifyOutcome.Failed, $"Logon check failed (Win32 error {err})."),
            };
        }

        private const int LOGON32_LOGON_INTERACTIVE = 2;
        private const int LOGON32_PROVIDER_DEFAULT  = 0;
        private const int ERROR_LOGON_FAILURE       = 1326;
        private const int ERROR_NO_SUCH_USER        = 1317;

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
                if (IsGmsa(c.ServiceAccount))
                {
                    log("  (gMSA account — impersonation not possible; files owned by admin, icacls will grant access)");
                    return null;
                }

                string domain, username;
                if (c.ServiceAccount.Contains('\\'))
                {
                    var parts = c.ServiceAccount.Split('\\', 2);
                    domain   = parts[0];
                    username = parts[1];
                }
                else if (c.ServiceAccount.Contains('@'))
                {
                    domain   = "";
                    username = c.ServiceAccount;
                }
                else
                {
                    domain   = ".";
                    username = c.ServiceAccount;
                }

                bool ok = LogonUser(username, domain, c.Password,
                    LOGON32_LOGON_INTERACTIVE, LOGON32_PROVIDER_DEFAULT, out IntPtr token);
                if (!ok)
                {
                    int err = Marshal.GetLastWin32Error();
                    log($"  Warning: LogonUser failed (Win32 error {err}) — files will be owned by admin, icacls will grant access.");
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

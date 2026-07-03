using System;
using System.Collections.Generic;
using System.Data;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Hosting;
using Serilog;
using Serilog.Events;
using Serilog.Core;
using System.Net.Http;
#if WINDOWS
using System.Management;
#endif
using System.Reflection;
using System.Runtime.InteropServices;

namespace DbxStarterService
{
    public class DbxStarterService : BackgroundService
    {
        //        private ProcessInfo       _dbxCentralProcess;
        //        private List<ProcessInfo> _runningProcesses = new List<ProcessInfo>();
        private List<SrvEntry> _runningSrvList = new List<SrvEntry>();
        private string _srvInfoFilePath;
        private const string DBX_CENTRAL = "DBX_CENTRAL";
        private Dictionary<string, string> _dbxEnvVariables = new Dictionary<string, string>();

        private Dictionary<string, string> _srvConsoleLogName = new Dictionary<string, string>();

        // Named Pipe Communication Server
        private ServiceCommunication _communicationInterface;

        private static LoggingLevelSwitch _logLevel = new LoggingLevelSwitch();

        private static readonly HttpClient _httpClient = new HttpClient();

        private static readonly Regex _dbxTuneRegex = new Regex(@"com\.dbxtune\..*Tune ", RegexOptions.Compiled);

        private bool _firstTimePrint_dbxCentralScriptName = true;
        private bool _addDbxCentral;


        //// For sending Ctrl+C signal
        //[DllImport("kernel32.dll", SetLastError = true)]
        //static extern bool GenerateConsoleCtrlEvent(uint dwCtrlEvent, uint dwProcessGroupId);

        //[DllImport("kernel32.dll", SetLastError = true)]
        //static extern bool AttachConsole(uint dwProcessId);

        //[DllImport("kernel32.dll", SetLastError = true)]
        //static extern bool SetConsoleCtrlHandler(ConsoleCtrlDelegate HandlerRoutine, bool Add);

        //[DllImport("kernel32.dll", SetLastError = true)]
        //static extern bool FreeConsole();

        //// Control signal constants
        //private const uint CTRL_C_EVENT = 0;

        //// Delegate for SetConsoleCtrlHandler
        //delegate bool ConsoleCtrlDelegate(uint CtrlType);




        //// Add this to your class
        //[DllImport("kernel32.dll", SetLastError = true)]
        //static extern IntPtr CreateJobObject(IntPtr lpJobAttributes, string lpName);

        //[DllImport("kernel32.dll", SetLastError = true)]
        //static extern bool AssignProcessToJobObject(IntPtr hJob, IntPtr hProcess);

        //[DllImport("kernel32.dll", SetLastError = true)]
        //static extern bool TerminateJobObject(IntPtr hJob, uint uExitCode);

        ////        private IntPtr _jobHandle;

        public static string GetServiceLogFileName()
        {
            string DBXTUNE_CENTRAL_LOG_DIR = Environment.GetEnvironmentVariable("DBXTUNE_CENTRAL_LOG_DIR");
            string DBXTUNE_LOG_DIR = Environment.GetEnvironmentVariable("DBXTUNE_LOG_DIR");
            string filename = "DbxStarterService.log";

            string logDir = DBXTUNE_CENTRAL_LOG_DIR;
            if (!string.IsNullOrEmpty(logDir))
            {
                return Path.Combine(logDir, filename);
            }

            logDir = DBXTUNE_LOG_DIR;
            if (!string.IsNullOrEmpty(logDir))
            {
                return Path.Combine(logDir, filename);
            }

            string HOME_DIR = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
            logDir = Path.Combine(HOME_DIR, ".dbxtune/dbxc/log");
            return Path.Combine(logDir, filename);
        }

        public static void SetupLogging()
        {
            string logFilename = GetServiceLogFileName();
            if (File.Exists(logFilename))
            {
                try
                {
                    File.Delete(logFilename);
                    Debug.WriteLine("Deleted existing log file: " + logFilename);
                    //Log.Information("Deleted existing log file: " + logFilename);
                }
                catch (Exception ex)
                {
                    Debug.WriteLine("Error deleting log file: " + logFilename + " " + ex.Message);
                    //Log.Warning(ex, "Error deleting log file: " + logFilename);

                    // try to empty the file
                    try
                    {
                        File.WriteAllText(logFilename, "");
                        Debug.WriteLine("Emptied log file: " + logFilename);
                    }
                    catch (Exception ex2)
                    {
                        Debug.WriteLine("Error emptying log file: " + logFilename + " " + ex2.Message);
                    }
                }
            }

            // Default log level
            _logLevel.MinimumLevel = LogEventLevel.Information;

            Log.Logger = new LoggerConfiguration()
                //.MinimumLevel.Debug()
                .MinimumLevel.ControlledBy(_logLevel)
                .WriteTo.Console()
                .WriteTo.File(
                    path: logFilename,                         // Fixed file name
                    fileSizeLimitBytes: 10 * 1024 * 1024,         // 10 MB
                    rollOnFileSizeLimit: true,                    // Enable size-based rolling
                    retainedFileCountLimit: 3,                    // Keep only 3 files
                    rollingInterval: RollingInterval.Infinite,    // Size-only rolling
                    outputTemplate: "{Timestamp:yyyy-MM-dd HH:mm:ss.fff} - [{Level:u5}] - {Message:lj}{NewLine}{Exception}"
                )
                .CreateLogger();

            Log.Information("Service bootstrap starting...");
        }


        public static string GetHomeDir()
        {
            string HOME_DIR = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
            return HOME_DIR;
        }
        public static string GetDbxTune_BaseDir()
        {
            string env = Environment.GetEnvironmentVariable("DBXTUNE_CENTRAL_BASE");
            if (string.IsNullOrEmpty(env))
            {
                env = Environment.GetEnvironmentVariable("DBXTUNE_BASE");
            }
            if (string.IsNullOrEmpty(env))
            {
                env = Path.Combine(GetHomeDir(), ".dbxtune");

            }
            return env;

            //DBXTUNE_CENTRAL_BASE = C:\Users\dbxtune\.dbxtune\dbxc
            //DBXTUNE_CENTRAL_CONF_DIR = C:\Users\dbxtune\.dbxtune\dbxc\conf
            //DBXTUNE_CENTRAL_INFO_DIR = C:\Users\dbxtune\.dbxtune\dbxc\info
            //DBXTUNE_CENTRAL_LOG_DIR = C:\Users\dbxtune\.dbxtune\dbxc\log
            //DBXTUNE_CENTRAL_REPORTS_DIR = C:\Users\dbxtune\.dbxtune\dbxc\reports
            //DBXTUNE_CENTRAL_SAVE_DIR = C:\Users\dbxtune\.dbxtune\dbxc\data

        }
        public static string GetDbxTune_ConfDir()
        {
            string env = Environment.GetEnvironmentVariable("DBXTUNE_CENTRAL_CONF_DIR");
            if (string.IsNullOrEmpty(env))
            {
                env = Environment.GetEnvironmentVariable("DBXTUNE_CONF_DIR");
            }
            if (string.IsNullOrEmpty(env))
            {
                env = Path.Combine(GetHomeDir(), ".dbxtune", "dbxc", "conf");

            }
            return env;
        }
        public static string GetDbxTune_InfoDir()
        {
            string env = Environment.GetEnvironmentVariable("DBXTUNE_CENTRAL_INFO_DIR");
            if (string.IsNullOrEmpty(env))
            {
                env = Environment.GetEnvironmentVariable("DBXTUNE_INFO_DIR");
            }
            if (string.IsNullOrEmpty(env))
            {
                env = Path.Combine(GetHomeDir(), ".dbxtune", "dbxc", "info");

            }
            return env;
        }
        public static string GetDbxTune_LogDir()
        {
            string env = Environment.GetEnvironmentVariable("DBXTUNE_CENTRAL_LOG_DIR");
            if (string.IsNullOrEmpty(env))
            {
                env = Environment.GetEnvironmentVariable("DBXTUNE_LOG_DIR");
            }
            if (string.IsNullOrEmpty(env))
            {
                env = Path.Combine(GetHomeDir(), ".dbxtune", "dbxc", "log");

            }
            return env;
        }
        public static string GetDbxTune_ReportsDir()
        {
            string env = Environment.GetEnvironmentVariable("DBXTUNE_CENTRAL_REPORTS_DIR");
            if (string.IsNullOrEmpty(env))
            {
                env = Environment.GetEnvironmentVariable("DBXTUNE_REPORTS_DIR");
            }
            if (string.IsNullOrEmpty(env))
            {
                env = Path.Combine(GetHomeDir(), ".dbxtune", "dbxc", "reports");

            }
            return env;
        }
        public static string GetDbxTune_SaveDir()
        {
            string env = Environment.GetEnvironmentVariable("DBXTUNE_CENTRAL_SAVE_DIR");
            if (string.IsNullOrEmpty(env))
            {
                env = Environment.GetEnvironmentVariable("DBXTUNE_SAVE_DIR");
            }
            if (string.IsNullOrEmpty(env))
            {
                env = Path.Combine(GetHomeDir(), ".dbxtune", "dbxc", "data");

            }
            return env;
        }
        public static string GetDbxTune_DataDir()
        {
            return GetDbxTune_SaveDir();
        }

        private static void SetEnvIfMissing(string key, string value)
        {
            if (string.IsNullOrEmpty(Environment.GetEnvironmentVariable(key)))
                Environment.SetEnvironmentVariable(key, value);
        }


        public DbxStarterService(IConfiguration configuration)
        {
            _addDbxCentral = configuration.GetValue<bool>("DbxStarter:ManageDbxCentral", true);
            Log.Information("ManageDbxCentral = {ManageDbxCentral}", _addDbxCentral);

            //InitializeComponent();

            // Set paths relative to the executable location
            //            string baseDir = AppDomain.CurrentDomain.BaseDirectory;
            //string baseDir = "C:\\Users\\goran\\source\\repos\\DbxTune\\DbxTuneStarter";

            //string logDir = Path.Combine(baseDir, "logs");

            //// Ensure log directory exists
            //if (!Directory.Exists(logDir))
            //    Directory.CreateDirectory(logDir);

            string envFile = Path.Combine(GetHomeDir(), ".dbxtune/DBXTUNE.env.bat");
            if (!File.Exists(envFile))
            {
                Log.Warning("Environment file not found at: " + envFile);
            }
            else
            {
                var env = EnvImporter.ImportEnvFromBat(envFile);
                Log.Information("From Environment file '{envFile}' read the following environment variables.", envFile);
                foreach (var kv in env)
                {
                    //psi.EnvironmentVariables[kv.Key] = kv.Value;

                    if (kv.Key.StartsWith("DBXTUNE_"))
                    {
                        Log.Information("Importing Env >>> {kv.Key} = {kv.Value}", kv.Key, kv.Value);
                        Environment.SetEnvironmentVariable(kv.Key, kv.Value);

                        _dbxEnvVariables[kv.Key] = kv.Value;
                    }
                }
            }

            // Ensure key DBXTUNE_* path variables are set so %VAR% expansion works in
            // script paths even when the bat file doesn't define every variable explicitly.
            SetEnvIfMissing("DBXTUNE_CENTRAL_BASE", GetDbxTune_BaseDir());
            SetEnvIfMissing("DBXTUNE_BASE", GetDbxTune_BaseDir());
            SetEnvIfMissing("DBXTUNE_CENTRAL_CONF_DIR", GetDbxTune_ConfDir());
            SetEnvIfMissing("DBXTUNE_CONF_DIR", GetDbxTune_ConfDir());
            SetEnvIfMissing("DBXTUNE_CENTRAL_LOG_DIR", GetDbxTune_LogDir());
            SetEnvIfMissing("DBXTUNE_LOG_DIR", GetDbxTune_LogDir());
            SetEnvIfMissing("DBXTUNE_CENTRAL_INFO_DIR", GetDbxTune_InfoDir());
            SetEnvIfMissing("DBXTUNE_INFO_DIR", GetDbxTune_InfoDir());

            // Setup logging
            SetupLogging();

            //            _serverListPath = Path.Combine(baseDir, "SERVER_LIST");
            //            _srvInfoFilePath = "C:\\Users\\goran\\.dbxtune\\dbxc\\conf\\SERVER_LIST";
            //string DBXTUNE_CONF_DIR = Environment.GetEnvironmentVariable("DBXTUNE_CONF_DIR");
            //string confDir = DBXTUNE_CONF_DIR;
            //if (string.IsNullOrEmpty(confDir))
            //{
            //    //string HOME_DIR = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
            //    confDir = Path.Combine(HOME_DIR, ".dbxtune/dbxc/conf");

            //    Log.Information("DBXTUNE_CONF_DIR environment variable is not set, using fallback ($HOME/.dbxtune/dbxc/conf): " + confDir);
            //}
            _srvInfoFilePath = Path.Combine(GetDbxTune_ConfDir(), "SERVER_LIST");

            // Set up Serilog
            //Log.Logger = new LoggerConfiguration()
            //    .MinimumLevel.Debug()
            //    .WriteTo.File(Path.Combine(logDir, "DbxStarterService-.log"),
            //        rollingInterval: RollingInterval.Day,
            //        retainedFileCountLimit: 31,
            //        outputTemplate: "{Timestamp:yyyy-MM-dd HH:mm:ss.fff} [{Level:u3}] {Message:lj}{NewLine}{Exception}")
            //    .WriteTo.EventLog("DbxStarterService", restrictedToMinimumLevel: LogEventLevel.Information)
            //    .CreateLogger();

            Log.Information("Service initializing...");

            // Create a dummy handler to prevent the process from terminating
            //SetConsoleCtrlHandler(null, true);

            // Initialize communication interface
            _communicationInterface = new ServiceCommunication(this);

        }


        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            string currentUser = Environment.UserName;
#pragma warning disable CA1416
#if WINDOWS
            string currentPrincipalUser = System.Security.Principal.WindowsIdentity.GetCurrent().Name;
#else
            string currentPrincipalUser = "(non-Windows)";
#endif
#pragma warning restore CA1416
            string homeDir = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
            Log.Information("Service starting... as user='{currentUser}', fullName='{currentUser2}', homeDir='{homeDir}'.", currentUser, currentPrincipalUser, homeDir);

            try
            {
                // Check if SERVER_LIST exists
                if (!File.Exists(_srvInfoFilePath))
                {
                    Log.Error("SERVER_LIST file not found at {Path}", _srvInfoFilePath);
                    throw new InvalidOperationException("SERVER_LIST file not found at: " + _srvInfoFilePath);
                }

                // Read and process SERVER_LIST
                List<SrvInfoFileEntry> srvInfoFileEntries = ReadServerList(_srvInfoFilePath);
                Log.Information("Found {srvInfoFileEntries.Count} server entries in SERVER_LIST", srvInfoFileEntries.Count);

                // Get already started processes
                List<DbxProcessInfo> psList = GetRunningServerProcesses();

                // Start enabled processes
                int startCount = 0;
                int skipCount = 0;
                foreach (var entry in srvInfoFileEntries.Where(e => e.IsEnabled))
                {
                    // Check if entry is already running
                    var psListEntry = psList.FirstOrDefault(p => p.ServerOrAliasName.Equals(entry.ServerOrAliasName, StringComparison.OrdinalIgnoreCase));
                    if (psListEntry != null)
                    {
                        Log.Information("Process for server '{entry.ServerOrAliasName}' is already running, at PID={psListEntry.Pid}. Skipping start.", entry.ServerOrAliasName, psListEntry.Pid);
                        skipCount++;
                        continue;
                    }

                    try
                    {
                        await StartServerProcess(entry);
                        startCount++;
                    }
                    catch (Exception ex)
                    {
                        Log.Error(ex, $"Error starting process for Server '{entry.ServerOrAliasName}'.");
                    }
                }

                Log.Information("Service started successfully. {startCount} processes Started. {skipCount} skipped (was already started).", startCount, skipCount);
            }
            catch (Exception ex)
            {
                Log.Error(ex, "Error during service startup");
            }

            // Start communication interface
            _communicationInterface.Start();

            // Keep the service alive until the host requests shutdown
            await Task.Delay(Timeout.Infinite, stoppingToken).ConfigureAwait(false);
        }

        public override async Task StopAsync(CancellationToken cancellationToken)
        {
            Log.Information("Service stopping...");

            try
            {
                var psList = GetRunningServerProcesses();
                await StopServerProcesses(psList);
            }
            catch (Exception ex)
            {
                Log.Error(ex, "Error during service shutdown");
            }
            finally
            {
                _communicationInterface?.Stop();
                Log.Information("Service stopped.");
                Log.CloseAndFlush();
            }

            await base.StopAsync(cancellationToken).ConfigureAwait(false);
        }


        private DbxProcessInfo GetRunningServerProcess(string srvName)
        {
            var psList = GetRunningServerProcesses();
            return psList.FirstOrDefault(p => p.ServerName.Equals(srvName, StringComparison.OrdinalIgnoreCase));
        }


        private List<DbxProcessInfo> GetRunningServerProcesses()
        {
            List<DbxProcessInfo> dbxCmdLineEntries = new List<DbxProcessInfo>();

            string currentUser = Environment.UserName;
            string domainName = Environment.UserDomainName;

            Log.Information("Checking/Getting for Server/Java processes started by '{domainName}\\{currentUser}'.", domainName, currentUser);

            foreach (Process process in Process.GetProcessesByName("java"))
            {
                using (process)
                {
                    uint pid = (uint)process.Id;
                    string startTime = "";
                    try { startTime = process.StartTime.ToString("yyyy-MM-dd HH:mm:ss"); } catch { }

                    string commandLine = GetProcessCommandLine(process.Id);

                    Log.Debug("Found java process PID={Pid}, cmdLine={Cmd}",
                        pid,
                        commandLine.Length > 120 ? commandLine[..120] + "…" : commandLine);

                    if (string.IsNullOrEmpty(commandLine))
                    {
                        Log.Debug("Skipping java PID={Pid}: command line is empty", pid);
                        continue;
                    }
                    bool isDbxCentral = commandLine.Contains("com.dbxtune.central.DbxTuneCentral");
                    bool isDbxTune = _dbxTuneRegex.IsMatch(commandLine);
                    if (isDbxCentral || isDbxTune)
                    {
                        DbxProcessInfo dbxCmdLineEntry = ParseDbxJavaCmdLine(pid, 0, startTime, commandLine, isDbxCentral, isDbxTune);
                        Log.Information("Matched DbxTune process PID={Pid}, server={Server}", pid, dbxCmdLineEntry.ServerName);
                        dbxCmdLineEntries.Add(dbxCmdLineEntry);
                    }
                }
            }

            return dbxCmdLineEntries;
        }

        private static string GetProcessCommandLine(int pid)
        {
#pragma warning disable CA1416
#if WINDOWS
            // Try WMI first (works when the service account has root\cimv2 read access).
            try
            {
                var searcher = new ManagementObjectSearcher(
                    $"SELECT CommandLine FROM Win32_Process WHERE ProcessId = {pid}");
                foreach (ManagementObject obj in searcher.Get())
                {
                    var cmd = obj["CommandLine"]?.ToString();
                    if (!string.IsNullOrEmpty(cmd)) return cmd;
                }
            }
            catch (Exception ex)
            {
                Log.Debug(ex, "WMI CommandLine query failed for PID {Pid} — trying NtQuery fallback", pid);
            }

            // Fallback: NtQueryInformationProcess requires only PROCESS_QUERY_LIMITED_INFORMATION
            // (0x1000), which is always available to the process owner — no WMI ACL or admin
            // rights needed.  Works for local accounts, AD accounts, and GMSA accounts.
            var ntResult = GetCommandLineViaNtQuery(pid);
            if (string.IsNullOrEmpty(ntResult))
                Log.Debug("NtQuery CommandLine also empty for PID {Pid}", pid);
            return ntResult;
#else
            try
            {
                string cmdlineFile = $"/proc/{pid}/cmdline";
                if (File.Exists(cmdlineFile))
                    return File.ReadAllText(cmdlineFile).Replace('\0', ' ').Trim();
            }
            catch { }
            return "";
#endif
#pragma warning restore CA1416
        }

#if WINDOWS
        [DllImport("kernel32.dll", SetLastError = true)]
        private static extern IntPtr OpenProcess(uint dwAccess, bool bInherit, uint dwPid);

        [DllImport("kernel32.dll", SetLastError = true)]
        private static extern bool CloseHandle(IntPtr hObject);

        [DllImport("ntdll.dll")]
        private static extern int NtQueryInformationProcess(
            IntPtr hProcess, int infoClass,
            IntPtr pInfo, int infoLen, out int returnLen);

        private static string GetCommandLineViaNtQuery(int pid)
        {
            const uint PROCESS_QUERY_LIMITED = 0x1000;
            const int  ProcessCmdLine        = 60;   // ProcessCommandLineInformation

            IntPtr hProc = OpenProcess(PROCESS_QUERY_LIMITED, false, (uint)pid);
            if (hProc == IntPtr.Zero) return "";
            try
            {
                // First call: get required buffer size
                NtQueryInformationProcess(hProc, ProcessCmdLine, IntPtr.Zero, 0, out int len);
                if (len == 0) return "";

                IntPtr buf = Marshal.AllocHGlobal(len);
                try
                {
                    if (NtQueryInformationProcess(hProc, ProcessCmdLine, buf, len, out _) != 0)
                        return "";

                    // The returned structure is UNICODE_STRING:
                    //   ushort Length       (+0)
                    //   ushort MaxLength    (+2)
                    //   [4 bytes padding on x64]
                    //   IntPtr Buffer       (+8 on x64)
                    int    strLen = Marshal.ReadInt16(buf, 0);   // byte length of string
                    IntPtr strPtr = Marshal.ReadIntPtr(buf, 8);  // pointer to char data
                    return Marshal.PtrToStringUni(strPtr, strLen / 2);
                }
                finally { Marshal.FreeHGlobal(buf); }
            }
            finally { CloseHandle(hProc); }
        }
#endif

        private DbxProcessInfo ParseDbxJavaCmdLine(uint pid, uint parentPid, string startTime, string commandLine, bool isDbxCentral, bool isDbxTune)
        {

            var dict = ParseCommandLine(commandLine);

            var pi = new DbxProcessInfo();
            pi.Pid = pid;
            pi.ParentPid = parentPid;
            pi.AliasName = "";
            pi.StartTime = startTime;
            pi.CommandLine = commandLine;

            string tmpStr;
            if (isDbxCentral)
            {
                pi.ServerName = DBX_CENTRAL;
                pi.LogFile = dict.TryGetValue("logName", out tmpStr) ? tmpStr : "";
                pi.ConfigFile = dict.TryGetValue("config", out tmpStr) ? tmpStr : "";
                pi.DbmsUsername = dict.TryGetValue("dbmsUsername", out tmpStr) ? tmpStr : "";
                pi.ServerType = "DbxTuneCentral";

                return pi;
            }
            if (isDbxTune)
            {
                pi.ServerName = dict.GetValueOrDefault("server", "");
                pi.AliasName = dict.GetValueOrDefault("serverAlias", "");
                pi.DisplayName = dict.GetValueOrDefault("displayName", "");
                // -L / --logName provides the full log file path, not a directory
                pi.LogFile = dict.GetValueOrDefault("logName", "");
                // Collectors pass the config file via -n / --noGui, not -C / --config
                pi.ConfigFile = dict.GetValueOrDefault("noGui", "");
                pi.DbmsUsername = dict.GetValueOrDefault("dbmsUsername", "");
                pi.SaveDir = dict.GetValueOrDefault("saveDir", "");

                Match match = Regex.Match(commandLine, "com\\.dbxtune\\..*Tune ");
                if (match.Success)
                {
                    pi.ServerType = match.Value.Trim().Replace("com.dbxtune.", "");
                }

                return pi;
            }
            return null;
        }

        static Dictionary<string, string> ParseCommandLine(string input)
        {
            var result = new Dictionary<string, string>();
            var tokens = new Queue<string>(input.Split(new[] { ' ' }, StringSplitOptions.RemoveEmptyEntries));

            while (tokens.Count > 0)
            {
                var token = tokens.Dequeue();

                if (token.StartsWith("--"))
                {
                    if (token == "--server" && tokens.Count > 0)
                    {
                        result["server"] = tokens.Dequeue();
                    }
                    else if (token == "--serverAlias" && tokens.Count > 0)
                    {
                        result["serverAlias"] = tokens.Dequeue();
                    }
                    else if (token == "--displayName" && tokens.Count > 0)
                    {
                        result["displayName"] = tokens.Dequeue();
                    }
                    else if (token == "--config" && tokens.Count > 0)
                    {
                        result["config"] = tokens.Dequeue();
                    }
                    else if (token == "--noGui" && tokens.Count > 0)
                    {
                        result["noGui"] = tokens.Dequeue();
                    }
                    else if (token == "--logName" && tokens.Count > 0)
                    {
                        result["logName"] = tokens.Dequeue();
                    }
                    else if (token == "--user" && tokens.Count > 0)
                    {
                        result["dbmsUsername"] = tokens.Dequeue();
                    }
                    else if (token == "--saveDir" && tokens.Count > 0)
                    {
                        result["saveDir"] = tokens.Dequeue();
                    }
                }
                else if (token.StartsWith("-"))
                {
                    if (token.StartsWith("-S"))
                    {
                        var val = token.Length > 2 ? token.Substring(2) : (tokens.Count > 0 ? tokens.Dequeue() : null);
                        if (val != null) result["server"] = val;
                    }
                    else if (token.StartsWith("-A"))
                    {
                        var val = token.Length > 2 ? token.Substring(2) : (tokens.Count > 0 ? tokens.Dequeue() : null);
                        if (val != null) result["serverAlias"] = val;
                    }
                    else if (token.StartsWith("-N"))
                    {
                        var val = token.Length > 2 ? token.Substring(2) : (tokens.Count > 0 ? tokens.Dequeue() : null);
                        if (val != null) result["displayName"] = val;
                    }
                    else if (token.StartsWith("-C"))
                    {
                        var val = token.Length > 2 ? token.Substring(2) : (tokens.Count > 0 ? tokens.Dequeue() : null);
                        if (val != null) result["config"] = val;
                    }
                    else if (token.StartsWith("-n"))
                    {
                        var val = token.Length > 2 ? token.Substring(2) : (tokens.Count > 0 ? tokens.Dequeue() : null);
                        if (val != null) result["noGui"] = val;
                    }
                    else if (token.StartsWith("-L"))
                    {
                        var val = token.Length > 2 ? token.Substring(2) : (tokens.Count > 0 ? tokens.Dequeue() : null);
                        if (val != null) result["logName"] = val;
                    }
                    else if (token.StartsWith("-U"))
                    {
                        var val = token.Length > 2 ? token.Substring(2) : (tokens.Count > 0 ? tokens.Dequeue() : null);
                        if (val != null) result["dbmsUsername"] = val;
                    }
                    else if (token.StartsWith("-R"))
                    {
                        var val = token.Length > 2 ? token.Substring(2) : (tokens.Count > 0 ? tokens.Dequeue() : null);
                        if (val != null) result["saveDir"] = val;
                    }
                }
            }

            return result;
        }


        protected bool IsPidRunning(uint pid)
        {
            try
            {
                // Check if the process with the given PID is running
                using (var process = Process.GetProcessById((int)pid))
                {
                    return !process.HasExited;
                }
            }
            catch (ArgumentException)
            {
                // Process with the given PID does not exist
                return false;
            }
            catch (Exception ex)
            {
                Log.Error(ex, $"Error checking if process with PID {pid} is running");
                return false;
            }
        }

        public List<SrvEntry> GetRunningProcesses()
        {
            // Get running proceses from the OS
            var psList = GetRunningServerProcesses();

            // Read what we should maintain: start/stop
            var srvInfoList = ReadServerList(_srvInfoFilePath);

            // Combine "server info" with "running processes"
            var returnList = new List<SrvEntry>();

            foreach (var srvInfoEntry in srvInfoList)
            {
                var psEntry = psList.FirstOrDefault(p => p.ServerOrAliasName.Equals(srvInfoEntry.ServerOrAliasName, StringComparison.OrdinalIgnoreCase));

                var addEntry = new SrvEntry();

                var srvName = srvInfoEntry.ServerOrAliasName;

                addEntry.ServerName = srvName;
                addEntry.ConsoleFile = _srvConsoleLogName.TryGetValue(srvName, out string consoleFile) ? consoleFile : "";
                addEntry.StartScript = srvInfoEntry.StartScript;
                addEntry.Info = srvInfoEntry.Description;

                if (psEntry != null)
                {
                    // Add additional info from the Process
                    addEntry.Pid = psEntry.Pid;
                    addEntry.isRunning = IsPidRunning(psEntry.Pid);
                    addEntry.StartTime = psEntry.StartTime;
                    addEntry.DbxProcessInfo = psEntry;
                    // Prefer config path from the running process command line; fall back to
                    // what was parsed from the SERVER_LIST start script (covers collectors that
                    // don't pass -C explicitly to java).
                    addEntry.ConfigFile = !string.IsNullOrEmpty(psEntry.ConfigFile)
                        ? psEntry.ConfigFile
                        : srvInfoEntry.ConfigFile;
                }
                else
                {
                    Log.Warning("No running process found for server '{ServerOrAliasName}'", srvInfoEntry.ServerOrAliasName);

                    // Add additional info from the Process
                    addEntry.Info = "";
                    if (!srvInfoEntry.IsEnabled)
                        addEntry.Info = "Entry is DISABLED in SERVER_LIST";

                    addEntry.isRunning = false;
                    addEntry.ConfigFile = srvInfoEntry.ConfigFile;
                }
                returnList.Add(addEntry);
            }

            // Add Process (from psList) that is NOT in srvInfoList
            foreach (DbxProcessInfo psEntry in psList)
            {
                if (!returnList.Any(e => e.ServerName.Equals(psEntry.ServerOrAliasName, StringComparison.OrdinalIgnoreCase)))
                {
                    var addEntry = new SrvEntry
                    {
                        ServerName = psEntry.ServerName,
                        Pid = psEntry.Pid,
                        isRunning = IsPidRunning(psEntry.Pid),
                        StartTime = psEntry.StartTime,
                        DbxProcessInfo = psEntry,
                        Info = "Process is NOT in SERVER_LIST"
                    };
                    returnList.Add(addEntry);
                }
            }


            return returnList.ToList();
        }


        private string GetLastLine(string filePath)
        {
            try
            {
                using (var fileStream = new FileStream(filePath, FileMode.Open, FileAccess.Read, FileShare.ReadWrite))
                using (var streamReader = new StreamReader(fileStream))
                {
                    string lastLine = string.Empty;
                    string currentLine;

                    while ((currentLine = streamReader.ReadLine()) != null)
                    {
                        // Skip empty lines
                        if (string.IsNullOrEmpty(currentLine))
                            continue;

                        lastLine = currentLine;
                    }

                    return lastLine;
                }
            }
            catch (Exception ex)
            {
                string msg = $"Error reading last line from file '{filePath}'. Exception: {ex.Message}";
                Log.Error(ex, msg);
                return msg;
            }
        }


        public bool ReloadServerList()
        {
            Log.Information("Reloading SERVER_LIST requested");
            try
            {
                var serverEntries = ReadServerList(_srvInfoFilePath);
                Log.Information("Found {serverEntries.Count} server entries in SERVER_LIST", serverEntries.Count);

                // Find entries that need to be stopped (disabled or removed)
                //var runningServerNames = _runningProcesses.Select(p => p.ServerName).ToList();
                //var enabledServerNames = serverEntries.Where(e => e.IsEnabled).Select(e => e.ServerName).ToList();

                //foreach (var entry in serverEntries.Where(e => e.IsEnabled))
                //{
                //    try
                //    {
                //        PreCheckServerProcess(entry);
                //    }
                //    catch (Exception ex)
                //    {
                //        Log.Error(ex, "Error checking for ALREADY STARTED process for '" + entry.ServerName + "'.");
                //    }
                //}
                GetRunningServerProcesses();

                //// Stop processes that are no longer in the list or disabled
                //foreach (var serverName in runningServerNames.Except(enabledServerNames))
                //{
                //    StopServer(serverName);
                //}

                //// Start new processes that weren't running before
                //foreach (var entry in serverEntries.Where(e => e.IsEnabled && !runningServerNames.Contains(e.ServerName)))
                //{
                //    StartServerProcess(entry);
                //}

                return true;
            }
            catch (Exception ex)
            {
                Log.Error(ex, "Error reloading SERVER_LIST");
                return false;
            }
        }

        public async Task<bool> RestartServer(string serverName)
        {
            Log.Information("Restart requested for server '{serverName}'.", serverName);

            try
            {
                // First stop the server
                if (!await StopServer(serverName))
                    return false;

                // Then read the server list to get the entry
                var serverEntries = ReadServerList(_srvInfoFilePath);
                var entry = serverEntries.FirstOrDefault(e => e.ServerOrAliasName.Equals(serverName, StringComparison.OrdinalIgnoreCase));

                if (entry == null)
                {
                    Log.Warning("Server '{serverName}' not found in SERVER_LIST", serverName);
                    return false;
                }

                if (!entry.IsEnabled)
                {
                    Log.Warning("Server '{serverName}' is disabled in SERVER_LIST", serverName);
                    return false;
                }

                // Start the server
                await StartServerProcess(entry);
                return true;
            }
            catch (Exception ex)
            {
                Log.Error(ex, "Error restarting server '{serverName}'.", serverName);
                return false;
            }
        }

        public async Task<bool> StopServer(string serverName)
        {
            Log.Information("Stop requested for server '{serverName}'.", serverName);

            try
            {
                var psEntry = GetRunningServerProcess(serverName);
                if (psEntry == null)
                {
                    Log.Warning("Server '{serverName}' not found in running processes.", serverName);
                    return false;
                }

                await StopServerProcess(psEntry);

                return true;
            }
            catch (Exception ex)
            {
                Log.Error(ex, "Error stopping server '{serverName}'", serverName);
                return false;
            }
        }

        public async Task<bool> StartServer(string serverName)
        {
            Log.Information("Start requested for server '{serverName}'.", serverName);

            try
            {
                // Check if it's in the Server List
                var serverEntries = ReadServerList(_srvInfoFilePath);
                var entry = serverEntries.FirstOrDefault(e => e.ServerOrAliasName.Equals(serverName, StringComparison.OrdinalIgnoreCase));

                if (entry == null)
                {
                    Log.Warning("Server '{serverName}' not found in SERVER_LIST", serverName);
                    return false;
                }

                if (!entry.IsEnabled)
                {
                    Log.Warning("Server '{serverName}' is disabled in SERVER_LIST", serverName);
                    return false;
                }

                // Check if already running
                var psEntry = GetRunningServerProcess(serverName);
                if (psEntry != null)
                {
                    Log.Warning("Server '{serverName}' is already running.", serverName);
                    return false;
                }

                // Start the server
                await StartServerProcess(entry);
                return true;
            }
            catch (Exception ex)
            {
                Log.Error(ex, "Error starting server '{serverName}'.", serverName);
                return false;
            }
        }

        private string GetServerInfoFile(string ServerName)
        {
            String infoFileName = ServerName + ".dbxtune";
            if (DBX_CENTRAL.Equals(ServerName))
                infoFileName = "DbxCentral.info";

            // Get file path from ENV variable
            string HOME_DIR = GetHomeDir();
            string DBXTUNE_CENTRAL_INFO_DIR = Environment.GetEnvironmentVariable("DBXTUNE_CENTRAL_INFO_DIR");
            string DBXTUNE_INFO_DIR = Environment.GetEnvironmentVariable("DBXTUNE_INFO_DIR");

            //string serverInfoFile = Path.Combine(HOME_DIR, ".dbxtune/dbxc/info/" + infoFileName);

            if (!string.IsNullOrEmpty(DBXTUNE_CENTRAL_INFO_DIR))
            {
                string serverInfoFile = Path.Combine(DBXTUNE_CENTRAL_INFO_DIR, infoFileName);
                if (File.Exists(serverInfoFile))
                {
                    return serverInfoFile;
                }
            }

            if (!string.IsNullOrEmpty(DBXTUNE_INFO_DIR))
            {
                string serverInfoFile = Path.Combine(DBXTUNE_INFO_DIR, infoFileName);
                if (File.Exists(serverInfoFile))
                {
                    return serverInfoFile;
                }
            }

            if (true)
            {
                string serverInfoFile = Path.Combine(HOME_DIR, ".dbxtune\\dbxc\\info\\" + infoFileName);
                if (File.Exists(serverInfoFile))
                {
                    return serverInfoFile;
                }
            }

            if (true)
            {
                string serverInfoFile = Path.Combine(HOME_DIR, ".dbxtune\\info\\" + infoFileName);
                if (File.Exists(serverInfoFile))
                {
                    return serverInfoFile;
                }
            }

            //throw new Exception($"Server info file not found for Server '{ServerName}'. Searched in: {DBXTUNE_CENTRAL_INFO_DIR}, {DBXTUNE_INFO_DIR}, {HOME_DIR}/.dbxtune/dbxc/info/{infoFileName}, {HOME_DIR}/.dbxtune/info/{infoFileName}.");
            Log.Warning("Server info file not found for Server '{ServerName}'. Searched in: {CentralInfoDir}, {InfoDir}, {HomeDir}/.dbxtune/dbxc/info/{InfoFile}, {HomeDir}/.dbxtune/info/{InfoFile}.", ServerName, DBXTUNE_CENTRAL_INFO_DIR, DBXTUNE_INFO_DIR, HOME_DIR, infoFileName);
            return null;
        }

        private string GetShutdownUrl(string ServerName)
        {
            string serverInfoFile = GetServerInfoFile(ServerName);
            if (serverInfoFile == null)
            {
                Log.Error("For server '{ServerName}' info file not found.", ServerName);
                return null;
            }
            else
            {
                Log.Information("For server '{ServerName}' found info file '{serverInfoFile}'.", ServerName, serverInfoFile);
            }

            // Read the Java Properties file and extract the shutdown URL with key "dbxtune.management.shutdown.url"
            string targetKey = "dbxtune.management.shutdown.url";
            //string shutdownUrl = $"http://localhost:8080/shutdown?server={ServerName}"; // Some generic that wont work
            string shutdownUrl = null;
            foreach (string line in File.ReadLines(serverInfoFile))
            {
                string trimmedLine = line.Trim();

                // Skip empty lines and comments
                if (string.IsNullOrEmpty(trimmedLine) || trimmedLine.StartsWith("#"))
                    continue;

                // Handle key=value or key: value
                int separatorIndex = trimmedLine.IndexOf('=');
                //if (separatorIndex < 0)
                //    separatorIndex = trimmedLine.IndexOf(':');

                if (separatorIndex > 0)
                {
                    string key = trimmedLine.Substring(0, separatorIndex).Trim();
                    string val = trimmedLine.Substring(separatorIndex + 1).Trim();

                    if (key == targetKey)
                    {
                        shutdownUrl = val.Replace("\\", "");
                        break;
                    }
                }
            }
            if (string.IsNullOrEmpty(shutdownUrl))
            {
                Log.Error("No URL Shutdown command was found for server name '{ServerName}'.", ServerName);
            }

            return shutdownUrl;
        }

        private static string ReplaceBashLikeVariables(string bashStyle, bool expandEnvVarsToValues)
        {
            // Note: This can enhanced to also handle ${VAR:-fallback} etc...
            // Or even replace the variables with "real values" if they are found in the Environment Variables
            bool isWindows = RuntimeInformation.IsOSPlatform(OSPlatform.Windows);
            string windowsStyle = Regex.Replace(bashStyle, @"\$\{(\w+)\}", "%$1%");

            if (isWindows)
            {
                if (expandEnvVarsToValues)
                {
                    windowsStyle = Environment.ExpandEnvironmentVariables(windowsStyle);
                }

                return windowsStyle;
            }
            else
            {
                if (expandEnvVarsToValues)
                {
                    windowsStyle = Environment.ExpandEnvironmentVariables(windowsStyle);
                }
                return windowsStyle;
            }
        }

        // Extract the executable path from a start-script command that may include
        // arguments (e.g. "start.bat <SRVNAME> --serverAlias foo").
        // Handles both quoted paths ("C:\path with spaces\x.bat") and unquoted ones.
        internal static string ExtractScriptFilePath(string cmd)
        {
            if (string.IsNullOrEmpty(cmd)) return cmd;
            cmd = cmd.TrimStart();
            if (cmd.StartsWith("\""))
            {
                int end = cmd.IndexOf('"', 1);
                return end > 1 ? cmd[1..end] : cmd;
            }
            int space = cmd.IndexOf(' ');
            return space > 0 ? cmd[..space] : cmd;
        }

        private string ParseStartScriptForAliasName(string startScript)
        {
            // Look for a pattern like "-A aliasName" or "--serverAlias aliasName"
            string aliasName = Regex.Match(startScript, @"(?:-A|--serverAlias)\s+(\S+)").Groups[1].Value.Trim();

            return aliasName;
        }

        private string ParseStartScriptForConfigFile(string startScript)
        {
            // DbxCentral uses -C / --config; collectors use -n / --noGui
            // Handles both quoted and unquoted path values
            var m = Regex.Match(startScript, @"(?:-C|--config|-n|--noGui)\s+(?:""([^""]+)""|(\S+))");
            if (!m.Success) return "";
            return m.Groups[1].Success ? m.Groups[1].Value : m.Groups[2].Value;
        }

        private List<SrvInfoFileEntry> ReadServerList(string filePath)
        {
            var entries = new List<SrvInfoFileEntry>();

            // Should we ADD "DBX_CENTRAL" here?
            if (_addDbxCentral)
            {
                //string DBXTUNE_INFO_DIR = Environment.GetEnvironmentVariable("DBXTUNE_INFO_DIR");
                bool isWindows = RuntimeInformation.IsOSPlatform(OSPlatform.Windows);
                string HOME_DIR = GetHomeDir();
                string startFile = Path.Combine(HOME_DIR, ".dbxtune", "dbxc", "bin", $"start_dbxcentral.{(isWindows ? "bat" : "sh")}");

                if (Environment.GetEnvironmentVariable("DBXTUNE_CENTRAL_START_SCRIPT") != null)
                {
                    startFile = Environment.GetEnvironmentVariable("DBXTUNE_CENTRAL_START_SCRIPT");
                }

                if (string.IsNullOrEmpty(startFile))
                {
                    Log.Information("DbxCentral will NOT be started, this due to Environment variable DBXTUNE_CENTRAL_START_SCRIPT is empty.");
                }
                else
                {
                    if (_firstTimePrint_dbxCentralScriptName)
                    {
                        Log.Information("DbxCentral will be started using the start script '{startFile}'.", startFile);
                        _firstTimePrint_dbxCentralScriptName = false;
                    }

                    var dbxCentral = new SrvInfoFileEntry
                    {
                        ServerPhysicalName = DBX_CENTRAL,
                        IsEnabled = true,
                        Description = "DbxCentral",
                        StartScript = startFile
                    };

                    entries.Add(dbxCentral);
                }
            }


            try
            {
                string[] lines = File.ReadAllLines(filePath);

                foreach (string line in lines)
                {
                    // Skip comments and empty lines
                    string trimmedLine = line.Trim();
                    if (string.IsNullOrEmpty(trimmedLine) || trimmedLine.StartsWith("#"))
                        continue;

                    // Parse entry
                    string[] parts = trimmedLine.Split(new[] { ';' }, StringSplitOptions.None);
                    if (parts.Length >= 4)
                    {
                        var entry = new SrvInfoFileEntry
                        {
                            ServerPhysicalName = parts[0].Trim(),
                            IsEnabled = parts[1].Trim() == "1",
                            Description = parts[2].Trim(),
                            StartScript = parts[3].Trim()
                        };

                        // enhance: replace bash-like variables with Windows style
                        entry.StartScript = ReplaceBashLikeVariables(entry.StartScript, true);
                        entry.ServerAliasName = ParseStartScriptForAliasName(entry.StartScript);
                        entry.ConfigFile = ParseStartScriptForConfigFile(entry.StartScript);

                        entries.Add(entry);
                        Log.Debug("Parsed entry: {ServerOrAliasName}, Enabled: {IsEnabled}, Script: {StartScript}", entry.ServerOrAliasName, entry.IsEnabled, entry.StartScript);
                    }
                }
            }
            catch (Exception ex)
            {
                Log.Error(ex, "Error reading SERVER_LIST");
            }

            return entries;
        }
        private static string QuoteForShell(string command)
        {
            return "'" + command.Replace("'", "'\\''") + "'";
        }
        private async Task StartServerProcess(SrvInfoFileEntry entry)
        {
            // Replace placeholder in script path
            // NOTE: If the "PysicalName" contains any special chars, escape them with a ^ 
            bool isWindows = RuntimeInformation.IsOSPlatform(OSPlatform.Windows);
            // On Windows, escape colons for cmd.exe; on Linux pass the name unchanged.
            string serverNameParam = isWindows
                ? entry.ServerPhysicalName.Replace(":", "^:")
                : entry.ServerPhysicalName;

            string fullCommand = entry.StartScript.Replace("<SRVNAME>", serverNameParam);

            // Expand environment variables: handles $VAR on Linux and %VAR% on Windows natively.
            // Also expand %VAR% style on Linux (SERVER_LIST files may use Windows-style placeholders).
            fullCommand = Environment.ExpandEnvironmentVariables(fullCommand);
            fullCommand = Regex.Replace(fullCommand, @"%([^%]+)%", m =>
                Environment.GetEnvironmentVariable(m.Groups[1].Value) ?? m.Value);

            // Create log file path
            string logPath = Path.Combine(GetDbxTune_LogDir(), $"{entry.ServerOrAliasName}.console");


            // Create or append log file
            //            FileStream logStream = new FileStream(logPath, FileMode.Append, FileAccess.Write, FileShare.Read);
            FileStream logStream = new FileStream(logPath, FileMode.Create, FileAccess.Write, FileShare.Read);
            StreamWriter logWriter = new StreamWriter(logStream);
            logWriter.AutoFlush = true;

            // Write header to log file
            string header = $"\r\n=== Process started at {DateTime.Now} for Server '{entry.ServerOrAliasName}', using command '{fullCommand}'";
            logWriter.WriteLine(header);

            Log.Information("STARTING process for Server '{entry.ServerOrAliasName}' using script '{fullCommand}', logPath='{logPath}'.", entry.ServerOrAliasName, fullCommand, logPath);

            // Store the log path for later use
            _srvConsoleLogName[entry.ServerOrAliasName] = logPath;

            //// Store process info
            //ProcessInfo pi = new ProcessInfo
            //{
            //    ServerName  = entry.ServerName,
            //    Process     = null,
            //    LogWriter   = logWriter,
            //    LogFilePath = logPath,
            //    JobHandle   = IntPtr.Zero,
            //    startedOk   = false
            //};
            //_runningProcesses.Add(pi);

            try
            {
                // Extract directory and filename from the script path
                string workingDirectory = Path.GetDirectoryName(fullCommand);
                string scriptFile = Path.GetFileName(fullCommand);

                //var match = Regex.Match(fullCommand, @"^(""(?:[^""]|"""")*""|\S+)\s+(.*)$");

                // Determine shell based on OS
                string scriptPath = isWindows ? "cmd.exe" : "/bin/bash";

                // Create process.
                // On Linux, use ArgumentList so the full command is passed as a single argv element
                // to bash -c. Using the Arguments string causes .NET to split on spaces (it only
                // respects double quotes, not single quotes), which would break the command.
                var psi = new ProcessStartInfo
                {
                    FileName = scriptPath,
                    WorkingDirectory = workingDirectory,
                    UseShellExecute = false,
                    RedirectStandardInput = true,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    CreateNoWindow = true,
                };

                if (isWindows)
                    psi.Arguments = "/c " + fullCommand;
                else
                {
                    psi.ArgumentList.Add("-c");
                    psi.ArgumentList.Add(fullCommand);
                }

                Log.Debug("ProcessStartInfo: FileName='{FileName}', Arguments='{Arguments}', ArgumentList=[{ArgumentList}], WorkingDirectory='{WorkingDirectory}'", psi.FileName, psi.Arguments, string.Join(", ", psi.ArgumentList), psi.WorkingDirectory);

                // Expose DbxTune environment variables to the process 
                foreach (var kvp in _dbxEnvVariables)
                {
                    psi.EnvironmentVariables[kvp.Key] = kvp.Value;
                }

                // CREATE the Process, and later on start it
                var process = new Process { StartInfo = psi };

                // Set up output and error redirection to the same log writer
                process.OutputDataReceived += (sender, args) =>
                {
                    if (args.Data != null)
                    {
                        try { logWriter.WriteLine(args.Data); }
                        catch (ObjectDisposedException) { }
                    }
                };

                process.ErrorDataReceived += (sender, args) =>
                {
                    if (args.Data != null)
                    {
                        try { logWriter.WriteLine($"[STDERR] {args.Data}"); }
                        catch (ObjectDisposedException) { }
                    }
                };

                process.EnableRaisingEvents = true;
                process.Exited += (sender, args) =>
                {
                    logWriter.WriteLine($"Process exited with code {process.ExitCode} at {DateTime.Now}");
                    logWriter.Flush();
                    logWriter.Close();
                    logWriter.Dispose();
                    Log.Information("Process for Server '{entry.ServerOrAliasName}' exited with code {process.ExitCode}. Check the logfile '{logPath}' for details.", entry.ServerOrAliasName, process.ExitCode, logPath);
                };

                // Start the process
                process.Start();

                // Begin async reading
                process.BeginOutputReadLine();
                process.BeginErrorReadLine();

                Log.Information("Process started for Server '{entry.ServerOrAliasName}' with PID: {process.Id}", entry.ServerOrAliasName, process.Id);
                Log.Information("Sleeping for 2 seconds, then checking if it started ok.");
                await Task.Delay(2_000);

                process.Refresh(); // Refresh process info to ensure it's running
                if (process.HasExited)
                {
                    Log.Error("Process for Server '{entry.ServerOrAliasName}' exited immediately with code {process.ExitCode}", entry.ServerOrAliasName, process.ExitCode);
                    logWriter.WriteLine($"Process exited immediately with code {process.ExitCode}");
                    throw new InvalidOperationException($"Process for {entry.ServerOrAliasName} exited immediately with code {process.ExitCode}");
                }
            }
            catch (Exception ex)
            {
                // Clean up in case of error
                logWriter.WriteLine($"Failed to start Server '{entry.ServerOrAliasName}' process: {ex.Message}");
                logWriter.Dispose();

                Log.Error(ex, $"Error starting process for Server '{entry.ServerOrAliasName}'.");
                throw;
            }
        }

        private async Task StopServerProcess(DbxProcessInfo psEntry) // TODO: Make this a List, so we can stop multiple processes, (with a single entry if we just want to stop 1 server)
        {
            List<DbxProcessInfo> psList = new List<DbxProcessInfo>();

            psList.Add(psEntry);

            await StopServerProcesses(psList);
        }

        private async Task StopServerProcesses(List<DbxProcessInfo> psList)
        {
            Log.Information("The following {psList.Count} server processes will be stopped!", psList.Count);
            foreach (var entry in psList)
            {
                Log.Information("STOPPING Server '{entry.ServerOrAliasName}' (PID: {entry.Pid})", entry.ServerOrAliasName, entry.Pid);
            }

            // Send a HTTP request to each process to trigger a graceful shutdown
            foreach (var entry in psList)
            {
                await SendHttpShutdownRequestAsync(entry.ServerOrAliasName, entry.Pid);
            }

            // Wait for processes to exit (with timeout)
            const int TIMEOUT_MS = 180_000; // 60 seconds  or should it be higher?
                                            //            FIXME; // Look at why GORAN_UB3_DS is not stopping... it looks like the Jetty Web threads isn't STOPPING... "qtp968113504-42" ...
            DateTime startTime = DateTime.Now;

            while (true)
            {
                // Check if all processes have exited
                if (psList.All(p => !IsPidRunning(p.Pid)))
                {
                    Log.Information("All {psList.Count} requested server processes have been stopped.", psList.Count);
                    break;
                }

                // Check for timeout
                if ((DateTime.Now - startTime).TotalMilliseconds > TIMEOUT_MS)
                {
                    Log.Warning("Timeout waiting for processes to exit.");
                    break;
                }

                // Print processes that are still running
                foreach (DbxProcessInfo entry in psList.Where(p => IsPidRunning(p.Pid)))
                {
                    Log.Warning("Process for Server '{ServerOrAliasName}' (PID: {Pid}) is still running.", entry.ServerOrAliasName, entry.Pid);

                    string logFile = entry.LogFile == null ? "" : entry.LogFile.Trim();
                    if (!string.IsNullOrEmpty(logFile))
                    {
                        String lastLine = GetLastLine(logFile);
                        Log.Information("Last LOG entry for Server '{entry.ServerOrAliasName}': {lastLine}", entry.ServerOrAliasName, lastLine);
                    }
                    else
                    {
                        string consoleFile = _srvConsoleLogName.TryGetValue(entry.ServerOrAliasName, out string tmpStr) ? tmpStr : "";
                        if (!string.IsNullOrEmpty(consoleFile))
                        {
                            String lastLine = GetLastLine(consoleFile);
                            Log.Information("Last CONSOLE entry for Server '{entry.ServerOrAliasName}': {lastLine}", entry.ServerOrAliasName, lastLine);
                        }
                        else
                        {
                            Log.Information("No LOG or CONSOLE file found for Server '{entry.ServerOrAliasName}'.", entry.ServerOrAliasName);
                        }
                    }
                }

                // Wait before checking again
                await Task.Delay(1_000);
            }

            // Force kill any remaining processes
            foreach (var entry in psList.Where(p => IsPidRunning(p.Pid)))
            {
                Log.Warning("Force killing process for Server '{ServerOrAliasName}' (PID: {Pid})", entry.ServerOrAliasName, entry.Pid);
                try
                {
                    Process.GetProcessById((int)entry.Pid).Kill();
                }
                catch (Exception ex)
                {
                    Log.Error(ex, $"Error killing process for Server '{entry.ServerOrAliasName}'.");
                }
            }
        }

        //        private void StopServerProcesses(List<ProcessInfo> procInfoList)
        //        {
        //            // Send Ctrl+C to all running processes
        //            foreach (var procInfo in procInfoList.ToList())
        //            {
        //                try
        //                {
        //                    Log.Information("Sending Ctrl+C to process {ServerName} (PID: {ProcessId})", procInfo.ServerName, procInfo.Process.Id);
        //                    SendCtrlC(procInfo.Process);
        //                }
        //                catch (Exception ex)
        //                {
        //                    Log.Error(ex, "Error sending Ctrl+C to {ServerName}", procInfo.ServerName);
        //                }
        //            }

        //            // But Ctrl-C does NOT seems to work for all processes (e.g. cmd.exe)
        //            // So send a http request to the process to trigger a graceful shutdown
        //            foreach (var procInfo in procInfoList.ToList())
        //            {
        //                SendHttpShutdownRequest(procInfo.ServerName, procInfo.ProcessId);
        //            }


        //            //// Send Ctrl+C to all running processes (but in ANOTHER WAY)
        //            //foreach (var procInfo in procInfoList.ToList())
        //            //{
        //            //    try
        //            //    {
        //            //        // Try sending Ctrl+C by writing to standard input if available
        //            //        if (procInfo.Process.StartInfo.RedirectStandardInput)
        //            //        {
        //            //            Log.Information("Attempting graceful shutdown of {ServerName} (PID: {ProcessId}) by sending Ctrl+C character to STDIN", procInfo.ServerName, procInfo.Process.Id);

        //            //            procInfo.Process.StandardInput.Write("\u0003"); // Ctrl+C character
        //            //            procInfo.Process.StandardInput.Flush();
        //            //        }
        //            //    }
        //            //    catch (Exception ex)
        //            //    {
        //            //        Log.Error(ex, "Error sending shutdown signal to {ServerName}", procInfo.ServerName);
        //            //    }
        //            //}

        //            // First try to close processes gracefully by writing to their input
        //            foreach (var procInfo in procInfoList.ToList())
        //            {
        //                try
        //                {
        //                    Log.Information("Attempting graceful shutdown of {ServerName} (PID: {ProcessId})", procInfo.ServerName, procInfo.Process.Id);

        //                    // Try sending Ctrl+C by writing to standard input if available
        //                    if (procInfo.Process.StartInfo.RedirectStandardInput)
        //                    {
        //                        procInfo.Process.StandardInput.Write("\u0003"); // Ctrl+C character
        //                        procInfo.Process.StandardInput.Flush();
        //                    }
        //                }
        //                catch (Exception ex)
        //                {
        //                    Log.Error(ex, "Error sending shutdown signal to {ServerName}", procInfo.ServerName);
        //                }
        //            }


        //            // Wait for processes to exit (with timeout)
        //            const int TIMEOUT_MS = 60_000; // 30 seconds
        //            DateTime startTime = DateTime.Now;

        //            while (procInfoList.Any(p => !p.Process.HasExited))
        //            {
        //                if ((DateTime.Now - startTime).TotalMilliseconds > TIMEOUT_MS)
        //                {
        //                    Log.Warning("Timeout waiting for processes to exit. Forcing termination.");
        //                    break;
        //                }

        //                Thread.Sleep(1_000);

        //                // TODO: Check if processes are still running and log their status (write LAST line from the log file)
        //                foreach (var procInfo in procInfoList.Where(p => !p.Process.HasExited))
        //                {
        //                    String lastLine = GetLastLine(procInfo.LogFilePath);
        //                    Log.Information("Server '{ServerName}', last log entry: {lastLine}", procInfo.ServerName, lastLine);
        //                }
        //            }

        //            // Force kill any remaining processes
        //            foreach (var procInfo in procInfoList.Where(p => !p.Process.HasExited))
        //            {
        //                Log.Warning("Force killing process {ServerName} (PID: {ProcessId})", procInfo.ServerName, procInfo.Process.Id);
        //                try
        //                {
        //                    procInfo.Process.Kill();
        //                }
        //                catch (Exception ex)
        //                {
        //                    Log.Error(ex, "Error killing process {ServerName}", procInfo.ServerName);
        //                }
        //            }

        //            // Force terminate using job object (will terminate all child processes too)
        //            foreach (var procInfo in procInfoList.Where(p => !p.Process.HasExited))
        //            {
        //                Log.Warning("Force killing JobHandlw {ServerName} (JoBHandle: {ProcessId})", procInfo.ServerName, procInfo.JobHandle);
        //                try
        //                {
        //                    if (procInfo.JobHandle != IntPtr.Zero)
        //                    {
        //                        if (!TerminateJobObject(procInfo.JobHandle, 0))
        //                        {
        //                            Log.Error("Failed to terminate job object: {Error}", Marshal.GetLastWin32Error());
        //                        }
        //                    }
        //                }
        //                catch (Exception ex)
        //                {
        //                    Log.Error(ex, "Error killing process {ServerName}", procInfo.ServerName);
        //                }
        //            }
        //            //if (_jobHandle != IntPtr.Zero)
        //            //{
        //            //    if (!TerminateJobObject(_jobHandle, 0))
        //            //    {
        //            //        Log.Error("Failed to terminate job object: {Error}", Marshal.GetLastWin32Error());
        //            //    }
        //            //}

        //            // As a fallback, try to kill processes individually
        //            foreach (var procInfo in procInfoList.Where(p => !p.Process.HasExited))
        //            {
        //                Log.Warning("Force killing process {ServerName} (PID: {ProcessId})", procInfo.ServerName, procInfo.Process.Id);
        //                try
        //                {
        //                    procInfo.Process.Kill();
        //                    //procInfo.Process.Kill(true); // true = kill entire process tree (requires .NET Core 3.0+)


        //                    // Optionally, you can also try to kill child processes by using the taskkill command
        //                    try
        //                    {
        //                        Log.Warning("Force killing process (using: TASKKILL /F /T /PID {procInfo.Process.Id}) {ServerName} (PID: {ProcessId})", procInfo.ServerName, procInfo.Process.Id);
        //                        Process.Start(new ProcessStartInfo
        //                        {
        //                            FileName = "taskkill",
        //                            Arguments = $"/F /T /PID {procInfo.Process.Id}",
        //                            CreateNoWindow = true,
        //                            UseShellExecute = false
        //                        }).WaitForExit(5000); // Wait up to 5 seconds for the command to complete
        //                    }
        //                    catch (Exception taskKillEx)
        //                    {
        //                        Log.Error(taskKillEx, "Error running taskkill for {ServerName}", procInfo.ServerName);
        //                    }
        //                }
        //                catch (Exception ex)
        //                {
        //                    Log.Error(ex, "Error killing process {ServerName}", procInfo.ServerName);
        //                }
        //            }

        //            // Clean up resources
        //            foreach (var procInfo in procInfoList)
        //            {
        //                try
        //                {
        //                    procInfo.Process.Dispose();
        //                    procInfo.LogWriter?.Dispose();
        //                }
        //                catch (Exception ex)
        //                {
        //                    Log.Error(ex, "Error cleaning up resources for {ServerName}", procInfo.ServerName);
        //                }
        //            }

        //            // Clean up resources
        //            foreach (var procInfo in procInfoList)
        //            {
        //                Log.Information("Server {ServerName} stopped successfully", procInfo.ServerName);

        //                // Remove from list
        ////                _runningProcesses.Remove(procInfo);
        //            }
        //        }

        //private void StopServerProcess(ProcessInfo procInfo) // TODO: Make this a List, so we can stop multiple processes, (with a single entry if we just want to stop 1 server)
        //{
        //    List<ProcessInfo> procList = new List<ProcessInfo>();

        //    procList.Add(procInfo);

        //    StopServerProcesses(procList);
        //}

        private async Task SendHttpShutdownRequestAsync(string srvName, uint pid)
        {
            string shutdownUrl = GetShutdownUrl(srvName);
            if (!string.IsNullOrEmpty(shutdownUrl))
            {
                try
                {
                    Log.Information("Sending HTTP request to process {srvName} (PID: {pid}) using URL: {shutdownUrl}", srvName, pid, shutdownUrl);

                    var response = await _httpClient.GetAsync(shutdownUrl).ConfigureAwait(false);
                    if (response.IsSuccessStatusCode)
                    {
                        Log.Information("Shutdown request sent successfully to {srvName}", srvName);
                    }
                    else
                    {
                        Log.Warning("Failed to send shutdown request to {srvName}: {response.StatusCode}", srvName, response.StatusCode);
                    }
                }
                catch (Exception ex)
                {
                    Log.Error(ex, $"Error sending HTTP request to {srvName}");
                }
            }
            else
            {
                Log.Warning("No shutdown URL found for server '{SrvName}'", srvName);
            }
        }

        internal string GetServiceLogName()
        {
            return GetServiceLogFileName();
            //            return "--NOT-YET-IMPLEMENTED--";
            //throw new NotImplementedException();
        }

        internal string GetServerInfoFile()
        {
            return _srvInfoFilePath;
        }

        internal string GetServiceLogLevel()
        {
            return _logLevel.MinimumLevel.ToString();
        }

        internal LogEventLevel ParseLogLevel(string logLevel)
        {
            switch (logLevel.ToLower())
            {
                case "trace": return LogEventLevel.Verbose;
                case "debug": return LogEventLevel.Debug;
                case "info": return LogEventLevel.Information;
                case "warning": return LogEventLevel.Warning;
                case "error": return LogEventLevel.Error;
                case "fatal": return LogEventLevel.Fatal;
                default:
                    throw new ArgumentException($"Invalid log level '{logLevel}'. Known levels: 'trace, debug, info, warning, error, fatal'.");
            }
        }
        internal string SetServiceLogLevel(string newLogLevel)
        {
            _logLevel.MinimumLevel = ParseLogLevel(newLogLevel);
            return "OK:" + _logLevel.MinimumLevel.ToString();
            //_logLevel = newLogLevel.ToLowerInvariant() => level switch
            //{
            //    "trace"       => LogEventLevel.Verbose,
            //    "debug"       => LogEventLevel.Debug,
            //    "infor"       => LogEventLevel.Information,
            //    "warning"     => LogEventLevel.Warning,
            //    "error"       => LogEventLevel.Error,
            //    "fatal"       => LogEventLevel.Fatal,
            //    _ => throw new ArgumentException($"Invalid log level: {newLogLevel}")
            //};
        }
    }

    public class SrvInfoFileEntry
    {
        public string ServerPhysicalName { get; set; }
        public string ServerAliasName { get; set; }
        public string ServerOrAliasName
        {
            get { return !string.IsNullOrWhiteSpace(ServerAliasName) ? ServerAliasName : ServerPhysicalName; }
        }
        public bool IsEnabled { get; set; }
        public string Description { get; set; }
        public string StartScript { get; set; }
        public string ConfigFile { get; set; }
    }

    public class SrvEntry
    {
        public string ServerName { get; set; }
        public uint Pid { get; set; }
        public bool isRunning { get; set; }
        public string StartTime { get; set; }
        public string ConsoleFile { get; set; }
        public string StartScript { get; set; }
        public string Info { get; set; }
        public string ConfigFile { get; set; }
        public DbxProcessInfo DbxProcessInfo { get; set; }
    }

    public class DbxProcessInfo
    {
        public string ServerName { get; set; }
        public string AliasName { get; set; }
        public string ServerOrAliasName
        {
            get { return !string.IsNullOrWhiteSpace(AliasName) ? AliasName : ServerName; }
        }
        public string DisplayName { get; internal set; }
        public string ServerType { get; set; }
        public string ConfigFile { get; set; }
        public string DbmsUsername { get; set; }
        public string LogFile { get; set; }
        public string SaveDir { get; set; }
        public uint Pid { get; set; }
        public uint ParentPid { get; set; }
        public string StartTime { get; internal set; }
        public string CommandLine { get; set; }

        public override string ToString()
        {
            var type = this.GetType();
            var result = $"{type.Name} {{\n";

            // Include public and non-public instance fields
            //foreach (var field in type.GetFields(BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic))
            //{
            //    var value = field.GetValue(this);
            //    result += $"  {field.Name} = {value}\n";
            //}

            // Include public and non-public instance properties
            foreach (var prop in type.GetProperties(BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic))
            {
                if (prop.GetMethod == null) continue; // Skip write-only
                var value = prop.GetValue(this);
                result += $"  {prop.Name} = {value}\n";
            }

            result += "}";
            return result;
        }
    }

    /**
     * EnvImporter class to import environment variables from a .bat file.
     * Pass the path to the .bat file to ImportEnvFromBat method.
     * 
     * @returns A dictionary with environment variable names as keys and their values as values.
     */
    class EnvImporter
    {
        public static Dictionary<string, string> ImportEnvFromBat(string batFilePath)
        {
            string tempBat = Path.GetTempFileName() + ".bat";
            string tempOutput = Path.GetTempFileName();

            // Create a wrapper .bat that calls the original and dumps env
            File.WriteAllText(tempBat, $@"
@echo off
call ""{batFilePath}""
set > ""{tempOutput}""
");

            var proc = Process.Start(new ProcessStartInfo
            {
                FileName = tempBat,
                UseShellExecute = false,
                CreateNoWindow = true
            });

            proc.WaitForExit();

            // Read and parse the environment variables
            var envVars = new Dictionary<string, string>();
            foreach (var line in File.ReadAllLines(tempOutput))
            {
                int idx = line.IndexOf('=');
                if (idx > 0)
                {
                    string key = line.Substring(0, idx);
                    string val = line.Substring(idx + 1);
                    envVars[key] = val;
                }
            }

            // Clean up temp files
            File.Delete(tempBat);
            File.Delete(tempOutput);

            return envVars;
        }
    }

}

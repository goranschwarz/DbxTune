using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

using System.IO;
using System.IO.Pipes;
using System.Text.Json;
using Serilog;
using System.Security.AccessControl;
using System.Security.Principal;
using System.Diagnostics;
using System.Collections.Concurrent;

namespace DbxStarterService
{
    public class ServiceCommunication
    {
        private readonly DbxStarterService _service;
        private CancellationTokenSource _cancellationTokenSource;
        private const string PIPE_NAME = "DbxStarterServicePipe";
        private static int _clientCounter = 0;
        private List<Task> _listenerTasks = new List<Task>();

        public ServiceCommunication(DbxStarterService service)
        {
            _service = service;
        }

        public void Start()
        {
            _cancellationTokenSource = new CancellationTokenSource();

            // Log current user context for debugging
            string currentUser = Environment.UserName;
#pragma warning disable CA1416
#if WINDOWS
            string currentPrincipal = System.Security.Principal.WindowsIdentity.GetCurrent().Name;
            Log.Information($"Service communication starting under user: '{currentUser}', principal: '{currentPrincipal}'");
#else
            Log.Information($"Service communication starting under user: '{currentUser}'");
#endif
#pragma warning restore CA1416

            // Start multiple listeners to handle concurrent clients
            const int INITIAL_LISTENER_COUNT = 3;
            for (int i = 0; i < INITIAL_LISTENER_COUNT; i++)
            {
                var listenerTask = Task.Run(async () => await PipeListenerLoop(_cancellationTokenSource.Token));
                _listenerTasks.Add(listenerTask);
            }

            Log.Information($"Service communication interface started with {INITIAL_LISTENER_COUNT} named pipe listeners");
        }

        public void Stop()
        {
            try
            {
                _cancellationTokenSource?.Cancel();
                Task.WaitAll(_listenerTasks.ToArray(), 5000);

                Log.Information("Service communication interface stopped");
            }
            catch (Exception ex)
            {
                Log.Error(ex, "Error stopping service communication");
            }
        }

        private async Task PipeListenerLoop(CancellationToken cancellationToken)
        {
            while (!cancellationToken.IsCancellationRequested)
            {
                NamedPipeServerStream pipeServer = null;
                try
                {
#pragma warning disable CA1416
#if WINDOWS
                    // Create pipe with proper security settings for cross-user access
                    PipeSecurity pipeSecurity = new PipeSecurity();

                    // Allow everyone (WorldSid)
                    SecurityIdentifier worldSid = new SecurityIdentifier(WellKnownSidType.WorldSid, null);
                    pipeSecurity.AddAccessRule(new PipeAccessRule(worldSid, PipeAccessRights.ReadWrite, AccessControlType.Allow));

                    // Allow the current user (service account)
                    SecurityIdentifier currentUserSid = WindowsIdentity.GetCurrent().User;
                    pipeSecurity.AddAccessRule(new PipeAccessRule(currentUserSid, PipeAccessRights.FullControl, AccessControlType.Allow));

                    // Allow the built-in Administrators group
                    SecurityIdentifier adminSid = new SecurityIdentifier(WellKnownSidType.BuiltinAdministratorsSid, null);
                    pipeSecurity.AddAccessRule(new PipeAccessRule(adminSid, PipeAccessRights.FullControl, AccessControlType.Allow));

                    Log.Debug($"Creating named pipe '{PIPE_NAME}' with cross-user access...");
                    Log.Debug($"Service running as: {WindowsIdentity.GetCurrent().Name}");
                    Log.Debug($"Service user SID: {currentUserSid}");
#else
                    Log.Debug($"Creating named pipe '{PIPE_NAME}'...");
#endif
#if WINDOWS
                    pipeServer = NamedPipeServerStreamAcl.Create(
                        PIPE_NAME,
                        PipeDirection.InOut,
                        NamedPipeServerStream.MaxAllowedServerInstances,
                        PipeTransmissionMode.Byte,
                        PipeOptions.Asynchronous,
                        4096,
                        4096,
                        pipeSecurity);
#else
                    pipeServer = new NamedPipeServerStream(
                        PIPE_NAME,
                        PipeDirection.InOut,
                        NamedPipeServerStream.MaxAllowedServerInstances,
                        PipeTransmissionMode.Byte,
                        PipeOptions.Asynchronous,
                        4096,
                        4096);
#endif
#pragma warning restore CA1416

                    Log.Debug($"Named pipe '{PIPE_NAME}' created successfully, waiting for client connection...");
                    await pipeServer.WaitForConnectionAsync(cancellationToken);

                    int clientId = Interlocked.Increment(ref _clientCounter);
                    Log.Information($"Client {clientId} connected to pipe '{PIPE_NAME}' (pipe handle: {pipeServer.GetHashCode()})");

                    // CRITICAL: Start a new listener immediately before handling the client
                    // This ensures we always have enough listeners available
                    StartNewListener();

                    // Handle the client in this same task
                    try
                    {
                        await HandleClient(pipeServer, clientId);
                    }
                    catch (IOException ex)
                    {
                        Log.Debug($"Client {clientId} disconnected. (IOException) Message: {ex.Message}");
                    }
                    catch (Exception ex)
                    {
                        Log.Error(ex, $"Error handling client {clientId}");
                    }
                    finally
                    {
                        pipeServer?.Dispose();
                        Log.Debug($"Client {clientId} disconnected, pipe disposed");
                    }
                }
                catch (OperationCanceledException)
                {
                    pipeServer?.Dispose();
                    Log.Debug("Pipe listener cancelled");
                    break;
                }
                catch (Exception ex)
                {
                    Log.Error(ex, "Error in pipe listener");
                    pipeServer?.Dispose();

                    // Brief delay before retrying to avoid tight loop
                    await Task.Delay(100, cancellationToken);
                }
            }
        }

        private void StartNewListener()
        {
            var listenerTask = Task.Run(async () => await PipeListenerLoop(_cancellationTokenSource.Token));
            _listenerTasks.Add(listenerTask);
        }

        private async Task HandleClient(NamedPipeServerStream pipeStream, int clientId)
        {
            using (var reader = new StreamReader(pipeStream, Encoding.UTF8, false, 4096, true))
            using (var writer = new StreamWriter(pipeStream, Encoding.UTF8, 4096, true))
            {
                writer.AutoFlush = true;

                try
                {
                    while (pipeStream.IsConnected)
                    {
                        try
                        {
                            // Read command
                            string command = await reader.ReadLineAsync();
                            if (string.IsNullOrEmpty(command))
                                continue;

                            Log.Verbose($"Client {clientId}: Received command: {command}");

                            // Process command
                            string response = await ProcessCommand(command, clientId);

                            // Send response
                            await writer.WriteLineAsync(response);
                            Log.Verbose($"Client {clientId}: Sent response for command: {command} -->> {response}");
                        }
                        catch (IOException ioEx)
                        {
                            Log.Error(ioEx, $"Client {clientId}: Pipe communication error");
                            break;
                        }
                    }
                }
                catch (IOException)
                {
                    Log.Information($"Client {clientId} disconnected abruptly.");
                }
                catch (Exception ex)
                {
                    Log.Error(ex, $"Error handling client {clientId}");
                }
                finally
                {
                    Log.Information($"Client {clientId} disconnected.");
                }
            }
        }

        private async Task<string> ProcessCommand(string command, int clientId)
        {
            // Time how long the command processing takes
            var stopwatch = System.Diagnostics.Stopwatch.StartNew();

            // Send the command
            string result = await ProcessCommand_private(command);

            stopwatch.Stop();
            Log.Debug($"Client {clientId}, Command '{command}' processed in {stopwatch.ElapsedMilliseconds} ms: -->> {result}");

            return result;
        }

        private async Task<string> ProcessCommand_private(string command)
        {
            try
            {
                if (command.Equals("GetStatus", StringComparison.OrdinalIgnoreCase))
                {
                    //var status = new ServiceStatus
                    //{
                    //    RunningProcesses = _service.GetRunningProcesses().Select(p => new ProcessStatus
                    //    {
                    //        ServerName =  p.ServerName,
                    //        ProcessId  = !p.Process.HasExited ? -1 : p.Process.Id,
                    //        Running    = !p.Process.HasExited,
                    //        StartTime  = !p.Process.HasExited ? DateTime.MinValue : p.Process.StartTime,
                    //        LogFile    =  p.LogFilePath
                    //    }).ToList()
                    //};
                    var status = new ServiceStatus();
                    foreach (SrvEntry srvEntry in _service.GetRunningProcesses())
                    {
                        var srvStatus = new ProcessStatus();

                        srvStatus.ServerName = srvEntry.ServerName;
                        srvStatus.ServerAliasName = "";
                        srvStatus.ServerType = "<unknown>";
                        srvStatus.ConsoleFile = srvEntry.ConsoleFile;
                        srvStatus.StartScript = DbxStarterService.ExtractScriptFilePath(srvEntry.StartScript);
                        srvStatus.Info = srvEntry.Info;

                        srvStatus.ParentProcessId = "";
                        srvStatus.ProcessId = "";
                        srvStatus.Running = false;
                        srvStatus.StartTime = "";
                        srvStatus.LogFile = "";
                        srvStatus.ConfigFile = srvEntry.ConfigFile;
                        srvStatus.DbmsUsername = "";
                        srvStatus.CommandLine = "";

                        if (srvEntry.isRunning)
                        {
                            srvStatus.ServerName = srvEntry.DbxProcessInfo.ServerName;
                            srvStatus.ServerAliasName = srvEntry.DbxProcessInfo.AliasName;
                            srvStatus.ServerType = srvEntry.DbxProcessInfo.ServerType;
                            srvStatus.ParentProcessId = srvEntry.DbxProcessInfo.ParentPid.ToString();
                            srvStatus.ProcessId = srvEntry.Pid.ToString();
                            srvStatus.Running = srvEntry.isRunning;
                            srvStatus.StartTime = srvEntry.StartTime;
                            srvStatus.LogFile = srvEntry.DbxProcessInfo.LogFile;
                            srvStatus.ConfigFile = srvEntry.ConfigFile;
                            srvStatus.DbmsUsername = srvEntry.DbxProcessInfo.DbmsUsername;
                            srvStatus.CommandLine = srvEntry.DbxProcessInfo.CommandLine;
                        }

                        status.RunningProcesses.Add(srvStatus);
                    }

                    return SerializeToJson(status);
                }
                else if (command.Equals("ReloadServerList", StringComparison.OrdinalIgnoreCase))
                {
                    bool success = _service.ReloadServerList();
                    return success ? "OK" : "ERROR";
                }
                else if (command.StartsWith("RestartServer:", StringComparison.OrdinalIgnoreCase))
                {
                    string serverName = command.Substring("RestartServer:".Length);
                    bool success = await _service.RestartServer(serverName);
                    return success ? "OK" : "ERROR";
                }
                else if (command.StartsWith("StopServer:", StringComparison.OrdinalIgnoreCase))
                {
                    string serverName = command.Substring("StopServer:".Length);
                    bool success = await _service.StopServer(serverName);
                    return success ? "OK" : "ERROR";
                }
                else if (command.StartsWith("StartServer:", StringComparison.OrdinalIgnoreCase))
                {
                    string serverName = command.Substring("StartServer:".Length);
                    bool success = await _service.StartServer(serverName);
                    return success ? "OK" : "ERROR";
                }
                else if (command.Equals("GetVersion", StringComparison.OrdinalIgnoreCase))
                {
                    return DbxStarterCommon.Version.VersionAndBuildString;
                }
                else if (command.Equals("GetServiceLogName", StringComparison.OrdinalIgnoreCase))
                {
                    return _service.GetServiceLogName();
                }
                else if (command.Equals("GetServerInfoFile", StringComparison.OrdinalIgnoreCase))
                {
                    return _service.GetServerInfoFile();
                }
                else if (command.Equals("GetServiceLogLevel", StringComparison.OrdinalIgnoreCase))
                {
                    return _service.GetServiceLogLevel();
                }
                else if (command.StartsWith("SetServiceLogLevel:", StringComparison.OrdinalIgnoreCase))
                {
                    string newLogLevel = command.Substring("SetServiceLogLevel:".Length);
                    return _service.SetServiceLogLevel(newLogLevel);
                    //return success ? "OK" : "ERROR";
                }

                return "UNKNOWN_COMMAND";
            }
            catch (Exception ex)
            {
                Log.Error(ex, "Error processing command: {Command}", command);
                return "ERROR";
            }
        }

        private string SerializeToJson<T>(T obj)
        {
            return JsonSerializer.Serialize(obj);
        }

        public class ServiceStatus
        {
            public string Version { get; set; } = DbxStarterCommon.Version.VersionAndBuildString;
            public List<ProcessStatus> RunningProcesses { get; set; } = new List<ProcessStatus>();
        }

        public class ProcessStatus
        {
            public string ServerName { get; set; }
            public string ServerAliasName { get; set; }
            public string ServerType { get; set; }
            public string ParentProcessId { get; set; }
            public string ProcessId { get; set; }
            public bool Running { get; set; }
            public string Info { get; set; }
            public string StartTime { get; set; }
            public string ConsoleFile { get; set; }
            public string LogFile { get; set; }
            public string ConfigFile { get; set; }
            public string DbmsUsername { get; set; }
            public string StartScript { get; set; }
            public string CommandLine { get; set; }
        }
    }
}

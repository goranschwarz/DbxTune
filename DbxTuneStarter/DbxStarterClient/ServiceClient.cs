using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Pipes;
using System.Runtime.Serialization;

namespace DbxStarterClient
{
    // ── data contracts ────────────────────────────────────────────────────────

    [DataContract]
    public class ServiceStatus
    {
        [DataMember]
        public string? Version { get; set; }

        [DataMember]
        public List<ProcessStatus> RunningProcesses { get; set; } = new List<ProcessStatus>();
    }

    [DataContract]
    public class ProcessStatus
    {
        [DataMember] public required string ServerName      { get; set; }
        [DataMember] public required string ServerAliasName { get; set; }
        [DataMember] public required string ServerType      { get; set; }
        [DataMember] public required string ParentProcessId { get; set; }
        [DataMember] public required string ProcessId       { get; set; }
        [DataMember] public bool            Running         { get; set; }
        [DataMember] public required string Info            { get; set; }
        [DataMember] public required string StartTime       { get; set; }
        [DataMember] public required string ConsoleFile     { get; set; }
        [DataMember] public required string LogFile         { get; set; }
        [DataMember] public required string ConfigFile      { get; set; }
        [DataMember] public required string DbmsUsername    { get; set; }
        [DataMember] public required string StartScript     { get; set; }
        [DataMember] public required string CommandLine     { get; set; }
    }

    // ── named pipe client ─────────────────────────────────────────────────────

    public class ServiceClient : IDisposable
    {
        public const string PIPE_NAME = "DbxStarterServicePipe";

        private NamedPipeClientStream? _pipeClient;
        private StreamReader?          _reader;
        private StreamWriter?          _writer;
        private bool                  _isConnected;
        private readonly object       _lock = new object();

        public bool Connect(int timeoutMs = 1000)
        {
            lock (_lock)
            {
                if (_isConnected) return true;
                try
                {
                    _pipeClient = new NamedPipeClientStream(".", PIPE_NAME, PipeDirection.InOut);
                    _pipeClient.Connect(timeoutMs);
                    _reader      = new StreamReader(_pipeClient);
                    _writer      = new StreamWriter(_pipeClient) { AutoFlush = true };
                    _isConnected = true;
                    return true;
                }
                catch
                {
                    CleanupConnection();
                    throw;
                }
            }
        }

        public string SendCommand(string command)
        {
            lock (_lock)
            {
                try
                {
                    if (!_isConnected && !Connect())
                        return "ERROR: Unable to connect to service";

                    _writer?.WriteLine(command);
                    return _reader?.ReadLine() ?? "ERROR: No response";
                }
                catch
                {
                    CleanupConnection();
                    throw;
                }
            }
        }

        private void CleanupConnection()
        {
            try { _reader    ?.Dispose(); } catch { /* ignore */ }
            try { _writer    ?.Dispose(); } catch { /* ignore */ }
            try { _pipeClient?.Dispose(); } catch { /* ignore */ }
            _reader      = null;
            _writer      = null;
            _pipeClient  = null;
            _isConnected = false;
        }

        public void Dispose() => CleanupConnection();
    }
}

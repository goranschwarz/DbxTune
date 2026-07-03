using System;
using System.IO;
using System.Linq;
using System.Text.Json;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Serilog;

namespace DbxStarterService
{
    public static class WebInterface
    {
        public static void MapRoutes(WebApplication app)
        {
            // Serve embedded index.html
            app.MapGet("/", () =>
            {
                var assembly = typeof(WebInterface).Assembly;
                using var stream = assembly.GetManifestResourceStream("DbxStarterService.wwwroot.index.html");
                if (stream == null)
                    return Results.NotFound("index.html not found");
                return Results.Text(new StreamReader(stream).ReadToEnd(), "text/html");
            });

            // GET /api/status
            app.MapGet("/api/status", (DbxStarterService service) =>
            {
                var status = BuildServiceStatus(service);
                return Results.Json(status);
            });

            // POST /api/servers/{name}/start
            app.MapPost("/api/servers/{name}/start", async (string name, DbxStarterService service) =>
            {
                Log.Information("WebUI: StartServer '{Name}'", name);
                bool ok = await service.StartServer(name);
                return ok ? Results.Ok("OK") : Results.Problem("ERROR");
            });

            // POST /api/servers/{name}/stop
            app.MapPost("/api/servers/{name}/stop", async (string name, DbxStarterService service) =>
            {
                Log.Information("WebUI: StopServer '{Name}'", name);
                bool ok = await service.StopServer(name);
                return ok ? Results.Ok("OK") : Results.Problem("ERROR");
            });

            // POST /api/servers/{name}/restart
            app.MapPost("/api/servers/{name}/restart", async (string name, DbxStarterService service) =>
            {
                Log.Information("WebUI: RestartServer '{Name}'", name);
                bool ok = await service.RestartServer(name);
                return ok ? Results.Ok("OK") : Results.Problem("ERROR");
            });

            // POST /api/servers/reload
            app.MapPost("/api/servers/reload", (DbxStarterService service) =>
            {
                Log.Information("WebUI: ReloadServerList");
                bool ok = service.ReloadServerList();
                return ok ? Results.Ok("OK") : Results.Problem("ERROR");
            });

            // GET /api/service/logname
            app.MapGet("/api/service/logname", (DbxStarterService service) =>
                Results.Text(service.GetServiceLogName()));

            // GET /api/service/loglevel
            app.MapGet("/api/service/loglevel", (DbxStarterService service) =>
                Results.Text(service.GetServiceLogLevel()));

            // POST /api/service/loglevel
            app.MapPost("/api/service/loglevel", async (DbxStarterService service, HttpContext ctx) =>
            {
                using var reader = new StreamReader(ctx.Request.Body);
                string body = await reader.ReadToEndAsync();
                string level;
                try
                {
                    var doc = JsonDocument.Parse(body);
                    level = doc.RootElement.GetProperty("level").GetString();
                }
                catch
                {
                    return Results.BadRequest("Expected JSON: {\"level\":\"Debug\"}");
                }
                string result = service.SetServiceLogLevel(level);
                return Results.Text(result);
            });

            // GET /api/service/serverlist-path
            app.MapGet("/api/service/serverlist-path", (DbxStarterService service) =>
                Results.Text(service.GetServerInfoFile()));

            // GET /api/files/read?path=...&tail=N&raw=true
            // raw=true → plain content, no line-number prefix (use for editors)
            // raw=false (default) → each line prefixed with "    N: " (use for viewers)
            app.MapGet("/api/files/read", (HttpRequest req, DbxStarterService service) =>
            {
                string path = req.Query["path"].ToString();
                if (string.IsNullOrEmpty(path))
                    return Results.BadRequest("path parameter is required");

                string fullPath;
                try { fullPath = Path.GetFullPath(path); }
                catch { return Results.BadRequest("Invalid path"); }

                if (!IsAllowedReadPath(fullPath, service))
                    return Results.StatusCode(403);

                if (!File.Exists(fullPath))
                    return Results.NotFound();

                int tail = 0;
                if (req.Query.ContainsKey("tail") && int.TryParse(req.Query["tail"], out int t))
                    tail = t;

                bool raw = req.Query["raw"].ToString().Equals("true", StringComparison.OrdinalIgnoreCase);

                try
                {
                    string content = ReadFileWithShare(fullPath, tail, raw);
                    return Results.Text(content, "text/plain");
                }
                catch (Exception ex)
                {
                    Log.Error(ex, "Error reading file {Path}", fullPath);
                    return Results.Problem("Error reading file");
                }
            });

            // GET /api/files/meta?path=...
            app.MapGet("/api/files/meta", (HttpRequest req, DbxStarterService service) =>
            {
                string path = req.Query["path"].ToString();
                if (string.IsNullOrEmpty(path))
                    return Results.BadRequest("path parameter is required");

                string fullPath;
                try { fullPath = Path.GetFullPath(path); }
                catch { return Results.BadRequest("Invalid path"); }

                if (!IsAllowedReadPath(fullPath, service))
                    return Results.StatusCode(403);

                if (!File.Exists(fullPath))
                    return Results.NotFound();

                var lastWrite = File.GetLastWriteTimeUtc(fullPath);
                return Results.Json(new { lastWriteTime = lastWrite.ToString("o") });
            });

            // POST /api/files/write
            app.MapPost("/api/files/write", async (DbxStarterService service, HttpContext ctx) =>
            {
                using var reader = new StreamReader(ctx.Request.Body);
                string body = await reader.ReadToEndAsync();
                string path, content;
                try
                {
                    var doc = JsonDocument.Parse(body);
                    path = doc.RootElement.GetProperty("path").GetString();
                    content = doc.RootElement.GetProperty("content").GetString();
                }
                catch
                {
                    return Results.BadRequest("Expected JSON: {\"path\":\"...\",\"content\":\"...\"}");
                }

                string fullPath;
                try { fullPath = Path.GetFullPath(path); }
                catch { return Results.BadRequest("Invalid path"); }

                string allowedPath = Path.GetFullPath(service.GetServerInfoFile());
                if (!string.Equals(fullPath, allowedPath, StringComparison.OrdinalIgnoreCase))
                    return Results.StatusCode(403);

                try
                {
                    File.WriteAllText(fullPath, content);
                    Log.Information("WebUI: Wrote file {Path}", fullPath);
                    return Results.Ok("OK");
                }
                catch (Exception ex)
                {
                    Log.Error(ex, "Error writing file {Path}", fullPath);
                    return Results.Problem("Error writing file");
                }
            });

            // Serve embedded wwwroot static assets (JS, CSS, …).
            // Single-segment paths only — /api/** routes are unaffected.
            app.MapGet("/{file}", (string file) =>
            {
                var contentType = Path.GetExtension(file).ToLowerInvariant() switch
                {
                    ".js"  => "application/javascript; charset=utf-8",
                    ".css" => "text/css; charset=utf-8",
                    ".ico" => "image/x-icon",
                    _      => null,
                };
                if (contentType is null) return Results.NotFound();

                var assembly = typeof(WebInterface).Assembly;
                using var stream = assembly.GetManifestResourceStream(
                    "DbxStarterService.wwwroot." + file);
                return stream is null
                    ? Results.NotFound()
                    : Results.Text(new StreamReader(stream).ReadToEnd(), contentType);
            });
        }

        private static ServiceCommunication.ServiceStatus BuildServiceStatus(DbxStarterService service)
        {
            var status = new ServiceCommunication.ServiceStatus();
            foreach (SrvEntry srvEntry in service.GetRunningProcesses())
            {
                var srvStatus = new ServiceCommunication.ProcessStatus();

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
            return status;
        }

        private static bool IsAllowedReadPath(string fullPath, DbxStarterService service)
        {
            var allowedRoots = new[]
            {
                DbxStarterService.GetDbxTune_BaseDir(),
                DbxStarterService.GetDbxTune_LogDir(),
                DbxStarterService.GetDbxTune_ConfDir(),
                DbxStarterService.GetDbxTune_InfoDir(),
            };

            foreach (var root in allowedRoots)
            {
                if (string.IsNullOrEmpty(root)) continue;
                string normalizedRoot = Path.GetFullPath(root);
                if (!normalizedRoot.EndsWith(Path.DirectorySeparatorChar.ToString()))
                    normalizedRoot += Path.DirectorySeparatorChar;
                if (fullPath.StartsWith(normalizedRoot, StringComparison.OrdinalIgnoreCase))
                    return true;
            }
            return false;
        }

        private static string ReadFileWithShare(string path, int tail, bool raw = false)
        {
            using var fs = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.ReadWrite);
            using var sr = new StreamReader(fs);
            string all = sr.ReadToEnd();

            // raw=true: return the file content exactly as-is (for editors — no line numbers)
            if (raw)
            {
                if (tail <= 0) return all;
                var rawLines = all.Split('\n');
                int rawStart = Math.Max(0, rawLines.Length - tail);
                return string.Join('\n', rawLines, rawStart, rawLines.Length - rawStart);
            }

            // raw=false: prefix each line with its 1-based line number (for viewers)
            var lines = all.Split('\n');
            int start = tail > 0 ? Math.Max(0, lines.Length - tail) : 0;
            var sb = new System.Text.StringBuilder();
            for (int i = start; i < lines.Length; i++)
            {
                string line = lines[i].TrimEnd('\r');
                sb.Append($"{i + 1,5}: {line}\n");
            }
            return sb.ToString();
        }
    }
}

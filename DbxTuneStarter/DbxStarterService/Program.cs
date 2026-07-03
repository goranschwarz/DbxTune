#nullable enable
using System;
using System.IO;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;

namespace DbxStarterService
{
    internal static class Program
    {
        static void Main(string[] args)
        {
            var builder = WebApplication.CreateBuilder(args);

            // Locate DbxStarterService.json from the registry key written by the installer.
            // HKLM\SYSTEM\CurrentControlSet\Services\DbxStarterService\Parameters\ConfigDir
            // This separates the live config from the exe so upgrades never overwrite user settings.
            string? configDir = null;
            if (OperatingSystem.IsWindows())
            {
                try
                {
                    using var key = Microsoft.Win32.Registry.LocalMachine.OpenSubKey(
                        @"SYSTEM\CurrentControlSet\Services\DbxStarterService\Parameters");
                    configDir = key?.GetValue("ConfigDir") as string;
                }
                catch { /* missing key — handled below */ }
            }

            if (!string.IsNullOrWhiteSpace(configDir))
            {
                // Production path: read from the conf directory set by the installer.
                string configFile = Path.Combine(configDir, "DbxStarterService.json");
                builder.Configuration.AddJsonFile(configFile, optional: true, reloadOnChange: true);
            }
            else
            {
                // Developer / fallback path: read from the exe's own directory.
                // Place a DbxStarterService.json there for local testing.
                builder.Configuration.AddJsonFile("DbxStarterService.json", optional: true, reloadOnChange: true);
            }

            builder.Host
                .UseWindowsService(o => o.ServiceName = "DbxStarterService")
                .UseSystemd();

            builder.Services.AddSingleton<DbxStarterService>();
            builder.Services.AddHostedService(sp => sp.GetRequiredService<DbxStarterService>());

            int    webPort = builder.Configuration.GetValue<int>   ("DbxStarter:WebPort", 8055);
            string webBind = builder.Configuration.GetValue<string>("DbxStarter:WebBind", "localhost") ?? "localhost";

            if (webPort < 0)
            {
                // Disabled: configure Kestrel with no endpoints and clear any URL defaults
                // so the service runs as a pure Windows service with no HTTP listener.
                builder.WebHost.UseUrls();
                builder.WebHost.ConfigureKestrel(_ => { });
            }
            else
            {
                bool anyNetwork = webBind.Equals("*",   StringComparison.OrdinalIgnoreCase)
                               || webBind.Equals("any", StringComparison.OrdinalIgnoreCase);
                var address = anyNetwork
                    ? System.Net.IPAddress.Any
                    : System.Net.IPAddress.Loopback;

                builder.WebHost.ConfigureKestrel(options =>
                {
                    options.Listen(address, webPort);
                });
            }

            var app = builder.Build();
            WebInterface.MapRoutes(app);
            app.Run();
        }
    }
}

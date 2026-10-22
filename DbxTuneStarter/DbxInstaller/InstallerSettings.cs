using System;
using System.IO;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace DbxInstaller
{
    // Values the wizard remembers between runs, so restarting the installer doesn't mean typing
    // them again — never the password. Stored in %APPDATA%\DbxInstaller\settings.json of the user
    // running the installer (the elevated admin). Best effort: a missing, unreadable or corrupt
    // file just means the built-in defaults, and a failed save is ignored.
    internal static class InstallerSettings
    {
        private static readonly string _path = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "DbxInstaller", "settings.json");

        private static readonly SettingsData _data = Load();

        // Package Source page: true = download, false = local ZIP, null = nothing remembered yet
        public static bool?   DownloadZip    => _data.DownloadZip;
        public static string? ZipLocalPath   => _data.ZipLocalPath;

        // Service Account page
        public static string? ServiceAccount => _data.ServiceAccount;

        public static void RememberPackageSource(bool downloadZip, string zipLocalPath)
        {
            _data.DownloadZip = downloadZip;
            if (zipLocalPath.Length > 0) _data.ZipLocalPath = zipLocalPath;   // keep the last ZIP while "Download" is chosen
            Save();
        }

        public static void RememberServiceAccount(string account)
        {
            if (account.Length == 0) return;
            _data.ServiceAccount = account;
            Save();
        }

        private static SettingsData Load()
        {
            try
            {
                if (File.Exists(_path))
                    return JsonSerializer.Deserialize<SettingsData>(File.ReadAllText(_path)) ?? new SettingsData();
            }
            catch { /* unreadable or corrupt — use the defaults */ }
            return new SettingsData();
        }

        private static void Save()
        {
            try
            {
                Directory.CreateDirectory(Path.GetDirectoryName(_path)!);
                File.WriteAllText(_path, JsonSerializer.Serialize(_data, new JsonSerializerOptions { WriteIndented = true }));
            }
            catch { /* ignore — remembering is a convenience */ }
        }

        private sealed class SettingsData
        {
            [JsonPropertyName("downloadZip")]    public bool?   DownloadZip    { get; set; }
            [JsonPropertyName("zipLocalPath")]   public string? ZipLocalPath   { get; set; }
            [JsonPropertyName("serviceAccount")] public string? ServiceAccount { get; set; }
        }
    }
}

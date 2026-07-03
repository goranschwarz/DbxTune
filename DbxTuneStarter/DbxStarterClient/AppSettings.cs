using System;
using System.IO;
using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.Win32;

namespace DbxStarterClient
{
    internal static class AppSettings
    {
        private static readonly string _path = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "DbxStarterClient", "settings.json");

        public static bool DarkMode       { get; private set; }
        public static bool HasUserOverride { get; private set; }

        public static double? WindowLeft      { get; private set; }
        public static double? WindowTop       { get; private set; }
        public static double? WindowWidth     { get; private set; }
        public static double? WindowHeight    { get; private set; }
        public static bool    WindowMaximized { get; private set; }

        public static void Load()
        {
            try
            {
                if (File.Exists(_path))
                {
                    var data = JsonSerializer.Deserialize<SettingsData>(File.ReadAllText(_path));
                    if (data != null)
                    {
                        if (data.DarkMode.HasValue)
                        {
                            DarkMode        = data.DarkMode.Value;
                            HasUserOverride = true;
                        }
                        WindowLeft      = data.WindowLeft;
                        WindowTop       = data.WindowTop;
                        WindowWidth     = data.WindowWidth;
                        WindowHeight    = data.WindowHeight;
                        WindowMaximized = data.WindowMaximized;
                    }
                }
            }
            catch { /* fall through to system default */ }

            // No saved dark-mode preference — follow the Windows system theme.
            if (!HasUserOverride)
                DarkMode = ReadSystemDarkMode();
        }

        /// <summary>
        /// Reads the Windows "Apps use light theme" registry value.
        /// Returns true  → dark mode (AppsUseLightTheme = 0 or missing).
        /// Returns false → light mode (AppsUseLightTheme = 1).
        /// </summary>
        public static bool ReadSystemDarkMode()
        {
            try
            {
                using var key = Registry.CurrentUser.OpenSubKey(
                    @"Software\Microsoft\Windows\CurrentVersion\Themes\Personalize");
                if (key?.GetValue("AppsUseLightTheme") is int v)
                    return v == 0;   // 0 = dark apps, 1 = light apps
            }
            catch { }
            return false;  // fallback: light (most users still run light theme)
        }

        /// <summary>
        /// Called when Windows reports a theme change.
        /// Applies the new system theme only if the user has not manually overridden.
        /// Returns true if DarkMode actually changed (so the caller can repaint).
        /// </summary>
        public static bool ApplySystemTheme()
        {
            if (HasUserOverride) return false;
            bool newDark = ReadSystemDarkMode();
            if (newDark == DarkMode) return false;
            DarkMode = newDark;
            return true;
        }

        public static void ToggleDarkMode()
        {
            DarkMode       = !DarkMode;
            HasUserOverride = true;
            Save();
        }

        public static void SaveWindowBounds(double left, double top, double width, double height, bool maximized)
        {
            WindowLeft      = left;
            WindowTop       = top;
            WindowWidth     = width;
            WindowHeight    = height;
            WindowMaximized = maximized;
            Save();
        }

        private static void Save()
        {
            try
            {
                Directory.CreateDirectory(Path.GetDirectoryName(_path)!);
                var data = new SettingsData
                {
                    DarkMode        = HasUserOverride ? DarkMode : (bool?)null,
                    WindowLeft      = WindowLeft,
                    WindowTop       = WindowTop,
                    WindowWidth     = WindowWidth,
                    WindowHeight    = WindowHeight,
                    WindowMaximized = WindowMaximized,
                };
                File.WriteAllText(_path, JsonSerializer.Serialize(data));
            }
            catch { /* ignore */ }
        }

        private sealed class SettingsData
        {
            [JsonPropertyName("darkMode")]        public bool?  DarkMode        { get; set; }
            [JsonPropertyName("windowLeft")]       public double? WindowLeft      { get; set; }
            [JsonPropertyName("windowTop")]        public double? WindowTop       { get; set; }
            [JsonPropertyName("windowWidth")]      public double? WindowWidth     { get; set; }
            [JsonPropertyName("windowHeight")]     public double? WindowHeight    { get; set; }
            [JsonPropertyName("windowMaximized")]  public bool    WindowMaximized { get; set; }
        }
    }
}

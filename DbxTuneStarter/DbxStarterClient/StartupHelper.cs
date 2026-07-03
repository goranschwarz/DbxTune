using System;
using Microsoft.Win32;

namespace DbxStarterClient
{
    internal static class StartupHelper
    {
        private const string RUN_KEY  = @"SOFTWARE\Microsoft\Windows\CurrentVersion\Run";
        private const string APP_NAME = "DbxStarterClient";

        public static bool IsEnabled()
        {
            using var key = Registry.CurrentUser.OpenSubKey(RUN_KEY, writable: false);
            return key?.GetValue(APP_NAME) != null;
        }

        public static void Enable(string exePath)
        {
            using var key = Registry.CurrentUser.OpenSubKey(RUN_KEY, writable: true);
            key?.SetValue(APP_NAME, $"\"{exePath}\"");
        }

        public static void Disable()
        {
            using var key = Registry.CurrentUser.OpenSubKey(RUN_KEY, writable: true);
            key?.DeleteValue(APP_NAME, throwOnMissingValue: false);
        }

        // Call on startup: if already registered, refresh the path in case the exe moved.
        public static void RefreshIfEnabled(string exePath)
        {
            if (IsEnabled())
                Enable(exePath);
        }
    }
}

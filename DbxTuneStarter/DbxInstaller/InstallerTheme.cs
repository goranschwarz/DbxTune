using Microsoft.Win32;

namespace DbxInstaller
{
    /// Reads the Windows system theme once at startup so the wizard can pick
    /// which WPF ResourceDictionary (Themes/Light.xaml or Themes/Dark.xaml) to merge.
    internal static class InstallerTheme
    {
        public static bool IsDark { get; } = ReadSystemDarkMode();

        private static bool ReadSystemDarkMode()
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
    }
}

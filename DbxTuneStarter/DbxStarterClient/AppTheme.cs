using System;
using System.Windows;

namespace DbxStarterClient
{
    // Live theme-swap engine. Unlike DbxInstaller's InstallerTheme (which picks
    // light/dark once at startup and never changes it), DbxStarterClient must
    // support toggling dark mode at runtime and following OS theme changes live.
    //
    // Index 0 of Application.Resources.MergedDictionaries is always the palette
    // (Light.xaml or Dark.xaml); index 1 is AppStyles.xaml and is never swapped.
    // Because every themed brush is wired via DynamicResource, replacing index 0
    // makes WPF repaint every control automatically.
    internal static class AppTheme
    {
        public static void Apply(bool dark)
        {
            var dict = new ResourceDictionary
            {
                Source = new Uri(dark ? "Themes/Dark.xaml" : "Themes/Light.xaml", UriKind.Relative)
            };
            Application.Current.Resources.MergedDictionaries[0] = dict;
        }
    }
}

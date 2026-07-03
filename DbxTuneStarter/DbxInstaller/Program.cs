using System;
using System.Linq;
using System.Windows;
using DbxInstaller.Wizard;

namespace DbxInstaller
{
    static class Program
    {
        static bool IsRemove(string a) =>
            a.Equals("--remove", StringComparison.OrdinalIgnoreCase) ||
            a.Equals("-remove",  StringComparison.OrdinalIgnoreCase) ||
            a.Equals("/remove",  StringComparison.OrdinalIgnoreCase);

        [STAThread]
        static void Main(string[] args)
        {
            var unknown = args.Where(a => !IsRemove(a)).ToArray();
            if (unknown.Length > 0)
            {
                ShowUsage(unknown);
                return;
            }

            var app = new Application { ShutdownMode = ShutdownMode.OnMainWindowClose };
            app.Resources.MergedDictionaries.Add(new ResourceDictionary
            {
                Source = new Uri(InstallerTheme.IsDark ? "Themes/Dark.xaml" : "Themes/Light.xaml", UriKind.Relative)
            });
            app.Resources.MergedDictionaries.Add(new ResourceDictionary
            {
                Source = new Uri("Themes/WizardStyles.xaml", UriKind.Relative)
            });

            app.DispatcherUnhandledException += (s, e) =>
            {
                ShowFatal("Unhandled UI exception", e.Exception);
                e.Handled = true;
            };
            AppDomain.CurrentDomain.UnhandledException += (s, e) =>
                ShowFatal("Fatal error", e.ExceptionObject as Exception);

            try
            {
                var config = new InstallConfig { Mode = args.Any(IsRemove) ? InstallMode.Remove : InstallMode.Install };
                var window = new MainWizardWindow(config, skipWelcome: args.Any(IsRemove));
                app.Run(window);
            }
            catch (Exception ex)
            {
                ShowFatal("Startup error", ex);
            }
        }

        private static void ShowUsage(string[] unknownArgs)
        {
            string bad = string.Join("  ", unknownArgs.Select(a => $"'{a}'"));
            string msg =
                $"Unknown argument{(unknownArgs.Length == 1 ? "" : "s")}:  {bad}\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "  DbxInstaller.exe\n" +
                "        Open the setup wizard (no arguments)\n" +
                "\n" +
                "  DbxInstaller.exe  --remove\n" +
                "        Open the setup wizard directly in Remove mode\n" +
                "        Aliases: -remove  /remove\n";

            MessageBox.Show(msg, "DbxInstaller — Usage", MessageBoxButton.OK, MessageBoxImage.Warning);
        }

        private static void ShowFatal(string title, Exception? ex)
        {
            string text = ex?.ToString() ?? "Unknown error";

            var txt = new System.Windows.Controls.TextBox
            {
                IsReadOnly = true,
                AcceptsReturn = true,
                TextWrapping = TextWrapping.NoWrap,
                VerticalScrollBarVisibility = System.Windows.Controls.ScrollBarVisibility.Auto,
                HorizontalScrollBarVisibility = System.Windows.Controls.ScrollBarVisibility.Auto,
                FontFamily = new System.Windows.Media.FontFamily("Consolas"),
                Text = text,
            };
            var btn = new System.Windows.Controls.Button { Content = "Close", Height = 30, Width = 90,
                Margin = new Thickness(8), HorizontalAlignment = HorizontalAlignment.Right };

            var grid = new System.Windows.Controls.Grid();
            grid.RowDefinitions.Add(new System.Windows.Controls.RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
            grid.RowDefinitions.Add(new System.Windows.Controls.RowDefinition { Height = GridLength.Auto });
            System.Windows.Controls.Grid.SetRow(txt, 0);
            System.Windows.Controls.Grid.SetRow(btn, 1);
            grid.Children.Add(txt);
            grid.Children.Add(btn);

            var dlg = new Window
            {
                Title = $"DbxInstaller — {title}",
                Width = 700,
                Height = 400,
                WindowStartupLocation = WindowStartupLocation.CenterScreen,
                Content = grid,
            };
            btn.Click += (_, _) => dlg.Close();
            dlg.ShowDialog();
        }
    }
}

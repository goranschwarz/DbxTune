using System;
using System.Windows;

namespace DbxStarterClient
{
    static class Program
    {
        [STAThread]
        static void Main()
        {
            // Load the persisted/system dark-mode preference before the first paint so
            // the theme dictionaries below are correct from frame one (no light→dark flash).
            AppSettings.Load();

            var app = new Application { ShutdownMode = ShutdownMode.OnExplicitShutdown };
            app.Resources.MergedDictionaries.Add(new ResourceDictionary
            {
                Source = new Uri(AppSettings.DarkMode ? "Themes/Dark.xaml" : "Themes/Light.xaml", UriKind.Relative)
            });
            app.Resources.MergedDictionaries.Add(new ResourceDictionary
            {
                Source = new Uri("Themes/AppStyles.xaml", UriKind.Relative)
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
                var window = new MainWindow();
                app.Run(window);
            }
            catch (Exception ex)
            {
                ShowFatal("Startup error", ex);
            }
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
                Title = $"DbxStarter Client — {title}",
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

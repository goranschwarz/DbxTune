using System;
using System.Globalization;
using System.Windows;
using System.Windows.Data;
using System.Windows.Media;

namespace DbxStarterClient
{
    // Drives the colored status dot in MainWindow's DataGrid (Running = green, Stopped = red).
    // Looks up the brush from Application.Current.Resources on every conversion (not cached),
    // so it automatically tracks the live light/dark theme swap done by AppTheme.Apply.
    internal sealed class StatusToBrushConverter : IValueConverter
    {
        public object Convert(object? value, Type targetType, object? parameter, CultureInfo culture)
        {
            bool isRunning = "Running".Equals(value as string, StringComparison.OrdinalIgnoreCase);
            string key = isRunning ? "GreenBrush" : "RedBrush";
            return Application.Current.Resources[key] as Brush ?? Brushes.Gray;
        }

        public object ConvertBack(object? value, Type targetType, object? parameter, CultureInfo culture)
            => throw new NotSupportedException();
    }
}

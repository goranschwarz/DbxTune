using System;
using System.Diagnostics;
using System.IO;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;

namespace DbxInstaller.Wizard
{
    // Small builders shared by every wizard page to keep page code focused on content, not layout boilerplate.
    internal static class PageHelpers
    {
        // A Hyperlink inline that opens in the user's default browser and also copies the URL to
        // the clipboard — this installer is often run on servers with no internet access, so the
        // user may need to paste the link into a browser on a different machine.
        private static Hyperlink BuildHyperlink(string text, string url)
        {
            var link = new Hyperlink(new Run(text)) { NavigateUri = new Uri(url) };
            link.RequestNavigate += (_, e) =>
            {
                try { Clipboard.SetText(e.Uri.AbsoluteUri); } catch { /* best effort */ }
                try { Process.Start(new ProcessStartInfo(e.Uri.AbsoluteUri) { UseShellExecute = true }); }
                catch { /* best effort — e.g. no internet access; the URL is on the clipboard regardless */ }
                e.Handled = true;
            };
            return link;
        }

        // A standalone clickable/copyable hyperlink line — see BuildHyperlink.
        public static TextBlock Link(string text, string url)
        {
            var tb = new TextBlock { Margin = new Thickness(16, 2, 0, 2) };
            tb.Inlines.Add(BuildHyperlink(text, url));
            return tb;
        }

        // A dim bullet-list line ("  • <prefix>") ending in a clickable/copyable link — used to
        // list a dependency and its download link together, always visible regardless of whether
        // the dependency is currently satisfied.
        public static TextBlock BulletWithLink(string prefix, string linkText, string url)
        {
            var tb = new TextBlock
            {
                Style = (Style)Application.Current.Resources["DimText"],
                TextWrapping = TextWrapping.Wrap,
            };
            tb.Inlines.Add(new Run("  • " + prefix));
            tb.Inlines.Add(BuildHyperlink(linkText, url));
            return tb;
        }

        // A dim bullet-list line with no link.
        public static TextBlock Bullet(string text) => Dim("  • " + text);

        public static TextBlock Title(string text) =>
            new() { Text = text, Style = (Style)Application.Current.Resources["PageTitle"] };

        public static TextBlock Subtitle(string text) =>
            new() { Text = text, Style = (Style)Application.Current.Resources["PageSubtitle"] };

        public static TextBlock SectionHeader(string text) =>
            new() { Text = text, Style = (Style)Application.Current.Resources["SectionHeader"] };

        public static TextBlock Dim(string text) =>
            new() { Text = text, Style = (Style)Application.Current.Resources["DimText"], TextWrapping = TextWrapping.Wrap };

        // Looks like Title/Subtitle/Dim text but is selectable/copyable — use for any text that
        // includes a file or directory path (log files, install/config paths, etc.).
        public static TextBox SelectableText(string text) =>
            new() { Text = text, Style = (Style)Application.Current.Resources["SelectableText"] };

        public static TextBox SelectableDim(string text) =>
            new() { Text = text, Style = (Style)Application.Current.Resources["SelectableDimText"] };

        // A [label] [input] row, followed by any number of trailing elements in their own columns
        // (e.g. a Browse button, then a Save button, then a status label) — the "field + button(s)
        // + inline status" layout used by HTTP Port-style rows.
        // Note: the label TextBlock is always added as Children[0] — callers that need to reach it
        // afterwards (e.g. to attach a tooltip for a disabled row) rely on that ordering.
        public static Grid Row(string label, FrameworkElement input, params FrameworkElement[] extras)
        {
            var grid = new Grid { Margin = new Thickness(0, 4, 0, 4) };
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(160) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            foreach (var _ in extras)
                grid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });

            var lbl = new TextBlock { Text = label, VerticalAlignment = VerticalAlignment.Center };
            Grid.SetColumn(lbl, 0);
            grid.Children.Add(lbl);

            Grid.SetColumn(input, 1);
            grid.Children.Add(input);

            for (int i = 0; i < extras.Length; i++)
            {
                var extra = extras[i];
                Grid.SetColumn(extra, 2 + i);
                extra.Margin = new Thickness(6, 0, 0, 0);
                extra.VerticalAlignment = VerticalAlignment.Center;
                grid.Children.Add(extra);
            }
            return grid;
        }

        public static TextBox TextBox(string text = "") => new() { Text = text, Margin = new Thickness(0, 2, 0, 2) };

        // A "..." button that opens a folder-picker dialog and writes the chosen path into target.
        public static Button BrowseFolderButton(TextBox target)
        {
            var btn = new Button { Content = "...", MinWidth = 0, Padding = new Thickness(10, 6, 10, 6) };
            btn.Click += (_, _) =>
            {
                var dlg = new Microsoft.Win32.OpenFolderDialog { Title = "Select Folder" };
                string current = target.Text.Trim();
                if (Directory.Exists(current)) dlg.InitialDirectory = current;
                if (dlg.ShowDialog() == true) target.Text = dlg.FolderName;
            };
            return btn;
        }

        public static StackPanel Stack(params UIElement[] children)
        {
            var p = new StackPanel();
            foreach (var c in children) p.Children.Add(c);
            return p;
        }
    }
}

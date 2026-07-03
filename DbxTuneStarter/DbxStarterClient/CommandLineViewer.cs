using System.Collections.Generic;
using System.Text;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Media;

namespace DbxStarterClient
{
    // Shows a process's full command line plus a colorized, one-argument-per-line breakdown.
    // The tokenize/format logic below is a direct C# port of tokenizeCmdLine/formatCmdLineMulti
    // in DbxStarterService/wwwroot/index.html, so the two UIs read command lines identically.
    public class CommandLineViewer : Window
    {
        private static readonly Color ExecutableColor = (Color)ColorConverter.ConvertFromString("#E8C87A");
        private static readonly Color DoubleDashColor  = (Color)ColorConverter.ConvertFromString("#88B4E8");
        private static readonly Color SingleDashColor  = (Color)ColorConverter.ConvertFromString("#88C8E8");
        private static readonly Color ValueColor       = (Color)ColorConverter.ConvertFromString("#BBBBBB");
        private static readonly Color OtherColor       = (Color)ColorConverter.ConvertFromString("#C8B88A");

        public CommandLineViewer(string serverName, string commandLine)
        {
            Title     = $"Command Line — {serverName}";
            Width     = 900;
            Height    = 600;
            MinWidth  = 500;
            MinHeight = 300;
            WindowStartupLocation = WindowStartupLocation.CenterOwner;

            var root = new DockPanel();

            // ── button bar (bottom) ─────────────────────────────────────────────
            var btnCopy = new Button { Content = "Copy Full Command Line", Margin = new Thickness(0, 0, 8, 0) };
            btnCopy.Click += (_, _) => Clipboard.SetText(commandLine);

            var btnClose = new Button { Content = "Close" };
            btnClose.Click += (_, _) => Close();

            var buttonRow = new StackPanel
            {
                Orientation = Orientation.Horizontal,
                HorizontalAlignment = HorizontalAlignment.Right,
                Margin = new Thickness(12),
            };
            buttonRow.Children.Add(btnCopy);
            buttonRow.Children.Add(btnClose);
            DockPanel.SetDock(buttonRow, Dock.Bottom);
            root.Children.Add(buttonRow);

            // ── "Full command line" (raw, selectable, horizontally scrollable) ──
            var fullLabel = new TextBlock { Text = "Full command line", Margin = new Thickness(12, 12, 12, 4) };
            fullLabel.SetResourceReference(TextBlock.ForegroundProperty, "FgDimBrush");
            DockPanel.SetDock(fullLabel, Dock.Top);
            root.Children.Add(fullLabel);

            var fullBox = new TextBox
            {
                Text = commandLine,
                IsReadOnly = true,
                IsReadOnlyCaretVisible = true,
                TextWrapping = TextWrapping.NoWrap,
                FontFamily = new FontFamily("Consolas"),
                FontSize = 12,
                Background = new SolidColorBrush(Color.FromRgb(0x1A, 0x1A, 0x1A)),
                Foreground = new SolidColorBrush(Color.FromRgb(0xBB, 0xBB, 0xBB)),
                BorderThickness = new Thickness(0),
                Padding = new Thickness(8, 6, 8, 6),
                Margin = new Thickness(12, 0, 12, 12),
                MaxHeight = 80,
                VerticalScrollBarVisibility = ScrollBarVisibility.Auto,
                HorizontalScrollBarVisibility = ScrollBarVisibility.Auto,
                AcceptsReturn = true,
            };
            DockPanel.SetDock(fullBox, Dock.Top);
            root.Children.Add(fullBox);

            // ── "Arguments (one per line)" ───────────────────────────────────────
            var splitLabel = new TextBlock { Text = "Arguments (one per line)", Margin = new Thickness(12, 0, 12, 4) };
            splitLabel.SetResourceReference(TextBlock.ForegroundProperty, "FgDimBrush");
            DockPanel.SetDock(splitLabel, Dock.Top);
            root.Children.Add(splitLabel);

            // Colors above are tuned for a dark backdrop (matching the web UI's own panel
            // background), so this area stays a fixed dark surface regardless of app theme —
            // same reasoning as LogViewer's always-dark Scintilla background.
            var splitText = new TextBlock
            {
                FontFamily = new FontFamily("Consolas"),
                FontSize = 12,
                Background = Brushes.Transparent,
            };
            BuildColorizedLines(commandLine, splitText);

            var splitBorder = new Border
            {
                Background = new SolidColorBrush(Color.FromRgb(0x1A, 0x1A, 0x1A)),
                Padding = new Thickness(8, 6, 8, 6),
                Margin = new Thickness(12, 0, 12, 12),
                Child = new ScrollViewer
                {
                    Content = splitText,
                    VerticalScrollBarVisibility = ScrollBarVisibility.Auto,
                    HorizontalScrollBarVisibility = ScrollBarVisibility.Auto,
                },
            };
            root.Children.Add(splitBorder);   // last child — fills remaining space

            Content = root;
        }

        // Tokenize a command line respecting double-quoted strings (quote chars are kept in
        // the token, matching the web UI's tokenizeCmdLine).
        private static List<string> TokenizeCmdLine(string cmdLine)
        {
            var tokens = new List<string>();
            var cur = new StringBuilder();
            bool inQuotes = false;
            foreach (char c in cmdLine)
            {
                if (c == '"') { inQuotes = !inQuotes; cur.Append(c); }
                else if (c == ' ' && !inQuotes) { if (cur.Length > 0) { tokens.Add(cur.ToString()); cur.Clear(); } }
                else cur.Append(c);
            }
            if (cur.Length > 0) tokens.Add(cur.ToString());
            return tokens;
        }

        // Rules (ported from formatCmdLineMulti):
        //   token[0]            → executable (amber)
        //   token starts with - → flag (blue); if it contains no '=' AND the next token
        //                         doesn't start with '-', pair it with that token as its value
        //   anything else       → standalone (class name, unrecognised arg — warm grey)
        private static void BuildColorizedLines(string cmdLine, TextBlock target)
        {
            if (string.IsNullOrEmpty(cmdLine)) return;

            var tokens = TokenizeCmdLine(cmdLine);
            int i = 0;
            bool firstLine = true;
            while (i < tokens.Count)
            {
                if (!firstLine) target.Inlines.Add(new LineBreak());
                firstLine = false;

                string t = tokens[i];
                if (i == 0)
                {
                    target.Inlines.Add(Run(t, ExecutableColor));
                    i++;
                }
                else if (t.StartsWith("-"))
                {
                    var color = t.StartsWith("--") ? DoubleDashColor : SingleDashColor;
                    bool hasValue = !t.Contains('=') && i + 1 < tokens.Count && !tokens[i + 1].StartsWith("-");
                    if (hasValue)
                    {
                        target.Inlines.Add(Run("  " + t, color));
                        target.Inlines.Add(Run(" " + tokens[i + 1], ValueColor));
                        i += 2;
                    }
                    else
                    {
                        target.Inlines.Add(Run("  " + t, color));
                        i++;
                    }
                }
                else
                {
                    target.Inlines.Add(Run("  " + t, OtherColor));
                    i++;
                }
            }
        }

        private static Run Run(string text, Color color) => new(text) { Foreground = new SolidColorBrush(color) };
    }
}

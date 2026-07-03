using System;
using System.Drawing;
using System.IO;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Forms.Integration;
using ScintillaNET;

namespace DbxInstaller.Wizard
{
    // Wraps a Scintilla control — the same editing engine (and base setup) as DbxStarterClient's
    // FileEditor/LogViewer, which is itself the engine Notepad++ is built on — inside a WPF
    // WindowsFormsHost. Gives the wizard's inline editors line numbers, Notepad++-style
    // word-highlight-on-selection, and (unlike FileEditor, which uses the null lexer) real
    // syntax highlighting for the file types DbxTune actually uses: .conf/.properties (Scintilla's
    // built-in "props" lexer — key = value, # comments, [section] headers) and .bat (Scintilla's
    // built-in "batch" lexer).
    internal static class ScintillaHost
    {
        private const int IndicWordHL = 0;

        // lexerOverride bypasses the file-extension inference below — used for content that
        // isn't itself a real file with a matching extension (e.g. the DB-setup SQL instructions,
        // which come from a .txt embedded resource but should still highlight as SQL).
        public static (FrameworkElement Element, Scintilla Editor) Create(string text, string? filePath = null, bool readOnly = false, string? lexerOverride = null)
        {
            var sci = new Scintilla { Dock = System.Windows.Forms.DockStyle.Fill };

            sci.StyleResetDefault();
            sci.Styles[ScintillaNET.Style.Default].Font = "Consolas";
            sci.Styles[ScintillaNET.Style.Default].Size = 10;

            bool dark = InstallerTheme.IsDark;
            if (dark)
            {
                sci.Styles[ScintillaNET.Style.Default].BackColor = Color.FromArgb(20, 20, 20);
                sci.Styles[ScintillaNET.Style.Default].ForeColor = Color.FromArgb(200, 200, 200);
                sci.CaretForeColor = Color.White;
            }
            sci.StyleClearAll(); // propagates the Default style above to every style index — must run before per-lexer colors below

            string kind = lexerOverride ?? (filePath != null ? Path.GetExtension(filePath).ToLowerInvariant() : "");
            ConfigureLexer(sci, kind, dark);

            // Line-number margin, sized to fit the current line count (grown as needed below).
            sci.Margins[0].Type  = MarginType.Number;
            sci.Margins[0].Width = 40;
            sci.Margins[1].Width = 4; // thin spacer between margin and text
            sci.Margins[2].Width = 0;
            sci.Margins[3].Width = 0;

            sci.WrapMode            = WrapMode.None;
            sci.ScrollWidth         = 1;
            sci.ScrollWidthTracking = true;
            sci.TabWidth            = 4;
            sci.UseTabs             = false;

            // Word-highlight indicator: translucent blue box under the text (like Notepad++).
            sci.Indicators[IndicWordHL].Style        = IndicatorStyle.RoundBox;
            sci.Indicators[IndicWordHL].ForeColor    = Color.FromArgb(0, 120, 215);
            sci.Indicators[IndicWordHL].Alpha        = 40;
            sci.Indicators[IndicWordHL].OutlineAlpha = 100;
            sci.Indicators[IndicWordHL].Under        = true;

            sci.Text = text ?? "";
            sci.ReadOnly = readOnly;
            sci.EmptyUndoBuffer();
            sci.SetSavePoint(); // loading the initial text shouldn't count as a user edit — Modified starts false

            string lastWordHL = "";
            sci.UpdateUI += (_, e) =>
            {
                if (!e.Change.HasFlag(UpdateChange.Selection)) return;
                string word = sci.SelectedText;
                if (word == lastWordHL) return;
                lastWordHL = word;

                sci.IndicatorCurrent = IndicWordHL;
                sci.IndicatorClearRange(0, sci.TextLength);
                if (string.IsNullOrEmpty(word) || word.IndexOfAny(new[] { ' ', '\t', '\r', '\n' }) >= 0) return;

                sci.SearchFlags = SearchFlags.MatchCase;
                sci.TargetStart = 0;
                sci.TargetEnd   = sci.TextLength;
                int pos;
                while ((pos = sci.SearchInTarget(word)) >= 0)
                {
                    sci.IndicatorFillRange(pos, word.Length);
                    sci.TargetStart = pos + word.Length;
                    sci.TargetEnd   = sci.TextLength;
                    if (sci.TargetStart >= sci.TargetEnd) break;
                }
            };

            var host = new WindowsFormsHost { Child = sci };
            return (host, sci);
        }

        // Standard Save / Reload / status row for an inline file editor. An unsaved edit (dirty
        // buffer) is reported via onDirtyChanged so the caller can disable its Next control while
        // it's true — the two ways out of "dirty" are Save (write to disk) or Reload (discard the
        // edit and re-read the file from disk), both of which clear it.
        public static UIElement BuildSaveBar(Scintilla sci, string path, Action<bool> onDirtyChanged,
            Action? onSaved = null, bool includeExternalButton = false)
        {
            var btnSave   = new Button { Content = "Save", Margin = new Thickness(0, 0, 8, 0), IsEnabled = false };
            var btnReload = new Button { Content = "Reload", Margin = new Thickness(0, 0, 8, 0), IsEnabled = false };
            var lblStatus = new TextBlock { VerticalAlignment = VerticalAlignment.Center, TextWrapping = TextWrapping.Wrap };

            void SetDirty(bool dirty)
            {
                btnSave.IsEnabled = dirty;
                btnReload.IsEnabled = dirty;
                lblStatus.Text = dirty ? "Unsaved changes — Save or Reload before continuing." : "";
                lblStatus.SetResourceReference(TextBlock.ForegroundProperty, dirty ? "OrangeBrush" : "FgBrush");
                onDirtyChanged(dirty);
            }

            sci.SavePointLeft    += (_, _) => SetDirty(true);
            sci.SavePointReached += (_, _) => SetDirty(false);

            btnSave.Click += (_, _) =>
            {
                File.WriteAllText(path, sci.Text, System.Text.Encoding.UTF8);
                sci.SetSavePoint();
                lblStatus.Text = "Saved.";
                lblStatus.SetResourceReference(TextBlock.ForegroundProperty, "GreenBrush");
                onSaved?.Invoke();
            };
            btnReload.Click += (_, _) =>
            {
                sci.Text = File.Exists(path) ? File.ReadAllText(path) : sci.Text;
                sci.SetSavePoint();
                lblStatus.Text = "Reloaded from disk — unsaved changes discarded.";
                lblStatus.SetResourceReference(TextBlock.ForegroundProperty, "FgDimBrush");
            };

            var row = new StackPanel { Orientation = System.Windows.Controls.Orientation.Horizontal };
            row.Children.Add(btnSave);
            row.Children.Add(btnReload);
            if (includeExternalButton)
            {
                var btnExternal = new Button { Content = "Open in Notepad", Margin = new Thickness(0, 0, 8, 0) };
                btnExternal.Click += (_, _) =>
                {
                    try { System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo("notepad.exe", $"\"{path}\"") { UseShellExecute = false }); }
                    catch { /* best effort — Notepad should always be present on Windows */ }
                };
                row.Children.Add(btnExternal);
            }
            lblStatus.Margin = new Thickness(8, 0, 0, 0);
            row.Children.Add(lblStatus);
            return row;
        }

        private static void ConfigureLexer(Scintilla sci, string kind, bool dark)
        {
            // A small, readable palette per theme — not trying to match any specific editor's
            // exact colors, just distinct enough to make key/value/comment structure scannable.
            Color comment = dark ? Color.FromArgb(106, 153, 85)  : Color.FromArgb(0, 128, 0);
            Color keyword = dark ? Color.FromArgb(86, 156, 214)  : Color.FromArgb(0, 0, 255);
            Color value   = dark ? Color.FromArgb(206, 145, 120) : Color.FromArgb(163, 21, 21);
            Color heading = dark ? Color.FromArgb(78, 201, 176)  : Color.FromArgb(38, 127, 153);
            Color ident   = dark ? Color.FromArgb(156, 220, 254) : Color.FromArgb(43, 106, 208);
            Color str     = dark ? Color.FromArgb(214, 157, 133) : Color.FromArgb(163, 21, 21);
            Color number  = dark ? Color.FromArgb(181, 206, 168) : Color.FromArgb(9, 134, 88);

            switch (kind)
            {
                case ".conf":
                case ".properties":
                case ".ini":
                case "props":
                    sci.LexerName = "props";
                    sci.Styles[ScintillaNET.Style.Properties.Comment].ForeColor    = comment;
                    sci.Styles[ScintillaNET.Style.Properties.Section].ForeColor    = heading;
                    sci.Styles[ScintillaNET.Style.Properties.Key].ForeColor        = keyword;
                    sci.Styles[ScintillaNET.Style.Properties.Assignment].ForeColor = value;
                    sci.Styles[ScintillaNET.Style.Properties.DefVal].ForeColor     = value;
                    break;

                case ".bat":
                case ".cmd":
                    sci.LexerName = "batch";
                    sci.SetKeywords(0,
                        "call cd chdir cls color copy date del dir echo else endlocal erase exist exit for goto " +
                        "if md mkdir move pause popd prompt pushd rem ren rename rd rmdir set setlocal shift " +
                        "start time title type ver verify vol xcopy net sc");
                    sci.Styles[ScintillaNET.Style.Batch.Comment].ForeColor    = comment;
                    sci.Styles[ScintillaNET.Style.Batch.Word].ForeColor       = keyword;
                    sci.Styles[ScintillaNET.Style.Batch.Command].ForeColor    = keyword;
                    sci.Styles[ScintillaNET.Style.Batch.Label].ForeColor      = heading;
                    sci.Styles[ScintillaNET.Style.Batch.Identifier].ForeColor = ident;
                    sci.Styles[ScintillaNET.Style.Batch.Hide].ForeColor       = comment;
                    break;

                case ".sql":
                case "sql":
                    sci.LexerName = "sql";
                    sci.SetKeywords(0,
                        "select insert update delete create alter drop grant revoke from where and or not null " +
                        "into values set join inner left right outer on group by order having as distinct exec " +
                        "execute procedure function view index primary key foreign references default check " +
                        "unique constraint use database schema role user login password with if exists begin end " +
                        "declare go table trigger case when then else union all top identity");
                    sci.Styles[ScintillaNET.Style.Sql.Comment].ForeColor     = comment;
                    sci.Styles[ScintillaNET.Style.Sql.CommentLine].ForeColor = comment;
                    sci.Styles[ScintillaNET.Style.Sql.CommentDoc].ForeColor  = comment;
                    sci.Styles[ScintillaNET.Style.Sql.Word].ForeColor        = keyword;
                    sci.Styles[ScintillaNET.Style.Sql.String].ForeColor     = str;
                    sci.Styles[ScintillaNET.Style.Sql.Character].ForeColor  = str;
                    sci.Styles[ScintillaNET.Style.Sql.Number].ForeColor     = number;
                    sci.Styles[ScintillaNET.Style.Sql.Identifier].ForeColor = ident;
                    break;

                default:
                    sci.LexerName = "null";
                    break;
            }
        }
    }
}

using System;
using System.Drawing;
using System.IO;
using System.Text;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Forms.Integration;
using System.Windows.Input;
using System.Windows.Threading;
using ScintillaNET;

namespace DbxStarterClient
{
    public class LogViewer : Window
    {
        // ── controls ──────────────────────────────────────────────────────────
        private readonly Scintilla _sci;
        private readonly Button    _btnGoToEnd;
        private readonly Button    _btnRefresh;
        private readonly Button    _btnClose;
        private readonly Button    _btnOpenExternal;
        private readonly CheckBox  _chkAutoRefresh;
        private readonly TextBlock _lblStatus;
        private readonly TextBox   _txtSearch;
        private readonly Button    _btnFindNext;
        private readonly TextBlock _lblMatches;
        private readonly DispatcherTimer _timer;

        // ── file state ────────────────────────────────────────────────────────
        private readonly string _logFilePath;
        private readonly int    _tailMode;
        private long            _fileByteOffset = 0;   // next unread byte in the file

        // ── search state ──────────────────────────────────────────────────────
        private int _searchStart    = -1;   // byte offset of last match; -1 = no previous match
        private int _lineNumWidth   =  0;   // cached digit count; avoids redundant margin resets

        // ── custom style indices (0-31 are user-definable in Scintilla) ───────
        private const int StyDefault = 0;   // normal text
        private const int StyError   = 1;   // [ERROR]  — red
        private const int StyWarning = 2;   // [WARNI]  — amber
        private const int StyDebug   = 3;   // [DEBUG]  — grey
        private const int StyVerbose = 4;   // [VERBO]  — dark grey
        private const int StyFatal   = 5;   // [FATAL]  — magenta

        // ── indicator index for search highlights ─────────────────────────────
        private const int IndicSearch = 0;

        // ── indicator index for word highlights (Notepad++-style double-click) ──
        private const int IndicWordHL = 1;

        // ── last highlighted word (avoids redundant redraws on cursor moves) ───
        private string _lastWordHL = "";

        // ─────────────────────────────────────────────────────────────────────

        public LogViewer(string serverName, string logFilePath, bool autoRefresh, int tailMode)
        {
            _logFilePath = logFilePath;
            _tailMode    = tailMode;

            Width       = 1100;
            Height      = 740;
            MinWidth    = 680;
            MinHeight   = 420;
            WindowStartupLocation = WindowStartupLocation.CenterOwner;
            Title = $"Log Viewer — {serverName} — {logFilePath}";

            (_sci, _btnGoToEnd, _btnRefresh, _btnClose, _btnOpenExternal,
             _chkAutoRefresh, _lblStatus, _txtSearch, _btnFindNext, _lblMatches) = BuildLayout();

            _timer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(1) };
            _timer.Tick += (_, _) => LoadIncremental();

            _chkAutoRefresh.IsChecked = autoRefresh;   // fires Checked/Unchecked → starts timer if true

            LoadInitial();

            Closing += (_, _) => _timer.Stop();
        }

        // ── layout ────────────────────────────────────────────────────────────

        private (Scintilla, Button, Button, Button, Button, CheckBox, TextBlock, TextBox, Button, TextBlock) BuildLayout()
        {
            // ── Scintilla control ─────────────────────────────────────────────
            var sci = new Scintilla
            {
                Dock                = System.Windows.Forms.DockStyle.Fill,
                WrapMode            = WrapMode.None,
                ScrollWidthTracking = true,
                ScrollWidth         = 1,
            };

            // Dark background: set Default then propagate to all 256 style slots
            sci.StyleResetDefault();
            sci.Styles[ScintillaNET.Style.Default].BackColor = Color.FromArgb(20, 20, 20);
            sci.Styles[ScintillaNET.Style.Default].ForeColor = Color.FromArgb(200, 200, 200);
            sci.Styles[ScintillaNET.Style.Default].Font      = "Consolas";
            sci.Styles[ScintillaNET.Style.Default].Size      = 10;
            sci.StyleClearAll();   // copy Default → all other styles

            // Log-level colours (all inherit the dark background from Default)
            sci.Styles[StyDefault].ForeColor = Color.FromArgb(200, 200, 200);
            sci.Styles[StyError  ].ForeColor = Color.FromArgb(255, 100, 100);
            sci.Styles[StyWarning].ForeColor = Color.FromArgb(255, 200,  80);
            sci.Styles[StyDebug  ].ForeColor = Color.FromArgb(150, 150, 150);
            sci.Styles[StyVerbose].ForeColor = Color.FromArgb(100, 100, 100);
            sci.Styles[StyFatal  ].ForeColor = Color.FromArgb(255,  80, 255);

            // Line-number gutter (margin 0)
            sci.Styles[ScintillaNET.Style.LineNumber].BackColor = Color.FromArgb(30, 30, 30);
            sci.Styles[ScintillaNET.Style.LineNumber].ForeColor = Color.FromArgb(90, 90, 90);
            sci.Margins[0].Type  = MarginType.Number;
            sci.Margins[0].Width = 50;   // adjusted dynamically by UpdateLineNumberMargin()
            sci.Margins[1].Width = 0;
            sci.Margins[2].Width = 0;

            // Disable auto-lexing — we apply styles manually after each append.
            // "null" is the no-op lexer in Lexilla; tolerate API differences gracefully.
            try { sci.LexerName = "null"; } catch { /* non-fatal; manual styles still work */ }

            // Search-highlight indicator: translucent yellow box drawn under the text
            sci.Indicators[IndicSearch].Style        = IndicatorStyle.StraightBox;
            sci.Indicators[IndicSearch].Under        = true;
            sci.Indicators[IndicSearch].ForeColor    = Color.Yellow;
            sci.Indicators[IndicSearch].OutlineAlpha = 200;
            sci.Indicators[IndicSearch].Alpha        = 50;

            // Word-highlight indicator (Notepad++-style: lights up all occurrences of the
            // selected word).  Uses a translucent blue rounded box drawn under the text.
            sci.Indicators[IndicWordHL].Style        = IndicatorStyle.RoundBox;
            sci.Indicators[IndicWordHL].ForeColor    = Color.FromArgb(100, 180, 255);
            sci.Indicators[IndicWordHL].Alpha        = 60;
            sci.Indicators[IndicWordHL].OutlineAlpha = 140;
            sci.Indicators[IndicWordHL].Under        = true;

            // Read-only: user cannot type; we programmatically lift it for appends
            sci.ReadOnly = true;

            // Widen the line-number gutter automatically as line count grows
            sci.TextChanged += (_, _) => UpdateLineNumberMargin();

            // Highlight all occurrences of the selected word (Notepad++-style).
            // UpdateUI fires on every UI change; the Selection flag gates it to selection moves.
            sci.UpdateUI += (_, e) =>
            {
                if (e.Change.HasFlag(UpdateChange.Selection))
                    OnWordHighlight();
            };

            var editorHost = new WindowsFormsHost { Child = sci };

            // ── search bar (top) ──────────────────────────────────────────────
            var lblFind = new Label { Content = "Find:", VerticalAlignment = VerticalAlignment.Center };

            var txtSearch = new TextBox { Width = 270, Margin = new Thickness(4, 0, 8, 0), VerticalAlignment = VerticalAlignment.Center };
            txtSearch.TextChanged += (_, _) => { _searchStart = -1; HighlightAll(); };
            txtSearch.PreviewKeyDown += (_, e) => { if (e.Key == Key.Enter) FindNext(); };

            var btnFindNext = new Button { Content = "Find Next", Margin = new Thickness(0, 0, 8, 0) };
            btnFindNext.Click += (_, _) => FindNext();

            var lblMatches = new TextBlock { VerticalAlignment = VerticalAlignment.Center };
            lblMatches.SetResourceReference(TextBlock.ForegroundProperty, "FgDimBrush");

            var searchPanel = new StackPanel
            {
                Orientation = Orientation.Horizontal,
                Margin = new Thickness(6, 4, 6, 4),
            };
            searchPanel.Children.Add(lblFind);
            searchPanel.Children.Add(txtSearch);
            searchPanel.Children.Add(btnFindNext);
            searchPanel.Children.Add(lblMatches);
            DockPanel.SetDock(searchPanel, Dock.Top);

            // ── status / button bar (bottom) ────────────────────────────────────
            var chkAutoRefresh = new CheckBox { Content = "Auto-refresh", VerticalAlignment = VerticalAlignment.Center };
            chkAutoRefresh.Checked   += (_, _) => _timer.Start();
            chkAutoRefresh.Unchecked += (_, _) => _timer.Stop();

            var lblStatus = new TextBlock { VerticalAlignment = VerticalAlignment.Center, Margin = new Thickness(12, 0, 0, 0) };
            lblStatus.SetResourceReference(TextBlock.ForegroundProperty, "FgDimBrush");

            var btnClose = new Button { Content = "Close", Margin = new Thickness(8, 0, 0, 0) };
            btnClose.Click += (_, _) => Close();

            var btnRefresh = new Button { Content = "Refresh", Margin = new Thickness(8, 0, 0, 0) };
            btnRefresh.Click += (_, _) => LoadInitial();

            var btnGoToEnd = new Button { Content = "Go to End", Margin = new Thickness(8, 0, 0, 0) };
            btnGoToEnd.Click += (_, _) => GotoEnd();

            var btnOpenExternal = new Button { Content = "Open in Editor", Margin = new Thickness(8, 0, 0, 0) };
            btnOpenExternal.Click += (_, _) => OpenInExternalEditor();

            var rightButtons = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Right };
            rightButtons.Children.Add(btnGoToEnd);
            rightButtons.Children.Add(btnRefresh);
            rightButtons.Children.Add(btnOpenExternal);
            rightButtons.Children.Add(btnClose);

            var buttonBar = new DockPanel { Margin = new Thickness(6, 4, 6, 4) };
            DockPanel.SetDock(chkAutoRefresh, Dock.Left);
            DockPanel.SetDock(lblStatus, Dock.Left);
            buttonBar.Children.Add(chkAutoRefresh);
            buttonBar.Children.Add(lblStatus);
            buttonBar.Children.Add(rightButtons);   // last, fills remaining space, right-aligned content
            DockPanel.SetDock(buttonBar, Dock.Bottom);

            var root = new DockPanel();
            root.Children.Add(searchPanel);
            root.Children.Add(buttonBar);
            root.Children.Add(editorHost);   // last child fills remaining space
            Content = root;

            return (sci, btnGoToEnd, btnRefresh, btnClose, btnOpenExternal,
                    chkAutoRefresh, lblStatus, txtSearch, btnFindNext, lblMatches);
        }

        // ── file loading ──────────────────────────────────────────────────────

        // Full (re)load — reads the entire file, applies tail-mode trimming, resets state.
        // Called on first open and when the user clicks Refresh.
        public void LoadInitial()
        {
            if (!File.Exists(_logFilePath))
            {
                _lblStatus.Text = "File not found";
                return;
            }
            try
            {
                string content;
                using (var fs = new FileStream(_logFilePath, FileMode.Open,
                                               FileAccess.Read, FileShare.ReadWrite))
                {
                    using var sr = new StreamReader(fs, Encoding.UTF8,
                                                    detectEncodingFromByteOrderMarks: true,
                                                    bufferSize: 65536, leaveOpen: true);
                    content = sr.ReadToEnd();
                    _fileByteOffset = fs.Position;   // always tracks the full file end
                }

                // Tail mode: keep only the last _tailMode lines
                if (_tailMode > 0 && content.Length > 0)
                {
                    int pos   = content.Length;
                    int found = 0;
                    while (found < _tailMode && pos > 0)
                    {
                        pos = content.LastIndexOf('\n', pos - 1);
                        found++;
                    }
                    if (pos > 0)
                        content = content[(pos + 1)..];
                }

                _sci.ReadOnly = false;
                _sci.ClearAll();
                _sci.AppendText(content);
                _sci.ReadOnly = true;

                StyleAll();
                UpdateLineNumberMargin();

                long kb = _fileByteOffset / 1024;
                _lblStatus.Text = $"{_sci.Lines.Count:N0} lines  ({kb:N0} KB)";
                GotoEnd();
            }
            catch (Exception ex)
            {
                _lblStatus.Text = $"Error: {ex.Message}";
            }
        }

        // Incremental load — reads only the bytes appended to the file since the last read.
        // O(new bytes) regardless of total file size.  Called every timer tick.
        private void LoadIncremental()
        {
            if (!File.Exists(_logFilePath))
            {
                _lblStatus.Text = "File not found";
                return;
            }
            try
            {
                using var fs = new FileStream(_logFilePath, FileMode.Open,
                                              FileAccess.Read, FileShare.ReadWrite);

                // File was truncated/rotated — start over
                if (fs.Length < _fileByteOffset)
                {
                    LoadInitial();
                    return;
                }

                if (fs.Length == _fileByteOffset) return;   // nothing new

                // Read only the new tail
                fs.Seek(_fileByteOffset, SeekOrigin.Begin);
                using var sr = new StreamReader(fs, Encoding.UTF8,
                                                detectEncodingFromByteOrderMarks: false,
                                                bufferSize: 65536, leaveOpen: true);
                string newContent = sr.ReadToEnd();
                _fileByteOffset = fs.Position;

                if (string.IsNullOrEmpty(newContent)) return;

                bool wasAtEnd = IsScrolledToEnd();

                int styleFrom = _sci.TextLength;   // byte offset before the append
                _sci.ReadOnly = false;
                _sci.AppendText(newContent);
                _sci.ReadOnly = true;

                StyleRange(styleFrom, _sci.TextLength);   // colour only the new lines

                long kb = _fileByteOffset / 1024;
                _lblStatus.Text = $"{_sci.Lines.Count:N0} lines  ({kb:N0} KB)  · {DateTime.Now:HH:mm:ss}";

                if (wasAtEnd) GotoEnd();
            }
            catch (Exception ex)
            {
                _lblStatus.Text = $"Read error: {ex.Message}";
                _timer.Stop();
                _chkAutoRefresh.IsChecked = false;
            }
        }

        // ── styling ───────────────────────────────────────────────────────────

        private void StyleAll() => StyleRange(0, _sci.TextLength);

        // Applies per-line log-level colouring for the byte range [startPos, endPos).
        // Uses StartStyling + SetStyling which is Scintilla's efficient batch-style API.
        private void StyleRange(int startPos, int endPos)
        {
            if (startPos >= endPos) return;

            int firstLine = _sci.LineFromPosition(startPos);
            int lastLine  = _sci.LineFromPosition(Math.Max(0, endPos - 1));

            // StartStyling must begin at the line boundary, not mid-line
            _sci.StartStyling(_sci.Lines[firstLine].Position);

            for (int i = firstLine; i <= lastLine; i++)
            {
                var line = _sci.Lines[i];
                // line.Length is the byte count (including EOL) — required by SetStyling
                _sci.SetStyling(line.Length, GetStyleIndex(line.Text));
            }
        }

        private static int GetStyleIndex(string line)
        {
            // Serilog format: [{Level:u5}] → [FATAL] [ERROR] [WARNI] [INFOR] [DEBUG] [VERBO]
            if (line.Contains("[FATAL]"))                              return StyFatal;
            if (line.Contains("[ERROR]"))                              return StyError;
            if (line.Contains("[WARNI]"))                              return StyWarning;
            if (line.Contains("[DEBUG]"))                              return StyDebug;
            if (line.Contains("[VERBO]"))                              return StyVerbose;
            // Fallback for other common log formats
            if (line.Contains(" ERROR ") || line.Contains("|ERROR|")) return StyError;
            if (line.Contains(" WARN ")  || line.Contains("|WARN|"))  return StyWarning;
            return StyDefault;
        }

        // ── line-number margin ────────────────────────────────────────────────

        private void UpdateLineNumberMargin()
        {
            int digits = Math.Max(1, _sci.Lines.Count.ToString().Length);
            if (digits == _lineNumWidth) return;   // no change — skip the API call
            _lineNumWidth = digits;
            const int pad = 6;
            _sci.Margins[0].Width =
                _sci.TextWidth(ScintillaNET.Style.LineNumber, new string('9', digits + 1)) + pad;
        }

        // ── search ────────────────────────────────────────────────────────────

        // Highlights all occurrences with the search indicator, updates match count.
        private void HighlightAll()
        {
            _sci.IndicatorCurrent = IndicSearch;
            _sci.IndicatorClearRange(0, _sci.TextLength);
            _lblMatches.Text = "";

            string term = _txtSearch.Text;
            if (string.IsNullOrEmpty(term)) return;

            _sci.SearchFlags = SearchFlags.None;
            _sci.TargetStart = 0;
            _sci.TargetEnd   = _sci.TextLength;

            int count = 0, pos;
            while ((pos = _sci.SearchInTarget(term)) != -1)
            {
                _sci.IndicatorFillRange(pos, term.Length);
                count++;
                _sci.TargetStart = pos + term.Length;
                _sci.TargetEnd   = _sci.TextLength;
            }

            _lblMatches.Text = count == 0 ? "Not found"
                             : $"{count} match{(count == 1 ? "" : "es")}";
        }

        // Navigates to the next occurrence after the last match, wrapping at the end.
        private void FindNext()
        {
            string term = _txtSearch.Text;
            if (string.IsNullOrEmpty(term)) return;

            // Start one byte past the end of the last match (or from 0 on first call)
            int from = _searchStart < 0 ? 0 : _searchStart + term.Length;

            _sci.SearchFlags = SearchFlags.None;
            _sci.TargetStart = from;
            _sci.TargetEnd   = _sci.TextLength;
            int pos = _sci.SearchInTarget(term);

            if (pos == -1 && from > 0)
            {
                // Wrap around from the beginning
                _sci.TargetStart = 0;
                _sci.TargetEnd   = _sci.TextLength;
                pos = _sci.SearchInTarget(term);
            }

            if (pos != -1)
            {
                _sci.SetSelection(pos, pos + term.Length);
                _sci.ScrollCaret();
                _searchStart = pos;
            }
        }

        // ── helpers ───────────────────────────────────────────────────────────

        private void GotoEnd()
        {
            _sci.GotoPosition(_sci.TextLength);
        }

        // Returns true if the viewport is showing (or very close to) the last line.
        // Used to decide whether to auto-scroll after an incremental append.
        private bool IsScrolledToEnd()
        {
            return _sci.FirstVisibleLine + _sci.LinesOnScreen >= _sci.Lines.Count - 2;
        }

        private void OpenInExternalEditor()
        {
            try
            {
                System.Diagnostics.Process.Start(
                    new System.Diagnostics.ProcessStartInfo(_logFilePath)
                    { UseShellExecute = true });
            }
            catch (Exception ex)
            {
                MessageBox.Show(this, $"Cannot open file:\n{ex.Message}", "Error",
                    MessageBoxButton.OK, MessageBoxImage.Warning);
            }
        }

        // ── word highlight (Notepad++-style) ──────────────────────────────────

        private void OnWordHighlight()
        {
            string word = _sci.SelectedText;
            if (word == _lastWordHL) return;   // selection didn't change — skip redraw
            _lastWordHL = word;

            _sci.IndicatorCurrent = IndicWordHL;
            _sci.IndicatorClearRange(0, _sci.TextLength);

            // Only highlight non-empty, single-token selections (no spaces / newlines)
            if (string.IsNullOrEmpty(word) || word.IndexOfAny(new[] { ' ', '\t', '\r', '\n' }) >= 0)
                return;

            _sci.SearchFlags = SearchFlags.MatchCase;
            _sci.TargetStart = 0;
            _sci.TargetEnd   = _sci.TextLength;

            int pos;
            while ((pos = _sci.SearchInTarget(word)) >= 0)
            {
                _sci.IndicatorFillRange(pos, word.Length);
                _sci.TargetStart = pos + word.Length;
                _sci.TargetEnd   = _sci.TextLength;
                if (_sci.TargetStart >= _sci.TargetEnd) break;
            }
        }
    }
}

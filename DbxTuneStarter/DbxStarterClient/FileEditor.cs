using System;
using System.IO;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using ScintillaNET;

namespace DbxStarterClient
{
    public class FileEditor : Window
    {
        private readonly string _filePath;
        private readonly Scintilla _sci;

        private readonly Border   _findPanel;
        private readonly TextBox  _txtFind;

        private int _searchPos = -1;  // last match start position; -1 = start from top

        public FileEditor(string filePath)
        {
            _filePath = filePath;

            Title       = "File Editor";
            Width       = 900;
            Height      = 650;
            MinWidth    = 600;
            MinHeight   = 400;
            WindowStartupLocation = WindowStartupLocation.CenterOwner;

            string text = File.Exists(_filePath) ? File.ReadAllText(_filePath) : "";
            var (editorElement, sci) = ScintillaHost.Create(text, _filePath);
            _sci = sci;

            // ── Find bar (hidden initially, docked to top) ─────────────────────
            _txtFind = new TextBox { Width = 240, Margin = new Thickness(0, 0, 4, 0), VerticalAlignment = VerticalAlignment.Center };
            _txtFind.TextChanged += (_, _) => _searchPos = -1;
            _txtFind.PreviewKeyDown += (_, e) =>
            {
                if (e.Key == Key.Enter)  { e.Handled = true; FindNext(); }
                if (e.Key == Key.Escape) { e.Handled = true; HideFindPanel(); }
            };

            var btnFindNext = new Button { Content = "Find Next", Margin = new Thickness(0, 0, 4, 0) };
            btnFindNext.Click += (_, _) => FindNext();

            var btnFindClose = new Button { Content = "✕", MinWidth = 0, Padding = new Thickness(8, 4, 8, 4) };
            btnFindClose.Click += (_, _) => HideFindPanel();

            var findRow = new StackPanel { Orientation = Orientation.Horizontal, Margin = new Thickness(6) };
            findRow.Children.Add(new Label { Content = "Find:", Margin = new Thickness(0, 0, 4, 0) });
            findRow.Children.Add(_txtFind);
            findRow.Children.Add(btnFindNext);
            findRow.Children.Add(btnFindClose);

            _findPanel = new Border { Child = findRow, Visibility = Visibility.Collapsed };
            _findPanel.SetResourceReference(Border.BackgroundProperty, "Bg2Brush");
            _findPanel.BorderThickness = new Thickness(0, 0, 0, 1);
            _findPanel.SetResourceReference(Border.BorderBrushProperty, "Bg3Brush");
            DockPanel.SetDock(_findPanel, Dock.Top);

            // ── Button panel (Save / Cancel) docked to bottom ──────────────────
            var btnSave = new Button { Content = "Save", Margin = new Thickness(0, 0, 8, 0) };
            btnSave.Click += (_, _) => { SaveFile(); Close(); };

            var btnCancel = new Button { Content = "Cancel" };
            btnCancel.Click += (_, _) => Close();

            var buttonRow = new StackPanel
            {
                Orientation = Orientation.Horizontal,
                HorizontalAlignment = HorizontalAlignment.Right,
                Margin = new Thickness(8),
            };
            buttonRow.Children.Add(btnSave);
            buttonRow.Children.Add(btnCancel);

            var buttonPanel = new Border { Child = buttonRow };
            buttonPanel.SetResourceReference(Border.BorderBrushProperty, "Bg3Brush");
            buttonPanel.BorderThickness = new Thickness(0, 1, 0, 0);
            DockPanel.SetDock(buttonPanel, Dock.Bottom);

            var root = new DockPanel();
            root.Children.Add(_findPanel);
            root.Children.Add(buttonPanel);
            root.Children.Add(editorElement);   // last child fills remaining space
            Content = root;

            _sci.SavePointLeft    += (_, _) => UpdateTitle(dirty: true);
            _sci.SavePointReached += (_, _) => UpdateTitle(dirty: false);
            UpdateTitle(dirty: false);

            PreviewKeyDown += FileEditor_PreviewKeyDown;
            Closing        += FileEditor_Closing;
        }

        // ── keyboard ─────────────────────────────────────────────────────────

        private void FileEditor_PreviewKeyDown(object sender, KeyEventArgs e)
        {
            if (Keyboard.Modifiers == ModifierKeys.Control && e.Key == Key.F)
            {
                e.Handled = true;
                ShowFindPanel();
            }
            else if (Keyboard.Modifiers == ModifierKeys.Control && e.Key == Key.S)
            {
                e.Handled = true;
                SaveFile();
            }
        }

        // ── Find bar ─────────────────────────────────────────────────────────

        private void ShowFindPanel()
        {
            _findPanel.Visibility = Visibility.Visible;
            _txtFind.Focus();
            _txtFind.SelectAll();
        }

        private void HideFindPanel()
        {
            _findPanel.Visibility = Visibility.Collapsed;
            _sci.Focus();
        }

        private void FindNext()
        {
            string term = _txtFind.Text;
            if (string.IsNullOrEmpty(term)) return;

            int start = _searchPos < 0 ? 0 : _searchPos + term.Length;
            if (start >= _sci.TextLength) start = 0;

            _sci.SearchFlags = SearchFlags.None;
            _sci.TargetStart = start;
            _sci.TargetEnd   = _sci.TextLength;
            int pos = _sci.SearchInTarget(term);

            // Wrap around if not found
            if (pos == -1 && start > 0)
            {
                _sci.TargetStart = 0;
                _sci.TargetEnd   = _sci.TextLength;
                pos = _sci.SearchInTarget(term);
            }

            if (pos >= 0)
            {
                _searchPos           = pos;
                _sci.AnchorPosition  = pos;
                _sci.CurrentPosition = pos + term.Length;
                _sci.ScrollCaret();
            }
            else
            {
                _searchPos = -1;
                MessageBox.Show(this, $"'{term}' not found.", "Find",
                    MessageBoxButton.OK, MessageBoxImage.Information);
            }
        }

        // ── save / close ─────────────────────────────────────────────────────

        private void UpdateTitle(bool dirty)
        {
            Title = $"File Editor — {Path.GetFileName(_filePath)}{(dirty ? " *" : "")}";
        }

        private void SaveFile()
        {
            File.WriteAllText(_filePath, _sci.Text, System.Text.Encoding.UTF8);
            _sci.SetSavePoint();
        }

        private void FileEditor_Closing(object? sender, System.ComponentModel.CancelEventArgs e)
        {
            if (_sci.Modified)
            {
                var answer = MessageBox.Show(this,
                    "You have unsaved changes. Save before closing?",
                    "Unsaved Changes",
                    MessageBoxButton.YesNoCancel,
                    MessageBoxImage.Warning);

                if (answer == MessageBoxResult.Yes)
                    SaveFile();
                else if (answer == MessageBoxResult.Cancel)
                {
                    e.Cancel = true;
                }
            }
        }
    }
}

using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;

namespace DbxInstaller.Wizard
{
    // Base window for every wizard flow (install / upgrade / uninstall). Hosts a left-hand
    // step rail, a content area for the current WizardPage, and a pinned Back/Next/Cancel bar.
    //
    // Pages is intentionally mutable after construction: MainWizardWindow starts with just the
    // Welcome page, then appends the rest of the sequence once the user picks Install/Upgrade/Remove
    // (see OnLeavingPage). This is why navigation uses a history stack of visited indices rather
    // than a simple "index - 1" — future pages aren't known until the mode is chosen, and some
    // pages (e.g. DBMS Configuration) are skipped conditionally.
    internal abstract class WizardWindow : Window
    {
        protected readonly WizardContext Context;
        protected readonly List<WizardPage> Pages = new();

        private readonly List<int> _history = new();
        private int _currentIndex = -1;

        private readonly StackPanel _railPanel;
        private readonly ContentControl _contentHost;
        private readonly TextBlock _errorBanner;
        private readonly Button _btnBack, _btnNext, _btnCancel;

        protected WizardWindow(InstallConfig config)
        {
            Context = new WizardContext(config);

            Title = $"DbxTune Setup v{DbxStarterCommon.Version.VersionString}";
            // Larger by default, but capped to the current screen's work area (minus a small
            // margin) so it still fits on smaller/lower-resolution displays.
            Width = Math.Min(1300, SystemParameters.WorkArea.Width - 40);
            Height = Math.Min(900, SystemParameters.WorkArea.Height - 40);
            MinWidth = 760;
            MinHeight = 520;
            WindowStartupLocation = WindowStartupLocation.CenterScreen;

            var root = new Grid();
            root.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(220) });
            root.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            Content = root;

            // ── left rail ──────────────────────────────────────────────────────
            var railBorder = new Border();
            railBorder.SetResourceReference(Border.BackgroundProperty, "Bg2Brush");
            railBorder.BorderThickness = new Thickness(0, 0, 1, 0);
            railBorder.SetResourceReference(Border.BorderBrushProperty, "Bg3Brush");
            _railPanel = new StackPanel { Margin = new Thickness(16) };
            railBorder.Child = new ScrollViewer { Content = _railPanel, VerticalScrollBarVisibility = ScrollBarVisibility.Auto };
            Grid.SetColumn(railBorder, 0);
            root.Children.Add(railBorder);

            // ── right side: content + error banner + button bar ────────────────
            var rightPanel = new Grid();
            rightPanel.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
            rightPanel.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            rightPanel.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            Grid.SetColumn(rightPanel, 1);
            root.Children.Add(rightPanel);

            _contentHost = new ContentControl { Margin = new Thickness(28) };
            Grid.SetRow(_contentHost, 0);
            rightPanel.Children.Add(_contentHost);

            _errorBanner = new TextBlock
            {
                Margin = new Thickness(28, 0, 28, 8),
                TextWrapping = TextWrapping.Wrap,
                Visibility = Visibility.Collapsed,
            };
            _errorBanner.SetResourceReference(TextBlock.ForegroundProperty, "RedBrush");
            Grid.SetRow(_errorBanner, 1);
            rightPanel.Children.Add(_errorBanner);

            var buttonBar = new Border { Padding = new Thickness(20, 12, 20, 12) };
            buttonBar.SetResourceReference(Border.BorderBrushProperty, "Bg3Brush");
            buttonBar.BorderThickness = new Thickness(0, 1, 0, 0);
            var buttonRow = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Right };
            _btnCancel = new Button { Content = "Cancel", Margin = new Thickness(0, 0, 8, 0) };
            _btnBack   = new Button { Content = "< Back", Margin = new Thickness(0, 0, 8, 0) };
            _btnNext   = new Button { Content = "Next >", Style = (Style)FindResource("PrimaryButton") };
            _btnCancel.Click += (_, _) => Close();
            _btnBack.Click   += (_, _) => OnBackClicked();
            _btnNext.Click   += async (_, _) => await OnNextClickedAsync();
            buttonRow.Children.Add(_btnCancel);
            buttonRow.Children.Add(_btnBack);
            buttonRow.Children.Add(_btnNext);
            buttonBar.Child = buttonRow;
            Grid.SetRow(buttonBar, 2);
            rightPanel.Children.Add(buttonBar);

            Closing += WizardWindow_Closing;
        }

        // Call once from the derived constructor after the initial page (Welcome) has been added to Pages.
        protected void Start()
        {
            NavigateTo(0, addHistory: false);
        }

        // Hook for subclasses: called right after a page's OnLeave, before computing the next index.
        // MainWizardWindow uses this to append the Install/Upgrade/Uninstall page group once the
        // Welcome page has set ctx.Config.Mode.
        protected virtual void OnLeavingPage(WizardPage page) { }

        private async System.Threading.Tasks.Task OnNextClickedAsync()
        {
            var current = Pages[_currentIndex];

            if (current.IsFinishPage) { Close(); return; }

            _btnNext.IsEnabled = false;
            try
            {
                var result = await current.ValidateBeforeNextAsync(Context);
                if (!result.IsValid)
                {
                    ShowError(result.Message);
                    return;
                }
                ShowError(null);

                current.OnLeave(Context);
                OnLeavingPage(current);

                int next = _currentIndex + 1;
                if (next >= Pages.Count) { Close(); return; }

                NavigateTo(next, addHistory: true);
            }
            finally
            {
                _btnNext.IsEnabled = true;
            }
        }

        private void OnBackClicked()
        {
            if (_history.Count == 0) return;
            Pages[_currentIndex].OnLeave(Context);
            int prev = _history[^1];
            _history.RemoveAt(_history.Count - 1);
            NavigateTo(prev, addHistory: false);
        }

        private void NavigateTo(int index, bool addHistory)
        {
            if (addHistory && _currentIndex >= 0) _history.Add(_currentIndex);
            if (_currentIndex >= 0) Pages[_currentIndex].NextEnabledChanged -= Page_NextEnabledChanged;

            _currentIndex = index;
            var page = Pages[index];
            page.NextEnabledChanged += Page_NextEnabledChanged;

            ShowError(null);
            _contentHost.Content = page;
            page.OnEnter(Context);
            RefreshChrome();
        }

        private void Page_NextEnabledChanged(object? sender, EventArgs e) => RefreshChrome();

        private void RefreshChrome()
        {
            var page = Pages[_currentIndex];
            _btnBack.IsEnabled   = _history.Count > 0 && page.CanGoBack(Context);
            _btnCancel.IsEnabled = page.CanCancel(Context);
            _btnNext.IsEnabled   = page.NextEnabled;
            _btnNext.Content     = page.IsFinishPage ? "Finish" : "Next >";
            RebuildRail();
        }

        private void RebuildRail()
        {
            _railPanel.Children.Clear();
            for (int i = 0; i < Pages.Count; i++)
            {
                var tb = new TextBlock
                {
                    Text = Pages[i].StepTitle,
                    Margin = new Thickness(0, 0, 0, 10),
                    TextWrapping = TextWrapping.Wrap,
                };
                if (i == _currentIndex)
                {
                    tb.FontWeight = FontWeights.SemiBold;
                    tb.SetResourceReference(TextBlock.ForegroundProperty, "FgHdrBrush");
                    tb.Text = "> " + tb.Text;
                }
                else if (i < _currentIndex)
                {
                    tb.SetResourceReference(TextBlock.ForegroundProperty, "GreenBrush");
                    tb.Text = "✓ " + tb.Text;
                }
                else
                {
                    tb.SetResourceReference(TextBlock.ForegroundProperty, "FgDimBrush");
                }
                _railPanel.Children.Add(tb);
            }
        }

        private void ShowError(string? message)
        {
            _errorBanner.Text = message ?? "";
            _errorBanner.Visibility = string.IsNullOrEmpty(message) ? Visibility.Collapsed : Visibility.Visible;
        }

        private void WizardWindow_Closing(object? sender, CancelEventArgs e)
        {
            if (_currentIndex < 0) return;
            if (Pages[_currentIndex] is IBusyPage { IsRunning: true })
            {
                e.Cancel = true;
                MessageBox.Show(this, "Please wait for the current operation to finish before closing.",
                    "DbxTune Setup", MessageBoxButton.OK, MessageBoxImage.Warning);
            }
        }
    }
}

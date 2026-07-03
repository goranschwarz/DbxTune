using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Runtime.Serialization.Json;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Interop;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;
using System.Windows.Threading;
using System.Runtime.InteropServices;
using Microsoft.Win32;
using Path = System.IO.Path;

namespace DbxStarterClient
{
    public class MainWindow : Window
    {
        private const int NORMAL_REFRESH_RATE_MS = 60_000;

        private readonly DispatcherTimer _refreshTimer;
        private readonly ServiceClient   _serviceClient;
        private readonly System.Windows.Forms.NotifyIcon _trayIcon = new();
        private System.Windows.Forms.ToolStripMenuItem    _darkModeItem = null!;
        private bool _isConnected;

        private readonly ObservableCollection<ServerRow> _rows = new();

        private DataGrid    _grid          = null!;
        private ContextMenu _gridContextMenu = null!;
        private TextBlock   _statusLabel   = null!;
        private TextBlock   _serverSummaryLabel = null!;
        private TextBlock   _serverListFileText = null!;
        private ComboBox    _logLevelCombo = null!;

        public MainWindow()
        {
            Title     = "DbxStarter Client";
            var workArea = SystemParameters.WorkArea;
            double defaultWidth  = Math.Max(1150, Math.Min(workArea.Width - 100, 2200));
            double defaultHeight = Math.Max(550, Math.Min(workArea.Height - 100, 900));
            MinWidth  = 700;
            MinHeight = 400;
            Icon      = LoadImageSource("dbxtune_central_starter.ico");
            ApplySavedWindowBounds(defaultWidth, defaultHeight);

            BuildLayout();

            _refreshTimer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(NORMAL_REFRESH_RATE_MS) };
            _refreshTimer.Tick += async (s, e) => await RefreshServerStatusAsync(true);

            _serviceClient = new ServiceClient();

            SetupTrayIcon();

            PreviewKeyDown    += MainWindow_PreviewKeyDown;
            Loaded            += MainWindow_Loaded;
            StateChanged      += MainWindow_StateChanged;
            Closing           += MainWindow_Closing;
            SourceInitialized += MainWindow_SourceInitialized;
        }

        // ── native window icon (small titlebar + big taskbar/Alt-Tab) ────────────
        //
        // WPF's Icon property wraps a single BitmapImage frame — Windows then scales that
        // one frame for every context that needs an icon, including the taskbar's larger
        // slot, which is why a 16x16-sourced icon looks blurry there. Sending WM_SETICON
        // directly with a correctly-sized System.Drawing.Icon for each of ICON_SMALL/ICON_BIG
        // lets each context use its own sharp frame from the combined multi-resolution .ico.

        private const int WM_SETICON  = 0x0080;
        private const int ICON_SMALL  = 0;
        private const int ICON_BIG    = 1;

        [DllImport("user32.dll")]
        private static extern IntPtr SendMessage(IntPtr hWnd, int msg, IntPtr wParam, IntPtr lParam);

        private void MainWindow_SourceInitialized(object? sender, EventArgs e)
        {
            var hwnd = new WindowInteropHelper(this).Handle;
            var small = LoadWinFormsIcon("dbxtune_central_starter.ico", 16);
            var big   = LoadWinFormsIcon("dbxtune_central_starter.ico", 32);
            SendMessage(hwnd, WM_SETICON, (IntPtr)ICON_SMALL, small.Handle);
            SendMessage(hwnd, WM_SETICON, (IntPtr)ICON_BIG,   big.Handle);
        }

        // ── layout construction ─────────────────────────────────────────────────

        private void BuildLayout()
        {
            var root = new Grid();
            root.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            root.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
            root.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            Content = root;

            var toolbar = BuildToolbar();
            Grid.SetRow(toolbar, 0);
            root.Children.Add(toolbar);

            _grid = BuildDataGrid();
            Grid.SetRow(_grid, 1);
            root.Children.Add(_grid);

            var statusBar = BuildStatusBar();
            Grid.SetRow(statusBar, 2);
            root.Children.Add(statusBar);
        }

        private Border BuildToolbar()
        {
            var left  = new StackPanel { Orientation = Orientation.Horizontal, VerticalAlignment = VerticalAlignment.Center };
            var right = new StackPanel { Orientation = Orientation.Horizontal, VerticalAlignment = VerticalAlignment.Center, HorizontalAlignment = HorizontalAlignment.Right };

            left.Children.Add(MakeButton("Refresh", "refresh.png", BtnRefresh_Click));
            AddSeparator(left);
            left.Children.Add(MakeGlyphButton("Start All", "▶", "GreenBrush", BtnStartAll_Click));
            left.Children.Add(MakeGlyphButton("Stop All", "■", "RedBrush", BtnStopAll_Click));
            AddSeparator(left);
            left.Children.Add(MakeButton("View Service Log", "logview.png", BtnViewServiceLog_Click));
            AddSeparator(left);

            left.Children.Add(new Image { Source = LoadImageSource("loglevel.png"), Width = 16, Height = 16, Margin = new Thickness(0, 0, 4, 0), VerticalAlignment = VerticalAlignment.Center });
            var logLevelLbl = new TextBlock { Text = "Change Service Log Level", VerticalAlignment = VerticalAlignment.Center, Margin = new Thickness(0, 0, 6, 0) };
            left.Children.Add(logLevelLbl);

            _logLevelCombo = new ComboBox { Width = 90, VerticalAlignment = VerticalAlignment.Center };
            _logLevelCombo.Items.Add("Trace");
            _logLevelCombo.Items.Add("Debug");
            _logLevelCombo.Items.Add("Info");
            _logLevelCombo.Items.Add("Warning");
            _logLevelCombo.Items.Add("Error");
            _logLevelCombo.Items.Add("Fatal");
            _logLevelCombo.SelectionChanged += LogLevelCombo_SelectionChanged;
            left.Children.Add(_logLevelCombo);

            right.Children.Add(MakeButton("Reload Server List", "serverlist.png", BtnReloadServerList_Click));
            AddSeparator(right);

            var serverListFileLabelBtn = MakeFlatButton("Edit Server List", "serverlist.png", ServerListFileLabel_Click);
            right.Children.Add(serverListFileLabelBtn);

            _serverListFileText = new TextBlock { Text = "-not-yet-known-", VerticalAlignment = VerticalAlignment.Center, Margin = new Thickness(0, 0, 8, 0) };
            var serverListFileValueBtn = new Button { Content = _serverListFileText, Style = (Style)FindResource("FlatLinkButton") };
            serverListFileValueBtn.Click += ServerListFileLabel_Click;
            right.Children.Add(serverListFileValueBtn);

            var dock = new DockPanel();
            DockPanel.SetDock(right, Dock.Right);
            dock.Children.Add(right);
            dock.Children.Add(left);

            var border = new Border { Padding = new Thickness(6, 4, 6, 4), Child = dock };
            border.SetResourceReference(Border.BackgroundProperty, "Bg2Brush");
            border.SetResourceReference(Border.BorderBrushProperty, "Bg3Brush");
            border.BorderThickness = new Thickness(0, 0, 0, 1);
            return border;
        }

        private DataGrid BuildDataGrid()
        {
            var grid = new DataGrid { ItemsSource = _rows };
            grid.SetValue(ScrollViewer.HorizontalScrollBarVisibilityProperty, ScrollBarVisibility.Auto);
            grid.SetValue(ScrollViewer.CanContentScrollProperty, true);

            grid.Columns.Add(TextColumn("Server Name",   nameof(ServerRow.ServerName)));
            grid.Columns.Add(TextColumn("Alias Name",    nameof(ServerRow.AliasName)));
            grid.Columns.Add(TextColumn("Server Type",   nameof(ServerRow.ServerType)));
            grid.Columns.Add(TextColumn("Process ID",    nameof(ServerRow.ProcessId)));
            grid.Columns.Add(TextColumn("Parent PID",    nameof(ServerRow.ParentProcessId)));
            grid.Columns.Add(StatusColumn());
            grid.Columns.Add(TextColumn("Info",          nameof(ServerRow.Info)));
            grid.Columns.Add(TextColumn("Start Time",    nameof(ServerRow.StartTime)));
            grid.Columns.Add(TextColumn("DBMS Username", nameof(ServerRow.DbmsUsername)));
            grid.Columns.Add(TextColumn("Console File",  nameof(ServerRow.ConsoleFile)));
            grid.Columns.Add(TextColumn("Start Script",  nameof(ServerRow.StartScript)));
            grid.Columns.Add(TextColumn("Config File",   nameof(ServerRow.ConfigFile)));
            grid.Columns.Add(TextColumn("Log File",      nameof(ServerRow.LogFile)));
            grid.Columns.Add(TextColumn("Command Line",  nameof(ServerRow.CommandLine)));

            _gridContextMenu = BuildContextMenu();
            grid.ContextMenu = _gridContextMenu;
            _gridContextMenu.Opened += (s, e) =>
            {
                bool hasSelection = grid.SelectedItem != null;
                foreach (var item in _gridContextMenu.Items)
                    if (item is System.Windows.Controls.MenuItem mi)
                        mi.IsEnabled = hasSelection;
            };

            grid.PreviewMouseLeftButtonDown += Grid_PreviewMouseLeftButtonDown;
            grid.MouseDoubleClick           += Grid_MouseDoubleClick;

            return grid;
        }

        private static DataGridTextColumn TextColumn(string header, string path)
            => new DataGridTextColumn
            {
                Header   = header,
                Binding  = new Binding(path),
                Width    = DataGridLength.Auto,
                // DataGridLength.Auto is supposed to size to max(header, cell content), but with a
                // custom DataGridCell template that measurement is unreliable — MinWidth guarantees
                // the (bold) header text is never clipped regardless.
                MinWidth = header.Length * 9 + 24,
            };

        private static DataGridTemplateColumn StatusColumn()
        {
            var stackFactory = new FrameworkElementFactory(typeof(StackPanel));
            stackFactory.SetValue(StackPanel.OrientationProperty, Orientation.Horizontal);

            var ellipseFactory = new FrameworkElementFactory(typeof(Ellipse));
            ellipseFactory.SetValue(FrameworkElement.WidthProperty, 10.0);
            ellipseFactory.SetValue(FrameworkElement.HeightProperty, 10.0);
            ellipseFactory.SetValue(FrameworkElement.MarginProperty, new Thickness(2, 0, 6, 0));
            ellipseFactory.SetValue(FrameworkElement.VerticalAlignmentProperty, VerticalAlignment.Center);
            ellipseFactory.SetBinding(Shape.FillProperty, new Binding(nameof(ServerRow.Status)) { Converter = new StatusToBrushConverter() });

            var textFactory = new FrameworkElementFactory(typeof(TextBlock));
            textFactory.SetValue(TextBlock.VerticalAlignmentProperty, VerticalAlignment.Center);
            textFactory.SetBinding(TextBlock.TextProperty, new Binding(nameof(ServerRow.Status)));

            stackFactory.AppendChild(ellipseFactory);
            stackFactory.AppendChild(textFactory);

            return new DataGridTemplateColumn
            {
                Header   = "Status",
                CellTemplate = new DataTemplate { VisualTree = stackFactory },
                MinWidth = 90,
            };
        }

        private ContextMenu BuildContextMenu()
        {
            var menu = new ContextMenu();
            menu.Items.Add(MenuItem("Start", StartMenuItem_Click));
            menu.Items.Add(MenuItem("Stop", StopMenuItem_Click));
            menu.Items.Add(MenuItem("Restart", RestartMenuItem_Click));
            menu.Items.Add(new Separator());
            menu.Items.Add(MenuItem("Copy Cell Content", CopyRowCellMenuItem_Click));
            menu.Items.Add(MenuItem("Copy Row Content", CopyRowAllCellsMenuItem_Click));
            menu.Items.Add(new Separator());
            menu.Items.Add(MenuItem("View Console Log", ViewConsoleMenuItem_Click));
            menu.Items.Add(MenuItem("View Log", ViewLogMenuItem_Click));
            menu.Items.Add(new Separator());
            menu.Items.Add(MenuItem("View Config File", ViewConfigMenuItem_Click));
            menu.Items.Add(MenuItem("View Start Script", ViewStartScriptMenuItem_Click));
            menu.Items.Add(MenuItem("View Command Line Options", ViewCommandLineMenuItem_Click));
            return menu;
        }

        private static System.Windows.Controls.MenuItem MenuItem(string header, RoutedEventHandler onClick)
        {
            var mi = new System.Windows.Controls.MenuItem { Header = header };
            mi.Click += onClick;
            return mi;
        }

        private Border BuildStatusBar()
        {
            _statusLabel        = new TextBlock { VerticalAlignment = VerticalAlignment.Center, Text = "Ready" };
            _serverSummaryLabel = new TextBlock { VerticalAlignment = VerticalAlignment.Center, Margin = new Thickness(12, 0, 0, 0) };
            DockPanel.SetDock(_serverSummaryLabel, Dock.Right);

            var dock = new DockPanel();
            dock.Children.Add(_serverSummaryLabel);
            dock.Children.Add(_statusLabel);

            var border = new Border { Padding = new Thickness(8, 3, 8, 3), Child = dock };
            border.SetResourceReference(Border.BackgroundProperty, "Bg2Brush");
            border.SetResourceReference(Border.BorderBrushProperty, "Bg3Brush");
            border.BorderThickness = new Thickness(0, 1, 0, 0);
            return border;
        }

        // ── toolbar helpers ──────────────────────────────────────────────────────

        private static Button MakeButton(string text, string? icon, RoutedEventHandler onClick)
        {
            var content = new StackPanel { Orientation = Orientation.Horizontal };
            if (icon != null)
                content.Children.Add(new Image { Source = LoadImageSource(icon), Width = 16, Height = 16, Margin = new Thickness(0, 0, 6, 0) });
            content.Children.Add(new TextBlock { Text = text, VerticalAlignment = VerticalAlignment.Center });

            var btn = new Button { Content = content, Margin = new Thickness(0, 0, 4, 0) };
            btn.Click += onClick;
            return btn;
        }

        // No dedicated icon assets exist for Start/Stop — a colored Unicode glyph fills in
        // for a proper icon without needing new image files.
        private static Button MakeGlyphButton(string text, string glyph, string brushKey, RoutedEventHandler onClick)
        {
            var glyphBlock = new TextBlock { Text = glyph, VerticalAlignment = VerticalAlignment.Center, Margin = new Thickness(0, 0, 6, 0), FontSize = 11 };
            glyphBlock.SetResourceReference(TextBlock.ForegroundProperty, brushKey);

            var content = new StackPanel { Orientation = Orientation.Horizontal };
            content.Children.Add(glyphBlock);
            content.Children.Add(new TextBlock { Text = text, VerticalAlignment = VerticalAlignment.Center });

            var btn = new Button { Content = content, Margin = new Thickness(0, 0, 4, 0) };
            btn.Click += onClick;
            return btn;
        }

        private Button MakeFlatButton(string text, string? icon, RoutedEventHandler onClick)
        {
            var content = new StackPanel { Orientation = Orientation.Horizontal };
            if (icon != null)
                content.Children.Add(new Image { Source = LoadImageSource(icon), Width = 16, Height = 16, Margin = new Thickness(0, 0, 6, 0) });
            content.Children.Add(new TextBlock { Text = text, VerticalAlignment = VerticalAlignment.Center });

            var btn = new Button { Content = content, Style = (Style)FindResource("FlatLinkButton"), Margin = new Thickness(0, 0, 4, 0) };
            btn.Click += onClick;
            return btn;
        }

        private static void AddSeparator(Panel panel)
        {
            // Bg3Brush is barely distinguishable from the toolbar's own Bg2Brush background —
            // FgDimBrush gives an actually-visible divider, with generous margin on both sides
            // so the gap itself reads as a break between button groups even at a glance.
            var sep = new Border { Width = 1, Margin = new Thickness(10, 6, 10, 6) };
            sep.SetResourceReference(Border.BackgroundProperty, "FgDimBrush");
            panel.Children.Add(sep);
        }

        private static BitmapImage LoadImageSource(string fileName)
            => new BitmapImage(new Uri($"pack://application:,,,/Resources/{fileName}", UriKind.Absolute));

        private static System.Drawing.Icon LoadWinFormsIcon(string fileName, int size)
        {
            var info = System.Windows.Application.GetResourceStream(new Uri($"Resources/{fileName}", UriKind.Relative));
            return new System.Drawing.Icon(info!.Stream, size, size);
        }

        // ── tray icon ─────────────────────────────────────────────────────────

        private void SetupTrayIcon()
        {
            _trayIcon.Icon = LoadWinFormsIcon("dbxtune_central_starter.ico", 16);
            _trayIcon.Text = "DbxStarter Client";
            _trayIcon.Visible = true;
            _trayIcon.DoubleClick += (s, e) => RestoreFromTray();

            var startupItem = new System.Windows.Forms.ToolStripMenuItem("Start with Windows")
            {
                Checked      = StartupHelper.IsEnabled(),
                CheckOnClick = false
            };
            startupItem.Click += (s, e) =>
            {
                string exePath = Environment.ProcessPath!;
                if (startupItem.Checked) { StartupHelper.Disable(); startupItem.Checked = false; }
                else { StartupHelper.Enable(exePath); startupItem.Checked = true; }
            };

            _darkModeItem = new System.Windows.Forms.ToolStripMenuItem("Dark Mode")
            {
                CheckOnClick = false,
                Checked      = AppSettings.DarkMode
            };
            _darkModeItem.Click += (s, e) =>
            {
                AppSettings.ToggleDarkMode();
                AppTheme.Apply(AppSettings.DarkMode);
                _darkModeItem.Checked = AppSettings.DarkMode;
            };

            var trayMenu = new System.Windows.Forms.ContextMenuStrip();
            trayMenu.Items.Add(startupItem);
            trayMenu.Items.Add(_darkModeItem);
            trayMenu.Items.Add(new System.Windows.Forms.ToolStripSeparator());
            trayMenu.Items.Add("Restore", null, (s, e) => RestoreFromTray());
            trayMenu.Items.Add(new System.Windows.Forms.ToolStripSeparator());
            trayMenu.Items.Add("Exit", null, (s, e) =>
            {
                _trayIcon.Visible = false;
                System.Windows.Application.Current.Shutdown();
            });
            _trayIcon.ContextMenuStrip = trayMenu;
        }

        private void RestoreFromTray()
        {
            Show();
            WindowState = WindowState.Normal;
            Activate();
        }

        // ── window lifecycle ────────────────────────────────────────────────────

        private async void MainWindow_Loaded(object sender, RoutedEventArgs e)
        {
            StartupHelper.RefreshIfEnabled(Environment.ProcessPath!);

            // Follow Windows system theme changes live (only when the user hasn't
            // manually overridden via the tray menu).
            SystemEvents.UserPreferenceChanged += OnSystemThemeChanged;

            await ConnectWithRetryAsync();
        }

        // On startup DbxStarterService may have just been (re)started and not be listening on
        // its named pipe yet, so the very first connection attempt can legitimately fail even
        // though the service is about to be ready. Retry a few times before settling into the
        // normal periodic refresh cadence, instead of showing "NOT CONNECTED" for up to a
        // minute until the next automatic refresh.
        private async Task ConnectWithRetryAsync()
        {
            const int maxRetries   = 5;
            const int retryDelayMs = 3000;

            for (int attempt = 0; attempt <= maxRetries; attempt++)
            {
                await RefreshServerStatusAsync(true);
                if (_isConnected) return;

                if (attempt < maxRetries)
                {
                    StatusLabelInfo($"Not connected yet — service may still be starting. Retrying ({attempt + 1}/{maxRetries})…");
                    await Task.Delay(retryDelayMs);
                }
            }
        }

        private void OnSystemThemeChanged(object sender, UserPreferenceChangedEventArgs e)
        {
            if (e.Category != UserPreferenceCategory.General) return;
            if (AppSettings.ApplySystemTheme())
                Dispatcher.BeginInvoke(() =>
                {
                    AppTheme.Apply(AppSettings.DarkMode);
                    _darkModeItem.Checked = AppSettings.DarkMode;
                });
        }

        private void MainWindow_StateChanged(object? sender, EventArgs e)
        {
            if (WindowState == WindowState.Minimized)
            {
                Hide();
                _trayIcon.ShowBalloonTip(1500, "DbxStarter Client", "Running in system tray.", System.Windows.Forms.ToolTipIcon.Info);
            }
        }

        private void MainWindow_Closing(object? sender, CancelEventArgs e)
        {
            SystemEvents.UserPreferenceChanged -= OnSystemThemeChanged;

            // RestoreBounds reflects the last non-minimized geometry regardless of current
            // WindowState, so this captures the "normal" size/position even if we're closing
            // while maximized.
            var bounds = RestoreBounds;
            if (bounds.Width > 0 && bounds.Height > 0)
                AppSettings.SaveWindowBounds(bounds.Left, bounds.Top, bounds.Width, bounds.Height,
                    WindowState == WindowState.Maximized);

            _trayIcon.Visible = false;
            _serviceClient.Dispose();
            System.Windows.Application.Current.Shutdown();
        }

        // Restores the window's last saved size/position, falling back to a screen-relative
        // default and centering when nothing was saved yet (first run) or the saved position
        // no longer fits any current monitor (e.g. a monitor was unplugged/reconfigured).
        private void ApplySavedWindowBounds(double defaultWidth, double defaultHeight)
        {
            Width  = AppSettings.WindowWidth  ?? defaultWidth;
            Height = AppSettings.WindowHeight ?? defaultHeight;

            bool hasPosition = AppSettings.WindowLeft.HasValue && AppSettings.WindowTop.HasValue;
            if (hasPosition && IsOnScreen(AppSettings.WindowLeft!.Value, AppSettings.WindowTop!.Value, Width, Height))
            {
                Left = AppSettings.WindowLeft!.Value;
                Top  = AppSettings.WindowTop!.Value;
                WindowStartupLocation = WindowStartupLocation.Manual;
            }
            else
            {
                WindowStartupLocation = WindowStartupLocation.CenterScreen;
            }

            if (AppSettings.WindowMaximized)
                WindowState = WindowState.Maximized;
        }

        private static bool IsOnScreen(double left, double top, double width, double height)
        {
            double virtLeft   = SystemParameters.VirtualScreenLeft;
            double virtTop    = SystemParameters.VirtualScreenTop;
            double virtRight  = virtLeft + SystemParameters.VirtualScreenWidth;
            double virtBottom = virtTop + SystemParameters.VirtualScreenHeight;
            return left + width > virtLeft && left < virtRight && top + height > virtTop && top < virtBottom;
        }

        private void MainWindow_PreviewKeyDown(object sender, KeyEventArgs e)
        {
            if (e.Key == Key.F5)
                _ = RefreshServerStatusAsync(true);
        }

        // ── status helpers ────────────────────────────────────────────────────

        private void StatusLabelReset()
        {
            _statusLabel.SetResourceReference(TextBlock.ForegroundProperty, "FgBrush");
            _statusLabel.Text = "";
            _statusLabel.ToolTip = null;
        }

        private void StatusLabelInfo(string message) => StatusLabelInfo(message, "");

        private void StatusLabelInfo(string message, string tooltip)
        {
            _statusLabel.SetResourceReference(TextBlock.ForegroundProperty, "FgBrush");
            _statusLabel.Text = message;
            _statusLabel.ToolTip = string.IsNullOrEmpty(tooltip) ? null : tooltip;
        }

        private void StatusLabelError(string message) => StatusLabelError(message, null);

        private void StatusLabelError(string message, Exception? ex)
        {
            _statusLabel.SetResourceReference(TextBlock.ForegroundProperty, "RedBrush");
            _statusLabel.Text = message;
            _statusLabel.ToolTip = ex?.ToString();
        }

        private void UpdateConnectionTitle(bool connected)
        {
            _isConnected = connected;
            Title = connected
                ? "DbxStarter Client — connected"
                : "DbxStarter Client — NOT CONNECTED";
        }

        private void UpdateServerSummary()
        {
            int total   = _rows.Count;
            int running = _rows.Count(r => "Running".Equals(r.Status, StringComparison.OrdinalIgnoreCase));
            _serverSummaryLabel.Text = total == 0 ? "" : $"{running} of {total} running";
        }

        private bool IfNoRowIsSelected()
        {
            if (_grid.SelectedItem != null)
                return false;
            StatusLabelInfo("No row selected — click a row first.", "");
            return true;
        }

        // ── refresh ───────────────────────────────────────────────────────────

        private async Task RefreshServerStatusAsync(bool resetStatus)
        {
            if (resetStatus)
                StatusLabelReset();

            Mouse.OverrideCursor = Cursors.Wait;
            try
            {
                var sw = Stopwatch.StartNew();

                var (serverInfoFile, logLevel, statusJson) = await Task.Run(() =>
                {
                    var f = SendCommand("GetServerInfoFile");
                    var l = GetServiceLogLevel();
                    var s = SendCommand("GetStatus");
                    return (f, l, s);
                });

                if (serverInfoFile != null)
                    _serverListFileText.Text = serverInfoFile;

                if (logLevel != null)
                    _logLevelCombo.SelectedItem = logLevel;

                if (statusJson.StartsWith("ERROR") || statusJson.StartsWith("UNKNOWN_COMMAND"))
                {
                    StatusLabelError($"ERROR retrieving server status: {statusJson}");
                    UpdateConnectionTitle(false);
                    _rows.Clear();
                    UpdateServerSummary();
                    return;
                }

                var serviceStatus = await Task.Run(() => DeserializeFromJson<ServiceStatus>(statusJson));

                UpdateServerList(serviceStatus);
                UpdateConnectionTitle(true);
                sw.Stop();
                StatusLabelInfo($"Last Refreshed: {DateTime.Now:HH:mm:ss}   ({sw.ElapsedMilliseconds} ms)");
            }
            catch (Exception ex)
            {
                _rows.Clear();
                UpdateConnectionTitle(false);
                UpdateServerSummary();
                StatusLabelError($"Error: {ex.Message}", ex);
            }
            finally
            {
                Mouse.OverrideCursor    = null;
                _refreshTimer.Interval  = TimeSpan.FromMilliseconds(NORMAL_REFRESH_RATE_MS);
                _refreshTimer.Start();
            }
        }

        private void UpdateServerList(ServiceStatus status)
        {
            string? selectedServer = (_grid.SelectedItem as ServerRow)?.ServerName;

            _rows.Clear();
            foreach (var e in status.RunningProcesses)
            {
                _rows.Add(new ServerRow
                {
                    ServerName      = e.ServerName,
                    AliasName       = e.ServerAliasName,
                    ServerType      = e.ServerType,
                    ProcessId       = e.ProcessId,
                    ParentProcessId = e.ParentProcessId,
                    Status          = e.Running ? "Running" : "Stopped",
                    Info            = e.Info,
                    StartTime       = e.StartTime,
                    ConsoleFile     = e.ConsoleFile,
                    StartScript     = e.StartScript,
                    ConfigFile      = e.ConfigFile,
                    DbmsUsername    = e.DbmsUsername,
                    LogFile         = e.LogFile,
                    CommandLine     = e.CommandLine,
                });
            }

            _grid.SelectedItem = string.IsNullOrEmpty(selectedServer)
                ? null
                : _rows.FirstOrDefault(r => r.ServerName == selectedServer);

            UpdateServerSummary();
        }

        // ── toolbar button handlers ───────────────────────────────────────────

        private async void BtnRefresh_Click(object sender, RoutedEventArgs e)
            => await RefreshServerStatusAsync(true);

        private async void BtnReloadServerList_Click(object sender, RoutedEventArgs e)
        {
            StatusLabelReset();
            Mouse.OverrideCursor = Cursors.Wait;
            try
            {
                var result = await Task.Run(() => SendCommand("ReloadServerList"));
                if (result == "OK")
                {
                    StatusLabelInfo("Server list reloaded.");
                    await RefreshServerStatusAsync(false);
                }
                else
                {
                    StatusLabelError($"Failed to reload server list: {result}");
                }
            }
            catch (Exception ex) { StatusLabelError($"Error: {ex.Message}", ex); }
            finally { Mouse.OverrideCursor = null; }
        }

        private async void BtnStartAll_Click(object sender, RoutedEventArgs e)
        {
            StatusLabelReset();
            var targets = StoppedServerNames();
            if (targets.Count == 0) { StatusLabelInfo("All servers are already running."); return; }

            Mouse.OverrideCursor = Cursors.Wait;
            try
            {
                StatusLabelInfo($"Starting {targets.Count} server(s)…");
                foreach (var name in targets)
                    await Task.Run(() => SendCommand($"StartServer:{name}"));
                TriggerImmediateRefresh(5000);
            }
            catch (Exception ex) { StatusLabelError($"Error: {ex.Message}", ex); }
            finally { Mouse.OverrideCursor = null; }
        }

        private async void BtnStopAll_Click(object sender, RoutedEventArgs e)
        {
            StatusLabelReset();
            var targets = RunningServerNames();
            if (targets.Count == 0) { StatusLabelInfo("No servers are running."); return; }

            var confirm = System.Windows.MessageBox.Show(this,
                $"Stop all {targets.Count} running server(s)?",
                "Confirm Stop All", MessageBoxButton.YesNo, MessageBoxImage.Warning);
            if (confirm != MessageBoxResult.Yes) return;

            Mouse.OverrideCursor = Cursors.Wait;
            try
            {
                StatusLabelInfo($"Stopping {targets.Count} server(s)…");
                foreach (var name in targets)
                    await Task.Run(() => SendCommand($"StopServer:{name}"));
                TriggerImmediateRefresh(2000);
            }
            catch (Exception ex) { StatusLabelError($"Error: {ex.Message}", ex); }
            finally { Mouse.OverrideCursor = null; }
        }

        private void BtnViewServiceLog_Click(object sender, RoutedEventArgs e)
        {
            StatusLabelReset();
            try
            {
                var result = SendCommand("GetServiceLogName");
                StatusLabelInfo($"Opening: {result}");
                if (!result.StartsWith("--"))
                    OpenLogViewer("ServiceLog", result, true);
            }
            catch
            {
                try
                {
                    var result = GetServiceLogFileName();
                    StatusLabelInfo($"Opening: {result}");
                    if (!result.StartsWith("--"))
                        OpenLogViewer("ServiceLog", result, true);
                }
                catch (Exception ex2) { StatusLabelError($"Error: {ex2.Message}", ex2); }
            }
        }

        private void ServerListFileLabel_Click(object sender, RoutedEventArgs e)
            => new FileEditor(_serverListFileText.Text ?? string.Empty) { Owner = this }.ShowDialog();

        // ── context menu handlers ─────────────────────────────────────────────

        private async void RestartMenuItem_Click(object sender, RoutedEventArgs e)
        {
            StatusLabelReset();
            if (IfNoRowIsSelected()) return;
            string name = GetAliasOrServerName();
            Mouse.OverrideCursor = Cursors.Wait;
            try
            {
                StatusLabelInfo($"Restarting {name}…");
                var result = await Task.Run(() => SendCommand($"RestartServer:{name}"));
                if (result == "OK") { await RefreshServerStatusAsync(false); TriggerImmediateRefresh(5000); }
                else StatusLabelError($"Failed to restart {name}");
            }
            catch (Exception ex) { StatusLabelError($"Error: {ex.Message}", ex); }
            finally { Mouse.OverrideCursor = null; }
        }

        private async void StopMenuItem_Click(object sender, RoutedEventArgs e)
        {
            StatusLabelReset();
            if (IfNoRowIsSelected()) return;
            string name = GetAliasOrServerName();
            Mouse.OverrideCursor = Cursors.Wait;
            try
            {
                StatusLabelInfo($"Stopping {name}…");
                var result = await Task.Run(() => SendCommand($"StopServer:{name}"));
                if (result == "OK") { await RefreshServerStatusAsync(false); TriggerImmediateRefresh(1000); }
                else StatusLabelError($"Failed to stop {name}");
            }
            catch (Exception ex) { StatusLabelError($"Error: {ex.Message}", ex); }
            finally { Mouse.OverrideCursor = null; }
        }

        private async void StartMenuItem_Click(object sender, RoutedEventArgs e)
        {
            StatusLabelReset();
            if (IfNoRowIsSelected()) return;
            string name = GetAliasOrServerName();
            Mouse.OverrideCursor = Cursors.Wait;
            try
            {
                StatusLabelInfo($"Starting {name}…");
                var result = await Task.Run(() => SendCommand($"StartServer:{name}"));
                if (result == "OK") { await RefreshServerStatusAsync(false); TriggerImmediateRefresh(5000); }
                else StatusLabelError($"Failed to start {name}");
            }
            catch (Exception ex) { StatusLabelError($"Error: {ex.Message}", ex); }
            finally { Mouse.OverrideCursor = null; }
        }

        private void CopyRowAllCellsMenuItem_Click(object sender, RoutedEventArgs e)
        {
            StatusLabelReset();
            if (IfNoRowIsSelected()) return;
            if (_grid.SelectedItem is not ServerRow row) return;

            var output = new StringBuilder();
            foreach (var col in _grid.Columns)
            {
                string header = col.Header?.ToString() ?? "";
                output.AppendLine($"{header,-20}: {GetColumnValue(row, header)}");
            }
            System.Windows.Clipboard.SetText(output.ToString());
            StatusLabelInfo("Copied all cells to clipboard.", output.ToString());
        }

        private void CopyRowCellMenuItem_Click(object sender, RoutedEventArgs e)
        {
            StatusLabelReset();
            if (IfNoRowIsSelected()) return;
            if (_grid.SelectedItem is not ServerRow row) return;

            var col = _grid.CurrentColumn;
            string header = col?.Header?.ToString() ?? "Server Name";
            string val = GetColumnValue(row, header);
            System.Windows.Clipboard.SetText(val);
            StatusLabelInfo($"Copied: {val}", val);
        }

        private void ViewConsoleMenuItem_Click(object sender, RoutedEventArgs e)
        {
            StatusLabelReset();
            if (IfNoRowIsSelected()) return;
            if (_grid.SelectedItem is not ServerRow row) return;
            if (File.Exists(row.ConsoleFile)) OpenLogViewer(GetAliasOrServerName(), row.ConsoleFile, true);
            else StatusLabelError($"Console file not found: {row.ConsoleFile}");
        }

        private void ViewLogMenuItem_Click(object sender, RoutedEventArgs e)
        {
            StatusLabelReset();
            if (IfNoRowIsSelected()) return;
            if (_grid.SelectedItem is not ServerRow row) return;
            if (File.Exists(row.LogFile)) OpenLogViewer(GetAliasOrServerName(), row.LogFile, true);
            else StatusLabelError($"Log file not found: {row.LogFile}");
        }

        private void ViewConfigMenuItem_Click(object sender, RoutedEventArgs e)
        {
            StatusLabelReset();
            if (IfNoRowIsSelected()) return;
            if (_grid.SelectedItem is not ServerRow row) return;
            if (File.Exists(row.ConfigFile)) new FileEditor(row.ConfigFile) { Owner = this }.ShowDialog();
            else StatusLabelError($"Config file not found: {row.ConfigFile}");
        }

        private void ViewStartScriptMenuItem_Click(object sender, RoutedEventArgs e)
        {
            StatusLabelReset();
            if (IfNoRowIsSelected()) return;
            if (_grid.SelectedItem is not ServerRow row) return;
            string raw   = row.StartScript ?? "";
            var match    = Regex.Match(raw, @"^(""[^""]+""|[^\s]+)");
            string path  = match.Success ? match.Value.Trim('"') : "";
            if (File.Exists(path)) new FileEditor(path) { Owner = this }.ShowDialog();
            else StatusLabelError($"Start script not found: {raw}");
        }

        private void ViewCommandLineMenuItem_Click(object sender, RoutedEventArgs e)
        {
            StatusLabelReset();
            if (IfNoRowIsSelected()) return;
            if (_grid.SelectedItem is not ServerRow row) return;
            if (string.IsNullOrWhiteSpace(row.CommandLine)) { StatusLabelError("No command line available."); return; }
            new CommandLineViewer(GetAliasOrServerName(), row.CommandLine) { Owner = this }.Show();
        }

        private static string GetColumnValue(ServerRow row, string header) => header switch
        {
            "Server Name"   => row.ServerName,
            "Alias Name"    => row.AliasName,
            "Server Type"   => row.ServerType,
            "Process ID"    => row.ProcessId,
            "Parent PID"    => row.ParentProcessId,
            "Status"        => row.Status,
            "Info"          => row.Info,
            "Start Time"    => row.StartTime,
            "Console File"  => row.ConsoleFile,
            "Start Script"  => row.StartScript,
            "Config File"   => row.ConfigFile,
            "DBMS Username" => row.DbmsUsername,
            "Log File"      => row.LogFile,
            "Command Line"  => row.CommandLine,
            _               => "",
        };

        // ── grid events ───────────────────────────────────────────────────────

        private void Grid_PreviewMouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            var dep = e.OriginalSource as DependencyObject;
            while (dep != null && dep is not DataGridRow) dep = VisualTreeHelper.GetParent(dep);
            if (dep is DataGridRow row && row.IsSelected)
                Dispatcher.BeginInvoke(new Action(() => _grid.SelectedItem = null));
        }

        private void Grid_MouseDoubleClick(object sender, MouseButtonEventArgs e)
        {
            if (_grid.SelectedItem is not ServerRow row) return;
            string name = GetAliasOrServerName();
            if (File.Exists(row.LogFile))     { OpenLogViewer(name, row.LogFile, true); return; }
            if (File.Exists(row.ConsoleFile)) { OpenLogViewer(name, row.ConsoleFile, true); return; }
            StatusLabelError($"No log file found for {name}");
        }

        // ── log-level combo ───────────────────────────────────────────────────

        private void LogLevelCombo_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            StatusLabelReset();
            if (_logLevelCombo.SelectedIndex == -1) return;
            string newLevel = _logLevelCombo.SelectedItem?.ToString() ?? "";
            if (string.IsNullOrEmpty(newLevel)) return;
            try
            {
                var result = SendCommand($"SetServiceLogLevel:{newLevel}");
                if (result.StartsWith("OK:"))
                    StatusLabelInfo($"Service log level changed to '{result.Substring(3)}'");
                else
                    StatusLabelError($"Failed to change log level: {result}");
            }
            catch (Exception ex) { StatusLabelError($"Error: {ex.Message}", ex); }
        }

        // ── helpers ───────────────────────────────────────────────────────────

        private void TriggerImmediateRefresh(int nextMs)
        {
            _refreshTimer.Stop();
            _statusLabel.Text      = $"Refreshing in {nextMs / 1000} second(s)…";
            _refreshTimer.Interval = TimeSpan.FromMilliseconds(nextMs);
            _refreshTimer.Start();
        }

        private string GetAliasOrServerName()
        {
            if (_grid.SelectedItem is not ServerRow row) return string.Empty;
            return !string.IsNullOrEmpty(row.AliasName) ? row.AliasName : row.ServerName;
        }

        private List<string> RunningServerNames() => ServerNamesByStatus("Running");
        private List<string> StoppedServerNames()  => ServerNamesByStatus("Stopped");

        private List<string> ServerNamesByStatus(string status)
            => _rows
                .Where(r => status.Equals(r.Status, StringComparison.OrdinalIgnoreCase))
                .Select(r => !string.IsNullOrEmpty(r.AliasName) ? r.AliasName : r.ServerName)
                .Where(n => !string.IsNullOrEmpty(n))
                .ToList();

        private string GetServiceLogLevel()
        {
            var result = SendCommand("GetServiceLogLevel");
            if (result.StartsWith("OK:")) result = result.Substring(3);
            return result.ToUpper() switch
            {
                "VERBOSE"     => "Trace",
                "DEBUG"       => "Debug",
                "INFORMATION" => "Info",
                "WARNING"     => "Warning",
                "ERROR"       => "Error",
                "FATAL"       => "Fatal",
                _             => result
            };
        }

        private static string GetServiceLogFileName()
        {
            string logDir = Environment.GetEnvironmentVariable("DBXTUNE_LOG_DIR")
                         ?? Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
                                         ".dbxtune\\dbxc\\log");
            string path = Path.Combine(logDir, "DbxStarterService.log");
            if (File.Exists(path)) return path;

            path = @"C:\Users\dbxtune\.dbxtune\dbxc\log\DbxStarterService.log";
            if (File.Exists(path)) return path;

            return "--GetServiceLogFileName--did-NOT-find-the-service-logFile";
        }

        private static void OpenLogViewer(string serverName, string logFilePath, bool autoRefresh)
            => new LogViewer(serverName, logFilePath, autoRefresh, -1).Show();

        private string SendCommand(string command) => _serviceClient.SendCommand(command);

        private static T DeserializeFromJson<T>(string json)
        {
            using var ms = new MemoryStream(Encoding.UTF8.GetBytes(json));
            var serializer = new DataContractJsonSerializer(typeof(T));
            return (T)serializer.ReadObject(ms)!;
        }
    }

    // Thin view-model row backing the server-list DataGrid. Rows are fully replaced on every
    // refresh (see MainWindow.UpdateServerList), so no INotifyPropertyChanged is needed.
    internal sealed class ServerRow
    {
        public string ServerName      { get; set; } = "";
        public string AliasName       { get; set; } = "";
        public string ServerType      { get; set; } = "";
        public string ProcessId       { get; set; } = "";
        public string ParentProcessId { get; set; } = "";
        public string Status          { get; set; } = "";
        public string Info            { get; set; } = "";
        public string StartTime       { get; set; } = "";
        public string ConsoleFile     { get; set; } = "";
        public string StartScript     { get; set; } = "";
        public string ConfigFile      { get; set; } = "";
        public string DbmsUsername    { get; set; } = "";
        public string LogFile         { get; set; } = "";
        public string CommandLine     { get; set; } = "";
    }
}

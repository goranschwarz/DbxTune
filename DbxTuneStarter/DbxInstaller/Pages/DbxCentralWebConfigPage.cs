using System.IO;
using System.Linq;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using DbxInstaller.Wizard;

namespace DbxInstaller.Pages
{
    // Settings for DbxTune Central's own web server (DBX_CENTRAL.conf), plus a full inline
    // editor for that file. Placed after OS Monitoring since it depends on files created during
    // install (DBX_CENTRAL.conf is written by "Initialize DbxTune home"), and before Firewall +
    // Start Service so a changed port is picked up by the firewall rule configured there.
    //
    // "Let DbxStarterService Manage/Start DbxCentral" used to live on the DbxStarter Web UI Config
    // page, but that page runs before install — before DbxStarterService.json even exists — so
    // moving it here means its value can't just flow through InstallConfig like other fields do:
    // WriteServiceConfig (an install step, runs before this page) already wrote the file's initial
    // ManageDbxCentral value from InstallConfig's default. This checkbox instead reads/writes that
    // key directly in DbxStarterService.json, the same way the HTTP/HTTPS Port fields below read/
    // write DBX_CENTRAL.conf directly rather than routing through InstallConfig.
    //
    // Only the HTTP/HTTPS ports are exposed as their own fields for now — more fields (or a small
    // per-setting sub-wizard, similar to DBMS Configuration) are planned. The inline editor covers
    // everything else in the meantime.
    internal sealed class DbxCentralWebConfigPage : WizardPage
    {
        public override string StepTitle => "DbxCentral Web Config";

        private const string HttpPortKey = "DbxTuneCentral.web.http.port.windows";
        private const int DefaultHttpPort = 80;
        private const string HttpsPortKey = "DbxTuneCentral.web.https.port.windows";
        private const int DefaultHttpsPort = 443;

        // Suffixes passed to InstallActions.ReadMailProp/WriteMailProp — those match against
        // "mail.<suffix>" or any writer-specific "*ToMail.<suffix>" key already in the file.
        private const string MailHostnameSuffix = "smtp.hostname";
        private const string MailToSuffix = "to";
        private const string MailFromSuffix = "from";

        private readonly StackPanel _header = new();
        private readonly ContentControl _bodyHost = new();
        // Auto by default; switched to Star whenever the body (port rows + editor, or the
        // missing-file message) is actually built, so it fills the rest of the page.
        private readonly RowDefinition _bodyRow = new() { Height = GridLength.Auto };
        private ScintillaNET.Scintilla? _editor;
        private TextBox? _txtHttpPort;
        private TextBox? _txtHttpsPort;

        private readonly TextBlock _readCarefullyNote = new()
        {
            Text = "Read through the DBX_CENTRAL.conf file below in detail and adjust it for your environment — " +
                   "especially the email (SMTP) settings.",
            FontWeight = FontWeights.Bold,
            TextWrapping = TextWrapping.Wrap,
            Margin = new Thickness(0, 4, 0, 12),
        };

        public DbxCentralWebConfigPage()
        {
            _readCarefullyNote.SetResourceReference(TextBlock.ForegroundProperty, "OrangeBrush");

            var root = new Grid();
            root.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            root.RowDefinitions.Add(_bodyRow);

            Grid.SetRow(_header, 0);
            root.Children.Add(_header);

            Grid.SetRow(_bodyHost, 1);
            root.Children.Add(_bodyHost);

            Content = root;
        }

        public override void OnEnter(WizardContext ctx)
        {
            _header.Children.Clear();
            _bodyHost.Content = null;
            _bodyRow.Height = GridLength.Auto;
            _editor = null;
            _txtHttpPort = null;
            _txtHttpsPort = null;

            _header.Children.Add(PageHelpers.Title("DbxCentral Web Config"));
            _header.Children.Add(PageHelpers.Subtitle(
                "Settings for DbxTune Central's own web server, read from DBX_CENTRAL.conf. More fields are " +
                "coming soon — for now, use the editor below for anything not yet exposed here."));
            _header.Children.Add(_readCarefullyNote);

            bool managed = InstallActions.ReadServiceConfigBool(ctx.Config.DbxConfDir, "ManageDbxCentral", true);
            var chkManage = new CheckBox
            {
                Content = "Let DbxStarterService Manage/Start DbxCentral  (uncheck for collector-only nodes)",
                IsChecked = managed,
                Margin = new Thickness(0, 4, 0, 16),
            };
            _header.Children.Add(chkManage);

            var body = new Grid();
            body.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            body.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            body.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            body.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            body.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            body.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });

            string confPath = Path.Combine(ctx.Config.DbxConfDir, "DBX_CENTRAL.conf");
            if (!File.Exists(confPath))
            {
                var missing = PageHelpers.SelectableDim(
                    $"Not found: {confPath} — this is normally created during install (Initialize DbxTune home).");
                Grid.SetRow(missing, 0);
                body.Children.Add(missing);
            }
            else
            {
                var (httpRow, txtHttp) = BuildPortRow(confPath, "HTTP Port", HttpPortKey, DefaultHttpPort, enabled: true, tooltip: null);
                _txtHttpPort = txtHttp;
                Grid.SetRow(httpRow, 0);
                body.Children.Add(httpRow);

                string sslDir   = Path.Combine(ctx.Config.DbxConfDir, "ssl");
                string certPath = Path.Combine(sslDir, "cert.pem");
                string keyPath  = Path.Combine(sslDir, "key.pem");
                bool sslReady = File.Exists(certPath) && File.Exists(keyPath);
                string httpsTooltip = sslReady
                    ? $"SSL certificate found:\n  {certPath}\n  {keyPath}"
                    : $"HTTPS requires an SSL certificate — not found:\n  {certPath}\n  {keyPath}\n\n" +
                      "Create the 'ssl' folder under the conf directory containing cert.pem and key.pem to enable this.";

                var (httpsRow, txtHttps) = BuildPortRow(confPath, "HTTPS Port", HttpsPortKey, DefaultHttpsPort, enabled: sslReady, tooltip: httpsTooltip);
                _txtHttpsPort = txtHttps;
                Grid.SetRow(httpsRow, 1);
                body.Children.Add(httpsRow);

                var mailSection = BuildMailSection(ctx, confPath);
                Grid.SetRow(mailSection, 2);
                body.Children.Add(mailSection);

                var header = PageHelpers.SectionHeader("DBX_CENTRAL.conf");
                header.Margin = new Thickness(0, 8, 0, 0);
                Grid.SetRow(header, 3);
                body.Children.Add(header);

                var path = PageHelpers.SelectableDim(confPath);
                Grid.SetRow(path, 4);
                body.Children.Add(path);

                var editorGrid = BuildFileEditor(confPath);
                Grid.SetRow(editorGrid, 5);
                body.Children.Add(editorGrid);
            }

            // A checkbox toggle is an unambiguous, atomic action (unlike free text, where you
            // don't want to write to disk on every keystroke) — write it straight to
            // DbxStarterService.json rather than requiring a separate Save click.
            void ApplyManagedState(bool value)
            {
                InstallActions.WriteServiceConfigBool(ctx.Config.DbxConfDir, "ManageDbxCentral", value);
                ctx.Config.ManageDbxCentral = value;
                // Hidden (not just disabled) when unmanaged — nothing here applies to a
                // collector-only node — and the body row collapses back to Auto so hiding it
                // doesn't leave a dead blank area reserved by the Star row below.
                body.Visibility = value ? Visibility.Visible : Visibility.Collapsed;
                _bodyRow.Height = value ? new GridLength(1, GridUnitType.Star) : GridLength.Auto;
            }
            chkManage.Checked   += (_, _) => ApplyManagedState(true);
            chkManage.Unchecked += (_, _) => ApplyManagedState(false);

            _bodyHost.Content = body;
            body.Visibility = managed ? Visibility.Visible : Visibility.Collapsed;
            _bodyRow.Height = managed ? new GridLength(1, GridUnitType.Star) : GridLength.Auto;
        }

        // Shared structure for HTTP/HTTPS-style port entries: [label] [field] [Save button]
        // [live availability status], plus an outcome line and a "changed from default" info
        // note. When enabled is false, the whole thing is greyed out with tooltip explaining why
        // (WPF still shows tooltips on disabled elements) — used for HTTPS until an SSL cert is present.
        private (UIElement Element, TextBox Field) BuildPortRow(
            string confPath, string label, string key, int defaultPort, bool enabled, string? tooltip)
        {
            int currentPort = InstallActions.ReadJavaPropInt(confPath, key, defaultPort);

            var txtPort = PageHelpers.TextBox(currentPort.ToString());
            var btnSave = new Button { Content = "Save Port" };
            // Collapsed (not just empty) until there's an actual message — an empty TextBlock
            // still reserves a line's height, which was showing up as extra gap between rows.
            var lblStatus = new TextBlock { Margin = new Thickness(8, 0, 0, 0), VerticalAlignment = VerticalAlignment.Center, Visibility = Visibility.Collapsed };
            var lblAvailability = new TextBlock { TextWrapping = TextWrapping.Wrap };
            var infoText = PageHelpers.Dim("");
            infoText.Margin = new Thickness(0, 8, 0, 0);
            infoText.Visibility = Visibility.Collapsed;

            void UpdateInfo()
            {
                bool changed = int.TryParse(txtPort.Text.Trim(), out int p) && p != defaultPort;
                infoText.Visibility = changed ? Visibility.Visible : Visibility.Collapsed;
                infoText.Text = $"The default {label} on Windows is {defaultPort}. Since you're changing it: " +
                    "DbxTune Central must be (re)started for the new port to take effect, the Windows Firewall " +
                    "rule configured later in this wizard will automatically use this new value, and any existing " +
                    "bookmarks or shortcuts pointing at the old port should be updated.";
            }

            // Informational only (doesn't block Save) — DbxCentral only picks up a port change on
            // its next (re)start, so a conflict "right now" doesn't necessarily mean it'll still
            // conflict then, and the port may currently be held by DbxCentral's own running instance.
            void UpdateAvailability()
            {
                if (!int.TryParse(txtPort.Text.Trim(), out int p) || p is < 1 or > 65535)
                {
                    lblAvailability.Text = "";
                    return;
                }
                var (free, owner, likelyDbxCentral) = InstallActions.CheckLocalPortAvailable(p);
                if (free)
                {
                    lblAvailability.Text = "Port is available.";
                    lblAvailability.SetResourceReference(TextBlock.ForegroundProperty, "GreenBrush");
                }
                else if (likelyDbxCentral)
                {
                    lblAvailability.Text = $"Port {p} is already in use by {owner} — likely DbxTune Central itself, already running. Not a conflict.";
                    lblAvailability.SetResourceReference(TextBlock.ForegroundProperty, "GreenBrush");
                }
                else
                {
                    lblAvailability.Text = $"Port {p} appears to be in use{(owner != null ? $" by {owner}" : "")} right now.";
                    lblAvailability.SetResourceReference(TextBlock.ForegroundProperty, "OrangeBrush");
                }
            }

            txtPort.TextChanged += (_, _) => { UpdateInfo(); UpdateAvailability(); };
            UpdateInfo();
            UpdateAvailability();

            btnSave.Click += (_, _) =>
            {
                lblStatus.Visibility = Visibility.Visible;
                if (!int.TryParse(txtPort.Text.Trim(), out int port) || port is < 1 or > 65535)
                {
                    lblStatus.Text = "Enter a valid port number.";
                    lblStatus.SetResourceReference(TextBlock.ForegroundProperty, "RedBrush");
                    return;
                }
                InstallActions.WriteJavaPropInt(confPath, key, port);
                RefreshEditorFromDisk(confPath);
                lblStatus.Text = "Saved.";
                lblStatus.SetResourceReference(TextBlock.ForegroundProperty, "GreenBrush");
            };

            var row = PageHelpers.Row(label, txtPort, btnSave, lblAvailability);
            var panel = PageHelpers.Stack(row, lblStatus, infoText);
            panel.IsEnabled = enabled;

            if (tooltip != null)
            {
                // ToolTipService.ShowOnDisabled defaults to false, so a disabled row (HTTPS with
                // no SSL cert yet) would otherwise show no tooltip at all — enable it explicitly.
                // Also set directly on the row's label so hovering the label itself always works,
                // not just relying on it inheriting the panel's tooltip.
                panel.ToolTip = tooltip;
                ToolTipService.SetShowOnDisabled(panel, true);
                if (row.Children.Count > 0 && row.Children[0] is FrameworkElement rowLabel)
                {
                    rowLabel.ToolTip = tooltip;
                    ToolTipService.SetShowOnDisabled(rowLabel, true);
                }
            }

            return (panel, txtPort);
        }

        // Mail settings (SMTP host, To, From) used by DbxCentral's own alarm/report emails.
        // If DBMS Configuration has already been run for at least one server, its collector
        // config file(s) are searched for the same settings (mail.*, or *ToMail.* for a
        // writer-specific override such as AlarmWriterToMail/ReportSenderToMail) — any value
        // found there and not yet set in DBX_CENTRAL.conf is filled in and saved automatically,
        // so the user isn't asked to type the same SMTP details twice.
        private UIElement BuildMailSection(WizardContext ctx, string confPath)
        {
            var (detectedHost, detectedTo, detectedFrom) = ctx.DbmsConfigFiles.Count > 0
                ? InstallActions.FindDbmsMailSettings(ctx.DbmsConfigFiles)
                : (null, null, null);

            var stack = new StackPanel();
            stack.Children.Add(PageHelpers.SectionHeader("Mail Settings"));
            stack.Children.Add(BuildDbmsSourceNote(ctx));
            stack.Children.Add(BuildTextFieldRow(ctx, confPath, "Mail hostname", MailHostnameSuffix, detectedHost));
            stack.Children.Add(BuildTextFieldRow(ctx, confPath, "Mail to", MailToSuffix, detectedTo));
            stack.Children.Add(BuildTextFieldRow(ctx, confPath, "Mail from", MailFromSuffix, detectedFrom));
            return stack;
        }

        // Visible diagnostic: exactly which DBMS Configuration file(s) (if any) were searched for
        // mail settings, and whether they still exist on disk — makes a silent "nothing found"
        // (wrong/stale path, or a transient file that's since been deleted) obvious instead of
        // just showing blank fields with no explanation.
        private static TextBox BuildDbmsSourceNote(WizardContext ctx)
        {
            string text = ctx.DbmsConfigFiles.Count == 0
                ? "No DBMS Configuration file detected yet — configure a DBMS server above to auto-fill these " +
                  "fields from mail settings already set there."
                : "Searched for existing mail settings in: " + string.Join("; ", ctx.DbmsConfigFiles.Select(p =>
                      p + (File.Exists(p) ? "" : " (file not found)")));
            var tb = PageHelpers.SelectableDim(text);
            tb.Margin = new Thickness(0, 0, 0, 8);
            return tb;
        }

        // Shared structure for free-text DBX_CENTRAL.conf mail entries: [label] [field] [Save
        // button]. Save uses InstallActions.WriteMailProp, which updates EVERY key in the file
        // matching this suffix — not just one fixed key — since the file may carry separate
        // per-writer-type keys (mail.*, AlarmWriterToMail.*, ReportSenderToMail.*, ...) that all
        // need to stay in sync. Each matching key is updated via the same comment-aware algorithm
        // as the HTTP/HTTPS port rows: an active line updates in place, a commented-out line is
        // uncommented and updated, and — only if nothing matches at all — a new "mail.<suffix>"
        // line is inserted at the top of the file.
        // If autoFillValue is non-null (detected from a DBMS collector's config) and hasn't
        // already been auto-applied once this wizard run, it's filled in AND immediately saved —
        // overwriting whatever's currently there. This intentionally does NOT check "is the
        // current value unset" first: DBX_CENTRAL.conf ships with its own active (non-commented)
        // default values (e.g. "mail.smtp.hostname = localhost"), so that check would almost
        // always find something already "set" and skip the real DBMS-detected value. The
        // AutoFilledMailSuffixes guard on WizardContext makes this a one-time overwrite instead —
        // a later manual edit (via Save below) is preserved on any subsequent visit.
        private UIElement BuildTextFieldRow(WizardContext ctx, string confPath, string label, string suffix, string? autoFillValue)
        {
            bool autoFilled = autoFillValue != null && ctx.AutoFilledMailSuffixes.Add(suffix);
            string? current = InstallActions.ReadMailProp(confPath, suffix);
            string initialValue = autoFilled ? autoFillValue! : (current ?? autoFillValue ?? "");

            var txtValue = PageHelpers.TextBox(initialValue);
            var btnSave = new Button { Content = "Save" };
            var lblStatus = new TextBlock { Margin = new Thickness(8, 0, 0, 0), VerticalAlignment = VerticalAlignment.Center, Visibility = Visibility.Collapsed };

            void Save(string value)
            {
                InstallActions.WriteMailProp(confPath, suffix, value);
                RefreshEditorFromDisk(confPath);
            }

            btnSave.Click += (_, _) =>
            {
                lblStatus.Visibility = Visibility.Visible;
                Save(txtValue.Text.Trim());
                lblStatus.Text = "Saved.";
                lblStatus.SetResourceReference(TextBlock.ForegroundProperty, "GreenBrush");
            };

            // lblStatus is a trailing column of the row itself (like the port rows' availability
            // label), not stacked on its own line below — otherwise it pushes the next field down.
            var row = PageHelpers.Row(label, txtValue, btnSave, lblStatus);

            if (autoFilled)
            {
                Save(initialValue);
                lblStatus.Visibility = Visibility.Visible;
                lblStatus.Text = "Auto-filled and saved from DBMS Configuration.";
                lblStatus.SetResourceReference(TextBlock.ForegroundProperty, "GreenBrush");
            }

            return row;
        }

        private void RefreshEditorFromDisk(string confPath)
        {
            if (_editor == null) return;
            _editor.Text = File.ReadAllText(confPath);
            _editor.SetSavePoint();
        }

        // Reverse direction of the above — called after the full-file editor's own Save, in case
        // the user edited a port line directly in the raw text instead of via one of the fields.
        private void RefreshPortFieldsFromDisk(string confPath)
        {
            if (_txtHttpPort != null)
                _txtHttpPort.Text = InstallActions.ReadJavaPropInt(confPath, HttpPortKey, DefaultHttpPort).ToString();
            if (_txtHttpsPort != null)
                _txtHttpsPort.Text = InstallActions.ReadJavaPropInt(confPath, HttpsPortKey, DefaultHttpsPort).ToString();
        }

        private Grid BuildFileEditor(string confPath)
        {
            var grid = new Grid { Margin = new Thickness(0, 8, 0, 0), MinHeight = 260 };
            grid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
            grid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });

            var (editorElement, sci) = ScintillaHost.Create(File.ReadAllText(confPath), confPath);
            _editor = sci;
            editorElement.Margin = new Thickness(0, 0, 0, 8);
            Grid.SetRow(editorElement, 0);
            grid.Children.Add(editorElement);

            // Unsaved edits block the wizard's own Next until Saved or Reloaded — otherwise the
            // in-progress edit is silently lost when the user moves to the next page.
            var saveRow = ScintillaHost.BuildSaveBar(sci, confPath,
                onDirtyChanged: dirty => SetNextEnabled(!dirty),
                onSaved: () => RefreshPortFieldsFromDisk(confPath),
                includeExternalButton: false);
            Grid.SetRow(saveRow, 1);
            grid.Children.Add(saveRow);

            return grid;
        }

        public override Task<WizardValidationResult> ValidateBeforeNextAsync(WizardContext ctx) =>
            Task.FromResult(WizardValidationResult.Ok());
    }
}

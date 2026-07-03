using System;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using DbxInstaller.Wizard;

namespace DbxInstaller.Pages
{
    internal sealed class PrerequisitesPage : WizardPage
    {
        public override string StepTitle => "Prerequisites";

        private const string JavaDownloadUrl   = "https://docs.microsoft.com/java/openjdk/download";
        private const string DotNetDownloadUrl = "https://dotnet.microsoft.com/download/dotnet/10.0";

        private readonly TextBox _txtJavaExe = PageHelpers.TextBox("java");
        private readonly Button  _btnDetect  = new() { Content = "Auto-detect" };
        private readonly TextBlock _lblJavaStatus = new() { TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 4, 0, 4) };
        private readonly TextBlock _lblDotNetStatus = new() { TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 4, 0, 4) };

        // Repeated next to the status message (below), shown only while that dependency isn't
        // satisfied, so the download link and a way to re-check are right there instead of
        // making the user scroll back up / re-touch the Java path field.
        private readonly TextBlock _javaLink = PageHelpers.Link(JavaDownloadUrl, JavaDownloadUrl);
        private readonly TextBlock _dotNetLink = PageHelpers.Link(DotNetDownloadUrl, DotNetDownloadUrl);
        private readonly Button _btnRecheckJava = new() { Content = "Recheck", Margin = new Thickness(12, 0, 0, 0) };
        private readonly Button _btnRecheckDotNet = new() { Content = "Recheck", Margin = new Thickness(12, 0, 0, 0) };
        private readonly StackPanel _javaActionRow;
        private readonly StackPanel _dotNetActionRow;

        // Cache: avoid re-spawning "java -version" on every Back/Next re-entry unless the path changed.
        private string? _lastCheckedJavaExe;
        private bool _lastJavaOk;

        public PrerequisitesPage()
        {
            _btnDetect.Click += (_, _) => { _txtJavaExe.Text = InstallActions.FindJavaExe().exe; CheckJava(force: true); };
            _txtJavaExe.TextChanged += (_, _) => CheckJava(force: false);
            _btnRecheckJava.Click += (_, _) => CheckJava(force: true);
            _btnRecheckDotNet.Click += (_, _) => CheckDotNet();

            _javaActionRow = new StackPanel { Orientation = Orientation.Horizontal, Visibility = Visibility.Collapsed };
            _javaActionRow.Children.Add(_javaLink);
            _javaActionRow.Children.Add(_btnRecheckJava);

            _dotNetActionRow = new StackPanel { Orientation = Orientation.Horizontal, Visibility = Visibility.Collapsed };
            _dotNetActionRow.Children.Add(_dotNetLink);
            _dotNetActionRow.Children.Add(_btnRecheckDotNet);

            // Download links are always shown here, in plain URL form, even when a dependency is
            // already satisfied — useful for re-downloading, checking on a different machine, etc.
            var depsList = PageHelpers.Stack(
                PageHelpers.BulletWithLink("Java 17 or later — ", JavaDownloadUrl, JavaDownloadUrl),
                PageHelpers.Bullet(".NET 10 Desktop Runtime (already satisfied, since this wizard is running)"),
                PageHelpers.BulletWithLink("ASP.NET Core 10 Runtime, for the DbxStarter web UI — ", DotNetDownloadUrl, DotNetDownloadUrl));
            depsList.Margin = new Thickness(0, 4, 0, 16);

            Content = PageHelpers.Stack(
                PageHelpers.Title("Prerequisites"),
                PageHelpers.Subtitle("DbxTune requires the following on this machine:"),
                depsList,
                PageHelpers.SectionHeader("Java"),
                PageHelpers.Row("Java executable", _txtJavaExe, _btnDetect),
                _lblJavaStatus,
                _javaActionRow,
                PageHelpers.SectionHeader(".NET / ASP.NET Core Runtime"),
                _lblDotNetStatus,
                _dotNetActionRow);
        }

        public override void OnEnter(WizardContext ctx)
        {
            if (string.IsNullOrWhiteSpace(_txtJavaExe.Text))
                _txtJavaExe.Text = InstallActions.FindJavaExe().exe;
            CheckJava(force: false);
            CheckDotNet();
        }

        private void CheckJava(bool force)
        {
            string exe = _txtJavaExe.Text.Trim();
            if (!force && exe == _lastCheckedJavaExe)
            {
                SetNextEnabled(_lastJavaOk);
                return;
            }

            _lastCheckedJavaExe = exe;
            var (_, output) = InstallActions.Run(exe, "-version");
            var match = System.Text.RegularExpressions.Regex.Match(output ?? "", "version \"(?:1\\.)?(\\d+)");
            if (!match.Success)
            {
                _lblJavaStatus.Text = "Not detected — install Java 17+ or browse to java.exe above.";
                _lblJavaStatus.SetResourceReference(TextBlock.ForegroundProperty, "RedBrush");
                _lastJavaOk = false;
            }
            else
            {
                int major = int.Parse(match.Groups[1].Value);
                bool ok = major >= 17;
                _lblJavaStatus.Text = ok ? $"Java {major} — OK." : $"Java {major} detected — Java 17 or later is required.";
                _lblJavaStatus.SetResourceReference(TextBlock.ForegroundProperty, ok ? "GreenBrush" : "RedBrush");
                _lastJavaOk = ok;
            }
            _javaActionRow.Visibility = _lastJavaOk ? Visibility.Collapsed : Visibility.Visible;
            SetNextEnabled(_lastJavaOk);
        }

        private void CheckDotNet()
        {
            bool ok = InstallActions.IsAspNetCore10Installed();
            _lblDotNetStatus.Text = ok
                ? "ASP.NET Core 10 runtime — OK."
                : "ASP.NET Core 10 runtime not found — the DbxStarter web UI will not run until it is installed.";
            _lblDotNetStatus.SetResourceReference(TextBlock.ForegroundProperty, ok ? "GreenBrush" : "OrangeBrush");
            _dotNetActionRow.Visibility = ok ? Visibility.Collapsed : Visibility.Visible;
        }

        public override Task<WizardValidationResult> ValidateBeforeNextAsync(WizardContext ctx)
        {
            CheckJava(force: false);
            return Task.FromResult(_lastJavaOk
                ? WizardValidationResult.Ok()
                : WizardValidationResult.Fail("Java 17 or later is required before continuing."));
        }

        public override void OnLeave(WizardContext ctx)
        {
            ctx.Config.JavaExe = _txtJavaExe.Text.Trim();
        }
    }
}

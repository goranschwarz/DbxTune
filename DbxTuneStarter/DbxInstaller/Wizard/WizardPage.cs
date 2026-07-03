using System;
using System.Threading.Tasks;
using System.Windows.Controls;

namespace DbxInstaller.Wizard
{
    // Base class for every wizard page. Pages are plain WPF UserControls built in code
    // (no XAML) so the whole wizard can be scanned/edited without a designer round-trip.
    internal abstract class WizardPage : UserControl
    {
        public abstract string StepTitle { get; }

        // True for the last page of a page group (Finish pages) — the Next button
        // becomes "Close" and clicking it ends the wizard instead of advancing.
        public virtual bool IsFinishPage => false;

        // Called every time the page becomes the visible page, forward or via Back.
        // Must be idempotent — live checks (port, Java) may re-run on every visit.
        public virtual void OnEnter(WizardContext ctx) { }

        // Called when navigating away (forward or Back) — commit control values into ctx.Config here.
        public virtual void OnLeave(WizardContext ctx) { }

        // Only called when moving forward. Async so live checks (port probe, java -version) don't
        // block the UI thread. Never called on Back — Back never blocks.
        public virtual Task<WizardValidationResult> ValidateBeforeNextAsync(WizardContext ctx) =>
            Task.FromResult(WizardValidationResult.Ok());

        public virtual bool CanGoBack(WizardContext ctx) => true;
        public virtual bool CanCancel(WizardContext ctx) => true;

        public event EventHandler? NextEnabledChanged;

        private bool _nextEnabled = true;
        public bool NextEnabled
        {
            get => _nextEnabled;
            private set
            {
                if (_nextEnabled == value) return;
                _nextEnabled = value;
                NextEnabledChanged?.Invoke(this, EventArgs.Empty);
            }
        }

        // Pages call this to gate Next mid-page (e.g. while an async Java/port check is running).
        protected void SetNextEnabled(bool enabled) => NextEnabled = enabled;
    }

    // Implemented by pages that run background work (Installing/Upgrading/Uninstalling progress
    // pages) so WizardWindow can block Back/Cancel/window-close while a run is in flight.
    internal interface IBusyPage
    {
        bool IsRunning { get; }
    }
}

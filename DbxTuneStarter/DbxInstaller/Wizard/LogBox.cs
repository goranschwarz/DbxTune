using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;

namespace DbxInstaller.Wizard
{
    // A themed, read-only, copyable, per-line-colored log view — used by the Install/Upgrade/
    // Uninstall progress pages. RichTextBox supports selection and Ctrl+C/Ctrl+A just like a
    // plain TextBox, so there's no need to give up per-line color (yellow step headers, green
    // success, red errors) to get a copyable log.
    internal sealed class LogBox
    {
        public RichTextBox Control { get; }
        // A RichTextBox wraps its FlowDocument's paragraphs to fit PageWidth — there's no simple
        // "NoWrap" flag like TextBox has. Setting PageWidth far wider than the control and turning
        // on the horizontal scrollbar effectively disables wrapping: long lines (e.g. wide table
        // output, stack traces) stay on one line and scroll sideways instead of wrapping.
        private readonly FlowDocument _doc = new() { PagePadding = new Thickness(0), PageWidth = 20000 };

        public LogBox()
        {
            Control = new RichTextBox
            {
                IsReadOnly = true,
                IsReadOnlyCaretVisible = true,
                FontFamily = new System.Windows.Media.FontFamily("Consolas"),
                Document = _doc,
                HorizontalScrollBarVisibility = ScrollBarVisibility.Auto,
            };
        }

        public void Clear() => _doc.Blocks.Clear();

        // Normalizes CRLF/CR to LF — raw process output (e.g. from batch scripts) uses CRLF,
        // and a lone trailing '\r' renders as an extra blank paragraph if left in.
        public void AppendLine(string text, string brushKey = "FgBrush")
        {
            string normalized = text.Replace("\r\n", "\n").Replace("\r", "\n");
            foreach (var line in normalized.Split('\n'))
            {
                var run = new Run(line);
                run.SetResourceReference(TextElement.ForegroundProperty, brushKey);
                _doc.Blocks.Add(new Paragraph(run) { Margin = new Thickness(0) });
            }
            Control.ScrollToEnd();
        }
    }
}

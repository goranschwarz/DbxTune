namespace DbxInstaller.Wizard
{
    internal sealed class WizardValidationResult
    {
        public bool IsValid { get; }
        public string? Message { get; }

        private WizardValidationResult(bool isValid, string? message)
        {
            IsValid = isValid;
            Message = message;
        }

        public static WizardValidationResult Ok() => new(true, null);
        public static WizardValidationResult Fail(string message) => new(false, message);
    }
}

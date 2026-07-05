namespace DbxStarterCommon
{
    // Bump these by hand before each release (no build-time auto-stamping),
    // matching the convention used by com.dbxtune.Version on the Java side.
    // Intentionally decoupled from any assembly-level version attributes.
    public static class Version
    {
        public static readonly string ProductString = "DbxStarter";
        public static readonly string VersionString = "1.0.0";
        public static readonly string BuildString   = "2026-07-05";

        public static readonly string VersionAndBuildString = $"{VersionString} ({BuildString})";
    }
}

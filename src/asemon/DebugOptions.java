package asemon;

import asemon.utils.Debug;

public class DebugOptions
{
	public static String EDT_HANG = "EDT_HANG";

	private static boolean _doneInit = false;

	/**
	 * Register all known DEBUG options in the Debug object.
	 */
	public static void init()
	{
		if (_doneInit)
			return;

		Debug.addKnownDebug(EDT_HANG, "Install a Swing EDT (Event Dispatch Thread) - hook, that check for deadlocks, traces task that takes to long to execute by the Swing Event Dispatch Thread.");
		
		_doneInit = true;
	}

	static
	{
		init();
	}
}

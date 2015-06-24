package com.asetune.tools;

public enum WindowType
{
	/** Create the "window" using a JFrame, meaning it would have a Icon in the Task bar, but from CmdLine */
	CMDLINE_JFRAME, 

	/** Create the "window" using a JFrame, meaning it would have a Icon in the Task bar */
	JFRAME, 

	/** Create the "window" using a JDialog, meaning it would NOT have a Icon in the Task bar */
	JDIALOG, 

	/** Create the "window" using a JDialog, with modal option set to true. */
	JDIALOG_MODAL 
}

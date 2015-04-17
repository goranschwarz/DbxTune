package com.asetune.sql.conn.info;

public interface DbxConnectionStateInfo
{
	/**
	 * If not in normal state the methods (getWaterMarkText, getStatusBarText, getStatusBarToolTipText) could be called to get more information about what's not so normal...
	 * @return true = NORMAL, false = Something isn't normal
	 */
	public boolean isNormalState();
	
	/**
	 * Set some GUI background text in the GUI if there are problems
	 * @return null or "" if nothing to show, else the text in <b>plain</b> text.
	 */
	public String getWaterMarkText();
	
	/**
	 * Get a information or warning text that can be used in a GUI status bar<br>
	 * Use HTML string if you want to have other colors in the text
	 * @return
	 */
	public String getStatusBarText();

	/**
	 * Get a information or warning text that can be used in a GUI status bar, to display a tooltip to get more detailed information about the connection.<br>
	 * Use HTML string if you want to have other colors in the text, use tables etc...
	 * @return 
	 */
	public String getStatusBarToolTipText();
}

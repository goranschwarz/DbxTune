/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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

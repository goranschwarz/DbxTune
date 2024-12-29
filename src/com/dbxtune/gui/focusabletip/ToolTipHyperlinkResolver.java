/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.gui.focusabletip;

import javax.swing.event.HyperlinkEvent;

/**
 * The resolver could be used in conjunction with the ToolTipSupplier to help you 
 * build a better ToolTip navigation, especially if you will create your own
 * HTML texts within the ToolTip.
 * 
 * @author Goran Schwarz
 */
public interface ToolTipHyperlinkResolver
{
	
	/**
	 * This is called when the TipWindow <code>HyperlinkListener</code> is called, and the <code>HyperlinkEvent.EventType</code> is <code>EventType.ACTIVATED</code>
	 * <p>
	 * With this method you can decide what you want to do with the event.<br>
	 * Example of thing you might want to do:
	 * <ul>
	 * <li>Instead of the clicked link, you might want to return a HTML String, which should be displayed in the tooltip (instead of the link)<br>
	 *     This would be good if you have your own internal anchor tags, where you would display your own help text.</li>
	 * <li>If you want to return some other page than the user clicked on, return the URL string you want the user to load.</li>
	 * <li>If you don't want to load the link clicked on, simply return <code>null</code></li>
	 * </ul> 
	 * @param event The event passed on from the <code>HyperlinkListener</code>, when a link is clicked <code>EventType.ACTIVATED</code>
	 * @return a ResolverReturn
	 */
	ResolverReturn hyperlinkResolv(HyperlinkEvent event);
}

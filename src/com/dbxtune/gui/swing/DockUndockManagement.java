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
package com.dbxtune.gui.swing;

import javax.swing.JButton;

public interface DockUndockManagement
{
//	/** Sets the button that could be used to dock/undock */
//	public void setDockUndockButton(JButton button);

	/** 
	 * Get a button that should be used to dock/undock<p> 
	 * The default GUI rules will be applied for the button. 
	 * Default GUI = no text, no border, Icon is fetched using getWindow{Dock|Undock}Icon() 
	 */
	public JButton getDockUndockButton();

	/**
	 * called just before the component is docked back into the TabbedPane
	 * @return true if we allow the dock operation
	 */
	public boolean beforeDock();

	/**
	 * called after the component has been docked back into the TabbedPane
	 */
	public void afterDock();

	/**
	 * called just before the component is Undocked to its own frame
	 * @return true if we allow the undock operation
	 */
	public boolean beforeUndock();

	/**
	 * called after the component has been undocked to its own frame
	 */
	public void afterUndock();

	/**
	 * 
	 */
	public void saveWindowProps(GTabbedPaneWindowProps winProps);
	public GTabbedPaneWindowProps getWindowProps();
}

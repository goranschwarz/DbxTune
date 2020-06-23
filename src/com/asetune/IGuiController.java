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
package com.asetune;

import java.awt.Component;

import javax.swing.JPanel;

import com.asetune.gui.DbmsVersionPanelAbstract;
import com.asetune.gui.ShowCmPropertiesDialog;
import com.asetune.gui.swing.GTabbedPane;

public interface IGuiController
{

	boolean hasGUI();

	void addPanel(JPanel panel);
//	void addPanel(TabularCntrPanel panel);
//	void addPanel(ISummaryPanel panel);

	public Component   getActiveTab();
	public GTabbedPane getTabbedPane();
	
	public void splashWindowProgress(String msg);

	/**
	 * Sets values in the status panel.
	 * @param type <code>ST_CONNECT, ST_DISCONNECT, ST_STATUS_FIELD, ST_MEMORY</code>
	 */
	public void setStatus(int type);

	/**
	 * Sets values in the status panel.
	 * @param type <code>ST_CONNECT, ST_DISCONNECT, ST_STATUS_FIELD, ST_MEMORY</code>
	 * @param param The actual string to set (this is only used for <code>ST_STATUS_FIELD</code>)
	 */
	public void setStatus(int type, String param);
	
	/**
	 * Get GUI "main" window, so it can be used for various message windows etc
	 * @return
	 */
	public Component getGuiHandle();

	/**
	 * A part of the properties entry key to get valid entries for the Table right click popup menu.
	 * @return for example: "ase", "iq", "rs", "hana", "oracle", "sqlserver"
	 */
	public String getTablePopupDbmsVendorString();

	/**
	 * Create a DBMS Version specific Version Panel, which is used in TabularCntrlPanel when clicking 'Properties...'
	 * @param showCmPropertiesDialog
	 * @return
	 */
	DbmsVersionPanelAbstract createDbmsVersionPanel(ShowCmPropertiesDialog showCmPropertiesDialog);
}

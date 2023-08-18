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
package com.asetune.gui;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.asetune.Version;
import com.asetune.config.dbms.DbmsConfigManager;
import com.asetune.config.dbms.DbmsConfigTextManager;
import com.asetune.config.dbms.IDbmsConfig;
import com.asetune.config.dbms.IDbmsConfigText;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.config.dict.MonTablesDictionaryMySql;
import com.asetune.gui.ConnectionDialog.Options;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.hostmon.HostMonitorConnection;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.SwingUtils;

public class MainFrameMySql 
extends MainFrame
{
	private static final long serialVersionUID = 1L;

	public MainFrameMySql()
	{
		super();
	}

	@Override
	public int getDefaultRefreshInterval()
	{
		return 20;
	}

	@Override public String    getTablePopupDbmsVendorString() { return "mysql"; }
	@Override public ImageIcon getApplicationIcon16() { return SwingUtils.readImageIcon(Version.class, "images/mysqltune_icon_16.png"); };
	@Override public ImageIcon getApplicationIcon32() { return SwingUtils.readImageIcon(Version.class, "images/mysqltune_icon_32.png"); };

	@Override
	public Options getConnectionDialogOptions()
	{
		Options options = new Options();

		options._showAseTab               = false;
		options._showDbxTuneOptionsInTds  = false;
		options._showHostmonTab           = true;
		options._showOfflineTab           = true;
		options._showPcsTab               = true;

		options._showJdbcTab              = true;
		options._showDbxTuneOptionsInJdbc = true;

		options._srvExtraChecks = createConnectionProgressExtraActions();
		
		return options;
	}

	public ConnectionProgressExtraActions createConnectionProgressExtraActions()
	{
		return new ConnectionProgressExtraActions()
		{
			@Override public boolean doInitializeVersionInfo()        { return true; } 
			@Override public boolean initializeVersionInfo(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				MonTablesDictionaryManager.getInstance().initializeVersionInfo(conn, true);
				return true;
			}
			
			@Override public boolean doCheckMonitorConfig()           { return false; } 
			@Override public boolean checkMonitorConfig(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				return true;
			}

			@Override public boolean doInitMonitorDictionary()        { return true; } 
			@Override public boolean initMonitorDictionary(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				MonTablesDictionaryManager.getInstance().initialize(conn, true);
				return true;
			}
			
			@Override public boolean doInitDbServerConfigDictionary() { return true; } 
			@Override public boolean initDbServerConfigDictionary(DbxConnection conn, HostMonitorConnection hostMonConn, ConnectionProgressDialog cpd) throws Exception
			{
				if (DbmsConfigManager.hasInstance())
				{
					cpd.setStatus("Getting MySQL Configurations");
					IDbmsConfig dbmsCfg = DbmsConfigManager.getInstance();
					if ( ! dbmsCfg.isInitialized() )
						dbmsCfg.initialize(conn, true, false, null);
				}
				if (DbmsConfigTextManager.hasInstances())
				{
					List<IDbmsConfigText> list = DbmsConfigTextManager.getInstanceList();
					for (IDbmsConfigText t : list)
					{
						if ( ! t.isInitialized() )
						{
							cpd.setStatus("Getting '"+t.getTabLabel()+"' settings");
							t.initialize(conn, hostMonConn, true, false, null);
						}
					}

					cpd.setStatus("");
				}
				return true;
			}
			
			@Override public boolean doInitCounterCollector()         { return false; } 
			@Override public boolean initCounterCollector(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				return true;
			}			
		};
	}

	@Override
	public void connectMonitorHookin()
	{
	}

	@Override
	public void connectOfflineHookin()
	{
		MonTablesDictionaryMySql.initExtraMonTablesDictionary();
	}

	@Override
	public boolean disconnectAbort(boolean canBeAborted)
	{
		return false;
	}

	@Override
	public void disconnectHookin(WaitForExecDialog waitDialog)
	{
	}

	@Override
	public void actionPerformed(ActionEvent e, Object source, String actionCmd)
	{
	}

	/**
	 * get icon for a specific group
	 * @param groupName
	 * @return an icon for the specified group, null if unknown/undefined group
	 */
	@Override
	protected Icon getGroupIcon(String groupName)
	{
		if (TCP_GROUP_SERVER.equals(groupName)) 
			return TCP_GROUP_ICON_SERVER;
		else
			return super.getGroupIcon(groupName);
	}
	public static final ImageIcon TCP_GROUP_ICON_SERVER        = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_mysqlserver.png");

	@Override
	protected boolean addTabGroup(String groupName)
	{
		if (TCP_GROUP_REP_AGENT.equals(groupName))
			return false;
		return super.addTabGroup(groupName);
	}


	@Override
	public DbmsVersionPanelAbstract createDbmsVersionPanel(ShowCmPropertiesDialog showCmPropertiesDialog)
	{
		return new DbmsVersionPanelMySql(showCmPropertiesDialog);
	}
}

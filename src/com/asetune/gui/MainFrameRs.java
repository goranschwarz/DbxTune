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
import java.sql.SQLException;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.asetune.Version;
import com.asetune.config.dbms.DbmsConfigManager;
import com.asetune.config.dbms.DbmsConfigTextManager;
import com.asetune.config.dbms.IDbmsConfig;
import com.asetune.config.dbms.IDbmsConfigText;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.ConnectionDialog.Options;
import com.asetune.gui.swing.GTabbedPane;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.hostmon.HostMonitorConnection;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.SwingUtils;

public class MainFrameRs 
extends MainFrame
{
	private static final long serialVersionUID = 1L;

	public MainFrameRs()
	{
		super();
	}

	@Override
	public int getDefaultRefreshInterval()
	{
		return 30;
	}

	@Override public String    getTablePopupDbmsVendorString() { return "rs"; }
	@Override public ImageIcon getApplicationIcon16() { return SwingUtils.readImageIcon(Version.class, "images/rstune_icon_16.png"); };
	@Override public ImageIcon getApplicationIcon32() { return SwingUtils.readImageIcon(Version.class, "images/rstune_icon_32.png"); };

	@Override
	public Options getConnectionDialogOptions()
	{
		Options options = new Options();

		options._showAseTab               = true;
		options._showDbxTuneOptionsInTds  = true;
		options._showHostmonTab           = true;
		options._showOfflineTab           = true;
		options._showPcsTab               = true;

		options._showJdbcTab              = false;
		options._showDbxTuneOptionsInJdbc = false;

		options._srvExtraChecks = createConnectionProgressExtraActions();
		
		return options;
	}
	public ConnectionProgressExtraActions createConnectionProgressExtraActions()
	{
		return new ConnectionProgressExtraActions()
		{
			@Override public boolean doInitializeVersionInfo() { return true; } 
			@Override public boolean initializeVersionInfo(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				// Just get ASE Version, this will be good for error messages, sent to WEB server, this will write ASE Version in the info...
				MonTablesDictionaryManager.getInstance().initializeVersionInfo(conn, true);
				return true;
			}
			
			@Override public boolean doCheckMonitorConfig() { return false; } 
			@Override public boolean checkMonitorConfig(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				return true;
			}

			@Override public boolean doInitMonitorDictionary() { return false; } 
			@Override public boolean initMonitorDictionary(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				return true;
			}
			
			@Override public boolean doInitDbServerConfigDictionary() { return true; } 
			@Override public boolean initDbServerConfigDictionary(DbxConnection conn, HostMonitorConnection hostMonConn, ConnectionProgressDialog cpd) throws SQLException
			{
				if (DbmsConfigManager.hasInstance())
				{
					cpd.setStatus("Getting 'admin config' settings");
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
			
			@Override public boolean doInitCounterCollector() { return false; } 
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
	}

//	@Override
//	public void sendConnectInfoNoBlock(int connType, SshTunnelInfo sshTunnelInfo)
//	{
////		CheckForUpdates.sendConnectInfoNoBlock(connType, sshTunnelInfo);
//		System.out.println("NOT_YET_IMPLEMENTED: sendConnectInfoNoBlock(connType="+connType+", sshTunnelInfo='"+sshTunnelInfo+"')");
//	}
//
//	@Override
//	public void sendCounterUsageInfo(boolean blockingCall)
//	{
////		CheckForUpdates.sendCounterUsageInfo(blockingCall);
//		System.out.println("NOT_YET_IMPLEMENTED: sendCounterUsageInfo(blockingCall="+blockingCall+")");
//	}

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

	@Override
	public GTabbedPane createGroupTabbedPane(GTabbedPane mainTabbedPane)
	{
		mainTabbedPane = super.createGroupTabbedPane(mainTabbedPane);
		
		GTabbedPane tabGroupMc = new GTabbedPane("MainFrame_TabbedPane_Mc");

		// Lets do setTabLayoutPolicy for all sub tabs...
		tabGroupMc.setTabLayoutPolicy(mainTabbedPane.getTabLayoutPolicy());

		int insLocation = mainTabbedPane.indexOfTab(TCP_GROUP_HOST_MONITOR);
		if (addTabGroup(TCP_GROUP_MC))
			mainTabbedPane.insertTab(TCP_GROUP_MC, getGroupIcon(TCP_GROUP_MC), tabGroupMc, getGroupToolTipText(TCP_GROUP_MC), insLocation);
		
		return mainTabbedPane;
	}

	/**
	 * get icon for a specific group
	 * @param groupName
	 * @return an icon for the specified group, null if unknown/undefined group
	 */
	@Override
	protected Icon getGroupIcon(String groupName)
	{
		if      (TCP_GROUP_SERVER.equals(groupName)) return TCP_GROUP_ICON_SERVER;
		else if (TCP_GROUP_MC    .equals(groupName)) return TCP_GROUP_ICON_MC;
		else                                         return super.getGroupIcon(groupName);
	}
	public static final ImageIcon TCP_GROUP_ICON_SERVER = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_repserver.png");
	public static final ImageIcon TCP_GROUP_ICON_MC     = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_repserver_mc.png");

	public static final String    TCP_GROUP_MC          = "Monitor & Counters";

	@Override
	protected boolean addTabGroup(String groupName)
	{
		if      (TCP_GROUP_OBJECT_ACCESS.equals(groupName)) return false;
		if      (TCP_GROUP_CACHE        .equals(groupName)) return false;
		else if (TCP_GROUP_REP_AGENT    .equals(groupName)) return false;

		return super.addTabGroup(groupName);
	}
	@Override
	protected String getGroupToolTipText(String groupName)
	{
		if (TCP_GROUP_MC.equals(groupName)) return "<html>RS Monitor & Performace Counters</html>";
		else return super.getGroupToolTipText(groupName);
	}


	@Override
	public DbmsVersionPanelAbstract createDbmsVersionPanel(ShowCmPropertiesDialog showCmPropertiesDialog)
	{
		return new DbmsVersionPanelRs(showCmPropertiesDialog);
	}
}

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
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.Version;
import com.asetune.cache.DbmsObjectIdCache;
import com.asetune.cache.DbmsObjectIdCacheSqlServer;
import com.asetune.config.dbms.DbmsConfigManager;
import com.asetune.config.dbms.DbmsConfigTextManager;
import com.asetune.config.dbms.IDbmsConfig;
import com.asetune.config.dbms.IDbmsConfigText;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.ConnectionDialog.Options;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.SwingUtils;

public class MainFrameSqlServer 
extends MainFrame
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger = Logger.getLogger(MainFrameSqlServer.class);

	public MainFrameSqlServer()
	{
		super();
	}

	@Override
	public int getDefaultRefreshInterval()
	{
		return 20;
	}

	@Override public String    getTablePopupDbmsVendorString() { return "sqlserver"; }
	@Override public ImageIcon getApplicationIcon16() { return SwingUtils.readImageIcon(Version.class, "images/sqlservertune_icon_16.png"); };
	@Override public ImageIcon getApplicationIcon32() { return SwingUtils.readImageIcon(Version.class, "images/sqlservertune_icon_32.png"); };

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
			@Override public boolean doInitializeVersionInfo() { return true; } 
			@Override public boolean initializeVersionInfo(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
//				// Just get DBMS Version, this will be good for error messages, sent to WEB server, this will write DBMS Version in the info...
				MonTablesDictionaryManager.getInstance().initializeVersionInfo(conn, true);
				return true;
			}
			
			@Override public boolean doCheckMonitorConfig() { return true; } 
			@Override public boolean checkMonitorConfig(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				Thread.sleep(1*1000);
				return true;
//				return AseConnectionUtils.checkForMonitorOptions(conn, null, true, cpd, "enable monitoring");
			}

			@Override public boolean doInitMonitorDictionary() { return true; } 
			@Override public boolean initMonitorDictionary(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				MonTablesDictionaryManager.getInstance().initialize(conn, true);
//				Thread.sleep(1*1000);
				return true;
			}
			
			@Override public boolean doInitDbServerConfigDictionary() { return true; } 
			@Override public boolean initDbServerConfigDictionary(DbxConnection conn, ConnectionProgressDialog cpd) throws SQLException
			{
				if (DbmsConfigManager.hasInstance())
				{
					cpd.setStatus("Getting SQL-Server Configurations");
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
							t.initialize(conn, true, false, null);
						}
					}

					cpd.setStatus("");
				}
				return true;
			}
			
			@Override public boolean doInitCounterCollector() { return true; } 
			@Override public boolean initCounterCollector(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				CounterController.getInstance().initCounters(
						conn,
						true,
						MonTablesDictionaryManager.getInstance().getDbmsExecutableVersionNum(),
						MonTablesDictionaryManager.getInstance().isClusterEnabled(),
						MonTablesDictionaryManager.getInstance().getDbmsMonTableVersion());

//				Thread.sleep(1*1000);
				return true;
			}			
		};
	}

	@Override
	public void connectMonitorHookin()
	{
		Connection conn = CounterController.getInstance().getMonConnection();

		//------------------------------------------------
		// Set some options
		//   -- set deadlock priority LOW or similar... (SET DEADLOCK_PRIORITY LOW)
		//      if SqlServerTune is involved in a DEADLOCK, then the "other" SPID will have a higher chance to "win"
		//      SET DEADLOCK_PRIORITY { LOW | NORMAL | HIGH | <numeric-priority> | @deadlock_var | @deadlock_intvar }  
		//          <numeric-priority> ::= { -10 | -9 | -8 | ... | 0 | ... | 8 | 9 | 10 }
		//          LOW=-5, NORMAL=9, HIGH=5
		// see: https://docs.microsoft.com/en-us/sql/t-sql/statements/set-deadlock-priority-transact-sql?view=sql-server-ver15
		String sql = "SET DEADLOCK_PRIORITY LOW"; // should we go for -7 or -8 to be even more submissive
		try (Statement stmnt = conn.createStatement() )
		{
			stmnt.executeUpdate(sql);
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems in connectMonitorHookin(): When Initializing DBMS SET Properties, using sql='" + sql + "'. Continuing... Caught: MsgNum=" + ex.getErrorCode() + ": " + ex);
		}
		

		// DBMS ObjectID --> ObjectName Cache... maybe it's not the perfect place to initialize this...
		DbmsObjectIdCache.setInstance( new DbmsObjectIdCacheSqlServer(this) );
		
		// Populate Object ID Cache
		if (DbmsObjectIdCache.hasInstance() && DbmsObjectIdCache.getInstance().isBulkLoadOnStartEnabled())
			DbmsObjectIdCache.getInstance().getBulk(null); // null == ALL Databases
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
	public static final ImageIcon TCP_GROUP_ICON_SERVER        = SwingUtils.readImageIcon(Version.class, "images/sqlserver_16.png");

	@Override
	protected boolean addTabGroup(String groupName)
	{
		if (TCP_GROUP_REP_AGENT.equals(groupName))
			return false;
//		if (TCP_GROUP_HOST_MONITOR.equals(groupName))
//			return false;
		return super.addTabGroup(groupName);
	}

	@Override
	public DbmsVersionPanelAbstract createDbmsVersionPanel(ShowCmPropertiesDialog showCmPropertiesDialog)
	{
		return new DbmsVersionPanelSqlServer(showCmPropertiesDialog);
	}
}

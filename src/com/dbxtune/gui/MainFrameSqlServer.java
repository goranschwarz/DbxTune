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
package com.dbxtune.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.Version;
import com.dbxtune.cache.DbmsObjectIdCache;
import com.dbxtune.cache.DbmsObjectIdCacheSqlServer;
import com.dbxtune.cm.sqlserver.TempdbUsagePerSpid;
import com.dbxtune.config.dbms.DbmsConfigManager;
import com.dbxtune.config.dbms.DbmsConfigTextManager;
import com.dbxtune.config.dbms.IDbmsConfig;
import com.dbxtune.config.dbms.IDbmsConfigText;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.ConnectionDialog.Options;
import com.dbxtune.gui.swing.WaitForExecDialog;
import com.dbxtune.hostmon.HostMonitorConnection;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.tools.sqlw.QueryWindow;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

public class MainFrameSqlServer 
extends MainFrame
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

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
		boolean doInitializeVersionInfo        = true;
		boolean doCheckMonitorConfig           = true;
		boolean doInitMonitorDictionary        = true;
		boolean doInitDbServerConfigDictionary = true;
		boolean doInitCounterCollector         = true;

		return new ConnectionProgressExtraActionsAbstract(doInitializeVersionInfo, doCheckMonitorConfig, doInitMonitorDictionary, doInitDbServerConfigDictionary, doInitCounterCollector)
		{
//			@Override public boolean doInitializeVersionInfo() { return true; } 
			@Override public boolean initializeVersionInfo(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
//				// Just get DBMS Version, this will be good for error messages, sent to WEB server, this will write DBMS Version in the info...
				MonTablesDictionaryManager.getInstance().initializeVersionInfo(conn, true);
				return true;
			}
			
//			@Override public boolean doCheckMonitorConfig() { return true; } 
			@Override public boolean checkMonitorConfig(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				Thread.sleep(1*1000);
				return true;
//				return AseConnectionUtils.checkForMonitorOptions(conn, null, true, cpd, "enable monitoring");
			}

//			@Override public boolean doInitMonitorDictionary() { return true; } 
			@Override public boolean initMonitorDictionary(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				MonTablesDictionaryManager.getInstance().initialize(conn, true);
//				Thread.sleep(1*1000);
				return true;
			}
			
//			@Override public boolean doInitDbServerConfigDictionary() { return true; } 
			@Override public boolean initDbServerConfigDictionary(DbxConnection conn, HostMonitorConnection hostMonConn, ConnectionProgressDialog cpd) throws SQLException
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
							t.initialize(conn, hostMonConn, true, false, null);
						}
					}

					cpd.setStatus("");
				}
				return true;
			}
			
//			@Override public boolean doInitCounterCollector() { return true; } 
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
		// DBMS ObjectID --> ObjectName Cache... maybe it's not the perfect place to initialize this...
		DbmsObjectIdCache.setInstance( new DbmsObjectIdCacheSqlServer(this) );
		
		// Populate Object ID Cache
		if (DbmsObjectIdCache.hasInstance() && DbmsObjectIdCache.getInstance().isBulkLoadOnStartEnabled())
			DbmsObjectIdCache.getInstance().getBulk(null); // null == ALL Databases
		else
			_logger.info("Skipping BULK load of ObjectId's at connectMonitorHookin(), isBulkLoadOnStartEnabled() was NOT enabled. Property '" + DbmsObjectIdCacheSqlServer.PROPKEY_BulkLoadOnStart + "=true|false'.");
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
		TempdbUsagePerSpid.getInstance().close();
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




	private JMenu               _installProcs_m;

	@Override
	protected JMenu createToolsMenu()
	{
		JMenu menu = super.createToolsMenu();

		_installProcs_m               = createInstallProcsMenu(this);

		if (_installProcs_m != null) 
			menu.add(_installProcs_m,      7);

		return menu;
	}

	@Override
	public void setMenuMode(int status)
	{
		super.setMenuMode(status);

		if (status == ST_CONNECT)
		{
			// TOOLBAR

			// File

			// View
//			_aseConfigView_mi             .setEnabled(true);
			
			// Tools
			_installProcs_m              .setEnabled(true);

			// Help
		}
		else if (status == ST_OFFLINE_CONNECT)
		{
			// TOOLBAR

			// File

			// View
//			_aseConfigView_mi             .setEnabled(true);

			// Tools
			_installProcs_m              .setEnabled(false);

			// Help
		}
		else if (status == ST_DISCONNECT)
		{
			// TOOLBAR

			// File

			// View
//			_aseConfigView_mi             .setEnabled(false);

			// Tools
			_installProcs_m              .setEnabled(false);

			// Help
		}
	}





	private static String propPrefix(int item)
	{
		return "system.install.sql." + String.format("%03d", item);
	}

	public static JMenu createInstallProcsMenu(final Object callerInstance)
	{
		_logger.debug("createInstallProcsMenu(): called.");

		final JMenu menu = new JMenu("<html>Install some extra <i>system</i> stored procedures</html>");
		menu.setToolTipText("<html>Install some Extra stored procedures...<br>The user you are logged in as need to have the correct priviliges to create procedues in the master database.</html>");;
		menu.setIcon(SwingUtils.readImageIcon(Version.class, "images/pre_defined_sql_statement.png"));

		Configuration systmp = new Configuration();
		
		int item;
		//------------------------------------------------------------------
		item = 0;
		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Adam Machanic -- sp_WhoIsActive</b> - <i><font color=\"green\">main branch</font></i></html>");
		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://raw.githubusercontent.com/amachanic/sp_whoisactive/refs/heads/master/sp_WhoIsActive.sql");

		//------------------------------------------------------------------
		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "separator");

		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Brent Ozar -- Install-All-Scripts</b> - <i><font color=\"green\">main branch</font></i></html>");
		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://raw.githubusercontent.com/BrentOzarULTD/SQL-Server-First-Responder-Kit/refs/heads/main/Install-All-Scripts.sql");

		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Brent Ozar -- ONLY: sp_Blitz</b> - <i><font color=\"green\">main branch</font></i></html>");
		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://raw.githubusercontent.com/BrentOzarULTD/SQL-Server-First-Responder-Kit/refs/heads/main/sp_Blitz.sql");

		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Brent Ozar -- ONLY: sp_BlitzCache</b> - <i><font color=\"green\">main branch</font></i></html>");
		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://raw.githubusercontent.com/BrentOzarULTD/SQL-Server-First-Responder-Kit/refs/heads/main/sp_BlitzCache.sql");

		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Brent Ozar -- ONLY: sp_BlitzFirst</b> - <i><font color=\"green\">main branch</font></i></html>");
		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://https://raw.githubusercontent.com/BrentOzarULTD/SQL-Server-First-Responder-Kit/refs/heads/main/sp_BlitzFirst.sql");

		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Brent Ozar -- ONLY: sp_BlitzIndex</b> - <i><font color=\"green\">main branch</font></i></html>");
		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://raw.githubusercontent.com/BrentOzarULTD/SQL-Server-First-Responder-Kit/refs/heads/main/sp_BlitzIndex.sql");

		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Brent Ozar -- ONLY: sp_BlitzLock</b> - <i><font color=\"green\">main branch</font></i></html>");
		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://raw.githubusercontent.com/BrentOzarULTD/SQL-Server-First-Responder-Kit/refs/heads/main/sp_BlitzLock.sql");

		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Brent Ozar -- ONLY: sp_BlitzWho</b> - <i><font color=\"green\">main branch</font></i></html>");
		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://raw.githubusercontent.com/BrentOzarULTD/SQL-Server-First-Responder-Kit/refs/heads/main/sp_BlitzWho.sql");

		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Brent Ozar -- Uninstall-All</b> - <i><font color=\"green\">main branch</font></i></html>");
		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://raw.githubusercontent.com/BrentOzarULTD/SQL-Server-First-Responder-Kit/refs/heads/main/Uninstall.sql");

		// NOTE: dev/Install-All-Scripts.sql is NOT Updated... Only the individual sp_xxx ... So no Really need for this...
//		item++;
//		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Brent Ozar -- Install-All-Scripts</b> - <i><font color=\"green\">DEVELOPMENT branch</font></i></html>");
//		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
//		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://raw.githubusercontent.com/BrentOzarULTD/SQL-Server-First-Responder-Kit/refs/heads/dev/Install-All-Scripts.sql");

		//------------------------------------------------------------------
		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "separator");

		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Erik Darling -- Install-All</b> - <i><font color=\"green\">main branch</font></i></html>");
		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://raw.githubusercontent.com/erikdarlingdata/DarlingData/refs/heads/main/Install-All/DarlingData.sql");

		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Erik Darling -- ONLY: sp_QuickieStore</b> - <i><font color=\"green\">main branch</font></i></html>");
		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://raw.githubusercontent.com/erikdarlingdata/DarlingData/refs/heads/main/sp_QuickieStore/sp_QuickieStore.sql");

		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Erik Darling -- ONLY: sp_PressureDetector</b> - <i><font color=\"green\">main branch</font></i></html>");
		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://raw.githubusercontent.com/erikdarlingdata/DarlingData/refs/heads/main/sp_PressureDetector/sp_PressureDetector.sql");

		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Erik Darling -- ONLY: sp_HumanEvents</b> - <i><font color=\"green\">main branch</font></i></html>");
		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://raw.githubusercontent.com/erikdarlingdata/DarlingData/refs/heads/main/sp_HumanEvents/sp_HumanEvents.sql");

		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Erik Darling -- ONLY: sp_HealthParser</b> - <i><font color=\"green\">main branch</font></i></html>");
		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://raw.githubusercontent.com/erikdarlingdata/DarlingData/refs/heads/main/sp_HealthParser/sp_HealthParser.sql");

		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Erik Darling -- ONLY: sp_LogHunter</b> - <i><font color=\"green\">main branch</font></i></html>");
		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://raw.githubusercontent.com/erikdarlingdata/DarlingData/refs/heads/main/sp_LogHunter/sp_LogHunter.sql");

		//------------------------------------------------------------------
		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "separator");

		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Ola Hallengren -- Install-All</b> - <i><font color=\"green\">main branch</font></i></html>");
		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://raw.githubusercontent.com/olahallengren/sql-server-maintenance-solution/refs/heads/main/MaintenanceSolution.sql");

		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Ola Hallengren -- ONLY: Database Backup</b> - <i><font color=\"green\">main branch</font></i></html>");
		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://raw.githubusercontent.com/olahallengren/sql-server-maintenance-solution/refs/heads/main/DatabaseBackup.sql");

		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Ola Hallengren -- ONLY: Database Integrity Check</b> - <i><font color=\"green\">main branch</font></i></html>");
		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://raw.githubusercontent.com/olahallengren/sql-server-maintenance-solution/refs/heads/main/DatabaseIntegrityCheck.sql");

		item++;
		systmp.setProperty(propPrefix(item) + ".name",                        "<html><b>Ola Hallengren -- ONLY: Index and Statistics Maintenance</b> - <i><font color=\"green\">main branch</font></i></html>");
		systmp.setProperty(propPrefix(item) + ".install.dbname",              "master");
		systmp.setProperty(propPrefix(item) + ".install.scriptLocation",      "https://raw.githubusercontent.com/olahallengren/sql-server-maintenance-solution/refs/heads/main/IndexOptimize.sql");

		
		createPredefinedSqlMenu(menu, "system.install.sql.", systmp, callerInstance);
		createPredefinedSqlMenu(menu, "user.install.sql.",   null,   callerInstance);

		if ( menu.getMenuComponentCount() == 0 )
		{
			_logger.warn("No Menuitems has been assigned for the '"+menu.getText()+"'.");
			return null;

//			JMenuItem empty = new JMenuItem("No Predefined SQL Statements available.");
//			empty.setEnabled(false);
//			menu.add(empty);
//
//			return menu;
		}
		else
			return menu;
	}

	/**
	 * 
	 * @param menu   if null a new JMenuPopup will be created otherwise it will be appended to it
	 * @param prefix prefix of the property string. Should contain a '.' at the end
	 * @param conf
	 * @param sqlWindowInstance can be null
	 * @return
	 */
	protected static JMenu createPredefinedSqlMenu(JMenu menu, String prefix, Configuration conf, final Object callerInstance)
	{
		if (prefix == null)           throw new IllegalArgumentException("prefix cant be null.");
		if (prefix.trim().equals("")) throw new IllegalArgumentException("prefix cant be empty.");
		prefix = prefix.trim();
		if ( ! prefix.endsWith(".") )
			prefix = prefix + ".";

		if (conf == null)
			conf = Configuration.getCombinedConfiguration();

		_logger.debug("createMenu(): prefix='"+prefix+"'.");		

		//Create the menu, if it didnt exists. 
		if (menu == null)
			menu = new JMenu();

		boolean firstAdd = true;
		for (String prefixStr : conf.getUniqueSubKeys(prefix, true))
		{
			_logger.debug("createPredefinedSqlMenu(): found prefix '"+prefixStr+"'.");

			// Read properties
			final String menuItemName      = conf.getProperty(    prefixStr + ".name");
			final String dbname            = conf.getProperty(    prefixStr + ".install.dbname"); 
			final String scriptLocationStr = conf.getProperty(    prefixStr + ".install.scriptLocation"); 

			if ("separator".equalsIgnoreCase(menuItemName))
			{
				menu.addSeparator();
			}
			
			//---------------------------------------
			// Check that we got everything we needed
			//---------------------------------------
			if (menuItemName == null)
			{
				_logger.warn("Missing property '"+prefixStr+".name'");
				continue;
			}
			if (scriptLocationStr == null)
			{
				_logger.warn("Missing property '"+prefixStr+".install.scriptLocation'");
				continue;
			}

			//---------------------------------------
			// Create the executor class
			//---------------------------------------
			ActionListener action = new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					DbxConnection conn = null;

					QueryWindow queryWindowInstance = null;
					MainFrame   dbxTuneInstance     = null;
					
					if (callerInstance != null)
					{
						if (callerInstance instanceof QueryWindow) queryWindowInstance = (QueryWindow) callerInstance;
						if (callerInstance instanceof MainFrame)   dbxTuneInstance     = (MainFrame) callerInstance;

						if (dbxTuneInstance != null)
						{
							// Check that we are connected
//							if ( ! AseTune.getCounterCollector().isMonConnected(true, true) )
							if ( ! CounterController.getInstance().isMonConnected(true, true) )
							{
								SwingUtils.showInfoMessage(MainFrame.getInstance(), "Not connected", "Not yet connected to a server.");
								return;
							}
							// Get a new connection
							conn = dbxTuneInstance.getNewConnection(Version.getAppName()+"-InstallProc");
						
							// Open A dialog
							InstallSqlFromUrlDialog installer = new InstallSqlFromUrlDialog(dbxTuneInstance, "", dbname, menuItemName, scriptLocationStr);
							installer.setConnection(conn);
							installer.setVisible(true);
						}

						if (queryWindowInstance != null)
						{
							// Get the Connection used by sqlWindow
							conn = queryWindowInstance.getConnection();
//							conn = queryWindowInstance.getNewConnection(Version.getAppName()+"-InstallProc");

							// Open A dialog
							InstallSqlFromUrlDialog installer = new InstallSqlFromUrlDialog(queryWindowInstance.getWindow(), "", dbname, menuItemName, scriptLocationStr);
							installer.setConnection(conn);
							installer.setVisible(true);
						}
					}
				}
			};

			JMenuItem menuItem = new JMenuItem(menuItemName);
			menuItem.addActionListener(action);

			if ( firstAdd )
			{
				firstAdd = false;
				if (menu.getMenuComponentCount() > 0)
					menu.addSeparator();
			}
			menu.add(menuItem);
		}

//		menu.addPopupMenuListener( createPopupMenuListener() );

		return menu;
	}

}

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
import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.Version;
import com.dbxtune.cache.DbmsObjectIdCache;
import com.dbxtune.cache.DbmsObjectIdCachePostgres;
import com.dbxtune.cache.XmlPlanCache;
import com.dbxtune.cache.XmlPlanCachePostgres;
import com.dbxtune.config.dbms.DbmsConfigManager;
import com.dbxtune.config.dbms.DbmsConfigTextManager;
import com.dbxtune.config.dbms.IDbmsConfig;
import com.dbxtune.config.dbms.IDbmsConfigText;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.config.dict.MonTablesDictionaryPostgres;
import com.dbxtune.gui.ConnectionDialog.Options;
import com.dbxtune.gui.swing.GTabbedPane;
import com.dbxtune.gui.swing.WaitForExecDialog;
import com.dbxtune.hostmon.HostMonitorConnection;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.SwingUtils;

public class MainFramePostgres 
extends MainFrame
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public MainFramePostgres()
	{
		super();
	}

	@Override
	public int getDefaultRefreshInterval()
	{
		return 20;
	}

	@Override public String    getTablePopupDbmsVendorString() { return "postgres"; }
	@Override public ImageIcon getApplicationIcon16() { return SwingUtils.readImageIcon(Version.class, "images/postgrestune_icon_16.png"); };
	@Override public ImageIcon getApplicationIcon32() { return SwingUtils.readImageIcon(Version.class, "images/postgrestune_icon_32.png"); };

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
		boolean doCheckMonitorConfig           = false;
		boolean doInitMonitorDictionary        = true;
		boolean doInitDbServerConfigDictionary = true;
		boolean doInitCounterCollector         = false;

		return new ConnectionProgressExtraActionsAbstract(doInitializeVersionInfo, doCheckMonitorConfig, doInitMonitorDictionary, doInitDbServerConfigDictionary, doInitCounterCollector)
//		return new ConnectionProgressExtraActions()
		{
//			@Override public boolean doInitializeVersionInfo()        { return true; } 
			@Override public boolean initializeVersionInfo(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
//				// NOTE: This will only work in online mode...
//				//       To make it for in offline mode we need to do simular stuff as we do in AseTune...
//				//       and also redo MonTablesDictionary... and subclass it etc...
//				//MonTablesDictionary.getInstance().initializeVersionInfo(conn, true);
//
//				DatabaseMetaData md = conn.getMetaData();
//				
//				List<CountersModel> cmList = CounterController.getInstance().getCmList();
//				for (CountersModel cm : cmList)
//				{
//					String[] sa = cm.getMonTablesInQuery();
//					if (sa == null)
//						continue;
//					for (String tableName : sa)
//					{
//						MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
//						mtd.addTable(tableName,  "");
//
//						ResultSet rs = md.getColumns(null, null, tableName, "%");
//						while(rs.next())
//						{
//							String tName = rs.getString("TABLE_NAME");
//							String cName = rs.getString("COLUMN_NAME");
//							String desc  = rs.getString("REMARKS");
//
//                            try 
//                            {
//    							if (StringUtil.hasValue(desc))
//    								mtd.addColumn(tName, cName, "<html>"+desc.replace("\n", "<br>")+"</html>");
//                            }
//                    		catch (NameNotFoundException e) {/*ignore*/ e.printStackTrace();}
//						}
//						rs.close();
//					}
//				}

				MonTablesDictionaryManager.getInstance().initializeVersionInfo(conn, true);
				return true;
			}
			
//			@Override public boolean doCheckMonitorConfig()           { return false; } 
			@Override public boolean checkMonitorConfig(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				return true;
			}

//			@Override public boolean doInitMonitorDictionary()        { return true; } 
			@Override public boolean initMonitorDictionary(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				MonTablesDictionaryManager.getInstance().initialize(conn, true);
				return true;
			}
			
//			@Override public boolean doInitDbServerConfigDictionary() { return true; } 
			@Override public boolean initDbServerConfigDictionary(DbxConnection conn, HostMonitorConnection hostMonConn, ConnectionProgressDialog cpd) throws Exception
			{
				if (DbmsConfigManager.hasInstance())
				{
					cpd.setStatus("Getting Postgress Configurations");
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
			
//			@Override public boolean doInitCounterCollector()         { return false; } 
			@Override public boolean initCounterCollector(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				return true;
			}			
		};
	}

	@Override
	public void connectMonitorHookin()
	{
		// DBMS ObjectID --> ObjectName Cache... maybe it's not the perfect place to initialize this...
		DbmsObjectIdCache.setInstance( new DbmsObjectIdCachePostgres(this) );
		
		// Populate Object ID Cache
		if (DbmsObjectIdCache.hasInstance() && DbmsObjectIdCache.getInstance().isBulkLoadOnStartEnabled())
			DbmsObjectIdCache.getInstance().getBulk(null); // null == ALL Databases
		else
			_logger.info("Skipping BULK load of ObjectId's at connectMonitorHookin(), isBulkLoadOnStartEnabled() was NOT enabled. Property '" + DbmsObjectIdCachePostgres.PROPKEY_BulkLoadOnStart + "=true|false'.");

		// (XML) Plan Cache... maybe it's not the perfect place to initialize this...
		XmlPlanCache.setInstance( new XmlPlanCachePostgres(this) );
	}

	@Override
	public void connectOfflineHookin()
	{
		MonTablesDictionaryPostgres.initExtraMonTablesDictionary();
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
		if      (TCP_GROUP_SERVER     .equals(groupName)) return TCP_GROUP_ICON_SERVER;
		else if (TCP_GROUP_PROGRESS   .equals(groupName)) return TCP_GROUP_ICON_PROGRESS;
		else if (TCP_GROUP_REPLICATION.equals(groupName)) return TCP_GROUP_ICON_REPLICATION;
		else
			return super.getGroupIcon(groupName);
	}
	public static final ImageIcon TCP_GROUP_ICON_SERVER        = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_postgresserver.png");
	public static final ImageIcon TCP_GROUP_ICON_PROGRESS      = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_progress.png");
	public static final ImageIcon TCP_GROUP_ICON_REPLICATION   = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_pg_replication.png");

	public static final String    TCP_GROUP_PROGRESS         = "Progress";
	public static final String    TCP_GROUP_REPLICATION      = "Replication";
	
	@Override
	protected boolean addTabGroup(String groupName)
	{
		if (TCP_GROUP_REP_AGENT.equals(groupName))
			return false;
		return super.addTabGroup(groupName);
	}

	/**
	 * @param mainTabbedPane 
	 */
	@Override
	public GTabbedPane createGroupTabbedPane(GTabbedPane mainTabbedPane)
	{
		GTabbedPane tabGroupServer       = new GTabbedPane("MainFrame_TabbedPane_Server");
		GTabbedPane tabGroupObjectAccess = new GTabbedPane("MainFrame_TabbedPane_ObjectAccess");
		GTabbedPane tabGroupCache        = new GTabbedPane("MainFrame_TabbedPane_Cache");
//		GTabbedPane tabGroupDisk         = new GTabbedPane("MainFrame_TabbedPane_Disk");
//		GTabbedPane tabGroupRepAgent     = new GTabbedPane("MainFrame_TabbedPane_RepAgent");
		GTabbedPane tabGroupProgress     = new GTabbedPane("MainFrame_TabbedPane_Progress");
		GTabbedPane tabGroupReplication  = new GTabbedPane("MainFrame_TabbedPane_Replication");
		GTabbedPane tabGroupHostMonitor  = new GTabbedPane("MainFrame_TabbedPane_HostMonitor");
		GTabbedPane tabGroupUdc          = new GTabbedPane("MainFrame_TabbedPane_Udc");

		// Lets do setTabLayoutPolicy for all sub tabs...
		tabGroupServer      .setTabLayoutPolicy(mainTabbedPane.getTabLayoutPolicy());
		tabGroupObjectAccess.setTabLayoutPolicy(mainTabbedPane.getTabLayoutPolicy());
		tabGroupCache       .setTabLayoutPolicy(mainTabbedPane.getTabLayoutPolicy());
//		tabGroupDisk        .setTabLayoutPolicy(mainTabbedPane.getTabLayoutPolicy());
//		tabGroupRepAgent    .setTabLayoutPolicy(mainTabbedPane.getTabLayoutPolicy());
		tabGroupProgress    .setTabLayoutPolicy(mainTabbedPane.getTabLayoutPolicy());
		tabGroupReplication .setTabLayoutPolicy(mainTabbedPane.getTabLayoutPolicy());
		tabGroupHostMonitor .setTabLayoutPolicy(mainTabbedPane.getTabLayoutPolicy());
		tabGroupUdc         .setTabLayoutPolicy(mainTabbedPane.getTabLayoutPolicy());

		if (addTabGroup(TCP_GROUP_SERVER))        mainTabbedPane.addTab(TCP_GROUP_SERVER,        getGroupIcon(TCP_GROUP_SERVER),        tabGroupServer,       getGroupToolTipText(TCP_GROUP_SERVER));
		if (addTabGroup(TCP_GROUP_OBJECT_ACCESS)) mainTabbedPane.addTab(TCP_GROUP_OBJECT_ACCESS, getGroupIcon(TCP_GROUP_OBJECT_ACCESS), tabGroupObjectAccess, getGroupToolTipText(TCP_GROUP_OBJECT_ACCESS));
		if (addTabGroup(TCP_GROUP_CACHE))         mainTabbedPane.addTab(TCP_GROUP_CACHE,         getGroupIcon(TCP_GROUP_CACHE),         tabGroupCache,        getGroupToolTipText(TCP_GROUP_CACHE));
//		if (addTabGroup(TCP_GROUP_DISK))          mainTabbedPane.addTab(TCP_GROUP_DISK,          getGroupIcon(TCP_GROUP_DISK),          tabGroupDisk,         getGroupToolTipText(TCP_GROUP_DISK));
//		if (addTabGroup(TCP_GROUP_REP_AGENT))     mainTabbedPane.addTab(TCP_GROUP_REP_AGENT,     getGroupIcon(TCP_GROUP_REP_AGENT),     tabGroupRepAgent,     getGroupToolTipText(TCP_GROUP_REP_AGENT));
		if (addTabGroup(TCP_GROUP_PROGRESS))      mainTabbedPane.addTab(TCP_GROUP_PROGRESS,      getGroupIcon(TCP_GROUP_PROGRESS),      tabGroupProgress,     "Show information from progress reports: 'pg_stat_progress_*' tables.");
		if (addTabGroup(TCP_GROUP_REPLICATION))   mainTabbedPane.addTab(TCP_GROUP_REPLICATION,   getGroupIcon(TCP_GROUP_REPLICATION),   tabGroupReplication,  "Show information about Replication.");
		if (addTabGroup(TCP_GROUP_HOST_MONITOR))  mainTabbedPane.addTab(TCP_GROUP_HOST_MONITOR,  getGroupIcon(TCP_GROUP_HOST_MONITOR),  tabGroupHostMonitor,  getGroupToolTipText(TCP_GROUP_HOST_MONITOR));
		if (addTabGroup(TCP_GROUP_UDC))           mainTabbedPane.addTab(TCP_GROUP_UDC,           getGroupIcon(TCP_GROUP_UDC),           tabGroupUdc,          getGroupToolTipText(TCP_GROUP_UDC));
		
		tabGroupUdc.setEmptyTabMessage(
			"No User Defined Performance Counters has been added.\n" +
			"\n" +
			"To create one just follow the Wizard under:\n" +
			"Menu -> Tools -> Create 'User Defined Counter' Wizard...\n" +
			"\n" +
			"This enables you to write Performance Counters on you'r Application Tables,\n" +
			"which enables you to measure Application specific performance issues.\n" +
			"Or simply write you'r own MDA table queries...");
		
		return mainTabbedPane;
	}
	

	@Override
	public DbmsVersionPanelAbstract createDbmsVersionPanel(ShowCmPropertiesDialog showCmPropertiesDialog)
	{
		return new DbmsVersionPanelPostgres(showCmPropertiesDialog);
	}
}

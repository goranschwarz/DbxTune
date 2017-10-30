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
import com.asetune.config.dict.MonTablesDictionaryPostgres;
import com.asetune.gui.ConnectionDialog.Options;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.SwingUtils;

public class MainFramePostgres 
extends MainFrame
{
	private static final long serialVersionUID = 1L;

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
		return new ConnectionProgressExtraActions()
		{
			@Override public boolean doInitializeVersionInfo()        { return true; } 
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
//					}
//				}

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
			@Override public boolean initDbServerConfigDictionary(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
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
							t.initialize(conn, true, false, null);
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
		if (TCP_GROUP_SERVER.equals(groupName)) 
			return TCP_GROUP_ICON_SERVER;
		else
			return super.getGroupIcon(groupName);
	}
	public static final ImageIcon TCP_GROUP_ICON_SERVER        = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_postgresserver.png");

	@Override
	protected boolean addTabGroup(String groupName)
	{
		if (TCP_GROUP_REP_AGENT.equals(groupName))
			return false;
		return super.addTabGroup(groupName);
	}
}

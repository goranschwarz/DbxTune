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
import com.asetune.gui.ConnectionDialog.Options;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.SwingUtils;

public class MainFrameRax 
extends MainFrame
{
	private static final long serialVersionUID = 1L;

	public MainFrameRax()
	{
		super();
	}

	@Override
	public int getDefaultRefreshInterval()
	{
		return 30;
	}

	@Override public ImageIcon getApplicationIcon16() { return SwingUtils.readImageIcon(Version.class, "images/raxtune_icon_16.png"); };
	@Override public ImageIcon getApplicationIcon32() { return SwingUtils.readImageIcon(Version.class, "images/raxtune_icon_32.png"); };

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
			@Override public boolean doInitializeVersionInfo()        { return false; } 
			@Override public boolean doCheckMonitorConfig()           { return false; } 
			@Override public boolean doInitMonitorDictionary()        { return false; } 
			@Override public boolean doInitDbServerConfigDictionary() { return true; } 
			@Override public boolean doInitCounterCollector()         { return false; } 

			@Override
			public boolean initializeVersionInfo(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				return true;
			}
			
			@Override
			public boolean checkMonitorConfig(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				return true;
			}

			@Override
			public boolean initMonitorDictionary(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				return true;
			}
			
			@Override
			public boolean initDbServerConfigDictionary(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				if (DbmsConfigManager.hasInstance())
				{
					cpd.setStatus("Getting ra_config settings");
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
			
			@Override
			public boolean initCounterCollector(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
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
		if      (TCP_GROUP_SERVER.equals(groupName)) return TCP_GROUP_ICON_SERVER;
		else                                         return super.getGroupIcon(groupName);
	}
//	public static final ImageIcon TCP_GROUP_ICON_SERVER = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_raxserver.png");
	public static final ImageIcon TCP_GROUP_ICON_SERVER = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_repagent.png");

	@Override
	protected boolean addTabGroup(String groupName)
	{
		if      (TCP_GROUP_OBJECT_ACCESS.equals(groupName)) return false;
		else if (TCP_GROUP_CACHE        .equals(groupName)) return false;
		else if (TCP_GROUP_DISK         .equals(groupName)) return false;
		else if (TCP_GROUP_REP_AGENT    .equals(groupName)) return false;

		return super.addTabGroup(groupName);
	}
}

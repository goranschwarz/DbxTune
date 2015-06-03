package com.asetune.gui;

import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.asetune.Version;
import com.asetune.gui.ConnectionDialog.Options;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.SwingUtils;

public class MainFrameSqlServer 
extends MainFrame
{
	private static final long serialVersionUID = 1L;

	public MainFrameSqlServer()
	{
		super();
	}

	@Override
	public int getDefaultRefreshInterval()
	{
		return 20;
	}

	@Override public ImageIcon getApplicationIcon16() { return SwingUtils.readImageIcon(Version.class, "images/sqlservertune_icon_16.png"); };
	@Override public ImageIcon getApplicationIcon32() { return SwingUtils.readImageIcon(Version.class, "images/sqlservertune_icon_32.png"); };

	@Override
	public Options getConnectionDialogOptions()
	{
		Options options = new Options();

		options._showAseTab               = false;
		options._showDbxTuneOptionsInTds  = false;
		options._showHostmonTab           = false;
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
			@Override public boolean doInitializeVersionInfo()        { return false; } 
			@Override public boolean doCheckMonitorConfig()           { return false; } 
			@Override public boolean doInitMonitorDictionary()        { return false; } 
			@Override public boolean doInitDbServerConfigDictionary() { return true; } 
			@Override public boolean doInitCounterCollector()         { return false; } 

			@Override
			public boolean initializeVersionInfo(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
//				// Just get ASE Version, this will be good for error messages, sent to WEB server, this will write ASE Version in the info...
//				MonTablesDictionary.getInstance().initializeVersionInfo(conn, true);
				return true;
			}
			
			@Override
			public boolean checkMonitorConfig(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				Thread.sleep(1*1000);
				return true;
//				return AseConnectionUtils.checkForMonitorOptions(conn, null, true, cpd, "enable monitoring");
			}

			@Override
			public boolean initMonitorDictionary(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
//				if ( ! ConnectionDialog.checkReconnectVersion(conn) )
//					throw new Exception("Connecting to a different ASE Version, This is NOT supported now...");
//
//				MonTablesDictionary.getInstance().initialize(conn, true);
//				CounterControllerAse.initExtraMonTablesDictionary();
				
				Thread.sleep(1*1000);
				return true;
			}
			
			@Override
			public boolean initDbServerConfigDictionary(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
//				AseConfig aseCfg = AseConfig.getInstance();
//				if ( ! aseCfg.isInitialized() )
//					aseCfg.initialize(conn, true, false, null);
//
//				// initialize ASE Config Text Dictionary
//				AseConfigText.initializeAll(conn, true, false, null);

				Thread.sleep(1*1000);
				return true;
			}
			
			@Override
			public boolean initCounterCollector(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
//				CounterController.getInstance().initCounters(
//						conn,
//						true,
//						MonTablesDictionary.getInstance().getAseExecutableVersionNum(),
//						MonTablesDictionary.getInstance().isClusterEnabled(),
//						MonTablesDictionary.getInstance().getMdaVersion());

				Thread.sleep(1*1000);
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
		if (TCP_GROUP_HOST_MONITOR.equals(groupName))
			return false;
		return super.addTabGroup(groupName);
	}
}

package com.asetune.gui;

import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.asetune.Version;
import com.asetune.gui.ConnectionDialog.Options;
import com.asetune.gui.swing.GTabbedPane;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.utils.SwingUtils;

public class MainFrameIq 
extends MainFrame
{
	private static final long serialVersionUID = 1L;

	public MainFrameIq()
	{
		super();
	}

	@Override
	public int getDefaultRefreshInterval()
	{
		return 30;
	}

	@Override public String    getTablePopupDbmsVendorString() { return "iq"; }
	@Override public ImageIcon getApplicationIcon16() { return SwingUtils.readImageIcon(Version.class, "images/iqtune_icon_16.png"); };
	@Override public ImageIcon getApplicationIcon32() { return SwingUtils.readImageIcon(Version.class, "images/iqtune_icon_32.png"); };

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

		options._srvExtraChecks = null;
		
		return options;
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
		
		GTabbedPane tabGroupCatalog   = new GTabbedPane("MainFrame_TabbedPane_Cat");
		GTabbedPane tabGroupMultiplex = new GTabbedPane("MainFrame_TabbedPane_MPlex");

		// Lets do setTabLayoutPolicy for all sub tabs...
		tabGroupCatalog  .setTabLayoutPolicy(mainTabbedPane.getTabLayoutPolicy());
		tabGroupMultiplex.setTabLayoutPolicy(mainTabbedPane.getTabLayoutPolicy());

		int insLocation = mainTabbedPane.indexOfTab(TCP_GROUP_CACHE);
		if (addTabGroup(TCP_GROUP_CATALOG))
			mainTabbedPane.insertTab(TCP_GROUP_CATALOG, getGroupIcon(TCP_GROUP_CATALOG), tabGroupCatalog, getGroupToolTipText(TCP_GROUP_CATALOG), insLocation);

		insLocation = mainTabbedPane.indexOfTab(TCP_GROUP_CACHE);
		if (addTabGroup(TCP_GROUP_MULTIPLEX))
			mainTabbedPane.insertTab(TCP_GROUP_MULTIPLEX, getGroupIcon(TCP_GROUP_MULTIPLEX), tabGroupMultiplex, getGroupToolTipText(TCP_GROUP_MULTIPLEX), insLocation);
		
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
		if      (TCP_GROUP_SERVER   .equals(groupName)) return TCP_GROUP_ICON_SERVER;
		else if (TCP_GROUP_CATALOG  .equals(groupName)) return TCP_GROUP_ICON_CATALOG;
		else if (TCP_GROUP_MULTIPLEX.equals(groupName)) return TCP_GROUP_ICON_MULTIPLEX;
		else                                            return super.getGroupIcon(groupName);
	}
	public static final ImageIcon TCP_GROUP_ICON_SERVER    = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_iqserver.png");
	public static final ImageIcon TCP_GROUP_ICON_CATALOG   = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_catalog.png");
	public static final ImageIcon TCP_GROUP_ICON_MULTIPLEX = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_multiplex.png");

	public static final String    TCP_GROUP_CATALOG     = "Catalog";
	public static final String    TCP_GROUP_MULTIPLEX   = "Multiplex";

	@Override
	protected boolean addTabGroup(String groupName)
	{
		if      (TCP_GROUP_OBJECT_ACCESS.equals(groupName)) return false;
		else if (TCP_GROUP_REP_AGENT    .equals(groupName)) return false;

		return super.addTabGroup(groupName);
	}

	@Override
	protected String getGroupToolTipText(String groupName)
	{
		if      (TCP_GROUP_CATALOG  .equals(groupName)) return "<html>SQL Anywhere Catalog Performace Counters</html>";
		else if (TCP_GROUP_MULTIPLEX.equals(groupName)) return "<html>IQ Multiplex/Cluster Performace Counters</html>";
		else return super.getGroupToolTipText(groupName);
	}
}

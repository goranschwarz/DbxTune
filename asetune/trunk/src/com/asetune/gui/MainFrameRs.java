package com.asetune.gui;

import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.asetune.Version;
import com.asetune.gui.ConnectionDialog.Options;
import com.asetune.gui.swing.GTabbedPane;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.utils.SwingUtils;

public class MainFrameRs 
extends MainFrame
{
	private static final long serialVersionUID = 1L;

	public MainFrameRs()
	{
		super();
	}

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
}

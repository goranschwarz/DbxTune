package com.asetune.gui;

import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.asetune.Version;
import com.asetune.gui.ConnectionDialog.Options;
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
	public static final ImageIcon TCP_GROUP_ICON_SERVER        = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_iqserver.png");

	@Override
	protected boolean addTabGroup(String groupName)
	{
		if (TCP_GROUP_REP_AGENT.equals(groupName))
			return false;
		return super.addTabGroup(groupName);
	}
}

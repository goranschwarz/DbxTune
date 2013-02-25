package com.asetune.tools;

import java.io.File;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import net.miginfocom.swing.MigLayout;

import com.asetune.gui.ConnectionDialog;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.AseConnectionUtils.ConnectionStateInfo;
import com.asetune.utils.StringUtil;

public class StatusBar extends JPanel
{
	private static final long	serialVersionUID	= 1L;

	private static String NOT_CONNECTED = "Not Connected";

	private JLabel	    _msgline                = new JLabel("");
	private JLabel	    _currentFilename        = new JLabel("");
	private JLabel	    _currentFilenameIsDirty = new JLabel("*");

	private JLabel	    _userName               = new JLabel("");
	private JLabel	    _serverName             = new JLabel(NOT_CONNECTED);
	private JLabel	    _productStringShort     = new JLabel("");
	private String	    _productStringLong      = null;
	private String	    _productVersion         = null;
	private String	    _productServerName      = null;

	private JLabel	    _aseConnStateInfo       = new JLabel("");

	public StatusBar()
	{
		init();
	}

	private void init()
	{
		setLayout(new MigLayout("insets 2 5 2 5")); // top left bottom right

		// msg
		_msgline.setToolTipText("Various status information from the tool.");

		_aseConnStateInfo.setToolTipText(
				"<html>" +
				"Various status for the current connection. Are we in a transaction or not.<br>" +
				"<br>" +
				"<code>@@trancount</code> / TranCount Explanation:<br>" +
				"<ul>" +
				"  <li>Simply a counter on <code>begin transaction</code> nesting level<br>" +
				"      Try to issue <code>begin/commit/rollback tran</code> multiple times and see how @@trancount increases/decreases (rollback always resets it to 0)</li>" +
				"</ul>" +
				"<code>@@transtate</code> / TranState Explanation:<br>" +
				"<ul>" +
				"  <li><b>TRAN_IN_PROGRESS:</b> Transaction in progress. A transaction is in effect; The previous statement executed successfully</li>" +
				"  <li><b>TRAN_SUCCEED:    </b> Last Transaction succeeded. The transaction completed and committed its changes.</li>" +
				"  <li><b>STMT_ABORT:      </b> Last Statement aborted. The previous statement was aborted; No effect on the transaction.</li>" +
				"  <li><b>TRAN_ABORT:      </b> Last Transaction aborted. The transaction aborted and rolled back any changes.</li>" +
				"</ul>" +
				"To get rid of status 'TRAN_ABORT' simply issue <code>begin tran commit tran</code> to induce a dummy transaction that succeeds..." +
				"</html>");
		
		// isDirty
		_currentFilenameIsDirty.setVisible(false);
		_currentFilenameIsDirty.setToolTipText("The file has been changed.");

		// Current file
		_currentFilename.setToolTipText("Current filename");

		// UserName
		_userName.setToolTipText("What users did we connect as");

		// ServerName
		_serverName.setToolTipText("What Server Name are we connected to");

		// Product
		_productStringShort.setToolTipText(""); // Done later
		
		add(_msgline,                "width 200:200:null");
		add(new JSeparator(JSeparator.VERTICAL), "grow");

//		add(new JLabel(),            "push, grow"); // Dummy filler

		add(_currentFilenameIsDirty, "");
		add(_currentFilename,        "push, grow");
		add(new JSeparator(JSeparator.VERTICAL), "grow");

		add(_aseConnStateInfo,       "split, grow, hidemode 2");
		add(new JSeparator(JSeparator.VERTICAL), "grow");

		add(_userName,               "grow");
		add(new JSeparator(JSeparator.VERTICAL), "grow");

		add(_serverName,             "grow");
		add(new JSeparator(JSeparator.VERTICAL), "grow");

		add(_productStringShort,     "grow");
	}
	
	public void setMsg(String text)
	{
		_msgline.setText(text);
	}

	public void setFilename(String filename)
	{
		File f = new File(filename);
		if ( ! f.exists() )
		{
			_currentFilename.setText("no-file");
//			_currentFilename.setText("Untitled.txt");
			return;
		}
		_currentFilename.setText(filename);
	}

	public void setFilenameDirty(boolean dirty)
	{
		_currentFilenameIsDirty.setVisible(dirty);
	}

	public void setServerName(String srvName, String productName, String productVersion, String serverName, String username, String withUrl, String sysListeners)
	{
		if (srvName        == null) srvName        = NOT_CONNECTED;
		if (productName    == null) productName    = "";
		if (productVersion == null) productVersion = "";
		if (serverName     == null) serverName     = "";
		if (username       == null) username       = "";
		if (withUrl        == null) withUrl        = "";

		String productStrShort = "";
		if      (productName.equals(""))                                       productStrShort = "";
		else if (productName.equals(ConnectionDialog.DB_PROD_NAME_SYBASE_ASE)) productStrShort = "ASE";
		else if (productName.equals(ConnectionDialog.DB_PROD_NAME_SYBASE_ASA)) productStrShort = "ASA";
		else if (productName.equals(ConnectionDialog.DB_PROD_NAME_SYBASE_RS))  productStrShort = "RS";
		else if (productName.equals(ConnectionDialog.DB_PROD_NAME_H2))         productStrShort = "H2";
		else productStrShort = "UNKNOWN";

		_productStringShort.setText(productStrShort);
		_productStringLong = productName;
		_productVersion    = productVersion;
		_productServerName = serverName;

		_serverName.setText(srvName);
		_userName  .setText(username);

		if ("".equals(productStrShort))
		{
			_productStringShort.setToolTipText(NOT_CONNECTED);
		}
		else
		{
			String listeners = "";
			if ( ! StringUtil.isNullOrBlank(sysListeners) )
				listeners = "    <li>ASE Listens on: <b>" + sysListeners + "</b></li>";

			_productStringShort.setToolTipText(
					"<html>" +
					"Connected to:<br>" +
					"<ul>" +
					(StringUtil.isNullOrBlank(_productServerName) ? "" : "<li>Server Name: <b>" +_productServerName+"</b></li>") +
					"    <li>Product Name:    <b>"+_productStringLong+"</b></li>" +
					"    <li>Product Version: <b>"+_productVersion   +"</b></li>" +
					listeners +
					"</ul>" +
					"</html>");
		}
	}

	public String getServerName()
	{
		return _serverName.getText();
	}

	public void setAseConnectionStateInfo(ConnectionStateInfo csi)
	{
		if (csi == null)
		{
			_aseConnStateInfo.setVisible(false);
			return;
		}

		String dbname    = "db="        + csi._dbname;
		String spid      = "spid="      + csi._spid;
		String tranState = "TranState=" + csi.getTranStateStr();
		String tranCount = "TranCount=" + csi._tranCount;

		if (csi._tranCount > 0)
			tranCount = "TranCount=<b><font color=\"red\">" + csi._tranCount        + "</font></b>";

		if (csi._tranState != AseConnectionUtils.ConnectionStateInfo.TSQL_TRAN_SUCCEED)
			tranState = "TranState=<b><font color=\"red\">" + csi.getTranStateStr() + "</font></b>";
		
		String text = "<html>"
			 + dbname    + ", "
			 + spid      + ", " 
			 + tranState + ", " 
			 + tranCount + 
			 "</html>";

		_aseConnStateInfo.setVisible(true);
		_aseConnStateInfo.setText(text);
	}
}
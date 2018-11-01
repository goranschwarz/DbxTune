package com.asetune.tools.sqlw;

import java.io.File;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import com.asetune.gui.swing.GLabel;
import com.asetune.gui.swing.GMemoryIndicator;
import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;

import net.miginfocom.swing.MigLayout;

public class StatusBar extends JPanel
{
	private static final long	serialVersionUID	= 1L;

	private static String NOT_CONNECTED = "Not Connected";

	private JLabel     _msgline                   = new JLabel("");
	private JTextArea  _currentFilename           = new JTextArea("");
	private GLabel     _currentFilenameEncoding   = new GLabel();         // use focusable tool tip 
	private JLabel     _currentFilenameIsDirty    = new JLabel("*");

	private JLabel     _userName               = new JLabel("");
	private JLabel     _serverName             = new JLabel(NOT_CONNECTED);
	private GLabel     _productStringShort     = new GLabel("");
	private String     _productStringLong      = null;
	private String     _productVersion         = null;
	private String     _productServerName      = null;

	private GLabel     _srvConnStateInfo       = new GLabel("");

	private JLabel     _editorAtLineCol        = new JLabel("1,1");

	private ServerInfo _serverInfo            = null;

	public StatusBar()
	{
		init();
	}

//	juniversalchardet is a Java port of 'universalchardet', that is the encoding detector library of Mozilla. 
//	http://code.google.com/p/juniversalchardet/
	private static final String FILENAME_TOOLTIP_BASE = 
		"<html>" +
		"Name of the current file.<br>" +
		"<br>" +
		"<b>File Encoding</b>: CHARSET-NAME<br>" + // CHARSET-NAME will be changed in setFilename()
		"<br>" +
		"What encoding is the file using.<br>" +
		"Or actually: The encoding we have <b>guessed</b> that it's using.<br>" +
		"<br>" +
		"The guess is based on: <br>" +
		"The library <b>juniversalchardet</b> which is a Java port of 'universalchardet', that is the encoding detector library of Mozilla.<br>" +
		"More information can be found at <br>" +
		"<ul>" +
		"   <li><A HREF=\"http://code.google.com/p/juniversalchardet\">http://code.google.com/p/juniversalchardet</A> </li>" +
		"   <li><A HREF=\"http://www-archive.mozilla.org/projects/intl/UniversalCharsetDetection.html\">http://www-archive.mozilla.org/projects/intl/UniversalCharsetDetection.html</A> </li>" +
		"</ul>" +
		"</html>";

	private static final String ASE_STATE_INFO_TOOLTIP_BASE = 
		"<html>" +
		"Various status for the current connection. Are we in a transaction or not.<br>" +
		"<br>" +
		"<code>@@trancount</code> / TranCount Explanation:" +
		"<ul>" +
		"  <li>Simply a counter on <code>begin transaction</code> nesting level<br>" +
		"      Try to issue <code>begin/commit/rollback tran</code> multiple times and see how @@trancount increases/decreases (rollback always resets it to 0)</li>" +
		"</ul>" +
		"<code>@@transtate</code> / TranState Explanation:" +
		"<ul>" +
		"  <li><b>TRAN_IN_PROGRESS:</b> Transaction in progress. A transaction is in effect; The previous statement executed successfully</li>" +
		"  <li><b>TRAN_SUCCEED:    </b> Last Transaction succeeded. The transaction completed and committed its changes.</li>" +
		"  <li><b>STMT_ABORT:      </b> Last Statement aborted. The previous statement was aborted; No effect on the transaction.</li>" +
		"  <li><b>TRAN_ABORT:      </b> Last Transaction aborted. The transaction aborted and rolled back any changes.<br>" +
		"                               To get rid of status 'TRAN_ABORT' simply issue <code>begin tran commit tran</code> to induce a dummy transaction that succeeds...<br>" +
		"      </li>" +
		"</ul>" +
		"<br>" +
		"<code>@@tranchained</code> / TranChained Explanation:" +
		"<ul>" +
		"  <li><b>0</b>: autocommit=true  (<code>set chained off</code>): The default mode, called <b>unchained</b> mode or Transact-SQL mode, requires explicit <b>begin transaction</b> statements paired with <b>commit transaction</b> or <b>rollback transaction</b> statements to complete the transaction.</li>" +
		"  <li><b>1</b>: autocommit=false (<code>set chained on</code>):  <b>chained</b> mode implicitly begins a transaction before any data-retrieval or modification statement: <b>delete</b>, <b>insert</b>, <b>open</b>, <b>fetch</b>, <b>select</b>, and <b>update</b>. You must still explicitly end the transaction with <b>commit transaction</b> or <b>rollback transaction</b></li>" +
		"</ul>" +
		"</html>";
	
	private static final String ASE_STATE_INFO_TOOLTIP_BASE_NO_TRANSTATE = 
			"<html>" +
			"Various status for the current connection. Are we in a transaction or not.<br>" +
			"<br>" +
			"<code>@@trancount</code> / TranCount Explanation:<br>" +
			"<ul>" +
			"  <li>Simply a counter on <code>begin transaction</code> nesting level<br>" +
			"      Try to issue <code>begin/commit/rollback tran</code> multiple times and see how @@trancount increases/decreases (rollback always resets it to 0)</li>" +
			"</ul>" +
			"</html>";
		
	private static final String JDBC_STATE_INFO_TOOLTIP_BASE = 
		"<html>" +
		"Various status for the current connection. Are we in a transaction or not.<br>" +
		"</html>";
	
	private static final String MSG_LINE_TOOLTIP_BASE = "Various status information from the tool.";  
	private void init()
	{
		setLayout(new MigLayout("insets 1 5 1 5")); // top left bottom right

		// msg
		_msgline.setToolTipText(MSG_LINE_TOOLTIP_BASE);

		_srvConnStateInfo.setToolTipText(ASE_STATE_INFO_TOOLTIP_BASE);
		
		// isDirty
		_currentFilenameIsDirty.setVisible(false);
		_currentFilenameIsDirty.setToolTipText("The file has been changed.");

		// Current file
		_currentFilename.setToolTipText(FILENAME_TOOLTIP_BASE);
		_currentFilename.setEditable(false);
		_currentFilename.setBackground( _currentFilenameIsDirty.getBackground() );
		_currentFilename.setFont(       _currentFilenameIsDirty.getFont() );

		// Current file encoding
		_currentFilenameEncoding.setToolTipText(
			"<html>What encoding is the file using.<br>" +
			"Or actually: The encoding we have <b>guessed</b> that it's using.<br>" +
			"<br>" +
			"The guess is based on: <br>" +
			"The library <b>juniversalchardet</b> which is a Java port of 'universalchardet', that is the encoding detector library of Mozilla.<br>" +
			"More information can be found at <br>" +
			"<ul>" +
			"   <li><A HREF=\"http://code.google.com/p/juniversalchardet\">http://code.google.com/p/juniversalchardet</A> </li>" +
			"   <li><A HREF=\"http://www-archive.mozilla.org/projects/intl/UniversalCharsetDetection.html\">http://www-archive.mozilla.org/projects/intl/UniversalCharsetDetection.html</A> </li>" +
			"</ul>" +
			"</html>");

		// UserName
		_userName.setToolTipText("What users did we connect as");

		// ServerName
		_serverName.setToolTipText("What Server Name are we connected to");

		// Product
		_productStringShort.setToolTipText(""); // Done later
		
		// Editor: Line, Column
		_editorAtLineCol.setToolTipText("Carets position 'Line:Col'"); // Done later
		
		add(_msgline,                "width 200:200:null:push");
		add(new JSeparator(JSeparator.VERTICAL), "grow");

//		add(new JLabel(),            "push, grow"); // Dummy filler

		add(_currentFilenameIsDirty, "");
		add(_currentFilename,        "pushx, growx");
		add(_currentFilenameEncoding,"hidemode 3");           
		add(new JSeparator(JSeparator.VERTICAL), "grow");

		add(_srvConnStateInfo,       "split, growx, hidemode 2");
		add(new JSeparator(JSeparator.VERTICAL), "grow");

		add(_userName,               "growx");
		add(new JSeparator(JSeparator.VERTICAL), "grow");

		add(_serverName,             "growx");
		add(new JSeparator(JSeparator.VERTICAL), "grow");

		add(_productStringShort,     "growx");
		add(new JSeparator(JSeparator.VERTICAL), "grow");

		
		add(new GMemoryIndicator(1000),  "growx, hidemode 3");
		add(new JSeparator(JSeparator.VERTICAL), "grow");
//		add(new JMemoryMonitor(),    "growx, hidemode 3");
//		add(new JSeparator(JSeparator.VERTICAL), "grow");

		add(_editorAtLineCol,        "growx, hidemode 3");

		// encode name seems to be slightly off, lets hide it until we have a better implementation
//		_currentFilenameEncoding.setVisible(false); 
	}
	
	public void setMsg(String text)
	{
		_msgline.setText(text);
		_msgline.setToolTipText("<html>" + MSG_LINE_TOOLTIP_BASE + "<br><hr><b>" + text + "</b></html>");
	}

	public void setEditorPos(int line, int col)
	{
		_editorAtLineCol.setText( (line+1) + ":" + (col+1) );
	}
	public void setVisibleAtLineCol(boolean visible)
	{
		_editorAtLineCol.setVisible(visible);
	}
	
	public final static String NO_FILE = "no-file";
	public String getFilename()
	{
		return _currentFilename.getText();
	}
	public void setFilename(String filename, String encoding)
	{
		File f = new File(filename);
		if ( ! f.exists() )
		{
			_currentFilename.setText(NO_FILE);
//			_currentFilename.setText("Untitled.txt");
			_currentFilenameEncoding.setText("");
			return;
		}
		_currentFilename.setText(filename);
		_currentFilenameEncoding.setText(encoding == null ? "" : encoding);
		_currentFilename.setToolTipText(FILENAME_TOOLTIP_BASE.replace("CHARSET-NAME", (encoding == null ? "UNKNOWN" : encoding)));
	}

	public void setFilenameDirty(boolean dirty)
	{
		_currentFilenameIsDirty.setVisible(dirty);
	}

	public void setNotConnected()
	{
//		setServerName(null, null, null, null, null, null, null);
		setServerInfo(null);
		setConnectionStateInfo(null);
//		setAseConnectionStateInfo(null);
	}

	public void setServerInfo(ServerInfo si)
	{
		_serverInfo = si;

		if (si == null)
		{
			_productStringShort.setToolTipText(NOT_CONNECTED);

			_productStringShort.setText("");
			_productStringLong = "";
			_productVersion    = "";
			_productServerName = "";

			_serverName.setText("");
			_userName  .setText("");
			
			return;
		}

		_productStringShort.setText(si.getProductNameShort());
		_productStringLong = si.getProductName();
		_productVersion    = si.getProductVersion();
		_productServerName = si.getServerName();

		_serverName.setText(si.getServerName());
		_userName  .setText(si.getUsername());

		if ( "".equals(si.getProductNameShort()) )
		{
			_productStringShort.setToolTipText(NOT_CONNECTED);
		}
		else
		{
			String listeners = "";
			if ( ! StringUtil.isNullOrBlank(si.getSysListeners()) )
				listeners = "    <li>ASE Listens on: <b>" + si.getSysListeners() + "</b></li>";

			Map<String, Object> extraInfoMap = si.getExtraInfo();
			String extraInfo = "";
			if ( extraInfoMap != null && !extraInfoMap.isEmpty() )
			{
				extraInfo += "<hr>";
				extraInfo += "Extra DBMS Information";
				extraInfo += "<ul>";
				for (String key : extraInfoMap.keySet())
					extraInfo += "<li>" + key + ": <b>" + extraInfoMap.get(key) + "</b></li>";
				extraInfo += "</ul>";
			}
			
			_productStringShort.setToolTipText(
					"<html>" +
					"Connected to:<br>" +
					"<ul>" +
					(StringUtil.isNullOrBlank(si.getServerName()) ? "" : "<li>Server Name:     <b>" + si.getServerName()     + "</b></li>") +
					(StringUtil.isNullOrBlank(si.getServerName()) ? "" : "<li>Initial Catalog: <b>" + si.getInitialCatalog() + "</b></li>") +
					"    <li>Product Name:    <b>" + si.getProductName()    + "</b></li>" +
					"    <li>Product Version: <b>" + si.getProductVersion() + "</b></li>" +
					listeners +
					(StringUtil.isNullOrBlank(si.getPageSizeKb()       ) ? "" : "<li>Page Size in KB:     <b>" + si.getPageSizeKb()        + "</b></li>") +
					(StringUtil.isNullOrBlank(si.getCharset()          ) ? "" : "<li>Server Charset:      <b>" + si.getCharset()           + "</b></li>") +
					(StringUtil.isNullOrBlank(si.getSortorder()        ) ? "" : "<li>Server SortOrder:    <b>" + si.getSortorder()         + "</b></li>") +
					(StringUtil.isNullOrBlank(si.getClientCharsetId()  ) ? "" : "<li>Client Charset ID:   <b>" + si.getClientCharsetId()   + "</b></li>") +
					(StringUtil.isNullOrBlank(si.getClientCharsetName()) ? "" : "<li>Client Charset Name: <b>" + si.getClientCharsetName() + "</b></li>") +
					(StringUtil.isNullOrBlank(si.getClientCharsetDesc()) ? "" : "<li>Client Charset Desc: <b>" + si.getClientCharsetDesc() + "</b></li>") +
					"</ul>" +
					extraInfo +
					"</html>");
		}
	}

//	public void setServerName(String srvName, String productName, String productVersion, String serverName, String username, String withUrl, String sysListeners)
//	{
//		if (srvName        == null) srvName        = NOT_CONNECTED;
//		if (productName    == null) productName    = "";
//		if (productVersion == null) productVersion = "";
//		if (serverName     == null) serverName     = "";
//		if (username       == null) username       = "";
//		if (withUrl        == null) withUrl        = "";
//
//		String productStrShort = "";
//		if      (productName.equals(""))                                       productStrShort = "";
//		else if (productName.equals(DbUtils.DB_PROD_NAME_SYBASE_ASE)) productStrShort = "ASE";
//		else if (productName.equals(DbUtils.DB_PROD_NAME_SYBASE_ASA)) productStrShort = "ASA";
//		else if (productName.equals(DbUtils.DB_PROD_NAME_SYBASE_IQ))  productStrShort = "IQ";
//		else if (productName.equals(DbUtils.DB_PROD_NAME_SYBASE_RS))  productStrShort = "RS";
//		else if (productName.equals(DbUtils.DB_PROD_NAME_H2))         productStrShort = "H2";
//		else productStrShort = "UNKNOWN";
//
//		_productStringShort.setText(productStrShort);
//		_productStringLong = productName;
//		_productVersion    = productVersion;
//		_productServerName = serverName;
//
//		_serverName.setText(srvName);
//		_userName  .setText(username);
//
//		if ("".equals(productStrShort))
//		{
//			_productStringShort.setToolTipText(NOT_CONNECTED);
//		}
//		else
//		{
//			String listeners = "";
//			if ( ! StringUtil.isNullOrBlank(sysListeners) )
//				listeners = "    <li>ASE Listens on: <b>" + sysListeners + "</b></li>";
//
//			_productStringShort.setToolTipText(
//					"<html>" +
//					"Connected to:<br>" +
//					"<ul>" +
//					(StringUtil.isNullOrBlank(_productServerName) ? "" : "<li>Server Name: <b>" +_productServerName+"</b></li>") +
//					"    <li>Product Name:    <b>"+_productStringLong+"</b></li>" +
//					"    <li>Product Version: <b>"+_productVersion   +"</b></li>" +
//					listeners +
//					"</ul>" +
//					"</html>");
//		}
//	}

	public String getServerName()
	{
		return _serverName.getText();
	}

	public void setConnectionStateInfo(DbxConnectionStateInfo csi)
	{
		if (csi == null)
		{
			_srvConnStateInfo.setVisible(false);
			return;
		}

		_srvConnStateInfo.setVisible(true);
		_srvConnStateInfo.setText(csi.getStatusBarText());
		
		_srvConnStateInfo.setToolTipText(csi.getStatusBarToolTipText());
	}

//	public void setAseConnectionStateInfo(ConnectionStateInfo csi)
//	{
//		if (csi == null)
//		{
//			_srvConnStateInfo.setVisible(false);
//			return;
//		}
//
//		String dbname      = "db="          + csi._dbname;
//		String spid        = "spid="        + csi._spid;
//		String username    = "user="        + csi._username;
//		String susername   = "login="       + csi._susername;
//		String tranState   = "TranState="   + csi.getTranStateStr();
//		String tranCount   = "TranCount="   + csi._tranCount;
//		String tranChained = "TranChained=" + csi._tranChained;
//		String lockCount   = "LockCount="   + csi._lockCount;
//
//		if (csi._tranCount > 0)
//			tranCount = "TranCount=<b><font color=\"red\">" + csi._tranCount        + "</font></b>";
//
//		if ( csi.isNonNormalTranState() )
//			tranState = "TranState=<b><font color=\"red\">" + csi.getTranStateStr() + "</font></b>";
//		
//		if (csi._tranCount > 0)
//			tranChained = "TranChained=<b><font color=\"red\">" + csi._tranChained    + "</font></b>";
//
//		if (csi._lockCount > 0)
//			lockCount = "LockCount=<b><font color=\"red\">" + csi._lockCount    + "</font></b>";
//
//		String text = spid + ", " + username + ", " + susername;
//		if (csi._tranCount > 0 || csi.isNonNormalTranState())
//		{
//			text = "<html>"
//				+ dbname    + ", "
//				+ spid      + ", " 
//				+ username  + ", " 
//				+ susername + ", " 
//				+ (csi.isTranStateUsed() ? (tranState + ", ") : "") 
//				+ tranCount + ", "
//				+ tranChained + ", "
//				+ lockCount
//				+ "</html>";
//		}
//		// If we are in CHAINED mode, and do NOT hold any locks, set state to "normal"
//		if (csi._tranChained == 1 && csi._lockCount == 0)
//		{ // color #B45F04 = dark yellow/orange
//			text = "<html><font color=\"#B45F04\">CHAINED mode</font>, "+spid + ", " + username + ", " + susername + "</html>";
//		}
//
//		_srvConnStateInfo.setVisible(true);
//		_srvConnStateInfo.setText(text);
//		
//		String lockText = "<hr>";
//		if (csi._lockCount > 0)
//			lockText = "<hr>Locks held by this SPID:" + csi.getLockListTableAsHtmlTable() + "<hr>";
//
//		String tooltip = "<html>" +
//			"<table border=0 cellspacing=0 cellpadding=1>" +
//			                         "<tr> <td>Current DB:    </td> <td><b>" + csi._dbname           + "</b> </td> </tr>" +
//			                         "<tr> <td>SPID:          </td> <td><b>" + csi._spid             + "</b> </td> </tr>" +
//			                         "<tr> <td>Current User:  </td> <td><b>" + csi._username         + "</b> </td> </tr>" +
//			                         "<tr> <td>Current Login: </td> <td><b>" + csi._susername        + "</b> </td> </tr>" +
//			(csi.isTranStateUsed() ? "<tr> <td>Tran State:    </td> <td><b>" + csi.getTranStateStr() + "</b> </td> </tr>" : "") +
//			                         "<tr> <td>Tran Count:    </td> <td><b>" + csi._tranCount        + "</b> </td> </tr>" +
//			                         "<tr> <td>Tran Chained:  </td> <td><b>" + csi._tranChained      + "</b> </td> </tr>" +
//			                         "<tr> <td>Lock Count:    </td> <td><b>" + csi._lockCount        + "</b> </td> </tr>" +
//			"</table>" +
//			lockText +
//			(csi.isTranStateUsed() ? ASE_STATE_INFO_TOOLTIP_BASE : ASE_STATE_INFO_TOOLTIP_BASE_NO_TRANSTATE).replace("<html>", ""); // remove the first/initial <html> tag...
//
//		_srvConnStateInfo.setToolTipText(tooltip); //add dbname,spid,transtate, trancount here
//
//	}
//
//	
//	public void setJdbcConnectionStateInfo(JdbcConnectionStateInfo csi)
//	{
//		if (csi == null)
//		{
//			_srvConnStateInfo.setVisible(false);
//			return;
//		}
//
//		String catalog    = "cat="        + csi._catalog;
//		String isolation  = "Isolation="  + csi.getIsolationLevelStr();
//		String autocommit = "AutoCommit=" + csi.getAutoCommit();
//
//		if ( ! csi.getAutoCommit() )
//			autocommit = "AutoCommit=<b><font color=\"red\">" + csi.getAutoCommit() + "</font></b>";
//
//		String text = "ac="+csi.getAutoCommit();
//		if ( ! csi.getAutoCommit() )
//		{
//			text = "<html>"
//				+ autocommit + ", "
//				+ catalog    + ", " 
//				+ isolation  + 
//				"</html>";
//		}
//
//		_srvConnStateInfo.setVisible(true);
//		_srvConnStateInfo.setText(text);
//		
//		String tooltip = "<html>" +
//			"<table border=0 cellspacing=0 cellpadding=1>" +
//			"<tr> <td>Current Catalog: </td> <td><b>" + csi.getCatalog()           + "</b> </td> </tr>" +
//			"<tr> <td>AutoCommit:      </td> <td><b>" + csi.getAutoCommit()        + "</b> </td> </tr>" +
//			"<tr> <td>Isolation Level: </td> <td><b>" + csi.getIsolationLevelStr() + "</b> </td> </tr>" +
//			"</table>" +
//			"<hr>" + 
//			JDBC_STATE_INFO_TOOLTIP_BASE.replace("<html>", ""); // remove the first/initial <html> tag...
//
//		_srvConnStateInfo.setToolTipText(tooltip);
//	}

	
	//------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------
	// Sub Classes
	//------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------
	public static class ServerInfo
	{
		public String _srvName;
		public String _productName;
		public String _productVersion;
		public String _serverName;
		public String _initialCatalog;
		public String _username;
		public String _withUrl;
		public String _sysListeners;
		public String _pageSizeInKb;
		public String _charset;
		public String _sortorder;

		public String _clientCharsetId;
		public String _clientCharsetName;
		public String _clientCharsetDesc;
		
		public Map<String, Object> _extraInfo;
		
		public ServerInfo(String srvName, String productName, String productVersion, 
				String serverName, String initialCatalog, String username, String withUrl, String sysListeners, 
				String srvPageSizeInKb, String srvCharset, String srvSortorder,
				String clientCharsetId, String clientCharsetName, String clientCharsetDesc, Map<String, Object> extraInfo)
		{
			_srvName           = srvName;
			_productName       = productName;
			_productVersion    = productVersion;
			_serverName        = serverName;
			_initialCatalog    = initialCatalog;
			_username          = username;
			_withUrl           = withUrl;
			_sysListeners      = sysListeners;
			_pageSizeInKb      = srvPageSizeInKb;
			_charset           = srvCharset;
			_sortorder         = srvSortorder;

			_clientCharsetId   = clientCharsetId;
			_clientCharsetName = clientCharsetName;
			_clientCharsetDesc = clientCharsetDesc;
			
			_extraInfo         = extraInfo;
		}

		public String getSrvName()           { return _srvName           != null ? _srvName           : NOT_CONNECTED; }
		public String getProductName()       { return _productName       != null ? _productName       : ""; }
		public String getProductVersion()    { return _productVersion    != null ? _productVersion    : ""; }
		public String getServerName()        { return _serverName        != null ? _serverName        : ""; }
		public String getInitialCatalog()    { return _initialCatalog    != null ? _initialCatalog    : ""; }
		public String getUsername()          { return _username          != null ? _username          : ""; }
		public String getWithUrl()           { return _withUrl           != null ? _withUrl           : ""; }
		public String getSysListeners()      { return _sysListeners      != null ? _sysListeners      : ""; }
		public String getPageSizeKb()        { return _pageSizeInKb      != null ? _pageSizeInKb      : ""; }
		public String getCharset()           { return _charset           != null ? _charset           : ""; }
		public String getSortorder()         { return _sortorder         != null ? _sortorder         : ""; }

		public String getClientCharsetId()   { return _clientCharsetId   != null ? _clientCharsetId   : ""; }
		public String getClientCharsetName() { return _clientCharsetName != null ? _clientCharsetName : ""; }
		public String getClientCharsetDesc() { return _clientCharsetDesc != null ? _clientCharsetDesc : ""; }

		public Map<String, Object> getExtraInfo() { return _extraInfo; }

		public String getProductNameShort()
		{
			String productName = getProductName();
			String productNameShort = "";

			if      (productName.equals(""))                              productNameShort = "";
			else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_ASE))   productNameShort = "ASE";
			else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_ASA))   productNameShort = "ASA";
			else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_IQ))    productNameShort = "IQ";
			else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_RS))    productNameShort = "RS";
			else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_RAX))   productNameShort = "RAX";
			else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_RSDRA)) productNameShort = "RSDRA";
			else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_RSDA))  productNameShort = "RSDA";
			else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_HANA))         productNameShort = "HANA";
			else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MAXDB))        productNameShort = "MaxDB";
			else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_H2))           productNameShort = "H2";
			else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_ORACLE))       productNameShort = "ORA";
			else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MSSQL))        productNameShort = "MS-SQL";
			else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DB2_UX))       productNameShort = "DB2";
			else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DB2_ZOS))      productNameShort = "DB2-MF";
			else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MYSQL))        productNameShort = "MySQL";
			else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DERBY))        productNameShort = "DERBY";
			else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_POSTGRES))     productNameShort = "PG";
			else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_APACHE_HIVE))  productNameShort = "HIVE";
			else productNameShort = "UNKNOWN";
			
			return productNameShort;
		}

		public void   setSrvName          (String srvName)           { _srvName           = srvName; }
		public void   setProductName      (String productName)       { _productName       = productName; }
		public void   setProductVersion   (String productVersion)    { _productVersion    = productVersion; }
		public void   setServerName       (String serverName)        { _serverName        = serverName; }
		public void   setInitialCatalog   (String initialCatalog)    { _initialCatalog    = initialCatalog; }
		public void   setUsername         (String username)          { _username          = username; }
		public void   setWithUrl          (String withUrl)           { _withUrl           = withUrl; }
		public void   setSysListeners     (String sysListeners)      { _sysListeners      = sysListeners; }
		public void   getPageSizeKb       (String srvPageSizeInKb)   { _pageSizeInKb      = srvPageSizeInKb; }
		public void   setCharset          (String charset)           { _charset           = charset; }
		public void   setSortorder        (String sortorder)         { _sortorder         = sortorder; }

		public void   setClientCharsetId  (String clientCharsetId)   { _clientCharsetId   = clientCharsetId; }
		public void   setClientCharsetName(String clientCharsetName) { _clientCharsetName = clientCharsetName; }
		public void   setClientCharsetDesc(String clientCharsetDesc) { _clientCharsetDesc = clientCharsetDesc; }

		public void   setExtraInfo        (Map<String, Object> map)  { _extraInfo         = map; }
	}
}
package com.asetune.gui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.apache.log4j.Logger;

import com.asetune.AseConfig;
import com.asetune.AseConfigText;
import com.asetune.CounterController;
import com.asetune.CounterControllerAse;
import com.asetune.MonTablesDictionary;
import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.CmPCacheModuleUsage;
import com.asetune.cm.ase.CmSummary;
import com.asetune.cm.ase.CmSysLoad;
import com.asetune.cm.sql.VersionInfo;
import com.asetune.gui.ConnectionDialog.Options;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.tools.AseAppTraceDialog;
import com.asetune.tools.AseStackTraceAnalyzer;
import com.asetune.tools.AseStackTraceAnalyzer.AseStackTreeView;
import com.asetune.tools.WindowType;
import com.asetune.tools.sqlcapture.ProcessDetailFrame;
import com.asetune.tools.sqlw.QueryWindow;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.Ver;

public class MainFrameAse 
extends MainFrame
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger = Logger.getLogger(MainFrameAse.class);

	public MainFrameAse()
	{
		super();
	}

	@Override public ImageIcon getApplicationIcon16() { return SwingUtils.readImageIcon(Version.class, "images/asetune_icon.gif"); };
	@Override public ImageIcon getApplicationIcon32() { return SwingUtils.readImageIcon(Version.class, "images/asetune_icon_32.gif"); };

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
			@Override
			public boolean initializeVersionInfo(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				// Just get ASE Version, this will be good for error messages, sent to WEB server, this will write ASE Version in the info...
				MonTablesDictionary.getInstance().initializeVersionInfo(conn, true);
				return true;
			}
			
			@Override
			public boolean checkMonitorConfig(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				return AseConnectionUtils.checkForMonitorOptions(conn, null, true, cpd, "enable monitoring");
			}

			@Override
			public boolean initMonitorDictionary(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				if ( ! ConnectionDialog.checkReconnectVersion(conn) )
					throw new Exception("Connecting to a different ASE Version, This is NOT supported now...");

				MonTablesDictionary.getInstance().initialize(conn, true);
				CounterControllerAse.initExtraMonTablesDictionary();
				
				return true;
			}
			
			@Override
			public boolean initDbServerConfigDictionary(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				AseConfig aseCfg = AseConfig.getInstance();
				if ( ! aseCfg.isInitialized() )
					aseCfg.initialize(conn, true, false, null);

				// initialize ASE Config Text Dictionary
				AseConfigText.initializeAll(conn, true, false, null);

				return true;
			}
			
			@Override
			public boolean initCounterCollector(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception
			{
				CounterController.getInstance().initCounters(
						conn,
						true,
						MonTablesDictionary.getInstance().getAseExecutableVersionNum(),
						MonTablesDictionary.getInstance().isClusterEnabled(),
						MonTablesDictionary.getInstance().getMdaVersion());

				return true;
			}			
		};
	}

	@Override
	public void connectMonitorHookin()
	{
//		// Initialize the MonTablesDictionary
//		// This will serve as a dictionary for ToolTip
//		if ( ! MonTablesDictionary.getInstance().isInitialized() )
//		{
//			MonTablesDictionary.getInstance().initialize(CounterController.getInstance().getMonConnection(), true);
//			GetCounters.initExtraMonTablesDictionary();
//		}
//
//		// initialize ASE Config Dictionary
//		AseConfig aseCfg = AseConfig.getInstance();
//		if ( ! aseCfg.isInitialized() )
//		{
//			aseCfg.initialize(CounterController.getInstance().getMonConnection(), true, false, null);
//		}
//
//		// initialize ASE Config Text Dictionary, this has an internal check if it's already initialized.
//		AseConfigText.initializeAll(CounterController.getInstance().getMonConnection(), true, false, null);


		// if not MON_ROLE or MonitoringNotEnabled, show all GRAPHS available for this mode (global variables)
		boolean setMinimalGraphConfig = false;
//		Connection conn = AseTune.getCounterCollector().getMonConnection();
		Connection conn = CounterController.getInstance().getMonConnection();

		boolean hasMonRole          = AseConnectionUtils.hasRole(conn, AseConnectionUtils.MON_ROLE);
		boolean hasEnableMonitoring = AseConnectionUtils.getAseConfigRunValueBooleanNoEx(conn, "enable monitoring");
		int     aseVersion          = AseConnectionUtils.getAseVersionNumber(conn); 

		// MON_ROLE
		if ( ! hasMonRole )
			setMinimalGraphConfig = true;

		// enable monitoring
		if ( ! hasEnableMonitoring )
			setMinimalGraphConfig = true;

		if ( setMinimalGraphConfig )
		{
			// GRAPHS in SUMMARY
			CountersModel cm = CounterController.getInstance().getCmByName(CmSummary.CM_NAME);
			if (cm != null)
			{
				cm.setTrendGraphEnable(CmSummary.GRAPH_NAME_AA_CPU,             true);
				cm.setTrendGraphEnable(CmSummary.GRAPH_NAME_CONNECTION,         true);
				cm.setTrendGraphEnable(CmSummary.GRAPH_NAME_AA_DISK_READ_WRITE, true);
				cm.setTrendGraphEnable(CmSummary.GRAPH_NAME_AA_NW_PACKET,       true);
				cm.setTrendGraphEnable(CmSummary.GRAPH_NAME_OLDEST_TRAN_IN_SEC, true);

				if (aseVersion >= Ver.ver(15,0,3,3) && hasMonRole)
					cm.setTrendGraphEnable(CmSummary.GRAPH_NAME_TRANSACTION,    true);
			}

			// GRAPHS in SYSLOAD
			if (aseVersion >= Ver.ver(15,5) && hasMonRole)
			{
				cm = CounterController.getInstance().getCmByName(CmSysLoad.CM_NAME);
				if (cm != null)
					cm.setTrendGraphEnable(CmSysLoad.GRAPH_NAME_ENGINE_RUN_QUEUE_LENTH, true);
			}

			// GRAPHS in PROC_CACHE_MODULE_USAGE
			if (aseVersion >= Ver.ver(15,0,1) && hasMonRole)
			{
				cm = CounterController.getInstance().getCmByName(CmPCacheModuleUsage.CM_NAME);
				if (cm != null)
					cm.setTrendGraphEnable(CmPCacheModuleUsage.GRAPH_NAME_MODULE_USAGE, true);
			}
		} // end: setMinimalGraphConfig
	}

	@Override
	public void connectOfflineHookin()
	{
		// Read in the MonTablesDictionary from the offline store
		// This will serve as a dictionary for ToolTip
		MonTablesDictionary.getInstance().initializeMonTabColHelper(getOfflineConnection(), true);
		CounterControllerAse.initExtraMonTablesDictionary();

		// initialize ASE Config Dictionary
		AseConfig.getInstance().initialize(getOfflineConnection(), true, true, null);

		// initialize ASE Config Text Dictionary
		AseConfigText.initializeAll(getOfflineConnection(), true, true, null);
	}

//	@Override
//	public void sendConnectInfoNoBlock(int connType, SshTunnelInfo sshTunnelInfo)
//	{
//		CheckForUpdates.sendConnectInfoNoBlock(connType, sshTunnelInfo);
//	}
//
//	@Override
//	public void sendCounterUsageInfo(boolean blockingCall)
//	{
//		CheckForUpdates.sendCounterUsageInfo(blockingCall);
//	}

	@Override
	public boolean disconnectAbort(boolean canBeAborted)
	{
//		int answer = AseConfigMonitoringDialog.onExit(_instance, AseTune.getCounterCollector().getMonConnection(), canBeAborted);
		int answer = AseConfigMonitoringDialog.onExit(this, CounterController.getInstance().getMonConnection(), canBeAborted);

		// This means that a "prompt" was raised to ask for "disable monitoring"
		// AND the user pressed ABORT/CANCEL button
		if (answer > 0)
		{
			return true;
		}
		
		return false;
	}

	@Override
	public void disconnectHookin(WaitForExecDialog waitDialog)
	{
		waitDialog.setState("Clearing ASE Config Dictionary.");
		AseConfig.reset();
		AseConfigText.reset();

		waitDialog.setState("Clearing Mon Tables Dictionary.");
		MonTablesDictionary.reset();      // Most probably need to work more on this one...

		//waitDialog.setState("Clearing Wait Event Dictionary.");
		//MonWaitEventIdDictionary.reset(); // Do not need to be reset, it's not getting anything from DB
	}

	
	
	@Override
	public void actionPerformed(ActionEvent e, Object source, String actionCmd)
	{
		//-----------------------------
		// MENU - VIEW
		//-----------------------------
		if (ACTION_OPEN_ASE_CONFIG_VIEW.equals(actionCmd))
			AseConfigViewDialog.showDialog(this, this);

		//-----------------------------
		// MENU - TOOLS
		//-----------------------------
		if (ACTION_OPEN_ASE_CONFIG_MON.equals(actionCmd))
		{
//			Connection conn = CounterController.getInstance().getMonConnection();
			DbxConnection conn = CounterController.getInstance().getMonConnection();
			AseConfigMonitoringDialog.showDialog(this, conn, -1);

			// If monitoring is NOT enabled anymore, do disconnect
			// By the way, changes can only be made if you have SA_ROLE
			boolean hasSaRole           = AseConnectionUtils.hasRole(conn, AseConnectionUtils.SA_ROLE);
			boolean isMonitoringEnabled = AseConnectionUtils.getAseConfigRunValueBooleanNoEx(conn, "enable monitoring");
			if ( ! isMonitoringEnabled && hasSaRole )
				super.action_disconnect(e, false);
		}

		if (ACTION_OPEN_TCP_PANEL_CONFIG.equals(actionCmd))
			TcpConfigDialog.showDialog(this);

		if (ACTION_OPEN_CAPTURE_SQL.equals(actionCmd))
			new ProcessDetailFrame(this, -1, -1);

		if (ACTION_OPEN_ASE_APP_TRACE.equals(actionCmd))
		{
			String servername    = MonTablesDictionary.getInstance().getAseServerName();
			String aseVersionStr = MonTablesDictionary.getInstance().getAseExecutableVersionStr();
			int    aseVersionNum = MonTablesDictionary.getInstance().getAseExecutableVersionNum();
			if (aseVersionNum >= Ver.ver(15,0,2))
			{
				AseAppTraceDialog apptrace = new AseAppTraceDialog(-1, servername, aseVersionStr);
				apptrace.setVisible(true);
			}
			else
			{
				// NOT supported in ASE versions below 15.0.2
				String htmlMsg = 
					"<html>" +
					"  <h2>Sorry this functionality is not available in ASE "+Ver.versionIntToStr(aseVersionNum)+"</h2>" +
					"  Application Tracing is introduced in ASE 15.0.2" +
					"</html>";
				SwingUtils.showInfoMessage(this, "Not supported for this ASE Version", htmlMsg);
			}
		}

		if (ACTION_OPEN_ASE_STACKTRACE_TOOL.equals(actionCmd))
		{
			AseStackTraceAnalyzer.AseStackTreeView view = new AseStackTreeView(null);
			view.setVisible(true);
		}

	}

	public static final String ACTION_OPEN_ASE_CONFIG_VIEW              = "ACTION_OPEN_ASE_CONFIG_VIEW";
	public static final String ACTION_OPEN_ASE_CONFIG_MON               = "OPEN_ASE_CONFIG_MON";
	public static final String ACTION_OPEN_CAPTURE_SQL                  = "OPEN_CAPTURE_SQL";
	public static final String ACTION_OPEN_ASE_APP_TRACE                = "OPEN_ASE_APP_TRACE";
	public static final String ACTION_OPEN_ASE_STACKTRACE_TOOL          = "OPEN_ASE_STACKTRACE_TOOL";

	private JMenuItem           _aseConfigView_mi;

	private JMenuItem           _aseConfMon_mi;
	private JMenuItem           _captureSql_mi;
	private JMenuItem           _aseAppTrace_mi;
	private JMenuItem           _aseStackTraceAnalyzer_mi;
//	private JMenuItem           _lockTool_mi;
	private JMenu               _preDefinedSql_m;

	@Override
	protected JMenu createFileMenu()
	{
		JMenu menu = super.createFileMenu();
		return menu;
	}

	@Override
	protected JMenu createViewMenu()
	{
		JMenu menu = super.createViewMenu();

		_aseConfigView_mi              = new JMenuItem("View ASE Configuration...");
		_aseConfigView_mi             .setIcon(SwingUtils.readImageIcon(Version.class, "images/config_ase_view.png"));
		_aseConfigView_mi             .setActionCommand(ACTION_OPEN_ASE_CONFIG_VIEW);
		_aseConfigView_mi             .addActionListener(this);

		menu.add(_aseConfigView_mi, 2);

		return menu;
	}

	@Override
	protected JMenu createToolsMenu()
	{
		JMenu menu = super.createToolsMenu();

		_aseConfMon_mi                 = new JMenuItem("Configure ASE for Monitoring...");
		_captureSql_mi                 = new JMenuItem("Capture SQL...");
		_aseAppTrace_mi                = new JMenuItem("ASE Application Tracing...");
		_aseStackTraceAnalyzer_mi      = new JMenuItem("ASE StackTrace Analyzer...");
		_preDefinedSql_m               = createPredefinedSqlMenu(this);
//		_lockTool_mi                   = new JMenuItem("Lock Tool (NOT YET IMPLEMENTED)");

		_aseConfMon_mi                .setIcon(SwingUtils.readImageIcon(Version.class, "images/config_ase_mon.png"));
		_captureSql_mi                .setIcon(SwingUtils.readImageIcon(Version.class, "images/capture_sql_tool.gif"));
		_aseAppTrace_mi               .setIcon(SwingUtils.readImageIcon(Version.class, "images/ase_app_trace_tool.png"));
		_aseStackTraceAnalyzer_mi     .setIcon(SwingUtils.readImageIcon(Version.class, "images/ase_stack_trace_tool.png"));
//		_lockTool_mi                  .setIcon(SwingUtils.readImageIcon(Version.class, "images/locktool16.gif"));

		_aseConfMon_mi                .setActionCommand(ACTION_OPEN_ASE_CONFIG_MON);
		_captureSql_mi                .setActionCommand(ACTION_OPEN_CAPTURE_SQL);
		_aseAppTrace_mi               .setActionCommand(ACTION_OPEN_ASE_APP_TRACE);
		_aseStackTraceAnalyzer_mi     .setActionCommand(ACTION_OPEN_ASE_STACKTRACE_TOOL);
//		_lockTool_mi                  .setActionCommand(ACTION_OPEN_LOCK_TOOL);

		_aseConfMon_mi                .addActionListener(this);
		_captureSql_mi                .addActionListener(this);
		_aseAppTrace_mi               .addActionListener(this);
		_aseStackTraceAnalyzer_mi     .addActionListener(this);
//		_lockTool_mi                  .addActionListener(this);

		_aseConfMon_mi               .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		menu.add(_aseConfMon_mi,            0);
		// here should 'Tail/View DB Server Log' be, but it's added in the super.createToolsMenu();
		menu.add(_captureSql_mi,            2);
		menu.add(_aseAppTrace_mi,           3);
		menu.add(_aseStackTraceAnalyzer_mi, 4);
		if (_preDefinedSql_m != null) 
			menu.add(_preDefinedSql_m,      5);

		return menu;
	}

	@Override
	protected JMenu createHelpMenu()
	{
		JMenu menu = super.createHelpMenu();
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
			_aseConfigView_mi             .setEnabled(true);
			
			// Tools
			_aseConfMon_mi                .setEnabled(true);
			_captureSql_mi                .setEnabled(true);
			_aseAppTrace_mi               .setEnabled(true);
			_aseStackTraceAnalyzer_mi     .setEnabled(true); // always TRUE
			_preDefinedSql_m              .setEnabled(true);
//			_lockTool_mi                  .setEnabled(true);

			// Help
		}
		else if (status == ST_OFFLINE_CONNECT)
		{
			// TOOLBAR

			// File

			// View
			_aseConfigView_mi             .setEnabled(true);

			// Tools
			_aseConfMon_mi                .setEnabled(false);
			_captureSql_mi                .setEnabled(false);
			_aseAppTrace_mi               .setEnabled(false);
			_aseStackTraceAnalyzer_mi     .setEnabled(true); // always TRUE
			_preDefinedSql_m              .setEnabled(false);
//			_lockTool_mi                  .setEnabled(false);

			// Help
		}
		else if (status == ST_DISCONNECT)
		{
			// TOOLBAR

			// File

			// View
			_aseConfigView_mi             .setEnabled(false);

			// Tools
			_aseConfMon_mi                .setEnabled(false);
			_captureSql_mi                .setEnabled(false);
			_aseAppTrace_mi               .setEnabled(false);
			_aseStackTraceAnalyzer_mi     .setEnabled(true); // always TRUE
			_preDefinedSql_m              .setEnabled(false);
//			_lockTool_mi                  .setEnabled(false);

			// Help
		}
	}


	public static JMenu createPredefinedSqlMenu(final Object callerInstance)
	{
		_logger.debug("createPredefinedSqlMenu(): called.");

		final JMenu menu = new JMenu("Predefined SQL Statements");
		menu.setToolTipText("<html>This is a bunch of stored procedures...<br>If the prcocedure doesn't exist. It will be created.<br>The user you are logged in as need to have the correct priviliges to create procedues in sybsystemprocs.</html>");;
		menu.setIcon(SwingUtils.readImageIcon(Version.class, "images/pre_defined_sql_statement.png"));

		Configuration systmp = new Configuration();
		
		//----- sp_list_unused_indexes.sql -----
		systmp.setProperty("system.predefined.sql.01.name",                        "<html><b>sp_list_unused_indexes</b> - <i><font color=\"green\">List unused indexes in all databases.</font></i></html>");
//		systmp.setProperty("system.predefined.sql.01.name",                        "List unused indexes in all databases.");
		systmp.setProperty("system.predefined.sql.01.execute",                     "exec sp_list_unused_indexes");
		systmp.setProperty("system.predefined.sql.01.install.needsVersion",        "0");
		systmp.setProperty("system.predefined.sql.01.install.dbname",              "sybsystemprocs");
		systmp.setProperty("system.predefined.sql.01.install.procName",            "sp_list_unused_indexes");
		systmp.setProperty("system.predefined.sql.01.install.procDateThreshold",   VersionInfo.SP_LIST_UNUSED_INDEXES_CR_STR);
		systmp.setProperty("system.predefined.sql.01.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
		systmp.setProperty("system.predefined.sql.01.install.scriptName",          "sp_list_unused_indexes.sql");
		systmp.setProperty("system.predefined.sql.01.install.needsRole",           "sa_role");

		//----- sp_whoisw.sql -----
		systmp.setProperty("system.predefined.sql.02.name",                        "<html><b>sp_whoisw</b> - <i><font color=\"green\">Who Is Working (list SPID's that are doing stuff).</font></i></html>");
//		systmp.setProperty("system.predefined.sql.02.name",                        "sp_whoisw - Who Is Working (list SPID's that are doing stuff).");
		systmp.setProperty("system.predefined.sql.02.execute",                     "exec sp_whoisw");
		systmp.setProperty("system.predefined.sql.02.install.needsVersion",        "0");
		systmp.setProperty("system.predefined.sql.02.install.dbname",              "sybsystemprocs");
		systmp.setProperty("system.predefined.sql.02.install.procName",            "sp_whoisw");
		systmp.setProperty("system.predefined.sql.02.install.procDateThreshold",   VersionInfo.SP_WHOISW_CR_STR);
		systmp.setProperty("system.predefined.sql.02.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
		systmp.setProperty("system.predefined.sql.02.install.scriptName",          "sp_whoisw.sql");
		systmp.setProperty("system.predefined.sql.02.install.needsRole",           "sa_role");

		//----- sp_whoisb.sql -----
		systmp.setProperty("system.predefined.sql.03.name",                        "<html><b>sp_whoisb</b> - <i><font color=\"green\">Who Is Blocking (list info about SPID's taht are blocking other SPID's from running).</font></i></html>");
//		systmp.setProperty("system.predefined.sql.03.name",                        "sp_whoisb - Who Is Blocking (list info about SPID's taht are blocking other SPID's from running).");
		systmp.setProperty("system.predefined.sql.03.execute",                     "exec sp_whoisb");
		systmp.setProperty("system.predefined.sql.03.install.needsVersion",        "0");
		systmp.setProperty("system.predefined.sql.03.install.dbname",              "sybsystemprocs");
		systmp.setProperty("system.predefined.sql.03.install.procName",            "sp_whoisb");
		systmp.setProperty("system.predefined.sql.03.install.procDateThreshold",   VersionInfo.SP_WHOISB_CR_STR);
		systmp.setProperty("system.predefined.sql.03.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
		systmp.setProperty("system.predefined.sql.03.install.scriptName",          "sp_whoisb.sql");
		systmp.setProperty("system.predefined.sql.03.install.needsRole",           "sa_role");

		//----- sp_opentran.sql -----
		systmp.setProperty("system.predefined.sql.04.name",                        "<html><b>sp_opentran</b> - <i><font color=\"green\">List information about the SPID that is holding the oldest open transaction (using syslogshold).</font></i></html>");
//		systmp.setProperty("system.predefined.sql.04.name",                        "sp_opentran - List information about the SPID that is holding the oldest open transaction (using syslogshold).");
		systmp.setProperty("system.predefined.sql.04.execute",                     "exec sp_opentran");
		systmp.setProperty("system.predefined.sql.04.install.needsVersion",        "0");
		systmp.setProperty("system.predefined.sql.04.install.dbname",              "sybsystemprocs");
		systmp.setProperty("system.predefined.sql.04.install.procName",            "sp_opentran");
		systmp.setProperty("system.predefined.sql.04.install.procDateThreshold",   VersionInfo.SP_OPENTRAN_CR_STR);
		systmp.setProperty("system.predefined.sql.04.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
		systmp.setProperty("system.predefined.sql.04.install.scriptName",          "sp_opentran.sql");
		systmp.setProperty("system.predefined.sql.04.install.needsRole",           "sa_role");

		//----- sp_lock2.sql -----
		systmp.setProperty("system.predefined.sql.05.name",                        "<html><b>sp_lock2</b> - <i><font color=\"green\">More or less the same as sp_lock, but uses 'table name' instead of 'table id'.</font></i></html>");
//		systmp.setProperty("system.predefined.sql.05.name",                        "sp_lock2 - More or less the same as sp_lock, but uses 'table name' instead of 'table id'.");
		systmp.setProperty("system.predefined.sql.05.execute",                     "exec sp_lock2");
		systmp.setProperty("system.predefined.sql.05.install.needsVersion",        "0");
		systmp.setProperty("system.predefined.sql.05.install.dbname",              "sybsystemprocs");
		systmp.setProperty("system.predefined.sql.05.install.procName",            "sp_lock2");
		systmp.setProperty("system.predefined.sql.05.install.procDateThreshold",   VersionInfo.SP_LOCK2_CR_STR);
		systmp.setProperty("system.predefined.sql.05.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
		systmp.setProperty("system.predefined.sql.05.install.scriptName",          "sp_lock2.sql");
		systmp.setProperty("system.predefined.sql.05.install.needsRole",           "sa_role");

		//----- sp_locksum.sql -----
		systmp.setProperty("system.predefined.sql.06.name",                        "<html><b>sp_locksum</b> - <i><font color=\"green\">Prints number of locks each SPID has.</font></i></html>");
//		systmp.setProperty("system.predefined.sql.06.name",                        "sp_locksum - Prints number of locks each SPID has.");
		systmp.setProperty("system.predefined.sql.06.execute",                     "exec sp_locksum");
		systmp.setProperty("system.predefined.sql.06.install.needsVersion",        "0");
		systmp.setProperty("system.predefined.sql.06.install.dbname",              "sybsystemprocs");
		systmp.setProperty("system.predefined.sql.06.install.procName",            "sp_locksum");
		systmp.setProperty("system.predefined.sql.06.install.procDateThreshold",   VersionInfo.SP_LOCKSUM_CR_STR);
		systmp.setProperty("system.predefined.sql.06.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
		systmp.setProperty("system.predefined.sql.06.install.scriptName",          "sp_locksum.sql");
		systmp.setProperty("system.predefined.sql.06.install.needsRole",           "sa_role");

		//----- sp_spaceused2.sql -----
		systmp.setProperty("system.predefined.sql.07.name",                        "<html><b>sp_spaceused2</b> - <i><font color=\"green\">List space and row used by each table in the current database.</font></i></html>");
//		systmp.setProperty("system.predefined.sql.07.name",                        "sp_spaceused2 - List space and row used by each table in the current database");
		systmp.setProperty("system.predefined.sql.07.execute",                     "exec sp_spaceused2");
//		systmp.setProperty("system.predefined.sql.07.install.needsVersion",        "15000");
//		systmp.setProperty("system.predefined.sql.07.install.needsVersion",        "1500000");
		systmp.setProperty("system.predefined.sql.07.install.needsVersion",        Ver.ver(15,0));
		systmp.setProperty("system.predefined.sql.07.install.dbname",              "sybsystemprocs");
		systmp.setProperty("system.predefined.sql.07.install.procName",            "sp_spaceused2");
		systmp.setProperty("system.predefined.sql.07.install.procDateThreshold",   VersionInfo.SP_SPACEUSED2_CR_STR);
		systmp.setProperty("system.predefined.sql.07.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
		systmp.setProperty("system.predefined.sql.07.install.scriptName",          "sp_spaceused2.sql");
		systmp.setProperty("system.predefined.sql.07.install.needsRole",           "sa_role");

		createPredefinedSqlMenu(menu, "system.predefined.sql.", systmp, callerInstance);
		createPredefinedSqlMenu(menu, "user.predefined.sql.",   null,   callerInstance);

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
			final String menuItemName      = conf.getProperty(prefixStr   +".name");
			final String sqlStr            = conf.getProperty(prefixStr   +".execute");
			final int    needsVersion      = conf.getIntProperty(prefixStr+".install.needsVersion", 0); 
			final String dbname            = conf.getProperty(prefixStr   +".install.dbname"); 
			final String procName          = conf.getProperty(prefixStr   +".install.procName"); 
			final String procDateThreshStr = conf.getProperty(prefixStr   +".install.procDateThreshold"); 
			final String scriptLocationStr = conf.getProperty(prefixStr   +".install.scriptLocation"); 
			final String scriptName        = conf.getProperty(prefixStr   +".install.scriptName"); 
			final String needsRole         = conf.getProperty(prefixStr   +".install.needsRole"); 

			//---------------------------------------
			// Check that we got everything we needed
			//---------------------------------------
			if (menuItemName == null)
			{
				_logger.warn("Missing property '"+prefixStr+".name'");
				continue;
			}
			if (sqlStr == null)
			{
				_logger.warn("Missing property '"+prefixStr+".execute'");
				continue;
			}

			//---------------------------------------
			// is any "install" there?
			//---------------------------------------
			boolean tmpDoCheckCreate = false;
			if (dbname != null)
			{
				tmpDoCheckCreate = true;

				String missing = "";
				// Check rest of the mandatory parameters
				if (procName          == null) missing += prefixStr   +".install.procName, ";
				if (procDateThreshStr == null) missing += prefixStr   +".install.procDateThreshold, ";
//				if (scriptLocationStr == null) missing += prefixStr   +".install.scriptLocation, ";
				if (scriptName        == null) missing += prefixStr   +".install.scriptName, ";
				
				if ( ! missing.equals("") )
				{
					_logger.warn("Missing property '"+missing+"'.");
					continue;
				}
			}
			final boolean doCheckCreate = tmpDoCheckCreate;

			
			//---------------------------------------
			// if 'install.scriptLocation' is used...
			//---------------------------------------
			Class<?> tmpScriptLocation = null;
			if (scriptLocationStr != null)
			{
				try { tmpScriptLocation = Class.forName(scriptLocationStr); }
				catch (ClassNotFoundException e)
				{
					_logger.warn("Property "+prefixStr+".install.scriptLocation, contained '"+scriptLocationStr+"', the class can't be loaded. This should be a classname in asetune.jar, If it's a path name you want to specify, please use the property '"+prefixStr+".install.scriptName' instead.");
					continue;
				}
			}
			final Class<?> scriptLocation = tmpScriptLocation;

			
			//---------------------------------------
			// if 'install.procDateThreshold' is used...
			//---------------------------------------
			java.util.Date tmpProcDateThreshold = null;
			if (procDateThreshStr != null)
			{
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				try { tmpProcDateThreshold = sdf.parse(procDateThreshStr); }
				catch (ParseException e)
				{
					_logger.warn("Property "+prefixStr+".install.procDateThreshold, contained '"+procDateThreshStr+"', Problems parsing the string, it should look like 'yyyy-MM-dd'.");
					continue;
				}
			}
			final java.util.Date procDateThreshold = tmpProcDateThreshold;

			
			//---------------------------------------
			// Create the executor class
			//---------------------------------------
			ActionListener action = new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
//					Connection conn = null;
					DbxConnection conn = null;
					try
					{
						QueryWindow queryWindowInstance = null;
						MainFrame   dbxTuneInstance     = null;
						
						if (callerInstance != null)
						{
							if (callerInstance instanceof QueryWindow) queryWindowInstance = (QueryWindow) callerInstance;
							if (callerInstance instanceof MainFrame)   dbxTuneInstance     = (MainFrame) callerInstance;

							if (dbxTuneInstance != null)
							{
								// Check that we are connected
//								if ( ! AseTune.getCounterCollector().isMonConnected(true, true) )
								if ( ! CounterController.getInstance().isMonConnected(true, true) )
								{
									SwingUtils.showInfoMessage(MainFrame.getInstance(), "Not connected", "Not yet connected to a server.");
									return;
								}
								// Get a new connection
//								conn = AseConnectionFactory.getConnection(null, Version.getAppName()+"-PreDefinedSql", null);
//								conn = DbxConnection.connect(Version.getAppName()+"-PreDefinedSql");
								// or even better, use a ConnectionProvider...
								conn = dbxTuneInstance.getNewConnection(Version.getAppName()+"-PreDefinedSql");
							
								// Check if the procedure exists (and create it if it dosn't)
								if (doCheckCreate)
									AseConnectionUtils.checkCreateStoredProc(conn, needsVersion, dbname, procName, procDateThreshold, scriptLocation, scriptName, needsRole);

								// Open the SQL Window
								QueryWindow qf = new QueryWindow(conn, sqlStr, null, true, WindowType.JFRAME, null);
								qf.openTheWindow();
							}

							if (queryWindowInstance != null)
							{
								// Get the Connection used by sqlWindow
								conn = queryWindowInstance.getConnection();

								// Check if the procedure exists (and create it if it dosn't)
								if (doCheckCreate)
									AseConnectionUtils.checkCreateStoredProc(conn, needsVersion, dbname, procName, procDateThreshold, scriptLocation, scriptName, needsRole);

								// Execute the query
								queryWindowInstance.displayQueryResults(sqlStr, 0, false);
							}
						}
					}
					catch (Throwable t)
					{
						String msg = "Problems when checking/creating the procedure";
						if (conn == null) 
							msg = "Problems when getting a database connection";

						SwingUtils.showErrorMessage("Problems executing Predefined SQL statement", 
								msg + "\n\n" + t.getMessage(), t);
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

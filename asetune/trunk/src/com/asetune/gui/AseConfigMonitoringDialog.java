/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.asetune.CounterController;
import com.asetune.ICounterController;
import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.gui.swing.WaitForExecDialog.BgExecutor;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.Ver;


public class AseConfigMonitoringDialog
    extends JDialog
    implements ActionListener
{
	private static final long serialVersionUID = 7587303888489834272L;
	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(AseConfigMonitoringDialog.class);
	
	private static final String msgDialogTitle    = "ASE monitor configuration";

	private static final String PDC_OPTIONS_STR[] = { "From ASE", "None", "Minimal", "Small", "Medium", "Large" };

	private static final int PDC_FROM_ASE = 0;
	private static final int PDC_NONE     = 1;
	private static final int PDC_MINIMAL  = 2;
	private static final int PDC_SMALL    = 3;
	private static final int PDC_MEDIUM   = 4;
	private static final int PDC_LARGE    = 5;

	private static final String ON_EXIT_STR[] = { "none", "disable", "prompt" };

	private static final int ON_EXIT_NONE    = 0;
	private static final int ON_EXIT_DISABLE = 1;
	private static final int ON_EXIT_PROMPT  = 2;

	private static final String default_configurabelMemoryText = " MB available for reconfiguration.";

	private static final String ASE_CONFIG       = "ASE_CONFIG";

//	private Connection         _conn             = null;
	private DbxConnection      _conn             = null;
	private boolean            _configErrors     = false;

	private int                _aseVersionNum    = 0;
	private boolean            _isClusterEnabled = false;
	private boolean            _isXfsLicenseEnabled = true;
	
	/** Save tool tip in a Map so we can restore them later */
	private ToolTipStore       _tts              = new ToolTipStore();

	
	// PANEL: MONITORING
	private JPanel             _monitoringPanel_1                = null;
	private JPanel             _monitoringPanel_2                = null;
	private JPanel             _monitoringPanel_3                = null;
	private JCheckBox          _enableMonitoring_chk             = new JCheckBox("Enable monitoring");
	private JCheckBox          _perObjectStatisticsActive_chk    = new JCheckBox("Per object statistics active");
	private JCheckBox          _statementStatisticsActive_chk    = new JCheckBox("Statement statistics active");
	private JCheckBox          _enableSpinlockMonitoring_chk     = new JCheckBox("Enable Spinlock Monitoring");
	private JCheckBox          _executionTimeMonitoring_chk      = new JCheckBox("Execution Time Monitoring");
	private JCheckBox          _statementCacheMonitoring_chk     = new JCheckBox("Statement Cache Monitoring");
	private JCheckBox          _captureCompressionStatistics_chk = new JCheckBox("Capture Compression Statistics");
	private JCheckBox          _objectLockwaitTiming_chk         = new JCheckBox("Object lockwait timing");
	private JCheckBox          _processWaitEvents_chk            = new JCheckBox("Process wait events");
	private JCheckBox          _sqlBatchCapture_chk              = new JCheckBox("SQL batch capture");
	private JCheckBox          _waitEventTiming_chk              = new JCheckBox("Wait event timing");

	private JCheckBox          _lockTimeoutPipeActive_chk     = new JCheckBox("Lock Timeout pipe active");
	private SpinnerNumberModel _lockTimeoutPipeMaxMessages_spm= new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 500); // value, min, max, step
	private JSpinner           _lockTimeoutPipeMaxMessages_sp = new JSpinner(_lockTimeoutPipeMaxMessages_spm);

	private JCheckBox          _deadlockPipeActive_chk        = new JCheckBox("Deadlock pipe active");
	private SpinnerNumberModel _deadlockPipeMaxMessages_spm   = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 500); // value, min, max, step
	private JSpinner           _deadlockPipeMaxMessages_sp    = new JSpinner(_deadlockPipeMaxMessages_spm);

	private JCheckBox          _errorlogPipeActive_chk        = new JCheckBox("Errorlog pipe active");
	private SpinnerNumberModel _errorlogPipeMaxMessages_spm   = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 500); // value, min, max, step
	private JSpinner           _errorlogPipeMaxMessages_sp    = new JSpinner(_errorlogPipeMaxMessages_spm);

	private JCheckBox          _thresholdEventMonitoring_chk  = new JCheckBox("Threshold event monitoring");
	private SpinnerNumberModel _thresholdEventMaxMessages_spm = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 2000); // value, min, max, step
	private JSpinner           _thresholdEventMaxMessages_sp  = new JSpinner(_thresholdEventMaxMessages_spm);

	private JCheckBox          _sqlTextPipeActive_chk         = new JCheckBox("SQL text pipe active");
	private SpinnerNumberModel _sqlTextPipeMaxMessages_spm    = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1000); // value, min, max, step
	private JSpinner           _sqlTextPipeMaxMessages_sp     = new JSpinner(_sqlTextPipeMaxMessages_spm);

	private JCheckBox          _statementPipeActive_chk       = new JCheckBox("Statement pipe active");
	private SpinnerNumberModel _statementPipeMaxMessages_spm  = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 2000); // value, min, max, step
	private JSpinner           _statementPipeMaxMessages_sp   = new JSpinner(_statementPipeMaxMessages_spm);

	private JCheckBox          _planTextPipeActive_chk        = new JCheckBox("Plan text pipe active");
	private SpinnerNumberModel _planTextPipeMaxMessages_spm   = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 3000); // value, min, max, step
	private JSpinner           _planTextPipeMaxMessages_sp    = new JSpinner(_planTextPipeMaxMessages_spm);

	private JLabel             _maxSqlTextMonitored_lbl       = new JLabel("max SQL text monitored");
	private SpinnerNumberModel _maxSqlTextMonitored_spm       = new SpinnerNumberModel(0, 0, Short.MAX_VALUE, 512); // value, min, max, step
	private JSpinner           _maxSqlTextMonitored_sp        = new JSpinner(_maxSqlTextMonitored_spm);

	private JLabel             _predefinedConfigs_lbl         = new JLabel("Configuration Templates");
	private JComboBox          _predefinedConfigs_cbx         = new JComboBox(PDC_OPTIONS_STR);

	private JLabel             _configurabelMemory_lbl        = new JLabel("#"+default_configurabelMemoryText);

	// PANEL: OTHER
	private JCheckBox          _cfgCapMissingStatistics_chk   = new JCheckBox("Capture Missing Statistics");

	private JCheckBox          _cfgEnableMetricsCapture_chk   = new JCheckBox("Enable QP Metrics Capture");

	private JLabel             _cfgMetricsElapMax_lbl         = new JLabel("metrics elap max");
	private SpinnerNumberModel _cfgMetricsElapMax_spm         = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 100); // value, min, max, step
	private JSpinner           _cfgMetricsElapMax_sp          = new JSpinner(_cfgMetricsElapMax_spm);

	private JLabel             _cfgMetricsExecMax_lbl         = new JLabel("metrics exec max");
	private SpinnerNumberModel _cfgMetricsExecMax_spm         = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 100); // value, min, max, step
	private JSpinner           _cfgMetricsExecMax_sp          = new JSpinner(_cfgMetricsExecMax_spm);

	private JLabel             _cfgMetricsLioMax_lbl          = new JLabel("metrics lio max");
	private SpinnerNumberModel _cfgMetricsLioMax_spm          = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1000); // value, min, max, step
	private JSpinner           _cfgMetricsLioMax_sp           = new JSpinner(_cfgMetricsLioMax_spm);

	private JLabel             _cfgMetricsPioMax_lbl          = new JLabel("metrics pio max");
	private SpinnerNumberModel _cfgMetricsPioMax_spm          = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 100); // value, min, max, step
	private JSpinner           _cfgMetricsPioMax_sp           = new JSpinner(_cfgMetricsPioMax_spm);

	// TODO: Check that the License check WORKS on a 12.5.x server
	private JCheckBox          _enableFileAccess_chk          = new JCheckBox("Enable ASE to Read OS Files");

	// PANEL: ON-EXIT
	private JRadioButton       _onExitDoNotDisable_rb         = new JRadioButton("Do Not Disable", true);
	private JRadioButton       _onExitAutoDisable_rb          = new JRadioButton("Automatically");
	private JRadioButton       _onExitAsk_rb                  = new JRadioButton("Prompt");

	// PANEL: OK-CANCEL
	private JButton            _ok                            = new JButton("OK");
	private JButton            _cancel                        = new JButton("Cancel");
	private JButton            _apply                         = new JButton("Apply");
	
	
	/*---------------------------------------------------
	** BEGIN: constructors
	**---------------------------------------------------
	*/
//	private AseConfigMonitoringDialog(Frame owner, Connection conn, int aseVersionNum, String title)
	private AseConfigMonitoringDialog(Frame owner, DbxConnection conn, int aseVersionNum, String title)
	{
		super(owner, title, true);
		init(owner, conn, aseVersionNum);
	}
//	private AseConfigMonitoringDialog(Dialog owner, Connection conn, int aseVersionNum, String title)
	private AseConfigMonitoringDialog(Dialog owner, DbxConnection conn, int aseVersionNum, String title)
	{
		super(owner, title, true);
		init(owner, conn, aseVersionNum);
	}
//	private void init(Window owner, Connection conn, int aseVersionNum)
	private void init(Window owner, DbxConnection conn, int aseVersionNum)
	{

		_conn = conn;
		
		setAseVersion(aseVersionNum);
		if ( AseConnectionUtils.isConnectionOk(_conn, false, null) )
		{
			// Try to get a new version number if it doesn't exist...
			if (_aseVersionNum <= 0)
			{
				_aseVersionNum    = AseConnectionUtils.getAseVersionNumber(_conn);
				_isClusterEnabled = AseConnectionUtils.isClusterEnabled(_conn);

				_isXfsLicenseEnabled = isXfsLicenseEnabled(_conn);

				_logger.debug("init() Need to refresh the ASE Server version number, it is now '"+_aseVersionNum+"', isClusterEnabled="+_isClusterEnabled+".");
			}
		}

		initComponents();
		pack();

		// Set a reasonable size on the window/dialog
		SwingUtils.setSizeWithingScreenLimit(this, 10);

		setLocationRelativeTo(owner);

		setFocus();
	}


//	public static void showDialog(Frame owner, Connection conn, int aseVersionNum)
	public static void showDialog(Frame owner, DbxConnection conn, int aseVersionNum)
	{
		AseConfigMonitoringDialog dialog = new AseConfigMonitoringDialog(owner, conn, aseVersionNum, msgDialogTitle);

		if ( ! AseConnectionUtils.isConnectionOk(conn, true, owner) )
			return;

		dialog.setVisible(true);
		dialog.dispose();
	}
//	public static void showDialog(Dialog owner, Connection conn, int aseVersionNum)
	public static void showDialog(Dialog owner, DbxConnection conn, int aseVersionNum)
	{
		AseConfigMonitoringDialog dialog = new AseConfigMonitoringDialog(owner, conn, aseVersionNum, msgDialogTitle);

		if ( ! AseConnectionUtils.isConnectionOk(conn, true, owner) )
			return;

		dialog.setVisible(true);
		dialog.dispose();
	}
//	public static void showDialog(Component owner, Connection conn, int aseVersionNum)
	public static void showDialog(Component owner, DbxConnection conn, int aseVersionNum)
	{
		AseConfigMonitoringDialog dialog = null;
		if (owner instanceof Frame)
			dialog = new AseConfigMonitoringDialog((Frame)owner, conn, aseVersionNum, msgDialogTitle);
		else if (owner instanceof Dialog)
			dialog = new AseConfigMonitoringDialog((Dialog)owner, conn, aseVersionNum, msgDialogTitle);
		else
			dialog = new AseConfigMonitoringDialog((Dialog)null, conn, aseVersionNum, msgDialogTitle);

		if ( ! AseConnectionUtils.isConnectionOk(conn, true, owner) )
			return;

		dialog.setVisible(true);
		dialog.dispose();
	}

//	public AseConfigMonitoringDialog(JDialog dialog, Connection conn)
//	{
//		super(dialog);
//		_tmpConn = conn;
//
////		_mainFrame = frame;
//		init();
//	}
//	public AseConfigMonitoringDialog(JFrame frame, Connection conn)
//	{
//		super(frame);
//		_tmpConn = conn;
//
////		_mainFrame = frame;
//		init();
//	}
//
//	public AseConfigMonitoringDialog(MainFrame frame)
//	{
//		super(frame);
//		_mainFrame = frame;
//		init();
//	}
	/*---------------------------------------------------
	** END: constructors
	**---------------------------------------------------
	*/


	
	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	protected void initComponents()
	{
		JPanel panel    = new JPanel();
		JPanel topPanel = new JPanel();
		panel   .setLayout(new BorderLayout());
		topPanel.setLayout(new MigLayout("wrap 1","grow",""));   // insets Top Left Bottom Right

		setConfigProps();
		_monitoringPanel_1 = createMonitoringPanel();
		_monitoringPanel_2 = createOtherMonitorConfigPanel();
		_monitoringPanel_3 = createOnExitPanel();

		topPanel.add(_monitoringPanel_1, "growx");
		topPanel.add(_monitoringPanel_2, "growx");
		topPanel.add(_monitoringPanel_3, "growx");

		// ADD TOP and OK panel
		panel.add(new JScrollPane(topPanel), BorderLayout.CENTER);
		panel.add(createOkCancelPanel(),     BorderLayout.SOUTH);

		loadProps();

		setContentPane(panel);
	}

	private void setConfigProps()
	{
		_enableMonitoring_chk            .putClientProperty(ASE_CONFIG, "enable monitoring"             );
		_perObjectStatisticsActive_chk   .putClientProperty(ASE_CONFIG, "per object statistics active"  );
		_statementStatisticsActive_chk   .putClientProperty(ASE_CONFIG, "statement statistics active"   );
		_enableSpinlockMonitoring_chk    .putClientProperty(ASE_CONFIG, "enable spinlock monitoring"    );
		_executionTimeMonitoring_chk     .putClientProperty(ASE_CONFIG, "execution time monitoring"     );
		_statementCacheMonitoring_chk    .putClientProperty(ASE_CONFIG, "enable stmt cache monitoring"  );
		_captureCompressionStatistics_chk.putClientProperty(ASE_CONFIG, "capture compression statistics");
		_objectLockwaitTiming_chk        .putClientProperty(ASE_CONFIG, "object lockwait timing"        );
		_processWaitEvents_chk           .putClientProperty(ASE_CONFIG, "process wait events"           );
		_sqlBatchCapture_chk             .putClientProperty(ASE_CONFIG, "SQL batch capture"             );
		_waitEventTiming_chk             .putClientProperty(ASE_CONFIG, "wait event timing"             );

		_lockTimeoutPipeActive_chk       .putClientProperty(ASE_CONFIG, "lock timeout pipe active"      );
		_deadlockPipeActive_chk          .putClientProperty(ASE_CONFIG, "deadlock pipe active"          );
		_errorlogPipeActive_chk          .putClientProperty(ASE_CONFIG, "errorlog pipe active"          );
		_thresholdEventMonitoring_chk    .putClientProperty(ASE_CONFIG, "threshold event monitoring"    );
		_sqlTextPipeActive_chk           .putClientProperty(ASE_CONFIG, "sql text pipe active"          );
		_statementPipeActive_chk         .putClientProperty(ASE_CONFIG, "statement pipe active"         );
		_planTextPipeActive_chk          .putClientProperty(ASE_CONFIG, "plan text pipe active"         );

		_lockTimeoutPipeMaxMessages_sp   .putClientProperty(ASE_CONFIG, "lock timeout pipe max messages");
		_deadlockPipeMaxMessages_sp      .putClientProperty(ASE_CONFIG, "deadlock pipe max messages"    );
		_errorlogPipeMaxMessages_sp      .putClientProperty(ASE_CONFIG, "errorlog pipe max messages"    );
		_thresholdEventMaxMessages_sp    .putClientProperty(ASE_CONFIG, "threshold event max messages"  );
		_sqlTextPipeMaxMessages_sp       .putClientProperty(ASE_CONFIG, "sql text pipe max messages"    );
		_statementPipeMaxMessages_sp     .putClientProperty(ASE_CONFIG, "statement pipe max messages"   );
		_planTextPipeMaxMessages_sp      .putClientProperty(ASE_CONFIG, "plan text pipe max messages"   );
		_maxSqlTextMonitored_sp          .putClientProperty(ASE_CONFIG, "max SQL text monitored"        );
		
		_cfgCapMissingStatistics_chk     .putClientProperty(ASE_CONFIG, "capture missing statistics"    );
		_cfgEnableMetricsCapture_chk     .putClientProperty(ASE_CONFIG, "enable metrics capture"        );
		_cfgMetricsElapMax_sp            .putClientProperty(ASE_CONFIG, "metrics elap max"              );
		_cfgMetricsExecMax_sp            .putClientProperty(ASE_CONFIG, "metrics exec max"              );
		_cfgMetricsLioMax_sp             .putClientProperty(ASE_CONFIG, "metrics lio max"               );
		_cfgMetricsPioMax_sp             .putClientProperty(ASE_CONFIG, "metrics pio max"               );

		_enableFileAccess_chk            .putClientProperty(ASE_CONFIG, "enable file access"            );
	}

	private JPanel createMonitoringPanel()
	{
		JPanel panel = SwingUtils.createPanel("sp_configure 'Monitoring'", true);
		panel.setLayout(new MigLayout("gap 0","[][grow,fill]",""));   // insets Top Left Bottom Right
		
		//--- TOOLTIP 
		// The ToolTip is also used to display configuration problems...
		// So the ToolTipStore is used to reset the original ToolTip when problem is solved.
		_enableMonitoring_chk            .setToolTipText(_tts.add(_enableMonitoring_chk,             "enable monitoring specifies whether the Adaptive Server will collect information for the Monitoring and Diagnostic System."));
		_perObjectStatisticsActive_chk   .setToolTipText(_tts.add(_perObjectStatisticsActive_chk,    "per object statistics active determines whether the Adaptive Server will collect monitoring information on a per object basis."));
		_statementStatisticsActive_chk   .setToolTipText(_tts.add(_statementStatisticsActive_chk,    "statement statistics active indicates whether ASE will collect ad-hoc statement monitoring information."));
		_enableSpinlockMonitoring_chk    .setToolTipText(_tts.add(_enableSpinlockMonitoring_chk,     "<html>Use 'enable spinlock monitoring' to monitor <i>spinlock contention</i>.<br> Or contention on locks that protects ASE shared internal structures, that is accessed from different engines.<br><b>Note:</b> This is available in ASE 15.7.0 ESD#2 and above.</html>"));
		_executionTimeMonitoring_chk     .setToolTipText(_tts.add(_executionTimeMonitoring_chk,      "<html>Use 'execution time monitoring' to monitor <i>System Time Execution</i>.<br> <b>Note:</b> This is available in ASE 15.7 SP100 and above.</html>"));
		_statementCacheMonitoring_chk    .setToolTipText(_tts.add(_statementCacheMonitoring_chk,     "<html>Use 'enable stmt cache monitoring' to configure Adaptive Server to collect the monitoring information on the statement cache.<br><b>Note:</b> This is available in ASE 12.5.2 and above.</html>"));
		_captureCompressionStatistics_chk.setToolTipText(_tts.add(_captureCompressionStatistics_chk, "<html>Use 'capture compression statistics' to configure Adaptive Server to collect the monitoring information on the data compression.<br><b>Note:</b> This is available in ASE 15.7 and above.</html>"));
		_objectLockwaitTiming_chk        .setToolTipText(_tts.add(_objectLockwaitTiming_chk,         "object lockwait timing specifies whether the Adaptive Server will collect timing data on lock requests."));
		_processWaitEvents_chk           .setToolTipText(_tts.add(_processWaitEvents_chk,            "process event timing specifies whether the Adaptive Server will collect monitoring data on wait events for individual processes."));
		_sqlBatchCapture_chk             .setToolTipText(_tts.add(_sqlBatchCapture_chk,              "SQL batch capture indicates whether the Adaptive Server will collect sql batch text for each process."));
		_waitEventTiming_chk             .setToolTipText(_tts.add(_waitEventTiming_chk,              "wait event timing specifies whether the Adaptive Server will collect monitoring data for all wait events."));

		_lockTimeoutPipeActive_chk       .setToolTipText(_tts.add(_lockTimeoutPipeActive_chk,        "<html>lock timeout pipe active controls whether Adaptive Server collects lock timeout messages.<br> If lock timeout pipe active and lock timeout pipe max messages are enabled, Adaptive Server collects the text for each lock timeout.<br> Retrieve the lock timeout messages from the monLockTimeout monitor table.<br><b>Note:</b> This is available in ASE 15.7 and above.</html>"));
		_lockTimeoutPipeMaxMessages_sp   .setToolTipText(_tts.add(_lockTimeoutPipeMaxMessages_sp,    "<html>lock timeout pipe max messages determines the number of lock timeout messages Adaptive Server stores, and the amount of memory it allocates for the task.<br><b>Note:</b> This is available in ASE 15.7 and above.</html>"));

		_deadlockPipeActive_chk          .setToolTipText(_tts.add(_deadlockPipeActive_chk,           "deadlock pipe active indicates whether the Adaptive Server will collect historical deadlock monitoring information."));
		_deadlockPipeMaxMessages_sp      .setToolTipText(_tts.add(_deadlockPipeMaxMessages_sp,       "deadlock pipe max messages specifies the maximum number of messages that can be stored for historical deadlock data."));

		_errorlogPipeActive_chk          .setToolTipText(_tts.add(_errorlogPipeActive_chk,           "errorlog pipe active indicates whether the Adaptive Server will collect historical errorlog monitoring information."));
		_errorlogPipeMaxMessages_sp      .setToolTipText(_tts.add(_errorlogPipeMaxMessages_sp,       "errorlog pipe max messages specifies the maximum number of messages that can be stored for historical errorlog text."));

		_thresholdEventMonitoring_chk    .setToolTipText(_tts.add(_thresholdEventMonitoring_chk,     "<html>If we should collect events generated by the Resource Governor, when the Governor kicks in and does restrictions. <br> <br><b>Note:</b> This is available in ASE 16.0 and above.</html>"));
		_thresholdEventMaxMessages_sp    .setToolTipText(_tts.add(_thresholdEventMaxMessages_sp,     "<html>How many events that can be stored in table 'monThresholdEvent'<br> <br><b>Note:</b> This is available in ASE 16.0 and above.</html>"));

		_sqlTextPipeActive_chk           .setToolTipText(_tts.add(_sqlTextPipeActive_chk,            "<html>sql text pipe active indicates whether the Adaptive Server will collect historical sql batch text information.<br>  <b>Note</b>: This may degrade ASE performance up to 5%</html>"));
		_sqlTextPipeMaxMessages_sp       .setToolTipText(_tts.add(_sqlTextPipeMaxMessages_sp,        "<html>sql text pipe max messages specifies the maximum number of messages that can be stored for historical sql text.<br> <b>Note</b>: This may degrade ASE performance up to 5%</html>"));

		_statementPipeActive_chk         .setToolTipText(_tts.add(_statementPipeActive_chk,          "<html>statement pipe active indicates whether the Adaptive Server will collect historical statement level monitoring information.<br> <b>Note</b>: This may degrade ASE performance up to 10%</html>"));
		_statementPipeMaxMessages_sp     .setToolTipText(_tts.add(_statementPipeMaxMessages_sp,      "<html>statement pipe max messages specifies the maximum number of messages that can be stored for historical statement text.<br>      <b>Note</b>: This may degrade ASE performance up to 10%</html>"));

		_planTextPipeActive_chk          .setToolTipText(_tts.add(_planTextPipeActive_chk,           "<html>plan text pipe active indicates whether the Adaptive Server will collect historical plan text monitoring information.<br> <b>Note</b>: This may degrade ASE performance up to 25%</html>"));
		_planTextPipeMaxMessages_sp      .setToolTipText(_tts.add(_planTextPipeMaxMessages_sp,       "<html>plan text pipe max messages specifies the maximum number of messages that can be stored for historical plan text.<br>     <b>Note</b>: This may degrade ASE performance up to 25%</html>"));

		_maxSqlTextMonitored_lbl         .setToolTipText(_tts.add(_maxSqlTextMonitored_lbl,          "Restart required: specifies the amount of memory allocated per user connection for saving SQL text to memory shared by Adaptive Server. The default value is 0."));
		_maxSqlTextMonitored_sp          .setToolTipText(_tts.add(_maxSqlTextMonitored_sp,           "Restart required: specifies the amount of memory allocated per user connection for saving SQL text to memory shared by Adaptive Server. The default value is 0."));

		_predefinedConfigs_lbl           .setToolTipText(_tts.add(_predefinedConfigs_lbl,            "A pre defined value set of the above configurations."));
		_predefinedConfigs_cbx           .setToolTipText(_tts.add(_predefinedConfigs_cbx,            "A pre defined value set of the above configurations."));

		_configurabelMemory_lbl          .setToolTipText(_tts.add(_configurabelMemory_lbl,           "Available memory to be used for additional configurations or 'data caches' etc. This memory is basically not used by the ASE, meaning it's \"waste\" and ready for usage by someone..."));

		_enableFileAccess_chk            .setToolTipText(_tts.add(_enableFileAccess_chk,              
				"<html>Let ASE access Operating System files.<br>"
                + "With this enabled, the Configuration dialog can read the ASE Configuration file.<br>"
                + "<code>sp_configure 'enable file access', 1</code><br>"
                + "<br>"
                + "<b>Note:</b> in ASE 12.5.x this needs a license... So if the license is not enabled, this option wont be enabled as well."
                + "</html>"));

		//--- LAYOUT
		panel.add(_predefinedConfigs_lbl,            "");
		panel.add(_predefinedConfigs_cbx,            "right, pushx, wrap 10");

		panel.add(new JSeparator(),                  "span, grow, push, wrap 10");

		panel.add(_enableMonitoring_chk,             "wrap 15");

		panel.add(_perObjectStatisticsActive_chk,    "wrap");
		panel.add(_statementStatisticsActive_chk,    "wrap");
		panel.add(_enableSpinlockMonitoring_chk,     "wrap");
		panel.add(_executionTimeMonitoring_chk,      "wrap");
		panel.add(_statementCacheMonitoring_chk,     "wrap");
		panel.add(_captureCompressionStatistics_chk, "wrap");
		panel.add(_objectLockwaitTiming_chk,         "wrap");
		panel.add(_processWaitEvents_chk,            "wrap");
		panel.add(_sqlBatchCapture_chk,              "wrap");
		panel.add(_waitEventTiming_chk,              "wrap 15");

		panel.add(_lockTimeoutPipeActive_chk,        "");
		panel.add(_lockTimeoutPipeMaxMessages_sp,    "right, pushx, wrap");

		panel.add(_deadlockPipeActive_chk,           "");
		panel.add(_deadlockPipeMaxMessages_sp,       "right, pushx, wrap");

		panel.add(_errorlogPipeActive_chk,           "");
		panel.add(_errorlogPipeMaxMessages_sp,       "right, pushx, wrap");

		panel.add(_thresholdEventMonitoring_chk,     "");
		panel.add(_thresholdEventMaxMessages_sp,     "right, pushx, wrap 10");

		panel.add(_sqlTextPipeActive_chk,            "");
		panel.add(_sqlTextPipeMaxMessages_sp,        "right, pushx, wrap");

		panel.add(_statementPipeActive_chk,          "");
		panel.add(_statementPipeMaxMessages_sp,      "right, pushx, wrap");

		panel.add(_planTextPipeActive_chk,           "");
		panel.add(_planTextPipeMaxMessages_sp,       "right, pushx, wrap 15");

		panel.add(_maxSqlTextMonitored_lbl,          "");
		panel.add(_maxSqlTextMonitored_sp,           "right, pushx, wrap 10");

		panel.add(new JSeparator(),                  "span, grow, push, wrap 10");

		panel.add(_configurabelMemory_lbl,           "span, wrap");

		//--- ACTIONS
		_predefinedConfigs_cbx.addActionListener(this);

		return panel;
	}

	private JPanel createOtherMonitorConfigPanel()
	{
		JPanel panel = SwingUtils.createPanel("Other Monitor Configuration", true);
		panel.setLayout(new MigLayout("gap 0","[][grow,fill]",""));   // insets Top Left Bottom Right

		String toolTipText;

		//--- TOOLTIP 
		// The ToolTip is also used to display configuration problems...
		// So the ToolTipStore is used to reset the original ToolTip when problem is solved.

		// TOOLTIP: _cfgCapMissingStatistics_chk
		toolTipText	= "<html>" +
			"Missing Statistics are captured in the system catalogs, and can be view from <i>dbname</i>..sysstatistics <br>" +
			"<br>" +
			"This metrics is used in the Performance Counter Tab 'Missing Statistics'.<br>" +
			"<b>Note:</b> This is available in ASE 15.0.3 ESD#1 and above." +
			"</html>";
		_cfgCapMissingStatistics_chk   .setToolTipText(_tts.add(_cfgCapMissingStatistics_chk, toolTipText));

		// TOOLTIP: _cfgEnableMetricsCapture
		toolTipText	= "<html>" +
			"Enables Adaptive Server to capture metrics at the server level. <br>" +
			"Metrics for ad hoc statements are captured in the system catalogs, and can be view from <i>dbname</i>..sysquerymetrics <br>" +
			"metrics for statements in a stored procedure are saved in the procedure cache.<br>" +
			"<br>" +
			"This metrics is used in the Performance Counter Tab 'QP Metrics'.<br>" +
			"<b>Note:</b> This is available in ASE 15.0.2 and above." +
			"</html>";
		_cfgEnableMetricsCapture_chk.setToolTipText(_tts.add(_cfgEnableMetricsCapture_chk, toolTipText));

		// TOOLTIP: _cfgMetricsElapMax_*
		toolTipText	= "<html>" +
			"If the elapsed time of the query is less than the value of this configuration parameter, then the query metrics associated <br>with this query are <b>not</b> written to the system tables, avoiding excessive catalog writes for simple queries.<br>" +
			"<br>" +
			"Unit: milliseconds<br>" +
			"Note: <code><b>metrics elap max</b></code> has an effect only when <code><b>enable metrics capture</b></code> is on. <br>" +
			"Note: This is available in ASE 15.0.2 and above." +
			"</html>";
		_cfgMetricsElapMax_lbl.setToolTipText(_tts.add(_cfgMetricsElapMax_lbl, toolTipText));
		_cfgMetricsElapMax_sp .setToolTipText(_tts.add(_cfgMetricsElapMax_sp,  toolTipText));

		// TOOLTIP: _cfgMetricsExecMax_*
		toolTipText = "<html>" +
			"If the execution time of the query is less than the value of this configuration parameter, then the query metrics associated <br>with this query are <b>not</b> written to the system tables, avoiding excessive catalog writes for simple queries.<br>" +
			"<br>" +
			"Unit: milliseconds<br>" +
			"Note: <code><b>metrics exec max</b></code> has an effect only when <code><b>enable metrics capture</b></code> is on. <br>" +
			"Note: This is available in ASE 15.0.2 and above." +
			"</html>";
		_cfgMetricsExecMax_lbl.setToolTipText(_tts.add(_cfgMetricsExecMax_lbl, toolTipText));
		_cfgMetricsExecMax_sp .setToolTipText(_tts.add(_cfgMetricsExecMax_sp,  toolTipText));

		// TOOLTIP: _cfgMetricsLioMax_*
		toolTipText = "<html>" +
			"If the logical IO time of the query is less than the value of this configuration parameter, then the query metrics associated <br>with this query are <b>not</b> written to the system tables, avoiding excessive catalog writes for simple queries.<br>" +
			"<br>" +
			"Unit: logical pages<br>" +
			"Note: <code><b>metrics lio max</b></code> has an effect only when <code><b>enable metrics capture</b></code> is on. <br>" +
			"Note: This is available in ASE 15.0.2 and above." +
			"</html>";
		_cfgMetricsLioMax_lbl .setToolTipText(_tts.add(_cfgMetricsLioMax_lbl, toolTipText));
		_cfgMetricsLioMax_sp  .setToolTipText(_tts.add(_cfgMetricsLioMax_sp,  toolTipText));

		// TOOLTIP: _cfgMetricsPioMax_*
		toolTipText = "<html>" +
			"If the physical IO time of the query is less than the value of this configuration parameter, then the query metrics associated <br>with this query are <b>not</b> written to the system tables, avoiding excessive catalog writes for simple queries.<br>" +
			"<br>" +
			"Unit: logical pages<br>" +
			"Note: <code><b>metrics pio max</b></code> has an effect only when <code><b>enable metrics capture</b></code> is on. <br>" +
			"Note: This is available in ASE 15.0.2 and above." +
			"</html>";
		_cfgMetricsPioMax_lbl .setToolTipText(_tts.add(_cfgMetricsPioMax_lbl, toolTipText));
		_cfgMetricsPioMax_sp  .setToolTipText(_tts.add(_cfgMetricsPioMax_sp,  toolTipText));

		//--- LAYOUT
		panel.add(_cfgCapMissingStatistics_chk,   "wrap");

		panel.add(_cfgEnableMetricsCapture_chk,   "wrap");

		panel.add(_cfgMetricsElapMax_lbl,         "gapleft 50");
		panel.add(_cfgMetricsElapMax_sp,          "right, pushx, wrap");
		
		panel.add(_cfgMetricsExecMax_lbl,         "gapleft 50");
		panel.add(_cfgMetricsExecMax_sp,          "right, pushx, wrap");
		
		panel.add(_cfgMetricsLioMax_lbl,          "gapleft 50");
		panel.add(_cfgMetricsLioMax_sp,           "right, pushx, wrap");
		
		panel.add(_cfgMetricsPioMax_lbl,          "gapleft 50");
		panel.add(_cfgMetricsPioMax_sp,           "right, pushx, wrap 10");

		panel.add(_enableFileAccess_chk,          "wrap");

		return panel;
	}

	private JPanel createOnExitPanel()
	{
		JPanel panel = SwingUtils.createPanel("Disable Monitoring On Disconnect Or Exit", true);
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

		//--- TOOLTIP
		_onExitDoNotDisable_rb.setToolTipText("Monitoring is not disabled when "+Version.getAppName()+" is terminated.");
		_onExitAutoDisable_rb .setToolTipText("If no other "+Version.getAppName()+" is running at the same time, the monitoring will be disabled and on next login it will be enabled again.");
		_onExitAsk_rb         .setToolTipText("A popup will ask you if you want to disable Monitoring everytime the '"+Version.getAppName()+"' is terminated.");

		ButtonGroup group = new ButtonGroup();
		group.add(_onExitDoNotDisable_rb);
		group.add(_onExitAutoDisable_rb);
		group.add(_onExitAsk_rb);

		//--- LAYOUT
		panel.add(_onExitDoNotDisable_rb, "");
		panel.add(_onExitAutoDisable_rb,  "");
		panel.add(_onExitAsk_rb,          "");

		//--- ACTIONS
		// no actions

		return panel;
	}

	private JPanel createOkCancelPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

		// ADD the OK, Cancel, Apply buttons
		panel.add(new JLabel(), "pushx, growx");
		panel.add(_ok,          "tag ok, right");
		panel.add(_cancel,      "tag cancel");
		panel.add(_apply,       "tag apply");

		_apply.setEnabled(false);

		// ADD ACTIONS TO COMPONENTS
		_ok           .addActionListener(this);
		_cancel       .addActionListener(this);
		_apply        .addActionListener(this);

		return panel;
	}
	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/


	
	private void disableAllInput()
	{
		SwingUtils.setEnabled(_monitoringPanel_1, false);
		SwingUtils.setEnabled(_monitoringPanel_2, false);
		SwingUtils.setEnabled(_monitoringPanel_3, false);
		_apply.setEnabled(false);
		_ok.setEnabled(false);
	}

	/*---------------------------------------------------
	** BEGIN: override methods
	**---------------------------------------------------
	*/
	@Override
	public void setVisible(boolean visible)
	{
		_logger.debug("AseConfigMonitoringDialog.setVisible("+visible+")");

//		if ( ! AseConnectionUtils.hasRole(_conn, AseConnectionUtils.SA_ROLE) )
//		{
//			String msg = "Sorry, you need 'sa_role' to configure monitoring parameters. Ask you'r System Administrator to configure this.";
//			SwingUtils.showErrorMessage(this, "Not authorized to do sp_configure", msg, null);
//			
//			return;
//		}

		_configErrors = false;
		if ( visible )
		{
			if ( ! AseConnectionUtils.isConnectionOk(_conn, true, this) )
				return;

			// Try to get a new version number if it doesn't exist...
			if (_aseVersionNum <= 0)
			{
				_aseVersionNum = AseConnectionUtils.getAseVersionNumber(_conn);
				_logger.debug("setVisible("+visible+") Need to refresh the ASE Server version number, it is now '"+_aseVersionNum+"'.");

				_isClusterEnabled = AseConnectionUtils.isClusterEnabled(_conn);
			}

			_predefinedConfigs_cbx.setSelectedIndex(PDC_FROM_ASE);
			setVisibleForJPanel(_monitoringPanel_1, visible);
			setVisibleForJPanel(_monitoringPanel_2, visible);
		}
		else
		{
			_predefinedConfigs_cbx.setSelectedIndex(PDC_NONE);
			setVisibleForJPanel(_monitoringPanel_1, visible);
			setVisibleForJPanel(_monitoringPanel_2, visible);
		}

		if (visible)
			loadProps();

		// IF NOT SA, disable input...
		if ( visible && ! AseConnectionUtils.hasRole(_conn, AseConnectionUtils.SA_ROLE) )
		{
			String msg = "<html>" +
				"Sorry, you need 'sa_role' to <b>change</b> monitoring parameters.<br>" +
				"Ask you'r System Administrator to configure this.<br>" +
				"<br>" +
				"At this moment you can only <b>view</b> how monitoring is configured" +
				"</html>";
			SwingUtils.showErrorMessage(this, "Not authorized to do sp_configure", msg, null);
			disableAllInput();
		}

		super.setVisible(visible);
	}
	/*---------------------------------------------------
	** END: override methods
	**---------------------------------------------------
	*/
	/** Helper method to setVisible() */
	private void setVisibleForJPanel(JPanel jpanel, boolean visible)
	{
		if (jpanel == null)
			throw new RuntimeException("The passed JPanel can't be null");

		if ( visible )
		{
			// Set the panel to visibility
			jpanel.setEnabled(true);

			// Check each component inside the JPanel
			for (int i=0; i<jpanel.getComponentCount(); i++)
			{
				Component comp = jpanel.getComponent(i);

				// Simplifies things if we always set this to FALSE first
				// and then set the various components to true.
				comp.setEnabled(false);

				// Special components that has ASE version dependencies
				if (    comp.equals(_cfgEnableMetricsCapture_chk) 
				     || comp.equals(_cfgMetricsElapMax_lbl)
				     || comp.equals(_cfgMetricsElapMax_sp)
				     || comp.equals(_cfgMetricsExecMax_lbl)
				     || comp.equals(_cfgMetricsExecMax_sp)
				     || comp.equals(_cfgMetricsLioMax_lbl)
				     || comp.equals(_cfgMetricsLioMax_sp)
				     || comp.equals(_cfgMetricsPioMax_lbl)
				     || comp.equals(_cfgMetricsPioMax_sp)
				   )
				{
//					if (_aseVersionNum >= 15020)
//					if (_aseVersionNum >= 1502000)
					if (_aseVersionNum >= Ver.ver(15,0,2))
						comp.setEnabled(true);
				}
				else if ( comp.equals(_enableSpinlockMonitoring_chk) )
				{
//					if (_aseVersionNum >= 15702)
//					if (_aseVersionNum >= 1570020)
					if (_aseVersionNum >= Ver.ver(15,7,0,2))
						comp.setEnabled(true);
				}
				else if ( comp.equals(_executionTimeMonitoring_chk) )
				{
//					if (_aseVersionNum >= 1570100)
					if (_aseVersionNum >= Ver.ver(15,7,0,100))
						comp.setEnabled(true);
				}
				else if ( comp.equals(_statementCacheMonitoring_chk) )
				{
//					if (_aseVersionNum >= 15020)
//					if (_aseVersionNum >= 1502000)
					if (_aseVersionNum >= Ver.ver(15,0,2))
						comp.setEnabled(true);
				}
				else if ( comp.equals(_captureCompressionStatistics_chk) )
				{
//					if (_aseVersionNum >= 15700)
//					if (_aseVersionNum >= 1570000)
					if (_aseVersionNum >= Ver.ver(15,7))
						comp.setEnabled(true);
				}
				else if ( comp.equals(_cfgCapMissingStatistics_chk) )
				{
//					if (_aseVersionNum >= 15031)
//					if (_aseVersionNum >= 1503010)
					if (_aseVersionNum >= Ver.ver(15,0,3,1))
						comp.setEnabled(true);
				}
				else if ( comp.equals(_lockTimeoutPipeActive_chk) )
				{
//					if (_aseVersionNum >= 15700)
//					if (_aseVersionNum >= 1570000)
					if (_aseVersionNum >= Ver.ver(15,7))
						comp.setEnabled(true);
				}
				else if ( comp.equals(_lockTimeoutPipeMaxMessages_sp) )
				{
//					if (_aseVersionNum >= 15700)
//					if (_aseVersionNum >= 1570000)
					if (_aseVersionNum >= Ver.ver(15,7))
						comp.setEnabled(true);
				}
				else if ( comp.equals(_thresholdEventMonitoring_chk) )
				{
//					if (_aseVersionNum >= 1600000)
					if (_aseVersionNum >= Ver.ver(16,0))
						comp.setEnabled(true);
				}
				else if ( comp.equals(_thresholdEventMaxMessages_sp) )
				{
//					if (_aseVersionNum >= 1600000)
					if (_aseVersionNum >= Ver.ver(16,0))
						comp.setEnabled(true);
				}
				else if ( comp.equals(_enableFileAccess_chk) )
				{
					comp.setEnabled(_isXfsLicenseEnabled);
				}
				else // All other components
				{
					comp.setEnabled(true);
				}
			}
		}
		else
		{
			// Set the panel to visibility
			jpanel.setEnabled(false);

			// Check each component inside the JPanel
			for (int i=0; i<jpanel.getComponentCount(); i++)
			{
				jpanel.getComponent(i).setEnabled(false);
			}
		}
	}
	
	
	
	/*---------------------------------------------------
	** BEGIN: implementing ActionListener, KeyListeners
	**---------------------------------------------------
	*/
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

		// --- CHECKBOX: SERVERS ---
		if (_predefinedConfigs_cbx.equals(source))
		{
			String str = _predefinedConfigs_cbx.getSelectedItem().toString();
			performPredefinedConfigsAction(str);
		}

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			setVisible(false);
		}

		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			storeInAse(_conn);
			if ( ! _configErrors )
			{
				setVisible(false);
			}
			saveProps();
		}

		// --- BUTTON: APPLY ---
		if (_apply.equals(source))
		{
			storeInAse(_conn);
			saveProps();
		}
	}
	/*---------------------------------------------------
	** END: implementing ActionListener, KeyListeners
	**---------------------------------------------------
	*/



	
	
	/*---------------------------------------------------
	** BEGIN: helper methods
	**---------------------------------------------------
	*/
	public void setAseVersion(int versionNumber)
	{
		_aseVersionNum = versionNumber;
	}
	public int getAseVersion()
	{
		return _aseVersionNum;
	}

	/**
	 * Set focus to a good field or button
	 */
	private void setFocus()
	{
		// The components needs to be visible for the requestFocus()
		// to work, so lets the EventThreda do it for us after the windows is visible.
		Runnable deferredAction = new Runnable()
		{
			@Override
			public void run()
			{
				_ok.requestFocus();
			}
		};
		SwingUtilities.invokeLater(deferredAction);
	}


	private void performPredefinedConfigsAction(String str) 
	{
		if ( str == null || (str != null && str.trim().equals("")) )
		{
			_logger.debug("performPredefinedConfigsAction() has empty or null string as input.");
			return;
		}
		if ( str.equals(PDC_OPTIONS_STR[PDC_FROM_ASE]) )
		{
			loadFromAse(_conn);
			if (_conn == null)
			{
				str = PDC_OPTIONS_STR[PDC_NONE];
			}
		}
		if ( str.equals(PDC_OPTIONS_STR[PDC_NONE]) ) // NONE
		{
			_enableMonitoring_chk            .setSelected(false);

			_perObjectStatisticsActive_chk   .setSelected(false);
			_statementStatisticsActive_chk   .setSelected(false);
			_enableSpinlockMonitoring_chk    .setSelected(false);
			_executionTimeMonitoring_chk     .setSelected(false);
			_statementCacheMonitoring_chk    .setSelected(false);
			_captureCompressionStatistics_chk.setSelected(false);
			_objectLockwaitTiming_chk        .setSelected(false);
			_processWaitEvents_chk           .setSelected(false);
			_sqlBatchCapture_chk             .setSelected(false);
			_waitEventTiming_chk             .setSelected(false);

			_lockTimeoutPipeActive_chk       .setSelected(false);
			_deadlockPipeActive_chk          .setSelected(false);
			_errorlogPipeActive_chk          .setSelected(false);
			_thresholdEventMonitoring_chk    .setSelected(false);
			_sqlTextPipeActive_chk           .setSelected(false);
			_planTextPipeActive_chk          .setSelected(false);
			_statementPipeActive_chk         .setSelected(false);

			_lockTimeoutPipeMaxMessages_spm  .setValue( new Integer(0) );
			_deadlockPipeMaxMessages_spm     .setValue( new Integer(0) );
			_errorlogPipeMaxMessages_spm     .setValue( new Integer(0) );
			_thresholdEventMaxMessages_spm   .setValue( new Integer(0) );

			_sqlTextPipeMaxMessages_spm      .setValue( new Integer(0) );
			_statementPipeMaxMessages_spm    .setValue( new Integer(0) );
			_planTextPipeMaxMessages_spm     .setValue( new Integer(0) );

			_maxSqlTextMonitored_spm         .setValue( new Integer(0) );
			
			_enableFileAccess_chk            .setSelected(false);

//			_cfgCapMissingStatistics_chk     .setSelected(false);
//			_cfgEnableMetricsCapture_chk     .setSelected(false);
//			_cfgMetricsElapMax_spm           .setValue( new Integer(0) );
//			_cfgMetricsExecMax_spm           .setValue( new Integer(0) );
//			_cfgMetricsLioMax_spm            .setValue( new Integer(0) );
//			_cfgMetricsPioMax_spm            .setValue( new Integer(0) );
		}
		if ( str.equals(PDC_OPTIONS_STR[PDC_MINIMAL]) ) // MINIMAL
		{
			_enableMonitoring_chk            .setSelected(true);

			_perObjectStatisticsActive_chk   .setSelected(true);
			_statementStatisticsActive_chk   .setSelected(false);
			_enableSpinlockMonitoring_chk    .setSelected(false);
			_executionTimeMonitoring_chk     .setSelected(false);
			_statementCacheMonitoring_chk    .setSelected(false);
			_captureCompressionStatistics_chk.setSelected(false);
			_objectLockwaitTiming_chk        .setSelected(false);
			_processWaitEvents_chk           .setSelected(false);
			_sqlBatchCapture_chk             .setSelected(false);
			_waitEventTiming_chk             .setSelected(true);

			_lockTimeoutPipeActive_chk       .setSelected(false);
			_deadlockPipeActive_chk          .setSelected(false);
			_errorlogPipeActive_chk          .setSelected(false);
			_thresholdEventMonitoring_chk    .setSelected(false);
			_sqlTextPipeActive_chk           .setSelected(false);
			_statementPipeActive_chk         .setSelected(false);
			_planTextPipeActive_chk          .setSelected(false);

			_lockTimeoutPipeMaxMessages_spm  .setValue( new Integer(0) );
			_deadlockPipeMaxMessages_spm     .setValue( new Integer(0) );
			_errorlogPipeMaxMessages_spm     .setValue( new Integer(0) );
			_thresholdEventMaxMessages_spm   .setValue( new Integer(0) );

			_sqlTextPipeMaxMessages_spm      .setValue( new Integer(0) );
			_statementPipeMaxMessages_spm    .setValue( new Integer(0) );
			_planTextPipeMaxMessages_spm     .setValue( new Integer(0) );

			_maxSqlTextMonitored_spm         .setValue( new Integer(0) );

			_enableFileAccess_chk            .setSelected(true);

//			_cfgCapMissingStatistics_chk     .setSelected(false);
//			_cfgEnableMetricsCapture_chk     .setSelected(false);
//			_cfgMetricsElapMax_spm           .setValue( new Integer(0) );
//			_cfgMetricsExecMax_spm           .setValue( new Integer(0) );
//			_cfgMetricsLioMax_spm            .setValue( new Integer(0) );
//			_cfgMetricsPioMax_spm            .setValue( new Integer(0) );
		}
		if ( str.equals(PDC_OPTIONS_STR[PDC_SMALL]) ) // SMALL
		{
			_enableMonitoring_chk            .setSelected(true);

			_perObjectStatisticsActive_chk   .setSelected(true);
			_statementStatisticsActive_chk   .setSelected(true);
			_enableSpinlockMonitoring_chk    .setSelected(true);
			_executionTimeMonitoring_chk     .setSelected(true);
			_statementCacheMonitoring_chk    .setSelected(true);
			_captureCompressionStatistics_chk.setSelected(false);
			_objectLockwaitTiming_chk        .setSelected(true);
			_processWaitEvents_chk           .setSelected(true);
			_sqlBatchCapture_chk             .setSelected(false);
			_waitEventTiming_chk             .setSelected(true);

			_lockTimeoutPipeActive_chk       .setSelected(false);
			_deadlockPipeActive_chk          .setSelected(false);
			_errorlogPipeActive_chk          .setSelected(false);
			_thresholdEventMonitoring_chk    .setSelected(false);
			_sqlTextPipeActive_chk           .setSelected(false);
			_statementPipeActive_chk         .setSelected(false);
			_planTextPipeActive_chk          .setSelected(false);

			_lockTimeoutPipeMaxMessages_spm  .setValue( new Integer(0) );
			_deadlockPipeMaxMessages_spm     .setValue( new Integer(0) );
			_errorlogPipeMaxMessages_spm     .setValue( new Integer(0) );
			_thresholdEventMaxMessages_spm   .setValue( new Integer(0) );

			_sqlTextPipeMaxMessages_spm      .setValue( new Integer(0) );
			_statementPipeMaxMessages_spm    .setValue( new Integer(0) );
			_planTextPipeMaxMessages_spm     .setValue( new Integer(0) );

			_maxSqlTextMonitored_spm         .setValue( new Integer(0) );

			_enableFileAccess_chk            .setSelected(true);

//			_cfgCapMissingStatistics_chk     .setSelected(false);
//			_cfgEnableMetricsCapture_chk     .setSelected(false);
//			_cfgMetricsElapMax_spm           .setValue( new Integer(0) );
//			_cfgMetricsExecMax_spm           .setValue( new Integer(0) );
//			_cfgMetricsLioMax_spm            .setValue( new Integer(0) );
//			_cfgMetricsPioMax_spm            .setValue( new Integer(0) );
		}
		if ( str.equals(PDC_OPTIONS_STR[PDC_MEDIUM]) ) // MEDIUM
		{
			_enableMonitoring_chk            .setSelected(true);

			_perObjectStatisticsActive_chk   .setSelected(true);
			_statementStatisticsActive_chk   .setSelected(true);
			_enableSpinlockMonitoring_chk    .setSelected(true);
			_executionTimeMonitoring_chk     .setSelected(true);
			_statementCacheMonitoring_chk    .setSelected(true);
			_captureCompressionStatistics_chk.setSelected(false);
			_objectLockwaitTiming_chk        .setSelected(true);
			_processWaitEvents_chk           .setSelected(true);
			_sqlBatchCapture_chk             .setSelected(true);
			_waitEventTiming_chk             .setSelected(true);

			_lockTimeoutPipeActive_chk       .setSelected(true);
			_deadlockPipeActive_chk          .setSelected(true);
			_errorlogPipeActive_chk          .setSelected(true);
			_thresholdEventMonitoring_chk    .setSelected(true);
			_sqlTextPipeActive_chk           .setSelected(true);
			_statementPipeActive_chk         .setSelected(true);
			_planTextPipeActive_chk          .setSelected(false);

			_lockTimeoutPipeMaxMessages_spm  .setValue( new Integer(500) );
			_deadlockPipeMaxMessages_spm     .setValue( new Integer(500) );
			_errorlogPipeMaxMessages_spm     .setValue( new Integer(200) );
			_thresholdEventMaxMessages_spm   .setValue( new Integer(500) );

			_sqlTextPipeMaxMessages_spm      .setValue( new Integer(1000) );
			_statementPipeMaxMessages_spm    .setValue( new Integer(5000) );
			_planTextPipeMaxMessages_spm     .setValue( new Integer(0) );

			_maxSqlTextMonitored_spm         .setValue( new Integer(2048) );

			_enableFileAccess_chk            .setSelected(true);

//			_cfgCapMissingStatistics_chk     .setSelected(false);
//			_cfgEnableMetricsCapture_chk     .setSelected(false);
//			_cfgMetricsElapMax_spm           .setValue( new Integer(0) );
//			_cfgMetricsExecMax_spm           .setValue( new Integer(0) );
//			_cfgMetricsLioMax_spm            .setValue( new Integer(0) );
//			_cfgMetricsPioMax_spm            .setValue( new Integer(0) );
		}
		if ( str.equals(PDC_OPTIONS_STR[PDC_LARGE]) ) // LARGE
		{
			_enableMonitoring_chk            .setSelected(true);

			_perObjectStatisticsActive_chk   .setSelected(true);
			_statementStatisticsActive_chk   .setSelected(true);
			_enableSpinlockMonitoring_chk    .setSelected(true);
			_executionTimeMonitoring_chk     .setSelected(true);
			_statementCacheMonitoring_chk    .setSelected(true);
			_captureCompressionStatistics_chk.setSelected(true);
			_objectLockwaitTiming_chk        .setSelected(true);
			_processWaitEvents_chk           .setSelected(true);
			_sqlBatchCapture_chk             .setSelected(true);
			_waitEventTiming_chk             .setSelected(true);

			_lockTimeoutPipeActive_chk       .setSelected(true);
			_deadlockPipeActive_chk          .setSelected(true);
			_errorlogPipeActive_chk          .setSelected(true);
			_thresholdEventMonitoring_chk    .setSelected(true);
			_sqlTextPipeActive_chk           .setSelected(true);
			_statementPipeActive_chk         .setSelected(true);
			_planTextPipeActive_chk          .setSelected(false);

			_lockTimeoutPipeMaxMessages_spm  .setValue( new Integer(1000) );
			_deadlockPipeMaxMessages_spm     .setValue( new Integer(1000) );
			_errorlogPipeMaxMessages_spm     .setValue( new Integer(1000) );
			_thresholdEventMaxMessages_spm   .setValue( new Integer(1000) );

			_sqlTextPipeMaxMessages_spm      .setValue( new Integer(5000) );
			_statementPipeMaxMessages_spm    .setValue( new Integer(50000) );
			_planTextPipeMaxMessages_spm     .setValue( new Integer(0) );

			_maxSqlTextMonitored_spm         .setValue( new Integer(4096) );

			_enableFileAccess_chk            .setSelected(true);

//			_cfgCapMissingStatistics_chk     .setSelected(false);
//			_cfgEnableMetricsCapture_chk     .setSelected(false);
//			_cfgMetricsElapMax_spm           .setValue( new Integer(0) );
//			_cfgMetricsExecMax_spm           .setValue( new Integer(0) );
//			_cfgMetricsLioMax_spm            .setValue( new Integer(0) );
//			_cfgMetricsPioMax_spm            .setValue( new Integer(0) );
		}
	}

	/**
	 * 	1> select SNAP_VERSION=@@version
	 * 	2> go
	 *   SNAP_VERSION
	 * 	 -----------------------------------------------------------------------------------------------------------------
	 * 	 Adaptive Server Enterprise/12.5.4/EBF 13387/P/NT (IX86)/OS 4.0/ase1254/2006/32-bit/OPT/Sat May 20 00:54:28 2006
	 * 
	 *  - And the same config values for the following version(s)
	 *    Adaptive Server Enterprise/15.0.1/EBF 13823/P/NT (IX86)/Windows 2000/ase1501/2379/32-bit/OPT/Mon Aug 14 22:12:39 2006
	 * 
	 * 	1> sp_configure "Monitoring"
	 * 	2> go
	 * 
	 * 	Group: Monitoring
	 * 
	 * 	 Parameter Name                 Default     Memory Used Config Value Run Value   Unit                 Type
	 * 	 ------------------------------ ----------- ----------- ------------ ----------- -------------------- ----------
	 * 	 SQL batch capture                        0           0           1            1 switch               dynamic
	 * 	 deadlock pipe active                     0           0           1            1 switch               dynamic
	 * 	 deadlock pipe max messages               0          89         200          200 number               dynamic
	 * 	 enable monitoring                        0           0           1            1 switch               dynamic
	 * 	 errorlog pipe active                     0           0           1            1 switch               dynamic
	 * 	 errorlog pipe max messages               0         266         500          500 number               dynamic
	 * 	 max SQL text monitored                   0         206        2048         2048 bytes                static
	 * 	 object lockwait timing                   0           0           1            1 switch               dynamic
	 * 	 per object statistics active             0           0           1            1 switch               dynamic
	 * 	 performance monitoring option            0           0           0            0 switch               dynamic
	 * 	 plan text pipe active                    0           0           1            1 switch               dynamic
	 * 	 plan text pipe max messages              0        1857       10000        10000 number               dynamic
	 * 	 process wait events                      0           0           1            1 switch               dynamic
	 *   sql text pipe active                     0           0           1            1 switch               dynamic
	 *   sql text pipe max messages               0         272        1000         1000 number               dynamic
	 * 	 statement pipe active                    0           0           1            1 switch               dynamic
	 * 	 statement pipe max messages              0         446        5000         5000 number               dynamic
	 * 	 statement statistics active              0           0           1            1 switch               dynamic
	 * 	 wait event timing                        0           0           1            1 switch               dynamic
	 * 
	 */

	public void loadFromAse(Connection conn)
	{
		if ( ! AseConnectionUtils.isConnectionOk(conn, true, this) )
			return;

		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery("sp_configure 'Monitoring'");
			while (rs.next())
			{
				String config = rs.getString(1).trim();
				int    runVal = rs.getInt(5);
				int    cfgVal = rs.getInt(4);
//				String typeVal = rs.getString(7).trim();
//				boolean isStatic = typeVal.equals("static");

				if      ( config.equals("enable monitoring") )              _enableMonitoring_chk            .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("per object statistics active") )   _perObjectStatisticsActive_chk   .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("statement statistics active") )    _statementStatisticsActive_chk   .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("enable spinlock monitoring") )     _enableSpinlockMonitoring_chk    .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("execution time monitoring") )      _executionTimeMonitoring_chk     .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("enable stmt cache monitoring") )   _statementCacheMonitoring_chk    .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("capture compression statistics") ) _captureCompressionStatistics_chk.setSelected( runVal == 1 ? true : false); // but this is for the moment not part of sp_configure 'Monitoring'
				else if ( config.equals("object lockwait timing") )         _objectLockwaitTiming_chk        .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("process wait events") )            _processWaitEvents_chk           .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("SQL batch capture") )              _sqlBatchCapture_chk             .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("wait event timing") )              _waitEventTiming_chk             .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("lock timeout pipe active") )       _lockTimeoutPipeActive_chk       .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("lock timeout pipe max messages") ) _lockTimeoutPipeMaxMessages_spm  .setValue( new Integer(runVal) );
				else if ( config.equals("deadlock pipe active") )           _deadlockPipeActive_chk          .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("deadlock pipe max messages") )     _deadlockPipeMaxMessages_spm     .setValue( new Integer(runVal) );
				else if ( config.equals("errorlog pipe active") )           _errorlogPipeActive_chk          .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("errorlog pipe max messages") )     _errorlogPipeMaxMessages_spm     .setValue( new Integer(runVal) );
				else if ( config.equals("threshold event monitoring") )     _thresholdEventMonitoring_chk    .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("threshold event max messages") )   _thresholdEventMaxMessages_spm   .setValue( new Integer(runVal) );
				else if ( config.equals("sql text pipe active") )           _sqlTextPipeActive_chk           .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("sql text pipe max messages") )     _sqlTextPipeMaxMessages_spm      .setValue( new Integer(runVal) );
				else if ( config.equals("statement pipe active") )          _statementPipeActive_chk         .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("statement pipe max messages") )    _statementPipeMaxMessages_spm    .setValue( new Integer(runVal) );
				else if ( config.equals("plan text pipe active") )          _planTextPipeActive_chk          .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("plan text pipe max messages") )    _planTextPipeMaxMessages_spm     .setValue( new Integer(runVal) );
				else if ( config.equals("max SQL text monitored") )
				{
					if (cfgVal != runVal)
					{
						_logger.debug("Config option("+cfgVal+") not same as run("+runVal+")");
						((JSpinner.DefaultEditor)((JSpinner)this._maxSqlTextMonitored_sp).getEditor()).getTextField().setForeground(Color.RED);
						_maxSqlTextMonitored_sp.setToolTipText("ASE server needs to be rebooted since this is a static configuration parameter. The run value is still "+runVal+".");
					}
					else
					{
						// Take same colour as other spinner
						_maxSqlTextMonitored_sp.setToolTipText( _tts.get(_maxSqlTextMonitored_sp) );
						((JSpinner.DefaultEditor)((JSpinner)this._maxSqlTextMonitored_sp).getEditor()).getTextField().setForeground(Color.BLACK);
					}
					_maxSqlTextMonitored_spm.setValue( new Integer(cfgVal) );
				}
				else if ( config.equals("performance monitoring option") )
				{
					// Do nothing here
					_logger.debug("Skipping 'performance monitoring option'.");
				}
				else
				{
					// Do nothing here
					// We could print out "unhandled" configuration options.
					_logger.info("UNKNOWN option '"+config+"' this is probably a new option after "+Version.getAppName()+" has been developed. executed command sp_configure 'Monitoring'.");
				}
			}
			rs.close();

//			if (_aseVersionNum >= 15031) 
//			if (_aseVersionNum >= 1503010)
			if (_aseVersionNum >= Ver.ver(15,0,3,1))
				_cfgCapMissingStatistics_chk.setSelected( AseConnectionUtils.getAseConfigRunValue(conn, "capture missing statistics") > 0 );

//			if (_aseVersionNum >= 15020) 
//			if (_aseVersionNum >= 1502000) 
			if (_aseVersionNum >= Ver.ver(15,0,2)) 
			{
				_cfgEnableMetricsCapture_chk.setSelected( AseConnectionUtils.getAseConfigRunValue(conn, "enable metrics capture") > 0 );
				_cfgMetricsElapMax_spm      .setValue(    AseConnectionUtils.getAseConfigRunValue(conn, "metrics elap max") );
				_cfgMetricsExecMax_spm      .setValue(    AseConnectionUtils.getAseConfigRunValue(conn, "metrics exec max") );
				_cfgMetricsLioMax_spm       .setValue(    AseConnectionUtils.getAseConfigRunValue(conn, "metrics lio max") );
				_cfgMetricsPioMax_spm       .setValue(    AseConnectionUtils.getAseConfigRunValue(conn, "metrics pio max") );
			}

//			if (_aseVersionNum >= 15700) 
//			if (_aseVersionNum >= 1570000) 
			if (_aseVersionNum >= Ver.ver(15,7)) 
			{
				_captureCompressionStatistics_chk.setSelected( AseConnectionUtils.getAseConfigRunValue(conn, "capture compression statistics") > 0 );
			}

			// "enable file access"
			_enableFileAccess_chk.setSelected( AseConnectionUtils.getAseConfigRunValue(conn, "enable file access") > 0 );
			
			// How much memory are there left for re-configuration
			rs = stmt.executeQuery(
					"select additional_free_memory = str( ((max(b.value) - min(b.value)) / 512.0), 15,1 ) " +
					"from master.dbo.sysconfigures a, master.dbo.syscurconfigs b " +
					"where a.name in ('max memory', 'total logical memory') " +
					"  and a.config = b.config");
			while (rs.next())
			{
				String mb = rs.getString(1).trim();
				_configurabelMemory_lbl.setText(mb + default_configurabelMemoryText);
			}
			rs.close();
			
			stmt.close();
		}
		catch (SQLException e)
		{
			AseConnectionUtils.showSqlExceptionMessage(this, msgDialogTitle, 
					"Error when executing the following SQL statement: sp_configure 'Monitoring'", e); 
		}
		catch (Exception e)
		{
			SwingUtils.showErrorMessage(this, msgDialogTitle, 
					"Error when executing the following SQL statement: sp_configure 'Monitoring'" +
					"\n\n" + e.getMessage(), e);
		}
	}

	private boolean isXfsLicenseEnabled(Connection conn)
	{
		if (conn == null)
			return false;

		if (_aseVersionNum >= Ver.ver(15,0))
			return true;

		int enabled = 0;
		String sql = "select count(*) from master.dbo.monLicense where Name = 'ASE_XFS'";

		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				enabled = rs.getInt(1);
			}
		}
		catch (SQLException e)
		{
			AseConnectionUtils.showSqlExceptionMessage(this, msgDialogTitle, 
					"Error when executing the following SQL statement: "+sql, e); 
		}
		return enabled > 0;
	}

	private void reInitializeDependantPerformanceCounters(String aseConfig, boolean newValue) 
//	throws SQLException
	{
		if ( ! StringUtil.isNullOrBlank(aseConfig) )
		{
			int newConfigVal = newValue ? 1 : 0;

			List<CountersModel> cmList = CounterController.getInstance().getCmListDependsOnConfig(aseConfig, _conn, _aseVersionNum, _isClusterEnabled);
			if (cmList.size() > 0)
			{
				// List for info message
				List<String> cmNames = new ArrayList<String>();
				for (CountersModel cm : cmList)
					cmNames.add(cm.getName());

				_logger.info("Re-Initializing Performance Counter(s) '"+StringUtil.toCommaStr(cmNames)+"', due to ASE Configuration changes. config='"+aseConfig+"', to='"+newConfigVal+"'.");

				for (CountersModel cm : cmList)
				{
					if ( ! cm.isRuntimeInitialized() )
						continue;

					// set it to active state, it will be reset to false later "cm.init()"
					cm.setActive(true, null);

					//------------------------------------------------------
					// Below is copied from: GetCounters.initCounters()
					//------------------------------------------------------
					
					// set the version
//					cm.setServerVersion(_aseVersionNum);
//					cm.setClusterEnabled(_isClusterEnabled);
					
					// set the active roles, so it can be used in initSql()
//					cm.setActiveRoles(_activeRoleList);

					// set the ASE Monitor Configuration, so it can be used in initSql() and elsewhere
					//cm.setMonitorConfigs(monitorConfigMap);
					// OR do this for ALL CM's after the loop.
					Map<String,Integer> mcm = cm.getMonitorConfigMap();
					if (mcm != null)
						mcm.put(aseConfig, newConfigVal);

					// Now when we are connected to a server, and properties are set in the CM, 
					// mark it as runtime initialized (or late initialization)
					// do NOT do this at the end of this method, because some of the above stuff
					// will be used by the below method calls.
//					cm.setRuntimeInitialized(true);

					// Initializes SQL, use getServerVersion to check what we are connected to.
					cm.initSql(_conn);

					// Use this method if we need to check anything in the database.
					// for example "deadlock pipe" may not be active...
					// If server version is below 15.0.2 statement cache info should not be VISABLE
//					cm.init(_conn);
					try
					{
						cm.clear(); // clears the Counters and GUI parts
						cm.init(_conn);
					}
					catch (Exception e)
					{
						String cmShortName = cm.getName();
						String cmLongName  = cm.getDisplayName();

						_logger.warn("Re-Initializing Performance Counter '"+cmLongName+"' shortName='"+cmShortName+"', due to ASE Configuration changes. config='"+aseConfig+"', to='"+newConfigVal+"'.", e);
					}
				}
			}
		}
		
	}
	private boolean checkAndSetAseConfig(Connection conn, String config, JComponent comp)
	{
		// Resetting the tool tip to it's original tool tip message
		comp.setToolTipText(_tts.get(comp));

		try
		{
			if ( ! comp.isEnabled() )
			{
				return false;
			}

			//------------------------------------------
			// Check and set, for various OBJECT types
			//------------------------------------------
			if (comp instanceof JCheckBox)
			{
				JCheckBox chkbox = (JCheckBox) comp;
				chkbox.setForeground( Color.BLACK );

				boolean aseCfgBool = false;
				boolean guiCfgBool = false;
	
				aseCfgBool = AseConnectionUtils.getAseConfigConfigValue(conn, config) != 0;
				guiCfgBool = chkbox.isSelected();
				if ( aseCfgBool != guiCfgBool )
				{
					AseConnectionUtils.setAseConfigValue(conn, config, guiCfgBool);
					
					// Maube get the configured value...
					//int newRunValue = AseConnectionUtils.getAseConfigRunValue(conn, config);

					// reinitialize PerformanceCounters that depends on this configuration.
					reInitializeDependantPerformanceCounters(config, guiCfgBool);
				}
			}
			else if (comp instanceof JSpinner)
			{
				JSpinner spinner = (JSpinner) comp;
				((JSpinner.DefaultEditor)((JSpinner)spinner).getEditor()).getTextField().setForeground(Color.BLACK);
	
				int     aseCfgInt  = 0;
				int     guiCfgInt  = 0;
				Object  guiCfgObj  = null;
				
				aseCfgInt = AseConnectionUtils.getAseConfigConfigValue(conn, config);
				guiCfgObj = spinner.getModel().getValue();
				
				if ( guiCfgObj instanceof Integer )
				{
					guiCfgInt = ((Integer)guiCfgObj).intValue();
					if ( aseCfgInt != guiCfgInt )
					{
						AseConnectionUtils.setAseConfigValue(conn, config, guiCfgInt);
					}
				}
				else
				{
					// Only handles JSpinner Containing Integer objects
					//new NotImplementedException();
				}
			}
			else
			{
				// Not handled this type of object
				//new NotImplementedException();
			}
		}
		catch (SQLException sqle)
		{
			String errStr = "";
			while (sqle != null)
			{
				errStr += sqle.getMessage() + " ";
				sqle = sqle.getNextException();
			}
			if ( ! errStr.equals("") )
			{
				_configErrors = true;
				comp.setToolTipText(errStr);
				
				_logger.warn("checkAndSetAseConfig(): Problems when configuring '"+config+"', got error '"+errStr+"'.");
				
				if (comp instanceof JCheckBox)
				{
					JCheckBox chkbox = (JCheckBox) comp;
					chkbox.setForeground( Color.RED );
				}
				else if (comp instanceof JSpinner)
				{
					JSpinner spinner = (JSpinner) comp;
					((JSpinner.DefaultEditor)((JSpinner)spinner).getEditor()).getTextField().setForeground(Color.RED);
				}
			}
		}

		//------------------------------------------
		// Check if it was correctly set
		//------------------------------------------
		return true;
	}

	public void storeInAse(Connection conn)
	{
		if (conn == null)
		{
			if ( _monitoringPanel_1.isEnabled() )
			{
				SwingUtils.showInfoMessage(this, msgDialogTitle, "Not connected to any ASE server.");
			}
			return;
		}

		_configErrors = false;

		
		// If we are currenty in REFRESH, then do GUI wait for the sample to finish.
//		if (GetCounters.hasInstance() && GetCounters.getInstance().isRefreshing())
		if (CounterController.hasInstance() && CounterController.getInstance().isRefreshing())
		{
			WaitForExecDialog wait = new WaitForExecDialog(MainFrame.getInstance(), "Waiting for currect sample to finish");

			// Kick this of as it's own thread, otherwise the sleep below, might block the Swing Event Dispatcher Thread
			BgExecutor terminateConnectionTask = new BgExecutor(wait)
			{
				@Override
				public Object doWork()
				{
					// - Wait for current refresh period to end.
//					GetCounters getCnt = AseTune.getCounterCollector();
					ICounterController getCnt = CounterController.getInstance();
					if (getCnt != null)
					{
						boolean pauseState = MainFrame.getInstance().isPauseSampling();
						// just to be sure, pause it
						MainFrame.getInstance().setPauseSampling(true);

						long startTime = System.currentTimeMillis();
						char[] progressChars = new char[] {'-', '\\', '|', '/'}; 
						for (int i=0; ; i++)
						{
							// wait until the current refresh loop has finished
							if (!getCnt.isRefreshing())
								break;
	
							// don't sleep forever, lets wait 60 seconds.
							int timeoutAfter = 60;
							long sleptSoFar = System.currentTimeMillis() - startTime;
							if (sleptSoFar > (timeoutAfter * 1000) )
								break;
	
							char pc = progressChars[ i % 4 ];
							_logger.info("Waiting for GetCounters to stop before I can: Clearing components... Waited for "+sleptSoFar+" ms so far. Giving up after "+timeoutAfter+" seconds");
							getWaitDialog().setState("Waiting for 'refresh' to end "+pc);
	
							try { Thread.sleep(500); }
							catch (InterruptedException ignore) {}
						}

						// if initial state was "running", then un-pause it.
						if ( ! pauseState);
							MainFrame.getInstance().setPauseSampling(false);
					}
			
					return null;
				}
			};
			wait.execAndWait(terminateConnectionTask);
		}
		
		// NOTE: Version checking is NOT needed here
		//       checkAndSetAseConfig() only do stuff if: comp.isEnabled()
		
		checkAndSetAseConfig(conn, "enable monitoring",              _enableMonitoring_chk);
		checkAndSetAseConfig(conn, "per object statistics active",   _perObjectStatisticsActive_chk);
		checkAndSetAseConfig(conn, "statement statistics active",    _statementStatisticsActive_chk);
		checkAndSetAseConfig(conn, "enable spinlock monitoring",     _enableSpinlockMonitoring_chk);
		checkAndSetAseConfig(conn, "execution time monitoring",      _executionTimeMonitoring_chk);
		checkAndSetAseConfig(conn, "enable stmt cache monitoring",   _statementCacheMonitoring_chk);
		checkAndSetAseConfig(conn, "capture compression statistics", _captureCompressionStatistics_chk);
		checkAndSetAseConfig(conn, "object lockwait timing",         _objectLockwaitTiming_chk);
		checkAndSetAseConfig(conn, "process wait events",            _processWaitEvents_chk);
		checkAndSetAseConfig(conn, "SQL batch capture",              _sqlBatchCapture_chk);
		checkAndSetAseConfig(conn, "wait event timing",              _waitEventTiming_chk);

		checkAndSetAseConfig(conn, "lock timeout pipe active",       _lockTimeoutPipeActive_chk);
		checkAndSetAseConfig(conn, "deadlock pipe active",           _deadlockPipeActive_chk);
		checkAndSetAseConfig(conn, "errorlog pipe active",           _errorlogPipeActive_chk);
		checkAndSetAseConfig(conn, "threshold event monitoring",     _thresholdEventMonitoring_chk);
		checkAndSetAseConfig(conn, "sql text pipe active",           _sqlTextPipeActive_chk);
		checkAndSetAseConfig(conn, "statement pipe active",          _statementPipeActive_chk);
		checkAndSetAseConfig(conn, "plan text pipe active",          _planTextPipeActive_chk);

		checkAndSetAseConfig(conn, "lock timeout pipe max messages", _lockTimeoutPipeMaxMessages_sp);
		checkAndSetAseConfig(conn, "deadlock pipe max messages",     _deadlockPipeMaxMessages_sp);
		checkAndSetAseConfig(conn, "errorlog pipe max messages",     _errorlogPipeMaxMessages_sp);
		checkAndSetAseConfig(conn, "threshold event max messages",   _thresholdEventMaxMessages_sp);
		checkAndSetAseConfig(conn, "sql text pipe max messages",     _sqlTextPipeMaxMessages_sp);
		checkAndSetAseConfig(conn, "statement pipe max messages",    _statementPipeMaxMessages_sp);
		checkAndSetAseConfig(conn, "plan text pipe max messages",    _planTextPipeMaxMessages_sp);
		checkAndSetAseConfig(conn, "max SQL text monitored",         _maxSqlTextMonitored_sp);

		checkAndSetAseConfig(conn, "enable file access",             _enableFileAccess_chk);

		checkAndSetAseConfig(conn, "capture missing statistics",     _cfgCapMissingStatistics_chk);
		checkAndSetAseConfig(conn, "enable metrics capture",         _cfgEnableMetricsCapture_chk);
		checkAndSetAseConfig(conn, "metrics elap max",               _cfgMetricsElapMax_sp);
		checkAndSetAseConfig(conn, "metrics exec max",               _cfgMetricsExecMax_sp);
		checkAndSetAseConfig(conn, "metrics lio max",                _cfgMetricsLioMax_sp);
		checkAndSetAseConfig(conn, "metrics pio max",                _cfgMetricsPioMax_sp);

		// Go ahead and load "current" ASE configuration.
		if ( ! _configErrors )
		{
			loadFromAse(conn);
		}
	}

//	public boolean onExitDisable()
//	{
//		return _onExitAutoDisable_rb.isSelected();
//	}
//	public boolean onExitPrompt()
//	{
//		return _onExitAsk_rb.isSelected();
//	}

	public static int onExit(Component parent, Connection conn, boolean canBeAborted)
	{
//		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return 0;
		}

		boolean onExitDoNotDisable = false;
		boolean onExitAutoDisable  = false;
		boolean onExitPrompt       = false;

		String str = conf.getProperty("config.on_exit", ON_EXIT_STR[ON_EXIT_NONE]);
		_logger.debug("loadProps: 'config.on_exit' = '"+str+"'");
		if (str != null)
		{
			if ( str.equals(ON_EXIT_STR[ON_EXIT_NONE]) )    onExitDoNotDisable = true;
			if ( str.equals(ON_EXIT_STR[ON_EXIT_DISABLE]) ) onExitAutoDisable  = true;
			if ( str.equals(ON_EXIT_STR[ON_EXIT_PROMPT]) )  onExitPrompt       = true;
		}

		// No need to continue
		if (onExitDoNotDisable)
			return 0;

		// No need to continue, if config is already DISABLED
		boolean isEnabled = AseConnectionUtils.getAseConfigRunValueBooleanNoEx(conn, "enable monitoring");
		if ( ! isEnabled )
			return 0;

		// No need to continue, if we do NOT hace SA_ROLE
		boolean hasSaRole = AseConnectionUtils.hasRole(conn, AseConnectionUtils.SA_ROLE);
		if ( ! hasSaRole )
			return 0;

		
		boolean disable = onExitAutoDisable;

		if ( onExitPrompt )
		{
			String cancelDesc = "";
			if (canBeAborted)
				cancelDesc =	"<br><li>If 'Cancel' the Disconnect will be aborted.<br></li>";

			str = "<html>" +
					"Do you want to disable ASE monitoring when exiting "+Version.getAppName()+"<br>" +
					"<ul>" +
					"<li>" +
					   "If 'Yes' the following SQL statement will be sent to the ASE Server:<br>" +
					   "<b>exec sp_configure 'enable monitoring', 0</b><br>" +
					"</li>" +
					cancelDesc +
					"</html>";

			int optionType = JOptionPane.YES_NO_OPTION;
			if (canBeAborted)
				optionType = JOptionPane.YES_NO_CANCEL_OPTION;

			int answer = JOptionPane.showConfirmDialog(parent, str, "Disable Monitoring On Disconnect Or Exit", optionType, JOptionPane.QUESTION_MESSAGE);
			_logger.debug("onExitPrompt: "+answer);
 			
			if ( answer == JOptionPane.YES_OPTION )
			{
				disable = true;
			}
			if ( answer == JOptionPane.CANCEL_OPTION )
			{
				disable = false;
				return answer;
			}
		}

		if ( disable )
		{
			// No need to continue if the connection is already dead or closed
			if ( ! AseConnectionUtils.isConnectionOk(conn, true, parent) )
				return 0;

			try 
			{
				int otherAseTuneCount = 0;

				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select count(*)-1 from master..sysprocesses where program_name like '"+Version.getAppName()+"%'");
				while (rs.next())
				{
					otherAseTuneCount = rs.getInt(1);
				}
				rs.close();
				stmt.close();
				
				if ( otherAseTuneCount > 0 )
				{
					JOptionPane.showMessageDialog(parent, "There are "+otherAseTuneCount+" other '"+Version.getAppName()+"' applications connected to the ASE Server. I can't disable monitoring for the moment.", "Disable Monitoring On Disconnect Or Exit", JOptionPane.INFORMATION_MESSAGE);
				}
				else
				{
					AseConnectionUtils.setAseConfigValue(conn, "enable monitoring", false);
				}
			}
			catch (SQLException e)
			{
				return 0;
			}			
		}
		return 0;
	}
	
	/*---------------------------------------------------
	** END: helper methods
	**---------------------------------------------------
	*/


	/*---------------------------------------------------
	** BEGIN: implementing saveProps & loadProps
	**---------------------------------------------------
	*/
	private void saveProps()
	{
		String onExit = null;
		if ( _onExitDoNotDisable_rb.isSelected() ) onExit = ON_EXIT_STR[ON_EXIT_NONE];
		if ( _onExitAutoDisable_rb .isSelected() ) onExit = ON_EXIT_STR[ON_EXIT_DISABLE];
		if ( _onExitAsk_rb         .isSelected() ) onExit = ON_EXIT_STR[ON_EXIT_PROMPT];

		_logger.debug("saveProps: 'config.on_exit' = '"+onExit+"'");

		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}

		conf.setProperty("config.on_exit", onExit);

		conf.save();
	}

	private void loadProps()
	{
//		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}

		String str = null;

		str = conf.getProperty("config.on_exit", ON_EXIT_STR[ON_EXIT_NONE]);
		_logger.debug("loadProps: 'config.on_exit' = '"+str+"'");
		if (str != null)
		{
			if ( str.equals(ON_EXIT_STR[ON_EXIT_NONE]) )    _onExitDoNotDisable_rb.setSelected(true);
			if ( str.equals(ON_EXIT_STR[ON_EXIT_DISABLE]) ) _onExitAutoDisable_rb .setSelected(true);
			if ( str.equals(ON_EXIT_STR[ON_EXIT_PROMPT]) )  _onExitAsk_rb         .setSelected(true);
		}
	}
	/*---------------------------------------------------
	** END: implementing saveProps & loadProps
	**---------------------------------------------------
	*/


	/*---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	**---- SUBCLASSES ---- SUBCLASES ---- SUBCLASES -----
	**---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	*/
	private class ToolTipStore
	{
		private Map<JComponent,String> _storage = new HashMap<JComponent,String>();

		protected String add(JComponent comp, String toolTipText)
		{
			String tt = toolTipText;

			// remove <html> at START
			if (tt.startsWith("<html>") || tt.startsWith("<HTML>"))
				tt = tt.substring("<html>".length());

			// remove </html> at END
			if (tt.endsWith("</html>") || tt.endsWith("<HTML>"))
				tt = tt.substring(0, tt.length()-"</html>".length());
			

			// Add BEGIN html tags
			tt = "<html>"+tt;

			// Add CM's that depends on this configuration
			String cfg = (String) comp.getClientProperty(ASE_CONFIG);
			if ( ! StringUtil.isNullOrBlank(cfg) )
			{
				List<CountersModel> cmList = CounterController.getInstance().getCmListDependsOnConfig(cfg, _conn, _aseVersionNum, _isClusterEnabled);
				if (cmList.size() > 0)
				{
					tt += "<br><br>";
					tt += "Configuration '<b>"+cfg+"</b>', must be set for the following Performance Counters to work.";
					tt += "<ul>";
					for (CountersModel cm : cmList)
					{
						tt += "<li>";
						tt += cm.getDisplayName();
						tt += "</li>";
					}
					tt += "</ul>";
				}
			}

			// Add END html tag
			tt += "</html>";

			_storage.put(comp, tt);
			return tt;
		}

		protected String get(JComponent comp)
		{
			return _storage.get(comp);
		}
	}




	/*---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	**----- TEST-CODE ---- TEST-CODE ---- TEST-CODE -----
	**---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	*/
	public static void main(String[] args)
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		Properties log4jProps = new Properties();
		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		Configuration conf1 = new Configuration("c:\\projects\\asetune\\asetune.save.properties");
		Configuration.setInstance(Configuration.USER_TEMP, conf1);

		Configuration conf2 = new Configuration("c:\\projects\\asetune\\asetune.properties");
		Configuration.setInstance(Configuration.SYSTEM_CONF, conf2);


		// DO THE THING
		try
		{
			System.out.println("Open the Dialog with a VALID connection.");
//			Connection conn = AseConnectionFactory.getConnection("gorans-xp", 5000, null, "sa", "", "test-AseConfigMonitoringDialog", null);
			DbxConnection conn = DbxConnection.createDbxConnection( AseConnectionFactory.getConnection("gorans-xp", 5000, null, "sa", "", "test-AseConfigMonitoringDialog", null) );
//			AseConfigMonitoringDialog.showDialog((Frame)null, conn, 1251000);
			AseConfigMonitoringDialog.showDialog((Frame)null, conn, Ver.ver(12,5,1));

			System.out.println("Open the Dialog with a CLOSED connection.");
			conn.close();
//			AseConfigMonitoringDialog.showDialog((Frame)null, conn, 1251000);
			AseConfigMonitoringDialog.showDialog((Frame)null, conn, Ver.ver(12,5,1));

			System.out.println("Open the Dialog with a NULL connection.");
//			AseConfigMonitoringDialog.showDialog((Frame)null, null, 1251000);
			AseConfigMonitoringDialog.showDialog((Frame)null, null, Ver.ver(12,5,1));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
}

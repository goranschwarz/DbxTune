/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui;

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
import java.util.HashMap;
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
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import asemon.Version;
import asemon.utils.AseConnectionFactory;
import asemon.utils.AseConnectionUtils;
import asemon.utils.Configuration;
import asemon.utils.SwingUtils;

public class AseMonitoringConfigDialog
    extends JDialog
    implements ActionListener
{
	private static final long serialVersionUID = 7587303888489834272L;
	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(AseMonitoringConfigDialog.class);
	
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

	private Connection         _conn          = null;
	private boolean            _configErrors  = false;

	private int                _aseVersionNum = 0;

	/** Save tool tip in a Map so we can restore them later */
	private ToolTipStore       _tts           = new ToolTipStore();

	
	// PANEL: MONITORING
	private JPanel             _monitoringPanel               = null;
	private JCheckBox          _enableMonitoring_chk          = new JCheckBox("Enable monitoring");
	private JCheckBox          _perObjectStatisticsActive_chk = new JCheckBox("Per object statistics active");
	private JCheckBox          _statementStatisticsActive_chk = new JCheckBox("Statement statistics active");
	private JCheckBox          _statementCacheMonitoring_chk  = new JCheckBox("Statement Cache Monitoring");
	private JCheckBox          _objectLockwaitTiming_chk      = new JCheckBox("Object lockwait timing");
	private JCheckBox          _processWaitEvents_chk         = new JCheckBox("Process wait events");
	private JCheckBox          _sqlBatchCapture_chk           = new JCheckBox("SQL batch capture");
	private JCheckBox          _waitEventTiming_chk           = new JCheckBox("Wait event timing");

	private JCheckBox          _deadlockPipeActive_chk        = new JCheckBox("Deadlock pipe active");
	private SpinnerNumberModel _deadlockPipeMaxMessages_spm   = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 500); // value, min, max, step
	private JSpinner           _deadlockPipeMaxMessages_sp    = new JSpinner(_deadlockPipeMaxMessages_spm);

	private JCheckBox          _errorlogPipeActive_chk        = new JCheckBox("Errorlog pipe active");
	private SpinnerNumberModel _errorlogPipeMaxMessages_spm   = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 500); // value, min, max, step
	private JSpinner           _errorlogPipeMaxMessages_sp    = new JSpinner(_errorlogPipeMaxMessages_spm);

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

	private JLabel             _predefinedConfigs_lbl         = new JLabel("Use pre-defined configuration");
	private JComboBox          _predefinedConfigs_cbx         = new JComboBox(PDC_OPTIONS_STR);

	private JLabel             _configurabelMemory_lbl        = new JLabel("#"+default_configurabelMemoryText);

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
	private AseMonitoringConfigDialog(Frame owner, Connection conn, int aseVersionNum, String title)
	{
		super(owner, title, true);
		init(owner, conn, aseVersionNum);
	}
	private AseMonitoringConfigDialog(Dialog owner, Connection conn, int aseVersionNum, String title)
	{
		super(owner, title, true);
		init(owner, conn, aseVersionNum);
	}
	private void init(Window owner, Connection conn, int aseVersionNum)
	{

		_conn = conn;

		setAseVersion(aseVersionNum);
		initComponents();
		pack();

//		Dimension size = getPreferredSize();
//		size.width += 200;
//
//		setPreferredSize(size);
////		setMinimumSize(size);
//		setSize(size);

		setLocationRelativeTo(owner);

		setFocus();
	}


	public static void showDialog(Frame owner, Connection conn, int aseVersionNum)
	{
		AseMonitoringConfigDialog dialog = new AseMonitoringConfigDialog(owner, conn, aseVersionNum, msgDialogTitle);
		dialog.setVisible(true);
		dialog.dispose();
	}
	public static void showDialog(Dialog owner, Connection conn, int aseVersionNum)
	{
		AseMonitoringConfigDialog dialog = new AseMonitoringConfigDialog(owner, conn, aseVersionNum, msgDialogTitle);
		dialog.setVisible(true);
		dialog.dispose();
	}
	public static void showDialog(Component owner, Connection conn, int aseVersionNum)
	{
		AseMonitoringConfigDialog dialog = null;
		if (owner instanceof Frame)
			dialog = new AseMonitoringConfigDialog((Frame)owner, conn, aseVersionNum, msgDialogTitle);
		else if (owner instanceof Dialog)
			dialog = new AseMonitoringConfigDialog((Dialog)owner, conn, aseVersionNum, msgDialogTitle);
		else
			dialog = new AseMonitoringConfigDialog((Dialog)null, conn, aseVersionNum, msgDialogTitle);

		dialog.setVisible(true);
		dialog.dispose();
	}

//	public AseMonitoringConfigDialog(JDialog dialog, Connection conn)
//	{
//		super(dialog);
//		_tmpConn = conn;
//
////		_mainFrame = frame;
//		init();
//	}
//	public AseMonitoringConfigDialog(JFrame frame, Connection conn)
//	{
//		super(frame);
//		_tmpConn = conn;
//
////		_mainFrame = frame;
//		init();
//	}
//
//	public AseMonitoringConfigDialog(MainFrame frame)
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
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("wrap 1","grow",""));   // insets Top Left Bottom Right

		_monitoringPanel = createMonitoringPanel();

		// ADD the OK, Cancel, Apply buttons
		panel.add(_monitoringPanel,  "grow");
		panel.add(createOnExitPanel(),      "grow");
		panel.add(createOkCancelPanel(),    "bottom, right, push");

		loadProps();

		setContentPane(panel);
	}

	private JPanel createMonitoringPanel()
	{
		JPanel panel = SwingUtils.createPanel("sp_configure 'Monitoring'", true);
		panel.setLayout(new MigLayout("gap 0","",""));   // insets Top Left Bottom Right

		//--- TOOLTIP 
		// The ToolTip is also used to display configuration problems...
		// So the ToolTipStore is used to reset the original ToolTip when problem is solved.
		_enableMonitoring_chk          .setToolTipText(_tts.add(_enableMonitoring_chk,          "enable monitoring specifies whether the Adaptive Server will collect information for the Monitoring and Diagnostic System."));
		_perObjectStatisticsActive_chk .setToolTipText(_tts.add(_perObjectStatisticsActive_chk, "per object statistics active determines whether the Adaptive Server will collect monitoring information on a per object basis."));
		_statementStatisticsActive_chk .setToolTipText(_tts.add(_statementStatisticsActive_chk, "statement statistics active indicates whether ASE will collect ad-hoc statement monitoring information."));
		_statementCacheMonitoring_chk  .setToolTipText(_tts.add(_statementCacheMonitoring_chk,  "Use 'enable stmt cache monitoring' to configure Adaptive Server to collect the monitoring information on the statement cache.."));
		_objectLockwaitTiming_chk      .setToolTipText(_tts.add(_objectLockwaitTiming_chk,      "object lockwait timing specifies whether the Adaptive Server will collect timing data on lock requests."));
		_processWaitEvents_chk         .setToolTipText(_tts.add(_processWaitEvents_chk,         "process event timing specifies whether the Adaptive Server will collect monitoring data on wait events for individual processes."));
		_sqlBatchCapture_chk           .setToolTipText(_tts.add(_sqlBatchCapture_chk,           "SQL batch capture indicates whether the Adaptive Server will collect sql batch text for each process."));
		_waitEventTiming_chk           .setToolTipText(_tts.add(_waitEventTiming_chk,           "wait event timing specifies whether the Adaptive Server will collect monitoring data for all wait events."));

		_deadlockPipeActive_chk        .setToolTipText(_tts.add(_deadlockPipeActive_chk,        "deadlock pipe active indicates whether the Adaptive Server will collect historical deadlock monitoring information."));
		_deadlockPipeMaxMessages_sp    .setToolTipText(_tts.add(_deadlockPipeMaxMessages_sp,    "deadlock pipe max messages specifies the maximum number of messages that can be stored for historical deadlock data."));

		_errorlogPipeActive_chk        .setToolTipText(_tts.add(_errorlogPipeActive_chk,        "errorlog pipe active indicates whether the Adaptive Server will collect historical errorlog monitoring information."));
		_errorlogPipeMaxMessages_sp    .setToolTipText(_tts.add(_errorlogPipeMaxMessages_sp,    "errorlog pipe max messages specifies the maximum number of messages that can be stored for historical errorlog text."));

		_sqlTextPipeActive_chk         .setToolTipText(_tts.add(_sqlTextPipeActive_chk,         "sql text pipe active indicates whether the Adaptive Server will collect historical sql batch text information. (This may degrade performance up to 5%)"));
		_sqlTextPipeMaxMessages_sp     .setToolTipText(_tts.add(_sqlTextPipeMaxMessages_sp,     "sql text pipe max messages specifies the maximum number of messages that can be stored for historical sql text. (This may degrade performance up to 5%)"));

		_statementPipeActive_chk       .setToolTipText(_tts.add(_statementPipeActive_chk,       "statement pipe active indicates whether the Adaptive Server will collect historical statement level monitoring information. (This may degrade performance up to 10%)"));
		_statementPipeMaxMessages_sp   .setToolTipText(_tts.add(_statementPipeMaxMessages_sp,   "statement pipe max messages specifies the maximum number of messages that can be stored for historical statement text. (This may degrade performance up to 10%)"));

		_planTextPipeActive_chk        .setToolTipText(_tts.add(_planTextPipeActive_chk,        "plan text pipe active indicates whether the Adaptive Server will collect historical plan text monitoring information. (This may degrade performance up to 25%)"));
		_planTextPipeMaxMessages_sp    .setToolTipText(_tts.add(_planTextPipeMaxMessages_sp,    "plan text pipe max messages specifies the maximum number of messages that can be stored for historical plan text. (This may degrade performance up to 25%)"));

		_maxSqlTextMonitored_lbl       .setToolTipText(_tts.add(_maxSqlTextMonitored_lbl,       "Restart required: specifies the amount of memory allocated per user connection for saving SQL text to memory shared by Adaptive Server. The default value is 0."));
		_maxSqlTextMonitored_sp        .setToolTipText(_tts.add(_maxSqlTextMonitored_sp,        "Restart required: specifies the amount of memory allocated per user connection for saving SQL text to memory shared by Adaptive Server. The default value is 0."));

		_predefinedConfigs_lbl         .setToolTipText(_tts.add(_predefinedConfigs_lbl,         "A pre defined value set of the above configurations."));
		_predefinedConfigs_cbx         .setToolTipText(_tts.add(_predefinedConfigs_cbx,         "A pre defined value set of the above configurations."));

		_configurabelMemory_lbl        .setToolTipText(_tts.add(_configurabelMemory_lbl,        "Available memory to be used for additional configurations or 'data caches' etc. This memory is basically not used by the ASE, meaning it's \"waste\" and ready for usage by someone..."));



		//--- LAYOUT
		panel.add(_enableMonitoring_chk,          "wrap 15");

		panel.add(_perObjectStatisticsActive_chk, "wrap");
		panel.add(_statementStatisticsActive_chk, "wrap");
		panel.add(_statementCacheMonitoring_chk,  "wrap");
		panel.add(_objectLockwaitTiming_chk,      "wrap");
		panel.add(_processWaitEvents_chk,         "wrap");
		panel.add(_sqlBatchCapture_chk,           "wrap");
		panel.add(_waitEventTiming_chk,           "wrap 15");

		panel.add(_deadlockPipeActive_chk,        "");
		panel.add(_deadlockPipeMaxMessages_sp,    "right, wrap");

		panel.add(_errorlogPipeActive_chk,        "");
		panel.add(_errorlogPipeMaxMessages_sp,    "right, wrap 10");

		panel.add(_sqlTextPipeActive_chk,         "");
		panel.add(_sqlTextPipeMaxMessages_sp,     "right, wrap");

		panel.add(_statementPipeActive_chk,       "");
		panel.add(_statementPipeMaxMessages_sp,   "right, wrap");

		panel.add(_planTextPipeActive_chk,        "");
		panel.add(_planTextPipeMaxMessages_sp,    "right, wrap 15");

		panel.add(_maxSqlTextMonitored_lbl,       "");
		panel.add(_maxSqlTextMonitored_sp,        "right, wrap 10");

		panel.add(new JSeparator(),               "span, grow, push, wrap 10");

		panel.add(_predefinedConfigs_lbl,         "");
		panel.add(_predefinedConfigs_cbx,         "right, wrap 10");

		panel.add(_configurabelMemory_lbl,        "span, wrap");

		//--- ACTIONS
		_predefinedConfigs_cbx.addActionListener(this);

		return panel;
	}

	private JPanel createOnExitPanel()
	{
		JPanel panel = SwingUtils.createPanel("Disable Monitoring On Exit", true);
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

		//--- TOOLTIP
		_onExitDoNotDisable_rb.setToolTipText("Monitoring is not disabled when sybmon is terminated.");
		_onExitAutoDisable_rb .setToolTipText("If no other sybmon is running at the same time, the monitoring will be disabled and on next login it will be enabled again.");
		_onExitAsk_rb         .setToolTipText("A popup will ask you if you want to disable Monitoring everytime the 'AseMon' is terminated.");

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
		panel.add(_ok,     "tag ok, right");
		panel.add(_cancel, "tag cancel");
		panel.add(_apply,  "tag apply");

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


	
	
	/*---------------------------------------------------
	** BEGIN: override methods
	**---------------------------------------------------
	*/
	public void setVisible(boolean visible)
	{
		_logger.debug("AseMonitoringConfigDialog.setVisible("+visible+")");

		if ( ! AseConnectionUtils.hasRole(_conn, AseConnectionUtils.SA_ROLE) )
		{
			String msg = "Sorry, you need 'sa_role' to configure monitoring parameters. Ask you'r System Administrator to configure this.";
			SwingUtils.showErrorMessage(this, "Not authorized to do sp_configure", msg, null);
			
			return;
		}

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
			}

			_predefinedConfigs_cbx.setSelectedIndex(PDC_FROM_ASE);
			_monitoringPanel.setEnabled(true);
			for (int i=0; i<_monitoringPanel.getComponentCount(); i++)
			{
				Component comp = _monitoringPanel.getComponent(i);

				// Simplifies things if we always set this to FALSE first
				// and then set the various components to true.
				comp.setEnabled(false);

				// Special components that has ASE version dependencies
				if ( comp.equals(_statementCacheMonitoring_chk) )
				{
					if (_aseVersionNum >= 15020)
					{
						comp.setEnabled(true);
					}
				}
				else // All other components
				{
					comp.setEnabled(true);
				}
			}
		}
		else
		{
			_predefinedConfigs_cbx.setSelectedIndex(PDC_NONE);
			_monitoringPanel.setEnabled(false);
			for (int i=0; i<_monitoringPanel.getComponentCount(); i++)
			{
				_monitoringPanel.getComponent(i).setEnabled(false);
			}
		}

		if (visible)
			loadProps();

		super.setVisible(visible);
	}
	/*---------------------------------------------------
	** END: override methods
	**---------------------------------------------------
	*/

	
	
	
	/*---------------------------------------------------
	** BEGIN: implementing ActionListener, KeyListeners
	**---------------------------------------------------
	*/
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
			_enableMonitoring_chk         .setSelected(false);

			_perObjectStatisticsActive_chk.setSelected(false);
			_statementStatisticsActive_chk.setSelected(false);
			_statementCacheMonitoring_chk .setSelected(false);
			_objectLockwaitTiming_chk     .setSelected(false);
			_processWaitEvents_chk        .setSelected(false);
			_sqlBatchCapture_chk          .setSelected(false);
			_waitEventTiming_chk          .setSelected(false);
			_deadlockPipeActive_chk       .setSelected(false);
			_errorlogPipeActive_chk       .setSelected(false);
			_sqlTextPipeActive_chk        .setSelected(false);
			_planTextPipeActive_chk       .setSelected(false);
			_statementPipeActive_chk      .setSelected(false);

			_deadlockPipeMaxMessages_spm  .setValue( new Integer(0) );
			_errorlogPipeMaxMessages_spm  .setValue( new Integer(0) );

			_sqlTextPipeMaxMessages_spm   .setValue( new Integer(0) );
			_statementPipeMaxMessages_spm .setValue( new Integer(0) );
			_planTextPipeMaxMessages_spm  .setValue( new Integer(0) );

			_maxSqlTextMonitored_spm      .setValue( new Integer(0) );
		}
		if ( str.equals(PDC_OPTIONS_STR[PDC_MINIMAL]) ) // MINIMAL
		{
			_enableMonitoring_chk         .setSelected(true);

			_perObjectStatisticsActive_chk.setSelected(true);
			_statementStatisticsActive_chk.setSelected(false);
			_statementCacheMonitoring_chk .setSelected(false);
			_objectLockwaitTiming_chk     .setSelected(false);
			_processWaitEvents_chk        .setSelected(false);
			_sqlBatchCapture_chk          .setSelected(false);
			_waitEventTiming_chk          .setSelected(true);
			_deadlockPipeActive_chk       .setSelected(false);
			_errorlogPipeActive_chk       .setSelected(false);
			_sqlTextPipeActive_chk        .setSelected(false);
			_statementPipeActive_chk      .setSelected(false);
			_planTextPipeActive_chk       .setSelected(false);

			_deadlockPipeMaxMessages_spm  .setValue( new Integer(0) );
			_errorlogPipeMaxMessages_spm  .setValue( new Integer(0) );

			_sqlTextPipeMaxMessages_spm   .setValue( new Integer(0) );
			_statementPipeMaxMessages_spm .setValue( new Integer(0) );
			_planTextPipeMaxMessages_spm  .setValue( new Integer(0) );

			_maxSqlTextMonitored_spm      .setValue( new Integer(0) );
		}
		if ( str.equals(PDC_OPTIONS_STR[PDC_SMALL]) ) // SMALL
		{
			_enableMonitoring_chk         .setSelected(true);

			_perObjectStatisticsActive_chk.setSelected(true);
			_statementStatisticsActive_chk.setSelected(true);
			_statementCacheMonitoring_chk .setSelected(true);
			_objectLockwaitTiming_chk     .setSelected(true);
			_processWaitEvents_chk        .setSelected(true);
			_sqlBatchCapture_chk          .setSelected(false);
			_waitEventTiming_chk          .setSelected(true);
			_deadlockPipeActive_chk       .setSelected(false);
			_errorlogPipeActive_chk       .setSelected(false);
			_sqlTextPipeActive_chk        .setSelected(false);
			_statementPipeActive_chk      .setSelected(false);
			_planTextPipeActive_chk       .setSelected(false);

			_deadlockPipeMaxMessages_spm  .setValue( new Integer(0) );
			_errorlogPipeMaxMessages_spm  .setValue( new Integer(0) );

			_sqlTextPipeMaxMessages_spm   .setValue( new Integer(0) );
			_statementPipeMaxMessages_spm .setValue( new Integer(0) );
			_planTextPipeMaxMessages_spm  .setValue( new Integer(0) );

			_maxSqlTextMonitored_spm      .setValue( new Integer(0) );
		}
		if ( str.equals(PDC_OPTIONS_STR[PDC_MEDIUM]) ) // MEDIUM
		{
			_enableMonitoring_chk         .setSelected(true);

			_perObjectStatisticsActive_chk.setSelected(true);
			_statementStatisticsActive_chk.setSelected(true);
			_statementCacheMonitoring_chk .setSelected(true);
			_objectLockwaitTiming_chk     .setSelected(true);
			_processWaitEvents_chk        .setSelected(true);
			_sqlBatchCapture_chk          .setSelected(true);
			_waitEventTiming_chk          .setSelected(true);
			_deadlockPipeActive_chk       .setSelected(true);
			_errorlogPipeActive_chk       .setSelected(true);
			_sqlTextPipeActive_chk        .setSelected(true);
			_statementPipeActive_chk      .setSelected(true);
			_planTextPipeActive_chk       .setSelected(false);

			_deadlockPipeMaxMessages_spm  .setValue( new Integer(500) );
			_errorlogPipeMaxMessages_spm  .setValue( new Integer(200) );

			_sqlTextPipeMaxMessages_spm   .setValue( new Integer(1000) );
			_statementPipeMaxMessages_spm .setValue( new Integer(5000) );
			_planTextPipeMaxMessages_spm  .setValue( new Integer(0) );

			_maxSqlTextMonitored_spm      .setValue( new Integer(2048) );
		}
		if ( str.equals(PDC_OPTIONS_STR[PDC_LARGE]) ) // LARGE
		{
			_enableMonitoring_chk         .setSelected(true);

			_perObjectStatisticsActive_chk.setSelected(true);
			_statementStatisticsActive_chk.setSelected(true);
			_statementCacheMonitoring_chk .setSelected(true);
			_objectLockwaitTiming_chk     .setSelected(true);
			_processWaitEvents_chk        .setSelected(true);
			_sqlBatchCapture_chk          .setSelected(true);
			_waitEventTiming_chk          .setSelected(true);
			_deadlockPipeActive_chk       .setSelected(true);
			_errorlogPipeActive_chk       .setSelected(true);
			_sqlTextPipeActive_chk        .setSelected(true);
			_statementPipeActive_chk      .setSelected(true);
			_planTextPipeActive_chk       .setSelected(false);

			_deadlockPipeMaxMessages_spm  .setValue( new Integer(1000) );
			_errorlogPipeMaxMessages_spm  .setValue( new Integer(1000) );

			_sqlTextPipeMaxMessages_spm   .setValue( new Integer(5000) );
			_statementPipeMaxMessages_spm .setValue( new Integer(50000) );
			_planTextPipeMaxMessages_spm  .setValue( new Integer(0) );

			_maxSqlTextMonitored_spm      .setValue( new Integer(4096) );
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

				if      ( config.equals("enable monitoring") )            _enableMonitoring_chk         .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("per object statistics active") ) _perObjectStatisticsActive_chk.setSelected( runVal == 1 ? true : false);
				else if ( config.equals("statement statistics active") )  _statementStatisticsActive_chk.setSelected( runVal == 1 ? true : false);
				else if ( config.equals("enable stmt cache monitoring") ) _statementCacheMonitoring_chk .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("object lockwait timing") )       _objectLockwaitTiming_chk     .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("process wait events") )          _processWaitEvents_chk        .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("SQL batch capture") )            _sqlBatchCapture_chk          .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("wait event timing") )            _waitEventTiming_chk          .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("deadlock pipe active") )         _deadlockPipeActive_chk       .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("deadlock pipe max messages") )   _deadlockPipeMaxMessages_spm  .setValue( new Integer(runVal) );
				else if ( config.equals("errorlog pipe active") )         _errorlogPipeActive_chk       .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("errorlog pipe max messages") )   _errorlogPipeMaxMessages_spm  .setValue( new Integer(runVal) );
				else if ( config.equals("sql text pipe active") )         _sqlTextPipeActive_chk        .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("sql text pipe max messages") )   _sqlTextPipeMaxMessages_spm   .setValue( new Integer(runVal) );
				else if ( config.equals("statement pipe active") )        _statementPipeActive_chk      .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("statement pipe max messages") )  _statementPipeMaxMessages_spm .setValue( new Integer(runVal) );
				else if ( config.equals("plan text pipe active") )        _planTextPipeActive_chk       .setSelected( runVal == 1 ? true : false);
				else if ( config.equals("plan text pipe max messages") )  _planTextPipeMaxMessages_spm  .setValue( new Integer(runVal) );
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
					_logger.info("UNKNOWN option '"+config+"' this is probably a new option after AseMon has been developed. executed command sp_configure 'Monitoring'.");
				}
			}
			rs.close();

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
			if ( _monitoringPanel.isEnabled() )
			{
				SwingUtils.showInfoMessage(this, msgDialogTitle, "Not connected to any ASE server.");
			}
			return;
		}

		_configErrors = false;

		checkAndSetAseConfig(conn, "enable monitoring",           _enableMonitoring_chk);
		checkAndSetAseConfig(conn, "per object statistics active",_perObjectStatisticsActive_chk);
		checkAndSetAseConfig(conn, "statement statistics active", _statementStatisticsActive_chk);
		checkAndSetAseConfig(conn, "enable stmt cache monitoring",_statementCacheMonitoring_chk);
		checkAndSetAseConfig(conn, "object lockwait timing",      _objectLockwaitTiming_chk);
		checkAndSetAseConfig(conn, "process wait events",         _processWaitEvents_chk);
		checkAndSetAseConfig(conn, "SQL batch capture",           _sqlBatchCapture_chk);
		checkAndSetAseConfig(conn, "wait event timing",           _waitEventTiming_chk);
		checkAndSetAseConfig(conn, "deadlock pipe active",        _deadlockPipeActive_chk);
		checkAndSetAseConfig(conn, "errorlog pipe active",        _errorlogPipeActive_chk);
		checkAndSetAseConfig(conn, "sql text pipe active",        _sqlTextPipeActive_chk);
		checkAndSetAseConfig(conn, "statement pipe active",       _statementPipeActive_chk);
		checkAndSetAseConfig(conn, "plan text pipe active",       _planTextPipeActive_chk);
		checkAndSetAseConfig(conn, "deadlock pipe max messages",  _deadlockPipeMaxMessages_sp);
		checkAndSetAseConfig(conn, "errorlog pipe max messages",  _errorlogPipeMaxMessages_sp);
		checkAndSetAseConfig(conn, "sql text pipe max messages",  _sqlTextPipeMaxMessages_sp);
		checkAndSetAseConfig(conn, "statement pipe max messages", _statementPipeMaxMessages_sp);
		checkAndSetAseConfig(conn, "plan text pipe max messages", _planTextPipeMaxMessages_sp);
		checkAndSetAseConfig(conn, "max SQL text monitored",      _maxSqlTextMonitored_sp);

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

	public static int onExit(Component parent, Connection conn)
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

		
		boolean disable = onExitAutoDisable;

		if ( onExitPrompt )
		{
			str = "Do you want to disable ASE monitoring when exiting asemon.\n\nIf yes the following SQL statement will be sent to the ASE Server:\nexec sp_configure 'enable monitoring', 0\n\n";
			int answer = JOptionPane.showConfirmDialog(parent, str, "Disable Monitoring On Exit", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
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
				int otherAsemonCount = 0;

				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select count(*)-1 from master..sysprocesses where program_name like '"+Version.getAppName()+"%'");
				while (rs.next())
				{
					otherAsemonCount = rs.getInt(1);
				}
				rs.close();
				stmt.close();
				
				if ( otherAsemonCount > 0 )
				{
					JOptionPane.showMessageDialog(parent, "There are "+otherAsemonCount+" other '"+Version.getAppName()+"' applications connected to the ASE Server. I cant disable monitoring for the moment.", "Disable Monitoring On Exit", JOptionPane.INFORMATION_MESSAGE);
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
			_storage.put(comp, toolTipText);
			return toolTipText;
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

		Configuration conf1 = new Configuration("c:\\projects\\asemon\\asemon.save.properties");
		Configuration.setInstance(Configuration.USER_TEMP, conf1);

		Configuration conf2 = new Configuration("c:\\projects\\asemon\\asemon.properties");
		Configuration.setInstance(Configuration.SYSTEM_CONF, conf2);


		// DO THE THING
		try
		{
			System.out.println("Open the Dialog with a VALID connection.");
			Connection conn = AseConnectionFactory.getConnection("goransxp", 5000, null, "sa", "", "test-AseMonitoringConfigDialog", null);
			AseMonitoringConfigDialog.showDialog((Frame)null, conn, 12510);

			System.out.println("Open the Dialog with a CLOSED connection.");
			conn.close();
			AseMonitoringConfigDialog.showDialog((Frame)null, conn, 12510);

			System.out.println("Open the Dialog with a NULL connection.");
			AseMonitoringConfigDialog.showDialog((Frame)null, null, 12510);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
}

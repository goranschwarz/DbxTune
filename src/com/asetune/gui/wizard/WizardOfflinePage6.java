/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.log4j.Logger;
import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardPage;
import org.netbeans.spi.wizard.WizardPanelNavResult;

import com.asetune.Version;
import com.asetune.gui.swing.MultiLineLabel;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;


public class WizardOfflinePage6
extends WizardPage
implements ActionListener
{
    private static final long serialVersionUID = 1L;
	private static final String WIZ_NAME = "sql-capture";
	private static final String WIZ_DESC = "Capture DDL/SQL";
	private static final String WIZ_HELP = ""
			+ "<html>"
			+ "Should individual DDL/SQL Statements be captured.<br>"
			+ "<br>"
			+ "<b>Note:</b> This is only available for AseTune and SqlServerTune<br>"
			+ "</html>";

	private static Logger _logger = Logger.getLogger(WizardOfflinePage6.class);

	//---- PCS:DDL Lookup & Store
	private JCheckBox            _pcsDdl_doDdlLookupAndStore_chk             = new JCheckBox("Do DDL lookup and Store", PersistentCounterHandler.DEFAULT_ddl_doDdlLookupAndStore);
	private JCheckBox            _pcsDdl_enabledForDatabaseObjects_chk       = new JCheckBox("DB Objects",              PersistentCounterHandler.DEFAULT_ddl_enabledForDatabaseObjects);
	private JCheckBox            _pcsDdl_enabledForStatementCache_chk        = new JCheckBox("Statement Cache",         PersistentCounterHandler.DEFAULT_ddl_enabledForStatementCache);
	private JLabel               _pcsDdl_afterDdlLookupSleepTimeInMs_lbl     = new JLabel("Sleep Time");
	private JTextField           _pcsDdl_afterDdlLookupSleepTimeInMs_txt     = new JTextField(""+PersistentCounterHandler.DEFAULT_ddl_afterDdlLookupSleepTimeInMs);
	private JCheckBox            _pcsDdl_addDependantObjectsToDdlInQueue_chk = new JCheckBox("Store Dependent Objects", PersistentCounterHandler.DEFAULT_ddl_addDependantObjectsToDdlInQueue);

	//---- PCS: Capture SQL Statements
	private JCheckBox            _pcsCapSql_doSqlCaptureAndStore_chk          = new JCheckBox("Do SQL Capture and Store", PersistentCounterHandler.DEFAULT_sqlCap_doSqlCaptureAndStore);
	private JLabel               _pcsCapSql_sleepTimeInMs_lbl                 = new JLabel("Sleep Time");
	private JTextField           _pcsCapSql_sleepTimeInMs_txt                 = new JTextField(""+PersistentCounterHandler.DEFAULT_sqlCap_sleepTimeInMs);
	private JCheckBox            _pcsCapSql_doSqlText_chk                     = new JCheckBox("SQL Text",       PersistentCounterHandler.DEFAULT_sqlCap_doSqlText);
	private JCheckBox            _pcsCapSql_doStatementInfo_chk               = new JCheckBox("Statement Info", PersistentCounterHandler.DEFAULT_sqlCap_doStatementInfo);
	private JCheckBox            _pcsCapSql_doPlanText_chk                    = new JCheckBox("Plan Text",      PersistentCounterHandler.DEFAULT_sqlCap_doPlanText);

	private JLabel               _pcsCapSql_saveStatement_lbl                 = new JLabel("                   But only save Statements if: "); // apces is for "aligning" with the field _pcsCapSql_sendDdlForLookup_chk
	private JLabel               _pcsCapSql_saveStatement_execTime_lbl        = new JLabel("Exec Time is above (ms)");
	private JTextField           _pcsCapSql_saveStatement_execTime_txt        = new JTextField(""+PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_execTime, 3);
	private JLabel               _pcsCapSql_saveStatement_logicalRead_lbl     = new JLabel(", and Logical Reads >");
	private JTextField           _pcsCapSql_saveStatement_logicalRead_txt     = new JTextField(""+PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_logicalReads, 3);
	private JLabel               _pcsCapSql_saveStatement_physicalRead_lbl    = new JLabel(", and Physical Reads >");
	private JTextField           _pcsCapSql_saveStatement_physicalRead_txt    = new JTextField(""+PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_physicalReads, 3);

	private JCheckBox            _pcsCapSql_sendDdlForLookup_chk              = new JCheckBox("Send Statements for DDL Lookup if:", PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup);
	private JLabel               _pcsCapSql_sendDdlForLookup_execTime_lbl     = new JLabel("Exec Time is above (ms)");
	private JTextField           _pcsCapSql_sendDdlForLookup_execTime_txt     = new JTextField(""+PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_execTime, 3);
	private JLabel               _pcsCapSql_sendDdlForLookup_logicalRead_lbl  = new JLabel(", and Logical Reads >");
	private JTextField           _pcsCapSql_sendDdlForLookup_logicalRead_txt  = new JTextField(""+PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_logicalReads, 3);
	private JLabel               _pcsCapSql_sendDdlForLookup_physicalRead_lbl = new JLabel(", and Physical Reads >");
	private JTextField           _pcsCapSql_sendDdlForLookup_physicalRead_txt = new JTextField(""+PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_physicalReads, 3);

	public static String getDescription() { return WIZ_DESC; }
	@Override
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	private JPanel createPcsDdlLookupAndStorePanel()
	{
		JPanel panel = SwingUtils.createPanel("DDL Lookup and Store", true);
		panel.setLayout(new MigLayout("", "", ""));   // insets Top Left Bottom Right

		_pcsDdl_doDdlLookupAndStore_chk            .setToolTipText("<html>If you want the most accessed objects, Stored procedures, views etc and active statements to be DDL information to be stored in the PCS.<br>You can view them with the tool 'DDL Viewer' when connected to a offline database.<html>");
		_pcsDdl_enabledForDatabaseObjects_chk      .setToolTipText("<html>Store Database Objects (Stored procedures, views, tables, triggers, etc).<html>");
		_pcsDdl_enabledForStatementCache_chk       .setToolTipText("<html>Store Statement Cache (XML Plans).<html>");
		_pcsDdl_addDependantObjectsToDdlInQueue_chk.setToolTipText("Also do DDL Lookup and Storage of dependant objects. Simply does 'exec sp_depends tabname' and add dependant objects for lookup...");
		_pcsDdl_afterDdlLookupSleepTimeInMs_lbl    .setToolTipText("How many milliseconds should we wait between DDL Lookups, this so we do not saturate the source DB Server.");
		_pcsDdl_afterDdlLookupSleepTimeInMs_txt    .setToolTipText("How many milliseconds should we wait between DDL Lookups, this so we do not saturate the source DB Server.");

		// LAYOUT
		panel.add(_pcsDdl_doDdlLookupAndStore_chk,             "");
		panel.add(_pcsDdl_enabledForDatabaseObjects_chk,       "");
		panel.add(_pcsDdl_enabledForStatementCache_chk,        "");
		panel.add(_pcsDdl_addDependantObjectsToDdlInQueue_chk, "");

		panel.add(_pcsDdl_afterDdlLookupSleepTimeInMs_lbl,     "gap 50");
		panel.add(_pcsDdl_afterDdlLookupSleepTimeInMs_txt,     "pushx, growx, wrap");
		
		// ACTIONS
		_pcsDdl_doDdlLookupAndStore_chk            .addActionListener(this);
		_pcsDdl_enabledForDatabaseObjects_chk      .addActionListener(this);
		_pcsDdl_enabledForStatementCache_chk       .addActionListener(this);
		_pcsDdl_addDependantObjectsToDdlInQueue_chk.addActionListener(this);
		_pcsDdl_afterDdlLookupSleepTimeInMs_txt    .addActionListener(this);

		// ADD FOCUS LISTENERS
//		xxx_txt.addFocusListener(this);
//		xxx_txt.addFocusListener(this);

		return panel;
	}

	private JPanel createPcsSqlCaptureAndStorePanel()
	{
		JPanel panel = SwingUtils.createPanel("Capture SQL and Store", true);
		panel.setLayout(new MigLayout("", "", ""));   // insets Top Left Bottom Right

		_pcsCapSql_doSqlCaptureAndStore_chk.setToolTipText("<html>Store executed SQL Statements and it's SQL Text when recording a session.<html>");
//		_pcsCapSql_xxx_chk                 .setToolTipText("<html>xxx</html>");
		_pcsCapSql_doSqlText_chk           .setToolTipText("<html>Collect SQL Text                   <br>NOTE requires: sp_configure 'sql text pipe active'  and 'sql text pipe max messages'.</html>");
		_pcsCapSql_doStatementInfo_chk     .setToolTipText("<html>Collect SQL Statements information <br>NOTE requires: sp_configure 'statement pipe active' and 'statement pipe max messages'.</html>");
		_pcsCapSql_doPlanText_chk          .setToolTipText("<html>Collect SQL Plans                  <br>NOTE requires: sp_configure 'plan text pipe active' and 'plan text pipe max messages'.</html>");
		_pcsCapSql_sleepTimeInMs_lbl       .setToolTipText("<html>How many milliseconds should we wait between SQL Capture Lookups.</html>");
		_pcsCapSql_sleepTimeInMs_txt       .setToolTipText("<html>How many milliseconds should we wait between SQL Capture Lookups.</html>");

		_pcsCapSql_saveStatement_lbl             .setToolTipText("<html>You can choose to save Statements that are more expensive... set some limits on what to save...</html>");
		_pcsCapSql_saveStatement_execTime_lbl    .setToolTipText("<html>Only save Statements if the execution time for this statement is above this value in milliseconds. <br>Note: -1 means save all statements </html>"); // <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to save it.</html>");
		_pcsCapSql_saveStatement_execTime_txt    .setToolTipText("<html>Only save Statements if the execution time for this statement is above this value in milliseconds. <br>Note: -1 means save all statements </html>"); // <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to save it.</html>");
		_pcsCapSql_saveStatement_logicalRead_lbl .setToolTipText("<html>Only save Statements if the number of LogicalReads for this statement is above this value.         <br>Note: -1 means save all statements </html>"); // <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to save it.</html>");
		_pcsCapSql_saveStatement_logicalRead_txt .setToolTipText("<html>Only save Statements if the number of LogicalReads for this statement is above this value.         <br>Note: -1 means save all statements </html>"); // <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to save it.</html>");
		_pcsCapSql_saveStatement_physicalRead_lbl.setToolTipText("<html>Only save Statements if the number of PhysicalReads for this statement is above this value.        <br>Note: -1 means save all statements </html>"); // <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to save it.</html>");
		_pcsCapSql_saveStatement_physicalRead_txt.setToolTipText("<html>Only save Statements if the number of PhysicalReads for this statement is above this value.        <br>Note: -1 means save all statements </html>"); // <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to save it.</html>");

		_pcsCapSql_sendDdlForLookup_chk             .setToolTipText("<html>When a procedure name is found in monSysStatement send it of for DDL Lookup</html>");
		_pcsCapSql_sendDdlForLookup_execTime_lbl    .setToolTipText("<html>Send DDL only if the execution time for this statement is above this value in milliseconds. </html>"); // <br>Note: -1 means send all statements <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to send it for DDL lookup.</html>");
		_pcsCapSql_sendDdlForLookup_execTime_txt    .setToolTipText("<html>Send DDL only if the execution time for this statement is above this value in milliseconds. </html>"); // <br>Note: -1 means send all statements <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to send it for DDL lookup.</html>");
		_pcsCapSql_sendDdlForLookup_logicalRead_lbl .setToolTipText("<html>Send DDL only if the number of LogicalReads for this statement is above this value.         </html>"); // <br>Note: -1 means send all statements <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to send it for DDL lookup.</html>");
		_pcsCapSql_sendDdlForLookup_logicalRead_txt .setToolTipText("<html>Send DDL only if the number of LogicalReads for this statement is above this value.         </html>"); // <br>Note: -1 means send all statements <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to send it for DDL lookup.</html>");
		_pcsCapSql_sendDdlForLookup_physicalRead_lbl.setToolTipText("<html>Send DDL only if the number of PhysicalReads for this statement is above this value.        </html>"); // <br>Note: -1 means send all statements <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to send it for DDL lookup.</html>");
		_pcsCapSql_sendDdlForLookup_physicalRead_txt.setToolTipText("<html>Send DDL only if the number of PhysicalReads for this statement is above this value.        </html>"); // <br>Note: -1 means send all statements <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to send it for DDL lookup.</html>");

		// LAYOUT
		panel.add(_pcsCapSql_doSqlCaptureAndStore_chk, "split");
		panel.add(_pcsCapSql_doSqlText_chk,            "");
		panel.add(_pcsCapSql_doStatementInfo_chk,      "");
		panel.add(_pcsCapSql_doPlanText_chk,           "");
//		panel.add(_pcsCapSql_xxx_chk,                  "");
		panel.add(_pcsCapSql_sleepTimeInMs_lbl,        "gap 50");
		panel.add(_pcsCapSql_sleepTimeInMs_txt,        "pushx, growx, wrap");
		
		panel.add(_pcsCapSql_saveStatement_lbl,                 "split");
		panel.add(_pcsCapSql_saveStatement_execTime_lbl,        "");
		panel.add(_pcsCapSql_saveStatement_execTime_txt,        "wmin 10lp, pushx, growx");
		panel.add(_pcsCapSql_saveStatement_logicalRead_lbl,     "");
		panel.add(_pcsCapSql_saveStatement_logicalRead_txt,     "wmin 10lp, pushx, growx");
		panel.add(_pcsCapSql_saveStatement_physicalRead_lbl,    "");
		panel.add(_pcsCapSql_saveStatement_physicalRead_txt,    "wmin 10lp, pushx, growx, wrap");

		panel.add(_pcsCapSql_sendDdlForLookup_chk,              "split");
		panel.add(_pcsCapSql_sendDdlForLookup_execTime_lbl,     "");
		panel.add(_pcsCapSql_sendDdlForLookup_execTime_txt,     "wmin 10lp, pushx, growx");
		panel.add(_pcsCapSql_sendDdlForLookup_logicalRead_lbl,  "");
		panel.add(_pcsCapSql_sendDdlForLookup_logicalRead_txt,  "wmin 10lp, pushx, growx");
		panel.add(_pcsCapSql_sendDdlForLookup_physicalRead_lbl, "");
		panel.add(_pcsCapSql_sendDdlForLookup_physicalRead_txt, "wmin 10lp, pushx, growx, wrap");

		// ACTIONS
		_pcsCapSql_doSqlCaptureAndStore_chk.addActionListener(this);
		_pcsCapSql_doSqlText_chk           .addActionListener(this);
		_pcsCapSql_doStatementInfo_chk     .addActionListener(this);
		_pcsCapSql_doPlanText_chk          .addActionListener(this);
//		_pcsCapSql_xxx_chk                 .addActionListener(this);
		_pcsCapSql_sleepTimeInMs_txt       .addActionListener(this);

		_pcsCapSql_saveStatement_execTime_txt    .addActionListener(this);
		_pcsCapSql_saveStatement_logicalRead_txt .addActionListener(this);
		_pcsCapSql_saveStatement_physicalRead_txt.addActionListener(this);
		
		_pcsCapSql_sendDdlForLookup_chk             .addActionListener(this);
		_pcsCapSql_sendDdlForLookup_execTime_txt    .addActionListener(this);
		_pcsCapSql_sendDdlForLookup_logicalRead_txt .addActionListener(this);
		_pcsCapSql_sendDdlForLookup_physicalRead_txt.addActionListener(this);
		
		// ADD FOCUS LISTENERS
////		xxx_txt.addFocusListener(this);
//		_pcsCapSql_sleepTimeInMs_txt                .addFocusListener(this);
//		_pcsCapSql_sendDdlForLookup_execTime_txt    .addFocusListener(this);
//		_pcsCapSql_sendDdlForLookup_logicalRead_txt .addFocusListener(this);
//		_pcsCapSql_sendDdlForLookup_physicalRead_txt.addFocusListener(this);

		return panel;
	}

	/** Helper class to toggle if fields are "enabled" or not */ 
	private void updateEnabledForPcsDdlLookupAndSqlCapture()
	{
		boolean enableDdlLookupFields  = _pcsDdl_doDdlLookupAndStore_chk    .isSelected();
		boolean enableSqlCaptureFields = _pcsCapSql_doSqlCaptureAndStore_chk.isSelected();
		
		_pcsDdl_enabledForDatabaseObjects_chk      .setEnabled(enableDdlLookupFields);
		_pcsDdl_enabledForStatementCache_chk       .setEnabled(enableDdlLookupFields);
		_pcsDdl_addDependantObjectsToDdlInQueue_chk.setEnabled(enableDdlLookupFields);
		_pcsDdl_afterDdlLookupSleepTimeInMs_lbl    .setEnabled(enableDdlLookupFields);
		_pcsDdl_afterDdlLookupSleepTimeInMs_txt    .setEnabled(enableDdlLookupFields);
		
		_pcsCapSql_doSqlText_chk           .setEnabled(enableSqlCaptureFields);
		_pcsCapSql_doStatementInfo_chk     .setEnabled(enableSqlCaptureFields);
		_pcsCapSql_doPlanText_chk          .setEnabled(enableSqlCaptureFields);
		_pcsCapSql_sleepTimeInMs_lbl       .setEnabled(enableSqlCaptureFields);
		_pcsCapSql_sleepTimeInMs_txt       .setEnabled(enableSqlCaptureFields);

		_pcsCapSql_saveStatement_lbl             .setEnabled(enableSqlCaptureFields);
		_pcsCapSql_saveStatement_execTime_lbl    .setEnabled(enableSqlCaptureFields);
		_pcsCapSql_saveStatement_execTime_txt    .setEnabled(enableSqlCaptureFields);
		_pcsCapSql_saveStatement_logicalRead_lbl .setEnabled(enableSqlCaptureFields);
		_pcsCapSql_saveStatement_logicalRead_txt .setEnabled(enableSqlCaptureFields);
		_pcsCapSql_saveStatement_physicalRead_lbl.setEnabled(enableSqlCaptureFields);
		_pcsCapSql_saveStatement_physicalRead_txt.setEnabled(enableSqlCaptureFields);
		
		_pcsCapSql_sendDdlForLookup_chk             .setEnabled(enableDdlLookupFields && enableSqlCaptureFields);
		_pcsCapSql_sendDdlForLookup_execTime_lbl    .setEnabled(enableDdlLookupFields && enableSqlCaptureFields);
		_pcsCapSql_sendDdlForLookup_execTime_txt    .setEnabled(enableDdlLookupFields && enableSqlCaptureFields);
		_pcsCapSql_sendDdlForLookup_logicalRead_lbl .setEnabled(enableDdlLookupFields && enableSqlCaptureFields);
		_pcsCapSql_sendDdlForLookup_logicalRead_txt .setEnabled(enableDdlLookupFields && enableSqlCaptureFields);
		_pcsCapSql_sendDdlForLookup_physicalRead_lbl.setEnabled(enableDdlLookupFields && enableSqlCaptureFields);
		_pcsCapSql_sendDdlForLookup_physicalRead_txt.setEnabled(enableDdlLookupFields && enableSqlCaptureFields);
	}
	
	
	public WizardOfflinePage6()
	{
		super(WIZ_NAME, WIZ_DESC);
		
//		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));
		setLayout(new MigLayout());

		// Add a helptext
		add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );

//		add(new JLabel("<html>This is <b>NOT YET</b> implemented, stay tuned.</html>"), "growx, wrap 10");
//		add(new JLabel("<html>Just press <b>Next</b> to continue.</html>"), "growx, wrap 10");

		add(createPcsDdlLookupAndStorePanel(),  "growx, pushx, wrap");
		add(createPcsSqlCaptureAndStorePanel(), "growx, pushx, wrap");

		initData();
	}

	private void initData()
	{
	}

	@Override
	protected String validateContents(Component comp, Object event)
	{
		return null;
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
		updateEnabledForPcsDdlLookupAndSqlCapture();
	}

	private void saveWizardData()
	{
		if ("AseTune".equals(Version.getAppName()) || "SqlServerTune".equals(Version.getAppName()))
		{
			putWizardData(PersistentCounterHandler.PROPKEY_ddl_doDdlLookupAndStore                 , _pcsDdl_doDdlLookupAndStore_chk             .isSelected() +"");
			putWizardData(PersistentCounterHandler.PROPKEY_ddl_enabledForDatabaseObjects           , _pcsDdl_enabledForDatabaseObjects_chk       .isSelected() +"");
			putWizardData(PersistentCounterHandler.PROPKEY_ddl_enabledForStatementCache            , _pcsDdl_enabledForStatementCache_chk        .isSelected() +"");
			putWizardData(PersistentCounterHandler.PROPKEY_ddl_afterDdlLookupSleepTimeInMs         , _pcsDdl_afterDdlLookupSleepTimeInMs_txt     .getText()    +"");
			putWizardData(PersistentCounterHandler.PROPKEY_ddl_addDependantObjectsToDdlInQueue     , _pcsDdl_addDependantObjectsToDdlInQueue_chk .isSelected() +"");

			putWizardData(PersistentCounterHandler.PROPKEY_sqlCap_doSqlCaptureAndStore             , _pcsCapSql_doSqlCaptureAndStore_chk         .isSelected() +"");
			putWizardData(PersistentCounterHandler.PROPKEY_sqlCap_sleepTimeInMs                    , _pcsCapSql_sleepTimeInMs_txt                .getText()    +"");
			putWizardData(PersistentCounterHandler.PROPKEY_sqlCap_doSqlText                        , _pcsCapSql_doSqlText_chk                    .isSelected() +"");
			putWizardData(PersistentCounterHandler.PROPKEY_sqlCap_doStatementInfo                  , _pcsCapSql_doStatementInfo_chk              .isSelected() +"");
			putWizardData(PersistentCounterHandler.PROPKEY_sqlCap_doPlanText                       , _pcsCapSql_doPlanText_chk                   .isSelected() +"");
			putWizardData(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_execTime        , _pcsCapSql_saveStatement_execTime_txt       .getText()    +"");
			putWizardData(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_logicalReads    , _pcsCapSql_saveStatement_logicalRead_txt    .getText()    +"");
			putWizardData(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_physicalReads   , _pcsCapSql_saveStatement_physicalRead_txt   .getText()    +"");
			putWizardData(PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup                 , _pcsCapSql_sendDdlForLookup_chk             .isSelected() +"");
			putWizardData(PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_execTime     , _pcsCapSql_sendDdlForLookup_execTime_txt    .getText()    +"");
			putWizardData(PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_logicalReads , _pcsCapSql_sendDdlForLookup_logicalRead_txt .getText()    +"");
			putWizardData(PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_physicalReads, _pcsCapSql_sendDdlForLookup_physicalRead_txt.getText()    +"");
		}
		else
		{
			_logger.info("Functionality for '" + WIZ_DESC + "' is not supported for '"+Version.getAppName()+"'. No properties will be written for this wizard page.");
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public WizardPanelNavResult allowBack(String stepName, Map settings, Wizard wizard)
    {
		saveWizardData();
		return WizardPanelNavResult.PROCEED;
    }

	@SuppressWarnings("rawtypes")
	@Override
	public WizardPanelNavResult allowNext(String stepName, Map settings, Wizard wizard)
	{
		saveWizardData();
		return WizardPanelNavResult.PROCEED;
	}
}


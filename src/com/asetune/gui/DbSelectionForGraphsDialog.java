/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.table.TableColumnExt;

import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.CmOpenDatabases;
import com.asetune.gui.swing.GTableFilter;
import com.asetune.gui.swing.MultiLineLabel;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class DbSelectionForGraphsDialog
extends JDialog
implements ActionListener, TableModelListener
{
	private static Logger _logger = Logger.getLogger(DbSelectionForGraphsDialog.class);
	private static final long serialVersionUID = 1L;
	
	private final static String COLNAME_DBName         = "DBName";
	private final static String COLNAME_DbSizeInMb     = "DbSizeInMb";
	private final static String COLNAME_LogSizeUsedPct = "LogSizeUsedPct";

	private MultiLineLabel          _description1   = new MultiLineLabel("<html>Choose what databases that should included or exluded in attached graphs.</html>");
	private MultiLineLabel          _description2   = new MultiLineLabel("<html>In the '<b>Keep</b>' and '<b>Skip</b>' fields, you may use Java Regular Expression, for example '<code>syb.*</code>' to keep/skip databases that starts with 'syb'</html>");
	private MultiLineLabel          _description3   = new MultiLineLabel("<html>Below is a table which reflects what databases will be part of the attached graphs.<br>Clicking the columns '<b>Keep</b>' and '<b>Skip</b>' will add or remove records from the above text fields.</html>");

	private JLabel                  _keepDbsInGraphs_lbl               = new JLabel("Keep DB's");
	private JTextField              _keepDbsInGraphs_txt               = new JTextField();
	private JLabel                  _skipDbsInGraphs_lbl               = new JLabel("Skip DB's");
	private JTextField              _skipDbsInGraphs_txt               = new JTextField();
	private JLabel                  _skipDbsWithSizeLtInGraphs_lbl     = new JLabel("Skip DB's With Size Less Than");
//	private JFormattedTextField     _skipDbsWithSizeLtInGraphs_txt     = new JFormattedTextField(new DefaultFormatterFactory(new NumberFormatter()));
	private JTextField              _skipDbsWithSizeLtInGraphs_txt     = new JTextField();
	private JLabel                  _keepDbsWithPctUsageGtInGraphs_lbl = new JLabel("Keep DB's With Log Percent Usage Greater Than");
	private JTextField              _keepDbsWithPctUsageGtInGraphs_txt = new JTextField();

	// Used to check if "apply" should be visible or not.
	private String                  _save_keepDbsInGraphs               = "";
	private String                  _save_skipDbsInGraphs               = "";
	private String                  _save_skipDbsWithSizeLtInGraphs     = "";
	private String                  _save_keepDbsWithPctUsageGtInGraphs = "";

	// Used to set "to start order"
	private String                  _start_keepDbsInGraphs               = "";
	private String                  _start_skipDbsInGraphs               = "";
	private String                  _start_skipDbsWithSizeLtInGraphs     = "";
	private String                  _start_keepDbsWithPctUsageGtInGraphs = "";

	private JButton                 _toStartOrder   = new JButton("To Start Order");
	private JButton                 _clearSavedInfo = new JButton("Clear saved info");
	private DefaultTableModel       _tableModel     = null;
	private JXTable                 _table          = null;
	private GTableFilter            _tableFilter     = null;
	
	private JButton                 _ok             = new JButton("OK");
	private JButton                 _cancel         = new JButton("Cancel");
	private JButton                 _apply          = new JButton("Apply");
	private int                     _dialogReturnSt = JOptionPane.CANCEL_OPTION; //JOptionPane.CLOSED_OPTION;

	private CountersModel           _cm             = null;
	
	private enum TabPos {VisibleInGraphs, DbName, DbSizeInMb, LogSizeUsedPct, KeepDb, SkipDb, Description}; 

	private DbSelectionForGraphsDialog(Frame owner, CountersModel cm)
	{
		super(owner, "Databases to be included in Various Graphs", true);

		_cm = cm;
//		_modelOrder   = gTabbedPane.getModelTabOrder();
//		_orderAtStart = gTabbedPane.getTabOrder(true);

		initComponents();
		pack();

		// Try to fit all rows on the open window
		Dimension size = getSize();
		size.height += (_table.getRowCount() - 6) * 18; // lets say 6 rows is the default showed AND each row takes 18 pixels
		size.width = 812;
		
		size = SwingUtils.getSizeWithingScreenLimit(size.width, size.height, 10);

		setSize(size);
	}

	/**
	 * Show dialog which can include/exclude databases from being part of the displayed graphs. 
	 * @param owner
	 * @param dbs names of the databases
	 * @return JOptionPane.CANCEL_OPTION or JOptionPane.OK_OPTION
	 */
	public static int showDialog(Frame owner, CountersModel cm)
	{
		DbSelectionForGraphsDialog dialog = new DbSelectionForGraphsDialog(owner, cm);

		dialog.setLocationRelativeTo(owner);
		dialog.setVisible(true);
		dialog.dispose();
		
		return dialog._dialogReturnSt;
	}

	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
//	private ActionListener _actionListener = new ActionListener()
//	{
//		public void actionPerformed(ActionEvent actionevent)
//		{
//			checkForChanges();
//		}
//	};
	private KeyListener _keyListener = new KeyListener()
	{
		 // Changes in the fields are visible first when the key has been released.
		@Override public void keyPressed (KeyEvent keyevent) {}
		@Override public void keyTyped   (KeyEvent keyevent) {}
		@Override public void keyReleased(KeyEvent keyevent) { checkForChanges(); }
	};

	protected void initComponents() 
	{
		JPanel panel = new JPanel();
//		panel.setLayout(new MigLayout("insets 20 20","[][grow]",""));   // insets Top Left Bottom Right
		panel.setLayout(new MigLayout());   // insets Top Left Bottom Right

		String tooltip = 
			"<html>" +
			"Databases in the attached Graphs can be included or excluded<br>" +
			"Include a database from graphs is based on:" +
			"<ul>" +
			"    <li>Part of the 'keep' list</li>" +
			"    <li>Database size is <b>above</b> the 'skip size' limit</li>" +
			"    <li>Database Transaction Log Usage is <b>above</b> the Percent Threshold</li>" +
			"</ul>" +
			"Exclude a database from graphs is based on:" +
			"<ul>" +
			"    <li>Part of the 'skip' list</li>" +
			"    <li>Database size is <b>above</b> the 'skip size' limit</li>" +
			"</ul>" +
			"The above values can be manipulated in this dialog.<br>" +
			"Or by manipulaiting the below properties in the file '<code>"+Configuration.getInstance(Configuration.USER_CONF).getFilename()+"</code>'." +
			"<ul>" +
			"    <li>DB Keep List can be changed with the property <code>"+CmOpenDatabases.CM_NAME+"."+PROPERTY_keepDbsInGraphs+"=db1ToKeep, db2ToKeep...</code></li>" +
			"    <li>DB Skip List can be changed with the property <code>"+CmOpenDatabases.CM_NAME+"."+PROPERTY_skipDbsInGraphs+"=db1ToSkip, db2ToSkip...</code></li>" +
			"    <li>DB Size Limit can be changed with the property <code>"+CmOpenDatabases.CM_NAME+"."+PROPERTY_skipDbsWithSizeLtInGraphs+"=#mb</code></li>" +
			"    <li>DB Tranlog Usage Percent Limit can be changed with the property <code>"+CmOpenDatabases.CM_NAME+"."+PROPERTY_keepDbsWithPctUsageGtInGraphs+"=#pct</code></li>" +
			"</ul>" +
			"The default keep list is: <code>"+DEFAULT_keepDbsInGraphs+"</code><br>" +
			"The default skip list is: <code>"+DEFAULT_skipDbsInGraphs+"</code><br>" +
			"The default size limit is: <code>"+DEFAULT_skipDbsWithSizeLtInGraphs+"</code><br>" +
			"The default PercentUsage limit is: <code>"+DEFAULT_keepDbsWithPctUsageGtInGraphs+"</code><br>" +
			"<br>" +
			"Note: If you <b>always</b> want a database present in the graphs, add database to property <code>"+CmOpenDatabases.CM_NAME+"."+PROPERTY_keepDbsInGraphs+"=db1, db2...</code><br>" +
			"</html>";
		panel.setToolTipText(tooltip);

		panel.add(_description1, "grow, wrap");
		panel.add(_description2, "grow, wrap 10");

		panel.add(_keepDbsInGraphs_lbl,               "w 50px, split");
		panel.add(_keepDbsInGraphs_txt,               "push, grow, wrap");
		panel.add(_skipDbsInGraphs_lbl,               "w 50px, split");
		panel.add(_skipDbsInGraphs_txt,               "push, grow, wrap");
		panel.add(_skipDbsWithSizeLtInGraphs_lbl,     "w 235px, split");
		panel.add(_skipDbsWithSizeLtInGraphs_txt,     "push, grow, wrap");
		panel.add(_keepDbsWithPctUsageGtInGraphs_lbl, "w 235px, split");
		panel.add(_keepDbsWithPctUsageGtInGraphs_txt, "push, grow, wrap 10");

		panel.add(_toStartOrder,   "split");
		panel.add(_clearSavedInfo, "wrap 10");
		
		panel.add(_description3, "wrap");

		readPropsFromConfig();
		normalizeInputFields();
		setStartOrder();
		saveAfterApply();

		_table = createTable();
		_tableFilter = new GTableFilter(_table);

		JScrollPane jScrollPane = new JScrollPane();
		jScrollPane.setViewportView(_table);
		
		panel.add(_tableFilter, "growx, pushx, wrap");
		panel.add(jScrollPane, "span, grow, push, width 100%, height 100%, wrap");

		panel.add(createOkPanel(), "gap top 20, right");

		// Initial state for buttons
		_apply.setEnabled(false);
		
		setContentPane(panel);

		// ADD ACTIONS TO COMPONENTS
		_keepDbsInGraphs_txt              .addActionListener(this);
		_skipDbsInGraphs_txt              .addActionListener(this);
		_skipDbsWithSizeLtInGraphs_txt    .addActionListener(this);
		_keepDbsWithPctUsageGtInGraphs_txt.addActionListener(this);
		_toStartOrder                     .addActionListener(this);
		_clearSavedInfo                   .addActionListener(this);
		
		// enable/disable "apply" button if TXT fields chnages
		_keepDbsInGraphs_txt              .addKeyListener(_keyListener);
		_skipDbsInGraphs_txt              .addKeyListener(_keyListener);
		_skipDbsWithSizeLtInGraphs_txt    .addKeyListener(_keyListener);
		_keepDbsWithPctUsageGtInGraphs_txt.addKeyListener(_keyListener);
		
		// Set some tooltip
		tooltip = "<html>If you always want a database <b>included</b> in the graphs.<br>This is a comma separated list of database names.<br>Java Regular Expression can be used, most common usage would be 'xxx.*' to specify databases starting with 'xxx'.</html>";
		_keepDbsInGraphs_lbl.setToolTipText(tooltip);
		_keepDbsInGraphs_txt.setToolTipText(tooltip);

		tooltip = "<html>If you always want a database <b>excluded</b> from the graphs.<br>This is a comma separated list of database names.<br>Java Regular Expression can be used, most common usage would be 'xxx.*' to specify databases starting with 'xxx'.</html>";
		_skipDbsInGraphs_lbl.setToolTipText(tooltip);
		_skipDbsInGraphs_txt.setToolTipText(tooltip);

		tooltip = "<html>If you want to <b>exclude</b> databases with a size <b>less than</b> from the graphs.<br>This is specified in number of MB.</html>";
		_skipDbsWithSizeLtInGraphs_lbl.setToolTipText(tooltip);
		_skipDbsWithSizeLtInGraphs_txt.setToolTipText(tooltip);

		tooltip = "<html>If you want to <b>include</b> databases with a Log size usage in Percent <b>greater than</b> in the graphs.<br>This is specified in Percent usage of the transaction log.</html>";
		_keepDbsWithPctUsageGtInGraphs_lbl.setToolTipText(tooltip);
		_keepDbsWithPctUsageGtInGraphs_txt.setToolTipText(tooltip);

		_toStartOrder  .setToolTipText("Restore the \"selection\" it had when this dialog was opened.");
		_clearSavedInfo.setToolTipText("<html>" +
		                                   "Remove/clear the persisted values for \"selection\" and the visibility.<br>" +
		                                   "This is usually stored in the configuration or properties file.<br>" +
		                                   "<br>" +
		                                   "<b>Note:</b> The values are removed from the configuration file when this button is pushed.<br>" +
		                                   "This means that the \"apply\" or \"OK\" button is overridden." +
		                              "</html>");
		pack();
	}

	JPanel createOkPanel()
	{
		// ADD the OK, Cancel, Apply buttons
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0 0","",""));
		panel.add(_ok,     "tag ok");
		panel.add(_cancel, "tag cancel");
		panel.add(_apply,  "tag apply");
		
		_ok    .addActionListener(this);
		_cancel.addActionListener(this);
		_apply .addActionListener(this);

		return panel;
	}
	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/
	private void apply()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
			return;

		String keepDbsStr               = _keepDbsInGraphs_txt.getText();
		String skipDbsStr               = _skipDbsInGraphs_txt.getText();
		String skipDbsWithSizeLtStr     = _skipDbsWithSizeLtInGraphs_txt.getText();
		String keepDbsWithPctUsageGtStr = _keepDbsWithPctUsageGtInGraphs_txt.getText();

		String cmShortName = _cm.getName();

		// keepDbsInGraphs
		if (keepDbsStr.equals(DEFAULT_keepDbsInGraphs))
			conf.remove(cmShortName+"."+PROPERTY_keepDbsInGraphs);
		else
			conf.setProperty(cmShortName+"."+PROPERTY_keepDbsInGraphs, keepDbsStr);

		// skipDbsInGraphs
		if (skipDbsStr.equals(DEFAULT_skipDbsInGraphs))
			conf.remove(cmShortName+"."+PROPERTY_skipDbsInGraphs);
		else
			conf.setProperty(cmShortName+"."+PROPERTY_skipDbsInGraphs, skipDbsStr);

		// skipDbsWithSizeLtInGraphs
		if (skipDbsWithSizeLtStr.equals(DEFAULT_skipDbsWithSizeLtInGraphs))
			conf.remove(cmShortName+"."+PROPERTY_skipDbsWithSizeLtInGraphs);
		else
			conf.setProperty(cmShortName+"."+PROPERTY_skipDbsWithSizeLtInGraphs, skipDbsWithSizeLtStr);

		// keepDbsWithPctUsageGtInGraphs
		if (keepDbsWithPctUsageGtStr.equals(DEFAULT_keepDbsWithPctUsageGtInGraphs))
			conf.remove(cmShortName+"."+PROPERTY_keepDbsWithPctUsageGtInGraphs);
		else
			conf.setProperty(cmShortName+"."+PROPERTY_keepDbsWithPctUsageGtInGraphs, keepDbsWithPctUsageGtStr);

		conf.save();
		saveAfterApply();

		// update the graph panel
		if (_cm != null && _cm.getTabPanel() != null)
			_cm.getTabPanel().updateExtendedInfoPanel();
		
		_apply.setEnabled(false);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

		// --- FIELD: keepDbs ---
		if (_keepDbsInGraphs_txt.equals(source))
		{
			updateSelectionTable();
		}

		// --- FIELD: skipDbs ---
		if (_skipDbsInGraphs_txt.equals(source))
		{
			updateSelectionTable();
		}

		// --- FIELD: skipDbsWithSizeLt ---
		if (_skipDbsWithSizeLtInGraphs_txt.equals(source))
		{
			updateSelectionTable();
		}

		// --- FIELD:  ---
		if (_keepDbsWithPctUsageGtInGraphs_txt.equals(source))
		{
			updateSelectionTable();
		}

		// --- BUTTON: TO_START_ORDER ---
		if (_toStartOrder.equals(source))
		{
			_keepDbsInGraphs_txt              .setText(_start_keepDbsInGraphs);
			_skipDbsInGraphs_txt              .setText(_start_skipDbsInGraphs);
			_skipDbsWithSizeLtInGraphs_txt    .setText(_start_skipDbsWithSizeLtInGraphs);
			_keepDbsWithPctUsageGtInGraphs_txt.setText(_start_keepDbsWithPctUsageGtInGraphs);

			updateSelectionTable();
		}

		// --- BUTTON: REMOVE_TAB_VISIBILITY_AND_ORDER ---
		if (_clearSavedInfo.equals(source))
		{
			Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
			if (conf == null)
				return;

			String cmShortName = _cm.getName();
			conf.remove(cmShortName+"."+PROPERTY_keepDbsInGraphs);
			conf.remove(cmShortName+"."+PROPERTY_skipDbsInGraphs);
			conf.remove(cmShortName+"."+PROPERTY_skipDbsWithSizeLtInGraphs);
			conf.remove(cmShortName+"."+PROPERTY_keepDbsWithPctUsageGtInGraphs);
			conf.save();
			
			readPropsFromConfig();
			updateSelectionTable();
		}

		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			apply();
			_dialogReturnSt = JOptionPane.OK_OPTION;
			setVisible(false);
		}

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			_dialogReturnSt = JOptionPane.CANCEL_OPTION;
			setVisible(false);
		}

		// --- BUTTON: APPLY ---
		if (_apply.equals(source))
		{
			apply();
		}
	}

	private void setStartOrder()
	{
		_start_keepDbsInGraphs               = _keepDbsInGraphs_txt.getText();
		_start_skipDbsInGraphs               = _skipDbsInGraphs_txt.getText();
		_start_skipDbsWithSizeLtInGraphs     = _skipDbsWithSizeLtInGraphs_txt.getText();
		_start_keepDbsWithPctUsageGtInGraphs = _keepDbsWithPctUsageGtInGraphs_txt.getText();
	}
	
	private void saveAfterApply()
	{
		_save_keepDbsInGraphs               = _keepDbsInGraphs_txt.getText();
		_save_skipDbsInGraphs               = _skipDbsInGraphs_txt.getText();
		_save_skipDbsWithSizeLtInGraphs     = _skipDbsWithSizeLtInGraphs_txt.getText();
		_save_keepDbsWithPctUsageGtInGraphs = _keepDbsWithPctUsageGtInGraphs_txt.getText();
	}
	
	private void readPropsFromConfig()
	{
		String cmShortName = _cm.getName();
		String propName    = "";
		Configuration conf = Configuration.getCombinedConfiguration();

		// databases that should ALWAYS be part of the graphs 
		propName = cmShortName+"."+PROPERTY_keepDbsInGraphs;
		String keepDbsInGraphs = conf.getProperty(propName, DEFAULT_keepDbsInGraphs);

		// databases that should be left OUT in the graphs 
		propName = cmShortName+"."+PROPERTY_skipDbsInGraphs;
		String skipDbsInGraphs = conf.getProperty(propName, DEFAULT_skipDbsInGraphs);

		// databases size smaller than this should be left OUT in the graphs
		propName = cmShortName+"."+PROPERTY_skipDbsWithSizeLtInGraphs;
		int skipDbsWithSizeLtInGraphs = conf.getIntProperty(propName, DEFAULT_skipDbsWithSizeLtInGraphs);

		// 
		propName = cmShortName+"."+PROPERTY_keepDbsWithPctUsageGtInGraphs;
		int keepDbsWithPctUsageGtInGraphs = conf.getIntProperty(propName, DEFAULT_keepDbsWithPctUsageGtInGraphs);

		// set the GUI fields
		_keepDbsInGraphs_txt              .setText(keepDbsInGraphs);
		_skipDbsInGraphs_txt              .setText(skipDbsInGraphs);
		_skipDbsWithSizeLtInGraphs_txt    .setText(skipDbsWithSizeLtInGraphs+"");
		_keepDbsWithPctUsageGtInGraphs_txt.setText(keepDbsWithPctUsageGtInGraphs+"");
	}

	private void checkForChanges()
	{
		boolean enabled = false;

		if ( ! _save_keepDbsInGraphs.equals(_keepDbsInGraphs_txt.getText()) ) 
			enabled = true;

		if ( ! _save_skipDbsInGraphs.equals(_skipDbsInGraphs_txt.getText()) ) 
			enabled = true;

		if ( ! _save_skipDbsWithSizeLtInGraphs.equals(_skipDbsWithSizeLtInGraphs_txt.getText()) ) 
			enabled = true;
		
		if ( ! _save_keepDbsWithPctUsageGtInGraphs.equals(_keepDbsWithPctUsageGtInGraphs_txt.getText()) ) 
			enabled = true;
		
		_apply.setEnabled(enabled);
	}

	private void normalizeInputFields()
	{
		// TODO: in here we can add code to discard entries that ARE part of any regex 
		// for example if keep/skip is: 'syb.*, sybsystemprocs, sybxxx, sybyyy'
		// the we can discard: sybsystemprocs, sybxxx and sybyyy
		// databases that should ALWAYS be part of the graphs

//		String[] keepDbsInGraphs = StringUtil.commaStrToArray(_keepDbsInGraphs_txt.getText());
//		Set<String> keepDbsInGraphs = new LinkedHashSet<String>();
//		Set<String> x_keepDbsInGraphs = StringUtil.commaStrToSet(_keepDbsInGraphs_txt.getText());
//		for (String dbname : x_keepDbsInGraphs)
//		{
//			if ( ! StringUtil.matchesRegexSet(dbname, x_keepDbsInGraphs) )
//				keepDbsInGraphs.add(dbname);
//		}
//		_keepDbsInGraphs_txt.setText(StringUtil.toCommaStr(keepDbsInGraphs));
//		Set<String> keepDbsInGraphs = StringUtil.commaStrToSet(_keepDbsInGraphs_txt.getText());
//		for (String dbname : keepDbsInGraphs)
//			addKeepDb(dbname);
//		keepDbsInGraphs = StringUtil.commaStrToSet(_keepDbsInGraphs_txt.getText());
		
		// KEEP fields
		Set<String> keepDbsSet = StringUtil.commaStrToSet(_keepDbsInGraphs_txt.getText());
		_keepDbsInGraphs_txt.setText(StringUtil.toCommaStr(keepDbsSet));

		// SKIP fields 
		Set<String> skipDbsSet = StringUtil.commaStrToSet(_skipDbsInGraphs_txt.getText());
		_skipDbsInGraphs_txt.setText(StringUtil.toCommaStr(skipDbsSet));

		// Skip DBs with size less than
		int skipDbsWithSizeLt = DEFAULT_skipDbsWithSizeLtInGraphs;
		String numStr = _skipDbsWithSizeLtInGraphs_txt.getText();
		try 
		{
			skipDbsWithSizeLt = Integer.parseInt(numStr);
		}
		catch (NumberFormatException ignore) 
		{
			SwingUtils.showErrorMessage("Not a number", "Expected the size '"+numStr+"' to be a Number, resetting to default value of "+skipDbsWithSizeLt, ignore);
			_skipDbsWithSizeLtInGraphs_txt.setText(skipDbsWithSizeLt+"");
		}
		_skipDbsWithSizeLtInGraphs_txt.setText(skipDbsWithSizeLt+"");

		// Keep PCT
		int keepDbsWithPctUsageGt = DEFAULT_keepDbsWithPctUsageGtInGraphs;
		numStr = _keepDbsWithPctUsageGtInGraphs_txt.getText();
		try 
		{
			keepDbsWithPctUsageGt = Integer.parseInt(numStr);
		}
		catch (NumberFormatException ignore) 
		{
			SwingUtils.showErrorMessage("Not a number", "Expected the Percent Usage '"+numStr+"' to be a Number, resetting to default value of "+keepDbsWithPctUsageGt, ignore);
			_keepDbsWithPctUsageGtInGraphs_txt.setText(skipDbsWithSizeLt+"");
		}
		_keepDbsWithPctUsageGtInGraphs_txt.setText(keepDbsWithPctUsageGt+"");
	}

	private void updateSelectionTable()
	{
		normalizeInputFields();

		// databases that should ALWAYS be part of the graphs
		Set<String> keepDbsInGraphs = StringUtil.commaStrToSet(_keepDbsInGraphs_txt.getText());
		
		// databases that should be left OUT in the graphs
		Set<String> skipDbsInGraphs = StringUtil.commaStrToSet(_skipDbsInGraphs_txt.getText());

		// databases size smaller than this should be left OUT in the graphs
		int skipDbsWithSizeLtInGraphs = DEFAULT_skipDbsWithSizeLtInGraphs;
		String numStr = _skipDbsWithSizeLtInGraphs_txt.getText();
		try 
		{
			skipDbsWithSizeLtInGraphs = Integer.parseInt(numStr);
		}
		catch (NumberFormatException ignore) 
		{
			SwingUtils.showErrorMessage("Not a number", "Expected the size '"+numStr+"' to be a Number, resetting to default value of "+skipDbsWithSizeLtInGraphs, ignore);
			_skipDbsWithSizeLtInGraphs_txt.setText(skipDbsWithSizeLtInGraphs+"");
		}

		// Keep PCT
		int keepDbsWithPctUsageGtInGraphs = DEFAULT_keepDbsWithPctUsageGtInGraphs;
		numStr = _keepDbsWithPctUsageGtInGraphs_txt.getText();
		try 
		{
			keepDbsWithPctUsageGtInGraphs = Integer.parseInt(numStr);
		}
		catch (NumberFormatException ignore) 
		{
			SwingUtils.showErrorMessage("Not a number", "Expected the Percent Usage '"+numStr+"' to be a Number, resetting to default value of "+keepDbsWithPctUsageGtInGraphs, ignore);
			_keepDbsWithPctUsageGtInGraphs_txt.setText(keepDbsWithPctUsageGtInGraphs+"");
		}

		DefaultTableModel tm = _tableModel;
		if (tm == null)
		{
			_logger.error("_tableModel is null, can't continue.");
			return;
		}

		try
		{
			// Remove the listener while we manipulate rows in here.
			if (tm instanceof AbstractTableModel)
				((AbstractTableModel)tm).removeTableModelListener(this);
			
			for (int r=0; r<tm.getRowCount(); r++)
			{
				String reason            = "";
				String dbname            = tm.getValueAt(r, TabPos.DbName        .ordinal())+"";
				Object dbSizeInMbObj     = tm.getValueAt(r, TabPos.DbSizeInMb    .ordinal());
				Object logSizeUsedPctObj = tm.getValueAt(r, TabPos.LogSizeUsedPct.ordinal());
				int    dbSizeInMb        = -1;
				int    logSizeUsedPct    = -1;

				if (dbSizeInMbObj != null && dbSizeInMbObj instanceof Number)
					dbSizeInMb = ((Number)dbSizeInMbObj).intValue();
	
				if (logSizeUsedPctObj != null && logSizeUsedPctObj instanceof Number)
					logSizeUsedPct = ((Number)logSizeUsedPctObj).intValue();
	
				boolean visibleInGraphs = true;
				boolean skipFlag = false;
				boolean keepFlag = false;
	
				if (StringUtil.matchesRegexSet(dbname, skipDbsInGraphs))
				{
					visibleInGraphs = false;
					skipFlag        = true;
					reason         += "DB in the SKIP section, ";
				}
	
				// dbSizeInMb -1: column wasn't found
				if (dbSizeInMb != -1 && dbSizeInMb < skipDbsWithSizeLtInGraphs)
				{
					visibleInGraphs = false;
					reason         += "DB Size is below "+skipDbsWithSizeLtInGraphs+" MB, ";
				}
	
				// logSizeUsedPct -1: column wasn't found
				if (logSizeUsedPct != -1 && logSizeUsedPct >= keepDbsWithPctUsageGtInGraphs)
				{
					visibleInGraphs = true;
					reason         += "TranLog Usage is above "+keepDbsWithPctUsageGtInGraphs+"%, ";
				}
	
				if (StringUtil.matchesRegexSet(dbname, keepDbsInGraphs))
				{
					visibleInGraphs = true;
					keepFlag        = true;
					reason         += "DB in the KEEP section, ";
				}

				if (reason.length() > 0)
				{
					reason = (visibleInGraphs ? "<b>KEEP:</b> " : "<b>SKIP:</b> ") + reason;
					reason = StringUtil.removeLastComma(reason);
					reason = "<html>" + reason + "</html>";
				}
				else
					reason = "<html><b>KEEP:</b> Not in SKIP section and DB Size above "+skipDbsWithSizeLtInGraphs+" MB.</html>";
					
					
				tm.setValueAt(visibleInGraphs, r, TabPos.VisibleInGraphs.ordinal());
				tm.setValueAt(skipFlag,        r, TabPos.SkipDb         .ordinal());
				tm.setValueAt(keepFlag,        r, TabPos.KeepDb         .ordinal());
				tm.setValueAt(reason,          r, TabPos.Description    .ordinal());
			}
		}
		finally
		{
			// Install the listener again.
			if (tm instanceof AbstractTableModel)
				((AbstractTableModel)tm).addTableModelListener(this);
		}

		// enable/disable "apply"
		checkForChanges();
	}

	/* Called on fire* has been called on the TableModel */
	@Override
	public void tableChanged(final TableModelEvent e)
	{
		// We clicked some of the checkboxes
		if (e.getType() == TableModelEvent.UPDATE)
		{
			int row = e.getFirstRow();
			int col = e.getColumn();
			
			Object oVal    = _tableModel.getValueAt(row, col);
			String dbname = _tableModel.getValueAt(row, TabPos.DbName.ordinal())+"";
			Boolean bVal  = null;
			if (oVal instanceof Boolean)
				bVal = (Boolean) oVal;

			if (col == TabPos.KeepDb.ordinal())
				setKeepDb(dbname, bVal);

			if (col == TabPos.SkipDb.ordinal())
				setSkipDb(dbname, bVal);

			updateSelectionTable();
		}

		checkForChanges();
	}

	private void setKeepDb(String dbname, boolean add)
	{
		Set<String> dbSet = StringUtil.commaStrToSet(_keepDbsInGraphs_txt.getText());
		dbSet.remove(""); // if input str, was empty, still 1 entry will be in it

		// If the dbname is part of any regex in the other strings, then we are done
		if (add && StringUtil.matchesRegexSet(dbname, dbSet))
			return;

		if (add)
			dbSet.add(dbname);
		else
			dbSet.remove(dbname);

		_keepDbsInGraphs_txt.setText(StringUtil.toCommaStr(dbSet));

//		updateSelectionTable();
	}

	private void setSkipDb(String dbname, boolean add)
	{
		Set<String> dbSet = StringUtil.commaStrToSet(_skipDbsInGraphs_txt.getText());
		dbSet.remove(""); // if input str, was empty, still 1 entry will be in it

		// If the dbname is part of any regex in the other strings, then we are done
		if (add && StringUtil.matchesRegexSet(dbname, dbSet))
			return;

		if (add)
			dbSet.add(dbname);
		else
			dbSet.remove(dbname);

		_skipDbsInGraphs_txt.setText(StringUtil.toCommaStr(dbSet));

//		updateSelectionTable();
	}

	public JXTable createTable()
	{
		// Create a TABLE
		Vector<String> tabHead = new Vector<String>();
		tabHead.setSize(TabPos.values().length);
		tabHead.set(TabPos.VisibleInGraphs.ordinal(), "Visible In Graphs");
		tabHead.set(TabPos.DbName         .ordinal(), "DB Name");
		tabHead.set(TabPos.DbSizeInMb     .ordinal(), "DB Size");
		tabHead.set(TabPos.LogSizeUsedPct .ordinal(), "Log Usage");
		tabHead.set(TabPos.KeepDb         .ordinal(), "Keep");
		tabHead.set(TabPos.SkipDb         .ordinal(), "Skip");
		tabHead.set(TabPos.Description    .ordinal(), "Description");

		Vector<Vector<Object>> tabData = populateTable();

		//-----------------------------
		// CREATE MODEL
		//-----------------------------
		_tableModel = new DefaultTableModel(tabData, tabHead)
		{
            private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(int column) 
			{
				if      (column == TabPos.VisibleInGraphs.ordinal()) return Boolean.class;
				else if (column == TabPos.KeepDb         .ordinal()) return Boolean.class;
				else if (column == TabPos.SkipDb         .ordinal()) return Boolean.class;
				else if (column == TabPos.DbSizeInMb     .ordinal()) return Number.class;
				else if (column == TabPos.LogSizeUsedPct .ordinal()) return Number.class;
				else return Object.class;
			}
			@Override
			public boolean isCellEditable(int row, int col)
			{
				if (col == TabPos.KeepDb.ordinal()) return true;
				if (col == TabPos.SkipDb.ordinal()) return true;

				return false;
			}
		};
		_tableModel.addTableModelListener(this);

		//-----------------------------
		// CREATE JTABLE
		//-----------------------------
		JXTable table = new JXTable(_tableModel)
		{
			private static final long serialVersionUID = 1L;

			/** Enable/Disable + add some color to non changeable columns */
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
			{
				Component c = super.prepareRenderer(renderer, row, column);
				if (column == TabPos.VisibleInGraphs.ordinal())
					c.setEnabled( isCellEditable(row, column) );
//				c.setBackground(TAB_PCS_COL_BG);
				return c;
			}

			/** TABLE HEADER tool tip. */
			@Override
			protected JTableHeader createDefaultTableHeader()
			{
				JTableHeader tabHeader = new JXTableHeader(getColumnModel())
				{
	                private static final long serialVersionUID = 0L;

					@Override
					public String getToolTipText(MouseEvent e)
					{
						String tip = null;
						int col = getColumnModel().getColumnIndexAtX(e.getPoint().x);
						if (col < 0) return null;

						if      (col == TabPos.VisibleInGraphs.ordinal()) tip = "<b>ReadOnly:</b> Indicates that this database will be part of the selection.";
						else if (col == TabPos.DbName         .ordinal()) tip = "Name of the database to included or excluded";
						else if (col == TabPos.DbSizeInMb     .ordinal()) tip = "The full database size in MB";
						else if (col == TabPos.LogSizeUsedPct .ordinal()) tip = "The transaction log usage in Percent";
						else if (col == TabPos.KeepDb         .ordinal()) tip = "Click this to <b>include</b> this database in the selection.";
						else if (col == TabPos.SkipDb         .ordinal()) tip = "Click this to <b>exclude</b> this database in the selection. ";
						else if (col == TabPos.Description    .ordinal()) tip = "What is the cause why this database was included or not in the selection";

						if (tip == null)
							return null;
						return "<html>" + tip + "</html>";
					}
				};
				return tabHeader;
			}
		};
		table.addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				return (Boolean) adapter.getValue(TabPos.VisibleInGraphs.ordinal());
			}
		}, Color.GREEN, null));

//		table.addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				return ! (Boolean) adapter.getValue(TabPos.VisibleInGraphs.ordinal());
//			}
//		}, Color.RED, null));
		
		table.setSortable(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setShowGrid(false);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		updateSelectionTable();

		SwingUtils.calcColumnWidths(table);

//		TableColumnModelExt tcmx = (TableColumnModelExt)getColumnModel();
//		TableColumnExt tcx = tcmx.getColumnExt(colName);
//			if (tcx != null)
//			{
//				tcx.setPreferredWidth(colWidth);
//				tcx.setWidth(colWidth);
//			}
		TableColumnExt tcx = table.getColumnExt(TabPos.Description.ordinal());
		if (tcx != null)
		{
			// make the 'max' string fit in Description.
			// KEEP: DB in the SKIP section, DB Size is below ### MB, DB in the KEEP section
			tcx.setPreferredWidth(430);
			tcx.setWidth(430);
		}

		return table;
	}

	private Vector<Vector<Object>> populateTable()
	{
		Vector<Vector<Object>> tab = new Vector<Vector<Object>>();
		Vector<Object>         row = new Vector<Object>();

		CountersModel cm = _cm;

		Map<String, Integer> visibleDbMap = getDbsInGraphList(cm);

		for (int r=0; r<cm.getRowCount(); r++)
		{
			row = new Vector<Object>();
			row.setSize(TabPos.values().length);
			
			String dbname            = cm.getRateString(r, COLNAME_DBName);
			Object dbSizeInMbObj     = cm.getRateValue (r, COLNAME_DbSizeInMb);
			Object logSizeUsedPctObj = cm.getRateValue (r, COLNAME_LogSizeUsedPct);
			int    dbSizeInMb        = -1;
			int    logSizeUsedPct    = -1;

			if (dbSizeInMbObj != null && dbSizeInMbObj instanceof Number)
				dbSizeInMb = ((Number)dbSizeInMbObj).intValue();

			if (logSizeUsedPctObj != null && logSizeUsedPctObj instanceof Number)
				logSizeUsedPct = ((Number)logSizeUsedPctObj).intValue();

			boolean visibleInGraphs = visibleDbMap.containsKey(dbname);

			row.set(TabPos.VisibleInGraphs.ordinal(), visibleInGraphs);
			row.set(TabPos.DbName         .ordinal(), dbname);
			row.set(TabPos.DbSizeInMb     .ordinal(), dbSizeInMb);
			row.set(TabPos.LogSizeUsedPct .ordinal(), logSizeUsedPct);
			row.set(TabPos.KeepDb         .ordinal(), false);
			row.set(TabPos.SkipDb         .ordinal(), false);
			row.set(TabPos.Description    .ordinal(), "");

			tab.add(row);
		}

		return tab;
	}

	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	// TEST-CODE
	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	
	
	
	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	// MAIN 
	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	
	
	
	
	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	// STATIC METHODS, which acts as helpers....
	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	public final static String PROPERTY_keepDbsInGraphs               = "keepDbsInGraphs";
	public final static String PROPERTY_skipDbsInGraphs               = "skipDbsInGraphs";
	public final static String PROPERTY_skipDbsWithSizeLtInGraphs     = "skipDbsWithSizeLtInGraphs";
	public final static String PROPERTY_keepDbsWithPctUsageGtInGraphs = "keepDbsWithPctUsageGtInGraphs";

	public final static String DEFAULT_keepDbsInGraphs                = "";
//	public final static String DEFAULT_skipDbsInGraphs                = "master, model, pubs2, sybmgmtdb, sybpcidb, sybsecurity, sybsystemdb, sybsystemprocs";
	public final static String DEFAULT_skipDbsInGraphs                = "master, model, pubs2, sybmgmtdb, sybpcidb, sybsystemdb, sybsystemprocs"; // removed 'sybsecurity' if auditing is enabled, then this database is critical
	public final static int    DEFAULT_skipDbsWithSizeLtInGraphs      = 300;
	public final static int    DEFAULT_keepDbsWithPctUsageGtInGraphs  = 80;

	/**
	 * 
	 * @param cm
	 * @return
	 */
	public static Map<String, Integer> getDbsInGraphList(CountersModel cm)
	{
		String cmShortName = cm.getName();
		String propName    = "";
		Configuration conf = Configuration.getCombinedConfiguration();

		// databases that should ALWAYS be part of the graphs
		propName = cmShortName+"."+PROPERTY_keepDbsInGraphs;
		String[] keepDbsInGraphs = StringUtil.commaStrToArray(conf.getProperty(propName, DEFAULT_keepDbsInGraphs));

		// databases that should be left OUT in the graphs
		propName = cmShortName+"."+PROPERTY_skipDbsInGraphs;
		String[] skipDbsInGraphs = StringUtil.commaStrToArray(conf.getProperty(propName, DEFAULT_skipDbsInGraphs));

		// databases size smaller than this should be left OUT in the graphs
		propName = cmShortName+"."+PROPERTY_skipDbsWithSizeLtInGraphs;
		int skipDbsWithSizeLtInGraphs = conf.getIntProperty(propName, DEFAULT_skipDbsWithSizeLtInGraphs);
		
		// databases size smaller than this should be left OUT in the graphs
		propName = cmShortName+"."+PROPERTY_keepDbsWithPctUsageGtInGraphs;
		int keepDbsWithPctUsageGtInGraphs = conf.getIntProperty(propName, DEFAULT_keepDbsWithPctUsageGtInGraphs);

		return getDbsInGraphList(cm, keepDbsInGraphs, skipDbsInGraphs, skipDbsWithSizeLtInGraphs, keepDbsWithPctUsageGtInGraphs);
	}

	/**
	 * FIXME: this will probably crash if columns 'DBName' or 'DbSizeInMb' is not part of the CM
	 * @param cm
	 * @return
	 */
	public static Map<String, Integer> getDbsInGraphList(CountersModel cm, String[] keepDbsInGraphs, String[] skipDbsInGraphs, int skipDbsWithSizeLtInGraphs, int keepDbsWithPctUsageGtInGraphs)
	{

		// Filter out rows we do NOT want in the list
		// For rows we want to look at: put the row'ids in a list
		HashMap<String, Integer> dbMap = new HashMap<String, Integer>(); 

		for (int r=0; r<cm.size(); r++)
		{
			String dbname            = cm.getRateString(r, COLNAME_DBName);
			Object dbSizeInMbObj     = cm.getRateValue (r, COLNAME_DbSizeInMb);
			Object logSizeUsedPctObj = cm.getRateValue (r, COLNAME_LogSizeUsedPct);

			int    dbSizeInMb        = -1;
			int    logSizeUsedPct    = -1;

			if (dbSizeInMbObj != null && dbSizeInMbObj instanceof Number)
				dbSizeInMb = ((Number)dbSizeInMbObj).intValue();

			if (logSizeUsedPctObj != null && logSizeUsedPctObj instanceof Number)
				logSizeUsedPct = ((Number)logSizeUsedPctObj).intValue();

			boolean keepDb = true;

			if (StringUtil.matchesRegexArr(dbname, skipDbsInGraphs))
				keepDb = false;

			// dbSizeInMb -1: column wasn't found
			if (dbSizeInMb != -1 && dbSizeInMb < skipDbsWithSizeLtInGraphs)
				keepDb = false;

			// logSizeUsedPct -1: column wasn't found
			if (logSizeUsedPct != -1 && logSizeUsedPct >= keepDbsWithPctUsageGtInGraphs)
				keepDb = true;

			if (StringUtil.matchesRegexArr(dbname, keepDbsInGraphs))
				keepDb = true;
				
			if (keepDb)
				dbMap.put(dbname, r);
//System.out.println("getDbsInGraphList(cm='"+cm.getName()+"'): size="+cm.size()+", dbname='"+dbname+"', dbSizeInMb="+dbSizeInMb+", logSizeUsedPct="+logSizeUsedPct+". <<<<<< keepDb="+keepDb);
//System.out.println("getDbsInGraphList(cm='"+cm.getName()+"', keepDbsInGraphs="+StringUtil.toCommaStr(keepDbsInGraphs)+", skipDbsInGraphs="+StringUtil.toCommaStr(skipDbsInGraphs)+", skipDbsWithSizeLtInGraphs="+skipDbsWithSizeLtInGraphs+", keepDbsWithPctUsageGtInGraphs="+keepDbsWithPctUsageGtInGraphs+"): dbname='"+dbname+"', dbSizeInMb="+dbSizeInMb+", logSizeUsedPct="+logSizeUsedPct+". <<<<<< keepDb="+keepDb);
		}

		return dbMap;
	}

//	/**
//	 * set the configuration 'cmName.keepDbsInGraphs=db1, db2, db3' etc...
//	 * @param cmShortName
//	 * @param dbList
//	 */
//	public static void saveKeepDbsInGraphs(String cmShortName, Collection<String> dbList)
//	{
//		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
//		if (conf == null)
//			return;
//
//		conf.setProperty(cmShortName+"."+PROPERTY_keepDbsInGraphs, StringUtil.toCommaStr(dbList));
//		conf.save();
//	}

}

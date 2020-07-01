/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.tools.sqlw;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.apache.log4j.Logger;

import com.asetune.gui.swing.GInputValidationGroup;
import com.asetune.gui.swing.GInputValidator;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class ResultSetJXTableSettingsDialog
extends JDialog
implements ActionListener //, FocusListener
{
	private static Logger _logger = Logger.getLogger(ResultSetJXTableSettingsDialog.class);
	private static final long serialVersionUID = 1L;

	private Window             _owner                      = null;

	private JCheckBox          _tt_showAllCols_chk         = new JCheckBox("Show all cells for current row in the ToolTip as a 'table'", false);

	private JLabel             _tt_xmlInlineMaxSize_lbl    = new JLabel("Max Size in KB for XML Inline");
	private JTextField         _tt_xmlInlineMaxSize_txt    = new JTextField();

	private JLabel             _tt_cellDisplMaxSize_lbl    = new JLabel("Max Size in KB for Cell Content");
	private JTextField         _tt_cellDisplMaxSize_txt    = new JTextField("");

	private JLabel             _r_timestamp_lbl            = new JLabel("Timestamp");
	private JTextField         _r_timestamp_txt            = new JTextField("");

	private JLabel             _r_date_lbl                 = new JLabel("Date");
	private JTextField         _r_date_txt                 = new JTextField("");

	private JLabel             _r_time_lbl                 = new JLabel("Time");
	private JTextField         _r_time_txt                 = new JTextField("");

	private JLabel             _r_maxWidth_lbl             = new JLabel("Max Visible Width");
	private JTextField         _r_maxWidth_txt             = new JTextField("");

	private JLabel             _r_minNumberDecimal_lbl   = new JLabel("Min Decimals on Numbers");
	private JTextField         _r_minNumberDecimal_txt   = new JTextField("");

	private JLabel             _r_maxNumberDecimal_lbl   = new JLabel("Max Decimals on Numbers");
	private JTextField         _r_maxNumberDecimal_txt   = new JTextField("");

	private JButton            _ok                       = new JButton("OK");
	private JButton            _cancel                   = new JButton("Cancel");
//	private JLabel             _warning_lbl              = new JLabel("Warning messages, goes in here");
	private JLabel             _warning_lbl              = new JLabel("");

	private JPanel             _infoPanel                = null;
	private JPanel             _ttPanel                  = null;
	private JPanel             _renderPanel              = null;
	private JLabel             _reboot_lbl               = new JLabel();

	private GInputValidationGroup _vg                    = new GInputValidationGroup(_ok);

	private int                _dialogReturnSt           = JOptionPane.CANCEL_OPTION; //JOptionPane.CLOSED_OPTION;
	

	public static int showDialog(Frame owner)
	{
		ResultSetJXTableSettingsDialog dialog = new ResultSetJXTableSettingsDialog(owner);

		dialog.setLocationRelativeTo(owner);
		dialog.setVisible(true);
		dialog.dispose();
		
		return dialog._dialogReturnSt;
	}
	public ResultSetJXTableSettingsDialog(Frame owner)
	{
		super(owner, "ResulSet Table Properties", true);

		_owner        = owner;
		
		init();
	}

	private void initValues()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		
		// ToolTip
		_tt_showAllCols_chk.setSelected(conf.getBooleanProperty(ResultSetJXTable.PROPKEY_TABLE_TOOLTIP_SHOW_ALL_COLUMNS, ResultSetJXTable.DEFAULT_TABLE_TOOLTIP_SHOW_ALL_COLUMNS));

		_tt_xmlInlineMaxSize_txt.setText("" + conf.getIntProperty(ResultSetJXTable.PROPKEY_TOOLTIP_XML_INLINE_MAX_SIZE_KB  , ResultSetJXTable.DEFAULT_TOOLTIP_XML_INLINE_MAX_SIZE_KB));
		_tt_cellDisplMaxSize_txt.setText("" + conf.getIntProperty(ResultSetJXTable.PROPKEY_TOOLTIP_CELL_DISPLAY_MAX_SIZE_KB, ResultSetJXTable.DEFAULT_TOOLTIP_CELL_DISPLAY_MAX_SIZE_KB));

	
		// Renders
		_r_timestamp_txt       .setText(     conf.getProperty   (ResultSetJXTable.PROPKEY_TABLE_CELL_RENDERER_TIMESTAMP          , ResultSetJXTable.DEFAULT_TABLE_CELL_RENDERER_TIMESTAMP          ));
		_r_date_txt            .setText(     conf.getProperty   (ResultSetJXTable.PROPKEY_TABLE_CELL_RENDERER_DATE               , ResultSetJXTable.DEFAULT_TABLE_CELL_RENDERER_DATE               ));
		_r_time_txt            .setText(     conf.getProperty   (ResultSetJXTable.PROPKEY_TABLE_CELL_RENDERER_TIME               , ResultSetJXTable.DEFAULT_TABLE_CELL_RENDERER_TIME               ));
		_r_minNumberDecimal_txt.setText("" + conf.getIntProperty(ResultSetJXTable.PROPKEY_TABLE_CELL_RENDERER_MIN_NUMBER_DECIMALS, ResultSetJXTable.DEFAULT_TABLE_CELL_RENDERER_MIN_NUMBER_DECIMALS));
		_r_maxNumberDecimal_txt.setText("" + conf.getIntProperty(ResultSetJXTable.PROPKEY_TABLE_CELL_RENDERER_MAX_NUMBER_DECIMALS, ResultSetJXTable.DEFAULT_TABLE_CELL_RENDERER_MAX_NUMBER_DECIMALS));
		_r_maxWidth_txt        .setText("" + conf.getIntProperty(ResultSetJXTable.PROPKEY_TABLE_CELL_MAX_PREFERRED_WIDTH         , ResultSetJXTable.DEFAULT_TABLE_CELL_MAX_PREFERRED_WIDTH         ));
	}

	private void init()
	{
		setContentPane(createPanel());
		initValues();
		loadProps();
		
		pack();
//		SwingUtils.setLocationCenterParentWindow(null, this);
		setLocationRelativeTo(_owner);

		// Window size can not be "smaller" than the minimum size
		// If so "OK" button etc will be hidden.
		SwingUtils.setWindowMinSize(this);
	}

	private JPanel createPanel()
	{
		JPanel panel = new JPanel();
		// panel.setLayout(new MigLayout("","","")); // insets Top Left Bottom
		// Right
//		panel.setLayout(new MigLayout("insets 10 10", "", "")); // insets Top Left Bottom Right
		panel.setLayout(new MigLayout());

		_warning_lbl  .setToolTipText("Why we can't press OK.");
		_warning_lbl  .setForeground(Color.RED);
		_warning_lbl  .setHorizontalAlignment(SwingConstants.RIGHT);
		_warning_lbl  .setVerticalAlignment(SwingConstants.BOTTOM);

		// Add panel
		_infoPanel    = createInfoPanel();
		_ttPanel      = createToolTipPanel();
		_renderPanel  = createRendererPanel();
		
		panel.add(_renderPanel, "push, grow, wrap");
		panel.add(_ttPanel,     "push, grow, wrap");
		panel.add(_infoPanel,   "push, grow, wrap");

		// ADD the OK, Cancel, Apply buttons
		panel.add(_warning_lbl,  "split, pushx, growx");
		panel.add(_ok,           "tag ok,     split, bottom, right, push");
		panel.add(_cancel,       "tag cancel, split, bottom");

		// ADD ACTIONS TO COMPONENTS
		_ok    .addActionListener(this);
		_cancel.addActionListener(this);

		return panel;
	}
	

	private JPanel createInfoPanel()
	{
		JPanel panel = SwingUtils.createPanel("Info", false);
		panel.setLayout(new MigLayout());

		_reboot_lbl .setText("The changes will not take effect on already created tables, only on new.");

		panel.add(_reboot_lbl,        "growx, pushx, wrap");
		_reboot_lbl.setForeground(Color.RED);

		return panel;
	}

	private JPanel createToolTipPanel()
	{
		JPanel panel = SwingUtils.createPanel("ToolTip Settings", true);
		panel.setLayout(new MigLayout());

		_tt_showAllCols_chk.setToolTipText("Show all cells for this row in the ToolTip as a 'table'. Values will be full in full length (for numbers, no decimal roundings");

		_tt_xmlInlineMaxSize_lbl.setToolTipText("How many KB do we want to show in the ToolTip");
		_tt_xmlInlineMaxSize_txt.setToolTipText(_tt_xmlInlineMaxSize_lbl.getToolTipText());

		_tt_cellDisplMaxSize_lbl.setToolTipText("How many KB do we want to show in the ToolTip");
		_tt_cellDisplMaxSize_txt.setToolTipText(_tt_cellDisplMaxSize_lbl.getToolTipText());

		new GInputValidator(_tt_xmlInlineMaxSize_txt, _vg, new GInputValidator.IntegerInputValidator());
		new GInputValidator(_tt_cellDisplMaxSize_txt, _vg, new GInputValidator.IntegerInputValidator());

		panel.add(_tt_showAllCols_chk, "span 2, wrap");
		
		panel.add(_tt_xmlInlineMaxSize_lbl, "");
		panel.add(_tt_xmlInlineMaxSize_txt, "pushx, growx, wrap");

		panel.add(_tt_cellDisplMaxSize_lbl, "");
		panel.add(_tt_cellDisplMaxSize_txt, "pushx, growx, wrap");

//		_tt_xmlInlineMaxSize_txt.addFocusListener(this);
//		_tt_cellDisplMaxSize_txt.addFocusListener(this);

		return panel;
	}

	private JPanel createRendererPanel()
	{
		JPanel panel = SwingUtils.createPanel("Content Rendering Settings", true);
		panel.setLayout(new MigLayout());

		_r_minNumberDecimal_lbl.setToolTipText("Minumum decimals we show on numbers");
		_r_minNumberDecimal_txt.setToolTipText(_r_minNumberDecimal_lbl.getToolTipText());
		
		_r_maxNumberDecimal_lbl.setToolTipText("Maximum decimals we show on numbers");
		_r_maxNumberDecimal_txt.setToolTipText(_r_maxNumberDecimal_lbl.getToolTipText());
		
		_r_timestamp_lbl.setToolTipText("How should a java.sql.Timestamp be presented. See SimpleDateFormat");
		_r_timestamp_txt.setToolTipText(_r_timestamp_lbl.getToolTipText());
		
		_r_date_lbl.setToolTipText("How should a java.sql.Date be presented. See SimpleDateFormat");
		_r_date_txt.setToolTipText(_r_date_lbl.getToolTipText());
		
		_r_time_lbl.setToolTipText("How should a java.sql.Time be presented. See SimpleDateFormat");
		_r_time_txt.setToolTipText(_r_time_lbl.getToolTipText());
		
		_r_maxWidth_lbl.setToolTipText("The preferred Max Width (in Pixels) this cell be in");
		_r_maxWidth_txt.setToolTipText(_r_maxWidth_lbl.getToolTipText());
		
		new GInputValidator(_r_minNumberDecimal_txt, _vg, new GInputValidator.IntegerInputValidator());
		new GInputValidator(_r_maxNumberDecimal_txt, _vg, new GInputValidator.IntegerInputValidator());
		new GInputValidator(_r_timestamp_txt       , _vg, new GInputValidator.SimpleDateFormatInputValidator());
		new GInputValidator(_r_date_txt            , _vg, new GInputValidator.SimpleDateFormatInputValidator());
		new GInputValidator(_r_time_txt            , _vg, new GInputValidator.SimpleDateFormatInputValidator());
		new GInputValidator(_r_maxWidth_txt        , _vg, new GInputValidator.IntegerInputValidator());

		panel.add(_r_minNumberDecimal_lbl, "");
		panel.add(_r_minNumberDecimal_txt, "pushx, growx, wrap");

		panel.add(_r_maxNumberDecimal_lbl, "");
		panel.add(_r_maxNumberDecimal_txt, "pushx, growx, wrap");

		panel.add(_r_timestamp_lbl,        "");
		panel.add(_r_timestamp_txt,        "pushx, growx, wrap");
                                        
		panel.add(_r_date_lbl,             "");
		panel.add(_r_date_txt,             "pushx, growx, wrap");
                                        
		panel.add(_r_time_lbl,             "");
		panel.add(_r_time_txt,             "pushx, growx, wrap");

		panel.add(_r_maxWidth_lbl,         "");
		panel.add(_r_maxWidth_txt,         "pushx, growx, wrap");

		
//		_r_numberDecimal_txt.addFocusListener(this);
//		_r_timestamp_txt    .addFocusListener(this);
//		_r_date_txt         .addFocusListener(this);
//		_r_time_txt         .addFocusListener(this);
		
		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e==null ? null : e.getSource();

		// BUT: OK
		if (_ok.equals(source))
		{
			saveProps();
			_dialogReturnSt = JOptionPane.OK_OPTION;
			setVisible(false);
		}
		
		// BUT: CANCEL
		if (_cancel.equals(source))
		{
			_dialogReturnSt = JOptionPane.CANCEL_OPTION;
			setVisible(false);
		}
		
//		validateInput();
	}

	
//	@Override
//	public void focusGained(FocusEvent e)
//	{
//		validateInput();
//	}

//	@Override
//	public void focusLost(FocusEvent e)
//	{
//		validateInput();
//	}
	
//	private boolean validateInput()
//	{
//		String warn = "";
//
//		if ( ! _tt_cellDisplMaxSize_txt.getText().matches("[0-9]+")) warn = "Field '" + _tt_cellDisplMaxSize_lbl.getText() + "' can only contain numbers";
//		if ( ! _tt_xmlInlineMaxSize_txt.getText().matches("[0-9]+")) warn = "Field '" + _tt_xmlInlineMaxSize_lbl.getText() + "' can only contain numbers";
//
//		if ( ! _r_numberDecimal_txt.getText().matches("[0-9]+")    ) warn = "Field '" + _r_numberDecimal_lbl    .getText() + "' can only contain numbers";
//
//		_warning_lbl.setText(warn);
//
//		boolean ok = StringUtil.isNullOrBlank(warn);
//		_ok.setEnabled(ok);
//
//		return ok;
//	}

//	@Override
//	public void groupStateChanged(boolean groupIsValid)
//	{
//		_ok.setEnabled(groupIsValid);
//	}

	
	private void saveProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
			return;

		// ToolTip
		conf.setProperty(ResultSetJXTable.PROPKEY_TABLE_TOOLTIP_SHOW_ALL_COLUMNS,   _tt_showAllCols_chk     .isSelected());
		conf.setProperty(ResultSetJXTable.PROPKEY_TOOLTIP_CELL_DISPLAY_MAX_SIZE_KB, _tt_cellDisplMaxSize_txt.getText());
		conf.setProperty(ResultSetJXTable.PROPKEY_TOOLTIP_XML_INLINE_MAX_SIZE_KB,   _tt_xmlInlineMaxSize_txt.getText());
		
		// Rendering
		conf.setProperty(ResultSetJXTable.PROPKEY_TABLE_CELL_RENDERER_TIMESTAMP,           _r_timestamp_txt       .getText());
		conf.setProperty(ResultSetJXTable.PROPKEY_TABLE_CELL_RENDERER_DATE,                _r_time_txt            .getText());
		conf.setProperty(ResultSetJXTable.PROPKEY_TABLE_CELL_RENDERER_TIME,                _r_date_txt            .getText());
		conf.setProperty(ResultSetJXTable.PROPKEY_TABLE_CELL_RENDERER_MIN_NUMBER_DECIMALS, _r_minNumberDecimal_txt.getText());
		conf.setProperty(ResultSetJXTable.PROPKEY_TABLE_CELL_RENDERER_MAX_NUMBER_DECIMALS, _r_maxNumberDecimal_txt.getText());
		conf.setProperty(ResultSetJXTable.PROPKEY_TABLE_CELL_MAX_PREFERRED_WIDTH,          _r_maxWidth_txt        .getText());

		conf.save();
	}

	private void loadProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
			return;

		// first: LOAD for this specific server.db, then use FALLBACK "last used"
		String str;
		
		// USER
//		str = conf.getProperty("rs.connection."+_name+".ra.username");
//		if (str == null)
//			str = conf.getProperty("rs.connection.ra.username", "sa");
//		_raUsername_txt.setText(str);
//
//		// PASSWORD
//		str = conf.getProperty("rs.connection."+_name+".ra.password");
//		if (str == null)
//			str = conf.getProperty("rs.connection.ra.password", "");
//		_raPassword_txt.setText(str);
//
//		// SAVE PASSWORD
//		str = conf.getProperty("rs.connection."+_name+".ra.savePassword");
//		if (str == null)
//			str = conf.getProperty("rs.connection.ra.savePassword", "true");
//		_raPassword_chk.setSelected(Boolean.parseBoolean(str));
	}
}

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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
import java.awt.Font;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.Version;
import com.asetune.sql.pipe.PipeCommandGraph;
import com.asetune.sql.pipe.PipeCommandGraph.GraphType;
import com.asetune.tools.sqlw.msg.JGraphResultSet;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CreateGraphDialog
extends JDialog
implements ActionListener, TableModelListener, FocusListener, KeyListener
{
//	private static Logger _logger = Logger.getLogger(CreateGraphDialog.class);
	private static final long	serialVersionUID	= -1L;

	private static final String DIALOG_TITLE = "Create Graph or Chart";
	
	private static String[] SDF_EXAMPLES = new String[] {"", "HH:mm", "yyyy-MM-dd", "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss.SSSZ"};
	@SuppressWarnings("unused")
	private Window                  _owner           = null;

	private ResultSetTableModel     _rstm;

	private JPanel                   _topPanel;
	private JPanel                   _okCancelPanelPanel;

	
	private JLabel                   _graphType_swl      = new JLabel("--type");
	private JLabel                   _graphType_lbl      = new JLabel("Type of Graph/Chart to Create");
	private JComboBox<GraphType>     _graphType_cbx      = new JComboBox<>(GraphType.values());
	
	private JLabel                   _data_swl           = new JLabel("--data");
	private JCheckBox                _data_chk           = new JCheckBox("Show Source Data in Output (Just for building the cmdline, data will NOT show in Window)", false);
	
	private JLabel                   _pivot_swl          = new JLabel("--pivot");
	private JCheckBox                _pivot_chk          = new JCheckBox("Pivot, or try to Rotate the Data in the Source Table", false);

	private JLabel                   _3d_swl             = new JLabel("--3d");
	private JCheckBox                _3d_chk             = new JCheckBox("Use 3D Graph or Chart (same data just a 3D presentation)", false);

	private JLabel                   _graphName_swl      = new JLabel("--name");
	private JLabel                   _graphName_lbl      = new JLabel("Title Name of the Graph or Chart");
	private JTextField               _graphName_txt      = new JTextField("");
	
	private JLabel                   _labelCategory_swl  = new JLabel("--labelCategory");
	private JLabel                   _labelCategory_lbl  = new JLabel("Name for the Categories Axis (Below)");
	private JTextField               _labelCategory_txt  = new JTextField("");
	
	private JLabel                   _labelValue_swl     = new JLabel("--labelValue");
	private JLabel                   _labelValue_lbl     = new JLabel("Name for the Value Axis (Left side)");
	private JTextField               _labelValue_txt     = new JTextField("");

	private JLabel                   _rotateCatLab_swl   = new JLabel("--rotateCategoryLabels");
	private JLabel                   _rotateCatLab_lbl   = new JLabel("Rotate Category Labels");
//	private JCheckBox                _rotateCatLab_chk   = new JCheckBox("Rotate Category Labels", false);
	private JTextField               _rotateCatLab_txt   = new JTextField("");

	private JLabel                   _keySdf_swl         = new JLabel("--keySimpleDateFormat");
	private JLabel                   _keySdf_lbl         = new JLabel("Format key date with SimpleDateFormat");
//	private JTextField               _keySdf_txt         = new JTextField("");
	private JComboBox<String>        _keySdf_cbx         = new JComboBox<>(SDF_EXAMPLES);

	private JLabel                   _groupByKeySdf_swl  = new JLabel("--groupByKeySdf");
	private JCheckBox                _groupByKeySdf_chk  = new JCheckBox("Group values by the --keySimpleDateFormat.", false);

	private JLabel                   _str2num_swl        = new JLabel("--str2num");
	private JCheckBox                _str2num_chk        = new JCheckBox("Try to Convert 'string' Columns to Numbers (use --removeRegEx to remove some chars)", false);

	private JLabel                   _removeRegEx_swl    = new JLabel("--removeRegEx");
	private JLabel                   _removeRegEx_lbl    = new JLabel("Regex to Remove chars from String fields");
	private JTextField               _removeRegEx_txt    = new JTextField("");

	private JLabel                   _layoutWidth_swl    = new JLabel("--width");
	private JLabel                   _layoutWidth_lbl    = new JLabel("Layout Width");
	private JTextField               _layoutWidth_txt    = new JTextField("");
	
	private JLabel                   _layoutHeight_swl   = new JLabel("--height");
	private JLabel                   _layoutHeight_lbl   = new JLabel("Layout Height");
	private JTextField               _layoutHeight_txt   = new JTextField("");
	
	private JLabel                   _showDataValues_swl = new JLabel("--showDataValues");
	private JCheckBox                _showDataValues_chk = new JCheckBox("Show Data Values in graphs (easier to see data values)", false);

	private JLabel                   _showShapes_swl     = new JLabel("--showShapes");
	private JCheckBox                _showShapes_chk     = new JCheckBox("Show Shapes/boxes on data points (easier see data points in smaller datasets)", false);

	private JLabel                   _inWindow_swl       = new JLabel("--window");
	private JCheckBox                _inWindow_chk       = new JCheckBox("Show in it's own Window (Just for building the cmdline, this dialog will always create a Window)", false);

	private JLabel                   _debug_swl          = new JLabel("--debug");
	private JCheckBox                _debug_chk          = new JCheckBox("Debug Option (print what's done)", false);

	private JLabel                   _feedback_lbl       = new JLabel("SQL Window Command, If you want to present a Graph or Chart directly from the SQL Prompt, use the below 'go' command");
	private RSyntaxTextArea          _feedback_txt       = new RSyntaxTextArea(4, 50);
	private RTextScrollPane          _feedback_scroll    = new RTextScrollPane(_feedback_txt);

	private JLabel                   _tableKey_swl       = new JLabel("--keyCols");
	private JLabel                   _tableKey_lbl       = new JLabel("Select Key Columns from the below Table");
	
	private JLabel                   _tableGrp_swl       = new JLabel("--groupCols");
	private JLabel                   _tableGrp_lbl       = new JLabel("Select Group Columns from the below Table");
	
	private JLabel                   _tableVal_swl       = new JLabel("--valCols");
	private JLabel                   _tableVal_lbl       = new JLabel("Select Data/Value Columns from the below Table");

	private LocalTableModel          _tm; 
	private LocalTable               _table;

	
	private JButton          _apply         = new JButton("Open in Window Now");
	private JButton          _ok            = new JButton("OK");
	private JButton          _cancel        = new JButton("Cancel");
	
	private CreateGraphDialog(Window owner, ResultSetTableModel rstm)
	{
//		super(owner, DIALOG_TITLE, ModalityType.DOCUMENT_MODAL);
		super(owner, DIALOG_TITLE, ModalityType.MODELESS);
		_owner = owner;
		_rstm  = rstm;

		initComponents();
	}

	public static void showDialog(Window owner, ResultSetTableModel rstm)
	{
		CreateGraphDialog dialog = new CreateGraphDialog(owner, rstm);
		dialog.setLocationRelativeTo(owner);
		dialog.setVisible(true);
//		dialog.dispose();
//		return dialog._madeChanges;
		
		return;
	}

	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	public static ImageIcon getIcon16() { return SwingUtils.readImageIcon(Version.class, "images/create_graph_16.png"); }
	public static ImageIcon getIcon32() { return SwingUtils.readImageIcon(Version.class, "images/create_graph_32.png"); }

	protected void initComponents()
	{
//		super(_owner);
//		if (_owner != null)
//			setIconImage(_owner.getIconImage());

//		setTitle(DIALOG_TITLE);

		// Set the icon, if we "just" do setIconImage() on the JDialog
		// it will not be the "correct" icon in the Alt-Tab list on Windows
		// So we need to grab the owner, and set that since the icon is grabbed from the owner...
		ImageIcon icon16 = getIcon16();
		ImageIcon icon32 = getIcon32();
		if (icon16 != null || icon32 != null)
		{
			ArrayList<Image> iconList = new ArrayList<Image>();
			if (icon16 != null) iconList.add(icon16.getImage());
			if (icon32 != null) iconList.add(icon32.getImage());

			setIconImages(iconList);
		}

		
		JPanel panel = new JPanel();
		//panel.setLayout(new MigLayout("debug, insets 0 0 0 0, wrap 1","",""));   // insets Top Left Bottom Right
		panel.setLayout(new MigLayout("insets 0","",""));   // insets Top Left Bottom Right

		_topPanel           = createTopPanel();
		_okCancelPanelPanel = createOkCancelPanel();

		panel.add(_topPanel,           "gapbottom 0, grow, push, wrap");
		
		panel.add(_okCancelPanelPanel, "gaptop 0, growx, pushx, wrap");


		loadProps();

//		Do We Need To Use a BorderLayout ???
//		JPanel xxx = new JPanel(new BorderLayout());
//		xxx.add(panel, BorderLayout.CENTER);

		setContentPane(panel);
//		setContentPane(xxx);

//		initComponentActions();
		
		buildFeedback();

		pack();
		setFocus(_cancel);
	}

	private void setFocus(final JComponent comp)
	{
		// The components needs to be visible for the requestFocus()
		// to work, so lets the EventThreda do it for us after the windows is visible.
		Runnable deferredAction = new Runnable()
		{
			@Override
			public void run()
			{
				comp.requestFocus();
			}
		};
		SwingUtilities.invokeLater(deferredAction);
	}

	private JPanel createTopPanel()
	{
		JPanel panel = SwingUtils.createPanel("Options", false);
//		panel.setLayout(new MigLayout("insets 0 0 0 0", "", ""));
		panel.setLayout(new MigLayout("", "", ""));

//		panel.setToolTipText("<html>" +
//				"Templates will help you to set/restore values in the below table...<br>" +
//				"<UL>" +
//				"<li> To <b>Load</b> a template, just choose one from the drop down list. </li>" +
//				"<li> To <b>Save</b> current settings as a template, just press the 'Save..' button and choose a name to save as. </li>" +
//				"<li> To <b>Remove</b> a template, just press the 'Remove..' button and button and choose a template name to deleted. </li>" +
//				"</UL>" +
//				"If all selected values in the table is matching a template, that template name will be displayed in the drop down list.<br>" +
//				"<br>" +
//				"<b>Note:</b> User Defined Counters is <b>not</b> saved/restored in the templates, only <i>System Performance Counters</i><br>" +
//				"</html>");


		Font f = _graphType_lbl.getFont();
		// bold
		_graphType_swl     .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_data_swl          .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_pivot_swl         .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_3d_swl            .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_graphName_swl     .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_labelCategory_swl .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_labelValue_swl    .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_rotateCatLab_swl  .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_keySdf_swl        .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_groupByKeySdf_swl .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_str2num_swl       .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_removeRegEx_swl   .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_layoutWidth_swl   .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_layoutHeight_swl  .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_showDataValues_swl.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_showShapes_swl    .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_inWindow_swl      .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_debug_swl         .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_tableKey_swl      .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_tableGrp_swl      .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_tableVal_swl      .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		
		_feedback_lbl      .setFont(f.deriveFont(f.getStyle() | Font.BOLD));

		panel.add(_graphType_swl     , "");
		panel.add(_graphType_lbl     , "");
		panel.add(_graphType_cbx     , "wrap");
		
		panel.add(_data_swl          , "");
		panel.add(_data_chk          , "skip 1, wrap");

		panel.add(_pivot_swl         , "");
		panel.add(_pivot_chk         , "skip 1, wrap");

		panel.add(_3d_swl            , "");
		panel.add(_3d_chk            , "skip 1, wrap");

		panel.add(_graphName_swl     , "");
		panel.add(_graphName_lbl     , "");
		panel.add(_graphName_txt     , "growx, wrap");

		panel.add(_labelCategory_swl , "");
		panel.add(_labelCategory_lbl , "");
		panel.add(_labelCategory_txt , "growx, wrap");

		panel.add(_labelValue_swl    , "");
		panel.add(_labelValue_lbl    , "");
		panel.add(_labelValue_txt    , "growx, wrap");

		panel.add(_rotateCatLab_swl  , "");
//		panel.add(_rotateCatLab_chk  , "skip 1, wrap");
		panel.add(_rotateCatLab_lbl  , "");
		panel.add(_rotateCatLab_txt  , "growx, wrap");
		
		panel.add(_keySdf_swl        , "");
		panel.add(_keySdf_lbl        , "");
//		panel.add(_keySdf_txt        , "growx, wrap");
		panel.add(_keySdf_cbx        , "growx, wrap");
		
		panel.add(_groupByKeySdf_swl , "");
		panel.add(_groupByKeySdf_chk , "skip 1, wrap");

		panel.add(_str2num_swl       , "");
		panel.add(_str2num_chk       , "skip 1, wrap");
		
		panel.add(_removeRegEx_swl   , "");
		panel.add(_removeRegEx_lbl   , "");
		panel.add(_removeRegEx_txt   , "growx, wrap");

		panel.add(_layoutWidth_swl   , "");
		panel.add(_layoutWidth_lbl   , "");
		panel.add(_layoutWidth_txt   , "growx, wrap");

		panel.add(_layoutHeight_swl  , "");
		panel.add(_layoutHeight_lbl  , "");
		panel.add(_layoutHeight_txt  , "growx, wrap");

		panel.add(_showDataValues_swl, "");
		panel.add(_showDataValues_chk, "skip 1, wrap");

		panel.add(_showShapes_swl    , "");
		panel.add(_showShapes_chk    , "skip 1, wrap");
		
		panel.add(_inWindow_swl      , "");
		panel.add(_inWindow_chk      , "skip 1, wrap");
		
		panel.add(_debug_swl         , "");
		panel.add(_debug_chk         , "skip 1, wrap");

		panel.add(_tableKey_swl      , "");
		panel.add(_tableKey_lbl      , "span, wrap");

		panel.add(_tableGrp_swl      , "");
		panel.add(_tableGrp_lbl      , "span, wrap");

		panel.add(_tableVal_swl      , "");
		panel.add(_tableVal_lbl      , "span, wrap");
		
		
		panel.add(createTablePanel() , "span, grow, push, wrap");
		
		_feedback_txt.setEditable(false);
		_feedback_txt.setLineWrap(true);

		_graphType_cbx.setMaximumRowCount(50);

		_keySdf_cbx        .setEditable(true);

		_keySdf_swl        .setToolTipText("The format is used to display time at the label (at the bottom)");
		_keySdf_lbl        .setToolTipText(_keySdf_swl.getToolTipText());
		_keySdf_cbx        .setToolTipText(_keySdf_swl.getToolTipText());
		
		_groupByKeySdf_swl .setToolTipText("Use if you have many values in same day/hour/minute which you want to be summarized...");
		_groupByKeySdf_chk .setToolTipText(_groupByKeySdf_swl.getToolTipText());
		

		_graphType_cbx     .addActionListener(this);
		
		_data_chk          .addActionListener(this);
		_pivot_chk         .addActionListener(this);
		_3d_chk            .addActionListener(this);

		_graphName_txt     .addActionListener(this);
		_graphName_txt     .addFocusListener(this);
		_graphName_txt     .addKeyListener(this);

		_labelCategory_txt .addActionListener(this);
		_labelCategory_txt .addFocusListener(this);
		_labelCategory_txt .addKeyListener(this);

		_labelValue_txt    .addActionListener(this);
		_labelValue_txt    .addFocusListener(this);
		_labelValue_txt    .addKeyListener(this);

//		_rotateCatLab_chk  .addActionListener(this);
		_rotateCatLab_txt  .addActionListener(this);
		_rotateCatLab_txt  .addFocusListener(this);
		_rotateCatLab_txt  .addKeyListener(this);

//		_keySdf_txt        .addActionListener(this);
//		_keySdf_txt        .addFocusListener(this);
//		_keySdf_txt        .addKeyListener(this);
		_keySdf_cbx        .addActionListener(this);
		_keySdf_cbx        .addFocusListener(this);
		_keySdf_cbx        .addKeyListener(this);

		_groupByKeySdf_chk .addActionListener(this);

		_str2num_chk       .addActionListener(this);
		_removeRegEx_txt   .addActionListener(this);
		_removeRegEx_txt   .addFocusListener(this);
		_removeRegEx_txt   .addKeyListener(this);

		_layoutWidth_txt   .addActionListener(this);
		_layoutWidth_txt   .addFocusListener(this);
		_layoutWidth_txt   .addKeyListener(this);

		_layoutHeight_txt  .addActionListener(this);
		_layoutHeight_txt  .addFocusListener(this);
		_layoutHeight_txt  .addKeyListener(this);

		_showDataValues_chk.addActionListener(this);
		_showShapes_chk    .addActionListener(this);
		_inWindow_chk      .addActionListener(this);
		_debug_chk         .addActionListener(this);

		
		return panel;
	}

	private JPanel createTablePanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0","",""));   // insets Top Left Bottom Right

		_tm = new LocalTableModel(_rstm); 
		_table = new LocalTable(_tm);
		
		_tm.addTableModelListener(this);

		panel.add(new JScrollPane(_table), "grow, push, wrap");
		
		panel.add(_feedback_lbl      , "wrap");
		panel.add(_feedback_scroll   , "span, grow, push, wrap");

		return panel;
	}
	private JPanel createOkCancelPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

//		_previewConfig.setToolTipText("Show current configuration in a text dialog. This to view/copy current config properties...");

		// ADD the OK, Cancel, Apply buttons
//		panel.add(_previewConfig, "");
//		panel.add(new JLabel(),   "growx, pushx");
//		panel.add(_apply,         "tag apply, hidemode 3");
		panel.add(_apply,         "");
		panel.add(new JLabel(""), "growx, pushx");
		panel.add(_ok,            "tag ok, right");
		panel.add(_cancel,        "tag cancel");

		_apply .setToolTipText("Create a Graph/Chart Window and Stay in the Dialog, so you can make Modifications and Launch another Graph/Chart Window.");
		_ok    .setToolTipText("Create a Graph/Chart Window and Close the dialog");
		_cancel.setToolTipText("Close the dialog");
		
//		_apply.setEnabled(false);
//		_apply.setVisible(false); // LETS NOT USE THIS FOR THE MOMENT

		// ADD ACTIONS TO COMPONENTS
//		_previewConfig.addActionListener(this);
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
	** BEGIN: Action Listeners, and helper methods for it
	**---------------------------------------------------
	*/
	@Override
	public void actionPerformed(ActionEvent e)
    {
		Object source = e.getSource();

//		// --- BUTTON: PREVIEW CONFIG ---
//		if (_previewConfig.equals(source))
//		{
//			previewConfig();
//		}

		if (_keySdf_cbx.equals(source))
		{
			checkSdfFormat();
		}

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
//			_return = null;
			setVisible(false);
		}

		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			doApply();
			saveProps();
			setVisible(false);
		}

		// --- BUTTON: APPLY ---
		if (_apply.equals(source))
		{
			doApply();
			saveProps();
		}

		// --- FIELD: REGEX ---
		if (_removeRegEx_txt.equals(e.getSource()))
			if ( ! isRegExOk( (JTextField)e.getSource() ) )
				return;

		// --- FIELD: str2num ---
		if (_str2num_chk.equals(e.getSource()))
			_tm.fireTableDataChanged();

		buildFeedback();
    }

	@Override
	public void tableChanged(TableModelEvent e)
	{
		buildFeedback();
//		System.out.println("tableChanged(): TableModelEvent="+e);
//		_apply.setEnabled(true);
	}

	@Override
	public void focusGained(FocusEvent e)
	{
	}

	@Override
	public void focusLost(FocusEvent e)
	{
		if (_removeRegEx_txt.equals(e.getSource()))
			if ( ! isRegExOk( (JTextField)e.getSource() ) )
				return;
		
		// --- Field -- KeySdf -- validate the Simple Date Format
//		if (_keySdf_txt.equals(e.getSource()))
//		{
//			checkSdfFormat();
//		}
		if (_keySdf_cbx.equals(e.getSource()))
		{
			checkSdfFormat();
		}
		
		buildFeedback();
	}
	
	@Override public void keyTyped(KeyEvent e) {}
	@Override public void keyPressed(KeyEvent e) {}
	@Override public void keyReleased(KeyEvent e)
	{
		buildFeedback();
	}

	
	/*---------------------------------------------------
	** END: Action Listeners
	**---------------------------------------------------
	*/

	private void checkSdfFormat()
	{
//		if (StringUtil.hasValue(_keySdf_txt.getText()))
//		{
//			try
//			{
//				new SimpleDateFormat(_keySdf_txt.getText()).format(new Date());
//			}
//			catch(IllegalArgumentException ex)
//			{
//				// make the txtx field a editable combobox, with some examples
//				// use the error tooltip, used in Alarm add dialog
//				SwingUtils.showTimedBalloonTip(_keySdf_txt, 10*1000, true, 
//						"<html>"
//						+ "Validation error: <b>Value will be discarded</b><br><pre>"+ex.getMessage()+"</pre>"
//						+ "<br>"
//						+ "See: https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html"
//						+ "</html>");
//			}
//		}
		
		String format = _keySdf_cbx.getSelectedItem() == null ? "" : _keySdf_cbx.getSelectedItem().toString();
		
		if (StringUtil.hasValue(format))
		{
			try
			{
				new SimpleDateFormat(format).format(new Date());
			}
			catch(IllegalArgumentException ex)
			{
				// make the txtx field a editable combobox, with some examples
				// use the error tooltip, used in Alarm add dialog
				SwingUtils.showTimedBalloonTip(_keySdf_cbx, 10*1000, true, 
						"<html>"
						+ "Validation error: <b>Value will be discarded</b><br><pre>"+ex.getMessage()+"</pre>"
						+ "<br>"
						+ "See: https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html"
						+ "</html>");
			}
		}
	}
	
	private String buildFeedback()
	{
		String sqlCmd = "graph ";

		// Enable or disable --str2num REGEX field 
		_removeRegEx_txt.setEnabled( _str2num_chk.isSelected() );

		
		// --data
		if (_data_chk.isSelected())
			sqlCmd += "--data ";
		
		// --type
		Object obj = _graphType_cbx.getSelectedItem();
		if (obj != null)
			sqlCmd += "--type "+obj+" ";
		
		// --pivot
		if (_pivot_chk.isSelected())
			sqlCmd += "--pivot ";
			
		// --3D
		if (_3d_chk.isSelected())
			sqlCmd += "--3d ";
			
		// --name
		if (StringUtil.hasValue(_graphName_txt.getText()))
			sqlCmd += "--name '"+_graphName_txt.getText()+"' ";

		// --labelCategory
		if (StringUtil.hasValue(_labelCategory_txt.getText()))
			sqlCmd += "--labelCategory '"+_labelCategory_txt.getText()+"' ";

		// --labelValue
		if (StringUtil.hasValue(_labelValue_txt.getText()))
			sqlCmd += "--labelValue '"+_labelValue_txt.getText()+"' ";

		// --rotateCategoryLabels
//		if (_rotateCatLab_chk.isSelected())
//			sqlCmd += "--rotateCategoryLabels 90 ";
		if (StringUtil.hasValue(_rotateCatLab_txt.getText()))
		{
			String rotateStr = _rotateCatLab_txt.getText().trim();
			if ( rotateStr.equals("0") || rotateStr.equals("1") || rotateStr.equals("2") || rotateStr.equals("3") || rotateStr.equals("4") )
			{
				sqlCmd += "--rotateCategoryLabels "+rotateStr+" ";
			}
			else
			{
				SwingUtils.showWarnMessage(this, "Wrong value", "Sorry, value can only be: 0,1,2,3,4\n\n 0 = disabled\n 1 = UP_45\n 2 = UP_90\n 3 = DOWN_45\n 4 = DOWN_90\n", null);
				_rotateCatLab_txt.setText("1");
			}
		}

		// -- keySimpleDateFormat
		String sdfFormat = _keySdf_cbx.getSelectedItem() == null ? "" : _keySdf_cbx.getSelectedItem().toString();
//		if (StringUtil.hasValue(sdfFormat))
//			sqlCmd += "--keySimpleDateFormat '"+sdfFormat+"' ";
		if (StringUtil.hasValue(sdfFormat))
			sqlCmd += "--keySimpleDateFormat '"+sdfFormat+"' ";

		// --groupByKeySdf
		if (_groupByKeySdf_chk.isSelected())
			sqlCmd += "--groupByKeySdf ";
			
		// --str2num
		if (_str2num_chk.isSelected())
			sqlCmd += "--str2num ";
			
		// --removeRegEx
		if (StringUtil.hasValue(_removeRegEx_txt.getText()))
			sqlCmd += "--removeRegEx '"+_removeRegEx_txt.getText()+"' ";

		// --width
		if (StringUtil.hasValue(_layoutWidth_txt.getText()))
			sqlCmd += "--width '"+_layoutWidth_txt.getText()+"' ";

		// --height
		if (StringUtil.hasValue(_layoutHeight_txt.getText()))
			sqlCmd += "--height '"+_layoutHeight_txt.getText()+"' ";

		// --showDataValues
		if (_showDataValues_chk.isSelected())
			sqlCmd += "--showDataValues ";
			
		// --showShapes
		if (_showShapes_chk.isSelected())
			sqlCmd += "--showShapes ";
			
		// --window
		if (_inWindow_chk.isSelected())
			sqlCmd += "--window ";
			
		// --debug
		if (_debug_chk.isSelected())
			sqlCmd += "--debug ";

		// --keyCols
		String keyCols = _tm.getKeyColumns();
		if (StringUtil.hasValue(keyCols))
			sqlCmd += "--keyCols '"+keyCols+"' ";
		
		// --groupCols
		String groupCols = _tm.getGroupColumns();
		if (StringUtil.hasValue(groupCols))
			sqlCmd += "--groupCols '"+groupCols+"' ";
		
		// --keyCols
		String valCols = _tm.getValueColumns();
		if (StringUtil.hasValue(valCols))
			sqlCmd += "--valCols '"+valCols+"' ";
		
		// Set the feedback field
		_feedback_txt.setText("select c1,c2,c3 from someTable\ngo | " + sqlCmd);
		return sqlCmd;
	}
	
	private void doApply()
	{
		JFrame frame = null;
		try
		{
			PipeCommandGraph pipeCommandGraph = new PipeCommandGraph(buildFeedback(), "", null);//_rstm.getSqlText());
			JGraphResultSet grs = new JGraphResultSet(_rstm, pipeCommandGraph);
			
			frame = grs.createWindow();
		}
		catch (Exception ex)
		{
			SwingUtils.showErrorMessage("Create Graph", "Problems Creating Graph: "+ex.getMessage(), ex);

			if (frame != null)
				frame.setVisible(false);
		}
		
	}

	private boolean isRegExOk(JTextField textfield)
	{
		String regex = textfield.getText();
		
		if (StringUtil.isNullOrBlank(regex))
			return true;

		try
		{
			Pattern.compile(regex);
			return true;
		}
		catch(PatternSyntaxException ex)
		{
			SwingUtils.showErrorMessage(this, "Faulty Regex", 
					  "<html>"
					+ "The regex '<b>"+regex+"</b>' is not valid.<br>"
					+ "Error:"
					+ "<pre>"
					+ StringUtil.toHtmlString(ex.getMessage())
					+ "</pre>"
					+ "</html>", ex);
			
			setFocus(textfield);
			
			return false;
		}
	}

	/*---------------------------------------------------
	** BEGIN: Property handling
	**---------------------------------------------------
	*/
	private void saveProps()
	{
	}

	private void loadProps()
	{
	}
	/*---------------------------------------------------
	** END: Property handling
	**---------------------------------------------------
	*/
	

	private static class LocalTable
//	extends GTable
	extends JXTable
	{
		private static final long serialVersionUID = 1L;
		private LocalTableModel _ltm;

		public LocalTable(LocalTableModel ltm)
		{
//			super(ltm, "Columns");
			super(ltm);
			_ltm = ltm;
			localInit();
		}

		private void localInit()
		{
//			setShowGrid(false);
			setSortable(true);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			packAll(); // set size so that all content in all cells are visible
			setColumnControlVisible(true);
			setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);

			
			// COLOR CODE SOME ROWS/CELLS
			Configuration conf = Configuration.getCombinedConfiguration();
			String colorStr = null;

			if (conf != null) colorStr = conf.getProperty(getName()+".color.considdered");
			addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				@Override
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
//					if (LocalTableModel.TAB_POS_CONSIDDERED == convertColumnIndexToModel(adapter.column))
					if (LocalTableModel.TAB_POS_VAL_COL == convertColumnIndexToModel(adapter.column))
					{
						RowEntry entry = _ltm.getEntryForRow(convertRowIndexToModel(adapter.row));
						return entry.isConsiddered();
					}
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.LIGHT_GRAY), null));
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

					int vcol = getColumnModel().getColumnIndexAtX(e.getPoint().x);
					if (vcol == -1) return null;

					int mcol = convertColumnIndexToModel(vcol);
					if (mcol == -1) return null;

					tip = _ltm.getToolTipText(mcol);

					if (tip == null)
						return null;
					return "<html>" + tip + "</html>";
				}
			};

			return tabHeader;
		}

//		protected void refreshTable(AlarmEntry alarmEntry)
//		{
//			_alarmSettingsTableModel.refreshTable(alarmEntry);
//			packAll(); // set size so that all content in all cells are visible
//		}
	}
	
//	private static class RowEntry
	private class RowEntry
	{
		private boolean _isKey        = false;
		private boolean _isGrp        = false;
		private boolean _isVal        = false;
		private String  _colName      = "";
		private int     _jdbcType     = -99;
		private String  _jdbcTypeStr  = "";
		private String  _dbmsDatatype = "";

		public RowEntry(boolean isKey, boolean isVal, String colName, int jdbcType, String dbmsDatatype)  				
		{
			_isKey        = isKey      ;
			_isVal        = isVal      ;
			_colName      = colName    ;
			_jdbcType     = jdbcType   ;
			_jdbcTypeStr  = ResultSetTableModel.getColumnJavaSqlTypeName(jdbcType);
			_dbmsDatatype = dbmsDatatype;
		}
		
		public boolean isKey()           { return _isKey; }
		public boolean isGrp()           { return _isGrp; }
		public boolean isVal()           { return _isVal; }
		public String  getName()         { return _colName; }
		public int     getJdbcType()     { return _jdbcType; }
		public String  getJdbcTypeStr()  { return _jdbcTypeStr; }
		public String  getDbmsDatatype() { return _dbmsDatatype; }
		public boolean isConsiddered()   
		{
			if (_str2num_chk.isSelected())
				return JGraphResultSet.isNumberColumn(null, _jdbcType) || JGraphResultSet.isStringColumn(null, _jdbcType);

			return JGraphResultSet.isNumberColumn(null, _jdbcType); 
		}
	}
	
//	private static class LocalTableModel
	private class LocalTableModel
	extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;

		private List<RowEntry> _rows = new ArrayList<>();

//		protected        final String[] TAB_HEADER = {"Key Column", "Value Column", "Column Name", "Considdered", "JDBC Datatype", "JDBC Datatype String", "Guessed DBMS Datatype"};
		protected        final String[] TAB_HEADER = {"Key Column", "Group Column", "Value Column", "Column Name", "JDBC Datatype", "JDBC Datatype String", "Guessed DBMS Datatype"};
		protected static final int TAB_POS_KEY_COL           = 0;
		protected static final int TAB_POS_GRP_COL           = 1;
		protected static final int TAB_POS_VAL_COL           = 2;
		protected static final int TAB_POS_COL_NAME          = 3;
//		protected static final int TAB_POS_CONSIDDERED       = 4;
		protected static final int TAB_POS_COL_DATATYPE      = 4;
		protected static final int TAB_POS_COL_DATATYPE_STR  = 5;
		protected static final int TAB_POS_COL_GUESS_DBMS_DT = 6;

		LocalTableModel(ResultSetTableModel rstm)
		{
			super();
			for (int c=0; c<rstm.getColumnCount(); c++)
			{
				RowEntry entry = new RowEntry(c==0, JGraphResultSet.isNumberColumn(rstm, c), rstm.getColumnName(c), rstm.getSqlType(c), rstm.getGuessedDbmsDatatype(c)); 
				_rows.add(entry);
			}
		}
		
		public RowEntry getEntryForRow(int row)
		{
			return _rows.get(row);
		}

		public String getKeyColumns()
		{
			String retStr = "";
			for (RowEntry entry : _rows)
			{
				if (entry.isKey())
					retStr += entry.getName() + ",";
			}

			if (StringUtil.hasValue(retStr))
				retStr = StringUtil.removeLastComma(retStr);

			return retStr;
		}

		public String getGroupColumns()
		{
			String retStr = "";
			for (RowEntry entry : _rows)
			{
				if (entry.isGrp())
					retStr += entry.getName() + ",";
			}

			if (StringUtil.hasValue(retStr))
				retStr = StringUtil.removeLastComma(retStr);

			return retStr;
		}

		public String getValueColumns()
		{
			String retStr = "";
			for (RowEntry entry : _rows)
			{
				if (entry.isVal())
					retStr += entry.getName() + ",";
			}

			if (StringUtil.hasValue(retStr))
				retStr = StringUtil.removeLastComma(retStr);

			return retStr;
		}

		public String getToolTipText(int col)
		{
			switch(col)
			{
			case TAB_POS_KEY_COL:           return "Select if this should be a KEY Column";
			case TAB_POS_GRP_COL:           return "Select if this should be a GROUP Column, this is if we want to group the data in some way (probably most interesting for STACKED BAR/AREA";
			case TAB_POS_VAL_COL:           return "Select if this should be a VALUE Column. If the background is grey, then this column will be considdered as a 'selectable data source'.";
			case TAB_POS_COL_NAME:          return "ResultSet column name";
//			case TAB_POS_CONSIDDERED:       return "Can this column be used as a Data Value (based on the datatype)";
			case TAB_POS_COL_DATATYPE:      return "JDBC Datatype for this column";
			case TAB_POS_COL_DATATYPE_STR:  return "JDBC Datatype for this column";
			case TAB_POS_COL_GUESS_DBMS_DT: return "DBMS Datatype for this column";
			}
			return null;
		}

		@Override
		public int getRowCount()
		{
			return _rows.size();
		}

		@Override
		public int getColumnCount()
		{
			return TAB_HEADER.length;
		}

		@Override
		public String getColumnName(int col)
		{
			return TAB_HEADER[col];
		}
		
		@Override
		public Object getValueAt(int row, int col)
		{
			RowEntry e = _rows.get(row);
			switch (col)
			{
			case TAB_POS_KEY_COL:           return e.isKey();
			case TAB_POS_GRP_COL:           return e.isGrp();
			case TAB_POS_VAL_COL:           return e.isVal();
			case TAB_POS_COL_NAME:          return e.getName();
//			case TAB_POS_CONSIDDERED:       return e.isConsiddered();
			case TAB_POS_COL_DATATYPE:      return e.getJdbcType();
			case TAB_POS_COL_DATATYPE_STR:  return e.getJdbcTypeStr();
			case TAB_POS_COL_GUESS_DBMS_DT: return e.getDbmsDatatype();
			}
			
			return null;
		}

		@Override
		public void setValueAt(Object newVal, int row, int col)
		{
			if ( ! isCellEditable(row, col) )
				return;
	
			RowEntry e = _rows.get(row);
			
			// Set the value
			if (col == TAB_POS_KEY_COL) e._isKey = newVal.toString().equalsIgnoreCase("true");
			if (col == TAB_POS_GRP_COL) e._isGrp = newVal.toString().equalsIgnoreCase("true");
			if (col == TAB_POS_VAL_COL) e._isVal = newVal.toString().equalsIgnoreCase("true");
			
//			Object oldVal = getValueAt(row, col);
//	
//			// has the value changed: mark it as modified
//			if (oldVal != null )
//				if ( ! oldVal.equals(newVal) )
//					e._modified = true;
//			if (newVal != null )
//				if ( ! newVal.equals(oldVal) )
//					e._modified = true;
//	
//			// Set the value
//			if (col == TAB_POS_ENABLED) e._enabled = newVal.toString().equalsIgnoreCase("true");
//			if (col == TAB_POS_VALUE)   e._value   = newVal.toString();
			
			fireTableCellUpdated(row, col);
		}

		@Override
		public Class<?> getColumnClass(int col)
		{
			if (col == TAB_POS_KEY_COL)       return Boolean.class;
			if (col == TAB_POS_GRP_COL)       return Boolean.class;
			if (col == TAB_POS_VAL_COL)       return Boolean.class;
//			if (col == TAB_POS_CONSIDDERED)   return Boolean.class;

			return Object.class;
		}

		@Override
		public boolean isCellEditable(int row, int col)
		{
			if (col == TAB_POS_KEY_COL)        return true;
			if (col == TAB_POS_GRP_COL)        return true;
			if (col == TAB_POS_VAL_COL)        return true;

			return false;
		}

//		/** Check if this model has changed */
//		public boolean isDirty()
//		{
//			for (CmSettingsHelper ase : _settings)
//			{
//				if (ase.isModified())
//					return true;
//			}
//
//			// Finally: no changes 
//			return false;
//		}
 	}
}

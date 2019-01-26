package com.asetune.tools.sqlw;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SortOrder;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.swing.GTabbedPane;
import com.asetune.sql.SqlPickList;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.tools.sqlw.SqlStatementCmdLoadFile.PreviewObject;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.DbUtils;
import com.asetune.utils.FileUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class TableImportDialog
extends JDialog
implements ActionListener, KeyListener
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger = Logger.getLogger(TableImportDialog.class);
	
	private static final String PROPKEY_lastKnownFile = "TableImportDialog.lastKnownFile";
	private static final String DEFAULT_lastKnownFile = "";
	
	private static final String PROPKEY_previewRowCount = "TableImportDialog.preview.row.count";
	private static final int    DEFAULT_previewRowCount = 100;
	
	private JButton    _ok             = new JButton("OK");
	private JButton    _cancel         = new JButton("Cancel");
	
	private String     _qicDefault     = "\""; // used if _qic is null
	private String     _qic            = null; // set: on first database call
	
	private int                  _previewFirstRowCount = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_previewRowCount, DEFAULT_previewRowCount);
	private PreviewObject        _sourcefilePreviewObj = null;

	private JLabel               _dialogDescription_lbl = new JLabel("");

	private JSplitPane           _mainSplitPane  = new JSplitPane();

	private JLabel               _sqlWinCmd_lbl  = new JLabel("Sql Window Command Preview");
	private JTextField           _sqlWinCmd_txt  = new JTextField();
	private JLabel               _sqlWinCmd_lbl2 = new JLabel("Tip: Copy the above command and paste it into the SQL Command Window");
	
	//-----------------------------------------------------------------
	private JPanel               _sf_pan;
	private JLabel               _sfName_lbl = new JLabel("Source Filename");
	private JTextField           _sfName_txt = new JTextField();
	private JButton              _sfName_but = new JButton("...");
	private JButton              _sfNameTestLoad_but = new JButton("Test load file");

	private JLabel               _sfEncoding_lbl = new JLabel("Source File Encoding");
	private JComboBox<String>    _sfEncoding_cbx = new JComboBox<>();
	private JLabel               _sfGuessedEncoding_lbl = new JLabel("Guessed File Encoding");
	private JTextField           _sfGuessedEncoding_txt = new JTextField();

	private JCheckBox            _sfCsv_chk        = new JCheckBox("CSV File, according to RFC 4180");
	private JLabel               _sfCsv_lbl        = new JLabel("<html> --- A Description of RFC 4180 can be found here <a href='http://tools.ietf.org/html/rfc4180'>http://tools.ietf.org/html/rfc4180</a></html>");
//	  -R,--rfc4180                 Use RFC 4180 to write file.                    DEFAULT=false
//              see: http://tools.ietf.org/html/rfc4180 
//              Basically: embeds newline and quotes within a quoted string. 
//              This sets -f to ',' and -r to '\r\n'

	private JCheckBox            _sfHasHeader_chk      = new JCheckBox("Source File has a column header (skip loading first row)");

	private JLabel               _sfFieldTerm_lbl = new JLabel("Field Terminator");
	private JTextField           _sfFieldTerm_txt = new JTextField();
	private JLabel               _sfRowTerm_lbl = new JLabel("Field Terminator");
	private JTextField           _sfRowTerm_txt = new JTextField();
	
	private JLabel               _sfNullValueStr_lbl  = new JLabel("NULL Value");
	private JTextField           _sfNullValueStr_txt  = new JTextField();
	private JCheckBox            _sfEmtyStrIsNull_chk = new JCheckBox("Treat empty source columns strings as a NULL value");

	private JLabel               _sfSkipRowRegex_lbl = new JLabel("Skip Rows");
	private JTextField           _sfSkipRowRegex_txt = new JTextField("");

	private JLabel               _sfOnlyRowRegex_lbl = new JLabel("Only Matching Rows");
	private JTextField           _sfOnlyRowRegex_txt = new JTextField("");
	
	private JLabel               _sfPreview_lbl      = new JLabel("Source File Preview");

	private RSyntaxTextAreaX     _sfPreviewRaw_txt      = new RSyntaxTextAreaX(_previewFirstRowCount, 120);
//	private RSyntaxTextAreaX     _sfPreviewRaw_txt      = new RSyntaxTextAreaX(10, 120);
//	private RSyntaxTextAreaX     _sfPreviewRaw_txt      = new RSyntaxTextAreaX(30, 120);
	private RTextScrollPane      _sfPreviewRaw_scroll   = new RTextScrollPane(_sfPreviewRaw_txt);

	private RSyntaxTextAreaX     _sfPreviewParsed_txt      = new RSyntaxTextAreaX(_previewFirstRowCount, 120);
//	private RSyntaxTextAreaX     _sfPreviewParsed_txt      = new RSyntaxTextAreaX(10, 120);
//	private RSyntaxTextAreaX     _sfPreviewParsed_txt      = new RSyntaxTextAreaX(30, 120);
	private RTextScrollPane      _sfPreviewParsed_scroll   = new RTextScrollPane(_sfPreviewParsed_txt);

	private GTabbedPane          _sfPreview_tp       = new GTabbedPane();
	
	//-----------------------------------------------------------------
	private JPanel               _tt_pan;
	
	private JLabel               _ttName_lbl    = new JLabel("Target Table: ");
//	private JTextField           _ttName_txt = new JTextField();
	private JLabel               _ttCatalog_lbl = new JLabel("Catalog");
	private JTextField           _ttCatalog_txt = new JTextField();
	private JLabel               _ttSchema_lbl  = new JLabel("Schema");
	private JTextField           _ttSchema_txt  = new JTextField();
	private JLabel               _ttTable_lbl   = new JLabel("Target");
	private JTextField           _ttTable_txt   = new JTextField();
	private JButton              _ttName_but    = new JButton("Search...");
	private JButton              _ttCreate_but  = new JButton("Create...");

	private JLabel               _tt_lbl    = new JLabel("Target Table Definition (and mapping)");
//	private JXTable              _tt_tab    = new JXTable();
	private ResultSetJXTable     _tt_tab    = new ResultSetJXTable( new DefaultTableModel() );
	private JScrollPane          _tt_scroll = new JScrollPane(_tt_tab);

	private GTabbedPane          _ttInfo_tp       = new GTabbedPane();
//	private ResultSetJXTable     _ttMap_tab       = new ResultSetJXTable( new DefaultTableModel() );
//	private JScrollPane          _ttMap_scroll    = new JScrollPane(_ttMap_tab);
//	private ResultSetJXTable     _ttRs_tab        = new ResultSetJXTable( new DefaultTableModel() );
//	private JScrollPane          _ttRs_scroll     = new JScrollPane(_ttRs_tab);
//	private ResultSetJXTable     _ttJdbcMd_tab    = new ResultSetJXTable( new DefaultTableModel() );
//	private JScrollPane          _ttJdbcMd_scroll = new JScrollPane(_ttJdbcMd_tab);
	private ResultSetJXTable     _ttMap_tab       = null;
	private JScrollPane          _ttMap_scroll    = null;
	private ResultSetJXTable     _ttRs_tab        = null;
	private JScrollPane          _ttRs_scroll     = null;
	private ResultSetJXTable     _ttJdbcMd_tab    = null;
	private JScrollPane          _ttJdbcMd_scroll = null;
	
	private JCheckBox            _ttTruncate_chk        = new JCheckBox("Truncate target table before load");
	private JCheckBox            _ttSkipProblemRows_chk = new JCheckBox("Skip Problem Rows/Records (this will set 'send batch size' to 1)");
	private JCheckBox            _ttTestLoad_chk        = new JCheckBox("Test load (ony 1 row will be tested, and rolled back)");

	private JLabel               _ttBatchSize_lbl  = new JLabel("Batch Size");
	private SpinnerNumberModel   _ttBatchSize_spm  = new SpinnerNumberModel(SqlStatementCmdLoadFile.DEFAULT_TRAN_BATCH_SIZE, 0, Integer.MAX_VALUE, 1000);
	private JSpinner             _ttBatchSize_sp   = new JSpinner(_ttBatchSize_spm);
	private JLabel               _ttBatchSize_lbl2 = new JLabel("0 = All in one transaction");
	
	private JLabel               _ttSendBatchSize_lbl  = new JLabel("Send Batch Size");
	private SpinnerNumberModel   _ttSendBatchSize_spm  = new SpinnerNumberModel(SqlStatementCmdLoadFile.DEFAULT_SEND_BATCH_SIZE, 1, Integer.MAX_VALUE, 1000);
	private JSpinner             _ttSendBatchSize_sp   = new JSpinner(_ttSendBatchSize_spm);
	
	private ConnectionProvider   _connProvider = null;
	
	private TableImportDialog(Window owner, ConnectionProvider connProvider)
	{
//		super(owner, "", true);
		super(owner, "", JDialog.DEFAULT_MODALITY_TYPE);

		_connProvider = connProvider;
		
		initComponents();
		pack();
	}

	public static void showDialog(Window owner, ConnectionProvider connProvider)
	{
		TableImportDialog dialog = new TableImportDialog(owner, connProvider);
		dialog.setLocationRelativeTo(owner);
		dialog.setFocus();
		dialog.setVisible(true);
		dialog.dispose();
	}

	protected void initComponents() 
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0 0 0 0, wrap 1","", ""));   // insets Top Left Bottom Right

//		panel.add(_favoriteType_lbl,  "");
//		panel.add(_favoriteType_cbx,  "pushx, growx, wrap");
//		
//		panel.add(_favoriteName_lbl,  "");
//		panel.add(_favoriteName_txt,  "pushx, growx, wrap");
//		
//		panel.add(_favoriteDesc_lbl,  "");
//		panel.add(_favoriteDesc_txt,  "pushx, growx, wrap");
//		
//		panel.add(_favoriteIcon_lbl,  "");
//		panel.add(_favoriteIcon_txt,  "pushx, growx, wrap");
//		
//		panel.add(_preview1_lbl,      "");
//		panel.add(_preview2_lbl,      "pushx, growx, wrap 15");
//		
////		panel.add(_filePreview_lbl,       "wrap");
//		panel.add(_filePreview_scroll,    "span, push, grow, wrap");
//		
//		// ADD the OK, Cancel, Apply buttons
//		panel.add(_ok,     "tag ok,     gap top 20, skip, split, bottom, right, pushx");
//		panel.add(_cancel, "tag cancel,                   split, bottom");

		String description = "<html>"
				+ "<font color='red'>"
				+ 	"WARNING!<br>"
				+ 	"This Dialog is NOT readdy yet!"
				+ "</font>"
				+ "Please use the sql command utility <code>\\loadfile</code>"
				+ "</html>";
		_dialogDescription_lbl.setText(description);

		JPanel top = new JPanel(new MigLayout("insets 0 0 0 0"));
		top.add(createSourceFilePanel(), "dock center");

		JPanel bottom = new JPanel(new MigLayout("insets 0 0 0 0"));
		bottom.add(createTargetTablePanel(), "dock center");

		_mainSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);   // top and bottom
//		_mainSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT); // side by side
		_mainSplitPane.setDividerLocation(0.5); // 50%
		_mainSplitPane.setTopComponent(top);
		_mainSplitPane.setBottomComponent(bottom);
		
		
		panel.add(_dialogDescription_lbl,   "gap 10 10 10 10, wrap");

//		panel.add(createSourceFilePanel(),  "grow, push, wrap");
//		panel.add(createTargetTablePanel(), "grow, push, wrap");
		panel.add(_mainSplitPane,           "growx, pushx, wrap");
		panel.add(createFeedbackPanel(),    "growx, pushx, wrap");
		panel.add(createOkCancelPanel(),    "bottom, right");

		setContentPane(panel);

		// Fill in some start values
//		_favoriteType_cbx.setSelectedItem(_entry.getType());
//		_favoriteName_txt.setText(_entry.getName());
//		_favoriteDesc_txt.setText(_entry.getDescription());
//		_favoriteIcon_txt.setText(_entry.getIcon());
//		_filePreview_txt     .setText(_entry.getOriginCommand());
		
		// ADD KEY listeners
//		_favoriteName_txt.addKeyListener(this);
//		_favoriteDesc_txt.addKeyListener(this);
//		_favoriteIcon_txt.addKeyListener(this);
//		_filePreview_txt     .addKeyListener(this);
		
		// ADD ACTIONS TO COMPONENTS
	}
	
	private JPanel createSourceFilePanel()
	{
		JPanel panel = SwingUtils.createPanel("Source File", true, new MigLayout("","",""));

		_sfEncoding_cbx.addItem("<unknown>");
		Map<String, Charset> charSets = Charset.availableCharsets();
		Iterator<String> it = charSets.keySet().iterator();
		while (it.hasNext())
		{
			StringBuilder sb = new StringBuilder();
			String csName = it.next();
			sb.append(csName);
//			Iterator<String> aliases = charSets.get(csName).aliases().iterator();
//			if ( aliases.hasNext() )
//				sb.append(": ");
//			while (aliases.hasNext())
//			{
//				sb.append(aliases.next());
//				if ( aliases.hasNext() )
//					sb.append(", ");
//			}
			_sfEncoding_cbx.addItem(sb.toString());
		}

		// Set some DEFAULT Values for some fields
		_sfCsv_chk          .setSelected(true);
		_sfHasHeader_chk    .setSelected(true);
		_sfFieldTerm_txt    .setText(SqlStatementCmdLoadFile.DEFAULT_FIELD_TERM_STRING);
		_sfRowTerm_txt      .setText(SqlStatementCmdLoadFile.DEFAULT_ROW_TERM_STRING);
		_sfNullValueStr_txt .setText(SqlStatementCmdLoadFile.DEFAULT_NULL_STRING);
		_sfEmtyStrIsNull_chk.setSelected(false);
		_sfSkipRowRegex_txt .setText("");
		_sfOnlyRowRegex_txt .setText("");

        int r = _previewFirstRowCount;
		_sfPreview_tp.addTab("Raw (first 64KB)",          _sfPreviewRaw_scroll);
		_sfPreview_tp.addTab("Parsed (first "+r+" rows)", _sfPreviewParsed_scroll);
		
		_sfPreviewRaw_txt   .setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
		_sfPreviewParsed_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);

		panel.add(_sfName_lbl,            "");
		panel.add(_sfName_txt,            "span, split, pushx, growx");
		panel.add(_sfName_but,            "");
		panel.add(_sfNameTestLoad_but,    "wrap");

		panel.add(_sfEncoding_lbl,        "");
		panel.add(_sfEncoding_cbx,        "span, split");
		panel.add(_sfGuessedEncoding_lbl, "");
		panel.add(_sfGuessedEncoding_txt, "width 150lp, wrap");

		panel.add(_sfCsv_chk,             "span, split");
		panel.add(_sfCsv_lbl,             "wrap");
		
		panel.add(_sfHasHeader_chk,       "span, wrap");

		panel.add(_sfFieldTerm_lbl,       "");
		panel.add(_sfFieldTerm_txt,       "span, split, width 40lp");
		panel.add(_sfRowTerm_lbl,         "");
		panel.add(_sfRowTerm_txt,         "width 40lp, wrap");

		panel.add(_sfNullValueStr_lbl,    "");
		panel.add(_sfNullValueStr_txt,    "span, split, width 75lp");
		panel.add(_sfEmtyStrIsNull_chk,   "wrap");
		
//		panel.add(_sfSkipRowRegex_lbl,    "");
//		panel.add(_sfSkipRowRegex_txt,    "pushx, growx, wrap");

//		panel.add(_sfOnlyRowRegex_lbl,    "");
//		panel.add(_sfOnlyRowRegex_txt,    "pushx, growx, wrap");
		
		panel.add(_sfPreview_lbl,         "span, wrap");
//		panel.add(_sfPreview_scroll,      "span, grow, wrap");
		panel.add(_sfPreview_tp,          "span, grow, wrap");

		// ADD ACTION listeners
		_sfName_txt         .addActionListener(this);
		_sfName_but         .addActionListener(this);
		_sfNameTestLoad_but .addActionListener(this);
		_sfEncoding_cbx     .addActionListener(this);
		_sfCsv_chk          .addActionListener(this);
		_sfHasHeader_chk    .addActionListener(this);
		_sfFieldTerm_txt    .addActionListener(this);
		_sfRowTerm_txt      .addActionListener(this);
		_sfNullValueStr_txt .addActionListener(this);
		_sfEmtyStrIsNull_chk.addActionListener(this);
		_sfSkipRowRegex_txt .addActionListener(this);
		_sfOnlyRowRegex_txt .addActionListener(this);
		
		// ADD KEY listeners
		_sfName_txt        .addKeyListener(this);
		_sfFieldTerm_txt   .addKeyListener(this);
		_sfRowTerm_txt     .addKeyListener(this);
		_sfNullValueStr_txt.addKeyListener(this);
		_sfSkipRowRegex_txt.addKeyListener(this);
		_sfOnlyRowRegex_txt.addKeyListener(this);
		
		return panel;
	}

	private ResultSetJXTable createEmptyTable()
	{
		ResultSetJXTable tab = new ResultSetJXTable( new DefaultTableModel() );
		tab.setSortable(true);
		tab.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
		tab.packAll(); // set size so that all content in all cells are visible
		tab.setColumnControlVisible(true);
//		tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		return tab;
	}
	private void setTableModel(ResultSetJXTable tab, TableModel tm)
	{
		tab.setModel(tm);
		tab.packAll(); // set size so that all content in all cells are visible
		tab.setColumnControlVisible(true);
		tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	}

	private JPanel createTargetTablePanel()
	{
		JPanel panel = SwingUtils.createPanel("Target Table", true, new MigLayout("","",""));

        _ttMap_tab       = createEmptyTable();
        _ttMap_scroll    = new JScrollPane(_ttMap_tab);
        
        _ttRs_tab        = createEmptyTable();
        _ttRs_scroll     = new JScrollPane(_ttRs_tab);
        
        _ttJdbcMd_tab    = createEmptyTable();
        _ttJdbcMd_scroll = new JScrollPane(_ttJdbcMd_tab);

        
		// Add tabs
        int r = _previewFirstRowCount;
		_ttInfo_tp.addTab("Mapping",                      _ttMap_scroll);
		_ttInfo_tp.addTab("ResultSet (first "+r+" rows)", _ttRs_scroll);
		_ttInfo_tp.addTab("JDBC MetaData",                _ttJdbcMd_scroll);
		
		panel.add(_ttName_lbl,            "");
//		panel.add(_ttName_txt,            "span, split, pushx, growx");
		panel.add(_ttCatalog_lbl,         "span, split");
		panel.add(_ttCatalog_txt,         "pushx, growx");
		panel.add(_ttSchema_lbl,          "");
		panel.add(_ttSchema_txt,          "pushx, growx");
		panel.add(_ttTable_lbl,           "");
		panel.add(_ttTable_txt,           "pushx, growx");
		panel.add(_ttName_but,            "");
		panel.add(_ttCreate_but,          "wrap");

		panel.add(_tt_lbl,                "span, wrap");
		panel.add(_ttInfo_tp,             "span, split, grow, wrap");
		
		panel.add(_ttTruncate_chk,        "span, wrap");
		panel.add(_ttSkipProblemRows_chk, "span, wrap");
		panel.add(_ttTestLoad_chk,        "span, wrap");

		panel.add(_ttBatchSize_lbl,       "");
		panel.add(_ttBatchSize_sp,        "split");
		panel.add(_ttBatchSize_lbl2,      "wrap");
		
		panel.add(_ttSendBatchSize_lbl,   "");
		panel.add(_ttSendBatchSize_sp,    "wrap");
		
		// ADD ACTION listeners
//		_ttName_txt         .addActionListener(this);
		_ttCatalog_txt      .addActionListener(this);
		_ttSchema_txt       .addActionListener(this);
		_ttTable_txt        .addActionListener(this);
		_ttName_but         .addActionListener(this);
		_ttCreate_but       .addActionListener(this);
//		_ttBatchSize_sp     .addActionListener(this);
//		_ttSendBatchSize_sp .addActionListener(this);
		
		// ADD KEY listeners
//		_ttName_txt        .addKeyListener(this);
		_ttCatalog_txt     .addKeyListener(this);
		_ttSchema_txt      .addKeyListener(this);
		_ttTable_txt       .addKeyListener(this);
		_ttBatchSize_sp    .addKeyListener(this);
		_ttSendBatchSize_sp.addKeyListener(this);
		
		return panel;
	}

	private JPanel createFeedbackPanel()
	{
		JPanel panel = SwingUtils.createPanel("Cmd Feedback", false, new MigLayout("","",""));

		panel.add(_sqlWinCmd_lbl, "");
		panel.add(_sqlWinCmd_txt, "pushx, growx, wrap");
		panel.add(_sqlWinCmd_lbl2, "skip 1, wrap");
		
		return panel;
	}

	private JPanel createOkCancelPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

//		_ok_lbl.setForeground(Color.RED);
//		_ok_lbl.setFont( _ok_lbl.getFont().deriveFont(Font.BOLD) );
//		_ok_lbl.setText("");

		// ADD the OK, Cancel, Apply buttons
//		panel.add(_ok_lbl, "left");
		panel.add(_ok,     "tag ok, right");
		panel.add(_cancel, "tag cancel");

		// ADD ACTIONS TO COMPONENTS
		_ok    .addActionListener(this);
		_cancel.addActionListener(this);

		return panel;
	}


	@Override public void keyPressed (KeyEvent e) {}
	@Override public void keyTyped   (KeyEvent e) {}
	@Override public void keyReleased(KeyEvent e)
	{
		setCmdPreview();
	}

	private void setCmdPreview()
	{
		String cmd = "";
		
		String str = "";
		int num = -1;

		String qic = _qic;
		if (qic == null)
			qic = _qicDefault;
		
		// --tablename
//		str = _ttName_txt.getText();
//		if (StringUtil.hasValue(str)) 
//			cmd += " --tablename '" + str + "'";
		String cat = _ttCatalog_txt.getText();
		String sch = _ttSchema_txt .getText();
		String tab = _ttTable_txt  .getText();
		if (StringUtil.hasValue(cat)) str += qic + cat + qic + ".";
		if (StringUtil.hasValue(sch)) str += qic + sch + qic + ".";
		str += qic + tab + qic;
		if (StringUtil.hasValue(cat) || StringUtil.hasValue(sch) || StringUtil.hasValue(tab))
			cmd += " --tablename '" + str + "'";
	
		// --skipProblemRows
		if (_ttSkipProblemRows_chk.isSelected() != SqlStatementCmdLoadFile.DEFAULT_SKIP_PROBLEM_ROWS)
			cmd += " --skipProblemRows";

		// --noHeader
		if ( ! _sfHasHeader_chk.isSelected() != SqlStatementCmdLoadFile.DEFAULT_NOHEADER)
			cmd += " --noHeader";

		// --field_terminator
		str = _sfFieldTerm_txt.getText();
		if (StringUtil.hasValue(str) && !str.equals(SqlStatementCmdLoadFile.DEFAULT_FIELD_TERM_STRING)) 
			cmd += " --field_terminator '" + str + "'";

		// --row_terminator
		str = _sfRowTerm_txt.getText();
		if (StringUtil.hasValue(str) && !str.equals(SqlStatementCmdLoadFile.DEFAULT_ROW_TERM_STRING)) 
			cmd += " --row_terminator '" + str + "'";


		// --nullValue ... 
		String nullStr = "";
		str = _sfNullValueStr_txt.getText();
		if (StringUtil.hasValue(str)) 
			nullStr = str;

		if (_sfEmtyStrIsNull_chk.isSelected()) 
			nullStr = "";

		// --nullValue
		if (!nullStr.equals(SqlStatementCmdLoadFile.DEFAULT_NULL_STRING)) 
			cmd += " --nullValue '" + nullStr + "'";


		// --charset
		Object charsetObj = _sfEncoding_cbx.getSelectedItem();
		str = charsetObj == null ? "" : charsetObj.toString();
		if (str.startsWith("<"))
			str = "";
		if (StringUtil.hasValue(str)) 
			cmd += " --charset '" + str + "'";

		// --checkAndStop
		if (_ttTestLoad_chk.isSelected() != SqlStatementCmdLoadFile.DEFAULT_CHECK_AND_STOP) 
			cmd += " --checkAndStop";

		// --truncateTable
		if (_ttTruncate_chk.isSelected() != SqlStatementCmdLoadFile.DEFAULT_TRUNCATE_TABLE) 
			cmd += " --truncateTable";

		// --batchSize
		num = _ttBatchSize_spm.getNumber().intValue();
		if (num != SqlStatementCmdLoadFile.DEFAULT_TRAN_BATCH_SIZE)
			cmd += " --batchSize " + num;

		// --sendBatchSize
		num = _ttSendBatchSize_spm.getNumber().intValue();
		if (num != SqlStatementCmdLoadFile.DEFAULT_SEND_BATCH_SIZE)
			cmd += " --sendBatchSize " + num;

		// input file name
		str = _sfName_txt.getText();
		if (StringUtil.hasValue(str)) 
			cmd += " '" + str + "'";

		
		_sqlWinCmd_txt.setText("\\loadfile"+cmd);
	}
//
//		usage: loadfile [options] -T tablename filename
//		   
//		description: 
//		  Reads a file and insert data into a table.
//		  This is used to import (a lot of rows) from plain files.
//		  The file can be in CSV (Comma Separated Value) format
//		   
//		options: 
//		  -T,--tablename               Name of the table to insert into.
//		  -l,--listJavaCharSets        List Available Java Character sets.
//		  -s,--skipProblemRows         If insert to a specific row has problems
//		                               simply skip this row and continue with next one.
//		                               Note: this will set batchsize=0 and sendbatchsize=1
//		  -n,--noHeader                The input file doesnt have column header DEFAULT=false
//		  -f,--field_terminator <str>  Character between fields        DEFAULT=,
//		  -r,--row_terminator <str>    Character(s) to terminate a row DEFAULT=\n
//		  -N,--nullValue <str>         NULL Value representation       DEFAULT=<NULL>
//		  -c,--charset <name>          File content Characterset name  DEFAULT=guessed by the content
//		  -C,--checkAndStop            Try to add first record, but then rollback
//		                               Note: this can be used to check if it will work.
//		  -B,--batchSize               Commit every X record           DEFAULT=0, All in One tran
//		  -b,--sendBatchSize           Send batch of records           DEFAULT=1000
//		  -t,--truncateTable           Truncate or delete records from the table at start.
//		  -m,--columnMapping <str>     Redirect columns in file to table column
//		                               Example: '1>c2, 2>c1, 3>c3, 4>c4'
//		                               If file has field names they can be specified...
//		                               Example: 'fname>c2, lname>c1, phone>c3, address>c4'
//		  -P,--preview                 Preview the file, read first 10 rows, then quit.
//		  

	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

		// --- FIELD: Filename ... ---
		if (_sfName_txt.equals(source))
		{
			_sfNameTestLoad_but.doClick();
		}
		
		// --- BUTTON: File ... ---
		if (_sfName_but.equals(source))
		{
			String lastOpenedFile = Configuration.getCombinedConfiguration().getProperty(PROPKEY_lastKnownFile, DEFAULT_lastKnownFile);
			File lastDir = new File(lastOpenedFile);
			if (lastDir.isFile())
				lastDir = lastDir.getParentFile();
			
			JFileChooser fc = new JFileChooser(lastDir);
			fc.setDialogType(JFileChooser.OPEN_DIALOG);
	
			int returnVal = fc.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) 
			{
				String selectedFileStr = fc.getSelectedFile().getAbsolutePath();
				
				Configuration saveCfg =  Configuration.getInstance(Configuration.USER_TEMP);
				if (saveCfg != null)
				{
					saveCfg.setProperty(PROPKEY_lastKnownFile, selectedFileStr);
					saveCfg.save();
				}
				_sfName_txt.setText(selectedFileStr);
				_sfNameTestLoad_but.doClick();
			}
		}
		
		// --- BUTTON: Test Load File ... ---
		if (_sfNameTestLoad_but.equals(source))
		{
			String filename = _sfName_txt.getText().trim();
			
			// - Open Source File
			// - read X bytes... try to figure out the file encoding
			// - from the guessed FileEncoding... set the field 'Source file encoding'
			// - load first 100 rows in the preview text field.
			try
			{
				String guessedEncoding = FileUtils.getFileEncoding(filename);
				String encoding        = guessedEncoding;
				if (encoding == null)
					encoding = Charset.defaultCharset().name();
System.out.println("XXXX: guessedEncoding='"+guessedEncoding+"', encoding='"+encoding+"'.");

				// Read 64 K from file
				String firstRawChunk    = FileUtils.readFile(filename, encoding, 64*1024);
				
				// Read first 100 rows
				boolean noheader = false;
				String  nullStr  = "<NULL>";
				if (StringUtil.hasValue(_sfNullValueStr_txt.getText()))
					nullStr = _sfNullValueStr_txt.getText();
				if (_sfEmtyStrIsNull_chk.isSelected())
					nullStr = "";
				_sourcefilePreviewObj = SqlStatementCmdLoadFile.readFirstRows(filename, encoding, _previewFirstRowCount, noheader, nullStr);
				String firstParsedChunk = _sourcefilePreviewObj.toTableString();

				// Set values 
				_sfPreviewRaw_txt   .setText(firstRawChunk);
				_sfPreviewParsed_txt.setText(firstParsedChunk);
				_sfPreviewRaw_txt   .setCaretPosition(0);
				_sfPreviewParsed_txt.setCaretPosition(0);

				_sfGuessedEncoding_txt.setText(guessedEncoding);
				_sfEncoding_cbx.setSelectedItem(encoding);
			}
			catch(Exception ex)
			{
				SwingUtils.showErrorMessage(this, "Error reading file", "Problems reading file '"+filename+"'.", ex);
			}
		}
		

		// --- BUTTON: '...' in Target Table ---
		if (_ttName_but.equals(source))
		{
			action_searchTargetTable();
		}

		// --- BUTTON: 'Create...' in Target Table ---
		if (_ttCreate_but.equals(source))
		{
			DbxConnection conn = _connProvider.getConnection();
			if (conn == null)
			{
				SwingUtils.showErrorMessage(this, "No Connection to any DBMS.", "No Connection to any DBMS.", null);
			}
			else
			{
				SwingUtils.showErrorMessage(this, "not yet implemented", "Sotrry not yet implemeted.\nTODO: Create table from CSV Headers, and guess datatype.", null);
			}
		}
		

		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			setVisible(false);
		}

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			setVisible(false);
		}
		
		setCmdPreview();
	}

	private void action_searchTargetTable()
	{
		if (_sourcefilePreviewObj == null)
		{
			SwingUtils.showErrorMessage(null, "No Source File has yet been selected", 
					"Source File Preview...\nChoose a Source File.", null);
			return;
		}

		
		DbxConnection conn = _connProvider.getConnection();
		if (conn == null)
		{
			SwingUtils.showErrorMessage(this, "No Connection to any DBMS.", "No Connection to any DBMS.", null);
			return;
		}

		try { _qic = conn.getMetaData().getIdentifierQuoteString(); }
		catch (SQLException ignore) {}

		String qic = _qic;
		if (qic == null)
			qic = _qicDefault;

		String dbmsVendor = "";
		try { dbmsVendor = conn.getMetaData().getDatabaseProductName(); }
		catch (SQLException ignore) {}
		
		boolean dbStoresUpperCaseIdentifiers = false;
		try { dbStoresUpperCaseIdentifiers = conn.getMetaData().storesUpperCaseIdentifiers(); }
		catch (SQLException ignore) {}
			

//		String inputStr = _ttName_txt.getText().trim();
//		SqlObjectName son = new SqlObjectName(inputStr, dbmsVendor, qic, dbStoresUpperCaseIdentifiers);
			
//		String catalog = son.hasCatalogName() ? son.getCatalogName() : "null";
//		String schema  = son.hasSchemaName()  ? son.getSchemaName()  : "null";
//		String table   = inputStr;

		String catalog = _ttCatalog_txt.getText();
		String schema  = _ttSchema_txt .getText();
		String table   = _ttTable_txt  .getText();
		
		
		ResultSetTableModel tabRstm = getTables(conn, catalog, schema, table, null);

		// Show a list
		SqlPickList pickList = new SqlPickList(getOwner(), tabRstm, "getTables()", false);
		pickList.setVisible(true);
		
		if (pickList.wasOkPressed())
		{
			catalog = pickList.getSelectedValuesAsString("TABLE_CAT");
			schema  = pickList.getSelectedValuesAsString("TABLE_SCHEM");
			table   = pickList.getSelectedValuesAsString("TABLE_NAME");

			String fullTableName = "";
			if (StringUtil.hasValue(catalog)) fullTableName += qic + catalog + qic + ".";
			if (StringUtil.hasValue(schema))  fullTableName += qic + schema  + qic + ".";
			fullTableName += qic + table + qic;

//			_ttName_txt.setText(fullTableName);
			if (conn.isDatabaseProduct(DbUtils.DB_PROD_NAME_SYBASE_ASE) || conn.isDatabaseProduct(DbUtils.DB_PROD_NAME_MSSQL))
			{
				fullTableName = "";
				if (StringUtil.hasValue(catalog)) fullTableName += "[" + catalog + "]" + ".";
				if (StringUtil.hasValue(schema))  fullTableName += "[" + schema  + "]" + ".";
				fullTableName += "[" + table + "]";
			}

			_ttCatalog_txt.setText(catalog);
			_ttSchema_txt .setText(schema);
			_ttTable_txt  .setText(table);
			
			TableModel tm = createColumnsTableModel(conn, catalog, schema, table);
			if (tm != null)
			{
				System.out.println("TableModel: rows="+tm.getRowCount()+", cols="+tm.getColumnCount());
				setTableModel(_ttJdbcMd_tab, tm);
				
				String simpleSelect = "select * from " + fullTableName;
				try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(simpleSelect) )
				{
					ResultSetTableModel rstm = new ResultSetTableModel(rs, null, false, "dummySelectForImport", _previewFirstRowCount, -1, false, null, null);
					setTableModel(_ttRs_tab, rstm);
					
					MapTableModel mtm = new MapTableModel(_ttMap_tab, _sourcefilePreviewObj, rstm);
					// set Model is done inside new MapTableModel()
					//setTableModel(_ttMap_tab, mtm);
				}
				catch(SQLException ex)
				{
					SwingUtils.showErrorMessage(this, "Get Target Table", "Problems getting first records from the DBMS.\n\nSQL:\n "+simpleSelect, ex);
				}
			}
		}
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
				_sfName_txt.requestFocus();
				
				_mainSplitPane.setDividerLocation(0.5);
			}
		};
		SwingUtilities.invokeLater(deferredAction);
	}
	
	/**
	 * 
	 * @param conn
	 * @param catalog
	 * @param schemaPattern
	 * @param valueNamePattern
	 * @param tableTypesStr default is to get type of TABLE
	 * @return
	 */
	public ResultSetTableModel getTables(DbxConnection conn, String catalog, String schemaPattern, String valueNamePattern, String tableTypesStr)
	{
		String[] tableTypes = new String[] {"TABLE"};

		if (StringUtil.isNullOrBlank(valueNamePattern))
			valueNamePattern = "";

		if (catalog         .equalsIgnoreCase("null")) catalog          = null;
		if (schemaPattern   .equalsIgnoreCase("null")) schemaPattern    = null;
		if (valueNamePattern.equalsIgnoreCase(""))     valueNamePattern = "%";
		if (tableTypesStr != null)
		{
    		if (tableTypesStr   .equalsIgnoreCase("null")) tableTypesStr    = null;
    		if (tableTypesStr   != null)                   tableTypes       = StringUtil.commaStrToArray(tableTypesStr);
		}

		try
		{
    		ResultSet rs = conn.getMetaData().getTables(catalog, schemaPattern, valueNamePattern, tableTypes);
    		ResultSetTableModel rstm = new ResultSetTableModel(rs, false, "JdbcMetaDataInfoDialog.TablesModel");
    		return rstm;
		}
		catch(SQLException ex)
		{
			SwingUtils.showErrorMessage(getOwner(), "Problems getting tables", "Problems getting tables", ex);
			return null;
		}
	}
	
	public TableModel createColumnsTableModel(DbxConnection conn, String catalog, String schemaPattern, String tablePattern)
	{
		if (catalog       != null && catalog      .equalsIgnoreCase("null")) catalog          = null;
		if (schemaPattern != null && schemaPattern.equalsIgnoreCase("null")) schemaPattern    = null;
		if (tablePattern  != null && tablePattern .equalsIgnoreCase(""))     tablePattern = "%";

//		String catalogDesc       = catalog           == null ? "null" : '"' + catalog          + '"';
//		String schemaPatternDesc = schemaPattern     == null ? "null" : '"' + schemaPattern    + '"';
//		String tablePatternDesc  = tablePattern      == null ? "null" : '"' + tablePattern + '"';
//		String columnTypesDesc   = columnNamePattern == null ? "null" : '"' + columnNamePattern + '"';
		
//		String apiCall = "parameters to getColumns("+catalogDesc+", "+schemaPatternDesc+", "+valueNamePatternDesc+", "+columnTypesDesc+")";
//		_co_api_lbl.setText(apiCall);

		System.out.println("parameters to getColumns("+catalog+", "+schemaPattern+", "+tablePattern+", %)");
		
		try
		{
			ResultSet rs = conn.getMetaData().getColumns(catalog, schemaPattern, tablePattern, "%");
			ResultSetTableModel rstm = new ResultSetTableModel(rs, "JdbcMetaDataInfoDialog.ColumnsModel");
			return rstm;
		}
		catch (Throwable e)
		{
			SwingUtils.showErrorMessage(getOwner(), "Problems getting columns", "Problems getting columns", e);
			return null;
		}
	}
	
	private static class MapTableModel
	extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;
		
		private ResultSetJXTable    _mapTable;
		private PreviewObject       _sourcefilePreviewObj = null;
		private ResultSetTableModel _targetTableRstm      = null;
		
		private ArrayList<MapTableEntry> _dataRows = new ArrayList<>();
		
		private static final String SELECT_A_FIELD = "<select a field>";

		private static final String ACTION_SKIP   = "skip";
		private static final String ACTION_IMPORT = "import";

		public MapTableModel(ResultSetJXTable mapTable, PreviewObject sourcefilePreviewObj, ResultSetTableModel targetTableRstm)
		{
			_mapTable = mapTable;
			_sourcefilePreviewObj = sourcefilePreviewObj;
			_targetTableRstm      = targetTableRstm;
			
			init();
		}

		private void init()
		{
//			JComboBox<String> target_cbx  = new JComboBox<>();
			JComboBox<String> source_cbx  = new JComboBox<>();
			JComboBox<String> actions_cbx = new JComboBox<>();

			actions_cbx.addItem(ACTION_SKIP);
			actions_cbx.addItem(ACTION_IMPORT);

			for (String srcField : _sourcefilePreviewObj._fileColList)
				source_cbx.addItem(srcField);
			
			for (int c=0; c<_targetTableRstm.getColumnCount(); c++)
			{
				MapTableEntry e = new MapTableEntry();
				
				String targetCol = _targetTableRstm.getColumnName(c);
				String sourceFld = SELECT_A_FIELD;
				String action    = "skip";
				
				if (containsIgnoreCase(targetCol, _sourcefilePreviewObj._fileColList))
				{
					sourceFld = targetCol;
					action    = "import";
				}
				
				e._targetCol = targetCol;
				e._sourceFld = sourceFld;
				e._action    = action;
				
				_dataRows.add(e);
			}

			// This needs to be done here, othewise we can not do setCellEditor() below
			_mapTable.setModel(this);
			_mapTable.packAll(); // set size so that all content in all cells are visible
			_mapTable.setColumnControlVisible(true);
			_mapTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

			// Set Cell Editors
			_mapTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(source_cbx));
			_mapTable.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(actions_cbx));
		}
		
		private boolean containsIgnoreCase(String checkStr, List<String> list)
		{
			for(String str : list)
			{
				if(str.equalsIgnoreCase(checkStr))
					return true;
			}
			return false;
		}
		
		@Override
		public int getRowCount()
		{
			return _dataRows.size();
		}

		@Override
		public int getColumnCount()
		{
			return 4;
		}
		
		@Override
		public String getColumnName(int col)
		{
			switch (col)
			{
			case 0:  return "Target Column";
			case 1:  return "Source Field";
			case 2:  return "Action";
			case 3:  return "xxx";
			default: return "col-"+col;
			}
		}
		
		@Override
		public boolean isCellEditable(int row, int col)
		{
			switch (col)
			{
			case 1: return true;
			case 2: return true;
			}
			return false;
		}

		@Override
		public void setValueAt(Object val, int row, int col)
		{
			System.out.println("val=|"+val+"| class="+val.getClass().getName());
			
			MapTableEntry e = _dataRows.get(row);
			
			if (col == 1)
			{
				e._sourceFld = val.toString();

				if ( ! SELECT_A_FIELD.equals(e._sourceFld) )
					e._action = ACTION_IMPORT;
			}
			
			if (col == 2)
			{
				e._action = val.toString();
			}
		}
		
		@Override
		public Object getValueAt(int row, int col)
		{
			MapTableEntry e = _dataRows.get(row);
			switch (col)
			{
			case 0: return e._targetCol;
			case 1: return e._sourceFld;
			case 2: return e._action;
			case 3: return "";
			}
			return "row="+row+",col="+col;
		}
	}
	private static class MapTableEntry
	{
		String _targetCol;
		String _sourceFld;
		String _action;
	}
}
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

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.gui.swing.GTable;
import com.asetune.gui.swing.GTableFilter;
import com.asetune.gui.swing.ListSelectionListenerDeferred;
import com.asetune.sql.ResultSetMetaDataCached;
import com.asetune.sql.ResultSetMetaDataCached.Entry;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.ddl.DataTypeNotResolvedException;
import com.asetune.sql.ddl.IDbmsDdlResolver;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.asetune.utils.ColorUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class ResultSetMetaDataViewDialog
extends JDialog
implements ActionListener
{
	private static final long serialVersionUID = 1L;

	private static final String DIALOG_TITLE  = "ResultSet MetaData View";
	private static final String CHOOSE_VALUES = "<Select DBMS Vendor>";

	private Window _owner;

	private JPanel  _descPanel;
	private JPanel  _originRsmdPanel;
	private JPanel  _sourceRsmdPanel;
	private JPanel  _targetRsmdPanel;
	private JPanel  _targetDdlPanel;
	private JSplitPane _targetSplitPanel;
	private JPanel  _okCancelPanel;

	private LocalTable  _origin_tab;
	private LocalTable  _source_tab;
	private LocalTable  _target_tab;

	private JLabel            _origin_lbl = new JLabel("DBMS Vendor");
	private JTextField        _origin_txt = new JTextField();
	private JLabel            _source_lbl = new JLabel("DBMS Vendor");
	private JTextField        _source_txt = new JTextField();
	private JLabel            _target_lbl = new JLabel("DBMS Vendor");
//	private JComboBox<String> _source_cbx;
	private JComboBox<String> _target_cbx;

	private RSyntaxTextAreaX  _targetDdlText_txt    = new RSyntaxTextAreaX();
	private RTextScrollPane   _targetDdlText_scroll = new RTextScrollPane(_targetDdlText_txt);

	private GTableFilter _tableFilter = new GTableFilter(null, GTableFilter.ROW_COUNT_LAYOUT_LEFT, true);
	
	private JButton _close = new JButton("Close");

	private ResultSetTableModel _rstm;
	private String              _originDbmsProductName;
	
	private ResultSetMetaDataCached _originRsmdCache;
	private ResultSetMetaDataCached _sourceRsmdCache;
	private ResultSetMetaDataCached _targetRsmdCache;
	

	public ResultSetMetaDataViewDialog(JComponent owner, ResultSetTableModel rstm)
	{
		this(SwingUtils.getParentWindow(owner), rstm);
	}

	public ResultSetMetaDataViewDialog(Window owner, ResultSetTableModel rstm)
	{
		super(owner, DIALOG_TITLE, ModalityType.MODELESS);
		
		_owner = owner;
		_rstm  = rstm;

		_originDbmsProductName = rstm.getOriginDbmsProductName();

		initComponents();
		pack();
		
		// Focus to 'OK', escape to 'CANCEL'
		SwingUtils.installEscapeButton(this, _close);
		SwingUtils.setFocus(_close);
		
		setLocationRelativeTo(owner);
		
		this.addWindowListener(new java.awt.event.WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				saveProps();
			}
		});

		loadProps();
	}


	private void initComponents()
	{
		JPanel panel = new JPanel();
		//panel.setLayout(new MigLayout("debug, insets 0 0 0 0, wrap 1","",""));   // insets Top Left Bottom Right
		panel.setLayout(new MigLayout("insets 0","",""));   // insets Top Left Bottom Right

		_descPanel       = createDescPanel();
		_originRsmdPanel = createOriginRsmdPanel();
		_sourceRsmdPanel = createSourceRsmdPanel();
		_targetRsmdPanel = createTargetRsmdPanel();
		_targetDdlPanel  = createTargetDdlPanel();
		_okCancelPanel   = createOkCancelPanel();

		_targetSplitPanel = new JSplitPane();
		_targetSplitPanel.setLeftComponent( _targetRsmdPanel);
		_targetSplitPanel.setRightComponent(_targetDdlPanel);
		_targetSplitPanel.setDividerLocation(0.50);
		
		panel.add(_descPanel,        "growx, pushx, gapafter 0, wrap");
		panel.add(_originRsmdPanel,  "grow, push, wrap");
		panel.add(_sourceRsmdPanel,  "grow, push, wrap");
//		panel.add(_targetRsmdPanel,  "split, grow, push");
//		panel.add(_targetDdlPanel,   "grow, push, wrap");
		panel.add(_targetSplitPanel, "grow, push, wrap");
		panel.add(_okCancelPanel,    "growx, pushx, wrap");

		// connect "the other" tables to the same filter
		_tableFilter.setTable(_origin_tab); // Set
		_tableFilter.addTable(_source_tab); // Also link this table to the filter
		_tableFilter.addTable(_target_tab); // Also link this table to the filter
		
		// let all the tables know about each other... so they can interact
		_origin_tab._originTable = _origin_tab;
		_origin_tab._sourceTable = _source_tab;
		_origin_tab._targetTable = _target_tab;
		_origin_tab._targetDdlTxt= _targetDdlText_txt;

		_source_tab._originTable = _origin_tab;
		_source_tab._sourceTable = _source_tab;
		_source_tab._targetTable = _target_tab;
		_source_tab._targetDdlTxt= _targetDdlText_txt;

		_target_tab._originTable = _origin_tab;
		_target_tab._sourceTable = _source_tab;
		_target_tab._targetTable = _target_tab;
		_target_tab._targetDdlTxt= _targetDdlText_txt;

		// set content
		setContentPane(panel);
	}

	private JPanel createDescPanel()
	{
		JPanel panel = SwingUtils.createPanel("Description", false, new MigLayout("","","")); // insets top left bottom right

		JLabel desc = new JLabel("<html>"
				+ "Shows 3 different Tables with ResultSet MetaData in various ways.<br>"
				+ "   &nbsp; <b>Origin</b> - The original/unchanged ResultSetMetaData object, and all it's values.<br>"
				+ "   &nbsp; <b>Source</b> - Here rows might have changed som entries, due to DBMS Data Type Normalization. For Oracle, various Number lengths, will be switched into java.sql.Types.INTEGER instead. For Sybase, 'unsigned int' values will be <i>bumped up</i> to Types.BIGINT.<br>"
				+ "   &nbsp; <b>Target</b> - Here you can choose what DBMS Target you want to see. The important column to look at is 'Column Resolved Type' which is the DBMS Data Type that will be used if DbxTune creates tables etc...<br>"
				+ "</html>");
		
		panel.add(desc,          "growx, pushx, wrap");
//		panel.add(_tableFilter,  "growx, pushx, wrap");

		return panel;
	}
	
	private JPanel createOriginRsmdPanel()
	{
		JPanel panel = SwingUtils.createPanel("Origin RS MetaData", true, new MigLayout("","",""));

//		JLabel desc = new JLabel("<html></html>"); 

		_origin_txt.setText( _originDbmsProductName );
		_origin_txt.setEditable(false);

		_originRsmdCache = _rstm.getResultSetMetaDataCached();
		
		ResultSetMetaDataTableModel tm = new ResultSetMetaDataTableModel(_originRsmdCache);
		
		_origin_tab = new LocalTable("ResultSetMetaDataViewDialog.origin");
		_origin_tab.setModel(tm);

//		_origin_tab.setShowGrid(false);
		_origin_tab.setSortable(true);
		_origin_tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_origin_tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		_origin_tab.packAll(); // set size so that all content in all cells are visible
		_origin_tab.setColumnControlVisible(true);
		_origin_tab.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);

//		_tableFilter = new GTableFilter(_origin_tab, GTableFilter.ROW_COUNT_LAYOUT_LEFT, true);
		
		JScrollPane tab_scroll = new JScrollPane(_origin_tab);

//		panel.add(desc,          "wrap");
		panel.add(_origin_lbl,   "split");
		panel.add(_origin_txt,   "growx, pushx, wrap");
		panel.add(_tableFilter,  "growx, pushx, wrap");
		panel.add(tab_scroll,    "grow, push, wrap");

		// ADD ACTIONS TO COMPONENTS
//		_close.addActionListener(this);

		return panel;
	}

	private JPanel createSourceRsmdPanel()
	{
		JPanel panel = SwingUtils.createPanel("Source RS MetaData", true, new MigLayout("","",""));

		JLabel desc = new JLabel("<html>Note: Yellow marked cells has been modified/translated from the <i>origin</i> meta data.</html>");

//		List<String> dbmsVendorList = DbxConnection.getDbmsDdlSupportedVendors();
//		dbmsVendorList.add(0, CHOOSE_VALUES);
//		_source_cbx = new JComboBox<>(dbmsVendorList.toArray(new String[0]));
		
		_source_txt.setText( _originDbmsProductName );
		_source_txt.setEditable(false);


		_sourceRsmdCache = ResultSetMetaDataCached.createNormalizedRsmd(_originRsmdCache, _originDbmsProductName);
		
		final ResultSetMetaDataTableModel tm = new ResultSetMetaDataTableModel(_sourceRsmdCache);

		_source_tab = new LocalTable("ResultSetMetaDataViewDialog.source");
		_source_tab.setModel(tm);

//		_source_tab.setShowGrid(false);
		_source_tab.setSortable(true);
		_source_tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_source_tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		_source_tab.packAll(); // set size so that all content in all cells are visible
		_source_tab.setColumnControlVisible(true);
		_source_tab.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);

		// COLOR CODE SOME ROWS/CELLS
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		if (conf != null) colorStr = conf.getProperty("ResultSetMetaDataViewDialog.color.altered");
		_source_tab.addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Entry entry = tm.getEntryForRow(_source_tab.convertRowIndexToModel(adapter.row));
				int   mcol  = _source_tab.convertColumnIndexToModel(adapter.column);
				int   bmap  = (int) ResultSetMetaDataTableModel.COLUMN_HEADERS[mcol][ResultSetMetaDataTableModel.COL_CHANGE_BIT];
				if ( entry.wasChanged(bmap) )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, ColorUtils.LIGHT_YELLOW), null));
		
		JScrollPane tab_scroll = new JScrollPane(_source_tab);
		
		panel.add(desc,          "wrap");
		panel.add(_source_lbl,   "split");
		panel.add(_source_txt,   "growx, pushx, wrap");
		panel.add(tab_scroll,    "grow, push, wrap");

		// ADD ACTIONS TO COMPONENTS
//		_source_cbx.addActionListener(this);

		return panel;
	}


	private JPanel createTargetRsmdPanel()
	{
		JPanel panel = SwingUtils.createPanel("Target RS MetaData", true, new MigLayout("","",""));

		JLabel desc = new JLabel("<html>Choose any Target DBMS Vendor to see how Data Types are <b>translated</b> to that specific DBMS Vendor.</html>"); 
		
		List<String> dbmsVendorList = DbxConnection.getDbmsDdlSupportedVendors();
		dbmsVendorList.add(0, CHOOSE_VALUES);
		_target_cbx = new JComboBox<>(dbmsVendorList.toArray(new String[0]));

		_target_tab = new LocalTable("ResultSetMetaDataViewDialog.target");
		
//		_target_tab.setShowGrid(false);
		_target_tab.setSortable(true);
		_target_tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_target_tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		_target_tab.packAll(); // set size so that all content in all cells are visible
		_target_tab.setColumnControlVisible(true);
		_target_tab.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);

		JScrollPane tab_scroll = new JScrollPane(_target_tab);

		panel.add(desc,          "wrap");
		panel.add(_target_lbl,   "split");
		panel.add(_target_cbx,   "growx, pushx, wrap");
		panel.add(tab_scroll,    "span, grow, push, wrap");

		// ADD ACTIONS TO COMPONENTS
		_target_cbx.addActionListener(this);

		// Entries in Combobox
		_target_cbx.setMaximumRowCount(30);

		// set auto completion
//		AutoCompleteDecorator.decorate(_target_cbx);

		return panel;
	}

	private JPanel createTargetDdlPanel()
	{
		JPanel panel = SwingUtils.createPanel("Target DDL", true, new MigLayout("","",""));

		JLabel desc = new JLabel("<html>This is how a 'create table' DDL statement will look like with the Target DBMS Vendor</html>"); 

		_targetDdlText_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);

		panel.add(desc,                  "wrap");
		panel.add(_targetDdlText_scroll, "grow, push, wrap");

		// ADD ACTIONS TO COMPONENTS

		return panel;
	}

	private JPanel createOkCancelPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0","",""));   // insets Top Left Bottom Right

		// ADD the OK, Cancel, Apply buttons
		panel.add(new JLabel(),   "growx, pushx");
		panel.add(_close,         "gapright 10, tag ok, right");

		// ADD ACTIONS TO COMPONENTS
		_close.addActionListener(this);

		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		
		if (_close.equals(source))
		{
			saveProps();
			this.setVisible(false);
		}
		
//		if (_source_cbx.equals(source))
//		{
//			String dbmsProductName = _source_cbx.getSelectedItem() + "";
//
//			if (dbmsProductName.equals(CHOOSE_VALUES))
//			{
//				_target_tab.setModel( new DefaultTableModel() );
//			}
//			else
//			{
//				ResultSetMetaDataCached rsmdc = _rstm.getResultSetMetaDataCached();
//				ResultSetMetaDataCached newRsmdc = ResultSetMetaDataCached.createNormalizedRsmd(rsmdc, dbmsProductName);
//				
//				ResultSetMetaDataTableModel tm = new ResultSetMetaDataTableModel(newRsmdc);
//
//				_source_tab.setModel(tm);
//				_source_tab.packAll(); // set size so that all content in all cells are visible
//			}
//		}

		if (_target_cbx.equals(source))
		{
			String dbmsProductName = _target_cbx.getSelectedItem() + "";
			
			if (dbmsProductName.equals(CHOOSE_VALUES))
			{
				_target_tab.setModel( new DefaultTableModel() );
				_targetDdlText_txt.setText("NO Target DBMS is selected");
			}
			else
			{
				if (_sourceRsmdCache == null)
				{
					SwingUtils.showErrorMessage(_owner, "NO Source ResultSet MetaData", "NO Source ResultSet MetaData has yet been produced.", null);
				}
				else
				{
					try
					{
						boolean firstTime = _targetRsmdCache == null;
						
						_targetRsmdCache = ResultSetMetaDataCached.transformToTargetDbms(_sourceRsmdCache, dbmsProductName);
						
						ResultSetMetaDataTableModel tm = new ResultSetMetaDataTableModel(_targetRsmdCache);
						_target_tab.setModel(tm);

						// --- Only SHOW some columns in this table model.
						if (firstTime)
							_target_tab.setVisibleColumns(ResultSetMetaDataTableModel.TARGET_visible_columns);
						
						_target_tab.packAll();      // set size so that all content in all cells are visible
						_tableFilter.applyFilter(); // if we have any filters apply those to the new rows in _target_tab

					
						// Transform Source MetaData to TARGET
						IDbmsDdlResolver dbmsDdlResolver = DbxConnection.createDbmsDdlResolver(dbmsProductName);
						String crTabSql = dbmsDdlResolver.ddlTextTable(_targetRsmdCache);
						
						_targetDdlText_txt.setText(crTabSql);
						_targetDdlText_txt.setCaretPosition(0);
					}
					catch (DataTypeNotResolvedException ex)
					{
						SwingUtils.showErrorMessage(_owner, "Transform to Target DBMS Problems", 
								"<html>Problems when tranforming DBMS data types to Target DBMS Vendor '" + dbmsProductName + "'.<br><br>" +
								"<b>Exception Message:</b><br>" + 
								ex.getMessage() + "</html>", ex);
					}
				}
			}
		}
	}

	/*---------------------------------------------------
	** BEGIN: Property handling
	**---------------------------------------------------
	*/
	private void saveProps()
  	{
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		String base = this.getClass().getSimpleName() + ".";

		if (tmpConf != null)
		{
			tmpConf.setLayoutProperty(base + "window.width", this.getSize().width);
			tmpConf.setLayoutProperty(base + "window.height", this.getSize().height);
			tmpConf.setLayoutProperty(base + "window.pos.x", this.getLocationOnScreen().x);
			tmpConf.setLayoutProperty(base + "window.pos.y", this.getLocationOnScreen().y);

			tmpConf.save();
		}
  	}

  	private void loadProps()
  	{
		int     width     = SwingUtils.hiDpiScale(1600);  // initial window with   if not opened before
		int     height    = SwingUtils.hiDpiScale(1024);   // initial window height if not opened before
		int     x         = -1;
		int     y         = -1;

//		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
		Configuration tmpConf = Configuration.getCombinedConfiguration();
		String base = this.getClass().getSimpleName() + ".";

		setSize(width, height);

		if (tmpConf == null)
			return;

		width  = tmpConf.getLayoutProperty(base + "window.width",  width);
		height = tmpConf.getLayoutProperty(base + "window.height", height);
		x      = tmpConf.getLayoutProperty(base + "window.pos.x",  -1);
		y      = tmpConf.getLayoutProperty(base + "window.pos.y",  -1);

		if (width != -1 && height != -1)
		{
			setSize(width, height);
		}
		if (x != -1 && y != -1)
		{
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
				this.setLocation(x, y);
		}
		else
		{
			SwingUtils.centerWindow(this);
		}
	}
	/*---------------------------------------------------
	** END: Property handling
	**---------------------------------------------------
	*/

  	private static class LocalTable
	extends GTable
	{
		private static final long serialVersionUID = 1L;

		private LocalTable       _originTable;
		private LocalTable       _sourceTable;
		private LocalTable       _targetTable;
		private RSyntaxTextAreaX _targetDdlTxt;

		public LocalTable(String name)
		{
			super(name);
			
			getSelectionModel().addListSelectionListener(new ListSelectionListenerDeferred(10)
//			getSelectionModel().addListSelectionListener(new ListSelectionListener()
			{
				@Override
				public void deferredValueChanged(ListSelectionEvent event)
//				public void valueChanged(ListSelectionEvent event)
				{
					int vrow = getSelectedRow();
					int sqlColNum = -1;
					if (vrow >= 0)
						sqlColNum = getValueAsInteger(vrow, ResultSetMetaDataTableModel.COLUMN_NUMBER_HEADER);

					if (LocalTable.this.equals(_originTable))
					{
//						System.out.println("ORIGIN TABLE: name='"+name+"', row="+row+", selectedVRow="+vrow+", getFirstIndex="+fvrow);
					//	setCompanionSelection("ORIGIN", _originTable, sqlColNum);
						setCompanionSelection("ORIGIN", _sourceTable, sqlColNum);
						setCompanionSelection("ORIGIN", _targetTable, sqlColNum);
					}
					else if (LocalTable.this.equals(_sourceTable))
					{
//						System.out.println("SOURCE TABLE: name='"+name+"', row="+row+", selectedVRow="+vrow+", getFirstIndex="+fvrow);
						setCompanionSelection("SOURCE", _originTable, sqlColNum);
					//	setCompanionSelection("SOURCE", _sourceTable, sqlColNum);
						setCompanionSelection("SOURCE", _targetTable, sqlColNum);
					}
					else if (LocalTable.this.equals(_targetTable))
					{
//						System.out.println("TARGET TABLE: name='"+name+"', row="+row+", selectedVRow="+vrow+", getFirstIndex="+fvrow);
						setCompanionSelection("TARGET", _originTable, sqlColNum);
						setCompanionSelection("TARGET", _sourceTable, sqlColNum);
					//	setCompanionSelection("TARGET", _targetTable, sqlColNum);
					}
				}
		    });
		}

		private int getViewRowForSqlColNum(int sqlColNum)
		{
			for (int r=0; r<getRowCount(); r++)
			{
				int rowSqlColNum = getValueAsInteger(r, ResultSetMetaDataTableModel.COLUMN_NUMBER_HEADER);
				if (rowSqlColNum == sqlColNum)
					return r;
			}
			return -1;
		}
		private void setCompanionSelection(String origin, LocalTable tab, int sqlColNum)
		{
			if (tab == null)
				return;

			boolean select = sqlColNum >= 0;

			if (select)
			{
				int vrow = tab.getViewRowForSqlColNum(sqlColNum);

				if (vrow >= 0)
				{
					tab.setRowSelectionInterval(vrow, vrow);
					tab.scrollRectToVisible( tab.getCellRect(vrow, 0, true));
					
					String str = tab.getValueAsString(vrow, ResultSetMetaDataTableModel.COLUMN_LABEL_HEADER);
					SearchContext context = new SearchContext( "[" + str + "]");
					context.setMarkAll(true);
					context.setMatchCase(true);
					context.setWholeWord(true);
					SearchEngine.markAll(_targetDdlTxt, context);
				}
				else
				{
					tab.getSelectionModel().clearSelection();

					SearchContext context = new SearchContext();
					context.setMarkAll(true);
					context.setMatchCase(true);
					context.setWholeWord(true);
					SearchEngine.markAll(_targetDdlTxt, context);
				}
			}
			else
			{
				tab.getSelectionModel().clearSelection();

				SearchContext context = new SearchContext();
				context.setMarkAll(true);
				context.setMatchCase(true);
				context.setWholeWord(true);
				SearchEngine.markAll(_targetDdlTxt, context);
			}
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

					tip = ResultSetMetaDataTableModel.getToolTipText(mcol);

					return tip;
				}
			};

			return tabHeader;
		}
		
		
	}
	
	private static class ResultSetMetaDataTableModel
	extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;
		
		private static final String COLUMN_NUMBER_HEADER = "Col#";
		private static final String COLUMN_LABEL_HEADER  = "Column Label";
		
		public static String[] TARGET_visible_columns = new String[] {COLUMN_NUMBER_HEADER, "Column Label", "Column Resolved Type", "Column Type Str"};

		private static final int COL_NAME        = 0;
		private static final int COL_EDITABLE    = 1;
		private static final int COL_CLASS       = 2;
		private static final int COL_CHANGE_BIT  = 3;
		private static final int COL_TOOLTIP     = 4;
		private static Object[][] COLUMN_HEADERS = 
		{
		//   ColumnName,               Editable, JTable type,   ChangeBit                         Tooltip
		//   ------------------------- --------- -------------- --------------------------------- --------------------------------------------
			{"Catalog"               , true    , String .class, Entry.CHANGED_catalogName       , "<html> <code>String  ResultSetMetaData.getCatalogName()      </code> <br><br> Gets the designated column's table's catalog name.  </html>"},
			{"Schema"                , true    , String .class, Entry.CHANGED_schemaName        , "<html> <code>String  ResultSetMetaData.getSchemaName()       </code> <br><br> Get the designated column's table's schema.  </html>"},
			{"Table Name"            , true    , String .class, Entry.CHANGED_tableName         , "<html> <code>String  ResultSetMetaData.getTableName()        </code> <br><br> Gets the designated column's table name.  </html>"},
			{COLUMN_NUMBER_HEADER    , true    , Integer.class, 0                               , "<html>                                                                        Column Number </html>"},
			{"Column Name"           , true    , String .class, Entry.CHANGED_columnName        , "<html> <code>String  ResultSetMetaData.getColumnName()       </code> <br><br> Get the designated column's name.  </html>"},
			{"Column Label"          , true    , String .class, Entry.CHANGED_columnLabel       , "<html> <code>String  ResultSetMetaData.getColumnLabel()      </code> <br><br> Gets the designated column's suggested title for use in printouts and displays.<br> The suggested title is usually specified by the SQL AS clause.<br> If a SQL AS is not specified, the value returned from getColumnLabel will be the same as the value returned by the getColumnName method.  </html>"},
			{"Column Resolved Type"  , true    , String .class, 0                               , "<html>                                                                        Resolved DBMS Vendor (may be translated via the DbmsDataTypeRosolver functionality. <br> This is DBMS Data Types that inclused type length etc. <br> For example: <code>varchar(50)</code>  </html>"},
			{"Column Type Name"      , true    , String .class, Entry.CHANGED_columnTypeName    , "<html> <code>String  ResultSetMetaData.getColumnTypeName()   </code> <br><br> Retrieves the designated column's database-specific type name. (DBMS Vendors SQL Data Type Name without length/scale  </html>"},
			{"Column Display Size"   , true    , Integer.class, Entry.CHANGED_columnDisplaySize , "<html> <code>int     ResultSetMetaData.getColumnDisplaySize()</code> <br><br> Indicates the designated column's normal maximum width in characters.  </html>"},
			{"Precision"             , true    , Integer.class, Entry.CHANGED_precision         , "<html> <code>int     ResultSetMetaData.getPrecision()        </code> <br><br> Get the designated column's specified column size. <br> - For numeric data, this is the maximum precision. <br> - For character data, this is the length in characters. <br> - For datetime datatypes, this is the length in characters of the String representation (assuming the maximum allowed precision of the fractional seconds component). <br> - For binary data, this is the length in bytes. <br> - For the ROWID datatype, this is the length in bytes. <br> - 0 is returned for data types where the column size is not applicable.  </html>"},
			{"Scale"                 , true    , Integer.class, Entry.CHANGED_scale             , "<html> <code>int     ResultSetMetaData.getScale()            </code> <br><br> Gets the designated column's number of digits to right of the decimal point. <br> 0 is returned for data types where the scale is not applicable.  </html>"},
			{"is Nullable"           , true    , Boolean.class, Entry.CHANGED_nullable          , "<html> <code>boolean ResultSetMetaData.isNullable()          </code> <br><br> Indicates the nullability of values in the designated column.  </html>"},
			{"Column Type Str"       , true    , String .class, 0                               , "<html>                                                                        A String representation of the JDBC's SQL type. (<code>java.sql.Types.<i>DATATYPE</i></code>)  </html>"},
			{"Column Type"           , true    , Integer.class, Entry.CHANGED_columnType        , "<html> <code>int     ResultSetMetaData.getColumnType()       </code> <br><br> Retrieves the designated column's SQL type. (the number representation of: java.sql.Types.DATATYPE)  </html>"},
			{"is Signed"             , true    , Boolean.class, Entry.CHANGED_signed            , "<html> <code>boolean ResultSetMetaData.isSigned()            </code> <br><br> Indicates whether values in the designated column are signed numbers.  </html>"},
			{"is Auto Increment"     , true    , Boolean.class, Entry.CHANGED_autoIncrement     , "<html> <code>boolean ResultSetMetaData.isAutoIncrement()     </code> <br><br> Indicates whether the designated column is automatically numbered.  </html>"},
			{"is Case Sensitive"     , true    , Boolean.class, Entry.CHANGED_caseSensitive     , "<html> <code>boolean ResultSetMetaData.isCaseSensitive()     </code> <br><br> Indicates whether a column's case matters.  </html>"},
			{"is Searchable"         , true    , Boolean.class, Entry.CHANGED_searchable        , "<html> <code>boolean ResultSetMetaData.isSearchable()        </code> <br><br> Indicates whether the designated column can be used in a where clause.  </html>"},
			{"is ReadOnly"           , true    , Boolean.class, Entry.CHANGED_readOnly          , "<html> <code>boolean ResultSetMetaData.isReadOnly()          </code> <br><br> Indicates whether the designated column is definitely <b>not</b> writable.  </html>"},
			{"is Writable"           , true    , Boolean.class, Entry.CHANGED_writable          , "<html> <code>boolean ResultSetMetaData.isWritable()          </code> <br><br> Indicates whether it is possible for a write on the designated column to succeed.  </html>"},
			{"is Definitely Writable", true    , Boolean.class, Entry.CHANGED_definitelyWritable, "<html> <code>boolean ResultSetMetaData.isDefinitelyWritable()</code> <br><br> Indicates whether a write on the designated column will definitely succeed.  </html>"},
			{"is Currency"           , true    , Boolean.class, Entry.CHANGED_currency          , "<html> <code>boolean ResultSetMetaData.isCurrency()          </code> <br><br> Indicates whether the designated column is a cash value.  </html>"},
			{"Column Class Name"     , true    , String .class, Entry.CHANGED_columnClassName   , "<html> <code>String  ResultSetMetaData.getColumnClassName()  </code> <br><br> Returns the fully-qualified name of the Java class whose instances are manufactured if the method ResultSet.getObject is called to retrieve a value from the column. ResultSet.getObject may return a subclass of the class returned by this method.  </html>"}
		};

		public static String getToolTipText(int columnIndex)
		{
			return (String)COLUMN_HEADERS[columnIndex][COL_TOOLTIP]; 
		}


		private ResultSetMetaDataCached _rsmdc;

		public ResultSetMetaDataTableModel(ResultSetMetaDataCached rsmdc)
		{
			_rsmdc = rsmdc;
		}
		
		public Entry getEntryForRow(int row)
		{
			return _rsmdc.getEntry(row);
		}
		
		@Override
		public Object getValueAt(int row, int col)
		{
//System.out.println("getValueAt(row="+row+", col="+col+") ---------------------------- ");
			
			int sqlCol = row + 1;
			
			switch (col)
			{
			case  0: return _rsmdc.getCatalogName      (sqlCol);
			case  1: return _rsmdc.getSchemaName       (sqlCol);
			case  2: return _rsmdc.getTableName        (sqlCol);
			case  3: return sqlCol;
			case  4: return _rsmdc.getColumnName       (sqlCol);
			case  5: return _rsmdc.getColumnLabel      (sqlCol);
//			case  6: return _rsmdc.getGuessedDbmsType  (sqlCol);
			case  6: return _rsmdc.getColumnResolvedTypeName(sqlCol);
			case  7: return _rsmdc.getColumnTypeName   (sqlCol);
			case  8: return _rsmdc.getColumnDisplaySize(sqlCol);
			case  9: return _rsmdc.getPrecision        (sqlCol);
			case 10: return _rsmdc.getScale            (sqlCol);
			case 11: return _rsmdc.isNullableBoolean   (sqlCol);
			case 12: return _rsmdc.getColumnTypeStr    (sqlCol);
			case 13: return _rsmdc.getColumnType       (sqlCol);
			case 14: return _rsmdc.isSigned            (sqlCol);
			case 15: return _rsmdc.isAutoIncrement     (sqlCol);
			case 16: return _rsmdc.isCaseSensitive     (sqlCol);
			case 17: return _rsmdc.isSearchable        (sqlCol);
			case 18: return _rsmdc.isReadOnly          (sqlCol);
			case 19: return _rsmdc.isWritable          (sqlCol);
			case 20: return _rsmdc.isDefinitelyWritable(sqlCol);
			case 21: return _rsmdc.isCurrency          (sqlCol);
			case 22: return _rsmdc.getColumnClassName  (sqlCol);
			}
			return "-unknown-col-" + col;
		}

		@Override
		public int getRowCount()
		{
//System.out.println("getRowCount(): " + _rsmdc.getColumnCount());
			return _rsmdc.getColumnCount();
		}

		@Override
		public int getColumnCount()
		{
			return COLUMN_HEADERS.length;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			return (Class<?>) COLUMN_HEADERS[columnIndex][COL_CLASS];
		}

		@Override
		public String getColumnName(int columnIndex)
		{
			return (String) COLUMN_HEADERS[columnIndex][COL_NAME];
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return (Boolean) COLUMN_HEADERS[columnIndex][COL_EDITABLE];
		}
	}
}

/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.tools.sqlw;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Types;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.RowSorter;
import javax.swing.event.HyperlinkEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.autocomplete.Completion;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.LabelProvider;
import org.jdesktop.swingx.renderer.StringValue;
import org.jdesktop.swingx.renderer.StringValues;
import org.jdesktop.swingx.table.TableColumnExt;
import org.mozilla.universalchardet.UniversalDetector;

import com.dbxtune.cm.CmToolTipSupplierDefault;
import com.dbxtune.cm.sqlserver.ToolTipSupplierSqlServer;
import com.dbxtune.gui.ResultSetMetaDataViewDialog;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.focusabletip.FocusableTip;
import com.dbxtune.gui.focusabletip.ResolverReturn;
import com.dbxtune.gui.focusabletip.ToolTipHyperlinkResolver;
import com.dbxtune.gui.swing.DeferredMouseMotionListener;
import com.dbxtune.gui.swing.GTableSortController;
import com.dbxtune.sql.SqlObjectName;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.ui.autocomplete.CompletionProviderAbstract;
import com.dbxtune.ui.autocomplete.CompletionProviderAbstractSql;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.JavaVersion;
import com.dbxtune.utils.JsonUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;
import com.dbxtune.xmenu.SqlSentryPlanExplorer;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;

public class ResultSetJXTable
extends JXTable
implements ToolTipHyperlinkResolver
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public final static Color NULL_VALUE_COLOR = new Color(240, 240, 240);

	public static final String  PROPKEY_TABLE_TOOLTIP_SHOW_ALL_COLUMNS = "ResultSetJXTable.table.tooltip.show.all.columns";
	public static final boolean DEFAULT_TABLE_TOOLTIP_SHOW_ALL_COLUMNS = true;

	public static final String  PROPKEY_TOOLTIP_XML_INLINE_MAX_SIZE_KB = "ResultSetJXTable.tooltip.xml.inline.max.sizeKb";
	public static final int     DEFAULT_TOOLTIP_XML_INLINE_MAX_SIZE_KB = 100;

	public static final String  PROPKEY_TOOLTIP_CELL_DISPLAY_MAX_SIZE_KB = "ResultSetJXTable.tooltip.cell.display.max.sizeKb";
	public static final int     DEFAULT_TOOLTIP_CELL_DISPLAY_MAX_SIZE_KB = 10 * 1024; // 10MB


	public static final String  PROPKEY_TABLE_CELL_RENDERER_TIMESTAMP       = "ResultSetJXTable.cellRenderer.format.Timestamp";
	public static final String  DEFAULT_TABLE_CELL_RENDERER_TIMESTAMP       = "yyyy-MM-dd HH:mm:ss.SSS";

	public static final String  PROPKEY_TABLE_CELL_RENDERER_DATE            = "ResultSetJXTable.cellRenderer.format.Date";
	public static final String  DEFAULT_TABLE_CELL_RENDERER_DATE            = "yyyy-MM-dd";

	public static final String  PROPKEY_TABLE_CELL_RENDERER_TIME            = "ResultSetJXTable.cellRenderer.format.Time";
	public static final String  DEFAULT_TABLE_CELL_RENDERER_TIME            = "HH:mm:ss";

//	public static final String  PROPKEY_TABLE_CELL_RENDERER_BIGDECIMAL      = "ResultSetJXTable.cellRenderer.format.BigDecimal";
//	public static final String  DEFAULT_TABLE_CELL_RENDERER_BIGDECIMAL      = "";

	public static final String  PROPKEY_TABLE_CELL_RENDERER_MIN_NUMBER_DECIMALS = "ResultSetJXTable.cellRenderer.format.min.Number.decimals";
	public static final int     DEFAULT_TABLE_CELL_RENDERER_MIN_NUMBER_DECIMALS = 0;
	public static final String  PROPKEY_TABLE_CELL_RENDERER_MAX_NUMBER_DECIMALS = "ResultSetJXTable.cellRenderer.format.max.Number.decimals";
	public static final int     DEFAULT_TABLE_CELL_RENDERER_MAX_NUMBER_DECIMALS = 128;
//	public static final int     DEFAULT_TABLE_CELL_RENDERER_NUMBER_DECIMALS = 9;

	public static final String  PROPKEY_TABLE_CELL_MAX_PREFERRED_WIDTH       = "ResultSetJXTable.cellRenderer.preferred.maxWidth";
	public static final int     DEFAULT_TABLE_CELL_MAX_PREFERRED_WIDTH       = 1000;



	public static final String  PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_NL_REPLACE       = "ResultSetJXTable.generate.rows.to.sql.newline.replace";
	public static final boolean DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_NL_REPLACE       = true;

	public static final String  PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_NL_REPLACE_STR   = "ResultSetJXTable.generate.rows.to.sql.newline.replace.str";
	public static final String  DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_NL_REPLACE_STR   = "\\n";

	public static final String  PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_STMNT_TERM       = "ResultSetJXTable.generate.rows.to.sql.statement.terminator";
	public static final boolean DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_STMNT_TERM       = true;

	public static final String  PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_STMNT_TERM_STR   = "ResultSetJXTable.generate.rows.to.sql.statement.terminator.str";
	public static final String  DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_STMNT_TERM_STR   = ";";

	public static final String  PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_NULL_REPLACE     = "ResultSetJXTable.generate.rows.to.sql.null.replace";
	public static final boolean DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_NULL_REPLACE     = true;

	public static final String  PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_NULL_REPLACE_STR = "ResultSetJXTable.generate.rows.to.sql.null.replace.str";
	public static final String  DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_NULL_REPLACE_STR = "NULL";

	public static final String  PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_INCLUDE_DBNAME   = "ResultSetJXTable.generate.rows.to.sql.include.dbname";
	public static final boolean DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_INCLUDE_DBNAME   = false;

	public static final String  PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_INCLUDE_SCHEMA   = "ResultSetJXTable.generate.rows.to.sql.include.schema";
	public static final boolean DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_INCLUDE_SCHEMA   = true;

	public static final String  PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI           = "ResultSetJXTable.generate.rows.to.sql.add.quotedIdentifier";
	public static final boolean DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI           = true;

	public static final String  PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI_BEGIN_STR = "ResultSetJXTable.generate.rows.to.sql.add.quotedIdentifier.begin.str";
	public static final String  DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI_BEGIN_STR = "[";

	public static final String  PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI_END_STR   = "ResultSetJXTable.generate.rows.to.sql.add.quotedIdentifier.end.str";
	public static final String  DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI_END_STR   = "]";

	private Point _lastMouseClick = null;

	private boolean      _tabHeader_useFocusableTips   = true;
	private boolean      _cellContent_useFocusableTips = true;
	private FocusableTip _focusableTip                 = null;

	private CompletionProviderAbstract _compleationProvider;
	
	public Point getLastMouseClickPoint()
	{
		return _lastMouseClick;
	}

	public ResultSetJXTable(TableModel tm)
	{
		super(tm);
		
		// if it's java9 there seems to be some problems with repainting... (if the table is not added to a ScrollPane, which takes upp the whole scroll)
		// This is better in java 17, but still not 100% good, so lets keep this for a while longer
		if (JavaVersion.isJava9orLater())
		{
//			_logger.info("For Java-9 and above, add a 'repaint' when the mouse moves. THIS SHOULD BE REMOVED WHEN THE BUG IS FIXED IN SOME JAVA RELEASE.");

			// Add a repaint every 50ms (when the mouse stops moving = no more repaint until we start to move it again)
			// with the second parameter to true: it will only do repaint 50ms after you have stopped moving the mouse.
			addMouseMotionListener(new DeferredMouseMotionListener(50, false)
			{
				@Override
				public void deferredMouseMoved(MouseEvent e)
				{
					//System.out.println("ResultSetJXTable.this.repaint(): "+System.currentTimeMillis());
					ResultSetJXTable.this.repaint();
				}
			});
		}

		addMouseListener(new MouseListener()
		{
			@Override public void mouseReleased(MouseEvent e) {}
			@Override public void mousePressed(MouseEvent e) {}
			@Override public void mouseExited(MouseEvent e) {}
			@Override public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseClicked(MouseEvent e)
			{
				_lastMouseClick = e.getPoint();
			}
		});

		// java.sql.Timestamp format
		@SuppressWarnings("serial")
		StringValue svTimestamp = new StringValue() 
		{
//			DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
//			String format = Configuration.getCombinedConfiguration().getProperty("ResultSetJXTable.cellRenderer.format.Timestamp", "yyyy-MM-dd HH:mm:ss.SSS");
			String format = Configuration.getCombinedConfiguration().getProperty(PROPKEY_TABLE_CELL_RENDERER_TIMESTAMP, DEFAULT_TABLE_CELL_RENDERER_TIMESTAMP);
			DateFormat df = new SimpleDateFormat(format);
			@Override
			public String getString(Object value) 
			{
				if (value != null && value instanceof java.sql.Timestamp)
				{
    				try 
    				{ 
    					return df.format(value); 
    				}
    				catch(Throwable ignore) 
    				{
    					_logger.warn("Problems in ResultSetJXTable when rendering type '"+value.getClass().getName()+"', object '"+value+"'. returning a toString instead. Caught: "+ignore);
    					return value==null ? ResultSetTableModel.DEFAULT_NULL_REPLACE : value.toString(); 
    				}
				}
				else
				{
					return value==null ? ResultSetTableModel.DEFAULT_NULL_REPLACE : value.toString();
				}
			}
		};
		setDefaultRenderer(java.sql.Timestamp.class, new DefaultTableRenderer(svTimestamp));

		// java.sql.Date format
		@SuppressWarnings("serial")
		StringValue svDate = new StringValue() 
		{
//			String format = Configuration.getCombinedConfiguration().getProperty("ResultSetJXTable.cellRenderer.format.Date", "yyyy-MM-dd");
			String format = Configuration.getCombinedConfiguration().getProperty(PROPKEY_TABLE_CELL_RENDERER_DATE, DEFAULT_TABLE_CELL_RENDERER_DATE);
			DateFormat df = new SimpleDateFormat(format);
			@Override
			public String getString(Object value) 
			{
				if (value != null && value instanceof java.sql.Date)
				{
    				try 
    				{ 
    					return df.format(value); 
    				}
    				catch(Throwable ignore) 
    				{
    					_logger.warn("Problems in ResultSetJXTable when rendering type '"+value.getClass().getName()+"', object '"+value+"'. returning a toString instead. Caught: "+ignore);
    					return value==null ? ResultSetTableModel.DEFAULT_NULL_REPLACE : value.toString(); 
    				}
				}
				else
				{
					return value==null ? ResultSetTableModel.DEFAULT_NULL_REPLACE : value.toString();
				}
			}
		};
		setDefaultRenderer(java.sql.Date.class, new DefaultTableRenderer(svDate));

		// java.sql.Time format
		@SuppressWarnings("serial")
		StringValue svTime = new StringValue() 
		{
//			String format = Configuration.getCombinedConfiguration().getProperty("ResultSetJXTable.cellRenderer.format.Time", "HH:mm:ss");
			String format = Configuration.getCombinedConfiguration().getProperty(PROPKEY_TABLE_CELL_RENDERER_TIME, DEFAULT_TABLE_CELL_RENDERER_TIME);
			DateFormat df = new SimpleDateFormat(format);
			@Override
			public String getString(Object value) 
			{
				if (value != null && value instanceof java.sql.Time)
				{
    				try 
    				{ 
    					return df.format(value); 
    				}
    				catch(Throwable ignore) 
    				{
    					_logger.warn("Problems in ResultSetJXTable when rendering type '"+value.getClass().getName()+"', object '"+value+"'. returning a toString instead. Caught: "+ignore);
    					return value==null ? ResultSetTableModel.DEFAULT_NULL_REPLACE : value.toString(); 
    				}
				}
				else
				{
					return value==null ? ResultSetTableModel.DEFAULT_NULL_REPLACE : value.toString();
				}
			}
		};
		setDefaultRenderer(java.sql.Time.class, new DefaultTableRenderer(svTime));

		// BigDecimal, Double, Float format
		@SuppressWarnings("serial")
		StringValue svInExactNumber = new StringValue() 
		{
//			int decimals = Configuration.getCombinedConfiguration().getIntProperty("ResultSetJXTable.cellRenderer.format.BigDecimal.decimals", 3);
			int minDecimals = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_TABLE_CELL_RENDERER_MIN_NUMBER_DECIMALS, DEFAULT_TABLE_CELL_RENDERER_MIN_NUMBER_DECIMALS);
			int maxDecimals = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_TABLE_CELL_RENDERER_MAX_NUMBER_DECIMALS, DEFAULT_TABLE_CELL_RENDERER_MAX_NUMBER_DECIMALS);

			NumberFormat nf = null;
			{ // init/constructor section
				try
				{
					nf = new DecimalFormat();
					nf.setMinimumFractionDigits(minDecimals);
					nf.setMaximumFractionDigits(maxDecimals);
				}
				catch (Throwable t)
				{
					nf = NumberFormat.getInstance();
				}
			}
			@Override
			public String getString(Object value) 
			{
				try
				{
					if ( ! (value instanceof Number) ) 
						return StringValues.TO_STRING.getString(value);
					
					return nf.format(value);
				}
				catch (RuntimeException rte)
				{
					_logger.warn("Problems to render... the value |" + value + "| class=" + (value == null ? null : value.getClass().getName()) + ". returning 'toString instead'. Caught: " + rte, rte);
					
					return StringValues.TO_STRING.getString(value);
				}
			}
		};
//		DefaultTableRenderer InExactNumberRenderer = new DefaultTableRenderer(svInExactNumber);
		DefaultTableRenderer InExactNumberRenderer = new DefaultTableRenderer( new LabelProvider(svInExactNumber, JLabel.TRAILING) );
		
		setDefaultRenderer(BigDecimal.class, InExactNumberRenderer);
		setDefaultRenderer(Double    .class, InExactNumberRenderer);
		setDefaultRenderer(Float     .class, InExactNumberRenderer);

		// NULL Values: SET BACKGROUND COLOR
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				// Check NULL value
				Object cellObj = adapter.getValue();
				if (cellObj == null || ResultSetTableModel.DEFAULT_NULL_REPLACE.equals(cellObj))
					return true;
//				String cellValue = adapter.getString();
//				if (cellValue == null || ResultSetTableModel.DEFAULT_NULL_REPLACE.equals(cellValue))
//					return true;
				
				// Check ROWID Column
				int mcol = adapter.convertColumnIndexToModel(adapter.column);
				String colName = adapter.getColumnName(mcol);
				if (mcol == 0 && ResultSetTableModel.ROW_NUMBER_COLNAME.equals(colName))
					return true;

				return false;
			}
		}, NULL_VALUE_COLOR, null));

		// Set columnHeader popup menu
		getTableHeader().setComponentPopupMenu(createTableHeaderPopupMenu());
	}


	/**
	 * workaround to handle "(NULL)" values on Timestamps and other issues, fallback is to compare them as strings in case of **Failures**
	 */
	@Override
	protected RowSorter<? extends TableModel> createDefaultRowSorter()
	{
//		return super.createDefaultRowSorter();
		return new GTableSortController<TableModel>(getModel());
	}


	/**
	 * Creates the JMenu on the Component, this can be overrided by a subclass.
	 */
	public JPopupMenu createTableHeaderPopupMenu()
	{
		JPopupMenu popup = new JPopupMenu();
		JMenuItem mi;

		// SHOW RESULTSET METADATA INFORMATION...
		mi = new JMenuItem("Show ResultSet MetaData Information...");
		popup.add(mi);
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				TableModel tm = getModel();
				if (tm instanceof ResultSetTableModel)
				{
					ResultSetMetaDataViewDialog dialog = new ResultSetMetaDataViewDialog( ResultSetJXTable.this, (ResultSetTableModel) getModel() );
					dialog.setVisible(true);
				}
				else
				{
					SwingUtils.showErrorMessage(ResultSetJXTable.this, "Show ResultSet MetaData - NOT POSSIBLE", "Table Model must be 'ResultSetTableModel', this model is '" + tm.getClass().getName() + "'.", null);
				}
			}
		});
		
//		popup.addPopupMenuListener(new PopupMenuListener()
//		{
//			@Override
//			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
//			{
//			}
//			
//			@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
//			@Override public void popupMenuCanceled(PopupMenuEvent e) {}
//		});

		// Separator
		popup.add(new JSeparator());

		// ADJUST COLUMN WIDTH
		mi = new JMenuItem("Adjust Column Width, both shrink and grow (to 100% width).");
		popup.add(mi);
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				ResultSetJXTable.super.packAll();
//				ResultSetJXTable.this.packAll();
			}
		});

		// ADJUST COLUMN WIDTH
		mi = new JMenuItem("Adjust Column Width, both shrink and grow (to visible max preferred width)");
		popup.add(mi);
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				ResultSetJXTable.this.packAllGrowAndShrink();
			}
		});

		// ADJUST COLUMN WIDTH
		mi = new JMenuItem("Adjust Column Width, grow only");
		popup.add(mi);
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				ResultSetJXTable.this.packAllGrowOnly();
			}
		});

		// ResultSet Table Properties
		mi = new JMenuItem("ResultSet Table Properties...");
		popup.add(mi);
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Window win =SwingUtils.getParentWindow(ResultSetJXTable.this);
				Frame owner = null;
				if (win instanceof Frame)
					owner = (Frame) win;

				// get the file, if not found... give it a default...
				int ret = ResultSetJXTable.showSettingsDialog(owner);
				if (ret == JOptionPane.OK_OPTION)
				{
				}
			}
		});

		return popup;
	}

	@Override
	public void packAll()
	{
//		super.packAll();
		packAllGrowOnly();
	}
	
//	private int _packMaxColWidth = SwingUtils.hiDpiScale(1000);
	private int _packMaxColWidth = -1;
	public void setPackMaxColWidth(int maxWidth) { _packMaxColWidth = maxWidth; }
	public int getPackMaxColWidth() 
	{ 
		if (_packMaxColWidth != -1 )
			return _packMaxColWidth;
		
		return SwingUtils.hiDpiScale(Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_TABLE_CELL_MAX_PREFERRED_WIDTH, DEFAULT_TABLE_CELL_MAX_PREFERRED_WIDTH)); 
	}

	public void packAllGrowOnly()
	{
		int margin = -1;
		boolean onlyGrowWidth = true;

		for (int c = 0; c < getColumnCount(); c++)
		{
			TableColumnExt ce = getColumnExt(c);

//			int maxWidth = -1;
			int maxWidth = getPackMaxColWidth();
			int beforePackWidth = ce.getPreferredWidth();
			
			packColumn(c, margin, maxWidth);

			if (onlyGrowWidth)
			{
				int afterPackWidth = ce.getPreferredWidth();
				if (afterPackWidth < beforePackWidth)
					ce.setPreferredWidth(beforePackWidth);

				/* Check if the width exceeds the max */
				if (maxWidth != -1 && afterPackWidth > maxWidth)
					ce.setPreferredWidth(maxWidth);
			}
		}
	}

	public void packAllGrowAndShrink()
	{
		int margin = -1;

		for (int c = 0; c < getColumnCount(); c++)
		{
			int maxWidth = getPackMaxColWidth();
			
			packColumn(c, margin, maxWidth);
		}
	}

	// 
	// TOOL TIP for: TABLE HEADERS
	//
	@Override
	protected JTableHeader createDefaultTableHeader()
	{
		return new JXTableHeader(getColumnModel())
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getToolTipText(MouseEvent e)
			{
				// Now get the column name, which we point at
				Point p = e.getPoint();
				int index = getColumnModel().getColumnIndexAtX(p.x);
				if ( index < 0 )
					return null;
				
				TableModel tm = getModel();
				if (tm instanceof ResultSetTableModel)
				{
					ResultSetTableModel rstm = (ResultSetTableModel) tm;
					String tooltip = rstm.getToolTipTextForTableHeader(index);

					if (_tabHeader_useFocusableTips)
					{
						if (tooltip != null) 
						{
							if (_focusableTip == null) 
								_focusableTip = new FocusableTip(this);

							_focusableTip.toolTipRequested(e, tooltip);
						}
						// No tool tip text at new location - hide tip window if one is
						// currently visible
						else if (_focusableTip != null) 
						{
							_focusableTip.possiblyDisposeOfTipWindow();
						}
						return null;
					}
					else
						return tooltip;
				}
				return null;
			}
		};
	}

	// 
	// TOOL TIP for: CELL DATA
	//
	@Override
	public String getToolTipText(MouseEvent e)
	{
		String tooltip = null;
		Point p = e.getPoint();
		int vrow = rowAtPoint(p);    // View Row
		int vcol = columnAtPoint(p); // View Column
		if ( vrow >= 0 && vcol >= 0 )
		{
			int mcol = super.convertColumnIndexToModel(vcol); // Model Column
			int mrow = super.convertRowIndexToModel(vrow);    // Model Row

			TableModel tm = getModel();
			if (tm instanceof ResultSetTableModel)
			{
				ResultSetTableModel rstm = (ResultSetTableModel) tm;
				int sqlType = rstm.getSqlType(mcol);

				// type
				// 0 == Other
				// 1 == BINARY
				// 2 == TEXT(long)
				// 3 == TEXT(short)
				int type = 0;  
				if (sqlType == Types.LONGVARBINARY || sqlType == Types.VARBINARY || sqlType == Types.BLOB)
					type = 1;
				else if (sqlType == Types.LONGVARCHAR || sqlType == Types.LONGNVARCHAR || sqlType == Types.CLOB)
					type = 2;
				else if (sqlType == Types.CHAR || sqlType == Types.VARCHAR || sqlType == Types.NCHAR || sqlType == Types.NVARCHAR)
					type = 3;

				// for "any of the above"...
				// Show special tool tip (but only if its "long enough", 100 chars)
				if (type != 0)
				{
					Object cellValue = tm.getValueAt(mrow, mcol);
					if (cellValue == null)
						return null;
					String cellStr = cellValue.toString();

					// Show special tooltip for special column names
					String colName = rstm.getColumnName(mcol);
					if (colName != null && StringUtil.equalsAnyIgnoreCase(colName, "tablename", "table_name", "table name", "objectname", "object_name", "object name"))
					{
						// Reuse the tooltip in the SQL editor (CompletionProvider)
						if (_logger.isDebugEnabled())
							_logger.debug("SHOW TABLE TOOLTIP FOR: cellStr=|" + cellStr + "|");
						
						if (_compleationProvider != null && _compleationProvider instanceof CompletionProviderAbstractSql)
						{
							CompletionProviderAbstractSql compleationProviderSql = (CompletionProviderAbstractSql) _compleationProvider;

							DbxConnection conn = compleationProviderSql.getConnectionProvider().getConnection();
							SqlObjectName sqlObjName = new SqlObjectName(conn, cellStr);
							
							String dbname       = sqlObjName.getCatalogNameNull();
							String tabOwnerName = sqlObjName.getSchemaNameNull();
							String objectName   = sqlObjName.getObjectName();

							if ("dbo".equals(tabOwnerName))
								tabOwnerName = null;

							// Override dbname: if we can find: 'dbname', 'databasename', 'database_name' or 'database name'
							if (rstm.hasColumnNoCase("dbname") || rstm.hasColumnNoCase("databasename") || rstm.hasColumnNoCase("database_name") || rstm.hasColumnNoCase("database name"))
							{
								int colPos = -1;
								if (colPos == -1) colPos = rstm.findColumnNoCase("dbname");
								if (colPos == -1) colPos = rstm.findColumnNoCase("databasename");
								if (colPos == -1) colPos = rstm.findColumnNoCase("database_name");
								if (colPos == -1) colPos = rstm.findColumnNoCase("database name");

								if (colPos != -1)
									dbname = rstm.getValueAsString(mrow, colPos);
							}
							
							// Override tabOwnerName: if we can find: 'schemaname', 'schema_name' or 'schema name'
							if (rstm.hasColumnNoCase("schemaname") || rstm.hasColumnNoCase("schema_name") || rstm.hasColumnNoCase("schema name"))
							{
								int colPos = -1;
								if (colPos == -1) colPos = rstm.findColumnNoCase("schemaname");
								if (colPos == -1) colPos = rstm.findColumnNoCase("schema_name");
								if (colPos == -1) colPos = rstm.findColumnNoCase("schema name");

								if (colPos != -1)
									tabOwnerName = rstm.getValueAsString(mrow, colPos);
							}
								
							
							if (_logger.isDebugEnabled())
								_logger.debug("SHOW TABLE TOOLTIP FOR: cellStr=|" + cellStr + "|, dbname=|" + dbname + "|, tabOwnerName=|" + tabOwnerName + "|, objectName=|" + objectName + "|.");

							List<Completion> list = compleationProviderSql.getTableListWithGuiProgress(conn, dbname, tabOwnerName, objectName, false);

							if (_logger.isDebugEnabled())
								_logger.debug("compleationProviderSql.getTableListWithGuiProgress(ObjectName): dbname='" + dbname + "', ownerName='" + tabOwnerName + "', objectName='" + objectName + "'. list.size()=" + (list == null ? "-null-" : list.size()) + ".");

							if ( list != null )
							{
								if      (list.size() == 0) 
								{
									tooltip = "No table information found for table '" + tabOwnerName + "." + objectName + "' in database '" + dbname + "'.";
								}
								else if (list.size() == 1) 
								{
									tooltip = list.get(0).getSummary();
								}
								else
								{
									String names = "";
									String firstEntry = list.get(0).getSummary();
									for (Completion completion : list)
									{
										names += "<li><code>" + completion.getReplacementText() + "</code><br></li>";
									}
									tooltip = "<FONT COLOR='red'>"
											+ "Found table information, but I found MORE than 1 table, count=" + list.size() + ". <br>"
											+ "I can only show info for 1 table. (database='" + dbname + "', table='" + tabOwnerName + "." + objectName + "'): <br>"
											+ "<ul>" 
											+ names
											+ "</ul>"
											+ "<br>"
											+ "<b>Here is the FIRST Entry</b><br>"
											+ "<hr><br>"
											+ "</FONT>"
											+ firstEntry;
								}
							}
						}
					}
					
					// Special logic if it's a SQL-Server "showplan" XML
					if (cellStr.startsWith("<ShowPlanXML xmlns="))
					{
						tooltip = ToolTipSupplierSqlServer.createXmlPlanTooltip(cellStr);
					}

					// only do if: MEDIUM Size
					//  - to small is just irritating
					//  - to big will "freeze" the GUI
					if (tooltip == null && cellStr.length() >= 100)
					{
						int maxCellStrLen = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_TOOLTIP_CELL_DISPLAY_MAX_SIZE_KB, DEFAULT_TOOLTIP_CELL_DISPLAY_MAX_SIZE_KB); // default 10MB
						if (cellStr.length() > maxCellStrLen*1024) // 10 MB
						{
							tooltip = "<html><b>WARNING</b>: Tooltip content for this cell is to big, it will not be displayed. (size is " + (cellStr.length()/1024) + " KB)</html>";
						}
						else
						{
							// Convert it to a BYTE ARRAY
							byte[] bytes;
							if (type == 1)
								bytes = StringUtil.hexToBytes(cellStr);
							else 
								bytes = cellStr.getBytes();

							// Get the tooltip
							tooltip = getContentSpecificToolTipText(cellStr, bytes, type);
						}
					}
				}
				
				// If not yet any tooltip, generate a "table" with all columns
				if (tooltip == null)
				{
					boolean useAllColumnsTableTooltip = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_TABLE_TOOLTIP_SHOW_ALL_COLUMNS, DEFAULT_TABLE_TOOLTIP_SHOW_ALL_COLUMNS);

					String msgPrefix = "<a href='" + CmToolTipSupplierDefault.SET_PROPERTY_TEMP + PROPKEY_TABLE_TOOLTIP_SHOW_ALL_COLUMNS + "=" + (!useAllColumnsTableTooltip) + "'>" + (useAllColumnsTableTooltip ? "Disable" : "Enable") + "</a> - Show row as tooltip table.<br>";

					if (useAllColumnsTableTooltip)
					{
						String  startMsg         = msgPrefix + "<hr>";
						String  endMsg           = "";
						boolean borders          = false;
						boolean stripedRows      = true;
						boolean addOuterHtmlTags = true;

						tooltip = rstm.toHtmlTableString(mrow, startMsg, endMsg, borders, stripedRows, addOuterHtmlTags);
					}
					else
					{
						tooltip = "<html>" + msgPrefix + "</html>";
					}
				}

				// Should we use FOCUSABLE tooltip
				if (_cellContent_useFocusableTips)
				{
					if (tooltip != null) 
					{
						if (_focusableTip == null) 
							_focusableTip = new FocusableTip(this, null, this);

						_focusableTip.toolTipRequested(e, tooltip);
					}
					// No tool tip text at new location - hide tip window if one is currently visible
					else if (_focusableTip != null) 
					{
						_focusableTip.possiblyDisposeOfTipWindow();
					}
					return null;
				}
				else
					return tooltip;

			} // end: ResultSetTableModel
		}

		return tooltip;
	}


//	/** internally used to specify that a HTML LINK should be opened in EXTERNAL Browser */
//	private static final String OPEN_IN_EXTERNAL_BROWSER = "OPEN-IN-EXTERNAL-BROWSER:";
	
	@Override
	public ResolverReturn hyperlinkResolv(HyperlinkEvent event)
	{
		String desc = event.getDescription();
		if (_logger.isDebugEnabled())
		{
			_logger.debug("");
			_logger.debug("##################################################################################");
			_logger.debug("hyperlinkResolv(): event.getDescription()  ="+event.getDescription());
			_logger.debug("hyperlinkResolv(): event.getURL()          ="+event.getURL());
			_logger.debug("hyperlinkResolv(): event.getEventType()    ="+event.getEventType());
			_logger.debug("hyperlinkResolv(): event.getSourceElement()="+event.getSourceElement());
			_logger.debug("hyperlinkResolv(): event.getSource()       ="+event.getSource());
			_logger.debug("hyperlinkResolv(): event.toString()        ="+event.toString());
		}

		if (desc.startsWith(CmToolTipSupplierDefault.OPEN_IN_EXTERNAL_BROWSER))
		{
			String urlStr = desc.substring(CmToolTipSupplierDefault.OPEN_IN_EXTERNAL_BROWSER.length());
			try
			{
				return ResolverReturn.createOpenInExternalBrowser(event, urlStr);
			}
			catch (MalformedURLException e)
			{
				_logger.warn("Problems open URL='"+urlStr+"', in external Browser.", e);
			}
		}

		if (desc.startsWith(CmToolTipSupplierDefault.OPEN_IN_SENTRY_ONE_PLAN_EXPLORER))
		{
			String urlStr = desc.substring(CmToolTipSupplierDefault.OPEN_IN_SENTRY_ONE_PLAN_EXPLORER.length());
			if (urlStr.startsWith("file:///"))
				urlStr = urlStr.substring("file:///".length());
			
			File tempFile = new File(urlStr);
			SqlSentryPlanExplorer.openSqlPlanExplorer(tempFile);
			
			return ResolverReturn.createDoNothing(event);
			//return null;
		}

		if (desc.startsWith(CmToolTipSupplierDefault.SET_PROPERTY_TEMP))
		{
			String str = desc.substring(CmToolTipSupplierDefault.SET_PROPERTY_TEMP.length());
			return ResolverReturn.createSetProperyTemp(event, str);
		}

		return ResolverReturn.createOpenInCurrentTooltipWindow(event);
	}

	/**
	 * 
	 * @param compleationProviderAbstract
	 */
	public void setCompleationProvider(CompletionProviderAbstract compleationProvider)
	{
		_compleationProvider = compleationProvider;
	}

	/**
	 * Generate a special tooltip for "longer" cell content<br>
	 * for CLOB/BLOB we try to identify the "content" and possibly display the content in any registered application (by file extention or MIME type)
	 * 
	 * @param cellStr
	 * @param bytes
	 * @param type
	 * @return
	 */
	private String getContentSpecificToolTipText(String cellStr, byte[] bytes, int type)
	{
		if (bytes == null)
			bytes = cellStr.getBytes();

		// Get a MIME type
		ContentInfoUtil util = new ContentInfoUtil();
		ContentInfo info = util.findMatch( bytes );

		// Get the bytearray charset...
		UniversalDetector detector1 = new UniversalDetector(null);
		detector1.handleData(bytes, 0, bytes.length);
		detector1.dataEnd();
		String detectedCharsetAtStart = detector1.getDetectedCharset();
		detector1.reset();

		String guessedCharset = detectedCharsetAtStart;
		
//System.out.println("getContentSpecificToolTipText(cellStr, bytes): info(mime-type)="+info+", detectedCharsetAtStart="+detectedCharsetAtStart);

		// If it's a BINARY HTML String was passed...
		// Type == 1: is a BINARY, lets convert the passed "bytes" to a String
		if (type == 1 && info != null)
		{
			boolean isMimeString = false;
			if ( "html".equals(info.getName()) ) isMimeString = true;
			if ( "xml" .equals(info.getName()) ) isMimeString = true;

			// If binary looks like "text" format... transform the binary to STR
			if (isMimeString)
			{
				try
				{
					String tmpCharset = detectedCharsetAtStart;
					if (tmpCharset == null)
						tmpCharset = Charset.defaultCharset().name();
					
					guessedCharset = tmpCharset;
					
					String tmpStr = new String(bytes, tmpCharset );
//System.out.println("Passed cellStr=|"+cellStr+"|.");
//System.out.println("tmpStr=|"+tmpStr+"|.");
					cellStr = tmpStr;
				}
				catch(UnsupportedEncodingException ex)
				{
					_logger.warn("getContentSpecificToolTipText(cellStr, bytes): POSSIBLY_BINARY_HTML, AFTER DECODE: info(mime-type)="+info+", Problems creating a string with detectedCharsetAtStart '"+detectedCharsetAtStart+"'. Caught: "+ex);
				}
			}
		}

		// Unrecognized byte stream...
		// Try to check if it's possibly a BASE64 encoded value...
		boolean wasBase64_decoded = false;
		if (info == null)
		{
			if (Base64.isBase64(cellStr))
			{
//System.out.println("getContentSpecificToolTipText(cellStr, bytes): isBase64=true");
				byte[] base64Decoded_bytes = Base64.decodeBase64(cellStr);

				// Once again... get MIME type for the DECODED value
				util = new ContentInfoUtil();
				info = util.findMatch( base64Decoded_bytes );

//System.out.println("getContentSpecificToolTipText(cellStr, bytes): isBase64=true, AFTER DECODE: info(mime-type)="+info);
				// IF we got a valid MIME type... Then try to convert the value into a new String with the PROPER CHARTSET ENCODING
				if (info != null)
				{
					UniversalDetector detector2 = new UniversalDetector(null);
					detector2.handleData(base64Decoded_bytes, 0, base64Decoded_bytes.length);
					detector2.dataEnd();
					String detectedCharsetBase64Decoded = detector2.getDetectedCharset();
					detector2.reset();

//System.out.println("getContentSpecificToolTipText(cellStr, bytes): isBase64=true, AFTER DECODE: info(mime-type)="+info+", assigning 'bytes'. and 'cellStr' using detectedCharsetBase64Decoded="+detectedCharsetBase64Decoded+", if null use: "+Charset.defaultCharset().name());

					if (detectedCharsetBase64Decoded == null)
						detectedCharsetBase64Decoded = Charset.defaultCharset().name();

					guessedCharset = detectedCharsetBase64Decoded;
					
					try
					{
						String base64Decoded_cellStr = new String(base64Decoded_bytes, detectedCharsetBase64Decoded);

						wasBase64_decoded = true;
						bytes             = base64Decoded_bytes;
						cellStr           = base64Decoded_cellStr;
						
						//System.out.println("CONVERTED: cellStr: "+cellStr);
					}
					catch(UnsupportedEncodingException ex)
					{
						_logger.warn("getContentSpecificToolTipText(cellStr, bytes): isBase64=true, AFTER DECODE: info(mime-type)="+info+", Problems creating a string with detectedCharsetBase64Decoded '"+detectedCharsetBase64Decoded+"'. Caught: "+ex);
					}
				}
			}
		}

		//------------------------------------------------------------------------
		// "simple magic" do NOT seem to recognize JSON
		// So lets do an extra check here
		//------------------------------------------------------------------------
		if (JsonUtils.isPossibleJson(cellStr))
		{
			if (JsonUtils.isJsonValid(cellStr))
			{
				_logger.info("Tooltip: Possible a JSON cell content. JsonUtils.isJsonValid(cellStr)==true. info=|" + info + "|. ACTION RESETTING: info=null");
				info = null;
			}
		}

		//------------------------------------------------------------------------
		// Unrecognized MIME Type
		//------------------------------------------------------------------------
		if (info == null)
		{

			//------------------------------------------------------------------------
			// JSON isn't picked up by the ContentInfoUtil
			//------------------------------------------------------------------------
			if (JsonUtils.isPossibleJson(cellStr))
			{
				if (JsonUtils.isJsonValid(cellStr))
				{
//System.out.println("getContentSpecificToolTipText(cellStr, bytes): isJsonValid=true");
//					StringBuilder sb = new StringBuilder();
//					sb.append("<html>");
//					sb.append("Cell content looks like <i>JSON</i>, so displaying it as formated JSON. Origin length="+cellStr.length()+"<br>");
//					sb.append("<hr>");
//					sb.append("<pre><code>");
//					sb.append(JsonUtils.format(cellStr).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
//					sb.append("</code></pre>");
//					sb.append("</html>");
//
//					return sb.toString();
					
					File tmpFile = null;
					try
					{
						// put content in a TEMP file 
						tmpFile = createTempFile("sqlw_JSON_tooltip_", ".html", bytes); // NOTE: A Browser is possibly better at reading the JSON than any registered app???

						// Compose ToolTip HTML (with content, & a LINK to be opened in "browser")
						String urlStr = ("file:///"+tmpFile);
						try	
						{
							URL url = new URL(urlStr);
							
							StringBuilder sb = new StringBuilder();
							sb.append("<html>");
							sb.append("<h2>Tooltip for 'JSON'</h2>");
							if (wasBase64_decoded)
								sb.append("<i>Cell Content was decoded using 'decodeBase64', before actual content could be determined.</i><br>");
							sb.append("<br>");
							sb.append("Using temp file: <code>").append(tmpFile).append("</code><br>");
							sb.append("File Size: <code>").append(StringUtil.bytesToHuman(tmpFile.length(), "#.#")).append("</code><br>");
							sb.append("Guessed Charset: <code>").append(guessedCharset).append("</code><br>");
							sb.append("<a href='").append(CmToolTipSupplierDefault.OPEN_IN_EXTERNAL_BROWSER + url).append("'>Open in External Browser</a> (registered application for file extention <b>'.html'</b> will be used)<br>");
							sb.append("<hr>");
							
							sb.append("<pre><code>");
							sb.append(JsonUtils.format(cellStr).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
							sb.append("</code></pre>");

							sb.append("</html>");

							return sb.toString();
						}
						catch (Exception ex) 
						{
							_logger.warn("Problems when open the URL '"+urlStr+"'. Caught: "+ex, ex); 
							return 
								"<html>Problems when open the URL '<code>"+urlStr+"</code>'.<br>"
								+ "Caught: <b>" + ex + "</b><br>"
								+ "<hr>"
								+ "<a href='" + CmToolTipSupplierDefault.OPEN_IN_EXTERNAL_BROWSER + urlStr + "'>Open tempfile in External Browser</a> (registered application for file extention <b>'.html'</b> will be used)<br>"
								+ "Or copy the above filename, and open it in any application or text editor<br>"
								+ "<html/>";
						}
					}
					catch (Exception ex)
					{
						return "<html>Sorry problems when creating temporary file '"+tmpFile+"'<br>Caught: "+ex+"</html>";
					}
				}
				else
				{
					_logger.info("Tooltip: Possible JSON 'JsonUtils.isPossibleJson(cellStr)==true', but 'JsonUtils.isJsonValid(cellStr)==false'. JsonUtils.isJsonValid_returnException(cellStr): " + JsonUtils.isJsonValid_returnException(cellStr));
				}
			}

			//------------------------------------------------------------------------
			// HTML XML that do NOT start with '<html ' isn't picked up by the ContentInfoUtil, so lets dig into the String and check if it *might* be a HTML content...
			//------------------------------------------------------------------------
			if (StringUtil.isPossibleHtml(cellStr))
			{
//				String first250Char = cellStr.substring(0, Math.min(255, cellStr.length()-1));
//				boolean hasStartHtmlTag = StringUtils.containsIgnoreCase(first250Char, "<html");
				
				File tmpFile = null;
				try
				{
					String fileExtention = ".html";
					
					// put content in a TEMP file 
					tmpFile = createTempFile("sqlw_HTML_tooltip_", fileExtention, bytes);

					// Compose ToolTip HTML (with content, & a LINK to be opened in "browser")
					String urlStr = ("file:///"+tmpFile);
					try	
					{
						URL url = new URL(urlStr);
						
						StringBuilder sb = new StringBuilder();
						sb.append("<html>");
						sb.append("<h2>Tooltip for 'HTML' (without html start tag)</h2>");
						if (wasBase64_decoded)
							sb.append("<i>Cell Content was decoded using 'decodeBase64', before actual content could be determined.</i><br>");
						sb.append("<br>");
						sb.append("Using temp file: <code>").append(tmpFile).append("</code><br>");
						sb.append("File Size: <code>").append(StringUtil.bytesToHuman(tmpFile.length(), "#.#")).append("</code><br>");
						sb.append("Guessed Charset: <code>").append(guessedCharset).append("</code><br>");
						sb.append("<a href='").append(CmToolTipSupplierDefault.OPEN_IN_EXTERNAL_BROWSER + url).append("'>Open in External Browser</a> (registered application for file extention <b>'" + fileExtention + "'</b> will be used)<br>");
						sb.append("<hr>");

						sb.append(cellStr);

						sb.append("</html>");

						return sb.toString();
					}
					catch (Exception ex) 
					{
						_logger.warn("Problems when open the URL '"+urlStr+"'. Caught: "+ex, ex); 
						return 
							"<html>Problems when open the URL '<code>"+urlStr+"</code>'.<br>"
							+ "Caught: <b>" + ex + "</b><br>"
							+ "<hr>"
							+ "<a href='" + CmToolTipSupplierDefault.OPEN_IN_EXTERNAL_BROWSER + urlStr + "'>Open tempfile in External Browser</a> (registered application for file extention <b>'" + fileExtention + "'</b> will be used)<br>"
							+ "Or copy the above filename, and open it in any application or text editor<br>"
							+ "<html/>";
					}
				}
				catch (Exception ex)
				{
					return "<html>Sorry problems when creating temporary file '"+tmpFile+"'<br>Caught: "+ex+"</html>";
				}
			}

			//------------------------------------------------------------------------
			// Special... for Brent Ozar, sp_blitz *** procedures
			//------------------------------------------------------------------------
			if (cellStr.startsWith("<?ClickToSee"))
			{
				return    "<html>"
						+ "<pre><code>"
						+ cellStr.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
						+ "</code></pre>"
						+ "</html>"
						;
			}
			
			//------------------------------------------------------------------------
			// XML that do NOT start with '<?xml ' isn't picked up by the ContentInfoUtil, so lets dig into the String and check if it *might* be a XML content...
			//------------------------------------------------------------------------
			if (StringUtil.isPossibleXml(cellStr))
			{
				String first250Char = cellStr.substring(0, Math.min(255, cellStr.length()-1));
				String xmlExtention = ".xml"; 
				
				// Check for XML subcategory
				
				// -- SQL Server -- Deadlock report -- to filename: .xdl
				if (first250Char.contains("<deadlock>"))
				{
					xmlExtention = ".xdl";
				}

				// -- SQL Server -- Showplan -- to filename: .sqlplan
				if (first250Char.contains("<ShowPlanXML "))
				{
					xmlExtention = ".sqlplan";
				}

				File tmpFile = null;
				try
				{
					// put content in a TEMP file 
//					tmpFile = createTempFile("sqlw_XML_tooltip_", ".html", bytes);
					tmpFile = createTempFile("sqlw_XML_tooltip_", xmlExtention, bytes);

					// Compose ToolTip HTML (with content, & a LINK to be opened in "browser")
					String urlStr = ("file:///"+tmpFile);
					try	
					{
						URL url = new URL(urlStr);
						
						StringBuilder sb = new StringBuilder();
						sb.append("<html>");
						sb.append("<h2>Tooltip for 'XML'</h2>");
						if (wasBase64_decoded)
							sb.append("<i>Cell Content was decoded using 'decodeBase64', before actual content could be determined.</i><br>");
						sb.append("<br>");
						sb.append("Using temp file: <code>").append(tmpFile).append("</code><br>");
						sb.append("File Size: <code>").append(StringUtil.bytesToHuman(tmpFile.length(), "#.#")).append("</code><br>");
						sb.append("Guessed Charset: <code>").append(guessedCharset).append("</code><br>");
						sb.append("<a href='").append(CmToolTipSupplierDefault.OPEN_IN_EXTERNAL_BROWSER + url).append("'>Open in External Browser</a> (registered application for file extention <b>'" + xmlExtention + "'</b> will be used)<br>");
						sb.append("<hr>");

						int maxDisplayLenKb = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_TOOLTIP_XML_INLINE_MAX_SIZE_KB, DEFAULT_TOOLTIP_XML_INLINE_MAX_SIZE_KB);
						if (cellStr.length() < maxDisplayLenKb * 1024)
						{
							sb.append("<pre><code>");
							sb.append(StringUtil.xmlFormat(cellStr).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
							sb.append("</code></pre>");
						}
						else
						{
    						sb.append("<font color='red'>Sorry the content is to big to display <b>here</b>, please open the file in above link!</font><br>");
    						sb.append("Current max size is: "+maxDisplayLenKb+" KB<br>");
    						sb.append("Note: This can be changed with property '"+PROPKEY_TOOLTIP_XML_INLINE_MAX_SIZE_KB+"'<br>");
						}

						sb.append("</html>");

						return sb.toString();
					}
					catch (Exception ex) 
					{
						_logger.warn("Problems when open the URL '"+urlStr+"'. Caught: "+ex, ex); 
						return 
							"<html>Problems when open the URL '<code>"+urlStr+"</code>'.<br>"
							+ "Caught: <b>" + ex + "</b><br>"
							+ "<hr>"
							+ "<a href='" + CmToolTipSupplierDefault.OPEN_IN_EXTERNAL_BROWSER + urlStr + "'>Open tempfile in External Browser</a> (registered application for file extention <b>'.html'</b> will be used)<br>"
							+ "Or copy the above filename, and open it in any application or text editor<br>"
							+ "<html/>";
					}
				}
				catch (Exception ex)
				{
					return "<html>Sorry problems when creating temporary file '"+tmpFile+"'<br>Caught: "+ex+"</html>";
				}
			}
			
//			StringBuilder sb = new StringBuilder();
//			sb.append("<html>");
//			sb.append("Cell content is <i>unknown</i>, so displaying it as raw text. Length="+cellStr.length()+"<br>");
//			sb.append("<hr>");
//			sb.append("<pre><code>");
//			sb.append(cellStr.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
//			sb.append("</code></pre>");
//			sb.append("</html>");
//
//			return sb.toString();
			
			//------------------------------------------------------------------------
			// UNKNWON
			//------------------------------------------------------------------------
			File tmpFile = null;
			try
			{
				// put content in a TEMP file 
				tmpFile = createTempFile("sqlw_UNKNOWN_tooltip_", ".txt", bytes);

				// Compose ToolTip HTML (with content, & a LINK to be opened in "browser")
				String urlStr = ("file:///"+tmpFile);
				try	
				{
					URL url = new URL(urlStr);
					
					StringBuilder sb = new StringBuilder();
					sb.append("<html>");
					sb.append("<h2>Tooltip for 'UNKNOWN' Cell Content</h2>");
					if (wasBase64_decoded)
						sb.append("<i>Cell Content was decoded using 'decodeBase64', before actual content could be determined.</i><br>");
					sb.append("<br>");
					sb.append("Using temp file: <code>").append(tmpFile).append("</code><br>");
					sb.append("File Size: <code>").append(StringUtil.bytesToHuman(tmpFile.length(), "#.#")).append("</code><br>");
					sb.append("Guessed Charset: <code>").append(guessedCharset).append("</code><br>");
					sb.append("<a href='").append(CmToolTipSupplierDefault.OPEN_IN_EXTERNAL_BROWSER + url).append("'>Open in External Browser</a> (registered application for file extention <b>'.txt'</b> will be used)<br>");
					sb.append("<hr>");
					
					String truncatedMsg = "";
					int    maxStrLen    = Configuration.getCombinedConfiguration().getIntProperty(ResultSetTableModel.PROPKEY_HtmlToolTip_maxCellLength, ResultSetTableModel.DEFAULT_HtmlToolTip_maxCellLength);
					int    strValLen    = cellStr.length(); 
					if (strValLen > maxStrLen)
					{
						cellStr =  cellStr.substring(0, maxStrLen) + " ... ";
						truncatedMsg = "<font color='orange'><i><b>NOTE:</b> content is truncated after " + maxStrLen + " chars (actual length is "+strValLen+").</i></font>";
					}
						
					sb.append("<pre><code>");
					sb.append(cellStr.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
					sb.append("</code></pre>");
					sb.append(truncatedMsg);

					sb.append("</html>");

					return sb.toString();
				}
				catch (Exception ex) 
				{
					_logger.warn("Problems when open the URL '"+urlStr+"'. Caught: "+ex, ex); 
					return 
						"<html>Problems when open the URL '<code>"+urlStr+"</code>'.<br>"
						+ "Caught: <b>" + ex + "</b><br>"
						+ "<hr>"
						+ "<a href='" + CmToolTipSupplierDefault.OPEN_IN_EXTERNAL_BROWSER + urlStr + "'>Open tempfile in External Browser</a> (registered application for file extention <b>'.txt'</b> will be used)<br>"
						+ "Or copy the above filename, and open it in any application or text editor<br>"
						+ "<html/>";
				}
			}
			catch (Exception ex)
			{
				return "<html>Sorry problems when creating temporary file '"+tmpFile+"'<br>Caught: "+ex+"</html>";
			}
		} // end: if (info == null)
		//---------------------------------------------------------------------------
		// Known MIME Types
		//---------------------------------------------------------------------------
		else
		{
//			System.out.println("info.getName()           = |" + info.getName()           +"|.");
//			System.out.println("info.getMimeType()       = |" + info.getMimeType()       +"|.");
//			System.out.println("info.getMessage()        = |" + info.getMessage()        +"|.");
//			System.out.println("info.getFileExtensions() = |" + StringUtil.toCommaStr(info.getFileExtensions()) +"|.");

			//------------------------------------------------------------------------
			// IMAGE
			//------------------------------------------------------------------------
			String mimeType = info.getMimeType();
			if (mimeType != null && mimeType.startsWith("image/"))
			{
				boolean imageToolTipInline = Configuration.getCombinedConfiguration().getBooleanProperty("QueryWindow.tooltip.cellContent.image.inline.", false);
				if (imageToolTipInline)
				{
//					String bytesEncoded = Base64.encode(bytes);
					String bytesEncoded = Base64.encodeBase64String(bytes);
					
					StringBuilder sb = new StringBuilder();
					sb.append("<html>");
					sb.append("Cell content is an image of type: ").append(info).append("<br>");
					sb.append("<hr>");
					sb.append("<img src=\"data:").append(info.getMimeType()).append(";base64,").append(bytesEncoded).append("\" alt=\"").append(info).append("\"/>");
					sb.append("</html>");
//System.out.println("htmlImage: "+sb.toString());

					return sb.toString();
				}
				else
				{
					File tmpFile = null;
					try
					{
						String suffix = null;
						String[] extArr = info.getFileExtensions();
						if (extArr != null && extArr.length > 0)
							suffix = "." + extArr[0];
							
						// put content in a TEMP file 
						tmpFile = createTempFile("sqlw_image_tooltip_", suffix, bytes);

						boolean imageToolTipInlineLaunchBrowser = Configuration.getCombinedConfiguration().getBooleanProperty("QueryWindow.tooltip.cellContent.image.launchBrowser", false);
						if (imageToolTipInlineLaunchBrowser)
						{
							return openInLocalAppOrBrowser(info, wasBase64_decoded, guessedCharset, tmpFile);
						}
						else
						{
							ImageIcon tmpImage = new ImageIcon(bytes);
							int width  = tmpImage.getIconWidth();
							int height = tmpImage.getIconHeight();

							// calculate a new image size max 500x500, but keep image aspect ratio
							Dimension originSize   = new Dimension(width, height);
							Dimension boundarySize = new Dimension(500, 500);
							Dimension newSize      = SwingUtils.getScaledDimension(originSize, boundarySize);

//							StringBuilder sb = new StringBuilder();
//							sb.append("<html>");
//							sb.append("Cell content is an image of type: ").append(info).append("<br>");
//							sb.append("Using temp file: <code>").append(tmpFile).append("</code><br>");
//							sb.append("Width/Height: <code>").append(originSize.width).append(" x ").append(originSize.height).append("</code><br>");
//							sb.append("Size:  <code>").append(StringUtil.bytesToHuman(bytes.length, "#.#")).append("</code><br>");
//							sb.append("<hr>");
//							sb.append("<img src=\"file:///").append(tmpFile).append("\" alt=\"").append(info).append("\" width=\"").append(newSize.width).append("\" height=\"").append(newSize.height).append("\">");
//							sb.append("</html>");
//
//							return sb.toString();

							String urlStr = ("file:///"+tmpFile);
							try	
							{
								URL url = new URL(urlStr);
								
								StringBuilder sb = new StringBuilder();
								sb.append("<html>");
								sb.append("<h2>Tooltip for MIME Type '").append(mimeType).append("'</h2>");
								sb.append("Full Description of Content: ").append("<code>").append(info).append("</code><br>");
								if (wasBase64_decoded)
									sb.append("<i>Cell Content was decoded using 'decodeBase64', before actual content could be determined.</i><br>");
								sb.append("<br>");
								sb.append("Using temp file: <code>").append(tmpFile).append("</code><br>");
								sb.append("Width/Height: <code>").append(originSize.width).append(" x ").append(originSize.height).append("</code><br>");
								sb.append("File Size: <code>").append(StringUtil.bytesToHuman(tmpFile.length(), "#.#")).append("</code><br>");
								sb.append("<a href='").append(CmToolTipSupplierDefault.OPEN_IN_EXTERNAL_BROWSER + url).append("'>Open in External Browser</a> (registered application for mime type <b>'").append(mimeType).append("'</b> will be used)<br>");
								sb.append("<hr>");

								sb.append("<img src=\"file:///").append(tmpFile).append("\" alt=\"").append(info).append("\" width=\"").append(newSize.width).append("\" height=\"").append(newSize.height).append("\">");
								
								sb.append("</html>");

								return sb.toString();
							}
							catch (Exception ex) 
							{
								_logger.warn("Problems when open the URL '"+urlStr+"'. Caught: "+ex, ex); 
								return 
									"<html>Problems when open the URL '<code>"+urlStr+"</code>'.<br>"
									+ "Caught: <b>" + ex + "</b><br>"
									+ "<hr>"
									+ "<a href='" + CmToolTipSupplierDefault.OPEN_IN_EXTERNAL_BROWSER + urlStr + "'>Open tempfile in External Browser</a> (registered application for mime type <b>'" + mimeType + "'</b> will be used)<br>"
									+ "Or copy the above filename, and open it in any application or text editor<br>"
									+ "<html/>";
							}
						}
					}
					catch (Exception ex)
					{
						return "<html>Sorry problems when creating temporary file '"+tmpFile+"'<br>Caught: "+ex+"</html>";
					}
				}
			} // end: is "image/"

			//------------------------------------------------------------------------
			// HTML
			//------------------------------------------------------------------------
			else if (info.getName().equals("html"))
			{
				String cellStrStartUpper = cellStr.substring(0, 40).trim().toUpperCase();
//System.out.println("cellStrStartUpper=|"+cellStrStartUpper+"|");

				// newer html versions, just use the "default" browser, so create a file, and kick it off
				if (    cellStrStartUpper.startsWith("<!DOCTYPE HTML")
				     || cellStrStartUpper.startsWith("<HTML ")
				   )
				{
					File tmpFile = null;
					try
					{
						// put content in a TEMP file 
						tmpFile = createTempFile("sqlw_html_tooltip_", ".html", bytes);

//						boolean launchBrowserOnHtmlTooltip = Configuration.getCombinedConfiguration().getBooleanProperty("QueryWindow.tooltip.cellContent.html.launchBrowser", true);
//						if (launchBrowserOnHtmlTooltip)
//						{
//							return openInLocalAppOrBrowser(info, wasBase64_decoded, tmpFile);
//						}
//						else
//							return cellStr;

						return openInLocalAppOrBrowser(info, wasBase64_decoded, guessedCharset, tmpFile);
					}
					catch (Exception ex) 
					{
						return "<html>Sorry problems when creating temporary file '"+tmpFile+"'<br>Caught: "+ex+"</html>";
					}
				}
				else // probably starts with '<html>' ... which should be HTML Version 4 or less
				{
					return cellStr;
				}
			}

			//------------------------------------------------------------------------
			// XML
			//------------------------------------------------------------------------
			else if (info.getName().equals("xml")) // ?xml version="1.1" encoding="UTF-8"?>  XXXX 
			{
//				StringBuilder sb = new StringBuilder();
//				sb.append("<html>");
//				sb.append("Cell content is <i>XML</i>, so displaying it as formated XML. Origin length="+cellStr.length()+"<br>");
//				sb.append("<hr>");
//				sb.append("<pre><code>");
//				sb.append(StringUtil.xmlFormat(cellStr).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
//				sb.append("</code></pre>");
//				sb.append("</html>");
//                
//				return sb.toString();
				
				File tmpFile = null;
				try
				{
					// put content in a TEMP file 
					tmpFile = createTempFile("sqlw_XML_tooltip_", ".xml", bytes);

					// Compose ToolTip HTML (with content, & a LINK to be opened in "browser")
					String urlStr = ("file:///"+tmpFile);
					try	
					{
						URL url = new URL(urlStr);
						
						StringBuilder sb = new StringBuilder();
						sb.append("<html>");
						sb.append("<h2>Tooltip for MIME Type '").append(mimeType).append("'</h2>");
						sb.append("Full Description of Content: ").append("<code>").append(info).append("</code><br>");
						if (wasBase64_decoded)
							sb.append("<i>Cell Content was decoded using 'decodeBase64', before actual content could be determined.</i><br>");
						sb.append("<br>");
						sb.append("Using temp file: <code>").append(tmpFile).append("</code><br>");
						sb.append("File Size: <code>").append(StringUtil.bytesToHuman(tmpFile.length(), "#.#")).append("</code><br>");
						sb.append("Guessed Charset: <code>").append(guessedCharset).append("</code><br>");
						sb.append("<a href='").append(CmToolTipSupplierDefault.OPEN_IN_EXTERNAL_BROWSER + url).append("'>Open in External Browser</a> (registered application for mime type <b>'").append(mimeType).append("'</b> will be used)<br>");
						sb.append("<hr>");

						int maxDisplayLenKb = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_TOOLTIP_XML_INLINE_MAX_SIZE_KB, DEFAULT_TOOLTIP_XML_INLINE_MAX_SIZE_KB);
						if (cellStr.length() < maxDisplayLenKb * 1024)
						{
    						sb.append("<pre><code>");
    						sb.append(StringUtil.xmlFormat(cellStr).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
    						sb.append("</code></pre>");
						}
						else
						{
    						sb.append("<font color='red'>Sorry the content is to big to display <b>here</b>, please open the file in above link!</font><br>");
    						sb.append("Current max size is: "+maxDisplayLenKb+" KB<br>");
    						sb.append("Note: This can be changed with property '"+PROPKEY_TOOLTIP_XML_INLINE_MAX_SIZE_KB+"'<br>");
						}

						sb.append("</html>");

						return sb.toString();
					}
					catch (Exception ex) 
					{
						_logger.warn("Problems when open the URL '"+urlStr+"'. Caught: "+ex, ex); 
//System.out.println("ResultSetJXTablegetContentSpecificToolTipText(): XML String causing problem=|"+cellStr+"|.");
						return 
							"<html>Problems when open the URL '<code>"+urlStr+"</code>'.<br>"
							+ "Caught: <b>" + ex + "</b><br>"
							+ "<hr>"
							+ "<a href='" + CmToolTipSupplierDefault.OPEN_IN_EXTERNAL_BROWSER + urlStr + "'>Open tempfile in External Browser</a> (registered application for mime type <b>'" + mimeType + "'</b> will be used)<br>"
							+ "Or copy the above filename, and open it in any application or text editor<br>"
							+ "<html/>";
					}
				}
				catch (Exception ex)
				{
					return "<html>Sorry problems when creating temporary file '"+tmpFile+"'<br>Caught: "+ex+"</html>";
				}
			}

			//------------------------------------------------------------------------
			// MIME Types NOT HANDLED in above code (open file...)
			//------------------------------------------------------------------------
//System.out.println("getContentSpecificToolTipText() unhandled mime type: info.getName()='"+info.getName()+"'.");
			// If "document type" isn't handle above, lets go "generic" and launch a browser with the registered content...
			boolean launchBrowserOnUnknownMimeTypes = Configuration.getCombinedConfiguration().getBooleanProperty("QueryWindow.tooltip.cellContent.unknown.launchBrowser", true);
			if (launchBrowserOnUnknownMimeTypes)
			{
				String[] fileExtentions = info.getFileExtensions();
				String fileExt = "txt";
				if (fileExtentions != null && fileExtentions.length >= 1)
					fileExt = fileExtentions[0];

//System.out.println("getContentSpecificToolTipText() unhandled mime type: choosen file extention='"+fileExt+"' all extentions: "+StringUtil.toCommaStr(fileExtentions));
				
				String mimeTypeName = info.getName();
//				boolean promptExternalAppForThisMimeType = Configuration.getCombinedConfiguration().getBooleanProperty("QueryWindow.tooltip.cellContent.mimetype."+mimeTypeName+".launchExternalTool.ask", true);
//				boolean launchExternalAppForThisMimeType = Configuration.getCombinedConfiguration().getBooleanProperty("QueryWindow.tooltip.cellContent.mimetype."+mimeTypeName+".launchExternalTool", false);
//				if (promptExternalAppForThisMimeType)
//				{
//					String msgHtml = 
//							"<html>" +
//							   "<h2>Tooltip for MIME Type '"+mimeTypeName+"'</h2>" +
//							   "Sorry I have no way to internally show the content type '"+mimeTypeName+"'.<br>" +
//							   "Do you want to view the content with any external tool?<br>" +
//							   "<ul>" +
//							   "  <li><b>Show, This time</b> - Ask me every type if it should be opened in an external tool.</li>" +
//							   "  <li><b>Show, Always</b> - Always do this in the future for '"+mimeTypeName+"' mime type (do not show this popup in the future).</li>" +
//							   "  <li><b>Never</b> Do NOT show me the content at all (do not show this popup in the future).</li>" +
//							   "  <li><b>Cancel</b> Do NOT show me the content this time.</li>" +
//							   "</ul>" +
//							"</html>";
//		
//						Object[] options = {
//								"Show, This time",
//								"Show, Always",
//								"Never",
//								"Cancel"
//								};
//						int answer = JOptionPane.showOptionDialog(this, 
//							msgHtml,
//							"View content in external tool.", // title
//							JOptionPane.YES_NO_CANCEL_OPTION,
//							JOptionPane.QUESTION_MESSAGE,
//							null,     //do not use a custom Icon
//							options,  //the titles of buttons
//							options[0]); //default button title
//		
//						if (answer == 0) 
//						{
//							launchExternalAppForThisMimeType = true;
//						}
//						else if (answer == 1)
//						{
//							launchExternalAppForThisMimeType = true;
//							Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
//							if (conf != null)
//							{
//								conf.setProperty("QueryWindow.tooltip.cellContent.mimetype."+mimeTypeName+".launchExternalTool.ask", false);
//								conf.setProperty("QueryWindow.tooltip.cellContent.mimetype."+mimeTypeName+".launchExternalTool", true);
//								conf.save();
//							}
//						}
//						else if (answer == 2)
//						{
//							launchExternalAppForThisMimeType = false;
//							Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
//							if (conf != null)
//							{
//								conf.setProperty("QueryWindow.tooltip.cellContent.mimetype."+mimeTypeName+".launchExternalTool.ask", false);
//								conf.setProperty("QueryWindow.tooltip.cellContent.mimetype."+mimeTypeName+".launchExternalTool", false);
//								conf.save();
//							}
//						}
//						else
//						{
//							launchExternalAppForThisMimeType = false;
//						}
//				}
				boolean launchExternalAppForThisMimeType = true;				
				if (launchExternalAppForThisMimeType)
				{
					File tmpFile = null;
					try
					{
						// put content in a TEMP file 
						tmpFile = createTempFile("sqlw_mime_type_"+mimeTypeName+"_tooltip_", fileExt, bytes);

						return openInLocalAppOrBrowser(info, wasBase64_decoded, guessedCharset, tmpFile);
					}
					catch (Exception ex) 
					{
						return "<html>Sorry problems when creating temporary file '"+tmpFile+"'<br>Caught: "+ex+"</html>";
					}
				}
				else
				{
					return info.toString();
				}
			}
			else
			{
				return info.toString();
			}
		}
	}
	
	private File createTempFile(String prefix, String suffix, byte[] bytes) 
	throws IOException
	{
		// add "." if the suffix doesn't have that
		if (StringUtil.hasValue(suffix) && !suffix.startsWith("."))
			suffix = "." + suffix;

		File tmpFile = File.createTempFile(prefix, suffix);
		tmpFile.deleteOnExit();
		FileOutputStream fos = new FileOutputStream(tmpFile);
		fos.write(bytes);
		fos.close();
		
		return tmpFile;
	}

	private String openInLocalAppOrBrowser(ContentInfo info, boolean wasBase64_decoded, String guessedCharset, File tmpFile)
	{
		String mimeType = "UNKONW";
		if (info != null)
			mimeType = info.getMimeType();

		// open the default Browser
		if (Desktop.isDesktopSupported())
		{
			Desktop desktop = Desktop.getDesktop();
			if ( desktop.isSupported(Desktop.Action.BROWSE) )
			{
				String fileExt = FilenameUtils.getExtension(tmpFile.getAbsolutePath());
				String urlStr = ("file:///"+tmpFile);
				try	
				{
					URL url = new URL(urlStr);
//					desktop.browse(url.toURI()); 
//					return 
//							"<html>"
//							+ "Opening the contect in the registered application (or browser)<br>"
//							+ "The Content were saved in the temporary file: "+tmpFile+"<br>"
//							+ "And opened using local application using URL: "+url+"<br>"
//							+ "<html/>";

					StringBuilder sb = new StringBuilder();
					sb.append("<html>");
					sb.append("<h2>Tooltip for MIME Type '").append(mimeType).append("'</h2>");
					sb.append("Full Description of Content: ").append("<code>").append(info).append("</code><br>");
					if (wasBase64_decoded)
						sb.append("<i>Cell Content was decoded using 'decodeBase64', before actual content could be determined.</i><br>");
					sb.append("<br>");
					sb.append("This type may not be possible to show in the current tooltip window!<br>");
					sb.append("<br>");
					sb.append("Using temp file: <code>").append(tmpFile).append("</code><br>");
					sb.append("File Size: <code>").append(StringUtil.bytesToHuman(tmpFile.length(), "#.#")).append("</code><br>");
					sb.append("Guessed Charset: <code>").append(guessedCharset).append("</code><br>");
					sb.append("<hr>");
					sb.append("How do you want to view the content?");
					sb.append("<ul>");
					sb.append("  <li><a href='").append(CmToolTipSupplierDefault.OPEN_IN_EXTERNAL_BROWSER + url).append("'>Open in External Browser</a> (registered application for mime type <b>'").append(mimeType).append("'</b> or file extention <b>'").append(fileExt).append("'</b> will be used)</li>");
					sb.append("  <li><a href='").append(url                           ).append("'>Try to Open in this window</a></li>");
					sb.append("</ul>");
					sb.append("</html>");

					return sb.toString();

//					return 
//						"<html>"
//						+ "Open in External Browser: <a href='" + OPEN_IN_EXTERNAL_BROWSER + url + "'>" + url + "</a><br>"
//						+ "Try to Open Content in this window: <a href='" + url + "'>" + url + "</a><br>"
//						+ "<hr><br>"
//						+ "Opening the contect in the registered application (or browser)<br>"
//						+ "The Content were saved in the temporary file: "+tmpFile+"<br>"
//						+ "And opened using local application using URL: "+url+"<br>"
//						+ "<html/>";
				}
				catch (Exception ex) 
				{
					_logger.warn("Problems when open the URL '"+urlStr+"'. Caught: "+ex, ex); 
					return 
						"<html>Problems when open the URL '<code>"+urlStr+"</code>'.<br>"
						+ "Caught: <b>" + ex + "</b><br>"
						+ "<hr>"
						+ "<a href='" + CmToolTipSupplierDefault.OPEN_IN_EXTERNAL_BROWSER + urlStr + "'>Open tempfile in External Browser</a> (registered application for mime type <b>'" + mimeType + "'</b> or file extention <b>'" + fileExt + "'</b> will be used)<br>"
						+ "Or copy the above filename, and open it in any application or text editor<br>"
						+ "<html/>";
				}
			}
		}
		return 
			"<html>"
			+ "Desktop browsing is not supported.<br>"
			+ "But the file '"+tmpFile+"' was produced."
			+ "<hr>"
			+ "Note: You can still open the above file in any application!"
			+ "<html/>";
	}

//	THE BELOW NEEDS MORE WORK, but the idea is that "somehow" it's called from SqlWindow - ResultSetTable - rightClickMenu - Select PARENT/CHILD rows...
//	public enum ForeignKey
//	{
//		Parent, Child
//	};
//	public String getForeignKeySqlForSelectedRows(DbxConnection conn, JMenu menu, ForeignKey fkType)
//	{
//		int[] selRows = getSelectedRows();
//
//		TableModel tm = getModel();
//		if (tm instanceof ResultSetTableModel)
//		{
//			ResultSetTableModel rstm = (ResultSetTableModel) tm;
//			List<String> uniqueTables = rstm.getRsmdReferencedTableNames();
//			if (uniqueTables.isEmpty())
//			{
//				menu.add(new JMenuItem("No tables refrences was found in the ResultSet"));
//			}
//			else
//			{
//				for (String tabname : uniqueTables)
//				{
//System.out.println("getForeignKeySqlForSelectedRows(): tabname=|" + tabname + "|");
//					if (tabname.startsWith("/"))
//						tabname = tabname.substring(1); // Remove first "/"
//
//					
//					try
//					{
//						DatabaseMetaData dbmd = conn.getMetaData();
//						
//						SqlObjectName sqlObj = new SqlObjectName(conn, tabname);
//						String catName    = sqlObj.getCatalogNameNull(); 
//						String schemaName = sqlObj.getSchemaNameNull(); 
//
//						ResultSetTableModel mdFkOut = new ResultSetTableModel( dbmd.getImportedKeys(catName, schemaName, tabname), "getImportedKeys");
//						ResultSetTableModel mdfkIn  = new ResultSetTableModel( dbmd.getExportedKeys(catName, schemaName, tabname), "getExportedKeys");
//
//System.out.println("FK-OUT: \n" + mdFkOut.toAsciiTableString());
//System.out.println("FK-IN: \n" + mdfkIn.toAsciiTableString());
////						ResultSet rs = conn.getMetaData().getImportedKeys(null, schemaName, tabname);
////						TableInfo.getFkOutboundDesc // dbxtune.ui.autocomplete.completions.TableInfo.java
//					}
//					catch (SQLException e)
//					{
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//				
//			}
//		}
//
//		return "";
//	}
	
	public enum DmlOperation
	{
		Insert, Update, Delete
	};
	public String getDmlForSelectedRows(DmlOperation dmlOperation)
	{
		boolean doReplaceNewline   = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_NL_REPLACE      , DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_NL_REPLACE);
		String  replaceNewlineStr  = Configuration.getCombinedConfiguration().getProperty       (PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_NL_REPLACE_STR  , DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_NL_REPLACE_STR);

		boolean doStmntTerminator  = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_STMNT_TERM      , DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_STMNT_TERM);
		String  stmntTerminatorStr = Configuration.getCombinedConfiguration().getProperty       (PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_STMNT_TERM_STR  , DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_STMNT_TERM_STR);

		boolean doReplaceNull      = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_NULL_REPLACE    , DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_NULL_REPLACE);
		String  replaceNullStr     = Configuration.getCombinedConfiguration().getProperty       (PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_NULL_REPLACE_STR, DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_NULL_REPLACE_STR);

		boolean includeDbname      = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_INCLUDE_DBNAME  , DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_INCLUDE_DBNAME);
		boolean includeSchema      = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_INCLUDE_SCHEMA  , DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_INCLUDE_SCHEMA);

		boolean addQuotedIdent     = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI          , DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI);
		String  qiBeginStr         = Configuration.getCombinedConfiguration().getProperty       (PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI_BEGIN_STR, DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI_BEGIN_STR);
		String  qiEndStr           = Configuration.getCombinedConfiguration().getProperty       (PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI_END_STR  , DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI_END_STR);

		String q1 = "";
		String q2 = "";
		if (addQuotedIdent)
		{
			q1 = qiBeginStr;
			q2 = qiEndStr;
		}
		
		int[] selRows = getSelectedRows();

		String sqlname = "SOME_TABLE_NAME";
		TableModel tm = getModel();
		if (tm instanceof ResultSetTableModel)
		{
			ResultSetTableModel rstm = (ResultSetTableModel) tm;
			List<String> uniqueTables = rstm.getRsmdReferencedTableNames();
			if (uniqueTables.size() == 1)
				sqlname = uniqueTables.get(0);
			else
			{
//				for (String tab : uniqueTables)
//					tabname = tabname + "/" + tab;
				sqlname = "QUERY_REFERENCED_" + uniqueTables.size() + "_TABLES";
			}
		}
		if (sqlname.startsWith("/"))
			sqlname = sqlname.substring(1); // Remove first "/"

		// Parse the table and break it up into: dbname.schema.table
//		SqlObjectName sqlObj = new SqlObjectNameSimple(tabname);
		String dbname  = "";
		String schname = "";
		String tabname = "";
		String[] sa = sqlname.split("\\.");
		if      (sa.length >= 3) { dbname = sa[sa.length - 3];  schname = sa[sa.length - 2]; tabname = sa[sa.length - 1]; }
		else if (sa.length >= 2) { dbname = "";                 schname = sa[sa.length - 2]; tabname = sa[sa.length - 1]; } 
		else if (sa.length >= 1) { dbname = "";                 schname = "";                tabname = sa[sa.length - 1]; }

		if ("-none-".equals(tabname) || "".equals(tabname))
			tabname = "-tablename-";

		sqlname = q1 + tabname + q2;
		if (includeSchema && StringUtil.hasValue(schname)) sqlname = q1 + schname + q2 + '.' + q1 + sqlname + q2;
		if (includeDbname && StringUtil.hasValue(dbname )) sqlname = q1 + dbname  + q2 + '.' + sqlname;

		// If sqlname name contains dbname/schema
//		sqlname = sqlname.replace(".", "].[");

		// add "generic" quoted identifiers (in Sybase and SQL Server format... which will be replaced at execution with the DBMS specific chars)
//		sqlname = "[" + sqlname + "]";
		
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO ").append(sqlname).append(" (");
		for (int c=0; c<getColumnCount(); c++)
		{
			String colName = getColumnName(c);
			if (addQuotedIdent)
			{
				// If we are using '[]' as Quoted Identifier, escape ']' into ']]'
				if ("]".equals(q2))
					colName.replace("]", "]]");

				colName = q1 + colName + q2;  // add '[' and ']' AROUND the column, if colName contains ']' escape that with a "]]"
			}
			sb.append(colName).append(", ");
		}
		sb.replace(sb.length()-2, sb.length(), ""); // remove last comma
		sb.append(") VALUES(");
		
		String insIntoStr = sb.toString();
		sb.setLength(0);

		// Figure out the MAX length for each columns values (so we can format it in a nice way)
		int[] colMaxLen = new int[getColumnCount()];
		for (int r : selRows)
		{
			for (int c=0; c<getColumnCount(); c++)
			{
				Object val = getValueAt(r, c);
				if (ResultSetTableModel.DEFAULT_NULL_REPLACE.equals(val))
					val = null;

				String nullStr = "NULL";
				if (doReplaceNull)
					nullStr = replaceNullStr;

				if (val == null)
					val = nullStr;

				int adjustLength = 0;
//				if (needsQuotes(r, c, val))
//					adjustLength = 2;
				if (val instanceof String)
				{
					adjustLength += StringUtils.countMatches((String)val, "'");
					if (doReplaceNewline)
					{
						int nlCnt = 0;
						nlCnt += StringUtils.countMatches((String)val, "\r");
						nlCnt += StringUtils.countMatches((String)val, "\n");
						adjustLength += nlCnt * replaceNewlineStr.length();
					}
				}
				
				colMaxLen[c] = Math.max(colMaxLen[c], (String.valueOf(val).length() + adjustLength) ); 
			}
		}

		for (int r : selRows)
		{
			sb.append(insIntoStr);
			for (int c=0; c<getColumnCount(); c++)
			{
				Object val = getValueAt(r, c);
				if (ResultSetTableModel.DEFAULT_NULL_REPLACE.equals(val))
					val = null;

				int adjustLength = 0;
				if (needsQuotes(r, c, val))
					adjustLength = 2;

				if (val != null && needsQuotes(r, c, val))
				{
					if (val instanceof String)
					{
						// Escape ' into ''
						val = val.toString().replace("'", "''");
						if (doReplaceNewline)
						{
							val = val.toString().replace("\r\n", replaceNewlineStr);
							val = val.toString().replace("\n",   replaceNewlineStr);
						}
					}
					int spaceCnt = colMaxLen[c] - String.valueOf(val).length();
					sb.append("'").append(val).append("'").append(", ").append(StringUtil.fillSpace(spaceCnt));
				}
				else
				{
					String nullStr = "NULL";
					if (doReplaceNull)
						nullStr = replaceNullStr;

					if (val == null)
						val = nullStr;

					int spaceCnt = colMaxLen[c] - String.valueOf(val).length();
					sb.append(val).append(", ").append(StringUtil.fillSpace(spaceCnt + adjustLength));
				}
			}
			int lastCommaPos = sb.lastIndexOf(", ");
			sb.replace(lastCommaPos, sb.length(), ""); // remove last comma
			sb.append(")");
			
			if (doStmntTerminator)
			{
				String tmpStmntTerminatorStr = stmntTerminatorStr;
				if (stmntTerminatorStr.startsWith("\\n"))
					tmpStmntTerminatorStr = stmntTerminatorStr.substring(2);
				sb.append(tmpStmntTerminatorStr);
			}

			// Simply end with a newline
			sb.append("\n");
		}
		return sb.toString();
	}

	private boolean needsQuotes(int row, int col, Object val)
	{
		TableModel tm = getModel();
		if (tm instanceof ResultSetTableModel)
		{
			ResultSetTableModel rstm = (ResultSetTableModel) tm;
			int sqlType = rstm.getSqlType(col);

			// Return the "object" via getXXX method for "known" datatypes
			switch (sqlType)
			{
			case java.sql.Types.BIT:           return false;
			case java.sql.Types.TINYINT:       return false;
			case java.sql.Types.SMALLINT:      return false;
			case java.sql.Types.INTEGER:       return false;
			case java.sql.Types.BIGINT:        return false;
			case java.sql.Types.FLOAT:         return false;
			case java.sql.Types.REAL:          return false;
			case java.sql.Types.DOUBLE:        return false;
			case java.sql.Types.NUMERIC:       return false;
			case java.sql.Types.DECIMAL:       return false;
			case java.sql.Types.CHAR:          return true;
			case java.sql.Types.VARCHAR:       return true;
			case java.sql.Types.LONGVARCHAR:   return true;
			case java.sql.Types.DATE:          return true;
			case java.sql.Types.TIME:          return true;
			case java.sql.Types.TIMESTAMP:     return true;
			case java.sql.Types.BINARY:        return false;
			case java.sql.Types.VARBINARY:     return false;
			case java.sql.Types.LONGVARBINARY: return false;
			case java.sql.Types.NULL:          return false;
			case java.sql.Types.OTHER:         return false;
			case java.sql.Types.JAVA_OBJECT:   return false;
			case java.sql.Types.DISTINCT:      return false;
			case java.sql.Types.STRUCT:        return false;
			case java.sql.Types.ARRAY:         return false;
			case java.sql.Types.BLOB:          return false;
			case java.sql.Types.CLOB:          return true;
			case java.sql.Types.REF:           return false;
			case java.sql.Types.DATALINK:      return false;
			case java.sql.Types.BOOLEAN:       return false;

			//------------------------- JDBC 4.0 -----------------------------------
			case java.sql.Types.ROWID:         return false;
			case java.sql.Types.NCHAR:         return true;
			case java.sql.Types.NVARCHAR:      return true;
			case java.sql.Types.LONGNVARCHAR:  return true;
			case java.sql.Types.NCLOB:         return true;
			case java.sql.Types.SQLXML:        return true;

			//------------------------- UNHANDLED TYPES  ---------------------------
			default:
				return false;
			}
		}
		
		if (val instanceof String)
			return true;
		return false;
	}

	
	/**
	 * Open a Dialog where you can change Settings... 
	 * @param jframe
	 * @return
	 */
	public static int showSettingsDialog(Frame owner)
	{
		return ResultSetJXTableSettingsDialog.showDialog(owner);
	}

}

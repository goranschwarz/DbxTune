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
package com.asetune.gui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.net.URI;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIDefaults;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.event.TableColumnModelExtListener;
import org.jdesktop.swingx.hyperlink.HyperlinkAction;
import org.jdesktop.swingx.renderer.CheckBoxProvider;
import org.jdesktop.swingx.renderer.ComponentProvider;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.HyperlinkProvider;
import org.jdesktop.swingx.renderer.IconValue;
import org.jdesktop.swingx.renderer.IconValues;
import org.jdesktop.swingx.renderer.LabelProvider;
import org.jdesktop.swingx.renderer.MappedValue;
import org.jdesktop.swingx.renderer.StringValue;
import org.jdesktop.swingx.renderer.StringValues;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.table.TableColumnModelExt;

import com.asetune.cm.CounterTableModel;
import com.asetune.cm.CountersModel;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.focusabletip.FocusableTip;
import com.asetune.gui.focusabletip.ToolTipHyperlinkResolver;
import com.asetune.tools.sqlw.ResultSetJXTable;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

public class GTable 
extends JXTable
{
	private static Logger       _logger = Logger.getLogger(GTable.class);

	private static final long	serialVersionUID			= 1L;
	private int					_lastMousePressedAtViewHeaderCol= -1;
	private int					_lastMousePressedAtModelCol	= -1;
	private int					_lastMousePressedAtModelRow	= -1;
//	private GTable              _thisTable                  = null;
//	private boolean             _hasNewModel                = true;
	private boolean             _tableStructureChangedFlag  = true;

	private FocusableTip        _focusableTip;

	/** If columns are reordered, save it after X seconds inactivity */
	protected Timer             _columnLayoutTimer          = null;

	public static final String  PROPKEY_NULL_REPLACE = "GTable.replace.null.with";
	public static final String  DEFAULT_NULL_REPLACE = "(NULL)";

	public static final String TOOLTIP_TYPE_NORMAL    = "ToolTipType=normal:";
	public static final String TOOLTIP_TYPE_FOCUSABLE = "ToolTipType=focusable:";

//	private static final String NULL_REPLACE = Configuration.getCombinedConfiguration().getProperty(PROPKEY_NULL_REPLACE, DEFAULT_NULL_REPLACE);
	private String NULL_REPLACE = Configuration.getCombinedConfiguration().getProperty(PROPKEY_NULL_REPLACE, DEFAULT_NULL_REPLACE);
//	private String _nullReplace = null;

	private Color _nullValueBgColor = null;

	private int _packMaxColWidth = SwingUtils.hiDpiScale(1000);
	public int getPackMaxColWidth() { return _packMaxColWidth; }
	public void setPackMaxColWidth(int maxWidth) { _packMaxColWidth = maxWidth; }

	public GTable()
	{
		this(null, null, null);
	}

	public GTable(String name)
	{
		this(null, null, name);
	}

	public GTable(String nullReplaceStr, Color nullValueBgColor)
	{
		this(null, null, null);
	}

	public GTable(String nullReplaceStr, Color nullValueBgColor, String name)
	{
		if (name != null)
			setName(name);

		setNullValueDisplay(nullReplaceStr);
		setNullValueDisplayBgColor(nullValueBgColor);
		init();
	}

	public GTable(TableModel tm, String name)
	{
		super(tm);

		if (name != null)
			setName(name);

		setNullValueDisplay(null);
		setNullValueDisplayBgColor(null);
		init();
	}

	@Override
	public <R extends TableModel> void setRowFilter(RowFilter<? super R, ? super Integer> filter)
	{
//		new Exception("DUMMY-EXCEPTION-to-track-orgin-of-setRowFilter(): filter="+filter).printStackTrace();
		super.setRowFilter(filter);
	}

	@Override
	public void packAll()
	{
		super.packAll();
		//packAllGrowOnly();
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

	
	public void setNullValueDisplay(String nullReplaceStr)
	{
		NULL_REPLACE = nullReplaceStr;
		if (nullReplaceStr == null)
			NULL_REPLACE = Configuration.getCombinedConfiguration().getProperty(PROPKEY_NULL_REPLACE, DEFAULT_NULL_REPLACE);
	}
	public String getNullValueDisplay()
	{
		return NULL_REPLACE;
	}

	public void setNullValueDisplayBgColor(Color color)
	{
		_nullValueBgColor = color;
		if (_nullValueBgColor == null)
			_nullValueBgColor = ResultSetJXTable.NULL_VALUE_COLOR;
	}
	public Color setNullValueDisplayBgColor()
	{
		return _nullValueBgColor;
	}

	public int getLastMousePressedAtViewHeaderCol()
	{
		return _lastMousePressedAtViewHeaderCol;
	}

	public int getLastMousePressedAtModelCol()
	{
		return _lastMousePressedAtModelCol;
	}

	public int getLastMousePressedAtModelRow()
	{
		return _lastMousePressedAtModelRow;
	}

	public boolean isLastMousePressedAtModelRowColValid()
	{
		return _lastMousePressedAtModelRow >= 0 && _lastMousePressedAtModelCol >= 0;
	}

	/** just wrap the super setModel() */
	@Override
	public void setModel(TableModel newModel)
	{
		setModelInternal(newModel, 0);
	}

	/** just wrap the super setModel() */
	private void setModelInternal(TableModel newModel, int neastLevel)
	{
		// In some cases, we get the following stack trace
		// -----------------------------------------------------
		// 2011-12-15 09:42:24,561 - WARN  - AWT-EventQueue-0          - 16  :SwingExceptionHandler.java     - Problems in AWT/Swing Event Dispatch Thread, Caught: java.lang.ArrayIndexOutOfBoundsException: 58 >= 54
		// java.lang.ArrayIndexOutOfBoundsException: 58 >= 54
		// 	at java.util.Vector.elementAt(Unknown Source)
		// 	at javax.swing.table.DefaultTableColumnModel.getColumn(Unknown Source)
		// 	at org.jdesktop.swingx.JXTable.getColumn(JXTable.java:2265)
		// 	at org.jdesktop.swingx.JXTable.columnAdded(JXTable.java:2231)
		// 	at javax.swing.table.DefaultTableColumnModel.fireColumnAdded(Unknown Source)
		// 	at javax.swing.table.DefaultTableColumnModel.addColumn(Unknown Source)
		// 	at org.jdesktop.swingx.table.DefaultTableColumnModelExt.addColumn(DefaultTableColumnModelExt.java:198)
		// 	at org.jdesktop.swingx.JXTable.createAndAddColumns(JXTable.java:2601)
		// 	at org.jdesktop.swingx.JXTable.createDefaultColumnsFromModel(JXTable.java:2575)
		// 	at javax.swing.JTable.tableChanged(Unknown Source)
		// 	at org.jdesktop.swingx.JXTable.tableChanged(JXTable.java:1529)
		// 	at com.asetune.gui.swing.GTable.privateTableChanged(GTable.java:711)
		// 	at com.asetune.gui.swing.GTable.tableChanged(GTable.java:696)
		// 	at javax.swing.JTable.setModel(Unknown Source)
		// 	at org.jdesktop.swingx.JXTable.setModel(JXTable.java:1609)
		// 	at com.asetune.gui.swing.GTable.setModel(GTable.java:113)
		// 	at com.asetune.gui.TabularCntrPanel.setDisplayCm(TabularCntrPanel.java:379)
		// 	at com.asetune.gui.MainFrame.actionPerformed(MainFrame.java:1163)
		// 	at javax.swing.AbstractButton.fireActionPerformed(Unknown Source)
		//...swing stack code deleted...
		// -----------------------------------------------------
		// Then switching to InMemory View is not possible
		// so, lets add a try catch...
		try
		{
//			// No need to continue if it's the same model ????
//			TableModel currentModel = getModel();
//			if (newModel.equals(currentModel))
//			{
//				System.out.println("TCP: same model as before: currentModel="+currentModel);
//				return;
//			}
				
//			_hasNewModel = true;
			super.setModel(newModel);
			
			if (newModel instanceof CountersModel)
			{
//				String tabName = _thisTable.getName();
				String tabName = GTable.this.getName();
				if (StringUtil.isNullOrBlank(tabName))
				{
					CountersModel cm = (CountersModel) newModel;
//					_thisTable.setName(cm.getName());
					GTable.this.setName(cm.getName());
				}
			}
			loadColumnLayout();
		}
		catch (IndexOutOfBoundsException ex)
		{
			// If called for first time and we get problems, try once more...
			if (neastLevel == 0)
			{
				_logger.info("Problems setting GTable.setModel(). (first time call) Table/Component name='"+getName()+"'. TableModel=(class="+(newModel==null?"null":newModel.getClass().getName())+", toString='"+newModel+"'). I guess this is a bug in JXTable, which doesn't take into account that we have hidden columns... Caught: "+ex);
				setModelInternal(newModel, neastLevel++);
			}
			else
			{
				_logger.warn("Problems setting GTable.setModel(). (second time call) Table/Component name='"+getName()+"'. TableModel=(class="+(newModel==null?"null":newModel.getClass().getName())+", toString='"+newModel+"'). I guess this is a bug in JXTable, which doesn't take into account that we have hidden columns... Caught: "+ex, ex);
			}
		}
		catch (Throwable ex)
		{
			_logger.warn("Problems setting GTable.setModel(). Table/Component name='"+getName()+"'. TableModel=(class="+(newModel==null?"null":newModel.getClass().getName())+", toString='"+newModel+"').", ex);
		}
	}

	public int getBigDecimalFormatMinimumFractionDigits()
	{
		return 0;
	}

	public void setBigDecimalFormatMinimumFractionDigits(final int digits)
	{
		//
		// Cell renderer changes to "Rate" Counters
		//
		// The normal formatter doesn't add '.0' if values are even
		// Make '0'     -> '0.0' 
		//  and '123'   -> '123.0' 
		//  and '123.5' -> '123.5'
		@SuppressWarnings("serial")
		StringValue sv = new StringValue() 
		{
			NumberFormat nf = null;
			{ // init/constructor section
				try
				{
					nf = new DecimalFormat();
					nf.setMinimumFractionDigits(digits);
				}
				catch (Throwable t)
				{
					nf = NumberFormat.getInstance();
				}
			}
			@Override
			public String getString(Object value) 
			{
				if ( ! (value instanceof BigDecimal) ) 
					return StringValues.TO_STRING.getString(value);
				return nf.format(value);
			}
		};
		// bind the RATE values (which happens to be BigDecimal)
		setDefaultRenderer(BigDecimal.class, new DefaultTableRenderer(sv, JLabel.RIGHT));
	}

	@Override
	// NOTE: this is grabbed from "super" 
	protected void createDefaultRenderers() 
	{
		// super.createDefaultRenderers();

		defaultRenderersByColumnClass = new UIDefaults(8, 0.75f);

		// configured default table renderer (internally LabelProvider)
		setDefaultRenderer(Object.class, new DefaultTableRendererNullAware());
		setDefaultRenderer(Number.class, new DefaultTableRendererNullAware(StringValues.NUMBER_TO_STRING, JLabel.RIGHT));
		setDefaultRenderer(Date.class,   new DefaultTableRendererNullAware(StringValues.DATE_TO_STRING));

		// use the same center aligned default for Image/Icon
		TableCellRenderer renderer = new DefaultTableRendererNullAware(new MappedValue(StringValues.EMPTY, IconValues.ICON), JLabel.CENTER);
		setDefaultRenderer(Icon.class, renderer);
		setDefaultRenderer(ImageIcon.class, renderer);

		// use a ButtonProvider for booleans
		setDefaultRenderer(Boolean.class, new DefaultTableRendererNullAware(new CheckBoxProvider()));

		try {
			setDefaultRenderer(URI.class, new DefaultTableRendererNullAware(new HyperlinkProvider(new HyperlinkAction())) );
		} catch (Exception e) {
			// nothing to do - either headless or Desktop not supported
		}
	}

	private class DefaultTableRendererNullAware extends DefaultTableRenderer
	{
		private static final long serialVersionUID = 1L;
		private StringValue       _nullStr = new StringValue()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getString(Object value)
			{
				return NULL_REPLACE;
			}
		};
		private TableCellRenderer _nullValue= new DefaultTableRenderer(new LabelProvider(_nullStr));

		public DefaultTableRendererNullAware()                                                            { super();}
		public DefaultTableRendererNullAware(ComponentProvider<?> componentProvider)                      { super(componentProvider); }
		public DefaultTableRendererNullAware(StringValue converter)                                       { super(converter); }
		public DefaultTableRendererNullAware(StringValue converter, int alignment)                        { super(converter, alignment); }
		@SuppressWarnings("unused")
		public DefaultTableRendererNullAware(StringValue stringValue, IconValue iconValue)                { super(stringValue, iconValue); }
		@SuppressWarnings("unused")
		public DefaultTableRendererNullAware(StringValue stringValue, IconValue iconValue, int alignment) { super(stringValue, iconValue, alignment); }

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			if (value == null)
				return _nullValue.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
		
	}

	private void init()
	{
		// wait 1 seconds before column layout is saved, this simply means less config writes...
		_columnLayoutTimer = new Timer(1000, new ColumnLayoutTimerAction(this));
//		_thisTable = this;

		
		//
		// Cell renderer changes to "Rate" Counters
		//
		// The normal formatter doesn't add '.0' if values are even
		// Make '0'     -> '0.0' 
		//  and '123'   -> '123.0' 
		//  and '123.5' -> '123.5'
		setBigDecimalFormatMinimumFractionDigits(getBigDecimalFormatMinimumFractionDigits());

		// ---------------------------------------------------------------------------------------------
		// Below is Cell renderer for:  java.sql.Timestamp, java.sql.Date, java.sql.Time
		// ---------------------------------------------------------------------------------------------

		// java.sql.Timestamp format
		@SuppressWarnings("serial")
		StringValue svTimestamp = new StringValue() 
		{
//			DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
			String format = Configuration.getCombinedConfiguration().getProperty("GTable.cellRenderer.format.Timestamp", "yyyy-MM-dd HH:mm:ss.SSS");
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
    					_logger.warn("Problems in GTable when rendering type '"+value.getClass().getName()+"', object '"+value+"'. returning a toString instead. Caught: "+ignore);
    					return value==null ? NULL_REPLACE : value.toString(); 
    				}
				}
				else
				{
					return value==null ? NULL_REPLACE : value.toString();
				}
			}
		};
		setDefaultRenderer(java.sql.Timestamp.class, new DefaultTableRenderer(svTimestamp));

		// java.sql.Date format
		@SuppressWarnings("serial")
		StringValue svDate = new StringValue() 
		{
			String format = Configuration.getCombinedConfiguration().getProperty("GTable.cellRenderer.format.Date", "yyyy-MM-dd");
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
    					_logger.warn("Problems in GTable when rendering type '"+value.getClass().getName()+"', object '"+value+"'. returning a toString instead. Caught: "+ignore);
    					return value==null ? NULL_REPLACE : value.toString(); 
    				}
				}
				else
				{
					return value==null ? NULL_REPLACE : value.toString();
				}
			}
		};
		setDefaultRenderer(java.sql.Date.class, new DefaultTableRenderer(svDate));

		// java.sql.Time format
		@SuppressWarnings("serial")
		StringValue svTime = new StringValue() 
		{
			String format = Configuration.getCombinedConfiguration().getProperty("GTable.cellRenderer.format.Time", "HH:mm:ss");
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
    					_logger.warn("Problems in GTable when rendering type '"+value.getClass().getName()+"', object '"+value+"'. returning a toString instead. Caught: "+ignore);
    					return value==null ? NULL_REPLACE : value.toString(); 
    				}
				}
				else
				{
					return value==null ? NULL_REPLACE : value.toString();
				}
			}
		};
		setDefaultRenderer(java.sql.Time.class, new DefaultTableRenderer(svTime));

		//---------------------------------------------
		// NULL Values: SET BACKGROUND COLOR
		//---------------------------------------------
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				// Check NULL value
				Object cellValue = adapter.getValue();
				if (cellValue == null || NULL_REPLACE.equals(cellValue))
					return true;

				return false;
			}
		}, _nullValueBgColor, null));

		//--------------------------------------------------------------------
		// Add mouse listener to be used to identify what row/col we are at.
		// this is used from the context menu, to do copy of cell or row
		//--------------------------------------------------------------------
		addMouseListener(new MouseAdapter()
		{
			// public void mouseClicked(MouseEvent e)

			// Done on left&right click
			// if you any want left-click(select) use method mouseClicked()
			// instead
			@Override
			public void mousePressed(MouseEvent e)
			{
				_lastMousePressedAtModelCol = -1;
				_lastMousePressedAtModelRow = -1;

				Point p = new Point(e.getX(), e.getY());
				int col = columnAtPoint(p);
				int row = rowAtPoint(p);

				if ( row >= 0 && col >= 0 )
				{
					_lastMousePressedAtModelCol = convertColumnIndexToModel(col);
					_lastMousePressedAtModelRow = convertRowIndexToModel(row);
				}
			}
		});

		//--------------------------------------------------------------------
		// listen on changes in the column header.
		// Used to save/restore column order
		//--------------------------------------------------------------------
		TableColumnModelExtListener columnModelListener = new TableColumnModelExtListener() 
		{
			@Override
			public void columnPropertyChange(PropertyChangeEvent e) {}
			@Override
			public void columnMarginChanged(ChangeEvent e)          {columnMovedOrRemoved(null);}
			@Override
			public void columnSelectionChanged(ListSelectionEvent e){}

			@Override
			public void columnAdded(TableColumnModelEvent e)
			{
				// If a new model has been loaded AND it's the LAST column we are adding
				// then load the column layout
				//System.out.println("------columnAdded(): tabName='"+getName()+"', _hasNewModel="+_hasNewModel+", modelCount="+getModel().getColumnCount()+", getToIndex="+e.getToIndex()+".");
				
//				if (_hasNewModel && getModel().getColumnCount()-1 == e.getToIndex())
//				{
//					_logger.debug("columnAdded(): tabName='"+getName()+"', TIME TO LOAD COL ORDER.");
//					_hasNewModel = false;
//					_thisTable.loadColumnLayout();
//				}
//System.out.println("tabname="+StringUtil.left(getName(), 30)+", modelColCount-1="+(getModel().getColumnCount()-1)+", toIndex="+e.getToIndex());
				if (_tableStructureChangedFlag && getModel().getColumnCount()-1 == e.getToIndex())
				{
//if (getName().equals("CMobjActivity"))
//System.out.println("columnAdded(): tabName='"+getName()+"', TIME TO LOAD COL ORDER.");
					_logger.debug("columnAdded(): tabName='"+getName()+"', TIME TO LOAD COL ORDER.");
					_tableStructureChangedFlag = false;
//					_thisTable.loadColumnLayout();
					GTable.this.loadColumnLayout();
				}
			}

			@Override
			public void columnRemoved(TableColumnModelEvent e)      {columnMovedOrRemoved(e);}
			@Override
			public void columnMoved(TableColumnModelEvent e)        {columnMovedOrRemoved(e);}
			private void columnMovedOrRemoved(TableColumnModelEvent e)
			{
				if (_columnLayoutTimer.isRunning())
					_columnLayoutTimer.restart();
				else
					_columnLayoutTimer.start();
			}
		};
		getColumnModel().addColumnModelListener(columnModelListener);

		// Add mouse listener to the Column Header, used in 
		getTableHeader().addMouseListener(new MouseListener()
		{
			@Override public void mouseReleased(MouseEvent e) { columnHeaderMouseActivity(e); }
			@Override public void mouseExited  (MouseEvent e) { columnHeaderMouseActivity(e); }
			@Override public void mouseEntered (MouseEvent e) { columnHeaderMouseActivity(e); }
			@Override public void mouseClicked (MouseEvent e) { columnHeaderMouseActivity(e); }
			@Override public void mousePressed (MouseEvent e) { columnHeaderMouseActivity(e); }
		});

		// Set special Render to print multiple columns sorts
		getTableHeader().setDefaultRenderer(new MultiSortTableCellHeaderRenderer());

		// Set columnHeader popup menu
		getTableHeader().setComponentPopupMenu(createTableHeaderPopupMenu());

		//--------------------------------------------------------------------
		// New SORTER that toggles from DESCENDING -> ASCENDING -> UNSORTED
		//--------------------------------------------------------------------
		setSortOrderCycle(SortOrder.DESCENDING, SortOrder.ASCENDING, SortOrder.UNSORTED);
	}

	private void columnHeaderMouseActivity(MouseEvent e)
	{
		_lastMousePressedAtViewHeaderCol = -1;

		Point p = new Point(e.getX(), e.getY());
		int col = columnAtPoint(p);

		if ( col >= 0 )
		{
			_lastMousePressedAtViewHeaderCol = col;
		}
	}
	
	
	/**
	 * Creates the JMenu on the Component, this can be overrided by a subclass.
	 */
	public JPopupMenu createTableHeaderPopupMenu()
	{
		JPopupMenu popup = new JPopupMenu();

		popup.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				JPopupMenu p = (JPopupMenu) e.getSource();

				// remove all old items (if any)
				p.removeAll();

				// Add all columns to the menu if the column control is available
				if (isColumnControlVisible())
				{
					JMenuItem mi;

					// RESTORE ORIGINAL COLUMN LAYOUT
					mi = new JMenuItem("Reset to Original Column Layout");
					p.add(mi);
					mi.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							GTable.this.setOriginalColumnLayout();
						}
					});

					// ADJUST COLUMN WIDTH
					mi = new JMenuItem("Adjust Column Width, both shrink and grow."); // Resizes all columns to fit their content
					p.add(mi);
					mi.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							GTable.this.packAll();
						}
					});

					// ADJUST COLUMN WIDTH
					mi = new JMenuItem("Adjust Column Width, grow only."); // Resizes all columns to fit their content
					p.add(mi);
					mi.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							GTable.this.packAllGrowOnly();
						}
					});

					// Open Hide/View Dialog
					mi = new JMenuItem("Hide/View Column Dialog...");
					p.add(mi);
					mi.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							int ret = GTableHeaderPropertiesDialog.showDialog(null, GTable.this); // NOTE: owner is null here... so this we might want to fix
							if (ret == JOptionPane.OK_OPTION)
							{
							}
						}
					});

					// HIDE THIS COLUMN
					// Now get the column name, which we point at
					if ( getLastMousePressedAtViewHeaderCol() >= 0 )
					{
						final TableColumnExt tcx = (TableColumnExt) getColumnModel().getColumn(getLastMousePressedAtViewHeaderCol());
    
    					mi = new JMenuItem("Hide this column '"+tcx.getHeaderValue()+"'"); // Resizes all columns to fit their content
    					p.add(mi);
    					mi.addActionListener(new ActionListener()
    					{
    						@Override
    						public void actionPerformed(ActionEvent e)
    						{
								tcx.setVisible(false);
    						}
    					});
					}

					// Separator
					p.add(new JSeparator());
					
					// Create all column checkbox entries "on the fly"
					for (TableColumn tc : getColumns(true))
					{
						final TableColumnExt tcx = (TableColumnExt) tc;
	
						String  colName      = tcx.getHeaderValue() + "";
						boolean colIsVisible = tcx.isVisible();
	
						mi = new JCheckBoxMenuItem();
						mi.setText(colName);
						mi.setSelected(colIsVisible);
						mi.addActionListener(new ActionListener()
						{
							@Override
							public void actionPerformed(ActionEvent e)
							{
								JCheckBoxMenuItem mi = (JCheckBoxMenuItem) e.getSource();
								try
								{
									// If first time fails, try a second time
									try	{ tcx.setVisible(mi.isSelected()); }
									catch (IndexOutOfBoundsException ex)
									{
										_logger.info("Problems setting TableColumnExt.setVisible(). (first time exec) Table/Component name='"+getName()+"'. I guess this is a bug in JXTable, which doesn't take into account that we have hidden columns... Caught: "+ex);
										tcx.setVisible(mi.isSelected());
									}
								}
								catch (IndexOutOfBoundsException ex)
								{
									_logger.warn("Problems setting TableColumnExt.setVisible(). (second time exec) Table/Component name='"+getName()+"'. I guess this is a bug in JXTable, which doesn't take into account that we have hidden columns... Caught: "+ex);
								}
								catch (Throwable ex)
								{
									_logger.warn("Problems setting TableColumnExt.setVisible(). Table/Component name='"+getName()+"'. I guess this is a bug in JXTable, which doesn't take into account that we have hidden columns... Caught: "+ex, ex);
								}
							}
						});
	
						p.add(mi);
					}
				}
			}
			
			@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
			@Override public void popupMenuCanceled(PopupMenuEvent e) {}
		});

		return popup;
	}

//    /**
//     * Configures the enclosing <code>JScrollPane</code>.
//     */
//	@Override
//	protected void configureEnclosingScrollPane() 
//	{
//		super.configureEnclosingScrollPane();
//		configureTableWatermark();
//	}

//	/**
//	 * Configures the Watermark to anchor the <code>JScrollPane</code>.
//	 */
//	protected void configureTableWatermark()
//	{
//		Container p = getParent();
//		if ( p instanceof JViewport )
//		{
//			Container gp = p.getParent();
//			if ( gp instanceof JScrollPane )
//			{
//				JScrollPane scrollPane = (JScrollPane) gp;
//				// Make certain we are the viewPort's view and not, for
//				// example, the rowHeaderView of the scrollPane -
//				// an implementor of fixed columns might do this.
//				JViewport viewport = scrollPane.getViewport();
//				if ( viewport == null || viewport.getView() != this )
//				{
//					return;
//				}
//				
//				setWatermarkAnchor(scrollPane); // gets into some kind of lock/deadlock situation.
//			}
//		}
//	}

	/**
	 * Like findColumn() on the TableModel<br>
	 * Note: Case sensitive
	 * @return -1: If the column name is doesn't exists in the view (could be hidden) nor in the Model
	 */
	public int findViewColumn(String colName)
	{
		return findViewColumn(colName, true);
	}
	/**
	 * Like findColumn() on the TableModel<br>
	 * @return -1: If the column name is doesn't exists in the view (could be hidden) nor in the Model
	 */
	public int findViewColumn(String colName, boolean caseSensitive)
	{
		int viewColPos  = -1;
		int modelColPos = -1;

		// Get the model position
		TableModel tm = getModel();
		for (int c=0; c<tm.getColumnCount(); c++) 
		{
			if ( caseSensitive ?	colName.equals(tm.getColumnName(c)) : colName.equalsIgnoreCase(tm.getColumnName(c)) ) 
			{
				modelColPos = c;
				break;
			}
		}
		if (modelColPos < 0)
			return -1;

		viewColPos = convertColumnIndexToView(modelColPos);
		if (viewColPos < 0)
			_logger.debug(getName()+ ": findViewColumn('"+colName+"'): modelIndex="+modelColPos+", viewIndex="+viewColPos+", the column must be hidden in the view.");

		return viewColPos;
	}


    public void printColumnLayout(String prefix)
	{
		if (getColumnCount() == 0)
			return;

		// Get prefix name to use
		String cmName = getName(); // Name of the JTable component, which should be the CM name
		if (cmName == null)
		{
			_logger.debug("Can't print Column Layout, because the JTable has not been assigned a name. getName() on the JTable is null.");
			return;
		}

		TableColumnModel tcm = getColumnModel();

		for (TableColumn tc : getColumns(true))
		{
			TableColumnExt tcx     = (TableColumnExt) tc;
			String         colName = tc.getHeaderValue().toString();

			// Visible
			boolean colIsVisible = tcx.isVisible();

			// Sorted
			SortOrder colSort = getSortOrder(colName);

			// View/model position
			int colModelPos = tcx.getModelIndex();
			int colViewPos  = -1;
			try {colViewPos = tcm.getColumnIndex(colName);}
			catch (IllegalArgumentException ignore) {}

			System.out.println(prefix + "printColumnLayout() cm='"+cmName+"': colName="+StringUtil.left(colName,30)+", modelPos="+colModelPos+", viewPos="+colViewPos+", isVisible="+colIsVisible+", sort="+colSort+", identifier='"+tcx.getIdentifier()+"', toString="+tc);
		}
	}
	/**
	 * Load column order/layout from the saved vales in the temporary properties file.
	 */
	public void loadColumnLayout()
	{
//if ("CMobjActivity".equals(getName()))
//new Exception("loadColumnLayout() was CALLED from").printStackTrace();
//		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf == null) 
			return;

		if (getColumnCount() == 0)
			return;

		// Get prefix name to use
		String cmName = getName(); // Name of the JTable component, which should be the CM name
		if (StringUtil.isNullOrBlank(cmName))
		{
			_logger.debug("Can't load Column Layout, because the JTable has not been assigned a name. getName() on the JTable is null.");
			return;
		}

		long srvVersion = 0;
		if (getModel() instanceof CountersModel)
		{
			CountersModel cm = (CountersModel) getModel();
			if (cm.isRuntimeInitialized())
				srvVersion = cm.getServerVersion();
		}
		// get the values from configuration
		String confKey = cmName + ".gui.column.header.props.["+SwingUtils.getScreenResulutionAsString()+"]." + srvVersion;
		String confVal = conf.getProperty(confKey);
		if (confVal == null)
		{
			// Revert back to "previous" version
			confKey = cmName + ".gui.column.header.props.["+SwingUtils.getScreenResulutionAsString()+"]";
			confVal = conf.getProperty(confKey);
		}
		if (confVal == null)
		{
			loadColumnLayout(getPreferredColumnLayout());
			return;
		}

		// split on '; ' and stuff the entries in a Map object
		LinkedHashMap<String, ColumnHeaderPropsEntry> colProps = new LinkedHashMap<String, ColumnHeaderPropsEntry>();
		String[] strArr = confVal.split("; ");
		for (int i=0; i<strArr.length; i++)
		{
			try 
			{
				// each entry looks like: colName={modelPos=1,viewPos=1,isVisible=true,sort=unsorted}
				// where modelPos=int[0..999], viewPos=int[0..999], isVisible=boolean[true|false], sort=String[unsorted|ascending|descending]
				ColumnHeaderPropsEntry chpe = ColumnHeaderPropsEntry.parseKeyValue(strArr[i]);
				colProps.put(chpe._colName, chpe);
			}
			catch (ParseException e)
			{
				_logger.info("Problems parsing '"+confKey+"' with string '"+strArr[i]+"'. Caught: "+e);
				continue;
			}
		}

		// If table model and config are "out of sync", do not load
		if (colProps.size() != getModel().getColumnCount())
		{
			_logger.info(confKey + " has '"+colProps.size()+"' values and the table model has '"+getModel().getColumnCount()+"' columns. I will skip moving columns around, the original column layout will be used.");
			loadColumnLayout(getPreferredColumnLayout());
			return;
		}

		// Now move the columns in right position
		// make it recursive until no more has to be moved
		for (int i=0; i<colProps.size(); i++)
		{
			if (loadColumnLayout(colProps) == 0)
				break;
		}

		// SETTING SORT ORDER
		// Find the highest sorted column
		int maxSortOrderPos = -1;
		for (ColumnHeaderPropsEntry chpe : colProps.values())
			maxSortOrderPos = Math.max(maxSortOrderPos, chpe._sortOrderPos);

		// now APPLY the sorts in the correct order.
		// Starting with the highest number... 
		// The LAST one you do setSortOrder() will be SORT COLUMN 1
		for (int i=maxSortOrderPos; i>0; i--)
		{
			for (ColumnHeaderPropsEntry chpe : colProps.values())
			{
				if (chpe._sortOrderPos == i)
				{
					if (_logger.isDebugEnabled())
						_logger.debug(i+": Setting '"+StringUtil.left(chpe._colName,20)+"', viewPos="+chpe._viewPos+",  to "+chpe._sortOrder+", sortOrderPos="+chpe._sortOrderPos+", ModelColumnCount="+getModel().getColumnCount()+", RowSorterModelColumnCount="+getRowSorter().getModel().getColumnCount()+", name="+getName());

					if (chpe._viewPos < getRowSorter().getModel().getColumnCount())
						setSortOrder(chpe._viewPos, chpe._sortOrder);
					else
						_logger.debug("Can't set the sort order for column '"+chpe._colName+"'. viewPos < RowSorterModelColumnCount, this will be retried later? Info RowSorterModelColumnCount="+getRowSorter().getModel().getColumnCount()+", TableModelColumnCount="+getModel().getColumnCount()+", viewPos="+chpe._viewPos+", TableName="+getName());
				}
			}
		}
		
	}

	/**
	 * Override this to fetch the preferred Column Layout properties
	 * @return The desired layout. null or and empty HashMap simply does nothing 
	 */
	public Map<String, ColumnHeaderPropsEntry> getPreferredColumnLayout()
	{
		return null;
	}

	protected int loadColumnLayout(Map<String, ColumnHeaderPropsEntry> colProps)
	{
//System.out.println("GTable("+getName()+"): loadColumnLayout: " + (colProps == null ? "-NULL-" : colProps.keySet()) );
//new Exception("DUMMY_EXCEPTION").printStackTrace();
		if (colProps == null)
			return 0;
		if (colProps.isEmpty())
			return 0;

		int fixCount = 0;
		TableColumnModelExt tcmx = (TableColumnModelExt)getColumnModel();
		for (Map.Entry<String,ColumnHeaderPropsEntry> entry : colProps.entrySet()) 
		{
			String                 colName = entry.getKey();
			ColumnHeaderPropsEntry chpe    = entry.getValue();

			// Hide/show column
			TableColumnExt tcx = tcmx.getColumnExt(colName);
			if (tcx != null)
			{
				if ( chpe._isVisible == false && tcx.isVisible() )
				{
					_logger.trace("loadColumnLayout() cm='"+getName()+"': ACTION -> HIDE '"+colName+"'.");
					tcx.setVisible(false);
					fixCount++;
				}

				if ( chpe._isVisible == true && !tcx.isVisible() )
				{
					_logger.trace("loadColumnLayout() cm='"+getName()+"': ACTION -> SHOW '"+colName+"'.");
					tcx.setVisible(true);
					fixCount++;
				}
			}

			// Move column
			int colViewPos = -1;
			try {colViewPos = tcmx.getColumnIndex(colName);}
			catch (IllegalArgumentException ignore) {}
			
			int propViewPos = chpe._viewPos; 

			_logger.trace("loadColumnLayout() cm='"+getName()+"': info '"+StringUtil.left(colName,30)+"' colViewPos(from)='"+colViewPos+"', chpe._viewPos(to)='"+chpe._viewPos+"'.");
			if (colViewPos >= 0 && propViewPos >= 0)
			{
				if (colViewPos != propViewPos)
				{
					_logger.trace("loadColumnLayout() cm='"+getName()+"': ACTION -> MOVE '"+colName+"' from '"+colViewPos+"' -> '"+propViewPos+"'.");

					// hmmm, this will trigger columnMove
					// but we have the timer before saveColumnLayout is kicked of, so we should be fine
					// and also since we have already read it into local variables it doesn't matter.
					
					// If we have hidden columns, we might throw: java.lang.ArrayIndexOutOfBoundsException: 56 >= 56
					try 
					{ 
						if (propViewPos == ColumnHeaderPropsEntry.AS_LAST_VIEW_COLUMN)
						{
							// if it's the LAST position it looks like it "squizes" in "before last"
							//tcmx.moveColumn(colViewPos, tcmx.getColumnCount()-1); 
							tcmx.removeColumn(tcx); 
							tcmx.addColumn(tcx); 
						}
						else
						{
							tcmx.moveColumn(colViewPos, propViewPos); 
						}
					}
					catch (Throwable t) 
					{
						_logger.info ("loadColumnLayout() problems when calling tcmx.moveColumn(colViewPos, propViewPos): (to get stacktrace enable debug loggin) Caught: "+t); 
						_logger.debug("loadColumnLayout() problems when calling tcmx.moveColumn(colViewPos, propViewPos): Caught: "+t, t); 
					}

					fixCount++;
				}
			}

//			// sorting
//			SortOrder currentSortOrder = SortOrder.UNSORTED;
//			if (colViewPos >= 0) 
//				currentSortOrder = getSortOrder(colViewPos);
////if (getName().equals("CMobjActivity"))
////System.out.println("loadColumnLayout() SORT TO: cm='"+getName()+"': info '"+StringUtil.left(colName,30)+"' chpe._sortOrder='"+chpe._sortOrder+"', currentSortOrder='"+currentSortOrder+"'.");
//			if (chpe._sortOrder != currentSortOrder)
//			{
////if (getName().equals("CMobjActivity"))
////System.out.println("loadColumnLayout() CHANGING SORT ORDER to: chpe._viewPos="+chpe._viewPos+", chpe._sortOrder="+chpe._sortOrder);
//				_logger.trace("loadColumnLayout() SORT TO: cm='"+getName()+"': info '"+StringUtil.left(colName,30)+"' viewPos='"+chpe._viewPos+"', sortOrder(to)='"+chpe._sortOrder+"'.");
//				setSortOrder(chpe._viewPos, chpe._sortOrder);
//			}

			// WIDTH
			int colWidth = chpe._width; 
			if (colWidth > 0)
			{
				if (tcx != null)
				{
					tcx.setPreferredWidth(colWidth);
					tcx.setWidth(colWidth);
				}
			}

			// Initially set all columns to UNSORTED
			// setting the order will be done later
			if (colViewPos >= 0) 
			{
				if (getSortOrder(colViewPos) != SortOrder.UNSORTED)
					setSortOrder(colViewPos, SortOrder.UNSORTED);
			}
		}
		return fixCount;
	}

	/** Save column order/layout in the temporary properties file. */
	public void saveColumnLayout()
	{
		saveColumnLayout(false);
	}
	/** Save column order/layout in the temporary properties file. 
	 * @param toOriginalLayout if we want to save the original layout, which makes restore esier.
	 */
	public void saveColumnLayout(boolean toOriginalLayout)
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null) 
			return;

		if (getColumnCount() == 0)
			return;

		// Get prefix name to use
		String cmName = getName(); // Name of the JTable component, which should be the CM name
		if (StringUtil.isNullOrBlank(cmName))
		{
			_logger.debug("Can't load Column Layout, because the JTable has not been assigned a name. getName() on the JTable is null.");
			return;
		}

		long srvVersion = 0;
		if (getModel() instanceof CountersModel)
		{
			CountersModel cm = (CountersModel) getModel();
			if (cm.isRuntimeInitialized())
				srvVersion = cm.getServerVersion();
		}
		String confKeyBase    = cmName + ".gui.column.header.props.["+SwingUtils.getScreenResulutionAsString()+"]";
		String confKeyVersion = cmName + ".gui.column.header.props.["+SwingUtils.getScreenResulutionAsString()+"]." + srvVersion;
		String confVal = "";

		TableColumnModel tcm = getColumnModel();

		for (TableColumn tc : getColumns(true))
		{
			TableColumnExt tcx     = (TableColumnExt) tc;
			String         colName = tc.getHeaderValue().toString();

			// Visible
			boolean colIsVisible = tcx.isVisible();

			// View/model position
			int colModelPos = tcx.getModelIndex();
			int colViewPos  = -1;
			try {colViewPos = tcm.getColumnIndex(colName);}
			catch (IllegalArgumentException ignore) {}

			// Column width
			int colWidth = tc.getWidth();

			// Sorted
			SortOrder colSort    = getSortOrder(colName);
			int       colSortPos = getSortOrderIndex(colName);

//if (getName().equals("CMobjActivity"))
//System.out.println("saveColumnLayout() cm='"+cmName+"': colName="+StringUtil.left(colName,30)+", modelPos="+colModelPos+", viewPos="+colViewPos+", isVisible="+colIsVisible+", sort="+colSort+", identifier='"+tcx.getIdentifier()+"', toString="+tc);
			_logger.debug("saveColumnLayout() cm='"+cmName+"': colName="+StringUtil.left(colName,30)+", modelPos="+colModelPos+", viewPos="+colViewPos+", isVisible="+colIsVisible+", sort="+colSort+", sortPos="+colSortPos+", identifier='"+tcx.getIdentifier()+"', width="+colWidth+", toString="+tc);

			ColumnHeaderPropsEntry chpe = new ColumnHeaderPropsEntry(colName, colModelPos, colViewPos, colIsVisible, colSort, colSortPos, colWidth);
			if (toOriginalLayout)
			{
				chpe._viewPos      = colModelPos;
				chpe._isVisible    = true;
				chpe._sortOrder    = SortOrder.UNSORTED;
				chpe._sortOrderPos = 0;
				chpe._width        = -1;

				// If we got any preferred layout, lets override the MODEL values with the PREFERRED layout
				Map<String, ColumnHeaderPropsEntry> prefMap = getPreferredColumnLayout();
				if (prefMap != null && !prefMap.isEmpty() )
				{
					ColumnHeaderPropsEntry prefEntry = prefMap.get(colName);
					if (prefEntry != null)
					{
						chpe._viewPos      = prefEntry._viewPos >= 0 ? prefEntry._viewPos : colModelPos; // viewPos can't be less than zero; then use the model...
						chpe._isVisible    = prefEntry._isVisible;
						chpe._sortOrder    = prefEntry._sortOrder;
						chpe._sortOrderPos = prefEntry._sortOrderPos;
						chpe._width        = prefEntry._width;
					}
				}
			}

			// Append to the Config Value
			confVal += chpe+"; ";
		}
		confVal = confVal.substring(0, confVal.length()-2);
		_logger.debug("saveColumnLayout() SAVE PROPERTY: "+confKeyBase+"="+confVal);
		_logger.debug("saveColumnLayout() SAVE PROPERTY: "+confKeyVersion+"="+confVal);

		conf.setProperty(confKeyBase,    confVal);
		conf.setProperty(confKeyVersion, confVal);
		conf.save();
	}
	
	/** 
	 * restore original column layout, the original layout is the same as the order from the model 
	 */
	public void setOriginalColumnLayout()
	{
		saveColumnLayout(true);
		loadColumnLayout();
	}

	/**
	 * Get the sort index for a specific column.
	 * @param colModelIndex
	 * @return -1 if the column is not sorted, else it would be a number greater than 0.
	 */
	public int getSortOrderIndex(int colModelIndex)
	{
		List<? extends SortKey> sortKeys = this.getRowSorter().getSortKeys();
		if ( sortKeys == null || sortKeys.size() == 0 )
			return -1;

		int sortIndex = 1;
		for (SortKey sortKey : sortKeys)
		{
			if (sortKey.getSortOrder() == SortOrder.UNSORTED)
				continue;

			if ( sortKey.getColumn() == colModelIndex )
				return sortIndex;

			sortIndex++;
		}

		return -1;
	}

	/**
	 * Get the sort index for a specific column.
	 * @param colName
	 * @return -1 if the column is not sorted, else it would be a number greater than 0.
	 */
	public int getSortOrderIndex(String colName)
	{
		try
		{
			int colModelIndex = this.getColumn(colName).getModelIndex();
			return getSortOrderIndex(colModelIndex);
		}
		catch (IllegalArgumentException ignore)
		{
			return -1;
		}
	}

	/**
	 * To be able select/UN-SELECT rows in a table Called when a row/cell is
	 * about to change. getSelectedRow(), still shows what the *current*
	 * selection is
	 */
	@Override
	public void changeSelection(int row, int column, boolean toggle, boolean extend)
	{
		_logger.debug("changeSelection(row=" + row + ", column=" + column + ", toggle=" + toggle + ", extend=" + extend + "), getSelectedRow()=" + getSelectedRow() + ", getSelectedColumn()=" + getSelectedColumn());

		// if "row we clicked on" is equal to "currently selected row"
		// and also check that we do not do "left/right on keyboard"
		if ( row == getSelectedRow() && (column == getSelectedColumn() || getSelectedColumn() < 0) )
		{
			toggle = true;
			_logger.debug("changeSelection(): change toggle to " + toggle + ".");
		}

		super.changeSelection(row, column, toggle, extend);
	}

	/* Called on fire* has been called on the TableModel */
	@Override
	public void tableChanged(final TableModelEvent e)
	{
		if ( ! SwingUtilities.isEventDispatchThread() )
		{
//		    SwingUtilities.invokeLater(new Runnable() {
//		    	public void run() {
//		    		privateTableChanged(e);
//		    	}
//		    });
		    try
			{
				SwingUtilities.invokeAndWait(new Runnable() {
				    @Override
					public void run() {
				    	privateTableChanged(e);
				    }
				});
			}
			catch (InterruptedException e1)      { _logger.info("SwingUtilities.invokeAndWait(privateTableChanged), Caught: "+e1); }
			catch (InvocationTargetException e1) { _logger.info("SwingUtilities.invokeAndWait(privateTableChanged), threw exception: "+e1, e1); }
		}
		else
        	privateTableChanged(e);
			
	}
	private void privateTableChanged(TableModelEvent e)
	{
		// it looks like either JTable or JXTable looses the selected row
		// after "fireTableDataChanged" has been called...
		// So try to set it back to where it previously was!
		// NOTE: value is set in valueChanged(ListSelectionEvent e), when a rows is selected.
		try
		{
			_inPrivateTableChanged = true;

			// Is some cases during initialization of table it throws: java.lang.ArrayIndexOutOfBoundsException: 62 >= 60
			// if some columns are hidden in the JXTable, lets see what happens if we try to catch the error...
			try
			{
				super.tableChanged(e);
			}
			catch (Throwable t)
			{
				int mcols = getModel().getColumnCount();
				int vcols = getColumnCount();
				if (mcols > vcols)
				{
					// Lets do nothing here, since the there are probably *hidden* columns in the view
				}
				else
				{
					_logger.info("GTable='"+getName()+"', Problems when calling super.tableChanged(e). (enable debug mode to see stacktrace) Caught: "+t); // no stacktrace to log, just info message...
					_logger.debug("GTable='"+getName()+"', Problems when calling super.tableChanged(e). Caught: "+t, t);
				}
			}

			// restoring current selected row by PK is sometimes a problem... 
			// so catch it and log it with the CM Name this so it's easier to debug
			try
			{
				// restore current selected row by PK
				// _currentSelectedModelPk is maintained in valueChanged(ListSelectionEvent e)
				if (_lastSelectedModelPk != null)
				{
					TableModel tm = getModel();
					if (tm instanceof CountersModel)
					{
						CounterTableModel ctm = ((CountersModel)tm).getCounterData();
						if (ctm != null)
						{
							int modelPkRow = ctm.getRowNumberForPkValue(_lastSelectedModelPk);
							if (modelPkRow >= 0)
							{
								int viewRow = -1;
								try {viewRow = convertRowIndexToView(modelPkRow);} catch(Throwable t) {/*ignore*/}
								if ( viewRow >= 0  && viewRow < getRowCount() )
									getSelectionModel().setSelectionInterval(viewRow, viewRow);
							}
						}
					}
				}
				else
				{
					// try use previous selected row, which we remembered at the start 
					if ( _lastSelectedModelRow >= 0 )
					{
						// If no rows in model, no need to restore selected row.
						if (getRowCount() > 0 && _lastSelectedModelRow < getRowCount())
						{
							int viewRowNow = convertRowIndexToView(_lastSelectedModelRow);
							if ( viewRowNow >= 0 && viewRowNow < getRowCount() )
								getSelectionModel().setSelectionInterval(viewRowNow, viewRowNow);
						}
					}
				}
			}
			catch (Throwable t)
			{
				_logger.warn("GTable='"+getName()+"', Problems when restoring selected row. Caught: "+t, t);
			}
			
	
			// event: AbstactTableModel.fireTableStructureChanged
			if ( SwingUtils.isStructureChanged(e) )
			{
				_tableStructureChangedFlag = true;
				loadColumnLayout();
			}
		}
		finally
		{
			_inPrivateTableChanged = false;			
		}
	}

	private boolean _inPrivateTableChanged = false; // true when inside method privateTableChanged
	private String  _lastSelectedModelPk   = null;  // remember last selected row PK (only if TableModel support this)
	private int     _lastSelectedModelRow  = -1;    // remember last selected row (used as a fall back if PK, can't be used)

	/** implements ListSelectionListener, remember last selected row (by Primary Key) */
	@Override
    public void valueChanged(ListSelectionEvent e) 
	{
		// Call super to do all it's dirty work.
		super.valueChanged(e);

		if (_inPrivateTableChanged)
			return;

		// If we are still moving the mouse
		if (e.getValueIsAdjusting()) 
            return;

		// Reset
		_lastSelectedModelPk  = null;
		_lastSelectedModelRow = -1;

		// no rows, get out of here
		if (getRowCount() <= 0 || getColumnCount() <= 0) 
			return;

		// Get selected row
		int viewRow = getSelectedRow();
		if ( viewRow >= 0 )
			_lastSelectedModelRow = convertRowIndexToModel(viewRow);

		// Save current selected PK
		if (_lastSelectedModelRow >= 0)
		{
			TableModel tm = getModel();
			if (tm != null && tm instanceof CountersModel)
			{
				CountersModel cm = (CountersModel)tm;
				CounterTableModel ctm = cm.getCounterData();
				if (ctm != null)
					_lastSelectedModelPk = ctm.getPkValue(_lastSelectedModelRow);
			}
		}
    }

	// public TableCellRenderer getCellRenderer(int row, int column)
	// {
	// return _tableDiffDataCellRenderer;
	// TableCellRenderer renderer = super.getCellRenderer(row, column);
	// if (_cm != null )
	// {
	// if (_cm.showAbsolute())
	// return renderer;
	//
	// if (_cm.isDeltaCalculatedColumn(column))
	// {
	// return _tableDiffDataCellRenderer;
	// }
	// }
	// return renderer;
	// }

	// 
	// TOOL TIP for: TABLE HEADERS
	//
	@Override
	protected JTableHeader createDefaultTableHeader()
	{
		return new JXTableHeader(getColumnModel())
		{
			private static final long	serialVersionUID	= -4987530843165661043L;

			@Override
			public String getToolTipText(MouseEvent e)
			{
				// Now get the column name, which we point at
				Point p = e.getPoint();
				int index = getColumnModel().getColumnIndexAtX(p.x);
				if ( index < 0 )
					return null;
				Object colNameObj = getColumnModel().getColumn(index).getHeaderValue();

				// Now get the ToolTip from the CounterTableModel
				String toolTip = null;
				if ( colNameObj instanceof String )
				{
					String colName = (String) colNameObj;
					TableModel tm = getModel();
					if (tm instanceof ITableTooltip)
					{
						ITableTooltip tt = (ITableTooltip) tm;
						toolTip = tt.getToolTipTextOnTableColumnHeader(colName);
					}
//					if ( _cm != null )
//						toolTip = _cm.getToolTipTextOnTableColumn(colName);
				}
				return toolTip;
			}
		};
	}
	
	public String getToolTipTextForColumn(String colname)
	{
		String toolTip = "";
		
		TableModel tm = getModel();
		if (tm instanceof ITableTooltip)
		{
			ITableTooltip tt = (ITableTooltip) tm;
			toolTip = tt.getToolTipTextOnTableColumnHeader(colname);
		}
		return toolTip; 
	}

	// 
	// TOOL TIP for: CELLS
	//
	@Override
	public String getToolTipText(MouseEvent e)
	{
		String tip = null;
		Point p = e.getPoint();
		int row = rowAtPoint(p);
		int col = columnAtPoint(p);
		if ( row >= 0 && col >= 0 )
		{
			col = super.convertColumnIndexToModel(col);
			row = super.convertRowIndexToModel(row);

			TableModel model = getModel();
			String colName = model.getColumnName(col);
			Object cellValue = model.getValueAt(row, col);

			if ( model instanceof ITableTooltip )
			{
				ITableTooltip tt = (ITableTooltip) model;
				tip = tt.getToolTipTextOnTableCell(e, colName, cellValue, row, col);
				// Do we want to use "focusable" tips?
				if (tip != null) 
				{
					boolean normalTooltip = false;
					if (tip.startsWith(TOOLTIP_TYPE_NORMAL))
					{
						normalTooltip = true;
						tip = tip.substring(TOOLTIP_TYPE_NORMAL.length());
					}
					if (tip.startsWith(TOOLTIP_TYPE_FOCUSABLE))
					{
						normalTooltip = false;
						tip = tip.substring(TOOLTIP_TYPE_FOCUSABLE.length());
					}
					
					// If it's a SHORT tip, display the "normal" tooltip, else use "focusable" tooltip
					if (normalTooltip)
						return tip;

					if (_focusableTip == null) 
					{
						// Try to get a "Tool Tip Resolver" from the CounterModels, ToolsTip Supplier
						ToolTipHyperlinkResolver resolver = null;
						if (model instanceof CountersModel)
						{
							ITableTooltip ttSupplier = ((CountersModel)model).getToolTipSupplier();
							if (ttSupplier != null && ttSupplier instanceof ToolTipHyperlinkResolver)
								resolver = (ToolTipHyperlinkResolver) ttSupplier;
						}

						_focusableTip = new FocusableTip(this, null, resolver);
					}

//						_focusableTip.setImageBase(imageBase);
					_focusableTip.toolTipRequested(e, tip);
				}
				// No tooltip text at new location - hide tip window if one is
				// currently visible
				else if (_focusableTip!=null) 
				{
					_focusableTip.possiblyDisposeOfTipWindow();
				}
				return null;
			}
		}
//		if ( tip != null )
//			return tip;
		return getToolTipText();
	}
	
	// // TableCellRenderer _tableDiffDataCellRenderer = new
	// DefaultTableCellRenderer()
	// TableCellRenderer _tableDiffDataCellRenderer = new
	// DefaultTableRenderer()
	// {
	// private static final long serialVersionUID = -4439199147374261543L;
	//
	// public Component getTableCellRendererComponent(JTable table, Object
	// value, boolean isSelected, boolean hasFocus, int row, int column)
	// {
	// Component comp = super.getTableCellRendererComponent(table, value,
	// isSelected, hasFocus, row, column);
	// // if (value == null || _cm == null)
	// // return comp;
	// // if (value == null)
	// // return comp;
	//
	// // ((JLabel)comp).setHorizontalAlignment(RIGHT);
	// // if ( _cm.isPctColumn(column) )
	// // {
	// // comp.setForeground(Color.red);
	// // }
	// // else
	// // {
	// // comp.setForeground(Color.blue);
	// // if ( value instanceof Number )
	// // {
	// // if ( ((Number)value).doubleValue() != 0.0 )
	// // {
	// // comp.setFont( comp.getFont().deriveFont(Font.BOLD) );
	// // }
	// // }
	// // }
	// // return comp;
	// if ( value instanceof Number )
	// {
	// comp.setForeground(Color.blue);
	// // ((JLabel)comp).setHorizontalAlignment(RIGHT);
	// if ( ((Number)value).doubleValue() != 0.0 )
	// {
	// comp.setFont( comp.getFont().deriveFont(Font.BOLD) );
	// }
	// }
	// return comp;
	// }
	// };

	/**
	 * Get a list of column names as it's stored in the model
	 * @return
	 */
	public List<String> getOriginColumnOrderStrList()
	{
		List<String> list = new ArrayList<String>();

		TableModel tm = getModel();
		int modelCols = tm.getColumnCount();
		for (int c=0; c<modelCols; c++)
			list.add(tm.getColumnName(c));

		return list;
	}

	/**
	 * Get a list of column names as it's sorted in the view, invisible columns will be added at the end
	 * @return
	 */
	public List<String> getCurrentColumnOrderStrList()
	{
		List<String> list = new ArrayList<String>();

		// Get visible columns in order they are visible
		for (TableColumn tc : getColumns())
		{
			String  colName      = tc.getHeaderValue() + "";

			list.add(colName);
		}

		// Get hidden columns and add them at the end
		for (TableColumn tc : getColumns(true))
		{
			final TableColumnExt tcx = (TableColumnExt) tc;

			String  colName      = tcx.getHeaderValue() + "";
			boolean colIsVisible = tcx.isVisible();

			if (colIsVisible)
				continue;

			list.add(colName);
		}
		
		return list;
	}

	public void setVisibleColumns(String[] columns)
	{
		if (columns == null)
			return;
		
		setVisibleColumns(Arrays.asList(columns));
	}

	/**
	 * What columns should be visible when showing data 
	 * @param columns
	 */
	public void setVisibleColumns(List<String> columns)
	{
		if (columns == null)
			return;
		
		if (getModel() == null)
			return;

		TableColumnModelExt tcmx = (TableColumnModelExt)getColumnModel();
		for (TableColumn tc : tcmx.getColumns(true))
		{
			if (tc instanceof TableColumnExt)
			{
				TableColumnExt tcx = (TableColumnExt) tc;
				String colName = tcx.getTitle();
				
				boolean visible = columns.contains(colName);
//				System.out.println("setVisibleColumns: colName='"+colName+"', setVisible("+visible+")");
				tcx.setVisible(visible);
			}
			else
			{
				int mpos = tc.getModelIndex();
				String mColName = getModel().getColumnName(mpos);

				System.out.println("setVisibleColumns(List<String> columns): not instance of 'TableColumnExt'. mpos="+mpos+", mColName='"+mColName+"'.");
			}
		}
	}
	
	/**
	 * Get a list of columns that are visible
	 * @return
	 */
	public List<String> getVisibleColumns()
	{
		List<String> columns = new ArrayList<>();

		TableColumnModelExt tcmx = (TableColumnModelExt)getColumnModel();
		for (TableColumn tc : tcmx.getColumns(true))
		{
			if (tc instanceof TableColumnExt)
			{
				TableColumnExt tcx = (TableColumnExt) tc;
				String colName = tcx.getTitle();
				
				if (tcx.isVisible())
					columns.add(colName);
			}
			else
			{
				int mpos = tc.getModelIndex();
				String mColName = getModel().getColumnName(mpos);

				System.out.println("getVisibleColumns(): not instance of 'TableColumnExt'. mpos="+mpos+", mColName='"+mColName+"'.");
			}
		}

		return columns;
	}

	//------------------------------------------------------------
	//-- BEGIN: getValueAsXXXXX using column name
	//          more methods will be added as they are needed
	//------------------------------------------------------------
	public String getValueAsString(int vrow, String colName)
	{
		return getValueAsString(vrow, colName, true);
	}
	public String getValueAsString(int vrow, String colName, boolean caseSensitive)
	{
		Object o = getValueAsObject(vrow, colName, caseSensitive);

		if (o == null)
			return null;

		return o.toString();
	}

	public Short getValueAsShort(int vrow, String colName)
	{
		return getValueAsShort(vrow, colName, true);
	}
	public Short getValueAsShort(int vrow, String colName, boolean caseSensitive)
	{
		Object o = getValueAsObject(vrow, colName, caseSensitive);

		if (o == null)
			return null;

		if (o instanceof Number)
			return ((Number)o).shortValue();

		try
		{
			return Short.parseShort(o.toString());
		}
		catch(NumberFormatException e)
		{
			_logger.warn("Problem reading Short value for vrow="+vrow+", column='"+colName+"', tableName='"+getName()+"', returning null. Caught: "+e);
			return null;
		}
	}

	public Integer getValueAsInteger(int vrow, String colName)
	{
		return getValueAsInteger(vrow, colName, true);
	}
	public Integer getValueAsInteger(int vrow, String colName, boolean caseSensitive)
	{
		Object o = getValueAsObject(vrow, colName, caseSensitive);

		if (o == null)
			return null;

		if (o instanceof Number)
			return ((Number)o).intValue();

		try
		{
			return Integer.parseInt(o.toString());
		}
		catch(NumberFormatException e)
		{
			_logger.warn("Problem reading Integer value for vrow="+vrow+", column='"+colName+"', tableName='"+getName()+"', returning null. Caught: "+e);
			return null;
		}
	}

	public Long getValueAsLong(int vrow, String colName)
	{
		return getValueAsLong(vrow, colName, true);
	}
	public Long getValueAsLong(int vrow, String colName, boolean caseSensitive)
	{
		Object o = getValueAsObject(vrow, colName, caseSensitive);

		if (o == null)
			return null;

		if (o instanceof Number)
			return ((Number)o).longValue();

		try
		{
			return Long.parseLong(o.toString());
		}
		catch(NumberFormatException e)
		{
			_logger.warn("Problem reading Long value for vrow="+vrow+", column='"+colName+"', tableName='"+getName()+"', returning null. Caught: "+e);
			return null;
		}
	}

	public Timestamp getValueAsTimestamp(int vrow, String colName)
	{
		return getValueAsTimestamp(vrow, colName, true);
	}
	public Timestamp getValueAsTimestamp(int vrow, String colName, boolean caseSensitive)
	{
		Object o = getValueAsObject(vrow, colName, caseSensitive);

		if (o == null)
			return null;

		if (o instanceof Timestamp)
			return ((Timestamp)o);

		try
		{
			SimpleDateFormat sdf = new SimpleDateFormat();
			java.util.Date date = sdf.parse(o.toString());
			return new Timestamp(date.getTime());
		}
		catch(ParseException e)
		{
			_logger.warn("Problem reading Timestamp value for vrow="+vrow+", column='"+colName+"', tableName='"+getName()+"', returning null. Caught: "+e);
			return null;
		}
	}

	public BigDecimal getValueAsBigDecimal(int vrow, String colName)
	{
		return getValueAsBigDecimal(vrow, colName, true);
	}
	public BigDecimal getValueAsBigDecimal(int vrow, String colName, boolean caseSensitive)
	{
		Object o = getValueAsObject(vrow, colName, caseSensitive);

		if (o == null)
			return null;

		if (o instanceof BigDecimal)
			return ((BigDecimal)o);

		try
		{
			return new BigDecimal(o.toString());
		}
		catch(NumberFormatException e)
		{
			_logger.warn("Problem reading BigDecimal value for vrow="+vrow+", column='"+colName+"', tableName='"+getName()+"', returning null. Caught: "+e);
			return null;
		}
	}

	public Object getValueAsObject(int vrow, String colName)
	{
		return getValueAsObject(vrow, colName, true);
	}
	public Object getValueAsObject(int vrow, String colName, boolean caseSensitive)
	{
//		int col_pos = findViewColumn(colName, caseSensitive);
//		if (col_pos < 0)
//			throw new RuntimeException("Can't find column '"+colName+"' in JTable named '"+getName()+"'.");

		TableModel tm = getModel();
		int mrow = convertRowIndexToModel(vrow);
//		int mcol = convertColumnIndexToModel(col_pos);
		int mcol = -1;

		// get column pos from the model, if it's hidden in the JXTable
		for (int c=0; c<tm.getColumnCount(); c++) 
		{
			if ( caseSensitive ? colName.equals(tm.getColumnName(c)) : colName.equalsIgnoreCase(tm.getColumnName(c)) ) 
			{
				mcol = c;
				break;
			}
		}
		if (mcol < 0)
			throw new RuntimeException("Can't find column '"+colName+"' in JTable named '"+getName()+"'.");
		
//System.out.println("getValueAsObject(vrow="+vrow+", colName='"+colName+"'): col_pos="+col_pos+", mrow="+mrow+", mcol="+mcol+".");
		Object o = tm.getValueAt(mrow, mcol);

		if (tm instanceof ResultSetTableModel)
		{
			if (o != null && o instanceof String)
			{
				if (NULL_REPLACE.equals(o))
					return null;
			}
		}
		return o;
	}
	//------------------------------------------------------------
	//-- END: getValueAsXXXXX using column name
	//------------------------------------------------------------

	
	//------------------------------------------------------------
	//-- BEGIN: getSelectedValuesAsXXXX using column name
	//          more methods will be added as they are needed
	//------------------------------------------------------------
	public String getSelectedValuesAsString(String colName)
	{
		return getSelectedValuesAsString(colName, true);
	}
	public String getSelectedValuesAsString(String colName, boolean caseSensitive)
	{
		Object o = getSelectedValuesAsObject(colName, caseSensitive);

		if (o == null)
			return null;

		return o.toString();
	}

	public Short getSelectedValuesAsShort(String colName)
	{
		return getSelectedValuesAsShort(colName, true);
	}
	public Short getSelectedValuesAsShort(String colName, boolean caseSensitive)
	{
		Object o = getSelectedValuesAsObject(colName, caseSensitive);

		if (o == null)
			return null;

		if (o instanceof Number)
			return ((Number)o).shortValue();

		try
		{
			return Short.parseShort(o.toString());
		}
		catch(NumberFormatException e)
		{
			_logger.warn("Problem reading Short value for vrow="+getSelectedRow()+", column='"+colName+"', tableName='"+getName()+"', returning null. Caught: "+e);
			return null;
		}
	}

	public Integer getSelectedValuesAsInteger(String colName)
	{
		return getSelectedValuesAsInteger(colName, true);
	}
	public Integer getSelectedValuesAsInteger(String colName, boolean caseSensitive)
	{
		Object o = getSelectedValuesAsObject(colName, caseSensitive);

		if (o == null)
			return null;

		if (o instanceof Number)
			return ((Number)o).intValue();

		try
		{
			return Integer.parseInt(o.toString());
		}
		catch(NumberFormatException e)
		{
			_logger.warn("Problem reading Integer value for vrow="+getSelectedRow()+", column='"+colName+"', tableName='"+getName()+"', returning null. Caught: "+e);
			return null;
		}
	}

	public Long getSelectedValuesAsLong(String colName)
	{
		return getSelectedValuesAsLong(colName, true);
	}
	public Long getSelectedValuesAsLong(String colName, boolean caseSensitive)
	{
		Object o = getSelectedValuesAsObject(colName, caseSensitive);

		if (o == null)
			return null;

		if (o instanceof Number)
			return ((Number)o).longValue();

		try
		{
			return Long.parseLong(o.toString());
		}
		catch(NumberFormatException e)
		{
			_logger.warn("Problem reading Long value for vrow="+getSelectedRow()+", column='"+colName+"', tableName='"+getName()+"', returning null. Caught: "+e);
			return null;
		}
	}

	public Timestamp getSelectedValuesAsTimestamp(String colName)
	{
		return getSelectedValuesAsTimestamp(colName, true);
	}
	public Timestamp getSelectedValuesAsTimestamp(String colName, boolean caseSensitive)
	{
		Object o = getSelectedValuesAsObject(colName, caseSensitive);

		if (o == null)
			return null;

		if (o instanceof Timestamp)
			return ((Timestamp)o);

		try
		{
			SimpleDateFormat sdf = new SimpleDateFormat();
			java.util.Date date = sdf.parse(o.toString());
			return new Timestamp(date.getTime());
		}
		catch(ParseException e)
		{
			_logger.warn("Problem reading Timestamp value for vrow="+getSelectedRow()+", column='"+colName+"', tableName='"+getName()+"', returning null. Caught: "+e);
			return null;
		}
	}

	public BigDecimal getSelectedValuesAsBigDecimal(String colName)
	{
		return getSelectedValuesAsBigDecimal(colName, true);
	}
	public BigDecimal getSelectedValuesAsBigDecimal(String colName, boolean caseSensitive)
	{
		Object o = getSelectedValuesAsObject(colName, caseSensitive);

		if (o == null)
			return null;

		if (o instanceof BigDecimal)
			return ((BigDecimal)o);

		try
		{
			return new BigDecimal(o.toString());
		}
		catch(NumberFormatException e)
		{
			_logger.warn("Problem reading BigDecimal value for vrow="+getSelectedRow()+", column='"+colName+"', tableName='"+getName()+"', returning null. Caught: "+e);
			return null;
		}
	}

	public Object getSelectedValuesAsObject(String colName)
	{
		return getSelectedValuesAsObject(colName, true);
	}
	public Object getSelectedValuesAsObject(String colName, boolean caseSensitive)
	{
		int vrow = getSelectedRow();
		if (vrow == -1)
			return null;

		TableModel tm = getModel();
		int mrow = convertRowIndexToModel(vrow);
		int mcol = -1;

		// get column pos from the model, if it's hidden in the JXTable
		for (int c=0; c<tm.getColumnCount(); c++) 
		{
			if ( caseSensitive ? colName.equals(tm.getColumnName(c)) : colName.equalsIgnoreCase(tm.getColumnName(c)) ) 
			{
				mcol = c;
				break;
			}
		}
		if (mcol < 0)
			throw new RuntimeException("Can't find column '"+colName+"' in JTable named '"+getName()+"'.");
		
		Object o = tm.getValueAt(mrow, mcol);

		if (tm instanceof ResultSetTableModel)
		{
			if (o != null && o instanceof String)
			{
				if (NULL_REPLACE.equals(o))
					return null;
			}
		}
		return o;
	}
	//------------------------------------------------------------
	//-- END: getSelectedValuesAsXXXX using column name
	//------------------------------------------------------------
	
	
	/*----------------------------------------------------
	 **---------------------------------------------------
	 **---------------------------------------------------
	 **---- SUBCLASSES ---- SUBCLASES ---- SUBCLASES ----- 
	 **---------------------------------------------------
	 **---------------------------------------------------
	 **---------------------------------------------------
	 */
	
	/**
	 * Get tooltip for a column header. 
	 */
	public interface ITableTooltip
	{
		/**
		 * Get tooltip for a specific Table Column
		 * @param colName
		 * @return the tooltip
		 */
		public String getToolTipTextOnTableColumnHeader(String colName);

		/**
		 * Used to get tool tip on a cell level.
		 * Override it to set specific tooltip... 
		 * 
		 * @param e
		 * @param colName
		 * @param modelRow
		 * @param modelCol
		 * @return
		 */
		public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol);
	}

	/**
	 * This timer is started when a column in the table has been moved/removed
	 * It will save the column order layout...
	 * A timer is needed because, when we move a column the method columnMoved() is kicked of
	 * for every pixel we move the mouse.
	 */
	private class ColumnLayoutTimerAction implements ActionListener
	{
		private GTable _tab = null;
		ColumnLayoutTimerAction(GTable tab)
		{
			_tab = tab;
		}
		@Override
		public void actionPerformed(ActionEvent actionevent)
		{
			_tab.saveColumnLayout();
			_tab._columnLayoutTimer.stop();
		}
	}



	/*---------------------------------------------------
	 ** BEGIN: Watermark stuff
	 **---------------------------------------------------
	 */
	private Watermark _watermark = null;

	public void setWatermarkText(String str)
	{
		if (_logger.isDebugEnabled())
			_logger.debug(getName() + ".setWatermarkText('" + str + "')");

		if (_watermark != null)
			_watermark.setWatermarkText(str);
	}

	public void setWatermarkAnchor(JComponent comp)
	{
		_watermark = new Watermark(comp, "");
	}

	private class Watermark extends AbstractComponentDecorator
	{
		public Watermark(JComponent target, String text)
		{
			super(target);
			if ( text == null )
				text = "";
			_textSave = text;
			_textBr   = text.split("\n");
		}

//		private String		_restartText	= "Note: Restart "+Version.getAppName()+" after you have enabled the configuration.";
//		private String		_restartText 	= "Note: Reconnect to ASE Server after you have enabled the configuration.";
		private String		_restartText1	= "Note: use Menu -> Tools -> Configure ASE for Monitoring: to reconfigure ASE.";
		private String		_restartText2	= "    or: Reconnect to ASE after you have enabled the configuration using isql.";
		private String[]	_textBr			= null; // Break Lines by '\n'
		private String      _textSave       = null; // Save last text so we don't need to do repaint if no changes.
		private Graphics2D	g				= null;
		private Rectangle	r				= null;

		@Override
		public void paint(Graphics graphics)
		{
			if ( _textBr == null || _textBr != null && _textBr.length < 0 )
				return;

			r = getDecorationBounds();
			g = (Graphics2D) graphics;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Font f = g.getFont();
//			g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 2.0f));
			g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 2.0f * SwingUtils.getHiDpiScale() ));
			g.setColor(new Color(128, 128, 128, 128));

			FontMetrics fm = g.getFontMetrics();
			int maxStrWidth = 0;
			int maxStrHeight = fm.getHeight();

			// get max with for all of the lines
			for (int i = 0; i < _textBr.length; i++)
			{
				int CurLineStrWidth = fm.stringWidth(_textBr[i]);
				maxStrWidth = Math.max(maxStrWidth, CurLineStrWidth);
			}
			int xPos = (r.width - maxStrWidth) / 2;
			int yPos = (int) (r.height - ((r.height - fm.getHeight()) / 2) * 1.3);

			int spConfigureCount = 0;

			// Print all the lines
			for (int i = 0; i < _textBr.length; i++)
			{
				g.drawString(_textBr[i], xPos, (yPos + (maxStrHeight * i)));

				if ( _textBr[i].startsWith("sp_configure") )
					spConfigureCount++;
			}

			if ( spConfigureCount > 0 )
			{
				int yPosRestartText = yPos + (maxStrHeight * (_textBr.length + 1));
				g.drawString(_restartText1, xPos, yPosRestartText);
				g.drawString(_restartText2, xPos, yPosRestartText + 25);
			}
		}

		public void setWatermarkText(String text)
		{
			if ( text == null )
				text = "";

			// If text has NOT changed, no need to continue
			if (text.equals(_textSave))
				return;

			_textSave = text;

			_textBr = text.split("\n");
			_logger.debug("setWatermarkText: to '" + text + "'.");

			repaint();
		}
	}

	/*---------------------------------------------------
	 ** END: Watermark stuff
	 **---------------------------------------------------
	 */

}

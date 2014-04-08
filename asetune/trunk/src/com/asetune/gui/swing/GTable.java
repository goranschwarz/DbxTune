package com.asetune.gui.swing;

import java.awt.Color;
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
import java.beans.PropertyChangeEvent;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.event.TableColumnModelExtListener;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.StringValue;
import org.jdesktop.swingx.renderer.StringValues;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.table.TableColumnModelExt;

import com.asetune.cm.CounterTableModel;
import com.asetune.cm.CountersModel;
import com.asetune.gui.focusabletip.FocusableTip;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

public class GTable 
extends JXTable
{
	private static Logger       _logger = Logger.getLogger(GTable.class);

	private static final long	serialVersionUID			= 1L;
	private int					_lastMousePressedAtModelCol	= -1;
	private int					_lastMousePressedAtModelRow	= -1;
//	private GTable              _thisTable                  = null;
//	private boolean             _hasNewModel                = true;
	private boolean             _tableStructureChangedFlag  = true;

	private FocusableTip        _focusableTip;

	/** If columns are reordered, save it after X seconds inactivity */
	protected Timer             _columnLayoutTimer          = null;

	public GTable()
	{
		init();
	}

	public GTable(TableModel tm)
	{
		super(tm);
		init();
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
		@SuppressWarnings("serial")
		StringValue sv = new StringValue() 
		{
			NumberFormat nf = null;
			{ // init/constructor section
				try
				{
					nf = new DecimalFormat();
					nf.setMinimumFractionDigits(1);
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

		// Set special Render to print multiple columns sorts
		getTableHeader().setDefaultRenderer(new MultiSortTableCellHeaderRenderer());

		// Set columnHeader popup menu
		getTableHeader().setComponentPopupMenu(createTableHeaderPopupMenu());

		//--------------------------------------------------------------------
		// New SORTER that toggles from DESCENDING -> ASCENDING -> UNSORTED
		//--------------------------------------------------------------------
		setSortOrderCycle(SortOrder.DESCENDING, SortOrder.ASCENDING, SortOrder.UNSORTED);
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
					mi = new JMenuItem("Adjust Column Width"); // Resizes all columns to fit their content
					p.add(mi);
					mi.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							GTable.this.packAll();
						}
					});

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
	 * @return -1: If the column name is doesn't exists in the view (could be hidden) nor in the Model
	 */
	public int findViewColumn(String colName)
	{
		int viewColPos  = -1;
		int modelColPos = -1;

		// Get the model position
		TableModel tm = getModel();
		if (tm instanceof AbstractTableModel)
		{
			modelColPos = ((AbstractTableModel)tm).findColumn(colName);
		}
		else
		{
			for (int c=0; c<tm.getColumnCount(); c++) 
			{
				if (colName.equals(tm.getColumnName(c))) 
				{
					modelColPos = c;
					break;
				}
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

		int srvVersion = 0;
		if (getModel() instanceof CountersModel)
		{
			CountersModel cm = (CountersModel) getModel();
			if (cm.isRuntimeInitialized())
				srvVersion = cm.getServerVersion();
		}
		// get the values from configuration
		String confKey = cmName + ".gui.column.header.props." + srvVersion;
		String confVal = conf.getProperty(confKey);
		if (confVal == null)
		{
			// Revert back to "previous" version
			confKey = cmName + ".gui.column.header.props";
			confVal = conf.getProperty(confKey);
		}
		if (confVal == null)
			return;

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

		// If cable model and config are "out of sync", do not load
		if (colProps.size() != getModel().getColumnCount())
		{
			_logger.info(confKey + " has '"+colProps.size()+"' values and the table model has '"+getModel().getColumnCount()+"' columns. I will skip moving columns around, the original column layout will be used.");
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
	protected int loadColumnLayout(Map<String, ColumnHeaderPropsEntry> colProps)
	{
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
					tcmx.moveColumn(colViewPos, propViewPos);
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

		int srvVersion = 0;
		if (getModel() instanceof CountersModel)
		{
			CountersModel cm = (CountersModel) getModel();
			if (cm.isRuntimeInitialized())
				srvVersion = cm.getServerVersion();
		}
		String confKeyBase    = cmName + ".gui.column.header.props";
		String confKeyVersion = cmName + ".gui.column.header.props." + srvVersion;
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
				_logger.warn("GTable='"+getName()+"', Problems when calling super.tableChanged(e). Caught: "+t, t);
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
								int viewRow = convertRowIndexToView(modelPkRow);
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
					if (_focusableTip == null) 
						_focusableTip = new FocusableTip(this);

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
			g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 2.0f));
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

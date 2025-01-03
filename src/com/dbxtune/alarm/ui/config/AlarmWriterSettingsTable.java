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
package com.dbxtune.alarm.ui.config;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.table.TableColumnExt;

import com.dbxtune.alarm.writers.AlarmWriterToMail;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.graph.TrendGraphColors;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

/** Extend the JXTable */
public class AlarmWriterSettingsTable extends JXTable
{
	private static final long serialVersionUID = 0L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	//	protected int           _lastTableHeaderPointX = -1;
	protected int           _lastTableHeaderColumn = -1;
	private   JPopupMenu    _popupMenu             = null;
	private   JPopupMenu    _headerPopupMenu       = null;

//	AlarmWriterSettingsPanel _alarmWriterSettingsPanel;
	AlarmWriterSettingsTableModel   _alarmWriterSettingsTableModel;
	
	String _currentCmName = "";
	int    _currentTmRow  = -1;

	String _currentWriterClassName = "";

	public AlarmWriterSettingsTable(AlarmWriterSettingsTableModel alarmWriterSettingsTableModel)
//	public AlarmWriterSettingsTable(AlarmWritersTableModel alarmWritersTableModel, AlarmWriterSettingsPanel alarmWriterSettingsPanel)
	{
		super();
		
		_alarmWriterSettingsTableModel   = alarmWriterSettingsTableModel;

		setModel(_alarmWriterSettingsTableModel);

		setShowGrid(false);
		setSortable(true);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		packAll(); // set size so that all content in all cells are visible
		setColumnControlVisible(true);
		setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
//		setHighlighters(_highliters);

		// Create some PopupMenus and attach them
		_popupMenu = createDataTablePopupMenu();
		setComponentPopupMenu(getDataTablePopupMenu());

		_headerPopupMenu = createDataTableHeaderPopupMenu();
		getTableHeader().setComponentPopupMenu(getDataTableHeaderPopupMenu());

		
		// COLOR CODE SOME ROWS/CELLS
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// MANDATORY
		if (conf != null) colorStr = conf.getProperty(getName()+".color.mandatory");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (AlarmWriterSettingsTableModel.TAB_POS_VALUE == convertColumnIndexToModel(adapter.column))
				{
					CmSettingsHelper awse = _alarmWriterSettingsTableModel.getSettingForRow(convertRowIndexToModel(adapter.row));
					if ( awse.isMandatory() && ! awse.hasValidValue() )
						return true;
				}
				return false;
			}
		}, SwingUtils.parseColor(colorStr, TrendGraphColors.LIGHT_RED), null));

		// PROBABLY
		if (conf != null) colorStr = conf.getProperty(getName()+".color.probably");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (AlarmWriterSettingsTableModel.TAB_POS_VALUE == convertColumnIndexToModel(adapter.column))
				{
					CmSettingsHelper awse = _alarmWriterSettingsTableModel.getSettingForRow(convertRowIndexToModel(adapter.row));
					if ( awse.isProbable() && ! awse.hasValidValue() )
						return true;
				}
				return false;
			}
		}, SwingUtils.parseColor(colorStr, TrendGraphColors.VERY_LIGHT_YELLOW), null));

		// NON DEFAULT
		if (conf != null) colorStr = conf.getProperty(getName()+".color.isNotDefaultValue");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (AlarmWriterSettingsTableModel.TAB_POS_IS_DEFAULT == convertColumnIndexToModel(adapter.column))
				{
					CmSettingsHelper awse = _alarmWriterSettingsTableModel.getSettingForRow(convertRowIndexToModel(adapter.row));
					if ( ! awse.isDefaultValue() )
						return true;
				}
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.LIGHT_GRAY), null));

		// TEMPLATE VALUE
		if (conf != null) colorStr = conf.getProperty(getName()+".color.isNotDefaultValue");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (AlarmWriterSettingsTableModel.TAB_POS_PROPERTY_KEY == convertColumnIndexToModel(adapter.column))
				{
					CmSettingsHelper awse = _alarmWriterSettingsTableModel.getSettingForRow(convertRowIndexToModel(adapter.row));
//					if ( awse.getPropName().toLowerCase().indexOf("template") != -1)
					if ( awse.getPropName().toLowerCase().endsWith("template"))
						return true;
				}
				return false;
			}
		}, null, SwingUtils.parseColor(colorStr, TrendGraphColors.VERY_DARK_GREEN)));
	}


	/** What table header was the last header we visited */
	public int getLastTableHeaderColumn()
	{
		return _lastTableHeaderColumn;
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

				tip = _alarmWriterSettingsTableModel.getToolTipText(mcol);

				if (tip == null)
					return null;
				return "<html>" + tip + "</html>";
			}
		};

		// Track where we are in the TableHeader, this is used by the Popup menus
		// to decide what column of the TableHeader we are currently located on.
		tabHeader.addMouseMotionListener(new MouseMotionListener()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				_lastTableHeaderColumn = getColumnModel().getColumnIndexAtX(e.getX());
				if (_lastTableHeaderColumn >= 0)
					_lastTableHeaderColumn = convertColumnIndexToModel(_lastTableHeaderColumn);
			}
			@Override
			public void mouseDragged(MouseEvent e) {/*ignore*/}
		});

		return tabHeader;
	}

	/** CELL tool tip */
	@Override
	public String getToolTipText(MouseEvent e)
	{
		String tip = null;
		Point p = e.getPoint();
		int vrow = rowAtPoint(p);
		int vcol = columnAtPoint(p);
		if (vrow == -1 || vcol == -1)
			return null;
		
		// Translate View row to Model row
		int mrow = super.convertRowIndexToModel(    vrow );
		int mcol = super.convertColumnIndexToModel( vcol );

		// is Boolean and isEditable then show the below tooltip
		TableModel tm = getModel();
		if (tm.getColumnClass(mcol).equals(Boolean.class))
		{
			if (tm.isCellEditable(mrow, mcol))
				tip = "Right click on the header column to mark or unmark all rows.";
			else
				tip = "Sorry you can't change this value. (it's a <i>static</i> field)";
		}
		else
		{
			int description_pos = SwingUtils.findColumn(tm, "Description");
			int value_pos       = SwingUtils.findColumn(tm, "Value");
			if (description_pos != -1 && value_pos != -1)
			{
				String desc = tm.getValueAt(mrow, description_pos) + "";
				String val  = tm.getValueAt(mrow, value_pos      ) + "";

				if (StringUtils.startsWithIgnoreCase(val, "<html>"))
					val = StringUtil.toHtmlString(val);

				// If the desc is a HTML string, remove start/end tags.. which will be added later
				if (StringUtils.startsWithIgnoreCase(desc, "<html>"))
				{
					desc = StringUtils.removeStartIgnoreCase(desc, "<html>");
					desc = StringUtils.removeEndIgnoreCase(desc, "</html>");
				}
				return "<html>"
						+ "<b>Description: </b><br>"
						+ desc
						+ "<br>"
						+ "<br>"
						+ "<b>Value: </b>"
						+ "<pre>"
						+ val
						+ "</pre>"
						+ "</html>"
						;
			}
		}

		if (tip == null)
			return null;
		return "<html>" + tip + "</html>";
	}


//	public void refreshTable(List<AlarmWriterSettingsEntry> settings)
//	{
//		_alarmWriterSettingsTableModel.refreshTable(settings);
//		packAll(); // set size so that all content in all cells are visible
//	}
	public void refreshTable(List<CmSettingsHelper> settings, String writerClassName)
	{
		_currentWriterClassName = writerClassName;

		_alarmWriterSettingsTableModel.refreshTable(settings);
		packAll(); // set size so that all content in all cells are visible
		
		
		// Set the width to ... if it's to wide
		Runnable doLater = new Runnable()
		{
			@Override
			public void run()
			{
				int maxWidth = 400;

				// Set "template" columns to max a max width...
				int colPos = convertColumnIndexToView(AlarmWriterSettingsTableModel.TAB_POS_VALUE);
				if (colPos >= 0)
				{
					TableColumnExt tce = getColumnExt(colPos);
					if (tce.getWidth() > maxWidth)
						tce.setPreferredWidth(maxWidth);
				}

				// Set "template" columns to max a max width...
				colPos = convertColumnIndexToView(AlarmWriterSettingsTableModel.TAB_POS_DEFAULT);
				if (colPos >= 0)
				{
					TableColumnExt tce = getColumnExt(colPos);
					if (tce.getWidth() > maxWidth)
						tce.setPreferredWidth(maxWidth);
				}
			}
		};
		SwingUtilities.invokeLater(doLater);
		
	}

	private Configuration getCurrentConfig()
	{
		Configuration conf = new Configuration();
		
		for (CmSettingsHelper sh : _alarmWriterSettingsTableModel.getSettings())
		{
			conf.setProperty(sh.getPropName(), sh.getStringValue());
		}
		
		return conf;
	}
	
	/*---------------------------------------------------
	** BEGIN: PopupMenu on the table
	**---------------------------------------------------
	*/
	/** Get the JMeny attached to the GTabbedPane */
	public JPopupMenu getDataTablePopupMenu()
	{
		return _popupMenu;
	}

	/**
	 * Creates the JMenu on the Component, this can be overrided by a subclass.<p>
	 * If you want to add stuff to the menu, its better to use
	 * getTabPopupMenu(), then add entries to the menu. This is much
	 * better than subclass the GTabbedPane
	 */
	public JPopupMenu createDataTablePopupMenu()
	{
		_logger.debug("createDataTablePopupMenu(): called.");

		JPopupMenu popup = new JPopupMenu();

		final JMenuItem edit      = new JMenuItem("Edit Value for selected row with the templates editor");
		final JMenuItem toDefault = new JMenuItem("Set the Default value for the selected row");
		final JMenuItem toHtmlDefault = new JMenuItem("For 'Msg-Template' set Default HTML template");
		final JMenuItem toTextDefault = new JMenuItem("For 'Msg-Template' set Default TEXT template");

		popup.add(edit);
		popup.add(toDefault);
		popup.add(toHtmlDefault);
		popup.add(toTextDefault);

		edit.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JTable table = AlarmWriterSettingsTable.this;
				int vrow = getSelectedRow();
				if (vrow == -1)
					return;

				int vcol = SwingUtils.findColumnView(table, "Value");
				int pcol = SwingUtils.findColumnView(table, "Property");
				
//				// Translate View row to Model row
//				int mrow = convertRowIndexToModel(vrow);
//				int mcol = SwingUtils.findColumn(getModel(), "Value");
//				
//				String val = getModel().getValueAt(mrow, mcol) + "";
				String val     = table.getValueAt(vrow, vcol) + "";
				String propKey = table.getValueAt(vrow, pcol) + "";
				
				val = TemplateEditor.showDialog(null, val, _currentWriterClassName, propKey, getCurrentConfig());
				if (StringUtil.hasValue(val))
				{
					table.setValueAt(val.trim(), vrow, vcol);
					//getModel().setValueAt(val, mrow, mcol);
				}
			}
		});

		toDefault.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JTable table = AlarmWriterSettingsTable.this;
				int vrow = getSelectedRow();
				if (vrow == -1)
					return;

				int vcol_value   = SwingUtils.findColumnView(table, "Value");
				int vcol_default = SwingUtils.findColumnView(table, "Default");
				
				String defaultVal = table.getValueAt(vrow, vcol_default) + "";
				table.setValueAt(defaultVal, vrow, vcol_value);
			}
		});

		toHtmlDefault.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JTable table = AlarmWriterSettingsTable.this;
				int vrow = getSelectedRow();
				if (vrow == -1)
					return;

				int vcol_value   = SwingUtils.findColumnView(table, "Value");
				//int vcol_default = SwingUtils.findColumnView(table, "Default");
				
				String defaultVal = AlarmWriterToMail.createHtmlMsgBodyTemplate();
				table.setValueAt(defaultVal, vrow, vcol_value);
			}
		});

		toTextDefault.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JTable table = AlarmWriterSettingsTable.this;
				int vrow = getSelectedRow();
				if (vrow == -1)
					return;

				int vcol_value   = SwingUtils.findColumnView(table, "Value");
				//int vcol_default = SwingUtils.findColumnView(table, "Default");
				
				String defaultVal = AlarmWriterToMail.createTextMsgBodyTemplate();
				table.setValueAt(defaultVal, vrow, vcol_value);
			}
		});

		// Hide toHtmlDefault if not in correct "place"
		popup.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				boolean visibleToHtmlAndTextDefault = false;
				
				JTable table = AlarmWriterSettingsTable.this;
				int vrow = getSelectedRow();
				if (vrow != -1)
				{
					int vcol_name = SwingUtils.findColumnView(table, "Name");
					
					String nameVal = table.getValueAt(vrow, vcol_name) + "";

					if ("Msg-Template".equals(nameVal))
						visibleToHtmlAndTextDefault = true;
				}

				toHtmlDefault.setVisible(visibleToHtmlAndTextDefault);
				toTextDefault.setVisible(visibleToHtmlAndTextDefault);
			}
			
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
			{
			}
			
			@Override
			public void popupMenuCanceled(PopupMenuEvent e)
			{
			}
		});

		if (popup.getComponentCount() == 0)
		{
			_logger.warn("No PopupMenu has been assigned for the data table in the panel.");
			return null;
		}
		else
			return popup;
	}

	/** Get the JMeny attached to the JTable header */
	public JPopupMenu getDataTableHeaderPopupMenu()
	{
		return _headerPopupMenu;
	}

	public JPopupMenu createDataTableHeaderPopupMenu()
	{
		_logger.debug("createDataTableHeaderPopupMenu(): called.");
		JPopupMenu popup = new JPopupMenu();
		JMenuItem mark   = new JMenuItem("Mark all rows for this column");
		JMenuItem unmark = new JMenuItem("UnMark all rows for this column");

		popup.add(mark);
		popup.add(unmark);

		mark.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				TableModel tm = getModel();
				int col = getLastTableHeaderColumn();
				if (tm.getColumnClass(col).equals(Boolean.class))
				{
					for (int r=0; r<tm.getRowCount(); r++)
					{
						if (tm.isCellEditable(r, col))
							tm.setValueAt(Boolean.valueOf(true), r, col);
					}
				}
			}
		});

		unmark.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				TableModel tm = getModel();
				int col = getLastTableHeaderColumn();
				if (tm.getColumnClass(col).equals(Boolean.class))
				{
					for (int r=0; r<tm.getRowCount(); r++)
					{
						if (tm.isCellEditable(r, col))
							tm.setValueAt(Boolean.valueOf(false), r, col);
					}
				}
			}
		});

		// add something like:
		// popup.preShow()... so we can enable/disable menu items when we are on specific columns
		//popup.add

		if (popup.getComponentCount() == 0)
		{
			_logger.warn("No PopupMenu has been assigned for the data table in the panel.");
			return null;
		}
		else
			return popup;
	}


	public boolean isDirty()
	{
		return _alarmWriterSettingsTableModel.isDirty();
	}

	/*---------------------------------------------------
	** END: PopupMenu on the table
	**---------------------------------------------------
	*/
}

/*---------------------------------------------------
** END: class LocalTable
**---------------------------------------------------
*/

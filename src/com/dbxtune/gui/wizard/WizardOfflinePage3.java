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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.gui.wizard;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.UUID;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.table.TableColumnModelExt;
import org.netbeans.spi.wizard.WizardPage;

import com.dbxtune.CounterController;
import com.dbxtune.Version;
import com.dbxtune.cm.CounterModelHostMonitor;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.swing.GTableFilter;
import com.dbxtune.gui.swing.MultiLineLabel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;


public class WizardOfflinePage3
extends WizardPage
implements ActionListener, TableModelListener
{
    private static final long serialVersionUID = 1L;
	private static final String WIZ_NAME = "offline-sessions";
	private static final String WIZ_DESC = "What should be offline sampled";
	private static final String WIZ_HELP = "What panels/tabs do we want to be offline sampled.";

//	private static final int TAB_POS_POSTPONE   = 0;
//	private static final int TAB_POS_STORE_PCS  = 1;
//	private static final int TAB_POS_STORE_ABS  = 2;
//	private static final int TAB_POS_STORE_DIFF = 3;
//	private static final int TAB_POS_STORE_RATE = 4;
//	private static final int TAB_POS_ICON       = 5;
//	private static final int TAB_POS_TAB_NAME   = 6;
//	private static final int TAB_POS_CM_NAME    = 7;
//	private static final int TAB_POS_LONG_DESC  = 8;
//	private static final int TAB_POS_MAX        = 9; // not a column, it's just the MAX ID +1 

	private static final String[] TAB_HEADER = {"Icon", "Short Desc", "Group", "Timeout", "Postpone", "Store", "Abs", "Diff", "Rate", "Long Description", "CM Name"};
	private static final int TAB_POS_ICON       = 0;
	private static final int TAB_POS_TAB_NAME   = 1;
	private static final int TAB_POS_GROUP_NAME = 2; 
	private static final int TAB_POS_TIMEOUT    = 3;
	private static final int TAB_POS_POSTPONE   = 4;
	private static final int TAB_POS_STORE_PCS  = 5;
	private static final int TAB_POS_STORE_ABS  = 6;
	private static final int TAB_POS_STORE_DIFF = 7;
	private static final int TAB_POS_STORE_RATE = 8;
	private static final int TAB_POS_LONG_DESC  = 9;
	private static final int TAB_POS_CM_NAME    = 10;

	private static final Color TAB_PCS_COL_BG = new Color(240, 240, 240);

	private JXTable _sessionTable = new JXTable()
	{
		private static final long	serialVersionUID	= 1L;

		/** Enable/Disable + add some color to pcsStore, Abs, Diff, Rate */
		@Override
		public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
		{
			Component c = super.prepareRenderer(renderer, row, column);

			int view_TAB_POS_STORE_PCS  = convertColumnIndexToView(TAB_POS_STORE_PCS);
			int view_TAB_POS_STORE_ABS  = convertColumnIndexToView(TAB_POS_STORE_ABS);
			int view_TAB_POS_STORE_RATE = convertColumnIndexToView(TAB_POS_STORE_RATE);
			
			if (column >= view_TAB_POS_STORE_PCS && column <= view_TAB_POS_STORE_RATE)
			{
				c.setBackground(TAB_PCS_COL_BG);
				if ((column >= view_TAB_POS_STORE_ABS && column <= view_TAB_POS_STORE_RATE) || row == 0)
				{
					// if not editable, lets disable it
					// calling isCellEditable instead of getModel().isCellEditable(row, column)
					// does the viewRow->modelRow translation for us.
					c.setEnabled( isCellEditable(row, column) );
				}
			}
			return c;
		}
	};

	public static String getDescription() { return WIZ_DESC; }
	@Override
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	public WizardOfflinePage3()
	{
		super(WIZ_NAME, WIZ_DESC);
		
		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		// Add a helptext
		add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );

		// Create a TABLE
		Vector<String> tabHead = new Vector<String>();
		tabHead.setSize(TAB_HEADER.length);
		tabHead.set(TAB_POS_TIMEOUT,    TAB_HEADER[TAB_POS_TIMEOUT]);
		tabHead.set(TAB_POS_POSTPONE,   TAB_HEADER[TAB_POS_POSTPONE]);
		tabHead.set(TAB_POS_STORE_PCS,  TAB_HEADER[TAB_POS_STORE_PCS]);
		tabHead.set(TAB_POS_STORE_ABS,  TAB_HEADER[TAB_POS_STORE_ABS]);
		tabHead.set(TAB_POS_STORE_DIFF, TAB_HEADER[TAB_POS_STORE_DIFF]);
		tabHead.set(TAB_POS_STORE_RATE, TAB_HEADER[TAB_POS_STORE_RATE]);
		tabHead.set(TAB_POS_ICON,       TAB_HEADER[TAB_POS_ICON]);
		tabHead.set(TAB_POS_TAB_NAME,   TAB_HEADER[TAB_POS_TAB_NAME]);
		tabHead.set(TAB_POS_GROUP_NAME, TAB_HEADER[TAB_POS_GROUP_NAME]);
		tabHead.set(TAB_POS_CM_NAME,    TAB_HEADER[TAB_POS_CM_NAME]);
		tabHead.set(TAB_POS_LONG_DESC,  TAB_HEADER[TAB_POS_LONG_DESC]);

		Vector<Vector<Object>> tabData = populateTable();

		DefaultTableModel defaultTabModel = new DefaultTableModel(tabData, tabHead)
		{
            private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(int column) 
			{
				if (column == TAB_POS_TIMEOUT)    return Integer.class;
				if (column == TAB_POS_POSTPONE)   return Integer.class;
				if (column == TAB_POS_STORE_PCS)  return Boolean.class;
				if (column == TAB_POS_STORE_ABS)  return Boolean.class;
				if (column == TAB_POS_STORE_DIFF) return Boolean.class;
				if (column == TAB_POS_STORE_RATE) return Boolean.class;
				if (column == TAB_POS_ICON)       return Icon.class;
				return Object.class;
			}
			@Override
			public boolean isCellEditable(int row, int col)
			{
				if (row == 0) // CMSummary
					return false;

				if (col == TAB_POS_TIMEOUT)
					return true;

				if (col == TAB_POS_POSTPONE)
					return true;

				if (col == TAB_POS_STORE_PCS)
					return true;

				if (col <= TAB_POS_STORE_RATE)
				{
					// get some values from the MODEL viewRow->modelRow translation should be done before calling isCellEditable
					boolean storePcs    = ((Boolean) getValueAt(row, TAB_POS_STORE_PCS)).booleanValue();
					String tabName      = (String)   getValueAt(row, TAB_POS_TAB_NAME);

					//System.out.println("isCellEditable: row="+row+", col="+col+", storePcs="+storePcs+", tabName='"+tabName+"'.");

					// Get CountersModel and check if that model supports editing for Abs, Diff & Rate
//					CountersModel cm  = GetCounters.getInstance().getCmByDisplayName(tabName);
					CountersModel cm  = CounterController.getInstance().getCmByDisplayName(tabName);
					if (cm != null)
					{
						if (col == TAB_POS_STORE_ABS)  return storePcs && cm.isPersistCountersAbsEditable();
						if (col == TAB_POS_STORE_DIFF) return storePcs && cm.isPersistCountersDiffEditable();
						if (col == TAB_POS_STORE_RATE) return storePcs && cm.isPersistCountersRateEditable();
					}
				}
				return false;
			}
		};
		defaultTabModel.addTableModelListener(this);

		_sessionTable.setModel( defaultTabModel );
		_sessionTable.setSortable(false);
//		_sessionTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		_sessionTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//		_sessionTable.setAutoscrolls(true);
//		_sessionTable.doLayout();
		_sessionTable.setShowGrid(false);
//		_sessionTable.setShowHorizontalLines(false);
//		_sessionTable.setShowVerticalLines(false);
//		_sessionTable.setMaximumSize(new Dimension(10000, 10000));

		SwingUtils.calcColumnWidths(_sessionTable);

		// hide 'Group Name' if no child's are found
		if ( MainFrame.hasInstance() && ! MainFrame.getInstance().getTabbedPane().hasChildPanels() )
		{
			TableColumnModelExt tcmx = (TableColumnModelExt)_sessionTable.getColumnModel();
			tcmx.getColumnExt(TAB_HEADER[TAB_POS_GROUP_NAME]).setVisible(false);
		}

		GTableFilter filter = new GTableFilter(_sessionTable, GTableFilter.ROW_COUNT_LAYOUT_LEFT, true);
		filter.setText("");
		
		add(filter,      "span, growx, wrap");

		JScrollPane jScrollPane = new JScrollPane();
		jScrollPane.setViewportView(_sessionTable);
//		jScrollPane.setMaximumSize(new Dimension(10000, 10000));
		add(jScrollPane, "span, grow, height 100%, wrap");

		JButton button = null;
		button = new JButton("Select All");
		button.addActionListener(this);
		button.putClientProperty("NAME", "BUTTON_SELECT_ALL");
		add(button, "");

		button = new JButton("Deselect All");
		button.addActionListener(this);
		button.putClientProperty("NAME", "BUTTON_DESELECT_ALL");
		add(button, "split");

		button = new JButton("Reset");
		button.setToolTipText("Use current setting of the tabs as a template.");
		button.addActionListener(this);
		button.putClientProperty("NAME", "BUTTON_TEMPLATE");
		add(button, "split");

		initData();
	}

	private void initData()
	{
	}

	private Vector<Vector<Object>> populateTable()
	{
		Vector<Vector<Object>> tab = new Vector<Vector<Object>>();
		Vector<Object>         row = new Vector<Object>();

		boolean debug = false;
		if (!debug)
		{
			for (CountersModel cm : CounterController.getInstance().getCmList())
			{
				if (cm != null)
				{
					row = new Vector<Object>();
					row.setSize(TAB_HEADER.length);
					row.set(TAB_POS_TIMEOUT,    Integer.valueOf(cm.getQueryTimeout()));
					row.set(TAB_POS_POSTPONE,   Integer.valueOf(cm.getPostponeTime()));
					row.set(TAB_POS_STORE_PCS,  Boolean.valueOf(cm.isPersistCountersEnabled() || cm.isBackgroundDataPollingEnabled()));
					row.set(TAB_POS_STORE_ABS,  Boolean.valueOf(cm.isPersistCountersAbsEnabled()));
					row.set(TAB_POS_STORE_DIFF, Boolean.valueOf(cm.isPersistCountersDiffEnabled()));
					row.set(TAB_POS_STORE_RATE, Boolean.valueOf(cm.isPersistCountersRateEnabled()));
//					row.set(TAB_POS_ICON,       cm.getTabPanel() == null ? null : cm.getTabPanel().getIcon());
					row.set(TAB_POS_ICON,       cm.getIconFile() == null ? null : SwingUtils.readImageIcon(Version.class, cm.getIconFile()));
					row.set(TAB_POS_TAB_NAME,   cm.getDisplayName());
					row.set(TAB_POS_GROUP_NAME, cm.getGroupName());
					row.set(TAB_POS_CM_NAME,    cm.getName());
					row.set(TAB_POS_LONG_DESC,  cm.getDescription().replaceAll("\\<.*?\\>", "")); // STRIP HTML Tags from the description.
					tab.add(row);

//					if (cm.getName().equals(SummaryPanel.CM_NAME))
//					if (cm.getName().equals(CounterController.getSummaryCmName()))
//					{
//						row.set(TAB_POS_ICON, SwingUtils.readImageIcon(Version.class, "images/summary_tab.png"));
//					}
				}
			}
			setToTemplate();
		}
		else
		{
			row = new Vector<Object>();
			row.setSize(TAB_HEADER.length);
			row.set(TAB_POS_TIMEOUT,    Integer.valueOf(0));
			row.set(TAB_POS_POSTPONE,   Integer.valueOf(0));
			row.set(TAB_POS_STORE_PCS,  Boolean.valueOf(true));
			row.set(TAB_POS_STORE_ABS,  Boolean.valueOf(true));
			row.set(TAB_POS_STORE_DIFF, Boolean.valueOf(true));
			row.set(TAB_POS_STORE_RATE, Boolean.valueOf(true));
			row.set(TAB_POS_ICON,       SwingUtils.readImageIcon(Version.class, "images/cm_summary_activity.png"));
			row.set(TAB_POS_TAB_NAME,   "Summary");
			row.set(TAB_POS_CM_NAME,    "cmSummary");
			row.set(TAB_POS_LONG_DESC,  "All the fields on the left hand side of the graphs.");
			tab.add(row);
	
			row = new Vector<Object>();
			row.setSize(TAB_HEADER.length);
			row.set(TAB_POS_TIMEOUT,    Integer.valueOf(0));
			row.set(TAB_POS_POSTPONE,   Integer.valueOf(0));
			row.set(TAB_POS_STORE_PCS,  Boolean.valueOf(true));
			row.set(TAB_POS_STORE_ABS,  Boolean.valueOf(true));
			row.set(TAB_POS_STORE_DIFF, Boolean.valueOf(true));
			row.set(TAB_POS_STORE_RATE, Boolean.valueOf(true));
			row.set(TAB_POS_ICON,       SwingUtils.readImageIcon(Version.class, "images/cm_engine_activity.png"));
			row.set(TAB_POS_TAB_NAME,   "CPU Usage");
			row.set(TAB_POS_CM_NAME,    "cmCpu");
			row.set(TAB_POS_LONG_DESC,  "bla bla bla... asfdha dkjfg askj gfakj gfkajgshd fagsakgdfakdfhs kjfhgoiqay edatfshjghv kfdsjhgaks dfajhdfskjdf glkash df.");
			tab.add(row);
	
			row = new Vector<Object>();
			row.setSize(TAB_HEADER.length);
			row.set(TAB_POS_TIMEOUT,    Integer.valueOf(0));
			row.set(TAB_POS_POSTPONE,   Integer.valueOf(0));
			row.set(TAB_POS_STORE_PCS,  Boolean.valueOf(true));
			row.set(TAB_POS_STORE_ABS,  Boolean.valueOf(true));
			row.set(TAB_POS_STORE_DIFF, Boolean.valueOf(true));
			row.set(TAB_POS_STORE_RATE, Boolean.valueOf(true));
			row.set(TAB_POS_ICON,       SwingUtils.readImageIcon(Version.class, "images/cm_device_activity.png"));
			row.set(TAB_POS_TAB_NAME,   "Device Usage");
			row.set(TAB_POS_CM_NAME,    "cmDevice");
			row.set(TAB_POS_LONG_DESC,  "wwwwwwwwwwwwwwww wwww ttttt uuuuuu bla bla bla... hhhhhhhhhhhhh  kkkkkkkkkkkk yyyyyyy ssssssssssssssssss ggggggggggggg w wwww aaaaa.");
			tab.add(row);
	
			for (int i=0; i<40; i++)
			{
				row = new Vector<Object>();
				row.setSize(TAB_HEADER.length);
				row.set(TAB_POS_TIMEOUT,    Integer.valueOf(0));
				row.set(TAB_POS_POSTPONE,   Integer.valueOf(0));
				row.set(TAB_POS_STORE_PCS,  Boolean.valueOf(true));
				row.set(TAB_POS_STORE_ABS,  Boolean.valueOf(true));
				row.set(TAB_POS_STORE_DIFF, Boolean.valueOf(true));
				row.set(TAB_POS_STORE_RATE, Boolean.valueOf(true));
				row.set(TAB_POS_ICON,       SwingUtils.readImageIcon(Version.class, "images/ud_counter_activity.png"));
				row.set(TAB_POS_TAB_NAME,   "Dummy Tab "+i);
				row.set(TAB_POS_CM_NAME,    "cmDummy"+i);
				row.set(TAB_POS_LONG_DESC,  UUID.randomUUID().toString() + " : " + UUID.randomUUID().toString());
				tab.add(row);
			}			
		}

		return tab;
	}

	@Override
	protected String validateContents(Component comp, Object event)
	{
//		String name = null;
//		if (comp != null)
//			name = comp.getName();

//		System.out.println("validateContents: name='"+name+"',\n\ttoString='"+comp+"'\n\tcomp='"+comp+"',\n\tevent='"+event+"'.");

		putWizardData("to-be-discarded.HostMonitorIsSelected", "false");

		int rows = 0;
		TableModel tm = _sessionTable.getModel();
		for (int r=0; r<tm.getRowCount(); r++)
		{
			Integer timeout   = (Integer) tm.getValueAt(r, TAB_POS_TIMEOUT);
			Integer postpone  = (Integer) tm.getValueAt(r, TAB_POS_POSTPONE);
			boolean storePcs  = ((Boolean)tm.getValueAt(r, TAB_POS_STORE_PCS)).booleanValue();
			boolean storeAbs  = ((Boolean)tm.getValueAt(r, TAB_POS_STORE_ABS)).booleanValue();
			boolean storeDiff = ((Boolean)tm.getValueAt(r, TAB_POS_STORE_DIFF)).booleanValue();
			boolean storeRate = ((Boolean)tm.getValueAt(r, TAB_POS_STORE_RATE)).booleanValue();
			String  cmName    = (String)  tm.getValueAt(r, TAB_POS_CM_NAME);
			if ( storePcs )
			{
				rows++;
			}
			
			CountersModel cm = CounterController.getInstance().getCmByName(cmName);

			String p_timeout   = (cm.getDefaultQueryTimeout()                 == timeout  ) ? Configuration.USE_DEFAULT_PREFIX + timeout   : "" + timeout;
			String p_postpone  = (cm.getDefaultPostponeTime()                 == postpone ) ? Configuration.USE_DEFAULT_PREFIX + postpone  : "" + postpone;
			String p_storePcs  = (cm.getDefaultIsPersistCountersEnabled()     == storePcs ) ? Configuration.USE_DEFAULT_PREFIX + storePcs  : "" + storePcs;
			String p_storeAbs  = (cm.getDefaultIsPersistCountersAbsEnabled()  == storeAbs ) ? Configuration.USE_DEFAULT_PREFIX + storeAbs  : "" + storeAbs;
			String p_storeDiff = (cm.getDefaultIsPersistCountersDiffEnabled() == storeDiff) ? Configuration.USE_DEFAULT_PREFIX + storeDiff : "" + storeDiff;
			String p_storeRate = (cm.getDefaultIsPersistCountersRateEnabled() == storeRate) ? Configuration.USE_DEFAULT_PREFIX + storeRate : "" + storeRate;
			
			
			// This line is picked up by WizardOffline.finish(), which produces the out file.
			// and will be used for User Defined Counter checking...
			putWizardData( "to-be-discarded" + ".udc." + cmName, cmName);

//			putWizardData( cmName+"."+CountersModel.PROPKEY_queryTimeout,         timeout.toString());
//			putWizardData( cmName+"."+CountersModel.PROPKEY_postponeTime,         postpone.toString());
//
//			putWizardData( cmName+"."+CountersModel.PROPKEY_persistCounters,      storePcs  +"");
//			putWizardData( cmName+"."+CountersModel.PROPKEY_persistCounters_abs,  storeAbs  +"");
//			putWizardData( cmName+"."+CountersModel.PROPKEY_persistCounters_diff, storeDiff +"");
//			putWizardData( cmName+"."+CountersModel.PROPKEY_persistCounters_rate, storeRate +"");

			putWizardData( cmName+"."+CountersModel.PROPKEY_queryTimeout,         p_timeout);
			putWizardData( cmName+"."+CountersModel.PROPKEY_postponeTime,         p_postpone);

			putWizardData( cmName+"."+CountersModel.PROPKEY_persistCounters,      p_storePcs );
			putWizardData( cmName+"."+CountersModel.PROPKEY_persistCounters_abs,  p_storeAbs );
			putWizardData( cmName+"."+CountersModel.PROPKEY_persistCounters_diff, p_storeDiff);
			putWizardData( cmName+"."+CountersModel.PROPKEY_persistCounters_rate, p_storeRate);
			
			if (cm != null)
			{
				if (cm instanceof CounterModelHostMonitor && storePcs)
					putWizardData("to-be-discarded.HostMonitorIsSelected", "true");
			}
		}

		return rows > 1 ? null : "Atleast one session needs to be checked (except Summary).";
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
		JComponent src = (JComponent) ae.getSource();
		String name = (String)src.getClientProperty("NAME");
		if (name == null)
			name = "-null-";

//		System.out.println("Source("+name+"): " + src);

		if (name.equals("BUTTON_SELECT_ALL"))
		{
			TableModel tm = _sessionTable.getModel();
			for (int r=0; r<tm.getRowCount(); r++)
			{
				if (_sessionTable.isCellEditable(r, TAB_POS_STORE_PCS))
					tm.setValueAt(Boolean.valueOf(true), r, TAB_POS_STORE_PCS);
			}
		}

		if (name.equals("BUTTON_DESELECT_ALL"))
		{
			TableModel tm = _sessionTable.getModel();
			for (int r=0; r<tm.getRowCount(); r++)
			{
				if (_sessionTable.isCellEditable(r, TAB_POS_STORE_PCS))
					tm.setValueAt(Boolean.valueOf(false), r, TAB_POS_STORE_PCS);

				if (_sessionTable.isCellEditable(r, TAB_POS_POSTPONE))
					tm.setValueAt(Integer.valueOf(0), r, TAB_POS_POSTPONE);

				if (_sessionTable.isCellEditable(r, TAB_POS_TIMEOUT))
					tm.setValueAt(Integer.valueOf(0), r, TAB_POS_TIMEOUT);
			}
		}

		if (name.equals("BUTTON_TEMPLATE"))
		{
			setToTemplate();
		}

		// This wasnt kicked off for a table change...
		// Do not fire this on every row changed...
		//setProblem(validateContents(null,null));
	}

	@Override
	public void tableChanged(TableModelEvent e)
	{
		// This wasnt kicked off for a table change...
		setProblem(validateContents(null,null));
	}

	private void setToTemplate()
	{
		TableModel tm = _sessionTable.getModel();
		for (int r=0; r<tm.getRowCount(); r++)
		{
			String        cmName = (String)  tm.getValueAt(r, TAB_POS_CM_NAME);
//			CountersModel cm     = GetCounters.getInstance().getCmByName(cmName);
			CountersModel cm     = CounterController.getInstance().getCmByName(cmName);
			if (cm == null)
				continue;

			tm.setValueAt(Integer.valueOf(cm.getQueryTimeout()),              r, TAB_POS_TIMEOUT);
			tm.setValueAt(Integer.valueOf(cm.getPostponeTime()),              r, TAB_POS_POSTPONE);
			tm.setValueAt(Boolean.valueOf(cm.isPersistCountersEnabled()),     r, TAB_POS_STORE_PCS);
			tm.setValueAt(Boolean.valueOf(cm.isPersistCountersAbsEnabled()),  r, TAB_POS_STORE_ABS);
			tm.setValueAt(Boolean.valueOf(cm.isPersistCountersDiffEnabled()), r, TAB_POS_STORE_DIFF);
			tm.setValueAt(Boolean.valueOf(cm.isPersistCountersRateEnabled()), r, TAB_POS_STORE_RATE);
		}
	}
}

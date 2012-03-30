/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;
import org.netbeans.spi.wizard.WizardPage;

import com.asetune.GetCounters;
import com.asetune.cm.CountersModel;
import com.asetune.gui.swing.MultiLineLabel;
import com.asetune.utils.Configuration;


public class WizardOfflinePage4
extends WizardPage
implements ActionListener, TableModelListener
{
    private static final long serialVersionUID = 1L;
	private static final String WIZ_NAME = "local-options";
	private static final String WIZ_DESC = "Performance Counters Options";
	private static final String WIZ_HELP = "Some Performance Counters has local options, which you can edit here. Click 'Value' cell to change the desired value.";

	public static String getDescription() { return WIZ_DESC; }
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	private static final String[] TAB_HEADER = {"Icon", "Performance Counter", "Local Option", "Value", "Data Type", "Description"};
	private static final int TAB_POS_ICON         = 0;
	private static final int TAB_POS_TAB_NAME     = 1;
	private static final int TAB_POS_OPTION       = 2;
	private static final int TAB_POS_OPTION_VALUE = 3;
	private static final int TAB_POS_OPTION_DTYPE = 4;
	private static final int TAB_POS_OPTION_DESC  = 5;

	private JXTable _optionsTable = new JXTable()
	{
		private static final long	serialVersionUID	= 1L;

		// tooltip on cells
		public String getToolTipText(MouseEvent e) 
		{
			String tip = null;
			Point p = e.getPoint();
			int col = columnAtPoint(p);
			int row = rowAtPoint(p);
			if (col >= 0 && row >= 0)
			{
				col = super.convertColumnIndexToModel(col);
				row = super.convertRowIndexToModel(row);

				TableModel tm = getModel();

				tip = tm.getValueAt(row, col) + "";
			}
			return tip;
		}
	};

	public WizardOfflinePage4()
	{
		super(WIZ_NAME, WIZ_DESC);
		
		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		// Add a helptext
		add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );

		// Create a TABLE
		Vector<String> tabHead = new Vector<String>();
		tabHead.setSize(TAB_HEADER.length);
		tabHead.set(TAB_POS_ICON,         TAB_HEADER[TAB_POS_ICON]);
		tabHead.set(TAB_POS_TAB_NAME,     TAB_HEADER[TAB_POS_TAB_NAME]);
		tabHead.set(TAB_POS_OPTION,       TAB_HEADER[TAB_POS_OPTION]);
		tabHead.set(TAB_POS_OPTION_VALUE, TAB_HEADER[TAB_POS_OPTION_VALUE]);
		tabHead.set(TAB_POS_OPTION_DTYPE, TAB_HEADER[TAB_POS_OPTION_DTYPE]);
		tabHead.set(TAB_POS_OPTION_DESC,  TAB_HEADER[TAB_POS_OPTION_DESC]);

		Vector<Vector<Object>> tabData = populateTable();

		DefaultTableModel defaultTabModel = new DefaultTableModel(tabData, tabHead)
		{
            private static final long serialVersionUID = 1L;

			public Class<?> getColumnClass(int column) 
			{
				if (column == TAB_POS_ICON)       return Icon.class;
				return Object.class;
			}
			public boolean isCellEditable(int row, int col)
			{
				if (col == TAB_POS_OPTION_VALUE)
					return true;

				return false;
			}
		};

		defaultTabModel.addTableModelListener(this);

		_optionsTable.setModel( defaultTabModel );
		_optionsTable.setSortable(false);
		_optionsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		_optionsTable.setShowGrid(false);
		_optionsTable.packAll();

		JScrollPane jScrollPane = new JScrollPane();
		jScrollPane.setViewportView(_optionsTable);

		add(jScrollPane, "span, grow, height 100%, wrap");
	}

	private Vector<Vector<Object>> populateTable()
	{
		Vector<Vector<Object>> tab = new Vector<Vector<Object>>();
		Vector<Object>         row = new Vector<Object>();

		boolean debug = false;
		if (!debug)
		{
			for (CountersModel cm : GetCounters.getCmList())
			{
				if (cm != null)
				{
					Configuration conf = cm.getLocalConfiguration();
					if (conf != null)
					{
						for (Entry<Object, Object> entry : conf.entrySet()) 
						{
							String key      = (String)entry.getKey();
							String val      = (String)entry.getValue();
							String desc     = cm.getLocalConfigurationDescription(key);
							String dataType = cm.getLocalConfigurationDataType(key);

							row = new Vector<Object>();
							row.setSize(TAB_HEADER.length);

							row.set(TAB_POS_ICON,         cm.getTabPanel() == null ? null : cm.getTabPanel().getIcon());
							row.set(TAB_POS_TAB_NAME,     cm.getDisplayName());
							row.set(TAB_POS_OPTION,       key);
							row.set(TAB_POS_OPTION_VALUE, val);
							row.set(TAB_POS_OPTION_DTYPE, dataType);
							row.set(TAB_POS_OPTION_DESC,  desc);

							tab.add(row);
						}
					}
				}
			}
		}

		return tab;
	}

	@Override
	public void tableChanged(TableModelEvent e)
	{
		// This wasnt kicked off for a table change...
		setProblem(validateContents(null,null));
	}

	@Override
	protected String validateContents(Component comp, Object event)
	{
		TableModel tm = _optionsTable.getModel();
		for (int r=0; r<tm.getRowCount(); r++)
		{
			String  option     = (String)  tm.getValueAt(r, TAB_POS_OPTION);
			String  optionVal  = (String)  tm.getValueAt(r, TAB_POS_OPTION_VALUE);
			String  optionType = (String)  tm.getValueAt(r, TAB_POS_OPTION_DTYPE);

			if (optionType.equals("Integer"))
			{
				try 
				{
					Integer.parseInt(optionVal); 
				}
				catch (NumberFormatException ignore) 
				{
					return "option '"+option+"', must be a number. Now it's '"+optionVal+"'.";
				}
			}
			if (optionType.equals("Boolean"))
			{
				boolean ok = false;
				if      (optionVal.equalsIgnoreCase("true"))  ok = true;
				else if (optionVal.equalsIgnoreCase("false")) ok = true;
				else ok = false;
				
				if (!ok)
					return "option '"+option+"', must be 'true' or 'false'. Now it's '"+optionVal+"'.";
			}

			// Now write the info...
			putWizardData( option, optionVal);
		}
		return null;
	}

	public void actionPerformed(ActionEvent ae)
	{
	}
}


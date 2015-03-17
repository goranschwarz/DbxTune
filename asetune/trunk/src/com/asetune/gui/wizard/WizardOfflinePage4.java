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
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;
import org.netbeans.spi.wizard.WizardPage;

import com.asetune.CounterController;
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
	@Override
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	private static final String[] TAB_HEADER = {"Icon", "Performance Counter", "Local Option", "Data Type", "Value", "StringValue", "Description"};
	private static final int TAB_POS_ICON             = 0;
	private static final int TAB_POS_TAB_NAME         = 1;
	private static final int TAB_POS_OPTION           = 2;
	private static final int TAB_POS_OPTION_DTYPE     = 3;
	private static final int TAB_POS_OPTION_BOL_VALUE = 4;
	private static final int TAB_POS_OPTION_STR_VALUE = 5;
	private static final int TAB_POS_OPTION_DESC      = 6;

//	private static final Color TAB_DISABLED_COL_BG = new Color(240, 240, 240);

	private JXTable _optionsTable = new JXTable()
	{
		private static final long	serialVersionUID	= 1L;

		// tooltip on cells
		@Override
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

		/** Enable/Disable + add some color to pcsStore, Abs, Diff, Rate */
		@Override
		public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
		{
			Component c = super.prepareRenderer(renderer, row, column);

			int view_TAB_POS_OPTION_BOL_VALUE  = convertColumnIndexToView(TAB_POS_OPTION_BOL_VALUE);
			int view_TAB_POS_OPTION_STR_VALUE  = convertColumnIndexToView(TAB_POS_OPTION_STR_VALUE);
			
			if (column == view_TAB_POS_OPTION_BOL_VALUE || column == view_TAB_POS_OPTION_STR_VALUE)
			{
				// if not editable, lets disable it
				// calling isCellEditable instead of getModel().isCellEditable(row, column)
				// does the viewRow->modelRow translation for us.
				boolean isCellEditable = isCellEditable(row, column);

				c.setEnabled( isCellEditable );
//				if ( ! isCellEditable)
//					c.setBackground(TAB_DISABLED_COL_BG);
			}
			return c;
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
		tabHead.set(TAB_POS_ICON,             TAB_HEADER[TAB_POS_ICON]);
		tabHead.set(TAB_POS_TAB_NAME,         TAB_HEADER[TAB_POS_TAB_NAME]);
		tabHead.set(TAB_POS_OPTION,           TAB_HEADER[TAB_POS_OPTION]);
		tabHead.set(TAB_POS_OPTION_DTYPE,     TAB_HEADER[TAB_POS_OPTION_DTYPE]);
		tabHead.set(TAB_POS_OPTION_BOL_VALUE, TAB_HEADER[TAB_POS_OPTION_BOL_VALUE]);
		tabHead.set(TAB_POS_OPTION_STR_VALUE, TAB_HEADER[TAB_POS_OPTION_STR_VALUE]);
		tabHead.set(TAB_POS_OPTION_DESC,      TAB_HEADER[TAB_POS_OPTION_DESC]);

		Vector<Vector<Object>> tabData = populateTable();

		DefaultTableModel defaultTabModel = new DefaultTableModel(tabData, tabHead)
		{
            private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(int column) 
			{
				if (column == TAB_POS_ICON)             return Icon.class;
				if (column == TAB_POS_OPTION_BOL_VALUE) return Boolean.class;
				return Object.class;
			}

			@Override
			public boolean isCellEditable(int row, int col)
			{
				if (col == TAB_POS_OPTION_BOL_VALUE || col == TAB_POS_OPTION_STR_VALUE)
				{
					String datatype = getValueAt(row, TAB_POS_OPTION_DTYPE).toString();
					boolean isBoolean = "Boolean".equals(datatype);
					if (   isBoolean && col == TAB_POS_OPTION_BOL_VALUE) return true;
					if ( ! isBoolean && col == TAB_POS_OPTION_STR_VALUE) return true;
				}

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
			for (CountersModel cm : CounterController.getInstance().getCmList())
			{
				if (cm != null)
				{
					Configuration conf = cm.getLocalConfiguration();
					if (conf != null)
					{
//						for (Entry<Object, Object> entry : conf.entrySet()) 
//						{
//							String key      = (String)entry.getKey();
//							String val      = (String)entry.getValue();
						for (Object keyObj : conf.keySet()) 
						{
							String key      = keyObj + "";
							String val      = conf.getProperty(key);
							String desc     = cm.getLocalConfigurationDescription(key);
							String dataType = cm.getLocalConfigurationDataType(key);

							row = new Vector<Object>();
							row.setSize(TAB_HEADER.length);

							row.set(TAB_POS_ICON,             cm.getTabPanel() == null ? null : cm.getTabPanel().getIcon());
							row.set(TAB_POS_TAB_NAME,         cm.getDisplayName());
							row.set(TAB_POS_OPTION,           key);
							row.set(TAB_POS_OPTION_DTYPE,     dataType);
							row.set(TAB_POS_OPTION_BOL_VALUE, "Boolean".equals(dataType) ? new Boolean(val) : new Boolean(false));
							row.set(TAB_POS_OPTION_STR_VALUE, val);
							row.set(TAB_POS_OPTION_DESC,      desc);

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
		// This wasn't kicked off for a table change...
		setProblem(validateContents(null,null));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected String validateContents(Component comp, Object event)
	{
		DefaultTableModel tm = (DefaultTableModel) _optionsTable.getModel();
		for (int r=0; r<tm.getRowCount(); r++)
		{
			String  option        = (String)  tm.getValueAt(r, TAB_POS_OPTION);
			Boolean optionBolVal  = (Boolean) tm.getValueAt(r, TAB_POS_OPTION_BOL_VALUE);
			String  optionStrVal  = (String)  tm.getValueAt(r, TAB_POS_OPTION_STR_VALUE);
			String  optionType    = (String)  tm.getValueAt(r, TAB_POS_OPTION_DTYPE);

			if (optionType.equals("Integer"))
			{
				try 
				{
					Integer.parseInt(optionStrVal); 
				}
				catch (NumberFormatException ignore) 
				{
					return "option '"+option+"', must be a number. Now it's '"+optionStrVal+"'.";
				}
			}
			if (optionType.equals("Boolean"))
			{
			//	setValueAt(optionBolVal.toString(),  r, TAB_POS_OPTION_STR_VALUE);
				((Vector)tm.getDataVector().get(r)).set(TAB_POS_OPTION_STR_VALUE, optionBolVal.toString());
				optionStrVal  = (String)  tm.getValueAt(r, TAB_POS_OPTION_STR_VALUE);
				
				boolean ok = false;
				if      (optionStrVal.equalsIgnoreCase("true"))  ok = true;
				else if (optionStrVal.equalsIgnoreCase("false")) ok = true;
				else ok = false;
				
				if (!ok)
					return "option '"+option+"', must be 'true' or 'false'. Now it's '"+optionStrVal+"'.";
			}

			// Now write the info...
			putWizardData( option, optionStrVal);
		}
		return null;
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
	}
}


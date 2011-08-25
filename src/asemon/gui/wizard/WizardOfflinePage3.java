/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;

import org.netbeans.spi.wizard.WizardPage;

import asemon.CountersModel;
import asemon.GetCounters;
import asemon.gui.swing.MultiLineLabel;
import asemon.utils.SwingUtils;

public class WizardOfflinePage3
extends WizardPage
implements ActionListener, TableModelListener
{
    private static final long serialVersionUID = 1L;
	private static final String WIZ_NAME = "offline-sessions";
	private static final String WIZ_DESC = "What should be offline sampled";
	private static final String WIZ_HELP = "What panels/tabs do we want to be offline sampled.";

	private JTable _sessionTable = new JTable();

	public static String getDescription() { return WIZ_DESC; }
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	public WizardOfflinePage3()
	{
		super(WIZ_NAME, WIZ_DESC);
		
		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		// Add a helptext
		add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );

		// Create a TABLE
		Vector tabHead = new Vector();
		tabHead.add("Sample");
		tabHead.add("Name");
		tabHead.add("Short Desc");
		tabHead.add("Long Description");

		Vector tabData = populateTable();

		DefaultTableModel defaultTabModel = new DefaultTableModel(tabData, tabHead)
		{
            private static final long serialVersionUID = 1L;

			public Class getColumnClass(int column) 
			{
				if (column == 0)
				{
					return Boolean.class;
				}
				return Object.class;
			}
			public boolean isCellEditable(int row, int column)
			{
				return column == 0;
			}
		};
		defaultTabModel.addTableModelListener(this);

		_sessionTable.setModel( defaultTabModel );
//		_sessionTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		_sessionTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//		_sessionTable.setAutoscrolls(true);
//		_sessionTable.doLayout();
		_sessionTable.setShowGrid(false);
//		_sessionTable.setShowHorizontalLines(false);
//		_sessionTable.setShowVerticalLines(false);
//		_sessionTable.setMaximumSize(new Dimension(10000, 10000));

		SwingUtils.calcColumnWidths(_sessionTable);

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

		initData();
	}

	private void initData()
	{
	}

	private Vector populateTable()
	{
		Vector tab = new Vector();
		Vector row = new Vector();

		boolean debug = false;
		if (!debug)
		{
			Iterator iter = GetCounters.getCmList().iterator();
			while (iter.hasNext())
			{
				CountersModel cm = (CountersModel) iter.next();
				
				if (cm != null)
				{
					row = new Vector();
					row.add(new Boolean(true));
					row.add(cm.getName());
					row.add(cm.getDisplayName());
					row.add(cm.getDescription().replaceAll("\\<.*?\\>", "")); // STRIP HTML Tags from the description.
					tab.add(row);
				}
			}
		}
		else
		{
			row = new Vector();
			row.add(new Boolean(true));
			row.add("cmSummary");
			row.add("Summary");
			row.add("All the fields on the left hand side of the graphs.");
			tab.add(row);
	
			row = new Vector();
			row.add(new Boolean(true));
			row.add("cmCpu");
			row.add("CPU Usage");
			row.add("bla bla bla... asfdha dkjfg askj gfakj gfkajgshd fagsakgdfakdfhs kjfhgoiqay edatfshjghv kfdsjhgaks dfajhdfskjdf glkash df.");
			tab.add(row);
	
			row = new Vector();
			row.add(new Boolean(true));
			row.add("cmDevice");
			row.add("Device Usage");
			row.add("wwwwwwwwwwwwwwww wwww ttttt uuuuuu bla bla bla... hhhhhhhhhhhhh  kkkkkkkkkkkk yyyyyyy ssssssssssssssssss ggggggggggggg w wwww aaaaa.");
			tab.add(row);
	
			for (int i=0; i<40; i++)
			{
				row = new Vector();
				row.add(new Boolean(true));
				row.add("cmDummy"+i);
				row.add("Dummy Tab "+i);
				row.add( UUID.randomUUID().toString() + " : " + UUID.randomUUID().toString());
				tab.add(row);
			}			
		}

		return tab;
	}

	protected String validateContents(Component comp, Object event)
	{
		String name = null;
		if (comp != null)
			name = comp.getName();

//		System.out.println("validateContents: name='"+name+"',\n\ttoString='"+comp+"'\n\tcomp='"+comp+"',\n\tevent='"+event+"'.");

		int rows = 0;
		TableModel tm = _sessionTable.getModel();
		for (int r=0; r<tm.getRowCount(); r++)
		{
			if ( ((Boolean)tm.getValueAt(r, 0)).booleanValue() )
			{
				rows++;
			}
			putWizardData( tm.getValueAt(r, 1), "" + ((Boolean)tm.getValueAt(r, 0)).booleanValue());
		}

		return rows > 0 ? null : "Atleast one session needs to be checked.";
	}

	public void actionPerformed(ActionEvent ae)
	{
		JComponent src = (JComponent) ae.getSource();
		String name = (String)src.getClientProperty("NAME");
		if (name == null)
			name = "-null-";

		System.out.println("Source("+name+"): " + src);

		if (name.equals("BUTTON_SELECT_ALL"))
		{
			TableModel tm = _sessionTable.getModel();
			for (int r=0; r<tm.getRowCount(); r++)
			{
				tm.setValueAt(new Boolean(true), r, 0);
			}
		}

		if (name.equals("BUTTON_DESELECT_ALL"))
		{
			TableModel tm = _sessionTable.getModel();
			for (int r=0; r<tm.getRowCount(); r++)
			{
				tm.setValueAt(new Boolean(false), r, 0);
			}
		}

		// This wasnt kicked off for a table change...
		// Do not fire this on every row changed...
		//setProblem(validateContents(null,null));
	}

	public void tableChanged(TableModelEvent e)
	{
		// This wasnt kicked off for a table change...
		setProblem(validateContents(null,null));
	}
}

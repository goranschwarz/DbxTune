/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;
import org.netbeans.spi.wizard.WizardPage;

import asemon.GetCounters;
import asemon.Version;
import asemon.cm.CountersModel;
import asemon.gui.SummaryPanel;
import asemon.gui.swing.MultiLineLabel;
import asemon.utils.SwingUtils;

//PAGE 1
public class WizardUserDefinedCmPage1
extends WizardPage
implements ActionListener
{
	private static final long serialVersionUID = 1L;
	private static final String WIZ_NAME  = "Name-info";
	private static final String WIZ_DESC  = "Name information";
	private static final String WIZ_HELP1 = "Name of the Counter Model, without spaces or special characters.";
	private static final String WIZ_HELP2 = "A short description of the Counter Model, use only 2 or 3 words, this will be the text on the GUI tab.";
	private static final String WIZ_HELP3 = "The long description, which will be used as a tooltip on the GUI tab.";

	private boolean    _firtsTimeRender = true;

	private JLabel     _name_lbl      = new JLabel("Name");
	private JTextField _name_txt      = new JTextField("");
	private JLabel     _shortDesc_lbl = new JLabel("Short Description");
	private JTextField _shortDesc_txt = new JTextField("");
	private JLabel     _longDesc_lbl  = new JLabel("Long Description");
	private JTextField _longDesc_txt  = new JTextField("");

	public static String getDescription() { return WIZ_DESC; }
	public Dimension getPreferredSize() { return WizardUserDefinedCm.preferredSize; }

	public WizardUserDefinedCmPage1()
	{
		super(WIZ_NAME, WIZ_DESC);
		
		setLayout(new MigLayout("", "[] [grow] []", ""));

		_name_txt     .setName("name");
		_shortDesc_txt.setName("displayName");
		_longDesc_txt .setName("description");

		add( new MultiLineLabel(WIZ_HELP1), "wmin 100, span, pushx, growx, wrap" );
		add(_name_lbl);
		add(_name_txt, "growx, pushx, split");

		JButton button = new JButton("...");
		button.setToolTipText("Use one of the current Counter Models as a template.");
		button.addActionListener(this);
		button.putClientProperty("NAME", "BUTTON_TEMPLATE");
		add(button, "wrap 30");

		add( new MultiLineLabel(WIZ_HELP2), "wmin 100, span, pushx, growx, wrap" );
		add(_shortDesc_lbl);
		add(_shortDesc_txt, "growx, pushx, wrap 30");

		add( new MultiLineLabel(WIZ_HELP3), "wmin 100, span, pushx, growx, wrap" );
		add(_longDesc_lbl);
		add(_longDesc_txt, "growx, pushx, wrap 30");

		initData();
	}

	private void initData()
	{
	}

	/** Called when we enter the page */
	@Override
	protected void renderingPage()
    {
		if (_firtsTimeRender)
		{
		}
	    _firtsTimeRender = false;
    }

	@Override
	protected String validateContents(Component comp, Object event)
	{
		// Check if we already have a CM with that name
		String cmName = _name_txt.getText().trim();
		CountersModel cm = GetCounters.getCmByName(cmName);
		if (cm != null)
		{
			return "There is alraedy a CM named '"+cmName+"'.";
		}

		String cmDesc = _shortDesc_txt.getText().trim();
		cm = GetCounters.getCmByDisplayName(cmDesc);
		if (cm != null)
		{
			return "The Short Description '"+cmDesc+"' is already used.";
		}

		String problem = "";
		if ( _name_txt     .getText().trim().length() <= 0) problem += "Name, ";
		if ( _shortDesc_txt.getText().trim().length() <= 0) problem += "Short Desc, ";
		if ( _longDesc_txt .getText().trim().length() <= 0) problem += "Long Desc, ";

		
		if (problem.length() > 0)
		{
			// Discard last ', '
			problem = problem.substring(0, problem.length()-2);
		}

		if (problem.length() > 0)
			problem = "Following fields cant be empty: " + problem;

		if ( _name_txt.getText().trim().indexOf(" ") >= 0) 
			problem = "The field 'Name' cant contain spaces.";

		if (    _name_txt.getText().trim().length() > 0 
		     && ! CountersModel.isValidCmName(_name_txt.getText()) ) 
			problem = CountersModel.checkForValidCmName(_name_txt.getText());

		if (problem.length() > 0)
		{
			return problem;
		}
		
		return null;
	}

	public void actionPerformed(ActionEvent ae)
	{
		JComponent src = (JComponent) ae.getSource();
		String name = (String)src.getClientProperty("NAME");
		if (name == null)
			name = "-null-";

//		System.out.println("Source("+name+"): " + src);
		
		if (name.equals("BUTTON_TEMPLATE"))
		{
			CmChooserDialog cmChooser = new CmChooserDialog(null);
			String cmName = cmChooser.chooseOne();
			if (cmName != null)
			{
				putWizardData("cmTemplate", cmName);
				CountersModel cm = GetCounters.getCmByName(cmName);
				if (cm != null)
				{
					_name_txt     .setText(cm.getName()        + "Copy");
					_shortDesc_txt.setText(cm.getDisplayName() + " Copy");
					_longDesc_txt .setText(cm.getDescription());
					
					_longDesc_txt.setCaretPosition(0);
				}
				else
					_name_txt.setText("NoLuck-"+cmName);
			}
		}
	}

	
	/*---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	**---- SUBCLASSES ---- SUBCLASES ---- SUBCLASES ----- 
	**---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	*/
	protected class CmChooserDialog
	extends JDialog
	implements ActionListener
	{
        private static final long serialVersionUID = 1L;

        private final String[] TAB_HEADER = {"Choose", "Cm", "Name", "Short Desc", "Long Description"};
		private static final int TAB_POS_CHECK          = 0;
		private static final int TAB_POS_COL_ICON       = 1;
		private static final int TAB_POS_COL_NAME       = 2;
		private static final int TAB_POS_COL_SHORT_DESC = 3;
		private static final int TAB_POS_COL_LONG_DESC  = 4;

		private JXTable _table                = new JXTable();
		private JButton _tableSelectAll_but   = new JButton("Select All");
		private JButton _tableDeSelectAll_but = new JButton("Deselect All");

		private JButton _ok                   = new JButton("OK");
		private JButton _cancel               = new JButton("Cancel");

		/**
		 * Choose one of the CM's in the GUI
		 * @return String with a name of the chosen one. null if none
		 */
		public String chooseOne()
		{
			_tableSelectAll_but.setVisible(false);
			_tableDeSelectAll_but.setVisible(false);
			_table.removeColumn(_table.getColumn(TAB_POS_CHECK));
			
			// On double click... close the window
			_table.addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
					{
						setVisible(false);
					}
				}
			});

			setVisible(true);

			int row = _table.getSelectedRow();
			if (row == -1)
				return null;

			row = _table.convertRowIndexToModel(row);
			TableModel tm = _table.getModel();
			return (String) tm.getValueAt(row, TAB_POS_COL_NAME);
		}

		/**
		 * Choose one or several of the CM's in the GUI
		 * @return List of Strings with a name of the chosen one. null if none
		 */
		public List<String> chooseList()
		{
			setVisible(true);
			List<String> list = new LinkedList<String>();

			TableModel tm = _table.getModel();
			for (int r=0; r<tm.getRowCount(); r++)
			{
				if (((Boolean)tm.getValueAt(r, TAB_POS_CHECK)).booleanValue())
				{
					list.add( (String) tm.getValueAt(r, TAB_POS_COL_NAME) );
				}
			}
			if (list.size() > 0)
				return list;
			return null;
		}

		public CmChooserDialog(Frame owner)
		{
			super(owner, "Choose a Counter Model", true);

			initComponents();
			pack();
			
			Dimension size = getPreferredSize();
			size.height = 500;
			size.width  = 500;

			setSize(size);

			setLocationRelativeTo(owner);
		}
		
		protected void initComponents() 
		{
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout("insets 0 0 0 0, wrap 1", "", ""));   // insets Top Left Bottom Right

			panel.add(createCmTablePanel(),  "height 100%, grow, push, wrap");
			panel.add(createOkCancelPanel(), "shrinkprio 0, bottom, right");

			setContentPane(panel);
		}

		private JPanel createOkCancelPanel()
		{
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

			// ADD the OK, Cancel, Apply buttons
			panel.add(_ok,     "tag ok, right");
			panel.add(_cancel, "tag cancel");

			// ADD ACTIONS TO COMPONENTS
			_ok    .addActionListener(this);
			_cancel.addActionListener(this);

			return panel;
		}

		private JPanel createCmTablePanel()
		{
			JPanel panel = SwingUtils.createPanel("Available Counter Models", false);
			panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right
	
			// Create a TABLE
			Vector<String> tabHead = new Vector<String>();
			tabHead.add(TAB_HEADER[TAB_POS_CHECK]);
			tabHead.add(TAB_HEADER[TAB_POS_COL_ICON]);
			tabHead.add(TAB_HEADER[TAB_POS_COL_NAME]);
			tabHead.add(TAB_HEADER[TAB_POS_COL_SHORT_DESC]);
			tabHead.add(TAB_HEADER[TAB_POS_COL_LONG_DESC]);

			Vector<Vector<Object>> tabData = populateCmTable();
	
			DefaultTableModel defaultTabModel = new DefaultTableModel(tabData, tabHead)
			{
	            private static final long serialVersionUID = 1L;
	
				public Class<?> getColumnClass(int column) 
				{
					if (column == TAB_POS_CHECK)    return Boolean.class;
					if (column == TAB_POS_COL_ICON)	return Icon.class;
					return Object.class;
				}
				public boolean isCellEditable(int row, int column)
				{
					return column == TAB_POS_CHECK;
				}
			};
	
			// On double click... toogle the choosen box
			_table.addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
					{
						int row = _table.getSelectedRow();
						if (row == -1)
							return;

						row = _table.convertRowIndexToModel(row);
						TableModel tm = _table.getModel();
						boolean choosen = ((Boolean)tm.getValueAt(row, TAB_POS_CHECK)).booleanValue();
						tm.setValueAt(new Boolean(!choosen), row, TAB_POS_CHECK);
					}
				}
			});

			_table.setModel( defaultTabModel );
			_table.setShowGrid(false);
			_table.setSortable(true);
			_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			_table.packAll(); // set size so that all content in all cells are visible
			_table.setSortable(true);
			_table.setColumnControlVisible(true);
	
			JScrollPane jScrollPane = new JScrollPane();
			jScrollPane.setViewportView(_table);
			panel.add(jScrollPane, "push, grow, height 100%, wrap");
	
			panel.add(_tableSelectAll_but,   "hidemode 3, split");
			panel.add(_tableDeSelectAll_but, "hidemode 3");
	
			// ADD ACTION LISTENERS
			_tableSelectAll_but  .addActionListener(this);
			_tableDeSelectAll_but.addActionListener(this);
	
			return panel;
		}

		private Vector<Vector<Object>> populateCmTable()
		{
			Vector<Vector<Object>> tab = new Vector<Vector<Object>>();
			Vector<Object>         row = new Vector<Object>();

			for (CountersModel cm : GetCounters.getCmList())
			{
				if (cm != null)
				{
					row = new Vector<Object>();
					row.add(new Boolean( false ));
					row.add(cm.getTabPanel() == null ? null : cm.getTabPanel().getIcon());
					row.add(cm.getName());
					row.add(cm.getDisplayName());
					row.add(cm.getDescription().replaceAll("\\<.*?\\>", "")); // STRIP HTML Tags from the description.
					tab.add(row);
					
					if (cm.getName().equals(SummaryPanel.CM_NAME))
					{
						row.set(1, SwingUtils.readImageIcon(Version.class, "images/summary_tab.png"));
					}
				}
			}
			if (tab.size() == 0)
			{
				for (int i=0; i<40; i++)
				{
					row = new Vector<Object>();
					row.add(new Boolean(false));
					row.add( SwingUtils.readImageIcon(Version.class, "images/ud_counter_activity.png") );
					row.add("cmDummy"+i);
					row.add("Dummy Tab "+i);
					row.add( UUID.randomUUID().toString() + " : " + UUID.randomUUID().toString());
					tab.add(row);
				}			
			}

			return tab;
		}
		public void actionPerformed(ActionEvent e)
		{
			Object source = e.getSource();

			// --- PCS: BUTTON: "Select All" 
			if (_tableSelectAll_but.equals(source))
			{
				TableModel tm = _table.getModel();
				for (int r=0; r<tm.getRowCount(); r++)
				{
					tm.setValueAt(new Boolean(true), r, TAB_POS_CHECK);
				}
			}

			// --- PCS: BUTTON: "DeSelect All" 
			if (_tableDeSelectAll_but.equals(source))
			{
				TableModel tm = _table.getModel();
				for (int r=0; r<tm.getRowCount(); r++)
				{
					tm.setValueAt(new Boolean(false), r, TAB_POS_CHECK);
				}
			}

			// --- BUTTON: CANCEL ---
			if (_cancel.equals(source))
			{
//				_completeType = CANCEL;
				setVisible(false);
			}

			// --- BUTTON: OK ---
			if (_ok.equals(source))
			{
//				_completeType = ONE;
//				_completeType = SEVERAL;
				setVisible(false);
			}
		}
		
		/*---------------------------------------------------
		** END: implementing ActionListener, KeyListeners
		**---------------------------------------------------
		*/
	}

}


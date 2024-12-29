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
package com.dbxtune.cm.os.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.dbxtune.Version;
import com.dbxtune.gui.swing.GTable;
import com.dbxtune.gui.swing.GTableFilter;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class IoStatDeviceMapperDialog
//extends JFrame
extends JDialog
implements ActionListener, FocusListener //, ChangeListener
{
	private static Logger _logger = Logger.getLogger(IoStatDeviceMapperDialog.class);
	private static final long serialVersionUID = 1L;

//	private JSplitPane           _splitPane              = new JSplitPane();

	private JButton              _ok                     = new JButton("OK");
	private JButton              _cancel                 = new JButton("Cancel");
	private JButton              _apply                  = new JButton("Apply");

	private JPanel               _topPanel               = null;
//	private JPanel               _okPanel                = null;

	private JPanel               _cmdPanel               = null;

	private JButton              _addCmd_but             = new JButton("Add");
	private JButton              _changeCmd_but          = new JButton("Change");
	private JButton              _removeCmd_but          = new JButton("Remove");
	private JButton              _cloneCmd_but           = new JButton("Clone");

	private MappingTableModel    _tm                     = null;
	private GTable               _table                  = null;
	private GTableFilter         _tableFilter            = null;
	private JCheckBox            _filterOnCurrentHost_chk= new JCheckBox("Show only hostname for:");
	private JLabel               _filterOnCurrentHost_lbl= new JLabel("");

	private Window               _parentWindow           = null;
	
	private Set<String>          _inputDevicesSet        = null;
	private String               _inputHostName          = null;

	public static final String PROPKEY_DEVICE_PREFIX        = "iostat.device.";
	public static final String PROPKEY_DEVICE_MAPPING_START = "iostat.device.mapping.";
	public static final String PROPKEY_DEVICE_HIDDEN_START  = "iostat.device.hidden.";

	private final static Color FIXME_COLOR = new Color(255, 216, 204); // Very light "orange/pink"

	public IoStatDeviceMapperDialog(Window window)
	{
		this(window, null, null, null);
	}
	public IoStatDeviceMapperDialog(Window window, String forHostName, Set<String> devices, Map<String, String> dmToNameMap)
	{
		super(window, "Device Mapping", ModalityType.MODELESS);
//		super(window, "Favorite Commands", ModalityType.APPLICATION_MODAL);
//		super();
		_parentWindow = window;
		
		_inputHostName   = forHostName;
		_inputDevicesSet = devices;
		
		init();
		_tm.setChanged(false);
		
		addNewDevicesFromInput(forHostName, devices, dmToNameMap);
	}
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	// GUI initialization code
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	private void init()
	{
		setTitle("Device Mapping"); // Set window title
		
		// Set the icon, if we "just" do setIconImage() on the JDialog
		// it will not be the "correct" icon in the Alt-Tab list on Windows
		// So we need to grab the owner, and set that since the icon is grabbed from the owner...
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/device_mapping.png");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/device_mapping_32.png");
		if (icon16 != null || icon32 != null)
		{
			ArrayList<Image> iconList = new ArrayList<Image>();
			if (icon16 != null) iconList.add(icon16.getImage());
			if (icon32 != null) iconList.add(icon32.getImage());

//			Object owner = getOwner();
//			if (owner != null && owner instanceof Frame)
//				((Frame)owner).setIconImages(iconList);
//			else
				setIconImages(iconList);
		}

		setLayout( new BorderLayout() );
		
		loadProps();

		_topPanel     = createTopPanel();
		_cmdPanel     = createCmdPanel();
//		_okPanel      = createOkCancelPanel();

		add(_topPanel, BorderLayout.CENTER);
		add(_cmdPanel, BorderLayout.SOUTH);
//		_splitPane.setDividerLocation(300);
//		_splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
//		_splitPane.setTopComponent(_topPanel);
//		_splitPane.setBottomComponent(_cmdPanel);
//		add(_splitPane, BorderLayout.CENTER);

		// Set the filter if we have a hostname, but do it after the gui is displaied
		if (StringUtil.hasValue(_inputHostName))
		{
			SwingUtilities.invokeLater( new Runnable()
			{
				@Override
				public void run()
				{
					setFilterOnCurrentHost(true);
				}
			});
		}

		pack();
		getSavedWindowProps();
		setComponentVisibility();
	}


	private JPanel createTopPanel()
	{
		JPanel panel = SwingUtils.createPanel("Top Panel", false);
//		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		panel.setLayout(new MigLayout());

//		panel.setToolTipText("<html></html>");

		String htmlMsg = 
				"<html>"
				+ "Set a description for each of the devices so it's easier to see what they are dedicated to.<br>"
				+ "Or hide the devices that you <b>know</b> are not used for the database.<br>"
				+ "<br>"
				+ "To change a description: Simply <i>click</i> in the <i>Description</i> field and enter a good description, or press the <i>change</i> button, or use the right click menu.<br>"
				+ "<br>"
				+ "Devices with a background color has not yet been <i>descibed</i> by you.<br>"
				+ "In Linux the devices <i>sda</i> and <i>sdb</i> is normally OS disk...<br>"
				+ "In Linux the devices <i>dm-#</i> you may get a better name from: ls -Fal /dev/mapper/<br>"
				+ "<b>Tip</b>: Use 'Show only hostname for' to display the entries connected to <i>current</i> hostname.<br>"
				+ "<html>";
		
		JLabel info = new JLabel(htmlMsg);
		
		panel.add(info,          "push, grow, wrap");
		panel.add(createTable(), "push, grow, wrap");

		return panel;
	}

	private JPanel createTable()
	{
		JPanel panel = SwingUtils.createPanel("Device Mappings", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

//		_table = new JXTable()
		_table = new GTable()
		{
			private static final long serialVersionUID = 1L;

			// anonymous initializer 
			{
				setName("iostatDeviceDescTable");
			}

			// 
			// TOOL TIP for: TABLE HEADERS
			//
			@Override
			protected JXTableHeader createDefaultTableHeader()
			{
				//TableColumnModel tcm = getColumnModel();
				JXTableHeader jxth = new JXTableHeader(columnModel)
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
						int mIndex = convertColumnIndexToModel(index);
						if ( mIndex < 0 )
							return null;
	
						// Now get the ToolTip from the CounterTableModel
						String toolTip = null;
						if (mIndex == MappingTableModel.TAB_POS_HOSTNAME) toolTip = "<html>For what hostname is this entry valid for.</html>";
						if (mIndex == MappingTableModel.TAB_POS_DEVICE)   toolTip = "<html>For what physical device name is this entry valid for.</html>";
						if (mIndex == MappingTableModel.TAB_POS_HIDDEN)   toolTip = "<html>Should we hide or display this physical device in the list.</html>";
						if (mIndex == MappingTableModel.TAB_POS_DESC)     toolTip = "<html>A more readable form of what this device is used for.</html>";
						return toolTip;
					}
				};
				return jxth;
			}

			// 
			// TOOL TIP for: CELLS
			//
//			@Override
//			public String getToolTipText(MouseEvent e) 
//			{
//				String tip = null;
//				Point p = e.getPoint();
////				int col = columnAtPoint(p);
//				int vcol = MappingTableModel.TAB_POS_COMMAND;
//				int vrow = rowAtPoint(p);
//				if (vcol >= 0 && vrow >= 0)
//				{
//					int mcol = MappingTableModel.TAB_POS_COMMAND; //super.convertColumnIndexToModel(vcol);
//					int mrow = super.convertRowIndexToModel(vrow);
//
//					TableModel tm = getModel();
//
//					tip = tm.getValueAt(mrow, mcol) + "";
//
//					if (tm instanceof MappingTableModel)
//					{
//						MappingTableModel ctm = (MappingTableModel) tm;
//						
//						String type   = ctm.getValueAt(mrow, MappingTableModel.TAB_POS_TYPE) + "";
//						String name   = ctm.getValueAt(mrow, MappingTableModel.TAB_POS_NAME) + "";
//						String desc   = ctm.getValueAt(mrow, MappingTableModel.TAB_POS_DESC) + "";
//						String icon   = ctm.getValueAt(mrow, MappingTableModel.TAB_POS_ICON) + "";
//						String cmd    = ctm.getCommand(mrow);
////						private static final String[] TAB_HEADER = {"Row", "Time", "Server", "User", "Db", "Source", "#", "Command"};
//
//						tip = "<html>" +
//						      "<table align=\"left\" border=0 cellspacing=0 cellpadding=0>" +
//						        "<tr> <td><b>Type:        </b></td> <td>" + type + "</td> </tr>" +
//						        "<tr> <td><b>Name:        </b></td> <td>" + name + "</td> </tr>" +
//						        "<tr> <td><b>Description: </b></td> <td>" + desc + "</td> </tr>" +
//						        "<tr> <td><b>Icon:        </b></td> <td>" + icon + "</td> </tr>" +
//						      "</table>" +
//						      "<hr>" +
//						      "<pre>"+cmd+"</pre>" +
//						      "</html>";
//					}
//				}
//				return tip;
//			}
		};

		// Double-Click on a row
		_table.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e) 
			{
				if (e.getClickCount() == 2)
				{
					Point p = e.getPoint();
					int vrow = _table.rowAtPoint(p);
					int mrow = _table.convertRowIndexToModel(vrow);
					
					MappingEntry entry = _tm.getEntry(mrow);

					// Open the editor
					MappingEntry e2 = AddOrChangeEntryDialog.showDialog(IoStatDeviceMapperDialog.this, entry, AddOrChangeEntryDialog.CHANGE_DIALOG);
					if (e2 != null)
					{
						_tm.setChanged(true);
						_tm.fireTableDataChanged();
					}
				}
			}
		});

		// Selection listener
		_table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
//				int vrow = _table.getSelectedRow();
//				if (vrow == -1)
//					return;
//				int vcol = MappingTableModel.TAB_POS_COMMAND;
//	
//				if (vcol >= 0 && vrow >= 0)
//				{
////					int mcol = MappingTableModel.TAB_POS_COMMAND; //_table.convertColumnIndexToModel(col);
//					int mrow = _table.convertRowIndexToModel(vrow);
//
//					TableModel tm = _table.getModel();
//
//					String type = tm.getValueAt(mrow, MappingTableModel.TAB_POS_TYPE)    + "";
//					String name = tm.getValueAt(mrow, MappingTableModel.TAB_POS_NAME)    + "";
//					String desc = tm.getValueAt(mrow, MappingTableModel.TAB_POS_DESC)    + "";
//					String icon = tm.getValueAt(mrow, MappingTableModel.TAB_POS_ICON)    + "";
//					String cmd  = tm.getValueAt(mrow, MappingTableModel.TAB_POS_COMMAND) + "";
//					if (tm instanceof MappingTableModel)
//					{
//						MappingTableModel ctm = (MappingTableModel) tm;
//						cmd = ctm.getCommand(mrow);
//					}
//
//					_favoriteType_txt.setText(type);
//					_favoriteName_txt.setText(name);
//					_favoriteDesc_txt.setText(desc);
//					_favoriteIcon_txt.setText(icon);
//					
//					setBasicInfo(_preview2_lbl, name, desc, icon, cmd);
//
//					_cmd_txt.setText(cmd);
//					_cmd_txt.setCaretPosition(0);
//				}
				
				setComponentVisibility();
			}
		});

		// POPUP Menu
		//FIXME: create a popup menu
//		_panelPopupMenu = createPanelPopupMenu();
//		_panel.setComponentPopupMenu(_panelPopupMenu);
		_table.setComponentPopupMenu(createPanelPopupMenu());

		
		String actionName = null;
		KeyStroke key     = null;
		//---------------------------------------------------
		// In the JTable: on ENTER, execute the current query
//		actionName = "TableEnterAction";
//		key        = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
//		_table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, actionName);
//		_table.getActionMap().put(actionName, _executeHistoryRowsAction);

		//---------------------------------------------------
		// In the JTable: on DELETE, delete current row
		actionName = "TableDeleteRow";

		key = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
		_table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, actionName);

		key = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
		_table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, actionName);

		_table.getActionMap().put(actionName, _deleteSelectedRowsAction);

		//---------------------------------------------------
		// Ctrl-c: copy all selected rows
//		_table.getActionMap().put("copy", _copySelectedRowsAction);

		_tm = new MappingTableModel();
		_tm.populateTable();

//		_table.setName("FavoriteCommandsTable"); // GTable want's a name so it can remember column: sort and placement
		_table.setModel( _tm );
//		_table.setSortable(false);
		_table.setSortable(true);
		_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		_table.setColumnControlVisible(true);
		_table.packAll();

		JScrollPane jScrollPane = new JScrollPane();
		jScrollPane.setViewportView(_table);

		// NULL Values: SET BACKGROUND COLOR
		_table.addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String desc = adapter.getString(adapter.getColumnIndex("Description"));
				if ( desc != null && desc.startsWith("FIXME: "))
					return true;
				return false;
			}
		}, FIXME_COLOR, null));

		panel.add(jScrollPane, "span, push, grow, height 100%, wrap");
		// Focus action listener

		return panel;
	}

	private JPanel createInfoPanel()
	{
		JPanel panel = SwingUtils.createPanel("Ino Panel", false);
		panel.setLayout(new MigLayout());

//		panel.setToolTipText("<html></html>");

		_addCmd_but             .setToolTipText("<html>Add a new Mapping.</html>");
		_changeCmd_but          .setToolTipText("<html>Open the Edit Dialog where you can change the description.</html>");
		_removeCmd_but          .setToolTipText("<html>Remove the mapping.</html>");
		_cloneCmd_but           .setToolTipText("<html>Clone a pamming and open the dialog where you can change it.</html>");

		panel.add(_addCmd_but,          "span, split");  // gap left [right] [top] [bottom]
		panel.add(_changeCmd_but,       "");
		panel.add(_removeCmd_but,       "");
		panel.add(_cloneCmd_but,        "wrap");

		_tableFilter = new GTableFilter(_table);
		panel.add(_tableFilter,             "push, grow, wrap");
		panel.add(_filterOnCurrentHost_chk, "gapleft 32lp, span, split");
		panel.add(_filterOnCurrentHost_lbl, "wrap");

		_filterOnCurrentHost_lbl.setText(_inputHostName);
		
		// Focus action listener
		
		// action
		_addCmd_but      .addActionListener(this);
		_changeCmd_but   .addActionListener(this);
		_removeCmd_but   .addActionListener(this);
		_cloneCmd_but    .addActionListener(this);

		_filterOnCurrentHost_chk.addActionListener(this);

		// Action Commands
		_addCmd_but      .setAction(_addFavoriteAction);
		_changeCmd_but   .setAction(_changeFavoriteAction);
		_removeCmd_but   .setAction(_deleteSelectedRowsAction);
		_cloneCmd_but    .setAction(_cloneSelectedRowAction);
		
		return panel;
	}

	private JPanel createCmdPanel()
	{
		JPanel panel = SwingUtils.createPanel("Commands", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		// Tooltip
//		panel   .setToolTipText("Details View of what this command does");
		
		panel.add(createInfoPanel(),      "span, growx, pushx, wrap");
		panel.add(createStatusBarPanel(), "growx, pushx");
		panel.add(createOkCancelPanel(),  "bottom, right, pushx");

		return panel;
	}

//	private static final String FAVORITE_FILE_TOOLTIP_TEMPLATE = "<html>Set a new Favorite Commands File to use.<br>Current file: <code> FAVORITE_FILE_NAME </code></html>"; 

	private JPanel createStatusBarPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		
//		_favoriteFile_lbl.setToolTipText("<html>Favorite Commands File used for the moment. Press <i>button on the left</i> to change Favorite Commands File.</html>");
//		_favoriteFile_but.setToolTipText(FAVORITE_FILE_TOOLTIP_TEMPLATE);
//
//		// History file button
//		_favoriteFile_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/favorite_file.png"));
//		_favoriteFile_but.setText(null);
//		_favoriteFile_but.setContentAreaFilled(false);
//		_favoriteFile_but.setMargin( new Insets(0,0,0,0) );
//
//		panel.add(_favoriteFile_but, "");
//		panel.add(_favoriteFile_lbl, "pushx, growx");
//		
//		_favoriteFile_but.addActionListener(this);

		return panel;
	}

	private JPanel createOkCancelPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

		// ADD the OK, Cancel, Apply buttons
		panel.add(_ok,     "tag ok, right");
		panel.add(_cancel, "tag cancel");
		panel.add(_apply,  "tag apply");

		_apply.setEnabled(false);

		// ADD ACTIONS TO COMPONENTS
		_ok           .addActionListener(this);
		_cancel       .addActionListener(this);
		_apply        .addActionListener(this);

		return panel;
	}

	/**
	 * Creates the JMenu on the Component, this can be override by a subclass.
	 * <p>
	 * If you want to add stuff to the menu, its better to use
	 * getPanelPopupMenu(), then add entries to the menu. This is much better than subclass this Class
	 */
	public JPopupMenu createPanelPopupMenu()
	{
		_logger.debug("createDataTablePopupMenu(): called.");

		JPopupMenu popup = new JPopupMenu();
		for (JComponent comp : createPopupMenuComponents())
			popup.add(comp);
		
		if ( popup.getComponentCount() == 0 )
		{
//			_logger.warn("No PopupMenu has been assigned for the Graph in the panel '" + _graphName + "'.");
			return null;
		}
		else
			return popup;
	}
	/**
	 * Create the Menu components that can be used my a JMenu or a JPopupMenu
	 * @return a List of Menu components (null is never returned, instead a empty List)
	 */
	private List<JComponent> createPopupMenuComponents()
	{
		ArrayList<JComponent> list = new ArrayList<JComponent>();
		JMenuItem  mi = null;

		//------------------------------------------------------------
		mi = new JMenuItem("Add");
		mi.setAction(_addFavoriteAction);
		list.add(mi);

		//------------------------------------------------------------
		mi = new JMenuItem("Change");
		mi.setAction(_changeFavoriteAction);
		list.add(mi);

		//------------------------------------------------------------
		mi = new JMenuItem("Delete");
		mi.setAction(_deleteSelectedRowsAction);
		list.add(mi);

		//------------------------------------------------------------
		mi = new JMenuItem("Clone");
		mi.setAction(_cloneSelectedRowAction);
		list.add(mi);

		return list;
	}

	private void doApply()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
			return;

		// Remove all entries from the configuration
		conf.removeAll(PROPKEY_DEVICE_PREFIX);

		for (MappingEntry entry : _tm.getEntries())
		{
			setDescription(entry.getHostName(), entry.getDeviceName(), entry.getDescription());
			setHidden     (entry.getHostName(), entry.getDeviceName(), entry.isHidden());
		}
		conf.save();
		
		_tm.setChanged(false);
		_apply.setEnabled(false);
	}
	
	public void setFilterOnCurrentHost(boolean enable)
	{
		_filterOnCurrentHost_chk.setSelected(enable);
		
		if (_filterOnCurrentHost_chk.isSelected())
			_tableFilter.setFilterText("where Hostname='"+_inputHostName+"'");
		else
			_tableFilter.setFilterText("");
	}
	/*---------------------------------------------------
	** BEGIN: implementing: ChangeListener, ActionListener, FocusListener, FileTail.TraceListener, Memory.MemoryListener
	**---------------------------------------------------
	*/	

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source    = e.getSource();
		//String actionCmd = e.getActionCommand();
		
		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			setVisible(false);
		}

		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			doApply();
			saveProps();
			setVisible(false);
		}

		// --- BUTTON: APPLY ---
		if (_apply.equals(source))
		{
			doApply();
			saveProps();
		}

		if (_filterOnCurrentHost_chk.equals(source) && _tableFilter != null)
		{
			setFilterOnCurrentHost(_filterOnCurrentHost_chk.isSelected());
//			if (_filterOnCurrentHost_chk.isSelected())
//				_tableFilter.setFilterText("where Hostname='"+_inputHostName+"'");
//			else
//				_tableFilter.setFilterText("");
		}

		if (isVisible())
		{
			setComponentVisibility();
			saveProps();
		}
	}
	@Override
	public void focusGained(FocusEvent e)
	{
	}
	@Override
	public void focusLost(FocusEvent e)
	{
	}

	private void setComponentVisibility()
	{
//		boolean visible = _table.getRowCount() > 0;
		boolean visible = _table.getSelectedRowCount() > 0;
		boolean enabled = visible;

		// Set what should be enabled/disabled
//		_addCmd_but      .setEnabled(enabled);
		_addCmd_but      .setEnabled(true);
		_changeCmd_but   .setEnabled(enabled);
		_removeCmd_but   .setEnabled(enabled);
		_cloneCmd_but    .setEnabled(enabled);

		_changeFavoriteAction      .setEnabled(enabled);
		_deleteSelectedRowsAction  .setEnabled(enabled);
		_cloneSelectedRowAction    .setEnabled(enabled);

		_apply.setEnabled(_tm.isChanged());
	}

	/*---------------------------------------------------
	** BEGIN: implementing saveProps & loadProps
	**---------------------------------------------------
	*/	
	private void saveProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}

		//------------------
		// XXX
		//------------------
//		conf.setProperty(PROPKEY_MOVE_TO_LATEST_ENTRY,     _moveToLatestEntry_cbx.isSelected());
//		conf.setProperty(PROPKEY_HISTORY_SIZE,             _historySize_spm      .getNumber().intValue());
//		conf.setProperty(PROPKEY_SHOW_ONLY_LOCAL_COMMANDS, _showOnlyLocalCmds_cbx.isSelected());
		
		
		//------------------
		// SPLIT PANE
		//------------------
//		conf.setLayoutProperty(PROPKEY_SPLITPANE_DIV_LOC, _splitPane.getDividerLocation());

		
		//------------------
		// WINDOW
		//------------------
		conf.setLayoutProperty("IoStatDeviceMapperDialog.window.width",  this.getSize().width);
		conf.setLayoutProperty("IoStatDeviceMapperDialog.window.height", this.getSize().height);
		conf.setLayoutProperty("IoStatDeviceMapperDialog.window.pos.x",  this.getLocationOnScreen().x);
		conf.setLayoutProperty("IoStatDeviceMapperDialog.window.pos.y",  this.getLocationOnScreen().y);

		conf.save();
	}

	private void loadProps()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}

		//------------------
		// XXX
		//------------------
//		_moveToLatestEntry_cbx.setSelected( conf.getBooleanProperty(PROPKEY_MOVE_TO_LATEST_ENTRY,     DEFAULT_MOVE_TO_LATEST_ENTRY));
//		_historySize_spm      .setValue(    conf.getIntProperty(    PROPKEY_HISTORY_SIZE,             DEFAULT_HISTORY_SIZE));
//		_showOnlyLocalCmds_cbx.setSelected( conf.getBooleanProperty(PROPKEY_SHOW_ONLY_LOCAL_COMMANDS, DEFAULT_SHOW_ONLY_LOCAL_COMMANDS));

//		_historySize_last   = ((Number)_historySize_spm.getValue()).intValue();
	}

	private void getSavedWindowProps()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}
		//----------------------------------
		// TAB: Offline
		//----------------------------------
		int width  = conf.getLayoutProperty("IoStatDeviceMapperDialog.window.width",  SwingUtils.hiDpiScale(470));
		int height = conf.getLayoutProperty("IoStatDeviceMapperDialog.window.height", SwingUtils.hiDpiScale(660));
		int x      = conf.getLayoutProperty("IoStatDeviceMapperDialog.window.pos.x",  -1);
		int y      = conf.getLayoutProperty("IoStatDeviceMapperDialog.window.pos.y",  -1);

		if (width != -1 && height != -1)
			this.setSize(width, height);

		if (x == -1 && y == -1)
			SwingUtils.setLocationCenterParentWindow(_parentWindow, this);

		if (x != -1 && y != -1)
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
				this.setLocation(x, y);

		//------------------
		// SPLIT PANE
		//------------------
//		int dividerLocation = conf.getLayoutProperty(PROPKEY_SPLITPANE_DIV_LOC, DEFAULT_SPLITPANE_DIV_LOC);
//		_splitPane.setDividerLocation(dividerLocation);

	}
	/*---------------------------------------------------
	** END: implementing saveProps & loadProps
	**---------------------------------------------------
	*/	
	
	/**
	 * Add devices past when opening the window
	 * @param forHostName
	 * @param devices
	 * @param dmToNameMap 
	 */
	public void addNewDevicesFromInput(String forHostName, Set<String> devices, Map<String, String> dmToNameMap)
	{
		// CHeck input
		if (StringUtil.isNullOrBlank(forHostName)) return;
		if (devices == null)                       return;
		if (devices.size() == 0)                   return;

		// Add devices that NOT already exists to a new Set
		Set<String> newDevices = new LinkedHashSet<String>();
		for (String device : devices)
			if ( ! exists(forHostName, device) )
				newDevices.add(device);

		// No new devices; so no need to continue
		if (newDevices.size() == 0)
			return;
		
		// Ask question if we want to "auto add" the list
		String msgHtml = "<html><h3>The following devices can be added.</h3><br><table border=1 cellspacing=0 cellpadding=1> <tr> <th>Hostname</th> <th>Device Name</th> </tr>";
		for (String device : newDevices)
			msgHtml += "<tr> <td>"+forHostName+"</td> <td>"+device+"</td> </tr>";
		msgHtml += "</table></html>";
		
		Object[] options = {
				"Yes, Add them",
				"No, I'll add them manually"
				};
		int answer = JOptionPane.showOptionDialog(this, 
			msgHtml,
			"Add devices?", // title
			JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null,     //do not use a custom Icon
			options,  //the titles of buttons
			options[0]); //default button title

		// YES, Add them
		if (answer == 0)
		{
			for (String device : newDevices)
			{
				if (device.startsWith("sd") || device.startsWith("hd"))
					addEntry(forHostName, device, false, "FIXME: Local Disk");
				else
				{
					String desc = "FIXME: Describe this device";
					if (dmToNameMap != null && dmToNameMap.containsKey(device))
						desc = "FIXME: " + dmToNameMap.get(device);
					addEntry(forHostName, device, false, desc);
				}
			}
		}
	}
	
	public boolean exists(String hostName, String device)
	{
		for (int r=0; r<_tm.getRowCount(); r++)
		{
			MappingEntry entry = _tm.getEntry(r);
			if (entry.getHostName().equals(hostName) && entry.getDeviceName().equals(device))
				return true;
		}
		return false;
	}

	/**
	 * Get all Favorite Commands
	 * @return a list of entries
	 */
	public ArrayList<MappingEntry> getEntries()
	{
		return _tm.getEntries();
	}
	/**
	 * Add entry 
	 */
	public void addEntry(String hostName, String deviceName, boolean hidden, String description)
	{
		addEntry(-1, new MappingEntry(hostName, deviceName, hidden, description));
	}

	/**
	 * Internally called from the tail reader 
	 * @param pos -1 = At the end
	 * @param entry
	 */
	private void addEntry(int pos, MappingEntry entry)
	{
		// Add entry
		_tm.addEntry(entry);

		if ( _defferedEntryAdd.isRunning() )
			_defferedEntryAdd.restart();
		else
			_defferedEntryAdd.start();
	}

	Timer _defferedEntryAdd = new Timer(100, new DefferedEntryAddTimer());

	private class DefferedEntryAddTimer
	implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			// Make the view aware of the change.
			_tm.fireTableDataChanged();

			_table.packAll();
	
			setComponentVisibility();
	
			_defferedEntryAdd.stop();
		}
	}


	//-------------------------------------------------------------------
	// Local ACTION classes
	//-------------------------------------------------------------------
	private AddFavoriteAction          _addFavoriteAction          = new AddFavoriteAction();
	private ChangeFavoriteAction       _changeFavoriteAction       = new ChangeFavoriteAction();
	private DeleteSelectedRowsAction   _deleteSelectedRowsAction   = new DeleteSelectedRowsAction();
	private CloneSelectedRowAction     _cloneSelectedRowAction     = new CloneSelectedRowAction();

	private class AddFavoriteAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Add";
		private static final String ICON = "images/device_mapping_add.png";

		public AddFavoriteAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			MappingEntry entry = AddOrChangeEntryDialog.showDialog(IoStatDeviceMapperDialog.this, null, AddOrChangeEntryDialog.ADD_DIALOG);
			if (entry != null)
				addEntry(-1, entry);
		}
	}


	private class ChangeFavoriteAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Change";
		private static final String ICON = "images/device_mapping_change.png";

		public ChangeFavoriteAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			int selRow = _table.getSelectedRow();
			if (selRow >= 0)
			{
				int mrow = _table.convertRowIndexToModel(selRow);

				MappingEntry entry = _tm.getEntry(mrow);
				MappingEntry e2 = AddOrChangeEntryDialog.showDialog(IoStatDeviceMapperDialog.this, entry, AddOrChangeEntryDialog.CHANGE_DIALOG);
				if (e2 != null)
				{
					_tm.setChanged(true);
					_tm.fireTableDataChanged();
				}
			}
			else
			{
				SwingUtils.showInfoMessage(IoStatDeviceMapperDialog.this, "Select a entry", "No entry is selected");
			}
		}
	}


	private class DeleteSelectedRowsAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Delete";
		private static final String ICON = "images/device_mapping_delete.png";
//		private static final ImageIcon ICON = SwingUtils.readImageIcon(Version.class, "images/delete.png");

		public DeleteSelectedRowsAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			int[] vrows = _table.getSelectedRows();
			int[] mrows = new int[vrows.length];
			for (int r=0; r<vrows.length; r++)
				mrows[r] = _table.convertRowIndexToModel(vrows[r]);

			TableModel tm = _table.getModel();
			if (tm instanceof MappingTableModel)
			{
				MappingTableModel ctm = (MappingTableModel) tm;
				
				// Delete all rows, but start at the end of the model
				for (int r=mrows.length-1; r>=0; r--)
					ctm.deleteEntry(mrows[r]);

				setComponentVisibility();
			}
		}
	}


	private class CloneSelectedRowAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Clone";
		private static final String ICON = "images/device_mapping_clone.png";

		public CloneSelectedRowAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			int selRow = _table.getSelectedRow();
			if (selRow >= 0)
			{
				int mrow = _table.convertRowIndexToModel(selRow);

				MappingEntry entry = _tm.getEntry(mrow);
				MappingEntry e2 = AddOrChangeEntryDialog.showDialog(IoStatDeviceMapperDialog.this, entry, AddOrChangeEntryDialog.CLONE_DIALOG);
				if (e2 != null)
				{
					_tm.addEntry(e2);
					_tm.setChanged(true);
					_tm.fireTableDataChanged();
				}
			}
			else
			{
				SwingUtils.showInfoMessage(IoStatDeviceMapperDialog.this, "Select a entry", "No entry is selected");
			}
		}
	}


	//-------------------------------------------------------------------
	// Local private classes
	//-------------------------------------------------------------------
	public static class MappingEntry
	{
		private String     _hostName    = null;
		private String     _deviceName  = null;
		private boolean    _hidden      = false;
		private String     _description = null;

		public MappingEntry()
		{
		}
		public MappingEntry(String hostName, String deviceName, boolean isHidden, String description)
		{
			setHostName(hostName);
			setDeviceName(deviceName);
			setHidden(isHidden);
			setDescription(description);
		}

		public String     getHostName()        { return _hostName    == null ? "" : _hostName;    }
		public String     getDeviceName()      { return _deviceName  == null ? "" : _deviceName;  }
		public boolean    isHidden()           { return _hidden; }
		public String     getDescription()     { return _description == null ? "" : _description; }

		public void setHostName(String hostName)
		{
			if (hostName == null)
				hostName = "";
			_hostName = hostName;
		}

		public void setDeviceName(String deviceName)
		{
			if (deviceName == null)
				deviceName = "";
			_deviceName = deviceName;
		}

		public void setHidden(boolean hidden)
		{
			_hidden = hidden;
		}

		public void setDescription(String description)
		{
			if (description == null)
				description = "";
			_description = description;
		}
	}

	private static class MappingTableModel
	extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;

		private static final String[] TAB_HEADER = {"Hostname", "Device", "Hidden", "Description"};
		private static final int TAB_POS_HOSTNAME = 0;
		private static final int TAB_POS_DEVICE   = 1;
		private static final int TAB_POS_HIDDEN   = 2;
		private static final int TAB_POS_DESC     = 3;

		private ArrayList<MappingEntry> _rows = new ArrayList<MappingEntry>();
		private boolean _hasChanged = false;

		public MappingTableModel()
		{
		}

		public boolean isChanged()
		{
			return _hasChanged;
		}

		public void setChanged(boolean changed)
		{
			_hasChanged = changed;
		}

//		public void clear(boolean fireChange)
//		{
//			_rows.clear();
//			setChanged(true);
//			if (fireChange)
//				fireTableDataChanged();
//		}

//		/**
//		 *  Moves one or more rows from the inclusive range <code>start</code> to 
//		 *  <code>end</code> to the <code>to</code> position in the model. 
//		 *  After the move, the row that was at index <code>start</code> 
//		 *  will be at index <code>to</code>. 
//		 *  This method will send a <code>tableChanged</code> notification
//		 *  message to all the listeners. <p>
//		 *
//		 *  <pre>
//		 *  Examples of moves:
//		 *  <p>
//		 *  1. moveRow(1,3,5);
//		 *          a|B|C|D|e|f|g|h|i|j|k   - before
//		 *          a|e|f|g|h|B|C|D|i|j|k   - after
//		 *  <p>
//		 *  2. moveRow(6,7,1);
//		 *          a|b|c|d|e|f|G|H|i|j|k   - before
//		 *          a|G|H|b|c|d|e|f|i|j|k   - after
//		 *  <p> 
//		 *  </pre>
//		 *
//		 * @param   start       the starting row index to be moved
//		 * @param   end         the ending row index to be moved
//		 * @param   to          the destination of the rows to be moved
//		 * @exception  ArrayIndexOutOfBoundsException  if any of the elements would be moved out of the table's range 
//		 * 
//		 */
//		// STOLEN FROM: DefaultTableModel
//		public void moveRow(int start, int end, int to) 
//		{ 
//			int shift = to - start; 
//			int first, last; 
//			if (shift < 0) { 
//				first = to; 
//				last = end; 
//			}
//			else { 
//				first = start; 
//				last = to + end - start;  
//			}
//			rotate(_rows, first, last + 1, shift); 
//
//			setChanged(true);
//			fireTableRowsUpdated(first, last);
//		}
//		// STOLEN FROM: DefaultTableModel
//		private static void rotate(List<MappingEntry> l, int a, int b, int shift)
//		{
//			int size = b - a;
//			int r = size - shift;
//			int g = gcd(size, r);
//			for (int i = 0; i < g; i++)
//			{
//				int to = i;
//				MappingEntry tmp = l.get(a + to);
//				for (int from = (to + r) % size; from != i; from = (to + r) % size)
//				{
//					l.set(a + to, l.get(a + from));
//					to = from;
//				}
//				l.set(a + to, tmp);
//			}
//		}
//		// STOLEN FROM: DefaultTableModel
//		private static int gcd(int i, int j)
//		{
//			return (j == 0) ? i : gcd(j, i % j);
//		}

		public void setEntries(ArrayList<MappingEntry> entries, boolean fireChange)
		{
			_rows = entries;
			setChanged(true);
			if (fireChange)
				fireTableDataChanged();
		}

		public MappingEntry getEntry(int mrow)
		{
			return _rows.get(mrow);
		}

		public ArrayList<MappingEntry> getEntries()
		{
			return _rows;
		}

		public void deleteEntry(int row)
		{
			_rows.remove(row);
			setChanged(true);
			fireTableDataChanged();
		}

		public void addEntry(MappingEntry entry)
		{
			_rows.add(entry);
			setChanged(true);
			fireTableDataChanged();
		}

		@Override
		public int getColumnCount() 
		{
			return TAB_HEADER.length;
		}

		@Override
		public String getColumnName(int column) 
		{
			switch (column)
			{
			case TAB_POS_HOSTNAME: return TAB_HEADER[TAB_POS_HOSTNAME];
			case TAB_POS_DEVICE:   return TAB_HEADER[TAB_POS_DEVICE];
			case TAB_POS_HIDDEN:   return TAB_HEADER[TAB_POS_HIDDEN];
			case TAB_POS_DESC:     return TAB_HEADER[TAB_POS_DESC];
			}
			return null;
		}

		@Override
		public int getRowCount()
		{
			return _rows.size();
		}

		@Override
		public Object getValueAt(int row, int column)
		{
			MappingEntry entry = _rows.get(row);
			switch (column)
			{
			case TAB_POS_HOSTNAME:  return entry.getHostName();
			case TAB_POS_DEVICE:    return entry.getDeviceName();
			case TAB_POS_HIDDEN:    return entry.isHidden();
			case TAB_POS_DESC:      return entry.getDescription();
			}
			return null;
		}

		@Override
		public void setValueAt(Object value, int row, int column)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("setValueAt(row="+row+", column="+column+", val='"+value+"', Objtype='"+value.getClass().getName()+"')");

			MappingEntry entry = _rows.get(row);
			switch (column)
			{
			case TAB_POS_HOSTNAME:  { entry.setHostName   ( (String)  value ); break; }
			case TAB_POS_DEVICE:    { entry.setDeviceName ( (String)  value ); break; }
			case TAB_POS_HIDDEN:    { entry.setHidden     ( (Boolean) value ); break; }
			case TAB_POS_DESC:      { entry.setDescription( (String)  value ); break; }
			}
			setChanged(true);
		}

		@Override
		public boolean isCellEditable(int row, int column)
		{
			switch (column)
			{
			case TAB_POS_HOSTNAME: return false;
			case TAB_POS_DEVICE:   return false;
			case TAB_POS_HIDDEN:   return true;
			case TAB_POS_DESC:     return true;
			}
			return false;
		}

		@Override
		public Class<?> getColumnClass(int column)
		{
			if (column == TAB_POS_HIDDEN) 
				return Boolean.class;

			return super.getColumnClass(column);
		}

		private void populateTable()
		{
			Configuration conf = Configuration.getCombinedConfiguration();
			
			for (String key : conf.getKeys(PROPKEY_DEVICE_PREFIX))
			{
				String val = conf.getProperty(key);
//System.out.println("populateTable(): key='"+key+"', val='"+val+"'.");
				if (key.startsWith(PROPKEY_DEVICE_MAPPING_START))
				{
					String sub = key.substring(PROPKEY_DEVICE_MAPPING_START.length());
					int lastDot = sub.lastIndexOf('.');
					
					String  hostName   = sub.substring(0, lastDot);
					String  deviceName = sub.substring(lastDot+1);
					boolean isHidden   = conf.getBooleanProperty(key.replace(PROPKEY_DEVICE_MAPPING_START, PROPKEY_DEVICE_HIDDEN_START), false);
//System.out.println("hostname='"+hostName+"', devicename='"+deviceName+"', isHidden="+isHidden+".");

					addEntry(new MappingEntry(hostName, deviceName, isHidden, val));
				}
			}
			setChanged(false);

//			boolean dummyEntries = true;
//			if (dummyEntries)
//			{
//				for (int i=0; i<10; i++)
//				{
//					String    srv  = "TEST_1_DS";
//					String    cmd  = "row "+i+": js fdgls�kfj \n gsdlfghjslkfjg hsjkgf ksjhdgf kjgh akjsdhg \naksfdjasgf akjs gfkajgsdh fkajs gfkajshdg fkajs gfkajs gfkajs gfkjasdgh fkjasdh gfkjasgfkjas \ngfkajf g";
//
//					addEntry(srv, cmd, null, null, null);
//				}
//			}
		}
	}



	/*---------------------------------------------------
	** BEGIN: class AddOrChangeEntryDialog
	**---------------------------------------------------
	*/
	private static class AddOrChangeEntryDialog
	extends JDialog
	implements ActionListener//, KeyListener
	{
		private static final long serialVersionUID = 1L;

		public        int  _dialogType     = -1;
		public static int  ADD_DIALOG      = 1;
		public static int  CHANGE_DIALOG   = 2;
		public static int  CLONE_DIALOG    = 3;

		private JButton    _ok             = new JButton("OK");
		private JButton    _cancel         = new JButton("Cancel");

		private MappingEntry _return = null;
		private MappingEntry _entry  = null;
		
		private JLabel               _hostName_lbl = new JLabel("Host Name");
		private JTextField           _hostName_txt = new JTextField();

		private JLabel               _deviceName_lbl = new JLabel("Device Name");
		private JTextField           _deviceName_txt = new JTextField();

		private JCheckBox            _isHidden_chk = new JCheckBox("Hide this device");
		
		private JLabel               _devDesc_lbl = new JLabel("Description");
		private JTextField           _devDesc_txt = new JTextField();

		private AddOrChangeEntryDialog(JDialog owner, MappingEntry entry, int dialogType)
		{
			super(owner, "", true);

//			_dialogType   = entry == null ? ADD_DIALOG : CHANGE_DIALOG;
			_dialogType   = dialogType;
			
			_entry        = entry;
			if (_entry == null)
				_entry = new MappingEntry();

			 if (_entry != null && _dialogType == CLONE_DIALOG)
				_entry = new MappingEntry(entry.getHostName(), entry.getDeviceName(), entry.isHidden(), entry.getDescription());

			initComponents();
			pack();
		}

		public static MappingEntry showDialog(JDialog owner, MappingEntry entry, int dialogType)
		{
			AddOrChangeEntryDialog dialog = new AddOrChangeEntryDialog(owner, entry, dialogType);
			dialog.setLocationRelativeTo(owner);
			dialog.setFocus();
			dialog.setVisible(true);
			dialog.dispose();

			return dialog._return;
		}

		protected void initComponents() 
		{
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout("insets 20 20","[][grow]",""));   // insets Top Left Bottom Right

			if (_dialogType == ADD_DIALOG)
			{
				setTitle("New Device Mapping");
			}
			else if (_dialogType == CHANGE_DIALOG)
			{
				setTitle("Change Device Mapping");
			}
			else if (_dialogType == CLONE_DIALOG)
			{
				setTitle("Clone Device Mapping");
			}
			else throw new RuntimeException("Unknown Dialog Type");

			_hostName_lbl  .setToolTipText("<html>For what hostname is this mapping valid for.</html>");
			_hostName_txt  .setToolTipText(_hostName_lbl.getToolTipText());
			_deviceName_lbl.setToolTipText("<html>For what device is this mapping valid for.</html>");
			_deviceName_txt.setToolTipText(_deviceName_lbl.getToolTipText());
			_isHidden_chk  .setToolTipText("<html>Should this device be visibale or discarded from the output</html>");
			_devDesc_lbl   .setToolTipText("<html>What is this device used for.</html>");
			_devDesc_txt   .setToolTipText(_devDesc_lbl.getToolTipText());

			panel.add(_hostName_lbl,  "");
			panel.add(_hostName_txt,  "pushx, growx, wrap");
			
			panel.add(_deviceName_lbl,  "");
			panel.add(_deviceName_txt,  "pushx, growx, wrap");
			
			panel.add(_isHidden_chk,  "skip, pushx, growx, wrap");
			
			panel.add(_devDesc_lbl,      "");
			panel.add(_devDesc_txt,      "pushx, growx, wrap 15");
			
			// ADD the OK, Cancel, Apply buttons
//			panel.add(_ok,     "tag ok,     gap top 20, skip, split, bottom, right, pushx");
			panel.add(_ok,     "tag ok,                 skip, split, bottom, right, pushx");
			panel.add(_cancel, "tag cancel,                   split, bottom");

			setContentPane(panel);

			// Fill in some start values
			_hostName_txt  .setText(_entry.getHostName());
			_deviceName_txt.setText(_entry.getDeviceName());
			_isHidden_chk  .setSelected(_entry.isHidden());
			_devDesc_txt   .setText(_entry.getDescription());
			
//			// ADD KEY listeners
//			_hostName_txt  .addKeyListener(this);
//			_deviceName_txt.addKeyListener(this);
//			_devDesc_txt   .addKeyListener(this);
			
			// ADD ACTIONS TO COMPONENTS
			_ok           .addActionListener(this);
			_cancel       .addActionListener(this);
		}

//		@Override public void keyPressed (KeyEvent e) {}
//		@Override public void keyTyped   (KeyEvent e) {}
//		@Override public void keyReleased(KeyEvent e)
//		{
//			setBasicInfo( _preview2_lbl, _favoriteName_txt.getText(), _favoriteDesc_txt.getText(), _favoriteIcon_txt.getText(), _command_txt.getText() );
//		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			Object source = e.getSource();

			// --- BUTTON: OK ---
			if (_ok.equals(source))
			{
				_entry.setHostName   (_hostName_txt   .getText());
				_entry.setDeviceName (_deviceName_txt .getText());
				_entry.setHidden     (_isHidden_chk   .isSelected());
				_entry.setDescription(_devDesc_txt    .getText());

				_return = _entry;

				setVisible(false);
			}

			// --- BUTTON: CANCEL ---
			if (_cancel.equals(source))
			{
				_return = null;
				setVisible(false);
			}
		}

		/**
		 * Set focus to a good field or button
		 */
		private void setFocus()
		{
			// The components needs to be visible for the requestFocus()
			// to work, so lets the EventThreda do it for us after the windows is visible.
			Runnable deferredAction = new Runnable()
			{
				@Override
				public void run()
				{
					_devDesc_txt.requestFocus();
				}
			};
			SwingUtilities.invokeLater(deferredAction);
		}
	}
	/*---------------------------------------------------
	** END: class AddOrChangeEntryDialog
	**---------------------------------------------------
	*/



	/**
	 * Helper method to check if a specific device is hidden
	 * 
	 * @param hostname
	 * @param deviceName
	 * @return true or false
	 */
	public static boolean isHidden(String hostname, String deviceName)
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		String propName = PROPKEY_DEVICE_HIDDEN_START + hostname + "." + deviceName;
		return conf.getBooleanProperty(propName, false);
	}

	/**
	 * Helper method to get a description for a specific device
	 * 
	 * @param hostname
	 * @param deviceName
	 * @return Description of the device, or null if not mapped
	 */
	public static String getDescription(String hostname, String deviceName)
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		String propName = PROPKEY_DEVICE_MAPPING_START + hostname + "." + deviceName;
//		return conf.getProperty(propName, "");
		return conf.getProperty(propName);
	}

	/**
	 * Helper method to set if a specific device is hidden
	 * 
	 * @param hostname
	 * @param deviceName
	 * @param true or false
	 */
	public static void setHidden(String hostname, String deviceName, boolean isHidden)
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
			return;

		String propName = PROPKEY_DEVICE_HIDDEN_START + hostname + "." + deviceName;
		conf.setProperty(propName, isHidden);
	}

	/**
	 * Helper method to set a description for a specific device
	 * 
	 * @param hostname
	 * @param deviceName
	 * @param Description of the device, or null if not mapped
	 */
	public static void setDescription(String hostname, String deviceName, String description)
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
			return;

		String propName = PROPKEY_DEVICE_MAPPING_START + hostname + "." + deviceName;
		conf.setProperty(propName, description);
	}
}

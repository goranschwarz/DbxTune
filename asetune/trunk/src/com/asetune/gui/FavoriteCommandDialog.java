package com.asetune.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.asetune.Version;
import com.asetune.gui.swing.GTable;
import com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.AseSqlScriptReader;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

public class FavoriteCommandDialog
//extends JFrame
extends JDialog
implements ActionListener, FocusListener //, ChangeListener
{
	private static Logger _logger = Logger.getLogger(FavoriteCommandDialog.class);
	private static final long serialVersionUID = 1L;

	public final static String PROPKEY_VENDOR_TYPE = "FavoriteCommand.VendorType";
	
	public enum VendorType
	{
		GENERIC,  // Show for "any" Vendor Type
		
		ASE, 
		ASA, 
		IQ, 
		RS, 
		RAX, 
		RSDRA, 
		
		HANA, 
		MAXDB, 

		ORACLE, 
		MSSQL, 
		DB2, 
		H2, 
		HSQL, 
		MYSQL, 
		DERBY
	};
	
	private JSplitPane           _splitPane              = new JSplitPane();

	private JButton              _ok                     = new JButton("OK");
	private JButton              _cancel                 = new JButton("Cancel");
	private JButton              _apply                  = new JButton("Apply");

	private JPanel               _topPanel               = null;
//	private JPanel               _okPanel                = null;

	private JPanel               _cmdPanel               = null;
	private RSyntaxTextAreaX     _cmd_txt                = new RSyntaxTextAreaX();
	private RTextScrollPane      _cmd_scroll             = new RTextScrollPane(_cmd_txt);

//	private JPanel               _statusBarPanel         = null;
	private JButton              _favoriteFile_but       = new JButton("...");
	private JLabel               _favoriteFile_lbl       = new JLabel();

	private JLabel               _preview1_lbl           = new JLabel("Preview");
	private JLabel               _preview2_lbl           = new JLabel("<html><i>How the item will look like</i></html>");
	private JLabel               _favoriteType_lbl       = new JLabel("Type");
	private JTextField           _favoriteType_txt       = new JTextField();
	private JLabel               _favoriteName_lbl       = new JLabel("Name");
	private JTextField           _favoriteName_txt       = new JTextField();
	private JLabel               _favoriteDesc_lbl       = new JLabel("Description");
	private JTextField           _favoriteDesc_txt       = new JTextField();
	private JLabel               _favoriteIcon_lbl       = new JLabel("Icon");
	private JTextField           _favoriteIcon_txt       = new JTextField();

	private JButton              _execCmd_but            = new JButton("Execute");
	private JButton              _addCmd_but             = new JButton("Add");
	private JButton              _addSeparator_but       = new JButton("Add Separator");
	private JButton              _changeCmd_but          = new JButton("Change");
	private JButton              _removeCmd_but          = new JButton("Remove");
	private JButton              _moveUp_but             = new JButton("Up");
	private JButton              _moveDown_but           = new JButton("Down");

	private FavoriteTableModel   _tm                     = null;
//	private JXTable              _table                  = null;
	private GTable               _table                  = null;

	private FavoriteOwner        _owner;
	private Window               _parentWindow;

	private String               _favoriteFileName       = null;

	private static final String  PROPKEY_SPLITPANE_DIV_LOC        = "FavoriteCommandDialog.splitpane.divider.location";
	private static final int     DEFAULT_SPLITPANE_DIV_LOC        = 300;

	private static final String  SEPARATOR = "SEPARATOR";


	public interface FavoriteOwner
	{
//		/** Execute the history entry */
//		public void   favoriteExecute(String cmd);
//
//		/** In case of a parameter substitution, get selected text from any component, return null if no text is selected */
//		public String getSelectedText();

		/** Favorites has been changed, please rebuild your favorite representation */ 
		public void   rebuild();
		
		/** save the place where the history file is to be stored at */
		public void   saveFavoriteFilename(String filename);
		
		/** Get what history file to use */
		public String getFavoriteFilename();
		
		/** Delegate Execution of the Statement... */
		public void doExecute(String statement);
	}

	public FavoriteCommandDialog(FavoriteOwner owner, Window window)
	{
		super(window, "Favorite Commands", ModalityType.MODELESS);
//		super(window, "Favorite Commands", ModalityType.APPLICATION_MODAL);
//		super();
		_owner = owner;
		_parentWindow = window;
		init();
		setFileName(owner.getFavoriteFilename());
		_tm.setChanged(false);
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
		setTitle("Favorite Commands"); // Set window title
		
		// Set the icon, if we "just" do setIconImage() on the JDialog
		// it will not be the "correct" icon in the Alt-Tab list on Windows
		// So we need to grab the owner, and set that since the icon is grabbed from the owner...
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/favorite.png");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/favorite_32.png");
		if (icon16 != null || icon32 != null)
		{
			ArrayList<Image> iconList = new ArrayList<Image>();
			if (icon16 != null) iconList.add(icon16.getImage());
			if (icon32 != null) iconList.add(icon32.getImage());

			Object owner = getOwner();
			if (owner != null && owner instanceof Frame)
				((Frame)owner).setIconImages(iconList);
			else
				setIconImages(iconList);
		}

		setLayout( new BorderLayout() );
		
		loadProps();

		_topPanel     = createTopPanel();
		_cmdPanel     = createCmdPanel();
//		_okPanel      = createOkCancelPanel();

//		add(_topPanel, BorderLayout.NORTH);
//		add(_cmdPanel, BorderLayout.CENTER);
//		_splitPane.setDividerLocation(300);
		_splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		_splitPane.setTopComponent(_topPanel);
		_splitPane.setBottomComponent(_cmdPanel);
		add(_splitPane, BorderLayout.CENTER);
		

		pack();
		getSavedWindowProps();
		setComponentVisibility();

//		addWindowListener(new WindowAdapter()
//		{
//			@Override
//			public void windowClosing(WindowEvent e)
//			{
//				if (_historyFileWatchDog != null)
//					_historyFileWatchDog.stop();
//				destroy();
//			}
//		});
	}


//	/** call this when window is closing */
//	private void destroy()
//	{
//		dispose();
//	}

	private JPanel createTopPanel()
	{
		JPanel panel = SwingUtils.createPanel("Top Panel", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

//		panel.setToolTipText("<html></html>");

		panel.add(createFavoriteTable(),   "push, grow, wrap");

		return panel;
	}

	private JPanel createFavoriteTable()
	{
		JPanel panel = SwingUtils.createPanel("Favorites", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

//		_table = new JXTable()
		_table = new GTable()
		{
			private static final long serialVersionUID = 1L;

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
						if (mIndex == FavoriteTableModel.TAB_POS_POS)     toolTip = "<html>Position in the Favorite Commands Menu.</html>";
						if (mIndex == FavoriteTableModel.TAB_POS_TYPE)    toolTip = "<html>Show this command only when connected to this Vendor.</html>";
						if (mIndex == FavoriteTableModel.TAB_POS_NAME)    toolTip = "<html>Name of this Favorite Command.</html>";
						if (mIndex == FavoriteTableModel.TAB_POS_DESC)    toolTip = "<html>Description of this Favorite Command.</html>";
						if (mIndex == FavoriteTableModel.TAB_POS_ICON)    toolTip = "<html>Filename of any Icon, representing the Statement.</html>";
						if (mIndex == FavoriteTableModel.TAB_POS_COMMAND) toolTip = "<html>The command that will be executed.</html>";
						return toolTip;
					}
				};
				return jxth;
			}

			// 
			// TOOL TIP for: CELLS
			//
			@Override
			public String getToolTipText(MouseEvent e) 
			{
				String tip = null;
				Point p = e.getPoint();
//				int col = columnAtPoint(p);
				int vcol = FavoriteTableModel.TAB_POS_COMMAND;
				int vrow = rowAtPoint(p);
				if (vcol >= 0 && vrow >= 0)
				{
					int mcol = FavoriteTableModel.TAB_POS_COMMAND; //super.convertColumnIndexToModel(vcol);
					int mrow = super.convertRowIndexToModel(vrow);

					TableModel tm = getModel();

					tip = tm.getValueAt(mrow, mcol) + "";

					if (tm instanceof FavoriteTableModel)
					{
						FavoriteTableModel ctm = (FavoriteTableModel) tm;
						
						String type   = ctm.getValueAt(mrow, FavoriteTableModel.TAB_POS_TYPE) + "";
						String name   = ctm.getValueAt(mrow, FavoriteTableModel.TAB_POS_NAME) + "";
						String desc   = ctm.getValueAt(mrow, FavoriteTableModel.TAB_POS_DESC) + "";
						String icon   = ctm.getValueAt(mrow, FavoriteTableModel.TAB_POS_ICON) + "";
						String cmd    = ctm.getCommand(mrow);
//						private static final String[] TAB_HEADER = {"Row", "Time", "Server", "User", "Db", "Source", "#", "Command"};

						tip = "<html>" +
						      "<table align=\"left\" border=0 cellspacing=0 cellpadding=0>" +
						        "<tr> <td><b>Type:        </b></td> <td>" + type + "</td> </tr>" +
						        "<tr> <td><b>Name:        </b></td> <td>" + name + "</td> </tr>" +
						        "<tr> <td><b>Description: </b></td> <td>" + desc + "</td> </tr>" +
						        "<tr> <td><b>Icon:        </b></td> <td>" + icon + "</td> </tr>" +
						      "</table>" +
						      "<hr>" +
						      "<pre>"+cmd+"</pre>" +
						      "</html>";
					}
				}
				return tip;
			}
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
					
					FavoriteCommandEntry entry = _tm.getEntry(mrow);

					// Execute in owner
					_owner.doExecute( entry.getOriginCommand() );
					
//					// Open the editor
//					FavoriteCommandEntry e2 = AddOrChangeEntryDialog.showDialog(FavoriteCommandDialog.this, entry);
//					if (e2 != null)
//					{
//						_tm.setChanged(true);
//						_tm.fireTableDataChanged();
//					}
				}
			}
		});

		// Selection listener
		_table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				int vrow = _table.getSelectedRow();
				int vcol = FavoriteTableModel.TAB_POS_COMMAND;
	
				if (vcol >= 0 && vrow >= 0)
				{
//					int mcol = FavoriteTableModel.TAB_POS_COMMAND; //_table.convertColumnIndexToModel(col);
					int mrow = _table.convertRowIndexToModel(vrow);

					TableModel tm = _table.getModel();

					String type = tm.getValueAt(mrow, FavoriteTableModel.TAB_POS_TYPE)    + "";
					String name = tm.getValueAt(mrow, FavoriteTableModel.TAB_POS_NAME)    + "";
					String desc = tm.getValueAt(mrow, FavoriteTableModel.TAB_POS_DESC)    + "";
					String icon = tm.getValueAt(mrow, FavoriteTableModel.TAB_POS_ICON)    + "";
					String cmd  = tm.getValueAt(mrow, FavoriteTableModel.TAB_POS_COMMAND) + "";
					if (tm instanceof FavoriteTableModel)
					{
						FavoriteTableModel ctm = (FavoriteTableModel) tm;
						cmd = ctm.getCommand(mrow);
					}

					_favoriteType_txt.setText(type);
					_favoriteName_txt.setText(name);
					_favoriteDesc_txt.setText(desc);
					_favoriteIcon_txt.setText(icon);
					
					setBasicInfo(_preview2_lbl, name, desc, icon, cmd);

					_cmd_txt.setText(cmd);
					_cmd_txt.setCaretPosition(0);
				}
				
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
		_table.getActionMap().put("copy", _copySelectedRowsAction);

		//---------------------------------------------------
		// Highlighter
		_table.addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				int mrow = adapter.convertRowIndexToModel(adapter.row);
				FavoriteCommandEntry entry = _tm.getEntry(mrow);
				if (entry != null && entry.isSeparator())
					return true;
				return false;
			}
		}, new Color(150, 200, 250), null));  // some blue'ish thing...
//		}, Color.BLUE, null));


		//--------------------------------------
		// Drag and Drop FROM JTable
		//--------------------------------------
//		_table.setDropMode(DropMode.INSERT_ROWS);
		_table.setDragEnabled(true);
		_table.setTransferHandler(new TransferHandler()
		{
			private static final long serialVersionUID = 1L;
			
			@Override
			protected Transferable createTransferable(JComponent c)
			{
				return new StringSelection(getCommandsForSelectedRows());
			}
			@Override
		    public int getSourceActions(JComponent c) 
			{
				return TransferHandler.COPY;
			}	
		});
		
		_tm = new FavoriteTableModel();
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

		panel.add(jScrollPane, "span, push, grow, height 100%, wrap");
		// Focus action listener

		return panel;
	}

	private JPanel createInfoPanel()
	{
		JPanel panel = SwingUtils.createPanel("Ino Panel", false);
		panel.setLayout(new MigLayout());

//		panel.setToolTipText("<html></html>");

		_execCmd_but            .setToolTipText("<html>Execute the Statement in the text area <b>below</b>.</html>");
		_addCmd_but             .setToolTipText("<html>Add a new Command to the Favorite list.</html>");
		_addSeparator_but       .setToolTipText("<html>Add a separator</html>");
		_changeCmd_but          .setToolTipText("<html>Open the Edit Dialog where you can change the command.</html>");
		_removeCmd_but          .setToolTipText("<html>Remove the Selected Command from the Favorite List.</html>");
		_moveUp_but             .setToolTipText("<html>Move the Selected Command <b>up</b> in the Favorite List.</html>");
		_moveDown_but           .setToolTipText("<html>Move the Selected Command <b>down</b> in the Favorite List.</html>");

		_favoriteType_txt.setEnabled(false);
		_favoriteName_txt.setEnabled(false);
		_favoriteDesc_txt.setEnabled(false);
		_favoriteIcon_txt.setEnabled(false);

		panel.add(_favoriteType_lbl,  "");
		panel.add(_favoriteType_txt,  "pushx, growx, wrap");
		
		panel.add(_favoriteName_lbl,  "");
		panel.add(_favoriteName_txt,  "pushx, growx, wrap");
		
		panel.add(_favoriteDesc_lbl,  "");
		panel.add(_favoriteDesc_txt,  "pushx, growx, wrap");
		
		panel.add(_favoriteIcon_lbl,  "");
		panel.add(_favoriteIcon_txt,  "pushx, growx, wrap");
		
		panel.add(_preview1_lbl,      "");
		panel.add(_preview2_lbl,      "pushx, growx, wrap 15");
		
		panel.add(_execCmd_but,         "span, split");  // gap left [right] [top] [bottom]
		panel.add(_addCmd_but,          "");
		panel.add(_addSeparator_but,    "");
		panel.add(_changeCmd_but,       "");
		panel.add(_removeCmd_but,       "");
		panel.add(_moveUp_but,          "gap 10");
		panel.add(_moveDown_but,        "");

		// Focus action listener
		
		// action
		_execCmd_but     .addActionListener(this);
		_addCmd_but      .addActionListener(this);
		_addSeparator_but.addActionListener(this);
		_changeCmd_but   .addActionListener(this);
		_removeCmd_but   .addActionListener(this);
		_moveUp_but      .addActionListener(this);
		_moveDown_but    .addActionListener(this);

		// Action Commands
//		_addCmd_but      .setActionCommand(ACTION_EXECUTE);
		_execCmd_but     .setAction(_executeFavoriteAction);
		_addCmd_but      .setAction(_addFavoriteAction);
		_addSeparator_but.setAction(_addSeparatorAction);
		_changeCmd_but   .setAction(_changeFavoriteAction);
		_removeCmd_but   .setAction(_deleteSelectedRowsAction);
		_moveUp_but      .setAction(_moveUpSelectedRowsAction);
		_moveDown_but    .setAction(_moveDownSelectedRowsAction);
		
		return panel;
	}

	private JPanel createCmdPanel()
	{
		JPanel panel = SwingUtils.createPanel("Trace Command Log", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		// Tooltip
		panel   .setToolTipText("Details View of what this command does");
		_cmd_txt.setToolTipText("<html>Read only view, to edit the command please press <b>change</b></html>");
		
//		_cmd_txt.setEnabled(false);
		_cmd_txt.setEditable(false);

		panel.add(createInfoPanel(),      "span, growx, pushx, wrap");
		panel.add(_cmd_scroll,            "span, grow, push, wrap");
		panel.add(createStatusBarPanel(), "gap 5, growx, pushx");
		panel.add(createOkCancelPanel(),  "bottom, right, pushx");

		RSyntaxUtilitiesX.installRightClickMenuExtentions(_cmd_scroll, this);
		// The SYBASE_TSQL dosn't seem to work *here* don't know why...???
//		_cmd_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);
		_cmd_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);

		return panel;
	}

	private static final String FAVORITE_FILE_TOOLTIP_TEMPLATE = "<html>Set a new Favorite Commands File to use.<br>Current file: <code> FAVORITE_FILE_NAME </code></html>"; 

	private JPanel createStatusBarPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		
		_favoriteFile_lbl.setToolTipText("<html>Favorite Commands File used for the moment. Press <i>button on the left</i> to change Favorite Commands File.</html>");
		_favoriteFile_but.setToolTipText(FAVORITE_FILE_TOOLTIP_TEMPLATE);

		// History file button
		_favoriteFile_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/favorite_file.png"));
		_favoriteFile_but.setText(null);
		_favoriteFile_but.setContentAreaFilled(false);
		_favoriteFile_but.setMargin( new Insets(0,0,0,0) );

		panel.add(_favoriteFile_but, "");
		panel.add(_favoriteFile_lbl, "pushx, growx");
		
		_favoriteFile_but.addActionListener(this);

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
	@SuppressWarnings("serial")
	private List<JComponent> createPopupMenuComponents()
	{
		ArrayList<JComponent> list = new ArrayList<JComponent>();
		JMenuItem  mi = null;

		//------------------------------------------------------------
		mi = new JMenuItem("Execute Selected Row(s)")
		{
			@Override
			public String getToolTipText()
			{
				return "<html>" + 
						"<h3>The below text will be executed in the <i>owners</i> context.</h3>" +
						"<pre>" +
						getCommandsForSelectedRows() +
						"</pre>" +
						"</html>";
			}
		};
		mi.setAction(_executeFavoriteRowsAction);
		ToolTipManager.sharedInstance().registerComponent(mi); // just to register so that getToolTipText will be called
		list.add(mi);

		//------------------------------------------------------------
		mi = new JMenuItem("Move Up");
		mi.setAction(_moveUpSelectedRowsAction);
		list.add(mi);

		//------------------------------------------------------------
		mi = new JMenuItem("Move Down");
		mi.setAction(_moveDownSelectedRowsAction);
		list.add(mi);

		//------------------------------------------------------------
		mi = new JMenuItem("Add");
		mi.setAction(_addFavoriteAction);
		list.add(mi);

		//------------------------------------------------------------
		mi = new JMenuItem("Add Separator");
		mi.setAction(_addSeparatorAction);
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
		mi = new JMenuItem("Copy");
		mi.setAction(_copySelectedRowsAction);
		list.add(mi);

		return list;
	}

	private void doApply()
	{
		try
		{
			saveFavoriteFile();
		}
		catch (Throwable e)
		{
			_logger.warn("Problems writing to the Favorite Command File '"+getFileName()+"'. Caught: "+e);

			// GUI popup with the error, AND a JCheckBox to "not show this message again"
			String msgHtml = 
				"<html>" +
				"<h2>Problems writing to the Favorite Command File</h2>" +
				"Filename: <code>"+getFileName()+"</code><br>" +
				"</html>";

			SwingUtils.showErrorMessage(_parentWindow, "Problems Saving to Favorite Command File.", msgHtml, e);
			return;
		}
		
		_owner.saveFavoriteFilename(_favoriteFileName);
		_owner.rebuild();

		_tm.setChanged(false);
		_apply.setEnabled(false);
	}
	
	/*---------------------------------------------------
	** BEGIN: implementing: ChangeListener, ActionListener, FocusListener, FileTail.TraceListener, Memory.MemoryListener
	**---------------------------------------------------
	*/	

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source    = e.getSource();
		String actionCmd = e.getActionCommand();
		
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

//		if (_addCmd_but.equals(source))
//		{
//			FavoriteCommandEntry entry = AddOrChangeEntryDialog.showDialog(this, null);
//			if (entry != null)
//				addEntry(-1, entry);
//		}

//		if (_addSeparator_but.equals(source))
//		{
//			FavoriteCommandEntry entry = FavoriteCommandEntry.addSeparator();
//
//			int selRow = _table.getSelectedRow();
//			if (selRow >= 0)
//			{
//				int mrow = _table.convertRowIndexToModel(selRow);
//				addEntry(mrow, entry);
//			}
//			else // Add to the end
//			{
//				addEntry(1, entry);
//			}
//		}

//		if (_changeCmd_but.equals(source))
//		{
//			int selRow = _table.getSelectedRow();
//			if (selRow >= 0)
//			{
//				int mrow = _table.convertRowIndexToModel(selRow);
//
//				FavoriteCommandEntry entry = _tm.getEntry(mrow);
//				FavoriteCommandEntry e2 = AddOrChangeEntryDialog.showDialog(FavoriteCommandDialog.this, entry);
//				if (e2 != null)
//				{
//					_tm.setChanged(true);
//					_tm.fireTableDataChanged();
//				}
//			}
//		}

		if (_favoriteFile_but.equals(source))
		{
			String dir = getFileName();

			JFileChooser fc = new JFileChooser(dir);
			int returnVal = fc.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) 
			{
				setFileName(fc.getSelectedFile().getAbsolutePath());
			}
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

		// remove text fields if no rows...
		if ( ! enabled )
			_cmd_txt.setText("");
		
		// Set what should be enabled/disabled
//		_addCmd_but      .setEnabled(enabled);
//		_addSeparator_but.setEnabled(enabled);
//		_changeCmd_but   .setEnabled(enabled);
//		_removeCmd_but   .setEnabled(enabled);
//		_moveUp_but      .setEnabled(enabled);
//		_moveDown_but    .setEnabled(enabled);

		_changeFavoriteAction      .setEnabled(enabled);
		_deleteSelectedRowsAction  .setEnabled(enabled);
		_moveUpSelectedRowsAction  .setEnabled(enabled);
		_moveDownSelectedRowsAction.setEnabled(enabled);

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
		conf.setProperty(PROPKEY_SPLITPANE_DIV_LOC, _splitPane.getDividerLocation());

		
		//------------------
		// WINDOW
		//------------------
		conf.setProperty("FavoriteCommandDialog.window.width",  this.getSize().width);
		conf.setProperty("FavoriteCommandDialog.window.height", this.getSize().height);
		conf.setProperty("FavoriteCommandDialog.window.pos.x",  this.getLocationOnScreen().x);
		conf.setProperty("FavoriteCommandDialog.window.pos.y",  this.getLocationOnScreen().y);

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
		int width  = conf.getIntProperty("FavoriteCommandDialog.window.width",  900);
		int height = conf.getIntProperty("FavoriteCommandDialog.window.height", 740);
		int x      = conf.getIntProperty("FavoriteCommandDialog.window.pos.x",  -1);
		int y      = conf.getIntProperty("FavoriteCommandDialog.window.pos.y",  -1);

		if (width != -1 && height != -1)
			this.setSize(width, height);

		if (x != -1 && y != -1)
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
				this.setLocation(x, y);

		//------------------
		// SPLIT PANE
		//------------------
		int dividerLocation = conf.getIntProperty(PROPKEY_SPLITPANE_DIV_LOC, DEFAULT_SPLITPANE_DIV_LOC);
		_splitPane.setDividerLocation(dividerLocation);

	}
	/*---------------------------------------------------
	** END: implementing saveProps & loadProps
	**---------------------------------------------------
	*/	
	
	public void setFileName(String filename)
	{
		_favoriteFileName = filename;
		_favoriteFile_but.setToolTipText( FAVORITE_FILE_TOOLTIP_TEMPLATE.replace("FAVORITE_FILE_NAME", filename) );
		_favoriteFile_lbl.setText(filename);
	
		// load the information from the file.
		reload();
	}

	public String getFileName()
	{
		//_favoriteFileName = _owner.getHistoryFilename();
		return _favoriteFileName;
	}

	/** 
	 * load/reload content from current file 
	 */
	public void reload()
	{
		// and we need to do X stuff here
		// - clean favorite table
		// - parse the current file
		// - notify the owner of a change
		_tm.clear(false);

		FavoriteFileXmlParser parser = new FavoriteFileXmlParser();
		ArrayList<FavoriteCommandEntry> entries = parser.parseFile(_favoriteFileName);
		
		_tm.setEntries(entries, true);
		_table.packAll();
	}

	/**
	 * Get all Favorite Commands
	 * @return a list of entries
	 */
	public ArrayList<FavoriteCommandEntry> getEntries()
	{
		return _tm.getEntries();
	}
	/**
	 * Add entry 
	 */
	public void addEntry(VendorType type, String name, String description, String icon, String cmd)
	{
		addEntry(-1, new FavoriteCommandEntry(type, name, description, icon, cmd));
	}

	/**
	 * Internally called from the tail reader 
	 * @param pos -1 = At the end
	 * @param entry
	 */
	private void addEntry(int pos, FavoriteCommandEntry entry)
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

	private void saveFavoriteFile()
	throws IOException
	{
		saveFavoriteFile(getFileName(), _tm.getEntries());
	}
	private void saveFavoriteFile(ArrayList<FavoriteCommandEntry> list)
	throws IOException
	{
		saveFavoriteFile(getFileName(), list);
	}
	/**
	 * Write a new history file...
	 * @param fileName       Name of the file can be null, then we will use current file.
	 * @param list           Entries to write to the file
	 * @throws IOException   When we had problems to write.
	 */
	private void saveFavoriteFile(String fileName, ArrayList<FavoriteCommandEntry> list)
	throws IOException
	{
		if (StringUtil.isNullOrBlank(fileName))
			fileName = getFileName();

		try
		{
			RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
			FileChannel channel = raf.getChannel();

			try 
			{
				// Get an exclusive lock on the whole file
				FileLock lock = channel.lock();

				try 
				{
					// To start of the file, truncate everything beyond position 0
					channel.truncate(0);

					// ----------------------------------------------------
					// Add Beginning of the file
					StringBuilder sb = new StringBuilder();
					sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
					sb.append("\n");
					sb.append(XML_BEGIN_TAG_FAVORITE_COMMAND_LIST).append("\n");
					sb.append("\n");

					//-----------------------------------------
					// Write header
					ByteBuffer byteBuffer;
					byteBuffer = ByteBuffer.wrap(sb.toString().getBytes(Charset.forName("UTF-8")));
					channel.write(byteBuffer);


					//--------------------------------------
					// Write all the history tags in the input list
					for (FavoriteCommandEntry entry : list)
					{
						sb.setLength(0);
						
						sb.append(entry.toXml());
						
						byteBuffer = ByteBuffer.wrap(sb.toString().getBytes(Charset.forName("UTF-8")));
						channel.write(byteBuffer);
					}

					
					//-----------------------------------------
					// Write -end- entries
					sb.setLength(0);
					
					sb.append("\n");
					sb.append(XML_END___TAG_FAVORITE_COMMAND_LIST).append("\n");

					byteBuffer = ByteBuffer.wrap(sb.toString().getBytes(Charset.forName("UTF-8")));
					channel.write(byteBuffer);

					// Make sure it's written to disk.
					channel.force(true);
				}
				finally 
				{
					lock.release();
				}
			} 
			finally 
			{
				channel.close();
			}			
		}
		catch (IOException e)
		{
			_logger.warn("Problems writing to Favorite Commands file '"+fileName+"'. No history entry was added. Caught: "+e);
			throw e;
		}
	}
	
	//---------------------------------------------------
	// Below is XML tags
	//---------------------------------------------------
	private static final String       XML_TAG_FAVORITE_COMMAND_LIST  = "FavoriteCommandList";
	private static final String XML_BEGIN_TAG_FAVORITE_COMMAND_LIST  = "<"  + XML_TAG_FAVORITE_COMMAND_LIST + ">";
	private static final String XML_END___TAG_FAVORITE_COMMAND_LIST  = "</" + XML_TAG_FAVORITE_COMMAND_LIST + ">";
	
	private static final String       XML_TAG_FAVORITE_COMMAND_ENTRY = "FavoriteCommandEntry";
	private static final String XML_BEGIN_TAG_FAVORITE_COMMAND_ENTRY = "<"  + XML_TAG_FAVORITE_COMMAND_ENTRY + ">";
	private static final String XML_END___TAG_FAVORITE_COMMAND_ENTRY = "</" + XML_TAG_FAVORITE_COMMAND_ENTRY + ">";

	private static final String       XML_SUBTAG_TYPE                = "Type";
	private static final String XML_BEGIN_SUBTAG_TYPE                = "<"  + XML_SUBTAG_TYPE + ">";
	private static final String XML_END___SUBTAG_TYPE                = "</" + XML_SUBTAG_TYPE + ">";

	private static final String       XML_SUBTAG_NAME                = "Name";
	private static final String XML_BEGIN_SUBTAG_NAME                = "<"  + XML_SUBTAG_NAME + ">";
	private static final String XML_END___SUBTAG_NAME                = "</" + XML_SUBTAG_NAME + ">";

	private static final String       XML_SUBTAG_DESCRIPTION         = "Description";
	private static final String XML_BEGIN_SUBTAG_DESCRIPTION         = "<"  + XML_SUBTAG_DESCRIPTION + ">";
	private static final String XML_END___SUBTAG_DESCRIPTION         = "</" + XML_SUBTAG_DESCRIPTION + ">";

	private static final String       XML_SUBTAG_ICON                = "Icon";
	private static final String XML_BEGIN_SUBTAG_ICON                = "<"  + XML_SUBTAG_ICON + ">";
	private static final String XML_END___SUBTAG_ICON                = "</" + XML_SUBTAG_ICON + ">";

	private static final String       XML_SUBTAG_COMMAND             = "Command";
	private static final String XML_BEGIN_SUBTAG_COMMAND             = "<"  + XML_SUBTAG_COMMAND + ">";
	private static final String XML_END___SUBTAG_COMMAND             = "</" + XML_SUBTAG_COMMAND + ">";

	//-------------------------------------------------------------------
	// XML PARSER
	//-------------------------------------------------------------------
	private class FavoriteFileXmlParser
	extends DefaultHandler
	{
		private SAXParserFactory _saxFactory      = SAXParserFactory.newInstance();
		private SAXParser        _saxParser       = null;

		private FavoriteFileXmlParser()
		{
			try
			{
				_saxParser = _saxFactory.newSAXParser();
			}
			catch (SAXException e)
			{
				_logger.warn("Problems Creating Favorite Commands XML Parser '"+getFileName()+"'. Caught: "+e, e);
			}
			catch (ParserConfigurationException e)
			{
				_logger.warn("Problems Creating Favorite Commands XML Parser '"+getFileName()+"'. Caught: "+e, e);
			}
		}

//		public FavoriteCommandEntry parseEntry(String entry)
//		{
//			_lastEntry       = null;
//			_entryList       = null;
//			try
//			{
//				_saxParser.parse(new InputSource(new StringReader(entry)), this);
//			}
//			catch (SAXException e)
//			{
//				_logger.warn("Problems Parsing Command history Entry '"+entry+"'. Caught: "+e, e);
//			}
//			catch (IOException e)
//			{
//				_logger.warn("Problems Parsing Command history Entry '"+entry+"'. Caught: "+e, e);
//			}
//			return _lastEntry;
//		}

		/**
		 *  Parse a file
		 *  @param filename        Name of the file
		 *  
		 *  @return A ArrayList with FavoriteCommandEntry
		 */
		public ArrayList<FavoriteCommandEntry> parseFile(String fileName)
		{
			_lastEntry       = new FavoriteCommandEntry();
			_entryList       = new ArrayList<FavoriteCommandEntry>();

			try
			{
//				_saxParser.parse(new InputSource(new FileReader(fileName)), this);
				_saxParser.parse(new File(fileName), this);
			}
			catch (SAXException e)
			{
				_logger.warn("Problems Parsing Favorite Commands File '"+fileName+"'. Caught: "+e, e);
			}
			catch (FileNotFoundException e)
			{
				_logger.info("The Favorite Command File '"+fileName+"' wasn't found. Caught: "+e);
			}
			catch (IOException e)
			{
				_logger.warn("Problems Parsing Favorite Commands File '"+fileName+"'. Caught: "+e, e);
			}
			return _entryList;
		}

		//----------------------------------------------------------
		// START: XML Parsing code
		//----------------------------------------------------------
		private StringBuilder                   _xmlTagBuffer = new StringBuilder();
		private FavoriteCommandEntry            _lastEntry    = new FavoriteCommandEntry();
		private ArrayList<FavoriteCommandEntry> _entryList    = new ArrayList<FavoriteCommandEntry>();

		@Override
		public void characters(char[] buffer, int start, int length)
		{
			_xmlTagBuffer.append(buffer, start, length);
//			System.out.println("XML.character: start="+start+", length="+length+", _xmlTagBuffer="+_xmlTagBuffer);
		}

		@Override
		public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) 
		throws SAXException
		{
			_xmlTagBuffer.setLength(0);
//			System.out.println("SAX: startElement: qName='"+qName+"', attributes="+attributes);
			if (XML_TAG_FAVORITE_COMMAND_ENTRY.equals(qName))
			{
				_lastEntry = new FavoriteCommandEntry();
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) 
		throws SAXException
		{
//			System.out.println("SAX: endElement: qName='"+qName+"', _xmlTagBuffer="+_xmlTagBuffer);
			if (XML_TAG_FAVORITE_COMMAND_ENTRY.equals(qName))
			{
				if (_entryList == null)
					_entryList = new ArrayList<FavoriteCommandEntry>();
				_entryList.add(_lastEntry);
			}
			else
			{
				if      (XML_SUBTAG_COMMAND    .equals(qName)) _lastEntry.setCommand    (_xmlTagBuffer.toString().trim());
				else if (XML_SUBTAG_TYPE       .equals(qName)) _lastEntry.setType       (_xmlTagBuffer.toString().trim());
				else if (XML_SUBTAG_NAME       .equals(qName)) _lastEntry.setName       (_xmlTagBuffer.toString().trim());
				else if (XML_SUBTAG_DESCRIPTION.equals(qName)) _lastEntry.setDescription(_xmlTagBuffer.toString().trim());
				else if (XML_SUBTAG_ICON       .equals(qName)) _lastEntry.setIcon       (_xmlTagBuffer.toString().trim());
			}
			_xmlTagBuffer.setLength(0);
		}
		//----------------------------------------------------------
		// END: XML Parsing code
		//----------------------------------------------------------
	}


	//-------------------------------------------------------------------
	// Local ACTION classes
	//-------------------------------------------------------------------
	private MoveUpSelectedRowsAction   _moveUpSelectedRowsAction   = new MoveUpSelectedRowsAction();
	private MoveDownSelectedRowsAction _moveDownSelectedRowsAction = new MoveDownSelectedRowsAction();
	private ExecuteFavoriteRowsAction  _executeFavoriteRowsAction  = new ExecuteFavoriteRowsAction();
	private ExecuteFavoriteAction      _executeFavoriteAction      = new ExecuteFavoriteAction();
	private AddFavoriteAction          _addFavoriteAction          = new AddFavoriteAction();
	private AddSeparatorAction         _addSeparatorAction         = new AddSeparatorAction();
	private ChangeFavoriteAction       _changeFavoriteAction       = new ChangeFavoriteAction();
	private DeleteSelectedRowsAction   _deleteSelectedRowsAction   = new DeleteSelectedRowsAction();
	private CopySelectedRowsAction     _copySelectedRowsAction     = new CopySelectedRowsAction();

	private class MoveUpSelectedRowsAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Move Up";
		private static final String ICON = "images/up.png";
//		private static final ImageIcon ICON = SwingUtils.readImageIcon(Version.class, "images/move_up.png");

		public MoveUpSelectedRowsAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			int vrow = _table.getSelectedRow();
			int mrow = _table.convertRowIndexToModel(vrow);

			int[] vrows = _table.getSelectedRows();
			int[] mrows = new int[vrows.length];
			for (int r=0; r<vrows.length; r++)
				mrows[r] = _table.convertRowIndexToModel(vrows[r]);

			TableModel tm = _table.getModel();
			if (tm instanceof FavoriteTableModel)
			{
				FavoriteTableModel ctm = (FavoriteTableModel) tm;
				
				Arrays.sort(mrows);
				int start = mrows[0];
				int end   = mrows[ mrows.length - 1 ];
				int to    = mrow - 1;
				if (to < 0)
					return;

				ctm.moveRow(start, end, to);
				_table.getSelectionModel().setSelectionInterval(vrows[0]-1, vrows[vrows.length-1]-1);

				setComponentVisibility();
			}
		}
	}


	private class MoveDownSelectedRowsAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Move Down";
		private static final String ICON = "images/down.png";
//		private static final ImageIcon ICON = SwingUtils.readImageIcon(Version.class, "images/move_up.png");

		public MoveDownSelectedRowsAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			int vrow = _table.getSelectedRow();
			int mrow = _table.convertRowIndexToModel(vrow);

			int[] vrows = _table.getSelectedRows();
			int[] mrows = new int[vrows.length];
			for (int r=0; r<vrows.length; r++)
				mrows[r] = _table.convertRowIndexToModel(vrows[r]);

			TableModel tm = _table.getModel();
			if (tm instanceof FavoriteTableModel)
			{
				FavoriteTableModel ctm = (FavoriteTableModel) tm;
				
				Arrays.sort(mrows);
				int start = mrows[0];
				int end   = mrows[ mrows.length - 1 ];
				int to    = mrow + 1;
				if ( (to + (mrows.length-1)) >= ctm.getRowCount())
					return;

				ctm.moveRow(start, end, to);
				_table.getSelectionModel().setSelectionInterval(vrows[0]+1, vrows[vrows.length-1]+1);

				setComponentVisibility();
			}
		}
	}


	private class ExecuteFavoriteAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Execute";
		private static final String ICON = "images/exec.png";

		public ExecuteFavoriteAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			_owner.doExecute(_cmd_txt.getText());
		}
	}

	private class ExecuteFavoriteRowsAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Execute Selected Row(s)";
		private static final String ICON = "images/exec.png";

		public ExecuteFavoriteRowsAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			_owner.doExecute(getCommandsForSelectedRows());
		}
	}

	private class AddFavoriteAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Add";
		private static final String ICON = "images/favorite_add.png";

		public AddFavoriteAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			FavoriteCommandEntry entry = AddOrChangeEntryDialog.showDialog(FavoriteCommandDialog.this, null);
			if (entry != null)
				addEntry(-1, entry);
		}
	}


	private class AddSeparatorAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Add Separator";
		private static final String ICON = "images/separator.png";

		public AddSeparatorAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			FavoriteCommandEntry entry = FavoriteCommandEntry.addSeparator();
			
			int selRow = _table.getSelectedRow();
			if (selRow >= 0)
			{
				int mrow = _table.convertRowIndexToModel(selRow);
				addEntry(mrow, entry);
			}
			else // Add to the end
			{
				addEntry(-1, entry);
			}
		}
	}


	private class ChangeFavoriteAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Change";
		private static final String ICON = "images/favorite_change.png";

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

				FavoriteCommandEntry entry = _tm.getEntry(mrow);
				FavoriteCommandEntry e2 = AddOrChangeEntryDialog.showDialog(FavoriteCommandDialog.this, entry);
				if (e2 != null)
				{
					_tm.setChanged(true);
					_tm.fireTableDataChanged();
				}
			}
			else
			{
				SwingUtils.showInfoMessage(FavoriteCommandDialog.this, "Select a entry", "No entry is selected");
			}
		}
	}


	private class DeleteSelectedRowsAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Delete";
		private static final String ICON = "images/favorite_delete.png";
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
			if (tm instanceof FavoriteTableModel)
			{
				FavoriteTableModel ctm = (FavoriteTableModel) tm;
				
				// Delete all rows, but start at the end of the model
				for (int r=mrows.length-1; r>=0; r--)
					ctm.deleteEntry(mrows[r]);

				setComponentVisibility();
			}
		}
	}


	private class CopySelectedRowsAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Copy";
		private static final String ICON = "images/copy.png";
//		private        final ImageIcon ICON = SwingUtils.readImageIcon(Version.class, "images/copy.png");

		public CopySelectedRowsAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			SwingUtils.setClipboardContents(getCommandsForSelectedRows());
		}
	}


//	private String getTextForSelectedRowsInTable()
//	{
//		int[] vrows = _table.getSelectedRows();
//
//		// Multiple rows is selected in the favorite list
//		if (vrows.length > 1)
//		{
////			System.out.println("MULTI ROW EXECUTION: rows: "+rows.length+", "+rows);
//
//			// Copy rows into a String List
//			// BECAUSE if history size is low, rows will be deleted from the history table at execution
//			ArrayList<String> execList = new ArrayList<String>();
//
//			int[] mrows = new int[vrows.length];
//			for (int r=0; r<vrows.length; r++)
//				mrows[r] = _table.convertRowIndexToModel(vrows[r]);
//
//			TableModel tm = _table.getModel();
//			if (tm instanceof FavoriteTableModel)
//			{
//				FavoriteTableModel ftm = (FavoriteTableModel) tm;
//				
//				// Get each selected command and add it to a list, to be executed in next step
//				for (int r=0; r<mrows.length; r++)
//					execList.add( ftm.getCommand(mrows[r]) );
//			}
//
//			// compose a statement, 'go' will be added at the end of each table entry
//			StringBuilder sb = new StringBuilder();
//			for (String str : execList)
//				sb.append(str).append("\ngo\n");
//
//			// return str
//			return sb.toString();
//		}
//		else // one or no selection, use the text in the editor
//		{
//			return _cmd_txt.getText();
//		}
//	} // end: method
//
	/**
	 * helper method used by dragAndDrop and CopyHistoryRowsAction
	 * @return
	 */
	private String getCommandsForSelectedRows()
	{
		StringBuilder sb = new StringBuilder();

		int[] vrows = _table.getSelectedRows();
		int[] mrows = new int[vrows.length];
		for (int r=0; r<vrows.length; r++)
			mrows[r] = _table.convertRowIndexToModel(vrows[r]);

		boolean addGo = false;
		if (vrows.length > 1)
			addGo = true;

		TableModel tm = _table.getModel();
		if (tm instanceof FavoriteTableModel)
		{
			FavoriteTableModel ctm = (FavoriteTableModel) tm;
			
			// Copy all of the selected rows
			for (int r=0; r<mrows.length; r++)
			{
				String cmd = ctm.getCommand(mrows[r]);

				sb.append( cmd );
				if (addGo)
				{
					String goTerminator = AseSqlScriptReader.getConfiguredGoTerminator();

					// If it's NOT ended with any 'go' command or other terminator, add a 'go' at the end... 
					if ( ! AseSqlScriptReader.hasCommandTerminator(sb.toString(), goTerminator) )
						sb.append("\ngo");
				}
				if ( ! cmd.endsWith("\n") )
					sb.append("\n");
			}
		}

		return sb.toString();
	}

	
	public static String setBasicInfo(JComponent comp, FavoriteCommandEntry entry)
	{
		if (entry == null)
			return null;
		return setBasicInfo(comp, entry.getName(), entry.getDescription(), entry.getIcon(), entry.getOriginCommand());
	}
	public static String setBasicInfo(JComponent comp, String name, String desc, String icon, String cmd)
	{
		String xName = (StringUtil.isNullOrBlank(name)) ? cmd  : name;
		String xDesc = (StringUtil.isNullOrBlank(desc)) ? ""   : " - <i><font color=\"green\">"+desc+"</font></i>";
		String xText = "<html><b>"+xName+"</b>"+xDesc+"</html>";

//		String xTTip = (desc != null) ? desc : "<html><pre>" + cmd + "</pre></html>";
		String xTTip = "<html><pre>" + cmd + "</pre></html>";

		if (SEPARATOR.equals(name) || SEPARATOR.equals(desc) || SEPARATOR.equals(cmd))
		{
			xText = "<html><hr width=\"400\"></html>";
		}

		if (comp != null)
		{
			//----------------- JMenuItem
			if (comp instanceof JMenuItem)
			{
				JMenuItem c = (JMenuItem) comp;
				c.setText(xText);
				c.setActionCommand(cmd);
				c.setToolTipText(xTTip);
		
				if (icon != null)
					c.setIcon(SwingUtils.readImageIcon(Version.class, icon));
			}
			//---------------------- JLabel
			else if (comp instanceof JLabel)
			{
				JLabel c = (JLabel) comp;
				c.setText(xText);
				c.setToolTipText(xTTip);
		
				if (icon != null)
					c.setIcon(SwingUtils.readImageIcon(Version.class, icon));
			}
			//---------------------- UNSUPPORTED
			else
				throw new IllegalArgumentException("Unknown component instance was passed. supported types (JMenuItem, JLabel)");
		}

		return xText;
	}

	//-------------------------------------------------------------------
	// Local private classes
	//-------------------------------------------------------------------
	public static class FavoriteCommandEntry
	{
		private VendorType _type        = null;
		private String     _name        = null;
		private String     _description = null;
		private String     _icon        = null;
		private String     _originCmd   = null;
		private String     _formatedCmd = null;

		public FavoriteCommandEntry()
		{
		}
		public FavoriteCommandEntry(VendorType type, String cmd, String name, String description)
		{
			setType(type);
			setName(name);
			setDescription(description);
			setCommand(cmd);
		}
		public FavoriteCommandEntry(VendorType type, String name, String description, String icon, String cmd)
		{
			setType(type);
			setName(name);
			setDescription(description);
			setIcon(icon);
			setCommand(cmd);
		}

		public static FavoriteCommandEntry addSeparator()
		{
			return new FavoriteCommandEntry(VendorType.GENERIC, SEPARATOR, SEPARATOR, null, SEPARATOR);
		}

		public boolean isSeparator()
		{
			return (SEPARATOR.equals(getName()) || SEPARATOR.equals(getDescription()) || SEPARATOR.equals(getOriginCommand()));
		}

		public VendorType getType()            { return _type        == null ? VendorType.GENERIC : _type;        }
		public String     getName()            { return _name        == null ? ""                 : _name;        }
		public String     getDescription()     { return _description == null ? ""                 : _description; }
		public String     getIcon()            { return _icon        == null ? ""                 : _icon;        }
		public String     getFormatedCommand() { return _formatedCmd == null ? ""                 : _formatedCmd; }
		public String     getOriginCommand()   { return _originCmd   == null ? ""                 : _originCmd;   }

		public void setType(VendorType type)
		{
			if (type == null)
				type = VendorType.GENERIC;
			_type = type;
		}

		public void setType(String type)
		{
			if (type == null)
				_type = VendorType.GENERIC;
			else
			{
				_type = VendorType.valueOf(type);
			}
		}

		public void setName(String name)
		{
			if (name == null)
				name = "";
			_name = name;
		}

		public void setDescription(String description)
		{
			if (description == null)
				description = "";
			_description = description;
		}

		public void setIcon(String icon)
		{
			if (icon == null)
				icon = "";
			_icon = icon;
		}

		public void setCommand(String command)
		{
			if (command == null)
				command = "";
			_originCmd   = command;
			_formatedCmd = command.replace('\n', ' ');
		}
		
		public String toXml()
		{
			// Add entry
			StringBuilder sb = new StringBuilder();
				
			sb.append("\n");
			sb.append("    ").append(XML_BEGIN_TAG_FAVORITE_COMMAND_ENTRY).append("\n");
			sb.append("        ").append(XML_BEGIN_SUBTAG_TYPE)       .append(StringUtil.xmlSafe(getType().toString() )).append(XML_END___SUBTAG_TYPE)       .append("\n");
			sb.append("        ").append(XML_BEGIN_SUBTAG_NAME)       .append(StringUtil.xmlSafe(getName()            )).append(XML_END___SUBTAG_NAME)       .append("\n");
			sb.append("        ").append(XML_BEGIN_SUBTAG_DESCRIPTION).append(StringUtil.xmlSafe(getDescription()     )).append(XML_END___SUBTAG_DESCRIPTION).append("\n");
			sb.append("        ").append(XML_BEGIN_SUBTAG_ICON)       .append(StringUtil.xmlSafe(getIcon()            )).append(XML_END___SUBTAG_ICON)       .append("\n");
			sb.append("        ").append(XML_BEGIN_SUBTAG_COMMAND)    .append(StringUtil.xmlSafe(getOriginCommand()   )).append(XML_END___SUBTAG_COMMAND)    .append("\n");
			sb.append("    ").append(XML_END___TAG_FAVORITE_COMMAND_ENTRY).append("\n");

			return sb.toString();
		}
	}

	private static class FavoriteTableModel
	extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;

		private static final String[] TAB_HEADER = {"Pos", "Type", "Name", "Description", "Icon", "Command"};
		private static final int TAB_POS_POS     = 0;
		private static final int TAB_POS_TYPE    = 1;
		private static final int TAB_POS_NAME    = 2;
		private static final int TAB_POS_DESC    = 3;
		private static final int TAB_POS_ICON    = 4;
		private static final int TAB_POS_COMMAND = 5;

		private ArrayList<FavoriteCommandEntry> _rows = new ArrayList<FavoriteCommandEntry>();
		private boolean _hasChanged = false;

		public FavoriteTableModel()
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

		public void clear(boolean fireChange)
		{
			_rows.clear();
			setChanged(true);
			if (fireChange)
				fireTableDataChanged();
		}

		/**
		 *  Moves one or more rows from the inclusive range <code>start</code> to 
		 *  <code>end</code> to the <code>to</code> position in the model. 
		 *  After the move, the row that was at index <code>start</code> 
		 *  will be at index <code>to</code>. 
		 *  This method will send a <code>tableChanged</code> notification
		 *  message to all the listeners. <p>
		 *
		 *  <pre>
		 *  Examples of moves:
		 *  <p>
		 *  1. moveRow(1,3,5);
		 *          a|B|C|D|e|f|g|h|i|j|k   - before
		 *          a|e|f|g|h|B|C|D|i|j|k   - after
		 *  <p>
		 *  2. moveRow(6,7,1);
		 *          a|b|c|d|e|f|G|H|i|j|k   - before
		 *          a|G|H|b|c|d|e|f|i|j|k   - after
		 *  <p> 
		 *  </pre>
		 *
		 * @param   start       the starting row index to be moved
		 * @param   end         the ending row index to be moved
		 * @param   to          the destination of the rows to be moved
		 * @exception  ArrayIndexOutOfBoundsException  if any of the elements would be moved out of the table's range 
		 * 
		 */
		// STOLEN FROM: DefaultTableModel
		public void moveRow(int start, int end, int to) 
		{ 
			int shift = to - start; 
			int first, last; 
			if (shift < 0) { 
				first = to; 
				last = end; 
			}
			else { 
				first = start; 
				last = to + end - start;  
			}
			rotate(_rows, first, last + 1, shift); 

			setChanged(true);
			fireTableRowsUpdated(first, last);
		}
		// STOLEN FROM: DefaultTableModel
		private static void rotate(List<FavoriteCommandEntry> l, int a, int b, int shift)
		{
			int size = b - a;
			int r = size - shift;
			int g = gcd(size, r);
			for (int i = 0; i < g; i++)
			{
				int to = i;
				FavoriteCommandEntry tmp = l.get(a + to);
				for (int from = (to + r) % size; from != i; from = (to + r) % size)
				{
					l.set(a + to, l.get(a + from));
					to = from;
				}
				l.set(a + to, tmp);
			}
		}
		// STOLEN FROM: DefaultTableModel
		private static int gcd(int i, int j)
		{
			return (j == 0) ? i : gcd(j, i % j);
		}

		public void setEntries(ArrayList<FavoriteCommandEntry> entries, boolean fireChange)
		{
			_rows = entries;
			setChanged(true);
			if (fireChange)
				fireTableDataChanged();
		}

		public FavoriteCommandEntry getEntry(int mrow)
		{
			return _rows.get(mrow);
		}

		public ArrayList<FavoriteCommandEntry> getEntries()
		{
			return _rows;
		}

		public void deleteEntry(int row)
		{
			_rows.remove(row);
			setChanged(true);
			fireTableDataChanged();
		}

		public String getCommand(int row)
		{
			FavoriteCommandEntry entry = _rows.get(row);
			return entry.getOriginCommand();
		}

		public void addEntry(FavoriteCommandEntry entry)
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
			case TAB_POS_POS:     return TAB_HEADER[TAB_POS_POS];
			case TAB_POS_TYPE:    return TAB_HEADER[TAB_POS_TYPE];
			case TAB_POS_NAME:    return TAB_HEADER[TAB_POS_NAME];
			case TAB_POS_DESC:    return TAB_HEADER[TAB_POS_DESC];
			case TAB_POS_ICON:    return TAB_HEADER[TAB_POS_ICON];
			case TAB_POS_COMMAND: return TAB_HEADER[TAB_POS_COMMAND];
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
			FavoriteCommandEntry entry = _rows.get(row);
			switch (column)
			{
			case TAB_POS_POS:     return row + 1;
			case TAB_POS_TYPE:    return entry.getType();
			case TAB_POS_NAME:    return entry.getName();
			case TAB_POS_DESC:    return entry.getDescription();
			case TAB_POS_ICON:    return entry.getIcon();
			case TAB_POS_COMMAND: return entry.getFormatedCommand();
			}
			return null;
		}

		@Override
		public Class<?> getColumnClass(int column)
		{
			if (column == TAB_POS_POS) 
				return Integer.class;

			return super.getColumnClass(column);
		}

		private void populateTable()
		{
//			boolean dummyEntries = true;
//			if (dummyEntries)
//			{
//				for (int i=0; i<10; i++)
//				{
//					String    srv  = "TEST_1_DS";
//					String    cmd  = "row "+i+": js fdglsökfj \n gsdlfghjslkfjg hsjkgf ksjhdgf kjgh akjsdhg \naksfdjasgf akjs gfkajgsdh fkajs gfkajshdg fkajs gfkajs gfkajs gfkjasdgh fkjasdh gfkjasgfkjas \ngfkajf g";
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
	implements ActionListener, KeyListener
	{
		private static final long serialVersionUID = 1L;

		public        int  _dialogType     = -1;
		public static int  ADD_DIALOG      = 1;
		public static int  CHANGE_DIALOG   = 2;

		private JButton    _ok             = new JButton("OK");
		private JButton    _cancel         = new JButton("Cancel");

		private FavoriteCommandEntry _return = null;
		private FavoriteCommandEntry _entry  = null;
		
		private JLabel               _preview1_lbl     = new JLabel("Preview");
		private JLabel               _preview2_lbl     = new JLabel("<html><i>How the item will look like</i></html>");

		private JLabel               _command_lbl      = new JLabel("Command");
		private RSyntaxTextAreaX     _command_txt      = new RSyntaxTextAreaX(15, 80);
		private RTextScrollPane      _command_scroll   = new RTextScrollPane(_command_txt);

		private JLabel               _favoriteType_lbl = new JLabel("Type");
		private JComboBox            _favoriteType_cbx = new JComboBox(VendorType.values());

		private JLabel               _favoriteName_lbl = new JLabel("Name");
		private JTextField           _favoriteName_txt = new JTextField();

		private JLabel               _favoriteDesc_lbl = new JLabel("Description");
		private JTextField           _favoriteDesc_txt = new JTextField();

		private JLabel               _favoriteIcon_lbl = new JLabel("Icon");
		private JTextField           _favoriteIcon_txt = new JTextField();

		private AddOrChangeEntryDialog(JDialog owner, FavoriteCommandEntry entry)
		{
			super(owner, "", true);

			_dialogType   = entry == null ? ADD_DIALOG : CHANGE_DIALOG;
			_entry        = entry;
			if (_entry == null)
				_entry = new FavoriteCommandEntry();

			initComponents();
			pack();
		}

		public static FavoriteCommandEntry showDialog(JDialog owner, FavoriteCommandEntry entry)
		{
			AddOrChangeEntryDialog dialog = new AddOrChangeEntryDialog(owner, entry);
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
				setTitle("New Favorite Command");
			}
			else if (_dialogType == CHANGE_DIALOG)
			{
				setTitle("Change Favorite Command");
			}
			else throw new RuntimeException("Unknown Dialog Type");

			_favoriteType_lbl.setToolTipText("<html>GENERIC = Always show this command, else only show this command when connected to a specific vendor.</html>");
			_favoriteType_cbx.setToolTipText(_favoriteType_lbl.getToolTipText());
			_favoriteName_lbl.setToolTipText("<html><i><b>Optional</b></i> Name of the command, if not given the <i>menu text</i> will be the Command itself<br><br>Tip: The preview field below indicates how the <i>menu text</i> will be displayed.</html>");
			_favoriteName_txt.setToolTipText(_favoriteName_lbl.getToolTipText());
			_favoriteDesc_lbl.setToolTipText("<html><i><b>Optional</b></i> A Short Description of the command, which be displayed in the <i>menu text</i><br><br>Tip: The preview field below indicates how the <i>menu text</i> will be displayed.</html>");
			_favoriteDesc_txt.setToolTipText(_favoriteDesc_lbl.getToolTipText());
			_favoriteIcon_lbl.setToolTipText("<html><i><b>Optional</b></i> Name of a file, which holds a Graphical Icon that will be displayed before the <i>menu text</i><br><br>Tip: The preview field below indicates how the <i>menu text</i> will be displayed.</html>");
			_favoriteIcon_txt.setToolTipText(_favoriteIcon_lbl.getToolTipText());
			_preview1_lbl    .setToolTipText("<html>A Preview of what the <i>menu text</i> will look like.</html>");
			_preview2_lbl    .setToolTipText(_preview2_lbl.getToolTipText());

			_command_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);

			panel.add(_favoriteType_lbl,  "");
			panel.add(_favoriteType_cbx,  "pushx, growx, wrap");
			
			panel.add(_favoriteName_lbl,  "");
			panel.add(_favoriteName_txt,  "pushx, growx, wrap");
			
			panel.add(_favoriteDesc_lbl,  "");
			panel.add(_favoriteDesc_txt,  "pushx, growx, wrap");
			
			panel.add(_favoriteIcon_lbl,  "");
			panel.add(_favoriteIcon_txt,  "pushx, growx, wrap");
			
			panel.add(_preview1_lbl,      "");
			panel.add(_preview2_lbl,      "pushx, growx, wrap 15");
			
//			panel.add(_command_lbl,       "wrap");
			panel.add(_command_scroll,    "span, push, grow, wrap");
			
			// ADD the OK, Cancel, Apply buttons
			panel.add(_ok,     "tag ok,     gap top 20, skip, split, bottom, right, pushx");
			panel.add(_cancel, "tag cancel,                   split, bottom");

			setContentPane(panel);

			_favoriteType_cbx.setMaximumRowCount(50);

			// Fill in some start values
			_favoriteType_cbx.setSelectedItem(_entry.getType());
			_favoriteName_txt.setText(_entry.getName());
			_favoriteDesc_txt.setText(_entry.getDescription());
			_favoriteIcon_txt.setText(_entry.getIcon());
			_command_txt     .setText(_entry.getOriginCommand());
			
			// ADD KEY listeners
			_favoriteName_txt.addKeyListener(this);
			_favoriteDesc_txt.addKeyListener(this);
			_favoriteIcon_txt.addKeyListener(this);
			_command_txt     .addKeyListener(this);
			
			// ADD ACTIONS TO COMPONENTS
			_ok           .addActionListener(this);
			_cancel       .addActionListener(this);
		}

		@Override public void keyPressed (KeyEvent e) {}
		@Override public void keyTyped   (KeyEvent e) {}
		@Override public void keyReleased(KeyEvent e)
		{
			setBasicInfo( _preview2_lbl, _favoriteName_txt.getText(), _favoriteDesc_txt.getText(), _favoriteIcon_txt.getText(), _command_txt.getText() );
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			Object source = e.getSource();

			// --- BUTTON: OK ---
			if (_ok.equals(source))
			{
				_entry.setType       (_favoriteType_cbx.getSelectedItem()+"");
				_entry.setName       (_favoriteName_txt.getText());
				_entry.setDescription(_favoriteDesc_txt.getText());
				_entry.setIcon       (_favoriteIcon_txt.getText());
				_entry.setCommand    (_command_txt     .getText());

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
					_command_txt.requestFocus();
				}
			};
			SwingUtilities.invokeLater(deferredAction);
		}
	}
	/*---------------------------------------------------
	** END: class AddOrChangeEntryDialog
	**---------------------------------------------------
	*/

	
	
	private static boolean isVendorType(String connectedToProductName, VendorType vendorType)
	{
		if (vendorType == null || connectedToProductName == null)
			return false;

		if (VendorType.GENERIC.equals(vendorType)) 
			return true;
		
		if      (VendorType.ASE   .equals(vendorType) && DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_SYBASE_ASE  )) return true;
		else if (VendorType.ASA   .equals(vendorType) && DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_SYBASE_ASA  )) return true;
		else if (VendorType.IQ    .equals(vendorType) && DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_SYBASE_IQ   )) return true;
		else if (VendorType.RS    .equals(vendorType) && DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_SYBASE_RS   )) return true;
		else if (VendorType.RAX   .equals(vendorType) && DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_SYBASE_RAX  )) return true;
		else if (VendorType.RSDRA .equals(vendorType) && DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_SYBASE_RSDRA)) return true;
		else if (VendorType.HANA  .equals(vendorType) && DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_HANA        )) return true;
		else if (VendorType.MAXDB .equals(vendorType) && DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_MAXDB       )) return true;
		else if (VendorType.ORACLE.equals(vendorType) && DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_ORACLE      )) return true;
		else if (VendorType.MSSQL .equals(vendorType) && DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_MSSQL       )) return true;
		else if (VendorType.DB2   .equals(vendorType) && DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_DB2_UX      )) return true;
		else if (VendorType.HSQL  .equals(vendorType) && DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_HSQL        )) return true;
		else if (VendorType.MYSQL .equals(vendorType) && DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_MYSQL       )) return true;
		else if (VendorType.DERBY .equals(vendorType) && DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_DERBY       )) return true;
		
		return false;
	}



	/**
	 * Hide all menu entries that should not be visible based on what DBMS Vendor we are connected to
	 * @param jcomp
	 * @param connectedToProductName
	 */
	public static void setVisibilityForPopupMenu(JComponent jcomp, String connectedToProductName)
	{
		if (jcomp == null)
			return;

		int count = jcomp.getComponentCount();
		if (jcomp instanceof JMenu)
			count = ((JMenu)jcomp).getMenuComponentCount();

		for (int i=0; i<count; i++)
		{
			Component c = (jcomp instanceof JMenu) ? ((JMenu)jcomp).getMenuComponent(i) : jcomp.getComponent(i);
			if (c instanceof JComponent)
			{
				JComponent jc = (JComponent) c;
				Object oVendorType = jc.getClientProperty(PROPKEY_VENDOR_TYPE);
				if (oVendorType != null && oVendorType instanceof VendorType)
				{
					VendorType vendorType = (VendorType)oVendorType;
					boolean visible = isVendorType(connectedToProductName, vendorType);
					jc.setVisible(visible);
				}

				// If it's a Menu, it will probably have children, so lets check them
				if (jc instanceof JMenu)
					setVisibilityForPopupMenu((JMenu)jc, connectedToProductName);
			}
		}
	}
}

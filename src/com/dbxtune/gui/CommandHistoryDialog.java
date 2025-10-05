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
package com.dbxtune.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.dbxtune.Version;
import com.dbxtune.gui.swing.GTable;
import com.dbxtune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.dbxtune.utils.ArrayUtils;
import com.dbxtune.utils.AseSqlScriptReader;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.FileTail;
import com.dbxtune.utils.FileUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CommandHistoryDialog
extends JFrame
//extends JDialog
implements ChangeListener, ActionListener, FocusListener, KeyListener
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = 1L;

	private JSplitPane           _splitPane              = new JSplitPane();

	private JPanel               _topPanel               = null;

	private JPanel               _cmdPanel               = null;
	private RSyntaxTextAreaX     _cmd_txt                = new RSyntaxTextAreaX();
	private RTextScrollPane      _cmd_scroll             = new RTextScrollPane(_cmd_txt);

	private JPanel               _statusBarPanel         = null;
	private JButton              _historyFile_but        = new JButton("...");
//	private JLabel               _historyFile_lbl        = new JLabel();
	private JTextArea            _historyFile_txt        = new JTextArea();

	private JButton              _exec_but               = new JButton("Exec");
	private JCheckBox            _moveToLatestEntry_cbx  = new JCheckBox("Move to Latest Entry", DEFAULT_MOVE_TO_LATEST_ENTRY);

	private JLabel               _historySize_lbl        = new JLabel("History Size");
	private SpinnerNumberModel   _historySize_spm        = new SpinnerNumberModel(DEFAULT_HISTORY_SIZE, 10, 99999, 10);
	private JSpinner             _historySize_sp         = new JSpinner(_historySize_spm);

	private JCheckBox            _showOnlyLocalCmds_chk  = new JCheckBox("Show only local commands", DEFAULT_SHOW_ONLY_LOCAL_COMMANDS);

	private JLabel               _cmdTextFilter_lbl      = new JLabel("Filter");
	private JTextField           _cmdTextFilter_txt      = new JTextField();
	private JCheckBox            _cmdTextFilterInSensitive_chk = new JCheckBox("Case in-sensitive", DEFAULT_FILTER_CASE_IN_SENSITIVE);
	
	private JButton              _flashParent_but        = new JButton("Owner");

	private CommandsTableModel   _tm                     = null;
//	private JXTable              _table                  = null;
	private GTable               _table                  = null;

	private HistoryExecutor      _owner;
	private Window               _parentWindow;

	private String               _historyFileName       = null;

	private HistoryFileWatchDog  _historyFileWatchDog   = null;

	private static final String  ACTION_EXECUTE                   = "ACTION_EXECUTE";

	private static final String  PROPKEY_MOVE_TO_LATEST_ENTRY     = "CommandHistory.history.move.to.latest.entry";
	private static final boolean DEFAULT_MOVE_TO_LATEST_ENTRY     = true;
	
	private static final String  PROPKEY_HISTORY_SIZE             = "CommandHistory.history.size";
	private static final int     DEFAULT_HISTORY_SIZE             = 10000;
	
	private static final String  PROPKEY_SHOW_ONLY_LOCAL_COMMANDS = "CommandHistory.history.show.only.local.commands";
	private static final boolean DEFAULT_SHOW_ONLY_LOCAL_COMMANDS = false;
	
	private static final String  PROPKEY_SPLITPANE_DIV_LOC        = "CommandHistory.splitpane.divider.location";
	private static final int     DEFAULT_SPLITPANE_DIV_LOC        = SwingUtils.hiDpiScale(300);
	
	private static final String  PROPKEY_FILE_MAX_SIZE_KB         = "CommandHistory.file.max.size.kb";
	private static final int     DEFAULT_FILE_MAX_SIZE_KB         = 1024 * 10; // 10MB

	private static final String  PROPKEY_FILE_SAVE_ENTRIES        = "CommandHistory.file.save.entries";
	private static final int     DEFAULT_FILE_SAVE_ENTRIES        = 100;

	private static final String  PROPKEY_ENTRY_LIMIT_SIZE         = "CommandHistory.entry.limit.size";
	private static final int     DEFAULT_ENTRY_LIMIT_SIZE         = 10*1024; // 10KB

	private static final String  PROPKEY_FILTER_CASE_IN_SENSITIVE = "CommandHistory.filter.caseInSensitive";
	private static final boolean DEFAULT_FILTER_CASE_IN_SENSITIVE = true;

	private static final String  PROPKEY_showDialogWriteHistoryFail = "CommandHistory.showDialogOnWriteHistoryFail";


	public interface HistoryExecutor
	{
		/** Execute the history entry */
		public void   historyExecute(String cmd);
		
		/** save the place where the history file is to be stored at */
		public void   saveHistoryFilename(String filename);
		
		/** Get what history file to use */
		public String getHistoryFilename();
		
		/** get source identification, this would typically be ManagementFactory.getRuntimeMXBean().getName() */
		public String getSourceId();
	}

	public CommandHistoryDialog(HistoryExecutor owner, Window window)
	{
//		super(window, "Command History", ModalityType.MODELESS);
		super();
		_owner = owner;
		_parentWindow = window;
		init();
		setFileName(owner.getHistoryFilename());
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
		setTitle("Command History - " + _owner.getSourceId()); // Set window title
		
		// Set the icon, if we "just" do setIconImage() on the JDialog
		// it will not be the "correct" icon in the Alt-Tab list on Windows
		// So we need to grab the owner, and set that since the icon is grabbed from the owner...
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/command_history.png");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/command_history_32.png");
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

		addWindowListener(new WindowAdapter()
		{
//			@Override
//			public void windowClosing(WindowEvent e)
//			{
//				if (_historyFileWatchDog != null)
//					_historyFileWatchDog.stop();
//				destroy();
//			}
			@Override
			public void windowActivated(WindowEvent e)
			{
				if (_parentWindow != null)
				{
					// Blink the parent window, to get GUI feedback of what Window this History is connected to.
					// But I have NO idea how to do this...
//					_parentWindow.blink();
				}
			}
		});

		// If the file is to big, lets make it smaller
		fixHistoryFileSize(_owner.getHistoryFilename());
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

		panel.setToolTipText(
			"<html>" +
			"Commands from all SQLWindows are stored in the shared history file '"+getFileName()+"'.<br>" +
			"The <i>Source</i> column in the table indicates from which SQLWindow Session it was executed in.<br>" +
			"<br>" +
			"<b>Tip</b>: Drag command(s) from any History Table Window and Drop them on the 'execute' button in any SQLWindow to execute them." +
			"</html>");

		_exec_but             .setToolTipText("<html>Execute the selected row(s) or the text in the editor below</html>");
		_moveToLatestEntry_cbx.setToolTipText("<html>Automatically select the last executed entry in the history.</html>");
		_historySize_lbl      .setToolTipText("<html>How many commands should be in the history list.<br> <b>Note</b>: Hidden entries will be included in this size.</html>");
		_historySize_sp       .setToolTipText(_historySize_lbl.getToolTipText());
		_showOnlyLocalCmds_chk.setToolTipText("<html>Hide Commands executed from other SQL Windows Sessions.<br>Commands executed in other sessions will have a light-gray background color.</html>");
		_cmdTextFilter_lbl    .setToolTipText("<html>Use Regular expession to only show commands that contains the text.</html>");
		_cmdTextFilter_txt    .setToolTipText(_cmdTextFilter_lbl.getToolTipText());
		_cmdTextFilterInSensitive_chk.setToolTipText("<html>Should the regular expresion be case in-sensitive or sensitive<br>This simply adds '(?i)' at the start of the search text</html>");
		_flashParent_but      .setToolTipText("<html>Blink/Flash parent window that opened this Command History Window<br>It's in the <i>owner</i> window any commands will be executed.</html>");


		panel.add(createHistoryTable(),    "push, grow, wrap");

		panel.add(_exec_but,               "gap 10 10 0 10, span, split");  // gap left [right] [top] [bottom]

		panel.add(_moveToLatestEntry_cbx,  "gapleft 20");

		panel.add(_historySize_lbl,        "gapleft 20");
		panel.add(_historySize_sp,         "");

		panel.add(_showOnlyLocalCmds_chk,  "gapleft 20");

		panel.add(_cmdTextFilter_lbl,      "gapleft 20");
		panel.add(_cmdTextFilter_txt,      "pushx, growx");
		panel.add(_cmdTextFilterInSensitive_chk, "");
		
//		panel.add(new JLabel(),            "pushx, growx"); // dummy to space out...
		panel.add(_flashParent_but,        "gapleft 20");

		// Focus action listener
		
		// action
		_exec_but             .addActionListener(this);
		_moveToLatestEntry_cbx.addActionListener(this);
		_showOnlyLocalCmds_chk.addActionListener(this);
		_cmdTextFilter_txt    .addActionListener(this);
		_cmdTextFilterInSensitive_chk.addActionListener(this);
		_historySize_sp       .addChangeListener(this); // note: a change listener
		_flashParent_but      .addActionListener(this);

		_cmdTextFilter_txt    .addKeyListener(this);

		// Action Commands
//		_exec_but.setActionCommand(ACTION_EXECUTE);
		_exec_but.setAction(_executeHistoryRowsAction);
		
		return panel;
	}

	private JPanel createHistoryTable()
	{
		JPanel panel = SwingUtils.createPanel("History", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

//		// Create the TableModel, which will be passed to GTable, if this is done later with setMode(), we might have some problems due to hidden columns etc...
//		// hhmmm... even if we did the above: when hiding/showing columns first time after a restart we still got stacktrace...
//		_tm = new CommandsTableModel();

//		_table = new JXTable()
//		_table = new GTable(_tm)
		_table = new GTable()
		{
			private static final long serialVersionUID = 1L;

			// 
			// TOOL TIP for: TABLE HEADERS
			//
//		    protected JTableHeader createDefaultTableHeader() 
//		    {
//		        return new JXTableHeader(columnModel);
//		    }
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
						if (mIndex == CommandsTableModel.TAB_POS_ROW)     toolTip = "<html>Row number in the history table.</html>";
						if (mIndex == CommandsTableModel.TAB_POS_TIME)    toolTip = "<html>When did we execute the command.</html>";
						if (mIndex == CommandsTableModel.TAB_POS_SERVER)  toolTip = "<html>At what server was the command executed.</html>";
						if (mIndex == CommandsTableModel.TAB_POS_USER)    toolTip = "<html>ASE Username this command was executed with.</html>";
						if (mIndex == CommandsTableModel.TAB_POS_DB)      toolTip = "<html>What was the database context when executing the command.</html>";
						if (mIndex == CommandsTableModel.TAB_POS_COMMAND) toolTip = "<html>The command that was executed.</html>";
						if (mIndex == CommandsTableModel.TAB_POS_SOURCE)  toolTip = 
							"<html>" +
							  "At what SQL Window instance was the command executed.<br>" +
							  "Gray entries wasn't executed from this instance. (they might be old entries or executed from another running SQLWindow)<br>" +
							  "SQL Window Instance ID can be seen at the top right corner of The SQL Window Tool.<br>" +
							"</html>";
						if (mIndex == CommandsTableModel.TAB_POS_COUNT)   toolTip = 
							"<html>" +
							  "Number of times the command has been re-executed after each other.<br>" +
							  "You can also read this value as <i>number of sequential repetitions</i>, where <code>command</code>, <code>server</code> and <code>source</code> is repeated in the history file.<br>" +
							"</html>";
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
				int vcol = CommandsTableModel.TAB_POS_COMMAND;
				int vrow = rowAtPoint(p);
				if (vcol >= 0 && vrow >= 0)
				{
					int mcol = CommandsTableModel.TAB_POS_COMMAND; //super.convertColumnIndexToModel(vcol);
					int mrow = super.convertRowIndexToModel(vrow);

					TableModel tm = getModel();

					tip = tm.getValueAt(mrow, mcol) + "";

					if (tm instanceof CommandsTableModel)
					{
						CommandsTableModel ctm = (CommandsTableModel) tm;
						
						String time   = ctm.getValueAt(mrow, CommandsTableModel.TAB_POS_TIME)   + "";
						String srv    = ctm.getValueAt(mrow, CommandsTableModel.TAB_POS_SERVER) + "";
						String user   = ctm.getValueAt(mrow, CommandsTableModel.TAB_POS_USER)   + "";
						String db     = ctm.getValueAt(mrow, CommandsTableModel.TAB_POS_DB)     + "";
						String source = ctm.getValueAt(mrow, CommandsTableModel.TAB_POS_SOURCE) + "";
						String cmd    = ctm.getCommand(mrow);
//						private static final String[] TAB_HEADER = {"Row", "Time", "Server", "User", "Db", "Source", "#", "Command"};

						tip = "<html>" +
						      "<table align=\"left\" border=0 cellspacing=0 cellpadding=0>" +
						        "<tr> <td><b>Time:   </b></td> <td>" + time   + "</td> </tr>" +
						        "<tr> <td><b>Server: </b></td> <td>" + srv    + "</td> </tr>" +
						        "<tr> <td><b>User:   </b></td> <td>" + user   + "</td> </tr>" +
						        "<tr> <td><b>DbName: </b></td> <td>" + db     + "</td> </tr>" +
						        "<tr> <td><b>Source: </b></td> <td>" + source + "</td> </tr>" +
						      "</table>" +
						      "<hr>" +
						      "<pre>"+cmd+"</pre>" +
						      "</html>";
					}
				}
				return tip;
			}
			
			@Override
			public void packAll()
			{
				// call super to do main work
				super.packAll();
				
				// Parent would be a JScrollPane or similar, if we dont have a parent we cant continue.
				if (getParent() == null)
					return;

				// Make COMMAND Text Column smaller, if it's there
				int cmdColPos = -1;
				int colsWidth = 0;
				for (int c=0; c<getColumnCount(); c++)
				{
					if ( CommandsTableModel.TAB_HEADER[CommandsTableModel.TAB_POS_COMMAND].equals(getColumnName(c)))
						cmdColPos = c;
					else
						colsWidth += getColumn(c).getPreferredWidth();
				}

				int maxWidth = getParent().getWidth() - colsWidth;
				if (cmdColPos >= 0 && maxWidth > 0)
					packColumn(cmdColPos, -1, maxWidth);
			}
		};

		// Click on a row
		_table.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e) 
			{
				if (e.getClickCount() == 2)
				{
					_exec_but.doClick();
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
				int vcol = CommandsTableModel.TAB_POS_COMMAND;
	
				if (vcol >= 0 && vrow >= 0)
				{
					int mcol = CommandsTableModel.TAB_POS_COMMAND; //_table.convertColumnIndexToModel(col);
					int mrow = _table.convertRowIndexToModel(vrow);

					TableModel tm = _table.getModel();

					String cmd = tm.getValueAt(mrow, mcol) + "";
					if (tm instanceof CommandsTableModel)
					{
						CommandsTableModel ctm = (CommandsTableModel) tm;
						cmd = ctm.getCommand(mrow);
					}

					_cmd_txt.setText(cmd);
					_cmd_txt.setCaretPosition(0);
				}
			}
		});

		// POPUP Menu
		//FIXME: create a popup menu
//		_panelPopupMenu = createPanelPopupMenu();
//		_panel.setComponentPopupMenu(_panelPopupMenu);
		_table.setComponentPopupMenu(createPanelPopupMenu());

		
		// In the JTable: on enter, execute the current query
		String actionName = "TableEnterAction";
		KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		_table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, actionName);
		_table.getActionMap().put(actionName, _executeHistoryRowsAction);

		// In the JTable: on DELETE, delete current row
		actionName = "TableDeleteRow";

		key = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
		_table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, actionName);

		key = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
		_table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, actionName);

		_table.getActionMap().put(actionName, _deleteHistoryRowsAction);

		// Ctrl-c: copy all selected rows
		_table.getActionMap().put("copy", _copyHistoryRowsAction);

		// Highlighter
		_table.addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String tSource = (String) adapter.getValue(adapter.getColumnIndex("Source"));
				String oSource = _owner.getSourceId();
				if ( tSource != null && oSource != null && ! oSource.equals(tSource))
					return true;
				return false;
			}
		}, new Color(233, 233, 233), null));  // Color(233, 233, 233) = Very Light Gray


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
		
		_tm = new CommandsTableModel();
		_tm.populateTable();

		_table.setName("CommandHistoryTable"); // GTable want's a name so it can remember column: sort and placement
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

	private JPanel createCmdPanel()
	{
		JPanel panel = SwingUtils.createPanel("Trace Command Log", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		// Tooltip
//		panel       .setToolTipText("Tail of the Log");
//		_logTail_txt.setToolTipText("Tail of the Log");
		
		// Command Execution: ADD Ctrl+e, F5, F9
		_cmd_txt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_E,  Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), ACTION_EXECUTE);
		_cmd_txt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), ACTION_EXECUTE);
		_cmd_txt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), ACTION_EXECUTE);

		_cmd_txt.getActionMap().put(ACTION_EXECUTE, _executeHistoryRowsAction);
//		_cmd_txt.getActionMap().put(ACTION_EXECUTE, new AbstractAction(ACTION_EXECUTE)
//		{
//			private static final long serialVersionUID = 1L;
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				_exec_but.doClick();
//			}
//		});

		_cmd_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);

		_statusBarPanel = createStatusBarPanel();
		
		panel.add(_cmd_scroll,     "grow, push, wrap");
		panel.add(_statusBarPanel, "gap 5, growx, pushx, wrap");

		RSyntaxUtilitiesX.installRightClickMenuExtentions(_cmd_scroll, this);

		return panel;
	}

	private static final String HISTORY_FILE_TOOLTIP_TEMPLATE = "<html>Set a new history file to use. It may be shared by several users, if you want to share history between several users.<br>Current file: <code> HISTORY_FILE_NAME </code></html>"; 

	private JPanel createStatusBarPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		
//		_historyFile_lbl.setToolTipText("<html>History file used for the moment. Press <i>button on the left</i> to change history file.</html>");
		_historyFile_but.setToolTipText(HISTORY_FILE_TOOLTIP_TEMPLATE);

		_historyFile_txt.setToolTipText("<html>History file used for the moment. Press <i>button on the left</i> to change history file.</html>");
		_historyFile_txt.setEditable(false);
		_historyFile_txt.setBackground( _historySize_lbl.getBackground() );
		_historyFile_txt.setFont(       _historySize_lbl.getFont() );

		// History file button
		_historyFile_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/command_history_file.png"));
		_historyFile_but.setText(null);
		_historyFile_but.setContentAreaFilled(false);
		_historyFile_but.setMargin( new Insets(0,0,0,0) );

		panel.add(_historyFile_but, "");
//		panel.add(_historyFile_lbl, "pushx, growx");
		panel.add(_historyFile_txt, "pushx, growx");
		
		_historyFile_but.addActionListener(this);

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

		//------------------------------------------------------------
		JMenuItem  mi = new JMenuItem("Execute Selected Row(s)");
		mi.setAction(_executeHistoryRowsAction);
		list.add(mi);

		//------------------------------------------------------------
		mi = new JMenuItem("Delete Selected Row(s)");
		mi.setAction(_deleteHistoryRowsAction);
		list.add(mi);

		//------------------------------------------------------------
		mi = new JMenuItem("Copy Selected Row(s)");
		mi.setAction(_copyHistoryRowsAction);
		list.add(mi);

		return list;
	}

	
	/*---------------------------------------------------
	** BEGIN: implementing: ChangeListener, ActionListener, FocusListener, FileTail.TraceListener, Memory.MemoryListener
	**---------------------------------------------------
	*/	

	// implementing: ChangeListener
	private int   _historySize_last   = ((Number)_historySize_spm.getValue()).intValue();
	private Timer _defferedSizeChange = new Timer(500, new DefferedSizeChange());

	private class DefferedSizeChange
	implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			_defferedSizeChange.stop();

			int historySize_now = ((Number)_historySize_spm.getValue()).intValue();
			if (historySize_now > _historySize_last)
			{
				//default icon, custom title
				int n = JOptionPane.showConfirmDialog(CommandHistoryDialog.this,
						"Do you want to re-read the history file, to include older objects?",
						"Re-Read History",
						JOptionPane.YES_NO_OPTION);
	
				if( n == JOptionPane.YES_OPTION )
				{
//					JOptionPane.showMessageDialog(null, "Not yet implemented...");
					_historyFileWatchDog.stop();
					_tm.clear(false);

					_historyFileWatchDog = new HistoryFileWatchDog(getFileName());
					_historyFileWatchDog.start();
				}
			}
			if (historySize_now < _historySize_last)
			{
				// Remove entries
				_tm.setSize(historySize_now);
			}

			_historySize_last = ((Number)_historySize_spm.getValue()).intValue();
			
			setComponentVisibility();
			saveProps();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		Object source = e.getSource();
		if (_historySize_sp.equals(source))
		{
			if ( _defferedSizeChange.isRunning() )
				_defferedSizeChange.restart();
			else
				_defferedSizeChange.start();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source    = e.getSource();
		
		if (_showOnlyLocalCmds_chk.equals(source) || _cmdTextFilter_txt.equals(source) || _cmdTextFilterInSensitive_chk.equals(source))
		{
			setTableFilters();
		}
		
		if (_historyFile_but.equals(source))
		{
			String dir = getFileName();

			JFileChooser fc = new JFileChooser(dir);
			int returnVal = fc.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) 
			{
				setFileName(fc.getSelectedFile().getAbsolutePath());
			}
			
		}

		if(_flashParent_but.equals(source))
		{
			if (_parentWindow != null)
				_parentWindow.toFront();
			// FIXME: is there a better way to "flash" the parent window
			this.toFront();
		}

		setComponentVisibility();
		saveProps();
	}
	@Override
	public void focusGained(FocusEvent e)
	{
	}
	@Override
	public void focusLost(FocusEvent e)
	{
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
		setTableFilters();
	}
	@Override public void keyPressed(KeyEvent e) {}
	@Override public void keyReleased(KeyEvent e) {}

	private void setTableFilters()
	{
		List<RowFilter<Object,Object>> filters = new ArrayList<RowFilter<Object,Object>>(2);
//		filters.add(RowFilter.regexFilter("foo"));
//		filters.add(RowFilter.regexFilter("bar"));
//		RowFilter<Object,Object> andFilter = RowFilter.andFilter(filters);

		if (_showOnlyLocalCmds_chk.isSelected())
		{
			String lookupFieldText = _owner.getSourceId();
			int    sourceColPos    = _table.convertColumnIndexToView(CommandsTableModel.TAB_POS_SOURCE);
			
			try
			{
				if (sourceColPos > 0)
					filters.add(RowFilter.regexFilter(lookupFieldText, sourceColPos));
				else
					filters.add(RowFilter.regexFilter(lookupFieldText));
			} 
			catch (PatternSyntaxException pse)
			{
				_logger.warn("JXTable: setRowFilter(): Incorrect pattern syntax '"+lookupFieldText+"'.");
			}
		}
		
		
		if (StringUtil.hasValue(_cmdTextFilter_txt.getText()))
		{
			boolean caseInsensitive = _cmdTextFilterInSensitive_chk.isSelected();
			String lookupFieldText = _cmdTextFilter_txt.getText();
			int    sourceColPos    = _table.convertColumnIndexToView(CommandsTableModel.TAB_POS_COMMAND);

			if (caseInsensitive)
				lookupFieldText = "(?i)" + lookupFieldText;
			try
			{
				if (sourceColPos > 0)
					filters.add(RowFilter.regexFilter(lookupFieldText, sourceColPos));
				else
					filters.add(RowFilter.regexFilter(lookupFieldText));
			} 
			catch (PatternSyntaxException pse)
			{
				_logger.warn("JXTable: setRowFilter(): Incorrect pattern syntax '"+lookupFieldText+"'.");
			}
		}

		// Set the filters...
        if ( filters.isEmpty() )
			_table.setRowFilter(null);
		else
			_table.setRowFilter(RowFilter.andFilter(filters));
	}
	
	private void setComponentVisibility()
	{
		boolean visible = _table.getRowCount() > 0;
		boolean enabled = visible;

		// remove text fields if no rows...
		if ( ! enabled )
			_cmd_txt.setText("");
		
		// Set what should be enabled/disabled
		_exec_but.setEnabled(enabled);
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
		conf.setProperty(PROPKEY_MOVE_TO_LATEST_ENTRY,     _moveToLatestEntry_cbx.isSelected());
		conf.setProperty(PROPKEY_HISTORY_SIZE,             _historySize_spm      .getNumber().intValue());
		conf.setProperty(PROPKEY_SHOW_ONLY_LOCAL_COMMANDS, _showOnlyLocalCmds_chk.isSelected());
		conf.setProperty(PROPKEY_FILTER_CASE_IN_SENSITIVE, _cmdTextFilterInSensitive_chk.isSelected());
		
		
		//------------------
		// SPLIT PANE
		//------------------
		conf.setLayoutProperty(PROPKEY_SPLITPANE_DIV_LOC, _splitPane.getDividerLocation());

		
		//------------------
		// WINDOW
		//------------------
		conf.setLayoutProperty("CommandHistory.dialog.window.width",  this.getSize().width);
		conf.setLayoutProperty("CommandHistory.dialog.window.height", this.getSize().height);
		conf.setLayoutProperty("CommandHistory.dialog.window.pos.x",  this.getLocationOnScreen().x);
		conf.setLayoutProperty("CommandHistory.dialog.window.pos.y",  this.getLocationOnScreen().y);

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
		_moveToLatestEntry_cbx       .setSelected( conf.getBooleanProperty(PROPKEY_MOVE_TO_LATEST_ENTRY,     DEFAULT_MOVE_TO_LATEST_ENTRY));
		_historySize_spm             .setValue(    conf.getIntProperty(    PROPKEY_HISTORY_SIZE,             DEFAULT_HISTORY_SIZE));
//		_showOnlyLocalCmds_cbx       .setSelected( conf.getBooleanProperty(PROPKEY_SHOW_ONLY_LOCAL_COMMANDS, DEFAULT_SHOW_ONLY_LOCAL_COMMANDS));
		_cmdTextFilterInSensitive_chk.setSelected( conf.getBooleanProperty(PROPKEY_FILTER_CASE_IN_SENSITIVE, DEFAULT_FILTER_CASE_IN_SENSITIVE));

		_historySize_last   = ((Number)_historySize_spm.getValue()).intValue();
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
		int width  = conf.getLayoutProperty("CommandHistory.dialog.window.width",  SwingUtils.hiDpiScale(900));
		int height = conf.getLayoutProperty("CommandHistory.dialog.window.height", SwingUtils.hiDpiScale(740));
		int x      = conf.getLayoutProperty("CommandHistory.dialog.window.pos.x",  -1);
		int y      = conf.getLayoutProperty("CommandHistory.dialog.window.pos.y",  -1);

		if (width != -1 && height != -1)
			this.setSize(width, height);

		if (x != -1 && y != -1)
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
				this.setLocation(x, y);

		//------------------
		// SPLIT PANE
		//------------------
		int dividerLocation = conf.getLayoutProperty(PROPKEY_SPLITPANE_DIV_LOC, DEFAULT_SPLITPANE_DIV_LOC);
		_splitPane.setDividerLocation(dividerLocation);

	}
	/*---------------------------------------------------
	** END: implementing saveProps & loadProps
	**---------------------------------------------------
	*/	
	
	/**
	 * Get last entry
	 * @return
	 */
	public String getLastCommand()
	{
		if (_tm == null)
			return null;

		return _tm.getLastCommand();
	}

	public void setFileName(String filename)
	{
		_historyFileName = filename;
		_historyFile_but.setToolTipText( HISTORY_FILE_TOOLTIP_TEMPLATE.replace("HISTORY_FILE_NAME", filename) );
//		_historyFile_lbl.setText(filename);
		_historyFile_txt.setText(filename);
		
		_owner.saveHistoryFilename(_historyFileName);
		
		// and we need to do X stuff here
		// - clean history table
		// - stop/start reader
		// - Parse the new file.
		_tm.clear(false);

		// Stop reader if one exist
		if (_historyFileWatchDog != null)
			_historyFileWatchDog.stop();
		
		// Create/Start a new of the History file reader, which will parse the new file
		_historyFileWatchDog = new HistoryFileWatchDog(getFileName());
		_historyFileWatchDog.start();
	}

	public String getFileName()
	{
		//_historyFileName = _owner.getHistoryFilename();
		return _historyFileName;
	}

	/**
	 * Add entry to the history.
	 * @param server   Server of where this was executed
	 * @param username User which executed the command
	 * @param dbname   In what database were the users executing it from 
	 * @param cmd      executed command
	 */
	public void addEntry(String server, String username, String dbname, String cmd)
	{
		// Dont append empty stuff
		if (StringUtil.isNullOrBlank(cmd))
			return;

		// Is it to big, DONT APPEND
		int cmdLen = cmd.length();
		int limitLen = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_ENTRY_LIMIT_SIZE, DEFAULT_ENTRY_LIMIT_SIZE);
		if (cmdLen > limitLen)
		{
			_logger.info("Adding SQL entry to the history was skipped. It was to big, size limit is set to "+limitLen+" and the Command length was "+cmdLen+" bytes.");
			return;
		}
		// Only append to the history file if it's a NEW SQL Statement
		// Counter in GUI will be incremented, but the history file will nor be appended
		//
		// If the appendToHistoryFile() hit's any IO or Runtime problems.
		// - don't stop the caller from continuing...
		// - show errormessage
		// - still try to add it to the History table (but skipping the history file)
		try
		{
			// Add to history file
			// The "tail" process will read the entry and add it to the history list
			appendToHistoryFile(server, cmd, username, dbname);
		}
		catch (Throwable e)
		{
			_logger.warn("Problems writing to the history file '"+getFileName()+"'. Continuing without appending record to the history file. It will still be available in the History Table, just not persisted to the history file. Caught: "+e);

			// Add it to the GUI at least...
			CommandHistoryEntry entry = new CommandHistoryEntry(null, server, username, dbname, cmd, null, null);
			addEntry(entry);

			// GUI popup with the error, AND a JCheckBox to "not show this message again"
			boolean showInfo = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_showDialogWriteHistoryFail, true);
			if (showInfo)
			{
				String msgHtml = 
					"<html>" +
					"<h2>Problems writing to the history file</h2>" +
					"Filename: <code>"+getFileName()+"</code><br>" +
					"<br>" +
					"Continuing without appending record to the history file. <br>" +
					"The Command will still be available in the History Table, just not persisted to the history file.<br>" +
					"</html>";

				// Create a check box that will be passed to the message
				JCheckBox chk = new JCheckBox("Show History Append Errors in the future.", showInfo);
				chk.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
						if (conf == null)
							return;
						conf.setProperty(PROPKEY_showDialogWriteHistoryFail, ((JCheckBox)e.getSource()).isSelected());
						conf.save();
					}
				});

				SwingUtils.showWarnMessageExt(_parentWindow, "Problems Saving to history file.",
						msgHtml, chk, e);
			}
		}
	}

	/**
	 * Internally called from the tail reader 
	 * @param entry
	 */
	private void addEntry(CommandHistoryEntry entry)
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
			// remove entry, if above history size
			while (_tm.getRowCount() > _historySize_spm.getNumber().intValue())
				_tm.removeOldestEntry();
	
			// Make the view aware of the change.
			_tm.fireTableDataChanged();

			// Move to entry
			if (_moveToLatestEntry_cbx.isSelected())
			{
				int vrow = _table.getRowCount() - 1;
				if (vrow > 0)
				{
					vrow = _table.convertRowIndexToView(vrow);
	
					_table.getSelectionModel().setSelectionInterval(vrow, vrow);
				
					// make it visible if it's not 
					_table.scrollRowToVisible(vrow);
				}
			}
			_table.packAll();
	
			setComponentVisibility();
	
			_defferedEntryAdd.stop();
		}
	}

	/**
	 * Append Records to the shared history file.
	 */
	private void appendToHistoryFile(String server, String cmd, String username, String dbname)
	throws IOException
	{
//		try
//		{
			@SuppressWarnings("resource")
			RandomAccessFile raf = new RandomAccessFile(getFileName(), "rw");
			FileChannel channel = raf.getChannel();

			boolean emptyFile = false;
			if (channel.size() == 0)
				emptyFile = true;

			try 
			{
				CommandHistoryEntry entry = new CommandHistoryEntry(null, server, username, dbname, cmd, null, _owner.getSourceId());

				// Get an exclusive lock on the whole file
				FileLock lock = channel.lock();

				try 
				{
					// ----------------------------------------------------
					// Build a Buffer of what too write to the history file
					// Add Beginning of the file, if it's an empty file
					StringBuilder sb = new StringBuilder();
					if (emptyFile)
					{
						sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
						sb.append("\n");
						sb.append(XML_BEGIN_TAG_COMMAND_HISTORY_LIST).append("\n");
						sb.append("\n");
					}

					// BEFORE WE WRITE/APPEND to the file
					// we need to FIND the closing </CommandHistoryList> at the end.
					// Set the location at this entry and write from here
					// Yes, since it's a XML document we *need* the BEGIN/END tags to match...
					// This so the file still looks like:
					// --------------------------------------
					// <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
					// <CommandHistoryList>
					//     <CommandHistoryEntry>
					//         ...the sub tags goes here...
					//     </CommandHistoryEntry>
					// </CommandHistoryList>
					// --------------------------------------
					// To do this:
					// - read last 1024 bytes (as a ByteBuffer, since it's UTF-8)
					// - get index of where </CommandHistoryList> is located (on byte/char level)
					//   Note: a String can't be used (since UTF-8 chars can occupy more than 1-byte)
					// - set file position to index of the byte array
					// - fill in spaces from here to end of file (otherwise 'tail' wont work)
					// - write the information
					int readSize = 1024;
					long readPos = channel.size() - readSize;
					if (readPos < 0) // we can't start a read prior to file start... 
						readPos = 0;
					channel.position(readPos);

					long writePos = channel.size();
					String fillStr = ""; // used to replace </CommandHistoryList>...EOF with spaces

					ByteBuffer buf = ByteBuffer.allocate(readSize);
					int bytesRead = channel.read(buf);
					if (bytesRead > 0)
					{
						// Find last </CommandHistoryList> tag
						byte[] lastChunk = buf.array();
						char[] findTagBytes = XML_END___TAG_COMMAND_HISTORY_LIST.toCharArray();
	
						int tagPos = ArrayUtils.indexOfArray(lastChunk, findTagBytes);
						if (tagPos >= 0)
						{
							// make up new: write position (where to start write)
							writePos = channel.size() - bytesRead + tagPos;
							
							// add spaces to replace the </CommandHistoryList> + to-end-of-file
							// If not 'tail' wont pick up the changes... or start to read at the wrong place...
							int fillStrLen = readSize - tagPos;
							fillStr = StringUtil.replicate(" ", fillStrLen);
						}
					}
					// Position to where we will write the StringBuilder
					channel.position( writePos );

					//--------------------------------------
					// Compose what to append at the end
					
					// add filler to "rewrite" empty chars at the end, so that 'tail' will work. 
					sb.append(fillStr);

					// Add the Command Entry
					sb.append(entry.toXml());

					// Add the closing </CommandHistoryList> tag
					sb.append("\n");
					sb.append(XML_END___TAG_COMMAND_HISTORY_LIST).append("\n");

//					System.out.println("BEGIN: WRITE(writePos="+writePos+", channel.size()="+channel.size()+"):");
//					System.out.println("WRITE: |"+sb.toString()+"|");
//					System.out.println("END__: WRITE:");

					//-----------------------------------------
					// Now write the Buffer to end of the file.
					ByteBuffer byteBuffer = ByteBuffer.wrap(sb.toString().getBytes(Charset.forName("UTF-8")));
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
//		}
//		catch (IOException e)
//		{
//			_logger.warn("Problems writing to history file '"+getFileName()+"'. No history entry was added.", e);
//		}
	}

	/**
	 * Write a new history file...
	 * @param fileName       Name of the file
	 * @param list           Entries to write to the file
	 * @throws IOException   When we had problems to write.
	 */
	private void newHistoryFile(String fileName, ArrayList<CommandHistoryEntry> list)
	throws IOException
	{
		try
		{
			_logger.warn("Writing a new history file '"+fileName+"'. Entries that will be written "+list.size()+".");
			@SuppressWarnings("resource")
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
					sb.append(XML_BEGIN_TAG_COMMAND_HISTORY_LIST).append("\n");
					sb.append("\n");

					//-----------------------------------------
					// Write header
					ByteBuffer byteBuffer;
					byteBuffer = ByteBuffer.wrap(sb.toString().getBytes(Charset.forName("UTF-8")));
					channel.write(byteBuffer);


					//--------------------------------------
					// Write all the history tags in the input list
					for (CommandHistoryEntry entry : list)
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
					sb.append(XML_END___TAG_COMMAND_HISTORY_LIST).append("\n");

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
			_logger.warn("Problems writing to history file '"+getFileName()+"'. No history entry was added. Caught: "+e);
			throw e;
		}
	}
	
	//---------------------------------------------------
	// Below is XML tags
	//---------------------------------------------------
	private static final String       XML_TAG_COMMAND_HISTORY_LIST  = "CommandHistoryList";
	private static final String XML_BEGIN_TAG_COMMAND_HISTORY_LIST  = "<"  + XML_TAG_COMMAND_HISTORY_LIST + ">";
	private static final String XML_END___TAG_COMMAND_HISTORY_LIST  = "</" + XML_TAG_COMMAND_HISTORY_LIST + ">";
	
	private static final String       XML_TAG_COMMAND_HISTORY_ENTRY = "CommandHistoryEntry";
	private static final String XML_BEGIN_TAG_COMMAND_HISTORY_ENTRY = "<"  + XML_TAG_COMMAND_HISTORY_ENTRY + ">";
	private static final String XML_END___TAG_COMMAND_HISTORY_ENTRY = "</" + XML_TAG_COMMAND_HISTORY_ENTRY + ">";

	private static final String       XML_SUBTAG_SERVER_NAME        = "ServerName";
	private static final String XML_BEGIN_SUBTAG_SERVER_NAME        = "<"  + XML_SUBTAG_SERVER_NAME + ">";
	private static final String XML_END___SUBTAG_SERVER_NAME        = "</" + XML_SUBTAG_SERVER_NAME + ">";

	private static final String       XML_SUBTAG_USER_NAME          = "UserName";
	private static final String XML_BEGIN_SUBTAG_USER_NAME          = "<"  + XML_SUBTAG_USER_NAME + ">";
	private static final String XML_END___SUBTAG_USER_NAME          = "</" + XML_SUBTAG_USER_NAME + ">";

	private static final String       XML_SUBTAG_DB_NAME            = "DbName";
	private static final String XML_BEGIN_SUBTAG_DB_NAME            = "<"  + XML_SUBTAG_DB_NAME + ">";
	private static final String XML_END___SUBTAG_DB_NAME            = "</" + XML_SUBTAG_DB_NAME + ">";

	private static final String       XML_SUBTAG_EXEC_TIME          = "ExecTime";
	private static final String XML_BEGIN_SUBTAG_EXEC_TIME          = "<"  + XML_SUBTAG_EXEC_TIME + ">";
	private static final String XML_END___SUBTAG_EXEC_TIME          = "</" + XML_SUBTAG_EXEC_TIME + ">";

	private static final String       XML_SUBTAG_UUID               = "UUID";
	private static final String XML_BEGIN_SUBTAG_UUID               = "<"  + XML_SUBTAG_UUID + ">";
	private static final String XML_END___SUBTAG_UUID               = "</" + XML_SUBTAG_UUID + ">";

	private static final String       XML_SUBTAG_SOURCE             = "Source";
	private static final String XML_BEGIN_SUBTAG_SOURCE             = "<"  + XML_SUBTAG_SOURCE + ">";
	private static final String XML_END___SUBTAG_SOURCE             = "</" + XML_SUBTAG_SOURCE + ">";
	
	private static final String       XML_SUBTAG_COMMAND            = "Command";
	private static final String XML_BEGIN_SUBTAG_COMMAND            = "<"  + XML_SUBTAG_COMMAND + ">";
	private static final String XML_END___SUBTAG_COMMAND            = "</" + XML_SUBTAG_COMMAND + ">";

	//-------------------------------------------------------------------
	// HISTORY FILE WATCHDOG
	//-------------------------------------------------------------------
	private class HistoryFileWatchDog
	extends DefaultHandler
	implements FileTail.TraceListener
	{
		private String   _fileName  = null;
		private FileTail _fileTail  = null;
		private int      _sleepTime = 200;
		
		StringBuilder    _buffer = new StringBuilder();

		HistoryFileXmlParser _parser = null;

		private HistoryFileWatchDog(String fileName)
		{
			_fileName = fileName;
			_fileTail = new FileTail(_fileName, true);
			_fileTail.setSleepTime(_sleepTime);
			_fileTail.setLocalFileCharset(Charset.forName("UTF-8"));

			_fileTail.addTraceListener(this);

			_parser = new HistoryFileXmlParser();
		}

		public void start()
		{
			try
			{
				if ( ! _fileTail.doFileExist() )
					_fileTail.createFile();
			}
			catch (Exception e)
			{
				_logger.warn("Problems creating the file '"+_fileName+"'");
				return;
			}
			_fileTail.start();
		}

		public void stop()
		{
			if (_fileTail != null)
				_fileTail.shutdown();
		}

		@Override
		public void newTraceRow(String row)
		{
			boolean startTag = (row.indexOf(XML_BEGIN_TAG_COMMAND_HISTORY_ENTRY) >= 0);
			boolean endTag   = (row.indexOf(XML_END___TAG_COMMAND_HISTORY_ENTRY) >= 0);

			// on start tag: reset the buffer,
			if (startTag)
			{
				// Reset buffer for new entry to be read.
				_buffer.setLength(0);
			}

			// Append to buffer 
			_buffer.append(row).append("\n");

			if (endTag)
			{
				CommandHistoryEntry entry = _parser.parseEntry(_buffer.toString());
				addEntry(entry);
				_buffer.setLength(0);
			}
		}
	}

	//-------------------------------------------------------------------
	// XML PARSER
	//-------------------------------------------------------------------
	private class HistoryFileXmlParser
	extends DefaultHandler
	{
		private SAXParserFactory _saxFactory      = SAXParserFactory.newInstance();
		private SAXParser        _saxParser       = null;
		private int              _tailRecordCount = -1; // if above 0, only save last X records in the list


		private HistoryFileXmlParser()
		{
			try
			{
				_saxParser = _saxFactory.newSAXParser();
			}
			catch (SAXException e)
			{
				_logger.warn("Problems Creating History XML Parser '"+getFileName()+"'. Caught: "+e, e);
			}
			catch (ParserConfigurationException e)
			{
				_logger.warn("Problems Creating History XML Parser '"+getFileName()+"'. Caught: "+e, e);
			}
		}

		public CommandHistoryEntry parseEntry(String entry)
		{
			_lastEntry       = null;
			_entryList       = null;
			_tailRecordCount = -1;
			try
			{
				_saxParser.parse(new InputSource(new StringReader(entry)), this);
			}
			catch (SAXException e)
			{
				_logger.warn("Problems Parsing Command history Entry '"+entry+"'. Caught: "+e, e);
			}
			catch (IOException e)
			{
				_logger.warn("Problems Parsing Command history Entry '"+entry+"'. Caught: "+e, e);
			}
			return _lastEntry;
		}

//		public ArrayList<CommandHistoryEntry> parseFile(String fileName)
//		{
//			return parseFile(fileName, -1);
//		}
		/**
		 *  Parse a file
		 *  @param filename        Name of the file
		 *  @param tailRecordCount Number of records to place in the returned list (last X records in file). <br>
		 *                         -1 = read all entries
		 *  
		 *  @return A ArrayList with CommandHistoryEntries
		 */
		public ArrayList<CommandHistoryEntry> parseFile(String fileName, int tailRecordCount)
		{
			_lastEntry       = null;
			_entryList       = null;
			_tailRecordCount = tailRecordCount;

			try
			{
//				_saxParser.parse(new InputSource(new FileReader(fileName)), this);
				_saxParser.parse(new File(fileName), this);
			}
			catch (SAXException e)
			{
				_logger.warn("Problems Parsing Command history File '"+fileName+"'. Caught: "+e, e);
			}
			catch (IOException e)
			{
				_logger.warn("Problems Parsing Command history File '"+fileName+"'. Caught: "+e, e);
			}
			return _entryList;
		}

		//----------------------------------------------------------
		// START: XML Parsing code
		//----------------------------------------------------------
		private StringBuilder                  _xmlTagBuffer = new StringBuilder();
		private CommandHistoryEntry            _lastEntry    = null;
		private ArrayList<CommandHistoryEntry> _entryList    = null;

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
			if (XML_TAG_COMMAND_HISTORY_ENTRY.equals(qName))
			{
				_lastEntry = new CommandHistoryEntry();
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) 
		throws SAXException
		{
//			System.out.println("SAX: endElement: qName='"+qName+"', _xmlTagBuffer="+_xmlTagBuffer);
			if (XML_TAG_COMMAND_HISTORY_ENTRY.equals(qName))
			{
				if (_entryList == null)
					_entryList = new ArrayList<CommandHistoryEntry>();
				_entryList.add(_lastEntry);

				// Only keep the tail
				if (_tailRecordCount > 0)
				{
					while (_entryList.size() > _tailRecordCount)
						_entryList.remove(0);
				}
			}
			else
			{
				if      (XML_SUBTAG_COMMAND    .equals(qName)) _lastEntry.setCommand   (_xmlTagBuffer.toString().trim());
				else if (XML_SUBTAG_EXEC_TIME  .equals(qName)) _lastEntry.setExecTime  (_xmlTagBuffer.toString().trim());
				else if (XML_SUBTAG_SERVER_NAME.equals(qName)) _lastEntry.setServerName(_xmlTagBuffer.toString().trim());
				else if (XML_SUBTAG_USER_NAME  .equals(qName)) _lastEntry.setUsername  (_xmlTagBuffer.toString().trim());
				else if (XML_SUBTAG_DB_NAME    .equals(qName)) _lastEntry.setDbname    (_xmlTagBuffer.toString().trim());
				else if (XML_SUBTAG_SOURCE     .equals(qName)) _lastEntry.setSource    (_xmlTagBuffer.toString().trim());
				else if (XML_SUBTAG_UUID       .equals(qName)) _lastEntry.setUUID      (_xmlTagBuffer.toString().trim());
			}
			_xmlTagBuffer.setLength(0);
		}
		//----------------------------------------------------------
		// END: XML Parsing code
		//----------------------------------------------------------
	}

	/**
	 * If the history file is to big, make it smaller.
	 * <ul>
	 *   <li>If size is above #KB                    <br><code>config: CommandHistory.file.max.size.kb=##</code> </li>
	 *   <li>Keep last # entries in current history  <br><code>config: CommandHistory.file.save.entries=##</code> </li>
	 * </ul>
	 */
	private void fixHistoryFileSize(String fileName)
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		int rSizeInKb = conf.getIntProperty(PROPKEY_FILE_MAX_SIZE_KB,  DEFAULT_FILE_MAX_SIZE_KB);
		int saveCount = conf.getIntProperty(PROPKEY_FILE_SAVE_ENTRIES, DEFAULT_FILE_SAVE_ENTRIES);  // save XX entries in the new file.
//rSizeInKb = 10;

		String dateStr = new SimpleDateFormat("yyyy-MM-dd_HHmm").format(new Date(System.currentTimeMillis()));

		String fromFileStr   = fileName;
		String backupFileStr = fileName + ".backup."+dateStr+".xml";

		File fromFile   = new File(fromFileStr);
//		File backupFile = new File(backupFileStr);

		// if size is to big
		// 1: copy the current file into a "backup file"
		// 2: open current file: remember last X entries
		// 3: Write back: the remembered X entries
		long fromFileSize = fromFile.length();
		if (fromFileSize > rSizeInKb*1024)
		try
		{
			_logger.info("History file will be reduced to contain only "+saveCount+" entries. Current File Size is "+(fromFileSize/1024)+" KB. The reduction only happens if file is larger than "+rSizeInKb+" KB.");
			_logger.info("History file will be copied from '"+fromFileStr+"', to '"+backupFileStr+"'.");
			FileUtils.copy(fromFileStr, backupFileStr);

			// Parse the file into a List
			HistoryFileXmlParser parser = new HistoryFileXmlParser();
			ArrayList<CommandHistoryEntry> entryList = parser.parseFile(fromFileStr, saveCount);

			// Write the List
			newHistoryFile(fromFileStr, entryList);

			_logger.info("History file reduction was successfully for file '"+fromFileSize+"'.");
		}
		catch (IOException e)
		{
			_logger.warn("Problem copy the history file to a backup. from '"+fromFileStr+"', to '"+backupFileStr+"'. Caught: "+e, e);
		}
	}

	
	//-------------------------------------------------------------------
	// Local ACTION classes
	//-------------------------------------------------------------------
	private DeleteHistoryRowsAction  _deleteHistoryRowsAction  = new DeleteHistoryRowsAction();
	private ExecuteHistoryRowsAction _executeHistoryRowsAction = new ExecuteHistoryRowsAction();
	private CopyHistoryRowsAction    _copyHistoryRowsAction    = new CopyHistoryRowsAction();

	private class DeleteHistoryRowsAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Delete Selected Row(s)";
		private static final String ICON = "images/delete.png";
//		private static final ImageIcon ICON = SwingUtils.readImageIcon(Version.class, "images/delete.png");

		public DeleteHistoryRowsAction()
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
			if (tm instanceof CommandsTableModel)
			{
				CommandsTableModel ctm = (CommandsTableModel) tm;
				
				// Delete all rows, but start at the end of the model
				for (int r=mrows.length-1; r>=0; r--)
					ctm.deleteEntry(mrows[r]);

				setComponentVisibility();
			}
		}
	}

	private class ExecuteHistoryRowsAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Execute Selected Row(s)";
		private static final String ICON = "images/exec.png";
//		private        final ImageIcon ICON = SwingUtils.readImageIcon(Version.class, "images/exec.png");

		public ExecuteHistoryRowsAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			// if we have several rows selected in the tab
			// load the row one by one in the _cmd_txt
			String execStr = _cmd_txt.getSelectedText();
			
			int[] vrows = _table.getSelectedRows();

			// If you have selected text and has multiple rows, selected in the list, show warning message...
			if (!StringUtil.isNullOrBlank(execStr) && vrows.length > 1)
			{
				String htmlMsg = 
					"<html>" +
					"<h2>Execution Aborted</h2>" +
					"Reason:" +
					"<ul>" +
					"  <li>Multiple rows are selected in the History List</li>" +
					"  <li>And text is <i>selected<i>/<i>marked</i> in the history editor</li>" +
					"</ul>" +
					"This combination of execution is not supported." +
					"</html>";
				SwingUtils.showInfoMessage(CommandHistoryDialog.this, "multiple rows selected", htmlMsg);
				return;
			}

			// Multiple rows is selected in the history list
			if (vrows.length > 1)
			{
//				System.out.println("MULTI ROW EXECUTION: rows: "+rows.length+", "+rows);

				// Copy rows into a String List
				// BECAUSE if history size is low, rows will be deleted from the history table at execution
				ArrayList<String> execList = new ArrayList<String>();

				int[] mrows = new int[vrows.length];
				for (int r=0; r<vrows.length; r++)
					mrows[r] = _table.convertRowIndexToModel(vrows[r]);

				TableModel tm = _table.getModel();
				if (tm instanceof CommandsTableModel)
				{
					CommandsTableModel ctm = (CommandsTableModel) tm;
					
					// Get each selected command and add it to a list, to be executed in next step
					for (int r=0; r<mrows.length; r++)
						execList.add( ctm.getCommand(mrows[r]) );
				}
				// Now: execute everything in the list
				for (String str : execList)
					_owner.historyExecute(str);

			}
			else // one or no selection, use the text in the editor
			{
				if (StringUtil.isNullOrBlank(execStr))
					execStr = _cmd_txt.getText();

				_owner.historyExecute(execStr);
			}
		} // end: method
	}

	private class CopyHistoryRowsAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Copy Selected Row(s)";
		private static final String ICON = "images/copy.png";
//		private        final ImageIcon ICON = SwingUtils.readImageIcon(Version.class, "images/copy.png");

		public CopyHistoryRowsAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			SwingUtils.setClipboardContents(getCommandsForSelectedRows());
		}
	}


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
		if (tm instanceof CommandsTableModel)
		{
			CommandsTableModel ctm = (CommandsTableModel) tm;
			
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


	//-------------------------------------------------------------------
	// Local private classes
	//-------------------------------------------------------------------
//	private static class CommandHistoryEntry
	private class CommandHistoryEntry
	{
		private String _execTime    = null;
		private int    _execCount   = 1;
		private String _server      = null;
		private String _username    = null;
		private String _dbname      = null;
		private String _source      = null;
		private String _uuid        = null;
		private String _originCmd   = null;
		private String _formatedCmd = null;

		public CommandHistoryEntry()
		{
		}
		public CommandHistoryEntry(String time, String server, String username, String dbname, String cmd, String uuid, String source)
		{
			setExecTime(time);
			setServerName(server);
			setUsername(username);
			setDbname(dbname);
			setCommand(cmd);
			setUUID(uuid);
			setSource(source);
		}

		public String getUUID()            { return _uuid        == null ? "" : _uuid;        }
		public String getSource()          { return _source      == null ? "" : _source;      }
		public String getServerName()      { return _server      == null ? "" : _server;      }
		public String getUsername()        { return _username    == null ? "" : _username;    }
		public String getDbname()          { return _dbname      == null ? "" : _dbname;      }
		public String getExecTime()        { return _execTime    == null ? "" : _execTime;    }
		public int    getExecCount()       { return _execCount; }
		public String getFormatedCommand() { return _formatedCmd == null ? "" : _formatedCmd; }
		public String getOriginCommand()   { return _originCmd   == null ? "" : _originCmd;   }

		public void setUUID(String uuid)
		{
			if (uuid == null)
				uuid = UUID.randomUUID().toString();
			_uuid = uuid;
		}

		public void setSource(String source)
		{
			if (source == null)
				source = _owner.getSourceId();
			_source = source;
		}

		public void setServerName(String server)
		{
			if (server == null)
				server = "";
			_server = server;
		}

		public void setUsername(String username)
		{
			if (username == null)
				username = "";
			_username = username;
		}

		public void setDbname(String dbname)
		{
			if (dbname == null)
				dbname = "";
			_dbname = dbname;
		}

		public void setExecTime(String execTime)
		{
			if (execTime == null)
				execTime = new Timestamp(System.currentTimeMillis()).toString();
			_execTime = execTime;
		}

		public void setCommand(String command)
		{
			if (command == null)
				command = "";
			_originCmd   = command;
			_formatedCmd = command.replace('\n', ' ');
		}
		
		public boolean isReExecuted(CommandHistoryEntry newEntry)
		{
			// Lets not care about extra chars and newlines in the compare
			String thisOriginCmd =     this.getOriginCommand().trim();
			String  newOriginCmd = newEntry.getOriginCommand().trim();
			if (    thisOriginCmd       .equals(newOriginCmd) 
			     && this.getServerName().equals(newEntry.getServerName()) 
			     && this.getUsername()  .equals(newEntry.getUsername())
			     && this.getDbname()    .equals(newEntry.getDbname())
			     && this.getSource()    .equals(newEntry.getSource())
			   )
			{
				_execTime = newEntry._execTime;
				_execCount++;
				return true;
			}
			return false;
		}

		public String toXml()
		{
			// Add entry
			StringBuilder sb = new StringBuilder();
				
			sb.append("\n");
			sb.append("    ").append(XML_BEGIN_TAG_COMMAND_HISTORY_ENTRY).append("\n");
			sb.append("        ").append(XML_BEGIN_SUBTAG_SERVER_NAME).append(StringUtil.xmlSafe(getServerName()   )).append(XML_END___SUBTAG_SERVER_NAME).append("\n");
			sb.append("        ").append(XML_BEGIN_SUBTAG_USER_NAME)  .append(StringUtil.xmlSafe(getUsername()     )).append(XML_END___SUBTAG_USER_NAME)  .append("\n");
			sb.append("        ").append(XML_BEGIN_SUBTAG_DB_NAME)    .append(StringUtil.xmlSafe(getDbname()       )).append(XML_END___SUBTAG_DB_NAME)    .append("\n");
			sb.append("        ").append(XML_BEGIN_SUBTAG_EXEC_TIME)  .append(StringUtil.xmlSafe(getExecTime()     )).append(XML_END___SUBTAG_EXEC_TIME)  .append("\n");
			sb.append("        ").append(XML_BEGIN_SUBTAG_UUID)       .append(StringUtil.xmlSafe(getUUID()         )).append(XML_END___SUBTAG_UUID)       .append("\n");
			sb.append("        ").append(XML_BEGIN_SUBTAG_SOURCE)     .append(StringUtil.xmlSafe(getSource()       )).append(XML_END___SUBTAG_SOURCE)     .append("\n");
			sb.append("        ").append(XML_BEGIN_SUBTAG_COMMAND)    .append(StringUtil.xmlSafe(getOriginCommand())).append(XML_END___SUBTAG_COMMAND)    .append("\n");
			sb.append("    ").append(XML_END___TAG_COMMAND_HISTORY_ENTRY).append("\n");

			return sb.toString();
		}
	}

	private static class CommandsTableModel
	extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;

		private static final String[] TAB_HEADER = {"Row", "Time", "Server", "User", "Db", "Source", "#", "Command"};
		private static final int TAB_POS_ROW     = 0;
		private static final int TAB_POS_TIME    = 1;
		private static final int TAB_POS_SERVER  = 2;
		private static final int TAB_POS_USER    = 3;
		private static final int TAB_POS_DB      = 4;
		private static final int TAB_POS_SOURCE  = 5;
		private static final int TAB_POS_COUNT   = 6;
		private static final int TAB_POS_COMMAND = 7;

		private ArrayList<CommandHistoryEntry> _rows = new ArrayList<CommandHistoryEntry>();

		public CommandsTableModel()
		{
		}

		public void clear(boolean fireChange)
		{
			_rows.clear();
			if (fireChange)
				fireTableDataChanged();
		}

		public void deleteEntry(int row)
		{
			_rows.remove(row);
			fireTableDataChanged();
		}

		public void removeOldestEntry()
		{
			_rows.remove(0);
		}

		public void setSize(int newSize)
		{
			while( _rows.size() > newSize)
				_rows.remove(0);
			fireTableDataChanged();
		}

		public boolean isLastCommandReExecuted(CommandHistoryEntry newEntry)
		{
			// If the list is empty, no need to check.
			if (_rows.size() == 0)
				return false;
			
			int entryIndex = _rows.size() - 1;
			CommandHistoryEntry lastEntry = _rows.get(entryIndex);

			return lastEntry.isReExecuted(newEntry);
		}

		public String getCommand(int row)
		{
			CommandHistoryEntry entry = _rows.get(row);
			return entry.getOriginCommand();
		}

		public String getLastCommand()
		{
			if (_rows.isEmpty())
				return null;

			return getCommand( getRowCount() - 1 );
		}

		public void addEntry(CommandHistoryEntry entry)
		{
			if ( ! isLastCommandReExecuted(entry) )
				_rows.add(entry);
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
			case TAB_POS_ROW:     return TAB_HEADER[TAB_POS_ROW];
			case TAB_POS_TIME:    return TAB_HEADER[TAB_POS_TIME];
			case TAB_POS_SERVER:  return TAB_HEADER[TAB_POS_SERVER];
			case TAB_POS_USER:    return TAB_HEADER[TAB_POS_USER];
			case TAB_POS_DB:      return TAB_HEADER[TAB_POS_DB];
			case TAB_POS_SOURCE:  return TAB_HEADER[TAB_POS_SOURCE];
			case TAB_POS_COUNT:   return TAB_HEADER[TAB_POS_COUNT];
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
			CommandHistoryEntry entry = _rows.get(row);
			switch (column)
			{
			case TAB_POS_ROW:     return row + 1;
			case TAB_POS_TIME:    return entry.getExecTime();
			case TAB_POS_SERVER:  return entry.getServerName();
			case TAB_POS_USER:    return entry.getUsername();
			case TAB_POS_DB:      return entry.getDbname();
			case TAB_POS_SOURCE:  return entry.getSource();
			case TAB_POS_COUNT:   return entry.getExecCount();
			case TAB_POS_COMMAND: return entry.getFormatedCommand();
			}
			return null;
		}

		@Override
		public Class<?> getColumnClass(int column)
		{
			if (column == TAB_POS_ROW) 
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
//					String    cmd  = "row "+i+": js fdgls�kfj \n gsdlfghjslkfjg hsjkgf ksjhdgf kjgh akjsdhg \naksfdjasgf akjs gfkajgsdh fkajs gfkajshdg fkajs gfkajs gfkajs gfkjasdgh fkjasdh gfkjasgfkjas \ngfkajf g";
//
//					addEntry(srv, cmd, null, null, null);
//				}
//			}
		}
	}
}

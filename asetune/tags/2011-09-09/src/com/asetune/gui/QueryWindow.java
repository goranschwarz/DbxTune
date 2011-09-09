/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.JTableHeader;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jdesktop.swingx.JXTable;

import com.asetune.DebugOptions;
import com.asetune.Version;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionFactory;
import com.asetune.utils.Debug;
import com.asetune.utils.JavaVersion;
import com.asetune.utils.Logging;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.xmenu.TablePopupFactory;
import com.sybase.jdbcx.EedInfo;
import com.sybase.jdbcx.SybConnection;
import com.sybase.jdbcx.SybMessageHandler;

/**
 * This class creates a Swing GUI that allows the user to enter a SQL query.
 * It then obtains a ResultSetTableModel for the query and uses it to display
 * the results of the query in a scrolling JTable component.
 **/
public class QueryWindow
//	extends JFrame
//	extends JDialog
	implements SybMessageHandler, ConnectionFactory
{
	private static Logger _logger = Logger.getLogger(QueryWindow.class);
	private static final long serialVersionUID = 1L;

	public enum WindowType 
	{
		/** Create the QueryWindow using a JFrame, meaning it would have a Icon in the Task bar, but from CmdLine */
		CMDLINE_JFRAME, 

		/** Create the QueryWindow using a JFrame, meaning it would have a Icon in the Task bar */
		JFRAME, 

		/** Create the QueryWindow using a JDialog, meaning it would NOT have a Icon in the Task bar */
		JDIALOG, 

		/** Create the QueryWindow using a JDialog, with modal option set to true. */
		JDIALOG_MODAL 
	}

	private Connection  _conn            = null;
//	private JTextArea	_query           = new JTextArea();        // A field to enter a query in
	private RSyntaxTextArea	_query       = new RSyntaxTextArea();        // A field to enter a query in
	private RTextScrollPane _queryScroll     = new RTextScrollPane(_query);
	private JButton     _exec            = new JButton("Exec");    // Execute the
	private JButton     _copy            = new JButton("Copy");    // Copy All resultsets to clipboard
	private JCheckBox   _showplan        = new JCheckBox("GUI Showplan", false);
	private JCheckBox   _rsInTabs        = new JCheckBox("Resultsets in Tabbed Panel", false);
	private JComboBox   _dbs_cobx        = new JComboBox();
	private JPanel      _resPanel        = new JPanel();
	private JScrollPane _resPanelScroll  = new JScrollPane(_resPanel);
	private JLabel	    _msgline         = new JLabel("");	     // For displaying messages
	private JSplitPane  _splitPane       = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	private int         _lastTabIndex    = -1;
	private boolean     _closeConnOnExit = true;
	private Font        _aseMsgFont      = null;
	private ArrayList<JComponent> _resultCompList  = null;

	// The base Window can be either a JFrame or a JDialog
	private Window      _window          = null;
	private JFrame      _jframe          = null;
	private JDialog     _jdialog         = null;

	/**
	 * Constructor for CommandLine parameters
	 * @param cmd
	 * @throws Exception
	 */
	public QueryWindow(CommandLine cmd)
	throws Exception
	{
		Version.setAppName("AseSqlWindow");
		
		// Create store dir if it did not exists.
		File appStoreDir = new File(Version.APP_STORE_DIR);
		if ( ! appStoreDir.exists() )
		{
			if (appStoreDir.mkdir())
				System.out.println("Creating directory '"+appStoreDir+"' to hold various files for "+Version.getAppName());
		}

		
		// -----------------------------------------------------------------
		// CHECK/SETUP information from the CommandLine switches
		// -----------------------------------------------------------------
		final String CONFIG_FILE_NAME      = System.getProperty("CONFIG_FILE_NAME",      "asetune.properties");
		final String USER_CONFIG_FILE_NAME = System.getProperty("USER_CONFIG_FILE_NAME", "asetune.user.properties");
		final String TMP_CONFIG_FILE_NAME  = System.getProperty("TMP_CONFIG_FILE_NAME",  "asesqlw.save.properties");
		final String ASESQLW_HOME          = System.getProperty("ASESQLW_HOME");
		
		String defaultPropsFile     = (ASESQLW_HOME          != null) ? ASESQLW_HOME          + "/" + CONFIG_FILE_NAME      : CONFIG_FILE_NAME;
		String defaultUserPropsFile = (Version.APP_STORE_DIR != null) ? Version.APP_STORE_DIR + "/" + USER_CONFIG_FILE_NAME : USER_CONFIG_FILE_NAME;
		String defaultTmpPropsFile  = (Version.APP_STORE_DIR != null) ? Version.APP_STORE_DIR + "/" + TMP_CONFIG_FILE_NAME  : TMP_CONFIG_FILE_NAME;

		// Compose MAIN CONFIG file (first USER_HOME then ASETUNE_HOME)
		String filename = Version.APP_STORE_DIR + "/" + CONFIG_FILE_NAME;
		if ( (new File(filename)).exists() )
			defaultPropsFile = filename;

		String propFile        = cmd.getOptionValue("config",     defaultPropsFile);
		String userPropFile    = cmd.getOptionValue("userConfig", defaultUserPropsFile);
		String tmpPropFile     = cmd.getOptionValue("tmpConfig",  defaultTmpPropsFile);

		// Check if the configuration file exists
		if ( ! (new File(propFile)).exists() )
			throw new FileNotFoundException("The configuration file '"+propFile+"' doesn't exists.");

		// -----------------------------------------------------------------
		// CHECK JAVA JVM VERSION
		// -----------------------------------------------------------------
		int javaVersionInt = JavaVersion.getVersion();
		if (   javaVersionInt != JavaVersion.VERSION_NOTFOUND 
		    && javaVersionInt <  JavaVersion.VERSION_1_6
		   )
		{
			System.out.println("");
			System.out.println("===============================================================");
			System.out.println(" "+Version.getAppName()+" needs a runtime JVM 1.6 or higher.");
			System.out.println(" java.version = " + System.getProperty("java.version"));
			System.out.println(" which is parsed into the number: " + JavaVersion.getVersion());
			System.out.println("---------------------------------------------------------------");
			System.out.println("");
			throw new Exception(Version.getAppName()+" needs a runtime JVM 1.6 or higher.");
		}

		// The SAVE Properties...
		Configuration appSaveProps = new Configuration(tmpPropFile);
		Configuration.setInstance(Configuration.USER_TEMP, appSaveProps);

		// Get the USER properties that could override CONF
		Configuration appUserProps = new Configuration(userPropFile);
		Configuration.setInstance(Configuration.USER_CONF, appUserProps);

		// Get the "OTHER" properties that has to do with LOGGING etc...
		Configuration appProps = new Configuration(propFile);
		Configuration.setInstance(Configuration.SYSTEM_CONF, appProps);

		// Set the Configuration search order when using the: Configuration.getCombinedConfiguration()
		Configuration.setSearchOrder(
			Configuration.USER_TEMP,    // First
			Configuration.USER_CONF,    // second
			Configuration.SYSTEM_CONF); // Third

		//---------------------------------------------------------------
		// OK, lets get ASE user/passwd/server/dbname
		//---------------------------------------------------------------
		String aseUsername = System.getProperty("user.name"); 
		String asePassword = "";
		String aseServer   = System.getenv("DSQUERY");
		String aseDbname   = "";
		String sqlQuery    = "";
		if (cmd.hasOption('U'))	aseUsername = cmd.getOptionValue('U');
		if (cmd.hasOption('P'))	asePassword = cmd.getOptionValue('P');
		if (cmd.hasOption('S'))	aseServer   = cmd.getOptionValue('S');
		if (cmd.hasOption('D'))	aseDbname   = cmd.getOptionValue('D');
		if (cmd.hasOption('q'))	sqlQuery    = cmd.getOptionValue('q');

		if (aseServer == null)
			aseServer = "SYBASE";

		DebugOptions.init();
		if (cmd.hasOption('x'))
		{
			String cmdLineDebug = cmd.getOptionValue('x');
			String[] sa = cmdLineDebug.split(",");
			for (int i=0; i<sa.length; i++)
			{
				String str = sa[i].trim();

				if (str.equalsIgnoreCase("list"))
				{
					System.out.println();
					System.out.println(" Option          Description");
					System.out.println(" --------------- -------------------------------------------------------------");
					for (Map.Entry<String,String> entry : Debug.getKnownDebugs().entrySet()) 
					{
						String debugOption = entry.getKey();
						String description = entry.getValue();

						System.out.println(" "+StringUtil.left(debugOption, 15, true) + " " + description);
					}
					System.out.println();
					// Get of of here if it was a list option
					throw new NormalExitException("List of debug options");
				}
				else
				{
					// add debug option
					Debug.addDebug(str);
				}
			}
		}

//		System.setProperty("Logging.print.noDefaultLoggerMessage", "false");
		Logging.init("asesqlw.", propFile);
		
    	try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		String hostPortStr = "";
		if (aseServer.indexOf(":") == -1)
			hostPortStr = AseConnectionFactory.getIHostPortStr(aseServer);
		else
			hostPortStr = aseServer;

		_logger.info("Connecting as user '"+aseUsername+"' to server='"+aseServer+"'. Which is located on '"+hostPortStr+"'.");
		Connection conn = null;
		try
		{
			Properties props = new Properties();
//			props.put("CHARSET", "iso_1");
			conn = AseConnectionFactory.getConnection(hostPortStr, aseDbname, aseUsername, asePassword, "AseSqlWindow", null, props, null);

			// Set the correct dbname, if it hasnt already been done
			AseConnectionUtils.useDbname(conn, aseDbname);
		}
		catch (SQLException e)
		{
			_logger.error("Problems connecting: " + AseConnectionUtils.sqlExceptionToString(e));
			throw e;
		}


		// Create a QueryWindow component that uses the factory object.
		QueryWindow qw = new QueryWindow(conn, sqlQuery, true, QueryWindow.WindowType.CMDLINE_JFRAME);
		qw.openTheWindow();
	}

	/**
	 * This constructor method creates a simple GUI and hooks up an event
	 * listener that updates the table when the user enters a new query.
	 **/
	public QueryWindow(Connection conn, WindowType winType)
	{
		this(conn, null, true, winType);
	}
	public QueryWindow(Connection conn, boolean closeConnOnExit, WindowType winType)
	{
		this(conn, null, closeConnOnExit, winType);
	}
	public QueryWindow(Connection conn, String sql, WindowType winType)
	{
		this(conn, sql, true, winType);
	}
	public QueryWindow(Connection conn, String sql, boolean closeConnOnExit, WindowType winType)
	{
		if (winType == WindowType.CMDLINE_JFRAME)
		{
			_jframe  = new JFrame(Version.getAppName()+" Query Window");
			_jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			_window  = _jframe;
		}
		if (winType == WindowType.JFRAME)
		{
			_jframe  = new JFrame(Version.getAppName()+" Query");
			_window  = _jframe;
		}
		if (winType == WindowType.JDIALOG)
		{
			_jdialog = new JDialog((Dialog)null, Version.getAppName()+" Query");
			_window  = _jdialog;
		}
		if (winType == WindowType.JDIALOG_MODAL)
		{
			_jdialog = new JDialog((Dialog)null, Version.getAppName()+" Query", true);
			_window  = _jdialog;
		}
		if (_window == null)
			throw new RuntimeException("_window is null, this should never happen.");

		//super();
		//super.setTitle(Version.getAppName()+" Query"); // Set window title
//		ImageIcon icon = new ImageIcon(getClass().getResource("swing/images/query16.gif"));
		ImageIcon icon = SwingUtils.readImageIcon(Version.class, "images/sql_query_window.png");
//		super.setIconImage(icon.getImage()); // works if we are a JFrame
//		((Frame)this.getOwner()).setIconImage(icon.getImage()); // works if we are a JDialog

		_window.setIconImage(icon.getImage());

		_closeConnOnExit = closeConnOnExit;

		// Arrange to quit the program when the user closes the window
		_window.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				if (_closeConnOnExit)
					close();
			}
		});

		// Remember the factory object that was passed to us
		_conn = conn;

		// Setup a message handler
		((SybConnection)_conn).setSybMessageHandler(this);
		int aseVersion = AseConnectionUtils.getAseVersionNumber(conn);

		// Set various components
		_exec.setToolTipText("Executes the select sql statement above (Ctrl-e)(Alt+e)(F5)(F9)."); 
		_exec.setMnemonic('e');
//		_exec.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));

		_showplan.setToolTipText("<html>Show Graphical showplan for the sql statement (work with ASE 15.x).</html>");
		
		_dbs_cobx.setToolTipText("<html>Change database context.</html>");
		_rsInTabs.setToolTipText("<html>Check this if you want to have multiple result sets in individual tabs.</html>");
		_copy    .setToolTipText("<html>Copy All resultsets to clipboard, tables will be into ascii format.</html>");
		_query   .setToolTipText("<html>" +
									"Put your SQL query here.<br>" +
									"'go' statements is not allowed.<br>" +
									"If you select text and press 'exec' only the highlighted text will be sent to the ASE.<br>" +
									"<br>" +
									"Note: <b>Ctrl+Space</b> Brings up code completion. This is <b>not</b> working good for the moment, but it will be enhanced.<br>" +
									"<br>" +
									"<br>" +
								"</html>");

		_query.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		_queryScroll.setLineNumbersEnabled(true);
		
		// Setup Auto-Completion for SQL
		CompletionProvider acProvider = createCompletionProvider();
		AutoCompletion ac = new AutoCompletion(acProvider);
		ac.install(_query);
		
		JPopupMenu menu =_query.getPopupMenu();
		menu.addSeparator();
		
		JCheckBoxMenuItem mi;
		mi = new JCheckBoxMenuItem("Word Wrap", _query.getLineWrap());
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_query.setLineWrap( ! _query.getLineWrap() );
			}
		});
		menu.add(mi);

		mi = new JCheckBoxMenuItem("Line Numbers", _queryScroll.getLineNumbersEnabled());
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_queryScroll.setLineNumbersEnabled( ! _queryScroll.getLineNumbersEnabled() );
			}
		});
		menu.add(mi);

		mi = new JCheckBoxMenuItem("Current Line Highlight", _queryScroll.getLineNumbersEnabled());
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_query.setHighlightCurrentLine( ! _query.getHighlightCurrentLine() );
			}
		});
		menu.add(mi);

		// FIXME: new JScrollPane(_query)
		// But this is not working as I want it
		// It disables the "auto grow" of the _query window, which is problematic
		// maybe add a JSplitPane or simular...

		// Place the components within this window
		Container contentPane = _jframe != null ? _jframe.getContentPane() : _jdialog.getContentPane();
		contentPane.setLayout(new BorderLayout());
		JPanel top    = new JPanel(new BorderLayout());
		JPanel bottom = new JPanel(new MigLayout());
		_splitPane.setTopComponent(top);
		_splitPane.setBottomComponent(bottom);
		_splitPane.setContinuousLayout(true);
//		_splitPane.setOneTouchExpandable(true);
		contentPane.add(_splitPane);

		top.add(_queryScroll, BorderLayout.CENTER);
		top.setMinimumSize(new Dimension(300, 100));

		bottom.add(_exec,           "split 4");
		bottom.add(_dbs_cobx,       "");
		bottom.add(_rsInTabs,       "");
		bottom.add(_showplan,       "");
		bottom.add(_copy,           "right, wrap");
		bottom.add(_resPanelScroll, "span 4, width 100%, height 100%");
		bottom.add(_msgline, "dock south");

		_resPanelScroll.getVerticalScrollBar().setUnitIncrement(16);

		_showplan.setEnabled( (aseVersion >= 15000) );

		// ADD Ctrl-e
		_query.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK), "execute");
		// ADD F5, F9
		_query.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), "execute");
		_query.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "execute");

		_query.getActionMap().put("execute", new AbstractAction("execute")
		{
			private static final long	serialVersionUID	= 1L;
			public void actionPerformed(ActionEvent e)
			{
				_exec.doClick();
//				actionExecute(e);
			}
		});

		// ACTION for "exec"
		_exec.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				actionExecute(e);
			}
		});

		// ACTION for "database context"
		_dbs_cobx.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				useDb( (String) _dbs_cobx.getSelectedItem() );
			}
		});
		setDbNames();

		// ACTION for "copy"
		_copy.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				actionCopy(e);
			}
		});

		// Kick of a initial SQL query, if one is specified.
		if (sql != null && !sql.equals(""))
		{
			_query.setText(sql);
			displayQueryResults(sql);
		}
		else
		{
			String helper = "Write your SQL query here";
			_query.setText(helper);
			_query.setSelectionStart(0);
			_query.setSelectionEnd(helper.length());
		}

		// Set initial size of the JFrame, and make it visable
		//this.setSize(600, 400);
		//this.setVisible(true);
	}

	/**
	 * Create a simple provider that adds some SQL completions.
	 *
	 * @return The completion provider.
	 */
	private CompletionProvider createCompletionProvider()
	{
		// A DefaultCompletionProvider is the simplest concrete implementation
		// of CompletionProvider. This provider has no understanding of
		// language semantics. It simply checks the text entered up to the
		// caret position for a match against known completions. This is all
		// that is needed in the majority of cases.
		DefaultCompletionProvider provider = new DefaultCompletionProvider();

		// Add completions for all SQL keywords. A BasicCompletion is just a straightforward word completion.
		provider.addCompletion(new BasicCompletion(provider, "SELECT * FROM "));
		provider.addCompletion(new BasicCompletion(provider, "SELECT row_count(db_id()), object_id('') "));
		provider.addCompletion(new BasicCompletion(provider, "CASE WHEN x=1 THEN 'x=1' WHEN x=2 THEN 'x=2' ELSE 'not' END"));
		provider.addCompletion(new BasicCompletion(provider, "SELECT * FROM master..monTables ORDER BY TableName"));
		provider.addCompletion(new BasicCompletion(provider, "SELECT * FROM master..monTableColumns WHERE TableName = 'monXXX' ORDER BY ColumnID"));

		// Add a couple of "shorthand" completions. These completions don't
		// require the input text to be the same thing as the replacement text.
		provider.addCompletion(new ShorthandCompletion(provider, "sp_cacheconfig", "exec sp_cacheconfig 'default data cache', '#G'",                 "Cache Size"));
		provider.addCompletion(new ShorthandCompletion(provider, "sp_cacheconfig", "exec sp_cacheconfig 'default data cache', 'cache_partitions=#'", "Cache Partitions"));
		provider.addCompletion(new ShorthandCompletion(provider, "sp_configure",   "exec sp_configure 'memory'",                                     "Memory left for reconfigure"));
		provider.addCompletion(new ShorthandCompletion(provider, "sp_configure",   "exec sp_configure 'Monitoring'",                                 "Check Monitor configuration"));
		provider.addCompletion(new ShorthandCompletion(provider, "sp_configure",   "exec sp_configure 'nondefault'",                                 "Get changed configuration parameters"));

		// monTables
		provider.addCompletion(new ShorthandCompletion(provider, 
				"monTables",  
				"select TableID, TableName, Columns, Description from monTables where TableName like 'mon%'", 
				"Get monitor tables in this system."));
		// monColumns
		provider.addCompletion(new ShorthandCompletion(provider, 
				"monColumns", 
				"select TableName, ColumnName, TypeName, Length, Description from monTableColumns where TableName like 'mon%'", 
				"Get monitor tables and columns in this system."));
		
		return provider;
	}

	/**
	 * 
	 * @param width
	 * @param height
	 */
	public void setSize(int width, int height)
	{
		_window.setSize(width, height);
	}
	/**
	 * 
	 * @param comp
	 */
	public void setLocationRelativeTo(Component comp)
	{
		_window.setLocationRelativeTo(comp);
	}
	/**
	 * 
	 * @param b
	 */
	public void setVisible(boolean b)
	{
		_window.setVisible(b);
	}

	
	private void actionExecute(ActionEvent e)
	{
		// If we had an JTabbedPane, what was the last index
		_lastTabIndex = -1;
		for (int i=0; i<_resPanel.getComponentCount(); i++)
		{
			Component comp = (Component) _resPanel.getComponent(i);
			if (comp instanceof JTabbedPane)
			{
				JTabbedPane tp = (JTabbedPane) comp;
				_lastTabIndex = tp.getSelectedIndex();
				_logger.trace("Save last tab index pos as "+_lastTabIndex+", tp="+tp);
			}
		}

		// Get the user's query and pass to displayQueryResults()
		String q = _query.getSelectedText();
		if ( q != null && !q.equals(""))
			displayQueryResults(q);
		else
			displayQueryResults(_query.getText());
	}

	private void actionCopy(ActionEvent e)
	{
		System.out.println("-------COPY---------");
		StringBuilder sb = getResultPanelAsText(_resPanel);

		if (sb != null)
		{
			StringSelection data = new StringSelection(sb.toString());
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(data, data);
		}
	}

	private StringBuilder getResultPanelAsText(JComponent panel)
	{
		StringBuilder sb = new StringBuilder();
		String terminatorStr = "\n";
//		String terminatorStr = "----------------------------------------------------------------------------\n";

		for (int i=0; i<panel.getComponentCount(); i++)
		{
			Component comp = (Component) panel.getComponent(i);
			if (comp instanceof JPanel)
			{
//				JPanel p = (JPanel) comp;
//				String title = "";
//				Border border = p.getBorder();
//				if (border instanceof TitledBorder)
//				{
//					TitledBorder tb = (TitledBorder) border;
//					title = tb.getTitle();
//				}
//				sb.append("\n");
//				sb.append("#################################################################\n");
//				sb.append("## ").append(title).append("\n");
//				sb.append("#################################################################\n");
				sb.append( getResultPanelAsText( (JPanel)comp ) );
			}
			else if (comp instanceof JTabbedPane)
			{
				JTabbedPane tp = (JTabbedPane) comp;
				for (int t=0; t<tp.getTabCount(); t++)
				{
//					sb.append("\n");
//					sb.append("#################################################################\n");
//					sb.append("## ").append(tp.getTitleAt(t)).append("\n");
//					sb.append("#################################################################\n");
					Component tabComp = tp.getComponentAt(t);
					if (tabComp instanceof JComponent)
						sb.append( getResultPanelAsText((JComponent)tabComp) );
				}
			}
			else if (comp instanceof JTable)
			{
				JTable table = (JTable)comp;
				String textTable = SwingUtils.tableToString(table.getModel());
				sb.append( textTable );
				//sb.append("\n");
				sb.append(terminatorStr);
			}
			else if (comp instanceof JEditorPane)
			{
				JEditorPane text = (JEditorPane)comp;
//				sb.append( StringUtil.stripHtml(text.getText()) );

				// text.getText(), will get the actual HTML content and we just want the text
				// so lets copy the stuff into the clipboard and get it from there :)
				// Striping the HTML is an alternative, but that lead to other problems
				text.selectAll();
				text.copy();
				text.select(0, 0);

				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				Transferable clipData = clipboard.getContents(this);
				String strFromClipboard;
				try { strFromClipboard = (String) clipData.getTransferData(DataFlavor.stringFlavor); } 
				catch (Exception ee) { strFromClipboard = ee.toString(); }

				sb.append( strFromClipboard );
				sb.append("\n");
				sb.append(terminatorStr);
			}
			else if (comp instanceof JTextArea)  // JAseMessage extends JTextArea
			{
				JTextArea text = (JTextArea)comp;
				sb.append( text.getText() );
				sb.append("\n");
				sb.append(terminatorStr);
			}
			else if (comp instanceof JTableHeader)
			{
				// discard the table header, we get that info in JTable
			}
			else
			{
				sb.append( comp.toString() );
				sb.append("\n");
				sb.append(terminatorStr);
			}
		}
		return sb;
	}
	
	public void openTheWindow() 
	{
		openTheWindow(600, 400);
	}
	public void openTheWindow(int width, int height) 
	{
//		this.setSize(width, width);
		_window.setSize(width, width);

		// Create a Runnable to set the main visible, and get Swing to invoke.
		SwingUtilities.invokeLater(new Runnable() 
		{
			public void run() 
			{
				openTheWindowAsThread();
				_logger.debug("openTheWindowAsThread() AFTER... tread is terminating...");
			}
		});
	}

	public void setSql(String sql)
	{
		_query.setText(sql);
	}
	public String getSql()
	{
		return _query.getText();
	}
	
	
	
	private void openTheWindowAsThread()
	{		
//		//		super.isTrayIconWindow
//		final Window w=this;
//		AccessController.doPrivileged(new PrivilegedAction()
//		{
//			Class windowClass;
//			Field fieldIsTrayIconWindow;
//
//		    public Object run() 
//		    {
//		        try {
//		            windowClass = Class.forName("java.awt.Window");
//		            fieldIsTrayIconWindow = windowClass.getDeclaredField("isTrayIconWindow");
//		            fieldIsTrayIconWindow.setAccessible(true);
//
//		        } catch (NoSuchFieldException e) {
//		            _logger.error("Unable to initialize WindowAccessor: ", e);
//		        } catch (ClassNotFoundException e) {
//		        	_logger.error("Unable to initialize WindowAccessor: ", e);
//		        }
//			    try { fieldIsTrayIconWindow.set(w, true); }
//			    catch (IllegalAccessException e) {_logger.error("Unable to access the Window object", e);}
//		        return null;
//		    }
//		});

//		this.setVisible(true);
		_window.setVisible(true);
	}


	/** close the db connection */
	private void close()
	{
		if (_conn != null)
		{
			try { _conn.close(); }
			catch (SQLException sqle) {/*ignore*/}
		}
		_conn = null;
	}

	/** Automatically close the connection when we're garbage collected */
	protected void finalize()
	{
		if (_closeConnOnExit)
			close();
	}
	
	/**
	 * Change database context in the ASE 
	 * @param dbname name of the database to change to
	 * @return true on success
	 */
	private boolean useDb(String dbname)
	{
		if (dbname == null || (dbname!=null && dbname.trim().equals("")))
			return false;

		try
		{
			_conn.createStatement().execute("use "+dbname);
			return true;
		}
		catch(SQLException e)
		{
			// Then display the error in a dialog box
			JOptionPane.showMessageDialog(
//					QueryWindow.this, 
					_window, 
					"Error Number: "+e.getErrorCode()+"\n" + e.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
			getCurrentDb();
			//e.printStackTrace();
			return false;
		}
	}

	/**
	 * What is current working database
	 * @return database name, null on failure
	 */
	private String getCurrentDb()
	{
		try
		{
			Statement stmnt   = _conn.createStatement();
			ResultSet rs      = stmnt.executeQuery("select db_name()");
			String cwdb = "";
			while (rs.next())
			{
				cwdb = rs.getString(1);
			}
			_dbs_cobx.setSelectedItem(cwdb);
			return cwdb;
		}
		catch(SQLException e)
		{
			JOptionPane.showMessageDialog(
//					QueryWindow.this, 
					_window, 
					"Problems getting current Working Database:\n" +
					"Error Number: "+e.getErrorCode()+"\n" + e.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}

	/**
	 * Get 'all' databases from ASE, and set the ComboBox to Current Working database
	 */
	private void setDbNames()
	{
		try
		{
			Statement stmnt   = _conn.createStatement();
			ResultSet rs      = stmnt.executeQuery("select name, db_name() from master..sysdatabases order by name");
			DefaultComboBoxModel cbm = new DefaultComboBoxModel();
			String cwdb = "";
			while (rs.next())
			{
				cbm.addElement(rs.getString(1));
				cwdb = rs.getString(2);
			}
			_dbs_cobx.setModel(cbm);

			_dbs_cobx.setSelectedItem(cwdb);
		}
		catch(SQLException e)
		{
			DefaultComboBoxModel cbm = new DefaultComboBoxModel();
			cbm.addElement("Problems getting dbnames");
			_dbs_cobx.setModel(cbm);
		}
	}

	/*---------------------------------------------------
	** BEGIN: implementing ConnectionFactory
	**---------------------------------------------------
	*/
	public Connection getConnection(String appname)
	{
		try
		{
			return AseConnectionFactory.getConnection(null, appname, null);
		}
		catch (Exception e)  // SQLException, ClassNotFoundException
		{
			_logger.error("Problems getting a new Connection", e);
			return null;
		}
	}
	/*---------------------------------------------------
	** END: implementing ConnectionFactory
	**---------------------------------------------------
	*/

	private JPopupMenu createDataTablePopupMenu(JTable table)
	{
		_logger.debug("createDataTablePopupMenu(): called.");

		JPopupMenu popup = new JPopupMenu();

		TablePopupFactory.createCopyTable(popup);

		TablePopupFactory.createMenu(popup, 
			TablePopupFactory.TABLE_PUPUP_MENU_PREFIX, 
//			Configuration.getInstance(Configuration.CONF), 
			Configuration.getCombinedConfiguration(), 
			table, this);

		TablePopupFactory.createMenu(popup, 
			"QueryWindow." + TablePopupFactory.TABLE_PUPUP_MENU_PREFIX, 
//			Configuration.getInstance(Configuration.CONF), 
			Configuration.getCombinedConfiguration(), 
			table, this);
		
		if (popup.getComponentCount() == 0)
			return null;
		else
			return popup;
	}


	public void displayQueryResults(final String sql)
	{
		SwingWorker<String, Object> doBgThread = new SwingWorker<String, Object>()
		{
			@Override
			protected String doInBackground() throws Exception
			{
				if (_showplan.isSelected())
					new AsePlanViewer(_conn, sql);
				else
					displayQueryResults(_conn, sql);
				return null;
			}

		};
//		JDialog dialog = new JDialog((Frame)null, "Waiting for server...", true);
//		JLabel label = new JLabel("Executing SQL at ASE Server", JLabel.CENTER);
//		label.setFont(new java.awt.Font(Font.DIALOG, Font.BOLD, 16));
//		dialog.add(label);
//		dialog.pack();
//		dialog.setSize( dialog.getSize().width + 100, dialog.getSize().height + 70);
//		dialog.setLocationRelativeTo(_window);

		JDialog dialog = new JDialog((Frame)null, "Waiting for server...", true);
		dialog.setLayout(new MigLayout());
		JLabel label = new JLabel("Executing SQL at ASE Server", JLabel.CENTER);
		label.setFont(new java.awt.Font(Font.DIALOG, Font.BOLD, 16));

//		JTextPane sql_txt = new JTextPane();
//		sql_txt.setText(sql);
		RSyntaxTextArea sql_txt   = new RSyntaxTextArea();
		RTextScrollPane sql_sroll = new RTextScrollPane(sql_txt);

		sql_txt.setText(sql);
		sql_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		sql_txt.setHighlightCurrentLine(false);
		//sql_txt.setLineWrap(true);
		//sql_sroll.setLineNumbersEnabled(true);

		dialog.add(label,     "push, grow, wrap");
		dialog.add(sql_sroll, "push, grow, wrap");

		dialog.pack();
		dialog.setSize( dialog.getSize().width + 100, dialog.getSize().height + 70);
		dialog.setLocationRelativeTo(_window);

		doBgThread.addPropertyChangeListener(new SwingWorkerCompletionWaiter(dialog));
		doBgThread.execute();

		//the dialog will be visible until the SwingWorker is done
		dialog.setVisible(true); 

		// We will continue here, when results has been sent by server
		//System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
	}
	private class SwingWorkerCompletionWaiter implements PropertyChangeListener
	{
		private JDialog m_dialog;
	 
		public SwingWorkerCompletionWaiter(JDialog dialog) 
		{
			m_dialog = dialog;
		}
	 
		public void propertyChange(PropertyChangeEvent event) 
		{
			if ("state".equals(event.getPropertyName()) && StateValue.DONE == event.getNewValue()) 
			{
				m_dialog.setVisible(false);
				m_dialog.dispose();
			}
		}
	}

//	public void OLD_displayQueryResults(final String sql)
//	{
//		// If the SQL take a long time, we do not want to block other
//		// user activities, so do the db access in a thread.
//		SwingWorker w = new SwingWorker()
//		{
//			public Object construct()
//			{
//				if (_showplan.isSelected())
//					new AsePlanViewer(_conn, sql);
//				else
//					displayQueryResults(_conn, sql);
//				return null;
//			}			
//		};
//		w.start();
//		
////		SwingWorker<Integer, Integer> w = new SwingWorker<Integer, Integer>()
////		{
////			@Override
////			protected Integer doInBackground() throws Exception
////			{
////				displayQueryResults(_conn, _tmpSql);
////				return 1;
////			}
////		};
////		w.execute();
////		SwingUtilities.invokeLater(new Runnable()
////		{
////			public void run()
////			{
////				displayQueryResults(_conn, _tmpSql);
////			}
////		});
//	}

//	/**
//	 * This method uses the supplied SQL query string, and the
//	 * ResultSetTableModelFactory object to create a TableModel that holds
//	 * the results of the database query.  It passes that TableModel to the
//	 * JTable component for display.
//	 **/
//	private void displayQueryResults(Connection conn, String sql)
//	{
//		// It may take a while to get the results, so give the user some
//		// immediate feedback that their query was accepted.
//		_msgline.setText("Sending SQL to ASE...");
//
//		try
//		{
//			// If we've called close(), then we can't call this method
//			if (conn == null)
//				throw new IllegalStateException("Connection already closed.");
//
//			SQLWarning sqlw  = null;
//			Statement  stmnt = conn.createStatement();			
//			ResultSet  rs    = null;
//			int rowsAffected = 0;
//
//			// a linked list where to "store" result sets or messages
//			// before "displaying" them
//			_resultCompList = new ArrayList<JComponent>();
//
//			_logger.debug("Executing SQL statement: "+sql);
//			// Execute
//			boolean hasRs = stmnt.execute(sql);
//
//			_msgline.setText("Waiting for ASE to deliver first resultset.");
//
//			// iterate through each result set
//			int rsCount = 0;
//			do
//			{
//				if(hasRs)
//				{
//					rsCount++;
//					_msgline.setText("Reading resultset "+rsCount+".");
//
//					// Get next resultset to work with
//					rs = stmnt.getResultSet();
//
//					// Convert the ResultSet into a TableModel, which fits on a JTable
//					ResultSetTableModel tm = new ResultSetTableModel(rs, true);
//
//					// Create the JTable, using the just created TableModel/ResultSet
//					JXTable tab = new JXTable(tm);
//					tab.setSortable(true);
//					tab.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
//					tab.packAll(); // set size so that all content in all cells are visible
//					tab.setColumnControlVisible(true);
//					tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//					tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
////					SwingUtils.calcColumnWidths(tab);
//
//					// Add a popup menu
//					tab.setComponentPopupMenu( createDataTablePopupMenu(tab) );
//
////					for(int i=0; i<tm.getColumnCount(); i++)
////					{
////						Object o = tm.getValueAt(0, i);
////						if (o!=null)
////							System.out.println("Col="+i+", Class="+o.getClass()+", Comparable="+((o instanceof Comparable)?"true":"false"));
////						else
////							System.out.println("Col="+i+", ---NULL--");
////					}
//					// Add the JTable to a list for later use
//					_resultCompList.add(tab);
//
//					// Check for warnings
//					// If warnings found, add them to the LIST
//					for (sqlw = rs.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
//					{
//						_logger.trace("--In loop, sqlw: "+sqlw);
//						//compList.add(new JAseMessage(sqlw.getMessage()));
//					}
//
//					// Close it
//					rs.close();
//				}
//
//				// Treat update/row count(s)
//				rowsAffected = stmnt.getUpdateCount();
//				if (rowsAffected >= 0)
//				{
////					rso.add(rowsAffected);
//				}
//
//				// Check if we have more resultsets
//				hasRs = stmnt.getMoreResults();
//
//				_logger.trace( "--hasRs="+hasRs+", rowsAffected="+rowsAffected );
//			}
//			while (hasRs || rowsAffected != -1);
//
//			// Check for warnings
//			for (sqlw = stmnt.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
//			{
//				_logger.trace("====After read RS loop, sqlw: "+sqlw);
//				//compList.add(new JAseMessage(sqlw.getMessage()));
//			}
//
//			// Close the statement
//			stmnt.close();
//
//
//
//			//-----------------------------
//			// Add data... to panel(s) in various ways
//			// - one result set, just add it
//			// - many result sets
//			//        - Add to JTabbedPane
//			//        - OR: append the result sets as named panels
//			//-----------------------------
//			_resPanel.removeAll();
//
//			int numOfTables = countTables(_resultCompList);
//			if (numOfTables == 1)
//			{
//				int msgCount = 0;
//				int rowCount = 0;
//				_logger.trace("Only 1 RS");
//
//				// Add ResultSet  
//				_resPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0 0, wrap 1"));
//				for (JComponent jcomp: _resultCompList)
//				{
//					if (jcomp instanceof JTable)
//					{
//						JTable tab = (JTable) jcomp;
//
//						// JScrollPane is on _resPanel
//						// So we need to display the table header ourself
//						JPanel p = new JPanel(new MigLayout("insets 0 0 0 0, gap 0 0, wrap 1"));
//						p.add(tab.getTableHeader(), "wrap");
//						p.add(tab,                  "wrap");
//
//						_resPanel.add(p, "");
//
//						rowCount = tab.getRowCount();
//					}
//					else if (jcomp instanceof JAseMessage)
//					{
//						JAseMessage msg = (JAseMessage) jcomp;
//						_logger.trace("1-RS: JAseMessage: "+msg.getText());
//						_resPanel.add(msg, "growx, pushx");
//
//						msgCount++;
//					}
//				}
//				_msgline.setText(" "+rowCount+" rows, and "+msgCount+" messages.");
//			}
//			else if (numOfTables > 1)
//			{
//				int msgCount = 0;
//				int rowCount = 0;
//				_logger.trace("Several RS: "+_resultCompList.size());
//				
//				if (_rsInTabs.isSelected())
//				{
//					// Add Result sets to individual tabs, on a JTabbedPane 
//					JTabbedPane tabPane = new JTabbedPane();
//					_resPanel.add(tabPane, "");
//
//					int i = 1;
//					for (JComponent jcomp: _resultCompList)
//					{
//						if (jcomp instanceof JTable)
//						{
//							JTable tab = (JTable) jcomp;
//
//							// JScrollPane is on _resPanel
//							// So we need to display the table header ourself
//							JPanel p = new JPanel(new MigLayout("insets 0 0 0 0, gap 0 0"));
//							p.add(tab.getTableHeader(), "wrap");
//							p.add(tab,                  "wrap");
//
//							tabPane.addTab("Result "+(i++), p);
//
//							rowCount += tab.getRowCount();
//						}
//						else if (jcomp instanceof JAseMessage)
//						{
//							JAseMessage msg = (JAseMessage) jcomp;
//							_resPanel.add(msg, "growx, pushx");
//							_logger.trace("JTabbedPane: JAseMessage: "+msg.getText());
//
//							msgCount++;
//						}
//					}
//					if (_lastTabIndex > 0)
//					{
//						if (_lastTabIndex < tabPane.getTabCount())
//						{
//							tabPane.setSelectedIndex(_lastTabIndex);
//							_logger.trace("Restore last tab index pos to "+_lastTabIndex);
//						}
//					}
//					_msgline.setText(" "+numOfTables+" ResultSet with totally "+rowCount+" rows, and "+msgCount+" messages.");
//				}
//				else
//				{
//					// Add Result sets to individual panels, which are 
//					// appended to the result panel
//					_resPanel.setLayout(new MigLayout("insets 0 0 0 0, wrap 1"));
//					int i = 1;
//					for (JComponent jcomp: _resultCompList)
//					{
//						if (jcomp instanceof JTable)
//						{
//							JTable tab = (JTable) jcomp;
//
//							// JScrollPane is on _resPanel
//							// So we need to display the table header ourself
//							JPanel p = new JPanel(new MigLayout("insets 0 0, gap 0 0"));
//							Border border = BorderFactory.createTitledBorder("ResultSet "+(i++));
//							p.setBorder(border);
//							p.add(tab.getTableHeader(), "wrap");
//							p.add(tab,                  "wrap");
//							_resPanel.add(p, "");
//
//							rowCount += tab.getRowCount();
//						}
//						else if (jcomp instanceof JAseMessage)
//						{
//							JAseMessage msg = (JAseMessage) jcomp;
//							_logger.trace("JPane: JAseMessage: "+msg.getText());
//							_resPanel.add(msg, "growx, pushx");
//
//							msgCount++;
//						}
//					}
//					_msgline.setText(" "+numOfTables+" ResultSet with totally "+rowCount+" rows, and "+msgCount+" messages.");
//				}
//			}
//			else
//			{
//				_logger.trace("NO RS: "+_resultCompList.size());
//				_resPanel.setLayout(new MigLayout("insets 0 0 0 0, wrap 1"));
//				int msgCount = 0;
//
//				StringBuilder sb = new StringBuilder();
//				sb.append("<html>");
//				sb.append("<head>");
//				sb.append("<style type=\"text/css\">");
//				sb.append("<!-- body {font-family: Courier New; margin: 0px} -->");
//				sb.append("<!-- pre  {font-family: Courier New; margin: 0px} -->");
//				sb.append("</style>");
//				sb.append("</head>");
//				sb.append("<body>");
//				sb.append("<pre>");
//				// There might be "just" print statements... 
//				for (JComponent jcomp: _resultCompList)
//				{
//					if (jcomp instanceof JAseMessage)
//					{
//						JAseMessage msg = (JAseMessage) jcomp;
////						msg.setFont( _aseMsgFont );
////						_logger.trace("NO-RS: JAseMessage: "+msg.getText());
////						_resPanel.add(msg, "");
////						sb.append("<P>").append(msg.getText()).append("</P>\n");
////						sb.append(msg.getText()).append("<BR>\n");
//						sb.append(msg.getText()).append("\n");
//
//						msgCount++;
//					}
//				}
//				sb.append("</pre>");
//				sb.append("</body>");
//				sb.append("</html>");
//
////				JTextPane text = new JTextPane();
//				JEditorPane textPane = new JEditorPane("text/html", sb.toString());
//				textPane.setEditable(false);
//				textPane.setOpaque(false);
////				if (_aseMsgFont == null)
////					_aseMsgFont = new Font("Courier", Font.PLAIN, 12);
////				textPane.setFont(_aseMsgFont);
//				
//				_resPanel.add(textPane, "");
//
//				_msgline.setText("NO ResultSet, but "+msgCount+" messages.");
//			}
//			
//			// We're done, so clear the feedback message
//			//_msgline.setText(" ");
//		}
//		catch (SQLException ex)
//		{
//			// If something goes wrong, clear the message line
//			_msgline.setText("Error: "+ex.getMessage());
//			ex.printStackTrace();
//
//			// Then display the error in a dialog box
//			JOptionPane.showMessageDialog(
////					QueryWindow.this, 
//					_window, 
//					new String[] { // Display a 2-line message
//							ex.getClass().getName() + ": ", 
//							ex.getMessage() },
//					"Error", JOptionPane.ERROR_MESSAGE);
//		}
//		
//		// In some cases, some of the area in not repainted
//		// example: when no RS, but only messages has been displayed
//		_resPanel.repaint();
//	}
	
	private void putSqlWarningMsgs(ResultSet rs, ArrayList<JComponent> resultCompList, String debugStr)
	{
		if (rs == null)
			return;
		try
		{
			putSqlWarningMsgs(rs.getWarnings(), resultCompList, debugStr);
			rs.clearWarnings();
		}
		catch (SQLException e)
		{
		}
	}
	private void putSqlWarningMsgs(Statement stmnt, ArrayList<JComponent> resultCompList, String debugStr)
	{
		if (stmnt == null)
			return;
		try
		{
			putSqlWarningMsgs(stmnt.getWarnings(), resultCompList, debugStr);
			stmnt.clearWarnings();
		}
		catch (SQLException e)
		{
		}
	}

	private void putSqlWarningMsgs(SQLException sqe, ArrayList<JComponent> resultCompList, String debugStr)
	{
		while (sqe != null)
		{
			StringBuilder sb = new StringBuilder();
			if(sqe instanceof EedInfo)
			{
				// Error is using the addtional TDS error data.
				EedInfo eedi = (EedInfo) sqe;
				if(eedi.getSeverity() > 10)
				{
					boolean firstOnLine = true;
					sb.append("Msg " + sqe.getErrorCode() +
							", Level " + eedi.getSeverity() + ", State " +
							eedi.getState() + ":\n");

					if( eedi.getServerName() != null)
					{
						sb.append("Server '" + eedi.getServerName() + "'");
						firstOnLine = false;
					}
					if(eedi.getProcedureName() != null)
					{
						sb.append( (firstOnLine ? "" : ", ") +
								"Procedure '" + eedi.getProcedureName() + "'");
						firstOnLine = false;
					}
					sb.append( (firstOnLine ? "" : ", ") +
							"Line " + eedi.getLineNumber() +
							", Status " + eedi.getStatus() + 
							", TranState " + eedi.getTranState() + ":\n");
				}
				// Now print the error or warning
				sb.append(sqe.getMessage()+"\n");
			}
			else
			{
				// SqlState: 010P4 java.sql.SQLWarning: 010P4: An output parameter was received and ignored.
				if ( ! sqe.getSQLState().equals("010P4") )
				{
					sb.append("Unexpected exception : " +
							"SqlState: " + sqe.getSQLState()  +
							" " + sqe.toString() +
							", ErrorCode: " + sqe.getErrorCode() + "\n");
				}
			}
			
			// Add the info to the list
			if (sb.length() > 0)
			{
				// If new-line At the end, remove it
				if ( sb.charAt(sb.length()-1) == '\n' )
					sb.deleteCharAt(sb.length()-1);

				String aseMsg = sb.toString();
				resultCompList.add( new JAseMessage(aseMsg) );

				if (_logger.isTraceEnabled())
					_logger.trace("ASE Msg("+debugStr+"): "+aseMsg);
			}

			sqe = sqe.getNextException();
		}
	}

	/**
	 * This method uses the supplied SQL query string, and the
	 * ResultSetTableModelFactory object to create a TableModel that holds
	 * the results of the database query.  It passes that TableModel to the
	 * JTable component for display.
	 **/
	private void displayQueryResults(Connection conn, String sql)
	{
		// It may take a while to get the results, so give the user some
		// immediate feedback that their query was accepted.
		_msgline.setText("Sending SQL to ASE...");

		try
		{
			// If we've called close(), then we can't call this method
			if (conn == null)
				throw new IllegalStateException("Connection already closed.");

			Statement  stmnt = conn.createStatement();			
			ResultSet  rs    = null;
			int rowsAffected = 0;

			// a linked list where to "store" result sets or messages
			// before "displaying" them
			_resultCompList = new ArrayList<JComponent>();

			_logger.debug("Executing SQL statement: "+sql);
			// Execute
			boolean hasRs = stmnt.execute(sql);

			_msgline.setText("Waiting for ASE to deliver first resultset.");

			// iterate through each result set
			int rsCount = 0;
			do
			{
				// Append, messages and Warnings to _resultCompList, if any
				putSqlWarningMsgs(stmnt, _resultCompList, "-before-hasRs-");

				if(hasRs)
				{
					rsCount++;
					_msgline.setText("Reading resultset "+rsCount+".");

					// Get next resultset to work with
					rs = stmnt.getResultSet();

					// Append, messages and Warnings to _resultCompList, if any
					putSqlWarningMsgs(stmnt, _resultCompList, "-after-getResultSet()-Statement-");
					putSqlWarningMsgs(rs,    _resultCompList, "-after-getResultSet()-ResultSet-");

					// Convert the ResultSet into a TableModel, which fits on a JTable
					ResultSetTableModel tm = new ResultSetTableModel(rs, true);
					for (SQLWarning sqlw : tm.getSQLWarningList())
						putSqlWarningMsgs(sqlw, _resultCompList, "-after-ResultSetTableModel()-tm.getSQLWarningList()-");

					// Create the JTable, using the just created TableModel/ResultSet
					JXTable tab = new JXTable(tm);
					tab.setSortable(true);
					tab.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
					tab.packAll(); // set size so that all content in all cells are visible
					tab.setColumnControlVisible(true);
					tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
					tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//					SwingUtils.calcColumnWidths(tab);

					// Add a popup menu
					tab.setComponentPopupMenu( createDataTablePopupMenu(tab) );

//					for(int i=0; i<tm.getColumnCount(); i++)
//					{
//						Object o = tm.getValueAt(0, i);
//						if (o!=null)
//							System.out.println("Col="+i+", Class="+o.getClass()+", Comparable="+((o instanceof Comparable)?"true":"false"));
//						else
//							System.out.println("Col="+i+", ---NULL--");
//					}
					// Add the JTable to a list for later use
					_resultCompList.add(tab);

					// Append, messages and Warnings to _resultCompList, if any
					putSqlWarningMsgs(stmnt, _resultCompList, "-before-rs.close()-");

					// Close it
					rs.close();
				}

				// Treat update/row count(s)
				rowsAffected = stmnt.getUpdateCount();
				if (rowsAffected >= 0)
				{
//					rso.add(rowsAffected);
				}

				// Check if we have more resultsets
				// If any SQLWarnings has not been found above, it will throw one here
				// so catch raiserrors or other stuff that is not SQLWarnings.
				hasRs = stmnt.getMoreResults();

				_logger.trace( "--hasRs="+hasRs+", rowsAffected="+rowsAffected );
			}
			while (hasRs || rowsAffected != -1);

			// Append, messages and Warnings to _resultCompList, if any
			putSqlWarningMsgs(stmnt, _resultCompList, "-before-stmnt.close()-");

			// Close the statement
			stmnt.close();



			//-----------------------------
			// Add data... to panel(s) in various ways
			// - one result set, just add it
			// - many result sets
			//        - Add to JTabbedPane
			//        - OR: append the result sets as named panels
			//-----------------------------
			_resPanel.removeAll();

			int numOfTables = countTables(_resultCompList);
			if (numOfTables == 1)
			{
				int msgCount = 0;
				int rowCount = 0;
				_logger.trace("Only 1 RS");

				// Add ResultSet  
				_resPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0 0, wrap 1"));
				for (JComponent jcomp: _resultCompList)
				{
					if (jcomp instanceof JTable)
					{
						JTable tab = (JTable) jcomp;

						// JScrollPane is on _resPanel
						// So we need to display the table header ourself
						JPanel p = new JPanel(new MigLayout("insets 0 0 0 0, gap 0 0, wrap 1"));
						p.add(tab.getTableHeader(), "wrap");
						p.add(tab,                  "wrap");

						_logger.trace("1-RS: add: JTable");
						_resPanel.add(p, "");

						rowCount = tab.getRowCount();
					}
					else if (jcomp instanceof JAseMessage)
					{
						JAseMessage msg = (JAseMessage) jcomp;
						_logger.trace("1-RS: JAseMessage: "+msg.getText());
						_resPanel.add(msg, "growx, pushx");

						msgCount++;
					}
				}
				_msgline.setText(" "+rowCount+" rows, and "+msgCount+" messages.");
			}
			else if (numOfTables > 1)
			{
				int msgCount = 0;
				int rowCount = 0;
				_logger.trace("Several RS: "+_resultCompList.size());
				
				if (_rsInTabs.isSelected())
				{
					// Add Result sets to individual tabs, on a JTabbedPane 
					JTabbedPane tabPane = new JTabbedPane();
					_logger.trace("JTabbedPane: add: JTabbedPane");
					_resPanel.add(tabPane, "");

					int i = 1;
					for (JComponent jcomp: _resultCompList)
					{
						if (jcomp instanceof JTable)
						{
							JTable tab = (JTable) jcomp;

							// JScrollPane is on _resPanel
							// So we need to display the table header ourself
							JPanel p = new JPanel(new MigLayout("insets 0 0 0 0, gap 0 0"));
							p.add(tab.getTableHeader(), "wrap");
							p.add(tab,                  "wrap");

							_logger.trace("JTabbedPane: add: JTable("+i+")");
							tabPane.addTab("Result "+(i++), p);

							rowCount += tab.getRowCount();
						}
						else if (jcomp instanceof JAseMessage)
						{
							JAseMessage msg = (JAseMessage) jcomp;
							_resPanel.add(msg, "growx, pushx");
							_logger.trace("JTabbedPane: JAseMessage: "+msg.getText());

							msgCount++;
						}
					}
					if (_lastTabIndex > 0)
					{
						if (_lastTabIndex < tabPane.getTabCount())
						{
							tabPane.setSelectedIndex(_lastTabIndex);
							_logger.trace("Restore last tab index pos to "+_lastTabIndex);
						}
					}
					_msgline.setText(" "+numOfTables+" ResultSet with totally "+rowCount+" rows, and "+msgCount+" messages.");
				}
				else
				{
					// Add Result sets to individual panels, which are 
					// appended to the result panel
					_resPanel.setLayout(new MigLayout("gapy 1, insets 0 0 0 0, wrap 1"));
					int i = 1;
					for (JComponent jcomp: _resultCompList)
					{
						if (jcomp instanceof JTable)
						{
							JTable tab = (JTable) jcomp;

							// JScrollPane is on _resPanel
							// So we need to display the table header ourself
							JPanel p = new JPanel(new MigLayout("insets 0 0, gap 0 0"));
							Border border = BorderFactory.createTitledBorder("ResultSet "+(i++));
							p.setBorder(border);
							p.add(tab.getTableHeader(), "wrap");
							p.add(tab,                  "wrap");
							_logger.trace("JPane: add: JTable("+i+")");
							_resPanel.add(p, "");

							rowCount += tab.getRowCount();
						}
						else if (jcomp instanceof JAseMessage)
						{
							JAseMessage msg = (JAseMessage) jcomp;
							_logger.trace("JPane: JAseMessage: "+msg.getText());
							_resPanel.add(msg, "growx, pushx");

							msgCount++;
						}
					}
					_msgline.setText(" "+numOfTables+" ResultSet with totally "+rowCount+" rows, and "+msgCount+" messages.");
				}
			}
			else
			{
				_logger.trace("NO RS: "+_resultCompList.size());
				_resPanel.setLayout(new MigLayout("insets 0 0 0 0, wrap 1"));
				int msgCount = 0;

				StringBuilder sb = new StringBuilder();
				sb.append("<html>");
				sb.append("<head>");
				sb.append("<style type=\"text/css\">");
				sb.append("<!-- body {font-family: Courier New; margin: 0px} -->");
				sb.append("<!-- pre  {font-family: Courier New; margin: 0px} -->");
				sb.append("</style>");
				sb.append("</head>");
				sb.append("<body>");
				sb.append("<pre>");
				// There might be "just" print statements... 
				for (JComponent jcomp: _resultCompList)
				{
					if (jcomp instanceof JAseMessage)
					{
						JAseMessage msg = (JAseMessage) jcomp;
//						msg.setFont( _aseMsgFont );
//						_logger.trace("NO-RS: JAseMessage: "+msg.getText());
//						_resPanel.add(msg, "");
//						sb.append("<P>").append(msg.getText()).append("</P>\n");
//						sb.append(msg.getText()).append("<BR>\n");
						sb.append(msg.getText()).append("\n");

						msgCount++;
					}
				}
				sb.append("</pre>");
				sb.append("</body>");
				sb.append("</html>");

//				JTextPane text = new JTextPane();
				JEditorPane textPane = new JEditorPane("text/html", sb.toString());
				textPane.setEditable(false);
				textPane.setOpaque(false);
//				if (_aseMsgFont == null)
//					_aseMsgFont = new Font("Courier", Font.PLAIN, 12);
//				textPane.setFont(_aseMsgFont);
				
				_resPanel.add(textPane, "");

				_msgline.setText("NO ResultSet, but "+msgCount+" messages.");
			}
			
			// We're done, so clear the feedback message
			//_msgline.setText(" ");
		}
		catch (SQLException ex)
		{
			// If something goes wrong, clear the message line
			_msgline.setText("Error: "+ex.getMessage());
			ex.printStackTrace();

			// Then display the error in a dialog box
			JOptionPane.showMessageDialog(
//					QueryWindow.this, 
					_window, 
					new String[] { // Display a 2-line message
							ex.getClass().getName() + ": ", 
							ex.getMessage() },
					"Error", JOptionPane.ERROR_MESSAGE);
		}
		
		// In some cases, some of the area in not repainted
		// example: when no RS, but only messages has been displayed
		_resPanel.repaint();
	}

	private int countTables(ArrayList<JComponent> list)
	{
		int count = 0;
		for (JComponent jcomp: list)
		{
			if (jcomp instanceof JTable)
			{
				count++;
			}
		}
		return count;
	}

//	public SQLException messageHandler(SQLException sqe)
//	{
//		// Take care of some specific messages...
//		int code = sqe.getErrorCode();
//		StringBuilder sb = new StringBuilder();
//
//		if (sqe instanceof EedInfo)
//		{
//			EedInfo m = (EedInfo) sqe;
////			m.getServerName();
////			m.getSeverity();
////			m.getState();
////			m.getLineNumber();
////			m.getStatus();
////			sqe.getMessage();
////			sqe.getErrorCode();
//			_logger.debug(
//					"Server='"+m.getServerName()+"', " +
//					"MsgNum='"+sqe.getErrorCode()+"', " +
//					"Severity='"+m.getSeverity()+"', " +
//					"State='"+m.getState()+"', " +
//					"Status='"+m.getStatus()+"', " +
//					"Proc='"+m.getProcedureName()+"', " +
//					"Line='"+m.getLineNumber()+"', " +
//					"Msg: "+sqe.getMessage());
//			
//			if (m.getSeverity() <= 10)
//			{
//				sb.append(sqe.getMessage());
//				
//				// Discard empty messages
//				String str = sb.toString();
//				if (str == null || (str != null && str.trim().equals("")) )
//					return null;
//			}
//			else
//			{
//				// Msg 222222, Level 16, State 1:
//				// Server 'GORANS_1_DS', Line 1:
//				//	mmmm
//
//				sb.append("Msg ").append(sqe.getErrorCode())
//					.append(", Level ").append(m.getSeverity())
//					.append(", State ").append(m.getState())
//					.append(":\n");
//
//				boolean addComma = false;
//				String str = m.getServerName();
//				if ( str != null && !str.equals(""))
//				{
//					addComma = true;
//					sb.append("Server '").append(str).append("'");
//				}
//
//				str = m.getProcedureName();
//				if ( str != null && !str.equals(""))
//				{
//					if (addComma) sb.append(", ");
//					addComma = true;
//					sb.append("Procedure '").append(str).append("'");
//				}
//
//				str = m.getLineNumber() + "";
//				if ( str != null && !str.equals(""))
//				{
//					if (addComma) sb.append(", ");
//					addComma = true;
//					sb.append("Line ").append(str).append(":");
//					addComma = false;
//					sb.append("\n");
//				}
//				sb.append(sqe.getMessage());
//			}
//
//			// If new-line At the end, remove it
//			if ( sb.charAt(sb.length()-1) == '\n' )
//			{
//				sb.deleteCharAt(sb.length()-1);
//			}
//		}
//		
//		//if (code == 987612) // Just a dummy example
//		//{
//		//	_logger.info(getPreStr()+"Downgrading " + code + " to a warning");
//		//	sqe = new SQLWarning(sqe.getMessage(), sqe.getSQLState(), sqe.getErrorCode());
//		//}
//
//		//-------------------------------
//		// TREAT DIFFERENT MESSAGES
//		//-------------------------------
//
//		// 3604 Duplicate key was ignored.
//		if (code == 3604)
//		{
////			_logger.debug(getPreStr()+"Ignoring ASE message " + code + ": Duplicate key was ignored.");
////			super.messageAdd("INFO: Ignoring ASE message " + code + ": Duplicate key was ignored.");
////			return null;
//		}
//
//
//		// Not Yet Recovered
//		// 921: Database 'xxx' has not been recovered yet - please wait and try again.
//		// 950: Database 'xxx' is currently offline. Please wait and try your command again later.
//		if (code == 921 || code == 950)
//		{
//		}
//
//		// DEADLOCK
//		if (code == 1205)
//		{
//		}
//
//		// LOCK-TIMEOUT
//		if (code == 12205)
//		{
//		}
//
//
//		//
//		// Write some extra info in some cases
//		//
//		// error   severity description
//		// ------- -------- -----------
//		//    208        16 %.*s not found. Specify owner.objectname or use sp_help to check whether the object exists (sp_help may produce lots of output).
//		//    504        11 Stored procedure '%.*s' not found.
//		//   2501        16 Table named %.*s not found; check sysobjects
//		//   2812        16 Stored procedure '%.*s' not found. Specify owner.objectname or use sp_help to check whether the object exists (sp_help may produce lots of output).
//		//   9938        16 Table with ID %d not found; check sysobjects.
//		//  10303        14 Object named '%.*s' not found; check sysobjects.
//		//  10337        16 Object '%S_OBJID' not found.
//		//  11901        16 Table '%.*s' was not found.
//		//  11910        16 Index '%.*s' was not found.
//		//  18826         0 Procedure '%1!' not found.
//
//		if (    code == 208 
//		     || code == 504 
//		     || code == 2501 
//		     || code == 2812 
//		     || code == 9938 
//		     || code == 10303 
//		     || code == 10337 
//		     || code == 11901 
//		     || code == 11910 
//		     || code == 18826 
//		   )
//		{
////			_logger.info("MessageHandler for SPID "+getSpid()+": Current database was '"+this.getDbname()+"' while receiving the above error/warning.");
////			super.messageAdd("INFO: Current database was '"+this.getDbname()+"' while receiving the above error/warning.");
//		}
//
//		if (_resultCompList != null)
//			_resultCompList.add(new JAseMessage( sb.toString() ));
//
//		// Pass the Exception on.
//		return null;
////		return sqe;
//	}

	public SQLException messageHandler(SQLException sqe)
	{
		// Pass Warning on...
		if (sqe instanceof SQLWarning)
			return sqe;

		// Discard SQLExceptions... but first send them to the _resultCompList
		// This is a bit ugly...
		putSqlWarningMsgs(sqe, _resultCompList, "-from-messageHandler()-");
		return null;
	}

	private class JAseMessage 
	extends JTextArea
	{

		private static final long serialVersionUID = 1L;

//		public JAseMessage()
//		{
//			_init();
//		}

		public JAseMessage(final String s)
		{
			super(s);
			_init();
		}

		private void _init()
		{
			super.setEditable(false);

			if (_aseMsgFont == null)
				_aseMsgFont = new Font("Courier", Font.PLAIN, 12);
			setFont(_aseMsgFont);

			setLineWrap(true);
			setWrapStyleWord(true);
//			setOpaque(false); // Transparent
		}

//		public boolean isFocusable()
//		{
//			return false;
//		}
//
//		public boolean isRequestFocusEnabled()
//		{
//			return false;
//		}
	}

	
	/**
	 * This simple main method tests the class.  It expects four command-line
	 * arguments: the driver classname, the database URL, the username, and
	 * the password
	 **/
	public static void test_main(String args[]) throws Exception
	{
		// FIXME: parse input parameters

		Properties log4jProps = new Properties();
		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		// Set configuration, right click menus are in there...
		Configuration conf = new Configuration("c:\\projects\\asetune\\asetune.properties");
		Configuration.setInstance(Configuration.SYSTEM_CONF, conf);

		// Create the factory object that holds the database connection using
		// the data specified on the command line
    	try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    		//UIManager.setLookAndFeel(new SubstanceOfficeSilver2007LookAndFeel());
		} catch (Exception e) {
			e.printStackTrace();
		}

		String server = "GORAN_1_DS";
//		String host = AseConnectionFactory.getIHost(server);
//		int    port = AseConnectionFactory.getIPort(server);
		String hostPortStr = AseConnectionFactory.getIHostPortStr(server);
		System.out.println("Connectiong to server='"+server+"'. Which is located on '"+hostPortStr+"'.");
		Connection conn = null;
		try
		{
			Properties props = new Properties();
			props.put("CHARSET", "iso_1");
			conn = AseConnectionFactory.getConnection(hostPortStr, null, "sa", "", Version.getAppName()+"-QueryWindow", null, props, null);
		}
		catch (SQLException e)
		{
			System.out.println("Problems connecting: " + AseConnectionUtils.sqlExceptionToString(e));
//			AseConnectionUtils.sqlWarningToString(e);
			throw e;
		}


		// Create a QueryWindow component that uses the factory object.
		QueryWindow qw = new QueryWindow(conn, 
				"print 'a very long string that starts here.......................and continues,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,with some more characters------------------------and some more++++++++++++++++++++++++++++++++++++ yes even more 00000 0 0 0 0 0 000000000 0 00000000 00000, lets do some more.......................... end it ends here. -END-'\n" +
				"print '11111111'\n" +
				"select getdate()\n" +
				"exec sp_whoisw2\n" +
				"select \"ServerName\" = @@servername\n" +
				"raiserror 123456 'A dummy message by raiserror'\n" +
				"exec sp_help sysobjects\n" +
				"select \"Current Date\" = getdate()\n" +
				"print '222222222'\n" +
				"select * from master..sysdatabases\n" +
				"print '|3-33333333'\n" +
				"print '|4-33333333'\n" +
				"print '|5-33333333'\n" +
				"print '|6-33333333'\n" +
				"print '|7-33333333'\n" +
				"print '|8-33333333'\n" +
				"print '|9-33333333'\n" +
				"print '|10-33333333'\n" +
				"                             exec sp_opentran \n" +
				"print '|11-33333333'\n" +
				"print '|12-33333333'\n" +
				"print '|13-33333333'\n" +
				"print '|14-33333333'\n" +
				"print '|15-33333333'\n" +
				"print '|16-33333333'\n" +
				"print '|17-33333333'\n" +
				"select * from sysobjects \n" +
				"select * from sysprocesses ",
				true, QueryWindow.WindowType.JFRAME);
		qw.openTheWindow();
	}	
	/**
	 * Print command line options.
	 * @param options
	 */
	private static void printHelp(Options options, String errorStr)
	{
		PrintWriter pw = new PrintWriter(System.out);

		if (errorStr != null)
		{
			pw.println();
			pw.println(errorStr);
			pw.println();
		}

		pw.println("usage: asesqlw [-U <user>] [-P <passwd>] [-S <server>] [-D <dbname>]");
		pw.println("               [-q <sqlStatement>] [-h] [-v] [-x] <debugOptions> ");
		pw.println("  ");
		pw.println("options:");
		pw.println("  -h,--help                 Usage information.");
		pw.println("  -v,--version              Display "+Version.getAppName()+" and JVM Version.");
		pw.println("  -x,--debug <dbg1,dbg2>    Debug options: a comma separated string");
		pw.println("                            To get available option, do -x list");
		pw.println("  ");
		pw.println("  -U,--user <user>          Username when connecting to server.");
		pw.println("  -P,--passwd <passwd>      Password when connecting to server. null=noPasswd");
		pw.println("  -S,--server <server>      Server to connect to.");
		pw.println("  -D,--dbname <dbname>      Database to use when connecting");
		pw.println("  -q,--query <sqlStatement> SQL Statement to execute");
		pw.println("");
		pw.flush();
	}

	/**
	 * Build the options parser. Has to be synchronized because of the way
	 * Options are constructed.
	 * 
	 * @return an options parser.
	 */
	private static synchronized Options buildCommandLineOptions()
	{
		Options options = new Options();

		// create the Options
		options.addOption( "h", "help",        false, "Usage information." );
		options.addOption( "v", "version",     false, "Display "+Version.getAppName()+" and JVM Version." );
		options.addOption( "x", "debug",       true,  "Debug options: a comma separated string dbg1,dbg2,dbg3" );

		options.addOption( "U", "user",        true, "Username when connecting to server." );
		options.addOption( "P", "passwd",      true, "Password when connecting to server. (null=noPasswd)" );
		options.addOption( "S", "server",      true, "Server to connect to." );
		options.addOption( "D", "dbname",      true, "Database use when connecting" );
		options.addOption( "q", "sqlStatement",true, "SQL statement to execute" );

		return options;
	}


	//---------------------------------------------------
	// Command Line Parsing
	//---------------------------------------------------
	private static CommandLine parseCommandLine(String[] args, Options options)
	throws ParseException
	{
		// create the command line parser
		CommandLineParser parser = new PosixParser();	
	
		// parse the command line arguments
		CommandLine cmd = parser.parse( options, args );

		// Validate any mandatory options or dependencies of switches
		

		if (_logger.isDebugEnabled())
		{
			for (@SuppressWarnings("unchecked") Iterator<Option> it=cmd.iterator(); it.hasNext();)
			{
				Option opt = it.next();
				_logger.debug("parseCommandLine: swith='"+opt.getOpt()+"', value='"+opt.getValue()+"'.");
			}
		}

		return cmd;
	}

	//---------------------------------------------------
	// MAIN
	//---------------------------------------------------
	public static void main(String[] args)
	{
		Options options = buildCommandLineOptions();
		try
		{
			CommandLine cmd = parseCommandLine(args, options);

			//-------------------------------
			// HELP
			//-------------------------------
			if ( cmd.hasOption("help") )
			{
				printHelp(options, "The option '--help' was passed.");
			}
			//-------------------------------
			// VERSION
			//-------------------------------
			else if ( cmd.hasOption("version") )
			{
				System.out.println();
				System.out.println(Version.getAppName()+" Version: " + Version.getVersionStr() + " JVM: " + System.getProperty("java.version"));
				System.out.println();
			}
			//-------------------------------
			// Check for correct number of cmd line parameters
			//-------------------------------
			else if ( cmd.getArgs() != null && cmd.getArgs().length > 0 )
			{
				String error = "Unknown options: " + StringUtil.toCommaStr(cmd.getArgs());
				printHelp(options, error);
			}
			//-------------------------------
			// Start AseTune, GUI/NOGUI will be determined later on.
			//-------------------------------
			else
			{
				new QueryWindow(cmd);
			}
		}
		catch (ParseException pe)
		{
			String error = "Error: " + pe.getMessage();
			printHelp(options, error);
		}
		catch (NormalExitException e)
		{
			// This was probably throws when checking command line parameters
			// do normal exit
		}
		catch (Exception e)
		{
			System.out.println();
			System.out.println("Error: " + e.getMessage());
			System.out.println();
			System.out.println("Printing a stacktrace, where the error occurred.");
			System.out.println("--------------------------------------------------------------------");
			e.printStackTrace();
			System.out.println("--------------------------------------------------------------------");
		}
	}
}

class NormalExitException
extends Exception
{
	private static final long serialVersionUID = 1L;
	public NormalExitException()
	{
		super();
	}
	public NormalExitException(String msg)
	{
		super(msg);
	}
}

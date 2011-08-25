/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdesktop.swingx.JXTable;

import asemon.utils.AseConnectionFactory;
import asemon.utils.Configuration;
import asemon.utils.ConnectionFactory;
import asemon.utils.SwingWorker;
import asemon.xmenu.TablePopupFactory;

import com.sybase.jdbcx.EedInfo;
import com.sybase.jdbcx.SybConnection;
import com.sybase.jdbcx.SybMessageHandler;

/**
 * This class creates a Swing GUI that allows the user to enter a SQL query.
 * It then obtains a ResultSetTableModel for the query and uses it to display
 * the results of the query in a scrolling JTable component.
 **/
public class QueryFrame
	extends JFrame
	implements SybMessageHandler, ConnectionFactory
{
	private static Logger _logger = Logger.getLogger(QueryFrame.class);
	private static final long serialVersionUID = 1L;

	private Connection  _conn      = null;
	private JTextArea	_query     = new JTextArea();        // A field to enter a query in
	private JButton     _exec      = new JButton("Exec");    // Execute the
	private JCheckBox   _showplan  = new JCheckBox("GUI Showplan", false);
	private JCheckBox   _rsInTabs  = new JCheckBox("Resultsets in Tabbed Panel", false);
	private JComboBox   _dbs_cobx  = new JComboBox();
	private JPanel      _resPanel  = new JPanel();
	private JLabel	    _msgline   = new JLabel("");	     // For displaying messages
	private int         _lastTabIndex    = -1;
	private boolean     _closeConnOnExit = true;
	private Font        _aseMsgFont      = null;
	private ArrayList   _resultCompList  = null;
	/**
	 * This constructor method creates a simple GUI and hooks up an event
	 * listener that updates the table when the user enters a new query.
	 **/
	public QueryFrame(Connection conn)
	{
		this(conn, null, true);
	}
	public QueryFrame(Connection conn, boolean closeConnOnExit)
	{
		this(conn, null, closeConnOnExit);
	}
	public QueryFrame(Connection conn, String sql)
	{
		this(conn, sql, true);
	}
	public QueryFrame(Connection conn, String sql, boolean closeConnOnExit)
	{
		super("ASEMon Query"); // Set window title
		ImageIcon icon = new ImageIcon(getClass().getResource("swing/images/query16.gif"));
		super.setIconImage(icon.getImage());
		
		_closeConnOnExit = closeConnOnExit;

		// Arrange to quit the program when the user closes the window
		addWindowListener(new WindowAdapter()
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

		// Set various components
//		_exec.setToolTipText("Executes the select sql statement above (Ctrl-e)(Alt+e)."); 
		_exec.setToolTipText("Executes the select sql statement above (Alt+e)."); 
		_exec.setMnemonic('e');
//		_exec.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));

//		_exec.setMnemonic(KeyEvent.);

		_showplan.setToolTipText("Show Graphical showplan for the sql statement.");
		
		_dbs_cobx.setToolTipText("Change database context.");
		_rsInTabs.setToolTipText("Check this if you want to have multiple result sets in individual tabs.");
		_query   .setToolTipText("Put your SQL query here.\n'go' statements is not allowed.\nIf you select text and press 'exec' only the highlighted text will be sent to the ASE.");

		// Place the components within this window
		Container contentPane = getContentPane();
		contentPane.setLayout(new MigLayout());
		contentPane.add(_query,     "width 100%, wrap");
		contentPane.add(_exec,      "split 4");
		contentPane.add(_dbs_cobx,  "");
		contentPane.add(_rsInTabs,  "");
		contentPane.add(_showplan,  "wrap");
		contentPane.add(new JScrollPane(_resPanel), "span 4, width 100%, height 100%");
		contentPane.add(_msgline, "dock south");

		// ACTION for "exec"
		_exec.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
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
	public void openTheWindow() 
	{
		openTheWindow(600, 400);
	}
	public void openTheWindow(int width, int height) 
	{
		this.setSize(width, width);

		// Create a Runnable to set the main visible, and get Swing to invoke.
		SwingUtilities.invokeLater(new Runnable() 
		{
			public void run() 
			{
				openTheWindowAsThread();
				System.out.println("openTheWindowAsThread() AFTER... tread is terminating...");
			}
		});
	}
	private void openTheWindowAsThread()
	{
		this.setVisible(true);
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
					QueryFrame.this, 
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
					QueryFrame.this, 
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
			return AseConnectionFactory.getConnection(null, appname);
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

		TablePopupFactory.createMenu(popup, 
			TablePopupFactory.TABLE_PUPUP_MENU_PREFIX, 
			Configuration.getInstance(Configuration.CONF), 
			table, this);

		TablePopupFactory.createMenu(popup, 
			"QueryFrame." + TablePopupFactory.TABLE_PUPUP_MENU_PREFIX, 
			Configuration.getInstance(Configuration.CONF), 
			table, this);
		
		if (popup.getComponentCount() == 0)
			return null;
		else
			return popup;
	}


	public void displayQueryResults(final String sql)
	{
		// If the SQL take a long time, we do not want to block other
		// user activities, so do the db access in a thread.
		SwingWorker w = new SwingWorker()
		{
			public Object construct()
			{
				if (_showplan.isSelected())
					new AsePlanViewer(_conn, sql);
				else
					displayQueryResults(_conn, sql);
				return null;
			}			
		};
		w.start();
		
//		SwingWorker<Integer, Integer> w = new SwingWorker<Integer, Integer>()
//		{
//			@Override
//			protected Integer doInBackground() throws Exception
//			{
//				displayQueryResults(_conn, _tmpSql);
//				return 1;
//			}
//		};
//		w.execute();
//		SwingUtilities.invokeLater(new Runnable()
//		{
//			public void run()
//			{
//				displayQueryResults(_conn, _tmpSql);
//			}
//		});
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

			SQLWarning sqlw  = null;
			Statement  stmnt = conn.createStatement();			
			ResultSet  rs    = null;
			int rowsAffected = 0;

			// a linked list where to "store" result sets or messages
			// before "displaying" them
			_resultCompList = new ArrayList();

			_logger.debug("Executing SQL statement: "+sql);
			// Execute
			boolean hasRs = stmnt.execute(sql);

			// iterate through each result set
			do
			{
				if(hasRs)
				{
					// Get next resultset to work with
					rs = stmnt.getResultSet();

					// Convert the ResultSet into a TableModel, which fits on a JTable
					ResultSetTableModel tm = new ResultSetTableModel(rs, true);

					// Create the JTable, using the just created TableModel/ResultSet
					JXTable tab = new JXTable(tm);
					tab.setSortable(true);
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

					// Check for warnings
					// If warnings found, add them to the LIST
					for (sqlw = rs.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
					{
						_logger.trace("--In loop, sqlw: "+sqlw);
						//compList.add(new JAseMessage(sqlw.getMessage()));
					}

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
				hasRs = stmnt.getMoreResults();

				_logger.trace( "--hasRs="+hasRs+", rowsAffected="+rowsAffected );
			}
			while (hasRs || rowsAffected != -1);

			// Check for warnings
			for (sqlw = stmnt.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
			{
				_logger.trace("====After read RS loop, sqlw: "+sqlw);
				//compList.add(new JAseMessage(sqlw.getMessage()));
			}

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
				_logger.trace("Only 1 RS");

				// Add ResultSet  
				_resPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0 0, wrap 1"));
				for (Iterator it=_resultCompList.iterator(); it.hasNext();)
				{
					Object o = it.next();
					if (o instanceof JTable)
					{
						JTable tab = (JTable) o;

						// JScrollPane is on _resPanel
						// So we need to display the table header ourself
						JPanel p = new JPanel(new MigLayout("insets 0 0 0 0, gap 0 0, wrap 1"));
						p.add(tab.getTableHeader(), "wrap");
						p.add(tab,                  "wrap");

						_resPanel.add(p, "");
					}
					else if (o instanceof JAseMessage)
					{
						JAseMessage msg = (JAseMessage) o;
						_logger.trace("1-RS: JAseMessage: "+msg.getText());
						_resPanel.add(msg, "grow, push");
					}
				}
			}
			else if (numOfTables > 1)
			{
				_logger.trace("Several RS: "+_resultCompList.size());
				
				if (_rsInTabs.isSelected())
				{
					// Add Result sets to individual tabs, on a JTabbedPane 
					JTabbedPane tabPane = new JTabbedPane();
					_resPanel.add(tabPane, "");

					int i = 1;
					for (Iterator it=_resultCompList.iterator(); it.hasNext(); i++)
					{
						Object o = it.next();
						if (o instanceof JTable)
						{
							JTable tab = (JTable) o;

							// JScrollPane is on _resPanel
							// So we need to display the table header ourself
							JPanel p = new JPanel(new MigLayout("insets 0 0 0 0, gap 0 0"));
							p.add(tab.getTableHeader(), "wrap");
							p.add(tab,                  "wrap");

							tabPane.addTab("Result "+i, p);
						}
						else if (o instanceof JAseMessage)
						{
							JAseMessage msg = (JAseMessage) o;
							_resPanel.add(msg, "grow, push");
							_logger.trace("JTabbedPane: JAseMessage: "+msg.getText());
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
				}
				else
				{
					// Add Result sets to individual panels, which are 
					// appended to the result panel
					_resPanel.setLayout(new MigLayout("insets 0 0 0 0, wrap 1"));
					int i = 1;
					for (Iterator it=_resultCompList.iterator(); it.hasNext(); i++)
					{
						Object o = it.next();
						if (o instanceof JTable)
						{
							JTable tab = (JTable) o;

							// JScrollPane is on _resPanel
							// So we need to display the table header ourself
							JPanel p = new JPanel(new MigLayout("insets 0 0, gap 0 0"));
							Border border = BorderFactory.createTitledBorder("ResultSet "+i);
							p.setBorder(border);
							p.add(tab.getTableHeader(), "wrap");
							p.add(tab,                  "wrap");
							_resPanel.add(p, "");
						}
						else if (o instanceof JAseMessage)
						{
							JAseMessage msg = (JAseMessage) o;
							_logger.trace("JPane: JAseMessage: "+msg.getText());
							_resPanel.add(msg, "grow, push");
						}
					}
				}
			}
			else
			{
				_logger.trace("NO RS: "+_resultCompList.size());
				_resPanel.setLayout(new MigLayout("insets 0 0 0 0, wrap 1"));

				StringBuilder sb = new StringBuilder();
				sb.append("<html>");
				sb.append("<head>");
				sb.append("<style type=\"text/css\">");
				sb.append("<!--");
				sb.append("body {font-family: Courier New; margin: 0px}");
				sb.append("-->");
				sb.append("</style>");
				sb.append("</head>");
				sb.append("<body>");
				// There might be "just" print statements... 
				for (Iterator it=_resultCompList.iterator(); it.hasNext();)
				{
					Object o = it.next();
					if (o instanceof JAseMessage)
					{
						JAseMessage msg = (JAseMessage) o;
//						msg.setFont( _aseMsgFont );
//						_logger.trace("NO-RS: JAseMessage: "+msg.getText());
//						_resPanel.add(msg, "");
//						sb.append("<P>").append(msg.getText()).append("</P>\n");
						sb.append(msg.getText()).append("<BR>\n");
					}
				}
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

			}
			
			// We're done, so clear the feedback message
			_msgline.setText(" ");
		}
		catch (SQLException ex)
		{
			// If something goes wrong, clear the message line
			_msgline.setText("Error: "+ex.getMessage());
			ex.printStackTrace();

			// Then display the error in a dialog box
			JOptionPane.showMessageDialog(
					QueryFrame.this, 
					new String[] { // Display a 2-line message
							ex.getClass().getName() + ": ", 
							ex.getMessage() },
					"Error", JOptionPane.ERROR_MESSAGE);
		}
		
		// In some cases, some of the area in not repainted
		// example: when no RS, but only messages has been displayed
		_resPanel.repaint();
	}
	
	private int countTables(ArrayList list)
	{
		int count = 0;
		for (Iterator it=list.iterator(); it.hasNext();)
		{
			Object o = it.next();
			if (o instanceof JTable)
			{
				count++;
			}
		}
		return count;
	}

	public SQLException messageHandler(SQLException sqe)
	{
		// Take care of some specific messages...
		int code = sqe.getErrorCode();
		StringBuilder sb = new StringBuilder();

		if (sqe instanceof EedInfo)
		{
			EedInfo m = (EedInfo) sqe;
//			m.getServerName();
//			m.getSeverity();
//			m.getState();
//			m.getLineNumber();
//			m.getStatus();
//			sqe.getMessage();
//			sqe.getErrorCode();
			_logger.debug(
					"Server='"+m.getServerName()+"', " +
					"MsgNum='"+sqe.getErrorCode()+"', " +
					"Severity='"+m.getSeverity()+"', " +
					"State='"+m.getState()+"', " +
					"Status='"+m.getStatus()+"', " +
					"Proc='"+m.getProcedureName()+"', " +
					"Line='"+m.getLineNumber()+"', " +
					"Msg: "+sqe.getMessage());
			
			if (m.getSeverity() <= 10)
			{
				sb.append(sqe.getMessage());
				
				// Discard empty messages
				String str = sb.toString();
				if (str == null || (str != null && str.trim().equals("")) )
					return null;
			}
			else
			{
				// Msg 222222, Level 16, State 1:
				// Server 'GORANS_1_DS', Line 1:
				//	mmmm

				sb.append("Msg ").append(sqe.getErrorCode())
					.append(", Level ").append(m.getSeverity())
					.append(", State ").append(m.getState())
					.append(":\n");

				boolean addComma = false;
				String str = m.getServerName();
				if ( str != null && !str.equals(""))
				{
					addComma = true;
					sb.append("Server '").append(str).append("'");
				}

				str = m.getProcedureName();
				if ( str != null && !str.equals(""))
				{
					if (addComma) sb.append(", ");
					addComma = true;
					sb.append("Procedure '").append(str).append("'");
				}

				str = m.getLineNumber() + "";
				if ( str != null && !str.equals(""))
				{
					if (addComma) sb.append(", ");
					addComma = true;
					sb.append("Line ").append(str).append(":");
					addComma = false;
					sb.append("\n");
				}
				sb.append(sqe.getMessage());
			}

			// If new-line At the end, remove it
			if ( sb.charAt(sb.length()-1) == '\n' )
			{
				sb.deleteCharAt(sb.length()-1);
			}
		}
		
		//if (code == 987612) // Just a dummy example
		//{
		//	_logger.info(getPreStr()+"Downgrading " + code + " to a warning");
		//	sqe = new SQLWarning(sqe.getMessage(), sqe.getSQLState(), sqe.getErrorCode());
		//}

		//-------------------------------
		// TREAT DIFFERENT MESSAGES
		//-------------------------------

		// 3604 Duplicate key was ignored.
		if (code == 3604)
		{
//			_logger.debug(getPreStr()+"Ignoring ASE message " + code + ": Duplicate key was ignored.");
//			super.messageAdd("INFO: Ignoring ASE message " + code + ": Duplicate key was ignored.");
//			return null;
		}


		// Not Yet Recovered
		// 921: Database 'xxx' has not been recovered yet - please wait and try again.
		// 950: Database 'xxx' is currently offline. Please wait and try your command again later.
		if (code == 921 || code == 950)
		{
		}

		// DEADLOCK
		if (code == 1205)
		{
		}

		// LOCK-TIMEOUT
		if (code == 12205)
		{
		}


		//
		// Write some extra info in some cases
		//
		// error   severity description
		// ------- -------- -----------
		//    208        16 %.*s not found. Specify owner.objectname or use sp_help to check whether the object exists (sp_help may produce lots of output).
		//    504        11 Stored procedure '%.*s' not found.
		//   2501        16 Table named %.*s not found; check sysobjects
		//   2812        16 Stored procedure '%.*s' not found. Specify owner.objectname or use sp_help to check whether the object exists (sp_help may produce lots of output).
		//   9938        16 Table with ID %d not found; check sysobjects.
		//  10303        14 Object named '%.*s' not found; check sysobjects.
		//  10337        16 Object '%S_OBJID' not found.
		//  11901        16 Table '%.*s' was not found.
		//  11910        16 Index '%.*s' was not found.
		//  18826         0 Procedure '%1!' not found.

		if (    code == 208 
		     || code == 504 
		     || code == 2501 
		     || code == 2812 
		     || code == 9938 
		     || code == 10303 
		     || code == 10337 
		     || code == 11901 
		     || code == 11910 
		     || code == 18826 
		   )
		{
//			_logger.info("MessageHandler for SPID "+getSpid()+": Current database was '"+this.getDbname()+"' while receiving the above error/warning.");
//			super.messageAdd("INFO: Current database was '"+this.getDbname()+"' while receiving the above error/warning.");
		}

		if (_resultCompList != null)
			_resultCompList.add(new JAseMessage( sb.toString() ));

		// Pass the Exception on.
		return null;
//		return sqe;
	}

	private class JAseMessage 
	extends JTextArea
	{

		private static final long serialVersionUID = 1L;

		public JAseMessage()
		{
			_init();
		}

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
	public static void main(String args[]) throws Exception
	{
		Properties log4jProps = new Properties();
		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		// Create the factory object that holds the database connection using
		// the data specified on the command line
    	try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    		//UIManager.setLookAndFeel(new SubstanceOfficeSilver2007LookAndFeel());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Connection conn = AseConnectionFactory.getConnection("goransxp", 5000, null, "sa", "", "AseMon-QueryFrame");


		// Create a QueryFrame component that uses the factory object.
		QueryFrame qf = new QueryFrame(conn, 
				"print '11111111'\n" +
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
				"",
				true);
		qf.openTheWindow();
	}
}

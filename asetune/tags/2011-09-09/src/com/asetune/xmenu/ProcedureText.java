/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.xmenu;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.asetune.gui.QueryWindow;


/**
 * @author gorans
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ProcedureText 
extends XmenuActionBase 
{
	private Connection _conn     = null;
	private String     _dbname   = null;
	private String     _procname = null;
	private int        _linenum  = -1;
	private boolean    _closeConnOnExit;

	/**
	 * 
	 */
	public ProcedureText() 
	{
		super();
	}

	/* (non-Javadoc)
	 * @see com.sybase.jisql.xmenu.XmenuActionBase#doWork()
	 */
	public void doWork() 
	{
		_conn     = getConnection();
		_dbname   = getParamValue(0);
		_procname = getParamValue(1);
		_linenum  = Integer.parseInt( getParamValue(2,"0") );
		_closeConnOnExit = isCloseConnOnExit();

		showText(_conn, _dbname, _procname, _linenum, true);
	}


	public void showText(Connection conn, String dbname, String procName, int line, boolean closeConn)
	{
		JPanel textPanel = new JPanel();
		//final JTextArea procText = new JTextArea();
//		final JTextArea procText = new LineNumberedPaper(0,0);
		final RSyntaxTextArea procText      = new RSyntaxTextArea();
		final RTextScrollPane procTextSroll = new RTextScrollPane(procText);
		final JFrame textFrame = new JFrame(procName);

		//procText.setText(sql);
		procText.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		procText.setHighlightCurrentLine(true);
		//procText.setLineWrap(true);
		//procTextSroll.setLineNumbersEnabled(true);

		if (closeConn)
		{
			textFrame.addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					if (_closeConnOnExit)
					{
						try { _conn.close(); }
						catch(SQLException sqle) { /*ignore*/ }
					}
				}
			});
		}

		createRightClickMenyPopup(procText);

		ActionListener action = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				textFrame.dispose();
			}
		};

//		JScrollPane scrollPane = new JScrollPane(procText);
		textPanel.setLayout(new BorderLayout());
//		procText.setBackground(Color.white);
		procText.setEnabled(true);
		procText.setEditable(false);

		String sqlStatement = "select c.text"
			+ " from "+dbname+"..sysobjects o, "+dbname+"..syscomments c"
			+ " where o.name = '"+procName+"'"
			+ "   and o.id = c.id"
			+ " order by c.number, c.colid2, c.colid";


		boolean found = false;
		try
		{
			Statement statement = conn.createStatement();
			ResultSet rs = statement.executeQuery(sqlStatement);
			while(rs.next())
			{
				found = true;
				String textPart = rs.getString(1);
				procText.append(textPart);
			}
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(null, "Executing SQL command '"+sqlStatement+"'. Found the following error:\n."+e, "Error", JOptionPane.ERROR_MESSAGE);
		}
		textPanel.add("Center", procTextSroll);
		JPanel buttonPanel = new JPanel();
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(action);
		buttonPanel.add(closeButton);
		textPanel.add("South",buttonPanel); 
		textFrame.getContentPane().add("Center", textPanel);
		textFrame.setSize(1000, 600);

		if ( ! found )
		{
			JOptionPane.showMessageDialog(null, "The stored procedure '"+procName+"' can't be found in database '"+dbname+"'.", "Error", JOptionPane.ERROR_MESSAGE);
		}
		else
		{
			setCaretToLineNumber(procText, line);
			textFrame.setVisible(true);
		}
	}

	public void setCaretToLineNumber(JTextArea text, int linenumber) 
	{
		text.setCaretPosition(0);
		if (linenumber<2) 
			return;

		StringTokenizer st = new StringTokenizer(text.getText(),"\n",true);
		int count = 0;
		int countRowAfter = 0;
		while (st.hasMoreTokens() & (linenumber>1))
		{
			String s = st.nextToken();
			count += s.length();
			if (s.equals("\n")) 
				linenumber--;
		}
		// Look for next row aswell, this so we can "mark" the linenumber
		if (st.hasMoreTokens())
		{
			String s = st.nextToken();
			countRowAfter = count + s.length();
		}

		text.setCaretPosition(count);
		text.select(count, countRowAfter);
	}


	
	private JPopupMenu _rightClickPopupMenu = null;
	private JComponent _rightClickComponent = null;
	private void createRightClickMenyPopup(JComponent jcomp)
	{
		_rightClickPopupMenu = new JPopupMenu();
		_rightClickComponent = jcomp;

		JMenuItem menuItem = null;

		menuItem = new JMenuItem("Procedure Text");
		menuItem.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JComponent comp = _rightClickComponent;
				if (comp instanceof JTextArea)
	            {
					JTextArea text = (JTextArea) comp;
					String selectedText = text.getSelectedText();

					String dbname   = _dbname;
					String procname = selectedText;
					int    linenum  = 0;

					// split: dbname.owner.procname
					if (selectedText != null)
					{
						String ss[] = selectedText.split("\\.");
						if (ss.length == 3)
						{
							dbname   = ss[0];
							procname = ss[2];
						}
					}
					showText(_conn, dbname, procname, linenum, false);
	            }
			}
		});
		_rightClickPopupMenu.add(menuItem);

		menuItem = new JMenuItem("Table Information");
		menuItem.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
//				String dbname   = "sybsystemprocs";
//				String tabname  = "sysobjects";
//				System.out.println("SHOW:Table Information:dbname='"+dbname+"', tablename='"+tabname+"'.");
//				//new AseTableInfo(_conn, dbname, tabname);

				JComponent comp = _rightClickComponent;
				if (comp instanceof JTextArea)
	            {
					JTextArea text = (JTextArea) comp;
					String selectedText = text.getSelectedText();

					String dbname   = _dbname;
					String tabname  = selectedText;

					// split: dbname.owner.procname
					if (selectedText != null)
					{
						String ss[] = selectedText.split("\\.");
						if (ss.length == 3)
						{
							dbname  = ss[0];
							tabname = ss[2];
						}
					}
					String sql = "exec "+dbname+"..sp_help '"+tabname+"'";

					QueryWindow qw = new QueryWindow(_conn, sql, false, QueryWindow.WindowType.JFRAME);
					qw.openTheWindow();
	            }
			}
		});
		_rightClickPopupMenu.add(menuItem);

		
		MouseAdapter ma = new java.awt.event.MouseAdapter() 
		{
			public void mousePressed (MouseEvent e) { maybeShowPopup(e); }
			public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }
			public void mouseClicked (MouseEvent e) { maybeShowPopup(e); }

			private void maybeShowPopup(MouseEvent e) 
			{
				if (e.isPopupTrigger())
				{
					System.out.println("_popup.show(e.getComponent()='"+e.getComponent()+"', e.getX()='"+e.getX()+"', e.getY()='"+e.getY()+"')");
					if (_rightClickPopupMenu != null)
					{
						//System.out.println("SHOW AT, e.getX()='"+e.getX()+"', e.getY()='"+e.getY()+"')");
						_rightClickPopupMenu.show(e.getComponent(), e.getX(), e.getY());
					}
				}
			}
		};
		
		jcomp.addMouseListener(ma);
	}
	
}

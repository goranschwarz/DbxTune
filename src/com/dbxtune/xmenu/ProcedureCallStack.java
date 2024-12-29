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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.xmenu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.dbxtune.gui.LineNumberedPaper;


/**
 * @author gorans
 */
public class ProcedureCallStack 
extends XmenuActionBase 
{
	private Connection _conn = null;
	private int        _spid = -1;
	private boolean    _closeConnOnExit;

	/**
	 * 
	 */
	public ProcedureCallStack() 
	{
		super();
	}

	/* (non-Javadoc)
	 * @see com.sybase.jisql.xmenu.XmenuActionBase#doWork()
	 */
	@Override 
	public void doWork() 
	{
		_conn = getConnection();
		_spid = Integer.parseInt( getParamValue(0) );
		_closeConnOnExit = isCloseConnOnExit();

		showCallStack();
	}


	public void getCallStack(JTextArea procText)
	{
		String sqlStatement = 
			"select ContextID, DBName, ObjectName, ObjectType \n" +
			"from master..monProcessProcedures \n" +
			"where SPID = " + _spid + " \n" +
			"order by ContextID";

		procText.setText("");
		boolean rows = false;
		try
		{
			Statement statement = _conn.createStatement();
			ResultSet rs = statement.executeQuery(sqlStatement);
			while(rs.next())
			{
				rows = true;
				//String ContextID  = rs.getString(1);
				String DBName     = rs.getString(2);
				String ObjectName = rs.getString(3);
				String ObjectType = rs.getString(4);

				String row = DBName + " " + ObjectName + " " + ObjectType + "\n";
				procText.append(row);
			}
			rs.close();
		}
		catch (Exception e)
		{
//			JXErrorPane.showDialog(null, new ErrorInfo(
//				"Error", "basicErroMessage", 
//				"Executing SQL statement: "+sqlStatement, 
//				"Category", e, Level.WARNING, null) );
			
			JOptionPane.showMessageDialog(null, "Executing SQL command '"+sqlStatement+"'. Found the following error:\n."+e, "Error", JOptionPane.ERROR_MESSAGE);
			//_window.openErrorWindow(e,_messages.getLocalized(
			//	_messages.JED06));
		}

		if ( ! rows )
		{
			procText.setText("NO Procedure is currentley executing.");
		}
		//setCaretToLineNumber(procText, line);
	}
	public void showCallStack()
	{
		JPanel textPanel = new JPanel();
		//final JTextArea procText = new JTextArea();
		final JTextArea procText = new LineNumberedPaper(0,0);
		final JFrame textFrame = new JFrame("Procedure Call stack for SPID "+_spid);
			/** ActionListener handles MouseClicks in the Frame's buttons
			 * In this case to close the Frame or save the text to a file
			 *@see  java.awt.event.ActionListener
			 */
		textFrame.addWindowListener(new WindowAdapter()
		{
			@Override 
			public void windowClosing(WindowEvent e)
			{
				if (_closeConnOnExit)
				{
					try { if (_conn != null) _conn.close(); }
					catch(SQLException sqle) { /*ignore*/ }
				}
			}
		});

		ActionListener action = new ActionListener()
		{
			@Override 
			public void actionPerformed(ActionEvent e)
			{
				if (e.getActionCommand().equals("Refresh"))
				{
					getCallStack(procText);
				}
				else
				{
					if (_closeConnOnExit)
					{
						try { if (_conn != null) _conn.close(); }
						catch(SQLException sqle) { /*ignore*/ }
					}
					textFrame.dispose();
				}
			}
		}
		;
		JScrollPane scrollPane = new JScrollPane(procText);
		textPanel.setLayout(new BorderLayout());
		procText.setBackground(Color.white);
		procText.setEnabled(true);
		procText.setEditable(false);

		textPanel.add("Center", scrollPane);
		JPanel buttonPanel = new JPanel();
		JButton refreshButton = new JButton("Refresh");
		JButton closeButton = new JButton("Close");
		refreshButton.addActionListener(action);
		closeButton.addActionListener(action);
		buttonPanel.add(refreshButton);
		buttonPanel.add(closeButton);
		textPanel.add("South",buttonPanel); 
		textFrame.getContentPane().add("Center", textPanel);
		textFrame.setSize(500, 300);

		getCallStack(procText);
		
		textFrame.setVisible(true);
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
}

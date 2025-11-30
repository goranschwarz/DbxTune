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
package com.dbxtune.xmenu;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.fife.ui.rtextarea.RTextScrollPane;

import com.dbxtune.gui.AsePlanViewer;
import com.dbxtune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.dbxtune.utils.StringUtil;

public class AseGuiShowplan
extends XmenuActionBase 
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private Connection _conn = null;
	private String     _SSQLID = null;
//	private boolean    _closeConnOnExit;

	/**
	 * 
	 */
	public AseGuiShowplan() 
	{
		super();
	}

	/**
	 * @see com.sybase.jisql.xmenu.XmenuActionBase#doWork()
	 */
	@Override 
	public void doWork() 
	{
		_conn = getConnection();
		_SSQLID = getParamValue(0);
//		_closeConnOnExit = isCloseConnOnExit();

		String queryPlan = getPlan();
		if (StringUtil.hasValue(queryPlan))
		{
//			ShowplanHtmlView.show(Type.ASE, queryPlan);
//			showText(queryPlan);

			AsePlanViewer pv = new AsePlanViewer(queryPlan);
			pv.setVisible(true);
		}
	}


	public String getPlan()
	{
		String sqlStatement = "select show_cached_plan_in_xml("+_SSQLID+",0,0)";

		String query_plan = null;
		
		try
		{
			Statement statement = _conn.createStatement();
			ResultSet rs = statement.executeQuery(sqlStatement);
			while(rs.next())
			{
				query_plan = rs.getString(1);
			}
			rs.close();
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(null, "Executing SQL command '"+sqlStatement+"'. Found the following error:\n."+e, "Error", JOptionPane.ERROR_MESSAGE);
		}

		return query_plan;
	}

	
//	public void showText(Connection conn, String dbname, String procName, int line, boolean closeConn)
	public void showText(String queryPlan)
	{
		JPanel textPanel = new JPanel();
		final RSyntaxTextAreaX textarea      = new RSyntaxTextAreaX();
		final RTextScrollPane  textareaSroll = new RTextScrollPane(textarea);
		final JFrame textFrame = new JFrame("XML Plan for SSQLID "+_SSQLID);

		queryPlan = 
			"<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->\n" +
			"<!-- Sorry: the HTML Plan isn't yet implemented...                         -->\n" +
			"<!-- In the meantime you have to read the XML Showplan :(                  -->\n" +
			"<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->\n" +
			"\n" +
			queryPlan;
		
		textarea.setText(queryPlan);

		textarea.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_XML);
		textarea.setHighlightCurrentLine(true);
		textarea.setCodeFoldingEnabled(true);
		//textarea.setLineWrap(true);
		textareaSroll.setLineNumbersEnabled(true);

		RSyntaxUtilitiesX.installRightClickMenuExtentions(textareaSroll, textFrame);

		ActionListener action = new ActionListener()
		{
			@Override 
			public void actionPerformed(ActionEvent e)
			{
				textFrame.dispose();
			}
		};

		textPanel.setLayout(new BorderLayout());
		textarea.setEnabled(true);

		textPanel.add("Center", textareaSroll);
		JPanel buttonPanel = new JPanel();
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(action);
		buttonPanel.add(closeButton);
		textPanel.add("South",buttonPanel); 
		textFrame.getContentPane().add("Center", textPanel);
		textFrame.setSize(1000, 600);

		textFrame.setVisible(true);
	}
}

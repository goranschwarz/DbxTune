/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.xmenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import com.asetune.gui.MainFrame;
import com.asetune.utils.Configuration;
import com.asetune.utils.OSCommand;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class SqlSentryPlanExplorer
extends XmenuActionBase 
{
	private static Logger _logger = Logger.getLogger(SqlSentryPlanExplorer.class);
	private Connection _conn = null;
//	private String     _planHandle = null;
//	private boolean    _closeConnOnExit;

	public final static String PROPKEY_SQL_PLAN_EXPLORER = "SqlSentryPlanExplorer";
	/**
	 * 
	 */
	public SqlSentryPlanExplorer() 
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
		String inputText = getParamValue(0);
//		_closeConnOnExit = isCloseConnOnExit();

		String queryPlan = null;
		if (StringUtil.hasValue(inputText))
		{
			if (inputText.startsWith("<ShowPlanXML "))
			{
				queryPlan = inputText;
			}
			else
			{
				queryPlan = getPlan(inputText);
			}
		}
		if (StringUtil.hasValue(queryPlan))
		{
			// Write to temp file
			File tempFile = null;
			try
			{
				// Write a file
				tempFile = File.createTempFile("sqlSrvPlan_", ".xml");
				tempFile.deleteOnExit();
				
				_logger.info("Writing temporary query plan file at " + tempFile.toURI());

				// Delete temp file when program exits.
				tempFile.deleteOnExit();

    			BufferedWriter out = new BufferedWriter(new FileWriter(tempFile));
    			out.write(queryPlan);
    			out.close();
			}
			catch (Exception e)
			{
				SwingUtils.showErrorMessage(null, "Problems Writing tempfile", 
						"Problems writing to tempfile '"+tempFile+"':\n\n" +
						e.getMessage()+"\n\n",
						e);
			}
			
			// Open the file via: SQL Sentry Plan Explorer
			String baseCmd = Configuration.getCombinedConfiguration().getProperty(PROPKEY_SQL_PLAN_EXPLORER, "C:\\Program Files\\SQL Sentry\\SQL Sentry Plan Explorer\\SQL Sentry Plan Explorer.exexxx");
			String cmd = baseCmd + " " + tempFile;
			try
			{
				OSCommand.execute(cmd);
//				OSCommand osCmd = OSCommand.execute(cmd);
//				String retVal = osCmd.getOutput();
			}
			catch (Exception e)
			{
				final Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);

				final JPanel     panel   = new JPanel(new MigLayout());
				final JLabel     cmd_lbl = new JLabel("Command");
				final JTextField cmd_txt = new JTextField(baseCmd);
				final JButton    cmd_but = new JButton("...");
				
				panel.add(cmd_lbl, "");
				panel.add(cmd_txt, "pushx, growx");
				panel.add(cmd_but, "");
				
				cmd_but.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						final JFileChooser fc = new JFileChooser();
						fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
						fc.setApproveButtonText("Save To File");
						fc.setDialogTitle("Choose a Command to view a SQL-Server Execution Plan");
						
						int returnValue = fc.showOpenDialog(MainFrame.getInstance());
						if (returnValue == JFileChooser.APPROVE_OPTION) 
						{
							cmd_txt.setText( fc.getSelectedFile()+"" );
							conf.setProperty(PROPKEY_SQL_PLAN_EXPLORER, cmd_txt.getText());
							conf.save();
						}
					}
				});
				cmd_txt.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						conf.setProperty(PROPKEY_SQL_PLAN_EXPLORER, cmd_txt.getText());
						conf.save();
					}
				});
				cmd_txt.addFocusListener(new FocusListener()
				{
					@Override public void focusGained(FocusEvent e) {}
					@Override public void focusLost(FocusEvent e)
					{
						if (conf != null)
						{
							conf.setProperty(PROPKEY_SQL_PLAN_EXPLORER, cmd_txt.getText());
							conf.save();
						}
					}
				});

				String htmlMsg = 
						"<html>"
						+ "<h3>Problems executing the Operating system command</h3>"
						+ "Command: <code>" + baseCmd  + "</code><br>"
						+ "Param 1: <code>" + tempFile + "</code><br>"
						+ "<br>"
						+ "Problem: <code>" + e.getMessage() + "</code><br>"
						+ "<br>"
						+ "If the command can't be found, you can specify what binary to run using the <br>"
						+ "property <code>"+PROPKEY_SQL_PLAN_EXPLORER+"=...</code> In the file <code>"+conf.getFilename()+"</code>.<br>"
						+ "<br>"
						+ "Or specify the command in the text field below. <b>Note</b>: The button \"...\" opens a file chooser dialog.<br>"
						+ "<br>"
						+ "SQL Sentry Plan Explorer can be downloaded at: <A HREF=\"http://www.sqlsentry.com/products/plan-explorer\">http://www.sqlsentry.com/products/plan-explorer</A>";

//				SwingUtils.showErrorMessageExt(MainFrame.getInstance(), "Problems Executing SQL Sentry Plan Explorer", htmlMsg, panel, e);
				SwingUtils.showErrorMessageExt(MainFrame.getInstance(), "Problems Executing SQL Sentry Plan Explorer", htmlMsg, null, panel);
			}
			
		}
	}


	public String getPlan(String planHandle)
	{
		String sqlStatement = "select query_plan from sys.dm_exec_query_plan("+planHandle+")";

		String query_plan = null;
		
		try
		{
			Statement statement = _conn.createStatement();
			ResultSet rs = statement.executeQuery(sqlStatement);
			while(rs.next())
			{
//				String dbid       = rs.getString(1);
//				String objectid   = rs.getString(2);
//				String number     = rs.getString(3);
//				String encrypted  = rs.getString(4);
//				String query_plan = rs.getString(5);

				query_plan = rs.getString("query_plan");
			}
			rs.close();
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(null, "Executing SQL command '"+sqlStatement+"'. Found the following error:\n."+e, "Error", JOptionPane.ERROR_MESSAGE);
		}

		return query_plan;
	}
}

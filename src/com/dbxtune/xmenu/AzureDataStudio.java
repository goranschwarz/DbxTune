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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.OSCommand;
import com.dbxtune.utils.SqlServerUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class AzureDataStudio
extends XmenuActionBase 
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private DbxConnection _conn = null;
//	private String     _planHandle = null;
//	private boolean    _closeConnOnExit;

	public final static String PROPKEY_AZURE_DATA_STUDIO = "AzureDataStudio";
	public final static String DEFAULT_AZURE_DATA_STUDIO = "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\Programs\\Azure Data Studio\\azuredatastudio.exe";

	public final static String PROPKEY_CMD_LINE_SWITCHES = "AzureDataStudio.cmd.switches";
	public final static String DEFAULT_CMD_LINE_SWITCHES = "--reuse-window";
	/**
	 * 
	 */
	public AzureDataStudio() 
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
			
			open(tempFile);
		}
	}


	public static void open(File tempFile)
	{
		// Open the file via: SQL Sentry Plan Explorer
		String baseCmd  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_AZURE_DATA_STUDIO, DEFAULT_AZURE_DATA_STUDIO);
		String switches = Configuration.getCombinedConfiguration().getProperty(PROPKEY_CMD_LINE_SWITCHES, DEFAULT_CMD_LINE_SWITCHES);
		String cmd = baseCmd + " " + switches + " " + tempFile;
		try
		{
			OSCommand.execute(cmd);
//			OSCommand osCmd = OSCommand.execute(cmd);
//			String retVal = osCmd.getOutput();
		}
		catch (Exception e)
		{
			final Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);

			final JPanel     panel   = new JPanel(new MigLayout());
			final JLabel     cmd_lbl = new JLabel("Command");
			final JTextField cmd_txt = new JTextField(baseCmd);
			final JButton    cmd_but = new JButton("...");

			final JLabel     sw_lbl  = new JLabel("Switches");
			final JTextField sw_txt  = new JTextField(switches);
			
			panel.add(cmd_lbl, "");
			panel.add(cmd_txt, "pushx, growx");
			panel.add(cmd_but, "wrap");

			panel.add(sw_lbl, "");
			panel.add(sw_txt, "pushx, growx, wrap");
			
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
						conf.setProperty(PROPKEY_AZURE_DATA_STUDIO, cmd_txt.getText());
						conf.save();
					}
				}
			});
			cmd_txt.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					conf.setProperty(PROPKEY_AZURE_DATA_STUDIO, cmd_txt.getText());
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
						conf.setProperty(PROPKEY_AZURE_DATA_STUDIO, cmd_txt.getText());
						conf.save();
					}
				}
			});

			sw_txt.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					conf.setProperty(PROPKEY_CMD_LINE_SWITCHES, sw_txt.getText());
					conf.save();
				}
			});
			sw_txt.addFocusListener(new FocusListener()
			{
				@Override public void focusGained(FocusEvent e) {}
				@Override public void focusLost(FocusEvent e)
				{
					if (conf != null)
					{
						conf.setProperty(PROPKEY_CMD_LINE_SWITCHES, sw_txt.getText());
						conf.save();
					}
				}
			});

			String htmlMsg = 
					"<html>"
					+ "<h3>Problems executing the Operating system command</h3>"
					+ "Command: <code>" + baseCmd  + "</code><br>"
					+ "Param 1: <code>" + switches + "</code><br>"
					+ "Param 2: <code>" + tempFile + "</code><br>"
					+ "<br>"
					+ "Problem: <code>" + e.getMessage() + "</code><br>"
					+ "<br>"
					+ "If the command can't be found, you can specify what binary to run using the <br>"
					+ "property <code>"+PROPKEY_AZURE_DATA_STUDIO+"=...</code> In the file <code>"+conf.getFilename()+"</code>.<br>"
					+ "<br>"
					+ "Or specify the command in the text field below. <b>Note</b>: The button \"...\" opens a file chooser dialog.<br>"
					+ "<br>"
					+ "Azure Data Studio can be downloaded at: <A HREF='https://azure.microsoft.com/en-us/products/data-studio/'>https://azure.microsoft.com/en-us/products/data-studio/</A>";

			SwingUtils.showErrorMessageExt(MainFrame.getInstance(), "Problems Executing Azure Data Studio", htmlMsg, null, panel);
		}
	}

	public String getPlan(String planHandleHexStr)
	{
		String query_plan = null;
		try
		{
			query_plan = SqlServerUtils.getXmlQueryPlan(_conn, planHandleHexStr);
		}
		catch (SQLException e)
		{
			JOptionPane.showMessageDialog(null, "Executing 'SqlServerUtils.getXmlQueryPlan()'. Found the following error:\n."+e, "Error", JOptionPane.ERROR_MESSAGE);
		}
		return query_plan;
	}
}

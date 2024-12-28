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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.asetune.Version;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.AseSqlScript;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class InstallSqlFromUrlDialog
extends JDialog
implements ActionListener
{
	private static Logger _logger = Logger.getLogger(InstallSqlFromUrlDialog.class);
	private static final long serialVersionUID = 1L;

	private JPanel          _top_panel         = null;
	private JPanel          _text_panel        = null;
	private JPanel          _ok_panel          = null;

	private JButton         _exec_but          = new JButton("Execute");
	private JButton         _close_but         = new JButton("Close");
	private JLabel          _feedback_lbl      = new JLabel("");

	private JSplitPane      _splitPane         = null;

	private RSyntaxTextAreaX _ddl_txt          = new RSyntaxTextAreaX();
	private RTextScrollPane  _ddl_scroll       = new RTextScrollPane(_ddl_txt);
	
	private RSyntaxTextAreaX _output_txt       = new RSyntaxTextAreaX();
	private RTextScrollPane  _output_scroll    = new RTextScrollPane(_output_txt);

	private JLabel           _name_lbl         = new JLabel("Name");
	private JLabel           _name_lbl2        = new JLabel();

	private JLabel           _dbname_lbl       = new JLabel("In Database");
	private JTextField       _dbname_txt       = new JTextField();
	private JButton          _dbname_but       = new JButton("Apply");

	private JLabel           _url_lbl          = new JLabel("URL");
	private JTextField       _url_txt          = new JTextField();
	private JButton          _url_but          = new JButton("Apply");

	private JLabel           _postExec_lbl     = new JLabel("Post SQL");
	private RSyntaxTextAreaX _postExec_txt     = new RSyntaxTextAreaX();
	private JButton          _postExec_but     = new JButton("Apply");

	private Window           _owner;
	private String           _sqlUrlDdlText;
	private String           _sqlDialect;

	private String           _name;
	private String           _dbname;
	private String           _urlStr;

	private DbxConnection    _dbxConn;

	public InstallSqlFromUrlDialog(Window owner)
	{
		this(owner, null, null, null, null, null);
	}

	public InstallSqlFromUrlDialog(Window owner, String text, String dbname, String name, String urlStr)
	{
		this(owner, text, null, dbname, name, urlStr);
	}

	public InstallSqlFromUrlDialog(Window owner, String text, String sqlDialect, String dbname, String name, String urlStr)
	{
		super();
		
		_owner         = owner;
		_sqlUrlDdlText = text;
		_sqlDialect    = sqlDialect;
		_name          = name;
		_dbname        = dbname;
		_urlStr        = urlStr;
		
		if (StringUtil.isNullOrBlank(_sqlDialect))
			_sqlDialect = AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL;

		init();
	}

	public void setConnection(DbxConnection conn)
	{
		_dbxConn = conn;
	}
	public DbxConnection getConnection()
	{
		return _dbxConn;
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
		setTitle("Install SQL from URL"); // Set window title
		
		// Set the icon
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/ddlgen_16.png");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/ddlgen_32.png");
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

//		setLayout( new BorderLayout() );
		setLayout( new MigLayout("insets 0 0 0 0") );
		
		loadProps();

		_top_panel  = createTopPanel();
		_text_panel = createTextPanel();
//		_ok_panel   = createOkPanel();

		add(_top_panel,  "grow, push, wrap");
		add(_text_panel, "grow, push, wrap");
//		add(_ok_panel,   "pushx, growx, wrap");
		
//		_splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, _ddl_scroll, _output_scroll);
//		add(_splitPane, BorderLayout.CENTER);
//
//		add(_top_panel,  BorderLayout.NORTH);
//		add(_text_panel, BorderLayout.CENTER);
////		add(_splitPane,  BorderLayout.CENTER);
//		add(_ok_panel,   BorderLayout.SOUTH);

		pack();
		getSavedWindowProps();

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				saveProps();
				destroy();
			}
		});
	}

	/** call this when window is closing */
	private void destroy()
	{
		// cleanup...
		dispose();
	}
	
	@Override
	public void setVisible(boolean b)
	{
		super.setVisible(b);
		loadUrl();
		setDdlText();
	}

	private JPanel createTopPanel()
	{
		JPanel panel = SwingUtils.createPanel("Input", true);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		
//		_object_txt.setSyntaxEditingStyle(_sqlDialect);
//		RSyntaxUtilitiesX.installRightClickMenuExtentions(_object_scroll, this);
//
//		_object_txt.setText(_sqlText);
//
//		// Set caret at "top"
//		_object_txt.setCaretPosition(0);
//
//		panel.add(_object_scroll, "push, grow, wrap");

		JLabel txt = new JLabel(""
				+ "<html>"
				+ "lajdhf lakjdsfhalkjdsh lakjsdh lkajdhf lkajsdhf lkajdsflkajdsf<br>"
				+ "askljdhfalkjdhf akjhsd kjah dsfjahsdfssds<br>"
				+ "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa aaaaaaaaaaaaaaaaaaaaaa"
				+ "</html>");

		_name_lbl2 .setText(_name);
		_url_txt   .setText(_urlStr);
		_dbname_txt.setText(_dbname);

		String toolTipText;

		toolTipText = "<html>Enter the URL/HTTP Address you want to fetch the SQL Text from.<br><b>NOTE:</b> Press <b>Return</b> in this field or press the <i>'Apply Button'</i> to load the URL.</b></html>";
		_url_lbl   .setToolTipText(toolTipText);
		_url_txt   .setToolTipText(toolTipText);
		_url_but   .setToolTipText("Load the URL...");

		toolTipText = "<html>Name of the database you want to install the SQL Text or Procedure in.<br><b>NOTE:</b> Press <b>Return</b> in this field or press the <i>'Apply Button'</i> to set database context in below SQL Text.</html>";
		_dbname_lbl.setToolTipText(toolTipText);
		_dbname_txt.setToolTipText(toolTipText);
		_dbname_but.setToolTipText("Set dbname in the below SQL Text");

		toolTipText = "<html>In here you can for example put 'GRANT EXEC on procname to someUser'.<br><b>NOTE:</b> Press press the <i>'Apply Button'</i> to set 'Post SQL Text' below SQL Text.</html>";
		_postExec_lbl.setToolTipText(toolTipText);
		_postExec_txt.setToolTipText(toolTipText);
		_postExec_but.setToolTipText("Set 'Post Exec SQL' in the below SQL Text");

		_exec_but.setToolTipText("<html>Execute the text in the Editor<br><b>Check the 'Output Panel' at the bottom, to check for success!</b></html>");
		_close_but.setToolTipText("Close the dialog.");
		
		_postExec_txt.setSyntaxEditingStyle(_sqlDialect);
//		RSyntaxUtilitiesX.installRightClickMenuExtentions(_postExec_scroll, this);
		
//		panel.add(txt,       "push, grow, wrap");

		panel.add(_name_lbl,      "");
		panel.add(_name_lbl2,     "push, grow, wrap");

		panel.add(_url_lbl,       "");
		panel.add(_url_txt,       "push, grow");
		panel.add(_url_but,       "wrap");

		panel.add(_dbname_lbl,    "");
		panel.add(_dbname_txt,    "push, grow");
		panel.add(_dbname_but,    "wrap");

		panel.add(_postExec_lbl,  "");
		panel.add(_postExec_txt,  "push, grow");
		panel.add(_postExec_but,  "wrap");

		panel.add(_exec_but,      "skip, split");
		panel.add(_close_but,     "");
		panel.add(_feedback_lbl,  "");

		_exec_but    .addActionListener(this);
		_close_but   .addActionListener(this);

		_url_txt     .addActionListener(this);
		_url_but     .addActionListener(this);

		_dbname_txt  .addActionListener(this);
		_dbname_but  .addActionListener(this);

//		_postExec_txt.addActionListener(this);
		_postExec_but.addActionListener(this);
		
		return panel;
	}

	private JPanel createTextPanel()
	{
		JPanel panel = SwingUtils.createPanel("Xxx", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		
		JPanel sqlPanel = SwingUtils.createPanel("SQL Text", true);
		sqlPanel.setLayout(new MigLayout("insets 0 0 0 0"));
		
		JPanel outPanel = SwingUtils.createPanel("Excute Output", true);
		outPanel.setLayout(new MigLayout("insets 0 0 0 0"));
		

		_ddl_txt.setSyntaxEditingStyle(_sqlDialect);
		RSyntaxUtilitiesX.installRightClickMenuExtentions(_ddl_scroll, this);

		_output_txt.setSyntaxEditingStyle(_sqlDialect);
		RSyntaxUtilitiesX.installRightClickMenuExtentions(_output_scroll, this);


		_ddl_txt.setText(_sqlUrlDdlText);

		// Set caret at "top"
		_ddl_txt.setCaretPosition(0);

		_output_scroll.setMinimumSize( new Dimension(10, 60) );
		

		sqlPanel.add(_ddl_scroll   , "push, grow, wrap");
		outPanel.add(_output_scroll, "push, grow, wrap");

		_splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sqlPanel, outPanel);
		_splitPane.setDividerLocation(0.5);
		_splitPane.setResizeWeight(1.0);


		panel.add(_splitPane, "push, grow, wrap");

		
		return panel;
	}

//	private JPanel createOkPanel()
//	{
//		JPanel panel = SwingUtils.createPanel("TreeView", false);
//		panel.setLayout(new MigLayout("insets 0 0 0 0"));
////		panel.setLayout(new MigLayout());
//		
//		panel.add(new JLabel(),   "pushx, growx");
//		panel.add(_close_but,     "gapright 5, tag right");
//
//		_close_but.addActionListener(this);
//
//		return panel;
//	}


	/**
	 * IMPLEMENTS: ActionListener
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		
		if (_exec_but.equals(source))
		{
			execute();
			saveProps();
		}

		if (_close_but.equals(source))
		{
			saveProps();
			setVisible(false);
		}

		if (_url_but.equals(source) || _url_txt.equals(source))
		{
			loadUrl();
			setDdlText();
		}

		if (_dbname_but.equals(source) || _dbname_txt.equals(source))
		{
			setDdlText();
		}

		if (_postExec_but.equals(source) || _postExec_txt.equals(source))
		{
			setDdlText();
		}

		saveProps();
	}

	private void setDdlText()
	{
		String sqlInput = _sqlUrlDdlText;
		String dbname   = _dbname_txt.getText();
		String urlStr   = _url_txt   .getText();
		
		String installDbname = dbname;
		if (StringUtil.isNullOrBlank(installDbname))
		{
			installDbname = "master";
		}

		String sqlPostInstall = _postExec_txt.getText().trim();

		// Add 'go' if postInstall does not have it...
		if (StringUtil.hasValue(sqlPostInstall))
		{
			if ( ! StringUtils.endsWithIgnoreCase(sqlPostInstall, "go"))
			{
				sqlPostInstall += "\ngo\n";
			}
		}
		
		String sqlInputNeedsGo = "\ngo\n";
		if (StringUtils.endsWithIgnoreCase(sqlInput.trim(), "go"))
			sqlInputNeedsGo = "";


		String sqlToExecute = ""
				+ "/* -------------------------------------------------------------------------------------- \n"
				+ "** -- The below proecude/DDL text was fetched from: \n"
				+ "** -- " + urlStr + " \n"
				+ "** -------------------------------------------------------------------------------------- \n"
				+ "** -- Actions: \n"
				+ "** --  * Possibly change the database you want to install it in. \n"
				+ "** --  * And add possibly: GRANT EXEC on procname TO public|userName \n"
				+ "** -- Then execute to install it. \n"
				+ "** -------------------------------------------------------------------------------------- \n"
				+ "*/ \n"
				+ "use " + installDbname + "    /*** <<<<< INSTALL INTO THIS DATABASE <<<<< ***/ \n"
				+ "go \n"
				+ "\n"
				+ sqlInput
				+ sqlInputNeedsGo
				+ "/* -------------------------------------------------------------------------------------- \n"
				+ "** -- Post Install actions. \n"
				+ "** -------------------------------------------------------------------------------------- \n"
				+ "*/ \n"
				+ sqlPostInstall
				+ "print '--END-OF-INSTALL--' \n"
				+ "go \n"
				;

		_ddl_txt.setText(sqlToExecute);

		// If NO post install: Set cursor at START
		if (StringUtil.isNullOrBlank(sqlPostInstall))
		{
			_ddl_txt.setCaretPosition(0);
		}
	}

	private void loadUrl()
	{
		String urlStr = _url_txt.getText();

		if (StringUtil.hasValue(_urlStr) && _urlStr.equals(urlStr))
			_name_lbl2.setText(_name);
		else
			_name_lbl2.setText("<html><b>The URL Was changed from the origin.</b></html>");
			
		String output = null;
		try
		{
			// first get the SQL Code
			URL url = new URL(urlStr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			int    responseCode = conn.getResponseCode();
			String responseMsg  = conn.getResponseMessage();

			// success
			if (responseCode == HttpURLConnection.HTTP_OK) 
			{
				StringBuffer response = new StringBuffer();

//				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
				// If the file has a BOM (Byte order mark), then read/discard it... 
				// If the BOM is NOT removed, then strange problems may appear, as "Incorrect Syntax near ''" or similar, due to the BOM character is in the text (but NOT visible)
				BufferedReader in = new BufferedReader(new InputStreamReader( new BOMInputStream(conn.getInputStream()) ));
				String inputLine;

				while ((inputLine = in.readLine()) != null) 
				{
					response.append(inputLine).append("\n");
				}
				in.close();

				output = response.toString();
			} 
			else 
			{
				SwingUtils.showErrorMessage("Problems Getting Procedure code from '" + urlStr + "'.", responseCode + " - " + responseMsg, null);
				_sqlUrlDdlText = ""
						+ "-- Problems Getting Procedure code from '" + urlStr + "'.\n" 
						+ "-- URL Response Code: " + responseCode + "\n"
						+ "-- URL Response Message: " + responseMsg;
				return;
			}
		}
		catch (Throwable t)
		{
			SwingUtils.showErrorMessage("Problems Getting Procedure code from '" + urlStr + "'.", t.getMessage(), t);
			_sqlUrlDdlText = ""
					+ "-- Problems Getting Procedure code from '" + urlStr + "'.\n" 
					+ "-- Exception Message: " + t.getMessage() + "\n";
			return;
		}
		
		if (StringUtil.isNullOrBlank(output))
		{
			SwingUtils.showErrorMessage("Problems Getting Procedure code from '" + urlStr + "'.", "The DDL Text is empty.", null);
			_sqlUrlDdlText = ""
					+ "-- Problems Getting Procedure code from '" + urlStr + "'.\n" 
					+ "-- The DDL Text is empty \n";
			return;
		}
		
		// do some "changes" to some to the scripts
//		if ("https://raw.githubusercontent.com/olahallengren/sql-server-maintenance-solution/refs/heads/master/MaintenanceSolution.sql".equals(urlStr))
		if (urlStr.endsWith("MaintenanceSolution.sql"))
		{
			output = output.replace("USE [master] -- Specify the database in which the objects will be created", "--USE [master] -- Specify the database in which the objects will be created");
		}

		_sqlUrlDdlText = output;
	}

	private void execute()
	{
		//_dbxConn;
		
		_feedback_lbl.setForeground(_name_lbl.getForeground());
		try
		{
			AseSqlScript sqlScript = new AseSqlScript(_dbxConn, 0, true);

			String sql = _ddl_txt.getText();
			String output = sqlScript.executeSqlStr(sql, true);
			
			_output_txt.setText(output);

			sqlScript.close();
			
			_feedback_lbl.setText("Check the 'Output Panel' at the bottom, to check for success!");
		}
		catch (SQLException ex)
		{
			_feedback_lbl.setForeground(Color.RED);
			_feedback_lbl.setText("ERROR... Check the 'Output Panel' at the bottom, to check for success!");

			String msg = ""
					+ "ErrorCode: " + ex.getErrorCode() + "\n"
					+ "ErrorText: " + ex.getMessage()   + "\n"
					;
			_output_txt.setText(msg);
		}

		_output_txt.setCaretPosition(0);
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

		if (isVisible())
		{
    		conf.setLayoutProperty("installSqlFromUrl.dialog.window.width",  this.getSize().width);
    		conf.setLayoutProperty("installSqlFromUrl.dialog.window.height", this.getSize().height);
    		conf.setLayoutProperty("installSqlFromUrl.dialog.window.pos.x",  this.getLocationOnScreen().x);
    		conf.setLayoutProperty("installSqlFromUrl.dialog.window.pos.y",  this.getLocationOnScreen().y);
		}

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
		// TAB: s
		//----------------------------------
		int width  = conf.getLayoutProperty("installSqlFromUrl.dialog.window.width",  SwingUtils.hiDpiScale(600));
		int height = conf.getLayoutProperty("installSqlFromUrl.dialog.window.height", SwingUtils.hiDpiScale(240));
		int x      = conf.getLayoutProperty("installSqlFromUrl.dialog.window.pos.x",  -1);
		int y      = conf.getLayoutProperty("installSqlFromUrl.dialog.window.pos.y",  -1);
		if (width != -1 && height != -1)
		{
			this.setSize(width, height);
		}
		if (x != -1 && y != -1)
		{
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
				this.setLocation(x, y);
		}
	}
	/*---------------------------------------------------
	** END: implementing saveProps & loadProps
	**---------------------------------------------------
	*/	
}

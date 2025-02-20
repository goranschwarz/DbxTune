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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.gui.swing.WaitForExecDialog;
import com.dbxtune.gui.swing.WaitForExecDialog.BgExecutor;
import com.dbxtune.utils.AseConnectionFactory;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.RepServerUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;
import com.dbxtune.utils.TimeUtils;

import net.miginfocom.swing.MigLayout;

public class RsResumeSuspendDialog
extends JDialog
implements ActionListener, FocusListener
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = 1L;

	public enum RsThreadType {DSI, REP_AGENT}
	public enum ActionType   {RESUME, SUSPEND}

	private Window             _owner                    = null;
	private Connection         _conn                     = null;
	private ActionType         _actionType               = null;
	private RsThreadType       _rsThreadType             = null;
	private String             _name                     = null;
	private String             _dsname                   = null;
	private String             _dbname                   = null;

//	private JPanel             _topPanel                 = null;
	private JButton            _ok                       = new JButton("OK");
	private JButton            _cancel                   = new JButton("Cancel");
	private JLabel             _warning_lbl              = new JLabel("Warning messages, goes in here");

	private JPanel             _raPanel                  = null;
	private JLabel             _raInfo_lbl               = new JLabel();
	private JCheckBox          _raResume_chk             = new JCheckBox("Resume Replication Agent", true);
	private JLabel             _raSrvName_lbl            = new JLabel("Server");
	private JTextField         _raSrvName_txt            = new JTextField("");
	private JLabel             _raDbName_lbl             = new JLabel("DB Name");
	private JTextField         _raDbName_txt             = new JTextField("");
	private JLabel             _raUsername_lbl           = new JLabel("Username");
	private JTextField         _raUsername_txt           = new JTextField("");
	private JLabel             _raPassword_lbl           = new JLabel("Password");
	private JTextField         _raPassword_txt           = new JPasswordField("");
	private JCheckBox          _raPassword_chk           = new JCheckBox("Save Password", true);

	private JPanel             _dsiPanel                 = null;
	private JLabel             _dsiInfo_lbl              = new JLabel();
	private JCheckBox          _dsiResume_chk            = new JCheckBox("Resume DSI", true);
	private JCheckBox          _dsiResumeSkipFirst_chk   = new JCheckBox("Skip first transaction", false);
	private JCheckBox          _dsiResumeSkipNumTran_chk = new JCheckBox("Skip first X transactions", false);
	private SpinnerNumberModel _dsiResumeSkipNumTran_spm = new SpinnerNumberModel(1, 1, 9999, 1);
	private JSpinner           _dsiResumeSkipNumTran_sp  = new JSpinner(_dsiResumeSkipNumTran_spm);
	private JCheckBox          _dsiResumeExecSysTran_chk = new JCheckBox("Execute System Transaction", false);

	private JCheckBox          _dsiSuspendNormal_chk     = new JCheckBox("Wait for current transaction to complete", true);
	private JCheckBox          _dsiSuspendWithNowait_chk = new JCheckBox("With nowait", false);
	
	public RsResumeSuspendDialog(Window owner, Connection conn, String name, ActionType actionType, RsThreadType rsThreadType)
	{
		super(owner);
		
		_owner        = owner;
		_conn         = conn;
		_actionType   = actionType;
		_rsThreadType = rsThreadType;
		_name         = name;
		
		String[] sa = name.split("\\.");
		_dsname = sa.length >= 1 ? sa[0] : null;
		_dbname = sa.length >= 2 ? sa[1] : null;

		init();
	}

	private void init()
	{
		if (_actionType == ActionType.RESUME)
		{
			if (_rsThreadType == RsThreadType.REP_AGENT)
				setTitle(_name + " - Start Replication Agent");
			else
				setTitle(_name + " - Resume Connection");
		}
		else if (_actionType == ActionType.SUSPEND)
		{
			if (_rsThreadType == RsThreadType.REP_AGENT)
				setTitle(_name + " - Stop Replication Agent");
			else
				setTitle(_name + " - Suspend Connection");
		}
		else
			throw new RuntimeException("Unknown action type: "+_actionType);


		setContentPane(createPanel());
		loadProps();
		actionPerformed(null); // to set initial values etc.
		
		pack();
//		SwingUtils.setLocationCenterParentWindow(null, this);
		setLocationRelativeTo(_owner);

		// Window size can not be "smaller" than the minimum size
		// If so "OK" button etc will be hidden.
		SwingUtils.setWindowMinSize(this);
	}

	private JPanel createPanel()
	{
		JPanel panel = new JPanel();
		// panel.setLayout(new MigLayout("","","")); // insets Top Left Bottom
		// Right
		panel.setLayout(new MigLayout("insets 10 10", "", "")); // insets Top Left Bottom Right

		_warning_lbl  .setToolTipText("Why we can't press OK.");
		_warning_lbl  .setForeground(Color.RED);
		_warning_lbl  .setHorizontalAlignment(SwingConstants.RIGHT);
		_warning_lbl  .setVerticalAlignment(SwingConstants.BOTTOM);

		// Add Action and NextTime panel
		_raPanel  = createRaPanel();
		_dsiPanel = createDsiPanel();
		panel.add(_raPanel,  "push, grow, wrap, hidemode 3");
		panel.add(_dsiPanel, "push, grow, wrap, hidemode 3");

		// ADD the OK, Cancel, Apply buttons
		panel.add(_warning_lbl,  "split, pushx, growx");
		panel.add(_ok,           "tag ok,     gap top 20, split, bottom, right, push");
		panel.add(_cancel,       "tag cancel, split, bottom");
//		panel.add(_ok,           "tag ok");
//		panel.add(_cancel,       "tag cancel");

		// ADD ACTIONS TO COMPONENTS
		_ok    .addActionListener(this);
		_cancel.addActionListener(this);

		_dsiPanel.setVisible(false);
		_raPanel .setVisible(false);
		if (_rsThreadType == RsThreadType.DSI) 
		{
			_dsiPanel.setVisible(true);
		}
		else if (_rsThreadType == RsThreadType.REP_AGENT)
		{
			_raPanel .setVisible(true);
		}

		return panel;
	}
	

	private JPanel createRaPanel()
	{
		JPanel panel = SwingUtils.createPanel("Replication Agent", true);
//		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		panel.setLayout(new MigLayout());

		// Tooltip
		panel           .setToolTipText("Replication Agent Information.");
		_raResume_chk   .setToolTipText("Check this box if you want to resume ");
		_raSrvName_lbl .setToolTipText("Source Server, to start the Rep Agent");
		_raSrvName_txt .setToolTipText("Source Server, to start the Rep Agent");
		_raDbName_lbl  .setToolTipText("Source Database, to start the Rep Agent");
		_raDbName_txt  .setToolTipText("Source Database, to start the Rep Agent");
		_raUsername_lbl.setToolTipText("Username used to login to the Source Server, to start the Rep Agent");
		_raUsername_txt.setToolTipText("Username used to login to the Source Server, to start the Rep Agent");
		_raPassword_lbl.setToolTipText("Password used to login to the Source Server, to start the Rep Agent");
		_raPassword_txt.setToolTipText("Password used to login to the Source Server, to start the Rep Agent");
		_raPassword_chk.setToolTipText("Save the password in the configuration file, and YES it's encrypted.");

		if (_actionType == ActionType.RESUME)
		{
			// If you change the position of <br> the window width will change
			String htmlInfo = "<html>" +
				"To <b>start</b> the Replication Agent thread, which is located in <br>" +
				"the source server (normally in ASE) I need to connect to the <br>" +
				"source server, therefore I need a username and password...<br>" +
				"<br>" +
				"If the RepAgent is already running, it will first be restarted<br>" +
				"<br>" +
				"In the server I will execute: <code>sp_start_rep_agent dbname</code><br>" +
				"<br>" +
				"</html>";
			_raInfo_lbl.setText(htmlInfo);
			// Do not set a small size here, keep the *width* of the MAXIMUM html width
			//_raInfo_lbl.setPreferredSize(new Dimension(10, 10));
		}
		else
		{
			// If you change the position of <br> the window width will change
			String htmlInfo = "<html>" +
				"To <b>stop</b> the Replication Agent thread, which is located in <br>" +
				"the source server (normally in ASE) I need to connect to the <br>" +
				"source server, therefore I need a username and password...<br>" +
				"<br>" +
				"In the server I will execute: <code>sp_stop_rep_agent dbname</code><br>" +
				"<br>" +
				"If the RepAgent has not been stopped after 20 seconds<br>" +
				"The 'nowait' option to <code>sp_stop_rep_agent</code> will be tried.<br>" +
				"<br>" +
				"</html>";
			_raInfo_lbl.setText(htmlInfo);
			// Do not set a small size here, keep the *width* of the MAXIMUM html width
			//_raInfo_lbl.setPreferredSize(new Dimension(10, 10));
		}


		panel.add(_raInfo_lbl,        "span 2, grow, wrap");

//		panel.add(_raResume_chk,      "span 2, wrap");

		panel.add(_raSrvName_lbl,     "");
		panel.add(_raSrvName_txt,     "growx, pushx, wrap");

		panel.add(_raDbName_lbl,      "");
		panel.add(_raDbName_txt,      "growx, pushx, wrap");

		panel.add(_raUsername_lbl,    "");
		panel.add(_raUsername_txt,    "growx, pushx, wrap");

		panel.add(_raPassword_lbl,    "");
		panel.add(_raPassword_txt,    "growx, pushx, wrap");
		panel.add(_raPassword_chk,    "skip, wrap 10");

		
		// disable input to some fields
		_raSrvName_txt.setEnabled(false);
		_raDbName_txt .setEnabled(false);

		// initial values
		_raSrvName_txt.setText(_dsname);
		_raDbName_txt .setText(_dbname);

		// for validation
		_raResume_chk  .addActionListener(this);
		_raUsername_txt.addActionListener(this);
		_raPassword_txt.addActionListener(this);
		_raPassword_chk.addActionListener(this);

		// Focus action listener
		_raUsername_txt.addFocusListener(this);
		_raPassword_txt.addFocusListener(this);

		return panel;
	}

	
	private JPanel createDsiPanel()
	{
		JPanel panel = SwingUtils.createPanel("Data Server Interface", true);
//		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		panel.setLayout(new MigLayout());

		// Tooltip
		panel                    .setToolTipText("<html>Data Server Interface Information.</html>");
		_dsiResume_chk           .setToolTipText("<html>Start the DSI thread, apply first transaction that is in the outbound queue.<br><br>Do: <code>resume connection to SRV.DB</code></html>");
		_dsiResumeSkipFirst_chk  .setToolTipText("<html>Start the DSI thread, but <b>skip</b> first transaction that is in the outbound queue.<br><br>Do: <code>resume connection to SRV.DB skip transaction</code></html>");
		_dsiResumeSkipNumTran_chk.setToolTipText("<html>Start the DSI thread, but <b>skip</b> first # number of transactions that is in the outbound queue.<br><br>Do: <code>resume connection to SRV.DB skip # transaction</code></html>");
		_dsiResumeSkipNumTran_sp .setToolTipText("<html>Number of transactions to skip</html>");
		_dsiResumeExecSysTran_chk.setToolTipText("<html>This is used if there is a <i>system</i> transaction first in the queue<br><br>Do: <code>resume connection to SRV.DB execute transaction</code></html>");

		_dsiSuspendNormal_chk    .setToolTipText("<html>Suspend the DSI thread and <b>wait</b> for the current transaction to end/complete.<br><br>Do: <code>suspend connection to SRV.DB</code></html>");
		_dsiSuspendWithNowait_chk.setToolTipText("<html>Suspend the DSI thread <b>without</b> waiting for the current transaction to end/complete.<br><br>Do: <code>suspend connection to SRV.DB with nowait</code></html>");

		if (_actionType == ActionType.RESUME)
		{
			String htmlInfo = "<html>" +
				"<b>Resume</b> the data flow from Replication Server to the Destination database<br>" +
				"<br>" +
				"<i>Hower</i> over the options below, and the tooltip will describe more information.<br>" +
				"<br>" +
				"</html>";
			_dsiInfo_lbl.setText(htmlInfo);
			// Set this to a small size, otherwise the html MAX width will be used as the window width
			_dsiInfo_lbl.setPreferredSize(new Dimension(10, 10));
		}
		else
		{
			String htmlInfo = "<html>" +
				"<b>Suspend</b> the data flow from Replication Server to the Destination database<br>" +
				"<br>" +
				"<i>Hower</i> over the options below, and the tooltip will describe more information.<br>" +
				"<br>" +
				"</html>";
			_dsiInfo_lbl.setText(htmlInfo);
			// Set this to a small size, otherwise the html MAX width will be used as the window width
			//_dsiInfo_lbl.setPreferredSize(new Dimension(10, 10));
		}

		panel.add(_dsiInfo_lbl,                  "span 2, grow, wrap");

		if (_actionType == ActionType.RESUME)
		{
			panel.add(_dsiResume_chk,                "span 2, wrap");
			panel.add(_dsiResumeSkipFirst_chk,       "span 2, wrap");
			panel.add(_dsiResumeSkipNumTran_chk,     "");
			panel.add(_dsiResumeSkipNumTran_sp,      "wrap");
			panel.add(_dsiResumeExecSysTran_chk,     "span 2, wrap");

			ButtonGroup gt = new ButtonGroup();
			gt.add(_dsiResume_chk);
			gt.add(_dsiResumeSkipFirst_chk);
			gt.add(_dsiResumeSkipNumTran_chk);
			gt.add(_dsiResumeExecSysTran_chk);
			_dsiResume_chk.setSelected(true);
		}
		else // ActionType.SUSPEND
		{
			panel.add(_dsiSuspendNormal_chk,         "span 2, wrap");
			panel.add(_dsiSuspendWithNowait_chk,     "span 2, wrap");

			ButtonGroup gt = new ButtonGroup();
			gt.add(_dsiSuspendNormal_chk);
			gt.add(_dsiSuspendWithNowait_chk);
			_dsiSuspendNormal_chk.setSelected(true);
		}

		
		// disable input to some fields

		// for validation
		_dsiResume_chk           .addActionListener(this);
		_dsiResumeSkipFirst_chk  .addActionListener(this);
		_dsiResumeSkipNumTran_chk.addActionListener(this);
		_dsiResumeExecSysTran_chk.addActionListener(this);
		_dsiSuspendWithNowait_chk.addActionListener(this);

		// Focus action listener

		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e==null ? null : e.getSource();

		// BUT: OK
		if (_ok.equals(source))
		{
			if (_rsThreadType == RsThreadType.DSI) 
			{
				if (_actionType == ActionType.RESUME)
					doResumeConnection();
				else
					doSuspendConnection();
			}
			else if (_rsThreadType == RsThreadType.REP_AGENT)
			{
				if (_actionType == ActionType.RESUME)
					doStartRepAgent();
				else
					doStopRepAgent();
			}

			saveProps();
			setVisible(false);
		}
		
		// BUT: CANCEL
		if (_cancel.equals(source))
		{
			setVisible(false);
		}
		
		// Enable/Disable components
		_dsiResumeSkipNumTran_sp.setEnabled(_dsiResumeSkipNumTran_chk.isSelected());

		validateInput();
	}

	@Override
	public void focusGained(FocusEvent e)
	{
		validateInput();
	}

	@Override
	public void focusLost(FocusEvent e)
	{
		validateInput();
	}
	
	private boolean validateInput()
	{
		String warn = "";

		if (_rsThreadType == RsThreadType.REP_AGENT)
		{
			if (StringUtil.isNullOrBlank(_raUsername_txt.getText()) )
					warn = "RepAgent User Name can't be blank";
		}

		_warning_lbl.setText(warn);

		boolean ok = StringUtil.isNullOrBlank(warn);
		_ok.setEnabled(ok);

		return ok;
	}

	private void saveProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
			return;

		// Save generic or last one used
		conf.setProperty("rs.connection.ra.username",     _raUsername_txt.getText());
		conf.setProperty("rs.connection.ra.password",     _raPassword_txt.getText());
		conf.setProperty("rs.connection.ra.savePassword", _raPassword_chk.isSelected());

		// Save for this specific server.db
		conf.setProperty("rs.connection."+_name+".ra.username",     _raUsername_txt.getText());
		conf.setProperty("rs.connection."+_name+".ra.password",     _raPassword_txt.getText());
		conf.setProperty("rs.connection."+_name+".ra.savePassword", _raPassword_chk.isSelected());

		conf.save();
	}

	private void loadProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
			return;

		// first: LOAD for this specific server.db, then use FALLBACK "last used"
		String str;
		
		// USER
		str = conf.getProperty("rs.connection."+_name+".ra.username");
		if (str == null)
			str = conf.getProperty("rs.connection.ra.username", "sa");
		_raUsername_txt.setText(str);

		// PASSWORD
		str = conf.getProperty("rs.connection."+_name+".ra.password");
		if (str == null)
			str = conf.getProperty("rs.connection.ra.password", "");
		_raPassword_txt.setText(str);

		// SAVE PASSWORD
		str = conf.getProperty("rs.connection."+_name+".ra.savePassword");
		if (str == null)
			str = conf.getProperty("rs.connection.ra.savePassword", "true");
		_raPassword_chk.setSelected(Boolean.parseBoolean(str));
	}
	
	
	
	
	//-------------------------------------------------------------
	// RESUME actions
	//-------------------------------------------------------------

	private void doResumeConnection()
	{
		String cmd = "resume connection to "+_name+" ";

		if (_dsiResumeSkipFirst_chk.isSelected())
			cmd += "skip transaction";

		if (_dsiResumeSkipNumTran_chk.isSelected())
			cmd += "skip " + _dsiResumeSkipNumTran_spm.getNumber() + " transaction";

		if (_dsiResumeExecSysTran_chk.isSelected())
			cmd += "execute transaction";

		final String cmd_final = cmd;

		WaitForExecDialog wait = new WaitForExecDialog(this, _conn, "Resume connection to: "+_name);

		// Kick this of as it's own thread, otherwise the sleep below, might block the Swing Event Dispatcher Thread
		BgExecutor bgExec = new BgExecutor(wait)
		{
			@Override
			public Object doWork()
			{
				getWaitDialog().setState("RCL: "+cmd_final);
				// Do the work
				try
				{
					Connection conn = _conn;

					_logger.info("Executing: "+cmd_final);
					Statement stmnt = conn.createStatement();
					//stmnt.setQueryTimeout( value );
					stmnt.executeUpdate(cmd_final);
					
					String warnings = AseConnectionUtils.getSqlWarningMsgs(stmnt.getWarnings());
					stmnt.close();

					if ( ! StringUtil.isNullOrBlank(warnings) )
					{
						SwingUtils.showInfoMessage("Message", warnings);

//						JCheckBox doAdminWhoIsDown = new JCheckBox("Execute 'admin who_is_down' on Close", true);
//						SwingUtils.showInfoMessageExt(RsResumeSuspendDialog.this, "Message", warnings, doAdminWhoIsDown, null);
//						Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
//						if (conf != null)
//							conf.setProperty("RsResumeSuspendDialog.doAdminWhoIsDown", doAdminWhoIsDown.isSelected());
//						System.out.println("OWNER="+_owner);
					}
				}
				catch (SQLException sqle)
				{
					AseConnectionUtils.showSqlExceptionMessage(RsResumeSuspendDialog.this, "Problems Executing", "Problems when executing SQL Command '"+cmd_final+"'.", sqle); 
				}
				return null;
			}
		};
		wait.execAndWait(bgExec, 300);
	}

	private void doStartRepAgent()
	{
		final String dsname  = _dsname;
		final String dbname  = _dbname;
		final String username = _raUsername_txt.getText();
		final String password = _raPassword_txt.getText();
		final String appname  = "RsResumeRa";

		WaitForExecDialog wait = new WaitForExecDialog(this, _conn, "Start Replication Agent "+_name);

		// Kick this of as it's own thread, otherwise the sleep below, might block the Swing Event Dispatcher Thread
		BgExecutor bgExec = new BgExecutor(wait)
		{
			@Override
			public Object doWork()
			{
				// Do the work
				String cmd = "";
				Connection conn = null;
				try
				{
					// CONNECT TO ASE
					getWaitDialog().setState("Connecting to server "+dsname);
					cmd = "connecting";
					conn = AseConnectionFactory.getConnection(dsname, dbname, username, password, appname);

					// If REPAGENT is running
					// - stop it
					// - if it can't be stopped after 20 seconds.
					//    - stop it with 'nowait' option
					//    - if it can't be stopped after 20 seconds.
					//       - show error message and return.
					getWaitDialog().setState("Checking if the RepAgent is not up and running.");
					cmd = "isRepAgentAlive";
					if (RepServerUtils.isRepAgentAlive(conn, dsname, dbname))
					{
						getWaitDialog().setState("Stopping RepAgent.");
						cmd = "stopRepAgent";
						RepServerUtils.stopRepAgent(conn, dsname, dbname, false);

						// Wait for a while, timeout
						getWaitDialog().setState("Waiting for RepAgent to stop.");
						long startTime = System.currentTimeMillis();
						long timeout = 20 * 1000;
						boolean stopWasDone = false;
						while (true)
						{
							cmd = "stopRepAgent:wait";
							getWaitDialog().setState("Waiting for RepAgent to stop. "+TimeUtils.msToTimeStr("%MM:%SS", System.currentTimeMillis() - startTime));
							if ( ! RepServerUtils.isRepAgentAlive(conn, dsname, dbname) )
							{
								stopWasDone = true;
								break;
							}
							if (System.currentTimeMillis() - startTime > timeout)
								break;
							Thread.sleep(1*1000);
						}
						if ( ! stopWasDone )
						{
							cmd = "stopRepAgent:with-nowait";
							RepServerUtils.stopRepAgent(conn, dsname, dbname, true);

							// Wait for a while, timeout
							getWaitDialog().setState("Waiting for RepAgent to stop, with 'nowait'.");
							startTime = System.currentTimeMillis();
							timeout = 20 * 1000;
							stopWasDone = false;
							while (true)
							{
								cmd = "stopRepAgent:wait:nowait";
								getWaitDialog().setState("Waiting for RepAgent to stop, with 'nowait'. "+TimeUtils.msToTimeStr("%MM:%SS", System.currentTimeMillis() - startTime));
								if ( ! RepServerUtils.isRepAgentAlive(conn, dsname, dbname) )
								{
									stopWasDone = true;
									break;
								}
								if (System.currentTimeMillis() - startTime > timeout)
									break;
								Thread.sleep(1*1000);
							}
						}
						if ( ! stopWasDone )
						{
							SwingUtils.showErrorMessage("Problems Stopping RepAgent", "Could not stop the RepAgent at '"+dsname+"."+dbname+"'.", null);
							return null;
						}
					}

					// START THE REPAGENT
					getWaitDialog().setState("Starting RepAgent.");
					cmd = "startRepAgent";
					RepServerUtils.startRepAgent(conn, dsname, dbname);
					
					// WAIT for it to start
					// - if it's not started after 20 seconds.
					//    - show error message and return.
					getWaitDialog().setState("Waiting for RepAgent to start.");
					long startTime = System.currentTimeMillis();
					long timeout = 20 * 1000;
					boolean startWasDone = false;
					while (true)
					{
						cmd = "startRepAgent:wait";
						getWaitDialog().setState("Waiting for RepAgent to start. "+TimeUtils.msToTimeStr("%MM:%SS", System.currentTimeMillis() - startTime));
						if ( RepServerUtils.isRepAgentAlive(conn, dsname, dbname) )
						{
							startWasDone = true;
							break;
						}
						if (System.currentTimeMillis() - startTime > timeout)
							break;
						Thread.sleep(1*1000);
					}
					if ( ! startWasDone)
					{
						SwingUtils.showErrorMessage("Problems Starting RepAgent", "Could not start the RepAgent at '"+dsname+"."+dbname+"'. Please check the ASE errorlog for more information.", null);
						return null;
					}
				}
				catch (InterruptedException interupt)
				{
					// Cancel was called
					_logger.info("doResumeRepAgent() dsname='"+dsname+"', dbname='"+dbname+"' was interrupted.");
				}
				catch (SQLException sqle)
				{
					AseConnectionUtils.showSqlExceptionMessage(RsResumeSuspendDialog.this, "Problems Executing", "Problems when executing Command '"+cmd+"'.", sqle); 
				}
				catch (ClassNotFoundException e)
				{
					SwingUtils.showErrorMessage("Problems connection", "Could not make a connection to "+_name, e);
				}
				finally
				{
					if (conn != null)
					{
						try { conn.close(); } 
						catch (SQLException ignore) {}
					}
				}
				return null;
			}
		};
		wait.execAndWait(bgExec, 300);
	}
	
	
	
	//-------------------------------------------------------------
	// SUSPEND actions
	//-------------------------------------------------------------

	private void doSuspendConnection()
	{
		String cmd = "suspend connection to "+_name+" ";

		if (_dsiSuspendWithNowait_chk.isSelected())
			cmd += "with nowait";

		final String cmd_final = cmd;

		WaitForExecDialog wait = new WaitForExecDialog(this, _conn, "Suspend connection to: "+_name);

		// Kick this of as it's own thread, otherwise the sleep below, might block the Swing Event Dispatcher Thread
		BgExecutor bgExec = new BgExecutor(wait)
		{
			@Override
			public Object doWork()
			{
				getWaitDialog().setState("RCL: "+cmd_final);
				// Do the work
				try
				{
					Connection conn = _conn;

					_logger.info("Executing: "+cmd_final);
					Statement stmnt = conn.createStatement();
					//stmnt.setQueryTimeout( value );
					stmnt.executeUpdate(cmd_final);
					
					String warnings = AseConnectionUtils.getSqlWarningMsgs(stmnt.getWarnings());
					if ( ! StringUtil.isNullOrBlank(warnings) )
						SwingUtils.showInfoMessage("Message", warnings);

					stmnt.close();
				}
				catch (SQLException sqle)
				{
					AseConnectionUtils.showSqlExceptionMessage(RsResumeSuspendDialog.this, "Problems Executing", "Problems when executing SQL Command '"+cmd_final+"'.", sqle); 
				}
				return null;
			}
		};
		wait.execAndWait(bgExec, 300);
	}

	private void doStopRepAgent()
	{
		final String dsname  = _dsname;
		final String dbname  = _dbname;
		final String username = _raUsername_txt.getText();
		final String password = _raPassword_txt.getText();
		final String appname  = "RsSuspendRa";

		WaitForExecDialog wait = new WaitForExecDialog(this, _conn, "Stop Replication Agent "+_name);

		// Kick this of as it's own thread, otherwise the sleep below, might block the Swing Event Dispatcher Thread
		BgExecutor bgExec = new BgExecutor(wait)
		{
			@Override
			public Object doWork()
			{
				// Do the work
				String cmd = "";
				Connection conn = null;
				try
				{
					// CONNECT TO ASE
					getWaitDialog().setState("Connecting to server "+dsname);
					cmd = "connecting";
					conn = AseConnectionFactory.getConnection(dsname, dbname, username, password, appname);

					// If REPAGENT is running
					// - stop it
					// - if it can't be stopped after 20 seconds.
					//    - stop it with 'nowait' option
					//    - if it can't be stopped after 20 seconds.
					//       - show error message and return.
					getWaitDialog().setState("Checking if the RepAgent is not up and running.");
					cmd = "isRepAgentAlive";
					if (RepServerUtils.isRepAgentAlive(conn, dsname, dbname))
					{
						getWaitDialog().setState("Stopping RepAgent.");
						cmd = "stopRepAgent";
						RepServerUtils.stopRepAgent(conn, dsname, dbname, false);

						// Wait for a while, timeout
						getWaitDialog().setState("Waiting for RepAgent to stop.");
						long startTime = System.currentTimeMillis();
						long timeout = 20 * 1000;
						boolean stopWasDone = false;
						while (true)
						{
							cmd = "stopRepAgent:wait";
							getWaitDialog().setState("Waiting for RepAgent to stop. "+TimeUtils.msToTimeStr("%MM:%SS", System.currentTimeMillis() - startTime));
							if ( ! RepServerUtils.isRepAgentAlive(conn, dsname, dbname) )
							{
								stopWasDone = true;
								break;
							}
							if (System.currentTimeMillis() - startTime > timeout)
								break;
							Thread.sleep(1*1000);
						}
						if ( ! stopWasDone )
						{
							cmd = "stopRepAgent:with-nowait";
							RepServerUtils.stopRepAgent(conn, dsname, dbname, true);

							// Wait for a while, timeout
							getWaitDialog().setState("Waiting for RepAgent to stop, with 'nowait'.");
							startTime = System.currentTimeMillis();
							timeout = 20 * 1000;
							stopWasDone = false;
							while (true)
							{
								cmd = "stopRepAgent:wait:nowait";
								getWaitDialog().setState("Waiting for RepAgent to stop, with 'nowait'. "+TimeUtils.msToTimeStr("%MM:%SS", System.currentTimeMillis() - startTime));
								if ( ! RepServerUtils.isRepAgentAlive(conn, dsname, dbname) )
								{
									stopWasDone = true;
									break;
								}
								if (System.currentTimeMillis() - startTime > timeout)
									break;
								Thread.sleep(1*1000);
							}
						}
						if ( ! stopWasDone )
						{
							SwingUtils.showErrorMessage("Problems Stopping RepAgent", "Could not stop the RepAgent at '"+dsname+"."+dbname+"'.", null);
							return null;
						}
					}
					else // RepAgent was already running...
					{
						SwingUtils.showErrorMessage("Stopping RepAgent", "The RepAgent at '"+dsname+"."+dbname+"' was not running.", null);
						return null;
					}
				}
				catch (InterruptedException interupt)
				{
					// Cancel was called
					_logger.info("doSuspendRepAgent() dsname='"+dsname+"', dbname='"+dbname+"' was interrupted.");
				}
				catch (SQLException sqle)
				{
					AseConnectionUtils.showSqlExceptionMessage(RsResumeSuspendDialog.this, "Problems Executing", "Problems when executing Command '"+cmd+"'.", sqle); 
				}
				catch (ClassNotFoundException e)
				{
					SwingUtils.showErrorMessage("Problems connection", "Could not make a connection to "+_name, e);
				}
				finally
				{
					if (conn != null)
					{
						try { conn.close(); } 
						catch (SQLException ignore) {}
					}
				}
				return null;
			}
		};
		wait.execAndWait(bgExec, 300);
	}
	
}

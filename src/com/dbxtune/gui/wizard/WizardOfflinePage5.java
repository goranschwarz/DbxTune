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
package com.dbxtune.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardPage;
import org.netbeans.spi.wizard.WizardPanelNavResult;

import com.dbxtune.Version;
import com.dbxtune.gui.swing.MultiLineLabel;
import com.dbxtune.utils.StringUtil;

import net.miginfocom.swing.MigLayout;



public class WizardOfflinePage5
extends WizardPage
implements ActionListener
{
    private static final long serialVersionUID = 1L;

	private static final String WIZ_NAME = "SshConnection";
	private static final String WIZ_DESC = "Host Monitor Connection information";
	private static final String WIZ_HELP = "If you selected any Performance Counter Module in the previous page that was of the type 'Host Monitor'\nThen you need to specify SSH (Secure Shell) information, otherwise we can't connect to it when polling for Performance Counters.";

	private JLabel     _noHostMonWasSelected = new JLabel("<html><b>No Host Monitor, Performance Counter was selected.</b> Just press <b>Next</b> to continue.</b></html>");
	private JCheckBox  _cmdLine_chk = new JCheckBox("Use Command Line Switches for the below information", false);

//	private boolean    _firtsTimeRender = true;

	private JTextField _sshHostname = new JTextField();
	private JTextField _sshPort     = new JTextField(); 
	private JTextField _sshUsername = new JTextField();
	private JTextField _sshPassword = new JPasswordField();
	private JTextField _sshKeyFile  = new JTextField();
	private JButton    _sshKeyFile_but  = new JButton("...");

	public static String getDescription() { return WIZ_DESC; }
	@Override
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	public WizardOfflinePage5()
	{
		super(WIZ_NAME, WIZ_DESC);
		
		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		_cmdLine_chk.setToolTipText("<html>" +
				"Command Line Switches '-uuser -ppasswd -ssrvname' will override information in this wizard.<br>" +
				"This so you can generate one template file that is applicable for many servers.<br>" +
				"</html>");

		_sshHostname.setName("sshHostname");
		_sshPort    .setName("sshPort");
		_sshUsername.setName("sshUsername");
		_sshPassword.setName("sshPassword");
		_sshKeyFile .setName("sskKeyFile");

		// tool tip
		_sshHostname.setToolTipText("What host name should we connect to when polling for Performance Counters.");
		_sshPort    .setToolTipText("Port number, the SSH server runs on.");
		_sshUsername.setToolTipText("User name to be used when logging on the the above host name.");
		_sshPassword.setToolTipText("Password, you can override this with the '-p' command line switch when starting "+Version.getAppName()+" in no-gui mode, and yes the stored value is encrypted.");
		_sshKeyFile .setToolTipText("SSH Private Key File, you can override this with the '-k' command line switch when starting "+Version.getAppName()+" in no-gui mode.");
		_noHostMonWasSelected.setToolTipText("");

		_noHostMonWasSelected.setVisible(false);
		
		// Add a helptext
		add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );

		add(_noHostMonWasSelected, "span 2, growx, hidemode 3, wrap 20");
		add(_cmdLine_chk,          "skip, wrap");

		add(new JLabel("SSH Hostname"));
		add(_sshHostname, "growx, wrap");

		add(new JLabel("SSH Port"));
		add(_sshPort,     "growx, wrap");

		add(new JLabel("Username"));
		add(_sshUsername, "growx, wrap");

		add(new JLabel("Password"));
		add(_sshPassword, "growx, wrap");

		add(new JLabel("SSH Key File"));
		add(_sshKeyFile,     "split, growx");
		add(_sshKeyFile_but, "wrap");

		// Command line switches
		String cmdLineSwitched = 
			"<html>" +
			"The above options can be overridden or specified using the following command line switches" +
			"<table>" +
			"<tr><code>-u,--sshUser    &lt;user&gt;      </code><td></td>SSH Username, used by Host Monitoring subsystem</tr>" +
			"<tr><code>-p,--sshPasswd  &lt;passwd&gt;    </code><td></td>SSH Password, used by Host Monitoring subsystem</tr>" +
			"<tr><code>-s,--sshServer  &lt;host&gt;      </code><td></td>SSH Hostname, used by Host Monitoring subsystem</tr>" +
			"<tr><code>-k,--sshKeyFile &lt;filename&gt;  </code><td></td>SSH KeyFile,  used by Host Monitoring subsystem</tr>" +
			"</table>" +
			"</html>";
		add( new JLabel(""), "span, wrap 30" );
		add( new MultiLineLabel(cmdLineSwitched), "span, wrap" );

		_sshKeyFile_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String dir = System.getProperty("user.home") + File.separatorChar + ".ssh";

				JFileChooser fc = new JFileChooser(dir);
				int returnVal = fc.showOpenDialog(WizardOfflinePage5.this);
				if(returnVal == JFileChooser.APPROVE_OPTION) 
				{
					String filename = fc.getSelectedFile().getAbsolutePath();
					_sshKeyFile.setText(filename);
				}
			}
		});

		initData();
	}

	private void initData()
	{
		// Set initial text for jdbc driver && kick of the action...
//		_sshHostname.setText(  );
	}

	/** Called when we enter the page */
	@Override
	protected void renderingPage()
    {
		String val = (String) getWizardData("to-be-discarded.HostMonitorIsSelected");
		boolean hasHostMonSelection = (val != null && val.trim().equalsIgnoreCase("true"));

		if (hasHostMonSelection)
		{
			_noHostMonWasSelected.setVisible(false);
			_sshHostname.setEnabled(true);
			_sshPort    .setEnabled(true);
			_sshUsername.setEnabled(true);
			_sshPassword.setEnabled(true);
			_sshKeyFile .setEnabled(true);

			if (_sshHostname.getText().trim().length() == 0)
			{
				Object oAseHost = getWizardData("aseHost");
				String aseHost = oAseHost == null ? null : oAseHost.toString();

				if (aseHost != null && ! aseHost.equals(""))
				{
					String oshostname = "";
					String[] hosts = StringUtil.commaStrToArray(aseHost);
					for (String host : hosts)
					{
						if (host.equals("localhost")) continue;
						if (host.equals("127.0.0.1")) continue;
						oshostname = host;
						break;
					}
					_sshHostname.setText(oshostname);
				}
				_sshPort.setText("22");
				_sshUsername.setText("sybase");
			}
		}
		else
		{
			_noHostMonWasSelected.setVisible(true);
			_sshHostname.setEnabled(false);
			_sshPort    .setEnabled(false);
			_sshUsername.setEnabled(false);
			_sshPassword.setEnabled(false);
			_sshKeyFile .setEnabled(false);
		}

//	    _firtsTimeRender = false;
    }

	@Override
	protected String validateContents(Component comp, Object event)
	{
		// If the nothing needs to be done, then return here.
		if (_noHostMonWasSelected.isVisible())
			return null;  // NO Need to continue

		if (_cmdLine_chk.isSelected())
		{
			_sshHostname.setEnabled(false);
			_sshPort    .setEnabled(false);
			_sshUsername.setEnabled(false);
			_sshPassword.setEnabled(false);
			_sshKeyFile .setEnabled(false);

			return null; // NO Need to continue
		}
		else
		{
			_sshHostname.setEnabled(true);
			_sshPort    .setEnabled(true);
			_sshUsername.setEnabled(true);
			_sshPassword.setEnabled(true);
			_sshKeyFile .setEnabled(true);
		}
		
		// CHECK required fields
		String problem = "";
		if ( _sshHostname.getText().trim().length() <= 0) problem += "Hostname, ";
		if ( _sshPort    .getText().trim().length() <= 0) problem += "Port, ";
		if ( _sshUsername.getText().trim().length() <= 0) problem += "User, ";
		if ( _sshPassword.getText().trim().length() <= 0 && _sshKeyFile.getText().trim().length() <= 0) problem += "Password AND SSH Key File, ";

		if (problem.length() > 0  &&  problem.endsWith(", "))
		{
			// Discard last ', '
			problem = problem.substring(0, problem.length()-2);
		}
		if ( problem.length() > 0 )
			return "Following fields can't be empty: "+problem;


		// CHECK port number
		try { Integer.parseInt(_sshPort.getText()); }
		catch (NumberFormatException e)
		{
			return "Port must be a NUMBER, current value is '"+_sshPort.getText()+"'.";
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private void saveWizardData()
	{
		if (_cmdLine_chk.isSelected())
		{
			Map<String, Object> wizData = getWizardDataMap();
			wizData.remove(_sshHostname.getName());
			wizData.remove(_sshPort    .getName());
			wizData.remove(_sshUsername.getName());
			wizData.remove(_sshPassword.getName());
			wizData.remove(_sshKeyFile .getName());
		}
		else
		{
			Map<String, Object> wizData = getWizardDataMap();
			wizData.put(_sshHostname.getName(), _sshHostname.getText());
			wizData.put(_sshPort    .getName(), _sshPort    .getText());
			wizData.put(_sshUsername.getName(), _sshUsername.getText());
			wizData.put(_sshPassword.getName(), _sshPassword.getText());
			wizData.put(_sshKeyFile .getName(), _sshKeyFile .getText());
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public WizardPanelNavResult allowBack(String stepName, Map settings, Wizard wizard)
    {
		saveWizardData();
		return WizardPanelNavResult.PROCEED;
    }

	@SuppressWarnings("rawtypes")
	@Override
	public WizardPanelNavResult allowNext(String stepName, Map settings, Wizard wizard)
	{
		saveWizardData();
		return WizardPanelNavResult.PROCEED;
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
	}
}

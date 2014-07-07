/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.netbeans.spi.wizard.WizardPage;

import com.asetune.Version;
import com.asetune.gui.swing.MultiLineLabel;
import com.asetune.utils.StringUtil;



public class WizardOfflinePage5
extends WizardPage
implements ActionListener
{
    private static final long serialVersionUID = 1L;
//	private static Logger _logger          = Logger.getLogger(WizardOfflinePage4.class);

	private static final String WIZ_NAME = "SshConnection";
	private static final String WIZ_DESC = "Host Monitor Connection information";
	private static final String WIZ_HELP = "If you selected any Performance Counter Module in the previous page that was of the type 'Host Monitor'\nThen you need to specify SSH (Secure Shell) information, otherwise we can't connect to it when polling for Performance Counters.";

	private JLabel     _noHostMonWasSelected = new JLabel("<html><b>No Host Monitor, Performance Counter was selected.</b> Just press <b>Next</b> to continue.</b></html>");

//	private boolean    _firtsTimeRender = true;

	private JTextField _sshHostname = new JTextField();
	private JTextField _sshPort     = new JTextField(); 
	private JTextField _sshUsername = new JTextField();
	private JTextField _sshPassword = new JPasswordField();

	public static String getDescription() { return WIZ_DESC; }
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	public WizardOfflinePage5()
	{
		super(WIZ_NAME, WIZ_DESC);
		
		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		_sshHostname.setName("sshHostname");
		_sshPort    .setName("sshPort");
		_sshUsername.setName("sshUsername");
		_sshPassword.setName("sshPassword");

		// tool tip
		_sshHostname.setToolTipText("What host name should we connect to when polling for Performance Counters.");
		_sshPort    .setToolTipText("Port number, the SSH server runs on.");
		_sshUsername.setToolTipText("User name to be used when logging on the the above host name.");
		_sshPassword.setToolTipText("Password, you can override this with the '-p' command line switch when starting "+Version.getAppName()+" in no-gui mode, and yes the stored value is encrypted.");
		_noHostMonWasSelected.setToolTipText("");

		_noHostMonWasSelected.setVisible(false);
		
		// Add a helptext
		add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );

		add(_noHostMonWasSelected, "span 2, growx, hidemode 3, wrap 20");

		add(new JLabel("SSH Hostname"));
		add(_sshHostname, "growx, wrap");

		add(new JLabel("SSH Port"));
		add(_sshPort,     "growx, wrap");

		add(new JLabel("Username"));
		add(_sshUsername, "growx, wrap");

		add(new JLabel("Password"));
		add(_sshPassword, "growx, wrap");

		// Command line switches
		String cmdLineSwitched = 
			"<html>" +
			"The above options can be overridden or specified using the following command line switches" +
			"<table>" +
			"<tr><code>-u,--sshUser &lt;user&gt;    </code><td></td>SSH Username, used by Host Monitoring subsystem</tr>" +
			"<tr><code>-p,--sshPasswd &lt;passwd&gt;</code><td></td>SSH Password, used by Host Monitoring subsystem</tr>" +
			"<tr><code>-s,--sshServer &lt;host&gt;  </code><td></td>SSH Hostname, used by Host Monitoring subsystem</tr>" +
			"</table>" +
			"</html>";
		add( new JLabel(""), "span, wrap 30" );
		add( new MultiLineLabel(cmdLineSwitched), "span, wrap" );

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

			if (_sshHostname.getText().trim().length() == 0)
			{
				String aseHost = getWizardData("aseHost").toString();
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
		}

//	    _firtsTimeRender = false;
    }

	protected String validateContents(Component comp, Object event)
	{
		// If the nothing needs to be done, then return here.
		if (_noHostMonWasSelected.isVisible())
			return null;

		// CHECK required fields
		String problem = "";
		if ( _sshHostname.getText().trim().length() <= 0) problem += "Hostname, ";
		if ( _sshPort    .getText().trim().length() <= 0) problem += "Port, ";
		if ( _sshUsername.getText().trim().length() <= 0) problem += "User, ";
		if ( _sshPassword.getText().trim().length() <= 0) problem += "Password, ";

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

	public void actionPerformed(ActionEvent ae)
	{
	}
}

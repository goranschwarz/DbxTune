/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardPage;
import org.netbeans.spi.wizard.WizardPanelNavResult;

import com.asetune.AseTune;
import com.asetune.Version;
import com.asetune.gui.swing.MultiLineLabel;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.StringUtil;
import com.sybase.util.ds.interfaces.SyInterfacesDriver;

public class WizardOfflinePage1
extends WizardPage
implements ActionListener
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger          = Logger.getLogger(WizardOfflinePage1.class);

	private static final String WIZ_NAME = "ase-info";
	private static final String WIZ_DESC = "ASE information";
	private static final String WIZ_HELP = "Connection information about the ASE server to sample statistics from.";
	
	private JCheckBox  _cmdLine_chk = new JCheckBox("Use Command Line Switches for the below information", false);
	private JComboBox  _aseName	    = new JComboBox();
//	private JTextField _aseName     = new JTextField("");
	private JTextField _aseHost     = new JTextField("");
	private JTextField _asePort     = new JTextField("");
	private JTextField _aseUsername = new JTextField("");
	private JTextField _asePassword = new JPasswordField();

	private SyInterfacesDriver _interfacesDriver = null;

	public static String getDescription() { return WIZ_DESC; }
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	public WizardOfflinePage1()
	{
		super(WIZ_NAME, WIZ_DESC);

		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		_aseName    .setName("aseName");
		_aseHost    .setName("aseHost");
		_asePort    .setName("asePort");
		_aseUsername.setName("aseUsername");
		_asePassword.setName("asePassword");

		_cmdLine_chk.setToolTipText("<html>" +
				"Command Line Switches '-Uuser -Ppasswd -Ssrvname' will override information in this wizard.<br>" +
				"This so you can generate one template file that is applicable for many servers.<br>" +
				"</html>");

		// Add a helptext
		add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );

		add(_cmdLine_chk, "skip, wrap");

		add(new JLabel("ASE Name"));
		add(_aseName, "growx, wrap");
		_aseName.putClientProperty("NAME", "ASE_NAME");
		_aseName.addActionListener(this);

		add(new JLabel("Host Name"));
		add(_aseHost, "growx, wrap");

		add(new JLabel("Port Number"));
		add(_asePort, "growx, wrap");

		add(new JLabel("Username"));
		add(_aseUsername, "growx, wrap");

		add(new JLabel("Password"));
		add(_asePassword, "growx, wrap");

		JButton button = new JButton("Test Connection");
		button.addActionListener(this);
		button.putClientProperty("NAME", "BUTTON_CONN_TEST_ASE");
		add(button, "span, align right");

		// Command line switches
		String cmdLineSwitched = 
			"<html>" +
			"The above options can be overridden or specified using the following command line switches" +
			"<table>" +
			"<tr><code>-U,--user &lt;user&gt;    </code><td></td>Username when connecting to server.</tr>" +
			"<tr><code>-P,--passwd &lt;passwd&gt;</code><td></td>Password when connecting to server. null=noPasswd</tr>" +
			"<tr><code>-S,--server &lt;server&gt;</code><td></td>Server to connect to</tr>" +
			"</table>" +
			"</html>";
		add( new JLabel(""), "span, wrap 30" );
		add( new MultiLineLabel(cmdLineSwitched), "span, wrap" );

		initData();
	}

	private void initData()
	{
		try 
		{
			String interfacesFile = System.getProperty("interfaces.file");
			if (interfacesFile != null)
			{
				_interfacesDriver = new SyInterfacesDriver(interfacesFile);
				_interfacesDriver.open(interfacesFile);
			}
			else
			{
				_interfacesDriver = new SyInterfacesDriver();
				_interfacesDriver.open();
			}
		}
		catch(Exception ex)
		{
			_logger.error("Problems reading interfaces or sql.ini file.", ex);
		}

		if (_interfacesDriver != null)
		{
			_logger.debug("Just opened the interfaces file '"+ _interfacesDriver.getBundle() +"'.");
			
			String[] servers = _interfacesDriver.getServers();
			if (servers != null)
			{
				Arrays.sort(servers);
				for (int i=0; i<servers.length; i++)
				{
					_logger.debug("Adding server '"+ servers[i] +"' to serverListCB.");
					_aseName.addItem(servers[i]);
				}
			}
		}
		
		if (AseTune.getCounterCollector().isMonConnected())
		{
			String servername = "";
			try 
			{
				servername = AseConnectionFactory.getServer();
				_aseName.setSelectedItem( servername ); 
			} 
			catch(RuntimeException e)
			{
				_logger.warn("Problems getting info about server '"+servername+"' from the interfaces or sql.ini file.");
			}
//			_aseName.setText("");
//			_aseHost.setText("");
//			_asePort.setText("");
			_aseUsername.setText( AseConnectionFactory.getUser() );
			_asePassword.setText( AseConnectionFactory.getPassword() );
		}
	}

	@Override
	protected String validateContents(Component comp, Object event)
	{
		if (_cmdLine_chk.isSelected())
		{
			_aseName    .setEnabled(false);
			_aseHost    .setEnabled(false);
			_asePort    .setEnabled(false);
			_aseUsername.setEnabled(false);
			_asePassword.setEnabled(false);
			return null;
		}
		else
		{
			_aseName    .setEnabled(true);
			_aseHost    .setEnabled(true);
			_asePort    .setEnabled(true);
			_aseUsername.setEnabled(true);
			_asePassword.setEnabled(true);
		}

//		String name = null;
//		if (comp != null)
//			name = comp.getName();

		//System.out.println("validateContents: name='"+name+"',\n\ttoString='"+comp+"'\n\tcomp='"+comp+"',\n\tevent='"+event+"'.");

		String problem = "";
//		if ( _aseName    .getText().trim().length() <= 0) problem += "ASE Name, ";
		if ( _aseHost    .getText().trim().length() <= 0) problem += "Host Name, ";
		if ( _asePort    .getText().trim().length() <= 0) problem += "Port Number, ";
		if ( _aseUsername.getText().trim().length() <= 0) problem += "Username, ";

		// Check if PORT_NUMBER is an integer
		if (_asePort.getText().trim().length() > 0)
		{
			String portStr = _asePort.getText();
			String[] sa = StringUtil.commaStrToArray(portStr);
			for (int i=0; i<sa.length; i++)
			{
				try { Integer.parseInt(sa[i]); } 
				catch (NumberFormatException e) 
				{
					return "Port Number '"+sa[i]+"' needs to be a number.";
				}
			}
			
//			try { Integer.parseInt( _asePort.getText().trim() ); }
//			catch (NumberFormatException e)
//			{
//				problem = "Port Number needs to be a number, ";
//			}
		}
		
		if (problem.length() > 0)
		{
			// Discard last ', '
			problem = problem.substring(0, problem.length()-2);
		}
		
		return problem.length() == 0 ? null : "Following fields can't be empty: "+problem;
	}

	public void actionPerformed(ActionEvent ae)
	{
		JComponent src = (JComponent) ae.getSource();
		String name = (String)src.getClientProperty("NAME");
		if (name == null)
			name = "-null-";

//		System.out.println("Source("+name+"): " + src);

		if (name.equals("ASE_NAME"))
		{
			String srv = (String) _aseName.getSelectedItem();
			_aseHost.setText(AseConnectionFactory.getIHosts(srv));
			_asePort.setText(AseConnectionFactory.getIPorts(srv));
			
//			SyInterfacesEntry interfaceEntry = null;
//			if ( srv != null &&  ! srv.trim().equals("") )
//			{
//				if ( _interfacesDriver != null )
//				{
//					interfaceEntry = _interfacesDriver.getEntry(srv);
//				}
//			}
//	
//			if (interfaceEntry != null)
//			{
//				_aseHost.setText( interfaceEntry.getHost() );
//				_asePort.setText( interfaceEntry.getPort() );
//			}
//			else
//			{
//			}
		}

		if (name.equals("BUTTON_CONN_TEST_ASE"))
		{
			testAseConnection("testConnect", 
				_aseUsername.getText(), 
				_asePassword.getText(), 
				_aseHost.getText(), 
				_asePort.getText());
		}
	}
	
	private boolean testAseConnection(String appname, String user, String passwd, String host, String port)
	{
//		String driverClassName = System.getProperty("jdbc_driver_class_name", "com.sybase.jdbc3.jdbc.SybDriver");
//		String startOfConnUrl  = System.getProperty("jdbc_start_of_conn_url", "jdbc:sybase:Tds:");


		try
		{
//			Class.forName(driverClassName).newInstance();
//			Properties props = new Properties();
//			props.put("user", user);
//			props.put("password", passwd);
////			props.put("JCONNECT_VERSION", "6");
////			props.put("USE_METADATA", "FALSE");
////			props.put("PACKETSIZE", "512");
//			props.put("APPLICATIONNAME", appname);
////			props.put("CHARSET", "iso_1");
//	
//			_logger.debug("Try getConnection to " + host + ":" + port + " user=" + user);
//			Connection conn = DriverManager.getConnection(startOfConnUrl + host + ":" + port, props);

			Connection conn = AseConnectionFactory.getConnection(host, port, null, user, passwd, appname, "", null);
		
			// select @@version
			String aseVersionStr = "unknown";
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select @@version");
			while ( rs.next() )
			{
				aseVersionStr = rs.getString(1);
			}
			rs.close();
			conn.close();

			JOptionPane.showMessageDialog(this, "Connection succeeded.\n\n"+aseVersionStr, Version.getAppName()+" - connect check", JOptionPane.INFORMATION_MESSAGE);
			return true;
		}
		catch (SQLException e)
		{
			StringBuffer sb = new StringBuffer();
			while (e != null)
			{
				sb.append( "\n" );
				sb.append( e.getMessage() );
				e = e.getNextException();
			}
			JOptionPane.showMessageDialog(this, "Connection FAILED.\n\n"+sb.toString(), Version.getAppName()+" - connect check", JOptionPane.ERROR_MESSAGE);
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, "Connection FAILED.\n\n"+e.toString(),  Version.getAppName()+" - connect check", JOptionPane.ERROR_MESSAGE);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private void saveWizardData()
	{
		if (_cmdLine_chk.isSelected())
		{
			Map<String, Object> wizData = getWizardDataMap();
			wizData.remove(_aseName    .getName());
			wizData.remove(_aseHost    .getName());
			wizData.remove(_asePort    .getName());
			wizData.remove(_aseUsername.getName());
			wizData.remove(_asePassword.getName());
		}
		else
		{
			Map<String, Object> wizData = getWizardDataMap();
			wizData.put(_aseName    .getName(), _aseName    .getSelectedItem());
			wizData.put(_aseHost    .getName(), _aseHost    .getText());
			wizData.put(_asePort    .getName(), _asePort    .getText());
			wizData.put(_aseUsername.getName(), _aseUsername.getText());
			wizData.put(_asePassword.getName(), _asePassword.getText());
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
}

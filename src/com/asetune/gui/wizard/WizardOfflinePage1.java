/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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

import org.apache.log4j.Logger;
import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardPage;
import org.netbeans.spi.wizard.WizardPanelNavResult;

import com.asetune.CounterController;
import com.asetune.Version;
import com.asetune.gui.swing.MultiLineLabel;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.StringUtil;
import com.sybase.util.ds.interfaces.SyInterfacesDriver;

import net.miginfocom.swing.MigLayout;

public class WizardOfflinePage1
extends WizardPage
implements ActionListener
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger          = Logger.getLogger(WizardOfflinePage1.class);

	private static final String WIZ_NAME = "dbms-info";
	private static final String WIZ_DESC = "DBMS information";
	private static final String WIZ_HELP = "Connection information about the DBMS server to sample statistics from.";
	
	private JCheckBox         _cmdLine_chk = new JCheckBox("Use Command Line Switches for the below information", false);
	private JComboBox<String> _dbmsName	    = new JComboBox<String>();
//	private JTextField        _dbmsName     = new JTextField("");
	private JTextField        _dbmsHost     = new JTextField("");
	private JTextField        _dbmsPort     = new JTextField("");
	private JTextField        _dbmsUsername = new JTextField("");
	private JTextField        _dbmsPassword = new JPasswordField();

	private SyInterfacesDriver _interfacesDriver = null;

	boolean _isInterfacesAware = false;

	public static String getDescription() { return WIZ_DESC; }
	@Override public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	public WizardOfflinePage1()
	{
		super(WIZ_NAME, WIZ_DESC);

		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		if (Version.getAppName().toLowerCase().equals("asetune")) _isInterfacesAware = true;
		if (Version.getAppName().toLowerCase().equals("iqtune"))  _isInterfacesAware = true;
		if (Version.getAppName().toLowerCase().equals("rstune"))  _isInterfacesAware = true;
		if (Version.getAppName().toLowerCase().equals("raxtune")) _isInterfacesAware = true;

		
		if (_isInterfacesAware)
			_dbmsName.setName("dbmsName");
		_dbmsHost    .setName("dbmsHost");
		_dbmsPort    .setName("dbmsPort");
		_dbmsUsername.setName("dbmsUsername");
		_dbmsPassword.setName("dbmsPassword");

		_cmdLine_chk.setToolTipText("<html>" +
				"Command Line Switches '-Uuser -Ppasswd -Ssrvname' will override information in this wizard.<br>" +
				"This so you can generate one template file that is applicable for many servers.<br>" +
				"</html>");

		// Add a helptext
		add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );

		add(_cmdLine_chk, "skip, wrap");

		if (_isInterfacesAware)
		{
			String label = "DBMS";
			if (Version.getAppName().toLowerCase().equals("asetune")) label = "ASE";
			if (Version.getAppName().toLowerCase().equals("iqtune"))  label = "IQ";
			if (Version.getAppName().toLowerCase().equals("rstune"))  label = "RS";
			if (Version.getAppName().toLowerCase().equals("raxtune")) label = "RAX";
			
			add(new JLabel(label + " Name"));
			add(_dbmsName, "growx, wrap");
			_dbmsName.putClientProperty("NAME", "ASE_NAME");
			_dbmsName.addActionListener(this);
		}

		add(new JLabel("Host Name"));
		add(_dbmsHost, "growx, wrap");

		add(new JLabel("Port Number"));
		add(_dbmsPort, "growx, wrap");

		add(new JLabel("Username"));
		add(_dbmsUsername, "growx, wrap");

		add(new JLabel("Password"));
		add(_dbmsPassword, "growx, wrap");

		JButton button = new JButton("Test Connection");
		button.addActionListener(this);
		button.putClientProperty("NAME", "BUTTON_CONN_TEST_ASE");
		add(button, "span, align right");
		
		if (_isInterfacesAware)
			button.setEnabled(true);
		else
			button.setEnabled(false);

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
		if (_isInterfacesAware)
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
						_dbmsName.addItem(servers[i]);
					}
				}
			}
			
//			if (AseTune.getCounterCollector().isMonConnected())
			if (CounterController.getInstance().isMonConnected())
			{
				String servername = "";
				try 
				{
					servername = AseConnectionFactory.getServer();
					_dbmsName.setSelectedItem( servername ); 
				} 
				catch(RuntimeException e)
				{
					_logger.warn("Problems getting info about server '"+servername+"' from the interfaces or sql.ini file.");
				}
//				_dbmsName.setText("");
//				_dbmsHost.setText("");
//				_dbmsPort.setText("");
				_dbmsUsername.setText( AseConnectionFactory.getUser() );
				_dbmsPassword.setText( AseConnectionFactory.getPassword() );
			}
		}
	}

	@Override
	protected String validateContents(Component comp, Object event)
	{
		if (_cmdLine_chk.isSelected())
		{
			_dbmsName    .setEnabled(false);
			_dbmsHost    .setEnabled(false);
			_dbmsPort    .setEnabled(false);
			_dbmsUsername.setEnabled(false);
			_dbmsPassword.setEnabled(false);
			return null;
		}
		else
		{
			_dbmsName    .setEnabled(true);
			_dbmsHost    .setEnabled(true);
			_dbmsPort    .setEnabled(true);
			_dbmsUsername.setEnabled(true);
			_dbmsPassword.setEnabled(true);
		}

//		String name = null;
//		if (comp != null)
//			name = comp.getName();

		//System.out.println("validateContents: name='"+name+"',\n\ttoString='"+comp+"'\n\tcomp='"+comp+"',\n\tevent='"+event+"'.");

		String problem = "";
//		if ( _dbmsName    .getText().trim().length() <= 0) problem += "ASE Name, ";
		if ( _dbmsHost    .getText().trim().length() <= 0) problem += "Host Name, ";
		if ( _dbmsPort    .getText().trim().length() <= 0) problem += "Port Number, ";
		if ( _dbmsUsername.getText().trim().length() <= 0) problem += "Username, ";

		// Check if PORT_NUMBER is an integer
		if (_dbmsPort.getText().trim().length() > 0)
		{
			String portStr = _dbmsPort.getText();
			String[] sa = StringUtil.commaStrToArray(portStr);
			for (int i=0; i<sa.length; i++)
			{
				try { Integer.parseInt(sa[i]); } 
				catch (NumberFormatException e) 
				{
					return "Port Number '"+sa[i]+"' needs to be a number.";
				}
			}
			
//			try { Integer.parseInt( _dbmsPort.getText().trim() ); }
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

	@Override
	public void actionPerformed(ActionEvent ae)
	{
		JComponent src = (JComponent) ae.getSource();
		String name = (String)src.getClientProperty("NAME");
		if (name == null)
			name = "-null-";

//		System.out.println("Source("+name+"): " + src);

		if (name.equals("ASE_NAME"))
		{
			String srv = (String) _dbmsName.getSelectedItem();
			_dbmsHost.setText(AseConnectionFactory.getIHosts(srv));
			_dbmsPort.setText(AseConnectionFactory.getIPorts(srv));
			
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
//				_dbmsHost.setText( interfaceEntry.getHost() );
//				_dbmsPort.setText( interfaceEntry.getPort() );
//			}
//			else
//			{
//			}
		}

		if (name.equals("BUTTON_CONN_TEST_ASE"))
		{
			testAseConnection("testConnect", 
				_dbmsUsername.getText(), 
				_dbmsPassword.getText(), 
				_dbmsHost.getText(), 
				_dbmsPort.getText());
		}
	}
	
	private boolean testAseConnection(String appname, String user, String passwd, String host, String port)
	{
//		String driverClassName = System.getProperty("jdbc_driver_class_name", "com.sybase.jdbc42.jdbc.SybDriver");
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

			Connection conn = AseConnectionFactory.getConnection(host, port, null, user, passwd, appname, null, "", null);
		
			// select @@version
			String srvVersionStr = "unknown";
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select @@version");
			while ( rs.next() )
			{
				srvVersionStr = rs.getString(1);
			}
			rs.close();
			conn.close();

			JOptionPane.showMessageDialog(this, "Connection succeeded.\n\n"+srvVersionStr, Version.getAppName()+" - connect check", JOptionPane.INFORMATION_MESSAGE);
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
			wizData.remove(_dbmsName    .getName());
			wizData.remove(_dbmsHost    .getName());
			wizData.remove(_dbmsPort    .getName());
			wizData.remove(_dbmsUsername.getName());
			wizData.remove(_dbmsPassword.getName());
		}
		else
		{
			Map<String, Object> wizData = getWizardDataMap();
			if (_isInterfacesAware)
				wizData.put(_dbmsName    .getName(), _dbmsName    .getSelectedItem());
			wizData.put(_dbmsHost    .getName(), _dbmsHost    .getText());
			wizData.put(_dbmsPort    .getName(), _dbmsPort    .getText());
			wizData.put(_dbmsUsername.getName(), _dbmsUsername.getText());
			wizData.put(_dbmsPassword.getName(), _dbmsPassword.getText());
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

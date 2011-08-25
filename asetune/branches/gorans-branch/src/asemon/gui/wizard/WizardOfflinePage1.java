/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.netbeans.spi.wizard.WizardPage;

import asemon.Asemon;
import asemon.gui.swing.MultiLineLabel;
import asemon.utils.AseConnectionFactory;
import asemon.utils.StringUtil;

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

		// Add a helptext
		add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );

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
		
		initData();
	}

	private void initData()
	{
		try 
		{
			_interfacesDriver = new SyInterfacesDriver();
			_interfacesDriver.open();
		}
		catch(Exception ex)
		{
			_logger.error("Problems reading interfaces or sql.ini file.", ex);
		}

		if (_interfacesDriver != null)
		{
			_logger.debug("Just opened the interfaces file '"+ _interfacesDriver.getBundle() +"'.");
			
			String[] servers = _interfacesDriver.getServers();
			Arrays.sort(servers);
			for (int i=0; i<servers.length; i++)
			{
				_logger.debug("Adding server '"+ servers[i] +"' to serverListCB.");
				_aseName.addItem(servers[i]);
			}
		}
		
		if (Asemon.getCounterCollector().isMonConnected())
		{
			_aseName.setSelectedItem( AseConnectionFactory.getServer() );
//			_aseName.setText("");
//			_aseHost.setText("");
//			_asePort.setText("");
			_aseUsername.setText( AseConnectionFactory.getUser() );
			_asePassword.setText( AseConnectionFactory.getPassword() );
		}
	}

	protected String validateContents(Component comp, Object event)
	{
		String name = null;
		if (comp != null)
			name = comp.getName();

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
		
		return problem.length() == 0 ? null : "Following fields cant be empty: "+problem;
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

			Connection conn = AseConnectionFactory.getConnection(host, port, null, user, passwd, appname, null);
		
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

			JOptionPane.showMessageDialog(this, "Connection succeeded.\n\n"+aseVersionStr, "asemon - connect check", JOptionPane.INFORMATION_MESSAGE);
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
			JOptionPane.showMessageDialog(this, "Connection FAILED.\n\n"+sb.toString(), "asemon - connect check", JOptionPane.ERROR_MESSAGE);
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, "Connection FAILED.\n\n"+e.toString(),  "asemon - connect check", JOptionPane.ERROR_MESSAGE);
		}
		return false;
	}
}

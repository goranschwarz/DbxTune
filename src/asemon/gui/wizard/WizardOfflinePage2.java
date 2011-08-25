/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.netbeans.spi.wizard.WizardPage;

import asemon.gui.swing.MultiLineLabel;


public class WizardOfflinePage2
extends WizardPage
implements ActionListener
{
    private static final long serialVersionUID = 1L;
	private static Logger _logger          = Logger.getLogger(WizardOfflinePage2.class);

	private static final String WIZ_NAME = "connection";
	private static final String WIZ_DESC = "JDBC Connection information";
	private static final String WIZ_HELP = "This is the JDBC Connectivity information to a datastore where the sampled data will be stored.\nIf desired tables are not created in the destination database, they will be created be the offline sampler.";

	private JTextField _jdbcDriver   = new JTextField("org.h2.Driver");
//	private JTextField _jdbcUrl      = new JTextField("jdbc:h2:pcdb_yyy");
	private JTextField _jdbcUrl      = new JTextField("jdbc:h2:file:[<path>]<databaseName>"); 
	private JTextField _jdbcUsername = new JTextField("sa");
	private JTextField _jdbcPassword = new JPasswordField();

	public static String getDescription() { return WIZ_DESC; }
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	public WizardOfflinePage2()
	{
		super(WIZ_NAME, WIZ_DESC);
		
		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		_jdbcDriver  .setName("jdbcDriver");
		_jdbcUrl     .setName("jdbcUrl");
		_jdbcUsername.setName("jdbcUsername");
		_jdbcPassword.setName("jdbcPassword");

		// Add a helptext
		add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );

		add(new JLabel("JDBC Driver"));
		add(_jdbcDriver, "growx, wrap");

		add(new JLabel("JDBC Url"));
		add(_jdbcUrl, "growx, wrap");

		add(new JLabel("Username"));
		add(_jdbcUsername, "growx, wrap");

		add(new JLabel("Password"));
		add(_jdbcPassword, "growx, wrap");

		JButton button = new JButton("Test Connection");
		button.addActionListener(this);
		button.putClientProperty("NAME", "BUTTON_CONN_TEST_JDBC");
		add(button, "span, align right");
		initData();
	}

	private void initData()
	{
	}

	protected String validateContents(Component comp, Object event)
	{
		String name = null;
		if (comp != null)
			name = comp.getName();

		//System.out.println("validateContents: name='"+name+"',\n\ttoString='"+comp+"'\n\tcomp='"+comp+"',\n\tevent='"+event+"'.");

		String problem = "";
		if ( _jdbcDriver  .getText().trim().length() <= 0) problem += "Driver, ";
		if ( _jdbcUrl     .getText().trim().length() <= 0) problem += "Url, ";
		if ( _jdbcUsername.getText().trim().length() <= 0) problem += "User, ";

		if (problem.length() > 0  &&  problem.endsWith(", "))
		{
			// Discard last ', '
			problem = problem.substring(0, problem.length()-2);
		}
		if ( problem.length() > 0 )
			return "Following fields cant be empty: "+problem;

		if ( _jdbcDriver.getText().trim().equals("org.h2.Driver") )
		{
			if ( _jdbcUrl.getText().indexOf("<databaseName>") > 0)
				problem = "Please replace the <databaseName> with a real database name string.";

			if ( _jdbcUrl.getText().indexOf("[<path>]") > 0)
				problem = "Please replace the [<path>] with a real pathname or delete it.";
		}
		if ( problem.length() > 0 )
			return problem;

		return null;
	}

	public void actionPerformed(ActionEvent ae)
	{
		JComponent src = (JComponent) ae.getSource();
		String name = (String)src.getClientProperty("NAME");
		if (name == null)
			name = "-null-";

		System.out.println("Source("+name+"): " + src);

		if (name.equals("BUTTON_CONN_TEST_JDBC"))
		{
			testJdbcConnection("testConnect", 
				_jdbcDriver.getText(), 
				_jdbcUrl.getText(),
				_jdbcUsername.getText(), 
				_jdbcPassword.getText());
		}
	}

	private boolean testJdbcConnection(String appname, String driver, String url, String user, String passwd)
	{
		try
		{
			Class.forName(driver).newInstance();
			Properties props = new Properties();
			props.put("user", user);
			props.put("password", passwd);
	
			_logger.debug("Try getConnection to driver='"+driver+"', url='"+url+"', user='"+user+"'.");
			Connection conn = DriverManager.getConnection(url, props);
			conn.close();
	
			JOptionPane.showMessageDialog(this, "Connection succeeded.", "asemon - connect check", JOptionPane.INFORMATION_MESSAGE);
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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardPage;
import org.netbeans.spi.wizard.WizardPanelNavResult;

import com.asetune.Version;
import com.asetune.gui.ConnectionDialog;
import com.asetune.gui.swing.MultiLineLabel;



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
	private JTextField _jdbcUrl      = new JTextField("jdbc:h2:file:[<path>]<dbname>"); 
	private JButton    _jdbcUrl_but  = new JButton("...");
	private JTextField _jdbcUsername = new JTextField("sa");
	private JTextField _jdbcPassword = new JPasswordField();

	// Specific options if we are using H2 as PCS
	private JCheckBox  _pcsH2Option_startH2NetworkServer_chk = new JCheckBox("Start H2 Database as a Network Server", false);

	//-----------------------------
	// FIXME: add "..." button to grab a file name for the [<path>]<databaseName>, use ConnectionDialog for example
	//-----------------------------

	public static String getDescription() { return WIZ_DESC; }
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	public WizardOfflinePage2()
	{
		super(WIZ_NAME, WIZ_DESC);
		
		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		_jdbcDriver  .setName("jdbcDriver");
		_jdbcUrl     .setName("jdbcUrl");
		_jdbcUsername.setName("jdbcUser");
		_jdbcPassword.setName("jdbcPasswd");
//		_pcsH2Option_startH2NetworkServer_chk.setName("startH2NetworkServer");

		// tool tip
		_jdbcUrl_but .setToolTipText("Open a File chooser dialog to get a filename, for some templates values are replaced");
		_pcsH2Option_startH2NetworkServer_chk.setToolTipText("Start the H2 database engine in 'server' mode, so we can connect to the server while the PCS is storing information...");

		// Add a helptext
		add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );

		add(new JLabel("JDBC Driver"));
		add(_jdbcDriver, "growx, wrap");

		add(new JLabel("JDBC Url"));
		add(_jdbcUrl,     "growx, split");
		add(_jdbcUrl_but, "wrap");

		add(new JLabel("Username"));
		add(_jdbcUsername, "growx, wrap");

		add(new JLabel("Password"));
		add(_jdbcPassword, "growx, wrap");

		add(_pcsH2Option_startH2NetworkServer_chk, "skip, hidemode 3, wrap");

		JButton button = new JButton("Test Connection");
		button.addActionListener(this);
		button.putClientProperty("NAME", "BUTTON_CONN_TEST_JDBC");
		add(button, "span, align right, wrap 20");

		// Add comment at the bottom
		String remember = "<html>" +
			"Remember that you can use variables " +
			"<code>${DATE}</code>, <code>${SERVERNAME}</code>, <code>${ASEHOSTNAME}<code>, <code>${ASETUNE_HOME}<code> and <code>${ASETUNE_SAVE_DIR}<code> " +
			"in the 'JDBC Url' specification above." +
			"</html>";

		MultiLineLabel mll = new MultiLineLabel(remember);
		mll.setToolTipText(ConnectionDialog.JDBC_URL_TOOLTIP);
		mll.setEnabled(true);
		add(mll, "span, push, bottom, wrap" );
		
		// Also add example that you can COPY from
		JTextField urlExample_txt = new JTextField("jdbc:h2:file:${ASETUNE_SAVE_DIR}/${ASEHOSTNAME}_${DATE}");
		urlExample_txt.setEditable(false);
		urlExample_txt.setToolTipText(ConnectionDialog.JDBC_URL_TOOLTIP);
		add(new JLabel("Example:"), "span, split" );
		add(urlExample_txt, "pushx, growx, wrap" );

		_jdbcUrl_but.addActionListener(this);
		_jdbcDriver.addActionListener(this);

		initData();
	}

	private void initData()
	{
		// Set initial text for jdbc driver && kick of the action...
		_jdbcDriver.setText( _jdbcDriver.getText() );
	}

	protected String validateContents(Component comp, Object event)
	{
//		String name = null;
//		if (comp != null)
//			name = comp.getName();

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
			if ( _jdbcUrl.getText().indexOf("<>") > 0)
				problem = "Please replace the <dbname> with a real database name string.";

			if ( _jdbcUrl.getText().indexOf("[<path>]") > 0)
				problem = "Please replace the [<path>] with a real pathname or delete it.";
		}
		if ( problem.length() > 0 )
			return problem;

		return null;
	}

	public void actionPerformed(ActionEvent ae)
	{
		JComponent source = (JComponent) ae.getSource();
		String name = (String)source.getClientProperty("NAME");
		if (name == null)
			name = "-null-";

//		System.out.println("Source("+name+"): " + src);

		if (name.equals("BUTTON_CONN_TEST_JDBC"))
		{
			testJdbcConnection("testConnect", 
				_jdbcDriver.getText(), 
				_jdbcUrl.getText(),
				_jdbcUsername.getText(), 
				_jdbcPassword.getText());
		}

		// --- URL: BUTTON: "..." 
		if (_jdbcUrl_but.equals(source))
		{
			JFileChooser fc = new JFileChooser();
			if (System.getProperty("ASETUNE_SAVE_DIR") != null)
				fc.setCurrentDirectory(new File(System.getProperty("ASETUNE_SAVE_DIR")));
			int returnVal = fc.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) 
			{
				String url  = _jdbcUrl.getText();
				String path = fc.getSelectedFile().getAbsolutePath().replace('\\', '/');

				// Take away db suffix. ".h2.db"
				if (path.matches(".*\\.h2\\.db.*"))
					path = path.replaceAll("\\.h2\\.db", "");

				// Take away db suffix. ".data.db"
				if (path.matches(".*\\.data\\.db.*"))
					path = path.replaceAll("\\.data\\.db", "");

				// Take away index suffix. ".index.db"
				if (path.matches(".*\\.index\\.db.*"))
					path = path.replaceAll("\\.index\\.db", "");

				// Take away log suffix. ".99.log.db"
				if (path.matches(".*\\.[0-9]*\\.log\\.db.*"))
					path = path.replaceAll("\\.[0-9]*\\.log\\.db", "");

				// fill in the template
				if ( url.matches(".*\\[<path>\\]<dbname>.*") )
					url = url.replaceFirst("\\[<path>\\]<dbname>", path);
				else
					url += path;

				_jdbcUrl.setText(url);
			}
		}
		
		// --- TEXT FILED: "JDBC Driver" 
		if (_jdbcDriver.equals(source))
		{
			String jdbcDriver = _jdbcDriver.getText();
			if ("org.h2.Driver".equals(jdbcDriver))
				_pcsH2Option_startH2NetworkServer_chk.setVisible(true);
			else
				_pcsH2Option_startH2NetworkServer_chk.setVisible(false);
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
	
			JOptionPane.showMessageDialog(this, "Connection succeeded.", Version.getAppName()+" - connect check", JOptionPane.INFORMATION_MESSAGE);
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

	private void saveWizardData()
	{
		putWizardData("startH2NetworkServer", _pcsH2Option_startH2NetworkServer_chk.isSelected() + "");
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

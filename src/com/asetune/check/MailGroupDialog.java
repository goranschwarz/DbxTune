/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.check;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URL;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.asetune.Version;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;


public class MailGroupDialog
	extends JDialog 
	implements ActionListener, HyperlinkListener
{
	private static Logger _logger              = Logger.getLogger(MailGroupDialog.class);
	private static final long serialVersionUID = 0L;
	
	protected static final String ASETUNE_MAIL_GROUP_URL = "http://groups.google.com/group/asetune/subscribe";

	private JButton    _ok_but             = new JButton("OK");
	private JCheckBox  _doNotShow_chk      = new JCheckBox("Show this message again.");

	// Used to launch WEB BROWSER
	private Desktop            _desktop = null;

	/*---------------------------------------------------
	** BEGIN: constructors & Factories
	**---------------------------------------------------
	*/
	private MailGroupDialog(Frame owner)
	{
		super(owner, "", true);
		init(owner);
	}
	private MailGroupDialog(Dialog owner)
	{
		super(owner, "", true);
		init(owner);
	}

	private void init(Window owner)
	{
		if (Desktop.isDesktopSupported())
		{
			_desktop = Desktop.getDesktop();
			if ( ! _desktop.isSupported(Desktop.Action.BROWSE) ) 
				_desktop = null;
		}

		initComponents();

		// Set initial size
		pack();

		setLocationRelativeTo(owner);
	}

	//---------------------
	// FACTORIES
	public static void showDialog(Component owner)
	{
		if ( ! Desktop.isDesktopSupported() )
		{
			_logger.info("Desktop integration is not supported, can't show this dialog.");
			return;
		}

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean show = conf.getBooleanProperty("MailGroupDialog.show", true);

		_logger.debug("Show this message again is: "+show);
		if ( ! show )
			return;

		MailGroupDialog dialog = null;

		if      (owner instanceof Frame)  dialog = new MailGroupDialog((Frame) owner);
		else if (owner instanceof Dialog) dialog = new MailGroupDialog((Dialog)owner);
		else                              dialog = new MailGroupDialog((Dialog)null);

		dialog._doNotShow_chk.setToolTipText("<html>Show this message on next startup.</html>");
		dialog._doNotShow_chk.setSelected(show);

		dialog.setVisible(true);
		dialog.dispose();
	}

	
	/*---------------------------------------------------
	** END: constructors & Factories
	**---------------------------------------------------
	*/
	
	public void actionPerformed(ActionEvent e)
	{
		if ( _ok_but.equals(e.getSource()) )
		{
			Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);

			boolean show = _doNotShow_chk.isSelected();

			_logger.debug("Show this message again will be set to: "+show);
			conf.setProperty("MailGroupDialog.show", show);
			conf.save();

			dispose();
		}
	}

	@SuppressWarnings("unused")
	public void hyperlinkUpdate(HyperlinkEvent hle) 
	{  
		if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) 
		{  
			URL    url    = hle.getURL();
			String urlStr = ""+hle.getURL();
			_logger.info("You clicked on '"+urlStr+"'. Browser will be opened.");  

			try
			{
				_desktop.browse(new URI(urlStr));
			}
			catch (Exception e)
			{
				_logger.error("Problems when open the URL '"+urlStr+"'. Caught: "+e);
			}
		}
	}

	
	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	protected void initComponents() 
	{
		setTitle("Join Mail Group");
		
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("ins 0","[fill]",""));


		panel.add(initCheckPanel(),    "height 100%, wrap 15");
		panel.add(_doNotShow_chk,      "gapleft 15, growx, pushx");
		panel.add(_ok_but,             "tag ok, gapright 15, bottom, right, pushx, wrap 15");

		setContentPane(panel);

		// ADD ACTIONS TO COMPONENTS
		_ok_but        .addActionListener(this);
	}

	protected JPanel initCheckPanel() 
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 20 20 20 20","",""));

		JLabel icon        = new JLabel(SwingUtils.readImageIcon(Version.class, "images/asetune_icon_32.gif"));

		JLabel appName     = new JLabel(); 
		appName.setText(Version.getAppName());
		appName.setFont(new java.awt.Font("Dialog", Font.BOLD, 20));

		String msg =
			"<html>" +
			"<HEAD> " +
			"<style type=\"text/css\"> " +
			"<!-- " +
			"body {font-family: Arial, Helvetica, sans-serif;} " +
			"--> " +
			"</style> " +
			"</HEAD> " +
			"<BODY> " +

			"<b>Join the Information Mailing List.</b><br><br>" +
			"Go to this page to join the Mailing List<br>" +
			"<A HREF=\""+ASETUNE_MAIL_GROUP_URL+"\">"+ASETUNE_MAIL_GROUP_URL+"</A><br>" +
			"<br>" +
			"<HR size=\"1\">" +
			"In this group you can:<br>" +
			"<ul>" +
			"  <li>Get help.</li>" +
			"  <li>Learn from others how to use the tool.</li>" +
			"  <li>Give tips to others of how to use the tool.</li>" +
			"  <li>Share <i>User Defined Counters</i> with other users.</li>" +
			"  <li>Post feature request.</li>" +
			"  <li>etc...</li>" +
			"</ul>" +
			"<br>" +

			"</BODY> " +
			"</html>";

		JEditorPane feedback   = new JEditorPane("text/html", msg);
		feedback.setEditable(false);
		feedback.setOpaque(false);  
		feedback.addHyperlinkListener(this);

		
		panel.add(icon,       "span, center, wrap");
		panel.add(appName,    "span, center, wrap 20");

		panel.add(feedback,   "span, pushx, grow, wrap 20");

		return panel;
	}

	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/
	
	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
//		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		Configuration conf = new Configuration("c:\\MailGroupDialog.tmp.deleteme.properties");
		Configuration.setInstance(Configuration.USER_TEMP, conf);

		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		JFrame frame = new JFrame();
		MailGroupDialog.showDialog(frame);
	}
}


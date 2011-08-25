/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.check;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.asetune.Version;
import com.asetune.gui.AboutBox;
import com.asetune.utils.Configuration;
import com.asetune.utils.PlatformUtils;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.TimeUtils;


public class CheckDialog
	extends JDialog 
	implements ActionListener, HyperlinkListener
{
	private static Logger _logger              = Logger.getLogger(AboutBox.class);
	private static final long serialVersionUID = -2789087991515012041L;

	private JButton    _ok_but             = new JButton("OK");
	private JButton    _resetProxy_but     = new JButton("Reset Proxy Settings");
	private JLabel     _proxyHost_lbl      = new JLabel("HTTP Proxy Host");
	private JTextField _proxyHost_txt      = new JTextField();
	private JLabel     _proxyPort_lbl      = new JLabel("HTTP Proxy Port");
	private JTextField _proxyPort_txt      = new JTextField();
	private JCheckBox  _doNotShow_chk      = new JCheckBox("Do not show this again.");
	private JCheckBox  _doNotShowFeedback_chk = new JCheckBox("Do not show this feedback again.");

	private CheckForUpdates _cfu = null;



	/*---------------------------------------------------
	** BEGIN: constructors & Factories
	**---------------------------------------------------
	*/
	private CheckDialog(Frame owner, CheckForUpdates cfu)
	{
		super(owner, "", true);
		init(owner, cfu);
	}
	private CheckDialog(Dialog owner, CheckForUpdates cfu)
	{
		super(owner, "", true);
		init(owner, cfu);
	}
	private void init(Window owner, CheckForUpdates cfu)
	{
		_cfu = cfu;
		
		initComponents();

		// Set initial size
//		int width  = (3 * Toolkit.getDefaultToolkit().getScreenSize().width)  / 4;
//		int height = (3 * Toolkit.getDefaultToolkit().getScreenSize().height) / 4;
//		setSize(width, height);
		pack();

//		Dimension size = getPreferredSize();
//		size.width = 500;
//
//		setPreferredSize(size);
////		setMinimumSize(size);
//		setSize(size);

		setLocationRelativeTo(owner);
	}

	//---------------------
	// FACTORIES
//	public static void showDialog(Frame owner, CheckForUpdates cfu)
//	{
//		CheckDialog dialog = new CheckDialog(owner, cfu);
//		dialog.setVisible(true);
//		dialog.dispose();
//	}
//	public static void showDialog(Dialog owner, CheckForUpdates cfu)
//	{
//		CheckDialog dialog = new CheckDialog(owner, cfu);
//		dialog.setVisible(true);
//		dialog.dispose();
//	}
	public static void showDialog(Component owner, CheckForUpdates cfu)
	{
		//		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean doNotShowOnFailure = conf.getBooleanProperty("CheckDialog.doNotShowOnFailure", false);
		long lastShowDate = conf.getLongProperty("CheckDialog.lastShowTime", System.currentTimeMillis());

		long msSinceLastShow = System.currentTimeMillis() - lastShowDate;
		// Build a timeLimit        ms *  sec *  min *  hour * days
		long showTimeLimit   =   1000L *  60L *  60L *   24L *  30L; // show me in 30 days again

		if (_logger.isDebugEnabled())
		{
			String lastShowDateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(lastShowDate));
			_logger.debug("Last show date for CheckForUpdate Dialog was '"+lastShowDateStr+"', Saved 'CheckDialog.doNotShowOnFailure' was '"+doNotShowOnFailure+"', msSinceLastShow="+msSinceLastShow+"("+TimeUtils.msToTimeStr(msSinceLastShow)+"), showTimeLimit="+showTimeLimit+"("+TimeUtils.msToTimeStr(showTimeLimit)+").");
		}

		if (msSinceLastShow > showTimeLimit)
			doNotShowOnFailure = false;

		if (cfu != null && !cfu._checkSucceed && doNotShowOnFailure)
			return;


		// FEEDBACK
		boolean doNotShowFeedback = conf.getBooleanProperty("CheckDialog.doNotShowFeedback", false);
		long lastFeedbackDate = conf.getLongProperty("CheckDialog.lastFeebackDate", 0);

		if (cfu != null && cfu.hasFeedback() && doNotShowFeedback)
		{
			_logger.debug("This feedback date was '"+cfu.getFeedbackTime()+"', Saved 'CheckDialog.lastFeebackDate' was '"+lastFeedbackDate+"', doNotShowFeedback="+doNotShowFeedback+".");
			// Do not show if: savedFeedbackDate  
			if ( cfu.getFeedbackTime() > lastFeedbackDate)
			{
				_logger.debug("SHOW Feedback.");
			}
			else
			{
				_logger.debug("--- DO NOT SHOW Feedback.");
				return;
			}
		}
		
		
		CheckDialog dialog = null;

		if      (owner instanceof Frame)  dialog = new CheckDialog((Frame) owner, cfu);
		else if (owner instanceof Dialog) dialog = new CheckDialog((Dialog)owner, cfu);
		else                              dialog = new CheckDialog((Dialog)null, cfu);

		dialog._doNotShow_chk.setToolTipText("Even if you check this option, a new question like this will popup after 30 days.");
		dialog._doNotShow_chk.setSelected(doNotShowOnFailure);

		dialog._doNotShowFeedback_chk.setToolTipText("<html>Do not show <b>this</b> feedback question again.<br>Future feedbacks questions will still show up!</html>");
		dialog._doNotShowFeedback_chk.setSelected(doNotShowFeedback);

		dialog.setVisible(true);
		dialog.dispose();
	}

	
	/*---------------------------------------------------
	** END: constructors & Factories
	**---------------------------------------------------
	*/
	
	public void actionPerformed(ActionEvent e)
	{
		if ( _resetProxy_but.equals(e.getSource()) )
		{
			_proxyHost_txt.setText("");
			_proxyPort_txt.setText("");

			Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
			conf.remove("http.proxyHost");
			conf.remove("http.proxyPort");
			conf.save();

			System.clearProperty("http.proxyHost");
			System.clearProperty("http.proxyPort");			
		}
		if ( _ok_but.equals(e.getSource()) )
		{
			Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);

			String proxyHost = _proxyHost_txt.getText().trim();
			String proxyPort = _proxyPort_txt.getText().trim();
			if ( ! proxyHost.equals("") )
			{
				if (proxyPort.equals(""))
					proxyPort = "80";

				conf.setProperty("http.proxyHost", proxyHost);
				conf.setProperty("http.proxyPort", proxyPort);

				System.setProperty("http.proxyHost", proxyHost);
				System.setProperty("http.proxyPort", proxyPort);

				_proxyHost_txt.setText(proxyHost);
				_proxyPort_txt.setText(proxyPort);
			}
			conf.setProperty("CheckDialog.doNotShowOnFailure", _doNotShow_chk.isSelected());
			conf.setProperty("CheckDialog.lastShowTime", System.currentTimeMillis());

			if (_cfu.hasFeedback())
			{
				conf.setProperty("CheckDialog.doNotShowFeedback", _doNotShowFeedback_chk.isSelected());
				conf.setProperty("CheckDialog.lastFeebackDate", _cfu.getFeedbackTime());
			}
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
			_logger.info("You clicked on '"+urlStr+"'. On Windows systems a mail client or http browser will be opened.");  

			//Desktop.getDesktop().mail(someURI);
//			if ( System.getProperty("os.name").startsWith("Windows"))
			if ( PlatformUtils.getCurrentPlattform() == PlatformUtils.Platform_WIN )
			{
				if (urlStr.startsWith("file:/"))
					urlStr = urlStr.substring("file:/".length());

				String oscmd = "cmd.exe /c start "+urlStr;
				try
				{
					Runtime rt = Runtime.getRuntime();
					rt.exec(oscmd);
				}
				catch (Exception e)
				{
					SwingUtils.showErrorMessage("Problems executing command", "Problems when executing a Windows command to start '"+oscmd+"'.", e);
				}
			}
		}
	}

	
	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	protected void initComponents() 
	{
		setTitle("Check For Updates");
		
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("ins 0","[fill]",""));


		panel.add(initCheckPanel(),    "height 100%, wrap 15");
		panel.add(_ok_but,             "tag ok, gapright 15, bottom, right, pushx, wrap 15");

		setContentPane(panel);

		// ADD ACTIONS TO COMPONENTS
		_ok_but        .addActionListener(this);
		_resetProxy_but.addActionListener(this);
	}

	protected JPanel initCheckPanel() 
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 20 20 20 20","",""));

		JLabel icon        = new JLabel(SwingUtils.readImageIcon(Version.class, "images/asetune_icon_32.gif"));

		JLabel appName     = new JLabel(); 
		appName.setText(Version.getAppName());
		appName.setFont(new java.awt.Font("Dialog", Font.BOLD, 20));

		String msg = "";
		boolean showWhatsNew = false;
		boolean showFeedback = false;

		if (_cfu.checkSucceed())
		{
			if (_cfu.hasUpgrade())
			{
				msg   = "<b><center>New Upgrade is Available</center></b><br><br>" +
						"Latest version is "+_cfu.getNewAppVersionStr()+"<br><br>" +
						"And can be downloaded:<br>" +
						"<A HREF=\""+_cfu.getDownloadUrl()+"\">"+_cfu.getDownloadUrl()+"</A>";

				showWhatsNew = true;
			}
			else
			{
				msg = "<b>You have got the latest release.</b><br><br>" +
				      "I sure hope you like it.<br>" +
				      "And don't forget to donate!";
				
				if (_cfu.hasFeedback())
				{
					msg = "<b>You have got the latest release.</b><br>" +
						"But please read the below messages.";

					showFeedback = true;
				}
			}
		}
		else
		{
			msg = "<b>The Update Check failed.</b><br><br>" +
			      "This could happen for two reasons:<br>" +
			      "- You do <b>not</b> have internet access from this machine.<br>" +
			      "- You sit behind a proxy server.<br>" +
			      "<br>" +
			      "If you do not have internet access from this machine,<br>" +
			      "please do manual checks for new releases at:<br>" +
			      "<A HREF=\""+CheckForUpdates.ASETUNE_HOME_URL+"\">"+CheckForUpdates.ASETUNE_HOME_URL+"</A><br>" +
			      "<br>" +
			      "If you normally have internet access from this machine<br>" +
			      "I'm guessing you sit behind a Proxy Server and<br>" +
			      "you are using i PAC file for proxy lookups<br>" +
			      "Sorry! I do not support that right now.<br>" +
			      "<br>" +
			      "Please fill in the Proxy Server and Port below and try again.";
		}


		
		String str = 
			"<html>" +
			"<HEAD> " +
			"<style type=\"text/css\"> " +
			"<!-- " +
			"body {font-family: Arial, Helvetica, sans-serif;} " +
			"--> " +
			"</style> " +
			"</HEAD> " +

			msg +
//			"<br>" +
//			"<br>" +
//			"<HR ALIGN=\"center\" WIDTH=\"100%\">" +
//			"Current Version: "+Version.getVersionStr()+"<br>" +
//			"Current Build: "+Version.getBuildStr()+"<br>" +
//			"<br>" +
//			"Send comments and suggestions to: <br>" +
//			"<A HREF=\"mailto:goran_schwarz@hotmail.com\">goran_schwarz@hotmail.com</A><br>" +
//			"<A HREF=\"mailto:gorans@sybase.com\">gorans@sybase.com</A><br>" +
//			"<br>" +
//			"If this application gave you better performance, <br>" +
//			"<B>please</B> donate whatever you think it was worth: <br>" +
//			"<A HREF=\"http://www.asetune.com\">http://www.asetune.com</A><br>" +
			"</html>";
		
		JEditorPane feedback   = new JEditorPane("text/html", str);
		feedback.setEditable(false);
		feedback.setOpaque(false);  
		feedback.addHyperlinkListener(this);

		
		panel.add(icon,       "span, center, wrap");
		panel.add(appName,    "span, center, wrap 20");

		panel.add(feedback,   "span, pushx, grow, wrap 20");

		if ( showWhatsNew )
		{
			String whatsNewUrl = _cfu.getWhatsNewUrl();//default is: "http://www.asetune.com/history.html";
			try
			{
				_logger.info(Version.getAppName()+" What's new page is '"+whatsNewUrl+"'.");
				JEditorPane whatsNew   = new JEditorPane(new URL(whatsNewUrl));
				whatsNew.setEditable(false);
				//whatsNew.setOpaque(false);  
				whatsNew.addHyperlinkListener(this);

				JScrollPane scrollpane = new JScrollPane(whatsNew);
				scrollpane.setMinimumSize(new Dimension(200, 300));
				scrollpane.setPreferredSize(new Dimension(500, 300));
				panel.add(scrollpane,   "span, push, grow, wrap 10");
			}
			catch (Exception e)
			{
				_logger.warn("Problems opening What's new page '"+whatsNewUrl+"'. Caught: "+e.getMessage());
			}
		}

		if ( showFeedback )
		{
			String feedbackUrl = _cfu.getFeedbackUrl();
			try
			{
				_logger.info(Version.getAppName()+" feedback page is '"+feedbackUrl+"'.");
				JEditorPane feedbackPane   = new JEditorPane(new URL(feedbackUrl));
				feedbackPane.setEditable(false);
				//feedbackPane.setOpaque(false);  
				feedbackPane.addHyperlinkListener(this);

				JScrollPane scrollpane = new JScrollPane(feedbackPane);
				scrollpane.setMinimumSize(new Dimension(200, 300));
				scrollpane.setPreferredSize(new Dimension(500, 300));
				panel.add(scrollpane,   "span, push, grow, wrap 10");
			}
			catch (Exception e)
			{
				_logger.warn("Problems opening feedback page '"+feedbackUrl+"'. Caught: "+e.getMessage());
			}
		}

		if ( _cfu.checkSucceed() )
		{
			if (_cfu.hasFeedback())
				panel.add(_doNotShowFeedback_chk, "wrap");
		}
		else
		{
			_proxyHost_txt.setText(System.getProperty("http.proxyHost", ""));
			_proxyPort_txt.setText(System.getProperty("http.proxyPort", ""));
			
			panel.add(_proxyHost_lbl, "");
			panel.add(_proxyHost_txt, "pushx, growx, wrap");
	
			panel.add(_proxyPort_lbl, "");
			panel.add(_proxyPort_txt, "pushx, growx, wrap");

			panel.add(_resetProxy_but,"skip, right, pushx, wrap");

			panel.add(_doNotShow_chk, "wrap");
		}

		return panel;
	}

	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/
	
	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
//		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		CheckForUpdates check = new CheckForUpdates();

		JFrame frame = new JFrame();

		check._asetuneVersion = "1.0.0";
		check._downloadUrl    = "http://www.asetune.com/download.html";
		check._checkSucceed   = true;
		check._hasUpgrade     = true;
		CheckDialog.showDialog(frame, check);

		check._checkSucceed = true;
		check._hasUpgrade   = false;
		CheckDialog.showDialog(frame, check);

		check._checkSucceed = false;
		check._hasUpgrade   = false;
		CheckDialog.showDialog(frame, check);

	}
}


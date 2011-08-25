/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */

package asemon.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;

import asemon.Asemon;
import asemon.Version;
import asemon.check.CheckForUpdates;
import asemon.utils.PlatformUtils;
import asemon.utils.SwingUtils;

public class AboutBox 
	extends JDialog 
	implements ActionListener, HyperlinkListener
{
	private static Logger _logger              = Logger.getLogger(AboutBox.class);
	private static final long serialVersionUID = -2789087991515012041L;

	private JButton   _ok_but             = new JButton("OK");
	private JButton   _checkForUpdate_but = new JButton("Check For Updates...");

	public AboutBox(Frame owner)
	{
		super(owner);

		initComponents();

		// Set initial size
//		int width  = (3 * Toolkit.getDefaultToolkit().getScreenSize().width)  / 4;
//		int height = (3 * Toolkit.getDefaultToolkit().getScreenSize().height) / 4;
//		setSize(width, height);
		pack();

		Dimension size = getPreferredSize();
		size.width = 500;

		setPreferredSize(size);
//		setMinimumSize(size);
		setSize(size);

		setLocationRelativeTo(owner);

		setVisible(true);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if ( _ok_but.equals(e.getSource()) )
		{
			dispose();
		}

		if ( _checkForUpdate_but.equals(e.getSource()) )
		{
			// Not yet implemeted.
			//JOptionPane.showMessageDialog(this, "This in not yet implemented...");
			CheckForUpdates.noBlockCheck(this, true, true);
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
		setTitle("About");
		
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("ins 0","[fill]",""));

		JTabbedPane tab = new JTabbedPane();
		tab.add("About",       initTabAbout());
		tab.add("Todo",        initTabTodo());
		tab.add("History",     initTabHistory());
		tab.add("System Info", initTabSystemInfo());

		panel.add(tab,                 "height 100%, wrap 15");
		panel.add(_checkForUpdate_but, "        gapleft  15, bottom, left, split");
		panel.add(_ok_but,             "tag ok, gapright 15, bottom, right, pushx, wrap 15");

		setContentPane(panel);

		// ADD ACTIONS TO COMPONENTS
		_ok_but.addActionListener(this);
		_checkForUpdate_but.addActionListener(this);

	}

	protected JPanel initTabAbout() 
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 20 20 20 20","[grow]",""));

		JLabel icon        = new JLabel(SwingUtils.readImageIcon(Asemon.class, "images/asemon_icon_32.gif"));

		JLabel appName     = new JLabel(); 
		appName.setText(Version.getAppName());
		appName.setFont(new java.awt.Font("Dialog", Font.BOLD, 20));

		String str = 
			"<html>" +
			"<HEAD> " +
			"<style type=\"text/css\"> " +
			"<!-- " +
			"body {font-family: Arial, Helvetica, sans-serif;} " +
			"--> " +
			"</style> " +
			"</HEAD> " +

			"Version: "+Version.getVersionStr()+"<br>" +
			"Build: "+Version.getBuildStr()+"<br>";

		if (Version.getVersionStr().endsWith(".dev"))
			str += "<b>Note: This is a development version</b><br>";

		str +=
			"<br>" +
			"Source Code Revision: "+Version.getSourceRev()+"<br>" +
			"Source Code Date: "+Version.getSourceDate()+"<br>" +
			"<br>" +
			"Send comments and suggestions to: <br>" +
			"<A HREF=\"mailto:goran_schwarz@hotmail.com\">goran_schwarz@hotmail.com</A><br>" +
			"<A HREF=\"mailto:gorans@sybase.com\">gorans@sybase.com</A><br>" +
			"<br>" +
			"Credits to:<br>" +
			"<UL>" +
			"<LI>Jean-Paul Martin  for initial version of AseMon.<br>" +
			"<LI>Reine Lindqvist   for various help.<br>" +
			"</UL>" +
			"<br>" +
			"If this application gave you better performance, <br>" +
			"<B>please</B> donate whatever you think it was worth: <br>" +
			"<A HREF=\"http://www.asemon.se\">http://www.asemon.se</A><br>" +
			"</html>";
		
		JEditorPane feedback   = new JEditorPane("text/html", str);
		feedback.setEditable(false);
		feedback.setOpaque(false);  
		feedback.addHyperlinkListener(this);

		
		panel.add(icon,       "center, wrap");
		panel.add(appName,    "center, wrap 20");

		panel.add(feedback,   "wrap 20");

		return panel;
	}

	protected JPanel initTabTodo() 
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		JEditorPane htmlPane   = new JEditorPane();
		htmlPane.addHyperlinkListener(this);
		htmlPane.setEditable(false);
		htmlPane.setOpaque(false);  

		URL url = Version.class.getResource("todo.html");
		if (url != null) 
		{
			try 
			{
				htmlPane.setPage(url);
			} 
			catch (IOException e) 
			{
				htmlPane.setText("Attempted to read a bad URL: " + url);
				_logger.error("Attempted to read a bad URL: " + url);
			}
		} 
		else 
		{
			htmlPane.setText("Couldn't find file: todo.html");
		}


		panel.add(new JScrollPane(htmlPane), BorderLayout.CENTER);
		return panel;
	}

	protected JPanel initTabHistory() 
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		JEditorPane htmlPane   = new JEditorPane();
		htmlPane.addHyperlinkListener(this);
		htmlPane.setEditable(false);
		htmlPane.setOpaque(false);  

		URL url = Version.class.getResource("history.html");
		if (url != null) 
		{
			try 
			{
				htmlPane.setPage(url);
			} 
			catch (IOException e) 
			{
				htmlPane.setText("Attempted to read a bad URL: " + url);
				_logger.error("Attempted to read a bad URL: " + url);
			}
		} 
		else 
		{
			htmlPane.setText("Couldn't find file: history.html");
		}


		panel.add(new JScrollPane(htmlPane), BorderLayout.CENTER);
		return panel;
	}
	
	
	protected JPanel initTabSystemInfo() 
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		Properties sysProps = System.getProperties();

		Vector<String> cols = new Vector<String>();
		cols.add("Keys");
		cols.add("Values");

		Vector<Vector<String>> rows = new Vector<Vector<String>>();

		Enumeration<Object> e = sysProps.keys();
		while(e.hasMoreElements())
		{
			String key = (String)e.nextElement();
			String val = sysProps.getProperty(key);

			Vector<String> row = new Vector<String>();
			row.add(key);
			row.add(val);
			
			rows.add(row);
		}

		JXTable tab = new JXTable(rows, cols);
		tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tab.packAll(); // set size so that all content in all cells are visible
		tab.setSortable(true);
		tab.setColumnControlVisible(true);
		
		panel.add(new JScrollPane(tab), BorderLayout.CENTER);
		return panel;
	}
	
	
	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/
	
	public static void main(String[] args)
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		new AboutBox(null);
	}
}


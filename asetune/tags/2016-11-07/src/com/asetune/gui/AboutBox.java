/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */

package com.asetune.gui;

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
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.sort.RowFilters;

import com.asetune.Version;
import com.asetune.check.CheckForUpdates;
import com.asetune.tools.sqlw.QueryWindow;
import com.asetune.utils.PlatformUtils;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;


public class AboutBox
	extends JDialog
	implements ActionListener, HyperlinkListener
{
	private static Logger _logger              = Logger.getLogger(AboutBox.class);
	private static final long serialVersionUID = -2789087991515012041L;

	private JButton   _ok_but             = new JButton("OK");
	private JButton   _checkForUpdate_but = new JButton("Check For Updates...");

	protected JLabel            _filter_lbl   = new JLabel("Filter: ");
	protected JTextField        _filter_txt   = new JTextField();
	protected JLabel            _filter_cnt   = new JLabel();

	public AboutBox(Frame owner)
	{
		super(owner);
		setModal(true);

		initComponents();

		// Set initial size
//		int width  = (3 * Toolkit.getDefaultToolkit().getScreenSize().width)  / 4;
//		int height = (3 * Toolkit.getDefaultToolkit().getScreenSize().height) / 4;
//		setSize(width, height);
		pack();

		Dimension size = getPreferredSize();
		size.width = SwingUtils.hiDpiScale(500);

		setPreferredSize(size);
//		setMinimumSize(size);
		setSize(size);

		setLocationRelativeTo(owner);
	}

	public static void show(Frame owner)
	{
		AboutBox dlg = new AboutBox(owner);
		dlg.setVisible(true);
		dlg.dispose();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if ( _ok_but.equals(e.getSource()) )
		{
			dispose();
		}

		if ( _checkForUpdate_but.equals(e.getSource()) )
		{
			CheckForUpdates.getInstance().checkForUpdateNoBlock(this, true, true);
//			String appName = Version.getAppName();
//			
//			if (AseTune.APP_NAME.equals(appName))
//			{
//    			CheckForUpdates.noBlockCheck(this, true, true);
//			}
//			else if (QueryWindow.APP_NAME.equals(appName))
//			{
//    			CheckForUpdates.noBlockCheckSqlWindow(this, true, true);
//			}
////			else if (IqTune.APP_NAME.equals(appName))
////			{
////    			CheckForUpdates.noBlockCheckIqTune(this, true, true);
////			}
//			else
//			{
//    			// Not yet implemented for application ?????
//    			JOptionPane.showMessageDialog(this, "This in not yet implemented for application '"+appName+"'.");
//			}
		}
	}

	@Override
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
//		tab.add("Todo",        initTabTodo());
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

		JLabel icon = new JLabel(SwingUtils.readImageIcon(Version.class, "images/asetune_icon_32.gif"));
		
		String appName = Version.getAppName();
//		if      (AseTune    .APP_NAME.equals(appName)) icon = new JLabel(SwingUtils.readImageIcon(Version.class, "images/asetune_icon_32.gif"));
//		else if (QueryWindow.APP_NAME.equals(appName)) icon = new JLabel(SwingUtils.readImageIcon(Version.class, "images/sql_query_window_32.png"));
//		else if (IqTune     .APP_NAME.equals(appName)) icon = new JLabel(SwingUtils.readImageIcon(Version.class, "images/iqtune_icon_32.png"));

		if (QueryWindow.APP_NAME.equals(appName))
		{
			icon = new JLabel(SwingUtils.readImageIcon(Version.class, "images/sql_query_window_32.png"));
		}
		else
		{
			if (MainFrame.hasInstance())
			{
    			ImageIcon appIcon = MainFrame.getInstance().getApplicationIcon32();
    			if (appIcon != null)
    				icon = new JLabel(appIcon);
			}
		}

		JLabel appName_lbl     = new JLabel();
		appName_lbl.setText(Version.getAppName());
		appName_lbl.setFont(new java.awt.Font("Dialog", Font.BOLD, SwingUtils.hiDpiScale(20)));

		String fontSize = "";
		if (SwingUtils.isHiDpi())
			fontSize = "font-size: " + UIManager.getFont("Label.font").getSize() + "px";

		String str =
			"<html>" +
			"<HEAD> " +
			"<style type=\"text/css\"> " +
			"<!-- " +
			"body {font-family: Arial, Helvetica, sans-serif; " + fontSize + "} " +
			"--> " +
			"</style> " +
			"</HEAD> " +

			"Version: "+Version.getVersionStr()+"<br>" +
			"Build: "+Version.getBuildStr()+"<br>";

//		if (Version.getVersionStr().endsWith(".dev"))
		if (Version.IS_DEVELOPMENT_VERSION)
			str += "<b>Note: This is a development version</b><br>";

		String creditTo =
			"Credits to:<br>" +
			"<UL>" +
			"<LI>Jean-Paul Martin  for initial version of AseMon.<br>" +
			"<LI>Reine Lindqvist   for various help.<br>" +
			"<LI>Niklas Andersson  for various help.<br>" +
			"</UL>" +
			"<br>";

		String suggestion = 
			"If this application gave you better performance, <br>";

		if (QueryWindow.APP_NAME.equals(appName))
		{
			creditTo = "";
			suggestion = "If you liked this application, <br>";
		}

		str +=
			"<br>" +
			"Source Code Revision: "+Version.getSourceRev()+"<br>" +
			"Source Code Date: "+Version.getSourceDate()+"<br>" +
			"<br>" +
			"Send comments and suggestions to: <br>" +
			"<A HREF=\"mailto:goran_schwarz@hotmail.com\">goran_schwarz@hotmail.com</A><br>" +
			"<br>" +
			creditTo +
			suggestion +
			"<B>please</B> donate whatever you think it was worth: <br>" +
			"<A HREF=\"http://www.asetune.com\">http://www.asetune.com</A><br>" +
			"</html>";

		JEditorPane feedback   = new JEditorPane("text/html", str);
		feedback.setEditable(false);
		feedback.setOpaque(false);
		feedback.addHyperlinkListener(this);


		panel.add(icon,        "center, wrap");
		panel.add(appName_lbl, "center, wrap 20");

		panel.add(feedback,    "wrap 20");

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
//		htmlPane.setFont(UIManager.getFont("Label.font"));
		htmlPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		htmlPane.setFont(UIManager.getFont("Label.font"));

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
//		panel.setLayout(new BorderLayout());
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		_filter_txt.setToolTipText("Client filter, that does regular expression on all table cells using this value");
		_filter_cnt.setToolTipText("Visible rows / actual rows in the GUI Table");

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

		final JXTable tab = new JXTable(rows, cols);
		tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tab.packAll(); // set size so that all content in all cells are visible
		tab.setSortable(true);
		tab.setColumnControlVisible(true);

		panel.add(_filter_lbl,           "split");
		panel.add(_filter_txt,           "growx, pushx");
		panel.add(_filter_cnt,           "wrap");

//		panel.add(new JScrollPane(tab),  BorderLayout.CENTER);
		panel.add(new JScrollPane(tab),  "push, grow, wrap");

		_filter_txt.addCaretListener(new CaretListener()
		{
			@Override
			public void caretUpdate(CaretEvent e)
			{
				String searchString = _filter_txt.getText().trim();
				if ( searchString.length() <= 0 ) 
					tab.setRowFilter(null);
				else
				{
					// Create a array with all visible columns... hence: it's only those we want to search
					// Note the indices are MODEL column index
					int[] mcols = new int[tab.getColumnCount()];
					for (int i=0; i<mcols.length; i++)
						mcols[i] = tab.convertColumnIndexToModel(i);
					
					tab.setRowFilter(RowFilters.regexFilter(Pattern.CASE_INSENSITIVE, searchString + ".*", mcols));
				}
				
				String rowc = tab.getRowCount() + "/" + tab.getModel().getRowCount();
				_filter_cnt.setText(rowc);
			}
		});

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


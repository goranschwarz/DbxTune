/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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

package com.dbxtune.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.swing.ImageIcon;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdesktop.swingx.JXTable;

import com.dbxtune.Version;
import com.dbxtune.check.CheckForUpdates;
import com.dbxtune.gui.swing.GTableFilter;
import com.dbxtune.tools.sqlw.QueryWindow;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.PlatformUtils;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;


public class AboutBox
	extends JDialog
	implements ActionListener, HyperlinkListener
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = -2789087991515012041L;

	private JButton   _ok_but             = new JButton("OK");
	private JButton   _checkForUpdate_but = new JButton("Check For Updates...");

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
		tab.add("About",           initTabAbout());
//		tab.add("Todo",            initTabTodo());
		tab.add("History",         initTabHistory());
		tab.add("System Info",     initTabSystemInfo());
		tab.add("Combined Config", initTabCombinedConfig());

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
			"<A HREF=\"http://www.dbxtune.com\">http://www.dbxtune.com</A><br>" +
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

		GTableFilter filter = new GTableFilter(tab);
		
		panel.add(filter,                "pushx, growx, wrap");
		panel.add(new JScrollPane(tab),  "push, grow, wrap");

		return panel;
	}

	protected JPanel initTabCombinedConfig()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		Configuration combinedConfig = Configuration.getCombinedConfiguration();

		Vector<String> cols = new Vector<String>();
		cols.add("Keys");
		cols.add("Values");

		Vector<Vector<String>> rows = new Vector<Vector<String>>();

		List<String> keyList = combinedConfig.getKeys("");
		for (String key : keyList)
		{
			String val = combinedConfig.getProperty(key);

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

		GTableFilter filter = new GTableFilter(tab);
		
		panel.add(filter,                "pushx, growx, wrap");
		panel.add(new JScrollPane(tab),  "push, grow, wrap");

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


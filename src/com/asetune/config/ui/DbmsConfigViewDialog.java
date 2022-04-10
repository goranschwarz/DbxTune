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
package com.asetune.config.ui;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.config.dbms.DbmsConfigDiffEngine;
import com.asetune.config.dbms.DbmsConfigDiffEngine.Context;
import com.asetune.config.dbms.DbmsConfigManager;
import com.asetune.config.dbms.DbmsConfigTextManager;
import com.asetune.config.dbms.IDbmsConfig;
import com.asetune.config.dbms.IDbmsConfigText;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.gui.swing.WaitForExecDialog.BgExecutor;
import com.asetune.pcs.PersistReader;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class DbmsConfigViewDialog
//extends JDialog
extends JFrame
implements ActionListener, ConnectionProvider
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger = Logger.getLogger(DbmsConfigViewDialog.class);

	// PANEL: OK-CANCEL
	private JButton                _ok                   = new JButton("OK");
	private JButton                _cancel               = new JButton("Cancel");

	@SuppressWarnings("unused")
	private Window                 _owner                = null;

	private JButton                _refresh              = new JButton("Refresh");
	private JButton                _showIssues           = new JButton("Show Issues");
	private JButton                _doExternalConfigDiff = new JButton("Do External Config Diff...");
	private JLabel                 _freeMb               = new JLabel();
	
	private ConnectionProvider     _connProvider         = null;

	// This will hold all DBMS Config & Text panels
	private JTabbedPane            _tabPane                          = new JTabbedPane();
	
	private DbmsConfigViewDialog(Frame owner, ConnectionProvider connProvider)
	{
		super("DBMS Configuration");
		init(owner, connProvider);
	}
	private DbmsConfigViewDialog(Dialog owner, ConnectionProvider connProvider)
	{
		super("DBMS Configuration");
		init(owner, connProvider);
	}

	public static void showDialog(Frame owner, ConnectionProvider connProvider)
	{
		DbmsConfigViewDialog dialog = new DbmsConfigViewDialog(owner, connProvider);
		dialog.setVisible(true);
	}
	public static void showDialog(Dialog owner, ConnectionProvider connProvider)
	{
		DbmsConfigViewDialog dialog = new DbmsConfigViewDialog(owner, connProvider);
		dialog.setVisible(true);
	}
	public static void showDialog(Component owner, ConnectionProvider connProvider)
	{
		DbmsConfigViewDialog dialog = null;
		if (owner instanceof Frame)
			dialog = new DbmsConfigViewDialog((Frame)owner, connProvider);
		else if (owner instanceof Dialog)
			dialog = new DbmsConfigViewDialog((Dialog)owner, connProvider);
		else
			dialog = new DbmsConfigViewDialog((Dialog)null, connProvider);

		dialog.setVisible(true);
	}

	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);

		// Refresh only enabled if connected to ASE, not offline for the moment
		if (visible)
		{
			boolean b = true;
			if (PersistReader.hasInstance())
				if (PersistReader.getInstance().isConnected())
					b = false;

			_refresh             .setEnabled(b);
//			_showIssues          .setEnabled(b);

			_doExternalConfigDiff.setVisible(b);
		}
	}
	
	private void init(Window owner, ConnectionProvider connProvider)
	{
		_owner = owner;

		_connProvider = connProvider;
		initComponents();

//		pack();

//		Dimension size = getPreferredSize();
//		size.width += 200;
//
//		setPreferredSize(size);
////		setMinimumSize(size);
//		setSize(size);

		setLocationRelativeTo(owner);

//		setFocus();
	}

	protected void initComponents()
	{
		// Set the icon, if we "just" do setIconImage() on the JDialog
		// it will not be the "correct" icon in the Alt-Tab list on Windows
		// So we need to grab the owner, and set that since the icon is grabbed from the owner...
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/config_dbms_view_16.png");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/config_dbms_view_32.png");
		if (icon16 != null || icon32 != null)
		{
			ArrayList<Image> iconList = new ArrayList<Image>();
			if (icon16 != null) iconList.add(icon16.getImage());
			if (icon32 != null) iconList.add(icon32.getImage());

			Object owner = getOwner();
			if (owner != null && owner instanceof Frame)
				((Frame)owner).setIconImages(iconList);
			else
				setIconImages(iconList);
		}

		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0, wrap 1","",""));   // insets Top Left Bottom Right

		//JTabbedPane tabPane = new JTabbedPane();
		_tabPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		
		// ADD: DBMS Config & Text tabs
		if (DbmsConfigManager.hasInstance())
		{
			DbmsConfigPanel dcp = new DbmsConfigPanel(this, DbmsConfigManager.getInstance());
			_tabPane.add(DbmsConfigManager.getInstance().getTabLabel(), dcp);
		}
		if (DbmsConfigTextManager.hasInstances())
		{
			for (IDbmsConfigText dct : DbmsConfigTextManager.getInstanceList())
			{
				DbmsConfigTextPanel dctPanel = new DbmsConfigTextPanel(this, dct);
				_tabPane.addTab(dct.getTabLabel(), dctPanel);
			}
		}
			
		
		panel.add(_tabPane,              "grow, height 100%, width 100%");
		panel.add(createOkCancelPanel(), "grow, push, bottom");

		this.addWindowListener(new java.awt.event.WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				saveProps();
			}
		});

		loadProps();

		setContentPane(panel);
	}

	private JPanel createOkCancelPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

		panel.add(_refresh,              "left");
		panel.add(_showIssues,           "left");
		panel.add(_doExternalConfigDiff, "hidemode 3, left");
		panel.add(_freeMb,               "left");

		// ADD the OK, Cancel, Apply buttons
		panel.add(_ok,     "push, tag ok");
		panel.add(_cancel, "tag cancel");
//		panel.add(_apply,  "tag apply");

//		_apply.setEnabled(false);

		// Initialize some fields.
		_freeMb.setToolTipText("How much memory is available for reconfiguration, same value as you can get from sp_configure 'memory'.");
//		_freeMb.setText(AseCacheConfig.getInstance().getFreeMemoryStr());
//		_freeMb.setText(((Cache) AseConfigText.getInstance(ConfigType.AseCacheConfig)).getFreeMemoryStr());
		_freeMb.setText(DbmsConfigTextManager.hasInstances() ? DbmsConfigManager.getInstance().getFreeMemoryStr() : "");

		_refresh        .setToolTipText("Re-read the configuration.");
		_showIssues     .setToolTipText("When connecting to the DBMS, we check for various configuration isues... This opens the window with any config issues...");
		_doExternalConfigDiff.setToolTipText(
				"<html>"
				+ "<h3>Do difference check on DBMS Configuration</h3>"
				+ "This will do the following steps:"
				+ "<ul>"
//				+ "  <li>Open a Connection Dialog, which lets you connect to another <b>running DBMS Instance</b><br>"
//				+ "      Or open a Connection to a previously <b>recorded session</b>"
//				+ "      </li>"
				+ "  <li>Open a Connection Dialog, which lets you connect to"
				+ "     <ul>"
				+ "        <li>Another <b>running DBMS Instance</b></li>"
				+ "        <li>A previously <b>recorded session</b></li>"
				+ "     </ul>"
				+ "  </li>"
				+ "  <li>Get a simple Configuration, like: ConfigName, ConfigValue </li>"
				+ "  <li>Do difference check.</li>"
				+ "  <li>Present any differences in a Simple Table...</li>"
				+ "</ul>"
				+ "</html>");

		// ADD ACTIONS TO COMPONENTS
		_ok                  .addActionListener(this);
		_cancel              .addActionListener(this);
//		_apply               .addActionListener(this);
		_refresh             .addActionListener(this);
		_doExternalConfigDiff.addActionListener(this);
		_showIssues          .addActionListener(this);

		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

		// --- BUTTON: REFRESH ---
		if (_refresh.equals(source))
		{
			doRefresh();
		}

		// --- BUTTON: REFRESH ---
		if (_doExternalConfigDiff.equals(source))
		{
			doExternalConfigurationDiff();
		}

		// --- BUTTON: SHOW ISSUES ---
		if (_showIssues.equals(source))
		{
			IDbmsConfig dbmsConfig = DbmsConfigManager.getInstance();
			DbmsConfigIssuesDialog.showDialog(this, dbmsConfig, this);
		}

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			setVisible(false);
		}

		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			doApply();
			saveProps();
			setVisible(false);
		}

	}

	private void doRefresh()
	{
		WaitForExecDialog wait = new WaitForExecDialog(this, "Getting DBMS Configuration");

		// Kick this of as it's own thread, otherwise the sleep below, might block the Swing Event Dispatcher Thread
		BgExecutor bgExec = new BgExecutor(wait)
		{
			@Override
			public Object doWork()
			{
				try
				{
					for (int t=0; t<_tabPane.getTabCount(); t++)
					{
						Component comp = _tabPane.getComponentAt(t);
						String    name = _tabPane.getTitleAt(t);
			
						getWaitDialog().setState("Refreshing tab '"+name+"'.");
						if (comp instanceof DbmsConfigPanel)
						{
							((DbmsConfigPanel)comp).refresh();
						}
						else if (comp instanceof DbmsConfigTextPanel)
						{
							((DbmsConfigTextPanel)comp).refresh();
						}
					}
				}
				catch(Exception ex) 
				{
					_logger.info("Initialization of the DBMS Configuration did not succeed. Caught: "+ex); 
				}
				getWaitDialog().setState("Done");

				return null;
			}
		};
		wait.execAndWait(bgExec);

		_freeMb.setText(DbmsConfigTextManager.hasInstances() ? DbmsConfigManager.getInstance().getFreeMemoryStr() : "");
	}

	private void doApply()
	{
	}

	/**
	 * External DBMS Configuration Check
	 */
	private void doExternalConfigurationDiff()
	{
//		DbmsConfigDiffEngine diffEngine = new DbmsConfigDiffEngine(this, getConnection(), null);
		DbmsConfigDiffEngine diffEngine = new DbmsConfigDiffEngine(this, null);
		if (diffEngine.initialize())
		{
			Context diffContext = diffEngine.checkForDiffrens();
			
//			if ( ! diffContext.hasDifferences() )
//			{
//				SwingUtils.showInfoMessage(this, "No Difference", "SUCCESS - No DBMS Configuration Difference was found.");
//			}
//			else
//			{
//			}

			// Always show the Dialog, even if there are NO differences
			DbmsConfigDiffViewDialog diffDialog = new DbmsConfigDiffViewDialog(this, diffContext);
			diffDialog.setVisible(true);
		}
	}
	
	/*---------------------------------------------------
	** BEGIN: Property handling
	**---------------------------------------------------
	*/
	private void saveProps()
  	{
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		String base = this.getClass().getSimpleName() + ".";

		if (tmpConf != null)
		{
			tmpConf.setLayoutProperty(base + "window.width", this.getSize().width);
			tmpConf.setLayoutProperty(base + "window.height", this.getSize().height);
			tmpConf.setLayoutProperty(base + "window.pos.x", this.getLocationOnScreen().x);
			tmpConf.setLayoutProperty(base + "window.pos.y", this.getLocationOnScreen().y);

			tmpConf.save();
		}
  	}

  	private void loadProps()
  	{
		int     width     = SwingUtils.hiDpiScale(1160);  // initial window with   if not opened before
		int     height    = SwingUtils.hiDpiScale(700);   // initial window height if not opened before
		int     x         = -1;
		int     y         = -1;

//		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
		Configuration tmpConf = Configuration.getCombinedConfiguration();
		String base = this.getClass().getSimpleName() + ".";

		setSize(width, height);

		if (tmpConf == null)
			return;

		width  = tmpConf.getLayoutProperty(base + "window.width",  width);
		height = tmpConf.getLayoutProperty(base + "window.height", height);
		x      = tmpConf.getLayoutProperty(base + "window.pos.x",  -1);
		y      = tmpConf.getLayoutProperty(base + "window.pos.y",  -1);

		if (width != -1 && height != -1)
		{
			setSize(width, height);
		}
		if (x != -1 && y != -1)
		{
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
				this.setLocation(x, y);
		}
		else
		{
			SwingUtils.centerWindow(this);
		}
	}
	/*---------------------------------------------------
	** END: Property handling
	**---------------------------------------------------
	*/

  	@Override
	public DbxConnection getConnection()
	{
		return _connProvider.getConnection();
	}
	@Override
	public DbxConnection getNewConnection(String appname)
	{
		return _connProvider.getNewConnection(appname);
	}
}

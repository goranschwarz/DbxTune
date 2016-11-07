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
import com.asetune.config.dbms.DbmsConfigManager;
import com.asetune.config.dbms.DbmsConfigTextManager;
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
	private JButton                _ok              = new JButton("OK");
	private JButton                _cancel          = new JButton("Cancel");

	@SuppressWarnings("unused")
	private Window                 _owner           = null;

	private JButton                _refresh         = new JButton("Refresh");
	private JLabel                 _freeMb          = new JLabel();
	
	private ConnectionProvider     _connProvider    = null;
	
	private JTabbedPane            _tabPane                          = new JTabbedPane();
//	private DbmsConfigPanel         _aseConfigPanel                   = new DbmsConfigPanel(this);
//	private DbmsConfigTextPanel     _aseConfigCachePanel              = new DbmsConfigTextPanel(this, ConfigType.AseCacheConfig);
//	private DbmsConfigTextPanel     _aseConfigThreadPoolPanel         = new DbmsConfigTextPanel(this, ConfigType.AseThreadPool);
//	private DbmsConfigTextPanel     _aseConfigHelpDbPanel             = new DbmsConfigTextPanel(this, ConfigType.AseHelpDb);
//	private DbmsConfigTextPanel     _aseConfigTempdbPanel             = new DbmsConfigTextPanel(this, ConfigType.AseTempdb);
//	private DbmsConfigTextPanel     _aseConfigHelpDevicePanel         = new DbmsConfigTextPanel(this, ConfigType.AseHelpDevice);
//	private DbmsConfigTextPanel     _aseConfigDeviceFsSpaceUsagePanel = new DbmsConfigTextPanel(this, ConfigType.AseDeviceFsSpaceUsage);
//	private DbmsConfigTextPanel     _aseConfigHelpServerPanel         = new DbmsConfigTextPanel(this, ConfigType.AseHelpServer);
//	private DbmsConfigTextPanel     _aseConfigTraceflagsPanel         = new DbmsConfigTextPanel(this, ConfigType.AseTraceflags);
//	private DbmsConfigTextPanel     _aseConfigSpVersionPanel          = new DbmsConfigTextPanel(this, ConfigType.AseSpVersion);
//	private DbmsConfigTextPanel     _aseConfigShmDumpCfgPanel         = new DbmsConfigTextPanel(this, ConfigType.AseShmDumpConfig);
//	private DbmsConfigTextPanel     _aseConfigMonitorCfgPanel         = new DbmsConfigTextPanel(this, ConfigType.AseMonitorConfig);
//	private DbmsConfigTextPanel     _aseConfigHelpSortPanel           = new DbmsConfigTextPanel(this, ConfigType.AseHelpSort);
//	private DbmsConfigTextPanel     _aseConfigLicenseInfoPanel        = new DbmsConfigTextPanel(this, ConfigType.AseLicenseInfo);
//	private DbmsConfigTextPanel     _aseConfigClusterInfoPanel        = new DbmsConfigTextPanel(this, ConfigType.AseClusterInfo);
//	private DbmsConfigTextPanel     _aseConfigFileContentPanel        = new DbmsConfigTextPanel(this, ConfigType.AseConfigFile);
	
	private DbmsConfigViewDialog(Frame owner, ConnectionProvider connProvider)
	{
//		super(owner, "ASE Configuration", true);
		super("DBMS Configuration");
//		setModalityType(ModalityType.MODELESS);
		init(owner, connProvider);
	}
	private DbmsConfigViewDialog(Dialog owner, ConnectionProvider connProvider)
	{
//		super(owner, "ASE Configuration", true);
		super("DBMS Configuration");
//		setModalityType(ModalityType.MODELESS);
		init(owner, connProvider);
	}

	public static void showDialog(Frame owner, ConnectionProvider connProvider)
	{
		DbmsConfigViewDialog dialog = new DbmsConfigViewDialog(owner, connProvider);
		dialog.setVisible(true);
//		dialog.dispose();
	}
	public static void showDialog(Dialog owner, ConnectionProvider connProvider)
	{
		DbmsConfigViewDialog dialog = new DbmsConfigViewDialog(owner, connProvider);
		dialog.setVisible(true);
//		dialog.dispose();
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
//		dialog.dispose();
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

			_refresh.setEnabled(b);
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

//		super(_owner);
//		if (_owner != null)
//			setIconImage(_owner.getIconImage());

//		setTitle("ASE Configuration");

		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0, wrap 1","",""));   // insets Top Left Bottom Right

		//JTabbedPane tabPane = new JTabbedPane();
		_tabPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

//		_tabPane.add("ASE Config",       _aseConfigPanel);
//		_tabPane.add("Cache Config",     _aseConfigCachePanel);
//		_tabPane.add("Thread Pools",     _aseConfigThreadPoolPanel);
//		_tabPane.add("sp_helpdb",        _aseConfigHelpDbPanel);
//		_tabPane.add("tempdb",           _aseConfigTempdbPanel);
//		_tabPane.add("sp_helpdevice",    _aseConfigHelpDevicePanel);
//		_tabPane.add("Device FS Usage",  _aseConfigDeviceFsSpaceUsagePanel);
//		_tabPane.add("sp_helpserver",    _aseConfigHelpServerPanel);
//		_tabPane.add("traceflags",       _aseConfigTraceflagsPanel);
//		_tabPane.add("sp_version",       _aseConfigSpVersionPanel);
//		_tabPane.add("sp_shmdumpconfig", _aseConfigShmDumpCfgPanel);
//		_tabPane.add("sp_monitorconfig", _aseConfigMonitorCfgPanel);
//		_tabPane.add("sp_helpsort",      _aseConfigHelpSortPanel);
//		_tabPane.add("ASE License Info", _aseConfigLicenseInfoPanel);
//		_tabPane.add("Cluster Info",     _aseConfigClusterInfoPanel);
//		_tabPane.add("Config File",      _aseConfigFileContentPanel);

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

		panel.add(_refresh, "left");
		panel.add(_freeMb,  "left");

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

		_refresh.setToolTipText("Re-read the configuration.");

		// ADD ACTIONS TO COMPONENTS
		_ok           .addActionListener(this);
		_cancel       .addActionListener(this);
//		_apply        .addActionListener(this);
		_refresh      .addActionListener(this);

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
	}

	private void doApply()
	{
	}

	/*---------------------------------------------------
	** BEGIN: Property handling
	**---------------------------------------------------
	*/
	private void saveProps()
  	{
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		String base = "aseConfigViewDialog.";

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
		String base = "aseConfigViewDialog.";

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
//	public Connection getConnection()
	public DbxConnection getConnection()
	{
		return _connProvider.getConnection();
	}
	@Override
//	public Connection getNewConnection(String appname)
	public DbxConnection getNewConnection(String appname)
	{
		return _connProvider.getNewConnection(appname);
	}
}

package com.asetune.gui;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.asetune.AseConfigText;
import com.asetune.AseConfigText.Cache;
import com.asetune.AseConfigText.ConfigType;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class AseConfigViewDialog
extends JDialog
implements ActionListener
{
	private static final long serialVersionUID = 1L;
//	private static Logger _logger = Logger.getLogger(AseConfigViewDialog.class);

	// PANEL: OK-CANCEL
	private JButton                _ok              = new JButton("OK");
	private JButton                _cancel          = new JButton("Cancel");

	private JLabel                 _freeMb          = new JLabel();
	
	private AseConfigPanel         _aseConfigPanel           = new AseConfigPanel();
//	private AseCacheConfigPanel    _aseCacheConfigPanel = new AseCacheConfigPanel();
	private AseConfigTextPanel     _aseConfigCachePanel      = new AseConfigTextPanel(ConfigType.AseCacheConfig);
	private AseConfigTextPanel     _aseConfigHelpDbPanel     = new AseConfigTextPanel(ConfigType.AseHelpDb);
	private AseConfigTextPanel     _aseConfigHelpDevicePanel = new AseConfigTextPanel(ConfigType.AseHelpDevice);
	private AseConfigTextPanel     _aseConfigHelpServerPanel = new AseConfigTextPanel(ConfigType.AseHelpServer);
	private AseConfigTextPanel     _aseConfigTraceflagsPanel = new AseConfigTextPanel(ConfigType.AseTraceflags);
	private AseConfigTextPanel     _aseConfigSpVersionPanel  = new AseConfigTextPanel(ConfigType.AseSpVersion);
	private AseConfigTextPanel     _aseConfigShmDumpCfgPanel = new AseConfigTextPanel(ConfigType.AseShmDumpConfig);
	private AseConfigTextPanel     _aseConfigMonitorCfgPanel = new AseConfigTextPanel(ConfigType.AseMonitorConfig);
	
	private AseConfigViewDialog(Frame owner)
	{
		super(owner, "ASE Configuration", true);
		init(owner);
	}
	private AseConfigViewDialog(Dialog owner)
	{
		super(owner, "ASE Configuration", true);
		init(owner);
	}

	public static void showDialog(Frame owner)
	{
		AseConfigViewDialog dialog = new AseConfigViewDialog(owner);
		dialog.setVisible(true);
		dialog.dispose();
	}
	public static void showDialog(Dialog owner)
	{
		AseConfigViewDialog dialog = new AseConfigViewDialog(owner);
		dialog.setVisible(true);
		dialog.dispose();
	}
	public static void showDialog(Component owner)
	{
		AseConfigViewDialog dialog = null;
		if (owner instanceof Frame)
			dialog = new AseConfigViewDialog((Frame)owner);
		else if (owner instanceof Dialog)
			dialog = new AseConfigViewDialog((Dialog)owner);
		else
			dialog = new AseConfigViewDialog((Dialog)null);

		dialog.setVisible(true);
		dialog.dispose();
	}

	
	private void init(Window owner)
	{
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
//		super(_owner);
//		if (_owner != null)
//			setIconImage(_owner.getIconImage());

//		setTitle("ASE Configuration");

		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0, wrap 1","",""));   // insets Top Left Bottom Right

		JTabbedPane tabPane = new JTabbedPane();
		tabPane.add("ASE Config",       _aseConfigPanel);
		tabPane.add("Cache Config",     _aseConfigCachePanel);
		tabPane.add("sp_helpdb",        _aseConfigHelpDbPanel);
		tabPane.add("sp_helpdevice",    _aseConfigHelpDevicePanel);
		tabPane.add("sp_helpserver",    _aseConfigHelpServerPanel);
		tabPane.add("traceflags",       _aseConfigTraceflagsPanel);
		tabPane.add("sp_version",       _aseConfigSpVersionPanel);
		tabPane.add("sp_shmdumpconfig", _aseConfigShmDumpCfgPanel);
		tabPane.add("sp_monitorconfig", _aseConfigMonitorCfgPanel);

		panel.add(tabPane,               "grow, height 100%, width 100%");
		panel.add(createOkCancelPanel(), "grow, push, bottom");

		this.addWindowListener(new java.awt.event.WindowAdapter()
		{
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

		panel.add(_freeMb, "left");

		// ADD the OK, Cancel, Apply buttons
		panel.add(_ok,     "push, tag ok");
		panel.add(_cancel, "tag cancel");
//		panel.add(_apply,  "tag apply");

//		_apply.setEnabled(false);

		// Initialize some fields.
		_freeMb.setToolTipText("How much memory is available for reconfiguration, same value as you can get from sp_configure 'memory'.");
//		_freeMb.setText(AseCacheConfig.getInstance().getFreeMemoryStr());
		_freeMb.setText(((Cache) AseConfigText.getInstance(ConfigType.AseCacheConfig)).getFreeMemoryStr());

		// ADD ACTIONS TO COMPONENTS
		_ok           .addActionListener(this);
		_cancel       .addActionListener(this);
//		_apply        .addActionListener(this);

		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

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
			tmpConf.setProperty(base + "window.width", this.getSize().width);
			tmpConf.setProperty(base + "window.height", this.getSize().height);
			tmpConf.setProperty(base + "window.pos.x", this.getLocationOnScreen().x);
			tmpConf.setProperty(base + "window.pos.y", this.getLocationOnScreen().y);

			tmpConf.save();
		}
  	}

  	private void loadProps()
  	{
		int     width     = 950;  // initial window with   if not opened before
		int     height    = 700;  // initial window height if not opened before
		int     x         = -1;
		int     y         = -1;

//		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
		Configuration tmpConf = Configuration.getCombinedConfiguration();
		String base = "aseConfigViewDialog.";

		setSize(width, height);

		if (tmpConf == null)
			return;

		width  = tmpConf.getIntProperty(base + "window.width",  width);
		height = tmpConf.getIntProperty(base + "window.height", height);
		x      = tmpConf.getIntProperty(base + "window.pos.x",  -1);
		y      = tmpConf.getIntProperty(base + "window.pos.y",  -1);

		if (width != -1 && height != -1)
		{
			setSize(width, height);
		}
		if (x != -1 && y != -1)
		{
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
}

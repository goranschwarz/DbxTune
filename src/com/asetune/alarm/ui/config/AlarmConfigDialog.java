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
package com.asetune.alarm.ui.config;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import com.asetune.Version;
import com.asetune.gui.TextDialog;
import com.asetune.gui.swing.GTabbedPane;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;


public class AlarmConfigDialog
extends JDialog
implements ActionListener, TableModelListener, PropertyChangeListener
{
//	private static Logger _logger = Logger.getLogger(AlarmConfigDialog.class);
	private static final long	serialVersionUID	= -1L;

//	private Frame                  _owner           = null;

	JPanel                   _topPanel;

//	AlarmWritersTablePanel   _alarmWritersTablePanel;
//	AlarmWriterSettingsPanel _alarmWriterSettingsPanel;
//
//	AlarmTablePanel          _alarmTablePanel;
//	AlarmDetailsPanel        _alarmDetailsPanel;

	AlarmPanel               _alarmPanel;
	AlarmWritersPanel        _alarmWritersPanel;

	JPanel                   _okCancelPanelPanel;
                             
//	JSplitPane               _splitPaneAlarms;
//	JSplitPane               _splitPaneWriters;

	GTabbedPane              _tabbedPane;
	
	/** This is what will be returned from the dialog */
	Configuration            _return;

	// PANEL: OK-CANCEL
	private JButton          _ok            = new JButton("OK");
	private JButton          _cancel        = new JButton("Cancel");
	private JButton          _apply         = new JButton("Apply");
	private JButton          _previewConfig = new JButton("Preview Config");

	@SuppressWarnings("unused")
	private boolean          _madeChanges = false;
	
	private static final String DIALOG_TITLE = "Alarm Configuration and Settings";

	/*---------------------------------------------------
	** BEGIN: constructors
	**---------------------------------------------------
	*/
//	private AlarmConfigDialog(Frame owner)
//	{
//		super(owner, DIALOG_TITLE, true);
////		_owner           = owner;
//
//		initComponents();
//	}
//	private AlarmConfigDialog(Dialog owner)
//	{
//		super(owner, DIALOG_TITLE, true);
////		_owner           = owner;
//
//		initComponents();
//	}
	private AlarmConfigDialog(Window owner)
	{
		super(owner, DIALOG_TITLE, ModalityType.DOCUMENT_MODAL);
//		_owner           = owner;

		initComponents();
	}

	
	public static Configuration showDialog(Window owner)
	{
		return showDialog(owner, null);
	}

	public static Configuration showDialog(Window owner, String cmName)
	{
		AlarmConfigDialog dialog = new AlarmConfigDialog(owner);
		dialog.setLocationRelativeTo(owner);
		dialog.setSelectedCmName(cmName);
		dialog.setVisible(true);
		dialog.dispose();
//		return dialog._madeChanges;
		
		return dialog._return;
	}

//	public static boolean showDialog(Frame owner)
//	{
//		AlarmConfigDialog dialog = new AlarmConfigDialog(owner);
//		dialog.setLocationRelativeTo(owner);
//		dialog.setVisible(true);
//		dialog.dispose();
//		return dialog._madeChanges;
//	}
//	public static boolean showDialog(Dialog owner)
//	{
//		AlarmConfigDialog dialog = new AlarmConfigDialog(owner);
//		dialog.setLocationRelativeTo(owner);
//		dialog.setVisible(true);
//		dialog.dispose();
//		return dialog._madeChanges;
//	}

	/*---------------------------------------------------
	** END: constructors
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	public static ImageIcon getIcon16() { return SwingUtils.readImageIcon(Version.class, "images/alarm_view_settings_16.png"); }
	public static ImageIcon getIcon32() { return SwingUtils.readImageIcon(Version.class, "images/alarm_view_settings_32.png"); }

	private static final String TAB_NAME_WRITERS = "Alarm Writers";
	private static final String TAB_NAME_ALARMS  = "Alarm Settings";

	
	protected void initComponents()
	{
//		super(_owner);
//		if (_owner != null)
//			setIconImage(_owner.getIconImage());

//		setTitle(DIALOG_TITLE);

		// Set the icon, if we "just" do setIconImage() on the JDialog
		// it will not be the "correct" icon in the Alt-Tab list on Windows
		// So we need to grab the owner, and set that since the icon is grabbed from the owner...
		ImageIcon icon16 = getIcon16();
		ImageIcon icon32 = getIcon32();
		if (icon16 != null || icon32 != null)
		{
			ArrayList<Image> iconList = new ArrayList<Image>();
			if (icon16 != null) iconList.add(icon16.getImage());
			if (icon32 != null) iconList.add(icon32.getImage());

			setIconImages(iconList);
		}

		
		JPanel panel = new JPanel();
		//panel.setLayout(new MigLayout("debug, insets 0 0 0 0, wrap 1","",""));   // insets Top Left Bottom Right
		panel.setLayout(new MigLayout("insets 0, wrap 1","",""));   // insets Top Left Bottom Right

		_topPanel           = createTopPanel();
		
//		_alarmWriterSettingsPanel  = new AlarmWriterSettingsPanel();
//		_alarmWritersTablePanel    = new AlarmWritersTablePanel(_alarmWriterSettingsPanel);
//
//		_alarmDetailsPanel  = new AlarmDetailsPanel();
//		_alarmTablePanel    = new AlarmTablePanel(_alarmDetailsPanel);
//
//		_okCancelPanelPanel = createOkCancelPanel();
//
//		_splitPaneWriters = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
//		_splitPaneWriters.setTopComponent(_alarmWritersTablePanel);
//		_splitPaneWriters.setBottomComponent(_alarmWriterSettingsPanel);
////		_splitPaneWriters.setDividerLocation(0.5);
//
//		_splitPaneAlarms = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
//		_splitPaneAlarms.setTopComponent(_alarmTablePanel);
//		_splitPaneAlarms.setBottomComponent(_alarmDetailsPanel);
////		_splitPaneAlarms.setDividerLocation(0.5);
//
//		_tabbedPane = new GTabbedPane();
//		_tabbedPane.add(TAB_NAME_ALARMS,  createPanelWrapper(_splitPaneAlarms));
//		_tabbedPane.add(TAB_NAME_WRITERS, createPanelWrapper(_splitPaneWriters));
//
//		_tabbedPane.setIconAtTitle(TAB_NAME_ALARMS,  SwingUtils.readImageIcon(Version.class, "images/alarm_view_settings_16.png"));
//		_tabbedPane.setIconAtTitle(TAB_NAME_WRITERS, SwingUtils.readImageIcon(Version.class, "images/alarm_writer_16.png"));

		_alarmPanel        = new AlarmPanel();
		_alarmWritersPanel = new AlarmWritersPanel();

		_okCancelPanelPanel = createOkCancelPanel();

		_tabbedPane = new GTabbedPane();
		_tabbedPane.add(TAB_NAME_ALARMS,  createPanelWrapper(_alarmPanel));
		_tabbedPane.add(TAB_NAME_WRITERS, createPanelWrapper(_alarmWritersPanel));

		_tabbedPane.setIconAtTitle(TAB_NAME_ALARMS,  AlarmPanel       .getIcon16());
		_tabbedPane.setIconAtTitle(TAB_NAME_WRITERS, AlarmWritersPanel.getIcon16());

		
		
		panel.add(_topPanel,           "grow, push");
		panel.add(_tabbedPane,         "grow, push, height 100%"); // <<-- pu this in a JSplitPanel
//		panel.add(_alarmWritersTablePanel,  "grow, push");
//		panel.add(_alarmTablePanel,    "grow, push, height 100%"); // <<-- pu this in a JSplitPanel
//		panel.add(_alarmDetailsPanel,  "grow, push, height 100%"); // <<-- pu this in a JSplitPanel
//		panel.add(_splitPaneAlarms,          "grow, push, height 100%"); // <<-- pu this in a JSplitPanel
//		panel.add(_okCancelPanelPanel, "bottom, right, push");
		panel.add(_okCancelPanelPanel, "bottom, growx, pushx");

		_alarmPanel       .addPropertyChangeListener(this);
		_alarmWritersPanel.addPropertyChangeListener(this);
//		_ok.setEnabled(false); // For the moment the isDirty() functionality isn't as good as I want it to be: so let the OK be enabled all the time


		loadProps();

		setContentPane(panel);

		initComponentActions();
		
		setFocus(_cancel);
	}

	@SuppressWarnings("unused")
	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
//		System.out.println("propertyChange(): evt="+evt);
//		System.out.println("propertyChange(): evt.getPropertyName()='"+evt.getPropertyName()+"', evt.source="+evt.getSource());

		if ( evt.getPropertyName().startsWith("tableChanged") )
		{
//			System.out.println("propertyChange(): call: userInputReceived(null, null)");

			boolean alarmPanel_isDirty        = _alarmPanel.isDirty();
			boolean alarmWritersPanel_isDirty = _alarmWritersPanel.isDirty();
			
			boolean enableOk = _alarmPanel.isDirty() || _alarmWritersPanel.isDirty();
//			_ok.setEnabled(enableOk); // For the moment the isDirty() functionality isn't as good as I want it to be: so let the OK be enabled all the time

//			String newVal = evt.getNewValue()+"";
//			String oldVal = evt.getOldValue()+"";
//			System.out.println("                : enableOk="+enableOk+", alarmPanel_isDirty="+alarmPanel_isDirty+", alarmWritersPanel_isDirty="+alarmWritersPanel_isDirty+". newVal=|"+newVal+"|, oldVal=|"+oldVal+"|.");
		}
	}

	/** Simply create a JPanel that holds the passed component */
	private JPanel createPanelWrapper(Component comp)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(comp, BorderLayout.CENTER);
		return panel;
	}

	private JPanel createTopPanel()
	{
		JPanel panel = SwingUtils.createPanel("AlarmWriters", false);
//		panel.setLayout(new MigLayout("insets 0 0 0 0", "", ""));
		panel.setLayout(new MigLayout("", "", ""));

//		panel.setToolTipText("<html>" +
//				"Templates will help you to set/restore values in the below table...<br>" +
//				"<UL>" +
//				"<li> To <b>Load</b> a template, just choose one from the drop down list. </li>" +
//				"<li> To <b>Save</b> current settings as a template, just press the 'Save..' button and choose a name to save as. </li>" +
//				"<li> To <b>Remove</b> a template, just press the 'Remove..' button and button and choose a template name to deleted. </li>" +
//				"</UL>" +
//				"If all selected values in the table is matching a template, that template name will be displayed in the drop down list.<br>" +
//				"<br>" +
//				"<b>Note:</b> User Defined Counters is <b>not</b> saved/restored in the templates, only <i>System Performance Counters</i><br>" +
//				"</html>");

		return panel;
	}

	private JPanel createOkCancelPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

		_previewConfig.setToolTipText("Show current configuration in a text dialog. This to view/copy current config properties...");

		// ADD the OK, Cancel, Apply buttons
		panel.add(_previewConfig, "");
		panel.add(new JLabel(),   "growx, pushx");
		panel.add(_ok,            "tag ok, right");
		panel.add(_cancel,        "tag cancel");
		panel.add(_apply,         "tag apply, hidemode 3");

		_apply.setEnabled(false);
		_apply.setVisible(false); // LETS NOT USE THIS FOR THE MOMENT

		// ADD ACTIONS TO COMPONENTS
		_previewConfig.addActionListener(this);
		_ok           .addActionListener(this);
		_cancel       .addActionListener(this);
		_apply        .addActionListener(this);

		return panel;
	}

	private void initComponentActions()
	{
		//---- Top PANEL -----

		//---- Tab PANEL -----

		this.addWindowListener(new java.awt.event.WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				saveProps();
			}
		});
	}
	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/



	/*---------------------------------------------------
	** BEGIN: Action Listeners, and helper methods for it
	**---------------------------------------------------
	*/
	@Override
	public void actionPerformed(ActionEvent e)
    {
		Object source = e.getSource();

		// --- BUTTON: PREVIEW CONFIG ---
		if (_previewConfig.equals(source))
		{
			previewConfig();
		}

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			_return = null;
			setVisible(false);
		}

		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			doApply();
			saveProps();
			setVisible(false);
		}

		// --- BUTTON: APPLY ---
		if (_apply.equals(source))
		{
			doApply();
			saveProps();
		}
    }

	@Override
	public void tableChanged(TableModelEvent e)
	{
//		System.out.println("tableChanged(): TableModelEvent="+e);
		_apply.setEnabled(true);
	}
	/*---------------------------------------------------
	** END: Action Listeners
	**---------------------------------------------------
	*/

	private void setSelectedCmName(String cmName)
	{
		if (StringUtil.isNullOrBlank(cmName))
			return;

		_alarmPanel.setSelectedCmName(cmName);
	}

	private void previewConfig()
	{
		Configuration conf = new Configuration();

		Configuration alarmConfig   = _alarmPanel       .getConfig();
		Configuration writersConfig = _alarmWritersPanel.getConfig();

		conf.add(alarmConfig);
		conf.add(writersConfig);

		try
		{
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			PrintStream ps = new PrintStream(baos, true, "utf-8");
//
//			conf.print(ps, "AlarmConfigDialog.previewConfig(): config:");
//
//			String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
//			ps.close();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			conf.store(baos, "Alarm Configuration Preview");
			String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
			baos.close();

			
			TextDialog dialog = new TextDialog(null, "Config Preview", SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE, content);
			dialog.setVisible(true);
		}
		catch (IOException ex)
		{
			SwingUtils.showErrorMessage(this, "Error when Preview Config", "Problems viewing the configuration", ex);
		}
	}
	private void doApply()
	{
		
		Configuration conf = new Configuration();

		Configuration alarmConfig   = _alarmPanel       .getConfig();
		Configuration writersConfig = _alarmWritersPanel.getConfig();

		conf.add(alarmConfig);
		conf.add(writersConfig);

//conf.print(System.out, "AlarmConfigDialog.doApply(): config:");
		
		_return = conf;
		
//		TableModel tm = _alarmTable.getModel();
//		AlarmTableModel tm = (AlarmTableModel)_alarmTable.getModel();

//		for (int r=0; r<tm.getRowCount(); r++)
//		{
//			String  tabName      = (String)  tm.getValueAt(r, TAB_POS_TAB_NAME);
//
//			int     queryTimeout = ((Integer) tm.getValueAt(r, TAB_POS_QUERY_TIMEOUT)).intValue();
//			int     postpone     = ((Integer) tm.getValueAt(r, TAB_POS_POSTPONE)).intValue();
//			boolean paused       = ((Boolean) tm.getValueAt(r, TAB_POS_PAUSED)).booleanValue();
//			boolean bgPoll       = ((Boolean) tm.getValueAt(r, TAB_POS_BG)).booleanValue();
//			boolean rnc20        = ((Boolean) tm.getValueAt(r, TAB_POS_RNC20)).booleanValue();
//
//			boolean storePcs     = ((Boolean) tm.getValueAt(r, TAB_POS_STORE_PCS)).booleanValue();
//			boolean storeAbs     = ((Boolean) tm.getValueAt(r, TAB_POS_STORE_ABS)).booleanValue();
//			boolean storeDiff    = ((Boolean) tm.getValueAt(r, TAB_POS_STORE_DIFF)).booleanValue();
//			boolean storeRate    = ((Boolean) tm.getValueAt(r, TAB_POS_STORE_RATE)).booleanValue();
//
////			CountersModel cm  = GetCounters.getInstance().getCmByDisplayName(tabName);
//			CountersModel cm  = CounterController.getInstance().getCmByDisplayName(tabName);
//			
//			if (cm == null)
//			{
//				_logger.warn("The cm named '"+tabName+"' can't be found in the 'GetCounters' object.");
//				continue;
//			}
//
//			if (_logger.isDebugEnabled())
//			{
//				String debugStr = "doApply() name="+StringUtil.left("'"+tabName+"'", 30) +
//					" "+CounterSetTemplates.PROPKEY_queryTimeout+"="+(tm.isCellChanged(r, TAB_POS_QUERY_TIMEOUT) ? "X":" ") +
//					" "+CounterSetTemplates.PROPKEY_postpone    +"="+(tm.isCellChanged(r, TAB_POS_POSTPONE     ) ? "X":" ") +
//					" "+CounterSetTemplates.PROPKEY_paused      +"="+(tm.isCellChanged(r, TAB_POS_PAUSED       ) ? "X":" ") +
//					" "+CounterSetTemplates.PROPKEY_bg          +"="+(tm.isCellChanged(r, TAB_POS_BG           ) ? "X":" ") +
//					" "+CounterSetTemplates.PROPKEY_resetNC20   +"="+(tm.isCellChanged(r, TAB_POS_RNC20        ) ? "X":" ") +
//					" "+CounterSetTemplates.PROPKEY_storePcs    +"="+(tm.isCellChanged(r, TAB_POS_STORE_PCS    ) ? "X":" ") +
//					" "+CounterSetTemplates.PROPKEY_pcsAbs      +"="+(tm.isCellChanged(r, TAB_POS_STORE_ABS    ) ? "X":" ") +
//					" "+CounterSetTemplates.PROPKEY_pcsDiff     +"="+(tm.isCellChanged(r, TAB_POS_STORE_DIFF   ) ? "X":" ") +
//					" "+CounterSetTemplates.PROPKEY_pcsRate     +"="+(tm.isCellChanged(r, TAB_POS_STORE_RATE   ) ? "X":" ");
//				_logger.debug(debugStr);
//			}
//
//			if (tm.isCellChanged(r, TAB_POS_QUERY_TIMEOUT)) cm.setQueryTimeout(                 queryTimeout, true);
//			if (tm.isCellChanged(r, TAB_POS_POSTPONE     )) cm.setPostponeTime(                 postpone,   true);
//			if (tm.isCellChanged(r, TAB_POS_PAUSED       )) cm.setPauseDataPolling(             paused,     true);
//			if (tm.isCellChanged(r, TAB_POS_BG           )) cm.setBackgroundDataPollingEnabled( bgPoll,     true);
//			if (tm.isCellChanged(r, TAB_POS_RNC20        )) cm.setNegativeDiffCountersToZero(   rnc20,      true);
//
//			if (tm.isCellChanged(r, TAB_POS_STORE_PCS    )) cm.setPersistCounters(    storePcs,  true);
//			if (tm.isCellChanged(r, TAB_POS_STORE_ABS    )) cm.setPersistCountersAbs( storeAbs,  true);
//			if (tm.isCellChanged(r, TAB_POS_STORE_DIFF   )) cm.setPersistCountersDiff(storeDiff, true);
//			if (tm.isCellChanged(r, TAB_POS_STORE_RATE   )) cm.setPersistCountersRate(storeRate, true);
//		}

		_madeChanges = true;

//		_alarmTable.resetCellChanges();
		_apply.setEnabled(false);
	}

	private void setFocus(final JComponent comp)
	{
		// The components needs to be visible for the requestFocus()
		// to work, so lets the EventThreda do it for us after the windows is visible.
		Runnable deferredAction = new Runnable()
		{
			@Override
			public void run()
			{
				comp.requestFocus();
			}
		};
		SwingUtilities.invokeLater(deferredAction);
	}

	/*---------------------------------------------------
	** BEGIN: Property handling
	**---------------------------------------------------
	*/
	private void saveProps()
  	{
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
//		String base = "AlarmConfigDialog.";  // this.getClass().getSimpleName()+"."
		String base = this.getClass().getSimpleName() + ".";

		if (tmpConf != null)
		{
			tmpConf.setLayoutProperty(base + "window.width",  this.getSize().width);
			tmpConf.setLayoutProperty(base + "window.height", this.getSize().height);
			tmpConf.setLayoutProperty(base + "window.pos.x",  this.getLocationOnScreen().x);
			tmpConf.setLayoutProperty(base + "window.pos.y",  this.getLocationOnScreen().y);

			tmpConf.setLayoutProperty(base + "window.splitPane.alarms.div.location",  _alarmPanel       .getDividerLocation());
			tmpConf.setLayoutProperty(base + "window.splitPane.writers.div.location", _alarmWritersPanel.getDividerLocation());
			
			tmpConf.save();
		}
  	}

  	private void loadProps()
  	{
		int     width     = 950;  // initial window with   if not opened before
		int     height    = 800;  // initial window height if not opened before
		int     x         = -1;
		int     y         = -1;
		int     splitDivLoc;

		Configuration tmpConf = Configuration.getCombinedConfiguration();
//		String base = "AlarmConfigDialog.";  // this.getClass().getSimpleName()+"."
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
		
		
		splitDivLoc = tmpConf.getLayoutProperty(base + "window.splitPane.alarms.div.location", 250);
		if (splitDivLoc > 0)
			_alarmPanel.setDividerLocation(splitDivLoc);

		splitDivLoc = tmpConf.getLayoutProperty(base + "window.splitPane.writers.div.location", 250);
		if (splitDivLoc > 0)
			_alarmWritersPanel.setDividerLocation(splitDivLoc);
	}
	/*---------------------------------------------------
	** END: Property handling
	**---------------------------------------------------
	*/





	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//// MAIN & TEST - MAIN & TEST - MAIN & TEST - MAIN & TEST ///
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////

//	public static void main(String[] args)
//	{
//		Properties log4jProps = new Properties();
//		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
//		//log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
//		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
//		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
//		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
//		PropertyConfigurator.configure(log4jProps);
//
//		// set native L&F
//		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
//		catch (Exception e) {}
//
//
//		Configuration conf = new Configuration("c:\\OfflineSessionsViewer.tmp.deleteme.properties");
//		Configuration.setInstance(Configuration.USER_TEMP, conf);
//
//		MainFrame frame = new MainFrameAse();
//
//		// Create and Start the "collector" thread
//		GetCounters getCnt = new GetCountersGui();
//		CounterController.setInstance(getCnt);
//		try
//		{
//			getCnt.init();
//		}
//		catch (Exception e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
////		getCnt.start();
//
//		frame.pack();
//
//		TcpConfigDialog.showDialog(frame);
//	}
}

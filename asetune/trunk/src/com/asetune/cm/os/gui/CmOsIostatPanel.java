/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.cm.os.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.asetune.cm.CounterTableModel;
import com.asetune.cm.CountersModel;
import com.asetune.cm.os.CmOsIostat;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.hostmon.HostMonitor;
import com.asetune.hostmon.MonitorIoLinux;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmOsIostatPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmOsIostatPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmOsIostat.CM_NAME;

	public CmOsIostatPanel(CountersModel cm)
	{
		super(cm);

//		if (cm.getIconFile() != null)
//			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
	}

	private JLabel     _hostmonThreadNotInit_lbl;
	private JLabel     _hostmonThreadIsRunning_lbl;
	private JLabel     _hostmonThreadIsStopped_lbl;
	private JLabel     _hostmonHostname_lbl;
	private JButton    _hostmonStart_but;
	private JButton    _hostmonStop_but;
	private JCheckBox  _opt_N_chk;
	private JCheckBox  _excludeDevices_chk;
	private JTextField _excludeDevicesRegExp_txt;
	private JButton    _iostatMapping_but;
//	private IoStatDeviceMapperDialog _deviceMapperDialog = null;

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		final JPanel panel = SwingUtils.createPanel("Host Monitor", true);
		panel.setLayout(new MigLayout("ins 5, gap 0", "", "0[0]0"));
		panel.setToolTipText(
			"<html>" +
				"Use this panel to check or control the underlying Host Monitoring Thread.<br>" +
				"You can Start and/or Stop the hostmon thread.<br>" +
			"</html>");

		_hostmonThreadNotInit_lbl    = new JLabel("<html><b>Not yet initialized</b></html>");
		_hostmonThreadIsRunning_lbl  = new JLabel("<html>Is running</html>");
		_hostmonThreadIsStopped_lbl  = new JLabel("<html><b>Is stopped</b></html>");
		_hostmonHostname_lbl         = new JLabel();
		_hostmonStart_but            = new JButton("Start");
		_hostmonStop_but             = new JButton("Stop");
		_opt_N_chk                   = new JCheckBox("Use Device Mapper Name", CmOsIostat.DEFAULT_linux_opt_N);
		_excludeDevices_chk          = new JCheckBox("Exclude devices", CmOsIostat.DEFAULT_excludeDevices);
		_excludeDevicesRegExp_txt    = new JTextField(CmOsIostat.DEFAULT_excludeDevicesRegExp, 10);
		_iostatMapping_but           = new JButton("Device Mapping");

		_hostmonThreadNotInit_lbl  .setToolTipText("<html>Indicates whether the underlying Host Monitor Thread has not yet been initialized.</html>");
		_hostmonThreadIsRunning_lbl.setToolTipText("<html>Indicates whether the underlying Host Monitor Thread is running.</html>");
		_hostmonThreadIsStopped_lbl.setToolTipText("<html>Indicates whether the underlying Host Monitor Thread is running.</html>");
		_hostmonHostname_lbl       .setToolTipText("<html>What host name are we monitoring.</html>");
		_hostmonStart_but          .setToolTipText("<html>Start the underlying Host Monitor Thread.</html>");
		_hostmonStop_but           .setToolTipText("<html>Stop the underlying Host Monitor Thread.</html>");
		_opt_N_chk                 .setToolTipText("<html>iostat -N switch: Display the registered device mapper names for any device mapper devices.<br>"
		                                               + "<br>"
		                                               + "For more info on the -N switch, do: man iostat<br>"
		                                               + "<b>Note 1</b>: Linux Only<br>"
		                                               + "<b>Note 2</b>: You need to Restart (Stop-&gt;Start) the collector, for this to take effect...<br>"
		                                               + "</html>");
		_excludeDevices_chk        .setToolTipText("<html>Enable or Disable Exclution of device nemes by regular expressions.</html>");
		_excludeDevicesRegExp_txt  .setToolTipText("<html>RegExp to test for when excuding devices by name.</html>");
		_iostatMapping_but         .setToolTipText("<html>Open a dialog where you can map 'device name' to a more readable name, which is displaied in the column 'deviceDescription'.</html>");

		_hostmonThreadNotInit_lbl  .setVisible(true);
		_hostmonThreadIsRunning_lbl.setVisible(false);
		_hostmonThreadIsStopped_lbl.setVisible(false);
		_hostmonStart_but          .setVisible(false);
		_hostmonStop_but           .setVisible(false);
		_opt_N_chk                 .setVisible(true);
		_excludeDevices_chk        .setVisible(true);
		_excludeDevicesRegExp_txt  .setVisible(true);
		_iostatMapping_but         .setVisible(true);

		// Set initial values
		Configuration conf = Configuration.getCombinedConfiguration();
		_opt_N_chk               .setSelected(conf.getBooleanProperty(CmOsIostat.PROPKEY_linux_opt_N,          CmOsIostat.DEFAULT_linux_opt_N));
		_excludeDevices_chk      .setSelected(conf.getBooleanProperty(CmOsIostat.PROPKEY_excludeDevices,       CmOsIostat.DEFAULT_excludeDevices));
		_excludeDevicesRegExp_txt.setText(    conf.getProperty       (CmOsIostat.PROPKEY_excludeDevicesRegExp, CmOsIostat.DEFAULT_excludeDevicesRegExp));
		
		panel.add( _hostmonThreadNotInit_lbl,   "hidemode 3, wrap 10");
		panel.add( _hostmonThreadIsRunning_lbl, "hidemode 3, wrap 10");
		panel.add( _hostmonThreadIsStopped_lbl, "hidemode 3, wrap 10");
		panel.add( _hostmonHostname_lbl,        "hidemode 3, wrap 10");
		panel.add( _hostmonStart_but,           "hidemode 3, wrap");
		panel.add( _hostmonStop_but,            "hidemode 3, split");
		panel.add( _opt_N_chk,                  "hidemode 3, wrap");
		panel.add( _excludeDevices_chk,         "hidemode 3, split");
		panel.add( _excludeDevicesRegExp_txt,   "hidemode 3, pushx, growx, wrap"); 
		panel.add( _iostatMapping_but,          "hidemode 3, wrap");

		_hostmonStart_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				CountersModel cm = getCm();
				if (cm != null)
				{
					HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
					if (hostMonitor != null)
					{
						try
						{
							hostMonitor.setPaused(false);
							hostMonitor.start();
						}
						catch (Exception ex)
						{
							SwingUtils.showErrorMessage("Start", "Problems Starting the Host Monitoring Thread.", ex);
						}
					}
				}
			}
		});

		_hostmonStop_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				CountersModel cm = getCm();
				if (cm != null)
				{
					HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
					if (hostMonitor != null)
					{
						hostMonitor.setPaused(true);
						hostMonitor.shutdown();
					}
				}
			}
		});

		_opt_N_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Configuration tempConf = Configuration.getInstance(Configuration.USER_TEMP);
				if (tempConf != null)
				{
					tempConf.setProperty(CmOsIostat.PROPKEY_linux_opt_N, _opt_N_chk.isSelected());
					tempConf.save();
				}
			}
		});
		
		_excludeDevices_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Configuration tempConf = Configuration.getInstance(Configuration.USER_TEMP);
				if (tempConf != null)
				{
					tempConf.setProperty(CmOsIostat.PROPKEY_excludeDevices, _excludeDevices_chk.isSelected());
					tempConf.save();
				}
			}
		});
		
		_excludeDevicesRegExp_txt.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Configuration tempConf = Configuration.getInstance(Configuration.USER_TEMP);
				if (tempConf != null)
				{
					tempConf.setProperty(CmOsIostat.PROPKEY_excludeDevicesRegExp, _excludeDevicesRegExp_txt.getText());
					tempConf.save();
				}
			}
		});
		
		_iostatMapping_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String forHostName = null; 
				Map<String, String> dmToNameMap = new LinkedHashMap<>(); // Map: 'dm-0' -> 'nameOfLogiclaVolume'

				HostMonitor hostMonitor = (HostMonitor) getCm().getClientProperty(HostMonitor.PROPERTY_NAME);
				if (hostMonitor != null)
				{
					// What host are we connected to
					forHostName = hostMonitor.getHostname();
					
					// If Linux, list '/dev/mapper' and put the entries in the Map dmToNameMap :: 'dm-0' -> 'nameOfLogiclaVolume'
					// The Map will be used to GUESS name when adding them...
					if (hostMonitor instanceof MonitorIoLinux)
					{
						try
						{
							String dir_devMapper = hostMonitor.getConnection().execCommandOutputAsStr("ls -l /dev/mapper");

							// loop lines and get info
							Scanner scanner = new Scanner(dir_devMapper);
							while (scanner.hasNextLine()) 
							{
								String lineStr = scanner.nextLine();
								if (StringUtil.isNullOrBlank(lineStr)) // Skip empty lines
									continue;
								if (lineStr.indexOf(" -> ") == -1) // Skip lines that do not contain ' -> '
									continue;

								String[] sa = lineStr.split(" ");
								int ptrPos     = Arrays.asList(sa).indexOf("->");
								int volNamePos = ptrPos - 1;
								int dmNamePos  = ptrPos + 1;

								if (ptrPos != -1)
								{
									String volNameStr = sa[volNamePos];
									String dmNameStr  = sa[dmNamePos];
									while (dmNameStr.startsWith("../"))
										dmNameStr = dmNameStr.substring("../".length()); // Remove leading ../
									
									dmToNameMap.put(dmNameStr, volNameStr);
								}
							}
							scanner.close();
						}
						catch(Throwable ignore) {}
					}
				}

				// Add all "devices names" to a Set, which will be passed to the dialog
				Set<String> devices = new LinkedHashSet<String>();
				CounterTableModel tm = getCm().getCounterDataAbs();
				if (tm != null)
				{
    				for (int r=0; r<tm.getRowCount(); r++)
    				{
    					Object deviceName_o = tm.getValueAt(r, 0); // The device name is always the first, even if they have different name
    					if (deviceName_o != null)
    						devices.add(deviceName_o.toString());
    				}
				}

				// Open the dialog
				IoStatDeviceMapperDialog deviceMapperDialog = new IoStatDeviceMapperDialog(SwingUtilities.getWindowAncestor(panel), forHostName, devices, dmToNameMap);
				deviceMapperDialog.setVisible(true);
			}
		});

		return panel;
	}

	@Override
	public void checkLocalComponents()
	{
		CountersModel cm = getCm();
		if (cm != null)
		{
			HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
			if (hostMonitor != null)
			{
				boolean isRunning = hostMonitor.isRunning();
				boolean isPaused  = hostMonitor.isPaused();

				_hostmonThreadIsRunning_lbl.setText("<html>Command: <b>"+hostMonitor.getCommand()+"</b></html>");
				_hostmonThreadNotInit_lbl  .setVisible( false );
				_hostmonThreadIsRunning_lbl.setVisible(   isRunning );
				_hostmonThreadIsStopped_lbl.setVisible( ! isRunning );
				_hostmonHostname_lbl       .setText("<html> Host: <b>"+hostMonitor.getHostname()+"</b></html>");
				_hostmonStart_but          .setVisible( ! isRunning );
				_hostmonStop_but           .setVisible(   isRunning );

				if (isPaused)
					setWatermarkText("Warning: The host monitoring thread is Stopped/Paused!");
			}
			else
			{
				setWatermarkText("Host Monitoring is Disabled or Initializing at Next sample.\n" +
						"\n" +
						"Enabled it when you connect to the DBMS by:\n" +
						"- selecting the checkbox 'Monitor the OS Host for ...'\n" +
						"In the Options panel at the bottom.");

				_hostmonThreadNotInit_lbl  .setVisible( true );
				_hostmonThreadIsRunning_lbl.setVisible( false );
				_hostmonThreadIsStopped_lbl.setVisible( false );
				_hostmonStart_but          .setVisible( false );
				_hostmonStop_but           .setVisible( false );
				if (cm.getSampleException() != null)
					setWatermarkText(cm.getSampleException().toString());
			}
		}
	}
}

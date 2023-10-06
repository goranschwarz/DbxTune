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
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.asetune.cm.CounterModelHostMonitor;
import com.asetune.cm.CounterTableModel;
import com.asetune.cm.CountersModel;
import com.asetune.cm.os.CmOsIostat;
import com.asetune.hostmon.HostMonitor;
import com.asetune.hostmon.HostMonitor.OsVendor;
import com.asetune.hostmon.MonitorIoLinux;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class CmOsIostatPanel
extends CmOsGenericPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmOsIostatPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmOsIostat.CM_NAME;

	public CmOsIostatPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
	}

	private JCheckBox  _opt_N_chk;
	private JCheckBox  _excludeDevices_chk;
	private JTextField _excludeDevicesRegExp_txt;
	private JButton    _iostatMapping_but;
//	private IoStatDeviceMapperDialog _deviceMapperDialog = null;

	
	@Override
	public void checkLocalComponents()
	{
		// Device mapping is NOT supported on Windows... so disable the GUI Objects
		CountersModel cm = getCm();
		if (cm != null && cm instanceof CounterModelHostMonitor)
		{
			CounterModelHostMonitor hmCm = (CounterModelHostMonitor) cm;

			if (hmCm.isConnectedToVendor(OsVendor.Windows))
			{
				_opt_N_chk               .setVisible(false);
				_excludeDevices_chk      .setVisible(false);
				_excludeDevicesRegExp_txt.setVisible(false);
				_iostatMapping_but       .setVisible(false);
			}
		}

		// Call the super to do it's job...
		super.checkLocalComponents();
	}
	

	@Override
	protected JPanel configLocalOptionsPanel(JPanel panel)
	{
		if (panel instanceof LocalOptionsConfigPanel)
		{
			((LocalOptionsConfigPanel)panel).setLocalOptionsConfigChanges(new LocalOptionsConfigChanges()
			{
				@Override
				public void configWasChanged(String propName, String propVal)
				{
					Configuration conf = Configuration.getCombinedConfiguration();
					
//					list.add(new CmSettingsHelper("Exclude some devices",           PROPKEY_excludeDevices      , Boolean.class, conf.getBooleanProperty(PROPKEY_excludeDevices      , DEFAULT_excludeDevices      ), DEFAULT_excludeDevices      , "Enable/Disable: Exclude devices by name" ));
//					list.add(new CmSettingsHelper("Exclude some devices RegExp",    PROPKEY_excludeDevicesRegExp, String .class, conf.getProperty       (PROPKEY_excludeDevicesRegExp, DEFAULT_excludeDevicesRegExp), DEFAULT_excludeDevicesRegExp, "If Exclude is enabled (true), this is the regular expression to use when testing device names." ));
//					list.add(new CmSettingsHelper("iostat switch: -N (Linux Only)", PROPKEY_linux_opt_N         , Boolean.class, conf.getBooleanProperty(PROPKEY_linux_opt_N         , DEFAULT_linux_opt_N         ), DEFAULT_linux_opt_N         , "Add Switch -N to iostat 'Display the registered device mapper names for any device mapper devices'. NOTE: Linux Only" ));

					_opt_N_chk               .setSelected(conf.getBooleanProperty(CmOsIostat.PROPKEY_linux_opt_N,          CmOsIostat.DEFAULT_linux_opt_N));
					_excludeDevices_chk      .setSelected(conf.getBooleanProperty(CmOsIostat.PROPKEY_excludeDevices,       CmOsIostat.DEFAULT_excludeDevices));
					_excludeDevicesRegExp_txt.setText(    conf.getProperty       (CmOsIostat.PROPKEY_excludeDevicesRegExp, CmOsIostat.DEFAULT_excludeDevicesRegExp));
				}
			});
		}

		_opt_N_chk                   = new JCheckBox("Use Device Mapper Name", CmOsIostat.DEFAULT_linux_opt_N);
		_excludeDevices_chk          = new JCheckBox("Exclude devices", CmOsIostat.DEFAULT_excludeDevices);
		_excludeDevicesRegExp_txt    = new JTextField(CmOsIostat.DEFAULT_excludeDevicesRegExp, 10);
		_iostatMapping_but           = new JButton("Device Mapping");

		_opt_N_chk                 .setToolTipText("<html>iostat -N switch: Display the registered device mapper names for any device mapper devices.<br>"
		                                               + "<br>"
		                                               + "For more info on the -N switch, do: man iostat<br>"
		                                               + "<b>Note 1</b>: Linux Only<br>"
		                                               + "<b>Note 2</b>: You need to Restart (Stop-&gt;Start) the collector, for this to take effect...<br>"
		                                               + "</html>");
		_excludeDevices_chk        .setToolTipText("<html>Enable or Disable Exclution of device nemes by regular expressions.</html>");
		_excludeDevicesRegExp_txt  .setToolTipText("<html>RegExp to test for when excuding devices by name.</html>");
		_iostatMapping_but         .setToolTipText("<html>Open a dialog where you can map 'device name' to a more readable name, which is displaied in the column 'deviceDescription'.</html>");
		
		_opt_N_chk                 .setVisible(true);
		_excludeDevices_chk        .setVisible(true);
		_excludeDevicesRegExp_txt  .setVisible(true);
		_iostatMapping_but         .setVisible(true);

		// Set initial values
		Configuration conf = Configuration.getCombinedConfiguration();
		_opt_N_chk               .setSelected(conf.getBooleanProperty(CmOsIostat.PROPKEY_linux_opt_N,          CmOsIostat.DEFAULT_linux_opt_N));
		_excludeDevices_chk      .setSelected(conf.getBooleanProperty(CmOsIostat.PROPKEY_excludeDevices,       CmOsIostat.DEFAULT_excludeDevices));
		_excludeDevicesRegExp_txt.setText(    conf.getProperty       (CmOsIostat.PROPKEY_excludeDevicesRegExp, CmOsIostat.DEFAULT_excludeDevicesRegExp));

		panel.add( _opt_N_chk,                  "hidemode 3, wrap");
		panel.add( _excludeDevices_chk,         "hidemode 3, split");
		panel.add( _excludeDevicesRegExp_txt,   "hidemode 3, pushx, growx, wrap"); 
		panel.add( _iostatMapping_but,          "hidemode 3, wrap");

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
}

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

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.asetune.cm.CountersModel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.hostmon.HostMonitor;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmOsMeminfoPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmOsMeminfoPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmOsMeminfo.CM_NAME;

	public CmOsMeminfoPanel(CountersModel cm)
	{
		super(cm);

//		if (cm.getIconFile() != null)
//			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
	}


	JLabel  _hostmonThreadNotInit_lbl;
	JLabel  _hostmonThreadIsRunning_lbl;
//	JLabel  _hostmonThreadIsStopped_lbl;
	JLabel  _hostmonHostname_lbl;
//	JButton _hostmonStart_but;
//	JButton _hostmonStop_but;

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Host Monitor", true);
		panel.setLayout(new MigLayout("ins 5, gap 0", "", "0[0]0"));
		panel.setToolTipText(
			"<html>" +
				"Use this panel to check or control the underlying Host Monitoring Thread.<br>" +
				"You can Start and/or Stop the hostmon thread.<br>" +
			"</html>");

		_hostmonThreadNotInit_lbl    = new JLabel("<html><b>Not yet initialized</b></html>");
		_hostmonThreadIsRunning_lbl  = new JLabel("<html>Is running</html>");
//		_hostmonThreadIsStopped_lbl  = new JLabel("<html><b>Is stopped</b></html>");
		_hostmonHostname_lbl         = new JLabel();
//		_hostmonStart_but            = new JButton("Start");
//		_hostmonStop_but             = new JButton("Stop");

		_hostmonThreadNotInit_lbl  .setToolTipText("<html>Indicates whether the underlying Host Monitor Thread has not yet been initialized.</html>");
		_hostmonThreadIsRunning_lbl.setToolTipText("<html>Indicates whether the underlying Host Monitor Thread is running.</html>");
//		_hostmonThreadIsStopped_lbl.setToolTipText("<html>Indicates whether the underlying Host Monitor Thread is running.</html>");
		_hostmonHostname_lbl       .setToolTipText("<html>What host name are we monitoring.</html>");
//		_hostmonStart_but          .setToolTipText("<html>Start the underlying Host Monitor Thread.</html>");
//		_hostmonStop_but           .setToolTipText("<html>Stop the underlying Host Monitor Thread.</html>");

		_hostmonThreadNotInit_lbl  .setVisible(true);
		_hostmonThreadIsRunning_lbl.setVisible(false);
//		_hostmonThreadIsStopped_lbl.setVisible(false);
//		_hostmonStart_but          .setVisible(false);
//		_hostmonStop_but           .setVisible(false);

		panel.add( _hostmonThreadNotInit_lbl,   "hidemode 3, wrap 10");
		panel.add( _hostmonThreadIsRunning_lbl, "hidemode 3, wrap 10");
//		panel.add( _hostmonThreadIsStopped_lbl, "hidemode 3, wrap 10");
		panel.add( _hostmonHostname_lbl,        "hidemode 3, wrap 10");
//		panel.add( _hostmonStart_but,           "hidemode 3, wrap");
//		panel.add( _hostmonStop_but,            "hidemode 3, wrap");

//		_hostmonStart_but.addActionListener(new ActionListener()
//		{
//			public void actionPerformed(ActionEvent e)
//			{
//				CountersModel cm = getCm();
//				if (cm != null)
//				{
//					HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
//					if (hostMonitor != null)
//					{
//						try
//						{
//							hostMonitor.setPaused(false);
//							hostMonitor.start();
//						}
//						catch (Exception ex)
//						{
//							SwingUtils.showErrorMessage("Start", "Problems Starting the Host Monitoring Thread.", ex);
//						}
//					}
//				}
//			}
//		});
//
//		_hostmonStop_but.addActionListener(new ActionListener()
//		{
//			public void actionPerformed(ActionEvent e)
//			{
//				CountersModel cm = getCm();
//				if (cm != null)
//				{
//					HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
//					if (hostMonitor != null)
//					{
//						hostMonitor.setPaused(true);
//						hostMonitor.shutdown();
//					}
//				}
//			}
//		});
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
				_hostmonThreadIsRunning_lbl.setVisible(   isRunning || !hostMonitor.isOsCommandStreaming() );
//				_hostmonThreadIsRunning_lbl.setVisible(   isRunning );
//				_hostmonThreadIsStopped_lbl.setVisible( ! isRunning );
				_hostmonHostname_lbl       .setText("<html> Host: <b>"+hostMonitor.getHostname()+"</b></html>");
//				_hostmonStart_but          .setVisible( ! isRunning );
//				_hostmonStop_but           .setVisible(   isRunning );

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
//				_hostmonThreadIsStopped_lbl.setVisible( false );
//				_hostmonStart_but          .setVisible( false );
//				_hostmonStop_but           .setVisible( false );
				if (cm.getSampleException() != null)
					setWatermarkText(cm.getSampleException().toString());
			}
		}
	}
}

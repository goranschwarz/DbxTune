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
package com.dbxtune.cm.os.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.hostmon.HostMonitor;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmOsGenericPanel
extends CmOsAbstractPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmOsGenericPanel.class);
	private static final long    serialVersionUID      = 1L;

	public CmOsGenericPanel(CountersModel cm)
	{
		super(cm);
		localInit();
	}
	
	protected void localInit()
	{
	}

	/**
	 * If we want any implementors "hook-in" and adjust/add entries to the panel
	 * 
	 * @param panel The panel created in <code>createLocalOptionsPanel()</code>
	 * @return The panel which will be used by the "local options" (probably the same JPanel that was passed in)
	 */
	protected JPanel configLocalOptionsPanel(JPanel panel)
	{
		return panel;
	}

	private JLabel  _hostmonThreadNotInit_lbl;
	private JLabel  _hostmonThreadIsRunning_lbl;
	private JLabel  _hostmonThreadIsStopped_lbl;
	private JLabel  _hostmonHostname_lbl;
	private JButton _hostmonStart_but;
	private JButton _hostmonStop_but;

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		LocalOptionsConfigPanel panel = new LocalOptionsConfigPanel("Host Monitor", new LocalOptionsConfigChanges()
		{
			@Override
			public void configWasChanged(String propName, String propVal)
			{
			}
		});

//		JPanel panel = SwingUtils.createPanel("Host Monitor", true);
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

		_hostmonThreadNotInit_lbl  .setToolTipText("<html>Indicates whether the underlying Host Monitor Thread has not yet been initialized.</html>");
		_hostmonThreadIsRunning_lbl.setToolTipText("<html>Indicates whether the underlying Host Monitor Thread is running.</html>");
		_hostmonThreadIsStopped_lbl.setToolTipText("<html>Indicates whether the underlying Host Monitor Thread is running.</html>");
		_hostmonHostname_lbl       .setToolTipText("<html>What host name are we monitoring.</html>");
		_hostmonStart_but          .setToolTipText("<html>Start the underlying Host Monitor Thread.</html>");
		_hostmonStop_but           .setToolTipText("<html>Stop the underlying Host Monitor Thread.</html>");

		_hostmonThreadNotInit_lbl  .setVisible(true);
		_hostmonThreadIsRunning_lbl.setVisible(false);
		_hostmonThreadIsStopped_lbl.setVisible(false);
		_hostmonStart_but          .setVisible(false);
		_hostmonStop_but           .setVisible(false);

		panel.add( _hostmonThreadNotInit_lbl,   "hidemode 3, wrap 10");
		panel.add( _hostmonThreadIsRunning_lbl, "hidemode 3, wrap 10");
		panel.add( _hostmonThreadIsStopped_lbl, "hidemode 3, wrap 10");
		panel.add( _hostmonHostname_lbl,        "hidemode 3, wrap 10");
		panel.add( _hostmonStart_but,           "hidemode 3, wrap");
		panel.add( _hostmonStop_but,            "hidemode 3, wrap");

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
		
		return configLocalOptionsPanel(panel);
	}

	@Override
	public void checkLocalComponents()
	{
		CountersModel cm = getCm();
		if (cm != null)
		{
			// This sets how many decimals we should show. (For Windows, we will show 6 decimals since PerfMon Counters has that)
			setTableCellRenders();

			HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
			if (hostMonitor != null)
			{
				boolean isStreamingCmd = hostMonitor.isOsCommandStreaming();
				boolean isRunning      = hostMonitor.isRunning();
				boolean isPaused       = hostMonitor.isPaused();

				_hostmonThreadIsRunning_lbl.setText("<html>Command: <b>" + hostMonitor.getCommand() + "</b></html>");
				_hostmonThreadNotInit_lbl  .setVisible( false );
				_hostmonThreadIsRunning_lbl.setVisible(   isRunning || !isStreamingCmd);
				_hostmonThreadIsStopped_lbl.setVisible( ! isRunning && isStreamingCmd);
				_hostmonHostname_lbl       .setText("<html> Host: <b>" + hostMonitor.getHostname() + "</b></html>");
				_hostmonStart_but          .setVisible( ! isRunning && isStreamingCmd);
				_hostmonStop_but           .setVisible(   isRunning && isStreamingCmd);

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

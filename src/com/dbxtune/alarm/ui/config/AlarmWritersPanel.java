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
package com.dbxtune.alarm.ui.config;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.dbxtune.Version;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

public class AlarmWritersPanel
extends JPanel
implements PropertyChangeListener
{
	private static final long serialVersionUID = 1L;

	AlarmWritersTablePanel   _alarmWritersTablePanel;
//	AlarmWriterSettingsPanel _alarmWriterSettingsPanel;
	AlarmWriterDetailsPanel _alarmWriterDetailsPanel;

	JSplitPane               _splitPane;

	public static ImageIcon getIcon16() { return SwingUtils.readImageIcon(Version.class, "images/alarm_writer_16.png"); }

//	public AlarmWritersPanel()
//	{
//		super(new BorderLayout());
//
//		_alarmWriterSettingsPanel  = new AlarmWriterSettingsPanel();
//		_alarmWritersTablePanel    = new AlarmWritersTablePanel(_alarmWriterSettingsPanel);
//
//		_splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
//		_splitPane.setTopComponent(_alarmWritersTablePanel);
//		_splitPane.setBottomComponent(_alarmWriterSettingsPanel);
//		_splitPane.setDividerLocation(0.5f);
//		_splitPane.setResizeWeight   (0.5f);
//
//		add(_splitPane, BorderLayout.CENTER);
//		
//		_alarmWritersTablePanel  .addPropertyChangeListener("tableChanged", this);
//		_alarmWriterSettingsPanel.addPropertyChangeListener("tableChanged", this);
//	}
	public AlarmWritersPanel()
	{
		super(new BorderLayout());

		_alarmWriterDetailsPanel  = new AlarmWriterDetailsPanel();
//		_alarmWritersTablePanel   = new AlarmWritersTablePanel(_alarmWriterDetailsPanel.getAlarmWriterSettingsPanel());
		_alarmWritersTablePanel   = new AlarmWritersTablePanel(_alarmWriterDetailsPanel);

		_splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		_splitPane.setTopComponent(_alarmWritersTablePanel);
		_splitPane.setBottomComponent(_alarmWriterDetailsPanel);
		_splitPane.setDividerLocation(0.5f);
		_splitPane.setResizeWeight   (0.5f);

		add(_splitPane, BorderLayout.CENTER);
		
		_alarmWritersTablePanel .addPropertyChangeListener("tableChanged", this);
		_alarmWriterDetailsPanel.addPropertyChangeListener("tableChanged", this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		// Just "pass on" the "tableChanged" event to my listeners
		firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
	}

	public Configuration getConfig()
	{
		return _alarmWritersTablePanel.getConfig();
	}

	public String getProblem()
	{
		return _alarmWritersTablePanel.getProblem();
	}

	public int getDividerLocation() 
	{
		return _splitPane.getDividerLocation();
	}

	public void setDividerLocation(int location) 
	{
		_splitPane.setDividerLocation(location);
	}

	public boolean isDirty()
	{
		if (_alarmWritersTablePanel.isDirty())
			return true;

//		if (_alarmWriterSettingsPanel.isDirty())
		if (_alarmWriterDetailsPanel.isDirty())
			return true;
		
		return false;
	}
}

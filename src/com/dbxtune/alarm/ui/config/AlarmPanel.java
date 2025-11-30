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
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

public class AlarmPanel
extends JPanel 
implements PropertyChangeListener
{
	private static final long serialVersionUID = 1L;

	AlarmTablePanel          _alarmTablePanel;
	AlarmDetailsPanel        _alarmDetailsPanel;

	JSplitPane               _splitPane;

	public static ImageIcon getIcon16() { return SwingUtils.readImageIcon(Version.class, "images/alarm_view_settings_16.png"); }

	public AlarmPanel()
	{
		super(new BorderLayout());

		_alarmDetailsPanel  = new AlarmDetailsPanel();
		_alarmTablePanel    = new AlarmTablePanel(_alarmDetailsPanel);

		_splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		_splitPane.setTopComponent(_alarmTablePanel);
		_splitPane.setBottomComponent(_alarmDetailsPanel);
		_splitPane.setDividerLocation(0.5f);
		_splitPane.setResizeWeight   (0.5f);
		
		add(_splitPane, BorderLayout.CENTER);

		_alarmTablePanel  .addPropertyChangeListener("tableChanged", this);
		_alarmDetailsPanel.addPropertyChangeListener("tableChanged", this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		// Just "pass on" the "tableChanged" event to my listeners
		firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());

		// Recheck values in the table...
		if ("tableChanged".equals(evt.getPropertyName()) && "javaEditor".equals(evt.getOldValue()))
		{
			_alarmTablePanel.fireUserDefinedAlarmMayHaveChanged(evt.getNewValue()+"");
		}
	}

	public Configuration getConfig()
	{
		return _alarmTablePanel.getConfig();
	}

	public String getProblem()
	{
		return _alarmTablePanel.getProblem();
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
		if (_alarmTablePanel.isDirty())
			return true;

		if (_alarmDetailsPanel.isDirty())
			return true;
		
		return false;
	}

	public void setSelectedCmName(String cmName)
	{
		if (StringUtil.isNullOrBlank(cmName))
			return;

		_alarmTablePanel.setSelectedCmName(cmName);
	}
}

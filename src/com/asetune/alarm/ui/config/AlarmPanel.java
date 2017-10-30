package com.asetune.alarm.ui.config;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.asetune.Version;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

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
}

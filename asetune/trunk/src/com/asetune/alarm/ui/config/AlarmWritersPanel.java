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

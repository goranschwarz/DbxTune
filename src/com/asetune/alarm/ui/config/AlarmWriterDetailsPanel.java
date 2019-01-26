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
package com.asetune.alarm.ui.config;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;

import com.asetune.Version;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.gui.swing.GTabbedPane;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class AlarmWriterDetailsPanel
extends JPanel
implements PropertyChangeListener
{
	private static final long serialVersionUID = 1L;

	GTabbedPane _alarmWriterDetailedTabbedPanel;

//	AlarmDetailSystemPanel      _alarmWriterSettingsPanel;
//	AlarmDetailUserDefinedPanel _alarmWriterFiltersPanel;
	AlarmWriterSettingsPanel _alarmWriterSettingsPanel;
	AlarmWriterFiltersPanel  _alarmWriterFiltersPanel;
	
//	String _currentWriterClassName = "";

	private static final ImageIcon hasSettings_ico = SwingUtils.readImageIcon(Version.class, "images/alarm_details_has_system_settings.png");
	private static final ImageIcon hasFilters_ico  = SwingUtils.readImageIcon(Version.class, "images/alarm_details_has_userdefined_settings.png");
	private static final ImageIcon noSettings_ico  = SwingUtils.readImageIcon(Version.class, "images/alarm_details_no_system_settings.png");
	private static final ImageIcon noFilters_ico   = SwingUtils.readImageIcon(Version.class, "images/alarm_details_no_userdefined_settings.png");

	private static final String TAB_NAME_SETTINGS = "Settings";
	private static final String TAB_NAME_FILTERS  = "Filters";

	public AlarmWriterDetailsPanel()
	{
		super();
		
		Border border = BorderFactory.createTitledBorder("Alarm Writer Details");
		setBorder(border);

		setLayout(new MigLayout("insets 0 0 0 0", "", ""));

		_alarmWriterDetailedTabbedPanel = new GTabbedPane();
		
		_alarmWriterSettingsPanel = new AlarmWriterSettingsPanel();
		_alarmWriterFiltersPanel  = new AlarmWriterFiltersPanel();

		_alarmWriterDetailedTabbedPanel.add(TAB_NAME_SETTINGS, _alarmWriterSettingsPanel);
		_alarmWriterDetailedTabbedPanel.add(TAB_NAME_FILTERS,  _alarmWriterFiltersPanel);

		String desc = "<html>Below you can see Alarm Writer Details for the above <b>selected</b> Writer</html>";

		add(new JLabel(desc),                "wrap");
		add(_alarmWriterDetailedTabbedPanel, "push, grow, wrap");

		_alarmWriterSettingsPanel.addPropertyChangeListener("tableChanged", this);
		_alarmWriterFiltersPanel .addPropertyChangeListener("tableChanged", this);
	}

//	public AlarmWriterSettingsPanel getAlarmWriterSettingsPanel()
//	{
//		return _alarmWriterSettingsPanel;
//	}
//	public AlarmWriterFiltersPanel getAlarmWriterFiltersPanel()
//	{
//		return _alarmWriterFiltersPanel;
//	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		// Just "pass on" the "tableChanged" event to my listeners
		firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
	}

	public void setWriterSettings(List<CmSettingsHelper> settings, String writerClassName)
	{
		// Check if any "settings" has been changed in the underlying panels...
		// If so: Alert
		if (_alarmWriterSettingsPanel.isDirty())
		{
			String htmlMsg = "<html>"
					+ "You have made changes without saving<br>"
					+ "<br>"
					+ "Do you want to save changed settings in"
					+ "<ul>"
					+ (_alarmWriterSettingsPanel.isDirty() ? "  <li>Settings</li>" : "")
					+ "</ul>"
					+ "</html>";
			int answer = JOptionPane.showConfirmDialog(this, htmlMsg, "Save Changes", JOptionPane.YES_NO_OPTION);
			if (answer == 0) 
			{
				if (_alarmWriterSettingsPanel.isDirty()) _alarmWriterSettingsPanel.save();
			}
		}
		
		// Show current settings in the panels
		_alarmWriterSettingsPanel.setWriterSettings(settings, writerClassName);

		// Set icon of TAB
		_alarmWriterDetailedTabbedPanel.setIconAtTitle(TAB_NAME_SETTINGS, _alarmWriterSettingsPanel.hasData() && _alarmWriterSettingsPanel.hasCheckedRows() ? hasSettings_ico : noSettings_ico);
		_alarmWriterDetailedTabbedPanel.setIconAtTitle(TAB_NAME_FILTERS,  _alarmWriterFiltersPanel .hasData() && _alarmWriterFiltersPanel .hasCheckedRows() ? hasFilters_ico  : noFilters_ico);
	}
	
//	public void setWriterClassName(String writerClassName)
//	{
//		_currentWriterClassName = writerClassName;
//	}
//	public String getWriterClassName()
//	{
//		return _currentWriterClassName;
//	}

	public void setWriterFilters(List<CmSettingsHelper> filters, String writerClassName)
	{
		// Check if any "settings" has been changed in the underlying panels...
		// If so: Alert
		if (_alarmWriterFiltersPanel.isDirty())
		{
			String htmlMsg = "<html>"
					+ "You have made changes without saving<br>"
					+ "<br>"
					+ "Do you want to save changed settings in"
					+ "<ul>"
					+ (_alarmWriterFiltersPanel.isDirty() ? "  <li>Filters</li>" : "")
					+ "</ul>"
					+ "</html>";
			int answer = JOptionPane.showConfirmDialog(this, htmlMsg, "Save Changes", JOptionPane.YES_NO_OPTION);
			if (answer == 0) 
			{
				if (_alarmWriterFiltersPanel.isDirty()) _alarmWriterFiltersPanel.save();
			}
		}
		
		// Show current settings in the panels
		_alarmWriterFiltersPanel.setWriterFilters(filters, writerClassName);

		// Set icon of TAB
		_alarmWriterDetailedTabbedPanel.setIconAtTitle(TAB_NAME_SETTINGS, _alarmWriterSettingsPanel.hasData() && _alarmWriterSettingsPanel.hasCheckedRows() ? hasSettings_ico : noSettings_ico);
		_alarmWriterDetailedTabbedPanel.setIconAtTitle(TAB_NAME_FILTERS,  _alarmWriterFiltersPanel .hasData() && _alarmWriterFiltersPanel .hasCheckedRows() ? hasFilters_ico  : noFilters_ico);
	}
	
//	public void setAlarmEntry(AlarmEntry alarmEntry)
//	{
//		// Check if any "settings" has been changed in the underlying panels...
//		// If so: Alert
//		if (_alarmWriterFiltersPanel.isDirty())
//		{
//			String htmlMsg = "<html>"
//					+ "You have made changes without saving<br>"
//					+ "<br>"
//					+ "Do you want to save changed settings in"
//					+ "<ul>"
//					+ (_alarmWriterFiltersPanel.isDirty() ? "  <li>Filters</li>" : "")
//					+ "</ul>"
//					+ "</html>";
//			int answer = JOptionPane.showConfirmDialog(this, htmlMsg, "Save Changes", JOptionPane.YES_NO_OPTION);
//			if (answer == 0) 
//			{
//				if (_alarmWriterFiltersPanel.isDirty()) _alarmWriterFiltersPanel.save();
//			}
//		}
//		
//		// Show current settings in the panels
////		_alarmWriterSettingsPanel     .setCm(cm);
////		_alarmWriterFiltersPanel.setCm(cm);
//		_alarmWriterSettingsPanel.setAlarmEntry(alarmEntry);
//		_alarmWriterFiltersPanel .setAlarmEntry(alarmEntry);
//
//		// Set icon of TAB
//		_alarmWriterDetailedTabbedPanel.setIconAtTitle(TAB_NAME_SETTINGS,      _alarmWriterSettingsPanel     .hasData() ? hasSettings_ico      : noSettings_ico);
//		_alarmWriterDetailedTabbedPanel.setIconAtTitle(TAB_NAME_FILTERS, _alarmWriterFiltersPanel.hasData() ? hasFilters_ico : noFilters_ico);
//	}

//	public void setCm(CountersModel cm)
//	{
//		// Check if any "settings" has been changed in the underlying panels...
//		// If so: Alert
//		if (_alarmWriterSettingsPanel.isDirty() || _alarmWriterFiltersPanel.isDirty())
//		{
//			String htmlMsg = "<html>"
//					+ "You have made changes without saving<br>"
//					+ "<br>"
//					+ "Do you want to save changed settings in"
//					+ "<ul>"
//					+ (_alarmWriterSettingsPanel     .isDirty() ? "  <li>System Alarms</li>"       : "")
//					+ (_alarmWriterFiltersPanel.isDirty() ? "  <li>User Defined Alarms</li>" : "")
//					+ "</ul>"
//					+ "</html>";
//			int answer = JOptionPane.showConfirmDialog(this, htmlMsg, "Save Changes", JOptionPane.YES_NO_OPTION);
//			if (answer == 0) 
//			{
//				if (_alarmWriterSettingsPanel     .isDirty()) _alarmWriterSettingsPanel     .save();
//				if (_alarmWriterFiltersPanel.isDirty()) _alarmWriterFiltersPanel.save();
//			}
//		}
//		
//		// Show current settings in the panels
//		_alarmWriterSettingsPanel     .setCm(cm);
//		_alarmWriterFiltersPanel.setCm(cm);
//
//		// Set icon of TAB
//		_alarmWriterDetailedTabbedPanel.setIconAtTitle(TAB_NAME_SETTINGS,      _alarmWriterSettingsPanel     .hasData() ? hasSettings_ico      : noSettings_ico);
//		_alarmWriterDetailedTabbedPanel.setIconAtTitle(TAB_NAME_FILTERS, _alarmWriterFiltersPanel.hasData() ? hasFilters_ico : noFilters_ico);
//	}

	/**
	 * Check if we have changed anything
	 * @return
	 */
	public boolean isDirty()
	{
		return _alarmWriterSettingsPanel.isDirty() || _alarmWriterFiltersPanel.isDirty();
	}

	public String getProblem()
	{
		return null;
//		String systemDetailsProblem = _alarmWriterSettingsPanel     .getProblem()
//		String userDefinedProblem   = _alarmWriterFiltersPanel.getProblem();

//		if ( ! _warning_lbl.isVisible() )
//			return null;
//		return _warning_lbl.getText();
	}

	public Configuration getConfig()
	{
		// TODO Auto-generated method stub
		return null;
	}
}

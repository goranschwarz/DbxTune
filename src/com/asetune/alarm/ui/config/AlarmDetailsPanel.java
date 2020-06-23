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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;

import com.asetune.Version;
import com.asetune.alarm.ui.config.AlarmTableModel.AlarmEntry;
import com.asetune.gui.swing.GTabbedPane;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class AlarmDetailsPanel
extends JPanel
implements PropertyChangeListener
{
	private static final long serialVersionUID = 1L;

	GTabbedPane _alarmDetailedTabbedPanel;

	AlarmDetailSystemPanel      _alarmDetailSystemPanel;
	AlarmDetailUserDefinedPanel _alarmDetailUserDefinedPanel;

	private static final ImageIcon hasSystemSettings_ico      = SwingUtils.readImageIcon(Version.class, "images/alarm_details_has_system_settings.png");
	private static final ImageIcon hasUserDefinedSettings_ico = SwingUtils.readImageIcon(Version.class, "images/alarm_details_has_userdefined_settings.png");
	private static final ImageIcon noSystemSettings_ico       = SwingUtils.readImageIcon(Version.class, "images/alarm_details_no_system_settings.png");
	private static final ImageIcon noUserDefinedSettings_ico  = SwingUtils.readImageIcon(Version.class, "images/alarm_details_no_userdefined_settings.png");

	private static final String TAB_NAME_SYSTEM      = "System Alarms";
	private static final String TAB_NAME_USERDEFINED = "User Defined Alarms";

	public AlarmDetailsPanel()
	{
		super();
		
		Border border = BorderFactory.createTitledBorder("Alarm Details");
		setBorder(border);

		setLayout(new MigLayout("insets 0 0 0 0", "", ""));

		_alarmDetailedTabbedPanel = new GTabbedPane();
		
		_alarmDetailSystemPanel      = new AlarmDetailSystemPanel();
		_alarmDetailUserDefinedPanel = new AlarmDetailUserDefinedPanel();

		_alarmDetailedTabbedPanel.add(TAB_NAME_SYSTEM,      _alarmDetailSystemPanel);
		_alarmDetailedTabbedPanel.add(TAB_NAME_USERDEFINED, _alarmDetailUserDefinedPanel);

		String desc = "<html>Below you can see Alarm Details for the above <b>selected</b> Counter Collector (CM)</html>";

		add(new JLabel(desc),          "wrap");
		add(_alarmDetailedTabbedPanel, "push, grow, wrap");

		_alarmDetailSystemPanel     .addPropertyChangeListener("tableChanged", this);
		_alarmDetailUserDefinedPanel.addPropertyChangeListener("tableChanged", this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		// Just "pass on" the "tableChanged" event to my listeners
		firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
	}

	public void setAlarmEntry(AlarmEntry alarmEntry)
	{
		// Check if any "settings" has been changed in the underlying panels...
		// If so: Alert
		if (_alarmDetailUserDefinedPanel.isDirty())
		{
			String htmlMsg = "<html>"
					+ "You have made changes without saving<br>"
					+ "<br>"
					+ "Do you want to save changed settings in"
					+ "<ul>"
					+ (_alarmDetailUserDefinedPanel.isDirty() ? "  <li>User Defined Alarms</li>" : "")
					+ "</ul>"
					+ "</html>";
			int answer = JOptionPane.showConfirmDialog(this, htmlMsg, "Save Changes", JOptionPane.YES_NO_OPTION);
			if (answer == 0) 
			{
				if (_alarmDetailUserDefinedPanel.isDirty()) _alarmDetailUserDefinedPanel.save();
			}
		}
		
		// Show current settings in the panels
//		_alarmDetailSystemPanel     .setCm(cm);
//		_alarmDetailUserDefinedPanel.setCm(cm);
		_alarmDetailSystemPanel     .setAlarmEntry(alarmEntry);
		_alarmDetailUserDefinedPanel.setAlarmEntry(alarmEntry);

		// Set icon of TAB
		_alarmDetailedTabbedPanel.setIconAtTitle(TAB_NAME_SYSTEM,      _alarmDetailSystemPanel     .hasData() ? hasSystemSettings_ico      : noSystemSettings_ico);
		_alarmDetailedTabbedPanel.setIconAtTitle(TAB_NAME_USERDEFINED, _alarmDetailUserDefinedPanel.hasData() ? hasUserDefinedSettings_ico : noUserDefinedSettings_ico);
	}
//	public void setCm(CountersModel cm)
//	{
//		// Check if any "settings" has been changed in the underlying panels...
//		// If so: Alert
//		if (_alarmDetailSystemPanel.isDirty() || _alarmDetailUserDefinedPanel.isDirty())
//		{
//			String htmlMsg = "<html>"
//					+ "You have made changes without saving<br>"
//					+ "<br>"
//					+ "Do you want to save changed settings in"
//					+ "<ul>"
//					+ (_alarmDetailSystemPanel     .isDirty() ? "  <li>System Alarms</li>"       : "")
//					+ (_alarmDetailUserDefinedPanel.isDirty() ? "  <li>User Defined Alarms</li>" : "")
//					+ "</ul>"
//					+ "</html>";
//			int answer = JOptionPane.showConfirmDialog(this, htmlMsg, "Save Changes", JOptionPane.YES_NO_OPTION);
//			if (answer == 0) 
//			{
//				if (_alarmDetailSystemPanel     .isDirty()) _alarmDetailSystemPanel     .save();
//				if (_alarmDetailUserDefinedPanel.isDirty()) _alarmDetailUserDefinedPanel.save();
//			}
//		}
//		
//		// Show current settings in the panels
//		_alarmDetailSystemPanel     .setCm(cm);
//		_alarmDetailUserDefinedPanel.setCm(cm);
//
//		// Set icon of TAB
//		_alarmDetailedTabbedPanel.setIconAtTitle(TAB_NAME_SYSTEM,      _alarmDetailSystemPanel     .hasData() ? hasSystemSettings_ico      : noSystemSettings_ico);
//		_alarmDetailedTabbedPanel.setIconAtTitle(TAB_NAME_USERDEFINED, _alarmDetailUserDefinedPanel.hasData() ? hasUserDefinedSettings_ico : noUserDefinedSettings_ico);
//	}

	/**
	 * Check if we have changed anything
	 * @return
	 */
	public boolean isDirty()
	{
		return _alarmDetailSystemPanel.isDirty() || _alarmDetailUserDefinedPanel.isDirty();
	}

	public String getProblem()
	{
		return null;
//		String systemDetailsProblem = _alarmDetailSystemPanel     .getProblem()
//		String userDefinedProblem   = _alarmDetailUserDefinedPanel.getProblem();

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

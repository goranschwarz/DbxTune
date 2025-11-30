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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.JLabel;

import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardPage;
import org.netbeans.spi.wizard.WizardPanelNavResult;

import com.dbxtune.alarm.ui.config.AlarmPanel;
import com.dbxtune.gui.swing.MultiLineLabel;

import net.miginfocom.swing.MigLayout;


public class WizardOfflinePage9
extends WizardPage
implements ActionListener
{
    private static final long serialVersionUID = 1L;
	private static final String WIZ_NAME = "alarmConfig";
	private static final String WIZ_DESC = "Alarm Configuration";
	private static final String WIZ_HELP = "Configure what Alarms you want to enable, and it's threshold values.";

	private boolean    _enableAlarmHandling;
	private JLabel     _notEnabled_lbl          = new JLabel("Alarm Writers isn't enabled, so this page can be skipped.");

	private AlarmPanel _alarmPanel;

	public static String getDescription() { return WIZ_DESC; }
	@Override
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	public WizardOfflinePage9()
	{
		super(WIZ_NAME, WIZ_DESC);

		_alarmPanel = new AlarmPanel();

		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		// Add a helptext
		add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );

		add(_notEnabled_lbl,          "hidemode 3, wrap");
		add(_alarmPanel,              "push, grow, hidemode 3, wrap");

		initData();
	}

	private void initData()
	{
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
	}

	/** Called when we enter the page */
	@Override
	protected void renderingPage()
    {
//		String val = (String) getWizardData("to-be-discarded.enableAlarmHandling");
		String val = getWizardData("to-be-discarded.enableAlarmHandling") + "";
		_enableAlarmHandling = (val != null && val.trim().equalsIgnoreCase("true"));

		_notEnabled_lbl.setVisible(false);
		_alarmPanel    .setVisible(true);

		if ( ! _enableAlarmHandling )
		{
			_notEnabled_lbl.setVisible(true);
			_alarmPanel    .setVisible(false);
		}
    }

	@Override
	protected String validateContents(Component comp, Object event)
	{
//		if (_enableAlarmHandling)
//		{
//			putWizardData("to-be-discarded.alarmPanelConfig", _alarmPanel.getConfig().toString());
//System.out.println("_alarmPanel.getConfig().toString(): "+ _alarmPanel.getConfig().toString());
//		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private void saveWizardData()
	{
		if (_enableAlarmHandling)
		{
			Map<String, Object> wizData = getWizardDataMap();

//System.out.println("ALARM-CONFIG: "+ _alarmPanel.getConfig());
			wizData.put("to-be-discarded.alarmPanelConfig", _alarmPanel.getConfig());
		}
		else
		{
			Map<String, Object> wizData = getWizardDataMap();

			wizData.remove("to-be-discarded.alarmPanelConfig");
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public WizardPanelNavResult allowBack(String stepName, Map settings, Wizard wizard)
    {
//System.out.println("Page-9------: allowBack()");
		saveWizardData();
		return WizardPanelNavResult.PROCEED;
    }

	@SuppressWarnings("rawtypes")
	@Override
	public WizardPanelNavResult allowNext(String stepName, Map settings, Wizard wizard)
	{
//System.out.println("Page-9------: allowNext()");
		saveWizardData();
		return WizardPanelNavResult.PROCEED;
	}
}

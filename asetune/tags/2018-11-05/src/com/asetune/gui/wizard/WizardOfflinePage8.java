/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

import javax.swing.JCheckBox;

import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardPage;
import org.netbeans.spi.wizard.WizardPanelNavResult;

import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.ui.config.AlarmWritersPanel;
import com.asetune.gui.swing.MultiLineLabel;

import net.miginfocom.swing.MigLayout;


public class WizardOfflinePage8
extends WizardPage
implements ActionListener, PropertyChangeListener
{
    private static final long serialVersionUID = 1L;
	private static final String WIZ_NAME = "alarmWriters";
	private static final String WIZ_DESC = "Alarm Writers";
	private static final String WIZ_HELP = "Generated Alarms Should be sent somewhere, this is the setup for that.";

	private JCheckBox  _enableAlarmHandling_chk = new JCheckBox("Enable Alarm Handling", true);

	private AlarmWritersPanel _alarmWritersPanel;
	
	
	public static String getDescription() { return WIZ_DESC; }
	@Override
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	public WizardOfflinePage8()
	{
		super(WIZ_NAME, WIZ_DESC);

		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		_alarmWritersPanel = new AlarmWritersPanel();

		_enableAlarmHandling_chk.setName("to-be-discarded.enableAlarmHandling");


		// Add a helptext
		add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );
		add(_enableAlarmHandling_chk, "span, wrap 20");
		add(_alarmWritersPanel, "span, grow, push, wrap");
		
		_alarmWritersPanel.setVisible(false);

		_alarmWritersPanel.addPropertyChangeListener(this);

		initData();
	}
	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
//		System.out.println("propertyChange(): evt="+evt);

		if ( evt.getPropertyName().startsWith("tableChanged") )
		{
//			System.out.println("propertyChange(): call: userInputReceived(null, null)");

			// Just to kick off validateContents()...
			userInputReceived(null, null); // Just to kick off validateContents()... 
			// but the above did not work; so use setProblem() instead
			if (_enableAlarmHandling_chk.isSelected())
			{
				setProblem(_alarmWritersPanel.getProblem());
			}
		}
	}

	private void initData()
	{
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
	}

	@Override
	protected String validateContents(Component comp, Object event)
	{
//System.out.println("PAGE-8 ------------------------- validateContents() comp=|"+comp+"|, event=|"+event+"|.");
//		String name = null;
//		if (comp != null)
//			name = comp.getName();

		//System.out.println("validateContents: name='"+name+"',\n\ttoString='"+comp+"'\n\tcomp='"+comp+"',\n\tevent='"+event+"'.");

		boolean isAlarmHandlingEnabled = _enableAlarmHandling_chk.isSelected();
		_alarmWritersPanel.setVisible(isAlarmHandlingEnabled);
		
		putWizardData("to-be-discarded.enableAlarmHandling", isAlarmHandlingEnabled+""); // Note: STRING
		putWizardData(AlarmHandler.PROPKEY_enable,           isAlarmHandlingEnabled+""); // Note: STRING

		if ( isAlarmHandlingEnabled )
		{
			String problem = _alarmWritersPanel.getProblem();
			if (problem != null)
			{
				return problem;
			}
//System.out.println("_alarmWritersPanel.getConfig().toString(): "+ _alarmWritersPanel.getConfig());
			putWizardData("to-be-discarded.alarmWritersPanelConfig", _alarmWritersPanel.getConfig());
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private void saveWizardData()
	{
		boolean isAlarmHandlingEnabled = _enableAlarmHandling_chk.isSelected();

		if (isAlarmHandlingEnabled)
		{
			Map<String, Object> wizData = getWizardDataMap();

//System.out.println("WRITERS-CONFIG: "+ _alarmWritersPanel.getConfig());
			wizData.put("to-be-discarded.alarmWritersPanelConfig", _alarmWritersPanel.getConfig());
		}
		else
		{
			Map<String, Object> wizData = getWizardDataMap();

			wizData.remove("to-be-discarded.alarmWritersPanelConfig");
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public WizardPanelNavResult allowBack(String stepName, Map settings, Wizard wizard)
    {
//System.out.println("Page-8------: allowBack()");
		saveWizardData();
		return WizardPanelNavResult.PROCEED;
    }

	@SuppressWarnings("rawtypes")
	@Override
	public WizardPanelNavResult allowNext(String stepName, Map settings, Wizard wizard)
	{
//System.out.println("Page-8------: allowNext()");
		saveWizardData();
		return WizardPanelNavResult.PROCEED;
	}
}

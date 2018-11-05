/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui.wizard;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.netbeans.spi.wizard.WizardPage;

import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.ValidationException;
import com.asetune.gui.swing.MultiLineLabel;
import com.asetune.pcs.report.DailySummaryReportFactory;
import com.asetune.pcs.report.senders.ReportSenderToMail;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;


public class WizardOfflinePage11
extends WizardPage
implements ActionListener
{
    private static final long serialVersionUID = 1L;
	private static final String WIZ_NAME = "pcsDailyReport";
	private static final String WIZ_DESC = "Send Daily Report";
	private static final String WIZ_HELP = "<html>Configure if you want a Daily Report Sent somewhere.<html>";

	private JCheckBox  _enableDailyReport_chk = new JCheckBox("Enable Daily Summary Report", DailySummaryReportFactory.DEFAULT_create);

	private JLabel     _keepServerNameRegExp_lbl = new JLabel("Keep ServerName RegExp");
	private JTextField _keepServerNameRegExp_txt = new JTextField();

	private JLabel     _skipServerNameRegExp_lbl = new JLabel("Skip ServerName RegExp");
	private JTextField _skipServerNameRegExp_txt = new JTextField();

	private JLabel     _reportGeneratorClassname_lbl = new JLabel("Report Generator Classname");
	private JTextField _reportGeneratorClassname_txt = new JTextField();

	private JLabel     _reportSenderClassname_lbl = new JLabel("Report Sender Classname");
	private JTextField _reportSenderClassname_txt = new JTextField();

//	private AlarmWritersPanel _alarmWritersPanel;
	private JPanel _reportPanel;
	private JPanel _sendToMailPanel;
	
	private List<CmSettingsHelper>  _toMailSettingslist = new ArrayList<>();
	private Map<String, JComponent> _toMailComponents   = new HashMap<>();
	
	public static String getDescription() { return WIZ_DESC; }
	@Override
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	public WizardOfflinePage11()
	{
		super(WIZ_NAME, WIZ_DESC);

		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		_toMailSettingslist = new ReportSenderToMail().getAvailableSettings();

		_reportPanel     = createReportPanel();
		_sendToMailPanel = createSendToMailPanel();

		// Add a helptext
		add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );
		add(_enableDailyReport_chk, "span, wrap 20");
		
		add(_reportPanel,     "span, growx, pushx, wrap");
		add(_sendToMailPanel, "span, grow, push, wrap");

		initData();
	}

	private JPanel createReportPanel()
	{
		JPanel panel = SwingUtils.createPanel("Report Configuration", true, new MigLayout());

		_keepServerNameRegExp_lbl.setToolTipText("<html>Regular expression of server names you want to create 'Daily Summary Reports' for. Default: <i>all servers</i>.</html>");
		_keepServerNameRegExp_txt.setToolTipText(_keepServerNameRegExp_lbl.getText());
		
		_skipServerNameRegExp_lbl.setToolTipText("<html>Regular expression of server names you want to skip/discard when creating 'Daily Summary Reports'. Default: <i>none</i>.</html>");
		_skipServerNameRegExp_txt.setToolTipText(_skipServerNameRegExp_lbl.getText());
		
		_reportGeneratorClassname_lbl.setToolTipText("<html>Classname of the Report which created the report.</html>");
		_reportGeneratorClassname_txt.setToolTipText(_reportGeneratorClassname_lbl.getText());
		
		_reportSenderClassname_lbl.setToolTipText("<html>Classname of the Sender that sends the report somewhere.</html>");
		_reportSenderClassname_txt.setToolTipText(_reportSenderClassname_lbl.getText());
		
		_enableDailyReport_chk        .setName(DailySummaryReportFactory.PROPKEY_create);
		_keepServerNameRegExp_txt     .setName(DailySummaryReportFactory.PROPKEY_filter_keep_servername);
		_skipServerNameRegExp_txt     .setName(DailySummaryReportFactory.PROPKEY_filter_skip_servername);
		_reportGeneratorClassname_txt .setName(DailySummaryReportFactory.PROPKEY_reportClassname);
		_reportSenderClassname_txt    .setName(DailySummaryReportFactory.PROPKEY_senderClassname);
		
		Configuration conf = Configuration.getCombinedConfiguration();
		_enableDailyReport_chk       .setSelected( conf.getBooleanProperty(DailySummaryReportFactory.PROPKEY_create                , DailySummaryReportFactory.DEFAULT_create));
		_keepServerNameRegExp_txt    .setText(     conf.getProperty       (DailySummaryReportFactory.PROPKEY_filter_keep_servername, DailySummaryReportFactory.DEFAULT_filter_keep_servername));
		_skipServerNameRegExp_txt    .setText(     conf.getProperty       (DailySummaryReportFactory.PROPKEY_filter_skip_servername, DailySummaryReportFactory.DEFAULT_filter_skip_servername));
		_reportGeneratorClassname_txt.setText(     conf.getProperty       (DailySummaryReportFactory.PROPKEY_reportClassname       , DailySummaryReportFactory.DEFAULT_reportClassname));
		_reportSenderClassname_txt   .setText(     conf.getProperty       (DailySummaryReportFactory.PROPKEY_senderClassname       , DailySummaryReportFactory.DEFAULT_senderClassname));

		panel.add(_keepServerNameRegExp_lbl, "");
		panel.add(_keepServerNameRegExp_txt, "pushx, growx, wrap");
		
		panel.add(_skipServerNameRegExp_lbl, "");
		panel.add(_skipServerNameRegExp_txt, "pushx, growx, wrap");
		
		panel.add(_reportGeneratorClassname_lbl, "");
		panel.add(_reportGeneratorClassname_txt, "pushx, growx, wrap");
		
		panel.add(_reportSenderClassname_lbl, "");
		panel.add(_reportSenderClassname_txt, "pushx, growx, wrap 20");

		return panel;
	}

	private JPanel createSendToMailPanel()
	{
		JPanel panel = SwingUtils.createPanel("Send To Mail", true, new MigLayout());

//		panel.add(new JLabel("NOT-YET-IMPLEMENTED"), "");
		
		Configuration conf = Configuration.getCombinedConfiguration();

		
		for (CmSettingsHelper cmsh : _toMailSettingslist)
		{
			String datatype = cmsh.getDataTypeString();

			if ("String".equals(datatype) || "Integer".equals(datatype))
			{
				JLabel     lbl = new JLabel(cmsh.getName());
				JTextField txt = new JTextField(conf.getProperty(cmsh.getPropName(), cmsh.getDefaultValue()));
				
				txt.setName(cmsh.getPropName());
				
				lbl.setToolTipText(cmsh.getDescription());
				txt.setToolTipText(cmsh.getDescription());
				
				_toMailComponents.put(cmsh.getPropName(), txt);

				panel.add(lbl, "");
				panel.add(txt, "pushx, growx, wrap");
			}
			else if ("Boolean".equals(datatype))
			{
				JCheckBox chk = new JCheckBox(cmsh.getName(), cmsh.getDefaultValue().equalsIgnoreCase("true"));

				chk.setName(cmsh.getPropName());
				
				chk.setToolTipText(cmsh.getDescription());

				_toMailComponents.put(cmsh.getPropName(), chk);

				panel.add(chk, "span, wrap");
			}
		}
		
		return panel;
	}

//this needs ALOT MORE attention before it's done...

//openssl: https://stackoverflow.com/questions/32508961/java-equivalent-of-an-openssl-aes-cbc-encryption


	private void initData()
	{
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
	}

	@Override
	protected String validateContents(Component component, Object event)
	{
		boolean isEnabled = _enableDailyReport_chk.isSelected();
		SwingUtils.setEnabled(_reportPanel,     isEnabled);
		SwingUtils.setEnabled(_sendToMailPanel, isEnabled);
		
		if ( ! isEnabled )
			return null;
		
		// Check: _keepServerNameRegExp
		String regxpVal = _keepServerNameRegExp_txt.getText();
		String fieldName = _keepServerNameRegExp_lbl.getText();
		if (StringUtil.hasValue(regxpVal))
		{
			try { Pattern.compile(regxpVal); }
			catch(PatternSyntaxException ex) 
			{ 
				return fieldName+": The RegExp '"+regxpVal+"' seems to be faulty. Caught: "+ex.getMessage(); 
			}
		}
			
		// Check: _skipServerNameRegExp
		regxpVal = _skipServerNameRegExp_txt.getText();
		fieldName = _skipServerNameRegExp_lbl.getText();
		if (StringUtil.hasValue(regxpVal))
		{
			try { Pattern.compile(regxpVal); }
			catch(PatternSyntaxException ex) 
			{ 
				return fieldName+": The RegExp '"+regxpVal+"' seems to be faulty. Caught: "+ex.getMessage(); 
			}
		}
		
		// Check if classname exists in classpath
		JLabel     lbl = _reportGeneratorClassname_lbl;
		JTextField txt = _reportGeneratorClassname_txt;
		String classname = txt.getText();
		try 
		{ 
			Class.forName(classname);
			txt.setBackground(_skipServerNameRegExp_txt.getBackground());
			txt.setToolTipText(lbl.getToolTipText());
		}
		catch (ClassNotFoundException ex)
		{
			txt.setBackground(Color.YELLOW);
			txt.setToolTipText(ex.toString());
		}
		
		lbl = _reportSenderClassname_lbl;
		txt = _reportSenderClassname_txt;
		classname = txt.getText();
		try 
		{ 
			Class.forName(classname);
			txt.setBackground(_skipServerNameRegExp_txt.getBackground());
			txt.setToolTipText(lbl.getToolTipText());
		}
		catch (ClassNotFoundException ex)
		{
			txt.setBackground(Color.YELLOW);
			txt.setToolTipText(ex.toString());
		}
			

		// Disable _sendToMailPanel if not "ReportSenderToMail".
		boolean isReportSenderToMail = false;
		String[] sa = _reportSenderClassname_txt.getText().split("\\.");
		if (sa.length > 0)
			isReportSenderToMail = "ReportSenderToMail".equals(sa[sa.length - 1]);
		SwingUtils.setEnabled(_sendToMailPanel, isReportSenderToMail);


		// check stuff for: ReportSenderToMail
		if (isReportSenderToMail)
		{
			// Check for mandatory fields
			// and if the field is "valid"
			for (CmSettingsHelper cmsh : _toMailSettingslist)
			{
				JComponent comp = _toMailComponents.get(cmsh.getPropName());
				if (comp != null && comp instanceof JTextField)
				{
					String val = ((JTextField)comp).getText();

					if (cmsh.isMandatory())
					{
						if (StringUtil.isNullOrBlank(val))
						{
							return "Field '"+cmsh.getName()+"' is mandatory.";
						}
					}

					// Validate content
					try { cmsh.isValidInput(val); }
					catch (ValidationException ex)
					{
						return "Field '"+cmsh.getName()+"': "+ex.getMessage();
					}
				}
			}
		}

		return null;
	}
//	@Override
//	protected String validateContents(Component comp, Object event)
//	{
////System.out.println("PAGE-11 ------------------------- validateContents() comp=|"+comp+"|, event=|"+event+"|.");
////		String name = null;
////		if (comp != null)
////			name = comp.getName();
//
//		//System.out.println("validateContents: name='"+name+"',\n\ttoString='"+comp+"'\n\tcomp='"+comp+"',\n\tevent='"+event+"'.");
//
//		boolean isEnabled = _enableDailyReport_chk.isSelected();
//		_alarmWritersPanel.setVisible(isEnabled);
//		
//		putWizardData("to-be-discarded.enableDailySummaryReport", isEnabled+""); // Note: STRING
//		putWizardData(DailySummaryReportFactory.PROPKEY_create, isEnabled+""); // Note: STRING
//
//		if ( isEnabled )
//		{
//			String problem = _alarmWritersPanel.getProblem();
//			if (problem != null)
//			{
//				return problem;
//			}
////System.out.println("_alarmWritersPanel.getConfig().toString(): "+ _alarmWritersPanel.getConfig());
//			putWizardData("to-be-discarded.dailySummaryReportConfig", _alarmWritersPanel.getConfig());
//		}
//
//		return null;
//	}
//
//	@SuppressWarnings("unchecked")
//	private void saveWizardData()
//	{
//		boolean isEnabled = _enableDailyReport_chk.isSelected();
//
//		if (isEnabled)
//		{
//			Map<String, Object> wizData = getWizardDataMap();
//
////System.out.println("WRITERS-CONFIG: "+ _alarmWritersPanel.getConfig());
//			wizData.put("to-be-discarded.dailySummaryReportConfig", _alarmWritersPanel.getConfig());
//		}
//		else
//		{
//			Map<String, Object> wizData = getWizardDataMap();
//
//			wizData.remove("to-be-discarded.dailySummaryReportConfig");
//		}
//	}
//
//	@SuppressWarnings("rawtypes")
//	@Override
//	public WizardPanelNavResult allowBack(String stepName, Map settings, Wizard wizard)
//    {
////System.out.println("Page-11------: allowBack()");
//		saveWizardData();
//		return WizardPanelNavResult.PROCEED;
//    }
//
//	@SuppressWarnings("rawtypes")
//	@Override
//	public WizardPanelNavResult allowNext(String stepName, Map settings, Wizard wizard)
//	{
////System.out.println("Page-11------: allowNext()");
//		saveWizardData();
//		return WizardPanelNavResult.PROCEED;
//	}
}

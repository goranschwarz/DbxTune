/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardPage;
import org.netbeans.spi.wizard.WizardPanelNavResult;

import com.asetune.gui.swing.MultiLineLabel;
import com.asetune.pcs.PersistWriterToHttpJson;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;


public class WizardOfflinePage10
extends WizardPage
implements ActionListener
{
    private static final long serialVersionUID = 1L;
	private static final String WIZ_NAME = "pcsWriters";
	private static final String WIZ_DESC = "Other PCS Writers";
	private static final String WIZ_HELP = "<html>Configure other writers that can send Counter data to <i>various</i> places.<html>";

//	private JLabel     _notEnabled_lbl          = new JLabel("Alarm Writers isn't enabled, so this page can be skipped.");
	private JCheckBox  _pcsWriters_chk = new JCheckBox("<html>Send Counter Data to <i>other</i> places than a database.</html>", true);

	private JPanel     _dbxCentral_pan;
	private JLabel     _dbxCentral_lbl     = new JLabel("<html>Dbx Central is a service that stores Thrend Graphs for any DbxTune collector<br>Then you can view Trend Graphs from any html Browser.</html>");
	private JCheckBox  _dbxCentral_chk     = new JCheckBox("<html>Send Counters to Dbx Central.</html>", false);
	private JLabel     _dbxCentralHost_lbl = new JLabel("Hostname");
	private JTextField _dbxCentralHost_txt = new JTextField("localhost");
	private JLabel     _dbxCentralPort_lbl = new JLabel("Port");
	private JTextField _dbxCentralPort_txt = new JTextField("8080");

	private JPanel     _udWriters_pan;
	
	public static String getDescription() { return WIZ_DESC; }
	@Override
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	public WizardOfflinePage10()
	{
		super(WIZ_NAME, WIZ_DESC);

		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		// Add a helptext
		add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );

//		add(_pcsWriters_chk,          "hidemode 3, wrap");
//		add(new JLabel(""),          "hidemode 3, wrap 30");
//		add(new JLabel("NOT YET IMPLEMENTED"),          "hidemode 3, wrap");

		_dbxCentral_pan = createDbxCentralPanel();
		_udWriters_pan  = createUdWriterPanel();
		
		add(_dbxCentral_pan,  "growx, pushx, wrap");
		add(_udWriters_pan,   "growx, pushx, wrap");

		initData();
	}
	
	private JPanel createDbxCentralPanel()
	{
		JPanel panel = SwingUtils.createPanel("Dbx Central", true, new MigLayout());

		_dbxCentral_chk     .setToolTipText("Select if you want to send data to DbxCentral");
		_dbxCentralHost_txt .setToolTipText("Hostname where the DbxCentral server is running");
		_dbxCentralPort_txt .setToolTipText("Port number where the DbxCentral server is running");
		
		panel.add(_dbxCentral_lbl,     "span, wrap 20");

		panel.add(_dbxCentral_chk,     "skip 1, wrap");

		panel.add(_dbxCentralHost_lbl, "");
		panel.add(_dbxCentralHost_txt, "growx, pushx, wrap");

		panel.add(_dbxCentralPort_lbl, "");
		panel.add(_dbxCentralPort_txt, "growx, pushx, wrap");

		return panel;
	}

	private JPanel createUdWriterPanel()
	{
		JPanel panel = SwingUtils.createPanel("Send to 'other' writers", true, new MigLayout());

		panel.add(new JLabel("NOT YET IMPLEMENTED"), "wrap 20");
		panel.add(new JLabel("You can add this manually to the saved file. Use key 'pcs.write.writerClass', which is a comma separated list where you can append your writers."), "wrap");

		return panel;
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
		return null;
	}

	@SuppressWarnings("rawtypes")
	private void saveWizardData()
	{
		Map wizardMap = getWizardDataMap();

		String urlKey = PersistWriterToHttpJson.replaceKey(PersistWriterToHttpJson.PROPKEY_url);
		String urlVal = PersistWriterToHttpJson.DEFAULT_url;

		String propKey = "to-be-discarded.pcsWriterClassCsv";
//		Object writerObj = wizardMap.get(propKey);
//
//		if (writerObj != null && writerObj instanceof String)
//		{
			// Remove properties (if they already exists)
			wizardMap.remove(propKey);
			wizardMap.remove(urlKey);

			//List<String> currentList = StringUtil.commaStrToList((String)writerObj);
			List<String> newList     = new ArrayList<>();

			// DbxCentral
			if (_dbxCentral_chk.isSelected())
			{
				newList.add(PersistWriterToHttpJson.class.getName());

				urlVal = urlVal.replace("localhost",       _dbxCentralHost_txt.getText().trim());
				urlVal = urlVal.replace(":8080",     ":" + _dbxCentralPort_txt.getText().trim());

				// PersistWriterToHttpJson.url = http://localhost:8080/api/pcs/receiver
				putWizardData(urlKey, urlVal);
			}

			// Other writers... which is not yet implemeted
			// - remove current prop keys
			// - add writer to 'newList'
			// - add configuration properties for ecach of the user defined writers
			
			// pcsWriterClassCsv = com.asetune.pcs.PersistWriterToHttpJson [, otherWriters]
			if ( ! newList.isEmpty() )
				putWizardData(propKey, StringUtil.toCommaStr(newList));
//		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public WizardPanelNavResult allowBack(String stepName, Map settings, Wizard wizard)
    {
		saveWizardData();
		return WizardPanelNavResult.PROCEED;
    }

	@SuppressWarnings("rawtypes")
	@Override
	public WizardPanelNavResult allowNext(String stepName, Map settings, Wizard wizard)
	{
		saveWizardData();
		return WizardPanelNavResult.PROCEED;
	}
}

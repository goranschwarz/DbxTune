/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
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
import com.asetune.pcs.PersistWriterToInfluxDb;
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
	private JLabel     _dbxCentral_lbl      = new JLabel("<html>Dbx Central is a service that stores Thrend Graphs for any DbxTune collector<br>"
	                                                         + "Then you can view Trend Graphs from any html Browser."
	                                                         + "</html>");
	private JCheckBox  _dbxCentral_chk      = new JCheckBox("<html>Send Counters to Dbx Central.</html>", false);
	private JLabel     _dbxCentralHost_lbl  = new JLabel("Hostname");
	private JTextField _dbxCentralHost_txt  = new JTextField("localhost");
	private JLabel     _dbxCentralPort_lbl  = new JLabel("Port");
	private JTextField _dbxCentralPort_txt  = new JTextField("8080");

	private JPanel     _influxDb_pan;
	private JLabel     _influxDb_lbl        = new JLabel("<html>InfluxDB (https://www.influxdata.com/) is a Time Series database that stores Trend Graphs for any DbxTune collector<br>"
	                                                         + "From there on; you could for example use Grafana (https://grafana.com/) to visualize the Thrend Graphs."
	                                                         + "</html>");
	private JCheckBox  _influxDb_chk        = new JCheckBox("<html>Send Thrend Graph Counters to InfluxDB.</html>", false);
	private JLabel     _influxDbHost_lbl    = new JLabel("Hostname");
	private JTextField _influxDbHost_txt    = new JTextField("localhost");
	private JLabel     _influxDbPort_lbl    = new JLabel("Port");
	private JTextField _influxDbPort_txt    = new JTextField("8086");
	private JLabel     _influxDbUser_lbl    = new JLabel("Username");
	private JTextField _influxDbUser_txt    = new JTextField("");
	private JLabel     _influxDbPasswd_lbl  = new JLabel("Password");
	private JTextField _influxDbPasswd_txt  = new JTextField("");
//	private JCheckBox  _influxDbAsUtc_chk   = new JCheckBox("<html>Convert local sample times to UTC when sending to InfuxDb</html>", true);
	private JCheckBox  _influxDbAddTags_chk = new JCheckBox("<html>Store <i>graphLabels</i> and other <i>graph meta data</i> as <b>tags</b> in InfuxDb</html>", false);

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
		_influxDb_pan   = createInfluxDbPanel();
		_udWriters_pan  = createUdWriterPanel();
		
		add(_dbxCentral_pan,  "growx, pushx, wrap");
		add(_influxDb_pan,    "growx, pushx, wrap");
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

	private JPanel createInfluxDbPanel()
	{
		JPanel panel = SwingUtils.createPanel("InfluxDB", true, new MigLayout());

		_influxDb_chk     .setToolTipText("Select if you want to send data to InfluxDB");
		_influxDbHost_txt .setToolTipText("Hostname where the InfluxDB server is running");
		_influxDbPort_txt .setToolTipText("Port number where the InfluxDB server is running");
		
		panel.add(_influxDb_lbl,       "span, wrap 20");

		panel.add(_influxDb_chk,       "skip 1, wrap");

		panel.add(_influxDbHost_lbl, "");
		panel.add(_influxDbHost_txt, "growx, pushx, wrap");

		panel.add(_influxDbPort_lbl, "");
		panel.add(_influxDbPort_txt, "growx, pushx, wrap");

		panel.add(_influxDbUser_lbl,   "");
		panel.add(_influxDbUser_txt,   "growx, pushx, wrap");

		panel.add(_influxDbPasswd_lbl, "");
		panel.add(_influxDbPasswd_txt, "growx, pushx, wrap");

//		panel.add(_influxDbAsUtc_chk,  "skip 1, wrap");

		panel.add(_influxDbAddTags_chk, "skip 1, wrap");

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

	private int removeKeyPrefixed(Map<?, ?> map, String keyPrefix)
	{
        int cnt = 0;

        final Iterator<?> iter = map.keySet().iterator();
		while (iter.hasNext()) 
		{
			if (iter.next().toString().startsWith(keyPrefix)) 
			{
				iter.remove();
				cnt++;
			}
		}
		return cnt;
	}
	
	@SuppressWarnings("rawtypes")
	private void saveWizardData()
	{
		Map wizardMap = getWizardDataMap();

		String propKey = "to-be-discarded.pcsWriterClassCsv";
//		Object writerObj = wizardMap.get(propKey);
//
//		if (writerObj != null && writerObj instanceof String)
//		{
			// Remove properties (if they already exists)
			wizardMap.remove(propKey);
			wizardMap.remove(PersistWriterToHttpJson.PROPKEY_url);
			wizardMap.remove(PersistWriterToInfluxDb.PROPKEY_url);
			
			// remove all properties starting with:
			// - PersistWriterToHttpJson
			// - PersistWriterToInfluxDb
			removeKeyPrefixed(wizardMap, PersistWriterToHttpJson.class.getName() + ".");
			removeKeyPrefixed(wizardMap, PersistWriterToInfluxDb.class.getName() + ".");


			//List<String> currentList = StringUtil.commaStrToList((String)writerObj);
			List<String> newList     = new ArrayList<>();

			// DbxCentral
			if (_dbxCentral_chk.isSelected())
			{
				// Add writer class
				newList.add(PersistWriterToHttpJson.class.getName());

				String urlKey = PersistWriterToHttpJson.replaceKey(PersistWriterToHttpJson.PROPKEY_url);
				String urlVal = PersistWriterToHttpJson.DEFAULT_url;

				urlVal = urlVal.replace("localhost",       _dbxCentralHost_txt.getText().trim());
				urlVal = urlVal.replace(":8080",     ":" + _dbxCentralPort_txt.getText().trim());

				// PersistWriterToHttpJson.url = http://localhost:8080/api/pcs/receiver
				putWizardData(urlKey, urlVal);
			}

			// InfluxDB
			if (_influxDb_chk.isSelected())
			{
				// Add writer class
				newList.add(PersistWriterToInfluxDb.class.getName());

				String urlKey = PersistWriterToInfluxDb.replaceKey(PersistWriterToInfluxDb.PROPKEY_url);
				String urlVal = PersistWriterToInfluxDb.DEFAULT_url;

				urlVal = urlVal.replace("localhost",       _influxDbHost_txt.getText().trim());
				urlVal = urlVal.replace(":8086",     ":" + _influxDbPort_txt.getText().trim());

				// PersistWriterToInfluxDb.url = http://localhost:8080/api/pcs/receiver
				putWizardData(urlKey, urlVal);

				putWizardData(PersistWriterToInfluxDb.replaceKey(PersistWriterToInfluxDb.PROPKEY_username   ), _influxDbUser_txt   .getText().trim());
				putWizardData(PersistWriterToInfluxDb.replaceKey(PersistWriterToInfluxDb.PROPKEY_password   ), _influxDbPasswd_txt .getText().trim());
//				putWizardData(PersistWriterToInfluxDb.replaceKey(PersistWriterToInfluxDb.PROPKEY_asUtcTime  ), _influxDbAsUtc_chk  .isSelected());
				putWizardData(PersistWriterToInfluxDb.replaceKey(PersistWriterToInfluxDb.PROPKEY_addMetaTags), _influxDbAddTags_chk.isSelected());
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

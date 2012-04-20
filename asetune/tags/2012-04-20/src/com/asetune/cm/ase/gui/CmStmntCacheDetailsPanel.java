package com.asetune.cm.ase.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

public class CmStmntCacheDetailsPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmStmntCacheDetailsPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmStmntCacheDetails.CM_NAME;

	private JCheckBox sampleXmlPlan_chk;

	public CmStmntCacheDetailsPanel(CountersModel cm)
	{
		super(cm);

		if (cm.getIconFile() != null)
			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
//		Configuration conf = Configuration.getCombinedConfiguration();
//		String colorStr = null;

	}

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		
		// SAMPLE SQL TEXT
		JCheckBox sampleSqlText_chk  = new JCheckBox("Get SQL Text", conf == null ? false : conf.getBooleanProperty(getName()+".sample.sqlText", false));
		sampleSqlText_chk.setName(getName()+".sample.sqlText");
		sampleSqlText_chk.setToolTipText("<html>Get SQL Text (using: show_cached_text(SSQLID)) assosiated with the Cached Statement.<br><b>Note</b>: This is not a filter, you will have to wait for next sample time for this option to take effect.</html>");
		panel.add(sampleSqlText_chk, "wrap");

		sampleSqlText_chk.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(getName()+".sample.sqlText", ((JCheckBox)e.getSource()).isSelected());
				conf.save();
			}
		});
		
		// SAMPLE SHOWPLAN
		JCheckBox sampleShowplan_chk = new JCheckBox("Get Showplan", conf == null ? false : conf.getBooleanProperty(getName()+".sample.showplan", false));
		sampleShowplan_chk.setName(getName()+".sample.showplan");
		sampleShowplan_chk.setToolTipText("<html>Get Showplan assosiated with the Cached Statement.<br><b>Note</b>: This is not a filter, you will have to wait for next sample time for this option to take effect.</html>");
		panel.add(sampleShowplan_chk, "wrap");

		sampleShowplan_chk.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(getName()+".sample.showplan", ((JCheckBox)e.getSource()).isSelected());
				conf.save();
			}
		});

		// SAMPLE XML PLAN
//		JCheckBox sampleXmlPlan_chk  = new JCheckBox("Get XML Plan", conf == null ? false : conf.getBooleanProperty(localName+".sample.xmlPlan", false));
		sampleXmlPlan_chk  = new JCheckBox("Get XML Plan", conf == null ? false : conf.getBooleanProperty(getName()+".sample.xmlPlan", false));
		sampleXmlPlan_chk.setName(getName()+".sample.xmlPlan");
		sampleXmlPlan_chk.setToolTipText("<html>ONLY ON ASE 15.7 and above<br>Get XML Plan (using: show_cached_plan_in_xml(SSQLID, 0)) assosiated with the Cached Statement.<br><b>Note</b>: Try uncheck 'Get Showplan' if not the whole XML is displayed.<br>This is not a filter, you will have to wait for next sample time for this option to take effect.</html>");
		panel.add(sampleXmlPlan_chk, "wrap");

		sampleXmlPlan_chk.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(getName()+".sample.xmlPlan", ((JCheckBox)e.getSource()).isSelected());
				conf.save();
			}
		});
		
		return panel;
	}

	@Override
	public void checkLocalComponents()
	{
		CountersModel cm = getCm();
		if (cm != null)
		{
			boolean enabled = false;
			if (cm.isRuntimeInitialized() && cm.getServerVersion() >= 15700)
				enabled = true;

			sampleXmlPlan_chk.setEnabled(enabled);
		}
	}
}

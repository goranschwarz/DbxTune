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
package com.asetune.cm.ase.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.CmStmntCacheDetails;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.Ver;

import net.miginfocom.swing.MigLayout;

public class CmStmntCacheDetailsPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmStmntCacheDetailsPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmStmntCacheDetails.CM_NAME;

	public static final String  TOOLTIP_sample_sqlText               = "<html>Get SQL Text (using: show_cached_text(SSQLID)) associated with the Cached Statement.<br><b>Note</b>: This is not a filter, you will have to wait for next sample time for this option to take effect.</html>";
	public static final String  TOOLTIP_sample_showplan              = "<html>Get Showplan assosiated with the Cached Statement.<br><b>Note</b>: This is not a filter, you will have to wait for next sample time for this option to take effect.</html>";
	public static final String  TOOLTIP_sample_xmlPlan               = "<html>ONLY ON ASE 15.7 and above<br>Get XML Plan (using: show_cached_plan_in_xml(SSQLID, 0)) associated with the Cached Statement.<br><b>Note</b>: Try uncheck 'Get Showplan' if not the whole XML is displayed.<br>This is not a filter, you will have to wait for next sample time for this option to take effect.</html>";
	public static final String  TOOLTIP_sample_xmlPlan_levelOfDetail = "<html>ONLY ON ASE 15.7 and above<br>Get XML Plan (using: show_cached_plan_in_xml(SSQLID, 0, <levelOfDetail>)) This is a number which is level-of-details<br><b>Note</b>: Check ASE Manual function show_cached_plan_in_xml() for more information.</html>";
	public static final String  TOOLTIP_sample_metricsCountGtZero    = "<html>Modifies the SQL statement to include/exclude <code>MetricsCount > 0</code> to the where clause.<br><b>Note</b>: This is not a filter, you will have to wait for next sample time for this option to take effect.<br><b>Note</b>: In some releases MetricsCount that is 0 will generate some <i>strange</i> values in date fields (LastUsedDate, LastRecompiledDate, CachedDate).</html>";

	private JCheckBox sampleXmlPlan_chk;

	public CmStmntCacheDetailsPanel(CountersModel cm)
	{
		super(cm);

//		if (cm.getIconFile() != null)
//			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

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
		JCheckBox sampleSqlText_chk  = new JCheckBox("Get SQL Text", conf == null ? CmStmntCacheDetails.DEFAULT_sample_sqlText : conf.getBooleanProperty(CmStmntCacheDetails.PROPKEY_sample_sqlText, CmStmntCacheDetails.DEFAULT_sample_sqlText));
		sampleSqlText_chk.setName(CmStmntCacheDetails.PROPKEY_sample_sqlText);
		sampleSqlText_chk.setToolTipText(TOOLTIP_sample_sqlText);
		panel.add(sampleSqlText_chk, "wrap");

		sampleSqlText_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmStmntCacheDetails.PROPKEY_sample_sqlText, ((JCheckBox)e.getSource()).isSelected());
				conf.save();

				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		
		// SAMPLE SHOWPLAN
		JCheckBox sampleShowplan_chk = new JCheckBox("Get Showplan", conf == null ? CmStmntCacheDetails.DEFAULT_sample_showplan : conf.getBooleanProperty(CmStmntCacheDetails.PROPKEY_sample_showplan, CmStmntCacheDetails.DEFAULT_sample_showplan));
		sampleShowplan_chk.setName(CmStmntCacheDetails.PROPKEY_sample_showplan);
		sampleShowplan_chk.setToolTipText(TOOLTIP_sample_showplan);
		panel.add(sampleShowplan_chk, "wrap");

		sampleShowplan_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmStmntCacheDetails.PROPKEY_sample_showplan, ((JCheckBox)e.getSource()).isSelected());
				conf.save();

				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

		// SAMPLE XML PLAN
		sampleXmlPlan_chk  = new JCheckBox("Get XML Plan", conf == null ? CmStmntCacheDetails.DEFAULT_sample_xmlPlan : conf.getBooleanProperty(CmStmntCacheDetails.PROPKEY_sample_xmlPlan, CmStmntCacheDetails.DEFAULT_sample_xmlPlan));
		sampleXmlPlan_chk.setName(CmStmntCacheDetails.PROPKEY_sample_xmlPlan);
		sampleXmlPlan_chk.setToolTipText(TOOLTIP_sample_xmlPlan);
		panel.add(sampleXmlPlan_chk, "wrap");

		sampleXmlPlan_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmStmntCacheDetails.PROPKEY_sample_xmlPlan, ((JCheckBox)e.getSource()).isSelected());
				conf.save();

				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		
		// SAMPLE MetricsCount > 0
		JCheckBox metricsCountGtZero_chk = new JCheckBox("WHERE MetricsCount > 0", conf == null ? CmStmntCacheDetails.DEFAULT_sample_metricsCountGtZero : conf.getBooleanProperty(CmStmntCacheDetails.PROPKEY_sample_metricsCountGtZero, CmStmntCacheDetails.DEFAULT_sample_metricsCountGtZero));
		metricsCountGtZero_chk.setName(CmStmntCacheDetails.PROPKEY_sample_metricsCountGtZero);
		metricsCountGtZero_chk.setToolTipText(TOOLTIP_sample_metricsCountGtZero);
		panel.add(metricsCountGtZero_chk, "wrap");

		metricsCountGtZero_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmStmntCacheDetails.PROPKEY_sample_metricsCountGtZero, ((JCheckBox)e.getSource()).isSelected());
				conf.save();

				// ReInitialize the SQL
				getCm().setSql(null);
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
//			if (cm.isRuntimeInitialized() && cm.getServerVersion() >= 15700)
//			if (cm.isRuntimeInitialized() && cm.getServerVersion() >= 1570000)
			if ( cm.isRuntimeInitialized() && cm.getServerVersion() >= Ver.ver(15,7) )
				enabled = true;

			sampleXmlPlan_chk.setEnabled(enabled);
		}
	}
}

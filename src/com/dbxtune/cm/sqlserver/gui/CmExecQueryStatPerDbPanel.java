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
package com.dbxtune.cm.sqlserver.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.sqlserver.CmExecQueryStatPerDb;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.utils.Configuration;

import net.miginfocom.swing.MigLayout;

public class CmExecQueryStatPerDbPanel
extends TabularCntrPanel
{
	private static final long    serialVersionUID      = 1L;

	public static final String  TOOLTIP_sample_resourcedb = "Sample information about dbid=32767 (Microsoft System Resource DB). https://learn.microsoft.com/en-us/sql/relational-databases/databases/resource-database";

	public CmExecQueryStatPerDbPanel(CountersModel cm)
	{
		super(cm);

//		init();
	}
	
	private JCheckBox l_sampleMsResourceDb_chk;

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		LocalOptionsConfigPanel panel = new LocalOptionsConfigPanel("Local Options", new LocalOptionsConfigChanges()
		{
			@Override
			public void configWasChanged(String propName, String propVal)
			{
				Configuration conf = Configuration.getCombinedConfiguration();

//				list.add(new CmSettingsHelper("Sample MS Resource DB", PROPKEY_sample_MsResourceDb, Boolean.class, conf.getBooleanProperty(PROPKEY_sample_MsResourceDb, DEFAULT_sample_MsResourceDb), DEFAULT_ALARM_isAlarmsEnabled, CmExecQueryStatPerDbPanel.TOOLTIP_sample_resourcedb));
				
				l_sampleMsResourceDb_chk.setSelected(conf.getBooleanProperty(CmExecQueryStatPerDb.PROPKEY_sample_MsResourceDb, CmExecQueryStatPerDb.DEFAULT_sample_MsResourceDb));

				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

//		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		l_sampleMsResourceDb_chk = new JCheckBox("Sample MS Resource DB", conf == null ? CmExecQueryStatPerDb.DEFAULT_sample_MsResourceDb : conf.getBooleanProperty(CmExecQueryStatPerDb.PROPKEY_sample_MsResourceDb, CmExecQueryStatPerDb.DEFAULT_sample_MsResourceDb));

		l_sampleMsResourceDb_chk.setName(CmExecQueryStatPerDb.PROPKEY_sample_MsResourceDb);
		l_sampleMsResourceDb_chk.setToolTipText(TOOLTIP_sample_resourcedb);

		panel.add(l_sampleMsResourceDb_chk, "wrap");

		l_sampleMsResourceDb_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmExecQueryStatPerDb.PROPKEY_sample_MsResourceDb, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		
		return panel;
	}
}

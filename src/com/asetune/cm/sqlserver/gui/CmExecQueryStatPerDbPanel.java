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
package com.asetune.cm.sqlserver.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.CmExecQueryStatPerDb;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmExecQueryStatPerDbPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmTempdbSpidUsagePanel.class);
	private static final long    serialVersionUID      = 1L;

	public static final String  TOOLTIP_sample_resourcedb = "Sample information about dbid=32767 (Microsoft System Resource DB). https://learn.microsoft.com/en-us/sql/relational-databases/databases/resource-database";

	public CmExecQueryStatPerDbPanel(CountersModel cm)
	{
		super(cm);

//		init();
	}
	
	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		JCheckBox sampleMsResourceDb_chk = new JCheckBox("Sample MS Resource DB", conf == null ? CmExecQueryStatPerDb.DEFAULT_sample_MsResourceDb : conf.getBooleanProperty(CmExecQueryStatPerDb.PROPKEY_sample_MsResourceDb, CmExecQueryStatPerDb.DEFAULT_sample_MsResourceDb));

		sampleMsResourceDb_chk.setName(CmExecQueryStatPerDb.PROPKEY_sample_MsResourceDb);
		sampleMsResourceDb_chk.setToolTipText(TOOLTIP_sample_resourcedb);

		panel.add(sampleMsResourceDb_chk, "wrap");

		sampleMsResourceDb_chk.addActionListener(new ActionListener()
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

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
package com.dbxtune.cm.postgres.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.postgres.CmPgTablesIo;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.utils.Configuration;

import net.miginfocom.swing.MigLayout;

public class CmPgTablesIoPanel
extends TabularCntrPanel
{
	private static final long    serialVersionUID      = 1L;

	private JCheckBox l_sampleSystemTables_chk;

	public CmPgTablesIoPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
	}

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		LocalOptionsConfigPanel panel = new LocalOptionsConfigPanel("Local Options", new LocalOptionsConfigChanges()
		{
			@Override
			public void configWasChanged(String propName, String propVal)
			{
				Configuration conf = Configuration.getCombinedConfiguration();

//				list.add(new CmSettingsHelper("Sample System Tables", PROPKEY_sample_systemTables , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemTables  , DEFAULT_sample_systemTables  ), DEFAULT_sample_systemTables, "Sample System Tables" ));

				l_sampleSystemTables_chk.setSelected(conf.getBooleanProperty(CmPgTablesIo.PROPKEY_sample_systemTables, CmPgTablesIo.DEFAULT_sample_systemTables));

				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

//		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean defaultOpt;
//		int     defaultIntOpt;

		//-----------------------------------------
		// sample system tables:
		//-----------------------------------------
		defaultOpt = conf == null ? CmPgTablesIo.DEFAULT_sample_systemTables : conf.getBooleanProperty(CmPgTablesIo.PROPKEY_sample_systemTables, CmPgTablesIo.DEFAULT_sample_systemTables);
		l_sampleSystemTables_chk = new JCheckBox("Include System Tables", defaultOpt);

		l_sampleSystemTables_chk.setName(CmPgTablesIo.PROPKEY_sample_systemTables);
		l_sampleSystemTables_chk.setToolTipText("<html>" +
				"Include system tables in the output<br>" +
				"</html>");

		l_sampleSystemTables_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmPgTablesIo.PROPKEY_sample_systemTables, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);
			}
		});
		
		// LAYOUT
		panel.add(l_sampleSystemTables_chk, "wrap");

		return panel;
	}

	@Override
	public void checkLocalComponents()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean confProp = conf.getBooleanProperty(CmPgTablesIo.PROPKEY_sample_systemTables, CmPgTablesIo.DEFAULT_sample_systemTables);
		boolean guiProp  = l_sampleSystemTables_chk.isSelected();

		if (confProp != guiProp)
			l_sampleSystemTables_chk.setSelected(confProp);
	}
}

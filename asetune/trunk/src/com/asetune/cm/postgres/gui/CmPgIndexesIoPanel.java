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
package com.asetune.cm.postgres.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.asetune.cm.CountersModel;
import com.asetune.cm.postgres.CmPgIndexesIo;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmPgIndexesIoPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmPgIndexesPanel.class);
	private static final long    serialVersionUID      = 1L;

	private JCheckBox l_sampleSystemTables_chk;

	public CmPgIndexesIoPanel(CountersModel cm)
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
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean defaultOpt;
//		int     defaultIntOpt;

		//-----------------------------------------
		// sample system tables:
		//-----------------------------------------
		defaultOpt = conf == null ? CmPgIndexesIo.DEFAULT_sample_systemTables : conf.getBooleanProperty(CmPgIndexesIo.PROPKEY_sample_systemTables, CmPgIndexesIo.DEFAULT_sample_systemTables);
		l_sampleSystemTables_chk = new JCheckBox("Include System Tables", defaultOpt);

		l_sampleSystemTables_chk.setName(CmPgIndexesIo.PROPKEY_sample_systemTables);
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
				conf.setProperty(CmPgIndexesIo.PROPKEY_sample_systemTables, ((JCheckBox)e.getSource()).isSelected());
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
		boolean confProp = conf.getBooleanProperty(CmPgIndexesIo.PROPKEY_sample_systemTables, CmPgIndexesIo.DEFAULT_sample_systemTables);
		boolean guiProp  = l_sampleSystemTables_chk.isSelected();

		if (confProp != guiProp)
			l_sampleSystemTables_chk.setSelected(confProp);
	}
}

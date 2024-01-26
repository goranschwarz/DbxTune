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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.cm.postgres.CmPgStatementsSumDb;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmPgStatementsSumDbPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmPgStatementsSumDbPanel.class);
	private static final long    serialVersionUID      = 1L;

	private JCheckBox l_excludeDbxTune_chk;

	public CmPgStatementsSumDbPanel(CountersModel cm)
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
		defaultOpt = conf == null ? CmPgStatementsSumDb.DEFAULT_excludeDbxTune : conf.getBooleanProperty(CmPgStatementsSumDb.PROPKEY_excludeDbxTune, CmPgStatementsSumDb.DEFAULT_excludeDbxTune);
		l_excludeDbxTune_chk = new JCheckBox("Exclude Statements issued by " + Version.getAppName(), defaultOpt);

		l_excludeDbxTune_chk.setName(CmPgStatementsSumDb.PROPKEY_excludeDbxTune);
		l_excludeDbxTune_chk.setToolTipText("<html>" +
				"Exclude Statements issued by " + Version.getAppName() + " (discards all statements issued by the same <b>username</b> that any " + Version.getAppName() + " connected as.<br>" +
				"<b>Note</b>: If " + Version.getAppName() + " connects with username 'postgres', we will NOT discard those..." +
				"</html>");

		l_excludeDbxTune_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmPgStatementsSumDb.PROPKEY_excludeDbxTune, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);
			}
		});
		
		// LAYOUT
		panel.add(l_excludeDbxTune_chk, "wrap");

		return panel;
	}

	@Override
	public void checkLocalComponents()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean confProp = conf.getBooleanProperty(CmPgStatementsSumDb.PROPKEY_excludeDbxTune, CmPgStatementsSumDb.DEFAULT_excludeDbxTune);
		boolean guiProp  = l_excludeDbxTune_chk.isSelected();

		if (confProp != guiProp)
			l_excludeDbxTune_chk.setSelected(confProp);
	}
}

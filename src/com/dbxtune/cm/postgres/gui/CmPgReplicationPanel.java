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
package com.dbxtune.cm.postgres.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.postgres.CmPgReplication;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

import net.miginfocom.swing.MigLayout;

public class CmPgReplicationPanel
extends TabularCntrPanel
{
	private static final long    serialVersionUID      = 1L;

	public CmPgReplicationPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
//		Configuration conf = Configuration.getCombinedConfiguration();
//		String colorStr = null;
//
//		// RED = active
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.checksum_failures");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				String colName = adapter.getColumnName(adapter.column);
//				if ( ! StringUtil.equalsAny(colName, "checksum_failures", "checksum_last_failure"))
//					return false;
//				
//				if ( "checksum_failures".equals(colName) && adapter.getValue() instanceof Number)
//				{
//					Number failures = (Number) adapter.getValue();
//					return failures.intValue() > 0;
//				}
//
//				if ( "checksum_last_failure".equals(colName))
//					return adapter.getValue() != null;
//
//				return false;
//			}
//		}, SwingUtils.parseColor(colorStr, Color.RED), null));
	}

	private JCheckBox  l_updateActive_chk;

	private JLabel     l_updateActiveInterval_lbl;
	private JTextField l_updateActiveInterval_txt;

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		LocalOptionsConfigPanel panel = new LocalOptionsConfigPanel("Local Options", new LocalOptionsConfigChanges()
		{
			@Override
			public void configWasChanged(String propName, String propVal)
			{
				Configuration conf = Configuration.getCombinedConfiguration();

				l_updateActive_chk        .setSelected(conf.getBooleanProperty(CmPgReplication.PROPKEY_update_primary             , CmPgReplication.DEFAULT_update_primary));
				l_updateActiveInterval_txt.setText(""+ conf.getLongProperty   (CmPgReplication.PROPKEY_update_primaryIntervalInSec, CmPgReplication.DEFAULT_update_primaryIntervalInSec));

				// ReInitialize the SQL
				//getCm().setSql(null);
			}
		});

//		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
//		int     defaultIntOpt;
//		boolean defaultBoolOpt;

		
		l_updateActive_chk         = new JCheckBox("Update Primary Instance,", conf.getBooleanProperty(CmPgReplication.PROPKEY_update_primary, CmPgReplication.DEFAULT_update_primary));
		l_updateActiveInterval_lbl = new JLabel("Interval in Seconds");
		l_updateActiveInterval_txt = new JTextField(conf.getLongProperty(CmPgReplication.PROPKEY_update_primaryIntervalInSec, CmPgReplication.DEFAULT_update_primaryIntervalInSec)+"", 5);
		
		l_updateActive_chk        .setToolTipText("<html>If we are Connect to PRIMARY Instance (and we are in RW mode and there are <i>replication subscribers</i>, then Update a dummy table, this to introduce network traffic in a <i>calm</i> system.</html>");
		l_updateActiveInterval_lbl.setToolTipText("<html>Do not update primary side every time, but wait for x second between updates.</html>");
		l_updateActiveInterval_txt.setToolTipText(l_updateActiveInterval_lbl.getToolTipText());


		//-----------------------------------------
		// LAYOUT
		//-----------------------------------------
		panel.add(l_updateActive_chk,         "split");
		panel.add(l_updateActiveInterval_lbl, "");
		panel.add(l_updateActiveInterval_txt, "wrap");


		//-----------------------------------------
		// Actions
		//-----------------------------------------
		l_updateActive_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				applyLocalSettings();
			}
		});
		
		l_updateActiveInterval_txt.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				applyLocalSettings();
			}
		});
		l_updateActiveInterval_txt.addFocusListener(new FocusListener()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				applyLocalSettings();
			}
			
			@Override
			public void focusGained(FocusEvent e) {}
		});


		return panel;
	}

	private void applyLocalSettings()
	{
		// Need TMP since we are going to save the configuration somewhere
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null) 
			return;

		conf.setProperty(CmPgReplication.PROPKEY_update_primary             , l_updateActive_chk.isSelected());
		conf.setProperty(CmPgReplication.PROPKEY_update_primaryIntervalInSec, StringUtil.parseLong(l_updateActiveInterval_txt.getText(), CmPgReplication.DEFAULT_update_primaryIntervalInSec));

		conf.save();

//		// If the 'l_showRemoteRows_chk' or 'l_showLiveRemoteData_chk' the table needs to be updated
//		getCm().fireTableDataChanged();

		// This will force the CM to re-initialize the SQL statement.
//		CountersModel cm = getCm().getCounterController().getCmByName(getName());
//		if (cm != null)
//			cm.setSql(null);
	}


}

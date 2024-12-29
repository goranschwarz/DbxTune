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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.sqlserver.CmActiveStatements;
import com.dbxtune.cm.sqlserver.CmOpenTransactions;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmOpenTransactionsPanel
extends TabularCntrPanel
{
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmOpenTransactions.CM_NAME;

	public CmOpenTransactionsPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// PINK = spid is BLOCKED by some other user
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocked");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number BlockedBySpid = (Number) adapter.getValue(adapter.getColumnIndex("BlockedBySpid"));
				if ( BlockedBySpid != null && BlockedBySpid.intValue() != 0 )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.PINK), null));

		// RED = spid is BLOCKING other spids from running
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocking");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number BlockingOtherSpidCount = (Number) adapter.getValue(adapter.getColumnIndex("BlockingOtherSpidCount"));
				if ( BlockingOtherSpidCount != null && BlockingOtherSpidCount.intValue() > 0 )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.RED), null));
	}
	

	private JCheckBox l_sampleLocksForSpid_chk;

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		LocalOptionsConfigPanel panel = new LocalOptionsConfigPanel("Local Options", new LocalOptionsConfigChanges()
		{
			@Override
			public void configWasChanged(String propName, String propVal)
			{
				Configuration conf = Configuration.getCombinedConfiguration();

//				list.add(new CmSettingsHelper("Get SPID Locks"           , PROPKEY_sample_spidLocks       , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_spidLocks       , DEFAULT_sample_spidLocks      ), DEFAULT_sample_spidLocks      , "Do 'select <i>someCols</i> from syslockinfo where spid = ?' on every row in the table. This will help us to diagnose what the current SQL statement is locking."));

				l_sampleLocksForSpid_chk.setSelected(conf.getBooleanProperty(CmOpenTransactions.PROPKEY_sample_spidLocks, CmOpenTransactions.DEFAULT_sample_spidLocks));

				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

//		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));
		panel.setToolTipText(
			"<html>" +
			"</html>");

		Configuration conf = Configuration.getCombinedConfiguration();
		l_sampleLocksForSpid_chk   = new JCheckBox("Get Locks for SPID",       conf == null ? CmActiveStatements.DEFAULT_sample_spidLocks     : conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_spidLocks    , CmActiveStatements.DEFAULT_sample_spidLocks));

		l_sampleLocksForSpid_chk  .setName(CmActiveStatements.PROPKEY_sample_spidLocks);

		l_sampleLocksForSpid_chk  .setToolTipText("<html>Do 'select <i>someCols</i> from syslockinfo where spid = ?' on every row in the table. This will help us to diagnose what the current SQL statement is locking.</html>");

		panel.add(l_sampleLocksForSpid_chk,   "wrap");

		l_sampleLocksForSpid_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmOpenTransactions.PROPKEY_sample_spidLocks, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		
		return panel;
	}
}

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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.CmMemoryGrants;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmMemoryGrantsPanel 
extends TabularCntrPanel
{
	private static final long serialVersionUID = 1L;

	private JCheckBox l_sampleMonSqltext_chk;
	private JCheckBox l_sampleShowplan_chk;
	private JCheckBox l_sampleLiveQueryPlan_chk;
//	private JCheckBox l_initLiveQueryPlan_chk;

	public CmMemoryGrantsPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
		// TODO: add color on grant < expected

		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// Mark the row as ORANGE if "waiting" for memory grant
		if (conf != null) colorStr = conf.getProperty(getName()+".color.waiting");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Object queueId = adapter.getValue(adapter.getColumnIndex("queue_id"));
				if (queueId != null && queueId instanceof Number)
				{
					if (((Number)queueId).intValue() > 0)
						return true;
				}
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));

		// Mark the row as PINK if granted < requested
		if (conf != null) colorStr = conf.getProperty(getName()+".color.granted-lt-requested");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Object requested_memory_kb = adapter.getValue(adapter.getColumnIndex("requested_memory_kb"));
				Object granted_memory_kb   = adapter.getValue(adapter.getColumnIndex("granted_memory_kb"));
				if (requested_memory_kb != null && granted_memory_kb != null && requested_memory_kb instanceof Number && granted_memory_kb instanceof Number)
				{
					if (((Number)granted_memory_kb).intValue() < ((Number)requested_memory_kb).intValue())
						return true;
				}
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.PINK), null));
	}

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));
		panel.setToolTipText(
			"<html>" +
//				"All the options in this panel executes additional SQL lookups in the database <b>after</b> the result set has been delivered.<br>" +
//				"This means that we are doing 1 extra SQL lookup for every checkbox option per row on the result set table.<br>" +
//				"<br>" +
//				"NOTE: So if you check all the options, the time to do refresh on this tab will <b>increase</b>." +
			"</html>");

		Configuration conf = Configuration.getCombinedConfiguration();
		l_sampleMonSqltext_chk     = new JCheckBox("Get SQL Text",                                conf == null ? CmMemoryGrants.DEFAULT_sample_sqlText       : conf.getBooleanProperty(CmMemoryGrants.PROPKEY_sample_sqlText,       CmMemoryGrants.DEFAULT_sample_sqlText));
		l_sampleShowplan_chk       = new JCheckBox("Get Query Plan",                              conf == null ? CmMemoryGrants.DEFAULT_sample_queryPlan     : conf.getBooleanProperty(CmMemoryGrants.PROPKEY_sample_queryPlan,     CmMemoryGrants.DEFAULT_sample_queryPlan));
		l_sampleLiveQueryPlan_chk  = new JCheckBox("Get Live Query Plan (2016 SP1 or later)",     conf == null ? CmMemoryGrants.DEFAULT_sample_liveQueryPlan : conf.getBooleanProperty(CmMemoryGrants.PROPKEY_sample_liveQueryPlan, CmMemoryGrants.DEFAULT_sample_liveQueryPlan));
//		l_sampleLiveQueryPlan_chk  = new JCheckBox("Get Live Query Plan (2019 or later)",         conf == null ? CmMemoryGrants.DEFAULT_sample_liveQueryPlan : conf.getBooleanProperty(CmMemoryGrants.PROPKEY_sample_liveQueryPlan, CmMemoryGrants.DEFAULT_sample_liveQueryPlan));
//		l_initLiveQueryPlan_chk    = new JCheckBox("Initialize Live Query Plan (2019 or later)",  conf == null ? CmMemoryGrants.DEFAULT_init_liveQueryPlan   : conf.getBooleanProperty(CmMemoryGrants.PROPKEY_init_liveQueryPlan,   CmMemoryGrants.DEFAULT_init_liveQueryPlan));

		l_sampleMonSqltext_chk    .setName(CmMemoryGrants.PROPKEY_sample_sqlText);
		l_sampleShowplan_chk      .setName(CmMemoryGrants.PROPKEY_sample_queryPlan);
		l_sampleLiveQueryPlan_chk .setName(CmMemoryGrants.PROPKEY_sample_liveQueryPlan);
//		l_initLiveQueryPlan_chk   .setName(CmMemoryGrants.PROPKEY_init_liveQueryPlan);

		l_sampleMonSqltext_chk    .setToolTipText("<html>Do 'Show SQLText' on every row in the table.<br>           This will help us to diagnose what SQL the client sent to the server.</html>");
		l_sampleShowplan_chk      .setToolTipText("<html>Do 'get query plan' on every row in the table.<br>         This will help us to diagnose if the current SQL statement is doing something funky.</html>");
		l_sampleLiveQueryPlan_chk .setToolTipText("<html>Use <code>sys.dm_exec_query_statistics_xml(spid)</code> instead of <code>sys.dm_exec_query_plan</code> on every row in the table.<br>       This will give us the LIVE Query Plan of each active session. <br>NOTE: Only available in SQL Server 2019 and later.</html>");
//		l_sampleLiveQueryPlan_chk .setToolTipText("<html>Use <code>sys.dm_exec_query_plan_stats</code> instead of <code>sys.dm_exec_query_plan</code> on every row in the table.<br>       This will give us the LIVE Query Plan of each active session. <br>NOTE: Only available in SQL Server 2019 and later.</html>");
//		l_initLiveQueryPlan_chk   .setToolTipText("<html>When this CM starts execute <code>dbcc traceon(2451, -1) with no_infomsgs</code>.<br>       This will enable us the LIVE Query Plan of each active session. <br>NOTE: Only available in SQL Server 2019 and later.</html>");

		panel.add(l_sampleMonSqltext_chk,     "wrap");
		panel.add(l_sampleShowplan_chk,       "wrap");
		panel.add(l_sampleLiveQueryPlan_chk,  "wrap");
//		panel.add(l_initLiveQueryPlan_chk,    "wrap");

		l_sampleMonSqltext_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmMemoryGrants.PROPKEY_sample_sqlText, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		l_sampleShowplan_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmMemoryGrants.PROPKEY_sample_queryPlan, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		l_sampleLiveQueryPlan_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmMemoryGrants.PROPKEY_sample_liveQueryPlan, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
//		l_initLiveQueryPlan_chk.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				// Need TMP since we are going to save the configuration somewhere
//				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
//				if (conf == null) return;
//				conf.setProperty(CmMemoryGrants.PROPKEY_init_liveQueryPlan, ((JCheckBox)e.getSource()).isSelected());
//				conf.save();
//				
//				// ReInitialize the SQL
//				getCm().setSql(null);
//			}
//		});
		
		return panel;
	}
//	@Override
//	public void checkLocalComponents()
//	{
//		CountersModel cm = getCm();
//		if (cm != null)
//		{
//			if (cm.isRuntimeInitialized())
//			{
//				// disable some options if we do not have 'sybase_ts_role'
//				if ( cm.isServerRoleOrPermissionActive(AseConnectionUtils.SYBASE_TS_ROLE))
//				{
//					l_sampleDbccSqltext_chk   .setEnabled(true);
//					l_sampleDbccStacktrace_chk.setEnabled(true);
//				}
//				else
//				{
//					l_sampleDbccSqltext_chk   .setEnabled(false);
//					l_sampleDbccStacktrace_chk.setEnabled(false);
//
//					Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
//					if (conf != null)
//					{
//						conf.setProperty(getName()+".sample.dbccSqltext",    false);
//						conf.setProperty(getName()+".sample.dbccStacktrace", false);
//					}
//				}
//			} // end isRuntimeInitialized
//		} // end (cm != null)
//	}
}

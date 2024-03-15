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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.CounterController;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.CmActiveStatements;
import com.asetune.graph.TrendGraphColors;
import com.asetune.gui.ChangeToJTabDialog;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmActiveStatementsPanel 
extends TabularCntrPanel
{
	private static final long serialVersionUID = 1L;

	public CmActiveStatementsPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{

		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// YELLOW = SYSTEM process
		if (conf != null) colorStr = conf.getProperty(getName()+".color.system");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Boolean isUserProcess = (Boolean) adapter.getValue(adapter.getColumnIndex("is_user_process"));
				if ( isUserProcess == null || (isUserProcess != null && isUserProcess == false) )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.YELLOW), null));


		// Mark the row as ORANGE if PK has been visible on more than 1 sample
		if (conf != null) colorStr = conf.getProperty(getName()+".color.multiSampled");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String multiSampled = adapter.getString(adapter.getColumnIndex("multiSampled"));
				if (multiSampled != null)
					multiSampled = multiSampled.trim();
				if ( ! "".equals(multiSampled))
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));

		
		// Mark the row as LIGHT_BLUE if this SPID is waiting for MEMORY GRANTS
		if (conf != null) colorStr = conf.getProperty(getName()+".color.waitingForMemoryGrant");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String memory_grant_wait_time_ms = adapter.getString(adapter.getColumnIndex("memory_grant_wait_time_ms"));
				if (memory_grant_wait_time_ms != null)
					memory_grant_wait_time_ms = memory_grant_wait_time_ms.trim();
				if ( ! "0".equals(memory_grant_wait_time_ms))
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, TrendGraphColors.LIGHT_BLUE), null));


		// Mark the row as PINK if this SPID is BLOCKED by another thread
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocked");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String blockingSpid = adapter.getString(adapter.getColumnIndex("ImBlockedBySessionId"));
				if (blockingSpid != null)
					blockingSpid = blockingSpid.trim();
				if ( ! "0".equals(blockingSpid))
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.PINK), null));


		// Mark the row as RED if blocks other users from working
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocking");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String listOfBlockedSpids = adapter.getString(adapter.getColumnIndex("ImBlockingOtherSessionIds"));
				String blockedBySessionId = adapter.getString(adapter.getColumnIndex("ImBlockedBySessionId"));
				if (listOfBlockedSpids != null)
					listOfBlockedSpids = listOfBlockedSpids.trim();
				if ( ! "".equals(listOfBlockedSpids) && "0".equals(blockedBySessionId))
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.RED), null));

	
		// Mark the CELL as GREEN if "dop" > 1
		if (conf != null) colorStr = conf.getProperty(getName()+".color.dop");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String colName = adapter.getColumnName(adapter.column);
				
				// ONLY check for cell named "dop"
				if ( "dop".equals(colName) && adapter.getValue() instanceof Number)
				{
					Number dop = (Number) adapter.getValue();
					return dop.intValue() > 1;
				}

				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.GREEN), null));
	}

	private JCheckBox l_sampleSystemSpids_chk;
	private JCheckBox l_sampleMonSqltext_chk;
//	private JCheckBox l_sampleDbccSqltext_chk;
//	private JCheckBox l_sampleProcCallStack_chk;
	private JCheckBox l_sampleShowplan_chk;
//	private JCheckBox l_sampleDbccStacktrace_chk;
	private JCheckBox l_sampleLiveQueryPlan_chk;
	private JCheckBox l_sampleHoldingLocks_chk;
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

//				list.add(new CmSettingsHelper("Sample System SPIDs"      , PROPKEY_sample_systemSpids  , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemSpids  , DEFAULT_sample_systemSpids  ), DEFAULT_sample_systemSpids  , "Sample System SPID's" ));
//				list.add(new CmSettingsHelper("Get Query Plan"           , PROPKEY_sample_showplan     , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_showplan     , DEFAULT_sample_showplan     ), DEFAULT_sample_showplan     , "Also get queryplan" ));
//				list.add(new CmSettingsHelper("Get SQL Text"             , PROPKEY_sample_monSqlText   , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_monSqlText   , DEFAULT_sample_monSqlText   ), DEFAULT_sample_monSqlText   , "Also get SQL Text"  ));
//				list.add(new CmSettingsHelper("Get Live Query Plan"      , PROPKEY_sample_liveQueryPlan, Boolean.class, conf.getBooleanProperty(PROPKEY_sample_liveQueryPlan, DEFAULT_sample_liveQueryPlan), DEFAULT_sample_liveQueryPlan, "Also get LIVE queryplan" ));
//				list.add(new CmSettingsHelper("Get SPID's holding locks" , PROPKEY_sample_holdingLocks , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_holdingLocks , DEFAULT_sample_holdingLocks ), DEFAULT_sample_holdingLocks , "Include SPID's that holds locks even if that are not active in the server." ));
//				list.add(new CmSettingsHelper("Get SPID Locks"           , PROPKEY_sample_spidLocks    , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_spidLocks    , DEFAULT_sample_spidLocks    ), DEFAULT_sample_spidLocks    , "Do 'select <i>someCols</i> from syslockinfo where spid = ?' on every row in the table. This will help us to diagnose what the current SQL statement is locking."));

				l_sampleSystemSpids_chk   .setSelected(conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_systemSpids,   CmActiveStatements.DEFAULT_sample_systemSpids));
				l_sampleShowplan_chk      .setSelected(conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_showplan,      CmActiveStatements.DEFAULT_sample_showplan));
				l_sampleMonSqltext_chk    .setSelected(conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_monSqlText,    CmActiveStatements.DEFAULT_sample_monSqlText));
				l_sampleLiveQueryPlan_chk .setSelected(conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_liveQueryPlan, CmActiveStatements.DEFAULT_sample_liveQueryPlan));
				l_sampleHoldingLocks_chk  .setSelected(conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_holdingLocks , CmActiveStatements.DEFAULT_sample_holdingLocks));
				l_sampleLocksForSpid_chk  .setSelected(conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_spidLocks    , CmActiveStatements.DEFAULT_sample_spidLocks));

				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

//		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));
		panel.setToolTipText(
			"<html>" +
//				"All the options in this panel executes additional SQL lookups in the database <b>after</b> the result set has been delivered.<br>" +
//				"This means that we are doing 1 extra SQL lookup for every checkbox option per row on the result set table.<br>" +
//				"<br>" +
//				"NOTE: So if you check all the options, the time to do refresh on this tab will <b>increase</b>." +
			"</html>");

		final JButton resetMoveToTab_but = new JButton("Reset 'Move to Tab' settings");

//		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		Configuration conf = Configuration.getCombinedConfiguration();
		l_sampleSystemSpids_chk    = new JCheckBox("Sample System SPID's",     conf == null ? CmActiveStatements.DEFAULT_sample_systemSpids   : conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_systemSpids,   CmActiveStatements.DEFAULT_sample_systemSpids));
//		l_sampleMonSqltext_chk     = new JCheckBox("Get Monitored SQL Text",   conf == null ? true : conf.getBooleanProperty(getName()+".sample.monSqltext",     true));
		l_sampleMonSqltext_chk     = new JCheckBox("Get SQL Text",             conf == null ? CmActiveStatements.DEFAULT_sample_monSqlText    : conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_monSqlText,    CmActiveStatements.DEFAULT_sample_monSqlText));
//		l_sampleDbccSqltext_chk    = new JCheckBox("Get DBCC SQL Text",        conf == null ? true : conf.getBooleanProperty(getName()+".sample.dbccSqltext",    false));
//		l_sampleProcCallStack_chk  = new JCheckBox("Get Procedure Call Stack", conf == null ? true : conf.getBooleanProperty(getName()+".sample.procCallStack",  true));
//		l_sampleShowplan_chk       = new JCheckBox("Get Showplan",             conf == null ? true : conf.getBooleanProperty(getName()+".sample.showplan",       true));
		l_sampleShowplan_chk       = new JCheckBox("Get Query Plan",           conf == null ? CmActiveStatements.DEFAULT_sample_showplan      : conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_showplan,      CmActiveStatements.DEFAULT_sample_showplan));
//		l_sampleDbccStacktrace_chk = new JCheckBox("Get ASE Stacktrace",       conf == null ? true : conf.getBooleanProperty(getName()+".sample.dbccStacktrace", false));
		l_sampleLiveQueryPlan_chk  = new JCheckBox("Get Live Query Plan",      conf == null ? CmActiveStatements.DEFAULT_sample_liveQueryPlan : conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_liveQueryPlan, CmActiveStatements.DEFAULT_sample_liveQueryPlan));
		l_sampleHoldingLocks_chk   = new JCheckBox("Show SPID's holding locks",conf == null ? CmActiveStatements.DEFAULT_sample_holdingLocks  : conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_holdingLocks , CmActiveStatements.DEFAULT_sample_holdingLocks));
		l_sampleLocksForSpid_chk   = new JCheckBox("Get Locks for SPID",       conf == null ? CmActiveStatements.DEFAULT_sample_spidLocks     : conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_spidLocks    , CmActiveStatements.DEFAULT_sample_spidLocks));

		l_sampleSystemSpids_chk   .setName(CmActiveStatements.PROPKEY_sample_systemSpids);
		l_sampleMonSqltext_chk    .setName(CmActiveStatements.PROPKEY_sample_monSqlText);
//		l_sampleDbccSqltext_chk   .setName(getName()+".sample.dbccSqltext");
//		l_sampleProcCallStack_chk .setName(getName()+".sample.procCallStack");
		l_sampleShowplan_chk      .setName(CmActiveStatements.PROPKEY_sample_showplan);
//		l_sampleDbccStacktrace_chk.setName(getName()+".sample.dbccStacktrace");
		l_sampleLiveQueryPlan_chk .setName(CmActiveStatements.PROPKEY_sample_liveQueryPlan);
		l_sampleHoldingLocks_chk  .setName(CmActiveStatements.PROPKEY_sample_holdingLocks);
		l_sampleLocksForSpid_chk  .setName(CmActiveStatements.PROPKEY_sample_spidLocks);
		
		l_sampleSystemSpids_chk   .setToolTipText("<html>Do 'Incluse System SPID's in the list.</html>");
		l_sampleMonSqltext_chk    .setToolTipText("<html>Do 'select SQLText from monProcessSQLText where SPID=spid' on every row in the table.<br>    This will help us to diagnose what SQL the client sent to the server.</html>");
//		l_sampleDbccSqltext_chk   .setToolTipText("<html>Do 'dbcc sqltext(spid)' on every row in the table.<br>     This will help us to diagnose what SQL the client sent to the server.<br><b>Note:</b> Role 'sybase_ts_role' is needed.</html>");
//		l_sampleProcCallStack_chk .setToolTipText("<html>Do 'select * from monProcessProcedures where SPID=spid.<br>This will help us to diagnose what stored procedure called before we ended up here.</html>");
		l_sampleShowplan_chk      .setToolTipText("<html>Do 'sp_showplan spid' on every row in the table.<br>       This will help us to diagnose if the current SQL statement is doing something funky.</html>");
//		l_sampleDbccStacktrace_chk.setToolTipText("<html>do 'dbcc stacktrace(spid)' on every row in the table.<br>  This will help us to diagnose what peace of code the ASE Server is currently executing.<br><b>Note:</b> Role 'sybase_ts_role' is needed.</html>");
		l_sampleLiveQueryPlan_chk .setToolTipText("<html>Do 'select query_plan from sys.dm_exec_query_statistics_xml(spid)' on every row in the table.<br>       This will give us the LIVE Query Plan of each active session.</html>");
		l_sampleHoldingLocks_chk  .setToolTipText("<html>Include SPID's that are holding <i>any</i> locks in dm_tran_locks.<br>This will help you trace Statements that havn't released it's locks and are <b>not</b> active. (meaning that the control is at the client side)</html>");
		l_sampleLocksForSpid_chk  .setToolTipText("<html>Do 'select <i>someCols</i> from syslockinfo where spid = ?' on every row in the table. This will help us to diagnose what the current SQL statement is locking.</html>");

		resetMoveToTab_but.setToolTipText(
				"<html>" +
				"Reset the option: To automatically switch to this tab when you have <b>blocking locks</b>.<br>" +
				"Next time this happens, a popup will ask you what you want to do." +
				"</html>");

		resetMoveToTab_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				ChangeToJTabDialog.resetSavedSettings(getPanelName());
				CounterController.getSummaryPanel().resetGoToTabSettings(getPanelName());
			}
		});

//		panel.add(l_sampleMonSqltext_chk,     "");
//		panel.add(l_sampleProcCallStack_chk,  "wrap");
//		panel.add(l_sampleDbccSqltext_chk,    "wrap");
//		panel.add(l_sampleShowplan_chk,       "wrap");
//		panel.add(l_sampleDbccStacktrace_chk, "wrap");

		panel.add(l_sampleSystemSpids_chk,    "");      // x  -
		panel.add(l_sampleHoldingLocks_chk,   "wrap");  // -  x
		panel.add(l_sampleMonSqltext_chk,     "");      // x  -
		panel.add(l_sampleLocksForSpid_chk,   "wrap");  // -  x
		panel.add(l_sampleShowplan_chk,       "wrap");  // x  -
		panel.add(l_sampleLiveQueryPlan_chk,  "wrap");  // x  -
		panel.add(resetMoveToTab_but,         "wrap");  // x  -

		l_sampleSystemSpids_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmActiveStatements.PROPKEY_sample_systemSpids, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// re-create the SQL Statement on next sample
				getCm().setSql(null);
			}
		});
		l_sampleMonSqltext_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmActiveStatements.PROPKEY_sample_monSqlText, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
			}
		});
//		l_sampleDbccSqltext_chk.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				// Need TMP since we are going to save the configuration somewhere
//				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
//				if (conf == null) return;
//				conf.setProperty(getName()+".sample.dbccSqltext", ((JCheckBox)e.getSource()).isSelected());
//				conf.save();
//			}
//		});
//		l_sampleProcCallStack_chk.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				// Need TMP since we are going to save the configuration somewhere
//				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
//				if (conf == null) return;
//				conf.setProperty(getName()+".sample.procCallStack", ((JCheckBox)e.getSource()).isSelected());
//				conf.save();
//			}
//		});
		l_sampleShowplan_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmActiveStatements.PROPKEY_sample_showplan, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
			}
		});
//		l_sampleDbccStacktrace_chk.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				// Need TMP since we are going to save the configuration somewhere
//				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
//				if (conf == null) return;
//				conf.setProperty(getName()+".sample.dbccStacktrace", ((JCheckBox)e.getSource()).isSelected());
//				conf.save();
//			}
//		});
		l_sampleLiveQueryPlan_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmActiveStatements.PROPKEY_sample_liveQueryPlan, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
			}
		});
		l_sampleHoldingLocks_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmActiveStatements.PROPKEY_sample_holdingLocks, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		
		l_sampleLocksForSpid_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmActiveStatements.PROPKEY_sample_spidLocks, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		
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

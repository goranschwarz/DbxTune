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
import com.dbxtune.cm.CountersModel.ClearOption;
import com.dbxtune.cm.sqlserver.CmWhoIsActive;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.gui.swing.GCheckBox;
import com.dbxtune.pcs.PersistWriterJdbc.GraphStorageType;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmWhoIsActivePanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmWhoIsActivePanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmProcessActivity.CM_NAME;

	public static final String  TOOLTIP_sample_systemThreads   = "<html> <code>@show_system_spids    = 1 </code> <BR> Retrieve data about system sessions</html>";
	public static final String  TOOLTIP_sample_memoryInfo      = "<html> <code>@get_memory_info      = 1 </code> <BR> Get additional information related to workspace memory: <b>requested_memory, granted_memory, max_used_memory, and memory_info</b>.</html>";
	public static final String  TOOLTIP_sample_locks           = "<html> <code>@get_locks            = 1 </code> <BR> Gets associated locks for each request, aggregated in an XML format</html>";
	public static final String  TOOLTIP_sample_transactionInfo = "<html> <code>@get_transaction_info = 1 </code> <BR> Enables pulling transaction log write info, transaction duration, and the implicit_transaction identification column</html>";
	public static final String  TOOLTIP_sample_outerCommand    = "<html> <code>@get_outer_command    = 1 </code> <BR> Get the associated outer ad hoc query or stored procedure call, if available</html>";
	public static final String  TOOLTIP_sample_plans           = "<html> <code>@get_plans            = 1 </code> <BR> Get associated query plans for running tasks, if available</html>";
	public static final String  TOOLTIP_sample_blockLeaders    = "<html> <code>@find_block_leaders   = 1 </code> <BR> Walk the blocking chain and count the number of total SPIDs blocked all the way down by a given session. Also enables task_info Level 1, if @get_task_info is set to 0</html>";
	public static final String  TOOLTIP_sample_additionalInfo  = "<html> <code>@get_additional_info  = 1 </code> <BR> Get additional non-performance-related information about the session or request: <BR>"
	                                                                                                                  + "    &emsp;<b>text_size, language, date_format, date_first, quoted_identifier, arithabort, ansi_null_dflt_on, ansi_defaults, ansi_warnings, ansi_padding, ansi_nulls, concat_null_yields_null, transaction_isolation_level, lock_timeout, deadlock_priority, row_count, command_type</b> <BR>"
	                                                                                                                  + "<BR> "
	                                                                                                                  + "If a SQL Agent job is running, an subnode called agent_info will be populated with some or all of the following: <BR>"
	                                                                                                                  + "    &emsp; <b>job_id, job_name, step_id, step_name, msdb_query_error</b> (in the event of an error) <BR>"
	                                                                                                                  + "<BR> "
	                                                                                                                  + "If @get_task_info is set to 2 and a lock wait is detected, a subnode called block_info will be populated with some or all of the following:<BR>"
	                                                                                                                  + "    &emsp; <b>lock_type, database_name, object_id, file_id, hobt_id, applock_hash, metadata_resource, metadata_class_id, object_name, schema_name</b>"
	                                                                                                                  + "</html>";
	public static final String  TOOLTIP_sample_sleepingSpids   = "<html> <code>@show_sleeping_spids  = 2 </code> <BR> true = 'pulls all sleeping SPIDs' <BR>false = 'pulls only those sleeping SPIDs that also have an open transaction'</html>";
	public static final String  TOOLTIP_sample_taskInfo        = "<html> <code>@get_task_info        = 2 </code> <BR> true = 'pulls all available task-based metrics, including: <b>number of active tasks, current wait stats, physical I/O, context switches, and blocker information</b>', <BR>false = 'a lightweight mode that pulls the top non-CXPACKET wait, giving preference to blockers'</html>";

	private GCheckBox l_sampleSystemThreads_chk;
	private GCheckBox l_sampleMemoryInfo_chk;
	private GCheckBox l_sampleLocks_chk;
	private GCheckBox l_sampleTransactionInfo_chk;
	private GCheckBox l_sampleOuterCommand_chk;
	private GCheckBox l_samplePlans_chk;
	private GCheckBox l_sampleBlockLeaders_chk;
	private GCheckBox l_sampleAdditionalInfo_chk;
	private GCheckBox l_sampleSleepingSpids_chk;
	private GCheckBox l_sampleTaskInfo_chk;

	public CmWhoIsActivePanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		//-------------------------------------------
		// YELLOW = SYSTEM process
		if (conf != null) colorStr = conf.getProperty(getName()+".color.system");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				// possible session_id < 50
				String status = adapter.getString(adapter.getColumnIndex("status"));
				if ( status != null && status.equals("background") )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.YELLOW), null));

		
		//-------------------------------------------
//		// GREEN = RUNNING or RUNNABLE process
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.running");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				String status = (String) adapter.getValue(adapter.getColumnIndex("status"));
//				if ( status != null && (status.startsWith("running") || status.startsWith("runnable")) )
//					return true;
//				return false;
//			}
//		}, SwingUtils.parseColor(colorStr, Color.GREEN), null));

		
		//-------------------------------------------
		// ORANGE = spid has OpenTrans
		if (conf != null) colorStr = conf.getProperty(getName()+".color.opentran");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				int pos_open_tran_count = adapter.getColumnIndex("open_tran_count");
				if (pos_open_tran_count == -1)
					return false;
				Number open_tran_count = (Number) adapter.getValue(pos_open_tran_count);
				if ( open_tran_count != null && open_tran_count.intValue() != 0 )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));

		
		//-------------------------------------------
		// LIGHT_GREEN = suspended process
		if (conf != null) colorStr = conf.getProperty(getName()+".color.suspended");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String status = (String) adapter.getValue(adapter.getColumnIndex("status"));
				if ( status != null && status.startsWith("suspended") )
					return true;
				return false;
			}
//		}, SwingUtils.parseColor(colorStr, ColorUtils.VERY_LIGHT_GREEN), null));
		}, SwingUtils.parseColor(colorStr, new Color(212, 255, 163)), null));
		

		//-------------------------------------------
		// PINK = spid is BLOCKED by some other user
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocked");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				int pos_blocking_session_id = adapter.getColumnIndex("blocking_session_id");
				if (pos_blocking_session_id == -1)
					return false;
				Number blockingSpid = (Number) adapter.getValue(pos_blocking_session_id);
				if ( blockingSpid != null && blockingSpid.intValue() != 0 )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.PINK), null));


		//-------------------------------------------
		// RED = spid is BLOCKING other spids from running
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocking");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				int pos_blocking_session_id   = adapter.getColumnIndex("blocking_session_id");
				int pos_blocked_session_count = adapter.getColumnIndex("blocked_session_count");
				if (pos_blocking_session_id == -1 || pos_blocked_session_count == -1)
					return false;

				Number blocking_session_id   = (Number) adapter.getValue(pos_blocking_session_id);
				Number blocked_session_count = (Number) adapter.getValue(pos_blocked_session_count);
				
				if ( blocking_session_id == null || blocked_session_count == null )
					return false;

				if ( blocking_session_id.intValue() == 0 && blocked_session_count.intValue() > 0)
					return true;

				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.RED), null));

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

//				list.add(new CmSettingsHelper("Sample System Threads", PROPKEY_sample_systemThreads , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemThreads  , DEFAULT_sample_systemThreads  ), DEFAULT_sample_systemThreads, CmWhoPanel.TOOLTIP_sample_systemThreads ));

				l_sampleSystemThreads_chk  .setSelected(conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_systemThreads  , CmWhoIsActive.DEFAULT_sample_systemThreads  ));
				l_sampleMemoryInfo_chk     .setSelected(conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_memoryInfo     , CmWhoIsActive.DEFAULT_sample_memoryInfo     ));
				l_sampleLocks_chk          .setSelected(conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_locks          , CmWhoIsActive.DEFAULT_sample_locks          ));
				l_sampleTransactionInfo_chk.setSelected(conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_transactionInfo, CmWhoIsActive.DEFAULT_sample_transactionInfo));
				l_sampleOuterCommand_chk   .setSelected(conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_outerCommand   , CmWhoIsActive.DEFAULT_sample_outerCommand   ));
				l_samplePlans_chk          .setSelected(conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_plans          , CmWhoIsActive.DEFAULT_sample_plans          ));
				l_sampleBlockLeaders_chk   .setSelected(conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_blockLeaders   , CmWhoIsActive.DEFAULT_sample_blockLeaders   ));
				l_sampleAdditionalInfo_chk .setSelected(conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_additionalInfo , CmWhoIsActive.DEFAULT_sample_additionalInfo ));
				l_sampleSleepingSpids_chk  .setSelected(conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_sleepingSpids  , CmWhoIsActive.DEFAULT_sample_sleepingSpids  ));
				l_sampleTaskInfo_chk       .setSelected(conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_taskInfo       , CmWhoIsActive.DEFAULT_sample_taskInfo       ));

				// If the 'l_sampleSystemThreads_chk' the table needs to be updated... so that filters are applied
				getCm().fireTableDataChanged();

				// ReInitialize the SQL
				getCm().setSql(null);
				getCm().clear(ClearOption.COLUMN_CHANGES);

			}
		});

//		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));
		panel.setToolTipText("<html><b>NOTE:</b> When any of this settings are changed, The Counter Model will have to be <b>re-initialized</b> due to new columns will be added/removed.</html>");

		Configuration conf = Configuration.getCombinedConfiguration();
		l_sampleSystemThreads_chk   = new GCheckBox("Show System Spid's"      , conf == null ? CmWhoIsActive.DEFAULT_sample_systemThreads  : conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_systemThreads  , CmWhoIsActive.DEFAULT_sample_systemThreads  ));
		l_sampleMemoryInfo_chk      = new GCheckBox("Show Memory Info"        , conf == null ? CmWhoIsActive.DEFAULT_sample_memoryInfo     : conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_memoryInfo     , CmWhoIsActive.DEFAULT_sample_memoryInfo     ));
		l_sampleLocks_chk           = new GCheckBox("Show Lock Info"          , conf == null ? CmWhoIsActive.DEFAULT_sample_locks          : conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_locks          , CmWhoIsActive.DEFAULT_sample_locks          ));
		l_sampleTransactionInfo_chk = new GCheckBox("Show Transaction Info"   , conf == null ? CmWhoIsActive.DEFAULT_sample_transactionInfo: conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_transactionInfo, CmWhoIsActive.DEFAULT_sample_transactionInfo));
		l_sampleOuterCommand_chk    = new GCheckBox("Show Outer Command"      , conf == null ? CmWhoIsActive.DEFAULT_sample_outerCommand   : conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_outerCommand   , CmWhoIsActive.DEFAULT_sample_outerCommand   ));
		l_samplePlans_chk           = new GCheckBox("Show Execution Plan"     , conf == null ? CmWhoIsActive.DEFAULT_sample_plans          : conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_plans          , CmWhoIsActive.DEFAULT_sample_plans          ));
		l_sampleBlockLeaders_chk    = new GCheckBox("Show Block Leader"       , conf == null ? CmWhoIsActive.DEFAULT_sample_blockLeaders   : conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_blockLeaders   , CmWhoIsActive.DEFAULT_sample_blockLeaders   ));
		l_sampleAdditionalInfo_chk  = new GCheckBox("Show Additional Info"    , conf == null ? CmWhoIsActive.DEFAULT_sample_additionalInfo : conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_additionalInfo , CmWhoIsActive.DEFAULT_sample_additionalInfo ));
		l_sampleSleepingSpids_chk   = new GCheckBox("Show Sleeping Spid's"    , conf == null ? CmWhoIsActive.DEFAULT_sample_sleepingSpids  : conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_sleepingSpids  , CmWhoIsActive.DEFAULT_sample_sleepingSpids  ));
		l_sampleTaskInfo_chk        = new GCheckBox("Show Extended Task Info" , conf == null ? CmWhoIsActive.DEFAULT_sample_taskInfo       : conf.getBooleanProperty(CmWhoIsActive.PROPKEY_sample_taskInfo       , CmWhoIsActive.DEFAULT_sample_taskInfo       ));

		l_sampleSystemThreads_chk  .setName(CmWhoIsActive.PROPKEY_sample_systemThreads  );
		l_sampleMemoryInfo_chk     .setName(CmWhoIsActive.PROPKEY_sample_memoryInfo     );
		l_sampleLocks_chk          .setName(CmWhoIsActive.PROPKEY_sample_locks          );
		l_sampleTransactionInfo_chk.setName(CmWhoIsActive.PROPKEY_sample_transactionInfo);
		l_sampleOuterCommand_chk   .setName(CmWhoIsActive.PROPKEY_sample_outerCommand   );
		l_samplePlans_chk          .setName(CmWhoIsActive.PROPKEY_sample_plans          );
		l_sampleBlockLeaders_chk   .setName(CmWhoIsActive.PROPKEY_sample_blockLeaders   );
		l_sampleAdditionalInfo_chk .setName(CmWhoIsActive.PROPKEY_sample_additionalInfo );
		l_sampleSleepingSpids_chk  .setName(CmWhoIsActive.PROPKEY_sample_sleepingSpids  );
		l_sampleTaskInfo_chk       .setName(CmWhoIsActive.PROPKEY_sample_taskInfo       );

		l_sampleSystemThreads_chk  .setToolTipText(TOOLTIP_sample_systemThreads  );
		l_sampleMemoryInfo_chk     .setToolTipText(TOOLTIP_sample_memoryInfo     );
		l_sampleLocks_chk          .setToolTipText(TOOLTIP_sample_locks          );
		l_sampleTransactionInfo_chk.setToolTipText(TOOLTIP_sample_transactionInfo);
		l_sampleOuterCommand_chk   .setToolTipText(TOOLTIP_sample_outerCommand   );
		l_samplePlans_chk          .setToolTipText(TOOLTIP_sample_plans          );
		l_sampleBlockLeaders_chk   .setToolTipText(TOOLTIP_sample_blockLeaders   );
		l_sampleAdditionalInfo_chk .setToolTipText(TOOLTIP_sample_additionalInfo );
		l_sampleSleepingSpids_chk  .setToolTipText(TOOLTIP_sample_sleepingSpids  );
		l_sampleTaskInfo_chk       .setToolTipText(TOOLTIP_sample_taskInfo       );

		ActionListener actionListener = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Object source = e.getSource();
				if (source instanceof JCheckBox)
				{
					JCheckBox chk = (JCheckBox) source;
					String name = chk.getName();
					
					Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
					if (conf == null) 
						return;

//System.out.println("CmWhoIsActivePanel.actionListener: setProperty('" + name + "', " + chk.isSelected() + ");");
					tmpConf.setProperty(name, chk.isSelected());
					tmpConf.save();

					CountersModel cm = getCm();
					
					// ReInitialize the SQL
					cm.setSql(null);

					// The below option will NOT change COLUMNS, so no need to re-initialize the CM
					if (StringUtil.containsAny(name, CmWhoIsActive.PROPKEY_sample_systemThreads, CmWhoIsActive.PROPKEY_sample_sleepingSpids))
						return;

					// Some options changes number of columns that are collected/displayed
					// So we need to CLEAR the CM (and start all over with initialization)
					cm.clear(ClearOption.COLUMN_CHANGES);
				}
			}
		};
		
		l_sampleSystemThreads_chk  .addActionListener(actionListener);
		l_sampleMemoryInfo_chk     .addActionListener(actionListener);
		l_sampleLocks_chk          .addActionListener(actionListener);
		l_sampleTransactionInfo_chk.addActionListener(actionListener);
		l_sampleOuterCommand_chk   .addActionListener(actionListener);
		l_samplePlans_chk          .addActionListener(actionListener);
		l_sampleBlockLeaders_chk   .addActionListener(actionListener);
		l_sampleAdditionalInfo_chk .addActionListener(actionListener);
		l_sampleSleepingSpids_chk  .addActionListener(actionListener);
		l_sampleTaskInfo_chk       .addActionListener(actionListener);

		
		panel.add(l_sampleSystemThreads_chk  , "");
		panel.add(l_sampleMemoryInfo_chk     , "wrap");
		panel.add(l_sampleLocks_chk          , "");
		panel.add(l_sampleTransactionInfo_chk, "wrap");
		panel.add(l_sampleOuterCommand_chk   , "");
		panel.add(l_samplePlans_chk          , "wrap");
		panel.add(l_sampleBlockLeaders_chk   , "");
		panel.add(l_sampleAdditionalInfo_chk , "wrap");
		panel.add(l_sampleSleepingSpids_chk  , "");
		panel.add(l_sampleTaskInfo_chk       , "wrap");

		return panel;
	}
}

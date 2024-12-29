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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jfree.chart.ChartColor;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.postgres.CmActiveStatements;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmActiveStatementsPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmPgActivityPanel.class);
	private static final long    serialVersionUID      = 1L;

	public CmActiveStatementsPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

//		TODO;
//		// Mark the row as ORANGE if PK has been visible on more than 1 sample
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.multiSampled");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				String multiSampled = adapter.getString(adapter.getColumnIndex("multiSampled"));
//				if (multiSampled != null)
//					multiSampled = multiSampled.trim();
//				if ( ! "".equals(multiSampled))
//					return true;
//				return false;
//			}
//		}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));

		// GREEN = active
		if (conf != null) colorStr = conf.getProperty(getName()+".color.active");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String status = (String) adapter.getValue(adapter.getColumnIndex("state"));
//				if ( status != null && (status.startsWith("running") || status.startsWith("runnable")) )
//					return true;
				if ( status != null && status.equals("active") )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.GREEN), null));

		// YELLOW = idle in transaction
		if (conf != null) colorStr = conf.getProperty(getName()+".color.idle_in_transaction");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String status = (String) adapter.getValue(adapter.getColumnIndex("state"));
				if ( status != null && status.equals("idle in transaction") )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.YELLOW), null));

		// Mark the row as ORANGE if PK has been visible on more than 1 sample
		if (conf != null) colorStr = conf.getProperty(getName()+".color.multi_sampled");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String multiSampled = adapter.getString(adapter.getColumnIndex("multi_sampled"));
				if (multiSampled != null)
					multiSampled = multiSampled.trim();
				if ( ! "".equals(multiSampled))
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));

//		// PINK = idle in transaction (aborted)
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.idle_in_transaction");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				String status = (String) adapter.getValue(adapter.getColumnIndex("state"));
//				if ( status != null && status.equals("idle in transaction (aborted)") )
//					return true;
//				return false;
//			}
//		}, SwingUtils.parseColor(colorStr, Color.PINK), null));

		// Mark the row as PINK if this SPID is BLOCKED by another thread
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocked");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (adapter.getColumnIndex("im_blocking_other_pids") == -1)
					return false;

				String blockingSpid = adapter.getString(adapter.getColumnIndex("im_blocked_by_pids"));
				if (StringUtil.hasValue(blockingSpid))
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
				if (adapter.getColumnIndex("im_blocking_other_pids") == -1)
					return false;

				String listOfBlockedSpids = adapter.getString(adapter.getColumnIndex("im_blocking_other_pids"));
				String blockedBySessionId = adapter.getString(adapter.getColumnIndex("im_blocked_by_pids"));

				if ( StringUtil.hasValue(listOfBlockedSpids) && StringUtil.isNullOrBlank(blockedBySessionId))
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.RED), null));

		// At the END, because it will only color ONE CELL
		// LIGHT_BLUE = Has Exclusive lock
		if (conf != null) colorStr = conf.getProperty(getName()+".color.has_exlusive_lock");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if ("pid_exlock_count".equals(adapter.getColumnName(adapter.column)))
				{
					Object o_exclLockCount = adapter.getValue();
					if (o_exclLockCount instanceof Number)
					{
						if (((Number)o_exclLockCount).intValue() > 0)
							return true;
					}
				}
				return false;
			}
		}, SwingUtils.parseColor(colorStr, ChartColor.VERY_LIGHT_BLUE), null));
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

//				list.add(new CmSettingsHelper("Get PID's Locks"           , PROPKEY_sample_pidLocks    , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_pidLocks    , DEFAULT_sample_pidLocks    ), DEFAULT_sample_pidLocks    , "Do 'select <i>someCols</i> from pg_locks where pid = ?' on every row in the table. This will help us to diagnose what the current SQL statement is locking."));

				l_sampleLocksForSpid_chk  .setSelected(conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_pidLocks    , CmActiveStatements.DEFAULT_sample_pidLocks));

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
		l_sampleLocksForSpid_chk   = new JCheckBox("Get Locks for PID's",       conf == null ? CmActiveStatements.DEFAULT_sample_pidLocks     : conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_pidLocks    , CmActiveStatements.DEFAULT_sample_pidLocks));

		l_sampleLocksForSpid_chk  .setName(CmActiveStatements.PROPKEY_sample_pidLocks);
		
		l_sampleLocksForSpid_chk  .setToolTipText("<html>Do 'select <i>someCols</i> from pg_locks where pid = ?' on every row in the table. This will help us to diagnose what the current SQL statement is locking.</html>");

		panel.add(l_sampleLocksForSpid_chk,   "wrap");

		
		l_sampleLocksForSpid_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmActiveStatements.PROPKEY_sample_pidLocks, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		
		return panel;
	}

}

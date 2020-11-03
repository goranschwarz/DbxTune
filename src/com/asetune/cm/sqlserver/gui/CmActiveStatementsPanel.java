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
import com.asetune.gui.ChangeToJTabDialog;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmActiveStatementsPanel 
extends TabularCntrPanel
{
	private static final long serialVersionUID = 1L;

	private JCheckBox l_sampleMonSqltext_chk;
//	private JCheckBox l_sampleDbccSqltext_chk;
//	private JCheckBox l_sampleProcCallStack_chk;
	private JCheckBox l_sampleShowplan_chk;
//	private JCheckBox l_sampleDbccStacktrace_chk;

	public CmActiveStatementsPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{

		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

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

		final JButton resetMoveToTab_but = new JButton("Reset 'Move to Tab' settings");

//		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		Configuration conf = Configuration.getCombinedConfiguration();
//		l_sampleMonSqltext_chk     = new JCheckBox("Get Monitored SQL Text",   conf == null ? true : conf.getBooleanProperty(getName()+".sample.monSqltext",     true));
		l_sampleMonSqltext_chk     = new JCheckBox("Get SQL Text",             conf == null ? true : conf.getBooleanProperty(getName()+".sample.monSqltext",     true));
//		l_sampleDbccSqltext_chk    = new JCheckBox("Get DBCC SQL Text",        conf == null ? true : conf.getBooleanProperty(getName()+".sample.dbccSqltext",    false));
//		l_sampleProcCallStack_chk  = new JCheckBox("Get Procedure Call Stack", conf == null ? true : conf.getBooleanProperty(getName()+".sample.procCallStack",  true));
//		l_sampleShowplan_chk       = new JCheckBox("Get Showplan",             conf == null ? true : conf.getBooleanProperty(getName()+".sample.showplan",       true));
		l_sampleShowplan_chk       = new JCheckBox("Get Query Plan",           conf == null ? true : conf.getBooleanProperty(getName()+".sample.showplan",       true));
//		l_sampleDbccStacktrace_chk = new JCheckBox("Get ASE Stacktrace",       conf == null ? true : conf.getBooleanProperty(getName()+".sample.dbccStacktrace", false));

		l_sampleMonSqltext_chk    .setName(getName()+".sample.monSqltext");
//		l_sampleDbccSqltext_chk   .setName(getName()+".sample.dbccSqltext");
//		l_sampleProcCallStack_chk .setName(getName()+".sample.procCallStack");
		l_sampleShowplan_chk      .setName(getName()+".sample.showplan");
//		l_sampleDbccStacktrace_chk.setName(getName()+".sample.dbccStacktrace");
		
		l_sampleMonSqltext_chk    .setToolTipText("<html>Do 'select SQLText from monProcessSQLText where SPID=spid' on every row in the table.<br>    This will help us to diagnose what SQL the client sent to the server.</html>");
//		l_sampleDbccSqltext_chk   .setToolTipText("<html>Do 'dbcc sqltext(spid)' on every row in the table.<br>     This will help us to diagnose what SQL the client sent to the server.<br><b>Note:</b> Role 'sybase_ts_role' is needed.</html>");
//		l_sampleProcCallStack_chk .setToolTipText("<html>Do 'select * from monProcessProcedures where SPID=spid.<br>This will help us to diagnose what stored procedure called before we ended up here.</html>");
		l_sampleShowplan_chk      .setToolTipText("<html>Do 'sp_showplan spid' on every row in the table.<br>       This will help us to diagnose if the current SQL statement is doing something funky.</html>");
//		l_sampleDbccStacktrace_chk.setToolTipText("<html>do 'dbcc stacktrace(spid)' on every row in the table.<br>  This will help us to diagnose what peace of code the ASE Server is currently executing.<br><b>Note:</b> Role 'sybase_ts_role' is needed.</html>");

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

		panel.add(l_sampleMonSqltext_chk,     "wrap");
		panel.add(l_sampleShowplan_chk,       "wrap");
		panel.add(resetMoveToTab_but,         "wrap");

		l_sampleMonSqltext_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(getName()+".sample.monSqltext", ((JCheckBox)e.getSource()).isSelected());
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
				conf.setProperty(getName()+".sample.showplan", ((JCheckBox)e.getSource()).isSelected());
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

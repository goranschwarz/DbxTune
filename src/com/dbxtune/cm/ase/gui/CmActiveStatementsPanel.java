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
package com.dbxtune.cm.ase.gui;

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
import com.dbxtune.cm.ase.CmActiveStatements;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;
import com.dbxtune.utils.Ver;

import net.miginfocom.swing.MigLayout;

public class CmActiveStatementsPanel 
extends TabularCntrPanel 
{
	private static final long serialVersionUID = 1L;

	private JCheckBox l_sampleMonSqltext_chk;
	private JCheckBox l_sampleDbccSqltext_chk;
	private JCheckBox l_sampleProcCallStack_chk;
	private JCheckBox l_sampleShowplan_chk;
	private JCheckBox l_sampleDbccStacktrace_chk;
	private JCheckBox l_sampleCachedPlanInXml_chk;
	private JCheckBox l_sampleHoldingLocks_chk;
	private JCheckBox l_sampleSpidLocks_chk;

	public CmActiveStatementsPanel(CountersModel cm)
	{
		super(cm);

//		if (cm.getIconFile() != null)
//			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{

		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// Mark the row as YELLOW if holding any locks
		if (conf != null) colorStr = conf.getProperty(getName()+".color.holdingLocks");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String monSource          = adapter.getString(adapter.getColumnIndex("monSource"));
				if ( "HOLDING-LOCKS".equals(monSource))
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

		// Mark the row as PINK if this SPID is BLOCKED by another thread
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocked");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String blockingSpid = adapter.getString(adapter.getColumnIndex("BlockingSPID"));
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
				String listOfBlockedSpids = adapter.getString(adapter.getColumnIndex("BlockingOtherSpids"));
				String monSource          = adapter.getString(adapter.getColumnIndex("monSource"));
				if (listOfBlockedSpids != null)
					listOfBlockedSpids = listOfBlockedSpids.trim();
				if ( ! "".equals(listOfBlockedSpids))
					return true;
				if ( "BLOCKER".equals(monSource))
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

				l_sampleShowplan_chk        .setSelected(conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_showplan        , CmActiveStatements.DEFAULT_sample_showplan       ));
				l_sampleMonSqltext_chk      .setSelected(conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_monSqlText      , CmActiveStatements.DEFAULT_sample_monSqlText     ));
				l_sampleDbccSqltext_chk     .setSelected(conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_dbccSqlText     , CmActiveStatements.DEFAULT_sample_dbccSqlText    ));
				l_sampleProcCallStack_chk   .setSelected(conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_procCallStack   , CmActiveStatements.DEFAULT_sample_procCallStack  ));
				l_sampleDbccStacktrace_chk  .setSelected(conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_dbccStacktrace  , CmActiveStatements.DEFAULT_sample_dbccStacktrace ));
				l_sampleCachedPlanInXml_chk .setSelected(conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_cachedPlanInXml , CmActiveStatements.DEFAULT_sample_cachedPlanInXml));
				l_sampleHoldingLocks_chk    .setSelected(conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_holdingLocks    , CmActiveStatements.DEFAULT_sample_holdingLocks   ));
				l_sampleSpidLocks_chk       .setSelected(conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_spidLocks       , CmActiveStatements.DEFAULT_sample_spidLocks      ));

				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

//		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));
		panel.setToolTipText(
			"<html>" +
				"All the options in this panel executes additional SQL lookups in the database <b>after</b> the result set has been delivered.<br>" +
				"This means that we are doing 1 extra SQL lookup for every checkbox option per row on the result set table.<br>" +
				"<br>" +
				"NOTE: So if you check all the options, the time to do refresh on this tab will <b>increase</b>." +
			"</html>");

		Configuration conf = Configuration.getCombinedConfiguration();
		l_sampleShowplan_chk        = new JCheckBox("Get Showplan",              conf == null ? CmActiveStatements.DEFAULT_sample_showplan        : conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_showplan        , CmActiveStatements.DEFAULT_sample_showplan       ));
		l_sampleMonSqltext_chk      = new JCheckBox("Get Monitored SQL Text",    conf == null ? CmActiveStatements.DEFAULT_sample_monSqlText      : conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_monSqlText      , CmActiveStatements.DEFAULT_sample_monSqlText     ));
		l_sampleDbccSqltext_chk     = new JCheckBox("Get DBCC SQL Text",         conf == null ? CmActiveStatements.DEFAULT_sample_dbccSqlText     : conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_dbccSqlText     , CmActiveStatements.DEFAULT_sample_dbccSqlText    ));
		l_sampleProcCallStack_chk   = new JCheckBox("Get Procedure Call Stack",  conf == null ? CmActiveStatements.DEFAULT_sample_procCallStack   : conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_procCallStack   , CmActiveStatements.DEFAULT_sample_procCallStack  ));
		l_sampleDbccStacktrace_chk  = new JCheckBox("Get ASE Stacktrace",        conf == null ? CmActiveStatements.DEFAULT_sample_dbccStacktrace  : conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_dbccStacktrace  , CmActiveStatements.DEFAULT_sample_dbccStacktrace ));
		l_sampleCachedPlanInXml_chk = new JCheckBox("Show Cached Plan in XML",   conf == null ? CmActiveStatements.DEFAULT_sample_cachedPlanInXml : conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_cachedPlanInXml , CmActiveStatements.DEFAULT_sample_cachedPlanInXml));
		l_sampleHoldingLocks_chk    = new JCheckBox("Show SPID's holding locks", conf == null ? CmActiveStatements.DEFAULT_sample_holdingLocks    : conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_holdingLocks    , CmActiveStatements.DEFAULT_sample_holdingLocks   ));
		l_sampleSpidLocks_chk       = new JCheckBox("Get SPID Locks",            conf == null ? CmActiveStatements.DEFAULT_sample_spidLocks       : conf.getBooleanProperty(CmActiveStatements.PROPKEY_sample_spidLocks       , CmActiveStatements.DEFAULT_sample_spidLocks      ));

		l_sampleShowplan_chk       .setName(CmActiveStatements.PROPKEY_sample_showplan       );
		l_sampleMonSqltext_chk     .setName(CmActiveStatements.PROPKEY_sample_monSqlText     );
		l_sampleDbccSqltext_chk    .setName(CmActiveStatements.PROPKEY_sample_dbccSqlText    );
		l_sampleProcCallStack_chk  .setName(CmActiveStatements.PROPKEY_sample_procCallStack  );
		l_sampleDbccStacktrace_chk .setName(CmActiveStatements.PROPKEY_sample_dbccStacktrace );
		l_sampleCachedPlanInXml_chk.setName(CmActiveStatements.PROPKEY_sample_cachedPlanInXml);
		l_sampleHoldingLocks_chk   .setName(CmActiveStatements.PROPKEY_sample_holdingLocks   );
		l_sampleSpidLocks_chk      .setName(CmActiveStatements.PROPKEY_sample_spidLocks      );
		
		l_sampleShowplan_chk       .setToolTipText("<html>Do 'sp_showplan spid' on every row in the table.<br>       This will help us to diagnose if the current SQL statement is doing something funky.</html>");
		l_sampleMonSqltext_chk     .setToolTipText("<html>Do 'select SQLText from monProcessSQLText where SPID=spid' on every row in the table.<br>    This will help us to diagnose what SQL the client sent to the server.</html>");
		l_sampleDbccSqltext_chk    .setToolTipText("<html>Do 'dbcc sqltext(spid)' on every row in the table.<br>     This will help us to diagnose what SQL the client sent to the server.<br><b>Note:</b> Role 'sybase_ts_role' is needed.</html>");
		l_sampleProcCallStack_chk  .setToolTipText("<html>Do 'select * from monProcessProcedures where SPID=spid.<br>This will help us to diagnose what stored procedure called before we ended up here.</html>");
		l_sampleDbccStacktrace_chk .setToolTipText("<html>Do 'dbcc stacktrace(spid)' on every row in the table.<br>  This will help us to diagnose what peace of code the ASE Server is currently executing.<br><b>Note:</b> Role 'sybase_ts_role' is needed.</html>");
		l_sampleCachedPlanInXml_chk.setToolTipText("<html>Do 'select show_cached_plan_in_xml(planid, 0, 0)' on every row in the table.<br>  This will help us to diagnose if the current SQL statement is doing something funky.</html>");
		l_sampleHoldingLocks_chk   .setToolTipText("<html>Include SPID's that are holding <i>any</i> locks in syslocks.<br>This will help you trace Statements that havn't released it's locks and are <b>not</b> active. (meaning that the control is at the client side)</html>");
		l_sampleSpidLocks_chk      .setToolTipText("<html>Do 'select <i>db,table,type,cnt</i> from syslocks where spid = ?' on every row in the table.<br>       This will help us to diagnose what the current SQL statement is locking.</html>");

		panel.add(l_sampleMonSqltext_chk,      "");
		panel.add(l_sampleProcCallStack_chk,   "wrap");
		panel.add(l_sampleDbccSqltext_chk,     "");
		panel.add(l_sampleSpidLocks_chk,       "wrap");
		panel.add(l_sampleShowplan_chk,        "");
		panel.add(l_sampleCachedPlanInXml_chk, "wrap");
		panel.add(l_sampleDbccStacktrace_chk,  "");
		panel.add(l_sampleHoldingLocks_chk,    "wrap");

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
		l_sampleDbccSqltext_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmActiveStatements.PROPKEY_sample_dbccSqlText, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
			}
		});
		l_sampleProcCallStack_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmActiveStatements.PROPKEY_sample_procCallStack, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
			}
		});
		l_sampleDbccStacktrace_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmActiveStatements.PROPKEY_sample_dbccStacktrace, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
			}
		});
		l_sampleCachedPlanInXml_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmActiveStatements.PROPKEY_sample_cachedPlanInXml, ((JCheckBox)e.getSource()).isSelected());
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
		l_sampleSpidLocks_chk.addActionListener(new ActionListener()
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
	@Override
	public void checkLocalComponents()
	{
		CountersModel cm = getCm();
		if (cm != null)
		{
			if (cm.isRuntimeInitialized())
			{
				// disable some options if we do not have 'sybase_ts_role'
				if ( cm.isServerRoleOrPermissionActive(AseConnectionUtils.SYBASE_TS_ROLE))
				{
					l_sampleDbccSqltext_chk   .setEnabled(true);
					l_sampleDbccStacktrace_chk.setEnabled(true);
				}
				else
				{
					l_sampleDbccSqltext_chk   .setEnabled(false);
					l_sampleDbccStacktrace_chk.setEnabled(false);

					Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
					if (conf != null)
					{
						conf.setProperty(CmActiveStatements.PROPKEY_sample_dbccSqlText,    CmActiveStatements.DEFAULT_sample_dbccSqlText);
						conf.setProperty(CmActiveStatements.PROPKEY_sample_dbccStacktrace, CmActiveStatements.DEFAULT_sample_dbccStacktrace);
					}
				}

				// disable CachedPlanInXml is not 15.7
				if ( cm.getServerVersion() > Ver.ver(15,7))
				{
					l_sampleCachedPlanInXml_chk.setEnabled(true);
				}
				else
				{
					l_sampleCachedPlanInXml_chk.setEnabled(false);

					Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
					if (conf != null)
					{
						conf.setProperty(CmActiveStatements.PROPKEY_sample_cachedPlanInXml, CmActiveStatements.DEFAULT_sample_cachedPlanInXml);
					}
				}
			} // end isRuntimeInitialized
		} // end (cm != null)
	}
}

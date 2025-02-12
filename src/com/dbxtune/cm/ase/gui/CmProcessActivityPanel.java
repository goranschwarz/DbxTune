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
import java.util.HashMap;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.dbxtune.Version;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.ase.CmProcessActivity;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.utils.ColorUtils;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;
import com.dbxtune.utils.Ver;

import net.miginfocom.swing.MigLayout;

public class CmProcessActivityPanel
extends TabularCntrPanel
{
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmProcessActivity.CM_NAME;

	public static final String  TOOLTIP_sample_systemThreads        = "<html>Sample System SPID's that executes in the ASE Server.<br><b>Note</b>: This is not a filter, you will have to wait for next sample time for this option to take effect.</html>";
	public static final String  TOOLTIP_summaryGraph_discardDbxTune = "<html>Do <b>not</b> include values where Application name starts with '"+Version.getAppName()+"' in the Summary Graphs.</html>";
	public static final String  TOOLTIP_sample_sqlText              = "<html>Get SQL Text for SPID's that are active<br>Using ASE Function query_text(spid) but only if WaitEventID != 250 <i>'waiting for input from the network'</i>.<br><b>Note</b>: This functionality is only available in ASE 16 and above.</html>";
	public static final String  TOOLTIP_sample_lastKnownSqlText     = "<html>Try to get Last Known SQL Text, <b>NOTE: Recording Must be enabled</b></html>";
	public static final String  TOOLTIP_sample_lastKnownSqlText_all = "<html>if 'get Last Known SQL Text' is enabled, we can choose to get <b>more</b> than just the last SQL Text... </html>";

	private JCheckBox l_sampleSystemThreads_chk;
	private JCheckBox l_discardAppnameDbxTune_chk;
	private JCheckBox l_sampleSqlText_chk;
	private JCheckBox l_sampleLastKnownSqlText_chk;
	private JCheckBox l_sampleLastKnownSqlTextAll_chk;
	
	private static final Color WORKER_PARENT    = new Color(229, 194, 149); // DARK Beige 
	private static final Color WORKER_PROCESSES = new Color(255, 245, 216); // Beige
	
	public CmProcessActivityPanel(CountersModel cm)
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

		// DARK BEIGE = PARENT of WORKER processes
		if (conf != null) colorStr = conf.getProperty(getName()+".color.worker.parent");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number FamilyID = (Number) adapter.getValue(adapter.getColumnIndex("FamilyID"));
				Number SPID     = (Number) adapter.getValue(adapter.getColumnIndex("SPID"));
				if (FamilyID != null && SPID != null && FamilyID.intValue() == SPID.intValue())
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, WORKER_PARENT), null));

		// BEIGE = WORKER process
		if (conf != null) colorStr = conf.getProperty(getName()+".color.worker");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String cmd = (String) adapter.getValue(adapter.getColumnIndex("Command"));
				if ("WORKER PROCESS".equals(cmd))
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, WORKER_PROCESSES), null));

		// YELLOW = SYSTEM process
		if (conf != null) colorStr = conf.getProperty(getName()+".color.system");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String login = (String) adapter.getValue(adapter.getColumnIndex("Login"));
				if (login == null || "".equals(login) || "probe".equals(login))
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.YELLOW), null));

		// EXTREME_LIGHT_GREEN = Has Statement that is currently executing
		if (conf != null) colorStr = conf.getProperty(getName()+".color.runningStatement");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number StatementExecInMs = (Number) adapter.getValue(adapter.getColumnIndex("StatementExecInMs"));
				Number WaitEventID       = (Number) adapter.getValue(adapter.getColumnIndex("WaitEventID"));
				if ( StatementExecInMs != null && WaitEventID != null && StatementExecInMs.intValue() > 0 && WaitEventID.intValue() != 250) // WaitEventID=250: waiting for input from the network
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, new Color(212, 255, 163)), null)); // Extreme light green

		// GREEN = RUNNING or RUNNABLE process
		if (conf != null) colorStr = conf.getProperty(getName()+".color.running");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String status = (String) adapter.getValue(adapter.getColumnIndex("status"));
				if ( status != null && (status.startsWith("running") || status.startsWith("runnable")) )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.GREEN), null));

		// VERY_LIGHT_GREEN = SEND SLEEP
		if (conf != null) colorStr = conf.getProperty(getName()+".color.sendSleep");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String status = (String) adapter.getValue(adapter.getColumnIndex("status"));
				if ( status != null && status.startsWith("send sleep") )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, ColorUtils.VERY_LIGHT_GREEN), null));

		// PINK = spid is BLOCKED by some other user
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocked");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number blockingSpid = (Number) adapter.getValue(adapter.getColumnIndex("BlockingSPID"));
				if ( blockingSpid != null && blockingSpid.intValue() != 0 )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.PINK), null));

		// RED = spid is BLOCKING other spids from running
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocking");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			@SuppressWarnings("unchecked")
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				boolean isBlocking                     = false;
				Number  thisSpid                       = (Number)                 adapter.getValue(adapter.getColumnIndex("SPID"));
				HashMap<Number,Object> blockingSpidMap = (HashMap<Number,Object>) adapter.getComponent().getClientProperty("blockingSpidMap");

				if (blockingSpidMap != null && thisSpid != null)
					isBlocking = blockingSpidMap.containsKey(thisSpid);

				if (isBlocking)
				{
					// Check that the SPID is not the victim of another blocked SPID
					Number blockingSpid = (Number) adapter.getValue(adapter.getColumnIndex("BlockingSPID"));
					if ( blockingSpid != null && blockingSpid.intValue() == 0 )
						return true;
				}
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

				l_sampleSystemThreads_chk      .setSelected(conf.getBooleanProperty(CmProcessActivity.PROPKEY_sample_systemThreads,        CmProcessActivity.DEFAULT_sample_systemThreads));
				l_discardAppnameDbxTune_chk    .setSelected(conf.getBooleanProperty(CmProcessActivity.PROPKEY_summaryGraph_discardDbxTune, CmProcessActivity.DEFAULT_summaryGraph_discardDbxTune));
				l_sampleSqlText_chk            .setSelected(conf.getBooleanProperty(CmProcessActivity.PROPKEY_sample_sqlText,              CmProcessActivity.DEFAULT_sample_sqlText));
				l_sampleLastKnownSqlText_chk   .setSelected(conf.getBooleanProperty(CmProcessActivity.PROPKEY_sample_lastKnownSqlText,     CmProcessActivity.DEFAULT_sample_lastKnownSqlText));
				l_sampleLastKnownSqlTextAll_chk.setSelected(conf.getBooleanProperty(CmProcessActivity.PROPKEY_sample_lastKnownSqlText_all, CmProcessActivity.DEFAULT_sample_lastKnownSqlText_all));

				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

//		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
//		JCheckBox sampleSystemThreads_chk   = new JCheckBox("Show system processes",                                                                   conf == null ? CmProcessActivity.DEFAULT_sample_systemThreads        : conf.getBooleanProperty(CmProcessActivity.PROPKEY_sample_systemThreads,        CmProcessActivity.DEFAULT_sample_systemThreads));
//		JCheckBox discardAppnameDbxTune_chk = new JCheckBox("<html>Discard '"+Version.getAppName()+"' Activity from the <b>Summary</b> Graphs</html>", conf == null ? CmProcessActivity.DEFAULT_summaryGraph_discardDbxTune : conf.getBooleanProperty(CmProcessActivity.PROPKEY_summaryGraph_discardDbxTune, CmProcessActivity.DEFAULT_summaryGraph_discardDbxTune));
//		JCheckBox sampleSqlText_chk         = new JCheckBox("<html>Get SQL Text for active SPID's</html>",                                             conf == null ? CmProcessActivity.DEFAULT_sample_sqlText              : conf.getBooleanProperty(CmProcessActivity.PROPKEY_sample_sqlText,              CmProcessActivity.DEFAULT_sample_sqlText));
		l_sampleSystemThreads_chk       = new JCheckBox("Show system processes",                                                                   conf == null ? CmProcessActivity.DEFAULT_sample_systemThreads        : conf.getBooleanProperty(CmProcessActivity.PROPKEY_sample_systemThreads,        CmProcessActivity.DEFAULT_sample_systemThreads));
		l_discardAppnameDbxTune_chk     = new JCheckBox("<html>Discard '"+Version.getAppName()+"' Activity from the <b>Summary</b> Graphs</html>", conf == null ? CmProcessActivity.DEFAULT_summaryGraph_discardDbxTune : conf.getBooleanProperty(CmProcessActivity.PROPKEY_summaryGraph_discardDbxTune, CmProcessActivity.DEFAULT_summaryGraph_discardDbxTune));
		l_sampleSqlText_chk             = new JCheckBox("<html>Get SQL Text for active SPID's</html>",                                             conf == null ? CmProcessActivity.DEFAULT_sample_sqlText              : conf.getBooleanProperty(CmProcessActivity.PROPKEY_sample_sqlText,              CmProcessActivity.DEFAULT_sample_sqlText));
		l_sampleLastKnownSqlText_chk    = new JCheckBox("<html>Get Last Active SQL Text for SPID's</html>",                                        conf == null ? CmProcessActivity.DEFAULT_sample_lastKnownSqlText     : conf.getBooleanProperty(CmProcessActivity.PROPKEY_sample_lastKnownSqlText,     CmProcessActivity.DEFAULT_sample_lastKnownSqlText));
		l_sampleLastKnownSqlTextAll_chk = new JCheckBox("<html>All Available</html>",                                                              conf == null ? CmProcessActivity.DEFAULT_sample_lastKnownSqlText     : conf.getBooleanProperty(CmProcessActivity.PROPKEY_sample_lastKnownSqlText_all, CmProcessActivity.DEFAULT_sample_lastKnownSqlText_all));

		l_sampleSystemThreads_chk      .setName(CmProcessActivity.PROPKEY_sample_systemThreads);
		l_sampleSystemThreads_chk      .setToolTipText(TOOLTIP_sample_systemThreads);
                                       
		l_discardAppnameDbxTune_chk    .setName(CmProcessActivity.PROPKEY_summaryGraph_discardDbxTune);
		l_discardAppnameDbxTune_chk    .setToolTipText(TOOLTIP_summaryGraph_discardDbxTune);
                                       
		l_sampleSqlText_chk            .setName(CmProcessActivity.PROPKEY_sample_sqlText);
		l_sampleSqlText_chk            .setToolTipText(TOOLTIP_sample_sqlText);
                                       
		l_sampleLastKnownSqlText_chk   .setName(CmProcessActivity.PROPKEY_sample_lastKnownSqlText);
		l_sampleLastKnownSqlText_chk   .setToolTipText(TOOLTIP_sample_lastKnownSqlText);

		l_sampleLastKnownSqlTextAll_chk.setName(CmProcessActivity.PROPKEY_sample_lastKnownSqlText_all);
		l_sampleLastKnownSqlTextAll_chk.setToolTipText(TOOLTIP_sample_lastKnownSqlText_all);

		panel.add(l_sampleSystemThreads_chk,       "wrap");
		panel.add(l_discardAppnameDbxTune_chk,     "wrap");
		panel.add(l_sampleSqlText_chk,             "wrap");
		panel.add(l_sampleLastKnownSqlText_chk,    "split"); // on same row as next 
		panel.add(l_sampleLastKnownSqlTextAll_chk, "wrap");

		l_sampleSystemThreads_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmProcessActivity.PROPKEY_sample_systemThreads, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		
		l_discardAppnameDbxTune_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmProcessActivity.PROPKEY_summaryGraph_discardDbxTune, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				//getCm().setSql(null);
			}
		});
		
		l_sampleSqlText_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmProcessActivity.PROPKEY_sample_sqlText, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		
		l_sampleLastKnownSqlText_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmProcessActivity.PROPKEY_sample_lastKnownSqlText, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				//getCm().setSql(null);
			}
		});
		
		l_sampleLastKnownSqlTextAll_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmProcessActivity.PROPKEY_sample_lastKnownSqlText_all, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				//getCm().setSql(null);
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
				// disable CachedPlanInXml is not 16.0
				if ( cm.getServerVersion() > Ver.ver(16,0))
				{
					l_sampleSqlText_chk.setEnabled(true);
				}
				else
				{
					l_sampleSqlText_chk.setEnabled(false);

					Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
					if (conf != null)
					{
						conf.setProperty(CmProcessActivity.PROPKEY_sample_sqlText, false);
					}
				}
			} // end isRuntimeInitialized
		} // end (cm != null)
	}
}

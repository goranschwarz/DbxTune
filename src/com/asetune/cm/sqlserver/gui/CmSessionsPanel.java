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

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.cm.CountersModel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

public class CmSessionsPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmSessionsPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmSessionsPanel.CM_NAME;

//	public static final String  TOOLTIP_sample_systemThreads = "<html>Sample System SPID's that executes in the ASE Server.<br><b>Note</b>: This is not a filter, you will have to wait for next sample time for this option to take effect.</html>";

	public CmSessionsPanel(CountersModel cm)
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

//		// YELLOW = SYSTEM process
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.system");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				String sid = (String) adapter.getValue(adapter.getColumnIndex("sid"));
//				if ("0x0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000".equals(sid))
//					return true;
//				return false;
//			}
//		}, SwingUtils.parseColor(colorStr, Color.YELLOW), null));

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

		// LIGHT_GREEN = suspended process
		if (conf != null) colorStr = conf.getProperty(getName()+".color.suspended");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String status = (String) adapter.getValue(adapter.getColumnIndex("exec_status"));
				if ( status != null && status.startsWith("suspended"))
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, new Color(212, 255, 163)), null));

		// ORANGE = spid has OpenTrans
		if (conf != null) colorStr = conf.getProperty(getName()+".color.opentran");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number blockingSpid = (Number) adapter.getValue(adapter.getColumnIndex("open_transaction_count"));
				if ( blockingSpid != null && blockingSpid.intValue() != 0 )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));

		// PINK = spid is BLOCKED by some other user
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocked");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number blockingSpid = (Number) adapter.getValue(adapter.getColumnIndex("blocking_session_id"));
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
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String listOfBlockedSpids  = adapter.getString(adapter.getColumnIndex("BlockingOtherSpids"));
				String blocking_session_id = adapter.getString(adapter.getColumnIndex("blocking_session_id"));

				if (listOfBlockedSpids != null)
					listOfBlockedSpids = listOfBlockedSpids.trim();
				if ( ! "".equals(listOfBlockedSpids) && "0".equals(blocking_session_id))
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.RED), null));
	}


//	@Override
//	protected JPanel createLocalOptionsPanel()
//	{
//		JPanel panel = SwingUtils.createPanel("Local Options", true);
//		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));
//
//		Configuration conf = Configuration.getCombinedConfiguration();
//		JCheckBox sampleSystemThreads_chk = new JCheckBox("Show system processes", conf == null ? CmWho.DEFAULT_sample_systemThreads : conf.getBooleanProperty(CmWho.PROPKEY_sample_systemThreads, CmWho.DEFAULT_sample_systemThreads));
//
//		sampleSystemThreads_chk.setName(CmWho.PROPKEY_sample_systemThreads);
//		sampleSystemThreads_chk.setToolTipText(TOOLTIP_sample_systemThreads);
//		panel.add(sampleSystemThreads_chk, "wrap");
//
//		sampleSystemThreads_chk.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				// Need TMP since we are going to save the configuration somewhere
//				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
//				if (conf == null) return;
//				conf.setProperty(CmWho.PROPKEY_sample_systemThreads, ((JCheckBox)e.getSource()).isSelected());
//				conf.save();
//				
//				// ReInitialize the SQL
//				getCm().setSql(null);
//			}
//		});
//		
//		return panel;
//	}
}

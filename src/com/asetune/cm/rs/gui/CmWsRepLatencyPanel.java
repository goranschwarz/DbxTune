/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.cm.rs.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.cm.CountersModel;
import com.asetune.cm.rs.CmWsRepLatency;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmWsRepLatencyPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmWsRepLatencyPanel.class);
	private static final long    serialVersionUID      = 1L;

	private JCheckBox  l_updateActive_chk;

	private JLabel     l_updateActiveInterval_lbl;
	private JTextField l_updateActiveInterval_txt;

	public CmWsRepLatencyPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// Mark the row as PINK if this SPID is BLOCKED by another thread
		if (conf != null) colorStr = conf.getProperty(getName()+".color.status.notOk");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String activeState  = adapter.getString(adapter.getColumnIndex("ActiveState"));
				String standbyState = adapter.getString(adapter.getColumnIndex("StandbyState"));
				if (activeState  != null) activeState  = activeState .trim();
				if (standbyState != null) standbyState = standbyState.trim();

				if ( ! "Active/".equalsIgnoreCase(activeState) )
					return true;
				if ( ! "Active/".equalsIgnoreCase(standbyState) )
					return true;
				
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.PINK), null));
	}

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean defaultOpt;
//		int     defaultIntOpt;

		//-----------------------------------------
		// sample system tables:
		//-----------------------------------------
		defaultOpt = conf == null ? CmWsRepLatency.DEFAULT_update_active : conf.getBooleanProperty(CmWsRepLatency.PROPKEY_update_active, CmWsRepLatency.DEFAULT_update_active);
		l_updateActive_chk = new JCheckBox("Update Active DB", defaultOpt);

		l_updateActiveInterval_lbl = new JLabel("Update Active Interval in Seconds");
		l_updateActiveInterval_txt = new JTextField(conf.getLongProperty(CmWsRepLatency.PROPKEY_update_activeIntervalInSec, CmWsRepLatency.DEFAULT_update_activeIntervalInSec)+"", 5);

		l_updateActive_chk.setName(CmWsRepLatency.PROPKEY_update_active);

		l_updateActive_chk        .setToolTipText("<html>Connect to Active side of the WarmStandby side and Update a dummy table, this to induce network traffic in a <i>calm</i> system.<br></html>");
		l_updateActiveInterval_lbl.setToolTipText("<html>Do not update active side every time, but wait for x second between updates.</html>");
		l_updateActiveInterval_txt.setToolTipText(l_updateActiveInterval_lbl.getToolTipText());

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
		
		// LAYOUT
		panel.add(l_updateActive_chk,         "span, wrap");
		panel.add(l_updateActiveInterval_lbl, "");
		panel.add(l_updateActiveInterval_txt, "wrap");

		return panel;
	}
	private void applyLocalSettings()
	{
		// Need TMP since we are going to save the configuration somewhere
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null) 
			return;

		conf.setProperty(CmWsRepLatency.PROPKEY_update_active, l_updateActive_chk.isSelected());

		conf.setProperty(CmWsRepLatency.PROPKEY_update_activeIntervalInSec, StringUtil.parseLong(l_updateActiveInterval_txt.getText(), CmWsRepLatency.DEFAULT_update_activeIntervalInSec));

		conf.save();
		
		// This will force the CM to re-initialize the SQL statement.
//		CountersModel cm = getCm().getCounterController().getCmByName(getName());
//		if (cm != null)
//			cm.setSql(null);
	}

	@Override
	public void checkLocalComponents()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean confProp = conf.getBooleanProperty(CmWsRepLatency.PROPKEY_update_active, CmWsRepLatency.DEFAULT_update_active);
		boolean guiProp  = l_updateActive_chk.isSelected();

		if (confProp != guiProp)
			l_updateActive_chk.setSelected(confProp);
	}
}

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
package com.asetune.cm.ase.gui;

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
import com.asetune.cm.ase.CmCachePools;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmCachePoolsPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmLocksPanel.class);
	private static final long    serialVersionUID      = 1L;

	public static final String  TOOLTIP_slideWindowTime = 
			"<html>"
			+ "Set number of seconds the <i>slide window time</i> will keep <code>PagesRead</code> for.<br>"
			+ "This will affect the columns 'CacheReplacementSlidePct', 'CacheSlideTime', 'PagesReadInSlide', 'CacheEfficiencySlide'.<br>"
			+ "<b>tip:</b> '10m' is 10 minutes, '2h' is 2 hours<br>"
			+ "</html>";
		
	public static final String  TOOLTIP_onZeroLogicalReads = 
			"<html>"
			+ "When LogicalReads is Zero, set CacheHitRate to 100% instead of 0%.<br>"
			+ "</html>";
		
	public CmCachePoolsPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// Mark the row as RED if Stalls is above 0
		if (conf != null) colorStr = conf.getProperty(getName()+".color.stalls");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number Stalls = (Number) adapter.getValue(adapter.getColumnIndex("Stalls"));
				if ( Stalls != null && Stalls.intValue() > 0)
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.RED), null));
	}

	private JLabel     _slideWindowTime_lbl;
	private JTextField _slideWindowTime_txt;

	private JCheckBox  _onZeroLogicalReads_chk;

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		LocalOptionsConfigPanel panel = new LocalOptionsConfigPanel("Local Options", new LocalOptionsConfigChanges()
		{
			@Override
			public void configWasChanged(String propName, String propVal)
			{
				Configuration conf = Configuration.getCombinedConfiguration();

				_onZeroLogicalReads_chk.setSelected(conf.getBooleanProperty(CmCachePools.PROPKEY_CacheHitRateTo100PctOnZeroReads, CmCachePools.DEFAULT_CacheHitRateTo100PctOnZeroReads));
				_slideWindowTime_txt   .setText(""+ conf.getIntProperty    (CmCachePools.PROPKEY_CacheSlideTimeInSec            , CmCachePools.DEFAULT_CacheSlideTimeInSec));

				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

//		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		int     defaultIntOpt;
		boolean defaultBoolOpt;

		//-----------------------------------------
		// Slide Time
		//-----------------------------------------
		defaultIntOpt = conf == null ? CmCachePools.DEFAULT_CacheSlideTimeInSec : conf.getIntProperty(CmCachePools.PROPKEY_CacheSlideTimeInSec, CmCachePools.DEFAULT_CacheSlideTimeInSec);
		_slideWindowTime_lbl = new JLabel("Slide Window Time");
		_slideWindowTime_txt = new JTextField(Integer.toString(defaultIntOpt), 6); // 6 is length of text field

		_slideWindowTime_txt.setName(CmCachePools.PROPKEY_CacheSlideTimeInSec);
		_slideWindowTime_txt.setToolTipText(TOOLTIP_slideWindowTime);
		_slideWindowTime_lbl.setToolTipText(TOOLTIP_slideWindowTime);

		defaultBoolOpt = conf == null ? CmCachePools.DEFAULT_CacheHitRateTo100PctOnZeroReads : conf.getBooleanProperty(CmCachePools.PROPKEY_CacheHitRateTo100PctOnZeroReads, CmCachePools.DEFAULT_CacheHitRateTo100PctOnZeroReads);
		_onZeroLogicalReads_chk = new JCheckBox("set CacheHitRate to 100% when LogicalReads is 0", defaultBoolOpt);

		_onZeroLogicalReads_chk.setName(CmCachePools.PROPKEY_CacheHitRateTo100PctOnZeroReads);
		_onZeroLogicalReads_chk.setToolTipText(TOOLTIP_onZeroLogicalReads);

		final ActionListener slideWindowTime_action = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				
				// Convert 10m to 600, 1h to 3600 etc..
				int intVal = parseHourMinuteTime(_slideWindowTime_txt.getText(), -1, true);
				if (intVal < 0)
					intVal = CmCachePools.DEFAULT_CacheSlideTimeInSec;
				if ( intVal >= 0 )
				{
					conf.setProperty(CmCachePools.PROPKEY_CacheSlideTimeInSec, intVal);
					conf.save();

					_slideWindowTime_txt.setText(Integer.toString(intVal));
				}
			}
		};

		_slideWindowTime_txt.addActionListener(slideWindowTime_action);
		_slideWindowTime_txt.addFocusListener(new FocusListener()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				// Just call the "action" on _slideWindowTime_txt, so we don't have to duplicate code.
				slideWindowTime_action.actionPerformed(null);
			}
			
			@Override public void focusGained(FocusEvent e) {}
		});
		
		
		_onZeroLogicalReads_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;

				conf.setProperty(CmCachePools.PROPKEY_CacheHitRateTo100PctOnZeroReads, _onZeroLogicalReads_chk.isSelected());
				conf.save();
			}
		});

		
		//-----------------------------------------
		// LAYOUT
		//-----------------------------------------
		panel.add(_slideWindowTime_lbl, "split");
		panel.add(_slideWindowTime_txt, "wrap");

		panel.add(_onZeroLogicalReads_chk, "wrap");

		return panel;
	}

}

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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.cm.postgres.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.asetune.cm.CountersModel;
import com.asetune.cm.postgres.CmPgDatabase;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmPgDatabasePanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmPgDatabasePanel.class);
	private static final long    serialVersionUID      = 1L;

	public static final String  TOOLTIP_slideWindowTime = 
			"<html>"
			+ "Set number of seconds the <i>slide window time</i> will keep <code>tup_fetched</code> and <code>tup_returned</code> for.<br>"
			+ "This will affect the columns 'fetch_efficency_slide_pct', 'tup_fetched_in_slide', 'tup_returned_in_slide', 'slide_time'.<br>"
			+ "<b>tip:</b> '10m' is 10 minutes, '2h' is 2 hours<br>"
			+ "</html>";
		
	public CmPgDatabasePanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
	}

	private JLabel     _slideWindowTime_lbl;
	private JTextField _slideWindowTime_txt;

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		int     defaultIntOpt;
		boolean defaultBoolOpt;

		//-----------------------------------------
		// Slide Time
		//-----------------------------------------
		defaultIntOpt = conf == null ? CmPgDatabase.DEFAULT_SlideTimeInSec : conf.getIntProperty(CmPgDatabase.PROPKEY_SlideTimeInSec, CmPgDatabase.DEFAULT_SlideTimeInSec);
		_slideWindowTime_lbl = new JLabel("Slide Window Time");
		_slideWindowTime_txt = new JTextField(Integer.toString(defaultIntOpt), 6); // 6 is length of text field

		_slideWindowTime_txt.setName(CmPgDatabase.PROPKEY_SlideTimeInSec);
		_slideWindowTime_txt.setToolTipText(TOOLTIP_slideWindowTime);
		_slideWindowTime_lbl.setToolTipText(TOOLTIP_slideWindowTime);

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
					intVal = CmPgDatabase.DEFAULT_SlideTimeInSec;
				if ( intVal >= 0 )
				{
					conf.setProperty(CmPgDatabase.PROPKEY_SlideTimeInSec, intVal);
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
		
		
		//-----------------------------------------
		// LAYOUT
		//-----------------------------------------
		panel.add(_slideWindowTime_lbl, "split");
		panel.add(_slideWindowTime_txt, "wrap");

		return panel;
	}

}

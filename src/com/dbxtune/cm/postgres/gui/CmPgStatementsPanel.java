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
package com.dbxtune.cm.postgres.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.postgres.CmPgStatements;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

import net.miginfocom.swing.MigLayout;

public class CmPgStatementsPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmPgStatementsPanel.class);
	private static final long    serialVersionUID      = 1L;

	public static final String  TOOLTIP_totalTimeGt = 
			"<html>"
			+ "Set number of milli seconds the statement has to acumulate in <code>pg_stat_statements</code> before we consider it as <i>collectable</i>"
			+ "</html>";
		
	public CmPgStatementsPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
	}

	private JLabel     _totalTimeGt_lbl;
	private JTextField _totalTimeGt_txt;

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		LocalOptionsConfigPanel panel = new LocalOptionsConfigPanel("Local Options", new LocalOptionsConfigChanges()
		{
			@Override
			public void configWasChanged(String propName, String propVal)
			{
				Configuration conf = Configuration.getCombinedConfiguration();

//				list.add(new CmSettingsHelper("Sample 'total_time' above", PROPKEY_sample_total_time_gt  , Integer.class, conf.getIntProperty(PROPKEY_sample_total_time_gt  , DEFAULT_sample_total_time_gt  ), DEFAULT_sample_total_time_gt  , "Sample 'total_time' above" ));

				_totalTimeGt_txt.setText(""+ conf.getIntProperty(CmPgStatements.PROPKEY_sample_total_time_gt, CmPgStatements.DEFAULT_sample_total_time_gt));

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
		defaultIntOpt = conf == null ? CmPgStatements.DEFAULT_sample_total_time_gt : conf.getIntProperty(CmPgStatements.PROPKEY_sample_total_time_gt, CmPgStatements.DEFAULT_sample_total_time_gt);
		_totalTimeGt_lbl = new JLabel("where 'total_time' above");
		_totalTimeGt_txt = new JTextField(Integer.toString(defaultIntOpt), 6); // 6 is length of text field

		_totalTimeGt_txt.setName(CmPgStatements.PROPKEY_sample_total_time_gt);
		_totalTimeGt_txt.setToolTipText(TOOLTIP_totalTimeGt);
		_totalTimeGt_lbl.setToolTipText(TOOLTIP_totalTimeGt);

		final ActionListener slideWindowTime_action = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				
				// Convert 10m to 600, 1h to 3600 etc..
				int intVal = StringUtil.parseInt(_totalTimeGt_txt.getText(), -1);
				if (intVal < 0)
					intVal = CmPgStatements.DEFAULT_sample_total_time_gt;
				if ( intVal >= 0 )
				{
					conf.setProperty(CmPgStatements.PROPKEY_sample_total_time_gt, intVal);
					conf.save();

					_totalTimeGt_txt.setText(Integer.toString(intVal));
					
					getCm().setSql(null);
				}
			}
		};

		_totalTimeGt_txt.addActionListener(slideWindowTime_action);
		_totalTimeGt_txt.addFocusListener(new FocusListener()
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
		panel.add(_totalTimeGt_lbl, "split");
		panel.add(_totalTimeGt_txt, "wrap");

		return panel;
	}

}

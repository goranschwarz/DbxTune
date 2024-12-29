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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.sqlserver.CmWaitStatsPerDb;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.dbxtune.utils.Configuration;

import net.miginfocom.swing.MigLayout;

public class CmWaitStatsPerDbPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmPlanCacheDetailsPanel.class);
	private static final long    serialVersionUID      = 1L;

	public static final String  TOOLTIP_sample_extraWhereClause = 
		"<html>" +
		"Add extra where clause to the query that fetches information.<br>" +
		"To check SQL statement that are used: Right click on the 'tab', and choose 'Properties'<br>" +
		"<br>" +
		"</html>";

	public CmWaitStatsPerDbPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
	}

	private RSyntaxTextAreaX _sampleExtraWhereClause_txt;
	private JButton          _sampleExtraWhereClause_but;
	private JButton          _sampleExtraWhereToDef_but;

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		LocalOptionsConfigPanel panel = new LocalOptionsConfigPanel("Local Options", new LocalOptionsConfigChanges()
		{
			@Override
			public void configWasChanged(String propName, String propVal)
			{
				Configuration conf = Configuration.getCombinedConfiguration();

//				list.add(new CmSettingsHelper("Extra Where Clause",     PROPKEY_sample_extraWhereClause      , String.class,  conf.getProperty       (PROPKEY_sample_extraWhereClause      , DEFAULT_sample_extraWhereClause      ), DEFAULT_sample_extraWhereClause     , CmSpidWaitPanel.TOOLTIP_sample_extraWhereClause                     ));

				_sampleExtraWhereClause_txt .setText(conf.getProperty(CmWaitStatsPerDb.PROPKEY_sample_extraWhereClause, CmWaitStatsPerDb.DEFAULT_sample_extraWhereClause));

				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

//		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));
//		panel.setToolTipText(
//			"<html>" +
//				"All the options in this panel executes additional SQL lookups in the database <b>after</b> the result set has been delivered.<br>" +
//				"This means that we are doing 1 extra SQL lookup for every checkbox option per row on the result set table.<br>" +
//				"<br>" +
//				"NOTE: So if you check all the options, the time to do refresh on this tab will <b>increase</b>." +
//			"</html>");

		Configuration conf = Configuration.getCombinedConfiguration();

		_sampleExtraWhereClause_txt = new RSyntaxTextAreaX();
		_sampleExtraWhereClause_but = new JButton("Apply Extra Where Clause");
		_sampleExtraWhereToDef_but  = new JButton("To Default");

		_sampleExtraWhereClause_but.setToolTipText(TOOLTIP_sample_extraWhereClause);
		_sampleExtraWhereClause_txt.setToolTipText(TOOLTIP_sample_extraWhereClause);


		// Set initial values for some fields
		String sampleExtraWhereClause = (conf == null ? CmWaitStatsPerDb.DEFAULT_sample_extraWhereClause : conf.getProperty(CmWaitStatsPerDb.PROPKEY_sample_extraWhereClause, CmWaitStatsPerDb.DEFAULT_sample_extraWhereClause));

		_sampleExtraWhereClause_txt.setText(sampleExtraWhereClause);
		_sampleExtraWhereClause_txt.setHighlightCurrentLine(false);
		_sampleExtraWhereClause_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);

		panel.add(_sampleExtraWhereClause_txt, "grow, push, wrap");
		panel.add(_sampleExtraWhereClause_but, "split");
		panel.add(_sampleExtraWhereToDef_but,  "wrap");

		
		_sampleExtraWhereClause_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) 
					return;

				String extraWhere = _sampleExtraWhereClause_txt.getText().trim();
				if (extraWhere.startsWith("--"))
					extraWhere = "";
				
				conf.setProperty(CmWaitStatsPerDb.PROPKEY_sample_extraWhereClause, extraWhere);
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

		_sampleExtraWhereToDef_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_sampleExtraWhereClause_txt.setText(CmWaitStatsPerDb.DEFAULT_sample_extraWhereClause);
				_sampleExtraWhereClause_but.doClick(); // click APPLY button
			}
		});

		return panel;
	}
}

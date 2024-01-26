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
package com.asetune.cm.postgres.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.asetune.cm.CountersModel;
import com.asetune.cm.postgres.CmPgBufferCacheDet;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmPgBufferCacheDetPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmPgBufferCacheDetPanel.class);
	private static final long    serialVersionUID      = 1L;

	public CmPgBufferCacheDetPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
//		Configuration conf = Configuration.getCombinedConfiguration();
//		String colorStr = null;
//
//		// ORANGE = Index id > 0
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.index");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				Number indexId = (Number) adapter.getValue(adapter.getColumnIndex("IndexID"));
//				if ( indexId != null && indexId.intValue() > 0)
//					return true;
//				return false;
//			}
//		}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));
//
//		// BLOB (text/image columns)
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.blob");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				Number indexId = (Number) adapter.getValue(adapter.getColumnIndex("IndexID"));
//				if ( indexId != null && indexId.intValue() == 255)
//					return true;
//				return false;
//			}
//		}, SwingUtils.parseColor(colorStr, ColorConstants.COLOR_DATATYPE_BLOB), null));
	}

	private JCheckBox  l_sampleTopRows_chk;
	private JTextField l_sampleTopRowsCount_txt;

	private JButton l_filterOutSystemTables_but;
	
	@Override
	protected JPanel createLocalOptionsPanel()
	{
		LocalOptionsConfigPanel panel = new LocalOptionsConfigPanel("Local Options", new LocalOptionsConfigChanges()
		{
			@Override
			public void configWasChanged(String propName, String propVal)
			{
				Configuration conf = Configuration.getCombinedConfiguration();

//				list.add(new CmSettingsHelper("Limit num of rows",     PROPKEY_sample_topRows      , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_topRows      , DEFAULT_sample_topRows      ), DEFAULT_sample_topRows     , "Get only first # rows (select top # ...) true or false"   ));
//				list.add(new CmSettingsHelper("Limit num of rowcount", PROPKEY_sample_topRowsCount , Integer.class, conf.getIntProperty    (PROPKEY_sample_topRowsCount , DEFAULT_sample_topRowsCount ), DEFAULT_sample_topRowsCount, "Get only first # rows (select top # ...), number of rows" ));

				l_sampleTopRows_chk      .setSelected(conf.getBooleanProperty(CmPgBufferCacheDet.PROPKEY_sample_topRows,      CmPgBufferCacheDet.DEFAULT_sample_topRows));
				l_sampleTopRowsCount_txt .setText(""+ conf.getIntProperty    (CmPgBufferCacheDet.PROPKEY_sample_topRowsCount, CmPgBufferCacheDet.DEFAULT_sample_topRowsCount));

				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

//		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean defaultOpt;
		int     defaultIntOpt;

		// Top Rows (top #)
		defaultOpt    = conf == null ? CmPgBufferCacheDet.DEFAULT_sample_topRows      : conf.getBooleanProperty(CmPgBufferCacheDet.PROPKEY_sample_topRows,      CmPgBufferCacheDet.DEFAULT_sample_topRows);
		defaultIntOpt = conf == null ? CmPgBufferCacheDet.DEFAULT_sample_topRowsCount : conf.getIntProperty    (CmPgBufferCacheDet.PROPKEY_sample_topRowsCount, CmPgBufferCacheDet.DEFAULT_sample_topRowsCount);
		l_sampleTopRows_chk      = new JCheckBox("Limit number of rows (top #)", defaultOpt);
		l_sampleTopRowsCount_txt = new JTextField(Integer.toString(defaultIntOpt), 5);

		l_filterOutSystemTables_but = new JButton("Set 'filter', remove system tables");
		
		l_sampleTopRows_chk.setName(CmPgBufferCacheDet.PROPKEY_sample_topRows);
		l_sampleTopRows_chk.setToolTipText("<html>Restrict number of rows fetch from the server<br>Uses: <code>select <b>top "+CmPgBufferCacheDet.DEFAULT_sample_topRowsCount+"</b> c1, c2, c3 from tablename where...</code></html>");

		l_sampleTopRowsCount_txt.setName(CmPgBufferCacheDet.PROPKEY_sample_topRowsCount);
		l_sampleTopRowsCount_txt.setToolTipText("<html>Restrict number of rows fetch from the server<br>Uses: <code>select <b>top "+CmPgBufferCacheDet.DEFAULT_sample_topRowsCount+"</b> c1, c2, c3 from tablename where...</code></html>");

		l_filterOutSystemTables_but.setToolTipText("<html>The will just set the free text filter to discard schema names 'pg_*' and 'information_schema' </html>");
		
		l_sampleTopRows_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmPgBufferCacheDet.PROPKEY_sample_topRows, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);
			}
		});
		
		final ActionListener sampleTopRowsCount_action = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				
				String strVal = l_sampleTopRowsCount_txt.getText();
				int    intVal = CmPgBufferCacheDet.DEFAULT_sample_topRowsCount;
				try { intVal = Integer.parseInt(strVal);}
				catch (NumberFormatException nfe)
				{
					intVal = CmPgBufferCacheDet.DEFAULT_sample_topRowsCount;
					SwingUtils.showWarnMessage(CmPgBufferCacheDetPanel.this, "Not a Number", "<html>This must be a number, you entered '"+strVal+"'.<br>Setting to default value '"+intVal+"'.</html>", nfe);
					l_sampleTopRowsCount_txt.setText(intVal+"");
				}
				conf.setProperty(CmPgBufferCacheDet.PROPKEY_sample_topRowsCount, intVal);
				conf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);
			}
		};
		l_sampleTopRowsCount_txt.addActionListener(sampleTopRowsCount_action);
		l_sampleTopRowsCount_txt.addFocusListener(new FocusListener()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				// Just call the "action" on sampleTopRowsCount_txt, so we don't have to duplicate code.
				sampleTopRowsCount_action.actionPerformed(null);
			}
			
			@Override public void focusGained(FocusEvent e) {}
		});
		
		
		// SET FILTER TEXT
		l_filterOutSystemTables_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String filter = "WHERE schema_name not like 'pg_%' and schema_name != 'information_schema'";

				// Set and Apply the filter
				getFilterFreeText().setFilterChkboxSelected(true);
				getFilterFreeText().setFilterText(filter);
			}
		});
		
		// LAYOUT
		panel.add(l_sampleTopRows_chk,         "split");
		panel.add(l_sampleTopRowsCount_txt,    "wrap");

		panel.add(l_filterOutSystemTables_but, "wrap");
		
		return panel;
	}
}

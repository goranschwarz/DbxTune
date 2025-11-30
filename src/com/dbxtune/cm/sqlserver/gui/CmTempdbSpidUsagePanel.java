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

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.sqlserver.CmTempdbSpidUsage;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmTempdbSpidUsagePanel
extends TabularCntrPanel
{
	private static final long    serialVersionUID      = 1L;

	public static final String  TOOLTIP_sample_systemThreads    = "<html>Sample System SPID's that executes in the SQL Server.<br><b>Note</b>: This is not a filter, you will have to wait for next sample time for this option to take effect.</html>";
	public static final String  TOOLTIP_sample_sqlText          = "<html>Sample last executed SQL Text from the client.<br><b>Note</b>: This may NOT be the SQL Statement that is responsible for the tempdb usage, it's just the lastest SQL text executed by the session_id.</html>";
	public static final String  TOOLTIP_sample_TotalUsageMb_min = "<html>Sample only records with 'TotalUsageMb' above this amount of MB</html>";
//	public static final String  TOOLTIP_sample_TotalUsageMb_includeInternalObjects = "<html>The <i>Sess/TaskInternalObjectMb</i> columns seems to be a bit strange (not trustworthy). So this option is if the columns 'SessInternalObjectMb' and 'TaskInternalObjectMb' should be <b>included</b> in the column 'TotalUsageMb'.</html>";

	public CmTempdbSpidUsagePanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// YELLOW = SYSTEM process
		if (conf != null) colorStr = conf.getProperty(getName()+".color.system");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Boolean is_user_process = (Boolean) adapter.getValue(adapter.getColumnIndex("is_user_process"));
				if ( is_user_process != null && !is_user_process )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.YELLOW), null));

		// GREEN = RUNNING or RUNNABLE process
		if (conf != null) colorStr = conf.getProperty(getName()+".color.running");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
//				String status = (String) adapter.getValue(adapter.getColumnIndex("status"));
				String status = (String) adapter.getValue(adapter.getColumnIndex("session_status"));
				if ( status != null && (status.startsWith("running") || status.startsWith("runnable")) )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.GREEN), null));

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
	}

	private JCheckBox  l_sampleSystemThreads_chk;
	private JCheckBox  l_sampleSqlText_chk;
	private JLabel     l_sampleTotalUsageMbMin_lbl;
	private JTextField l_sampleTotalUsageMbMin_txt;
//	private JCheckBox  l_sampleIntObj_chk;

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		LocalOptionsConfigPanel panel = new LocalOptionsConfigPanel("Local Options", new LocalOptionsConfigChanges()
		{
			@Override
			public void configWasChanged(String propName, String propVal)
			{
				Configuration conf = Configuration.getCombinedConfiguration();

//				list.add(new CmSettingsHelper("Sample System Threads"                     , PROPKEY_sample_systemThreads                      , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemThreads                      , DEFAULT_sample_systemThreads                      ), DEFAULT_sample_systemThreads                      , CmTempdbSpidUsagePanel.TOOLTIP_sample_systemThreads ));
//				list.add(new CmSettingsHelper("Sample SQL Text"                           , PROPKEY_sample_sqlText                            , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_sqlText                            , DEFAULT_sample_sqlText                            ), DEFAULT_sample_sqlText                            , CmTempdbSpidUsagePanel.TOOLTIP_sample_sqlText ));
//				list.add(new CmSettingsHelper("Total Usage Mb Min Value"                  , PROPKEY_sample_TotalUsageMb_min                   , Double .class, conf.getDoubleProperty (PROPKEY_sample_TotalUsageMb_min                   , DEFAULT_sample_TotalUsageMb_min                   ), DEFAULT_sample_TotalUsageMb_min                   , CmTempdbSpidUsagePanel.TOOLTIP_sample_TotalUsageMb_min ));

				l_sampleSystemThreads_chk  .setSelected(conf.getBooleanProperty(CmTempdbSpidUsage.PROPKEY_sample_systemThreads   , CmTempdbSpidUsage.DEFAULT_sample_systemThreads));
				l_sampleSqlText_chk        .setSelected(conf.getBooleanProperty(CmTempdbSpidUsage.PROPKEY_sample_sqlText         , CmTempdbSpidUsage.DEFAULT_sample_sqlText));      
				l_sampleTotalUsageMbMin_txt.setText(""+ conf.getDoubleProperty (CmTempdbSpidUsage.PROPKEY_sample_TotalUsageMb_min, CmTempdbSpidUsage.DEFAULT_sample_TotalUsageMb_min));

				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

//		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		l_sampleSystemThreads_chk   = new JCheckBox ("Show system processes",                                 conf == null ? CmTempdbSpidUsage.DEFAULT_sample_systemThreads                       : conf.getBooleanProperty(CmTempdbSpidUsage.PROPKEY_sample_systemThreads                      , CmTempdbSpidUsage.DEFAULT_sample_systemThreads));
		l_sampleSqlText_chk         = new JCheckBox ("Sample Last Executed SQL Text",                         conf == null ? CmTempdbSpidUsage.DEFAULT_sample_sqlText                             : conf.getBooleanProperty(CmTempdbSpidUsage.PROPKEY_sample_sqlText                            , CmTempdbSpidUsage.DEFAULT_sample_sqlText));
		l_sampleTotalUsageMbMin_lbl = new JLabel    ("Total Usage Mb Min Value");
		l_sampleTotalUsageMbMin_txt = new JTextField(""+(                                                     conf == null ? CmTempdbSpidUsage.DEFAULT_sample_TotalUsageMb_min                    : conf.getDoubleProperty (CmTempdbSpidUsage.PROPKEY_sample_TotalUsageMb_min                   , CmTempdbSpidUsage.DEFAULT_sample_TotalUsageMb_min)), 5);
//		l_sampleIntObj_chk          = new JCheckBox ("Include 'Sess/TaskInternalObjectMb' in 'TotalUsageMb'", conf == null ? CmTempdbSpidUsage.DEFAULT_sample_TotalUsageMb_includeInternalObjects : conf.getBooleanProperty(CmTempdbSpidUsage.PROPKEY_sample_TotalUsageMb_includeInternalObjects, CmTempdbSpidUsage.DEFAULT_sample_TotalUsageMb_includeInternalObjects));

		l_sampleSystemThreads_chk.setName(CmTempdbSpidUsage.PROPKEY_sample_systemThreads);
		l_sampleSystemThreads_chk.setToolTipText(TOOLTIP_sample_systemThreads);

		l_sampleSqlText_chk.setName(CmTempdbSpidUsage.PROPKEY_sample_sqlText);
		l_sampleSqlText_chk.setToolTipText(TOOLTIP_sample_sqlText);

//		l_sampleIntObj_chk.setName(CmTempdbSpidUsage.PROPKEY_sample_TotalUsageMb_includeInternalObjects);
//		l_sampleIntObj_chk.setToolTipText(TOOLTIP_sample_TotalUsageMb_includeInternalObjects);

		panel.add(l_sampleSystemThreads_chk  , "wrap");
		panel.add(l_sampleSqlText_chk        , "wrap");
//		panel.add(l_sampleIntObj_chk         , "wrap");
		panel.add(l_sampleTotalUsageMbMin_lbl, "wrap");
		panel.add(l_sampleTotalUsageMbMin_txt, "wrap");

		l_sampleSystemThreads_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmTempdbSpidUsage.PROPKEY_sample_systemThreads, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
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
				conf.setProperty(CmTempdbSpidUsage.PROPKEY_sample_sqlText, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		
//		l_sampleIntObj_chk.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				// Need TMP since we are going to save the configuration somewhere
//				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
//				if (conf == null) return;
//				conf.setProperty(CmTempdbSpidUsage.PROPKEY_sample_TotalUsageMb_includeInternalObjects, ((JCheckBox)e.getSource()).isSelected());
//				conf.save();
//				
//				// ReInitialize the SQL
//				getCm().setSql(null);
//			}
//		});
		
//		l_sampleTotalUsageMbMin_txt.addActionListener(new ActionListener()
		final ActionListener sampleTotalUsageMbMin_action = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				
				String strVal = l_sampleTotalUsageMbMin_txt.getText();
				double dblVal = CmTempdbSpidUsage.DEFAULT_sample_TotalUsageMb_min;
				try { dblVal = Double.parseDouble(strVal);}
				catch (NumberFormatException nfe)
				{
					dblVal = CmTempdbSpidUsage.DEFAULT_sample_TotalUsageMb_min;
					SwingUtils.showWarnMessage(CmTempdbSpidUsagePanel.this, "Not a Number", "<html>This must be a number, you entered '"+strVal+"'.<br>Setting to default value '"+dblVal+"'.</html>", nfe);
					l_sampleTotalUsageMbMin_txt.setText(dblVal+"");
				}
				conf.setProperty(CmTempdbSpidUsage.PROPKEY_sample_TotalUsageMb_min, dblVal);
				conf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);
			}
		};
		l_sampleTotalUsageMbMin_txt.addActionListener(sampleTotalUsageMbMin_action);
		l_sampleTotalUsageMbMin_txt.addFocusListener(new FocusListener()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				// Just call the "action" on sampleTopRowsCount_txt, so we don't have to duplicate code.
				sampleTotalUsageMbMin_action.actionPerformed(null);
			}
			
			@Override public void focusGained(FocusEvent e) {}
		});
		
		
		return panel;
	}
}

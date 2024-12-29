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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.sqlserver.CmIndexPhysical;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmIndexPhysicalPanel
extends TabularCntrPanel
{
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmOpenTransactions.CM_NAME;

	public CmIndexPhysicalPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

//		// PINK = spid is BLOCKED by some other user
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocked");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				Number BlockedBySpid = (Number) adapter.getValue(adapter.getColumnIndex("BlockedBySpid"));
//				if ( BlockedBySpid != null && BlockedBySpid.intValue() != 0 )
//					return true;
//				return false;
//			}
//		}, SwingUtils.parseColor(colorStr, Color.PINK), null));
//
//		// RED = spid is BLOCKING other spids from running
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocking");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				Number BlockingOtherSpidCount = (Number) adapter.getValue(adapter.getColumnIndex("BlockingOtherSpidCount"));
//				if ( BlockingOtherSpidCount != null && BlockingOtherSpidCount.intValue() > 0 )
//					return true;
//				return false;
//			}
//		}, SwingUtils.parseColor(colorStr, Color.RED), null));
	}

//	@Override
//	protected JPanel createLocalOptionsPanel()
//	{
//		JPanel panel = SwingUtils.createPanel("Local Options", true);
//		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));
//
//		Configuration conf = Configuration.getCombinedConfiguration();
//		JCheckBox sampleSystemThreads_chk = new JCheckBox("Show system processes", conf == null ? CmTempdbSpidUsage.DEFAULT_sample_systemThreads : conf.getBooleanProperty(CmTempdbSpidUsage.PROPKEY_sample_systemThreads, CmTempdbSpidUsage.DEFAULT_sample_systemThreads));
//
//		sampleSystemThreads_chk.setName(CmTempdbSpidUsage.PROPKEY_sample_systemThreads);
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
//				conf.setProperty(CmTempdbSpidUsage.PROPKEY_sample_systemThreads, ((JCheckBox)e.getSource()).isSelected());
//				conf.save();
//				
//				// ReInitialize the SQL
//				getCm().setSql(null);
//			}
//		});
//		
//		return panel;
//	}

	private JLabel            l_sampleMode_lbl;
	private JComboBox<String> l_sampleMode_cbx;

	private JLabel            l_sampleMinPageCount_lbl;
	private JTextField        l_sampleMinPageCount_txt;

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		LocalOptionsConfigPanel panel = new LocalOptionsConfigPanel("Local Options", new LocalOptionsConfigChanges()
		{
			@Override
			public void configWasChanged(String propName, String propVal)
			{
				Configuration conf = Configuration.getCombinedConfiguration();

//				list.add(new CmSettingsHelper("Detail Level", PROPKEY_sample_mode         , String .class, conf.getProperty(   PROPKEY_sample_mode         , DEFAULT_sample_mode         ), DEFAULT_sample_mode        , "Parameter 5 to the function 'dm_db_index_physical_stats' (valid values are DEFAULT, 'LIMITED', 'SAMPLED' or 'DETAILED')." ));
//				list.add(new CmSettingsHelper("Table Size",   PROPKEY_sample_minPageCount , Integer.class, conf.getIntProperty(PROPKEY_sample_minPageCount , DEFAULT_sample_minPageCount ), DEFAULT_sample_minPageCount, "How many data pages should the table have to be included." ));

				l_sampleMode_cbx.setSelectedItem(    conf.getProperty   (CmIndexPhysical.PROPKEY_sample_mode        , CmIndexPhysical.DEFAULT_sample_mode));
				l_sampleMinPageCount_txt.setText(""+ conf.getIntProperty(CmIndexPhysical.PROPKEY_sample_minPageCount, CmIndexPhysical.DEFAULT_sample_minPageCount));

				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

//		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		l_sampleMode_lbl = new JLabel("Mode");
		l_sampleMode_cbx = new JComboBox<String>(new String[] {"DEFAULT", "'LIMITED'", "'SAMPLED'", "'DETAILED'"});

		l_sampleMinPageCount_lbl = new JLabel("Min Page Count");
		l_sampleMinPageCount_txt = new JTextField(5);

		Configuration conf = Configuration.getCombinedConfiguration();
		String defaultMode = conf.getProperty(CmIndexPhysical.PROPKEY_sample_mode, CmIndexPhysical.DEFAULT_sample_mode);
		l_sampleMode_cbx.setSelectedItem(defaultMode);

		String defaultCount = "" + conf.getIntProperty(CmIndexPhysical.PROPKEY_sample_minPageCount, CmIndexPhysical.DEFAULT_sample_minPageCount);
		l_sampleMinPageCount_txt.setText(defaultCount);
		
		panel.add(l_sampleMode_lbl,         "");
		panel.add(l_sampleMode_cbx,         "span 2, wrap");

		panel.add(l_sampleMinPageCount_lbl, "");
		panel.add(l_sampleMinPageCount_txt, "growx, pushx, wrap");

		
		l_sampleMode_cbx.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmIndexPhysical.PROPKEY_sample_mode, l_sampleMode_cbx.getSelectedItem().toString());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

		final ActionListener sampleMinPageCount_action = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				
				String strVal = l_sampleMinPageCount_txt.getText();
				int    intVal = CmIndexPhysical.DEFAULT_sample_minPageCount;
				try { intVal = Integer.parseInt(strVal);}
				catch (NumberFormatException nfe)
				{
					intVal = CmIndexPhysical.DEFAULT_sample_minPageCount;
					SwingUtils.showWarnMessage(CmIndexPhysicalPanel.this, "Not a Number", "<html>This must be a number, you entered '"+strVal+"'.<br>Setting to default value '"+intVal+"'.</html>", nfe);
					l_sampleMinPageCount_txt.setText(intVal+"");
				}
				conf.setProperty(CmIndexPhysical.PROPKEY_sample_minPageCount, intVal);
				conf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);
			}
		};
		l_sampleMinPageCount_txt.addActionListener(sampleMinPageCount_action);
		l_sampleMinPageCount_txt.addFocusListener(new FocusListener()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				// Just call the "action" on sampleTopRowsCount_txt, so we don't have to duplicate code.
				sampleMinPageCount_action.actionPerformed(null);
			}
			
			@Override public void focusGained(FocusEvent e) {}
		});
		
		
		return panel;
	}
}

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
package com.asetune.cm.ase.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.CmTableStatistics;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmTableStatisticsPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmTableStatisticsPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmTableStatisticsPanel.CM_NAME;

	private JCheckBox l_sampleSpaceUsage_chk;
	private JCheckBox l_sampleSystemTables_chk;
	private JCheckBox l_samplePartitions_chk;

	public CmTableStatisticsPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// (LockSchema == 'allpages') >>>> extremely light blue, close to while
		if (conf != null) colorStr = conf.getProperty(getName()+".color.apl");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String LockSchema = adapter.getString(adapter.getColumnIndex("LockSchema"));
				if ( "allpages".equalsIgnoreCase(LockSchema))
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, new Color(230, 242, 255)), null));

		// Table do not have any indexes >>>> extremely light pink
		if (conf != null) colorStr = conf.getProperty(getName()+".color.noindexes");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number IndexCount = (Number) adapter.getValue(adapter.getColumnIndex("IndexCount"));
				if ( IndexCount != null && IndexCount.intValue() == 0)
					return true;
				return false;
			}
//		}, SwingUtils.parseColor(colorStr, new Color(255, 51, 0)), null));
		}, SwingUtils.parseColor(colorStr, new Color(255, 230, 230)), null));

		// (IndexID > 0) >>>> ORANGE
		if (conf != null) colorStr = conf.getProperty(getName()+".color.index");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number indexId    = (Number) adapter.getValue (adapter.getColumnIndex("IndexID"));
				String lockSchema =          adapter.getString(adapter.getColumnIndex("LockSchema"));
				if ("allpages".equals(lockSchema) && indexId != null && indexId.intValue() <= 1)
					return false;
				if ( indexId != null && indexId.intValue() > 0)
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));

		// BLOB (text/image columns)
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blob");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number indexId = (Number) adapter.getValue(adapter.getColumnIndex("IndexID"));
				if ( indexId != null && indexId.intValue() == 255)
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, ColorConstants.COLOR_DATATYPE_BLOB), null));

		// System Tables >>>> YELLOW
		if (conf != null) colorStr = conf.getProperty(getName()+".color.apl");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number ObjectID = (Number) adapter.getValue(adapter.getColumnIndex("ObjectID"));
				if ( ObjectID != null && ObjectID.intValue() < 100)
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.YELLOW), null));
	}

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean defaultBolOpt;
		int     defaultIntOpt;

		//-----------------------------------------
		// RowCount
		//-----------------------------------------
		defaultBolOpt = conf == null ? CmTableStatistics.DEFAULT_sample_spaceUsage : conf.getBooleanProperty(CmTableStatistics.PROPKEY_sample_spaceUsage, CmTableStatistics.DEFAULT_sample_spaceUsage);
		l_sampleSpaceUsage_chk = new JCheckBox("Sample Space Usage", defaultBolOpt);

		l_sampleSpaceUsage_chk.setName(CmTableStatistics.PROPKEY_sample_spaceUsage);
		l_sampleSpaceUsage_chk.setToolTipText("<html>"
				+ "Sample Table Space Usage with ASE functions <code>data_pages()</code> and <code>reserved_pages()</code>.<br>"
				+ "This will affect the columns 'PageUtilization', 'ActualDataPages', 'ActualIndexPages', 'ReservedPages'."
				+ "</html>");

		l_sampleSpaceUsage_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmTableStatistics.PROPKEY_sample_spaceUsage, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);
			}
		});
		
		//-----------------------------------------
		// Minimum Page Limit
		//-----------------------------------------
		defaultBolOpt = conf == null ? CmTableStatistics.DEFAULT_sample_minPageLimit      : conf.getBooleanProperty(CmTableStatistics.PROPKEY_sample_minPageLimit,      CmTableStatistics.DEFAULT_sample_minPageLimit);
		defaultIntOpt = conf == null ? CmTableStatistics.DEFAULT_sample_minPageLimitCount : conf.getIntProperty    (CmTableStatistics.PROPKEY_sample_minPageLimitCount, CmTableStatistics.DEFAULT_sample_minPageLimitCount);
		final JCheckBox  sampleMinPageLimit_chk      = new JCheckBox("Minimum Number of Pages", defaultBolOpt);
		final JTextField sampleMinPageLimitCount_txt = new JTextField(Integer.toString(defaultIntOpt), 6);

		sampleMinPageLimit_chk.setName(CmTableStatistics.PROPKEY_sample_minPageLimit);
		sampleMinPageLimit_chk.setToolTipText("<html>Only fetch Table Information for Tables that has more than "+CmTableStatistics.DEFAULT_sample_minPageLimitCount+" Pages. So Skip <i>smaller</i> tables.<br>Note: When it's disabled, the Page Limit will be set to 1.</html>");

		sampleMinPageLimitCount_txt.setName(CmTableStatistics.PROPKEY_sample_minPageLimitCount);
		sampleMinPageLimitCount_txt.setToolTipText("<html>Only fetch Table Information for Tables that has more than "+CmTableStatistics.DEFAULT_sample_minPageLimitCount+" Pages. So Skip <i>smaller</i> tables<br>Note: When it's disabled, the Page Limit will be set to 1.</html>");

		sampleMinPageLimit_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmTableStatistics.PROPKEY_sample_minPageLimit, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);
			}
		});
		
		final ActionListener sampleMinPageLimitCount_action = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				
				String strVal = sampleMinPageLimitCount_txt.getText();
				int    intVal = CmTableStatistics.DEFAULT_sample_minPageLimitCount;
				try { intVal = Integer.parseInt(strVal);}
				catch (NumberFormatException nfe)
				{
					intVal = CmTableStatistics.DEFAULT_sample_minPageLimitCount;
					SwingUtils.showWarnMessage(CmTableStatisticsPanel.this, "Not a Number", "<html>This must be a number, you entered '"+strVal+"'.<br>Setting to default value '"+intVal+"'.</html>", nfe);
					sampleMinPageLimitCount_txt.setText(intVal+"");
				}
				conf.setProperty(CmTableStatistics.PROPKEY_sample_minPageLimitCount, intVal);
				conf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);
			}
		};
		sampleMinPageLimitCount_txt.addActionListener(sampleMinPageLimitCount_action);
		sampleMinPageLimitCount_txt.addFocusListener(new FocusListener()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				// Just call the "action" on sampleMinPageLimitCount_txt, so we don't have to duplicate code.
				sampleMinPageLimitCount_action.actionPerformed(null);
			}
			
			@Override public void focusGained(FocusEvent e) {}
		});
		
		
		//-----------------------------------------
		// sample system tables
		//-----------------------------------------
		defaultBolOpt = conf == null ? CmTableStatistics.DEFAULT_sample_systemTables : conf.getBooleanProperty(CmTableStatistics.PROPKEY_sample_systemTables, CmTableStatistics.DEFAULT_sample_systemTables);
		l_sampleSystemTables_chk = new JCheckBox("Include System Tables", defaultBolOpt);

		l_sampleSystemTables_chk.setName(CmTableStatistics.PROPKEY_sample_systemTables);
		l_sampleSystemTables_chk.setToolTipText("<html>Include system tables in the output<br> </html>");

		l_sampleSystemTables_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmTableStatistics.PROPKEY_sample_systemTables, ((JCheckBox)e.getSource()).isSelected());
				conf.save();

				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);
			}
		});
		
		//-----------------------------------------
		// sample system tables
		//-----------------------------------------
		defaultBolOpt = conf == null ? CmTableStatistics.DEFAULT_sample_partitions : conf.getBooleanProperty(CmTableStatistics.PROPKEY_sample_partitions, CmTableStatistics.DEFAULT_sample_partitions);
		l_samplePartitions_chk = new JCheckBox("Stats at Partition Level", defaultBolOpt);

		l_samplePartitions_chk.setName(CmTableStatistics.PROPKEY_sample_systemTables);
		l_samplePartitions_chk.setToolTipText("<html>Sample statistics on a partition level, not summarized at the table level.</html>");

		l_samplePartitions_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmTableStatistics.PROPKEY_sample_partitions, ((JCheckBox)e.getSource()).isSelected());
				conf.save();

				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);
			}
		});
		
		//-----------------------------------------
		// LAYOUT
		//-----------------------------------------
		panel.add(l_sampleSpaceUsage_chk,      "wrap");
		
		panel.add(sampleMinPageLimit_chk,      "split");
		panel.add(sampleMinPageLimitCount_txt, "wrap");

		panel.add(l_sampleSystemTables_chk,    "wrap");

		panel.add(l_samplePartitions_chk,      "wrap");

		return panel;
	}

	@Override
	public void checkLocalComponents()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		// PROPKEY_sample_systemTables
		boolean confProp = conf.getBooleanProperty(CmTableStatistics.PROPKEY_sample_systemTables, CmTableStatistics.DEFAULT_sample_systemTables);
		boolean guiProp  = l_sampleSystemTables_chk.isSelected();

		if (confProp != guiProp)
			l_sampleSystemTables_chk.setSelected(confProp);

		
		confProp = conf.getBooleanProperty(CmTableStatistics.PROPKEY_sample_spaceUsage, CmTableStatistics.DEFAULT_sample_spaceUsage);
		guiProp  = l_sampleSpaceUsage_chk.isSelected();

		if (confProp != guiProp)
			l_sampleSpaceUsage_chk.setSelected(confProp);
	}
}

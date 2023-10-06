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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.IconHighlighter;

import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.CmCachedProcs;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmCachedProcsPanel
extends TabularCntrPanel
{
	private JCheckBox l_sample_statementCacheObjects_chk;
	private JCheckBox l_sample_dynamicSqlObjects_chk;

	public final static String TOOLTIP_sample_statementCacheObjects = "<html>Normally you will find Statement Cache entries in the StatementCache Details Collector, but you can also enable it here.</html>";
	public final static String TOOLTIP_sample_dynamicSqlObjects     = "<html>Normally you will find Dynamic SQL Statements entries in the StatementCache Details Collector, but you can also enable it here.</html>";

	//	private static final Logger  _logger	           = Logger.getLogger(CmCachedProcsPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmCachedProcsPanel.CM_NAME;

	public CmCachedProcsPanel(CountersModel cm)
	{
		super(cm);

//		if (cm.getIconFile() != null)
//			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
		// PROCEDURE ICON
		addHighlighter( new IconHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				int modelCol = adapter.getColumnIndex("ObjectType");
				if (modelCol == adapter.convertColumnIndexToModel(adapter.column))
				{
					String objectType = adapter.getString(modelCol);
					if (objectType != null)
						objectType = objectType.trim();
					if ( objectType.startsWith("stored procedure"))
						return true;
				}
				return false;
			}
		}, SwingUtils.readImageIcon(Version.class, "images/highlighter_procedure.png")));
					
		// TRIGGER ICON
		addHighlighter( new IconHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				int modelCol = adapter.getColumnIndex("ObjectType");
				if (modelCol == adapter.convertColumnIndexToModel(adapter.column))
				{
					String objectType = adapter.getString(modelCol);
					if (objectType != null)
						objectType = objectType.trim();
					if ( objectType.startsWith("trigger"))
						return true;
				}
				return false;
			}
		}, SwingUtils.readImageIcon(Version.class, "images/highlighter_trigger.png")));

		// VIEW ICON
		addHighlighter( new IconHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				int modelCol = adapter.getColumnIndex("ObjectType");
				if (modelCol == adapter.convertColumnIndexToModel(adapter.column))
				{
					String objectType = adapter.getString(modelCol);
					if (objectType != null)
						objectType = objectType.trim();
					if ( objectType.startsWith("view"))
						return true;
				}
				return false;
			}
		}, SwingUtils.readImageIcon(Version.class, "images/highlighter_view.png")));

		// DEFAULT VALUE icon
		addHighlighter( new IconHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				int modelCol = adapter.getColumnIndex("ObjectType");
				if (modelCol == adapter.convertColumnIndexToModel(adapter.column))
				{
					String objectType = adapter.getString(modelCol);
					if (objectType != null)
						objectType = objectType.trim();
					if ( objectType.startsWith("default value spec"))
						return true;
				}
				return false;
			}
		}, SwingUtils.readImageIcon(Version.class, "images/highlighter_default_value.png")));

		// RULE icon
		addHighlighter( new IconHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				int modelCol = adapter.getColumnIndex("ObjectType");
				if (modelCol == adapter.convertColumnIndexToModel(adapter.column))
				{
					String objectType = adapter.getString(modelCol);
					if (objectType != null)
						objectType = objectType.trim();
					if ( objectType.startsWith("rule"))
						return true;
				}
				return false;
			}
		}, SwingUtils.readImageIcon(Version.class, "images/highlighter_rule.png")));
	}

	
	@Override
	protected JPanel createLocalOptionsPanel()
	{
		LocalOptionsConfigPanel panel = new LocalOptionsConfigPanel("Local Options", new LocalOptionsConfigChanges()
		{
			@Override
			public void configWasChanged(String propName, String propVal)
			{
				Configuration conf = Configuration.getCombinedConfiguration();

				l_sample_statementCacheObjects_chk.setSelected(conf.getBooleanProperty(CmCachedProcs.PROPKEY_sample_statementCacheObjects, CmCachedProcs.DEFAULT_sample_statementCacheObjects));
				l_sample_dynamicSqlObjects_chk    .setSelected(conf.getBooleanProperty(CmCachedProcs.PROPKEY_sample_dynamicSqlObjects,     CmCachedProcs.DEFAULT_sample_dynamicSqlObjects));

				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

//		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();

		l_sample_statementCacheObjects_chk = new JCheckBox("Sample Lightweight Procs for Statement Cache",        conf == null ? CmCachedProcs.DEFAULT_sample_statementCacheObjects : conf.getBooleanProperty(CmCachedProcs.PROPKEY_sample_statementCacheObjects, CmCachedProcs.DEFAULT_sample_statementCacheObjects));
		l_sample_dynamicSqlObjects_chk     = new JCheckBox("Sample Lightweight Procs for Dynamic SQL Statements", conf == null ? CmCachedProcs.DEFAULT_sample_dynamicSqlObjects     : conf.getBooleanProperty(CmCachedProcs.PROPKEY_sample_dynamicSqlObjects,     CmCachedProcs.DEFAULT_sample_dynamicSqlObjects));

		l_sample_statementCacheObjects_chk.setName(CmCachedProcs.PROPKEY_sample_statementCacheObjects);
		l_sample_dynamicSqlObjects_chk    .setName(CmCachedProcs.PROPKEY_sample_dynamicSqlObjects);

		l_sample_dynamicSqlObjects_chk    .setToolTipText(TOOLTIP_sample_dynamicSqlObjects);
		l_sample_statementCacheObjects_chk.setToolTipText(TOOLTIP_sample_statementCacheObjects);

		panel.add(l_sample_statementCacheObjects_chk, "wrap");
		panel.add(l_sample_dynamicSqlObjects_chk,     "wrap");

		l_sample_statementCacheObjects_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmCachedProcs.PROPKEY_sample_statementCacheObjects, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		
		l_sample_dynamicSqlObjects_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmCachedProcs.PROPKEY_sample_dynamicSqlObjects, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		
		return panel;
	}

//	@Override
//	public void checkLocalComponents()
//	{
//		CountersModel cm = getCm();
//		if (cm != null)
//		{
//			if (cm.isRuntimeInitialized())
//			{
//				// disable CachedPlanInXml is not 16.0
//				if ( cm.getServerVersion() > Ver.ver(16,0))
//				{
//					l_sampleSqlText_chk.setEnabled(true);
//				}
//				else
//				{
//					l_sampleSqlText_chk.setEnabled(false);
//
//					Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
//					if (conf != null)
//					{
//						conf.setProperty(CmProcessActivity.PROPKEY_sample_sqlText, false);
//					}
//				}
//			} // end isRuntimeInitialized
//		} // end (cm != null)
//	}
}

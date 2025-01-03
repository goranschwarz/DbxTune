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
package com.dbxtune.cm.postgres.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.invoke.MethodHandles;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.postgres.CmPgPidWait;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmPgPidWaitPanel
extends TabularCntrPanel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID      = 1L;

	public static final String  TOOLTIP_show_systemThreads = "<html>Show System PID's that executes in Postgres.<br><b>Note</b>: Filter out where 'datname' is NULL.</html>";
	public static final String  TOOLTIP_show_clientRead    = "<html>Show wait event 'ClientRead', when the server is waiting for input from client.</html>";
	
	private JCheckBox l_showSystemThreads_chk;
	private JCheckBox l_showClientRead_chk;

	public CmPgPidWaitPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// HIGHLIGHTER that changes color when a new SPID number is on next row...

		if (conf != null) 
			colorStr = conf.getProperty(getName()+".color.group");

		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			boolean[] _rowIsHighlighted = new boolean[0];
			int       _lastRowId        = 0;     // Used to sheet on table refresh

			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (adapter.row == 0)
					return false;

				// Resize array if it's to small
				if (_rowIsHighlighted.length < adapter.getRowCount())
					_rowIsHighlighted = new boolean[adapter.getRowCount()];

				// Lets try to sheet a bit, if we are of some row as last invocation, reuse that decision
				if (_lastRowId == adapter.row)
					return _rowIsHighlighted[adapter.row];
				_lastRowId = adapter.row;

				// Lets get values of "change color" column
				int    spidCol      = adapter.getColumnIndex("pid");
				int    thisModelRow = adapter.convertRowIndexToModel(adapter.row);
				int    prevModelRow = adapter.convertRowIndexToModel(adapter.row - 1);

				Object thisSpid    = adapter.getValueAt(thisModelRow, spidCol);
				Object prevSpid    = adapter.getValueAt(prevModelRow, spidCol);

				if (thisSpid == null) thisSpid = "dummy";
				if (prevSpid == null) prevSpid = "dummy";

				// Previous rows highlight will be a decision to keep or invert the highlight
				boolean prevRowIsHighlighted = _rowIsHighlighted[adapter.row - 1];

				if (thisSpid.equals(prevSpid))
				{
					// Use same highlight value as previous row
					boolean isHighlighted = prevRowIsHighlighted;
					_rowIsHighlighted[adapter.row] = isHighlighted;

					return isHighlighted;
				}
				else
				{
					// Invert previous highlight value
					boolean isHighlighted = ! prevRowIsHighlighted;
					_rowIsHighlighted[adapter.row] = isHighlighted;

					return isHighlighted;
				}
			}
		}, SwingUtils.parseColor(colorStr, HighlighterFactory.GENERIC_GRAY), null));

	
		// GREEN = active
		if (conf != null) colorStr = conf.getProperty(getName()+".color.active");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if ( ! "state".equals(adapter.getColumnName(adapter.column)) )
					return false;
				
				return "active".equals(adapter.getString());
					
//				String status = (String) adapter.getValue(adapter.getColumnIndex("state"));
//				if ( status != null && status.equals("active") )
//					return true;
//				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.GREEN), null));
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

//				list.add(new CmSettingsHelper("Sample System Threads", PROPKEY_show_systemThreads , Boolean.class, conf.getBooleanProperty(PROPKEY_show_systemThreads  , DEFAULT_show_systemThreads  ), DEFAULT_show_systemThreads, CmPgPidWaitPanel.TOOLTIP_show_systemThreads ));

				l_showSystemThreads_chk.setSelected(conf.getBooleanProperty(CmPgPidWait.PROPKEY_show_systemThreads, CmPgPidWait.DEFAULT_show_systemThreads));
				l_showClientRead_chk   .setSelected(conf.getBooleanProperty(CmPgPidWait.PROPKEY_show_clientRead   , CmPgPidWait.DEFAULT_show_clientRead));

				// If the 'l_showSystemThreads_chk' the table needs to be updated... so that filters are applied
				getCm().fireTableDataChanged();

				// ReInitialize the SQL
				//getCm().setSql(null);
			}
		});

//		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		l_showSystemThreads_chk = new JCheckBox("Show system processes",   conf == null ? CmPgPidWait.DEFAULT_show_systemThreads : conf.getBooleanProperty(CmPgPidWait.PROPKEY_show_systemThreads, CmPgPidWait.DEFAULT_show_systemThreads));
		l_showClientRead_chk    = new JCheckBox("Show event 'ClientRead'", conf == null ? CmPgPidWait.DEFAULT_show_clientRead    : conf.getBooleanProperty(CmPgPidWait.PROPKEY_show_clientRead   , CmPgPidWait.DEFAULT_show_clientRead));

		l_showSystemThreads_chk.setName(CmPgPidWait.PROPKEY_show_systemThreads);
		l_showSystemThreads_chk.setToolTipText(TOOLTIP_show_systemThreads);

		l_showClientRead_chk.setName(CmPgPidWait.PROPKEY_show_clientRead);
		l_showClientRead_chk.setToolTipText(TOOLTIP_show_clientRead);

		panel.add(l_showSystemThreads_chk, "wrap");
		panel.add(l_showClientRead_chk   , "wrap");

		l_showSystemThreads_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// If the 'l_showSystemThreads_chk' the table needs to be updated... so that filters are applied
				getCm().fireTableDataChanged();
			}
		});
		
		l_showClientRead_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// If the 'l_showSystemThreads_chk' the table needs to be updated... so that filters are applied
				getCm().fireTableDataChanged();
			}
		});
		
		addRowFilter(new RowFilter<TableModel, Integer>()
		{
			@Override
			public boolean include(Entry<? extends TableModel, ? extends Integer> entry)
			{
				boolean doInclude = true;
				
				boolean isSystemPid       = false;
				boolean isEventClientRead = false;

				// NO Filters
				if ( l_showSystemThreads_chk.isSelected() && l_showClientRead_chk.isSelected() )
				{
					return doInclude;
				}
				else
				{
					int pos__datname = ((AbstractTableModel)entry.getModel()).findColumn("datname");
					int pos__event   = ((AbstractTableModel)entry.getModel()).findColumn("event");

					doInclude = true;
					isSystemPid       = (String)entry.getValue(pos__datname) == null;
					isEventClientRead = "ClientRead".equals( (String)entry.getValue(pos__event) );

					if (doInclude && isSystemPid && !l_showSystemThreads_chk.isSelected())
						doInclude = false;

					if (doInclude && isEventClientRead && !l_showClientRead_chk.isSelected())
						doInclude = false;

					return doInclude;
				}
			}
		});
		
		return panel;
	}
	
}

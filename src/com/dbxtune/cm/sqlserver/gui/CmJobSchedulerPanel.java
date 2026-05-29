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

import javax.swing.SwingConstants;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.StringValues;
import org.jdesktop.swingx.table.ColumnFactory;
import org.jdesktop.swingx.table.TableColumnExt;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.gui.swing.GTable;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

public class CmJobSchedulerPanel
extends TabularCntrPanel
{
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmWaitingTasksPanel.CM_NAME;

	public CmJobSchedulerPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
//	"    <li>GREEN       - RUNNING_OK      - Jobs is Currently running</li>" +
//	"    <li>ORANGE      - RUNNING_LONG    - Job is Running longer than it usually does.</li>" +
//	"    <li>PINK        - MISSED          - Job was scheduled to run, but it hasn't.</li>" +
//	"    <li>RED         - FAILED_LAST_RUN - Jobs Failed on last execution.</li>" +

	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// GREEN = RUNNING_OK
		if (conf != null) colorStr = conf.getProperty(getName()+".color.RUNNING_OK");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String status = (String) adapter.getValue(adapter.getColumnIndex("status"));
				if ( "RUNNING_OK".equals(status) )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.GREEN), null));

		// ORANGE = RUNNING_LONG
		if (conf != null) colorStr = conf.getProperty(getName()+".color.RUNNING_LONG");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String status = (String) adapter.getValue(adapter.getColumnIndex("status"));
				if ( "RUNNING_LONG".equals(status) )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));

		// PINK = MISSED
		if (conf != null) colorStr = conf.getProperty(getName()+".color.MISSED");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String status = (String) adapter.getValue(adapter.getColumnIndex("status"));
				if ( "MISSED".equals(status) )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.PINK), null));

		// RED = FAILED_LAST_RUN
		if (conf != null) colorStr = conf.getProperty(getName()+".color.FAILED_LAST_RUN");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String status = (String) adapter.getValue(adapter.getColumnIndex("status"));
				if ( "FAILED_LAST_RUN".equals(status) )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.RED), null));

	}
	
	@Override
	protected void configureTableColumns(GTable dataTable)
	{
		super.configureTableColumns(dataTable);

		dataTable.setColumnFactory(new ColumnFactory() 
		{
			@Override
			public void configureTableColumn(TableModel model, TableColumnExt columnExt) 
			{
				super.configureTableColumn(model, columnExt);
				
				String headerValue = columnExt.getHeaderValue().toString();
				if (headerValue.toLowerCase().endsWith("hms")) 
				{
					columnExt.setCellRenderer(new DefaultTableRenderer(StringValues.TO_STRING, SwingConstants.RIGHT));
				}
			}
		});
	}
}

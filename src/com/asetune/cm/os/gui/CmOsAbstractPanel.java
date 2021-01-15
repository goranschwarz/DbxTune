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
package com.asetune.cm.os.gui;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JLabel;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.LabelProvider;
import org.jdesktop.swingx.renderer.StringValue;
import org.jdesktop.swingx.renderer.StringValues;

import com.asetune.cm.CounterModelHostMonitor;
import com.asetune.cm.CountersModel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.swing.GTable;
import com.asetune.hostmon.HostMonitor.OsVendor;
import com.asetune.utils.Configuration;

public abstract class CmOsAbstractPanel
extends TabularCntrPanel
{
	private static Logger     _logger          = Logger.getLogger(CmOsAbstractPanel.class);
	private static final long serialVersionUID = 1L;

	public static final String  PROPKEY_TABLE_CELL_RENDERER_MIN_NUMBER_DECIMALS = "CounterModelHostMonitor.windows.perfmon.cellRenderer.format.min.Number.decimals";
	public static final int     DEFAULT_TABLE_CELL_RENDERER_MIN_NUMBER_DECIMALS = 0;
	public static final String  PROPKEY_TABLE_CELL_RENDERER_MAX_NUMBER_DECIMALS = "CounterModelHostMonitor.windows.perfmon.cellRenderer.format.max.Number.decimals";
	public static final int     DEFAULT_TABLE_CELL_RENDERER_MAX_NUMBER_DECIMALS = 128;

	
	private boolean _tableCellRendersIsInitialized = false;

	public CmOsAbstractPanel(CountersModel cm)
	{
		super(cm);
	}

	@Override
	public void resetCm()
	{
		super.resetCm();
		_tableCellRendersIsInitialized = false;
	}

	/** 
	 * possible called from <code>checkLocalComponents()</code> in any sub classes to set Cell Renders to a "more" deicmals
	 * */
	public void setTableCellRenders()
	{
		//System.out.println("setTableProps(): initialized=" + _initialized);

		if (_tableCellRendersIsInitialized)
			return;

		if (getCm() instanceof CounterModelHostMonitor)
		{
			CounterModelHostMonitor cm = (CounterModelHostMonitor) getCm();
			GTable table = getDataTable();

			if ( ! cm.isConnectedToVendor(OsVendor.Windows) )
			{
				// If we disconnect from Windows and connect to a "Unix/Linux" system we might want to "reset" the renders...
				table.setDefaultRenderers();
			}
			else
			{
				// BigDecimal, Double, Float format
				@SuppressWarnings("serial")
				StringValue svInExactNumber = new StringValue() 
				{
//					int decimals = Configuration.getCombinedConfiguration().getIntProperty("ResultSetJXTable.cellRenderer.format.BigDecimal.decimals", 3);
					int minDecimals = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_TABLE_CELL_RENDERER_MIN_NUMBER_DECIMALS, DEFAULT_TABLE_CELL_RENDERER_MIN_NUMBER_DECIMALS);
					int maxDecimals = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_TABLE_CELL_RENDERER_MAX_NUMBER_DECIMALS, DEFAULT_TABLE_CELL_RENDERER_MAX_NUMBER_DECIMALS);

					NumberFormat nf = null;
					{ // init/constructor section
						try
						{
							nf = new DecimalFormat();
							nf.setMinimumFractionDigits(minDecimals);
							nf.setMaximumFractionDigits(maxDecimals);
						}
						catch (Throwable t)
						{
							nf = NumberFormat.getInstance();
						}
					}
					@Override
					public String getString(Object value) 
					{
						try
						{
							if ( ! (value instanceof Number) ) 
								return StringValues.TO_STRING.getString(value);
							
							return nf.format(value);
						}
						catch (RuntimeException rte)
						{
							_logger.warn("Problems to render... the value |" + value + "| class=" + (value == null ? null : value.getClass().getName()) + ". returning 'toString instead'. Caught: " + rte, rte);
							
							return StringValues.TO_STRING.getString(value);
						}
					}
				};
				DefaultTableRenderer InExactNumberRenderer = new DefaultTableRenderer( new LabelProvider(svInExactNumber, JLabel.TRAILING) );
				
				table.setDefaultRenderer(BigDecimal.class, InExactNumberRenderer);
				table.setDefaultRenderer(Double    .class, InExactNumberRenderer);
				table.setDefaultRenderer(Float     .class, InExactNumberRenderer);
			}
		}
		
		_tableCellRendersIsInitialized = true;
	}
}

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
package com.asetune.hostmon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.h2.util.StringUtils;

import com.asetune.config.dict.WindowsPerformanceCountersDictionary;
import com.asetune.hostmon.WindowsTypePerfCsvReader.CounterColumnRewrite;
import com.asetune.utils.StringUtil;


public class HostMonitorMetaDataWindowsTypePerf
extends HostMonitorMetaData
{
	private static Logger _logger = Logger.getLogger(HostMonitorMetaDataWindowsTypePerf.class);

	WindowsTypePerfCsvReader _csvReader = new WindowsTypePerfCsvReader();

	@Override
	public void doInitializeUsingFirstRow(String row)
	{
		if (row.startsWith("(PDH-CSV ", 1))
		{
			_csvReader.readHeader(row);
			
			List<String> percentColumns = new ArrayList<>();

			int sqlPosExtraCol = 0; // Increment if/when a extra column is added, which is NOT part if the "parsed input"
			int sqlPosSkipCol  = 0; // Decrement if/when a column is not needed, which IS part if the "parsed input"
//			int sqlPos = 1;
//			int arrPos = 0;
			for (Entry<String, Integer> entry : _csvReader.getCounterNames().entrySet())
			{
				String colName     = entry.getKey();
				int    colSqlPos   = entry.getValue() + 1 + sqlPosExtraCol + sqlPosSkipCol;
				int    colParsePos = entry.getValue() + 1;

//System.out.println("    [" + getTableName() + "] >>> colSqlPos="+colSqlPos+", colParsePos="+colParsePos+", colName='"+colName+"'");
				
				if (WindowsTypePerfCsvReader.COLNAME_TIMESTAMP.equals(colName)) 
				{
//					sqlPosSkipCol--;
					addDatetimeColumn( colName, colSqlPos, colParsePos, false,     "When the data was sampled");

					// Add "extra" column to read how many samples the value is calculated/based on
					sqlPosExtraCol++;
					colSqlPos += sqlPosExtraCol + sqlPosSkipCol;
					addIntColumn( "samples",  colSqlPos,  0, true, "Number of 'sub' sample entries of 'typeperf' this value is based on");
				}
				else if (WindowsTypePerfCsvReader.COLNAME_HOSTNAME .equals(colName)) 
				{
					addStrColumn( colName, colSqlPos, colParsePos, false, 30, "Hostname where the data was sampled");
				}
				else if (WindowsTypePerfCsvReader.COLNAME_GROUP    .equals(colName)) 
				{
					addStrColumn( colName, colSqlPos, colParsePos, false, 60, "Perfmon Counter Group the Counter belongs to");
				}
				else if (WindowsTypePerfCsvReader.COLNAME_INSTANCE .equals(colName)) 
				{
					addStrColumn( colName, colSqlPos, colParsePos, false, 60, "Perfmon Counter Group INSTANCE the Counter belongs to");
				}
				else 
				{
				//	addDecColumn( colName, colSqlPos, colParsePos, false, 20, 6, "");
					addStatColumn( colName, colSqlPos, colParsePos, false, 20, 6, "");
					
					if (colName.contains("%"))
						percentColumns.add(colName);
				}
			}

			// What counter Groups was used...
			setDescriptionGroup(_csvReader.getCounterGroups().toArray(new String[0]));
			
			// Set Percent Columns
			if ( ! percentColumns.isEmpty() )
				setPercentCol(percentColumns.toArray(new String[0]));
			
			// set PrimaryKey
			setPkCol(WindowsTypePerfCsvReader.COLNAME_HOSTNAME, WindowsTypePerfCsvReader.COLNAME_GROUP, WindowsTypePerfCsvReader.COLNAME_INSTANCE);

			// Set DATE parse format
			setDateParseFormatCol(WindowsTypePerfCsvReader.COLNAME_TIMESTAMP, "yyyy-MM-dd HH:mm:ss.SSS");
			
			// One input-row produces several output-rows (table rows) or OneRow to ManyRows
			setUnPivot(true);
			
			// Set column "samples", to a special status, which will contain number of 
			// underlying samples the summary/average calculation was based on
			setStatusCol("samples", HostMonitorMetaData.STATUS_COL_SUB_SAMPLE);

			// Add any user defined columns in dynamic initialization
			addUserDefinedCountersInInitializeUsingFirstRow();
			
			// Mark as initialized
			setFirstRowInitDone(true);
		}
	}

	@Override
	public String[][] parseRowOneToMany(HostMonitorMetaData md, String row, String[] preParsed, int type)
	{
		String[][] rows = _csvReader.readRowToStringRows(row);
		
		// Debug logging
		if (_logger.isDebugEnabled())
		{
			for (int r=0; r<rows.length; r++)
				_logger.debug("ROW[" + getTableName() + "][" + r + "][len=" + rows[r].length + "]: " + StringUtil.toCommaStr(rows[r]));
		}
//        for (int r=0; r<rows.length; r++)
//        	System.out.println("ROW[" + getTableName() + "][" + r + "][len=" + rows[r].length + "]: " + StringUtil.toCommaStr(rows[r]));
		
		return rows;
	}

	@Override
	public String getDescription(String colname)
	{
		String[] groups = getDescriptionGroup();

		for (String group : groups)
		{
			String toolTip = WindowsPerformanceCountersDictionary.getInstance().getDescriptionPlain(group, colname);
			if (StringUtils.isNullOrEmpty(toolTip))
				continue;

			if (toolTip.startsWith("<html>"))
				return toolTip;

			return "<html>" + StringUtil.makeApproxLineBreak(toolTip, 100, 10, "<br>\n") + "</html>";
		}
		
		return super.getDescription(colname);
	}

	public void setInitializeUsingFirstRowRewriteRules(Map<CounterColumnRewrite, CounterColumnRewrite> rewriteRules)
	{
		_csvReader.setCounterRewriteRules(rewriteRules);
	}
}

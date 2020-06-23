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
package com.asetune.pcs.report.content.sqlserver;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.asetune.gui.ModelMissmatchException;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ReportEntryAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;

public abstract class SqlServerAbstract
extends ReportEntryAbstract
{
	private static Logger _logger = Logger.getLogger(SqlServerAbstract.class);

	public SqlServerAbstract(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	public Set<String> getPlanHandleObjects(ResultSetTableModel rstm, String colName)
	{
		Set<String> set = new LinkedHashSet<>();
		
		int pos_colName = rstm.findColumn(colName);
		if (pos_colName != -1)
		{
			for (int r=0; r<rstm.getRowCount(); r++)
			{
				String name = rstm.getValueAsString(r, pos_colName);
				
				if (name != null && name.trim().startsWith("0x"))
				{
					set.add(name);
				}
			}
		}
		
		return set;
	}

	public ResultSetTableModel getShowplanFromMonDdlStorage(DbxConnection conn, Set<String> nameSet)
	throws SQLException
	{
		ResultSetTableModel planRstm = null;
		
		for (String name : nameSet)
		{
			String sql = ""
				    + "select [objectName], [extraInfoText] as [SQLText] \n"
				    + "from [MonDdlStorage] \n"
				    + "where 1 = 1 \n"
				    + "  and [dbname]     = 'statement_cache' \n"
//				    + "  and [owner]      = 'ssql' \n"
				    + "  and [objectName] = " + DbUtils.safeStr(name) + " \n"
				    + "";
			
			sql = conn.quotifySqlString(sql);
			try ( Statement stmnt = conn.createStatement() )
			{
				// Unlimited execution time
				stmnt.setQueryTimeout(0);
				try ( ResultSet rs = stmnt.executeQuery(sql) )
				{
//					ResultSetTableModel rstm = new ResultSetTableModel(rs, "planRstm");
					ResultSetTableModel rstm = createResultSetTableModel(rs, "planRstm", sql);
					
					if (planRstm == null)
						planRstm = rstm;
					else
						planRstm.add(rstm);

					if (_logger.isDebugEnabled())
						_logger.debug("_planRstm.getRowCount()="+ rstm.getRowCount());
				}
			}
			catch(SQLException ex)
			{
				//_problem = ex;

				_logger.warn("Problems getting SQL Statement name = '"+name+"': " + ex);
				throw ex;
			} 
			catch(ModelMissmatchException ex)
			{
				//_problem = ex;

				_logger.warn("Problems (merging into previous ResultSetTableModel) when getting SQL by name = '"+name+"': " + ex);
				
				throw new SQLException("Problems (merging into previous ResultSetTableModel) when getting SQL by name = '"+name+"': " + ex, ex);
			} 
		}
		
		// Replace the XML Showplan with JUST the SQL
//		if (planRstm != null)
//		{
//			int pos_SQLText = planRstm.findColumn("SQLText");
//			if (pos_SQLText >= 0)
//			{
//				for (int r=0; r<planRstm.getRowCount(); r++)
//				{
//					String SQLText = planRstm.getValueAsString(r, pos_SQLText);
//					if (StringUtil.hasValue(SQLText))
//					{
//						int startPos = SQLText.indexOf("<![CDATA[");
//						int endPos   = SQLText.indexOf("]]>");
//
//						if (startPos >= 0 && endPos >= 0)
//						{
//							startPos += "<![CDATA[".length();
//							endPos   -= "]]>"      .length();
//							
//							String newSQLText = SQLText.substring(startPos, endPos).trim();
//							
//							if (newSQLText.startsWith("SQL Text:"))
//								newSQLText = newSQLText.substring("SQL Text:".length()).trim();
//
//							// make it a bit more HTML like
//							newSQLText = "<xmp>" + newSQLText + "</xmp>";
//
//							// Finally SET the SQL Text
//							planRstm.setValueAtWithOverride(newSQLText, r, pos_SQLText);
//						}
//					}
//				}
//			}
//		}
		
		return planRstm;
	}

	public Map<String, String> getShowplanAsMapFromMonDdlStorage(DbxConnection conn, Set<String> nameSet)
	throws SQLException
	{
		Map<String, String> planMap = new LinkedHashMap<>();
		
		for (String name : nameSet)
		{
			String sql = ""
				    + "select [objectName], [extraInfoText] as [SQLText] \n"
				    + "from [MonDdlStorage] \n"
				    + "where 1 = 1 \n"
				    + "  and [dbname]     = 'statement_cache' \n"
//				    + "  and [owner]      = 'ssql' \n"
				    + "  and [objectName] = " + DbUtils.safeStr(name) + " \n"
				    + "";
			
			sql = conn.quotifySqlString(sql);
			try ( Statement stmnt = conn.createStatement() )
			{
				// Unlimited execution time
				stmnt.setQueryTimeout(0);
				try ( ResultSet rs = stmnt.executeQuery(sql) )
				{
					while(rs.next())
					{
						String objectName    = rs.getString(1);
						String extraInfoText = rs.getString(2);
						
						planMap.put(objectName, extraInfoText);
					}
				}
			}
			catch(SQLException ex)
			{
				//_problem = ex;

				_logger.warn("Problems getting SQL Statement name = '"+name+"': " + ex);
				throw ex;
			} 
		}
		
		return planMap;
	}
}

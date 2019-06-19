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
package com.asetune.pcs.report.content.ase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.asetune.gui.ModelMissmatchException;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ReportEntryAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;

public abstract class AseAbstract
extends ReportEntryAbstract
{
	private static Logger _logger = Logger.getLogger(AseAbstract.class);

	protected int _statement_gt_execTime      = -1;
	protected int _statement_gt_logicalReads  = -1;
	protected int _statement_gt_physicalReads = -1;


	public AseAbstract(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	protected void getSlowQueryThresholds(DbxConnection conn)
	{
		String tabName = "MonSessionParams";
		String sql = ""
			    + "select [Type], [ParamName], [ParamValue] \n"
			    + "from ["+tabName+"] \n"
			    + "where [ParamName] in("
			    		+ "'"    + PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_execTime
			    		+ "', '" + PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_logicalReads
			    		+ "', '" + PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_physicalReads
			    		+ "') \n"
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
//					String type = rs.getString(1);
					String name = rs.getString(2);
					String val  = rs.getString(3);
					
					if (PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_execTime     .equals(name)) _statement_gt_execTime      = StringUtil.parseInt(val, -1);
					if (PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_logicalReads .equals(name)) _statement_gt_logicalReads  = StringUtil.parseInt(val, -1);
					if (PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_physicalReads.equals(name)) _statement_gt_physicalReads = StringUtil.parseInt(val, -1);
				}
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting values from '"+tabName+"': " + ex);
		}

		Configuration conf = Configuration.getCombinedConfiguration();

		if (_statement_gt_execTime      == -1) _statement_gt_execTime      = conf.getIntProperty(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_execTime,      PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_execTime);
		if (_statement_gt_logicalReads  == -1) _statement_gt_logicalReads  = conf.getIntProperty(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_logicalReads,  PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_logicalReads);
		if (_statement_gt_physicalReads == -1) _statement_gt_physicalReads = conf.getIntProperty(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_physicalReads, PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_physicalReads);
	}
	

	public Set<String> getStatementCacheObjects(ResultSetTableModel rstm, String colName)
	{
		Set<String> set = new LinkedHashSet<>();
		
		int pos_colName = rstm.findColumn(colName);
		if (pos_colName != -1)
		{
			for (int r=0; r<rstm.getRowCount(); r++)
			{
				String name = rstm.getValueAsString(r, pos_colName);
				
				if (name != null && (name.trim().startsWith("*ss") || name.trim().startsWith("*sq")) )
				{
					set.add(name);
				}
			}
		}
		
		return set;
	}

	public ResultSetTableModel getSqlStatementsFromMonDdlStorage(DbxConnection conn, Set<String> nameSet)
	throws SQLException
	{
		ResultSetTableModel ssqlRstm = null;
		
		for (String name : nameSet)
		{
			String sql = ""
				    + "select [objectName], [extraInfoText] as [SQLText] \n"
				    + "from [MonDdlStorage] \n"
				    + "where 1 = 1 \n"
				    + "  and [dbname]     = 'statement_cache' \n"
				    + "  and [owner]      = 'ssql' \n"
				    + "  and [objectName] = " + DbUtils.safeStr(name) + " \n"
				    + "";
			
			sql = conn.quotifySqlString(sql);
			try ( Statement stmnt = conn.createStatement() )
			{
				// Unlimited execution time
				stmnt.setQueryTimeout(0);
				try ( ResultSet rs = stmnt.executeQuery(sql) )
				{
//					ResultSetTableModel rstm = new ResultSetTableModel(rs, "ssqlRstm");
					ResultSetTableModel rstm = createResultSetTableModel(rs, "ssqlRstm", sql);
					
					if (ssqlRstm == null)
						ssqlRstm = rstm;
					else
						ssqlRstm.add(rstm);

					if (_logger.isDebugEnabled())
						_logger.debug("_ssqlRstm.getRowCount()="+ rstm.getRowCount());
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
		if (ssqlRstm != null)
		{
			int pos_SQLText = ssqlRstm.findColumn("SQLText");
			if (pos_SQLText >= 0)
			{
				for (int r=0; r<ssqlRstm.getRowCount(); r++)
				{
					String SQLText = ssqlRstm.getValueAsString(r, pos_SQLText);
					if (StringUtil.hasValue(SQLText))
					{
						int startPos = SQLText.indexOf("<![CDATA[");
						int endPos   = SQLText.indexOf("]]>");

						if (startPos >= 0 && endPos >= 0)
						{
							startPos += "<![CDATA[".length();
							endPos   -= "]]>"      .length();
							
							String newSQLText = SQLText.substring(startPos, endPos).trim();
							
							if (newSQLText.startsWith("SQL Text:"))
								newSQLText = newSQLText.substring("SQL Text:".length()).trim();

							// make it a bit more HTML like
							newSQLText = "<pre>\n" + newSQLText + "\n</pre>";

							// Finally SET the SQL Text
							ssqlRstm.setValueAtWithOverride(newSQLText, r, pos_SQLText);
						}
					}
				}
			}
		}
		
		return ssqlRstm;
	}
	
}

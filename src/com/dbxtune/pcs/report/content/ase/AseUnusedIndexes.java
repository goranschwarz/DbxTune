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
package com.dbxtune.pcs.report.content.ase;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public class AseUnusedIndexes
extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(AseUnusedIndexes.class);

	private ResultSetTableModel _shortRstm;
	private int _totalUnusedIndexCount;

	public AseUnusedIndexes(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasMinimalMessageText()
	{
		return false;
	}

	@Override
	public boolean hasShortMessageText()
	{
		return false;
	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		sb.append("Total number of unused indexes are: " + _totalUnusedIndexCount + "<br>\n");
		sb.append("<br>\n");
		
		sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
		sb.append(toHtmlTable(_shortRstm));
	}

	@Override
	public String getSubject()
	{
		return "Top Unused Indexes, ordered by 'RowsInsUpdDel' (origin: CmObjectActivity)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmObjectActivity_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
//		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);
		int topRows = getTopRows() * 2;

		String sql = "";

		// get TOTAL unused count
		sql = ""
			    + "select count(*) \n"
			    + "from [CmObjectActivity_abs] \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmObjectActivity_abs]) \n"
			    + "  and [IndexID]      > 0 \n"
//			    + "  and [OptSelectCount] = 0 \n"
			    + "  and [UsedCount]      = 0 \n"
//			    + "  and [Operations]     = 0 \n"
			    + "";

		sql = conn.quotifySqlString(sql);
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				_totalUnusedIndexCount = rs.getInt(1);
		}
		catch(SQLException ignore) {}
		
		// get TOP # unused indexes
		sql = ""
			    + "select top " + topRows + " \n"
			    + "     [DBName] \n"
			    + "    ,[ObjectName] \n"
			    + "    ,NULL as [SchemaName] \n"
			    + "    ,[IndexName] \n"
			    + "    ,[IndexID] \n"
			    + "    ,NULL as [CreateDate] \n"
			    + "    ,[RowsInsUpdDel] \n"
			    + "    ,[OptSelectCount] \n"
			    + "    ,[UsedCount] \n"
			    + "    ,[Operations] \n"
			    + " \n"
			    + "    ,[UsageInMb]        as [IndexSizeMB] -- if: -1 get from DDL Store \n"
			    + "    ,[TabRowCount]      as [TabRowCount] -- if: -1 get from DDL Store \n"
			    + "    ,cast(-1 as bigint) as [TableSizeMB] \n"
//			    + "    ,'DROP INDEX [' || [ObjectName] || '].[' || [IndexName] || ']' as [DropStatement] \n"
			    + "    ,'' as [DropStatement] \n"
			    + " \n"
			    + "from [CmObjectActivity_abs] \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmObjectActivity_abs]) \n"
			    + "  and [IndexID]        > 0 \n"
//			    + "  and [OptSelectCount] = 0 \n"
			    + "  and [UsedCount]      = 0 \n"
//			    + "  and [Operations]     = 0 \n"
			    + "order by [RowsInsUpdDel] desc \n"
			    + "";
		
		_shortRstm = executeQuery(conn, sql, true, "CmObjectActivity_abs");

		// Go and get some information from the DDL Storage
		if ( ! _shortRstm.isEmpty() )
		{
			int pos_SchemaName    = _shortRstm.findColumn("SchemaName");
			int pos_CreateDate    = _shortRstm.findColumn("CreateDate");
			int pos_IndexSizeMB   = _shortRstm.findColumn("IndexSizeMB");
			int pos_TabRowCount   = _shortRstm.findColumn("TabRowCount");
			int pos_TableSizeMB   = _shortRstm.findColumn("TableSizeMB");
			int pos_DropStatement = _shortRstm.findColumn("DropStatement");
			
			for (int r=0; r<_shortRstm.getRowCount(); r++)
			{
				String DBName     = _shortRstm.getValueAsString(r, "DBName");
				String ObjectName = _shortRstm.getValueAsString(r, "ObjectName");
				String IndexName  = _shortRstm.getValueAsString(r, "IndexName");

				String problem = "";
				Set<AseTableInfo> aseTableInfoSet = null;
				try	{ aseTableInfoSet = getTableInformationFromMonDdlStorage(conn, DBName, null, ObjectName); }
				catch (SQLException ex) { problem = "ERROR: Mon DDL Storage lookup, Msg: " + ex.getMessage(); }

				if (aseTableInfoSet != null && !aseTableInfoSet.isEmpty())
				{
					AseTableInfo aseTableInfo = aseTableInfoSet.iterator().next(); // Get first entry from iterator (simulating: list.get(0))

					String dropStmnt = "DROP INDEX [" + ObjectName + "].[" + IndexName + "]";
					
					// Set: CreateDate, SchemaName, TabRowCount, TableSizeMB
					_shortRstm.setValueAtWithOverride(aseTableInfo.getSchemaName(), r, pos_SchemaName);
					_shortRstm.setValueAtWithOverride(aseTableInfo.getRowTotal()  , r, pos_TabRowCount);
					_shortRstm.setValueAtWithOverride(aseTableInfo.getSizeMb()    , r, pos_TableSizeMB);
					_shortRstm.setValueAtWithOverride(dropStmnt                   , r, pos_DropStatement);

					// Set: IndexSizeMB
					AseIndexInfo indexInfo = aseTableInfo.getIndexInfoForName(IndexName);
					if (indexInfo != null)
					{
						_shortRstm.setValueAtWithOverride(indexInfo.getCreationDate(), r, pos_CreateDate);
						_shortRstm.setValueAtWithOverride(indexInfo.getSizeMb()      , r, pos_IndexSizeMB);
					}
				}
				else
				{
					if (StringUtil.isNullOrBlank(problem))
						problem = "INFO: Object wasn't found in DDL Storage";
					
					_shortRstm.setValueAtWithOverride(problem, r, pos_DropStatement);
				}
			}
		}
		
		// Highlight sort column
		_shortRstm.setHighlightSortColumns("RowsInsUpdDel");

		// Describe the table
		setSectionDescription(_shortRstm);
	}

	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"Information from last collector sample from the table <code>CmObjectActivity_abs</code><br>" +
				"");
	}
}


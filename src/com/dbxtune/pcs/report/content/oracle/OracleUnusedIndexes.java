/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune.pcs.report.content.oracle;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.Set;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IndexAnalysisHtml;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class OracleUnusedIndexes
extends OracleAbstract
{
	private ResultSetTableModel _shortRstm; // null = index usage was not recorded
	private ResultSetTableModel _sumRstm;
	private int _minDays;

	public OracleUnusedIndexes(DailySummaryReportAbstract reportingInstance)
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
		return true;
	}

	@Override
	public void writeMessageText(Writer w, MessageType messageType)
	throws IOException
	{
		if (_shortRstm == null)
		{
			w.append("<b>Index usage was not recorded.</b><br>\n");
			w.append("Oracle tracks how the indexes are used from version 12.2 (the view DBA_INDEX_USAGE), so for older versions there is nothing to show.<br>\n");
			return;
		}

		if (_shortRstm.getRowCount() == 0)
		{
			w.append("<b>No unused indexes was found.</b><br>\n");
			w.append("All the indexes that the collector recorded (the biggest ones, not the ones in the schemas that comes with Oracle) has been used the last 30 days.<br>\n");
			return;
		}

		// Get a description of this section, and column names
		w.append(getSectionDescriptionHtml(_shortRstm, true));

		// How many, and how much space
		w.append(toHtmlTable(_sumRstm));
		w.append("<br>\n");

		w.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
		w.append(_shortRstm.toHtmlTableString("sortable", true, true, null, new ReportEntryTableStringRenderer()
		{
			@Override
			public String tagTrAttr(ResultSetTableModel rstm, int row)
			{
				// Gray: too new to trust 'unused'
				Object validDays = rstm.getValueAsObject(row, "valid_days");
				if ( ! (validDays instanceof Number) || ((Number)validDays).doubleValue() < _minDays )
					return "style='color:gray;'";
				return super.tagTrAttr(rstm, row);
			}
		}));
	}

	@Override
	public String getSubject()
	{
		return "Indexes that are Not Used (order by: SIZE_MB, origin: CmIndexUsage / DBA_INDEX_USAGE)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// No mandatory table: below Oracle 12.2 the CM is not active (and that is something the reader should be told), not a red ERROR
		if ( ! doTableExist(conn, getReportingInstance().getDbmsSchemaName(), "CmIndexUsage_abs") )
			return;

		int topRows = getTopRows();
		_minDays = Configuration.getCombinedConfiguration().getIntProperty(IndexAnalysisHtml.PROPKEY_unused_minDays, IndexAnalysisHtml.DEFAULT_unused_minDays);

		// The indexes at the last sample, that has not been used the last 30 days (or never)
		String sql_where = ""
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmIndexUsage_abs] where 1 = 1 " + getReportPeriodSqlWhere().trim() + ") \n"
			    + "  and ([LAST_USED] is null or [DAYS_SINCE_USED] > 30) \n"
			    + "";

		// DBA_INDEX_USAGE is saved (kept over a restart): not used since LAST_USED, or since the index was created if it was never seen used
		String validSinceExpr = "coalesce([LAST_USED], [CREATED])";
		String validDaysExpr  = "cast(datediff(minute, " + validSinceExpr + ", [SessionSampleTime]) / 1440.0 as numeric(8,1))";
		String trustedExpr    = validDaysExpr + " >= " + _minDays;

		// The biggest first
		String sql = ""
			    + "select top " + topRows + " \n"
			    + "     [CON_NAME] \n"
			    + "    ,[OWNER] \n"
			    + "    ,[INDEX_NAME] \n"
			    + "    ,[TABLE_NAME] \n"
			    + "    ,[UNIQUENESS] \n"
			    + "    ,[INDEX_TYPE] \n"
			    + "    ,[STATUS] \n"
			    + "    ,[VISIBILITY] \n"
			    + "    ,[SIZE_MB] \n"
			    + "    ,[LAST_USED] \n"
			    + "    ,[DAYS_SINCE_USED] \n"
			    + "    ,[CREATED] \n"
			    + "    ," + validSinceExpr + " as [valid_since] \n"
			    + "    ," + validDaysExpr  + " as [valid_days] \n"
			    + "    ,cast('' as varchar(4000)) as [DropStatement] \n"
			    + "    ,cast('' as varchar(4000)) as [CreateStatement] \n" // the ROLLBACK of the drop
			    + "from [CmIndexUsage_abs] \n"
			    + sql_where
			    + "order by case when " + trustedExpr + " then 0 else 1 end, [SIZE_MB] desc, [OWNER], [INDEX_NAME] \n" // the ones that can be trusted first
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "OracleUnusedIndexes");

		// DropStatement and CreateStatement (the ROLLBACK of the drop): the CREATE INDEX is built from the DDL Storage, like the Index Analysis rollback
		int pos_DropStatement   = _shortRstm.findColumn("DropStatement");
		int pos_CreateStatement = _shortRstm.findColumn("CreateStatement");
		for (int r = 0; r < _shortRstm.getRowCount(); r++)
		{
			String conName   = _shortRstm.getValueAsString(r, "CON_NAME");
			String owner     = _shortRstm.getValueAsString(r, "OWNER");
			String indexName = _shortRstm.getValueAsString(r, "INDEX_NAME");
			String tableName = _shortRstm.getValueAsString(r, "TABLE_NAME");
			String qIndex    = "\"" + owner + "\".\"" + indexName + "\"";
			String getDdl    = "SELECT DBMS_METADATA.GET_DDL('INDEX', '" + indexName.replace("'", "''") + "', '" + owner.replace("'", "''") + "') FROM dual;";

			Set<String> tableList = new LinkedHashSet<>();
			tableList.add(owner + "." + tableName);
			Set<OraTableInfo> tiSet = getTableInformationFromMonDdlStorage(conn, "(NULL)".equals(conName) ? null : conName, tableList);
			OraTableInfo ti = tiSet.isEmpty() ? null : tiSet.iterator().next();

			// The index of a primary key or a unique constraint: DROP INDEX gives ORA-02429, the constraint is what to drop
			String constraintType = null;
			if (ti != null)
				for (OraIndexInfo ii : ti.indexes)
					if (indexName.equalsIgnoreCase(ii.name) && owner.equalsIgnoreCase(ii.owner))
						constraintType = ii.constraintType;

			String dropStmnt;
			String createStmnt;
			if ("P".equals(constraintType) || "U".equals(constraintType))
			{
				dropStmnt   = "-- The index of a " + ("P".equals(constraintType) ? "primary key" : "unique constraint") + ": drop the constraint (ALTER TABLE \"" + (ti == null ? owner : ti.owner) + "\".\"" + tableName + "\" DROP CONSTRAINT ...), not the index";
				createStmnt = "-- Save it first: SELECT DBMS_METADATA.GET_DEPENDENT_DDL('CONSTRAINT', '" + tableName.replace("'", "''") + "', '" + (ti == null ? owner : ti.owner).replace("'", "''") + "') FROM dual;";
			}
			else
			{
				dropStmnt   = "DROP INDEX " + qIndex + ";";
				createStmnt = getIndexRestoreDdl(ti, owner, indexName);
				createStmnt = createStmnt == null
						? "-- " + (ti == null ? "Not in the DDL Storage" : "Can't be written from the DDL Storage (partitioned, domain...)") + ": save it first with " + getDdl
						: createStmnt + ";";
			}
			_shortRstm.setValueAtWithOverride(dropStmnt  , r, pos_DropStatement);
			_shortRstm.setValueAtWithOverride(createStmnt, r, pos_CreateStatement);
		}

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("SIZE_MB");

		_shortRstm.setDescription(
				"Indexes that has not been used the last 30 days, or never, the biggest first (from the last sample in the table <code>CmIndexUsage_abs</code>).<br>" +
				"Oracle saves the index usage (<code>DBA_INDEX_USAGE</code>, written every 15 minutes), so it is kept over a restart. " +
				"<code>valid_since</code> is when the index was used last, or when it was created if it was never seen used. " +
				"An index that was created less than " + _minDays + " days ago (property <code>" + IndexAnalysisHtml.PROPKEY_unused_minDays + "</code>) is too new to trust: it is last, in gray.<br>" +
				"An index that is not used still costs: every INSERT, UPDATE and DELETE has to maintain it, and it takes space.<br>" +
				"<b>Before you drop one</b>:" +
				"<ul>" +
				"  <li><code>LAST_USED</code> is empty = <i>not seen used since Oracle started to track it</i> (when the index was created, or when the database was upgraded to 12.2 if that is later: so <code>valid_days</code> can be too high for an old index). Think of what only runs at month end or year end.</li>" +
				"  <li>By default Oracle only looks at some of the executions (it samples), so an index that is used very seldom can be missed.</li>" +
				"  <li>A <code>UNIQUE</code> index is there to stop duplicates, and it does that also when no query uses it.</li>" +
				"  <li>An index on a foreign key can be needed even if no query uses it: without it the child table is locked and scanned when rows in the parent table are deleted.</li>" +
				"  <li>Make it <code>INVISIBLE</code> first, and drop it later: <code>ALTER INDEX ... INVISIBLE</code></li>" +
				"  <li>Save the <code>CreateStatement</code> first (the rollback, from the DDL Storage), then run the <code>DropStatement</code>. In a Container Database: connected to the PDB in <code>CON_NAME</code>. " +
				"      The CREATE INDEX has the keys, the tablespace and the compression, not the storage settings: <code>DBMS_METADATA.GET_DDL</code> gives all of it.</li>" +
				"</ul>" +
				"<b>Note</b>: The collector only records the biggest indexes (property <code>CmIndexUsage.sample.topRows</code>), and not the ones in the schemas that comes with Oracle.<br>" +
				"");

		_shortRstm.setColumnDescription("CON_NAME"       , "Container (PDB) name. Empty when the database is not a Container Database, or when the collector is connected to a PDB.");
		_shortRstm.setColumnDescription("OWNER"          , "The schema that owns the index");
		_shortRstm.setColumnDescription("INDEX_NAME"     , "Name of the index");
		_shortRstm.setColumnDescription("TABLE_NAME"     , "The table that the index is on");
		_shortRstm.setColumnDescription("UNIQUENESS"     , "UNIQUE or NONUNIQUE. A UNIQUE index stops duplicates (a primary key or a unique constraint): do not drop it just because no query uses it.");
		_shortRstm.setColumnDescription("INDEX_TYPE"     , "NORMAL, BITMAP, FUNCTION-BASED NORMAL, IOT - TOP, DOMAIN...");
		_shortRstm.setColumnDescription("STATUS"         , "VALID, UNUSABLE or N/A (a partitioned index)");
		_shortRstm.setColumnDescription("VISIBILITY"     , "VISIBLE or INVISIBLE. An INVISIBLE index is maintained, but the optimizer does not use it.");
		_shortRstm.setColumnDescription("SIZE_MB"        , "Space allocated to the index (all it's partitions), in MB");
		_shortRstm.setColumnDescription("LAST_USED"      , "When the index was used last. Empty = not seen used since Oracle started to track it.");
		_shortRstm.setColumnDescription("DAYS_SINCE_USED", "Number of days since LAST_USED, counted at the last collector sample");
		_shortRstm.setColumnDescription("CREATED"        , "When the index was created");
		_shortRstm.setColumnDescription("DropStatement"  , "The DROP INDEX (for the index of a primary key or unique constraint: drop the constraint). Save the CreateStatement first: it is the only way back if the index was needed");
		_shortRstm.setColumnDescription("CreateStatement", "The CREATE INDEX that gives the index back (the rollback of the DROP), from the DDL Storage like the Index Analysis rollback. When it can't be built: the DBMS_METADATA call to save it with");
		_shortRstm.setColumnDescription("valid_since"    ,"Not used since: LAST_USED, or CREATED if it was never seen used");
		_shortRstm.setColumnDescription("valid_days"     , "Days without use (valid_since to the last sample). Under " + _minDays + " days (property " + IndexAnalysisHtml.PROPKEY_unused_minDays + "): too new to trust, shown in gray");

		// How many are there, and how much space do they take (all of them, not just the top ##)
		sql = ""
			    + "select \n"
			    + "     count(*)                                                    as [unused_indexes] \n"
			    + "    ,sum([SIZE_MB])                                              as [unused_size_mb] \n"
			    + "    ,sum(case when [UNIQUENESS] = 'UNIQUE' then 1 else 0 end)    as [of_which_unique] \n"
			    + "    ,sum(case when " + trustedExpr + " then 1 else 0 end) as [trusted_unused] \n"
			    + "    ,(select count(*) from [CmIndexUsage_abs] where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmIndexUsage_abs] where 1 = 1 " + getReportPeriodSqlWhere().trim() + ")) as [recorded_indexes] \n"
			    + "from [CmIndexUsage_abs] \n"
			    + sql_where
			    + "";

		_sumRstm = executeQuery(conn, sql, true, "OracleUnusedIndexesSum");

		_sumRstm.setColumnDescription("unused_indexes"  , "Number of indexes that has not been used the last 30 days, or never");
		_sumRstm.setColumnDescription("unused_size_mb"  , "The space they take, in MB");
		_sumRstm.setColumnDescription("of_which_unique" , "How many of them that are UNIQUE (they are there to stop duplicates)");
		_sumRstm.setColumnDescription("trusted_unused"  , "How many of them have not been used for at least " + _minDays + " days (property " + IndexAnalysisHtml.PROPKEY_unused_minDays + "): the ones you can trust");
		_sumRstm.setColumnDescription("recorded_indexes", "Number of indexes that the collector recorded at the last sample");
	}
}

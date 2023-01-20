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
package com.asetune.utils;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.asetune.cache.DbmsObjectIdCache;
import com.asetune.cache.DbmsObjectIdCache.ObjectInfo;
import com.asetune.sql.conn.DbxConnection;

public class PostgresUtils
{
	private static Logger _logger = Logger.getLogger(PostgresUtils.class);

	public static Timestamp getStartDate(DbxConnection conn)
	throws SQLException
	{
		String sql = "SELECT pg_postmaster_start_time()";

		Timestamp ts = null;
		try ( PreparedStatement stmnt = conn.prepareStatement(sql) )
		{
			stmnt.setQueryTimeout(10);
			try ( ResultSet rs = stmnt.executeQuery() )
			{
				while (rs.next())
				{
					ts = rs.getTimestamp(1);
				}
			}
		}
		return ts;
	}


	/**
	 * Get a lock summary for a SPID
	 * 
	 * @param conn           The connection to use 
	 * @param spid           The SPID we want to get locks for
	 * @return List&lt;LockRecord&gt; never null
	 * @throws TimeoutException 
	 */
	public static List<PgLockRecord> getLockSummaryForPid(DbxConnection conn, int pid) 
	throws TimeoutException
	{
//		String sql = ""
//			    + "select \n"
//			    + "     spid       = req_spid \n"
//			    + "    ,dbid       = rsc_dbid \n"
//			    + "    ,objectid   = rsc_objid \n"
//			    + "    ,indexId    = rsc_indid \n"
//			    + "    ,rsc_type \n" // Resolved to a String on client side
//			    + "    ,req_mode \n" // Resolved to a String on client side
//			    + "    ,req_status \n" // Resolved to a String on client side
//			    + "    ,lockCount  = count(*) \n"
//			    + "from master.dbo.syslockinfo WITH (READUNCOMMITTED) \n"
//			    + "where rsc_type != 2 -- DB \n"
//			    + "  and (req_spid = ? OR req_status = 3) -- req_status=3 is 'WAIT' \n"
//			    + "group by req_spid, rsc_dbid, rsc_objid, rsc_indid, rsc_type, req_mode, req_status \n"
//			    + "";
		long pgVersion = conn.getDbmsVersionInfo().getLongVersion();

		String waitstart_col        = ""; 
		String waitstart_in_sec_col = ""; 
		if (pgVersion >= Ver.ver(14))
		{
			waitstart_col        = "    ,max(waitstart) as max_waitstart \n";
			waitstart_in_sec_col = "    ,CAST( MAX( EXTRACT('epoch' FROM clock_timestamp()) - EXTRACT('epoch' FROM waitstart) ) as numeric(12,1) ) AS max_wait_in_sec \n";
		}
		
		// FIXME: Possibly SKIP 'lock_count' (and group by... return ALL rows: Postgres do NOT hold 1 entry in pg_locks for each row/table it locks)... think this over, and make it better...
		String sql = "/* PostgresTune:getLockSummaryForPid() */"
			    + "select \n"
			    + "     pid \n"
			    + "    ,database \n"
			    + "    ,relation \n"
			    + "    ,granted \n"
			    + "    ,max(transactionid::text) AS transactionid \n"
			    + "    ,max(locktype)      AS locktype \n"
			    + "    ,max(mode)          AS mode \n"
			    + "    ,count(*)           AS lock_count \n"
			    + waitstart_col
			    + waitstart_in_sec_col
			    + "from pg_locks \n"
			    + "where 1 = 1 \n"
//			    + "  and database is not null \n"
//			    + "  and pid = ? \n"
			    + "  and (pid = ? OR not granted)\n"
			    + "group by pid, database, relation, granted, locktype, mode \n"
			    + "";

		List<PgLockRecord> lockList = new ArrayList<>();
		List<PgLockRecord> otherSpidsWaiting = null;

		long execStartTime = System.currentTimeMillis();

		try (PreparedStatement pstmnt = conn.prepareStatement(sql)) // Auto CLOSE
		{
			// Timeout after 1 second --- if we get blocked when doing: object_name()
			pstmnt.setQueryTimeout(1);
			
			// set SPID
			pstmnt.setInt(1, pid);
			
			try (ResultSet rs = pstmnt.executeQuery()) // Auto CLOSE
			{
				while(rs.next())
				{
					long       PID        = rs.getLong     (1);
					long       dbid       = rs.getLong     (2);
					long       objectid   = rs.getLong     (3);
					boolean    granted    = rs.getBoolean  (4);
					String     xactId     = rs.getString   (5);
					String     lockType   = rs.getString   (6);
					String     lockMode   = rs.getString   (7);
					int        lockCount  = rs.getInt      (8);
					Timestamp  waitstart  = StringUtil.hasValue(waitstart_col) ? rs.getTimestamp (9)  : null;
					BigDecimal waitInSec  = StringUtil.hasValue(waitstart_col) ? rs.getBigDecimal(10) : null;

					// If this SPID is same as we are checking for... Add it to 'lockList' otherwise add it to 'otherSpidsWaiting'
					if (pid == PID)
					{
						lockList.add(          new PgLockRecord(PID, dbid, objectid, granted, xactId, lockType, lockMode, waitstart, waitInSec, lockCount) );
					}
					else
					{
						if (otherSpidsWaiting == null)
							otherSpidsWaiting = new ArrayList<>();

						otherSpidsWaiting.add( new PgLockRecord(PID, dbid, objectid, granted, xactId, lockType, lockMode, waitstart, waitInSec, lockCount) );
					}
				}
			}
			
			// Check/Set for SPID's that are BLOCKING *this* PID 
			// FIXME: This is not REALLY working for Postgres (PG do not hold 1 entry in pg_locks for each row/table it locks)... think this over... 
			if (otherSpidsWaiting != null)
			{
				for (PgLockRecord lr : lockList)
				{
					// if the LockRecord is already in lockStatus 'WAIT'... (not granted) then it can't be a "root cause locker"... so get next row
					if ( ! lr._lockGranted )
						continue;

					for (PgLockRecord wlr : otherSpidsWaiting)
					{
						// only check for 'WAIT' (lookStatus == 3) ... this is not needed since it'a already taken care of in the SQL WHERE Clause
//						if (wlr._lockStatus != 3)
//							continue;
						if (wlr._lockGranted)
							continue;

						// If: dbid && objectid && lockType are matching --> isBlocking
//						if (lr._dbid == wlr._dbid && lr._objectid == wlr._objectid)
						if (lr._xactId != null && wlr._xactId != null && lr._xactId.equals(wlr._xactId))
						{
							// We have a BLOCKING PID
							if (lr._blockingPids == null)
								lr._blockingPids = new ArrayList<>();

							lr._blockingPids.add(wlr._pid);

							// Get Max wait time
							lr._blockedPidsMaxWaitInSec = MathUtils.max(lr._blockedPidsMaxWaitInSec, wlr._lockWaitInSec);
						}
					}
				}
			}
		}
		catch (SQLException ex)
		{
			long execTime = TimeUtils.msDiffNow(execStartTime);

			if (ex.getMessage() != null && ex.getMessage().contains("query has timed out"))
			{
				_logger.warn("getLockSummaryForPid: Problems getting Lock List (from pg_locks). The query has timed out after execTimeInMs=" + execTime + ".");
				throw new TimeoutException();
			}
			else
			{
				_logger.warn("Problems when executing sql: " + sql + ". SQLException Error=" + ex.getErrorCode() + ", Msg='" + StringUtil.stripNewLine(ex.getMessage()) + "'.", ex);
			}
		}

		// Lookup ID's to Names (using a CACHED way... keeping already looked-up id's in memory)
		if (DbmsObjectIdCache.hasInstance())
		{
			DbmsObjectIdCache objIdCache = DbmsObjectIdCache.getInstance();

			for (PgLockRecord r : lockList)
			{
				// When ObjectID is 0... just get the DBName 
				if (r._objectid == 0)
				{
					r._dbname = objIdCache.getDBName(r._dbid);
					continue;
				}

				ObjectInfo oi = objIdCache.getByObjectId(r._dbid, r._objectid);
				if (oi != null)
				{
					r._dbname        = oi.getDBName();
					r._schemaName    = oi.getSchemaName();
					r._tableName     = oi.getObjectName();
					r._objectTypeStr = oi.getObjectTypeStr();
//					r._indexName     = oi.getIndexName(r._indexId);

					if (r._dbname     == null) r._dbname     = "";
					if (r._schemaName == null) r._schemaName = "";
					if (r._tableName  == null) r._tableName  = "";
//					if (r._indexName  == null) r._indexName  = "";
				}
				else
				{
					// At least try to get the 'dbname'
					r._dbname = objIdCache.getDBName(r._dbid);
				}
			}
		}
		
		return lockList;
	}

	private static String toString(Object obj)
	{
		if (obj == null)
			return "";
		return obj.toString();
	}

	/** 
	 * @return "" if no locks, otherwise a HTML TABLE, with the headers: DB, Table, Type, Count
	 */
	public static String getLockListTableAsHtmlTable(List<PgLockRecord> list)
	{
		if (list.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder("<TABLE BORDER=1>");
		sb.append("<TR>");
		sb.append(" <TH>").append("pid"          ).append("</TH>");
		sb.append(" <TH>").append("dbid"         ).append("</TH>");
		sb.append(" <TH>").append("dbname"       ).append("</TH>");
		sb.append(" <TH>").append("ObjectType"   ).append("</TH>");
		sb.append(" <TH>").append("ObjectID"     ).append("</TH>");
		sb.append(" <TH>").append("SchemaName"   ).append("</TH>");
		sb.append(" <TH>").append("TableName"    ).append("</TH>");
		sb.append(" <TH>").append("XactID"       ).append("</TH>");
		sb.append(" <TH>").append("LockGranted"  ).append("</TH>");
		sb.append(" <TH>").append("LockType"     ).append("</TH>");
		sb.append(" <TH>").append("LockMode"     ).append("</TH>");
		sb.append(" <TH>").append("LockStartWait").append("</TH>");
		sb.append(" <TH>").append("LockWaitInSec").append("</TH>");
		sb.append(" <TH>").append("LockCount"    ).append("</TH>");
		sb.append("</TR>");
		for (PgLockRecord lr : list)
		{
			String color = null;

			// blue == has Exclusive locks
			if ( !StringUtil.containsAny(lr._lockType, "virtualxid") && StringUtil.containsAny(lr._lockMode, "Exclusive"))
				color = "blue";

			// Some other colors that "overrides" above
			if      (lr._blockingPids  != null          ) color = "red";     // BLOCKING
			else if (lr._lockWaitStart != null          ) color = "orange";  // WAIT
			else if (lr._lockGranted  == false          ) color = "orange";  // WAIT
			else if ("pg_catalog".equals(lr._schemaName)) color = "gray";    // Postgres system schema object

			if (color == null)
				sb.append("<TR>");
			else
				sb.append("<TR style='color: " + color + ";'>");
			
			sb.append("<TD>").append(toString( lr._pid           )).append("</TD>");
			sb.append("<TD>").append(toString( lr._dbid          )).append("</TD>");
			sb.append("<TD>").append(toString( lr._dbname        )).append("</TD>");
			sb.append("<TD>").append(toString( lr._objectTypeStr )).append("</TD>");
			sb.append("<TD>").append(toString( lr._objectid      )).append("</TD>");
			sb.append("<TD>").append(toString( lr._schemaName    )).append("</TD>");
			sb.append("<TD>").append(toString( lr._tableName     )).append("</TD>");
			sb.append("<TD>").append(toString( lr._xactId        )).append("</TD>");
			sb.append("<TD>").append(toString( lr._lockGranted   )).append("</TD>");
			sb.append("<TD>").append(toString( lr._lockType      )).append("</TD>");
			sb.append("<TD>").append(toString( lr._lockMode      )).append("</TD>");
			sb.append("<TD>").append(toString( lr._lockWaitStart )).append("</TD>");
			sb.append("<TD>").append(toString( lr._lockWaitInSec )).append("</TD>");
			sb.append("<TD>").append(toString( lr._lockCount     )).append("</TD>");
			sb.append("</TR>");
		}
		sb.append("</TABLE>");
		return sb.toString();
	}

	/** 
	 * @return "" if no locks, otherwise a ASCII TABLE, with the headers: DBName, TableName, LockType, LockCount
	 */
	public static String getLockListTableAsAsciiTable(List<PgLockRecord> list)
	{
		if (list.isEmpty())
			return "";

		// Table HEAD
		String[] tHead = new String[] {
				"pid", 
				"dbid", 
				"dbname", 
				"ObjectType", 
				"ObjectID", 
				"SchemaName", 
				"TableName", 
				"XactID", 
				"LockGranted", 
				"LockType", 
				"LockMode", 
				"LockStartWait", 
				"LockWaitInSec", 
				"LockCount"};

		// Table DATA
		List<List<Object>> tData = new ArrayList<>();
		for (PgLockRecord lr : list)
		{
			List<Object> row = new ArrayList<>();
			
			row.add(lr._pid          );
			row.add(lr._dbid         );
			row.add(lr._dbname       );
			row.add(lr._objectTypeStr);
			row.add(lr._objectid     );
			row.add(lr._schemaName   );
			row.add(lr._tableName    );
			row.add(lr._xactId       );
			row.add(lr._lockGranted  );
			row.add(lr._lockType     );
			row.add(lr._lockMode     );
			row.add(lr._lockWaitStart);
			row.add(lr._lockWaitInSec);
			row.add(lr._lockCount    );
			
			tData.add(row);
		}

		return StringUtil.toTableString(Arrays.asList(tHead), tData);
	}

	/**
	 * Get a lock summary for a SPID
	 * 
	 * @param lockList       The lockList produced by: getLockSummaryForSpid(DbxConnection conn, int spid)
	 * @param asHtml         Produce a HTML table (if false a ASCII table will be produced)
	 * @param htmlBeginEnd   (if asHtml=true) should we wrap the HTML with begin/end tags
	 * @return
	 */
	public static String getLockSummaryForPid(List<PgLockRecord> lockList, boolean asHtml, boolean htmlBeginEnd)
	{
		if (lockList.isEmpty())
			return null;
	
		if (asHtml)
		{
			String htmlTable = getLockListTableAsHtmlTable(lockList);
			if (htmlBeginEnd)
				return "<html>" + htmlTable + "</html>";
			else
				return htmlTable;
		}
		else
			return getLockListTableAsAsciiTable(lockList);
	}

	/**
	 * Get a lock summary for a SPID
	 * 
	 * @param conn           The connection to use 
	 * @param spid           The SPID we want to get locks for
	 * @param asHtml         Produce a HTML table (if false a ASCII table will be produced)
	 * @param htmlBeginEnd   (if asHtml=true) should we wrap the HTML with begin/end tags
	 * @return
	 * @throws TimeoutException 
	 */
	public static String getLockSummaryForSpid(DbxConnection conn, int spid, boolean asHtml, boolean htmlBeginEnd) 
	throws TimeoutException
	{
		List<PgLockRecord> lockList = getLockSummaryForPid(conn, spid);

		return getLockSummaryForPid(lockList, asHtml, htmlBeginEnd);
	}

	/**
	 * LockRecord used by: getLockSummaryForSpid(spid)
	 */
	public static class PgLockRecord
	{
		public long       _pid           = 0;
		public long       _dbid          = 0;
		public String     _dbname        = ""; // Filled in by DbmsObjectIdCache
		public long       _objectid      = 0;
		public String     _schemaName    = ""; // Filled in by DbmsObjectIdCache
		public String     _tableName     = ""; // Filled in by DbmsObjectIdCache
//		public int        _indexId       = 0;
//		public String     _indexName     = "";
		public String     _objectTypeStr = "";
//		public int        _lockType      = -1;
//		public String     _lockTypeStr   = "";
//		public int        _lockMode      = -1;
//		public String     _lockModeStr   = "";
//		public int        _lockStatus    = -1;
//		public String     _lockStatusStr = "";
		public boolean    _lockGranted   = true;
		public String     _xactId        = null;
		public String     _lockType      = null;
		public String     _lockMode      = null;
		public Timestamp  _lockWaitStart = null;
		public BigDecimal _lockWaitInSec = null;
		
		public int     _lockCount     = 0;
		public int     _exLockCount   = 0; // Exclusive Lock Count (for lockType "relation")
//		public boolean _isBlocking    = false; // If the record is BLOCKING Other SPID's (set at a second pass)
		public List<Long> _blockingPids = null;
		public BigDecimal _blockedPidsMaxWaitInSec = null;

		public PgLockRecord(long pid, long dbid, long objectid, boolean lockGranted, String xactId, String lockType, String lockMode, Timestamp lockWaitStart, BigDecimal lockWaitInSec, int lockCount)
		{
			_pid           = pid          ;
			_dbid          = dbid         ;
			_objectid      = objectid     ;
			_lockGranted   = lockGranted  ;
			_xactId        = xactId       ;
			_lockType      = lockType     ;
			_lockMode      = lockMode     ;
			_lockWaitStart = lockWaitStart;
			_lockWaitInSec = lockWaitInSec;
			_lockCount     = lockCount    ;

			if ( !StringUtil.containsAny(_lockType, "virtualxid") && StringUtil.containsAny(_lockMode, "Exclusive"))
			{
				_exLockCount++;
			}
//System.out.println("--------- PgLockRecord(): pid="+pid+", dbid="+dbid+", objectid="+objectid+", lockGranted="+lockGranted+", xactId="+xactId+", lockType='"+lockType+"', lockMode='"+lockMode+"', lockWaitStart='"+lockWaitStart+"', lockWaitInSec="+lockWaitInSec+", lockCount="+lockCount+", _exLockCount="+_exLockCount);
		}
	}
}

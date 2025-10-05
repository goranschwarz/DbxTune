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
package com.dbxtune.tools;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;

import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.norm.NormalizerCompiler;
import com.dbxtune.sql.norm.StatementFixerManager;
import com.dbxtune.sql.norm.StatementNormalizer;
import com.dbxtune.sql.norm.UserDefinedNormalizerManager;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class AseStmntCacheBloatDetect
{
	private PrintStream _ps = System.out;
	private DbxConnection _conn;

	// 
	private Map<String, Map<String, StmntEntry>> _dbStmntMap = new LinkedHashMap<>();
	//          dbname      normHash 

	private boolean _printAvgStat       = Configuration.getCombinedConfiguration().getBooleanProperty("AseStmntCacheBloatDetect.printAvgStat"  , false);
	private boolean _printTotalStat     = Configuration.getCombinedConfiguration().getBooleanProperty("AseStmntCacheBloatDetect.printTotalStat", false);
	private int     _duplicateThreshold = Configuration.getCombinedConfiguration().getIntProperty    ("AseStmntCacheBloatDetect.duplicateThreshold", 10);

	private int _parseErrorCount = 0;

	private static class StmntCacheDetailsEntry
	{
		private int       _useCount;
		private Timestamp _cachedDate;
		private Timestamp _lastUsedDate;
		private int       _maxPlanSizeKB;
		private int       _avgLIO;
		private int       _avgPIO;
		private int       _avgCpuTime;
		private int       _avgElapsedTime;
		private long      _totalLIO;
		private long      _totalPIO;
		private long      _totalCpuTime;
		private long      _totalElapsedTime;
		private String    _sqlText;

//		public StmntCacheDetailsEntry(int useCount, Timestamp cachedDate, Timestamp lastUsedDate, int maxPlanSizeKB, long totalLIO, long totalPIO, long totalCpuTime, long totalElapsedTime, String sqlText)
//		{
//			this._useCount         = useCount;
//			this._cachedDate       = cachedDate;
//			this._lastUsedDate     = lastUsedDate;
//			this._maxPlanSizeKB    = maxPlanSizeKB;
//			this._totalLIO         = totalLIO;         
//			this._totalPIO         = totalPIO;         
//			this._totalCpuTime     = totalCpuTime;     
//			this._totalElapsedTime = totalElapsedTime; 
//			this._sqlText          = sqlText;
//		}

		@Override
		public int hashCode()
		{
			return Objects.hash(_sqlText);
		}

		@Override
		public boolean equals(Object obj)
		{
			if ( this == obj )
				return true;
			if ( obj == null )
				return false;
			if ( getClass() != obj.getClass() )
				return false;
			StmntCacheDetailsEntry other = (StmntCacheDetailsEntry) obj;
			return Objects.equals(_sqlText, other._sqlText);
		}

		public String getLastUsedDateAsString()
		{
			if (_cachedDate == null)
				return "----------null---------";
			//          2018-01-08 09:56:53.716

			return TimeUtils.toString(_lastUsedDate);
		}
		
	}

	private static class StmntEntry
	{
		public StmntEntry(String hash, String normalizedSqlText)
		{
			this._hash = hash;
			this._normalizedSqlText = normalizedSqlText;
		}
		private String _hash;
		private String _normalizedSqlText;
//		private Set<String> _originSqlTextSet = new LinkedHashSet<>();
		private Set<StmntCacheDetailsEntry> _stmntCacheDetailsEntrySet = new LinkedHashSet<>();
		private int _count_sq;
		private int _count_ss;
		private long _sumPlanSizeKB;
		
		public void addOriginalSqlText(String objName, StmntCacheDetailsEntry stmntCacheDetailsEntry)
		{
			if (StringUtil.hasValue(objName))
			{
				if (objName.startsWith("*sq")) _count_sq++;
				if (objName.startsWith("*ss")) _count_ss++;
			}
			_stmntCacheDetailsEntrySet.add(stmntCacheDetailsEntry);
			
			_sumPlanSizeKB += stmntCacheDetailsEntry._maxPlanSizeKB;
		}
	}

	public AseStmntCacheBloatDetect(PrintStream printStream)
	{
		_ps = printStream != null ? printStream : System.out;
	}

	public static void main(String[] args)
	{
		// DBA_1_ASE
//		String url = "jdbc:sybase:Tds:dba-1-ase:5000/master";  // Context

		// PROD_A1_ASE
		String url = "jdbc:sybase:Tds:prod-a1-ase:5000/master";

		String user   = "sa";
		String passwd = "sjhyr564s_Wq26kl73";

		try
		{
			DbxConnection conn = AseStmntCacheBloatDetect.connect(url, user, passwd, System.out);

			AseStmntCacheBloatDetect.doWork(conn, true, System.out, 10);
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
		}
	}

	public static void doWork(DbxConnection conn, boolean closeConnection, PrintStream printStream, int duplicateThreshold)
	throws SQLException
	{
		if (conn == null)
			throw new NullPointerException("Connection cant be null.");

		AseStmntCacheBloatDetect bd = new AseStmntCacheBloatDetect(printStream);
		bd._conn = conn;
		bd._duplicateThreshold = duplicateThreshold;
		
		bd.printReportPrologue();

		bd.populate();
		bd.sort();

		bd.printReport();

		if (closeConnection)
			bd.close();
	}

	private void populate()
	throws SQLException
	{
		String sql = ""
			    + "SELECT \n"
			    + "     object_name(SSQLID, 2) as Name \n"
			    + "    ,DBName \n"
			    + "    ,UseCount \n"
			    + "    ,CachedDate \n"
			    + "    ,LastUsedDate \n"
			    + "    ,MaxPlanSizeKB \n"
			    + "    ,AvgLIO \n"
			    + "    ,AvgPIO \n"
			    + "    ,AvgCpuTime \n"
			    + "    ,AvgElapsedTime \n"
			    + "    ,TotalLIO \n"
			    + "    ,TotalPIO \n"
			    + "    ,TotalCpuTime \n"
			    + "    ,TotalElapsedTime \n"
			    + "    ,show_cached_text(SSQLID) as SQLText \n"
			    + "FROM master.dbo.monCachedStatement \n"
			    + "ORDER BY LastUsedDate DESC \n"
			    + "";

		NormalizerCompiler          .getInstance();
		UserDefinedNormalizerManager.getInstance();
		StatementFixerManager       .getInstance();

		StatementNormalizer.NormalizeParameters normalizeParameters = new StatementNormalizer.NormalizeParameters();

		try (Statement stmnt = _conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				String    objName          = rs.getString   (1);
				String    dbname           = rs.getString   (2);
				int       useCount         = rs.getInt      (3);
				Timestamp cachedDate       = rs.getTimestamp(4);
				Timestamp lastUsedDate     = rs.getTimestamp(5);
				int       maxPlanSizeKB    = rs.getInt      (6);
				int       avgLIO           = rs.getInt      (7);
				int       avgPIO           = rs.getInt      (8);
				int       avgCpuTime       = rs.getInt      (9);
				int       avgElapsedTime   = rs.getInt      (10);
				long      totalLIO         = rs.getLong     (11);
				long      totalPIO         = rs.getLong     (12);
				long      totalCpuTime     = rs.getLong     (13);
				long      totalElapsedTime = rs.getLong     (14);
				String    sqlText          = rs.getString   (15);

				if (sqlText == null)
					continue;

				//----------------------------------------------
				// Cleanup the SQL a bit 
				//----------------------------------------------

				// remove all '\n' 
				sqlText = sqlText.replace('\n', ' ');

				// and potentially extra spaces
				sqlText = sqlText.replaceAll("\\s+", " ");

				// trim
				sqlText = sqlText.trim();

				// Remove Sybase trailing parameter declarations like: 
				//    >>> (@P123 VARCHAR(64) output, @P124 VARCHAR(64) output, ...)
				//    >>> (@@@...)
				if (sqlText.endsWith(")"))
				{
					String before = sqlText;
					String after  = removeLastSybaseXxx(sqlText);
					
					if ( ! before.equals(after) )
					{
//						_ps.println();
//						_ps.println("   ---->>>> |" + before + "|");
//						_ps.println("   <<<<---- |" + after  + "|");
						sqlText = after;
					}
				}

				// Translate special variables @@@ into "normal" variables
				sqlText = sqlText.replace("@@@", "@");

//				String normalizedSqlText = StatementNormalizer.getInstance().normalizeSqlText(sqlText, normalizeParameters, tableList);
				String normalizedSqlText = StatementNormalizer.getInstance().normalizeSqlText(sqlText, null, null);

				if (normalizedSqlText == null)
				{
					_parseErrorCount++;
					_ps.println("   >>> WARNING: Problems parsing, skipping and continuing... sqlText=|" + sqlText + "|.");
					
//					if (_parseErrorCount > 20)
//						return;
					continue;
				}

				String hash = DigestUtils.md5Hex(normalizedSqlText);

				// Get "dbname" entry
				Map<String, StmntEntry> dbStmnt = _dbStmntMap.get(dbname);
				if (dbStmnt == null)
				{
					dbStmnt = new LinkedHashMap<String, StmntEntry>();
					_dbStmntMap.put(dbname, dbStmnt);
				}
				
				// Get "normalizedHashId" entry
				StmntEntry stmntEntry = dbStmnt.get(hash);
				if (stmntEntry == null)
				{
					stmntEntry = new StmntEntry(hash, normalizedSqlText);
					dbStmnt.put(hash, stmntEntry);
				}
				
				// Finally Add "original" SQL Text
				StmntCacheDetailsEntry stmntCacheDetailsEntry = new StmntCacheDetailsEntry();
				stmntCacheDetailsEntry._useCount         = useCount;
				stmntCacheDetailsEntry._cachedDate       = cachedDate;
				stmntCacheDetailsEntry._lastUsedDate     = lastUsedDate;
				stmntCacheDetailsEntry._maxPlanSizeKB    = maxPlanSizeKB;
				stmntCacheDetailsEntry._avgLIO           = avgLIO;
				stmntCacheDetailsEntry._avgPIO           = avgPIO;
				stmntCacheDetailsEntry._avgCpuTime       = avgCpuTime;
				stmntCacheDetailsEntry._avgElapsedTime   = avgElapsedTime;
				stmntCacheDetailsEntry._totalLIO         = totalLIO;
				stmntCacheDetailsEntry._totalPIO         = totalPIO;
				stmntCacheDetailsEntry._totalCpuTime     = totalCpuTime;
				stmntCacheDetailsEntry._totalElapsedTime = totalElapsedTime;
				stmntCacheDetailsEntry._sqlText          = sqlText;

				stmntEntry.addOriginalSqlText(objName, stmntCacheDetailsEntry);
			}
		}

		if (_parseErrorCount > 0)
		{
			_ps.println("");
			_ps.println("Parse Error Count: " + _parseErrorCount);
			_ps.println("");
		}
	}
	

	private String removeLastSybaseXxx(String sqlText)
	{
		int end = sqlText.length() - 1;
		if ( sqlText.charAt(end) != ')' )
			return sqlText;

		int balance = 0;
		int start   = -1;

		for (int i = end; i >= 0; i--)
		{
			char c = sqlText.charAt(i);
			
			if      ( c == ')' ) balance++;
			else if ( c == '(' ) balance--;

			if ( balance == 0 )
			{
				start = i;
				break;
			}
		}

		if ( start != -1 )
		{
//			_ps.println("Start index: " + start);
//			_ps.println("End index: " + end);
//			String result = sqlText.substring(0, start).trim();
//			_ps.println("Result: '" + result + "'");
			String xxx = sqlText.substring(start);

			if (xxx.contains(" output"))
				return sqlText.substring(0, start).trim();

			if (xxx.startsWith("(@@@"))
				return sqlText.substring(0, start).trim();
			
			return sqlText;
		}
		else
		{
			return sqlText;
		}
	}

	//	private void sort()
//	{
//		Map<String, Map<String, StmntEntry>> sorted = _dbStmntMap.entrySet().stream()
//				.sorted((e1, e2) -> {
//					int count1 = e1.getValue().values().stream()
//							.mapToInt(s -> s._originSqlTextSet.size())
//							.sum();
//					int count2 = e2.getValue().values().stream()
//							.mapToInt(s -> s._originSqlTextSet.size())
//							.sum();
//					return Integer.compare(count2, count1); // descending
//				})
//				.collect(Collectors.toMap(
//						Map.Entry::getKey,
//						Map.Entry::getValue,
//						(a, b) -> a,
//						LinkedHashMap::new // preserve order
//						));
//
//		// Now 'sorted' is ordered by the count
//		sorted.forEach((db, stmts) -> {
//			int total = stmts.values().stream().mapToInt(s -> s._originSqlTextSet.size()).sum();
//			_ps.println("############# dbname=" + db + " => total originSqlTextSet size = " + total);
//		});
//		
//		_dbStmntMap = sorted;
//	}
	private void sort()
	{
		// This looks a bit "funky" ... But it was Generated by AI (Chat GPT)
		Map<String, Map<String, StmntEntry>> sorted = _dbStmntMap.entrySet().stream()
				.map(entry -> {
					// Sort inner map by _stmntCacheDetailsEntrySet size
					Map<String, StmntEntry> sortedInner =
							entry.getValue().entrySet().stream()
							.sorted((a, b) -> Integer.compare(
									b.getValue()._stmntCacheDetailsEntrySet.size(),
									a.getValue()._stmntCacheDetailsEntrySet.size()
									))
							.collect(Collectors.toMap(
									Map.Entry::getKey,
									Map.Entry::getValue,
									(a, b) -> a,
									LinkedHashMap::new
									));
					return Map.entry(entry.getKey(), sortedInner);
				})
				// Now sort outer map by total count of origin SQL texts
				.sorted((e1Outer, e2Outer) -> {
					int count1 = e1Outer.getValue().values().stream().mapToInt(s -> s._stmntCacheDetailsEntrySet.size()).sum();
					int count2 = e2Outer.getValue().values().stream().mapToInt(s -> s._stmntCacheDetailsEntrySet.size()).sum();
					return Integer.compare(count2, count1); // descending
				})
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(a, b) -> a,
						LinkedHashMap::new
						));

		// --- Print result ---
//		sorted.forEach((db, stmts) -> 
//		{
//			int total = stmts.values().stream().mapToInt(s -> s._originSqlTextSet.size()).sum();
//			_ps.println(db + " => total = " + total);
//			stmts.forEach((hash, stmnt) -> _ps.println("   " + hash + " -> " + stmnt._originSqlTextSet.size()));
//		});
		
		_dbStmntMap = sorted;
	}

	private void printReportPrologue()
	{
		_ps.println();
		_ps.println("--============================================================================");
		_ps.println("-- Summary of what this report does!");
		_ps.println("------------------------------------------------------------------------------");
		_ps.println("-- * Get ALL SQL Statements from the Statement Cache.");
		_ps.println("-- * Pre step to remove some Syntax that is added by the Statement Cache ");
		_ps.println("-- * Normalize each SQL Text (where we remove constants and replace them with ? ");
		_ps.println("-- * Make a MD5 Hash of the above Normalized SQL Text");
		_ps.println("-- * Keep a Map of each Normalized SQL Text and all it's duplicates (origin SQL Text)");
		_ps.println("-- * Sort the Map with: most 'un-paraetarized-SQLText' per Normalized SQL Text.");
		_ps.println("-- * Print a report... ");
		_ps.println("-- * ");
		_ps.println("------------------------------------------------------------------------------");
		_ps.println("-- NOTE: Some SQL Texts wont pass the Normalization step.");
		_ps.println("--       They are printed as '>>> WARNING: Problems parsing...' at the top of the report");
		_ps.println("------------------------------------------------------------------------------");
		_ps.println("-- Config: ");
		_ps.println("-- * duplicateThreshold = " + _duplicateThreshold);
		_ps.println("-- * printAvgStat       = " + _printAvgStat);
		_ps.println("-- * printTotalStat     = " + _printTotalStat);
		_ps.println("------------------------------------------------------------------------------");
		_ps.println();
		
	}
	private void printReport()
	{
		int bloatStmntCount = 0;
		int bloatStmntCountAboveThreshold = 0;

		for (Entry<String, Map<String, StmntEntry>> dbEntry : _dbStmntMap.entrySet())
		{
			String dbname = dbEntry.getKey();
			Map<String, StmntEntry> hashStmntEntry = dbEntry.getValue();

			boolean dbHasBloatRecords = false;
			for (StmntEntry stmntEntry : hashStmntEntry.values())
			{
				int size = stmntEntry._stmntCacheDetailsEntrySet.size();
				
				if (size >= _duplicateThreshold)
				{
					dbHasBloatRecords = true;
				}
			}
			if ( ! dbHasBloatRecords )
				continue;
			
			_ps.println();
			_ps.println();
			_ps.println("--=======================================================");
			_ps.println("---- For dbname '" + dbname + "'.");
			_ps.println("---------------------------------------------------------");

			for (StmntEntry stmntEntry : hashStmntEntry.values())
			{
				bloatStmntCount++;
				int size = stmntEntry._stmntCacheDetailsEntrySet.size();
				
				if (size >= _duplicateThreshold)
				{
					String type = "";
					if (stmntEntry._count_sq > 0) type += "*sq (Prepared Statement)";
					if (stmntEntry._count_ss > 0) type += "*ss (Language Statement in Statement Cache)";
					if (StringUtil.isNullOrBlank(type))
						type = "??";

					bloatStmntCountAboveThreshold++;

					_ps.println();
					_ps.println("Count: " + stmntEntry._stmntCacheDetailsEntrySet.size() + ", Type=" + type);
//					_ps.println("    >>>>> Normalized SQL = " + stmntEntry._normalizedSqlText);
					_ps.println("    >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> sumPlanSizeKB=" + stmntEntry._sumPlanSizeKB + " >>>>>>>>>>>>>>> Normalized SQL = " + stmntEntry._normalizedSqlText);
					
					int cnt = 0;
					for (StmntCacheDetailsEntry stmntCacheDetailsEntry : stmntEntry._stmntCacheDetailsEntrySet)
					{
						cnt++;
						
//						_ps.println("    Entry: " + cnt + ", Origin SQL = " + originSql);
						_ps.println("    Entry: " + cnt 
								+ ", UseCount="         + stmntCacheDetailsEntry._useCount
								+ ", lastUsedDate="     + stmntCacheDetailsEntry.getLastUsedDateAsString()
								+ ", maxPlanSizeKB="    + stmntCacheDetailsEntry._maxPlanSizeKB
								+ ( _printAvgStat ? (""
        								+ ", avgLIO="           + stmntCacheDetailsEntry._avgLIO 
        								+ ", avgPIO="           + stmntCacheDetailsEntry._avgPIO 
        								+ ", avgCpuTime="       + stmntCacheDetailsEntry._avgCpuTime 
        								+ ", avgElapsedTime="   + stmntCacheDetailsEntry._avgElapsedTime
    								) : ""
								)
								+ ( _printAvgStat ? (""
        								+ ", totalLIO="         + stmntCacheDetailsEntry._totalLIO 
        								+ ", totalPIO="         + stmntCacheDetailsEntry._totalPIO 
        								+ ", totalCpuTime="     + stmntCacheDetailsEntry._totalCpuTime 
        								+ ", totalElapsedTime=" + stmntCacheDetailsEntry._totalElapsedTime 
    								) : ""
								)
								+ ", Origin SQL = "     + stmntCacheDetailsEntry._sqlText
								);
					}
				}
			}
		}
		
		_ps.println("");
		_ps.println("Found " + bloatStmntCount + " Bloated Statements, where " + bloatStmntCountAboveThreshold + " had more than " + _duplicateThreshold + " duplicates.");
		_ps.println("");
		_ps.println("--END-OF-REPORT-");
		_ps.println("");
	}


	private static DbxConnection connect(String url, String user, String passwd, PrintStream ps)
	throws SQLException
	{
		if (ps == null)
			ps = System.out;

		Connection conn = DriverManager.getConnection(url, user, passwd);
		DbxConnection dbxConn = DbxConnection.createDbxConnection(conn);

		String sql = "select @@servername, @@version";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				String srvName    = rs.getString(1);
				String srvVersion = rs.getString(2);

				ps.println(" - Connected to: @@servername='" + srvName + "', @@version='" + srvVersion + "'.");
			}
		}
		
		return dbxConn;
	}

	private void close()
	throws SQLException
	{
		if (_conn != null)
			_conn.close();
	}
}

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
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private PrintStream _ps = System.out;
	private DbxConnection _conn;

	// 
	private Map<String, Map<String, StmntEntry>> _dbStmntMap = new LinkedHashMap<>();
	//          dbname      normHash 

	private int     _duplicateThreshold = Configuration.getCombinedConfiguration().getIntProperty    ("AseStmntCacheBloatDetect.duplicateThreshold", 10);
	private boolean _printAvgStat       = Configuration.getCombinedConfiguration().getBooleanProperty("AseStmntCacheBloatDetect.printAvgStat"  , false);
	private boolean _printTotalStat     = Configuration.getCombinedConfiguration().getBooleanProperty("AseStmntCacheBloatDetect.printTotalStat", false);

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
			if (_lastUsedDate == null)
				return "----------null---------";
			//          2018-01-08 09:56:53.716

			return TimeUtils.toString(_lastUsedDate);
		}
		
		public String getCacheAgeAsHMS()
		{
			if (_cachedDate == null)
				return "--null--";
			//          09:56:53
			long secondsSinceCached = (System.currentTimeMillis() - _cachedDate.getTime()) / 1000;
			return TimeUtils.secToTimeStrLong(secondsSinceCached);
			
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

	public static void doWork(DbxConnection conn, boolean closeConnection, PrintStream printStream, int duplicateThreshold, boolean printAvgStat, boolean printTotalStat)
	throws SQLException
	{
		if (conn == null)
			throw new NullPointerException("Connection cant be null.");

		AseStmntCacheBloatDetect bd = new AseStmntCacheBloatDetect(printStream);
		bd._conn = conn;
		bd._duplicateThreshold = duplicateThreshold;
		bd._printAvgStat       = printAvgStat;
		bd._printTotalStat     = printTotalStat;
		
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
//			    + "    ,show_cached_text(SSQLID) as SQLText \n"
			    + "    ,show_cached_text_long(SSQLID) as SQLText \n"
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

				if (objName == null)
				{
					_ps.println("   >>> WARNING: objName=null, skipping and continuing... sqlText=|" + sqlText + "|.");
					continue;
				}

				if (dbname == null)
				{
					_ps.println("   >>> WARNING: dbname=null, skipping and continuing... sqlText=|" + sqlText + "|.");
					continue;
				}

				//----------------------------------------------
				// Cleanup the SQL a bit 
				//----------------------------------------------

				// trim
				sqlText = sqlText.trim();

				// Remove Sybase trailing parameter declarations like: 
				//    >>> (@P123 VARCHAR(64) output, @P124 VARCHAR(64) output, ...)
				//    >>> (@@@...)
				if (sqlText.endsWith(")"))
				{
					String before = sqlText;
					String after  = removeLastSybaseStatementCacheSpecifics(sqlText);
					
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

				// remove any potential: PLAN '(...)'
				sqlText = removePlanHints(sqlText);
				
				// remove all "single line comments" like: select a,b,c -- Remove this comment until eol
				// If not when removing all "\n"... we will simply comment out "everything" after first '--'
				// Regex explanation:
				//   '--'     matches the literal comment start
				//   '[^\n]*' matches any characters except newline (zero or more times)
				sqlText= sqlText.replaceAll("--[^\n]*", "");

				// and potentially extra spaces
				sqlText = sqlText.replaceAll("\\s+", " ");

				// trim
				sqlText = sqlText.trim();

//				String normalizedSqlText = StatementNormalizer.getInstance().normalizeSqlText(sqlText, normalizeParameters, tableList);
				String normalizedSqlText = StatementNormalizer.getInstance().normalizeSqlText(sqlText, null, null);

				Exception firstParseEx = StatementNormalizer.getInstance().getLastParserException();
				Exception lastParseEx  = StatementNormalizer.getInstance().getLastParserException();
				String firstParseExStr = null;
				String lastParseExStr  = null;
				if (firstParseEx != null) { firstParseExStr = (firstParseEx + "").replace('\n', ' '); }
				if (lastParseEx  != null) { lastParseExStr  = (lastParseEx  + "").replace('\n', ' '); }

				// Compose a "Exception" String to append...
				String exStr = "";
				if (firstParseExStr != null && lastParseExStr != null && firstParseExStr.equals(lastParseExStr))
					exStr = "   ----- Exception: " + firstParseExStr;
				else
					exStr = "   ----- FirstException: " + firstParseExStr + ", LastException: " + lastParseExStr;

				// Parse FAILED
				if (normalizedSqlText == null)
				{
					_parseErrorCount++;
					_ps.println("   -->>> WARNING: Problems parsing, skipping and continuing... sqlText=|" + sqlText + "|. " + exStr);
					
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
	

	private String removeLastSybaseStatementCacheSpecifics(String sqlText)
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

	private static final Pattern PLAN_PATTERN = Pattern.compile("\\s+PLAN\\s+'[^']*'", Pattern.CASE_INSENSITIVE);
	/**
	 * Removes Sybase ASE PLAN hints from SQL statements
	 * @param sql The SQL statement to clean
	 * @return The cleaned SQL statement
	 */
	public static String removePlanHints(String sql) 
	{
		if (sql == null || sql.isEmpty()) 
			return sql;
        
		// Remove PLAN clauses with quotes
		String cleaned = PLAN_PATTERN.matcher(sql).replaceAll("");

		// Clean up any extra whitespace that may have been left
//		cleaned = cleaned.replaceAll("\\s+", " ").trim();

		return cleaned;
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
				.peek(entry -> { if (entry.getKey() == null || entry.getValue() == null) System.out.println("Outer entry: key=" + entry.getKey() + ", value=" + entry.getValue()); }) // NULL DEBUGGING
				.map(entry -> {
					// Sort inner map by _stmntCacheDetailsEntrySet size
					Map<String, StmntEntry> sortedInner =
							entry.getValue().entrySet().stream()
							.peek(innerEntry -> { if (innerEntry.getKey() == null || innerEntry.getValue() == null) System.out.println("  Inner entry: key=" + innerEntry.getKey() + ", value=" + innerEntry.getValue()); })
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
				.peek(mapEntry -> { if (mapEntry.getKey() == null || mapEntry.getValue() == null) System.out.println("After Map.entry: key=" + mapEntry.getKey() + ", value=" + mapEntry.getValue()); }) // NULL DEBUGGING
				// Now sort outer map by total count of origin SQL texts
				.sorted((e1Outer, e2Outer) -> {
					int count1 = e1Outer.getValue().values().stream().mapToInt(s -> s._stmntCacheDetailsEntrySet.size()).sum();
					int count2 = e2Outer.getValue().values().stream().mapToInt(s -> s._stmntCacheDetailsEntrySet.size()).sum();
					return Integer.compare(count2, count1); // descending
				})
				.peek(mapEntry -> { if (mapEntry.getKey() == null || mapEntry.getValue() == null) System.out.println("After Sort.entry: key=" + mapEntry.getKey() + ", value=" + mapEntry.getValue()); }) // NULL DEBUGGING
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

		NumberFormat nf = NumberFormat.getInstance();

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

					String extraInfo = "";
					if (stmntEntry._normalizedSqlText.contains(" IN (...)"))
					{
						extraInfo = ", Contains IN (...)";
					}

					_ps.println();
					_ps.println("Count: " + stmntEntry._stmntCacheDetailsEntrySet.size() + ", Type=" + type + extraInfo);
//					_ps.println("    >>>>> Normalized SQL = " + stmntEntry._normalizedSqlText);
					_ps.println("    >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> sumPlanSizeKB=" + stmntEntry._sumPlanSizeKB + " >>>>>>>>>>>>>>> Normalized SQL = " + stmntEntry._normalizedSqlText);

					long sum_maxPlanSizeKb    = 0;
					long sum_totalLIO         = 0;
					long sum_totalPIO         = 0;
					long sum_totalCpuTime     = 0;
					long sum_totalElapsedTime = 0;

					int cnt = 0;
					for (StmntCacheDetailsEntry stmntCacheDetailsEntry : stmntEntry._stmntCacheDetailsEntrySet)
					{
						cnt++;

						sum_maxPlanSizeKb    += stmntCacheDetailsEntry._maxPlanSizeKB   ;
						sum_totalLIO         += stmntCacheDetailsEntry._totalLIO        ;
						sum_totalPIO         += stmntCacheDetailsEntry._totalPIO        ;
						sum_totalCpuTime     += stmntCacheDetailsEntry._totalCpuTime    ;
						sum_totalElapsedTime += stmntCacheDetailsEntry._totalElapsedTime;
						
//						_ps.println("    Entry: " + cnt + ", Origin SQL = " + originSql);
						_ps.println("    Entry: " + cnt 
								+ ", UseCount="         + stmntCacheDetailsEntry._useCount
								+ ", lastUsedDate="     + stmntCacheDetailsEntry.getLastUsedDateAsString()
								+ ", createAge="        + stmntCacheDetailsEntry.getCacheAgeAsHMS()
								+ ", maxPlanSizeKB="    + stmntCacheDetailsEntry._maxPlanSizeKB
								+ ( _printAvgStat ? (""
        								+ ", avgLIO="           + stmntCacheDetailsEntry._avgLIO 
        								+ ", avgPIO="           + stmntCacheDetailsEntry._avgPIO 
        								+ ", avgCpuTime="       + stmntCacheDetailsEntry._avgCpuTime 
        								+ ", avgElapsedTime="   + stmntCacheDetailsEntry._avgElapsedTime
    								) : ""
								)
								+ ( _printTotalStat ? (""
        								+ ", totalLIO="         + stmntCacheDetailsEntry._totalLIO 
        								+ ", totalPIO="         + stmntCacheDetailsEntry._totalPIO 
        								+ ", totalCpuTime="     + stmntCacheDetailsEntry._totalCpuTime 
        								+ ", totalElapsedTime=" + stmntCacheDetailsEntry._totalElapsedTime 
    								) : ""
								)
								+ ", Origin SQL = "     + stmntCacheDetailsEntry._sqlText
								);
					} // end: stmntCacheDetailsEntry

					_ps.println("    SUM: "
							+ "maxPlanSizeKB="      + nf.format(sum_maxPlanSizeKb) 
							+ "; totalLIO="         + nf.format(sum_totalLIO)
							+ "; totalPIO="         + nf.format(sum_totalPIO)
							+ "; totalCpuTime="     + TimeUtils.msToTimeStrDHMSms(sum_totalCpuTime) 
							+ "; totalElapsedTime=" + TimeUtils.msToTimeStrDHMSms(sum_totalElapsedTime) 
							);

				} // end: if (size >= _duplicateThreshold)
				
			} // end: for (StmntEntry stmntEntry : hashStmntEntry.values())
			
		} // end: for (Entry<String, Map<String, StmntEntry>> dbEntry : _dbStmntMap.entrySet())
		
		_ps.println("");
		_ps.println("Found " + bloatStmntCount + " Bloated Statements, where " + bloatStmntCountAboveThreshold + " had more than " + _duplicateThreshold + " duplicates.");
		_ps.println("");
		_ps.println("--END-OF-REPORT-");
		_ps.println("");
		
		_logger.info("Found " + bloatStmntCount + " Bloated Statements, where " + bloatStmntCountAboveThreshold + " had more than " + _duplicateThreshold + " duplicates.");
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

	public static void main(String[] args)
	{
		final int defaultDuplicateThreshold = 10;

//		System.out.println("args=" + args.length);
		if (args.length < 3)
		{
			System.out.println("");
			System.out.println("ERROR: Missing some parameters");
			System.out.println("");
			System.out.println("Usage: url/Hostname:port username password [duplicateThreshold] [printAvgStat] [printTotalStat]");
			System.out.println("");
			System.out.println("Example 1: dbxtune.sh com.dbxtune.tools.AseStmntCacheBloatDetect prod-a1-ase.acme.com:5000 sa secretPassword");
			System.out.println("Example 2: dbxtune.sh com.dbxtune.tools.AseStmntCacheBloatDetect jdbc:sybase:Tds:prod-a1-ase:5000/master sa secretPassword");
			System.out.println("Example 3: dbxtune.sh com.dbxtune.tools.AseStmntCacheBloatDetect jdbc:sybase:Tds:prod-a1-ase:5000/master sa secretPassword");
			System.out.println("");
			System.out.println("Note: If you run on Windows, repace '.sh' with '.bat'");
			System.out.println("");
			System.exit(1);
		}

		// PROD_A1_ASE
//		String url = "jdbc:sybase:Tds:prod-a1-ase:5000/master";
		String url    = args[0];
		String user   = args[1];
		String passwd = args[2];
		int     duplicateThreshold = args.length > 3 ? StringUtil.parseInt(args[3], defaultDuplicateThreshold)     : defaultDuplicateThreshold;
		boolean printAvgStat       = args.length > 4 ? StringUtil.parseInt(args[4], 0                        ) > 0 : false;
		boolean printTotalStat     = args.length > 5 ? StringUtil.parseInt(args[5], 0                        ) > 0 : false;
		
		if ( ! url.startsWith("jdbc:sybase:Tds:") )
		{
			url = "jdbc:sybase:Tds:" + url;
		}
		
		System.out.println("");
		System.out.println("##============================================================================");
		System.out.println("## URL:                " + url);
		System.out.println("## Username:           " + user);
		System.out.println("## Password:           " + "**secret**");
		System.out.println("## duplicateThreshold: " + duplicateThreshold);
		System.out.println("## printAvgStat:       " + printAvgStat);
		System.out.println("## printTotalStat:     " + printTotalStat);
		System.out.println("##----------------------------------------------------------------------------");
		System.out.println("");

		try
		{
			System.out.println(" - Connectiong to URL: " +url);
			DbxConnection conn = AseStmntCacheBloatDetect.connect(url, user, passwd, System.out);

			System.out.println(" - Executing AseStmntCacheBloatDetect");
			AseStmntCacheBloatDetect.doWork(conn, true, System.out, duplicateThreshold, printAvgStat, printTotalStat);
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
		}
	}
}

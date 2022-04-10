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
package com.asetune.sql;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.PropertyConfigurator;

import com.asetune.utils.StringUtil;

public class SqlParserUtils
{
//	private static Logger _logger = Logger.getLogger(SqlParserUtils.class);
//
//	private static final String REGEXP_MLC = "(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)";   // MLC=MultiLineComment  from http://blog.ostermiller.org/find-comment
//
//	/**
//	 * Get a List of Tables that is specified in a SQL Statement
//	 * 
//	 * @param sql                The SQL Statement to parse
//	 * @return                   Always a List of tables. (if not found, an empty list will be returned)
//	 * @throws ParseException    If there was issues with the SQL Statement
//	 */
//	public static List<String> getTables(String sql) 
//	throws ParseException 
//	{
//		return getTables(sql, true, false);
//	}
//
//	/**
//	 * Get a List of Tables that is specified in a SQL Statement
//	 * 
//	 * @param sql                The SQL Statement to parse
//	 * @param preParseFix        A Normalized Statement has some chars that will be replaced (?->'?', ...->'...') and other "fixes" before we parse the passed SQL
//	 * @return                   Always a List of tables. (if not found, an empty list will be returned)
//	 * @throws ParseException    If there was issues with the SQL Statement
//	 */
//	public static List<String> getTables(String sql, boolean preParseFix) 
//	throws ParseException 
//	{
//		return getTables(sql, true, false);
//	}
//
//	/**
//	 * Get a List of Tables that is specified in a SQL Statement
//	 * 
//	 * @param sql                The SQL Statement to parse
//	 * @param preParseFix        A Normalized Statement has some chars that will be replaced (?->'?', ...->'...') and other "fixes" before we parse the passed SQL
//	 * @param logIssues          In case of parse issues, log to error log
//	 * @return                   Always a List of tables. (if not found, an empty list will be returned)
//	 * @throws ParseException    If there was issues with the SQL Statement
//	 */
//	public static List<String> getTables(String sql, boolean preParseFix, boolean logIssues) 
//	throws ParseException 
//	{
//		// exit early
//		if (StringUtil.isNullOrBlank(sql))
//			return Collections.emptyList();
//
//		if ("--not-found--".equals(sql))
//			return Collections.emptyList();
//
//		// 
//		if (preParseFix)
//		{
//			// Start by trimming, it's needed later in code
//			sql = sql.trim();
//
//			// Remove *starting* comments 
//			if (sql.startsWith("/*"))
//				sql = sql.replaceAll(REGEXP_MLC, "").trim();
//
//			//----------------------------------------------------------------
//			// Strip various things for: Sybase ASE
//			//----------------------------------------------------------------
//
//			// Some stuff done by the "normalizer" in DbxTune... which we may want to replace
//			sql = sql.replace("?", "'?'");
//			sql = sql.replace("(...)", "('...')");
//			sql = sql.replace("*=", "= /*really:left-outer-join*/");
//			sql = sql.replace("=*", "= /*really:right-outer-join*/");
//
//			// FIX bulk insert...
//			if (sql.contains(" bulk ") && sql.contains(" with "))
//			{
//				// transform: insert bulk SEK_DATAMART_prod..RTA_HDG_EFF_T_I_REP with arrayinsert, nodescribe
//				//      into: insert into SEK_DATAMART_prod..RTA_HDG_EFF_T_I_REP values('dummy1', 'dummy2')
//				// The above just to pass the pars stage and get table name!
//				sql = sql.replace(" bulk ", " into ");
//				sql = sql.substring(0, sql.indexOf(" with "));
//				sql = sql + " values('dummy1', 'dummy2')";
//			}
//
//
//
//			// Remove "ASE Dynamic SQL" parameters specifications (at the end of a statement)
//			// example1: SELECT jobname from MxGJobStatus where meid=@@@V0_VCHAR1(@@@V0_VCHAR1 VARCHAR(64))
//			//                                                                   ^^^^^^^^^^^^^^^^^^^^^^^^^^
//			// example2: select M_CHLD_PRMT from CAC_RTGB_DBF  where M__INDEX_ = @@@V0_INT(@@@V0_INT INT)
//			//                                                                            ^^^^^^^^^^^^^^^
//			// Also the statement cache has some extra chars the current parser can't handle
//			// The END of the Statement look like below:
//			// select c1 from t1 where c2 = @p0 and c3 = @p1(@p1 VARCHAR(64) output, @p0 VARCHAR(64) output)
//			//      remove the following                    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//			// 
//			// if statement ENDS with ')' and the matching start parameter '(' if followed by '@@@'... or '@p' then remove that whole section!
//			//
////			if ( sql.endsWith(")") && (sql.contains("@@@") || sql.contains("@p0")) )
//			if ( sql.endsWith(")") && sql.contains("@") )
//			{
//				int cnt = 0;
//				// Find matching start '(' position!
//				for (int c=sql.length()-1; c>0; c--)
//				{
//					char ch = sql.charAt(c);
//					if (ch == ')') cnt ++;
//					if (ch == '(') cnt --;
//
//					if (cnt == 0)
//					{
////						String tmp = sql.substring(c+1, c+4);
////						if ("@@@".equals(tmp) || tmp.startsWith("@p"))
//						String tmp = sql.substring(c+1, c+2);
//						if ("@".equals(tmp))
//						{
//							if (_logger.isDebugEnabled())
//								_logger.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> old SQL=|" + sql + "|.");
//
//							sql = sql.substring(0, c);
//
//							if (_logger.isDebugEnabled())
//								_logger.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> new SQL=|" + sql + "|.");
//						}
//						
//						break;
//					}
//				}
//
//				// Remove "ASE Dynamic SQL" parameters
//				sql = sql.replace("@@@", "");
//			}
//			
//			// Skip things like: "DYNAMIC_SQL dyn123:"   (remove everything before first ':') 
//			if (sql.startsWith("DYNAMIC_SQL "))
//			{
//				int pos = sql.indexOf(":");
//				if (pos != -1)
//				{
//					pos++;
//					sql = sql.substring(pos).trim();
//				}
//			}
//
//			// Skip things like this.
//			if (StringUtil.indexOfIgnoreCase(sql, "dump database "            ) != -1) return Collections.emptyList();
//			if (StringUtil.indexOfIgnoreCase(sql, "dump tran "                ) != -1) return Collections.emptyList();
//			if (StringUtil.indexOfIgnoreCase(sql, "dump transaction "         ) != -1) return Collections.emptyList();
//			if (StringUtil.indexOfIgnoreCase(sql, "-- Queue Service Execution") != -1) return Collections.emptyList();
//			
//			// if it's a view ... take away the "create view xxx as "
//			if (StringUtil.startsWithIgnoreBlankIgnoreCase(sql, "create view "))
//			{
//				sql = sql.replace('\n', ' ');
//				int start = StringUtil.indexOfIgnoreCase(sql, " AS ");
//				if (start != -1)
//					sql = sql.substring(start + " AS ".length());
//			}
//
//			//----------------------------------------------------------------
//			// Strip various things for: SQL Server 
//			//----------------------------------------------------------------
//			if (sql.startsWith("("))
//			{
//				int endPos = StringUtil.indexOfEndBrace(sql, 1, ')');
//				if (endPos != -1)
//				{
//					endPos++;
//					sql = sql.substring(endPos).trim();
//				}
//			}
//		}
//		
//		// starts with create...
//		if (StringUtil.startsWithIgnoreBlankIgnoreCase(sql, "create "))
//			return Collections.emptyList();
//
//		// starts with begin...
//		if (StringUtil.startsWithIgnoreBlankIgnoreCase(sql, "begin "))
//			return Collections.emptyList();
//
//		// Parser can't handle T-SQL Specifics... like: declare @id int
//		if (StringUtil.startsWithIgnoreBlankIgnoreCase(sql, "declare "))
//			return Collections.emptyList();
//
//		// starts with exec... no way the parser can know what tables are inside the procedure!
//		if (StringUtil.startsWithIgnoreBlankIgnoreCase(sql, "exec"))
//			return Collections.emptyList();
//
//		// Can any any SQL Statement (using any table) be shorter than 10 chars?
//		if (sql.length() < 10)
//			return Collections.emptyList();
//
//
//		// Now parse and get the table list
//		try
//		{
//			Statement stmt = CCJSqlParserUtil.parse(sql, parser -> parser.withSquareBracketQuotation(true));
//
//			try
//			{
//				TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
//				List<String> tableList = tablesNamesFinder.getTableList(stmt);
//				return tableList;
//			}
//			catch(RuntimeException rte)
//			{
//				_logger.error("StatementTablesFinder.getTables(): Problems getting table names for SQL Statement=|" + sql + "|. Caught: " + rte, rte);
//				_logger.error("", rte);
//
//				return Collections.emptyList();
//			}
//		}
//		catch (JSQLParserException ex)
//		{
//			Throwable cause = ex.getCause();
//			String causeStr = cause == null ? "" : cause.toString().replace('\n', ' ');
//
//			if (cause instanceof ParseException)
//			{
//				if (logIssues)
//					_logger.warn("StatementTablesFinder.getTables(): Problems parsing SQL Statement=|" + sql + "|. ex=" + ex + ", cause=" + causeStr);
//				throw (ParseException) cause;
//			}
//
//			if (logIssues)
//				_logger.error("StatementTablesFinder.getTables(): Problems parsing SQL Statement=|" + sql + "|. ex=" + ex + ", cause="+causeStr, cause);
//
//			//throw new ParseException("Parser caught unexpected exception '" + cause + "'.");
//
//			return Collections.emptyList();
////			return null;
//		}
//	}
//
//	/**
//	 * Same as getTables() but it do not throw exceptions
//	 * @param sql
//	 * @return
//	 */
//	public static List<String> getTablesNoThrow(String sql)
//	{
//		return getTablesNoThrow(sql, true, true);
//	}
//	/**
//	 * Same as getTables() but it do not throw exceptions
//	 * @param sql
//	 * @return
//	 */
//	public static List<String> getTablesNoThrow(String sql, boolean replaceOffendingNormalizedChars)
//	{
//		return getTablesNoThrow(sql, true, true);
//	}
//	/**
//	 * Same as getTables() but it do not throw exceptions
//	 * @param sql
//	 * @return
//	 */
//	public static List<String> getTablesNoThrow(String sql, boolean replaceOffendingNormalizedChars, boolean logIssues)
//	{
//		try
//		{
//			return getTables(sql, replaceOffendingNormalizedChars, logIssues);
//		}
//		catch (ParseException ex)
//		{
//			_logger.warn("getTablesNoThrow(), had problems. Caught: " + ex);
//			return Collections.emptyList();
////			return null;
//		}
//	}

	
	
	/**
	 * Get a List of Tables that is specified in a SQL Statement
	 * 
	 * @param sql  The SQL Statement to parse
	 * @return     Always a List of tables. (if not found, an empty list will be returned)
	 */
//	public static List<String> getTables(String sql) 
//	{
////FIXME; change this to return a Set... LinkedHashSet implemented AFTER this method
//		if (StringUtil.isNullOrBlank(sql))
//			return Collections.emptyList();
//			
//		TableNameParser tableNameParser = new TableNameParser(sql);
//		Collection<String> tables = tableNameParser.tables();
//
//		// CheckTheTables; if they typically is "@xxxx" or "declare" or similar... then remove them from the list.
//		// Solved with another workaround: The caller can store "faulty" entries in a "discarded" Set ... (thats how I solved it in DDL Storage) 
//		return new ArrayList<>(tables);
//	}
	public static Set<String> getTables(String sql) 
	{
		if (StringUtil.isNullOrBlank(sql))
			return Collections.emptySet();
			
		TableNameParser tableNameParser = new TableNameParser(sql);
		Collection<String> tables = tableNameParser.tables();

		// Remove some stuff that the 'TableNameParser' don't get "right"
		tables.removeIf(t -> t.startsWith("@") || t.startsWith("#") || t.equalsIgnoreCase("ROW_NUMBER"));

		// CheckTheTables; if they typically is "@xxxx" or "declare" or similar... then remove them from the list.
		// Solved with another workaround: The caller can store "faulty" entries in a "discarded" Set ... (thats how I solved it in DDL Storage) 
		return new LinkedHashSet<>(tables);
	}
	
	
	private static Collection<String> test(String sql)
	{
//		return getTablesNoThrow(sql);
//		return new TableNameParser(sql).tables();
		return getTables(sql);
	}
	
	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		//log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		System.out.println("NULL: " + test(null));
		System.out.println("1:  " + test("SELECT T1.TIMESTAMP, T1.M_IDENTITY, T1.M_LABEL, T1.M__INDEX_, T1.M__LINK_, T1.M_COL0, T1.M_COL1, T1.M_COL2, T1.M_COL3, T1.M_COL4, T1.M_COL5, T1.M_COL6, T1.M_COL7, T1.M_COL8, T1.M_COL9, T1.M_COL10, T1.M_COL11, T1.M_COL12, T1.M_COL13, T1.M_COL14, T1.M_COL15, T1.M_COL16, T1.M_COL17, T1.M_COL18, T1.M_COL19, T1.M_COL20, T1.M_COL21, T1.M_COL22, T1.M_COL23, T1.M_COL24, T1.M_COL25, T1.M_COL26, T1.M_COL27, T1.M__DATE_, T1.M__REPLICAT_, T1.M_PERMUTID, T1.M_DSP_WDTH FROM MPX_ASGRC_DBF T1 WHERE ((T1.M_LABEL = ?) AND (T1.M__DATE_ = ?))"));
		System.out.println("2:  " + test("SELECT DISTINCT T1.M_INSTRUM, T1.M_MARKET, T1.M_TYPE, T1.M_MDIREF, T1.M_MATCOD FROM MPX_VOL_DBF T1 WHERE T1.M_INSTRUM IN (...)"));
		System.out.println("3:  " + test("DELETE FROM MPAUD_BD_DBF WHERE MPAUD_BD_DBF.M_LINK IN (...)"));
		System.out.println("4:  " + test("SELECT T1.M_TARGET_DEAL, T1.M_LATEST_DEAL FROM CONTRACT_TRADE_MAPPING_DBF T1 WHERE (T1.M_TARGET_DEAL = ?)"));
		System.out.println("5:  " + test("SELECT T1.M_SCNLABEL, T1.M_SCNNUM, T1.M_SCNTYPE, T1.M_FAMILY0, T1.M_FAMILY1, T1.M_TYPE0, T1.M_TYPE1, T1.M_MLABEL0, T1.M_MLABEL1, T1.M_SLABEL0, T1.M_SLABEL1, T1.M_MAT0, T1.M_MAT1, T1.M_DAYSNB, T1.M_VARITYP, T1.M_VARI, T1.M_VALUE, T1.M_OPTMAT, T1.M_STRIKE, T1.M_PARAMTYPE, T1.M_PARAMLABEL, T1.M_OPTMAT1, T1.M_STRIKE1, T1.M_PARAMGROUP, T1.M_HORIZOND FROM SE_VARCS_DBF T1 WHERE (((((T1.M_SCNLABEL = ?) AND T1.M_SCNNUM IN (...)) AND T1.M_SCNTYPE IN (...)) AND T1.M_MLABEL0 IN (...)) AND T1.M_SLABEL0 IN (...)) ORDER BY T1.M_SCNNUM ASC"));
		System.out.println("6:  " + test("/***DBXTUNE-REWRITE: [Changed: old style left-outer-join(*=), to equal-join(=)]***/ SELECT T1.M__DATE_, T1.M__ALIAS_, T1.M_REFERENCE, T1.M_SETNAME, T1.M_US_TYPE, T1.M_TYPE, T2.M__DATE_, T2.M__ALIAS_, T2.M_REFERENCE, T2.M_MATCODE FROM MPX_VOLMAT_DBF T1, MPY_VOLMAT_DBF T2 WHERE T1.M_REFERENCE = T2.M_REFERENCE AND T1.M__DATE_ = T2.M__DATE_ AND T1.M__ALIAS_ = T2.M__ALIAS_ AND ((((T1.M__DATE_ = ?) AND (T1.M__ALIAS_ = ?)) AND ((T1.M_SETNAME = ? AND T1.M_US_TYPE = ?) AND T1.M_TYPE = ?)) AND ((T2.M__DATE_ = ?) AND (T2.M__ALIAS_ = ?))) ORDER BY T1.M_SETNAME ASC, T1.M_US_TYPE ASC, T1.M_TYPE ASC"));
		System.out.println("7:  " + test("/***DBXTUNE-NORMALIZE: [Removed: numbers]***/ DYNAMIC_SQL dyn: "));
		System.out.println("8:  " + test("/***DBXTUNE-REWRITE: [Changed: old style left-outer-join(*=), to equal-join(=)]***/ SELECT T1.M__DATE_, T1.M__ALIAS_, T1.M_REFERENCE, T1.M_SETNAME, T1.M_US_TYPE, T1.M_TYPE, T2.M__DATE_, T2.M__ALIAS_, T2.M_REFERENCE, T2.M_MATCODE FROM MPX_VOLMAT_DBF T1, MPY_VOLMAT_DBF T2 WHERE T1.M_REFERENCE = T2.M_REFERENCE AND T1.M__DATE_ = T2.M__DATE_ AND T1.M__ALIAS_ = T2.M__ALIAS_ AND ((((T1.M__DATE_ = ?) AND (T1.M__ALIAS_ = ?)) AND ((T1.M_SETNAME IN (...) AND T1.M_US_TYPE = ?) AND T1.M_TYPE = ?)) AND ((T2.M__DATE_ = ?) AND (T2.M__ALIAS_ = ?))) ORDER BY T1.M_SETNAME ASC, T1.M_US_TYPE ASC, T1.M_TYPE ASC"));
		System.out.println("9:  " + test("/***DBXTUNE-REWRITE: [Removed: 'set nocount on|off', Changed: DoubleQuotes(\") into SingleQuotes(')]***/ SELECT jobname FROM MxGJobStatus WHERE meid = ?"));
		System.out.println("10: " + test("/***DBXTUNE-REWRITE: [Removed: 'set nocount on|off', Changed: DoubleQuotes(\") into SingleQuotes(')]***/ SELECT meid FROM MxGJobStatus WHERE pid = ? AND soj >= convert(datetime, ?, ?) AND eoj IS NULL"));
		System.out.println("11: " + test("SELECT T1.TIMESTAMP, T1.M_IDENTITY, T1.M_REFERENCE, T1.M_LABEL FROM MATSET_DBF T1 ORDER BY T1.M_REFERENCE ASC"));
		System.out.println("12: " + test("SELECT DISTINCT (SELECT DISTINCT M_PCG_DATE FROM MUREXDB.TRN_ENTD_DBF WHERE M_LABEL = ?) AS M_REP_DATE, getdate() AS M_SYS_DATE, E.FC_PACKAGE_ID AS M_PACKAGE, E.FC_ORIGIN_ID AS M_ORIG_REF, T.M_NB AS M_TRADE, E.FC_VERSION AS M_VERSION, CASE WHEN CR.STATUS_VALIDATION_LEVEL = ? THEN CASE WHEN CR.LTM_STATUS = ? THEN ? WHEN CR.LTM_STATUS = ? THEN ? ELSE CR.LTM_STATUS END ELSE E.JOB_DESCRIPTION END AS M_LTM_STATUS, T.M_TRN_DATE AS M_LTM_INOUTD, ? AS M_LTM_COM, CASE WHEN CR.STATUS_VALIDATION_LEVEL = ? THEN CR.LST_HNDLR ELSE E.STP_LAST_VALIDATOR END AS M_LTM_USER, CASE WHEN CR.STATUS_VALIDATION_LEVEL = ? THEN CR.COMPLETE_DATE ELSE E.STP_LAST_VALIDATION_DATE END AS M_LTM_ACTION, convert(numeric(?, ?), ?) AS M_MX_REF_JOB, convert(numeric(?, ?), ?) AS M_REF_DATA FROM TRN_HDR_DBF T LEFT JOIN STPFC_ENTRY_TABLE E ON T.M_CONTRACT = E.FC_ID LEFT JOIN TRN_EXT_DBF TE ON TE.M_TRADE_REF = T.M_NB LEFT JOIN UDF_VIEW_DBF TI ON TI.M_NB = TE.M_UDF_REF LEFT JOIN (SELECT DISTINCT E.FC_PACKAGE_ID AS PACKAGE_NB, E.FC_ORIGIN_ID AS CONTRACT_NB, E.FC_VERSION AS CONTRACT_VRSN, T.M_TRN_DATE AS TRADE_DATE, ? AS OP, T.M_COMMENT_BS AS B_S, E.FC_TYPOLOGY AS GRP, E.CTP_LABEL AS CTRP, E.FC_MATURITY_DATE AS EXPIRY, T.M_BRW_NOM1 AS NOMINELLT, T.M_BRW_NOMU1 AS VALUTA, E.FC_PORTFOLIO AS PORTFOLIO, CASE WHEN E.CHASING_TRIALS = ? THEN ? ELSE E.CHASING_TRIALS END AS CH, E.LAST_CHASING_DATE AS L_CH_D, E.JOB_DESCRIPTION AS LTM_STATUS, E.STP_LAST_VALIDATION_DATE AS COMPLETE_DATE, E.STP_LAST_VALIDATION_TIME AS COMPLETE_TIME, DATEPART(year, E.STP_LAST_VALIDATION_DATE) AS COMPLETE_DATE_YEAR, DATEPART(month, E.STP_LAST_VALIDATION_DATE) AS COMPLETE_DATE_MONTH, E.STP_LAST_VALIDATOR AS LST_HNDLR, E.STP_STATUS_VALIDATION_LEVEL AS STATUS_VALIDATION_LEVEL FROM TRN_HDR_DBF T LEFT JOIN STPFC_ENTRY_TABLE E ON T.M_CONTRACT = E.FC_ID LEFT JOIN TRN_EXT_DBF TE ON TE.M_TRADE_REF = T.M_NB LEFT JOIN UDF_VIEW_DBF TI ON TI.M_NB = TE.M_UDF_REF WHERE ? = ? AND TI.M_CTP_SCEN = ? AND TI.M_MODEVT_LAB NOT LIKE ? AND E.STP_STATUS_VALIDATION_LEVEL = ? AND E.JOB_DESCRIPTION IN (...) AND (TE.M_EVT_INTID <> ? OR (TE.M_EVT_INTID <> ? AND T.M_COUNTRPART IN (...))) UNION SELECT DISTINCT E.FC_PACKAGE_ID AS PACKAGE_NB, E.FC_ORIGIN_ID AS CONTRACT_NB, E.FC_VERSION AS CONTRACT_VRSN, E.EVT_DATE AS TRADE_DATE, ? AS OP, T.M_COMMENT_BS AS B_S, E.FC_TYPOLOGY AS GRP, E.CTP_LABEL AS CTRP, T.M_TRN_EXP AS EXPIRY, T.M_BRW_NOM1 AS NOMINELLT, T.M_BRW_NOMU1 AS VALUTA, E.FC_PORTFOLIO AS PORTFOLIO, CASE WHEN E.CHASING_TRIALS = ? THEN ? ELSE E.CHASING_TRIALS END AS CH, E.CHASING_DATE AS L_CH_D, E.JOB_DESCRIPTION AS LTM_STATUS, E.STP_LAST_VALIDATION_DATE AS COMPLETE_DATE, E.STP_LAST_VALIDATION_TIME AS COMPLETE_TIME, DATEPART(year, E.STP_LAST_VALIDATION_DATE) AS COMPLETE_DATE_YEAR, DATEPART(month, E.STP_LAST_VALIDATION_DATE) AS COMPLETE_DATE_MONTH, E.STP_LAST_VALIDATOR AS LST_HNDLR, E.STP_STATUS_VALIDATION_LEVEL AS STATUS_VALIDATION_LEVEL FROM TRN_HDR_DBF T LEFT JOIN STPEVT_ENTRY_TABLE E ON T.M_CONTRACT = E.FC_ID LEFT JOIN TRN_EXT_DBF TE ON TE.M_TRADE_REF = T.M_NB AND TE.M_EVT_INTID = ? LEFT JOIN UDF_VIEW_DBF TI ON TI.M_NB = TE.M_UDF_REF WHERE ? = ? AND TI.M_CTP_SCEN = ? AND E.STP_STATUS_VALIDATION_LEVEL = ? AND E.JOB_DESCRIPTION IN (...) UNION SELECT DISTINCT E.FC_PACKAGE_ID AS PACKAGE_NB, E.FC_ORIGIN_ID AS CONTRACT_NB, E.FC_VERSION AS CONTRACT_VRSN, TE.M_DATE AS TRADE_DATE, ? AS OP, T.M_COMMENT_BS AS B_S, E.FC_TYPOLOGY AS GRP, E.CTP_LABEL AS CTRP, E.FC_MATURITY_DATE AS EXPIRY, T.M_BRW_NOM1 AS NOMINELLT, T.M_BRW_NOMU1 AS VALUTA, E.FC_PORTFOLIO AS PORTFOLIO, CASE WHEN E.CHASING_TRIALS = ? THEN ? ELSE E.CHASING_TRIALS END AS CH, E.LAST_CHASING_DATE AS L_CH_D, E.JOB_DESCRIPTION AS LTM_STATUS, E.STP_LAST_VALIDATION_DATE AS COMPLETE_DATE, E.STP_LAST_VALIDATION_TIME AS COMPLETE_TIME, DATEPART(year, E.STP_LAST_VALIDATION_DATE) AS COMPLETE_DATE_YEAR, DATEPART(month, E.STP_LAST_VALIDATION_DATE) AS COMPLETE_DATE_MONTH, E.STP_LAST_VALIDATOR AS LST_HNDLR, E.STP_STATUS_VALIDATION_LEVEL AS STATUS_VALIDATION_LEVEL FROM TRN_HDR_DBF T LEFT JOIN STPFC_ENTRY_TABLE E ON T.M_CONTRACT = E.FC_ID LEFT JOIN TRN_EXT_DBF TE ON TE.M_TRADE_REF = T.M_NB AND TE.M_EVT_INTID = ? LEFT JOIN UDF_VIEW_DBF TI ON TI.M_NB = TE.M_UDF_REF WHERE ? = ? AND TI.M_CTP_SCEN = ? AND E.STP_STATUS_VALIDATION_LEVEL = ? AND E.JOB_DESCRIPTION IN (...) AND T.M_COUNTRPART NOT IN (...)) CR ON CR.CONTRACT_NB = E.FC_ORIGIN_ID WHERE ? = ? AND TI.M_CTP_SCEN = ? AND E.JOB_DESCRIPTION != ? AND E.STATUS_TAKEN = ?"));
		System.out.println("13: " + test("SELECT T1.TIMESTAMP, T1.M_IDENTITY, T1.M_REFERENCE, T1.M_LABEL, T1.M_TYPE FROM MATSET_DBF T1 ORDER BY T1.M_REFERENCE ASC"));
		System.out.println("14: " + test("SELECT T1.M_TARGET_DEAL, T1.M_LATEST_DEAL FROM CONTRACT_TRADE_MAPPING_DBF T1 WHERE (T1.M_ORIG_TRADE = ?)"));
		System.out.println("15: " + test("SELECT DISTINCT D.*, E.M_TREE_NAME AS M_PTREE, E.M_LVL_LABEL1 AS M_PTREE1, E.M_LVL_LABEL2 AS M_PTREE2, E.M_LVL_LABEL3 AS M_PTREE3, E.M_LVL_LABEL4 AS M_PTREE4, E.M_LVL_LABEL5 AS M_PTREE5, E.M_LVL_LABEL6 AS M_PTREE6, E.M_LVL_LABEL7 AS M_PTREE7, convert(numeric(?, ?), ?) AS M_MX_REF_JOB, convert(numeric(?, ?), ?) AS M_REF_DATA FROM (SELECT C.* FROM (SELECT A.M_REP_DATE, A.M_NB, A.M_CTP, A.M_CCY, A.M_DATE2, A.M_DATE3, A.M_DATE4, A.M_DATE5, A.M_TRNGRP, A.M_CNTID, A.M_PCKID, A.M_INST, A.M_PFLIO, A.M_STATUS, A.M_TRDTE, A.M_VALTYPE, SUM(A.M_ECNPL_T1) AS M_ECNPL_T1, SUM(A.M_ECNPL_T2) AS M_ECNPL_T2, SUM(A.M_DAILYPL) AS M_DAILYPL, SUM(A.M_ECNPLS_T1) AS M_ECNPLS_T1, SUM(A.M_ECNPLS_T2) AS M_ECNPLS_T2, SUM(A.M_SEK_DLYPL) AS M_SEK_DLYPL FROM (SELECT BO.M_REP_DATE AS M_REP_DATE, BO.M_NB AS M_NB, BO.M_TP_CNTRP AS M_CTP, BO.M_PL_INSCUR AS M_CCY, BO.M_DATE2 AS M_DATE2, BO.M_DATE3 AS M_DATE3, BO.M_DATE4 AS M_DATE4, BO.M_DATE5 AS M_DATE5, BO.M_TRN_GRP AS M_TRNGRP, BO.M_CNT_ORG AS M_CNTID, BO.M_PACKAGE AS M_PCKID, BO.M_INSTRUMENT AS M_INST, BO.M_TP_PFOLIO AS M_PFLIO, BO.M_TP_STATUS2 AS M_STATUS, BO.M_TP_DTETRN AS M_TRDTE, BO.M_ECN_PL1 AS M_ECNPL_T1, BO.M_ECN_PL2 AS M_ECNPL_T2, BO.M_ECN_PLSEK1 AS M_ECNPLS_T1, BO.M_ECN_PLSEK2 AS M_ECNPLS_T2, BO.M_ECN_PL2 - BO.M_ECN_PL1 AS M_DAILYPL, BO.M_ECN_PLSEK2 - BO.M_ECN_PLSEK1 AS M_SEK_DLYPL, CASE WHEN BO.M_ODV = ? THEN CASE WHEN (BO.M_ECN_PL2 - BO.M_ECN_PL1) = ? THEN ? ELSE ? END ELSE ? END AS M_VALTYPE FROM IB_PL_REP BO WHERE BO.M_NB NOT IN (...) AND BO.M_NB NOT IN (...)) A GROUP BY A.M_REP_DATE, A.M_NB, A.M_CTP, A.M_CCY, A.M_DATE2, A.M_DATE3, A.M_DATE4, A.M_DATE5, A.M_TRNGRP, A.M_CNTID, A.M_PCKID, A.M_INST, A.M_PFLIO, A.M_STATUS, A.M_TRDTE, A.M_VALTYPE UNION SELECT B.M_REP_DATE, B.M_NB, B.M_CTP, B.M_CCY, B.M_DATE2, B.M_DATE3, B.M_DATE4, B.M_DATE5, B.M_TRNGRP, B.M_CNTID, B.M_PCKID, B.M_INST, B.M_PFLIO, B.M_STATUS, B.M_TRDTE, B.M_VALTYPE, SUM(B.M_ECNPL_T1) AS M_ECNPL_T1, SUM(B.M_ECNPL_T2) AS M_ECNPL_T2, SUM(B.M_DAILYPL) AS M_DAILYPL, SUM(B.M_ECNPLS_T1) AS M_ECNPLS_T1, SUM(B.M_ECNPLS_T2) AS M_ECNPLS_T2, SUM(B.M_SEK_DLYPL) AS M_SEK_DLYPL FROM (SELECT RMV.M_REP_DATE AS M_REP_DATE, RMV.M_NB AS M_NB, RMV.M_TP_CNTRP AS M_CTP, RMV.M_PL_INSCUR AS M_CCY, RMV.M_DATE2 AS M_DATE2, RMV.M_DATE3 AS M_DATE3, RMV.M_DATE4 AS M_DATE4, RMV.M_DATE5 AS M_DATE5, RMV.M_TRN_GRP AS M_TRNGRP, RMV.M_CNT_ORG AS M_CNTID, RMV.M_PACKAGE AS M_PCKID, RMV.M_INSTRUMENT AS M_INST, RMV.M_TP_PFOLIO AS M_PFLIO, RMV.M_TP_STATUS2 AS M_STATUS, RMV.M_TP_DTETRN AS M_TRDTE, RMV.M_ECN_PL1 AS M_ECNPL_T1, RMV.M_ECN_PL2 AS M_ECNPL_T2, RMV.M_ECN_PLSEK1 AS M_ECNPLS_T1, RMV.M_ECN_PLSEK2 AS M_ECNPLS_T2, RMV.M_ECN_PL2 - RMV.M_ECN_PL1 AS M_DAILYPL, RMV.M_ECN_PLSEK2 - RMV.M_ECN_PLSEK1 AS M_SEK_DLYPL, ? AS M_VALTYPE FROM IB_PL_RMV_REP RMV WHERE RMV.M_NB != ?) B GROUP BY B.M_REP_DATE, B.M_NB, B.M_CTP, B.M_CCY, B.M_DATE2, B.M_DATE3, B.M_DATE4, B.M_DATE5, B.M_TRNGRP, B.M_CNTID, B.M_PCKID, B.M_INST, B.M_PFLIO, B.M_STATUS, B.M_TRDTE, B.M_VALTYPE UNION SELECT S.M_REP_DATE, S.M_NB, S.M_CTP, S.M_CCY, S.M_DATE2, S.M_DATE3, S.M_DATE4, S.M_DATE5, S.M_TRNGRP, S.M_CNTID, S.M_PCKID, S.M_INST, S.M_PFLIO, S.M_STATUS, S.M_TRDTE, S.M_VALTYPE, SUM(S.M_ECNPL_T1) AS M_ECNPL_T1, SUM(S.M_ECNPL_T2) AS M_ECNPL_T2, SUM(S.M_DAILYPL) AS M_DAILYPL, SUM(S.M_ECNPLS_T1) AS M_ECNPLS_T1, SUM(S.M_ECNPLS_T2) AS M_ECNPLS_T2, SUM(S.M_SEK_DLYPL) AS M_SEK_DLYPL FROM (SELECT RMV.M_REP_DATE AS M_REP_DATE, RMV.M_NB AS M_NB, RMV.M_TP_CNTRP AS M_CTP, RMV.M_PL_INSCUR AS M_CCY, RMV.M_DATE2 AS M_DATE2, RMV.M_DATE3 AS M_DATE3, RMV.M_DATE4 AS M_DATE4, RMV.M_DATE5 AS M_DATE5, RMV.M_TRN_GRP AS M_TRNGRP, RMV.M_CNT_ORG AS M_CNTID, RMV.M_PACKAGE AS M_PCKID, RMV.M_INSTRUMENT AS M_INST, RMV.M_TP_PFOLIO AS M_PFLIO, RMV.M_TP_STATUS2 AS M_STATUS, RMV.M_TP_DTETRN AS M_TRDTE, RMV.M_ECN_PL1 AS M_ECNPL_T1, RMV.M_ECN_PL2 AS M_ECNPL_T2, RMV.M_ECN_PLSEK1 AS M_ECNPLS_T1, RMV.M_ECN_PLSEK2 AS M_ECNPLS_T2, RMV.M_ECN_PL2 - RMV.M_ECN_PL1 AS M_DAILYPL, RMV.M_ECN_PLSEK2 - RMV.M_ECN_PLSEK1 AS M_SEK_DLYPL, ? AS M_VALTYPE FROM IB_PL_STR_RMV_REP RMV WHERE RMV.M_NB != ?) S GROUP BY S.M_REP_DATE, S.M_NB, S.M_CTP, S.M_CCY, S.M_DATE2, S.M_DATE3, S.M_DATE4, S.M_DATE5, S.M_TRNGRP, S.M_CNTID, S.M_PCKID, S.M_INST, S.M_PFLIO, S.M_STATUS, S.M_TRDTE, S.M_VALTYPE) C GROUP BY C.M_REP_DATE, C.M_NB, C.M_CTP, C.M_CCY, C.M_DATE2, C.M_DATE3, C.M_DATE4, C.M_DATE5, C.M_TRNGRP, C.M_CNTID, C.M_PCKID, C.M_INST, C.M_PFLIO, C.M_STATUS, C.M_TRDTE, C.M_VALTYPE) D LEFT JOIN (SELECT * FROM ETA_PFOLIO_TREE_REP WHERE M_TREE_NAME = ? AND M_MX_REF_JOB = (SELECT MAX(M_MX_REF_JOB) FROM ETA_PFOLIO_TREE_REP)) E ON D.M_PFLIO = E.M_LVL_LABEL5"));

		System.out.println("D-0: " + test("--not-found--"));
		System.out.println("D-1: " + test("select T1.TIMESTAMP, T1.M_IDENTITY, T1.M_LABEL, T1.M__INDEX_, T1.M__LINK_, T1.M_COL0, T1.M_COL1, T1.M_COL2, T1.M_COL3, T1.M_COL4, T1.M_COL5, T1.M_COL6, T1.M_COL7, T1.M_COL8, T1.M_COL9, T1.M_COL10, T1.M_COL11, T1.M_COL12, T1.M_COL13, T1.M_COL14, T1.M_COL15, T1.M_COL16, T1.M_COL17, T1.M_COL18, T1.M_COL19, T1.M_COL20, T1.M_COL21, T1.M_COL22, T1.M_COL23, T1.M_COL24, T1.M_COL25, T1.M_COL26, T1.M_COL27, T1.M__DATE_, T1.M__REPLICAT_, T1.M_PERMUTID, T1.M_DSP_WDTH from MPX_ASGRC_DBF T1 where ((T1.M_LABEL=@@@V0_VCHAR1) and (T1.M__DATE_=@@@V1_VCHAR1))(@@@V0_VCHAR1 VARCHAR(64), @@@V1_VCHAR1 VARCHAR(64))"));
		System.out.println("D-2: " + test("delete MPAUD_BD_DBF where MPAUD_BD_DBF.M_LINK IN ( select MPAUD_HD_DBF.M_LINK from MPAUD_HD_DBF where MPAUD_HD_DBF.M_PARAMDATE = @@@V0_VCHAR1 )(@@@V0_VCHAR1 VARCHAR(64))"));
		System.out.println("D-3: " + test("select T1.M_TARGET_DEAL, T1.M_LATEST_DEAL from CONTRACT_TRADE_MAPPING_DBF T1 where (T1.M_TARGET_DEAL = @@@V0_NUME199)(@@@V0_NUME199 NUMERIC(19, 9))"));
		System.out.println("D-4: " + test("select T1.M__DATE_, T1.M__ALIAS_, T1.M_REFERENCE, T1.M_SETNAME, T1.M_US_TYPE, T1.M_TYPE,T2.M__DATE_, T2.M__ALIAS_, T2.M_REFERENCE, T2.M_MATCODE from MPX_VOLMAT_DBF T1 ,MPY_VOLMAT_DBF T2 where T1.M_REFERENCE *= T2.M_REFERENCE and T1.M__DATE_ *= T2.M__DATE_ and T1.M__ALIAS_ *= T2.M__ALIAS_ and ((((T1.M__DATE_=@@@V0_VCHAR1) and (T1.M__ALIAS_=@@@V1_VCHAR1)) and ((T1.M_SETNAME = @@@V2_VCHAR1 and T1.M_US_TYPE = @@@V3_INT) and T1.M_TYPE = @@@V4_INT)) and ((T2.M__DATE_=@@@V5_VCHAR1) and (T2.M__ALIAS_=@@@V6_VCHAR1))) order by T1.M_SETNAME asc ,T1.M_US_TYPE asc ,T1.M_TYPE asc(@@@V0_VCHAR1 VARCHAR(64), @@@V1_VCHAR1 VARCHAR(64), @@@V2_VCHAR1 VARCHAR(64), @@@V3_INT INT, @@@V4_INT INT, @@@V5_VCHAR1 VARCHAR(64), @@@V6_VCHAR1 VARCHAR(64))"));
		System.out.println("D-5: " + test("select distinct T1.M_INSTRUM, T1.M_MARKET, T1.M_TYPE, T1.M_MDIREF, T1.M_MATCOD from MPX_VOL_DBF T1 where T1.M_INSTRUM in (@@@V0_VCHAR1,@@@V1_VCHAR1,@@@V2_VCHAR1,@@@V3_VCHAR1,@@@V4_VCHAR1,@@@V5_VCHAR1,@@@V6_VCHAR1,@@@V7_VCHAR1,@@@V8_VCHAR1,@@@V9_VCHAR1,@@@V10_VCHAR1,@@@V11_VCHAR1,@@@V12_VCHAR1,@@@V13_VCHAR1,@@@V14_VCHAR1,@@@V15_VCHAR1,@@@V16_VCHAR1,@@@V17_VCHAR1,@@@V18_VCHAR1,@@@V19_VCHAR1,@@@V20_VCHAR1,@@@V21_VCHAR1,@@@V22_VCHAR1,@@@V23_VCHAR1,@@@V24_VCHAR1,@@@V25_VCHAR1,@@@V26_VCHAR1,@@@V27_VCHAR1,@@@V28_VCHAR1,@@@V29_VCHAR1,@@@V30_VCHAR1,@@@V31_VCHAR1,@@@V32_VCHAR1,@@@V33_VCHAR1,@@@V34_VCHAR1,@@@V35_VCHAR1,@@@V36_VCHAR1,@@@V37_VCHAR1,@@@V38_VCHAR1,@@@V39_VCHAR1,@@@V40_VCHAR1,@@@V41_VCHAR1,@@@V42_VCHAR1,@@@V43_VCHAR1,@@@V44_VCHAR1,@@@V45_VCHAR1,@@@V46_VCHAR1,@@@V47_VCHAR1,@@@V48_VCHAR1,@@@V49_VCHAR1,@@@V50_VCHAR1,@@@V51_VCHAR1,@@@V52_VCHAR1,@@@V53_VCHAR1,@@@V54_VCHAR1,@@@V55_VCHAR1,@@@V56_VCHAR1,@@@V57_VCHAR1,@@@V58_VCHAR1,@@@V59_VCHAR1)(@@@V59_VCHAR1 VARCHAR(64), @@@V58_VCHAR1 VARCHAR(64), @@@V57_VCHAR1 VARCHAR(64), @@@V56_VCHAR1 VARCHAR(64), @@@V55_VCHAR1 VARCHAR(64), @@@V54_VCHAR1 VARCHAR(64), @@@V53_VCHAR1 VARCHAR(64), @@@V52_VCHAR1 VARCHAR(64), @@@V51_VCHAR1 VARCHAR(64), @@@V50_VCHAR1 VARCHAR(64), @@@V49_VCHAR1 VARCHAR(64), @@@V48_VCHAR1 VARCHAR(64), @@@V47_VCHAR1 VARCHAR(64), @@@V46_VCHAR1 VARCHAR(64), @@@V45_VCHAR1 VARCHAR(64), @@@V44_VCHAR1 VARCHAR(64), @@@V43_VCHAR1 VARCHAR(64), @@@V42_VCHAR1 VARCHAR(64), @@@V41_VCHAR1 VARCHAR(64), @@@V40_VCHAR1 VARCHAR(64), @@@V39_VCHAR1 VARCHAR(64), @@@V38_VCHAR1 VARCHAR(64), @@@V37_VCHAR1 VARCHAR(64), @@@V36_VCHAR1 VARCHAR(64), @@@V35_VCHAR1 VARCHAR(64), @@@V34_VCHAR1 VARCHAR(64), @@@V33_VCHAR1 VARCHAR(64), @@@V32_VCHAR1 VARCHAR(64), @@@V31_VCHAR1 VARCHAR(64), @@@V30_VCHAR1 VARCHAR(64), @@@V29_VCHAR1 VARCHAR(64), @@@V28_VCHAR1 VARCHAR(64), @@@V27_VCHAR1 VARCHAR(64), @@@V26_VCHAR1 VARCHAR(64), @@@V25_VCHAR1 VARCHAR(64), @@@V24_VCHAR1 VARCHAR(64), @@@V23_VCHAR1 VARCHAR(64), @@@V22_VCHAR1 VARCHAR(64), @@@V21_VCHAR1 VARCHAR(64), @@@V20_VCHAR1 VARCHAR(64), @@@V19_VCHAR1 VARCHAR(64), @@@V18_VCHAR1 VARCHAR(64), @@@V17_VCHAR1 VARCHAR(64), @@@V16_VCHAR1 VARCHAR(64), @@@V15_VCHAR1 VARCHAR(64), @@@V14_VCHAR1 VARCHAR(64), @@@V13_VCHAR1 VARCHAR(64), @@@V12_VCHAR1 VARCHAR(64), @@@V11_VCHAR1 VARCHAR(64), @@@V10_VCHAR1 VARCHAR(64), @@@V9_VCHAR1 VARCHAR(64), @@@V8_VCHAR1 VARCHAR(64), @@@V7_VCHAR1 VARCHAR(64), @@@V6_VCHAR1 VARCHAR(64), @@@V5_VCHAR1 VARCHAR(64), @@@V4_VCHAR1 VARCHAR(64), @@@V3_VCHAR1 VARCHAR(64), @@@V2_VCHAR1 VARCHAR(64), @@@V1_VCHAR1 VARCHAR(64), @@@V0_VCHAR1 VARCHAR(64))"));
		System.out.println("D-6: " + test("SELECT jobname from MxGJobStatus where meid=@@@V0_VCHAR1(@@@V0_VCHAR1 VARCHAR(64))"));
		System.out.println("D-7: " + test("select meid from MxGJobStatus where pid=@@@V0_INT and soj>=convert(datetime,@@@V1_VCHAR1,@@@V2_INT) and eoj is null(@@@V0_INT INT, @@@V2_INT INT, @@@V1_VCHAR1 VARCHAR(64))"));
		System.out.println("D-8: " + test("select T1.TIMESTAMP, T1.M_IDENTITY, T1.M_REFERENCE, T1.M_LABEL from MATSET_DBF T1 order by T1.M_REFERENCE asc"));
		System.out.println("D-9: " + test("select distinct T1.M_INSTRUM, T1.M_MARKET, T1.M_TYPE, T1.M_MDIREF, T1.M_MATCOD from MPX_VOL_DBF T1 where T1.M_INSTRUM in (@@@V0_VCHAR1,@@@V1_VCHAR1,@@@V2_VCHAR1,@@@V3_VCHAR1,@@@V4_VCHAR1,@@@V5_VCHAR1,@@@V6_VCHAR1,@@@V7_VCHAR1,@@@V8_VCHAR1,@@@V9_VCHAR1,@@@V10_VCHAR1,@@@V11_VCHAR1,@@@V12_VCHAR1,@@@V13_VCHAR1,@@@V14_VCHAR1,@@@V15_VCHAR1,@@@V16_VCHAR1,@@@V17_VCHAR1,@@@V18_VCHAR1,@@@V19_VCHAR1,@@@V20_VCHAR1,@@@V21_VCHAR1,@@@V22_VCHAR1,@@@V23_VCHAR1,@@@V24_VCHAR1,@@@V25_VCHAR1,@@@V26_VCHAR1,@@@V27_VCHAR1,@@@V28_VCHAR1,@@@V29_VCHAR1,@@@V30_VCHAR1,@@@V31_VCHAR1,@@@V32_VCHAR1,@@@V33_VCHAR1,@@@V34_VCHAR1,@@@V35_VCHAR1,@@@V36_VCHAR1,@@@V37_VCHAR1,@@@V38_VCHAR1,@@@V39_VCHAR1,@@@V40_VCHAR1,@@@V41_VCHAR1,@@@V42_VCHAR1,@@@V43_VCHAR1,@@@V44_VCHAR1,@@@V45_VCHAR1,@@@V46_VCHAR1,@@@V47_VCHAR1,@@@V48_VCHAR1,@@@V49_VCHAR1,@@@V50_VCHAR1,@@@V51_VCHAR1,@@@V52_VCHAR1,@@@V53_VCHAR1,@@@V54_VCHAR1,@@@V55_VCHAR1,@@@V56_VCHAR1,@@@V57_VCHAR1,@@@V58_VCHAR1,@@@V59_VCHAR1,@@@V60_VCHAR1,@@@V61_VCHAR1,@@@V62_VCHAR1,@@@V63_VCHAR1,@@@V64_VCHAR1,@@@V65_VCHAR1,@@@V66_VCHAR1,@@@V67_VCHAR1,@@@V68_VCHAR1,@@@V69_VCHAR1,@@@V70_VCHAR1,@@@V71_VCHAR1,@@@V72_VCHAR1,@@@V73_VCHAR1,@@@V74_VCHAR1,@@@V75_VCHAR1,@@@V76_VCHAR1,@@@V77_VCHAR1,@@@V78_VCHAR1,@@@V79_VCHAR1,@@@V80_VCHAR1,@@@V81_VCHAR1,@@@V82_VCHAR1,@@@V83_VCHAR1,@@@V84_VCHAR1,@@@V85_VCHAR1,@@@V86_VCHAR1,@@@V87_VCHAR1,@@@V88_VCHAR1,@@@V89_VCHAR1,@@@V90_VCHAR1,@@@V91_VCHAR1,@@@V92_VCHAR1,@@@V93_VCHAR1,@@@V94_VCHAR1,@@@V95_VCHAR1,@@@V96_VCHAR1,@@@V97_VCHAR1,@@@V98_VCHAR1,@@@V99_VCHAR1,@@@V100_VCHAR1,@@@V101_VCHAR1,@@@V102_VCHAR1,@@@V103_VCHAR1,@@@V104_VCHAR1)(@@@V104_VCHAR1 VARCHAR(64), @@@V103_VCHAR1 VARCHAR(64), @@@V102_VCHAR1 VARCHAR(64), @@@V101_VCHAR1 VARCHAR(64), @@@V100_VCHAR1 VARCHAR(64), @@@V99_VCHAR1 VARCHAR(64), @@@V98_VCHAR1 VARCHAR(64), @@@V97_VCHAR1 VARCHAR(64), @@@V96_VCHAR1 VARCHAR(64), @@@V95_VCHAR1 VARCHAR(64), @@@V94_VCHAR1 VARCHAR(64), @@@V93_VCHAR1 VARCHAR(64), @@@V92_VCHAR1 VARCHAR(64), @@@V91_VCHAR1 VARCHAR(64), @@@V90_VCHAR1 VARCHAR(64), @@@V89_VCHAR1 VARCHAR(64), @@@V88_VCHAR1 VARCHAR(64), @@@V87_VCHAR1 VARCHAR(64), @@@V86_VCHAR1 VARCHAR(64), @@@V85_VCHAR1 VARCHAR(64), @@@V84_VCHAR1 VARCHAR(64), @@@V83_VCHAR1 VARCHAR(64), @@@V82_VCHAR1 VARCHAR(64), @@@V81_VCHAR1 VARCHAR(64), @@@V80_VCHAR1 VARCHAR(64), @@@V79_VCHAR1 VARCHAR(64), @@@V78_VCHAR1 VARCHAR(64), @@@V77_VCHAR1 VARCHAR(64), @@@V76_VCHAR1 VARCHAR(64), @@@V75_VCHAR1 VARCHAR(64), @@@V74_VCHAR1 VARCHAR(64), @@@V73_VCHAR1 VARCHAR(64), @@@V72_VCHAR1 VARCHAR(64), @@@V71_VCHAR1 VARCHAR(64), @@@V70_VCHAR1 VARCHAR(64), @@@V69_VCHAR1 VARCHAR(64), @@@V68_VCHAR1 VARCHAR(64), @@@V67_VCHAR1 VARCHAR(64), @@@V66_VCHAR1 VARCHAR(64), @@@V65_VCHAR1 VARCHAR(64), @@@V64_VCHAR1 VARCHAR(64), @@@V63_VCHAR1 VARCHAR(64), @@@V62_VCHAR1 VARCHAR(64), @@@V61_VCHAR1 VARCHAR(64), @@@V60_VCHAR1 VARCHAR(64), @@@V59_VCHAR1 VARCHAR(64), @@@V58_VCHAR1 VARCHAR(64), @@@V57_VCHAR1 VARCHAR(64), @@@V56_VCHAR1 VARCHAR(64), @@@V55_VCHAR1 VARCHAR(64), @@@V54_VCHAR1 VARCHAR(64), @@@V53_VCHAR1 VARCHAR(64), @@@V52_VCHAR1 VARCHAR(64), @@@V51_VCHAR1 VARCHAR(64), @@@V50_VCHAR1 VARCHAR(64), @@@V49_VCHAR1 VARCHAR(64), @@@V48_VCHAR1 VARCHAR(64), @@@V47_VCHAR1 VARCHAR(64), @@@V46_VCHAR1 VARCHAR(64), @@@V45_VCHAR1 VARCHAR(64), @@@V44_VCHAR1 VARCHAR(64), @@@V43_VCHAR1 VARCHAR(64), @@@V42_VCHAR1 VARCHAR(64), @@@V41_VCHAR1 VARCHAR(64), @@@V40_VCHAR1 VARCHAR(64), @@@V39_VCHAR1 VARCHAR(64), @@@V38_VCHAR1 VARCHAR(64), @@@V37_VCHAR1 VARCHAR(64), @@@V36_VCHAR1 VARCHAR(64), @@@V35_VCHAR1 VARCHAR(64), @@@V34_VCHAR1 VARCHAR(64), @@@V33_VCHAR1 VARCHAR(64), @@@V32_VCHAR1 VARCHAR(64), @@@V31_VCHAR1 VARCHAR(64), @@@V30_VCHAR1 VARCHAR(64), @@@V29_VCHAR1 VARCHAR(64), @@@V28_VCHAR1 VARCHAR(64), @@@V27_VCHAR1 VARCHAR(64), @@@V26_VCHAR1 VARCHAR(64), @@@V25_VCHAR1 VARCHAR(64), @@@V24_VCHAR1 VARCHAR(64), @@@V23_VCHAR1 VARCHAR(64), @@@V22_VCHAR1 VARCHAR(64), @@@V21_VCHAR1 VARCHAR(64), @@@V20_VCHAR1 VARCHAR(64), @@@V19_VCHAR1 VARCHAR(64), @@@V18_VCHAR1 VARCHAR(64), @@@V17_VCHAR1 VARCHAR(64), @@@V16_VCHAR1 VARCHAR(64), @@@V15_VCHAR1 VARCHAR(64), @@@V14_VCHAR1 VARCHAR(64), @@@V13_VCHAR1 VARCHAR(64), @@@V12_VCHAR1 VARCHAR(64), @@@V11_VCHAR1 VARCHAR(64), @@@V10_VCHAR1 VARCHAR(64), @@@V9_VCHAR1 VARCHAR(64), @@@V8_VCHAR1 VARCHAR(64), @@@V7_VCHAR1 VARCHAR(64), @@@V6_VCHAR1 VARCHAR(64), @@@V5_VCHAR1 VARCHAR(64), @@@V4_VCHAR1 VARCHAR(64), @@@V3_VCHAR1 VARCHAR(64), @@@V2_VCHAR1 VARCHAR(64), @@@V1_VCHAR1 VARCHAR(64), @@@V0_VCHAR1 VARCHAR(64))"));
		System.out.println("D-10: " + test("select distinct T1.M_INSTRUM, T1.M_MARKET, T1.M_TYPE, T1.M_MDIREF, T1.M_MATCOD from MPX_VOL_DBF T1 where T1.M_INSTRUM in (@@@V0_VCHAR1,@@@V1_VCHAR1,@@@V2_VCHAR1,@@@V3_VCHAR1,@@@V4_VCHAR1,@@@V5_VCHAR1,@@@V6_VCHAR1,@@@V7_VCHAR1,@@@V8_VCHAR1,@@@V9_VCHAR1,@@@V10_VCHAR1,@@@V11_VCHAR1,@@@V12_VCHAR1,@@@V13_VCHAR1,@@@V14_VCHAR1,@@@V15_VCHAR1,@@@V16_VCHAR1,@@@V17_VCHAR1,@@@V18_VCHAR1,@@@V19_VCHAR1,@@@V20_VCHAR1,@@@V21_VCHAR1,@@@V22_VCHAR1,@@@V23_VCHAR1,@@@V24_VCHAR1,@@@V25_VCHAR1,@@@V26_VCHAR1,@@@V27_VCHAR1,@@@V28_VCHAR1,@@@V29_VCHAR1,@@@V30_VCHAR1,@@@V31_VCHAR1,@@@V32_VCHAR1,@@@V33_VCHAR1,@@@V34_VCHAR1,@@@V35_VCHAR1,@@@V36_VCHAR1,@@@V37_VCHAR1,@@@V38_VCHAR1,@@@V39_VCHAR1,@@@V40_VCHAR1,@@@V41_VCHAR1,@@@V42_VCHAR1,@@@V43_VCHAR1,@@@V44_VCHAR1)(@@@V44_VCHAR1 VARCHAR(64), @@@V43_VCHAR1 VARCHAR(64), @@@V42_VCHAR1 VARCHAR(64), @@@V41_VCHAR1 VARCHAR(64), @@@V40_VCHAR1 VARCHAR(64), @@@V39_VCHAR1 VARCHAR(64), @@@V38_VCHAR1 VARCHAR(64), @@@V37_VCHAR1 VARCHAR(64), @@@V36_VCHAR1 VARCHAR(64), @@@V35_VCHAR1 VARCHAR(64), @@@V34_VCHAR1 VARCHAR(64), @@@V33_VCHAR1 VARCHAR(64), @@@V32_VCHAR1 VARCHAR(64), @@@V31_VCHAR1 VARCHAR(64), @@@V30_VCHAR1 VARCHAR(64), @@@V29_VCHAR1 VARCHAR(64), @@@V28_VCHAR1 VARCHAR(64), @@@V27_VCHAR1 VARCHAR(64), @@@V26_VCHAR1 VARCHAR(64), @@@V25_VCHAR1 VARCHAR(64), @@@V24_VCHAR1 VARCHAR(64), @@@V23_VCHAR1 VARCHAR(64), @@@V22_VCHAR1 VARCHAR(64), @@@V21_VCHAR1 VARCHAR(64), @@@V20_VCHAR1 VARCHAR(64), @@@V19_VCHAR1 VARCHAR(64), @@@V18_VCHAR1 VARCHAR(64), @@@V17_VCHAR1 VARCHAR(64), @@@V16_VCHAR1 VARCHAR(64), @@@V15_VCHAR1 VARCHAR(64), @@@V14_VCHAR1 VARCHAR(64), @@@V13_VCHAR1 VARCHAR(64), @@@V12_VCHAR1 VARCHAR(64), @@@V11_VCHAR1 VARCHAR(64), @@@V10_VCHAR1 VARCHAR(64), @@@V9_VCHAR1 VARCHAR(64), @@@V8_VCHAR1 VARCHAR(64), @@@V7_VCHAR1 VARCHAR(64), @@@V6_VCHAR1 VARCHAR(64), @@@V5_VCHAR1 VARCHAR(64), @@@V4_VCHAR1 VARCHAR(64), @@@V3_VCHAR1 VARCHAR(64), @@@V2_VCHAR1 VARCHAR(64), @@@V1_VCHAR1 VARCHAR(64), @@@V0_VCHAR1 VARCHAR(64))"));
		System.out.println("D-11: " + test("select distinct T1.M_INSTRUM, T1.M_MARKET, T1.M_TYPE, T1.M_MDIREF, T1.M_MATCOD from MPX_VOL_DBF T1 where T1.M_INSTRUM in (@@@V0_VCHAR1,@@@V1_VCHAR1,@@@V2_VCHAR1,@@@V3_VCHAR1,@@@V4_VCHAR1,@@@V5_VCHAR1,@@@V6_VCHAR1,@@@V7_VCHAR1,@@@V8_VCHAR1,@@@V9_VCHAR1,@@@V10_VCHAR1,@@@V11_VCHAR1,@@@V12_VCHAR1,@@@V13_VCHAR1,@@@V14_VCHAR1,@@@V15_VCHAR1,@@@V16_VCHAR1,@@@V17_VCHAR1,@@@V18_VCHAR1,@@@V19_VCHAR1,@@@V20_VCHAR1,@@@V21_VCHAR1,@@@V22_VCHAR1,@@@V23_VCHAR1,@@@V24_VCHAR1,@@@V25_VCHAR1,@@@V26_VCHAR1,@@@V27_VCHAR1,@@@V28_VCHAR1,@@@V29_VCHAR1)(@@@V29_VCHAR1 VARCHAR(64), @@@V28_VCHAR1 VARCHAR(64), @@@V27_VCHAR1 VARCHAR(64), @@@V26_VCHAR1 VARCHAR(64), @@@V25_VCHAR1 VARCHAR(64), @@@V24_VCHAR1 VARCHAR(64), @@@V23_VCHAR1 VARCHAR(64), @@@V22_VCHAR1 VARCHAR(64), @@@V21_VCHAR1 VARCHAR(64), @@@V20_VCHAR1 VARCHAR(64), @@@V19_VCHAR1 VARCHAR(64), @@@V18_VCHAR1 VARCHAR(64), @@@V17_VCHAR1 VARCHAR(64), @@@V16_VCHAR1 VARCHAR(64), @@@V15_VCHAR1 VARCHAR(64), @@@V14_VCHAR1 VARCHAR(64), @@@V13_VCHAR1 VARCHAR(64), @@@V12_VCHAR1 VARCHAR(64), @@@V11_VCHAR1 VARCHAR(64), @@@V10_VCHAR1 VARCHAR(64), @@@V9_VCHAR1 VARCHAR(64), @@@V8_VCHAR1 VARCHAR(64), @@@V7_VCHAR1 VARCHAR(64), @@@V6_VCHAR1 VARCHAR(64), @@@V5_VCHAR1 VARCHAR(64), @@@V4_VCHAR1 VARCHAR(64), @@@V3_VCHAR1 VARCHAR(64), @@@V2_VCHAR1 VARCHAR(64), @@@V1_VCHAR1 VARCHAR(64), @@@V0_VCHAR1 VARCHAR(64))"));
		System.out.println("D-12: " + test("select T1.M__INDEX_, T1.M__REPLICAT_, T1.M_CURRENCY, T1.M_FAMILY, T1.M_GROUP, T1.M_PRC_CTP, T1.M_PROC_A, T1.M_TYPE, T1.M_TYPOLOGY, T1.M_USAGE, T1.M_REFERENCE, T1.M_CHLD_PRMT from CAC_RTGB_DBF T1 where T1.M__INDEX_ = @@@V0_INT(@@@V0_INT INT)"));
		System.out.println("D-13: " + test("select T1.M__DATE_, T1.M__ALIAS_, T1.M_REFERENCE, T1.M_SETNAME, T1.M_US_TYPE, T1.M_TYPE,T2.M__DATE_, T2.M__ALIAS_, T2.M_REFERENCE, T2.M_MATCODE from MPX_VOLMAT_DBF T1 ,MPY_VOLMAT_DBF T2 where T1.M_REFERENCE *= T2.M_REFERENCE and T1.M__DATE_ *= T2.M__DATE_ and T1.M__ALIAS_ *= T2.M__ALIAS_ and ((((T1.M__DATE_=@@@V0_VCHAR1) and (T1.M__ALIAS_=@@@V1_VCHAR1)) and ((T1.M_SETNAME in (@@@V2_VCHAR1,@@@V3_VCHAR1,@@@V4_VCHAR1) and T1.M_US_TYPE = @@@V5_INT) and T1.M_TYPE = @@@V6_INT)) and ((T2.M__DATE_=@@@V7_VCHAR1) and (T2.M__ALIAS_=@@@V8_VCHAR1))) order by T1.M_SETNAME asc ,T1.M_US_TYPE asc ,T1.M_TYPE asc(@@@V0_VCHAR1 VARCHAR(64), @@@V1_VCHAR1 VARCHAR(64), @@@V4_VCHAR1 VARCHAR(64), @@@V3_VCHAR1 VARCHAR(64), @@@V2_VCHAR1 VARCHAR(64), @@@V5_INT INT, @@@V6_INT INT, @@@V7_VCHAR1 VARCHAR(64), @@@V8_VCHAR1 VARCHAR(64))"));
		System.out.println("D-14: " + test("select distinct T1.M_INSTRUM, T1.M_MARKET, T1.M_TYPE, T1.M_MDIREF, T1.M_MATCOD from MPX_VOL_DBF T1 where T1.M_INSTRUM in (@@@V0_VCHAR1,@@@V1_VCHAR1,@@@V2_VCHAR1,@@@V3_VCHAR1,@@@V4_VCHAR1,@@@V5_VCHAR1,@@@V6_VCHAR1,@@@V7_VCHAR1,@@@V8_VCHAR1,@@@V9_VCHAR1,@@@V10_VCHAR1,@@@V11_VCHAR1,@@@V12_VCHAR1,@@@V13_VCHAR1,@@@V14_VCHAR1,@@@V15_VCHAR1,@@@V16_VCHAR1,@@@V17_VCHAR1,@@@V18_VCHAR1,@@@V19_VCHAR1,@@@V20_VCHAR1,@@@V21_VCHAR1,@@@V22_VCHAR1,@@@V23_VCHAR1,@@@V24_VCHAR1,@@@V25_VCHAR1,@@@V26_VCHAR1,@@@V27_VCHAR1,@@@V28_VCHAR1,@@@V29_VCHAR1,@@@V30_VCHAR1,@@@V31_VCHAR1,@@@V32_VCHAR1,@@@V33_VCHAR1,@@@V34_VCHAR1,@@@V35_VCHAR1,@@@V36_VCHAR1,@@@V37_VCHAR1,@@@V38_VCHAR1,@@@V39_VCHAR1,@@@V40_VCHAR1,@@@V41_VCHAR1,@@@V42_VCHAR1,@@@V43_VCHAR1,@@@V44_VCHAR1,@@@V45_VCHAR1,@@@V46_VCHAR1,@@@V47_VCHAR1,@@@V48_VCHAR1,@@@V49_VCHAR1,@@@V50_VCHAR1,@@@V51_VCHAR1,@@@V52_VCHAR1,@@@V53_VCHAR1,@@@V54_VCHAR1,@@@V55_VCHAR1,@@@V56_VCHAR1,@@@V57_VCHAR1,@@@V58_VCHAR1,@@@V59_VCHAR1,@@@V60_VCHAR1,@@@V61_VCHAR1,@@@V62_VCHAR1,@@@V63_VCHAR1,@@@V64_VCHAR1,@@@V65_VCHAR1,@@@V66_VCHAR1,@@@V67_VCHAR1,@@@V68_VCHAR1,@@@V69_VCHAR1,@@@V70_VCHAR1,@@@V71_VCHAR1,@@@V72_VCHAR1,@@@V73_VCHAR1,@@@V74_VCHAR1,@@@V75_VCHAR1,@@@V76_VCHAR1,@@@V77_VCHAR1,@@@V78_VCHAR1,@@@V79_VCHAR1,@@@V80_VCHAR1,@@@V81_VCHAR1,@@@V82_VCHAR1,@@@V83_VCHAR1,@@@V84_VCHAR1,@@@V85_VCHAR1,@@@V86_VCHAR1,@@@V87_VCHAR1,@@@V88_VCHAR1,@@@V89_VCHAR1)(@@@V89_VCHAR1 VARCHAR(64), @@@V88_VCHAR1 VARCHAR(64), @@@V87_VCHAR1 VARCHAR(64), @@@V86_VCHAR1 VARCHAR(64), @@@V85_VCHAR1 VARCHAR(64), @@@V84_VCHAR1 VARCHAR(64), @@@V83_VCHAR1 VARCHAR(64), @@@V82_VCHAR1 VARCHAR(64), @@@V81_VCHAR1 VARCHAR(64), @@@V80_VCHAR1 VARCHAR(64), @@@V79_VCHAR1 VARCHAR(64), @@@V78_VCHAR1 VARCHAR(64), @@@V77_VCHAR1 VARCHAR(64), @@@V76_VCHAR1 VARCHAR(64), @@@V75_VCHAR1 VARCHAR(64), @@@V74_VCHAR1 VARCHAR(64), @@@V73_VCHAR1 VARCHAR(64), @@@V72_VCHAR1 VARCHAR(64), @@@V71_VCHAR1 VARCHAR(64), @@@V70_VCHAR1 VARCHAR(64), @@@V69_VCHAR1 VARCHAR(64), @@@V68_VCHAR1 VARCHAR(64), @@@V67_VCHAR1 VARCHAR(64), @@@V66_VCHAR1 VARCHAR(64), @@@V65_VCHAR1 VARCHAR(64), @@@V64_VCHAR1 VARCHAR(64), @@@V63_VCHAR1 VARCHAR(64), @@@V62_VCHAR1 VARCHAR(64), @@@V61_VCHAR1 VARCHAR(64), @@@V60_VCHAR1 VARCHAR(64), @@@V59_VCHAR1 VARCHAR(64), @@@V58_VCHAR1 VARCHAR(64), @@@V57_VCHAR1 VARCHAR(64), @@@V56_VCHAR1 VARCHAR(64), @@@V55_VCHAR1 VARCHAR(64), @@@V54_VCHAR1 VARCHAR(64), @@@V53_VCHAR1 VARCHAR(64), @@@V52_VCHAR1 VARCHAR(64), @@@V51_VCHAR1 VARCHAR(64), @@@V50_VCHAR1 VARCHAR(64), @@@V49_VCHAR1 VARCHAR(64), @@@V48_VCHAR1 VARCHAR(64), @@@V47_VCHAR1 VARCHAR(64), @@@V46_VCHAR1 VARCHAR(64), @@@V45_VCHAR1 VARCHAR(64), @@@V44_VCHAR1 VARCHAR(64), @@@V43_VCHAR1 VARCHAR(64), @@@V42_VCHAR1 VARCHAR(64), @@@V41_VCHAR1 VARCHAR(64), @@@V40_VCHAR1 VARCHAR(64), @@@V39_VCHAR1 VARCHAR(64), @@@V38_VCHAR1 VARCHAR(64), @@@V37_VCHAR1 VARCHAR(64), @@@V36_VCHAR1 VARCHAR(64), @@@V35_VCHAR1 VARCHAR(64), @@@V34_VCHAR1 VARCHAR(64), @@@V33_VCHAR1 VARCHAR(64), @@@V32_VCHAR1 VARCHAR(64), @@@V31_VCHAR1 VARCHAR(64), @@@V30_VCHAR1 VARCHAR(64), @@@V29_VCHAR1 VARCHAR(64), @@@V28_VCHAR1 VARCHAR(64), @@@V27_VCHAR1 VARCHAR(64), @@@V26_VCHAR1 VARCHAR(64), @@@V25_VCHAR1 VARCHAR(64), @@@V24_VCHAR1 VARCHAR(64), @@@V23_VCHAR1 VARCHAR(64), @@@V22_VCHAR1 VARCHAR(64), @@@V21_VCHAR1 VARCHAR(64), @@@V20_VCHAR1 VARCHAR(64), @@@V19_VCHAR1 VARCHAR(64), @@@V18_VCHAR1 VARCHAR(64), @@@V17_VCHAR1 VARCHAR(64), @@@V16_VCHAR1 VARCHAR(64), @@@V15_VCHAR1 VARCHAR(64), @@@V14_VCHAR1 VARCHAR(64), @@@V13_VCHAR1 VARCHAR(64), @@@V12_VCHAR1 VARCHAR(64), @@@V11_VCHAR1 VARCHAR(64), @@@V10_VCHAR1 VARCHAR(64), @@@V9_VCHAR1 VARCHAR(64), @@@V8_VCHAR1 VARCHAR(64), @@@V7_VCHAR1 VARCHAR(64), @@@V6_VCHAR1 VARCHAR(64), @@@V5_VCHAR1 VARCHAR(64), @@@V4_VCHAR1 VARCHAR(64), @@@V3_VCHAR1 VARCHAR(64), @@@V2_VCHAR1 VARCHAR(64), @@@V1_VCHAR1 VARCHAR(64), @@@V0_VCHAR1 VARCHAR(64))"));
		System.out.println("D-15: " + test("select T1.M__INDEX_, T1.M__REPLICAT_, T1.M_CURRENCY, T1.M_FAMILY, T1.M_GROUP, T1.M_PRC_CTP, T1.M_PROC_A, T1.M_TYPE, T1.M_TYPOLOGY, T1.M_USAGE, T1.M_REFERENCE, T1.M_CHLD_PRMT from CAC_RTGB_DBF T1 where T1.M__INDEX_ = @@@V0_INT(@@@V0_INT INT)"));
		System.out.println("D-16: " + test("/***DBXTUNE-REWRITE: [Changed: old style left-outer-join(*=), to equal-join(=)]***/ SELECT T1.M__DATE_, T1.M__ALIAS_, T1.M_REFERENCE, T1.M_SETNAME, T1.M_US_TYPE, T1.M_TYPE, T2.M__DATE_, T2.M__ALIAS_, T2.M_REFERENCE, T2.M_MATCODE FROM MPX_VOLMAT_DBF T1, MPY_VOLMAT_DBF T2 WHERE T1.M_REFERENCE = T2.M_REFERENCE AND T1.M__DATE_ = T2.M__DATE_ AND T1.M__ALIAS_ = T2.M__ALIAS_ AND ((((T1.M__DATE_ = ?) AND (T1.M__ALIAS_ = ?)) AND ((T1.M_SETNAME IN (...) AND T1.M_US_TYPE = ?) AND T1.M_TYPE = ?)) AND ((T2.M__DATE_ = ?) AND (T2.M__ALIAS_ = ?))) ORDER BY T1.M_SETNAME ASC, T1.M_US_TYPE ASC, T1.M_TYPE ASC"));
		System.out.println("D-17: " + test("/***DBXTUNE-NORMALIZE: [Removed: numbers]***/ DYNAMIC_SQL dyn: "));
		System.out.println("D-18: " + test("create view dummy as select t1.* from t1 join t2 on t1.id = t2.id where t1.c2 = 'someVal'"));
		System.out.println("D-19: " + test("SELECT a.number, a.id FROM customer.AgreementRiskBenefit arb INNER JOIN customer.Agreement a ON a.id = arb.agreementId WHERE a.orgNrCustomer = @p0 AND arb.interim = V0_INT(@p0 VARCHAR(64) output, V0_INT INT)"));
		System.out.println("D-20: " + test("select conversati0_.id as id1_0_, conversati0_.currentState as currentState2_0_, conversati0_.externalParty as externalParty3_0_, conversati0_.respondTo as respondTo4_0_, conversati0_.txId as txId5_0_, conversati0_.type as type6_0_, conversati0_.updated as updated7_0_ from Conversation conversati0_ where (conversati0_.currentState in (@p0 , @p1 , @p2 , @p3 , @p4 , @p5 , @p6 , @p7 , @p8 , @p9 , @p10)) and (conversati0_.id in (select metadata1_.conversation_id from Metadata metadata1_ where metadata1_.mKey=@p11 and metadata1_.mValue=@p12))(@p10 VARCHAR(64) output, @p9 VARCHAR(64) output, @p8 VARCHAR(64) output, @p7 VARCHAR(64) output, @p6 VARCHAR(64) output, @p5 VARCHAR(64) output, @p4 VARCHAR(64) output, @p3 VARCHAR(64) output, @p2 VARCHAR(64) output, @p1 VARCHAR(64) output, @p0 VARCHAR(64) output, @p11 VARCHAR(64) output, @p12 VARCHAR(64) output)"));
		System.out.println("D-21: " + test("delete from dbo.Event where txId in (select c.txId from dbo.Conversation c where c.updated <= dateadd(month, @monthsToDelete, getdate()) and c.type = @typeToDelete)(@monthsToDelete INT output, @typeToDelete VARCHAR(30) output)"));

		System.out.println("SS-1: " + test("(@P0 nvarchar(4000))select agarochkon0_.formular_ref as formular9_8_0_, agarochkon0_.id as id1_8_0_, agarochkon0_.id as id1_8_1_, agarochkon0_.uuid as uuid2_8_1_, agarochkon0_.agarandel as agarande3_8_1_, agarochkon0_.agt_bolag_ref as agt_bola8_8_1_, agarochkon0_.formular_ref as formular9_8_1_, agarochkon0_.person_el_org_nummer as person_e4_8_1_, agarochkon0_.kundbolaget as kundbola5_8_1_, agarochkon0_.namn as namn6_8_1_, agarochkon0_.person as person7_8_1_, agareentit1_.id as id1_8_2_, agareentit1_.uuid as uuid2_8_2_, agareentit1_.agarandel as agarande3_8_2_, agareentit1_.agt_bolag_ref as agt_bola8_8_2_, agareentit1_.formular_ref as formular9_8_2_, agareentit1_.person_el_org_nummer as person_e4_8_2_, agareentit1_.kundbolaget as kundbola5_8_2_, agareentit1_.namn as namn6_8_2_, agareentit1_.person as person7_8_2_ from agare agarochkon0_ left outer join agare agareentit1_ on agarochkon0_.agt_bolag_ref=agareentit1_.id where agarochkon0_.formular_ref=@P0"));
	}
}

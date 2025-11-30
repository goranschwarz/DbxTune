/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.sql.norm;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.dbxtune.sql.norm.StatementNormalizer.NormalizeParameters.AddStatus;
import com.dbxtune.utils.StringUtil;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.HexValue;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.SupportsOldOracleJoinSyntax;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;

/**
 * Helper class that tries to parse a SQL Statement and replace <i>constants</i> into question marks (?)<br>
 * This may be used to anonymize data<br>
 * Or it could be used to look for SQL statements that are repeated (but with <i>stripped</i> where clauses
 */
public class StatementNormalizer
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	//----------------------------------------------------------------
	// BEGIN: instance
	private static StatementNormalizer _instance = null;
	public static StatementNormalizer getInstance()
	{
		if (_instance == null)
		{
			StatementNormalizer instance = new StatementNormalizer();
			setInstance(instance);
		}
		return _instance;
	}
	public static void setInstance(StatementNormalizer instance)
	{
		_instance = instance;
	}
	public static boolean hasInstance()
	{
		return _instance != null;
	}
	// END: instance
	//----------------------------------------------------------------

	static class ReplaceConstantValues extends ExpressionDeParser
	{
// Changed 'InExpression' when upgarding from JSqlParser version 3.2 -->> 4.3
//		@Override
//		public void visit(InExpression inExpression) 
//		{
//			StringBuilder sb = getBuffer();
//			
//// getLeftItemsList() ... REMOVED IN JSqlParser version ???? CHECK if this any side effect... DO: expr.getLeftExpression().accept(this); handle this removed method???
////			if (inExpression.getLeftExpression() == null) { 
////				inExpression.getLeftItemsList().accept(this);
////			} else {
////				inExpression.getLeftExpression().accept(this);
////				if (inExpression.getOldOracleJoinSyntax() == SupportsOldOracleJoinSyntax.ORACLE_JOIN_RIGHT) {
////					sb.append("(+)");
////				}
////			}
//// Below code was grabbed from: https://github.com/JSQLParser/JSqlParser/blob/master/src/main/java/net/sf/jsqlparser/util/deparser/ExpressionDeParser.java
//			inExpression.getLeftExpression().accept(this);
//			if (inExpression.getOldOracleJoinSyntax() == SupportsOldOracleJoinSyntax.ORACLE_JOIN_RIGHT) {
//				buffer.append("(+)");
//			}
//			if (inExpression.isNot()) {
//				sb.append(" NOT");
//			}
//			sb.append(" IN ");
//
////System.out.println("---->>>> VISIT >> IN (xxx  xxx) ==== " + sb.toString());
//
//			inExpression.getRightItemsList().accept(this);
//
//			// Get the just produced SQL... to decide if it's a SUB-SELECT or just static values, which can be normalized to '...'
//			int start = sb.lastIndexOf(" IN ("); 
//			int end   = start + " IN (".length();
//			while (end < sb.length()) // Loop until we find the first end ')'  QUESTION: Should we keep track of "embedded ()" or is it enough to find next ')'
//			{
//				String ch = sb.substring(end, end + 1);
////System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxx: end=" + end + ", ch=|" + ch + "|");
//				end++;
//				if (")".equals(ch))
//					break;
//			}
//
//			// if it's NOT a sub-select... then: replace: "IN (?, ?, ?)" with "IN (...)" 
//			// but KEEP all sub-selects
//			String inSql = sb.substring(start, end);
////System.out.println("<<<<---- VISIT >> IN (near-end) >>>> [start=" + start + ", end=" + end + "]: lastInSql=|" + inSql + "| ==== " + sb.toString());
//			if ( ! inSql.contains("SELECT ") )
//				sb.replace(start, end, " IN (...)");
//		}
//
		@Override
		public void visit(InExpression inExpression) 
		{
			StringBuilder sb = getBuffer();
			
			inExpression.getLeftExpression().accept(this);
			if (inExpression.getOldOracleJoinSyntax() == SupportsOldOracleJoinSyntax.ORACLE_JOIN_RIGHT) {
				buffer.append("(+)");
			}
			if (inExpression.isNot()) {
				sb.append(" NOT");
			}
			sb.append(" IN ");

//System.out.println("---->>>> VISIT >> IN (xxx  xxx) ==== " + sb.toString());

			if (inExpression.getRightExpression() != null) {
				inExpression.getRightExpression().accept(this);
			} else {
				inExpression.getRightItemsList().accept(this);
			}

			// Get the just produced SQL... to decide if it's a SUB-SELECT or just static values, which can be normalized to '...'
			int start = sb.lastIndexOf(" IN ("); 
			int end   = start + " IN (".length();
			while (end < sb.length()) // Loop until we find the first end ')'  QUESTION: Should we keep track of "embedded ()" or is it enough to find next ')'
			{
				String ch = sb.substring(end, end + 1);
//System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxx: end=" + end + ", ch=|" + ch + "|");
				end++;
				if (")".equals(ch))
					break;
			}

			// if it's NOT a sub-select... then: replace: "IN (?, ?, ?)" with "IN (...)" 
			// but KEEP all sub-selects
			String inSql = sb.substring(start, end);
//System.out.println("<<<<---- VISIT >> IN (near-end) >>>> [start=" + start + ", end=" + end + "]: lastInSql=|" + inSql + "| ==== " + sb.toString());
			if ( ! inSql.contains("SELECT ") )
				sb.replace(start, end, " IN (...)");
		}

		@Override
		public void visit(DoubleValue doubleValue) {
			this.getBuffer().append("?");
		}

		@Override
		public void visit(HexValue hexValue) {
			this.getBuffer().append("?");
		}

		@Override
		public void visit(LongValue longValue) {
			this.getBuffer().append("?");
		}

		@Override
		public void visit(StringValue stringValue) {
			this.getBuffer().append("?");
		}

		@Override
		public void visit(DateValue dateValue) {
			this.getBuffer().append("?");
		}

		@Override
		public void visit(TimestampValue timestampValue) {
			this.getBuffer().append("?");
		}

		@Override
		public void visit(TimeValue timeValue) {
			this.getBuffer().append("?");
		}

		@Override
	    public void visit(UserVariable var) {
			String varStr = var.toString();
			// "@p0" or "@p9754" then do "?" because it's a "runtime value sent in dynamic sql"...
			// but if @someName then keep the original variable name
			if (varStr.matches("^@p[0-9]+$"))
				varStr = "?";
			this.getBuffer().append(varStr);
	    }

	}

	private StringBuilder      _buffer;
	private ExpressionDeParser _exprDeParser;
	private SelectDeParser     _selectDeParser;
	private StatementDeParser  _stmtDeParser;

	public StatementNormalizer()
	{
		_buffer         = new StringBuilder();
		_exprDeParser   = new ReplaceConstantValues();
		_selectDeParser = new SelectDeParser(_exprDeParser, _buffer);
		_stmtDeParser   = new StatementDeParser(_exprDeParser, _selectDeParser, _buffer);

		_exprDeParser.setSelectVisitor(_selectDeParser);
		_exprDeParser.setBuffer(_buffer);
	}

	public String normalizeStatement(String sql, List<String> tableList) 
	throws JSQLParserException
	{
		if (StringUtil.isNullOrBlank(sql))
			return "";

		Statement stmt = CCJSqlParserUtil.parse(sql, parser -> parser.withSquareBracketQuotation(true));

		stmt.accept(_stmtDeParser);
		
		// Get tables that TABLES are part of the "slow" statement, then send those tables to the DDL Storage
		// The blow might work, but I have not tested it (and what performance side effects it will add)
		if (tableList != null)
		{
			try 
			{
				TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
				List<String> tmpTableList = tablesNamesFinder.getTableList(stmt);
				
				tableList.addAll(tmpTableList);
			} 
			catch (RuntimeException ignore) {}
		}

		// Get string and truncate the buffer for next statement
		String normalizedSql = _buffer.toString();
		_buffer.setLength(0);

		if (StringUtil.isNullOrBlank(normalizedSql))
			return "";

		return normalizedSql;
	}


	/** simple class to pass 'addStatus' as a parameter by reference (because we can't have pointers in Java, this is a simple workaround) */ 
	public static class NormalizeParameters
	{
		public enum AddStatus
		{
			NONE                           (0),
			NORMALIZE_SUCCESS_LEVEL_1      (1),
			NORMALIZE_SUCCESS_LEVEL_2      (2),
			NORMALIZE_SUCCESS_LEVEL_3      (3),
			NORMALIZE_STATIC_REPLACE       (10),
			NORMALIZE_SUCCESS_AFTER_REWRITE(20),
			NORMALIZE_FAILED               (30),
			NOT_SUPPORTED_BY_PARSER        (40);

			private AddStatus(int v) { val = v; }
			private final int val;
			public int getIntValue() { return val; }		
		};
		
		public AddStatus addStatus;
	}
	
	private final NormalizeParameters _dummyNormalizeParameters = new NormalizeParameters();


	/**
	 * Called before "real" parser... this to change some stuff that the parser copes with, but is miss intepered ...
	 * like <code>exec procName "param1"</code> should really be <code>exec procName 'param1'</code> otherwise it will se the "param1" as a identifier instead of a string
	 * <p>
	 * <b>DO AS LITTLE AS POSSIBLE in here</b>, because ALL Statements will go through this code path
	 * 
	 * @param sqlText
	 * @return
	 */
	public String preParseStatement(String sqlText)
	{
		// starts with 'sp_' add 'exec' at start
		if (StringUtil.startsWithIgnoreBlankIgnoreCase(sqlText, "sp_"))
		{
			sqlText = "exec " + sqlText;
		}

		// starts with 'exec' or 'execute'
		if (StringUtil.startsWithIgnoreBlankIgnoreCase(sqlText, "exec ") || StringUtil.startsWithIgnoreBlankIgnoreCase(sqlText, "execute "))
		{
			// and has any double quotes: change to single quotes
			if (sqlText.contains("\""))
				sqlText = sqlText.replace('"', '\'');
			
			// Remove any ";1"
			if (sqlText.contains(";1"))
				sqlText = sqlText.replace(";1", "");
		}
		
		return sqlText;
	}

	/**
	 * Normalize SQL Text - translate all constants in SQL into question marks(?) so we can create a HashCode to easier group SQL text in reporting  
	 * @param sqlText
	 * @param addStatus
	 * @return
	 */
	public String normalizeSqlText(String sqlText, NormalizeParameters np, List<String> tableList)
	{
		// If parameter is null, use a dummy internal object so we don't have to do NULL checks everywhere
		if (np == null)
			np = _dummyNormalizeParameters;

		np.addStatus = AddStatus.NONE;

		String normalizedSqlText = null;

		try
		{
			// Pre Parse: for example rewrites: |exec procname "a"| -->> |exec procname 'a'|
			sqlText = preParseStatement(sqlText);

			// Parse and normalize: rewrite: |select * from 'a'| -->> |select * from ?|
			normalizedSqlText = normalizeStatement(sqlText, tableList);
			
			np.addStatus = AddStatus.NORMALIZE_SUCCESS_LEVEL_1;

			_statNormalizeSuccessCount++;
		}
		catch(JSQLParserException ex)
		{
			// go into "FIX/Workaround" the parse problem
			// - Rewrite the SQL Statement, and comment out "known" sections where we know that the parser wont work.
			// - Then parse the Statement again
			// - set the "AddStatus" to XXX to indicate that this was made.

			_statNormalizeErrorLevel1Count++;
			
			// Before we go and parse/normalize the SQL Text... check if we can short-circuit things...
			NomalizerAction normAction = getNormalizerAction(sqlText);
			
			switch (normAction)
			{
			case DO_USER_DEFINED_NORMALIZATION:
				normalizedSqlText = userDefinedNormalizeStatement(sqlText);
				np.addStatus = AddStatus.NORMALIZE_STATIC_REPLACE;

				_statNormalizeUdCount++;
				break;

			case NOT_SUPPORTED_BY_PARSER:
				normalizedSqlText = null;
				np.addStatus = AddStatus.NOT_SUPPORTED_BY_PARSER;

				_statNormalizeSkipCount++;
				break;

			case DO_REWRITE:
			{
				// go into "Workaround/ReWrite" the SQL Text we had problems with (the parser do not accept everything)
				// - Rewrite the SQL Statement, and remove/change the SQL where there are "known parser weakness" 
				// - Then parse the Statement again
				// - set the "AddStatus" to XXX to indicate that this was made.

				List<String> rewriteComments = new ArrayList<>();
				boolean isRewritten = false;
				boolean failedInParseAfterReWrite = false;

				// Get available "ReWrites" and apply them
				for (IStatementFixer fixer : StatementFixerManager.getInstance().getFixerEntries())
				{
//					if (_logger.isDebugEnabled())
//						_logger.debug("normalizeSqlText(): Checking Rewrite: name='" + fixer.getName() + "', isRewritable=" + fixer.isRewritable(sqlText) + ", SQL=|" + sqlText + "|");
					
					// Is this "fixer" valid for this SQL Text?
					if (fixer.isRewritable(sqlText))
					{
						// make the rewrite
						sqlText = fixer.rewrite(sqlText);
						
						// mark as "re-written"
						rewriteComments.add(fixer.getComment());
						isRewritten = true;
						
//						System.out.println("  ++ SQL REWITE TO: " + sqlText);
					}
//					else
//					{
//						System.out.println("<< SKIPPING REWRITE: name='" + fixer.getName() + "', sqlText=|" + sqlText +"|");
//					}
				}

				// If we got a re-write... try to parse the "new" SQL Text
				if (isRewritten)
				{
//					System.out.println("  ++ FINAL +++++ SQL REWITE TO: " + sqlText);

					_statNormalizeReWriteCount++;

					try
					{
						// Parse and normalize
						normalizedSqlText = normalizeStatement(sqlText, tableList);

						np.addStatus = AddStatus.NORMALIZE_SUCCESS_LEVEL_2;

						_statNormalizeSuccessCount++;

//						if (StringUtil.hasValue(normalizedSqlText))
//						{
//							System.out.println("  >> SECOND LEVEL NORMALIZED VALUE: |" + normalizedSqlText + "|");
//						}
					}
					catch(JSQLParserException ex2)
					{
						failedInParseAfterReWrite = true;

						normalizedSqlText = null;
						_statNormalizeErrorLevel2Count++;
						np.addStatus = AddStatus.NORMALIZE_FAILED;

//						System.out.println("  !! SECOND LEVEL PARSER EXCEPTION: " + ex);
//						ex.printStackTrace();
						
//FIX below -- Report this to a "good" place...

						// Report this to a "good" place, so we can look at it later and create new User Defined Fixers/Normalizers 
						// Note: do NOT add it to the log as a Error or Warning
						if (_logger.isDebugEnabled())
							_logger.debug("SQL Normalizer failed at level-2 to parse the SQL Text |" + sqlText + "|.", ex2);
					}
				}

				// SO We still have a problem... (noReWrite or ReWrite and FAILED parse)
				// As a *last* resort: Lets check if it's maybe a Stored procedure that needs "exec" in front of the SQL Text
				if ( !isRewritten || failedInParseAfterReWrite)
				{
					try
					{
						// pretend it's a Stored Procedure EXECution
						sqlText = "exec " + sqlText;

						// Parse and normalize
						normalizedSqlText = normalizeStatement(sqlText, tableList);

						rewriteComments.add("Add: EXEC as prefix");

						np.addStatus = AddStatus.NORMALIZE_SUCCESS_LEVEL_3;

						_statNormalizeSuccessCount++;
					}
					catch(JSQLParserException ex3)
					{
						normalizedSqlText = null;
						_statNormalizeErrorLevel3Count++;
						np.addStatus = AddStatus.NORMALIZE_FAILED;

						// Report this to a "good" place, so we can look at it later and create new User Defined Fixers/Normalizers 
						// Note: do NOT add it to the log as a Error or Warning
						if (_logger.isDebugEnabled())
							_logger.debug("SQL Normalizer failed at level-3 (AFTER Adding 'EXEC') to parse the SQL Text |" + sqlText + "|.", ex3);
					}
				}

				// If the Second Level Parse was successful, add a comment at the start of *what* we changed
				// Note: This can't be added before the second level parsing, because the Parser removes comments...
				if (StringUtil.hasValue(normalizedSqlText))
				{
					if ( ! rewriteComments.isEmpty() )
					{
						normalizedSqlText = IStatementFixer.REWRITE_MSG_BEGIN + rewriteComments + IStatementFixer.REWRITE_MSG_END + normalizedSqlText;
						np.addStatus = AddStatus.NORMALIZE_SUCCESS_AFTER_REWRITE;
					}
				}
				break;
			} // end: case {} block
			} // end: switch
		}
		
		return normalizedSqlText;
	}
	
	public long _statNormalizeSuccessCount = 0;
	public long _statNormalizeSkipCount    = 0;
	public long _statNormalizeErrorLevel1Count   = 0;
	public long _statNormalizeErrorLevel2Count   = 0;
	public long _statNormalizeErrorLevel3Count   = 0;
//	public long _statNormalizeErrorCount   = 0;

	public long _statNormalizeUdCount      = 0;
	public long _statNormalizeReWriteCount = 0;






	public enum NomalizerAction
	{
		/** Try to Normalize the SQL Text, if it fails, then check if it's re-writable and normalize the re-written SQL Text */
		DO_REWRITE, 

//		/** The SQL Text isn't supported by the Parser, but It's a static SQL which can be considered as <b>already normalized</b> */
//		ALREADY_NORMALIZED, 

		/** Do not call the parser, instead make call to User Defined Logic than would normalize the SQL Text... The parser will fail to parse this SQL Text */
		DO_USER_DEFINED_NORMALIZATION, 

		/** This will not be supported by the parser, so do not even try to parse it */
		NOT_SUPPORTED_BY_PARSER
	};
	
	public NomalizerAction getNormalizerAction(String sql)
	{
		if (StringUtil.isNullOrBlank(sql)) 
			return NomalizerAction.NOT_SUPPORTED_BY_PARSER;

		// Check USER DEFINED Normalizers
		for (IUserDefinedNormalizer entry : UserDefinedNormalizerManager.getInstance().getUserDefinedEntries())
		{
//			if (_logger.isDebugEnabled())
//				_logger.debug("getNormalizerAction(): Checking UD Normalize: name='" + entry.getName() + "', isHandled=" + entry.isHandled(sql) + ", SQL=|" + sql + "|");

			if (entry.isHandled(sql))
				return NomalizerAction.DO_USER_DEFINED_NORMALIZATION;
		}

		return NomalizerAction.DO_REWRITE;
	}

	public String userDefinedNormalizeStatement(String sql)
	{
		for (IUserDefinedNormalizer entry : UserDefinedNormalizerManager.getInstance().getUserDefinedEntries())
		{
//			if (_logger.isDebugEnabled())
//				_logger.debug("userDefinedNormalizeStatement(): Checking UD Normalize: name='" + entry.getName() + "', isHandled=" + entry.isHandled(sql) + ", SQL=|" + sql + "|");

			if (entry.isHandled(sql))
				return entry.normalize(sql);
		}
		return sql;
	}

//	/**
//	 * Hardcoded list of "stuff" the JSqlParser is not capable of handling
//	 * 
//	 * @param sql
//	 * @return true = NOT Supported, do not parse...  false = try to parse
//	 */
//	public boolean isNotYetSupportedByParser(String sql)
//	{
//		if (sql == null) return true;
//
//		// DUMP TRAN|DATABASE ...
//		if (StringUtil.startsWithIgnoreBlankIgnoreCase(sql, "dump ")) return true;
//
//		// set nocount on Any-SQL-stmnt ... 
//		if (StringUtil.startsWithIgnoreBlankIgnoreCase(sql, "set nocount ")) return true;
//
//		// BULK Inserts: insert bulk dbname..tabname with arrayinsert, nodescribe
//		if (StringUtil.startsWithIgnoreBlankIgnoreCase(sql, "insert bulk ")) return true;
//
//		// COMMIT TRANSACTION ...
//		// This seems strange...
//		if (StringUtil.startsWithIgnoreBlankIgnoreCase(sql, "commit transaction ")) return true;
//
//		// writetext
//		if (StringUtil.startsWithIgnoreBlankIgnoreCase(sql, "writetext ")) return true;
//		
//		// OPEN_CURSOR
//		if (StringUtil.startsWithIgnoreBlankIgnoreCase(sql, "OPEN_CURSOR ")) return true;
//		
//		// Old style outer join
//		if (sql.indexOf("*=") != -1) return true;
//		if (sql.indexOf("=*") != -1) return true;
//
//		// should we skip where we have more than ### newlines (since it may be a "strange" statement)
//
//		// delete top 200000 ...
//
//		// .... set @xxx = value
//
//		// OK
//		return false;
//	}
	
//	public String normalizeStatementNoThrow(String sql, boolean skipKnownUnhandledStatements) 
//	{
//		try
//		{
//			// Do not try to parse if the statement is in the "not-yet-supported-by-the-parser" section
//			if (skipKnownUnhandledStatements && isNotYetSupportedByParser(sql))
//				return "";
//
//			return normalizeStatement(sql);
//		}
//		catch(JSQLParserException ex)
//		{
//			// TODO: Possibly add SQL-Text to a list, which can be skipped by 'isNotYetSupportedByParser()' 
//			//       This to make it a bit more "self learning"...
//			if (_logger.isDebugEnabled())
//				_logger.debug("Problems normalizing SQL=|"+sql+"|", ex);
//
////_logger.info("Problems normalizing SQL=|"+sql+"|", ex);
//			
//			return "";
//			//return ex.getCause()+"";
//		}
//	}

//	public static String normalizeStatement(String sql) throws JSQLParserException
//	{
//		StringBuilder buffer = new StringBuilder();
//		ExpressionDeParser expr = new ReplaceConstantValues();
//
//		SelectDeParser selectDeparser = new SelectDeParser(expr, buffer);
//		expr.setSelectVisitor(selectDeparser);
//		expr.setBuffer(buffer);
//		StatementDeParser stmtDeparser = new StatementDeParser(expr, selectDeparser, buffer);
//
//		Statement stmt = CCJSqlParserUtil.parse(sql);
//
//		stmt.accept(stmtDeparser);
//		return stmtDeparser.getBuffer().toString();
//	}

//	public static void main(String[] args) throws JSQLParserException
//	{
//		StatementNormalizer sn = new StatementNormalizer();
//		System.out.println(sn.normalizeStatement("SELECT 'abc', 5 FROM mytable WHERE col='test'"));
//		System.out.println(sn.normalizeStatement("SELECT 'abc', 5 FROM mytable WHERE col in('a','b','c')"));
//		System.out.println(sn.normalizeStatement("SELECT \"abc\", 5 FROM mytable WHERE col in(\"a\",'b','c') and c99 = \"abc\" ---"));
//		System.out.println(sn.normalizeStatement("UPDATE table1 A SET A.columna = 'XXX' WHERE A.cod_table = 'YYY'"));
//		System.out.println(sn.normalizeStatement("INSERT INTO example (num, name, address, tel) VALUES (1, 'name', 'test ', '1234-1234')"));
//		System.out.println(sn.normalizeStatement("DELETE FROM table1 where col=5 and col2=4"));
//		System.out.println(sn.normalizeStatementNoThrow("declare @xxx select @xxx=id from dummy1", true));
//		System.out.println(sn.normalizeStatementNoThrow("SELECT a.xforeta, a.personnummer_2, a.Fornamn, a.Efternamn, a.status, CASE WHEN pp.xPerson IS NULL THEN 0 ELSE 1 END AS protected, a.employeeid AS anstallningsnr, e.xid AS employmentid, e.xanstallda, e.dateemployed, e.datequit, e.terminated, f.xid, f.foretag, f.orgnr, f.xmaklar, f.ansvass, f.xLivAdmAssistent, f.xcreated_by, f.xcreated, f.xmodified_by, f.xmodified, CASE WHEN x.varde IS NULL THEN 0 ELSE 1 END AS blocked, k.xid AS kostnadsstalleid, k.namn AS kostnadsstallenamn, k.xForeta AS kostnadsstalleforetag, pc.Name AS pensionCategory, a.pkassa, prem.plannamn, ca.name AS collectiveAgreementName FROM employmentExtended e JOIN Anstallda a ON e.xanstallda = a.xid JOIN foretag f ON e.xforetag = f.xid LEFT JOIN PensionCategory pc ON a.xPensionCategory = pc.xID LEFT JOIN kostnadsstallen k ON (k.xid <> 0 and a.xkostnadsstalle = k.xid) LEFT JOIN personparameter pp ON a.xid = pp.xperson AND pp.value = '1' AND pp.xparametertype = (SELECT xid FROM PERSONPARAMETERTYPE WHERE code = 'PROTECTED_INFO') LEFT JOIN (SELECT varde FROM xkod WHERE typ = 'MM_AuthXID') x on x.varde = cast(f.xid as varchar) LEFT JOIN premieplaner prem ON a.xpremplan = prem.xid LEFT JOIN collectiveagreementemployee cae ON a.xid = cae.xanstallda LEFT JOIN collectiveagreement ca ON cae.xcollectiveagreement = ca.xid WHERE f.status2 = 1 AND  a.personnummer_2 IN (@p0,@p1,@p2,@p3,@p4,@p5,@p6,@p7,@p8,@p9,@p10,@p11,@p12,@p13,@p14,@p15,@p16,@p17,@p18,@p19,@p20,@p21,@p22,@p23,@p24,@p25,@p26,@p27,@p28,@p29,@p30,@p31,@p32,@p33,@p34,@p35,@p36,@p37,@p38,@p39,@p40,@p41,@p42,@p43,@p44,@p45,@p46,@p47,@p48,@p49,@p50,@p51,@p52,@p53,@p54,@p55,@p56,@p57,@p58,@p59,@p60,@p61,@p62,@p63,@p64,@p65,@p66,@p67,@p68,@p69,@p70,@p71,@p72,@p73,@p74,@p75,@p76,@p77,@p78,@p79,@p80,@p81,@p82,@p83,@p84,@p85,@p86,@p87,@p88,@p89,@p90,@p91,@p92,@p93,@p94,@p95,@p96,@p97,@p98,@p99,@p100,@p101,@p102,@p103,@p104,@p105,@p106,@p107,@p108,@p109,@p110,@p111,@p112,@p113,@p114,@p115,@p116,@p117,@p118,@p119,@p120,@p121,@p122,@p123,@p124,@p125,@p126,@p127,@p128,@p129,@p130,@p131,@p132,@p133,@p134,@p135,@p136,@p137,@p138,@p139,@p140,@p141,@p142,@p143,@p144,@p145,@p146,@p147,@p148,@p149,@p150,@p151,@p152,@p153,@p154,@p155,@p156,@p157,@p158,@p159,@p160,@p161,@p162,@p163,@p164,@p165,@p166,@p167,@p168,@p169,@p170,@p171,@p172,@p173,@p174,@p175,@p176,@p177,@p178,@p179,@p180,@p181,@p182,@p183,@p184,@p185,@p186,@p187,@p188,@p189,@p190,@p191,@p192,@p193,@p194,@p195,@p196,@p197,@p198,@p199,@p200,@p201,@p202,@p203,@p204,@p205,@p206,@p207,@p208,@p209,@p210,@p211,@p212,@p213,@p214,@p215,@p216,@p217,@p218,@p219,@p220,@p221,@p222,@p223,@p224,@p225,@p226,@p227,@p228,@p229,@p230,@p231,@p232,@p233,@p234,@p235,@p236,@p237,@p238,@p239,@p240,@p241,@p242,@p243,@p244,@p245,@p246,@p247,@p248,@p249,@p250,@p251,@p252,@p253,@p254,@p255,@p256,@p257,@p258,@p259,@p260,@p261,@p262,@p263,@p264,@p265,@p266,@p267,@p268,@p269,@p270,@p271,@p272,@p273,@p274,@p275,@p276,@p277,@p278,@p279,@p280,@p281,@p282,@p283,@p284,@p285,@p286,@p287,@p288,@p289,@p290,@p291,@p292,@p293,@p294,@p295,@p296,@p297,@p298,@p299,@p300,@p301,@p302,@p303,@p304,@p305,@p306,@p307,@p308,@p309,@p310,@p311,@p312,@p313,@p314,@p315,@p316,@p317,@p318,@p319,@p320,@p321,@p322,@p323,@p324,@p325,@p326,@p327,@p328,@p329,@p330,@p331,@p332,@p333,@p334,@p335,@p336,@p337,@p338,@p339,@p340,@p341,@p342,@p343,@p344,@p345,@p346,@p347,@p348,@p349,@p350,@p351,@p352,@p353,@p354,@p355,@p356,@p357,@p358,@p359,@p360,@p361,@p362,@p363,@p364,@p365,@p366,@p367,@p368,@p369,@p370,@p371,@p372,@p373,@p374,@p375,@p376,@p377,@p378,@p379,@p380,@p381,@p382,@p383,@p384,@p385,@p386,@p387,@p388,@p389,@p390,@p391,@p392,@p393,@p394,@p395,@p396,@p397,@p398,@p399,@p400,@p401,@p402,@p403,@p404,@p405,@p406,@p407,@p408,@p409,@p410,@p411,@p412,@p413,@p414,@p415,@p416,@p417,@p418,@p419,@p420,@p421,@p422,@p423,@p424,@p425,@p426,@p427,@p428,@p429,@p430,@p431,@p432,@p433,@p434,@p435,@p436,@p437,@p438,@p439,@p440,@p441,@p442,@p443,@p444,@p445,@p446,@p447,@p448,@p449,@p450,@p451,@p452,@p453,@p454,@p455,@p456,@p457,@p458,@p459,@p460,@p461,@p462,@p463,@p464,@p465,@p466,@p467,@p468,@p469,@p470,@p471,@p472,@p473,@p474,@p475,@p476,@p477,@p478,@p479,@p480,@p481,@p482,@p483,@p484,@p485,@p486,@p487,@p488,@p489,@p490,@p491,@p492,@p493,@p494,@p495,@p496,@p497,@p498,@p499) AND f.Orgnr = @p500 ORDER BY e.dateEmployed DESC", true));
//		System.out.println(sn.normalizeStatementNoThrow("SELECT name, organizationNumber FROM customer.CustomerCompany WHERE organizationNumber = @p0", true));
//		System.out.println(sn.normalizeStatementNoThrow("SELECT name, organizationNumber FROM customer.CustomerCompany WHERE organizationNumber = @someName", true));
//		
//
//		//System.out.println(sn.normalizeStatement("dump DATABASE master to \"sybackup::-SERV netbackup.maxm.se -CLIENT prod-a-ase.maxm.se -POL Sybase_ASE -SCHED Default-Application-Backup\""));
//	}

	public static void main(String[] args) throws JSQLParserException
	{
		// Set Log4j Log Level
		Configurator.setRootLevel(Level.TRACE);

		
		StatementNormalizer sn = new StatementNormalizer();

		test(sn, "SELECT 'abc', 5 FROM mytable WHERE col='test'");
		test(sn, "SELECT 'abc' as test_abc_col");
		test(sn, "SELECT test_abc_col = 'abc'");
		test(sn, "exec xxx 1,2,3");
		test(sn, "exec xxx 'a', 'b', 'c'");
		test(sn, "exec xxx \"a\", \"b\", \"c\"");
		test(sn, "execute dbo.NTT_sel_list;1");
		test(sn, "execute dbo.NTT_sel_list;1 @NameSearch = 'MTG-B%'");
		test(sn, "execute dbo.NTT_sel_list @NameSearch = 'MTG-B%'");
		test(sn, "sp_who '1'");
		test(sn, "xxx \"a\", \"b\", \"c\"");
		test(sn, "nti_sel_dup_tic_gac 5,'20210121'");
		test(sn, "select T1.M_NB from ACG_ENTRY_VIEW_DBF T1  where ((((1 > 0) and (T1.M_EN_DATE<='20211231')) and T1.M_ENTITY in ('SEK AB','SEK S-SECT')) and (M_NB in ( select AE.M_NB from ACG_ENTRY_DBF AE where AE.M_NB > ( select max(M_NB) from ACCTOXOR_ID_DBF where M_SYS_DATE < (select M_ACC_DATE from TRN_ENTD_DBF where M_LABEL='SEK AB'     ))) ))");
		test(sn, "delete MPAUD_BD_DBF where MPAUD_BD_DBF.M_LINK IN ( select MPAUD_HD_DBF.M_LINK from MPAUD_HD_DBF where MPAUD_HD_DBF.M_PARAMDATE = '20150107' )");
		test(sn, "select * from t1 where c1 in (select c2 from t2 where c2 in (select c3 from t3 where c3 in ('val-3-1', 'val-3-2') or c3 in (select c4 from t4 where c4 in ('val-4-1', 'val-4-2') ) )) or c1 in ('val-1-1')");
	}

	private static String test(StatementNormalizer sn, String sql)
	{
		System.out.println();
		System.out.println("--------------------------------");
		System.out.println(">>> |" + sql + "|.");

		
		String norm = null;
		try 
		{
			norm = sn.normalizeSqlText(sql, null, null);
		}
		catch(Exception ex)
		{
			System.out.println(" ++++++ EXCEPTION");
			ex.printStackTrace();
		}

		System.out.println("<<< |" + norm + "|.");

		return norm;
	}
	
	// public static void main(String[] args)
	// {
	// try
	// {
	// String sql ="SELECT NAME, ADDRESS, COL1 FROM USER WHERE SSN IN
	// ('11111111111111', '22222222222222');";
	// Select select = (Select) CCJSqlParserUtil.parse(sql);
	//
	// StringBuilder sb = new StringBuilder();
	// StatementDeParser stDeParser = new StatementDeParser(sb);
	//
	//
	// }
	// catch(Exception ex)
	// {
	// ex.printStackTrace();
	// }
	// }
}

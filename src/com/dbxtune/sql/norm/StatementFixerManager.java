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
package com.dbxtune.sql.norm;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public class StatementFixerManager
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private Set<IStatementFixer> _statementFixerEntries = new LinkedHashSet<>();

	//----------------------------------------------------------------
	// BEGIN: instance
	private static StatementFixerManager _instance = null;
	public static StatementFixerManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new StatementFixerManager();
			//throw new RuntimeException("UserDefinedChartManager dosn't have an instance yet, please set with setInstance(instance).");
		}
		return _instance;
	}
//	public static void setInstance(StatementFixerManager instance)
//	{
//		_instance = instance;
//	}
//	public static boolean hasInstance()
//	{
//		return _instance != null;
//	}
	// END: instance
	//----------------------------------------------------------------

	//----------------------------------------------------------------
	// BEGIN: Constructors
	public StatementFixerManager()
	{
		init();
	}
	// END: Constructors
	//----------------------------------------------------------------

	private void init()
	{
		// SET NOCOUNT ON -->> ''
		add( new StatementFixerRegEx("set-nocount", "set\\s+nocount\\s+(on|off)", Pattern.CASE_INSENSITIVE, "", "Removed: 'set nocount on|off'") );

		// OLD STYLE OUTER JOIN -->> =
		// This is handled in JSqlParser Version: 5.x
//		add( new StatementFixerRegEx("old-style-left-outer-join",  " \\*= ", " = ", "Changed: old style left-outer-join(*=), to equal-join(=)") );
//		add( new StatementFixerRegEx("old-style-right-outer-join", " =\\* ", " = ", "Changed: old style right-outer-join(=*), to equal-join(=)") );

		// COMMIT TRANSACTION NAME -->> COMMIT
		// This is handled in JSqlParser Version: x
//		add( new StatementFixerRegEx("commit-tran", "commit\\s+(tran|transaction)\\s+\\w*", "commit", "Changed: 'commit transaction name' into 'commit'") );

		// DELETE TOP ### -->> DELETE
		// This is handled in JSqlParser Version: 5.x
//		add( new StatementFixerRegEx("delete-top", "delete\\s+top\\s+\\d+\\s+from\\s+", Pattern.CASE_INSENSITIVE, "delete from ", "Changed: 'delete top ### from ' into 'delete from '") );

		// " -->> '
		// This is handled in JSqlParser Version: 5.x
//		add( new StatementFixerDoubleQuotes2SingleQuotes() );

		//------------------------------------------------------------
		// Sybase ASE Syntax: ROWS LIMIT 3001
		//                    ROWS OFFSET 0 LIMIT 3001
		// Pagination Queries Using limit and offset
		// https://help.sap.com/docs/SAP_ASE/e0d4539d39c34f52ae9ef822c2060077/26d84b4ddae94fed89d4e7c88bc8d1e6.html
		//------------------------------------------------------------
		add( new IStatementFixer() 
		{
			// Regex pattern for "rows limit ### offset ##" where offset is optional
			// \s+ means one or more whitespace characters
			// \d+ means one or more digits
			// (?:\s+offset\s+\d+)? means the offset part is optional (non-capturing group with ?)
//			private final Pattern LIMIT_OFFSET_PATTERN = Pattern.compile("rows\\s+limit\\s+\\d+(?:\\s+offset\\s+\\d+)?", Pattern.CASE_INSENSITIVE);

			// Regex pattern explanation:
		    // - \bROWS\b: matches "ROWS" as a whole word
		    // - \s+: one or more whitespace characters
		    // - (LIMIT\s+\d+|OFFSET\s+\d+): matches either "LIMIT ###" or "OFFSET ###"
		    // - (\s+(OFFSET|LIMIT)\s+\d+)?: optionally matches the second clause
		    // - (?i): case-insensitive flag
			private final Pattern PATTERN = Pattern.compile("(?i)\\bROWS\\b\\s+(LIMIT\\s+\\d+|OFFSET\\s+\\d+)(\\s+(OFFSET|LIMIT)\\s+\\d+)?", Pattern.CASE_INSENSITIVE);

			@Override public String  getName()                { return "ase-rows-limit"; }
			@Override public String  getDescrition()          { return getComment(); }
			@Override public boolean isRewritable(String sql) { return PATTERN.matcher(sql).find(); }
			@Override public String  getComment()             { return "Commented out: 'ROWS LIMIT #' or 'ROWS OFFSET # LIMIT #'"; }
			@Override public String  rewrite(String sql) 
			{
				// Check if it was already fixed
				if (sql.startsWith(getPrefix()))
					return sql;

				Matcher matcher = PATTERN.matcher(sql);
				return getPrefix() + matcher.replaceAll(IStatementFixer.REWRITE_MSG_BEGIN + " $0 " + IStatementFixer.REWRITE_MSG_END);
			}
		});

		//------------------------------------------------------------
		// Sybase ASE Syntax: delete|update top ##
		//------------------------------------------------------------
		add( new IStatementFixer() 
		{
	        // Regular expression pattern to match DELETE/UPDATE statements with TOP syntax
	        // Pattern breakdown:
	        // (?i)                 - Case-insensitive flag (matches DELETE, delete, DeLeTe, etc.)
	        // (DELETE|UPDATE)      - Captures either DELETE or UPDATE keyword (captured as group $1)
	        // \\s+                 - Matches one or more whitespace characters (spaces, tabs, newlines)
	        // (TOP\\s+\\d+)        - Captures "TOP" followed by whitespace and digits (captured as group $2)
	        //                        \\s+ matches whitespace between TOP and the number
	        //                        \\d+ matches one or more digits (the number after TOP)
	        // \\s+                 - Matches trailing whitespace after the number
			private final Pattern PATTERN = Pattern.compile("(?i)(DELETE|UPDATE)\\s+(TOP\\s+\\d+)\\s+", Pattern.CASE_INSENSITIVE);
			
			@Override public String  getName()                { return "ase-del-upd-top-rows"; }
			@Override public String  getDescrition()          { return getComment(); }
			@Override public boolean isRewritable(String sql) { return PATTERN.matcher(sql).find(); }
			@Override public String  getComment()             { return "Commented out: 'HOLDLOCK'"; }
			@Override public String  rewrite(String sql) 
			{
				// Check if it was already fixed
				if (sql.startsWith(getPrefix()))
					return sql;

				Matcher matcher = PATTERN.matcher(sql);
				return getPrefix() + matcher.replaceAll("$1 " + IStatementFixer.REWRITE_MSG_BEGIN + " $2 " + IStatementFixer.REWRITE_MSG_END);
			}
		});

		//------------------------------------------------------------
		// Sybase ASE Syntax: select ... from tablename HOLDLOCK
		//------------------------------------------------------------
		add( new IStatementFixer() 
		{
			// Regex pattern for table name with optional alias followed by HOLDLOCK
			// Matches patterns like:
			// - tableName HOLDLOCK
			// - tableName alias HOLDLOCK
			// - tableName WITH (HOLDLOCK)
			// - tableName alias WITH (HOLDLOCK)
//			private final Pattern HOLDLOCK_PATTERN = Pattern.compile("(\\w+)\\s+(\\w+\\s+)?(WITH\\s*\\()?\\s*HOLDLOCK\\s*(\\))?", Pattern.CASE_INSENSITIVE);
			private final Pattern PATTERN = Pattern.compile("\\bHOLDLOCK\\b", Pattern.CASE_INSENSITIVE);

			@Override public String  getName()                { return "ase-tab-holdlock"; }
			@Override public String  getDescrition()          { return getComment(); }
//			@Override public boolean isRewritable(String sql) { return HOLDLOCK_PATTERN.matcher(sql).find(); }
			@Override public boolean isRewritable(String sql) { return PATTERN.matcher(sql).find(); }
			@Override public String  getComment()             { return "Commented out: 'HOLDLOCK'"; }
			@Override public String  rewrite(String sql) 
			{
				// Check if it was already fixed
				if (sql.startsWith(getPrefix()))
					return sql;

				Matcher matcher = PATTERN.matcher(sql);
				return getPrefix() + matcher.replaceAll(IStatementFixer.REWRITE_MSG_BEGIN + " $0 " + IStatementFixer.REWRITE_MSG_END);
//				return getPrefix() + matcher.replaceAll("/*** HOLDLOCK ****/");
			}
//			@Override public String  rewrite(String sql) 
//			{
//				Matcher matcher = HOLDLOCK_PATTERN.matcher(sql);
//				StringBuffer result = new StringBuffer();
//
//				while (matcher.find()) 
//				{
//					// Replace with table name followed by commented out HOLDLOCK
//					String tableName = matcher.group(1);
//					String withPart = matcher.group(2) != null ? matcher.group(2) : "";
//					String closeParen = matcher.group(3) != null ? matcher.group(3) : "";
//
//					String replacement = tableName + IStatementFixer.REWRITE_MSG_BEGIN + withPart + "holdlock" + closeParen + IStatementFixer.REWRITE_MSG_END;
//					matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
//				}
//				matcher.appendTail(result);
//				return getPrefix() + result.toString();
//			}
		});
		
		//------------------------------------------------------------
		// Sybase ASE Syntax: insert... insert... insert...
		// fix: add ';' after each 'insert'
		//------------------------------------------------------------
		add( new IStatementFixer() 
		{
			// Compiled regex pattern for adding semicolons to INSERT statements
			// Pattern breakdown:
			// (?i)                          - Case-insensitive flag (matches INSERT, insert, Insert, etc.)
			// (                             - Start of capturing group 1
			//   INSERT\\s+INTO\\s+          - Match "INSERT INTO" with one or more whitespace characters
			//   .*?                         - Match any characters (non-greedy) - this captures table name, columns, VALUES keyword, and data
			//   \\)                         - Match the closing parenthesis of the VALUES clause
			// )                             - End of capturing group 1
			// [\\s]*                        - Match zero or more whitespace characters after the closing parenthesis
			// (?=INSERT|$)                  - Positive lookahead: ensure next content is either another INSERT or end of string
			//	                             - This ensures we don't match beyond the current INSERT statement
			private final Pattern PATTERN = Pattern.compile("(?i)(INSERT\\s+INTO\\s+.*?\\))[\\s]*(?=INSERT|$)", Pattern.CASE_INSENSITIVE);
			
			@Override public String  getName()                { return "ase-many-insert-in-same-batch"; }
			@Override public String  getDescrition()          { return getComment(); }
			@Override public boolean isRewritable(String sql) { return PATTERN.matcher(sql).find(); }
			@Override public String  getComment()             { return "Added simicolon on multiple inserts"; }
			@Override public String  rewrite(String sql) 
			{
				// Check if it was already fixed
				if (sql.startsWith(getPrefix()))
					return sql;

				// Replace each matched INSERT statement with itself ($1) plus a semicolon and newline
				// $1 refers to the content captured in group 1
				Matcher matcher = PATTERN.matcher(sql);
				String result = matcher.replaceAll("$1;\n");

				// Clean up: remove any duplicate semicolons that might have been created
				// ;+ matches one or more consecutive semicolons and replaces with single semicolon
				result = result.replaceAll(";+", ";");

				// Remove leading/trailing whitespace
				result = result.trim();

//				// Ensure the entire SQL text ends with exactly one semicolon
//				if (!result.endsWith(";")) 
//				{
//					result += ";";
//				}

				return result;
			}
		});

		//------------------------------------------------------------
		// Get USER Defined Fixers (from Properties). 
		//------------------------------------------------------------
		Configuration conf = Configuration.getCombinedConfiguration();
		
		List<String> removeList = StringUtil.parseCommaStrToList(conf.getProperty("StatementFixerManager.remove.list", ""));
		for (String name : removeList)
		{
			if ("all".equalsIgnoreCase(name))
				_statementFixerEntries.clear();

			for (Iterator<IStatementFixer> iterator = _statementFixerEntries.iterator(); iterator.hasNext();)
			{
				IStatementFixer entry = iterator.next();

				if (name.equalsIgnoreCase(entry.getName()))
					iterator.remove();
			}
		}
			
//		for (String key : conf.getKeys("StatementFixerManager."))
//		{
//			// FIXME: parse StatementFixerManager entries, and instantiate entries
//		}

		//-----------------------------------------------------------
		// Get System names for the below output
		//-----------------------------------------------------------
		Set<String> systemNames = new LinkedHashSet<>();
		for (IStatementFixer entry : _statementFixerEntries)
			systemNames.add(entry.getName());
		int systemNamesCount = systemNames.size();

		//------------------------------------------------------------
		// Get USER Defined Fixers (from Java source files). 
		//------------------------------------------------------------
		Set<IStatementFixer> compiledUd = NormalizerCompiler.getInstance().getStatementFixers(); 
		_statementFixerEntries.addAll(compiledUd);

		Set<String> udNames = new LinkedHashSet<>();
		for (IStatementFixer entry : compiledUd)
			udNames.add(entry.getName());
		int udNamesCount = udNames.size();

		_logger.info("StatementFixerManager was initialized with " + systemNamesCount + " System Entries " + systemNames + ", and " + udNamesCount + " User Defined Entries " + udNames);
	}


	/**
	 * Get entries
	 * @return
	 */
	public Set<IStatementFixer> getFixerEntries()
	{
		return _statementFixerEntries;
	}

	public void add(IStatementFixer fixer)
	{
		_statementFixerEntries.add(fixer);
	}
}

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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public class StatementFixerManager
{
	private static Logger _logger = Logger.getLogger(StatementFixerManager.class);

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
		add( new StatementFixerRegEx("old-style-left-outer-join",  " \\*= ", " = ", "Changed: old style left-outer-join(*=), to equal-join(=)") );
		add( new StatementFixerRegEx("old-style-right-outer-join", " =\\* ", " = ", "Changed: old style right-outer-join(=*), to equal-join(=)") );

//		// COMMIT TRANSACTION NAME -->> COMMIT
//		add( new StatementFixerRegEx("commit-tran", "commit\\s+(tran|transaction)\\s+\\w*", "commit", "Changed: 'commit transaction name' into 'commit'") );

		// DELETE TOP ### -->> DELETE
		add( new StatementFixerRegEx("delete-top", "delete\\s+top\\s+\\d+\\s+from\\s+", Pattern.CASE_INSENSITIVE, "delete from ", "Changed: 'delete top ### from ' into 'delete from '") );

		// " -->> '
		add( new StatementFixerDoubleQuotes2SingleQuotes() );

		
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

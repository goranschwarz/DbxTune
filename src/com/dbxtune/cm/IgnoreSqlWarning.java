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
package com.dbxtune.cm;

import java.sql.SQLWarning;

public class IgnoreSqlWarning
{
	private int    _errorCode = -1;
	private String _sqlState  = null;
	private String _regexStr  = null;

	/**
	 * @param errorCode    What Error Code to ignore (Integer.MIN_VALUE == ignore ALL SQL Warnings)
	 */
	public IgnoreSqlWarning(int errorCode)
	{
		this(errorCode, null, null);
	}

	/**
	 * @param errorCode    What Error Code to ignore (Integer.MIN_VALUE == ignore ALL SQL Warnings)
	 * @param sqlState     What SQL State to ignore (can be null)
	 */
	public IgnoreSqlWarning(int errorCode, String sqlState)
	{
		this(errorCode, sqlState, null);
	}

	/**
	 * @param errorCode    What Error Code to ignore (Integer.MIN_VALUE == ignore ALL SQL Warnings)
	 * @param sqlState     What SQL State to ignore (can be null)
	 * @param regexStr     What Message to ignore, uses regex <code>regexStr.matches(sqlWarning.getMessage())</code> (can be null) 
	 */
	public IgnoreSqlWarning(int errorCode, String sqlState, String regexStr)
	{
		_errorCode = errorCode;
		_sqlState  = sqlState;
		_regexStr  = regexStr;
	}

	/**
	 * Should we ignore the passed SQLWarning
	 * 
	 * @param sqlWarning   The SQLWarning passed for checking
	 * @return
	 */
	public boolean ignore(SQLWarning sqlWarning)
	{
		// Ignore ALL Warnings
		if (_errorCode == Integer.MIN_VALUE)
		{
			return true;
		}

		// Ignore With ErrorCode
		if (sqlWarning.getErrorCode() == _errorCode)
		{
			return true;
		}
			
		// Ignore With SQLState
		if (sqlWarning.getSQLState().equals(_sqlState))
		{
			return true;
		}

		// Use Regex to match the error message
		if (_regexStr != null && _regexStr.matches(sqlWarning.getMessage()))
		{
			return true;
		}

		// Keep the SQLWarning
		return false;
	}

}

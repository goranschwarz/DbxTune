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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class StatementFixerRegEx
extends StatementFixerAbstract
{
//	private String _regExStr;
	private Pattern _pattern;

	private String _replacementText;

	public StatementFixerRegEx(String name, String regExStr, String replacementText, String comment)
	throws PatternSyntaxException
	{
		this(name, regExStr, 0, replacementText, comment);
	}
	
	public StatementFixerRegEx(String name, String regExStr, int regExFlags, String replacementText, String comment)
	throws PatternSyntaxException
	{
		super(name, comment, comment);
		
//		_regExStr = regExStr;
		_pattern = Pattern.compile(regExStr, regExFlags);

		_replacementText = replacementText;
	}

	@Override
	public boolean isRewritable(String sqlText)
	{
		Matcher matcher = _pattern.matcher(sqlText);
		return matcher.find();
	}

	@Override
	public String rewrite(String sqlText)
	{
		Matcher matcher = _pattern.matcher(sqlText);
		return matcher.replaceAll(_replacementText);
	}

}

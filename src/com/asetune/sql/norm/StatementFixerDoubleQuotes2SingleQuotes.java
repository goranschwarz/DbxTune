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
package com.asetune.sql.norm;

/**
 * Translate Sybase or SQL-Server String quotation with double quotes
 */
public class StatementFixerDoubleQuotes2SingleQuotes
extends StatementFixerAbstract
{
	public StatementFixerDoubleQuotes2SingleQuotes()
	{
		super("double-quotes-into-singel-quotes", "Rewrite Duoble Quotes (\") into Single Quotes (')", "Changed: DoubleQuotes(\") into SingleQuotes(')");
	}

	@Override
	public boolean isRewritable(String sqlText)
	{
		return sqlText.indexOf('"') != -1;
	}

	@Override
	public String rewrite(String sqlText)
	{
		// FROM: .... "some constant string" ....
		// TO:   .... 'some constant string' ....

		return sqlText.replace('"', '\'');
	}
}

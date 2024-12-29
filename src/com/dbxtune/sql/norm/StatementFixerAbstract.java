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

public abstract class StatementFixerAbstract
implements IStatementFixer
{
	private String _name;
	private String _description;
	private String _comment;

	public StatementFixerAbstract(String name, String description, String comment)
	{
		_name        = name;
		_description = description;
		_comment     = comment;
	}

	@Override
	public String getName()
	{
		return _name == null ? "" : _name;
	}

	@Override
	public String getDescrition()
	{
		return _description == null ? "" : _description;
	}

	@Override
	public String getComment()
	{
		return _comment == null ? "" : _comment;
	}

	public void setComment(String comment)
	{
		_comment = comment;
	}

	@Override
	public abstract boolean isRewritable(String sqlText);

	@Override
	public abstract String rewrite(String sqlText);

}

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

public interface IStatementFixer
{
	public static final String REWRITE_MSG_BEGIN = "/***DBXTUNE-REWRITE: ";
	public static final String REWRITE_MSG_END   = "***/ ";

	/** Get name of this instance */
	String getName();

	/** Get description of this instance */
	String getDescrition();
	
	/**
	 * Returns true if this entry can be fixed or re-writable
	 * @param sqlText   SQL Statement to check if it's re-writable or not
	 * @return
	 */
	boolean isRewritable(String sqlText);

	/**
	 * Is called if <code>isRewritable(String)</code> returns true
	 * 
	 * @param sqlText The SQL Text that should be re-written
	 * @return The new SQL Statement
	 */
	String rewrite(String sqlText);

	/**
	 * Get comment on what we did 
	 * @return
	 */
	String getComment();
}

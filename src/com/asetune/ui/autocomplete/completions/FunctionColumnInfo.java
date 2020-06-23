/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.ui.autocomplete.completions;

import java.io.Serializable;

/**
* Holds information about columns
*/
public class FunctionColumnInfo
implements Serializable
{
	private static final long serialVersionUID = 1L;

	public String _colName       = null;
	public int    _colPos        = -1;
	public String _colType       = null;
//	public String _colType2      = null;
	public int    _colLength     = -1;
	public int    _colIsNullable = -1;
	public String _colRemark     = null;
	public String _colDefault    = null;
//	public int    _colPrec       = -1;
	public int    _colScale      = -1;
}


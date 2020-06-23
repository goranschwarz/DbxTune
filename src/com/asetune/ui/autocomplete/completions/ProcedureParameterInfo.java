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
 * Holds information about parameters
 */
public class ProcedureParameterInfo
implements Serializable
{
	private static final long serialVersionUID = 1L;

	public String _paramName       = null;
	public int    _paramPos        = -1;
	public String _paramInOutType  = null;
	public String _paramType       = null;
//	public String _paramType2      = null;
	public int    _paramLength     = -1;
	public int    _paramIsNullable = -1;
	public String _paramRemark     = null;
	public String _paramDefault    = null;
//	public int    _paramPrec       = -1;
	public int    _paramScale      = -1;
}

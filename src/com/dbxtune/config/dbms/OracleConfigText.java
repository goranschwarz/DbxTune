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
package com.dbxtune.config.dbms;

import com.dbxtune.sql.conn.info.DbmsVersionInfo;

public abstract class OracleConfigText
{
	/** What sub types exists */
	public enum ConfigType
	{
		OraNlsParams
//		,OraSgaConfig
		};

	/** Log4j logging. */
//	private static Logger _logger = Logger.getLogger(OracleConfigText.class);


	public static void createAndRegisterAllInstances()
	{
		DbmsConfigTextManager.addInstance(new OracleConfigText.NlsParams());
	}

	
	/*-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**----- SUB CLASSES ----- SUB CLASSES ----- SUB CLASSES -----
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	*/
	public static class NlsParams extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "NLS Params"; }
		@Override public    String     getName()                              { return ConfigType.OraNlsParams.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
//		@Override protected String     getSqlCurrentConfig(long srvVersion)   { return "select * from v$nls_parameters"; }
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) { return "select * from v$nls_parameters"; }
	}
}

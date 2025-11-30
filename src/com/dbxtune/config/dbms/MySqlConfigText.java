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

public abstract class MySqlConfigText
{
	/** What sub types exists */
	public enum ConfigType
	{
		 SqlMode
		,HelpDb
		,Replicas
		};

	/** Log4j logging. */

	public static void createAndRegisterAllInstances()
	{
		DbmsConfigTextManager.addInstance(new MySqlConfigText.SqlMode());
		DbmsConfigTextManager.addInstance(new MySqlConfigText.HelpDb());
		DbmsConfigTextManager.addInstance(new MySqlConfigText.Replicas());
	}

	/*-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**----- SUB CLASSES ----- SUB CLASSES ----- SUB CLASSES -----
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	*/
	public static class SqlMode extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "SQL Mode"; }
		@Override public    String     getName()                              { return ConfigType.SqlMode.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
//		@Override protected String     getSqlCurrentConfig(long srvVersion)   { return "SELECT @@GLOBAL.sql_mode"; }
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) { return "SELECT @@GLOBAL.sql_mode"; }
	}

	public static class HelpDb extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "Databases"; }
		@Override public    String     getName()                              { return ConfigType.HelpDb.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
//		@Override protected String     getSqlCurrentConfig(long srvVersion)   { return "SHOW DATABASES"; }
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) { return "SHOW DATABASES"; }
	}

	public static class Replicas extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "Replicas"; }
		@Override public    String     getName()                              { return ConfigType.Replicas.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
//		@Override protected String     getSqlCurrentConfig(long srvVersion)   { return "SHOW SLAVE HOSTS"; }
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) { return "SHOW SLAVE HOSTS"; }
	}

}

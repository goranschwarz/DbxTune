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
package com.asetune.config.dbms;

import com.asetune.utils.Ver;

public abstract class SqlServerConfigText
{
	/** What sub types exists */
	public enum ConfigType
	{
		 SqlServerHelpDb
		,SqlServerSysDatabases
		,SqlServerHelpSort
		,SqlServerHostInfo
		,SqlServerSysInfo
		,SqlServerServerRegistry
		,SqlServerClusterNodes
		};

	/** Log4j logging. */
//	private static Logger _logger          = Logger.getLogger(SqlServerConfigText.class);

	public static void createAndRegisterAllInstances()
	{
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.HelpDb());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.SysDatabases());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.HelpSort());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.HostInfo());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.SysInfo());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.ServerRegistry());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.ClusterNodes());
	}

	/*-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**----- SUB CLASSES ----- SUB CLASSES ----- SUB CLASSES -----
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	*/
	public static class HelpDb extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                        { return "sp_helpdb"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerHelpDb.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "exec sp_helpdb"; }
	}

	public static class SysDatabases extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                        { return "sys.databases"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerSysDatabases.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "select has_dbaccess(name) AS has_dbaccess, * from sys.databases"; }
	}

	public static class HelpSort extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                        { return "sp_helpsort"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerHelpSort.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "exec sp_helpsort   SELECT SERVERPROPERTY_Collation = convert(varchar(255), SERVERPROPERTY('Collation'))"; }
	}

	public static class HostInfo extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                        { return "Host Info"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerHostInfo.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "select * from sys.dm_os_host_info"; }
		@Override public    long       needVersion()                        { return Ver.ver(2017); }
//		@Override public    long       needVersion()                        { return Ver.ver(14,0); } // 14 == 2017
	}

	public static class SysInfo extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                        { return "System Info"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerSysInfo.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "select * from sys.dm_os_sys_info"; }
	}

	public static class ServerRegistry extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                        { return "Server Registry"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerServerRegistry.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "select * from sys.dm_server_registry"; }
	}

	public static class ClusterNodes extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                        { return "Cluster Nodes"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerClusterNodes.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "select * from sys.dm_os_cluster_nodes"; }
	}

}

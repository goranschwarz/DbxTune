package com.asetune.config.dbms;

public abstract class SqlServerConfigText
{
	/** What sub types exists */
	public enum ConfigType
	{
		 SqlServerHelpDb
		,SqlServerSysDatabases
		};

	/** Log4j logging. */
//	private static Logger _logger          = Logger.getLogger(SqlServerConfigText.class);

	public static void createAndRegisterAllInstances()
	{
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.HelpDb());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.SysDatabases());
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
		@Override public    String     getTabLabel()                       { return "sp_helpdb"; }
		@Override public    String     getName()                           { return ConfigType.SqlServerHelpDb.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
		@Override protected String     getSqlCurrentConfig(int aseVersion) { return "exec sp_helpdb"; }
	}

	public static class SysDatabases extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                       { return "sys.databases"; }
		@Override public    String     getName()                           { return ConfigType.SqlServerSysDatabases.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
		@Override protected String     getSqlCurrentConfig(int aseVersion) { return "select * from sys.databases"; }
	}

}

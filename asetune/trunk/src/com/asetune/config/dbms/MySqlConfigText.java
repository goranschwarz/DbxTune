package com.asetune.config.dbms;

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
//	private static Logger _logger          = Logger.getLogger(MySqlConfigText.class);

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
		@Override public    String     getTabLabel()                       { return "SQL Mode"; }
		@Override public    String     getName()                           { return ConfigType.SqlMode.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "SELECT @@GLOBAL.sql_mode"; }
	}

	public static class HelpDb extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                       { return "Databases"; }
		@Override public    String     getName()                           { return ConfigType.HelpDb.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "SHOW DATABASES"; }
	}

	public static class Replicas extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                       { return "Replicas"; }
		@Override public    String     getName()                           { return ConfigType.Replicas.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "SHOW SLAVE HOSTS"; }
	}

}

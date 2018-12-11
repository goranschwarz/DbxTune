package com.asetune.config.dbms;

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
		@Override public    String     getTabLabel()                       { return "NLS Params"; }
		@Override public    String     getName()                           { return ConfigType.OraNlsParams.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "select * from v$nls_parameters"; }
	}
}

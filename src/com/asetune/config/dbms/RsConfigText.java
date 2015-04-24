package com.asetune.config.dbms;

import java.sql.Timestamp;

import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.RepServerUtils;


public abstract class RsConfigText
{
	/** What sub types exists */
//	public enum ConfigType {RsConfigAsText, RsConfigAsTextNonDefault, RsLicenseInfo, AseTempdb, AseHelpDevice, AseDeviceFsSpaceUsage, AseHelpServer, AseTraceflags, AseSpVersion, AseShmDumpConfig, AseMonitorConfig, AseHelpSort, AseLicenseInfo, AseClusterInfo, AseConfigFile};

	/** Log4j logging. */
//	private static Logger _logger          = Logger.getLogger(RsConfigText.class);

	public static void createAndRegisterAllInstances()
	{
		DbmsConfigTextManager.addInstance(new RsConfigText.RsConfigAsText());
		DbmsConfigTextManager.addInstance(new RsConfigText.RsConfigAsTextNonDefault());
		DbmsConfigTextManager.addInstance(new RsConfigText.RsConfig());
		DbmsConfigTextManager.addInstance(new RsConfigText.RsTbConfig());
		DbmsConfigTextManager.addInstance(new RsConfigText.RsHelpDb());
		DbmsConfigTextManager.addInstance(new RsConfigText.RsHelpDbRep());
		DbmsConfigTextManager.addInstance(new RsConfigText.RsHelpDbSub());
		DbmsConfigTextManager.addInstance(new RsConfigText.RsHelpRep());
		DbmsConfigTextManager.addInstance(new RsConfigText.RsHelpSub());
		DbmsConfigTextManager.addInstance(new RsConfigText.RsLicenceInfo());
	}

	/*-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**----- SUB CLASSES ----- SUB CLASSES ----- SUB CLASSES -----
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	*/
	public static class RsConfigAsText extends DbmsConfigTextAbstract
	{
		@Override
		public void refresh(DbxConnection conn, Timestamp ts)
		{
			if (isOffline())
			{
				super.refresh(conn, ts);
			}
			else
			{
				String str = RepServerUtils.printConfig(conn, false, null);
				setConfig(str);
			}
		}

		@Override public    String  getConfigType()                     { return getName(); }
		@Override public    String  getName()                           { return "RsConfigAsText"; }
		@Override public    String  getTabLabel()                       { return "RCL Config (all)"; }
		@Override protected String  getSqlCurrentConfig(int aseVersion) { return "-- well nothing, since we do it all in refresh()"; }
		@Override public    boolean getKeepDbmsState()                  { return false; }
	}

	public static class RsConfigAsTextNonDefault extends DbmsConfigTextAbstract
	{
		@Override
		public void refresh(DbxConnection conn, Timestamp ts)
		{
			if (isOffline())
			{
				super.refresh(conn, ts);
			}
			else
			{
				String str = RepServerUtils.printConfig(conn, true, null);
				setConfig(str);
			}
		}

		@Override public    String  getConfigType()                     { return getName(); }
		@Override public    String  getName()                           { return "RsConfigAsTextNonDefault"; }
		@Override public    String  getTabLabel()                       { return "RCL Config (non defaults)"; }
		@Override protected String  getSqlCurrentConfig(int aseVersion) { return "-- well nothing, since we do it all in refresh()"; }
		@Override public    boolean getKeepDbmsState()                  { return false; }
	}

	public static class RsConfig extends DbmsConfigTextAbstract
	{
		@Override public    String  getTabLabel()                       { return "rs_config"; }
		@Override public    String  getName()                           { return "RsConfig"; }
		@Override public    String  getConfigType()                     { return getName(); }
		@Override protected String  getSqlCurrentConfig(int aseVersion) { return "connect to rssd\ngo\n select * from rs_config \ngo\ndisconnect"; }
		@Override public    boolean getKeepDbmsState()                  { return false; }
	}

	public static class RsTbConfig extends DbmsConfigTextAbstract
	{
		@Override public    String  getTabLabel()                       { return "rs_tbconfig"; }
		@Override public    String  getName()                           { return "RsTbConfig"; }
		@Override public    String  getConfigType()                     { return getName(); }
		@Override protected String  getSqlCurrentConfig(int aseVersion) { return "connect to rssd\ngo\n select * from rs_tbconfig \ngo\ndisconnect"; }
		@Override public    boolean getKeepDbmsState()                  { return false; }
	}

	public static class RsHelpDb extends DbmsConfigTextAbstract
	{
		@Override public    String  getTabLabel()                       { return "rs_help-db"; }
		@Override public    String  getName()                           { return "RsHelpDb"; }
		@Override public    String  getConfigType()                     { return getName(); }
		@Override protected String  getSqlCurrentConfig(int aseVersion) { return "connect to rssd\ngo\n exec rs_helpdb \ngo\ndisconnect"; }
		@Override public    boolean getKeepDbmsState()                  { return false; }
	}

	public static class RsHelpDbRep extends DbmsConfigTextAbstract
	{
		@Override public    String  getTabLabel()                       { return "rs_help-db-rep"; }
		@Override public    String  getName()                           { return "RsHelpDbRep"; }
		@Override public    String  getConfigType()                     { return getName(); }
		@Override protected String  getSqlCurrentConfig(int aseVersion) { return "connect to rssd\ngo\n exec rs_helpdbrep \ngo\ndisconnect"; }
		@Override public    boolean getKeepDbmsState()                  { return false; }
	}

	public static class RsHelpDbSub extends DbmsConfigTextAbstract
	{
		@Override public    String  getTabLabel()                       { return "rs_help-db-sub"; }
		@Override public    String  getName()                           { return "RsHelpDbSub"; }
		@Override public    String  getConfigType()                     { return getName(); }
		@Override protected String  getSqlCurrentConfig(int aseVersion) { return "connect to rssd\ngo\n exec rs_helpdbsub \ngo\ndisconnect"; }
		@Override public    boolean getKeepDbmsState()                  { return false; }
	}

	public static class RsHelpRep extends DbmsConfigTextAbstract
	{
		@Override public    String  getTabLabel()                       { return "rs_help-rep"; }
		@Override public    String  getName()                           { return "RsHelpRep"; }
		@Override public    String  getConfigType()                     { return getName(); }
		@Override protected String  getSqlCurrentConfig(int aseVersion) { return "connect to rssd\ngo\n exec rs_helprep \ngo\ndisconnect"; }
		@Override public    boolean getKeepDbmsState()                  { return false; }
	}

	public static class RsHelpSub extends DbmsConfigTextAbstract
	{
		@Override public    String  getTabLabel()                       { return "rs_help-sub"; }
		@Override public    String  getName()                           { return "RsHelpSub"; }
		@Override public    String  getConfigType()                     { return getName(); }
		@Override protected String  getSqlCurrentConfig(int aseVersion) { return "connect to rssd\ngo\n exec rs_helpsub \ngo\ndisconnect"; }
		@Override public    boolean getKeepDbmsState()                  { return false; }
	}

	public static class RsLicenceInfo extends DbmsConfigTextAbstract
	{
		@Override public    String  getTabLabel()                       { return "RS License Info"; }
		@Override public    String  getName()                           { return "RsLicenceInfo"; }
		@Override public    String  getConfigType()                     { return getName(); }
		@Override protected String  getSqlCurrentConfig(int aseVersion) { return "sysadmin lmconfig"; }
		@Override public    boolean getKeepDbmsState()                  { return false; }
	}
}

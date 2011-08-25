package asemon;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import asemon.pcs.PersistWriterJdbc;
import asemon.utils.AseConnectionFactory;
import asemon.utils.AseSqlScript;
import asemon.utils.Configuration;

public class AseCacheConfig
{
    /** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(AseCacheConfig.class);

	/** Instance variable */
	private static AseCacheConfig _instance = null;

	private boolean _offline = false;

	private static final String MB_AVAIL_FOR_RECONF_STR = "MB available for reconfiguration";

	private static String GET_CONFIG_ONLINE_SQL = 
		"select ConfigSnapshotAtDateTime = convert(varchar(30),getdate(),109) \n" +

		"print '' \n" +
		"print '######################################################################################' \n" +
		"print '## ASE Memory available for reconfiguration' \n" +
		"print '######################################################################################' \n" +
		"declare @freeMb varchar(30) \n" +
		" \n" +
		"select @freeMb = ltrim(str( ((max(b.value) - min(b.value)) / 512.0), 15,1 )) \n" +
		"from master.dbo.sysconfigures a, master.dbo.syscurconfigs b \n" +
		"where a.name in ('max memory', 'total logical memory') \n" +
		"  and a.config = b.config \n" +
		" \n" +
		"print '%1! "+MB_AVAIL_FOR_RECONF_STR+".', @freeMb  \n" +

		"print '' \n" +
		"print '######################################################################################' \n" +
		"print '## sp_helpcache' \n" +
		"print '######################################################################################' \n" +
		"exec sp_helpcache \n" +

		"print '' \n" +
		"print '######################################################################################' \n" +
		"print '## sp_cacheconfig' \n" +
		"print '######################################################################################' \n" +
		"exec sp_cacheconfig \n" +
		"\n"; 

	private static String GET_CONFIG_OFFLINE_SQL = 
		"select \"configText\" \n" +
		"from " + PersistWriterJdbc.getTableName(PersistWriterJdbc.SESSION_ASE_CONFIG_TEXT, null, true) + " \n" +
		"where \"configName\"       = 'AseCacheConfig' \n" +
		"  and \"SessionStartTime\" = SESSION_START_TIME \n";

	private static String GET_CONFIG_OFFLINE_MAX_SESSION_SQL = 
		" (select max(\"SessionStartTime\") " +
		"  from "+PersistWriterJdbc.getTableName(PersistWriterJdbc.SESSION_ASE_CONFIG_TEXT, null, true) + 
		" ) ";
	
	/** The configuration is kept in a String */
	private String _configStr = null;
	
	// xxx.x MB available for reconfiguration
	private String _freeMemoryStr = null;
	private Double _freeMemory    = null;

	
	/** check if we got an instance or not */
	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	/** Get a instance of the class, if we didn't have an instance, one will be created, but not initialized */
	public static AseCacheConfig getInstance()
	{
		if (_instance == null)
			_instance = new AseCacheConfig();
		return _instance;
	}

	/** get the Config String */
	public String getConfig()
	{
		return _configStr;
	}

	/** How much memory is available for reconfiguration */
	public String getFreeMemoryStr()
	{
		return _freeMemoryStr;
	}

	/** How much memory is available for reconfiguration */
	public Double getFreeMemory()
	{
		return _freeMemory == null ? 0 :_freeMemory;
	}

	/** check if the AseConfig is initialized or not */
	public boolean isInitialized()
	{
		return (_configStr != null ? true : false);
	}

	/**
	 * Initialize 
	 * @param conn
	 */
	public void initialize(Connection conn, boolean offline, Timestamp ts)
	{
		_offline = offline;
		refresh(conn, ts);
	}

	/**
	 * refresh 
	 * @param conn
	 */
	public void refresh(Connection conn, Timestamp ts)
	{
		if (conn == null)
			return;

		_configStr = null;

		if ( ! _offline )
		{
			String sql = GET_CONFIG_ONLINE_SQL;
			
			try
			{
				AseSqlScript script = new AseSqlScript(conn);
				_configStr = script.executeSqlStr(sql);
			}
			catch (SQLException ex)
			{
//				if (offline && ex.getMessage().contains("not found"))
//				{
//					_logger.warn("Tooltip on column headers wasn't available in the offline database. This simply means that tooltip wont be showed in various places.");
//					return;
//				}
				_logger.error("AseCacheConfig:initialize:sql='"+sql+"'", ex);
				_configStr = null;
				return;
			}
		}
		else 
		{
			String sql = GET_CONFIG_OFFLINE_SQL;
			String tsStr = "";
			if (ts == null)
			{
				tsStr = GET_CONFIG_OFFLINE_MAX_SESSION_SQL;
			}
			else
			{
				tsStr = "'" + ts + "'";
			}
			sql = sql.replace("SESSION_START_TIME", tsStr);

			try
			{
				Statement stmt = conn.createStatement();

				ResultSet rs = stmt.executeQuery(sql);
				while ( rs.next() )
				{
					_configStr = rs.getString(1);
				}
				rs.close();
				stmt.close();
			}
			catch (SQLException ex)
			{
				if (_offline && ex.getMessage().contains("not found"))
				{
					_logger.warn("The saved value for CacheConfig wasn't available in the offline database, sorry.");
					return;
				}
				_logger.error("AseCacheConfig:initialize:sql='"+sql+"'", ex);
				_configStr = null;
				return;
			}
		}

		if (_configStr != null)
		{
			// find where to copy the info
			int stop = _configStr.indexOf(MB_AVAIL_FOR_RECONF_STR) - 1;
			int start = stop;
			for (; start>0; start--)
			{
				if (_configStr.charAt(start) == '\n')
				{
					start++;
					break;
				}
			}

			// Then copy and parse the information
			_freeMemory = null;
			if (stop > 0)
			{
				String mb = _configStr.substring(start, stop);
				_logger.debug("parse Available Memory for reconfiguration: start="+start+", stop="+stop+", MB='"+mb+"'.");
				try 
				{
					_freeMemory = Double.parseDouble(mb);
				}
				catch(NumberFormatException e)
				{
					_logger.warn("Can't parse the Free MB for reuse, MB='"+mb+"'. Caught="+e);
					_freeMemory = null; 
				}
			}
			_freeMemoryStr =  (_freeMemory == null ? "UNKNOWN" : _freeMemory) + " " + MB_AVAIL_FOR_RECONF_STR;
		}
	}

	/*---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	**----- TEST-CODE ---- TEST-CODE ---- TEST-CODE -----
	**---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	*/
	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		Configuration conf2 = new Configuration("c:\\projects\\asemon\\asemon.properties");
		Configuration.setInstance(Configuration.CONF, conf2);


		// DO THE THING
		try
		{
			System.out.println("Open the Dialog with a VALID connection.");
			Connection conn = AseConnectionFactory.getConnection("localhost", 5000, null, "sa", "", "test-AseCacheConfig", null);

			AseCacheConfig c = AseCacheConfig.getInstance();
			c.initialize(conn, false, null);
			System.out.println("AseCacheConfig.getConfig()=\n"+c.getConfig());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}

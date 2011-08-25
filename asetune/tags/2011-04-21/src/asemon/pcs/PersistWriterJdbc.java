/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.pcs;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import asemon.AseCacheConfig;
import asemon.AseConfig;
import asemon.MonTablesDictionary;
import asemon.MonTablesDictionary.MonTableColumnsEntry;
import asemon.MonTablesDictionary.MonTableEntry;
import asemon.TrendGraphDataPoint;
import asemon.Version;
import asemon.cm.CountersModel;
import asemon.cm.CountersModelAppend;
import asemon.utils.Configuration;
import asemon.utils.StringUtil;

public class PersistWriterJdbc
    extends PersistWriterBase
{
    /** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(PersistWriterJdbc.class);

	/*---------------------------------------------------
	** DEFINITIONS
	**---------------------------------------------------
	*/

	
	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/

	// Persistent Counter CONNection
	protected Connection _conn = null;
	private boolean _keepConnOpen = true;

	protected String _jdbcDriver = "org.h2.Driver";
	protected String _jdbcUrl    = "jdbc:h2:pcdb_yyy";
	protected String _jdbcUser   = "sa";
	protected String _jdbcPasswd = "";
	
	private   boolean _startH2NetworkServer = false;

	protected String _name       = "PersistWriterJdbc";
	
	protected boolean _jdbcDriverInfoHasBeenWritten = false;

	private org.h2.tools.Server _h2ServerTcp = null;
	private org.h2.tools.Server _h2ServerWeb = null;

	
	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/
	public PersistWriterJdbc()
	{
	}


	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/
	public void beginOfSample()
	{
		open();
	}

	public void endOfSample(boolean caughtErrors)
	{
		close();
	}

	public void startServices()
	throws Exception
	{
		// Everything could NOT be done with the jdbcUrl... so here goes some special
		// start the H2 TCP Server
		if ( _jdbcDriver.equals("org.h2.Driver") && _startH2NetworkServer )
		{
			_logger.info("Starting a H2 TCP server.");
			String[] args = new String[] { "-tcpAllowOthers" };
			_h2ServerTcp = org.h2.tools.Server.createTcpServer(args);
			_h2ServerTcp.start();

//			_logger.info("H2 TCP server, listening on port='"+h2Server.getPort()+"', url='"+h2Server.getURL()+"', service='"+h2Server.getService()+"'.");
			_logger.info("H2 TCP server, url='"+_h2ServerTcp.getURL()+"', service='"+_h2ServerTcp.getService()+"'.");

			if (true)
			{
				_logger.info("Starting a H2 WEB server.");
				//String[] argsWeb = new String[] { "-trace" };
				String[] argsWeb = new String[] { "" };
				_h2ServerWeb = org.h2.tools.Server.createWebServer(argsWeb);
				_h2ServerWeb.start();

				_logger.info("H2 WEB server, url='"+_h2ServerWeb.getURL()+"', service='"+_h2ServerWeb.getService()+"'.");
			}
		}
	}
	public void stopServices()
	{
		if (_h2ServerTcp != null)
		{
			_logger.info("Stopping H2 TCP Service.");
			_h2ServerTcp.stop();
		}

		if (_h2ServerWeb != null)
		{
			_logger.info("Stopping H2 WEB Service.");
			_h2ServerWeb.stop();
		}
	}

	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/

	public String getName()
	{
		return _name;
	}

	public void init(Configuration props) throws Exception
	{
		String propPrefix = "PersistWriterJdbc.";
		String propname = null;

		// property: name
		propname = propPrefix+"name";
		_name = props.getProperty(propname, _name);

		// WRITE init message, jupp a little late, but I wanted to grab the _name
		_logger.info("Initializing the PersistentCounterHandler.WriterClass component named '"+_name+"'.");
		
		_jdbcDriver = props.getProperty(propPrefix+"jdbcDriver", "");
		_jdbcUrl    = props.getProperty(propPrefix+"jdbcUrl",    "");
		_jdbcUser   = props.getProperty(propPrefix+"jdbcUser",   "");
		_jdbcPasswd = props.getProperty(propPrefix+"jdbcPasswd", "");
		_keepConnOpen = props.getBooleanProperty(propPrefix+"jdbcKeepConnOpen", _keepConnOpen);
		if (_jdbcPasswd.equalsIgnoreCase("null"))
			_jdbcPasswd="";

		_startH2NetworkServer = props.getBooleanProperty(propPrefix+"startH2NetworkServer", _startH2NetworkServer);

//		String configStr = "jdbcDriver='"+_jdbcDriver+"', jdbcUrl='"+_jdbcUrl+"', jdbcUser='"+_jdbcUser+"', jdbcPasswd='*hidden*'.";
//		_logger.info("Configuration for PersistentCounterHandler.WriterClass component named '"+_name+"': "+configStr);
		_logger.info("Configuration for PersistentCounterHandler.WriterClass component named '"+_name+"'.");
		_logger.info("                  "+propPrefix+"jdbcDriver           = " + _jdbcDriver);
		_logger.info("                  "+propPrefix+"jdbcUrl              = " + _jdbcUrl);
		_logger.info("                  "+propPrefix+"jdbcUser             = " + _jdbcUser);
		_logger.info("                  "+propPrefix+"jdbcPasswd           = " + "*hidden*");
		_logger.info("                  "+propPrefix+"jdbcKeepConnOpen     = " + _keepConnOpen);
		_logger.info("                  "+propPrefix+"startH2NetworkServer = " + _startH2NetworkServer);
	}

	protected void finalize() throws Throwable
	{
		super.finalize();
		
		close(true);
	}

	public void close(boolean force)
	{
		if (_conn == null)
			return;

		if ( ! _keepConnOpen || force)
		{
			try 
			{ 
				_conn.close(); 
				_conn = null; 
			}
			catch(Exception ignore){}
		}
	}

	public void close()
	{
		close(false);
	}
	
	private Connection open()
	{
		// If we already has a valid connection, lets reuse it...
		if (_keepConnOpen && _conn != null)
		{
			try 
			{
				if ( ! _conn.isClosed() )
					return _conn;
			}
			catch (SQLException e) {}
		}
		
		try
		{
			Class.forName(_jdbcDriver).newInstance();

			_logger.debug("Try getConnection to counterStore");

			// Look for variables in the URL and change them into runtime
			String localJdbcUrl = _jdbcUrl;
//FIXME: the input property does NOT seems to support having ${DATE} in the value pair, possibly asemon.utils.Configuration that fucks this up.
//			if (localJdbcUrl.indexOf("${DATE}") > 0)
//			{
//				//String dateStr = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
//				String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
//				//String timeStr = new SimpleDateFormat("HH.mm.ss").format(new Date());
//				
//				localJdbcUrl = localJdbcUrl.replaceAll("${DATE}", dateStr);
//				_logger.info("Found variables in the URL '"+_jdbcUrl+"', the new URL will be '"+localJdbcUrl+"'.");
//			}

			_conn = DriverManager.getConnection(localJdbcUrl, _jdbcUser, _jdbcPasswd);

			_logger.debug("The connection has property auto-commit set to '"+_conn.getAutoCommit()+"'.");

			// Write info about what JDBC driver we connects via.
			if ( ! _jdbcDriverInfoHasBeenWritten )
			{
            	_jdbcDriverInfoHasBeenWritten = true;

            	if (_logger.isDebugEnabled()) 
                {
    				_logger.debug("The following drivers have been loaded:");
    				Enumeration<Driver> drvEnum = DriverManager.getDrivers();
    				while( drvEnum.hasMoreElements() )
    				{
    					_logger.debug("    " + drvEnum.nextElement().toString());
    				}
    			}
    			
            	DatabaseMetaData dbmd = _conn.getMetaData();
				if (dbmd != null)
				{
					String getDriverName             = "-";
					String getDriverVersion          = "-";
					int    getDriverMajorVersion     = -1;
					int    getDriverMinorVersion     = -1;
					int    getJDBCMajorVersion       = -1;
					int    getJDBCMinorVersion       = -1;

					String getDatabaseProductName    = "-";
					String getDatabaseProductVersion = "-";
					int    getDatabaseMajorVersion   = -1;
					int    getDatabaseMinorVersion   = -1;

					try	{ getDriverName             = dbmd.getDriverName();             } catch (Throwable ignore) {}
					try	{ getDriverVersion          = dbmd.getDriverVersion();          } catch (Throwable ignore) {}
					try	{ getDriverMajorVersion     = dbmd.getDriverMajorVersion();     } catch (Throwable ignore) {}
					try	{ getDriverMinorVersion     = dbmd.getDriverMinorVersion();     } catch (Throwable ignore) {}
					try	{ getJDBCMajorVersion       = dbmd.getJDBCMajorVersion();       } catch (Throwable ignore) {}
					try	{ getJDBCMinorVersion       = dbmd.getJDBCMinorVersion();       } catch (Throwable ignore) {}

					try	{ getDatabaseProductName    = dbmd.getDatabaseProductName();    } catch (Throwable ignore) {}
					try	{ getDatabaseProductVersion = dbmd.getDatabaseProductVersion(); } catch (Throwable ignore) {}
					try	{ getDatabaseMajorVersion   = dbmd.getDatabaseMajorVersion();   } catch (Throwable ignore) {}
					try	{ getDatabaseMinorVersion   = dbmd.getDatabaseMinorVersion();   } catch (Throwable ignore) {}

					_logger.info("Connected using JDBC driver Name='"+getDriverName
							+"', Version='"         +getDriverVersion
							+"', MajorVersion='"    +getDriverMajorVersion
							+"', MinorVersion='"    +getDriverMinorVersion
							+"', JdbcMajorVersion='"+getJDBCMajorVersion
							+"', JdbcMinorVersion='"+getJDBCMinorVersion
							+"'.");
					_logger.info("Connected to Database Product Name='"+getDatabaseProductName
							+"', Version='"     +getDatabaseProductVersion
							+"', MajorVersion='"+getDatabaseMajorVersion
							+"', MinorVersion='"+getDatabaseMinorVersion
							+"'.");

					// Set what type of database we are connected to.
					setDatabaseProductName(getDatabaseProductName == null ? "" : getDatabaseProductName);
				}
			}

			// if ASE, turn off error message like: Scale error during implicit conversion of NUMERIC value '1.2920528650283813' to a NUMERIC field.
			// or fix the actual values to be more correct when creating graph data etc.
			if ( DB_PROD_NAME_ASE.equals(getDatabaseProductName()) )
			{
				_logger.debug("Connected to ASE, do some specific settings 'set arithabort numeric_truncation off'.");
				dbExec("set arithabort numeric_truncation off ");
			}
		}
		catch (SQLException ev)
		{
			StringBuffer sb = new StringBuffer();
			while (ev != null)
			{
				sb.append( "\n" );
				sb.append( ev.getMessage() );
				ev = ev.getNextException();
			}
			_logger.error("Problems when connecting to a datastore Server. "+sb.toString());
			_conn = null;
		}
		catch (Exception ev)
		{
			_logger.error("openConnection", ev);
			_conn = null;
		}
		
		return _conn;
	}


	private boolean dbExec(String sql)
	throws SQLException
	{
		return dbExec(sql, true);
	}

	private boolean dbExec(String sql, boolean printErrors)
	throws SQLException
	{
		if (_logger.isDebugEnabled())
		{
			_logger.debug("SEND SQL: " + sql);
		}

		try
		{
			Statement s = _conn.createStatement();
			s.execute(sql);
			s.close();
		}
		catch(SQLException e)
		{
			_logger.warn("Problems when executing sql statement: "+sql);
			throw e;
		}

		return true;
	}
	
	private boolean dbDdlExec(String sql)
	throws SQLException
	{
		if (_logger.isDebugEnabled())
		{
			_logger.debug("SEND DDL SQL: " + sql);
		}

		try
		{
			boolean autoCommitWasChanged = false;

			if (_conn.getAutoCommit() != true)
			{
				autoCommitWasChanged = true;
				
				// In ASE the above _conn.getAutoCommit() does execute 'select @@tranchained' in the ASE
				// Which causes the _conn.setAutoCommit(true) -> set CHAINED off
				// to fail with error: Msg 226, SET CHAINED command not allowed within multi-statement transaction
				//
				// In the JDBC documentation it says:
				// NOTE: If this method is called during a transaction, the transaction is committed.
				//
				// So it should be safe to do a commit here, that is what jConnect should have done...
				_conn.commit();

				_conn.setAutoCommit(true);
			}

			Statement s = _conn.createStatement();
			s.execute(sql);
			s.close();

			if (autoCommitWasChanged)
			{
				_conn.setAutoCommit(false);
			}
		}
		catch(SQLException e)
		{
			_logger.warn("Problems when executing DDL sql statement: "+sql);
			throw e;
		}

		return true;
	}

	/** 
	 * Check if table has been created, if not create it.
	 * @param tabId 
	 * @return True if table was created
	 * @throws SQLException
	 */
	private boolean checkAndCreateTable(int tabId)
	throws SQLException
	{
		String tabName = getTableName(tabId, null, false);

		if ( ! isDdlCreated(tabName) )
		{
			// Obtain a DatabaseMetaData object from our current connection        
			DatabaseMetaData dbmd = _conn.getMetaData();
	
			ResultSet rs = dbmd.getColumns(null, null, tabName, "%");
			boolean tabExists = rs.next();
			rs.close();
	
			if( tabExists )
			{
				// Check some various things.
				if (tabId == VERSION_INFO)
				{
					// FIXME: check if "VersionString" is the same as Version.getVersionStr()
					//        if not, just throw a WARNING message to the log
				}
			}
			else
			{
				_logger.info("Creating table '" + tabName + "'.");
				incCreateTables();
				
				String sql = getTableDdlString(tabId, null);
				dbDdlExec(sql);

				sql = getIndexDdlString(tabId, null);
				if (sql != null)
				{
					dbDdlExec(sql);
				}				
			}
			
			markDdlAsCreated(tabName);

			return true;
		}
		return false;
	}

	private void insertSessionParam(Timestamp sessionsStartTime, String type, String key, String val)
	throws SQLException
	{
		String tabName = getTableName(SESSION_PARAMS, null, true);

		// make string a "safe" string, escape all ' (meaning with '')
		if (key != null) key = key.replaceAll("'", "''");
		if (val != null) val = val.replaceAll("'", "''");

		// insert into MonSessionParams(SessionStartTime, Type, ParamName, ParamValue) values(...)
		StringBuffer sbSql = new StringBuffer();
		sbSql.append(" insert into ").append(tabName);
		sbSql.append(" values('")
			.append(sessionsStartTime).append("', '")
			.append(type)             .append("', '")
			.append(key)              .append("', '")
			.append(val)              .append("')");

		dbExec(sbSql.toString());
		incInserts();
	}

	public void startSession(PersistContainer cont)
	{
		// Open connection to db
		open();

		if (_conn == null)
		{
			_logger.error("No database connection to Persistent Storage DB.'");
			return;
		}

		if (cont._counterObjects == null)
		{
			_logger.error("Input parameter PersistContainer._counterObjects can't be null. Can't continue startSession()...");
			return;
		}

		
		try
		{
			//
			// FIRST CHECK IF THE TABLE EXISTS, IF NOT CREATE IT
			//
			checkAndCreateTable(VERSION_INFO);
			checkAndCreateTable(SESSIONS);
			checkAndCreateTable(SESSION_PARAMS);
			checkAndCreateTable(SESSION_SAMPLES);
			checkAndCreateTable(SESSION_SAMPLE_SUM);
			checkAndCreateTable(SESSION_SAMPLE_DETAILES);
			checkAndCreateTable(SESSION_MON_TAB_DICT);
			checkAndCreateTable(SESSION_MON_TAB_COL_DICT);
			checkAndCreateTable(SESSION_ASE_CONFIG);
			checkAndCreateTable(SESSION_ASE_CONFIG_TEXT);
			
			//--------------------------
			// Now fill in some data
			String tabName = getTableName(VERSION_INFO, null, true);

			StringBuffer sbSql = new StringBuffer();
			sbSql.append(" insert into ").append(tabName);
			sbSql.append(" values(");
			sbSql.append("  '").append(cont.getSessionStartTime() ).append("'");
			sbSql.append(", '").append(Version.getAppName()       ).append("'");
			sbSql.append(", '").append(Version.getVersionStr()    ).append("'");
			sbSql.append(", '").append(Version.getBuildStr()      ).append("'");
			sbSql.append(", '").append(Version.getSourceDate()    ).append("'");
			sbSql.append(",  ").append(Version.getSourceRev()     ).append(" ");
			sbSql.append(" )");

			dbExec(sbSql.toString());
			incInserts();


			tabName = getTableName(SESSIONS, null, true);

			sbSql = new StringBuffer();
			sbSql.append(" insert into ").append(tabName);
			sbSql.append(" values('").append(cont.getSessionStartTime()).append("', '").append(cont.getServerName()).append("', 0, null)");

			dbExec(sbSql.toString());
			incInserts();
			

			_logger.info("Storing CounterModel information in table "+getTableName(SESSION_PARAMS, null, false));
			//--------------------------------
			// LOOP ALL THE CM's and store some information
			tabName = getTableName(SESSION_PARAMS, null, true);
			Timestamp ts = cont.getSessionStartTime();

			for (CountersModel cm : cont._counterObjects)
			{
				String prefix = cm.getName();

				insertSessionParam(ts, "cm", prefix+".name",     cm.getName());
				insertSessionParam(ts, "cm", prefix+".sqlInit",  cm.getSqlInit());
				insertSessionParam(ts, "cm", prefix+".sql",      cm.getSql());
				insertSessionParam(ts, "cm", prefix+".sqlClose", cm.getSqlClose());

				insertSessionParam(ts, "cm", prefix+".pk",       cm.getPk()==null ? "" : cm.getPk().toString());
				insertSessionParam(ts, "cm", prefix+".diff",     Arrays.deepToString(cm.getDiffColumns()));
				insertSessionParam(ts, "cm", prefix+".diffDiss", Arrays.deepToString(cm.getDiffDissColumns()));
				insertSessionParam(ts, "cm", prefix+".pct",      Arrays.deepToString(cm.getPctColumns()));

				insertSessionParam(ts, "cm", prefix+".graphNames",Arrays.deepToString(cm.getTrendGraphNames()));
			}
			
			_logger.info("Storing AseMon configuration information in table "+getTableName(SESSION_PARAMS, null, false));
			//--------------------------------
			// STORE the configuration file
//			Configuration conf = Configuration.getInstance(Configuration.CONF); 
			Configuration conf = Configuration.getCombinedConfiguration(); 
			Iterator<Object> it = conf.keySet().iterator();
			while (it.hasNext()) 
			{
				String key = (String)it.next();
				String val = conf.getPropertyRaw(key);

				insertSessionParam(ts, "config", key, val);
			}
//			conf = Configuration.getInstance(Configuration.TEMP);
			conf = Configuration.getCombinedConfiguration();
			it = conf.keySet().iterator();
			while (it.hasNext()) 
			{
				String key = (String)it.next();
				String val = conf.getPropertyRaw(key);

				insertSessionParam(ts, "tmpConfig", key, val);
			}

			// Storing the MonTablesDictionary(monTables & monTableColumns), 
			// this so we can restore the proper Column ToolTip for this ASE version.
			if (MonTablesDictionary.hasInstance())
			{
				_logger.info("Storing monTables & monTableColumns dictionary in table "+getTableName(SESSION_MON_TAB_DICT, null, false)+" and "+getTableName(SESSION_MON_TAB_COL_DICT, null, false));
				saveMonTablesDictionary(MonTablesDictionary.getInstance(), cont._sessionStartTime);
			}

			// Storing the AseConfig and AseCacheConfig 
			_logger.info("Storing ASE Configuration in table "+getTableName(SESSION_ASE_CONFIG, null, false));
			saveAseConfig(AseConfig.getInstance(), cont._sessionStartTime);

			_logger.info("Storing ASE Cache Configuration in table "+getTableName(SESSION_ASE_CONFIG_TEXT, null, false));
			saveAseCacheConfig(AseCacheConfig.getInstance(), cont._sessionStartTime);
		}
		catch (SQLException e)
		{
			_logger.warn("Error when startSession() writing to Persistent Counter Store.", e);
		}
		
		// Close connection to db
		close();
	}

	public void loadMonTablesDictionary(MonTablesDictionary mtd)
	{
		throw new RuntimeException("NOT IMPLEMENTED: loadMonTablesDictionary()");
		// FIXME: move this to the reader
	}

	public void saveAseCacheConfig(AseCacheConfig aseCacheCfg, Timestamp sessionStartTime)
	{
		if (_conn == null)
		{
			_logger.error("No database connection to Persistent Storage DB.'");
			return;
		}

		StringBuffer sbSql = null;

		try
		{
			// START a transaction
			// This will lower number of IO's to the transaction log
			if (_conn.getAutoCommit() == true)
				_conn.setAutoCommit(false);

			//----------------------------------------------
			// SESSION_MON_TAB_COL_DICT
			String tabName = getTableName(SESSION_ASE_CONFIG_TEXT, null, true);

			sbSql = new StringBuffer();
			sbSql.append(" insert into ").append(tabName).append(" \n");
			sbSql.append(" values('").append(sessionStartTime).append("' \n");
			sbSql.append("       ,'AseCacheConfig' \n");
			sbSql.append("       ,").append(safeStr(aseCacheCfg.getConfig()))  .append(" \n");
			sbSql.append("       )\n");

			dbExec(sbSql.toString());
			incInserts();


			// CLOSE the transaction
			_conn.commit();
		}
		catch (SQLException e)
		{
			try 
			{
				if (_conn.getAutoCommit() == true)
					_conn.rollback();
			}
			catch (SQLException e2) {}

			_logger.warn("Error writing to Persistent Counter Store. SQL: "+sbSql.toString(), e);
		}
		finally
		{
			try { _conn.setAutoCommit(true); }
			catch (SQLException e2) { _logger.error("Problems when setting AutoCommit to true.", e2); }
		}
	}

	public void saveAseConfig(AseConfig aseCfg, Timestamp sessionStartTime)
	{
		if (_conn == null)
		{
			_logger.error("No database connection to Persistent Storage DB.'");
			return;
		}

		StringBuffer sbSql = null;

		try
		{
			// START a transaction
			// This will lower number of IO's to the transaction log
			if (_conn.getAutoCommit() == true)
				_conn.setAutoCommit(false);

			//----------------------------------------------
			// SESSION_MON_TAB_COL_DICT
			String tabName = getTableName(SESSION_ASE_CONFIG, null, true);

			for (int r=0; r<aseCfg.getRowCount(); r++)
			{
				sbSql = new StringBuffer();
				sbSql.append(" insert into ").append(tabName).append(" \n");
				sbSql.append(" values('").append(sessionStartTime).append("' \n");

				for (int c=0; c<aseCfg.getColumnCount(); c++)
				{
					// Get value
					Object o = aseCfg.getValueAt(r, c);

					// if it's a string, surround it with '' or NULL
					if (o instanceof String)
						o = safeStr( (String)o );

					// ASE and ASA does not cope with true/false, so lets use 1 and 0 instead
					if (o instanceof Boolean)
						o = ((Boolean)o).booleanValue() ? "1" : "0";

					sbSql.append("        ,").append(o).append(" \n");
				}
				sbSql.append("       )\n");

				dbExec(sbSql.toString());
				incInserts();
			}


			// CLOSE the transaction
			_conn.commit();
		}
		catch (SQLException e)
		{
			try 
			{
				if (_conn.getAutoCommit() == true)
					_conn.rollback();
			}
			catch (SQLException e2) {}

			_logger.warn("Error writing to Persistent Counter Store. SQL: "+sbSql.toString(), e);
		}
		finally
		{
			try { _conn.setAutoCommit(true); }
			catch (SQLException e2) { _logger.error("Problems when setting AutoCommit to true.", e2); }
		}
	}



	/**
	 * Return a SQL safe string
	 * <p>
	 * if str is null, "NULL" will be returned<br>
	 * else all "'" chars will be translated into "''"
	 * @param str
	 * @return
	 */
	public static String safeStr(String str)
	{
		if (str == null)
			return "NULL";
		StringBuilder sb = new StringBuilder();
		sb.append("'");
		sb.append(str.replaceAll("'", "''"));
		sb.append("'");
		return sb.toString();
	}

	public void saveMonTablesDictionary(MonTablesDictionary mtd, Timestamp sessionStartTime)
	{
		if (_conn == null)
		{
			_logger.error("No database connection to Persistent Storage DB.'");
			return;
		}

		StringBuffer sbSql = null;

		try
		{
			// START a transaction
			// This will lower number of IO's to the transaction log
			if (_conn.getAutoCommit() == true)
				_conn.setAutoCommit(false);

			//----------------------------------------------
			// SESSION_MON_TAB_DICT
			// SESSION_MON_TAB_COL_DICT
			String monTabName    = getTableName(SESSION_MON_TAB_DICT,     null, true);
			String monTabColName = getTableName(SESSION_MON_TAB_COL_DICT, null, true);

			Map<String,MonTableEntry> monTablesDictMap = mtd.getMonTablesDictionaryMap();

			for (MonTableEntry mte : monTablesDictMap.values())
			{
				sbSql = new StringBuffer();
				sbSql.append(" insert into ").append(monTabName).append(" \n");
				sbSql.append(" values('").append(sessionStartTime).append("', \n");
				sbSql.append("         ").append(mte._tableID)    .append(", \n");
				sbSql.append("         ").append(mte._columns)    .append(", \n");
				sbSql.append("         ").append(mte._parameters) .append(", \n");
				sbSql.append("         ").append(mte._indicators) .append(", \n");
				sbSql.append("         ").append(mte._size)       .append(", \n");
				sbSql.append("         ").append(safeStr(mte._tableName))  .append(", \n");
				sbSql.append("         ").append(safeStr(mte._description)).append("\n");
				sbSql.append("       )\n");

				dbExec(sbSql.toString());
				incInserts();

				for (MonTableColumnsEntry mtce : mte._monTableColumns.values())
				{
					sbSql = new StringBuffer();
					sbSql.append(" insert into ").append(monTabColName).append(" \n");
					sbSql.append(" values('").append(sessionStartTime) .append("', \n");
					sbSql.append("         ").append(mtce._tableID)    .append(", \n");
					sbSql.append("         ").append(mtce._columnID)   .append(", \n");
					sbSql.append("         ").append(mtce._typeID)     .append(", \n");
					sbSql.append("         ").append(mtce._precision)  .append(", \n");
					sbSql.append("         ").append(mtce._scale)      .append(", \n");
					sbSql.append("         ").append(mtce._length)     .append(", \n");
					sbSql.append("         ").append(mtce._indicators) .append(", \n");
					sbSql.append("         ").append(safeStr(mtce._tableName))  .append(", \n");
					sbSql.append("         ").append(safeStr(mtce._columnName)) .append(", \n");
					sbSql.append("         ").append(safeStr(mtce._typeName))   .append(", \n");
					sbSql.append("         ").append(safeStr(mtce._description)).append(" \n");
					sbSql.append("       )\n");

					dbExec(sbSql.toString());
					incInserts();
				}
			}


			// CLOSE the transaction
			_conn.commit();
		}
		catch (SQLException e)
		{
			try 
			{
				if (_conn.getAutoCommit() == true)
					_conn.rollback();
			}
			catch (SQLException e2) {}

			_logger.warn("Error writing to Persistent Counter Store. SQL: "+sbSql.toString(), e);
		}
		finally
		{
			try { _conn.setAutoCommit(true); }
			catch (SQLException e2) { _logger.error("Problems when setting AutoCommit to true.", e2); }
		}
	}


	public void saveSample(PersistContainer cont)
	{
		if (_conn == null)
		{
			_logger.error("No database connection to Persistent Storage DB.'");
			return;
		}

		Timestamp sessionStartTime  = cont.getSessionStartTime();
		Timestamp sessionSampleTime = cont.getSampleTime();

		
		StringBuffer sbSql = null;

		try
		{
			// START a transaction
			// This will lower number of IO's to the transaction log
			if (_conn.getAutoCommit() == true)
				_conn.setAutoCommit(false);

			//
			// INSERT THE ROW
			//
			String tabName = getTableName(SESSION_SAMPLES, null, true);

			sbSql = new StringBuffer();
			sbSql.append(" insert into ").append(tabName);
			sbSql.append(" values('").append(sessionStartTime).append("', '").append(sessionSampleTime).append("')");

			dbExec(sbSql.toString());
			incInserts();

			// Increment the "counter" column and set LastSampleTime in the SESSIONS table
			tabName = getTableName(SESSIONS, null, true);
			sbSql = new StringBuffer();
			sbSql.append(" update ").append(tabName);
			sbSql.append("    set ").append(qic).append("NumOfSamples")  .append(qic).append(" = ").append(qic).append("NumOfSamples").append(qic).append(" + 1,");
			sbSql.append("        ").append(qic).append("LastSampleTime").append(qic).append(" = '").append(sessionSampleTime).append("'");
			sbSql.append("  where ").append(qic).append("SessionStartTime").append(qic).append(" = '").append(sessionStartTime).append("'");

			dbExec(sbSql.toString());
			incUpdates();

			//--------------------------------------
			// COUNTERS
			//--------------------------------------
			for (CountersModel cm : cont._counterObjects)
			{
				saveCounterData(cm, sessionStartTime, sessionSampleTime);
			}

			// CLOSE the transaction
			_conn.commit();
		}
		catch (SQLException e)
		{
			try 
			{
				if (_conn.getAutoCommit() == true)
					_conn.rollback();
			}
			catch (SQLException e2) {}

			_logger.warn("Error writing to Persistent Counter Store. SQL: "+sbSql.toString(), e);
		}
		finally
		{
			try { _conn.setAutoCommit(true); }
			catch (SQLException e2) { _logger.error("Problems when setting AutoCommit to true.", e2); }
		}
	}

	private void saveDdl(int type, CountersModel cm)
	throws SQLException
	{
		ResultSet rs = null;
		String tabName;

		// Obtain a DatabaseMetaData object from our current connection
		DatabaseMetaData dbmd = _conn.getMetaData();


		tabName = getTableName(type, cm, false);

		rs = dbmd.getColumns(null, null, tabName, "%");
		boolean tabExists = rs.next();
		rs.close();

		if( ! tabExists )
		{
			_logger.info("Persistent Counter DB: Creating table "+StringUtil.left("'"+tabName+"'", 32, true)+" for CounterModel '" + cm.getName() + "'.");

			String sqlTable = getTableDdlString(type, cm);
			String sqlIndex = getIndexDdlString(type, cm);

			dbDdlExec(sqlTable);
			dbDdlExec(sqlIndex);
			
			incCreateTables();
		}
		
	}
	
	public boolean saveDdl(CountersModel cm)
  	{
		if (cm == null)
		{
			_logger.debug("saveDdl: cm == null.");
			return false;
		}

		ResultSetMetaData rsmd = cm.getResultSetMetaData();
		if (rsmd == null)
		{
			_logger.debug("saveDdl: rsmd == null.");
			return false;
		}

		// Write SQL Table definition
		if (_conn == null)
		{
			_logger.debug("saveDdl: _conn == null.");
			return false;
		}

		
		//------------------------------
		// Write SQL table definition file
		//------------------------------
		try
		{
			saveDdl(ABS, cm);
			saveDdl(DIFF, cm);
			saveDdl(RATE, cm);
		}
		catch (SQLException e)
		{
			_logger.warn("SQLException, Error writing DDL to Persistent Counter DB.", e);
			throw new RuntimeException("SQLException, Error writing DDL to Persistent Counter DB. Caught: "+e);
			//return false;
		}

		return true;
  	} // end: method

	
	/**
	 * Save the counters in the database
	 * 
	 * @param cm
	 * @param sessionStartTime
	 * @param sessionSampleTime
	 */
	private void saveCounterData(CountersModel cm, Timestamp sessionStartTime, Timestamp sessionSampleTime)
	{
		if (cm == null)
		{
			_logger.debug("saveCounterData: cm == null.");
			return;
		}

		if (cm instanceof CountersModelAppend) 
			return;

		if ( ! cm.hasDiffData() && ( cm.isPersistCountersDiffEnabled() || cm.isPersistCountersRateEnabled() ) )
		{
			_logger.info("No diffData is available, skipping writing Counters for name='"+cm.getName()+"'.");
			return;
		}

		_logger.debug("Persisting Counters for CounterModel='"+cm.getName()+"'.");

		int counterType = 0;
		int absRows     = 0;
		int diffRows    = 0;
		int rateRows    = 0;
		if (cm.hasAbsData()  && cm.isPersistCountersAbsEnabled())  {counterType += 1; absRows  = save(cm, CountersModel.DATA_ABS,  sessionStartTime, sessionSampleTime);}
		if (cm.hasDiffData() && cm.isPersistCountersDiffEnabled()) {counterType += 2; diffRows = save(cm, CountersModel.DATA_DIFF, sessionStartTime, sessionSampleTime);}
		if (cm.hasRateData() && cm.isPersistCountersRateEnabled()) {counterType += 4; rateRows = save(cm, CountersModel.DATA_RATE, sessionStartTime, sessionSampleTime);}
		
		int graphCount = 0;
		Map<String,TrendGraphDataPoint> tgdMap = cm.getTrendGraphData();
		if (tgdMap != null)
		{
			for (Map.Entry<String,TrendGraphDataPoint> entry : tgdMap.entrySet()) 
			{
			//	String              key  = entry.getKey();
				TrendGraphDataPoint tgdp = entry.getValue();

				saveGraphData(cm, tgdp, sessionStartTime, sessionSampleTime);
				graphCount++;
			}

		}

		// here is how the SESSION_SAMPLE_DETAILES should look like
//		sbSql.append("    "+fill(qic+"SessionStartTime" +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//		sbSql.append("   ,"+fill(qic+"SessionSampleTime"+qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//		sbSql.append("   ,"+fill(qic+"CmName"           +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
//		sbSql.append("   ,"+fill(qic+"type"             +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
//		sbSql.append("   ,"+fill(qic+"graphCount"       +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
//		sbSql.append("   ,"+fill(qic+"absRows"          +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
//		sbSql.append("   ,"+fill(qic+"diffRows"         +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
//		sbSql.append("   ,"+fill(qic+"rateRows"         +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");

		// Store some info
		StringBuilder sbSql = new StringBuilder();
		String tabName = getTableName(SESSION_SAMPLE_DETAILES, null, true);

		sbSql.append(" insert into ").append(tabName);
		sbSql.append(" values('").append(sessionStartTime).append("'");
		sbSql.append(", '").append(sessionSampleTime).append("'");
		sbSql.append(", '").append(cm.getName()).append("'");
		sbSql.append(", ").append(counterType);
		sbSql.append(", ").append(graphCount);
		sbSql.append(", ").append(absRows);
		sbSql.append(", ").append(diffRows);
		sbSql.append(", ").append(rateRows);
		sbSql.append(")");

		try
		{
			dbExec(sbSql.toString());
			incInserts();
		}
		catch (SQLException e)
		{
			_logger.warn("Error writing to Persistent Counter Store. SQL: "+sbSql.toString(), e);
		}


		// SUMMARY INFO for the whole session
		tabName = getTableName(SESSION_SAMPLE_SUM, null, true);

		sbSql = new StringBuilder();
		sbSql.append(" update ").append(tabName);
		sbSql.append(" set \"absSamples\"  = \"absSamples\"  + ").append( (absRows  > 0 ? 1 : 0) ).append(", ");
		sbSql.append("     \"diffSamples\" = \"diffSamples\" + ").append( (diffRows > 0 ? 1 : 0) ).append(", ");
		sbSql.append("     \"rateSamples\" = \"rateSamples\" + ").append( (rateRows > 0 ? 1 : 0) ).append("");
		sbSql.append(" where \"SessionStartTime\" = '").append(sessionStartTime).append("'");
		sbSql.append("   and \"CmName\" = '").append(cm.getName()).append("'");

		try
		{
			Statement stmnt = _conn.createStatement();
			int updCount = stmnt.executeUpdate(sbSql.toString());
			
			if (updCount == 0)
			{
				sbSql = new StringBuilder();
				sbSql.append(" insert into ").append(tabName);
				sbSql.append(" values('").append(sessionStartTime).append("'");
				sbSql.append(", '").append(cm.getName()).append("', 1, 1, 1)");

				updCount = stmnt.executeUpdate(sbSql.toString());
				incInserts();
			}
			else
			{
				incUpdates();
			}
		}
		catch (SQLException e)
		{
			_logger.warn("Error writing to Persistent Counter Store. SQL: "+sbSql.toString(), e);
		}
	}

	private int save(CountersModel cm, int whatData, Timestamp sessionStartTime, Timestamp sessionSampleTime)
	{
		if (_conn == null)
		{
			//_logger.error("No database connection to Persistent Storage DB.'");
			return -1;
		}
		
		Object       colObj    = null;
		StringBuffer sqlSb     = new StringBuffer();
//		StringBuffer rowSb     = new StringBuffer();

		List<List<Object>> rows = cm.getDataCollection(whatData);
		List<String>       cols = cm.getColNames(whatData);

		if (rows == null || cols == null)
		{
			_logger.error("Rows or Columns cant be null. rows='"+rows+"', cols='"+cols+"'");
			return -1;
		}

		String tabName = cm.getName();
		if      (whatData == CountersModel.DATA_ABS)  tabName += "_abs";
		else if (whatData == CountersModel.DATA_DIFF) tabName += "_diff";
		else if (whatData == CountersModel.DATA_RATE) tabName += "_rate";
		else
		{
			_logger.error("Type of data is unknown, only 'ABD', 'DIFF' and 'RATE' is handled.");
			return -1;
		}

		int rowsCount = rows.size();
		int colsCount = cols.size();
		
		// First BUILD up SQL statement used for the insert
		sqlSb.append("insert into ").append(qic).append(tabName).append(qic);
		sqlSb.append(" values(?, ?, ?, ?");
		for (int c=0; c<colsCount; c++)
			sqlSb.append(", ?");
		sqlSb.append(")");

		try
		{
			PreparedStatement pstmt = _conn.prepareStatement(sqlSb.toString());

			// Loop all rows, and ADD them to the Prepared Statement
			for (int r=0; r<rowsCount; r++)
			{
//System.out.print("tabname='"+tabName+"': ");
				int col = 1;
				// Add sessionStartTime as the first column
//				pstmt.setTimestamp(col++, sessionStartTime);
				pstmt.setString(col++, sessionStartTime.toString());

				// Add sessionSampleTime as the first column
//				pstmt.setTimestamp(col++, sessionSampleTime);
				pstmt.setString(col++, sessionSampleTime.toString());

				// When THIS sample was taken
				// probably the same time as parentSampleTime, but it can vary some milliseconds or so
//				pstmt.setTimestamp(col++, cm.getTimestamp());
				pstmt.setString(col++, cm.getTimestamp().toString());

				// How long the sample was for, in Milliseconds
				pstmt.setInt(col++, cm.getLastSampleInterval());

				// loop all columns
				for (int c=0; c<colsCount; c++)
				{
					colObj =  rows.get(r).get(c);

					// Timestamp is stored with appending nanoseconds etc in a strange format
					// if you are using setObject() so use setString() instead...
					if (colObj != null && colObj instanceof Timestamp)
					{
						// Also try to parse the date to see if it's ok...
						// some timestamp seems to be "corrupt"...
						Timestamp ts = (Timestamp) colObj;
						String dateStr = colObj.toString();

						Calendar cal = Calendar.getInstance();
						cal.setTime(ts);
						int year = cal.get(Calendar.YEAR);
						if (year > 9999)
						{
							String colName = cm.getColumnName(col);
							_logger.warn("Date problems for table '"+tabName+"', column '"+colName+"', Timestamp value '"+dateStr+"', Year seems to be out of whack, replacing this with NULL.");
							pstmt.setString(col++, null);
						}
						else
						{
							pstmt.setString(col++, dateStr);
						}
					}
					else
						pstmt.setObject(col++, colObj);
				}
				
				// ADD the row to the BATCH
				pstmt.addBatch();
				incInserts();
			} // end: loop rows
	
			pstmt.executeBatch();
			pstmt.close();

			return rowsCount;
		}
		catch (SQLException e)
		{
			_logger.warn("Error writing to Persistent Counter Store. to table name '"+tabName+"'.", e);
			return -1;
		}
	}

//	private int save(CountersModel cm, int whatData, Timestamp sessionStartTime, Timestamp sessionSampleTime)
//	{
//		if (_conn == null)
//		{
//			//_logger.error("No database connection to Persistent Storage DB.'");
//			return -1;
//		}
//		
//		Object       colObj    = null;
//		StringBuffer rowSb     = new StringBuffer();
//
//		Vector rows = cm.getDataVector(whatData);
//		Vector cols = cm.getColNames(whatData);
//
//		if (rows == null || cols == null)
//		{
//			_logger.error("Rows or Columns cant be null. rows='"+rows+"', cols='"+cols+"'");
//			return -1;
//		}
//
//		String tabName = cm.getName();
//		if      (whatData == CountersModel.DATA_ABS)  tabName += "_abs";
//		else if (whatData == CountersModel.DATA_DIFF) tabName += "_diff";
//		else if (whatData == CountersModel.DATA_RATE) tabName += "_rate";
//		else
//		{
//			_logger.error("Type of data is unknown, only 'ABD', 'DIFF' and 'RATE' is handled.");
//			return -1;
//		}
//
//		int rowsCount = rows.size();
//		int colsCount = cols.size();
//		
//		// Loop all rows
//		for (int r=0; r<rowsCount; r++)
//		{
//			// Compose 1 row 
//			rowSb.setLength(0);
//
//			rowSb.append("insert into ").append(qic).append(tabName).append(qic);
//			rowSb.append(" values(");
//
//			// Add sessionStartTime as the first column
//			rowSb.append("'").append(sessionStartTime.toString()).append("', ");
//
//			// Add sessionSampleTime as the first column
//			rowSb.append("'").append(sessionSampleTime.toString()).append("', ");
//
//			// When THIS sample was taken
//			// probably the same time as parentSampleTime, but it can vary some milliseconds or so
//			rowSb.append("'").append(cm.getTimestamp().toString()).append("', ");
//
//			// How long the sample was for, in Milliseconds
//			rowSb.append(cm.getLastSampleInterval()).append(", ");
//
//			// loop all columns
//			for (int c=0; c<colsCount; c++)
//			{
//				colObj =  ((Vector)rows.get(r)).get(c);
//
//				if (colObj == null)
//					rowSb.append("NULL");
//				else
//				{
//					if (colObj instanceof Number)
//					{
//						rowSb.append(colObj);
//					}
//					else
//					{
//						rowSb.append("'").append(colObj).append("'");
//					}
//				}
//
//				// No colSep on last column
//				if ( (c+1) == colsCount )
//				{
//					// nothing
//					rowSb.append(")");
//				}
//				else
//				{
//					rowSb.append(", ");
//				}
//			}
//			
//			// Write that row
//			if (rowSb.length() > 0)
//			{
//				try
//				{
//					//--------------------
//					// Send the SQL to the database.
//					//--------------------
//					dbExec(rowSb.toString());
//				}
//				catch (SQLException e)
//				{
//					_logger.warn("Error writing to Persistent Counter Store.", e);
//					return -1;
//				}
//			}
//		} // end: loop rows
//		return rowsCount;
//	}

	
	private void saveGraphData(CountersModel cm, TrendGraphDataPoint tgdp, Timestamp sessionStartTime, Timestamp sessionSampleTime)
	{
		String tabName = cm.getName() + "_" + tgdp.getName();

		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(qic).append(tabName).append(qic);
		sb.append(" values(");

		// Add sessionStartTime as the first column
		sb.append("'").append(sessionStartTime).append("', ");

		// Add sessionSampleTime as the first column
		sb.append("'").append(sessionSampleTime).append("', ");

		sb.append("'").append(tgdp.getDate()).append("', ");

		// loop all data
		Double[] dataArr  = tgdp.getData();
		String[] labelArr = tgdp.getLabel();
		if (dataArr  == null) throw new RuntimeException("The CM '"+cm.getName()+"', graph '"+tgdp.getName()+"' has a null pointer for it's DATA array.");
		if (labelArr == null) throw new RuntimeException("The CM '"+cm.getName()+"', graph '"+tgdp.getName()+"' has a null pointer for it's LABEL array.");
		for (int d=0; d<dataArr.length; d++)
		{
			Double data  = dataArr[d];
			String label = null;
			if (d < labelArr.length)
				label = labelArr[d];

			if (label == null)
				sb.append("NULL, ");
			else
				sb.append("'").append(label).append("', ");

			if (data == null)
				sb.append("NULL");
			else
				sb.append(data);

			// No colSep on last column
			if ( (d+1) == dataArr.length )
				sb.append(")");
			else
				sb.append(", ");
		}
		//--------------------
		// Send the SQL to the database.
		//--------------------

		// CHECK/Create table
		try
		{
			if ( ! isDdlCreated(tabName) )
				saveGraphDataDdl(tabName, tgdp);
			markDdlAsCreated(tabName);
		}
		catch (SQLException e)
		{
			_logger.info("Problems writing Graph '"+tgdp.getName()+"' information to table '"+tabName+"', Problems when creating the table or checked if it existed. Caught: "+e);
		}

		// Add rows...
		try
		{
			dbExec(sb.toString(), false);
			incInserts();
		}
		catch (SQLException e)
		{
			_logger.info("Problems writing Graph '"+tgdp.getName()+"' information to table '"+tabName+"', This probably happens if series has been added to the graph, I will checking/create/alter the table and try again.");
			try
			{
				// we probably need to alter the table...
				saveGraphDataDdl(tabName, tgdp);
				dbExec(sb.toString());
				incInserts();
			}
			catch (SQLException e2)
			{
				_logger.warn("Error writing to Persistent Counter Store. SQL: "+sb.toString(), e2);
			}
		}
	}

	private void saveGraphDataDdl(String tabName, TrendGraphDataPoint tgdp)
	throws SQLException
	{
		ResultSet rs = null;

		// Obtain a DatabaseMetaData object from our current connection
		DatabaseMetaData dbmd = _conn.getMetaData();

		// If NOT in autocommit (it means that we are in a transaction)
		// Creating tables and some other operations is NOT allowed in a transaction
		// So:
		//  - commit the transaction
		//  - do the work (create table)
		//  - start a new transaction again
		// Yes, this is a bit uggly, but lets do it anyway...
		boolean inTransaction = (_conn.getAutoCommit() == false);
		try
		{
			if (inTransaction)
			{
				_logger.debug("Looks like we are in a transaction, temporary committing it, then create the table and start a new transaction.");
				_conn.commit();
				_conn.setAutoCommit(true);
			}
			
			rs = dbmd.getColumns(null, null, tabName, "%");
			boolean tabExists = rs.next();
			rs.close();

			if( ! tabExists )
			{
				_logger.info("Persistent Counter DB: Creating table "+StringUtil.left("'"+tabName+"'", 32, true)+" for CounterModel graph '" + tgdp.getName() + "'.");

				String sqlTable = getGraphTableDdlString(tabName, tgdp);
				String sqlIndex = getGraphIndexDdlString(tabName, tgdp);

				dbDdlExec(sqlTable);
				dbDdlExec(sqlIndex);
				
				incCreateTables();
			}
			else // Check if we need to add any new columns
			{
				String sqlAlterTable = getGraphAlterTableDdlString(_conn, tabName, tgdp);
				if ( ! sqlAlterTable.trim().equals("") )
				{
					_logger.info("Persistent Counter DB: Altering table '"+tabName+"' for CounterModel graph '" + tgdp.getName() + "'.");

					dbDdlExec(sqlAlterTable);
					incAlterTables();
				}
			}
		}
		catch (SQLException e)
		{
			throw e;
		}
		finally
		{
			// Start the transaction again
			if (inTransaction)
			{
				_logger.debug("Looks like we are in a transaction. Done with creating the table, so starting a transaction again.");
				_conn.setAutoCommit(false);
			}
		}
	}	
}

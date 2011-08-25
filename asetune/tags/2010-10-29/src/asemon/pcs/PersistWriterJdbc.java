/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.pcs;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

import asemon.TrendGraphDataPoint;
import asemon.cm.CountersModel;
import asemon.cm.CountersModelAppend;
import asemon.utils.Configuration;

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

	protected String _jdbcDriver = "org.h2.Driver";
	protected String _jdbcUrl    = "jdbc:h2:pcdb_yyy";
	protected String _jdbcUser   = "sa";
	protected String _jdbcPasswd = "";
	
	private   boolean _startH2NetworkServer = false;

	protected String _name       = "PersistWriterJdbc";
	
	protected boolean _jdbcDriverInfoHasBeenWritten = false;


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

	/** Empty implementation */
	public void endOfSample()
	{
		close();
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
		
		_jdbcDriver = props.getProperty(propPrefix+"jdbcDriver", "org.h2.Driver");
		_jdbcUrl    = props.getProperty(propPrefix+"jdbcUrl",    "jdbc:h2:pcdb");
		_jdbcUser   = props.getProperty(propPrefix+"jdbcUser",   "sa");
		_jdbcPasswd = props.getProperty(propPrefix+"jdbcPasswd", "");
		if (_jdbcPasswd.equalsIgnoreCase("null"))
			_jdbcPasswd="";

		_startH2NetworkServer = props.getBooleanProperty(propPrefix+"startH2NetworkServer", _startH2NetworkServer);

		String configStr = "jdbcDriver='"+_jdbcDriver+"', jdbcUrl='"+_jdbcUrl+"', jdbcUser='"+_jdbcUser+"', jdbcPasswd='*hidden*'.";
		_logger.info("Configuration for PersistentCounterHandler.WriterClass component named '"+_name+"': "+configStr);

		// Everything could NOT be done with the jdbcUrl... so here goes some special
		// start the H2 TCP Server
		if ( _jdbcDriver.equals("org.h2.Driver") && _startH2NetworkServer )
		{
			_logger.info("Starting a H2 TCP server.");
			String[] args = new String[] { "-tcpAllowOthers" };
			org.h2.tools.Server h2ServerTcp = org.h2.tools.Server.createTcpServer(args);
			h2ServerTcp.start();

//			_logger.info("H2 TCP server, listening on port='"+h2Server.getPort()+"', url='"+h2Server.getURL()+"', service='"+h2Server.getService()+"'.");
			_logger.info("H2 TCP server, url='"+h2ServerTcp.getURL()+"', service='"+h2ServerTcp.getService()+"'.");

			if (true)
			{
				_logger.info("Starting a H2 WEB server.");
				//String[] argsWeb = new String[] { "-trace" };
				String[] argsWeb = new String[] { "" };
				org.h2.tools.Server h2ServerWeb = org.h2.tools.Server.createWebServer(argsWeb);
				h2ServerWeb.start();

				_logger.info("H2 WEB server, url='"+h2ServerWeb.getURL()+"', service='"+h2ServerWeb.getService()+"'.");
			}
		}

	}

	protected void finalize() throws Throwable
	{
		super.finalize();
		
		close();
	}

	public void close()
	{
		if (_conn == null)
			return;
		
		try { _conn.close(); }
		catch(Exception ignore){}
	}

	
	private Connection open()
	{
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
    				Enumeration drvEnum = DriverManager.getDrivers();
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
				}
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
	
			if( ! tabExists )
			{
				_logger.info("Creating table '" + tabName + "'.");
				
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
	}

	public void startSession(PersistContainer cont)
	{
		// Open connection to db
		open();

		if (_conn == null)
		{
			_logger.error("No database connection to Persistant Storage DB.'");
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
			checkAndCreateTable(SESSIONS);
			checkAndCreateTable(SESSION_PARAMS);
			checkAndCreateTable(SESSION_SAMPLES);
			checkAndCreateTable(SESSION_SAMPLE_SUM);
			checkAndCreateTable(SESSION_SAMPLE_DETAILES);
			
			//--------------------------
			// Now fill in some data
			String tabName = getTableName(SESSIONS, null, true);

			StringBuffer sbSql = new StringBuffer();
			sbSql.append(" insert into ").append(tabName);
			sbSql.append(" values('").append(cont.getSessionStartTime()).append("', '").append(cont.getServerName()).append("', 0, null)");

			dbExec(sbSql.toString());
			

			_logger.info("Storing CounterModel information in table "+getTableName(SESSION_PARAMS, null, false));
			//--------------------------------
			// LOOP ALL THE CM's and store some information
			tabName = getTableName(SESSION_PARAMS, null, true);
			Timestamp ts = cont.getSessionStartTime();

			Iterator it = cont._counterObjects.iterator();
			while (it.hasNext()) 
			{
				CountersModel cm = (CountersModel) it.next();
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
			Configuration conf = Configuration.getInstance(Configuration.CONF); 
			it = conf.keySet().iterator();
			while (it.hasNext()) 
			{
				String key = (String)it.next();
				String val = conf.getPropertyRaw(key);

				insertSessionParam(ts, "config", key, val);
			}
			conf = Configuration.getInstance(Configuration.TEMP);
			it = conf.keySet().iterator();
			while (it.hasNext()) 
			{
				String key = (String)it.next();
				String val = conf.getPropertyRaw(key);

				insertSessionParam(ts, "tmpConfig", key, val);
			}
		}
		catch (SQLException e)
		{
			_logger.warn("Error when startSession() writing to Persistent Counter Store.", e);
		}
		
		// Close connection to db
		close();
	}

	public void saveSample(PersistContainer cont)
	{
		if (_conn == null)
		{
			_logger.error("No database connection to Persistant Storage DB.'");
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

			// Increment the "counter" column and set LastSampleTime in the SESSIONS table
			tabName = getTableName(SESSIONS, null, true);
			sbSql = new StringBuffer();
			sbSql.append(" update ").append(tabName);
			sbSql.append("    set ").append(qic).append("NumOfSamples")  .append(qic).append(" = ").append(qic).append("NumOfSamples").append(qic).append(" + 1,");
			sbSql.append("        ").append(qic).append("LastSampleTime").append(qic).append(" = '").append(sessionSampleTime).append("'");
			sbSql.append("  where ").append(qic).append("SessionStartTime").append(qic).append(" = '").append(sessionStartTime).append("'");

			dbExec(sbSql.toString());

			//--------------------------------------
			// COUNTERS
			//--------------------------------------
			Iterator cmIter = cont._counterObjects.iterator();
			while (cmIter.hasNext()) 
			{
				CountersModel cm = (CountersModel) cmIter.next();
				
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

			_logger.warn("Error writing to Persistent Counter Store.", e);
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
			_logger.info("Persistant Counter DB: Creating table '"+tabName+"' for CounterModel '" + cm.getName() + "'.");

			String sqlTable = getTableDdlString(type, cm);
			String sqlIndex = getIndexDdlString(type, cm);

			dbDdlExec(sqlTable);
			dbDdlExec(sqlIndex);
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
			_logger.warn("SQLException, Error writing Ddl to Persistant Counter DB.", e);
			throw new RuntimeException("SQLException, Error writing Ddl to Persistant Counter DB. Caught: "+e);
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

		if ( ! cm.hasDiffData() )
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
		Map tgdMap = cm.getTrendGraphData();
		if (tgdMap != null)
		{
			for (Iterator it = tgdMap.keySet().iterator(); it.hasNext();)
			{
				String              key  = (String) it.next();
				TrendGraphDataPoint tgdp = (TrendGraphDataPoint) tgdMap.get(key);

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
		}
		catch (SQLException e)
		{
			_logger.warn("Error writing to Persistent Counter Store. SQL="+sbSql.toString(), e);
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
			}
		}
		catch (SQLException e)
		{
			_logger.warn("Error writing to Persistent Counter Store. SQL="+sbSql.toString(), e);
		}
	}

	private int save(CountersModel cm, int whatData, Timestamp sessionStartTime, Timestamp sessionSampleTime)
	{
		if (_conn == null)
		{
			//_logger.error("No database connection to Persistant Storage DB.'");
			return -1;
		}
		
		Object       colObj    = null;
		StringBuffer sqlSb     = new StringBuffer();
//		StringBuffer rowSb     = new StringBuffer();

		Vector rows = cm.getDataVector(whatData);
		Vector cols = cm.getColNames(whatData);

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
					colObj =  ((Vector)rows.get(r)).get(c);
					// Timestamp is stored with appending nanoseconds etc in a strange format
					// if you are using setObject() so use setString() instead...
					if (colObj != null && colObj instanceof Timestamp)
						pstmt.setString(col++, colObj.toString());
					else
						pstmt.setObject(col++, colObj);
				}
				
				// ADD the row to the BATCH
				pstmt.addBatch();
			} // end: loop rows
	
			pstmt.executeBatch();
			pstmt.close();

			return rowsCount;
		}
		catch (SQLException e)
		{
			_logger.warn("Error writing to Persistant Counter Store.", e);
			return -1;
		}
	}

//	private int save(CountersModel cm, int whatData, Timestamp sessionStartTime, Timestamp sessionSampleTime)
//	{
//		if (_conn == null)
//		{
//			//_logger.error("No database connection to Persistant Storage DB.'");
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
//					_logger.warn("Error writing to Persistant Counter Store.", e);
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
		//System.out.println(sb);
		try
		{
			dbExec(sb.toString(), false);
		}
		catch (SQLException e)
		{
			_logger.info("Problems writing Graph '"+tgdp.getName()+"' information to table '"+tabName+"', This probably happens first time, I will checking/create/alter the table and try again.");
			try
			{
				saveGraphDataDdl(tabName, tgdp);
				dbExec(sb.toString());
			}
			catch (SQLException e2)
			{
				_logger.warn("Error writing to Persistant Counter Store.", e2);
			}
		}
	}

	private void saveGraphDataDdl(String tabName, TrendGraphDataPoint tgdp)
	throws SQLException
	{
		ResultSet rs = null;

		// Obtain a DatabaseMetaData object from our current connection
		DatabaseMetaData dbmd = _conn.getMetaData();


		rs = dbmd.getColumns(null, null, tabName, "%");
		boolean tabExists = rs.next();
		rs.close();

		if( ! tabExists )
		{
			_logger.info("Persistant Counter DB: Creating table '"+tabName+"' for CounterModel graph '" + tgdp.getName() + "'.");

			String sqlTable = getGraphTableDdlString(tabName, tgdp);
			String sqlIndex = getGraphIndexDdlString(tabName, tgdp);

			dbDdlExec(sqlTable);
			dbDdlExec(sqlIndex);
		}
		else // Check if we need to add any new columns
		{
			_logger.info("Persistant Counter DB: Altering table '"+tabName+"' for CounterModel graph '" + tgdp.getName() + "'.");

			String sqlAlterTable = getGraphAlterTableDdlString(_conn, tabName, tgdp);
			dbDdlExec(sqlAlterTable);
		}
	}	
}

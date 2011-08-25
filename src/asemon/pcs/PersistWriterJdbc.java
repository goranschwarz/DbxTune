/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.pcs;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;

import asemon.CountersModel;
import asemon.CountersModelAppend;
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
	private Connection _conn = null;

	private String _jdbcDriver = "org.h2.Driver";
	private String _jdbcUrl    = "jdbc:h2:pcdb_yyy";
	private String _jdbcUser   = "sa";
	private String _jdbcPasswd = "";
	
	private boolean _startH2NetworkServer = false;

	private String _name       = "PersistWriterJdbc";
	
	private boolean _jdbcDriverInfoHasBeenWritten = false;


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
			_logger.info("Starting a H2 TCP server, listening on port ????");
			String[] args = new String[] { "-tcpAllowOthers" };
			org.h2.tools.Server h2Server = org.h2.tools.Server.createTcpServer(args);
			h2Server.start();
		}

	}

	protected void finalize() throws Throwable
	{
		super.finalize();
		
		close();
	}

	private void close()
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
			_conn = DriverManager.getConnection(_jdbcUrl, _jdbcUser, _jdbcPasswd);

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
	
	public void saveSession(PersistContainer cont)
	{
		if (_conn == null)
		{
			_logger.error("No database connection to Persistant Storage DB.'");
			return;
		}

		Timestamp parentSampleTime = cont.getSampleTime();
		String    aseServerName    = cont.getServerName();

		
		StringBuffer sbSql = null;

		try
		{
			//
			// FIRST CHECK IF THE TABLE EXISTS, IF NOT CREATE IT
			//
			String tabName = getTableName(super.SESSION, null);

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
					
					String sql = getTableDdlString(super.SESSION, null);

					dbDdlExec(sql);
				}
				
				markDdlAsCreated(tabName);
			}

			// START a transaction
			// This will lower number of IO's to the transaction log
			if (_conn.getAutoCommit() == true)
				_conn.setAutoCommit(false);

			//
			// INSERT THE ROW
			//
			sbSql = new StringBuffer();
			sbSql.append("insert into ");
			sbSql.append(qic);
			sbSql.append(tabName);
			sbSql.append(qic);
			sbSql.append(" values('");
			sbSql.append(parentSampleTime);
			sbSql.append("', '");
			sbSql.append(aseServerName);
			sbSql.append("')");

			dbExec(sbSql.toString());

			//--------------------------------------
			// COUNTERS
			//--------------------------------------
			Iterator cmIter = cont._counterObjects.iterator();
			while (cmIter.hasNext()) 
			{
				CountersModel cm = (CountersModel) cmIter.next();
				
				saveCounterData(cm, parentSampleTime, aseServerName);
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


		tabName = getTableName(type, cm);

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
			saveDdl(super.ABS, cm);
			saveDdl(super.DIFF, cm);
			saveDdl(super.RATE, cm);
		}
		catch (SQLException e)
		{
			_logger.warn("SQLException, Error writing Ddl to Persistant Counter DB.", e);
			return false;
		}

		return true;
  	} // end: method

	
	/**
	 * Save the counters in the database
	 * 
	 * @param cm
	 * @param parentSampleTime
	 * @param aseServerName
	 */
	private void saveCounterData(CountersModel cm, Timestamp parentSampleTime, String aseServerName)
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

		save(cm, CountersModel.DATA_ABS,  parentSampleTime, aseServerName);
		save(cm, CountersModel.DATA_DIFF, parentSampleTime, aseServerName);
		save(cm, CountersModel.DATA_RATE, parentSampleTime, aseServerName);
	}

	private void save(CountersModel cm, int whatData, Timestamp parentSampleTime, String aseServerName)
	{
		if (_conn == null)
		{
			//_logger.error("No database connection to Persistant Storage DB.'");
			return;
		}
		
		Object       colObj    = null;
		StringBuffer rowSb     = new StringBuffer();

		Vector rows = cm.getDataVector(whatData);
		Vector cols = cm.getColNames(whatData);

		if (rows == null || cols == null)
		{
			_logger.error("Rows or Columns cant be null. rows='"+rows+"', cols='"+cols+"'");
			return;
		}

		String tabName = cm.getName();
		if      (whatData == CountersModel.DATA_ABS)  tabName += "_abs";
		else if (whatData == CountersModel.DATA_DIFF) tabName += "_diff";
		else if (whatData == CountersModel.DATA_RATE) tabName += "_rate";
		else
		{
			_logger.error("Type of data is unknown, only 'ABD', 'DIFF' and 'RATE' is handled.");
			return;
		}

		int rowsCount = rows.size();
		int colsCount = cols.size();
		
		// Loop all rows
		for (int r=0; r<rowsCount; r++)
		{
			// Compose 1 row 
			rowSb.setLength(0);

			rowSb.append("insert into ");
			rowSb.append(qic);
			rowSb.append(tabName);
			rowSb.append(qic);
			rowSb.append(" values(");

			// Add sqlSaveTime as the first column
			rowSb.append("'");
			rowSb.append(parentSampleTime.toString());
			rowSb.append("', ");

			// When THIS sample was taken
			// probably the same time as parentSampleTime, but it can vary some milliseconds or so
			rowSb.append("'");
			rowSb.append(cm.getTimestamp().toString());
			rowSb.append("', ");

			// How long the sample was for, in Milliseconds
			rowSb.append(cm.getLastSampleInterval());
			rowSb.append(", ");

			// Name of the ASE Server the sample was taken from
			rowSb.append("'");
			rowSb.append(aseServerName);
			rowSb.append("', ");

			// loop all columns
			for (int c=0; c<colsCount; c++)
			{
				colObj =  ((Vector)rows.get(r)).get(c);

				if (colObj == null)
					rowSb.append("NULL");
				else
				{
					if (colObj instanceof Number)
					{
						rowSb.append(colObj);
					}
					else
					{
						rowSb.append("'");
						rowSb.append(colObj);
						rowSb.append("'");
					}
				}

				// No colSep on last column
				if ( (c+1) == colsCount )
				{
					// nothing
					rowSb.append(")");
				}
				else
				{
					rowSb.append(", ");
				}
			}
			
			// Write that row
			if (rowSb.length() > 0)
			{
				try
				{
					//--------------------
					// Send the SQL to the database.
					//--------------------
					dbExec(rowSb.toString());
				}
				catch (SQLException e)
				{
					_logger.warn("Error writing to Persistant Counter Store.", e);
				}
			}
		} // end: loop rows
	}
}

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
package com.dbxtune.cm.rs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.DbxTuneResultSetMetaData;
import com.dbxtune.cm.rs.RsStatCounterDictionary.StatCounterEntry;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.AseSqlScript;
import com.dbxtune.utils.StringUtil;

public class CounterSampleAdminStats
extends CounterSample
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	private static DbxTuneResultSetMetaData _xrstm = new DbxTuneResultSetMetaData();

	/**
	 * List of all Instances that has been added
	 */
	private List<Instance> _instanceList  = new ArrayList<Instance>();

	/**
	 * This is the current instance, which we add Obesrver, Monitor and Counter objects to.
	 */
	private Instance       _currentInstance = null;

	static
	{
		_xrstm.addStrColumn ("Instance",       1,  false, 255, "FIXME: description");
		_xrstm.addIntColumn ("InstanceId",     2,  false,      "FIXME: description");
		_xrstm.addIntColumn ("ModTypeInstVal", 3,  false,      "FIXME: description");
		_xrstm.addStrColumn ("Type",           4,  false,  10, "FIXME: description");
		_xrstm.addStrColumn ("Name",           5,  false,  31, "FIXME: description");
		_xrstm.addLongColumn("Obs",            6,  false,      "FIXME: description");
		_xrstm.addLongColumn("Total",          7,  false,      "FIXME: description");
		_xrstm.addLongColumn("Last",           8,  false,      "FIXME: description");
		_xrstm.addLongColumn("Max",            9,  false,      "FIXME: description");
		_xrstm.addLongColumn("AvgTtlObs",      10, false,      "FIXME: description");
		_xrstm.addLongColumn("RateXsec",       11, false,      "FIXME: description");

		_xrstm.addStrColumn ("Module",         12, false,  10, "FIXME: description");
		_xrstm.addIntColumn ("CounterId",      13, false,      "FIXME: description");
		_xrstm.addStrColumn ("CounterStatus",  14, false, 255, "FIXME: description");
		_xrstm.addStrColumn ("CounterDescr",   15, false, 255, "FIXME: description");

		_xrstm.setPkCol("Instance", "Type", "Name");
	}

	public CounterSampleAdminStats(String name, boolean negativeDiffCountersToZero, String[] diffColNames, CounterSample prevSample)
	{
		super(name, negativeDiffCountersToZero, diffColNames, prevSample);
	}

	protected List<Instance> getInstanceList()
	{
		return _instanceList;
	}
	/*---------------------------------------------------------------------------------------------------
	 * ResultSetMetaData should look like the below
	 * 
	RS> 1    Instance        java.sql.Types.CHAR    char(255)        
	RS> 2    Instance ID     java.sql.Types.INTEGER int              
	RS> 3    ModType/InstVal java.sql.Types.INTEGER int              

	RS> 1    Counter     java.sql.Types.CHAR    char(31)         
	RS> 2    Obs         java.sql.Types.NUMERIC numeric(22,0)    
	RS> 3    Total       java.sql.Types.NUMERIC numeric(22,0)    
	RS> 4    Last        java.sql.Types.NUMERIC numeric(22,0)    
	RS> 5    Max         java.sql.Types.NUMERIC numeric(22,0)    
	RS> 6    Avg ttl/obs java.sql.Types.NUMERIC numeric(22,0)    
	RS> 7    Rate x/sec  java.sql.Types.NUMERIC numeric(22,0)    


	********                                              *****    ****
	Instance                Instance ID  ModType/InstVal  TYPE     Name  Obs  Total  Last  Max  Avg ttl/obs  Rate x/sec
	----------------------- ----------- ----------------- -------- ----- ---- ------ ----- ---- ------------ ------------
	                                                      OBSERVER     x    x      -     -    -            -            x
	                                                      COUNTER      x    x      x     x    x            x            x
	                                                      MONITOR      x    x      -     x    x            x            -
	 
	 *---------------------------------------------------------------------------------------------------
	 */
	@Override
	public boolean getSample(CountersModel cm, DbxConnection conn, String sql, List<String> pkList) throws SQLException
	{
		// Create/Initialize the dictionary if it hasn't been done yet.
		if ( ! RsStatCounterDictionary.hasInstance() )
		{
			RsStatCounterDictionary newDict = new RsStatCounterDictionary();
			RsStatCounterDictionary.setInstance(newDict);
			newDict.init(conn);
		}
		if ( ! RsStatCounterDictionary.getInstance().isInitialized() )
		{
			RsStatCounterDictionary.getInstance().init(conn);
		}
		
		setColumnNames (_xrstm.getColumnNames());
		setSqlType     (_xrstm.getSqlTypes());
		setSqlTypeNames(_xrstm.getSqlTypeNames());
		setColClassName(_xrstm.getClassNames());

		setPkColArray(_xrstm.getPkColArray());

		initPkStructures();

		if ( ! cm.hasResultSetMetaData() )
			cm.setResultSetMetaData(_xrstm);

		int queryTimeout = cm.getQueryTimeout();
		if (_logger.isDebugEnabled())
			_logger.debug(_name+": queryTimeout="+queryTimeout);

		try
		{
			String srvTimeCmd = cm.getServerTimeCmd();
			if (StringUtil.isNullOrBlank(srvTimeCmd))
				srvTimeCmd = "";
//			String sendSql = "select getdate() \n" + sql;
			String sendSql = srvTimeCmd + sql;

			Statement stmnt = conn.createStatement();
			ResultSet rs;

			stmnt.setQueryTimeout(queryTimeout); // XX seconds query timeout
			if (_logger.isDebugEnabled())
				_logger.debug("QUERY_TIMEOUT="+queryTimeout+", for SampleCnt='"+_name+"'.");

			_rows   = new ArrayList<List<Object>>(getColumnCount());

			// Allow 'go' in the string, then we should send multiple batches
			// this will take care about dropping tempdb tables prior to executing a batch that depends on it.
			// is a query batch we can't do:
			//     if ((select object_id('#cacheInfo')) is not null) drop table #cacheInfo 
			//     select CacheName, CacheID into #cacheInfo from master..monCachePool 
			// The second row will fail...
			//     Msg 12822, Level 16, State 1:
			//     Server 'GORAN_1_DS', Line 5, Status 0, TranState 0:
			//     Cannot create temporary table '#cacheInfo'. Prefix name '#cacheInfo' is already in use by another temporary table '#cacheInfo'.
			// So we need to send the statemenmts in two separate batches
			// so instead do:
			//     if ((select object_id('#cacheInfo')) is not null) drop table #cacheInfo 
			//     go
			//     select CacheName, CacheID into #cacheInfo from master..monCachePool 
			// Then it works...


			// treat each 'go' rows as a individual execution
			// readCommand(), does the job
			//int batchCount = AseSqlScript.countSqlGoBatches(sendSql);
			int batchCounter = 0;
			BufferedReader br = new BufferedReader( new StringReader(sendSql) );
			for(String sqlBatch=AseSqlScript.readCommand(br); sqlBatch!=null; sqlBatch=AseSqlScript.readCommand(br))
			{
				sendSql = sqlBatch;

				if (_logger.isDebugEnabled())
				{
					_logger.debug("##### BEGIN (send sql), batchCounter="+batchCounter+" ############################### "+ getName());
					_logger.debug(sendSql);
					_logger.debug("##### END   (send sql), batchCounter="+batchCounter+" ############################### "+ getName());
					_logger.debug("");
				}

	
				int rsNum = 0;
				int rowsAffected = 0;
				boolean hasRs = stmnt.execute(sendSql);
				checkWarnings(cm, stmnt);
				do
				{
					if (hasRs)
					{
						// Get next result set to work with
						rs = stmnt.getResultSet();
						checkWarnings(cm, stmnt);

						// first resultset in first command batch, will be the "select getdate()"
						if (rsNum == 0 && batchCounter == 0)
						{
							while(rs.next())
								_samplingTime = rs.getTimestamp(1);
							
							_interval = 0;
							if (_prevSample != null)
							{
								_interval = getSampleTimeAsLong() - _prevSample.getSampleTimeAsLong();
								
								// if _prevSample is not used any further, reset this pointer here
								// If this is NOT done we will have a memory leek...
								// If _prevSample is used somewhere else, please reset this pointer later
								//    and check memory consumption under 24 hours sampling...
								_prevSample = null;
							}
						}
						else
						{
							ResultSetMetaData rsmd = rs.getMetaData();
//							if ( ! cm.hasResultSetMetaData() )
//								cm.setResultSetMetaData(rsmd);
	
							if (readResultset(cm, rs, rsmd, rsmd, pkList, rsNum))
								rs.close();
	
							checkWarnings(cm, stmnt);
						}
	
						rsNum++;
					}
					else
					{
						// Treat update/row count(s)
						rowsAffected = stmnt.getUpdateCount();

						if (rowsAffected >= 0)
						{
							_logger.debug("DDL or DML rowcount = "+rowsAffected);
						}
						else
						{
							_logger.debug("No more results to process.");
						}
					}
	
					// Check if we have more result sets
					hasRs = stmnt.getMoreResults();
	
					_logger.trace( "--hasRs="+hasRs+", rsNum="+rsNum+", rowsAffected="+rowsAffected );
				}
				while (hasRs || rowsAffected != -1);
	
				checkWarnings(cm, stmnt);
				batchCounter++;
			}
			br.close();

			// Close the statement
			stmnt.close();

			/*---------------------------------------------------------------------------------------------------
			 * ResultSetMetaData should look like the below
			 * 
			RS> 1    Instance        java.sql.Types.CHAR    char(255)        
			RS> 2    Instance ID     java.sql.Types.INTEGER int              
			RS> 3    ModType/InstVal java.sql.Types.INTEGER int              

			RS> 1    Counter     java.sql.Types.CHAR    char(31)         
			RS> 2    Obs         java.sql.Types.NUMERIC numeric(22,0)    
			RS> 3    Total       java.sql.Types.NUMERIC numeric(22,0)    
			RS> 4    Last        java.sql.Types.NUMERIC numeric(22,0)    
			RS> 5    Max         java.sql.Types.NUMERIC numeric(22,0)    
			RS> 6    Avg ttl/obs java.sql.Types.NUMERIC numeric(22,0)    
			RS> 7    Rate x/sec  java.sql.Types.NUMERIC numeric(22,0)    


			********                                              *****    ****
			Instance                Instance ID  ModType/InstVal  TYPE     Name  Obs  Total  Last  Max  Avg ttl/obs  Rate x/sec
			----------------------- ----------- ----------------- -------- ----- ---- ------ ----- ---- ------------ ------------
			                                                      OBSERVER     x    x      -     -    -            -            x
			                                                      COUNTER      x    x      x     x    x            x            x
			                                                      MONITOR      x    x      -     x    x            x            -
			 
			 *---------------------------------------------------------------------------------------------------
			 */
			System.out.println("XXXX: _instanceList.size()=" + _instanceList.size() );
//			int emptInstanceCount = 0;
			RsStatCounterDictionary dict = RsStatCounterDictionary.getInstance();
			for (Instance i : _instanceList)
			{
//				if ("".equals(i._name))
//					i._name = "empty-" + (++emptInstanceCount);

				// SKIP empty instances, it looks like it's only on the RSSD RepAgent (if the RSSD isn't primary)
				if ("".equals(i._name))
					continue;

//				System.out.println("INSTANCE: id="+i._id+", name=|"+i._name+"|, val="+i._val+".");
//				System.out.println("          _counterList .size() = "+i._counterList.size());
//				System.out.println("          _monitorList .size() = "+i._monitorList.size());
//				System.out.println("          _observerList.size() = "+i._observerList.size());
				
				for (Counter counter : i._counterMap.values())
				{
					List<Object> row = new ArrayList<Object>(_xrstm.getColumnCount());
					row.add(i._name);
					row.add(i._id);
					row.add(i._val);
					row.add("COUNTER");
					row.add(counter._name);
					row.add(counter._obs);
					row.add(counter._total);
					row.add(counter._last);
					row.add(counter._max);
					row.add(counter._avg_ttl_obs);
					row.add(counter._rate_x_sec);

					StatCounterEntry c = dict.getCounter(i._name, counter._name);
					row.add( c == null ? null : c._moduleName);
					row.add( c == null ? null : c._counterId);
					row.add( c == null ? null : c.getStatusDesc());
					row.add( c == null ? null : c._description);

					addRow(cm, row);
				}

				for (Monitor monitor : i._monitorMap.values())
				{
					List<Object> row = new ArrayList<Object>(_xrstm.getColumnCount());
					row.add(i._name);
					row.add(i._id);
					row.add(i._val);
					row.add("MONITOR");
					row.add(monitor._name);
					row.add(monitor._obs);
					row.add(null);
					row.add(monitor._last);
					row.add(monitor._max);
					row.add(monitor._avg_ttl_obs);
					row.add(null);

					StatCounterEntry c = dict.getCounter(i._name, monitor._name);
					row.add( c == null ? null : c._moduleName);
					row.add( c == null ? null : c._counterId);
					row.add( c == null ? null : c.getStatusDesc());
					row.add( c == null ? null : c._description);

					addRow(cm, row);
				}

				for (Observer observer : i._observerMap.values())
				{
					List<Object> row = new ArrayList<Object>(_xrstm.getColumnCount());
					row.add(i._name);
					row.add(i._id);
					row.add(i._val);
					row.add("OBSERVER");
					row.add(observer._name);
					row.add(observer._obs);
					row.add(null);
					row.add(null);
					row.add(null);
					row.add(null);
					row.add(observer._rate_x_sec);

					StatCounterEntry c = dict.getCounter(i._name, observer._name);
					row.add( c == null ? null : c._moduleName);
					row.add( c == null ? null : c._counterId);
					row.add( c == null ? null : c.getStatusDesc());
					row.add( c == null ? null : c._description);

					addRow(cm, row);
				}
			}

//((CmAdminStats)cm).getModuleCounters("SQM", null);
//((CmAdminStats)cm).getModuleCounters("SQMR", null);
			return true;
		}
		catch (SQLException sqlEx)
		{
			_logger.warn("CounterSample("+_name+").getCnt : ErrorCode=" + sqlEx.getErrorCode() + ", Message=|" + sqlEx.getMessage() + "|. SQL: "+sql, sqlEx);
			if (sqlEx.toString().indexOf("SocketTimeoutException") > 0)
			{
				_logger.info("QueryTimeout in '"+_name+"', with query timeout '"+queryTimeout+"'. This can be changed with the config option '"+_name+".queryTimeout=seconds' in the config file.");
			}

			//return false;
			throw sqlEx;
		}
		catch (IOException ex)
		{
			_logger.error("While reading the input SQL 'go' String, caught: "+ex, ex);
			throw new SQLException("While reading the input SQL 'go' String, caught: "+ex, ex);
		}
	}

	@Override
	protected boolean readResultset(CountersModel cm, ResultSet rs, ResultSetMetaData rsmd, ResultSetMetaData originRsmd, List<String> pkList, int rsNum)
	throws SQLException
	{
		String firstColName = rsmd.getColumnName(1);

		if ( "Instance".equals(firstColName) )
		{
			rs.next();
			_currentInstance = new Instance(rs.getString(1), rs.getInt(2), rs.getInt(3));
			_instanceList.add(_currentInstance);
		}

		else if ( "Observer".equals(firstColName) )
		{
			while (rs.next())
				_currentInstance.addObserver( new Observer(rs.getString(1), rs.getLong(2), rs.getInt(3)) );
		}

		else if ( "Monitor".equals(firstColName) )
		{
			while (rs.next())
				_currentInstance.addMonitor( new Monitor(rs.getString(1), rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5)) );
		}

		else if ( "Counter".equals(firstColName) )
		{
			while (rs.next())
				_currentInstance.addCounter( new Counter(rs.getString(1), rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5), rs.getLong(6), rs.getLong(7)) );
		}

		else
		{
			_logger.warn("Unknown first column name '"+firstColName+"' in the ResultSet");
		}

        return false;
	}	

	/**
	 * Local class to hold a specific instance 
	 */
	public static class Instance
	{
		String         _name;
		int            _id;
		int            _val;
		
//		int            _observerNameDupCnt = 0; 
//		int            _monitorNameDupCnt = 0; 
//		int            _counterNameDupCnt = 0; 
		Map<String, Integer> _observerNameDupCntMap = null; 
		Map<String, Integer> _monitorNameDupCntMap  = null; 
		Map<String, Integer> _counterNameDupCntMap  = null; 

		LinkedHashMap<String, Observer> _observerMap= new LinkedHashMap<String, Observer>();
		LinkedHashMap<String, Monitor>  _monitorMap = new LinkedHashMap<String, Monitor>();
		LinkedHashMap<String, Counter>  _counterMap = new LinkedHashMap<String, Counter>();

		public Instance(String name, int id, int val)
		{
			_name = name.trim();
			_id   = id;
			_val  = val;
		}

//		public void addCounter(Counter counter)    { _counterList .add(counter); }
//		public void addMonitor(Monitor monitor)    { _monitorList .add(monitor); }
//		public void addObserver(Observer observer) { _observerList.add(observer); }

		//-----------------------------------------------------------------------
		// in some RS Versions it looks like
		//-----------------------------------------------------------------------
		// 5126 = GroupsClosedDispatch
		// 5127 = GroupsClosedDispatch, but this should really be: GroupsClosedSwitchSQT    (just guessing the name)
		// 5128 = GroupsClosedDispatch, but this should really be: GroupsClosedRsLastCommit (just guessing the name)
		// 
		// But I can't distinguage counter names when they are sent with the same name.... and since 'admin stats, all' don't send CounterID's we need to "guess"
		// So to keep them separately just add a "-#" after the duplicate counter (where the # would be a the duplicate number)
		// *HOPEFULLY* the CounterNames are sent in the counterID order...
		// And if they are "sent in counterID order" we could really "rename" known "duplicates" :)
		// so maybe we can rename first duplicate of 'GroupsClosedDispatch' -> 'GroupsClosedSwitchSQT' 
		//                   and second duplicate of 'GroupsClosedDispatch' -> 'GroupsClosedRsLastCommit'
		//-----------------------------------------------------------------------

		public void addCounter(Counter counter)    
		{
			if (_counterMap.containsKey(counter._name))
			{
				if (_counterNameDupCntMap == null)
					_counterNameDupCntMap = new HashMap<String, Integer>();
				
				Integer dupCount = _counterNameDupCntMap.get(counter._name);
				dupCount = (dupCount == null) ? 1 : dupCount + 1;
				_counterNameDupCntMap.put(counter._name, dupCount);
				
				counter._name += "-" + dupCount;
			}
			_counterMap.put(counter._name, counter); 
		}

		public void addMonitor(Monitor monitor)
		{
			if (_monitorMap.containsKey(monitor._name))
			{
				if (_monitorNameDupCntMap == null)
					_monitorNameDupCntMap = new HashMap<String, Integer>();
				
				Integer dupCount = _monitorNameDupCntMap.get(monitor._name);
				dupCount = (dupCount == null) ? 1 : dupCount + 1;
				_monitorNameDupCntMap.put(monitor._name, dupCount);
				
				monitor._name += "-" + dupCount;
			}
			_monitorMap.put(monitor._name, monitor); 
		}

		public void addObserver(Observer observer) 
		{
			if (_observerMap.containsKey(observer._name))
			{
				if (_observerNameDupCntMap == null)
					_observerNameDupCntMap = new HashMap<String, Integer>();
				
				Integer dupCount = _observerNameDupCntMap.get(observer._name);
				dupCount = (dupCount == null) ? 1 : dupCount + 1;
				_observerNameDupCntMap.put(observer._name, dupCount);

				// Special case if it's a "known" duplicate... and they are sent in the CounterID order from RS we can make a "hack"
				// Note: the below wont work if the counterId's comes in a unpredictive order. 
				//       expected order counterId order is: 5126, 5127, 5128 
				if ("GroupsClosedDispatch".equals(observer._name))
				{
					if (dupCount == 1) observer._name = "GroupsClosedSwitchSQT";
					if (dupCount == 2) observer._name = "GroupsClosedRsLastCommit";
				}
				else
				{
					observer._name += "-" + dupCount;
				}
			}
			_observerMap.put(observer._name, observer); 
		}
	}

	/** Counter names can start with '*' or '#' or have both in them, so strip that off... */
	private static String fixCounterName(String name)
	{
		if (name.startsWith("*") || name.startsWith("#") )
		{
			name = name.substring(1);

			// Do it once more
			if (name.startsWith("*") || name.startsWith("#") )
				name = name.substring(1);
		}
		return name.trim();
	}

	/**
	 * Local class to hold a Observer Counter Information
	 */
	public static class Observer
	{
		String _name;
		long   _obs;
		long   _rate_x_sec;

		public Observer(String name, long obs, long rate_x_sec)
		{
			_name       = fixCounterName(name);
			_obs        = obs;
			_rate_x_sec = rate_x_sec;
		}
	}

	/**
	 * Local class to hold a Monitor Counter Information
	 */
	public static class Monitor
	{
		String _name;
		long   _obs;
		long   _last;
		long   _max;
		long   _avg_ttl_obs;

		public Monitor(String name, long obs, long last, long max, long avg_ttl_obs)
		{
			_name        = fixCounterName(name);
			_obs         = obs;
			_last        = last;
			_max         = max;
			_avg_ttl_obs = avg_ttl_obs;
		}

	}

	/**
	 * Local class to hold a Counter Information
	 */
	public static class Counter
	{
		String _name;
		long   _obs;
		long   _total;
		long   _last;
		long   _max;
		long   _avg_ttl_obs;
		long   _rate_x_sec;

		public Counter(String name, long obs, long total, long last, long max, long avg_ttl_obs, long rate_x_sec)
		{
			_name        = fixCounterName(name);
			_obs         = obs;
			_total       = total;
			_last        = last;
			_max         = max;
			_avg_ttl_obs = avg_ttl_obs;
			_rate_x_sec  = rate_x_sec;
		}
	}
}

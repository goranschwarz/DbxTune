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
package com.asetune.pcs.sqlcapture;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.log4j.Logger;

import com.asetune.DbxTune;
import com.asetune.Version;
import com.asetune.cache.XmlPlanCache;
import com.asetune.pcs.PersistWriterBase;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.sql.StatementNormalizer;
import com.asetune.sql.conn.AseConnection;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;
import com.asetune.utils.Ver;
import com.sybase.jdbcx.SybMessageHandler;

public class SqlCaptureBrokerAse 
extends SqlCaptureBrokerAbstract
{
	private static Logger _logger = Logger.getLogger(SqlCaptureBrokerAse.class);

	protected boolean _inTestCode = false;
	
	private boolean _clearBeforeFirstPoll = PersistentCounterHandler.DEFAULT_sqlCap_clearBeforeFirstPoll; // initialized in init(Configuration conf)
	private boolean _firstPoll = true;
	
//	private int     _sendSizeThreshold = PersistentCounterHandler.DEFAULT_sqlCap_sendSizeThreshold; // initialized in init(Configuration conf)

	private boolean _isNonConfiguredMonitoringAllowed = PersistentCounterHandler.DEFAULT_sqlCap_isNonConfiguredMonitoringAllowed;
	
	protected static final String MON_SQL_TEXT      = PersistWriterBase.getTableName(null, PersistWriterBase.SQL_CAPTURE_SQLTEXT,    null, false); // "MonSqlCapSqlText";
	protected static final String MON_SQL_STATEMENT = PersistWriterBase.getTableName(null, PersistWriterBase.SQL_CAPTURE_STATEMENTS, null, false); // "MonSqlCapStatements";
	protected static final String MON_SQL_PLAN      = PersistWriterBase.getTableName(null, PersistWriterBase.SQL_CAPTURE_PLANS,      null, false); // "MonSqlCapPlans";

	private static final List<String> _storegeTableNames = Arrays.asList(MON_SQL_TEXT, MON_SQL_STATEMENT, MON_SQL_PLAN);

	private String _sql_sqlText       = null;
	private String _sql_sqlStatements = null; 
	private String _sql_sqlPlanText   = null;

	protected boolean _sampleSqlText    = true;
	protected boolean _sampleStatements = true;
	protected boolean _samplePlan       = false;

	// ASE Configuration values
	private int _sqlTextPipeActive        = -1;
	private int _sqlTextPipeMaxMessages   = -1;
	private int _statementPipeActive      = -1;
	private int _statementPipeMaxMessages = -1;
	private int _planTextPipeActive       = -1;
	private int _planTextPipeMaxMessages  = -1;

	// below Time/VSum/VCnt is used to print "less" warning messages
	//  Time = last time a log message was written
	//  VSum = Value Summary: incremented on every overflow with the overflow value
	//  VCnt = Value Count:   How many times this has happened
	private long _lastConfigOverflowMsgTimeThreshold = 1 * 60 * 60 * 1000; // Write Message: Every Hour   
	
	private long _lastConfigOverflowMsgTime_statementPipeMaxMessages = -1; // System.currentTimeMillis(); 
	private long _lastConfigOverflowMsgVSum_statementPipeMaxMessages = 0; 
	private long _lastConfigOverflowMsgVCnt_statementPipeMaxMessages = 0; 

	private long _lastConfigOverflowMsgTime_sqlTextPipeMaxMessages = -1; // System.currentTimeMillis(); 
	private long _lastConfigOverflowMsgVSum_sqlTextPipeMaxMessages = 0; 
	private long _lastConfigOverflowMsgVCnt_sqlTextPipeMaxMessages = 0; 

	private long _lastConfigOverflowMsgTime_planTextPipeMaxMessages = -1; // System.currentTimeMillis(); 
	private long _lastConfigOverflowMsgVSum_planTextPipeMaxMessages = 0; 
	private long _lastConfigOverflowMsgVCnt_planTextPipeMaxMessages = 0; 


	private long _nonConfiguredMonitoringCount = 0;

	/** 
	 * Keep X number of SQL/Plan-Text generations, before we send text for storage. 
	 * This so we can remove SQL/Plan Text associated with "short" Statement executions, 
	 * that typically can span over 2 or several calls to doSqlCapture() 
	 */
//	protected Queue<DeferredSqlAndPlanTextQueueEntry> _deferredQueueFor_sqlTextAndPlan = new LinkedList<>();
	protected DeferredSqlAndPlanTextQueue _deferredQueueFor_sqlTextAndPlan = new DeferredSqlAndPlanTextQueue();

	/** Keep SQLText & PlanText in memory for X seconds before flushing them to storage, so thay can be removed if the statement is not within the filter, for example execution time */
	protected int _deferredStorageThresholdFor_sqlTextAndPlan = DEFAULT_sqlCap_ase_sqlTextAndPlan_deferredQueueAge;

	private boolean _removeStaticSqlText = DEFAULT_sqlCap_ase_sqlTextAndPlan_removeStaticSqlText;
	private Map<String, Integer> _removeStaticSqlText_stat = new HashMap<>(); 
	private long _removeStaticSqlText_stat_printThreshold = DEFAULT_sqlCap_ase_sqlTextAndPlan_removeStaticSqlText_printMsgTimeThreshold; 
	private long _removeStaticSqlText_stat_printLastTime  = -1; // System.currentTimeMillis();


	private static int _stmnt_SPID_pos    = 2 + 1; // 2 = ListPos + 1 is for that the row starts with a string that contains the DestinationTablename
	private static int _stmnt_KPID_pos    = 3 + 1; // 3 = ListPos + 1 is for that the row starts with a string that contains the DestinationTablename
	private static int _stmnt_BatchID_pos = 7 + 1; // 7 = ListPos + 1 is for that the row starts with a string that contains the DestinationTablename

//	private SqlCaptureDetails _sqlCaptureDetails = new SqlCaptureDetails();

	public static final String  PROPKEY_sqlCap_ase_sqlTextAndPlan_deferredQueueAge   = "PersistentCounterHandler.sqlCapture.ase.sqlTextAndPlan.deferredQueueAge";
	public static final int     DEFAULT_sqlCap_ase_sqlTextAndPlan_deferredQueueAge   = 30;

	public static final String  PROPKEY_sqlCap_ase_sqlTextAndPlan_overflowMsgTimeThreshold   = "PersistentCounterHandler.sqlCapture.ase.sqlTextAndPlan.overflowMsgTimeThreshold";
	public static final int     DEFAULT_sqlCap_ase_sqlTextAndPlan_overflowMsgTimeThreshold   = 1 * 60 * 60 * 1000; // Write Message: Every Hour

	public static final String  PROPKEY_sqlCap_ase_sqlTextAndPlan_removeStaticSqlText = "PersistentCounterHandler.sqlCapture.ase.sqlTextAndPlan.removeStaticSqlText";
	public static final boolean DEFAULT_sqlCap_ase_sqlTextAndPlan_removeStaticSqlText = true;

	public static final String  PROPKEY_sqlCap_ase_sqlTextAndPlan_removeStaticSqlText_printMsgTimeThreshold = "PersistentCounterHandler.sqlCapture.ase.sqlTextAndPlan.removeStaticSqlText.printMsgTimeThreshold";
	public static final int     DEFAULT_sqlCap_ase_sqlTextAndPlan_removeStaticSqlText_printMsgTimeThreshold = 1 * 60 * 60 * 1000; // Write Message: Every Hour
//	public static final int     DEFAULT_sqlCap_ase_sqlTextAndPlan_removeStaticSqlText_printMsgTimeThreshold = 5 * 60 * 1000; // Write Message: Every 5 minute

	public static final String  PROPKEY_sqlCap_ase_sqlTextAndPlan_storeNormalizedSqlText     = "PersistentCounterHandler.sqlCapture.ase.sqlTextAndPlan.storeNormalizedSqlText";
	public static final boolean DEFAULT_sqlCap_ase_sqlTextAndPlan_storeNormalizedSqlText     = false;

	public static final String  PROPKEY_sqlCap_ase_sqlTextAndPlan_storeNormalizedSqlTextHash = "PersistentCounterHandler.sqlCapture.ase.sqlTextAndPlan.storeNormalizedSqlTextHash";
	public static final boolean DEFAULT_sqlCap_ase_sqlTextAndPlan_storeNormalizedSqlTextHash = false;

	@Override
	public void init(Configuration conf)
	{
		super.init(conf);
		
		_clearBeforeFirstPoll                       = getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_clearBeforeFirstPoll,                PersistentCounterHandler.DEFAULT_sqlCap_clearBeforeFirstPoll);
//		_sendSizeThreshold                          = getIntProperty    (PersistentCounterHandler.PROPKEY_sqlCap_sendSizeThreshold,                   PersistentCounterHandler.DEFAULT_sqlCap_sendSizeThreshold);
		_isNonConfiguredMonitoringAllowed           = getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_isNonConfiguredMonitoringAllowed,    PersistentCounterHandler.DEFAULT_sqlCap_isNonConfiguredMonitoringAllowed);
		_deferredStorageThresholdFor_sqlTextAndPlan = getIntProperty    (PROPKEY_sqlCap_ase_sqlTextAndPlan_deferredQueueAge,                          DEFAULT_sqlCap_ase_sqlTextAndPlan_deferredQueueAge);
		_lastConfigOverflowMsgTimeThreshold         = getIntProperty    (PROPKEY_sqlCap_ase_sqlTextAndPlan_overflowMsgTimeThreshold,                  DEFAULT_sqlCap_ase_sqlTextAndPlan_overflowMsgTimeThreshold);
		_removeStaticSqlText                        = getBooleanProperty(PROPKEY_sqlCap_ase_sqlTextAndPlan_removeStaticSqlText,                       DEFAULT_sqlCap_ase_sqlTextAndPlan_removeStaticSqlText);
		_removeStaticSqlText_stat_printThreshold    = getIntProperty    (PROPKEY_sqlCap_ase_sqlTextAndPlan_removeStaticSqlText_printMsgTimeThreshold, DEFAULT_sqlCap_ase_sqlTextAndPlan_removeStaticSqlText_printMsgTimeThreshold);
	}

	@Override
	public void onConnect(DbxConnection conn)
	{
		if (conn instanceof AseConnection)
		{
			// Discard some messages about "not configured"
			if (_isNonConfiguredMonitoringAllowed)
			{
				AseConnection aseConn = (AseConnection) conn;
				
				SybMessageHandler sybMessageHandler = new SybMessageHandler()
				{
					@Override
					public SQLException messageHandler(SQLException sqle)
					{
						int    code   = sqle.getErrorCode();
						String msgStr = sqle.getMessage();
						
						// Remove any newlines etc...
						if (msgStr != null)
							msgStr = msgStr.trim();
						
						// Msg 10353:               You must have any of the following role(s) to execute this command/procedure: 'mon_role' . Please contact a user with the appropriate role for help.
						// Msg 12052:               Collection of monitoring data for table '%.*s' requires that the %s configuration option(s) be enabled. To set the necessary configuration, contact a user who has the System Administrator (SA) role.
						// Msg 12036: ASE 12.5.0.3: Collection of monitoring data for table '%.*s' requires that the '%s' configuration option(s) be enabled. To set the necessary configuration, contact a user who has the System Administrator (SA) role.
						//     12036: ASE 15.7.0.0: Incomplete configuration on instance ID %d. Collection of monitoring data for table '%.*s' requires that the %s configuration option(s) be enabled. To set the necessary configuration, contact a user who has the System Administrator (SA) role.
						// so lets reinitialize CM, to check again for next sample
						if (code == 12052 || code == 12036)
						{
							if (_nonConfiguredMonitoringCount == 0)
								_logger.warn("sybMessageHandler: SQL Capture is allowing monitoring even if it's NOT properly configured. Received the following message which will just be printed this time. MsgNum="+code+", Text='"+msgStr+"'.");
							
							_nonConfiguredMonitoringCount++;
							
							if (_logger.isDebugEnabled())
								_logger.debug("sybMessageHandler: _nonConfiguredMonitoringCount="+_nonConfiguredMonitoringCount+": Msg="+code+", '"+msgStr+"'.");
							
							return null; // JDBC Caller will NOT be aborted, or a Exception will NOT be throw to the caller
						}
						
						// Msg 950: Database 'XXXXX' is currently offline. Please wait and try your command later. 
						if (code == 950)
						{
							if (_logger.isDebugEnabled())
								_logger.debug("sybMessageHandler: Discarding the following message: Msg="+code+", '"+msgStr+"'.");
							
							return null; // JDBC Caller will NOT be aborted, or a Exception will NOT be throw to the caller
						}
						
						return sqle;
					}
				};
				
				aseConn.setSybMessageHandler(sybMessageHandler);
			}
		}
	}

	@Override
	public List<String> getTableNames()
	{
		return _storegeTableNames;
	}

	private String fill(String str, int fill)                                { return PersistWriterBase.fill(str, fill); }
	private String getDatatype(String type, int length, int prec, int scale) { return PersistWriterBase.getDatatype(type, length, prec, scale); }
	private String getNullable(boolean nullable)                             { return PersistWriterBase.getNullable(nullable); }

	@Override
	public List<String> checkTableDdl(DbxConnection conn, DatabaseMetaData dbmd, String tabName)
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

		List<String> colNames = new ArrayList<>();
		List<String> list = new ArrayList<>();
		
		// Get column names and store it in colNames
		try
		{
			ResultSet rs = dbmd.getColumns(null, null, tabName, "%");
			while(rs.next())
			{
				colNames.add( rs.getString("COLUMN_NAME") );
			}
			rs.close();
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting column definition for tabel '"+tabName+"', skipping alter check. caught: "+ex);
		}

		//-------------------------------------------------------------
		if (MON_SQL_STATEMENT.equals(tabName))
		{
			if ( ! colNames.contains("NormJavaSqlHashCode") )
				list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"NormJavaSqlHashCode"+rq,40)+" "+fill(getDatatype("int", -1,-1,-1),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col

			return list;
		}

		//-------------------------------------------------------------
		if (MON_SQL_TEXT.equals(tabName))
		{
			if ( ! colNames.contains("AddMethod") )
				list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"AddMethod"+rq,40)+" "+fill(getDatatype("int",-1,-1,-1),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col

			if ( ! colNames.contains("NormJavaSqlHashCode") )
				list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"NormJavaSqlHashCode"+rq,40)+" "+fill(getDatatype("int", -1,-1,-1),20)+" "+getNullable(true)+"\n");

			if ( ! colNames.contains("NormSQLText") )
				list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"NormSQLText"+rq,40)+" "+fill(getDatatype("varchar", 65536,-1,-1),20)+" "+getNullable(true)+"\n");

			return list;
		}
		
		//-------------------------------------------------------------
		if (MON_SQL_PLAN.equals(tabName))
		{
			if ( ! colNames.contains("AddMethod") )
				list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"AddMethod"+rq,40)+" "+fill(getDatatype("int",-1,-1,-1),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col

			return list;
		}

		return list;
	}
	
	@Override
	public String getTableDdlString(DbxConnection conn, DatabaseMetaData dbmd, String tabName)
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection
		
		if (MON_SQL_TEXT.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();

			sbSql.append("create table " + tabName + "\n");
			sbSql.append("( \n");
			sbSql.append("    "+fill(lq+"sampleTime"          +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"InstanceID"          +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"SPID"                +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"KPID"                +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"BatchID"             +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"ServerLogin"         +rq,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"AddMethod"           +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"JavaSqlLength"       +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"JavaSqlHashCode"     +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"SQLText"             +rq,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true) +"\n");
			sbSql.append("   ,"+fill(lq+"SQLText"             +rq,40)+" "+fill(getDatatype("varchar",65536,-1,-1),20)+" "+getNullable(true) +"\n");
			sbSql.append("   ,"+fill(lq+"NormJavaSqlHashCode" +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(true) +"\n");
			sbSql.append("   ,"+fill(lq+"NormSQLText"         +rq,40)+" "+fill(getDatatype("varchar",65536,-1,-1),20)+" "+getNullable(true) +"\n");
//			sbSql.append("\n");
//			sbSql.append("   ,PRIMARY KEY ("+lq+"SPID"+rq+", "+lq+"KPID"+rq+", "+lq+"InstanceID"+rq+", "+lq+"BatchID"+rq+")\n");
			sbSql.append(") \n");

			return sbSql.toString();
		}

		if (MON_SQL_STATEMENT.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();

			sbSql.append("create table " + tabName + "\n");
			sbSql.append("( \n");
			sbSql.append("    "+fill(lq+"sampleTime"           +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"InstanceID"           +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"SPID"                 +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n"); // NOTE If this pos is changed: alter _stmnt_SPID_pos
			sbSql.append("   ,"+fill(lq+"KPID"                 +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n"); // NOTE If this pos is changed: alter _stmnt_KPID_pos
			sbSql.append("   ,"+fill(lq+"DBID"                 +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"ProcedureID"          +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"PlanID"               +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"BatchID"              +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n"); // NOTE If this pos is changed: alter _stmnt_BatchID_pos
			sbSql.append("   ,"+fill(lq+"ContextID"            +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"LineNumber"           +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"ObjOwnerID"           +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"DBName"               +rq,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"HashKey"              +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"SsqlId"               +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"ProcName"             +rq,40)+" "+fill(getDatatype("varchar",  255,-1,-1),20)+" "+getNullable(true)+"\n"); // NULLABLE
			sbSql.append("   ,"+fill(lq+"Elapsed_ms"           +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"CpuTime"              +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"WaitTime"             +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"MemUsageKB"           +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"PhysicalReads"        +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"LogicalReads"         +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"RowsAffected"         +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"ErrorStatus"          +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"ProcNestLevel"        +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"StatementNumber"      +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"QueryOptimizationTime"+rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"PagesModified"        +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"PacketsSent"          +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"PacketsReceived"      +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"NetworkPacketSize"    +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"PlansAltered"         +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"StartTime"            +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"EndTime"              +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"JavaSqlHashCode"      +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"NormJavaSqlHashCode"  +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(true)+"\n"); // NULLABLE
//			sbSql.append("\n");
//			sbSql.append("   ,PRIMARY KEY ("+lq+"SPID"+rq+", "+lq+"KPID"+rq+", "+lq+"InstanceID"+rq+", "+lq+"BatchID"+rq+")\n");
			sbSql.append(") \n");

			return sbSql.toString();
		}

		if (MON_SQL_PLAN.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();
			
			sbSql.append("create table " + tabName + "\n");
			sbSql.append("( \n");
			sbSql.append("    "+fill(lq+"sampleTime"       +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"InstanceID"       +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"SPID"             +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"KPID"             +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"PlanID"           +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"BatchID"          +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"ContextID"        +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"DBID"             +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"DBName"           +rq,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"ProcedureID"      +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"AddMethod"        +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"PlanText"         +rq,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true) +"\n");
			sbSql.append("   ,"+fill(lq+"PlanText"         +rq,40)+" "+fill(getDatatype("varchar",65536,-1,-1),20)+" "+getNullable(true) +"\n");
//			sbSql.append("\n");
//			sbSql.append("   ,PRIMARY KEY ("+lq+"SPID"+rq+", "+lq+"KPID"+rq+", "+lq+"InstanceID"+rq+", "+lq+"BatchID"+rq+")\n");
			sbSql.append(") \n");

			return sbSql.toString();
		}

		return null;
	}

//	private String dbmsQuotify(String qic, String...names)
//	{
//		StringBuilder sb = new StringBuilder();
//
//		for (String name : names)
//		{
//			sb.append(qic).append(name).append(qic).append(", ");
//		}
//		// Remove last ", "
//		sb.delete(sb.length()-2, sb.length());
//
//		return sb.toString();
//	}
//
//	@Override
//	public List<String> getIndexDdlString(DbxConnection conn, DatabaseMetaData dbmd, String tabName)
//	{
//		// NOTE: The DatabaseMetaData is to the PCS Writer/Storage Connection
//		String dbmsProductName = "unknown";
//		String qic             = "\"";
//		try { dbmsProductName = dbmd.getDatabaseProductName();   } catch(SQLException ex) { _logger.warn("Problems getting 'dbmd.getDatabaseProductName()', Caught: "+ex); }
//		try { qic             = dbmd.getIdentifierQuoteString(); } catch(SQLException ex) { _logger.warn("Problems getting 'dbmd.getIdentifierQuoteString()', Caught: "+ex); }
//		
//		String iQic = qic; // indexNameQic
//		if ( DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_SYBASE_ASE) )
//			iQic = "";
//
//		// Put indexes in this list that will be returned
//		List<String> list = new ArrayList<>();
//
//		if (MON_SQL_TEXT.equals(tabName))
//		{
//			conn.quotify(name)
//			list.add("create index " + dbmsQuotify(iQic, tabName+"_ix1") + " on " + dbmsQuotify(qic, tabName) + "(" + dbmsQuotify(qic, "BatchID", "SPID", "KPID") + ")\n");
//		}
//
//		if (MON_SQL_STATEMENT.equals(tabName))
//		{
//			list.add("create index " + dbmsQuotify(iQic, tabName+"_ix1") + " on " + dbmsQuotify(qic, tabName) + "(" + dbmsQuotify(qic, "BatchID", "SPID", "KPID") + ")\n");
//			list.add("create index " + dbmsQuotify(iQic, tabName+"_ix2") + " on " + dbmsQuotify(qic, tabName) + "(" + dbmsQuotify(qic, "StartTime", "EndTime")    + ")\n");
//		}
//
//		if (MON_SQL_PLAN.equals(tabName))
//		{
//			list.add("create index " + dbmsQuotify(iQic, tabName+"_ix1") + " on " + dbmsQuotify(qic, tabName) + "(" + dbmsQuotify(qic, "BatchID", "SPID", "KPID") + ")\n");
//		}
//
//		return list;
//	}

	@Override
	public List<String> getIndexDdlString(DbxConnection conn, DatabaseMetaData dbmd, String tabName)
	{
		// Put indexes in this list that will be returned
		List<String> list = new ArrayList<>();

		if (MON_SQL_TEXT.equals(tabName))
		{
			list.add("create index " + conn.quotify(tabName+"_ix1") + " on " + conn.quotify(tabName) + "(" + conn.quotify("BatchID", "SPID", "KPID") + ")\n");
		}

		if (MON_SQL_STATEMENT.equals(tabName))
		{
			list.add("create index " + conn.quotify(tabName+"_ix1") + " on " + conn.quotify(tabName) + "(" + conn.quotify("BatchID", "SPID", "KPID") + ")\n");
			list.add("create index " + conn.quotify(tabName+"_ix2") + " on " + conn.quotify(tabName) + "(" + conn.quotify("StartTime", "EndTime")    + ")\n");
		}

		if (MON_SQL_PLAN.equals(tabName))
		{
			list.add("create index " + conn.quotify(tabName+"_ix1") + " on " + conn.quotify(tabName) + "(" + conn.quotify("BatchID", "SPID", "KPID") + ")\n");
		}

		return list;
	}

	@Override
	public String getInsertStatement(DbxConnection conn, String tabName)
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection
		
		if (MON_SQL_TEXT.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();

			sbSql.append("insert into ").append(tabName);
			sbSql.append("(");
			sbSql.append(" ").append(lq).append("sampleTime"         ).append(rq); // 1
			sbSql.append(",").append(lq).append("InstanceID"         ).append(rq); // 2
			sbSql.append(",").append(lq).append("SPID"               ).append(rq); // 3
			sbSql.append(",").append(lq).append("KPID"               ).append(rq); // 4
			sbSql.append(",").append(lq).append("BatchID"            ).append(rq); // 5
			sbSql.append(",").append(lq).append("ServerLogin"        ).append(rq); // 6
			sbSql.append(",").append(lq).append("AddMethod"          ).append(rq); // 7
			sbSql.append(",").append(lq).append("JavaSqlLength"      ).append(rq); // 8
			sbSql.append(",").append(lq).append("JavaSqlHashCode"    ).append(rq); // 9
			sbSql.append(",").append(lq).append("SQLText"            ).append(rq); // 10
			sbSql.append(",").append(lq).append("NormJavaSqlHashCode").append(rq); // 11
			sbSql.append(",").append(lq).append("NormSQLText"        ).append(rq); // 12
			sbSql.append(") \n");
			sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n"); // 10 question marks
			//                   1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12

			return sbSql.toString();
		}

		if (MON_SQL_STATEMENT.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();

			sbSql.append("insert into ").append(tabName);
			sbSql.append("(");
			sbSql.append(" ").append(lq).append("sampleTime"           ).append(rq);  //  1
			sbSql.append(",").append(lq).append("InstanceID"           ).append(rq);  //  2
			sbSql.append(",").append(lq).append("SPID"                 ).append(rq);  //  3
			sbSql.append(",").append(lq).append("KPID"                 ).append(rq);  //  4
			sbSql.append(",").append(lq).append("DBID"                 ).append(rq);  //  5
			sbSql.append(",").append(lq).append("ProcedureID"          ).append(rq);  //  6
			sbSql.append(",").append(lq).append("PlanID"               ).append(rq);  //  7
			sbSql.append(",").append(lq).append("BatchID"              ).append(rq);  //  8
			sbSql.append(",").append(lq).append("ContextID"            ).append(rq);  //  9
			sbSql.append(",").append(lq).append("LineNumber"           ).append(rq);  // 10
			sbSql.append(",").append(lq).append("ObjOwnerID"           ).append(rq);  // 11
			sbSql.append(",").append(lq).append("DBName"               ).append(rq);  // 12
			sbSql.append(",").append(lq).append("HashKey"              ).append(rq);  // 13
			sbSql.append(",").append(lq).append("SsqlId"               ).append(rq);  // 14
			sbSql.append(",").append(lq).append("ProcName"             ).append(rq);  // 15
			sbSql.append(",").append(lq).append("Elapsed_ms"           ).append(rq);  // 16
			sbSql.append(",").append(lq).append("CpuTime"              ).append(rq);  // 17
			sbSql.append(",").append(lq).append("WaitTime"             ).append(rq);  // 18
			sbSql.append(",").append(lq).append("MemUsageKB"           ).append(rq);  // 19
			sbSql.append(",").append(lq).append("PhysicalReads"        ).append(rq);  // 20
			sbSql.append(",").append(lq).append("LogicalReads"         ).append(rq);  // 21
			sbSql.append(",").append(lq).append("RowsAffected"         ).append(rq);  // 22
			sbSql.append(",").append(lq).append("ErrorStatus"          ).append(rq);  // 23
			sbSql.append(",").append(lq).append("ProcNestLevel"        ).append(rq);  // 24
			sbSql.append(",").append(lq).append("StatementNumber"      ).append(rq);  // 25
			sbSql.append(",").append(lq).append("QueryOptimizationTime").append(rq);  // 26
			sbSql.append(",").append(lq).append("PagesModified"        ).append(rq);  // 27
			sbSql.append(",").append(lq).append("PacketsSent"          ).append(rq);  // 28
			sbSql.append(",").append(lq).append("PacketsReceived"      ).append(rq);  // 29
			sbSql.append(",").append(lq).append("NetworkPacketSize"    ).append(rq);  // 30
			sbSql.append(",").append(lq).append("PlansAltered"         ).append(rq);  // 31
			sbSql.append(",").append(lq).append("StartTime"            ).append(rq);  // 32
			sbSql.append(",").append(lq).append("EndTime"              ).append(rq);  // 33
			sbSql.append(",").append(lq).append("JavaSqlHashCode"      ).append(rq);  // 34
			sbSql.append(",").append(lq).append("NormJavaSqlHashCode"  ).append(rq);  // 35
			sbSql.append(") \n");
			sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n"); // 32 question marks
			//                   1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35

			return sbSql.toString();
		}

		if (MON_SQL_PLAN.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();

			sbSql.append("insert into ").append(tabName);
			sbSql.append("(");
			sbSql.append(" ").append(lq).append("sampleTime" ).append(rq); // 1
			sbSql.append(",").append(lq).append("InstanceID" ).append(rq); // 2
			sbSql.append(",").append(lq).append("SPID"       ).append(rq); // 3
			sbSql.append(",").append(lq).append("KPID"       ).append(rq); // 4
			sbSql.append(",").append(lq).append("PlanID"     ).append(rq); // 5
			sbSql.append(",").append(lq).append("BatchID"    ).append(rq); // 6
			sbSql.append(",").append(lq).append("ContextID"  ).append(rq); // 7
			sbSql.append(",").append(lq).append("DBID"       ).append(rq); // 8
			sbSql.append(",").append(lq).append("DBName"     ).append(rq); // 9
			sbSql.append(",").append(lq).append("ProcedureID").append(rq); // 10
			sbSql.append(",").append(lq).append("AddMethod"  ).append(rq); // 11
			sbSql.append(",").append(lq).append("PlanText"   ).append(rq); // 12
			sbSql.append(") \n");
			sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n"); // 12 question marks
			//                   1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12

			return sbSql.toString();
		}

		return null;
	}

	private void clearTable(DbxConnection conn, String tabName)
	{
		String sql = "select count(*) from master.dbo."+tabName;
		
		try
		{
			_logger.info("BEGIN: Discarding everything in the transient '"+tabName+"' table in the first sample.");

			long startTime = System.currentTimeMillis();

			Statement stmnt = conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			int discardCount = 0;
			while(rs.next()) 
			{
				discardCount = rs.getInt(1);
			}
			rs.close();
			stmnt.close();

			_logger.info("END:   Discarding everything in the transient '"+tabName+"' table in the first sample. discardCount="+discardCount+", this took "+TimeUtils.msToTimeStr(System.currentTimeMillis()-startTime)+".");
		}
		catch(SQLException ex)
		{
			_logger.error("END:   Discarding everything in the transient '"+tabName+"' table in the first sample FAILED. Caught: "+AseConnectionUtils.sqlExceptionToString(ex));
		}
	}

	public static final String  CFGNAME_aseConfig_sql_text_pipe_active        = "sql text pipe active";
	public static final String  PROPKEY_aseConfig_sql_text_pipe_active        = "SqlCapture.ase.config.sql_text_pipe_active";
	public static final int     DEFAULT_aseConfig_sql_text_pipe_active        = 1;

	public static final String  CFGNAME_aseConfig_sql_text_pipe_max_messages  = "sql text pipe max messages";
	public static final String  PROPKEY_aseConfig_sql_text_pipe_max_messages  = "SqlCapture.ase.config.sql_text_pipe_max_messages";
	public static final int     DEFAULT_aseConfig_sql_text_pipe_max_messages  = 1000;


	public static final String  CFGNAME_aseConfig_statement_pipe_active       = "statement pipe active";
	public static final String  PROPKEY_aseConfig_statement_pipe_active       = "SqlCapture.ase.config.statement_pipe_active";
	public static final int     DEFAULT_aseConfig_statement_pipe_active       = 1;

	public static final String  CFGNAME_aseConfig_statement_pipe_max_messages = "statement pipe max messages";
	public static final String  PROPKEY_aseConfig_statement_pipe_max_messages = "SqlCapture.ase.config.statement_pipe_max_messages";
	public static final int     DEFAULT_aseConfig_statement_pipe_max_messages = 5000;


	public static final String  CFGNAME_aseConfig_plan_text_pipe_active       = "plan text pipe active";
	public static final String  PROPKEY_aseConfig_plan_text_pipe_active       = "SqlCapture.ase.config.plan_text_pipe_active";
	public static final int     DEFAULT_aseConfig_plan_text_pipe_active       = 0; // or 1

	public static final String  CFGNAME_aseConfig_plan_text_pipe_max_messages = "plan text pipe max messages";
	public static final String  PROPKEY_aseConfig_plan_text_pipe_max_messages = "SqlCapture.ase.config.plan_text_pipe_max_messages";
	public static final int     DEFAULT_aseConfig_plan_text_pipe_max_messages = 0; // or 15000


	private int doReconfigure(DbxConnection conn, String cfgName, int cfgCurrent, int cfgValue)
	{
		_logger.info("ASE Configuration '"+cfgName+"' for 'SQL Capture', will be reconfigured from value '"+cfgCurrent+"' to value '"+cfgValue+"'.");

		try
		{
			AseConnectionUtils.setAseConfigValue(conn, cfgName, cfgValue);
		}
		catch (SQLException e)
		{
			_logger.error("Problems setting ASE configuration '"+cfgName+"' to '"+cfgValue+"'. Caught: "+AseConnectionUtils.sqlExceptionToString(e));
		}

		int configHasValue = AseConnectionUtils.getAseConfigRunValueNoEx(conn, cfgName);
		if (_logger.isDebugEnabled()) 
			_logger.debug("After re-config, the ASE Configuration '"+cfgName+"', now has value '"+configHasValue+"'.");

		if (configHasValue != cfgValue)
			_logger.warn("After re-config, the ASE Configuration '"+cfgName+"', now has value '"+configHasValue+"', but the value we wanted to be after reconiguring was '"+configHasValue+"'.");
		
		return configHasValue;
	}
	
	private void checkConfig(DbxConnection conn)
	{
		boolean doSqlText                = getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_doSqlText,       PersistentCounterHandler.DEFAULT_sqlCap_doSqlText);
		boolean doStatementInfo          = getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_doStatementInfo, PersistentCounterHandler.DEFAULT_sqlCap_doStatementInfo);
		boolean doPlanText               = getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_doPlanText,      PersistentCounterHandler.DEFAULT_sqlCap_doPlanText);

		int cfg_sqlTextPipeActive        = getIntProperty(PROPKEY_aseConfig_sql_text_pipe_active,        DEFAULT_aseConfig_sql_text_pipe_active);
		int cfg_sqlTextPipeMaxMessages   = getIntProperty(PROPKEY_aseConfig_sql_text_pipe_max_messages,  DEFAULT_aseConfig_sql_text_pipe_max_messages);
		int cfg_statementPipeActive      = getIntProperty(PROPKEY_aseConfig_statement_pipe_active,       DEFAULT_aseConfig_statement_pipe_active);
		int cfg_statementPipeMaxMessages = getIntProperty(PROPKEY_aseConfig_statement_pipe_max_messages, DEFAULT_aseConfig_statement_pipe_max_messages);
		int cfg_planTextPipeActive       = getIntProperty(PROPKEY_aseConfig_plan_text_pipe_active,       DEFAULT_aseConfig_plan_text_pipe_active);
		int cfg_planTextPipeMaxMessages  = getIntProperty(PROPKEY_aseConfig_plan_text_pipe_max_messages, DEFAULT_aseConfig_plan_text_pipe_max_messages);

		if ( _inTestCode )
		{
			_sqlTextPipeActive        = 1;
			_sqlTextPipeMaxMessages   = 10000;
			
			_statementPipeActive      = 1;
			_statementPipeMaxMessages = 10000;
			
			_planTextPipeActive       = 1;
			_planTextPipeMaxMessages  = 10000;

			return;
		}

		try
		{
			boolean doReconfigure = false;

			_sqlTextPipeActive        = AseConnectionUtils.getAseConfigRunValue(conn, CFGNAME_aseConfig_sql_text_pipe_active);
			_sqlTextPipeMaxMessages   = AseConnectionUtils.getAseConfigRunValue(conn, CFGNAME_aseConfig_sql_text_pipe_max_messages);
			
			_statementPipeActive      = AseConnectionUtils.getAseConfigRunValue(conn, CFGNAME_aseConfig_statement_pipe_active);
			_statementPipeMaxMessages = AseConnectionUtils.getAseConfigRunValue(conn, CFGNAME_aseConfig_statement_pipe_max_messages);
			
			_planTextPipeActive       = AseConnectionUtils.getAseConfigRunValue(conn, CFGNAME_aseConfig_plan_text_pipe_active);
			_planTextPipeMaxMessages  = AseConnectionUtils.getAseConfigRunValue(conn, CFGNAME_aseConfig_plan_text_pipe_max_messages);
			
			if (_sqlTextPipeActive        < cfg_sqlTextPipeActive)        { doReconfigure = true; _logger.warn("The ASE Configuration '" + CFGNAME_aseConfig_sql_text_pipe_active        + "' is lower (currently="+ _sqlTextPipeActive        +") than the suggested value '" + cfg_sqlTextPipeActive        + "'. If --reconfigure is enabled, this will automatically be configured."); }
			if (_sqlTextPipeMaxMessages   < cfg_sqlTextPipeMaxMessages)   { doReconfigure = true; _logger.warn("The ASE Configuration '" + CFGNAME_aseConfig_sql_text_pipe_max_messages  + "' is lower (currently="+ _sqlTextPipeMaxMessages   +") than the suggested value '" + cfg_sqlTextPipeMaxMessages   + "'. If --reconfigure is enabled, this will automatically be configured."); }
			if (_statementPipeActive      < cfg_statementPipeActive)      { doReconfigure = true; _logger.warn("The ASE Configuration '" + CFGNAME_aseConfig_statement_pipe_active       + "' is lower (currently="+ _statementPipeActive      +") than the suggested value '" + cfg_statementPipeActive      + "'. If --reconfigure is enabled, this will automatically be configured."); }
			if (_statementPipeMaxMessages < cfg_statementPipeMaxMessages) { doReconfigure = true; _logger.warn("The ASE Configuration '" + CFGNAME_aseConfig_statement_pipe_max_messages + "' is lower (currently="+ _statementPipeMaxMessages +") than the suggested value '" + cfg_statementPipeMaxMessages + "'. If --reconfigure is enabled, this will automatically be configured."); }
			if (_planTextPipeActive       < cfg_planTextPipeActive)       { doReconfigure = true; _logger.warn("The ASE Configuration '" + CFGNAME_aseConfig_plan_text_pipe_active       + "' is lower (currently="+ _planTextPipeActive       +") than the suggested value '" + cfg_planTextPipeActive       + "'. If --reconfigure is enabled, this will automatically be configured."); }
			if (_planTextPipeMaxMessages  < cfg_planTextPipeMaxMessages)  { doReconfigure = true; _logger.warn("The ASE Configuration '" + CFGNAME_aseConfig_plan_text_pipe_max_messages + "' is lower (currently="+ _planTextPipeMaxMessages  +") than the suggested value '" + cfg_planTextPipeMaxMessages  + "'. If --reconfigure is enabled, this will automatically be configured."); }

			_sampleSqlText    = doSqlText       && _sqlTextPipeActive   > 0 && _sqlTextPipeMaxMessages   > 0;
			_sampleStatements = doStatementInfo && _statementPipeActive > 0 && _statementPipeMaxMessages > 0;
			_samplePlan       = doPlanText      && _planTextPipeActive  > 0 && _planTextPipeMaxMessages  > 0;
			
			// If we are in GUI mode, do not reconfigure, just warn...
			if ( DbxTune.hasGui() )
				doReconfigure = false;
			
			// if not configured, should we try to reconfigure it...
			if ( doReconfigure )
			{
//				Configuration conf = Configuration.getInstance(Configuration.PCS);
				doReconfigure = getBooleanProperty("offline.configuration.fix", false);

				// CHECK IF WE HAVE "sa role", so we can re-configure
				if (doReconfigure)
				{
					boolean hasSaRole = AseConnectionUtils.hasRole(conn, AseConnectionUtils.SA_ROLE);
					if ( ! hasSaRole )
					{
						doReconfigure = false;
						_logger.warn("Can not adjust the configuration '* pipe active' or '* pipe max messages'. To do that the connected user needs to have '"+AseConnectionUtils.SA_ROLE+"'.");
					}
				}
			}
			//doReconfigure = true; // if you want to force when testing
			
			if (doReconfigure)
			{
				if (_sqlTextPipeActive        < cfg_sqlTextPipeActive)        _sqlTextPipeActive        = doReconfigure(conn, CFGNAME_aseConfig_sql_text_pipe_active,        _sqlTextPipeActive       , cfg_sqlTextPipeActive       );
				if (_sqlTextPipeMaxMessages   < cfg_sqlTextPipeMaxMessages)   _sqlTextPipeMaxMessages   = doReconfigure(conn, CFGNAME_aseConfig_sql_text_pipe_max_messages,  _sqlTextPipeMaxMessages  , cfg_sqlTextPipeMaxMessages  );
				if (_statementPipeActive      < cfg_statementPipeActive)      _statementPipeActive      = doReconfigure(conn, CFGNAME_aseConfig_statement_pipe_active,       _statementPipeActive     , cfg_statementPipeActive     );
				if (_statementPipeMaxMessages < cfg_statementPipeMaxMessages) _statementPipeMaxMessages = doReconfigure(conn, CFGNAME_aseConfig_statement_pipe_max_messages, _statementPipeMaxMessages, cfg_statementPipeMaxMessages);
				if (_planTextPipeActive       < cfg_planTextPipeActive)       _planTextPipeActive       = doReconfigure(conn, CFGNAME_aseConfig_plan_text_pipe_active,       _planTextPipeActive      , cfg_planTextPipeActive      );
				if (_planTextPipeMaxMessages  < cfg_planTextPipeMaxMessages)  _planTextPipeMaxMessages  = doReconfigure(conn, CFGNAME_aseConfig_plan_text_pipe_max_messages, _planTextPipeMaxMessages , cfg_planTextPipeMaxMessages );

				_sampleSqlText    = doSqlText       && _sqlTextPipeActive   > 0 && _sqlTextPipeMaxMessages   > 0;
				_sampleStatements = doStatementInfo && _statementPipeActive > 0 && _statementPipeMaxMessages > 0;
				_samplePlan       = doPlanText      && _planTextPipeActive  > 0 && _planTextPipeMaxMessages  > 0;
			}
			
			_logger.info("ASE 'sql text pipe *'  configuration: '" + CFGNAME_aseConfig_sql_text_pipe_active  + "'=" + _sqlTextPipeActive   + ", '" + CFGNAME_aseConfig_sql_text_pipe_max_messages  + "'=" + _sqlTextPipeMaxMessages   + ".");
			_logger.info("ASE 'statement pipe *' configuration: '" + CFGNAME_aseConfig_statement_pipe_active + "'=" + _statementPipeActive + ", '" + CFGNAME_aseConfig_statement_pipe_max_messages + "'=" + _statementPipeMaxMessages + ".");
			_logger.info("ASE 'plan text pipe *' configuration: '" + CFGNAME_aseConfig_plan_text_pipe_active + "'=" + _planTextPipeActive  + ", '" + CFGNAME_aseConfig_plan_text_pipe_max_messages + "'=" + _planTextPipeMaxMessages  + ".");
		}
		catch(SQLException ex)
		{
			_sampleSqlText    = doSqlText;
			_sampleStatements = doStatementInfo;
			_samplePlan       = doPlanText;

			_logger.warn("Problems getting ASE Configuration for 'sql text pipe *', 'statement pipe *' or 'plan text pipe *', Trusting "+Version.getAppName()+" config instead (sampleSqlText="+_sampleSqlText+", sampleStatements="+_sampleStatements+", samplePlanText="+_samplePlan+"). Caught: "+ex);
		}
	}

	private void setSql(DbxConnection conn)
	{
		checkConfig(conn);
		
		long srvVersion = conn.getDbmsVersionNumber();

		// Get any specific where clause for the monSysStatements
		String statementWhereClause = getProperty(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_whereClause, PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_whereClause);
		if (StringUtil.hasValue(statementWhereClause))
			_logger.info("Applying user defined where clause when sampling monSysStatements. extra where clause appended is '"+statementWhereClause+"'.");
		else
			statementWhereClause = "1 = 1";

//		String RowsAffected          = "RowsAffected = convert(int, -1), ";
//		String ErrorStatus           = "ErrorStatus = convert(int, -1), ";
//		String HashKey               = "HashKey = convert(int, -1), ";
//		String SsqlId                = "SsqlId = convert(int, -1), ";
//		String DBName                = "DBName = db_name(DBID), ";
//		String ProcNestLevel         = "ProcNestLevel = convert(int, -1), ";
//		String StatementNumber       = "StatementNumber = convert(int, -1), ";
//		String InstanceID            = "InstanceID = convert(int, -1), ";
//		String ProcName              = "ProcName = isnull(isnull(object_name(ProcedureID,DBID),object_name(ProcedureID,2)),object_name(ProcedureID,db_id('sybsystemprocs'))), \n";
//		String ObjOwnerID            = "ObjOwnerID = convert(int, 0), \n";
//		String QueryOptimizationTime = "QueryOptimizationTime = convert(int, -1), ";
//		String ServerLogin           = "ServerLogin = convert(varchar(30), '-1'), \n";
		
		String RowsAffected          = "convert(int, -1) as RowsAffected, ";
		String ErrorStatus           = "convert(int, -1) as ErrorStatus, ";
		String HashKey               = "convert(int, -1) as HashKey, ";
		String SsqlId                = "convert(int, -1) as SsqlId, ";
		String DBName                = "db_name(DBID) as DBName, ";
		String ProcNestLevel         = "convert(int, -1) as ProcNestLevel, ";
		String StatementNumber       = "convert(int, -1) as StatementNumber, ";
		String InstanceID            = "convert(int, -1) as InstanceID, ";
		String ProcName              = "isnull(isnull(object_name(ProcedureID,DBID),object_name(ProcedureID,2)),object_name(ProcedureID,db_id('sybsystemprocs'))) as ProcName, \n";
		String ObjOwnerID            = "convert(int, 0) as ObjOwnerID, \n";
		String QueryOptimizationTime = "convert(int, -1) as QueryOptimizationTime, ";
		String ServerLogin           = "convert(varchar(30), '-1') as ServerLogin, \n";

		if (srvVersion >= Ver.ver(15,0,0,2) || (srvVersion >= Ver.ver(12,5,4) && srvVersion < Ver.ver(15,0)) )
		{
			RowsAffected    = "RowsAffected, ";
			ErrorStatus     = "ErrorStatus, ";
		}
		if (srvVersion >= Ver.ver(12,5,3))
		{
			ServerLogin     = "ServerLogin = suser_name(ServerUserID), ";
		}
		if (srvVersion >= Ver.ver(15,0,2))
		{
			HashKey         = "HashKey, ";
			SsqlId          = "SsqlId, ";
//			ProcName        = "ProcName = CASE WHEN SsqlId > 0 THEN object_name(SsqlId,2) ELSE isnull(object_name(ProcedureID,DBID), object_name(ProcedureID,db_id('sybsystemprocs'))) END, \n";
			ProcName        = "ProcName = CASE WHEN SsqlId > 0 THEN object_name(SsqlId,2) ELSE isnull(isnull(object_name(ProcedureID,DBID),object_name(ProcedureID,2)),object_name(ProcedureID,db_id('sybsystemprocs'))) END, \n"; // *sq dynamic SQL (ct_dynamic/prepared_stmnt) does NOT set the SsqlId column
		}
		if (srvVersion >= Ver.ver(15,0,2,3))
		{
			DBName          = "DBName, ";
		}
		if (srvVersion >= Ver.ver(15,0,3))
		{
			ProcNestLevel   = "ProcNestLevel, ";
			StatementNumber = "StatementNumber, ";
			ObjOwnerID      = "ObjOwnerID = CASE WHEN SsqlId > 0 THEN 0 ELSE object_owner_id(ProcedureID, DBID) END,";
		}
		if (srvVersion >= Ver.ver(15,5))
		{
			InstanceID      = "InstanceID, ";
		}
		
		// ASE 16.0 SP3
		if (srvVersion >= Ver.ver(16,0,0, 3)) // 16.0 SP3
		{
			QueryOptimizationTime       = "QueryOptimizationTime, ";
		}

		// Change some stuff if H2 database, remove various ASE functions
		if (_inTestCode)
		{
			DBName          = "DBName, ";
			ProcName        = "'dummy' as ProcName, \n";
		}
		
		_sql_sqlText 
			= "select getdate() as sampleTime, \n"
			+ "    "+InstanceID+"\n"
			+ "    SPID, \n"
			+ "    KPID, \n"
			+ "    BatchID, \n"
			+ "    SequenceInBatch, \n"
			+ "    "+ServerLogin+"\n"
			+ "    convert(int, -1) as AddMethod,\n"
			+ "    convert(int, -1) as JavaSqlLength,\n"
			+ "    convert(int, -1) as JavaSqlHashCode,\n"
			+ "    SQLText \n"
			+ "from master.dbo.monSysSQLText \n"
			+ "where 1 = 1 \n"
			+ "  and SPID != @@spid \n"
			+ "  and SPID not in (select spid from master.dbo.sysprocesses where program_name like '"+Version.getAppName()+"%') \n"
//			+ "order by SPID, KPID, BatchID, SequenceInBatch \n" // TODO: make this sort internal/after the rows has been fetched for less server side impact
			+ "";

		_sql_sqlStatements 
			= "select \n"
			+ "    getdate() as sampleTime, \n"
			+ "    "+InstanceID+"\n"
			+ "    SPID, \n"
			+ "    KPID, \n"
			+ "    DBID, \n"
			+ "    ProcedureID, \n"
			+ "    PlanID, \n"
			+ "    BatchID, \n"
			+ "    ContextID, \n"
			+ "    LineNumber, \n"
			+ "    " + ObjOwnerID + " \n"
			+ "    " + DBName + " \n"
			+ "    " + HashKey + " \n"
			+ "    " + SsqlId + " \n"
			+ "    " + ProcName + " \n"
			+ "    CASE WHEN datediff(day, StartTime, EndTime) >= 24 THEN -1 ELSE  datediff(ms, StartTime, EndTime) END as Elapsed_ms, \n"
			+ "    CpuTime, \n"
			+ "    WaitTime, \n"
			+ "    MemUsageKB, \n"
			+ "    PhysicalReads, \n"
			+ "    LogicalReads, \n"
			+ "    " + RowsAffected + " \n"
			+ "    " + ErrorStatus + " \n"
			+ "    " + ProcNestLevel + " \n"
			+ "    " + StatementNumber + " \n"
			+ "    " + QueryOptimizationTime + " \n"
			+ "    PagesModified, \n"
			+ "    PacketsSent, \n"
			+ "    PacketsReceived, \n"
			+ "    NetworkPacketSize, \n"
			+ "    PlansAltered, \n"
			+ "    StartTime, \n"
			+ "    EndTime, \n"
			+ "    convert(int, -1) as JavaSqlHashCode\n"
			+ "from master.dbo.monSysStatement \n"
			+ "where 1 = 1 \n"
			+ "  and SPID != @@spid \n"
			+ "  and SPID not in (select spid from master.dbo.sysprocesses where program_name like '"+Version.getAppName()+"%') "
			+ "  and " + statementWhereClause
			+ "";
		
		_sql_sqlPlanText 
			= "select getdate() as sampleTime, \n"
			+ "    "+InstanceID+"\n"
			+ "    SPID, \n"
			+ "    KPID, \n"
			+ "    PlanID, \n"
			+ "    BatchID, \n"
			+ "    ContextID, \n"
			+ "    SequenceNumber, \n"
			+ "    DBID, \n"
			+ "    "+DBName+"\n"
			+ "    ProcedureID, \n"
			+ "    convert(int, -1) as AddMethod,\n"
			+ "    PlanText \n"
			+ "from master.dbo.monSysPlanText \n"
			+ "where 1 = 1 \n"
			+ "  and SPID != @@spid \n"
			+ "  and SPID not in (select spid from master.dbo.sysprocesses where program_name like '"+Version.getAppName()+"%') \n"
//			+ "order by SPID, KPID, BatchID, SequenceNumber \n" // TODO: make this sort internal/after the rows has been fetched for less server side impact
			+ "";
		
		if (_inTestCode)
		{
			// if we are using H2 for test: just strip off some stuff
			_sql_sqlStatements = _sql_sqlStatements.replace("master.dbo.", "");
			_sql_sqlText       = _sql_sqlText      .replace("master.dbo.", "");
			_sql_sqlPlanText   = _sql_sqlPlanText  .replace("master.dbo.", "");
			
			_sql_sqlStatements = _sql_sqlStatements.replace("@@spid", "-99999");
			_sql_sqlText       = _sql_sqlText      .replace("@@spid", "-99999");
			_sql_sqlPlanText   = _sql_sqlPlanText  .replace("@@spid", "-99999");
		}
	}

	/**
	 * Primary Key for SqlText, Statements, PlanText
	 * @author gorans
	 */
	protected class PK
	{
		int SPID;
		int KPID;
		int BatchID;
		
		public PK(int SPID, int KPID, int BatchID)
		{
			this.SPID    = SPID;
			this.KPID    = KPID;
			this.BatchID = BatchID;
		}

		// Genereated from Eclipse
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
//			result = prime * result + getOuterType().hashCode();
			result = prime * result + BatchID;
			result = prime * result + KPID;
			result = prime * result + SPID;
			return result;
		}

		// Genereated from Eclipse
		@Override
		public boolean equals(Object obj)
		{
			if ( this == obj )
				return true;
			if ( obj == null )
				return false;
			if ( getClass() != obj.getClass() )
				return false;
			PK other = (PK) obj;
//			if ( !getOuterType().equals(other.getOuterType()) )
//				return false;
//			if ( BatchID != other.BatchID )
//				return false;
//			if ( SPID != other.SPID )
//				return false;
//			return true;
			
			return SPID == other.SPID && KPID == other.KPID && BatchID == other.BatchID; 
		}

//		// Genereated from Eclipse
//		private SqlCaptureBrokerAse getOuterType()
//		{
//			return SqlCaptureBrokerAse.this;
//		}
		
		@Override
		public String toString()
		{
			return "PK [SPID=" + SPID + ", KPID=" + KPID +", BatchID=" + BatchID + "]" + "@" + Integer.toHexString(hashCode());
		}

		public boolean equals(int spid, int kpid, int batchId)
		{
			return this.SPID == spid && this.KPID == kpid && this.BatchID == batchId;
		}
	}

	/**
	 * Save detailed information about executed SQL.
	 * <p>
	 * One culprit that makes this a bit <i>hard</i> is how ASE stores data in the tables monSysStatement, monSysSQLText and monSysPlanText<br>
	 * For example when exeuting SQL: <code>waitfor delay '00:01:00'</code> (waitfor 1 minute) ASE Stores the following.
	 * <ul>
	 *   <li>At sql-exec-start-time; ASE stores values in: monSysSQLText and monSysPlanText</li>
	 *   <li>At sql-exec-end-time: ASE stores values in: monSysStatement</li>
	 * </ul>
	 * So for <i>long running</i> SQL executions, the SQL-Text and Statement information will not be collected <i>in the same method call</i>
	 * which means that we end up storing some <i>extra</i> SQL-Text, due to the fact that no Statement (filtering) information was avalibale
	 * when collecting the SQL-Text/Plan.   
	 * 
	 * <p>
	 * Below is the overall Logic used.
	 * <ul>
	 *   <li>get all SQL-Statements (from table: monSysStatement)<br>
	 *     Some special logic for Statements
	 *     <ul>
	 *       <li>Only keep Statements that exceeds filters, for example if execution time is above ### ms, or LogicalReads is above ##</li>
	 *       <li>If it's a StoredProc: we might want to send it of for DDL lookup, so that the proc text is stored as well.</li>
	 *     </ul>
	 *   </li>
	 *   <li>get all SQL-Text       (from table: monSysSQLText)</li>
	 *   <li>get all SQL-Plans      (from table: monSysPlanText)</li>
	 * </ul>
	 * After the "get" phase, we will do "post" phase, where we can discard SQL-Text and SQL-Plans 
	 * that has been discarded by the Statement filter logic above (meaning: if the Statement was below ExecutionTime 
	 * or below LogicalReads etc, then <b>do not store</b> the SQL-Text or SQL-Plan.<br>
	 * In the "post" phase we will also save the SQL-Text information for X number of calls to doSqlCapture(), this is keept in a "Deferred SQL Queue".<br>
	 * This is also an attempt to <b>not</b> sent to may extra SQL-Text records
	 * 
	 * Below are what's happening in the "post" phase
	 * <ul>
	 *   <li>Remove SQL Text and Plans that the Statement collector has filtered out (due to execTime or *Reads)</li>
	 *   <li>Remove some static SQL Text(s) that ASE has inserted without an assosiated Statement record<br>
	 *       Those SQL Texts are normally assosiated with 'set someOption' or dynamic sql using 
	 *       lightwaight procedures (ct_dynamic or jdbc: PreparedStatements)</li>
	 * </ul>
	 * 
	 * Then we will enter the "post" phase where the records are sent to the PCS - Persistent Counter Storage
	 * <ul>
	 *   <li>Send info to the PCS</li>
	 *   <li>Remove any SQL information in the <i>Deferred SQL Queue</i> </li>
	 * </ul>
	 * 
	 */
	@Override
	public int doSqlCapture(DbxConnection conn, PersistentCounterHandler pch)
	{
		if (_inTestCode)
			_clearBeforeFirstPoll = false;
			
		if (_firstPoll && _clearBeforeFirstPoll)
		{
			// If first time... discard everything in the transient monSysStatement table
			if (_sampleSqlText)    clearTable(conn, "monSysSQLText");
			if (_sampleStatements) clearTable(conn, "monSysStatement");
			if (_samplePlan)       clearTable(conn, "monSysPlanText");

			_firstPoll = false;
		}


//		int addCount = 0;
//		int statementReadCount = 0;
//		int statementAddCount = 0;
//		int statementDiscardCount = 0;
		
		// 
		Map<PK, List<Object>> sqlTextPkMap   = new HashMap<>();
//		List<List<Object>>    sqlTextRecords = new ArrayList<>();  // TODO: can we get rid of this and only use, the above MAP

		Map<PK, List<Object>> planTextPkMap   = new HashMap<>();
//		List<List<Object>>    planTextRecords = new ArrayList<>();  // TODO: can we get rid of this and only use, the above MAP

		List<List<Object>> statementRecords = new ArrayList<>();
		Set<PK> statementPkAdded            = new HashSet<>();
		Set<PK> statementPkDiscarded        = new HashSet<>();

		
		// If SQL queries are NOT initialized, do it now
		if (_sql_sqlText == null)
			setSql(conn);

		//------------------------------------------------
		// SQL STATEMENTS
		// - is available in the table monSysStatement *after* the statement has been executed
		//   so it it takes 2 minutes to execute, SQL-Text (and plan) will first be available, 
		//   then after 2 minutes the statement-statistics will end up here
		//------------------------------------------------
		if (_sampleStatements)
		{
			// Get some configuration
			int     saveStatement_gt_execTime         = getIntProperty    (PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_execTime,         PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_execTime);
			int     saveStatement_gt_logicalReads     = getIntProperty    (PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_logicalReads,     PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_logicalReads);
			int     saveStatement_gt_physicalReads    = getIntProperty    (PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_physicalReads,    PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_physicalReads);

			boolean sendDdlForLookup                  = getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup,                  PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup);
			int     sendDdlForLookup_gt_execTime      = getIntProperty    (PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_execTime,      PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_execTime);
			int     sendDdlForLookup_gt_logicalReads  = getIntProperty    (PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_logicalReads,  PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_logicalReads);
			int     sendDdlForLookup_gt_physicalReads = getIntProperty    (PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_physicalReads, PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_physicalReads);

//			long srvVersion = conn.getDbmsVersionNumber();
			
			try
			{
				Statement stmnt = conn.createStatement();
				ResultSet rs = stmnt.executeQuery(_sql_sqlStatements);
				int colCount = rs.getMetaData().getColumnCount();
				int rowCount = 0;
				while(rs.next())
				{
					rowCount++;
//					statementReadCount++;

					int SPID          = rs.getInt("SPID");
					int KPID          = rs.getInt("KPID");
					int BatchID       = rs.getInt("BatchID");
					int execTime      = rs.getInt("Elapsed_ms");
					int logicalReads  = rs.getInt("LogicalReads");
					int physicalReads = rs.getInt("PhysicalReads");
					int errorStatus   = rs.getInt("ErrorStatus");

					int cpuTime       = rs.getInt("CpuTime");
					int waitTime      = rs.getInt("WaitTime");
					int rowsAffected  = rs.getInt("RowsAffected"); // RowsAffected was introduced in ASE 12.5.4 (but before that we genereate -1) 

//					int procedureId   = rs.getInt("ProcedureID");
//					int ssqlId        = rs.getInt("SsqlId");
//					int ContextID     = rs.getInt("ContextID");

					String procName   = rs.getString("ProcName");
					int    lineNumber = rs.getInt("LineNumber");


					// To be used by keep/discard Set
					PK pk = new PK(SPID, KPID, BatchID);

					// add information to the StatementStatistics subsystem
					// which is used by a CM to check how Statement Execution within ranges
					// For Example: how many Statements that executed, between
					//     0-10 ms
					//     10-50 ms
					//     50-100 ms
					//     100-250 ms
					//     250-500 ms
					//     500-1000 ms
					//     1000-2500 ms
					//     2500-5000 ms
					//     5-10 sec
					//     10-15 sec
					//     15-20 sec
					//     20-30 sec
					//     above 30 sec
					if (_statementStatistics != null)
						updateStatementStats(execTime, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, procName, lineNumber);

					
					//System.out.println("Statement CHECK if above THRESHOLD: SPID="+SPID+",KPID="+KPID+",BatchID="+BatchID+": execTime="+execTime+", logicalReads="+logicalReads+", physicalReads="+physicalReads+".   SAVE="+(execTime > saveStatement_gt_execTime && logicalReads > saveStatement_gt_logicalReads && physicalReads > saveStatement_gt_physicalReads));
					// Add only rows that are above the limits
					if ( (    execTime      > saveStatement_gt_execTime
					       && logicalReads  > saveStatement_gt_logicalReads
					       && physicalReads > saveStatement_gt_physicalReads
					     )
					     || errorStatus   > 0
					   )
					{
						if (sendDdlForLookup)
						{
							String ProcName = rs.getString("ProcName");

							// send ProcName or StatementCacheEntry to the DDL Capture
							if (StringUtil.hasValue(ProcName))
							{
								String DBName        = rs.getString("DBName");

								// Only send if it's above the defined limits
								if (    execTime      > sendDdlForLookup_gt_execTime
								     && logicalReads  > sendDdlForLookup_gt_logicalReads
								     && physicalReads > sendDdlForLookup_gt_physicalReads
								   )
								{
									// if it's a statement cache entry, populate it into the cache so that the DDL Lookup wont have to sleep 
									if (ProcName.startsWith("*ss") || ProcName.startsWith("*sq") ) // *sq in ASE 15.7 esd#2, DynamicSQL can/will end up in statement cache
									{
										if (XmlPlanCache.hasInstance())
										{
											XmlPlanCache xmlPlanCache = XmlPlanCache.getInstance();
											if ( ! xmlPlanCache.isPlanCached(ProcName) )
											{
												xmlPlanCache.getPlan(ProcName);
											}
										}
										else
										{
											_logger.info("XmlPlanCache do not have an instance. Skipping XML Plan lookup for name '"+ProcName+"'.");
										}
									}
									
									// Now add the ProcName/StatementCacheEntryName to the DDL Lookup handler
									if (pch != null)
										pch.addDdl(DBName, ProcName, "SqlCapture");
								}
							}
						} //end: sendDdlForLookup

						List<Object> row = new ArrayList<Object>();
						row.add(MON_SQL_STATEMENT); // NOTE: the first object in the list should be the TableName where to store the data
						for (int c=1; c<=colCount; c++)
							row.add(rs.getObject(c));

						statementRecords.add(row);
						
						// Keep track of what PK's we have added (procs can have many statements, so it can be in both add and skipp Set)
						statementPkAdded.add(pk);
					}
					else // save DISCARDED records in a "skip" list so we can remove the SqlText and PlanText at the end
					{
						// Keep track of what PK's we have discarded (procs can have many statements, so it can be in both add and skipp Set)
						statementPkDiscarded.add(pk);
					}
				}
				rs.close();
				stmnt.close();
				
				int    configVal  = _statementPipeMaxMessages;
				String configName = CFGNAME_aseConfig_statement_pipe_max_messages;
				if (rowCount >= configVal)
				{
					_lastConfigOverflowMsgVCnt_statementPipeMaxMessages++;
					_lastConfigOverflowMsgVSum_statementPipeMaxMessages += rowCount;

					// Warning on firt time or every X minute/hour
					if (_lastConfigOverflowMsgTime_statementPipeMaxMessages == -1 || TimeUtils.msDiffNow(_lastConfigOverflowMsgTime_statementPipeMaxMessages) > _lastConfigOverflowMsgTimeThreshold)
					{
						_logger.warn("The configuration '"+configName+"' might be to low. " 
								+ "For the last '"+TimeUtils.msToTimeStr("%HH:%MM", _lastConfigOverflowMsgTimeThreshold)+"' (HH:MM), "
								+ "We have read "+_lastConfigOverflowMsgVSum_statementPipeMaxMessages+" rows. "
								+ "On " + _lastConfigOverflowMsgVCnt_statementPipeMaxMessages + " occations. "
								+ "Average read per occation was " + (_lastConfigOverflowMsgVSum_statementPipeMaxMessages / _lastConfigOverflowMsgVCnt_statementPipeMaxMessages) + " rows. "
								+ "And the configuration value for '"+configName+"' is "+configVal);

						// Reset the values, so we can print new message in X minutes/hours
						_lastConfigOverflowMsgTime_statementPipeMaxMessages = System.currentTimeMillis();
						_lastConfigOverflowMsgVCnt_statementPipeMaxMessages = 0;
						_lastConfigOverflowMsgVSum_statementPipeMaxMessages = 0;
					}
				}
			}
			catch(SQLException ex)
			{
				// Not configured
				if (ex.getErrorCode() == 12052)
					_sampleStatements = false;
				
				_logger.error("SQL Capture problems when capturing 'SQL Statements' Caught "+AseConnectionUtils.sqlExceptionToString(ex) + " when executing SQL: "+_sql_sqlStatements);
			}
		}

		//------------------------------------------------
		// SQL TEXT
		// - is available in the table monSysSQLText when the SQL *starts* to execute (or after it has been optimized)
		//   The statement-information/statistics will on the other hand be available *after* the statement has finnished to execute
		//------------------------------------------------
//		if (_sampleSqlText)
//		{
//			try
//			{
//				//----------------------------------------------------------------------------------------
//				// This might look a bit odd / backwards, but look at the data example at the end of this file
//				// SQLText can have several rows
//				// * so when FIRST row (for every group) is discovered - Start a new "row" (which is a List: tableNameToStoreItIn, c1-data, c2-data, c3-data... SQLText-concatenated-data)
//				// * on all "extra" rows that will just contain same c1-data, c2-data, c3-data but the SQLText will be read and appended to a StringBuilder
//				// * when a "new" row is found, we will set the StringBuffer content to replace the "first row" SQLText
//				// * At the END we will need to "post" the information from the last "group"
//				//----------------------------------------------------------------------------------------
//				Statement stmnt = conn.createStatement();
//				ResultSet rs = stmnt.executeQuery(_sql_sqlText);
//				ResultSetMetaData rsmd = rs.getMetaData();
//				int           colCount = rsmd.getColumnCount();
//				int           sequenceCol_pos = -1;
//				int           sequenceCol_val = -1;
//				int           seqTextCol_pos  = -1;
//				StringBuilder seqTextCol_val  = new StringBuilder();
//				for (int c=1; c<=colCount; c++)
//				{
//					if      ("SequenceInBatch".equals(rsmd.getColumnLabel(c))) sequenceCol_pos = c;
//					else if ("SQLText"        .equals(rsmd.getColumnLabel(c))) seqTextCol_pos  = c;
//				}
//				List<Object> row = null;
//				int rowCount = 0;
//				while(rs.next())
//				{
//					rowCount++;
//
//					// The below implies that the data is sorted by: SPID|KPID, BatchID, SequenceInBatch
//					sequenceCol_val = rs.getInt(sequenceCol_pos);
//					if (sequenceCol_val == 1)
//					{
//						if (row != null)
//						{
//							row.set(row.size()-1, seqTextCol_val.toString());
//							seqTextCol_val.setLength(0);
//
//							sqlTextRecords.add(row);
//						}
//
//						row = new ArrayList<Object>();
//						row.add(MON_SQL_TEXT); // NOTE: the first object in the list should be the TableName where to store the data
//						
//						// Add the ROW to the sqlTextPkMap so we can delete it at the end if not used...
//						int SPID    = rs.getInt("SPID");
//						int KPID    = rs.getInt("KPID");
//						int BatchID = rs.getInt("BatchID");
//
//						sqlTextPkMap.put(new PK(SPID, KPID, BatchID), row);
//						
//						for (int c=1; c<=colCount; c++)
//						{
//							// the sequence column should NOT be part of the result
//							if (c != sequenceCol_pos)
//								row.add(rs.getObject(c));
//						}
//						seqTextCol_val.append( rs.getString(seqTextCol_pos) );
//					}
//					else if (row != null)
//					{
//						seqTextCol_val.append( rs.getString(seqTextCol_pos) );
//					}
//				}
//				// Finally add content from LAST row, since we do addCountainer() only on NEW sequenceCol_val == 1
//				if (row != null)
//				{
//					row.set(row.size()-1, seqTextCol_val.toString());
//					seqTextCol_val.setLength(0);
//
//					sqlTextRecords.add(row);
//				}
//				
//				rs.close();
//				stmnt.close();
//
//				int    configVal  = _sqlTextPipeMaxMessages;
//				String configName = CFGNAME_aseConfig_sql_text_pipe_max_messages;
//				if (rowCount >= configVal)
//				{
//					_lastConfigOverflowMsgVCnt_sqlTextPipeMaxMessages++;
//					_lastConfigOverflowMsgVSum_sqlTextPipeMaxMessages += rowCount;
//
//					// Warning on firt time or every X minute/hour
//					if (_lastConfigOverflowMsgTime_sqlTextPipeMaxMessages == -1 || TimeUtils.msDiffNow(_lastConfigOverflowMsgTime_sqlTextPipeMaxMessages) > _lastConfigOverflowMsgTimeThreshold)
//					{
//						_logger.warn("The configuration '"+configName+"' might be to low. " 
//								+ "For the last '"+TimeUtils.msToTimeStr("%HH:%MM", _lastConfigOverflowMsgTimeThreshold)+"' (HH:MM), "
//								+ "We have read "+_lastConfigOverflowMsgVSum_sqlTextPipeMaxMessages+" rows. "
//								+ "On " + _lastConfigOverflowMsgVCnt_sqlTextPipeMaxMessages + " occations. "
//								+ "Average read per occation was " + (_lastConfigOverflowMsgVSum_sqlTextPipeMaxMessages / _lastConfigOverflowMsgVCnt_sqlTextPipeMaxMessages) + " rows. "
//								+ "And the configuration value for '"+configName+"' is "+configVal);
//
//						// Reset the values, so we can print new message in X minutes/hours
//						_lastConfigOverflowMsgTime_sqlTextPipeMaxMessages = System.currentTimeMillis();
//						_lastConfigOverflowMsgVCnt_sqlTextPipeMaxMessages = 0;
//						_lastConfigOverflowMsgVSum_sqlTextPipeMaxMessages = 0;
//					}
//				}
//			}
//			catch(SQLException ex)
//			{
//				// Not configured
//				if (ex.getErrorCode() == 12052)
//					_sampleSqlText = false;
//				
//				_logger.error("SQL Capture problems when capturing 'SQL Text' Caught "+AseConnectionUtils.sqlExceptionToString(ex) + " when executing SQL: "+_sql_sqlText);
//			}
//		}

		if (_sampleSqlText)
		{
			try ( Statement stmnt = conn.createStatement();
			      ResultSet rs = stmnt.executeQuery(_sql_sqlText); )
			{
				//----------------------------------------------------------------------------------------
				// SQLText can have several rows, each with the same SPID, KPID, BatchID, and SequenceInBatch incremented
				// * Simply create a PK from (SPID, KPID, BatchID)
				// * Stuff PK in a MAP
				//   If the entry not exists (add "all" columns to a List)
				//   If the entry do exists  (append SQLText, which is kept as the LAST entry in the List)
				//----------------------------------------------------------------------------------------
				ResultSetMetaData rsmd = rs.getMetaData();
				int colCount = rsmd.getColumnCount();
				int SPID_pos            = -1;
				int KPID_pos            = -1;
				int BatchID_pos         = -1;
				int SequenceInBatch_pos = -1;
				int SQLText_pos         = -1;

				// Get columns positions...
				for (int c=1; c<=colCount; c++)
				{
					String colName = rsmd.getColumnLabel(c);

					if      ("SPID"           .equals(colName)) SPID_pos            = c;
					else if ("KPID"           .equals(colName)) KPID_pos            = c;
					else if ("BatchID"        .equals(colName)) BatchID_pos         = c;
					else if ("SequenceInBatch".equals(colName)) SequenceInBatch_pos = c;
					else if ("SQLText"        .equals(colName)) SQLText_pos         = c;
				}

				PK pk = new PK(-99, -99, -99); // create a dummy, just so we have a object
				int rowCount = 0;
				while(rs.next())
				{
					rowCount++;

					int SPID    = rs.getInt(SPID_pos);
					int KPID    = rs.getInt(KPID_pos);
					int BatchID = rs.getInt(BatchID_pos);
					
					// This is where we can update for example:
					//   - How many "dynamic SQL Prepare" ---> 'create proc dyn###'
					//   - etc, etc..
					if (_sqlTextStatistics != null)
					{
						int SequenceInBatch = rs.getInt   (SequenceInBatch_pos);
						String sqlText      = rs.getString(SQLText_pos);

						updateSqlTextStats(SPID, KPID, BatchID, SequenceInBatch, sqlText);
					}


					// If not same PK as previous row, create a new PK Object
					if ( ! pk.equals(SPID, KPID, BatchID) )
						pk = new PK(SPID, KPID, BatchID);

					// Get PK from MAP
					List<Object> row = sqlTextPkMap.get(pk);

					// Is it a new row, or did we already have an entry
					if (row == null)
					{
						// Create NEW record
						row = new ArrayList<Object>();
						row.add(MON_SQL_TEXT); // NOTE: the first object in the list should be the TableName where to store the data

						for (int c=1; c<=colCount; c++)
						{
							// the "SequenceInBatch" column should NOT be part of the result
							if (c != SequenceInBatch_pos)
								row.add(rs.getObject(c));
						}
						
						// set the record in MAP
						sqlTextPkMap.put(pk, row);
					}
					else
					{
						// Append SQLText to current entry
						Object sqlTextObj = row.get(row.size()-1); // SQLText is LAST pos in List
						String sqlTextRs  = rs.getString(SQLText_pos);
						
						row.set(row.size()-1, sqlTextObj + sqlTextRs);
					}
				}

				int    configVal  = _sqlTextPipeMaxMessages;
				String configName = CFGNAME_aseConfig_sql_text_pipe_max_messages;
				if (rowCount >= configVal)
				{
					_lastConfigOverflowMsgVCnt_sqlTextPipeMaxMessages++;
					_lastConfigOverflowMsgVSum_sqlTextPipeMaxMessages += rowCount;

					// Warning on firt time or every X minute/hour
					if (_lastConfigOverflowMsgTime_sqlTextPipeMaxMessages == -1 || TimeUtils.msDiffNow(_lastConfigOverflowMsgTime_sqlTextPipeMaxMessages) > _lastConfigOverflowMsgTimeThreshold)
					{
						_logger.warn("The configuration '"+configName+"' might be to low. " 
								+ "For the last '"+TimeUtils.msToTimeStr("%HH:%MM", _lastConfigOverflowMsgTimeThreshold)+"' (HH:MM), "
								+ "We have read "+_lastConfigOverflowMsgVSum_sqlTextPipeMaxMessages+" rows. "
								+ "On " + _lastConfigOverflowMsgVCnt_sqlTextPipeMaxMessages + " occations. "
								+ "Average read per occation was " + (_lastConfigOverflowMsgVSum_sqlTextPipeMaxMessages / _lastConfigOverflowMsgVCnt_sqlTextPipeMaxMessages) + " rows. "
								+ "And the configuration value for '"+configName+"' is "+configVal);

						// Reset the values, so we can print new message in X minutes/hours
						_lastConfigOverflowMsgTime_sqlTextPipeMaxMessages = System.currentTimeMillis();
						_lastConfigOverflowMsgVCnt_sqlTextPipeMaxMessages = 0;
						_lastConfigOverflowMsgVSum_sqlTextPipeMaxMessages = 0;
					}
				}
			}
			catch(SQLException ex)
			{
				// Not configured
				if (ex.getErrorCode() == 12052)
					_sampleSqlText = false;
				
				_logger.error("SQL Capture problems when capturing 'SQL Text' Caught "+AseConnectionUtils.sqlExceptionToString(ex) + " when executing SQL: "+_sql_sqlText);
			}
		}

		//------------------------------------------------
		// SQL PLANS
		// - is available in the table monSysPlanText when the SQL *starts* to execute (or after it has been optimized)
		//   The statement-information/statistics will on the other hand be available *after* the statement has finnished to execute
		//------------------------------------------------
//		if (_samplePlan)
//		{
//			try
//			{
//				//----------------------------------------------------------------------------------------
//				// This might look a bit odd / backwards, but look at the data example at the end of this file
//				// PlanText can have several rows
//				// * so when FIRST row (for every group) is discovered - Start a new "row" (which is a List: tableNameToStoreItIn, c1-data, c2-data, c3-data... PlanText-concatenated-data)
//				// * on all "extra" rows that will just contain same c1-data, c2-data, c3-data but the PlanText will be read and appended to a StringBuilder
//				// * when a "new" row is found, we will set the StringBuffer content to replace the "first row" PlanText
//				// * At the END we will need to "post" the information from the last "group"
//				//----------------------------------------------------------------------------------------
//				Statement stmnt = conn.createStatement();
//				ResultSet rs = stmnt.executeQuery(_sql_sqlPlanText);
//				ResultSetMetaData rsmd = rs.getMetaData();
//				int           colCount = rsmd.getColumnCount();
//				int           sequenceCol_pos = -1;
//				int           sequenceCol     = -1;
//				int           seqTextCol_pos  = -1;
//				StringBuilder seqTextCol_val  = new StringBuilder();
//				for (int c=1; c<=colCount; c++)
//				{
//					if      ("SequenceNumber".equals(rsmd.getColumnLabel(c))) sequenceCol_pos = c;
//					else if ("PlanText"      .equals(rsmd.getColumnLabel(c))) seqTextCol_pos  = c;
//				}
//				List<Object> row = null;
//				int rowCount = 0;
//				while(rs.next())
//				{
//					rowCount++;
//
//					// The below implies that the data is sorted by: SPID|KPID, BatchID, SequenceNumber
//					sequenceCol = rs.getInt(sequenceCol_pos);
//					if (sequenceCol == 1)
//					{
//						if (row != null)
//						{
//							row.set(row.size()-1, seqTextCol_val.toString());
//							seqTextCol_val.setLength(0);
//
//							planTextRecords.add(row);
//						}
//
//						row = new ArrayList<Object>();
//						row.add(MON_SQL_PLAN); // NOTE: the first object in the list should be the TableName where to store the data
//
//						// Add the ROW to the planTextPkMap so we can delete it at the end if not used...
//						int SPID    = rs.getInt("SPID");
//						int KPID    = rs.getInt("KPID");
//						int BatchID = rs.getInt("BatchID");
//
//						planTextPkMap.put(new PK(SPID, KPID, BatchID), row);
//						
//						for (int c=1; c<=colCount; c++)
//						{
//							// the sequence column should NOT be part of the result
//							if (c != sequenceCol_pos)
//								row.add(rs.getObject(c));
//						}
//						seqTextCol_val.append( rs.getString(seqTextCol_pos) );
//					}
//					else if (row != null)
//					{
//						seqTextCol_val.append( rs.getString(seqTextCol_pos) );
//					}
//				}
//				// Finally add content from LAST row, since we do addCountainer() only on NEW sequenceCol_val == 1
//				if (row != null)
//				{
//					row.set(row.size()-1, seqTextCol_val.toString());
//					seqTextCol_val.setLength(0);
//
//					planTextRecords.add(row);
//				}
//
//				rs.close();
//				stmnt.close();
//
//				int    configVal  = _planTextPipeMaxMessages;
//				String configName = CFGNAME_aseConfig_plan_text_pipe_max_messages;
////				if (rowCount >= configVal)
////					_logger.warn("The configuration '"+configName+"' might be to low. Just read "+rowCount+" rows. And the configuration is "+configVal);
//				if (rowCount >= configVal)
//				{
//					_lastConfigOverflowMsgVCnt_planTextPipeMaxMessages++;
//					_lastConfigOverflowMsgVSum_planTextPipeMaxMessages += rowCount;
//
//					// Warning on firt time or every X minute/hour
//					if (_lastConfigOverflowMsgTime_planTextPipeMaxMessages == -1 || TimeUtils.msDiffNow(_lastConfigOverflowMsgTime_planTextPipeMaxMessages) > _lastConfigOverflowMsgTimeThreshold)
//					{
//						_logger.warn("The configuration '"+configName+"' might be to low. " 
//								+ "For the last '"+TimeUtils.msToTimeStr("%HH:%MM", _lastConfigOverflowMsgTimeThreshold)+"' (HH:MM), "
//								+ "We have read "+_lastConfigOverflowMsgVSum_planTextPipeMaxMessages+" rows. "
//								+ "On " + _lastConfigOverflowMsgVCnt_planTextPipeMaxMessages + " occations. "
//								+ "Average read per occation was " + (_lastConfigOverflowMsgVSum_planTextPipeMaxMessages / _lastConfigOverflowMsgVCnt_planTextPipeMaxMessages) + " rows. "
//								+ "And the configuration value for '"+configName+"' is "+configVal);
//
//						// Reset the values, so we can print new message in X minutes/hours
//						_lastConfigOverflowMsgTime_planTextPipeMaxMessages = System.currentTimeMillis();
//						_lastConfigOverflowMsgVCnt_planTextPipeMaxMessages = 0;
//						_lastConfigOverflowMsgVSum_planTextPipeMaxMessages = 0;
//					}
//				}
//			}
//			catch(SQLException ex)
//			{
//				// Msg 12052, Level 17, State 1:
//				// Server 'GORAN_UB2_DS', Line 1 (script row 862), Status 0, TranState 1:
//				// Collection of monitoring data for table 'monSysPlanText' requires that the 'plan text pipe max messages', 'plan text pipe active' configuration option(s) be enabled. To set the necessary configuration, contact a user who has the System Administrator (SA) role.
//				if (ex.getErrorCode() == 12052)
//					_samplePlan = false;
//				
//				_logger.error("SQL Capture problems when capturing 'SQL Plan Text' Caught "+AseConnectionUtils.sqlExceptionToString(ex) + " when executing SQL: "+_sql_sqlPlanText);
//			}
//		}

		if (_samplePlan)
		{
			try ( Statement stmnt = conn.createStatement();
			      ResultSet rs = stmnt.executeQuery(_sql_sqlPlanText); )
			{
				//----------------------------------------------------------------------------------------
				// This might look a bit odd / backwards, but look at the data example at the end of this file
				// PlanText can have several rows
				// * so when FIRST row (for every group) is discovered - Start a new "row" (which is a List: tableNameToStoreItIn, c1-data, c2-data, c3-data... PlanText-concatenated-data)
				// * on all "extra" rows that will just contain same c1-data, c2-data, c3-data but the PlanText will be read and appended to a StringBuilder
				// * when a "new" row is found, we will set the StringBuffer content to replace the "first row" PlanText
				// * At the END we will need to "post" the information from the last "group"
				//----------------------------------------------------------------------------------------
				ResultSetMetaData rsmd = rs.getMetaData();

				int colCount = rsmd.getColumnCount();
				int SPID_pos           = -1;
				int KPID_pos           = -1;
				int BatchID_pos        = -1;
				int SequenceNumber_pos = -1;
				int PlanText_pos       = -1;

				for (int c=1; c<=colCount; c++)
				{
					String colName = rsmd.getColumnLabel(c);

					if      ("SPID"          .equals(colName)) SPID_pos           = c;
					else if ("KPID"          .equals(colName)) KPID_pos           = c;
					else if ("BatchID"       .equals(colName)) BatchID_pos        = c;
					else if ("SequenceNumber".equals(colName)) SequenceNumber_pos = c;
					else if ("PlanText"      .equals(colName)) PlanText_pos       = c;
				}

				PK pk = new PK(-99, -99, -99); // create a dummy, just so we have a object
				int rowCount = 0;
				while(rs.next())
				{
					rowCount++;

					int SPID    = rs.getInt(SPID_pos);
					int KPID    = rs.getInt(KPID_pos);
					int BatchID = rs.getInt(BatchID_pos);

					// If not same PK as previous row, create a new PK Object
					if ( ! pk.equals(SPID, KPID, BatchID) )
						pk = new PK(SPID, KPID, BatchID);

					// Get PK from MAP
					List<Object> row = planTextPkMap.get(pk);

					// Is it a new row, or did we already have an entry
					if (row == null)
					{
						// Create NEW record
						row = new ArrayList<Object>();
						row.add(MON_SQL_PLAN); // NOTE: the first object in the list should be the TableName where to store the data

						for (int c=1; c<=colCount; c++)
						{
							// the "SequenceNumber" column should NOT be part of the result
							if (c != SequenceNumber_pos)
								row.add(rs.getObject(c));
						}
						
						// set the record in MAP
						planTextPkMap.put(pk, row);
					}
					else
					{
						// Append SQLText to current entry
						Object planTextObj = row.get(row.size()-1); // SQLText is LAST pos in List
						String planTextRs  = rs.getString(PlanText_pos);
						
						row.set(row.size()-1, planTextObj + planTextRs);
					}
				}

				int    configVal  = _planTextPipeMaxMessages;
				String configName = CFGNAME_aseConfig_plan_text_pipe_max_messages;
//				if (rowCount >= configVal)
//					_logger.warn("The configuration '"+configName+"' might be to low. Just read "+rowCount+" rows. And the configuration is "+configVal);
				if (rowCount >= configVal)
				{
					_lastConfigOverflowMsgVCnt_planTextPipeMaxMessages++;
					_lastConfigOverflowMsgVSum_planTextPipeMaxMessages += rowCount;

					// Warning on firt time or every X minute/hour
					if (_lastConfigOverflowMsgTime_planTextPipeMaxMessages == -1 || TimeUtils.msDiffNow(_lastConfigOverflowMsgTime_planTextPipeMaxMessages) > _lastConfigOverflowMsgTimeThreshold)
					{
						_logger.warn("The configuration '"+configName+"' might be to low. " 
								+ "For the last '"+TimeUtils.msToTimeStr("%HH:%MM", _lastConfigOverflowMsgTimeThreshold)+"' (HH:MM), "
								+ "We have read "+_lastConfigOverflowMsgVSum_planTextPipeMaxMessages+" rows. "
								+ "On " + _lastConfigOverflowMsgVCnt_planTextPipeMaxMessages + " occations. "
								+ "Average read per occation was " + (_lastConfigOverflowMsgVSum_planTextPipeMaxMessages / _lastConfigOverflowMsgVCnt_planTextPipeMaxMessages) + " rows. "
								+ "And the configuration value for '"+configName+"' is "+configVal);

						// Reset the values, so we can print new message in X minutes/hours
						_lastConfigOverflowMsgTime_planTextPipeMaxMessages = System.currentTimeMillis();
						_lastConfigOverflowMsgVCnt_planTextPipeMaxMessages = 0;
						_lastConfigOverflowMsgVSum_planTextPipeMaxMessages = 0;
					}
				}
			}
			catch(SQLException ex)
			{
				// Msg 12052, Level 17, State 1:
				// Server 'GORAN_UB2_DS', Line 1 (script row 862), Status 0, TranState 1:
				// Collection of monitoring data for table 'monSysPlanText' requires that the 'plan text pipe max messages', 'plan text pipe active' configuration option(s) be enabled. To set the necessary configuration, contact a user who has the System Administrator (SA) role.
				if (ex.getErrorCode() == 12052)
					_samplePlan = false;
				
				_logger.error("SQL Capture problems when capturing 'SQL Plan Text' Caught "+AseConnectionUtils.sqlExceptionToString(ex) + " when executing SQL: "+_sql_sqlPlanText);
			}
		}
		
		//------------------------------------------------
		// Post processing
		//------------------------------------------------
//System.out.println("CREATE 'current' deffered entry...");
//System.out.println("statementPkAdded     = "+statementPkAdded.size()); 
//System.out.println("statementPkDiscarded = "+statementPkDiscarded.size()); 
//System.out.println("statementRecords     = "+statementRecords.size()); 
//System.out.println("sqlTextPkMap         = "+sqlTextPkMap.size()); 
//System.out.println("sqlTextRecords       = "+sqlTextRecords.size()); 
//System.out.println("planTextPkMap        = "+planTextPkMap.size()); 
//System.out.println("planTextRecords      = "+planTextRecords.size()); 

//System.out.format("SAMPLE: statementPkAdded = %-5d, statementPkDiscarded = %-5d, statementRecords = %-5d, sqlTextPkMap = %-5d, sqlTextRecords = %-5d, planTextPkMap = %-5d, planTextRecords = %-5d \n",
//	statementPkAdded.size(),
//	statementPkDiscarded.size(),
//	statementRecords.size(),
//	sqlTextPkMap.size(),
//	sqlTextRecords.size(),
//	planTextPkMap.size(),
//	planTextRecords.size()); 

//for (List<Object> row : statementRecords)
//	System.out.println("ST---ROW: ------- "+row);
//
//for (List<Object> row : sqlTextRecords)
//	System.out.println("SQL--ROW: ------- "+row);
//
//for (List<Object> row : planTextRecords)
//	System.out.println("PLAN-ROW: ------- "+row);

		DeferredSqlAndPlanTextQueueEntry qe = new DeferredSqlAndPlanTextQueueEntry(
				statementPkAdded, statementPkDiscarded, statementRecords, //--->>// TODO: Why are we stuffing this in here???
				sqlTextPkMap, planTextPkMap);
//				sqlTextPkMap, sqlTextRecords,
//				planTextPkMap, planTextRecords);

		// Add info to the Deferred Queue, which we cleanup in toPcs() when we have passed threshold to do it...
		_deferredQueueFor_sqlTextAndPlan.add(qe);

		// Remove SQLText and PlanText that is not within filters (for example: execution time is lower than X)  
		doPostProcessing(qe);
		
		// Send counters for storage
		int count = toPcs(pch, qe);
		return count;
	}


	//--------------------------------------------------------------------------
	// BEGIN: Statement Statistics
	//--------------------------------------------------------------------------
	/**
	 * Update Statement Statistics, which is used by a CM to report exeution times within known spans. 
	 * <p>
	 * For Example: how many Statements that executed, between
	 * <ul>
	 *     <li> 0-10 ms       </li>
	 *     <li> 10-50 ms      </li>
	 *     <li> 50-100 ms     </li>
	 *     <li> 100-250 ms    </li>
	 *     <li> 250-500 ms    </li>
	 *     <li> 500-1000 ms   </li>
	 *     <li> 1000-2500 ms  </li>
	 *     <li> 2500-5000 ms  </li>
	 *     <li> 5-10 sec      </li>
	 *     <li> 10-15 sec     </li>
	 *     <li> 15-20 sec     </li>
	 *     <li> 20-30 sec     </li>
	 *     <li> above 30 sec  </li>
	 * </ul>
	 * 
	 * @param execTime
	 * @param logicalReads
	 * @param physicalReads
	 * @param waitTime 
	 * @param cpuTime 
	 * @param procName 
	 * @param lineNumber 
	 * @param contextID 
	 */
	private void updateStatementStats(int execTime, int logicalReads, int physicalReads, int cpuTime, int waitTime, int rowsAffected, int errorStatus, String procName, int lineNumber)
	{
		if (_statementStatistics == null)
			return;
		
		_statementStatistics.addStatementStats(execTime, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, procName, lineNumber);
	}

	public void closeStatementStats()
	{
		_statementStatistics = null;
	}

	public SqlCaptureStatementStatisticsSample getStatementStats(boolean reset)
	{
		// If we DO NOT have an object create one
		// This will happen only first time when the CM tries to retrive data.
		if (_statementStatistics == null)
		{
			_statementStatistics = new SqlCaptureStatementStatisticsSample();
			return _statementStatistics;
		}
		
		if ( ! reset)
		{
			return _statementStatistics;
		}
		else
		{
			// If we already have an object
			// - create a new object
			// - the old/current statistics object will be delivered to the CM
			// NOTE: do we need a critical section for this
			SqlCaptureStatementStatisticsSample newStmntStatObj = new SqlCaptureStatementStatisticsSample();
			SqlCaptureStatementStatisticsSample retStmntStatObj = _statementStatistics;
			
			_statementStatistics = newStmntStatObj;
			return retStmntStatObj;
		}
	}

	/** This will be NULL untill we call getStatementStats() for the first time. */
	private SqlCaptureStatementStatisticsSample _statementStatistics = null;

	//--------------------------------------------------------------------------
	// END: Statement Statistics
	//--------------------------------------------------------------------------



	
	//--------------------------------------------------------------------------
	// BEGIN: SQL Text Statistics
	//--------------------------------------------------------------------------
	private void updateSqlTextStats(int sPID, int kPID, int batchID, int sequenceInBatch, String sqlText)
	{
		if (_sqlTextStatistics == null)
			return;
		
		_sqlTextStatistics.addSqlTextStats(sPID, kPID, batchID, sequenceInBatch, sqlText);
	}

	public void closeSqlTextStats()
	{
		_sqlTextStatistics = null;
	}

	public SqlCaptureSqlTextStatisticsSample getSqlTextStats(boolean reset)
	{
		// If we DO NOT have an object create one
		// This will happen only first time when the CM tries to retrive data.
		if (_sqlTextStatistics == null)
		{
			_sqlTextStatistics = new SqlCaptureSqlTextStatisticsSample();
			return _sqlTextStatistics;
		}
		
		if ( ! reset)
		{
			return _sqlTextStatistics;
		}
		else
		{
			// If we already have an object
			// - create a new object
			// - the old/current statistics object will be delivered to the CM
			// NOTE: do we need a critical section for this
			SqlCaptureSqlTextStatisticsSample newSqlTextStatObj = new SqlCaptureSqlTextStatisticsSample();
			SqlCaptureSqlTextStatisticsSample retSqlTextStatObj = _sqlTextStatistics;
			
			_sqlTextStatistics = newSqlTextStatObj;
			return retSqlTextStatObj;
		}
	}

	/** This will be NULL untill we call getSqlTextStats() for the first time. */
	private SqlCaptureSqlTextStatisticsSample _sqlTextStatistics = null;

	//--------------------------------------------------------------------------
	// END: SQL Text Statistics
	//--------------------------------------------------------------------------



	/**
	 * 
	 */
	protected static class DeferredSqlAndPlanTextQueueEntry
	{
		long                  _crTime;
		Set<PK>               _statementPkAdded;
		Set<PK>               _statementPkDiscarded;
		List<List<Object>>    _statementRecords;
		Map<PK, List<Object>> _sqlTextPkMap;
//		List<List<Object>>    _sqlTextRecords;
		Map<PK, List<Object>> _planTextPkMap;
//		List<List<Object>>    _planTextRecords;
		
//		public DeferredSqlAndPlanTextQueueEntry(Set<PK> statementPkAdded, Set<PK> statementPkDiscarded, List<List<Object>> statementRecords, Map<PK, List<Object>> sqlTextPkMap, List<List<Object>> sqlTextRecords, Map<PK, List<Object>> planTextPkMap, List<List<Object>> planTextRecords)
		public DeferredSqlAndPlanTextQueueEntry(Set<PK> statementPkAdded, Set<PK> statementPkDiscarded, List<List<Object>> statementRecords, Map<PK, List<Object>> sqlTextPkMap, Map<PK, List<Object>> planTextPkMap)
		{
			_crTime               = System.currentTimeMillis();
			
			_statementPkAdded     = statementPkAdded;
			_statementPkDiscarded = statementPkDiscarded;
			_statementRecords     = statementRecords;
			_sqlTextPkMap         = sqlTextPkMap;
//			_sqlTextRecords       = sqlTextRecords;
			_planTextPkMap        = planTextPkMap;
//			_planTextRecords      = planTextRecords;
		}

		/** Remove any PK entries in the "defered queue" */
		public void removeStatementEntry(PK pk)
		{
			// Statements
			_statementPkAdded    .remove(pk);
			_statementPkDiscarded.remove(pk);
			
			// But how do we remove '_statementRecords' ???
			for (Iterator<List<Object>> iterator = _statementRecords.iterator(); iterator.hasNext();) 
			{
				List<Object> record = iterator.next();

				int SPID    = (Integer) record.get(_stmnt_SPID_pos);
				int KPID    = (Integer) record.get(_stmnt_KPID_pos);
				int BatchID = (Integer) record.get(_stmnt_BatchID_pos);
				
				if (pk.equals(SPID, KPID, BatchID))
			    	iterator.remove();
			}
		}

		/** Remove any PK entries in the "defered queue" */
		public void removeSqlEntry(PK pk)
		{
			List<Object> list = _sqlTextPkMap.get(pk);
			if (list != null)
			{
				_sqlTextPkMap.remove(pk);
//				_sqlTextRecords.remove(list);
			}
		}

		/** Remove any PK entries in the "defered queue" */
		public void removePlanEntry(PK pk)
		{
			List<Object> list = _planTextPkMap.get(pk);
			if (list != null)
			{
				_planTextPkMap.remove(pk);
//				_planTextRecords.remove(list);
			}
		}
		
		public boolean isEmpty()
		{
//System.out.println("isEmpty(): _statementPkAdded    .size() == " + _statementPkAdded    .size());
//System.out.println("isEmpty(): _statementPkDiscarded.size() == " + _statementPkDiscarded.size());
//System.out.println("isEmpty(): _statementRecords    .size() == " + _statementRecords    .size());
//System.out.println("isEmpty(): _sqlTextPkMap        .size() == " + _sqlTextPkMap        .size());
//System.out.println("isEmpty(): _sqlTextRecords      .size() == " + _sqlTextRecords      .size());
//System.out.println("isEmpty(): _planTextPkMap       .size() == " + _planTextPkMap       .size());
//System.out.println("isEmpty(): _planTextRecords     .size() == " + _planTextRecords     .size());

//			return     _statementPkAdded    .size() == 0
//				    && _statementPkDiscarded.size() == 0
//				    && _statementRecords    .size() == 0
//
//				    && _sqlTextPkMap        .size() == 0
//				    && _sqlTextRecords      .size() == 0
//
//				    && _planTextPkMap       .size() == 0
//				    && _planTextRecords     .size() == 0
//				    ;
			return true
					&& _sqlTextPkMap        .size() == 0
//					&& _sqlTextRecords      .size() == 0

					&& _planTextPkMap       .size() == 0
//					&& _planTextRecords     .size() == 0
					;
		}
	}

	/**
	 * Keep "old" SQL Text/Plan in a deferred queue so we can remove/retrive them later<br>
	 * The idea is to hold SQL Text/Plan for a while (over some spans/iterations over doSqlCapture())
	 * so we dont insert unneccecary SQL text/plans to PCS.
	 * When a "threshold" of X seconds has passed we will pass them to the PCS<br>
	 * But hopefully before that they can be discarded (due to that we find a Statement that has ended and 
	 * was filtered out due to a low executionTime or logical/physical IO
	 */
	protected static class DeferredSqlAndPlanTextQueue
	implements Iterable<DeferredSqlAndPlanTextQueueEntry>
	{
		protected Queue<DeferredSqlAndPlanTextQueueEntry> _queue = new LinkedList<>();

		public void add(DeferredSqlAndPlanTextQueueEntry qe)
		{
			_queue.add(qe);
		}

		public int size()
		{
			return _queue.size();
		}

		@Override
		public Iterator<DeferredSqlAndPlanTextQueueEntry> iterator()
		{
			return _queue.iterator();
		}

		/** Get SQL Text ENTRY stored in any of the "defered queue" entries */
		public List<Object> getSqlEntry(PK pk)
		{
			for (DeferredSqlAndPlanTextQueueEntry entry : _queue)
			{
				List<Object> sqlTextRecord = entry._sqlTextPkMap.get(pk);
				if (sqlTextRecord != null)
				{
					return sqlTextRecord;
				}
			}
			return null;
		}

		/** Get SQL Text stored in any of the "defered queue" entries */
		public String getSqlText(PK pk)
		{
			for (DeferredSqlAndPlanTextQueueEntry entry : _queue)
			{
				List<Object> sqlTextRecord = entry._sqlTextPkMap.get(pk);
				if (sqlTextRecord != null)
				{
					String sqlText = (String) sqlTextRecord.get(sqlTextRecord.size()-1); // SQLText = Last record
					return sqlText;
				}
			}
			return null;
		}

		/** Get Plan Text ENTRY stored in any of the "defered queue" entries */
		public List<Object> getPlanEntry(PK pk)
		{
			for (DeferredSqlAndPlanTextQueueEntry entry : _queue)
			{
				List<Object> planTextRecord = entry._planTextPkMap.get(pk);
				if (planTextRecord != null)
				{
					return planTextRecord;
				}
			}
			return null;
		}

		/** Get Plan Text stored in any of the "defered queue" entries */
		public String getPlanText(PK pk)
		{
			for (DeferredSqlAndPlanTextQueueEntry entry : _queue)
			{
				List<Object> planTextRecord = entry._planTextPkMap.get(pk);
				if (planTextRecord != null)
				{
					String planText = (String) planTextRecord.get(planTextRecord.size()-1); // PlanText = Last record
					return planText;
				}
			}
			return null;
		}

		/** Remove any PK entries in the "defered queue" */
		public void remove(PK pk)
		{
			removeStatementEntry(pk);
			removeSqlEntry(pk);
			removePlanEntry(pk);
		}

		/** Remove any PK entries in the "defered queue" */
		public void removeStatementEntry(PK pk)
		{
			for (DeferredSqlAndPlanTextQueueEntry entry : _queue)
				entry.removeStatementEntry(pk);
		}

		/** Remove any PK entries in the "defered queue" */
		public void removeSqlEntry(PK pk)
		{
			for (DeferredSqlAndPlanTextQueueEntry entry : _queue)
				entry.removeSqlEntry(pk);
		}

		/** Remove any PK entries in the "defered queue" */
		public void removePlanEntry(PK pk)
		{
			for (DeferredSqlAndPlanTextQueueEntry entry : _queue)
				entry.removePlanEntry(pk);
		}
		
		public void removeEmptyQueuEntries()
		{
			for (Iterator<DeferredSqlAndPlanTextQueueEntry> iterator = _queue.iterator(); iterator.hasNext();) 
			{
				DeferredSqlAndPlanTextQueueEntry entry = iterator.next();

				if ( entry.isEmpty() )
					iterator.remove();
			}
		}
	}

	/** 
	 * Remove all SqlText/PlanText records that should be filtered out due to little ExecTime or LogicalReads/PhysicalReads <br>
	 * The remove is done on the DEFERRED Queue (SQL Text/Plans that arrived before execution of statement was finnished)
	 * <p>
	 * Also remove some Static SQL/Plan text from the current sample
	 * 
	 * @param currentEntry
	 */
	protected void doPostProcessing(DeferredSqlAndPlanTextQueueEntry currentEntry)
	{
		// Remove all SqlText/PlanText records that should be filtered out due to little ExecTime or LogicalReads/PhysicalReads
		// The remove is done on the DEFERRED Queue (SQL Text/Plans that arrived before execution of statement was finnished)
		for (PK pk : currentEntry._statementPkDiscarded)
		{
			// If a statement has been added to the DISCARD set and also added to the ADD set, then it's probably a Stored Proc with both keep/skip set
			// And if any statement in the proc is in the "add" set, then DO NOT REMOVE IT
			if (currentEntry._statementPkAdded.contains(pk))
				continue;

			// Remove discarded entry from OLDER SQL Text/Plans in the deferred queue
//			_deferredQueueFor_sqlTextAndPlan.remove(pk);
			_deferredQueueFor_sqlTextAndPlan.removeSqlEntry(pk);
			_deferredQueueFor_sqlTextAndPlan.removePlanEntry(pk);
		}
		
		// Remove static SQL Text's (from current entry) that ASE inserted without have adding a Statement record (so they couldn't be identified and discarded by the Statement filter)
		// This is done in 2 steps
		//  1: get PK for SQL texts that we should delete
		//  2: Delete the records for SQL-Text and Plan-Text
		if (_removeStaticSqlText)
		{
			Set<PK> removeSet = new HashSet<>();
			for (PK pk : currentEntry._sqlTextPkMap.keySet())
			{
				// If a statement has been added to the KEEP set (The statement existed, and NOT filtered out) then DO NOT REMOVE IT
				if (currentEntry._statementPkAdded.contains(pk))
					continue;

				// Get SQL Text and check if we should keep it
				List<Object> sqlTextRecord = currentEntry._sqlTextPkMap.get(pk);
				if (sqlTextRecord != null)
				{
					String sqlText = (String) sqlTextRecord.get(sqlTextRecord.size()-1); // SQLText = Last record

					if (isDiscardSqlText(sqlText))
						removeSet.add(pk);
				}
			}
			if ( ! removeSet.isEmpty() )
			{
				for (PK pk : removeSet)
				{
					// Remove the SQL Text & Plan record associated with the PK
					currentEntry.removeSqlEntry(pk);
					currentEntry.removePlanEntry(pk);
				}

				// Write info messages when we remove SQL (every hour or so)
				if (_removeStaticSqlText_stat_printLastTime == -1 || TimeUtils.msDiffNow(_removeStaticSqlText_stat_printLastTime) > _removeStaticSqlText_stat_printThreshold)
				{
					int numOfRemovals = 0;
					for (Integer num : _removeStaticSqlText_stat.values())
						numOfRemovals += num;
					
					_logger.info("Information about ASE SQL Capture; Static SQL-Text removals. " 
							+ "For the last '"+TimeUtils.msToTimeStr("%HH:%MM", _removeStaticSqlText_stat_printThreshold)+"' (HH:MM), "
							+ "We have removed "+numOfRemovals+" SQL Text entries, for "+_removeStaticSqlText_stat.size()+" SQL slots. "
							+ "Here is the SQL Text and remove-counters: "+_removeStaticSqlText_stat);

					// TODO: Maybe add this information the the PCS ???
					
					// Reset the values, so we can print new message in X minutes/hours
					_removeStaticSqlText_stat_printLastTime = System.currentTimeMillis();
					_removeStaticSqlText_stat = new HashMap<>();
				}
			} // end: doRemove
		} // end: if (_removeStaticSqlText)
	} // end: method
	
	
	/** Before sending SQL for normalization, we might want to remove some stuff */
	public static String removeKnownPrefixes(String sqlText)
	{
		if (StringUtil.isNullOrBlank(sqlText))
			return "";

		// Remove some known "prefixes"
		if (sqlText.matches("^DYNAMIC_SQL .*: .*"))
		{
			int firstColon = sqlText.indexOf(": ");
			if (firstColon >= 0)
				sqlText = sqlText.substring(firstColon+2);
		}

		return sqlText;
	}

	/** What SQL Text should we NOT send to PCS */
	private boolean isDiscardSqlText(String sqlText)
	{
		if (sqlText == null)
			return false;

		boolean discard = false;
		String statKey = sqlText;

		// FIXME: we should probably have a configurage list of what to delete...

		//---------------------------------------
		// various 'create proc' (that has to do with DYNAMIC_SQL)
		//---------------------------------------
		if (!discard && sqlText.startsWith("create proc FetchMetaData as select * from "))
		{
			discard = true;
			statKey = "create proc FetchMetaData as select * from ...";
		}
		
		if (!discard && sqlText.startsWith("create proc dyn"))
		{
			discard = true;
			statKey = "create proc dyn######: ";
		}

		if (!discard && sqlText.startsWith("create proc "))
		{
			discard = true;
			statKey = "create proc ...";
		}

		//---------------------------------------
		// various 'DYNAMIC_SQL'
		//---------------------------------------
		if (!discard && sqlText.startsWith("DYNAMIC_SQL FetchMetaData:"))
		{
			discard = true;
//			statKey = sqlText;
			statKey = "DYNAMIC_SQL FetchMetaData:";
		}
		
		if (!discard && sqlText.startsWith("DYNAMIC_SQL dyn"))
		{
			discard = true;
			statKey = "DYNAMIC_SQL dyn######: ";
		}

		if (!discard && sqlText.startsWith("DYNAMIC_SQL "))
		{
			discard = true;
			statKey = "DYNAMIC_SQL ...: ";
		}

		//---------------------------------------
		// various 'set fmtonly'
		//---------------------------------------
		if (!discard && (sqlText.startsWith("set fmtonly ") || sqlText.startsWith("SET FMTONLY "))) // example: "set string_rtruncation on" 
		{
			discard = true;
			statKey = "set fmtonly ...";
		}
		
		//---------------------------------------
		// various 'set ...'
		//---------------------------------------
		if (!discard && (sqlText.startsWith("set ") || sqlText.startsWith("SET ")) && sqlText.length() < 40) // example: "set string_rtruncation on" 
		{
			discard = true;
			statKey = sqlText;
		}
		
		//---------------------------------------
		// various 'use dbname'
		//---------------------------------------
		if (!discard && (sqlText.startsWith("use ") || sqlText.startsWith("USE "))) // example: "use tempdb" 
		{
			discard = true;
			statKey = "use ...";
		}
		
		//---------------------------------------
		// select getdate()
		//---------------------------------------
		if (!discard && (sqlText.equalsIgnoreCase("SELECT getdate()"))) // example: "select getdate()" 
		{
			discard = true;
			statKey = "use ...";
		}
		
		// increment statistics
		if (discard)
		{
			Integer cnt = _removeStaticSqlText_stat.get(statKey);
			if (cnt == null)
				cnt = 0;
			_removeStaticSqlText_stat.put(statKey, cnt + 1);
		}

		return discard;
	}

	/**
	 * Send records to the PCS
	 * <p>
	 * When statements in the current/last sample indicates that it's finnished and NOT filtered out...<br>
	 * Get the SQL Text/Plan from the deferred queue
	 * <p>
	 * The Deferred Queue should only hold SQL Text/Plans for X seconds.<br>
	 * After that time it's SENT to the PCS (and not discarded)<br>
	 * (the idea here is that it's better to send to much SQL Text/Plans to PCS than caching everything, which will mean OutOfMemory.
	 *  the deferred queue should be thought of as a strategy to -do-not-send-shorter-statements-text/plan-to-the-PCS-)
	 * 
	 * 
	 * @param pch
	 * @param currentEntry
	 * @return
	 */
	protected int toPcs(PersistentCounterHandler pch, DeferredSqlAndPlanTextQueueEntry currentEntry)
	{
		// Container object used to send data to PCS
		SqlCaptureDetails capDet = new SqlCaptureDetails();

		// default values for: 
		//    - storeNormalizedSqlTextHash = true
		//    - storeNormalizedSqlText     = false  --->>> instead of storing it, in the offline view, we can read the 'SQLText' and normalize it "on the fly", which hopefully saves us some MB at-the-end-of-the-day... the storeNormalizedSqlTextHash is still stored so we can aggregate stuff via group by... but that just takes an INT value
		boolean storeNormalizedSqlTextHash = getConfiguration().getBooleanProperty(PROPKEY_sqlCap_ase_sqlTextAndPlan_storeNormalizedSqlTextHash, DEFAULT_sqlCap_ase_sqlTextAndPlan_storeNormalizedSqlTextHash);
		boolean storeNormalizedSqlText     = getConfiguration().getBooleanProperty(PROPKEY_sqlCap_ase_sqlTextAndPlan_storeNormalizedSqlText,     DEFAULT_sqlCap_ase_sqlTextAndPlan_storeNormalizedSqlText);
		
		// Used to anonymize or remove-constants in where clauses etc...
		StatementNormalizer stmntNorm = new StatementNormalizer();
		
		// STATEMENTS (from the passed Entry, which is the current sample we just did)
		if (currentEntry._statementRecords != null)
		{
			for (List<Object> record : currentEntry._statementRecords) 
			{
				int SPID    = (Integer) record.get(_stmnt_SPID_pos);
				int KPID    = (Integer) record.get(_stmnt_KPID_pos);
				int BatchID = (Integer) record.get(_stmnt_BatchID_pos);

				PK pk = new PK(SPID, KPID, BatchID);
				
				// set JavaSqlHashCode in the statement record...
				try
				{
					// Set 'JavaSqlHashCode' fields, if SQLText exists...
					// NOTE: or maybe we can do this in the database:::: Maybe the following SQL could work:  update MonSqlCapStatements set JavaSqlHashCode = (select JavaSqlHashCode from MonSqlCapSqlText where SPID = ### and KPID = ### and BatchID = ###) where SPID = ### and KPID = ### and BatchID = ### 
					String sqlText = _deferredQueueFor_sqlTextAndPlan.getSqlText(pk);
					if (sqlText != null)
					{
						// Set 'JavaSqlHashCode' fields
						int s = record.size();
						record.set(s-1, sqlText.hashCode()); // JavaSqlHashCode  = Last record
						
						// Set 'NormJavaSqlHashCode' field
						if (storeNormalizedSqlTextHash)
						{
							String normalizedSql = stmntNorm.normalizeStatementNoThrow(removeKnownPrefixes(sqlText));
							record.add(normalizedSql.hashCode()); // append to end
						}
						else
							record.add(0);
					}
					else
						record.add(0);
				}
				catch (Throwable t)
				{
					_logger.error("Problem when setting 'JavaSqlHashCode' in statementRecords, skipping this and continuing. Caught: "+t);
				}

				//-----------------------------------------------
				// Send Statement
				capDet.add(record); 

				//-----------------------------------------------
				// Send SQL TEXT (kept in the deferred queue)
				List<Object> sqlTextEntry  = _deferredQueueFor_sqlTextAndPlan.getSqlEntry(pk);
				if (sqlTextEntry != null)
				{
					_deferredQueueFor_sqlTextAndPlan.removeSqlEntry(pk);

					// Set AddMethod, JavaSqlLength and JavaSqlHashCode fields
					int s = sqlTextEntry.size();
					String sqlText = (String) sqlTextEntry.get(s-1); // SQLText = Last record

					int addMethod = 1; // 1=Direct
					sqlTextEntry.set(s-4, addMethod);          // AddMethod       = 3 recods before SQLText
					sqlTextEntry.set(s-3, sqlText.length());   // JavaSqlLength   = 2 recods before SQLText
					sqlTextEntry.set(s-2, sqlText.hashCode()); // JavaSqlHashCode = 1 recods before SQLText

					// Add 'NormJavaSqlHashCode', 'NormSQLText' field
					if (storeNormalizedSqlTextHash)
					{
						String normalizedSql = stmntNorm.normalizeStatementNoThrow(removeKnownPrefixes(sqlText));
						sqlTextEntry.add(normalizedSql.hashCode()); // append to end
						sqlTextEntry.add(storeNormalizedSqlText ? normalizedSql : ""); // append to end
					}
					else
					{
						sqlTextEntry.add(0); // append to end
						sqlTextEntry.add(""); // append to end
					}

					capDet.add(sqlTextEntry);
				}

				// Send PLAN TEXT (kept in the deferred queue)
				List<Object> planTextEntry = _deferredQueueFor_sqlTextAndPlan.getPlanEntry(pk);
				if (planTextEntry != null)
				{
					_deferredQueueFor_sqlTextAndPlan.removePlanEntry(pk);

					// Set AddMethod
					int s = planTextEntry.size();
					int addMethod = 1; // 1=Direct
					planTextEntry.set(s-2, addMethod);  // AddMethod       = 1 recods before PlanText

					capDet.add(planTextEntry); 
				}
			}
		}
		_deferredQueueFor_sqlTextAndPlan.removeEmptyQueuEntries();

		// Send older SQL Text/Plans to PCS 
		// (text/plans that has been in deferred cache for X second, hopefully long running sql that will complete "later")
//		Iterator<DeferredSqlAndPlanTextQueueEntry> iter = _deferredQueueFor_sqlTextAndPlan.iterator();
//		while (iter.hasNext())
//		{
//			DeferredSqlAndPlanTextQueueEntry entry = iter.next();
//			
//			// SEND records that are OLDER than the configured threshold
//			// Hopefully it's a _long_running_statement_ that we need the SQL text for later on.
//			// BUT: if we are unlucky... then it's just "crap" that we should have filtered out earlier
//			if (TimeUtils.msDiffNow(entry._crTime) > _deferredStorageThresholdFor_sqlTextAndPlan*1000)
//			{
//				List<List<Object>> sqlTextRecords   = entry._sqlTextRecords;
//				List<List<Object>> planTextRecords  = entry._planTextRecords;
//
//				if (sqlTextRecords != null)
//				{
//					for (List<Object> record : sqlTextRecords)
//					{
//						int s = record.size();
//						String sqlText = (String) record.get(s-1); // SQLText = Last record
//
//						// This shouldn't have to be done here... it's already done in doPostProcessing() but only on last/current SQL-text 
//						// But apperently (when testing in a *real* production env) it "slips" thrue... and I can't figgure out why...
//						if (_removeStaticSqlText)
//						{
//							if (isDiscardSqlText(sqlText))
//								continue;
//						}
//
//						// Set JavaSqlLength and JavaSqlHashCode fields
//						int addMethod = 2; // 2=Deferred
//						record.set(s-4, addMethod);          // AddMethod       = 3 recods before SQLText
//						record.set(s-3, sqlText.length());   // JavaSqlLength   = 2 recods before SQLText
//						record.set(s-2, sqlText.hashCode()); // JavaSqlHashCode = 1 recods before SQLText
//						
//						capDet.add(record); 
////						System.out.println("OLD-SQL: "+record);
//					}
//				}
//				
//				if (planTextRecords != null)
//				{
//					for (List<Object> record : planTextRecords)  
//					{ 
//						// Set AddMethod
//						int s = record.size();
//						int addMethod = 2; // 2=Deferred
//						record.set(s-2, addMethod);         // AddMethod       = 1 recods before PlanText
//
//						capDet.add(record); 
//					}
//				}
//
//				// remove the entry from the queue
//				iter.remove();
//			}
//		}
		Iterator<DeferredSqlAndPlanTextQueueEntry> iter = _deferredQueueFor_sqlTextAndPlan.iterator();
		while (iter.hasNext())
		{
			DeferredSqlAndPlanTextQueueEntry entry = iter.next();
			
			// SEND records that are OLDER than the configured threshold
			// Hopefully it's a _long_running_statement_ that we need the SQL text for later on.
			// BUT: if we are unlucky... then it's just "crap" that we should have filtered out earlier
			if (TimeUtils.msDiffNow(entry._crTime) > _deferredStorageThresholdFor_sqlTextAndPlan*1000)
			{
				Map<PK, List<Object>> sqlTextPkMap   = entry._sqlTextPkMap;
				Map<PK, List<Object>> planTextPkMap  = entry._planTextPkMap;

				if (sqlTextPkMap != null)
				{
					for (List<Object> record : sqlTextPkMap.values())
					{
						int s = record.size();
						String sqlText = (String) record.get(s-1); // SQLText = Last record

						// This shouldn't have to be done here... it's already done in doPostProcessing() but only on last/current SQL-text 
						// But apperently (when testing in a *real* production env) it "slips" thrue... and I can't figgure out why...
						if (_removeStaticSqlText)
						{
							if (isDiscardSqlText(sqlText))
								continue;
						}

						// Set JavaSqlLength and JavaSqlHashCode fields
						int addMethod = 2; // 2=Deferred
						record.set(s-4, addMethod);          // AddMethod       = 3 recods before SQLText
						record.set(s-3, sqlText.length());   // JavaSqlLength   = 2 recods before SQLText
						record.set(s-2, sqlText.hashCode()); // JavaSqlHashCode = 1 recods before SQLText
						
						// Add 'NormJavaSqlHashCode', 'NormSQLText' field
						if (storeNormalizedSqlTextHash)
						{
							String normalizedSql = stmntNorm.normalizeStatementNoThrow(removeKnownPrefixes(sqlText));
							record.add(normalizedSql.hashCode()); // append to end
							record.add(storeNormalizedSqlText ? normalizedSql : ""); // append to end
						}
						else
						{
							record.add(0); // append to end
							record.add(""); // append to end
						}

						capDet.add(record); 
//						System.out.println("OLD-SQL: "+record);
					}
				}
				
				if (planTextPkMap != null)
				{
					for (List<Object> record : planTextPkMap.values())  
					{ 
						// Set AddMethod
						int s = record.size();
						int addMethod = 2; // 2=Deferred
						record.set(s-2, addMethod);         // AddMethod       = 1 recods before PlanText

						capDet.add(record); 
					}
				}

				// remove the entry from the queue
				iter.remove();
			}
		}


		if (_logger.isDebugEnabled())
		{
			for (List<Object> record : capDet.getList())
				_logger.debug("  +++ "+record);
		}
		
//		if (true)
//		{
//			int stmntCount = 0;
//			int textCount = 0;
//			int planCount = 0;
//			
//			for (List<Object> record : capDet.getList())
//			{
//				String type = (String) record.get(0);
//				if      (MON_SQL_STATEMENT.equals(type)) stmntCount++; 
//				else if (MON_SQL_TEXT     .equals(type)) textCount++; 
//				else if (MON_SQL_PLAN     .equals(type)) planCount++; 
//			}
//			System.out.format("TO_PCS:   statementCount = %-5d,                                 statementCount = %-5d,                            textCount = %-5d,                              planCount = %-5d \n", stmntCount, stmntCount, textCount, planCount);
//
//			for (List<Object> record : capDet.getList())
//				System.out.println("----->>>> TO_PCS -record-: "+record);
//		}
		
		// Post the information to the PersistentCounterHandler, which will save the information to it's writers...
		// Do this in another method, because it would possibly make testing easier... /*pch.addSqlCapture(capDet);*/
		addToPcs(pch, capDet);
		
		return capDet.size();
	}
	
	/** 
	 * Post it to PCS, in it's own method to makle testing easier...<br>
	 * Meaning we can subclass and override this method when testing...
	 */
	protected void addToPcs(PersistentCounterHandler pch, SqlCaptureDetails sqlCaptureDetails)
	{
		pch.addSqlCapture(sqlCaptureDetails);
	}

	
//	private void addToContainer(List<Object> row, PersistentCounterHandler pch)
//	{
//		_sqlCaptureDetails.add(row);
//		
//		// When the "send size" is reached, then send and create a new Container.
//		if (_sqlCaptureDetails.size() >= _sendSizeThreshold)
//		{
//			pch.addSqlCapture(_sqlCaptureDetails);
//			_sqlCaptureDetails = new SqlCaptureDetails();
//		}
//	}
//	private void addToContainerFinal(PersistentCounterHandler pch)
//	{
//		pch.addSqlCapture(_sqlCaptureDetails);
//		_sqlCaptureDetails = new SqlCaptureDetails();
//	}
//
//	@Override
//	public int doSqlCapture(DbxConnection conn, PersistentCounterHandler pch)
//	{
//		if (_firstPoll && _clearBeforeFirstPoll)
//		{
//			// If first time... discard everything in the transient monSysStatement table
//			if (_sampleSqlText)    clearTable(conn, "monSysSQLText");
//			if (_sampleStatements) clearTable(conn, "monSysStatement");
//			if (_samplePlan)       clearTable(conn, "monSysPlanText");
//
//			_firstPoll = false;
//		}
//
//
//		int addCount = 0;
//
//
//		// If SQL queries are NOT initialized, do it now
//		if (_sql_sqlText == null)
//			setSql(conn);
//
//		//------------------------------------------------
//		// SQL TEXT
//		//------------------------------------------------
//		if (_sampleSqlText)
//		{
//			try
//			{
//				//----------------------------------------------------------------------------------------
//				// This might look a bit odd / backwards, but look at the data example at the end of this file
//				// SQLText can have several rows
//				// * so when FIRST row (for every group) is discovered - Start a new "row" (which is a List: tableNameToStoreItIn, c1-data, c2-data, c3-data... SQLText-concatenated-data)
//				// * on all "extra" rows that will just contain same c1-data, c2-data, c3-data but the SQLText will be read and appended to a StringBuilder
//				// * when a "new" row is found, we will set the StringBuffer content to replace the "first row" SQLText
//				// * At the END we will need to "post" the information from the last "group"
//				//----------------------------------------------------------------------------------------
//				Statement stmnt = conn.createStatement();
//				ResultSet rs = stmnt.executeQuery(_sql_sqlText);
//				ResultSetMetaData rsmd = rs.getMetaData();
//				int           colCount = rsmd.getColumnCount();
//				int           sequenceCol_pos = -1;
//				int           sequenceCol_val = -1;
//				int           seqTextCol_pos  = -1;
//				StringBuilder seqTextCol_val  = new StringBuilder();
//				for (int c=1; c<=colCount; c++)
//				{
//					if      ("SequenceInBatch".equals(rsmd.getColumnLabel(c))) sequenceCol_pos = c;
//					else if ("SQLText"        .equals(rsmd.getColumnLabel(c))) seqTextCol_pos  = c;
//				}
//				List<Object> row = null;
//				while(rs.next())
//				{
//					sequenceCol_val = rs.getInt(sequenceCol_pos);
//					if (sequenceCol_val == 1)
//					{
//						if (row != null)
//						{
//							row.set(row.size()-1, seqTextCol_val.toString());
//							seqTextCol_val.setLength(0);
//
//							addCount++;
//							addToContainer(row, pch);
//						}
//
//						row = new ArrayList<Object>();
//						row.add(MON_SQL_TEXT); // NOTE: the first object in the list should be the TableName where to store the data
//						for (int c=1; c<=colCount; c++)
//						{
//							// the sequence column should NOT be part of the result
//							if (c != sequenceCol_pos)
//								row.add(rs.getObject(c));
//						}
//						seqTextCol_val.append( rs.getString(seqTextCol_pos) );
//					}
//					else if (row != null)
//					{
//						seqTextCol_val.append( rs.getString(seqTextCol_pos) );
//					}
//				}
//				// Finally add content from LAST row, since we do addCountainer() only on NEW sequenceCol_val == 1
//				if (row != null)
//				{
//					row.set(row.size()-1, seqTextCol_val.toString());
//					seqTextCol_val.setLength(0);
//
//					addCount++;
//					addToContainer(row, pch);
//				}
//				
//				rs.close();
//				stmnt.close();
//			}
//			catch(SQLException ex)
//			{
//				// Not configured
//				if (ex.getErrorCode() == 12052)
//					_sampleSqlText = false;
//				
//				_logger.error("SQL Capture problems when capturing 'SQL Text' Caught "+AseConnectionUtils.sqlExceptionToString(ex) + " when executing SQL: "+_sql_sqlText);
//			}
//		}
//
//		//------------------------------------------------
//		// SQL STATEMENTS
//		//------------------------------------------------
//		if (_sampleStatements)
//		{
//			// Get some configuration
//			int     saveStatement_gt_execTime         = getIntProperty    (PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_execTime,         PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_execTime);
//			int     saveStatement_gt_logicalReads     = getIntProperty    (PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_logicalReads,     PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_logicalReads);
//			int     saveStatement_gt_physicalReads    = getIntProperty    (PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_physicalReads,    PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_physicalReads);
//
//			boolean sendDdlForLookup                  = getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup,                  PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup);
//			int     sendDdlForLookup_gt_execTime      = getIntProperty    (PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_execTime,      PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_execTime);
//			int     sendDdlForLookup_gt_logicalReads  = getIntProperty    (PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_logicalReads,  PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_logicalReads);
//			int     sendDdlForLookup_gt_physicalReads = getIntProperty    (PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_physicalReads, PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_physicalReads);
//
//			try
//			{
//				Statement stmnt = conn.createStatement();
//				ResultSet rs = stmnt.executeQuery(_sql_sqlStatements);
//				int colCount = rs.getMetaData().getColumnCount();
//				while(rs.next())
//				{
//					int    execTime      = rs.getInt   ("Elapsed_ms");
//					int    logicalReads  = rs.getInt   ("LogicalReads");
//					int    physicalReads = rs.getInt   ("PhysicalReads");
//
//					// Add only rows that are above the limits
//					if (    execTime      > saveStatement_gt_execTime
//					     && logicalReads  > saveStatement_gt_logicalReads
//					     && physicalReads > saveStatement_gt_physicalReads
//					   )
//					{
//						if (sendDdlForLookup)
//						{
//							String ProcName = rs.getString("ProcName");
//
//							// send ProcName or StatementCacheEntry to the DDL Capture
//							if (StringUtil.hasValue(ProcName))
//							{
//								String DBName        = rs.getString("DBName");
////								int    execTime      = rs.getInt   ("Elapsed_ms");
////								int    logicalReads  = rs.getInt   ("LogicalReads");
////								int    physicalReads = rs.getInt   ("PhysicalReads");
//
//								// Only send if it's above the defined limits
//								if (    execTime      > sendDdlForLookup_gt_execTime
//								     && logicalReads  > sendDdlForLookup_gt_logicalReads
//								     && physicalReads > sendDdlForLookup_gt_physicalReads
//								   )
//								{
//	    							// if it's a statement cache entry, populate it into the cache so that the DDL Lookup wont have to sleep 
//	    							if (ProcName.startsWith("*ss") || ProcName.startsWith("*sq") ) // *sq in ASE 15.7 esd#2, DynamicSQL can/will end up in statement cache
//	    							{
//	    								if (XmlPlanCache.hasInstance())
//	    								{
//	        								XmlPlanCache xmlPlanCache = XmlPlanCache.getInstance();
//	        								if ( ! xmlPlanCache.isPlanCached(ProcName) )
//	        								{
//	        									xmlPlanCache.getPlan(ProcName);
//	        								}
//	    								}
//	    								else
//	    								{
//	    									_logger.info("XmlPlanCache do not have an instance. Skipping XML Plan lookup for name '"+ProcName+"'.");
//	    								}
//	    							}
//	    
//	    							// Now add the ProcName/StatementCacheEntryName to the DDL Lookup handler
//	    							pch.addDdl(DBName, ProcName, "SqlCapture");
//								}
//							}
//						}
//
//						List<Object> row = new ArrayList<Object>();
//						row.add(MON_SQL_STATEMENT); // NOTE: the first object in the list should be the TableName where to store the data
//						for (int c=1; c<=colCount; c++)
//							row.add(rs.getObject(c));
//
//						addCount++;
//						addToContainer(row, pch);
//					}
//				}
//				rs.close();
//				stmnt.close();
//			}
//			catch(SQLException ex)
//			{
//				// Not configured
//				if (ex.getErrorCode() == 12052)
//					_sampleStatements = false;
//				
//				_logger.error("SQL Capture problems when capturing 'SQL Statements' Caught "+AseConnectionUtils.sqlExceptionToString(ex) + " when executing SQL: "+_sql_sqlStatements);
//			}
//		}
//
//		//------------------------------------------------
//		// SQL PLANS
//		//------------------------------------------------
//		if (_samplePlan)
//		{
//			try
//			{
//				//----------------------------------------------------------------------------------------
//				// This might look a bit odd / backwards, but look at the data example at the end of this file
//				// PlanText can have several rows
//				// * so when FIRST row (for every group) is discovered - Start a new "row" (which is a List: tableNameToStoreItIn, c1-data, c2-data, c3-data... PlanText-concatenated-data)
//				// * on all "extra" rows that will just contain same c1-data, c2-data, c3-data but the PlanText will be read and appended to a StringBuilder
//				// * when a "new" row is found, we will set the StringBuffer content to replace the "first row" PlanText
//				// * At the END we will need to "post" the information from the last "group"
//				//----------------------------------------------------------------------------------------
//				Statement stmnt = conn.createStatement();
//				ResultSet rs = stmnt.executeQuery(_sql_sqlPlanText);
//				ResultSetMetaData rsmd = rs.getMetaData();
//				int           colCount = rsmd.getColumnCount();
//				int           sequenceCol_pos = -1;
//				int           sequenceCol     = -1;
//				int           seqTextCol_pos  = -1;
//				StringBuilder seqTextCol_val  = new StringBuilder();
//				for (int c=1; c<=colCount; c++)
//				{
//					if      ("SequenceNumber".equals(rsmd.getColumnLabel(c))) sequenceCol_pos = c;
//					else if ("PlanText"      .equals(rsmd.getColumnLabel(c))) seqTextCol_pos  = c;
//				}
//				List<Object> row = null;
//				while(rs.next())
//				{
//					sequenceCol = rs.getInt(sequenceCol_pos);
//					if (sequenceCol == 1)
//					{
//						if (row != null)
//						{
//							row.set(row.size()-1, seqTextCol_val.toString());
//							seqTextCol_val.setLength(0);
//
//							addCount++;
//							addToContainer(row, pch);
//						}
//
//						row = new ArrayList<Object>();
//						row.add(MON_SQL_PLAN); // NOTE: the first object in the list should be the TableName where to store the data
//						for (int c=1; c<=colCount; c++)
//						{
//							// the sequence column should NOT be part of the result
//							if (c != sequenceCol_pos)
//								row.add(rs.getObject(c));
//						}
//						seqTextCol_val.append( rs.getString(seqTextCol_pos) );
//					}
//					else if (row != null)
//					{
//						seqTextCol_val.append( rs.getString(seqTextCol_pos) );
//					}
//				}
//				// Finally add content from LAST row, since we do addCountainer() only on NEW sequenceCol_val == 1
//				if (row != null)
//				{
//					row.set(row.size()-1, seqTextCol_val.toString());
//					seqTextCol_val.setLength(0);
//
//					addCount++;
//					addToContainer(row, pch);
//				}
//
//				rs.close();
//				stmnt.close();
//			}
//			catch(SQLException ex)
//			{
//				// Msg 12052, Level 17, State 1:
//				// Server 'GORAN_UB2_DS', Line 1 (script row 862), Status 0, TranState 1:
//				// Collection of monitoring data for table 'monSysPlanText' requires that the 'plan text pipe max messages', 'plan text pipe active' configuration option(s) be enabled. To set the necessary configuration, contact a user who has the System Administrator (SA) role.
//				if (ex.getErrorCode() == 12052)
//					_samplePlan = false;
//				
//				_logger.error("SQL Capture problems when capturing 'SQL Plan Text' Caught "+AseConnectionUtils.sqlExceptionToString(ex) + " when executing SQL: "+_sql_sqlPlanText);
//			}
//		}
//
//		addToContainerFinal(pch);
//		return addCount;
//	}
}

/*------------------------------------------------------------------------
** Below is an example of statements executed from SQL Window
** It has a bunch of "internal" JDBC/jConnect calls to sp_mda
** The RPC (ct_dynamic? and other?) calls is not visible in 'monSysSQLText' nor in 'monSysPlanText'
**------------------------------------------------------------------------
** Note:
** - monSysSQLText and monSysPlanText is posted "at once" when it's executed
** - monSysStatement is posted AFTER the statement is finished
** so for example: 'waitfor delay "00:10:00"'
** - The monSysSQLText and monSysPlanText is visible "at once"
** - The monSysStatement is visible after 10 minutes (and other Statements from other SPID's will be visible prior to that)
**------------------------------------------------------------------------

RS> Col# Label           JDBC Type Name         Guessed DBMS type Source Table            
RS> ---- --------------- ---------------------- ----------------- ------------------------
RS> 1    SPID            java.sql.Types.INTEGER int               master.dbo.monSysSQLText
RS> 2    InstanceID      java.sql.Types.TINYINT tinyint           master.dbo.monSysSQLText
RS> 3    KPID            java.sql.Types.INTEGER int               master.dbo.monSysSQLText
RS> 4    ServerUserID    java.sql.Types.INTEGER int               master.dbo.monSysSQLText
RS> 5    BatchID         java.sql.Types.INTEGER int               master.dbo.monSysSQLText
RS> 6    SequenceInBatch java.sql.Types.INTEGER int               master.dbo.monSysSQLText
RS> 7    SQLText         java.sql.Types.VARCHAR varchar(255)      master.dbo.monSysSQLText
+----+----------+--------+------------+-------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|SPID|InstanceID|KPID    |ServerUserID|BatchID|SequenceInBatch|SQLText                                                                                                                                                                                                            |
+----+----------+--------+------------+-------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|15  |0         |43712528|1           |582    |1              |select dbname=db_name(), spid=@@spid, username = user_name(), susername =suser_name(), trancount=@@trancount, tranchained=@@tranchained, transtate=@@transtate                                                     |
+----+----------+--------+------------+-------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|15  |0         |43712528|1           |583    |1              |select dbname=db_name(dbid), table_name=object_name(id, dbid), lock_type=type, lock_count=count(*)  from master.dbo.syslocks  where spid = @@spid         group by dbid, id, type                                  |
+----+----------+--------+------------+-------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|15  |0         |43712528|1           |585    |1              |select @@tranchained                                                                                                                                                                                               |
+----+----------+--------+------------+-------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|15  |0         |43712528|1           |587    |1              |select @@tranchained                                                                                                                                                                                               |
+----+----------+--------+------------+-------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|15  |0         |43712528|1           |588    |1              |select                                                                                                                                                                                                             |
|    |          |        |            |       |               |getdate(), '11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111',|
|    |          |        |            |       |               |getdate(), '22222222222222222222222                                                                                                                                                                                |
+----+----------+--------+------------+-------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|15  |0         |43712528|1           |588    |2              |222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222',                                   |
|    |          |        |            |       |               |getdate(), '333333333333333333333333333333333333333333333333333333333333333333                                                                                                                                     |
+----+----------+--------+------------+-------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|15  |0         |43712528|1           |588    |3              |33333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333',                                                                              |
|    |          |        |            |       |               |getdate(), '4444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444                                                                                          |
+----+----------+--------+------------+-------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|15  |0         |43712528|1           |588    |4              |4444444444444444444444444444444444444444444444444444444444444444444444444444444444444444',                                                                                                                         |
|    |          |        |            |       |               |'end'                                                                                                                                                                                                              |
+----+----------+--------+------------+-------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|15  |0         |43712528|1           |591    |1              |select dbname=db_name(), spid=@@spid, username = user_name(), susername =suser_name(), trancount=@@trancount, tranchained=@@tranchained, transtate=@@transtate                                                     |
+----+----------+--------+------------+-------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|15  |0         |43712528|1           |592    |1              |select dbname=db_name(dbid), table_name=object_name(id, dbid), lock_type=type, lock_count=count(*)  from master.dbo.syslocks  where spid = @@spid         group by dbid, id, type                                  |
+----+----------+--------+------------+-------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|15  |0         |43712528|1           |594    |1              |select @@tranchained                                                                                                                                                                                               |
+----+----------+--------+------------+-------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|15  |0         |43712528|1           |596    |1              |select @@tranchained                                                                                                                                                                                               |
+----+----------+--------+------------+-------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|15  |0         |43712528|1           |597    |1              |select * from dbo.monSysSQLText                                                                                                                                                                                    |
|    |          |        |            |       |               |--select * from dbo.monSysStatement                                                                                                                                                                                |
|    |          |        |            |       |               |select ProcName = CASE WHEN SsqlId > 0 THEN object_name(SsqlId,2) ELSE isnull(object_name(ProcedureID,DBID), object_name(ProcedureID,db_id('sybsystemprocs'))) END, * from dbo.monSysStatem                        |
+----+----------+--------+------------+-------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|15  |0         |43712528|1           |597    |2              |ent                                                                                                                                                                                                                |
|    |          |        |            |       |               |select * from dbo.monSysPlanText                                                                                                                                                                                   |
+----+----------+--------+------------+-------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
Rows 14


RS> Col# Label             JDBC Type Name           Guessed DBMS type Source Table              
RS> ---- ----------------- ------------------------ ----------------- --------------------------
RS> 1    ProcName          java.sql.Types.VARCHAR   varchar(255)      -none-                    
RS> 2    SPID              java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 3    InstanceID        java.sql.Types.TINYINT   tinyint           master.dbo.monSysStatement
RS> 4    KPID              java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 5    DBID              java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 6    ProcedureID       java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 7    PlanID            java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 8    BatchID           java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 9    ContextID         java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 10   LineNumber        java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 11   CpuTime           java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 12   WaitTime          java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 13   MemUsageKB        java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 14   PhysicalReads     java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 15   LogicalReads      java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 16   PagesModified     java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 17   PacketsSent       java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 18   PacketsReceived   java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 19   NetworkPacketSize java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 20   PlansAltered      java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 21   RowsAffected      java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 22   ErrorStatus       java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 23   HashKey           java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 24   SsqlId            java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 25   ProcNestLevel     java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 26   StatementNumber   java.sql.Types.INTEGER   int               master.dbo.monSysStatement
RS> 27   DBName            java.sql.Types.VARCHAR   varchar(30)       master.dbo.monSysStatement
RS> 28   StartTime         java.sql.Types.TIMESTAMP datetime          master.dbo.monSysStatement
RS> 29   EndTime           java.sql.Types.TIMESTAMP datetime          master.dbo.monSysStatement
+---------------------------+----+----------+--------+----+-----------+------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+----------+----------+-------------+---------------+------+-----------------------+-----------------------+
|ProcName                   |SPID|InstanceID|KPID    |DBID|ProcedureID|PlanID|BatchID|ContextID|LineNumber|CpuTime|WaitTime|MemUsageKB|PhysicalReads|LogicalReads|PagesModified|PacketsSent|PacketsReceived|NetworkPacketSize|PlansAltered|RowsAffected|ErrorStatus|HashKey   |SsqlId    |ProcNestLevel|StatementNumber|DBName|StartTime              |EndTime                |
+---------------------------+----+----------+--------+----+-----------+------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+----------+----------+-------------+---------------+------+-----------------------+-----------------------+
|*ss0087948680_1345721111ss*|15  |0         |43712528|1   |87948680   |388573|579    |2        |3         |5      |0       |16        |0            |0           |0            |6          |0              |2048             |0           |75          |0          |1345721111|87948680  |1            |3              |master|2016-06-15 00:47:30.526|2016-06-15 00:47:30.53 |
|*ss2027431587_1146561368ss*|15  |0         |43712528|1   |2027431587 |388538|579    |3        |3         |0      |0       |16        |0            |0           |0            |1          |0              |2048             |0           |28          |0          |1146561368|2027431587|1            |5              |master|2016-06-15 00:47:30.53 |2016-06-15 00:47:30.53 |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |0         |0      |0       |12        |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |0              |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |21        |0      |0       |10        |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |1              |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |22        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |2              |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |23        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |3              |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |26        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |4              |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |28        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |5              |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |30        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |6              |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |31        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |7              |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |37        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |8              |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |41        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |9              |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |49        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |10             |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |58        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |11             |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |71        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |12             |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |79        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |13             |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |80        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |14             |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |86        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |15             |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |88        |0      |0       |14        |0            |8           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |16             |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |137       |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |17             |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |141       |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |18             |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |145       |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |19             |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|580    |1        |0         |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |20             |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_jdbc_getcatalogs        |15  |0         |43712528|1   |1522101432 |388530|581    |1        |7         |0      |0       |12        |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |0              |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_jdbc_getcatalogs        |15  |0         |43712528|1   |1522101432 |388530|581    |1        |9         |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |1              |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_jdbc_getcatalogs        |15  |0         |43712528|1   |1522101432 |388530|581    |1        |12        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |2              |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_jdbc_getcatalogs        |15  |0         |43712528|1   |1522101432 |388530|581    |1        |17        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |3              |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|sp_jdbc_getcatalogs        |15  |0         |43712528|1   |1522101432 |388530|581    |1        |19        |0      |0       |14        |0            |1           |0            |0          |0              |2048             |0           |9           |0          |0         |0         |1            |4              |master|2016-06-15 00:47:30.553|2016-06-15 00:47:30.553|
|(NULL)                     |15  |0         |43712528|1   |0          |0     |582    |0        |1         |0      |0       |34        |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |0            |0              |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|*ss1979431416_0427372835ss*|15  |0         |43712528|1   |1979431416 |388532|583    |1        |1         |0      |0       |26        |0            |9           |0            |0          |0              |2048             |0           |0           |0          |427372835 |1979431416|1            |1              |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |0         |0      |0       |12        |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |0              |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |21        |0      |0       |10        |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |1              |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |22        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |2              |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |23        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |3              |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |26        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |4              |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |28        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |5              |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |30        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |6              |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |31        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |7              |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |37        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |8              |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |41        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |9              |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |49        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |10             |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |58        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |11             |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |71        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |12             |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |79        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |13             |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |80        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |14             |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |86        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |15             |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |88        |0      |0       |14        |0            |8           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |16             |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |137       |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |17             |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |141       |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |18             |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |145       |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |19             |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|584    |1        |0         |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |20             |master|2016-06-15 00:47:30.556|2016-06-15 00:47:30.556|
|(NULL)                     |15  |0         |43712528|1   |0          |0     |585    |0        |1         |0      |0       |30        |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |0            |0              |master|2016-06-15 00:47:30.56 |2016-06-15 00:47:30.56 |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |0         |0      |0       |16        |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |0              |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |21        |0      |0       |10        |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |1              |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |22        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |2              |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |23        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |3              |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |26        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |4              |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |28        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |5              |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |30        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |6              |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |31        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |7              |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |37        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |8              |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |41        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |9              |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |49        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |10             |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |58        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |11             |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |71        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |12             |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |79        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |13             |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |80        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |14             |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |86        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |15             |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |88        |0      |0       |14        |0            |8           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |16             |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |137       |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |17             |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |141       |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |18             |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |145       |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |19             |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|586    |1        |0         |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |20             |master|2016-06-15 00:47:33.4  |2016-06-15 00:47:33.4  |
|(NULL)                     |15  |0         |43712528|1   |0          |0     |587    |0        |1         |0      |0       |30        |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |0            |0              |master|2016-06-15 00:47:33.506|2016-06-15 00:47:33.506|
|(NULL)                     |15  |0         |43712528|1   |0          |0     |588    |0        |1         |0      |0       |58        |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |0            |0              |master|2016-06-15 00:47:33.51 |2016-06-15 00:47:33.51 |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |0         |0      |0       |28        |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |0              |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |21        |0      |0       |10        |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |1              |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |22        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |2              |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |23        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |3              |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |26        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |4              |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |28        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |5              |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |30        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |6              |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |31        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |7              |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |37        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |8              |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |41        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |9              |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |49        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |10             |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |58        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |11             |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |71        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |12             |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |79        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |13             |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |80        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |14             |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |86        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |15             |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |88        |0      |0       |14        |0            |8           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |16             |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |137       |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |17             |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |141       |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |18             |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |145       |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |19             |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|589    |1        |0         |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |20             |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_jdbc_getcatalogs        |15  |0         |43712528|1   |1522101432 |388530|590    |1        |7         |0      |0       |12        |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |0              |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_jdbc_getcatalogs        |15  |0         |43712528|1   |1522101432 |388530|590    |1        |9         |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |1              |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_jdbc_getcatalogs        |15  |0         |43712528|1   |1522101432 |388530|590    |1        |12        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |2              |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_jdbc_getcatalogs        |15  |0         |43712528|1   |1522101432 |388530|590    |1        |17        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |3              |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|sp_jdbc_getcatalogs        |15  |0         |43712528|1   |1522101432 |388530|590    |1        |19        |0      |0       |14        |0            |1           |0            |0          |0              |2048             |0           |9           |0          |0         |0         |1            |4              |master|2016-06-15 00:47:33.536|2016-06-15 00:47:33.536|
|(NULL)                     |15  |0         |43712528|1   |0          |0     |591    |0        |1         |0      |0       |34        |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |0            |0              |master|2016-06-15 00:47:33.54 |2016-06-15 00:47:33.54 |
|*ss1979431416_0427372835ss*|15  |0         |43712528|1   |1979431416 |388532|592    |1        |1         |0      |0       |26        |0            |9           |0            |0          |0              |2048             |0           |0           |0          |427372835 |1979431416|1            |1              |master|2016-06-15 00:47:33.54 |2016-06-15 00:47:33.54 |
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |0         |0      |0       |12        |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |0              |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |21        |0      |0       |10        |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |1              |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |22        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |2              |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |23        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |3              |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |26        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |4              |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |28        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |5              |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |30        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |6              |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |31        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |7              |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |37        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |8              |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |41        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |9              |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |49        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |10             |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |58        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |11             |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |71        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |12             |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |79        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |13             |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |80        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |14             |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |86        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |15             |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |88        |0      |0       |14        |0            |8           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |16             |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |137       |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |17             |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |141       |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |18             |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |145       |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |19             |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|593    |1        |0         |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |20             |master|2016-06-15 00:47:33.543|2016-06-15 00:47:33.543|
|(NULL)                     |15  |0         |43712528|1   |0          |0     |594    |0        |1         |0      |0       |30        |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |0            |0              |master|2016-06-15 00:47:33.546|2016-06-15 00:47:33.546|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |0         |0      |0       |16        |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |0              |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |21        |0      |0       |10        |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |1              |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |22        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |2              |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |23        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |3              |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |26        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |4              |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |28        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |5              |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |30        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |6              |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |31        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |7              |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |37        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |8              |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |41        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |9              |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |49        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |10             |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |58        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |11             |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |71        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |12             |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |79        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |13             |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |80        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |14             |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |86        |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |15             |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |88        |0      |0       |14        |0            |8           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |1            |16             |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |137       |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |17             |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |141       |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |18             |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |145       |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |19             |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|sp_mda                     |15  |0         |43712528|1   |1250100463 |388502|595    |1        |0         |0      |0       |0         |0            |0           |0            |0          |0              |2048             |0           |0           |0          |0         |0         |1            |20             |master|2016-06-15 00:47:36.983|2016-06-15 00:47:36.983|
|(NULL)                     |15  |0         |43712528|1   |0          |0     |596    |0        |1         |0      |0       |30        |0            |0           |0            |0          |0              |2048             |0           |1           |0          |0         |0         |0            |0              |master|2016-06-15 00:47:37.07 |2016-06-15 00:47:37.07 |
|*ss2043431644_0963144205ss*|15  |0         |43712528|1   |2043431644 |388540|597    |1        |1         |0      |0       |38        |0            |0           |0            |1          |0              |2048             |0           |14          |0          |963144205 |2043431644|1            |1              |master|2016-06-15 00:47:37.073|2016-06-15 00:47:37.073|
+---------------------------+----+----------+--------+----+-----------+------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+----------+----------+-------------+---------------+------+-----------------------+-----------------------+
Rows 148


RS> Col# Label          JDBC Type Name         Guessed DBMS type Source Table             
RS> ---- -------------- ---------------------- ----------------- -------------------------
RS> 1    PlanID         java.sql.Types.INTEGER int               master.dbo.monSysPlanText
RS> 2    InstanceID     java.sql.Types.TINYINT tinyint           master.dbo.monSysPlanText
RS> 3    SPID           java.sql.Types.INTEGER int               master.dbo.monSysPlanText
RS> 4    KPID           java.sql.Types.INTEGER int               master.dbo.monSysPlanText
RS> 5    BatchID        java.sql.Types.INTEGER int               master.dbo.monSysPlanText
RS> 6    ContextID      java.sql.Types.INTEGER int               master.dbo.monSysPlanText
RS> 7    SequenceNumber java.sql.Types.INTEGER int               master.dbo.monSysPlanText
RS> 8    DBID           java.sql.Types.INTEGER int               master.dbo.monSysPlanText
RS> 9    ProcedureID    java.sql.Types.INTEGER int               master.dbo.monSysPlanText
RS> 10   DBName         java.sql.Types.VARCHAR varchar(30)       master.dbo.monSysPlanText
RS> 11   PlanText       java.sql.Types.VARCHAR varchar(160)      master.dbo.monSysPlanText
+------+----------+----+--------+-------+---------+--------------+----+-----------+------+-----------------------------------------------------------------------+
|PlanID|InstanceID|SPID|KPID    |BatchID|ContextID|SequenceNumber|DBID|ProcedureID|DBName|PlanText                                                               |
+------+----------+----+--------+-------+---------+--------------+----+-----------+------+-----------------------------------------------------------------------+
|0     |0         |15  |43712528|582    |0        |1             |1   |0          |master|QUERY PLAN FOR STATEMENT 1 (at line 1).                                |
|0     |0         |15  |43712528|582    |0        |2             |1   |0          |master|Optimized using Serial Mode                                            |
|0     |0         |15  |43712528|582    |0        |3             |1   |0          |master|    STEP 1                                                             |
|0     |0         |15  |43712528|582    |0        |4             |1   |0          |master|        The type of query is SELECT.                                   |
|0     |0         |15  |43712528|583    |0        |1             |1   |0          |master|QUERY PLAN FOR STATEMENT 1 (at line 1).                                |
|0     |0         |15  |43712528|583    |0        |2             |1   |0          |master|    STEP 1                                                             |
|0     |0         |15  |43712528|583    |0        |3             |1   |0          |master|        The type of query is EXECUTE.                                  |
|0     |0         |15  |43712528|583    |0        |4             |1   |0          |master|        Executing a previously cached statement (SSQL_ID = 1979431416).|
|0     |0         |15  |43712528|585    |0        |1             |1   |0          |master|QUERY PLAN FOR STATEMENT 1 (at line 1).                                |
|0     |0         |15  |43712528|585    |0        |2             |1   |0          |master|Optimized using Serial Mode                                            |
|0     |0         |15  |43712528|585    |0        |3             |1   |0          |master|    STEP 1                                                             |
|0     |0         |15  |43712528|585    |0        |4             |1   |0          |master|        The type of query is SELECT.                                   |
|0     |0         |15  |43712528|587    |0        |1             |1   |0          |master|QUERY PLAN FOR STATEMENT 1 (at line 1).                                |
|0     |0         |15  |43712528|587    |0        |2             |1   |0          |master|Optimized using Serial Mode                                            |
|0     |0         |15  |43712528|587    |0        |3             |1   |0          |master|    STEP 1                                                             |
|0     |0         |15  |43712528|587    |0        |4             |1   |0          |master|        The type of query is SELECT.                                   |
|0     |0         |15  |43712528|588    |0        |1             |1   |0          |master|QUERY PLAN FOR STATEMENT 1 (at line 1).                                |
|0     |0         |15  |43712528|588    |0        |2             |1   |0          |master|Optimized using Serial Mode                                            |
|0     |0         |15  |43712528|588    |0        |3             |1   |0          |master|    STEP 1                                                             |
|0     |0         |15  |43712528|588    |0        |4             |1   |0          |master|        The type of query is SELECT.                                   |
|0     |0         |15  |43712528|591    |0        |1             |1   |0          |master|QUERY PLAN FOR STATEMENT 1 (at line 1).                                |
|0     |0         |15  |43712528|591    |0        |2             |1   |0          |master|Optimized using Serial Mode                                            |
|0     |0         |15  |43712528|591    |0        |3             |1   |0          |master|    STEP 1                                                             |
|0     |0         |15  |43712528|591    |0        |4             |1   |0          |master|        The type of query is SELECT.                                   |
|0     |0         |15  |43712528|592    |0        |1             |1   |0          |master|QUERY PLAN FOR STATEMENT 1 (at line 1).                                |
|0     |0         |15  |43712528|592    |0        |2             |1   |0          |master|    STEP 1                                                             |
|0     |0         |15  |43712528|592    |0        |3             |1   |0          |master|        The type of query is EXECUTE.                                  |
|0     |0         |15  |43712528|592    |0        |4             |1   |0          |master|        Executing a previously cached statement (SSQL_ID = 1979431416).|
|0     |0         |15  |43712528|594    |0        |1             |1   |0          |master|QUERY PLAN FOR STATEMENT 1 (at line 1).                                |
|0     |0         |15  |43712528|594    |0        |2             |1   |0          |master|Optimized using Serial Mode                                            |
|0     |0         |15  |43712528|594    |0        |3             |1   |0          |master|    STEP 1                                                             |
|0     |0         |15  |43712528|594    |0        |4             |1   |0          |master|        The type of query is SELECT.                                   |
|0     |0         |15  |43712528|596    |0        |1             |1   |0          |master|QUERY PLAN FOR STATEMENT 1 (at line 1).                                |
|0     |0         |15  |43712528|596    |0        |2             |1   |0          |master|Optimized using Serial Mode                                            |
|0     |0         |15  |43712528|596    |0        |3             |1   |0          |master|    STEP 1                                                             |
|0     |0         |15  |43712528|596    |0        |4             |1   |0          |master|        The type of query is SELECT.                                   |
|0     |0         |15  |43712528|597    |0        |1             |1   |0          |master|QUERY PLAN FOR STATEMENT 1 (at line 1).                                |
|0     |0         |15  |43712528|597    |0        |2             |1   |0          |master|    STEP 1                                                             |
|0     |0         |15  |43712528|597    |0        |3             |1   |0          |master|        The type of query is EXECUTE.                                  |
|0     |0         |15  |43712528|597    |0        |4             |1   |0          |master|        Executing a previously cached statement (SSQL_ID = 2043431644).|
|0     |0         |15  |43712528|597    |0        |5             |1   |0          |master|QUERY PLAN FOR STATEMENT 2 (at line 3).                                |
|0     |0         |15  |43712528|597    |0        |6             |1   |0          |master|    STEP 1                                                             |
|0     |0         |15  |43712528|597    |0        |7             |1   |0          |master|        The type of query is EXECUTE.                                  |
|0     |0         |15  |43712528|597    |0        |8             |1   |0          |master|        Executing a previously cached statement (SSQL_ID = 87948680).  |
|0     |0         |15  |43712528|597    |0        |9             |1   |0          |master|QUERY PLAN FOR STATEMENT 3 (at line 4).                                |
|0     |0         |15  |43712528|597    |0        |10            |1   |0          |master|    STEP 1                                                             |
|0     |0         |15  |43712528|597    |0        |11            |1   |0          |master|        The type of query is EXECUTE.                                  |
|0     |0         |15  |43712528|597    |0        |12            |1   |0          |master|        Executing a previously cached statement (SSQL_ID = 2027431587).|
+------+----------+----+--------+-------+---------+--------------+----+-----------+------+-----------------------------------------------------------------------+
Rows 48
*/

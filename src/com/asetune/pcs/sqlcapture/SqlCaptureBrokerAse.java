/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.asetune.DbxTune;
import com.asetune.Version;
import com.asetune.cache.XmlPlanCache;
import com.asetune.config.dict.AseErrorMessageDictionary;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.pcs.DictCompression;
import com.asetune.pcs.PersistWriterBase;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.sql.conn.AseConnection;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.norm.NormalizerCompiler;
import com.asetune.sql.norm.StatementFixerManager;
import com.asetune.sql.norm.StatementNormalizer;
import com.asetune.sql.norm.UserDefinedNormalizerManager;
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
	
//	protected static final String MON_SQL_TEXT      = PersistWriterBase.getTableName(null, PersistWriterBase.SQL_CAPTURE_SQLTEXT,    null, false); // "MonSqlCapSqlText";
	protected static final String MON_SQL_STATEMENT = PersistWriterBase.getTableName(null, PersistWriterBase.SQL_CAPTURE_STATEMENTS, null, false); // "MonSqlCapStatements";
//	protected static final String MON_SQL_PLAN      = PersistWriterBase.getTableName(null, PersistWriterBase.SQL_CAPTURE_PLANS,      null, false); // "MonSqlCapPlans";
	protected static final String MON_CAP_SPID_INFO = "MonSqlCapSpidInfo";//PersistWriterBase.getTableName(null, PersistWriterBase.SQL_CAPTURE_SPID_INFO,  null, false); // "MonSqlCapSpidInfo";
	protected static final String MON_CAP_WAIT_INFO = "MonSqlCapWaitInfo";//PersistWriterBase.getTableName(null, PersistWriterBase.SQL_CAPTURE_WAIT_INFO,  null, false); // "MonSqlCapWaitInfo";

//	private static final List<String> _storegeTableNames = Arrays.asList(MON_SQL_TEXT, MON_SQL_STATEMENT, MON_SQL_PLAN);
	private static final List<String> _storegeTableNames = Arrays.asList(MON_SQL_STATEMENT, MON_CAP_SPID_INFO, MON_CAP_WAIT_INFO);

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

	/** Keep SQLText & PlanText in memory for X seconds before flushing them to storage, so thay can be removed if the statement is not within the filter, for example execution time */
	protected int _deferredStorageThresholdFor_sqlTextAndPlan = DEFAULT_sqlCap_ase_sqlTextAndPlan_deferredQueueAge;

	/** used to determine if we should call removeUnusedSlots() */
	private long _lastCallTo_removeUnusedSlots = System.currentTimeMillis();
	private long _removeUnusedSlotsThresholdInSeconds = DEFAULT_sqlCap_ase_sqlTextAndPlan_removeUnusedSlotsThresholdInSeconds;

	
//	private boolean _removeStaticSqlText = DEFAULT_sqlCap_ase_sqlTextAndPlan_removeStaticSqlText;
//	private Map<String, Integer> _removeStaticSqlText_stat = new HashMap<>(); 
//	private long _removeStaticSqlText_stat_printThreshold = DEFAULT_sqlCap_ase_sqlTextAndPlan_removeStaticSqlText_printMsgTimeThreshold; 
//	private long _removeStaticSqlText_stat_printLastTime  = -1; // System.currentTimeMillis();

	private Map<Integer, String> _monSqlStatementDictCompColMap;

//	private static int _stmnt_SPID_pos    = 2 + 1; // 2 = ListPos + 1 is for that the row starts with a string that contains the DestinationTablename
//	private static int _stmnt_KPID_pos    = 3 + 1; // 3 = ListPos + 1 is for that the row starts with a string that contains the DestinationTablename
//	private static int _stmnt_BatchID_pos = 7 + 1; // 7 = ListPos + 1 is for that the row starts with a string that contains the DestinationTablename

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
//	public static final boolean DEFAULT_sqlCap_ase_sqlTextAndPlan_storeNormalizedSqlText     = false;
	public static final boolean DEFAULT_sqlCap_ase_sqlTextAndPlan_storeNormalizedSqlText     = true;

	public static final String  PROPKEY_sqlCap_ase_sqlTextAndPlan_storeNormalizedSqlTextHash = "PersistentCounterHandler.sqlCapture.ase.sqlTextAndPlan.storeNormalizedSqlTextHash";
	public static final boolean DEFAULT_sqlCap_ase_sqlTextAndPlan_storeNormalizedSqlTextHash = false;

	public static final String  PROPKEY_sqlCap_ase_sqlTextAndPlan_removeUnusedSlotsThresholdInSeconds = "PersistentCounterHandler.sqlCapture.ase.sqlTextAndPlan.removeUnusedSlotsThresholdInSeconds";
	public static final long    DEFAULT_sqlCap_ase_sqlTextAndPlan_removeUnusedSlotsThresholdInSeconds = 60 * 5;

	public static final String  PROPKEY_sqlCap_ase_statisticsReportEveryXSecond = "PersistentCounterHandler.sqlCapture.ase.statisticsReportEveryXSecond";
	public static final long    DEFAULT_sqlCap_ase_statisticsReportEveryXSecond = 60 * 10; // 10 minutes

	public static final String  PROPKEY_sqlCap_ase_sqlTextAndPlan_batchIdEntry_keepCount = "PersistentCounterHandler.sqlCapture.ase.batchIdEntry.keepCount";
//	public static final int     DEFAULT_sqlCap_ase_sqlTextAndPlan_batchIdEntry_keepCount = 2;
	public static final int     DEFAULT_sqlCap_ase_sqlTextAndPlan_batchIdEntry_keepCount = 10; // 10 should be WAY to much, it's just for testing...
	//NOTE: if we have a keep count higher than 1 or 2, then we should do LOW ON MEMORY to first remove all with a keep-count > 1 or similar...

	public static final String  PROPKEY_sqlCap_ase_sqlTextAndPlan_sqlText_shortLength = "PersistentCounterHandler.sqlCapture.ase.sqlText.shortLength";
	public static final int     DEFAULT_sqlCap_ase_sqlTextAndPlan_sqlText_shortLength = 256 * 3; // 768

	public static final String  PROPKEY_sqlCap_ase_sqlTextAndPlan_printPcsAdd = "PersistentCounterHandler.sqlCapture.ase.sqlTextAndPlan.printPcsAdd";
	public static final boolean DEFAULT_sqlCap_ase_sqlTextAndPlan_printPcsAdd = false;

	public static final String  PROPKEY_sqlCap_ase_spidInfo_sample     = "PersistentCounterHandler.sqlCapture.ase.spidInfo.sample";
	public static final boolean DEFAULT_sqlCap_ase_spidInfo_sample     = true;

	public static final String  PROPKEY_sqlCap_ase_spidWaitInfo_sample = "PersistentCounterHandler.sqlCapture.ase.spidWaitInfo.sample";
	public static final boolean DEFAULT_sqlCap_ase_spidWaitInfo_sample = true;


	public int _sqlText_shortLength = DEFAULT_sqlCap_ase_sqlTextAndPlan_sqlText_shortLength;

	// static so we can reach it from any private static sub classes
	private static int _default_batchIdEntry_keepCount = DEFAULT_sqlCap_ase_sqlTextAndPlan_batchIdEntry_keepCount;
	
	
	@Override
	public void init(Configuration conf)
	{
		super.init(conf);
		
		_clearBeforeFirstPoll                       = getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_clearBeforeFirstPoll,                PersistentCounterHandler.DEFAULT_sqlCap_clearBeforeFirstPoll);
//		_sendSizeThreshold                          = getIntProperty    (PersistentCounterHandler.PROPKEY_sqlCap_sendSizeThreshold,                   PersistentCounterHandler.DEFAULT_sqlCap_sendSizeThreshold);
		_isNonConfiguredMonitoringAllowed           = getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_isNonConfiguredMonitoringAllowed,    PersistentCounterHandler.DEFAULT_sqlCap_isNonConfiguredMonitoringAllowed);
		_deferredStorageThresholdFor_sqlTextAndPlan = getIntProperty    (PROPKEY_sqlCap_ase_sqlTextAndPlan_deferredQueueAge,                          DEFAULT_sqlCap_ase_sqlTextAndPlan_deferredQueueAge);
		_lastConfigOverflowMsgTimeThreshold         = getIntProperty    (PROPKEY_sqlCap_ase_sqlTextAndPlan_overflowMsgTimeThreshold,                  DEFAULT_sqlCap_ase_sqlTextAndPlan_overflowMsgTimeThreshold);
//		_removeStaticSqlText                        = getBooleanProperty(PROPKEY_sqlCap_ase_sqlTextAndPlan_removeStaticSqlText,                       DEFAULT_sqlCap_ase_sqlTextAndPlan_removeStaticSqlText);
//		_removeStaticSqlText_stat_printThreshold    = getIntProperty    (PROPKEY_sqlCap_ase_sqlTextAndPlan_removeStaticSqlText_printMsgTimeThreshold, DEFAULT_sqlCap_ase_sqlTextAndPlan_removeStaticSqlText_printMsgTimeThreshold);
		_removeUnusedSlotsThresholdInSeconds        = getLongProperty   (PROPKEY_sqlCap_ase_sqlTextAndPlan_removeUnusedSlotsThresholdInSeconds,       DEFAULT_sqlCap_ase_sqlTextAndPlan_removeUnusedSlotsThresholdInSeconds);

		_statReportAfterSec                         = getLongProperty   (PROPKEY_sqlCap_ase_statisticsReportEveryXSecond,                             DEFAULT_sqlCap_ase_statisticsReportEveryXSecond);
		_default_batchIdEntry_keepCount             = getIntProperty    (PROPKEY_sqlCap_ase_sqlTextAndPlan_batchIdEntry_keepCount,                    DEFAULT_sqlCap_ase_sqlTextAndPlan_batchIdEntry_keepCount);
		_sqlText_shortLength                        = getIntProperty    (PROPKEY_sqlCap_ase_sqlTextAndPlan_sqlText_shortLength,                       DEFAULT_sqlCap_ase_sqlTextAndPlan_sqlText_shortLength);

		boolean storeNormalizedSqlText              = getBooleanProperty(PROPKEY_sqlCap_ase_sqlTextAndPlan_storeNormalizedSqlText,     DEFAULT_sqlCap_ase_sqlTextAndPlan_storeNormalizedSqlText);

		// Just to write some start messages... and see ERRORS if we have User Defined "resolvers", in Java code that needs to be compiled!
		if (storeNormalizedSqlText)
		{
			NormalizerCompiler          .getInstance();
			UserDefinedNormalizerManager.getInstance();
			StatementFixerManager       .getInstance();
		}
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

	private String fill(String str, int fill)                                           { return PersistWriterBase.fill(str, fill); }
//	private String getDatatype(String type, int length, int prec, int scale)            { return PersistWriterBase.getDatatype(type, length, prec, scale); }
	private String getDatatype(DbxConnection conn, int jdbcType)                        { return PersistWriterBase.getDatatype(conn, jdbcType); }
	private String getDatatype(DbxConnection conn, int jdbcType, int length)            { return PersistWriterBase.getDatatype(conn, jdbcType, length); }
//	private String getDatatype(DbxConnection conn, int jdbcType, int length, int scale) { return PersistWriterBase.getDatatype(conn, jdbcType, length, scale); }
	private String getNullable(boolean nullable)                                        { return PersistWriterBase.getNullable(nullable); }

	@Override
	public boolean checkForDropTableDdl(DbxConnection conn, DatabaseMetaData dbmd, String tabName)
	{
		Set<String> colNames = null;;
		try
		{
			// Get column names
			colNames = DbUtils.getColumnNames(conn, null, tabName);

			// If no columns, then table DO NOT Exists... So no need to drop
			if (colNames.isEmpty())
				return false;
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems getting Column Names from table '" + tabName + "'.", ex);
		}
		
		if (colNames == null)
		{
			_logger.warn("Problems getting Column Names from table '" + tabName + "'. colNames == null");
			return false;
		}

		//-------------------------------------------------------------
		if (MON_SQL_STATEMENT.equals(tabName))
		{
			// If the table no NOT contain 'ServerLogin', then it's a OLD table layout... DROP THE TABLE
			// new layout is: ONE Table only that holds both Statement details AND SQLText & PlanText
			// It's much simpler to simply drop the table and create a new one...
			// NOTE: You will only loose X hours of data, since we create a new Recording Database Every 24 hour
			if ( ! colNames.contains("ServerLogin") )
				return true;
		}

		return false;
	}
	
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
			// Check The Dictionary Compressed Tables
			// -- If they do not exists they will be created
			// -- If they already exists, the in-memory-cache will be populated!
			// Yes I know this looks strange ... getTableDdlString actually create The DictionaryCompressed table(s)... but it might be rewritten "in a while"
			getTableDdlString(conn, dbmd, tabName);  

			// Now check for missing columns and add them
			//------------------------------------------------------
			
			// BEGIN: Special for 'BlockedBySqlText'
			String col_BlockedBySqlText_name     = "BlockedBySqlText";
			int    col_BlockedBySqlText_jdbcType = Types.VARCHAR;
			int    col_BlockedBySqlText_jdbcLen  = 65536;
			
			// When we want Dictionary Compression on 'SQLText' and 'NormSQLText'
			if ( DictCompression.isEnabled() )
			{
				DictCompression dcc = DictCompression.getInstance();
				
				//try
				//{
					// No need to do this here... it's done "somewhere else"
					//dcc.createTable(conn, null, MON_SQL_STATEMENT, col_BlockedBySqlText_name, col_BlockedBySqlText_jdbcType, col_BlockedBySqlText_jdbcLen, true);
					
					col_BlockedBySqlText_name     = dcc.getDigestSourceColumnName(col_BlockedBySqlText_name);
					col_BlockedBySqlText_jdbcType = dcc.getDigestJdbcType();
					col_BlockedBySqlText_jdbcLen  = dcc.getDigestLength();
				//}
				//catch (SQLException ex)
				//{
				//	_logger.error("Problems creating Dictionary Compressed table for '" + MON_SQL_STATEMENT + "' colName '" + col_BlockedBySqlText_name + "'.", ex);
				//}
			}
			if ( ! colNames.contains(col_BlockedBySqlText_name) ) list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+col_BlockedBySqlText_name+rq,40)+" "+fill(getDatatype(conn, col_BlockedBySqlText_jdbcType, col_BlockedBySqlText_jdbcLen),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col
			// END: Special for 'BlockedBySqlText'
			
			// Now check for missing columns and add them
			if ( ! colNames.contains("WaitTimeDetails"     ) ) list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"WaitTimeDetails"     +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 4000),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col
			if ( ! colNames.contains("BlockedBySpid"       ) ) list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"BlockedBySpid"       +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col
			if ( ! colNames.contains("BlockedByKpid"       ) ) list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"BlockedByKpid"       +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col
			if ( ! colNames.contains("BlockedByBatchId"    ) ) list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"BlockedByBatchId"    +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col
			if ( ! colNames.contains("BlockedByCommand"    ) ) list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"BlockedByCommand"    +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30  ),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col
			if ( ! colNames.contains("BlockedByApplication") ) list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"BlockedByApplication"+rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30  ),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col
			if ( ! colNames.contains("BlockedByTranId"     ) ) list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"BlockedByTranId"     +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 255 ),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col

			return list;			
		}

//		//-------------------------------------------------------------
//		if (MON_SQL_STATEMENT.equals(tabName))
//		{
//			if ( ! colNames.contains("NormJavaSqlHashCode") )
//				list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"NormJavaSqlHashCode"+rq,40)+" "+fill(getDatatype(conn, Types.INTEGER),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col
//
//			if ( ! colNames.contains("JavaSqlLengthShort") )
//				list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"JavaSqlLengthShort"+rq,40)+" "+fill(getDatatype(conn, Types.INTEGER),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col
//
//			if ( ! colNames.contains("JavaSqlHashCodeShort") )
//				list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"JavaSqlHashCodeShort"+rq,40)+" "+fill(getDatatype(conn, Types.INTEGER),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col
//
//			return list;
//		}

//		//-------------------------------------------------------------
//		if (MON_SQL_TEXT.equals(tabName))
//		{
//			if ( ! colNames.contains("AddMethod") )
//				list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"AddMethod"+rq,40)+" "+fill(getDatatype(conn, Types.INTEGER),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col
//
//			if ( ! colNames.contains("NormJavaSqlHashCode") )
//				list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"NormJavaSqlHashCode"+rq,40)+" "+fill(getDatatype(conn, Types.INTEGER),20)+" "+getNullable(true)+"\n");
//
//			if ( ! colNames.contains("JavaSqlHashCodeShort") )
//				list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"JavaSqlHashCodeShort"+rq,40)+" "+fill(getDatatype(conn, Types.INTEGER),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col
//
//			// SQLText / SQLText$dcc$
//			if ( DictCompression.isEnabled() )
//			{
//				DictCompression dcc = DictCompression.getInstance();
//				
//				if ( ! colNames.contains("SQLText"+DictCompression.DCC_MARKER) )
//					list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"SQLText"+DictCompression.DCC_MARKER+rq,40)+" "+fill(getDatatype(conn, dcc.getDigestJdbcType(), dcc.getDigestLength()),20)+" "+getNullable(true)+"\n");
//				
//				// create table (if not exists), otherwise populate the cache 
//				try {
//					dcc.createTable(conn, null, tabName, "SQLText", Types.VARCHAR, 65536, true);
//				} catch (SQLException ex) {
//					_logger.error("Problems creating/refreshing Dictionary Compression table for table '" + tabName + "' column 'SQLText'.", ex);
//				}
//			}
//
//			// NormSQLText / NormSQLText$dcc$
//			if ( DictCompression.isEnabled() )
//			{
//				DictCompression dcc = DictCompression.getInstance();
//				
//				if ( ! colNames.contains("NormSQLText"+DictCompression.DCC_MARKER) )
//					list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"NormSQLText"+DictCompression.DCC_MARKER+rq,40)+" "+fill(getDatatype(conn, dcc.getDigestJdbcType(), dcc.getDigestLength()),20)+" "+getNullable(true)+"\n");
//
//				// create table (if not exists), otherwise populate the cache 
//				try {
//					dcc.createTable(conn, null, tabName, "NormSQLText", Types.VARCHAR, 65536, true);
//				} catch (SQLException ex) {
//					_logger.error("Problems creating/refreshing Dictionary Compression table for table '" + tabName + "' column 'NormSQLText'.", ex);
//				}
//			}
//			else
//			{
//				if ( ! colNames.contains("NormSQLText") )
//					list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"NormSQLText"+rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 65536),20)+" "+getNullable(true)+"\n");
//			}
//
//			return list;
//		}
//		
//		//-------------------------------------------------------------
//		if (MON_SQL_PLAN.equals(tabName))
//		{
//			if ( ! colNames.contains("AddMethod") )
//				list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"AddMethod"+rq,40)+" "+fill(getDatatype(conn, Types.INTEGER),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col
//
//			return list;
//		}

		//-------------------------------------------------------------
		if (MON_CAP_SPID_INFO.equals(tabName))
		{
			if ( ! colNames.contains("SecondsConnected") ) list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"SecondsConnected"+rq,40)+" "+fill(getDatatype(conn, Types.INTEGER                                   ),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col
			if ( ! colNames.contains("SqlText"         ) ) list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"SqlText"         +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, SpidInfoBatchIdEntry.SQL_TEXT_LEN),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col
			if ( ! colNames.contains("BlockingSqlText" ) ) list.add("alter table " +lq+tabName+rq+ " add  "+ fill(lq+"BlockingSqlText" +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, SpidInfoBatchIdEntry.SQL_TEXT_LEN),20)+" "+getNullable(true)+"\n"); // needs: getNullable(true) when adding col
		}

		//-------------------------------------------------------------
		if (MON_CAP_WAIT_INFO.equals(tabName))
		{
		}

		return list;
	}
	
	@Override
	public Map<Integer, String> getDictionaryCompressionColumnMap(String tabName)
	{
		if (MON_SQL_STATEMENT.equals(tabName))
		{
			// On first time call, create the map and initiate values
			if (_monSqlStatementDictCompColMap == null)
			{
				_monSqlStatementDictCompColMap = new HashMap<>();

				_monSqlStatementDictCompColMap.put(MonSqlStatement.pos_SQLText         , "SQLText");          // Note: This is 1 based (this is a pointer to where in the value array/list we would find the column) 
				_monSqlStatementDictCompColMap.put(MonSqlStatement.pos_NormSQLText     , "NormSQLText");      // Note: This is 1 based
				_monSqlStatementDictCompColMap.put(MonSqlStatement.pos_PlanText        , "PlanText");         // Note: This is 1 based
				_monSqlStatementDictCompColMap.put(MonSqlStatement.pos_BlockedBySqlText, "BlockedBySqlText"); // Note: This is 1 based
			}
			return _monSqlStatementDictCompColMap;
		}
		return null;
	}
	

	@Override
	public String getTableDdlString(DbxConnection conn, DatabaseMetaData dbmd, String tabName)
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection
		
		if (MON_SQL_STATEMENT.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();

			String col_SQLText_name              = "SQLText";
			int    col_SQLText_jdbcType          = Types.VARCHAR;
			int    col_SQLText_jdbcLen           = 65536;
			                                     
			String col_NormSQLText_name          = "NormSQLText";
			int    col_NormSQLText_jdbcType      = Types.VARCHAR;
			int    col_NormSQLText_jdbcLen       = 65536;
			                                     
			String col_PlanText_name             = "PlanText";
			int    col_PlanText_jdbcType         = Types.VARCHAR;
			int    col_PlanText_jdbcLen          = 65536;
			
			String col_BlockedBySqlText_name     = "BlockedBySqlText";
			int    col_BlockedBySqlText_jdbcType = Types.VARCHAR;
			int    col_BlockedBySqlText_jdbcLen  = 65536;
			
			// When we want Dictionary Compression on 'SQLText' and 'NormSQLText'
			if ( DictCompression.isEnabled() )
			{
				DictCompression dcc = DictCompression.getInstance();
				
				try
				{
					dcc.createTable(conn, null, MON_SQL_STATEMENT, col_SQLText_name         , col_SQLText_jdbcType         , col_SQLText_jdbcLen         , true);
					dcc.createTable(conn, null, MON_SQL_STATEMENT, col_NormSQLText_name     , col_NormSQLText_jdbcType     , col_NormSQLText_jdbcLen     , true);
					dcc.createTable(conn, null, MON_SQL_STATEMENT, col_PlanText_name        , col_PlanText_jdbcType        , col_PlanText_jdbcLen        , true);
					dcc.createTable(conn, null, MON_SQL_STATEMENT, col_BlockedBySqlText_name, col_BlockedBySqlText_jdbcType, col_BlockedBySqlText_jdbcLen, true);
					
					col_SQLText_name              = dcc.getDigestSourceColumnName(col_SQLText_name);
					col_SQLText_jdbcType          = dcc.getDigestJdbcType();
					col_SQLText_jdbcLen           = dcc.getDigestLength();
                                                  
					col_NormSQLText_name          = dcc.getDigestSourceColumnName(col_NormSQLText_name);
					col_NormSQLText_jdbcType      = dcc.getDigestJdbcType();
					col_NormSQLText_jdbcLen       = dcc.getDigestLength();
                                                  
					col_PlanText_name             = dcc.getDigestSourceColumnName(col_PlanText_name);
					col_PlanText_jdbcType         = dcc.getDigestJdbcType();
					col_PlanText_jdbcLen          = dcc.getDigestLength();

					col_BlockedBySqlText_name     = dcc.getDigestSourceColumnName(col_BlockedBySqlText_name);
					col_BlockedBySqlText_jdbcType = dcc.getDigestJdbcType();
					col_BlockedBySqlText_jdbcLen  = dcc.getDigestLength();
				}
				catch (SQLException ex)
				{
					_logger.error("Problems creating Dictionary Compressed table for '" + MON_SQL_STATEMENT + "' colName '" + col_SQLText_name + "', '" + col_NormSQLText_name + "' or '" + col_PlanText_name + "'.", ex);
				}
			}
			
			sbSql.append("create table " + tabName + "\n");
			sbSql.append("( \n");
			sbSql.append("    "+fill(lq+"sampleTime"             +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"InstanceID"             +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"SPID"                   +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n"); // NOTE If this pos is changed: alter _stmnt_SPID_pos
			sbSql.append("   ,"+fill(lq+"KPID"                   +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n"); // NOTE If this pos is changed: alter _stmnt_KPID_pos
			sbSql.append("   ,"+fill(lq+"DBID"                   +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"ProcedureID"            +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"PlanID"                 +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"BatchID"                +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n"); // NOTE If this pos is changed: alter _stmnt_BatchID_pos
			sbSql.append("   ,"+fill(lq+"ContextID"              +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"LineNumber"             +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"ObjOwnerID"             +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"DBName"                 +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   30),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"HashKey"                +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"SsqlId"                 +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"ProcName"               +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,  255),20)+" "+getNullable(true)+"\n"); // NULLABLE
			sbSql.append("   ,"+fill(lq+"Elapsed_ms"             +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"CpuTime"                +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"WaitTime"               +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"MemUsageKB"             +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"PhysicalReads"          +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"LogicalReads"           +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"RowsAffected"           +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"ErrorStatus"            +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"ProcNestLevel"          +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"StatementNumber"        +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"QueryOptimizationTime"  +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"PagesModified"          +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"PacketsSent"            +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"PacketsReceived"        +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"NetworkPacketSize"      +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"PlansAltered"           +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"StartTime"              +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"EndTime"                +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");

			sbSql.append("   ,"+fill(lq+"ServerLogin"            +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30  ),20)+" "+getNullable(true)+"\n"); // NULLABLE
			sbSql.append("   ,"+fill(lq+"AddStatus"              +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n"); // NULLABLE
			sbSql.append("   ,"+fill(lq+"JavaSqlLength"          +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n"); // NULLABLE
			sbSql.append("   ,"+fill(lq+"JavaSqlLengthShort"     +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n"); // NULLABLE
			sbSql.append("   ,"+fill(lq+"NormJavaSqlLength"      +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n"); // NULLABLE
			sbSql.append("   ,"+fill(lq+"JavaSqlHashCode"        +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n"); // NULLABLE
			sbSql.append("   ,"+fill(lq+"JavaSqlHashCodeShort"   +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n"); // NULLABLE
			sbSql.append("   ,"+fill(lq+"NormJavaSqlHashCode"    +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n"); // NULLABLE

			sbSql.append("   ,"+fill(lq+col_SQLText_name         +rq,40)+" "+fill(getDatatype(conn, col_SQLText_jdbcType,          col_SQLText_jdbcLen         ),20)+" "+getNullable(true) +"\n"); // NULLABLE
			sbSql.append("   ,"+fill(lq+col_NormSQLText_name     +rq,40)+" "+fill(getDatatype(conn, col_NormSQLText_jdbcType,      col_NormSQLText_jdbcLen     ),20)+" "+getNullable(true) +"\n"); // NULLABLE
			sbSql.append("   ,"+fill(lq+col_PlanText_name        +rq,40)+" "+fill(getDatatype(conn, col_PlanText_jdbcType,         col_PlanText_jdbcLen        ),20)+" "+getNullable(true) +"\n"); // NULLABLE
			sbSql.append("   ,"+fill(lq+col_BlockedBySqlText_name+rq,40)+" "+fill(getDatatype(conn, col_BlockedBySqlText_jdbcType, col_BlockedBySqlText_jdbcLen),20)+" "+getNullable(true) +"\n"); // NULLABLE

			sbSql.append("   ,"+fill(lq+"WaitTimeDetails"        +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 4000),20)+" "+getNullable(true)+"\n"); // NULLABLE
			sbSql.append("   ,"+fill(lq+"BlockedBySpid"          +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n"); // NULLABLE
			sbSql.append("   ,"+fill(lq+"BlockedByKpid"          +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n"); // NULLABLE
			sbSql.append("   ,"+fill(lq+"BlockedByBatchId"       +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n"); // NULLABLE
			sbSql.append("   ,"+fill(lq+"BlockedByCommand"       +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30  ),20)+" "+getNullable(true)+"\n"); // NULLABLE
			sbSql.append("   ,"+fill(lq+"BlockedByApplication"   +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30  ),20)+" "+getNullable(true)+"\n"); // NULLABLE
			sbSql.append("   ,"+fill(lq+"BlockedByTranId"        +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 255 ),20)+" "+getNullable(true)+"\n"); // NULLABLE
			sbSql.append(") \n");
			
			return sbSql.toString();
		}

//		if (MON_SQL_TEXT.equals(tabName))
//		{
//			StringBuilder sbSql = new StringBuilder();
//
//			String col_SQLText_name         = "SQLText";
//			int    col_SQLText_jdbcType     = Types.VARCHAR;
//			int    col_SQLText_jdbcLen      = 65536;
//			
//			String col_NormSQLText_name     = "NormSQLText";
//			int    col_NormSQLText_jdbcType = Types.VARCHAR;
//			int    col_NormSQLText_jdbcLen  = 65536;
//			
//			// TODO: When we want Dictionary Compression on 'SQLText' and 'NormSQLText'
//			if ( DictCompression.isEnabled() )
//			{
//				DictCompression dcc = DictCompression.getInstance();
//				
//				try
//				{
//					dcc.createTable(conn, null, MON_SQL_TEXT, col_SQLText_name    , col_SQLText_jdbcType    , col_SQLText_jdbcLen    , true);
//					dcc.createTable(conn, null, MON_SQL_TEXT, col_NormSQLText_name, col_NormSQLText_jdbcType, col_NormSQLText_jdbcLen, true);
//					
//					col_SQLText_name         = dcc.getDigestSourceColumnName(col_SQLText_name);
//					col_SQLText_jdbcType     = dcc.getDigestJdbcType();
//					col_SQLText_jdbcLen      = dcc.getDigestLength();
//
//					col_NormSQLText_name     = dcc.getDigestSourceColumnName(col_NormSQLText_name);
//					col_NormSQLText_jdbcType = dcc.getDigestJdbcType();
//					col_NormSQLText_jdbcLen  = dcc.getDigestLength();
//				}
//				catch (SQLException ex)
//				{
//					_logger.error("Problems creating Dictionary Compressed table for '" + MON_SQL_TEXT + "' colName '" + col_SQLText_name + "' or '" + col_NormSQLText_name + "'.", ex);
//				}
//			}
//			
//			sbSql.append("create table " + tabName + "\n");
//			sbSql.append("( \n");
//			sbSql.append("    "+fill(lq+"sampleTime"          +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP                                  ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"InstanceID"          +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER                                    ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"SPID"                +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER                                    ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"KPID"                +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER                                    ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"BatchID"             +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER                                    ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"ServerLogin"         +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30                                ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"AddMethod"           +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER                                    ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"JavaSqlLength"       +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER                                    ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"JavaSqlLengthShort"  +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER                                    ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"JavaSqlHashCode"     +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER                                    ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"JavaSqlHashCodeShort"+rq,40)+" "+fill(getDatatype(conn, Types.INTEGER                                    ),20)+" "+getNullable(false)+"\n");
////			sbSql.append("   ,"+fill(lq+"SQLText"             +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 65536                             ),20)+" "+getNullable(true) +"\n");
//			sbSql.append("   ,"+fill(lq+col_SQLText_name      +rq,40)+" "+fill(getDatatype(conn, col_SQLText_jdbcType,     col_SQLText_jdbcLen    ),20)+" "+getNullable(true) +"\n");
//			sbSql.append("   ,"+fill(lq+"NormJavaSqlHashCode" +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER                                    ),20)+" "+getNullable(true) +"\n");
////			sbSql.append("   ,"+fill(lq+"NormSQLText"         +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 65536                             ),20)+" "+getNullable(true) +"\n");
//			sbSql.append("   ,"+fill(lq+col_NormSQLText_name  +rq,40)+" "+fill(getDatatype(conn, col_NormSQLText_jdbcType, col_NormSQLText_jdbcLen),20)+" "+getNullable(true) +"\n");
//			sbSql.append(") \n");
//
//			return sbSql.toString();
//		}

//		if (MON_SQL_STATEMENT.equals(tabName))
//		{
//			StringBuilder sbSql = new StringBuilder();
//
//			sbSql.append("create table " + tabName + "\n");
//			sbSql.append("( \n");
//			sbSql.append("    "+fill(lq+"sampleTime"           +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"InstanceID"           +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"SPID"                 +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n"); // NOTE If this pos is changed: alter _stmnt_SPID_pos
//			sbSql.append("   ,"+fill(lq+"KPID"                 +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n"); // NOTE If this pos is changed: alter _stmnt_KPID_pos
//			sbSql.append("   ,"+fill(lq+"DBID"                 +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"ProcedureID"          +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"PlanID"               +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"BatchID"              +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n"); // NOTE If this pos is changed: alter _stmnt_BatchID_pos
//			sbSql.append("   ,"+fill(lq+"ContextID"            +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"LineNumber"           +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"ObjOwnerID"           +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"DBName"               +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   30),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"HashKey"              +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"SsqlId"               +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"ProcName"             +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,  255),20)+" "+getNullable(true)+"\n"); // NULLABLE
//			sbSql.append("   ,"+fill(lq+"Elapsed_ms"           +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"CpuTime"              +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"WaitTime"             +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"MemUsageKB"           +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"PhysicalReads"        +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"LogicalReads"         +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"RowsAffected"         +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"ErrorStatus"          +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"ProcNestLevel"        +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"StatementNumber"      +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"QueryOptimizationTime"+rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"PagesModified"        +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"PacketsSent"          +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"PacketsReceived"      +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"NetworkPacketSize"    +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"PlansAltered"         +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"StartTime"            +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"EndTime"              +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"JavaSqlHashCode"      +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"JavaSqlHashCodeShort" +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"NormJavaSqlHashCode"  +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n"); // NULLABLE
//			sbSql.append(") \n");
//
//			return sbSql.toString();
//		}

//		if (MON_SQL_PLAN.equals(tabName))
//		{
//			StringBuilder sbSql = new StringBuilder();
//			
//			sbSql.append("create table " + tabName + "\n");
//			sbSql.append("( \n");
//			sbSql.append("    "+fill(lq+"sampleTime"       +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP     ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"InstanceID"       +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"SPID"             +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"KPID"             +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"PlanID"           +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"BatchID"          +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"ContextID"        +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"DBID"             +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"DBName"           +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30   ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"ProcedureID"      +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"AddMethod"        +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
//			sbSql.append("   ,"+fill(lq+"PlanText"         +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 65536),20)+" "+getNullable(true) +"\n");
//			sbSql.append(") \n");
//
//			return sbSql.toString();
//		}

		//-------------------------------------------------------------
		if (MON_CAP_SPID_INFO.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();
			
			sbSql.append("create table " + tabName + "\n");
			sbSql.append("( \n");
			sbSql.append("    "+fill(lq+"sampleTime"         +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP     ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"SPID"               +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"KPID"               +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"BatchID"            +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"ContextID"          +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"LineNumber"         +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"SecondsConnected"   +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"Command"            +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30   ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"SecondsWaiting"     +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"WaitEventID"        +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"BlockingSPID"       +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"BlockingKPID"       +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"BlockingBatchID"    +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"BlockingXLOID"      +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"NumChildren"        +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"Login"              +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30   ),20)+" "+getNullable(true )+"\n");
			sbSql.append("   ,"+fill(lq+"DBName"             +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30   ),20)+" "+getNullable(true )+"\n");
			sbSql.append("   ,"+fill(lq+"Application"        +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30   ),20)+" "+getNullable(true )+"\n");
			sbSql.append("   ,"+fill(lq+"HostName"           +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30   ),20)+" "+getNullable(true )+"\n");
			sbSql.append("   ,"+fill(lq+"MasterTransactionID"+rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 255  ),20)+" "+getNullable(true )+"\n");
			sbSql.append("   ,"+fill(lq+"snapWaitTimeDetails"+rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 4000 ),20)+" "+getNullable(true )+"\n");
			sbSql.append("   ,"+fill(lq+"SqlText"            +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, SpidInfoBatchIdEntry.SQL_TEXT_LEN ),20)+" "+getNullable(true )+"\n");
			sbSql.append("   ,"+fill(lq+"BlockingSqlText"    +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, SpidInfoBatchIdEntry.SQL_TEXT_LEN ),20)+" "+getNullable(true )+"\n");
			sbSql.append(") \n");

			return sbSql.toString();
		}

		//-------------------------------------------------------------
		if (MON_CAP_WAIT_INFO.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();
			
			sbSql.append("create table " + tabName + "\n");
			sbSql.append("( \n");
			sbSql.append("    "+fill(lq+"sampleTime"         +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP     ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"SPID"               +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"KPID"               +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"WaitEventID"        +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"WaitClassID"        +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"snapshotBatchID"    +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"Waits_abs"          +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"Waits_diff"         +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"WaitTime_abs"       +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(lq+"WaitTime_diff"      +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
			sbSql.append(") \n");

			return sbSql.toString();
		}

		return null;
	}

//	private static class MonSqlText
//	{
//		Timestamp  sampleTime           ; // Types.TIMESTAMP                                  ),20)+" "+getNullable(false)+"\n");
//		int        InstanceID           ; // Types.INTEGER                                    ),20)+" "+getNullable(false)+"\n");
//		int        SPID                 ; // Types.INTEGER                                    ),20)+" "+getNullable(false)+"\n");
//		int        KPID                 ; // Types.INTEGER                                    ),20)+" "+getNullable(false)+"\n");
//		int        BatchID              ; // Types.INTEGER                                    ),20)+" "+getNullable(false)+"\n");
//		String     ServerLogin          ; // Types.VARCHAR, 30                                ),20)+" "+getNullable(false)+"\n");
//		int        AddMethod            ; // Types.INTEGER                                    ),20)+" "+getNullable(false)+"\n");
//		int        JavaSqlLength        ; // Types.INTEGER                                    ),20)+" "+getNullable(false)+"\n");
//		int        JavaSqlLengthShort   ; // Types.INTEGER                                    ),20)+" "+getNullable(false)+"\n");
//		int        JavaSqlHashCode      ; // Types.INTEGER                                    ),20)+" "+getNullable(false)+"\n");
//		int        JavaSqlHashCodeShort ; // Types.INTEGER                                    ),20)+" "+getNullable(false)+"\n");
//		String     SQLText              ; // col_SQLText_jdbcType,     col_SQLText_jdbcLen    ),20)+" "+getNullable(true) +"\n");
//		int        NormJavaSqlHashCode  ; // Types.INTEGER                                    ),20)+" "+getNullable(true) +"\n");
//		String     NormSQLText          ; // col_NormSQLText_jdbcType, col_NormSQLText_jdbcLen),20)+" "+getNullable(true) +"\n");
//
////		// Below is used to "not have to lookup COLNAME->pos" every time
////		private static boolean _isColNamesToPosResolved = false;
//		// Below is used to create the List size for every row
//		private static int     _numberOfCol             = 15;
//
////		private static int pos_sampleTime           = -1;
////		private static int pos_InstanceID           = -1;
////		private static int pos_SPID                 = -1;
////		private static int pos_KPID                 = -1;
////		private static int pos_BatchID              = -1;
////		private static int pos_ServerLogin          = -1;
////		private static int pos_AddMethod            = -1;
////		private static int pos_JavaSqlLength        = -1;
////		private static int pos_JavaSqlLengthShort   = -1;
////		private static int pos_JavaSqlHashCode      = -1;
////		private static int pos_JavaSqlHashCodeShort = -1;
////		private static int pos_SQLText              = -1;
//		private static int pos_SQLText              = 12; // NOTE: this is used in method: getDictionaryCompressionColumnMap() and must be maintained if position is changed
////		private static int pos_NormJavaSqlHashCode  = -1;
////		private static int pos_NormSQLText          = -1;
//		private static int pos_NormSQLText          = 14; // NOTE: this is used in method: getDictionaryCompressionColumnMap() and must be maintained if position is changed
////
////		private static void resolveColNamesToPos(ResultSet rs)
////		throws SQLException
////		{
////			List<String> colNames = DbUtils.getColumnNames(rs.getMetaData());
////
////			pos_sampleTime           = findInListToJdbcPos(colNames, "sampleTime"          );
////			pos_InstanceID           = findInListToJdbcPos(colNames, "InstanceID"          );
////			pos_SPID                 = findInListToJdbcPos(colNames, "SPID"                );
////			pos_KPID                 = findInListToJdbcPos(colNames, "KPID"                );
////			pos_BatchID              = findInListToJdbcPos(colNames, "BatchID"             );
////			pos_ServerLogin          = findInListToJdbcPos(colNames, "ServerLogin"         );
////			pos_AddMethod            = findInListToJdbcPos(colNames, "AddMethod"           );
////			pos_JavaSqlLength        = findInListToJdbcPos(colNames, "JavaSqlLength"       );
////			pos_JavaSqlLengthShort   = findInListToJdbcPos(colNames, "JavaSqlLengthShort"  );
////			pos_JavaSqlHashCode      = findInListToJdbcPos(colNames, "JavaSqlHashCode"     );
////			pos_JavaSqlHashCodeShort = findInListToJdbcPos(colNames, "JavaSqlHashCodeShort");
////			pos_SQLText              = findInListToJdbcPos(colNames, "SQLText"             );
////			pos_NormJavaSqlHashCode  = findInListToJdbcPos(colNames, "NormJavaSqlHashCode" );
////			pos_NormSQLText          = findInListToJdbcPos(colNames, "NormSQLText"         );
////			
////			_numberOfCol = rs.getMetaData().getColumnCount();
////			_isColNamesToPosResolved = true;
////		}
////
////		public MonSqlText(ResultSet rs)
////		throws SQLException
////		{
////			if ( ! _isColNamesToPosResolved )
////			{
////				resolveColNamesToPos(rs);
////			}
////				
////			sampleTime            = rs.getTimestamp(pos_sampleTime          );
////			InstanceID            = rs.getInt      (pos_InstanceID          );
////			SPID                  = rs.getInt      (pos_SPID                );
////			KPID                  = rs.getInt      (pos_KPID                );
////			BatchID               = rs.getInt      (pos_BatchID             );
////			ServerLogin           = rs.getString   (pos_ServerLogin         );
////			AddMethod             = rs.getInt      (pos_AddMethod           );
////			JavaSqlLength         = rs.getInt      (pos_JavaSqlLength       );	
////			JavaSqlLengthShort    = rs.getInt      (pos_JavaSqlLengthShort  );	
////			JavaSqlHashCode       = rs.getInt      (pos_JavaSqlHashCode     );	
////			JavaSqlHashCodeShort  = rs.getInt      (pos_JavaSqlHashCodeShort);	
////			SQLText               = rs.getString   (pos_SQLText             );	
////			NormJavaSqlHashCode   = rs.getInt      (pos_NormJavaSqlHashCode );	
////			NormSQLText           = rs.getString   (pos_NormSQLText         );	
////		}
//		
//		public List<Object> getPcsRow()
//		{
//			List<Object> row = new ArrayList<>(_numberOfCol);
//
//			row.add(MON_SQL_TEXT);
//			row.add(sampleTime          );
//			row.add(InstanceID          );
//			row.add(SPID                );
//			row.add(KPID                );
//			row.add(BatchID             );
//			row.add(ServerLogin         );
//			row.add(AddMethod           );
//			row.add(JavaSqlLength       );
//			row.add(JavaSqlLengthShort  );
//			row.add(JavaSqlHashCode     );
//			row.add(JavaSqlHashCodeShort);
//			row.add(SQLText             );
//			row.add(NormJavaSqlHashCode );
//			row.add(NormSQLText         );
//			
//			return row;
//		}
//	}
//
//	private static class MonSqlPlan
//	{
//		Timestamp  sampleTime           ; // Types.TIMESTAMP     ),20)+" "+getNullable(false)+"\n");
//		int        InstanceID           ; // Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
//		int        SPID                 ; // Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
//		int        KPID                 ; // Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
//		int        PlanID               ; // Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
//		int        BatchID              ; // Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
//		int        ContextID            ; // Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
//		int        DBID                 ; // Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
//		String     DBName               ; // Types.VARCHAR, 30   ),20)+" "+getNullable(false)+"\n");
//		int        ProcedureID          ; // Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
//		int        AddMethod            ; // Types.INTEGER       ),20)+" "+getNullable(false)+"\n");
//		String     PlanText             ; // Types.VARCHAR, 65536),20)+" "+getNullable(true) +"\n");
//
//		// Below is used to "not have to lookup COLNAME->pos" every time
////		private static boolean _isColNamesToPosResolved = false;
//		// Below is used to create the List size for every row
//		private static int     _numberOfCol             = 13;
//
////		private static int pos_sampleTime  = -1;
////		private static int pos_InstanceID  = -1;
////		private static int pos_SPID        = -1;
////		private static int pos_KPID        = -1;
////		private static int pos_PlanID      = -1;
////		private static int pos_BatchID     = -1;
////		private static int pos_ContextID   = -1;
////		private static int pos_DBID        = -1;
////		private static int pos_DBName      = -1;
////		private static int pos_ProcedureID = -1;
////		private static int pos_AddMethod   = -1;
////		private static int pos_PlanText    = -1;
////
////		private static void resolveColNamesToPos(ResultSet rs)
////		throws SQLException
////		{
////			List<String> colNames = DbUtils.getColumnNames(rs.getMetaData());
////
////			pos_sampleTime  = findInListToJdbcPos(colNames, "sampleTime" );
////			pos_InstanceID  = findInListToJdbcPos(colNames, "InstanceID" );
////			pos_SPID        = findInListToJdbcPos(colNames, "SPID"       );
////			pos_KPID        = findInListToJdbcPos(colNames, "KPID"       );
////			pos_PlanID      = findInListToJdbcPos(colNames, "PlanID"     );
////			pos_BatchID     = findInListToJdbcPos(colNames, "BatchID"    );
////			pos_ContextID   = findInListToJdbcPos(colNames, "ContextID"  );
////			pos_DBID        = findInListToJdbcPos(colNames, "DBID"       );
////			pos_DBName      = findInListToJdbcPos(colNames, "DBName"     );
////			pos_ProcedureID = findInListToJdbcPos(colNames, "ProcedureID");
////			pos_AddMethod   = findInListToJdbcPos(colNames, "AddMethod"  );
////			pos_PlanText    = findInListToJdbcPos(colNames, "PlanText"   );
////			
////			_numberOfCol = rs.getMetaData().getColumnCount();
////			_isColNamesToPosResolved = true;
////		}
////
////		public MonSqlPlan(ResultSet rs)
////		throws SQLException
////		{
////			if ( ! _isColNamesToPosResolved )
////			{
////				resolveColNamesToPos(rs);
////			}
////				
////			sampleTime  = rs.getTimestamp(pos_sampleTime );
////			InstanceID  = rs.getInt      (pos_InstanceID );
////			SPID        = rs.getInt      (pos_SPID       );
////			KPID        = rs.getInt      (pos_KPID       );
////			PlanID      = rs.getInt      (pos_PlanID     );
////			BatchID     = rs.getInt      (pos_BatchID    );
////			ContextID   = rs.getInt      (pos_ContextID  );
////			DBID        = rs.getInt      (pos_DBID       );	
////			DBName      = rs.getString   (pos_DBName     );	
////			ProcedureID = rs.getInt      (pos_ProcedureID);	
////			AddMethod   = rs.getInt      (pos_AddMethod  );	
////			PlanText    = rs.getString   (pos_PlanText   );	
////		}
//		
//		public List<Object> getPcsRow()
//		{
//			List<Object> row = new ArrayList<>(_numberOfCol);
//
//			row.add(MON_SQL_PLAN);
//			row.add(sampleTime );
//			row.add(InstanceID );
//			row.add(SPID       );
//			row.add(KPID       );
//			row.add(PlanID     );
//			row.add(BatchID    );
//			row.add(ContextID  );
//			row.add(DBID       );
//			row.add(DBName     );
//			row.add(ProcedureID);
//			row.add(AddMethod  );
//			row.add(PlanText   );
//			
//			return row;
//		}
//	}

	private static class MonSqlStatement
	{
		Timestamp  sampleTime           ; // Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");
		int        InstanceID           ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        SPID                 ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n"); // NOTE If this pos is changed: alter _stmnt_SPID_pos
		int        KPID                 ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n"); // NOTE If this pos is changed: alter _stmnt_KPID_pos
		int        DBID                 ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        ProcedureID          ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        PlanID               ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        BatchID              ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n"); // NOTE If this pos is changed: alter _stmnt_BatchID_pos
		int        ContextID            ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        LineNumber           ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        ObjOwnerID           ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		String     DBName               ; // Types.VARCHAR,   30),20)+" "+getNullable(false)+"\n");
		int        HashKey              ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        SsqlId               ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		String     ProcName             ; // Types.VARCHAR,  255),20)+" "+getNullable(true)+"\n"); // NULLABLE
		int        Elapsed_ms           ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        CpuTime              ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        WaitTime             ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        MemUsageKB           ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        PhysicalReads        ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        LogicalReads         ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        RowsAffected         ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        ErrorStatus          ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        ProcNestLevel        ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        StatementNumber      ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        QueryOptimizationTime; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        PagesModified        ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        PacketsSent          ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        PacketsReceived      ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        NetworkPacketSize    ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        PlansAltered         ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		Timestamp  StartTime            ; // Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");
		Timestamp  EndTime              ; // Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");

		String     ServerLogin          ; // Types.VARCHAR, 30  ),20)+" "+getNullable(false)+"\n");
		int        AddStatus            ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        JavaSqlLength        ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        JavaSqlLengthShort   ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        NormJavaSqlLength    ; // Types.INTEGER      ),20)+" "+getNullable(true)+"\n"); // NULLABLE
		int        JavaSqlHashCode      ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        JavaSqlHashCodeShort ; // Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
		int        NormJavaSqlHashCode  ; // Types.INTEGER      ),20)+" "+getNullable(true)+"\n"); // NULLABLE

		String     SQLText              ;
		String     NormSQLText          ;
		String     PlanText             ;
		String     BlockedBySqlText     ;

		// Extra information that is "filled in" if _sampleSpidInfo && _sampleWaitInfo is enabled
		String     WaitTimeDetails      ;
		int        BlockedBySpid        ;
		int        BlockedByKpid        ;
		int        BlockedByBatchId     ;
		String     BlockedByCommand     ;
		String     BlockedByApplication ;
		String     BlockedByTranId      ;


		// Below is used to "not have to lookup COLNAME->pos" every time
		private static boolean _isColNamesToPosResolved = false;
		// Below is used to create the List size for every row
		private static int     _numberOfCol             = -1;

		private static int pos_sampleTime            = -1;
		private static int pos_InstanceID            = -1;
		private static int pos_SPID                  = -1;
		private static int pos_KPID                  = -1;
		private static int pos_DBID                  = -1;
		private static int pos_ProcedureID           = -1;
		private static int pos_PlanID                = -1;
		private static int pos_BatchID               = -1;
		private static int pos_ContextID             = -1;
		private static int pos_LineNumber            = -1;
		private static int pos_ObjOwnerID            = -1;
		private static int pos_DBName                = -1;
		private static int pos_HashKey               = -1;
		private static int pos_SsqlId                = -1;
		private static int pos_ProcName              = -1;
		private static int pos_Elapsed_ms            = -1;
		private static int pos_CpuTime               = -1;
		private static int pos_WaitTime              = -1;
		private static int pos_MemUsageKB            = -1;
		private static int pos_PhysicalReads         = -1;
		private static int pos_LogicalReads          = -1;
		private static int pos_RowsAffected          = -1;
		private static int pos_ErrorStatus           = -1;
		private static int pos_ProcNestLevel         = -1;
		private static int pos_StatementNumber       = -1;
		private static int pos_QueryOptimizationTime = -1;
		private static int pos_PagesModified         = -1;
		private static int pos_PacketsSent           = -1;
		private static int pos_PacketsReceived       = -1;
		private static int pos_NetworkPacketSize     = -1;
		private static int pos_PlansAltered          = -1;
		private static int pos_StartTime             = -1;
		private static int pos_EndTime               = -1;
		
		private static int pos_ServerLogin           = -1;
		private static int pos_AddStatus             = -1;
		private static int pos_JavaSqlLength         = -1;
		private static int pos_JavaSqlLengthShort    = -1;
		private static int pos_NormJavaSqlLength     = -1;
		private static int pos_JavaSqlHashCode       = -1;
		private static int pos_JavaSqlHashCodeShort  = -1;
		private static int pos_NormJavaSqlHashCode   = -1;

		private static int pos_SQLText               = -1;
		private static int pos_NormSQLText           = -1;
		private static int pos_PlanText              = -1;
		private static int pos_BlockedBySqlText      = -1;

		private static void resolveColNamesToPos(ResultSet rs)
		throws SQLException
		{
			List<String> colNames = DbUtils.getColumnNames(rs.getMetaData());

			pos_sampleTime            = findInListToJdbcPos(colNames, "sampleTime"); 
			pos_InstanceID            = findInListToJdbcPos(colNames, "InstanceID");
			pos_SPID                  = findInListToJdbcPos(colNames, "SPID");
			pos_KPID                  = findInListToJdbcPos(colNames, "KPID");
			pos_DBID                  = findInListToJdbcPos(colNames, "DBID");
			pos_ProcedureID           = findInListToJdbcPos(colNames, "ProcedureID");
			pos_PlanID                = findInListToJdbcPos(colNames, "PlanID");
			pos_BatchID               = findInListToJdbcPos(colNames, "BatchID");
			pos_ContextID             = findInListToJdbcPos(colNames, "ContextID");
			pos_LineNumber            = findInListToJdbcPos(colNames, "LineNumber");
			pos_ObjOwnerID            = findInListToJdbcPos(colNames, "ObjOwnerID");
			pos_DBName                = findInListToJdbcPos(colNames, "DBName");
			pos_HashKey               = findInListToJdbcPos(colNames, "HashKey");
			pos_SsqlId                = findInListToJdbcPos(colNames, "SsqlId");
			pos_ProcName              = findInListToJdbcPos(colNames, "ProcName");
			pos_Elapsed_ms            = findInListToJdbcPos(colNames, "Elapsed_ms");
			pos_CpuTime               = findInListToJdbcPos(colNames, "CpuTime");
			pos_WaitTime              = findInListToJdbcPos(colNames, "WaitTime");
			pos_MemUsageKB            = findInListToJdbcPos(colNames, "MemUsageKB");
			pos_PhysicalReads         = findInListToJdbcPos(colNames, "PhysicalReads");
			pos_LogicalReads          = findInListToJdbcPos(colNames, "LogicalReads");
			pos_RowsAffected          = findInListToJdbcPos(colNames, "RowsAffected");
			pos_ErrorStatus           = findInListToJdbcPos(colNames, "ErrorStatus");
			pos_ProcNestLevel         = findInListToJdbcPos(colNames, "ProcNestLevel");
			pos_StatementNumber       = findInListToJdbcPos(colNames, "StatementNumber");
			pos_QueryOptimizationTime = findInListToJdbcPos(colNames, "QueryOptimizationTime");
			pos_PagesModified         = findInListToJdbcPos(colNames, "PagesModified");
			pos_PacketsSent           = findInListToJdbcPos(colNames, "PacketsSent");
			pos_PacketsReceived       = findInListToJdbcPos(colNames, "PacketsReceived");
			pos_NetworkPacketSize     = findInListToJdbcPos(colNames, "NetworkPacketSize");
			pos_PlansAltered          = findInListToJdbcPos(colNames, "PlansAltered");
			pos_StartTime             = findInListToJdbcPos(colNames, "StartTime");
			pos_EndTime               = findInListToJdbcPos(colNames, "EndTime");
			
			pos_ServerLogin           = findInListToJdbcPos(colNames, "ServerLogin");
			pos_AddStatus             = findInListToJdbcPos(colNames, "AddStatus");
			pos_JavaSqlLength         = findInListToJdbcPos(colNames, "JavaSqlLength");
			pos_JavaSqlLengthShort    = findInListToJdbcPos(colNames, "JavaSqlLengthShort");
			pos_NormJavaSqlLength     = findInListToJdbcPos(colNames, "NormJavaSqlLength");
			pos_JavaSqlHashCode       = findInListToJdbcPos(colNames, "JavaSqlHashCode");
			pos_JavaSqlHashCodeShort  = findInListToJdbcPos(colNames, "JavaSqlHashCodeShort");
			pos_NormJavaSqlHashCode   = findInListToJdbcPos(colNames, "NormJavaSqlHashCode");

			pos_SQLText               = findInListToJdbcPos(colNames, "SQLText");
			pos_NormSQLText           = findInListToJdbcPos(colNames, "NormSQLText");
			pos_PlanText              = findInListToJdbcPos(colNames, "PlanText");
			pos_BlockedBySqlText      = findInListToJdbcPos(colNames, "BlockedBySqlText");

			_numberOfCol = rs.getMetaData().getColumnCount();
			_isColNamesToPosResolved = true;
		}

		public MonSqlStatement(ResultSet rs)
		throws SQLException
		{
			if ( ! _isColNamesToPosResolved )
			{
				resolveColNamesToPos(rs);
			}
				
			sampleTime            = rs.getTimestamp(pos_sampleTime           );
			InstanceID            = rs.getInt      (pos_InstanceID           );
			SPID                  = rs.getInt      (pos_SPID                 );
			KPID                  = rs.getInt      (pos_KPID                 );
			DBID                  = rs.getInt      (pos_DBID                 );
			ProcedureID           = rs.getInt      (pos_ProcedureID          );
			PlanID                = rs.getInt      (pos_PlanID               );
			BatchID               = rs.getInt      (pos_BatchID              );
			ContextID             = rs.getInt      (pos_ContextID            );
			LineNumber            = rs.getInt      (pos_LineNumber           );
			ObjOwnerID            = rs.getInt      (pos_ObjOwnerID           );
			DBName                = rs.getString   (pos_DBName               );
			HashKey               = rs.getInt      (pos_HashKey              );
			SsqlId                = rs.getInt      (pos_SsqlId               );
			ProcName              = rs.getString   (pos_ProcName             );
			Elapsed_ms            = rs.getInt      (pos_Elapsed_ms           );
			CpuTime               = rs.getInt      (pos_CpuTime              );
			WaitTime              = rs.getInt      (pos_WaitTime             );
			MemUsageKB            = rs.getInt      (pos_MemUsageKB           );
			PhysicalReads         = rs.getInt      (pos_PhysicalReads        );
			LogicalReads          = rs.getInt      (pos_LogicalReads         );
			RowsAffected          = rs.getInt      (pos_RowsAffected         );
			ErrorStatus           = rs.getInt      (pos_ErrorStatus          );
			ProcNestLevel         = rs.getInt      (pos_ProcNestLevel        );
			StatementNumber       = rs.getInt      (pos_StatementNumber      );
			QueryOptimizationTime = rs.getInt      (pos_QueryOptimizationTime);
			PagesModified         = rs.getInt      (pos_PagesModified        );
			PacketsSent           = rs.getInt      (pos_PacketsSent          );
			PacketsReceived       = rs.getInt      (pos_PacketsReceived      );
			NetworkPacketSize     = rs.getInt      (pos_NetworkPacketSize    );
			PlansAltered          = rs.getInt      (pos_PlansAltered         );
			StartTime             = rs.getTimestamp(pos_StartTime            );
			EndTime               = rs.getTimestamp(pos_EndTime              );

			ServerLogin           = rs.getString   (pos_ServerLogin);
			AddStatus             = rs.getInt      (pos_AddStatus);
			JavaSqlLength         = rs.getInt      (pos_JavaSqlLength);
			JavaSqlLengthShort    = rs.getInt      (pos_JavaSqlLengthShort);
			NormJavaSqlLength     = rs.getInt      (pos_NormJavaSqlLength);
			JavaSqlHashCode       = rs.getInt      (pos_JavaSqlHashCode);
			JavaSqlHashCodeShort  = rs.getInt      (pos_JavaSqlHashCodeShort);
			NormJavaSqlHashCode   = rs.getInt      (pos_NormJavaSqlHashCode);

			SQLText               = rs.getString   (pos_SQLText);
			NormSQLText           = rs.getString   (pos_NormSQLText);
			PlanText              = rs.getString   (pos_PlanText);
			BlockedBySqlText      = rs.getString   (pos_BlockedBySqlText);
		}
		
		public List<Object> getPcsRow()
		{
			List<Object> row = new ArrayList<>(_numberOfCol);

			row.add(MON_SQL_STATEMENT);
			row.add(sampleTime           );
			row.add(InstanceID           );
			row.add(SPID                 );
			row.add(KPID                 );
			row.add(DBID                 );
			row.add(ProcedureID          );
			row.add(PlanID               );
			row.add(BatchID              );
			row.add(ContextID            );
			row.add(LineNumber           );
			row.add(ObjOwnerID           );
			row.add(DBName               );
			row.add(HashKey              );
			row.add(SsqlId               );
			row.add(ProcName             );
			row.add(Elapsed_ms           );
			row.add(CpuTime              );
			row.add(WaitTime             );
			row.add(MemUsageKB           );
			row.add(PhysicalReads        );
			row.add(LogicalReads         );
			row.add(RowsAffected         );
			row.add(ErrorStatus          );
			row.add(ProcNestLevel        );
			row.add(StatementNumber      );
			row.add(QueryOptimizationTime);
			row.add(PagesModified        );
			row.add(PacketsSent          );
			row.add(PacketsReceived      );
			row.add(NetworkPacketSize    );
			row.add(PlansAltered         );
			row.add(StartTime            );
			row.add(EndTime              );

			row.add(ServerLogin          );
			row.add(AddStatus            );
			row.add(JavaSqlLength        );
			row.add(JavaSqlLengthShort   );
			row.add(NormJavaSqlLength    );
			row.add(JavaSqlHashCode      );
			row.add(JavaSqlHashCodeShort );
			row.add(NormJavaSqlHashCode  );

			row.add(SQLText              );
			row.add(NormSQLText          );
			row.add(PlanText             );
			row.add(BlockedBySqlText     );

			row.add(WaitTimeDetails      );
			row.add(BlockedBySpid        );
			row.add(BlockedByKpid        );
			row.add(BlockedByBatchId     );
			row.add(BlockedByCommand     );
			row.add(BlockedByApplication );
			row.add(BlockedByTranId      );

			return row;
		}
	}

	/** 
	 * Convert 
	 * @param colNames
	 * @param colName
	 * @return -1 if Column wasn't found. otherwise the List index + 1 (to adjust for JDBC pos)
	 */
	private static int findInListToJdbcPos(List<String> colNames, String colName)
	{
		int pos = colNames.indexOf(colName);
		if (pos == -1)
		{
			_logger.error("Trying to find column '" + colName + "', but it did NOT exist. returning -1. colNames=" + colNames);
			return pos;
		}

		return pos + 1;
	}

	@Override
	public List<String> getIndexDdlString(DbxConnection conn, DatabaseMetaData dbmd, String tabName)
	{
		// Put indexes in this list that will be returned
		List<String> list = new ArrayList<>();

//		if (MON_SQL_TEXT.equals(tabName))
//		{
//			list.add("create index " + conn.quotify(tabName+"_ix1") + " on " + conn.quotify(tabName) + "(" + conn.quotify("BatchID", "SPID", "KPID") + ")\n");
//		}

		if (MON_SQL_STATEMENT.equals(tabName))
		{
//			list.add("create index " + conn.quotify(tabName+"_ix1") + " on " + conn.quotify(tabName) + "(" + conn.quotify("BatchID", "SPID", "KPID") + ")\n");
			list.add("create index " + conn.quotify(tabName+"_ix2") + " on " + conn.quotify(tabName) + "(" + conn.quotify("StartTime", "EndTime")    + ")\n");
		}

//		if (MON_SQL_PLAN.equals(tabName))
//		{
//			list.add("create index " + conn.quotify(tabName+"_ix1") + " on " + conn.quotify(tabName) + "(" + conn.quotify("BatchID", "SPID", "KPID") + ")\n");
//		}

		if (MON_CAP_SPID_INFO.equals(tabName))
		{
			list.add("create index " + conn.quotify(tabName+"_ix1") + " on " + conn.quotify(tabName) + "(" + conn.quotify("sampleTime", "SPID", "KPID") + ")\n");
		}

		if (MON_CAP_WAIT_INFO.equals(tabName))
		{
			list.add("create index " + conn.quotify(tabName+"_ix1") + " on " + conn.quotify(tabName) + "(" + conn.quotify("sampleTime", "SPID", "KPID", "WaitEventID") + ")\n");
		}

		return list;
	}

	@Override
	public String getInsertStatement(DbxConnection conn, String tabName)
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection
		
//		if (MON_SQL_TEXT.equals(tabName))
//		{
//			StringBuilder sbSql = new StringBuilder();
//
//			String col_SQLText_name         = "SQLText";
//			String col_NormSQLText_name     = "NormSQLText";
//			
//			// TODO: When we want Dictionary Compression on 'SQLText' and 'NormSQLText'
//			if ( DictCompression.isEnabled() )
//			{
//				DictCompression dcc = DictCompression.getInstance();
//				
//				col_SQLText_name         = dcc.getDigestSourceColumnName(col_SQLText_name);
//				col_NormSQLText_name     = dcc.getDigestSourceColumnName(col_NormSQLText_name);
//			}
//
//			sbSql.append("insert into ").append(tabName);
//			sbSql.append("(");
//			sbSql.append(" ").append(lq).append("sampleTime"          ).append(rq); // 1
//			sbSql.append(",").append(lq).append("InstanceID"          ).append(rq); // 2
//			sbSql.append(",").append(lq).append("SPID"                ).append(rq); // 3
//			sbSql.append(",").append(lq).append("KPID"                ).append(rq); // 4
//			sbSql.append(",").append(lq).append("BatchID"             ).append(rq); // 5
//			sbSql.append(",").append(lq).append("ServerLogin"         ).append(rq); // 6
//			sbSql.append(",").append(lq).append("AddMethod"           ).append(rq); // 7
//			sbSql.append(",").append(lq).append("JavaSqlLength"       ).append(rq); // 8
//			sbSql.append(",").append(lq).append("JavaSqlLengthShort"  ).append(rq); // 9
//			sbSql.append(",").append(lq).append("JavaSqlHashCode"     ).append(rq); // 10
//			sbSql.append(",").append(lq).append("JavaSqlHashCodeShort").append(rq); // 11
//			sbSql.append(",").append(lq).append(col_SQLText_name      ).append(rq); // 12
//			sbSql.append(",").append(lq).append("NormJavaSqlHashCode" ).append(rq); // 13
//			sbSql.append(",").append(lq).append(col_NormSQLText_name  ).append(rq); // 14
//			sbSql.append(") \n");
//			sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n"); // 14 question marks
//			//                   1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14
//
//			return sbSql.toString();
//		}

		if (MON_SQL_STATEMENT.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();

			String col_SQLText_name          = "SQLText";
			String col_NormSQLText_name      = "NormSQLText";
			String col_PlanText_name         = "PlanText";
			String col_BlockedBySqlText_name = "BlockedBySqlText";
			
			// TODO: When we want Dictionary Compression on 'SQLText' and 'NormSQLText'
			if ( DictCompression.isEnabled() )
			{
				DictCompression dcc = DictCompression.getInstance();
				
				col_SQLText_name          = dcc.getDigestSourceColumnName(col_SQLText_name);
				col_NormSQLText_name      = dcc.getDigestSourceColumnName(col_NormSQLText_name);
				col_PlanText_name         = dcc.getDigestSourceColumnName(col_PlanText_name);
				col_BlockedBySqlText_name = dcc.getDigestSourceColumnName(col_BlockedBySqlText_name);
			}
			
			sbSql.append("insert into ").append(tabName);
			sbSql.append("(");
			sbSql.append(" ").append(lq).append("sampleTime"             ).append(rq);  //  1
			sbSql.append(",").append(lq).append("InstanceID"             ).append(rq);  //  2
			sbSql.append(",").append(lq).append("SPID"                   ).append(rq);  //  3
			sbSql.append(",").append(lq).append("KPID"                   ).append(rq);  //  4
			sbSql.append(",").append(lq).append("DBID"                   ).append(rq);  //  5
			sbSql.append(",").append(lq).append("ProcedureID"            ).append(rq);  //  6
			sbSql.append(",").append(lq).append("PlanID"                 ).append(rq);  //  7
			sbSql.append(",").append(lq).append("BatchID"                ).append(rq);  //  8
			sbSql.append(",").append(lq).append("ContextID"              ).append(rq);  //  9
			sbSql.append(",").append(lq).append("LineNumber"             ).append(rq);  // 10
			sbSql.append(",").append(lq).append("ObjOwnerID"             ).append(rq);  // 11
			sbSql.append(",").append(lq).append("DBName"                 ).append(rq);  // 12
			sbSql.append(",").append(lq).append("HashKey"                ).append(rq);  // 13
			sbSql.append(",").append(lq).append("SsqlId"                 ).append(rq);  // 14
			sbSql.append(",").append(lq).append("ProcName"               ).append(rq);  // 15
			sbSql.append(",").append(lq).append("Elapsed_ms"             ).append(rq);  // 16
			sbSql.append(",").append(lq).append("CpuTime"                ).append(rq);  // 17
			sbSql.append(",").append(lq).append("WaitTime"               ).append(rq);  // 18
			sbSql.append(",").append(lq).append("MemUsageKB"             ).append(rq);  // 19
			sbSql.append(",").append(lq).append("PhysicalReads"          ).append(rq);  // 20
			sbSql.append(",").append(lq).append("LogicalReads"           ).append(rq);  // 21
			sbSql.append(",").append(lq).append("RowsAffected"           ).append(rq);  // 22
			sbSql.append(",").append(lq).append("ErrorStatus"            ).append(rq);  // 23
			sbSql.append(",").append(lq).append("ProcNestLevel"          ).append(rq);  // 24
			sbSql.append(",").append(lq).append("StatementNumber"        ).append(rq);  // 25
			sbSql.append(",").append(lq).append("QueryOptimizationTime"  ).append(rq);  // 26
			sbSql.append(",").append(lq).append("PagesModified"          ).append(rq);  // 27
			sbSql.append(",").append(lq).append("PacketsSent"            ).append(rq);  // 28
			sbSql.append(",").append(lq).append("PacketsReceived"        ).append(rq);  // 29
			sbSql.append(",").append(lq).append("NetworkPacketSize"      ).append(rq);  // 30
			sbSql.append(",").append(lq).append("PlansAltered"           ).append(rq);  // 31
			sbSql.append(",").append(lq).append("StartTime"              ).append(rq);  // 32
			sbSql.append(",").append(lq).append("EndTime"                ).append(rq);  // 33
                                                                         
			sbSql.append(",").append(lq).append("ServerLogin"            ).append(rq);  // 34;
			sbSql.append(",").append(lq).append("AddStatus"              ).append(rq);  // 35;
			sbSql.append(",").append(lq).append("JavaSqlLength"          ).append(rq);  // 36;
			sbSql.append(",").append(lq).append("JavaSqlLengthShort"     ).append(rq);  // 37;
			sbSql.append(",").append(lq).append("NormJavaSqlLength"      ).append(rq);  // 38;
			sbSql.append(",").append(lq).append("JavaSqlHashCode"        ).append(rq);  // 39;
			sbSql.append(",").append(lq).append("JavaSqlHashCodeShort"   ).append(rq);  // 40;
			sbSql.append(",").append(lq).append("NormJavaSqlHashCode"    ).append(rq);  // 41;
                                                                         
			sbSql.append(",").append(lq).append(col_SQLText_name         ).append(rq);  // 42;
			sbSql.append(",").append(lq).append(col_NormSQLText_name     ).append(rq);  // 43;
			sbSql.append(",").append(lq).append(col_PlanText_name        ).append(rq);  // 44;
			sbSql.append(",").append(lq).append(col_BlockedBySqlText_name).append(rq);  // 45;
                                                                         
			sbSql.append(",").append(lq).append("WaitTimeDetails"        ).append(rq);  // 46;
			sbSql.append(",").append(lq).append("BlockedBySpid"          ).append(rq);  // 47;
			sbSql.append(",").append(lq).append("BlockedByKpid"          ).append(rq);  // 48;
			sbSql.append(",").append(lq).append("BlockedByBatchId"       ).append(rq);  // 49;
			sbSql.append(",").append(lq).append("BlockedByCommand"       ).append(rq);  // 50;
			sbSql.append(",").append(lq).append("BlockedByApplication"   ).append(rq);  // 51;
			sbSql.append(",").append(lq).append("BlockedByTranId"        ).append(rq);  // 52;

			sbSql.append(") \n");
			sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n"); // 52 question marks
			//                   1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52

			return sbSql.toString();
		}

//		if (MON_SQL_PLAN.equals(tabName))
//		{
//			StringBuilder sbSql = new StringBuilder();
//
//			sbSql.append("insert into ").append(tabName);
//			sbSql.append("(");
//			sbSql.append(" ").append(lq).append("sampleTime" ).append(rq); // 1
//			sbSql.append(",").append(lq).append("InstanceID" ).append(rq); // 2
//			sbSql.append(",").append(lq).append("SPID"       ).append(rq); // 3
//			sbSql.append(",").append(lq).append("KPID"       ).append(rq); // 4
//			sbSql.append(",").append(lq).append("PlanID"     ).append(rq); // 5
//			sbSql.append(",").append(lq).append("BatchID"    ).append(rq); // 6
//			sbSql.append(",").append(lq).append("ContextID"  ).append(rq); // 7
//			sbSql.append(",").append(lq).append("DBID"       ).append(rq); // 8
//			sbSql.append(",").append(lq).append("DBName"     ).append(rq); // 9
//			sbSql.append(",").append(lq).append("ProcedureID").append(rq); // 10
//			sbSql.append(",").append(lq).append("AddMethod"  ).append(rq); // 11
//			sbSql.append(",").append(lq).append("PlanText"   ).append(rq); // 12
//			sbSql.append(") \n");
//			sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n"); // 12 question marks
//			//                   1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12
//
//			return sbSql.toString();
//		}

		if (MON_CAP_SPID_INFO.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();

			sbSql.append("insert into ").append(tabName);
			sbSql.append("(");
			sbSql.append(" ").append(lq).append("sampleTime"         ).append(rq); // 1
			sbSql.append(",").append(lq).append("SPID"               ).append(rq); // 2
			sbSql.append(",").append(lq).append("KPID"               ).append(rq); // 3
			sbSql.append(",").append(lq).append("BatchID"            ).append(rq); // 4
			sbSql.append(",").append(lq).append("ContextID"          ).append(rq); // 5
			sbSql.append(",").append(lq).append("LineNumber"         ).append(rq); // 6
			sbSql.append(",").append(lq).append("SecondsConnected"   ).append(rq); // 7
			sbSql.append(",").append(lq).append("Command"            ).append(rq); // 8
			sbSql.append(",").append(lq).append("SecondsWaiting"     ).append(rq); // 9
			sbSql.append(",").append(lq).append("WaitEventID"        ).append(rq); // 10
			sbSql.append(",").append(lq).append("BlockingSPID"       ).append(rq); // 11
			sbSql.append(",").append(lq).append("BlockingKPID"       ).append(rq); // 12
			sbSql.append(",").append(lq).append("BlockingBatchID"    ).append(rq); // 13
			sbSql.append(",").append(lq).append("BlockingXLOID"      ).append(rq); // 14
			sbSql.append(",").append(lq).append("NumChildren"        ).append(rq); // 15
			sbSql.append(",").append(lq).append("Login"              ).append(rq); // 16
			sbSql.append(",").append(lq).append("DBName"             ).append(rq); // 17
			sbSql.append(",").append(lq).append("Application"        ).append(rq); // 18
			sbSql.append(",").append(lq).append("HostName"           ).append(rq); // 19
			sbSql.append(",").append(lq).append("MasterTransactionID").append(rq); // 20
			sbSql.append(",").append(lq).append("snapWaitTimeDetails").append(rq); // 21
			sbSql.append(",").append(lq).append("SqlText"            ).append(rq); // 22
			sbSql.append(",").append(lq).append("BlockingSqlText"    ).append(rq); // 23
			sbSql.append(") \n");
			sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n"); // 21 question marks
			//                   1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23

			return sbSql.toString();
		}

		if (MON_CAP_WAIT_INFO.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();

			sbSql.append("insert into ").append(tabName);
			sbSql.append("(");
			sbSql.append(" ").append(lq).append("sampleTime"         ).append(rq); // 1
			sbSql.append(",").append(lq).append("SPID"               ).append(rq); // 2
			sbSql.append(",").append(lq).append("KPID"               ).append(rq); // 3
			sbSql.append(",").append(lq).append("WaitEventID"        ).append(rq); // 4
			sbSql.append(",").append(lq).append("WaitClassID"        ).append(rq); // 5
			sbSql.append(",").append(lq).append("snapshotBatchID"    ).append(rq); // 6
			sbSql.append(",").append(lq).append("Waits_abs"          ).append(rq); // 7
			sbSql.append(",").append(lq).append("Waits_diff"         ).append(rq); // 8
			sbSql.append(",").append(lq).append("WaitTime_abs"       ).append(rq); // 9
			sbSql.append(",").append(lq).append("WaitTime_diff"      ).append(rq); // 10
			sbSql.append(") \n");
			sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n"); // 10 question marks
			//                   1, 2, 3, 4, 5, 6, 7, 8, 9,10

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

			// *sq dynamic SQL (ct_dynamic/prepared_stmnt) does NOT set the SsqlId column
//			ProcName        =     "ProcName = CASE WHEN SsqlId > 0 THEN object_name(SsqlId,2) \n" +
//			                  "                    ELSE isnull(isnull(object_name(ProcedureID,DBID),object_name(ProcedureID,2)),object_name(ProcedureID,db_id('sybsystemprocs'))) \n" + 
//			                  "               END, ";
			ProcName        =     "ProcName = CASE WHEN SsqlId > 0 THEN isnull(object_name(SsqlId,2), '*##'+right('0000000000'+convert(varchar(20),SsqlId),10)+'_'+right('0000000000'+convert(varchar(20),HashKey),10)+'##*') \n" +
			                  "                    ELSE coalesce(object_name(ProcedureID,DBID), object_name(ProcedureID,2), object_name(ProcedureID,db_id('sybsystemprocs'))) \n " +
			                  "               END, ";
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
		
		// Set any SQL-Prefix if desirable
		String sqlPrefix = ""; 
		if (useSqlPrefix())
		{
			sqlPrefix = getSqlPrefix(); 
		}

		_sql_sqlText 
			= sqlPrefix
			+ "select getdate() as sampleTime, \n"
			+ "    "+InstanceID+"\n"
			+ "    SPID, \n"
			+ "    KPID, \n"
			+ "    BatchID, \n"
			+ "    SequenceInBatch, \n"
			+ "    "+ServerLogin+"\n"
//			+ "    convert(int, -1) as AddMethod,\n"
//			+ "    convert(int, -1) as JavaSqlLength,\n"
//			+ "    convert(int, -1) as JavaSqlLengthShort,\n"
//			+ "    convert(int, -1) as JavaSqlHashCode,\n"
//			+ "    convert(int, -1) as JavaSqlHashCodeShort,\n"
			+ "    SQLText \n"
			+ "from master.dbo.monSysSQLText \n"
			+ "where 1 = 1 \n"
			+ "  and SPID != @@spid \n"
			+ "  and SPID not in (select spid from master.dbo.sysprocesses where program_name like '" + Version.getAppName() + "%') \n"
//			+ "order by SPID, KPID, BatchID, SequenceInBatch \n" // TODO: make this sort internal/after the rows has been fetched for less server side impact
			+ "";

		_sql_sqlStatements
			= sqlPrefix
			+ "select \n"
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

			+ "    convert(varchar(10), NULL) as ServerLogin, \n"       // Length of the char is NOT important
			+ "    convert(int,         -1  ) as AddStatus, \n"
			+ "    convert(int,         -1  ) as JavaSqlLength, \n"
			+ "    convert(int,         -1  ) as JavaSqlLengthShort, \n"
			+ "    convert(int,         -1  ) as NormJavaSqlLength, \n"
			+ "    convert(int,         -1  ) as JavaSqlHashCode, \n"
			+ "    convert(int,         -1  ) as JavaSqlHashCodeShort, \n"
			+ "    convert(int,         -1  ) as NormJavaSqlHashCode, \n"

			+ "    convert(varchar(10), NULL) as SQLText, \n"           // Length of the char is NOT important
			+ "    convert(varchar(10), NULL) as NormSQLText, \n"       // Length of the char is NOT important
			+ "    convert(varchar(10), NULL) as PlanText, \n"           // Length of the char is NOT important
			+ "    convert(varchar(10), NULL) as BlockedBySqlText \n"   // Length of the char is NOT important
			
			+ "from master.dbo.monSysStatement \n"
			+ "where 1 = 1 \n"
			+ "  and SPID != @@spid \n"
			+ "  and SPID not in (select spid from master.dbo.sysprocesses where program_name like '" + Version.getAppName() + "%') \n"
			+ "  and " + statementWhereClause
			+ "";
		
		_sql_sqlPlanText 
			= sqlPrefix
			+ "select getdate() as sampleTime, \n"
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
//			+ "    convert(int, -1) as AddMethod,\n"
			+ "    PlanText \n"
			+ "from master.dbo.monSysPlanText \n"
			+ "where 1 = 1 \n"
			+ "  and SPID != @@spid \n"
			+ "  and SPID not in (select spid from master.dbo.sysprocesses where program_name like '" + Version.getAppName() + "%') \n"
//			+ "order by SPID, KPID, BatchID, SequenceNumber \n" // TODO: make this sort internal/after the rows has been fetched for less server side impact
			+ "";

		// get information about (excluding your own SPID and System SPID's)
		// * All SPID's that are BLOCKED
		// * Or NOT waiting for 'clients to send new data' (WaitEventID: 250 -> waiting for incoming network data)
		String HostName = "    ,HostName='-not-available-'";
		if (srvVersion >= Ver.ver(15,7))
		{
			HostName          = "    ,p.HostName \n";
		}

		_sql_spidInfo = ""
			+ "select \n"
			+ "     sampleTime=getdate() \n"
			+ "    ,p.SPID \n"
			+ "    ,p.KPID \n"
			+ "    ,p.BatchID \n"
			+ "    ,p.ContextID \n"
			+ "    ,p.LineNumber \n"
			+ "    ,p.SecondsConnected \n"
			+ "    ,p.Command \n"
			+ "    ,p.SecondsWaiting \n"
			+ "    ,p.WaitEventID \n"
			+ "    ,p.BlockingSPID \n"
//			+ "    ,BlockingKPID    = i.KPID \n"
//			+ "    ,BlockingBatchID = i.BatchID \n"
			+ "    ,BlockingKPID    = null \n"
			+ "    ,BlockingBatchID = null \n"
			+ "    ,p.BlockingXLOID \n"
			+ "    ,p.NumChildren \n"
			+ "    ,p.Login \n"
			+ "    ,p.DBName \n"
			+ "    ,p.Application \n"
			+      HostName
			+ "    ,p.MasterTransactionID \n"
			+ "from master.dbo.monProcess p \n"
//			+ "left outer join master.dbo.monProcess i on p.BlockingSPID = i.SPID \n" // instead of the self join, do this locally (Map lookup)
			+ "where 1=1 \n"
			+ "  and p.SPID != @@spid \n"
			+ "  and p.Login is not null \n"
			+ "  and p.Login != 'probe' \n"
			+ "  and (p.BlockingSPID is not null or p.WaitEventID != 250) \n"
			+ "   or (p.WaitEventID = 250 and p.SPID in (select spid from master.dbo.syslocks)) \n"
			+ "";

		
//		_sql_waitInfo = ""
//		    + "select sampleTime=getdate(), SPID, KPID, WaitEventID, Waits, WaitTime \n"
//		    + "from master.dbo.monProcessWaits w \n"
//		    + "where 1=1 \n"
////		    + "  and SPID = ${SPID} \n"
////		    + "  and KPID = ${KPID} \n"
//		    + "";
		
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

//	/**
//	 * Primary Key for SqlText, Statements, PlanText
//	 * @author gorans
//	 */
//	protected class PK
//	{
//		int SPID;
//		int KPID;
//		int BatchID;
//		
//		public PK(int SPID, int KPID, int BatchID)
//		{
//			this.SPID    = SPID;
//			this.KPID    = KPID;
//			this.BatchID = BatchID;
//		}
//
//		// Genereated from Eclipse
//		@Override
//		public int hashCode()
//		{
//			final int prime = 31;
//			int result = 1;
////			result = prime * result + getOuterType().hashCode();
//			result = prime * result + BatchID;
//			result = prime * result + KPID;
//			result = prime * result + SPID;
//			return result;
//		}
//
//		// Genereated from Eclipse
//		@Override
//		public boolean equals(Object obj)
//		{
//			if ( this == obj )
//				return true;
//			if ( obj == null )
//				return false;
//			if ( getClass() != obj.getClass() )
//				return false;
//			PK other = (PK) obj;
////			if ( !getOuterType().equals(other.getOuterType()) )
////				return false;
////			if ( BatchID != other.BatchID )
////				return false;
////			if ( SPID != other.SPID )
////				return false;
////			return true;
//			
//			return SPID == other.SPID && KPID == other.KPID && BatchID == other.BatchID; 
//		}
//
////		// Genereated from Eclipse
////		private SqlCaptureBrokerAse getOuterType()
////		{
////			return SqlCaptureBrokerAse.this;
////		}
//		
//		@Override
//		public String toString()
//		{
//			return "PK [SPID=" + SPID + ", KPID=" + KPID +", BatchID=" + BatchID + "]" + "@" + Integer.toHexString(hashCode());
//		}
//
//		public boolean equals(int spid, int kpid, int batchId)
//		{
//			return this.SPID == spid && this.KPID == kpid && this.BatchID == batchId;
//		}
//	}

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
	 * which means that we end up storing some <i>extra</i> SQL-Text, due to the fact that no Statement (filtering) information was available
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
	 * In the "post" phase we will also save the SQL-Text information for X number of calls to doSqlCapture(), this is kept in a "Deferred SQL Queue".<br>
	 * The "Deferred Queue" is now the SpidSqlTextAndPlanManager where "last" SqlText and SqlPlan are kept (and removed in the post phase)<br>
	 * <br>
	 * Then we will enter the "post" phase where the records are sent to the PCS - Persistent Counter Storage
	 * <ul>
	 *   <li>For Statements that are above thresholds: Get SqlText & PlanText stored in SpidSqlTextAndPlanManager</li>
	 *   <li>Send info to the PCS</li>
	 *   <li>Remove any SqlText & PlanText information in the <i>Deferred SQL Queue</i>, call "endOfScan()" which does the following
	 *       <ul>
	 *           <li>Loop all SPID's in the SpidSqlTextAndPlanManager</li>  
	 *           <li>If the SPID has a new KPID, then reset this entry (it's a new KPID, hence a new login which reused the SPID number)... or if this is done in the "add" step...</li>
	 *           <li>Remove all SqlText & SqlPlan entries with a lower BatchId than the "maxBatchId" - Meaning: keep only the <b>last</b> entries.</li>  
	 *       </ul>  
	 *   </li>
	 *   <li>Every 5 minute (configurable), get all SPID's from ASE, remove SPID's from SpidSqlTextAndPlanManager that are no longer present in ASE (they have logged out).</li>
	 * </ul>
	 * 
	 */
	@Override
	public int doSqlCapture(DbxConnection conn, PersistentCounterHandler pch)
	{
		boolean sampleSpidInfo = getConfiguration().getBooleanProperty(PROPKEY_sqlCap_ase_spidInfo_sample,     DEFAULT_sqlCap_ase_spidInfo_sample);
		boolean sampleWaitInfo = getConfiguration().getBooleanProperty(PROPKEY_sqlCap_ase_spidWaitInfo_sample, DEFAULT_sqlCap_ase_spidWaitInfo_sample);

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

		List<MonSqlStatement> statementRecords = new ArrayList<>();
		
		// If SQL queries are NOT initialized, do it now
		if (_sql_sqlText == null)
			setSql(conn);

		// Just a "timestamp" so we know *when* we last updated this
		// if the "Capture thread" dies we can probably use this value to detect the "problem"
		if (_statementStatistics != null)
			_statementStatistics.setLastUpdateTime();
				
		//------------------------------------------------
		// SPID INFO
		// - get SPID information on spids that are "doing something"
		//------------------------------------------------
		if (sampleSpidInfo)
		{
			Map<Integer, SpidInfoBatchIdEntry> spidInfoJustAddedEntries  = new HashMap<>();
			List<WaitEventEntry> addedWaitEventEntries = new ArrayList<>();
			
			long captureStartTime = System.currentTimeMillis();
			try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(_sql_spidInfo) )
			{
				// Only keep the last ACTIVE SPID's in a Map
//				_spidInfo.clear();

				while(rs.next())
				{
					SpidInfoBatchIdEntry entry = new SpidInfoBatchIdEntry();
					int c = 1;

					entry._sampleTime          = rs.getTimestamp(c++); // java.sql.Types.TIMESTAMP datetime          -none-               
					entry._SPID                = rs.getInt      (c++); // java.sql.Types.INTEGER   int               master.dbo.monProcess
					entry._KPID                = rs.getInt      (c++); // java.sql.Types.INTEGER   int               master.dbo.monProcess
					entry._BatchID             = rs.getInt      (c++); // java.sql.Types.INTEGER   int               master.dbo.monProcess
					entry._ContextID           = rs.getInt      (c++); // java.sql.Types.INTEGER   int               master.dbo.monProcess
					entry._LineNumber          = rs.getInt      (c++); // java.sql.Types.INTEGER   int               master.dbo.monProcess
					entry._SecondsConnected    = rs.getInt      (c++); // java.sql.Types.INTEGER   int               master.dbo.monProcess
					entry._Command             = rs.getString   (c++); // java.sql.Types.VARCHAR   varchar(30)       master.dbo.monProcess
					entry._SecondsWaiting      = rs.getInt      (c++); // java.sql.Types.INTEGER   int               master.dbo.monProcess
					entry._WaitEventID         = rs.getInt      (c++); // java.sql.Types.SMALLINT  smallint          master.dbo.monProcess
					entry._BlockingSPID        = rs.getInt      (c++); // java.sql.Types.INTEGER   int               master.dbo.monProcess
					entry._BlockingKPID        = rs.getInt      (c++); // java.sql.Types.INTEGER   int
					entry._BlockingBatchID     = rs.getInt      (c++); // java.sql.Types.INTEGER   int
					entry._BlockingXLOID       = rs.getInt      (c++); // java.sql.Types.INTEGER   int               master.dbo.monProcess
					entry._NumChildren         = rs.getInt      (c++); // java.sql.Types.INTEGER   int               master.dbo.monProcess
					entry._Login               = rs.getString   (c++); // java.sql.Types.VARCHAR   varchar(30)       master.dbo.monProcess
					entry._DBName              = rs.getString   (c++); // java.sql.Types.VARCHAR   varchar(30)       master.dbo.monProcess
					entry._Application         = rs.getString   (c++); // java.sql.Types.VARCHAR   varchar(30)       master.dbo.monProcess
					entry._HostName            = rs.getString   (c++); // java.sql.Types.VARCHAR   varchar(30)       master.dbo.monProcess
					entry._MasterTransactionID = rs.getString   (c++); // java.sql.Types.VARCHAR   varchar(255)      master.dbo.monProcess		

					spidInfoJustAddedEntries.put(entry._SPID, entry);

					_spidInfoManager.addSpidInfoBatchIdEntry(entry);
				}
				
				// Some statistics
				long captureTime = TimeUtils.msDiffNow(captureStartTime);
				_statSpidInfoCaptureTime     += captureTime;
				_statSpidInfoCaptureTimeDiff += captureTime;

				_statSpidInfoCaptureRows       += spidInfoJustAddedEntries.size();
				_statSpidInfoCaptureRowsDiff   += spidInfoJustAddedEntries.size();
			}
			catch(SQLException ex)
			{
				_logger.error("SQL Capture problems when capturing 'SPID Information' Caught "+AseConnectionUtils.sqlExceptionToString(ex) + " when executing SQL: "+_sql_spidInfo, ex);
			}
			//if (! justAddedEntries.isEmpty() )
			//{
			//	System.out.println();
			//	for (SpidInfoBatchIdEntry entry : justAddedEntries.values())
			//		System.out.println("                  just read: SpidInfoBatchIdEntry: " + entry);
			//}
			//_spidInfoManager.getBatchIdSize();

			
			// Get blocking SPID SqlText
			if ( ! spidInfoJustAddedEntries.isEmpty() )
			{
				//String sql = "select SPID, KPID, BatchID, LineNumber, SequenceInLine, SQLText from master.dbo.monProcessSQLText where SPID != @@spid";					String sql = "select SPID, KPID, BatchID, LineNumber, SequenceInLine, SQLText from master.dbo.monProcessSQLText where SPID != @@spid";				
				String sql = "select SPID, LineNumber, SQLText from master.dbo.monProcessSQLText where SPID != @@spid";				
				try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql) )
				{
					int lastSpid       = Integer.MIN_VALUE;
					int lastLineNumber = 1;
					String preStr      = "";
					while(rs.next())
					{
						// NOTE 1: this do not work to 100% ... LineNumber, SequenceInLine probably need to be used to get correct NEW-LINE etc...
						// NOTE 2: If a blocking lock SPID is "at client side" the blocking SQL Text wont be available
						int    SPID       = rs.getInt   (1);
						int    LineNumber = rs.getInt   (2);
						String SQLText    = rs.getString(3);

						if (lastSpid != SPID)
							lastLineNumber = 1; // When a new SPID is seen... restart at line 1

						preStr = "";
						if (lastLineNumber != LineNumber)
							preStr = "\n";

						SpidInfoBatchIdEntry spidInfoBatchIdEntry = spidInfoJustAddedEntries.get(SPID);
						if (spidInfoBatchIdEntry != null)
						{
							if (spidInfoBatchIdEntry._SqlText == null)
								spidInfoBatchIdEntry._SqlText = "";

							spidInfoBatchIdEntry._SqlText += preStr + SQLText;
						}

						lastLineNumber = LineNumber;
						lastSpid       = SPID;
					}
				}
				catch(SQLException ex)
				{
					_logger.error("SQL Capture problems when capturing 'SPID SqlText' Caught "+AseConnectionUtils.sqlExceptionToString(ex) + " when executing SQL: "+sql, ex);
				}

				// Loop the record to update "BlockingKPID, BlockingBatchID, BlockingSqlText" (BlockingSPID is already known)
				for (SpidInfoBatchIdEntry e : spidInfoJustAddedEntries.values())
				{
					if (e._BlockingSPID != 0)
					{
						SpidInfoBatchIdEntry blockingSpidEntry = spidInfoJustAddedEntries.get(e._BlockingSPID);

						if (blockingSpidEntry != null)
						{
							e._BlockingSPID    = blockingSpidEntry._SPID;  // well this is already set... just for clarity
							e._BlockingKPID    = blockingSpidEntry._KPID;
							e._BlockingBatchID = blockingSpidEntry._BatchID;
							e._BlockingSqlText = blockingSpidEntry._SqlText;
						}
					}
				}
//System.out.println();
//System.out.println("##########################################################");
//for (SpidInfoBatchIdEntry e : spidInfoJustAddedEntries.values())
//	System.out.println("  >>> SQL-TEXT:          SPID="+e._SPID+", BlockingSPID="+e._BlockingSPID+", SqlText=|"+e._SqlText+"|.");
//
//for (SpidInfoBatchIdEntry e : spidInfoJustAddedEntries.values())
//	System.out.println("  >>> BLOCKING-SQL-TEXT: SPID="+e._SPID+", BlockingSPID="+e._BlockingSPID+", BlockingSqlText=|"+e._BlockingSqlText+"|.");
			}

			//------------------------------------------------
			// WAIT INFO
			// - on the active SPID's get what they have been waiting for
			//------------------------------------------------
			if (sampleWaitInfo && !spidInfoJustAddedEntries.isEmpty())
			{
				StringBuilder sb = new StringBuilder(100 + spidInfoJustAddedEntries.size() * 10);

				// Build SQL
				sb.append("select sampleTime=getdate(), SPID, KPID, WaitEventID, Waits, WaitTime \n");
				sb.append("from master.dbo.monProcessWaits w \n");
				sb.append("where SPID in(");
				// the in(...) list
				String comma = "";
				for (int spid : spidInfoJustAddedEntries.keySet())
				{
					sb.append(comma).append(spid);
					comma = ", ";
				}
				sb.append(")");
				String sql = sb.toString();

				captureStartTime = System.currentTimeMillis();
				// Execute SQL
				try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql) )
				{
					while(rs.next())
					{
						//WaitEventEntry entry = new WaitEventEntry();
						int c = 1;

						Timestamp sampleTime   = rs.getTimestamp(c++);   // RS> 1    sampleTime  java.sql.Types.TIMESTAMP datetime          -none-                    
						int       spid         = rs.getInt      (c++);   // RS> 2    SPID        java.sql.Types.INTEGER   int               master.dbo.monProcessWaits
						int       kpid         = rs.getInt      (c++);   // RS> 3    KPID        java.sql.Types.INTEGER   int               master.dbo.monProcessWaits
						int       waitEventId  = rs.getInt      (c++);   // RS> 4    WaitEventID java.sql.Types.SMALLINT  smallint          master.dbo.monProcessWaits
						int       waits_abs    = rs.getInt      (c++);   // RS> 5    Waits       java.sql.Types.INTEGER   int               master.dbo.monProcessWaits
						int       waitTime_abs = rs.getInt      (c++);   // RS> 6    WaitTime    java.sql.Types.INTEGER   int               master.dbo.monProcessWaits
						
						int       snapshotBatchId  = spidInfoJustAddedEntries.get(spid)._BatchID;
						int       secondsConnected = spidInfoJustAddedEntries.get(spid)._SecondsConnected;

						// Create a WaitEvent
						WaitEventEntry entry = new WaitEventEntry(sampleTime, spid, kpid, waitEventId, waits_abs, waitTime_abs, snapshotBatchId, secondsConnected);
						
						//System.out.println("ADD-WaitEventEntry: [intWaitEventId="+intWaitEventId+", shortWaitEventId="+shortWaitEventId+"]" + entry);
						//System.out.println(">>>> ADD-WaitEventEntry: " + entry);
						_waitInfo.addOrUpdate(entry);
						
						addedWaitEventEntries.add(entry);
					}
				}
				catch(SQLException ex)
				{
					_logger.error("SQL Capture problems when capturing 'SPID WAIT Information' Caught "+AseConnectionUtils.sqlExceptionToString(ex) + " when executing SQL: "+sql, ex);
				}
				
				// if we want to persist the "active" SPID's then we might want to add "current known Wait Information" to that entry
				for (SpidInfoBatchIdEntry e : spidInfoJustAddedEntries.values())
				{
					e._snapWaitTimeDetails = _waitInfo.getWaitTimeDetailsAsJson(false, e._SPID, e._KPID, e._BatchID);
				}

				// Some statistics
				long captureTime = TimeUtils.msDiffNow(captureStartTime);
				_statWaitInfoCaptureTime     += captureTime;
				_statWaitInfoCaptureTimeDiff += captureTime;

				_statWaitInfoCaptureRows       += addedWaitEventEntries.size();
				_statWaitInfoCaptureRowsDiff   += addedWaitEventEntries.size();
			}
			

			// Send the information for storage to PCS
			if ( !spidInfoJustAddedEntries.isEmpty() || !addedWaitEventEntries.isEmpty() )
			{
				// Container object used to send data to PCS
				SqlCaptureDetails capDet = new SqlCaptureDetails();

				// add SPID INFO records
				for (SpidInfoBatchIdEntry e : spidInfoJustAddedEntries.values())
				{
					capDet.add(e.getPcsRow());
				}

				// add WAIT EVENT records
				for (WaitEventEntry e : addedWaitEventEntries)
				{
					// Get the record from _waitInfo, because the passed entry in '_waitInfo.addOrUpdate(entry)' *will* be updated
					WaitEventEntry waitEventId = _waitInfo.getWaitEventId(e._SPID, e._KPID, e._WaitEventID);
					if (waitEventId != null)
						capDet.add(waitEventId.getPcsRow());
				}

				// add/send to PCS
				addToPcs(pch, capDet);
			}
		}

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
			
			long captureStartTime = System.currentTimeMillis();
			try
			{
				Statement    stmnt    = conn.createStatement();
				ResultSet    rs       = stmnt.executeQuery(_sql_sqlStatements);
				List<String> colNames = DbUtils.getColumnNames(rs.getMetaData());
				int          rowCount = 0;
				
				// used for DynamicSQL (where ProcName like '*sq%' and HashKey is 0)
//				int pos_ProcedureID = colNames.indexOf("ProcedureID");
//				int pos_SsqlId      = colNames.indexOf("SsqlId");
//				int pos_HashKey     = colNames.indexOf("HashKey");
//				if (pos_SsqlId  != -1) pos_SsqlId++;  // +1 to adjust for 'TableName' in the output storage array  
//				if (pos_HashKey != -1) pos_HashKey++; // +1 to adjust for 'TableName' in the output storage array

				int pos_SPID          = findInListToJdbcPos(colNames, "SPID");
//				int pos_KPID          = findInListToJdbcPos(colNames, "KPID");
//				int pos_BatchID       = findInListToJdbcPos(colNames, "BatchID");
				int pos_Elapsed_ms    = findInListToJdbcPos(colNames, "Elapsed_ms");
				int pos_LogicalReads  = findInListToJdbcPos(colNames, "LogicalReads");
				int pos_PhysicalReads = findInListToJdbcPos(colNames, "PhysicalReads");
				int pos_ErrorStatus   = findInListToJdbcPos(colNames, "ErrorStatus");
				int pos_CpuTime       = findInListToJdbcPos(colNames, "CpuTime");
				int pos_WaitTime      = findInListToJdbcPos(colNames, "WaitTime");
				int pos_RowsAffected  = findInListToJdbcPos(colNames, "RowsAffected");
				int pos_ProcedureID   = findInListToJdbcPos(colNames, "ProcedureID");
//				int pos_SsqlId        = findInListToJdbcPos(colNames, "SsqlId");
//				int pos_ContextID     = findInListToJdbcPos(colNames, "ContextID");
				int pos_ProcName      = findInListToJdbcPos(colNames, "ProcName");
				int pos_LineNumber    = findInListToJdbcPos(colNames, "LineNumber");
				int pos_DBName        = findInListToJdbcPos(colNames, "DBName");
				
				while(rs.next())
				{
					rowCount++;
//					statementReadCount++;

					int physicalReads = -1;
					
					int SPID          = rs.getInt(pos_SPID);
//					int KPID          = rs.getInt(pos_KPID);
//					int BatchID       = rs.getInt(pos_BatchID);
					int execTime      = rs.getInt(pos_Elapsed_ms);
					int logicalReads  = rs.getInt(pos_LogicalReads);
//					int physicalReads = rs.getInt(pos_PhysicalReads); // throws: java.sql.SQLException: JZ00B: Numeric overflow. sometimes
					try { physicalReads = rs.getInt(pos_PhysicalReads); } catch (SQLException ex) { _logger.warn("Problems reading column 'PhysicalReads', strVal='" + rs.getString(pos_PhysicalReads) + "', setting this to -1 and continuing. Caught: " + ex); }
					int errorStatus   = rs.getInt(pos_ErrorStatus);

					int cpuTime       = rs.getInt(pos_CpuTime);
					int waitTime      = rs.getInt(pos_WaitTime);
					int rowsAffected  = rs.getInt(pos_RowsAffected); // RowsAffected was introduced in ASE 12.5.4 (but before that we genereate -1) 

					int procedureId   = rs.getInt(pos_ProcedureID);
//					int ssqlId        = rs.getInt(pos_SsqlId);
//					int ContextID     = rs.getInt(pos_ContextID);

					String procName   = rs.getString(pos_ProcName);
					int    lineNumber = rs.getInt   (pos_LineNumber);
					
					String DBName     = rs.getString(pos_DBName);


					// To be used by keep/discard Set
//					PK pk = new PK(SPID, KPID, BatchID);

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
						updateStatementStats(execTime, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, procedureId, procName, lineNumber, DBName);

					
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
							// send ProcName or StatementCacheEntry to the DDL Capture
							if (StringUtil.hasValue(procName))
							{
//								String DBName = rs.getString(pos_DBName);

								// Only send if it's above the defined limits
								if (    execTime      > sendDdlForLookup_gt_execTime
								     && logicalReads  > sendDdlForLookup_gt_logicalReads
								     && physicalReads > sendDdlForLookup_gt_physicalReads
								   )
								{
									// if it's a statement cache entry, populate it into the cache so that the DDL Lookup wont have to sleep 
									if (procName.startsWith("*ss") || procName.startsWith("*sq") ) // *sq in ASE 15.7 esd#2, DynamicSQL can/will end up in statement cache
									{
										if (XmlPlanCache.hasInstance())
										{
											XmlPlanCache xmlPlanCache = XmlPlanCache.getInstance();
											if ( ! xmlPlanCache.isPlanCached(procName) )
											{
												// Only bring it into the cache. The value is of no interest here
												xmlPlanCache.getPlan(procName);
											}
										}
										else
										{
											_logger.info("XmlPlanCache do not have an instance. Skipping XML Plan lookup for name '"+procName+"'.");
										}
									}
									
									// Now add the ProcName/StatementCacheEntryName to the DDL Lookup handler
									if (pch != null)
										pch.addDdl(DBName, procName, "SqlCapture");
								}
							}
						} //end: sendDdlForLookup

						// Create a object from the ResultSet 
						// The entry will be added to the List: statementRecords
						MonSqlStatement row = new MonSqlStatement(rs);

						// if it's a DynamicSQL (from CT-Lib) the SSQLID and HashKey are not set... 
						// so we can set those from the object name '*sqXXXXXXXXXX_YYYYYYYYYYss*' where XXX=SSQLID and YYY=HashKey
						//                                           0123456789|123456789|123456
						if (StringUtil.hasValue(procName) && procName.startsWith("*"))
						{
							int SsqlId  = row.SsqlId; 
							int HashKey = row.HashKey;
							
							if (SsqlId == 0 || HashKey == 0)
							{
								String str_SsqlId  = procName.substring(3,  13);
								String str_HashKey = procName.substring(14, 24);;

								try
								{
									row.SsqlId  = new Integer(str_SsqlId);
									row.HashKey = new Integer(str_HashKey);

//System.out.println("Found a row with 'ProcName' that starts with '*' (a DynamicSQL or Statement Cache Entry) which had a zero in 'SsqlId' or 'HashKey'. ProcName='" + procName + "', SsqlId=" + SsqlId + ", HashKey=" + HashKey + ". New=[SsqlId=" + row.SsqlId + ", HashKey=" + row.HashKey + "], Prev=[SsqlId=" + SsqlId + ", HashKey=" + HashKey + "].");
									if (_logger.isDebugEnabled())
										_logger.debug("Found a row with 'ProcName' that starts with '*' (a DynamicSQL or Statement Cache Entry) which had a zero in 'SsqlId' or 'HashKey'. ProcName='" + procName + "', SsqlId=" + SsqlId + ", HashKey=" + HashKey + ". New=[SsqlId=" + row.SsqlId + ", HashKey=" + row.HashKey + "], Prev=[SsqlId=" + SsqlId + ", HashKey=" + HashKey + "].");
								}
								catch (NumberFormatException nfe)
								{
									_logger.warn("Problems parsing 'ProcName' content into 'SsqlId' and 'HashKey'. Found a row with ProcName='" + procName + "'. str_SsqlId=" + str_SsqlId + ", str_SsqlId=" + str_HashKey + ". Caught: " + nfe);
								}
							}
						}

						// Check if this SPID was blocked by anyone... when it was executing...
						// In that case... fill in the blocking details.
						SpidInfoBatchIdEntry spidInfoBatchEntry = _spidInfoManager.getSpidInfoBatchIdEntry(row.SPID, row.KPID, row.BatchID);
						//if (spidInfoBatchEntry == null)
						//	System.out.println("- @row: SPID INFO[spid="+row.SPID+", kpid="+row.KPID+", batchId="+row.BatchID+"]: --NOT-FOUND--");
						if (spidInfoBatchEntry != null)
						{
							//System.out.println("- @row: SPID INFO: " + spidInfoBatchEntry);
							
							if (spidInfoBatchEntry._BlockingSPID != 0 && spidInfoBatchEntry._BlockingKPID != 0 && spidInfoBatchEntry._BatchID != 0)
							{
								// System.out.println("-@row:-- BLOCKING INFO[spid="+spidInfoBatchEntry._SPID+", kpid="+spidInfoBatchEntry._KPID+", batchid="+spidInfoBatchEntry._BatchID+"] has blocker: blkSPID="+spidInfoBatchEntry._BlockingSPID+", blkKPID="+spidInfoBatchEntry._BlockingKPID+", blkBatchID="+spidInfoBatchEntry._BlockingBatchID+".");
								SpidInfoBatchIdEntry blockingInfoEntry = _spidInfoManager.getSpidInfoBatchIdEntry(spidInfoBatchEntry._BlockingSPID, spidInfoBatchEntry._BlockingKPID, spidInfoBatchEntry._BlockingBatchID);
								// System.out.println("-@row:-- BLOCKING INFO Entry: " + blockingInfoEntry);

								if (blockingInfoEntry != null)
								{
									row.BlockedBySpid        = blockingInfoEntry._SPID;
									row.BlockedByKpid        = blockingInfoEntry._KPID;
									row.BlockedByBatchId     = blockingInfoEntry._BatchID;
									
									row.BlockedByCommand     = blockingInfoEntry._Command;
									row.BlockedByApplication = blockingInfoEntry._Application;
									row.BlockedByTranId      = blockingInfoEntry._MasterTransactionID;

									row.BlockedBySqlText     = blockingInfoEntry._SqlText;

									// If the blocking SQL Text is not available from the "SpidInfoManager"
									// Check if it's stored in the "history"
									// NOTE: If it's a short lived SQL Statement AND the client has control (WaitEventID=250), then the SQL wont be available
									if (StringUtil.isNullOrBlank(row.BlockedBySqlText))
									{
										// Get SQL-Text for the statement
										BatchIdEntry batchIdEntry = _spidSqlTextAndPlanManager.getBatchIdEntryLazy(blockingInfoEntry._SPID, blockingInfoEntry._KPID, blockingInfoEntry._BatchID);
										if (batchIdEntry != null)
										{
											row.BlockedBySqlText = batchIdEntry.getSqlText();
										}
									}
								}
								else
								{
									row.BlockedBySpid        = spidInfoBatchEntry._BlockingSPID;
									row.BlockedByKpid        = spidInfoBatchEntry._BlockingKPID;
									row.BlockedByBatchId     = spidInfoBatchEntry._BatchID;
								}
							}
						}

						// Possibly fill in "WiatTimeDetails", which is a JSON or CSV: WaitEventID#=val, WaitEventID#=val, WaitEventID#=val
						if (sampleWaitInfo)
						{
							row.WaitTimeDetails  = _waitInfo.getWaitTimeDetailsAsJson(true, row.SPID, row.KPID, row.BatchID);
							// System.out.println("-@row:-- WAIT INFO: spid="+row.SPID+", kpid="+row.KPID+", batchId="+row.BatchID+": " + row.WaitTimeDetails);
						}

						
						// FINALLY: Add the row to the statements list
						statementRecords.add(row);
					}
				}
				rs.close();
				stmnt.close();

				_statStatementCaptureRows     += rowCount;
				_statStatementCaptureRowsDiff += rowCount;
				
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
				
				_logger.error("SQL Capture problems when capturing 'SQL Statements' Caught "+AseConnectionUtils.sqlExceptionToString(ex) + " when executing SQL: "+_sql_sqlStatements, ex);
			}
			
			long captureTime = TimeUtils.msDiffNow(captureStartTime);
			_statStatementCaptureTime     += captureTime;
			_statStatementCaptureTimeDiff += captureTime;
		}

		//------------------------------------------------
		// SQL TEXT
		// - is available in the table monSysSQLText when the SQL *starts* to execute (or after it has been optimized)
		//   The statement-information/statistics will on the other hand be available *after* the statement has finnished to execute
		//------------------------------------------------
		if (_sampleSqlText)
		{
			long captureStartTime = System.currentTimeMillis();

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
				List<String> colNames = DbUtils.getColumnNames(rs.getMetaData());

				int pos_SPID            = findInListToJdbcPos(colNames, "SPID");
				int pos_KPID            = findInListToJdbcPos(colNames, "KPID");
				int pos_BatchID         = findInListToJdbcPos(colNames, "BatchID");
				int pos_SequenceInBatch = findInListToJdbcPos(colNames, "SequenceInBatch");
				int pos_SQLText         = findInListToJdbcPos(colNames, "SQLText");
				int pos_ServerLogin     = findInListToJdbcPos(colNames, "ServerLogin");

				int rowCount = 0;
				while(rs.next())
				{
					rowCount++;

					int SPID            = rs.getInt   (pos_SPID);
					int KPID            = rs.getInt   (pos_KPID);
					int BatchID         = rs.getInt   (pos_BatchID);
					int SequenceInBatch = rs.getInt   (pos_SequenceInBatch);
					String sqlText      = rs.getString(pos_SQLText);
					String ServerLogin  = rs.getString(pos_ServerLogin);
					
					// This is where we can update for example:
					//   - How many "dynamic SQL Prepare" ---> 'create proc dyn###'
					//   - etc, etc..
					if (_sqlTextStatistics != null)
					{
						updateSqlTextStats(SPID, KPID, BatchID, SequenceInBatch, sqlText);
					}

					_spidSqlTextAndPlanManager.addSqlText(SPID, KPID, BatchID, SequenceInBatch, sqlText, ServerLogin);
				}
				_statSqlTextCaptureRows       += rowCount;
				_statSqlTextCaptureRowsDiff   += rowCount;

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
				
				_logger.error("SQL Capture problems when capturing 'SQL Text' Caught "+AseConnectionUtils.sqlExceptionToString(ex) + " when executing SQL: "+_sql_sqlText, ex);
			}

			long captureTime = TimeUtils.msDiffNow(captureStartTime);
			_statSqlTextCaptureTime     += captureTime;
			_statSqlTextCaptureTimeDiff += captureTime;
		}

		//------------------------------------------------
		// SQL PLANS
		// - is available in the table monSysPlanText when the SQL *starts* to execute (or after it has been optimized)
		//   The statement-information/statistics will on the other hand be available *after* the statement has finnished to execute
		//------------------------------------------------
		if (_samplePlan)
		{
			long captureStartTime = System.currentTimeMillis();

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
				List<String> colNames = DbUtils.getColumnNames(rs.getMetaData());

				int pos_SPID           = findInListToJdbcPos(colNames, "SPID");
				int pos_KPID           = findInListToJdbcPos(colNames, "KPID");
				int pos_BatchID        = findInListToJdbcPos(colNames, "BatchID");
				int pos_SequenceNumber = findInListToJdbcPos(colNames, "SequenceNumber");
				int pos_PlanText       = findInListToJdbcPos(colNames, "PlanText");

				int rowCount = 0;
				while(rs.next())
				{
					rowCount++;

					int SPID           = rs.getInt   (pos_SPID);
					int KPID           = rs.getInt   (pos_KPID);
					int BatchID        = rs.getInt   (pos_BatchID);
					int SequenceNumber = rs.getInt   (pos_SequenceNumber);
					String planText    = rs.getString(pos_PlanText);

					_spidSqlTextAndPlanManager.addPlanText(SPID, KPID, BatchID, SequenceNumber, planText);
				}
				_statPlanTextCaptureRows      += rowCount;
				_statPlanTextCaptureRowsDiff  += rowCount;

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
				
				_logger.error("SQL Capture problems when capturing 'SQL Plan Text' Caught "+AseConnectionUtils.sqlExceptionToString(ex) + " when executing SQL: "+_sql_sqlPlanText, ex);
			}

			long captureTime = TimeUtils.msDiffNow(captureStartTime);
			_statPlanTextCaptureTime     += captureTime;
			_statPlanTextCaptureTimeDiff += captureTime;
		}
		
		//------------------------------------------------
		// Post processing
		//------------------------------------------------

		// Send counters for storage
		int count = toPcs(pch, statementRecords);
		
		// Cleanup the structure that holds SQL-Text and SQL-ShowPlan information!
		// This will remove SQL and PLAN text's that are "below" the last sent entry for every SPID, KPID, BatchID
		// The "last known TEXT" entries will always be present/saved
		_spidSqlTextAndPlanManager.endOfScan();

		_spidInfoManager.endOfScan(); // this "end-of-scan" only removes spid/kpid/batchId which do NOT exists in: _spidSqlTextAndPlanManager 
//		_waitInfo.endOfScan();        // "end-of-scan" isn't needed in this, since we just add EventID entries... (cleanup is done with removeSpids() which is executed periodically, see next code block)
		
		// Possibly every now and then (every 10 minute or so)
		// get SPID, KPID, BatchID from monProcess and remove all SqlText/PlanText that no longer has a SPID/KPID connected 
		if (TimeUtils.msDiffNow(_lastCallTo_removeUnusedSlots) > (_removeUnusedSlotsThresholdInSeconds * 1000))
		{
			// set this even if we have problems with below
			_lastCallTo_removeUnusedSlots = System.currentTimeMillis();

			Set<Integer> removedSpids = _spidSqlTextAndPlanManager.removeUnusedSlots(conn);
			
			if (_spidInfoManager != null)
				_spidInfoManager.removeSpids(removedSpids);

			if (_waitInfo != null)
				_waitInfo.removeSpids(removedSpids);
		}

//System.out.println("### doSqlCapture(): -end- toPcsCount=" + count + ", getSpidSize()=" + _spidSqlTextAndPlanManager.getSpidSize() + ", getBatchIdSize()=" + _spidSqlTextAndPlanManager.getBatchIdSize());
		
		// Write some statistics
		if (TimeUtils.msDiffNow(_statReportLastTime) > (_statReportAfterSec * 1000))
		{
			_statReportLastTime = System.currentTimeMillis();

			_logger.info("STAT [SqlCaptureBrokerAse]: "
					+ "SqlTextAddCount="             + _statSqlTextAddCount 
					+ ", SqlTextNoSqlCount="         + _statSqlTextNoSqlCount 

					+ ", NormalizeSuccessCount="     + ( !StatementNormalizer.hasInstance() ? -1 : StatementNormalizer.getInstance()._statNormalizeSuccessCount )
					+ ", NormalizeSkipCount="        + ( !StatementNormalizer.hasInstance() ? -1 : StatementNormalizer.getInstance()._statNormalizeSkipCount )
					+ ", NormalizeErrorLevel1Count=" + ( !StatementNormalizer.hasInstance() ? -1 : StatementNormalizer.getInstance()._statNormalizeErrorLevel1Count )
					+ ", NormalizeErrorLevel2Count=" + ( !StatementNormalizer.hasInstance() ? -1 : StatementNormalizer.getInstance()._statNormalizeErrorLevel2Count )
					+ ", NormalizeUdCount="          + ( !StatementNormalizer.hasInstance() ? -1 : StatementNormalizer.getInstance()._statNormalizeUdCount )
					+ ", NormalizeReWriteCount="     + ( !StatementNormalizer.hasInstance() ? -1 : StatementNormalizer.getInstance()._statNormalizeReWriteCount )

					+ ", SpidInfoCaptureTimeDiff="   + TimeUtils.msToTimeStr(_statSpidInfoCaptureTimeDiff)
					+ ", WaitInfoCaptureTimeDiff="   + TimeUtils.msToTimeStr(_statWaitInfoCaptureTimeDiff)
					+ ", StatementCaptureTimeDiff="  + TimeUtils.msToTimeStr(_statStatementCaptureTimeDiff)
					+ ", SqlTextCaptureTimeDiff="    + TimeUtils.msToTimeStr(_statSqlTextCaptureTimeDiff)
					+ ", PlanTextCaptureTimeDiff="   + TimeUtils.msToTimeStr(_statPlanTextCaptureTimeDiff)

					+ ", SpidInfoCaptureTime="       + TimeUtils.msToTimeStr(_statSpidInfoCaptureTime)
					+ ", WaitInfoCaptureTime="       + TimeUtils.msToTimeStr(_statWaitInfoCaptureTime)
					+ ", StatementCaptureTime="      + TimeUtils.msToTimeStr(_statStatementCaptureTime)
					+ ", SqlTextCaptureTime="        + TimeUtils.msToTimeStr(_statSqlTextCaptureTime)
					+ ", PlanTextCaptureTime="       + TimeUtils.msToTimeStr(_statPlanTextCaptureTime)

					+ ", SpidInfoCaptureRowsDiff="   + _statSpidInfoCaptureRowsDiff
					+ ", WaitInfoCaptureRowsDiff="   + _statWaitInfoCaptureRowsDiff
					+ ", StatementCaptureRowsDiff="  + _statStatementCaptureRowsDiff
					+ ", SqlTextCaptureRowsDiff="    + _statSqlTextCaptureRowsDiff
					+ ", PlanTextCaptureRowsDiff="   + _statPlanTextCaptureRowsDiff

					+ ", SpidInfoCaptureRows="       + _statSpidInfoCaptureRows
					+ ", WaitInfoCaptureRows="       + _statWaitInfoCaptureRows
					+ ", StatementCaptureRows="      + _statStatementCaptureRows
					+ ", SqlTextCaptureRows="        + _statSqlTextCaptureRows
					+ ", PlanTextCaptureRows="       + _statPlanTextCaptureRows

					);

			// Reset DIFF Counters
			_statSpidInfoCaptureTimeDiff  = 0;
			_statWaitInfoCaptureTimeDiff  = 0;
			_statStatementCaptureTimeDiff = 0;
			_statSqlTextCaptureTimeDiff   = 0;
			_statPlanTextCaptureTimeDiff  = 0;

			_statSpidInfoCaptureRowsDiff  = 0;
			_statWaitInfoCaptureRowsDiff  = 0;
			_statStatementCaptureRowsDiff = 0;
			_statSqlTextCaptureRowsDiff   = 0;
			_statPlanTextCaptureRowsDiff  = 0;
		}

		return count;
	}
	
	//-------------------------------------------------------------------------
	// BEGIN: declare some STATISTICS Fields
	//-------------------------------------------------------------------------
	private long _statSqlTextAddCount   = 0;
	private long _statSqlTextNoSqlCount = 0;

//	private long _statNormalizeSuccessCount = 0;
//	private long _statNormalizeSkipCount    = 0;
//	private long _statNormalizeErrorCount   = 0;

	private long _statSpidInfoCaptureTime  = 0;
	private long _statWaitInfoCaptureTime  = 0;
	private long _statSqlTextCaptureTime   = 0;
	private long _statPlanTextCaptureTime  = 0;
	private long _statStatementCaptureTime = 0;

	private long _statSpidInfoCaptureTimeDiff  = 0;
	private long _statWaitInfoCaptureTimeDiff  = 0;
	private long _statSqlTextCaptureTimeDiff   = 0;
	private long _statPlanTextCaptureTimeDiff  = 0;
	private long _statStatementCaptureTimeDiff = 0;

	private long _statSpidInfoCaptureRows  = 0;
	private long _statWaitInfoCaptureRows  = 0;
	private long _statSqlTextCaptureRows   = 0;
	private long _statPlanTextCaptureRows  = 0;
	private long _statStatementCaptureRows = 0;

	private long _statSpidInfoCaptureRowsDiff  = 0;
	private long _statWaitInfoCaptureRowsDiff  = 0;
	private long _statSqlTextCaptureRowsDiff   = 0;
	private long _statPlanTextCaptureRowsDiff  = 0;
	private long _statStatementCaptureRowsDiff = 0;

	private long _statReportLastTime = System.currentTimeMillis();
	private long _statReportAfterSec = DEFAULT_sqlCap_ase_statisticsReportEveryXSecond;
	//-------------------------------------------------------------------------
	// END: declare some STATISTICS Fields
	//-------------------------------------------------------------------------

	/**
	 * FIXME: do documentation
	 * 
	 * @param pch
	 * @param statementRecords
	 * @return
	 */
	protected int toPcs(PersistentCounterHandler pch, List<MonSqlStatement> statementRecords)
	{
		// Container object used to send data to PCS
		SqlCaptureDetails capDet = new SqlCaptureDetails();

		// default values for: 
		//    - storeNormalizedSqlTextHash = true
		//    - storeNormalizedSqlText     = false  --->>> instead of storing it, in the offline view, we can read the 'SQLText' and normalize it "on the fly", which hopefully saves us some MB at-the-end-of-the-day... the storeNormalizedSqlTextHash is still stored so we can aggregate stuff via group by... but that just takes an INT value
//		boolean storeNormalizedSqlTextHash = getConfiguration().getBooleanProperty(PROPKEY_sqlCap_ase_sqlTextAndPlan_storeNormalizedSqlTextHash, DEFAULT_sqlCap_ase_sqlTextAndPlan_storeNormalizedSqlTextHash);
		boolean storeNormalizedSqlText     = getConfiguration().getBooleanProperty(PROPKEY_sqlCap_ase_sqlTextAndPlan_storeNormalizedSqlText, DEFAULT_sqlCap_ase_sqlTextAndPlan_storeNormalizedSqlText);

		boolean printPcsAdd                = getConfiguration().getBooleanProperty(PROPKEY_sqlCap_ase_sqlTextAndPlan_printPcsAdd,            DEFAULT_sqlCap_ase_sqlTextAndPlan_printPcsAdd);

		// Used to anonymize or remove-constants in where clauses etc...
//		StatementNormalizer stmntNorm = storeNormalizedSqlText ? new StatementNormalizer() : null;
		StatementNormalizer.NormalizeParameters normalizeParameters = null;

		for (MonSqlStatement stRec : statementRecords) 
		{
			// Get SQL-Text for the statement
			BatchIdEntry batchIdEntry = _spidSqlTextAndPlanManager.getBatchIdEntry(stRec.SPID, stRec.KPID, stRec.BatchID);

			String sqlText  = batchIdEntry.getSqlText();
			String planText = batchIdEntry.getPlanText();
			String srvLogin = batchIdEntry.getServerLogin();

			String normalizedSqlText = null;
			if (StringUtil.hasValue(sqlText))
			{
				// Normalize the SQL Statement
				if (storeNormalizedSqlText)
				{
					if (normalizeParameters == null)
						normalizeParameters = new StatementNormalizer.NormalizeParameters();
					
					// The below will do the following, to make us able to group by SQL Text that are "similar":
					// - Apply a Parser on the sqlText, to replace any constant values with question marks. IN(1,2,3,4) will be replaced with IN(...) 
					// - If the parser fails (due to a "JSqlParser" do not handle "everything":
					//   - We have X number of re-writes that can be applied
					//   - if any re.writes has been applied, we try to parse it again...
					//   - if we still have problems to parse: an null or empty string will be returned
					// The re-writes & UserDefinedNormalizations are held in: StatementFixerManager & UserDefinedNormalizerManager 
					List<String> tableNamesInStatement = new ArrayList<>();
					normalizedSqlText = StatementNormalizer.getInstance().normalizeSqlText(sqlText, normalizeParameters, tableNamesInStatement);

					// Send any table names the Statement was referencing to the Persistence Counter Handler for further lookup and storage
					if (tableNamesInStatement != null && !tableNamesInStatement.isEmpty())
					{
						for (String tableName : tableNamesInStatement)
							pch.addDdl(stRec.DBName, tableName, this.getClass().getSimpleName());
					}

					stRec.AddStatus = normalizeParameters.addStatus.getIntValue();
				}

				// Copy first ### chart so we can make a "short" hash-code 
				String sqlTextShort = sqlText.substring(0, Math.min(_sqlText_shortLength, sqlText.length()));

				// Set some fields in the Statement Record Object
				stRec.ServerLogin          = srvLogin;
//				stRec.AddStatus            = 0; // Not used for the moment
				stRec.JavaSqlLength        = sqlText           == null ? -1 : sqlText          .length();
				stRec.JavaSqlLengthShort   = sqlTextShort      == null ? -1 : sqlTextShort     .length();
				stRec.NormJavaSqlLength    = normalizedSqlText == null ? -1 : normalizedSqlText.length();
				stRec.JavaSqlHashCode      = sqlText           == null ? -1 : sqlText          .hashCode();
				stRec.JavaSqlHashCodeShort = sqlTextShort      == null ? -1 : sqlTextShort     .hashCode();
				stRec.NormJavaSqlHashCode  = normalizedSqlText == null ? -1 : normalizedSqlText.hashCode();

				stRec.SQLText              = sqlText;
				stRec.NormSQLText          = normalizedSqlText;
				stRec.PlanText             = planText;

//				MonSqlText monSqlText = new MonSqlText();
//
//				monSqlText.sampleTime           = stRec.sampleTime; 
//				monSqlText.InstanceID           = stRec.InstanceID; 
//				monSqlText.SPID                 = stRec.SPID; 
//				monSqlText.KPID                 = stRec.KPID; 
//				monSqlText.BatchID              = stRec.BatchID; 
//				monSqlText.ServerLogin          = srvLogin; 
//				monSqlText.AddMethod            = 1; // 1=Direct
//				monSqlText.JavaSqlLength        = sqlText     .length(); 
//				monSqlText.JavaSqlLengthShort   = sqlTextShort.length(); 
//				monSqlText.JavaSqlHashCode      = sqlText     .hashCode(); 
//				monSqlText.JavaSqlHashCodeShort = sqlTextShort.hashCode(); 
//				monSqlText.SQLText              = sqlText; 
//				monSqlText.NormSQLText          = normalizedSqlText;
//				monSqlText.NormJavaSqlHashCode  = normalizedSqlTextJavaHashCode;
//
////System.out.println("  >> pcs >> SQL-TEXT:  ms=" + stRec.Elapsed_ms + ", spid=" + stRec.SPID + ", kpid=" + stRec.KPID + ", batchId=" + stRec.BatchID + ", sql="+sqlText);
//				// Add the SQL-Text entry
//				capDet.add(monSqlText.getPcsRow());
			}

			if (StringUtil.hasValue(planText))
			{
				stRec.PlanText = planText;

//				MonSqlPlan monSqlPlan = new MonSqlPlan();
//
//				monSqlPlan.sampleTime  = stRec.sampleTime;
//				monSqlPlan.InstanceID  = stRec.InstanceID;
//				monSqlPlan.SPID        = stRec.SPID;
//				monSqlPlan.KPID        = stRec.KPID;
//				monSqlPlan.PlanID      = stRec.PlanID;
//				monSqlPlan.BatchID     = stRec.BatchID;
//				monSqlPlan.ContextID   = stRec.ContextID;
//				monSqlPlan.DBID        = stRec.DBID;
//				monSqlPlan.DBName      = stRec.DBName;
//				monSqlPlan.ProcedureID = stRec.ProcedureID;
//				monSqlPlan.AddMethod   = 1; // 1=Direct
//				monSqlPlan.PlanText    = planText;
//
//				// Add the PLAN entry
//				capDet.add(monSqlPlan.getPcsRow());
			}

			// DEBUG Print
//printPcsAdd = true;
			if (printPcsAdd || _logger.isDebugEnabled())
			{
				_logger.info("  >> pcs >> [addCnt=" + _statSqlTextAddCount + ",noSqlCnt=" + _statSqlTextNoSqlCount + "] STATEMENT: ms=" + stRec.Elapsed_ms + ", rowc=" + stRec.RowsAffected + ", error=" + stRec.ErrorStatus + ", spid=" + stRec.SPID + ", kpid=" + stRec.KPID + ", batchId=" + stRec.BatchID + ", addStatus=" + stRec.AddStatus + ", sql=|" + StringUtils.normalizeSpace(sqlText) + "|, normalizedSqlTextJavaHashCode=" + stRec.NormJavaSqlHashCode + (stRec.ErrorStatus == 0 ? "" : ", MsgText="+AseErrorMessageDictionary.getInstance().getDescription(stRec.ErrorStatus)) );
				if (StringUtil.isNullOrBlank(sqlText))
				{
					_logger.error("            ************************* NO-SQL-TEXT ************************** spid=" + stRec.SPID + ", kpid=" + stRec.KPID + ", batchId=" + stRec.BatchID + ", addStatus=" + stRec.AddStatus + ", sql=|"+sqlText+"|.");
				}
			}

			// Add the STATEMENT entry
			capDet.add(stRec.getPcsRow());

			// Increment some statistics
			if (StringUtil.isNullOrBlank(sqlText))
			{
				_statSqlTextNoSqlCount++;
			}
			_statSqlTextAddCount++;
		}

		// Post the information to the PersistentCounterHandler, which will save the information to it's writers...
		addToPcs(pch, capDet);
		
		return capDet.size();
	}

	/** 
	 * Post it to PCS, in it's own method to make testing easier...<br>
	 * Meaning we can subclass and override this method when testing...
	 */
	protected void addToPcs(PersistentCounterHandler pch, SqlCaptureDetails sqlCaptureDetails)
	{
//DEBUG	
//for (List<Object> list : sqlCaptureDetails.getList())
//	System.out.println("            ++pc++ " + list);
		
		pch.addSqlCapture(sqlCaptureDetails);
	}


	@Override
	public void lowOnMemoryHandler()
	{
		_logger.warn("Persistant Counter Handler, lowOnMemoryHandler() was called. Emtying the SPID SQL-Text, SQL-Plan and SPID-Info structure, which has " + _spidSqlTextAndPlanManager.getSpidSize() + " SPID entries and " + _spidSqlTextAndPlanManager.getBatchIdSize() + " BatchId entries and SPID-Info " + _spidInfoManager.getSpidSize() + " entries.");
		_spidSqlTextAndPlanManager.clear();
		_spidInfoManager.clear();
	}
	@Override
	public void outOfMemoryHandler()
	{
		_logger.warn("Persistant Counter Handler, outOfMemoryHandler() was called. Emtying the SPID SQL-Text, SQL-Plan and SPID-Info structure, which has " + _spidSqlTextAndPlanManager.getSpidSize() + " SPID entries and " + _spidSqlTextAndPlanManager.getBatchIdSize() + " BatchId entries and SPID-Info " + _spidInfoManager.getSpidSize() + " entries.");
		_spidSqlTextAndPlanManager.clear();
		_spidInfoManager.clear();
	}



	//--------------------------------------------------------------------------
	// BEGIN: SPID - SqlText and SqlPlan - manager 
	//--------------------------------------------------------------------------
	private SpidSqlTextAndPlanManager _spidSqlTextAndPlanManager = new SpidSqlTextAndPlanManager();

	//----------------------------------------------------------------------------------------
	private static class SpidSqlTextAndPlanManager
	{
		private HashMap<Integer, SpidEntry> _spidMap = new HashMap<>();
		
		private SpidEntry getSpidEntry(int spid, int kpid)
		{
			SpidEntry spidEntry = _spidMap.get(spid);
			if (spidEntry == null)
			{
				//System.out.println("  +++ Adding a new SPID entry: spid="+spid+", kpid="+kpid);
				spidEntry = new SpidEntry(spid, kpid);
				_spidMap.put(spid, spidEntry);
			}
			return spidEntry;
		}

		public int getBatchIdSize()
		{
			int size = 0;
			for (SpidEntry spidEntry : _spidMap.values())
			{
				size += spidEntry._batchIdMap.size();
			}
			return size;
		}
		public int getSpidSize()
		{
			return _spidMap.size();
		}
		
		/** Do not add any new SpidEntry or BatchIdEntry if they do NOT exists, just return null if no existance */
		public BatchIdEntry getBatchIdEntryLazy(int spid, int kpid, int batchId)
		{
			SpidEntry spidEntry = _spidMap.get(spid);
			if (spidEntry == null)
				return null;

			return spidEntry.getBatchIdEntryLazy(spid, kpid, batchId);
		}

		/** Check if a entry exists */
		public boolean exists(int spid, int kpid, int batchId)
		{
			return getBatchIdEntryLazy(spid, kpid, batchId) != null;
		}
		
		public BatchIdEntry getBatchIdEntry(int spid, int kpid, int batchId)
		{
			return getSpidEntry(spid, kpid).getBatchIdEntry(spid, kpid, batchId);
		}
		
		public void addSqlText(int spid, int kpid, int batchId, int sequenceNum, String sqlText, String serverLogin)
		{
			getSpidEntry(spid, kpid).addSqlText(spid, kpid, batchId, sequenceNum, sqlText, serverLogin);
		}
//		public String getSqlText(int spid, int kpid, int batchId)
//		{
//			return getSpidEntry(spid, kpid).getSqlText(spid, kpid, batchId);
//		}

		
//		public String getServerLogin(int spid, int kpid, int batchId)
//		{
//			return getSpidEntry(spid, kpid).getServerLogin(spid, kpid, batchId);
//		}
		

		public void addPlanText(int spid, int kpid, int batchId, int sequenceNum, String planText)
		{
			getSpidEntry(spid, kpid).addPlanText(spid, kpid, batchId, sequenceNum, planText);
		}
//		public String getPlanText(int spid, int kpid, int batchId)
//		{
//			return getSpidEntry(spid, kpid).getPlanText(spid, kpid, batchId);
//		}

		/**
		 * Get all active SPID's in ASE <br>
		 * Remove all SPID's from '_spidSqlTextAndPlanManager' that no longer exists in ASE
		 * 
		 * @param conn
		 * @return a Set of SPID's that was removed... which can be used elsewhere, to do the same thing.
		 */
		public Set<Integer> removeUnusedSlots(DbxConnection conn)
		{
//			String sql = "select SPID, KPID, BatchID from master.dbo.monProcess";
			String sql = "select spid from master.dbo.sysprocesses";

			try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
			{
				Set<Integer> dbmsSpidSet = new HashSet<>();
				while(rs.next())
				{
					int spid    = rs.getInt(1);
//					int kpid    = rs.getInt(2);
//					int BatchId = rs.getInt(3);

					dbmsSpidSet.add(spid);
				}

				// 1: Take a copy of SPID MAP
				// 2: Remove all spid's that exists in ASE 
				// 2: Remove all entries that is NOT active in ASE  
				Set<Integer> spidsToBeRemoved = new HashSet<>(_spidMap.keySet());
				spidsToBeRemoved.removeAll(dbmsSpidSet);
				// now remove the entries
				_spidMap.keySet().removeAll(spidsToBeRemoved);

				if ( ! spidsToBeRemoved.isEmpty() )
					_logger.debug("Removed the following SPIDs from the SqlText/PlanText structure, which was no longer present in ASE. size=" + spidsToBeRemoved.size() + ", removeSet=" + spidsToBeRemoved);
				
//				if ( ! spidsToBeRemoved.isEmpty() )
//					_logger.info("Removed the following SPIDs from the SqlText/PlanText structure, which was no longer present in ASE. size=" + spidsToBeRemoved.size() + ", removeSet=" + spidsToBeRemoved);

				return spidsToBeRemoved;
			}
			catch(Exception ex)
			{
				_logger.error("Problem cleaning up unused SPID/KPID/BatchID SqlTest/PlanText slots, using SQL='" + sql + "'. Caught: " + ex);
			}

			return Collections.emptySet();
		}

		/** Remove any SqlText and PlanText that is no longer used (save only last BatchId) */
		public void endOfScan()
		{
			for (SpidEntry spidEntry : _spidMap.values())
			{
				spidEntry.endOfScan();
			}
		}

//		public int getBatchIdCountAndReset()
//		{
//			int count = 0;
//			for (SpidEntry spidEntry : _spidMap.values())
//			{
//				count += spidEntry.getBatchIdCountAndReset();
//			}
//			return count;
//		}
		

		/** Clear the structure, can for example be used if we starting to get LOW on memory */
		public void clear()
		{
			_spidMap.clear();
//			_spidMap = new HashMap<>();
		}
	}

	//----------------------------------------------------------------------------------------
	private static class SpidEntry
	{
		private int _spid;
		private int _kpid;
		private int _maxBatchId;
//		private int _batchIdCount;
		
		private HashMap<Integer, BatchIdEntry> _batchIdMap = new HashMap<>();

		public SpidEntry(int spid, int kpid)
		{
			_spid = spid;
			_kpid = kpid;
		}

		/** Remove all BatchId entries that is no longer used */ 
		public void endOfScan()
		{
			if (_batchIdMap.size() <= 1)
				return;

			BatchIdEntry maxBatchIdEntry = _batchIdMap.get(_maxBatchId);
			//System.out.println("    EOS -- _batchIdMap.size="+_batchIdMap.size()+", maxBatchIdEntry=[spid="+maxBatchIdEntry._spid+", batchId="+maxBatchIdEntry._batchId+"] -- DynamicName='"+maxBatchIdEntry._dynamicSqlName+"', DynamicSqlText=|"+maxBatchIdEntry._dynamicSqlText+"|.");//, sqlText="+maxBatchIdEntry._sqlText);

			// Loop all entries... and remove the ones that we no longer need
			for(Iterator<Entry<Integer, BatchIdEntry>> it = _batchIdMap.entrySet().iterator(); it.hasNext(); )
			{
				// Get Key/value from the iterator
				Entry<Integer, BatchIdEntry> entry = it.next();
				int          batchId = entry.getKey();
				BatchIdEntry be      = entry.getValue();

				if(batchId < _maxBatchId) 
				{
					boolean remove = true;

					// if "last/max" entry has same "Dynamic SQL Name" as the entry we are looking at... 
					// then KEEP the entry (since we need "all/previous" batchId to get SqlText) 
					if (maxBatchIdEntry.isDynamicSql())
					{
						if (maxBatchIdEntry._dynamicSqlName.equals(be._dynamicSqlName) && be._isDynamicSqlPrepare)
						{
							//System.out.println("    <-- KEEP-DYNAMIC-SQL: SpidEntry[spid="+_spid+", kpid="+_kpid+", batchId="+be._batchId+"] -- DynamicName='"+be._dynamicSqlName+"', DynamicSqlText=|"+be._dynamicSqlText+"|, sqlText="+be._sqlText);
							remove = false;
						}
					}
					
					// if we want to keep a BatchId's (and it's content) for more than one -END-OF-SCAN- the "keepCounter" can be used.
					// Note: The initial value for the keepCounter is set in BatchIdEntry class
					if (be._keepCounter > 0)
					{
						be._keepCounter--;
						remove = false;
					}
					
					if (remove)
					{
						//System.out.println("    <-- SpidEntry[spid="+_spid+", kpid="+_kpid+", maxBatchId="+_maxBatchId+"] -- removing: batchId=" + entry.getValue()._batchId + ", sqlText="+entry.getValue().getSqlText());
						it.remove();
						
						// Should we remove entries for SpidInfo here since it wont be referenced anymore
						// When entries from monSysStatements has "aged out"... they wont be needed anymore
						//_spidInfoManager.remove(be._spid, be._kpid, be._batchId);
						//_waitInfo       .remove(be._spid, be._kpid, be._batchId);
					}
				}
			}
		}

		/** Do not add any new BatchIdEntry if they do NOT exists, just return null if no existence */
		public BatchIdEntry getBatchIdEntryLazy(int spid, int kpid, int batchId)
		{
			if (kpid != _kpid)
				return null;

			return _batchIdMap.get(batchId);
		}

		private BatchIdEntry getBatchIdEntry(int spid, int kpid, int batchId)
		{
			// If it's a NEW kpid... then clear the batchId Map
			if (kpid != _kpid)
			{
				//System.out.println("  --- Found NEW KPID for SPID clearing old batchMap: spid="+spid+", new-kpid="+kpid+", current-kpid="+_kpid);
				_batchIdMap.clear();
				_maxBatchId = 0;
				_kpid = kpid;
			}
			
			// Remember the highest batch id (used by endOfScan() to cleanup)
			_maxBatchId = Math.max(batchId, _maxBatchId);
//			if (batchId > _maxBatchId)
//			{
//				_batchIdCount += batchId - _maxBatchId;
//				_maxBatchId = batchId;
//			}

			// Find the BatchId (or create a new one)
			BatchIdEntry batchIdEntry = _batchIdMap.get(batchId);
			if (batchIdEntry == null)
			{
				batchIdEntry = new BatchIdEntry(this, spid, kpid, batchId);
				_batchIdMap.put(batchId, batchIdEntry);
			}

			return batchIdEntry;
		}
		
//		public int getBatchIdCountAndReset()
//		{
//			int count = _batchIdCount;
//			_batchIdCount = 0;
//			return count;
//		}

		public void addSqlText(int spid, int kpid, int batchId, int sequenceNum, String sqlText, String serverLogin)
		{
			getBatchIdEntry(spid, kpid, batchId).addSqlText(sequenceNum, sqlText, serverLogin);
		}
//		public String getSqlText(int spid, int kpid, int batchId)
//		{
//			return getBatchIdEntry(spid, kpid, batchId).getSqlText();
//		}

//		public String getServerLogin(int spid, int kpid, int batchId)
//		{
//			return getBatchIdEntry(spid, kpid, batchId).getServerLogin();
//		}

		public void addPlanText(int spid, int kpid, int batchId, int sequenceNum, String planText)
		{
			getBatchIdEntry(spid, kpid, batchId).addPlanText(sequenceNum, planText);
		}
//		public String getPlanText(int spid, int kpid, int batchId)
//		{
//			return getBatchIdEntry(spid, kpid, batchId).getPlanText();
//		}
	}

	//----------------------------------------------------------------------------------------
	private static class BatchIdEntry
	{
		private SpidEntry _parent;
		private int _spid;
		private int _kpid;
		private int _batchId;

		// if we want to keep a BatchId's (and it's content) for more than one -END-OF-SCAN- the "keepCounter" can be used.
		// This is decremented in SpidEntry.endOfScan(), and when it reaches 0 it is removed.
		public int _keepCounter = 0; 

		private String  _dynamicSqlName;
		private String  _dynamicSqlText;
		private boolean _isDynamicSqlPrepare = false;
//		private BatchIdEntry _dynamicSqlPrepareDirectLink; // if we have already have back-traced the origin BatchIdEntry which holds the "create proc text" lets remember it...

		private StringBuilder _sqlText  = new StringBuilder();
		private StringBuilder _planText = new StringBuilder();
		private String        _srvLogin = "";

		public BatchIdEntry(SpidEntry spidEntry, int spid, int kpid, int batchId)
		{
			_parent  = spidEntry;
			_spid    = spid;
			_kpid    = kpid;
			_batchId = batchId;
			
			_keepCounter = _default_batchIdEntry_keepCount;
		}

		public boolean isDynamicSql()
		{
			return _dynamicSqlName != null;
		}

		/**
		 * Append SQL Text to the buffer (called for every row we get from monSysSQLText)
		 * @param sequenceNum
		 * @param sqlText
		 * @param serverLogin
		 */
		public void addSqlText(int sequenceNum, String sqlText, String serverLogin)
		{
			if (sqlText == null)
				return;

			// Figure out if this is a "Dynamic SQL" entry (probably from CT-Lib)
			if (sqlText.startsWith("DYNAMIC_SQL "))
			{
				_dynamicSqlName = sqlText;
				//System.out.println("    --> DYNAMIC_SQL [spid="+_spid+", batchId="+_batchId+"] :: _dynamicSqlName="+_dynamicSqlName);
			}
			if (_dynamicSqlName != null && StringUtils.startsWithIgnoreCase(sqlText, "create proc "))
			{
				_isDynamicSqlPrepare = true;
				//System.out.println("    --> DYNAMIC_SQL [spid="+_spid+", batchId="+_batchId+"]     -- IS-PREPARE :: _dynamicSqlName="+_dynamicSqlName+"");
				int startPos = StringUtils.indexOfIgnoreCase(sqlText, " as ");
				if (startPos != -1)
				{
					startPos += " as ".length();
					_dynamicSqlText = sqlText.substring(startPos);
					//System.out.println("    --> DYNAMIC_SQL [spid="+_spid+", batchId="+_batchId+"]     -- IS-PREPARE :: _dynamicSqlName="+_dynamicSqlName+", _dynamicSqlText=|"+_dynamicSqlText+"|.");
				}
			}
			if (_isDynamicSqlPrepare && sequenceNum > 1)
			{
				if (_dynamicSqlText == null)
					_dynamicSqlText = sqlText;
				else
					_dynamicSqlText += sqlText;
				//System.out.println("    --> DYNAMIC_SQL [spid="+_spid+", batchId="+_batchId+"]     -- APPEND :: _dynamicSqlName="+_dynamicSqlName+", _dynamicSqlText=|"+_dynamicSqlText+"|.");
			}

			_sqlText.append(sqlText);
			_srvLogin = serverLogin;
		}
		
		/**
		 * Get SQL Text for this BatchID. <br>
		 * If current BatchID is a "Dynamic SQL", the SQL Text is probably NOT located in this BatchID (the execution batchId)...
		 * The "declare" part ("create proc ... as ..." part, which holds the SQL Text) is done in a earlier BatchID, so search the parent BatchID map) 
		 * 
		 * @return
		 */
		public String getSqlText()
		{
			// If the entry is a Dynamic SQL, then the SQLText is NOT stored in the same BatchID as the "execution" part of the Dynamic SQL (prepare, execute..., close)
			// So go and grab any "previous" BatchID where the "full" SqlText are stored.
			if (_dynamicSqlName != null && !_isDynamicSqlPrepare)
			{
				//System.out.println("    ??? getSqlText(): IS-DYNAMIC-SQL: SpidEntry[spid="+_spid+", kpid="+_kpid+", batchId="+_batchId+"] -- DynamicName='"+_dynamicSqlName+"', DynamicSqlText=|"+_dynamicSqlText+"|.");
				for (BatchIdEntry be : _parent._batchIdMap.values())
				{
					if (be._isDynamicSqlPrepare && _dynamicSqlName.equals(be._dynamicSqlName))
					{
						if (be._dynamicSqlText != null)
							return "/* DYNAMIC-SQL: */ " + be._dynamicSqlText;
						return "/* DYNAMIC-SQL: */ " + be._sqlText.toString();
					}
				}
			}
			return _sqlText.toString();
		}

		public String getServerLogin()
		{
			return _srvLogin;
		}

		public void addPlanText(int sequenceNum, String planText)
		{
			_planText.append(planText);
		}
		public String getPlanText()
		{
			return _planText.toString();
		}
	}

	//--------------------------------------------------------------------------
	// END: SPID - SqlText and SqlPlan - manager 
	//--------------------------------------------------------------------------




	//--------------------------------------------------------------------------
	// BEGIN: SPID - INFO/WAIT - manager 
	//--------------------------------------------------------------------------

	private SpidInfoManager _spidInfoManager = new SpidInfoManager();
//	private SpidInfo _spidInfo = new SpidInfo();
	private WaitInfo _waitInfo = new WaitInfo();

	private String _sql_spidInfo      = null;
//	private String _sql_waitInfo      = null;

//	protected boolean _sampleSpidInfo   = true;
//	protected boolean _sampleWaitInfo   = true;

	private class SpidInfoManager
	{
		private HashMap<Integer, SpidInfoEntry> _spidInfoMap = new HashMap<>();
		private long _lastCleanupTime = System.currentTimeMillis();
		private long _cleanupThreshold = 30_000; // cleanup every 30 seconds
		
		private SpidInfoEntry getSpidInfoEntry(int spid, int kpid)
		{
			SpidInfoEntry spidInfoEntry = _spidInfoMap.get(spid);
			if (spidInfoEntry == null)
			{
				//System.out.println("  +++ SpidInfoManager: Adding a new SPID entry: spid="+spid+", kpid="+kpid);

				spidInfoEntry = new SpidInfoEntry(spid, kpid);
				_spidInfoMap.put(spid, spidInfoEntry);
			}
			return spidInfoEntry;
		}

		public int getSpidSize()
		{
			return _spidInfoMap.size();
		}
		public int getBatchIdSize()
		{
			int size = 0;
			for (SpidInfoEntry spidInfoEntry : _spidInfoMap.values())
			{
				size += spidInfoEntry._batchIdMap.size();
				//System.out.println("           xxxxxx: spid=" + spidInfoEntry._spid + ", kpid=" + spidInfoEntry._kpid + ", batchCnt=" + spidInfoEntry._batchIdMap.size() + ", batchIds=" + spidInfoEntry._batchIdMap.keySet());
			}
			//System.out.println("           xxxxxx: getBatchIdSize: " + size);
			return size;
		}

		public SpidInfoBatchIdEntry getSpidInfoBatchIdEntry(int spid, int kpid, int batchId)
		{
			return getSpidInfoEntry(spid, kpid).getSpidInfoBatchIdEntry(spid, kpid, batchId);
		}
		
		public void addSpidInfoBatchIdEntry(SpidInfoBatchIdEntry entry)
		{
			getSpidInfoEntry(entry._SPID, entry._KPID).addSpidInfoBatchIdEntry(entry);
		}

//		public void addOrUpdateWaitInfo()
//		{
//			should we do this in here ???
//		}
		
		public void removeSpids(Set<Integer> removedSpids)
		{
			_spidInfoMap.keySet().removeAll(removedSpids);
		}

		/** Remove any Entries that is no longer used */
		public void endOfScan()
		{
			long msSinceCleanup = System.currentTimeMillis() - _lastCleanupTime;
			if (msSinceCleanup < _cleanupThreshold) // Cleanup every 30 seconds
				return;

			// Mark it has Cleanup was done at
			_lastCleanupTime = System.currentTimeMillis();

			// Loop all the children... if cild are empty remove it.
			for(Iterator<Entry<Integer, SpidInfoEntry>> it = _spidInfoMap.entrySet().iterator(); it.hasNext(); ) 
			{
				// Get Key/value from the iterator
				Entry<Integer, SpidInfoEntry> entry = it.next();
				SpidInfoEntry spidInfoEntry = entry.getValue();

				spidInfoEntry.endOfScan();

				// Remove it no batch entries
				if (spidInfoEntry.isEmpty())
					it.remove();
			}
		}

		/** remove a specific SPID, KPID, BatchID... called from other cleanup places */
		public void remove(int spid, int kpid, int batchId)
		{
			SpidInfoEntry spidInfoEntry = _spidInfoMap.get(spid);
			if (spidInfoEntry != null)
			{
				if (spidInfoEntry._kpid == kpid)
				{
					spidInfoEntry._batchIdMap.remove(batchId);
				}
			}
		}

		/** Clear the structure, can for example be used if we starting to get LOW on memory */
		public void clear()
		{
			_spidInfoMap.clear();
		}
	}

	
	private class SpidInfoEntry
	{
		private int _spid;
		private int _kpid;
		private int _maxBatchId;
		
		//      key=BatchID, SpidInfoBatchIdEntry
		private LinkedHashMap<Integer, SpidInfoBatchIdEntry> _batchIdMap = new LinkedHashMap<>();

		public SpidInfoEntry(int spid, int kpid)
		{
			_spid = spid;
			_kpid = kpid;
		}

		public void addSpidInfoBatchIdEntry(SpidInfoBatchIdEntry entry)
		{
			// If it's a NEW kpid... then clear the batchId Map
			if (entry._KPID != _kpid)
			{
				// System.out.println("  --- Found NEW KPID for SPID clearing old batchMap: spid="+_spid+", new-kpid="+entry._KPID+", current-kpid="+_kpid);

				_batchIdMap.clear();
				_maxBatchId = 0;
				_kpid = entry._KPID;
			}

			// Remember the highest batch id (used by endOfScan() to cleanup)
			_maxBatchId = Math.max(entry._BatchID, _maxBatchId);

			// Find the BatchId (or create a new one -- in this scenario -->> SET SOME OPTIONS)
			SpidInfoBatchIdEntry batchIdEntry = _batchIdMap.get(entry._BatchID);
			if (batchIdEntry == null)
			{
				// System.out.println("  +++ add BatchId="+entry._BatchID+": spid="+_spid+", kpid="+_kpid);
			//	entry._parent = this;
			}
			_batchIdMap.put(entry._BatchID, entry);
		}

		public SpidInfoBatchIdEntry getSpidInfoBatchIdEntry(int spid, int kpid, int batchId)
		{
			if (spid != _spid || kpid != _kpid) 
			{
				// throw new RuntimeException("in SpidInfoEntry when getSpidInfoBatchIdEntry(spid=" + spid + ", kpid=" + kpid + ", batchId=" + batchId + "), the entry found had _spid=" +_spid + ", _kpid" + _kpid + " This should NOT happen.");
				//_logger.error("in SpidInfoEntry when getSpidInfoBatchIdEntry(spid=" + spid + ", kpid=" + kpid + ", batchId=" + batchId + "), the entry found had _spid=" +_spid + ", _kpid" + _kpid + " This should NOT happen.");
				//FIXME; The above happens, so do a better job here... probably a short lived connection, where the SPID was reused, or a statement that wasn't long lived...
				return null;
			}

			return _batchIdMap.get(batchId);
		}

		/** Remove all BatchId entries that is no longer used */ 
		public void endOfScan()
		{
			// Cleanup entries that are older than X minutes
			// but keep at least the last entry
			for(Iterator<Entry<Integer, SpidInfoBatchIdEntry>> it = _batchIdMap.entrySet().iterator(); it.hasNext(); ) 
			{
				// Get Key/value from the iterator
				Entry<Integer, SpidInfoBatchIdEntry> entry = it.next();
				int                  batchId = entry.getKey();
				SpidInfoBatchIdEntry be      = entry.getValue();

				if(batchId < _maxBatchId) 
				{
					boolean remove = true;

					// Check if entry still exists in "SQL Text" structure, if it exists it will probably be accessed
					if (_spidSqlTextAndPlanManager.exists(be._SPID, be._KPID, be._BatchID))
						remove =false;
					
//					if (be.getAgeInMs() < 30*60*1000) // keep for maximum 30 minutes  // NOTE: hard-coded
//						remove = false;

					// if we want to keep a BatchId's (and it's content) for more than one -END-OF-SCAN- the "keepCounter" can be used.
					// Note: The initial value for the keepCounter is set in BatchIdEntry class
//					if (be._keepCounter > 0)
//					{
//						be._keepCounter--;
//						remove = false;
//					}
					
					if (remove)
					{
						//System.out.println("    <-- EOS: SpidInfoEntry[spid="+_spid+", kpid="+_kpid+", maxBatchId="+_maxBatchId+"] -- AGE removing: batchId=" + entry.getValue()._BatchID + ", msAge=" + entry.getValue().getAgeInMs() + ", entry=" + entry.getValue());
						it.remove();
					}
				}
			}
			
			// if we have MORE that X batch entries... remove oldest
			for(Iterator<Entry<Integer, SpidInfoBatchIdEntry>> it = _batchIdMap.entrySet().iterator(); it.hasNext(); ) 
			{
				Entry<Integer, SpidInfoBatchIdEntry> entry = it.next();

				if (_batchIdMap.size() > 50) // NOTE: hard-coded
				{
					//System.out.println("    <-- EOS: SpidInfoEntry[spid="+_spid+", kpid="+_kpid+", maxBatchId="+_maxBatchId+"] -- SIZE [maxSize=30, currentSize=" + _batchIdMap.size() + "] removing: batchId=" + entry.getValue()._BatchID + ", msAge=" + entry.getValue().getAgeInMs() + ", entry="+entry.getValue());
					it.remove();
				}
			}
		} // end: end-of-scan

//		public void clear()
//		{
//			_batchIdMap = new LinkedHashMap<>();
//		}
//
//		public int size()
//		{
//			if (_batchIdMap == null)
//				return 0;
//
//			return _batchIdMap.size();
//		}

		public boolean isEmpty()
		{
			if (_batchIdMap == null)
				return false;
			
			return _batchIdMap.isEmpty();
		}
	}

	//----------------------------------------------------------------------------------------
	private static class SpidInfoBatchIdEntry
	{
//		SpidInfoEntry _parent;

		// if we want to keep a BatchId's (and it's content) for more than one -END-OF-SCAN- the "keepCounter" can be used.
		// This is decremented in SpidEntry.endOfScan(), and when it reaches 0 it is removed.
//		int _keepCounter = _default_batchIdEntry_keepCount;

		long _addTime = System.currentTimeMillis();

		
		Timestamp  _sampleTime         ; // java.sql.Types.TIMESTAMP datetime          -none-               
		int        _SPID               ; // java.sql.Types.INTEGER   int               master.dbo.monProcess
		int        _KPID               ; // java.sql.Types.INTEGER   int               master.dbo.monProcess
		int        _BatchID            ; // java.sql.Types.INTEGER   int               master.dbo.monProcess
		int        _ContextID          ; // java.sql.Types.INTEGER   int               master.dbo.monProcess
		int        _LineNumber         ; // java.sql.Types.INTEGER   int               master.dbo.monProcess
		int        _SecondsConnected   ; // java.sql.Types.INTEGER   int               master.dbo.monProcess
		String     _Command            ; // java.sql.Types.VARCHAR   varchar(30)       master.dbo.monProcess
		int        _SecondsWaiting     ; // java.sql.Types.INTEGER   int               master.dbo.monProcess
		int        _WaitEventID        ; // java.sql.Types.SMALLINT  smallint          master.dbo.monProcess
		int        _BlockingSPID       ; // java.sql.Types.INTEGER   int               master.dbo.monProcess
		int        _BlockingKPID       ; 
		int        _BlockingBatchID    ;
		int        _BlockingXLOID      ; // java.sql.Types.INTEGER   int               master.dbo.monProcess
		int        _NumChildren        ; // java.sql.Types.INTEGER   int               master.dbo.monProcess
		String     _Login              ; // java.sql.Types.VARCHAR   varchar(30)       master.dbo.monProcess
		String     _DBName             ; // java.sql.Types.VARCHAR   varchar(30)       master.dbo.monProcess
		String     _Application        ; // java.sql.Types.VARCHAR   varchar(30)       master.dbo.monProcess
		String     _HostName           ; // java.sql.Types.VARCHAR   varchar(30)       master.dbo.monProcess
		String     _MasterTransactionID; // java.sql.Types.VARCHAR   varchar(255)      master.dbo.monProcess		

		String     _SqlText;		
		String     _BlockingSqlText;		

		String     _snapWaitTimeDetails; // varchar(1024) -- If we want to stuff all the above in a Table it might be interesting to see the "approximate" Wait events that was consumed  


//		public static String getKey(int spid, int kpid, int batchId)
//		{
//			return spid + "|" + kpid + "|" + batchId;
//		}
//		public String getKey()
//		{
//			//return _SPID + "|" + _KPID + "|" + _BatchID;
//			return getKey(_SPID, _KPID, _BatchID);
//		}
		
		@Override
		public String toString()
		{
			return super.toString() 
    			+ ": sampleTime         ".trim() + "='" + _sampleTime          + "'"
    			+ ", SPID               ".trim() + "="  + _SPID               
    			+ ", KPID               ".trim() + "="  + _KPID               
    			+ ", BatchID            ".trim() + "="  + _BatchID            
    			+ ", ContextID          ".trim() + "="  + _ContextID          
    			+ ", LineNumber         ".trim() + "="  + _LineNumber         
    			+ ", SecondsConnected   ".trim() + "="  + _SecondsConnected         
    			+ ", Command            ".trim() + "='" + _Command             + "'"
    			+ ", SecondsWaiting     ".trim() + "="  + _SecondsWaiting     
    			+ ", WaitEventID        ".trim() + "="  + _WaitEventID        
    			+ ", BlockingSPID       ".trim() + "="  + _BlockingSPID       
    			+ ", BlockingKPID       ".trim() + "="  + _BlockingKPID       
    			+ ", BlockingBatchID    ".trim() + "="  + _BlockingBatchID    
    			+ ", BlockingXLOID      ".trim() + "="  + _BlockingXLOID      
    			+ ", NumChildren        ".trim() + "="  + _NumChildren        
    			+ ", Login              ".trim() + "='" + _Login               + "'"
    			+ ", DBName             ".trim() + "='" + _DBName              + "'"
    			+ ", Application        ".trim() + "='" + _Application         + "'"
    			+ ", HostName           ".trim() + "='" + _HostName            + "'"
    			+ ", MasterTransactionID".trim() + "='" + _MasterTransactionID + "'"
    			+ ", SqlText            ".trim() + "='" + _SqlText             + "'"
    			+ ", BlockingSqlText    ".trim() + "='" + _BlockingSqlText     + "'"
    			+ "";
		}

		public long getAgeInMs()
		{
			return System.currentTimeMillis() - _addTime;
		}


		/** How many records is in the "getPcsRow()" ... which is used to allocate List size */ 
		int _numberOfCol = 24;

		public List<Object> getPcsRow()
		{
			List<Object> row = new ArrayList<>(_numberOfCol);

			row.add(MON_CAP_SPID_INFO);
			row.add(_sampleTime         );
			row.add(_SPID               );
			row.add(_KPID               );
			row.add(_BatchID            );
			row.add(_ContextID          );
			row.add(_LineNumber         );
			row.add(_SecondsConnected   );
			row.add(_Command            );
			row.add(_SecondsWaiting     );
			row.add(_WaitEventID        );
			row.add(_BlockingSPID       );
			row.add(_BlockingKPID       );
			row.add(_BlockingBatchID    );
			row.add(_BlockingXLOID      );
			row.add(_NumChildren        );
			row.add(_Login              );
			row.add(_DBName             );
			row.add(_Application        );
			row.add(_HostName           );
			row.add(_MasterTransactionID);

			row.add(_snapWaitTimeDetails);
			row.add(StringUtil.truncate(_SqlText        , SQL_TEXT_LEN, true, null));
			row.add(StringUtil.truncate(_BlockingSqlText, SQL_TEXT_LEN, true, null));

			return row;
		}
		
		public static final int SQL_TEXT_LEN = 4000;
	}
	




	//----------------------------------------------------------------------------------------
	private static class WaitInfo
	{
		private Map<Integer, WaitSpidEntry> _spidEntries = new HashMap<>();

		/**
		 * Add or update a WaitEventID
		 * 
		 * @param passedEntry
		 */
		public void addOrUpdate(WaitEventEntry passedEntry)
		{
			WaitSpidEntry spidEntry = _spidEntries.get(passedEntry._SPID);

			if (spidEntry == null || (spidEntry != null && spidEntry._KPID != passedEntry._KPID) )
			{
				spidEntry = new WaitSpidEntry();
				spidEntry._SPID = passedEntry._SPID;
				spidEntry._KPID = passedEntry._KPID;

				_spidEntries.put(spidEntry._SPID, spidEntry);
			}

			WaitEventEntry waitEventEntry = spidEntry._WaitEventID.get(passedEntry._WaitEventID);
			if (waitEventEntry == null)
			{
//System.out.println("+++ NEW EventId was passed: SPID=" + passedEntry._SPID + ", KPID=" + passedEntry._KPID + ", WaitEventID=" + passedEntry._WaitEventID);

				// Add and stop
				spidEntry._WaitEventID.put(passedEntry._WaitEventID, passedEntry);

				return;
			}
			else
			{
				// Diff Calc
				waitEventEntry._Waits_diff    = passedEntry._Waits_abs   - waitEventEntry._Waits_abs;
				waitEventEntry._WaitTime_diff = passedEntry._WaitTime_abs- waitEventEntry._WaitTime_abs;
//System.out.println("~~~ UPD EventId was passed: SPID=" + passedEntry._SPID + ", KPID=" + passedEntry._KPID + ", WaitEventID=" + passedEntry._WaitEventID + ", pWaits=" + passedEntry._Waits_abs + ", eWaits=" + waitEventEntry._Waits_abs + " [" + waitEventEntry._Waits_diff + "], pMs=" + passedEntry._WaitTime_abs + ", eMs=" + waitEventEntry._WaitTime_abs + " [" + waitEventEntry._WaitTime_diff + "]. ");

				// set new ABS
				waitEventEntry._Waits_abs    = passedEntry._Waits_abs;
				waitEventEntry._WaitTime_abs = passedEntry._WaitTime_abs;
				
				// Increment diff batchStart
				waitEventEntry._Waits_diff_batchStart    += waitEventEntry._Waits_diff;
				waitEventEntry._WaitTime_diff_batchStart += waitEventEntry._WaitTime_diff;

				// when a NEW BatchId, fix some "batch start" values
				if (waitEventEntry._snapshotBatchID != passedEntry._snapshotBatchID)
				{
					waitEventEntry._Waits_diff_batchStart    = waitEventEntry._Waits_diff;
					waitEventEntry._WaitTime_diff_batchStart = waitEventEntry._WaitTime_diff;
				}
				
				// When was this entry created/updated
				waitEventEntry._updateTime = passedEntry._updateTime;
				waitEventEntry._sampleTime = passedEntry._sampleTime;

				// At what BatchID are we at
				waitEventEntry._snapshotBatchID = passedEntry._snapshotBatchID;
			}
		}

		/**
		 * Get a wait event ID
		 * @param sPID
		 * @param kPID
		 * @param waitEventID
		 * @return
		 */
		public WaitEventEntry getWaitEventId(int spid, int kpid, int waitEventID)
		{
			WaitSpidEntry spidEntry = _spidEntries.get(spid);

			if (spidEntry == null || (spidEntry != null && spidEntry._KPID != kpid) )
				return null;

			WaitEventEntry waitEventEntry = spidEntry._WaitEventID.get(waitEventID);
			return waitEventEntry;
		}

		public void removeSpids(Set<Integer> removedSpids)
		{
			_spidEntries.keySet().removeAll(removedSpids);
		}

		/**
		 * Get a JSON text with <code>[{"id":250,"w":##,"ms":##},{"id":251,"w":##,"ms":##}]</code>
		 * <p>
		 * Or as a CSV <code>250=#waits#:#ms#, 251=#waits#:#ms#</code>
		 * <p>
		 * 
		 * @param spid
		 * @param kpid
		 * @param batchId
		 * @return
		 */
		public String getWaitTimeDetailsAsJson(boolean byBatch, int spid, int kpid, int batchId)
		{
			WaitSpidEntry spidEntry = _spidEntries.get(spid);
			if (spidEntry == null)
				return null;

			// Exit if it's not what we are looking for
			if (kpid != spidEntry._KPID)
				return null;
			if (spidEntry._WaitEventID == null)
				return null;
			if (spidEntry._WaitEventID.isEmpty())
				return null;
			
			StringBuilder sb = new StringBuilder(256);
			sb.append("[");

			String comma = "";
			int addCount = 0;
			for (WaitEventEntry e : spidEntry._WaitEventID.values())
			{
				int waitsDiff    = e._Waits_diff;
				int waitTimeDiff = e._WaitTime_diff;
				if (byBatch)
				{
					waitsDiff    = e._Waits_diff_batchStart;
					waitTimeDiff = e._WaitTime_diff_batchStart;
				}
				
				if (waitsDiff > 0 || waitTimeDiff > 0)
				{
					addCount++;

					int batchBeforeOrAfter = batchId - e._snapshotBatchID;
					long sampleAgeMs = System.currentTimeMillis() - e._updateTime;

					sb.append(comma);
					sb.append("{");
					sb.append( "\"id\":") .append(e._WaitEventID);
					sb.append(",\"cid\":").append(e._WaitClassID);
					sb.append(",\"w\":")  .append(waitsDiff);
					sb.append(",\"ms\":") .append(waitTimeDiff);
					sb.append(",\"bs\":") .append(e._snapshotBatchID);
					sb.append(",\"bd\":") .append(batchBeforeOrAfter);
					sb.append(",\"sa\":") .append(sampleAgeMs);
					sb.append("}");

					comma = ",";
				}
			}
			sb.append("]");

			if (addCount == 0)
			{
				//return "WaitEventEntry.size()="+spidEntry._WaitEventID.size()+"--- BUT NO CHANGES ---";
				return null;
			}

			return sb.toString();
		}

	}

	//----------------------------------------------------------------------------------------
	private static class WaitSpidEntry
	{
		int _SPID;
		int _KPID;
		
		Map<Integer, WaitEventEntry> _WaitEventID = new HashMap<>();
	}

	//----------------------------------------------------------------------------------------
	private static class WaitEventEntry
	{
		int _SPID;
		int _KPID;
		int _WaitEventID;
		int _WaitClassID;

		int _Waits_abs;
		int _WaitTime_abs;

		int _Waits_diff;    // diff since previous sample
		int _WaitTime_diff; // diff since previous sample
		
		int _Waits_diff_batchStart;     // diff since batch started
		int _WaitTime_diff_batchStart;  // diff since batch started

		int  _snapshotBatchID;
		Timestamp _sampleTime;
		long      _updateTime; // internally updated when "diff" counters was updated (since _sampleTime "may" be in another time-zone)
		

		public WaitEventEntry(Timestamp sampleTime, int spid, int kpid, int waitEventId, int waits_abs, int waitTime_abs, int snapshotBatchId, int secondsConnected)
		{
			_updateTime      = System.currentTimeMillis();

			_sampleTime      = sampleTime;
			_SPID            = spid;
			_KPID            = kpid;
			_WaitEventID     = waitEventId;
			_Waits_abs       = waits_abs;
			_WaitTime_abs    = waitTime_abs;
			_snapshotBatchID = snapshotBatchId;

			_Waits_diff               = 0;
			_WaitTime_diff            = 0;
			_Waits_diff_batchStart    = 0;
			_WaitTime_diff_batchStart = 0;

			if (secondsConnected <= 1)
			{
				_Waits_diff               = waits_abs;
				_WaitTime_diff            = waitTime_abs;
				_Waits_diff_batchStart    = waits_abs;
				_WaitTime_diff_batchStart = waitTime_abs;
			}

			// Get the WaitClassID
			_WaitClassID = -1;
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			if (mtd != null)
			{
				_WaitClassID = mtd.getWaitClassId(waitEventId);
			}
		}

		@Override
		public String toString()
		{
			return super.toString() 
	    			+ ", SPID                    ".trim() + "="  + _SPID               
	    			+ ", KPID                    ".trim() + "="  + _KPID               
	    			+ ", WaitEventID             ".trim() + "="  + _WaitEventID        
	    			+ ", WaitClassID             ".trim() + "="  + _WaitClassID        
                                                
	    			+ ", Waits_abs               ".trim() + "="  + _Waits_abs        
	    			+ ", WaitTime_abs            ".trim() + "="  + _WaitTime_abs        

	    			+ ", Waits_diff              ".trim() + "="  + _Waits_diff        
	    			+ ", WaitTime_diff           ".trim() + "="  + _WaitTime_diff        

	    			+ ", Waits_diff_batchStart   ".trim() + "="  + _Waits_diff_batchStart        
	    			+ ", WaitTime_diff_batchStart".trim() + "="  + _WaitTime_diff_batchStart        

	    			+ ", snapshotBatchID         ".trim() + "="  + _snapshotBatchID        
	    			+ ", sampleTime              ".trim() + "='" + _sampleTime + "'"
	    			+ "";
		}

		/** How many records is in the "getPcsRow()" ... which is used to allocate List size */ 
		int _numberOfCol = 10;

		public List<Object> getPcsRow()
		{
			if (_Waits_diff == 0 && _WaitTime_diff == 0)
				return null;
			
			List<Object> row = new ArrayList<>(_numberOfCol);

			row.add(MON_CAP_WAIT_INFO);
			row.add(_sampleTime         );
			row.add(_SPID               );
			row.add(_KPID               );
			row.add(_WaitEventID        );
			row.add(_WaitClassID        );
			
			row.add(_snapshotBatchID    );
			row.add(_Waits_abs          );
			row.add(_Waits_diff         );
			row.add(_WaitTime_abs       );
			row.add(_WaitTime_diff      );

			return row;
		}
	}
	//--------------------------------------------------------------------------
	// END: SPID - INFO/WAIT - manager 
	//--------------------------------------------------------------------------

	
	
	
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
	 * @param procedureId 
	 * @param procName 
	 * @param lineNumber 
	 * @param dbname 
	 * @param contextID 
	 */
	private void updateStatementStats(int execTime, int logicalReads, int physicalReads, int cpuTime, int waitTime, int rowsAffected, int errorStatus, int procedureId, String procName, int lineNumber, String dbname)
	{
		if (_statementStatistics == null)
			return;
		
		_statementStatistics.addStatementStats(execTime, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, procedureId, procName, lineNumber, dbname);
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

	/** This will be NULL until we call getSqlTextStats() for the first time. */
	private SqlCaptureSqlTextStatisticsSample _sqlTextStatistics = null;

	//--------------------------------------------------------------------------
	// END: SQL Text Statistics
	//--------------------------------------------------------------------------

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

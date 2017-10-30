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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.cache.XmlPlanCache;
import com.asetune.pcs.PersistWriterBase;
import com.asetune.pcs.PersistentCounterHandler;
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

	private boolean _clearBeforeFirstPoll = PersistentCounterHandler.DEFAULT_sqlCap_clearBeforeFirstPoll; // initialized in init(Configuration conf)
	private boolean _firstPoll = true;
	
//	private int     _sendSizeThreshold = PersistentCounterHandler.DEFAULT_sqlCap_sendSizeThreshold; // initialized in init(Configuration conf)

	private boolean _isNonConfiguredMonitoringAllowed = PersistentCounterHandler.DEFAULT_sqlCap_isNonConfiguredMonitoringAllowed;
	
	private static final String MON_SQL_TEXT      = PersistWriterBase.getTableName(PersistWriterBase.SQL_CAPTURE_SQLTEXT,    null, false); // "MonSqlCapSqlText";
	private static final String MON_SQL_STATEMENT = PersistWriterBase.getTableName(PersistWriterBase.SQL_CAPTURE_STATEMENTS, null, false); // "MonSqlCapStatements";
	private static final String MON_SQL_PLAN      = PersistWriterBase.getTableName(PersistWriterBase.SQL_CAPTURE_PLANS,      null, false); // "MonSqlCapPlans";

	private static final List<String> _storegeTableNames = Arrays.asList(MON_SQL_TEXT, MON_SQL_STATEMENT, MON_SQL_PLAN);

	private String _sql_sqlText       = null;
	private String _sql_sqlStatements = null; 
	private String _sql_sqlPlanText   = null;

	private boolean _sampleSqlText    = true;
	private boolean _sampleStatements = true;
	private boolean _samplePlan       = false;
	
	private long    _nonConfiguredMonitoringCount = 0;

	private int _stmnt_SPID_pos    = 2 + 1; // 2 = ListPos + 1 is for that the row starts with a string that contains the DestinationTablename
	private int _stmnt_BatchID_pos = 7 + 1; // 7 = ListPos + 1 is for that the row starts with a string that contains the DestinationTablename

//	private SqlCaptureDetails _sqlCaptureDetails = new SqlCaptureDetails();

	@Override
	public void init(Configuration conf)
	{
		super.init(conf);
		
		_clearBeforeFirstPoll             = getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_clearBeforeFirstPoll,             PersistentCounterHandler.DEFAULT_sqlCap_clearBeforeFirstPoll);
//		_sendSizeThreshold                = getIntProperty    (PersistentCounterHandler.PROPKEY_sqlCap_sendSizeThreshold,                PersistentCounterHandler.DEFAULT_sqlCap_sendSizeThreshold);
		_isNonConfiguredMonitoringAllowed = getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_isNonConfiguredMonitoringAllowed, PersistentCounterHandler.DEFAULT_sqlCap_isNonConfiguredMonitoringAllowed);
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
							
							return null;
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
	public String getTableDdlString(DatabaseMetaData dbmd, String tabName)
	{
		String qic = PersistWriterBase.qic;
		
		if (MON_SQL_TEXT.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();

			sbSql.append("create table " + tabName + "\n");
			sbSql.append("( \n");
			sbSql.append("    "+fill(qic+"sampleTime"       +qic,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"InstanceID"       +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"SPID"             +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"KPID"             +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"BatchID"          +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"ServerLogin"      +qic,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"JavaSqlLength"    +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"JavaSqlHashCode"  +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"SQLText"          +qic,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true) +"\n");
//			sbSql.append("\n");
//			sbSql.append("   ,PRIMARY KEY ("+qic+"SPID"+qic+", "+qic+"KPID"+qic+", "+qic+"InstanceID"+qic+", "+qic+"BatchID"+qic+")\n");
			sbSql.append(") \n");

			return sbSql.toString();
		}

		if (MON_SQL_STATEMENT.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();

			sbSql.append("create table " + tabName + "\n");
			sbSql.append("( \n");
			sbSql.append("    "+fill(qic+"sampleTime"           +qic,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"InstanceID"           +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"SPID"                 +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n"); // NOTE If this pos is changed: alter _stmnt_SPID_pos
			sbSql.append("   ,"+fill(qic+"KPID"                 +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"DBID"                 +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"ProcedureID"          +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"PlanID"               +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"BatchID"              +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n"); // NOTE If this pos is changed: alter _stmnt_BatchID_pos
			sbSql.append("   ,"+fill(qic+"ContextID"            +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"LineNumber"           +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"ObjOwnerID"           +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"DBName"               +qic,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"HashKey"              +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"SsqlId"               +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"ProcName"             +qic,40)+" "+fill(getDatatype("varchar",  255,-1,-1),20)+" "+getNullable(true)+"\n"); // NULLABLE
			sbSql.append("   ,"+fill(qic+"Elapsed_ms"           +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"CpuTime"              +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"WaitTime"             +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"MemUsageKB"           +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"PhysicalReads"        +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"LogicalReads"         +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"RowsAffected"         +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"ErrorStatus"          +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"ProcNestLevel"        +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"StatementNumber"      +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"QueryOptimizationTime"+qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"PagesModified"        +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"PacketsSent"          +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"PacketsReceived"      +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"NetworkPacketSize"    +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"PlansAltered"         +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"StartTime"            +qic,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"EndTime"              +qic,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"JavaSqlHashCode"      +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
//			sbSql.append("\n");
//			sbSql.append("   ,PRIMARY KEY ("+qic+"SPID"+qic+", "+qic+"KPID"+qic+", "+qic+"InstanceID"+qic+", "+qic+"BatchID"+qic+")\n");
			sbSql.append(") \n");

			return sbSql.toString();
		}

		if (MON_SQL_PLAN.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();
			
			sbSql.append("create table " + tabName + "\n");
			sbSql.append("( \n");
			sbSql.append("    "+fill(qic+"sampleTime"       +qic,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"InstanceID"       +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"SPID"             +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"KPID"             +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"PlanID"           +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"BatchID"          +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"ContextID"        +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"DBID"             +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"DBName"           +qic,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"ProcedureID"      +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"PlanText"         +qic,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true) +"\n");
//			sbSql.append("\n");
//			sbSql.append("   ,PRIMARY KEY ("+qic+"SPID"+qic+", "+qic+"KPID"+qic+", "+qic+"InstanceID"+qic+", "+qic+"BatchID"+qic+")\n");
			sbSql.append(") \n");

			return sbSql.toString();
		}

		return null;
	}

	private String dbmsQuotify(String qic, String...names)
	{
		StringBuilder sb = new StringBuilder();

		for (String name : names)
		{
			sb.append(qic).append(name).append(qic).append(", ");
		}
		// Remove last ", "
		sb.delete(sb.length()-2, sb.length());

		return sb.toString();
	}

	@Override
	public List<String> getIndexDdlString(DatabaseMetaData dbmd, String tabName)
	{
		// NOTE: The DatabaseMetaData is to the PCS Writer/Storage Connection
		String dbmsProductName = "unknown";
		String qic             = "\"";
		try { dbmsProductName = dbmd.getDatabaseProductName();   } catch(SQLException ex) { _logger.warn("Problems getting 'dbmd.getDatabaseProductName()', Caught: "+ex); }
		try { qic             = dbmd.getIdentifierQuoteString(); } catch(SQLException ex) { _logger.warn("Problems getting 'dbmd.getIdentifierQuoteString()', Caught: "+ex); }
		
		String iQic = qic; // indexNameQic
		if ( DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_SYBASE_ASE) )
			iQic = "";

		// Put indexes in this list that will be returned
		List<String> list = new ArrayList<>();

		if (MON_SQL_TEXT.equals(tabName))
		{
			list.add("create index " + dbmsQuotify(iQic, tabName+"_ix1") + " on " + dbmsQuotify(qic, tabName) + "(" + dbmsQuotify(qic, "BatchID", "SPID", "KPID") + ")\n");
		}

		if (MON_SQL_STATEMENT.equals(tabName))
		{
			list.add("create index " + dbmsQuotify(iQic, tabName+"_ix1") + " on " + dbmsQuotify(qic, tabName) + "(" + dbmsQuotify(qic, "BatchID", "SPID", "KPID") + ")\n");
			list.add("create index " + dbmsQuotify(iQic, tabName+"_ix2") + " on " + dbmsQuotify(qic, tabName) + "(" + dbmsQuotify(qic, "StartTime", "EndTime")    + ")\n");
		}

		if (MON_SQL_PLAN.equals(tabName))
		{
			list.add("create index " + dbmsQuotify(iQic, tabName+"_ix1") + " on " + dbmsQuotify(qic, tabName) + "(" + dbmsQuotify(qic, "BatchID", "SPID", "KPID") + ")\n");
		}

		return list;
	}

	@Override
	public String getInsertStatement(String tabName)
	{
		String qic = PersistWriterBase.qic;
		
		if (MON_SQL_TEXT.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();

			sbSql.append("insert into ").append(tabName);
			sbSql.append("(");
			sbSql.append(" ").append(qic).append("sampleTime"     ).append(qic); // 1
			sbSql.append(",").append(qic).append("InstanceID"     ).append(qic); // 2
			sbSql.append(",").append(qic).append("SPID"           ).append(qic); // 3
			sbSql.append(",").append(qic).append("KPID"           ).append(qic); // 4
			sbSql.append(",").append(qic).append("BatchID"        ).append(qic); // 5
			sbSql.append(",").append(qic).append("ServerLogin"    ).append(qic); // 6
			sbSql.append(",").append(qic).append("JavaSqlLength"  ).append(qic); // 7
			sbSql.append(",").append(qic).append("JavaSqlHashCode").append(qic); // 8
			sbSql.append(",").append(qic).append("SQLText"        ).append(qic); // 9
			sbSql.append(") \n");
			sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?) \n"); // 9 question marks
			//                   1, 2, 3, 4, 5, 6, 7, 8, 9

			return sbSql.toString();
		}

		if (MON_SQL_STATEMENT.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();

			sbSql.append("insert into ").append(tabName);
			sbSql.append("(");
			sbSql.append(" ").append(qic).append("sampleTime"           ).append(qic);  //  1
			sbSql.append(",").append(qic).append("InstanceID"           ).append(qic);  //  2
			sbSql.append(",").append(qic).append("SPID"                 ).append(qic);  //  3
			sbSql.append(",").append(qic).append("KPID"                 ).append(qic);  //  4
			sbSql.append(",").append(qic).append("DBID"                 ).append(qic);  //  5
			sbSql.append(",").append(qic).append("ProcedureID"          ).append(qic);  //  6
			sbSql.append(",").append(qic).append("PlanID"               ).append(qic);  //  7
			sbSql.append(",").append(qic).append("BatchID"              ).append(qic);  //  8
			sbSql.append(",").append(qic).append("ContextID"            ).append(qic);  //  9
			sbSql.append(",").append(qic).append("LineNumber"           ).append(qic);  // 10
			sbSql.append(",").append(qic).append("ObjOwnerID"           ).append(qic);  // 11
			sbSql.append(",").append(qic).append("DBName"               ).append(qic);  // 12
			sbSql.append(",").append(qic).append("HashKey"              ).append(qic);  // 13
			sbSql.append(",").append(qic).append("SsqlId"               ).append(qic);  // 14
			sbSql.append(",").append(qic).append("ProcName"             ).append(qic);  // 15
			sbSql.append(",").append(qic).append("Elapsed_ms"           ).append(qic);  // 16
			sbSql.append(",").append(qic).append("CpuTime"              ).append(qic);  // 17
			sbSql.append(",").append(qic).append("WaitTime"             ).append(qic);  // 18
			sbSql.append(",").append(qic).append("MemUsageKB"           ).append(qic);  // 19
			sbSql.append(",").append(qic).append("PhysicalReads"        ).append(qic);  // 20
			sbSql.append(",").append(qic).append("LogicalReads"         ).append(qic);  // 21
			sbSql.append(",").append(qic).append("RowsAffected"         ).append(qic);  // 22
			sbSql.append(",").append(qic).append("ErrorStatus"          ).append(qic);  // 23
			sbSql.append(",").append(qic).append("ProcNestLevel"        ).append(qic);  // 24
			sbSql.append(",").append(qic).append("StatementNumber"      ).append(qic);  // 25
			sbSql.append(",").append(qic).append("QueryOptimizationTime").append(qic);  // 26
			sbSql.append(",").append(qic).append("PagesModified"        ).append(qic);  // 27
			sbSql.append(",").append(qic).append("PacketsSent"          ).append(qic);  // 28
			sbSql.append(",").append(qic).append("PacketsReceived"      ).append(qic);  // 29
			sbSql.append(",").append(qic).append("NetworkPacketSize"    ).append(qic);  // 30
			sbSql.append(",").append(qic).append("PlansAltered"         ).append(qic);  // 31
			sbSql.append(",").append(qic).append("StartTime"            ).append(qic);  // 32
			sbSql.append(",").append(qic).append("EndTime"              ).append(qic);  // 33
			sbSql.append(",").append(qic).append("JavaSqlHashCode"      ).append(qic);  // 34
			sbSql.append(") \n");
			sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n"); // 32 question marks
			//                   1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34

			return sbSql.toString();
		}

		if (MON_SQL_PLAN.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();

			sbSql.append("insert into ").append(tabName);
			sbSql.append("(");
			sbSql.append(" ").append(qic).append("sampleTime" ).append(qic); // 1
			sbSql.append(",").append(qic).append("InstanceID" ).append(qic); // 2
			sbSql.append(",").append(qic).append("SPID"       ).append(qic); // 3
			sbSql.append(",").append(qic).append("KPID"       ).append(qic); // 4
			sbSql.append(",").append(qic).append("PlanID"     ).append(qic); // 5
			sbSql.append(",").append(qic).append("BatchID"    ).append(qic); // 6
			sbSql.append(",").append(qic).append("ContextID"  ).append(qic); // 7
			sbSql.append(",").append(qic).append("DBID"       ).append(qic); // 8
			sbSql.append(",").append(qic).append("DBName"     ).append(qic); // 9
			sbSql.append(",").append(qic).append("ProcedureID").append(qic); // 10
			sbSql.append(",").append(qic).append("PlanText"   ).append(qic); // 11
			sbSql.append(") \n");
			sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n"); // 11 question marks
			//                   1, 2, 3, 4, 5, 6, 7, 8, 9,10,11

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

	private void checkConfig(DbxConnection conn)
	{
		boolean doSqlText       = getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_doSqlText,       PersistentCounterHandler.DEFAULT_sqlCap_doSqlText);
		boolean doStatementInfo = getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_doStatementInfo, PersistentCounterHandler.DEFAULT_sqlCap_doStatementInfo);
		boolean doPlanText      = getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_doPlanText,      PersistentCounterHandler.DEFAULT_sqlCap_doPlanText);

		try
		{
			int sqlTextPipeActive        = AseConnectionUtils.getAseConfigRunValue(conn, "sql text pipe active");
			int sqlTextPipeMaxMessages   = AseConnectionUtils.getAseConfigRunValue(conn, "sql text pipe max messages");
			
			int statementPipeActive      = AseConnectionUtils.getAseConfigRunValue(conn, "statement pipe active");
			int statementPipeMaxMessages = AseConnectionUtils.getAseConfigRunValue(conn, "statement pipe max messages");
			
			int planTextPipeActive       = AseConnectionUtils.getAseConfigRunValue(conn, "plan text pipe active");
			int planTextPipeMaxMessages  = AseConnectionUtils.getAseConfigRunValue(conn, "plan text pipe max messages");
			
			_sampleSqlText    = doSqlText       && sqlTextPipeActive   > 0 && sqlTextPipeMaxMessages   > 0;
			_sampleStatements = doStatementInfo && statementPipeActive > 0 && statementPipeMaxMessages > 0;
			_samplePlan       = doPlanText      && planTextPipeActive  > 0 && planTextPipeMaxMessages  > 0;
			
			_logger.info("ASE 'sql text pipe'  configuration: 'sql text pipe active'="  + sqlTextPipeActive   + ", 'sql text pipe max messages'="  + sqlTextPipeMaxMessages   + ".");
			_logger.info("ASE 'statement pipe' configuration: 'statement pipe active'=" + statementPipeActive + ", 'statement pipe max messages'=" + statementPipeMaxMessages + ".");
			_logger.info("ASE 'plan text pipe' configuration: 'plan text pipe active'=" + planTextPipeActive  + ", 'plan text pipe max messages'=" + planTextPipeMaxMessages  + ".");
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
		
		int aseVersion = conn.getDbmsVersionNumber();

		// Get any specific where clause for the monSysStatements
		String statementWhereClause = getProperty(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_whereClause, PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_whereClause);
		if (StringUtil.hasValue(statementWhereClause))
			_logger.info("Applying user defined where clause when sampling monSysStatements. extra where clause appended is '"+statementWhereClause+"'.");
		else
			statementWhereClause = "1 = 1";

		String RowsAffected          = "RowsAffected = convert(int, -1), ";
		String ErrorStatus           = "ErrorStatus = convert(int, -1), ";
		String HashKey               = "HashKey = convert(int, -1), ";
		String SsqlId                = "SsqlId = convert(int, -1), ";
		String DBName                = "DBName=db_name(DBID), ";
		String ProcNestLevel         = "ProcNestLevel = convert(int, -1), ";
		String StatementNumber       = "StatementNumber = convert(int, -1), ";
		String InstanceID            = "InstanceID = convert(int, -1), ";
		String ProcName              = "ProcName = isnull(isnull(object_name(ProcedureID,DBID),object_name(ProcedureID,2)),object_name(ProcedureID,db_id('sybsystemprocs'))), \n";
		String ObjOwnerID            = "ObjOwnerID = convert(int, 0), \n";
		String QueryOptimizationTime = "QueryOptimizationTime = convert(int, -1), ";

		String ServerLogin           = "ServerLogin = convert(varchar(30), '-1'), \n";

		if (aseVersion >= Ver.ver(15,0,0,2) || (aseVersion >= Ver.ver(12,5,4) && aseVersion < Ver.ver(15,0)) )
		{
			RowsAffected    = "RowsAffected, ";
			ErrorStatus     = "ErrorStatus, ";
		}
		if (aseVersion >= Ver.ver(12,5,3))
		{
			ServerLogin     = "ServerLogin = suser_name(ServerUserID), ";
		}
		if (aseVersion >= Ver.ver(15,0,2))
		{
			HashKey         = "HashKey, ";
			SsqlId          = "SsqlId, ";
//			ProcName        = "ProcName = CASE WHEN SsqlId > 0 THEN object_name(SsqlId,2) ELSE isnull(object_name(ProcedureID,DBID), object_name(ProcedureID,db_id('sybsystemprocs'))) END, \n";
			ProcName        = "ProcName = CASE WHEN SsqlId > 0 THEN object_name(SsqlId,2) ELSE isnull(isnull(object_name(ProcedureID,DBID),object_name(ProcedureID,2)),object_name(ProcedureID,db_id('sybsystemprocs'))) END, \n"; // *sq dynamic SQL (ct_dynamic/prepared_stmnt) does NOT set the SsqlId column
		}
		if (aseVersion >= Ver.ver(15,0,2,3))
		{
			DBName          = "DBName, ";
		}
		if (aseVersion >= Ver.ver(15,0,3))
		{
			ProcNestLevel   = "ProcNestLevel, ";
			StatementNumber = "StatementNumber, ";
			ObjOwnerID      = "ObjOwnerID = CASE WHEN SsqlId > 0 THEN 0 ELSE object_owner_id(ProcedureID, DBID) END,";
		}
		if (aseVersion >= Ver.ver(15,5))
		{
			InstanceID      = "InstanceID, ";
		}
		
		// ASE 16.0 SP3
		if (aseVersion >= Ver.ver(16,0,0, 3)) // 16.0 SP3
		{
			QueryOptimizationTime       = "QueryOptimizationTime, ";
		}
		
		_sql_sqlText 
			= "select sampleTime = getdate(), \n"
			+ "    "+InstanceID+"\n"
			+ "    SPID, \n"
			+ "    KPID, \n"
			+ "    BatchID, \n"
			+ "    SequenceInBatch, \n"
			+ "    "+ServerLogin+"\n"
			+ "    JavaSqlLength = convert(int, -1),\n"
			+ "    JavaSqlHashCode = convert(int, -1),\n"
			+ "    SQLText \n"
			+ "from master.dbo.monSysSQLText \n"
			+ "where 1 = 1 \n"
			+ "  and SPID != @@spid \n"
			+ "  and SPID not in (select spid from master.dbo.sysprocesses where program_name like '"+Version.getAppName()+"%') \n"
			+ "order by SPID, BatchID, SequenceInBatch \n" // TODO: make this sort internal/after the rows has been fetched for less server side impact
			+ "";

		_sql_sqlStatements 
			= "select \n"
			+ "    sampleTime = getdate(), \n"
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
			+ "    Elapsed_ms = CASE WHEN datediff(day, StartTime, EndTime) >= 24 THEN -1 ELSE  datediff(ms, StartTime, EndTime) END, \n"
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
			+ "    JavaSqlHashCode = convert(int, -1)\n"
			+ "from master.dbo.monSysStatement \n"
			+ "where 1 = 1 \n"
			+ "  and SPID != @@spid \n"
			+ "  and SPID not in (select spid from master.dbo.sysprocesses where program_name like '"+Version.getAppName()+"%') "
			+ "  and " + statementWhereClause
			+ "";
		
		_sql_sqlPlanText 
			= "select sampleTime = getdate(), \n"
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
			+ "    PlanText \n"
			+ "from master.dbo.monSysPlanText \n"
			+ "where 1 = 1 \n"
			+ "  and SPID != @@spid \n"
			+ "  and SPID not in (select spid from master.dbo.sysprocesses where program_name like '"+Version.getAppName()+"%') \n"
			+ "order by SPID, BatchID, SequenceNumber \n" // TODO: make this sort internal/after the rows has been fetched for less server side impact
			+ "";
	}

	/**
	 * Primary Key for SqlText, Statements, PlanText
	 * @author gorans
	 */
	private class PK
	{
		int SPID;
		int BatchID;
		
		public PK(int SPID, int BatchID)
		{
			this.SPID    = SPID;
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
			
			return SPID == other.SPID && BatchID == other.BatchID; 
		}

//		// Genereated from Eclipse
//		private SqlCaptureBrokerAse getOuterType()
//		{
//			return SqlCaptureBrokerAse.this;
//		}
		
		@Override
		public String toString()
		{
			return "PK [SPID=" + SPID + ", BatchID=" + BatchID + "]" + "@" + Integer.toHexString(hashCode());
		}
	}

	@Override
	public int doSqlCapture(DbxConnection conn, PersistentCounterHandler pch)
	{
		if (_firstPoll && _clearBeforeFirstPoll)
		{
			// If first time... discard everything in the transient monSysStatement table
			if (_sampleSqlText)    clearTable(conn, "monSysSQLText");
			if (_sampleStatements) clearTable(conn, "monSysStatement");
			if (_samplePlan)       clearTable(conn, "monSysPlanText");

			_firstPoll = false;
		}


		int addCount = 0;
		int statementReadCount = 0;
		int statementAddCount = 0;
		int statementDiscardCount = 0;
		
		// 
		Map<PK, List<Object>> sqlTextPkMap   = new HashMap<>();
		List<List<Object>>    sqlTextRecords = new ArrayList<>();

		Map<PK, List<Object>> planTextPkMap   = new HashMap<>();
		List<List<Object>>    planTextRecords = new ArrayList<>();

		List<List<Object>> statementRecords = new ArrayList<>();
		Set<PK> statementPkAdded            = new HashSet<>();
		Set<PK> statementPkDiscarded        = new HashSet<>();

		
		// If SQL queries are NOT initialized, do it now
		if (_sql_sqlText == null)
			setSql(conn);

		//------------------------------------------------
		// SQL STATEMENTS
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

			try
			{
				Statement stmnt = conn.createStatement();
				ResultSet rs = stmnt.executeQuery(_sql_sqlStatements);
				int colCount = rs.getMetaData().getColumnCount();
				while(rs.next())
				{
					statementReadCount++;

					int SPID          = rs.getInt("SPID");
					int BatchID       = rs.getInt("BatchID");
					int execTime      = rs.getInt("Elapsed_ms");
					int logicalReads  = rs.getInt("LogicalReads");
					int physicalReads = rs.getInt("PhysicalReads");

					// To be used by keep/discard Set
					PK pk = new PK(SPID, BatchID);
					
					// Add only rows that are above the limits
					if (    execTime      > saveStatement_gt_execTime
					     && logicalReads  > saveStatement_gt_logicalReads
					     && physicalReads > saveStatement_gt_physicalReads
					   )
					{
						if (sendDdlForLookup)
						{
							String ProcName = rs.getString("ProcName");

							// send ProcName or StatementCacheEntry to the DDL Capture
							if (StringUtil.hasValue(ProcName))
							{
								String DBName        = rs.getString("DBName");
//								int    execTime      = rs.getInt   ("Elapsed_ms");
//								int    logicalReads  = rs.getInt   ("LogicalReads");
//								int    physicalReads = rs.getInt   ("PhysicalReads");

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
									pch.addDdl(DBName, ProcName, "SqlCapture");
								}
							}
						}

						List<Object> row = new ArrayList<Object>();
						row.add(MON_SQL_STATEMENT); // NOTE: the first object in the list should be the TableName where to store the data
						for (int c=1; c<=colCount; c++)
							row.add(rs.getObject(c));

//						addCount++;
//						addToContainer(row, pch);
						statementRecords.add(row);
statementAddCount++;
						
						// Keep track of what PK's we have added (procs can have many statements, so it can be in both add and skipp Set)
						statementPkAdded.add(pk);
					}
					else // save DISCARDED records in a "skip" list so we can remove the SqlText and PlanText at the end
					{
						// Keep track of what PK's we have discarded (procs can have many statements, so it can be in both add and skipp Set)
						statementPkDiscarded.add(pk);
statementDiscardCount++;
					}
				}
				rs.close();
				stmnt.close();
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
		//------------------------------------------------
		if (_sampleSqlText)
		{
			try
			{
				//----------------------------------------------------------------------------------------
				// This might look a bit odd / backwards, but look at the data example at the end of this file
				// SQLText can have several rows
				// * so when FIRST row (for every group) is discovered - Start a new "row" (which is a List: tableNameToStoreItIn, c1-data, c2-data, c3-data... SQLText-concatenated-data)
				// * on all "extra" rows that will just contain same c1-data, c2-data, c3-data but the SQLText will be read and appended to a StringBuilder
				// * when a "new" row is found, we will set the StringBuffer content to replace the "first row" SQLText
				// * At the END we will need to "post" the information from the last "group"
				//----------------------------------------------------------------------------------------
				Statement stmnt = conn.createStatement();
				ResultSet rs = stmnt.executeQuery(_sql_sqlText);
				ResultSetMetaData rsmd = rs.getMetaData();
				int           colCount = rsmd.getColumnCount();
				int           sequenceCol_pos = -1;
				int           sequenceCol_val = -1;
				int           seqTextCol_pos  = -1;
				StringBuilder seqTextCol_val  = new StringBuilder();
				for (int c=1; c<=colCount; c++)
				{
					if      ("SequenceInBatch".equals(rsmd.getColumnLabel(c))) sequenceCol_pos = c;
					else if ("SQLText"        .equals(rsmd.getColumnLabel(c))) seqTextCol_pos  = c;
				}
				List<Object> row = null;
				while(rs.next())
				{
					// The below implies that the data is sorted by: SPID|KPID, BatchID, SequenceInBatch
					sequenceCol_val = rs.getInt(sequenceCol_pos);
					if (sequenceCol_val == 1)
					{
						if (row != null)
						{
							row.set(row.size()-1, seqTextCol_val.toString());
							seqTextCol_val.setLength(0);

							sqlTextRecords.add(row);
//							addCount++;
//							addToContainer(row, pch);
						}

						row = new ArrayList<Object>();
						row.add(MON_SQL_TEXT); // NOTE: the first object in the list should be the TableName where to store the data
						
						// Add the ROW to the sqlTextPkMap so we can delete it at the end if not used...
						int SPID    = rs.getInt("SPID");
						int BatchID = rs.getInt("BatchID");

						sqlTextPkMap.put(new PK(SPID, BatchID), row);
						
						for (int c=1; c<=colCount; c++)
						{
							// the sequence column should NOT be part of the result
							if (c != sequenceCol_pos)
								row.add(rs.getObject(c));
						}
						seqTextCol_val.append( rs.getString(seqTextCol_pos) );
					}
					else if (row != null)
					{
						seqTextCol_val.append( rs.getString(seqTextCol_pos) );
					}
				}
				// Finally add content from LAST row, since we do addCountainer() only on NEW sequenceCol_val == 1
				if (row != null)
				{
					row.set(row.size()-1, seqTextCol_val.toString());
					seqTextCol_val.setLength(0);

					sqlTextRecords.add(row);
//					addCount++;
//					addToContainer(row, pch);
				}
				
				rs.close();
				stmnt.close();
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
		//------------------------------------------------
		if (_samplePlan)
		{
			try
			{
				//----------------------------------------------------------------------------------------
				// This might look a bit odd / backwards, but look at the data example at the end of this file
				// PlanText can have several rows
				// * so when FIRST row (for every group) is discovered - Start a new "row" (which is a List: tableNameToStoreItIn, c1-data, c2-data, c3-data... PlanText-concatenated-data)
				// * on all "extra" rows that will just contain same c1-data, c2-data, c3-data but the PlanText will be read and appended to a StringBuilder
				// * when a "new" row is found, we will set the StringBuffer content to replace the "first row" PlanText
				// * At the END we will need to "post" the information from the last "group"
				//----------------------------------------------------------------------------------------
				Statement stmnt = conn.createStatement();
				ResultSet rs = stmnt.executeQuery(_sql_sqlPlanText);
				ResultSetMetaData rsmd = rs.getMetaData();
				int           colCount = rsmd.getColumnCount();
				int           sequenceCol_pos = -1;
				int           sequenceCol     = -1;
				int           seqTextCol_pos  = -1;
				StringBuilder seqTextCol_val  = new StringBuilder();
				for (int c=1; c<=colCount; c++)
				{
					if      ("SequenceNumber".equals(rsmd.getColumnLabel(c))) sequenceCol_pos = c;
					else if ("PlanText"      .equals(rsmd.getColumnLabel(c))) seqTextCol_pos  = c;
				}
				List<Object> row = null;
				while(rs.next())
				{
					// The below implies that the data is sorted by: SPID|KPID, BatchID, SequenceNumber
					sequenceCol = rs.getInt(sequenceCol_pos);
					if (sequenceCol == 1)
					{
						if (row != null)
						{
							row.set(row.size()-1, seqTextCol_val.toString());
							seqTextCol_val.setLength(0);

//							addCount++;
//							addToContainer(row, pch);
							planTextRecords.add(row);
						}

						row = new ArrayList<Object>();
						row.add(MON_SQL_PLAN); // NOTE: the first object in the list should be the TableName where to store the data

						// Add the ROW to the planTextPkMap so we can delete it at the end if not used...
						int SPID    = rs.getInt("SPID");
						int BatchID = rs.getInt("BatchID");

						planTextPkMap.put(new PK(SPID, BatchID), row);
						
						for (int c=1; c<=colCount; c++)
						{
							// the sequence column should NOT be part of the result
							if (c != sequenceCol_pos)
								row.add(rs.getObject(c));
						}
						seqTextCol_val.append( rs.getString(seqTextCol_pos) );
					}
					else if (row != null)
					{
						seqTextCol_val.append( rs.getString(seqTextCol_pos) );
					}
				}
				// Finally add content from LAST row, since we do addCountainer() only on NEW sequenceCol_val == 1
				if (row != null)
				{
					row.set(row.size()-1, seqTextCol_val.toString());
					seqTextCol_val.setLength(0);

//					addCount++;
//					addToContainer(row, pch);
					planTextRecords.add(row);
				}

				rs.close();
				stmnt.close();
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
//		addToContainerFinal(pch);
		
		// here is how ASE works:
		//   - at start of execution add rows to: monSysSQLText
		//   - after execution add rows to:       monSysStatement and (monSysPlanText)
		// So if I execute: waitfor delay '00:00:10'
		//   The SQL Text is fetch in one sample
		//   The Statement info is only available at several "pools" later
		// So:
		//   there is possibly alot more SQLText inserted to the Capture database
		//   since it's not sampled in the *same* sample... and we only eliminate SQLText from the *same* sample...


int before_sqlTextRecords_size   = sqlTextRecords.size();
int before_planTextRecords_size  = planTextRecords.size();
		// Remove all SqlText/PlanText records that should be filtered out due to little ExecTime or LogicalReads/PhysicalReads
		for (PK pk : statementPkDiscarded)
		{
			// If a statement has been added to the DISCARD set and also added to the ADD set, then it's probably a Stored Proc with both keep/skip set
			// And if any statement in the proc is in the "add" set, then DO NOT REMOVE IT
			if (statementPkAdded.contains(pk))
				continue;

			// Remove the SQL Text record associated with the PK
			List<Object> sqlTextRecord = sqlTextPkMap.get(pk);
			if (sqlTextRecord != null)
			{
				sqlTextRecords.remove(sqlTextRecord);
//				System.out.println("  --- sqlTextRecords(pk="+pk+"): "+sqlTextRecord);
			}

			// Remove the SQL Text record associated with the PK
			List<Object> planTextRecord = planTextPkMap.get(pk);
			if (planTextRecord != null)
			{
				planTextRecords.remove(planTextRecord);
//System.out.println("  --- planTextRecords(pk="+pk+"): "+planTextRecords);
			}
		}
		
		boolean debugPrint = System.getProperty("SqlCaptureBrokerAse.debug", "false").equalsIgnoreCase("true");
		if (debugPrint || _logger.isDebugEnabled())
		{
			String debugStr = "###"
					+": sqlTextRecords.size() = "       + before_sqlTextRecords_size  + "/" + sqlTextRecords.size()  //+ "(" + sqlTextPkMap.size()  + ")"
					+", planTextRecords.size() = "      + before_planTextRecords_size + "/" + planTextRecords.size() //+ "(" + planTextPkMap.size() + ")"
					+", statementRecords.size() = "     + statementRecords.size()
					+", statementPkAdded.size() = "     + statementPkAdded.size()
					+", statementPkDiscarded.size() = " + statementPkDiscarded.size()
					+", statementReadCount="            + statementReadCount
					+", statementAddCount="             + statementAddCount
					+", statementDiscardCount="         + statementDiscardCount;

			_logger.debug(debugStr);

			if (debugPrint)
				System.out.println(debugStr);
		}
		
		//------------------------------------------------
		// Add the records to the PersistentCounterHandler
		//------------------------------------------------
		SqlCaptureDetails capDet = new SqlCaptureDetails();
		
		for (List<Object> record : sqlTextRecords)
		{ 
			int s = record.size(); 
			String sqlText = (String) record.get(s-1); // SQLText = Last record

			// Set JavaStrLength and JavaCashCode fields
			record.set(s-3, sqlText.length());   // JavaStrLength   = 2 recods before SQLText 
			record.set(s-2, sqlText.hashCode()); // JavaSqlHashCode = 1 recods before SQLText
			
			capDet.add(record); 
		}
		
		for (List<Object> record : statementRecords) 
		{
			try
			{
    			// Set JavaSqlHashCode fields, if SQLText exists...
				// NOTE: or maybe we can do this in the database:::: Maybe the following SQL could work:  update MonSqlCapStatements set JavaSqlHashCode = (select JavaSqlHashCode from MonSqlCapSqlText where SPID = ### and BatchID = ###) where SPID = ### and BatchID = ### 
    			int SPID    = (Integer) record.get(_stmnt_SPID_pos);
    			int BatchID = (Integer) record.get(_stmnt_BatchID_pos);
    			List<Object> sqlTextRecord = sqlTextPkMap.get(new PK(SPID, BatchID));
    			if (sqlTextRecord != null)
    			{
    				String sqlText = (String) sqlTextRecord.get(sqlTextRecord.size()-1); // SQLText = Last record
    
    				// Set JavaSqlHashCode fields
    				int s = record.size();
    				record.set(s-1, sqlText.hashCode()); // JavaSqlHashCode  = Last record
    			}
			}
			catch (Throwable t)
			{
				_logger.error("Problem when setting 'JavaSqlHashCode' in statementRecords, skipping this and continuing. Caught: "+t);
			}
			
			capDet.add(record); 
		}
		for (List<Object> record : planTextRecords)  
		{ 
			capDet.add(record); 
		}

//		for (List<Object> record : capDet.getList())
//			System.out.println("  +++ "+record);
		
		// Post the information to the PersistentCounterHandler, which will save the information to it's writers...
		pch.addSqlCapture(capDet);

		addCount = capDet.size();
		return addCount;
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

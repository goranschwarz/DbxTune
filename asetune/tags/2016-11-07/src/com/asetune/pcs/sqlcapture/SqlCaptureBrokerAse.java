package com.asetune.pcs.sqlcapture;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.cache.XmlPlanCache;
import com.asetune.pcs.PersistWriterBase;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;
import com.asetune.utils.Ver;

public class SqlCaptureBrokerAse 
extends SqlCaptureBrokerAbstract
{
	private static Logger _logger = Logger.getLogger(SqlCaptureBrokerAse.class);

	private boolean _clearBeforeFirstPoll = Configuration.getCombinedConfiguration().getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_clearBeforeFirstPoll, PersistentCounterHandler.DEFAULT_sqlCap_clearBeforeFirstPoll);
//	private boolean _clearBeforeFirstPoll = true;
	private boolean _firstPoll = true;
	
	private int _sendSizeThreshold = Configuration.getCombinedConfiguration().getIntProperty(PersistentCounterHandler.PROPKEY_sqlCap_sendSizeThreshold, PersistentCounterHandler.DEFAULT_sqlCap_sendSizeThreshold);
//	private int _sendSizeThreshold = 1000;

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

	private SqlCaptureDetails _sqlCaptureDetails = new SqlCaptureDetails();

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
			sbSql.append("    "+fill(qic+"sampleTime"       +qic,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"InstanceID"       +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"SPID"             +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"KPID"             +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"DBID"             +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"ProcedureID"      +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"PlanID"           +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"BatchID"          +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"ContextID"        +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"LineNumber"       +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"ObjOwnerID"       +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"DBName"           +qic,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"HashKey"          +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"SsqlId"           +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"ProcName"         +qic,40)+" "+fill(getDatatype("varchar",  255,-1,-1),20)+" "+getNullable(true)+"\n"); // NULLABLE
			sbSql.append("   ,"+fill(qic+"Elapsed_ms"       +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"CpuTime"          +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"WaitTime"         +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"MemUsageKB"       +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"PhysicalReads"    +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"LogicalReads"     +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"RowsAffected"     +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"ErrorStatus"      +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"ProcNestLevel"    +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"StatementNumber"  +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"PagesModified"    +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"PacketsSent"      +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"PacketsReceived"  +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"NetworkPacketSize"+qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"PlansAltered"     +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"StartTime"        +qic,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
			sbSql.append("   ,"+fill(qic+"EndTime"          +qic,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
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
	public String getIndexDdlString(DatabaseMetaData dbmd, String tabName)
	{
		// NOTE: The DatabaseMetaData is to the PCS Writer/Storage Connection
		String dbmsProductName = "unknown";
		String qic             = "\"";
		try { dbmsProductName = dbmd.getDatabaseProductName();   } catch(SQLException ex) { _logger.warn("Problems getting 'dbmd.getDatabaseProductName()', Caught: "+ex); }
		try { qic             = dbmd.getIdentifierQuoteString(); } catch(SQLException ex) { _logger.warn("Problems getting 'dbmd.getIdentifierQuoteString()', Caught: "+ex); }
		
		String iQic = qic; // indexNameQic
		if ( DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_SYBASE_ASE) )
			iQic = "";

		if (MON_SQL_TEXT.equals(tabName))
		{
			return "create index " + dbmsQuotify(iQic, tabName+"_ix1") + " on " + dbmsQuotify(qic, tabName) + "("+dbmsQuotify(qic, "BatchID", "SPID") + ")\n";
		}

		if (MON_SQL_STATEMENT.equals(tabName))
		{
			return "create index " + dbmsQuotify(iQic, tabName+"_ix1") + " on " + dbmsQuotify(qic, tabName) + "("+dbmsQuotify(qic, "BatchID", "SPID") + ")\n";
		}

		if (MON_SQL_PLAN.equals(tabName))
		{
			return "create index " + dbmsQuotify(iQic, tabName+"_ix1") + " on " + dbmsQuotify(qic, tabName) + "("+dbmsQuotify(qic, "BatchID", "SPID") + ")\n";
		}

		return null;
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
			sbSql.append(" ").append(qic).append("sampleTime" ).append(qic);
			sbSql.append(",").append(qic).append("InstanceID" ).append(qic);
			sbSql.append(",").append(qic).append("SPID"       ).append(qic);
			sbSql.append(",").append(qic).append("KPID"       ).append(qic);
			sbSql.append(",").append(qic).append("BatchID"    ).append(qic);
			sbSql.append(",").append(qic).append("ServerLogin").append(qic);
			sbSql.append(",").append(qic).append("SQLText"    ).append(qic);
			sbSql.append(") \n");
			sbSql.append("values(?, ?, ?, ?, ?, ?, ?) \n"); // 7 question marks
			//                   1, 2, 3, 4, 5, 6, 7

			return sbSql.toString();
		}

		if (MON_SQL_STATEMENT.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();

			sbSql.append("insert into ").append(tabName);
			sbSql.append("(");
			sbSql.append(" ").append(qic).append("sampleTime"       ).append(qic);
			sbSql.append(",").append(qic).append("InstanceID"       ).append(qic);
			sbSql.append(",").append(qic).append("SPID"             ).append(qic);
			sbSql.append(",").append(qic).append("KPID"             ).append(qic);
			sbSql.append(",").append(qic).append("DBID"             ).append(qic);
			sbSql.append(",").append(qic).append("ProcedureID"      ).append(qic);
			sbSql.append(",").append(qic).append("PlanID"           ).append(qic);
			sbSql.append(",").append(qic).append("BatchID"          ).append(qic);
			sbSql.append(",").append(qic).append("ContextID"        ).append(qic);
			sbSql.append(",").append(qic).append("LineNumber"       ).append(qic);
			sbSql.append(",").append(qic).append("ObjOwnerID"       ).append(qic);
			sbSql.append(",").append(qic).append("DBName"           ).append(qic);
			sbSql.append(",").append(qic).append("HashKey"          ).append(qic);
			sbSql.append(",").append(qic).append("SsqlId"           ).append(qic);
			sbSql.append(",").append(qic).append("ProcName"         ).append(qic);
			sbSql.append(",").append(qic).append("Elapsed_ms"       ).append(qic);
			sbSql.append(",").append(qic).append("CpuTime"          ).append(qic);
			sbSql.append(",").append(qic).append("WaitTime"         ).append(qic);
			sbSql.append(",").append(qic).append("MemUsageKB"       ).append(qic);
			sbSql.append(",").append(qic).append("PhysicalReads"    ).append(qic);
			sbSql.append(",").append(qic).append("LogicalReads"     ).append(qic);
			sbSql.append(",").append(qic).append("RowsAffected"     ).append(qic);
			sbSql.append(",").append(qic).append("ErrorStatus"      ).append(qic);
			sbSql.append(",").append(qic).append("ProcNestLevel"    ).append(qic);
			sbSql.append(",").append(qic).append("StatementNumber"  ).append(qic);
			sbSql.append(",").append(qic).append("PagesModified"    ).append(qic);
			sbSql.append(",").append(qic).append("PacketsSent"      ).append(qic);
			sbSql.append(",").append(qic).append("PacketsReceived"  ).append(qic);
			sbSql.append(",").append(qic).append("NetworkPacketSize").append(qic);
			sbSql.append(",").append(qic).append("PlansAltered"     ).append(qic);
			sbSql.append(",").append(qic).append("StartTime"        ).append(qic);
			sbSql.append(",").append(qic).append("EndTime"          ).append(qic);
			sbSql.append(") \n");
			sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n"); // 32 question marks
			//                   1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32

			return sbSql.toString();
		}

		if (MON_SQL_PLAN.equals(tabName))
		{
			StringBuilder sbSql = new StringBuilder();

			sbSql.append("insert into ").append(tabName);
			sbSql.append("(");
			sbSql.append(" ").append(qic).append("sampleTime" ).append(qic);
			sbSql.append(",").append(qic).append("InstanceID" ).append(qic);
			sbSql.append(",").append(qic).append("SPID"       ).append(qic);
			sbSql.append(",").append(qic).append("KPID"       ).append(qic);
			sbSql.append(",").append(qic).append("PlanID"     ).append(qic);
			sbSql.append(",").append(qic).append("BatchID"    ).append(qic);
			sbSql.append(",").append(qic).append("ContextID"  ).append(qic);
			sbSql.append(",").append(qic).append("DBID"       ).append(qic);
			sbSql.append(",").append(qic).append("DBName"     ).append(qic);
			sbSql.append(",").append(qic).append("ProcedureID").append(qic);
			sbSql.append(",").append(qic).append("PlanText"   ).append(qic);
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
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean doSqlText       = conf.getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_doSqlText,       PersistentCounterHandler.DEFAULT_sqlCap_doSqlText);
		boolean doStatementInfo = conf.getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_doStatementInfo, PersistentCounterHandler.DEFAULT_sqlCap_doStatementInfo);
		boolean doPlanText      = conf.getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_doPlanText,      PersistentCounterHandler.DEFAULT_sqlCap_doPlanText);
		
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

		_sql_sqlText = "select * from master.dbo.monSysSQLText";

		String RowsAffected    = "RowsAffected = convert(int, -1), ";
		String ErrorStatus     = "ErrorStatus = convert(int, -1), ";
		String HashKey         = "HashKey = convert(int, -1), ";
		String SsqlId          = "SsqlId = convert(int, -1), ";
		String DBName          = "DBName=db_name(DBID), ";
		String ProcNestLevel   = "ProcNestLevel = convert(int, -1), ";
		String StatementNumber = "StatementNumber = convert(int, -1), ";
		String InstanceID      = "InstanceID = convert(int, -1), ";
		String ProcName        = "ProcName = isnull(isnull(object_name(ProcedureID,DBID),object_name(ProcedureID,2)),object_name(ProcedureID,db_id('sybsystemprocs'))), \n";
		String ObjOwnerID      = "ObjOwnerID = convert(int, 0), \n";

		String ServerLogin     = "ServerLogin = convert(varchar(30), '-1'), \n";

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
		
		_sql_sqlText 
			= "select sampleTime = getdate(), \n"
			+ "    "+InstanceID+"\n"
			+ "    SPID, \n"
			+ "    KPID, \n"
			+ "    BatchID, \n"
			+ "    SequenceInBatch, \n"
			+ "    "+ServerLogin+"\n"
			+ "    SQLText \n"
			+ "from master.dbo.monSysSQLText \n"
			+ "where 1 = 1 \n"
			+ "  and SPID != @@spid \n"
			+ "  and SPID not in (select spid from master.dbo.sysprocesses where program_name like '"+Version.getAppName()+"%') ";

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
			+ "    Elapsed_ms = CASE WHEN datediff(day, StartTime, EndTime) > 20 THEN -1 ELSE  datediff(ms, StartTime, EndTime) END, \n"
			+ "    CpuTime, \n"
			+ "    WaitTime, \n"
			+ "    MemUsageKB, \n"
			+ "    PhysicalReads, \n"
			+ "    LogicalReads, \n"
			+ "    " + RowsAffected + " \n"
			+ "    " + ErrorStatus + " \n"
			+ "    " + ProcNestLevel + " \n"
			+ "    " + StatementNumber + " \n"
			+ "    PagesModified, \n"
			+ "    PacketsSent, \n"
			+ "    PacketsReceived, \n"
			+ "    NetworkPacketSize, \n"
			+ "    PlansAltered, \n"
			+ "    StartTime, \n"
			+ "    EndTime \n"
			+ "from master.dbo.monSysStatement \n"
			+ "where 1 = 1 \n"
			+ "  and SPID != @@spid \n"
			+ "  and SPID not in (select spid from master.dbo.sysprocesses where program_name like '"+Version.getAppName()+"%') ";
		
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
			+ "  and SPID not in (select spid from master.dbo.sysprocesses where program_name like '"+Version.getAppName()+"%') ";
	}

	private void addToContainer(List<Object> row, PersistentCounterHandler pch)
	{
		_sqlCaptureDetails.add(row);
		
		// When the "send size" is reached, then send and create a new Container.
		if (_sqlCaptureDetails.size() >= _sendSizeThreshold)
		{
			pch.addSqlCapture(_sqlCaptureDetails);
			_sqlCaptureDetails = new SqlCaptureDetails();
		}
	}
	private void addToContainerFinal(PersistentCounterHandler pch)
	{
		pch.addSqlCapture(_sqlCaptureDetails);
		_sqlCaptureDetails = new SqlCaptureDetails();
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


		// If SQL queries are NOT initialized, do it now
		if (_sql_sqlText == null)
			setSql(conn);

		//------------------------------------------------
		// SQL TEXT
		//------------------------------------------------
		if (_sampleSqlText)
		{
			try
			{
//				Statement stmnt = conn.createStatement();
//				ResultSet rs = stmnt.executeQuery(_sql_sqlText);
//				ResultSetMetaData rsmd = rs.getMetaData();
//				int colCount = rsmd.getColumnCount();
//				int sequenceCol_pos = -1;
//				int sequenceCol     = -1;
//				for (int c=1; c<=colCount; c++)
//				{
//					if ("SequenceInBatch".equals(rsmd.getColumnLabel(c)))
//						sequenceCol_pos = c;
//				}
////				List<Object> row = new ArrayList<Object>();
//				List<Object> row = null;
//				while(rs.next())
//				{
//					sequenceCol = rs.getInt(sequenceCol_pos);
//					if (sequenceCol == 1)
//					{
//						if (row != null)
//						{
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
//						
////						addCount++;
////						addToContainer(row, pch);
//					}
//					else if (row != null)
//					{
//						int    SQLTextListPos = row.size() - 1;
//						String SQLTextPrev    = (String) row.get(SQLTextListPos);
//						String SQLTextThis    = rs.getString("SQLText");
//						row.set(SQLTextListPos, SQLTextPrev + SQLTextThis);
//					}
//				}
//				rs.close();
//				stmnt.close();

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
					sequenceCol_val = rs.getInt(sequenceCol_pos);
					if (sequenceCol_val == 1)
					{
						if (row != null)
						{
							row.set(row.size()-1, seqTextCol_val.toString());
							seqTextCol_val.setLength(0);

							addCount++;
							addToContainer(row, pch);
						}

						row = new ArrayList<Object>();
						row.add(MON_SQL_TEXT); // NOTE: the first object in the list should be the TableName where to store the data
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

					addCount++;
					addToContainer(row, pch);
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
		// SQL STATEMENTS
		//------------------------------------------------
		if (_sampleStatements)
		{
			// Get some configuration
			Configuration conf = Configuration.getCombinedConfiguration();
			boolean sendDdlForLookup                  = conf.getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup,                  PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup);
			int     sendDdlForLookup_gt_execTime      = conf.getIntProperty    (PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_execTime,      PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_execTime);
			int     sendDdlForLookup_gt_logicalReads  = conf.getIntProperty    (PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_logicalReads,  PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_logicalReads);
			int     sendDdlForLookup_gt_physicalReads = conf.getIntProperty    (PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_physicalReads, PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_physicalReads);

			try
			{
				Statement stmnt = conn.createStatement();
				ResultSet rs = stmnt.executeQuery(_sql_sqlStatements);
				int colCount = rs.getMetaData().getColumnCount();
				while(rs.next())
				{
					if (sendDdlForLookup)
					{
						String ProcName = rs.getString("ProcName");

						// send ProcName or StatementCacheEntry to the DDL Capture
						if (StringUtil.hasValue(ProcName))
						{
							String DBName        = rs.getString("DBName");
							int    execTime      = rs.getInt   ("Elapsed_ms");
							int    logicalReads  = rs.getInt   ("LogicalReads");
							int    physicalReads = rs.getInt   ("PhysicalReads");

							// Only send if it's above the defined limits
							if (    execTime      > sendDdlForLookup_gt_execTime
							     && logicalReads  > sendDdlForLookup_gt_logicalReads
							     && physicalReads > sendDdlForLookup_gt_physicalReads
							   )
							{
    							// if it's a statement cache entry, populate it into the cache so that the DDL Lookup wont have to sleep 
    							if (ProcName.startsWith("*ss") || ProcName.startsWith("*sq") ) // *sq in ASE 15.7 esd#2, DynamicSQL can/will end up in statement cache
    							{
    								XmlPlanCache xmlPlanCache = XmlPlanCache.getInstance();
    								if ( ! xmlPlanCache.isPlanCached(ProcName) )
    								{
    									xmlPlanCache.getPlan(ProcName);
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

					addCount++;
					addToContainer(row, pch);
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
		// SQL PLANS
		//------------------------------------------------
		if (_samplePlan)
		{
			try
			{
//				Statement stmnt = conn.createStatement();
//				ResultSet rs = stmnt.executeQuery(_sql_sqlPlanText);
//				ResultSetMetaData rsmd = rs.getMetaData();
//				int colCount = rsmd.getColumnCount();
//				int sequenceCol_pos = -1;
//				int sequenceCol     = -1;
//				for (int c=1; c<=colCount; c++)
//				{
//					if ("SequenceNumber".equals(rsmd.getColumnLabel(c)))
//						sequenceCol_pos = c;
//				}
//				List<Object> row = new ArrayList<Object>();
//				while(rs.next())
//				{
//					sequenceCol = rs.getInt(sequenceCol_pos);
//					if (sequenceCol == 1)
//					{
//						row = new ArrayList<Object>();
//						row.add(MON_SQL_PLAN); // NOTE: the first object in the list should be the TableName where to store the data
//						for (int c=1; c<=colCount; c++)
//						{
//							// the sequence column should NOT be part of the result
//							if (c != sequenceCol_pos)
//								row.add(rs.getObject(c));
//						}
//	
//						addCount++;
//						addToContainer(row, pch);
//					}
//					else
//					{
//						int    PlanTextListPos = row.size() - 1;
//						String PlanTextPrev    = (String) row.get(PlanTextListPos);
//						String PlanTextThis    = rs.getString("PlanText");
//						row.set(PlanTextListPos, PlanTextPrev + PlanTextThis);
//					}
//				}
//				rs.close();
//				stmnt.close();

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
					sequenceCol = rs.getInt(sequenceCol_pos);
					if (sequenceCol == 1)
					{
						if (row != null)
						{
							row.set(row.size()-1, seqTextCol_val.toString());
							seqTextCol_val.setLength(0);

							addCount++;
							addToContainer(row, pch);
						}

						row = new ArrayList<Object>();
						row.add(MON_SQL_PLAN); // NOTE: the first object in the list should be the TableName where to store the data
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

					addCount++;
					addToContainer(row, pch);
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

		addToContainerFinal(pch);
		return addCount;
	}
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

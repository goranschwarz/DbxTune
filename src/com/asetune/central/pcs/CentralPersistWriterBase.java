package com.asetune.central.pcs;

import java.lang.invoke.MethodHandles;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asetune.central.pcs.DbxTuneSample.GraphEntry;
import com.asetune.cm.CountersModel;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;

public abstract class CentralPersistWriterBase
implements ICentralPersistWriter
{
	private static final Logger _logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
//	private final Logger _logger = LoggerFactory.getLogger(this.getClass().getName());

	/** Increment this every time you add/change table structure/layout, you will also have to implement an upgrade/(downgrade) path
	 * <br>
	 * <ul>
	 *   <li> 1 - First version</li>
	 *   <li> 2 - Add column 'CollectorSampleInterval'           to table 'DbxCentralSessions' </li>
	 *   <li> 3 - Add column 'ProfileDescription'                to table 'DbxCentralGraphProfiles' </li>
	 *   <li> 4 - Add column 'CollectorCurrentUrl', 'Status'     to table 'DbxCentralSessions' </li>
	 *   <li> 5 - Add column 'CollectorInfoFile'                 to table 'DbxCentralSessions' </li>
	 *   <li> 6 - Add column 'category'                          to table 'ALARM_ACTIVE, ALARM_HISTORY' in all schemas</li>
	 *   <li> 7 - Add column 'ProfileUrlOptions'                 to table 'DbxCentralGraphProfiles' </li>
	 *   <li> 7 - Add column 'GraphCategory'                     to table 'DbxGraphProperties' </li>
	 * </ul> 
	 */
	public static int DBX_CENTRAL_DB_VERSION = 8;
	
	
	public enum Table
	{
		CENTRAL_VERSION_INFO, 
		CENTRAL_SESSIONS,
		CENTRAL_GRAPH_PROFILES,
		SESSION_SAMPLES,
		SESSION_SAMPLE_SUM,
		SESSION_SAMPLE_DETAILS,
		GRAPH_PROPERTIES,
//		CHART_LABELS, 
		ALARM_ACTIVE, 
		ALARM_HISTORY, 
		ABS,
		DIFF,
		RATE
	};


	private static CentralPersistWriterBase _instance = null;

	private Configuration _config = null;
//	private boolean  _initialized = false;

	private HashMap<String, SessionInfo> _sessionInfoMap = new HashMap<>();
	private static class SessionInfo
	{
    	/** Determines if a session is started or not, or needs initialization */
    	private boolean _isSessionStarted = false;
    
    	/** Session start time is maintained from PersistCounterHandler */
    	private Timestamp _sessionStartTime = null;
	}
	
	private SessionInfo getSessionInfo(String sessionName)
	{
		// Create the Map if it do not exist
		if (_sessionInfoMap == null)
			_sessionInfoMap = new HashMap<>();
		
		// Get the session map, if it dosnt exists: create it
		SessionInfo si = _sessionInfoMap.get(sessionName);
		if (si == null)
		{
			si = new SessionInfo();
			_sessionInfoMap.put(sessionName, si);
		}

		return si;
	}

	@Override
	public boolean isSessionStarted(String sessionName)
	{
		return getSessionInfo(sessionName)._isSessionStarted;
	}
	
	@Override
	public void setSessionStarted(String sessionName, boolean isSessionStarted)
	{
		getSessionInfo(sessionName)._isSessionStarted = isSessionStarted;
	}

	/**
	 * Used by PersistCounterHandler to set a new session start time
	 */
	@Override
	public void setSessionStartTime(String sessionName, Timestamp sessionStartTime)
	{
		getSessionInfo(sessionName)._sessionStartTime  = sessionStartTime;
	}


	/**
	 * Used by PersistCounterHandler to get the session start time
	 */
	@Override
	public Timestamp getSessionStartTime(String sessionName)
	{
		return getSessionInfo(sessionName)._sessionStartTime;
	}	


	
	
	
	/**	what are we connected to: DatabaseMetaData.getDatabaseProductName() */
	private String _databaseProductName = "";

	/** Character used for quoted identifier, on connect we will get the correct character from the MetaData */
	public static String  _qic = "\"";

	
	public static CentralPersistWriterBase getInstance()
	{
		return _instance;
	}

	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static void setInstance(CentralPersistWriterBase inst)
	{
		_instance = inst;
	}

	@Override
	public Configuration getConfig()
	{
		return _config;
	}
	
	/** Initialize various member of the class */
	@Override
	public synchronized void init(Configuration conf)
	throws Exception
	{
		_config = conf; 

		_logger.info("Initializing the Persistent Writer.");
	}

	
	
	
	public void setDatabaseProductName(String databaseProductName)
	{
		_databaseProductName = databaseProductName;
	}
	public String getDatabaseProductName()
	{
		return _databaseProductName;
	}

	public static void setQuotedIdentifierChar(String qic)
	{
		_qic = qic;
	}
	public static String getQuotedIdentifierChar()
	{
		return _qic;
	}

	/** Helper method */
	public static String fill(String str, int fill)
	{
		if (str.length() < fill)
		{
			String fillStr = "                                                              ";
			return (str + fillStr).substring(0,fill);
		}
		return str;
	}

	/**
	 * This method can be overladed and used to change the syntax for various datatypes 
	 */
	public static String getDatatype(String type, int length, int prec, int scale)
	{
		if ( type.equals("char") || type.equals("varchar") )
			type = type + "(" + length + ")";
		
		if ( type.equals("numeric") || type.equals("decimal") )
			type = type + "(" + prec + "," + scale + ")";

		return type;
	}
	public static String getDatatype(int col, ResultSetMetaData rsmd, boolean isDeltaOrPct)
	throws SQLException
	{
		String type   = null;
		int    length = -1;
		int    prec   = -1;
		int    scale  = -1;

		if (isDeltaOrPct)
		{
			type    = "numeric";
			length  = -1;
			prec    = 10;
			scale   = 1;

			return getDatatype(type, length, prec, scale);
		}
		else
		{
			type  = ResultSetTableModel.getColumnTypeName(rsmd, col);

			// Most databases doesn't have unsigned datatypes, so lets leave "unsigned int" as "int"
			if ( type.startsWith("unsigned ") )
			{
				String newType = type.substring("unsigned ".length());
//				_logger.info("Found the uncommon data type '"+type+"', instead the data type '"+newType+"' will be used.");
				type = newType;
			}
			
			return type;
		}
	}

	/** Helper method to get a table name 
	 * @param schemaName */
	public static String getTableName(String schemaName, Table type, CountersModel cm, boolean addQuotedIdentifierChar)
	{
		String q = "";
		if (addQuotedIdentifierChar)
			q = getQuotedIdentifierChar();

		String prefix = "";
		if (StringUtil.hasValue(schemaName))
		{
			prefix = q + schemaName + q + ".";
		}

		switch (type)
		{
		case CENTRAL_VERSION_INFO:     return          q + "DbxCentralVersionInfo"       + q;
		case CENTRAL_SESSIONS:         return          q + "DbxCentralSessions"          + q;
		case CENTRAL_GRAPH_PROFILES:   return          q + "DbxCentralGraphProfiles"     + q;
		case SESSION_SAMPLES:          return prefix + q + "DbxSessionSamples"           + q;
		case SESSION_SAMPLE_SUM:       return prefix + q + "DbxSessionSampleSum"         + q;
		case SESSION_SAMPLE_DETAILS:   return prefix + q + "DbxSessionSampleDetailes"    + q;
		case ALARM_ACTIVE:             return prefix + q + "DbxAlarmActive"              + q;
		case ALARM_HISTORY:            return prefix + q + "DbxAlarmHistory"             + q;
		case GRAPH_PROPERTIES:         return prefix + q + "DbxGraphProperties"          + q;
//		case CHART_LABELS:             return prefix + q + "DbxChartLabels"              + q;
		case ABS:                      return prefix + q + cm.getName() + "_abs"         + q;
		case DIFF:                     return prefix + q + cm.getName() + "_diff"        + q;
		case RATE:                     return prefix + q + cm.getName() + "_rate"        + q;
		default:
			throw new RuntimeException("Unknown type of '"+type+"' in getTableName()."); 
		}
	}

	public static String getNullable(boolean nullable)
	{
		return nullable ? "    null" : "not null";
		
	}
	public static String getNullable(int col, ResultSetMetaData rsmd, boolean isDeltaOrPct)
	throws SQLException
	{
		// datatype "bit" can't be NULL declared in ASE
//		String type  = rsmd.getColumnTypeName(col);
		String type  = getDatatype(col, rsmd, isDeltaOrPct);
		if (type != null && type.equalsIgnoreCase("bit"))
			return getNullable(false);
		else
			return getNullable(true);
//		return getNullable(rsmd.isNullable(col)>0);
	}
	
	/** Helper method to generate a DDL string, to get the 'create table' 
	 * @param schemaName */
	public String getTableDdlString(String schemaName, Table type, CountersModel cm)
	throws SQLException
	{
		String tabName = getTableName(schemaName, type, cm, true);
		StringBuffer sbSql = new StringBuffer();
		
		String qic = getQuotedIdentifierChar();

		try
		{
			if (Table.CENTRAL_VERSION_INFO.equals(type))
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
//				sbSql.append("    "+fill(qic+"SessionStartTime"+qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("    "+fill(qic+"ProductString"   +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"VersionString"   +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"BuildString"     +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"DbVersion"       +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(qic+"SourceDate"      +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(qic+"SourceRev"       +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"DbProductName"   +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+qic+"ProductString"+qic+")\n");
//				sbSql.append("   ,PRIMARY KEY ("+qic+"SessionStartTime"+qic+")\n");
				sbSql.append(") \n");
			}
			else if (Table.CENTRAL_SESSIONS.equals(type))
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"SessionStartTime"        +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"Status"                  +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"ServerName"              +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"OnHostname"              +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"ProductString"           +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"VersionString"           +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"BuildString"             +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"CollectorHostname"       +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"CollectorSampleInterval" +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"CollectorCurrentUrl"     +qic,40)+" "+fill(getDatatype("varchar", 80,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(qic+"CollectorInfoFile"       +qic,40)+" "+fill(getDatatype("varchar",256,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(qic+"NumOfSamples"            +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"LastSampleTime"          +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+qic+"SessionStartTime"+qic+")\n");
				sbSql.append(") \n");
			}
			else if (Table.CENTRAL_GRAPH_PROFILES.equals(type))
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"ProductString"      +qic,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"UserName"           +qic,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"ProfileName"        +qic,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"ProfileDescription" +qic,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"ProfileValue"       +qic,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"ProfileUrlOptions"  +qic,40)+" "+fill(getDatatype("varchar", 1024,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+qic+"ProductString"+qic+", "+qic+"UserName"+qic+", "+qic+"ProfileName"+qic+")\n");
				sbSql.append(") \n");
			}
			else if (Table.SESSION_SAMPLES.equals(type))
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"SessionStartTime" +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"SessionSampleTime"+qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+qic+"SessionSampleTime"+qic+", "+qic+"SessionStartTime"+qic+")\n");
				sbSql.append(") \n");
			}
			else if (Table.SESSION_SAMPLE_SUM.equals(type))
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"SessionStartTime" +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"CmName"           +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"graphSamples"     +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"absSamples"       +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"diffSamples"      +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"rateSamples"      +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+qic+"SessionStartTime"+qic+", "+qic+"CmName"+qic+")\n");
				sbSql.append(") \n");
			}
			else if (Table.SESSION_SAMPLE_DETAILS.equals(type))
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"SessionStartTime"      +qic,40)+" "+fill(getDatatype("datetime",-1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"SessionSampleTime"     +qic,40)+" "+fill(getDatatype("datetime",-1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"CmName"                +qic,40)+" "+fill(getDatatype("varchar", 30,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"type"                  +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(qic+"graphCount"            +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(qic+"absRows"               +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(qic+"diffRows"              +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(qic+"rateRows"              +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"graphRecvCount"        +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"absRecvRows"           +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"diffRecvRows"          +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"rateRecvRows"          +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"graphSaveCount"        +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"absSaveRows"           +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"diffSaveRows"          +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"rateSaveRows"          +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"sqlRefreshTime"        +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"guiRefreshTime"        +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"lcRefreshTime"         +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"nonCfgMonHappened"     +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"nonCfgMonMissingParams"+qic,40)+" "+fill(getDatatype("varchar", 100, -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"nonCfgMonMessages"     +qic,40)+" "+fill(getDatatype("varchar", 1024,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"isCountersCleared"     +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"hasValidSampleData"    +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"exceptionMsg"          +qic,40)+" "+fill(getDatatype("varchar", 1024,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"exceptionFullText"     +qic,40)+" "+fill(getDatatype("text",    -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+qic+"SessionSampleTime"+qic+", "+qic+"CmName"+qic+", "+qic+"SessionStartTime"+qic+")\n");
				sbSql.append(") \n");
			}
			else if (Table.GRAPH_PROPERTIES.equals(type))
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"SessionStartTime"+qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"CmName"          +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"GraphName"       +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"TableName"       +qic,40)+" "+fill(getDatatype("varchar",128,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"GraphLabel"      +qic,40)+" "+fill(getDatatype("varchar",255,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"GraphCategory"   +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"isPercentGraph"  +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"visibleAtStart"  +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"initialOrder"    +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("\n");
//				sbSql.append("   ,PRIMARY KEY ("+qic+"SessionStartTime"+qic+", "+qic+"CmName"+qic+", "+qic+"GraphName"+qic+")\n");
				sbSql.append("   ,PRIMARY KEY ("+qic+"SessionStartTime"+qic+", "+qic+"GraphName"+qic+")\n");
				sbSql.append(") \n");
			}
			else if (Table.ALARM_ACTIVE.equals(type))
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"alarmClass"                 +qic,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"serviceType"                +qic,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"serviceName"                +qic,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"serviceInfo"                +qic,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"extraInfo"                  +qic,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(qic+"category"                   +qic,40)+" "+fill(getDatatype("varchar",   20,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"severity"                   +qic,40)+" "+fill(getDatatype("varchar",   10,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"state"                      +qic,40)+" "+fill(getDatatype("varchar",   10,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"repeatCnt"                  +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"duration"                   +qic,40)+" "+fill(getDatatype("varchar",   10,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"createTime"                 +qic,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"cancelTime"                 +qic,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(qic+"timeToLive"                 +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(qic+"threshold"                  +qic,40)+" "+fill(getDatatype("varchar",   15,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(qic+"data"                       +qic,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(qic+"lastData"                   +qic,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(qic+"description"                +qic,40)+" "+fill(getDatatype("varchar",  512,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"lastDescription"            +qic,40)+" "+fill(getDatatype("varchar",  512,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"extendedDescription"        +qic,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(qic+"lastExtendedDescription"    +qic,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+qic+"alarmClass"+qic+", "+qic+"serviceType"+qic+", "+qic+"serviceName"+qic+", "+qic+"serviceInfo"+qic+", "+qic+"extraInfo"+qic+")\n");
				sbSql.append(") \n");
			}
			else if (Table.ALARM_HISTORY.equals(type))
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"SessionStartTime"           +qic,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"SessionSampleTime"          +qic,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"eventTime"	              +qic,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"action"                     +qic,40)+" "+fill(getDatatype("varchar",   15,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(qic+"isActive"                   +qic,40)+" "+fill(getDatatype("bit",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"alarmClass"                 +qic,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"serviceType"                +qic,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"serviceName"                +qic,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"serviceInfo"                +qic,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"extraInfo"                  +qic,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(qic+"category"                   +qic,40)+" "+fill(getDatatype("varchar",   20,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"severity"                   +qic,40)+" "+fill(getDatatype("varchar",   10,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"state"                      +qic,40)+" "+fill(getDatatype("varchar",   10,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"repeatCnt"                  +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"duration"                   +qic,40)+" "+fill(getDatatype("varchar",   10,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"createTime"                 +qic,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"cancelTime"                 +qic,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(qic+"timeToLive"                 +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(qic+"threshold"                  +qic,40)+" "+fill(getDatatype("varchar",   15,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(qic+"data"                       +qic,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(qic+"lastData"                   +qic,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(qic+"description"                +qic,40)+" "+fill(getDatatype("varchar",  512,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"lastDescription"            +qic,40)+" "+fill(getDatatype("varchar",  512,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"extendedDescription"        +qic,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(qic+"lastExtendedDescription"    +qic,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+qic+"eventTime"+qic+", "+qic+"action"+qic+", "+qic+"alarmClass"+qic+", "+qic+"serviceType"+qic+", "+qic+"serviceName"+qic+", "+qic+"serviceInfo"+qic+", "+qic+"extraInfo"+qic+")\n");
				sbSql.append(") \n");
			}
//			else if (Table.CHART_LABELS.equals(type))
//			{
//				sbSql.append("create table " + tabName + "\n");
//				sbSql.append("( \n");
//				sbSql.append("    "+fill(qic+"SessionStartTime"+qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(qic+"ServerName"      +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(qic+"NumOfSamples"    +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(qic+"LastSampleTime"  +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("\n");
//				sbSql.append("   ,PRIMARY KEY ("+qic+"SessionStartTime"+qic+")\n");
//				sbSql.append(") \n");
//			}
			else if ( Table.ABS.equals(type) || Table.DIFF.equals(type) || Table.RATE.equals(type) )
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"SessionStartTime" +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"SessionSampleTime"+qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"CmSampleTime"     +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"CmSampleMs"       +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"CmNewDiffRateRow" +qic,40)+" "+fill(getDatatype("tinyint", -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("\n");
				
				ResultSetMetaData rsmd = cm.getResultSetMetaData();
				
				if ( rsmd == null )
					throw new SQLException("ResultSetMetaData for CM '"+cm.getName()+"' was null.");
				if ( rsmd.getColumnCount() == 0 )
					throw new SQLException("NO Columns was found for CM '"+cm.getName()+"'.");

				int cols = rsmd.getColumnCount();
				for (int c=1; c<=cols; c++) 
				{
					boolean isDeltaOrPct = false;
					if (Table.DIFF.equals(type))
					{
						if ( cm.isPctColumn(c-1) )
							isDeltaOrPct = true;
					}

					if (Table.RATE.equals(type))
					{
						if ( cm.isDiffColumn(c-1) || cm.isPctColumn(c-1) )
							isDeltaOrPct = true;
					}


					String colName = fill(qic+rsmd.getColumnLabel(c)+qic,       40);
					String dtName  = fill(getDatatype(c, rsmd, isDeltaOrPct),   20);
					String nullable= getNullable(c, rsmd, isDeltaOrPct);

					sbSql.append("   ," +colName+ " " + dtName + " " + nullable + "\n");
				}
				sbSql.append(") \n");
			}
			else
			{
				return null;
			}
		}
		catch (SQLException e)
		{
			_logger.warn("SQLException, Error generating DDL to Persistent Counter DB.", e);
		}
		
		return sbSql.toString();
	}

	/** Helper method to generate 'alter table ... add missingColName datatype null|not null'*/
	public List<String> getAlterTableDdlString(DbxConnection conn, String tabName, List<String> missingCols, Table type, CountersModel cm)
	throws SQLException
	{
		List<String> list = new ArrayList<String>();

		ResultSetMetaData rsmd = cm.getResultSetMetaData();
		String qic = getQuotedIdentifierChar();
		
		if ( rsmd == null )
			throw new SQLException("ResultSetMetaData for CM '"+cm.getName()+"' was null.");
		if ( rsmd.getColumnCount() == 0 )
			throw new SQLException("NO Columns was found for CM '"+cm.getName()+"'.");

		int cols = rsmd.getColumnCount();
		for (int c=1; c<=cols; c++) 
		{
			// If the current columns is NOT missing continue to next column
			if ( ! missingCols.contains(rsmd.getColumnLabel(c)) )
				continue;
				
			boolean isDeltaOrPct = false;
			if (type.equals(Table.DIFF))
			{
				if ( cm.isPctColumn(c-1) )
					isDeltaOrPct = true;
			}

			if (type.equals(Table.RATE))
			{
				if ( cm.isDiffColumn(c-1) || cm.isPctColumn(c-1) )
					isDeltaOrPct = true;
			}

			String colName = fill(qic+rsmd.getColumnLabel(c)+qic,       40);
			String dtName  = fill(getDatatype(c, rsmd, isDeltaOrPct),   20);
			String nullable= getNullable(c, rsmd, isDeltaOrPct);

			list.add("alter table " + qic+tabName+qic + " add  " + colName + " " + dtName + " " + nullable);
		}

		return list;
	}


	/**
	 * Helper method to generate a: "insert into TABNAME(c1,c2,c3) [values(?,?...)]"
	 * @param type ABS | DIFF | RATE | SYSTEM_TYPE
	 * @param cm Counter Model info (can be null if SYSTEM type is used)
	 * @param tgdp Trends Graph information
	 * @param addPrepStatementQuestionMarks if true add "values(?,?,?...)" which can be used by a prepared statement
	 * @return
	 */
	public String getTableInsertStr(String schemaName, Table type, CountersModel cm, boolean addPrepStatementQuestionMarks)
	throws SQLException
	{
		return getTableInsertStr(schemaName, type, cm, addPrepStatementQuestionMarks, null);
	}
	/**
	 * Helper method to generate a: "insert into TABNAME(c1,c2,c3) [values(?,?...)]"
	 * @param type ABS | DIFF | RATE | SYSTEM_TYPE
	 * @param cm Counter Model info (can be null if SYSTEM type is used)
	 * @param tgdp Trends Graph information
	 * @param addPrepStatementQuestionMarks if true add "values(?,?,?...)" which can be used by a prepared statement
	 * @return
	 */
	public String getTableInsertStr(String schemaName, Table type, CountersModel cm, boolean addPrepStatementQuestionMarks, List<String> cmColumns)
	throws SQLException
	{
		String qic = getQuotedIdentifierChar();
		String tabName = getTableName(schemaName, type, cm, true);
		StringBuffer sbSql = new StringBuffer();

		if (type.equals(Table.CENTRAL_VERSION_INFO))
		{
			sbSql.append("insert into ").append(tabName).append(" (");
//			sbSql.append(qic).append("SessionStartTime").append(qic).append(", ");
			sbSql.append(qic).append("ProductString")   .append(qic).append(", ");
			sbSql.append(qic).append("VersionString")   .append(qic).append(", ");
			sbSql.append(qic).append("BuildString")     .append(qic).append(", ");
			sbSql.append(qic).append("DbVersion")       .append(qic).append(", ");
//			sbSql.append(qic).append("SourceDate")      .append(qic).append(", ");
//			sbSql.append(qic).append("SourceRev")       .append(qic).append("  ");
			sbSql.append(qic).append("DbProductName")   .append(qic).append("  ");
			sbSql.append(") ");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?) \n");
		}
		else if (type.equals(Table.CENTRAL_SESSIONS))
		{
			sbSql.append("insert into ").append(tabName)       .append(" (");
			sbSql.append(qic).append("SessionStartTime")       .append(qic).append(", ");
			sbSql.append(qic).append("Status")                 .append(qic).append(", ");
			sbSql.append(qic).append("ServerName")             .append(qic).append(", ");
			sbSql.append(qic).append("OnHostname")             .append(qic).append(", ");
			sbSql.append(qic).append("ProductString")          .append(qic).append(", ");
			sbSql.append(qic).append("VersionString")          .append(qic).append(", ");
			sbSql.append(qic).append("BuildString")            .append(qic).append(", ");
			sbSql.append(qic).append("CollectorHostname")      .append(qic).append(", ");
			sbSql.append(qic).append("CollectorSampleInterval").append(qic).append(", ");
			sbSql.append(qic).append("CollectorCurrentUrl")    .append(qic).append(", ");
			sbSql.append(qic).append("CollectorInfoFile")      .append(qic).append(", ");
			sbSql.append(qic).append("NumOfSamples")           .append(qic).append(", ");
			sbSql.append(qic).append("LastSampleTime")         .append(qic).append("");
			sbSql.append(") ");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n");
		}
		else if (type.equals(Table.CENTRAL_GRAPH_PROFILES))
		{
			sbSql.append("insert into ").append(tabName)  .append(" (");
			sbSql.append(qic).append("ProductString")     .append(qic).append(", ");
			sbSql.append(qic).append("UserName")          .append(qic).append(", ");
			sbSql.append(qic).append("ProfileName")       .append(qic).append(", ");
			sbSql.append(qic).append("ProfileDescription").append(qic).append(", ");
			sbSql.append(qic).append("ProfileValue")      .append(qic).append(", ");
			sbSql.append(qic).append("ProfileUrlOptions") .append(qic).append("");
			sbSql.append(") ");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?) \n");
		}
		else if (type.equals(Table.SESSION_SAMPLES))
		{
			sbSql.append("insert into ").append(tabName) .append(" (");
			sbSql.append(qic).append("SessionStartTime") .append(qic).append(", ");
			sbSql.append(qic).append("SessionSampleTime").append(qic).append("");
			sbSql.append(") ");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?) \n");
		}
		else if (type.equals(Table.SESSION_SAMPLE_SUM))
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(qic).append("SessionStartTime").append(qic).append(", ");
			sbSql.append(qic).append("CmName")          .append(qic).append(", ");
			sbSql.append(qic).append("graphSamples")    .append(qic).append(", ");
			sbSql.append(qic).append("absSamples")      .append(qic).append(", ");
			sbSql.append(qic).append("diffSamples")     .append(qic).append(", ");
			sbSql.append(qic).append("rateSamples")     .append(qic).append("");
			sbSql.append(") ");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?) \n");
		}
		else if (type.equals(Table.SESSION_SAMPLE_DETAILS))
		{
			sbSql.append("insert into ").append(tabName)      .append(" (");
			sbSql.append(qic).append("SessionStartTime")      .append(qic).append(", ");
			sbSql.append(qic).append("SessionSampleTime")     .append(qic).append(", ");
			sbSql.append(qic).append("CmName")                .append(qic).append(", ");
			sbSql.append(qic).append("type")                  .append(qic).append(", ");
//			sbSql.append(qic).append("graphCount")            .append(qic).append(", ");
//			sbSql.append(qic).append("absRows")               .append(qic).append(", ");
//			sbSql.append(qic).append("diffRows")              .append(qic).append(", ");
//			sbSql.append(qic).append("rateRows")              .append(qic).append(", ");
			sbSql.append(qic).append("graphRecvCount")        .append(qic).append(", ");
			sbSql.append(qic).append("absRecvRows")           .append(qic).append(", ");
			sbSql.append(qic).append("diffRecvRows")          .append(qic).append(", ");
			sbSql.append(qic).append("rateRecvRows")          .append(qic).append(", ");
			sbSql.append(qic).append("graphSaveCount")        .append(qic).append(", ");
			sbSql.append(qic).append("absSaveRows")           .append(qic).append(", ");
			sbSql.append(qic).append("diffSaveRows")          .append(qic).append(", ");
			sbSql.append(qic).append("rateSaveRows")          .append(qic).append(", ");
			sbSql.append(qic).append("sqlRefreshTime")        .append(qic).append(", ");
			sbSql.append(qic).append("guiRefreshTime")        .append(qic).append(", ");
			sbSql.append(qic).append("lcRefreshTime")         .append(qic).append(", ");
			sbSql.append(qic).append("nonCfgMonHappened")     .append(qic).append(", ");
			sbSql.append(qic).append("nonCfgMonMissingParams").append(qic).append(", ");
			sbSql.append(qic).append("nonCfgMonMessages")     .append(qic).append(", ");
			sbSql.append(qic).append("isCountersCleared")     .append(qic).append(", ");
			sbSql.append(qic).append("hasValidSampleData")    .append(qic).append(", ");
			sbSql.append(qic).append("exceptionMsg")          .append(qic).append(", ");
			sbSql.append(qic).append("exceptionFullText")     .append(qic).append(" ");
			sbSql.append(") ");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n");
		}
		else if (type.equals(Table.GRAPH_PROPERTIES))
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(qic).append("SessionStartTime").append(qic).append(", ");
			sbSql.append(qic).append("CmName")          .append(qic).append(", ");
			sbSql.append(qic).append("GraphName")       .append(qic).append(", ");
			sbSql.append(qic).append("TableName")       .append(qic).append(", ");
			sbSql.append(qic).append("GraphLabel")      .append(qic).append(", ");
			sbSql.append(qic).append("GraphCategory")   .append(qic).append(", ");
			sbSql.append(qic).append("isPercentGraph")  .append(qic).append(", ");
			sbSql.append(qic).append("visibleAtStart")  .append(qic).append(", ");
			sbSql.append(qic).append("initialOrder")    .append(qic).append("");
			sbSql.append(") ");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?) \n");
		}
		else if (type.equals(Table.ALARM_ACTIVE))
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(qic).append("alarmClass"             ).append(qic).append(", ");
			sbSql.append(qic).append("serviceType"            ).append(qic).append(", ");
			sbSql.append(qic).append("serviceName"            ).append(qic).append(", ");
			sbSql.append(qic).append("serviceInfo"            ).append(qic).append(", ");
			sbSql.append(qic).append("extraInfo"              ).append(qic).append(", ");
			sbSql.append(qic).append("category"               ).append(qic).append(", ");
			sbSql.append(qic).append("severity"               ).append(qic).append(", ");
			sbSql.append(qic).append("state"                  ).append(qic).append(", ");
			sbSql.append(qic).append("repeatCnt"              ).append(qic).append(", ");
			sbSql.append(qic).append("duration"               ).append(qic).append(", ");
			sbSql.append(qic).append("createTime"             ).append(qic).append(", ");
			sbSql.append(qic).append("cancelTime"             ).append(qic).append(", ");
			sbSql.append(qic).append("timeToLive"             ).append(qic).append(", ");
			sbSql.append(qic).append("threshold"              ).append(qic).append(", ");
			sbSql.append(qic).append("data"                   ).append(qic).append(", ");
			sbSql.append(qic).append("lastData"               ).append(qic).append(", ");
			sbSql.append(qic).append("description"            ).append(qic).append(", ");
			sbSql.append(qic).append("lastDescription"        ).append(qic).append(", ");
			sbSql.append(qic).append("extendedDescription"    ).append(qic).append(", ");
			sbSql.append(qic).append("lastExtendedDescription").append(qic).append(", ");
			sbSql.append(") ");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n");
		}
		else if (type.equals(Table.ALARM_HISTORY))
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(qic).append("SessionStartTime"       ).append(qic).append(", ");
			sbSql.append(qic).append("SessionSampleTime"      ).append(qic).append(", ");
			sbSql.append(qic).append("eventTime"	          ).append(qic).append(", ");
			sbSql.append(qic).append("action"                 ).append(qic).append(", ");
			sbSql.append(qic).append("alarmClass"             ).append(qic).append(", ");
			sbSql.append(qic).append("serviceType"            ).append(qic).append(", ");
			sbSql.append(qic).append("serviceName"            ).append(qic).append(", ");
			sbSql.append(qic).append("serviceInfo"            ).append(qic).append(", ");
			sbSql.append(qic).append("extraInfo"              ).append(qic).append(", ");
			sbSql.append(qic).append("category"               ).append(qic).append(", ");
			sbSql.append(qic).append("severity"               ).append(qic).append(", ");
			sbSql.append(qic).append("state"                  ).append(qic).append(", ");
			sbSql.append(qic).append("repeatCnt"              ).append(qic).append(", ");
			sbSql.append(qic).append("duration"               ).append(qic).append(", ");
			sbSql.append(qic).append("createTime"             ).append(qic).append(", ");
			sbSql.append(qic).append("cancelTime"             ).append(qic).append(", ");
			sbSql.append(qic).append("timeToLive"             ).append(qic).append(", ");
			sbSql.append(qic).append("threshold"              ).append(qic).append(", ");
			sbSql.append(qic).append("data"                   ).append(qic).append(", ");
			sbSql.append(qic).append("lastData"               ).append(qic).append(", ");
			sbSql.append(qic).append("description"            ).append(qic).append(", ");
			sbSql.append(qic).append("lastDescription"        ).append(qic).append(", ");
			sbSql.append(qic).append("extendedDescription"    ).append(qic).append(", ");
			sbSql.append(qic).append("lastExtendedDescription").append(qic).append(", ");
			sbSql.append(") ");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n");
		}
//		else if (type.equals(Table.CHART_LABELS))
//		{
//			sbSql.append("insert into ").append(tabName).append(" (");
//			sbSql.append(qic).append("SessionStartTime").append(qic).append(", ");
//			sbSql.append(qic).append("ServerName")      .append(qic).append(", ");
//			sbSql.append(qic).append("NumOfSamples")    .append(qic).append(", ");
//			sbSql.append(qic).append("LastSampleTime")  .append(qic).append("");
//			sbSql.append(") ");
//			if (addPrepStatementQuestionMarks)
//				sbSql.append("values(?, ?, ?, ?) \n");
//		}
		else if ( Table.ABS.equals(type) || Table.DIFF.equals(type) || Table.RATE.equals(type) )
		{
			sbSql.append("insert into ").append(tabName) .append(" (");
			sbSql.append(qic).append("SessionStartTime") .append(qic).append(", ");
			sbSql.append(qic).append("SessionSampleTime").append(qic).append(", ");
			sbSql.append(qic).append("CmSampleTime")     .append(qic).append(", ");
			sbSql.append(qic).append("CmSampleMs")       .append(qic).append(", ");
			sbSql.append(qic).append("CmNewDiffRateRow") .append(qic).append(", ");
			
			// Get ALL other column names from the CM
//			int cols = cm.getColumnCount();
//			for (int c=0; c<cols; c++) 
//				sbSql.append(qic).append(cm.getColumnName(c)).append(qic).append(", ");
			int cols = cmColumns.size();
			for (int c=0; c<cols; c++) 
				sbSql.append(qic).append(cmColumns.get(c)).append(qic).append(", ");

			// remove last ", "
			sbSql.delete(sbSql.length()-2, sbSql.length());

			// Add ending )
			sbSql.append(") ");

			// add: values(?, ...)
			if (addPrepStatementQuestionMarks)
			{
				sbSql.append("values(?, ?, ?, ?, ?, ");
				for (int c=0; c<cols; c++) 
					sbSql.append("?, ");

				// remove last ", "
				sbSql.delete(sbSql.length()-2, sbSql.length());

				// Add ending )
				sbSql.append(") \n");
			}
		}
		else
		{
			return null;
		}

		String retStr = sbSql.toString(); 
		return retStr;
	}

//	/**
//	 * Helper method to generate a: "insert into TABNAME_GRAPH(c1,c2,c3) [values(?,?...)]"
//	 * @param cm Counter Model info
//	 * @param tgdp Trends Graph information
//	 * @param addPrepStatementQuestionMarks if true add "values(?,?,?...)" which can be used by a prepared statement
//	 * @return
//	 */
//	public String getTableInsertStr(CountersModel cm, TrendGraphDataPoint tgdp, boolean addPrepStatementQuestionMarks)
//	{
//		String tabName = cm.getName() + "_" + tgdp.getName();
//
//		StringBuilder sb = new StringBuilder();
//
//		sb.append("insert into ").append(qic).append(tabName).append(qic).append(" (");
//		sb.append(qic).append("SessionStartTime") .append(qic).append(", ");
//		sb.append(qic).append("SessionSampleTime").append(qic).append(", ");
//		sb.append(qic).append("CmSampleTime")     .append(qic).append(", ");
//
//		// loop all data
//		Double[] dataArr  = tgdp.getData();
//		if (dataArr == null)
//			return null;
//		for (int d=0; d<dataArr.length; d++)
//		{
//			sb.append(qic).append("label_").append(d).append(qic).append(", ");
//			sb.append(qic).append("data_") .append(d).append(qic).append(", ");
//		}
//		// remove last ", "
//		sb.delete(sb.length()-2, sb.length());
//
//		// Add ending )
//		sb.append(") \n");
//
//		// add: values(?, ...)
//		if (addPrepStatementQuestionMarks)
//		{
//			sb.append("values(?, ?, ?, ");
//			for (int d=0; d<dataArr.length; d++)
//				sb.append("?, ?, ");
//
//			// remove last ", "
//			sb.delete(sb.length()-2, sb.length());
//
//			// Add ending )
//			sb.append(") \n");
//		}
//
//		String retStr = sb.toString(); 
//		return retStr;
//	}


	/** Helper method to generate a DDL string, to get the 'create index' 
	 * @param schemaName */
	public String getIndexDdlString(String schemaName, Table type, CountersModel cm)
	{
		String qic = getQuotedIdentifierChar();

		if (type.equals(Table.CENTRAL_VERSION_INFO))
		{
			return null;
		}
//		else if (type.equals(Table.CHART_LABELS))
//		{
//			return null;
//		}
		else if ( type.equals(Table.ABS) || type.equals(Table.DIFF) || type.equals(Table.RATE) )
		{
			String tabName   = getTableName(null, type, cm, false);
			String tabPrefix = qic + schemaName + qic + ".";
//			return "create index " + qic+tabName+"_ix1"+qic + " on " + qic+tabName+qic + "("+qic+"SampleTime"+qic+", "+qic+"SessionSampleTime"+qic+")\n";

			if ( DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(getDatabaseProductName()) )
				return "create index " +                    tabName+"_ix1"     + " on " + tabPrefix+qic+tabName+qic + "("+qic+"SessionSampleTime"+qic+")\n";
			else
				return "create index " + qic+schemaName+"_"+tabName+"_ix1"+qic + " on " + tabPrefix+qic+tabName+qic + "("+qic+"SessionSampleTime"+qic+")\n";
		}
		else
		{
			return null;
		}
	}

//	public String getGraphTableDdlString(String schemaName, String tabName, TrendGraphDataPoint tgdp)
//	{
//		String qic = getQuotedIdentifierChar();
//		StringBuilder sb = new StringBuilder();
//
//		String tabPrefix = qic + schemaName + qic + ".";
//
//		sb.append("create table " + tabPrefix+qic+tabName+qic + "\n");
//		sb.append("( \n");
//		sb.append("    "+fill(qic+"SessionStartTime" +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//		sb.append("   ,"+fill(qic+"SessionSampleTime"+qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//		sb.append("   ,"+fill(qic+"CmSampleTime"     +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//		sb.append("\n");
//
//		// loop all data
//		Double[] dataArr  = tgdp.getData();
//		for (int d=0; d<dataArr.length; d++)
//		{
//			sb.append("   ,"+fill(qic+"label_"+d+qic,40)+" "+fill(getDatatype("varchar",100,-1,-1),20)+" "+getNullable(true)+"\n");
//			sb.append("   ,"+fill(qic+"data_" +d+qic,40)+" "+fill(getDatatype("numeric", -1,16, 1),20)+" "+getNullable(true)+"\n");
//		}
//		sb.append(") \n");
//
//		//System.out.println("getGraphTableDdlString: "+sb.toString());
//		return sb.toString();
//	}
	public String getGraphTableDdlString(String schemaName, String tabName, GraphEntry ge)
	{
		String qic = getQuotedIdentifierChar();
		StringBuilder sb = new StringBuilder();

		String tabPrefix = qic + schemaName + qic + ".";

		sb.append("create table " + tabPrefix+qic+tabName+qic + "\n");
		sb.append("( \n");
		sb.append("    "+fill(qic+"SessionStartTime" +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
		sb.append("   ,"+fill(qic+"SessionSampleTime"+qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
		sb.append("   ,"+fill(qic+"CmSampleTime"     +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
		sb.append("\n");

		// loop all labels
		for (String label : ge._labelValue.keySet())
			sb.append("   ,"+fill(qic+label+qic,40)+" "+fill(getDatatype("numeric", -1,16, 2),20)+" "+getNullable(true)+"\n");
		sb.append(") \n");

		//System.out.println("getGraphTableDdlString: "+sb.toString());
		return sb.toString();
	}

//	public String getGraphIndexDdlString(String schemaName, String tabName, TrendGraphDataPoint tgdp)
//	{
//		String qic = getQuotedIdentifierChar();
//		String tabPrefix = qic + schemaName + qic + ".";
//		
////		String sql = "create index " + qic+tgdp.getName()+"_ix1"+qic + " on " + qic+tabName+qic + "("+qic+"SampleTime"+qic+", "+qic+"SessionSampleTime"+qic+")\n"; 
//		if ( DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(getDatabaseProductName()) )
//			return "create index " +                   tgdp.getName()+"_ix1"     + " on " + tabPrefix+qic+tabName+qic + "("+qic+"SessionSampleTime"+qic+")\n";
//		else
//			return "create index " + qic+tabPrefix+"_"+tgdp.getName()+"_ix1"+qic + " on " + tabPrefix+qic+tabName+qic + "("+qic+"SessionSampleTime"+qic+")\n";
//	}
	public String getGraphIndexDdlString(String schemaName, String tabName, GraphEntry ge)
	{
		String qic = getQuotedIdentifierChar();
		String tabPrefix = qic + schemaName + qic + ".";
		
//		String sql = "create index " + qic+tgdp.getName()+"_ix1"+qic + " on " + qic+tabName+qic + "("+qic+"SampleTime"+qic+", "+qic+"SessionSampleTime"+qic+")\n"; 
		if ( DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(getDatabaseProductName()) )
			return "create index " + ge.getName()+"_ix1"    + " on " + tabPrefix+qic+tabName+qic + "("+qic+"SessionSampleTime"+qic+")\n";
		else
			return "create index " + qic+tabName+"_ix1"+qic + " on " + tabPrefix+qic+tabName+qic + "("+qic+"SessionSampleTime"+qic+")\n";
	}

//	public String getGraphAlterTableDdlString(Connection conn, String schemaName, String tabName, TrendGraphDataPoint tgdp)
//	throws SQLException
//	{
//		// Obtain a DatabaseMetaData object from our current connection
//		DatabaseMetaData dbmd = conn.getMetaData();
//
//		int colCounter = 0;
//		ResultSet rs = dbmd.getColumns(null, null, tabName, "%");
//		while(rs.next())
//		{
//			colCounter++;
//		}
//		rs.close();
//
//		if (colCounter > 0)
//		{
//			colCounter -= 3; // take away: SessionStartTime, SessionSampleTime, SampleTime
//			colCounter = colCounter / 2;
//			
//			Double[] dataArr  = tgdp.getData();
//			if (colCounter < dataArr.length)
//			{
//				StringBuilder sb = new StringBuilder();
//				sb.append("alter table " + qic+tabName+qic + "\n");
//				
//				for (int d=colCounter; d<dataArr.length; d++)
//				{
////					sb.append("alter table " + qic+tabName+qic + " add  "+fill(qic+"label_"+d+qic,40)+" "+fill(getDatatype("varchar",30,-1,-1),20)+" "+getNullable(true)+" \n");
////					sb.append("alter table " + qic+tabName+qic + " add  "+fill(qic+"data_" +d+qic,40)+" "+fill(getDatatype("numeric",-1,10, 1),20)+" "+getNullable(true)+" \n");
//					sb.append("   add  "+fill(qic+"label_"+d+qic,40)+" "+fill(getDatatype("varchar",30,-1,-1),20)+" "+getNullable(true)+",\n");
//					sb.append("        "+fill(qic+"data_" +d+qic,40)+" "+fill(getDatatype("numeric",-1,10, 1),20)+" "+getNullable(true)+" \n");
//				}
//				//System.out.println("getGraphAlterTableDdlString: "+sb.toString());
//				return sb.toString();
//			}
//		}
//		return "";
//	}

//	public List<String> getGraphAlterTableDdlString(DbxConnection conn, String tabName, TrendGraphDataPoint tgdp)
//	throws SQLException
//	{
//		String qic = getQuotedIdentifierChar();
//
//		// Obtain a DatabaseMetaData object from our current connection
//		DatabaseMetaData dbmd = conn.getMetaData();
//
//		int colCounter = 0;
//		ResultSet rs = dbmd.getColumns(null, null, tabName, "%");
//		while(rs.next())
//		{
//			colCounter++;
//		}
//		rs.close();
//
//		List<String> list = new ArrayList<String>();
//		if (colCounter > 0)
//		{
//			colCounter -= 3; // take away: SessionStartTime, SessionSampleTime, SampleTime
//			colCounter = colCounter / 2;
//			
//			Double[] dataArr  = tgdp.getData();
//			if (colCounter < dataArr.length)
//			{
//				for (int d=colCounter; d<dataArr.length; d++)
//				{
//					list.add("alter table " + qic+tabName+qic + " add  "+fill(qic+"label_"+d+qic,40)+" "+fill(getDatatype("varchar",60,-1,-1),20)+" "+getNullable(true)+" \n");
//					list.add("alter table " + qic+tabName+qic + " add  "+fill(qic+"data_" +d+qic,40)+" "+fill(getDatatype("numeric",-1,10, 1),20)+" "+getNullable(true)+" \n");
//				}
//			}
//		}
//		return list;
//	}
	public List<String> getGraphAlterTableDdlString(DbxConnection conn, String schemaName, String tabName, GraphEntry ge)
	throws SQLException
	{
		String qic = getQuotedIdentifierChar();

		String tabPrefix = qic + schemaName + qic + ".";

		// Obtain a DatabaseMetaData object from our current connection
		DatabaseMetaData dbmd = conn.getMetaData();

		LinkedHashSet<String> tabCols = new LinkedHashSet<>();
		ResultSet rs = dbmd.getColumns(null, schemaName, tabName, "%");
		while(rs.next())
			tabCols.add( rs.getString("COLUMN_NAME"));
		rs.close();

		LinkedHashSet<String> geCols = new LinkedHashSet<>();
		for (String label : ge._labelValue.keySet())
			geCols.add(label);
		
//System.out.println("########################################## getGraphAlterTableDdlString(): table-cols="+tabCols);
//System.out.println("########################################## getGraphAlterTableDdlString():    ge-cols="+geCols);

		// Remove "existing table columns"
		geCols.removeAll(tabCols);

//		cols.remove("SessionStartTime");
//		cols.remove("SessionSampleTime");
//		cols.remove("CmSampleTime");

//		// Remove columns from the GraphEntry that we are about to insert into... the columns left are the columns we need to add/alter to the table
//		for (String label : ge._labelValue.keySet())
//		{
//System.out.println("########################################## getGraphAlterTableDdlString(): removingCo='"+label+"' from cols...");
//			cols.remove(label);
//		}

//System.out.println("########################################## getGraphAlterTableDdlString(): alterCols ="+geCols);
		List<String> list = new ArrayList<String>();
		for (String addCol : geCols)
		{
			list.add("alter table " + tabPrefix + qic+tabName+qic + " add  "+fill(qic+addCol+qic,40)+" "+fill(getDatatype("numeric",-1,16, 2),20)+" "+getNullable(true)+" \n");
		}
		return list;
	}




	private CentralPcsWriterStatistics _writerStatistics = new CentralPcsWriterStatistics();

	@Override 
	public CentralPcsWriterStatistics getStatistics() 
	{
		if (_writerStatistics == null)
			_writerStatistics = new CentralPcsWriterStatistics();

		return _writerStatistics; 
	}

	@Override
	public void resetCounters()
	{ 
		_writerStatistics.clear();
	}
}

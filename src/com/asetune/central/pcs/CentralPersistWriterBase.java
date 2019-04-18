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
	 *   <li> 2 - Add    column 'CollectorSampleInterval'                      to table 'DbxCentralSessions' </li>
	 *   <li> 3 - Add    column 'ProfileDescription'                           to table 'DbxCentralGraphProfiles' </li>
	 *   <li> 4 - Add    column 'CollectorCurrentUrl', 'Status'                to table 'DbxCentralSessions' </li>
	 *   <li> 5 - Add    column 'CollectorInfoFile'                            to table 'DbxCentralSessions' </li>
	 *   <li> 6 - Add    column 'category'                                     to table 'ALARM_ACTIVE, ALARM_HISTORY' in all schemas</li>
	 *   <li> 7 - Add    column 'ProfileUrlOptions'                            to table 'DbxCentralGraphProfiles' </li>
	 *   <li> 8 - Add    column 'GraphCategory'                                to table 'DbxGraphProperties' </li>
	 *   <li> 9 - Change column 'data', 'lastData' from varchar(80) -> 160     in tables *schema*.'DbxAlarmActive', *schema*.'DbxAlarmHistory' </li>
	 * </ul> 
	 */
	public static int DBX_CENTRAL_DB_VERSION = 9;
	
	
	public enum Table
	{
		CENTRAL_VERSION_INFO, 
		CENTRAL_SESSIONS,
		CENTRAL_GRAPH_PROFILES,
		CENTRAL_USERS,
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
//	public static String  _qic = "\"";

	
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

//	public static void setQuotedIdentifierChar(String qic)
//	{
//		_qic = qic;
//	}
//	public static String getQuotedIdentifierChar()
//	{
//		return _qic;
//	}


	//-------------------------------------------------------------
	// BEGIN: FAKE Database Quoted Identifier Chars
	//-------------------------------------------------------------
	/** Left and Right Quote String used is SQL String to "fake" any real DBMS Vendor Specific Quoted Identifier, use conn.quotifySqlString() they will  be replaced with "real" Quotes */
	public static String[] _replaceQiChars = new String[] { "[", "]" };

	public static void setReplaceQuotedIdentifierChars(String leftQuote, String rightQuote)
	{
		_replaceQiChars = new String[] {leftQuote, rightQuote};
	}
	public static String[] getReplaceQuotedIdentifierChars()
	{
		return _replaceQiChars;
	}
	public static String getLeftQuoteReplace()
	{
		return _replaceQiChars[0];
	}
	public static String getRightQuoteReplace()
	{
		return _replaceQiChars[0];
	}
	//-------------------------------------------------------------
	// END: FAKE Database Quoted Identifier Chars
	//-------------------------------------------------------------


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
	public static String getTableName(DbxConnection conn, String schemaName, Table type, CountersModel cm, boolean addQuotedIdentifierChar)
	{
		String lq = "";
		String rq = "";
		if (addQuotedIdentifierChar)
		{
			lq = getLeftQuoteReplace();
			rq = getLeftQuoteReplace();
			if (conn != null)
			{
				lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
				rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection
			}
		}

		String prefix = "";
		if (StringUtil.hasValue(schemaName))
		{
			prefix = lq + schemaName + rq + ".";
		}

		switch (type)
		{
		case CENTRAL_VERSION_INFO:     return          lq + "DbxCentralVersionInfo"       + rq;
		case CENTRAL_SESSIONS:         return          lq + "DbxCentralSessions"          + rq;
		case CENTRAL_GRAPH_PROFILES:   return          lq + "DbxCentralGraphProfiles"     + rq;
		case CENTRAL_USERS:            return          lq + "DbxCentralUsers"             + rq;
		case SESSION_SAMPLES:          return prefix + lq + "DbxSessionSamples"           + rq;
		case SESSION_SAMPLE_SUM:       return prefix + lq + "DbxSessionSampleSum"         + rq;
		case SESSION_SAMPLE_DETAILS:   return prefix + lq + "DbxSessionSampleDetailes"    + rq;
		case ALARM_ACTIVE:             return prefix + lq + "DbxAlarmActive"              + rq;
		case ALARM_HISTORY:            return prefix + lq + "DbxAlarmHistory"             + rq;
		case GRAPH_PROPERTIES:         return prefix + lq + "DbxGraphProperties"          + rq;
//		case CHART_LABELS:             return prefix + lq + "DbxChartLabels"              + rq;
		case ABS:                      return prefix + lq + cm.getName() + "_abs"         + rq;
		case DIFF:                     return prefix + lq + cm.getName() + "_diff"        + rq;
		case RATE:                     return prefix + lq + cm.getName() + "_rate"        + rq;
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
	public String getTableDdlString(DbxConnection conn, String schemaName, Table type, CountersModel cm)
	throws SQLException
	{
		String tabName = getTableName(conn, schemaName, type, cm, true);
		StringBuffer sbSql = new StringBuffer();
		
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

		try
		{
			if (Table.CENTRAL_VERSION_INFO.equals(type))
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
//				sbSql.append("    "+fill(lq+"SessionStartTime"+rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("    "+fill(lq+"ProductString"   +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"VersionString"   +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"BuildString"     +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"DbVersion"       +rq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"SourceDate"      +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"SourceRev"       +rq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"DbProductName"   +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+lq+"ProductString"+rq+")\n");
//				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionStartTime"+rq+")\n");
				sbSql.append(") \n");
			}
			else if (Table.CENTRAL_SESSIONS.equals(type))
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"SessionStartTime"        +rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"Status"                  +rq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"ServerName"              +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"OnHostname"              +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"ProductString"           +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"VersionString"           +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"BuildString"             +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"CollectorHostname"       +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"CollectorSampleInterval" +rq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"CollectorCurrentUrl"     +rq,40)+" "+fill(getDatatype("varchar", 80,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"CollectorInfoFile"       +rq,40)+" "+fill(getDatatype("varchar",256,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"NumOfSamples"            +rq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"LastSampleTime"          +rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionStartTime"+rq+")\n");
				sbSql.append(") \n");
			}
			else if (Table.CENTRAL_GRAPH_PROFILES.equals(type))
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"ProductString"      +rq,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"UserName"           +rq,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"ProfileName"        +rq,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"ProfileDescription" +rq,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"ProfileValue"       +rq,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"ProfileUrlOptions"  +rq,40)+" "+fill(getDatatype("varchar", 1024,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+lq+"ProductString"+rq+", "+lq+"UserName"+rq+", "+lq+"ProfileName"+rq+")\n");
				sbSql.append(") \n");
			}
			else if (Table.CENTRAL_USERS.equals(type))
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"UserName"           +rq,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"Password"           +rq,40)+" "+fill(getDatatype("varchar",   60,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"Email"              +rq,40)+" "+fill(getDatatype("varchar",   60,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"Roles"              +rq,40)+" "+fill(getDatatype("varchar",  120,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+lq+"UserName"+rq+")\n");
				sbSql.append(") \n");
			}
			else if (Table.SESSION_SAMPLES.equals(type))
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"SessionSampleTime"+rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionSampleTime"+rq+", "+lq+"SessionStartTime"+rq+")\n");
				sbSql.append(") \n");
			}
			else if (Table.SESSION_SAMPLE_SUM.equals(type))
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"CmName"           +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"graphSamples"     +rq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"absSamples"       +rq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"diffSamples"      +rq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"rateSamples"      +rq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionStartTime"+rq+", "+lq+"CmName"+rq+")\n");
				sbSql.append(") \n");
			}
			else if (Table.SESSION_SAMPLE_DETAILS.equals(type))
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"SessionStartTime"      +rq,40)+" "+fill(getDatatype("datetime",-1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"SessionSampleTime"     +rq,40)+" "+fill(getDatatype("datetime",-1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"CmName"                +rq,40)+" "+fill(getDatatype("varchar", 30,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"type"                  +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"graphCount"            +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"absRows"               +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"diffRows"              +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"rateRows"              +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"graphRecvCount"        +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"absRecvRows"           +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"diffRecvRows"          +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"rateRecvRows"          +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"graphSaveCount"        +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"absSaveRows"           +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"diffSaveRows"          +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"rateSaveRows"          +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"sqlRefreshTime"        +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"guiRefreshTime"        +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"lcRefreshTime"         +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"nonCfgMonHappened"     +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"nonCfgMonMissingParams"+rq,40)+" "+fill(getDatatype("varchar", 100, -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"nonCfgMonMessages"     +rq,40)+" "+fill(getDatatype("varchar", 1024,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"isCountersCleared"     +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"hasValidSampleData"    +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"exceptionMsg"          +rq,40)+" "+fill(getDatatype("varchar", 1024,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"exceptionFullText"     +rq,40)+" "+fill(getDatatype("text",    -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionSampleTime"+rq+", "+lq+"CmName"+rq+", "+lq+"SessionStartTime"+rq+")\n");
				sbSql.append(") \n");
			}
			else if (Table.GRAPH_PROPERTIES.equals(type))
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"SessionStartTime"+rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"CmName"          +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"GraphName"       +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"TableName"       +rq,40)+" "+fill(getDatatype("varchar",128,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"GraphLabel"      +rq,40)+" "+fill(getDatatype("varchar",255,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"GraphCategory"   +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"isPercentGraph"  +rq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"visibleAtStart"  +rq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"initialOrder"    +rq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("\n");
//				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionStartTime"+rq+", "+lq+"CmName"+rq+", "+lq+"GraphName"+rq+")\n");
				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionStartTime"+rq+", "+lq+"GraphName"+rq+")\n");
				sbSql.append(") \n");
			}
			else if (Table.ALARM_ACTIVE.equals(type))
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"alarmClass"                 +rq,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"serviceType"                +rq,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"serviceName"                +rq,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"serviceInfo"                +rq,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"extraInfo"                  +rq,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"category"                   +rq,40)+" "+fill(getDatatype("varchar",   20,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"severity"                   +rq,40)+" "+fill(getDatatype("varchar",   10,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"state"                      +rq,40)+" "+fill(getDatatype("varchar",   10,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"repeatCnt"                  +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"duration"                   +rq,40)+" "+fill(getDatatype("varchar",   10,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"createTime"                 +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"cancelTime"                 +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"timeToLive"                 +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"threshold"                  +rq,40)+" "+fill(getDatatype("varchar",   15,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"data"                       +rq,40)+" "+fill(getDatatype("varchar",  160,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"lastData"                   +rq,40)+" "+fill(getDatatype("varchar",  160,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"description"                +rq,40)+" "+fill(getDatatype("varchar",  512,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"lastDescription"            +rq,40)+" "+fill(getDatatype("varchar",  512,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"extendedDescription"        +rq,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"lastExtendedDescription"    +rq,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+lq+"alarmClass"+rq+", "+lq+"serviceType"+rq+", "+lq+"serviceName"+rq+", "+lq+"serviceInfo"+rq+", "+lq+"extraInfo"+rq+")\n");
				sbSql.append(") \n");
			}
			else if (Table.ALARM_HISTORY.equals(type))
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"SessionStartTime"           +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"SessionSampleTime"          +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"eventTime"	                 +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"action"                     +rq,40)+" "+fill(getDatatype("varchar",   15,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"isActive"                   +rq,40)+" "+fill(getDatatype("bit",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"alarmClass"                 +rq,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"serviceType"                +rq,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"serviceName"                +rq,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"serviceInfo"                +rq,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"extraInfo"                  +rq,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"category"                   +rq,40)+" "+fill(getDatatype("varchar",   20,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"severity"                   +rq,40)+" "+fill(getDatatype("varchar",   10,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"state"                      +rq,40)+" "+fill(getDatatype("varchar",   10,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"repeatCnt"                  +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"duration"                   +rq,40)+" "+fill(getDatatype("varchar",   10,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"createTime"                 +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"cancelTime"                 +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"timeToLive"                 +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"threshold"                  +rq,40)+" "+fill(getDatatype("varchar",   15,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"data"                       +rq,40)+" "+fill(getDatatype("varchar",  160,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"lastData"                   +rq,40)+" "+fill(getDatatype("varchar",  160,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"description"                +rq,40)+" "+fill(getDatatype("varchar",  512,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"lastDescription"            +rq,40)+" "+fill(getDatatype("varchar",  512,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"extendedDescription"        +rq,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"lastExtendedDescription"    +rq,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true )+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+lq+"eventTime"+rq+", "+lq+"action"+rq+", "+lq+"alarmClass"+rq+", "+lq+"serviceType"+rq+", "+lq+"serviceName"+rq+", "+lq+"serviceInfo"+rq+", "+lq+"extraInfo"+rq+")\n");
				sbSql.append(") \n");
			}
//			else if (Table.CHART_LABELS.equals(type))
//			{
//				sbSql.append("create table " + tabName + "\n");
//				sbSql.append("( \n");
//				sbSql.append("    "+fill(lq+"SessionStartTime"+rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"ServerName"      +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"NumOfSamples"    +rq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"LastSampleTime"  +rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("\n");
//				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionStartTime"+rq+")\n");
//				sbSql.append(") \n");
//			}
			else if ( Table.ABS.equals(type) || Table.DIFF.equals(type) || Table.RATE.equals(type) )
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(rq+"SessionStartTime" +lq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(rq+"SessionSampleTime"+lq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(rq+"CmSampleTime"     +lq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(rq+"CmSampleMs"       +lq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(rq+"CmNewDiffRateRow" +lq,40)+" "+fill(getDatatype("tinyint", -1,-1,-1),20)+" "+getNullable(false)+"\n");
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


					String colName = fill( lq + rsmd.getColumnLabel(c) + rq,    40);
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
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection
		
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

			String colName = fill( lq + rsmd.getColumnLabel(c) + rq,    40);
			String dtName  = fill(getDatatype(c, rsmd, isDeltaOrPct),   20);
			String nullable= getNullable(c, rsmd, isDeltaOrPct);

			list.add("alter table " + lq+tabName+rq + " add  " + colName + " " + dtName + " " + nullable);
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
	public String getTableInsertStr(DbxConnection conn, String schemaName, Table type, CountersModel cm, boolean addPrepStatementQuestionMarks)
	throws SQLException
	{
		return getTableInsertStr(conn, schemaName, type, cm, addPrepStatementQuestionMarks, null);
	}
	/**
	 * Helper method to generate a: "insert into TABNAME(c1,c2,c3) [values(?,?...)]"
	 * @param type ABS | DIFF | RATE | SYSTEM_TYPE
	 * @param cm Counter Model info (can be null if SYSTEM type is used)
	 * @param tgdp Trends Graph information
	 * @param addPrepStatementQuestionMarks if true add "values(?,?,?...)" which can be used by a prepared statement
	 * @return
	 */
	public String getTableInsertStr(DbxConnection conn, String schemaName, Table type, CountersModel cm, boolean addPrepStatementQuestionMarks, List<String> cmColumns)
	throws SQLException
	{
		String lq = getLeftQuoteReplace();
		String rq = getLeftQuoteReplace();
		if (conn != null)
		{
			lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
			rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection
		}

		String tabName = getTableName(conn, schemaName, type, cm, true);
		StringBuffer sbSql = new StringBuffer();

		if (type.equals(Table.CENTRAL_VERSION_INFO))
		{
			sbSql.append("insert into ").append(tabName).append(" (");
//			sbSql.append(lq).append("SessionStartTime").append(rq).append(", ");
			sbSql.append(lq).append("ProductString")   .append(rq).append(", ");
			sbSql.append(lq).append("VersionString")   .append(rq).append(", ");
			sbSql.append(lq).append("BuildString")     .append(rq).append(", ");
			sbSql.append(lq).append("DbVersion")       .append(rq).append(", ");
//			sbSql.append(lq).append("SourceDate")      .append(rq).append(", ");
//			sbSql.append(lq).append("SourceRev")       .append(rq).append("  ");
			sbSql.append(lq).append("DbProductName")   .append(rq).append("  ");
			sbSql.append(") ");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?) \n");
		}
		else if (type.equals(Table.CENTRAL_SESSIONS))
		{
			sbSql.append("insert into ").append(tabName)      .append(" (");
			sbSql.append(lq).append("SessionStartTime")       .append(rq).append(", ");
			sbSql.append(lq).append("Status")                 .append(rq).append(", ");
			sbSql.append(lq).append("ServerName")             .append(rq).append(", ");
			sbSql.append(lq).append("OnHostname")             .append(rq).append(", ");
			sbSql.append(lq).append("ProductString")          .append(rq).append(", ");
			sbSql.append(lq).append("VersionString")          .append(rq).append(", ");
			sbSql.append(lq).append("BuildString")            .append(rq).append(", ");
			sbSql.append(lq).append("CollectorHostname")      .append(rq).append(", ");
			sbSql.append(lq).append("CollectorSampleInterval").append(rq).append(", ");
			sbSql.append(lq).append("CollectorCurrentUrl")    .append(rq).append(", ");
			sbSql.append(lq).append("CollectorInfoFile")      .append(rq).append(", ");
			sbSql.append(lq).append("NumOfSamples")           .append(rq).append(", ");
			sbSql.append(lq).append("LastSampleTime")         .append(rq).append("");
			sbSql.append(") ");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n");
		}
		else if (type.equals(Table.CENTRAL_GRAPH_PROFILES))
		{
			sbSql.append("insert into ").append(tabName) .append(" (");
			sbSql.append(lq).append("ProductString")     .append(rq).append(", ");
			sbSql.append(lq).append("UserName")          .append(rq).append(", ");
			sbSql.append(lq).append("ProfileName")       .append(rq).append(", ");
			sbSql.append(lq).append("ProfileDescription").append(rq).append(", ");
			sbSql.append(lq).append("ProfileValue")      .append(rq).append(", ");
			sbSql.append(lq).append("ProfileUrlOptions") .append(rq).append("");
			sbSql.append(") ");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?) \n");
		}
		else if (type.equals(Table.CENTRAL_USERS))
		{
			sbSql.append("insert into ").append(tabName)  .append(" (");
			sbSql.append(lq).append("UserName").append(rq).append(", ");
			sbSql.append(lq).append("Password").append(rq).append(", ");
			sbSql.append(lq).append("Email")   .append(rq).append(", ");
			sbSql.append(lq).append("Roles")   .append(rq).append("");
			sbSql.append(") ");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?) \n");
		}
		else if (type.equals(Table.SESSION_SAMPLES))
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(lq).append("SessionStartTime") .append(rq).append(", ");
			sbSql.append(lq).append("SessionSampleTime").append(rq).append("");
			sbSql.append(") ");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?) \n");
		}
		else if (type.equals(Table.SESSION_SAMPLE_SUM))
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(lq).append("SessionStartTime").append(rq).append(", ");
			sbSql.append(lq).append("CmName")          .append(rq).append(", ");
			sbSql.append(lq).append("graphSamples")    .append(rq).append(", ");
			sbSql.append(lq).append("absSamples")      .append(rq).append(", ");
			sbSql.append(lq).append("diffSamples")     .append(rq).append(", ");
			sbSql.append(lq).append("rateSamples")     .append(rq).append("");
			sbSql.append(") ");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?) \n");
		}
		else if (type.equals(Table.SESSION_SAMPLE_DETAILS))
		{
			sbSql.append("insert into ").append(tabName)     .append(" (");
			sbSql.append(lq).append("SessionStartTime")      .append(rq).append(", ");
			sbSql.append(lq).append("SessionSampleTime")     .append(rq).append(", ");
			sbSql.append(lq).append("CmName")                .append(rq).append(", ");
			sbSql.append(lq).append("type")                  .append(rq).append(", ");
//			sbSql.append(lq).append("graphCount")            .append(rq).append(", ");
//			sbSql.append(lq).append("absRows")               .append(rq).append(", ");
//			sbSql.append(lq).append("diffRows")              .append(rq).append(", ");
//			sbSql.append(lq).append("rateRows")              .append(rq).append(", ");
			sbSql.append(lq).append("graphRecvCount")        .append(rq).append(", ");
			sbSql.append(lq).append("absRecvRows")           .append(rq).append(", ");
			sbSql.append(lq).append("diffRecvRows")          .append(rq).append(", ");
			sbSql.append(lq).append("rateRecvRows")          .append(rq).append(", ");
			sbSql.append(lq).append("graphSaveCount")        .append(rq).append(", ");
			sbSql.append(lq).append("absSaveRows")           .append(rq).append(", ");
			sbSql.append(lq).append("diffSaveRows")          .append(rq).append(", ");
			sbSql.append(lq).append("rateSaveRows")          .append(rq).append(", ");
			sbSql.append(lq).append("sqlRefreshTime")        .append(rq).append(", ");
			sbSql.append(lq).append("guiRefreshTime")        .append(rq).append(", ");
			sbSql.append(lq).append("lcRefreshTime")         .append(rq).append(", ");
			sbSql.append(lq).append("nonCfgMonHappened")     .append(rq).append(", ");
			sbSql.append(lq).append("nonCfgMonMissingParams").append(rq).append(", ");
			sbSql.append(lq).append("nonCfgMonMessages")     .append(rq).append(", ");
			sbSql.append(lq).append("isCountersCleared")     .append(rq).append(", ");
			sbSql.append(lq).append("hasValidSampleData")    .append(rq).append(", ");
			sbSql.append(lq).append("exceptionMsg")          .append(rq).append(", ");
			sbSql.append(lq).append("exceptionFullText")     .append(rq).append(" ");
			sbSql.append(") ");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n");
		}
		else if (type.equals(Table.GRAPH_PROPERTIES))
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(lq).append("SessionStartTime").append(rq).append(", ");
			sbSql.append(lq).append("CmName")          .append(rq).append(", ");
			sbSql.append(lq).append("GraphName")       .append(rq).append(", ");
			sbSql.append(lq).append("TableName")       .append(rq).append(", ");
			sbSql.append(lq).append("GraphLabel")      .append(rq).append(", ");
			sbSql.append(lq).append("GraphCategory")   .append(rq).append(", ");
			sbSql.append(lq).append("isPercentGraph")  .append(rq).append(", ");
			sbSql.append(lq).append("visibleAtStart")  .append(rq).append(", ");
			sbSql.append(lq).append("initialOrder")    .append(rq).append("");
			sbSql.append(") ");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?) \n");
		}
		else if (type.equals(Table.ALARM_ACTIVE))
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(lq).append("alarmClass"             ).append(rq).append(", ");
			sbSql.append(lq).append("serviceType"            ).append(rq).append(", ");
			sbSql.append(lq).append("serviceName"            ).append(rq).append(", ");
			sbSql.append(lq).append("serviceInfo"            ).append(rq).append(", ");
			sbSql.append(lq).append("extraInfo"              ).append(rq).append(", ");
			sbSql.append(lq).append("category"               ).append(rq).append(", ");
			sbSql.append(lq).append("severity"               ).append(rq).append(", ");
			sbSql.append(lq).append("state"                  ).append(rq).append(", ");
			sbSql.append(lq).append("repeatCnt"              ).append(rq).append(", ");
			sbSql.append(lq).append("duration"               ).append(rq).append(", ");
			sbSql.append(lq).append("createTime"             ).append(rq).append(", ");
			sbSql.append(lq).append("cancelTime"             ).append(rq).append(", ");
			sbSql.append(lq).append("timeToLive"             ).append(rq).append(", ");
			sbSql.append(lq).append("threshold"              ).append(rq).append(", ");
			sbSql.append(lq).append("data"                   ).append(rq).append(", ");
			sbSql.append(lq).append("lastData"               ).append(rq).append(", ");
			sbSql.append(lq).append("description"            ).append(rq).append(", ");
			sbSql.append(lq).append("lastDescription"        ).append(rq).append(", ");
			sbSql.append(lq).append("extendedDescription"    ).append(rq).append(", ");
			sbSql.append(lq).append("lastExtendedDescription").append(rq).append(", ");
			sbSql.append(") ");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n");
		}
		else if (type.equals(Table.ALARM_HISTORY))
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(lq).append("SessionStartTime"       ).append(rq).append(", ");
			sbSql.append(lq).append("SessionSampleTime"      ).append(rq).append(", ");
			sbSql.append(lq).append("eventTime"              ).append(rq).append(", ");
			sbSql.append(lq).append("action"                 ).append(rq).append(", ");
			sbSql.append(lq).append("alarmClass"             ).append(rq).append(", ");
			sbSql.append(lq).append("serviceType"            ).append(rq).append(", ");
			sbSql.append(lq).append("serviceName"            ).append(rq).append(", ");
			sbSql.append(lq).append("serviceInfo"            ).append(rq).append(", ");
			sbSql.append(lq).append("extraInfo"              ).append(rq).append(", ");
			sbSql.append(lq).append("category"               ).append(rq).append(", ");
			sbSql.append(lq).append("severity"               ).append(rq).append(", ");
			sbSql.append(lq).append("state"                  ).append(rq).append(", ");
			sbSql.append(lq).append("repeatCnt"              ).append(rq).append(", ");
			sbSql.append(lq).append("duration"               ).append(rq).append(", ");
			sbSql.append(lq).append("createTime"             ).append(rq).append(", ");
			sbSql.append(lq).append("cancelTime"             ).append(rq).append(", ");
			sbSql.append(lq).append("timeToLive"             ).append(rq).append(", ");
			sbSql.append(lq).append("threshold"              ).append(rq).append(", ");
			sbSql.append(lq).append("data"                   ).append(rq).append(", ");
			sbSql.append(lq).append("lastData"               ).append(rq).append(", ");
			sbSql.append(lq).append("description"            ).append(rq).append(", ");
			sbSql.append(lq).append("lastDescription"        ).append(rq).append(", ");
			sbSql.append(lq).append("extendedDescription"    ).append(rq).append(", ");
			sbSql.append(lq).append("lastExtendedDescription").append(rq).append(", ");
			sbSql.append(") ");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n");
		}
//		else if (type.equals(Table.CHART_LABELS))
//		{
//			sbSql.append("insert into ").append(tabName).append(" (");
//			sbSql.append(lq).append("SessionStartTime").append(rq).append(", ");
//			sbSql.append(lq).append("ServerName")      .append(rq).append(", ");
//			sbSql.append(lq).append("NumOfSamples")    .append(rq).append(", ");
//			sbSql.append(lq).append("LastSampleTime")  .append(rq).append("");
//			sbSql.append(") ");
//			if (addPrepStatementQuestionMarks)
//				sbSql.append("values(?, ?, ?, ?) \n");
//		}
		else if ( Table.ABS.equals(type) || Table.DIFF.equals(type) || Table.RATE.equals(type) )
		{
			sbSql.append("insert into ").append(tabName) .append(" (");
			sbSql.append(lq).append("SessionStartTime") .append(rq).append(", ");
			sbSql.append(lq).append("SessionSampleTime").append(rq).append(", ");
			sbSql.append(lq).append("CmSampleTime")     .append(rq).append(", ");
			sbSql.append(lq).append("CmSampleMs")       .append(rq).append(", ");
			sbSql.append(lq).append("CmNewDiffRateRow") .append(rq).append(", ");
			
			// Get ALL other column names from the CM
//			int cols = cm.getColumnCount();
//			for (int c=0; c<cols; c++) 
//				sbSql.append(lq).append(cm.getColumnName(c)).append(rq).append(", ");
			int cols = cmColumns.size();
			for (int c=0; c<cols; c++) 
				sbSql.append(lq).append(cmColumns.get(c)).append(rq).append(", ");

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
	public String getIndexDdlString(DbxConnection conn, String schemaName, Table type, CountersModel cm)
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

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
			String tabName   = getTableName(conn, null, type, cm, false);
			String tabPrefix = lq + schemaName + rq + ".";

			return "create index " + lq+schemaName+"_"+tabName+"_ix1"+rq + " on " + tabPrefix+lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";
			
//			if ( DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(getDatabaseProductName()) )
//				return "create index " +                   tabName+"_ix1"    + " on " + tabPrefix+lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";
//			else
//				return "create index " + lq+schemaName+"_"+tabName+"_ix1"+rq + " on " + tabPrefix+lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";
		}
		else
		{
			return null;
		}
	}

//	public String getGraphTableDdlString(String schemaName, String tabName, TrendGraphDataPoint tgdp)
//	{
//		String lq = getLeftQuoteReplace();
//		String rq = getLeftQuoteReplace();
//		StringBuilder sb = new StringBuilder();
//
//		String tabPrefix = lq + schemaName + rq + ".";
//
//		sb.append("create table " + tabPrefix+lq+tabName+rq + "\n");
//		sb.append("( \n");
//		sb.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//		sb.append("   ,"+fill(lq+"SessionSampleTime"+rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//		sb.append("   ,"+fill(lq+"CmSampleTime"     +rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//		sb.append("\n");
//
//		// loop all data
//		Double[] dataArr  = tgdp.getData();
//		for (int d=0; d<dataArr.length; d++)
//		{
//			sb.append("   ,"+fill(lq+"label_"+d+rq,40)+" "+fill(getDatatype("varchar",100,-1,-1),20)+" "+getNullable(true)+"\n");
//			sb.append("   ,"+fill(lq+"data_" +d+rq,40)+" "+fill(getDatatype("numeric", -1,16, 1),20)+" "+getNullable(true)+"\n");
//		}
//		sb.append(") \n");
//
//		//System.out.println("getGraphTableDdlString: "+sb.toString());
//		return sb.toString();
//	}
	public String getGraphTableDdlString(DbxConnection conn, String schemaName, String tabName, GraphEntry ge)
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

		StringBuilder sb = new StringBuilder();

		String tabPrefix = lq + schemaName + rq + ".";

		sb.append("create table " + tabPrefix+lq+tabName+rq + "\n");
		sb.append("( \n");
		sb.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
		sb.append("   ,"+fill(lq+"SessionSampleTime"+rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
		sb.append("   ,"+fill(lq+"CmSampleTime"     +rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
		sb.append("\n");

		// loop all labels
		for (String label : ge._labelValue.keySet())
			sb.append("   ,"+fill(lq+label+rq,40)+" "+fill(getDatatype("numeric", -1,16, 2),20)+" "+getNullable(true)+"\n");
		sb.append(") \n");

		//System.out.println("getGraphTableDdlString: "+sb.toString());
		return sb.toString();
	}

//	public String getGraphIndexDdlString(String schemaName, String tabName, TrendGraphDataPoint tgdp)
//	{
//		String lq = getLeftQuoteReplace();
//		String rq = getLeftQuoteReplace();
//		String tabPrefix = lq + schemaName + rq + ".";
//		
////		String sql = "create index " + lq+tgdp.getName()+"_ix1"+rq + " on " + lq+tabName+rq + "("+lq+"SampleTime"+rq+", "+lq+"SessionSampleTime"+rq+")\n"; 
//		if ( DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(getDatabaseProductName()) )
//			return "create index " +                   tgdp.getName()+"_ix1"     + " on " + tabPrefix+lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";
//		else
//			return "create index " + lq+tabPrefix+"_"+tgdp.getName()+"_ix1"+rq + " on " + tabPrefix+lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";
//	}
	public String getGraphIndexDdlString(DbxConnection conn, String schemaName, String tabName, GraphEntry ge)
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection
		
		String tabPrefix = lq + schemaName + rq + ".";
		
		return "create index " + lq+tabName+"_ix1"+rq    + " on " + tabPrefix+lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";
		
//		if ( DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(getDatabaseProductName()) )
//			return "create index " +    ge.getName()+"_ix1"  + " on " + tabPrefix+lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";
//		else
//			return "create index " + lq+tabName+"_ix1"+rq    + " on " + tabPrefix+lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";
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
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

		String tabPrefix = lq + schemaName + rq + ".";

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
			list.add("alter table " + tabPrefix + lq+tabName+rq + " add  "+fill(lq+addCol+rq,40)+" "+fill(getDatatype("numeric",-1,16, 2),20)+" "+getNullable(true)+" \n");
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

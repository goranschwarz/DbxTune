/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.pcs;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.AseConfig;
import com.asetune.TrendGraphDataPoint;
import com.asetune.cm.CountersModel;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.utils.Configuration;
import com.asetune.utils.TimeUtils;


public abstract class PersistWriterBase
    implements IPersistWriter
{
    /** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(PersistWriterBase.class);

	/*---------------------------------------------------
	** DEFINITIONS/STATIC variables
	**---------------------------------------------------
	*/
	public static final int VERSION_INFO             = 0;
	public static final int SESSIONS                 = 1;
	public static final int SESSION_PARAMS           = 2;
	public static final int SESSION_SAMPLES          = 3;
	public static final int SESSION_SAMPLE_SUM       = 4;
	public static final int SESSION_SAMPLE_DETAILES  = 5;
	public static final int SESSION_MON_TAB_DICT     = 6;
	public static final int SESSION_MON_TAB_COL_DICT = 7;
	public static final int SESSION_ASE_CONFIG       = 8;
	public static final int SESSION_ASE_CONFIG_TEXT  = 9;
	public static final int DDL_STORAGE              = 50;
	public static final int ABS                      = 100;
	public static final int DIFF                     = 101;
	public static final int RATE                     = 102;
	
	public static final int SESSION_PARAMS_VAL_MAXLEN = 4096;

	/** Character used for quoted identifier */
	public static String  qic = "\"";
	
	/**	what are we connected to: DatabaseMetaData.getDatabaseProductName() */
	private String _databaseProductName = "";
	
	/** List some known DatabaseProductName that we can use here */
	public static String DB_PROD_NAME_ASE = "Adaptive Server Enterprise";
	public static String DB_PROD_NAME_ASA = "SQL Anywhere";
	public static String DB_PROD_NAME_H2  = "H2";


	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/
	/** when DDL is "created" we do not need to do this again */
	private List<String> _saveDdlIsCalled = new LinkedList<String>();

	private int _inserts      = 0;
	private int _updates      = 0;
	private int _deletes      = 0;

	private int _createTables = 0;
	private int _alterTables  = 0;
	private int _dropTables   = 0;

	/** Determines if a session is started or not, or needs initialization */
	private boolean _isSessionStarted = false;

	/** Session start time is maintained from PersistCounterHandler */
	private Timestamp _sessionStartTime = null;


	
	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/
	// NONE

	
	/*---------------------------------------------------
	** Methods Need to be implemented by sub classes
	**---------------------------------------------------
	*/
	/** Need to be implemented by the implementation */
	@Override
	abstract public String getName();

	/** Need to be implemented by the implementation */
	@Override
	abstract public void init(Configuration props) throws Exception;


	
	
	/*---------------------------------------------------
	** Methods handling counters
	**---------------------------------------------------
	*/
	@Override public void incInserts() { _inserts++; }
	@Override public void incUpdates() { _updates++; }
	@Override public void incDeletes() { _deletes++; }

	@Override public void incInserts(int cnt) { _inserts += cnt; }
	@Override public void incUpdates(int cnt) { _updates += cnt; }
	@Override public void incDeletes(int cnt) { _deletes += cnt; }

	@Override public void incCreateTables() { _createTables++; }
	@Override public void incAlterTables()  { _alterTables++;  }
	@Override public void incDropTables()   { _dropTables++;   }

	@Override public void incCreateTables(int cnt) { _createTables += cnt; }
	@Override public void incAlterTables (int cnt) { _alterTables  += cnt; }
	@Override public void incDropTables  (int cnt) { _dropTables   += cnt; }

	
	@Override public int getInserts() { return _inserts; }
	@Override public int getUpdates() { return _updates; }
	@Override public int getDeletes() { return _deletes; }

	@Override public int getCreateTables() { return _createTables; }
	@Override public int getAlterTables()  { return _alterTables;  }
	@Override public int getDropTables()   { return _dropTables;   }

	
	@Override
	public void resetCounters()
	{ 
		_inserts = 0;
		_updates = 0;
		_deletes = 0;

		_createTables = 0;
		_alterTables  = 0;
		_dropTables   = 0;
	}
	
	/*---------------------------------------------------
	** Methods implementing: IPersistWriter
	**---------------------------------------------------
	*/
	/** Empty implementation */
	@Override
	public void beginOfSample(PersistContainer cont)
	{
	}

	/** Empty implementation */
	@Override
	public void endOfSample(PersistContainer cont, boolean caughtErrors)
	{
	}


	@Override
	public boolean isSessionStarted()
	{
		return _isSessionStarted;
	}
	
	@Override
	public void setSessionStarted(boolean isSessionStarted)
	{
		_isSessionStarted = isSessionStarted;
	}

	/** Empty implementation */
	@Override
	public void startSession(PersistContainer cont)
	{
	}

	/**
	 * Used by PersistCounterHandler to set a new session start time
	 */
	@Override
	public void setSessionStartTime(Timestamp sessionStartTime)
	{
		_sessionStartTime  = sessionStartTime;
	}


	/**
	 * Used by PersistCounterHandler to get the session start time
	 */
	@Override
	public Timestamp getSessionStartTime()
	{
		return _sessionStartTime;
	}	

//	/** Empty implementation */
//	@Override
//	public void saveSession(PersistContainer cont)
//	{
//	}

	/** Empty implementation */
	@Override
	public void saveCounters(CountersModel cm)
	{
	}

	/** Empty implementation */
	@Override
	public void saveDdlDetails(DdlDetails ddlDetails)
	{
	}

	/** 
	 * Dummy implement, just saying "everything is already stored"<br>
	 * So any writer that does NOT want to store DDL's simply wont be 
	 * called with saveDdl(), since the record is already stored
	 */
	@Override
	public boolean isDdlDetailsStored(String dbname, String objectName)
	{
		return true;
	}

	/** Empty implementation */
	@Override
	public void markDdlDetailsAsStored(String dbname, String objectName)
	{
	}

	/** Empty implementation */
	@Override
	public void clearDdlDetailesCache()
	{
	}

	/** Empty implementation */
	@Override
	public void populateDdlDetailesCache()
	{
	}

	/** Empty implementation */
	@Override
	public boolean saveDdl(CountersModel cm)
	{
		return true;
	}
	
//	@Override
	public void clearIsDdlCreatedCache()
	{
		_logger.info("Clearing the in-memory cache, which tells us if a specific table has been created or not.");
		_saveDdlIsCalled.clear();
	}

	@Override
	public boolean isDdlCreated(CountersModel cm)
	{
		String tabName = cm.getName();

		return _saveDdlIsCalled.contains(tabName);
	}

//	@Override
	public boolean isDdlCreated(String tabName)
	{
		return _saveDdlIsCalled.contains(tabName);
	}

	@Override
	public void markDdlAsCreated(CountersModel cm)
	{
		String tabName = cm.getName();

		_saveDdlIsCalled.add(tabName);
	}

//	@Override
	public void markDdlAsCreated(String tabName)
	{
		_saveDdlIsCalled.add(tabName);
	}


	
	
	
	
	
	/*---------------------------------------------------
	** HELPER Methods that subclasses can use
	**---------------------------------------------------
	*/
	public void setDatabaseProductName(String databaseProductName)
	{
		_databaseProductName = databaseProductName;
	}
	public String getDatabaseProductName()
	{
		return _databaseProductName;
	}

	/** Helper method */
	public String fill(String str, int fill)
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
	public String getDatatype(String type, int length, int prec, int scale)
	{
		if ( type.equals("char") || type.equals("varchar") )
			type = type + "(" + length + ")";
		
		if ( type.equals("numeric") || type.equals("decimal") )
			type = type + "(" + prec + "," + scale + ")";

		return type;
	}
	public String getDatatype(int col, ResultSetMetaData rsmd, boolean isDeltaOrPct)
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
		}
		else
		{
			type  = rsmd.getColumnTypeName(col);
			if ( type.equals("char") || type.equals("varchar") )
			{
				length = rsmd.getColumnDisplaySize(col);
				prec   = -1;
				scale  = -1;
			}
			
			if ( type.equals("numeric") || type.equals("decimal") )
			{
				length = -1;
				prec   = rsmd.getPrecision(col);
				scale  = rsmd.getScale(col);
			}

			// Most databases doesn't have unsigned datatypes, so lets leave "unsigned int" as "int"
			if ( type.startsWith("unsigned ") )
			{
				String newType = type.substring("unsigned ".length());
				_logger.info("Found the uncommon data type '"+type+"', instead the data type '"+newType+"' will be used.");
				type = newType;
			}
		}
		return getDatatype(type, length, prec, scale);
	}
	public String getNullable(boolean nullable)
	{
		return nullable ? "    null" : "not null";
		
	}
	public String getNullable(int col, ResultSetMetaData rsmd, boolean isDeltaOrPct)
	throws SQLException
	{
		// datatype "bit" can't be NULL declared in ASE
		String type  = rsmd.getColumnTypeName(col);
		if (type != null && type.equalsIgnoreCase("bit"))
			return getNullable(false);
		else
			return getNullable(true);
//		return getNullable(rsmd.isNullable(col)>0);
	}

	/** Helper method to get a table name */
	public static String getTableName(int type, CountersModel cm, boolean addQuotedIdentifierChar)
	{
		String q = "";
		if (addQuotedIdentifierChar)
			q = qic;

		switch (type)
		{
		case VERSION_INFO:             return q + "MonVersionInfo"              + q;
		case SESSIONS:                 return q + "MonSessions"                 + q;
		case SESSION_PARAMS:           return q + "MonSessionParams"            + q;
		case SESSION_SAMPLES:          return q + "MonSessionSamples"           + q;
		case SESSION_SAMPLE_SUM:       return q + "MonSessionSampleSum"         + q;
		case SESSION_SAMPLE_DETAILES:  return q + "MonSessionSampleDetailes"    + q;
		case SESSION_MON_TAB_DICT:     return q + "MonSessionMonTablesDict"     + q;
		case SESSION_MON_TAB_COL_DICT: return q + "MonSessionMonTabColumnsDict" + q;
		case SESSION_ASE_CONFIG:       return q + "MonSessionAseConfig"         + q;
		case SESSION_ASE_CONFIG_TEXT:  return q + "MonSessionAseConfigText"     + q;
		case DDL_STORAGE:              return q + "MonDdlStorage"               + q;
		case ABS:                      return q + cm.getName() + "_abs"         + q;
		case DIFF:                     return q + cm.getName() + "_diff"        + q;
		case RATE:                     return q + cm.getName() + "_rate"        + q;
		default:
			throw new RuntimeException("Unknown type of '"+type+"' in getTableName()."); 
		}
	}

	/** Helper method to get a table name for GRAPH tables */
	public static String getTableName(CountersModel cm, TrendGraphDataPoint tgdp, boolean addQuotedIdentifierChar)
	{
		String q = "";
		if (addQuotedIdentifierChar)
			q = qic;

		String tabName = q + cm.getName() + "_" + tgdp.getName() + q;
		return tabName;
	}

	/** Helper method to generate a DDL string, to get the 'create table' */
	public String getTableDdlString(int type, CountersModel cm)
	throws SQLException
	{
		String tabName = getTableName(type, cm, true);
		StringBuffer sbSql = new StringBuffer();

		try
		{
			if (type == VERSION_INFO)
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"SessionStartTime"+qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"ProductString"   +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"VersionString"   +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"BuildString"     +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"SourceDate"      +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"SourceRev"       +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+qic+"SessionStartTime"+qic+")\n");
				sbSql.append(") \n");
			}
			else if (type == SESSIONS)
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"SessionStartTime"+qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"ServerName"      +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"NumOfSamples"    +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"LastSampleTime"  +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+qic+"SessionStartTime"+qic+")\n");
				sbSql.append(") \n");
			}
			else if (type == SESSION_PARAMS)
			{
//				int len = SESSION_PARAMS_VAL_MAXLEN;

				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"SessionStartTime"+qic,40)+" "+fill(getDatatype("datetime",-1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"Type"            +qic,40)+" "+fill(getDatatype("varchar", 20,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"ParamName"       +qic,40)+" "+fill(getDatatype("varchar", 255, -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(qic+"ParamValue"      +qic,40)+" "+fill(getDatatype("varchar", len, -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"ParamValue"      +qic,40)+" "+fill(getDatatype("text",    -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+qic+"SessionStartTime"+qic+", "+qic+"Type"+qic+", "+qic+"ParamName"+qic+")\n");
				sbSql.append(") \n");
			}
			else if (type == SESSION_SAMPLES)
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"SessionStartTime" +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"SessionSampleTime"+qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("\n");
//				sbSql.append("   ,PRIMARY KEY ("+qic+"SessionStartTime"+qic+", "+qic+"SessionSampleTime"+qic+")\n");
				sbSql.append("   ,PRIMARY KEY ("+qic+"SessionSampleTime"+qic+", "+qic+"SessionStartTime"+qic+")\n");
				sbSql.append(") \n");
			}
			else if (type == SESSION_SAMPLE_SUM)
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"SessionStartTime" +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"CmName"           +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"absSamples"       +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"diffSamples"      +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"rateSamples"      +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+qic+"SessionStartTime"+qic+", "+qic+"CmName"+qic+")\n");
				sbSql.append(") \n");
			}
			else if (type == SESSION_SAMPLE_DETAILES)
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"SessionStartTime"      +qic,40)+" "+fill(getDatatype("datetime",-1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"SessionSampleTime"     +qic,40)+" "+fill(getDatatype("datetime",-1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"CmName"                +qic,40)+" "+fill(getDatatype("varchar", 30,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"type"                  +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"graphCount"            +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"absRows"               +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"diffRows"              +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"rateRows"              +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"sqlRefreshTime"        +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"guiRefreshTime"        +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"lcRefreshTime"         +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"nonCfgMonHappened"     +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"nonCfgMonMissingParams"+qic,40)+" "+fill(getDatatype("varchar", 100, -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"nonCfgMonMessages"     +qic,40)+" "+fill(getDatatype("varchar", 1024,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append(") \n");
			}
			else if (type == SESSION_MON_TAB_DICT)
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"SessionStartTime" +qic,40)+" "+fill(getDatatype("datetime",-1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"TableID"          +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"Columns"          +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"Parameters"       +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"Indicators"       +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"Size"             +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"TableName"        +qic,40)+" "+fill(getDatatype("varchar", 30,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"Description"      +qic,40)+" "+fill(getDatatype("varchar", 1800,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append(") \n");
			}
			else if (type == SESSION_MON_TAB_COL_DICT)
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"SessionStartTime" +qic,40)+" "+fill(getDatatype("datetime",-1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"TableID"          +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"ColumnID"         +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"TypeID"           +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"Precision"        +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"Scale"            +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"Length"           +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"Indicators"       +qic,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"TableName"        +qic,40)+" "+fill(getDatatype("varchar", 30,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"ColumnName"       +qic,40)+" "+fill(getDatatype("varchar", 30,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"TypeName"         +qic,40)+" "+fill(getDatatype("varchar", 30,  -1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"Description"      +qic,40)+" "+fill(getDatatype("varchar", 1800,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append(") \n");
			}
			else if (type == SESSION_ASE_CONFIG)
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"SessionStartTime" +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");

				// Get ALL other column names from the AseConfig dictionary
				AseConfig aseCfg = AseConfig.getInstance();				
				for (int i=0; i<aseCfg.getColumnCount(); i++)
				{
					sbSql.append("   ,"+fill(qic+aseCfg.getColumnName(i)+qic,40)+" "+fill(aseCfg.getSqlDataType(i),20)+" "+getNullable( aseCfg.getSqlDataType(i).equalsIgnoreCase("bit") ? false : true)+"\n");
				}
				sbSql.append(") \n");
			}
			else if (type == SESSION_ASE_CONFIG_TEXT)
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"SessionStartTime" +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"configName"       +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"configText"       +qic,40)+" "+fill(getDatatype("text",    -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append(") \n");
			}
			else if (type == DDL_STORAGE)
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"dbname"           +qic,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"owner"            +qic,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"objectName"       +qic,40)+" "+fill(getDatatype("varchar",  255,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"type"             +qic,40)+" "+fill(getDatatype("varchar",   20,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"crdate"           +qic,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"sampleTime"       +qic,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"source"           +qic,40)+" "+fill(getDatatype("varchar",  255,-1,-1),20)+" "+getNullable(true) +"\n");
				sbSql.append("   ,"+fill(qic+"dependParent"     +qic,40)+" "+fill(getDatatype("varchar",  255,-1,-1),20)+" "+getNullable(true) +"\n");
				sbSql.append("   ,"+fill(qic+"dependLevel"      +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"dependList"       +qic,40)+" "+fill(getDatatype("varchar", 1500,-1,-1),20)+" "+getNullable(true) +"\n");
				sbSql.append("   ,"+fill(qic+"objectText"       +qic,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true) +"\n");
				sbSql.append("   ,"+fill(qic+"dependsText"      +qic,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true) +"\n");
				sbSql.append("   ,"+fill(qic+"optdiagText"      +qic,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true) +"\n");
				sbSql.append("   ,"+fill(qic+"extraInfoText"    +qic,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true) +"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+qic+"dbname"+qic+", "+qic+"owner"+qic+", "+qic+"objectName"+qic+")\n");
				sbSql.append(") \n");
			}
			else if (type == ABS || type == DIFF || type == RATE)
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"SessionStartTime" +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"SessionSampleTime"+qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"CmSampleTime"     +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"CmSampleMs"       +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
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
					if (type == DIFF)
					{
						if ( cm.isPctColumn(c-1) )
							isDeltaOrPct = true;
					}

					if (type == RATE)
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

	/**
	 * Helper method to generate a: "insert into TABNAME(c1,c2,c3) [values(?,?...)]"
	 * @param type ABS | DIFF | RATE | SYSTEM_TYPE
	 * @param cm Counter Model info (can be null if SYSTEM type is used)
	 * @param tgdp Trends Graph information
	 * @param addPrepStatementQuestionMarks if true add "values(?,?,?...)" which can be used by a prepared statement
	 * @return
	 */
	public String getTableInsertStr(int type, CountersModel cm, boolean addPrepStatementQuestionMarks)
	throws SQLException
	{
		String tabName = getTableName(type, cm, true);
		StringBuffer sbSql = new StringBuffer();

		if (type == VERSION_INFO)
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(qic).append("SessionStartTime").append(qic).append(", ");
			sbSql.append(qic).append("ProductString")   .append(qic).append(", ");
			sbSql.append(qic).append("VersionString")   .append(qic).append(", ");
			sbSql.append(qic).append("BuildString")     .append(qic).append(", ");
			sbSql.append(qic).append("SourceDate")      .append(qic).append(", ");
			sbSql.append(qic).append("SourceRev")       .append(qic).append("");
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?) \n");
		}
		else if (type == SESSIONS)
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(qic).append("SessionStartTime").append(qic).append(", ");
			sbSql.append(qic).append("ServerName")      .append(qic).append(", ");
			sbSql.append(qic).append("NumOfSamples")    .append(qic).append(", ");
			sbSql.append(qic).append("LastSampleTime")  .append(qic).append("");
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?) \n");
		}
		else if (type == SESSION_PARAMS)
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(qic).append("SessionStartTime").append(qic).append(", ");
			sbSql.append(qic).append("Type")            .append(qic).append(", ");
			sbSql.append(qic).append("ParamName")       .append(qic).append(", ");
			sbSql.append(qic).append("ParamValue")      .append(qic).append("");
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?) \n");
		}
		else if (type == SESSION_SAMPLES)
		{
			sbSql.append("insert into ").append(tabName) .append(" (");
			sbSql.append(qic).append("SessionStartTime") .append(qic).append(", ");
			sbSql.append(qic).append("SessionSampleTime").append(qic).append("");
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?) \n");
		}
		else if (type == SESSION_SAMPLE_SUM)
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(qic).append("SessionStartTime").append(qic).append(", ");
			sbSql.append(qic).append("CmName")          .append(qic).append(", ");
			sbSql.append(qic).append("absSamples")      .append(qic).append(", ");
			sbSql.append(qic).append("diffSamples")     .append(qic).append(", ");
			sbSql.append(qic).append("rateSamples")     .append(qic).append("");
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?) \n");
		}
		else if (type == SESSION_SAMPLE_DETAILES)
		{
			sbSql.append("insert into ").append(tabName)      .append(" (");
			sbSql.append(qic).append("SessionStartTime")      .append(qic).append(", ");
			sbSql.append(qic).append("SessionSampleTime")     .append(qic).append(", ");
			sbSql.append(qic).append("CmName")                .append(qic).append(", ");
			sbSql.append(qic).append("type")                  .append(qic).append(", ");
			sbSql.append(qic).append("graphCount")            .append(qic).append(", ");
			sbSql.append(qic).append("absRows")               .append(qic).append(", ");
			sbSql.append(qic).append("diffRows")              .append(qic).append(", ");
			sbSql.append(qic).append("rateRows")              .append(qic).append(", ");
			sbSql.append(qic).append("sqlRefreshTime")        .append(qic).append(", ");
			sbSql.append(qic).append("guiRefreshTime")        .append(qic).append(", ");
			sbSql.append(qic).append("lcRefreshTime")         .append(qic).append(", ");
			sbSql.append(qic).append("nonCfgMonHappened")     .append(qic).append(", ");
			sbSql.append(qic).append("nonCfgMonMissingParams").append(qic).append(", ");
			sbSql.append(qic).append("nonCfgMonMessages")     .append(qic).append(" ");
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n");
		}
		else if (type == SESSION_MON_TAB_DICT)
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(qic).append("SessionStartTime").append(qic).append(", ");
			sbSql.append(qic).append("TableID")         .append(qic).append(", ");
			sbSql.append(qic).append("Columns")         .append(qic).append(", ");
			sbSql.append(qic).append("Parameters")      .append(qic).append(", ");
			sbSql.append(qic).append("Indicators")      .append(qic).append(", ");
			sbSql.append(qic).append("Size")            .append(qic).append(", ");
			sbSql.append(qic).append("TableName")       .append(qic).append(", ");
			sbSql.append(qic).append("Description")     .append(qic).append("");
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?) \n");
		}
		else if (type == SESSION_MON_TAB_COL_DICT)
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(qic).append("SessionStartTime").append(qic).append(", ");
			sbSql.append(qic).append("TableID")         .append(qic).append(", ");
			sbSql.append(qic).append("ColumnID")        .append(qic).append(", ");
			sbSql.append(qic).append("TypeID")          .append(qic).append(", ");
			sbSql.append(qic).append("Precision")       .append(qic).append(", ");
			sbSql.append(qic).append("Scale")           .append(qic).append(", ");
			sbSql.append(qic).append("Length")          .append(qic).append(", ");
			sbSql.append(qic).append("Indicators")      .append(qic).append(", ");
			sbSql.append(qic).append("TableName")       .append(qic).append(", ");
			sbSql.append(qic).append("ColumnName")      .append(qic).append(", ");
			sbSql.append(qic).append("TypeName")        .append(qic).append(", ");
			sbSql.append(qic).append("Description")     .append(qic).append("");
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n");
		}
		else if (type == SESSION_ASE_CONFIG)
		{
			sbSql.append("insert into ").append(tabName).append("(");
			sbSql.append(qic).append("SessionStartTime").append(qic).append(", ");

			// Get ALL other column names from the AseConfig dictionary
			AseConfig aseCfg = AseConfig.getInstance();				
			for (int i=0; i<aseCfg.getColumnCount(); i++)
				sbSql.append(qic).append(aseCfg.getColumnName(i)).append(qic).append(", ");

			// remove last ", "
			sbSql.delete(sbSql.length()-2, sbSql.length());

			// Add ending )
			sbSql.append(") \n");

			// add: values(?, ...)
			if (addPrepStatementQuestionMarks)
			{
				sbSql.append("values(?, ");
				for (int i=0; i<aseCfg.getColumnCount(); i++)
					sbSql.append("?, ");

				// remove last ", "
				sbSql.delete(sbSql.length()-2, sbSql.length());

				// Add ending )
				sbSql.append(") \n");
			}
		}
		else if (type == SESSION_ASE_CONFIG_TEXT)
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(qic).append("SessionStartTime").append(qic).append(", ");
			sbSql.append(qic).append("configName")      .append(qic).append(", ");
			sbSql.append(qic).append("configText")      .append(qic).append("");
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?) \n");
		}
		else if (type == DDL_STORAGE)
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(qic).append("dbname")       .append(qic).append(", ");
			sbSql.append(qic).append("owner")        .append(qic).append(", ");
			sbSql.append(qic).append("objectName")   .append(qic).append(", ");
			sbSql.append(qic).append("type")         .append(qic).append(", ");
			sbSql.append(qic).append("crdate")       .append(qic).append(", ");
			sbSql.append(qic).append("sampleTime")   .append(qic).append(", ");
			sbSql.append(qic).append("source")       .append(qic).append(", ");
			sbSql.append(qic).append("dependParent") .append(qic).append(", ");
			sbSql.append(qic).append("dependLevel")  .append(qic).append(", ");
			sbSql.append(qic).append("dependList")   .append(qic).append(", ");
			sbSql.append(qic).append("objectText")   .append(qic).append(", ");
			sbSql.append(qic).append("dependsText")  .append(qic).append(", ");
			sbSql.append(qic).append("optdiagText")  .append(qic).append(", ");
			sbSql.append(qic).append("extraInfoText").append(qic).append("");
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n");
		}
		else if (type == ABS || type == DIFF || type == RATE)
		{
			sbSql.append("insert into ").append(tabName) .append(" (");
			sbSql.append(qic).append("SessionStartTime") .append(qic).append(", ");
			sbSql.append(qic).append("SessionSampleTime").append(qic).append(", ");
			sbSql.append(qic).append("CmSampleTime")     .append(qic).append(", ");
			sbSql.append(qic).append("CmSampleMs")       .append(qic).append(", ");
			
			// Get ALL other column names from the CM
			int cols = cm.getColumnCount();
			for (int c=0; c<cols; c++) 
				sbSql.append(qic).append(cm.getColumnName(c)).append(qic).append(", ");

			// remove last ", "
			sbSql.delete(sbSql.length()-2, sbSql.length());

			// Add ending )
			sbSql.append(") \n");

			// add: values(?, ...)
			if (addPrepStatementQuestionMarks)
			{
				sbSql.append("values(?, ?, ?, ?, ");
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

	/**
	 * Helper method to generate a: "insert into TABNAME_GRAPH(c1,c2,c3) [values(?,?...)]"
	 * @param cm Counter Model info
	 * @param tgdp Trends Graph information
	 * @param addPrepStatementQuestionMarks if true add "values(?,?,?...)" which can be used by a prepared statement
	 * @return
	 */
	public String getTableInsertStr(CountersModel cm, TrendGraphDataPoint tgdp, boolean addPrepStatementQuestionMarks)
	{
		String tabName = cm.getName() + "_" + tgdp.getName();

		StringBuilder sb = new StringBuilder();

		sb.append("insert into ").append(qic).append(tabName).append(qic).append(" (");
		sb.append(qic).append("SessionStartTime") .append(qic).append(", ");
		sb.append(qic).append("SessionSampleTime").append(qic).append(", ");
		sb.append(qic).append("CmSampleTime")     .append(qic).append(", ");

		// loop all data
		Double[] dataArr  = tgdp.getData();
		if (dataArr == null)
			return null;
		for (int d=0; d<dataArr.length; d++)
		{
			sb.append(qic).append("label_").append(d).append(qic).append(", ");
			sb.append(qic).append("data_") .append(d).append(qic).append(", ");
		}
		// remove last ", "
		sb.delete(sb.length()-2, sb.length());

		// Add ending )
		sb.append(") \n");

		// add: values(?, ...)
		if (addPrepStatementQuestionMarks)
		{
			sb.append("values(?, ?, ?, ");
			for (int d=0; d<dataArr.length; d++)
				sb.append("?, ?, ");

			// remove last ", "
			sb.delete(sb.length()-2, sb.length());

			// Add ending )
			sb.append(") \n");
		}

		String retStr = sb.toString(); 
		return retStr;
	}


	/** Helper method to generate a DDL string, to get the 'create index' */
	public String getIndexDdlString(int type, CountersModel cm)
	{
		if (type == VERSION_INFO)
		{
			return null;
		}
		else if (type == SESSIONS)
		{
			return null;
		}
		else if (type == SESSION_PARAMS)
		{
			return null;
		}
		else if (type == SESSION_SAMPLES)
		{
			return null;
		}
		else if (type == SESSION_SAMPLE_SUM)
		{
			return null;
		}
		else if (type == SESSION_SAMPLE_DETAILES)
		{
			String tabName = getTableName(type, null, false);
			if ( DB_PROD_NAME_ASE.equals(getDatabaseProductName()) )
				return "create index " +     tabName+"_ix1"     + " on " + qic+tabName+qic + "("+qic+"SessionSampleTime"+qic+")\n";
			else
				return "create index " + qic+tabName+"_ix1"+qic + " on " + qic+tabName+qic + "("+qic+"SessionSampleTime"+qic+")\n";
		}
		else if (type == SESSION_MON_TAB_DICT)
		{
			return null;
		}
		else if (type == SESSION_MON_TAB_COL_DICT)
		{
			return null;
		}
		else if (type == SESSION_ASE_CONFIG)
		{
			return null;
		}
		else if (type == SESSION_ASE_CONFIG_TEXT)
		{
			return null;
		}
		else if (type == DDL_STORAGE)
		{
			return null;
		}
		else if (type == ABS || type == DIFF || type == RATE)
		{
			String tabName = getTableName(type, cm, false);
//			return "create index " + qic+tabName+"_ix1"+qic + " on " + qic+tabName+qic + "("+qic+"SampleTime"+qic+", "+qic+"SessionSampleTime"+qic+")\n";

			if ( DB_PROD_NAME_ASE.equals(getDatabaseProductName()) )
				return "create index " +     tabName+"_ix1"     + " on " + qic+tabName+qic + "("+qic+"SessionSampleTime"+qic+")\n";
			else
				return "create index " + qic+tabName+"_ix1"+qic + " on " + qic+tabName+qic + "("+qic+"SessionSampleTime"+qic+")\n";
		}
		else
		{
			return null;
		}
	}

	public String getGraphTableDdlString(String tabName, TrendGraphDataPoint tgdp)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("create table " + qic+tabName+qic + "\n");
		sb.append("( \n");
		sb.append("    "+fill(qic+"SessionStartTime" +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
		sb.append("   ,"+fill(qic+"SessionSampleTime"+qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
		sb.append("   ,"+fill(qic+"CmSampleTime"     +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
		sb.append("\n");

		// loop all data
		Double[] dataArr  = tgdp.getData();
		for (int d=0; d<dataArr.length; d++)
		{
			sb.append("   ,"+fill(qic+"label_"+d+qic,40)+" "+fill(getDatatype("varchar",60,-1,-1),20)+" "+getNullable(true)+"\n");
			sb.append("   ,"+fill(qic+"data_" +d+qic,40)+" "+fill(getDatatype("numeric",-1,16, 1),20)+" "+getNullable(true)+"\n");
		}
		sb.append(") \n");

		//System.out.println("getGraphTableDdlString: "+sb.toString());
		return sb.toString();
	}

	public String getGraphIndexDdlString(String tabName, TrendGraphDataPoint tgdp)
	{
//		String sql = "create index " + qic+tgdp.getName()+"_ix1"+qic + " on " + qic+tabName+qic + "("+qic+"SampleTime"+qic+", "+qic+"SessionSampleTime"+qic+")\n"; 
		if ( DB_PROD_NAME_ASE.equals(getDatabaseProductName()) )
			return "create index " +     tgdp.getName()+"_ix1"     + " on " + qic+tabName+qic + "("+qic+"SessionSampleTime"+qic+")\n";
		else
			return "create index " + qic+tgdp.getName()+"_ix1"+qic + " on " + qic+tabName+qic + "("+qic+"SessionSampleTime"+qic+")\n";
	}

//	public String getGraphAlterTableDdlString(Connection conn, String tabName, TrendGraphDataPoint tgdp)
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

	public List<String> getGraphAlterTableDdlString(Connection conn, String tabName, TrendGraphDataPoint tgdp)
	throws SQLException
	{
		// Obtain a DatabaseMetaData object from our current connection
		DatabaseMetaData dbmd = conn.getMetaData();

		int colCounter = 0;
		ResultSet rs = dbmd.getColumns(null, null, tabName, "%");
		while(rs.next())
		{
			colCounter++;
		}
		rs.close();

		List<String> list = new ArrayList<String>();
		if (colCounter > 0)
		{
			colCounter -= 3; // take away: SessionStartTime, SessionSampleTime, SampleTime
			colCounter = colCounter / 2;
			
			Double[] dataArr  = tgdp.getData();
			if (colCounter < dataArr.length)
			{
				for (int d=colCounter; d<dataArr.length; d++)
				{
					list.add("alter table " + qic+tabName+qic + " add  "+fill(qic+"label_"+d+qic,40)+" "+fill(getDatatype("varchar",30,-1,-1),20)+" "+getNullable(true)+" \n");
					list.add("alter table " + qic+tabName+qic + " add  "+fill(qic+"data_" +d+qic,40)+" "+fill(getDatatype("numeric",-1,10, 1),20)+" "+getNullable(true)+" \n");
				}
			}
		}
		return list;
	}

	/**
	 * Get start time.
	 * 
	 * @param recordingStartTime
	 * @throws Exception
	 */
	public static Date getRecordingStartTime(String startTime)
	throws Exception
	{
		if (startTime == null)
			return null;

		String hourStr   = startTime;
		String minuteStr = "0";

		if (startTime.indexOf(":") >= 0)
		{
			String[] sa = startTime.split(":");
			if (sa.length != 2)
				throw new Exception("Must look like 'hours:minutes', example '5:15' for 5 hours and 15 minutes");

			hourStr   = sa[0];
			minuteStr = sa[1];
		}

		try 
		{
			int hour   = Integer.parseInt(hourStr);
			int minute = Integer.parseInt(minuteStr);
			if (hour   > 23  ) throw new NumberFormatException("Record time (Hour) is to high. hour='"+hour+"' must be between 0 and 23.");
			if (hour   < 0   ) throw new NumberFormatException("Record time (Hour) is to low.  hour='"+hour+"' must be between 0 and 23.");
			if (minute > 59  ) throw new NumberFormatException("Record time (Minute) is to high. hour='"+minute+"' must be between 0 and 59.");
			if (minute < 0   ) throw new NumberFormatException("Record time (Minute) is to low.  hour='"+minute+"' must be between 0 and 59.");

			Date now = new Date();
			Calendar cal = new GregorianCalendar();
			cal.setTime(now);
			cal.set(Calendar.HOUR_OF_DAY, hour);
			cal.set(Calendar.MINUTE,      minute);
			cal.set(Calendar.SECOND,      0);
			cal.set(Calendar.MILLISECOND, 0);
			
			// If the new date has already happened, then just add 1 day
			if (cal.getTime().getTime() < System.currentTimeMillis())
				cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR)+1);
				
			return cal.getTime();
		}
		catch (NumberFormatException e)
		{
			throw new Exception("Hour and Minute Must be in numbers. Caught: "+e.getMessage(), e);
		}
	}
	
	/**
	 * Get stop time.
	 * 
	 * @param recordingStartTime
	 * @throws Exception
	 */
	public static Date getRecordingStopTime(Date startTime, String stopTime)
	throws Exception
	{
		if (stopTime == null)
			return null;

		String hourStr   = stopTime;
		String minuteStr = "0";

		if (stopTime.indexOf(":") >= 0)
		{
			String[] sa = stopTime.split(":");
			if (sa.length != 2)
				throw new Exception("Must look like 'hours:minutes', example '5:15' for 5 hours and 15 minutes");

			hourStr   = sa[0];
			minuteStr = sa[1];
		}

		try 
		{
			int hour   = Integer.parseInt(hourStr);
			int minute = Integer.parseInt(minuteStr);
			if (hour   > 999 ) throw new NumberFormatException("Record time (Hour) is to high. hour='"+hour+"' must be between 0 and 999.");
			if (hour   < 0   ) throw new NumberFormatException("Record time (Hour) is to low.  hour='"+hour+"' must be between 0 and 999.");
			if (minute > 59  ) throw new NumberFormatException("Record time (Minute) is to high. hour='"+minute+"' must be between 00 and 59.");
			if (minute < 0   ) throw new NumberFormatException("Record time (Minute) is to low.  hour='"+minute+"' must be between 00 and 59.");

			Date now = new Date();
			if (startTime != null)
				now = startTime;

			Calendar cal = new GregorianCalendar();
			cal.setTime(now);
			cal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY) + hour);
			cal.set(Calendar.MINUTE,      cal.get(Calendar.MINUTE)      + minute);
			cal.set(Calendar.SECOND,      0);
			cal.set(Calendar.MILLISECOND, 0);
			
			return cal.getTime();
		}
		catch (NumberFormatException e)
		{
			throw new Exception("Hour and Minute Must be in numbers. Caught: "+e.getMessage(), e);
		}
	}
	
	/**
	 * Wait until, the desired start time. Just do sleep()...
	 * 
	 * @param startTime startTime in the form Hour:Minute
	 * @param waitDialog if GUI mode, pass a wait dialog (so we dont block the Swing Dispatch Thread)
	 * @throws InterruptedException
	 */
	public static void waitForRecordingStartTime(String startTime, WaitForExecDialog waitDialog)
	throws InterruptedException
	{
		if (startTime == null)
			return;

		Date   waitUntilDate = null;
		String waitUntilStr  = null;
		try 
		{
			waitUntilDate = getRecordingStartTime(startTime);
			waitUntilStr  = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(waitUntilDate);
			_logger.info("Found the deferred start time '"+waitUntilStr+"'. I will have to wait...");
		}
		catch (Exception e) 
		{
			_logger.warn("Problems getting deferred start time. So I will skip waiting and continue the start. Caught: "+e.getMessage());
			return;
		}

		int sleepTimeInSec; // determen later, based on GUI or NO-GUI
		while(true)
		{
			long now = System.currentTimeMillis();

			if (waitUntilDate.getTime() < now)
			{
				_logger.info("DONE waiting for the deferred start time '"+waitUntilStr+"'. Lets continuing the start sequence.");
				break;
			}

			String waitTimeLeft = TimeUtils.msToTimeStr("%HH:%MM:%SS", waitUntilDate.getTime() - now);
			if (waitDialog != null)
			{
				sleepTimeInSec = 1;
				waitDialog.setState("Time left to Connect (Hours:Minutes:Seconds) "+waitTimeLeft);
			}
			else
			{
				sleepTimeInSec = 60;
				_logger.info("Waiting for the start time '"+waitUntilStr+"' before continuing the start sequence ("+waitTimeLeft+").");
			}
			Thread.sleep(1000 * sleepTimeInSec); // may be interrupted
		}
	}
}

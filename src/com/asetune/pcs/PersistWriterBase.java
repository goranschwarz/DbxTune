/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.pcs;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.cm.CountersModel;
import com.asetune.config.dbms.DbmsConfigManager;
import com.asetune.config.dbms.IDbmsConfig;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.sql.conn.DbxConnection;
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
	public static final int SESSION_DBMS_CONFIG      = 8;
	public static final int SESSION_DBMS_CONFIG_TEXT = 9;
	public static final int DDL_STORAGE              = 50;
	public static final int SQL_CAPTURE_SQLTEXT      = 60;
	public static final int SQL_CAPTURE_STATEMENTS   = 61;
	public static final int SQL_CAPTURE_PLANS        = 62;
	public static final int ALARM_ACTIVE             = 90;
	public static final int ALARM_HISTORY            = 91;
	public static final int ABS                      = 100;
	public static final int DIFF                     = 101;
	public static final int RATE                     = 102;
	
	public static final int SESSION_PARAMS_VAL_MAXLEN = 4096;

	/** Character used for quoted identifier */
//	public static String  _qic = "\"";
	
	/**	what are we connected to: DatabaseMetaData.getDatabaseProductName() */
	private String _databaseProductName = "";
	
	/** List some known DatabaseProductName that we can use here */
//	public static String DB_PROD_NAME_ASE = "Adaptive Server Enterprise";
//	public static String DB_PROD_NAME_ASA = "SQL Anywhere";
//	public static String DB_PROD_NAME_H2  = "H2";


	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/
	/** when DDL is "created" we do not need to do this again */
	private List<String> _saveDdlIsCalled = new LinkedList<String>();

//	private int _inserts      = 0;
//	private int _updates      = 0;
//	private int _deletes      = 0;
//
//	private int _createTables = 0;
//	private int _alterTables  = 0;
//	private int _dropTables   = 0;
//	
//	private int _ddlSaveCount = 0;
//	private int _ddlSaveCountSum = 0;
//
//	private int _sqlCaptureEntryCount = 0;
//	private int _sqlCaptureBatchCount = 0;
	/** Statistics for the Writer */
	private PersistWriterStatistics _writerStatistics = new PersistWriterStatistics();

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

	
	
	/*---------------------------------------------------
	** Methods handling counters
	**---------------------------------------------------
	*/
	@Override public PersistWriterStatistics getStatistics() 
	{
		if (_writerStatistics == null)
			_writerStatistics = new PersistWriterStatistics();

		return _writerStatistics; 
	}

	@Override
	public void resetCounters()
	{ 
		if (_writerStatistics != null)
			_writerStatistics.clear();
	}
	
//	@Override public void incInserts() { _writerStatistics.incInserts(); }
//	@Override public void incUpdates() { _writerStatistics.incUpdates(); }
//	@Override public void incDeletes() { _writerStatistics.incDeletes(); }
//
//	@Override public void incInserts(int cnt) { _writerStatistics.incInserts(cnt); }
//	@Override public void incUpdates(int cnt) { _writerStatistics.incUpdates(cnt); }
//	@Override public void incDeletes(int cnt) { _writerStatistics.incDeletes(cnt); }
//
//	@Override public void incCreateTables() { _writerStatistics.incCreateTables(); }
//	@Override public void incAlterTables()  { _writerStatistics.incAlterTables();  }
//	@Override public void incDropTables()   { _writerStatistics.incDropTables();   }
//	@Override public void incDdlSaveCount() { _writerStatistics.incDdlSaveCount(); }
//	@Override public void incSqlCaptureEntryCount() { _writerStatistics.incSqlCaptureEntryCount(); }
//	@Override public void incSqlCaptureBatchCount() { _writerStatistics.incSqlCaptureBatchCount(); }
//
//	@Override public void incCreateTables(int cnt) { _writerStatistics.incCreateTables(cnt); }
//	@Override public void incAlterTables (int cnt) { _writerStatistics.incAlterTables (cnt); }
//	@Override public void incDropTables  (int cnt) { _writerStatistics.incDropTables  (cnt); }
//	@Override public void incDdlSaveCount(int cnt) { _writerStatistics.incDdlSaveCount(cnt); }
//	@Override public void incSqlCaptureEntryCount(int cnt) { _writerStatistics.incSqlCaptureEntryCount(cnt); }
//	@Override public void incSqlCaptureBatchCount(int cnt) { _writerStatistics.incSqlCaptureBatchCount(cnt); }
//
//	
//	@Override public int getInserts() { return _writerStatistics.getInserts(); }
//	@Override public int getUpdates() { return _writerStatistics.getUpdates(); }
//	@Override public int getDeletes() { return _writerStatistics.getDeletes(); }
//
//	@Override public int getCreateTables()    { return _writerStatistics.getCreateTables(); }
//	@Override public int getAlterTables()     { return _writerStatistics.getAlterTables();  }
//	@Override public int getDropTables()      { return _writerStatistics.getDropTables();   }
//	@Override public int getDdlSaveCount()    { return _writerStatistics.getDdlSaveCount(); }
//	@Override public int getDdlSaveCountSum() { return _writerStatistics.getDdlSaveCountSum(); }
//	@Override public int getSqlCaptureEntryCount() { return _writerStatistics.getSqlCaptureEntryCount(); }
//	@Override public int getSqlCaptureBatchCount() { return _writerStatistics.getSqlCaptureBatchCount(); }
	
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

	/** just sets session as started */
	@Override
	public void startSession(PersistContainer cont)
	{
		setSessionStarted(true);
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

	@Override
	public void storageQueueSizeWarning(int queueSize, int thresholdSize)
	{
	}


	
	
	
	
	
	/*---------------------------------------------------
	** HELPER Methods that subclasses can use
	**---------------------------------------------------
	*/
//	public static  void setQuotedIdentifierChar(String qic)
//	{
//		_qic = qic;
//	}
//	public static String getQuotedIdentifierChar()
//	{
//		return _qic;
//	}

	public void setDatabaseProductName(String databaseProductName)
	{
		_databaseProductName = databaseProductName;
	}
	public String getDatabaseProductName()
	{
		return _databaseProductName;
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

//	public static String jdbcTypeToSqlType(int columnType)
//	{
//		switch (columnType)
//		{
//		case java.sql.Types.BIT:          return "bit";
//		case java.sql.Types.TINYINT:      return "tinyint";
//		case java.sql.Types.SMALLINT:     return "smallint";
//		case java.sql.Types.INTEGER:      return "int";
//		case java.sql.Types.BIGINT:       return "bigint";
//		case java.sql.Types.FLOAT:        return "float";
//		case java.sql.Types.REAL:         return "real";
//		case java.sql.Types.DOUBLE:       return "double";
//		case java.sql.Types.NUMERIC:      return "numeric";
//		case java.sql.Types.DECIMAL:      return "decimal";
//		case java.sql.Types.CHAR:         return "char";
//		case java.sql.Types.VARCHAR:      return "varchar";
//		case java.sql.Types.LONGVARCHAR:  return "text";
//		case java.sql.Types.DATE:         return "date";
//		case java.sql.Types.TIME:         return "time";
//		case java.sql.Types.TIMESTAMP:    return "datetime";
//		case java.sql.Types.BINARY:       return "binary";
//		case java.sql.Types.VARBINARY:    return "varbinary";
//		case java.sql.Types.LONGVARBINARY:return "image";
////		case java.sql.Types.NULL:         return "-null-";
////		case java.sql.Types.OTHER:        return "-other-";
////		case java.sql.Types.JAVA_OBJECT:  return "-java_object";
////		case java.sql.Types.DISTINCT:     return "-DISTINCT-";
////		case java.sql.Types.STRUCT:       return "-STRUCT-";
////		case java.sql.Types.ARRAY:        return "-ARRAY-";
//		case java.sql.Types.BLOB:         return "image";
//		case java.sql.Types.CLOB:         return "text";
////		case java.sql.Types.REF:          return "-REF-";
////		case java.sql.Types.DATALINK:     return "-DATALINK-";
//		case java.sql.Types.BOOLEAN:      return "bit";
//
//		//------------------------- JDBC 4.0 -----------------------------------
////		case java.sql.Types.ROWID:         return "java.sql.Types.ROWID";
////		case java.sql.Types.NCHAR:         return "java.sql.Types.NCHAR";
////		case java.sql.Types.NVARCHAR:      return "java.sql.Types.NVARCHAR";
////		case java.sql.Types.LONGNVARCHAR:  return "java.sql.Types.LONGNVARCHAR";
////		case java.sql.Types.NCLOB:         return "java.sql.Types.NCLOB";
////		case java.sql.Types.SQLXML:        return "java.sql.Types.SQLXML";
//
//		//------------------------- VENDOR SPECIFIC TYPES ---------------------------
////		case -10:                          return "oracle.jdbc.OracleTypes.CURSOR";
//
//		//------------------------- UNHANDLED TYPES  ---------------------------
//		default:
//			return "unknown-jdbc-datatype("+columnType+")";
//		}
//	}
//	/**
//	 * This method can be overladed and used to change the syntax for various datatypes 
//	 */
//	public static String getDatatype(String type, int length, int prec, int scale)
//	{
//		if ( type.equals("char") || type.equals("varchar") )
//			type = type + "(" + length + ")";
//		
//		if ( type.equals("numeric") || type.equals("decimal") )
//			type = type + "(" + prec + "," + scale + ")";
//
//		return type;
//	}
//	public static String getDatatype(int col, ResultSetMetaData rsmd, boolean isDeltaOrPct)
//	throws SQLException
//	{
//		String type   = null;
//		int    length = -1;
//		int    prec   = -1;
//		int    scale  = -1;
//
//		if (isDeltaOrPct)
//		{
//			type    = "numeric";
//			length  = -1;
//			prec    = 10;
//			scale   = 1;
//
//			// If the source datatype is 'bigint', then lets make the Delta/Pct a bit bigger
//			String srcType  = ResultSetTableModel.getColumnTypeName(rsmd, col);
//			if (srcType.toLowerCase().equals("bigint"))
//				prec = 18;
//
//			return getDatatype(type, length, prec, scale);
//		}
//		else
//		{
//			type  = ResultSetTableModel.getColumnTypeName(rsmd, col);
//
//			// Most databases doesn't have unsigned datatypes, so lets leave "unsigned int" as "int"
//			if ( type.toLowerCase().startsWith("unsigned ") )
//			{
//				String newType = type.substring("unsigned ".length());
//				_logger.info("Found the uncommon data type '"+type+"', instead the data type '"+newType+"' will be used.");
//				type = newType;
//			}
//			
//			return type;
//		}
//	}
	public static String getDatatype(DbxConnection conn, int jdbcType)
	{
		return conn.getDbmsDataTypeResolver().dataTypeResolverToTarget(jdbcType, -1, -1);
	}
	public static String getDatatype(DbxConnection conn, int jdbcType, int length)
	{
		return conn.getDbmsDataTypeResolver().dataTypeResolverToTarget(jdbcType, length, -1);
	}
	public static String getDatatype(DbxConnection conn, int jdbcType, int length, int scale)
	{
		return conn.getDbmsDataTypeResolver().dataTypeResolverToTarget(jdbcType, length, scale);
	}
	public static String getDatatype(DbxConnection conn, int col, ResultSetMetaData rsmd, boolean isDeltaOrPct)
	throws SQLException
	{
		if (isDeltaOrPct)
		{
			int originJdbcType = rsmd.getColumnType(col);

			// Default data type, lets asume INT: max: 2 147 483 647
			int type  = Types.NUMERIC;
			int prec  = 10;
			int scale = 1;

			// Override for some "bigger" source data types
			if (originJdbcType == Types.BIGINT) // max: 9 223 372 036 854 775 807
			{
				type  = Types.NUMERIC;
				prec  = 19;
				scale = 1;
			}

			return getDatatype(conn, type, prec, scale);
		}
		else
		{
			int type   = rsmd.getColumnType(col);
//			int length = rsmd.getPrecision(col);
			int length = Math.max(rsmd.getColumnDisplaySize(col), rsmd.getPrecision(col)); // Use this instead of the below switch for different data types
			int scale  = rsmd.getScale(col);

//			switch (type)
//			{
//			case Types.CHAR:
//			case Types.VARCHAR:
//			case Types.NCHAR:
//			case Types.NVARCHAR:
//			case Types.LONGVARCHAR:
//			case Types.LONGNVARCHAR:
//				length = Math.max(rsmd.getColumnDisplaySize(col), rsmd.getPrecision(col));
//				break;
//
//			case Types.BINARY:
//			case Types.VARBINARY:
//				length = Math.max(rsmd.getColumnDisplaySize(col), rsmd.getPrecision(col));
//				break;
//
//			}
			
			return getDatatype(conn, type, length, scale);
		}
	}

	public static String getNullable(boolean nullable)
	{
		return nullable ? "    null" : "not null";
		
	}
//	public static String getNullable(int col, ResultSetMetaData rsmd, boolean isDeltaOrPct)
//	throws SQLException
//	{
//		// datatype "bit" can't be NULL declared in ASE
////		String type  = rsmd.getColumnTypeName(col);
//		String type  = getDatatype(col, rsmd, isDeltaOrPct);
//		if (type != null && type.equalsIgnoreCase("bit"))
//			return getNullable(false);
//		else
//			return getNullable(true);
////		return getNullable(rsmd.isNullable(col)>0);
//	}
	public static String getNullable(DbxConnection conn, int col, ResultSetMetaData rsmd, boolean isDeltaOrPct)
	throws SQLException
	{
		int jdbcType = rsmd.getColumnType(col);

		// data-type "bit" can't be NULL declared in ASE, so lets do that for "every" DBMS Vendor
//		if (jdbcType == Types.BIT && conn.isDatabaseProduct(DbUtils.DB_PROD_NAME_SYBASE_ASE))
		if (jdbcType == Types.BIT)
			return getNullable(false);
		else
			return getNullable(true);
	}

	/** Helper method to get a table name */
	public static String getTableName(DbxConnection conn, int type, CountersModel cm, boolean addQuotedIdentifierChar)
	{
//		if (addQuotedIdentifierChar && conn == null)
//			throw new NullPointerException("When addQuotedIdentifierChar is true, Then a DbxConnection must be passed, otherwise we wont be able to decide DBMS Specific Identity Quote Chars");

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
		
		switch (type)
		{
		case VERSION_INFO:             return lq + "MonVersionInfo"              + rq;
		case SESSIONS:                 return lq + "MonSessions"                 + rq;
		case SESSION_PARAMS:           return lq + "MonSessionParams"            + rq;
		case SESSION_SAMPLES:          return lq + "MonSessionSamples"           + rq;
		case SESSION_SAMPLE_SUM:       return lq + "MonSessionSampleSum"         + rq;
		case SESSION_SAMPLE_DETAILES:  return lq + "MonSessionSampleDetailes"    + rq;
		case SESSION_MON_TAB_DICT:     return lq + "MonSessionMonTablesDict"     + rq;
		case SESSION_MON_TAB_COL_DICT: return lq + "MonSessionMonTabColumnsDict" + rq;
		case SESSION_DBMS_CONFIG:      return lq + "MonSessionDbmsConfig"        + rq; // old name MonSessionAseConfig,     so AseTune needs to backward compatible
		case SESSION_DBMS_CONFIG_TEXT: return lq + "MonSessionDbmsConfigText"    + rq; // old name MonSessionAseConfigText, so AseTune needs to backward compatible
		case DDL_STORAGE:              return lq + "MonDdlStorage"               + rq;
		case SQL_CAPTURE_SQLTEXT:      return lq + "MonSqlCapSqlText"            + rq;
		case SQL_CAPTURE_STATEMENTS:   return lq + "MonSqlCapStatements"         + rq;
		case SQL_CAPTURE_PLANS:        return lq + "MonSqlCapPlans"              + rq;
		case ALARM_ACTIVE:             return lq + "MonAlarmActive"              + rq;
		case ALARM_HISTORY:            return lq + "MonAlarmHistory"             + rq;
		case ABS:                      return lq + cm.getName() + "_abs"         + rq;
		case DIFF:                     return lq + cm.getName() + "_diff"        + rq;
		case RATE:                     return lq + cm.getName() + "_rate"        + rq;
		default:
			throw new RuntimeException("Unknown type of '"+type+"' in getTableName()."); 
		}
	}

	/** Helper method to get a table name for GRAPH tables */
	public static String getTableName(DbxConnection conn, CountersModel cm, TrendGraphDataPoint tgdp, boolean addQuotedIdentifierChar)
	{
//		if (addQuotedIdentifierChar && conn == null)
//		throw new NullPointerException("When addQuotedIdentifierChar is true, Then a DbxConnection must be passed, otherwise we wont be able to decide DBMS Specific Identity Quote Chars");

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

//		String q = "";
//		if (addQuotedIdentifierChar)
//			q = getQuotedIdentifierChar();

		String tabName = lq + cm.getName() + "_" + tgdp.getName() + rq;
		return tabName;
	}

	/** Helper method to generate a DDL string, to get the 'create table' */
	public String getTableDdlString(DbxConnection conn, int type, CountersModel cm)
	throws SQLException
	{
		StringBuffer sbSql = new StringBuffer();

		String tabName = getTableName(conn, type, cm, true);
		
		String lq = getLeftQuoteReplace();
		String rq = getLeftQuoteReplace();
		if (conn != null)
		{
			lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
			rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection
		}

		try
		{
			if (type == VERSION_INFO)
			{
//				sbSql.append("create table " + tabName + "\n");
//				sbSql.append("( \n");
//				sbSql.append("    "+fill(lq+"SessionStartTime"+rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"ProductString"   +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"VersionString"   +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"BuildString"     +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"SourceDate"      +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"SourceRev"       +rq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
////				sbSql.append("   ,"+fill(lq+"DbProductName"   +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("\n");
//				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionStartTime"+rq+")\n");
//				sbSql.append(") \n");
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"SessionStartTime"+rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP  ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"ProductString"   +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"VersionString"   +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"BuildString"     +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"SourceDate"      +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"SourceRev"       +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER    ),20)+" "+getNullable(false)+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionStartTime"+rq+")\n");
				sbSql.append(") \n");
			}
			else if (type == SESSIONS)
			{
//				sbSql.append("create table " + tabName + "\n");
//				sbSql.append("( \n");
//				sbSql.append("    "+fill(lq+"SessionStartTime"+rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"ServerName"      +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"NumOfSamples"    +rq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"LastSampleTime"  +rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("\n");
//				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionStartTime"+rq+")\n");
//				sbSql.append(") \n");
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"SessionStartTime"+rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP  ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"ServerName"      +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"NumOfSamples"    +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER    ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"LastSampleTime"  +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP  ),20)+" "+getNullable(true)+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionStartTime"+rq+")\n");
				sbSql.append(") \n");
			}
			else if (type == SESSION_PARAMS)
			{
//				int len = SESSION_PARAMS_VAL_MAXLEN;

//				sbSql.append("create table " + tabName + "\n");
//				sbSql.append("( \n");
//				sbSql.append("    "+fill(lq+"SessionStartTime"+rq,40)+" "+fill(getDatatype("datetime",-1,  -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"Type"            +rq,40)+" "+fill(getDatatype("varchar", 20,  -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"ParamName"       +rq,40)+" "+fill(getDatatype("varchar", 255, -1,-1),20)+" "+getNullable(false)+"\n");
////				sbSql.append("   ,"+fill(lq+"ParamValue"      +rq,40)+" "+fill(getDatatype("varchar", len, -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"ParamValue"      +rq,40)+" "+fill(getDatatype("text",    -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("\n");
//				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionStartTime"+rq+", "+lq+"Type"+rq+", "+lq+"ParamName"+rq+")\n");
//				sbSql.append(") \n");
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"SessionStartTime"+rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP   ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"Type"            +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 20 ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"ParamName"       +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 255),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"ParamValue"      +rq,40)+" "+fill(getDatatype(conn, Types.CLOB        ),20)+" "+getNullable(true)+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionStartTime"+rq+", "+lq+"Type"+rq+", "+lq+"ParamName"+rq+")\n");
				sbSql.append(") \n");
			}
			else if (type == SESSION_SAMPLES)
			{
//				sbSql.append("create table " + tabName + "\n");
//				sbSql.append("( \n");
//				sbSql.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"SessionSampleTime"+rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("\n");
////				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionStartTime"+rq+", "+lq+"SessionSampleTime"+rq+")\n");
//				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionSampleTime"+rq+", "+lq+"SessionStartTime"+rq+")\n");
//				sbSql.append(") \n");
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"SessionSampleTime"+rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP),20)+" "+getNullable(false)+"\n");
				sbSql.append("\n");
//				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionStartTime"+rq+", "+lq+"SessionSampleTime"+rq+")\n");
				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionSampleTime"+rq+", "+lq+"SessionStartTime"+rq+")\n");
				sbSql.append(") \n");
			}
			else if (type == SESSION_SAMPLE_SUM)
			{
//				sbSql.append("create table " + tabName + "\n");
//				sbSql.append("( \n");
//				sbSql.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"CmName"           +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"absSamples"       +rq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"diffSamples"      +rq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"rateSamples"      +rq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("\n");
//				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionStartTime"+rq+", "+lq+"CmName"+rq+")\n");
//				sbSql.append(") \n");
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP  ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"CmName"           +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"absSamples"       +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER    ),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"diffSamples"      +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER    ),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"rateSamples"      +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER    ),20)+" "+getNullable(true)+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+lq+"SessionStartTime"+rq+", "+lq+"CmName"+rq+")\n");
				sbSql.append(") \n");
			}
			else if (type == SESSION_SAMPLE_DETAILES)
			{
//				sbSql.append("create table " + tabName + "\n");
//				sbSql.append("( \n");
//				sbSql.append("    "+fill(lq+"SessionStartTime"      +rq,40)+" "+fill(getDatatype("datetime",-1,  -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"SessionSampleTime"     +rq,40)+" "+fill(getDatatype("datetime",-1,  -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"CmName"                +rq,40)+" "+fill(getDatatype("varchar", 30,  -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"type"                  +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"graphCount"            +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"absRows"               +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"diffRows"              +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"rateRows"              +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"sqlRefreshTime"        +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"guiRefreshTime"        +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"lcRefreshTime"         +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"nonCfgMonHappened"     +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"nonCfgMonMissingParams"+rq,40)+" "+fill(getDatatype("varchar", 100, -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"nonCfgMonMessages"     +rq,40)+" "+fill(getDatatype("varchar", 1024,-1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"isCountersCleared"     +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"hasValidSampleData"    +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"exceptionMsg"          +rq,40)+" "+fill(getDatatype("varchar", 1024,-1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"exceptionFullText"     +rq,40)+" "+fill(getDatatype("text",    -1,  -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append(") \n");
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"SessionStartTime"      +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"SessionSampleTime"     +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"CmName"                +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30  ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"type"                  +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"graphCount"            +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"absRows"               +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"diffRows"              +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"rateRows"              +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"sqlRefreshTime"        +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"guiRefreshTime"        +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"lcRefreshTime"         +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"nonCfgMonHappened"     +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"nonCfgMonMissingParams"+rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 100 ),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"nonCfgMonMessages"     +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 1024),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"isCountersCleared"     +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"hasValidSampleData"    +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"exceptionMsg"          +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 1024),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"exceptionFullText"     +rq,40)+" "+fill(getDatatype(conn, Types.CLOB         ),20)+" "+getNullable(true)+"\n");
				sbSql.append(") \n");
			}
			else if (type == SESSION_MON_TAB_DICT)
			{
//				sbSql.append("create table " + tabName + "\n");
//				sbSql.append("( \n");
//				sbSql.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype("datetime",-1,  -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"TableID"          +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"Columns"          +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"Parameters"       +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"Indicators"       +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"Size"             +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"TableName"        +rq,40)+" "+fill(getDatatype("varchar", 255, -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"Description"      +rq,40)+" "+fill(getDatatype("varchar", 4000,-1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append(") \n");
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"TableID"          +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"Columns"          +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"Parameters"       +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"Indicators"       +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"Size"             +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"TableName"        +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 255 ),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"Description"      +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 4000),20)+" "+getNullable(true)+"\n");
				sbSql.append(") \n");
			}
			else if (type == SESSION_MON_TAB_COL_DICT)
			{
//				sbSql.append("create table " + tabName + "\n");
//				sbSql.append("( \n");
//				sbSql.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype("datetime",-1,  -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"TableID"          +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"ColumnID"         +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"TypeID"           +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"Precision"        +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"Scale"            +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"Length"           +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"Indicators"       +rq,40)+" "+fill(getDatatype("int",     -1,  -1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"TableName"        +rq,40)+" "+fill(getDatatype("varchar", 255, -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"ColumnName"       +rq,40)+" "+fill(getDatatype("varchar", 255, -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"TypeName"         +rq,40)+" "+fill(getDatatype("varchar", 255, -1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append("   ,"+fill(lq+"Description"      +rq,40)+" "+fill(getDatatype("varchar", 4000,-1,-1),20)+" "+getNullable(true)+"\n");
//				sbSql.append(") \n");
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"TableID"          +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"ColumnID"         +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"TypeID"           +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"Precision"        +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"Scale"            +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"Length"           +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"Indicators"       +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"TableName"        +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 255 ),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"ColumnName"       +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 255 ),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"TypeName"         +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 255 ),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(lq+"Description"      +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 4000),20)+" "+getNullable(true)+"\n");
				sbSql.append(") \n");
			}
			else if (type == SESSION_DBMS_CONFIG)
			{
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
//				sbSql.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP),20)+" "+getNullable(false)+"\n");

//				// Get ALL other column names from the AseConfig dictionary
//				IDbmsConfig aseCfg = AseConfig.getInstance();
//				for (int i=0; i<aseCfg.getColumnCount(); i++)
//				{
//					sbSql.append("   ,"+fill(lq+aseCfg.getColumnName(i)+rq,40)+" "+fill(aseCfg.getSqlDataType(i),20)+" "+getNullable( aseCfg.getSqlDataType(i).equalsIgnoreCase("bit") ? false : true)+"\n");
//				}
				// Get ALL other column names from the DBMS Config dictionary
				if (DbmsConfigManager.hasInstance())
				{
    				IDbmsConfig dbmsCfg = DbmsConfigManager.getInstance();
    				for (int i=0; i<dbmsCfg.getColumnCount(); i++)
    				{
//    					sbSql.append("   ,"+fill(lq+dbmsCfg.getColumnName(i)+rq,40)+" "+fill(dbmsCfg.getSqlDataType(i),20)+" "+getNullable( dbmsCfg.getSqlDataType(i).equalsIgnoreCase("bit") ? false : true)+"\n");
    					// Maybe this should also be fixed: meaning: dbmsCfg.getSqlDataType(i) should be java.sql.Types instead of a String
    					sbSql.append("   ,"+fill(lq+dbmsCfg.getColumnName(i)+rq,40)+" "+fill(dbmsCfg.getSqlDataType(conn, i),20)+" "+getNullable( dbmsCfg.getSqlDataType(conn, i).equalsIgnoreCase("bit") ? false : true)+"\n");
    				}
				}
				sbSql.append(") \n");
			}
			else if (type == SESSION_DBMS_CONFIG_TEXT)
			{
//				sbSql.append("create table " + tabName + "\n");
//				sbSql.append("( \n");
//				sbSql.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"configName"       +rq,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"configText"       +rq,40)+" "+fill(getDatatype("text",    -1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append(") \n");
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP  ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"configName"       +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 30),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"configText"       +rq,40)+" "+fill(getDatatype(conn, Types.CLOB       ),20)+" "+getNullable(false)+"\n");
				sbSql.append(") \n");
			}
			else if (type == DDL_STORAGE)
			{
//				sbSql.append("create table " + tabName + "\n");
//				sbSql.append("( \n");
//				sbSql.append("    "+fill(lq+"dbname"           +rq,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"owner"            +rq,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"objectName"       +rq,40)+" "+fill(getDatatype("varchar",  255,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"type"             +rq,40)+" "+fill(getDatatype("varchar",   20,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"crdate"           +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"sampleTime"       +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"source"           +rq,40)+" "+fill(getDatatype("varchar",  255,-1,-1),20)+" "+getNullable(true) +"\n");
//				sbSql.append("   ,"+fill(lq+"dependParent"     +rq,40)+" "+fill(getDatatype("varchar",  255,-1,-1),20)+" "+getNullable(true) +"\n");
//				sbSql.append("   ,"+fill(lq+"dependLevel"      +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"dependList"       +rq,40)+" "+fill(getDatatype("varchar", 1500,-1,-1),20)+" "+getNullable(true) +"\n");
//				sbSql.append("   ,"+fill(lq+"objectText"       +rq,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true) +"\n");
//				sbSql.append("   ,"+fill(lq+"dependsText"      +rq,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true) +"\n");
//				sbSql.append("   ,"+fill(lq+"optdiagText"      +rq,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true) +"\n");
//				sbSql.append("   ,"+fill(lq+"extraInfoText"    +rq,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true) +"\n");
//				sbSql.append("\n");
//				sbSql.append("   ,PRIMARY KEY ("+lq+"dbname"+rq+", "+lq+"owner"+rq+", "+lq+"objectName"+rq+")\n");
//				sbSql.append(") \n");
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"dbname"           +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   30),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"owner"            +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   30),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"objectName"       +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,  255),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"type"             +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   20),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"crdate"           +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"sampleTime"       +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"source"           +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,  255),20)+" "+getNullable(true) +"\n");
				sbSql.append("   ,"+fill(lq+"dependParent"     +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,  255),20)+" "+getNullable(true) +"\n");
				sbSql.append("   ,"+fill(lq+"dependLevel"      +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"dependList"       +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR, 1500),20)+" "+getNullable(true) +"\n");
				sbSql.append("   ,"+fill(lq+"objectText"       +rq,40)+" "+fill(getDatatype(conn, Types.CLOB         ),20)+" "+getNullable(true) +"\n");
				sbSql.append("   ,"+fill(lq+"dependsText"      +rq,40)+" "+fill(getDatatype(conn, Types.CLOB         ),20)+" "+getNullable(true) +"\n");
				sbSql.append("   ,"+fill(lq+"optdiagText"      +rq,40)+" "+fill(getDatatype(conn, Types.CLOB         ),20)+" "+getNullable(true) +"\n");
				sbSql.append("   ,"+fill(lq+"extraInfoText"    +rq,40)+" "+fill(getDatatype(conn, Types.CLOB         ),20)+" "+getNullable(true) +"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+lq+"dbname"+rq+", "+lq+"owner"+rq+", "+lq+"objectName"+rq+")\n");
				sbSql.append(") \n");
			}
			else if (type == ALARM_ACTIVE)
			{
//				sbSql.append("create table " + tabName + "\n");
//				sbSql.append("( \n");
//				sbSql.append("    "+fill(lq+"alarmClass"                 +rq,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"serviceType"                +rq,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"serviceName"                +rq,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"serviceInfo"                +rq,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"extraInfo"                  +rq,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(true )+"\n");
//				sbSql.append("   ,"+fill(lq+"category"                   +rq,40)+" "+fill(getDatatype("varchar",   20,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"severity"                   +rq,40)+" "+fill(getDatatype("varchar",   10,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"state"                      +rq,40)+" "+fill(getDatatype("varchar",   10,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"repeatCnt"                  +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"duration"                   +rq,40)+" "+fill(getDatatype("varchar",   10,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"createTime"                 +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"cancelTime"                 +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(true )+"\n");
//				sbSql.append("   ,"+fill(lq+"timeToLive"                 +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(true )+"\n");
//				sbSql.append("   ,"+fill(lq+"threshold"                  +rq,40)+" "+fill(getDatatype("varchar",   15,-1,-1),20)+" "+getNullable(true )+"\n");
//				sbSql.append("   ,"+fill(lq+"data"                       +rq,40)+" "+fill(getDatatype("varchar",  160,-1,-1),20)+" "+getNullable(true )+"\n");
//				sbSql.append("   ,"+fill(lq+"lastData"                   +rq,40)+" "+fill(getDatatype("varchar",  160,-1,-1),20)+" "+getNullable(true )+"\n");
//				sbSql.append("   ,"+fill(lq+"description"                +rq,40)+" "+fill(getDatatype("varchar",  512,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"lastDescription"            +rq,40)+" "+fill(getDatatype("varchar",  512,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"extendedDescription"        +rq,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true )+"\n");
//				sbSql.append("   ,"+fill(lq+"lastExtendedDescription"    +rq,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true )+"\n");
//				sbSql.append("\n");
//				sbSql.append("   ,PRIMARY KEY ("+lq+"alarmClass"+rq+", "+lq+"serviceType"+rq+", "+lq+"serviceName"+rq+", "+lq+"serviceInfo"+rq+", "+lq+"extraInfo"+rq+")\n");
//				sbSql.append(") \n");
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"alarmClass"                 +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   80),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"serviceType"                +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   80),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"serviceName"                +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   30),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"serviceInfo"                +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   80),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"extraInfo"                  +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   80),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"category"                   +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   20),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"severity"                   +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   10),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"state"                      +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   10),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"repeatCnt"                  +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"duration"                   +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   10),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"createTime"                 +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"cancelTime"                 +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP    ),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"timeToLive"                 +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"threshold"                  +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   15),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"data"                       +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,  512),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"lastData"                   +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,  512),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"description"                +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,  512),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"lastDescription"            +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,  512),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"extendedDescription"        +rq,40)+" "+fill(getDatatype(conn, Types.CLOB         ),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"lastExtendedDescription"    +rq,40)+" "+fill(getDatatype(conn, Types.CLOB         ),20)+" "+getNullable(true )+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+lq+"alarmClass"+rq+", "+lq+"serviceType"+rq+", "+lq+"serviceName"+rq+", "+lq+"serviceInfo"+rq+", "+lq+"extraInfo"+rq+")\n");
				sbSql.append(") \n");
			}
			else if (type == ALARM_HISTORY)
			{
//				sbSql.append("create table " + tabName + "\n");
//				sbSql.append("( \n");
//				sbSql.append("    "+fill(lq+"SessionStartTime"           +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"SessionSampleTime"          +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"eventTime"                  +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"action"                     +rq,40)+" "+fill(getDatatype("varchar",   15,-1,-1),20)+" "+getNullable(false)+"\n");
////				sbSql.append("   ,"+fill(lq+"isActive"                   +rq,40)+" "+fill(getDatatype("bit",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"alarmClass"                 +rq,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"serviceType"                +rq,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"serviceName"                +rq,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"serviceInfo"                +rq,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"extraInfo"                  +rq,40)+" "+fill(getDatatype("varchar",   80,-1,-1),20)+" "+getNullable(true )+"\n");
//				sbSql.append("   ,"+fill(lq+"category"                   +rq,40)+" "+fill(getDatatype("varchar",   20,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"severity"                   +rq,40)+" "+fill(getDatatype("varchar",   10,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"state"                      +rq,40)+" "+fill(getDatatype("varchar",   10,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"repeatCnt"                  +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"duration"                   +rq,40)+" "+fill(getDatatype("varchar",   10,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"createTime"                 +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"cancelTime"                 +rq,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(true )+"\n");
//				sbSql.append("   ,"+fill(lq+"timeToLive"                 +rq,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(true )+"\n");
//				sbSql.append("   ,"+fill(lq+"threshold"                  +rq,40)+" "+fill(getDatatype("varchar",   15,-1,-1),20)+" "+getNullable(true )+"\n");
//				sbSql.append("   ,"+fill(lq+"data"                       +rq,40)+" "+fill(getDatatype("varchar",  160,-1,-1),20)+" "+getNullable(true )+"\n");
//				sbSql.append("   ,"+fill(lq+"lastData"                   +rq,40)+" "+fill(getDatatype("varchar",  160,-1,-1),20)+" "+getNullable(true )+"\n");
//				sbSql.append("   ,"+fill(lq+"description"                +rq,40)+" "+fill(getDatatype("varchar",  512,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"lastDescription"            +rq,40)+" "+fill(getDatatype("varchar",  512,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"extendedDescription"        +rq,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true )+"\n");
//				sbSql.append("   ,"+fill(lq+"lastExtendedDescription"    +rq,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true )+"\n");
//				sbSql.append("\n");
//				sbSql.append("   ,PRIMARY KEY ("+lq+"eventTime"+rq+", "+lq+"action"+rq+", "+lq+"alarmClass"+rq+", "+lq+"serviceType"+rq+", "+lq+"serviceName"+rq+", "+lq+"serviceInfo"+rq+", "+lq+"extraInfo"+rq+")\n");
//				sbSql.append(") \n");
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"SessionStartTime"           +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"SessionSampleTime"          +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"eventTime"                  +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"action"                     +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   15),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"alarmClass"                 +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   80),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"serviceType"                +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   80),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"serviceName"                +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   30),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"serviceInfo"                +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   80),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"extraInfo"                  +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   80),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"category"                   +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   20),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"severity"                   +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   10),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"state"                      +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   10),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"repeatCnt"                  +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"duration"                   +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   10),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"createTime"                 +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP    ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"cancelTime"                 +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP    ),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"timeToLive"                 +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER      ),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"threshold"                  +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,   15),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"data"                       +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,  512),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"lastData"                   +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,  512),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"description"                +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,  512),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"lastDescription"            +rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,  512),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"extendedDescription"        +rq,40)+" "+fill(getDatatype(conn, Types.CLOB         ),20)+" "+getNullable(true )+"\n");
				sbSql.append("   ,"+fill(lq+"lastExtendedDescription"    +rq,40)+" "+fill(getDatatype(conn, Types.CLOB         ),20)+" "+getNullable(true )+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+lq+"eventTime"+rq+", "+lq+"action"+rq+", "+lq+"alarmClass"+rq+", "+lq+"serviceType"+rq+", "+lq+"serviceName"+rq+", "+lq+"serviceInfo"+rq+", "+lq+"extraInfo"+rq+")\n");
				sbSql.append(") \n");
			}
			else if (type == ABS || type == DIFF || type == RATE)
			{
//				sbSql.append("create table " + tabName + "\n");
//				sbSql.append("( \n");
//				sbSql.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"SessionSampleTime"+rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"CmSampleTime"     +rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"CmSampleMs"       +rq,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(lq+"CmNewDiffRateRow" +rq,40)+" "+fill(getDatatype("tinyint", -1,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("\n");
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"SessionSampleTime"+rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"CmSampleTime"     +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"CmSampleMs"       +rq,40)+" "+fill(getDatatype(conn, Types.INTEGER  ),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(lq+"CmNewDiffRateRow" +rq,40)+" "+fill(getDatatype(conn, Types.TINYINT  ),20)+" "+getNullable(false)+"\n");
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


//					String colName = fill( lq + rsmd.getColumnLabel(c) + rq,    40);
//					String dtName  = fill(getDatatype(c, rsmd, isDeltaOrPct),   20);
//					String nullable= getNullable(c, rsmd, isDeltaOrPct);
					String colName = fill( lq + rsmd.getColumnLabel(c) + rq,        40);
					String dtName  = fill(getDatatype(conn, c, rsmd, isDeltaOrPct), 20);
					String nullable= getNullable(conn, c, rsmd, isDeltaOrPct);

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
	public List<String> getAlterTableDdlString(DbxConnection conn, String tabName, List<String> missingCols, int type, CountersModel cm)
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

//			String colName = fill(lq + rsmd.getColumnLabel(c) + rq,     40);
//			String dtName  = fill(getDatatype(c, rsmd, isDeltaOrPct),   20);
//			String nullable= getNullable(c, rsmd, isDeltaOrPct);
			String colName = fill(lq + rsmd.getColumnLabel(c) + rq,         40);
			String dtName  = fill(getDatatype(conn, c, rsmd, isDeltaOrPct), 20);
//			String nullable= getNullable(conn, c, rsmd, isDeltaOrPct);
			String nullable= getNullable(true); // on alter, it should always be "nullable"

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
	public String getTableInsertStr(DbxConnection conn, int type, CountersModel cm, boolean addPrepStatementQuestionMarks)
	throws SQLException
	{
		return getTableInsertStr(conn, type, cm, addPrepStatementQuestionMarks, null);
	}
	/**
	 * Helper method to generate a: "insert into TABNAME(c1,c2,c3) [values(?,?...)]"
	 * @param type ABS | DIFF | RATE | SYSTEM_TYPE
	 * @param cm Counter Model info (can be null if SYSTEM type is used)
	 * @param tgdp Trends Graph information
	 * @param addPrepStatementQuestionMarks if true add "values(?,?,?...)" which can be used by a prepared statement
	 * @return
	 */
	public String getTableInsertStr(DbxConnection conn, int type, CountersModel cm, boolean addPrepStatementQuestionMarks, List<String> cmColumns)
	throws SQLException
	{
		String tabName = getTableName(conn, type, cm, true);
		StringBuffer sbSql = new StringBuffer();

		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

		if (type == VERSION_INFO)
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(lq).append("SessionStartTime").append(rq).append(", ");
			sbSql.append(lq).append("ProductString")   .append(rq).append(", ");
			sbSql.append(lq).append("VersionString")   .append(rq).append(", ");
			sbSql.append(lq).append("BuildString")     .append(rq).append(", ");
			sbSql.append(lq).append("SourceDate")      .append(rq).append(", ");
			sbSql.append(lq).append("SourceRev")       .append(rq).append("  ");
//			sbSql.append(lq).append("DbProductName")   .append(rq).append("  ");
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?) \n");
		}
		else if (type == SESSIONS)
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(lq).append("SessionStartTime").append(rq).append(", ");
			sbSql.append(lq).append("ServerName")      .append(rq).append(", ");
			sbSql.append(lq).append("NumOfSamples")    .append(rq).append(", ");
			sbSql.append(lq).append("LastSampleTime")  .append(rq).append("");
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?) \n");
		}
		else if (type == SESSION_PARAMS)
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(lq).append("SessionStartTime").append(rq).append(", ");
			sbSql.append(lq).append("Type")            .append(rq).append(", ");
			sbSql.append(lq).append("ParamName")       .append(rq).append(", ");
			sbSql.append(lq).append("ParamValue")      .append(rq).append("");
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?) \n");
		}
		else if (type == SESSION_SAMPLES)
		{
			sbSql.append("insert into ").append(tabName) .append(" (");
			sbSql.append(lq).append("SessionStartTime") .append(rq).append(", ");
			sbSql.append(lq).append("SessionSampleTime").append(rq).append("");
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?) \n");
		}
		else if (type == SESSION_SAMPLE_SUM)
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(lq).append("SessionStartTime").append(rq).append(", ");
			sbSql.append(lq).append("CmName")          .append(rq).append(", ");
			sbSql.append(lq).append("absSamples")      .append(rq).append(", ");
			sbSql.append(lq).append("diffSamples")     .append(rq).append(", ");
			sbSql.append(lq).append("rateSamples")     .append(rq).append("");
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?) \n");
		}
		else if (type == SESSION_SAMPLE_DETAILES)
		{
			sbSql.append("insert into ").append(tabName)      .append(" (");
			sbSql.append(lq).append("SessionStartTime")      .append(rq).append(", ");
			sbSql.append(lq).append("SessionSampleTime")     .append(rq).append(", ");
			sbSql.append(lq).append("CmName")                .append(rq).append(", ");
			sbSql.append(lq).append("type")                  .append(rq).append(", ");
			sbSql.append(lq).append("graphCount")            .append(rq).append(", ");
			sbSql.append(lq).append("absRows")               .append(rq).append(", ");
			sbSql.append(lq).append("diffRows")              .append(rq).append(", ");
			sbSql.append(lq).append("rateRows")              .append(rq).append(", ");
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
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n");
		}
		else if (type == SESSION_MON_TAB_DICT)
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(lq).append("SessionStartTime").append(rq).append(", ");
			sbSql.append(lq).append("TableID")         .append(rq).append(", ");
			sbSql.append(lq).append("Columns")         .append(rq).append(", ");
			sbSql.append(lq).append("Parameters")      .append(rq).append(", ");
			sbSql.append(lq).append("Indicators")      .append(rq).append(", ");
			sbSql.append(lq).append("Size")            .append(rq).append(", ");
			sbSql.append(lq).append("TableName")       .append(rq).append(", ");
			sbSql.append(lq).append("Description")     .append(rq).append("");
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?) \n");
		}
		else if (type == SESSION_MON_TAB_COL_DICT)
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(lq).append("SessionStartTime").append(rq).append(", ");
			sbSql.append(lq).append("TableID")         .append(rq).append(", ");
			sbSql.append(lq).append("ColumnID")        .append(rq).append(", ");
			sbSql.append(lq).append("TypeID")          .append(rq).append(", ");
			sbSql.append(lq).append("Precision")       .append(rq).append(", ");
			sbSql.append(lq).append("Scale")           .append(rq).append(", ");
			sbSql.append(lq).append("Length")          .append(rq).append(", ");
			sbSql.append(lq).append("Indicators")      .append(rq).append(", ");
			sbSql.append(lq).append("TableName")       .append(rq).append(", ");
			sbSql.append(lq).append("ColumnName")      .append(rq).append(", ");
			sbSql.append(lq).append("TypeName")        .append(rq).append(", ");
			sbSql.append(lq).append("Description")     .append(rq).append("");
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n");
		}
		else if (type == SESSION_DBMS_CONFIG)
		{
//			sbSql.append("insert into ").append(tabName).append("(");
//			sbSql.append(qic).append("SessionStartTime").append(qic).append(", ");
//
//			// Get ALL other column names from the AseConfig dictionary
//			IDbmsConfig aseCfg = AseConfig.getInstance();				
//			for (int i=0; i<aseCfg.getColumnCount(); i++)
//				sbSql.append(qic).append(aseCfg.getColumnName(i)).append(qic).append(", ");
//
//			// remove last ", "
//			sbSql.delete(sbSql.length()-2, sbSql.length());
			sbSql.append("insert into ").append(tabName).append("(");
			sbSql.append(lq).append("SessionStartTime").append(rq);

			// Get ALL other column names from the AseConfig dictionary
			if (DbmsConfigManager.hasInstance())
			{
    			IDbmsConfig dbmsCfg = DbmsConfigManager.getInstance();
    			for (int i=0; i<dbmsCfg.getColumnCount(); i++)
    				sbSql.append(", ").append(lq).append(dbmsCfg.getColumnName(i)).append(rq);
			}

			// Add ending )
			sbSql.append(") \n");

//			// add: values(?, ...)
//			if (addPrepStatementQuestionMarks)
//			{
//				sbSql.append("values(?, ");
//				for (int i=0; i<aseCfg.getColumnCount(); i++)
//					sbSql.append("?, ");
//
//				// remove last ", "
//				sbSql.delete(sbSql.length()-2, sbSql.length());
//
//				// Add ending )
//				sbSql.append(") \n");
//			}
			// add: values(?, ...)
			if (addPrepStatementQuestionMarks)
			{
				sbSql.append("values(?");
				if (DbmsConfigManager.hasInstance())
				{
    				for (int i=0; i<DbmsConfigManager.getInstance().getColumnCount(); i++)
    					sbSql.append(", ?");
				}

				// Add ending )
				sbSql.append(") \n");
			}
		}
		else if (type == SESSION_DBMS_CONFIG_TEXT)
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(lq).append("SessionStartTime").append(rq).append(", ");
			sbSql.append(lq).append("configName")      .append(rq).append(", ");
			sbSql.append(lq).append("configText")      .append(rq).append("");
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?) \n");
		}
		else if (type == DDL_STORAGE)
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(lq).append("dbname")       .append(rq).append(", ");
			sbSql.append(lq).append("owner")        .append(rq).append(", ");
			sbSql.append(lq).append("objectName")   .append(rq).append(", ");
			sbSql.append(lq).append("type")         .append(rq).append(", ");
			sbSql.append(lq).append("crdate")       .append(rq).append(", ");
			sbSql.append(lq).append("sampleTime")   .append(rq).append(", ");
			sbSql.append(lq).append("source")       .append(rq).append(", ");
			sbSql.append(lq).append("dependParent") .append(rq).append(", ");
			sbSql.append(lq).append("dependLevel")  .append(rq).append(", ");
			sbSql.append(lq).append("dependList")   .append(rq).append(", ");
			sbSql.append(lq).append("objectText")   .append(rq).append(", ");
			sbSql.append(lq).append("dependsText")  .append(rq).append(", ");
			sbSql.append(lq).append("optdiagText")  .append(rq).append(", ");
			sbSql.append(lq).append("extraInfoText").append(rq).append("");
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n");
		}
//		else if (type == SQL_CAPTURE_SQLTEXT)
//		{
//		}
//		else if (type == SQL_CAPTURE_STATEMENTS)
//		{
//		}
//		else if (type == SQL_CAPTURE_PLANS)
//		{
//		}
		else if (type == ALARM_ACTIVE)
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(lq).append("alarmClass"             ).append(rq).append(", "); // 1
			sbSql.append(lq).append("serviceType"            ).append(rq).append(", "); // 2
			sbSql.append(lq).append("serviceName"            ).append(rq).append(", "); // 3
			sbSql.append(lq).append("serviceInfo"            ).append(rq).append(", "); // 4
			sbSql.append(lq).append("extraInfo"              ).append(rq).append(", "); // 5
			sbSql.append(lq).append("category"               ).append(rq).append(", "); // 6
			sbSql.append(lq).append("severity"               ).append(rq).append(", "); // 7
			sbSql.append(lq).append("state"                  ).append(rq).append(", "); // 8
			sbSql.append(lq).append("repeatCnt"              ).append(rq).append(", "); // 9
			sbSql.append(lq).append("duration"               ).append(rq).append(", "); // 10
			sbSql.append(lq).append("createTime"             ).append(rq).append(", "); // 11
			sbSql.append(lq).append("cancelTime"             ).append(rq).append(", "); // 12
			sbSql.append(lq).append("timeToLive"             ).append(rq).append(", "); // 13
			sbSql.append(lq).append("threshold"              ).append(rq).append(", "); // 14
			sbSql.append(lq).append("data"                   ).append(rq).append(", "); // 15
			sbSql.append(lq).append("lastData"               ).append(rq).append(", "); // 16
			sbSql.append(lq).append("description"            ).append(rq).append(", "); // 17
			sbSql.append(lq).append("lastDescription"        ).append(rq).append(", "); // 18
			sbSql.append(lq).append("extendedDescription"    ).append(rq).append(", "); // 19
			sbSql.append(lq).append("lastExtendedDescription").append(rq).append("");   // 20
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n");
				//                   1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20
		}
		else if (type == ALARM_HISTORY)
		{
			sbSql.append("insert into ").append(tabName).append(" (");
			sbSql.append(lq).append("SessionStartTime"       ).append(rq).append(", "); // 1
			sbSql.append(lq).append("SessionSampleTime"      ).append(rq).append(", "); // 2
			sbSql.append(lq).append("eventTime"              ).append(rq).append(", "); // 3
			sbSql.append(lq).append("action"                 ).append(rq).append(", "); // 4
//			sbSql.append(lq).append("isActive"               ).append(rq).append(", "); // commented out
			sbSql.append(lq).append("alarmClass"             ).append(rq).append(", "); // 5
			sbSql.append(lq).append("serviceType"            ).append(rq).append(", "); // 6
			sbSql.append(lq).append("serviceName"            ).append(rq).append(", "); // 7
			sbSql.append(lq).append("serviceInfo"            ).append(rq).append(", "); // 8
			sbSql.append(lq).append("extraInfo"              ).append(rq).append(", "); // 9
			sbSql.append(lq).append("category"               ).append(rq).append(", "); // 10
			sbSql.append(lq).append("severity"               ).append(rq).append(", "); // 11
			sbSql.append(lq).append("state"                  ).append(rq).append(", "); // 12
			sbSql.append(lq).append("repeatCnt"              ).append(rq).append(", "); // 13
			sbSql.append(lq).append("duration"               ).append(rq).append(", "); // 14
			sbSql.append(lq).append("createTime"             ).append(rq).append(", "); // 15
			sbSql.append(lq).append("cancelTime"             ).append(rq).append(", "); // 16
			sbSql.append(lq).append("timeToLive"             ).append(rq).append(", "); // 17
			sbSql.append(lq).append("threshold"              ).append(rq).append(", "); // 18
			sbSql.append(lq).append("data"                   ).append(rq).append(", "); // 19
			sbSql.append(lq).append("lastData"               ).append(rq).append(", "); // 20
			sbSql.append(lq).append("description"            ).append(rq).append(", "); // 21
			sbSql.append(lq).append("lastDescription"        ).append(rq).append(", "); // 22
			sbSql.append(lq).append("extendedDescription"    ).append(rq).append(", "); // 23
			sbSql.append(lq).append("lastExtendedDescription").append(rq).append("");   // 24
			sbSql.append(") \n");
			if (addPrepStatementQuestionMarks)
				sbSql.append("values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) \n");
				//                   1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24
		}
		else if (type == ABS || type == DIFF || type == RATE)
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
			sbSql.append(") \n");

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

	/**
	 * Helper method to generate a: "insert into TABNAME_GRAPH(c1,c2,c3) [values(?,?...)]"
	 * @param cm Counter Model info
	 * @param tgdp Trends Graph information
	 * @param addPrepStatementQuestionMarks if true add "values(?,?,?...)" which can be used by a prepared statement
	 * @return
	 */
	public String getTableInsertStr(DbxConnection conn, CountersModel cm, TrendGraphDataPoint tgdp, boolean addPrepStatementQuestionMarks)
	{
		String tabName = cm.getName() + "_" + tgdp.getName();

		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

		StringBuilder sb = new StringBuilder();

		sb.append("insert into ").append(lq).append(tabName).append(rq).append(" (");
		sb.append(lq).append("SessionStartTime") .append(rq).append(", ");
		sb.append(lq).append("SessionSampleTime").append(rq).append(", ");
		sb.append(lq).append("CmSampleTime")     .append(rq).append(", ");

		// loop all data
		Double[] dataArr  = tgdp.getData();
		if (dataArr == null)
			return null;
		for (int d=0; d<dataArr.length; d++)
		{
			sb.append(lq).append("label_").append(d).append(rq).append(", ");
			sb.append(lq).append("data_") .append(d).append(rq).append(", ");
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
	public String getIndexDdlString(DbxConnection conn, int type, CountersModel cm)
	{
		String lq = getLeftQuoteReplace();
		String rq = getLeftQuoteReplace();
		if (conn != null)
		{
			lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
			rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection
		}

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
			String tabName = getTableName(conn, type, null, false);
			return "create index " + lq+tabName+"_ix1"+rq + " on " + lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";

//			if ( DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(getDatabaseProductName()) )
//				return "create index " +     tabName+"_ix1"   + " on " + lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";
//			else
//				return "create index " + lq+tabName+"_ix1"+rq + " on " + lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";
		}
		else if (type == SESSION_MON_TAB_DICT)
		{
			return null;
		}
		else if (type == SESSION_MON_TAB_COL_DICT)
		{
			return null;
		}
		else if (type == SESSION_DBMS_CONFIG)
		{
			return null;
		}
		else if (type == SESSION_DBMS_CONFIG_TEXT)
		{
			return null;
		}
		else if (type == DDL_STORAGE)
		{
			return null;
		}
//		else if (type == SQL_CAPTURE_SQLTEXT)
//		{
//			return null;
//		}
//		else if (type == SQL_CAPTURE_STATEMENTS)
//		{
//			return null;
//		}
//		else if (type == SQL_CAPTURE_PLANS)
//		{
//			return null;
//		}
		else if (type == ALARM_ACTIVE)
		{
			return null;
		}
		else if (type == ALARM_HISTORY)
		{
			String tabName = getTableName(conn, type, null, false);
			return "create index " + lq+tabName+"_ix1"+rq + " on " + lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";
			
//			if ( DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(getDatabaseProductName()) )
//				return "create index " +     tabName+"_ix1"   + " on " + lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";
//			else
//				return "create index " + lq+tabName+"_ix1"+rq + " on " + lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";
		}
		else if (type == ABS || type == DIFF || type == RATE)
		{
			String tabName = getTableName(conn, type, cm, false);
			return "create index " + lq+tabName+"_ix1"+rq + " on " + lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";

//			if ( DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(getDatabaseProductName()) )
//				return "create index " +     tabName+"_ix1"   + " on " + lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";
//			else
//				return "create index " + lq+tabName+"_ix1"+rq + " on " + lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";
		}
		else
		{
			return null;
		}
	}

	public String getGraphTableDdlString(DbxConnection conn, String tabName, TrendGraphDataPoint tgdp)
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

		StringBuilder sb = new StringBuilder();

//		sb.append("create table " + lq+tabName+rq + "\n");
//		sb.append("( \n");
//		sb.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//		sb.append("   ,"+fill(lq+"SessionSampleTime"+rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//		sb.append("   ,"+fill(lq+"CmSampleTime"     +rq,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
//		sb.append("\n");
		sb.append("create table " + lq+tabName+rq + "\n");
		sb.append("( \n");
		sb.append("    "+fill(lq+"SessionStartTime" +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP),20)+" "+getNullable(false)+"\n");
		sb.append("   ,"+fill(lq+"SessionSampleTime"+rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP),20)+" "+getNullable(false)+"\n");
		sb.append("   ,"+fill(lq+"CmSampleTime"     +rq,40)+" "+fill(getDatatype(conn, Types.TIMESTAMP),20)+" "+getNullable(false)+"\n");
		sb.append("\n");

		// loop all data
		Double[] dataArr  = tgdp.getData();
		for (int d=0; d<dataArr.length; d++)
		{
//			sb.append("   ,"+fill(lq+"label_"+d+rq,40)+" "+fill(getDatatype("varchar",100,-1,-1),20)+" "+getNullable(true)+"\n");
//			sb.append("   ,"+fill(lq+"data_" +d+rq,40)+" "+fill(getDatatype("numeric", -1,16, 2),20)+" "+getNullable(true)+"\n");
			sb.append("   ,"+fill(lq+"label_"+d+rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,100   ),20)+" "+getNullable(true)+"\n");
			sb.append("   ,"+fill(lq+"data_" +d+rq,40)+" "+fill(getDatatype(conn, Types.NUMERIC, 16, 2),20)+" "+getNullable(true)+"\n");
		}
		sb.append(") \n");

		//System.out.println("getGraphTableDdlString: "+sb.toString());
		return sb.toString();
	}

	public String getGraphIndexDdlString(DbxConnection conn, String tabName, TrendGraphDataPoint tgdp)
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection
		
		return "create index " + lq+tgdp.getName()+"_ix1"+rq + " on " + lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";
		
//		if ( DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(getDatabaseProductName()) )
//			return "create index " +    tgdp.getName()+"_ix1"    + " on " + lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";
//		else
//			return "create index " + lq+tgdp.getName()+"_ix1"+rq + " on " + lq+tabName+rq + "("+lq+"SessionSampleTime"+rq+")\n";
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

	public List<String> getGraphAlterTableDdlString(DbxConnection conn, String tabName, TrendGraphDataPoint tgdp)
	throws SQLException
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

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
//					list.add("alter table " + lq+tabName+rq + " add  "+fill(lq+"label_"+d+rq,40)+" "+fill(getDatatype("varchar",100,-1,-1),20)+" "+getNullable(true)+" \n");
//					list.add("alter table " + lq+tabName+rq + " add  "+fill(lq+"data_" +d+rq,40)+" "+fill(getDatatype("numeric", -1,16, 2),20)+" "+getNullable(true)+" \n");
					list.add("alter table " + lq+tabName+rq + " add  "+fill(lq+"label_"+d+rq,40)+" "+fill(getDatatype(conn, Types.VARCHAR,100   ),20)+" "+getNullable(true)+" \n");
					list.add("alter table " + lq+tabName+rq + " add  "+fill(lq+"data_" +d+rq,40)+" "+fill(getDatatype(conn, Types.NUMERIC, 16, 2),20)+" "+getNullable(true)+" \n");
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

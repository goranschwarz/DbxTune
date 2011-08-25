/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.pcs;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import asemon.AseConfig;
import asemon.TrendGraphDataPoint;
import asemon.cm.CountersModel;
import asemon.utils.Configuration;

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
	public static final int ABS                      = 100;
	public static final int DIFF                     = 101;
	public static final int RATE                     = 102;

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
	abstract public String getName();

	/** Need to be implemented by the implementation */
	abstract public void init(Configuration props) throws Exception;


	
	
	/*---------------------------------------------------
	** Methods handling counters
	**---------------------------------------------------
	*/
	public void incInserts() { _inserts++; }
	public void incUpdates() { _updates++; }
	public void incDeletes() { _deletes++; }

	public void incInserts(int cnt) { _inserts += cnt; }
	public void incUpdates(int cnt) { _updates += cnt; }
	public void incDeletes(int cnt) { _deletes += cnt; }

	public void incCreateTables() { _createTables++; }
	public void incAlterTables()  { _alterTables++;  }
	public void incDropTables()   { _dropTables++;   }

	public void incCreateTables(int cnt) { _createTables += cnt; }
	public void incAlterTables (int cnt) { _alterTables  += cnt; }
	public void incDropTables  (int cnt) { _dropTables   += cnt; }

	
	public int getInserts() { return _inserts; }
	public int getUpdates() { return _updates; }
	public int getDeletes() { return _deletes; }

	public int getCreateTables() { return _createTables; }
	public int getAlterTables()  { return _alterTables;  }
	public int getDropTables()   { return _dropTables;   }

	
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
	public void beginOfSample()
	{
	}

	/** Empty implementation */
	public void endOfSample()
	{
	}


	
	/** Empty implementation */
	public void startSession(PersistContainer cont)
	{
	}

	/** Empty implementation */
	public void saveSession(PersistContainer cont)
	{
	}

	/** Empty implementation */
	public void saveCounters(CountersModel cm)
	{
	}

	/** Empty implementation */
	public boolean saveDdl(CountersModel cm)
	{
		return true;
	}
	
	public boolean isDdlCreated(CountersModel cm)
	{
		String tabName = cm.getName();

		return _saveDdlIsCalled.contains(tabName);
	}

	public boolean isDdlCreated(String tabName)
	{
		return _saveDdlIsCalled.contains(tabName);
	}

	public void markDdlAsCreated(CountersModel cm)
	{
		String tabName = cm.getName();

		_saveDdlIsCalled.add(tabName);
	}

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
		case ABS:                      return q + cm.getName() + "_abs"         + q;
		case DIFF:                     return q + cm.getName() + "_diff"        + q;
		case RATE:                     return q + cm.getName() + "_rate"        + q;
		default:
			throw new RuntimeException("Unknown type of '"+type+"' in getTableName()."); 
		}
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
				sbSql.append("create table " + tabName + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"SessionStartTime"+qic,40)+" "+fill(getDatatype("datetime",-1,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"Type"            +qic,40)+" "+fill(getDatatype("varchar", 10,  -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"ParamName"       +qic,40)+" "+fill(getDatatype("varchar", 255, -1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"ParamValue"      +qic,40)+" "+fill(getDatatype("varchar", 4096,-1,-1),20)+" "+getNullable(false)+"\n");
//				sbSql.append("   ,"+fill(qic+"ParamValue"      +qic,40)+" "+fill(getDatatype("varchar", 1536,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+qic+"SessionStartTime"+qic+", "+qic+"ParamName"+qic+")\n");
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
				sbSql.append("    "+fill(qic+"SessionStartTime" +qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"SessionSampleTime"+qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"CmName"           +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"type"             +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"graphCount"       +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"absRows"          +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"diffRows"         +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
				sbSql.append("   ,"+fill(qic+"rateRows"         +qic,40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(true)+"\n");
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
			sb.append("   ,"+fill(qic+"data_" +d+qic,40)+" "+fill(getDatatype("numeric",-1,10, 1),20)+" "+getNullable(true)+"\n");
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

	public String getGraphAlterTableDdlString(Connection conn, String tabName, TrendGraphDataPoint tgdp)
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

		if (colCounter > 0)
		{
			colCounter -= 3; // take away: SessionStartTime, SessionSampleTime, SampleTime
			colCounter = colCounter / 2;
			
			Double[] dataArr  = tgdp.getData();
			if (colCounter < dataArr.length)
			{
				StringBuilder sb = new StringBuilder();
				sb.append("alter table " + qic+tabName+qic + "\n");
				
				for (int d=colCounter; d<dataArr.length; d++)
				{
					sb.append("   add  "+fill(qic+"label_"+d+qic,40)+" "+fill(getDatatype("varchar",30,-1,-1),20)+" "+getNullable(true)+",\n");
					sb.append("        "+fill(qic+"data_" +d+qic,40)+" "+fill(getDatatype("numeric",-1,10, 1),20)+" "+getNullable(true)+" \n");
				}
				//System.out.println("getGraphAlterTableDdlString: "+sb.toString());
				return sb.toString();
			}
		}
		return "";
	}
}

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.pcs;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import asemon.CountersModel;
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
	public final int SESSION  = 0;
	public final int ABS      = 1;
	public final int DIFF     = 2;
	public final int RATE     = 3;

	/** Character used for quoted identifier */
	public String  qic = "\"";
	

	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/
	/** when DDL is "created" we do not need to do this again */
	private List _saveDdlIsCalled = new LinkedList();


	
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
//		return getNullable(rsmd.isNullable(col)>0);
		return getNullable(true);
	}

	/** Helper method */
	public String getTableName(int type, CountersModel cm)
	{
		switch (type)
		{
		case SESSION: return "SampleSessions";
		case ABS:     return cm.getName() + "_abs";
		case DIFF:    return cm.getName() + "_diff";
		case RATE:    return cm.getName() + "_rate";
		default:
			throw new RuntimeException("Unknown type of '"+type+"' in getTableName()."); 
		}
	}

	/** Helper method to generate a DDL string, to get the 'create table' */
	public String getTableDdlString(int type, CountersModel cm)
	{
		String tabName = getTableName(type, cm);
		StringBuffer sbSql = new StringBuffer();

		try
		{
			if (type == SESSION)
			{
				sbSql.append("create table " + qic+tabName+qic + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"ParentSampleTime"+qic,40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"ServerName"      +qic,40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("\n");
				sbSql.append("   ,PRIMARY KEY ("+qic+"ParentSampleTime"+qic+", "+qic+"ServerName"+qic+")\n");
				sbSql.append(") \n");
			}
			else if (type == ABS || type == DIFF || type == RATE)
			{
				sbSql.append("create table " + qic+tabName+qic + "\n");
				sbSql.append("( \n");
				sbSql.append("    "+fill(qic+"ParentSampleTime"+qic, 40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"SampleTime"      +qic, 40)+" "+fill(getDatatype("datetime",-1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"SampleMs"        +qic, 40)+" "+fill(getDatatype("int",     -1,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("   ,"+fill(qic+"ServerName"      +qic, 40)+" "+fill(getDatatype("varchar", 30,-1,-1),20)+" "+getNullable(false)+"\n");
				sbSql.append("\n");
				
				ResultSetMetaData rsmd = cm.getResultSetMetaData();
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
			_logger.warn("SQLException, Error generating DDL to Persistant Counter DB.", e);
		}
		
		return sbSql.toString();
	}

	/** Helper method to generate a DDL string, to get the 'create index' */
	public String getIndexDdlString(int type, CountersModel cm)
	{
		if (type == SESSION)
		{
			return null;
		}
		else if (type == ABS || type == DIFF || type == RATE)
		{
			String tabName = getTableName(type, cm);
			return "create index " + qic+tabName+"_ix1"+qic + " on " + qic+tabName+qic + "("+qic+"ParentSampleTime"+qic+", "+qic+"ServerName"+qic+")\n";
//			return "create index " + qic+"ix1_sampleTime"+qic + " on " + qic+tabName+qic + "("+qic+"ParentSampleTime"+qic+", "+qic+"ServerName"+qic+")\n";
		}
		else
		{
			return null;
		}
	}

}

/**
*/


package com.asetune.cm;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.asetune.utils.AseConnectionUtils;


public  class SamplingCnt 
extends CounterTableModel
{
    /** Log4j logging. */
	private   static Logger _logger = Logger.getLogger(SamplingCnt.class);
    private static final long serialVersionUID = 4786911833477005253L;

	private   boolean            _negativeDiffCountersToZero = true;
	protected String             _name           = null; // Used for debuging
	private   String[]           _diffColNames   = null; // if we need to do PK merging...
	private   List<String>       _colNames       = null;
	private   List<Integer>      _colSqlType     = null;
	private   List<String>       _colSqlTypeName = null;
	private   List<String>       _colClassName   = null;
	private   boolean[]          _colIsPk        = null; // one boolean for each column
	private   int[]              _pkPosArray     = null; // one boolean for each column
	private   List<List<Object>> _rows           = null;

	/** List contains key<String>. position in List is the rowId */
	private ArrayList<String>    _rowidToKey     = null; 

	/** Map containing <Integer>, which is the rowId for this key<String> */
	private HashMap<String,Integer> _keysToRowid = null;

	/** Map that holds number of rows (merges) into this PrimaryKey, will be null if no PK duplicates exists
	 * NOTE: column name that reflects the merge count MUST be named 'dupMergeCount'/'dupRowCount'. */
	private HashMap<String,Integer> _pkDupCountMap = null; 
	
	public  Timestamp     samplingTime;
	public  long	      interval      = 0;

	// If messages inside a ResultSet should be treated and appended to a column
	// this is used for the StatementCache query: 
	//     select show_plan(-1,SSQLID,-1,-1) as Showplan, * from monCachedStatement
	// The column name should be: msgAsColValue
	int _pos_msgAsColValue = -1;


//	static
//	{
//		_logger.setLevel(Level.TRACE);
//	}

	public SamplingCnt(String name, boolean negativeDiffCountersToZero, String[] diffColNames)
	{
		_name = name;
		_negativeDiffCountersToZero = negativeDiffCountersToZero;
		_diffColNames = diffColNames;
	}
	private SamplingCnt(SamplingCnt sc, boolean cloneRows, String name)
	{
		_name                       = name;
		_keysToRowid                = sc._keysToRowid;
		_colNames                   = sc._colNames;
		_colSqlType                 = sc._colSqlType;
		_colSqlTypeName             = sc._colSqlTypeName;
		_colClassName               = sc._colClassName;
		_colIsPk                    = sc._colIsPk;
		_pkPosArray                 = sc._pkPosArray;
		_rowidToKey                 = sc._rowidToKey;
		samplingTime                = sc.samplingTime;
		interval                    = sc.interval;
		_negativeDiffCountersToZero = sc._negativeDiffCountersToZero;
		_diffColNames               = sc._diffColNames;

		if (cloneRows)
		{
//			_rows = sc._rows.clone();
			_rows = new ArrayList<List<Object>>(sc._rows);
		}
		else
		{
			_keysToRowid = new HashMap<String,Integer>();
			_rowidToKey  = new ArrayList<String>();
			_rows        = new ArrayList<List<Object>>();
		}
	}


	
	/*---------------------------------------------------
	** BEGIN: implementing TableModel or overriding AbstractTableModel
	**---------------------------------------------------
	* public int getRowCount();
	* public int getColumnCount();
	* public String getColumnName(int i);
	* public Class getColumnClass(int i);
	* public boolean isCellEditable(int i, int j);
	* public Object getValueAt(int i, int j);
	* public void setValueAt(Object obj, int i, int j);
	* public void addTableModelListener(TableModelListener tablemodellistener);
	* public void removeTableModelListener(TableModelListener tablemodellistener);
	*/
	public int getRowCount()
	{
		return (_rows == null) ? 0 : _rows.size();
	}

	public int getColumnCount()
	{
		return _colNames == null ? 0 : _colNames.size();
	}

	public String getColumnName(int colid)
	{
		if (_colNames == null || colid < 0 || colid >= getColumnCount())
			return null;
		return _colNames.get(colid);
	}

	public Class<?> getColumnClass(int colid)
	{
		if (getRowCount() == 0)
			return Object.class;

		Object o = getValueAt(0, colid);
		Class<?> clazz = o!=null ? o.getClass() : Object.class;
		if (o instanceof Timestamp)
			return Object.class;
		else
			return clazz;
//		return o!=null ? o.getClass() : Object.class;
	}

	public boolean isCellEditable(int i, int j)
	{
		return false;
    }

	public Object getValueAt(int row, int col)
	{
		if (_rows == null || row < 0 || col < 0)
			return null;

		//
		// JTable/JXTable in some cases, when:
		// - refreshing tables
		// - and rows has been deleted (from the model)
		// - and Sorter has been installed
		// - and Highlighters are used
		// - and doing JXTable.packAll() from within tableChanged() method for TableModelListener's
		// probably in some other cases as well....
		//
		// In some circumstances it tries to get a rows that is still in the view,
		// but has been deleted/removed from the model.
		// So lets be prepared and check for this... (less Exception creation, 
		// which could be expensive) and just return null in those cases.
		//
//if (row==0&&col==0)
//	System.out.println("Accessing: (0,0), SamplingCnt.name="+_name+", getRowCount=()"+getRowCount()+", getColumnCount()="+getColumnCount()+", Thread='"+Thread.currentThread().getName()+"'.");

		if (row >= _rows.size())
		{
			// Write a ';' might give me a clue that something is still not correct...
			// But writing a whole exception to the errorlog might be overkill in "normal" mode.
			System.out.print(";");
//			new Exception("getValueAt(r="+row+",c="+col+")-name="+_name+": Dummy exception from thread '("+Thread.currentThread().getName()+")'.").printStackTrace();
			
			if (_logger.isDebugEnabled())
			{
				try {_rows.get(row);}
				catch(IndexOutOfBoundsException e) 
				{
					_logger.debug("ERROR Accessing: getValueAt(row="+row+", col="+col+"), SamplingCnt.name="+_name+", model.size()="+_rows.size()+", Thread='"+Thread.currentThread().getName()+"', Exception="+e, e);
//					System.out.println("ERROR Accessing: getValueAt(row="+row+", col="+col+"), SamplingCnt.name="+_name+", model.size()="+_rows.size()+", Thread='"+Thread.currentThread().getName()+"', Exception="+e);
//					e.printStackTrace();
				}
			}
			return null;
		}

		// GET THE ROW NOW
		try
		{
			return _rows.get(row).get(col);
		}
		catch (IndexOutOfBoundsException e)
		{
			_logger.warn(_name+": getValueAt(row="+row+", col="+col+"): _rows.size()="+_rows.size()+", SamplingCnt.name="+_name+", Thread='"+Thread.currentThread().getName()+"', returning NULL, IndexOutOfBoundsException... "+e.getMessage());
		}
		catch (NullPointerException e)
		{
			_logger.warn(_name+": getValueAt(row="+row+", col="+col+"): SamplingCnt.name="+_name+", Thread='"+Thread.currentThread().getName()+"', returning NULL, NullPointerException... "+e.getMessage());
		}
		return null;
	}

	/** sets value in a cell, if the cell isn't there, "create" it... */
	public void setValueAt(Object value, int row, int col)
	{
		if (_rows == null)
			_rows = new ArrayList<List<Object>>();

		// Expand rows to correct numbers, if there wasn't enough
		if ( ! (row < _rows.size()) )
			for (int i=_rows.size(); i<=row; i++)
				_rows.add(new ArrayList<Object>(getColumnCount()));

		// Expand cols to correct numbers, if there wasn't enough
		List<Object> r = _rows.get(row);
		if ( ! (col < r.size()) )
			for (int i=r.size(); i<=col; i++)
				r.add("-empty-");

		// Set the value
		r.set(col, value);
	}
	
//	public void addTableModelListener(TableModelListener tablemodellistener)
//	{
//	}
//	public void removeTableModelListener(TableModelListener tablemodellistener)
//	{
//	}

	/*---------------------------------------------------
	** END: implementing TableModel or overriding AbstractTableModel
	**---------------------------------------------------
	*/


	
	public String getName()
	{
		return _name;
	}
	
	public void setColumnNames(List<String> cols)
	{
		_colNames = new ArrayList<String>(cols);
	}

	public List<String> getColNames()
	{
		return _colNames;
	}

	public int getColId(String colName)
	{
		if (_colNames == null)
			return -1;

		return _colNames.indexOf(colName);
	}

	public String getColClassName(int colId)
	{
		return _colClassName.get(colId);
	}

	public int getColSqlType(int colId)
	{
		return _colSqlType.get(colId);
	}

	public String getColSqlTypeName(int colId)
	{
		return _colSqlTypeName.get(colId);
	}

	public String getPkValue(int row)
	{
		return _rowidToKey.get(row);
	}
	public int getRowNumberForPkValue(String pkStr)
	{
		Integer i = _keysToRowid.get(pkStr);
		if (i == null)
		{
			// PK has ':' appended to the END of the str, so lets check for this as well
			// NOTE: this might change in the future...
			i = _keysToRowid.get(pkStr+":");
			if (i == null)
			{
				//throw new RuntimeException("Couldn't find pk '"+pkStr+"'.");
				return -1;
			}
		}
		if (_logger.isDebugEnabled())
			_logger.debug(_name+": getRowNumberForPkValue(pk='"+pkStr+"'), returns="+i);

		return i;
	}

	/** colId starts at 0 */
	public boolean isColPartOfPk(int colId)
	{
		if (_colIsPk == null)
			return false;

		if (colId < 0 || colId > getColumnCount() )
			return false; // or should we: throw new IndexOutOfBoundsException("description");

		return _colIsPk[colId];
	}

	private void checkWarnings(Statement st) 
	throws SQLException
	{
		boolean hasWarning = false;
		StringBuilder sb = new StringBuilder();
		try
		{
			SQLWarning w = st.getWarnings();
			while (w != null)
			{
				String sqlState = w.getSQLState();
				if (sqlState != null && sqlState.equals("010P4")) 
				{
					// skip/disregard: 010P4: An output parameter was received and ignored.
				}
				else
				{
					hasWarning = true;
	
					String wStr = w.toString();
					if (wStr == null)
						hasWarning = false;
					else if (wStr.trim().equals(""))
						hasWarning = false;
					else
					{
						wStr = "AseMsgNum="+w.getErrorCode()+", " + w.toString();
						_logger.warn("SamplingCnt("+_name+").Warning : " + wStr);
						sb.append(wStr).append("\n");
					}
				}

				w = w.getNextWarning();
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("SamplingCnt("+_name+").getWarnings : " + ex);
			ex.printStackTrace();
		}
		if (hasWarning)
		{
			throw new SQLException("SQL Warning in("+_name+") Messages: "+sb);
		}
	}

	private Object fixNullValue(int col)
	{
		if (_colSqlType == null)
		{
			_logger.error("colSqlType are null, this should not happen.");
			return new Object();
		}
		int objSqlType = _colSqlType.get(col - 1);

		switch (objSqlType)
		{
		case java.sql.Types.BIT:          return new Boolean(false);
		case java.sql.Types.TINYINT:      return new Byte(Byte.parseByte("0"));
		case java.sql.Types.SMALLINT:     return new Short(Short.parseShort("0"));
		case java.sql.Types.INTEGER:      return new Integer(0);
		case java.sql.Types.BIGINT:       return new Long(0);
		case java.sql.Types.FLOAT:        return new Float(0);
		case java.sql.Types.REAL:         return new Float(0);
		case java.sql.Types.DOUBLE:       return new Double(0);
		case java.sql.Types.NUMERIC:      return new BigDecimal(0);
		case java.sql.Types.DECIMAL:      return new BigDecimal(0);
		case java.sql.Types.CHAR:         return "";
		case java.sql.Types.VARCHAR:      return "";
		case java.sql.Types.LONGVARCHAR:  return "";
		case java.sql.Types.DATE:         return new Date(0);
		case java.sql.Types.TIME:         return new Time(0);
		case java.sql.Types.TIMESTAMP:    return new Timestamp(0);
		case java.sql.Types.BINARY:       return new byte[0];
		case java.sql.Types.VARBINARY:    return new byte[0];
		case java.sql.Types.LONGVARBINARY:return new byte[0];
		case java.sql.Types.NULL:         return null;
		case java.sql.Types.OTHER:        return null;
		case java.sql.Types.JAVA_OBJECT:  return new Object();
		case java.sql.Types.DISTINCT:     return "-DISTINCT-";
		case java.sql.Types.STRUCT:       return "-STRUCT-";
		case java.sql.Types.ARRAY:        return "-ARRAY-";
		case java.sql.Types.BLOB:         return "";
		case java.sql.Types.CLOB:         return "";
		case java.sql.Types.REF:          return "-REF-";
		case java.sql.Types.DATALINK:     return "-DATALINK-";
		case java.sql.Types.BOOLEAN:      return new Boolean(false);
		default:
			_logger.error("Unknow SQL datatype, when translating a NULL value.");
			return new Object();
		}
	}

	public boolean getCnt(CountersModel cm, Connection conn, String sql, List<String> pkList)
	throws SQLException
	{
		int queryTimeout = cm.getQueryTimeout();
		if (_logger.isDebugEnabled())
			_logger.debug(_name+": queryTimeout="+queryTimeout);

		try
		{
			String sendSql = "select getdate() \n" + sql;

			Statement stmnt = conn.createStatement();
			ResultSet rs;

			stmnt.setQueryTimeout(queryTimeout); // XX seconds query timeout
			if (_logger.isDebugEnabled())
				_logger.debug("QUERY_TIMEOUT="+queryTimeout+", for SampleCnt='"+_name+"'.");

			_rows   = new ArrayList<List<Object>>();

			if (_logger.isTraceEnabled())
			{
				_logger.trace("Sending SQL: "+sendSql);
			}

			// The below is solved with:
			// - Messages on the ResultSet is stored in a column named 'msgAsColValue'
			// - check the variable "_pos_msgAsColValue" for more...
			//
			// if The ResultSet contains a column named "msgAsColValue" or similar, stuff the message in that column
			// The intention for this is to fix: 
			//    select show_plan(-1,SSQLID,-1,-1) as Showplan, * from monCachedStatement
			// the above query delivers: msg + resultset for row row 1, msg + resultset for row row 2...
			// Example:
			//
			// SELECT InstanceID, SSQLID, 
			//        show_plan(-1,SSQLID,-1,-1) as Showplan, 
			//        Hashkey, UserID, SUserID, DBID, 
			//        show_cached_text(SSQLID) as sqltext 
			// FROM monCachedStatement
			// 
			// Msg: QUERY PLAN FOR STATEMENT 1 (at line 1).
			// Msg:
			// Msg:
			// Msg:    STEP 1
			// Msg:        The type of query is SELECT.
			// Msg:
			// Msg:        1 operator(s) under root
			// Msg:
			// Msg:       |ROOT:EMIT Operator (VA = 1)
			// Msg:       |
			// Msg:       |   |SCAN Operator (VA = 0)
			// Msg:       |   |  FROM TABLE
			// Msg:       |   |  monDeadLock
			// Msg:       |   | External Definition: $monCachedStatement
			// 
			// Rs row 1: InstanceID SSQLID      Showplan    Hashkey     UserID      SUserID     DBID
			// Rs row 1:         sqltext
			// Rs row 1: ---------- ----------- ----------- ----------- ----------- ----------- ------
			// Rs row 1:         --------------------------------------------------------------------------------------------------------------------
			// Rs row 1:          0    71220424           0  1836678972           1           1      1
			// Rs row 1:         select * from monDeadLock
			// 
			// Msg: QUERY PLAN FOR STATEMENT 1 (at line 1).
			// Msg:
			// Msg:
			// Msg:    STEP 1
			// Msg:        The type of query is SELECT.
			// Msg:
			// Msg:        2 operator(s) under root
			// Msg:
			// Msg:       |ROOT:EMIT Operator (VA = 2)
			// Msg:       |
			// Msg:       |   |SCALAR AGGREGATE Operator (VA = 1)
			// Msg:       |   |  Evaluate Ungrouped MAXIMUM AGGREGATE.
			// Msg:       |   |
			// Msg:       |   |   |SCAN Operator (VA = 0)
			// Msg:       |   |   |  FROM TABLE
			// Msg:       |   |   |  monWaitEventInfo
			// Msg:       |   |   | External Definition: $monCachedStatement
			// 
			// Rs row 2: InstanceID SSQLID      Showplan    Hashkey     UserID      SUserID     DBID
			// Rs row 2:         sqltext
			// Rs row 2: ---------- ----------- ----------- ----------- ----------- ----------- ------
			// Rs row 2:         --------------------------------------------------------------------------------------------------------------------
			// Rs row 2:          0   954699569           0   286605408           1           1      1
			// Rs row 2:         select max(WaitEventID) from monWaitEventInfo
			
			int rsNum = 0;
			int rowsAffected = 0;
			boolean hasRs = stmnt.execute(sendSql);
			checkWarnings(stmnt);
			do
			{
				if (hasRs)
				{
					// Get next result set to work with
					rs = stmnt.getResultSet();
					checkWarnings(stmnt);

					if (rsNum == 0)
					{
						while(rs.next())
							samplingTime = rs.getTimestamp(1);
					}
					else
					{
						ResultSetMetaData rsmd = rs.getMetaData();
						if ( ! cm.hasResultSetMetaData() )
							cm.setResultSetMetaData(rsmd);

						if (readResultset(rs, rsmd, pkList, rsNum))
							rs.close();

						checkWarnings(stmnt);
					}

					rsNum++;
				}

				// Treat update/row count(s)
				rowsAffected = stmnt.getUpdateCount();
				if (rowsAffected >= 0)
				{
				}

				// Check if we have more result sets
				hasRs = stmnt.getMoreResults();

				_logger.trace( "--hasRs="+hasRs+", rsNum="+rsNum+", rowsAffected="+rowsAffected );
			}
			while (hasRs || rowsAffected != -1);

			checkWarnings(stmnt);

			// Close the statement
			stmnt.close();

			return true;
		}
		catch (SQLException sqlEx)
		{
			_logger.warn("SamplingCnt("+_name+").getCnt : " + sqlEx.getErrorCode() + " " + sqlEx.getMessage() + ". SQL: "+sql, sqlEx);
			if (sqlEx.toString().indexOf("SocketTimeoutException") > 0)
			{
				_logger.info("QueryTimeout in '"+_name+"', with query timeout '"+queryTimeout+"'. This can be changed with the config option '"+_name+".queryTimeout=seconds' in the config file.");
			}
//			if (sqlEx.getMessage().equals("JZ0C0: Connection is already closed."))
//			{
//				GetCounters.setRefreshing(false);
//				MainFrame.terminateConnection();
//			}
			//return false;
			throw sqlEx;
		}
//		catch (Exception ev)
//		{
//			_logger.error("SamplingCnt.getCnt : " + ev);
//			ev.printStackTrace();
//			return false;
//		}
	}

	/**
	 * NOTE: if you change in this one, do not forger to 
	 *       replicate the change into method: 	public boolean addRow(List<Object> row)
	 *       
	 * @param rs
	 * @param rsmd
	 * @param pkList
	 * @param rsNum
	 * @return
	 * @throws SQLException
	 */
	private boolean readResultset(ResultSet rs, ResultSetMetaData rsmd, List<String> pkList, int rsNum)
	throws SQLException
	{
		if (_colNames == null)
		{
			// Initialize column names
			_colNames       = new ArrayList<String>();
			_colSqlType     = new ArrayList<Integer>();
			_colSqlTypeName = new ArrayList<String>();
			_colClassName   = new ArrayList<String>();
			int colCount    = rsmd.getColumnCount();
			_colIsPk        = new boolean[colCount];

			_keysToRowid    = new HashMap<String,Integer>();
			_rowidToKey     = new ArrayList<String>();

			List<Integer> tmpPkPos = new LinkedList<Integer>();

			for (int i=1; i<=colCount; i++)
			{
				//String colname = rsmd.getColumnName(i); 
				String colname = rsmd.getColumnLabel(i); 

				_colNames      .add(colname);
				_colSqlType    .add(new Integer(rsmd.getColumnType(i)));
				_colSqlTypeName.add(rsmd.getColumnTypeName(i));
				_colClassName  .add(rsmd.getColumnClassName(i));

				// pkList could contain the ColumnName
				// or a column position
				_colIsPk[i-1] = false;
				if ( pkList != null && pkList.size() > 0)
				{
					if ( pkList.contains(colname) ) 
						_colIsPk[i-1] = true;
					if ( pkList.contains( Integer.toString(i) ) )
						_colIsPk[i-1] = true;
					
					if (_colIsPk[i-1])
						tmpPkPos.add(new Integer(i));
				}

				// Special... if colname is "msgAsColValue", 
				// all TDS Msg on the ResultSet should be added to this column
				if ( "msgAsColValue".equals(colname) )
					_pos_msgAsColValue = i;
				// Note on the above:
				// It would be nice to do, something like: 
				// - strip away the msgAsColValue::theRealColName part and replace it with a "real" column name
				// - but since the ResultSetMetaData is not writable, it can't be done
				// - and the PersistWriterXXX uses the RSMD to get the column name... so the persist table would be faulty
			}

			if (pkList != null && (pkList.size() != tmpPkPos.size()) )
			{
				throw new RuntimeException("sample, can't find all the primary keys in the ResultSet. pkList='"+pkList+"', _colNames='"+_colNames+"', tmpPkPos='"+tmpPkPos+"'.");
			}
				
			_pkPosArray = new int[tmpPkPos.size()];
			for(int i= 0; i<tmpPkPos.size(); i++)
			{
//				_pkPosArray[i] = ((Integer)tmpPkPos.get(i)).intValue();
				_pkPosArray[i] = tmpPkPos.get(i);
			}
			tmpPkPos = null;
		}
		else // check previous result set for match
		{
			int cols = rsmd.getColumnCount();
			if (getColumnCount() != cols)
			{
				_logger.error("ResultSet number "+rsNum+" has "+cols+" while it was expected to have "+getColumnCount()+". Skipping this result set.");
				rs.close();
				return false;
			}

			// Check data types match for all columns.
			for (int i=1; i<=cols; i++)
			{
				String oldType = _colClassName.get(i-1);
				String newType = rsmd.getColumnClassName(i);
				if ( ! oldType.equals(newType) )
				{
					_logger.error("ResultSet number "+rsNum+" column number "+i+" has SQL datatype "+newType+", while we expected datatype "+oldType+".  Skipping this result set.");
					rs.close();
					return false;
				}
			}
		}

		// Load counters in memory
		int rsRowNum = 0;
		List<Object> row;
		Object val;
		StringBuilder key;
		_logger.debug("---");
		while (rs.next())
		{
			key = new StringBuilder();
			row = new ArrayList<Object>();
			int colCount = getColumnCount();

			// get any Messages that are attached to the ResultSet
			String msgAsColValue = "";
			SQLWarning sqlwInRs = rs.getWarnings();
			if (sqlwInRs != null)
			{
				if (_pos_msgAsColValue >= 0)
					msgAsColValue = AseConnectionUtils.getSqlWarningMsgs(sqlwInRs);
				else
					_logger.warn("Received a Msg while reading the resultset from '"+getName()+"', This could be mapped to a column by using a column name 'msgAsColValue' in the SELECT statement. Right now it's discarded. The message text: " + AseConnectionUtils.getSqlWarningMsgs(sqlwInRs));
			}

			// Get one row
			for (int i = 1; i <= colCount; i++)
			{
				val = rs.getObject(i);
				if (rsRowNum == 0 && _logger.isTraceEnabled() )
					_logger.trace("READ_RESULTSET(rsnum "+rsNum+", row 0): col=" + i + ", colName=" + (_colNames.get(i - 1) + "                                   ").substring(0, 25) + ", ObjectType=" + (val == null ? "NULL-VALUE" : val.getClass().getName()));

				if (val == null)
					val = fixNullValue(i);

				// IF we had TDS Msg at the ResultSet level
				// AND we had a column named 'msgAsColValue'
				// THEN add the Msg fields instead of the column object
				if (_pos_msgAsColValue == i)
					val = msgAsColValue;

				row.add(val);

				// If this column is part of the PrimaryKey
				if (_colIsPk != null && _colIsPk[i-1] )
				{
					if (val != null)
						key.append(val).append(":");
					else
						_logger.warn("Key containes NULL value, rsNum="+rsNum+", row="+rsRowNum+", col="+i+".");
				}
			}
			if (_logger.isTraceEnabled())
			{
				for (int c=0; c<colCount; c++)
					_logger.trace("   > rsNum="+rsNum+", rsRowNum="+rsRowNum+", getRowCount()="+getRowCount()+", c="+c+", className='"+row.get(c).getClass().getName()+"', value="+row.get(c));
			}
//			if (_name.startsWith(SummaryPanel.CM_NAME))
//			{
//				for (int c=0; c<colCount; c++)
//				System.out.println(SummaryPanel.CM_NAME+":   > rsNum="+rsNum+", rsRowNum="+rsRowNum+", getRowCount()="+getRowCount()+", c="+c+", className='"+row.get(c).getClass().getName()+"', colName="+StringUtil.left(getColumnName(c),20)+", value="+row.get(c));
//			}

			String keyStr = key.toString();

//			if (_colIsPk != null)
			if (pkList != null)
			{
				// Check for DUPLICATES
				Integer intObj = _keysToRowid.get(keyStr);
				if (intObj != null)
				{
					List<Object> curRow = getRow(keyStr);

					int dupMergeCountPos = findColumn("dupMergeCount");
					int dupRowCountPos   = findColumn("dupRowCount");

					// Decide what action to take
					int pkDuplicateAction = 0;
					if (dupRowCountPos   >= 0) pkDuplicateAction = 2;
					if (dupMergeCountPos >= 0) pkDuplicateAction = 1;

					if (pkDuplicateAction != 0 && _diffColNames == null)
					{
						_logger.error("Duplicate key in '"+_name+"', pkDuplicateAction="+pkDuplicateAction+", BUT _diffColNames is null, this should never happen.");

						// Read next row
						continue;
					}
					if (pkDuplicateAction == 0)
					{
						_logger.error("Duplicate key in '"+_name+"', a row for the key '"+key+"' already exists. CurrentRow='"+curRow+"'. NewRow='"+row+"'.");

						// Read next row
						continue;
						//throw new DuplicateKeyException(key, curRow, row);
					}

					// [FIXME] here is where I would put in row merge
					//if (allowRowMerge)
					//	doRowMerge(keyStr, curRow, row);

					if (_pkDupCountMap == null)
						_pkDupCountMap = new HashMap<String, Integer>();
					Integer dupMergeCount = _pkDupCountMap.get(keyStr);
					if (dupMergeCount == null)
						dupMergeCount = new Integer(1); // initial value is/should_be 0
					else
						dupMergeCount = new Integer(dupMergeCount.intValue() + 1);
					_pkDupCountMap.put(keyStr, dupMergeCount);

					// set/increment column 'dupMergeCount'/'dupRowCount'
					if (pkDuplicateAction == 1)
						curRow.set(dupMergeCountPos, dupMergeCount);
					else
						row.set(dupRowCountPos, dupMergeCount);

					// merge the two values
					if (pkDuplicateAction == 1)
					{
						for (String diffColName : _diffColNames) // FIXME _colNames is not the correct list.
						{
							int diffCollNamePos = findColumn(diffColName);
							Number prevColVal = (Number) curRow.get(diffCollNamePos);
							Number thisColVal = (Number)    row.get(diffCollNamePos);

							// Merge the columns and set the new value.
							// On failure, it returns the prevColVal
							Number mergeColVal = mergeColumnValue(prevColVal, thisColVal);
							curRow.set(diffCollNamePos, mergeColVal);

							if (_logger.isTraceEnabled())
							{
								_logger.trace("   >> MERGE for key='"+key+"', rowId="+intObj+", colName='"+diffColName+"', colPos="+diffCollNamePos+", prevValue="+prevColVal+", thisVal="+thisColVal+", mergedVal="+mergeColVal+".");
							}
						}
						
						// on MERGE no need to add the row, so just read next row
						_logger.trace("   >> MERGE continue and red next row from ResultSet.");
						continue;
					}
					// NOT IMPLEMENTED
					if (pkDuplicateAction == 2)
					{
						_logger.error("Duplicate key in '"+_name+"', pkDuplicateAction=2 IS NOT IMPLEMENTED.");

						// Read next row
						continue;
					}
				}
			}

			// save HKEY with corresponding row
//			if (_colIsPk != null)
			if (pkList != null)
			{
				_keysToRowid.put(keyStr, new Integer(getRowCount()));
				_rowidToKey.add(keyStr);
				if (_logger.isTraceEnabled())
				{
					_logger.trace("   >> key='"+key+"', rowId="+getRowCount()+", _rowidToKey.addPos="+(_rowidToKey.size()-1));
				}
			}

			if (_logger.isDebugEnabled())
				_logger.debug("   >> rowAdded: rowId="+getRowCount()+", key="+key+", row="+row);

			// ADD the row
			_rows.add(row);

			rsRowNum++;
		}
		return true;
	}

	/** re-make new row id's. This since we have made deletions of  */
	public void newRowIds()
	{
		// Update the hashtable (since rowid's have changed)
		_keysToRowid = new HashMap<String,Integer>();
		_rowidToKey  = new ArrayList<String>();

		for (int r=0; r<_rows.size(); r++)
		{
			List<Object> row = _rows.get(r);
			StringBuilder key = new StringBuilder();
			int colCount = getColumnCount();
			for (int c=0; c<colCount; c++)
			{
				// If this column is part of the PrimaryKey
				if (_colIsPk != null && _colIsPk[c] )
				{
					Object val = row.get(c);
					if (val != null)
					{
						key.append(val).append(":");
					}
				}
			}
			if (_colIsPk != null)
			{
				String keyStr = key.toString();
				_keysToRowid.put(keyStr, new Integer(r));
				_rowidToKey.add(keyStr);
			}
		}
	}

	/**
	 * NOTE: if you change in this one, do not forger to 
	 *       replicate the change into method: readResultset
	 * 
	 * @param row
	 * @return
	 */
	public boolean addRow(List<Object> row)
	{
		if (row == null)
			return false;
		
		int	colCount = getColumnCount();

		if (row.size() != colCount)
		{
			throw new IndexOutOfBoundsException("The number of columns in current structure is "+colCount+", while the row we attempt to add has "+row.size()+" columns."); 
		}
		// Load counters in memory
		Object val;
		StringBuilder key = new StringBuilder();

		// Fix null values 
		// fix PK and ROWID
		for (int c=0; c<colCount; c++)
		{
			val = row.get(c);
			if (val == null)
			{
				val = fixNullValue(c);
				row.set(c, val);
			}

			// If this column is part of the PrimaryKey
			if (_colIsPk != null && _colIsPk[c] )
			{
				if (val != null)
					key.append(val).append(":");
				else
					_logger.warn("Key containes NULL value, row="+getRowCount()+", col="+c+".");
			}
		}
		String keyStr = key.toString();

		// Check for DUPLICATES
		if (_colIsPk != null)
		{
			Integer io = _keysToRowid.get(keyStr);
			if (io != null)
			{
				List<Object> curRow = getRow(keyStr);

				// [FIXME] here is where I would put in row merge
				//if (allowRowMerge)
				//	doRowMerge(curRow, row);

				_logger.error("Duplicate key in '"+_name+"', a row for the key '"+key+"' already exists. CurrentRow='"+curRow+"'. NewRow='"+row+"'.");
				return false;
				//throw new DuplicateKeyException(key, curRow, row);
			}
		}

		// ADD the row
		_rows.add(row);

		int rowId = _rows.size()-1;

		if (_logger.isDebugEnabled())
			_logger.debug(_name+": addRow(): rowId="+rowId+", key="+key+", row="+row);

		// save HKEY with corresponding row
		if (_colIsPk != null)
		{
			_keysToRowid.put(keyStr, new Integer(getRowCount()));
			_rowidToKey.add(keyStr);
		}

		return true;
	}

	public void remove(String key)
	{
		removeRow(key);
	}

	public void removeRow(String key)
	{
		int row = getRowNumberForPkValue(key);
		removeRow(row);
	}

	public synchronized void removeRow(int rowId)
	{
		int maxRowId = _rows.size() - 1;

		// Get the key for this rowId
		String key = _rowidToKey.get(rowId);

		if (_logger.isDebugEnabled())
			_logger.debug(_name+": removeRow(rowId="+rowId+"): key="+key);

		// Removes the row from listOfRows
		_rows.remove(rowId);

		// Remove the rowId->key
		_rowidToKey.remove(rowId);

		// remove the key from the hash map
		_keysToRowid.remove(key);

		// if we delete in the middle of a list, we need to
		// change the rowId's for the rest of the list
		if (rowId < maxRowId )
		{
			for (int r=rowId; r<_rows.size(); r++)
			{
				// Get the key for row
				// rows has moved when "_rowidToKey.remove(rowId)" was done
				key = _rowidToKey.get(r);

				// Set the new rowId for the PrimaryKey
				_keysToRowid.put(key, new Integer(r)) ;
			}
			
		}
	}

	public synchronized void removeAllRows()
	{
		// Removes the row from listOfRows
		if (_rows != null) 
			_rows.clear();

		// Remove the rowId->key
		if (_rowidToKey != null) 
			_rowidToKey.clear();

		// remove the key from the hash map
		if (_keysToRowid != null) 
			_keysToRowid.clear();
	}


	

	public void setPk(String key, int row)
	{
		_keysToRowid.put(key, new Integer(row));

		while ( _rowidToKey.size() <= row )
		{
			_rowidToKey.add("-setPk:uninitialized-");
		}
		_rowidToKey.set(row, key);
	}

	public List<List<Object>> getDataCollection()
	{
		return _rows;
	}

	public List<Object> getRow(String key)
	{
		int row = getRowNumberForPkValue(key);
		if (row == -1)
			return null;

		return getRow(row);
	}

	public List<Object> getRow(int row)
	{
		return _rows.get(row);
	}
	

	/**
	 * [FIXME] Describe me
	 * 
	 * @param diff
	 * @param isDiffCol
	 * @param isPctCol
	 * @return
	 */
	static public SamplingCnt computeRatePerSec(SamplingCnt diff, boolean[] isDiffCol, boolean[] isPctCol) 
	{
		// Initialize result structure
		SamplingCnt rate  = new SamplingCnt(diff, false, diff._name+"-rate");

		// - Loop on all rows in the DIFF structure
		// - Do calculations on them
		// - And add them to the RATE structure
		for (int rowId=0; rowId < diff.getRowCount(); rowId++) 
		{
			// Get the row from the DIFF structure
			List<Object> diffRow = diff.getRow(rowId);

			// Create a new ROW "structure" for each row in the DIFF
			List<Object> newRow = new ArrayList<Object>();

			for (int i=0; i<diff.getColumnCount(); i++) 
			{
				// Get the RAW object from the DIFF structure
				Object originObject = diffRow.get(i);

				// If the below IF statements is not true... keep the same object
				Object newObject    = originObject;

				// If PCT column DO nothing.
				if ( isPctCol[i] ) 
				{
				}
				// If this is a column that has DIFF calculation.
				else if ( isDiffCol[i] ) 
				{
					double val = 0;

					// What to do if we CANT DO DIVISION
					if (diff.interval == 0)
						newObject = "N/A";

					// Calculate rate
					if (originObject instanceof Number)
					{
						// Get the object as a Double value
						if ( originObject instanceof Number )
							val = ((Number)originObject).doubleValue();
						else
							val = Double.parseDouble( originObject.toString() );

						// interval is in MilliSec, so val has to be multiplied by 1000
						val = (val * 1000) / diff.interval;
						BigDecimal newVal = new BigDecimal( val ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

						// Set the new object
						newObject = newVal;
					}
					// Unsupported columns, skip the calculation
					else
					{
						String colName = diff.getColumnName(i);
						_logger.warn("CounterSampleSetName='"+diff._name+"', className='"+originObject.getClass().getName()+"' columns can't be 'rate' calculated. colName='"+colName+"', originObject='"+originObject+"', keeping this object.");
						newObject = originObject;
					}
				}

				// set the data in the new row
				newRow.add(newObject);

			} // end: row loop

			rate.addRow(newRow);

		} // end: all rows loop
		
		return rate;
	}	
	
//	public SamplingCnt computeDiffCnt(SamplingCnt newSample)
//	{
//		return SamplingCnt.computeDiffCnt(this, newSample);
//	}

	// computeDiffCnt : generate a new SamplingCnt, with computed differences on some columns
	// bimapColsCalcDiff : Vector of Integers. For each col : 0 if not to compute, 1 if col must be computed
//	static public SamplingCnt computeDiffCnt(SamplingCnt oldSample, SamplingCnt newSample, int idKey1, int idKey2, int idKey3, Vector bitmapColsCalcDiff)
	/**
	 * [FIXME] Describe me
	 */
	static public SamplingCnt computeDiffCnt(SamplingCnt oldSample, SamplingCnt newSample, List<Integer> deletedRows, List<String> pkList, boolean[] isDiffCol)
	{
		// Initialize result structure
		SamplingCnt diffCnt = new SamplingCnt(newSample, false, newSample._name+"-diff");

		long newTsMilli      = newSample.samplingTime.getTime();
		long oldTsMilli      = oldSample.samplingTime.getTime();
		int newTsNano        = newSample.samplingTime.getNanos();
		int oldTsNano        = oldSample.samplingTime.getNanos();

		// Check if TsMilli has really ms precision (not the case before JDK 1.4)
		if ((newTsMilli - (newTsMilli / 1000) * 1000) == newTsNano / 1000000)
			// JDK > 1.3.1
			diffCnt.interval = newTsMilli - oldTsMilli;
		else
			diffCnt.interval = newTsMilli - oldTsMilli + (newTsNano - oldTsNano) / 1000000;

		List<Object> newRow;
		List<Object> oldRow;
		List<Object> diffRow;
		int oldRowId;

		if (diffCnt._colIsPk == null)
		{
			// Special case, only one row for each sample, no key
			oldRow = oldSample._rows.get(0);
			newRow = newSample._rows.get(0);
			diffRow = new ArrayList<Object>();
			for (int i = 0; i < newSample.getColumnCount(); i++)
			{
				diffRow.add(new Integer(((Integer) (newRow.get(i))).intValue() - ((Integer) (oldRow.get(i))).intValue()));
			}
			diffCnt._rows.add(diffRow);
			return diffCnt;
		}

		// Keep a array of what rows that we access of the old values
		// this will help us find out what rows we "deleted" from the previous to the new sample
		// or actually rows that are no longer available in the new sample...
		boolean oldSampleAccessArr[] = new boolean[oldSample.getRowCount()]; // default values: false

		// Loop on all rows from the NEW sample
		for (int newRowId = 0; newRowId < newSample.getRowCount(); newRowId++)
		{
			newRow = newSample.getRow(newRowId);
			diffRow = new ArrayList<Object>();
			
			// get PK of the new row
			String newPk = newSample.getPkValue(newRowId);

			// Retreive old same row
			oldRowId = oldSample.getRowNumberForPkValue(newPk);
			
			// if old Row EXISTS, we can do diff calculation
			if (oldRowId != -1)
			{
				// Mark the row as "not deleted" / or "accessed"
				if (oldRowId >= 0 && oldRowId < oldSampleAccessArr.length)
					oldSampleAccessArr[oldRowId] = true;
				
				// Old row found, compute the diffs
				oldRow = oldSample.getRow(oldRowId);
				for (int i = 0; i < newSample.getColumnCount(); i++)
				{
					if ( ! isDiffCol[i] )
						diffRow.add(newRow.get(i));
					else
					{
						//checkType(oldSample, oldRowId, i, newSample, newRowId, i);
						//if ((newRow.get(i)).getClass().toString().equals("class java.math.BigDecimal"))
						Object oldRowObj = oldRow.get(i);
						Object newRowObj = newRow.get(i);

						String colName = newSample.getColumnName(i);

						if ( newRowObj instanceof Number )
						{
							Number diffValue = diffColumnValue((Number)oldRowObj, (Number)newRowObj, diffCnt._negativeDiffCountersToZero, newSample._name, colName);
							diffRow.add(diffValue);
						}
						else
						{
							_logger.warn("CounterSampleSetName='"+newSample._name+"', className='"+newRowObj.getClass().getName()+"' columns can't be 'diff' calculated. colName='"+colName+"', key='"+newPk+"', oldObj='"+oldRowObj+"', newObj='"+newRowObj+"'.");
							diffRow.add(newRowObj);
						}
					}
				}
			} // end: old row was found
			else
			{
				// Row was NOT found in previous sample, which means it's a "new" row for this sample.
				// So we do not need to do DIFF calculation, just add the raw data...
				for (int i = 0; i < newSample.getColumnCount(); i++)
				{
					diffRow.add(newRow.get(i));
				}
			}

			diffCnt.addRow(diffRow);

		} // end: row loop
		
		// What rows was DELETED from previous sample.
		// meaning, rows in the previous sample that was NOT part of the new sample.
		if (deletedRows != null)
		{
			for (int i=0; i<oldSampleAccessArr.length; i++)
			{
				if (oldSampleAccessArr[i] == false)
				{
					deletedRows.add(i);
				}
			}
		}

		return diffCnt;
	}

	/**
	 * Do difference calculations newColVal - prevColVal
	 * @param prevColVal previous sample value
	 * @param newColVal current/new sample value
	 * @param negativeDiffCountersToZero if the counter is less than 0, reset it to 0
	 * @param counterSetName Used as a prefix for messages
	 * @return the difference of the correct subclass of Number
	 */
	private static Number diffColumnValue(Number prevColVal, Number newColVal, boolean negativeDiffCountersToZero, String counterSetName, String colName)
	{
		Number diffColVal = null;

//if (counterSetName.startsWith(SummaryPanel.CM_NAME))
//{
//	System.out.println(counterSetName+":   > colName="+StringUtil.left(colName,20)+", prevColVal="+prevColVal );
//	System.out.println(counterSetName+":     colName="+StringUtil.left(colName,20)+", newColVal ="+newColVal );
//}

		if (newColVal instanceof BigDecimal)
		{
			diffColVal = new BigDecimal(newColVal.doubleValue() - prevColVal.doubleValue());
			if (diffColVal.doubleValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = new BigDecimal(0);
		}
		else if (newColVal instanceof Byte)
		{
			diffColVal = new Byte((byte) (newColVal.byteValue() - prevColVal.byteValue()));
			if (diffColVal.intValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = new Byte("0");
		}
		else if (newColVal instanceof Double)
		{
			diffColVal = new Double(newColVal.doubleValue() - prevColVal.doubleValue());
			if (diffColVal.doubleValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = new Double(0);
		}
		else if (newColVal instanceof Float)
		{
			diffColVal = new Float(newColVal.floatValue() - prevColVal.floatValue());
			if (diffColVal.floatValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = new Float(0);
		}
		else if (newColVal instanceof Integer)
		{
			diffColVal = new Integer(newColVal.intValue() - prevColVal.intValue());
			if (diffColVal.intValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = new Integer(0);
		}
		else if (newColVal instanceof Long)
		{
			diffColVal = new Long(newColVal.longValue() - prevColVal.longValue());
			if (diffColVal.longValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = new Long(0);
		}
		else if (newColVal instanceof Short)
		{
			diffColVal = new Short((short) (newColVal.shortValue() - prevColVal.shortValue()));
			if (diffColVal.shortValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = new Short("0");
		}
		else if (newColVal instanceof AtomicInteger)
		{
			diffColVal = new AtomicInteger(newColVal.intValue() - prevColVal.intValue());
			if (diffColVal.intValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = new AtomicInteger(0);
		}
		else if (newColVal instanceof AtomicLong)
		{
			diffColVal = new AtomicLong(newColVal.longValue() - prevColVal.longValue());
			if (diffColVal.longValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = new AtomicLong(0);
		}
		else
		{
			_logger.warn(counterSetName+": failure in diffColumnValue(colName='"+colName+"', prevColVal='"+prevColVal+"', newColVal='"+newColVal+"'), with prevColVal='"+prevColVal.getClass().getName()+"', newColVal='"+newColVal.getClass().getName()+"'. Returning the new value instead.");
			return newColVal;
		}

		return diffColVal;
	}

	
	/**
	 * Merge column values...
	 * NOTE: this could fail with overflow...
	 * @param prevColVal
	 * @param thisColVal
	 * @return
	 */
	private Number mergeColumnValue(Number prevColVal, Number thisColVal)
	{
		Number mergeColVal = null;

		if      (thisColVal instanceof BigDecimal)
		{
			mergeColVal = new BigDecimal(prevColVal.doubleValue() + thisColVal.doubleValue());
		}
		else if (thisColVal instanceof Byte)
		{
			mergeColVal = new Byte((byte) (prevColVal.byteValue() + thisColVal.byteValue()));
		}
		else if (thisColVal instanceof Double)
		{
			mergeColVal = new Double(prevColVal.doubleValue() + thisColVal.doubleValue());
		}
		else if (thisColVal instanceof Float)
		{
			mergeColVal = new Float(prevColVal.floatValue() + thisColVal.floatValue());
		}
		else if (thisColVal instanceof Integer)
		{
			mergeColVal = new Integer(prevColVal.intValue() + thisColVal.intValue());
		}
		else if (thisColVal instanceof Long)
		{
			mergeColVal = new Long(prevColVal.longValue() + thisColVal.longValue());
		}
		else if (thisColVal instanceof Short)
		{
			mergeColVal = new Short((short) (prevColVal.shortValue() + thisColVal.shortValue()));
		}
		else if (thisColVal instanceof AtomicInteger)
		{
			mergeColVal = new AtomicInteger(prevColVal.intValue() + thisColVal.intValue());
		}
		else if (thisColVal instanceof AtomicLong)
		{
			mergeColVal = new AtomicLong(prevColVal.longValue() + thisColVal.longValue());
		}
		else
		{
			_logger.warn(_name+": failure in mergeColumnValue(prevColVal='"+prevColVal+"', thisColVal='"+thisColVal+"'), with prevColVal='"+prevColVal.getClass().getName()+"', thisColVal='"+thisColVal.getClass().getName()+"'. Returning the origin value instead.");
			return prevColVal;
		}
		return mergeColVal;
	}
	
	
	public static boolean isDiffAllowedForDatatype(String datatype)
	{
		boolean enabled = false;
		if      ("tinyint"          .equalsIgnoreCase(datatype)) enabled = true;
		else if ("unsigned smallint".equalsIgnoreCase(datatype)) enabled = true;
		else if ("smallint"         .equalsIgnoreCase(datatype)) enabled = true;
		else if ("unsigned int"     .equalsIgnoreCase(datatype)) enabled = true;
		else if ("int"              .equalsIgnoreCase(datatype)) enabled = true;
		else if ("unsigned bigint"  .equalsIgnoreCase(datatype)) enabled = true;
		else if ("bigint"           .equalsIgnoreCase(datatype)) enabled = true;
		else if ("decimal"          .equalsIgnoreCase(datatype)) enabled = true;
		else if ("numeric"          .equalsIgnoreCase(datatype)) enabled = true;
		else if ("float"            .equalsIgnoreCase(datatype)) enabled = true;
		else if ("real"             .equalsIgnoreCase(datatype)) enabled = true;
		else if ("double precision" .equalsIgnoreCase(datatype)) enabled = true;

		return enabled;
	}

}

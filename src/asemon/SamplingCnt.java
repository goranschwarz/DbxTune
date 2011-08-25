/**
*/


package asemon;

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
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;

import asemon.gui.MainFrame;

public  class SamplingCnt 
extends AbstractTableModel
{
    private static final long serialVersionUID = 4786911833477005253L;

    /** Log4j logging. */
	private   static Logger _logger	    = Logger.getLogger(SamplingCnt.class);
	private   boolean       _negativeDiffCountersToZero = true;
	protected String        _name         = null; // Used for debuging
	private   Vector        _colNames     = null;
	private   Vector        _colSqlType   = null;
	private   Vector        _colClassName = null;
	private   boolean[]     _colIsPk      = null; // one boolean for each column
	private   int[]         _pkPosArray   = null; // one boolean for each column
	private   Vector        _rows         = null;

	/** List containes key<String>. position in List is the rowId */
	private ArrayList     _rowidToKey     = null; 

	/** Hash contaning <Integer>, which is the rowId for this key<String> */
	private Hashtable     _keysToRowid  = null;

	public  Timestamp     samplingTime;
	public  long	      interval      = 0;
	private int           _nbRows       = 0;
	private int           _nbCols       = 0;

	public SamplingCnt(String name, boolean negativeDiffCountersToZero)
	{
		_name = name;
		_negativeDiffCountersToZero = negativeDiffCountersToZero;
	}
	private SamplingCnt(SamplingCnt sc, boolean cloneRows, String name)
	{
		_name                       = name;
		_nbCols                     = sc._nbCols;
		_nbRows                     = sc._nbRows;
		_keysToRowid                = sc._keysToRowid;
		_colNames                   = sc._colNames;
		_colSqlType                 = sc._colSqlType;
		_colClassName               = sc._colClassName;
		_colIsPk                    = sc._colIsPk;
		_pkPosArray                 = sc._pkPosArray;
		_rowidToKey                 = sc._rowidToKey;
		samplingTime                = sc.samplingTime;
		interval                    = sc.interval;
		_negativeDiffCountersToZero = sc._negativeDiffCountersToZero;

		if (cloneRows)
		{
			_rows = (Vector) sc._rows.clone();
		}
		else
		{
			_keysToRowid = new Hashtable();
			_rowidToKey  = new ArrayList();
			_rows        = new Vector();
		}

		_nbRows      = _rows.size();
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
		//return _nbRows;
		return _rows.size();
	}

	public int getColumnCount()
	{
		return _nbCols;
	}

	public String getColumnName(int colid)
	{
		if (_colNames == null || colid == -1 || colid >= _nbCols)
			return null;
		return (String) _colNames.get(colid);
	}

	public Class getColumnClass(int colid)
	{
		return Object.class;
	}

	public boolean isCellEditable(int i, int j)
	{
		return false;
    }

	public Object getValueAt(int row, int col)
	{
		if (row < 0 | col < 0) 
			return null;

		int counter = 0;
		while (true)
		{
			try
			{
				return ((Vector)_rows.get(row)).get(col);
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				counter++;
				if (counter >= 10 )
					return null;
				// This probably happens due to GetCounters thread modifies 
				// the Vector at the same time as AWT-EventQueue thread refreshes 
				// filters/cellRendering or does something...
				_logger.warn("GetValueAt(row="+row+", col="+col+"): ArrayIndexOutOfBoundsException, retry counter="+counter+"...");
				//Thread.yield();
				try {Thread.sleep(3);}
				catch (InterruptedException ignore) {}
			}
			catch (NullPointerException e)
			{
				counter++;
				if (counter >= 10 )
					return null;
				// This probably happens due to GetCounters thread modifies 
				// the Vector at the same time as AWT-EventQueue thread refreshes 
				// filters/cellRendering or does something...
				_logger.warn("GetValueAt(row="+row+", col="+col+"): NullPointerException, retry counter="+counter+"...");
				//Thread.yield();
				try {Thread.sleep(3);}
				catch (InterruptedException ignore) {}
			}
		}
	}

	public void setValueAt(Object value, int row, int col)
	{
		((Vector)_rows.get(row)).set(col, value);
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


	

	public Vector getColNames()
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
		return (String)_colClassName.get(colId);
	}

	public int getColSqlType(int colId)
	{
		return ((Integer)_colSqlType.get(colId)).intValue();
	}

	public String getPkValue(int row)
	{
		return (String) _rowidToKey.get(row);
	}
	public int getRowNumberForPkValue(String pkStr)
	{
		Object o = _keysToRowid.get(pkStr);
		if (o == null)
		{
			//throw new RuntimeException("Couldn't find pk '"+pkStr+"'.");
			return -1;
		}
		int i = ((Integer)o).intValue();

		if (_logger.isDebugEnabled())
			_logger.debug(_name+": "+i+" = getRowNumberForPkValue(pk='"+pkStr+"')");

		return i;
	}

	/** colId starts at 0 */
	public boolean isColPartOfPk(int colId)
	{
		if (_colIsPk == null)
			return false;

		if (colId < 0 || colId > _nbCols )
			return false; // or should we: throw new IndexOutOfBoundsException("description");

		return _colIsPk[colId];
	}

	private void checkWarnings(Statement st) 
	throws SQLException
	{
		boolean hasWarning = false;
		try
		{
			SQLWarning w = st.getWarnings();
			while (w != null)
			{
				hasWarning = true;
				_logger.warn("SamplingCnt. Warning : " + w);
				w = w.getNextWarning();
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("SamplingCnt.getWarnings : " + ex);
			ex.printStackTrace();
		}
		if (hasWarning)
		{
			throw new SQLException("SQL Warning");
		}
	}

	private Object fixNullValue(int col)
	{
		if (_colSqlType == null)
		{
			_logger.error("colSqlType are null, this should not happen.");
			return new Object();
		}
		int objSqlType = (int) ((Integer) _colSqlType.get(col - 1)).intValue();
	  
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
		case java.sql.Types.NUMERIC:      return new Double(0);
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

	public boolean getCnt(CountersModel cm, Connection conn, String sql, List pkList)
	throws SQLException
	{
		try
		{
			String sendSql = "select getdate() \n" + sql;

			Statement stmnt = conn.createStatement();
			ResultSet rs;

			stmnt.setQueryTimeout(10); // XX seconds query timeout

			_nbRows = 0;
			_rows   = new Vector();

			if (_logger.isTraceEnabled())
			{
				_logger.trace("Sending SQL: "+sendSql);
			}

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

			return true;
		}
		catch (SQLException sqlEx)
		{
			_logger.warn("SamplingCnt.getCnt : " + sqlEx.getErrorCode() + " " + sqlEx.getMessage() + ". SQL: "+sql, sqlEx);
			if (sqlEx.getMessage().equals("JZ0C0: Connection is already closed."))
			{
				GetCounters.setRefreshing(false);
				MainFrame.terminateConnection();
			}
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

	private boolean readResultset(ResultSet rs, ResultSetMetaData rsmd, List pkList, int rsNum)
	throws SQLException
	{
		if (_colNames == null)
		{
			// Initialize column names
			_colNames     = new Vector();
			_colSqlType   = new Vector();
			_colClassName = new Vector();
			_nbCols       = rsmd.getColumnCount();
			_colIsPk      = new boolean[_nbCols];

			_keysToRowid = new Hashtable();
			_rowidToKey  = new ArrayList();

			List tmpPkPos = new LinkedList();

			for (int i=1; i<=_nbCols; i++)
			{
				//String colname = rsmd.getColumnName(i); 
				String colname = rsmd.getColumnLabel(i); 

				_colNames    .add(colname);
				_colSqlType  .add(new Integer(rsmd.getColumnType(i)));
				_colClassName.add(rsmd.getColumnClassName(i));

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
			}
			
			if (pkList != null && (pkList.size() != tmpPkPos.size()) )
			{
				throw new RuntimeException("sample, cant find all the primary keys in the resultset. pkList='"+pkList+"', _colNames='"+_colNames+"', tmpPkPos='"+tmpPkPos+"'.");
			}
				
			_pkPosArray = new int[tmpPkPos.size()];
			for(int i= 0; i<tmpPkPos.size(); i++)
			{
				_pkPosArray[i] = ((Integer)tmpPkPos.get(i)).intValue();
            }
			tmpPkPos = null;
		}
		else // check priv result set for match
		{
			int cols = rsmd.getColumnCount();
			if (_nbCols != cols)
			{
				_logger.error("Resultset number "+rsNum+" has "+cols+" while it was expected to have "+_nbCols+". Skipping this result set.");
				rs.close();
				return false;
			}

			// Check data types match for all columns.
			for (int i=1; i<=cols; i++)
			{
				String oldType = (String)_colClassName.get(i-1);
				String newType = rsmd.getColumnClassName(i);
				if ( ! oldType.equals(newType) )
				{
					_logger.error("Resultset number "+rsNum+" column number "+i+" has sql datatype "+newType+", while we expected datatype "+oldType+".  Skipping this result set.");
					rs.close();
					return false;
				}
			}
			
		}

		// Load counters in memory
		int rsRowNum = 0;
		Vector row;
		Object val;
		String key;
		_logger.debug("---");
		while (rs.next())
		{
			// Get one row
			key = "";
			row = new Vector();
			_rows.add(row);
			for (int i = 1; i <= _nbCols; i++)
			{
				val = rs.getObject(i);
				if (rsRowNum == 0 && _logger.isDebugEnabled() )
					_logger.trace("READ_RESULTSET(rsnum "+rsNum+", row 0): col=" + i + ", colName=" + (_colNames.get(i - 1) + "                                   ").substring(0, 25) + ", ObjectType=" + (val == null ? "NULL-VALUE" : val.getClass().getName()));

				if (val == null)
				{
					val = fixNullValue(i);
				}
				row.add(val);

				// If this column is part of the PrimaryKey
				if (_colIsPk != null && _colIsPk[i-1] )
				{
					if (val != null)
					{
						key = key.concat(val.toString());
						key = key.concat(":");
					}
					else
						_logger.warn("Key containes NULL value, rsNum="+rsNum+", row="+rsRowNum+", col="+i+".");
				}
			}
			if (_logger.isTraceEnabled())
			{
				for (int c=0; c<_nbCols; c++)
				{
					_logger.trace("   > rsNum="+rsNum+", rsRowNum="+rsRowNum+", _nbRows="+_nbRows+", c="+c+", className='"+row.get(c).getClass().getName()+"', value="+row.get(c));
				}
			}

			// save HKEY with corresponding row
			if (_colIsPk != null)
			{
				_keysToRowid.put(key, new Integer(_nbRows));
				_rowidToKey.add(key);
				if (_logger.isTraceEnabled())
				{
					_logger.trace("   >> key='"+key+"', rowId="+_nbRows+", _rowidToKey.addPos="+(_rowidToKey.size()-1));
				}
			}

			rsRowNum++;
			_nbRows++;
		}
		_nbRows = _rows.size();
		return true;
	}
	
	/** remake new row id's. This since we have made deletions of  */
	public void newRowIds()
	{
		// Update the hashtable (since rowid's have changed)
		_keysToRowid = new Hashtable();
		_rowidToKey  = new ArrayList();

		for (int r=0; r<_rows.size(); r++)
		{
			Vector row = (Vector) _rows.get(r);
			String key = "";
			for (int c=0; c<_nbCols; c++)
			{
				// If this column is part of the PrimaryKey
				if (_colIsPk != null && _colIsPk[c] )
				{
					Object val = row.get(c);
					if (val != null)
					{
						key = key.concat(val.toString());
						key = key.concat(":");
					}
				}
			}
			if (_colIsPk != null)
			{
				_keysToRowid.put(key, new Integer(r));
				_rowidToKey.add(key);
			}
		}
	}

	public boolean addRow(Vector row)
	{
		if (row == null)
			return false;

		if (row.size() != _nbCols)
		{
			throw new IndexOutOfBoundsException("The number of columns in current structure is "+_nbCols+", while the row we attempt to add has "+row.size()+" columns."); 
		}
		// Load counters in memory
		Object val;
		String key = "";

		key = "";

		// Fix null values 
		// fix PK and ROWID
		for (int c=0; c<_nbCols; c++)
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
				{
					key = key.concat(val.toString());
					key = key.concat(":");
				}
				else
					_logger.warn("Key containes NULL value, row="+_nbRows+", col="+c+".");
			}
		}

		// Check for DUPLICATES
		if (_colIsPk != null)
		{
			Integer io = (Integer) _keysToRowid.get(key);
			if (io != null)
			{
				Vector curRow = getRow(key);
				_logger.error("Duplicate key, a row for the key '"+key+"' already exists. CurrentRow='"+curRow+"'. NewRow='"+row+"'.");
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
			_keysToRowid.put(key, new Integer(_nbRows));
			_rowidToKey.add(key);
		}

		_nbRows = _rows.size();
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
		String key = (String) _rowidToKey.get(rowId);

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
				key = (String)_rowidToKey.get(r);

				// Set the new rowId for the PrimaryKey
				_keysToRowid.put(key, new Integer(r)) ;
			}
			
		}

		_nbRows = _rows.size();
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

	/**
	 * Get value
	 * @param row starts at 0
	 * @param col starts at 0
	 * @return
	 */
//	public Object getValue(int row, int col)
//	{
//		// row : 0.._nbRows-1
//		// col : 1.._nbCols
//		// System.out.println ("SamplingCnt.getValue(row="+row+",
//		// col="+col+")");
//		try
//		{
//			if ((row < 0) || (row >= _nbRows))
//			{
//				_logger.error("Bad row number : " + row);
//				System.exit(1);
//			}
//
//			if ((col < 0) || (col >= _nbCols))
//			{
//				_logger.error("Bad col number : " + col);
//				System.exit(1);
//			}
//			Vector aRow = (Vector) _rows.get(row);
//			return aRow.elementAt(col);
//		}
//		catch (Throwable e)
//		{
//			return null;
//		}
//	}

	public Vector getDataVector()
	{
		return _rows;
	}

	public Vector getRow(String key)
	{
		int row = getRowNumberForPkValue(key);
		if (row == -1)
			return null;

		return getRow(row);
	}

	public Vector getRow(int row)
	{
		return (Vector)_rows.get(row);
	}
	

  // Debug function
//  static private void checkType (SamplingCnt oldcnt, int oldrowid, int oldcol,
//  	SamplingCnt newcnt, int newrowid, int newcol)
//   {
//      Vector oldrow = (Vector)oldcnt._rows.get(oldrowid);
//      Object oldval = oldrow.get(oldcol);
//      Vector newrow = (Vector)newcnt._rows.get(newrowid);
//      Object newval = newrow.get(newcol);
//      if ((oldval.getClass().toString().equals("class java.lang.Integer"))&&
//      	(newval.getClass().toString().equals("class java.lang.Integer")) )
//       return;
//
//      // Not Integer, so why ????
//      System.out.println ("BAD SamplingCnt :");
//      System.out.println ("  OLD : class = "+oldval.getClass().toString());
//      System.out.println ("  OLD :Cols :"+oldcnt._colNames);
//      System.out.println ("  OLD :_nbRows="+oldcnt._nbRows+" size="+oldcnt._rows.size()+ " curentRow="+oldrowid+" currentCol="+oldcol);
//
//      System.out.println ("  NEW : class = "+newval.getClass().toString());
//      System.out.println ("  NEW :Cols :"+newcnt._colNames);
//      System.out.println ("  NEW :_nbRows="+newcnt._nbRows+" size="+newcnt._rows.size()+ " curentRow="+newrowid+" currentCol="+newcol);
//
//  }


//	public SamplingCnt computeRatePerSec() 
//	{
//		return SamplingCnt.computeRatePerSec(this);
//	}

	static public SamplingCnt computeRatePerSec (SamplingCnt diff, boolean[] isDiffCol, boolean[] isPctCol) 
	{
		// Initialize result structure
		SamplingCnt rate  = new SamplingCnt(diff, false, "rate");
//		rate._nbCols      = diff._nbCols;
//		rate._nbRows      = diff._nbRows;
//		rate._rows        = new Vector();
//		rate._keysToRowid = diff._keysToRowid;
//		rate._colNames    = diff._colNames;
//		rate._colSqlType  = diff._colSqlType;
//		rate._colClassName= diff._colClassName;
//		rate._colIsPk     = diff._colIsPk;
//		rate._pkPosArray  = diff._pkPosArray;
//		rate._rowidToKey  = diff._rowidToKey;
//		rate.samplingTime = diff.samplingTime;
//		rate.interval     = diff.interval;

	    // - Loop on all rows in the DIFF structure
		// - Do calculations on them
		// - And add them to the RATE structure
		for (int rowId=0; rowId < diff.getRowCount(); rowId++) 
		{
			// Get the row from the DIFF structure
		    Vector diffRow = (Vector)diff.getRow(rowId);

		    // Create a new ROW "structure" for each row in the DIFF
		    Vector newRow = new Vector();

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
	static public SamplingCnt computeDiffCnt(SamplingCnt oldSample, SamplingCnt newSample, List pkList, boolean[] isDiffCol)
	{
		String key = new String("");
		//Object val=null;

		// Initialize result structure
		SamplingCnt diffCnt = new SamplingCnt(newSample, false, "diff");
//		diffCnt._nbCols      = newSample._nbCols;
//		diffCnt._nbRows      = newSample._nbRows;
//		diffCnt._rows        = new Vector();
//		diffCnt._keysToRowid = new Hashtable();
//		diffCnt._colNames    = newSample._colNames;
//		diffCnt._colSqlType  = newSample._colSqlType;
//		diffCnt._colClassName= newSample._colClassName;
//		diffCnt._colIsPk     = newSample._colIsPk;
//		diffCnt._pkPosArray  = newSample._pkPosArray;
//		diffCnt._rowidToKey  = newSample._rowidToKey;
//		diffCnt.samplingTime = newSample.samplingTime;

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

		Vector newRow;
		Vector oldRow;
		Vector diffRow;
		int oldRowId;

		if (diffCnt._colIsPk == null)
		{
			// Special case, only one row for each sample, no key
			oldRow = (Vector) oldSample._rows.get(0);
			newRow = (Vector) newSample._rows.get(0);
			diffRow = new Vector();
			for (int i = 0; i < newSample._nbCols; i++)
			{
				diffRow.add(new Integer(((Integer) (newRow.get(i))).intValue() - ((Integer) (oldRow.get(i))).intValue()));
			}
			diffCnt._rows.add(diffRow);
			return diffCnt;
		}

		// Loop on all new rows
		for (int newRowId = 0; newRowId < newSample._nbRows; newRowId++)
		{
			newRow = newSample.getRow(newRowId);
			diffRow = new Vector();
			//      key = newRow.get(idKey1-1).toString();
			//      if (idKey2 != 0)  key = key.concat(newRow.get(idKey2-1).toString());
			//      if (idKey3 != 0)  key = key.concat(newRow.get(idKey3-1).toString());
//			key = newRow.get(idKey1 - 1) + "";
//			if (idKey2 != 0) key = key.concat(newRow.get(idKey2 - 1) + "");
//			if (idKey3 != 0) key = key.concat(newRow.get(idKey3 - 1) + "");

			key = newSample.getPkValue(newRowId);
//			for (int i=0; i<diffCnt._pkPosArray.length; i++)
//			{
//				Object o = newRow.get(i);
//				if (o != null)
//				{
//					key = key.concat( o.toString() );
//					key = key.concat(":");
//				}
//			}

			// save HKEY with corresponding  row
//			diffCnt.keys.put(key, new Integer(newRowId));
//			diffCnt.setPk(key, newRowId);

			// Retreive old same row
//			Integer r = (Integer) oldSample.keys.get(key);
//			if (r != null)
//				oldRowId = r.intValue();
//			else
//				oldRowId = -1;

			oldRowId = oldSample.getRowNumberForPkValue(key);
			
			if (oldRowId != -1)
			{
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
						if ( newRowObj instanceof BigDecimal || newRowObj instanceof Long )
						{
							//long diff = ((java.math.BigDecimal)(newRow.get(i))).longValue() - ((java.math.BigDecimal)(oldRow.get(i))).longValue();
							long prevSample = ((Number)oldRowObj).longValue();
							long thisSample = ((Number)newRowObj).longValue();
							long diff = thisSample - prevSample;
							//System.out.println("BigDecimal: i="+i+", diff="+diff+", prevSample="+prevSample+", thisSample="+thisSample);
							if (diff < 0)
							{
								if (diffCnt._negativeDiffCountersToZero)
								{
									diff = 0; // force negatives numbers (bad samples) to 0
								}
							}
							diffRow.add(new Long(diff));
						}
						else
						{
							//               int diff = ((Integer)(newRow.get(i))).intValue() - ((Integer)(oldRow.get(i))).intValue();
							int prevSample = ((Integer)oldRowObj).intValue();
							int thisSample = ((Integer)newRowObj).intValue();
							int diff = thisSample - prevSample;
							//System.out.println("Integer: i="+i+", diff="+diff+", prevSample="+prevSample+", thisSample="+thisSample);
							if (diff < 0)
							{
								if (diffCnt._negativeDiffCountersToZero)
								{
									diff = 0; // force negatives numbers (bad samples) to 0
								}
							}
							diffRow.add(new Integer(diff));
						}
					}
				}
			} // end: old row was found
			else
			{
				// Old row not found, save current counters
				for (int i = 0; i < newSample._nbCols; i++)
				{
					diffRow.add(newRow.get(i));
				}
			}

			diffCnt.addRow(diffRow);

		} // end: row loop
		return diffCnt;
	}

}

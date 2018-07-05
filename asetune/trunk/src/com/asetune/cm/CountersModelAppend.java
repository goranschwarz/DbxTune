/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.cm;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.asetune.DbxTune;
import com.asetune.ICounterController;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sql.conn.DbxConnection;


public class CountersModelAppend
    extends CountersModel
{
	/** Log4j logging. */
	private static Logger	   _logger	          = Logger.getLogger(CountersModelAppend.class);
	private static final long	serialVersionUID	= 1L;

	private List<List<Object>> _data    = null;
	private List<String>       _cols    = null;
	private Timestamp _thisSamplingTime = null;
	private Timestamp _prevSamplingTime = null;
	private long      _interval         = 0;

	@SuppressWarnings("unused")
	private boolean   _newRows          = false;

	private boolean   _initialized      = false;

	public CountersModelAppend(
		ICounterController counterController,
		String name, 
		String groupName, 
		String sql, 
		String[] monTables, 
		String[] dependsOnRole, 
		String[] dependsOnConfig, 
		int dependsOnVersion, 
		int dependsOnCeVersion, 
		boolean systemCm)
	{
		super(counterController, name, groupName, sql, null, null, null, monTables, dependsOnRole, dependsOnConfig, dependsOnVersion, dependsOnCeVersion, true, systemCm);

		setDataSource(DATA_ABS, false);
	}

	@Override
	public boolean isDataInitialized()
	{
		return _initialized;
	}
	
	public boolean saveSampleToFile()
	{
		return false;
	}

//	@Override
//	public List<String> getColNames()
//	{
//		return _cols;
//	}
//
//	@Override
//	public List<List<Object>> getDataCollection()
//	{
//		return _data;
//	}

	@Override
	public List<List<Object>> getDataCollection(int whatData)
	{
		List<List<Object>> data = null;

		if      (whatData == DATA_ABS)  data = _data;
		else if (whatData == DATA_DIFF) data = _data;
		else if (whatData == DATA_RATE) data = _data;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		return data;
	}

	@Override
	public synchronized List<String> getColNames(int whatData)
	{
		List<String> data = null;

		if      (whatData == DATA_ABS)  data = _cols;
		else if (whatData == DATA_DIFF) data = _cols;
		else if (whatData == DATA_RATE) data = _cols;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		return data;
	}
	
	/*---------------------------------------------------
	** BEGIN: implementing TableModel or overriding CounterModel/AbstractTableModel
	**---------------------------------------------------
	*/
	@Override
	public int getColumnCount()
	{
		int c = 0;
		if (isDataInitialized() && _cols != null)
			c = _cols.size();
		return c;
    }

	@Override
	public String getColumnName(int col)
	{
		if (isDataInitialized() && _cols != null)
			return (String) _cols.get(col);
		return null;
	}

	@Override
	public int getRowCount()
	{
		int c = 0;
		if (isDataInitialized() && _data != null)
			c = _data.size();
		return c;
    }

	@Override
	public Object getValueAt(int row, int col)
	{
		if (!isDataInitialized())   return null;

		List<Object> rowList = _data.get(row);
		if (rowList == null) return null;
		return rowList.get(col);
    }

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return false;
    }

	@Override
	public int findColumn(String colName)
	{
		if (isDataInitialized() && _cols != null)
		{
			for (int i=0; i<_cols.size(); i++)
			{
				if ( colName.equals(getColumnName(i)) )
					return i;
			}
		}
		return -1;
	}

	/*---------------------------------------------------
	** END: implementing TableModel or overriding CounterModel/AbstractTableModel
	**---------------------------------------------------
	*/

	@Override public boolean discardDiffPctHighlighterOnAbsTable() { return true; }
	@Override public boolean isDiffCalcEnabled() { return false; }
	@Override public boolean isDiffColumn(int index) { return false; }
	@Override public boolean isPctColumn (int index) { return false; }

	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
	}
	
	@Override public boolean hasAbsData() { return _data != null; }

//	@Override public void    setPersistCountersAbs(boolean b) {}
	@Override public boolean isPersistCountersAbsEnabled() { return true; }
	@Override public boolean isPersistCountersAbsEditable(){ return false; }
	
//	@Override public void    setPersistCountersDiff(boolean b) {}
	@Override public boolean isPersistCountersDiffEnabled() { return false; }
	@Override public boolean isPersistCountersDiffEditable(){ return false; }
	
//	@Override public void    setPersistCountersRate(boolean b) {}
	@Override public boolean isPersistCountersRateEnabled() { return false; }
	@Override public boolean isPersistCountersRateEditable(){ return false; }

	@Override public boolean getDefaultIsPersistCountersEnabled()     { return false; }
	@Override public boolean getDefaultIsPersistCountersAbsEnabled()  { return true; }
	@Override public boolean getDefaultIsPersistCountersDiffEnabled() { return false; }
	@Override public boolean getDefaultIsPersistCountersRateEnabled() { return false; }
	
	

	private void checkWarnings(Statement st) 
	throws Exception
	{
		boolean hasWarning = false;
		try
		{
			SQLWarning w = st.getWarnings();
			while (w != null)
			{
				hasWarning = true;
				_logger.warn("CounterSample. Warning : " + w);
				w = w.getNextWarning();
			}
		}
		catch (Exception ex)
		{
			_logger.warn("CounterSample.getWarnings : " + ex);
			ex.printStackTrace();
		}
		if (hasWarning)
		{
			throw new Exception("SQL Warning");
		}
		return;
	}	

	private int _nbRows = 0;
	private int _nbCols = 0;


	private int getCnt(Connection cnx, String sql)
	throws Exception
	{
		if (sql == null)
			sql = getSql() + getSqlWhere();

		Statement stmt = cnx.createStatement();
		ResultSet rs;
		ResultSetMetaData rsmd;
		rs = stmt.executeQuery("select getdate() " + sql);
		checkWarnings(stmt);
		rs.next();
		if (_thisSamplingTime != null)
			_prevSamplingTime = _thisSamplingTime;
		_thisSamplingTime = (rs.getTimestamp(1));

		if (_prevSamplingTime != null && _thisSamplingTime != null)
		{
			_interval = _thisSamplingTime.getTime() - _prevSamplingTime.getTime();
		}

		rs.next();
		stmt.getMoreResults();
		checkWarnings(stmt);
		rs = stmt.getResultSet();
		rsmd = rs.getMetaData();

		if (_cols == null)
		{
			// Initialize column names
			_cols = new ArrayList<String>();
			_nbCols = rsmd.getColumnCount();
			for (int i = 1; i <= _nbCols; i++)
			{
				_cols.add(rsmd.getColumnName(i));
			}
		}

		// Initialize data structure
		if ( _data == null )
		{
			_nbRows = 0;
			_data = new ArrayList<List<Object>>();
		}

		// Load counters in memory
		List<Object> row = null;
		Object val;

		_logger.debug("---");
		while (rs.next())
		{
			_newRows = true;

			// Get one row
			row = new ArrayList<Object>();
			_data.add(row);
			for (int i = 1; i <= _nbCols; i++)
			{
				val = rs.getObject(i);
				
				if (_logger.isDebugEnabled())
				{
					if (_data.size() == 1)
						_logger.debug("READ_RESULTSET(row 0): col=" + i + ", colName=" + (_cols.get(i - 1) + "                                   ").substring(0, 25) + ", ObjectType=" + (val == null ? "NULL-VALUE" : val.getClass().getName()));
				}
				row.add(val);
			}
			_nbRows++;
		}
		rs.close();
		_logger.debug("Number of rows in '"+getName()+"': "+_nbRows);
		
		if (row == null)
			return -1;
		return row.size();
	}



	@Override
	protected int refreshGetData(DbxConnection conn) throws Exception
	{
		if (_logger.isDebugEnabled())
			_logger.debug("Entering refresh() method for " + getName());

		if (conn == null)
			return -1;

		int rowsFetched = getCnt(conn, null);

		// Update dates on panel
		TabularCntrPanel tabPanel = getTabPanel();
		if (tabPanel != null)
			tabPanel.setTimeInfo(null, getSampleTimeHead(), _thisSamplingTime, _interval);

		if ( DbxTune.hasGui() )
		{
			Runnable doWork = new Runnable()
			{
				@Override
				public void run()
				{
					beginGuiRefresh();

					if ( ! _initialized )
					{
						_logger.debug(getName()+":------doFireTableStructureChanged------");
						fireTableStructureChanged();

						_initialized = true;
					}

					if (getTabPanel() != null && !getTabPanel().isTableInitialized())
					{
						_logger.debug(getName()+":------doFireTableStructureChanged------");
						fireTableStructureChanged();
						getTabPanel().adjustTableColumnWidth();
					}
					else
					{
						_logger.debug(getName()+":-fireTableDataChanged-");
						fireTableDataChanged();
					}
					
					endGuiRefresh();
				}
			};
			// Invoke this job on the SWING Event Dispather Thread
			if ( ! SwingUtilities.isEventDispatchThread() )
				SwingUtilities.invokeLater(doWork);
			else
				doWork.run();
		}
		
		return rowsFetched;
	}

	/**
	 * NO PK is needed, we are NOT going to do DIFF calculations
	 */
	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		// NO PK is needed, we are NOT going to do DIFF calculations
		return null;
	}

	@Override
	public void clearForRead()
	{
		_nbRows = 0;
		_data = new ArrayList<List<Object>>();
	}

	@Override
	public void clear()
	{
		clear(100);
	}
	@Override
	public synchronized void clear(int clearLevel)
	{
		_initialized = false;

		_thisSamplingTime = null;
		_prevSamplingTime = null;
		_interval         = 0;

		// Clear dates on panel
		TabularCntrPanel tabPanel = getTabPanel();
		if (tabPanel != null)
			tabPanel.reset();

//		System.out.println(_name+":------doFireTableStructureChanged------");
		fireTableStructureChanged();
	}


	// Return the value of a cell (rowId, ColumnName)
	@Override
	public synchronized Timestamp getTimestamp()
	{
		return _thisSamplingTime;
	}
	
}

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.cm;

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

import asemon.Asemon;
import asemon.gui.TabularCntrPanel;

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
	private boolean   _newRows          = false;
	private boolean   _initialized      = false;

	public CountersModelAppend(
		String name, 
		String sql, 
		String[] monTables, 
		String[] dependsOnRole, 
		String[] dependsOnConfig, 
		int dependsOnVersion, 
		int dependsOnCeVersion, 
		boolean systemCm)
	{
		super(name, sql, null, null, null, monTables, dependsOnRole, dependsOnConfig, dependsOnVersion, dependsOnCeVersion, true, systemCm);

		setDataSource(DATA_ABS);
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

	@Override
	public boolean isDiffCalcEnabled() { return false; }
	@Override
	public boolean showAbsolute()      { return true; }
	@Override
	public boolean isDiffColumn(int index) { return false; }
	@Override
	public boolean isPctColumn (int index) { return false; }

	@Override
	public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
	{
	}
	
	@Override
	public boolean hasAbsData() { return _data != null; }

	@Override
	public void    setPersistCountersAbs(boolean b) {}
	@Override
	public boolean isPersistCountersAbsEnabled() { return true; }
	@Override
	public boolean isPersistCountersAbsEditable(){ return false; }
	
	@Override
	public void    setPersistCountersDiff(boolean b) {}
	@Override
	public boolean isPersistCountersDiffEnabled() { return false; }
	@Override
	public boolean isPersistCountersDiffEditable(){ return false; }
	
	@Override
	public void    setPersistCountersRate(boolean b) {}
	@Override
	public boolean isPersistCountersRateEnabled() { return false; }
	@Override
	public boolean isPersistCountersRateEditable(){ return false; }

	

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
				_logger.warn("SamplingCnt. Warning : " + w);
				w = w.getNextWarning();
			}
		}
		catch (Exception ex)
		{
			_logger.warn("SamplingCnt.getWarnings : " + ex);
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
//		try
//		{
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
			_logger.debug("Number of rows in '"+getName()+"': "+_nbRows);
			
			if (row == null)
				return -1;
			return row.size();
//		}
//		catch (SQLException sqlEx)
//		{
//			_logger.warn("getCnt : " + sqlEx.getErrorCode() + " " + sqlEx.getMessage());
//			if (sqlEx.getMessage().equals("JZ0C0: Connection is already closed."))
//			{
//				GetCounters.setRefreshing(false);
//				MainFrame.terminateConnection();
//			}
//			return false;
//		}
//		catch (Exception ev)
//		{
//			_logger.error("getCnt : " + ev);
//			return false;
//		}
	}



	@Override
	protected int refreshGetData(Connection conn) throws Exception
	{
		if (_logger.isDebugEnabled())
			_logger.debug("Entering refresh() method for " + getName());

//		if ( ! swingRefreshOK )
//		{
//			// a swing refresh is in process, don't break the data, so wait for
//			// the next loop
//			if (_logger.isDebugEnabled())
//				_logger.debug("Exit refresh() method for " + getName() + ". Swingrefresh already processing.");
//			return -1;
//		}

		if (conn == null)
			return -1;

		int rowsFetched = getCnt(conn, null);

//		if ( ! _initialized && _data != null )
//			setupTM();
//
//		updateTM();

		// Update dates on panel
		TabularCntrPanel tabPanel = getTabPanel();
		if (tabPanel != null)
			tabPanel.setTimeInfo(null, _thisSamplingTime, _interval);

		if ( Asemon.hasGUI() )
		{
			Runnable doWork = new Runnable()
			{
				public void run()
				{
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

//	private void setupTM()
//	{
//		Runnable setupTM_inSwingThread = new Runnable()
//		{
//			public void run()
//			{
//				setupTM_code();
//			}
//		};
//		swingRefreshOK = false;
//		SwingUtilities.invokeLater(setupTM_inSwingThread);
//	}

//	private void setupTM_code()
//	{
//		TabularCntrPanel tabPanel = getTabPanel();
//
//		if (tabPanel == null)
//			return;
//
//		try
//		{
//			if (_logger.isDebugEnabled())
//			{
//				_logger.debug("Entering setupTM_code() method for " + getName());
//			}
//			
////			tabPanel.setEnableCounterChoice(false);
////			tabPanel.setEnableFilter(false);
//
////			TM.setDataVector(_data, _cols);
////jre6			sorter.setTableModel(TM);
//
//			// Update dates on panel
//			tabPanel.setTimeInfo(null, _thisSamplingTime, 0);
////			tabPanel.absDateTxt.setText(MainFrame.summaryPanel.getCountersCleared());
////			tabPanel.intDateTxt.setText(_thisSamplingTime.toString());
//			if (_logger.isDebugEnabled())
//			{
//				_logger.debug("Leaving setupTM_code() method for " + getName() );
//			}
//			
//			_initialized = true;
//		}
//		finally
//		{
//			// Clear dates on panel
//			if (tabPanel != null)
//			{
//				tabPanel.setTimeInfo(null, null, 0);
////				tabPanel.absDateTxt.setText("");
////				tabPanel.intDateTxt.setText("");
////				tabPanel.intervalTxt.setText("");
////
////				tabPanel.calcColumnWidths();
//			}
//			
//			swingRefreshOK = true;
//		}
//	}


//	public synchronized void updateTM()
//	{
//		Runnable updateTM_inSwingThread = new Runnable()
//		{
//			public void run()
//			{
//				updateTM_code();
//			}
//		};
//
//		if ( _data != null && isSwingRefreshOK() )
//		{
//			swingRefreshOK = false;
//			SwingUtilities.invokeLater(updateTM_inSwingThread);
//		}
//		else
//		{
//			if (getTabPanel() != null)
//			{
//				getTabPanel().setWatermark();
//			}
//
//			if ( _data == null ) 
//			{
//				GetCounters.setWaitEvent("Data has not yet been initialized.");
////				MainFrame.statusFld.setText("Wait... Data has not yet been initialized.");
//			}
//			else if ( isSwingRefreshOK() )
//			{
//				GetCounters.setWaitEvent("Swing Refresh is not yet true.");
////				MainFrame.statusFld.setText("Wait... Swing Refresh is not yet true.");
//			}
//		}
//	}

//	private void updateTM_code()
//	{
//		// Used to refresh the JTable associated with this TABLEMODEL
//		TabularCntrPanel tabPanel = getTabPanel();
//
//		if (_logger.isDebugEnabled())
//			_logger.debug("Entering updateTM_code() method for " + getName());
//
//		try
//		{
//			// Update dates on panel
//			tabPanel.setTimeInfo(null, _thisSamplingTime, _interval);
////			tabPanel.absDateTxt.setText(MainFrame.summaryPanel.getCountersCleared());
////			tabPanel.intDateTxt.setText(_thisSamplingTime.toString());
////			tabPanel.intervalTxt.setText(Integer.toString((int) _interval));
//		}
//		finally
//		{
//			swingRefreshOK = true;
//			if (_newRows)
//			{
////jre6				sorter.setTableModel(TM);
//			}
//
//			if (tabPanel != null)
//			{
////				tabPanel.calcColumnWidths();
//				tabPanel.setWatermark();
//			}
//		}
//	}


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


//	private void clearCM_code()
//	{
//		_initialized = false;
//
////		if (TM != null)
////		{
////			TM.setRowCount(0);
////		}
//
//		_thisSamplingTime = null;
//		_prevSamplingTime = null;
//		_interval         = 0;
//
//		// Clear dates on panel
//		TabularCntrPanel tabPanel = getTabPanel();
//		if (tabPanel != null)
//		{
//			tabPanel.reset();
////			tabPanel.absDateTxt.setText("");
////			tabPanel.intDateTxt.setText("");
////			tabPanel.intervalTxt.setText("");
////			tabPanel.RBAbsolute.setSelected(true);
//		}
//	}

	// Return the value of a cell (rowId, ColumnName)
	@Override
	public synchronized Timestamp getTimestamp()
	{
		return _thisSamplingTime;
	}
	
}

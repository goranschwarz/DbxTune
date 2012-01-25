/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Vector;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import asemon.gui.MainFrame;
import asemon.gui.TabularCntrPanel;

public class CountersModelAppend
    extends CountersModel
{
	/** Log4j logging. */
	private static Logger	   _logger	          = Logger.getLogger(CountersModelAppend.class);

	private Vector    _data             = null;
	private Vector    _cols             = null;
	private Timestamp _thisSamplingTime = null;
	private Timestamp _prevSamplingTime = null;
	private long      _interval         = 0;
	private boolean   _newRows          = false;
	private boolean   _initialized      = false;

	public CountersModelAppend(String nm, String sql, String[] monTables)
	{
		//super(nm, sql, tp, 0, 0, 0, null, null, monTables);
		super(nm, sql, null, null, null, monTables, true);

//		displayMode = "ABS";
		setDataSource(DATA_ABS);
	}

	public boolean isDataInitialized()
	{
		return _initialized;
	}
	
	public boolean saveSampleToFile()
	{
		return false;
	}

	public Vector getDataVector()
	{
		return _data;
	}

	public synchronized Vector getColNames()
	{
		return _cols;
	}

	public Vector getDataVector(int whatData)
	{
		Vector data = null;

		if      (whatData == DATA_ABS)  data = _data;
		else if (whatData == DATA_DIFF) data = _data;
		else if (whatData == DATA_RATE) data = _data;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		return data;
	}

	public synchronized Vector getColNames(int whatData)
	{
		Vector data = null;

		if      (whatData == DATA_ABS)  data = _cols;
		else if (whatData == DATA_DIFF) data = _cols;
		else if (whatData == DATA_RATE) data = _cols;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		return data;
	}
	
	public boolean showAbsolute()
	{
		return true;
	}

	public boolean isDiffColumn(int index)
	{
		return false;
	}

	public boolean isPctColumn(int index)
	{
		return false;
	}

	public void localCalculation()
	{
	}
	
	public boolean hasAbsData()
	{
		return _data != null;
	}

	

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


	private boolean getCnt(Connection cnx, String sql)
	{
		try
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
				_cols = new Vector();
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
				_data = new Vector();
			}

			// Load counters in memory
			Vector row;
			Object val;

			_logger.debug("---");
			while (rs.next())
			{
				_newRows = true;

				// Get one row
				row = new Vector();
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
			return true;
		}
		catch (SQLException sqlEx)
		{
			_logger.warn("getCnt : " + sqlEx.getErrorCode() + " " + sqlEx.getMessage());
			if (sqlEx.getMessage().equals("JZ0C0: Connection is already closed."))
			{
				GetCounters.setRefreshing(false);
				MainFrame.terminateConnection();
			}
			return false;
		}
		catch (Exception ev)
		{
			_logger.error("getCnt : " + ev);
			return false;
		}
	}

	
	
	public synchronized void refresh() throws Exception
	{
		refresh(MainFrame.getMonConnection());
	}
	public synchronized void refresh(Connection conn) throws Exception
	{
		if (_logger.isDebugEnabled())
			_logger.debug("Entering refresh() method for " + getName());

		if ( ! swingRefreshOK )
		{
			// a swing refresh is in process, don't break the data, so wait for
			// the next loop
			if (_logger.isDebugEnabled())
				_logger.debug("Exit refresh() method for " + getName() + ". Swingrefresh already processing.");
			return;
		}

		if (conn == null)
			return;

		getCnt(conn, null);
		
//		if ( ! _initialized && TM != null )
			setupTM();

		updateTM();
	}

	private void setupTM()
	{
		Runnable setupTM_inSwingThread = new Runnable()
		{
			public void run()
			{
				setupTM_code();
			}
		};
		swingRefreshOK = false;
		SwingUtilities.invokeLater(setupTM_inSwingThread);
	}

	private void setupTM_code()
	{
		TabularCntrPanel tabPanel = getTabPanel();

		if (tabPanel == null)
			return;

		try
		{
			if (_logger.isDebugEnabled())
			{
				_logger.debug("Entering setupTM_code() method for " + getName());
			}
			
			tabPanel.setEnableCounterChoice(false);
			tabPanel.setEnableFilter(false);

//			TM.setDataVector(_data, _cols);
//jre6			sorter.setTableModel(TM);

			// Update dates on panel
			tabPanel.setTimeInfo(null, _thisSamplingTime, 0);
//			tabPanel.absDateTxt.setText(MainFrame.summaryPanel.getCountersCleared());
//			tabPanel.intDateTxt.setText(_thisSamplingTime.toString());
			if (_logger.isDebugEnabled())
			{
				_logger.debug("Leaving setupTM_code() method for " + getName() );
			}
			
			_initialized = true;
		}
		finally
		{
			// Clear dates on panel
			if (tabPanel != null)
			{
				tabPanel.setTimeInfo(null, null, 0);
//				tabPanel.absDateTxt.setText("");
//				tabPanel.intDateTxt.setText("");
//				tabPanel.intervalTxt.setText("");
//
//				tabPanel.calcColumnWidths();
			}
			
			swingRefreshOK = true;
		}
	}


	public synchronized void updateTM()
	{
		Runnable updateTM_inSwingThread = new Runnable()
		{
			public void run()
			{
				updateTM_code();
			}
		};

		if ( _data != null && isSwingRefreshOK() )
		{
			swingRefreshOK = false;
			SwingUtilities.invokeLater(updateTM_inSwingThread);
		}
		else
		{
			if (getTabPanel() != null)
			{
				getTabPanel().setWatermark();
			}

			if ( _data == null ) 
			{
				GetCounters.setWaitEvent("Data has not yet been initialized.");
//				MainFrame.statusFld.setText("Wait... Data has not yet been initialized.");
			}
			else if ( isSwingRefreshOK() )
			{
				GetCounters.setWaitEvent("Swing Refresh is not yet true.");
//				MainFrame.statusFld.setText("Wait... Swing Refresh is not yet true.");
			}
		}
	}

	private void updateTM_code()
	{
		// Used to refresh the JTable associated with this TABLEMODEL
		TabularCntrPanel tabPanel = getTabPanel();

		if (_logger.isDebugEnabled())
			_logger.debug("Entering updateTM_code() method for " + getName());

		try
		{
			// Update dates on panel
			tabPanel.setTimeInfo(null, _thisSamplingTime, _interval);
//			tabPanel.absDateTxt.setText(MainFrame.summaryPanel.getCountersCleared());
//			tabPanel.intDateTxt.setText(_thisSamplingTime.toString());
//			tabPanel.intervalTxt.setText(Integer.toString((int) _interval));
		}
		finally
		{
			swingRefreshOK = true;
			if (_newRows)
			{
//jre6				sorter.setTableModel(TM);
			}

			if (tabPanel != null)
			{
//				tabPanel.calcColumnWidths();
				tabPanel.setWatermark();
			}
		}
	}


	public void clear()
	{
		clear(100);
	}
	public synchronized void clear(int clearLevel)
	{
		Runnable clearCM_inSwingThread = new Runnable()
		{
			public void run()
			{
				clearCM_code();
			}
		};

		swingRefreshOK = false;
		SwingUtilities.invokeLater(clearCM_inSwingThread);
	}


	private void clearCM_code()
	{
		_initialized = false;

//		if (TM != null)
//		{
//			TM.setRowCount(0);
//		}

		_thisSamplingTime = null;
		_prevSamplingTime = null;
		_interval         = 0;

		// Clear dates on panel
		TabularCntrPanel tabPanel = getTabPanel();
		if (tabPanel != null)
		{
			tabPanel.reset();
//			tabPanel.absDateTxt.setText("");
//			tabPanel.intDateTxt.setText("");
//			tabPanel.intervalTxt.setText("");
//			tabPanel.RBAbsolute.setSelected(true);
		}
	}

	// Return the value of a cell (rowId, ColumnName)
	public synchronized Timestamp getTimestamp()
	{
		return _thisSamplingTime;
	}
	
}
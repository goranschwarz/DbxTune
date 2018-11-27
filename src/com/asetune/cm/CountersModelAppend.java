/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.cm;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;


public class CountersModelAppend
    extends CountersModel
{
	/** Log4j logging. */
//	private static Logger	   _logger	          = Logger.getLogger(CountersModelAppend.class);
	private static final long	serialVersionUID	= 1L;

	private List<List<Object>> _lastRefreshData = new ArrayList<>();
	private List<List<Object>> _allData         = new ArrayList<>();

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

	public static final String  PROPKEY_showAllRecords = "<CMNAME>.showAllRecords";
	public static final boolean DEFAULT_showAllRecords = true;
	public boolean showAllRecords()
	{
		String propName = PROPKEY_showAllRecords.replace("<CMNAME>", getName());
		return Configuration.getCombinedConfiguration().getBooleanProperty(propName, DEFAULT_showAllRecords);
	}
	public void setShowAllRecords(boolean b)
	{
		String propName = PROPKEY_showAllRecords.replace("<CMNAME>", getName());
		
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf != null)
		{
			conf.setProperty(propName, b);
			conf.save();
		}
	}
	
	/**
	 * This is used to get <i>LAST</i> data, can for example be used by the Persistent Counter Storage to "push" data for last sample
	 * @return
	 */
	public List<List<Object>> getDataCollectionForLastRefresh()
	{
		return _lastRefreshData;
	}

	
	/*---------------------------------------------------
	** BEGIN: implementing TableModel or overriding CounterModel/AbstractTableModel
	**---------------------------------------------------
	*/
	@Override
	public int getRowCount()
	{
		// Offline mode is not handled here
		if (isOfflineConnected())
			return super.getRowCount();

		int c = 0;

		if (isDataInitialized())
			c = showAllRecords() ? _allData.size() : _lastRefreshData.size();
		
		return c;
    }

	@Override
	public Object getValueAt(int row, int col)
	{
		// Offline mode is not handled here
		if (isOfflineConnected())
			return super.getValueAt(row, col);

		if (!isDataInitialized())   
			return null;

		List<Object> rowList = showAllRecords() ? _allData.get(row) : _lastRefreshData.get(row);
		if (rowList == null) 
			return null;

		return rowList.get(col);
    }
	/*---------------------------------------------------
	** END: implementing TableModel or overriding CounterModel/AbstractTableModel
	**---------------------------------------------------
	*/

	@Override public boolean discardDiffPctHighlighterOnAbsTable() { return true; }
	@Override public boolean isDiffCalcEnabled()     { return false; }
	@Override public boolean isDiffColumn(int index) { return false; }
	@Override public boolean isPctColumn (int index) { return false; }

	@Override public boolean hasAbsData() 
	{
		if (isOfflineConnected())
			return super.hasAbsData();
		
		return getRowCount() > 0; 
	}

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

	
	// The below can be used if we want to handle OFFLINE data in this CM (right now it's done by the Parent)
//	@Override
//	public void setOfflineValueAt(int type, Object value, int row, int col)
//	{
//		// TODO Auto-generated method stub
//		super.setOfflineValueAt(type, value, row, col);
//System.out.println("APPEND: setOfflineValueAt(type="+type+", row="+row+", col="+col+", value="+value);
//	}
	


//	/**
//	 * get the data a bit earlier than after 'super.refreshGetData(conn)' <br>
//	 * Since this is called from inside super.refreshGetData(conn), and not (in GUI maode after SwingUtilities.invokeLater(...) ) <br>
//	 * When we have "fixed" direct assign of: _prevSample=tmpNewSample, _newSample=tmpNewSample, _diffData=tmpDiffData, _rateData=tmpRateData we can use "attempt 1"
//	 */
//	@Override
//	public void localCalculation(CounterSample newSample)
//	{
//		// get last sample... as "last sample"
//		_lastRefreshData = newSample.getDataCollection();
//
//		// add all in this sample to "allData"
//		if (_lastRefreshData != null)
//			_allData.addAll(_lastRefreshData);
//
////System.out.println("APPEND("+Thread.currentThread().getName()+"): _lastRefreshData.size()="+(_lastRefreshData==null?null:_lastRefreshData.size())+", _allData.size()="+_allData.size());
//	}

//-------- BEGIN attempt 1 ------------------
// Note: if we change 'super.refreshGetData(conn)' to assign following values SYNCRONUS in the methos and not with: SwingUtilities.invokeLater(...)
//	_prevSample = tmpNewSample;
//	_newSample  = tmpNewSample;
//	_diffData   = tmpDiffData;
//	_rateData   = tmpRateData;
//
//	/**
//	 * Use parent to get most work done
//	 */
	@Override
	protected int refreshGetData(DbxConnection conn) throws Exception
	{
		// Call super to get all records...
		int superRows = super.refreshGetData(conn);
//System.out.println("APPEND: super.refreshGetData(conn): superRows="+superRows);

		// get last sample... as "last sample"
		_lastRefreshData = super.getDataCollection(DATA_ABS); // HERE is where we fail, since super.refreshGetData() assignes internal variables in SwingUtilities.invokeLater(...)

		// add all in this sample to "allData"
		if (_lastRefreshData != null)
			_allData.addAll(_lastRefreshData);

//System.out.println("APPEND("+Thread.currentThread().getName()+"): _lastRefreshData.size()="+(_lastRefreshData==null?null:_lastRefreshData.size())+", _allData.size()="+_allData.size());
		fireTableDataChanged();

		return superRows;
	}
//-------- END attempt 1 ------------------

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
	public synchronized void clear(int clearLevel)
	{
		super.clear(clearLevel);

		_allData         = new ArrayList<>();
		_lastRefreshData = new ArrayList<>();
	}
	
	@Override
	public void reset()
	{
		super.reset();
		clear(); // Should clear() be called here, or should we move some fields from clear method ???
	}
}

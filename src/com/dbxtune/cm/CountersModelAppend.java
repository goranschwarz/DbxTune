/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.cm;

import java.util.ArrayList;
import java.util.List;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;


public class CountersModelAppend
    extends CountersModel
{
	/** Log4j logging. */
	private static final long	serialVersionUID	= 1L;

	private List<List<Object>> _lastRefreshData = new ArrayList<>();
	private List<List<Object>> _allData         = new ArrayList<>();

	public CountersModelAppend(
		ICounterController counterController,
		IGuiController     guiController,
		String name, 
		String groupName, 
		String sql, 
		String[] monTables, 
		String[] dependsOnRole, 
		String[] dependsOnConfig, 
		long dependsOnVersion, 
		long dependsOnCeVersion, 
		boolean systemCm)
	{
		super(counterController, guiController, name, groupName, sql, null, null, null, monTables, dependsOnRole, dependsOnConfig, dependsOnVersion, dependsOnCeVersion, true, systemCm);

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

	@Override public boolean discardDiffHighlighterOnAbsTable() { return true; }
	@Override public boolean discardPctHighlighterOnAbsTable()  { return true; }
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
	


	// Keep data in 2 places
	//   1: last refresh --- keeps records in just the LAST sample
	//   2: all data     --- appand just sampled data to a "summary" list
	@Override
	public void hookInNearEndOfRefreshGetData()
	{
		// get last sample... as "last sample"
		_lastRefreshData = getDataCollection(DATA_ABS);

		// add all in this sample to "allData"
		if (_lastRefreshData != null)
			_allData.addAll(_lastRefreshData);

//System.out.println("APPEND("+Thread.currentThread().getName()+"): _lastRefreshData.size()="+(_lastRefreshData==null?null:_lastRefreshData.size())+", _allData.size()="+_allData.size());
	}


	/**
	 * NO PK is needed, we are NOT going to do DIFF calculations
	 */
//	@Override
//	public List<String> getPkForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
//	{
//		// NO PK is needed, we are NOT going to do DIFF calculations
//		return null;
//	}
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// NO PK is needed, we are NOT going to do DIFF calculations
		return null;
	}

	@Override
	public synchronized void clear(ClearOption... clearOptions)
	{
		super.clear(clearOptions);

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

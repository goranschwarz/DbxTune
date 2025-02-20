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
package com.dbxtune.cm.rs;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.dbxtune.CounterController;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.DbxTuneResultSetMetaData;
import com.dbxtune.cm.NoValidRowsInSample;
import com.dbxtune.sql.conn.DbxConnection;

public class CounterSampleAdminStatsModule
extends CounterSample
{
	private static final long    serialVersionUID = 1L;

	public CounterSampleAdminStatsModule(String name, boolean negativeDiffCountersToZero, String[] diffColNames, CounterSample prevSample)
	{
		super(name, negativeDiffCountersToZero, diffColNames, prevSample);
	}
	
	@Override
	public boolean getSample(CountersModel cm, DbxConnection conn, String sql, List<String> pkList) 
	throws SQLException, NoValidRowsInSample
	{
		// Get the CmAdminStats which holds all the counter which a Module is based on
//		CmAdminStats cmAdminStats = (CmAdminStats) CounterController.getInstance().getCmByName(CmAdminStats.CM_NAME);
		CmAdminStats cmAdminStats = (CmAdminStats) cm.getCounterController().getCmByName(CmAdminStats.CM_NAME);
//		if ( cmAdminStats.getColumnCount() == 0 )
//			return false;

		DbxTuneResultSetMetaData xrstm = new DbxTuneResultSetMetaData();
		
		// Get Data for this specific module
		String forModule = ((CmAdminStatsAbstract)cm).getModuleName();
		List<List<Object>> rows = cmAdminStats.getModuleCounters(forModule, xrstm); // NOTE: THIS IS WHERE IT ALL HAPPENS

		if (rows.size() == 0)
		//	return false;
			throw new NoValidRowsInSample("Could not find any records of type '"+forModule+"' in CM '"+CmAdminStats.SHORT_NAME+"'.");
		
		// Now set MetaData information...
		setColumnNames (xrstm.getColumnNames());
		setSqlType     (xrstm.getSqlTypes());
		setSqlTypeNames(xrstm.getSqlTypeNames());
		setColClassName(xrstm.getClassNames());

		xrstm.setPkCol(cm.getPk());
		
		List<String> diffColNames = new ArrayList<String>(xrstm.getColumnNames());
		diffColNames.remove("Instance");
		diffColNames.remove("InstanceId");
		cm.setDiffColumns(diffColNames);

		setPkColArray(xrstm.getPkColArray());

		initPkStructures();

		if ( ! cm.hasResultSetMetaData() )
			cm.setResultSetMetaData(xrstm);


		// Reuse the sample time from CmAdminStats
		setSampleTime(cmAdminStats.getSampleTime());
		setSampleInterval(cmAdminStats.getSampleInterval());
		
		// Finally add the rows
		for (List<Object> row : rows)
		{
			addRow(cm, row);
		}
		
		return true;
	}
}

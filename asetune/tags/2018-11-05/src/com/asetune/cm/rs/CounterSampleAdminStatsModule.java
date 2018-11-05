package com.asetune.cm.rs;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.asetune.CounterController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CountersModel;
import com.asetune.cm.DbxTuneResultSetMetaData;
import com.asetune.cm.NoValidRowsInSample;
import com.asetune.sql.conn.DbxConnection;

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
		CmAdminStats cmAdminStats = (CmAdminStats) CounterController.getInstance().getCmByName(CmAdminStats.CM_NAME);
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
			addRow(row);
		}
		
		return true;
	}
}

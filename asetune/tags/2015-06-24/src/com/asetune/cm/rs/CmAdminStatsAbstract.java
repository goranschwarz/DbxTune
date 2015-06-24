package com.asetune.cm.rs;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import com.asetune.cm.CounterSample;
import com.asetune.cm.CountersModel;
import com.asetune.cm.rs.RsStatCounterDictionary.StatCounterEntry;

public abstract class CmAdminStatsAbstract 
extends CountersModel
{
	private static final long serialVersionUID = 1L;

	private String _moduleName = "";
	
	public CmAdminStatsAbstract(String name, String groupName, String sql, List<String> pkList, String[] diffColumns, String[] pctColumns, String[] monTables, String[] dependsOnRole, String[] dependsOnConfig, int dependsOnVersion, int dependsOnCeVersion, boolean negativeDiffCountersToZero, boolean systemCm, int defaultPostponeTime)
	{
		super(name, groupName, sql, pkList, diffColumns, pctColumns, monTables, dependsOnRole, dependsOnConfig, dependsOnVersion, dependsOnCeVersion, negativeDiffCountersToZero, systemCm, defaultPostponeTime);
	}

	protected void setModuleName(String moduleName)
	{
		_moduleName = moduleName;
	}

	protected String getModuleName()
	{
		return _moduleName;
	}

	
//	@Override
//	public List<String> getDependsOnCm()
//	{
//		return super.getDependsOnCm();
//	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("Instance");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return "";
	}

	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		return new CounterSampleAdminStatsModule(name, negativeDiffCountersToZero, diffColumns, prevSample);
	}

	@Override
	public String getToolTipTextOnTableColumnHeader(String colName)
	{
		if (RsStatCounterDictionary.hasInstance())
		{
			RsStatCounterDictionary dict = RsStatCounterDictionary.getInstance();
			
			StatCounterEntry ce = dict.getCounter(_moduleName, colName);
			if (ce != null)
				return ce.getToolTipText();
		}

		// Revert back to the super if we can't find it in the RsStatCounterDictionary
		return super.getToolTipTextOnTableColumnHeader(colName);
	}
}

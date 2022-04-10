/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.cm.rs;

import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CountersModel;
import com.asetune.cm.rs.RsStatCounterDictionary.StatCounterEntry;
import com.asetune.sql.conn.DbxConnection;

public abstract class CmAdminStatsAbstract 
extends CountersModel
{
	private static final long serialVersionUID = 1L;

	private String _moduleName = "";
	
	public CmAdminStatsAbstract(ICounterController counterController, String name, String groupName, String sql, List<String> pkList, String[] diffColumns, String[] pctColumns, String[] monTables, String[] dependsOnRole, String[] dependsOnConfig, long dependsOnVersion, long dependsOnCeVersion, boolean negativeDiffCountersToZero, boolean systemCm, int defaultPostponeTime)
	{
		super(counterController, name, groupName, sql, pkList, diffColumns, pctColumns, monTables, dependsOnRole, dependsOnConfig, dependsOnVersion, dependsOnCeVersion, negativeDiffCountersToZero, systemCm, defaultPostponeTime);
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
	public List<String> getPkForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("Instance");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
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

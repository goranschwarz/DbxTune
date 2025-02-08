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

import java.util.LinkedList;
import java.util.List;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.rs.RsStatCounterDictionary.StatCounterEntry;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;

public abstract class CmAdminStatsAbstract 
extends CountersModel
{
	private static final long serialVersionUID = 1L;

	private String _moduleName = "";
	
	public CmAdminStatsAbstract(ICounterController counterController, IGuiController guiController, String name, String groupName, String sql, List<String> pkList, String[] diffColumns, String[] pctColumns, String[] monTables, String[] dependsOnRole, String[] dependsOnConfig, long dependsOnVersion, long dependsOnCeVersion, boolean negativeDiffCountersToZero, boolean systemCm, int defaultPostponeTime)
	{
		super(counterController, guiController, name, groupName, sql, pkList, diffColumns, pctColumns, monTables, dependsOnRole, dependsOnConfig, dependsOnVersion, dependsOnCeVersion, negativeDiffCountersToZero, systemCm, defaultPostponeTime);
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
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("Instance");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
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

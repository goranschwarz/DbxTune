/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.cm.sqlserver;

import com.asetune.cache.SqlAgentJobInfoCache;
import com.asetune.cm.CounterSample;
import com.asetune.utils.Configuration;

public class SqlServerCmUtils
{
	public static final String  PROPKEY_context_info_str_enabled = "SqlServerTune.CmAny.context_info_str.enable";
	public static final boolean DEFAULT_context_info_str_enabled = true;

	public static final String HELPTEXT_howToEnable__context_info_str  = "Enable with property: " + PROPKEY_context_info_str_enabled + " = true";
	public static final String HELPTEXT_howToDisable__context_info_str = "Disable with property: " + PROPKEY_context_info_str_enabled + " = false";
	
	public static boolean isContextInfoStrEnabled()
	{
		return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_context_info_str_enabled, DEFAULT_context_info_str_enabled);
	}
	

	
	public static final String  PROPKEY_resolve_sqlAgentProgramName = "SqlServerTune.CmAny.resolve.sqlAgent.programName.enable";
	public static final boolean DEFAULT_resolve_sqlAgentProgramName = true;
	
	/**
	 * Loop all rows in the passed CM and resolve SQL Agent Job/Step id's into real names
	 * 
	 * @param newSample
	 */
	public static void localCalculation_resolveSqlAgentProgramName(CounterSample newSample)
	{
		localCalculation_resolveSqlAgentProgramName(newSample, "program_name");
	}

	/**
	 * Loop all rows in the passed CM and resolve SQL Agent Job/Step id's into real names
	 * 
	 * @param newSample
	 */
	public static void localCalculation_resolveSqlAgentProgramName(CounterSample newSample, String colname)
	{
		boolean isEnabled = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_resolve_sqlAgentProgramName, DEFAULT_resolve_sqlAgentProgramName);
		if ( ! isEnabled )
			return;
		
		int pos_colname = newSample.findColumn(colname);
		if (pos_colname == -1)
		{
			return;
		}

		// Loop on all newSample rows
		int rowc = newSample.getRowCount();
		for (int rowId=0; rowId < rowc; rowId++) 
		{
			Object o_progName = newSample.getValueAt(rowId, pos_colname);
			
			if (o_progName instanceof String)
			{
				String progName = ((String)o_progName);

				progName = SqlAgentJobInfoCache.resolveProgramNameForJobName(progName);
				
				newSample.setValueAt(progName, rowId, pos_colname);
			}
		}
	}

}

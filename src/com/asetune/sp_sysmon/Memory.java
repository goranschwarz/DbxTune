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
package com.asetune.sp_sysmon;

import java.util.List;

import com.asetune.cm.CountersModel;

public class Memory 
extends AbstractSysmonType
{
	public Memory(SpSysmon sysmon, CountersModel cm)
	{
		super(sysmon, cm);
	}

	public Memory(SpSysmon sysmon, long srvVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		super(sysmon, srvVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	@Override
	public String getReportName()
	{
		return "Memory";
	}

	@SuppressWarnings("unused")
	@Override
	public void calc()
	{
		String fieldName  = "";
		String groupName  = "";
		int    instanceid = -1;
		int    field_id   = -1;
		int    value      = 0;

		int fld_PagesAllocated = 0;
		int fld_PagesReleased  = 0;

		for (List<Object> row : getData())
		{
			if (_instanceid_pos > 0)
				instanceid = ((Number)row.get(_instanceid_pos)).intValue();
			fieldName = (String)row.get(_fieldName_pos);
			groupName = (String)row.get(_groupName_pos);
//			field_id  = ((Number)row.get(_field_id_pos)).intValue();
			value     = ((Number)row.get(_value_pos)).intValue();

			//----------------------------
			// Memory
			//----------------------------

			// Pages Allocated
			if (groupName.equals("memory") && fieldName.equals("mempages_alloced"))
				fld_PagesAllocated += value; // NOT SUM, but += anyway

			// Pages Released
			if (groupName.equals("memory") && fieldName.equals("mempages_freed"))
				fld_PagesReleased += value; // NOT SUM, but += anyway
		}

		addReportHead("Memory Management");
		addReportLnCnt("  Pages Allocated",  fld_PagesAllocated);
		addReportLnCnt("  Pages Released",   fld_PagesReleased);
	}
}

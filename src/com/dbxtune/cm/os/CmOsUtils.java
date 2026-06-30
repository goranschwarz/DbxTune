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
package com.dbxtune.cm.os;

import java.util.ArrayList;
import java.util.List;

public class CmOsUtils
{

	/**
	 * List all KNOWN CmOs*
	 * 
	 * @return
	 */
	public static List<String> getCmOsNames()
	{
		List<String> names = new ArrayList<>();
		
		names.add(CmOsIostat    .class.getSimpleName());
		names.add(CmOsVmstat    .class.getSimpleName());
		names.add(CmOsMpstat    .class.getSimpleName());
		names.add(CmOsUptime    .class.getSimpleName());
		names.add(CmOsMeminfo   .class.getSimpleName());
		names.add(CmOsNwInfo    .class.getSimpleName());
		names.add(CmOsDiskSpace .class.getSimpleName());
		names.add(CmOsPs        .class.getSimpleName());

		return names;
	}
}

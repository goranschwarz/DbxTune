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
package com.asetune.gui;

import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

public abstract class DbmsVersionPanelTds 
extends DbmsVersionPanelAbstract
{
	private static final long serialVersionUID = 1L;

	public DbmsVersionPanelTds(ShowCmPropertiesDialog propDialog)
	{
		super(propDialog);
	}
	
	@Override
	public long parseVersionStringToNum(String versionStr)
	{
		if (StringUtil.isNullOrBlank(versionStr) || "0.0.0".equals(versionStr))
			versionStr = "00.0.0"; // then the below sybVersionStringToNumber() work better
		
		// if 1.2 --> 01.2
		if (versionStr.matches("^[0-9]\\.[0-9].*"))
			versionStr = "0" + versionStr; // then the below sybVersionStringToNumber() work better

		long version = Ver.sybVersionStringToNumber(versionStr);
		return version;
	}
	
	@Override
	public String versionNumToString(long srvVersion, int major, int minor, int maint, int sp, int pl)
	{
		String srvVersionStr = Ver.versionNumToStr(srvVersion);
		return srvVersionStr;
	}
}

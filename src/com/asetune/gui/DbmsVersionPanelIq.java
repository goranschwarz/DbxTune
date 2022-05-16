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

import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSybaseIq;
import com.asetune.utils.Ver;

public class DbmsVersionPanelIq
extends DbmsVersionPanelTds
{
	private static final long serialVersionUID = 1L;

	public DbmsVersionPanelIq(ShowCmPropertiesDialog propDialog)
	{
		super(propDialog);
	}

	@Override
	public long getMinVersion()
	{
		return Ver.ver(12,6);
	}

	@Override
	protected DbmsVersionInfo createEmptyDbmsVersionInfo()
	{
		return new DbmsVersionInfoSybaseIq(getMinVersion());
	}

	@Override
	protected DbmsVersionInfo createDbmsVersionInfo()
	{
		// Get long version number from GUI Spinners
		long ver = getVersionNumberFromSpinners();

		// Create a DBMS Server specific version object
		DbmsVersionInfoSybaseIq versionInfo = new DbmsVersionInfoSybaseIq(ver);

		return versionInfo;
	}
}

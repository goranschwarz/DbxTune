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
package com.dbxtune;

import com.dbxtune.gui.MainFrame;

/**
 * GUI: Show what the collector is doing in the MainFrame status bar
 */
public class RefreshStatusGui
implements IRefreshStatus
{
	@Override
	public void setStatus(String status)
	{
		MainFrame.getInstance().setStatus(MainFrame.ST_STATUS_FIELD, status);
	}

	@Override
	public void setSubStatus(String subStatus)
	{
		MainFrame.getInstance().setStatus(MainFrame.ST_STATUS2_FIELD, subStatus);
	}
}

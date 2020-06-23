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
package com.asetune.check;

import java.util.List;

public class CheckForUpdatesRax extends CheckForUpdatesDbx
{

//	@Override
//	public QueryString createCheckForUpdate(Object... params)
//	{
//		System.out.println("NOT_YET_IMPLEMENTED: createCheckForUpdate()");
//		return null;
//	}

//	@Override
//	public QueryString createSendConnectInfo(Object... params)
//	{
//		System.out.println("NOT_YET_IMPLEMENTED: createSendConnectInfo()");
//		return null;
//	}

//	@Override
//	public List<QueryString> createSendCounterUsageInfo(Object... params)
//	{
//		System.out.println("NOT_YET_IMPLEMENTED: createSendCounterUsageInfo()");
//		return null;
//	}

	@Override
	public List<QueryString> createSendMdaInfo(Object... params)
	{
		System.out.println("NOT_YET_IMPLEMENTED: createSendMdaInfo()");
		return null;
	}

//	@Override
//	public QueryString createSendUdcInfo(Object... params)
//	{
//		System.out.println("NOT_YET_IMPLEMENTED: createSendUdcInfo()");
//		return null;
//	}

//	@Override
//	public QueryString createSendLogInfo(Object... params)
//	{
//		System.out.println("NOT_YET_IMPLEMENTED: createSendLogInfo()");
//		return null;
//	}

}

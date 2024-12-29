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
package com.dbxtune.check;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.dbxtune.central.DbxCentralStatistics;
import com.dbxtune.central.DbxCentralStatistics.ServerEntry;
import com.dbxtune.gui.Log4jLogRecord;

public class CheckForUpdatesDbxCentral extends CheckForUpdatesDbx
{
	private static Logger _logger = Logger.getLogger(CheckForUpdatesDbxCentral.class);

	protected static final String DBXCENTRAL_CONNECT_INFO_URL   = "http://www.dbxtune.com/dbxc_connect_info.php";    // TODO: DO NOT EXISTS FOR THE MOMENT
	protected static final String DBXCENTRAL_STORE_INFO_URL     = "http://www.dbxtune.com/dbxc_store_info.php";      // TODO: DO NOT EXISTS FOR THE MOMENT
	protected static final String DBXCENTRAL_STORE_SRV_INFO_URL = "http://www.dbxtune.com/dbxc_store_srv_info.php";  // TODO: DO NOT EXISTS FOR THE MOMENT

//	@Override
//	public QueryString createCheckForUpdate(Object... params)
//	{
//		System.out.println("NOT_YET_IMPLEMENTED: createCheckForUpdate()");
//		return null;
//	}

	@Override
	public QueryString createSendConnectInfo(Object... params)
	{
		//System.out.println("NOT_YET_IMPLEMENTED: createSendConnectInfo()");
		return null;
	}

	@Override
	public List<QueryString> createSendCounterUsageInfo(Object... params)
	{
//System.out.println("DBX_CENTRAL: ------- createSendCounterUsageInfo()");

		// Get the statistics object
		DbxCentralStatistics stat = (DbxCentralStatistics) params[0];
		
		// List of URL Send that we shoudl do.
		List<QueryString> sendQueryList = new ArrayList<QueryString>();
		
		// COMPOSE: parameters to send to HTTP server
		QueryString storeParams = new QueryString(DBXCENTRAL_STORE_INFO_URL);
		sendQueryList.add(storeParams);

		String checkId = getCheckId() + "";

		if (_logger.isDebugEnabled())
			storeParams.add("debug",    "true");

		storeParams.add("checkId",              checkId);
		storeParams.add("userName",             System.getProperty("user.name"));

		storeParams.add("shutdownReason",       stat.getShutdownReason() );
		storeParams.add("wasRestartSpecified",  stat.getRestartSpecified() ? "1" : "0" ); // boolean to int string

		storeParams.add("writerJdbcUrl",        stat.getJdbcWriterUrl() );
		storeParams.add("H2DbFileSize1InMb",    stat.getH2DbFileSize1InMb() );
		storeParams.add("H2DbFileSize2InMb",    stat.getH2DbFileSize2InMb() );
		storeParams.add("H2DbFileSizeDiffInMb", stat.getH2DbFileSizeDiffInMb() );


		// Foreach server we have stored info for, send...
		for (ServerEntry srvEntry : stat.getServerEntries().values())
		{
			QueryString srvParams = new QueryString(DBXCENTRAL_STORE_SRV_INFO_URL);
			sendQueryList.add(srvParams);

			if (_logger.isDebugEnabled())
				srvParams.add("debug",    "true");
			
			srvParams.add("checkId",             checkId);
			srvParams.add("userName",            System.getProperty("user.name"));
			
			srvParams.add("srvName",             srvEntry.getSrvName() );
			srvParams.add("dbxProduct",          srvEntry.getDbxProduct() );
			
			srvParams.add("firstSamleTime",      srvEntry.getFirstSampleTimeStr());
			srvParams.add("lastSamleTime",       srvEntry.getLastSampleTimeStr());
			
			srvParams.add("alarmCount",          srvEntry.getAlarmCount() );
			srvParams.add("receiveCount",        srvEntry.getReceiveCount() );
			srvParams.add("receiveGraphCount",   srvEntry.getReceiveGraphCount() );
		}

//System.out.println("DBX_CENTRAL: <<<<<< ------- createSendCounterUsageInfo()");
//for (QueryString qs : sendQueryList)
//	System.out.println("DBX_CENTRAL: " + qs);
//System.out.println("DBX_CENTRAL: <<<<<< ------- createSendCounterUsageInfo()");
		
		return sendQueryList;
	}

	@Override
	public List<QueryString> createSendMdaInfo(Object... params)
	{
//		System.out.println("NOT_YET_IMPLEMENTED: createSendMdaInfo()");
		return null;
	}

	@Override
	public QueryString createSendUdcInfo(Object... params)
	{
//		System.out.println("NOT_YET_IMPLEMENTED: createSendUdcInfo()");
		return null;
	}

//	@Override
//	public QueryString createSendLogInfo(Object... params)
//	{
//		System.out.println("NOT_YET_IMPLEMENTED: createSendLogInfo()");
//		return null;
//	}

	/**
	 * Check if we should send a specific log message to dbxtune.com
	 * 
	 * @param record
	 * @return true if the message should be discarded
	 */
	@Override
	public boolean discardLogInfoMessage(final Log4jLogRecord record)
	{
		String msg = record.getMessage();
		if (msg != null)
		{
			// check for various messages
			
			// The persistent queue has 3 entries. The persistent writer might not keep in pace. The current consumer has been active for 00:00:04.124	
			// When the PCS has been active for MORE THAN 1 MINUTE
			if (msg.indexOf("The persistent writer might not keep in pace. The current consumer has been active for") >= 0)
			{
				// Discard message when the Writer has worked for less than a minute
				// OR: send only records that seems to be strange
				if (msg.indexOf("00:00:") >= 0) 
					return true;
			}

//			if (msg.indexOf("XXXXXXXXXXXXXXXXXXXXXX") >= 0)
//				return true;
			
//			if (msg.startsWith("XXXXXXXXXXXXXXXXXXXXXX"))
//				return true;
		}

		return false;
	}
}

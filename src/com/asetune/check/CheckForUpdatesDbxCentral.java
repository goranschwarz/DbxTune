package com.asetune.check;

import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.gui.Log4jLogRecord;

public class CheckForUpdatesDbxCentral extends CheckForUpdatesDbx
{
	private static Logger _logger = Logger.getLogger(CheckForUpdatesDbxCentral.class);

	protected static final String DBXCENTRAL_CONNECT_INFO_URL       = "http://www.dbxtune.com/dbxc_connect_info.php";        // TODO: DO NOT EXISTS FOR THE MOMENT
	protected static final String DBXCENTRAL_COUNTER_USAGE_INFO_URL = "http://www.dbxtune.com/dbxc_counter_usage_info.php";  // TODO: DO NOT EXISTS FOR THE MOMENT

//	@Override
//	public QueryString createCheckForUpdate(Object... params)
//	{
//		System.out.println("NOT_YET_IMPLEMENTED: createCheckForUpdate()");
//		return null;
//	}

	@Override
	public QueryString createSendConnectInfo(Object... params)
	{
//		System.out.println("NOT_YET_IMPLEMENTED: createSendConnectInfo()");
		return null;
	}

//	@Override
//	public List<QueryString> createSendCounterUsageInfo(Object... params)
//	{
//		System.out.println("NOT_YET_IMPLEMENTED: createSendCounterUsageInfo()");
//		return null;
//	}

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

	
	

//	@Override
//	public List<QueryString> createSendCounterUsageInfo(Object... params)
//	{
////System.out.println(">>>>>> CheckForUpdates-Generic-DbxTune >>>>>>>>> TRACE: createSendCounterUsageInfo()");
//		// URL TO USE
//		String urlStr = DBXCENTRAL_COUNTER_USAGE_INFO_URL;
//
//		Configuration conf = Configuration.getCombinedConfiguration();
//		if (conf == null)
//		{
//			_logger.debug("Configuration was null when trying to send 'Counter Usage' info, skipping this.");
//			return null;
//		}
//
//		// COMPOSE: parameters to send to HTTP server
//		QueryString urlParams = new QueryString(urlStr);
//
////		Date timeNow = new Date(System.currentTimeMillis());
//
//		String checkId          = getCheckId() + "";
////		String clientTime       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);
//
//		String sampleStartTime  = "";
//		String sampleEndTime    = "";
//
////		if (CounterController.hasInstance())
////		{
////			ICounterController cnt = CounterController.getInstance();
////
////			Timestamp startTs = cnt.getStatisticsFirstSampleTime();
////			Timestamp endTs   = cnt.getStatisticsLastSampleTime();
////
////			if (startTs != null) sampleStartTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startTs);
////			if (endTs   != null) sampleEndTime   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endTs);
////
////			// now RESET COUNTERS collector
////			cnt.resetStatisticsTime();
////		}
//
//		if (_logger.isDebugEnabled())
//			urlParams.add("debug",    "true");
//
//		urlParams.add("checkId",             checkId);
//		urlParams.add("connectId",           getConnectCount()+"");
////		urlParams.add("clientTime",          clientTime);
//		urlParams.add("sessionType",         "dbx-central");
//		urlParams.add("sessionStartTime",    sampleStartTime);
//		urlParams.add("sessionEndTime",      sampleEndTime);
//		urlParams.add("clientAppName",       Version.getAppName());
//		urlParams.add("userName",            System.getProperty("user.name"));
//
//		int rows = 0;
////		for (CountersModel cm : CounterController.getInstance().getCmList())
////		{
////			int minRefresh = 1;
////			if (_logger.isDebugEnabled())
////				minRefresh = 1;
////			if (cm.getRefreshCounter() >= minRefresh)
////			{
////				urlParams.add(cm.getName(), cm.getRefreshCounter() + "," + cm.getSumRowCount());
////				rows++;
////			}
////			
////			// now RESET COUNTERS in the CM's
////			cm.resetStatCounters();
////		}
//
//
//		// Keep the Query objects in a list, because the "offline" database can have multiple sends.
//		List<QueryString> sendQueryList = new ArrayList<QueryString>();
//		if (rows > 0)
//		{
//			sendQueryList.add(urlParams);
//		}
//		else // If NO UDC rows where found, Then lets try to see if we have the offline database has been read 
//		{
//			_logger.debug("No 'Counter Usage' was reported, skipping this.");
//
//			if ( ! PersistReader.hasInstance() )
//			{
//				// Nothing to do, lets get out of here
//				return null;
//			}
//			else
//			{
//				_logger.debug("BUT, trying to get the info from PersistReader.");
//
//				// Get reader and some other info.
//				PersistReader     reader      = PersistReader.getInstance();
//				List<SessionInfo> sessionList = reader.getLastLoadedSessionList();
//
//				// Loop the session list and add parameters to url send
//				int loopCnt = 0;
//				for (SessionInfo sessionInfo : sessionList)
//				{
//					loopCnt++;
//
//					// COMPOSE: parameters to send to HTTP server
//					urlParams = new QueryString(urlStr);
//
//					if (_logger.isDebugEnabled())
//						urlParams.add("debug",    "true");
//
//					checkId                 = getCheckId() + "";
//					String sessionStartTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(sessionInfo._sessionId);
//					String sessionEndTime   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(sessionInfo._lastSampleTime);
//
//					if (_logger.isDebugEnabled())
//						urlParams.add("debug",    "true");
//
//					urlParams.add("checkId",             checkId);
//					urlParams.add("connectId",           getConnectCount()+"");
////					urlParams.add("clientTime",          sessionTime);
//					urlParams.add("sessionType",         "offline-"+loopCnt);
//					urlParams.add("sessionStartTime",    sessionStartTime);
//					urlParams.add("sessionEndTime",      sessionEndTime);
//					urlParams.add("clientAppName",       Version.getAppName());
//					urlParams.add("userName",            System.getProperty("user.name"));
//
//					// Print info
////					System.out.println("sessionInfo: \n" +
////							"   _sessionId              = "+sessionInfo._sessionId              +", \n" +
////							"   _lastSampleTime         = "+sessionInfo._lastSampleTime         +", \n" +
////							"   _numOfSamples           = "+sessionInfo._numOfSamples           +", \n" +
////							"   _sampleList             = "+sessionInfo._sampleList             +", \n" +
////							"   _sampleCmNameSumMap     = "+sessionInfo._sampleCmNameSumMap     +", \n" +
////							"   _sampleCmNameSumMap     = "+(sessionInfo._sampleCmNameSumMap     == null ? "null" : "size="+sessionInfo._sampleCmNameSumMap.size()    +", keySet:"+sessionInfo._sampleCmNameSumMap.keySet()    ) +", \n" +
////							"   _sampleCmCounterInfoMap = "+(sessionInfo._sampleCmCounterInfoMap == null ? "null" : "size="+sessionInfo._sampleCmCounterInfoMap.size()+", keySet:"+sessionInfo._sampleCmCounterInfoMap.keySet()) +", \n" +
////							"   -end-.");
//
//					// Loop CM's
//					rows = 0;
//					for (CmNameSum cmNameSum : sessionInfo._sampleCmNameSumMap.values())
//					{
//						// Print info
////						System.out.println(
////							"   > CmNameSum: \n" +
////							"   >>  _cmName           = " + cmNameSum._cmName            +", \n" +
////							"   >>  _sessionStartTime = " + cmNameSum._sessionStartTime  +", \n" +
////							"   >>  _absSamples       = " + cmNameSum._absSamples        +", \n" +
////							"   >>  _diffSamples      = " + cmNameSum._diffSamples       +", \n" +
////							"   >>  _rateSamples      = " + cmNameSum._rateSamples       +", \n" +
////							"   >>  -end-.");
//
//						int minRefresh = 1;
//						if (cmNameSum._diffSamples >= minRefresh)
//						{
//							// Get how many rows has been sampled for this CM during this sample session
//							// In most cases the pointer sessionInfo._sampleCmCounterInfoMap will be null
//							// which means a 0 value
//							int rowsSampledInThisPeriod = 0;
//							
//							if (sessionInfo._sampleCmCounterInfoMap != null)
//							{
//								// Get details for "current" sample session
//								// summarize all "diff rows" into a summary, which will be sent as statistsics
//								for (SampleCmCounterInfo scmci : sessionInfo._sampleCmCounterInfoMap.values())
//								{
//									// Get current CMName in the map
//									CmCounterInfo cmci = scmci._ciMap.get(cmNameSum._cmName);
//									if (cmci != null)
//										rowsSampledInThisPeriod += cmci._diffRows;
//								}
//							}
//							
//							// finally add the info to the UrlParams
//							urlParams.add(cmNameSum._cmName, cmNameSum._diffSamples + "," + rowsSampledInThisPeriod);
//
//							rows++;
//						}
//					}
//
//					// add it to the list
//					if (rows > 0)
//						sendQueryList.add(urlParams);
//				}
//			}
//		} // end: offline data
//		
//		return sendQueryList;
//	}

}

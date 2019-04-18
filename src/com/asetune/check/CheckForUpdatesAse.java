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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.Version;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.utils.Ver;

public class CheckForUpdatesAse extends CheckForUpdatesDbx
{
	private static Logger _logger = Logger.getLogger(CheckForUpdatesAse.class);

//	@Override protected String getHomeUrl()            { return ASETUNE_HOME_URL; };
//	@Override protected String getDefaultDownloadUrl() { return getHomeUrl() + "/download.html"; }
//	@Override protected String getDefaultWhatsNewUrl() { return getHomeUrl() + "/history.html"; }

//	private static final String ASETUNE_HOME_URL               = "http://www.dbxtune.com";
//	private static final String ASETUNE_CHECK_UPDATE_URL       = "http://www.dbxtune.com/check_for_update.php";
//	private static final String ASETUNE_CONNECT_INFO_URL       = "http://www.dbxtune.com/connect_info.php";
	private static final String ASETUNE_MDA_INFO_URL           = "http://www.dbxtune.com/mda_info.php";
//	private static final String ASETUNE_UDC_INFO_URL           = "http://www.dbxtune.com/udc_info.php";
//	private static final String ASETUNE_COUNTER_USAGE_INFO_URL = "http://www.dbxtune.com/counter_usage_info.php";
//	private static final String ASETUNE_ERROR_INFO_URL         = "http://www.dbxtune.com/error_info.php";

	public CheckForUpdatesAse()
	{
		super();
	}

//	//-----------------------------------------------------------------------------------------------------------
//	//-----------------------------------------------------------------------------------------------------------
//	//-----------------------------------------------------------------------------------------------------------
//	@Override
////	public QueryString createCheckForUpdate()
//	public QueryString createCheckForUpdate(Object... params)
//	{
//System.out.println(">>>>>> CheckForUpdatesAse >>>>>>>>> TRACE: createCheckForUpdate()");
//		// URL TO USE
//		String urlStr = ASETUNE_CHECK_UPDATE_URL;
//
//		// COMPOSE: parameters to send to HTTP server
//		QueryString urlParams = new QueryString(urlStr);
//
//		Date timeNow = new Date(System.currentTimeMillis());
//		String clientTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);
//
//		String appStartupTime = TimeUtils.msToTimeStr("%MM:%SS.%ms", System.currentTimeMillis() - DbxTune.getStartTime());
//
//		if (_logger.isDebugEnabled())
//			urlParams.add("debug",    "true");
//
//		urlParams.add("clientCheckTime",     clientTime);
//
//		urlParams.add("clientSourceDate",     Version.getSourceDate());
//		urlParams.add("clientSourceVersion",  Version.getSourceRev());
//		urlParams.add("clientAppName",        Version.getAppName());
//		urlParams.add("clientAppVersion",     Version.getVersionStr());
////		urlParams.add("clientAseTuneVersion", Version.getVersionStr());
//		urlParams.add("clientExpireDate",     Version.DEV_VERSION_EXPIRE_STR);
//		urlParams.add("appStartupTime",       appStartupTime);
//
//		try
//		{
//			InetAddress addr = InetAddress.getLocalHost();
//
//			urlParams.add("clientHostName",          addr.getHostName());
//			urlParams.add("clientHostAddress",       addr.getHostAddress());
//			urlParams.add("clientCanonicalHostName", addr.getCanonicalHostName());
//
//		}
//		catch (UnknownHostException e)
//		{
//		}
//
//		urlParams.add("user_name",          System.getProperty("user.name"));
//		urlParams.add("user_home",          System.getProperty("user.home"));
//		urlParams.add("user_dir",           System.getProperty("user.dir"));
//		urlParams.add("user_country",       System.getProperty("user.country"));
//		urlParams.add("user_language",      System.getProperty("user.language"));
//		urlParams.add("user_timezone",      System.getProperty("user.timezone"));
//		urlParams.add("propfile",           Configuration.getInstance(Configuration.SYSTEM_CONF).getFilename());
//		urlParams.add("userpropfile",       Configuration.getInstance(Configuration.USER_TEMP).getFilename());
//		urlParams.add("gui",                DbxTune.hasGui()+"");
//
//		urlParams.add("java_version",       System.getProperty("java.version"));
//		urlParams.add("java_vm_version",    System.getProperty("java.vm.version"));
//		urlParams.add("java_vm_vendor",     System.getProperty("java.vm.vendor"));
//		urlParams.add("sun_arch_data_model",System.getProperty("sun.arch.data.model"));
//		urlParams.add("java_home",          System.getProperty("java.home"));
//		if (useHttpPost())
//			urlParams.add("java_class_path",System.getProperty("java.class.path"));
//		else
//			urlParams.add("java_class_path","discarded when using sendHttpParams()");
//		urlParams.add("memory",             (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB");
//		urlParams.add("os_name",            System.getProperty("os.name"));
//		urlParams.add("os_version",         System.getProperty("os.version"));
//		urlParams.add("os_arch",            System.getProperty("os.arch"));
//		urlParams.add("sun_desktop",        System.getProperty("sun.desktop"));
//		urlParams.add("-end-",              "-end-");
//
//		return urlParams;
//	}


	
	
//	//-----------------------------------------------------------------------------------------------------------
//	//-----------------------------------------------------------------------------------------------------------
//	//-----------------------------------------------------------------------------------------------------------
//	@Override
////	public QueryString createSendConnectInfo(int connType, SshTunnelInfo sshTunnelInfo)
//	public QueryString createSendConnectInfo(Object... params)
//	{
////System.out.println(">>>>>> CheckForUpdatesAse >>>>>>>>> TRACE: createSendConnectInfo()");
//		int connType                = (Integer)       params[0];
//		SshTunnelInfo sshTunnelInfo = (SshTunnelInfo) params[1];
//
//		// URL TO USE
//		String urlStr = ASETUNE_CONNECT_INFO_URL;
//
//		if ( ! MonTablesDictionaryManager.hasInstance() )
//		{
//			_logger.debug("MonTablesDictionary not initialized when trying to send connection info, skipping this.");
//			return null;
//		}
//		MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
//		if (mtd == null)
//		{
//			_logger.debug("MonTablesDictionary was null when trying to send connection info, skipping this.");
//			return null;
//		}
//		
//		if (connType != ConnectionDialog.TDS_CONN && connType != ConnectionDialog.OFFLINE_CONN)
//		{
//			_logger.warn("ConnectInfo: Connection type must be TDS_CONN | OFFLINE_CONN");
//			return null;
//		}
//
//		QueryString urlParams = new QueryString(urlStr);
//
//		Date timeNow = new Date(System.currentTimeMillis());
//
//		String checkId          = getCheckId() + "";
//		String clientTime       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);
//
//		String srvVersion       = "0";
//		String isClusterEnabled = "0";
//
//		String srvName          = "";
//		String srvIpPort        = "";
//		String srvUser          = "";
//		String srvUserRoles     = "";
//		String srvVersionStr    = "";
//		String srvSortOrderId   = "";
//		String srvSortOrderName = "";
//		String srvCharsetId     = "";
//		String srvCharsetName   = "";
//		String srvSapSystemInfo = "";
//		String sshTunnelInfoStr = "";
//
//		String usePcs           = "";
//		String pcsConfig        = "";
//
//		
//		if (connType == ConnectionDialog.TDS_CONN)
//		{
//			srvVersion       = mtd.getDbmsExecutableVersionNum() + "";
//			isClusterEnabled = mtd.isClusterEnabled() + "";
//
//			srvName          = AseConnectionFactory.getServer();
//			srvIpPort        = AseConnectionFactory.getHostPortStr();
//			srvUser          = AseConnectionFactory.getUser();
//			srvUserRoles     = "not_initialized";
//			srvVersionStr    = mtd.getDbmsExecutableVersionStr();
//			srvSortOrderId   = mtd.getDbmsSortId() + "";
//			srvSortOrderName = mtd.getDbmsSortName();
//			srvCharsetId     = mtd.getDbmsCharsetId() + "";
//			srvCharsetName   = mtd.getDbmsCharsetName();
//			srvSapSystemInfo = mtd.getSapSystemInfo();
//
//			if (sshTunnelInfo != null)
//				sshTunnelInfoStr = sshTunnelInfo.getInfoString();
//
//			// Get role list from the Summary CM
////			CountersModel summaryCm = GetCounters.getInstance().getCmByName(GetCounters.CM_NAME__SUMMARY);
//			CountersModel summaryCm = CounterController.getInstance().getSummaryCm();
//			if (summaryCm != null && summaryCm.isRuntimeInitialized())
//				srvUserRoles = StringUtil.toCommaStr(summaryCm.getActiveRoles());
//
//			usePcs           = "false";
//			pcsConfig        = "";
//			if (PersistentCounterHandler.hasInstance())
//			{
//				PersistentCounterHandler pch = PersistentCounterHandler.getInstance();
//				if (pch.isRunning())
//				{
//					usePcs = "true";
//					pcsConfig = pch.getConfigStr();
//				}
//			}
//		}
//		else if (connType == ConnectionDialog.OFFLINE_CONN)
//		{
//			srvVersion       = "-1";
//			isClusterEnabled = "-1";
//
//			srvName          = "offline-read";
//			srvIpPort        = "offline-read";
//			srvUser          = "offline-read";
//			srvUserRoles     = "offline-read";
//			srvVersionStr    = "offline-read";
//			srvSortOrderId   = "offline-read";
//			srvSortOrderName = "offline-read";
//			srvCharsetId     = "offline-read";
//			srvCharsetName   = "offline-read";
//			srvSapSystemInfo = "offline-read";
//			sshTunnelInfoStr = "offline-read";
//
//			usePcs           = "true";
//			pcsConfig        = "";
//
//			if ( PersistReader.hasInstance() )
//			{
//				PersistReader reader = PersistReader.getInstance();
//				pcsConfig = reader.GetConnectionInfo();
//			}
//		}
//
//		if (srvName       != null) srvName.trim();
//		if (srvIpPort     != null) srvIpPort.trim();
//		if (srvUser       != null) srvUser.trim();
//		if (srvVersionStr != null) srvVersionStr.trim();
//
//		if (_logger.isDebugEnabled())
//			urlParams.add("debug",    "true");
//
//		urlParams.add("checkId",             checkId);
//		urlParams.add("clientTime",          clientTime);
//		urlParams.add("clientAppName",       Version.getAppName());
//		urlParams.add("userName",            System.getProperty("user.name"));
//
//		urlParams.add("connectId",           getConnectCount()+"");
//		urlParams.add("srvVersion",          srvVersion);
//		urlParams.add("isClusterEnabled",    isClusterEnabled);
//
//		urlParams.add("srvName",             srvName);
//		urlParams.add("srvIpPort",           srvIpPort);
//		urlParams.add("srvUser",             srvUser);
//		urlParams.add("srvUserRoles",        srvUserRoles);
//		urlParams.add("srvVersionStr",       srvVersionStr);
//		urlParams.add("srvSortOrderId",      srvSortOrderId);
//		urlParams.add("srvSortOrderName",    srvSortOrderName);
//		urlParams.add("srvCharsetId",        srvCharsetId);
//		urlParams.add("srvCharsetName",      srvCharsetName);
//		urlParams.add("srvSapSystemInfo",    srvSapSystemInfo);
//		urlParams.add("sshTunnelInfo",       sshTunnelInfoStr);
//
//		urlParams.add("usePcs",              usePcs);
//		urlParams.add("pcsConfig",           pcsConfig);
//
//		return urlParams;
//	}


	
	//-----------------------------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------------
	@Override
//	public List<QueryString> createSendMdaInfo()
	public List<QueryString> createSendMdaInfo(Object... params)
	{
//System.out.println(">>>>>> CheckForUpdatesAse >>>>>>>>> TRACE: createSendMdaInfo()");
		if ( ! MonTablesDictionaryManager.hasInstance() )
		{
			_logger.debug("MonTablesDictionary not initialized when trying to send connection info, skipping this.");
			return null;
		}
		MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
		if (mtd == null)
		{
			_logger.debug("MonTablesDictionary was null when trying to send connection info, skipping this.");
			return null;
		}

		if (mtd.getDbmsExecutableVersionNum() <= 0)
		{
			_logger.debug("MonTablesDictionary srvVersionNum is zero, stopping here.");
			return null;
		}

		if (mtd.getDbmsMonTableVersionNum() > 0 && mtd.getDbmsExecutableVersionNum() != mtd.getDbmsMonTableVersionNum())
		{
			_logger.info("MonTablesDictionary srvVersionNum("+mtd.getDbmsExecutableVersionNum()+") and installmaster/monTables VersionNum("+mtd.getDbmsMonTableVersionNum()+") is not in sync, so we don't want to send MDA info about this.");
			return null;
		}

		if ( ! CounterController.getInstance().isMonConnected() )
		{
			_logger.debug("No ASE Connection to the monitored server.");
			return null;
		}

		List<QueryString> sendQueryList = new ArrayList<QueryString>();

//		int rowCountSum = 0;

		Date timeNow = new Date(System.currentTimeMillis());

		String checkId          = getCheckId() + "";
		String clientTime       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);

		String srvVersion       = mtd.getDbmsExecutableVersionNum() + "";
		String isClusterEnabled = mtd.isClusterEnabled() + "";		

		// Get MDA information 
		try
		{
			// monTables
			String sql_monTables_rowCount = 
				"select count(*) from master.dbo.monTables \n" +
				(mtd.getDbmsMonTableVersion() >= Ver.ver(15,7) ? "where Language = 'en_US' \n" : "");

			String sql_monTables = 
				"select "
						+ "type       = 'T', "
						+ "t.TableName, "
						+ "t.TableID, "
						+ "ColumnName = 'ColumnID=NumOfCols', "
						+ "ColumnID   = t.Columns, "
						+ "TypeName   = 'Length=NumOfParameters', "
						+ "Length     = t.Parameters, "
						+ "t.Indicators, "
						+ "t.Description  \n" +
			    "from master.dbo.monTables t \n" +
				(mtd.getDbmsMonTableVersion() >= Ver.ver(15,7) ? "where Language = 'en_US' \n" : "");

			// monTableColumns
			String sql_monTableColumns_rowCount = 
				"select count(*) from master.dbo.monTableColumns \n" +
				(mtd.getDbmsMonTableVersion() >= Ver.ver(15,7) ? "where Language = 'en_US' \n" : "");

			String sql_monTableColumns = 
				"select "
						+ "type = 'C', "
						+ "c.TableName, "
						+ "c.TableID, "
						+ "c.ColumnName, "
						+ "c.ColumnID, "
						+ "c.TypeName, "
						+ "c.Length, "
						+ "c.Indicators, "
						+ "c.Description  \n" +
			    "from master.dbo.monTableColumns c " +
				(mtd.getDbmsMonTableVersion() >= Ver.ver(15,7) ? "where Language = 'en_US' \n" : "");
			
			// monTableParameters
			String sql_monTableParameters_rowCount = 
				"select count(*) from master.dbo.monTableParameters \n";

			String sql_monTableParameters = 
				"select "
						+ "type = 'P', "
						+ "p.TableName, "
						+ "p.TableID, "
						+ "p.ParameterName, "
						+ "p.ParameterID, "
						+ "p.TypeName, "
						+ "p.Length, "
						+ "Indicators=-1, "
						+ "p.Description  \n" +
			    "from master.dbo.monTableParameters p \n";

			// sysobjects
			String sql_sysobjects_rowCount = 
				"select count(*) \n" +
				"from sysobjects o, syscolumns c \n" +
				"where o.id = c.id \n" +
				"  and o.name like 'mon%' \n" +
				"  and o.type = 'U' \n" +
				"";
			String sql_sysobjects = 
				"select "
						+ "type        = 'S', "
						+ "TableName   = o.name, "
						+ "TableID     = o.id, "
						+ "ColumnName  = c.name, "
						+ "ColumnID    = c.colid, "
						+ "TypeName    = t.name, "
						+ "Length      = t.length, "
						+ "Indicators  = -1, "
						+ "Description = '' \n" +
				"from sysobjects o, syscolumns c, systypes t \n" +
				"where o.id = c.id \n" +
				"  and c.usertype = t.usertype \n" +
				"  and o.name like 'mon%' \n" +
				"  and o.type = 'U' \n" +
				"order by o.name, c.colid \n" +
				"";

			int sendMdaInfoBatchSize = getSendMdaInfoBatchSize();

			// monTables
			getMdaInfo(CounterController.getInstance().getMonConnection(), 
					checkId, clientTime, System.getProperty("user.name"), srvVersion, isClusterEnabled, 
					sql_monTables_rowCount, sql_monTables, 
					sendMdaInfoBatchSize, sendQueryList);

			// monTableColumns
			getMdaInfo(CounterController.getInstance().getMonConnection(), 
					checkId, clientTime, System.getProperty("user.name"), srvVersion, isClusterEnabled, 
					sql_monTableColumns_rowCount, sql_monTableColumns, 
					sendMdaInfoBatchSize, sendQueryList);

			// monTableParameters
			getMdaInfo(CounterController.getInstance().getMonConnection(), 
					checkId, clientTime, System.getProperty("user.name"), srvVersion, isClusterEnabled, 
					sql_monTableParameters_rowCount, sql_monTableParameters, 
					sendMdaInfoBatchSize, sendQueryList);

			// ASE System Tables
			getMdaInfo(CounterController.getInstance().getMonConnection(), 
					checkId, clientTime, System.getProperty("user.name"), srvVersion, isClusterEnabled, 
					sql_sysobjects_rowCount, sql_sysobjects, 
					sendMdaInfoBatchSize, sendQueryList);

//			_logger.info("sendMdaInfo: Starting to send "+rowCountSum+" MDA information entries in "+sendQueryList.size()+" batches, for ASE Version '"+mtd.getAseExecutableVersionNum()+"'.");
			_logger.info("sendMdaInfo: Sending MDA information entries for ASE Version '"+mtd.getDbmsExecutableVersionNum()+"'. (sendMdaInfoBatchSize="+sendMdaInfoBatchSize+")");
		}
		catch (SQLException e)
		{
			sendQueryList.clear();
			_logger.debug("Problems when getting MDA information. Caught: "+e, e);
		}

		return sendQueryList;
	}

	private int getMdaInfo(
			Connection conn,
			String checkId, 
			String clientTime, 
			String userName, 
			String srvVersionNum, 
			String isClusterEnabled, 
			String sqlGetCount, 
			String sqlGetValues,
			int batchSize,
			List<QueryString> sendQueryList)
	throws SQLException
	{
		// URL TO USE
		String urlStr = ASETUNE_MDA_INFO_URL;

		Statement  stmt = conn.createStatement();
		ResultSet  rs;

		// get expected rows
		int expectedRows = 0;
		rs = stmt.executeQuery(sqlGetCount);
		while ( rs.next() )
			expectedRows = rs.getInt(1);
		rs.close();

		// get VALUES
		rs = stmt.executeQuery(sqlGetValues);

		int rowId        = 0;
		int rowsInBatch  = 0;
		int batchCounter = 0;
		QueryString urlParams = new QueryString(urlStr);

		while ( rs.next() )
		{
			rowId++;
			rowsInBatch++;

			if (batchCounter == 0)
			{
				if (_logger.isDebugEnabled())
					urlParams.add("debug",    "true");

				urlParams.add("checkId",            checkId);
				urlParams.add("clientTime",         clientTime);
				urlParams.add("clientAppName",      Version.getAppName());
				urlParams.add("userName",           userName);

				urlParams.add("srvVersion",         srvVersionNum);
				urlParams.add("isClusterEnabled",   isClusterEnabled);

				urlParams.add("expectedRows",       expectedRows+"");
			}

			urlParams.add("type"        + "-" + batchCounter, rs.getString(1)); // NOTE NOT yet added to PHP and database
			urlParams.add("TableName"   + "-" + batchCounter, rs.getString(2));
			urlParams.add("TableID"     + "-" + batchCounter, rs.getString(3));
			urlParams.add("ColumnName"  + "-" + batchCounter, rs.getString(4));
			urlParams.add("ColumnID"    + "-" + batchCounter, rs.getString(5));
			urlParams.add("TypeName"    + "-" + batchCounter, rs.getString(6));
			urlParams.add("Length"      + "-" + batchCounter, rs.getString(7));
			urlParams.add("Indicators"  + "-" + batchCounter, rs.getString(8));
			urlParams.add("Description" + "-" + batchCounter, rs.getString(9));

			urlParams.add("rowId"       + "-" + batchCounter, rowId+"");

			batchCounter++;

			// start new batch OR on last row
			if (batchCounter >= batchSize || rowId >= expectedRows)
			{
				// add number of records added to this entry
				urlParams.add("batchSize",      batchCounter+"");
//System.out.println("QueryString: length="+urlParams.length()+", entries="+urlParams.entryCount()+".");

				batchCounter = 0;
				urlParams.setCounter(rowsInBatch);
				sendQueryList.add(urlParams);

				urlParams = new QueryString(urlStr);
				rowsInBatch = 0;
				//rowId = 0; // Do NOT reset rowId here
			}
		}

		rs.close();
		stmt.close();
		
		return rowId;
	}


	
//	//-----------------------------------------------------------------------------------------------------------
//	//-----------------------------------------------------------------------------------------------------------
//	//-----------------------------------------------------------------------------------------------------------
//	@Override
////	public QueryString createSendUdcInfo()
//	public QueryString createSendUdcInfo(Object... params)
//	{
//System.out.println(">>>>>> CheckForUpdatesAse >>>>>>>>> TRACE: createSendUdcInfo()");
//		// URL TO USE
//		String urlStr = ASETUNE_UDC_INFO_URL;
//
//		Configuration conf = Configuration.getCombinedConfiguration();
//		if (conf == null)
//		{
//			_logger.debug("Configuration was null when trying to send UDC info, skipping this.");
//			return null;
//		}
//
//		// COMPOSE: parameters to send to HTTP server
//		QueryString urlParams = new QueryString(urlStr);
//
//		Date timeNow = new Date(System.currentTimeMillis());
//
//		String checkId          = getCheckId() + "";
//		String clientTime       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);
//
//		if (_logger.isDebugEnabled())
//			urlParams.add("debug",    "true");
//
//		urlParams.add("checkId",             checkId);
//		urlParams.add("clientTime",          clientTime);
//		urlParams.add("userName",            System.getProperty("user.name"));
//		urlParams.add("clientAppName",       Version.getAppName());
//
//		int udcRows = 0;
//		// key: UDC
//		for (String key : conf.getKeys("udc."))
//		{
//			String val = conf.getPropertyRaw(key);
//
//			urlParams.add(key, val);
//			udcRows++;
//		}
//		// key: HOSTMON.UDC
//		for (String key : conf.getKeys("hostmon.udc."))
//		{
//			String val = conf.getPropertyRaw(key);
//
//			urlParams.add(key, val);
//			udcRows++;
//		}
//
//		// If NO UDC rows where found, no need to continue
//		if (udcRows == 0)
//		{
//			_logger.debug("No 'udc.*' or 'hostmon.udc.*' was was found in Configuration, skipping this.");
//			return null;
//		}
//
//		return urlParams;
//	}


	
//	//-----------------------------------------------------------------------------------------------------------
//	//-----------------------------------------------------------------------------------------------------------
//	//-----------------------------------------------------------------------------------------------------------
//	@Override
////	public List<QueryString> createSendCounterUsageInfo()
//	public List<QueryString> createSendCounterUsageInfo(Object... params)
//	{
//System.out.println(">>>>>> CheckForUpdatesAse >>>>>>>>> TRACE: createSendCounterUsageInfo()");
//		// URL TO USE
//		String urlStr = ASETUNE_COUNTER_USAGE_INFO_URL;
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
////		if (GetCounters.hasInstance())
////		{
////			GetCounters cnt = GetCounters.getInstance();
//		if (CounterController.hasInstance())
//		{
////			GetCounters cnt = CounterController.getInstance();
//			ICounterController cnt = CounterController.getInstance();
//
//			Timestamp startTs = cnt.getStatisticsFirstSampleTime();
//			Timestamp endTs   = cnt.getStatisticsLastSampleTime();
//
//			if (startTs != null) sampleStartTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startTs);
//			if (endTs   != null) sampleEndTime   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endTs);
//
//			// now RESET COUNTERS collector
//			cnt.resetStatisticsTime();
//		}
//
//		if (_logger.isDebugEnabled())
//			urlParams.add("debug",    "true");
//
//		urlParams.add("checkId",             checkId);
//		urlParams.add("connectId",           getConnectCount()+"");
////		urlParams.add("clientTime",          clientTime);
//		urlParams.add("sessionType",         "online");
//		urlParams.add("sessionStartTime",    sampleStartTime);
//		urlParams.add("sessionEndTime",      sampleEndTime);
//		urlParams.add("userName",            System.getProperty("user.name"));
//		urlParams.add("clientAppName",       Version.getAppName());
//
//		int rows = 0;
//		for (CountersModel cm : CounterController.getInstance().getCmList())
//		{
//			int minRefresh = 1;
//			if (_logger.isDebugEnabled())
//				minRefresh = 1;
//			if (cm.getRefreshCounter() >= minRefresh)
//			{
//				urlParams.add(cm.getName(), cm.getRefreshCounter() + "," + cm.getSumRowCount());
//				rows++;
//			}
//			
//			// now RESET COUNTERS in the CM's
//			cm.resetStatCounters();
//		}
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
//					urlParams.add("userName",            System.getProperty("user.name"));
//					urlParams.add("clientAppName",       Version.getAppName());
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


	
//	//-----------------------------------------------------------------------------------------------------------
//	//-----------------------------------------------------------------------------------------------------------
//	//-----------------------------------------------------------------------------------------------------------
//	@Override
////	public QueryString createSendLogInfo(Log4jLogRecord record, int sendLogInfoCount)
//	public QueryString createSendLogInfo(Object... params)
//	{
//System.out.println(">>>>>> CheckForUpdatesAse >>>>>>>>> TRACE: createSendLogInfo()");
//		Log4jLogRecord record = (Log4jLogRecord) params[0];
//		int sendLogInfoCount  = (Integer)        params[1];
//
//		// URL TO USE
//		String urlStr = ASETUNE_ERROR_INFO_URL;
//
//		String srvVersion = "not-connected";
//		if ( MonTablesDictionary.hasInstance() )
//		{
//			if ( MonTablesDictionary.getInstance().isInitialized() )
//				srvVersion = MonTablesDictionary.getInstance().getAseExecutableVersionNum() + "";
//
//			else if (MonTablesDictionary.getInstance().hasEarlyVersionInfo())
//				srvVersion = MonTablesDictionary.getInstance().getAseExecutableVersionNum() + "";
//		}
//
//		// COMPOSE: parameters to send to HTTP server
//		QueryString urlParams = new QueryString(urlStr);
//
//		Date timeNow = new Date(System.currentTimeMillis());
//
//		String checkId          = getCheckId() + "";
//		String clientTime       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);
//
//		if (_logger.isDebugEnabled())
//			urlParams.add("debug",    "true");
//
//		urlParams.add("checkId",       checkId);
//		urlParams.add("sendCounter",   sendLogInfoCount +"");
//		urlParams.add("clientTime",    clientTime);
//		urlParams.add("userName",      System.getProperty("user.name"));
//		urlParams.add("clientAppName",       Version.getAppName());
//
//		urlParams.add("srvVersion",    srvVersion);
//		urlParams.add("appVersion",    Version.getVersionStr());
//
//		urlParams.add("logLevel",      record.getLevel().toString());
//		urlParams.add("logThreadName", record.getThreadDescription());
//		urlParams.add("logClassName",  record.getCategory());
//		urlParams.add("logLocation",   record.getLocation());
//		urlParams.add("logMessage",    record.getMessage());
//		urlParams.add("logStacktrace", record.getThrownStackTrace());
//
//		return urlParams;
//	}

}

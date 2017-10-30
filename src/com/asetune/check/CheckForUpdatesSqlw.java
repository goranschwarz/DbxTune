package com.asetune.check;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.gui.ConnectionDialog;
import com.asetune.ssh.SshTunnelInfo;
import com.asetune.tools.sqlw.QueryWindow;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.TimeUtils;

public class CheckForUpdatesSqlw extends CheckForUpdates
{
	private static Logger _logger = Logger.getLogger(CheckForUpdatesSqlw.class);

	@Override protected String getHomeUrl()            { return SQLWIN_HOME_URL; };
	@Override protected String getDefaultDownloadUrl() { return getHomeUrl() + "/download.html"; }
	@Override protected String getDefaultWhatsNewUrl() { return getHomeUrl() + "/history.html"; }

	private static final String SQLWIN_HOME_URL                = "http://www.asetune.com";
	private static final String SQLWIN_CHECK_UPDATE_URL        = "http://www.asetune.com/sqlw_check_for_update.php";
	private static final String SQLWIN_CONNECT_INFO_URL        = "http://www.asetune.com/sqlw_connect_info.php";
	private static final String SQLWIN_COUNTER_USAGE_INFO_URL  = "http://www.asetune.com/sqlw_counter_usage_info.php";

	public CheckForUpdatesSqlw()
	{
		super();
	}

	//-----------------------------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------------
	@Override
	public QueryString createCheckForUpdate(Object... params)
	{
		// COMPOSE: parameters to send to HTTP server
		QueryString urlParams = new QueryString(SQLWIN_CHECK_UPDATE_URL);

		Date timeNow = new Date(System.currentTimeMillis());
		String clientTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);

		if (_logger.isDebugEnabled())
			urlParams.add("debug",    "true");

		String appStartupTime = TimeUtils.msToTimeStr("%MM:%SS.%ms", System.currentTimeMillis() - QueryWindow.getStartTime());

		urlParams.add("clientCheckTime",     clientTime);

		urlParams.add("clientSourceDate",     Version.getSourceDate());
		urlParams.add("clientSourceVersion",  Version.getSourceRev());
		urlParams.add("clientAppName",        Version.getAppName());
		urlParams.add("clientAppVersion",     Version.getVersionStr());
//		urlParams.add("clientExpireDate",     Version.DEV_VERSION_EXPIRE_STR);
		urlParams.add("appStartupTime",       appStartupTime);

		try
		{
			InetAddress addr = InetAddress.getLocalHost();

			urlParams.add("clientHostName",          addr.getHostName());
			urlParams.add("clientHostAddress",       addr.getHostAddress());
			urlParams.add("clientCanonicalHostName", addr.getCanonicalHostName());

		}
		catch (UnknownHostException e)
		{
		}

		urlParams.add("screenResolution",   SwingUtils.getScreenResulutionAsString());
		urlParams.add("hiDpiScale",         SwingUtils.getHiDpiScale()+"");

		urlParams.add("user_name",          System.getProperty("user.name"));
		urlParams.add("user_home",          System.getProperty("user.home"));
		urlParams.add("user_dir",           System.getProperty("user.dir"));
		urlParams.add("user_country",       System.getProperty("user.country"));
		urlParams.add("user_language",      System.getProperty("user.language"));
		urlParams.add("user_timezone",      System.getProperty("user.timezone"));
		urlParams.add("propfile",           Configuration.getInstance(Configuration.SYSTEM_CONF).getFilename());
		urlParams.add("userpropfile",       Configuration.getInstance(Configuration.USER_TEMP).getFilename());
//		urlParams.add("gui",                AseTune.hasGUI()+"");

		urlParams.add("java_version",       System.getProperty("java.version"));
		urlParams.add("java_vm_version",    System.getProperty("java.vm.version"));
		urlParams.add("java_vm_vendor",     System.getProperty("java.vm.vendor"));
		urlParams.add("sun_arch_data_model",System.getProperty("sun.arch.data.model"));
		urlParams.add("java_home",          System.getProperty("java.home"));
//		if (_useHttpPost)
//			urlParams.add("java_class_path",System.getProperty("java.class.path"));
//		else
//			urlParams.add("java_class_path","discarded when using sendHttpParams()");
		urlParams.add("memory",             (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB");
		urlParams.add("os_name",            System.getProperty("os.name"));
		urlParams.add("os_version",         System.getProperty("os.version"));
		urlParams.add("os_arch",            System.getProperty("os.arch"));
		urlParams.add("sun_desktop",        System.getProperty("sun.desktop"));
		urlParams.add("-end-",              "-end-");

		return urlParams;
	}

	
	//-----------------------------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------------
	@Override
//	public void sendSqlwConnectInfo(final SqlwConnectInfo sqlwConnInfo)
	public QueryString createSendConnectInfo(Object... params)
	{
		SqlwConnectInfo sqlwConnInfo = (SqlwConnectInfo) params[0];

		// URL TO USE
		String urlStr = SQLWIN_CONNECT_INFO_URL;

		int connType = sqlwConnInfo._connType;
		if (connType != ConnectionDialog.TDS_CONN && connType != ConnectionDialog.OFFLINE_CONN && connType != ConnectionDialog.JDBC_CONN)
		{
			_logger.warn("ConnectInfo: Connection type must be TDS_CONN | OFFLINE_CONN | JDBC_CONN");
			return null;
		}
		
		// COMPOSE: parameters to send to HTTP server
		QueryString urlParams = new QueryString(urlStr);

		Date timeNow = new Date(System.currentTimeMillis());

		String checkId          = getCheckId() + "";
		String clientTime       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);

		if (_logger.isDebugEnabled())
			urlParams.add("debug",    "true");

		urlParams.add("checkId",             checkId);
		urlParams.add("clientTime",          clientTime);
		urlParams.add("userName",            System.getProperty("user.name"));

		urlParams.add("connectId",           getConnectCount()+"");
		urlParams.add("connectType",         sqlwConnInfo.getConnTypeStr());

		urlParams.add("prodName",            sqlwConnInfo.getProdName());
		urlParams.add("prodVersionStr",      sqlwConnInfo.getProdVersionStr());

		urlParams.add("jdbcDriverName",      sqlwConnInfo.getJdbcDriverName());
		urlParams.add("jdbcDriverVersion",   sqlwConnInfo.getJdbcDriverVersion());
		urlParams.add("jdbcDriver",          sqlwConnInfo.getJdbcDriver());
		urlParams.add("jdbcUrl",             sqlwConnInfo.getJdbcUrl());

		urlParams.add("srvVersionInt",       sqlwConnInfo.getSrvVersionInt()+"");
		urlParams.add("srvName",             sqlwConnInfo.getSrvName());
		urlParams.add("srvUser",             sqlwConnInfo.getSrvUser());
		urlParams.add("srvPageSizeInKb",     sqlwConnInfo.getSrvPageSIzeInKb());
		urlParams.add("srvCharsetName",      sqlwConnInfo.getSrvCharset());
		urlParams.add("srvSortOrderName",    sqlwConnInfo.getSrvSortorder());

		urlParams.add("sshTunnelInfo",       sqlwConnInfo.getSshTunnelInfoStr());

		return urlParams;
	}

	
	//-----------------------------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------------
	@Override
//	private void sendSqlwCounterUsageInfo(final SqlwUsageInfo sqlwUsageInfo)
	public List<QueryString> createSendCounterUsageInfo(Object... params)
	{
		SqlwUsageInfo sqlwUsageInfo = (SqlwUsageInfo) params[0];

		// URL TO USE
		String urlStr = SQLWIN_COUNTER_USAGE_INFO_URL;

		if ( sqlwUsageInfo == null )
		{
			_logger.error("Send SQLW 'Counter Usage info' the input can't be null.");
			return null;
		}

		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf == null)
		{
			_logger.debug("Configuration was null when trying to send SQlW 'Counter Usage info', skipping this.");
			return null;
		}


		// COMPOSE: parameters to send to HTTP server
		QueryString urlParams = new QueryString(urlStr);

		Date timeNow = new Date(System.currentTimeMillis());

		String checkId          = getCheckId() + "";
		String clientTime       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);

		String connectTime      = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(sqlwUsageInfo.getConnectTime());
		String disconnectTime   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(sqlwUsageInfo.getDisconnectTime());

		if (_logger.isDebugEnabled())
			urlParams.add("debug",    "true");

		urlParams.add("checkId",             checkId);
		urlParams.add("clientTime",          clientTime);
		urlParams.add("userName",            System.getProperty("user.name"));

		urlParams.add("connectId",           getConnectCount());

		urlParams.add("connectType",         sqlwUsageInfo.getConnTypeStr());
		urlParams.add("prodName",            sqlwUsageInfo.getProductName());
		urlParams.add("srvVersionInt",       sqlwUsageInfo.getSrvVersionInt());

		urlParams.add("connectTime",         connectTime);
		urlParams.add("disconnectTime",      disconnectTime);
		
		urlParams.add("execMainCount",       sqlwUsageInfo.getExecMainCount());
		urlParams.add("execBatchCount",      sqlwUsageInfo.getExecBatchCount());
		urlParams.add("execTimeTotal",       sqlwUsageInfo.getExecTimeTotal());
		urlParams.add("execTimeSqlExec",     sqlwUsageInfo.getExecTimeSqlExec());
		urlParams.add("execTimeRsRead",      sqlwUsageInfo.getExecTimeRsRead());
		urlParams.add("execTimeOther",       sqlwUsageInfo.getExecTimeOther());
		urlParams.add("rsCount",             sqlwUsageInfo.getRsCount());
		urlParams.add("rsRowsCount",         sqlwUsageInfo.getRsRowsCount());
		urlParams.add("iudRowsCount",        sqlwUsageInfo.getIudRowsCount());
		urlParams.add("sqlWarningCount",     sqlwUsageInfo.getSqlWarningCount());
		urlParams.add("sqlExceptionCount",   sqlwUsageInfo.getSqlExceptionCount());

		List<QueryString> sendQueryList = new ArrayList<QueryString>();
		sendQueryList.add(urlParams);
//		return urlParams;
		return sendQueryList;
	}

	
	//-----------------------------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------------
	@Override
	public List<QueryString> createSendMdaInfo(Object... params)
	{
		// TODO Auto-generated method stub
		return null;
	}

	
	//-----------------------------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------------
	@Override
	public QueryString createSendUdcInfo(Object... params)
	{
		// TODO Auto-generated method stub
		return null;
	}

	
	//-----------------------------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------------
	@Override
//	public QueryString createSendLogInfo(Log4jLogRecord record, int sendLogInfoCount)
	public QueryString createSendLogInfo(Object... params)
	{
//		Log4jLogRecord record = (Log4jLogRecord) params[0];
//		int sendLogInfoCount  = (Integer)        params[1];

		// TODO Auto-generated method stub
		return null;
	}




	public static class SqlwConnectInfo
	{
		private final int           _connType; 
		private final String        _connTypeStr;
		private       String        _prodName          = "";
		private       String        _prodVersionStr    = ""; // from jdbc metadata
		private       String        _jdbcDriverName    = "";
		private       String        _jdbcDriverVersion = "";
		private       String        _jdbcDriver        = "";
		private       String        _jdbcUrl           = "";
		private       int           _srvVersionInt     = 0;
		private       String        _srvName           = "";
		private       String        _srvUser           = "";
		private       String        _srvPageSizeInKb   = "";
		private       String        _srvCharset        = "";
		private       String        _srvSortorder      = "";
		private       SshTunnelInfo _sshInfo           = null;

		public SqlwConnectInfo(int connType)
		{
			_connType = connType;
			if      (_connType == ConnectionDialog.TDS_CONN)     _connTypeStr = "TDS";
			else if (_connType == ConnectionDialog.OFFLINE_CONN) _connTypeStr = "OFFLINE";
			else if (_connType == ConnectionDialog.JDBC_CONN)    _connTypeStr = "JDBC";
			else                                                 _connTypeStr = "UNKNOWN("+connType+")";
		}
		public int           getConnTypeInt      () { return _connType; }
		public String        getConnTypeStr      () { return _connTypeStr       == null ? "UNKNOWN" : _connTypeStr; }
		
		public String        getProdName         () { return _prodName          == null ? "" : _prodName         .trim(); }
		public String        getProdVersionStr   () { return _prodVersionStr    == null ? "" : _prodVersionStr   .trim(); }
		public String        getJdbcDriverName   () { return _jdbcDriverName    == null ? "" : _jdbcDriverName   .trim(); }
		public String        getJdbcDriverVersion() { return _jdbcDriverVersion == null ? "" : _jdbcDriverVersion.trim(); }
		public String        getJdbcDriver       () { return _jdbcDriver        == null ? "" : _jdbcDriver       .trim(); }
		public String        getJdbcUrl          () { return _jdbcUrl           == null ? "" : _jdbcUrl          .trim(); }
		public int           getSrvVersionInt    () { return _srvVersionInt; }
		public String        getSrvName          () { return _srvName           == null ? "" : _srvName          .trim(); }
		public String        getSrvUser          () { return _srvUser           == null ? "" : _srvUser          .trim(); }
		public String        getSrvPageSIzeInKb  () { return _srvPageSizeInKb   == null ? "" : _srvPageSizeInKb  .trim(); }
		public String        getSrvCharset       () { return _srvCharset        == null ? "" : _srvCharset       .trim(); }
		public String        getSrvSortorder     () { return _srvSortorder      == null ? "" : _srvSortorder     .trim(); }
		public SshTunnelInfo getSshTunnelInfo    () { return _sshInfo; }
		public String        getSshTunnelInfoStr () { return _sshInfo           == null ? "" : _sshInfo.getInfoString(); }

		public void setProdName         (String str)            { _prodName          = str; }
		public void setProdVersionStr   (String str)            { _prodVersionStr    = str; }
		public void setJdbcDriverName   (String str)            { _jdbcDriverName    = str; }
		public void setJdbcDriverVersion(String str)            { _jdbcDriverVersion = str; }
		public void setJdbcDriver       (String str)            { _jdbcDriver        = str; }
		public void setJdbcUrl          (String str)            { _jdbcUrl           = str; }
		public void setSrvVersionInt    (int    ver)            { _srvVersionInt     = ver; }
		public void setSrvName          (String str)            { _srvName           = str; }
		public void setSrvUser          (String str)            { _srvUser           = str; }
		public void setSrvPageSizeInKb  (String str)            { _srvPageSizeInKb   = str; }
		public void setSrvCharset       (String str)            { _srvCharset        = str; }
		public void setSrvSortorder     (String str)            { _srvSortorder      = str; }
		public void setSshTunnelInfo    (SshTunnelInfo sshInfo) { _sshInfo           = sshInfo; }
	}

	public static class SqlwUsageInfo
	{
		private int    _connType          = 0;
		private String _prodName          = "";
		private int    _srvVersionInt     = 0;

		private long   _connectTime       = 0;
		private long   _disconnectTime    = 0;

		private int    _execMainCount     = 0;
		private int    _execBatchCount    = 0;
		private long   _execTimeTotal     = 0;
		private long   _execTimeSqlExec   = 0;
		private long   _execTimeRsRead    = 0;
		private long   _execTimeOther     = 0;
		private int    _rsCount           = 0;
		private int    _rsRowsCount       = 0;
		private int    _iudRowsCount      = 0;
		private int    _sqlWarningCount   = 0;
		private int    _sqlExceptionCount = 0;

		
		public void setConnType         (int    val) { _connType          = val; }
		public void setProductName      (String val) { _prodName          = val; }
		public void setSrvVersionInt    (int    val) { _srvVersionInt     = val; }

		public void setConnectTime      (long   val) { _connectTime       = val; }
		public void setDisconnectTime   (long   val) { _disconnectTime    = val; }
		
		public void setExecMainCount    (int    val) { _execMainCount     = val; }
		public void setExecBatchCount   (int    val) { _execBatchCount    = val; }
		public void setExecTimeTotal    (long   val) { _execTimeTotal     = val; }
		public void setExecTimeSqlExec  (long   val) { _execTimeSqlExec   = val; }
		public void setExecTimeRsRead   (long   val) { _execTimeRsRead    = val; }
		public void setExecTimeOther    (long   val) { _execTimeOther     = val; }
		public void setRsCount          (int    val) { _rsCount           = val; }
		public void setRsRowsCount      (int    val) { _rsRowsCount       = val; }
		public void setIudRowsCount     (int    val) { _iudRowsCount      = val; }
		public void setSqlWarningCount  (int    val) { _sqlWarningCount   = val; }
		public void setSqlExceptionCount(int    val) { _sqlExceptionCount = val; }


		public int    getConnType         () { return _connType; }
		public String getProductName      () { return _prodName; }
		public int    getSrvVersionInt    () { return _srvVersionInt; }

		public long   getConnectTime      () { return _connectTime; }
		public long   getDisconnectTime   () { return _disconnectTime; }

		public int    getExecMainCount    () { return _execMainCount; }
		public int    getExecBatchCount   () { return _execBatchCount; }
		public long   getExecTimeTotal    () { return _execTimeTotal; }
		public long   getExecTimeSqlExec  () { return _execTimeSqlExec; }
		public long   getExecTimeRsRead   () { return _execTimeRsRead; }
		public long   getExecTimeOther    () { return _execTimeOther; }
		public int    getRsCount          () { return _rsCount; }
		public int    getRsRowsCount      () { return _rsRowsCount; }
		public int    getIudRowsCount     () { return _iudRowsCount; }
		public int    getSqlWarningCount  () { return _sqlWarningCount; }
		public int    getSqlExceptionCount() { return _sqlExceptionCount; }
		
		public String getConnTypeStr() 
		{
			if      (_connType == ConnectionDialog.TDS_CONN)     return "TDS";
			else if (_connType == ConnectionDialog.OFFLINE_CONN) return "OFFLINE";
			else if (_connType == ConnectionDialog.JDBC_CONN)    return "JDBC";
			else                                                 return "UNKNOWN("+_connType+")";
		}
	}
}

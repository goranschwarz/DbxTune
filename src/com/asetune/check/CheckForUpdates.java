package com.asetune.check;

public class CheckForUpdates
{
}
///**
// * Connect to a URL and check if there are later software versions available.
// * <p>
// * It tries to use Proxy Servers for http/https if this is specified for the
// * cleint... (atleast for Windows and Gnome) see <code>ProxySelector</code>
// * for more info about this.
// * <p>
// * Please call <code>init()</code> method before any connect attemt is done
// * by any class that uses the network. Otherwise the <code>DefaultProxySelector</code>
// * implementation is initialized before the System Property <code>java.net.useSystemProxies</code>
// * is set. This means that we wont use this property, which means that we wont
// * be able to use the System Settings for Proxy servers.
// * <p>
// * Our own <code>ProxySelector</code> passes http/https protocolls to the
// * <code>DefaultProxySelector</code>...
// * <p>
// * NOTE: the DefaultProxySelector.(native).getSystemProxy()<br>
// * does NOT work with PAC files it just checks for static proxy settings<br>
// * http://sun.calstatela.edu/~cysun/documentation/java/1.5.0-source/j2se/src/windows/native/sun/net/spi/DefaultProxySelector.c
// * for more details...<br>
// * Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\ProxyEnable
// * If this is set to 0, then the whole thing is more or less not used...
// *
// * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
// */
//public class CheckForUpdates
//{
//	private static Logger _logger = Logger.getLogger(CheckForUpdates.class);
//
////	private static final boolean _printDevTrace = false;
//
//	protected static final String ASETUNE_HOME_URL               = "http://www.asetune.com";
//	private   static final String ASETUNE_CHECK_UPDATE_URL       = "http://www.asetune.com/check_for_update.php";
//	private   static final String ASETUNE_CONNECT_INFO_URL       = "http://www.asetune.com/connect_info.php";
//	private   static final String ASETUNE_MDA_INFO_URL           = "http://www.asetune.com/mda_info.php";
//	private   static final String ASETUNE_UDC_INFO_URL           = "http://www.asetune.com/udc_info.php";
//	private   static final String ASETUNE_COUNTER_USAGE_INFO_URL = "http://www.asetune.com/counter_usage_info.php";
//	private   static final String ASETUNE_ERROR_INFO_URL         = "http://www.asetune.com/error_info.php";
//
//	private   static final String SQLWIN_CHECK_UPDATE_URL        = "http://www.asetune.com/sqlw_check_for_update.php";
//	private   static final String SQLWIN_CONNECT_INFO_URL        = "http://www.asetune.com/sqlw_connect_info.php";
//	private   static final String SQLWIN_COUNTER_USAGE_INFO_URL  = "http://www.asetune.com/sqlw_counter_usage_info.php";
//
//	private static final String DEFAULT_DOWNLOAD_URL =  "http://www.asetune.com/download.html";
//	private static final String DEFAULT_WHATSNEW_URL =  "http://www.asetune.com/history.html";
//
//
//	private static boolean _sendConnectInfo      = true;
//	private static boolean _sendMdaInfo          = true;
//	private static int     _sendMdaInfoBatchSize = 20;
//	private static boolean _sendUdcInfo          = true;
//	private static boolean _sendCounterUsageInfo = true;
//	private static boolean _sendLogInfoWarning   = false;
//	private static boolean _sendLogInfoError     = false;
//
//	private static int     _connectCount         = 0;
//
//	private static int     _sendLogInfoThreshold = 100;
//	private static int     _sendLogInfoCount     = 0;
//
//	/** What PHP variables will be used to pick up variables <br>
//	 *  true  = _POST[], HTTP POST will be used, "no" limit in size... <br>
//	 *  false = _GET[], send as: http://www.site.com?param1=var1&param2=var2 approximately 2000 chars is max<br>
//	 */
//	private boolean        _useHttpPost    = false;
//	// Note: when redirecting URL from www.asetune.com -> www.asemon.se, it looses the POST entries
//	//       So right now we need to use the 'http://www.site.com?param1=val1&param2=val2' instead
//
////	private URL	           _url;
//	private String         _action        = "";
//
//	// The below is protected, just because test purposes, it should be private
//	protected String       _newAppVersion  = "";
//	protected String       _downloadUrl    = "";
//	protected String       _whatsNewUrl    = "";
//	protected boolean      _hasUpgrade;
//	protected boolean      _checkSucceed;
//	protected String       _responseString = "";
//
//	protected String       _feedbackUrl     = "";
//	protected String       _feedbackDateStr = "";
//	protected java.util.Date _feedbackDate  = null;
//
//	private static boolean _initialized = false;
//
//	private static int     _checkId     = -1;
//	private static int     _checkIdSqlW = -1;
//
//	private final static int DEFAULT_TIMEOUT = 20*1000;
//
////	static
////	{
////		_logger.setLevel(Level.DEBUG);
////	}
//
//	/*---------------------------------------------------
//	** BEGIN: constructors
//	**---------------------------------------------------
//	*/
//	public CheckForUpdates()
//	{
//	}
////	public CheckForUpdates(URL url)
////	{
////		if (!url.getProtocol().toLowerCase().startsWith("http"))
////		{
////			throw new IllegalArgumentException("Posting only works for http URLs");
////		}
////		this.url = url;
////	}
//	/*---------------------------------------------------
//	** END: constructors
//	**---------------------------------------------------
//	*/
//
//
//	/*---------------------------------------------------
//	** BEGIN: private helper methods
//	**---------------------------------------------------
//	*/
//
//	/**
//	 * Send a HTTP data fields via X number of parameters<br>
//	 * The request would look like: http://www.some.com?param1=val1&param2=val2<br>
//	 * in a PHP page they could be picked up by: _GET['param1']<br>
//	 * NOTE: there is a limit at about 2000 character for this type.
//	 */
//	private InputStream sendHttpParams(String urlStr, QueryString urlParams)
//	throws MalformedURLException, IOException
//	{
//		return sendHttpParams(urlStr, urlParams, DEFAULT_TIMEOUT);
//	}
//	private InputStream sendHttpParams(String urlStr, QueryString urlParams, int timeoutInMs)
//	throws MalformedURLException, IOException
//	{
//		if (_logger.isDebugEnabled())
//		{
//			_logger.debug("sendHttpParams() BASE URL: "+urlStr);
//			_logger.debug("sendHttpParams() PARAMSTR: "+urlParams.toString());
//		}
//
//		// Get the URL
//		URL url = new URL( urlStr + "?" + urlParams.toString() );
//
//		// open the connection and prepare it to POST
//		URLConnection conn = url.openConnection();
//		conn.setConnectTimeout(timeoutInMs); 
//
//		// Return the response
//		return conn.getInputStream();
//	}
//
//
//	/**
//	 * Send a HTTP data fields via POST handling<br>
//	 * The request would look like: http://www.some.com?param1=val1&param2=val2<br>
//	 * in a PHP page they could be picked up by: _POST['param1']
//	 */
//	private InputStream sendHttpPost(String urlStr, QueryString urlParams)
//	throws MalformedURLException, IOException
//	{
//		return sendHttpPost(urlStr, urlParams, DEFAULT_TIMEOUT);
//	}
//	private InputStream sendHttpPost(String urlStr, QueryString urlParams, int timeoutInMs)
//	throws MalformedURLException, IOException
//	{
//		if (_logger.isDebugEnabled())
//		{
//			_logger.debug("sendHttpPost() BASE URL: "+urlStr);
//			_logger.debug("sendHttpPost() POST STR: "+urlParams.toString());
//		}
//
//		// Get the URL
//		URL url = new URL(urlStr);
//
//		// open the connection and prepare it to POST
//		URLConnection conn = url.openConnection();
//		conn.setConnectTimeout(timeoutInMs); 
//		conn.setDoOutput(true);
//		OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), "ASCII");
//
//		// The POST line, the Content-type header,
//		// and the Content-length headers are sent by the URLConnection.
//		// We just need to send the data
//		out.write(urlParams.toString());
//		out.write("\r\n");
//		out.flush();
//		out.close();
//
//		// Return the response
//		return conn.getInputStream();
//	}
//	/*---------------------------------------------------
//	** END: private helper methods
//	**---------------------------------------------------
//	*/
//
//
//
//	/*---------------------------------------------------
//	** BEGIN: public methods
//	**---------------------------------------------------
//	*/
//	/**
//	 * Initialize the class<br>
//	 * This might have to be done before using any other connect() method,
//	 * If the <code>DefaultProxySelector</code> is initialized by someone else
//	 * and the system proerty <code>java.net.useSystemProxies</code> is not TRUE
//	 * then it wont use System Proxy Settings.
//	 */
//	static
//	{
////		_logger.setLevel(Level.TRACE);
////		_logger.setLevel(Level.DEBUG);
//		init();
//	}
//
//	public static void init()
//	{
//		if (_initialized)
//			return;
//
//		// In JDK 5, we can make the JDK go and get if we use proxies
//		// for specific URL:s, just set the below property...
//		System.setProperty("java.net.useSystemProxies", "true");
//
//		// Create our own ProxySelector, but remember the Default one.
//		OnlyHttpProxySelector ps = new OnlyHttpProxySelector(ProxySelector.getDefault());
//		ProxySelector.setDefault( ps );
//
//		_initialized = true;
//	}
//
//
//	/**
//	 * Check for a later version of the software
//	 * @param owner A JFrame or similar
//	 * @param showNoUpgrade Even if no upgrade is available, show info about that in a GUI message.
//	 */
//	public static void noBlockCheck(final Component owner, final boolean showNoUpgrade, final boolean showFailure)
//	{
//		checkForUpdate(owner, showNoUpgrade, showFailure, DEFAULT_TIMEOUT, false);
//	}
//	/**
//	 * Check for a later version of the software, in a blocking manner
//	 * @param number of milliseconds to wait
//	 */
//	public static void blockCheck(final int timeout)
//	{
//		checkForUpdate(null, false, false, DEFAULT_TIMEOUT, true);
//	}
//
//	/**
//	 * Check for a later version of the software
//	 * @param owner A JFrame or similar
//	 * @param showNoUpgrade Even if no upgrade is available, show info about that in a GUI message.
//	 */
//	private static void checkForUpdate(final Component owner, final boolean showNoUpgrade, final boolean showFailure, final int timeout, boolean blockWhileCheckinh)
//	{
//		Runnable checkLater = new Runnable()
//		{
//			@Override
//			public void run()
//			{
//				CheckForUpdates chk = new CheckForUpdates();
//
//				// Go and check
//				chk.check(timeout);
//
//				if (! chk.checkSucceed())
//				{
//					if (owner != null && showFailure)
//						CheckDialog.showDialog(owner, chk);
//
//					_logger.info("Check for latest version failed.");
//					return;
//				}
//
//				if (chk.hasUpgrade())
//				{
//					if (owner != null)
//						CheckDialog.showDialog(owner, chk);
//
//					_logger.info("New Upgrade is Available. New version is '"+chk.getNewAppVersionStr()+"' " +
//							"and can be downloaded here '"+chk.getDownloadUrl()+"'.");
//				}
//				else
//				{
//					if (showNoUpgrade)
//					{
//						if (owner != null)
//							CheckDialog.showDialog(owner, chk);
//					}
//
//					if (chk.hasFeedback())
//					{
//						if (owner != null)
//							CheckDialog.showDialog(owner, chk);
//					}
//
//					if (chk.isResponseOfHtml())
//					{
//						if (owner != null)
//							CheckDialog.showDialog(owner, chk);
//					}
//					else
//					{
//						_logger.info("You have got the latest release of '"+Version.getAppName()+"'.");
//					}
//				}
//			}
//		};
//		if (blockWhileCheckinh)
//		{
//			checkLater.run();
//		}
//		else
//		{
//			Thread checkThread = new Thread(checkLater);
//			checkThread.setName("checkForUpdates");
//			checkThread.setDaemon(true);
//			checkThread.start();
//		}
//	}
//
//	/**
//	 * Go and check for updates
//	 */
//	public void check(int timeout)
//	{
//		// URS to use
//		String urlStr = ASETUNE_CHECK_UPDATE_URL;
//
//		_hasUpgrade   = false;
//		_checkSucceed = false;
//
//		// COMPOSE: parameters to send to HTTP server
//		QueryString urlParams = new QueryString();
//
//		Date timeNow = new Date(System.currentTimeMillis());
//		String clientTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);
//
//		String appStartupTime = TimeUtils.msToTimeStr("%MM:%SS.%ms", System.currentTimeMillis() - AseTune.getStartTime());
//
//		if (_logger.isDebugEnabled())
//			urlParams.add("debug",    "true");
//
//		urlParams.add("clientCheckTime",     clientTime);
//
//		urlParams.add("clientSourceDate",     Version.getSourceDate());
//		urlParams.add("clientSourceVersion",  Version.getSourceRev());
//		urlParams.add("clientAseTuneVersion", Version.getVersionStr());
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
//		urlParams.add("gui",                AseTune.hasGUI()+"");
//
//		urlParams.add("java_version",       System.getProperty("java.version"));
//		urlParams.add("java_vm_version",    System.getProperty("java.vm.version"));
//		urlParams.add("java_vm_vendor",     System.getProperty("java.vm.vendor"));
//		urlParams.add("sun_arch_data_model",System.getProperty("sun.arch.data.model"));
//		urlParams.add("java_home",          System.getProperty("java.home"));
//		if (_useHttpPost)
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
//		try
//		{
//			// SEND OFF THE REQUEST
//			InputStream in;
//			if (_useHttpPost)
//				in = sendHttpPost(urlStr, urlParams, timeout);
//			else
//				in = sendHttpParams(urlStr, urlParams, timeout);
//
//			//----------------------------------------------
//			// This is how a response would look like
//			//----------------------------------------------
//			// ACTION:UPGRADE:$ASETUNE_LATEST_VERSION_STR:$DOWNLOAD_URL"
//			// ACTION:NO-UPGRADE
//			//----------------------------------------------
//
//			_action        = "";
//			_newAppVersion = "";
//			_downloadUrl   = DEFAULT_DOWNLOAD_URL;
//			_whatsNewUrl   = DEFAULT_WHATSNEW_URL;
//
//			LineNumberReader lr = new LineNumberReader(new InputStreamReader(in));
//			String line;
//			String responseLines = "";
//			boolean foundActionLine = false;
//			while ((line = lr.readLine()) != null)
//			{
//				_logger.debug("response line "+lr.getLineNumber()+": " + line);
//				responseLines += line;
//				if (line.startsWith("ACTION:"))
//				{
//					foundActionLine = true;
//					String[] sa = line.split(":");
//					for (int i=0; i<sa.length; i++)
//					{
//						_logger.debug("   - STRING["+i+"]='"+sa[i]+"'.");
//
//						if (i == 1) _action        = sa[1];
//						if (i == 2) _newAppVersion = sa[2];
//						if (i == 3) _downloadUrl   = sa[3];
//						if (i == 4) _whatsNewUrl   = sa[4];
//					}
//				}
//				if (line.startsWith("OPTIONS:"))
//				{
//					// OPTIONS: sendConnectInfo = true|false, sendUdcInfo = true|false, sendCounterUsageInfo = true|false
//					String options = line.substring("OPTIONS:".length());
//					_logger.debug("Receiving Options from server '"+options+"'.");
//					String[] sa = options.split(",");
//					for (int i=0; i<sa.length; i++)
//					{
//						String[] keyVal = sa[i].split("=");
//						if (keyVal.length == 2)
//						{
//							String key = keyVal[0].trim();
//							String val = keyVal[1].trim();
//							boolean bVal = val.equalsIgnoreCase("true");
//
//							if (key.equalsIgnoreCase("sendConnectInfo"))
//							{
//								_sendConnectInfo = bVal;
//								_logger.debug("Setting option '"+key+"' to '"+bVal+"'.");
//							}
//							else if (key.equalsIgnoreCase("sendUdcInfo"))
//							{
//								_sendUdcInfo = bVal;
//								_logger.debug("Setting option '"+key+"' to '"+bVal+"'.");
//							}
//							else if (key.equalsIgnoreCase("sendMdaInfo"))
//							{
//								_sendMdaInfo = bVal;
//								_logger.debug("Setting option '"+key+"' to '"+bVal+"'.");
//							}
//							else if (key.equalsIgnoreCase("sendMdaInfoBatchSize"))
//							{
//								try
//								{
//									int intVal = Integer.parseInt(val);
//									_sendMdaInfoBatchSize = intVal;
//									_logger.debug("Setting option '"+key+"' to '"+intVal+"'.");
//								}
//								catch (NumberFormatException ex)
//								{
//									_logger.warn("Problems reading option '"+key+"', with value '"+val+"'. Can't convert to Integer. Caught: "+ex);
//								}
//							}
//							else if (key.equalsIgnoreCase("sendCounterUsageInfo"))
//							{
//								_sendCounterUsageInfo = bVal;
//								_logger.debug("Setting option '"+key+"' to '"+bVal+"'.");
//							}
//							else if (key.equalsIgnoreCase("sendLogInfoWarning"))
//							{
//								_sendLogInfoWarning = bVal;
//								_logger.debug("Setting option '"+key+"' to '"+bVal+"'.");
//							}
//							else if (key.equalsIgnoreCase("sendLogInfoError"))
//							{
//								_sendLogInfoError = bVal;
//								_logger.debug("Setting option '"+key+"' to '"+bVal+"'.");
//							}
//							else if (key.equalsIgnoreCase("sendLogInfoThreshold"))
//							{
//								try
//								{
//									_sendLogInfoThreshold = Integer.parseInt(val);
//									_logger.debug("Setting option '"+key+"' to '"+val+"'.");
//								}
//								catch (NumberFormatException e)
//								{
//									_logger.debug("NumberFormatException: Setting option '"+key+"' to '"+val+"'.");
//								}
//							}
//							else
//							{
//								_logger.debug("Unknown option '"+key+"' from server with value '"+val+"'.");
//							}
//						}
//						else
//							_logger.debug("Option '"+sa[i]+"' from server has strange key/valye.");
//					}
//				}
//				if (line.startsWith("FEEDBACK:"))
//				{
//					// OPTIONS: sendConnectInfo = true|false, sendUdcInfo = true|false, sendCounterUsageInfo = true|false
//					String feedback = line.substring("FEEDBACK:".length()).trim();
//					_logger.debug("Receiving feedback from server '"+feedback+"'.");
//
//					if ( ! "".equals(feedback) )
//					{
//						String[] sa = feedback.split(":");
//						for (int i=0; i<sa.length; i++)
//						{
//							if (i == 0) _feedbackDateStr = sa[0];
//							if (i == 1) _feedbackUrl     = sa[1];
//						}
//						try
//						{
//							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//							_feedbackDate = sdf.parse(_feedbackDateStr);
//						}
//						catch (ParseException e)
//						{
//						}
//					}
//				}
//				if (line.startsWith("ERROR:"))
//				{
//					_logger.warn("When checking for new version, found an 'ERROR:' response row, which looked like '" + line + "'.");
//				}
//				if (line.startsWith("CHECK_ID:"))
//				{
//					String actionResponse = line.substring( "CHECK_ID:".length() ).trim();
//					try { _checkId = Integer.parseInt(actionResponse); }
//					catch (NumberFormatException ignore) {}
//					_logger.debug("Received check_id='"+_checkId+"' from update site.");
//				}
//			}
//			in.close();
//			_responseString = responseLines;
//
//			// if not empty, check that it starts with 'http://' otherwise add it to the start
//			if ( _downloadUrl != null && !_downloadUrl.trim().equals("") )
//			{
//				if ( ! _downloadUrl.startsWith("http://") )
//					_downloadUrl = "http://" + _downloadUrl;
//			}
//
//			// if not empty, check that it starts with 'http://' otherwise add it to the start
//			if ( _whatsNewUrl != null && !_whatsNewUrl.trim().equals("") )
//			{
//				if ( ! _whatsNewUrl.startsWith("http://") )
//					_whatsNewUrl = "http://" + _whatsNewUrl;
//			}
//
//			// if not empty, check that it starts with 'http://' otherwise add it to the start
//			if ( _feedbackUrl != null && !_feedbackUrl.trim().equals("") )
//			{
//				if ( ! _feedbackUrl.startsWith("http://") )
//					_feedbackUrl = "http://" + _feedbackUrl;
//			}
//
//			if (_action.equals("UPGRADE"))
//			{
//				_hasUpgrade = true;
//				_logger.debug("-UPGRADE-");
//				_logger.debug("-to:"+_newAppVersion);
//				_logger.debug("-at:"+_downloadUrl);
//				_logger.debug("-at:"+_whatsNewUrl);
//			}
//			else
//			{
//				_hasUpgrade = false;
//				_logger.debug("-NO-UPGRADE-");
//			}
//
//			if ( ! foundActionLine )
//			{
//				_logger.warn("When checking for new version, no 'ACTION:' response was found. The response rows was '" + responseLines + "'.");
//			}
//
//			_checkSucceed = true;
//		}
//		catch (IOException ex)
//		{
//			_logger.debug("When we checking for later version, we had problems", ex);
//		}
//	}
//
//	/**
//	 * Did the check succeed
//	 */
//	public boolean checkSucceed()
//	{
//		return _checkSucceed;
//	}
//
//	/**
//	 * What did the server responded with
//	 */
//	public String getResponseString()
//	{
//		return _responseString;
//	}
//	public boolean isResponseOfHtml()
//	{
//		if (StringUtil.isNullOrBlank(_responseString))
//			return false;
//		
//		if (_responseString.indexOf("<html>") >= 0)
//			return true;
//		if (_responseString.indexOf("<HTML>") >= 0)
//			return true;
//
//		return false;
//	}
//
//	/**
//	 * If there is a newer release for download, this would return true
//	 */
//	public boolean hasUpgrade()
//	{
//		return _hasUpgrade;
//	}
//
//	/**
//	 * If there is an upgrade available, what version is the new one
//	 */
//	public String  getNewAppVersionStr()
//	{
//		return _newAppVersion;
//	}
//
//	/**
//	 * If there is an upgrade available, where do we download it
//	 */
//	public String  getDownloadUrl()
//	{
//		if (_downloadUrl == null || _downloadUrl.trim().equals(""))
//			return DEFAULT_DOWNLOAD_URL;
//		return _downloadUrl;
//	}
//
//	/**
//	 * @return
//	 */
//	public String getWhatsNewUrl()
//	{
//		if (_whatsNewUrl == null || _whatsNewUrl.trim().equals(""))
//			return DEFAULT_WHATSNEW_URL;
//		return _whatsNewUrl;
//	}
//
//	/**
//	 * @return null if no feedback URL was found, otherwise it's the URL where to fetch the feedback.
//	 */
//	public String getFeedbackUrl()
//	{
//		if (_feedbackUrl == null || _feedbackUrl.trim().equals(""))
//			return null;
//		return _feedbackUrl;
//	}
//
//	/**
//	 * If there is a feedback page, this would return true
//	 */
//	public boolean hasFeedback()
//	{
//		return (getFeedbackUrl() != null);
//	}
//
//	/**
//	 * Get last feedback time.
//	 * @return
//	 */
//	public long getFeedbackTime()
//	{
//		if (_feedbackDate == null)
//			return 0;
//
//		return _feedbackDate.getTime();
//	}
//
//	/**
//	 * what URL did we check at?
//	 * @return
//	 */
////	public URL getURL()
////	{
////		return _url;
////	}
//
//
//	/**
//	 * @param connType ConnectionDialog.TDS_CONN | ConnectionDialog.OFFLINE_CONN
//	 * @param sshTunnelInfo 
//	 */
//	public static void sendConnectInfoNoBlock(final int connType, final SshTunnelInfo sshTunnelInfo)
//	{
//		// TRACE IN DEVELOPMENT
////		if (_printDevTrace && _checkId < 0)
////		{
////			// well, this should NOT really be done here, but in test it's hopefully OK
////			_connectCount++;
////
////			System.out.println("DEV-TRACE(CheckForUpdates): sendConnectInfoNoBlock(): _connectInfoCount="+_connectCount);
////			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
////			if (mtd != null)
////			{
////				String srvVersion       = mtd.aseVersionNum + "";
////				String isClusterEnabled = mtd.isClusterEnabled + "";
////
////				System.out.println("DEV-TRACE(CheckForUpdates): sendConnectInfoNoBlock(): srvVersion="+srvVersion+", isClusterEnabled="+isClusterEnabled+".");
////			}
////		}
//
//		if ( ! _sendConnectInfo )
//		{
//			_logger.debug("Send 'Connect info' has been disabled.");
//			return;
//		}
//
//		Runnable doLater = new Runnable()
//		{
//			@Override
//			public void run()
//			{
//				CheckForUpdates connInfo = new CheckForUpdates();
//				connInfo.sendConnectInfo(connType, sshTunnelInfo);
//
//				CheckForUpdates udcInfo = new CheckForUpdates();
//				udcInfo.sendUdcInfo();
//			}
//		};
//		Thread checkThread = new Thread(doLater);
//		checkThread.setName("sendConnectInfo");
//		checkThread.setDaemon(true);
//		checkThread.start();
//	}
//
//	/**
//	 * Send info on connection
//	 * @param sshTunnelInfo 
//	 */
//	public void sendConnectInfo(final int connType, SshTunnelInfo sshTunnelInfo)
//	{
//		// URL TO USE
//		String urlStr = ASETUNE_CONNECT_INFO_URL;
//
//		if ( ! _sendConnectInfo )
//		{
//			_logger.debug("Send 'Connect info' has been disabled.");
//			return;
//		}
//
//		if (_checkId < 0)
//		{
//			_logger.debug("No checkId was discovered when trying to send connection info, skipping this.");
//			return;
//		}
//
//		if ( ! MonTablesDictionary.hasInstance() )
//		{
//			_logger.debug("MonTablesDictionary not initialized when trying to send connection info, skipping this.");
//			return;
//		}
//		MonTablesDictionary mtd = MonTablesDictionary.getInstance();
//		if (mtd == null)
//		{
//			_logger.debug("MonTablesDictionary was null when trying to send connection info, skipping this.");
//			return;
//		}
//		
//		if (connType != ConnectionDialog.TDS_CONN && connType != ConnectionDialog.OFFLINE_CONN)
//		{
//			_logger.warn("ConnectInfo: Connection type must be TDS_CONN | OFFLINE_CONN");
//			return;
//		}
//		
//		_connectCount++;
//
//		// COMPOSE: parameters to send to HTTP server
//		QueryString urlParams = new QueryString();
//
//		Date timeNow = new Date(System.currentTimeMillis());
//
//		String checkId          = _checkId + "";
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
//			srvVersion       = mtd.getAseExecutableVersionNum() + "";
//			isClusterEnabled = mtd.isClusterEnabled() + "";
//
//			srvName          = AseConnectionFactory.getServer();
//			srvIpPort        = AseConnectionFactory.getHostPortStr();
//			srvUser          = AseConnectionFactory.getUser();
//			srvUserRoles     = "not_initialized";
//			srvVersionStr    = mtd.getAseExecutableVersionStr();
//			srvSortOrderId   = mtd.getAseSortId() + "";
//			srvSortOrderName = mtd.getAseSortName();
//			srvCharsetId     = mtd.getAseCharsetId() + "";
//			srvCharsetName   = mtd.getAseCharsetName();
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
//		urlParams.add("userName",            System.getProperty("user.name"));
//
//		urlParams.add("connectId",           _connectCount+"");
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
//		try
//		{
//			// SEND OFF THE REQUEST
//			InputStream in;
//			if (_useHttpPost)
//				in = sendHttpPost(urlStr, urlParams);
//			else
//				in = sendHttpParams(urlStr, urlParams);
//
//			LineNumberReader lr = new LineNumberReader(new InputStreamReader(in));
//			String line;
//			String responseLines = "";
//			while ((line = lr.readLine()) != null)
//			{
//				_logger.debug("response line "+lr.getLineNumber()+": " + line);
//				responseLines += line;
//				if (line.startsWith("ERROR:"))
//				{
//					_logger.warn("When doing connection info 'ERROR:' response row, which looked like '" + line + "'.");
//				}
//				if (line.startsWith("DONE:"))
//				{
//				}
//				if (line.startsWith("SEND_MDA_INFO:"))
//				{
//					_logger.info("Received info to collect MDA Information.");
//					sendMdaInfoNoBlock();
//				}
//			}
//			in.close();
//			_responseString = responseLines;
//
////			_checkSucceed = true;
//		}
//		catch (IOException ex)
//		{
//			_logger.debug("when trying to send connection info, we had problems", ex);
//		}
//	}
//
//
//
//
//
//
//	/**
//	 * 
//	 */
//	public static void sendMdaInfoNoBlock()
//	{
//		if ( ! _sendMdaInfo )
//		{
//			_logger.debug("Send 'MDA info' has been disabled.");
//			return;
//		}
//
//		Runnable doLater = new Runnable()
//		{
//			@Override
//			public void run()
//			{
//				CheckForUpdates connInfo = new CheckForUpdates();
//				connInfo.sendMdaInfo();
//			}
//		};
//		Thread checkThread = new Thread(doLater);
//		checkThread.setName("sendMdaInfo");
//		checkThread.setDaemon(true);
//		checkThread.start();
//	}
//
//	/**
//	 * Send info on connection
//	 */
//	public void sendMdaInfo()
//	{
//		// URL TO USE
//		String urlStr = ASETUNE_MDA_INFO_URL;
//
//		if ( ! _sendMdaInfo )
//		{
//			_logger.debug("Send 'MDA info' has been disabled.");
//			return;
//		}
//
//		if (_checkId < 0)
//		{
//			_logger.debug("No checkId was discovered when trying to send connection info, skipping this.");
//			return;
//		}
//
//		if ( ! MonTablesDictionary.hasInstance() )
//		{
//			_logger.debug("MonTablesDictionary not initialized when trying to send connection info, skipping this.");
//			return;
//		}
//		MonTablesDictionary mtd = MonTablesDictionary.getInstance();
//		if (mtd == null)
//		{
//			_logger.debug("MonTablesDictionary was null when trying to send connection info, skipping this.");
//			return;
//		}
//
//		if (mtd.getAseExecutableVersionNum() <= 0)
//		{
//			_logger.debug("MonTablesDictionary aseVersionNum is zero, stopping here.");
//			return;
//		}
//
////		if (mtd.installmasterVersionNum > 0 && mtd.aseVersionNum != mtd.installmasterVersionNum)
//		if (mtd.getAseMonTableVersionNum() > 0 && mtd.getAseExecutableVersionNum() != mtd.getAseMonTableVersionNum())
//		{
//			_logger.info("MonTablesDictionary aseVersionNum("+mtd.getAseExecutableVersionNum()+") and installmaster/monTables VersionNum("+mtd.getAseMonTableVersionNum()+") is not in sync, so we don't want to send MDA info about this.");
//			return;
//		}
//
////		if ( ! GetCounters.getInstance().isMonConnected() )
//		if ( ! CounterController.getInstance().isMonConnected() )
//		{
//			_logger.debug("No ASE Connection to the monitored server.");
//			return;
//		}
//
//		Date timeNow = new Date(System.currentTimeMillis());
//
//		String checkId          = _checkId + "";
//		String clientTime       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);
//
//		String srvVersion       = mtd.getAseExecutableVersionNum() + "";
//		String isClusterEnabled = mtd.isClusterEnabled() + "";		
//
//		// Keep the Query objects in a list, because the "MDA" info WILL have multiple sends.
//		List<QueryString> sendQueryList = new ArrayList<QueryString>();
//		int rowCountSum = 0;
//
//		// Get MDA information 
//		try
//		{
//			// monTables
//			String sql_monTables_rowCount = 
//				"select count(*) from master.dbo.monTables \n" +
////				(mtd.getMdaVersion() >= 15700 ? "where Language = 'en_US' \n" : "");
////				(mtd.getMdaVersion() >= 1570000 ? "where Language = 'en_US' \n" : "");
//				(mtd.getMdaVersion() >= Ver.ver(15,7) ? "where Language = 'en_US' \n" : "");
//
//			String sql_monTables = 
//				"select type='T', t.TableName, t.TableID, ColumnName='ColumnID=NumOfCols', ColumnID=t.Columns, TypeName='Length=NumOfParameters', Length=t.Parameters, t.Indicators, t.Description  \n" +
//			    "from master.dbo.monTables t \n" +
////				(mtd.getMdaVersion() >= 15700 ? "where Language = 'en_US' \n" : "");
////				(mtd.getMdaVersion() >= 1570000 ? "where Language = 'en_US' \n" : "");
//				(mtd.getMdaVersion() >= Ver.ver(15,7) ? "where Language = 'en_US' \n" : "");
//
//			// monTableColumns
//			String sql_monTableColumns_rowCount = 
//				"select count(*) from master.dbo.monTableColumns \n" +
////				(mtd.getMdaVersion() >= 15700 ? "where Language = 'en_US' \n" : "");
////				(mtd.getMdaVersion() >= 1570000 ? "where Language = 'en_US' \n" : "");
//				(mtd.getMdaVersion() >= Ver.ver(15,7) ? "where Language = 'en_US' \n" : "");
//
//			String sql_monTableColumns = 
//				"select type='C', c.TableName, c.TableID, c.ColumnName, c.ColumnID, c.TypeName, c.Length, c.Indicators, c.Description  \n" +
//			    "from master.dbo.monTableColumns c " +
////				(mtd.getMdaVersion() >= 15700 ? "where Language = 'en_US' \n" : "");
////				(mtd.getMdaVersion() >= 1570000 ? "where Language = 'en_US' \n" : "");
//				(mtd.getMdaVersion() >= Ver.ver(15,7) ? "where Language = 'en_US' \n" : "");
//			
//			// monTableParameters
//			String sql_monTableParameters_rowCount = 
//				"select count(*) from master.dbo.monTableParameters \n";
//
//			String sql_monTableParameters = 
//				"select type='P', p.TableName, p.TableID, p.ParameterName, p.ParameterID, p.TypeName, p.Length, Indicators=-1, p.Description  \n" +
//			    "from master.dbo.monTableParameters p \n";
//
//			// sysobjects
//			String sql_sysobjects_rowCount = 
//				"select count(*) \n" +
//				"from sysobjects o, syscolumns c \n" +
//				"where o.id = c.id \n" +
//				"  and o.name like 'mon%' \n" +
//				"  and o.type = 'U' \n" +
//				"";
//			String sql_sysobjects = 
//				"select type='S', TableName = o.name, TableID = o.id, ColumnName = c.name, ColumnID = c.colid, TypeName = t.name, Length = t.length, Indicators = -1, Description = '' \n" +
//				"from sysobjects o, syscolumns c, systypes t \n" +
//				"where o.id = c.id \n" +
//				"  and c.usertype = t.usertype \n" +
//				"  and o.name like 'mon%' \n" +
//				"  and o.type = 'U' \n" +
//				"order by o.name, c.colid \n" +
//				"";
//
//			// monTables
////			rowCountSum += getMdaInfo(GetCounters.getInstance().getMonConnection(), 
//			rowCountSum += getMdaInfo(CounterController.getInstance().getMonConnection(), 
//					checkId, clientTime, System.getProperty("user.name"), srvVersion, isClusterEnabled, 
//					sql_monTables_rowCount, sql_monTables, 
//					_sendMdaInfoBatchSize, sendQueryList);
//
//			// monTableColumns
////			rowCountSum += getMdaInfo(GetCounters.getInstance().getMonConnection(), 
//			rowCountSum += getMdaInfo(CounterController.getInstance().getMonConnection(), 
//					checkId, clientTime, System.getProperty("user.name"), srvVersion, isClusterEnabled, 
//					sql_monTableColumns_rowCount, sql_monTableColumns, 
//					_sendMdaInfoBatchSize, sendQueryList);
//
//			// monTableParameters
////			rowCountSum += getMdaInfo(GetCounters.getInstance().getMonConnection(), 
//			rowCountSum += getMdaInfo(CounterController.getInstance().getMonConnection(), 
//					checkId, clientTime, System.getProperty("user.name"), srvVersion, isClusterEnabled, 
//					sql_monTableParameters_rowCount, sql_monTableParameters, 
//					_sendMdaInfoBatchSize, sendQueryList);
//
//			// ASE System Tables
////			rowCountSum += getMdaInfo(GetCounters.getInstance().getMonConnection(), 
//			rowCountSum += getMdaInfo(CounterController.getInstance().getMonConnection(), 
//					checkId, clientTime, System.getProperty("user.name"), srvVersion, isClusterEnabled, 
//					sql_sysobjects_rowCount, sql_sysobjects, 
//					_sendMdaInfoBatchSize, sendQueryList);
//		}
//		catch (SQLException e)
//		{
//			sendQueryList.clear();
//			_logger.debug("Problems when getting MDA information. Caught: "+e, e);
//		}
//
//		if (sendQueryList.size() > 0)
//		{
//			_logger.info("sendMdaInfo: Starting to send "+rowCountSum+" MDA information entries in "+sendQueryList.size()+" batches, for ASE Version '"+mtd.getAseExecutableVersionNum()+"'.");
//			long startTime = System.currentTimeMillis();
//			for (QueryString urlEntry : sendQueryList)
//			{
//				try
//				{
//					// SEND OFF THE REQUEST
//					InputStream in;
//					if (_useHttpPost)
//						in = sendHttpPost(urlStr, urlEntry);
//					else
//						in = sendHttpParams(urlStr, urlEntry);
//		
//					LineNumberReader lr = new LineNumberReader(new InputStreamReader(in));
//					String line;
//					String responseLines = "";
//					while ((line = lr.readLine()) != null)
//					{
//						_logger.debug("response line "+lr.getLineNumber()+": " + line);
//						responseLines += line;
//						if (line.startsWith("ERROR:"))
//						{
//							_logger.warn("When doing MDA info 'ERROR:' response row, which looked like '" + line + "'.");
//						}
//						if (line.startsWith("DONE:"))
//						{
//						}
//					}
//					in.close();
//					_responseString = responseLines;
//				}
//				catch (IOException ex)
//				{
//					_logger.debug("when trying to send MDA info, we had problems", ex);
//				}
//			}
//			String execTimeStr = TimeUtils.msToTimeStr(System.currentTimeMillis() - startTime);
//			_logger.info("sendMdaInfo: this took '"+execTimeStr+"' for all "+sendQueryList.size()+" batches, rows send was "+rowCountSum+".");
//		}
//	}
//
//	private int getMdaInfo(
//			Connection conn,
//			String checkId, 
//			String clientTime, 
//			String userName, 
//			String srvVersionNum, 
//			String isClusterEnabled, 
//			String sqlGetCount, 
//			String sqlGetValues,
//			int batchSize,
//			List<QueryString> sendQueryList)
//	throws SQLException
//	{
//		Statement  stmt = conn.createStatement();
//		ResultSet  rs;
//
//		// get expected rows
//		int expectedRows = 0;
//		rs = stmt.executeQuery(sqlGetCount);
//		while ( rs.next() )
//			expectedRows = rs.getInt(1);
//		rs.close();
//
//		// get VALUES
//		rs = stmt.executeQuery(sqlGetValues);
//
//		int rowId        = 0;
//		int batchCounter = 0;
//		QueryString urlParams = new QueryString();
//
//		while ( rs.next() )
//		{
//			rowId++;
//
//			if (batchCounter == 0)
//			{
//				if (_logger.isDebugEnabled())
//					urlParams.add("debug",    "true");
//
//				urlParams.add("checkId",            checkId);
//				urlParams.add("clientTime",         clientTime);
//				urlParams.add("userName",           userName);
//
//				urlParams.add("srvVersion",         srvVersionNum);
//				urlParams.add("isClusterEnabled",   isClusterEnabled);
//
//				urlParams.add("expectedRows",       expectedRows+"");
//			}
//
//			urlParams.add("type"        + "-" + batchCounter, rs.getString(1)); // NOTE NOT yet added to PHP and database
//			urlParams.add("TableName"   + "-" + batchCounter, rs.getString(2));
//			urlParams.add("TableID"     + "-" + batchCounter, rs.getString(3));
//			urlParams.add("ColumnName"  + "-" + batchCounter, rs.getString(4));
//			urlParams.add("ColumnID"    + "-" + batchCounter, rs.getString(5));
//			urlParams.add("TypeName"    + "-" + batchCounter, rs.getString(6));
//			urlParams.add("Length"      + "-" + batchCounter, rs.getString(7));
//			urlParams.add("Indicators"  + "-" + batchCounter, rs.getString(8));
//			urlParams.add("Description" + "-" + batchCounter, rs.getString(9));
//
//			urlParams.add("rowId"       + "-" + batchCounter, rowId+"");
//
//			batchCounter++;
//
//			// start new batch OR on last row
//			if (batchCounter >= batchSize || rowId >= expectedRows)
//			{
//				// add number of records added to this entry
//				urlParams.add("batchSize",      batchCounter+"");
////System.out.println("QueryString: length="+urlParams.length()+", entries="+urlParams.entryCount()+".");
//
//				batchCounter = 0;
//				sendQueryList.add(urlParams);
//				urlParams = new QueryString();
//			}
//		}
//
//		rs.close();
//		stmt.close();
//		
//		return rowId;
//	}
//
//
//
//
//
//	/**
//	 */
//	public static void sendUdcInfoNoBlock()
//	{
//		// TRACE IN DEVELOPMENT
////		if (_printDevTrace && _checkId < 0)
////		{
////			System.out.println("DEV-TRACE(CheckForUpdates): sendUdcInfoNoBlock(): ");
////		}
//
//		if ( ! _sendUdcInfo )
//		{
//			_logger.debug("Send 'UDC info' has been disabled.");
//			return;
//		}
//
//		Runnable doLater = new Runnable()
//		{
//			@Override
//			public void run()
//			{
//				CheckForUpdates chk = new CheckForUpdates();
//
//				// Go and check
//				chk.sendUdcInfo();
//			}
//		};
//		Thread checkThread = new Thread(doLater);
//		checkThread.setName("sendUdcInfo");
//		checkThread.setDaemon(true);
//		checkThread.start();
//	}
//
//	/**
//	 * Send info on User Defined Counters
//	 */
//	public void sendUdcInfo()
//	{
//		// URL TO USE
//		String urlStr = ASETUNE_UDC_INFO_URL;
//
//		if ( ! _sendUdcInfo )
//		{
//			_logger.debug("Send 'UDC info' has been disabled.");
//			return;
//		}
//
//		if (_checkId < 0)
//		{
//			_logger.debug("No checkId was discovered when trying to send UDC info, skipping this.");
//			return;
//		}
//
//		Configuration conf = Configuration.getCombinedConfiguration();
//		if (conf == null)
//		{
//			_logger.debug("Configuration was null when trying to send UDC info, skipping this.");
//			return;
//		}
//
//		// COMPOSE: parameters to send to HTTP server
//		QueryString urlParams = new QueryString();
//
//		Date timeNow = new Date(System.currentTimeMillis());
//
//		String checkId          = _checkId + "";
//		String clientTime       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);
//
//		if (_logger.isDebugEnabled())
//			urlParams.add("debug",    "true");
//
//		urlParams.add("checkId",             checkId);
//		urlParams.add("clientTime",          clientTime);
//		urlParams.add("userName",            System.getProperty("user.name"));
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
//			return;
//		}
//
//		try
//		{
//			// SEND OFF THE REQUEST
//			InputStream in;
//			if (_useHttpPost)
//				in = sendHttpPost(urlStr, urlParams);
//			else
//				in = sendHttpParams(urlStr, urlParams);
//
//			LineNumberReader lr = new LineNumberReader(new InputStreamReader(in));
//			String line;
//			String responseLines = "";
//			while ((line = lr.readLine()) != null)
//			{
//				_logger.debug("response line "+lr.getLineNumber()+": " + line);
//				responseLines += line;
//				if (line.startsWith("ERROR:"))
//				{
//					_logger.warn("When doing UDC info 'ERROR:' response row, which looked like '" + line + "'.");
//				}
//				if (line.startsWith("DONE:"))
//				{
//				}
//			}
//			in.close();
//			_responseString = responseLines;
//
////			_checkSucceed = true;
//		}
//		catch (IOException ex)
//		{
//			_logger.debug("when trying to send UDC info, we had problems", ex);
//		}
//	}
//
//
//
//
//
//
//	/**
//	 */
////	private static boolean _sendCounterUsage_done = false;
//	private static int _sendCounterUsage_atConnectCount = 0;
//
//	public static void sendCounterUsageInfo(boolean blockingCall)
//	{
//		// TRACE IN DEVELOPMENT
////		if (_printDevTrace && _checkId < 0)
////		{
////			System.out.println("DEV-TRACE(CheckForUpdates): sendCounterUsageInfo(blockingCall="+blockingCall+"): _connectCount="+_connectCount+", _sendCounterUsage_atConnectCount="+_sendCounterUsage_atConnectCount+".");
////
////			if ( _sendCounterUsage_atConnectCount < _connectCount )
////			{
////				// WARN: this should only be done while testing
////				_sendCounterUsage_atConnectCount++;
////				for (CountersModel cm : GetCounters.getCmList())
////				{
////					if (cm.getRefreshCounter() >= 0)
////						System.out.println("DEV-TRACE(CheckForUpdates): sendCounterUsageInfoNoBlock(): name="+StringUtil.left(cm.getName(),20)+", getRefreshCounter="+cm.getRefreshCounter()+", getSumRowCount="+cm.getSumRowCount());
////
////					// WARN: this should only be done while testing
////					cm.resetStatCounters();
////				}
////			}
////			else
////				System.out.println("DEV-TRACE(CheckForUpdates): sendCounterUsageInfo(blockingCall="+blockingCall+"): already done (_sendCounterUsage_atConnectCount="+_sendCounterUsage_atConnectCount+", _connectCount="+_connectCount+").");
////				_logger.debug("sendCounterUsageInfo, already done (_sendCounterUsage_atConnectCount="+_sendCounterUsage_atConnectCount+", _connectCount="+_connectCount+").");
////		}
//
//		if ( ! _sendCounterUsageInfo )
//		{
//			_logger.debug("Send 'Counter Usage Info' has been disabled.");
//			return;
//		}
//
//		Runnable doLater = new Runnable()
//		{
//			@Override
//			public void run()
//			{
////				if ( ! _sendCounterUsage_done )
////				{
////					_sendCounterUsage_done = true;
//				if ( _sendCounterUsage_atConnectCount < _connectCount )
//				{
//					_sendCounterUsage_atConnectCount++;
//
//					CheckForUpdates chk = new CheckForUpdates();
//
//					// Go and check
//					chk.sendCounterUsageInfo();
//				}
//				else
//				{
//					_logger.debug("sendCounterUsageInfo, already done (_sendCounterUsage_atConnectCount="+_sendCounterUsage_atConnectCount+", _connectCount="+_connectCount+").");
//				}
//			}
//		};
//		if (blockingCall)
//		{
//			// if synchronous, just call the run method on current thread
//			doLater.run();
//		}
//		else
//		{
//			// no blocking call, start a thread that does it.
//			Thread checkThread = new Thread(doLater);
//			checkThread.setName("sendCounterUsageInfo");
//			checkThread.setDaemon(true);
//			checkThread.start();
//		}
//	}
//
//	/**
//	 * Send info on User Defined Counters
//	 */
//	public void sendCounterUsageInfo()
//	{
//		// URL TO USE
//		String urlStr = ASETUNE_COUNTER_USAGE_INFO_URL;
//
//		if ( ! _sendCounterUsageInfo )
//		{
//			_logger.debug("Send 'Counter Usage Info' has been disabled.");
//			return;
//		}
//
//		if (_checkId < 0)
//		{
//			_logger.debug("No checkId was discovered when trying to send 'Counter Usage' info, skipping this.");
//			return;
//		}
//
//		Configuration conf = Configuration.getCombinedConfiguration();
//		if (conf == null)
//		{
//			_logger.debug("Configuration was null when trying to send 'Counter Usage' info, skipping this.");
//			return;
//		}
//
//		// COMPOSE: parameters to send to HTTP server
//		QueryString urlParams = new QueryString();
//
////		Date timeNow = new Date(System.currentTimeMillis());
//
//		String checkId          = _checkId + "";
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
//		urlParams.add("connectId",           _connectCount+"");
////		urlParams.add("clientTime",          clientTime);
//		urlParams.add("sessionType",         "online");
//		urlParams.add("sessionStartTime",    sampleStartTime);
//		urlParams.add("sessionEndTime",      sampleEndTime);
//		urlParams.add("userName",            System.getProperty("user.name"));
//
//		int rows = 0;
//		for (CountersModel cm : GetCounters.getCmList())
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
//				return;
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
//					urlParams = new QueryString();
//
//					if (_logger.isDebugEnabled())
//						urlParams.add("debug",    "true");
//
//					checkId                 = _checkId + "";
//					String sessionStartTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(sessionInfo._sessionId);
//					String sessionEndTime   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(sessionInfo._lastSampleTime);
//
//					if (_logger.isDebugEnabled())
//						urlParams.add("debug",    "true");
//
//					urlParams.add("checkId",             checkId);
//					urlParams.add("connectId",           _connectCount+"");
////					urlParams.add("clientTime",          sessionTime);
//					urlParams.add("sessionType",         "offline-"+loopCnt);
//					urlParams.add("sessionStartTime",    sessionStartTime);
//					urlParams.add("sessionEndTime",      sessionEndTime);
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
//		// Loop each of the request and send off data
//		for (QueryString urlEntry : sendQueryList)
//		{
//			try
//			{
//				// If NOT in the "known" thread name, then it's a synchronous call, then lower the timeout value. 
//				int timeout = DEFAULT_TIMEOUT;
//				String threadName = Thread.currentThread().getName(); 
//				if ( ! "sendCounterUsageInfo".equals(threadName) )
//					timeout = 5000;
//
//				// SEND OFF THE REQUEST
//				InputStream in;
//				if (_useHttpPost)
//					in = sendHttpPost(urlStr, urlEntry, timeout);
//				else
//					in = sendHttpParams(urlStr, urlEntry, timeout);
//
//				LineNumberReader lr = new LineNumberReader(new InputStreamReader(in));
//				String line;
//				String responseLines = "";
//				while ((line = lr.readLine()) != null)
//				{
//					_logger.debug("response line "+lr.getLineNumber()+": " + line);
//					responseLines += line;
//					if (line.startsWith("ERROR:"))
//					{
//						_logger.warn("When doing 'Counter Usage' info 'ERROR:' response row, which looked like '" + line + "'.");
//					}
//					if (line.startsWith("DONE:"))
//					{
//					}
//				}
//				in.close();
//				_responseString = responseLines;
//
////				_checkSucceed = true;
//			}
//			catch (IOException ex)
//			{
//				_logger.debug("when trying to send 'Counter Usage' info, we had problems", ex);
//			}
//		}
//	}
//
//
//	
//	
//	
//	/**
//	 * Note: Since this is synchronized, make sure that no _logger.{warn|error} is done inside the 
//	 * synchronized block, because then we will have a deadlock.
//	 * <p>
//	 * If we don't synchronize, then error/warn messages that are issued rapidly will be discarded, 
//	 * this due to _sendLogInfoCount is "global" and will be incremented... at least that's what I think.<br>
//	 * Another way of doing this would be to have a synchronized List where messages are pushed to.
//	 */
//	public static synchronized void sendLogInfoNoBlock(final Log4jLogRecord record)
//	{
//		// TRACE IN DEVELOPMENT
////		if (_printDevTrace && _checkId < 0)
////		{
////			System.out.println("DEV-TRACE(CheckForUpdates): sendLogInfoNoBlock(): ");
////		}
//
//		if (_checkId < 0)
//		{
//			_logger.debug("No checkId was discovered when trying to send ERROR info, skipping this.");
//			return;
//		}
//
//		if (record == null)
//			return;
//		
//		if ( record.isWarningLevel() && ! _sendLogInfoWarning )
//		{
//			_logger.debug("Send 'LOG WARNING info' has been disabled.");
//			return;
//		}
//		if ( record.isSevereLevel() && ! _sendLogInfoError )
//		{
//			_logger.debug("Send 'LOG ERROR info' has been disabled.");
//			return;
//		}
//
//		// DISCARD VARIOUS MESSAGES
//		String msg = record.getMessage();
//		if (msg != null)
//		{
//			// Filter out following messages:
//			//_logger.warn("When trying to initialize Counters Model '"+getName()+"' The following role(s) were needed '"+StringUtil.toCommaStr(dependsOnRole)+"', and you do not have the following role(s) '"+didNotHaveRoles+"'.");
//			//_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"' in ASE Version "+getServerVersion()+", I found that '"+configName+"' wasn't configured (which is done with: sp_configure '"+configName+"'"+reconfigOptionStr+"), so monitoring information about '"+getDisplayName()+"' will NOT be enabled.");
//			//_logger.warn("When trying to initialize Counters Model '"+getName()+"' in ASE Version "+getServerVersionStr()+", I need atleast ASE Version "+getDependsOnVersionStr()+" for that.");
//			//_logger.warn("When trying to initialize Counters Model '"+getName()+"' in ASE Cluster Edition Version "+getServerVersionStr()+", I need atleast ASE Cluster Edition Version "+getDependsOnCeVersionStr()+" for that.");
//			//_logger.warn("When trying to initialize Counters Model '"+getName()+"' in ASE Version "+getServerVersion()+", "+msg+" (connect with a user that has '"+needsRoleToRecreate+"' or load the proc from '$ASETUNE_HOME/classes' or unzip asetune.jar. under the class '"+scriptLocation.getClass().getName()+"' you will find the script '"+scriptName+"').");
//			if (msg.startsWith("When trying to initialize Counters Model")) 
//				return;
//			
//			if (msg.startsWith("CounterSample(CmRaSysmon).getCnt : 99000 ERROR: Found NO database that was marked for replication"))
//				return;
//			if (msg.startsWith("Date problems for table 'CmStmntCacheDetails"))
//				return;
//			
//		}
//		
//		if (_sendLogInfoCount > _sendLogInfoThreshold)
//		{
//			_logger.debug("Send 'LOG info' has exceed sendLogInfoThreshold="+_sendLogInfoThreshold+", sendLogInfoCount="+_sendLogInfoCount);
//			return;
//		}
//		// NOTE: we may need to synchronize this, but on the other hand it's just a counter incrementation
//		final int sendLogInfoCount = _sendLogInfoCount++;
//
//		// SEND This using a thread
//		Runnable doLater = new Runnable()
//		{
//			@Override
//			public void run()
//			{
//				CheckForUpdates errorInfo = new CheckForUpdates();
//				errorInfo.sendLogInfo(record, sendLogInfoCount);
//			}
//		};
//		Thread checkThread = new Thread(doLater);
//		checkThread.setName("sendLogInfo");
//		checkThread.setDaemon(true);
//		checkThread.start();
//	}
//
//	/**
//	 * Send info on ERRORs
//	 */
//	public void sendLogInfo(final Log4jLogRecord record, int sendLogInfoCount)
//	{
//		// URL TO USE
//		String urlStr = ASETUNE_ERROR_INFO_URL;
//
//		if (_checkId < 0)
//		{
//			_logger.debug("No checkId was discovered when trying to send LOG info, skipping this.");
//			return;
//		}
//
//		if (record == null)
//			return;
//		
//		if ( record.isWarningLevel() && ! _sendLogInfoWarning )
//		{
//			_logger.debug("Send 'LOG WARNING info' has been disabled.");
//			return;
//		}
//		if ( record.isSevereLevel() && ! _sendLogInfoError )
//		{
//			_logger.debug("Send 'LOG ERROR info' has been disabled.");
//			return;
//		}
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
//		QueryString urlParams = new QueryString();
//
//		Date timeNow = new Date(System.currentTimeMillis());
//
//		String checkId          = _checkId + "";
//		String clientTime       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);
//
//		if (_logger.isDebugEnabled())
//			urlParams.add("debug",    "true");
//
//		urlParams.add("checkId",       checkId);
//		urlParams.add("sendCounter",   sendLogInfoCount +"");
//		urlParams.add("clientTime",    clientTime);
//		urlParams.add("userName",      System.getProperty("user.name"));
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
//		try
//		{
//			// SEND OFF THE REQUEST
//			InputStream in;
//			if (_useHttpPost)
//				in = sendHttpPost(urlStr, urlParams, 750); // timeout after 750 ms
//			else
//				in = sendHttpParams(urlStr, urlParams, 750); // timeout after 750 ms
//
//			LineNumberReader lr = new LineNumberReader(new InputStreamReader(in));
//			String line;
//			String responseLines = "";
//			while ((line = lr.readLine()) != null)
//			{
//				_logger.debug("response line "+lr.getLineNumber()+": " + line);
//				responseLines += line;
//				if (line.startsWith("ERROR:"))
//				{
//					_logger.info("When doing LOG info 'ERROR:' response row, which looked like '" + line + "'.");
//				}
//				if (line.startsWith("DONE:"))
//				{
//				}
//			}
//			in.close();
//			_responseString = responseLines;
//		}
//		catch (IOException ex)
//		{
//			_logger.debug("when trying to send LOG info, we had problems", ex);
//		}
//	}
//
//
//
//
//
//
//
//
//
//
//
//
//	/*----------------------------------------------------
//	**----------------------------------------------------
//	**---- SQL WINDOW ---- SQL WINDOW ---- SQL WINDOW ----
//	**----------------------------------------------------
//	**----------------------------------------------------
//	*/
//	/**
//	 * Check for a later version of the software
//	 * @param owner A JFrame or similar
//	 * @param showNoUpgrade Even if no upgrade is available, show info about that in a GUI message.
//	 */
//	public static void noBlockCheckSqlWindow(final Component owner, final boolean showNoUpgrade, final boolean showFailure)
//	{
//		Runnable checkLater = new Runnable()
//		{
//			@Override
//			public void run()
//			{
//				CheckForUpdates chk = new CheckForUpdates();
//
//				// Go and check
//				chk.checkSqlWindow();
//
//				if (! chk.checkSucceed())
//				{
//					if (owner != null && showFailure)
//						CheckDialog.showDialog(owner, chk);
//
//					_logger.info("Check for latest version failed.");
//					return;
//				}
//
//				if (chk.hasUpgrade())
//				{
//					if (owner != null)
//						CheckDialog.showDialog(owner, chk);
//
//					_logger.info("New Upgrade is Available. New version is '"+chk.getNewAppVersionStr()+"' " +
//							"and can be downloaded here '"+chk.getDownloadUrl()+"'.");
//				}
//				else
//				{
//					if (showNoUpgrade)
//					{
//						if (owner != null)
//							CheckDialog.showDialog(owner, chk);
//					}
//
////					if (chk.hasFeedback())
////					{
////						if (owner != null)
////							CheckDialog.showDialog(owner, chk);
////					}
//
//					if (chk.isResponseOfHtml())
//					{
//						if (owner != null)
//							CheckDialog.showDialog(owner, chk);
//					}
//					else
//					{
//						_logger.info("You have got the latest release of '"+Version.getAppName()+"'.");
//					}
//				}
//			}
//		};
//		Thread checkThread = new Thread(checkLater);
//		checkThread.setName("checkForUpdates");
//		checkThread.setDaemon(true);
//		checkThread.start();
//	}
//
//	/**
//	 * Go and check for updates
//	 */
//	public void checkSqlWindow()
//	{
//		// URS to use
//		String urlStr = SQLWIN_CHECK_UPDATE_URL;
//
//		_hasUpgrade   = false;
//		_checkSucceed = false;
//
//		// COMPOSE: parameters to send to HTTP server
//		QueryString urlParams = new QueryString();
//
//		Date timeNow = new Date(System.currentTimeMillis());
//		String clientTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);
//
//		if (_logger.isDebugEnabled())
//			urlParams.add("debug",    "true");
//
//		String appStartupTime = TimeUtils.msToTimeStr("%MM:%SS.%ms", System.currentTimeMillis() - QueryWindow.getStartTime());
//
//		urlParams.add("clientCheckTime",     clientTime);
//
//		urlParams.add("clientSourceDate",     Version.getSourceDate());
//		urlParams.add("clientSourceVersion",  Version.getSourceRev());
//		urlParams.add("clientAppVersion",     Version.getVersionStr());
////		urlParams.add("clientExpireDate",     Version.DEV_VERSION_EXPIRE_STR);
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
////		urlParams.add("gui",                AseTune.hasGUI()+"");
//
//		urlParams.add("java_version",       System.getProperty("java.version"));
//		urlParams.add("java_vm_version",    System.getProperty("java.vm.version"));
//		urlParams.add("java_vm_vendor",     System.getProperty("java.vm.vendor"));
//		urlParams.add("sun_arch_data_model",System.getProperty("sun.arch.data.model"));
//		urlParams.add("java_home",          System.getProperty("java.home"));
////		if (_useHttpPost)
////			urlParams.add("java_class_path",System.getProperty("java.class.path"));
////		else
////			urlParams.add("java_class_path","discarded when using sendHttpParams()");
//		urlParams.add("memory",             (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB");
//		urlParams.add("os_name",            System.getProperty("os.name"));
//		urlParams.add("os_version",         System.getProperty("os.version"));
//		urlParams.add("os_arch",            System.getProperty("os.arch"));
//		urlParams.add("sun_desktop",        System.getProperty("sun.desktop"));
//		urlParams.add("-end-",              "-end-");
//
//		try
//		{
//			// SEND OFF THE REQUEST
//			InputStream in;
//			if (_useHttpPost)
//				in = sendHttpPost(urlStr, urlParams);
//			else
//				in = sendHttpParams(urlStr, urlParams);
//
//			//----------------------------------------------
//			// This is how a response would look like
//			//----------------------------------------------
//			// ACTION:UPGRADE:$ASETUNE_LATEST_VERSION_STR:$DOWNLOAD_URL"
//			// ACTION:NO-UPGRADE
//			//----------------------------------------------
//
//			_action        = "";
//			_newAppVersion = "";
//			_downloadUrl   = DEFAULT_DOWNLOAD_URL;
//			_whatsNewUrl   = DEFAULT_WHATSNEW_URL;
//
//			// Set default values for some stuff (which normally is for AseTune)
//			_sendConnectInfo      = true;
//			_sendMdaInfo          = false;
//			_sendMdaInfoBatchSize = 20;
//			_sendUdcInfo          = false;
//			_sendCounterUsageInfo = true;
//			_sendLogInfoWarning   = false;
//			_sendLogInfoError     = false;
//
//			_connectCount         = 0;
//
//			_sendLogInfoThreshold = 100;
//			_sendLogInfoCount     = 0;
//
//			// Read response
//			LineNumberReader lr = new LineNumberReader(new InputStreamReader(in));
//			String line;
//			String responseLines = "";
//			boolean foundActionLine = false;
//			while ((line = lr.readLine()) != null)
//			{
//				_logger.debug("response line "+lr.getLineNumber()+": " + line);
//				responseLines += line;
//				if (line.startsWith("ACTION:"))
//				{
//					foundActionLine = true;
//					String[] sa = line.split(":");
//					for (int i=0; i<sa.length; i++)
//					{
//						_logger.debug("   - STRING["+i+"]='"+sa[i]+"'.");
//
//						if (i == 1) _action        = sa[1];
//						if (i == 2) _newAppVersion = sa[2];
//						if (i == 3) _downloadUrl   = sa[3];
//						if (i == 4) _whatsNewUrl   = sa[4];
//					}
//				}
//				if (line.startsWith("OPTIONS:"))
//				{
//					// OPTIONS: sendConnectInfo = true|false, sendUdcInfo = true|false, sendCounterUsageInfo = true|false
//					String options = line.substring("OPTIONS:".length());
//					_logger.debug("Receiving Options from server '"+options+"'.");
//					String[] sa = options.split(",");
//					for (int i=0; i<sa.length; i++)
//					{
//						String[] keyVal = sa[i].split("=");
//						if (keyVal.length == 2)
//						{
//							String key = keyVal[0].trim();
//							String val = keyVal[1].trim();
//							boolean bVal = val.equalsIgnoreCase("true");
//
//							if (key.equalsIgnoreCase("sendConnectInfo"))
//							{
//								_sendConnectInfo = bVal;
//								_logger.debug("Setting option '"+key+"' to '"+bVal+"'.");
//							}
////							else if (key.equalsIgnoreCase("sendUdcInfo"))
////							{
////								_sendUdcInfo = bVal;
////								_logger.debug("Setting option '"+key+"' to '"+bVal+"'.");
////							}
////							else if (key.equalsIgnoreCase("sendMdaInfo"))
////							{
////								_sendMdaInfo = bVal;
////								_logger.debug("Setting option '"+key+"' to '"+bVal+"'.");
////							}
////							else if (key.equalsIgnoreCase("sendMdaInfoBatchSize"))
////							{
////								try
////								{
////									int intVal = Integer.parseInt(val);
////									_sendMdaInfoBatchSize = intVal;
////									_logger.debug("Setting option '"+key+"' to '"+intVal+"'.");
////								}
////								catch (NumberFormatException ex)
////								{
////									_logger.warn("Problems reading option '"+key+"', with value '"+val+"'. Can't convert to Integer. Caught: "+ex);
////								}
////							}
//							else if (key.equalsIgnoreCase("sendCounterUsageInfo"))
//							{
//								_sendCounterUsageInfo = bVal;
//								_logger.debug("Setting option '"+key+"' to '"+bVal+"'.");
//							}
//							else if (key.equalsIgnoreCase("sendLogInfoWarning"))
//							{
//								_sendLogInfoWarning = bVal;
//								_logger.debug("Setting option '"+key+"' to '"+bVal+"'.");
//							}
//							else if (key.equalsIgnoreCase("sendLogInfoError"))
//							{
//								_sendLogInfoError = bVal;
//								_logger.debug("Setting option '"+key+"' to '"+bVal+"'.");
//							}
//							else if (key.equalsIgnoreCase("sendLogInfoThreshold"))
//							{
//								try
//								{
//									_sendLogInfoThreshold = Integer.parseInt(val);
//									_logger.debug("Setting option '"+key+"' to '"+val+"'.");
//								}
//								catch (NumberFormatException e)
//								{
//									_logger.debug("NumberFormatException: Setting option '"+key+"' to '"+val+"'.");
//								}
//							}
//							else
//							{
//								_logger.debug("Unknown option '"+key+"' from server with value '"+val+"'.");
//							}
//						}
//						else
//							_logger.debug("Option '"+sa[i]+"' from server has strange key/valye.");
//					}
//				}
//				if (line.startsWith("FEEDBACK:"))
//				{
//					// OPTIONS: sendConnectInfo = true|false, sendUdcInfo = true|false, sendCounterUsageInfo = true|false
//					String feedback = line.substring("FEEDBACK:".length()).trim();
//					_logger.debug("Receiving feedback from server '"+feedback+"'.");
//
//					if ( ! "".equals(feedback) )
//					{
//						String[] sa = feedback.split(":");
//						for (int i=0; i<sa.length; i++)
//						{
//							if (i == 0) _feedbackDateStr = sa[0];
//							if (i == 1) _feedbackUrl     = sa[1];
//						}
//						try
//						{
//							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//							_feedbackDate = sdf.parse(_feedbackDateStr);
//						}
//						catch (ParseException e)
//						{
//						}
//					}
//				}
//				if (line.startsWith("ERROR:"))
//				{
//					_logger.warn("When checking for new version, found an 'ERROR:' response row, which looked like '" + line + "'.");
//				}
//				if (line.startsWith("CHECK_ID_SQLW:"))
//				{
//					String actionResponse = line.substring( "CHECK_ID_SQLW:".length() ).trim();
//					try { _checkIdSqlW = Integer.parseInt(actionResponse); }
//					catch (NumberFormatException ignore) {}
//					_logger.debug("Received check_id='"+_checkIdSqlW+"' from update site.");
//				}
//			}
//			in.close();
//			_responseString = responseLines;
//
//			// if not empty, check that it starts with 'http://' otherwise add it to the start
//			if ( _downloadUrl != null && !_downloadUrl.trim().equals("") )
//			{
//				if ( ! _downloadUrl.startsWith("http://") )
//					_downloadUrl = "http://" + _downloadUrl;
//			}
//
//			// if not empty, check that it starts with 'http://' otherwise add it to the start
//			if ( _whatsNewUrl != null && !_whatsNewUrl.trim().equals("") )
//			{
//				if ( ! _whatsNewUrl.startsWith("http://") )
//					_whatsNewUrl = "http://" + _whatsNewUrl;
//			}
//
//			// if not empty, check that it starts with 'http://' otherwise add it to the start
//			if ( _feedbackUrl != null && !_feedbackUrl.trim().equals("") )
//			{
//				if ( ! _feedbackUrl.startsWith("http://") )
//					_feedbackUrl = "http://" + _feedbackUrl;
//			}
//
//			if (_action.equals("UPGRADE"))
//			{
//				_hasUpgrade = true;
//				_logger.debug("-UPGRADE-");
//				_logger.debug("-to:"+_newAppVersion);
//				_logger.debug("-at:"+_downloadUrl);
//				_logger.debug("-at:"+_whatsNewUrl);
//			}
//			else
//			{
//				_hasUpgrade = false;
//				_logger.debug("-NO-UPGRADE-");
//			}
//
//			if ( ! foundActionLine )
//			{
//				_logger.warn("When checking for new version, no 'ACTION:' response was found. The response rows was '" + responseLines + "'.");
//			}
//
//			_checkSucceed = true;
//		}
//		catch (IOException ex)
//		{
//			_logger.debug("When we checking for later version, we had problems", ex);
//		}
//	}
//
//
//
//
//
//
//
//	//-------------------------------------------------------------------------------------
//	//-------------------------------------------------------------------------------------
//	//-------------------------------------------------------------------------------------
//
//	public static class SqlwConnectInfo
//	{
//		private final int           _connType; 
//		private final String        _connTypeStr;
//		private       String        _prodName          = "";
//		private       String        _prodVersionStr    = ""; // from jdbc metadata
//		private       String        _jdbcDriverName    = "";
//		private       String        _jdbcDriverVersion = "";
//		private       String        _jdbcDriver        = "";
//		private       String        _jdbcUrl           = "";
//		private       int           _srvVersionInt     = 0;
//		private       String        _srvName           = "";
//		private       String        _srvUser           = "";
//		private       String        _srvCharset        = "";
//		private       String        _srvSortorder      = "";
//		private       SshTunnelInfo _sshInfo           = null;
//
//		public SqlwConnectInfo(int connType)
//		{
//			_connType = connType;
//			if      (_connType == ConnectionDialog.TDS_CONN)     _connTypeStr = "TDS";
//			else if (_connType == ConnectionDialog.OFFLINE_CONN) _connTypeStr = "OFFLINE";
//			else if (_connType == ConnectionDialog.JDBC_CONN)    _connTypeStr = "JDBC";
//			else                                                 _connTypeStr = "UNKNOWN("+connType+")";
//		}
//		public int           getConnTypeInt      () { return _connType; }
//		public String        getConnTypeStr      () { return _connTypeStr       == null ? "UNKNOWN" : _connTypeStr; }
//		
//		public String        getProdName         () { return _prodName          == null ? "" : _prodName         .trim(); }
//		public String        getProdVersionStr   () { return _prodVersionStr    == null ? "" : _prodVersionStr   .trim(); }
//		public String        getJdbcDriverName   () { return _jdbcDriverName    == null ? "" : _jdbcDriverName   .trim(); }
//		public String        getJdbcDriverVersion() { return _jdbcDriverVersion == null ? "" : _jdbcDriverVersion.trim(); }
//		public String        getJdbcDriver       () { return _jdbcDriver        == null ? "" : _jdbcDriver       .trim(); }
//		public String        getJdbcUrl          () { return _jdbcUrl           == null ? "" : _jdbcUrl          .trim(); }
//		public int           getSrvVersionInt    () { return _srvVersionInt; }
//		public String        getSrvName          () { return _srvName           == null ? "" : _srvName          .trim(); }
//		public String        getSrvUser          () { return _srvUser           == null ? "" : _srvUser          .trim(); }
//		public String        getSrvCharset       () { return _srvCharset        == null ? "" : _srvCharset       .trim(); }
//		public String        getSrvSortorder     () { return _srvSortorder      == null ? "" : _srvSortorder     .trim(); }
//		public SshTunnelInfo getSshTunnelInfo    () { return _sshInfo; }
//		public String        getSshTunnelInfoStr () { return _sshInfo           == null ? "" : _sshInfo.getInfoString(); }
//
//		public void setProdName         (String str)            { _prodName          = str; }
//		public void setProdVersionStr   (String str)            { _prodVersionStr    = str; }
//		public void setJdbcDriverName   (String str)            { _jdbcDriverName    = str; }
//		public void setJdbcDriverVersion(String str)            { _jdbcDriverVersion = str; }
//		public void setJdbcDriver       (String str)            { _jdbcDriver        = str; }
//		public void setJdbcUrl          (String str)            { _jdbcUrl           = str; }
//		public void setSrvVersionInt    (int    ver)            { _srvVersionInt     = ver; }
//		public void setSrvName          (String str)            { _srvName           = str; }
//		public void setSrvUser          (String str)            { _srvUser           = str; }
//		public void setSrvCharset       (String str)            { _srvCharset        = str; }
//		public void setSrvSortorder     (String str)            { _srvSortorder      = str; }
//		public void setSshTunnelInfo    (SshTunnelInfo sshInfo) { _sshInfo           = sshInfo; }
//	}
//
//	/**
//	 * @param connType ConnectionDialog.TDS_CONN | ConnectionDialog.OFFLINE_CONN
//	 * @param sshTunnelInfo 
//	 */
//	public static void sendSqlwConnectInfoNoBlock(final SqlwConnectInfo sqlwConnInfo)
//	{
//		if ( ! _sendConnectInfo )
//		{
//			_logger.debug("Send 'Connect info' has been disabled.");
//			return;
//		}
//
//		Runnable doLater = new Runnable()
//		{
//			@Override
//			public void run()
//			{
//				CheckForUpdates connInfo = new CheckForUpdates();
//				connInfo.sendSqlwConnectInfo(sqlwConnInfo);
//
//				CheckForUpdates udcInfo = new CheckForUpdates();
//				udcInfo.sendUdcInfo();
//			}
//		};
//		Thread checkThread = new Thread(doLater);
//		checkThread.setName("sendConnectInfo");
//		checkThread.setDaemon(true);
//		checkThread.start();
//	}
//
//	/**
//	 * Send info on connection
//	 * @param sshTunnelInfo 
//	 */
//	public void sendSqlwConnectInfo(final SqlwConnectInfo sqlwConnInfo)
//	{
//		// URL TO USE
//		String urlStr = SQLWIN_CONNECT_INFO_URL;
//
//		if ( sqlwConnInfo == null )
//		{
//			_logger.error("Send 'SQLW Connect info' the input can't be null.");
//			return;
//		}
//
//		if ( ! _sendConnectInfo )
//		{
//			_logger.debug("Send 'Connect info' has been disabled.");
//			return;
//		}
//
//		if (_checkIdSqlW < 0)
//		{
//			_logger.debug("No checkId was discovered when trying to send connection info, skipping this.");
//			return;
//		}
//
//		int connType = sqlwConnInfo._connType;
//		if (connType != ConnectionDialog.TDS_CONN && connType != ConnectionDialog.OFFLINE_CONN && connType != ConnectionDialog.JDBC_CONN)
//		{
//			_logger.warn("ConnectInfo: Connection type must be TDS_CONN | OFFLINE_CONN | JDBC_CONN");
//			return;
//		}
//		
//		_connectCount++;
//
//		// COMPOSE: parameters to send to HTTP server
//		QueryString urlParams = new QueryString();
//
//		Date timeNow = new Date(System.currentTimeMillis());
//
//		String checkId          = _checkIdSqlW + "";
//		String clientTime       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);
//
//		if (_logger.isDebugEnabled())
//			urlParams.add("debug",    "true");
//
//		urlParams.add("checkId",             checkId);
//		urlParams.add("clientTime",          clientTime);
//		urlParams.add("userName",            System.getProperty("user.name"));
//
//		urlParams.add("connectId",           _connectCount+"");
//		urlParams.add("connectType",         sqlwConnInfo.getConnTypeStr());
//
//		urlParams.add("prodName",            sqlwConnInfo.getProdName());
//		urlParams.add("prodVersionStr",      sqlwConnInfo.getProdVersionStr());
//
//		urlParams.add("jdbcDriverName",      sqlwConnInfo.getJdbcDriverName());
//		urlParams.add("jdbcDriverVersion",   sqlwConnInfo.getJdbcDriverVersion());
//		urlParams.add("jdbcDriver",          sqlwConnInfo.getJdbcDriver());
//		urlParams.add("jdbcUrl",             sqlwConnInfo.getJdbcUrl());
//
//		urlParams.add("srvVersionInt",       sqlwConnInfo.getSrvVersionInt()+"");
//		urlParams.add("srvName",             sqlwConnInfo.getSrvName());
//		urlParams.add("srvUser",             sqlwConnInfo.getSrvUser());
//		urlParams.add("srvCharsetName",      sqlwConnInfo.getSrvCharset());
//		urlParams.add("srvSortOrderName",    sqlwConnInfo.getSrvSortorder());
//
//		urlParams.add("sshTunnelInfo",       sqlwConnInfo.getSshTunnelInfoStr());
//
//		try
//		{
//			// SEND OFF THE REQUEST
//			InputStream in;
//			if (_useHttpPost)
//				in = sendHttpPost(urlStr, urlParams);
//			else
//				in = sendHttpParams(urlStr, urlParams);
//
//			LineNumberReader lr = new LineNumberReader(new InputStreamReader(in));
//			String line;
//			String responseLines = "";
//			while ((line = lr.readLine()) != null)
//			{
//				_logger.debug("response line "+lr.getLineNumber()+": " + line);
//				responseLines += line;
//				if (line.startsWith("ERROR:"))
//				{
//					_logger.warn("When doing connection info 'ERROR:' response row, which looked like '" + line + "'.");
//				}
//				if (line.startsWith("DONE:"))
//				{
//				}
//				if (line.startsWith("SEND_MDA_INFO:"))
//				{
//					_logger.info("Received info to collect MDA Information.");
//					sendMdaInfoNoBlock();
//				}
//			}
//			in.close();
//			_responseString = responseLines;
//
////			_checkSucceed = true;
//		}
//		catch (IOException ex)
//		{
//			_logger.debug("when trying to send connection info, we had problems", ex);
//		}
//	}
//
//
//
//
//
//
//	//-------------------------------------------------------------------------------------
//	//-------------------------------------------------------------------------------------
//	//-------------------------------------------------------------------------------------
//
//	public static class SqlwUsageInfo
//	{
//		private int    _connType          = 0;
//		private String _prodName          = "";
//		private int    _srvVersionInt     = 0;
//
//		private long   _connectTime       = 0;
//		private long   _disconnectTime    = 0;
//
//		private int    _execMainCount     = 0;
//		private int    _execBatchCount    = 0;
//		private long   _execTimeTotal     = 0;
//		private long   _execTimeSqlExec   = 0;
//		private long   _execTimeRsRead    = 0;
//		private long   _execTimeOther     = 0;
//		private int    _rsCount           = 0;
//		private int    _rsRowsCount       = 0;
//		private int    _iudRowsCount      = 0;
//		private int    _sqlWarningCount   = 0;
//		private int    _sqlExceptionCount = 0;
//
//		
//		public void setConnType         (int    val) { _connType          = val; }
//		public void setProductName      (String val) { _prodName          = val; }
//		public void setSrvVersionInt    (int    val) { _srvVersionInt     = val; }
//
//		public void setConnectTime      (long   val) { _connectTime       = val; }
//		public void setDisconnectTime   (long   val) { _disconnectTime    = val; }
//		
//		public void setExecMainCount    (int    val) { _execMainCount     = val; }
//		public void setExecBatchCount   (int    val) { _execBatchCount    = val; }
//		public void setExecTimeTotal    (long   val) { _execTimeTotal     = val; }
//		public void setExecTimeSqlExec  (long   val) { _execTimeSqlExec   = val; }
//		public void setExecTimeRsRead   (long   val) { _execTimeRsRead    = val; }
//		public void setExecTimeOther    (long   val) { _execTimeOther     = val; }
//		public void setRsCount          (int    val) { _rsCount           = val; }
//		public void setRsRowsCount      (int    val) { _rsRowsCount       = val; }
//		public void setIudRowsCount     (int    val) { _iudRowsCount      = val; }
//		public void setSqlWarningCount  (int    val) { _sqlWarningCount   = val; }
//		public void setSqlExceptionCount(int    val) { _sqlExceptionCount = val; }
//
//
//		public int    getConnType         () { return _connType; }
//		public String getProductName      () { return _prodName; }
//		public int    getSrvVersionInt    () { return _srvVersionInt; }
//
//		public long   getConnectTime      () { return _connectTime; }
//		public long   getDisconnectTime   () { return _disconnectTime; }
//
//		public int    getExecMainCount    () { return _execMainCount; }
//		public int    getExecBatchCount   () { return _execBatchCount; }
//		public long   getExecTimeTotal    () { return _execTimeTotal; }
//		public long   getExecTimeSqlExec  () { return _execTimeSqlExec; }
//		public long   getExecTimeRsRead   () { return _execTimeRsRead; }
//		public long   getExecTimeOther    () { return _execTimeOther; }
//		public int    getRsCount          () { return _rsCount; }
//		public int    getRsRowsCount      () { return _rsRowsCount; }
//		public int    getIudRowsCount     () { return _iudRowsCount; }
//		public int    getSqlWarningCount  () { return _sqlWarningCount; }
//		public int    getSqlExceptionCount() { return _sqlExceptionCount; }
//		
//		public String getConnTypeStr() 
//		{
//			if      (_connType == ConnectionDialog.TDS_CONN)     return "TDS";
//			else if (_connType == ConnectionDialog.OFFLINE_CONN) return "OFFLINE";
//			else if (_connType == ConnectionDialog.JDBC_CONN)    return "JDBC";
//			else                                                 return "UNKNOWN("+_connType+")";
//		}
//	}
//
//	/**
//	 */
//	private static int _sendSqlwCounterUsage_atConnectCount = 0;
//
//	/**
//	 * Send info on SQLW Usage Info
//	 */
//	public static void sendSqlwCounterUsageInfoNoBlock(final SqlwUsageInfo sqlwUsageInfo, boolean blockingCall)
//	{
//		if ( ! _sendCounterUsageInfo )
//		{
//			_logger.debug("Send 'Counter Usage Info' has been disabled.");
//			return;
//		}
//
//		Runnable doLater = new Runnable()
//		{
//			@Override
//			public void run()
//			{
//				if ( _sendSqlwCounterUsage_atConnectCount < _connectCount )
//				{
//					_sendSqlwCounterUsage_atConnectCount++;
//
//					CheckForUpdates chk = new CheckForUpdates();
//
//					// Go and check
//					chk.sendSqlwCounterUsageInfo(sqlwUsageInfo);
//				}
//				else
//				{
//					_logger.debug("sendSqlwUsageInfo, already done (_sendSqlwCounterUsage_atConnectCount="+_sendSqlwCounterUsage_atConnectCount+", _connectCount="+_connectCount+").");
//				}
//			}
//		};
//		if (blockingCall)
//		{
//			// if synchronous, just call the run method on current thread
//			doLater.run();
//		}
//		else
//		{
//			// no blocking call, start a thread that does it.
//			Thread checkThread = new Thread(doLater);
//			checkThread.setName("sendSqlwCounterUsageInfo");
//			checkThread.setDaemon(true);
//			checkThread.start();
//		}
//	}
//
//	/**
//	 * Send info on SQLW Usage Info
//	 */
//	private void sendSqlwCounterUsageInfo(final SqlwUsageInfo sqlwUsageInfo)
//	{
//		// URL TO USE
//		String urlStr = SQLWIN_COUNTER_USAGE_INFO_URL;
//
//		if ( sqlwUsageInfo == null )
//		{
//			_logger.error("Send SQLW 'Counter Usage info' the input can't be null.");
//			return;
//		}
//
//		if ( ! _sendCounterUsageInfo )
//		{
//			_logger.debug("Send SQLW 'Counter Usage Info' has been disabled.");
//			return;
//		}
//
//		if (_checkIdSqlW < 0)
//		{
//			_logger.debug("No checkId was discovered when trying to send SQLW 'Counter Usage info', skipping this.");
//			return;
//		}
//
//		Configuration conf = Configuration.getCombinedConfiguration();
//		if (conf == null)
//		{
//			_logger.debug("Configuration was null when trying to send SQlW 'Counter Usage info', skipping this.");
//			return;
//		}
//
//
//		// COMPOSE: parameters to send to HTTP server
//		QueryString urlParams = new QueryString();
//
//		Date timeNow = new Date(System.currentTimeMillis());
//
//		String checkId          = _checkIdSqlW + "";
//		String clientTime       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);
//
//		String connectTime      = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(sqlwUsageInfo.getConnectTime());
//		String disconnectTime   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(sqlwUsageInfo.getDisconnectTime());
//
//		if (_logger.isDebugEnabled())
//			urlParams.add("debug",    "true");
//
//		urlParams.add("checkId",             checkId);
//		urlParams.add("clientTime",          clientTime);
//		urlParams.add("userName",            System.getProperty("user.name"));
//
//		urlParams.add("connectId",           _connectCount);
//
//		urlParams.add("connectType",         sqlwUsageInfo.getConnTypeStr());
//		urlParams.add("prodName",            sqlwUsageInfo.getProductName());
//		urlParams.add("srvVersionInt",       sqlwUsageInfo.getSrvVersionInt());
//
//		urlParams.add("connectTime",         connectTime);
//		urlParams.add("disconnectTime",      disconnectTime);
//		
//		urlParams.add("execMainCount",       sqlwUsageInfo.getExecMainCount());
//		urlParams.add("execBatchCount",      sqlwUsageInfo.getExecBatchCount());
//		urlParams.add("execTimeTotal",       sqlwUsageInfo.getExecTimeTotal());
//		urlParams.add("execTimeSqlExec",     sqlwUsageInfo.getExecTimeSqlExec());
//		urlParams.add("execTimeRsRead",      sqlwUsageInfo.getExecTimeRsRead());
//		urlParams.add("execTimeOther",       sqlwUsageInfo.getExecTimeOther());
//		urlParams.add("rsCount",             sqlwUsageInfo.getRsCount());
//		urlParams.add("rsRowsCount",         sqlwUsageInfo.getRsRowsCount());
//		urlParams.add("iudRowsCount",        sqlwUsageInfo.getIudRowsCount());
//		urlParams.add("sqlWarningCount",     sqlwUsageInfo.getSqlWarningCount());
//		urlParams.add("sqlExceptionCount",   sqlwUsageInfo.getSqlExceptionCount());
//
//		try
//		{
//			// If NOT in the "known" thread name, then it's a synchronous call, then lower the timeout value. 
//			int timeout = DEFAULT_TIMEOUT;
//			String threadName = Thread.currentThread().getName(); 
//			if ( ! "sendSqlwCounterUsageInfo".equals(threadName) )
//				timeout = 5000;
//
//			// SEND OFF THE REQUEST
//			InputStream in;
//			if (_useHttpPost)
//				in = sendHttpPost(urlStr, urlParams, timeout);
//			else
//				in = sendHttpParams(urlStr, urlParams, timeout);
//
//			LineNumberReader lr = new LineNumberReader(new InputStreamReader(in));
//			String line;
//			String responseLines = "";
//			while ((line = lr.readLine()) != null)
//			{
//				_logger.debug("response line "+lr.getLineNumber()+": " + line);
//				responseLines += line;
//				if (line.startsWith("ERROR:"))
//				{
//					_logger.warn("When doing connection info 'ERROR:' response row, which looked like '" + line + "'.");
//				}
//				if (line.startsWith("DONE:"))
//				{
//				}
//			}
//			in.close();
//			_responseString = responseLines;
//
////			_checkSucceed = true;
//		}
//		catch (IOException ex)
//		{
//			_logger.debug("when trying to send sqlw 'Usage Info', we had problems", ex);
//		}
//		
//	}
//
//	
//	
//	
//
//
//
//
//
//
//
//
//
//
//
//
//
//	/*---------------------------------------------------
//	**---------------------------------------------------
//	**---- SUBCLASSES ---- SUBCLASES ---- SUBCLASES -----
//	**---------------------------------------------------
//	**---------------------------------------------------
//	*/
//	private static class OnlyHttpProxySelector
//	extends ProxySelector
//	{
//		ProxySelector _defsel = null;
//		ProxySelector _proxyVole = null;
//
//		public OnlyHttpProxySelector(ProxySelector def)
//		{
//			if (def == null)
//			{
//				throw new IllegalArgumentException("ProxySelector can't be null.");
//			}
//			_defsel = def;
//			_logger.debug("Installing a new ProxySelector, but I will save and use the default '"+_defsel.getClass().getName()+"'.");
//
//			//===============================================
//			// Use proxy-vole project:
//			//-----------------------------------------------
//			// install the log backend implementation
//			com.btr.proxy.util.Logger.setBackend(new com.btr.proxy.util.Logger.LogBackEnd()
//			{
//				// Discard messages if they have already been seen, use a timestamp to determen if we should re-log them again
//				private HashMap<String, Long> _logDiscardCache = new HashMap<String, Long>();
//				private long _reLogTimeout = 1000 * 900; // 15 minutes
//
//				@Override
//				public boolean isLogginEnabled(com.btr.proxy.util.Logger.LogLevel logLevel)
//				{
////					System.out.println("PROXY-VOLE.isLogginEnabled: logLevel="+logLevel);
////					return true;
//					if      (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.TRACE)   && _logger.isTraceEnabled()) return true;
//					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.DEBUG)   && _logger.isDebugEnabled()) return true;
//					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.INFO)    && _logger.isInfoEnabled())  return true;
//					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.WARNING) && _logger.isEnabledFor(Level.WARN))  return true;
//					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.ERROR)   && _logger.isEnabledFor(Level.ERROR))  return true;
//					else return false;
//				}
//
////				public void log(Class clazz, com.btr.proxy.util.Logger.LogLevel logLevel, String message, Object[] params)
//				@Override
//				public void log(Class<?> clazz, com.btr.proxy.util.Logger.LogLevel logLevel, String message, Object... params)
//				{
//					String msg = MessageFormat.format(message, params);
//					msg = clazz.getName() + ": " + msg;
//
//					// Check input parameters, and keep last Throwable if any of that cind.
//					Throwable t = null;
//					String tMsg = "";
//					for (Object param : params)
//					{
//						if (param instanceof Throwable)
//						{
//							t = (Throwable)param;
//							tMsg = " Caught: " + t;
//						}
//					}
//					Configuration conf = Configuration.getCombinedConfiguration();
//					if ( ! conf.getBooleanProperty("proxyvole.debug.stacktaceOnLogMessages", false) )
//						t = null;
//
//					String logMsg = msg + tMsg;
//
//					// If the message has been written lately, discard it...
//					// TODO: maybe add a counter, so we can write how many times it has been repeated/discared
//					if (_logDiscardCache.containsKey(logMsg))
//					{
//						long firstLogTime = _logDiscardCache.get(logMsg);
//						long firstLogAge  = System.currentTimeMillis() - firstLogTime;
//						if ( firstLogAge < _reLogTimeout )
//						{
//							if (_logger.isDebugEnabled())
//								_logger.debug("com.btr.proxy.util.Logger.LogBackEnd: Discarding log message, (firstLogAge='"+firstLogAge+"', re-logTimeout='"+_reLogTimeout+"': "+logMsg);
//							return;
//						}
//					}
//					_logDiscardCache.put(logMsg, new Long(System.currentTimeMillis()));
//
//					
////					System.out.println("PROXY-VOLE.log(logLevel="+logLevel+"): "+msg);
//					if      (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.TRACE)  ) _logger.trace(logMsg, t);
//					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.DEBUG)  ) _logger.debug(logMsg, t);
//					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.INFO)   ) _logger.info( logMsg, t);
////					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.WARNING)) _logger.warn( logMsg, t);
////					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.ERROR)  ) _logger.error(logMsg, t);
//					// downgrade WAR and ERROR to INFO, this so it wont open the log window
//					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.WARNING)) _logger.info( logMsg, t);
//					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.ERROR)  ) _logger.info( logMsg, t);
//					else _logger.info("Unhandled loglevel("+logLevel+"): " + logMsg, t);
//				}
//			});
//
//			// Use the DEFAULT or SPECIALIZED "search order" for proxy resolver.
//			boolean useProxyVoleDefault = false;
//			ProxySearch proxySearch = null;
//			if (useProxyVoleDefault)
//			{
//				// The below does:
//				//  headless: JAVA, OS_DEFAULT, ENV_VAR
//				// !headless: JAVA, BROWSER, OS_DEFAULT, ENV_VAR
//				proxySearch = ProxySearch.getDefaultProxySearch();
//			}
//			else
//			{
//				proxySearch = new ProxySearch();
//
//				// Test if we are a server or a client.
//				boolean headless = GraphicsEnvironment.isHeadless();
//
//				if (headless)
//				{
//					proxySearch.addStrategy(Strategy.JAVA);
//					proxySearch.addStrategy(Strategy.OS_DEFAULT);
//					proxySearch.addStrategy(Strategy.ENV_VAR);
//				}
//				else
//				{
//					proxySearch.addStrategy(Strategy.JAVA);
//					if (PlatformUtils.getCurrentPlattform() == PlatformUtils.Platform_WIN)
//					{
//						// the below, changes from default to add IE & FIREFOX...
//						// the default for windows is just IE
//						proxySearch.addStrategy(Strategy.IE);
//						proxySearch.addStrategy(Strategy.FIREFOX);
//					}
//					else
//						proxySearch.addStrategy(Strategy.BROWSER);
//					proxySearch.addStrategy(Strategy.OS_DEFAULT);
//					proxySearch.addStrategy(Strategy.ENV_VAR);
//				}
//				com.btr.proxy.util.Logger.log(ProxySearch.class, com.btr.proxy.util.Logger.LogLevel.TRACE, "Using SPECIALIZED search priority: {0}", new Object[] {proxySearch});
//			}
//
//			try
//			{
//				_proxyVole = proxySearch.getProxySelector();
//				if (_proxyVole == null)
//				{
//					_logger.debug("PROXY-VOLE: Could not be used as a ProxySelector, getProxySelector() returned null.");
//				}
//			}
//			catch (Throwable e)
//			{
//				// NoClassDefFoundError: org/mozilla/javascript/ScriptableObject
//				// is thrown if we are using JVM 1.5 and the js.jar is not part of the classpath
//				// js.jar is Java Script Engine: Rhino, http://www.mozilla.org/rhino/scriptjava.html
//				// which is not part of this distribution, in JVM 6, we use the built in Java Script Engine (javax.script)
//				System.out.println("When initializing 'proxy-vole' caught exception '"+e+"', but I will continue anyway. ");
//				_logger.info("When initializing 'proxy-vole' caught exception '"+e+"', but I will continue anyway.");
//			}
//		}
//
//		@Override
//		public void connectFailed(URI uri, SocketAddress sa, IOException ioe)
//		{
//			if (_proxyVole != null)
//				_proxyVole.connectFailed(uri, sa, ioe);
//		}
//
//		@Override
//		public List<Proxy> select(URI uri)
//		{
//			if (uri == null)
//			{
//				throw new IllegalArgumentException("URI can't be null.");
//			}
//			String protocol = uri.getScheme();
//
//			_logger.debug("called select(uri): protocoll '"+protocol+"' with uri='"+uri+"')");
//
//			if ( "socket".equalsIgnoreCase(protocol) )
//			{
//				_logger.debug("Shortcircuting proxy resolving for the protocol '"+protocol+"'. No proxy server will be used.");
//
//				List<Proxy> proxyList = new ArrayList<Proxy>();
//				proxyList.add(Proxy.NO_PROXY);
//				return proxyList;
//			}
//			else
//			{
//				List<Proxy> proxyList = null;
//				if (_proxyVole != null)
//				{
//					_logger.debug("PROXY-VOLE: Trying to resolv a proxy server for the protocoll '"+protocol+"', using the ProxySelector '"+_proxyVole.getClass().getName()+"', with uri='"+uri+"'.)");
//
//					proxyList = _proxyVole.select(uri);
//
//					if (proxyList != null && proxyList.size() > 0)
//					{
//						for (int i=0; i<proxyList.size(); i++)
//						{
//							_logger.debug("PROXY-VOLE: ProxyList["+i+"]: "+proxyList.get(i));
//						}
//
//						return proxyList;
//					}
//				}
//
//				_logger.debug("FALLBACK: Trying to resolv a proxy server for the protocoll '"+protocol+"', using the ProxySelector '"+_defsel.getClass().getName()+"', with uri='"+uri+"'.)");
//
//				proxyList = _defsel.select(uri);
//
//				for (int i=0; i<proxyList.size(); i++)
//				{
//					_logger.debug("FALLBACK: ProxyList["+i+"]: "+proxyList.get(i));
//				}
//
//				return proxyList;
//			}
//        }
//	}
//
//
//
//	private static class QueryString
//	{
//		private StringBuffer	_query	= new StringBuffer();
//		private int             _entries = 0;
//
//		public QueryString()
//		{
//		}
//		@SuppressWarnings("unused")
//		public QueryString(String name, String value)
//		{
//			encode(name, value);
//		}
//
////		public int entryCount()
////		{
////			return _entries;
////		}
////
////		public int length()
////		{
////			return _query.length();
////		}
//
//		public synchronized void add(String name, String value)
//		{
//			if (_query.length() > 0)
//				_query.append('&');
//			encode(name, value);
//		}
//
//		public synchronized void add(String name, int value)
//		{
//			if (_query.length() > 0)
//				_query.append('&');
//			encode(name, Integer.toString(value));
//		}
//
//		public synchronized void add(String name, long value)
//		{
//			if (_query.length() > 0)
//				_query.append('&');
//			encode(name, Long.toString(value));
//		}
//
//		private synchronized void encode(String name, String value)
//		{
//			if (value == null)
//			value = "";
//
//			// replace all '\', with '/', which makes some fields more readable.
//			value = value.replace('\\', '/');
//
//			try
//			{
//				_query.append(URLEncoder.encode(name, "UTF-8"));
//				_query.append('=');
//				_query.append(URLEncoder.encode(value, "UTF-8"));
//				
//				_entries++;
//			}
//			catch (UnsupportedEncodingException ex)
//			{
//				throw new RuntimeException("Broken VM does not support UTF-8");
//			}
//		}
//
//		public String getQuery()
//		{
//			return _query.toString();
//		}
//
//		@Override
//		public String toString()
//		{
//			return getQuery();
//		}
//	}
//
//	/*----------------------------------------------
//	**----------------------------------------------
//	**---- TEST ---- TEST ---- TEST ----- TEST -----
//	**----------------------------------------------
//	**----------------------------------------------
//	*/
//	public static void main(String args[])
//	{
//		Properties log4jProps = new Properties();
//		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
//		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
//		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
//		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
//		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
//		PropertyConfigurator.configure(log4jProps);
//
//		Configuration conf1 = new Configuration("c:\\projects\\asetune\\asetune.save.properties");
//		Configuration.setInstance(Configuration.USER_TEMP, conf1);
//
//		Configuration conf2 = new Configuration("c:\\projects\\asetune\\asetune.properties");
//		Configuration.setInstance(Configuration.SYSTEM_CONF, conf2);
//
////		System.setProperty("java.net.useSystemProxies", "true");
//		CheckForUpdates.init();
//
////		System.setProperty("http.proxyHost", "www-proxy.dummy.se");
////		System.setProperty("http.proxyPort", "8080");
//
//		CheckForUpdates check = new CheckForUpdates();
//
//		System.out.println("-- DO CHECK");
//		check.check(DEFAULT_TIMEOUT);
//		if ( check.checkSucceed() )
//		{
//			if (check.hasUpgrade())
//			{
//				System.out.println("-- HAS NEW UPGRADE");
//				System.out.println("-- new version '"+check.getNewAppVersionStr()+"'.");
//				System.out.println("-- download url '"+check.getDownloadUrl()+"'.");
//			}
//			else
//			{
//				System.out.println("-- NO NEED TO UPGRADE");
//			}
//			System.out.println("-- FEEDBACK_URL: '"+check.getFeedbackUrl()+"'.");
//		}
//		else
//		{
//			System.out.println("-- CHECK FAILED...");
//		}
//
//
//		CheckForUpdates udcCheck = new CheckForUpdates();
//		udcCheck.sendUdcInfo();
//	}
//}
//

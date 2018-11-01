package com.asetune.check;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.gui.Log4jLogRecord;
import com.asetune.utils.Configuration;
import com.asetune.utils.PlatformUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;
import com.btr.proxy.search.ProxySearch;
import com.btr.proxy.search.ProxySearch.Strategy;

/**
 * Connect to a URL and check if there are later software versions available.
 * <p>
 * It tries to use Proxy Servers for http/https if this is specified for the
 * cleint... (atleast for Windows and Gnome) see <code>ProxySelector</code>
 * for more info about this.
 * <p>
 * Please call <code>init()</code> method before any connect attemt is done
 * by any class that uses the network. Otherwise the <code>DefaultProxySelector</code>
 * implementation is initialized before the System Property <code>java.net.useSystemProxies</code>
 * is set. This means that we wont use this property, which means that we wont
 * be able to use the System Settings for Proxy servers.
 * <p>
 * Our own <code>ProxySelector</code> passes http/https protocolls to the
 * <code>DefaultProxySelector</code>...
 * <p>
 * NOTE: the DefaultProxySelector.(native).getSystemProxy()<br>
 * does NOT work with PAC files it just checks for static proxy settings<br>
 * http://sun.calstatela.edu/~cysun/documentation/java/1.5.0-source/j2se/src/windows/native/sun/net/spi/DefaultProxySelector.c
 * for more details...<br>
 * Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\ProxyEnable
 * If this is set to 0, then the whole thing is more or less not used...
 *
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
public abstract class CheckForUpdates
{
	private static Logger _logger = Logger.getLogger(CheckForUpdates.class);

//	private static final boolean _printDevTrace = false;

//	protected static final String ASETUNE_HOME_URL               = "http://www.dbxtune.com";
//	protected static final String ASETUNE_CHECK_UPDATE_URL       = "http://www.dbxtune.com/check_for_update.php";
//	protected static final String ASETUNE_CONNECT_INFO_URL       = "http://www.dbxtune.com/connect_info.php";
//	protected static final String ASETUNE_MDA_INFO_URL           = "http://www.dbxtune.com/mda_info.php";
//	protected static final String ASETUNE_UDC_INFO_URL           = "http://www.dbxtune.com/udc_info.php";
//	protected static final String ASETUNE_COUNTER_USAGE_INFO_URL = "http://www.dbxtune.com/counter_usage_info.php";
//	protected static final String ASETUNE_ERROR_INFO_URL         = "http://www.dbxtune.com/error_info.php";
//
//	protected static final String SQLWIN_CHECK_UPDATE_URL        = "http://www.dbxtune.com/sqlw_check_for_update.php";
//	protected static final String SQLWIN_CONNECT_INFO_URL        = "http://www.dbxtune.com/sqlw_connect_info.php";
//	protected static final String SQLWIN_COUNTER_USAGE_INFO_URL  = "http://www.dbxtune.com/sqlw_counter_usage_info.php";

//	protected static final String DEFAULT_DOWNLOAD_URL =  "http://www.dbxtune.com/download.html";
//	protected static final String DEFAULT_WHATSNEW_URL =  "http://www.dbxtune.com/history.html";
	
	protected String getHomeUrl()            { return "http://www.dbxtune.com"; };
	protected String getDefaultDownloadUrl() { return getHomeUrl() + "/download.html"; }
	protected String getDefaultWhatsNewUrl() { return getHomeUrl() + "/history.html"; }

	private static CheckForUpdates _instance = null;
	
	private boolean _sendConnectInfo      = true;
	private boolean _sendMdaInfo          = true;
	private int     _sendMdaInfoBatchSize = 5;
	private boolean _sendUdcInfo          = true;
	private boolean _sendCounterUsageInfo = true;
	private boolean _sendLogInfoWarning   = false;
	private boolean _sendLogInfoError     = false;

	private int     _connectCount         = 0;

	private int     _sendLogInfoThreshold = 100;
	private int     _sendLogInfoCount     = 0;

	/** What PHP variables will be used to pick up variables <br>
	 *  true  = _POST[], HTTP POST will be used, "no" limit in size... <br>
	 *  false = _GET[], send as: http://www.site.com?param1=var1&param2=var2 approximately 2000 chars is max<br>
	 */
	private boolean        _useHttpPost    = false;
	// Note: when redirecting URL from www.asetune.com -> www.dbxtune.com, it looses the POST entries
	//       So right now we need to use the 'http://www.site.com?param1=val1&param2=val2' instead

//	private URL	           _url;
	private String         _action        = "";

	// The below is protected, just because test purposes, it should be private
	protected String       _newAppVersion  = "";
	protected String       _downloadUrl    = "";
	protected String       _whatsNewUrl    = "";
	protected boolean      _checkNoOp      = false;
	protected boolean      _hasUpgrade;
	protected boolean      _checkSucceed;
	protected String       _responseString = "";

	protected String       _feedbackUrl     = "";
	protected String       _feedbackDateStr = "";
	protected java.util.Date _feedbackDate  = null;

	private static boolean _initialized = false;

	private int     _checkId     = -1;
//	private int     _checkIdSqlW = -1;

	private final static int DEFAULT_TIMEOUT = 20*1000;

//	static
//	{
//		_logger.setLevel(Level.DEBUG);
//	}

	protected boolean useHttpPost()             { return _useHttpPost; }
	protected int     getCheckId()              { return _checkId; }
	protected int     getConnectCount()         { return _connectCount; }
	protected int     getSendMdaInfoBatchSize() { return _sendMdaInfoBatchSize; }

	public static CheckForUpdates getInstance()
	{
		if (_instance == null)
			throw new RuntimeException("CheckForUpdate subsystem is not yet initialized.");

		return _instance;
	}
	public static void setInstance(CheckForUpdates instance)
	{
		_instance = instance;
	}
	public static boolean hasInstance()
	{
		return _instance != null;
	}
	public static boolean hasInstance(Class<?> clazz)
	{
		if (_instance == null)
			return false;
		return (clazz.isAssignableFrom(_instance.getClass()));
	}

	/**
	 * Initialize the class<br>
	 * This might have to be done before using any other connect() method,
	 * If the <code>DefaultProxySelector</code> is initialized by someone else
	 * and the system proerty <code>java.net.useSystemProxies</code> is not TRUE
	 * then it wont use System Proxy Settings.
	 */
	private void init()
	{
		if (_initialized)
			return;

		// In JDK 5, we can make the JDK go and get if we use proxies
		// for specific URL:s, just set the below property...
		System.setProperty("java.net.useSystemProxies", "true");

		// Create our own ProxySelector, but remember the Default one.
		OnlyHttpProxySelector ps = new OnlyHttpProxySelector(ProxySelector.getDefault());
		ProxySelector.setDefault( ps );

		_initialized = true;
	}



	/*---------------------------------------------------
	** BEGIN: constructors
	**---------------------------------------------------
	*/
	public CheckForUpdates()
	{
		init();
	}
	/*---------------------------------------------------
	** END: constructors
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** BEGIN: abstract methods that subclass needs to implement
	**---------------------------------------------------
	*/
//	public abstract QueryString       createCheckForUpdate();
//	public abstract QueryString       createSendConnectInfo(int connType, SshTunnelInfo sshTunnelInfo);
//	public abstract List<QueryString> createSendCounterUsageInfo();
//	public abstract List<QueryString> createSendMdaInfo();
//	public abstract QueryString       createSendUdcInfo();
//	public abstract QueryString       createSendLogInfo(Log4jLogRecord record, int sendLogInfoCount);

	public abstract QueryString       createCheckForUpdate(Object... params);
	public abstract QueryString       createSendConnectInfo(Object... params);
	public abstract List<QueryString> createSendCounterUsageInfo(Object... params);
	public abstract List<QueryString> createSendMdaInfo(Object... params);
	public abstract QueryString       createSendUdcInfo(Object... params);
	public abstract QueryString       createSendLogInfo(Object... params);
	/*---------------------------------------------------
	** END: abstract methods that subclass needs to implement
	**---------------------------------------------------
	*/

	
	/*---------------------------------------------------
	** BEGIN: Some basic methods
	**---------------------------------------------------
	*/
	/**
	 * Did the check succeed
	 */
	public boolean checkNoOp()
	{
		return _checkNoOp;
	}

	/**
	 * Did the check succeed
	 */
	public boolean checkSucceed()
	{
		return _checkSucceed;
	}

	/**
	 * What did the server responded with
	 */
	public String getResponseString()
	{
		return _responseString;
	}
	public boolean isResponseOfHtml()
	{
		if (StringUtil.isNullOrBlank(_responseString))
			return false;
		
		if (_responseString.indexOf("<html>") >= 0)
			return true;
		if (_responseString.indexOf("<HTML>") >= 0)
			return true;

		return false;
	}

	/**
	 * If there is a newer release for download, this would return true
	 */
	public boolean hasUpgrade()
	{
		return _hasUpgrade;
	}

	/**
	 * If there is an upgrade available, what version is the new one
	 */
	public String  getNewAppVersionStr()
	{
		return _newAppVersion;
	}

	/**
	 * If there is an upgrade available, where do we download it
	 */
	public String  getDownloadUrl()
	{
		if (_downloadUrl == null || _downloadUrl.trim().equals(""))
			return getDefaultDownloadUrl();
		return _downloadUrl;
	}

	/**
	 * @return
	 */
	public String getWhatsNewUrl()
	{
		if (_whatsNewUrl == null || _whatsNewUrl.trim().equals(""))
			return getDefaultWhatsNewUrl();
		return _whatsNewUrl;
	}

	/**
	 * @return null if no feedback URL was found, otherwise it's the URL where to fetch the feedback.
	 */
	public String getFeedbackUrl()
	{
		if (_feedbackUrl == null || _feedbackUrl.trim().equals(""))
			return null;
		return _feedbackUrl;
	}

	/**
	 * If there is a feedback page, this would return true
	 */
	public boolean hasFeedback()
	{
		return (getFeedbackUrl() != null);
	}

	/**
	 * Get last feedback time.
	 * @return
	 */
	public long getFeedbackTime()
	{
		if (_feedbackDate == null)
			return 0;

		return _feedbackDate.getTime();
	}

	/**
	 * what URL did we check at?
	 * @return
	 */
//	public URL getURL()
//	{
//		return _url;
//	}

	/**
	 * Send a HTTP data fields via X number of parameters<br>
	 * The request would look like: http://www.some.com?param1=val1&param2=val2<br>
	 * in a PHP page they could be picked up by: _GET['param1']<br>
	 * NOTE: there is a limit at about 2000 character for this type.
	 */
	protected InputStream sendHttpParams(String urlStr, QueryString urlParams)
	throws MalformedURLException, IOException
	{
		return sendHttpParams(urlStr, urlParams, DEFAULT_TIMEOUT);
	}
	protected InputStream sendHttpParams(String urlStr, QueryString urlParams, int timeoutInMs)
	throws MalformedURLException, IOException
	{
		if (_logger.isDebugEnabled())
		{
			_logger.debug("sendHttpParams() BASE URL: "+urlStr);
			_logger.debug("sendHttpParams() PARAMSTR: "+urlParams.toString());
		}

		// Get the URL
		URL url = new URL( urlStr + "?" + urlParams.toString() );

		// open the connection and prepare it to POST
		URLConnection conn = url.openConnection();
		conn.setConnectTimeout(timeoutInMs); 

		// Return the response
		return conn.getInputStream();
	}


	/**
	 * Send a HTTP data fields via POST handling<br>
	 * The request would look like: http://www.some.com?param1=val1&param2=val2<br>
	 * in a PHP page they could be picked up by: _POST['param1']
	 */
	protected InputStream sendHttpPost(String urlStr, QueryString urlParams)
	throws MalformedURLException, IOException
	{
		return sendHttpPost(urlStr, urlParams, DEFAULT_TIMEOUT);
	}
	protected InputStream sendHttpPost(String urlStr, QueryString urlParams, int timeoutInMs)
	throws MalformedURLException, IOException
	{
		if (_logger.isDebugEnabled())
		{
			_logger.debug("sendHttpPost() BASE URL: "+urlStr);
			_logger.debug("sendHttpPost() POST STR: "+urlParams.toString());
		}

		// Get the URL
		URL url = new URL(urlStr);

		// open the connection and prepare it to POST
		URLConnection conn = url.openConnection();
		conn.setConnectTimeout(timeoutInMs); 
		conn.setDoOutput(true);
		OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), "ASCII");

		// The POST line, the Content-type header,
		// and the Content-length headers are sent by the URLConnection.
		// We just need to send the data
		out.write(urlParams.toString());
		out.write("\r\n");
		out.flush();
		out.close();

		// Return the response
		return conn.getInputStream();
	}
	/*---------------------------------------------------
	** END: Some basic methods
	**---------------------------------------------------
	*/



	//-------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------
	/**
	 * Check for a later version of the software
	 * @param owner A JFrame or similar
	 * @param showNoUpgrade Even if no upgrade is available, show info about that in a GUI message.
	 */
	public void checkForUpdateNoBlock(final Component owner, final boolean showNoUpgrade, final boolean showFailure)
	{
		checkForUpdate(owner, showNoUpgrade, showFailure, DEFAULT_TIMEOUT, false);
	}
	/**
	 * Check for a later version of the software, in a blocking manner
	 * @param number of milliseconds to wait
	 */
	public void checkForUpdateBlock(final int timeout)
	{
		checkForUpdate(null, false, false, DEFAULT_TIMEOUT, true);
	}

	/**
	 * Check for a later version of the software
	 * @param owner A JFrame or similar
	 * @param showNoUpgrade Even if no upgrade is available, show info about that in a GUI message.
	 */
	private void checkForUpdate(final Component owner, final boolean showNoUpgrade, final boolean showFailure, final int timeout, boolean blockWhileChecking)
	{
		Runnable checkLater = new Runnable()
		{
			@Override
			public void run()
			{
				// Go and check
				checkForUpdate(timeout);

				if (checkNoOp())
				{
					_logger.info("Check for latest version was disabled... createCheckForUpdate(): didn't return any URL string.");
					return;
				}

				if (! checkSucceed())
				{
					if (owner != null && showFailure)
						CheckDialog.showDialog(owner, CheckForUpdates.this);

					_logger.info("Check for latest version failed.");
					return;
				}

				if (hasUpgrade())
				{
					if (owner != null)
						CheckDialog.showDialog(owner, CheckForUpdates.this);

					_logger.info("New Upgrade is Available. New version is '"+getNewAppVersionStr()+"' " +
							"and can be downloaded here '"+getDownloadUrl()+"'.");
				}
				else
				{
					if (showNoUpgrade)
					{
						if (owner != null)
							CheckDialog.showDialog(owner, CheckForUpdates.this);
					}

					if (hasFeedback())
					{
						if (owner != null)
							CheckDialog.showDialog(owner, CheckForUpdates.this);
					}

					if (isResponseOfHtml())
					{
						if (owner != null)
							CheckDialog.showDialog(owner, CheckForUpdates.this);
					}
					else
					{
						_logger.info("You have got the latest release of '"+Version.getAppName()+"'.");
					}
				}
			}
		};
		if (blockWhileChecking)
		{
			checkLater.run();
		}
		else
		{
			Thread checkThread = new Thread(checkLater);
			checkThread.setName("checkForUpdates");
			checkThread.setDaemon(true);
			checkThread.start();
		}
	}

	/**
	 * Go and check for updates
	 */
	private void checkForUpdate(int timeout)
	{
		// URS to use
//		String urlStr = ASETUNE_CHECK_UPDATE_URL;

		_hasUpgrade   = false;
		_checkSucceed = false;

		// COMPOSE: parameters to send to HTTP server
		QueryString urlParams = createCheckForUpdate();
		if (urlParams == null)
		{
			_checkNoOp = true;
			return;
		}

		try
		{
			// SEND OFF THE REQUEST
			InputStream in;
			if (_useHttpPost)
				in = sendHttpPost(urlParams.getUrl(), urlParams, timeout);
			else
				in = sendHttpParams(urlParams.getUrl(), urlParams, timeout);

			//----------------------------------------------
			// This is how a response would look like
			//----------------------------------------------
			// ACTION:UPGRADE:$ASETUNE_LATEST_VERSION_STR:$DOWNLOAD_URL"
			// ACTION:NO-UPGRADE
			//----------------------------------------------

			_action        = "";
			_newAppVersion = "";
			_downloadUrl   = getDefaultDownloadUrl();
			_whatsNewUrl   = getDefaultWhatsNewUrl();

			LineNumberReader lr = new LineNumberReader(new InputStreamReader(in));
			String line;
			String responseLines = "";
			boolean foundActionLine = false;
			while ((line = lr.readLine()) != null)
			{
				_logger.debug("response line "+lr.getLineNumber()+": " + line);
				responseLines += line;
				if (line.startsWith("ACTION:"))
				{
					foundActionLine = true;
					String[] sa = line.split(":");
					for (int i=0; i<sa.length; i++)
					{
						_logger.debug("   - STRING["+i+"]='"+sa[i]+"'.");

						if (i == 1) _action        = sa[1];
						if (i == 2) _newAppVersion = sa[2];
						if (i == 3) _downloadUrl   = sa[3];
						if (i == 4) _whatsNewUrl   = sa[4];
					}
				}
				if (line.startsWith("OPTIONS:"))
				{
					// OPTIONS: sendConnectInfo = true|false, sendUdcInfo = true|false, sendCounterUsageInfo = true|false
					String options = line.substring("OPTIONS:".length());
					_logger.debug("Receiving Options from server '"+options+"'.");
					String[] sa = options.split(",");
					for (int i=0; i<sa.length; i++)
					{
						String[] keyVal = sa[i].split("=");
						if (keyVal.length == 2)
						{
							String key = keyVal[0].trim();
							String val = keyVal[1].trim();
							boolean bVal = val.equalsIgnoreCase("true");

							if (key.equalsIgnoreCase("sendConnectInfo"))
							{
								_sendConnectInfo = bVal;
								_logger.debug("Setting option '"+key+"' to '"+bVal+"'.");
							}
							else if (key.equalsIgnoreCase("sendUdcInfo"))
							{
								_sendUdcInfo = bVal;
								_logger.debug("Setting option '"+key+"' to '"+bVal+"'.");
							}
							else if (key.equalsIgnoreCase("sendMdaInfo"))
							{
								_sendMdaInfo = bVal;
								_logger.debug("Setting option '"+key+"' to '"+bVal+"'.");
							}
							else if (key.equalsIgnoreCase("sendMdaInfoBatchSize"))
							{
								try
								{
									int intVal = Integer.parseInt(val);
									_sendMdaInfoBatchSize = intVal;
									_logger.debug("Setting option '"+key+"' to '"+intVal+"'.");
								}
								catch (NumberFormatException ex)
								{
									_logger.warn("Problems reading option '"+key+"', with value '"+val+"'. Can't convert to Integer. Caught: "+ex);
								}
							}
							else if (key.equalsIgnoreCase("sendCounterUsageInfo"))
							{
								_sendCounterUsageInfo = bVal;
								_logger.debug("Setting option '"+key+"' to '"+bVal+"'.");
							}
							else if (key.equalsIgnoreCase("sendLogInfoWarning"))
							{
								_sendLogInfoWarning = bVal;
								_logger.debug("Setting option '"+key+"' to '"+bVal+"'.");
							}
							else if (key.equalsIgnoreCase("sendLogInfoError"))
							{
								_sendLogInfoError = bVal;
								_logger.debug("Setting option '"+key+"' to '"+bVal+"'.");
							}
							else if (key.equalsIgnoreCase("sendLogInfoThreshold"))
							{
								try
								{
									_sendLogInfoThreshold = Integer.parseInt(val);
									_logger.debug("Setting option '"+key+"' to '"+val+"'.");
								}
								catch (NumberFormatException e)
								{
									_logger.debug("NumberFormatException: Setting option '"+key+"' to '"+val+"'.");
								}
							}
							else
							{
								_logger.debug("Unknown option '"+key+"' from server with value '"+val+"'.");
							}
						}
						else
							_logger.debug("Option '"+sa[i]+"' from server has strange key/valye.");
					}
				}
				if (line.startsWith("FEEDBACK:"))
				{
					// OPTIONS: sendConnectInfo = true|false, sendUdcInfo = true|false, sendCounterUsageInfo = true|false
					String feedback = line.substring("FEEDBACK:".length()).trim();
					_logger.debug("Receiving feedback from server '"+feedback+"'.");

					if ( ! "".equals(feedback) )
					{
						String[] sa = feedback.split(":");
						for (int i=0; i<sa.length; i++)
						{
							if (i == 0) _feedbackDateStr = sa[0];
							if (i == 1) _feedbackUrl     = sa[1];
						}
						try
						{
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
							_feedbackDate = sdf.parse(_feedbackDateStr);
						}
						catch (ParseException e)
						{
						}
					}
				}
				if (line.startsWith("ERROR:"))
				{
					_logger.warn("When checking for new version, found an 'ERROR:' response row, which looked like '" + line + "'.");
				}
				if (line.startsWith("CHECK_ID:"))
				{
					String actionResponse = line.substring( "CHECK_ID:".length() ).trim();
					try { _checkId = Integer.parseInt(actionResponse); }
					catch (NumberFormatException ignore) {}
					_logger.debug("Received check_id='"+_checkId+"' from update site.");
				}
			}
			in.close();
			_responseString = responseLines;

			// if not empty, check that it starts with 'http://' otherwise add it to the start
			if ( _downloadUrl != null && !_downloadUrl.trim().equals("") )
			{
				if ( ! _downloadUrl.startsWith("http://") )
					_downloadUrl = "http://" + _downloadUrl;
			}

			// if not empty, check that it starts with 'http://' otherwise add it to the start
			if ( _whatsNewUrl != null && !_whatsNewUrl.trim().equals("") )
			{
				if ( ! _whatsNewUrl.startsWith("http://") )
					_whatsNewUrl = "http://" + _whatsNewUrl;
			}

			// if not empty, check that it starts with 'http://' otherwise add it to the start
			if ( _feedbackUrl != null && !_feedbackUrl.trim().equals("") )
			{
				if ( ! _feedbackUrl.startsWith("http://") )
					_feedbackUrl = "http://" + _feedbackUrl;
			}

			if (_action.equals("UPGRADE"))
			{
				_hasUpgrade = true;
				_logger.debug("-UPGRADE-");
				_logger.debug("-to:"+_newAppVersion);
				_logger.debug("-at:"+_downloadUrl);
				_logger.debug("-at:"+_whatsNewUrl);
			}
			else
			{
				_hasUpgrade = false;
				_logger.debug("-NO-UPGRADE-");
			}

			if ( ! foundActionLine )
			{
				_logger.warn("When checking for new version, no 'ACTION:' response was found. The response rows was '" + responseLines + "'.");
			}

			_checkSucceed = true;
		}
		catch (IOException ex)
		{
			_logger.debug("When we checking for later version, we had problems", ex);
		}
	}


	//-------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------
	/**
	 * @param connType ConnectionDialog.TDS_CONN | ConnectionDialog.OFFLINE_CONN
	 * @param sshTunnelInfo 
	 */
	public void sendConnectInfoNoBlock(final Object... params)
	{
		if ( ! _sendConnectInfo )
		{
			_logger.debug("Send 'Connect info' has been disabled.");
			return;
		}

		Runnable doLater = new Runnable()
		{
			@Override
			public void run()
			{
				sendConnectInfo(params);

				sendUdcInfo();
			}
		};
		Thread checkThread = new Thread(doLater);
		checkThread.setName("sendConnectInfo");
		checkThread.setDaemon(true);
		checkThread.start();
	}

	/**
	 * Send info on connection
	 * @param sshTunnelInfo 
	 */
	public void sendConnectInfo(Object... params)
	{
		// URL TO USE
//		String urlStr = ASETUNE_CONNECT_INFO_URL;

		if ( ! _sendConnectInfo )
		{
			_logger.debug("Send 'Connect info' has been disabled.");
			return;
		}

		if (_checkId < 0)
		{
			_logger.debug("No checkId was discovered when trying to send connection info, skipping this.");
			return;
		}

		_connectCount++;

		// COMPOSE: parameters to send to HTTP server
		QueryString urlParams = createSendConnectInfo(params);
		if (urlParams == null)
			return;

		try
		{
			// SEND OFF THE REQUEST
			InputStream in;
			if (_useHttpPost)
				in = sendHttpPost(urlParams.getUrl(), urlParams);
			else
				in = sendHttpParams(urlParams.getUrl(), urlParams);

			LineNumberReader lr = new LineNumberReader(new InputStreamReader(in));
			String line;
			String responseLines = "";
			while ((line = lr.readLine()) != null)
			{
				_logger.debug("response line "+lr.getLineNumber()+": " + line);
				responseLines += line;
				if (line.startsWith("ERROR:"))
				{
					_logger.warn("When doing connection info 'ERROR:' response row, which looked like '" + line + "'.");
				}
				if (line.startsWith("DONE:"))
				{
				}
				if (line.startsWith("SEND_MDA_INFO:"))
				{
					_logger.info("Received info to collect MDA Information.");
					sendMdaInfoNoBlock();
				}
			}
			in.close();
			_responseString = responseLines;

//			_checkSucceed = true;
		}
		catch (IOException ex)
		{
			_logger.debug("when trying to send connection info, we had problems", ex);
		}
	}






	//-------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------

	public void sendMdaInfoNoBlock()
	{
		if ( ! _sendMdaInfo )
		{
			_logger.debug("Send 'MDA info' has been disabled.");
			return;
		}

		Runnable doLater = new Runnable()
		{
			@Override
			public void run()
			{
				sendMdaInfo();
			}
		};
		Thread checkThread = new Thread(doLater);
		checkThread.setName("sendMdaInfo");
		checkThread.setDaemon(true);
		checkThread.start();
	}


	public void sendMdaInfo()
	{
		// URL TO USE
//		String urlStr = ASETUNE_MDA_INFO_URL;

		if ( ! _sendMdaInfo )
		{
			_logger.debug("Send 'MDA info' has been disabled.");
			return;
		}

		if (_checkId < 0)
		{
			_logger.debug("No checkId was discovered when trying to send connection info, skipping this.");
			return;
		}

		List<QueryString> sendQueryList = createSendMdaInfo();
		if (sendQueryList == null || (sendQueryList != null && sendQueryList.isEmpty()))
			return;


		if (sendQueryList.size() > 0)
		{
			int rowCountSum = 0;
			for (QueryString urlEntry : sendQueryList)
				rowCountSum += urlEntry.getCounter();

//			_logger.info("sendMdaInfo: Starting to send "+rowCountSum+" MDA information entries in "+sendQueryList.size()+" batches, for ASE Version '"+mtd.getAseExecutableVersionNum()+"'.");
			_logger.info("sendMdaInfo: Starting to send "+rowCountSum+" MDA information entries in "+sendQueryList.size()+" batches.");
			long startTime = System.currentTimeMillis();
			for (QueryString urlEntry : sendQueryList)
			{
				try
				{
					// SEND OFF THE REQUEST
					InputStream in;
					if (_useHttpPost)
						in = sendHttpPost(urlEntry.getUrl(), urlEntry);
					else
						in = sendHttpParams(urlEntry.getUrl(), urlEntry);
		
					LineNumberReader lr = new LineNumberReader(new InputStreamReader(in));
					String line;
					String responseLines = "";
					while ((line = lr.readLine()) != null)
					{
						_logger.debug("response line "+lr.getLineNumber()+": " + line);
						responseLines += line;
						if (line.startsWith("ERROR:"))
						{
							_logger.warn("When doing MDA info 'ERROR:' response row, which looked like '" + line + "'.");
						}
						if (line.startsWith("DONE:"))
						{
						}
					}
					in.close();
					_responseString = responseLines;
				}
				catch (IOException ex)
				{
					_logger.debug("when trying to send MDA info, we had problems", ex);
				}
			}
			String execTimeStr = TimeUtils.msToTimeStr(System.currentTimeMillis() - startTime);
			_logger.info("sendMdaInfo: this took '"+execTimeStr+"' for all "+sendQueryList.size()+" batches, rows send was "+rowCountSum+".");
		}
	}



	//-------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------

	public void sendUdcInfoNoBlock()
	{
		// TRACE IN DEVELOPMENT
//		if (_printDevTrace && _checkId < 0)
//		{
//			System.out.println("DEV-TRACE(CheckForUpdates): sendUdcInfoNoBlock(): ");
//		}

		if ( ! _sendUdcInfo )
		{
			_logger.debug("Send 'UDC info' has been disabled.");
			return;
		}

		Runnable doLater = new Runnable()
		{
			@Override
			public void run()
			{
				sendUdcInfo();
			}
		};
		Thread checkThread = new Thread(doLater);
		checkThread.setName("sendUdcInfo");
		checkThread.setDaemon(true);
		checkThread.start();
	}

	/**
	 * Send info on User Defined Counters
	 */
	public void sendUdcInfo()
	{
		// URL TO USE
//		String urlStr = ASETUNE_UDC_INFO_URL;

		if ( ! _sendUdcInfo )
		{
			_logger.debug("Send 'UDC info' has been disabled.");
			return;
		}

		if (_checkId < 0)
		{
			_logger.debug("No checkId was discovered when trying to send UDC info, skipping this.");
			return;
		}

		// COMPOSE: parameters to send to HTTP server
		QueryString urlParams = createSendUdcInfo();
		if (urlParams == null)
			return;

		try
		{
			// SEND OFF THE REQUEST
			InputStream in;
			if (_useHttpPost)
				in = sendHttpPost(urlParams.getUrl(), urlParams);
			else
				in = sendHttpParams(urlParams.getUrl(), urlParams);

			LineNumberReader lr = new LineNumberReader(new InputStreamReader(in));
			String line;
			String responseLines = "";
			while ((line = lr.readLine()) != null)
			{
				_logger.debug("response line "+lr.getLineNumber()+": " + line);
				responseLines += line;
				if (line.startsWith("ERROR:"))
				{
					_logger.warn("When doing UDC info 'ERROR:' response row, which looked like '" + line + "'.");
				}
				if (line.startsWith("DONE:"))
				{
				}
			}
			in.close();
			_responseString = responseLines;
		}
		catch (IOException ex)
		{
			_logger.debug("when trying to send UDC info, we had problems", ex);
		}
	}






	//-------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------

//	private static boolean _sendCounterUsage_done = false;
	private int _sendCounterUsage_atConnectCount = 0;

	public void sendCounterUsageInfo(boolean blockingCall, final Object... params)
	{
		if ( ! _sendCounterUsageInfo )
		{
			_logger.debug("Send 'Counter Usage Info' has been disabled.");
			return;
		}

		Runnable doLater = new Runnable()
		{
			@Override
			public void run()
			{
				if ( _sendCounterUsage_atConnectCount < _connectCount )
				{
					_sendCounterUsage_atConnectCount++;

					sendCounterUsageInfo(params);
				}
				else
				{
					_logger.debug("sendCounterUsageInfo, already done (_sendCounterUsage_atConnectCount="+_sendCounterUsage_atConnectCount+", _connectCount="+_connectCount+").");
				}
			}
		};
		if (blockingCall)
		{
			// if synchronous, just call the run method on current thread
			doLater.run();
		}
		else
		{
			// no blocking call, start a thread that does it.
			Thread checkThread = new Thread(doLater);
			checkThread.setName("sendCounterUsageInfo");
			checkThread.setDaemon(true);
			checkThread.start();
		}
	}

	/**
	 * Send info on User Defined Counters
	 */
	private void sendCounterUsageInfo(Object... params)
	{
		// URL TO USE
//		String urlStr = ASETUNE_COUNTER_USAGE_INFO_URL;

		if ( ! _sendCounterUsageInfo )
		{
			_logger.debug("Send 'Counter Usage Info' has been disabled.");
			return;
		}

		if (_checkId < 0)
		{
			_logger.debug("No checkId was discovered when trying to send 'Counter Usage' info, skipping this.");
			return;
		}


		// COMPOSE: parameters to send to HTTP server
		List<QueryString> sendQueryList = createSendCounterUsageInfo(params);
		if (sendQueryList == null || (sendQueryList != null && sendQueryList.isEmpty()))
			return;

		// Loop each of the request and send off data
		for (QueryString urlEntry : sendQueryList)
		{
			try
			{
				// If NOT in the "known" thread name, then it's a synchronous call, then lower the timeout value. 
				int timeout = DEFAULT_TIMEOUT;
				String threadName = Thread.currentThread().getName(); 
				if ( ! "sendCounterUsageInfo".equals(threadName) )
					timeout = 5000;

				// SEND OFF THE REQUEST
				InputStream in;
				if (_useHttpPost)
					in = sendHttpPost(urlEntry.getUrl(), urlEntry, timeout);
				else
					in = sendHttpParams(urlEntry.getUrl(), urlEntry, timeout);

				LineNumberReader lr = new LineNumberReader(new InputStreamReader(in));
				String line;
				String responseLines = "";
				while ((line = lr.readLine()) != null)
				{
					_logger.debug("response line "+lr.getLineNumber()+": " + line);
					responseLines += line;
					if (line.startsWith("ERROR:"))
					{
						_logger.warn("When doing 'Counter Usage' info 'ERROR:' response row, which looked like '" + line + "'.");
					}
					if (line.startsWith("DONE:"))
					{
					}
				}
				in.close();
				_responseString = responseLines;

//				_checkSucceed = true;
			}
			catch (IOException ex)
			{
				_logger.debug("when trying to send 'Counter Usage' info, we had problems", ex);
			}
		}
	}


	
	
	//-------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------
	
	/**
	 * Note: Since this is synchronized, make sure that no _logger.{warn|error} is done inside the 
	 * synchronized block, because then we will have a deadlock.
	 * <p>
	 * If we don't synchronize, then error/warn messages that are issued rapidly will be discarded, 
	 * this due to _sendLogInfoCount is "global" and will be incremented... at least that's what I think.<br>
	 * Another way of doing this would be to have a synchronized List where messages are pushed to.
	 */
	public synchronized void sendLogInfoNoBlock(final Log4jLogRecord record)
	{
		if (_checkId < 0)
		{
			_logger.debug("No checkId was discovered when trying to send ERROR info, skipping this.");
			return;
		}

		if (record == null)
			return;
		
		if ( record.isWarningLevel() && ! _sendLogInfoWarning )
		{
			_logger.debug("Send 'LOG WARNING info' has been disabled.");
			return;
		}
		if ( record.isSevereLevel() && ! _sendLogInfoError )
		{
			_logger.debug("Send 'LOG ERROR info' has been disabled.");
			return;
		}

		// DISCARD VARIOUS MESSAGES
		String msg = record.getMessage();
		if (msg != null)
		{
			// Filter out following messages:
			//_logger.warn("When trying to initialize Counters Model '"+getName()+"' The following role(s) were needed '"+StringUtil.toCommaStr(dependsOnRole)+"', and you do not have the following role(s) '"+didNotHaveRoles+"'.");
			//_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"' in ASE Version "+getServerVersion()+", I found that '"+configName+"' wasn't configured (which is done with: sp_configure '"+configName+"'"+reconfigOptionStr+"), so monitoring information about '"+getDisplayName()+"' will NOT be enabled.");
			//_logger.warn("When trying to initialize Counters Model '"+getName()+"' in ASE Version "+getServerVersionStr()+", I need atleast ASE Version "+getDependsOnVersionStr()+" for that.");
			//_logger.warn("When trying to initialize Counters Model '"+getName()+"' in ASE Cluster Edition Version "+getServerVersionStr()+", I need atleast ASE Cluster Edition Version "+getDependsOnCeVersionStr()+" for that.");
			//_logger.warn("When trying to initialize Counters Model '"+getName()+"' in ASE Version "+getServerVersion()+", "+msg+" (connect with a user that has '"+needsRoleToRecreate+"' or load the proc from '$ASETUNE_HOME/classes' or unzip asetune.jar. under the class '"+scriptLocation.getClass().getName()+"' you will find the script '"+scriptName+"').");
			if (msg.startsWith("When trying to initialize Counters Model")) 
				return;

			// DDL Lookup Storage: Discard entry due to PCS 'PersistWriterJdbc' has not yet been fully started'. (Storage tables might not be there yet).
			if (msg.startsWith("DDL Lookup Storage: Discard entry due to PCS")) 
				return;
			
			// Problems connecting/sending JSON-REST call to 'http://localhost:8080/api/pcs/receiver'. The entry will be saved in the 'error-queue' and sent later. Caught: java.net.ConnectException: Connection refused (Connection refused)
			if (msg.indexOf("Problems connecting/sending JSON-REST call to") >= 0 && msg.indexOf("java.net.ConnectException") >= 0) 
				return;

			// Rejected 1 plan names due to ' not executed '. For the last '01:00' (HH:MM), The following plans was rejected (planName=count). {*ss0986313580_1062367249ss*=1}	
			if (msg.indexOf("plan names due to ' not executed '.") >= 0)
				return;

			// The configuration 'statement pipe max messages' might be to low. For the last '01:00' (HH:MM), We have read 25225 rows. On 1 occations. Average read per occation was 25225 rows. And the configuration value for 'statement pipe max messages' is 25000		
			if (msg.indexOf("max messages' might be to low. For the last") >= 0)
				return;
			
			
			if (msg.startsWith("CounterSample(CmRaSysmon).getCnt : 99000 ERROR: Found NO database that was marked for replication"))
				return;
			if (msg.startsWith("Date problems for table 'CmStmntCacheDetails"))
				return;
			
			// The persistent queue has 3 entries. The persistent writer might not keep in pace. The current consumer has been active for 00:00:04.124	
			// When the PCS has been active for MORE THAN 1 MINUTE
			if (msg.indexOf("The persistent writer might not keep in pace. The current consumer has been active for") >= 0)
			{
				// Discard message when the Writer has worked for less than a minute
				// OR: send only records that seems to be strange
				if (msg.indexOf("00:00:") >= 0) 
					return;
			}
		}
		
		// Check any sub-implemeters that extends CheckForUpdate 
		if (discardLogInfoMessage(record))
			return;
		
		if (_sendLogInfoCount > _sendLogInfoThreshold)
		{
			_logger.debug("Send 'LOG info' has exceed sendLogInfoThreshold="+_sendLogInfoThreshold+", sendLogInfoCount="+_sendLogInfoCount);
			return;
		}
		// NOTE: we may need to synchronize this, but on the other hand it's just a counter incrementation
		final int sendLogInfoCount = _sendLogInfoCount++;

		// SEND This using a thread
		Runnable doLater = new Runnable()
		{
			@Override
			public void run()
			{
				sendLogInfo(record, sendLogInfoCount);
			}
		};
		Thread checkThread = new Thread(doLater);
		checkThread.setName("sendLogInfo");
		checkThread.setDaemon(true);
		checkThread.start();
	}
	
	/**
	 * Check if we should send a specific log message to dbxtune.com
	 * 
	 * @param record
	 * @return true if the message should be discarded
	 */
	public boolean discardLogInfoMessage(final Log4jLogRecord record)
	{
		String msg = record.getMessage();
		if (msg != null)
		{
			// check for various messages
			//if (msg.startsWith("Some message that we do not want to send")) 
			//	return;
		}

		return false;
	}

	/**
	 * Send info on ERRORs
	 */
	public void sendLogInfo(final Log4jLogRecord record, int sendLogInfoCount)
	{
		// URL TO USE
//		String urlStr = ASETUNE_ERROR_INFO_URL;

		if (_checkId < 0)
		{
			_logger.debug("No checkId was discovered when trying to send LOG info, skipping this.");
			return;
		}

		if (record == null)
			return;
		
		if ( record.isWarningLevel() && ! _sendLogInfoWarning )
		{
			_logger.debug("Send 'LOG WARNING info' has been disabled.");
			return;
		}
		if ( record.isSevereLevel() && ! _sendLogInfoError )
		{
			_logger.debug("Send 'LOG ERROR info' has been disabled.");
			return;
		}

		QueryString urlParams = createSendLogInfo(record, sendLogInfoCount);
		if (urlParams == null)
			return;

		try
		{
			// SEND OFF THE REQUEST
			InputStream in;
			if (_useHttpPost)
				in = sendHttpPost(urlParams.getUrl(), urlParams, 750); // timeout after 750 ms
			else
				in = sendHttpParams(urlParams.getUrl(), urlParams, 750); // timeout after 750 ms

			LineNumberReader lr = new LineNumberReader(new InputStreamReader(in));
			String line;
			String responseLines = "";
			while ((line = lr.readLine()) != null)
			{
				_logger.debug("response line "+lr.getLineNumber()+": " + line);
				responseLines += line;
				if (line.startsWith("ERROR:"))
				{
					_logger.info("When doing LOG info 'ERROR:' response row, which looked like '" + line + "'.");
				}
				if (line.startsWith("DONE:"))
				{
				}
			}
			in.close();
			_responseString = responseLines;
		}
		catch (IOException ex)
		{
			_logger.debug("when trying to send LOG info, we had problems", ex);
		}
	}





	/*---------------------------------------------------
	**---------------------------------------------------
	**---- SUBCLASSES ---- SUBCLASES ---- SUBCLASES -----
	**---------------------------------------------------
	**---------------------------------------------------
	*/
	private static class OnlyHttpProxySelector
	extends ProxySelector
	{
		ProxySelector _defsel = null;
		ProxySelector _proxyVole = null;

		public OnlyHttpProxySelector(ProxySelector def)
		{
			if (def == null)
			{
				throw new IllegalArgumentException("ProxySelector can't be null.");
			}
			_defsel = def;
			_logger.debug("Installing a new ProxySelector, but I will save and use the default '"+_defsel.getClass().getName()+"'.");

			//===============================================
			// Use proxy-vole project:
			//-----------------------------------------------
			// install the log backend implementation
			com.btr.proxy.util.Logger.setBackend(new com.btr.proxy.util.Logger.LogBackEnd()
			{
				// Discard messages if they have already been seen, use a timestamp to determen if we should re-log them again
				private HashMap<String, Long> _logDiscardCache = new HashMap<String, Long>();
				private long _reLogTimeout = 1000 * 900; // 15 minutes

				@Override
				public boolean isLogginEnabled(com.btr.proxy.util.Logger.LogLevel logLevel)
				{
//					System.out.println("PROXY-VOLE.isLogginEnabled: logLevel="+logLevel);
//					return true;
					if      (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.TRACE)   && _logger.isTraceEnabled()) return true;
					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.DEBUG)   && _logger.isDebugEnabled()) return true;
					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.INFO)    && _logger.isInfoEnabled())  return true;
					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.WARNING) && _logger.isEnabledFor(Level.WARN))  return true;
					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.ERROR)   && _logger.isEnabledFor(Level.ERROR))  return true;
					else return false;
				}

//				public void log(Class clazz, com.btr.proxy.util.Logger.LogLevel logLevel, String message, Object[] params)
				@Override
				public void log(Class<?> clazz, com.btr.proxy.util.Logger.LogLevel logLevel, String message, Object... params)
				{
					String msg = MessageFormat.format(message, params);
					msg = clazz.getName() + ": " + msg;

					// Check input parameters, and keep last Throwable if any of that cind.
					Throwable t = null;
					String tMsg = "";
					for (Object param : params)
					{
						if (param instanceof Throwable)
						{
							t = (Throwable)param;
							tMsg = " Caught: " + t;
						}
					}
					Configuration conf = Configuration.getCombinedConfiguration();
					if ( ! conf.getBooleanProperty("proxyvole.debug.stacktaceOnLogMessages", false) )
						t = null;

					String logMsg = msg + tMsg;

					// If the message has been written lately, discard it...
					// TODO: maybe add a counter, so we can write how many times it has been repeated/discared
					if (_logDiscardCache.containsKey(logMsg))
					{
						long firstLogTime = _logDiscardCache.get(logMsg);
						long firstLogAge  = System.currentTimeMillis() - firstLogTime;
						if ( firstLogAge < _reLogTimeout )
						{
							if (_logger.isDebugEnabled())
								_logger.debug("com.btr.proxy.util.Logger.LogBackEnd: Discarding log message, (firstLogAge='"+firstLogAge+"', re-logTimeout='"+_reLogTimeout+"': "+logMsg);
							return;
						}
					}
					_logDiscardCache.put(logMsg, new Long(System.currentTimeMillis()));

					
//					System.out.println("PROXY-VOLE.log(logLevel="+logLevel+"): "+msg);
					if      (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.TRACE)  ) _logger.trace(logMsg, t);
					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.DEBUG)  ) _logger.debug(logMsg, t);
					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.INFO)   ) _logger.info( logMsg, t);
//					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.WARNING)) _logger.warn( logMsg, t);
//					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.ERROR)  ) _logger.error(logMsg, t);
					// downgrade WAR and ERROR to INFO, this so it wont open the log window
					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.WARNING)) _logger.info( logMsg, t);
					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.ERROR)  ) _logger.info( logMsg, t);
					else _logger.info("Unhandled loglevel("+logLevel+"): " + logMsg, t);
				}
			});

			// Use the DEFAULT or SPECIALIZED "search order" for proxy resolver.
			boolean useProxyVoleDefault = false;
			ProxySearch proxySearch = null;
			if (useProxyVoleDefault)
			{
				// The below does:
				//  headless: JAVA, OS_DEFAULT, ENV_VAR
				// !headless: JAVA, BROWSER, OS_DEFAULT, ENV_VAR
				proxySearch = ProxySearch.getDefaultProxySearch();
			}
			else
			{
				proxySearch = new ProxySearch();

				// Test if we are a server or a client.
				boolean headless = GraphicsEnvironment.isHeadless();

				if (headless)
				{
					proxySearch.addStrategy(Strategy.JAVA);
					proxySearch.addStrategy(Strategy.OS_DEFAULT);
					proxySearch.addStrategy(Strategy.ENV_VAR);
				}
				else
				{
					proxySearch.addStrategy(Strategy.JAVA);
					if (PlatformUtils.getCurrentPlattform() == PlatformUtils.Platform_WIN)
					{
						// the below, changes from default to add IE & FIREFOX...
						// the default for windows is just IE
						proxySearch.addStrategy(Strategy.IE);
						proxySearch.addStrategy(Strategy.FIREFOX);
					}
					else
						proxySearch.addStrategy(Strategy.BROWSER);
					proxySearch.addStrategy(Strategy.OS_DEFAULT);
					proxySearch.addStrategy(Strategy.ENV_VAR);
				}
				com.btr.proxy.util.Logger.log(ProxySearch.class, com.btr.proxy.util.Logger.LogLevel.TRACE, "Using SPECIALIZED search priority: {0}", new Object[] {proxySearch});
			}

			try
			{
				_proxyVole = proxySearch.getProxySelector();
				if (_proxyVole == null)
				{
					_logger.debug("PROXY-VOLE: Could not be used as a ProxySelector, getProxySelector() returned null.");
				}
			}
			catch (Throwable e)
			{
				// NoClassDefFoundError: org/mozilla/javascript/ScriptableObject
				// is thrown if we are using JVM 1.5 and the js.jar is not part of the classpath
				// js.jar is Java Script Engine: Rhino, http://www.mozilla.org/rhino/scriptjava.html
				// which is not part of this distribution, in JVM 6, we use the built in Java Script Engine (javax.script)
				System.out.println("When initializing 'proxy-vole' caught exception '"+e+"', but I will continue anyway. ");
				_logger.info("When initializing 'proxy-vole' caught exception '"+e+"', but I will continue anyway.");
			}
		}

		@Override
		public void connectFailed(URI uri, SocketAddress sa, IOException ioe)
		{
			if (_proxyVole != null)
				_proxyVole.connectFailed(uri, sa, ioe);
		}

		@Override
		public List<Proxy> select(URI uri)
		{
			if (uri == null)
			{
				throw new IllegalArgumentException("URI can't be null.");
			}
			String protocol = uri.getScheme();

			_logger.debug("called select(uri): protocoll '"+protocol+"' with uri='"+uri+"')");

			if ( "socket".equalsIgnoreCase(protocol) )
			{
				_logger.debug("Shortcircuting proxy resolving for the protocol '"+protocol+"'. No proxy server will be used.");

				List<Proxy> proxyList = new ArrayList<Proxy>();
				proxyList.add(Proxy.NO_PROXY);
				return proxyList;
			}
			else
			{
				List<Proxy> proxyList = null;
				if (_proxyVole != null)
				{
					_logger.debug("PROXY-VOLE: Trying to resolv a proxy server for the protocoll '"+protocol+"', using the ProxySelector '"+_proxyVole.getClass().getName()+"', with uri='"+uri+"'.)");

					proxyList = _proxyVole.select(uri);

					if (proxyList != null && proxyList.size() > 0)
					{
						for (int i=0; i<proxyList.size(); i++)
						{
							_logger.debug("PROXY-VOLE: ProxyList["+i+"]: "+proxyList.get(i));
						}

						return proxyList;
					}
				}

				_logger.debug("FALLBACK: Trying to resolv a proxy server for the protocoll '"+protocol+"', using the ProxySelector '"+_defsel.getClass().getName()+"', with uri='"+uri+"'.)");

				proxyList = _defsel.select(uri);

				for (int i=0; i<proxyList.size(); i++)
				{
					_logger.debug("FALLBACK: ProxyList["+i+"]: "+proxyList.get(i));
				}

				return proxyList;
			}
        }
	}



	protected static class QueryString
	{
		private String          _url	 = null;
		private StringBuffer    _query	 = new StringBuffer();
		private int             _counter = 0;

		public QueryString(String url)
		{
			setUrl(url);
		}

		public QueryString(String name, String value)
		{
			encode(name, value);
		}

//		public int entryCount()
//		{
//			return _entries;
//		}
//
//		public int length()
//		{
//			return _query.length();
//		}

		public synchronized void add(String name, String value)
		{
			if (_query.length() > 0)
				_query.append('&');
			encode(name, value);
		}

		public synchronized void add(String name, int value)
		{
			if (_query.length() > 0)
				_query.append('&');
			encode(name, Integer.toString(value));
		}

		public synchronized void add(String name, long value)
		{
			if (_query.length() > 0)
				_query.append('&');
			encode(name, Long.toString(value));
		}

		private synchronized void encode(String name, String value)
		{
			if (value == null)
			value = "";

			// replace all '\', with '/', which makes some fields more readable.
			value = value.replace('\\', '/');

			try
			{
				_query.append(URLEncoder.encode(name, "UTF-8"));
				_query.append('=');
				_query.append(URLEncoder.encode(value, "UTF-8"));
			}
			catch (UnsupportedEncodingException ex)
			{
				throw new RuntimeException("Broken VM does not support UTF-8");
			}
		}

		/** This is just a number that can be used for "anything" */
		public int getCounter()
		{
			return _counter;
		}
		/** This is just a number that can be used for "anything" */
		public void setCounter(int counter)
		{
			_counter = counter;
		}

		public String getUrl()
		{
			return _url;
		}
		public void setUrl(String url)
		{
			_url = url;
		}

		public String getQuery()
		{
			return _query.toString();
		}

		@Override
		public String toString()
		{
			return getQuery();
		}
	}



	/*----------------------------------------------
	**----------------------------------------------
	**---- TEST ---- TEST ---- TEST ----- TEST -----
	**----------------------------------------------
	**----------------------------------------------
	*/
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
//		Configuration conf2 = new Configuration("c:\\projects\\asetune\\dbxtune.properties");
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
}


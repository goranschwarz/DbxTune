package com.asetune.check;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.sql.Date;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


import com.asetune.AseTune;
import com.asetune.GetCounters;
import com.asetune.MonTablesDictionary;
import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.Configuration;
import com.asetune.utils.PlatformUtils;
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
public class CheckForUpdates
{
	private static Logger _logger = Logger.getLogger(CheckForUpdates.class);

// The redirect from www.asetune.com -> www.asemon.se seems like it's stripping all the parameters
// So this is a bit problematic...

//	protected static final String ASETUNE_HOME_URL               = "http://www.asetune.com";
//	private   static final String ASETUNE_CHECK_UPDATE_URL       = "http://www.asetune.com/check_for_update.php";
//	private   static final String ASETUNE_CONNECT_INFO_URL       = "http://www.asetune.com/connect_info.php";
//	private   static final String ASETUNE_UDC_INFO_URL           = "http://www.asetune.com/udc_info.php";
//	private   static final String ASETUNE_COUNTER_USAGE_INFO_URL = "http://www.asetune.com/counter_usage_info.php";
//
//	private static final String DEFAULT_DOWNLOAD_URL =  "http://www.asetune.com/download.html";
//	private static final String DEFAULT_WHATSNEW_URL =  "http://www.asetune.com/history.html";
	
	protected static final String ASETUNE_HOME_URL               = "http://www.asemon.se";
	private   static final String ASETUNE_CHECK_UPDATE_URL       = "http://www.asemon.se/check_for_update.php";
	private   static final String ASETUNE_CONNECT_INFO_URL       = "http://www.asemon.se/connect_info.php";
	private   static final String ASETUNE_UDC_INFO_URL           = "http://www.asemon.se/udc_info.php";
	private   static final String ASETUNE_COUNTER_USAGE_INFO_URL = "http://www.asemon.se/counter_usage_info.php";

	private static final String DEFAULT_DOWNLOAD_URL =  "http://www.asemon.se/download.html";
	private static final String DEFAULT_WHATSNEW_URL =  "http://www.asemon.se/history.html";

	private static boolean _sendConnectInfo      = true;
	private static boolean _sendUdcInfo          = true;
	private static boolean _sendCounterUsageInfo = true;

	private URL	           _url;
	private QueryString	   _query	       = new QueryString();
	private String         _action        = "";
	
	// The below is protected, just because test purposes, it should be private
	protected String       _asetuneVersion = "";
	protected String       _downloadUrl    = "";
	protected String       _whatsNewUrl    = "";
	protected boolean      _hasUpgrade;
	protected boolean      _checkSucceed;

	protected String       _feedbackUrl     = "";
	protected String       _feedbackDateStr = "";
	protected java.util.Date _feedbackDate  = null;
	
	private static boolean _initialized = false;
	
	private static int     _checkId = -1;

//	static 
//	{
//		_logger.setLevel(Level.DEBUG);
//	}

	/*---------------------------------------------------
	** BEGIN: constructors
	**---------------------------------------------------
	*/
	public CheckForUpdates()
	{
	}
//	public CheckForUpdates(URL url)
//	{
//		if (!url.getProtocol().toLowerCase().startsWith("http"))
//		{
//			throw new IllegalArgumentException("Posting only works for http URLs");
//		}
//		this.url = url;
//	}
	/*---------------------------------------------------
	** END: constructors
	**---------------------------------------------------
	*/

	
	/*---------------------------------------------------
	** BEGIN: private helper methods
	**---------------------------------------------------
	*/
	private void add(String name, String value)
	{
		if (value == null)
			value = "";
		
		// replace all '\', with '/', which makes some fields more readable.
		value = value.replace('\\', '/');

		_query.add(name, value);
	}

	private InputStream post() throws IOException
	{
		return post(3*1000); // 3 seconds
	}
	private InputStream post(int timeoutInMs) throws IOException
	{
		// In JDK 5, we can make the JDK go and get if we use proxies
		// for specific URL:s, just set the below property...
		//		System.setProperty("java.net.useSystemProxies", "true");
		// The above is done in method init()
		// The init() method also creates our own ProxySelector, which only
		// calls the DefaultProxySelector in case the protocoll is 'http/https'
		//
		// This because it would try to use Proxy for the protcol 'socket'
		// aswell and then it would probably hang when we do a connect() via
		// JDBC... and that we dont want...

		// open the connection and prepare it to POST
		URLConnection conn = _url.openConnection();
		conn.setConnectTimeout(timeoutInMs); // 3 seconds
		conn.setDoOutput(true);
		OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), "ASCII");

		// The POST line, the Content-type header,
		// and the Content-length headers are sent by the URLConnection.
		// We just need to send the data
		if (_logger.isDebugEnabled())
			_logger.debug("post() query: "+_query.toString());
		out.write(_query.toString());
		out.write("\r\n");
		out.flush();
		out.close();

		// Return the response
		return conn.getInputStream();
	}

	/*---------------------------------------------------
	** END: private helper methods
	**---------------------------------------------------
	*/

	
	
	/*---------------------------------------------------
	** BEGIN: public methods
	**---------------------------------------------------
	*/
	/**
	 * Initialize the class<br>
	 * This might have to be done before using any other connect() method,
	 * If the <code>DefaultProxySelector</code> is initialized by someone else
	 * and the system proerty <code>java.net.useSystemProxies</code> is not TRUE
	 * then it wont use System Proxy Settings.
	 */
	static
	{
//		_logger.setLevel(Level.TRACE);
		init();
	}

	public static void init()
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

	
	/**
	 * Check for a later version of the software
	 * @param owner A JFrame or similar
	 * @param showNoUpgrade Even if no upgrade is available, show info about that in a GUI message.
	 */
	public static void noBlockCheck(final Component owner, final boolean showNoUpgrade, final boolean showFailure)
	{
		Runnable checkLater = new Runnable() 
		{
			public void run()
			{
				CheckForUpdates chk = new CheckForUpdates();

				// Go and check
				chk.check();

				if (! chk.checkSucceed())
				{
					if (owner != null && showFailure)
						CheckDialog.showDialog(owner, chk);

					_logger.info("Check for latest version failed.");
					return;
				}

				if (chk.hasUpgrade())
				{
					if (owner != null)
						CheckDialog.showDialog(owner, chk);
					
					_logger.info("New Upgrade is Available. New version is '"+chk.getNewAppVersionStr()+"' " +
							"and can be downloaded here '"+chk.getDownloadUrl()+"'.");
				}
				else
				{
					if (showNoUpgrade)
					{
						if (owner != null)
							CheckDialog.showDialog(owner, chk);
					}

					if (chk.hasFeedback())
					{
						if (owner != null)
							CheckDialog.showDialog(owner, chk);
					}

					_logger.info("You have got the latest release of '"+Version.getAppName()+"'.");
				}
			}
		};
		Thread checkThread = new Thread(checkLater);
		checkThread.setName("checkForUpdates");
		checkThread.setDaemon(true);
		checkThread.start();
	}

	/**
	 * Go and check for updates
	 */
	public void check()
	{
		_hasUpgrade   = false;
		_checkSucceed = false;

		try
		{
			_url = new URL(ASETUNE_CHECK_UPDATE_URL);
		}
		catch (MalformedURLException ex)
		{ 
			// shouldn't happen
			_logger.debug("When we checking for later version, we had problems", ex);
			return;
		}

//		add("name", "Goran Schwarz");
//		add("email", "goran_schwarz@hotmail.com");

		Date timeNow = new Date(System.currentTimeMillis());
		String clientTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);

		if (_logger.isDebugEnabled())
			add("debug",    "true");
			
		add("clientCheckTime",     clientTime);

		add("clientSourceDate",     Version.getSourceDate());
		add("clientSourceVersion",  Version.getSourceRev());
		add("clientAseTuneVersion", Version.getVersionStr());

		try 
		{
			InetAddress addr = InetAddress.getLocalHost();
			
			add("clientHostName",          addr.getHostName());
			add("clientHostAddress",       addr.getHostAddress());
			add("clientCanonicalHostName", addr.getCanonicalHostName());

		}
		catch (UnknownHostException e) 
		{
		}

		add("user_name",          System.getProperty("user.name"));
		add("user_dir",           System.getProperty("user.dir"));
		add("propfile",           Configuration.getInstance(Configuration.SYSTEM_CONF).getFilename());
		add("userpropfile",       Configuration.getInstance(Configuration.USER_TEMP).getFilename());
		add("gui",                AseTune.hasGUI()+"");

		add("java_version",       System.getProperty("java.version"));
		add("java_vm_version",    System.getProperty("java.vm.version"));
		add("java_vm_vendor",     System.getProperty("java.vm.vendor"));
		add("java_home",          System.getProperty("java.home"));
		add("java_class_path",    System.getProperty("java.class.path"));
		add("memory",             Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB");
		add("os_name",            System.getProperty("os.name"));
		add("os_version",         System.getProperty("os.version"));
		add("os_arch",            System.getProperty("os.arch"));
		add("sun_desktop",        System.getProperty("sun.desktop"));
		add("user_country",       System.getProperty("user.country"));
		add("user_language",      System.getProperty("user.language"));
		add("user_timezone",      System.getProperty("user.timezone"));
		add("-end-",              "-end-");

		try
		{
			// SEND OFF THE REQUEST
			InputStream in = post();

			//----------------------------------------------
			// This is how a response would look like
			//----------------------------------------------
			// ACTION:UPGRADE:$ASETUNE_LATEST_VERSION_STR:$DOWNLOAD_URL"
			// ACTION:NO-UPGRADE
			//----------------------------------------------

			_action         = "";
			_asetuneVersion = "";
			_downloadUrl    = DEFAULT_DOWNLOAD_URL;
			_whatsNewUrl    = DEFAULT_WHATSNEW_URL;

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

						if (i == 1) _action         = sa[1];
						if (i == 2) _asetuneVersion = sa[2];
						if (i == 3) _downloadUrl    = sa[3];
						if (i == 4) _whatsNewUrl    = sa[4];
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
							else if (key.equalsIgnoreCase("sendCounterUsageInfo"))
							{
								_sendUdcInfo = bVal;
								_logger.debug("Setting option '"+key+"' to '"+bVal+"'.");
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
				_logger.debug("-to:"+_asetuneVersion);
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
				_logger.warn("When checking for new version, no 'ACTION:' response was found. The responce rows was '" + responseLines + "'.");
			}
				
			_checkSucceed = true;
		}
		catch (IOException ex)
		{
			_logger.debug("When we checking for later version, we had problems", ex);
		}
	}

	/**
	 * Did the check succeed
	 */
	public boolean checkSucceed()
	{ 
		return _checkSucceed; 
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
		return _asetuneVersion; 
	}

	/**
	 * If there is an upgrade available, where do we download it
	 */
	public String  getDownloadUrl()
	{
		if (_downloadUrl == null || _downloadUrl.trim().equals(""))
			return DEFAULT_DOWNLOAD_URL;
		return _downloadUrl; 
	}

	/**
	 * @return
	 */
	public String getWhatsNewUrl()
	{
		if (_whatsNewUrl == null || _whatsNewUrl.trim().equals(""))
			return DEFAULT_WHATSNEW_URL;
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
	public URL getURL()
	{
		return _url;
	}

	
	/**
	 */
	public static void sendConnectInfoNoBlock()
	{
		if ( ! _sendConnectInfo )
		{
			_logger.debug("Send 'Connect info' has been disabled.");
			return;
		}

		Runnable doLater = new Runnable() 
		{
			public void run()
			{
				CheckForUpdates connInfo = new CheckForUpdates();
				connInfo.sendConnectInfo();

				CheckForUpdates udcInfo = new CheckForUpdates();
				udcInfo.sendUdcInfo();
			}
		};
		Thread checkThread = new Thread(doLater);
		checkThread.setName("sendConnectInfo");
		checkThread.setDaemon(true);
		checkThread.start();
	}

	/**
	 * Send info on connection
	 */
	public void sendConnectInfo()
	{
		if ( ! _sendConnectInfo )
		{
			_logger.debug("Send 'Connect info' has been disabled.");
			return;
		}

		if (_checkId < 0)
		{
			_logger.debug("No checkId was disovered when trying to send connection info, skipping this.");
			return;
		}

		MonTablesDictionary mtd = MonTablesDictionary.getInstance();
		if (mtd == null)
		{
			_logger.debug("MonTablesDictionary was null when trying to send connection info, skipping this.");
			return;
		}

		try
		{
			_url = new URL(ASETUNE_CONNECT_INFO_URL);
		}
		catch (MalformedURLException ex)
		{ 
			// shouldn't happen
			_logger.debug("When sending connection info, we had problems", ex);
			return;
		}

		Date timeNow = new Date(System.currentTimeMillis());

		String checkId          = _checkId + "";
		String clientTime       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);

		String srvVersion       = mtd.aseVersionNum + "";
		String isClusterEnabled = mtd.isClusterEnabled + "";

		String srvName          = AseConnectionFactory.getServer();
		String srvIpPort        = AseConnectionFactory.getHostPortStr();
		String srvUser          = AseConnectionFactory.getUser();
		String srvVersionStr    = mtd.aseVersionStr;

		if (srvName       != null) srvName.trim();
		if (srvIpPort     != null) srvIpPort.trim();
		if (srvUser       != null) srvUser.trim();
		if (srvVersionStr != null) srvVersionStr.trim();
		
		if (_logger.isDebugEnabled())
			add("debug",    "true");
			
		add("checkId",             checkId);
		add("clientTime",          clientTime);
		add("userName",            System.getProperty("user.name"));

		add("srvVersion",          srvVersion);
		add("isClusterEnabled",    isClusterEnabled);

		add("srvName",             srvName);
		add("srvIpPort",           srvIpPort);
		add("srvUser",             srvUser);
		add("srvVersionStr",       srvVersionStr);

		try
		{
			// SEND OFF THE REQUEST
			InputStream in = post();

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
			}
			in.close();

//			_checkSucceed = true;
		}
		catch (IOException ex)
		{
			_logger.debug("when trying to send connection info, we had problems", ex);
		}
	}

	
	
	

	
	/**
	 */
	public static void sendUdcInfoNoBlock()
	{
		if ( ! _sendUdcInfo )
		{
			_logger.debug("Send 'UDC info' has been disabled.");
			return;
		}

		Runnable doLater = new Runnable() 
		{
			public void run()
			{
				CheckForUpdates chk = new CheckForUpdates();

				// Go and check
				chk.sendUdcInfo();
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
		if ( ! _sendUdcInfo )
		{
			_logger.debug("Send 'UDC info' has been disabled.");
			return;
		}

		if (_checkId < 0)
		{
			_logger.debug("No checkId was disovered when trying to send UDC info, skipping this.");
			return;
		}

//		Configuration conf = Configuration.getInstance(Configuration.CONF);
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf == null)
		{
			_logger.debug("Configuration was null when trying to send UDC info, skipping this.");
			return;
		}

		try
		{
			_url = new URL(ASETUNE_UDC_INFO_URL);
		}
		catch (MalformedURLException ex)
		{ 
			// shouldn't happen
			_logger.debug("When sending UDC info, we had problems", ex);
			return;
		}

		Date timeNow = new Date(System.currentTimeMillis());

		String checkId          = _checkId + "";
		String clientTime       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);

		if (_logger.isDebugEnabled())
			add("debug",    "true");

		add("checkId",             checkId);
		add("clientTime",          clientTime);
		add("userName",            System.getProperty("user.name"));

		Iterator<Object> it = conf.keySet().iterator();
		int udcRows = 0;
		while (it.hasNext()) 
		{
			String key = (String)it.next();
			String val = conf.getPropertyRaw(key);

			if (key.startsWith("udc.") || key.startsWith("hostmon.udc."))
			{
				add(key, val);
				udcRows++;
			}
		}

		// If NO UDC rows where found, no need to continue
		if (udcRows == 0)
		{
			_logger.debug("No 'udc.*' or 'hostmon.udc.*' was was found in Configuration, skipping this.");
			return;
		}

		try
		{
			// SEND OFF THE REQUEST
			InputStream in = post();

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

//			_checkSucceed = true;
		}
		catch (IOException ex)
		{
			_logger.debug("when trying to send UDC info, we had problems", ex);
		}
	}

	
	
	

	
	/**
	 */
	public static void sendCounterUsageInfoNoBlock()
	{
		if ( ! _sendCounterUsageInfo )
		{
			_logger.debug("Send 'Counter Usage Info' has been disabled.");
			return;
		}

// Hmmm... skip the noblock thread... since this is done just before the JVM exits.
//		Runnable doLater = new Runnable() 
//		{
//			public void run()
//			{
				CheckForUpdates chk = new CheckForUpdates();

				// Go and check
				chk.sendCounterUsageInfo();
//			}
//		};
//		Thread checkThread = new Thread(doLater);
//		checkThread.setName("sendCounterUsageInfo");
////		checkThread.setDaemon(true);
//		checkThread.start();
	}

	/**
	 * Send info on User Defined Counters
	 */
	public void sendCounterUsageInfo()
	{
		if ( ! _sendCounterUsageInfo )
		{
			_logger.debug("Send 'Counter Usage Info' has been disabled.");
			return;
		}

		if (_checkId < 0)
		{
			_logger.debug("No checkId was disovered when trying to send 'Counter Usage' info, skipping this.");
			return;
		}

//		Configuration conf = Configuration.getInstance(Configuration.CONF);
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf == null)
		{
			_logger.debug("Configuration was null when trying to send 'Counter Usage' info, skipping this.");
			return;
		}

		try
		{
			_url = new URL(ASETUNE_COUNTER_USAGE_INFO_URL);
		}
		catch (MalformedURLException ex)
		{ 
			// shouldn't happen
			_logger.debug("When sending 'Counter Usage' info, we had problems", ex);
			return;
		}

		Date timeNow = new Date(System.currentTimeMillis());

		String checkId          = _checkId + "";
		String clientTime       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);

		if (_logger.isDebugEnabled())
			add("debug",    "true");

		add("checkId",             checkId);
		add("clientTime",          clientTime);
		add("userName",            System.getProperty("user.name"));

		int rows = 0;
		for (CountersModel cm : GetCounters.getCmList())
		{
			int minRefresh = 5;
			if (_logger.isDebugEnabled())
				minRefresh = 1;
			if (cm.getRefreshCounter() >= minRefresh)
			{
				add(cm.getName(), cm.getRefreshCounter()+","+cm.getSumRowCount());
				rows++;
			}
		}

		// If NO UDC rows where found, no need to continue
		if (rows == 0)
		{
			_logger.debug("No 'Counter Usage' was reported, skipping this.");
			return;
		}

		try
		{
			// SEND OFF THE REQUEST
			InputStream in = post(1500); // timeout of 1.5 seconds

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

//			_checkSucceed = true;
		}
		catch (IOException ex)
		{
			_logger.debug("when trying to send 'Counter Usage' info, we had problems", ex);
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

//					System.out.println("PROXY-VOLE.log(logLevel="+logLevel+"): "+msg);
					if      (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.TRACE)  ) _logger.trace(msg);
					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.DEBUG)  ) _logger.debug(msg);
					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.INFO)   ) _logger.info(msg);
					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.WARNING)) _logger.warn(msg);
					else if (logLevel.equals(com.btr.proxy.util.Logger.LogLevel.ERROR)  ) _logger.error(msg);
					else _logger.info("Unhandled loglevel("+logLevel+"): " + msg);
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


	
	private class QueryString
	{

		private StringBuffer	query	= new StringBuffer();

		public QueryString()
		{
		}
		@SuppressWarnings("unused")
		public QueryString(String name, String value)
		{
			encode(name, value);
		}

		public synchronized void add(String name, String value)
		{
			query.append('&');
			encode(name, value);
		}

		private synchronized void encode(String name, String value)
		{
			try
			{
				query.append(URLEncoder.encode(name, "UTF-8"));
				query.append('=');
				query.append(URLEncoder.encode(value, "UTF-8"));
			}
			catch (UnsupportedEncodingException ex)
			{
				throw new RuntimeException("Broken VM does not support UTF-8");
			}
		}

		public String getQuery()
		{
			return query.toString();
		}

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
	public static void main(String args[])
	{
		Properties log4jProps = new Properties();
		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		Configuration conf1 = new Configuration("c:\\projects\\asetune\\asetune.save.properties");
		Configuration.setInstance(Configuration.USER_TEMP, conf1);

		Configuration conf2 = new Configuration("c:\\projects\\asetune\\asetune.properties");
		Configuration.setInstance(Configuration.SYSTEM_CONF, conf2);

		System.setProperty("java.net.useSystemProxies", "true");

//		System.setProperty("http.proxyHost", "www-proxy.dummy.se");
//		System.setProperty("http.proxyPort", "8080");

		CheckForUpdates check = new CheckForUpdates();
		
		System.out.println("-- DO CHECK");
		check.check();
		if ( check.checkSucceed() )
		{
			if (check.hasUpgrade())
			{
				System.out.println("-- HAS NEW UPGRADE");
				System.out.println("-- new version '"+check.getNewAppVersionStr()+"'.");
				System.out.println("-- download url '"+check.getDownloadUrl()+"'.");
			}
			else
			{
				System.out.println("-- NO NEED TO UPGRADE");
			}
			System.out.println("-- FEEDBACK_URL: '"+check.getFeedbackUrl()+"'.");
		}
		else
		{
			System.out.println("-- CHECK FAILED...");
		}

	
		CheckForUpdates udcCheck = new CheckForUpdates();
		udcCheck.sendUdcInfo();
	}
}


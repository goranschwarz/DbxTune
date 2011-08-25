package asemon.check;

import java.awt.Component;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import asemon.Asemon;
import asemon.Version;
import asemon.utils.Configuration;
import asemon.utils.SwingUtils;

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
 * does NOT work with PAC files it just checks for ststic proxy settings<br>
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

	private static final String ASEMON_UPDATE_URL = "http://gorans.no-ip.org/asemon/check_for_update.php";

	private URL	           _url;
	private QueryString	   _query	       = new QueryString();
	private String         _action        = "";
	
	// The below is protected, just because test purposes, it should be private
	protected String       _asemonVersion = "";
	protected String       _downloadUrl   = "";
	protected boolean      _hasUpgrade;
	protected boolean      _checkSucceed;

	private static boolean _initialized = false;

	
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

		_query.add(name, value);
	}

	private InputStream post() throws IOException
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
		conn.setConnectTimeout(3*1000); // 3 seconds
		conn.setDoOutput(true);
		OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), "ASCII");

		// The POST line, the Content-type header,
		// and the Content-length headers are sent by the URLConnection.
		// We just need to send the data
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
					
					_logger.info("New Upgrade is Available. New version is '"+chk.getNewAsemonVersionStr()+"' " +
							"and can be downloaded here '"+chk.getDownloadUrl()+"'.");
				}
				else
				{
					if (showNoUpgrade)
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
			_url = new URL(ASEMON_UPDATE_URL);
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

		add("clientSourceDate",    Version.getSourceDate());
		add("clientSourceVersion", Version.getSourceRev());
		add("clientAsemonVersion", Version.getVersionStr());

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
		add("propfile",           Configuration.getInstance(Configuration.CONF).getFilename());
		add("gui",                Asemon.hasGUI()+"");

		add("java_version",       System.getProperty("java.version"));
		add("java_vm_version",    System.getProperty("java.vm.version"));
		add("java_vm_vendor",     System.getProperty("java.vm.vendor"));
		add("java_home",          System.getProperty("java.home"));
		add("java_class_path",    System.getProperty("java.class.path"));
		add("memory",             Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB");
		add("os_name",            System.getProperty("os.name"));
		add("os_version",         System.getProperty("os.version"));
		add("os_arch",            System.getProperty("os.arch"));
		add("-end-",              "-end-");

		try
		{
			// SEND OFF THE REQUEST
			InputStream in = post();

			//----------------------------------------------
			// This is how a response would look like
			//----------------------------------------------
			// ACTION:UPGRADE:$ASEMON_LATEST_VERSION_STR:$DOWNLOAD_URL"
			// ACTION:NO-UPGRADE
			//----------------------------------------------

			_action        = "";
			_asemonVersion = "";
			_downloadUrl   = "";

			LineNumberReader lr = new LineNumberReader(new InputStreamReader(in));
			String line;
			while ((line = lr.readLine()) != null)
			{
				_logger.debug("response line "+lr.getLineNumber()+": " + line);
				if (line.startsWith("ACTION:"))
				{
					String[] sa = line.split(":");
					for (int i=0; i<sa.length; i++)
					{
						_logger.debug("   - STRING["+i+"]='"+sa[i]+"'.");

						if (i == 1) _action        = sa[1];
						if (i == 2) _asemonVersion = sa[2];
						if (i == 3) _downloadUrl   = sa[3];
					}
				}
			}
			in.close();

			if (_action.equals("UPGRADE"))
			{
				_hasUpgrade = true;
				_logger.debug("-UPGRADE-");
				_logger.debug("-to:"+_asemonVersion);
				_logger.debug("-at:"+_downloadUrl);
			}
			else
			{
				_hasUpgrade = false;
				_logger.debug("-NO-UPGRADE-");
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
	public String  getNewAsemonVersionStr() 
	{ 
		return _asemonVersion; 
	}

	/**
	 * If there is an upgrade available, where do we download it
	 */
	public String  getDownloadUrl()
	{
		return _downloadUrl; 
	}

	/**
	 * what URL did we check at?
	 * @return
	 */
	public URL getURL()
	{
		return _url;
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
		public OnlyHttpProxySelector(ProxySelector def) 
		{
			if (def == null) 
			{
				throw new IllegalArgumentException("ProxySelector can't be null.");
			}
			_defsel = def;
			_logger.debug("Installing a new ProxySelector, but I will save and use the default '"+_defsel.getClass().getName()+"'.");
		}

		public void connectFailed(URI uri, SocketAddress sa, IOException ioe)
		{
			// skip this
		}

		public List select(URI uri)
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

				List proxyList = new ArrayList();
				proxyList.add(Proxy.NO_PROXY);
				return proxyList;
			}
			else
			{
				_logger.debug("Trying to resolv a proxy server for the protocoll '"+protocol+"', using the ProxySelector '"+_defsel.getClass().getName()+"', with uri='"+uri+"'.)");

				List proxyList = _defsel.select(uri);

				for (int i=0; i<proxyList.size(); i++)
					_logger.debug("ProxyList["+i+"]: "+proxyList.get(i));

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

		Configuration conf1 = new Configuration("c:\\projects\\asemon\\asemon.save.properties");
		Configuration.setInstance(Configuration.TEMP, conf1);

		Configuration conf2 = new Configuration("c:\\projects\\asemon\\asemon.properties");
		Configuration.setInstance(Configuration.CONF, conf2);

		System.setProperty("java.net.useSystemProxies", "true");

		CheckForUpdates check = new CheckForUpdates();
		
		check.check();
		if ( check.checkSucceed() )
		{
			if (check.hasUpgrade())
			{
				System.out.println("-- HAS NEW UPGRADE");
				System.out.println("-- new version '"+check.getNewAsemonVersionStr()+"'.");
				System.out.println("-- download url '"+check.getDownloadUrl()+"'.");
			}
			else
			{
				System.out.println("-- NO NEED TO UPGRADE");
			}
		}
		else
		{
			System.out.println("-- CHECK FAILED...");
		}
	}
}


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
package com.dbxtune.sql;

import java.net.URI;
import java.text.ParseException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.dbxtune.utils.AseUrlHelper;
import com.dbxtune.utils.H2UrlHelper;
import com.dbxtune.utils.StringUtil;

public class JdbcUrlParser
{
	private static Logger _logger = Logger.getLogger(JdbcUrlParser.class);

//	protected String _dbName = null;
	protected String _dbType = null;
	protected String _host   = null;
	protected int    _port   = -1;
	protected String _hostPortStr = null;

	protected String _path = null;
	protected String _optionsStr = null;
	protected String _hostPrefix = "";

	/** Returns the database name. */
//	public String getDbName()              { return _dbName; }
//	public void   setDbName(String dbname) { _dbName = dbname; }

	/** Returns the database type (actually the JDBC subprotocol, first "word" after jdbc:). */
	public String getDbType()            { return _dbType; }
	public void   setDbType(String type) { _dbType = type; }

	/** Returns the host name. */
	public String getHost()            { return _host; }
	public void   setHost(String host) { _host = host; }

	/** Returns the network port. */
	public int  getPort()         { return _port; }
	public void setPort(int port) { _port = port; }
	
	/** Returns the host and port number in the format host:port */
	public void   setHostPortStr(String str) { _hostPortStr = str; }
	public String getHostPortStr()           
	{
		if (StringUtil.hasValue(_hostPortStr))
			return _hostPortStr;
		
		if (StringUtil.hasValue(getHost()))
		{
			if (getPort() != -1)
				return getHost() + ":" + getPort();
			else
				return getHost();
		}

		return null;
	}

	/** Returns the "path". */
	public String getPath()            { return _path; }
	public void   setPath(String path) { _path = path; }

	/** Returns the "path". */
	public String getOptions()           { return _optionsStr; }
	public void   setOptions(String opt) { _optionsStr = opt; }

	/** Returns the "path". */
	public String getHostPrefix()           { return _hostPrefix; }
	public void   setHostPrefix(String str) { _hostPrefix = str; }

	/** trying to construct an URL again */
	public String toUrl()
	{
		// FIXME: THIS IS TO SIMPLIFIED...
		String hostPrefix = getHostPrefix() == null ? "" : getHostPrefix();
		String path       = getPath()       == null ? "" : getPath();
		String options    = getOptions()    == null ? "" : getOptions();
		
		if (StringUtil.hasValue(options))
			options = "?" + options;
		
		return "jdbc:" + getDbType() + ":" + hostPrefix + getHostPortStr() + path + options;
	}
	

	public static JdbcUrlParser parse(String url)
	{
		if (StringUtil.isNullOrBlank(url))
			throw new IllegalArgumentException("URL can't be null or blank");
		
		JdbcUrlParser p = new JdbcUrlParser();

		if (url.startsWith("jdbc:sybase:Tds:"))
		{
			try
			{
				// FIXME: make AseUrlHelper a subclass of JdbcUrlParser
				AseUrlHelper aseUrl = AseUrlHelper.parseUrl(url);
				
				p.setDbType("sybase");
	    		p.setHost  (aseUrl.getFirstHost());
	    		p.setPort  (aseUrl.getFirstPort());
	    		p.setHostPortStr(aseUrl.getHostPortStr());
	    		
	    		p.setPath   (aseUrl.getDbname());
	    		p.setOptions(aseUrl.getOptions("&"));
			}
			catch (ParseException ex)
			{
				_logger.warn("Problem parsing the SYBASE URL '"+url+"'. Caught: "+ex);
			}
		}
		else if (url.startsWith("jdbc:h2:"))
		{
			try
			{
				// FIXME: make H2UrlHelper a subclass of JdbcUrlParser
				H2UrlHelper h2Url = new H2UrlHelper(url);
				
				p.setDbType("h2");
	    		p.setHost  (h2Url.getUrlTcpHost());
	    		p.setPort  (h2Url.getUrlTcpPort());
	    		p.setHostPortStr(h2Url.getUrlTcpHostPort());

	    		p.setPath   (h2Url.getFilename());
	    		p.setOptions(h2Url.getUrlOptions());
			}
			catch (Exception ex)
			{
				_logger.warn("Problem parsing the H2 URL '"+url+"'. Caught: "+ex);
			}
		}
		else if (url.startsWith("jdbc:oracle:thin:"))
		{
			p = new OracleUrlParser(url);
		}
		else
		{
//			if (url.startsWith("jdbc:sqlserver://"))
//				url = url.replace("jdbc:sqlserver://", "jdbc:sqlserver:/");
//			if (url.startsWith("jdbc:sqlserver://"))
//				url = url.replace("jdbc:sqlserver://", "jdbc:sqlserver:");

//			String cleanURI = url;
			String cleanURI = url.replace('\\', '/');
//System.out.println("cleanURI='"+cleanURI+"'.");
			try
			{
				if (url.startsWith("jdbc:"))
					cleanURI = cleanURI.substring("jdbc:".length());
				
				// URI.create() do not like 'sqlserver://' so stripping out all '://'
//				if (cleanURI.indexOf("//") >= 0)
//					cleanURI = cleanURI.replace("://", ":");
				
				URI uri = URI.create(cleanURI);

				if (_logger.isDebugEnabled())
					_logger.debug("JdbcUrlParser.parse(url='"+url+"'): cleanURI='"+cleanURI+"', uri.getScheme()='"+uri.getScheme()+"', uri.getHost()='"+uri.getHost()+"', uri.getPort()='"+uri.getPort()+"'.");
				
				if (cleanURI.indexOf("//"+uri.getHost()) >= 0)
					p.setHostPrefix("//");
				
				p.setDbType     (uri.getScheme());
				p.setHost       (uri.getHost());
				p.setPort       (uri.getPort());
				p.setHostPortStr( null ); // getHostPortStr(): if _hostPortStr==null -> getHost() + ":" + getPort()

				p.setPath   (uri.getPath());
	    		p.setOptions(uri.getQuery());
			}
			catch (Throwable ex)
			{
				_logger.warn("Problem parsing the GENERIC URL '"+url+"', cleanURI '"+cleanURI+"'. Caught: "+ex);
			}
		}

		return p;
		
	}

//	https://stackoverflow.com/questions/9287052/how-to-parse-a-jdbc-url-to-get-hostname-port-etc
//	/**
//	 * Split di una url JDBC nei componenti. Estrae i componenti di una uri JDBC
//	 * del tipo: <br>
//	 * String url = "jdbc:derby://localhost:1527/netld;collation=TERRITORY_BASED:PRIMARY";
//	 * <br>
//	 * nelle rispettive variabili pubbliche.
//	 * 
//	 * @author Nicola De Nisco
//	 */
//	public class JdbcUrlSplitter
//	{
//		public String driverName, host, port, database, params;
//
//		public JdbcUrlSplitter(String jdbcUrl)
//		{
//			int pos, pos1, pos2;
//			String connUri;
//
//			if ( jdbcUrl == null || !jdbcUrl.startsWith("jdbc:") || (pos1 = jdbcUrl.indexOf(':', 5)) == -1 )
//				throw new IllegalArgumentException("Invalid JDBC url.");
//
//			driverName = jdbcUrl.substring(5, pos1);
//			if ( (pos2 = jdbcUrl.indexOf(';', pos1)) == -1 )
//			{
//				connUri = jdbcUrl.substring(pos1 + 1);
//			}
//			else
//			{
//				connUri = jdbcUrl.substring(pos1 + 1, pos2);
//				params = jdbcUrl.substring(pos2 + 1);
//			}
//
//			if ( connUri.startsWith("//") )
//			{
//				if ( (pos = connUri.indexOf('/', 2)) != -1 )
//				{
//					host = connUri.substring(2, pos);
//					database = connUri.substring(pos + 1);
//
//					if ( (pos = host.indexOf(':')) != -1 )
//					{
//						port = host.substring(pos + 1);
//						host = host.substring(0, pos);
//					}
//				}
//			}
//			else
//			{
//				database = connUri;
//			}
//		}
//	}	
	
	
	@Override
	public String toString()
	{
		return super.toString() + ": dbType='"+getDbType()+"', host='"+getHost()+"', port="+getPort()+", hostPortStr='"+getHostPortStr()+"', path='"+getPath()+"', options='"+getOptions()+"'.";
	}
	
	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		//log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		test("jdbc:derby://localhost:1527/netld;collation=TERRITORY_BASED:PRIMARY");
		test("jdbc:sybase:Tds:vadbjsa000.ash.od.sap.biz:30015?ENCRYPT_PASSWORD=true");
		test("jdbc:sapdb://vadbj00.od.sap.biz/J00");
		test("jdbc:oracle:thin:@//nlhtblob001.htb.sap.corp:1521/ORA11DEV");
		test("jdbc:sqlserver://mo-58db799f7.mo.sap.corp:1433");
		test("jdbc:sqlserver://gorans-ub2:1433");
		test("jdbc:sap://mo-b402c54f9.mo.sap.corp:30015");
		test("jdbc:h2:file:C:/projects/asetune_recordings_temp/spam_prod_b_2014-09-23.15;IFEXISTS=TRUE;DATABASE_TO_UPPER=false;AUTO_SERVER=TRUE");
		test("jdbc:h2:tcp://192.168.0.112/spam_prod_b_2014-09-23.15;IFEXISTS=TRUE;DATABASE_TO_UPPER=false;AUTO_SERVER=TRUE");
		test("jdbc:h2:tcp://192.168.0.111:9092/spam_prod_b_2014-09-23.15;IFEXISTS=TRUE;DATABASE_TO_UPPER=false;AUTO_SERVER=TRUE");
		test("jdbc:sybase:Tds:localhost:15702,STON60266746A:15702?IS_CLOSED_TEST=INTERNAL&ENCRYPT_PASSWORD=true");
		test("jdbc:sybase:Tds:localhost:15702/goransdb?IS_CLOSED_TEST=INTERNAL&ENCRYPT_PASSWORD=true");
		test("jdbc:oracle:thin:@//nlhtblob001.htb.sap.corp:1521/ora11dev");
		test("jdbc:oracle:thin:Herong/TopSecret@h1:9999:XE");
		test("jdbc:oracle:thin:Herong/TopSecret@:8888:XE");
		test("jdbc:oracle:thin:Herong/TopSecret@//h1:7777/XE");
		test("jdbc:oracle:thin:Herong/TopSecret@//:6666/XE");
		test("jdbc:oracle:thin:Herong/TopSecret@//h1/XE");
		test("jdbc:oracle:thin:Herong/TopSecret@///XE");
		test("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=h1)(PORT=5555)) (CONNECT_DATA=(SERVICE_NAME= service_name)))");
		test("jdbc:oracle:thin:@(DESCRIPTION=(LOAD_BALANCE=on) (ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=h1)(PORT=1111))(ADDRESS=(PROTOCOL=TCP)(HOST=h2)(PORT=2222))) (CONNECT_DATA=(SERVICE_NAME= service_name)))");
		test("jdbc:postgresql:database");
		test("dbc:postgresql:/");
		test("jdbc:postgresql://host/database");
		test("jdbc:postgresql://host/");
		test("jdbc:postgresql://host:1234/database");
		test("jdbc:postgresql://host:1234/");
		test("jdbc:postgresql://host:1234/database?ssl=true");
		test("jdbc:postgresql://host:1234/database?ssl=true&user=fred&password=secret");
		
		test2("jdbc:postgresql://host:1234/aDbName");
		test2("jdbc:postgresql://host:1234/database?ssl=true&user=fred&password=secret");
		
	}
	
	private static void test(String url)
	{
		System.out.println("");
		System.out.println("########################################################");
		System.out.println(" URL="+url);
		System.out.println(" toString="+JdbcUrlParser.parse(url));
	}

	private static void test2(String url)
	{
		System.out.println("");
		System.out.println("## test2 ######################################################");
		System.out.println("                 URL="+url);
		JdbcUrlParser p = JdbcUrlParser.parse(url);
		p.setPath("/YYY");
		System.out.println(" changed path. toUrl="+p.toUrl());
	}
	

	/**
	 * Try to parse some of the Oracles JDBC URL
	 * <pre>
	 * jdbc:oracle:thin:[user/password]@[host][:port]:SID
	 * jdbc:oracle:thin:[user/password]@//[host][:port]/SID
	 * 
	 *   user     - The login user name defined in the Oracle server.
	 *   password - The password for the login user.
	 *   host     - The host name where Oracle server is running.             Default is 127.0.0.1 - the IP address of localhost.
	 *   port     - The port number where Oracle is listening for connection. Default is 1521.
	 *   SID      - System ID of the Oracle server database instance. 
	 *              SID is a required value. By default, Oracle Database 10g Express Edition creates one database instance called XE.
	 * Some examples:
	 *   jdbc:oracle:thin:Herong/TopSecret@localhost:1521:XE
	 *   jdbc:oracle:thin:Herong/TopSecret@:1521:XE
	 * 
	 *   jdbc:oracle:thin:Herong/TopSecret@//localhost:1521/XE
	 *   jdbc:oracle:thin:Herong/TopSecret@//:1521/XE
	 *   jdbc:oracle:thin:Herong/TopSecret@//localhost/XE
	 *   jdbc:oracle:thin:Herong/TopSecret@///XE
	 * 
	 *   jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=localhost)(PORT=1521)) (CONNECT_DATA=(SERVICE_NAME= service_name)))
	 *   jdbc:oracle:thin:@(DESCRIPTION=(LOAD_BALANCE=on) (ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=localhost)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=host2)(PORT=1521))) (CONNECT_DATA=(SERVICE_NAME= service_name)))
	 * </pre>
	 */
	public static class OracleUrlParser
	extends JdbcUrlParser
	{
		final String DEFAULT_HOST = "localhost";
		final String DEFAULT_PORT = "1521";

		public OracleUrlParser(String url)
		{
//			String originUrl = url;
			
			if ( ! url.startsWith("jdbc:oracle:thin:") )
				throw new IllegalArgumentException("Oracle JDBC URL must start with 'jdbc:oracle:thin:', the current url '"+url+"' does not.");

			try
			{
				setDbType("oracle");

				// To make it a bit easier, strip off some parts
				url = url.replace("jdbc:oracle:thin:", "");

				// Remove user/passwd, up to the point of first '@'
				int firstAtIndex = url.indexOf('@');
				if (firstAtIndex != -1)
					url = url.substring(firstAtIndex+1);

				// Remove any prefix '//'
				if (url.startsWith("//")) 
					url = url.substring(2);

				url = url.trim();

				String host = "";
				String port = "";

				// if url starts with ( then it's the "long" description... lets skip parsing that for the moment...
				if (url.startsWith("("))
				{
					_logger.info("This looks like a TNS URL, this is not supported for the moment.");
					return;
				}
				else // Short form of the URL
				{
					// first remove the trailing "service_name" if it uses '/'
					int firstSlashIndex = url.indexOf('/');
					if (firstSlashIndex != -1)
						url = url.substring(0, firstSlashIndex);
						
					// Split what's left on ':'
					String[] sa = url.split(":");

					// Get various parts based on how many parts we got split 
					if (sa.length == 3 || sa.length == 2) // host:port:SID || host:port
					{
						host = sa[0];
						port = sa[1];
					}
					else if (sa.length == 1) // host
					{
						host = sa[0];
					}
				}

				if (StringUtil.isNullOrBlank(host)) host = DEFAULT_HOST;
				if (StringUtil.isNullOrBlank(port)) port = DEFAULT_PORT;

				setHost(host);
	    		setPort( Integer.parseInt(port) );
			}
			catch (Throwable ex)
			{
				_logger.warn("Problem parsing the ORACLE URL '"+url+"'. Caught: "+ex);
			}
		}
	}
}

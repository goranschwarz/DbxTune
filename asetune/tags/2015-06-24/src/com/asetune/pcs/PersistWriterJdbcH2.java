/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.pcs;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.asetune.utils.Configuration;


/**
 * -------------------------------------------------------------------
 * -------------------------------------------------------------------
 * -------------------------------------------------------------------
 * NOTE: NOT USED FOT THE MOMENT, BUT LETS CHECK IT IN ANYWAY...
 * -------------------------------------------------------------------
 * -------------------------------------------------------------------
 * -------------------------------------------------------------------
 */
public class PersistWriterJdbcH2 
extends PersistWriterJdbc 
{
    /** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(PersistWriterJdbcH2.class);

	/*---------------------------------------------------
	** DEFINITIONS
	**---------------------------------------------------
	*/

	
	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/
	private boolean _startH2NetworkServer = false;



	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/
	public PersistWriterJdbcH2()
	{
		super._name       = "PersistWriterJdbcH2";
		
		super._jdbcDriver = "org.h2.Driver";
		super._jdbcUrl    = "jdbc:h2:pcdb_yyy";
		super._jdbcUser   = "sa";
		super._jdbcPasswd = "";
		
		_startH2NetworkServer = false;
	}


	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/

	@Override
	public String getName()
	{
		return _name;
	}

	@Override
	public void init(Configuration props) throws Exception
	{
		String propPrefix = "PersistWriterJdbcH2.";
		String propname = null;

		// property: name
		propname = propPrefix+"name";
		_name = props.getProperty(propname, _name);

		// WRITE init message, jupp a little late, but I wanted to grab the _name
		_logger.info("Initializing the PersistentCounterHandler.WriterClass component named '"+_name+"'.");
		
		_jdbcDriver = props.getProperty(propPrefix+"jdbcDriver", "org.h2.Driver");
		_jdbcUrl    = props.getProperty(propPrefix+"jdbcUrl",    "jdbc:h2:pcdb");
		_jdbcUser   = props.getProperty(propPrefix+"jdbcUser",   "sa");
		_jdbcPasswd = props.getProperty(propPrefix+"jdbcPasswd", "");
		if (_jdbcPasswd.equalsIgnoreCase("null"))
			_jdbcPasswd="";

		_startH2NetworkServer = props.getBooleanProperty(propPrefix+"startH2NetworkServer", _startH2NetworkServer);

		String configStr = "jdbcDriver='"+_jdbcDriver+"', jdbcUrl='"+_jdbcUrl+"', jdbcUser='"+_jdbcUser+"', jdbcPasswd='*hidden*'.";
		_logger.info("Configuration for PersistentCounterHandler.WriterClass component named '"+_name+"': "+configStr);

		// Everything could NOT be done with the jdbcUrl... so here goes some special
		// start the H2 TCP Server
		if ( _startH2NetworkServer )
		{
			_logger.info("Starting a H2 TCP server.");
			org.h2.tools.Server h2ServerTcp = org.h2.tools.Server.createTcpServer("-tcpAllowOthers");
			h2ServerTcp.start();

//			_logger.info("H2 TCP server, listening on port='"+h2Server.getPort()+"', url='"+h2Server.getURL()+"', service='"+h2Server.getService()+"'.");
			_logger.info("H2 TCP server, url='"+h2ServerTcp.getURL()+"', service='"+h2ServerTcp.getService()+"'.");

			if (true)
			{
				_logger.info("Starting a H2 WEB server.");
				org.h2.tools.Server h2ServerWeb = org.h2.tools.Server.createWebServer();
				h2ServerWeb.start();

				_logger.info("H2 WEB server, url='"+h2ServerWeb.getURL()+"', service='"+h2ServerWeb.getService()+"'.");
			}
		}

	}

	public static void main(String[] args) 
	{
		// Log4j
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		//log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		// props
		Configuration conf = new Configuration();
		conf.setProperty("PersistWriterJdbcH2.jdbcDriver",           "org.h2.Driver");
		conf.setProperty("PersistWriterJdbcH2.jdbcUrl",              "jdbc:h2:file:C:/projects/asetune/data/xxx");
		conf.setProperty("PersistWriterJdbcH2.jdbcUser",             "sa");
		conf.setProperty("PersistWriterJdbcH2.jdbcPasswd",           "");
		conf.setProperty("PersistWriterJdbcH2.startH2NetworkServer", true);

		try
		{
			PersistWriterJdbcH2 pcs = new PersistWriterJdbcH2();
			pcs.init(conf);

			Object waitForever = new Object();
			waitForever.wait();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
}

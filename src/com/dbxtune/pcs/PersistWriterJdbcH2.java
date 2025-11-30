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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.pcs;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.utils.Configuration;


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
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

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
		throw new Exception("PersistWriterJdbcH2 should not be used...");
//		String propPrefix = "PersistWriterJdbcH2.";
//		String propname = null;
//
//		// property: name
//		propname = propPrefix+"name";
//		_name = props.getProperty(propname, _name);
//
//		// WRITE init message, jupp a little late, but I wanted to grab the _name
//		_logger.info("Initializing the PersistentCounterHandler.WriterClass component named '"+_name+"'.");
//		
//		_jdbcDriver = props.getProperty(propPrefix+"jdbcDriver", "org.h2.Driver");
//		_jdbcUrl    = props.getProperty(propPrefix+"jdbcUrl",    "jdbc:h2:pcdb");
//		_jdbcUser   = props.getProperty(propPrefix+"jdbcUser",   "sa");
//		_jdbcPasswd = props.getProperty(propPrefix+"jdbcPasswd", "");
//		if (_jdbcPasswd.equalsIgnoreCase("null"))
//			_jdbcPasswd="";
//
//		_startH2NetworkServer = props.getBooleanProperty(propPrefix+"startH2NetworkServer", _startH2NetworkServer);
//
//		String configStr = "jdbcDriver='"+_jdbcDriver+"', jdbcUrl='"+_jdbcUrl+"', jdbcUser='"+_jdbcUser+"', jdbcPasswd='*hidden*'.";
//		_logger.info("Configuration for PersistentCounterHandler.WriterClass component named '"+_name+"': "+configStr);
//
//		// Everything could NOT be done with the jdbcUrl... so here goes some special
//		// start the H2 TCP Server
//		if ( _startH2NetworkServer )
//		{
//			_logger.info("Starting a H2 TCP server.");
//			org.h2.tools.Server h2ServerTcp = org.h2.tools.Server.createTcpServer("-tcpAllowOthers");
//			h2ServerTcp.start();
//
////			_logger.info("H2 TCP server, listening on port='"+h2Server.getPort()+"', url='"+h2Server.getURL()+"', service='"+h2Server.getService()+"'.");
//			_logger.info("H2 TCP server, url='"+h2ServerTcp.getURL()+"', service='"+h2ServerTcp.getService()+"'.");
//
//			if (true)
//			{
//				_logger.info("Starting a H2 WEB server.");
//				org.h2.tools.Server h2ServerWeb = org.h2.tools.Server.createWebServer();
//				h2ServerWeb.start();
//
//				_logger.info("H2 WEB server, url='"+h2ServerWeb.getURL()+"', service='"+h2ServerWeb.getService()+"'.");
//			}
//		}

	}

	public static void main(String[] args) 
	{
		// Set Log4j Log Level
//		Configurator.setRootLevel(Level.TRACE);

		// props
		Configuration conf = new Configuration();
		conf.setProperty("PersistWriterJdbcH2.jdbcDriver",           "org.h2.Driver");
		conf.setProperty("PersistWriterJdbcH2.jdbcUrl",              "jdbc:h2:file:C:/projects/dbxtune/data/xxx");
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

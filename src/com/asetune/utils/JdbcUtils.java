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
package com.asetune.utils;

import java.awt.Component;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import com.asetune.Version;

public class JdbcUtils
{
	private static Logger _logger = Logger.getLogger(JdbcUtils.class);

	/**
	 * Connect to a JDBC database
	 * 
	 * @param parentComp Parent GUI component if any, can be null.
	 * @param driver     JDBC driver name
	 * @param url        JDBC url specification
	 * @param user       JDBC user
	 * @param passwd     JDBC password
	 * 
	 * @return a Connection returns null on failure.
	 */
	public static Connection connect(Component parentComp, String driver, String url, String user, String passwd)
	{
		return connect(parentComp, driver, url, user, passwd, null);
	}
	/**
	 * Connect to a JDBC database
	 * 
	 * @param parentComp Parent GUI component if any, can be null.
	 * @param driver     JDBC driver name
	 * @param url        JDBC url specification
	 * @param user       JDBC user
	 * @param passwd     JDBC password
	 * @param appname    Application name, not used by all drivers, can be null or empty
	 * 
	 * @return a Connection returns null on failure.
	 */
	public static Connection connect(Component parentComp, String driver, String url, String user, String passwd, String appname)
	{
		//-----------------------------------------------------
		// IF JDBC driver: H2, add hard coded stuff to URL
		//-----------------------------------------------------
		if ( driver.equals("org.h2.Driver") )
		{
			//-----------------------------------------------------
			// IF H2, add hard coded stuff to URL
			//-----------------------------------------------------
			H2UrlHelper urlHelper = new H2UrlHelper(url);
			Map<String, String> urlMap = urlHelper.getUrlOptionsMap();
			if (urlMap == null)
				urlMap = new LinkedHashMap<String, String>();

			boolean change = false;

			// Database short names are converted to uppercase for the DATABASE() function, 
			// and in the CATALOG column of all database meta data methods. 
			// Setting this to "false" is experimental. 
			// When set to false, all identifier names (table names, column names) are case 
			// sensitive (except aggregate, built-in functions, data types, and keywords).
			if ( ! urlMap.containsKey("DATABASE_TO_UPPER") )
			{
				change = true;
				_logger.info("H2 URL add option: DATABASE_TO_UPPER=false");
				urlMap.put("DATABASE_TO_UPPER", "false");
			}

//			// The maximum time in milliseconds used to compact a database when closing.
//			if ( ! urlMap.containsKey("MAX_COMPACT_TIME") )
//			{
//				change = true;
//				_logger.info("H2 URL add option: MAX_COMPACT_TIME=2000");
//				urlMap.put("MAX_COMPACT_TIME",  "2000");
//			}

//			// AutoServer mode
//			if ( ! urlMap.containsKey("AUTO_SERVER") )
//			{
//				change = true;
//				_logger.info("H2 URL add option: AUTO_SERVER=TRUE");
//				urlMap.put("AUTO_SERVER",  "TRUE");
//			}

//			// DATABASE_EVENT_LISTENER
//			if ( ! urlMap.containsKey("DATABASE_EVENT_LISTENER") )
//			{
//				change = true;
//				_logger.info("H2 URL add option: DATABASE_EVENT_LISTENER="+H2DatabaseEventListener.class.getName());
//				urlMap.put("DATABASE_EVENT_LISTENER",  H2DatabaseEventListener.class.getName());
//			}

			if (change)
			{
				urlHelper.setUrlOptionsMap(urlMap);
				url = urlHelper.getUrl();
				
				_logger.info("Added some options to the H2 URL. New URL is '"+url+"'.");
			}
		}

		try
		{
			Class.forName(driver).newInstance();
			Properties props = new Properties();
			props.put("user", user);
			props.put("password", passwd);
	
			_logger.debug("getConnection to driver='"+driver+"', url='"+url+"', user='"+user+"'.");
			Connection conn = DriverManager.getConnection(url, props);
	
			return conn;
		}
		catch (SQLException e)
		{
			StringBuffer sb = new StringBuffer();
			while (e != null)
			{
				sb.append( "\n" );
				sb.append( e.getMessage() );
				e = e.getNextException();
			}
			if (parentComp == null)
				_logger.error("Connection FAILED url='"+url+"', message: "+sb);
			else
				JOptionPane.showMessageDialog(parentComp, "Connection FAILED.\n\n"+sb.toString(), Version.getAppName()+" - jdbc connect", JOptionPane.ERROR_MESSAGE);
		}
		catch (Exception e)
		{
			if (parentComp == null)
				_logger.error("Connection FAILED url='"+url+"', Exception: "+e);
			else
				JOptionPane.showMessageDialog(parentComp, "Connection FAILED.\n\n"+e.toString(),  Version.getAppName()+" - jdbc connect", JOptionPane.ERROR_MESSAGE);
		}
		return null;
	}

}

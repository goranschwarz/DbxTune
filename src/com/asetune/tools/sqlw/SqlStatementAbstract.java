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
package com.asetune.tools.sqlw;

import java.awt.Component;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.JComponent;

import com.asetune.gui.ConnectionProfile;
import com.asetune.gui.ConnectionProfile.ConnProfileEntry;
import com.asetune.gui.ConnectionProfile.JdbcEntry;
import com.asetune.gui.ConnectionProfile.TdsEntry;
import com.asetune.sql.JdbcUrlParser;
import com.asetune.sql.SqlProgressDialog;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.tools.sqlw.msg.IMessageAware;
import com.asetune.tools.sqlw.msg.JLoadfileMessage;
import com.asetune.tools.sqlw.msg.MessageAwareAbstract;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.StringUtil;

public abstract class SqlStatementAbstract
extends MessageAwareAbstract
implements SqlStatement
{
	protected DbxConnection _conn;
	protected String _sqlOrigin;
	protected String _dbProductName;
	protected ArrayList<JComponent> _resultCompList;
	protected SqlProgressDialog _progress;
	protected Component _owner;
	protected QueryWindow _queryWindow;
	
	public SqlStatementAbstract(DbxConnection conn, String sqlOrigin, String dbProductName, ArrayList<JComponent> resultCompList, SqlProgressDialog progress, Component owner, QueryWindow queryWindow)
	throws SQLException
	{
		_conn           = conn;
		_sqlOrigin      = sqlOrigin;
		_dbProductName  = dbProductName;
		_resultCompList = resultCompList;
		_progress       = progress;
		_owner          = owner;
		_queryWindow    = queryWindow;
		
		if (StringUtil.isNullOrBlank(_dbProductName) && _conn != null)
			_dbProductName = _conn.getDatabaseProductName();
			
		setGuiOwner(owner); // in MessageAwareAbstract
	}

	@Override
	public void readRpcReturnCodeAndOutputParameters(ArrayList<JComponent> resultCompList, boolean asPlainText) throws SQLException
	{
	}
	
	public void setProgressState(String state)
	{
		if (_progress != null)
			_progress.setState(state);
	}

	public void addResultMessage(String msg)
	{
		if (_resultCompList != null)
			_resultCompList.add(new JLoadfileMessage(msg, _sqlOrigin));
	}

	@Override
	public String getPostExecSqlCommands()
	{
		return null;
	}

	@Override
	public void close()
	{
		// do nothing
	}



	/**
	 * Create a connection 
	 * 
	 * @param connProfile
	 * @param username
	 * @param password
	 * @param srvName
	 * @param dbname
	 * @param url
	 * @param sqlInitStr
	 * @param isDebug
	 * @return
	 * @throws Exception
	 */
	public DbxConnection getRightConnection(ConnectionProfile connProfile, String username, String password, String srvName, String dbname, String url, String sqlInitStr, boolean isDebug)
	throws Exception
	{
		return getRightConnection(this, connProfile, username, password, srvName, dbname, url, sqlInitStr, isDebug);
	}

	// NOTE: This code is also in: PipeCommandsDiff, SqlStatementCmdDbDiff, SqlStatementCmdRemoteSql, SqlStatementCmdTabDiff
	//       So if you change this... you will probably copy/paste it to those locations
	//       OR: implement it in a SUPER Class or a FACTORY Class
	/**
	 * Static method to create a connection 
	 * 
	 * @param ma
	 * @param connProfile
	 * @param username
	 * @param password
	 * @param srvName
	 * @param dbname
	 * @param url
	 * @param sqlInitStr
	 * @param isDebug
	 * @return
	 * @throws Exception
	 */
	public static DbxConnection getRightConnection(IMessageAware ma, ConnectionProfile connProfile, String username, String password, String srvName, String dbname, String url, String sqlInitStr, boolean isDebug)
	throws Exception
	{
		DbxConnection conn = null;
		ConnectionProp cp = new ConnectionProp();
		cp.setAppName("sqlw-dbdiff");
		
		if (connProfile == null)
		{
			cp.setUsername(username);
			cp.setPassword(password);
			cp.setServer  (srvName);
			cp.setDbname  (dbname);
			cp.setUrl     (url);
//			cp.setSshTunnelInfo(_params.);
//			cp.setUrlOptions(urlOptions);

			// Special for Sybase (ASE)
			if (StringUtil.hasValue(srvName))
			{
				String hostPortStr = null;
				if ( srvName.contains(":") )
					hostPortStr = srvName;
				else
					hostPortStr = AseConnectionFactory.getIHostPortStr(srvName);

				if (StringUtil.isNullOrBlank(hostPortStr))
					throw new Exception("Can't find server name information about '"+srvName+"', hostPortStr=null. Please try with -S hostname:port");

				url = "jdbc:sybase:Tds:" + hostPortStr;

				if ( ! StringUtil.isNullOrBlank(dbname) )
					url += "/" + dbname;
				cp.setUrl(url);
			}
		}
		else
		{
			ConnProfileEntry profileEntry = connProfile.getEntry();
			
			if (profileEntry instanceof TdsEntry)
			{
				TdsEntry entry = (TdsEntry) profileEntry;
				
				cp.setDbname       (entry._tdsDbname);
				cp.setPassword     (entry._tdsPassword);
				cp.setServer       (entry._tdsServer);
				cp.setSshTunnelInfo(entry._tdsShhTunnelUse ? entry._tdsShhTunnelInfo : null);
				cp.setUrl          (entry._tdsUseUrl ? entry._tdsUseUrlStr : null);
				cp.setUrlOptions   (entry._tdsUrlOptions);
				cp.setUsername     (entry._tdsUsername);
			}
			else if (profileEntry instanceof JdbcEntry)
			{
				JdbcEntry entry = (JdbcEntry) profileEntry;
				
//				cp.setDbname       (entry._jdbcDbname);
				cp.setPassword     (entry._jdbcPassword);
//				cp.setServer       (entry._jdbcServer);
				cp.setSshTunnelInfo(entry._jdbcShhTunnelUse ? entry._jdbcShhTunnelInfo : null);
				cp.setUrl          (entry._jdbcUrl);
				cp.setUrlOptions   (entry._jdbcUrlOptions);
				cp.setUsername     (entry._jdbcUsername);
			}
			
			// IF we have Command Line Parameters -->> Override the connection profile entry 
			if (StringUtil.hasValue(username)) cp.setUsername(username);
			if (StringUtil.hasValue(password)) cp.setPassword(password);
			if (StringUtil.hasValue(srvName )) cp.setServer  (srvName);
			if (StringUtil.hasValue(dbname  )) cp.setDbname  (dbname);
			if (StringUtil.hasValue(url     )) cp.setUrl     (url);
		}


		// if DBNAME is set and Postgres etc...
		// Special thing for Postgres (and possibly other DBMS where you can't do database context switch (with use) within a connection
		// meaning every database accessed needs to have it's own connection
		if (StringUtil.hasValue(cp.getDbname()))
		{
			String tmpUrl    = cp.getUrl();
			String tmpDbname = cp.getDbname();
			
			if (StringUtil.hasValue(tmpUrl))
			{
				if (   tmpUrl.startsWith("jdbc:postgresql:")
				    || tmpUrl.startsWith("jdbc:db2:")                // DB2 is not yet tested... I just assume
				   )
				{
					// Set the new database name
					JdbcUrlParser p = JdbcUrlParser.parse(tmpUrl);
					p.setPath("/"+tmpDbname); // set the new database name

					String newUrl = p.toUrl();
					cp.setUrl(newUrl);
					
					String profileName = ".";
					if (connProfile != null)
						profileName = ", from the Profile Name '" + connProfile.getName() + "'.";
					
					ma.addDebugMessage("Changing URL due to switch '-D'. from '" + tmpUrl + "', to '" + newUrl + "'" + profileName);
				}
			}
		}
		
		// Try to connect
		if (isDebug)
			ma.addDebugMessage("Try getConnection to: " + cp);
		
		// Make the connection
		conn = DbxConnection.connect(ma.getGuiOwnerAsWindow(), cp);
		
		// Change catalog... (but do not bail out on error)
		if ( ! StringUtil.isNullOrBlank(dbname) )
		{
			try { conn.setCatalog(dbname); }
			catch(SQLException ex) { ma.addErrorMessage("Changing database/catalog to '" + dbname + "' was not successful. Caught: " + ex); }
		}
		
		// Print out some destination information
		if (isDebug)
		{
			try
			{
				DatabaseMetaData dbmd = conn.getMetaData();
				String msg;

				try { msg = "Connected to DBMS Server Name '"          + conn.getDbmsServerName()         +"'."; ma.addDebugMessage(msg);} catch (SQLException ignore) {}
				try { msg = "Connected to URL '"                       + dbmd.getURL()                    +"'."; ma.addDebugMessage(msg);} catch (SQLException ignore) {}
				try { msg = "Connected using driver name '"            + dbmd.getDriverName()             +"'."; ma.addDebugMessage(msg);} catch (SQLException ignore) {}
				try { msg = "Connected using driver version '"         + dbmd.getDriverVersion()          +"'."; ma.addDebugMessage(msg);} catch (SQLException ignore) {}
				try { msg = "Connected to destination DBMS Vendor '"   + dbmd.getDatabaseProductName()    +"'."; ma.addDebugMessage(msg);} catch (SQLException ignore) {}
				try { msg = "Connected to destination DBMS Version '"  + dbmd.getDatabaseProductVersion() +"'."; ma.addDebugMessage(msg);} catch (SQLException ignore) {}
				try { msg = "Current Catalog in the destination srv '" + conn.getCatalog()                +"'."; ma.addDebugMessage(msg);} catch (SQLException ignore) {}
			}
			catch (SQLException ignore) {}
		}

		// Execute the SQL InitString
		if (StringUtil.hasValue(sqlInitStr))
		{
			String msg = "executing initialization SQL Stement '" + sqlInitStr + "'.";
			ma.addDebugMessage(msg);

			Statement stmnt = conn.createStatement();
			stmnt.executeUpdate(sqlInitStr);
			stmnt.close();
		}

		return conn;
	}
}

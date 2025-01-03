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
package com.dbxtune.utils;

import java.lang.invoke.MethodHandles;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.dbxtune.gui.swing.WaitForExecDialog;

public class RepServerUtils
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static class ConfigEntry
	{
		public boolean _isDefaultConfigured = false;
		public boolean _dynamicConfigOption = false;
		public String _configName           = "";
		public String _configValue          = "";
		public String _runValue             = "";
		public String _defaultValue         = "";
		public String _legalValues          = "";
		public String _datatype             = "";
		public String _status               = "";
		public String _table                = "";

		public ConfigEntry(boolean isDefaultConfigured, boolean dynamicConfigOption, String configName, String configValue, String runValue, String defaultValue, String legalValues, String datatype, String status, String table)
		{
			_isDefaultConfigured = isDefaultConfigured;
			_dynamicConfigOption = dynamicConfigOption;
			_configName          = configName;
			_configValue         = configValue;
			_runValue            = runValue;
			_defaultValue        = defaultValue;
			_legalValues         = legalValues;
			_datatype            = datatype;
			_status              = status;
			_table               = table;
		}

		public boolean isConfigOptionChanged()
		{
			return ! _isDefaultConfigured && ! _dynamicConfigOption;
		}
		public boolean isPending()
		{
			if (_configValue == null && _runValue != null)
				return true;
			return ! _configValue.equals(_runValue);
		}
		public boolean isRestartRequired()
		{
			if (_status == null)
				return false;
			
			return _status.indexOf("restart required") >= 0;
		}
		public String getConfigName()
		{
			return _configName;
		}
		public String getConfigValue()
		{
			return _configValue;
		}
		public String getTableName()
		{
			return _table;
		}
		public String getComments()
		{
			String comment          = "";
			String defaultConfigStr = "";

			String extraInfo = 
				"runValue="    +StringUtil.left(_runValue,    20, true, "'", "', ")
				+"datatype="   +StringUtil.left(_datatype,    11, true, "'", "', ")
				+"legalValues="+StringUtil.left(_legalValues, 35, true, "'", "', ")
				+"status="     +StringUtil.left(_status,      50, true, "'", "'.");

			if ( _isDefaultConfigured )
				comment = " -- Default Configured,                                " + extraInfo;

			if ( _dynamicConfigOption )
				comment = " -- This is a configuration this is normally different for all Replication Servers";

			if (isConfigOptionChanged())
				defaultConfigStr = " -- CHANGED CONFIGURATION: default="+StringUtil.left(_defaultValue, 20, true, "'", "'. ") + extraInfo;

			return defaultConfigStr + comment;
		}
	}

	public static List<ConfigEntry> getRsConfig(Connection conn)
	{
		String rcl = "admin config";
		return getConfig(conn, rcl);
	}

	public static List<ConfigEntry> getConnectionConfig(Connection conn, String ds, String db)
	{
		String rcl = "admin config, 'connection', '"+ds+"', '"+db+"'";
		return getConfig(conn, rcl);
	}

	public static List<ConfigEntry> getLogicalConnectionConfig(Connection conn, String lds, String ldb)
	{
		String rcl = "admin config, 'logical_connection', '"+lds+"', '"+ldb+"'";
		return getConfig(conn, rcl);
	}

	public static List<ConfigEntry> getTableConnectionConfig(Connection conn, String ds, String db)
	{
		String rcl = "admin config, 'table', '"+ds+"', '"+db+"'";
		return getConfig(conn, rcl);
	}

	public static List<ConfigEntry> getRouteConfig(Connection conn, String destinationRs)
	{
		String rcl = "admin config, 'route', '"+destinationRs+"'";
		return getConfig(conn, rcl);
	}

	private static String fixNewLineConfigString(String configValue)
	{
		String dsiCmdSeparator = configValue;
		dsiCmdSeparator = dsiCmdSeparator.replace("<server default>", "");
		dsiCmdSeparator = dsiCmdSeparator.replace('\r', ' ');
		dsiCmdSeparator = dsiCmdSeparator.replace('\n', ' ');
		dsiCmdSeparator = dsiCmdSeparator.replaceAll(" ", "");
		dsiCmdSeparator = dsiCmdSeparator.replace("newline()", "");
		dsiCmdSeparator = dsiCmdSeparator.trim();

		if ("".equals(dsiCmdSeparator))
			configValue = "newline()";
		
		return configValue;
	}
	private static List<ConfigEntry> getConfig(Connection conn, String rcl)
	{
//		LinkedHashMap<String, ConfigEntry> result = new LinkedHashMap<String, ConfigEntry>();
		List<ConfigEntry> result = new ArrayList<ConfigEntry>();

		boolean isTableLevel = rcl.indexOf("'table'") >= 0;
		try
		{
			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(rcl);
			if (stmt.execute(rcl))
			{
				// loop multiple resultset
				do
				{
					ResultSet rs = stmt.getResultSet();
					while (rs.next())
					{
						boolean isDefaultConfigured = false;
						boolean dynamicConfigOption = false;

						String configName   = rs.getString(1);
						String configValue  = rs.getString(2);
						String runValue     = rs.getString(3);
						String defaultValue = rs.getString(4);
						String legalValues  = rs.getString(5);
						String datatype     = rs.getString(6);
						String status       = rs.getString(7);
						String table        = isTableLevel ? rs.getString(8) : ""; // only if: admin config, 'table', SRV, DB 
		
	//System.out.println("DEBUG: configName='"+configName+"', configValue='"+configValue+"', defaultValue='"+defaultValue+"'.");
						if (configValue == null)
							continue;
		
						// Skip some configuration values
						if (    configName.equals("config_file")
						     || configName.equals("errorlog_file")
						     || configName.equals("id_server")
						     || configName.equals("ID_user")
						     || configName.equals("interfaces_file")
						     || configName.equals("keytab_file")
						     || configName.equals("oserver")
						     || configName.equals("RS_charset")
						     || configName.equals("RS_language")
						     || configName.equals("rs_name")
						     || configName.equals("RS_sortorder")
						     || configName.equals("RS_ssl_identity")
						     || configName.equals("RS_unicode_sortorder")
						     || configName.equals("RSSD_database")
						     || configName.equals("RSSD_maint_user")
						     || configName.equals("RSSD_primary_user")
						     || configName.equals("RSSD_sec_mechanism")
						     || configName.equals("RSSD_server")
						     || configName.equals("trace_file")
		
						     // all defaultValues with 'N/A'
						     || "N/A".equals(defaultValue)
						   )
							dynamicConfigOption = true;
//							continue;
		
						if (configName.equals("dsi_isolation_level") && "default".equals(configValue))
							isDefaultConfigured = true;
							
						if (configName.equals("dsi_cmd_separator"))
						{
							configValue  = fixNewLineConfigString(configValue);
							runValue     = fixNewLineConfigString(runValue);
							defaultValue = fixNewLineConfigString(defaultValue);

							if ("newline()".equals(configValue))
								isDefaultConfigured = true;
						}
						
						if ("default".equals(defaultValue))
							isDefaultConfigured = true;
		
						if ( configValue.equals(defaultValue))
							isDefaultConfigured = true;
		
						// if connection/logical_connection is inherited by the servers defaults
						if ("<server default>".equals(configValue))
							isDefaultConfigured = true;
						
						ConfigEntry cfgEntry = new ConfigEntry(isDefaultConfigured, dynamicConfigOption, 
								configName, configValue, runValue, defaultValue, legalValues, datatype, status, table);

//						result.put(configName, configValue);
//						result.put(configName, cfgEntry);
						result.add(cfgEntry);
					}
					rs.close();
				}
				while (stmt.getMoreResults());
			}
			stmt.close();
		}
		catch (SQLException ex)
		{
			// 15565 - No customized table-level configuration for any table.
			if (ex.getErrorCode() == 15565)
				return result;

			_logger.warn("Problems when executing rcl: "+rcl, ex);
			return null;
		}

		return result;
	}

	public static Map<String, String> getConfigDescriptions(Connection conn)
	{
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		String cmd = "select optionname, comments from rs_config";

		try
		{
			Statement stmt = conn.createStatement();
			
			// Connect in gateway mode to the RSSD
			connectGwRssd(conn);
			
			ResultSet rs = stmt.executeQuery(cmd);
			while (rs.next())
			{
				result.put(rs.getString(1), rs.getString(2));
			}
			rs.close();
			stmt.close();
	
			// Disconnect from gateway mode from the RSSD
			disconnectGw(conn);
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems when executing: "+cmd, ex);
		}
		
		return result;
	}

	public static String getServerName(Connection conn)
	throws SQLException
	{
		String cmd = "select charvalue from rs_config where optionname = 'oserver'";
		String retStr = "";

		Statement stmt = conn.createStatement();
		
		// Connect in gateway mode to the RSSD
		connectGwRssd(conn);
		
		ResultSet rs = stmt.executeQuery(cmd);
		while (rs.next())
		{
			retStr = rs.getString(1);
		}
		rs.close();
		stmt.close();

		// Disconnect from gateway mode from the RSSD
		disconnectGw(conn);
		
		return retStr;
	}

	/**
	 * Get the currently used errolog on the server 
	 * 
	 * @param conn
	 * @return the errolog 
	 * @throws SQLException
	 */
	public static String getServerLogFileName(Connection conn)
	throws SQLException
	{
		String cmd = "admin log_name";
		String retStr = "";

		Statement stmt = conn.createStatement();
		
		ResultSet rs = stmt.executeQuery(cmd);
		while (rs.next())
		{
			retStr = rs.getString(1);
		}
		rs.close();
		stmt.close();

		return retStr;
	}

	/**
	 * Get the version string of the server 
	 * 
	 * @param conn
	 * @return the version string 
	 * @throws SQLException
	 */
	public static String getServerVersionStr(Connection conn)
	throws SQLException
	{
		String cmd = "admin version";
		String retStr = "";

		Statement stmt = conn.createStatement();
		
		ResultSet rs = stmt.executeQuery(cmd);
		while (rs.next())
		{
			retStr = rs.getString(1);
		}
		rs.close();
		stmt.close();

		return retStr;
	}

	public static void connectGwDb(Connection conn, String srv, String db)
	throws SQLException
	{
		// connect "srv"."db"
		String cmd = "connect to \""+srv+"\".\""+db+"\"";
		try
		{
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(cmd);
			stmt.close();
		}
		catch (SQLException ex)
		{
			// 15539 - Gateway connection to 'GORAN_3_DS.rssd' is created.
			if (ex.getErrorCode() == 15539)
				return;
			_logger.warn("Problems when executing: "+cmd, ex);
			throw ex;
		}
	}
	public static void disconnectGw(Connection conn)
	throws SQLException
	{
		// disconnect "srv"."db"
		String cmd = "disconnect all";
		try
		{
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(cmd);
			stmt.close();
		}
		catch (SQLException ex)
		{
			// 15540 - Gateway connection to 'GORAN_3_DS.rssd' is dropped.
			if (ex.getErrorCode() == 15540)
				return;
			_logger.warn("Problems when executing: "+cmd, ex);
			throw ex;
		}
	}
	public static void disconnectGwNoThrow(Connection conn)
	{
		String cmd = "disconnect all";
		try
		{
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(cmd);
			stmt.close();
		}
		catch (SQLException ex)
		{
			// 15540 - Gateway connection to 'GORAN_3_DS.rssd' is dropped.
			if (ex.getErrorCode() == 15540)
				return;
			_logger.warn("Problems when executing: "+cmd, ex);
		}
	}

	public static void connectGwRssd(Connection conn)
	throws SQLException
	{
		String cmd = "connect to rssd";
		try
		{
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(cmd);
			stmt.close();
		}
		catch (SQLException ex)
		{
			// 15539 - Gateway connection to 'GORAN_3_DS.rssd' is created.
			if (ex.getErrorCode() == 15539)
				return;
			_logger.warn("Problems when executing: "+cmd, ex);
			throw ex;
		}
	}

	public static List<String> getConnections(Connection conn)
	{
		ArrayList<String> result = new ArrayList<String>();

		String cmd = "select dsname, dbname from rs_databases where ltype = 'P' and prsid = (select id from rs_sites where name = (select charvalue from rs_config where optionname = 'oserver'))";
		
		try
		{
			Statement stmt = conn.createStatement();
			
			// Connect in gateway mode to the RSSD
			connectGwRssd(conn);
			
			ResultSet rs = stmt.executeQuery(cmd);
			while (rs.next())
			{
				String ds = rs.getString(1);
				String db = rs.getString(2);

				result.add(ds + "." + db);
			}
			rs.close();
			stmt.close();

			// Disconnect from gateway mode from the RSSD
			disconnectGw(conn);			
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems when executing: "+cmd, ex);
			return null;
		}

		return result;
	}

	public static List<String> getLogicalConnections(Connection conn)
	{
		return getLogicalConnections(conn, false);
	}
	public static List<String> getLogicalConnections(Connection conn, boolean addActiveStandby)
	{
		ArrayList<String> result = new ArrayList<String>();

		String cmd = 
			"select dsname, dbname \n" +
			"from rs_databases \n" +
			"where ltype = 'L' \n" +
			"  and ptype = 'L' \n" +
			"  and prsid = (select id \n" +
			"               from rs_sites \n" +
			"               where name = (select charvalue \n" +
			"                             from rs_config \n" +
			"                             where optionname = 'oserver') \n" +
			"               )";
		
		try
		{
			Statement stmt = conn.createStatement();
			
			// Connect in gateway mode to the RSSD
			connectGwRssd(conn);
			
			ResultSet rs = stmt.executeQuery(cmd);
			while (rs.next())
			{
				String ds = rs.getString(1);
				String db = rs.getString(2);

				result.add(ds + "." + db);
			}
			rs.close();

			// add extra info
			if (addActiveStandby)
			{
				// Create a new List, which will hold the "logicalSrv.logicalDb:actievSrv.activeDb:standbySrv.standbyDb"
				ArrayList<String> extendedResult = new ArrayList<String>();

				// Loop the result list and get Active/standby connection names
				for (String dsdb : result)
				{
					String[] sa = dsdb.split("\\.");
					String ds = sa[0];
					String db = sa[1];
	
					cmd = "select dsname, dbname, ptype from rs_databases \n" +
					      "where ptype != 'L' \n" +
					      "  and ldbid = ( select dbid  \n" +
					      "                from rs_databases \n" +
					      "                where dsname = '"+ds+"' \n" +
					      "                  and dbname = '"+db+"' \n" +
					      "              ) \n";
					
					String active  = "none";
					String standby = "none";

					rs = stmt.executeQuery(cmd);
					while (rs.next())
					{
						String pds   = rs.getString(1);
						String pdb   = rs.getString(2);
						String ptype = rs.getString(3);

						if ("A".equals(ptype)) active  = pds + "." + pdb;
						if ("S".equals(ptype)) standby = pds + "." + pdb;
					}
					extendedResult.add(dsdb + ":" + active + ":" + standby);
					rs.close();
				}
				// Move the new list into the result list
				result = extendedResult;
			}

			stmt.close();

			// Disconnect from gateway mode from the RSSD
			disconnectGw(conn);
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems when executing: "+cmd, ex);
			return null;
		}

		return result;
	}

	public static List<String> getRoutes(Connection conn)
	{
		ArrayList<String> result = new ArrayList<String>();

//		String cmd = // CHECK THE BELOW SQL
//			"select r.name \n" +
//			"from rs_routes r, rs_sites s \n" +
//			"where r.source_rsid = (select x.id from rs_sites x where x.name = (select charvalue from rs_config c where c.optionname = 'oserver')) \n" +
//			"  and r.dest_rsid = s.id";
//		
//		try
//		{
//			Statement stmt = conn.createStatement();
//			
//			// Connect in gateway mode to the RSSD
//			connectRssd(conn);
//			
//			ResultSet rs = stmt.executeQuery(cmd);
//			while (rs.next())
//			{
//				String destRs = rs.getString(1);
//
//				result.add(destRs);
//			}
//			rs.close();
//			stmt.close();
//
//			// Disconnect from gateway mode from the RSSD
//			disConnectRssd(conn);
//		}
//		catch (SQLException ex)
//		{
//			_logger.warn("Problems when executing: "+cmd, ex);
//			return null;
//		}

		return result;
	}

	public static String printConfig(Connection conn, boolean onlyChangedConfigs, WaitForExecDialog waitDialog)
	{
		StringBuilder sb = new StringBuilder();
		
		Map<String, String> configDescription = getConfigDescriptions(conn);

		sb.append("-- BEGIN: OF CONFIGURATION REPORT").append("\n");

		// REP SERVER Configurations
		if (waitDialog != null) 
			waitDialog.setState("Getting Global Configuration");
		sb.append("\n");
		sb.append("-----------------------------------------------------------").append("\n");
		sb.append("-- REPLICATION SERVER GLOBAL CONFIGURATIONS").append("\n");
		sb.append("-----------------------------------------------------------").append("\n");
		List<ConfigEntry> rsConfig = getRsConfig(conn);
		for (ConfigEntry ce : rsConfig)
		{
			if ( (onlyChangedConfigs && ce.isConfigOptionChanged()) || ! onlyChangedConfigs )
			{
				String prefix = "   ";
				String cfgDesc = configDescription.get(ce.getConfigName());

				if (ce._dynamicConfigOption || ce._isDefaultConfigured)
					prefix = "-- ";

				sb.append(prefix)
					.append("configure replication server set ")
					.append(StringUtil.left(ce.getConfigName(), 30, true))
					.append(" to ")
					.append(StringUtil.left(ce.getConfigValue(), 40, true, "'"))
					.append(ce.getComments())
					.append(" Description='").append(cfgDesc).append("'.")
					.append("\n");
			}
		}
		

		// CONNECTIONS Configurations
		if (waitDialog != null) 
			waitDialog.setState("Getting Physical Connection Configuration");
		sb.append("\n");
		sb.append("\n");
		sb.append("-----------------------------------------------------------").append("\n");
		sb.append("-- CONNECTION(S) CONFIGURATIONS").append("\n");
		sb.append("-----------------------------------------------------------").append("\n");
		List<String> rsDbs = getConnections(conn);
		for (String dsdb : rsDbs)
		{
			String[] sa = dsdb.split("\\.");
			String ds = sa[0];
			String db = sa[1];
//System.out.println("CONN: dsdb='"+dsdb+"', ds='"+ds+"', db='"+db+"'.");

			if (waitDialog != null) 
				waitDialog.setState("Getting Physical Connection Configuration, for '"+ds+"."+db+"'.");

			sb.append("\n");
			sb.append("/* CONNECTION: "+ds+"."+db).append(" */\n");
			boolean printedRecords = false;
			List<ConfigEntry> config = getConnectionConfig(conn, ds, db);
			for (ConfigEntry ce : config)
			{
				if ( (onlyChangedConfigs && ce.isConfigOptionChanged()) || ! onlyChangedConfigs )
				{
					printedRecords = true;

					String prefix = "   ";
					String cfgDesc = configDescription.get(ce.getConfigName());

					if (ce._dynamicConfigOption || ce._isDefaultConfigured)
						prefix = "-- ";

					sb.append(prefix)
						.append("alter connection to ")
						.append(StringUtil.left("\"" + ds + "\".\"" + db + "\"", 40))
						.append(" set ")
						.append(StringUtil.left(ce.getConfigName(), 30, true))
						.append(" to ")
						.append(StringUtil.left(ce.getConfigValue(), 40, true, "'"))
						.append(ce.getComments())
						.append(" Description='").append(cfgDesc).append("'.")
						.append("\n");
				}
			}
			if ( ! printedRecords )
				sb.append("      -- no local configurations").append("\n");

			sb.append("   ---------------------------------------------------------------------------\n");
			sb.append("   -- TABLE LEVEL CONFIGURATION: "+ds+"."+db).append(" \n");
			printedRecords = false;
			config = getTableConnectionConfig(conn, ds, db);
			for (ConfigEntry ce : config)
			{
				if ( (onlyChangedConfigs && ce.isConfigOptionChanged()) || ! onlyChangedConfigs )
				{
					printedRecords = true;

					String prefix = "   ";
					String cfgDesc = configDescription.get(ce.getConfigName());

					if (ce._dynamicConfigOption || ce._isDefaultConfigured)
						prefix = "-- ";

					sb.append(prefix)
						.append("alter connection to ")
						.append(StringUtil.left("\"" + ds + "\".\"" + db + "\"", 40))
						.append(" for table named ")
						.append(StringUtil.left(ce.getTableName(), 30, true))
						.append(" set ")
						.append(StringUtil.left(ce.getConfigName(), 30, true))
						.append(" to ")
						.append(StringUtil.left(ce.getConfigValue(), 40, true, "'"))
						.append(ce.getComments())
						.append(" Description='").append(cfgDesc).append("'.")
						.append("\n");
				}
			}
			if ( ! printedRecords )
				sb.append("      -- No customized table-level configuration for any table.").append("\n");
		}
		if (rsDbs.size() == 0)
			sb.append("      -- NO CONNECTIONS").append("\n");


		// LOGICAL CONNECTION Configurations
		if (waitDialog != null) 
			waitDialog.setState("Getting Logical Connection Configuration");
		sb.append("\n");
		sb.append("\n");
		sb.append("-----------------------------------------------------------").append("\n");
		sb.append("-- LOGICAL CONNECTION(S) CONFIGURATIONS").append("\n");
		sb.append("-----------------------------------------------------------").append("\n");
		List<String> rsLDbs = getLogicalConnections(conn);
		for (String dsdb : rsLDbs)
		{
			String[] sa = dsdb.split("\\.");
			String lds = sa[0];
			String ldb = sa[1];
//System.out.println("LOG CONN: dsdb='"+dsdb+"', lds='"+lds+"', ldb='"+ldb+"'.");

			if (waitDialog != null) 
				waitDialog.setState("Getting Logical Connection Configuration, for '"+lds+"."+ldb+"'.");

			sb.append("\n");
			sb.append("/* LOGICAL CONNECTION: "+lds+"."+ldb).append(" */\n");
			boolean printedRecords = false;
			List<ConfigEntry> config = getLogicalConnectionConfig(conn, lds, ldb);
			for (ConfigEntry ce : config)
			{
				if ( (onlyChangedConfigs && ce.isConfigOptionChanged()) || ! onlyChangedConfigs )
				{
					printedRecords = true;

					String prefix = "   ";
					String cfgDesc = configDescription.get(ce.getConfigName());

					if (ce._dynamicConfigOption || ce._isDefaultConfigured)
						prefix = "-- ";

					sb.append(prefix)
						.append("alter logical connection to ")
						.append(StringUtil.left("\"" + lds + "\".\"" + ldb + "\"", 40))
						.append(" set ")
						.append(StringUtil.left(ce.getConfigName(), 30, true))
						.append(" to ")
						.append(StringUtil.left(ce.getConfigValue(), 40, true, "'"))
						.append(ce.getComments())
						.append(" Description='").append(cfgDesc).append("'.")
						.append("\n");
				}
			}
			if ( ! printedRecords )
				sb.append("      -- no local configurations").append("\n");
		}
		if (rsLDbs.size() == 0)
			sb.append("      -- NO LOGICAL CONNECTIONS").append("\n");

		// ROUTE Configurations
		if (waitDialog != null) 
			waitDialog.setState("Getting Route Configuration");
		sb.append("\n");
		sb.append("\n");
		sb.append("-----------------------------------------------------------").append("\n");
		sb.append("-- ROUTE CONFIGURATIONS").append("\n");
		sb.append("-----------------------------------------------------------").append("\n");
		List<String> rsRoutes = getRoutes(conn);
		for (String routeTo : rsRoutes)
		{
			if (waitDialog != null) 
				waitDialog.setState("Getting Route Configuration, for '"+routeTo+"'.");

			sb.append("\n");
			sb.append("/* ROUTE To: ").append(routeTo).append(" */\n");
			boolean printedRecords = false;
			List<ConfigEntry> config = getRouteConfig(conn, routeTo);
			for (ConfigEntry ce : config)
			{
				if ( (onlyChangedConfigs && ce.isConfigOptionChanged()) || ! onlyChangedConfigs )
				{
					printedRecords = true;

					String prefix = "   ";
					String cfgDesc = configDescription.get(ce.getConfigName());

					if (ce._dynamicConfigOption || ce._isDefaultConfigured)
						prefix = "-- ";

					sb.append(prefix)
						.append("alter route to ")
						.append(StringUtil.left(routeTo, 30, true, "\""))
						.append(" set ")
						.append(StringUtil.left(ce.getConfigName(), 30, true))
						.append(" to ")
						.append(StringUtil.left(ce.getConfigValue(), 40, true, "'"))
						.append(ce.getComments())
						.append(" Description='").append(cfgDesc).append("'.")
						.append("\n");
				}
			}
			if ( ! printedRecords )
				sb.append("      -- no route configurations").append("\n");
		}
		if (rsRoutes.size() == 0)
			sb.append("      -- NO ROUTES").append("\n");
		
		sb.append("\n");
		sb.append("-- END: OF CONFIGURATION REPORT").append("\n");
		
		return sb.toString();
	}

	public static boolean isRepAgentAlive(Connection conn, String dsname, String dbname)
	throws SQLException
	{
		String cmd = "exec sp_help_rep_agent '"+dbname+"', 'process'";

		// Expected output: for more info see: isRepAgentAlive()

		boolean isRunning = false;
		try
		{
			String spid = null;

			Statement stmt = conn.createStatement();
			//stmt.setQueryTimeout( getQueryTimeout() );
			ResultSet rs = stmt.executeQuery(cmd);

			while (rs.next())
			{
				spid        = rs.getString(2);
			}
			rs.close();
			stmt.close();

			if (spid != null)
			{
				if ( ! spid.equalsIgnoreCase("n/a") )
				{
					isRunning = true;
				}
			}

			return isRunning;
		}
		catch (SQLException sqle)
		{
			String msg = "Problems when executing '"+cmd+"' in ASE Server '"+dsname+"'. ";
			_logger.error(msg + AseConnectionUtils.sqlExceptionToString(sqle));
			throw sqle;
		}
	}
	
	public static void stopRepAgent(Connection conn, String dsname, String dbname, boolean force)
	throws SQLException
	{
		String cmd = "exec ["+dbname+"]..sp_stop_rep_agent '"+dbname+"'";
		if (force)
			cmd += ", 'nowait'";

		// Expected output: for more info see: isRepAgentAlive()

		try
		{
			Statement stmnt = conn.createStatement();
			//stmnt.setQueryTimeout( value );
			stmnt.executeUpdate(cmd);
			stmnt.close();
		}
		catch (SQLException sqle)
		{
			String msg = "Problems when executing '"+cmd+"' in ASE Server '"+dsname+"'. ";
			_logger.error(msg + AseConnectionUtils.sqlExceptionToString(sqle));
			throw sqle;
		}
	}

	public static void startRepAgent(Connection conn, String dsname, String dbname)
	throws SQLException
	{
		String cmd = "exec ["+dbname+"]..sp_start_rep_agent '"+dbname+"'";

		// Expected output: for more info see: isRepAgentAlive()

		try
		{
			Statement stmnt = conn.createStatement();
			//stmnt.setQueryTimeout( value );
			stmnt.executeUpdate(cmd);
			stmnt.close();
		}
		catch (SQLException sqle)
		{
			String msg = "Problems when executing '"+cmd+"' in ASE Server '"+dsname+"'. ";
			_logger.error(msg + AseConnectionUtils.sqlExceptionToString(sqle));
			throw sqle;
		}
	}

	public static String getRsCharset(Connection conn)
	{
//		String cmd = "\\rpc sp_serverinfo ? :(string='server_csname')";

		String retStr = "-unknown-";

		try
		{
			CallableStatement stmt = conn.prepareCall("{call sp_serverinfo ?}");
			stmt.setString(1, "server_csname");;
//			stmt.registerOutParameter(1, java.sql.Types.INTEGER);
			
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
				retStr = rs.getString(1);

			rs.close();
			stmt.close();
		}
		catch (SQLException e)
		{
			_logger.warn("Problem occurred when getting CHARSET from Replication Server. Caught: " + e);
		}
		return retStr;
	}

	public static String getRsSortorder(Connection conn)
	{
//		String cmd = "\\rpc sp_serverinfo ? :(string='server_soname')";

		String retStr = "-unknown-";

		try
		{
			CallableStatement stmt = conn.prepareCall("{call sp_serverinfo ?}");
			stmt.setString(1, "server_soname");;
//			stmt.registerOutParameter(1, java.sql.Types.INTEGER);
			
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
				retStr = rs.getString(1);

			rs.close();
			stmt.close();
		}
		catch (SQLException e)
		{
			_logger.warn("Problem occurred when getting SORT-ORDER from Replication Server. Caught: " + e);
		}
		return retStr;
	}

	public static void main(String[] args)
	{
		System.out.println("Usage: host port user passwd");

		String host = "localhost";
		String port = "5100";
		String user = "sa";
		String pass = "";

		if (args.length >= 1) host = args[0];
		if (args.length >= 2) port = args[1];
		if (args.length >= 3) user = args[2];
		if (args.length >= 4) pass = args[3];

		System.out.println("       host:   "+host);
		System.out.println("       port:   "+port);
		System.out.println("       user:   "+user);
		System.out.println("       passwd: "+pass);
		
		int portNum = Integer.parseInt(port);
		try
		{
			// Set Log4j Log Level
			Configurator.setRootLevel(Level.DEBUG);

			Connection conn = AseConnectionFactory.getConnection(host, portNum, null, user, pass, "test-rsConfig", null, null);
			System.out.println(printConfig(conn, true, null));
//			printConfig(conn, false);
		}
		catch (ClassNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

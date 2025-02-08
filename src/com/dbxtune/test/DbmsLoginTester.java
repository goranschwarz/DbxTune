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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * THIS IS A FAST HACK: To check/validate if *all* passwords stored in GitLab secrets is VALID (can we login)
 */
public class DbmsLoginTester
{
	private GitLabSecrets _gitlabInfo;
	private String        _configFile;
	private Configuration _config;
	private int           _debugLevel = 0;
	private List<DbmsConnTester> _testEntries = new ArrayList<>();
//	private Map<DbmsEnvType, List<String>> _envToHostsMap = new LinkedHashMap<>();
	private Map<DbmsEnvType, Map<DbmsVendorType, List<String>>> _envVendorToHostsMap = new LinkedHashMap<>();

	private List<ResultRecord> _resultRecords = new ArrayList<>();
	
	
	public enum DbmsEnvType
	{
		PROD, DEV, SYS, INT, STAGE//, UNKNOWN
	};
	
	public enum DbmsVendorType
	{
		SYBASE, MSSQL, POSTGRES
	};
	
	/**
	 * THIS IS A FAST HACK: To check/validate if *all* passwords stored in GitLab secrets is VALID (can we login)
	 */
	public static void main(String[] args)
	{
		System.out.println("Usage: filename");

		System.out.println(" *** Mapping of Getlab secrets/passwords will be do using the following algorithm!");
		System.out.println("     <VENDOR>_<ENV>__username__PASSWD");
		System.out.println();
		System.out.println("   - SYBASE   environments starts with 'SYB_'");
		System.out.println("   - MS SQL   environments starts with 'MSSQL_'");
		System.out.println("   - POSTGRES environments starts with 'PG_'");
		System.out.println();

		if (args.length == 0)
		{
			System.out.println();
			System.out.println(" - To few arguments - You need to pass a filename with properties (configurations)");
			System.out.println();
			System.out.println(" - Below is a example of this file:");
			System.out.println("-------------------------------------------------------------------------------");
			System.out.println("## Gitlab info");
			System.out.println("gitlab.secrets.projectId = 44904889");
			System.out.println("gitlab.secrets.token     = glpat-xxxxxxxxxxxxxxxxxxxx");
			System.out.println();
			System.out.println("## Regex to SKIP some users from secrets/variables in GitLab");
			System.out.println("test.skip.secrets.username.regex = .*_KEY");
			System.out.println();
			System.out.println("## What to test: below is a kombination of Vendors and 'types' of what to test...");
			System.out.println("test.dbms.vendor         = SYBASE, MSSQL, POSTGRES");
			System.out.println("test.dbms.env            = DEV, SYS, INT, STAGE, PROD");
			System.out.println();
			System.out.println("## What 'Vendor' and 'dbms-env' hosts we should try to connect to...");
			System.out.println("dbms.env.SYBASE.DEV      = dev-ase.maxm.se, sys-ase.maxm.se, int-ase.maxm.se, stage-ase.maxm.se");
			System.out.println("dbms.env.SYBASE.PROD     = prod-a1-ase.maxm.se, prod-b1-ase.maxm.se");
			System.out.println(); 
			System.out.println("dbms.env.MSSQL.DEV       = dev-mssql.maxm.se");
			System.out.println("dbms.env.MSSQL.SYS       = sys-mssql.maxm.se");
			System.out.println("dbms.env.MSSQL.INT       = int-mssql.maxm.se");
			System.out.println("dbms.env.MSSQL.STAGE     = stage-mssql.maxm.se");
			System.out.println("dbms.env.MSSQL.PROD      = prod-a1-mssql.maxm.se, prod-b1-mssql.maxm.se");
			System.out.println();
			System.out.println("dbms.env.POSTGRES.DEV    = dev-a1-mssql.maxm.se");
			System.out.println("dbms.env.POSTGRES.SYS    = sys-a1-mssql.maxm.se");
			System.out.println("dbms.env.POSTGRES.INT    = int-a1-mssql.maxm.se");
			System.out.println("dbms.env.POSTGRES.STAGE  = stage-a1-mssql.maxm.se");
			System.out.println("dbms.env.POSTGRES.PROD   = prod-a1-mssql.maxm.se, prod-b1-mssql.maxm.se");
			System.out.println();
			System.out.println("## And some other properties");
			System.out.println("#report.print.password.on.success = true");
			System.out.println("-------------------------------------------------------------------------------");
			System.out.println();
			System.exit(1);
		}
		
		String filename = args[0];

		Configuration conf = new Configuration(filename);
				
		//----------------------------------------------------------------
		// Get some properties
		//----------------------------------------------------------------
		String       projectId       = conf.getProperty("gitlab.secrets.projectId");
		String       gitlabToken     = conf.getProperty("gitlab.secrets.token");
		String       skipUsersRegex  = conf.getProperty("test.skip.secrets.username.regex", ".*_KEY");
		List<String> testDbmsVendors = StringUtil.parseCommaStrToList(conf.getProperty("test.dbms.vendor"), true);
		List<String> testDbmsEnvs    = StringUtil.parseCommaStrToList(conf.getProperty("test.dbms.env"),    true);

		boolean printPasswdOnSuccess   = conf.getBooleanProperty("report.print.password.on.success", false);
		
		List<String> missingPropsList = new ArrayList<>();
		if (StringUtil.isNullOrBlank(projectId))   { missingPropsList.add("gitlab.secrets.projectId"); }
		if (StringUtil.isNullOrBlank(gitlabToken)) { missingPropsList.add("gitlab.secrets.token"); }
		if (testDbmsVendors.isEmpty())             { missingPropsList.add("test.dbms.vendor"); }
		if (testDbmsEnvs   .isEmpty())             { missingPropsList.add("test.dbms.env"); }

		// EXIT if we are missing properties
		if ( ! missingPropsList.isEmpty() )
		{
			System.out.println();
			System.out.println("ERROR: Missing mandatory properties.");
			for (String name : missingPropsList)
			{
				System.out.println(" * '" + name + "'");
			}
			System.out.println();
			System.exit(1);
		}


		//----------------------------------------------------------------
		// Build login testers
		//----------------------------------------------------------------
		DbmsLoginTester loginTester = new DbmsLoginTester();

		System.out.println();
		System.out.println(" *** INFO: Building Connection Test Objects...");
		for (String vendor : testDbmsVendors)
		{
			DbmsVendorType vendorType = DbmsVendorType.valueOf(vendor);
			for (String env : testDbmsEnvs)
			{
				DbmsEnvType envType = DbmsEnvType.valueOf(env);

				String propName = "dbms.env." + vendorType + "." + envType;
				String hostname = conf.getProperty(propName);
				
				if (StringUtil.isNullOrBlank(hostname))
				{
					System.out.println(" *** WARNING: NO hostname was found for prop '" + propName + "', Skipping this entry...");
				}
				else
				{
					List<String> hostnameList = StringUtil.parseCommaStrToList(hostname, true);
					for (String host : hostnameList)
					{
						System.out.println("     INFO: adding host '" + host + "' for Vendor='" + vendorType + "' and env='" + envType + "'.");
						loginTester.addEnvHost(envType, vendorType, host);
					}
				}
			}
		}
		// EXIT if nothing was added
		if ( loginTester.isEmpty() )
		{
			System.out.println();
			System.out.println("ERROR: No host(s) was added to the test.");
			System.out.println();
			System.exit(1);
		}
		

		//----------------------------------------------------------------
		// Get Secrets
		//----------------------------------------------------------------
		GitLabSecrets gitlabSecrets = new GitLabSecrets(projectId, gitlabToken);

		try
		{
			System.out.println();
			System.out.println(" *** INFO: Getting Gitlab Secrets...");
			List<GitLabSecret> secrets = gitlabSecrets.getSecrets();
			System.out.println("     INFO: Found " + secrets.size() + " secrets.");
//			for (GitLabSecret entry : secrets)
//				System.out.println("GITLAB_ENTRY: " + entry);

//			loginTester.addSecrets(secrets, ".*_KEY");
			loginTester.addSecrets(secrets, skipUsersRegex);
		}
		catch (Exception ex)
		{
			System.out.println("ERROR: Can't access GitLab Secrets, Caught: " + ex);
			System.out.println("INFO: Used projectId='" + projectId + "', token='" + gitlabToken + "'.");
			System.out.println("Exiting...");
			System.exit(1);
		}


		//----------------------------------------------------------------
		// DO WORK
		//----------------------------------------------------------------
		System.out.println();
		System.out.println(" *** INFO: Connecting to all servers...");
		loginTester.doWork();
		
		System.out.println();
		System.out.println(" *** INFO: Creating Report");
		loginTester.printReport(printPasswdOnSuccess);
		
		//---------------------------------
		// SYB_PROD__fsuser__PASSWD		
		// ^^^ ^^^^  ^^^^^^
		// 1   2     3
		//---------------------------------
		// 1 = Dbms Type
		// 1 = Environment "marker" -- That is *mapped* to a List of servers (hostnames:port), can be one or several
		// 1 = Username
	}
	
	private boolean isEmpty()
	{
		return _envVendorToHostsMap.isEmpty();
//		return _testEntries.isEmpty();
	}

	public void addEnvHosts(DbmsEnvType envType, DbmsVendorType vendorType, List<String> hostList)
	{
		for (String hostname : hostList)
		{
			addEnvHost(envType, vendorType, hostname);
		}
	}

	public void addEnvHost(DbmsEnvType envType, DbmsVendorType vendorType, String hostname)
	{
		// Get Vendor Map for DBMS Environment 
		Map<DbmsVendorType, List<String>> envToVendorMap = _envVendorToHostsMap.get(envType);
		if (envToVendorMap == null)
		{
			envToVendorMap = new LinkedHashMap<>();
			_envVendorToHostsMap.put(envType, envToVendorMap);
		}
		
		// From the Vendors Map, get the List of host names 
		List<String> hostList = envToVendorMap.get(vendorType);
		if (hostList == null)
		{
			hostList = new ArrayList<>();
			envToVendorMap.put(vendorType, hostList);
		}
		
		// Finally add the hostname
		if ( ! hostList.contains(hostname) )
			hostList.add(hostname);
	}
	
	public List<String> getHosts(DbmsEnvType envType, DbmsVendorType vendorType)
	{
		Map<DbmsVendorType, List<String>> envToVendorMap = _envVendorToHostsMap.get(envType);
		if (envToVendorMap == null)
			return Collections.emptyList();

		List<String> hostList = envToVendorMap.get(vendorType);
		if (hostList == null)
			return Collections.emptyList();

		return hostList;
	}

	private void addSecrets(List<GitLabSecret> list, String skipRegEx)
	{
		for (GitLabSecret entry : list)
		{
			if ( ! entry.key.endsWith("__PASSWD") )
			{
				if (_debugLevel > 1)
					System.out.println("WARNING: Skipping GitlabSecret for key: " + entry.key);
				continue;
			}

			String dbmsType   = StringUtils.substringBefore(entry.key, "_");
			String envType    = StringUtils.substringBetween(entry.key, "_", "__");
			String dbmsUser   = StringUtils.substringBetween(entry.key, "__", "__PASSWD");
			String dbmsPasswd = entry.value;


			if (skipRegEx != null && !skipRegEx.trim().isEmpty())
			{
				if (dbmsUser.matches(skipRegEx))
				{
//					System.out.println("DEBUG: SKIPPING DUE TO, skipRegEx=|" + skipRegEx + "|. dbmsType=|" + dbmsType + "|, envType=|" + envType + "|, dbmsUser=|" + dbmsUser + "|, dbmsPasswd=|" + dbmsPasswd + "|.");
					System.out.println("DEBUG: SKIPPING DUE TO, skipRegEx=|" + skipRegEx + "|. dbmsType=|" + dbmsType + "|, envType=|" + envType + "|, dbmsUser=|" + dbmsUser + "|.");
					continue;
				}
			}
			
			if (_debugLevel > 5)
				System.out.println("DEBUG: dbmsType=|" + dbmsType + "|, envType=|" + envType + "|, dbmsUser=|" + dbmsUser + "|, dbmsPasswd=|" + dbmsPasswd + "|.");

			DbmsEnvType dbmsEnvType = DbmsEnvType.DEV;
			try 
			{
				dbmsEnvType = DbmsEnvType.valueOf(envType);
			}
			catch (Exception ex) 
			{
//				System.out.println("WARNING: Unsupported DBMS Environment type='" + envType + "' setting it to 'DEV', key='" + entry.key + "'");
//				dbmsEnvType = DbmsEnvType.DEV;
				System.out.println("WARNING: Unsupported DBMS Environment type='" + envType + "' Skipping this entry. key='" + entry.key + "'");
				continue;
			}
			
			if ("SYB".equals(dbmsType))
			{
				_testEntries.add( new AseConnTester(dbmsEnvType, dbmsUser, dbmsPasswd) );
			}
			else if ("MSSQL".equals(dbmsType))
			{
				_testEntries.add( new SqlServerConnTester(dbmsEnvType, dbmsUser, dbmsPasswd) );
			}
			else if ("PG".equals(dbmsType))
			{
				_testEntries.add( new PostgresConnTester(dbmsEnvType, dbmsUser, dbmsPasswd) );
			}
			else
			{
				System.out.println("WARNING: Unsupported DBMS Entry type='" + dbmsType + "', key='" + entry.key + "'");
			}
		}
	}
	
	public void doWork()
	{
		if (_debugLevel >= 5)
			System.out.println("TESTING: " + _testEntries.size() + " entries");

		int count = 0;
		for (DbmsConnTester entry : _testEntries)
		{
			count++;
			List<String> hostList = getHosts(entry._envType, entry._vendorType);

			if (_debugLevel >= 5)
				System.out.println("   --- [" + entry._vendorType + ":" + entry._envType + "]: hostnameList=" + hostList);

			if (_debugLevel == 0)
				System.out.println(" >>> Testing entry " + count + " of " + _testEntries.size() + " on " + (hostList == null ? 0 : hostList.size()) + " hostname(s). [" + entry._vendorType + ":" + entry._envType + "]: " + hostList);
			
			if (hostList != null)
			{
				for (String hostname : hostList)
				{
//					System.out.println("       >>>>>>>>>>>>>>>>>>>>>>>: " + this + " --- hostname=|" + hostname + "|.");
					boolean success = entry.test(hostname);

					addResultRecord(success, entry);
				}
			}
		}
	}
	
	public void addResultRecord(boolean success, DbmsConnTester entry)
	{
		ResultRecord record = new ResultRecord();
		
		record._vendorType = entry._vendorType;
		record._envType    = entry._envType;

		record._hostname   = entry._lastTestHostname;
		record._url        = entry._lastTestUrl;
		record._username   = entry.getUsername();
		record._password   = entry.getPassword();

		record._success    = success;
		record._result     = entry._lastResult;
		record._error      = entry._lastError;
		
		_resultRecords.add(record);
	}

	public class ResultRecord
	{
		DbmsVendorType _vendorType;
		DbmsEnvType _envType;

		String _hostname;
		String _url;
		String _username;
		String _password;

		boolean _success;
		String  _result;
		String  _error;
	}

	private void printReport(boolean printPasswdOnSuccess)
	{
		ResultSetTableModel rstm = ResultSetTableModel.createEmpty("report");
		
		int col = 0;		
		rstm.addColumn("Env"     , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
		rstm.addColumn("Vendor"  , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
		rstm.addColumn("Success" , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
		rstm.addColumn("Username", col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
		rstm.addColumn("Hostname", col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
		rstm.addColumn("Result"  , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
		rstm.addColumn("Url"     , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
		rstm.addColumn("Password", col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
		rstm.addColumn("Error"   , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);

		int successCount = 0;
		int warnCount = 0;
		int failCount = 0;
		
		System.out.println();
		System.out.println("---- BEGIN Test Report ----");
//		System.out.println("+--------+--------+----------+--------------");
		for (ResultRecord record : _resultRecords)
		{
			String password = "*secret*";
			if (printPasswdOnSuccess)
				password = record._password;

			ArrayList<Object> row = new ArrayList<>();

			row.add("" + record._envType);
			row.add("" + record._vendorType);
			
			if ( ! record._success ) 
			{
				row.add("FAIL");
				failCount++;

				password = record._password;
			}
			else
			{
				if (record._error == null)
				{
					row.add("OK");
					successCount++;
				}
				else
				{
					row.add("WARN");
					warnCount++;
				}
			}

			row.add("" + record._username);
			row.add("" + record._hostname);
			row.add("" + record._result);
			row.add("" + record._url);
			row.add("" + password);
			row.add("" + record._error);

			rstm.addRow(row);
			
			if (record._success)
				successCount++;
			else
				failCount++;
		}
		System.out.println(rstm.toAsciiTableString());
		System.out.println(" *** Success Count: " + successCount);
		System.out.println(" *** Warning Count: " + warnCount);
		System.out.println(" ***    Fail Count: " + failCount);
		System.out.println();
		System.out.println("---- END Test Report ----");
		System.out.println();
	}

	

	public abstract class DbmsConnTester
	{
		DbmsVendorType _vendorType;

		String _lastTestUrl;
		String _lastTestHostname;
		String _lastError;
		String _lastResult;

		String _dbname;
		int    _port;
		String _username;
		String _password;

		String _sql;
		String _expectedResult;
//		String _result;
		String _status;
//		Connection _conn; 
		DbmsEnvType _envType;
		
		public DbmsConnTester(DbmsEnvType dbmsEnvType, DbmsVendorType dbmsVendorType, String dbmsUser, String dbmsPasswd)
		{
			_vendorType = dbmsVendorType;
			_envType    = dbmsEnvType;
			_username   = dbmsUser;
			_password   = dbmsPasswd;
		}

		public abstract String getUrl(String hostname);
		
		public abstract String getBaseUrl();
		public abstract int    getDefaultPort();
		public abstract String getDefaultSql();
		public abstract String getDefaultDbname();
		public abstract List<Integer> getWarningErrorCodes();
		
		public boolean isVendorType(DbmsVendorType vendorType)
		{
			return _vendorType.equals(vendorType);
		}

//		public String getUrl(String hostname)
//		{
//			String url = getBaseUrl();
//			
////			url = url.replace("${HOSTNAME}", getHostname());
//			url = url.replace("${HOSTNAME}", hostname);
//			url = url.replace("${PORT}"    , getPort());
//			url = url.replace("${DBNAME}"  , getDbname());
//
//			return url;
//		}

//		public String getHostname()	{ return _hostname; }
//		public String getDbname()   { return _dbname != null ? _dbname : getDefaultDbname(); }
		public String getPort()     { return "" + (_port <= 0 ? getDefaultPort() : _port); }
		public String getPassword() { return _password; }
		public String getUsername() { return _username; }

		public String getSql()
		{
			if (_sql != null)
				return _sql;
			
			return getDefaultSql();
		}
		
		@Override
		public String toString()
		{
			return super.toString() +  "[envType='" + _envType + "', userName='" + _username + "', password='" + _password + "']";
		}

		public void test(List<String> hostList)
		{
			for (String hostname : hostList)
			{
				test(hostname);
			}
		}
		public boolean test(String hostname)
		{
			String url      = getUrl(hostname);
			String username = getUsername();
			String password = getPassword();
			
			_lastResult       = null;
			_lastError        = null;
			_lastTestUrl      = url;
			_lastTestHostname = hostname;

			if (_debugLevel >= 1)
				System.out.println("        TESTING[" + _envType + ":" + _vendorType + "@" + hostname + "]: username=|" + username + "|, password=|" + password + "|, url=|" + url + "|.");

//			if (true)
//				return true;

			try	(Connection conn = DriverManager.getConnection(url, username, password) )
			{
				boolean ret = true;

				String sql = getSql();
				if (sql != null && !sql.trim().isEmpty())
				{
					try (Statement stmnt= conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
					{
						while(rs.next())
						{
							_lastResult = rs.getString(1);
							if (_debugLevel >= 3)
								System.out.println("            >> RESULT: " + _lastResult);
						}
					}
					
					if (_expectedResult != null)
					{
						if ( ! _expectedResult.equals(_lastResult))
						{
							if (_debugLevel >= 1)
								System.out.println("ERROR: Unexpected result '" + _lastResult + "', expected '" + _expectedResult + "'.");

							_lastError = "Unexpected result '" + _lastResult + "', expected '" + _expectedResult + "'.";
							ret = false;
						}
					}
				}
				
				return ret;
			}
			catch (SQLException ex)
			{
				List<Integer> warningList = getWarningErrorCodes();
				if (warningList.contains(ex.getErrorCode()))
				{
					_lastError = "ErrorCode=" + ex.getErrorCode() + ", SqlState=" + ex.getSQLState() + ", Message=|" + ex.getMessage() + "|.";

					System.out.println("     WARNING: Problems Connecting to URL=|" + url + "|, username=|" + username + "|, password=|" + password + "|.");
					System.out.println("              ErrorCode=" + ex.getErrorCode() + ", SqlState=" + ex.getSQLState() + ", Message=|" + ex.getMessage() + "|.");
					
					return true;
				}
				else
				{
					_lastError = "ErrorCode=" + ex.getErrorCode() + ", SqlState=" + ex.getSQLState() + ", Message=|" + ex.getMessage() + "|.";

					System.out.println("     ERROR: Problems Connecting to URL=|" + url + "|, username=|" + username + "|, password=|" + password + "|.");
					System.out.println("            ErrorCode=" + ex.getErrorCode() + ", SqlState=" + ex.getSQLState() + ", Message=|" + ex.getMessage() + "|.");
					return false;
				}
			}
		}
	}

	public class AseConnTester
	extends DbmsConnTester
	{
		public AseConnTester(DbmsEnvType envType, String username, String password)
		{
			super(envType, DbmsVendorType.SYBASE, username, password);
		}

		@Override
		public String getUrl(String hostname)
		{
			String url = getBaseUrl();

			String port   = getPort();
//			String dbname = getDefaultDbname();

			url = url.replace("${HOSTNAME}", hostname);
			url = url.replace("${PORT}"    , port);
			url = url.replace("/${DBNAME}"  , ""); // SKIP Databases and see where they end of
			
			return url;
		}

		@Override
		public String getBaseUrl()
		{
			return "jdbc:sybase:Tds:${HOSTNAME}:${PORT}/${DBNAME}?ENCRYPT_PASSWORD=true";
		}

		@Override
		public int getDefaultPort()
		{
			return 5000;
		}

		@Override
		public String getDefaultDbname()
		{
			return "master";
		}

		@Override
		public String getDefaultSql()
		{
			// atSrv=xxx, dbname=xxx, username=xxx
			return "select 'atSrv=' + @@servername + ', dbname=' + db_name() + ', login=' + suser_name()";
		}

		@Override
		public List<Integer> getWarningErrorCodes()
		{
			return Collections.emptyList();
		}
	}

	public class SqlServerConnTester
	extends DbmsConnTester
	{
		public SqlServerConnTester(DbmsEnvType envType, String username, String password)
		{
			super(envType, DbmsVendorType.MSSQL, username, password);
		}

		@Override
		public String getUrl(String hostname)
		{
			String url = getBaseUrl();

			String port   = getPort();
			String dbname = getDefaultDbname();

			// Set 'dbname' based on 'username' logic
			String username = getUsername();
			
			if (username.endsWith("_owner")) dbname = username.substring(0, username.length() - "_owner".length());
			if (username.endsWith("_app"))   dbname = username.substring(0, username.length() - "_app"  .length());
			if (username.endsWith("_ro"))    dbname = username.substring(0, username.length() - "_ro"   .length());
			
			url = url.replace("${HOSTNAME}", hostname);
			url = url.replace("${PORT}"    , port);
			url = url.replace("${DBNAME}"  , dbname);
			
			return url;
		}

		@Override
		public String getBaseUrl()
		{
			return "jdbc:sqlserver://${HOSTNAME}:${PORT};databaseName=${DBNAME};encrypt=true;trustServerCertificate=true";
		}

		@Override
		public int getDefaultPort()
		{
			return 1433;
		}

		@Override
		public String getDefaultDbname()
		{
			return "master";
		}

		@Override
		public String getDefaultSql()
		{
			// atSrv=xxx, dbname=xxx, username=xxx
			return "select 'atSrv=' + @@servername + ', dbname=' + db_name() + ', login=' + suser_name()";
		}

		@Override
		public List<Integer> getWarningErrorCodes()
		{
			// 976: The target database, 'xxx', is participating in an availability group and is currently not accessible for queries. 
			//      Either data movement is suspended or the availability replica is not enabled for read access. 
			//      To allow read-only access to this and other databases in the availability group, enable read access to one or more 
			//      secondary availability replicas in the group.  For more information, see the ALTER AVAILABILITY GROUP statement 
			//      in SQL Server Books Online. ClientConnectionId:xxxxx
			return Arrays.asList(976);
		}
	}

	public class PostgresConnTester
	extends DbmsConnTester
	{
		public PostgresConnTester(DbmsEnvType envType, String username, String password)
		{
			super(envType, DbmsVendorType.POSTGRES, username, password);
		}

		@Override
		public String getUrl(String hostname)
		{
			String url = getBaseUrl();
			
			String port   = getPort();
			String dbname = getDefaultDbname();

			// Set 'dbname' based on 'username' logic
			String username = getUsername();
			
			if (username.endsWith("_owner")) dbname = username.substring(0, username.length() - "_owner".length());
			if (username.endsWith("_app"))   dbname = username.substring(0, username.length() - "_app"  .length());
			if (username.endsWith("_ro"))    dbname = username.substring(0, username.length() - "_ro"   .length());
			
			url = url.replace("${HOSTNAME}", hostname);
			url = url.replace("${PORT}"    , port);
			url = url.replace("${DBNAME}"  , dbname);
			
			return url;
		}

		@Override
		public String getBaseUrl()
		{
			return "jdbc:postgresql://${HOSTNAME}:${PORT}/${DBNAME}";
		}

		@Override
		public int getDefaultPort()
		{
			return 5432;
		}

		@Override
		public String getDefaultDbname()
		{
			return "postgres";
		}

		@Override
		public String getDefaultSql()
		{
			// atSrv=xxx, dbname=xxx, username=xxx
//			return "select 'atSrv=' + @@servername + ', dbname=' + db_name() + ', login=' + suser_name()";

			return "select 'atSrv=' || coalesce(current_setting('cluster_name'), inet_server_addr()||':'||inet_server_port(), '-unknown-') || ', dbname=' || current_database() || ', login=' || current_user";
		}


		@Override
		public List<Integer> getWarningErrorCodes()
		{
			return Collections.emptyList();
		}
	}








	private static class GitLabSecret 
	{
	    @JsonProperty("variable_type")
		public String variableType;

	    @JsonProperty("key")
		public String key;

	    @JsonProperty("value")
		public String value;

	    @JsonProperty("hidden")
		public boolean hidden;

	    @JsonProperty("protected")
		public boolean protectedVariable;

	    @JsonProperty("masked")
		public boolean masked;

	    @JsonProperty("raw")
		public boolean raw;

		@JsonProperty("environment_scope")
		public String environmentScope;

	    @JsonProperty("description")
		public String description;

	    @Override
		public String toString()
		{
			return "GitLabSecret [variableType=" + variableType + ", key=" + key + ", value=" + value + ", hidden=" + hidden + ", protectedVariable=" + protectedVariable + ", masked=" + masked + ", raw=" + raw + ", environmentScope=" + environmentScope + ", description=" + description + "]";
		}

//		@Override
//		public String toString()
//		{
//			return "GitLabSecret [key=" + key + ", value=" + value + "]";
//		}
	}

	private static class GitLabSecrets
	{
		String _projectId;
		String _gitlabToken;

//		String _apiUrl   = "https://gitlab.com/api/v4/projects/${PROJECT_ID}/variables?per_page=8000&page=1";
		String _apiUrl   = "https://gitlab.com/api/v4/projects/${PROJECT_ID}/variables?per_page=100&page=${PAGE_ID}";

		public GitLabSecrets(String projectId, String gitlabToken)
		{
			_projectId   = projectId;
			_gitlabToken = gitlabToken;
		}

		public String getApiUrl(int pageId)
		{
			return _apiUrl.replace("${PROJECT_ID}", _projectId).replace("${PAGE_ID}", Integer.toString(pageId));
		}

		private String getGitlabToken()
		{
			return _gitlabToken;
		}

		public List<GitLabSecret> getSecrets()
		throws Exception
		{
			// Get entries using pagination, for the moment 'per_page=100' is MAX
			// and then we need to make another request, with 'page=2', 'page=3'...
			try
			{
				List<GitLabSecret> secrets = new ArrayList<>();

				int page = 1;
				int totalPages = 1;
				
				while (page <= totalPages)
				{
					URL url = new URL(getApiUrl(page));
					System.out.println("     INFO: Getting Gitlab Secrets [page=" + page + "/" + (totalPages == 1 ? "?" : totalPages) + "] at URL: " + url);

					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					conn.setRequestProperty("PRIVATE-TOKEN", getGitlabToken());

					int responseCode = conn.getResponseCode();
					if ( responseCode == 200 )
					{
						BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
						String inputLine;
						StringBuilder response = new StringBuilder();

						while ((inputLine = in.readLine()) != null)
						{
							response.append(inputLine);
						}
						in.close();

//						System.out.println("Secret Value: " + response.toString());

						// Parse JSON using Jackson
						ObjectMapper objectMapper = new ObjectMapper();
						List<GitLabSecret> thisPageSecrets = objectMapper.readValue(response.toString(), new TypeReference<List<GitLabSecret>>() {});

						secrets.addAll(thisPageSecrets);
						
						// Get total pages from headers
						//  - First  try: 'X-Total-Pages'
						//  - Second try: 'x-total-pages'
						Map<String, List<String>> headers = conn.getHeaderFields();
						List<String> totalPagesHeader = headers.get("X-Total-Pages");
						if (totalPagesHeader == null) 
							totalPagesHeader = headers.get("x-total-pages");
						if (totalPagesHeader != null && !totalPagesHeader.isEmpty()) 
						{
							totalPages = Integer.parseInt(totalPagesHeader.get(0));
						}

						page++; // Move to next page
					}
					else
					{
						throw new Exception("Failed to retrieve secret. Response Code: " + responseCode);
//						System.out.println("Failed to retrieve secret. Response Code: " + responseCode);
//						return null;
					}
				}

				return secrets;
			}
			catch (Exception ex)
			{
				throw new Exception("Failed to retrieve secret. Caught: " + ex);
//				e.printStackTrace();
//				return null;
			}
		}

//		public List<GitLabSecret> getSecrets()
//		throws Exception
//		{
//			try
//			{
//				String nextPageUrl = getApiUrl();
//				List<GitLabSecret> secrets = new ArrayList<>();
//				
//				int count = 0;
//				while (nextPageUrl != null)
//				{
//					count++;
//					System.out.println("INFO: Getting Gitlab Secrets [page=" + count + "] at URL: " + nextPageUrl);
//					URL url = new URL(nextPageUrl);
//
//		            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//		            conn.setRequestMethod("GET");
//		            conn.setRequestProperty("PRIVATE-TOKEN", getGitlabToken());
//
//					int responseCode = conn.getResponseCode();
//					if ( responseCode == 200 )
//					{
//						BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//						String inputLine;
//						StringBuilder response = new StringBuilder();
//
//						while ((inputLine = in.readLine()) != null)
//						{
//							response.append(inputLine);
//						}
//						in.close();
//
////						System.out.println("Secret Value: " + response.toString());
//
//						// Parse JSON using Jackson
//						ObjectMapper objectMapper = new ObjectMapper();
//						List<GitLabSecret> thisPageSecrets = objectMapper.readValue(response.toString(), new TypeReference<List<GitLabSecret>>() {});
//
//						secrets.addAll(thisPageSecrets);
//						
//	                    // Get "Link" header for next page
//	                    nextPageUrl = getNextPageUrl(conn.getHeaderFields());
//					}
//					else
//					{
//						throw new Exception("Failed to retrieve secret. Response Code: " + responseCode);
////						System.out.println("Failed to retrieve secret. Response Code: " + responseCode);
////						return null;
//					}
//				}
//
//				return secrets;
//			}
//			catch (Exception ex)
//			{
//				throw new Exception("Failed to retrieve secret. Caught: " + ex);
////				e.printStackTrace();
////				return null;
//			}
//		}
//
//		private static String getNextPageUrl(Map<String, List<String>> headers)
//		{
//			List<String> linkHeaders = headers.get("Link");
//			if ( linkHeaders != null && !linkHeaders.isEmpty() )
//			{
//				String  linkHeader = linkHeaders.get(0);
//				Matcher matcher    = Pattern.compile("<(.*?)>; rel=\"next\"").matcher(linkHeader);
//				if ( matcher.find() )
//				{
//					// Extracts the next page URL
//					return matcher.group(1);
//				}
//			}
//			// No more pages
//			return null; 
//		}
		
//		public List<GitLabSecret> getSecrets()
//		throws Exception
//		{
//			try
//			{
//				URL url = new URL(getApiUrl());
//
//	            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//	            conn.setRequestMethod("GET");
//	            conn.setRequestProperty("PRIVATE-TOKEN", getGitlabToken());
//
//				int responseCode = conn.getResponseCode();
//				if ( responseCode == 200 )
//				{
//					BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//					String inputLine;
//					StringBuilder response = new StringBuilder();
//
//					while ((inputLine = in.readLine()) != null)
//					{
//						response.append(inputLine);
//					}
//					in.close();
//
////					System.out.println("Secret Value: " + response.toString());
//
//					// Parse JSON using Jackson
//					ObjectMapper objectMapper = new ObjectMapper();
//					List<GitLabSecret> secrets = objectMapper.readValue(response.toString(), new TypeReference<List<GitLabSecret>>() {});
//
//					// Convert to JSON string and print
////					String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(secrets);
////					System.out.println(jsonOutput);
////					
////					for (GitLabSecret entry : secrets)
////					{
////						System.out.println("   -- " + entry);
////					}
//					
//					return secrets;
//				}
//				else
//				{
//					throw new Exception("Failed to retrieve secret. Response Code: " + responseCode);
////					System.out.println("Failed to retrieve secret. Response Code: " + responseCode);
////					return null;
//				}
//			}
//			catch (Exception ex)
//			{
//				throw new Exception("Failed to retrieve secret. Caught: " + ex);
////				e.printStackTrace();
////				return null;
//			}
//		}

	}

}




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
package com.asetune.tools.ddlgen;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.asetune.sql.JdbcUrlParser;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.ssh.SshTunnelInfo;
import com.asetune.utils.StringUtil;
import com.sybase.ddlgen.DDLGenerator;

public class DdlGenAse extends DdlGen
{
	private static Logger _logger = Logger.getLogger(DdlGenAse.class);

	private String _visibleCommand = "";

	protected DdlGenAse(DbxConnection conn)
	{
		super(conn);
	}

	private String[] getArguments(Type type, String name)
	throws Exception
	{
		DbxConnection conn = getConnection();
		if (conn == null)
			return null;
		ConnectionProp cp = conn.getConnProp();
		JdbcUrlParser p = JdbcUrlParser.parse(cp.getUrl());
		String hostPort = p.getHost() + ":" + p.getPort();

		// If we have a SSH Tunnel enabled, then use the local host/port
		SshTunnelInfo ti = cp.getSshTunnelInfo();
		if (ti != null)
			hostPort = ti.getLocalHost() + ":" + ti.getLocalPort();

		//---------------------------------------------
		// Build up basic argument list
		//
		List<String> argList = new ArrayList<String>();
		argList.add("-U" + cp.getUsername()); 
		argList.add("-P" + cp.getPassword());
		argList.add("-S" + hostPort);
		
//		// Get current database
//		try
//		{
//			String curDbname = conn.getCatalog();
//			if (curDbname != null)
//				argList.add("-D" + curDbname);
//		}
//		catch(SQLException ex) { /* Ignore */ }

		if (getDefaultDbname() != null)
			argList.add("-D" + getDefaultDbname());

		//---------------------------------------------
		// Add more arguments, based on the type
		//
		if (type.equals(Type.DB))
		{
			argList.add("-TDB");
			argList.add("-N" + name);
		}
		else if (type.equals(Type.TABLE))
		{
			argList.add("-TU");
			argList.add("-N" + name);
		}
		else if (type.equals(Type.VIEW))
		{
			argList.add("-TV");
			argList.add("-N" + name);
		}
		else if (type.equals(Type.PROCEDURE))
		{
			argList.add("-TP");
			argList.add("-N" + name);
		}
		else if (type.equals(Type.FUNCTION))
		{
			argList.add("-TP"); // Try to use same as procedure...
			argList.add("-N" + name);
		}
		else if (type.equals(Type.RAW_PARAMS))
		{
			argList.clear();

			// Substitute some variables
			name = name.replace("${username}", cp.getUsername());
			name = name.replace("${password}", cp.getPassword());
			name = name.replace("${hostport}", hostPort);

			String[] args = StringUtil.translateCommandline(name);
			for (String arg : args)
				argList.add(arg);
		}
		else 
		{
			throw new Exception("Unknown type '"+type+"'.");
		}
//		argList.add("-TU"); 
//		argList.add("-Ntitles"); 
//		argList.add("-F%");
//		argList.add("-CENCRYPT_PASSWORD=true");
		
		String extraParams = getExtraParams();
		if (StringUtil.hasValue(extraParams))
		{
			String[] args = StringUtil.translateCommandline(extraParams);
			for (String arg : args)
				argList.add(arg);
		}

		String[] args = argList.toArray(new String[argList.size()]);


		// Set the public command that can be displayed... This hides password...
		String[] args2 = new String[args.length];
		for(int i=0; i<args.length; i++)
		{
			args2[i] = args[i];
			if (args2[i].startsWith("-P"))
				args2[i] = "-P*secret*";
		}
		_visibleCommand = "ddlgen "+StringUtil.toCommaStr(args2, " ");
		
		return args;
	}

	@Override
	public String getCommandForType(Type type, String name)
	{
		try { String[] args = getArguments(type, name); } catch (Exception ignore) { /*ignore*/ }
		return _visibleCommand;
	}
	@Override
	public String getUsedCommand()
	{
		return _visibleCommand;
	}

	@Override
	public String getDdlForType(Type type, String name) throws Exception
	{
		String[] args = getArguments(type, name);

		DDLGenerator ddlGen = new DDLGenerator(args);
		ddlGen.setParams(args);
		
		String version = StringUtils.trim(DDLGenerator.getVersion());
		_logger.info("Using 'ASE' DDLGenerator Version: " + version);

		return ddlGen.generateDDL();
//		DDLGenerator localDDLGenerator = null;
//		DDLGlobalParameters gp = null;
//		try
//		{
//			localDDLGenerator = new DDLGenerator(args);
////			localDDLGenerator._ddlGlobalParameters.setApplicationMode(false);
//			try
//			{
//				gp = (DDLGlobalParameters) FieldUtils.readField(localDDLGenerator, "_ddlGlobalParameters", true);
////    			gp.setApplicationMode(false); // This makes the DDLGenerator to do exit() somewhere...
//			}
//			catch (Throwable t)
//			{
//				t.printStackTrace();
//			}
//			return localDDLGenerator.generateDDL();
//		}
//		catch (DDLBaseException localDDLBaseException)
//		{
//			if (localDDLGenerator == null) 
//			{
//				return "" + new DDLBaseException("DG2", localDDLBaseException, "INTERNAL_ERROR", 1);
//			} 
//			else 
//			{
////				return "" + new DDLBaseException("DG2", localDDLBaseException, "INTERNAL_ERROR", 1, localDDLGenerator._ddlGlobalParameters);
//				return "" + new DDLBaseException("DG2", localDDLBaseException, "INTERNAL_ERROR", 1, gp);
//			}
//		}
	}

	@Override
	public String getDdlForDb(String name)
	throws Exception
	{
		return getDdlForType(Type.DB, name);
	}

	@Override
	public String getDdlForTable(String name) throws Exception
	{
		return getDdlForType(Type.TABLE, name);
	}

	@Override
	public String getDdlForView(String name) throws Exception
	{
		return getDdlForType(Type.VIEW, name);
	}

	@Override
	public String getDdlForProcedure(String name) throws Exception
	{
		return getDdlForType(Type.PROCEDURE, name);
	}

	@Override
	public String getDdlForFunction(String name) throws Exception
	{
		return getDdlForType(Type.FUNCTION, name);
	}

//	private String getDdlgenUsage()
//	{
//		StringBuffer sb = new StringBuffer("\nUsage: ddlgen [Option1][Option2][Option3].... where options are.. \n");
//		sb.append("_____________________________________________________________________________\n");
//		sb.append("\n");
//		sb.append("Option   Parameter             Required   Default        Description         \n");
//		sb.append("_____________________________________________________________________________\n");
//		sb.append("\n");
//		sb.append("  -U     <user name>             Yes      NA             User Name           \n");
//		sb.append("\n");
//		sb.append("  -P     <password>              Yes      NA             Password for Server \n");
//		sb.append("\n");
//		sb.append("  -S     <Server Name |          Yes      NA             Server Name OR      \n");
//		sb.append("          [ssl]:hostName:port >                          [ssl]:<hostName>:<port>\n");
//		sb.append("  -T     <object type>            No      DB             Type of the object  \n");
//		sb.append("                                                         Refer Docs for this \n");
//		sb.append("\n");
//		sb.append("  -N     <object name>            No      Default DB     Object Name         \n");
//		sb.append("\n");
//		sb.append("  -D     <database name>          No      Default DB     Database in which   \n");
//		sb.append("                                          for the login  the object specified\n");
//		sb.append("                                                         in -N option exist  \n");
//		sb.append("\n");
//		sb.append("  -X     <extended object type>   No      NA             Used in conjunction \n");
//		sb.append("                                                         with some -T option \n");
//		sb.append("                                                         Refer Docs for this \n");
//		sb.append("\n");
//		sb.append("  -F     <filter object type>     No      None           Used in conjunction \n");
//		sb.append("                                                         with some -T option \n");
//		sb.append("                                                         Refer Docs for this \n");
//		sb.append("\n");
//		sb.append("  -J     <client charset name>    No      Server's       Character set used  \n");
//		sb.append("                                          character set  by the ddlgen client\n");
//		sb.append("\n");
//		sb.append("  -I     <interfaces file name>   No      Default        Interfaces File to  \n");
//		sb.append("                                                         be used for decoding\n");
//		sb.append("                                                         the Server's Host   \n");
//		sb.append("                                                         Name and Port Number\n");
//		sb.append("\n");
//		sb.append("  -O     <output file name>       No      stdout         File to be used for \n");
//		sb.append("                                                         writing the         \n");
//		sb.append("                                                         DDL Output          \n");
//		sb.append("\n");
//		sb.append("  -E     <error file name>        No      stdout         File to be used for \n");
//		sb.append("                                                         logging the errors  \n");
//		sb.append("\n");
//		sb.append("  -L     <progress log file name> No      No Logging     File to be used for \n");
//		sb.append("                                                         logging the progress\n");
//		sb.append("\n");
//		sb.append("  -V     <version no.>            No      NA             Prints the Version  \n");
//		sb.append("                                                         of the DDLGenerator \n");
//		sb.append("\n");
//		sb.append("_____________________________________________________________________________\n");
//		return sb.toString();
//	}

}

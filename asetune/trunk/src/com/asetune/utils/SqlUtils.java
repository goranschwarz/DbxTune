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

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.io.IOUtils;

//import gudusoft.gsqlparser.EDbVendor;
//import gudusoft.gsqlparser.TGSqlParser;
//
//import gudusoft.gsqlparser.pp.para.GFmtOptFactory;
//import gudusoft.gsqlparser.pp.para.styleenums.TCaseOption;
//import gudusoft.gsqlparser.pp.para.GFmtOpt;
//import gudusoft.gsqlparser.pp.stmtformattor.FormattorFactory;

public class SqlUtils
{
//	/**
//	 * Pretty print a SQL Statement<br>
//	 * Uses a "generic" SQL, if you want a specific SQL Vendor to be used, use: format(String sql, String productName)
//	 * 
//	 * @param sql
//	 * @return A pretty printed version of the string.
//	 * @throws Exception  when the parser has problems parsing
//	 */
//	public static String format(String sql)
//	throws Exception
//	{
//		return format(sql, null);
//	}
//	/**
//	 * Pretty print a SQL Statement
//	 * 
//	 * @param sql
//	 * @param productName the value from Connection.getMetaData().getDatabaseProductName(), if we can't onnor that product we will fallback to generic...
//	 * @return A pretty printed version of the string.
//	 * @throws Exception  when the parser has problems parsing
//	 */
//	public static String format(String sql, String productName)
//	throws Exception
//	{
//		EDbVendor vendor = EDbVendor.dbvgeneric;
//		if      (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_ASE)) vendor = EDbVendor.dbvsybase;
//		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_ASA)) vendor = EDbVendor.dbvsybase;
//		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_IQ))  vendor = EDbVendor.dbvsybase;
//		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MYSQL))      vendor = EDbVendor.dbvmysql;
//		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_ORACLE))     vendor = EDbVendor.dbvoracle;
//		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DB2_UX))     vendor = EDbVendor.dbvdb2;
//		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DB2_ZOS))    vendor = EDbVendor.dbvdb2;
//		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_POSTGRES))   vendor = EDbVendor.dbvpostgresql;
//		
//System.out.println("formatSql using productName='"+productName+"', vendor='"+vendor+"'.");
//
//		TGSqlParser sqlparser = new TGSqlParser(vendor);
//		sqlparser.sqltext = sql;
//
//		int ret = sqlparser.parse();
//		if (ret == 0)
//		{
//			GFmtOpt option = GFmtOptFactory.newInstance();
//			option.caseFuncname = TCaseOption.CoNoChange;
//			
//			String result = FormattorFactory.pp(sqlparser, option);
//			return result;
//		}
//		else
//		{
//			throw new Exception("Parser promlem: " + sqlparser.getErrormessage());
//		}
//	}

	public static final String PROPKEY_SQL_VENDOR = "sql.vendor";
	

	// Not really used for the moment, but it might vert well be in the future.
	private static String _prettyPrintDatabaseProductName = "generic";

	/**
	 * Set the desired format for the prettyPrint formating
	 * @param productName Connection.getMetaData().getDatabaseProductName()
	 * 
	 * For the moment this is called from the connection dialog on a successful connect. (but it still "needs" to be reset on connection closed, if we need to change the vendor type)
	 */
	public static void setPrettyPrintDatabaseProductName(String productName)
	{
		if (StringUtil.isNullOrBlank(productName))
			productName = "generic";
		_prettyPrintDatabaseProductName = productName;
	}
	/**
	 * Get the desired format for the prettyPrint formating
	 */
	public static String getPrettyPrintDatabaseProductName()
	{
		return _prettyPrintDatabaseProductName;
	}
	
	public static String format(String sql)
	throws Exception
	{
		String dbxTuneHome = System.getenv("DBXTUNE_HOME");
		if (dbxTuneHome == null) dbxTuneHome = System.getProperties().getProperty("DBXTUNE_HOME");
		if (dbxTuneHome == null) dbxTuneHome = System.getenv("APPL_HOME");
		if (dbxTuneHome == null) dbxTuneHome = System.getProperties().getProperty("APPL_HOME");
		
		String progDir      = System.getProperties().getProperty("dbxtune.sql.pretty.print.dir",      new File(dbxTuneHome+"/resources/bin").toString());
		String progCmd      = System.getProperties().getProperty("dbxtune.sql.pretty.print.cmd",      "SqlFormatter");
		String progSwitches = System.getProperties().getProperty("dbxtune.sql.pretty.print.switches", "/ae");

		String progExec = progDir + File.separator + progCmd;
//		progExec = "C:\\projects\\AseTune\\resources\\bin\\SqlFormatter.exe";

		ProcessBuilder pb = new ProcessBuilder(progExec, progSwitches);

		// Change environment, this could be usable if the 'dbxtune.sql.pretty.print.cmd' is a shell script or a bat file
		Map<String, String> env = pb.environment();
		env.put("SQL_UTILS_DATABASE_PRODUCT_NAME", _prettyPrintDatabaseProductName);
		
		// Set current working directory to DBXTUNE_HOME
		pb.directory(new File(progDir));
		
//System.out.println("DBXTUNE_HOME: "+dbxTuneHome);
//System.out.println("progExec: "+progExec);
//System.out.println("xxx: "+pb.toString());
//System.out.println("xxx.dir: "+pb.directory());
//System.out.println("xxx.cmd: "+pb.command());

		// Start the process
		Process p = pb.start();

		// Write to the command + close the stream
		OutputStream stdin = p.getOutputStream();
		stdin.write(sql.getBytes(StandardCharsets.UTF_8));
		stdin.close();

		// Read ouput
		String stdout = StringUtil.removeLastNewLine(IOUtils.toString(p.getInputStream()));
		String stderr = StringUtil.removeLastNewLine(IOUtils.toString(p.getErrorStream()));

		if (StringUtil.hasValue(stderr) && StringUtil.isNullOrBlank(stdout))
		{
			throw new Exception("Problem parsing SQL: "+stderr);
		}

		if (sql.endsWith("\n"))
			return stdout;
		else
			return StringUtil.removeLastNewLine(stdout);
	}
}

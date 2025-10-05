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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.AseSqlScript;
import com.dbxtune.utils.TimeUtils;

public class AseStmntCacheMonSysXxx
{
	public static void main(String[] args)
	{
		// DBA_1_ASE
		String urlMonitor = "jdbc:sybase:Tds:dba-1-ase:5000/master";  // Context
		String urlSqlLoad = "jdbc:sybase:Tds:dba-1-ase:5000/PML";
//		String url    = "jdbc:sybase:Tds:dba-1-ase:5000/PML?DYNAMIC_PREPARE=false";

		// PROD_A1_ASE >>>> WARNING >>>> It CLEARS the Statement Cache
//		String urlMonitor = "jdbc:sybase:Tds:prod-a1-ase:5000/master";  // Context
//		String urlSqlLoad = "jdbc:sybase:Tds:prod-a1-ase:5000/PML";
////		String url    = "jdbc:sybase:Tds:prod-a1-ase:5000/PML?DYNAMIC_PREPARE=false";

		String user   = "sa";
		String passwd = "sjhyr564s_Wq26kl73";

		int spidSqlLoad = -1;

		try
		{
			System.out.println("\n\n\n#######################################################\n>>>> connect");
			DbxConnection connMonitor = connect(urlMonitor, user, passwd);
			DbxConnection connSqlLoad = connect(urlSqlLoad, user, passwd);
			
			spidSqlLoad = getSpid(connSqlLoad);
			
			
			
			System.out.println("\n\n\n#######################################################\n>>>> clearStatementCache");
			clearStatementCache(connMonitor);

			System.out.println("\n\n\n#######################################################\n>>>> testPreparedStatement");
			testPreparedStatement(connMonitor, connSqlLoad, spidSqlLoad);


			
//			System.out.println("\n\n\n#######################################################\n>>>> clearStatementCache");
//			clearStatementCache(connMonitor);
//
//			System.out.println("\n\n\n#######################################################\n>>>> testLanguageStatement -- USE stmntCache");
//			testLanguageStatement(connMonitor, connSqlLoad, spidSqlLoad, true);
//
//
//			
//			System.out.println("\n\n\n#######################################################\n>>>> clearStatementCache");
//			clearStatementCache(connMonitor);
//
//			System.out.println("\n\n\n#######################################################\n>>>> testLanguageStatement -- do NOT use stmntCache");
//			testLanguageStatement(connMonitor, connSqlLoad, spidSqlLoad, false);
//
//
//			
//			System.out.println("\n\n\n#######################################################\n>>>> clearStatementCache");
//			clearStatementCache(connMonitor);
//
//			System.out.println("\n\n\n#######################################################\n>>>> testExecProcedure");
//			testExecProcedure(connMonitor, connSqlLoad, spidSqlLoad);


			
			System.out.println("\n\n\n#######################################################\n>>>> disconnect");
			connMonitor.close();
			connSqlLoad.close();

			System.out.println("\n\n\n#######################################################\n<<<< EXIT");
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
		}
	}

	private static int getSpid(DbxConnection conn)
	throws SQLException
	{
		int spid = -1;
		String sql = "select @@spid";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				spid = rs.getInt(1);
			}
		}
		
		return spid;
	}


	private static DbxConnection connect(String url, String user, String passwd)
	throws SQLException
	{
		Connection conn = DriverManager.getConnection(url, user, passwd);
		DbxConnection dbxConn = DbxConnection.createDbxConnection(conn);
		
		return dbxConn;
	}

	private static void clearStatementCache(DbxConnection conn)
	throws SQLException
	{
		String sql = "dbcc purgesqlcache";

		System.out.println("    >>>> Clearing Statement Cache -- " + sql);

		try (Statement stmnt = conn.createStatement() )
		{
			stmnt.executeUpdate(sql);				
		}
	}

	private static void clearMonTables(DbxConnection conn)
	throws SQLException
	{
		String sql;

		System.out.println("    >>>> Clearing monSysXxxTables");

		sql = "SELECT * FROM master.dbo.monSysSQLText";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
			}
		}

		sql = "SELECT *	FROM master.dbo.monSysStatement";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
			}
		}
		
	}

	private static void printMonTables(DbxConnection conn, int spidSqlLoad)
	throws SQLException
	{
		String sql;
		int skipSqlTextWith_SequenceInBatch = 2; // set to: Integer.MAX_VALUE if you want to see ALL SQL Text

		System.out.println("    >>>> Getting monSysXxxTables");

		sql = ""
				+ "SELECT "
				+ "    * "
				+ "FROM master.dbo.monSysSQLText "
				+ "WHERE SPID = " + spidSqlLoad 
				+ "  AND SequenceInBatch <= " + skipSqlTextWith_SequenceInBatch + " "
//				+ "ORDER BY SPID, BatchID, SequenceInBatch"
				;
		System.out.println(ResultSetTableModel.executeQuery(conn, sql, "").toAsciiTableString());

//		sql = "select * from master.dbo.monSysStatement where SPID = " + spidSqlLoad + "";
		sql = ""
				+ "SELECT "
				+ "    ProcName = CASE WHEN SsqlId > 0 THEN isnull(object_name(SsqlId,2), '*##'+right('0000000000'+convert(varchar(20),SsqlId),10)+'_'+right('0000000000'+convert(varchar(20),HashKey),10)+'##*') ELSE coalesce(object_name(ProcedureID,DBID), object_name(ProcedureID,2), object_name(ProcedureID,db_id('sybsystemprocs'))) END "
				+ "   ,BatchID"
				+ "   ,SsqlId"
				+ "   ,HashKey"
				+ "   ,ProcedureID"
				+ "   ,ContextID"
				+ "   ,LineNumber"
				+ "   ,QueryOptimizationTime"
				+ "   ,LogicalReads"
				+ "   ,SEPARATOR = '>>> ALL COLUMNS >>>' "
				+ "   ,* "
				+ "FROM master.dbo.monSysStatement "
				+ "WHERE SPID = " + spidSqlLoad + " "
//				+ "ORDER BY SPID, BatchID, LineNumber"
				;
		
		System.out.println(ResultSetTableModel.executeQuery(conn, sql, "").toAsciiTableString());
	}
	

	
	private static void testPreparedStatement(DbxConnection connMonitor, DbxConnection connSqlLoad, int spidSqlLoad)
	throws SQLException
	{
		String sql;
		String param;
		long startTime;

		// Sends PreparedStatement with 1 parameter
		clearMonTables(connMonitor);

		param = "%Execute%IT%";
		sql = getSql(true, param);

//		System.out.println("    >>>> Prepared Stmnt: FIRST: sql='" + sql + "'. PARAM[1]='" + param + "'");
		System.out.println("    >>>> Prepared Stmnt: FIRST: sql='...'. PARAM[1]='" + param + "'");

		startTime = System.currentTimeMillis();
		try (PreparedStatement stmnt = connSqlLoad.prepareStatement(sql); )
		{
			stmnt.setString(1, param);
			try (ResultSet rs = stmnt.executeQuery(); )
			{
//				ResultSetTableModel rstm = new ResultSetTableModel(rs, sql);
//				System.out.println(rstm.toAsciiTableString());
				int rowc = 0;
				while(rs.next())
				{
					rowc++;
				}
				System.out.println("    #### ROW COUNT: " + rowc);
				printExecTime(startTime);
			}
		}
		printMonTables(connMonitor, spidSqlLoad);
		
		// Reuse Above PreparedStatement with 1 parameter
		param = "%another-value%";
//		System.out.println("    >>>> Prepared Stmnt: SECOND: sql='" + sql + "'. PARAM[1]='" + param + "'");
		System.out.println("    >>>> Prepared Stmnt: SECOND: sql='...'. PARAM[1]='" + param + "'");

		startTime = System.currentTimeMillis();
		try (PreparedStatement stmnt = connSqlLoad.prepareStatement(sql); )
		{
			stmnt.setString(1, param);
			try (ResultSet rs = stmnt.executeQuery(); )
			{
//				ResultSetTableModel rstm = new ResultSetTableModel(rs, sql);
//				System.out.println(rstm.toAsciiTableString());
				int rowc = 0;
				while(rs.next())
				{
					rowc++;
				}
				System.out.println("    #### ROW COUNT: " + rowc);
				printExecTime(startTime);
			}
		}
		printMonTables(connMonitor, spidSqlLoad);
	}



	private static void testLanguageStatement(DbxConnection connMonitor, DbxConnection connSqlLoad, int spidSqlLoad, boolean useStatementCacheIfAvailable)
	throws SQLException
	{
		String sql;
		String param;
		long startTime;
		
		if (useStatementCacheIfAvailable)
		{
			sql = ""
					+ "set statement_cache   on "
					+ "set literal_autoparam on "
					;
		}
		else
		{
			sql = ""
					+ "set statement_cache   off "
					+ "set literal_autoparam off "
					;
		}
		// Execute Above SETTING
		try (Statement stmnt = connSqlLoad.createStatement() )
		{
			stmnt.executeUpdate(sql);				
		}

		// Sends PreparedStatement with 1 parameter
		clearMonTables(connMonitor);

		param = "%Execute%IT%";
		sql = getSql(false, param);
//		sql = "select rowc = count(*) from PML.dbo.Foretag where 1=1 and Foretag like'" + param + "'";
//		System.out.println("    >>>> Language Stmnt: FIRST: sql='" + sql + "'. PARAM[1]='" + param + "'");
		System.out.println("    >>>> Language Stmnt: FIRST: sql='...'. PARAM[1]='" + param + "'");

		startTime = System.currentTimeMillis();
		try (Statement stmnt = connSqlLoad.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
		{
//			ResultSetTableModel rstm = new ResultSetTableModel(rs, sql);
//			System.out.println(rstm.toAsciiTableString());
			int rowc = 0;
			while(rs.next())
			{
				rowc++;
			}
			System.out.println("    #### ROW COUNT: " + rowc);
			printExecTime(startTime);
		}
		printMonTables(connMonitor, spidSqlLoad);
		
		// Reuse Above PreparedStatement with 1 parameter
//		param = "%another-value%";
		param = "%Execute%IT%";
		sql = getSql(false, param);
//		sql = "select rowc = count(*) from PML.dbo.Foretag where 1=1 and Foretag like'" + param + "'";
//		System.out.println("    >>>> Language Stmnt: SECOND: sql='" + sql + "'. PARAM[1]='" + param + "'");
		System.out.println("    >>>> Language Stmnt: SECOND: sql='...'. PARAM[1]='" + param + "'");

		startTime = System.currentTimeMillis();
		try (Statement stmnt = connSqlLoad.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
		{
//			ResultSetTableModel rstm = new ResultSetTableModel(rs, sql);
//			System.out.println(rstm.toAsciiTableString());
			int rowc = 0;
			while(rs.next())
			{
				rowc++;
			}
			System.out.println("    #### ROW COUNT: " + rowc);
			printExecTime(startTime);
		}
		printMonTables(connMonitor, spidSqlLoad);
	}

	private static void testExecProcedure(DbxConnection connMonitor, DbxConnection connSqlLoad, int spidSqlLoad)
	throws SQLException
	{
		createDummyProcs(connMonitor);
		
		clearMonTables(connMonitor);

//		System.out.println("    >>>> Prepared Stmnt: FIRST: sql='" + sql + "'. PARAM[1]='" + param + "'");
		System.out.println("    >>>> Procedure EXEC: ");

		executeDummyProcs(connSqlLoad);

		printMonTables(connMonitor, spidSqlLoad);

		dropDummyProcs(connMonitor);
	}




	private static void printExecTime(long startTime)
	{
		long durationMs = System.currentTimeMillis() - startTime;
		System.out.println("    #### EXEC TIME: " + TimeUtils.msToTimeStrDHMSms(durationMs));
	}


	private static String getSql(boolean isPreparedStmnt, String param)
	{
		String sql = ""
		    + "SELECT \n"
		    + "    aq.* \n"
		    + "    ,ec_company.\"xID\" AS ec_companyId, \n"
		    + "ec_company.\"Foretag\" AS ec_companyName, \n"
		    + "ec_company.\"Orgnr\" AS ec_companyRegistrationNumber, \n"
		    + "ec_companyBroker.\"xID\" AS ec_companyBrokerId, \n"
		    + "ec_companyAdmin.\"xID\" AS ec_companyAdminId, \n"
		    + "ec_companyBroker.\"xKontor\" AS ec_brokerOffice, \n"
		    + "ec_companyAdmin.\"xKontor\" AS ec_adminOffice, \n"
		    + "ec_companyBroker.\"xBrokerTeam\" AS ec_brokerTeam, \n"
		    + "ec_companyAdmin.\"xBrokerTeam\" AS ec_adminTeam, \n"
		    + "ec_companyParameterTeam.\"value\" AS ec_teamAccess, \n"
		    + "ec_companyParameterOffice.\"value\" AS ec_officeAccess, \n"
		    + "ec_hcc.\"orgnr\" AS ec_heartbeatChangeoverOrgnr, \n"
		    + "eemp_employment.\"xForetag\" as eemp_employmentCompanyId, \n"
		    + "eemp_employment.\"DateQuit\" as eemp_employmentDateQuit, \n"
		    + "e_employee.\"xID\" AS e_employeeId, \n"
		    + "e_employee.\"Personnummer_2\" AS e_employeeNationalRegistrationNumber, \n"
		    + "e_employee.\"xFORETA\" AS e_employeeCompanyId, \n"
		    + "e_employee.\"xMaklar\" AS e_employeeBrokerId, \n"
		    + "e_employee.\"xAnsvassistent\" AS e_employeeAdminId, \n"
		    + "e_employeeBroker.\"xMaklare\" AS e_employeePrivateBrokerId, \n"
		    + "e_employeePersonParameter.\"value\" AS e_protectedIdentityValue,c_company.\"xID\" AS c_companyId, \n"
		    + "c_company.\"Foretag\" AS c_companyName, \n"
		    + "c_company.\"Orgnr\" AS c_companyRegistrationNumber, \n"
		    + "c_companyBroker.\"xID\" AS c_companyBrokerId, \n"
		    + "c_companyAdmin.\"xID\" AS c_companyAdminId, \n"
		    + "c_companyBroker.\"xKontor\" AS c_brokerOffice, \n"
		    + "c_companyAdmin.\"xKontor\" AS c_adminOffice, \n"
		    + "c_companyBroker.\"xBrokerTeam\" AS c_brokerTeam, \n"
		    + "c_companyAdmin.\"xBrokerTeam\" AS c_adminTeam, \n"
		    + "c_companyParameterTeam.\"value\" AS c_teamAccess, \n"
		    + "c_companyParameterOffice.\"value\" AS c_officeAccess, \n"
		    + "c_hcc.\"orgnr\" AS c_heartbeatChangeoverOrgnr \n"
		    + "FROM \n"
		    + "  ( \n"
		    + "    SELECT \n"
		    + "        bq.*, \n"
		    + "        bq.base_employee_company_id AS employee_company_id, \n"
		    + "null AS eemp_id \n"
		    + "    FROM \n"
		    + "        ( \n"
		    + "        SELECT \n"
		    + "a.\"xID\"             AS te_xId, \n"
		    + "a.\"Atgardas_senast\" AS te_atgardasSenast, \n"
		    + "a.\"Atgardat\"        AS te_atgardat, \n"
		    + "a.\"Arenderubrik\"    AS te_arenderubrik, \n"
		    + "a.\"xArendenTyp\"     AS te_xarendentyp, \n"
		    + "a.\"Person\"          AS te_person, \n"
		    + "a.\"xModified\"       AS te_xModified, \n"
		    + "a.\"xModified_by\"    AS te_xModifiedBy, \n"
		    + "an.\"Fornamn\"        AS te_fornamn, \n"
		    + "an.\"Efternamn\"      AS te_efternamn, \n"
		    + "pep.\"value\"         AS te_protectedPerson, \n"
		    + "a.\"xID\"             AS taie_arendeId, \n"
		    + "a.\"xANSTAL\"         AS taie_xAnstal, \n"
		    + "a.\"xArendenTyp\"     AS taie_xArendenTyp, \n"
		    + "an.\"Personnummer_2\" AS taie_mPersnr, \n"
		    + "a.\"Atgardat\"        AS taie_atgardat, \n"
		    + "m.\"xID\"             AS bre_xId, \n"
		    + "m.\"Namn\"            AS bre_namn, \n"
		    + "mak.\"xARENDE\"       AS bre_xarende, \n"
		    + "bg.\"xID\"            AS bre_groupXid, \n"
		    + "bg.\"name\"           AS bre_groupName, \n"
		    + "bgt.\"xArende\"       AS bre_groupXArende, \n"
		    + "bgb.\"xMaklare\" \n"
		    + " \n"
		    + ", \n"
		    + "an.\"xID\" AS base_employee_id, \n"
		    + "an.\"xFORETA\"  AS base_employee_company_id, \n"
		    + "a.\"xFORETA\" AS base_company_id \n"
		    + "FROM \n"
		    + "\"arenden\" a \n"
		    + "INNER JOIN ( \n"
		    + "SELECT \"xARENDE\" AS xArende FROM \"xMAKARND\" WHERE XMAKLAR=-237 \n"
		    + "    UNION \n"
		    + "SELECT bgt.\"xArende\" AS xArende FROM \"BrokergroupTask\" bgt \n"
		    + "    INNER JOIN \"Brokergroup\" b on b.\"xID\" = bgt.\"xBrokerGroup\" \n"
		    + "    INNER JOIN \"BrokerBrokergroup\" bb on bgt.\"xBrokerGroup\" = bb.\"xBrokerGroup\" \n"
		    + "    WHERE bb.\"xMaklare\"=-237 ) AS ga ON a.\"xID\" = ga.xArende \n"
		    + " \n"
		    + "LEFT JOIN \"xMAKARND\" mak              ON mak.\"xARENDE\" = a.\"xID\" \n"
		    + "LEFT JOIN \"Maklare\" m                 ON m.\"xID\" = mak.XMAKLAR \n"
		    + "LEFT JOIN \"Anstallda\" an              ON an.\"xID\" = a.\"xANSTAL\" \n"
		    + "LEFT JOIN \"BrokergroupTask\" bgtFilter ON bgtFilter.\"xArende\"= a.\"xID\" \n"
		    + "LEFT JOIN \"BrokergroupTask\" bgt       ON bgt.\"xArende\"= a.\"xID\" \n"
		    + "LEFT JOIN \"Brokergroup\" bg            ON bg.\"xID\" = bgt.\"xBrokerGroup\" \n"
		    + "LEFT JOIN \"BrokerBrokergroup\" bgb     ON bgb.\"xBrokerGroup\" = bg.\"xID\" \n"
		    + "LEFT JOIN \"PersonParameter\" pep       ON pep.\"xPerson\"=an.\"xID\" and pep.\"xParameterType\" = 9 \n"
		    + " \n"
		    + "WHERE  (a.\"Atgardat\" = 'n' OR a.\"Atgardat\" IS NULL) \n"
		    ;
		    
		    if (isPreparedStmnt)
		    	sql += "AND  'xxx' = ? \n";
		    else
		    	sql += "AND  'xxx' = '" + param + "' \n";


		sql += ""
		    + "AND ( a.\"Atgardas_senast\" IS NULL OR a.\"Atgardas_senast\" = '0000-00-00' OR a.\"Atgardas_senast\" <= '2025-08-19') \n"
		    + " \n"
		    + " \n"
		    + "        ) AS bq \n"
		    + " \n"
		    + "    UNION \n"
		    + "    SELECT \n"
		    + "        bq.*, \n"
		    + "        eemp_employment.\"xForetag\" AS employee_company_id, \n"
		    + "eemp_employment.\"xID\" AS eemp_id \n"
		    + "    FROM \n"
		    + "        ( \n"
		    + "        SELECT \n"
		    + "a.\"xID\"             AS te_xId, \n"
		    + "a.\"Atgardas_senast\" AS te_atgardasSenast, \n"
		    + "a.\"Atgardat\"        AS te_atgardat, \n"
		    + "a.\"Arenderubrik\"    AS te_arenderubrik, \n"
		    + "a.\"xArendenTyp\"     AS te_xarendentyp, \n"
		    + "a.\"Person\"          AS te_person, \n"
		    + "a.\"xModified\"       AS te_xModified, \n"
		    + "a.\"xModified_by\"    AS te_xModifiedBy, \n"
		    + "an.\"Fornamn\"        AS te_fornamn, \n"
		    + "an.\"Efternamn\"      AS te_efternamn, \n"
		    + "pep.\"value\"         AS te_protectedPerson, \n"
		    + "a.\"xID\"             AS taie_arendeId, \n"
		    + "a.\"xANSTAL\"         AS taie_xAnstal, \n"
		    + "a.\"xArendenTyp\"     AS taie_xArendenTyp, \n"
		    + "an.\"Personnummer_2\" AS taie_mPersnr, \n"
		    + "a.\"Atgardat\"        AS taie_atgardat, \n"
		    + "m.\"xID\"             AS bre_xId, \n"
		    + "m.\"Namn\"            AS bre_namn, \n"
		    + "mak.\"xARENDE\"       AS bre_xarende, \n"
		    + "bg.\"xID\"            AS bre_groupXid, \n"
		    + "bg.\"name\"           AS bre_groupName, \n"
		    + "bgt.\"xArende\"       AS bre_groupXArende, \n"
		    + "bgb.\"xMaklare\" \n"
		    + " \n"
		    + ", \n"
		    + "an.\"xID\" AS base_employee_id, \n"
		    + "an.\"xFORETA\"  AS base_employee_company_id, \n"
		    + "a.\"xFORETA\" AS base_company_id \n"
		    + "FROM \n"
		    + "\"arenden\" a \n"
		    + "INNER JOIN ( \n"
		    + "SELECT \"xARENDE\" AS xArende FROM \"xMAKARND\" WHERE XMAKLAR=-237 \n"
		    + "    UNION \n"
		    + "SELECT bgt.\"xArende\" AS xArende FROM \"BrokergroupTask\" bgt \n"
		    + "    INNER JOIN \"Brokergroup\" b on b.\"xID\" = bgt.\"xBrokerGroup\" \n"
		    + "    INNER JOIN \"BrokerBrokergroup\" bb on bgt.\"xBrokerGroup\" = bb.\"xBrokerGroup\" \n"
		    + "    WHERE bb.\"xMaklare\"=-237 ) AS ga ON a.\"xID\" = ga.xArende \n"
		    + " \n"
		    + "LEFT JOIN \"xMAKARND\" mak              ON mak.\"xARENDE\" = a.\"xID\" \n"
		    + "LEFT JOIN \"Maklare\" m                 ON m.\"xID\" = mak.XMAKLAR \n"
		    + "LEFT JOIN \"Anstallda\" an              ON an.\"xID\" = a.\"xANSTAL\" \n"
		    + "LEFT JOIN \"BrokergroupTask\" bgtFilter ON bgtFilter.\"xArende\"= a.\"xID\" \n"
		    + "LEFT JOIN \"BrokergroupTask\" bgt       ON bgt.\"xArende\"= a.\"xID\" \n"
		    + "LEFT JOIN \"Brokergroup\" bg            ON bg.\"xID\" = bgt.\"xBrokerGroup\" \n"
		    + "LEFT JOIN \"BrokerBrokergroup\" bgb     ON bgb.\"xBrokerGroup\" = bg.\"xID\" \n"
		    + "LEFT JOIN \"PersonParameter\" pep       ON pep.\"xPerson\"=an.\"xID\" and pep.\"xParameterType\" = 9 \n"
		    + " \n"
		    + "WHERE  (a.\"Atgardat\" = 'n' OR a.\"Atgardat\" IS NULL) \n"
		    + "AND ( a.\"Atgardas_senast\" IS NULL OR a.\"Atgardas_senast\" = '0000-00-00' OR a.\"Atgardas_senast\" <= '2025-08-19') \n"
		    + " \n"
		    + " \n"
		    + "        ) AS bq \n"
		    + "        LEFT JOIN \"EmploymentExtended\" eemp_employment ON eemp_employment.\"xAnstallda\" = base_employee_id \n"
		    + "  ) AS aq \n"
		    + "  LEFT JOIN \"Anstallda\" e_employee ON e_employee.\"xID\" = aq.base_employee_id \n"
		    + "LEFT JOIN \"Foretag\" ec_company ON ec_company.\"xID\" = aq.employee_company_id \n"
		    + "LEFT JOIN \"EmploymentExtended\" eemp_employment ON eemp_employment.\"xID\" = aq.eemp_id \n"
		    + "LEFT JOIN \"Foretag\" c_company ON c_company.\"xID\" = aq.base_company_id \n"
		    + " \n"
		    + "LEFT JOIN \"anstalldaMaklare\" e_employeeBroker ON e_employeeBroker.\"xAnstallda\" = e_employee.\"xID\" \n"
		    + "LEFT JOIN \"PersonParameter\" e_employeePersonParameter ON e_employee.\"xID\" = e_employeePersonParameter.\"xPerson\" \n"
		    + "                                                      AND e_employeePersonParameter.\"xParameterType\" = 9 \n"
		    + "LEFT JOIN \"Maklare\" ec_companyBroker on ec_company.\"xMaklar\" = ec_companyBroker.\"xID\" \n"
		    + "LEFT JOIN \"Maklare\" ec_companyAdmin on ec_company.\"xansvassistent\" = ec_companyAdmin.\"xID\" \n"
		    + "LEFT JOIN \"CompanyParameter\" ec_companyParameterTeam \n"
		    + "    ON ec_companyParameterTeam.\"xCompany\"=ec_company.\"xID\" \n"
		    + "        AND ec_companyParameterTeam.\"xParameterType\" = 12 \n"
		    + "LEFT JOIN \"CompanyParameter\" ec_companyParameterOffice \n"
		    + "    ON ec_companyParameterOffice.\"xCompany\"=ec_company.\"xID\" \n"
		    + "        AND ec_companyParameterOffice.\"xParameterType\" = 13 \n"
		    + "LEFT JOIN (SELECT DISTINCT \"orgnr\" FROM \"HeartbeatCompanyChangeover\") ec_hcc \n"
		    + "    ON ec_hcc.\"orgnr\" = ec_company.\"Orgnr\" \n"
		    + " \n"
		    + "LEFT JOIN \"Maklare\" c_companyBroker on c_company.\"xMaklar\" = c_companyBroker.\"xID\" \n"
		    + "LEFT JOIN \"Maklare\" c_companyAdmin on c_company.\"xansvassistent\" = c_companyAdmin.\"xID\" \n"
		    + "LEFT JOIN \"CompanyParameter\" c_companyParameterTeam \n"
		    + "    ON c_companyParameterTeam.\"xCompany\"=c_company.\"xID\" \n"
		    + "        AND c_companyParameterTeam.\"xParameterType\" = 12 \n"
		    + "LEFT JOIN \"CompanyParameter\" c_companyParameterOffice \n"
		    + "    ON c_companyParameterOffice.\"xCompany\"=c_company.\"xID\" \n"
		    + "        AND c_companyParameterOffice.\"xParameterType\" = 13 \n"
		    + "LEFT JOIN (SELECT DISTINCT \"orgnr\" FROM \"HeartbeatCompanyChangeover\") c_hcc \n"
		    + "    ON c_hcc.\"orgnr\" = c_company.\"Orgnr\" \n"
		    + "";
	
		return sql;
	}

	private static void executeDummyProcs(DbxConnection conn)
	throws SQLException
	{
		String sql = "exec tempdb.dbo.test_dummy_proc_1";
		
		try (AseSqlScript script = new AseSqlScript(conn, 0))
		{
			script.setRsAsAsciiTable(true);

			String result = script.executeSqlStr(sql, true);
			System.out.println("    >>>> EXECUTE PROC RESULTS: " + result);
		}
	}

	private static void dropDummyProcs(DbxConnection conn)
	throws SQLException
	{
		String sql = ""
				+ "use tempdb \n"
				+ "go \n"
				
				+ "DROP PROC test_dummy_proc_1 \n"
				+ "DROP PROC test_dummy_proc_1_inner \n"
				+ "go \n";
		
		try (AseSqlScript script = new AseSqlScript(conn, 0))
		{
			script.setRsAsAsciiTable(true);

			String result = script.executeSqlStr(sql, true);
			System.out.println("    >>>> DROP PROC RESULTS: " + result);
		}
	}

	private static void createDummyProcs(DbxConnection conn)
	throws SQLException
	{
		String sql = ""
				+ "use tempdb \n"
				+ "go \n"

				+ "CREATE PROC test_dummy_proc_1_inner \n"
				+ "AS \n" 
				+ "BEGIN \n"
				+ "    print 'START: tempdb.dbo.test_dummy_proc_1_inner' \n" 
				+ "    select sysobjectsCount = count(*) from tempdb.dbo.sysobjects \n"
				+ "    waitfor delay '00:00:01' \n"
				+ "    print 'END:   tempdb.dbo.test_dummy_proc_1_inner' \n" 
				+ "END \n"
				+ "go \n" 

				
				+ "CREATE PROC test_dummy_proc_1 \n"
				+ "AS \n" 
				+ "BEGIN \n" 
				+ "    print 'START: tempdb.dbo.test_dummy_proc_1' \n" 
				+ "    EXEC test_dummy_proc_1_inner \n"
				+ "    select sysindexesCount = count(*) from tempdb.dbo.sysindexes \n"
				+ "    select LargeTableCount = count(*) from PML.dbo.Loner WHERE Skuggpremie > -1 \n"
				+ "    exec('select LargeTableCount = count(*) from PML.dbo.Loner WHERE Skuggpremie > -1') \n"
				+ "    print 'END:   tempdb.dbo.test_dummy_proc_1' \n" 
				+ "END \n" 
				+ "go \n" 
				;

		try (AseSqlScript script = new AseSqlScript(conn, 0))
		{
			script.setRsAsAsciiTable(true);

			String result = script.executeSqlStr(sql, true);
			System.out.println("    >>>> DROP PROC RESULTS: " + result);
		}
	}
	
}

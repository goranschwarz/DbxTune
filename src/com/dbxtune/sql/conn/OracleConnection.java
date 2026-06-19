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
package com.dbxtune.sql.conn;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.gui.swing.WaitForExecDialog;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.sql.conn.info.DbxConnectionStateInfo;
import com.dbxtune.sql.conn.info.DbxConnectionStateInfoGenericJdbc;
import com.dbxtune.ui.autocomplete.completions.ProcedureInfo;
import com.dbxtune.ui.autocomplete.completions.TableExtraInfo;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

public class OracleConnection 
extends DbxConnection
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	// Cached values
	private List<String> _getActiveServerRolesOrPermissions = null;

	@Override
	public void clearCachedValues()
	{
		_getActiveServerRolesOrPermissions = null;
		super.clearCachedValues();
	}
	
	public OracleConnection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = true;
//System.out.println("constructor::OracleConnection(conn): conn="+conn);
	}

	@Override
	public DbmsVersionInfo createDbmsVersionInfo()
	{
		return new DbmsVersionInfoOracle(this);
	}

	public List<ProcedureInfo> getPackageProcedures(WaitForExecDialog waitDialog, String catalogName, String schemaName, String procName)
	{
		final String stateMsg = "Getting Oracle Procedures in PACKAGES.";
		if (waitDialog != null)
			waitDialog.setState(stateMsg);

		ArrayList<ProcedureInfo> procInfoList = new ArrayList<ProcedureInfo>();

		String sql = "some select statment...";
		try
		{
			Statement stmnt = this.createStatement();
//			ResultSet rs = dbmd.getProcedures(catalogName, schemaName, procName);
			ResultSet rs = stmnt.executeQuery(sql);

			int counter = 0;
//			while(rs.next())
//			{
//				counter++;
//				if ( (counter % 100) == 0 )
//					waitDialog.setState(stateMsg + " (Fetch count "+counter+")");
//
//				ProcedureInfo pi = new ProcedureInfo();
//				pi._procCat          = rs.getString("PROCEDURE_CAT");
//				pi._procSchema       = rs.getString("PROCEDURE_SCHEM");
//				pi._procName         = rs.getString("PROCEDURE_NAME");
//				pi._procType         = decodeProcedureType(rs.getInt("PROCEDURE_TYPE"));
//				pi._procRemark       = rs.getString("REMARKS");
////				pi._procSpecificName = rs.getString("SPECIFIC_NAME"); //in HANA = not there...
//
//System.out.println("refreshCompletionFor ORACLE PACKAGE Procedures() ADD: pi="+pi);
//				// add schemas... this is a Set so duplicates is ignored
////				_schemaNames.add(pi._procSchema);
//
//				procInfoList.add(pi);
//				
//				if (waitDialog != null && waitDialog.wasCancelPressed())
//					return procInfoList;
//			}
			rs.close();
			
		}
		catch (SQLException ex)
		{
			
		}

		// TODO Auto-generated method stub
		return procInfoList;
	}

	@Override
	public boolean isInTransaction()
	throws SQLException
	{
		String sql = 
			  "select "
			+ "CASE "
			+ "  WHEN dbms_transaction.local_transaction_id IS NULL THEN 0 "
			+ "  ELSE 1 "
			+ "END FROM DUAL";

		boolean retVal = false;
		
		Statement stmt = createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		while (rs.next())
		{
			retVal = rs.getInt(1) == 1;
		}
		rs.close();
		stmt.close();

		return retVal;
	}

	@Override
	public DbxConnectionStateInfo refreshConnectionStateInfo()
	{
		DbxConnectionStateInfo csi = new DbxConnectionStateInfoGenericJdbc(this);
		setConnectionStateInfo(csi);
		return csi;
	}

	@Override
	public Map<String, TableExtraInfo> getTableExtraInfo(String cat, String schema, String table)
	{
		LinkedHashMap<String, TableExtraInfo> extraInfo = new LinkedHashMap<>();

//		cat    = StringUtil.isNullOrBlank(cat)    ? "" : cat    + ".";
//		schema = StringUtil.isNullOrBlank(schema) ? "" : schema + ".";

		
		String sql = 
				  "select num_rows, blocks/128*1024 as SizeKB \n"
				+ "from all_tables \n"
				+ "where 1=1 \n"
				+ (StringUtil.hasValue(schema) ? "  and owner      = '" + schema + "' \n" : "")
				+                                "  and table_name = '" + table  + "' \n";

//-----------------------------------------------------------------------------------------
//--- Possibly use the follwing in future (when I can test on an Oracle system)
//--- This gets the LOB size as well
//-----------------------------------------------------------------------------------------
//            	SELECT s.segment_name segment_name,
//                   SUM(s.bytes/1024/1024) tab_size,
//                   SUM((SELECT SUM(b.bytes/1024/1024)
//                          FROM dba_segments b
//                         WHERE b.owner = '<dbs_ora_schema>'
//                           AND b.segment_type = 'LOBSEGMENT'
//                           AND b.segment_name = l.segment_name)) lob_size
//              FROM dba_segments s LEFT JOIN dba_lobs l
//                ON s.owner = l.owner
//               AND s.segment_name = l.table_name
//             WHERE s.owner = '<dbs_ora_schema>'
//               AND s.segment_type LIKE 'TABLE%'
//               AND s.segment_name = '<table name>'
//            GROUP BY s.segment_name

		try
		{
			Statement stmnt = _conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			while(rs.next())
			{
				extraInfo.put(TableExtraInfo.TableRowCount,      new TableExtraInfo(TableExtraInfo.TableRowCount,      "Row Count",        rs.getLong(1), "Estimated rows in the table. Fetched using 'num_rows' from 'all_tables'", null));
				extraInfo.put(TableExtraInfo.TableTotalSizeInKb, new TableExtraInfo(TableExtraInfo.TableTotalSizeInKb, "Total Size In KB", rs.getLong(2), "Estimated table size in KB. Fetched using 'blocks/128*1024' from 'all_tables'", null));
			}
			rs.close();
		}
		catch (SQLException ex)
		{
			_logger.error("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex);
			if (_logger.isDebugEnabled())
				_logger.debug("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex, ex);
		}
		
		return extraInfo;
	}

	@Override
	protected int getDbmsSessionId_impl() throws SQLException
	{
		String sql = "select sys_context('USERENV','SID') from dual";

		int spid = -1;
		try (Statement stmnt = _conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				spid = rs.getInt(1);
		}
		
		return spid;
	}

	@Override
	public List<String> getActiveServerRolesOrPermissions()
	{
		if (_getActiveServerRolesOrPermissions != null)
			return _getActiveServerRolesOrPermissions;

        //  +--------------------------+
        //  |ROLE                      |
        //  +--------------------------+
        //  |DBA                       |
        //  |SELECT_CATALOG_ROLE       |
        //  |HS_ADMIN_SELECT_ROLE      |
        //  |EXECUTE_CATALOG_ROLE      |
        //  |HS_ADMIN_EXECUTE_ROLE     |
        //  |DELETE_CATALOG_ROLE       |
        //  |EXP_FULL_DATABASE         |
        //  |IMP_FULL_DATABASE         |
        //  |DATAPUMP_EXP_FULL_DATABASE|
        //  |DATAPUMP_IMP_FULL_DATABASE|
        //  |GATHER_SYSTEM_STATISTICS  |
        //  |SCHEDULER_ADMIN           |
        //  |PLUSTRACE                 |
        //  |XDBADMIN                  |
        //  |XDB_SET_INVOKER           |
        //  |AQ_ADMINISTRATOR_ROLE     |
        //  +--------------------------+
        //  Rows 16
		
		String sql = "SELECT * FROM SESSION_ROLES";  // or possibly: SESSION_PRIVS
		try
		{
			List<String> permissionList = new LinkedList<String>();
			
			try (Statement stmt = this.createStatement(); ResultSet rs = stmt.executeQuery(sql))
			{
				while (rs.next())
				{
					String role = rs.getString(1);
					if ( ! permissionList.contains(role) )
						permissionList.add(role);
				}
			}

			if (_logger.isDebugEnabled())
				_logger.debug("getActiveServerRolesOrPermissions() returns, permissionList='" + permissionList + "'.");

			// Cache the value for next execution
			_getActiveServerRolesOrPermissions = permissionList;
			return permissionList;
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems when executing sql: "+sql, ex);
			return null;
		}
	}
	
}

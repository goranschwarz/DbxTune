/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.sql.conn;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoGenericJdbc;
import com.asetune.ui.autocomplete.completions.ProcedureInfo;
import com.asetune.ui.autocomplete.completions.TableExtraInfo;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

public class OracleConnection 
extends DbxConnection
{
	private static Logger _logger = Logger.getLogger(OracleConnection.class);

	public OracleConnection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = true;
//System.out.println("constructor::OracleConnection(conn): conn="+conn);
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

}

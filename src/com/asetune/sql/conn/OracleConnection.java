package com.asetune.sql.conn;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoGenericJdbc;
import com.asetune.ui.autocomplete.completions.ProcedureInfo;

public class OracleConnection 
extends DbxConnection
{

	public OracleConnection(Connection conn)
	{
		super(conn);
System.out.println("constructor::OracleConnection(conn): conn="+conn);
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
}

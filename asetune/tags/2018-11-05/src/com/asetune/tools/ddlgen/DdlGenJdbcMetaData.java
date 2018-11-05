package com.asetune.tools.ddlgen;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.conn.DbxConnection;

public class DdlGenJdbcMetaData extends DdlGen
{

	protected DdlGenJdbcMetaData(DbxConnection conn)
	{
		super(conn);
	}

	@Override
	public String getDdlForType(Type type, String name) throws Exception
	{
		//---------------------------------------------
		// Add more arguments, based on the type
		//
		if (type.equals(Type.DB))              { return getDdlForDb       (name); }
		else if (type.equals(Type.TABLE))      { return getDdlForTable    (name); }
		else if (type.equals(Type.VIEW))       { return getDdlForView     (name); } 
		else if (type.equals(Type.PROCEDURE))  { return getDdlForProcedure(name); }
		else if (type.equals(Type.FUNCTION))   { return getDdlForFunction (name); }
		else if (type.equals(Type.RAW_PARAMS)) { throw new Exception("Type '"+type+"' is not supported for DdlGenJdbcMetaData."); }
		else 
		{
			throw new Exception("Unknown type '"+type+"'.");
		}
	}

	@Override
	public String getDdlForDb(String name)
	throws Exception
	{
		return "NOT-YET-IMPLEMETED (getDdlForDb)";
	}

	@Override
	public String getDdlForTable(String name) throws Exception
	{
		DatabaseMetaData dbmd = getConnection().getMetaData();
		StringBuilder sb = new StringBuilder();

//		dbmd.getTables(catalog, schemaPattern, tableNamePattern, types)
		sb.append("----- dbmd.getTables --------------\n");
		ResultSet rs = dbmd.getTables(null, null, name, null);
		sb.append( new ResultSetTableModel(rs, "dbmd.getTables").toTableString() );
		
		sb.append("----- dbmd.getColumns --------------\n");
		rs = dbmd.getColumns(null, null, name, "%");
		sb.append( new ResultSetTableModel(rs, "dbmd.getColumns").toTableString() );

		sb.append("----- dbmd.getPrimaryKeys --------------\n");
		rs = dbmd.getPrimaryKeys(null, null, name);
		sb.append( new ResultSetTableModel(rs, "dbmd.getPrimaryKeys").toTableString() );

		sb.append("----- dbmd.getIndexInfo --------------\n");
		rs = dbmd.getIndexInfo(null, null, name, false, true);
		sb.append( new ResultSetTableModel(rs, "dbmd.getPrimaryKeys").toTableString() );

		return sb.toString();
//		return "NOT-YET-IMPLEMETED (getDdlForTable)";
	}

	@Override
	public String getDdlForView(String name) throws Exception
	{
		return "NOT-YET-IMPLEMETED (getDdlForView)";
	}

	@Override
	public String getDdlForProcedure(String name) throws Exception
	{
		return "NOT-YET-IMPLEMETED (getDdlForProcedure)";
	}

	@Override
	public String getDdlForFunction(String name) throws Exception
	{
		return "NOT-YET-IMPLEMETED (getDdlForFunction)";
	}

}

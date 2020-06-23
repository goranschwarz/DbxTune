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

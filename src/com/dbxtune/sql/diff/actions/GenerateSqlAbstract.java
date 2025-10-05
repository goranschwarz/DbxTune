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
package com.dbxtune.sql.diff.actions;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.sql.SqlObjectName;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.diff.DiffContext;
import com.dbxtune.sql.diff.DiffContext.DiffSide;
import com.dbxtune.sql.diff.DiffSink;
import com.dbxtune.sql.diff.DiffSink.DiffColumnValues;
import com.dbxtune.sql.diff.DiffTable;
import com.dbxtune.utils.StringUtil;

public abstract class GenerateSqlAbstract
{
	private DiffSink _sink;
	
	private int _dmlCount     = 0;
	private int _commentCount = 0;
	private int _insertCount  = 0;
	private int _deleteCount  = 0;
	private int _updateCount  = 0;

	private DbxConnection _conn;
	
	// used to check that IDENTITY/AUTO_INCREMENT is enabled on some columns
	// we should do the following before after inserting into such tables:
	// Sybase-ASE:           BEFORE: 'set identity_insert <tabname> on' AFTER: 'set identity_insert <tabname> off'  NOTE: We probably need to SET THE MAX identity value somewhere as well ... exec sp_chgattribute 'dbo.table_name', 'identity_burn_max', 0, 'new-identity-value'      OR   dbcc set_identity_burn_max('database_name', 'table_name', 'new-identity-value')
	// Sybase-IQ:            BEFORE: ???? AFTER: ????
	// Sybase-SQL-Anywhere:  BEFORE: ???? AFTER: ???? (probably same as ASE)
	// Microsoft SQL-Server: BEFORE: 'set identity_insert <tabname> on' AFTER: 'set identity_insert <tabname> off'  NOTE: We probably need to SET THE MAX identity value somewhere as well
	// Oracle:               BEFORE: ???? AFTER: ???? (do we need to "fix" the sequence, and how do we know what sequence name to do it on)
	// MySQL:                BEFORE: ???? AFTER: ???? (do we need to "fix" the sequence, and how do we know what sequence name to do it on)
	// Postgres:             BEFORE: ???? AFTER: ???? (do we need to "fix" the sequence, and how do we know what sequence name to do it on) -- use: insert into t1(...) OVERRIDING SYSTEM  values(...);;; AND at the end if we want to SET the new starting point for the table with something like: SELECT setval(pg_get_serial_sequence('t2', 'id'), coalesce(MAX(id), 1)) from t2;   OR  'ALTER TABLE table ALTER COLUMN id RESTART WITH 1000;'
	// DB2:                  BEFORE: ???? AFTER: ???? (db2 has both identity and sequence...)
	// SAP-HANA:             BEFORE: ???? AFTER: ???? (HANA has both identity and sequence...)
	// ????????:             BEFORE: ???? AFTER: ???? (how should we handle DBMS's that we don't know... templates or something)
//	private ResultSetTableModel _rstm;
	private List<String> _autoIncrementCols = new ArrayList<>();
	
	public GenerateSqlAbstract(DiffContext context, DbxConnection conn)
	{
		this(context.getSink(), conn);
	}
	public GenerateSqlAbstract(DiffSink sink, DbxConnection conn)
	{
		_sink     = sink;
		_conn     = conn;
		
		if (conn != null)
		{
			setLeftQuote(  conn.getLeftQuote()  );
			setRightQuote( conn.getRightQuote() );
		}
	}

	public int getDmlCount()     { return _dmlCount;     }
	public int getCommentCount() { return _commentCount; }
	public int getInsertCount()  { return _insertCount;  }
	public int getDeleteCount()  { return _deleteCount;  }
	public int getUpdateCount()  { return _updateCount;  }

	private List<String> getAutoIncrementColumns(SqlObjectName sqlObj)
	{
		List<String> list = new ArrayList<>();

		try
		{
			ResultSet rs = _conn.getMetaData().getColumns(sqlObj.getCatalogNameNull(), sqlObj.getSchemaNameNull(), sqlObj.getObjectNameNull(), "%");
			ResultSetTableModel rstm = new ResultSetTableModel(rs, "getColumns");
			rs.close();
			
			int colPos = rstm.findColumnNoCase("IS_AUTOINCREMENT");
			for (int r=0; r<rstm.getRowCount(); r++)
			{
				String isAutoIncr = rstm.getValueAsString(r, "IS_AUTOINCREMENT", false);
				if ("YES".equalsIgnoreCase(isAutoIncr))
				{
					String colName = rstm.getValueAsString(r, "COLUMN_NAME", false);
					list.add(colName);
				}
			}
		}
		catch (SQLException ex)
		{
			throw new RuntimeException("Problems getting 'Auto Increment' columns from Table MetaData. Caught: "+ex, ex);
		}
		
		return list;
	}

	/**
	 * 
	 * @param side
	 * @param goString 
	 * @return
	 */
	protected List<String> generateSqlFix(DiffSide side, String goString)
	throws SQLException
	{
		List<String> list = new ArrayList<>();

		if (goString == null)
			goString = "";
		
		if (_sink.getContext().isTraceEnabled())
		{
			_sink.getContext().addTraceMessage("GO_STRING: " + goString);
			for ( Object[] row : _sink.getLeftMissingRows())    _sink.getContext().addTraceMessage("LEFT  MISSING: " + StringUtil.toCommaStr(row));
			for ( Object[] row : _sink.getRightMissingRows())   _sink.getContext().addTraceMessage("RIGHT MISSING: " + StringUtil.toCommaStr(row));
			
			for ( DiffColumnValues dcv : _sink.getDiffColumnValues())   _sink.getContext().addTraceMessage("DIFF COLUMNS:  " + dcv.toString());
		}

		if (DiffSide.LEFT.equals(side))
		{
			//-------------------------------
			// INSERT
			//-------------------------------
			if (_sink.getLeftMissingRows().size() > 0) 
			{
				_commentCount++;
				list.add("\n"
						+ "-------------------------------------------------------------------------------------------------------------------- \n"
						+ "-- INSERT Records on the LEFT hand side that existed on the RIGHT hand side, but was MISSING on the LEFT hands side. \n"
						+ "--------------------------------------------------------------------------------------------------------------------"
						+ goString);
				
				String identityInsert = generateSqlInsertPre(DiffSide.LEFT);
				if (identityInsert != null)
					list.add(identityInsert + goString);
			}

			for ( Object[] row : _sink.getLeftMissingRows()) 
			{
				_insertCount++;
				list.add(generateSqlInsert(side, row) + goString);
			}

			if (_sink.getLeftMissingRows().size() > 0) 
			{
				String identityInsert = generateSqlInsertPost(DiffSide.LEFT);
				if (identityInsert != null)
					list.add(identityInsert + goString);
			}
	
			//-------------------------------
			// DELETE
			//-------------------------------
			if (_sink.getRightMissingRows().size() > 0) 
			{
				_commentCount++;
				list.add("\n"
						+ "-------------------------------------------------------------------------------------------------------------------- \n"
						+ "-- DELETE Records on the LEFT side that exists on LEFT side, but do NOT exists on RIGHT hand side. \n"
						+ "--------------------------------------------------------------------------------------------------------------------"
						+  goString);
			}
			
			for ( Object[] row : _sink.getRightMissingRows()) 
			{
				_deleteCount++;
				list.add(generateSqlDelete(side, row) + goString);
			}

			//-------------------------------
			// UPDATE
			//-------------------------------
			if (_sink.getDiffColumnValues().size() > 0) 
			{
				_commentCount++;
				list.add("\n"
						+ "-------------------------------------------------------------------------------------------------------------------- \n"
						+ "-- UPDATE Records on the LEFT side with values from the RIGHT side, where column was NOT the same. \n"
						+ "--------------------------------------------------------------------------------------------------------------------"
						+ goString);
			}

			for ( DiffColumnValues dcv : _sink.getDiffColumnValues()) 
			{
				_updateCount++;
				list.add(generateSqlUpdate(side, dcv) + goString);
			}
		}

		if (DiffSide.RIGHT.equals(side))
		{
			//-------------------------------
			// INSERT
			//-------------------------------
			if (_sink.getRightMissingRows().size() > 0) 
			{
				_commentCount++;
				list.add("\n"
						+ "-------------------------------------------------------------------------------------------------------------------- \n"
						+ "-- INSERT Records on the RIGHT hand side that existed on the LEFT hand side, but was MISSING on the RIGHT hands side. \n"
						+ "--------------------------------------------------------------------------------------------------------------------"
						+ goString);

				String identityInsert = generateSqlInsertPre(DiffSide.RIGHT);
				if (identityInsert != null)
					list.add(identityInsert + goString);
			}

			for ( Object[] row : _sink.getRightMissingRows())
			{
				_insertCount++;
				list.add(generateSqlInsert(side, row) + goString);
			}

			if (_sink.getRightMissingRows().size() > 0) 
			{
				String identityInsert = generateSqlInsertPost(DiffSide.RIGHT);
				if (identityInsert != null)
					list.add(identityInsert + goString);
			}

			//-------------------------------
			// DELETE
			//-------------------------------
			if (_sink.getLeftMissingRows().size() > 0) 
			{
				_commentCount++;
				list.add("\n"
						+ "-------------------------------------------------------------------------------------------------------------------- \n"
						+ "-- DELETE Records on the RIGHT side that exists on RIGHT side, but do NOT exists on LEFT hand side. \n"
						+ "--------------------------------------------------------------------------------------------------------------------"
						+ goString);
			}

			for ( Object[] row : _sink.getLeftMissingRows()) 
			{
				_deleteCount++;
				list.add(generateSqlDelete(side, row) + goString);
			}

			//-------------------------------
			// UPDATE
			//-------------------------------
			if (_sink.getDiffColumnValues().size() > 0) 
			{
				_commentCount++;
				list.add("\n"
						+ "-------------------------------------------------------------------------------------------------------------------- \n"
						+ "-- UPDATE Records on the RIGHT side with values from the LEFT side, where column was NOT the same. \n"
						+ "--------------------------------------------------------------------------------------------------------------------"
						+ goString);
			}
			
			for ( DiffColumnValues dcv : _sink.getDiffColumnValues()) 
			{
				_updateCount++;
				list.add(generateSqlUpdate(side, dcv) + goString);
			}
		}
		
		return list;
	}
	
	private String _leftQuoteStr  = "[";
	private String _rightQuoteStr = "]";
	public  String getLeftQuote()            { return _leftQuoteStr;  }
	public  String getRightQuote()           { return _rightQuoteStr; }
	public  void   setLeftQuote (String str) { _leftQuoteStr  = str;  }
	public  void   setRightQuote(String str) { _rightQuoteStr = str;  }
	
	private String generateSqlInsertPre(DiffSide side)
	{
		DiffTable dt = DiffSide.LEFT.equals(side) ? _sink.getLeftDt() : _sink.getRightDt();
		SqlObjectName sqlObj = new SqlObjectName(_conn, dt.getFullTableName());

		List<String> autoIncCols = getAutoIncrementColumns(sqlObj);
		if (autoIncCols.size() > 0)
			return "set identity_insert " + sqlObj.getFullNameOriginQuoted() + " on"; // FIXME: make this into some template so we can be DBMS Vendor independent

		return null;
	}
	
	private String generateSqlInsertPost(DiffSide side)
	{
		DiffTable dt = DiffSide.LEFT.equals(side) ? _sink.getLeftDt() : _sink.getRightDt();
		SqlObjectName sqlObj = new SqlObjectName(_conn, dt.getFullTableName());

		List<String> autoIncCols = getAutoIncrementColumns(sqlObj);
		if (autoIncCols.size() > 0)
			return "set identity_insert " + sqlObj.getFullNameOriginQuoted() + " off"; // FIXME: make this into some template so we can be DBMS Vendor independent

		return null;
	}
	
	private String generateSqlInsert(DiffSide side, Object[] row)
	{
		DiffTable dt = DiffSide.LEFT.equals(side) ? _sink.getLeftDt() : _sink.getRightDt();

//		StringBuilder sb = new StringBuilder();
//FIXME: dt.getFullTableName() needs to use SqlObjectName to figgure out the QUOTING
		SqlObjectName sqlObj = new SqlObjectName(_conn, dt.getFullTableName());
		
		return "INSERT into " + sqlObj.getFullNameOriginQuoted() + " (" + dt.getColumnNamesCsv(getLeftQuote(),getRightQuote()) + ") "
		     + "values(" + dt.toSqlValues(row) + ")";
		
		//return sb.toString();
	}
	
	private String generateSqlDelete(DiffSide side, Object[] row)
	{
		DiffTable dt = DiffSide.LEFT.equals(side) ? _sink.getLeftDt() : _sink.getRightDt();
		SqlObjectName sqlObj = new SqlObjectName(_conn, dt.getFullTableName());
		
		return "DELETE from " + sqlObj.getFullNameOriginQuoted() + " " 
		      + dt.getPkWhereClause(row, getLeftQuote(), getRightQuote());
	}
	
	private String generateSqlUpdate(DiffSide side, DiffColumnValues dcv)
	{
		DiffTable     dt   = DiffSide.LEFT.equals(side) ? _sink.getLeftDt() : _sink.getRightDt();
		List<Integer> cols = DiffSide.LEFT.equals(side) ? dcv.getRightColumnsPos() : dcv.getLeftColumnsPos();   // NOTE: for LEFT use RIGHT VALUES
		Object[]      row  = DiffSide.LEFT.equals(side) ? dcv.getRightValues()     : dcv.getLeftValues();       // NOTE: for LEFT use RIGHT VALUES

		SqlObjectName sqlObj = new SqlObjectName(_conn, dt.getFullTableName());
		
		return "UPDATE " + sqlObj.getFullNameOriginQuoted() 
		     + dt.getUpdateSet(row, cols, getLeftQuote(), getRightQuote())
		     + dt.getPkWhereClause(row, getLeftQuote(), getRightQuote());
	}
}

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
package com.dbxtune.sql;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ResultSetMetaDataChangable 
implements ResultSetMetaData
{
	private ResultSetMetaData _rsmd = null;
	private List<ExtendedResultSetMetaDataEntry> _rsmdWrapEntries = null;

	public ResultSetMetaDataChangable(ResultSetMetaData rsmd)
	{
		_rsmd = rsmd;

		int size = 256;
		try { size = _rsmd.getColumnCount(); }
		catch (Exception ignore) { /* ignore */ }

		// initialize the list with null values, just so we don't get IndexOutOfBoundsException
		_rsmdWrapEntries = new ArrayList<ExtendedResultSetMetaDataEntry>(size);
		for (int i=0; i<=size; i++)
			_rsmdWrapEntries.add(i, null);
		
//		try
//		{
//			System.out.println("###############################################");
//			System.out.println("------new---- ResultSetMetaDataChangable ------");
//			System.out.println("getColumnCount: "+_rsmd.getColumnCount());
//			System.out.println("-----------------------------------------------");
//			for (int c=1; c<=_rsmd.getColumnCount(); c++)
//			{
//				System.out.println("column: "+c);
//				System.out.println("   getColumnLabel       = " + _rsmd.getColumnLabel(c));
//				System.out.println("   getColumnDisplaySize = " + _rsmd.getColumnDisplaySize(c));
//				System.out.println("   getColumnType        = " + _rsmd.getColumnType(c));
//				System.out.println("   getColumnTypeName    = " + _rsmd.getColumnTypeName(c));
//				System.out.println("   getColumnClassName   = " + _rsmd.getColumnClassName(c));
//				System.out.println("   -----");
//				System.out.println();
//				
//			}
//			System.out.println("-----------------------------------------------");
//			System.out.println("");
//		}
//		catch (Exception ignore) {}
	}

	private static class ExtendedResultSetMetaDataEntry
	{
		int    _columnDisplaySize;
		int    _columnType;
		String _columnTypeName;
		String _columnClassName;
	}

	public void setExtendedEntry(int column, int jdbcColumnType)
	{
		int    columnDisplaySize;
		int    columnType;
		String columnTypeName;
		String columnClassName;

		switch (jdbcColumnType)
		{
		case java.sql.Types.CLOB:
			columnDisplaySize = Integer.MAX_VALUE;          // jConnect seems to deliver Integer.MAX_VALUE
			columnType        = java.sql.Types.LONGVARCHAR; // jConnect seems to deliver LONGVARCHAR when select convert(text, '') and not java.sql.Types.CLOB
			columnTypeName    = "text";
			columnClassName   = String.class.getName();
			break;

		default:
			throw new IllegalArgumentException("Unknown translation type "+jdbcColumnType);
		}
		setExtendedEntry(column, columnDisplaySize, columnType, columnTypeName, columnClassName);
	}

	public void setExtendedEntry(int column, int columnDisplaySize, int columnType, String columnTypeName, String columnClassName)
	{
		ExtendedResultSetMetaDataEntry xe = new ExtendedResultSetMetaDataEntry();
		xe._columnDisplaySize = columnDisplaySize;
		xe._columnType        = columnType;
		xe._columnTypeName    = columnTypeName;
		xe._columnClassName   = columnClassName;
		
		_rsmdWrapEntries.set(column, xe);
	}

	public ExtendedResultSetMetaDataEntry getExtendedEntry(int column)
	{
		return _rsmdWrapEntries.get(column);
	}
	
	@Override
	public int getColumnDisplaySize(int column) throws SQLException
	{
		ExtendedResultSetMetaDataEntry xe = getExtendedEntry(column);
		if (xe != null)
			return xe._columnDisplaySize;

		return _rsmd.getColumnDisplaySize(column);
	}

	@Override
	public int getColumnType(int column) throws SQLException
	{
		ExtendedResultSetMetaDataEntry xe = getExtendedEntry(column);
		if (xe != null)
			return xe._columnType;

		return _rsmd.getColumnType(column);
	}

	@Override
	public String getColumnTypeName(int column) throws SQLException
	{
		ExtendedResultSetMetaDataEntry xe = getExtendedEntry(column);
		if (xe != null)
			return xe._columnTypeName;

		return _rsmd.getColumnTypeName(column);
	}

	@Override
	public String getColumnClassName(int column) throws SQLException
	{
		ExtendedResultSetMetaDataEntry xe = getExtendedEntry(column);
		if (xe != null)
			return xe._columnClassName;

		return _rsmd.getColumnClassName(column);
	}

	//---------------------------------------------------------------------------
	// below is wrapper methods.
	//---------------------------------------------------------------------------
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException
	{
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException
	{
		return false;
	}

	@Override
	public int getColumnCount() throws SQLException
	{
		return _rsmd.getColumnCount();
	}

	@Override
	public boolean isAutoIncrement(int column) throws SQLException
	{
		return _rsmd.isAutoIncrement(column);
	}

	@Override
	public boolean isCaseSensitive(int column) throws SQLException
	{
		return _rsmd.isCaseSensitive(column);
	}

	@Override
	public boolean isSearchable(int column) throws SQLException
	{
		return _rsmd.isSearchable(column);
	}

	@Override
	public boolean isCurrency(int column) throws SQLException
	{
		return _rsmd.isCurrency(column);
	}

	@Override
	public int isNullable(int column) throws SQLException
	{
		return _rsmd.isNullable(column);
	}

	@Override
	public boolean isSigned(int column) throws SQLException
	{
		return _rsmd.isSigned(column);
	}

//	@Override
//	public int getColumnDisplaySize(int column) throws SQLException
//	{
//		return _rsmd.getColumnDisplaySize(column);
//	}

	@Override
	public String getColumnLabel(int column) throws SQLException
	{
		return _rsmd.getColumnLabel(column);
	}

	@Override
	public String getColumnName(int column) throws SQLException
	{
		return _rsmd.getColumnName(column);
	}

	@Override
	public String getSchemaName(int column) throws SQLException
	{
		return _rsmd.getSchemaName(column);
	}

	@Override
	public int getPrecision(int column) throws SQLException
	{
		return _rsmd.getPrecision(column);
	}

	@Override
	public int getScale(int column) throws SQLException
	{
		return _rsmd.getScale(column);
	}

	@Override
	public String getTableName(int column) throws SQLException
	{
		return _rsmd.getTableName(column);
	}

	@Override
	public String getCatalogName(int column) throws SQLException
	{
		return _rsmd.getCatalogName(column);
	}

//	@Override
//	public int getColumnType(int column) throws SQLException
//	{
//		return _rsmd.getColumnType(column);
//	}

//	@Override
//	public String getColumnTypeName(int column) throws SQLException
//	{
//		return _rsmd.getColumnTypeName(column);
//	}

	@Override
	public boolean isReadOnly(int column) throws SQLException
	{
		return _rsmd.isReadOnly(column);
	}

	@Override
	public boolean isWritable(int column) throws SQLException
	{
		return _rsmd.isWritable(column);
	}

	@Override
	public boolean isDefinitelyWritable(int column) throws SQLException
	{
		return _rsmd.isDefinitelyWritable(column);
	}

//	@Override
//	public String getColumnClassName(int column) throws SQLException
//	{
//		return _rsmd.getColumnClassName(column);
//	}
}

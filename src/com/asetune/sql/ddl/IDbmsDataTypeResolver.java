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
package com.asetune.sql.ddl;

import com.asetune.sql.ResultSetMetaDataCached.Entry;
import com.asetune.sql.ddl.model.TableColumn;

public interface IDbmsDataTypeResolver
{

	/**
	 * Used when you want to <i>normalize</i> a specific DBMS Vendor data types into <i>something other</i><br>
	 * Example of this could be:
	 * <ul>
	 *   <li>Oracles various <code>Numbers(p,s)</code> into <code>java.sql.Types.INTEGER</code></li>
	 *   <li>Bump up <code>unsigned integer</code>     into <code>java.sql.Types.BIGINT </code></li>
	 * </ul>
	 * <p>
	 * To change/normalize a column.<br>
	 * Simply modify the passed <code>entry</code> record. 
	 * 
	 * @param entry    a record with the same methods as ResultSetMetaData 
	 */
	void    dataTypeResolverForSource(Entry entry);

	/**
	 * Used when <i>translate</i> a <code>java.sql.Types.<i>DATATYPE</i></code> into a specific DBMS Vendor Data Type<br>
	 * This would be used when a program wants to create a table and want's to map the internal Java <code>java.sql.Types.<i>DATATYPE</i></code> into a DBMS Vendor implementation.
	 * <p>
	 * Example of this could be:
	 * <ul>
	 *   <li><code>java.sql.Types.INTEGER</code> into Oracle <code>Numbers(10,0)</code>
	 *   <li><code>java.sql.Types.VARCHAR</code> into MS SQL-Server <code>[n]varchar(#)</code>, and for Oracle <code>varchar2(#)</code> 
	 *   <li><code>java.sql.Types.NVARCHAR</code> into MS SQL-Server <code>nvarchar(#)</code>, but when it's Postgres use <code>varchar(#)</code> (because varchar holds UTF8), and for Oracle <code>varchar2(#)</code> 
	 * </ul>
	 * <p>
	 * 
	 * @param javaSqlType    <code>java.sql.Types.<i>DATATYPE</i></code>
	 * @param length         Length of the data: for char/varchar it would be the string length, for numeric/decimal if would be length/precision of the column.
	 * @param scale          only used for for numeric/decimal and specifies the used data type scale (how many <i>digits</i> of the above length that should be used for decimals)  
	 * 
	 * @return A <i>full</i> String representation of the DBMS Vendors SQL Data Type (including length/scale), example <code>varchar(30)</code>
	 * @throws DataTypeNotResolvedException
	 */
	String  dataTypeResolverToTarget(int javaSqlType, int length, int scale) throws DataTypeNotResolvedException;

	/**
	 * Same as <code>dataTypeResolverTarget(int javaSqlType, int length, int scale)</code> but uses the <code>ResultSetMetaDataCached.Entry</code> instead.
	 * <p>
	 * This would typically be a wrapper that does: <br>
	 * <pre>
	 * public String dataTypeResolverTarget(Entry entry)
	 * {
	 *     int javaSqlType = entry.getColumnType();
	 *     int length      = entry.getPrecision();
	 *     int scale       = entry.getScale();
	 *     
	 *     return dataTypeResolverTarget(javaSqlType, length, scale);
	 * }
	 * </pre>
	 * @param entry
	 * @return
	 */
	String  dataTypeResolverToTarget(Entry entry);

	/**
	 * FIXME: describe this
	 * 
	 * @param column
	 * @return
	 */
	String  dataTypeResolverToTarget(TableColumn column);

}

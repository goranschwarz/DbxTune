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

import java.sql.ResultSet;
import java.sql.SQLException;

import com.asetune.sql.ResultSetMetaDataCached;
import com.asetune.sql.ddl.model.ForeignKey;
import com.asetune.sql.ddl.model.Index;
import com.asetune.sql.ddl.model.Schema;
import com.asetune.sql.ddl.model.Table;


/**
 * Used to normalize and transform DBMS Vendors Specific Data Type into <i>another</i> DBMS Vendors Data Type
 *  
 * <p> 
 * Possibly break this up into two interfaces:
 * <ul>
 *    <li>IDbmsDataTypeResolver</li>
 *    <li>IDbmsDdlResolver</li>
 * </ul>
 * 
 * @author gorans
 *
 */
public interface IDbmsDdlResolver
{
	public ResultSetMetaDataCached createNormalizedRsmd(ResultSet originRs) throws SQLException;
	public ResultSetMetaDataCached createNormalizedRsmd(ResultSetMetaDataCached originRsmd);
	
	public ResultSetMetaDataCached transformToTargetDbms(ResultSet sourceRs) throws SQLException;
	public ResultSetMetaDataCached transformToTargetDbms(ResultSetMetaDataCached normalizedRsmdc);

	boolean supportsIfNotExists(); // maybe for Table, Index etc... or a Enum parameter
	boolean skipCreateSchemaWithName(String schemaName);
	String  escapeQuotedIdentifier(String name);

	String ddlText(Schema schema);
	String ddlText(Table table);
	String ddlText(Table table, String inSchemaName, String inTableName);
	String ddlText(Index index, boolean pkAsConstraint);
	String ddlText(Index index, boolean pkAsConstraint, String inSchemaName, String inTableName);

	String ddlTextAlterTable(ForeignKey fk);
	String ddlTextAlterTable(ForeignKey fk, String inSchemaName, String inTableName);
	
	String ddlTextTable(ResultSetMetaDataCached rsmdc);
	String ddlTextTable(ResultSetMetaDataCached rsmdc, String schemaName, String tableName);
}

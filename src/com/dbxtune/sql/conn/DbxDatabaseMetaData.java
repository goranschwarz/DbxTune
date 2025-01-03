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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.utils.DbUtils;

public class DbxDatabaseMetaData implements DatabaseMetaData
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static DbxDatabaseMetaData create(DatabaseMetaData dbmd)
	{
		if (dbmd == null)
			throw new IllegalArgumentException("create(): dbmd can't be null");

		String dbmsProductName = "UNKNOWN";
		try 
		{
			dbmsProductName = dbmd.getDatabaseProductName();
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems calling dbmd.getDatabaseProductName(). dbmsProductName='" + dbmsProductName + "', errorCode=" + ex.getErrorCode() + ", sqlState=" + ex.getSQLState() + ", Message='" + ex.getMessage() + "'. Caught: " + ex);
		}
		
		// Create DBMS Specific implementations of the DbxDatabaseMetaData
		if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_MSSQL))
		{
			return new DbxDatabaseMetaDataSqlServer(dbmd);
		}
		else
		{
			return new DbxDatabaseMetaData(dbmd);
		}
	}

	public DbxDatabaseMetaData(DatabaseMetaData dbmd)
	{
		_dbmd = dbmd;
	}


	@Override
	public String toString()
	{
		return getClass().getName() + "@" + Integer.toHexString(hashCode()) + "[_dbmd=" + _dbmd + "]";
	}


	//#################################################################################
	//#################################################################################
	//### BEGIN: delegated methods
	//#################################################################################
	//#################################################################################
	protected DatabaseMetaData _dbmd;

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException
	{
		return _dbmd.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException
	{
		return _dbmd.isWrapperFor(iface);
	}

	@Override
	public boolean allProceduresAreCallable() throws SQLException
	{
		return _dbmd.allProceduresAreCallable();
	}

	@Override
	public boolean allTablesAreSelectable() throws SQLException
	{
		return _dbmd.allTablesAreSelectable();
	}

	@Override
	public String getURL() throws SQLException
	{
		// Something like this can be done for all methods, that is if we want to be able to trace all the method calls... but do I really want to write all that code...
		// How do I generate all that code???
		if (_logger.isDebugEnabled())
		{
			try
			{
				String str = _dbmd.getURL();
				_logger.debug("DbxDatabaseMetaData.getURL(): returns: "+str);
				return str;
			}
			catch (SQLException e)
			{
				_logger.debug("DbxDatabaseMetaData.getURL(): throws: "+e, e);
				throw e;
			}
		}
		return _dbmd.getURL();
	}

	@Override
	public String getUserName() throws SQLException
	{
		return _dbmd.getUserName();
	}

	@Override
	public boolean isReadOnly() throws SQLException
	{
		return _dbmd.isReadOnly();
	}

	@Override
	public boolean nullsAreSortedHigh() throws SQLException
	{
		return _dbmd.nullsAreSortedHigh();
	}

	@Override
	public boolean nullsAreSortedLow() throws SQLException
	{
		return _dbmd.nullsAreSortedLow();
	}

	@Override
	public boolean nullsAreSortedAtStart() throws SQLException
	{
		return _dbmd.nullsAreSortedAtStart();
	}

	@Override
	public boolean nullsAreSortedAtEnd() throws SQLException
	{
		return _dbmd.nullsAreSortedAtEnd();
	}

	@Override
	public String getDatabaseProductName() throws SQLException
	{
		return _dbmd.getDatabaseProductName();
	}

	@Override
	public String getDatabaseProductVersion() throws SQLException
	{
		return _dbmd.getDatabaseProductVersion();
	}

	@Override
	public String getDriverName() throws SQLException
	{
		return _dbmd.getDriverName();
	}

	@Override
	public String getDriverVersion() throws SQLException
	{
		return _dbmd.getDriverVersion();
	}

	@Override
	public int getDriverMajorVersion()
	{
		return _dbmd.getDriverMajorVersion();
	}

	@Override
	public int getDriverMinorVersion()
	{
		return _dbmd.getDriverMinorVersion();
	}

	@Override
	public boolean usesLocalFiles() throws SQLException
	{
		return _dbmd.usesLocalFiles();
	}

	@Override
	public boolean usesLocalFilePerTable() throws SQLException
	{
		return _dbmd.usesLocalFilePerTable();
	}

	@Override
	public boolean supportsMixedCaseIdentifiers() throws SQLException
	{
		return _dbmd.supportsMixedCaseIdentifiers();
	}

	@Override
	public boolean storesUpperCaseIdentifiers() throws SQLException
	{
		return _dbmd.storesUpperCaseIdentifiers();
	}

	@Override
	public boolean storesLowerCaseIdentifiers() throws SQLException
	{
		return _dbmd.storesLowerCaseIdentifiers();
	}

	@Override
	public boolean storesMixedCaseIdentifiers() throws SQLException
	{
		return _dbmd.storesMixedCaseIdentifiers();
	}

	@Override
	public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException
	{
		return _dbmd.supportsMixedCaseQuotedIdentifiers();
	}

	@Override
	public boolean storesUpperCaseQuotedIdentifiers() throws SQLException
	{
		return _dbmd.storesUpperCaseQuotedIdentifiers();
	}

	@Override
	public boolean storesLowerCaseQuotedIdentifiers() throws SQLException
	{
		return _dbmd.storesLowerCaseQuotedIdentifiers();
	}

	@Override
	public boolean storesMixedCaseQuotedIdentifiers() throws SQLException
	{
		return _dbmd.storesMixedCaseQuotedIdentifiers();
	}

	@Override
	public String getIdentifierQuoteString() throws SQLException
	{
		return _dbmd.getIdentifierQuoteString();
	}

	@Override
	public String getSQLKeywords() throws SQLException
	{
		return _dbmd.getSQLKeywords();
	}

	@Override
	public String getNumericFunctions() throws SQLException
	{
		return _dbmd.getNumericFunctions();
	}

	@Override
	public String getStringFunctions() throws SQLException
	{
		return _dbmd.getStringFunctions();
	}

	@Override
	public String getSystemFunctions() throws SQLException
	{
		return _dbmd.getSystemFunctions();
	}

	@Override
	public String getTimeDateFunctions() throws SQLException
	{
		return _dbmd.getTimeDateFunctions();
	}

	@Override
	public String getSearchStringEscape() throws SQLException
	{
		return _dbmd.getSearchStringEscape();
	}

	@Override
	public String getExtraNameCharacters() throws SQLException
	{
		return _dbmd.getExtraNameCharacters();
	}

	@Override
	public boolean supportsAlterTableWithAddColumn() throws SQLException
	{
		return _dbmd.supportsAlterTableWithAddColumn();
	}

	@Override
	public boolean supportsAlterTableWithDropColumn() throws SQLException
	{
		return _dbmd.supportsAlterTableWithDropColumn();
	}

	@Override
	public boolean supportsColumnAliasing() throws SQLException
	{
		return _dbmd.supportsColumnAliasing();
	}

	@Override
	public boolean nullPlusNonNullIsNull() throws SQLException
	{
		return _dbmd.nullPlusNonNullIsNull();
	}

	@Override
	public boolean supportsConvert() throws SQLException
	{
		return _dbmd.supportsConvert();
	}

	@Override
	public boolean supportsConvert(int fromType, int toType) throws SQLException
	{
		return _dbmd.supportsConvert(fromType, toType);
	}

	@Override
	public boolean supportsTableCorrelationNames() throws SQLException
	{
		return _dbmd.supportsTableCorrelationNames();
	}

	@Override
	public boolean supportsDifferentTableCorrelationNames() throws SQLException
	{
		return _dbmd.supportsDifferentTableCorrelationNames();
	}

	@Override
	public boolean supportsExpressionsInOrderBy() throws SQLException
	{
		return _dbmd.supportsExpressionsInOrderBy();
	}

	@Override
	public boolean supportsOrderByUnrelated() throws SQLException
	{
		return _dbmd.supportsOrderByUnrelated();
	}

	@Override
	public boolean supportsGroupBy() throws SQLException
	{
		return _dbmd.supportsGroupBy();
	}

	@Override
	public boolean supportsGroupByUnrelated() throws SQLException
	{
		return _dbmd.supportsGroupByUnrelated();
	}

	@Override
	public boolean supportsGroupByBeyondSelect() throws SQLException
	{
		return _dbmd.supportsGroupByBeyondSelect();
	}

	@Override
	public boolean supportsLikeEscapeClause() throws SQLException
	{
		return _dbmd.supportsLikeEscapeClause();
	}

	@Override
	public boolean supportsMultipleResultSets() throws SQLException
	{
		return _dbmd.supportsMultipleResultSets();
	}

	@Override
	public boolean supportsMultipleTransactions() throws SQLException
	{
		return _dbmd.supportsMultipleTransactions();
	}

	@Override
	public boolean supportsNonNullableColumns() throws SQLException
	{
		return _dbmd.supportsNonNullableColumns();
	}

	@Override
	public boolean supportsMinimumSQLGrammar() throws SQLException
	{
		return _dbmd.supportsMinimumSQLGrammar();
	}

	@Override
	public boolean supportsCoreSQLGrammar() throws SQLException
	{
		return _dbmd.supportsCoreSQLGrammar();
	}

	@Override
	public boolean supportsExtendedSQLGrammar() throws SQLException
	{
		return _dbmd.supportsExtendedSQLGrammar();
	}

	@Override
	public boolean supportsANSI92EntryLevelSQL() throws SQLException
	{
		return _dbmd.supportsANSI92EntryLevelSQL();
	}

	@Override
	public boolean supportsANSI92IntermediateSQL() throws SQLException
	{
		return _dbmd.supportsANSI92IntermediateSQL();
	}

	@Override
	public boolean supportsANSI92FullSQL() throws SQLException
	{
		return _dbmd.supportsANSI92FullSQL();
	}

	@Override
	public boolean supportsIntegrityEnhancementFacility() throws SQLException
	{
		return _dbmd.supportsIntegrityEnhancementFacility();
	}

	@Override
	public boolean supportsOuterJoins() throws SQLException
	{
		return _dbmd.supportsOuterJoins();
	}

	@Override
	public boolean supportsFullOuterJoins() throws SQLException
	{
		return _dbmd.supportsFullOuterJoins();
	}

	@Override
	public boolean supportsLimitedOuterJoins() throws SQLException
	{
		return _dbmd.supportsLimitedOuterJoins();
	}

	@Override
	public String getSchemaTerm() throws SQLException
	{
		return _dbmd.getSchemaTerm();
	}

	@Override
	public String getProcedureTerm() throws SQLException
	{
		return _dbmd.getProcedureTerm();
	}

	@Override
	public String getCatalogTerm() throws SQLException
	{
		return _dbmd.getCatalogTerm();
	}

	@Override
	public boolean isCatalogAtStart() throws SQLException
	{
		return _dbmd.isCatalogAtStart();
	}

	@Override
	public String getCatalogSeparator() throws SQLException
	{
		return _dbmd.getCatalogSeparator();
	}

	@Override
	public boolean supportsSchemasInDataManipulation() throws SQLException
	{
		return _dbmd.supportsSchemasInDataManipulation();
	}

	@Override
	public boolean supportsSchemasInProcedureCalls() throws SQLException
	{
		return _dbmd.supportsSchemasInProcedureCalls();
	}

	@Override
	public boolean supportsSchemasInTableDefinitions() throws SQLException
	{
		return _dbmd.supportsSchemasInTableDefinitions();
	}

	@Override
	public boolean supportsSchemasInIndexDefinitions() throws SQLException
	{
		return _dbmd.supportsSchemasInIndexDefinitions();
	}

	@Override
	public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException
	{
		return _dbmd.supportsSchemasInPrivilegeDefinitions();
	}

	@Override
	public boolean supportsCatalogsInDataManipulation() throws SQLException
	{
		return _dbmd.supportsCatalogsInDataManipulation();
	}

	@Override
	public boolean supportsCatalogsInProcedureCalls() throws SQLException
	{
		return _dbmd.supportsCatalogsInProcedureCalls();
	}

	@Override
	public boolean supportsCatalogsInTableDefinitions() throws SQLException
	{
		return _dbmd.supportsCatalogsInTableDefinitions();
	}

	@Override
	public boolean supportsCatalogsInIndexDefinitions() throws SQLException
	{
		return _dbmd.supportsCatalogsInIndexDefinitions();
	}

	@Override
	public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException
	{
		return _dbmd.supportsCatalogsInPrivilegeDefinitions();
	}

	@Override
	public boolean supportsPositionedDelete() throws SQLException
	{
		return _dbmd.supportsPositionedDelete();
	}

	@Override
	public boolean supportsPositionedUpdate() throws SQLException
	{
		return _dbmd.supportsPositionedUpdate();
	}

	@Override
	public boolean supportsSelectForUpdate() throws SQLException
	{
		return _dbmd.supportsSelectForUpdate();
	}

	@Override
	public boolean supportsStoredProcedures() throws SQLException
	{
		return _dbmd.supportsStoredProcedures();
	}

	@Override
	public boolean supportsSubqueriesInComparisons() throws SQLException
	{
		return _dbmd.supportsSubqueriesInComparisons();
	}

	@Override
	public boolean supportsSubqueriesInExists() throws SQLException
	{
		return _dbmd.supportsSubqueriesInExists();
	}

	@Override
	public boolean supportsSubqueriesInIns() throws SQLException
	{
		return _dbmd.supportsSubqueriesInIns();
	}

	@Override
	public boolean supportsSubqueriesInQuantifieds() throws SQLException
	{
		return _dbmd.supportsSubqueriesInQuantifieds();
	}

	@Override
	public boolean supportsCorrelatedSubqueries() throws SQLException
	{
		return _dbmd.supportsCorrelatedSubqueries();
	}

	@Override
	public boolean supportsUnion() throws SQLException
	{
		return _dbmd.supportsUnion();
	}

	@Override
	public boolean supportsUnionAll() throws SQLException
	{
		return _dbmd.supportsUnionAll();
	}

	@Override
	public boolean supportsOpenCursorsAcrossCommit() throws SQLException
	{
		return _dbmd.supportsOpenCursorsAcrossCommit();
	}

	@Override
	public boolean supportsOpenCursorsAcrossRollback() throws SQLException
	{
		return _dbmd.supportsOpenCursorsAcrossRollback();
	}

	@Override
	public boolean supportsOpenStatementsAcrossCommit() throws SQLException
	{
		return _dbmd.supportsOpenStatementsAcrossCommit();
	}

	@Override
	public boolean supportsOpenStatementsAcrossRollback() throws SQLException
	{
		return _dbmd.supportsOpenStatementsAcrossRollback();
	}

	@Override
	public int getMaxBinaryLiteralLength() throws SQLException
	{
		return _dbmd.getMaxBinaryLiteralLength();
	}

	@Override
	public int getMaxCharLiteralLength() throws SQLException
	{
		return _dbmd.getMaxCharLiteralLength();
	}

	@Override
	public int getMaxColumnNameLength() throws SQLException
	{
		return _dbmd.getMaxColumnNameLength();
	}

	@Override
	public int getMaxColumnsInGroupBy() throws SQLException
	{
		return _dbmd.getMaxColumnsInGroupBy();
	}

	@Override
	public int getMaxColumnsInIndex() throws SQLException
	{
		return _dbmd.getMaxColumnsInIndex();
	}

	@Override
	public int getMaxColumnsInOrderBy() throws SQLException
	{
		return _dbmd.getMaxColumnsInOrderBy();
	}

	@Override
	public int getMaxColumnsInSelect() throws SQLException
	{
		return _dbmd.getMaxColumnsInSelect();
	}

	@Override
	public int getMaxColumnsInTable() throws SQLException
	{
		return _dbmd.getMaxColumnsInTable();
	}

	@Override
	public int getMaxConnections() throws SQLException
	{
		return _dbmd.getMaxConnections();
	}

	@Override
	public int getMaxCursorNameLength() throws SQLException
	{
		return _dbmd.getMaxCursorNameLength();
	}

	@Override
	public int getMaxIndexLength() throws SQLException
	{
		return _dbmd.getMaxIndexLength();
	}

	@Override
	public int getMaxSchemaNameLength() throws SQLException
	{
		return _dbmd.getMaxSchemaNameLength();
	}

	@Override
	public int getMaxProcedureNameLength() throws SQLException
	{
		return _dbmd.getMaxProcedureNameLength();
	}

	@Override
	public int getMaxCatalogNameLength() throws SQLException
	{
		return _dbmd.getMaxCatalogNameLength();
	}

	@Override
	public int getMaxRowSize() throws SQLException
	{
		return _dbmd.getMaxRowSize();
	}

	@Override
	public boolean doesMaxRowSizeIncludeBlobs() throws SQLException
	{
		return _dbmd.doesMaxRowSizeIncludeBlobs();
	}

	@Override
	public int getMaxStatementLength() throws SQLException
	{
		return _dbmd.getMaxStatementLength();
	}

	@Override
	public int getMaxStatements() throws SQLException
	{
		return _dbmd.getMaxStatements();
	}

	@Override
	public int getMaxTableNameLength() throws SQLException
	{
		return _dbmd.getMaxTableNameLength();
	}

	@Override
	public int getMaxTablesInSelect() throws SQLException
	{
		return _dbmd.getMaxTablesInSelect();
	}

	@Override
	public int getMaxUserNameLength() throws SQLException
	{
		return _dbmd.getMaxUserNameLength();
	}

	@Override
	public int getDefaultTransactionIsolation() throws SQLException
	{
		return _dbmd.getDefaultTransactionIsolation();
	}

	@Override
	public boolean supportsTransactions() throws SQLException
	{
		return _dbmd.supportsTransactions();
	}

	@Override
	public boolean supportsTransactionIsolationLevel(int level) throws SQLException
	{
		return _dbmd.supportsTransactionIsolationLevel(level);
	}

	@Override
	public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException
	{
		return _dbmd.supportsDataDefinitionAndDataManipulationTransactions();
	}

	@Override
	public boolean supportsDataManipulationTransactionsOnly() throws SQLException
	{
		return _dbmd.supportsDataManipulationTransactionsOnly();
	}

	@Override
	public boolean dataDefinitionCausesTransactionCommit() throws SQLException
	{
		return _dbmd.dataDefinitionCausesTransactionCommit();
	}

	@Override
	public boolean dataDefinitionIgnoredInTransactions() throws SQLException
	{
		return _dbmd.dataDefinitionIgnoredInTransactions();
	}

	@Override
	public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException
	{
		return _dbmd.getProcedures(catalog, schemaPattern, procedureNamePattern);
	}

	@Override
	public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException
	{
		return _dbmd.getProcedureColumns(catalog, schemaPattern, procedureNamePattern, columnNamePattern);
	}

	@Override
	public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException
	{
		return _dbmd.getTables(catalog, schemaPattern, tableNamePattern, types);
	}

	@Override
	public ResultSet getSchemas() throws SQLException
	{
		return _dbmd.getSchemas();
	}

	@Override
	public ResultSet getCatalogs() throws SQLException
	{
		return _dbmd.getCatalogs();
	}

	@Override
	public ResultSet getTableTypes() throws SQLException
	{
		return _dbmd.getTableTypes();
	}

	@Override
	public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException
	{
		return _dbmd.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
	}

	@Override
	public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException
	{
		return _dbmd.getColumnPrivileges(catalog, schema, table, columnNamePattern);
	}

	@Override
	public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException
	{
		return _dbmd.getTablePrivileges(catalog, schemaPattern, tableNamePattern);
	}

	@Override
	public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException
	{
		return _dbmd.getBestRowIdentifier(catalog, schema, table, scope, nullable);
	}

	@Override
	public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException
	{
		return _dbmd.getVersionColumns(catalog, schema, table);
	}

	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException
	{
		return _dbmd.getPrimaryKeys(catalog, schema, table);
	}

	@Override
	public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException
	{
		return _dbmd.getImportedKeys(catalog, schema, table);
	}

	@Override
	public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException
	{
		return _dbmd.getExportedKeys(catalog, schema, table);
	}

	@Override
	public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException
	{
		return _dbmd.getCrossReference(parentCatalog, parentSchema, parentTable, foreignCatalog, foreignSchema, foreignTable);
	}

	@Override
	public ResultSet getTypeInfo() throws SQLException
	{
		return _dbmd.getTypeInfo();
	}

	@Override
	public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException
	{
		return _dbmd.getIndexInfo(catalog, schema, table, unique, approximate);
	}

	@Override
	public boolean supportsResultSetType(int type) throws SQLException
	{
		return _dbmd.supportsResultSetType(type);
	}

	@Override
	public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException
	{
		return _dbmd.supportsResultSetConcurrency(type, concurrency);
	}

	@Override
	public boolean ownUpdatesAreVisible(int type) throws SQLException
	{
		return _dbmd.ownUpdatesAreVisible(type);
	}

	@Override
	public boolean ownDeletesAreVisible(int type) throws SQLException
	{
		return _dbmd.ownDeletesAreVisible(type);
	}

	@Override
	public boolean ownInsertsAreVisible(int type) throws SQLException
	{
		return _dbmd.ownInsertsAreVisible(type);
	}

	@Override
	public boolean othersUpdatesAreVisible(int type) throws SQLException
	{
		return _dbmd.othersUpdatesAreVisible(type);
	}

	@Override
	public boolean othersDeletesAreVisible(int type) throws SQLException
	{
		return _dbmd.othersDeletesAreVisible(type);
	}

	@Override
	public boolean othersInsertsAreVisible(int type) throws SQLException
	{
		return _dbmd.othersInsertsAreVisible(type);
	}

	@Override
	public boolean updatesAreDetected(int type) throws SQLException
	{
		return _dbmd.updatesAreDetected(type);
	}

	@Override
	public boolean deletesAreDetected(int type) throws SQLException
	{
		return _dbmd.deletesAreDetected(type);
	}

	@Override
	public boolean insertsAreDetected(int type) throws SQLException
	{
		return _dbmd.insertsAreDetected(type);
	}

	@Override
	public boolean supportsBatchUpdates() throws SQLException
	{
		return _dbmd.supportsBatchUpdates();
	}

	@Override
	public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException
	{
		return _dbmd.getUDTs(catalog, schemaPattern, typeNamePattern, types);
	}

	@Override
	public Connection getConnection() throws SQLException
	{
		return _dbmd.getConnection();
	}

	@Override
	public boolean supportsSavepoints() throws SQLException
	{
		return _dbmd.supportsSavepoints();
	}

	@Override
	public boolean supportsNamedParameters() throws SQLException
	{
		return _dbmd.supportsNamedParameters();
	}

	@Override
	public boolean supportsMultipleOpenResults() throws SQLException
	{
		return _dbmd.supportsMultipleOpenResults();
	}

	@Override
	public boolean supportsGetGeneratedKeys() throws SQLException
	{
		return _dbmd.supportsGetGeneratedKeys();
	}

	@Override
	public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException
	{
		return _dbmd.getSuperTypes(catalog, schemaPattern, typeNamePattern);
	}

	@Override
	public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException
	{
		return _dbmd.getSuperTables(catalog, schemaPattern, tableNamePattern);
	}

	@Override
	public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException
	{
		return _dbmd.getAttributes(catalog, schemaPattern, typeNamePattern, attributeNamePattern);
	}

	@Override
	public boolean supportsResultSetHoldability(int holdability) throws SQLException
	{
		return _dbmd.supportsResultSetHoldability(holdability);
	}

	@Override
	public int getResultSetHoldability() throws SQLException
	{
		return _dbmd.getResultSetHoldability();
	}

	@Override
	public int getDatabaseMajorVersion() throws SQLException
	{
		return _dbmd.getDatabaseMajorVersion();
	}

	@Override
	public int getDatabaseMinorVersion() throws SQLException
	{
		return _dbmd.getDatabaseMinorVersion();
	}

	@Override
	public int getJDBCMajorVersion() throws SQLException
	{
		return _dbmd.getJDBCMajorVersion();
	}

	@Override
	public int getJDBCMinorVersion() throws SQLException
	{
		return _dbmd.getJDBCMinorVersion();
	}

	@Override
	public int getSQLStateType() throws SQLException
	{
		return _dbmd.getSQLStateType();
	}

	@Override
	public boolean locatorsUpdateCopy() throws SQLException
	{
		return _dbmd.locatorsUpdateCopy();
	}

	@Override
	public boolean supportsStatementPooling() throws SQLException
	{
		return _dbmd.supportsStatementPooling();
	}

	@Override
	public RowIdLifetime getRowIdLifetime() throws SQLException
	{
		return _dbmd.getRowIdLifetime();
	}

	@Override
	public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException
	{
		return _dbmd.getSchemas(catalog, schemaPattern);
	}

	@Override
	public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException
	{
		return _dbmd.supportsStoredFunctionsUsingCallSyntax();
	}

	@Override
	public boolean autoCommitFailureClosesAllResultSets() throws SQLException
	{
		return _dbmd.autoCommitFailureClosesAllResultSets();
	}

	@Override
	public ResultSet getClientInfoProperties() throws SQLException
	{
		return _dbmd.getClientInfoProperties();
	}

	@Override
	public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException
	{
		return _dbmd.getFunctions(catalog, schemaPattern, functionNamePattern);
	}

	@Override
	public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) throws SQLException
	{
		return _dbmd.getFunctionColumns(catalog, schemaPattern, functionNamePattern, columnNamePattern);
	}

	//#######################################################
	//############################# JDBC 4.1
	//#######################################################

	@Override
	public boolean generatedKeyAlwaysReturned() throws SQLException
	{
		return _dbmd.generatedKeyAlwaysReturned();
	}

	@Override
	public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException
	{
		return _dbmd.getPseudoColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
	}

}

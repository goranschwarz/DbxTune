/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.tools.sqlw;

import java.awt.Color;
import java.awt.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.SqlObjectName;
import com.asetune.sql.SqlProgressDialog;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.pipe.PipeCommandException;
import com.asetune.tools.sqlw.msg.JAseMessage;
import com.asetune.tools.sqlw.msg.JTableResultSet;
import com.asetune.ui.autocomplete.completions.TableInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

public class SqlStatementCmdUtf8Length 
extends SqlStatementAbstract
{
	private static Logger _logger = Logger.getLogger(SqlStatementCmdUtf8Length.class);

	private String[] _args = null;
	private String _originCmd = null;

//	Map<TableDetails, List<TableColumnDetails>> _tableMap = new LinkedHashMap<>();
	Set<TableDetails> _usedTableSet    = new LinkedHashSet<>();
	Set<TableDetails> _skippedTableSet = new LinkedHashSet<>();
	
	private static class CmdParams
	{
		//-----------------------
		// PARAMS
		//-----------------------
		boolean _allTypes          = false;
		boolean _showSkippedTables = false;
		boolean _showTableSummary  = true;
		boolean _showColumnReport  = true;
		boolean _showRowValDetails = true;
		int     _fetchSize         = -1;
		boolean _listOnly          = false;
		
		List<String> _inputList  = new ArrayList<>();
	}
	private CmdParams _params = new CmdParams();
	

	public SqlStatementCmdUtf8Length(DbxConnection conn, String sqlOrigin, String dbProductName, ArrayList<JComponent> resultCompList, SqlProgressDialog progress, Component owner, QueryWindow queryWindow)
	throws SQLException, PipeCommandException
	{
		super(conn, sqlOrigin, dbProductName, resultCompList, progress, owner, queryWindow);
		parse(sqlOrigin);
		init();
	}

	/**
	 * ============================================================
	 * see printHelp() for usage
	 * ------------------------------------------------------------
	 * 
	 * @param input
	 * @return
	 * @throws PipeCommandException
	 */
	public void parse(String input)
	throws SQLException, PipeCommandException
	{
		_originCmd = input;
		String params = input.replace("\\utf8len", "").trim();

		_args = StringUtil.translateCommandline(params, false);

		if (_args.length >= 1)
		{
			_params = new CmdParams();

			CommandLine cmdLine = parseCmdLine(_args);
			if (cmdLine.hasOption('a')) _params._allTypes          = true;
			if (cmdLine.hasOption('s')) _params._showSkippedTables = ! _params._showSkippedTables;
			if (cmdLine.hasOption('t')) _params._showTableSummary  = ! _params._showTableSummary;
			if (cmdLine.hasOption('c')) _params._showColumnReport  = ! _params._showColumnReport;
			if (cmdLine.hasOption('v')) _params._showRowValDetails = ! _params._showRowValDetails;
			if (cmdLine.hasOption('f')) _params._fetchSize         = StringUtil.parseInt(cmdLine.getOptionValue('f'), -1);
			if (cmdLine.hasOption('l')) _params._listOnly          = ! _params._listOnly;

			if (cmdLine.hasOption('?'))
				printHelp(null, "You wanted help...");

			if ( cmdLine.getArgs() != null && cmdLine.getArgs().length > 0 )
			{
				for (String arg : cmdLine.getArgs())
				{
					_params._inputList.add(arg);
				}
			}
			else if ( cmdLine.getArgs() != null && cmdLine.getArgs().length == 0 )
			{
				printHelp(null, "You need to specify a command");
			}

			if (_params._inputList.isEmpty())
			{
				printHelp(null, "You need to specify [schema.]table\n   NOTE: Use % as wildcard");
			}
		}
		else
		{
			printHelp(null, "Please specify some parameters.");
		}
	}

	private CommandLine parseCmdLine(String[] args)
	throws PipeCommandException
	{
		Options options = new Options();

		// create the Options
		// Switches       short long Option         hasParam Description (not really used)
		//                ----- ------------------- -------- ------------------------------------------
		options.addOption( "a", "all",              false,   "fixme" );
		options.addOption( "s", "showSkipped",      false,   "fixme" );
		options.addOption( "t", "tableSummary",     false,   "fixme" );
		options.addOption( "c", "columnReport",     false,   "fixme" );
		options.addOption( "v", "showRowValues",    false,   "fixme" );
		options.addOption( "f", "fetchSize",        true ,   "fixme" );
		options.addOption( "l", "listOnly",         false,   "fixme" );
		options.addOption( "?", "help",             false,   "fixme" );


		try
		{
			// create the command line com.asetune.parser
			CommandLineParser parser = new DefaultParser();

			// parse the command line arguments
			CommandLine cmd = parser.parse( options, args );

//			if ( cmd.getArgs() != null && cmd.getArgs().length == 0 )
//			{
//				String error = "You need to specify an output file";
//				printHelp(options, error);
//			}
//			if ( cmd.getArgs() != null && cmd.getArgs().length > 2 )
//			{
//				String error = "To many options: " + StringUtil.toCommaStr(cmd.getArgs());
//				printHelp(options, error);
//			}
			return cmd;
		}
		catch (ParseException pe)
		{
			String error = "Error: " + pe.getMessage();
			return null;
		}	
	}

	private static void printHelp(Options options, String errorStr)
	throws PipeCommandException
	{
		StringBuilder sb = new StringBuilder();

		if (errorStr != null)
		{
			sb.append("\n");
			sb.append(errorStr);
			sb.append("\n");
		}

		sb.append("\n");
		sb.append("usage: \\utf8len [schema.]table ... \n");
		sb.append("\n");
		sb.append("Description:\n");
		sb.append("  - Check the desired table names for char/varchar columns that exceeds the defined length.\n");
		sb.append("    This can be used for database migrations, where some DBMS vendor can hold number of characters while other DBMS will hold number of bytes in the string.\n");
		sb.append("\n");
		sb.append("Parameters:\n");
		sb.append("  [schema.]table     Name of the schema and table name. Note use '%' as a wildcard\n");
		sb.append("  -a --all           Normally we only check CHAR and VARCHAR columns, use this to check all column types that contains characters.\n");
		sb.append("  -l --listOnly      Do not check all tables, just list what tables will be checked\n");
		sb.append("  -s --showSkipped   Show tables (in it's own ResultSet) which do not have any String columns.\n");
		sb.append("  -t --tableSummary  Show tables summary (in it's own ResultSet).\n");
		sb.append("  -c --columnReport  Show tables column detail report (in it's own ResultSet).\n");
		sb.append("  -v --showRowValues Show UTF-8 Row Values that do are larger than the DBMS datatype (in it's own ResultSet)\n");
		sb.append("  -f --fetchSize ##  do Statement.setSize(##) when reading rows. (for Postgres the default is 1000)\n");
		sb.append("\n");
		
		throw new PipeCommandException(sb.toString());
	}
	
	private void init()
	throws SQLException
	{
		DatabaseMetaData dbmd = _conn.getMetaData();

		setProgressState("Fetching table information");
		setProgressWidth(150); // 150 Chars

		List<SqlObjectName> tabList = new ArrayList<>();
		
		for (String param : _params._inputList)
		{
//System.out.println("inputlist: param="+param);
			SqlObjectName sqlObj = new SqlObjectName(_conn, param);

			String catalogPattern = sqlObj.getCatalogNameNull();
			String schemaPattern  = sqlObj.getSchemaNameNull();
			String tablePattern   = sqlObj.getObjectNameOrigin();
			String[] tableTypes   = new String[] {"TABLE"};

			ResultSet rs = dbmd.getTables(catalogPattern, schemaPattern, tablePattern, tableTypes);
			while(rs.next())
			{
				String tmpCatalogName = StringUtils.trim(rs.getString(1));
				String tmpSchemaName  = StringUtils.trim(rs.getString(2));
				String tmpTableName   = StringUtils.trim(rs.getString(3));
				
				tabList.add( new SqlObjectName(_conn, tmpCatalogName + "." + tmpSchemaName + "." + tmpTableName) );
			}
			rs.close();
		}

		int tableCount = 0;
		int tableMaxCount = tabList.size();

		for (SqlObjectName sqlObj : tabList)
		{
//System.out.println("CHECKING Table (for stringCols): "+sqlObj.getFullName());
			String catalog = sqlObj.getCatalogNameNull();
			String schema  = sqlObj.getSchemaNameNull();
			String table   = sqlObj.getObjectNameOrigin();

			tableCount++;
			int tabPctDone = (int) ( ((tableCount*1.0) / (tableMaxCount*1.0)) * 100.0 );
			String tableProgress = "[" + tableCount + " of " + tableMaxCount + ", " + tabPctDone + "%]";

			setProgressState("Fetching column info for" + tableProgress + ": " + sqlObj.getFullName());
			
			TableDetails td = new TableDetails(this);
//			td._catalogName = catalog;
//			td._schemaName  = schema;
//			td._tableName   = table;
			
			ResultSet rs = dbmd.getColumns(catalog, schema, table, "%");
			while(rs.next())
			{
				TableColumnDetails tcd = new TableColumnDetails();
				
				tcd._tableDetails = td;

				td._catalogName = StringUtils.trim(rs.getString("TABLE_CAT"));
				td._schemaName  = StringUtils.trim(rs.getString("TABLE_SCHEM"));
				td._tableName   = StringUtils.trim(rs.getString("TABLE_NAME"));
//				tcd._catalogName = StringUtils.trim(rs.getString("TABLE_CAT"));
//				tcd._schemaName  = StringUtils.trim(rs.getString("TABLE_SCHEM"));
//				tcd._tableName   = StringUtils.trim(rs.getString("TABLE_NAME"));

				tcd._colName        = StringUtils.trim(rs.getString("COLUMN_NAME"));
				tcd._colPos         =                  rs.getInt   ("ORDINAL_POSITION");
				tcd._colJdbcType    =                  rs.getInt   ("DATA_TYPE");
				tcd._colType        = StringUtils.trim(rs.getString("TYPE_NAME"));
				tcd._colLength      =                  rs.getInt   ("COLUMN_SIZE");

				boolean addColumn = false;

				switch (tcd._colJdbcType)
				{
				case Types.CHAR:
				case Types.VARCHAR:
					addColumn = true;
					break;

				case Types.NCHAR:
				case Types.NVARCHAR:
				case Types.CLOB:
				case Types.NCLOB:
				case Types.LONGVARCHAR:
				case Types.LONGNVARCHAR:
				case Types.SQLXML:
					if ( _params._allTypes )
					{
						addColumn = true;
						break;
					}
				}
				
				if (addColumn)
				{
//System.out.println("CHECKING Table (for stringCols): "+sqlObj.getFullName());
//System.out.println("    ++++  FOUND COL: '"+tcd._colName+"', jdbcType="+tcd._colJdbcType+" -- "+ResultSetTableModel.getColumnJavaSqlTypeName(tcd._colJdbcType));
					td._columnDetails.add(tcd);
					_usedTableSet.add(td);
				}
				else
				{
					_skippedTableSet.add(td);
//System.out.println("    -----  SKIP COL: '"+tcd._colName+"', jdbcType="+tcd._colJdbcType+" -- "+ResultSetTableModel.getColumnJavaSqlTypeName(tcd._colJdbcType));
				}
				
				if (isCancelled())
					break;
			}
			rs.close();
		}


		// get PK for table
		tableCount = 0;
		tableMaxCount = _usedTableSet.size();

		for (TableDetails td : _usedTableSet)
		{
			tableCount++;
			int tabPctDone = (int) ( ((tableCount*1.0) / (tableMaxCount*1.0)) * 100.0 );
			String tableProgress = "[" + tableCount + " of " + tableMaxCount + ", " + tabPctDone + "%]";

			setProgressState("Fetching PrimaryKey for table" + tableProgress + ": " + td.getFullName(false));

			td._pkCols = TableInfo.getPkOrFirstUniqueIndex(_conn, td._catalogName, td._schemaName, td._tableName);

			if (isCancelled())
				break;
		}
		

		// Get estimate RowCount for each of the tables we want to check 
		tableCount = 0;
		tableMaxCount = _usedTableSet.size();

		for (TableDetails td : _usedTableSet)
		{
			tableCount++;
			int tabPctDone = (int) ( ((tableCount*1.0) / (tableMaxCount*1.0)) * 100.0 );
			String tableProgress = "[" + tableCount + " of " + tableMaxCount + ", " + tabPctDone + "%]";

			setProgressState("Fetching row estimate for table" + tableProgress + ": " + td.getFullName(false));

			td._estRowCount = _conn.getRowCountEstimate(td._catalogName, td._schemaName, td._tableName);
			
			if (isCancelled())
				break;
		}
	}
	
	@Override
	public Statement getStatement()
	{
		return new StatementDummy();
	}

	@Override
	public boolean execute() throws SQLException
	{
		// Just LIST what tables that will be checked
		if (_params._listOnly)
			return listOnly();

		if (isCancelled())
			return false;

		NumberFormat nf = NumberFormat.getInstance();

		String msgFetchSize = null;

		String initialCmdText = _progress == null ? "" : _progress.getAllSqlText();
		
		int tableCount = 0;
		int tableMaxCount = _usedTableSet.size();
		
		long totalRowCount = 0;
		long totalEstimateRowCount = 0;
		
		// Get total estimated count
		for (TableDetails td : _usedTableSet)
			totalEstimateRowCount += td._estRowCount;

		// Loop all entries and do select
		for (TableDetails td : _usedTableSet)
		{
			if (isCancelled())
				break;

			tableCount++;
			int tabPctDone = (int) ( ((tableCount*1.0) / (tableMaxCount*1.0)) * 100.0 );
			String tableProgress = "[" + tableCount + " of " + tableMaxCount + ", " + tabPctDone + "%]";

			setProgressState("Getting data for table" + tableProgress + ": " + td.getFullName(false));
			
			String sql = td.getSelectSql();
			sql = _conn.quotifySqlString(sql);
			
			setProgressState("Executing sql" + tableProgress + ": " + sql);
			if (_progress != null)
			{
				_progress.setAllSqlText(initialCmdText + "\n"
						+ "-------------------------------------------------------------------\n"
						+ "-- Now Executing below SQL, with estimated row count: " + nf.format(td._estRowCount) + "\n"
						+ "-------------------------------------------------------------------\n"
						+ sql);
			}

			// Get current state of Auto Commit so we can restore it later
			boolean connDoFetchInTransaction = false;
			boolean connOriginAutoCommit     = true;

			try (Statement stmnt = _conn.createStatement())
			{
				// NOTE: Postgres (and possibly other DBMS's) tries to read ALL records in the ResultSet into memory when executing a query
				//       This is possibly a way to get around this...
				//       https://jdbc.postgresql.org/documentation/head/query.html#fetchsize-example
				//       https://stackoverflow.com/questions/3682614/how-to-read-all-rows-from-huge-table
				connOriginAutoCommit = _conn.getAutoCommit();

				int fetchSize = _params._fetchSize;
				if ((fetchSize < 0 && _conn.isDatabaseProduct(DbUtils.DB_PROD_NAME_POSTGRES, DbUtils.DB_PROD_NAME_ORACLE)) || fetchSize > 0)
				{
					connDoFetchInTransaction = true;

					if (fetchSize < 0)
						fetchSize = 5_000;

					_conn.setAutoCommit(false); // Start a transaction

					//addInfoMessage("When checking table '" + td.getFullName(false) + "' the Connection is '" + _conn.getDatabaseProductName() + "', FetchSize will be set to " + fetchSize + " and we will start a Transaction where the select is made (row fetch).");
					msgFetchSize = "Used Connection is '" + _conn.getDatabaseProductName() + "', FetchSize was set to " + fetchSize + ". All rows was read in a Transaction.";

					if (fetchSize > 0)
						stmnt.setFetchSize(fetchSize);
				}

				// Set query timeout to: NO LIMIT
				stmnt.setQueryTimeout(0);

				long lastProgresUpdate = 0;
				long progresUpdatePeriodMs = 100;

				// EXECUTE
				try(ResultSet rs = stmnt.executeQuery(sql))
				{
					ResultSetMetaData rsmd = rs.getMetaData();
					int numCols = rsmd.getColumnCount();

					List<String> colNames = new ArrayList<>();
					for (int sqlPos=1; sqlPos<=numCols; sqlPos++)
						colNames.add(rsmd.getColumnLabel(sqlPos));
					
					int row = 0;
					while(rs.next())
					{
						if (isCancelled())
						{
							addWarningMessage("CANCEL Was pressed when reading table '" + td.getFullName(false) + "', after " + row + " rows.");
							break;
						}
							
						row++;
						totalRowCount++;

						td._rowsRead = row;

						// Progress report
						if ( row < 100 ||  System.currentTimeMillis() - lastProgresUpdate > progresUpdatePeriodMs )
						{
							String msg = "at row " + nf.format(row) + ", ";

							// Add percent done
							if (td._estRowCount > 0)
							{
								int pctDone = (int) ( ((row*1.0) / (td._estRowCount*1.0)) * 100.0 );
								msg = "at row " + nf.format(row) + " (" + pctDone + "%) of " + nf.format(td._estRowCount) + " Estimated rows, ";
							}

							setProgressState("Getting data " + msg + " for table" + tableProgress + ": " + td.getFullName(false));
							
							if (totalEstimateRowCount > 0)
							{
//								int pctDone = (int) ( ((totalRowCount*1.0) / (totalEstimateRowCount*1.0)) * 100.0 );
								BigDecimal pctDone = new BigDecimal( ( ((totalRowCount*1.0) / (totalEstimateRowCount*1.0)) * 100.0 ) ).setScale(1, RoundingMode.HALF_EVEN);
								setProgressState2("Total " + pctDone + "% done. Total Rows read " + nf.format(totalRowCount) + ". Estimated Rows to read " + nf.format(totalEstimateRowCount) + ".");
							}

							lastProgresUpdate = System.currentTimeMillis();
						}

						int rowIssueCount    = 0;
						int rowUtf8GrowCount = 0;

						// for each column in source set it to the output
						for (int sqlPos=1; sqlPos<=numCols; sqlPos++)
						{
							String colName = colNames.get(sqlPos - 1);
							
							if (td.isPk(colName))
							{
							}
							else
							{
								String colValue = rs.getString(sqlPos);
								if (colValue == null)
									continue; // get next column

								TableColumnDetails tcd = td.getColumn(colName);

								int destColumnMaxLength = tcd._colLength;
								int utf8Len             = StringUtil.utf8Length(colValue);
								int strLen              = colValue.length();

								// Remember MAX length
								tcd._maxSrLength   = Math.max(tcd._maxSrLength,   strLen);
								tcd._maxUtf8Length = Math.max(tcd._maxUtf8Length, utf8Len);

								if (utf8Len > strLen)
								{
									tcd._utf8GrowCount++;
									rowUtf8GrowCount++;
								}
									
								if (utf8Len > destColumnMaxLength)
								{
									tcd._utf8IssueCount++;
									rowIssueCount++;
									
									// build a list of PrimaryKey values for this row
									List<String> pkValues = new ArrayList<>();
									for (String pkColName : td._pkCols)
										pkValues.add(rs.getString(pkColName));

									
									// ADD the "Exception Value" to the list
									ColumnExceptionValue cev = new ColumnExceptionValue();
									cev._row    = row;
									cev._tcd    = tcd;
									cev._pkVals = pkValues;

									cev._originVal        = colValue;
									cev._originSrLength   = colValue.length();
									cev._originUtf8Length = utf8Len;

									cev._truncVal         = StringUtil.utf8Truncate(colValue, destColumnMaxLength);;
									cev._truncSrLength    = cev._truncVal.length();
									cev._truncUtf8Length  = StringUtil.utf8Length(cev._truncVal);
									
									tcd._problemValueList.add(cev);
								}
							}
						} // end: col
						
						if (rowIssueCount    > 0) td._rowIssueCount++;
						if (rowUtf8GrowCount > 0) td._rowUtf8GrowCount++;

					} // end: rs.next()
				} // end: try(ResultSet)
			} // end: try(Statement)
			catch(SQLException ex)
			{
				String msg = "Problems executing sql '" + sql + "', Caught: Err=" + ex.getErrorCode() + ", State='" + ex.getSQLState() + "', msg='" + ex.getMessage().trim() + "'.";
				
				_logger.error(msg);
				addErrorMessage(msg);

				throw ex;
			}
			finally
			{
				//----------------------
				// RESTORE AutoCommit...
				if (_conn != null && connDoFetchInTransaction)
					_conn.setAutoCommit(connOriginAutoCommit);
				
			}
		}

		// Some summary messages for ALL Table selects
		if (msgFetchSize != null)
			addInfoMessage(msgFetchSize);

			
		setProgressState("Done, now making report ResultSet(s)");



		// SHOW SKIPPED TABLES
		if (_params._showSkippedTables)
		{
			ResultSetTableModel rstm = ResultSetTableModel.createEmpty("utf8Length.skippedTables");

			int col = 0;
			rstm.addColumn("catalog"     , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
			rstm.addColumn("schema"      , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
			rstm.addColumn("table"       , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
			
			for (TableDetails td : _usedTableSet)
			{
				ArrayList<Object> row = new ArrayList<Object>();

				row.add(td._catalogName);
				row.add(td._schemaName);
				row.add(td._tableName);
			}
			_resultCompList.add(new JAseMessage("Below is a table, which was skipped due: to it had NO columns that contains string columns.", _originCmd));
			_resultCompList.add(new JTableResultSet(rstm));
		}


		
		// Show TABLE Summary 
		if (_params._showTableSummary)
		{
			ResultSetTableModel rstm = ResultSetTableModel.createEmpty("utf8Length.tableSummary");
			
			int col = 0;
			rstm.addColumn("catalog"         , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String .class);
			rstm.addColumn("schema"          , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String .class);
			rstm.addColumn("table"           , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String .class);
			rstm.addColumn("rowsRead"        , col++, Types.INTEGER, "int",     "int"         ,   0, 0, -1, Integer.class);
			rstm.addColumn("rowIssueCount"   , col++, Types.INTEGER, "int",     "int"         ,   0, 0, -1, Integer.class);
			rstm.addColumn("rowUtf8GrowCount", col++, Types.INTEGER, "int",     "int"         ,   0, 0, -1, Integer.class);
			rstm.addColumn("checkedCols"     , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String .class);
			rstm.addColumn("colIssueCount"   , col++, Types.INTEGER, "int",     "int"         ,   0, 0, -1, Integer.class);
			rstm.addColumn("colUtf8GrowCount", col++, Types.INTEGER, "int",     "int"         ,   0, 0, -1, Integer.class);
			
			String colorStr;
			colorStr = Configuration.getCombinedConfiguration().getProperty("utf8Length.hasIssues.color");
			rstm.addHighlighter(new ColorHighlighter(new HighlightPredicate()
			{
				@Override
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int mcol = adapter.convertColumnIndexToModel(adapter.column);
					String colName = adapter.getColumnName(mcol);
					
					if ("rowIssueCount".equals(colName) || "colIssueCount".equals(colName))
					{
						Number val = (Number) adapter.getValue();
						if ( val != null && val.intValue() > 0 )
							return true;
					}
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));

			colorStr = Configuration.getCombinedConfiguration().getProperty("utf8Length.maxUtf8Len.color");
			rstm.addHighlighter(new ColorHighlighter(new HighlightPredicate()
			{
				@Override
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int mcol = adapter.convertColumnIndexToModel(adapter.column);
					String colName = adapter.getColumnName(mcol);
					
					if ("rowUtf8GrowCount".equals(colName) || "colUtf8GrowCount".equals(colName))
					{
						Number val = (Number) adapter.getValue();
						if ( val != null && val.intValue() > 0 )
							return true;
					}
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.YELLOW), null));
			
			
			
			for (TableDetails td : _usedTableSet)
			{
				ArrayList<Object> row = new ArrayList<Object>();

//				int rowIssueCount = 0; 
				int colIssueCount = 0; 
				for (TableColumnDetails tcd : td._columnDetails)
					colIssueCount += tcd._utf8IssueCount;

//				int rowUtf8GrowCount = 0; 
				int colUtf8GrowCount = 0; 
				for (TableColumnDetails tcd : td._columnDetails)
					colUtf8GrowCount += tcd._utf8GrowCount;

				row.add(td._catalogName);
				row.add(td._schemaName);
				row.add(td._tableName);
				row.add(td._rowsRead);
				row.add(td._rowIssueCount);
				row.add(td._rowUtf8GrowCount);
				row.add(td._columnDetails.size());
				row.add(colIssueCount);
				row.add(colUtf8GrowCount);

				rstm.addRow(row);
			}

			_resultCompList.add(new JAseMessage("Below is a TABLE Summary. \n"
					+ "Check column 'colIssueCount' (marked with orange) for issues (how many column values had a UTF-8 length that was above the column MAX Size). \n"
					+ "The column 'colUtf8GrowCount' indicates how many rows had 'utf8Length' larger than 'strLength' (how many column values had a UTF-8 chars that was above 1 byte). \n"
					+ "The column 'rowIssueCount' and 'rowUtf8GrowCount' is the same as abobe, but on a row level (how many rows has issues or growCount).\n"
					+ "See Column details in next section.", _originCmd));
			_resultCompList.add(new JTableResultSet(rstm));
		}


		// Always SHOW all columns with STRING columns 
		if (_params._showColumnReport)
		{
			ResultSetTableModel rstm = ResultSetTableModel.createEmpty("utf8Length.columnReport");
			
			int col = 0;
			rstm.addColumn("catalog"      , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String .class);
			rstm.addColumn("schema"       , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String .class);
			rstm.addColumn("table"        , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String .class);
			rstm.addColumn("rowsRead"     , col++, Types.INTEGER, "int",     "int"         ,   0, 0, -1, Integer.class);
			rstm.addColumn("column"       , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String .class);
			rstm.addColumn("columnPos"    , col++, Types.INTEGER, "int",     "int"         ,   0, 0, -1, Integer.class);
			rstm.addColumn("colJdbcType"  , col++, Types.INTEGER, "int",     "int"         ,   0, 0, -1, Integer.class);
			rstm.addColumn("colJdbcName"  , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String .class);
			rstm.addColumn("colDbmsType"  , col++, Types.VARCHAR, "varchar", "varchar(30)" ,  30, 0, "", String .class);
			rstm.addColumn("issueCount"   , col++, Types.INTEGER, "int",     "int"         ,   0, 0, -1, Integer.class);
			rstm.addColumn("utf8GrowCount", col++, Types.INTEGER, "int",     "int"         ,   0, 0, -1, Integer.class);
			rstm.addColumn("colLength"    , col++, Types.INTEGER, "int",     "int"         ,   0, 0, -1, Integer.class);
			rstm.addColumn("maxStrLen"    , col++, Types.INTEGER, "int",     "int"         ,   0, 0, -1, Integer.class);
			rstm.addColumn("maxUtf8Len"   , col++, Types.INTEGER, "int",     "int"         ,   0, 0, -1, Integer.class);
			
			String colorStr;
			colorStr = Configuration.getCombinedConfiguration().getProperty("utf8Length.hasIssues.color");
			rstm.addHighlighter(new ColorHighlighter(new HighlightPredicate()
			{
				@Override
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int mcol = adapter.convertColumnIndexToModel(adapter.column);
					String colName = adapter.getColumnName(mcol);
					
					if ("issueCount".equals(colName))
					{
						Number val = (Number) adapter.getValue();
						if ( val != null && val.intValue() > 0 )
							return true;
					}
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));
			
			colorStr = Configuration.getCombinedConfiguration().getProperty("utf8Length.maxUtf8Len.color");
			rstm.addHighlighter(new ColorHighlighter(new HighlightPredicate()
			{
				@Override
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int mcol = adapter.convertColumnIndexToModel(adapter.column);
					String colName = adapter.getColumnName(mcol);
					
					if ("maxUtf8Len".equals(colName))
					{
						Number maxStrLen  = (Number) adapter.getValue(adapter.getColumnIndex("maxStrLen"));
						Number maxUtf8Len = (Number) adapter.getValue();
						if ( maxStrLen != null && maxUtf8Len != null && maxUtf8Len.intValue() > maxStrLen.intValue() )
							return true;
					}
					if ("utf8GrowCount".equals(colName))
					{
						Number val = (Number) adapter.getValue();
						if ( val != null && val.intValue() > 0 )
							return true;
					}
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.YELLOW), null));
			
			
			for (TableDetails td : _usedTableSet)
			{
				for (TableColumnDetails tcd : td._columnDetails)
				{
					ArrayList<Object> row = new ArrayList<Object>();
					row.add(td._catalogName);
					row.add(td._schemaName);
					row.add(td._tableName);
					row.add(td._rowsRead);
					row.add(tcd._colName);
					row.add(tcd._colPos);
					row.add(tcd._colJdbcType);
					row.add(ResultSetTableModel.getColumnJavaSqlTypeName(tcd._colJdbcType));
					row.add(tcd._colType);
					row.add(tcd._utf8IssueCount);
					row.add(tcd._utf8GrowCount);
					row.add(tcd._colLength);
					row.add(tcd._maxSrLength);
					row.add(tcd._maxUtf8Length);
					
					rstm.addRow(row);
				}
			}

			_resultCompList.add(new JAseMessage("Below is a table with COLUMNS that CONTAINS string columns. \n"
					+ "Check column 'issueCount' (marked with orange) for issues. See column 'colJdbcName' for what JDBC Type is is. \n"
					+ "Note: Use 'maxUtf8Len' (marked in yellow, if longer than 'maxStrLen') to figgure out the new Maximum Column length for a UTF-8 column specification.", _originCmd));
			_resultCompList.add(new JTableResultSet(rstm));
		}

		// SHOW VALUE DETAILS
		if (_params._showRowValDetails)
		{
			ResultSetTableModel rstm = ResultSetTableModel.createEmpty("utf8Length.utf8ProblemValues");

			int col = 0;
			rstm.addColumn("catalog"       , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String .class);
			rstm.addColumn("schema"        , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String .class);
			rstm.addColumn("table"         , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String .class);
			rstm.addColumn("rowNumber"     , col++, Types.INTEGER, "int",     "int"         ,   0, 0, -1, Integer.class);
			rstm.addColumn("pkCols"        , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String .class);
			rstm.addColumn("pkValues"      , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String .class);
			rstm.addColumn("column"        , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String .class);

			rstm.addColumn("originVal"     , col++, Types.VARCHAR, "text",    "text"      , 32768, 0, "", String .class);
			rstm.addColumn("originSrLen"   , col++, Types.INTEGER, "int",     "int"         ,   0, 0, -1, Integer.class);
			rstm.addColumn("originUtf8Len" , col++, Types.INTEGER, "int",     "int"         ,   0, 0, -1, Integer.class);

			rstm.addColumn("truncVal"      , col++, Types.VARCHAR, "text",    "text"      , 32768, 0, "", String .class);
			rstm.addColumn("truncSrLen"    , col++, Types.INTEGER, "int",     "int"         ,   0, 0, -1, Integer.class);
			rstm.addColumn("truncUtf8Len"  , col++, Types.INTEGER, "int",     "int"         ,   0, 0, -1, Integer.class);

			for (TableDetails td : _usedTableSet)
			{
				for (TableColumnDetails tcd : td._columnDetails)
				{
					if (tcd._utf8IssueCount == 0)
						continue;

					for (ColumnExceptionValue cev : tcd._problemValueList)
					{
						ArrayList<Object> row = new ArrayList<Object>();
						
						row.add(td._catalogName);
						row.add(td._schemaName);
						row.add(td._tableName);
						row.add(cev._row);
						row.add(StringUtil.toCommaStr(td ._pkCols));
						row.add(StringUtil.toCommaStr(cev._pkVals));
						row.add(tcd._colName);
						
						row.add(cev._originVal);
						row.add(cev._originSrLength);
						row.add(cev._originUtf8Length);
						
						row.add(cev._truncVal);
						row.add(cev._truncSrLength);
						row.add(cev._truncUtf8Length);
						
						rstm.addRow(row);
					}
				}
			}
			_resultCompList.add(new JAseMessage("Below is a table with columns VALUES that will NOT fit into the destination table if the DBMS uses Byte Length Storage for UTF-8 values. \n"
					+ "Both the 'originVal' and the potentially 'truncVal' is presented, along with 'rowNumber' and 'pkCols/pkValues'", _originCmd));
			_resultCompList.add(new JTableResultSet(rstm));
		}

		return false;
	}

	private boolean listOnly()
	{
		// SHOW SKIPPED TABLES
		if (_params._showSkippedTables)
		{
			ResultSetTableModel rstm = ResultSetTableModel.createEmpty("utf8Length.listOnly.skippedTables");

			int col = 0;
			rstm.addColumn("catalog"     , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
			rstm.addColumn("schema"      , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
			rstm.addColumn("table"       , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
			
			for (TableDetails td : _usedTableSet)
			{
				ArrayList<Object> row = new ArrayList<Object>();

				row.add(td._catalogName);
				row.add(td._schemaName);
				row.add(td._tableName);
			}
			_resultCompList.add(new JAseMessage("Below is a table, which was skipped due: to it had NO columns that contains string columns.", _originCmd));
			_resultCompList.add(new JTableResultSet(rstm));
		}


		
		// Always SHOW all columns with STRING columns 
		if (true)
		{
			ResultSetTableModel rstm = ResultSetTableModel.createEmpty("utf8Length.listOnly.tables");
			
			int col = 0;
			rstm.addColumn("catalog"      , col++, Types.VARCHAR, "varchar", "varchar(255)" ,  255, 0, "", String .class);
			rstm.addColumn("schema"       , col++, Types.VARCHAR, "varchar", "varchar(255)" ,  255, 0, "", String .class);
			rstm.addColumn("table"        , col++, Types.VARCHAR, "varchar", "varchar(255)" ,  255, 0, "", String .class);
			rstm.addColumn("estimatedRows", col++, Types.INTEGER, "int",     "int"          ,    0, 0, -1, Integer.class);
			rstm.addColumn("colsToCheck"  , col++, Types.INTEGER, "int",     "int"          ,    0, 0, -1, Integer.class);
			rstm.addColumn("pkCols"       , col++, Types.VARCHAR, "varchar", "varchar(255)" ,  255, 0, "", String .class);
			rstm.addColumn("sqlToExec"    , col++, Types.VARCHAR, "varchar", "varchar(1024)", 1024, 0, "", String .class);
			
			for (TableDetails td : _usedTableSet)
			{
				ArrayList<Object> row = new ArrayList<Object>();
				row.add(td._catalogName);
				row.add(td._schemaName);
				row.add(td._tableName);
				row.add(td._estRowCount);
				row.add(td._columnDetails.size());
				row.add(td._pkCols.isEmpty() ? null : StringUtil.toCommaStr(td._pkCols));
				row.add(_conn.quotifySqlString(td.getSelectSql()));
				
				rstm.addRow(row);
			}

			_resultCompList.add(new JAseMessage("Below is a list of tables that will be checked.", _originCmd));
			_resultCompList.add(new JTableResultSet(rstm));
		}

		return false;
	}
	

	//-----------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------
	// Below are data classes used in above functionality
	//-----------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------

	private static class TableDetails
	{
		SqlStatementAbstract _sqlStatementAbstract;
		String _catalogName;
		String _schemaName ;
		String _tableName  ;

		long   _estRowCount = -1;
		long   _rowsRead;
		int    _rowUtf8GrowCount;
		int    _rowIssueCount;
		
		List<String> _pkCols;
		List<TableColumnDetails> _columnDetails = new ArrayList<>();

		public TableDetails(SqlStatementAbstract sqlStatementAbstract)
		{
			_sqlStatementAbstract = sqlStatementAbstract;
		}

		public String getFullName(boolean quotedIdentifiers)
		{
			return _sqlStatementAbstract.getSourceConnection().getFullTableName(_catalogName, _schemaName, _tableName, quotedIdentifiers);
//			String fullName = "";
//
//			if (StringUtil.hasValue(_catalogName)) fullName += _catalogName + ".";
//			if (StringUtil.hasValue(_schemaName) ) fullName += _schemaName  + ".";
//			fullName += _tableName;
//
//			return fullName;
		}
		
		public boolean isPk(String colName)
		{
			return _pkCols.contains(colName);
		}
		
		public TableColumnDetails getColumn(String colName)
		{
			for (TableColumnDetails tcd : _columnDetails)
			{
				if (colName.equals(tcd._colName))
					return tcd;
			}
			return null;
		}

		/** produce: SELECT col, col2, col3 FROM cat.schema.table */
		public String getSelectSql()
		{
			StringBuilder sb = new StringBuilder();
			String comma = "";
			
			sb.append("SELECT ");
			// PK Columns so we can derive the data not only from a row number 
			for (String pkColName : _pkCols)
			{
				sb.append(comma).append("[").append(pkColName).append("]");
				comma = ", ";
			}
			// VALUE Columns
			for (TableColumnDetails c : _columnDetails)
			{
				// If the column is part of the PK, then it's already listed... no need to get it again
				if (_pkCols.contains(c._colName))
					continue;
				
				sb.append(comma).append("[").append(c._colName).append("]");
				comma = ", ";
			}
			sb.append(" \n");
			sb.append("FROM ");
			sb.append(_sqlStatementAbstract.getSourceConnection().getFullTableName(_catalogName, _schemaName, _tableName, true));
			
			return sb.toString();
		}
		

		/** generated by Eclipse */
		@Override
		public int hashCode()
		{
			final int prime  = 31;
			int       result = 1;
			result = prime * result + ((_catalogName == null) ? 0 : _catalogName.hashCode());
			result = prime * result + ((_schemaName == null) ? 0 : _schemaName.hashCode());
			result = prime * result + ((_tableName == null) ? 0 : _tableName.hashCode());
			return result;
		}

		/** generated by Eclipse */
		@Override
		public boolean equals(Object obj)
		{
			if ( this == obj )
				return true;
			if ( obj == null )
				return false;
			if ( getClass() != obj.getClass() )
				return false;
			TableDetails other = (TableDetails) obj;
			if ( _catalogName == null )
			{
				if ( other._catalogName != null )
					return false;
			}
			else if ( !_catalogName.equals(other._catalogName) )
				return false;
			if ( _schemaName == null )
			{
				if ( other._schemaName != null )
					return false;
			}
			else if ( !_schemaName.equals(other._schemaName) )
				return false;
			if ( _tableName == null )
			{
				if ( other._tableName != null )
					return false;
			}
			else if ( !_tableName.equals(other._tableName) )
				return false;
			return true;
		}
	}

	private static class TableColumnDetails
	{
		TableDetails _tableDetails;

		String _colName    ;
		int    _colPos     ;
		int    _colJdbcType;
		String _colType    ;
		int    _colLength  ;

		int _utf8GrowCount;  // number of rows that had UTF8 length larger than STR Length
		int _utf8IssueCount;

		int _maxSrLength;
		int _maxUtf8Length;

		List<ColumnExceptionValue> _problemValueList = new ArrayList<>();
	}

	private static class ColumnExceptionValue
	{
		TableColumnDetails _tcd;

		int _row;

		List<String> _pkVals;

		String _originVal; 
		int    _originSrLength;
		int    _originUtf8Length;

		String _truncVal; 
		int    _truncSrLength;
		int    _truncUtf8Length;
	}

}

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
package com.asetune.ui.autocomplete;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.asetune.Version;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.ConnectionProfile;
import com.asetune.gui.ConnectionProfileManager;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.SqlTextDialog;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.sql.SqlObjectName;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.OracleConnection;
import com.asetune.tools.ddlgen.DdlGen;
import com.asetune.tools.ddlgen.DdlGen.Type;
import com.asetune.tools.sqlw.QueryWindow;
import com.asetune.ui.autocomplete.completions.AbstractCompletionX;
import com.asetune.ui.autocomplete.completions.DbInfo;
import com.asetune.ui.autocomplete.completions.FunctionColumnInfo;
import com.asetune.ui.autocomplete.completions.FunctionInfo;
import com.asetune.ui.autocomplete.completions.ProcedureInfo;
import com.asetune.ui.autocomplete.completions.ProcedureParameterInfo;
import com.asetune.ui.autocomplete.completions.SchemaInfo;
import com.asetune.ui.autocomplete.completions.ShorthandCompletionX;
import com.asetune.ui.autocomplete.completions.SqlColumnCompletion;
import com.asetune.ui.autocomplete.completions.SqlCompletion;
import com.asetune.ui.autocomplete.completions.SqlDbCompletion;
import com.asetune.ui.autocomplete.completions.SqlFunctionCompletion;
import com.asetune.ui.autocomplete.completions.SqlProcedureCompletion;
import com.asetune.ui.autocomplete.completions.SqlSchemaCompletion;
import com.asetune.ui.autocomplete.completions.SqlTableCompletion;
import com.asetune.ui.autocomplete.completions.TableColumnInfo;
import com.asetune.ui.autocomplete.completions.TableInfo;
import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.ui.tooltip.suppliers.ToolTipSupplierAbstract;
import com.asetune.utils.CollectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.TimeUtils;

import net.miginfocom.swing.MigLayout;

public abstract class CompletionProviderAbstractSql
extends CompletionProviderAbstract
{
	private static Logger _logger = Logger.getLogger(CompletionProviderAbstractSql.class);
	
	
	public final static String  PROPKEY_PREFIX_alwaysQuoteTableNames  = "sqlw.CompletionProviderAbstractSql.always.quote.table.names.";
	public final static boolean DEFAULT_alwaysQuoteTableNames         = false;
	
	public final static String  PROPKEY_PREFIX_alwaysQuoteColumnNames = "sqlw.CompletionProviderAbstractSql.always.quote.column.names.";
	public final static boolean DEFAULT_alwaysQuoteColumnNames        = false;


	public CompletionProviderAbstractSql(Window owner, ConnectionProvider connectionProvider)
	{
		super(owner, connectionProvider);
	}

	protected Set<String>                   _schemaNames         = new HashSet<String>();

	protected List<DbInfo>                  _dbInfoList          = new ArrayList<DbInfo>();
	protected List<SqlDbCompletion>         _dbComplList         = new ArrayList<SqlDbCompletion>();

	protected List<TableInfo>               _tableInfoList       = new ArrayList<TableInfo>();
	protected List<SqlTableCompletion>      _tableComplList      = new ArrayList<SqlTableCompletion>();

	protected List<FunctionInfo>            _functionInfoList    = new ArrayList<FunctionInfo>();
	protected List<SqlFunctionCompletion>   _functionComplList   = new ArrayList<SqlFunctionCompletion>();

	protected List<ProcedureInfo>           _procedureInfoList   = new ArrayList<ProcedureInfo>();
	protected List<SqlProcedureCompletion>  _procedureComplList  = new ArrayList<SqlProcedureCompletion>();

	protected List<ProcedureInfo>           _systemProcInfoList  = new ArrayList<ProcedureInfo>();
	protected List<SqlProcedureCompletion>  _systemProcComplList = new ArrayList<SqlProcedureCompletion>();
	
	/** put quotes around the "tableNames" */
	protected boolean _quoteTableNames              = DEFAULT_alwaysQuoteTableNames;
	protected boolean _quoteColumnNames             = DEFAULT_alwaysQuoteColumnNames;
	protected boolean _addSchemaName                = true;

	protected String  _dbProductName                = "";
	protected String  _dbExtraNameCharacters        = "";
	protected String  _dbIdentifierQuoteString      = "\"";
//	protected String  _dbIdentifierQuoteStringStart = "\""; // FIXME Implement this EVERYWHERE (for SQL_Server and Sybase it should be '[') so we do not have to relay on "set quoted identifier on"
//	protected String  _dbIdentifierQuoteStringEnd   = "\""; // FIXME Implement this EVERYWHERE (for SQL_Server and Sybase it should be ']') so we do not have to relay on "set quoted identifier on"
//	protected String[] _dbIdentifierQuoteString     = new String[] {"\"", "\""}; // Or we can use an ARRAY with 2 fields
	protected boolean _dbStoresUpperCaseIdentifiers = false;
	protected boolean _dbSupportsSchema             = true; // so far it's only MySQL that do not support schema it uses the catalog as schemas...
	
	protected String _currentCatalog                = null;
	protected String _currentServerName             = null;

	private DbxConnection _localConn        = null; 
	private String        _localCatalogName = null; 

	@Override
	public void disconnect()
	{
		super.disconnect();

		_quoteTableNames         = DEFAULT_alwaysQuoteTableNames;
		_quoteColumnNames        = DEFAULT_alwaysQuoteColumnNames;
		_addSchemaName           = true;

		_dbProductName                = "";
		_dbExtraNameCharacters        = "";
		_dbIdentifierQuoteString      = "\"";
		_dbStoresUpperCaseIdentifiers = false;
		_dbSupportsSchema             = true;

		_currentCatalog          = null;
		_currentServerName       = null;

		_schemaNames             .clear();
		_dbInfoList              .clear();
		_dbComplList             .clear();
		_tableInfoList           .clear();
		_tableComplList          .clear();
		_functionInfoList        .clear();
		_functionComplList       .clear();
		_procedureInfoList       .clear();
		_procedureComplList      .clear();
		_systemProcInfoList      .clear();
		_systemProcComplList     .clear();

		if (_localConn != null)
		{
			try { _localConn.close(); }
			catch(SQLException ignore) {}
			_localCatalogName = null;
		}
	}

	public void addSqlCompletion(Completion c)
	{
		super.addCompletion(c);
	}
//	public void addSqlCompletions(List<SqlTableCompletion> list)
//	{
//		super.addCompletions(list);
//	}
	public void addSqlCompletions(List<? extends Completion> list)
	{
		// hmmm addCompletion, seems to sort the list, which takes *a*long*time*
//		for (Completion c : list)
//			super.addCompletion(c);

		// to workaround that super.addCompletions() is not defined as <? extends Completion>
		// move the records into a new list...
		// the add the list to the super
		List<Completion> cList = new ArrayList<Completion>();
		for (Completion c : list)
			cList.add(c);
		super.addCompletions(cList);
		
//		super.addCompletions(list);
	}

	public void setCatalog(String catName)
	{
		setCatalog(catName, false);
	}
	public void setCatalog(String catName, boolean withOverride)
	{
		//System.out.println("CompletionProviderAbstractSql: setCatalog("+catName+", withOverride="+withOverride+"), _localCatalogName='"+_localCatalogName+"'.");
		if (StringUtil.isNullOrBlank(catName))
			return;

		
		if ( ! withOverride )
		{
    		if (_localCatalogName != null && _localCatalogName.equals(catName))
    			return;
		}
			
		// Set this early, if the CompletionProvider isn't yet used; we will set it when first connection is made.
		_localCatalogName = catName;

		if (_localConn == null)
			return;

		_logger.info("Setting local catalog to '"+_localCatalogName+"' in Completion Provider.");
		try 
		{
			setNeedRefresh(true);
			_localConn.setCatalog(catName);
		}
		catch(SQLException ex) 
		{ 
			_logger.error("Problems setting catalog name to '"+_localCatalogName+"'. Caught: "+ex);
		}
	}

	public void releaseConnection()
	{
		if (_localConn == null)
			return;
		
		//_connectionProvider.releaseConnection(_localConn);
		try { _logger.info("Closing connection to: "+_localConn.getMetaData().getURL()); }
		catch(SQLException ignore) {}

		// Close and reset the connection
		try { _localConn.close(); }
		catch(SQLException ex) {}
		_localConn = null;
	}
	
	public DbxConnection getConnection()
	{
		if (isCreateLocalConnection())
		{
			if (_localConn == null)
			{
				_localConn = _connectionProvider.getNewConnection(Version.getAppName() + "-Compl"); // '-Completion' was a bit to long, it may truncate the version part at the end 
				
				try { _logger.info("Compleation Provider created a new connection to URL: "+_localConn.getMetaData().getURL()); }
				catch(SQLException ignore) {}
				
				setCatalog(_localCatalogName, true); // withOverride=true
			}
			
			if (_localConn != null)
			{
				if ( ! _localConn.isConnectionOk(false, _guiOwner) )
				{
					try
					{
						_localConn.reConnect(_guiOwner);
					}
					catch(Exception ex)
					{
						_logger.error("Problems in Code Compleation: When trying to re-connect to DBMS there was problems. Caught: "+ex);
					}
				}
			}
			
			return _localConn;
		}
		else
		{
			return _connectionProvider.getConnection();
		}
	}

	@Override
	public String getDbProductName()
	{
		if (StringUtil.isNullOrBlank(_dbProductName))
		{
//			Connection conn = _connectionProvider.getConnection();
			DbxConnection conn = getConnection();
			if (conn != null)
			{
//				try { _dbProductName = ConnectionDialog.getDatabaseProductName(conn); }
				try { _dbProductName = conn.getDatabaseProductName(); }
				catch (SQLException ignore) {}
			}
		}
		return _dbProductName;
	}

//	@Override
	public String getDbServerName()
	{
		if (StringUtil.isNullOrBlank(_currentServerName))
		{
//			Connection conn = _connectionProvider.getConnection();
			DbxConnection conn = getConnection();
			if (conn != null)
			{
//				_currentServerName = DbUtils.getDatabaseServerName(conn, getDbProductName());
				try { _currentServerName = conn.getDbmsServerName(); }
				catch (SQLException ignore) {}
			}
		}
		return _currentServerName;
	}

//	@Override
	public String getDbCatalogName()
	{
//		Connection conn = _connectionProvider.getConnection();
		DbxConnection conn = getConnection();
		if (conn != null)
		{
			try { _currentCatalog = conn.getCatalog(); }
			catch (SQLException ignore) {}
		}
		return _currentCatalog;
	}
	
	
	public String getDbExtraNameCharacters()
	{
		return _dbExtraNameCharacters;
	}
	public String getDbIdentifierQuoteString()
	{
		return _dbIdentifierQuoteString;
	}
	public boolean getDbStoresUpperCaseIdentifiers()
	{
		return _dbStoresUpperCaseIdentifiers;
	}
	public boolean getDbSupportsSchema()
	{
		return _dbSupportsSchema;
	}


//	/**
//	 * Returns whether the specified token is a single non-word char (e.g. not
//	 * in <tt>[A-Za-z]</tt>.  This is a HACK to work around the fact that many
//	 * standard token makers return things like semicolons and periods as
//	 * {@link Token#IDENTIFIER}s just to make the syntax highlighting coloring
//	 * look a little better.
//	 * 
//	 * @param t The token to check.  This cannot be <tt>null</tt>.
//	 * @return Whether the token is a single non-word char.
//	 */
//	private static final boolean isNonWordChar(Token t) 
//	{
//		return t.textCount==1 && !RSyntaxUtilities.isLetter(t.text[t.textOffset]);
//	}
//	/**
//	 * Returns whether the specified token is a type that we can do a
//	 * "mark occurrences" on.
//	 *
//	 * @param t The token.
//	 * @return Whether we should mark all occurrences of this token.
//	 */
//	private boolean isValidType(RSyntaxTextArea textArea, Token t) 
//	{
//		TokenMaker tmaker = TokenMakerFactory.getDefaultInstance().getTokenMaker(textArea.getSyntaxEditingStyle());
//		return tmaker.getMarkOccurrencesOfTokenType(t.type);
//	}


//	private void findXxx(TGSqlParser sqlParser, int dot)
//	{
////		dot++; // TextComponents starts at 0, while parser tokens starts at 1
//		TSourceToken atToken = null;
//
//		TSourceTokenList tl = sqlParser.sourcetokenlist;
//		for (int i=0; i<tl.size(); i++)
//		{
//			TSourceToken tt = tl.get(i);
//			TSourceToken tn = (i+1 < tl.size()) ? tl.get(i+1) : null;
//			if (dot >= tt.offset)
//			{
//				atToken = tt;
//				System.out.println("---thisToken(dot="+dot+")--- line="+atToken.lineNo+", col="+atToken.columnNo+", offset="+atToken.offset+", posinlist="+atToken.posinlist+", tokencode="+atToken.tokencode+", t='"+atToken+"', stmt='"+atToken.stmt+"'.");
//				if ( tn != null && dot < tn.offset )
//					break;
//			}
//
////			System.out.println(">>> line="+t.lineNo+", col="+t.columnNo+", offset="+t.offset+", posinlist="+t.posinlist+", t='"+t+"', stmt='"+t.stmt+"'.");
//		}
//		if (atToken != null)
//		{
//			System.out.println(">>>atToken(dot="+dot+")>>> line="+atToken.lineNo+", col="+atToken.columnNo+", offset="+atToken.offset+", posinlist="+atToken.posinlist+", t='"+atToken+"', stmt='"+atToken.stmt+"'.");
//
//			TCustomSqlStatement stmt = atToken.stmt; 
//			switch (stmt.sqlstatementtype)
//			{
////			case sstupdate: 
////			case sstdelete: 
//			case sstselect:
//				{
//					TSelectSqlStatement select = (TSelectSqlStatement) stmt;
//
//					int          tables   = select.tables.size();
//					TWhereClause tWhere   = select.getWhereClause();
//					TGroupBy     tGroupBy = select.getGroupByClause();
//					TExpression  tHaving  = tGroupBy != null ? tGroupBy.getHavingClause() : null;
//					TOrderBy     tOrderBy = select.getOrderbyClause();
//
//					if ( tables > 0 )
//					{
//						System.out.println("HAS TABLES");
//						for (int i = 0; i < select.tables.size(); i++)
//						{
//							TTable table = select.tables.getTable(i);
//							if ( table.isBaseTable() )
//								System.out.println("   Table name: " + table.getTableName());
//							else
//							{
//								String table_caption = "" + table.getTableType();
//								if ( table.getAliasClause() != null )
//									table_caption += " " + table.getAliasClause();
//								System.out.println("   Table source: " + table_caption);
//							}
//						}
//					}
//					if ( tWhere != null )
//					{
//						System.out.println("HAS WHERE");
//						System.out.println("StartToke: "+tWhere.getStartToken());
//						System.out.println("EndToken:  "+tWhere.getEndToken());
//					}
//
//					if ( tGroupBy != null )
//					{
//						System.out.println("HAS GROUP BY");
//						System.out.println("StartToke: "+tGroupBy.getStartToken());
//						System.out.println("EndToken:  "+tGroupBy.getEndToken());
//						if ( tHaving != null )
//						{
//							System.out.println("HAS HAVING");
//							System.out.println("StartToke: "+tHaving.getStartToken());
//							System.out.println("EndToken:  "+tHaving.getEndToken());
//						}
//					}
//
//					if ( tOrderBy != null )
//					{
//						System.out.println("HAS ORDER BY");
//						System.out.println("StartToke: "+tOrderBy.getStartToken());
//						System.out.println("EndToken:  "+tOrderBy.getEndToken());
//					}
//				}
//				break;
//			default:
//			}
//		}
//		else
//			System.out.println(">>>atToken(dot="+dot+")>>> NOT FOUND");
//	}
//
//	protected static void iterateStmt(TCustomSqlStatement stmt)
//	{
//		System.out.println("===================================================");
//		System.out.println("StatementType: " + stmt.sqlstatementtype);
//		switch (stmt.sqlstatementtype)
//		{
//		case sstselect:
//			printSelect((TSelectSqlStatement) stmt);
//			break;
//		default:
//		}
//		for (int i = 0; i < stmt.getStatements().size(); i++)
//		{
//			iterateStmt(stmt.getStatements().get(i));
//		}
//
////		TSourceTokenList tl = stmt.sourcetokenlist;
////		for (int i=0; i<tl.size(); i++)
////		{
////			TSourceToken t = tl.get(i);
////			System.out.println(">>> line="+t.lineNo+", col="+t.columnNo+", offset="+t.offset+", posinlist="+t.posinlist+", t='"+t+"', stmt='"+stmt+"'.");
//////			stmt.
////			
////		}
//////		System.out.println(":3:getLineNo()='"+table.+"', getColumnNo='"+table.getColumnNo()+"'.");
//
//	}
//
//	private static void printSelect(TSelectSqlStatement select)
//	{
//		if ( select.isCombinedQuery() )
//		{
//			printSelect(select.getLeftStmt());
//			printSelect(select.getRightStmt());
//			return;
//		}
//		System.out.println(":1:getLineNo()='"+select.getLineNo()+"', getColumnNo='"+select.getColumnNo()+"'.");
//		System.out.println("Result Columns:");
//		TResultColumnList trcl = select.getResultColumnList(); 
//		if (trcl != null)
//		{
//			for (int i = 0; i < trcl.size(); i++)
//			{
//				System.out.println(":2:getLineNo()='"+trcl.getLineNo()+"', getColumnNo='"+trcl.getColumnNo()+"'.");
//				System.out.println("  " + i + ": " + trcl.getResultColumn(i));
//				if ( trcl.getResultColumn(i).getExpr() != null )
//				{
//					new columnInClause().printColumns(trcl.getResultColumn(i).getExpr(), select);
//				}
//			}
//		}
//		if ( select.tables.size() > 0 )
//		{
//			for (int i = 0; i < select.tables.size(); i++)
//			{
//				TTable table = select.tables.getTable(i);
//				System.out.println(":3:getLineNo()='"+table.getLineNo()+"', getColumnNo='"+table.getColumnNo()+"'.");
//				System.out.println("getStartToken():"+table.getStartToken());
//				if ( table.isBaseTable() )
//				{
//					System.out.println("Table name: " + table.getTableName());
//				}
//				else
//				{
//					String table_caption = "" + table.getTableType();
//					if ( table.getAliasClause() != null )
//					{
//						table_caption += " " + table.getAliasClause();
//					}
//					System.out.println("Table source: " + table_caption);
//				}
//			}
//		}
//
//		if ( select.getWhereClause() != null )
//		{
//			System.out.println("WHERE clause:");
//			System.out.println("" + select.getWhereClause());
//			new columnInClause().printColumns(select.getWhereClause().getCondition(), select);
//		}
//
//		if ( select.getGroupByClause() != null )
//		{
//			System.out.println("GROUP BY clause:");
//			System.out.println(""+select.getGroupByClause().getItems());
//			new columnInClause().printColumns(select.getGroupByClause().getItems(), select);
//			if ( select.getGroupByClause().getHavingClause() != null )
//			{
//				System.out.println("HAVING clause:");
//				System.out.println(""+select.getGroupByClause().getHavingClause());
//				new columnInClause().printColumns(select.getGroupByClause().getHavingClause(), select);
//			}
//		}
//
//		if ( select.getOrderbyClause() != null )
//		{
//			System.out.println("ORDER BY clause:");
//			System.out.println(""+select.getOrderbyClause());
//			new columnInClause().printColumns(select.getOrderbyClause(), select);
//		}
//	}


//################### BEGIN old parser code #########################################	
//################### Keep this for a bit longer, I will eventually have to use the parsing method later on...	
//	try
//	{
//		SqlDocument doc = SqlDocument.parseDocument(allText);
//		ISqlItem si = doc.getRootItem();
//		System.out.println("xxx="+si);
//		System.out.println("yyy="+si.getSqlItemType());
//		System.out.println("zzz="+si.getSqlText());
//
//		List<ISqlStatement> xxx = doc.getStatementsReadonly();
//		for (ISqlStatement ss : xxx)
//		{
//			System.out.println("SS.sql="+ss.getSqlText());
//			System.out.println("SS.type="+ss.getSqlItemType());
//			System.out.println("SS.GroupType="+ss.getSqlItemGroupType());
//
//			System.out.println("objType="+ss.getClass().getName());
//			if (ss.getSqlItemType() == SqlItemType.SqlSelectStatement)
//			{
//				SqlSelectStatement xx = (SqlSelectStatement) ss;
//				List<SqlTable> tabList = xx.getTablesReadonly();
//				for (SqlTable sqlTable : tabList)
//				{
//					System.out.println("TAB.db="+sqlTable.getDatabase());
//					System.out.println("TAB.name="+sqlTable.getName());
//					System.out.println("TAB.alias="+sqlTable.getAlias());
//					System.out.println("TAB,index="+sqlTable.getIndex());
//					System.out.println("---------------------------------");
//				}
//			}
//			for (SqlAttribute attr : ss.getAttributesReadonly())
//			{
//				System.out.println("ATTR="+attr.getName());
//				System.out.println("ATTR="+attr);
//			}
//		}
//	}
//	catch (SqlParseException pe)
//	{
//		pe.printStackTrace();
//	}
	
//	// Don't do anything if they are selecting text.
//	Caret caret = textArea.getCaret();
////	if (c.getDot()!=c.getMark()) {
////		return;
////	}
//	RSyntaxDocument doc = (RSyntaxDocument)comp.getDocument();
//	//long time = System.currentTimeMillis();
//	doc.readLock();
//	try {
//
//		// Get the token at the caret position.
//		int line = textArea.getCaretLineNumber();
//		Token tokenList = textArea.getTokenListForLine(line);
//		int dot = caret.getDot();
//		Token t = RSyntaxUtilities.getTokenAtOffset(tokenList, dot);
//System.out.println("0: Line="+line+", dot="+dot+", Token.toString(): '"+(t==null ? "NULL" : t.toString())+"'.");
//		if (t==null /* EOL */ || !isValidType(textArea, t) || isNonWordChar(t)) {
//			// Try to the "left" of the caret.
//			dot--;
//			try {
//				if (dot>=textArea.getLineStartOffset(line)) {
//					t = RSyntaxUtilities.getTokenAtOffset(tokenList, dot);
//				}
//			} catch (BadLocationException ble) {
//				ble.printStackTrace(); // Never happens
//			}
//		}
//		System.out.println("1: Line="+line+", dot="+dot+", Token.toString(): '"+(t==null ? "NULL" : t.toString())+"'.");
//
//		// Add new highlights if an identifier is selected.
//		if (t!=null && isValidType(textArea, t) && !isNonWordChar(t)) {
////			removeHighlights();
////			RSyntaxTextAreaHighlighter h = (RSyntaxTextAreaHighlighter) textArea.getHighlighter();
//			char[] lexeme = t.getLexeme().toCharArray();
//			int type = t.type;
//			for (int i=0; i<textArea.getLineCount(); i++) {
//				Token temp = textArea.getTokenListForLine(i);
//				System.out.println("ForLine: "+i+", Token.toString(): '"+temp.toString()+"'.");
//				while (temp!=null && temp.isPaintable()) {
//					if (temp.is(type, lexeme)) {
//						System.out.println("xxxxxxx: Token.toString(): '"+temp.toString()+"'.");
////						try {
//							int end = temp.offset + temp.textCount;
////							h.addMarkedOccurrenceHighlight(temp.offset, end, p);
////						} catch (BadLocationException ble) {
////							ble.printStackTrace(); // Never happens
////						}
//					}
//					temp = temp.getNextToken();
//				}
//			}
////textArea.repaint();
////TODO: Do a textArea.repaint() instead of repainting each marker as it's added if count is huge
//		}
//
//	} finally {
//		doc.readUnlock();
//		//time = System.currentTimeMillis() - time;
//		//System.out.println("MarkOccurrencesSupport took: " + time + " ms");
//	}

//	System.out.println("XXXXXXXXXX: "+textArea.getLineCount());
//
//	Caret caret = textArea.getCaret();
//	RSyntaxDocument doc = (RSyntaxDocument)comp.getDocument();
//	//long time = System.currentTimeMillis();
//	doc.readLock();
//	try 
//	{
//		for (int row=0; row<textArea.getLineCount(); row++)
//		{
//			System.out.println("###################");
////			for (Token tok = tokenList.getNextToken(); tok != null; tok = tok.getNextToken())
//			for (Token t = textArea.getTokenListForLine(row); t != null; t = t.getNextToken())
//			{
//				if (t.type != TokenTypes.NULL)
//				{
//					System.out.println("Line: "+row+", offs="+t.offset+", type='"+AsetuneTokenMaker.getTokenString(t.type)+"', token.getLexeme='"+t.getLexeme()+"'.");
//				}
//			}
//		}
//
//	} 
//	finally 
//	{
//		doc.readUnlock();
//	}

//	System.out.println("!!!! PARSE(General SQL Parser) BEGIN !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//	System.out.println("Parser License: "+TGSqlParser.getLicenseType());
//	System.out.println("Parser License Message: "+TGSqlParser.getLicenseMessage());
//
//	TGSqlParser sqlparser = new TGSqlParser(EDbVendor.dbvsybase);
//	sqlparser.setSqltext(textArea.getText());
////	if (textArea instanceof TextEditorPane)
////		sqlparser.setSqlfilename(((TextEditorPane)textArea).getFileFullPath());
//	int parseRc = sqlparser.parse();
//
//	findXxx(sqlparser, caret.getDot());
//    
//	if (parseRc == 0)
//	{
//		System.out.println("parseRc=0: ---OK----");
//        for(int i=0;i<sqlparser.sqlstatements.size();i++)
//            iterateStmt(sqlparser.sqlstatements.get(i));
//
////		sl.get
////		WhereCondition w = new WhereCondition(sqlparser.sqlstatements.get( 0 ).getWhereClause( ).getCondition( ));
////		w.printColumn();
//	}
//	else
//	{
//		System.out.println("parseRc="+parseRc+": ################################### FAIL FAIL FAIL ###################");
//		System.out.println(sqlparser.getErrormessage( ));
//        for(int i=0;i<sqlparser.sqlstatements.size();i++)
//            iterateStmt(sqlparser.sqlstatements.get(i));
//	}
//	System.out.println("!!!! PARSE(General SQL Parser) END !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	
	
//	System.out.println("!!!! PARSE(JSqlParser) BEGIN !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//	CCJSqlParserManager pm = new CCJSqlParserManager();
//	String sql = "SELECT * FROM MY_TABLE1, MY_TABLE2, (SELECT * FROM MY_TABLE3) LEFT OUTER JOIN MY_TABLE4 "+
//	" WHERE ID = (SELECT MAX(ID) FROM MY_TABLE5) AND ID2 IN (SELECT * FROM MY_TABLE6)" ;
//	sql = textArea.getText();
//	try
//	{
//		net.sf.jsqlparser.statement.Statement statement = pm.parse(new StringReader(sql));
//		if (statement instanceof Select) 
//		{
//			Select selectStatement = (Select) statement;
//			TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
//			List<String> tableList = tablesNamesFinder.getTableList(selectStatement);
//			for (String tableName : tableList)
//			{
//				System.out.println(tableName);
//			}
////			for (Iterator iter = tableList.iterator(); iter.hasNext();) {
////				System.out.println(tableName);
////			}
//		}
//	}
//	catch (JSQLParserException e)
//	{
//		e.printStackTrace();
//	}
////	System.out.println("!!!! PARSE(JSqlParser) END !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//################### BEGIN old parser code #########################################	

	/**
	 * Compose a name that will be used during saving/restoring completions to/from file
	 */
	private String getInstanceName()
	{
		String productName = getDbProductName();
		String serverName  = getDbServerName();
		String catalogName = getDbCatalogName();
		
		if (StringUtil.hasValue(serverName))
			serverName = "." + serverName;
		else
			serverName = "";


		if (StringUtil.hasValue(catalogName))
			catalogName = "." + catalogName;
		else
			catalogName = "";

		return productName + serverName + catalogName;
	}

	/**
	 *  _schemaNames must be populated after a file has been loaded 
	 */
	@Override
	public void loadSavedCacheFromFilePostAction(List<? extends AbstractCompletionX> list, WaitForExecDialog waitDialog)
	{
System.out.println("loadSavedCacheFromFilePostAction: START... list.size()="+ (list == null ? null : list.size()) );
		if (waitDialog != null)
			waitDialog.setState("Adding Schema names...");

		for (AbstractCompletionX compl : list)
		{
//System.out.println("entry: classname="+compl.getClass().getName());
			if (compl instanceof SqlTableCompletion)
			{
				SqlTableCompletion c = (SqlTableCompletion) compl;
				_schemaNames.add(c._tableInfo._tabSchema);
			}
			else if (compl instanceof SqlFunctionCompletion)
			{
				SqlFunctionCompletion c = (SqlFunctionCompletion) compl;
				_schemaNames.add(c._functionInfo._funcSchema);
			}
			else if (compl instanceof SqlProcedureCompletion)
			{
				SqlProcedureCompletion c = (SqlProcedureCompletion) compl;
				_schemaNames.add(c._procInfo._procSchema);
			}
			else if (compl instanceof SqlSchemaCompletion)
			{
				SqlSchemaCompletion c = (SqlSchemaCompletion) compl;
				if (c._schemaInfo != null)
					_schemaNames.add(c._schemaInfo._name);
			}
		}

//		final Connection conn = _connectionProvider.getConnection();
		final DbxConnection conn = getConnection();
		if (conn == null)
		{
			_logger.warn("No connection, can't initiate Mandatory Settings, completions might not work at 100%");
			return;
		}

		if (waitDialog != null)
			waitDialog.setState("Setting Mandatory Settings");
		try
		{
			refreshCompletionForMandatory(conn, waitDialog);
    	}
    	catch (SQLException e)
    	{
    		_logger.info("Problems reading table information for SQL Table code completion.", e);
    	}
System.out.println("loadSavedCacheFromFilePostAction: END");
	}


	@Override
	public void refresh()
	{
		// Clear old completions
		clear();

		_schemaNames.clear();

		List<SqlTableCompletion> list = null;

		// DO THE REFRESH
		if (isSaveCacheEnabled())
			list = getSavedCacheFromFile(getInstanceName());

		// Still no valid list: restore must have failed or wasn't enabled
		if (list == null)
		{
			long startTime = System.currentTimeMillis();
			list = refreshCompletion();
			long refreshTimeInMs = System.currentTimeMillis() - startTime;
			
			// It above the "max" time to refresh, should we save the result in a serialized file
			if (refreshTimeInMs > getSaveCacheTimeInMs() && isSaveCacheEnabled())
			{
				if (isSaveCacheQuestionEnabled())
				{
					// POPUP question if user want's to save the result for later
					String refreshTimeStr = TimeUtils.msToTimeStr("%?HH[:]%MM:%SS.%ms", refreshTimeInMs);
					
					String htmlMsg = 
						"<html>"
						+ "Creating the completion list took '"+refreshTimeStr+"' (MM:SS.ms).<br>"
						+ "This is above the configued limit of "+getSaveCacheTimeInMs()+" ms.<br>"
						+ "<br>"
						+ "<b>Do you want to save the completion list to a file?</b><br>"
						+ "Next time you access the entity '<code>"+getInstanceName()+"</code>'<br>"
						+ "the completions will be restored from the saved file.<br>"
						+ "<br>"
						+ "This setting can be changed from the Completion button, choose 'Configue'<br>"
						+ "<html>";

					JPanel panel = new JPanel(new MigLayout());
					JCheckBox chk = new JCheckBox("<html>Do <b>not</b> ask this question in the future, <b>just save it</b>.</html>", false); 
					panel.add(new JLabel(htmlMsg), "grow, push, wrap");
					panel.add(chk,                 "wrap");
//					SwingUtils.showInfoMessageExt(_guiOwner, "Confirm", htmlMsg, chk, (JPanel)null);

					int response = JOptionPane.showConfirmDialog(_guiOwner, panel, "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
					if (response == JOptionPane.YES_OPTION) 
					{
						if (chk.isSelected())
							setSaveCacheQuestionEnabled(false);

						saveCacheToFile(list, getInstanceName());
					}
				}
				else
				{
					saveCacheToFile(list, getInstanceName());
				}
			}
		}

		// Restore the "saved" completions
		super.addCompletions(getStaticCompletions());

		if (list != null && list.size() > 0)
		{
			// Add the completion list
			addSqlCompletions(list);

			setNeedRefresh(false);
		}
	} // end method

	@Override
	protected List<Completion> getCompletionsImpl(JTextComponent comp)
	{
//		System.out.println("--------------- SQL: getCompletionsImpl() ---------------------- BEGIN");

		// Do the real work
		List<Completion> cList = getCompletionsSql(comp);
		if (cList == null)
			return EMPTY_COMPLETION_LIST;
//		int cSize = cList.size();
		
		// remove duplicates. NOTE: a proper comparator is needed...
		//cList = new ArrayList<>( new HashSet<>(cList) );

		int needsLookupCount = 0;
		for (Completion c : cList)
		{
			if (c instanceof SqlTableCompletion)
			{
				if ( ((SqlTableCompletion)c)._tableInfo.isColumnRefreshed() )
					needsLookupCount++;
			}
			if (c instanceof SqlFunctionCompletion)
			{
				if ( ((SqlFunctionCompletion)c)._functionInfo.isColumnRefreshed() )
					needsLookupCount++;
			}
			if (c instanceof SqlProcedureCompletion)
			{
				if ( ((SqlProcedureCompletion)c)._procInfo.isParamsRefreshed() )
					needsLookupCount++;
			}
		}
		// Do column/parameter completion in the background
		// If this is possible???
		if (needsLookupCount > 0)
		{
			//System.out.println("Do background completion of "+needsLookupCount+" tables/procedures.");
		}
		
		//System.out.println("--------------- SQL: getCompletionsImpl() ----- records("+cSize+") --- END");
		return cList;
	}
	protected List<Completion> getCompletionsSql(JTextComponent comp)
	{
		RSyntaxTextArea textArea = (RSyntaxTextArea)comp;

		String allowedChars = "-_.*/:%[]$\""; // []" will be stripped off when doing comparisons
		setCharsAllowedInWordCompletion(allowedChars);
		if ( ! StringUtil.isNullOrBlank(_dbExtraNameCharacters) )
			setCharsAllowedInWordCompletion( allowedChars + _dbExtraNameCharacters );

//		// Parse 
//		String allText     = comp.getText();
//		String enteredText = getAlreadyEnteredText(comp);

//		String currentWord = getCurrentWord(textArea);

		String currentLineStr = getCurrentLineStr(textArea);
		String enteredText = getAlreadyEnteredText(textArea);
		String currentWord = getCurrentWord(textArea);
//		String curFullWord = getCurrentFullWord(textArea);
//		String nextWord1   = getRelativeWord(textArea,  1);
//		String nextWord2   = getRelativeWord(textArea,  2);
		String prevWord1   = getRelativeWord(textArea, -1);
		String prevWord2   = getRelativeWord(textArea, -2);
		String prevWord3   = getRelativeWord(textArea, -3);
//		String prevWord4   = getRelativeWord(textArea, -4);

		if (enteredText != null) enteredText = enteredText.replace('%', '*'); // Change any SQL WildCardChar '%' into a more RegExp Friendly '*'
		if (currentWord != null) currentWord = currentWord.replace('%', '*'); // Change any SQL WildCardChar '%' into a more RegExp Friendly '*'
		
		if (_logger.isDebugEnabled())
		{
			_logger.debug("START: enteredText='"+enteredText+"'.");
			_logger.debug("START: currentWord='"+currentWord+"'.");
		}

		SqlObjectName etId = new SqlObjectName(enteredText, _dbProductName, _dbIdentifierQuoteString, _dbStoresUpperCaseIdentifiers, _dbSupportsSchema, false);
		//SqlObjectName cwId = new SqlObjectName(currentWord);

		if (_logger.isDebugEnabled())
		{
			_logger.debug("START: enteredText(etId) IDENTIFIER: "+ etId);
//			_logger.debug("START: currentWord(cwId) IDENTIFIER: "+ cwId);
		}

//		System.out.println("-----------------------------------");
//		System.out.println("enteredText = '"+enteredText+"'.");
//		System.out.println("currentWord = '"+currentWord+"'.");
//		System.out.println("curFullWord = '"+curFullWord+"'.");
//		System.out.println("-----------------------------------");
//		System.out.println("nextWord1   = '"+nextWord1+"'.");
//		System.out.println("nextWord2   = '"+nextWord2+"'.");
//		System.out.println("-----------------------------------");
//		System.out.println("prevWord1   = '"+prevWord1+"'.");
//		System.out.println("prevWord2   = '"+prevWord2+"'.");
//		System.out.println("prevWord3   = '"+prevWord3+"'.");
//		System.out.println("prevWord4   = '"+prevWord4+"'.");
//		System.out.println("-----------------------------------");

//		System.out.println("getCurrentWord()='"+currentWord+"'.");
//		System.out.println("getAlreadyEnteredText()='"+enteredText+"'.");


		// Code Completion Refresh
		if ( enteredText.equals(":r") )
		{
			setNeedRefresh(true);
			setNeedRefreshSystemInfo(true);
			clearSavedCache();
			
			refresh();
			return null;
		}
		
		if (needRefresh())
			refresh();

		if (_logger.isDebugEnabled())
			_logger.debug("getCompletionsSql(): _schemaNames="+_schemaNames);

		
		//-----------------------------------------------------------
		// Complete Connection Profiles
		//
		if (    currentLineStr != null 
		     && ( currentLineStr.startsWith("\\") || currentLineStr.toLowerCase().startsWith("go") )
		     && ( currentLineStr.startsWith("\\connect") || prevWord1.equals("-p") || prevWord2.equals("-p") || prevWord3.equals("-p") || prevWord1.equals("--profile") || prevWord2.equals("--profile") || prevWord3.equals("--profile") )
		   )
		{
			if (ConnectionProfileManager.hasInstance())
			{
				ArrayList<Completion> cList = new ArrayList<Completion>();

				ConnectionProfileManager cpm = ConnectionProfileManager.getInstance();
				Map<String, ConnectionProfile> profiles = cpm.getProfiles();
				
				for (String name : profiles.keySet())
				{
					ConnectionProfile cp = profiles.get(name);
					
					String replaceWith = name;
					if (name.indexOf(" ") >= 0 || name.indexOf("-") >= 0)
						replaceWith = "'" + name + "'"; // Quote the string if it contains spaces etc...

					ImageIcon icon = ConnectionProfileManager.getIcon16(cp.getSrvType());
//					BasicCompletion c = new BasicCompletion(this, replaceWith, null, cp.getToolTipText());
					SqlCompletion c = new SqlCompletion(this, name, replaceWith);
					c.setSummary(cp.getToolTipText());
					c.setIcon(icon);

					cList.add(c);
				}
				return getCompletionsFrom(cList, enteredText);
			}
		}
		
		//-----------------------------------------------------------
		// 'go' completion
		//
		if (currentLineStr.length() >= 2)
		{
			int endPos = Math.min(3, currentLineStr.length());
			String first3Chars = currentLineStr.substring(0, endPos).toLowerCase().trim();
			if (first3Chars.equals("go"))
			{
				if (enteredText.toLowerCase().equals("go"))
					return createGoCompletions();
				else
					return getCompletionsFrom(createGoCompletions(), enteredText);
			}
		}
		
		//-----------------------------------------------------------
		// Complete DATABASES
		//
		if ( currentLineStr != null && (currentLineStr.startsWith("\\use ") || currentLineStr.startsWith("\\USE ")) )
		{
			ConnectionProvider connProvider = getConnectionProvider();
			if (connProvider instanceof QueryWindow)
			{
				List<Completion> cList = new ArrayList<Completion>();

				// Get DBLIST from the QueryWindows ComboBox
				// - Add the databases to the Completion list
				// - return the databases that matching the current input
				QueryWindow queryWindow = (QueryWindow)connProvider;
				List<String> dblist = queryWindow.getDbNames();
				for (String dbname : dblist)
				{
					DbInfo dbinfo = new DbInfo();
					dbinfo._dbName = dbname;
					cList.add( new SqlDbCompletion(this, dbinfo));
				}

				String catName = SqlObjectName.stripQuote(enteredText, _dbIdentifierQuoteString);
				return getCompletionsFrom(cList, catName);
			}
		}

		if ("use".equalsIgnoreCase(prevWord1))
		{
			ArrayList<Completion> dbList = new ArrayList<Completion>();

			String catName = SqlObjectName.stripQuote(enteredText, _dbIdentifierQuoteString);
			
			for (SqlDbCompletion dc : _dbComplList)
			{
				DbInfo di = dc._dbInfo;
				if (startsWithIgnoreCaseOrRegExp(di._dbName, catName))
					dbList.add(dc);
			}
			return dbList;
		}

		if ( enteredText.startsWith(":db") )
		{
			String name = enteredText.substring(":db".length());

			ArrayList<Completion> cList = new ArrayList<Completion>();
			for (SqlDbCompletion dc : _dbComplList)
				cList.add(dc);
			
			return getCompletionsFrom(cList, name);
//			return cList;
		}

		//-----------------------------------------------------------
		// : = show all ':' completions
		//
		if ( enteredText.equals(":") )
		{
			if (_logger.isDebugEnabled())
				_logger.debug(">>> in: :s SCHEMA REPLACEMENT");

			ArrayList<Completion> cList = new ArrayList<Completion>();

			cList.add( new BasicCompletion(CompletionProviderAbstractSql.this, ":s",  "Show all schemas") );
			cList.add( new BasicCompletion(CompletionProviderAbstractSql.this, ":db", "Show all databases") );
			cList.add( new BasicCompletion(CompletionProviderAbstractSql.this, ":t",  "Show all user tables") );
			cList.add( new BasicCompletion(CompletionProviderAbstractSql.this, ":v",  "Show all user views") );
			cList.add( new BasicCompletion(CompletionProviderAbstractSql.this, ":st", "Show all system tables") );
			cList.add( new BasicCompletion(CompletionProviderAbstractSql.this, ":sv", "Show all system views") );
			cList.add( new BasicCompletion(CompletionProviderAbstractSql.this, ":TT", "Show all ToolTip from the ToolTipSupplier from file: INSTALL_PATH\\resources\\VENDOR_tooltip_provider.xml") );
			
			return cList;
		}

		//-------------------------------------
		// Only show SYSTEM VIEW ---------------------- NOTE: needs to be before ':s'
		if (enteredText.startsWith(":sv"))
		{
			if (_logger.isDebugEnabled())
				_logger.debug(">>> in: :sv SYSTEM VIEW REPLACEMENT");

			String name = enteredText.substring(":sv".length());
			
			ArrayList<Completion> clist = new ArrayList<Completion>();
			for (SqlTableCompletion tc : _tableComplList)
			{
				TableInfo ti = tc._tableInfo;
				if ("SYSTEM VIEW".equals(ti._tabType))
					clist.add(tc);
			}

			if ( ! clist.isEmpty() )
				return getCompletionsFrom(clist, name);
//				return clist;
		}

		//-------------------------------------
		// Only show SYSTEM TABLES ---------------------- NOTE: needs to be before ':s'
		if (enteredText.startsWith(":st"))
		{
			if (_logger.isDebugEnabled())
				_logger.debug(">>> in: :st SYSTEM-TABLE REPLACEMENT");

			String name = enteredText.substring(":st".length());
			
			ArrayList<Completion> clist = new ArrayList<Completion>();
			for (SqlTableCompletion tc : _tableComplList)
			{
				TableInfo ti = tc._tableInfo;
				if ("SYSTEM".equals(ti._tabType) || "SYSTEM TABLE".equals(ti._tabType) || "MDA Table".equals(ti._tabType))
					clist.add(tc);
			}

			if ( ! clist.isEmpty() )
				return getCompletionsFrom(clist, name);
//				return clist;
		}


		//-----------------------------------------------------------
		// :TT = get completions from: ToolTipSupplier
		if ( enteredText.startsWith(":TT") )
		{
			String name = enteredText.substring(":TT".length());
			if (StringUtil.isNullOrBlank(name))
				name = ".";

			ToolTipSupplierAbstract tts = getToolTipSupplier();
    		if (tts != null)
    		{
    			List<Completion> ttsCompletions = tts.getCompletionsFor(name);
    			if (ttsCompletions != null && !ttsCompletions.isEmpty())
    				return ttsCompletions;
    		}
		}

		//-----------------------------------------------------------
		// :s = show schemas in current database
		//
		if ( enteredText.startsWith(":s") )
		{
			if (_logger.isDebugEnabled())
				_logger.debug(">>> in: :s SCHEMA REPLACEMENT");

			String name = enteredText.substring(":s".length());
			
			ArrayList<Completion> cList = new ArrayList<Completion>();
			// lets return all schemas/owners
			for (String schemaName : _schemaNames)
				cList.add( new SqlSchemaCompletion(CompletionProviderAbstractSql.this, schemaName) );
			
			return getCompletionsFrom(cList, name);
			//return cList;
		}

		//-------------------------------------
		// Only show USER TABLES 
		if (enteredText.startsWith(":t"))
		{
			if (_logger.isDebugEnabled())
				_logger.debug(">>> in: :t TABLE REPLACEMENT");

			String name = enteredText.substring(":t".length());
			
			ArrayList<Completion> clist = new ArrayList<Completion>();
			for (SqlTableCompletion tc : _tableComplList)
			{
				TableInfo ti = tc._tableInfo;

				if (_logger.isDebugEnabled())
					_logger.debug("type='"+ti._tabType+"', name='"+ti._tabName+"'.");

//				if ("TABLE".equals(ti._tabType))
				if ("TABLE".equals(ti._tabType) || "BASE TABLE".equals(ti._tabType))
					clist.add(tc);
			}

			if ( ! clist.isEmpty() )
				return getCompletionsFrom(clist, name);
//				return clist;
		}

		//-------------------------------------
		// Only show VIEW 
		if (enteredText.startsWith(":v"))
		{
			if (_logger.isDebugEnabled())
				_logger.debug(">>> in: :v VIEW REPLACEMENT");

			String name = enteredText.substring(":v".length());
			
			ArrayList<Completion> clist = new ArrayList<Completion>();
			for (SqlTableCompletion tc : _tableComplList)
			{
				TableInfo ti = tc._tableInfo;
				if ("VIEW".equals(ti._tabType))
					clist.add(tc);
			}

			if ( ! clist.isEmpty() )
				return getCompletionsFrom(clist, name);
//				return clist;
		}

		//-----------------------------------------------------------
		// Complete STORED PROCS, if the previous word is EXEC
		// if enteredText also starts with "sp_", then do procedure lookup
		//
		if ( "exec".equalsIgnoreCase(prevWord1) || "execute".equalsIgnoreCase(prevWord1) || "call".equalsIgnoreCase(prevWord1) )
		{
			if (_logger.isDebugEnabled())
			{
				_logger.debug(">>> in: Complete STORED PROCS");
				_logger.debug("SqlObjectName: "+etId);
			}

			ArrayList<Completion> procList = new ArrayList<Completion>();

			// OTHER_DB_LOOKUP: Procedures in other databases, do lookup "on the fly"
			if (etId.isFullyQualifiedObject()) // CATALOG.SCHEMA.OBJECT
			{
				if (_logger.isDebugEnabled())
					_logger.debug(">>> in: Complete STORED PROCS: isFullyQualifiedObject");

				DbxConnection conn = getConnection();
				if (conn == null)
					return null;

				// Add matching procedures in specified database
				// but NOT if specified database is "sybsystemprocs" and input text starts with sp_
				if ( "sybsystemprocs".equalsIgnoreCase(etId.getCatalogName()) && etId.getObjectName().startsWith("sp_"))
				{
					// do not add sp_* if current working database is sybsystemprocs...
					// if entered text is 'sp_' those procs will be added a bit down (last in this section)
				}
				else
				{
					List<Completion> list = getProcedureListWithGuiProgress(conn, etId.getCatalogName(), etId.getSchemaName(), etId.getObjectName());
					if ( list != null && ! list.isEmpty() )
						procList.addAll(list);
				}

				// System stored procs, which should be executed in a specific database context
				if (etId.getObjectName().startsWith("sp_"))
				{
					// NOTE: _systemProcComplList probably does NOT have a completion with the database name in it.
					for (SqlProcedureCompletion pc : _systemProcComplList)
					{
						ProcedureInfo pi = pc._procInfo;
						if (startsWithIgnoreCaseOrRegExp(pi._procName, etId.getObjectName()))
						{
							SqlProcedureCompletion c = new SqlProcedureCompletion(CompletionProviderAbstractSql.this, pi, false, etId.getCatalogName(), false, _quoteTableNames);
							procList.add(c);
						}
					}
				}

				// Return here, since we did lookup in a specific database
				return procList;
			}

			// '.' at the end, DO: schema completion
			// NOTE: make this test AFTER: OTHER_DB_LOOKUP, otherwise the regexp
			if (etId.isSchemaQualifiedObject()) // SCHEMA.OBJECT
			{
				if (_logger.isDebugEnabled())
					_logger.debug("EXEC: SCHEMA.OBJECT: getProcedureCompletionsFromSchema(): etId.getSchemaName()='"+etId.getSchemaName()+"', etId.getObjectName()='"+etId.getObjectName()+"'.");

				// Get from the schemas
				List<Completion> list = getProcedureCompletionsFromSchema(_procedureComplList, etId.getSchemaName(), etId.getObjectName());

				if (_logger.isDebugEnabled())
					_logger.debug("EXEC: SCHEMA.OBJECT: getProcedureCompletionsFromSchema(): list.size() = "+list.size());

				// If cached schema lookup failed, the option might be OFF do a on-the-fly lookup...
				if (list.isEmpty())
				{
					if (_logger.isDebugEnabled())
						_logger.debug("EXEC: SCHEMA.OBJECT: NOT-IN LOCAL SCHEMA: -- ON-THE-FLY LOOKUP ---");

					DbxConnection conn = getConnection();
					if (conn == null)
						return null;

					list = getProcedureListWithGuiProgress(conn, etId.getCatalogName(), etId.getSchemaName(), etId.getObjectName());
					
					if (_logger.isDebugEnabled())
						_logger.debug("EXEC: SCHEMA.OBJECT: getProcedureCompletionsFromSchema(): list.size() = "+list.size());
				}

				return list;
			} // end: SCHEMA.OBJECT

			// Add matching procedures in local database
			// but NOT if current working database is "sybsystemprocs" and input text starts with sp_
			if ( "sybsystemprocs".equalsIgnoreCase(_currentCatalog) && etId.getObjectName().startsWith("sp_"))
			{
				// do not add sp_* if current working database is sybsystemprocs...
				// if entered text is 'sp_' those procs will be added a bit down (last in this section)
			}
			else
			{
				// Add matching procedures in local database
				for (SqlProcedureCompletion pc : _procedureComplList)
				{
					ProcedureInfo pi = pc._procInfo;
					if (startsWithIgnoreCaseOrRegExp(pi._procName, etId.getObjectName()))
						procList.add(pc);
				}
			}

			// Add matching databases
			for (SqlDbCompletion dc : _dbComplList)
			{
				DbInfo di = dc._dbInfo;
				if (startsWithIgnoreCaseOrRegExp(di._dbName, etId.getObjectName()))
					procList.add(dc);
			}

			// Add matching schemas
			for (String schemaName : _schemaNames)
			{
				if (_logger.isDebugEnabled())
					_logger.debug("EXEC(add matching schemas): schemaName='"+schemaName+"', etId.getObjectName()='"+etId.getObjectName()+"'.");

				if (startsWithIgnoreCaseOrRegExp(schemaName, etId.getObjectName()))
					procList.add( new SqlSchemaCompletion(CompletionProviderAbstractSql.this, schemaName) );
			}

			// Add matching 'sp_*' from system procedures
			if (startsWithIgnoreCaseOrRegExp(etId.getObjectName(), "sp_"))
			{
				if (_logger.isDebugEnabled())
					_logger.debug("SYSTEM PROC LOOKUP: schName='"+etId.getSchemaName()+"', objName='"+etId.getObjectName()+"'.");

				for (SqlProcedureCompletion pc : _systemProcComplList)
				{
					ProcedureInfo pi = pc._procInfo;
					if (startsWithIgnoreCaseOrRegExp(pi._procName, etId.getObjectName()))
						procList.add(pc);
				}
			}
			if (_logger.isDebugEnabled())
			{
				_logger.debug("<<<---PROC_LIST.size(): "+procList.size());
				_logger.debug("<<<---PROC_LIST: "+procList);
			}
			return procList;
		} // end: exec

		//-----------------------------------------------------------
		// TABLES in other databases, do lookup 'on the fly'
		// Check for database..tab<ctrl+space> 
		//
		if (_logger.isDebugEnabled())
			_logger.debug(">>> etId: "+etId);

		if (etId.isFullyQualifiedObject()) // CATALOG.SCHEMA.OBJECT
		{
			if (_logger.isDebugEnabled())
				_logger.debug(">>> in: TABLES in other databases, (isFullyQualifiedObject=TRUE, CATALOG.SCHEMA.OBJECT) do lookup 'on the fly' for: "+etId);

			DbxConnection conn = getConnection();
			if (conn == null)
				return null;

			// Note: this will also check for table-valued-functions, but right now that doesn't work :( at least for MS-SQL which I was testing against
			return getTableListWithGuiProgress(conn, etId.getCatalogName(), etId.getSchemaName(), etId.getObjectName());
		}

		//-----------------------------------------------------------
		// Check for TAB_ALIAS.COLUMN_NAME or SCHEMA.TAB or SCHEMA completion
		// 
		// OK - lets check if this is:
		// * A table alias doing column lookup 
		//      for: select * from         tab1 a where a.<ctrl+space>  == show columns for 'tab1'
		//       or: select * from     dbo.tab1 a where a.<ctrl+space>  == show columns for 'tab1' in schema 'dbo'
		//       or: select * from db1.dbo.tab1 a where a.<ctrl+space>  == show columns for 'tab1' in schema 'dbo', in catalog 'db1'
		//
		// * if the above can't be found, it's might be a catalog/dbname 
		//   or a owner/schema name that we should add to completion
		//      for: select * from db1.<ctrl+space>   == show schemas in catalog 'db1'
		//
		if (enteredText.indexOf('.') >= 0)
		{
			if (_logger.isDebugEnabled())
				_logger.debug(">>> in: TAB_ALIAS.COLUMN_NAME or SCHEMA.TAB or SCHEMA completion");

			String text = enteredText;

			int lastDot = text.lastIndexOf('.');
			
			String colName      = text.substring(lastDot+1);
			String tabAliasName = text.substring(0, lastDot);
			if (_logger.isDebugEnabled())
				_logger.debug("1-tabAliasName='"+tabAliasName+"'.");

			while(tabAliasName.indexOf('.') >= 0)
			{
				tabAliasName = tabAliasName.substring(tabAliasName.lastIndexOf('.')+1);
				if (_logger.isDebugEnabled())
					_logger.debug("2-tabAliasName='"+tabAliasName+"'.");
			}

			colName      = SqlObjectName.stripQuote(colName     , _dbIdentifierQuoteString);
			tabAliasName = SqlObjectName.stripQuote(tabAliasName, _dbIdentifierQuoteString);
			if (_logger.isDebugEnabled())
				_logger.debug("3-tabAliasName='"+tabAliasName+"'.");

			// If the "alias" name (word before the dot) is NOT A column, but a SCHEMA name (in the local database, cached in _schemaNames)
			// then continue lookup tables by schema name
//			if ( _schemaNames.contains(tabAliasName) )
			if ( CollectionUtils.containsIgnoreCase(_schemaNames, tabAliasName) )
			{
				String objNamePattern = colName;
				if (_logger.isDebugEnabled())
					_logger.debug("IN LOCAL SCHEMA: schema='"+tabAliasName+"', objNamePattern='"+objNamePattern+"'.");

				// do completion, but only for tables in a specific schema
//				List<Completion> tables    = getTableCompletionsFromSchema   (completions, tabAliasName, objNamePattern);
//				List<Completion> functions = getFunctionCompletionsFromSchema(completions, tabAliasName, objNamePattern);
//				tables.addAll(functions);
//				return tables;
				List<Completion> tables    = getTableAndFuncCompletionsFromSchema(completions, tabAliasName, objNamePattern);
				
				if (_logger.isDebugEnabled())
					_logger.debug("<<<---returns: "+tables);

				return tables;
			}
			else // alias is NOT in the "locals" schemas (so hopefully it's a column, but we will discover that...)
			{
				if (_logger.isDebugEnabled())
					_logger.debug("XXXX NOT-IN LOCAL SCHEMA: start");

				// Try to figure out what's the "real" table name for this alias
				// If we find it, display columns for the table (alias)
//				String tabName = getTableNameForAlias(comp, tabAliasName, true);
				String        aliasTabName     = getTableNameForAlias(comp, tabAliasName, false);
				SqlObjectName aliasFullTabName = new SqlObjectName( aliasTabName, _dbProductName, _dbIdentifierQuoteString, _dbStoresUpperCaseIdentifiers, _dbSupportsSchema);

				if (_logger.isDebugEnabled())
					_logger.debug("XXXX NOT-IN LOCAL SCHEMA: aliasTabName='"+aliasTabName+"', aliasFullTabName='"+aliasFullTabName+"'.");

				// Columns to show, will end up in here
				ArrayList<Completion> colList = new ArrayList<Completion>();

				// tablename has no Catalog specification, then do local/cached lookup 
				if ( ! aliasFullTabName.hasCatalogName() )
				{
					if (_logger.isDebugEnabled())
						_logger.debug("XXXX NOT-IN LOCAL SCHEMA: -- CACHED LOOKUP ---");

					// Search the cached table information.
					TableInfo ti = getTableInfo(aliasFullTabName.getObjectName());
					if (ti != null)
					{
						for (TableColumnInfo tci : ti._columns)
						{
							if (startsWithIgnoreCaseOrRegExp(tci._colName, colName))
								colList.add( new SqlColumnCompletion(CompletionProviderAbstractSql.this, tabAliasName, tci._colName, ti, _quoteColumnNames));
						}
					}
					// Search the cached function information.
					FunctionInfo fi = getFunctionInfo(aliasFullTabName.getObjectName());
					if (fi != null)
					{
						for (FunctionColumnInfo fci : fi._columns)
						{
							if (startsWithIgnoreCaseOrRegExp(fci._colName, colName))
								colList.add( new SqlColumnCompletion(CompletionProviderAbstractSql.this, tabAliasName, fci._colName, ti, _quoteColumnNames));
						}
					}
					// If not any columns was found "in cache", for this table, then do "on the fly" lookup
					// column information is optional to get when refreshing tables...
					// For the "non cached lookup" we need the catalog name, so set this...
					if (colList.isEmpty())
					{
						if (_logger.isDebugEnabled())
							_logger.debug("XXXX NOT-IN LOCAL SCHEMA: -- CACHED LOOKUP FAILED, found 0 column entries in cache --- (_currentCatalog='"+_currentCatalog+"'.)");

						aliasFullTabName.setCatalogName(_currentCatalog);
					}
				}

				// If cached column lookup failed to find any cached entries, the option might be OFF do a on the fly lookup...
				if (colList.isEmpty())
				{
					if (_logger.isDebugEnabled())
						_logger.debug("XXXX NOT-IN LOCAL SCHEMA: -- ON-THE-FLY LOOKUP ---");

					DbxConnection conn = getConnection();
					if (conn == null)
						return null;

					List<Completion> tabList = getTableListWithGuiProgress(conn, aliasFullTabName);
////					List<Completion> tabList = getTableListWithGuiProgress(conn, aliasFullTabName.getCatalogName(), aliasFullTabName.getSchemaName(), aliasFullTabName.getObjectName());
//					List<Completion> tabList;
//					if (_dbSupportsSchema)
//						tabList = getTableListWithGuiProgress(conn, aliasFullTabName.getCatalogName(), aliasFullTabName.getSchemaName(), aliasFullTabName.getObjectName());
//					else
//						tabList = getTableListWithGuiProgress(conn, aliasFullTabName.getSchemaName(), null, aliasFullTabName.getObjectName());
					
					if (_logger.isDebugEnabled())
					{
						_logger.debug("    LOOKUP-RESULT: " + tabList);
						_logger.debug("    LOOKUP-RESULT: size="+tabList.size());
					}

					// search the *tables* found when doing lookup (it might return several suggestions)
					// STOP after first *exact* TABLE match
					Completion c = null;
					for (Completion ce : tabList)
					{
						if (_logger.isDebugEnabled())
							_logger.debug("    COMP.getInputText()='"+ce.getInputText()+"'.");

						if (aliasFullTabName.getObjectName().equalsIgnoreCase(ce.getInputText()))
						{
							c = ce;
							break;
						}
					}
					if (_logger.isDebugEnabled())
						_logger.debug("    C = "+c);

					// If we found a TABLE, lets get columns that are matching up to currently entered text
					if (c != null && c instanceof SqlTableCompletion)
					{
						SqlTableCompletion jdbcCompl = (SqlTableCompletion) c;
						for (TableColumnInfo tci : jdbcCompl._tableInfo._columns)
						{
							if (startsWithIgnoreCaseOrRegExp(tci._colName, colName))
								colList.add( new SqlColumnCompletion(CompletionProviderAbstractSql.this, tabAliasName, tci._colName, jdbcCompl._tableInfo, _quoteColumnNames));
						}
					}
				}

				//
				// Still no values (no columns were found)
				// ---- Do SCHEMA lookup. ----
				// Example of entered text: select * from db1.<ctrl+space>   == show schemas in catalog 'db1'
				//
				if (colList.isEmpty())
				{
					int xdot1 = text.indexOf('.');
					final String catName = SqlObjectName.stripQuote( text.substring(0, xdot1), _dbIdentifierQuoteString);
					final String schName = SqlObjectName.stripQuote( text.substring(xdot1+1) , _dbIdentifierQuoteString);
					if (_logger.isDebugEnabled())
						_logger.debug("XXXX NOT-IN LOCAL SCHEMA: DO SCHEMA-LOOKUP catName='"+catName+"', schName='"+schName+"'.");

					// Search all known catalogs/databases
					for (SqlDbCompletion dc : _dbComplList)
					{
						DbInfo di = dc._dbInfo;
						if (_logger.isDebugEnabled())
							_logger.debug("XXXX NOT-IN LOCAL SCHEMA: searching catalog, di._dbName='"+di._dbName+"'.");

						if (catName.equalsIgnoreCase(di._dbName))
						{
							// if entered catalog/db name is current working catalog/db
							//   return known schema names in current catalog/db
							// else
							//   get available schemas for the specified catalog/db (NOT cached) 
							if (catName.equalsIgnoreCase(_currentCatalog))
							{
								if (_logger.isDebugEnabled())
									_logger.debug("XXXX NOT-IN LOCAL SCHEMA: DO SCHEMA-LOOKUP... in CURRENT-CATALOG.");

								// lets return all schemas/owners
								for (String schemaName : _schemaNames)
									colList.add( new SqlSchemaCompletion(CompletionProviderAbstractSql.this, catName+"."+schemaName) );
							}
							else // Lookup the schemas for the non-local-database do this ON THE FLY (NON CACHED)
							{
//								Connection conn = _connectionProvider.getConnection();
								DbxConnection conn = getConnection();
								if (conn == null)
									return null;

								if (_dbSupportsSchema)
								{
									if (_logger.isDebugEnabled())
										_logger.debug("XXXX NOT-IN LOCAL SCHEMA: DO SCHEMA-LOOKUP... LOOKUP-ON-THE-FLY.");

									List<Completion> list = getSchemaListWithGuiProgress(conn, catName, schName);
									if ( list != null && ! list.isEmpty() )
										colList.addAll(list);
								}
								else
								{
									if (_logger.isDebugEnabled())
										_logger.debug("XXXX NOT-IN LOCAL SCHEMA(_dbSupportsSchema="+_dbSupportsSchema+"): DO CATALOG-LOOKUP... LOOKUP-ON-THE-FLY. catName='"+catName+"', schName=*null*, colName='"+colName+"'.");

//									List<Completion> list = getTableListWithGuiProgress(conn, catName, schName, colName);
									List<Completion> list = getTableListWithGuiProgress(conn, catName, null, colName); // note: colName contains here the start of the table name 

									if (_logger.isDebugEnabled())
										_logger.debug("XXXX NOT-IN LOCAL SCHEMA(_dbSupportsSchema="+_dbSupportsSchema+"): DO CATALOG-LOOKUP... LOOKUP-ON-THE-FLY. catName='"+catName+"', schName=*null*, colName='"+colName+"'. list.size()=" + (list==null?"NULL":list.size()) );

									if ( list != null && ! list.isEmpty() )
										colList.addAll(list);
								}
							}

							break; // loop: _dbComplList, (after first match)
						}
					}
				}
	
				if (_logger.isDebugEnabled())
					_logger.debug("XXXX NOT-IN LOCAL SCHEMA: RETURN: colList.size: "+colList.size());

				return colList;
			}
		} // end ALIAS check: if (enteredText.indexOf('.') >= 0)

		
		//-------------------------------------
		// if it's Sybase, and the input starts with 'sp_' check the SYSTEM PROCEDURE LIST and the localDB
		if (enteredText.startsWith("sp_") && DbUtils.isProductName(_dbProductName, DbUtils.DB_PROD_NAME_SYBASE_ASE))
		{
			ArrayList<Completion> procList = new ArrayList<Completion>();
			
			// SYSTEM Procedures
			for (SqlProcedureCompletion pc : _systemProcComplList)
			{
				ProcedureInfo pi = pc._procInfo;
				if (startsWithIgnoreCaseOrRegExp(pi._procName, etId.getObjectName()))
					procList.add(pc);
			}
			// Local DB Procedures
			for (SqlProcedureCompletion pc : _procedureComplList)
			{
				ProcedureInfo pi = pc._procInfo;
				if (startsWithIgnoreCaseOrRegExp(pi._procName, etId.getObjectName()))
					procList.add(pc);
			}
			
			if ( ! procList.isEmpty() )
				return procList;
		}

		
		//-----------------------------------------------------------
		// No luck so far...
		// 
		// If we are in a: 
		//   * select <here> from t1
		//   * select c1, c2, c3, <here> from t1 
		//   * select c1, c2, c3 from t1 where <here> 
		//   * select c1, c2, c3 from t1 where c1 = 111 and <here> 
		//   * select c1, c2, c3 from t1 where c1 = 111 order by <here> 
		//   * select c1, c2, c3 from t1 where c1 = 111 order by c1, <here> 
		//   * select c1, count(*) from t1 group by <here> 
		//
		//   * update t1 where <here> 
		//   * update t1 from t2 where <here> 
		//
		//   * delete t1 where <here> 
		//   * delete t1 from t2 where <here> 
		//
		// In the above situations (and similar) where it's only ONE table involved: we could display columns from that table...
		// If there are several tables in the from list or (join clause) return columns for ALL of the tables
		// with completions as tabname.colname
		//
		//----- FIXME: implement the above
		List<Completion> colCompl = getColumnCompletionForTablesInSql(comp, enteredText);
		if (colCompl != null && !colCompl.isEmpty())
			return colCompl;

//		//-----------------------------------------------------------
//		// get completions from: ToolTipSupplier
//		ToolTipSupplierAbstract tts = getToolTipSupplier();
//		if (tts != null)
//		{
//			List<Completion> ttsCompletions = tts.getCompletionsFor(enteredText);
//			if (ttsCompletions != null && !ttsCompletions.isEmpty())
//				return ttsCompletions;
//		}

		// IF WE GET HERE, I could not figure out
		// what to deliver as Completion text so
		// LETS return "everything" that has been added as the BASE COMPLETION (catalogs, schemas, tables/views, staticCompletion), but NOT storedProcedures
		
		// completions is defined in org.fife.ui.autocomplete.AbstractCompletionProvider
		List<Completion> providerCompl = getCompletionsFrom(completions, enteredText);
		
		if (providerCompl.isEmpty())
		{
			//-----------------------------------------------------------
			// get completions from: ToolTipSupplier
			ToolTipSupplierAbstract tts = getToolTipSupplier();
			if (tts != null)
			{
				List<Completion> ttsCompletions = tts.getCompletionsFor(enteredText);
				if (ttsCompletions != null && !ttsCompletions.isEmpty())
					return ttsCompletions;
			}
		}
		
		return providerCompl;
	}


	private List<Completion> createGoCompletions()
	{
		ArrayList<Completion> cList = new ArrayList<Completion>();

		cList.add( new BasicCompletion(this, "#"        , "Number of times to repeat/execute the command batch",
		                                                  "<html>"
		                                                  + "<b>Example:<br><pre>"
		                                                  + "select 1\n"
		                                                  + "go 10\n"
		                                                  + "</pre><br>"
		                                                  + "Executes 'select 1' 10 times.<br>"
		                                                  + "</html>") );
		
		cList.add( new BasicCompletion(this, "top #"    , "Read only first # rows in the result set",
		                                                  "<html>"
		                                                  + "<b>Example:<br><pre>"
		                                                   + "select * from t1\n"
		                                                   + "go top 10\n"
		                                                   + "</pre><br>"
		                                                   + "First 10 rows of the ResultSet will be read, the rest will be skipped.<br>"
		                                                   + "This if the Statement doesn't allow 'top' or 'limit'.<br>"
		                                                   + "</html>") );
		
		cList.add( new BasicCompletion(this, "bottom #" , "Only display last # rows in the result set",
		                                                  "<html>"
		                                                  + "<b>Example:<br><pre>"
		                                                  + "select * from t1\n"
		                                                  + "go bottom 10\n"
		                                                  + "</pre><br>"
		                                                  + "Last 10 rows of the ResultSet will be read, the rest will be skipped.<br>"
		                                                  + "This if the Statement doesn't allow 'last' or 'bottom', which <i>nobody</i> does.<br>"
		                                                  + "</html>") );

		cList.add( new BasicCompletion(this, "wait #"   , "Wait #ms after the SQL Batch has been sent, probably used in conjunction with (multi go) 'go 10'",
		                                                  "<html>"
		                                                  + "<b>Example:<br><pre>"
		                                                  + "select count(*) from t1\n"
		                                                  + "go 1000 wait 100\n"
		                                                  + "</pre><br>"
		                                                  + "Execute Statement 1000 times, but after each execution: wait for 100 ms.<br>"
		                                                  + "This is good if you want to test something, but don't want to monopolize the server.<br>"
		                                                  + "</html>") );
		
		cList.add( new BasicCompletion(this, "plain"    , "Do NOT use a GUI table for result set, instead print as plain text.",
		                                                  "<html>"
		                                                  + "<b>Example:<br><pre>"
		                                                  + "select * from t1\n"
		                                                  + "go plain\n"
		                                                  + "</pre><br>"
		                                                  + "Execute Statement but do not show the results in a GUI table, instead print the output as plain text.<br>"
		                                                  + "This is good if the rows contains newlines etc, that can be hard to display in the GUI table.<br>"
		                                                  + "</html>") );

		cList.add( new BasicCompletion(this, "tab"      , "Present ResultSet(s) in Tabbed Panel.",
		                                                  "<html>"
		                                                  + "<b>Example:<br><pre>"
		                                                  + "select * from t1 \n"
		                                                  + "select * from t2 \n"
		                                                  + "select * from t3 \n"
		                                                  + "go tab\n"
		                                                  + "</pre><br>"
		                                                  + "Execute Statement, but do not show the results in GUI tables after each other, instead put each ResultSet in a 'gui-tab'.<br>"
		                                                  + "This is good if the ResultSet contains <b>many<b> rows, which the <i>normal<i> table mode have a problem to display.<br>"
		                                                  + "</html>") );
		
		cList.add( new BasicCompletion(this, "nodata"   , "Do NOT read the result set rows, just read the column headers. just do rs.next(), no rs.getString(col#)",
		                                                  "<html>"
		                                                  + "<b>Example:<br><pre>"
		                                                  + "select * from t1 \n"
		                                                  + "go nodata\n"
		                                                  + "</pre><br>"
		                                                  + "Execute Statement, but do not show the results, instaed just read the rows and <i>throw</i> them away.<br>"
		                                                  + "This is good if you want to test <i>end-to-end</i> comunication time without involving the GUI creation of table.<br>"
		                                                  + "</html>") );

		cList.add( new BasicCompletion(this, "append"   , "Do NOT clear results from previous executions. Append at the end.",
		                                                  "<html>"
		                                                  + "<b>Example:<br><pre>"
		                                                  + "select getdate() \n"
		                                                  + "go append\n"
		                                                  + "</pre><br>"
		                                                  + "Execute Statement, but do not <i>clear</i> previous results.<br>"
		                                                  + "This is good if you want to visually compare two (or more) results.<br>"
		                                                  + "</html>") );
		
		cList.add( new BasicCompletion(this, "psql"     , "Print the executed SQL Statement in the output",
		                                                  "<html>"
		                                                  + "<b>Example:<br><pre>"
		                                                  + "select * from t1 \n"
		                                                  + "go psql\n"
		                                                  + "</pre><br>"
		                                                  + "Print the just executed SQL Statement in the output.<br>"
		                                                  + "This is good if you want to <i>copy and paste</i> any examples in a mail or similar.<br>"
		                                                  + "</html>") );

		cList.add( new BasicCompletion(this, "prsi"     , "Print info about the ResultSet data types etc in the output",
		                                                  "<html>"
		                                                  + "<b>Example:<br><pre>"
		                                                  + "select * from t1 \n"
		                                                  + "go prsi\n"
		                                                  + "</pre><br>"
		                                                  + "Execute Statement and print more information about the ResultSet: Column names, data types, etc...<br>"
		                                                  + "This is good to determen if the result is a String or an Integer.<br>"
		                                                  + "</html>") );
		
		cList.add( new BasicCompletion(this, "time"     , "Print how long time the SQL Batch took, from the clients perspective",
		                                                  "<html>"
		                                                  + "<b>Example:<br><pre>"
		                                                  + "select * from t1 \n"
		                                                  + "go time\n"
		                                                  + "</pre><br>"
		                                                  + "Trace how long a Statement took to execute and read the ResultSet<br>"
		                                                  + "It also prints the time the command was executed.<br>"
		                                                  + "</html>") );
		
		cList.add( new BasicCompletion(this, "rowc"     , "Print the rowcount from JDBC driver, not the number of rows actually returned",
		                                                  "<html>"
		                                                  + "<b>Example:<br><pre>"
		                                                  + "select * from t1 \n"
		                                                  + "go rowc\n"
		                                                  + "</pre><br>"
		                                                  + "Print number of rows that was affected by the Statement<br>"
		                                                  + "</html>") );

		cList.add( new BasicCompletion(this, "skiprs"   , "If you have multiple ResultSet, skip some ResultSet(s) (starting at 1). Example skip ResultSet 1 and 2: go skiprs 1:2",
		                                                  "<html>"
		                                                  + "<b>Example:<br><pre>"
		                                                  + "exec sp_who \n"
		                                                  + "go skiprs 1:2\n"
		                                                  + "</pre><br>"
		                                                  + "<br>"
		                                                  + "Execute the procedure 'sp_who' but <i>discard</i> first and seconds ResultSet.<br>"
		                                                  + "Good if you want to use a specififc ResultSet for something like | chart...<br>"
		                                                  + "</html>") );

		cList.add( new BasicCompletion(this, "keeprs"   , "If you have multiple ResultSet, keep only ResultSet(s) (starting at 1). Example Keep only 2 : go keeprs 2",
		                                                  "<html>"
		                                                  + "<b>Example:<br><pre>"
		                                                  + "exec sp_spaceused t1 \n"
		                                                  + "go keeprs 2\n"
		                                                  + "</pre><br>"
		                                                  + "<br>"
		                                                  + "Execute the procedure 'sp_spaceused' but <i>discard</i> everything but the seconds ResultSet.<br>"
		                                                  + "Good if you want to use a specififc ResultSet for something like | chart... to create a graphical representation of any procedure that returns several ResultSets<br>"
		                                                  + "</html>") );
		
		cList.add( new BasicCompletion(this, "| pipe"  ,   "If you want to pipe ResultSet to any post processing",
		                                                  "<html>"
		                                                  + "Execute statement and pass the ResultSet to any post processing.<br>"
		                                                  + "<br>"

		                                                  + "<b>Example 1: </b>Create a PIE chart from second ResultSet ('graph' or 'chart' can be used)<br><pre>"
		                                                  + "exec sp_spaceused \n"
		                                                  + "go keeprs 2 | chart --str2num --removeRegEx '(KB|MB)'\n"
		                                                  + "</pre>"
		                                                  + "<br>"

		                                                  + "<b>Example 2: </b>Write the ResultSet to a CSV file (according to RFC 4180), NULL values will be <i>empty string</i><br><pre>"
		                                                  + "select * from t1 \n"
		                                                  + "go | tofile --header --rfc4180 -N none c:\\filename.csv \n"
		                                                  + "</pre>"
		                                                  + "<br>"

		                                                  + "<b>Example 3: </b>Copy/transfer the ResultSet into table toTableName' on another server (possibly another DBMS Vendor)<br><pre>"
		                                                  + "select * from t1 \n"
		                                                  + "go | bcp toTableName --profile 'MySQL at Home' --truncateTable\n"
		                                                  + "</pre>"
		                                                  + "<br>"

		                                                  + "<b>Example 4: </b>DIFF the ResultSet with another server (and possibly another DBMS Vendor)<br><pre>"
		                                                  + "select id1, id2, c1, c2, c3, c4 \n"
		                                                  + "from someTableName \n"
		                                                  + "where country = 'sweden' \n"
		                                                  + "order by id1, id2 \n"
		                                                  + "go | diff --profile 'PROD_1B_ASE - sa' -Dtempdb --keyCols 'id1, id2'\n"
		                                                  + "</pre>"
		                                                  + "<br>"
		                                                  + "<b>Note: </b>For Data Content <b>DIFF</b> you can also use <code>\\tabdiff</code> or the <code>\\dbdiff</code> command."
		                                                  + "<br>"

		                                                  + "<b>Note: </b>To list other available <code>pipe</code> commands, execute: <code>go | help</code>"
		                                                  + "<br>"

		                                                  + "<b>Note: </b>To list other available <code>\\localCmd</code> commands, execute: <code>\\help</code>"
		                                                  + "<br>"

		                                                  + "</html>") );

		return cList;
	}

	protected List<Completion> getTableAndFuncCompletionsFromSchema(List<Completion> completions, String schemaName, String lastPart)
	{
		ArrayList<Completion> retComp = new ArrayList<Completion>();
long startTime = System.currentTimeMillis();
		for (Completion c : completions)
		{
			if (c instanceof SqlTableCompletion)
			{
				SqlTableCompletion tabComp = (SqlTableCompletion) c;
				if (schemaName.equalsIgnoreCase(tabComp._tableInfo._tabSchema))
					retComp.add(c);
			}
			if (c instanceof SqlFunctionCompletion)
			{
				SqlFunctionCompletion funcComp = (SqlFunctionCompletion) c;
				if (schemaName.equalsIgnoreCase(funcComp._functionInfo._funcSchema) && funcComp._functionInfo._isTableValuedFunction)
					retComp.add(c);
			}
		}
System.out.println("get-TABLE/FUNC-CompletionsFromSchema: cnt="+retComp.size()+", ms="+(System.currentTimeMillis() - startTime));
		return getCompletionsFrom(retComp, lastPart);
	}

	protected List<Completion> getTableCompletionsFromSchema(List<Completion> completions, String schemaName, String lastPart)
	{
		ArrayList<Completion> retComp = new ArrayList<Completion>();
long startTime = System.currentTimeMillis();
		for (Completion c : completions)
		{
			if (c instanceof SqlTableCompletion)
			{
				SqlTableCompletion tabComp = (SqlTableCompletion) c;
				if (schemaName.equalsIgnoreCase(tabComp._tableInfo._tabSchema))
					retComp.add(c);
			}
		}
System.out.println("get-TABLE-CompletionsFromSchema: cnt="+retComp.size()+", ms="+(System.currentTimeMillis() - startTime));
		return getCompletionsFrom(retComp, lastPart);
	}
	
	protected List<Completion> getFunctionCompletionsFromSchema(List<Completion> completions, String schemaName, String lastPart)
	{
		ArrayList<Completion> retComp = new ArrayList<Completion>();
long startTime = System.currentTimeMillis();
		for (Completion c : completions)
		{
			if (c instanceof SqlFunctionCompletion)
			{
				SqlFunctionCompletion funcComp = (SqlFunctionCompletion) c;
				if (schemaName.equalsIgnoreCase(funcComp._functionInfo._funcSchema) && funcComp._functionInfo._isTableValuedFunction)
					retComp.add(c);
			}
		}
System.out.println("get-FUNCTION-CompletionsFromSchema: cnt="+retComp.size()+", ms="+(System.currentTimeMillis() - startTime));
		return getCompletionsFrom(retComp, lastPart);
	}
	
	protected List<Completion> getProcedureCompletionsFromSchema(List<SqlProcedureCompletion> ComplList, String schemaName, String lastPart)
	{
		ArrayList<Completion> retComp = new ArrayList<Completion>();
long startTime = System.currentTimeMillis();
		for (Completion c : ComplList)
		{
			if (c instanceof SqlProcedureCompletion)
			{
				SqlProcedureCompletion procComp = (SqlProcedureCompletion) c;
//System.out.println("get-PROCEDURE-CompletionsFromSchema: procComp._procInfo='"+procComp._procInfo+"'.");
//System.out.println("get-PROCEDURE-CompletionsFromSchema: procComp._procInfo._procSchema='"+procComp._procInfo._procSchema+"'.");
				if (schemaName.equalsIgnoreCase(procComp._procInfo._procSchema))
					retComp.add(c);
			}
		}
System.out.println("get-PROCEDURE-CompletionsFromSchema: cnt="+retComp.size()+", ms="+(System.currentTimeMillis() - startTime));
		return getCompletionsFrom(retComp, lastPart);
	}

	/**
	 * An attempt to minimal parse the text to find out what table name a alias has.<br>
	 * This is just a DUMMY, a real parse should be used instead. The problem with various
	 * SQL Parsers is that they throws exception if the syntax is faulty...<br>
	 * But when composing a SQL using completion the statement is "not yet complete" and ready
	 * to send to the server, therefore the syntax will be "off" 
	 *  
	 * @param comp            The text component
	 * @param alias           The alias name to search for a table name
	 * @param stripPrefix     If table name looks like 'dbo.t1' -> strip off all prefixes and return 't1'
	 * @return The Table name, if nothing is found the input alias will be returned.
	 */
	public String getTableNameForAlias(JTextComponent comp, String alias, boolean stripPrefix)
	{
		String table = alias;

		int cp = comp.getCaretPosition();
		String otxt = comp.getText();
		String ltxt = comp.getText().toLowerCase().replace('\n', ' ');
		int selectIndex = -1;
		while (true)
		{
			int i = ltxt.indexOf("select ", selectIndex + 1);
			if (i == -1)
				break;
			if (i >= cp)
				break;

			selectIndex = i;
		}
		int fromIndex = ltxt.indexOf(" from ", selectIndex);

		String fromStr = null;
		if (fromIndex >= 0)
		{
			fromIndex += " from ".length();
			int stopIndex = -1;
			if (stopIndex == -1) stopIndex = ltxt.indexOf(" where ",    fromIndex);
			if (stopIndex == -1) stopIndex = ltxt.indexOf(" group by ", fromIndex);
			if (stopIndex == -1) stopIndex = ltxt.indexOf(" order by ", fromIndex);
			if (stopIndex == -1) stopIndex = ltxt.indexOf(" select ",   fromIndex);
			if (stopIndex == -1) stopIndex = ltxt.indexOf(" update ",   fromIndex);
			if (stopIndex == -1) stopIndex = ltxt.indexOf(" delete ",   fromIndex);

			// If we can't find any "delimiter char", then try newline 
//			if (stopIndex == -1) stopIndex = otxt.indexOf('\n', fromIndex);
			if (stopIndex == -1) stopIndex = otxt.length();
			
			if (stopIndex >= 0)
				fromStr = otxt.substring(fromIndex, stopIndex);
		}

//System.out.println("CompletionProviderAbstractSql.getTableNameForAlias(): ---- fromStr = '"+fromStr+"'.");
		// Try to split the from str
		// fix so we can parse [xxx] [yyy] join tabname [[as][alias]] and NOT only ',' separated tables
// maybe we can "rewrite" the query to make the "parsing" a bit easier
// like removing keywords like " right ", " left ", " inner ", " full ", " outer ", " cross "
// and " as " -> ""
		
// select * from dbo.table1 t1 LEFT OUTER JOIN table2 t2 ON t1.id=t2.Advisor_id INNER JOIN table3 t3 ON t2.id = t3.id
// so we need to change "LEFT OUTER JOIN"->"," and remove everything after " ON " until we find next keyword like "next join" or where, group by, order by ....
		
		if (fromStr != null)
		{
			// First remove all " as " words for aliasing (this since the "as" is optional)
			fromStr = fromStr.replaceAll("(?i) as ", " ");
			
			int aliasIndex = fromStr.indexOf(" "+alias);
			if (aliasIndex >= 0)
			{
				// Now transform all "JOIN" stuff into ordinary table list: t1, t2, t2...
				// Remove all "extra" semantics about the join
				fromStr = fromStr.replaceAll("(?i) right ", " ");
				fromStr = fromStr.replaceAll("(?i) left ",  " ");
				fromStr = fromStr.replaceAll("(?i) inner ", " ");
				fromStr = fromStr.replaceAll("(?i) full ",  " ");
				fromStr = fromStr.replaceAll("(?i) outer ", " ");
				fromStr = fromStr.replaceAll("(?i) cross ", " ");
				fromStr = fromStr.replaceAll("(?i) apply ", " ");
				// Then replace the "join" with a simple ',' comma character
				fromStr = fromStr.replaceAll("(?i) join ",  ",");

				String[] sa = fromStr.split(",");
//System.out.println("sa[]: >"+StringUtil.toCommaStr(sa, "|")+"<");
				for (String s : sa)
				{
					s = s.trim();
					if (StringUtil.isNullOrBlank(s))
						continue;

					// if the thing is a "join", then remove the " ON ..."
					int onKeyword = StringUtil.indexOfIgnoreCase(s, " ON ");
//					int onKeyword = s.indexOf(" on ");
//					if (onKeyword == -1) onKeyword = s.indexOf(" On ");
//					if (onKeyword == -1) onKeyword = s.indexOf(" oN ");
//					if (onKeyword == -1) onKeyword = s.indexOf(" ON ");
					if (onKeyword >= 0)
						s = s.substring(0, onKeyword);
						
					String[] sa2 = s.split("\\s+");
//System.out.println("sa2[]: >"+StringUtil.toCommaStr(sa2, "|")+"<");
					if (sa2.length >= 2)
					{
						if (sa2[1].equalsIgnoreCase(alias))
						{
							table = sa2[0];
							if (stripPrefix && table.indexOf('.') >= 0)
							{
								table = table.substring( table.lastIndexOf('.') + 1 );
							}
//System.out.println("Alias to TabName: alias'"+alias+"', tabname='"+table+"'.");
						}
					}
				}
			}
			//System.out.println("FROM STR='"+fromStr+"'.");
		}

		return table;
	}

	/**
	 * FIXME: describe me
	 * @param comp
	 * @param enteredText 
	 * @return
	 */
	private List<Completion> getColumnCompletionForTablesInSql(JTextComponent comp, String enteredText)
	{
		boolean exitEarly = true;  // method: -not-yet-implemented-
		        exitEarly = false; // uncomment: -in-test-development-
		if (exitEarly)
			return null;

//		System.out.println();
//		System.out.println("################################################################");
//		System.out.println("## getColumnCompletionForTablesInSql");
//		System.out.println("################################################################");
		
		if ( ! (comp instanceof RSyntaxTextArea) )
		{
//			System.out.println("getColumnCompletionForTablesInSql(): NOT A RSyntaxTextArea");
			return null;
		}
		RSyntaxTextArea ta  = (RSyntaxTextArea) comp;
		RSyntaxDocument doc = (RSyntaxDocument) ta.getDocument();
//		int line = ta.getCaretLineNumber();
		int startOffset = comp.getCaretPosition();

		// loop max 1000 times (just so we dont end up in an infinite loop
		int maxCount = 1000;

		// Get previous token and loop BACKWARDS
		Token t = RSyntaxUtilities.getPreviousImportantTokenFromOffs(doc, startOffset);
		char foundOp = '-';
		int  foundAtOffset = -1;
//		int  parenthesesCount = 0; // increment / decrement every time we see a ( and ) so we can "skip" sub selects etc... 
		while(true)
		{
			String word = t.getLexeme().toLowerCase();

			// Increment and decrement when we see parentheses 
			// NOTE: we are scanning *backwards* here
//			if (t.isSingleChar(')')) parenthesesCount++; 
//			if (t.isSingleChar('(')) parenthesesCount--;
//			System.out.println("BACK[parenthesesCount="+parenthesesCount+"]: "+t);
			
			if ("go".equals(word) || word.equals(";"))
			{
//				System.out.println("BACKWARD SEARCH -- END SEARCH -- found 'go' or ';': "+t);
				break;
			}
			
//			if (t.getType() == TokenTypes.RESERVED_WORD)
//			if (t.is(TokenTypes.RESERVED_WORD, "select")) System.out.println("-------------- found 'select'"); // it's case SENSITIVE
//			if (t.is(TokenTypes.RESERVED_WORD, "SELECT")) System.out.println("-------------- found 'SELECT'"); // it's case SENSITIVE
			
//			if (parenthesesCount == 0 && ("select".equals(word) || "insert".equals(word) || "update".equals(word) || "delete".equals(word)))
//			{
				if      ("select".equals(word)) foundOp = 'S';
				else if ("insert".equals(word)) foundOp = 'I';
				else if ("update".equals(word)) foundOp = 'U';
				else if ("delete".equals(word)) foundOp = 'D';
				
				foundAtOffset = t.getOffset();
//				t = t.getNextToken(); // Move on to next token, just to position off from the one just found

				if (foundOp != '-')
				{
//					System.out.println("FOUND SELECT/UPDATE/INSERT/DELETE: foundOp='"+foundOp+"', foundAtOffset="+foundAtOffset);
					break;
				}
//			}
			t = RSyntaxUtilities.getPreviousImportantTokenFromOffs(doc, t.getOffset());
			maxCount--;
			if (maxCount < 0)
				break;
		}
		if (foundOp != '-')
		{
//			System.out.println("YES OP Was found: "+foundOp);

			// Loop forward to find the table name after select/insert/update/delete
			String fullTableName = getTableNameForSelInsUpdDel(foundOp, ta, t);

			// If we found a table name, lookup it's columns
			if (StringUtil.hasValue(fullTableName))
			{
				SqlObjectName tabNameObj = new SqlObjectName(fullTableName, _dbProductName, _dbIdentifierQuoteString, _dbStoresUpperCaseIdentifiers, _dbSupportsSchema, false);

//				System.out.println("LOOKUP for "+foundOp+" -- fullTableName ='" + fullTableName + "', enteredText='" + enteredText + "', tabNameObj="+tabNameObj);

				// Columns to show, will end up in here
				ArrayList<Completion> colList = new ArrayList<Completion>();

				// tablename has no Catalog specification, then do local/cached lookup 
				if ( ! tabNameObj.hasCatalogName() )
				{
					if (_logger.isDebugEnabled())
						_logger.debug("XXXX NOT-IN LOCAL SCHEMA: -- CACHED LOOKUP ---");

					// Search the cached table information.
					TableInfo ti = getTableInfo(null, null, tabNameObj.getObjectName(), true);
//					System.out.println("LOOKUP for SELECT -- ti=" + ti);
					if (ti != null)
					{
						for (TableColumnInfo tci : ti._columns)
						{
							if (startsWithIgnoreCaseOrRegExp(tci._colName, enteredText))
								colList.add( new SqlColumnCompletion(CompletionProviderAbstractSql.this, "", tci._colName, ti, _quoteColumnNames));
						}
					}
					// Search the cached function information.
					FunctionInfo fi = getFunctionInfo(null, null, tabNameObj.getObjectName(), true);
//					System.out.println("LOOKUP for SELECT -- fi=" + fi);
					if (fi != null)
					{
						for (FunctionColumnInfo fci : fi._columns)
						{
							if (startsWithIgnoreCaseOrRegExp(fci._colName, enteredText))
								colList.add( new SqlColumnCompletion(CompletionProviderAbstractSql.this, "", fci._colName, ti, _quoteColumnNames));
						}
					}
				}

				if (_logger.isDebugEnabled())
					_logger.debug("LOOKUP for SELECT -- colList.size=" + colList.size());

				if ( ! colList.isEmpty() )
					return colList;
			}
		
		} // end: find Operator S, I, U, D

		// Nothing was found
		return null;
	}
	
	private String getTableNameForSelInsUpdDel(char foundOp, RSyntaxTextArea ta, Token t)
	{
		int maxCount;
		int  parenthesesCount = 0; // increment / decrement every time we see a ( and ) so we can "skip" sub selects etc... 
		String fullTableName = "";
		int appendCount = 0;

		try
		{
			for(maxCount=1000; t!=null; t=RSyntaxUtilities.getNextImportantToken(t.getNextToken(), ta, ta.getLineOfOffset(t.getOffset())))
			{
				if (maxCount-- < 0 || t.getType() == TokenTypes.NULL)
					break;
//				System.out.println("T="+t);
//				System.out.println("                      at line: "+ta.getLineOfOffset(t.getOffset()));

				String word = t.getLexeme().toLowerCase();
				if ("go".equals(word) || word.equals(";"))
				{
//					System.out.println("SSSSSSSSS -- END SEARCH -- Passed 'go' or ';': "+t);
					break;
				}

				// Increment and decrement when we see parentheses 
				// NOTE: we are scanning *forward* here
				if (t.isSingleChar('(')) parenthesesCount++; 
				if (t.isSingleChar(')')) parenthesesCount--;

				if ( parenthesesCount == 0 )
				{
					if (foundOp == 'S' && ! ("from".equals(word) || "join".equals(word)))
						continue;
					
					if (foundOp == 'I' && ! "into".equals(word))
						continue;

					// UPDATE tablename <<<--- is next word
//					if (foundOp == 'U' && ! ("from".equals(word) || "join".equals(word)))
//						continue;
					
					if (foundOp == 'D' && ! "from".equals(word))
						continue;
					
					while(true)
					{
//						t = t.getNextToken();
						t = RSyntaxUtilities.getNextImportantToken(t.getNextToken(), ta, ta.getLineOfOffset(t.getOffset()));

						if (maxCount-- < 0 || t.getType() == TokenTypes.NULL)
							break;

						if ( t.getType() == TokenTypes.RESERVED_WORD )
						{
//							System.out.println("   <<<<< this is RESERVED_WORD... BREAK loop: " + t);
							break;
						}

						// STOP 
						word = t.getLexeme().toLowerCase();
						if (foundOp == 'S' && ("where".equals(word) || "group".equals(word) || "order".equals(word) || "having".equals(word))) break;
						if (foundOp == 'I' && ("(".equals(word) || "values".equals(word) || "select".equals(word))) break;
						if (foundOp == 'U' && ("set".equals(word))) break;
						if (foundOp == 'D' && ("where".equals(word) || "from".equals(word))) break;
						
//						System.out.println("   ++++++++++++ APPEND-fullTableName: "+t);
						fullTableName += t.getLexeme();
						appendCount++;

						// dbname.schema.tabname   <<<--- this is 5 tokens...
						if (appendCount >= 5)
							break;
					}
					
					if (appendCount > 0)
						break;
				}
			}
		}
		catch (BadLocationException ex)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("Problems in getTableNameForSelInsUpdDel(RSyntaxTextArea ta, Token t).", ex);
		}

		if (StringUtil.hasValue(fullTableName))
			return fullTableName;
		
		return null;
	}


//	abstract protected void refreshCompletionForStaticCmds();
//	protected void refreshCompletionForStaticCmds()
//	{
//		_logger.error("CompletionProviderAbstractSql.refreshCompletionForStaticCmds() is called this SHOULD REALLY BE ABSTACT IN HERE...");
//	}

	/**
	 * Used below to remove ASE and SQL-Server procname;0 and function;0
	 * @param str
	 */
	protected String removeSystemChars(String str)
	{
		if (str == null)
			return null;
		
		if (str.endsWith(";0") || str.endsWith(";1"))
			return str.substring(0, str.length()-2);
		
		return StringUtils.trim(str);
	}

	/**
	 * This one will *always* be called
	 * 
	 * @param conn
	 * @param waitDialog
	 * @return
	 * @throws SQLException
	 */
	protected void refreshCompletionForMandatory(Connection conn, WaitForExecDialog waitDialog)
	throws SQLException
	{
		if (waitDialog != null && waitDialog.wasCancelPressed())
			return;

		if (StringUtil.hasValue(_localCatalogName))
		{
			setCatalog(_localCatalogName);
		}

		// Obtain a DatabaseMetaData object from our current connection        
		DatabaseMetaData dbmd = conn.getMetaData();

		// What PRODUCT are we connected to 
		try { _dbProductName = dbmd.getDatabaseProductName(); } catch(SQLException ignore) { _logger.warn("ignoring this; dbmd.getDatabaseProductName() caused SQLException", ignore); }

		_currentServerName = DbUtils.getDatabaseServerName(conn, _dbProductName);

		try { _dbExtraNameCharacters        = dbmd.getExtraNameCharacters();     } catch(SQLException ignore) {}
		try { _dbIdentifierQuoteString      = dbmd.getIdentifierQuoteString();   } catch(SQLException ignore) {}
		try { _dbStoresUpperCaseIdentifiers = dbmd.storesUpperCaseIdentifiers(); } catch(SQLException ignore) {}

		// Note: this is also implemented in: DbUtils.isSchemaSupported(_conn);
		String schemaTerm       = "";
		int    maxSchemaNameLen = 0;
		try { schemaTerm       = dbmd.getSchemaTerm();          } catch(SQLException ignore) {}
		try { maxSchemaNameLen = dbmd.getMaxSchemaNameLength(); } catch(SQLException ignore) {}
		if (maxSchemaNameLen <= 0 && StringUtil.isNullOrBlank(schemaTerm))
		{
			_dbSupportsSchema = false;
		}
		
		// get current catalog/dbName
		try {_currentCatalog = conn.getCatalog(); }  catch(SQLException ignore) {}
		
		// Get DBMS Server name
		if (conn instanceof DbxConnection)
		{
			try {
				String dbmsSrvName = ((DbxConnection)conn).getDbmsServerName();
				_logger.info("JDBC DbxConnection   .getDbmsServerName()          is '"+dbmsSrvName+"'.");
			} catch (SQLException ignore) {} 
		}

		_logger.info("JDBC DatabaseMetaData.getDatabaseProductName()     is '"+_dbProductName+"'.");
		_logger.info("JDBC DatabaseMetaData.getExtraNameCharacters()     is '"+_dbExtraNameCharacters+"'.");
		_logger.info("JDBC DatabaseMetaData.getIdentifierQuoteString()   is '"+_dbIdentifierQuoteString+"'.");
		_logger.info("JDBC DatabaseMetaData.storesUpperCaseIdentifiers() is '"+_dbStoresUpperCaseIdentifiers+"'.");
		_logger.info("JDBC DatabaseMetaData.getSchemaTerm()              is '"+schemaTerm       + "' (dbSupportsSchema = "+_dbSupportsSchema+").");
		_logger.info("JDBC DatabaseMetaData.getMaxSchemaNameLength()     is " +maxSchemaNameLen + " (dbSupportsSchema = "+_dbSupportsSchema+").");
		_logger.info("JDBC _dbSupportsSchema                             is " +_dbSupportsSchema);
		_logger.info("JDBC _currentCatalog                               is " +_currentCatalog);

		if (false || _logger.isDebugEnabled())
		{
			try { _logger.info("getCatalogSeparator:            "+dbmd.getCatalogSeparator());                                                                    } catch(SQLException ignore) {}
			try { _logger.info("getCatalogTerm:                 "+dbmd.getCatalogTerm());                                                                         } catch(SQLException ignore) {}
			try { _logger.info("getDefaultTransactionIsolation: "+dbmd.getDefaultTransactionIsolation());                                                         } catch(SQLException ignore) {}
			try { _logger.info("getProcedureTerm:               "+dbmd.getProcedureTerm());                                                                       } catch(SQLException ignore) {}
			try { _logger.info("getSchemaTerm:                  "+dbmd.getSchemaTerm());                                                                          } catch(SQLException ignore) {}
			try { _logger.info("getSearchStringEscape:          "+dbmd.getSearchStringEscape());                                                                  } catch(SQLException ignore) {}
			try { _logger.info("getSQLKeywords:                 "+dbmd.getSQLKeywords());                                                                         } catch(SQLException ignore) {}
			try { _logger.info("getNumericFunctions:            "+dbmd.getNumericFunctions());                                                                    } catch(SQLException ignore) {}
			try { _logger.info("getSQLStateType:                "+dbmd.getSQLStateType());                                                                        } catch(SQLException ignore) {}
			try { _logger.info("getStringFunctions:             "+dbmd.getStringFunctions());                                                                     } catch(SQLException ignore) {}
			try { _logger.info("getSystemFunctions:             "+dbmd.getSystemFunctions());                                                                     } catch(SQLException ignore) {}
			try { _logger.info("getTimeDateFunctions:           "+dbmd.getTimeDateFunctions());                                                                   } catch(SQLException ignore) {}
			try { _logger.info("getURL:                         "+dbmd.getURL());                                                                                 } catch(SQLException ignore) {}
			try { _logger.info("getCatalogs\n"             +new ResultSetTableModel(dbmd.getCatalogs(),              "getCatalogs").toTableString());             } catch(SQLException ignore) {}
			try { _logger.info("getSchemas\n"              +new ResultSetTableModel(dbmd.getSchemas(),               "getSchemas").toTableString());              } catch(SQLException ignore) {}
//			try { _logger.info("getClientInfoProperties\n" +new ResultSetTableModel(dbmd.getClientInfoProperties(),  "getClientInfoProperties").toTableString()); } catch(SQLException ignore) {}
//			try { _logger.info("getTableTypes\n"           +new ResultSetTableModel(dbmd.getTableTypes(),            "getTableTypes").toTableString());           } catch(SQLException ignore) {}
//			try { _logger.info("getTypeInfo\n"             +new ResultSetTableModel(dbmd.getTypeInfo(),              "getTypeInfo").toTableString());             } catch(SQLException ignore) {}
		}
//_logger.info("getTableTypes\n"           +new ResultSetTableModel(dbmd.getTableTypes(),            "getTableTypes").toTableString());

		// ALWAYS Quote Table and Column Names
		Configuration conf = Configuration.getCombinedConfiguration();
		String PROPKEY_alwaysQuoteTableNames  = PROPKEY_PREFIX_alwaysQuoteTableNames  + _dbProductName;
		String PROPKEY_alwaysQuoteColumnNames = PROPKEY_PREFIX_alwaysQuoteColumnNames + _dbProductName;
		if (conf.hasProperty(PROPKEY_alwaysQuoteTableNames))  _quoteTableNames  = conf.getBooleanProperty(PROPKEY_alwaysQuoteTableNames,  DEFAULT_alwaysQuoteTableNames);
		if (conf.hasProperty(PROPKEY_alwaysQuoteColumnNames)) _quoteColumnNames = conf.getBooleanProperty(PROPKEY_alwaysQuoteColumnNames, DEFAULT_alwaysQuoteColumnNames);

		// For some DBMS go and check stuff and "override" defaults
		if (DbUtils.DB_PROD_NAME_H2.equals(_dbProductName))
		{
			if (waitDialog != null)
				waitDialog.setState("Getting H2 database settings");

			// if 'DATABASE_TO_UPPER' is true, then table names must be quoted
		//	String sql = "select VALUE from INFORMATION_SCHEMA.SETTINGS where NAME = 'DATABASE_TO_UPPER'";
			String sql = "select #NAME#, #VALUE# from #INFORMATION_SCHEMA#.#SETTINGS#".replace('#', '"');
			try
			{
				Statement stmnt = conn.createStatement();
				ResultSet rs = stmnt.executeQuery(sql);
				while(rs.next())
				{
					String name  = StringUtils.trim(rs.getString(1));
					String value = StringUtils.trim(rs.getString(2));

					//--------------------------------------------------------------------------
					// https://www.h2database.com/javadoc/org/h2/engine/DbSettings.html
					// Database setting DATABASE_TO_UPPER (default: true).
					// Database short names are converted to uppercase for the DATABASE() function, and in the CATALOG column of all database meta data methods. 
					// Setting this to "false" is experimental. When set to false, all identifier names (table names, column names) are case sensitive (except aggregate, built-in functions, data types, and keywords).
					//--------------------------------------------------------------------------
					if ("DATABASE_TO_UPPER".equals(name))
					{
						_quoteTableNames  = value.trim().equalsIgnoreCase("true");
						//_quoteTableNames  = value.trim().equalsIgnoreCase("false");
						_quoteColumnNames = _quoteTableNames;
					}

					//--------------------------------------------------------------------------
					// When I checked my DATABASE it's not part of the INFORMATION_SCHEMA.SETTINGS
					// But it exists according to: http://www.h2database.com/html/grammar.html#set_ignorecase
					//
					// If IGNORECASE is enabled, text columns in newly created tables will be case-insensitive. 
					// Already existing tables are not affected. The effect of case-insensitive columns is similar to using a collation with strength PRIMARY. 
					// Case-insensitive columns are compared faster than when using a collation. String literals and parameters are however still considered case sensitive even if this option is set.
					// Admin rights are required to execute this command, as it affects all connections. This command commits an open transaction in this connection. 
					// This setting is persistent. This setting can be appended to the database URL: jdbc:h2:test;IGNORECASE=TRUE
					//--------------------------------------------------------------------------
					//if ("IGNORECASE".equals(name))  
					//{
					//	_quoteTableNames  = value.trim().equalsIgnoreCase("true");
					//	_quoteColumnNames = _quoteTableNames;
					//}
				}
				rs.close();
				stmnt.close();
			}
			catch (SQLException sqle)
			{
				_logger.info("Problems when getting ASE monTables dictionary, skipping this and continuing. Caught: "+sqle);
			}
		}
	}

	
	/**
	 * Add either schema or catalog name to _schemaNames
	 * 
	 * @param catalog
	 * @param schema
	 */
	private void addSchema(String catalog, String schema)
	{
		String addStr = schema;
		if (StringUtil.isNullOrBlank(schema))
			addStr = catalog;
		
		if (StringUtil.hasValue(addStr))
			_schemaNames.add(addStr);

		// if (getDbSupportsSchema())
	}
	
	/**
	 * 
	 * @param conn
	 * @param waitDialog
	 * @return
	 * @throws SQLException
	 */
	protected void refreshCompletionForMisc(Connection conn, WaitForExecDialog waitDialog)
	throws SQLException
	{
//System.out.println("SQL: refreshCompletionForMisc()");
		if (waitDialog.wasCancelPressed())
			return;
	}

	//------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------
	// DB
	//------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------
	protected List<DbInfo> refreshCompletionForDbs(Connection conn, WaitForExecDialog waitDialog)
	throws SQLException
	{
//System.out.println("SQL: refreshCompletionForDbs()");
		final String stateMsg = "Getting Database information";
		waitDialog.setState(stateMsg);

		ArrayList<DbInfo> dbInfoList = new ArrayList<DbInfo>();

		// Obtain a DatabaseMetaData object from our current connection        
		DatabaseMetaData dbmd = conn.getMetaData();

		_logger.debug("refreshCompletionForDbs(): calling dbmd.getCatalogs()");

		ResultSet rs = dbmd.getCatalogs();
		while(rs.next())
		{
			DbInfo di = new DbInfo();

			di._dbName = StringUtils.trim(rs.getString("TABLE_CAT"));

			dbInfoList.add(di);
		}
		rs.close();

		return dbInfoList;
	}

	
	//------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------
	// SCHEMAS
	//------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------
	protected List<SchemaInfo> refreshCompletionForSchemas(Connection conn, WaitForExecDialog waitDialog, String catalogName, String schemaName)
	throws SQLException
	{
//System.out.println("SQL: refreshCompletionForDbs()");
		final String stateMsg = "Getting Schema information";
		waitDialog.setState(stateMsg);

		ArrayList<SchemaInfo> schemaInfoList = new ArrayList<SchemaInfo>();

		// Obtain a DatabaseMetaData object from our current connection        
		DatabaseMetaData dbmd = conn.getMetaData();

		_logger.debug("refreshCompletionForSchemas: calling dbmd.getCatalogs(catalog, schemaPattern)");

		if (schemaName != null)
		{
			schemaName = schemaName.replace('*', '%').trim();
			if ( isWildcatdMath() && ! schemaName.endsWith("%") )
				schemaName += "%";
		}

		ResultSet rs = dbmd.getSchemas(catalogName, schemaName);

		while(rs.next())
		{
			SchemaInfo si = new SchemaInfo();

			// Oracle dosn't seem to support TABLE_CATALOG so do workaround
			boolean getTabCatalog = true;

			if (getTabCatalog) { try { si._cat  = StringUtils.trim(rs.getString("TABLE_CATALOG")); } catch(SQLException ex) { getTabCatalog = false; if (_logger.isDebugEnabled()) _logger.warn("Problems getting 'TABLE_CATALOG' in refreshCompletionForSchemas() "); }	}
			si._name = StringUtils.trim(rs.getString("TABLE_SCHEM"));

			// On some databases, do not show all the ***_role things, they are not schema or users...
			if (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(_dbProductName) && si._name != null && si._name.endsWith("_role"))
				continue;

			schemaInfoList.add(si);
		}
		rs.close();

		if (schemaInfoList.isEmpty() && ! _dbSupportsSchema )
		{
			if (_dbInfoList != null && _dbInfoList.size() > 0)
			{
				for (DbInfo dbInfo : _dbInfoList)
				{
					SchemaInfo si = new SchemaInfo();
					si._cat  = dbInfo._dbName;
					si._name = dbInfo._dbName;

					schemaInfoList.add(si);
				}
			}
		}

		return schemaInfoList;
	}

	//------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------
	// TABLES
	//------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------
	@Override
	public TableModel getLookupTableTypesModel()
	{
//		Connection conn = _connectionProvider.getConnection();
		DbxConnection conn = getConnection();
		String[] cols = {"Include", "TableType"};
		DefaultTableModel tm = new DefaultTableModel(cols,  0)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(int index) 
			{
				if (index == 0) return Boolean.class;
				return String.class;
			}
		};

		if (conn == null)
			return tm;

		try
		{
			String propKey = PROPKEY_CODE_COMP_LOOKUP_TABLE_TYPES.replace("{PRODUCTNAME}", getDbProductName().replace(' ', '_'));
			String configTypes = Configuration.getCombinedConfiguration().getProperty(propKey);
			if (StringUtil.isNullOrBlank(configTypes))
				configTypes = null;

			List<String> configTypesList = StringUtil.parseCommaStrToList(configTypes); // null value will return an empty list
//System.out.println("getLookupTableTypesModel(): key='"+propKey+"', listSize="+configTypesList.size()+", listIsEmpty="+configTypesList.isEmpty()+", list="+configTypesList);

			DatabaseMetaData dbmd = conn.getMetaData();
			ResultSet rs = dbmd.getTableTypes();
			while (rs.next())
			{
				String type = StringUtils.trim(rs.getString(1));
				
				 // If the configTypesList is empty, then ALL should be set to true, otherwise just set the ones that are in the list
				boolean include = configTypesList.isEmpty();
				if (configTypesList.contains(type))
					include = true;

				Object[] oa = new Object[2];
				oa[0] = new Boolean(include);
				oa[1] = new String(type);
//System.out.println("getLookupTableTypesModel(): AddRow: include="+include+":(oa[0]="+oa[0]+"), type='"+type+"':(oa[1]='"+oa[1]+"').");

				tm.addRow(oa);
			}
			rs.close();
			
			return tm;
		}
		catch (SQLException e)
		{
			return tm;
		}
	}

	@Override
	public void setLookupTableTypes(List<String> tableTypes)
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
			return;

		String propKey = PROPKEY_CODE_COMP_LOOKUP_TABLE_TYPES.replace("{PRODUCTNAME}", getDbProductName().replace(' ', '_'));
		if ( tableTypes == null || (tableTypes != null && tableTypes.size() == 0) )
			conf.remove(propKey);
		else
			conf.setProperty(propKey, StringUtil.toCommaStr(tableTypes));
		conf.save();
//System.out.println("setLookupTableTypes(): key='"+propKey+"', list="+tableTypes);
	}

	protected String[] getTableTypes(Connection conn)
	{
		try
		{
			String propKey = PROPKEY_CODE_COMP_LOOKUP_TABLE_TYPES.replace("{PRODUCTNAME}", getDbProductName().replace(' ', '_'));
			String configTypes = Configuration.getCombinedConfiguration().getProperty(propKey);
			if (configTypes == null)
				return null;

			List<String> configTypesList = StringUtil.parseCommaStrToList(configTypes);
			List<String> addTypesList    = new ArrayList<String>();
			List<String> skipTypesList   = new ArrayList<String>();

			DatabaseMetaData dbmd = conn.getMetaData();
			ResultSet rs = dbmd.getTableTypes();
			while (rs.next())
			{
				String type = StringUtils.trim(rs.getString(1));
				if (configTypesList.contains(type))
					addTypesList.add(type);
				else
					skipTypesList.add(type);
			}
			rs.close();
			
			_logger.info("Code Completion: refreshCompletionForTables.getTableTypes(): key='"+propKey+"', addList="+addTypesList+", SkipList="+skipTypesList);

			if (addTypesList.size() > 0)
				return addTypesList.toArray(new String[0]);
			return null;
		}
		catch (SQLException e)
		{
			return null;
		}
	}
	
	protected void enrichCompletionForTables(Connection conn, WaitForExecDialog waitDialog)
	throws SQLException
	{
	}
	protected List<TableInfo> refreshCompletionForTables(Connection conn, WaitForExecDialog waitDialog)
	throws SQLException
	{
		return refreshCompletionForTables(conn, waitDialog, null, null, null);
	}
	protected List<TableInfo> refreshCompletionForTables(Connection conn, WaitForExecDialog waitDialog, String catalogName, String schemaName, String tableName)
	throws SQLException
	{
//System.out.println("SQL: refreshCompletionForTables()");
		// Obtain a DatabaseMetaData object from our current connection        
		DatabaseMetaData dbmd = conn.getMetaData();

		// Each table description has the following columns: 
		// 1:  TABLE_CAT String                 => table catalog (may be null) 
		// 2:  TABLE_SCHEM String               => table schema (may be null) 
		// 3:  TABLE_NAME String                => table name 
		// 4:  TABLE_TYPE String                => table type. Typical types are "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM". 
		// 5:  REMARKS String                   => explanatory comment on the table 
		// 6:  TYPE_CAT String                  => the types catalog (may be null) 
		// 7:  TYPE_SCHEM String                => the types schema (may be null) 
		// 8:  TYPE_NAME String                 => type name (may be null) 
		// 9:  SELF_REFERENCING_COL_NAME String => name of the designated "identifier" column of a typed table (may be null) 
		// 10: REF_GENERATION String            => specifies how values in SELF_REFERENCING_COL_NAME are created. Values are "SYSTEM", "USER", "DERIVED". (may be null) 

		final String stateMsg = "Getting Table information.";
		waitDialog.setState(stateMsg);

		ArrayList<TableInfo> tableInfoList = new ArrayList<TableInfo>();

		if (waitDialog.wasCancelPressed())
			return tableInfoList;

		if (schemaName != null)
		{
			schemaName = schemaName.replace('*', '%').trim();
			if ( isWildcatdMath() && ! schemaName.endsWith("%") )
				schemaName += "%";
		}

		if (tableName == null)
			tableName = "%";
		else
		{
			tableName = tableName.replace('*', '%').trim();
			if ( isWildcatdMath() && ! tableName.endsWith("%") )
				tableName += "%";
		}

		// What table types do we want to retrieve
		String[] types = getTableTypes(conn);

		if (_logger.isDebugEnabled())
			_logger.debug("refreshCompletionForTables(): calling dbmd.getTables(catalog='"+catalogName+"', schema='"+schemaName+"', table='"+tableName+"', types='"+StringUtil.toCommaStr(types)+"')");

		ResultSet rs = dbmd.getTables(catalogName, schemaName, tableName, types);
		
		MonTablesDictionary mtd = MonTablesDictionaryManager.hasInstance() ? MonTablesDictionaryManager.getInstance() : null;
		int counter = 0;
		while(rs.next())
		{
			counter++;
			if ( (counter % 100) == 0 )
				waitDialog.setState(stateMsg + " (Fetch count "+counter+")");

			TableInfo ti = new TableInfo();
			ti._tabCat     = StringUtils.trim(rs.getString(1));
			ti._tabSchema  = StringUtils.trim(rs.getString(2));
			ti._tabName    = StringUtils.trim(rs.getString(3));
			ti._tabType    = StringUtils.trim(rs.getString(4));
			ti._tabRemark  = StringUtils.trim(rs.getString(5));

			// Check with the MonTable dictionary for Descriptions
			if (mtd != null && StringUtil.isNullOrBlank(ti._tabRemark))
				ti._tabRemark = mtd.getDescriptionForTable(ti._tabName);
				
			// add schemas... this is a Set so duplicates is ignored
			addSchema(ti._tabCat, ti._tabSchema);
			
			// special case for MySQL and DBMS that do not support schemas... just copy dbname into the schema field
			if ( ! _dbSupportsSchema && StringUtil.isNullOrBlank(ti._tabSchema))
				ti._tabSchema = ti._tabCat;

			tableInfoList.add(ti);
			
			if (waitDialog.wasCancelPressed())
				return tableInfoList;
		}
		rs.close();

		return tableInfoList;
	}

	protected List<TableColumnInfo> refreshCompletionForTableColumns(Connection conn, WaitForExecDialog waitDialog, String catalogName, String schemaName, String tableName, String colName)
	throws SQLException
	{
		DatabaseMetaData dbmd = conn.getMetaData();
		
		ArrayList<TableColumnInfo> retList = new ArrayList<TableColumnInfo>();

		final String stateMsg = "Getting Column information for table '"+tableName+"'.";
		waitDialog.setState(stateMsg);

//		// fix catalogName
//		if (catalogName != null)
//		{
//			catalogName = catalogName.replace('*', '%').trim();
//			if ( isWildcatdMath() && ! catalogName.endsWith("%") )
//				catalogName += "%";
//		}
//
//		// fix schemaName
//		if (schemaName != null)
//		{
//			schemaName = schemaName.replace('*', '%').trim();
//			if ( isWildcatdMath() && ! schemaName.endsWith("%") )
//				schemaName += "%";
//		}
//
//		// fix tableName
//		if (tableName == null)
//			tableName = "%";
//		else
//		{
//			tableName = tableName.replace('*', '%').trim();
//			if ( isWildcatdMath() && ! tableName.endsWith("%") )
//				tableName += "%";
//		}

		// fix colName
		if (colName == null)
			colName = "%";
		else
		{
			colName = colName.replace('*', '%').trim();
			if ( isWildcatdMath() && ! colName.endsWith("%") )
				colName += "%";
		}

		// Obtain a DatabaseMetaData object from our current connection
		ResultSet rs = dbmd.getColumns(catalogName, schemaName, tableName, colName);

		MonTablesDictionary mtd = MonTablesDictionaryManager.hasInstance() ? MonTablesDictionaryManager.getInstance() : null;
		int counter = 0;
		while(rs.next())
		{
			counter++;
			if ( (counter % 100) == 0 )
				waitDialog.setState(stateMsg + " (Fetch count "+counter+")");

			String tabCatalog = StringUtils.trim(rs.getString("TABLE_CAT"));
			String tabSchema  = StringUtils.trim(rs.getString("TABLE_SCHEM"));
			String tabName    = StringUtils.trim(rs.getString("TABLE_NAME"));
			
			TableColumnInfo ci = new TableColumnInfo();
			ci._colName       = StringUtils.trim(rs.getString("COLUMN_NAME"));
			ci._colPos        =                  rs.getInt   ("ORDINAL_POSITION");
			ci._colType       = StringUtils.trim(rs.getString("TYPE_NAME"));
			ci._colLength     =                  rs.getInt   ("COLUMN_SIZE");
			ci._colIsNullable =                  rs.getInt   ("NULLABLE");
			ci._colRemark     = StringUtils.trim(rs.getString("REMARKS"));
			ci._colDefault    = StringUtils.trim(rs.getString("COLUMN_DEF"));
			ci._colScale      =                  rs.getInt   ("DECIMAL_DIGITS");

			// Check with the MonTable dictionary for Descriptions
			if (mtd != null && StringUtil.isNullOrBlank(ci._colRemark))
				ci._colRemark = StringUtil.stripHtmlStartEnd(mtd.getDescription(tabName, ci._colName));

			retList.add(ci);
			
			if (waitDialog.wasCancelPressed())
				return retList;
		}
		rs.close();

		return retList;
	}

	protected void refreshCompletionForTableColumns(Connection conn, WaitForExecDialog waitDialog, List<TableInfo> tableInfoList, boolean bulkGetColumns)
	throws SQLException
	{
//System.out.println("SQL: refreshCompletionForTableColumns()");

		//------------------------------------------------------------------------
		// DatabaseMetaData..getColumns(null, null, "%", "%");
		//------------------------------------------------------------------------
		// Retrieves a description of table columns available in the specified catalog. 
		// Only column descriptions matching the catalog, schema, table and column name criteria are returned. They are ordered by TABLE_CAT,TABLE_SCHEM, TABLE_NAME, and ORDINAL_POSITION. 
		// 
		// Each column description has the following columns: 
		// TABLE_CAT         String => table catalog (may be null) 
		// TABLE_SCHEM       String => table schema (may be null) 
		// TABLE_NAME        String => table name 
		// COLUMN_NAME       String => column name 
		// DATA_TYPE         int    => SQL type from java.sql.Types 
		// TYPE_NAME         String => Data source dependent type name, for a UDT the type name is fully qualified 
		// COLUMN_SIZE       int    => column size. 
		//                                  BUFFER_LENGTH is not used. 
		// DECIMAL_DIGITS    int    => the number of fractional digits. Null is returned for data types where DECIMAL_DIGITS is not applicable. 
		// NUM_PREC_RADIX    int    => Radix (typically either 10 or 2) 
		// NULLABLE          int    => is NULL allowed. 
		//                                  columnNoNulls - might not allow NULL values 
		//                                  columnNullable - definitely allows NULL values 
		//                                  columnNullableUnknown - nullability unknown 
		// REMARKS           String => comment describing column (may be null) 
		// COLUMN_DEF        String => default value for the column, which should be interpreted as a string when the value is enclosed in single quotes (may be null) 
		// SQL_DATA_TYPE     int    => unused 
		// SQL_DATETIME_SUB  int    => unused 
		// CHAR_OCTET_LENGTH int    => for char types the maximum number of bytes in the column 
		// ORDINAL_POSITION  int    => index of column in table (starting at 1) 
		// IS_NULLABLE       String => ISO rules are used to determine the nullability for a column. 
		//                                  YES --- if the parameter can include NULLs 
		//                                  NO --- if the parameter cannot include NULLs 
		//                                  empty string --- if the nullability for the parameter is unknown 
		// SCOPE_CATLOG      String => catalog of table that is the scope of a reference attribute (null if DATA_TYPE isn't REF) 
		// SCOPE_SCHEMA      String => schema of table that is the scope of a reference attribute (null if the DATA_TYPE isn't REF) 
		// SCOPE_TABLE       String => table name that this the scope of a reference attribure (null if the DATA_TYPE isn't REF) 
		// SOURCE_DATA_TYPE  short  => source type of a distinct type or user-generated Ref type, SQL type from java.sql.Types (null if DATA_TYPE isn't DISTINCT or user-generated REF) 
		// IS_AUTOINCREMENT  String => Indicates whether this column is auto incremented 
		//                                  YES --- if the column is auto incremented 
		//                                  NO --- if the column is not auto incremented 
		//                                  empty string --- if it cannot be determined whether the column is auto incremented parameter is unknown 
		//                                  The COLUMN_SIZE column the specified column size for the given column. For numeric data, this is the maximum precision. For character data, this is the length in characters. For datetime datatypes, this is the length in characters of the String representation (assuming the maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes. For the ROWID datatype, this is the length in bytes. Null is returned for data types where the column size is not applicable.
		// 
		// Parameters:
		// - catalog a catalog name; must match the catalog name as it is stored in the database; "" retrieves those without a catalog; null means that the catalog name should not be used to narrow the search
		// - schemaPattern a schema name pattern; must match the schema name as it is stored in the database; "" retrieves those without a schema; null means that the schema name should not be used to narrow the search
		// - tableNamePattern a table name pattern; must match the table name as it is stored in the database 
		// - columnNamePattern a column name pattern; must match the column name as it is stored in the database
		//------------------------------------------------------------------------


		// Obtain a DatabaseMetaData object from our current connection        
		DatabaseMetaData dbmd = conn.getMetaData();

//		boolean bulkGetColumns = true;
//		boolean bulkGetColumns = tableInfoList.size() > 20;

//System.out.println("refreshCompletionForTableColumns(): bulkGetColumns="+bulkGetColumns);

		if (bulkGetColumns)
		{
			final String stateMsg = "Getting Column information for ALL tables.";
			waitDialog.setState(stateMsg);

			String prevTabName = "";
			TableInfo tabInfo = null;
	
			// Obtain a DatabaseMetaData object from our current connection
			ResultSet rs = dbmd.getColumns(null, null, "%", "%");

			MonTablesDictionary mtd = MonTablesDictionaryManager.hasInstance() ? MonTablesDictionaryManager.getInstance() : null;
			int counter = 0;
			while(rs.next())
			{
				counter++;
				if ( (counter % 100) == 0 )
					waitDialog.setState(stateMsg + " (Fetch count "+counter+")");

	//			String tabCatalog = StringUtils.trim(rs.getString("TABLE_CAT"));
	//			String tabSchema  = StringUtils.trim(rs.getString("TABLE_SCHEM"));
				String tabName    = StringUtils.trim(rs.getString("TABLE_NAME"));
				
				TableColumnInfo ci = new TableColumnInfo();
				ci._colName       = StringUtils.trim(rs.getString("COLUMN_NAME"));
				ci._colPos        =                  rs.getInt   ("ORDINAL_POSITION");
				ci._colType       = StringUtils.trim(rs.getString("TYPE_NAME"));
				ci._colLength     =                  rs.getInt   ("COLUMN_SIZE");
				ci._colIsNullable =                  rs.getInt   ("NULLABLE");
				ci._colRemark     = StringUtils.trim(rs.getString("REMARKS"));
				ci._colDefault    = StringUtils.trim(rs.getString("COLUMN_DEF"));
				ci._colScale      =                  rs.getInt   ("DECIMAL_DIGITS");
	
				if ( ! prevTabName.equals(tabName) )
				{
					prevTabName = tabName;
					tabInfo = getTableInfo(tabName);
				}
				if (tabInfo == null)
					continue;
	
				// Check with the MonTable dictionary for Descriptions
				if (mtd != null && StringUtil.isNullOrBlank(ci._colRemark))
					ci._colRemark = StringUtil.stripHtmlStartEnd(mtd.getDescription(tabName, ci._colName));

				tabInfo.addColumn(ci);
				
				if (waitDialog.wasCancelPressed())
					return;
			}
			rs.close();
		}
		else
		{
			// ADD Column information
			for (TableInfo ti : tableInfoList)
			{
//System.out.println("refreshCompletionForTableColumns(): bulkGetColumns="+bulkGetColumns+", TableInfo="+ti);
				if (waitDialog.wasCancelPressed())
					return;

				waitDialog.setState("Getting Column information for table '"+ti._tabName+"'.");
//				ti._needColumnRefresh = false;
//
//				ResultSet rs = dbmd.getColumns(ti._tabCat, ti._tabSchema, ti._tabName, "%");
//
//				MonTablesDictionary mtd = MonTablesDictionaryManager.hasInstance() ? MonTablesDictionaryManager.getInstance() : null;
//				while(rs.next())
//				{
//					TableColumnInfo ci = new TableColumnInfo();
//					ci._colName       = StringUtils.trim(rs.getString("COLUMN_NAME"));
//					ci._colPos        =                  rs.getInt   ("ORDINAL_POSITION");
//					ci._colType       = StringUtils.trim(rs.getString("TYPE_NAME"));
//					ci._colLength     =                  rs.getInt   ("COLUMN_SIZE");
//					ci._colIsNullable =                  rs.getInt   ("NULLABLE");
//					ci._colRemark     = StringUtils.trim(rs.getString("REMARKS"));
//					ci._colDefault    = StringUtils.trim(rs.getString("COLUMN_DEF"));
//					ci._colScale      =                  rs.getInt   ("DECIMAL_DIGITS");
//					
////System.out.println("refreshCompletionForTableColumns(): bulkGetColumns="+bulkGetColumns+", ROW="+ci);
//					// Check with the MonTable dictionary for Descriptions
//					if (mtd != null && StringUtil.isNullOrBlank(ci._colRemark))
//						ci._colRemark = StringUtil.stripHtmlStartEnd(mtd.getDescription(ti._tabName, ci._colName));
//
//					ti.addColumn(ci);
//				}
//				rs.close();

//				ti.refreshColumnInfo(_connectionProvider);
				ti.refreshColumnInfo(getConnection());

//				// PK INFO
//				rs = dbmd.getPrimaryKeys(null, null, ti._tabName);
//				ResultSetTableModel rstm = new ResultSetTableModel(rs, "getPrimaryKeys");
//				System.out.println("######### PK("+ti._tabName+"):--------\n" + rstm.toTableString());
//				while(rs.next())
//				{
//					TablePkInfo pk = new TablePkInfo();
//					pk._colName       = StringUtils.trim(rs.getString("COLUMN_NAME"));
//					
//					ti.addPk(pk);
//				}
//				rs.close();

//				// INDEX INFO
//				rs = dbmd.getIndexInfo(null, null, ti._tabName, false, false);
//				rstm = new ResultSetTableModel(rs);
//				System.out.println("########## INDEX("+ti._tabName+"):--------\n" + rstm.toTableString());
//				while(rs.next())
//				{
//					TableIndexInfo index = new TableIndexInfo();
//					index._colName       = StringUtils.trim(rs.getString("COLUMN_NAME"));
//					
//					ti.addIndex(index);
//				}
//				rs.close();
			} // end: for (TableInfo ti : tableInfoList)
		} // end: fetch-table-by-table
	}



	//------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------
	// FUNCTIONS
	//------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------
	public String decodeFunctionType(int type)
	{
		if      (type == DatabaseMetaData.functionResultUnknown) return "Function (Return-Unknown)";
		else if (type == DatabaseMetaData.functionNoTable)       return "Function (Return-Value)";
		else if (type == DatabaseMetaData.functionReturnsTable)  return "Function (Returns-Table)";
		else return "Function (unknown-type="+type+")";
	}

	protected void enrichCompletionForFunctions(Connection conn, WaitForExecDialog waitDialog)
	throws SQLException
	{
	}
	protected List<FunctionInfo> refreshCompletionForFunctions(Connection conn, WaitForExecDialog waitDialog)
	throws SQLException
	{
		return refreshCompletionForFunctions(conn, waitDialog, null, null, null);
	}
	protected List<FunctionInfo> refreshCompletionForFunctions(Connection conn, WaitForExecDialog waitDialog, String catalogName, String schemaName, String functionName)
	throws SQLException
	{
//System.out.println("SQL: refreshCompletionForFunctions()");
//new Exception("DUMMY STACKTRACE").printStackTrace();
		// Obtain a DatabaseMetaData object from our current connection        
		DatabaseMetaData dbmd = conn.getMetaData();

		// Each table description has the following columns: 

        // 1: FUNCTION_CAT String => function catalog (may be null)
        // 2: FUNCTION_SCHEM String => function schema (may be null)
        // 3: FUNCTION_NAME String => function name. This is the name used to invoke the function
        // 4: REMARKS String => explanatory comment on the function
        // 5: FUNCTION_TYPE short => kind of function:
        //      functionResultUnknown - Cannot determine if a return value or table will be returned
        //      functionNoTable- Does not return a table
        //      functionReturnsTable - Returns a table 
        // 6: SPECIFIC_NAME String => the name which uniquely identifies this function within its schema. This is a user specified, or DBMS generated, name that may be different then the FUNCTION_NAME for example with overload functions 

		final String stateMsg = "Getting Function information.";
		waitDialog.setState(stateMsg);

		ArrayList<FunctionInfo> functionInfoList = new ArrayList<FunctionInfo>();

		if (waitDialog.wasCancelPressed())
			return functionInfoList;

		if (schemaName != null)
		{
			schemaName = schemaName.replace('*', '%').trim();
			if ( isWildcatdMath() && ! schemaName.endsWith("%") )
				schemaName += "%";
		}

		if (functionName == null)
			functionName = "%";
		else
		{
			functionName = functionName.replace('*', '%').trim();
			if ( isWildcatdMath() && ! functionName.endsWith("%") )
				functionName += "%";
		}

//		// What table types do we want to retrieve
//		String[] types = getTableTypes(conn);

		if (_logger.isDebugEnabled())
			_logger.debug("refreshCompletionForFunctions(): calling dbmd.getFunctions(catalog='"+catalogName+"', schema='"+schemaName+"', function='"+functionName+"')");
//System.out.println("XXXX(): calling dbmd.getFunctions(catalog='"+catalogName+"', schema='"+schemaName+"', function='"+functionName+"')");

		ResultSet rs = dbmd.getFunctions(catalogName, schemaName, functionName);
		
		MonTablesDictionary mtd = MonTablesDictionaryManager.hasInstance() ? MonTablesDictionaryManager.getInstance() : null;
		int counter = 0;
		while(rs.next())
		{
			counter++;
			if ( (counter % 100) == 0 )
				waitDialog.setState(stateMsg + " (Fetch count "+counter+")");

			// Oracle dosn't seem to support TABLE_CATALOG so do workaround
			boolean getTypeInt = true;

			FunctionInfo fi = new FunctionInfo();
			                        fi._funcCat     = StringUtils.trim( rs.getString("FUNCTION_CAT"));
			                        fi._funcSchema  = StringUtils.trim( rs.getString("FUNCTION_SCHEM"));
			                        fi._funcName    = removeSystemChars(rs.getString("FUNCTION_NAME"));
			if (getTypeInt) { try { fi._funcTypeInt =                   rs.getInt   ("FUNCTION_TYPE"); } catch(SQLException ex) { getTypeInt = false; if (_logger.isDebugEnabled()) _logger.warn("Problems getting 'FUNCTION_TYPE' in refreshCompletionForFunctions(), Caught: '"+ex+"', for: FUNCTION_CAT='"+fi._funcCat+"', FUNCTION_SCHEM='"+fi._funcSchema+"', FUNCTION_NAME='"+fi._funcName+"'."); }	}
			                        fi._funcRemark  = StringUtils.trim( rs.getString("REMARKS"));
//			                        fi._specificName= StringUtils.trim( rs.getString("SPECIFIC_NAME"));

			fi._funcType = decodeFunctionType(fi._funcTypeInt);
			fi._isTableValuedFunction = (fi._funcTypeInt == DatabaseMetaData.functionReturnsTable);
			if (_logger.isDebugEnabled())
				_logger.debug("refreshCompletionForFunctions: ROW("+counter+")-ADD: fi="+fi);

			// add schemas... this is a Set so duplicates is ignored
			addSchema(fi._funcCat, fi._funcSchema);

			// special case for MySQL and DBMS that do not support schemas... just copy dbname into the schema field
			if ( ! _dbSupportsSchema && StringUtil.isNullOrBlank(fi._funcSchema))
				fi._funcSchema = fi._funcCat;

			// Check with the MonTable dictionary for Descriptions
			if (mtd != null && StringUtil.isNullOrBlank(fi._funcName))
				fi._funcRemark = mtd.getDescriptionForTable(fi._funcName);

			functionInfoList.add(fi);
			
			if (waitDialog.wasCancelPressed())
				return functionInfoList;
		}
		rs.close();

		return functionInfoList;
	}

	protected List<FunctionColumnInfo> refreshCompletionForFunctionColumns(Connection conn, WaitForExecDialog waitDialog, String catalogName, String schemaName, String functionName, String colName)
	throws SQLException
	{
		DatabaseMetaData dbmd = conn.getMetaData();
		
		ArrayList<FunctionColumnInfo> retList = new ArrayList<FunctionColumnInfo>();

		final String stateMsg = "Getting Column information for function '"+functionName+"'.";
		waitDialog.setState(stateMsg);

//		// fix catalogName
//		if (catalogName != null)
//		{
//			catalogName = catalogName.replace('*', '%').trim();
//			if ( isWildcatdMath() && ! catalogName.endsWith("%") )
//				catalogName += "%";
//		}
//
//		// fix schemaName
//		if (schemaName != null)
//		{
//			schemaName = schemaName.replace('*', '%').trim();
//			if ( isWildcatdMath() && ! schemaName.endsWith("%") )
//				schemaName += "%";
//		}
//
//		// fix tableName
//		if (tableName == null)
//			tableName = "%";
//		else
//		{
//			tableName = tableName.replace('*', '%').trim();
//			if ( isWildcatdMath() && ! tableName.endsWith("%") )
//				tableName += "%";
//		}

		// fix colName
		if (colName == null)
			colName = "%";
		else
		{
			colName = colName.replace('*', '%').trim();
			if ( isWildcatdMath() && ! colName.endsWith("%") )
				colName += "%";
		}

		// Obtain a DatabaseMetaData object from our current connection
		ResultSet rs = dbmd.getFunctionColumns(catalogName, schemaName, functionName, colName);


        // 1:  FUNCTION_CAT String => function catalog (may be null)
        // 2:  FUNCTION_SCHEM String => function schema (may be null)
        // 3:  FUNCTION_NAME String => function name. This is the name used to invoke the function
        // 4:  COLUMN_NAME String => column/parameter name
        // 5:  COLUMN_TYPE Short => kind of column/parameter:
        //       functionColumnUnknown - nobody knows
        //       functionColumnIn - IN parameter
        //       functionColumnInOut - INOUT parameter
        //       functionColumnOut - OUT parameter
        //       functionColumnReturn - function return value
        //       functionColumnResult - Indicates that the parameter or column is a column in the ResultSet 
        // 6:  DATA_TYPE int => SQL type from java.sql.Types
        // 7:  TYPE_NAME String => SQL type name, for a UDT type the type name is fully qualified
        // 8:  PRECISION int => precision
        // 9:  LENGTH int => length in bytes of data
        // 10: SCALE short => scale - null is returned for data types where SCALE is not applicable.
        // 11: RADIX short => radix
        // 12: NULLABLE short => can it contain NULL.
        //       functionNoNulls - does not allow NULL values
        //       functionNullable - allows NULL values
        //       functionNullableUnknown - nullability unknown 
        // 13: REMARKS String => comment describing column/parameter
        // 14: CHAR_OCTET_LENGTH int => the maximum length of binary and character based parameters or columns. For any other datatype the returned value is a NULL
        // 15: ORDINAL_POSITION int => the ordinal position, starting from 1, for the input and output parameters. A value of 0 is returned if this row describes the function's return value. For result set columns, it is the ordinal position of the column in the result set starting from 1.
        // 16: IS_NULLABLE String => ISO rules are used to determine the nullability for a parameter or column.
        //       YES --- if the parameter or column can include NULLs
        //       NO --- if the parameter or column cannot include NULLs
        //       empty string --- if the nullability for the parameter or column is unknown 
        // 17: SPECIFIC_NAME String => the name which uniquely identifies this function within its schema. This is a user specified, or DBMS generated, name that may be different then the FUNCTION_NAME for example with overload functions 
        //

		MonTablesDictionary mtd = MonTablesDictionaryManager.hasInstance() ? MonTablesDictionaryManager.getInstance() : null;
		int counter = 0;
		while(rs.next())
		{
			counter++;
			if ( (counter % 100) == 0 )
				waitDialog.setState(stateMsg + " (Fetch count "+counter+")");

			String funcCatalog = StringUtils.trim( rs.getString("FUNCTION_CAT"));
			String funcSchema  = StringUtils.trim( rs.getString("FUNCTION_SCHEM"));
			String funcName    = removeSystemChars(rs.getString("FUNCTION_NAME"));
			
			FunctionColumnInfo ci = new FunctionColumnInfo();
			ci._colName       = StringUtils.trim(rs.getString("COLUMN_NAME"));
			ci._colPos        =                  rs.getInt   ("ORDINAL_POSITION");
			ci._colType       = StringUtils.trim(rs.getString("TYPE_NAME"));
			ci._colLength     =                  rs.getInt   ("LENGTH");
			ci._colIsNullable =                  rs.getInt   ("NULLABLE");
			ci._colRemark     = StringUtils.trim(rs.getString("REMARKS"));
//			ci._colDefault    = StringUtils.trim(rs.getString("COLUMN_DEF"));
//			ci._colScale      =                  rs.getInt   ("DECIMAL_DIGITS");

			// Check with the MonTable dictionary for Descriptions
			if (mtd != null && StringUtil.isNullOrBlank(ci._colRemark))
				ci._colRemark = StringUtil.stripHtmlStartEnd(mtd.getDescription(funcName, ci._colName));

			retList.add(ci);
			
			if (waitDialog.wasCancelPressed())
				return retList;
		}
		rs.close();

		return retList;
	}

	protected void refreshCompletionForFunctionColumns(Connection conn, WaitForExecDialog waitDialog, List<FunctionInfo> functionInfoList, boolean bulkGetColumns)
	throws SQLException
	{
//System.out.println("SQL: refreshCompletionForTableColumns()");

		//------------------------------------------------------------------------
		// DatabaseMetaData..getColumns(null, null, "%", "%");
		//------------------------------------------------------------------------
		// Retrieves a description of table columns available in the specified catalog. 
		// Only column descriptions matching the catalog, schema, table and column name criteria are returned. They are ordered by TABLE_CAT,TABLE_SCHEM, TABLE_NAME, and ORDINAL_POSITION. 
		// 
		// Each column description has the following columns: 
		// TABLE_CAT         String => table catalog (may be null) 
		// TABLE_SCHEM       String => table schema (may be null) 
		// TABLE_NAME        String => table name 
		// COLUMN_NAME       String => column name 
		// DATA_TYPE         int    => SQL type from java.sql.Types 
		// TYPE_NAME         String => Data source dependent type name, for a UDT the type name is fully qualified 
		// COLUMN_SIZE       int    => column size. 
		//                                  BUFFER_LENGTH is not used. 
		// DECIMAL_DIGITS    int    => the number of fractional digits. Null is returned for data types where DECIMAL_DIGITS is not applicable. 
		// NUM_PREC_RADIX    int    => Radix (typically either 10 or 2) 
		// NULLABLE          int    => is NULL allowed. 
		//                                  columnNoNulls - might not allow NULL values 
		//                                  columnNullable - definitely allows NULL values 
		//                                  columnNullableUnknown - nullability unknown 
		// REMARKS           String => comment describing column (may be null) 
		// COLUMN_DEF        String => default value for the column, which should be interpreted as a string when the value is enclosed in single quotes (may be null) 
		// SQL_DATA_TYPE     int    => unused 
		// SQL_DATETIME_SUB  int    => unused 
		// CHAR_OCTET_LENGTH int    => for char types the maximum number of bytes in the column 
		// ORDINAL_POSITION  int    => index of column in table (starting at 1) 
		// IS_NULLABLE       String => ISO rules are used to determine the nullability for a column. 
		//                                  YES --- if the parameter can include NULLs 
		//                                  NO --- if the parameter cannot include NULLs 
		//                                  empty string --- if the nullability for the parameter is unknown 
		// SCOPE_CATLOG      String => catalog of table that is the scope of a reference attribute (null if DATA_TYPE isn't REF) 
		// SCOPE_SCHEMA      String => schema of table that is the scope of a reference attribute (null if the DATA_TYPE isn't REF) 
		// SCOPE_TABLE       String => table name that this the scope of a reference attribure (null if the DATA_TYPE isn't REF) 
		// SOURCE_DATA_TYPE  short  => source type of a distinct type or user-generated Ref type, SQL type from java.sql.Types (null if DATA_TYPE isn't DISTINCT or user-generated REF) 
		// IS_AUTOINCREMENT  String => Indicates whether this column is auto incremented 
		//                                  YES --- if the column is auto incremented 
		//                                  NO --- if the column is not auto incremented 
		//                                  empty string --- if it cannot be determined whether the column is auto incremented parameter is unknown 
		//                                  The COLUMN_SIZE column the specified column size for the given column. For numeric data, this is the maximum precision. For character data, this is the length in characters. For datetime datatypes, this is the length in characters of the String representation (assuming the maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes. For the ROWID datatype, this is the length in bytes. Null is returned for data types where the column size is not applicable.
		// 
		// Parameters:
		// - catalog a catalog name; must match the catalog name as it is stored in the database; "" retrieves those without a catalog; null means that the catalog name should not be used to narrow the search
		// - schemaPattern a schema name pattern; must match the schema name as it is stored in the database; "" retrieves those without a schema; null means that the schema name should not be used to narrow the search
		// - tableNamePattern a table name pattern; must match the table name as it is stored in the database 
		// - columnNamePattern a column name pattern; must match the column name as it is stored in the database
		//------------------------------------------------------------------------


		// Obtain a DatabaseMetaData object from our current connection        
		DatabaseMetaData dbmd = conn.getMetaData();

//		boolean bulkGetColumns = true;
//		boolean bulkGetColumns = tableInfoList.size() > 20;

//System.out.println("refreshCompletionForFunctionColumns(): bulkGetColumns="+bulkGetColumns);

		if (bulkGetColumns)
		{
			final String stateMsg = "Getting Column information for ALL functions.";
//new Exception("GET_CALLBACK").printStackTrace();
			waitDialog.setState(stateMsg);

			String prevFuncName = "";
			FunctionInfo funcInfo = null;
	
			// Obtain a DatabaseMetaData object from our current connection
			ResultSet rs = dbmd.getColumns(null, null, "%", "%");

			MonTablesDictionary mtd = MonTablesDictionaryManager.hasInstance() ? MonTablesDictionaryManager.getInstance() : null;
			int counter = 0;
			while(rs.next())
			{
				counter++;
				if ( (counter % 100) == 0 )
					waitDialog.setState(stateMsg + " (Fetch count "+counter+")");

	//			String tabCatalog = StringUtils.trim( rs.getString("TABLE_CAT"));
	//			String tabSchema  = StringUtils.trim( rs.getString("TABLE_SCHEM"));
				String funcName   = removeSystemChars(rs.getString("TABLE_NAME"));
				
				FunctionColumnInfo ci = new FunctionColumnInfo();
				ci._colName       = StringUtils.trim(rs.getString("COLUMN_NAME"));
				ci._colPos        =                  rs.getInt   ("ORDINAL_POSITION");
				ci._colType       = StringUtils.trim(rs.getString("TYPE_NAME"));
				ci._colLength     =                  rs.getInt   ("COLUMN_SIZE");
				ci._colIsNullable =                  rs.getInt   ("NULLABLE");
				ci._colRemark     = StringUtils.trim(rs.getString("REMARKS"));
				ci._colDefault    = StringUtils.trim(rs.getString("COLUMN_DEF"));
				ci._colScale      =                  rs.getInt   ("DECIMAL_DIGITS");
	
				if ( ! prevFuncName.equals(funcName) )
				{
					prevFuncName = funcName;
					funcInfo = getFunctionInfo(funcName);
				}
				if (funcInfo == null)
					continue;
	
				// Check with the MonTable dictionary for Descriptions
				if (mtd != null && StringUtil.isNullOrBlank(ci._colRemark))
					ci._colRemark = StringUtil.stripHtmlStartEnd(mtd.getDescription(funcName, ci._colName));

				funcInfo.addColumn(ci);
				
				if (waitDialog.wasCancelPressed())
					return;
			}
			rs.close();
		}
		else
		{
			// ADD Column information
			for (FunctionInfo fi : functionInfoList)
			{
//System.out.println("refreshCompletionForFunctionColumns(): bulkGetColumns="+bulkGetColumns+", FunctionInfo="+fi);
				if (waitDialog.wasCancelPressed())
					return;

				waitDialog.setState("Getting Column information for function '"+fi._funcName+"'.");
				fi._needColumnRefresh = false;

				ResultSet rs = dbmd.getColumns(fi._funcCat, fi._funcSchema, fi._funcName, "%");

				MonTablesDictionary mtd = MonTablesDictionaryManager.hasInstance() ? MonTablesDictionaryManager.getInstance() : null;
				while(rs.next())
				{
					FunctionColumnInfo ci = new FunctionColumnInfo();
					ci._colName       = StringUtils.trim(rs.getString("COLUMN_NAME"));
					ci._colPos        =                  rs.getInt   ("ORDINAL_POSITION");
					ci._colType       = StringUtils.trim(rs.getString("TYPE_NAME"));
					ci._colLength     =                  rs.getInt   ("COLUMN_SIZE");
					ci._colIsNullable =                  rs.getInt   ("NULLABLE");
					ci._colRemark     = StringUtils.trim(rs.getString("REMARKS"));
					ci._colDefault    = StringUtils.trim(rs.getString("COLUMN_DEF"));
					ci._colScale      =                  rs.getInt   ("DECIMAL_DIGITS");
					
//System.out.println("refreshCompletionForFunctionColumns(): bulkGetColumns="+bulkGetColumns+", ROW="+ci);

					// Check with the MonTable dictionary for Descriptions
					if (mtd != null && StringUtil.isNullOrBlank(ci._colRemark))
						ci._colRemark = StringUtil.stripHtmlStartEnd(mtd.getDescription(fi._funcName, ci._colName));

					fi.addColumn(ci);
				}
				rs.close();

			} // end: for (FunctionInfo fi : functionInfoList)
		} // end: fetch-func-by-func
	}


	//------------------------------------------------------------------------------------------------------
	// PROCEDURES
	//------------------------------------------------------------------------------------------------------
	protected List<ProcedureInfo> refreshCompletionForProcedures(Connection conn, WaitForExecDialog waitDialog)
	throws SQLException
	{
		return refreshCompletionForProcedures(conn, waitDialog, null, null, null);
	}
	protected List<ProcedureInfo> refreshCompletionForProcedures(Connection conn, WaitForExecDialog waitDialog, String catalogName, String schemaName, String procName)
	throws SQLException
	{
//System.out.println("SQL: refreshCompletionForProcedures()");

		// Obtain a DatabaseMetaData object from our current connection        
		DatabaseMetaData dbmd = conn.getMetaData();

//		// What PRODUCT are we connected to 
//		String dbProductName = dbmd.getDatabaseProductName();
		
		//------------------------------------------------------------------------
		// Each procedure description has the the following columns: 
		// 1: PROCEDURE_CAT   String => procedure catalog (may be null) 
		// 2: PROCEDURE_SCHEM String => procedure schema (may be null) 
		// 3: PROCEDURE_NAME  String => procedure name 
		// 4: reserved for future use 
		// 5: reserved for future use 
		// 6: reserved for future use 
		// 7: REMARKS         String => explanatory comment on the procedure 
		// 8: PROCEDURE_TYPE  short  => kind of procedure: 
		//       * procedureResultUnknown - Cannot determine if a return value will be returned 
		//       * procedureNoResult - Does not return a return value 
		//       * procedureReturnsResult - Returns a return value 
		// 9: SPECIFIC_NAME   String => The name which uniquely identifies this procedure within its schema. 
		//------------------------------------------------------------------------

		final String stateMsg = "Getting Procedure information.";
		waitDialog.setState(stateMsg);

		ArrayList<ProcedureInfo> procInfoList = new ArrayList<ProcedureInfo>();

		if (waitDialog.wasCancelPressed())
			return procInfoList;

		if (schemaName != null)
		{
			schemaName = schemaName.replace('*', '%').trim();
			if ( isWildcatdMath() && ! schemaName.endsWith("%") )
				schemaName += "%";
		}

		if (procName == null)
			procName = "%";
		else
		{
			procName = procName.replace('*', '%').trim();
			if ( isWildcatdMath() && ! procName.endsWith("%") )
				procName += "%";
		}

		_logger.debug("refreshCompletionForProcedures(): calling dbmd.getProcedures(catalog='"+catalogName+"', schema=null, procName='"+procName+"')");

		ResultSet rs = dbmd.getProcedures(catalogName, schemaName, procName);

		MonTablesDictionary mtd = MonTablesDictionaryManager.hasInstance() ? MonTablesDictionaryManager.getInstance() : null;
		int counter = 0;
		while(rs.next())
		{
			counter++;
			if ( (counter % 100) == 0 )
				waitDialog.setState(stateMsg + " (Fetch count "+counter+")");

			ProcedureInfo pi = new ProcedureInfo();
			pi._procCat          = StringUtils.trim(   rs.getString("PROCEDURE_CAT"));
			pi._procSchema       = StringUtils.trim(   rs.getString("PROCEDURE_SCHEM"));
			pi._procName         = removeSystemChars(  rs.getString("PROCEDURE_NAME"));
			pi._procType         = decodeProcedureType(rs.getInt(   "PROCEDURE_TYPE"));
			pi._procRemark       = StringUtils.trim(   rs.getString("REMARKS"));
//			pi._procSpecificName = StringUtils.trim(   rs.getString("SPECIFIC_NAME")); //in HANA = not there...

//System.out.println("refreshCompletionForProcedures() ADD: pi="+pi);
			// add schemas... this is a Set so duplicates is ignored
			addSchema(pi._procCat, pi._procSchema);

			// special case for MySQL and DBMS that do not support schemas... just copy dbname into the schema field
			if ( ! _dbSupportsSchema && StringUtil.isNullOrBlank(pi._procSchema))
				pi._procSchema = pi._procCat;

			// Check with the MonTable dictionary for Descriptions
			if (mtd != null && StringUtil.isNullOrBlank(pi._procRemark))
				pi._procRemark = mtd.getDescriptionForTable(pi._procName);

			procInfoList.add(pi);
			
			if (waitDialog.wasCancelPressed())
				return procInfoList;
		}
		rs.close();
		
		// Special for ORACLE, get procedures with PACKAGES
		if (conn instanceof OracleConnection)
		{
			OracleConnection oraConn = (OracleConnection) conn;
			List<ProcedureInfo> oraPackProcs = oraConn.getPackageProcedures(waitDialog, catalogName, schemaName, procName);
			
			procInfoList.addAll(oraPackProcs);
		}
		
		
		return procInfoList;
	}
	public String decodeProcedureType(int type)
	{
		if      (type == DatabaseMetaData.procedureResultUnknown) return "Procedure (Result-Unknown)";
		else if (type == DatabaseMetaData.procedureNoResult)      return "Procedure (No-Result)";
		else if (type == DatabaseMetaData.procedureReturnsResult) return "Procedure (Returns-Results)";
		else return "Procedure (unknown-type="+type+")";
	}

	protected void refreshCompletionForProcedureParameters(Connection conn, WaitForExecDialog waitDialog, List<ProcedureInfo> procedureInfoList, boolean bulkMode )
	throws SQLException
	{
//System.out.println("SQL: refreshCompletionForProcedureParameters()");

		//------------------------------------------------------------------------
		//Each row in the ResultSet is a parameter description or column description with the following fields: 
		//
		//1:  PROCEDURE_CAT      String => procedure catalog (may be null) 
		//2:  PROCEDURE_SCHEM    String => procedure schema (may be null) 
		//3:  PROCEDURE_NAME     String => procedure name 
		//4:  COLUMN_NAME        String => column/parameter name 
		//5:  COLUMN_TYPE        Short  => kind of column/parameter: 
		//      * procedureColumnUnknown - nobody knows 
		//      * procedureColumnIn      - IN parameter 
		//      * procedureColumnInOut   - INOUT parameter 
		//      * procedureColumnOut     - OUT parameter 
		//      * procedureColumnReturn  - procedure return value 
		//      * procedureColumnResult  - result column in ResultSet 
		//6:  DATA_TYPE          int    => SQL type from java.sql.Types 
		//7:  TYPE_NAME          String => SQL type name, for a UDT type the type name is fully qualified 
		//8:  PRECISION          int    => precision 
		//9:  LENGTH             int    => length in bytes of data 
		//10: SCALE              short  => scale - null is returned for data types where SCALE is not applicable. 
		//11: RADIX              short  => radix 
		//12: NULLABLE           short  => can it contain NULL. 
		//      * procedureNoNulls         - does not allow NULL values 
		//      * procedureNullable        - allows NULL values 
		//      * procedureNullableUnknown - nullability unknown 
		//13: REMARKS            String => comment describing parameter/column 
		//14: COLUMN_DEF         String => default value for the column, which should be interpreted as a string when the value is enclosed in single quotes (may be null) 
		//      * The string NULL (not enclosed in quotes) - if NULL was specified as the default value 
		//      * TRUNCATE (not enclosed in quotes)        - if the specified default value cannot be represented without truncation 
		//      * NULL                                     - if a default value was not specified 
		//15: SQL_DATA_TYPE      int    => reserved for future use 
		//16: SQL_DATETIME_SUB   int    => reserved for future use 
		//17: CHAR_OCTET_LENGTH  int    => the maximum length of binary and character based columns. For any other datatype the returned value is a NULL 
		//18: ORDINAL_POSITION   int    => the ordinal position, starting from 1, for the input and output parameters for a procedure. A value of 0 is returned if this row describes the procedure's return value. For result set columns, it is the ordinal position of the column in the result set starting from 1. If there are multiple result sets, the column ordinal positions are implementation defined. 
		//19: IS_NULLABLE        String => ISO rules are used to determine the nullability for a column. 
		//      * YES          --- if the parameter can include NULLs 
		//      * NO           --- if the parameter cannot include NULLs 
		//      * empty string --- if the nullability for the parameter is unknown 
		//20: SPECIFIC_NAME      String => the name which uniquely identifies this procedure within its schema. 
		//------------------------------------------------------------------------
		// The above dosnt seems to be true, here is a output of the resultset
		//
		// ResultSetMetaData rsmd = rs.getMetaData();
		// for (int c=1; c<rsmd.getColumnCount(); c++)
		//     System.out.println("c="+c+", Label='"+rsmd.getColumnLabel(c)+"', ColName='"+rsmd.getColumnName(c)+"'.");
		//
		// c=1,  Label='PROCEDURE_CAT',   ColName='PROCEDURE_CAT'.
		// c=2,  Label='PROCEDURE_SCHEM', ColName='PROCEDURE_SCHEM'.
		// c=3,  Label='PROCEDURE_NAME',  ColName='PROCEDURE_NAME'.
		// c=4,  Label='COLUMN_NAME',     ColName='COLUMN_NAME'.
		// c=5,  Label='COLUMN_TYPE',     ColName='COLUMN_TYPE'.
		// c=6,  Label='DATA_TYPE',       ColName='DATA_TYPE'.
		// c=7,  Label='TYPE_NAME',       ColName='TYPE_NAME'.
		// c=8,  Label='PRECISION',       ColName='PRECISION'.
		// c=9,  Label='LENGTH',          ColName='LENGTH'.
		// c=10, Label='SCALE',           ColName='SCALE'.
		// c=11, Label='RADIX',           ColName='RADIX'.
		// c=12, Label='NULLABLE',        ColName='NULLABLE'.
		//------------------------------------------------------------------------

		// Obtain a DatabaseMetaData object from our current connection        
		DatabaseMetaData dbmd = conn.getMetaData();

//		boolean getBulk = true;

		if (bulkMode)
		{
			final String stateMsg = "Getting Procedure Parameter information for ALL procedures.";
			waitDialog.setState(stateMsg);

			String prevProcName = "";
			ProcedureInfo procInfo = null;
			int colId = 0;

			// Get params for all procs
			ResultSet rs = dbmd.getProcedureColumns(null, null, "%", "%");

			MonTablesDictionary mtd = MonTablesDictionaryManager.hasInstance() ? MonTablesDictionaryManager.getInstance() : null;
			int counter = 0;
			while(rs.next())
			{
				counter++;
				if ( (counter % 100) == 0 )
					waitDialog.setState(stateMsg + " (Fetch count "+counter+")");

				colId++;
	//			String procCatalog = StringUtils.trim( rs.getString("PROCEDURE_CAT"));
	//			String procSchema  = StringUtils.trim( rs.getString("PROCEDURE_SCHEM"));
				String procName    = removeSystemChars(rs.getString("PROCEDURE_NAME"));
				
				ProcedureParameterInfo pi = new ProcedureParameterInfo();
				pi._paramName       = StringUtils.trim(rs.getString("COLUMN_NAME"));
				pi._paramPos        = colId;
				pi._paramInOutType  = procInOutDecode( rs.getShort("COLUMN_TYPE")); // IN - OUT - INOUT
				pi._paramType       = StringUtils.trim(rs.getString("TYPE_NAME"));
				pi._paramLength     =                  rs.getInt   ("LENGTH");
				pi._paramIsNullable =                  rs.getInt   ("NULLABLE");
				pi._paramRemark     = StringUtils.trim(rs.getString("REMARKS"));
				pi._paramDefault    = StringUtils.trim(rs.getString("COLUMN_DEF"));
				pi._paramScale      =                  rs.getInt   ("SCALE");
	
				if ( ! prevProcName.equals(procName) )
				{
					colId = 0;
					prevProcName = procName;
					procInfo = getProcedureInfo(procName);
				}
				if (procInfo == null)
					continue;
	
				// Check with the MonTable dictionary for Descriptions
				if (mtd != null && StringUtil.isNullOrBlank(pi._paramRemark))
					pi._paramRemark = StringUtil.stripHtmlStartEnd(mtd.getDescription(procName, pi._paramName));

				procInfo.addParameter(pi);
				
				if (waitDialog.wasCancelPressed())
					return;
			}
			rs.close();
		}
		else
		{
			for (ProcedureInfo pi : procedureInfoList)
			{
//System.out.println("refreshCompletionForProcedureParameters(): bulkMode="+bulkMode+", ProcedureInfo="+pi);
				if (waitDialog.wasCancelPressed())
					return;

				waitDialog.setState("Getting Parameter information for Procedure '"+pi._procName+"'.");
				pi._needParamsRefresh = false;

				int colId = 0;
				ResultSet rs = dbmd.getProcedureColumns(pi._procCat, pi._procSchema, pi._procName, "%");

				MonTablesDictionary mtd = MonTablesDictionaryManager.hasInstance() ? MonTablesDictionaryManager.getInstance() : null;
				while(rs.next())
				{
					colId++;

					ProcedureParameterInfo ppi = new ProcedureParameterInfo();
					ppi._paramName       = StringUtils.trim(rs.getString("COLUMN_NAME"));
					ppi._paramPos        = colId;
					ppi._paramInOutType  = procInOutDecode( rs.getShort("COLUMN_TYPE")); // IN - OUT - INOUT
					ppi._paramType       = StringUtils.trim(rs.getString("TYPE_NAME"));
					ppi._paramLength     =                  rs.getInt   ("LENGTH");
					ppi._paramIsNullable =                  rs.getInt   ("NULLABLE");
					ppi._paramRemark     = StringUtils.trim(rs.getString("REMARKS"));
					ppi._paramDefault    = StringUtils.trim(rs.getString("COLUMN_DEF"));
					ppi._paramScale      =                  rs.getInt   ("SCALE");
					
//System.out.println("refreshCompletionForProcedureParameters(): bulkMode="+bulkMode+", ROW="+ppi);
					// Check with the MonTable dictionary for Descriptions

					if (mtd != null && StringUtil.isNullOrBlank(ppi._paramRemark))
						ppi._paramRemark = StringUtil.stripHtmlStartEnd(mtd.getDescription(pi._procName, ppi._paramName));

					pi.addParameter(ppi);
				}
				rs.close();

			} // end: for (TableInfo ti : tableInfoList)
		} // end: fetch-row-by-row
	}

	/**
	 * Decode DatabaseMetaData getProcedureColumns COLUMN_TYPE
	 * @param type
	 * @return
	 */
	public static String procInOutDecode(short type)
	{
		switch (type)
		{
		case DatabaseMetaData.procedureColumnIn:      return "IN";
		case DatabaseMetaData.procedureColumnOut:     return "OUT";
		case DatabaseMetaData.procedureColumnInOut:   return "INOUT";
		case DatabaseMetaData.procedureColumnResult:  return "RESULT";
		case DatabaseMetaData.procedureColumnReturn:  return "RETURN";
		case DatabaseMetaData.procedureColumnUnknown: return "UNKNOWN";
		}
		return "unknown("+type+")";
	}


	//------------------------------------------------------------------------------------------------------
	// SYSTEM PROCEDURES
	//------------------------------------------------------------------------------------------------------
	protected List<ProcedureInfo> refreshCompletionForSystemProcedures(Connection conn, WaitForExecDialog waitDialog)
	throws SQLException
	{
//System.out.println("ASE: refreshCompletionForSystemProcedures()");
		final String stateMsg = "Getting Procedure information";
		waitDialog.setState(stateMsg);

		ArrayList<ProcedureInfo> procInfoList = new ArrayList<ProcedureInfo>();

		return procInfoList;
	}

	protected void refreshCompletionForSystemProcedureParameters(Connection conn, WaitForExecDialog waitDialog, List<ProcedureInfo> procedureInfoList)
	throws SQLException
	{
//System.out.println("ASE: refreshCompletionForSystemProcedureParameters()");
		final String stateMsg = "Getting Procedure Parameter information";
		waitDialog.setState(stateMsg);

		// ADD Column information
		for (ProcedureInfo pi : procedureInfoList)
		{
			if (waitDialog.wasCancelPressed())
				return;

			waitDialog.setState("Getting Parameter information for Procedure '"+pi._procName+"'.");
		}
	}

	/**
	 * Go and get the Completions for the underlying JDBC Connection
	 * @return
	 */
//	protected List<SqlTableCompletion> refreshCompletion()
	protected <T extends AbstractCompletionX> List<T> refreshCompletion()
	{
//System.out.println("SQL: refreshCompletion()");
//		final Connection conn = _connectionProvider.getConnection();
		final DbxConnection conn = getConnection();
		if (conn == null)
			return null;

		// Create a Waitfor Dialog and Executor, then execute it.
		WaitForExecDialog wait = new WaitForExecDialog(_guiOwner, "Refreshing SQL Completion");

if (_guiOwner == null)
	System.out.println("WARNING: refreshCompletion(): WaitForExecDialog() _guiOwner=-NULL-");

		WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
		{
			// This is the object that will be returned.
//			ArrayList<SqlTableCompletion> completionList = new ArrayList<SqlTableCompletion>();
			ArrayList<ShorthandCompletionX> completionList = new ArrayList<ShorthandCompletionX>();
						
			@Override
			public boolean canDoCancel() { return true; };
			
			@Override
			public void cancel() 
			{
				_logger.info("refreshSqlCompletion(), CANCEL has been called.");
				// The WaitForExecDialog: wait.wasCancelPressed() will be true
			};
			
			@Override
			public Object doWork()
			{
//try {Thread.sleep(15000);} catch(InterruptedException ignore) {}
				boolean autoCommit = true;

				try
				{
					long allStartTime  = System.currentTimeMillis();
					long thisStartTime = System.currentTimeMillis();

					thisStartTime = System.currentTimeMillis();
					
					// Checking AutoCommit
					// if FALSE, set it to TRUE during refresh, otherwise it will fail, atleast for ASE
//					autoCommit = conn.getAutoCommit(); 
					autoCommit = DbUtils.getAutoCommitNoThrow(conn, _dbProductName); 
					if (autoCommit == false)
					{
						_logger.info("Code Completion: Refresh can NOT be done while in AutoCommit=false, switching to true while refreshing, then I will set it back.");
						autoCommit = DbUtils.setAutoCommit(conn, _dbProductName, getGuiOwner(), true, "This request came from the <b>Auto Completion</b> subsystem.<br><b>Note</b>: AutoCommit will be restored to <b>FALSE</b> after the Auto Completion has been refreshed.");
					}

					//----------------------------------------------------------
					// Always do this
					try
					{
						getWaitDialog().setState("Creating Mandatory Completions.");
						refreshCompletionForMandatory(conn, getWaitDialog());
						_logger.debug("---------------- Refresh Completion: Mandatory Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));
					}
					catch(SQLException sqle)
					{
						_logger.info ("Problems when getting Mandatory Info, continuing with next lookup. Caught: "+sqle);
						_logger.debug("Problems when getting Mandatory Info, continuing with next lookup. Caught: "+sqle, sqle);
					}

					//----------------------------------------------------------
					// Get Static Commands Completion
					if (isLookupStaticCmds())
					{
						getWaitDialog().setState("Creating Static Cmds Completions.");
						refreshCompletionForStaticCmds();
						_logger.debug("---------------- Refresh Completion: MISC Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));
					}

					//----------------------------------------------------------
					// Get Miscellaneous Completion
					if (isLookupMisc())
					{
						try
						{
							getWaitDialog().setState("Creating Miscelanious Completions.");
							refreshCompletionForMisc(conn, getWaitDialog());
							_logger.debug("---------------- Refresh Completion: MISC Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));
						}
						catch(SQLException sqle)
						{
							_logger.info ("Problems when getting Miscelenious Info, continuing with next lookup. Caught: "+sqle);
							_logger.debug("Problems when getting Miscelenious Info, continuing with next lookup. Caught: "+sqle, sqle);
						}
					}

					//----------------------------------------------------------
					// Get database information
					if (isLookupDb())
					{
						try
						{
							thisStartTime = System.currentTimeMillis();
							//----------------------------------------------------------
							// Get DB information
							_dbInfoList = refreshCompletionForDbs(conn, getWaitDialog());
							_logger.debug("---------------- Refresh Completion: DB Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));

//							// If SQLExceptions has been down graded to SQLWarnings in the jConnect message handler
//							AseConnectionUtils.checkSqlWarningsAndThrowSqlExceptionIfSeverityIsAbove10(conn.getWarnings());
//							
							// Create completion list
							getWaitDialog().setState("Creating Database Completions.");
							_dbComplList.clear();
							for (DbInfo di : _dbInfoList)
							{
		//						SqlDbCompletion c = new SqlDbCompletion(_thisBaseClass, di);
								SqlDbCompletion c = new SqlDbCompletion(CompletionProviderAbstractSql.this, di);
								completionList.add(c);
								_dbComplList.add(c);
							}
						}
						catch(SQLException sqle)
						{
							_logger.info ("Problems when getting Database/Catalog info, continuing with next lookup. Caught: "+sqle);
							_logger.debug("Problems when getting Database/Catalog info, continuing with next lookup. Caught: "+sqle, sqle);
						}
					}


					//----------------------------------------------------------
					// Get Table and Columns information
					if (isLookupTableName())
					{
						try
						{
							thisStartTime = System.currentTimeMillis();
							_tableInfoList = refreshCompletionForTables(conn, getWaitDialog());
							enrichCompletionForTables(conn, getWaitDialog());
							_logger.debug("---------------- Refresh Completion: TAB-1 Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));
							if (isLookupTableColumns())
							{
								if (_tableInfoList.size() < 25)
									refreshCompletionForTableColumns(conn, getWaitDialog(), _tableInfoList, false);

//								refreshCompletionForTableColumns(conn, getWaitDialog(), _tableInfoList, true);
//								_logger.debug("---------------- Refresh Completion: TAB-2 Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));
							}
		
							// Create completion list
							getWaitDialog().setState("Creating Table Completions.");
							_tableComplList.clear();
							for (TableInfo ti : _tableInfoList)
							{
								SqlTableCompletion c = new SqlTableCompletion(CompletionProviderAbstractSql.this, ti, false, _addSchemaName, _quoteTableNames);
								completionList.add(c);
								_tableComplList.add(c);
							}
							
							// Add all other schemas (that dosn't have a table)
							if (isLookupSchemaWithNoTables())
							{
								List<SchemaInfo> allSchemas = refreshCompletionForSchemas(conn, getWaitDialog(), null, null);

								// If there are NO schemas, fall back to use catalogs instead (this is mainly for MySql)
//								if (allSchemas.isEmpty() && _dbComplList != null)
//								{
//									for (DbInfo di : _dbInfoList)
//										_schemaNames.add(di._dbName);
//								}
								for (SchemaInfo si : allSchemas)
									addSchema(si._cat, si._name);
							}

							// Add all schema names
							for (String schemaName : _schemaNames)
							{
								SqlSchemaCompletion c = new SqlSchemaCompletion(CompletionProviderAbstractSql.this, schemaName);
								completionList.add(c);
							}
						}
						catch(SQLException sqle)
						{
							_logger.info ("Problems when getting SQL Tables/columns, continuing with next lookup. Caught: "+sqle);
							_logger.debug("Problems when getting SQL Tables/columns, continuing with next lookup. Caught: "+sqle, sqle);
						}
					}


					//----------------------------------------------------------
					// Get Function and Columns information
					if (isLookupFunctionName())
					{
						try
						{
							thisStartTime = System.currentTimeMillis();
							_functionInfoList = refreshCompletionForFunctions(conn, getWaitDialog());
							_logger.debug("---------------- Refresh Completion: FUNC-1 Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));
							if (isLookupFunctionColumns())
							{
								if (_functionInfoList.size() < 25)
									refreshCompletionForFunctionColumns(conn, getWaitDialog(), _functionInfoList, false);
								
//								refreshCompletionForFunctionColumns(conn, getWaitDialog(), _functionInfoList, true);
//								_logger.debug("---------------- Refresh Completion: FUNC-2 Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));
							}
		
							// Create completion list
							getWaitDialog().setState("Creating Function Completions.");
							_functionComplList.clear();
							for (FunctionInfo ti : _functionInfoList)
							{
								SqlFunctionCompletion c = new SqlFunctionCompletion(CompletionProviderAbstractSql.this, ti, false, _addSchemaName, _quoteTableNames);
								completionList.add(c);
								_functionComplList.add(c);
							}
							
//							// Add all other schemas (that dosn't have a table)
//							if (isLookupSchemaWithNoTables())
//							{
//	    						List<SchemaInfo> allSchemas = refreshCompletionForSchemas(conn, getWaitDialog(), null, null);
//	    						for (SchemaInfo si : allSchemas)
//	    							_schemaNames.add(si._name);
//							}
	//
//							// Add all schema names
//							for (String schemaName : _schemaNames)
//							{
//								SqlSchemaCompletion c = new SqlSchemaCompletion(CompletionProviderAbstractSql.this, schemaName);
//								completionList.add(c);
//							}
						}
						catch(SQLException sqle)
						{
							_logger.info ("Problems when getting SQL Functions/params, continuing with next lookup. Caught: "+sqle);
							_logger.debug("Problems when getting SQL Functions/params, continuing with next lookup. Caught: "+sqle, sqle);
						}
					}


					//----------------------------------------------------------
					// Get USER Procedure and Parameters information
					if (isLookupProcedureName())
					{
						try
						{
							thisStartTime = System.currentTimeMillis();
							_procedureInfoList = refreshCompletionForProcedures(conn, getWaitDialog());
							_logger.debug("---------------- Refresh Completion: PROC-1 Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));
							if (isLookupProcedureColumns())
							{
								refreshCompletionForProcedureParameters(conn, getWaitDialog(), _procedureInfoList, true);
								_logger.debug("---------------- Refresh Completion: PROC-2 Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));
							}
		
							getWaitDialog().setState("Creating Procedure Completions.");
							_procedureComplList.clear();
							for (ProcedureInfo pi : _procedureInfoList)
							{
		//						SqlProcedureCompletion c = new SqlProcedureCompletion(_thisBaseClass, pi);
								SqlProcedureCompletion c = new SqlProcedureCompletion(CompletionProviderAbstractSql.this, pi, false, null, _addSchemaName, _quoteTableNames);
								_procedureComplList.add(c);
							}
						}
						catch(SQLException sqle)
						{
							_logger.info ("Problems when getting SQL Procedures/params, continuing with next lookup. Caught: "+sqle);
							_logger.debug("Problems when getting SQL Procedures/params, continuing with next lookup. Caught: "+sqle, sqle);
						}
					}

					//----------------------------------------------------------
					// Get SYSTEM Procedure and Parameters information
					// Only do this once
					if (isLookupSystemProcedureName())
					{
						try
						{
							thisStartTime = System.currentTimeMillis();
							if ( _systemProcInfoList.size() == 0 || needRefreshSystemInfo() )
							{
								_systemProcInfoList = refreshCompletionForSystemProcedures(conn, getWaitDialog());
								_logger.debug("---------------- Refresh Completion: SYS PROC-1 Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));
								if (isLookupSystemProcedureColumns())
								{
									refreshCompletionForSystemProcedureParameters(conn, getWaitDialog(), _systemProcInfoList);
									_logger.debug("---------------- Refresh Completion: SYS PROC-2 Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));
								}
		
								getWaitDialog().setState("Creating System Procedure Completions.");
								_systemProcComplList.clear();
								for (ProcedureInfo pi : _systemProcInfoList)
								{
		//							SqlProcedureCompletion c = new SqlProcedureCompletion(_thisBaseClass, pi);
									SqlProcedureCompletion c = new SqlProcedureCompletion(CompletionProviderAbstractSql.this, pi, false, null, false, _quoteTableNames);
									_systemProcComplList.add(c);
								}
							}
						}
						catch(SQLException sqle)
						{
							_logger.info ("Problems when getting SQL System Procedures, continuing with next lookup. Caught: "+sqle);
							_logger.debug("Problems when getting SQL System Procedures, continuing with next lookup. Caught: "+sqle, sqle);
						}
					}

					_logger.debug("Refresh Completion: TOTAL Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-allStartTime));
				}
//				catch (SQLException e)
//				{
//					_logger.info("Problems reading table information for SQL Table code completion.", e);
//				}
				finally
				{
					// restore AutoCommit if it was changed during refresh
					if (autoCommit == false)
					{
						_logger.info("Code Completion: Restoring  AutoCommit=false, this was made after refresh was done.");
						try { conn.setAutoCommit(false); }
						catch (SQLException ignore) {}
					}
				}

				return completionList;
			}
		}; // END: new WaitForExecDialog.BgExecutor()

		// Execute and WAIT
//		ArrayList<SqlTableCompletion> list = (ArrayList)wait.execAndWait(doWork);
		ArrayList<T> list = (ArrayList) wait.execAndWait(doWork);

		if (list == null)
		{
			if (doWork.hasException())
				_logger.error("Problems when refreshing Code Completion. Caught:"+doWork.getException(), doWork.getException());
		}

		return list;
    }

	/**
	 * 
	 * @param conn
	 * @param sqlObj
	 * @return
	 */
	public List<Completion> getTableListWithGuiProgress(DbxConnection conn, SqlObjectName sqlObj)
	{
		List<Completion> tabList;
		
		// get tables
		if (sqlObj._dbSupportsSchema)
			tabList = getTableListWithGuiProgress(conn, sqlObj.getCatalogName(), sqlObj.getSchemaName(), sqlObj.getObjectName());
		else
			tabList = getTableListWithGuiProgress(conn, sqlObj.getSchemaName(), null, sqlObj.getObjectName());

		// NONE FOUND: Check once again... but with the *original* text that was put into SqlObjectName
		if ( tabList == null || (tabList != null && tabList.isEmpty()) )
		{
			if (_logger.isDebugEnabled())
				_logger.debug("getTableListWithGuiProgress(DbxConnection, SqlObjectName): NO Tables was found, TRY ORIGIN NAMES.");
			
			if (sqlObj._dbSupportsSchema)
				tabList = getTableListWithGuiProgress(conn, sqlObj.getCatalogNameOrigin(), sqlObj.getSchemaNameOrigin(), sqlObj.getObjectNameOrigin());
			else
				tabList = getTableListWithGuiProgress(conn, sqlObj.getSchemaNameOrigin(), null, sqlObj.getObjectNameOrigin());
		}

		return tabList;
	}

	/**
	 * Lookup TABLES "on the fly", do not cache
	 * 
	 * @param conn
	 * @param catName
	 * @param objName
	 * @return
	 */
//	private List<Completion> getTableListWithGuiProgress(final Connection conn, final String catName, final String schemaName, final String objName)
	public List<Completion> getTableListWithGuiProgress(final Connection conn, final String catName, final String schemaName, final String objName)
	{
//new Exception("DUMMY EXCEPTION at: getTableListWithGuiProgress(conn='"+conn+"', catName='"+catName+"', schemaName='"+schemaName+"', objName='"+objName+"')").printStackTrace();
		// Create a Waitfor Dialog and Executor, then execute it.
		WaitForExecDialog wait = new WaitForExecDialog(_guiOwner, "Refreshing SQL Completion");

		WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
		{
			// This is the object that will be returned.
//			ArrayList<SqlTableCompletion> completionList = new ArrayList<SqlTableCompletion>();
			ArrayList<Completion> completionList = new ArrayList<Completion>();
						
			@Override
			public boolean canDoCancel() { return true; };
			
			@Override
			public void cancel() 
			{
				_logger.info("refreshSqlCompletion(), CANCEL has been called.");
				// The WaitForExecDialog: wait.wasCancelPressed() will be true
			};
			
			@Override
			public Object doWork()
			{
				List<TableInfo>    tableInfoList    = new ArrayList<>();
				List<FunctionInfo> functionInfoList = new ArrayList<>();

				//----------------------------------------------------------
				// Get Table and Columns information
				try
				{
					getWaitDialog().setState("Getting Table Completions.");
					tableInfoList = refreshCompletionForTables(conn, getWaitDialog(), catName, schemaName, objName);

					// Add it to the global table so we don't have to do the same lookup next time
					_tableInfoList.addAll(tableInfoList);
					if (tableInfoList.size() < 25)
						refreshCompletionForTableColumns(conn, getWaitDialog(), tableInfoList, false);
				}
				catch(SQLException ex)
				{
					_logger.info("Problems reading table information for SQL Table code completion. Skipping Tables and continuing.", ex);
				}

				//----------------------------------------------------------
				// Get Function and Columns information (for Table Valued Functions)
				try
				{
					getWaitDialog().setState("Getting Function Completions.");
					functionInfoList = refreshCompletionForFunctions(conn, getWaitDialog(), catName, schemaName, objName);

					// Add it to the global table so we don't have to do the same lookup next time
					_functionInfoList.addAll(functionInfoList);
					if (functionInfoList.size() < 25)
						refreshCompletionForFunctionColumns(conn, getWaitDialog(), functionInfoList, false);
				}
				catch(SQLException ex)
				{
					_logger.info("Problems reading table information for SQL Function code completion. Skipping Functions and continuing.", ex);
				}


				// Create completion list for tables
				getWaitDialog().setState("Creating Table Completions.");
				for (TableInfo ti : tableInfoList)
				{
					SqlTableCompletion c = new SqlTableCompletion(CompletionProviderAbstractSql.this, ti, true, _addSchemaName, _quoteTableNames);
					_tableComplList.add(c);
					completionList.add(c);
				}

				// Create completion list for table valued functions
				getWaitDialog().setState("Creating Table Valued Function Completions.");
				for (FunctionInfo fi : functionInfoList)
				{
					if (fi._isTableValuedFunction)
					{
						SqlFunctionCompletion c = new SqlFunctionCompletion(CompletionProviderAbstractSql.this, fi, true, _addSchemaName, _quoteTableNames);
						_functionComplList.add(c);
						completionList.add(c);
					}
				}

				if (_logger.isDebugEnabled())
					_logger.debug("getTableListWithGuiProgress(): BgExecutor.doWork(): returned completionList.size() = " + (completionList == null ? null : completionList.size()) );

				return completionList;
			}
		}; // END: new WaitForExecDialog.BgExecutor()
		
		// Execute and WAIT
		ArrayList<Completion> list = (ArrayList)wait.execAndWait(doWork, 250);
		if (list == null)
		{
			if (doWork.hasException())
				_logger.error("Problems when refreshing Code Completion (for getTableListWithGuiProgress). Caught:"+doWork.getException(), doWork.getException());
		}

		if (_logger.isDebugEnabled())
			_logger.debug("getTableListWithGuiProgress(): returned list.size() = " + (list == null ? null : list.size()) );

		return list;
	}

//	/**
//	 * Lookup TABLE COLUMNS "on the fly", do not cache
//	 * 
//	 * @param conn
//	 * @param catName
//	 * @param objName
//	 * @return
//	 */
////	private List<Completion> getColumnListWithGuiProgress(final Connection conn, final String catName, final String schemaName, final String objName, final String colName)
//	private List<TableColumnInfo> getColumnListWithGuiProgress(final Connection conn, final String catName, final String schemaName, final String objName, final String colName)
//	{
//		// Create a Waitfor Dialog and Executor, then execute it.
//		WaitForExecDialog wait = new WaitForExecDialog(_guiOwner, "Refreshing SQL Completion");
//
//		WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
//		{
//			// This is the object that will be returned.
////			ArrayList<SqlTableCompletion> completionList = new ArrayList<SqlTableCompletion>();
//			ArrayList<Completion> completionList = new ArrayList<Completion>();
//						
//			@Override
//			public boolean canDoCancel() { return true; };
//			
//			@Override
//			public void cancel() 
//			{
//				_logger.info("refreshSqlCompletion(), CANCEL has been called.");
//				// The WaitForExecDialog: wait.wasCancelPressed() will be true
//			};
//			
//			@Override
//			public Object doWork()
//			{
//				try
//				{
//					//----------------------------------------------------------
//					// Get Columns informaation for table
//					return refreshCompletionForTableColumns(conn, getWaitDialog(), catName, schemaName, objName, colName);
////					List<TableColumnInfo> tableColumnInfoList = refreshCompletionForTableColumns(conn, getWaitDialog(), catName, schemaName, objName, colName);
////
////					// Create completion list
////					getWaitDialog().setState("Creating Table Column Completions.");
////					for (TableColumnInfo tci : tableColumnInfoList)
////					{
////						SqlColumnCompletion c = new SqlColumnCompletion(CompletionProviderAbstractSql.this, tci, true, _addSchemaName, _quoteTableNames);
//////						public SqlColumnCompletion(CompletionProvider provider, String tabAliasName, String colname, TableInfo tableInfo)
////
////						completionList.add(c);
////					}
//				}
//				catch (SQLException e)
//				{
//					_logger.info("Problems reading table information for SQL Table code completion.", e);
//				}
//
//				return completionList;
//			}
//		}; // END: new WaitForExecDialog.BgExecutor()
//		
//		// Execute and WAIT
////		ArrayList<Completion> list = (ArrayList)wait.execAndWait(doWork);
//		ArrayList<TableColumnInfo> list = (ArrayList)wait.execAndWait(doWork);
//		if (list == null)
//		{
//			if (doWork.hasException())
//				_logger.error("Problems when refreshing Code Completion (for getTableListWithGuiProgress). Caught:"+doWork.getException(), doWork.getException());
//		}
//
//		return list;
//	}
	
	/**
	 * Lookup PROCEDURES "on the fly", do not cache
	 * 
	 * @param conn
	 * @param catName
	 * @param objName
	 * @return
	 */
	private List<Completion> getProcedureListWithGuiProgress(final Connection conn, final String catName, final String schemaName, final String objName)
	{
		// Create a Waitfor Dialog and Executor, then execute it.
		WaitForExecDialog wait = new WaitForExecDialog(_guiOwner, "Refreshing SQL Completion");

		WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
		{
			// This is the object that will be returned.
//			ArrayList<SqlTableCompletion> completionList = new ArrayList<SqlTableCompletion>();
			ArrayList<Completion> localCompletionList = new ArrayList<Completion>();
						
			@Override
			public boolean canDoCancel() { return true; };
			
			@Override
			public void cancel() 
			{
				_logger.info("refreshSqlCompletion(), CANCEL has been called.");
				// The WaitForExecDialog: wait.wasCancelPressed() will be true
			};
			
			@Override
			public Object doWork()
			{
				try
				{
					//----------------------------------------------------------
					// Get Table and Columns informaation
					if (isLookupTableName())
					{
						getWaitDialog().setState("Getting Procedure Completions.");
						List<ProcedureInfo> procInfoList = refreshCompletionForProcedures(conn, getWaitDialog(), catName, schemaName, objName);

						// Add it to the global table so we don't have to do the same lookup next time
						_procedureInfoList.addAll(procInfoList);
						if (procInfoList.size() < 25)
							refreshCompletionForProcedureParameters(conn, getWaitDialog(), procInfoList, false);

						// Create completion list
						getWaitDialog().setState("Creating Procedure Completions.");
						for (ProcedureInfo pi : procInfoList)
						{
							SqlProcedureCompletion c = new SqlProcedureCompletion(CompletionProviderAbstractSql.this, pi, true, catName, _addSchemaName, _quoteTableNames);
							_procedureComplList.add(c);
							localCompletionList.add(c);
						}
					}
				}
				catch (SQLException e)
				{
					_logger.info("Problems reading table information for SQL Procedure code completion.", e);
				}

				return localCompletionList;
			}
		}; // END: new WaitForExecDialog.BgExecutor()
		
		// Execute and WAIT
		ArrayList<Completion> list = (ArrayList)wait.execAndWait(doWork);
		if (list == null)
		{
			if (doWork.hasException())
				_logger.error("Problems when refreshing Code Completion (for getProcListWithGuiProgress). Caught:"+doWork.getException(), doWork.getException());
		}

		return list;
	}

	
	/**
	 * Lookup PROCEDURES "on the fly", do not cache
	 * 
	 * @param conn
	 * @param catName
	 * @param objName
	 * @return
	 */
	private List<Completion> getSchemaListWithGuiProgress(final Connection conn, final String catName, final String schemaName)
	{
		// Create a Waitfor Dialog and Executor, then execute it.
		WaitForExecDialog wait = new WaitForExecDialog(_guiOwner, "Refreshing SQL Completion");

		WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
		{
			// This is the object that will be returned.
//			ArrayList<SqlTableCompletion> completionList = new ArrayList<SqlTableCompletion>();
			ArrayList<Completion> completionList = new ArrayList<Completion>();
						
			@Override
			public boolean canDoCancel() { return true; };
			
			@Override
			public void cancel() 
			{
				_logger.info("refreshSqlCompletion(), CANCEL has been called.");
				// The WaitForExecDialog: wait.wasCancelPressed() will be true
			};
			
			@Override
			public Object doWork()
			{
				try
				{
					//----------------------------------------------------------
					// Get Table and Columns information
					if (isLookupTableName())
					{
						List<SchemaInfo> schemaInfoList = refreshCompletionForSchemas(conn, getWaitDialog(), catName, schemaName);
	
						// Create completion list
						getWaitDialog().setState("Creating Schema Completions.");
						for (SchemaInfo si : schemaInfoList)
						{
//							SqlSchemaCompletion c = new SqlSchemaCompletion(CompletionProviderAbstractSql.this, pi, true, catName, _addSchemaName, _quoteTableNames);
							SqlSchemaCompletion c = new SqlSchemaCompletion(CompletionProviderAbstractSql.this, si, catName, _quoteTableNames);
							completionList.add(c);
						}
					}
				}
				catch (SQLException e)
				{
					_logger.info("Problems reading table information for SQL Schema code completion.", e);
				}

				return completionList;
			}
		}; // END: new WaitForExecDialog.BgExecutor()
		
		// Execute and WAIT
		ArrayList<Completion> list = (ArrayList)wait.execAndWait(doWork);
		if (list == null)
		{
			if (doWork.hasException())
				_logger.error("Problems when refreshing Code Completion (for getSchemaListWithGuiProgress). Caught:"+doWork.getException(), doWork.getException());
		}

		return list;
	}

	
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	// COMPLETIONS
	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	/** 
	 * Check if input contains normal chars, or non-normal chars
	 * @return "[" + name + "]" if the name contains anything other than Letter or Digits. If only normal chars it returns same as the input.
	 */
	public String fixStrangeNames(String name)
	{
		if (name == null)
			return null;

		boolean normalChars = true;
		for (int i=0; i<name.length(); i++)
		{
			char c = name.charAt(i);
//			if ( ! Character.isLetterOrDigit(c) )
//			{
//				normalChars = false;
//				break;
//			}
			// goto next character for any "allowed" characters
			if ( Character.isLetterOrDigit(c) ) continue;
			if ( c == '_' )                     continue;
//			if ( StringUtil.isCharIn(c, _dbExtraNameCharacters)) continue;
			if (_dbExtraNameCharacters != null && _dbExtraNameCharacters.indexOf(c) >= 0) continue;

			// if any other chars, then break and signal "non-normal-char" detected
			normalChars = false;
			break;
		}

		if (allowSquareBracketsAroundIdentifiers(_dbProductName))
			return normalChars ? name : "["+name+"]";
		else
			return normalChars ? name : _dbIdentifierQuoteString + name + _dbIdentifierQuoteString;
	}
	
	protected static boolean allowSquareBracketsAroundIdentifiers(String productName)
	{
		if (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(productName)) return true;
		if (DbUtils.DB_PROD_NAME_MSSQL     .equals(productName)) return true;
		if (DbUtils.DB_PROD_NAME_SYBASE_IQ .equals(productName)) return true;  // think so but not sure
		if (DbUtils.DB_PROD_NAME_SYBASE_ASA.equals(productName)) return true;  // think so but not sure

		return false;
	}

//	protected static class SqlCompletion
//	extends ShorthandCompletionX
//	implements Serializable
//	{
//		private static final long serialVersionUID = 1L;
//
//		public SqlCompletion(CompletionProvider provider, String inputText, String replacementText)
//		{
//			super(provider, inputText, replacementText);
//		}
//	}
//    /**
//	 * Our own Completion class, which overrides toString() to make it HTML aware
//	 */
//	protected static class SqlDbCompletion
//	extends SqlCompletion
//	{
//		private static final long serialVersionUID = 1L;
//
//		private DbInfo _dbInfo = null;
//		
//		public SqlDbCompletion(CompletionProviderAbstractSql provider, DbInfo di)
//		{
//			super(provider, di._dbName, fixStrangeNames(di._dbName));
//
//			_dbInfo = di;
//
//			String shortDesc = 
//				"<font color='blue'>"+di._dbType+"</font>" +
////				" -- <i><font color='green'>" + (StringUtil.isNullOrBlank(di._dbRemark) ? "No Description" : di._dbRemark) + "</font></i>";
//				" -- <i><font color='green'>" + (StringUtil.isNullOrBlank(di._dbRemark) ? "" : di._dbRemark) + "</font></i>";
//			setShortDescription(shortDesc);
////			setSummary(_dbInfo.toHtmlString());
//		}
//
//		@Override
//		public String getSummary()
//		{
//			return _dbInfo.toHtmlString();
//		}
//
//		/**
//		 * Make it HTML aware
//		 */
//		@Override
//		public String toString()
//		{
//			return "<html><body>" + super.toString() + "</body></html>";
//		}
//	}

//    /**
//	 * Our own Completion class, which overrides toString() to make it HTML aware
//	 */
//	protected static class SqlSchemaCompletion
//	extends SqlCompletion
//	{
//		private static final long serialVersionUID = 1L;
//
//		private SchemaInfo _schemaInfo = null;
//		
//		public SqlSchemaCompletion(CompletionProviderAbstractSql provider, String schemaName)
//		{
//			super(provider, schemaName, fixStrangeNames(schemaName)+".");
//
//			String shortDesc = 
//				"<font color='blue'>"+schemaName+"</font>" +
//				" -- <i><font color='green'>SCHEMA</font></i>";
//			setShortDescription(shortDesc);
//		}
//
//		public static String createReplacementText(SchemaInfo si, String catName, boolean quoteNames)
//		{
//			String q = _dbIdentifierQuoteString;
//			String catalogName = quoteNames ? q+si._cat+q  : fixStrangeNames(si._cat);
//			String schemaName  = quoteNames ? q+si._name+q : fixStrangeNames(si._name);
//
//			String out = "";
//			out += ((catName == null) ? catalogName : catName) + ".";
//			out += schemaName;
//			
//			return out;
//		}
//		public SqlSchemaCompletion(CompletionProviderAbstractSql provider, SchemaInfo si, String catName, boolean quoteNames)
//		{
//			super(provider, si._name, createReplacementText(si, catName, quoteNames));
//
//			_schemaInfo = si;
//
//			String shortDesc = 
//				"<font color='blue'>"+si._name+"</font>" +
//				" -- <i><font color='green'>" + (StringUtil.isNullOrBlank(si._remark) ? "" : si._remark) + "</font></i>";
//			setShortDescription(shortDesc);
////			setSummary(_schemaInfo.toHtmlString());
//		}
//
//		@Override
//		public String getSummary()
//		{
//			if (_schemaInfo != null)
//				return _schemaInfo.toHtmlString();
//			return super.getSummary();
//		}
//
//		/**
//		 * Make it HTML aware
//		 */
//		@Override
//		public String toString()
//		{
//			return "<html><body>" + super.toString() + "</body></html>";
//		}
//	}

//    /**
//	 * Our own Completion class, which overrides toString() to make it HTML aware
//	 */
//	protected static class SqlTableCompletion
//	extends SqlCompletion
//	{
//		private static final long serialVersionUID = 1L;
//
//		private TableInfo _tableInfo = null;
//
//		public static String createReplacementText(TableInfo ti, boolean addCatalog, boolean addSchema, boolean quoteNames)
//		{
//			String q = _dbIdentifierQuoteString;
//			String catalogName = quoteNames ? q+ti._tabCat+q    : fixStrangeNames(ti._tabCat);
//			String schemaName  = quoteNames ? q+ti._tabSchema+q : fixStrangeNames(ti._tabSchema);
//			String tableName   = quoteNames ? q+ti._tabName+q   : fixStrangeNames(ti._tabName);
//
//			// If the schemaname/owner is 'dbo', do not prefix it with 'dbo.'
////			if ("dbo".equalsIgnoreCase(ti._tabSchema))
////			{
////				schemaName = "";
////				addSchema = addCatalog; // if catalog is true, we need to add a simple '.'
////			}
//
//			String out = "";
//			if (addCatalog) out += catalogName + ".";
//			if (addSchema)  out += schemaName  + ".";
//			out += tableName;
//			
//			return out;
//		}
//		public SqlTableCompletion(CompletionProviderAbstractSql provider, TableInfo ti, boolean addCatalog, boolean addSchema, boolean quoteNames)
//		{
//			super(provider, ti._tabName, createReplacementText(ti, addCatalog, addSchema, quoteNames));
//			_tableInfo = ti;
//
//			String shortDesc = 
//				"<font color='blue'>"+ti._tabType+"</font>" +
////				" -- <i><font color='green'>" + (StringUtil.isNullOrBlank(ti._tabRemark) ? "No Description" : ti._tabRemark) + "</font></i>";
//				" -- <i><font color='green'>" + (StringUtil.isNullOrBlank(ti._tabRemark) ? "" : ti._tabRemark) + "</font></i>";
//			setShortDescription(shortDesc);
////			setSummary(_tableInfo.toHtmlString());
//		}
//		
//		public String getType()
//		{
//			if (_tableInfo          == null) return "";
//			if (_tableInfo._tabType == null) return "";
//			return _tableInfo._tabType;
//		}
//		public String getName()
//		{
//			if (_tableInfo          == null) return "";
//			if (_tableInfo._tabName == null) return "";
//			return _tableInfo._tabName;
//		}
//
//		@Override
//		public String getSummary()
//		{
//			if ( ! _tableInfo.isColumnRefreshed() )
//			{
//				CompletionProvider cp = getProvider();
//				if (cp instanceof CompletionProviderAbstract)
//				_tableInfo.refreshColumnInfo(((CompletionProviderAbstract)cp)._connectionProvider);
//			}
//			return _tableInfo.toHtmlString();
//		}
//
//		/**
//		 * Make it HTML aware
//		 */
//		@Override
//		public String toString()
//		{
//			return "<html><body>" + super.toString() + "</body></html>";
//		}
//	}

//    /**
//	 * Our own Completion class, which overrides toString() to make it HTML aware
//	 */
//	protected static class SqlProcedureCompletion
//	extends SqlCompletion
//	{
//		private static final long serialVersionUID = 1L;
//
//		private ProcedureInfo _procInfo = null;
//		
//		public static String createReplacementText(ProcedureInfo pi, boolean addCatalog, String catName, boolean addSchema, boolean quoteNames)
//		{
//			String tmpCatalogName = pi._procCat;
//			if (catName != null)
//			{
//				tmpCatalogName = catName;
//				addCatalog = true;
//			}
//				
//			String q = _dbIdentifierQuoteString;
//
//			String catalogName = quoteNames ? q+tmpCatalogName+q : fixStrangeNames(tmpCatalogName);
//			String schemaName  = quoteNames ? q+pi._procSchema+q : fixStrangeNames(pi._procSchema);
//			String tableName   = quoteNames ? q+pi._procName+q   : fixStrangeNames(pi._procName);
//
//			// If the schemaname/owner is 'dbo', do not prefix it with 'dbo.'
////			if ("dbo".equalsIgnoreCase(pi._procSchema))
////			{
////				schemaName = "";
////				addSchema = addCatalog; // if catalog is true, we need to add a simple '.'
////			}
//
//			String out = "";
//			if (addCatalog) out += catalogName + ".";
//			if (addSchema)  out += schemaName  + ".";
//			out += tableName;
//			
//			return out;
//		}
//
////		public SqlProcedureCompletion(CompletionProviderAbstractSql provider, ProcedureInfo pi)
//		public SqlProcedureCompletion(CompletionProviderAbstractSql provider, ProcedureInfo pi, boolean addCatalog, String catalogName, boolean addSchema, boolean quoteNames)
//		{
//			super(provider, pi._procName, createReplacementText(pi, addCatalog, catalogName, addSchema, quoteNames));
//			_procInfo = pi;
//
//			String shortDesc = 
//				"<font color='blue'>"+pi._procType+"</font>" +
//				(StringUtil.isNullOrBlank(pi._procSpecificName) ? "" : ", SpecificName="+pi._procSpecificName) +
////				" -- <i><font color='green'>" + (StringUtil.isNullOrBlank(pi._procRemark) ? "No Description" : pi._procRemark) + "</font></i>";
//				" -- <i><font color='green'>" + (StringUtil.isNullOrBlank(pi._procRemark) ? "" : pi._procRemark) + "</font></i>";
//			setShortDescription(shortDesc);
//			//setSummary(_procInfo.toHtmlString());
//		}
//
//		public String getType()
//		{
//			if (_procInfo           == null) return "";
//			if (_procInfo._procType == null) return "";
//			return _procInfo._procType;
//		}
//		public String getName()
//		{
//			if (_procInfo           == null) return "";
//			if (_procInfo._procName == null) return "";
//			return _procInfo._procName;
//		}
//		public String getRemark()
//		{
//			if (_procInfo             == null) return "";
//			if (_procInfo._procRemark == null) return "";
//			return _procInfo._procRemark;
//		}
//
//		@Override
//		public String getSummary()
//		{
//			if ( ! _procInfo.isParamsRefreshed() )
//			{
//				CompletionProvider cp = getProvider();
//				if (cp instanceof CompletionProviderAbstract)
//					_procInfo.refreshParameterInfo(((CompletionProviderAbstract)cp)._connectionProvider);
//			}
//			return _procInfo.toHtmlString();
//		}
//
//		/**
//		 * Make it HTML aware
//		 */
//		@Override
//		public String toString()
//		{
//			return "<html><body>" + super.toString() + "</body></html>";
//		}
//	}

//    /**
//	 * Our own Completion class, which overrides toString() to make it HTML aware
//	 */
//	protected static class SqlColumnCompletion
//	extends SqlCompletion
//	{
//		private static final long serialVersionUID = 1L;
//
//		private TableInfo _tableInfo = null;
//		
//		public SqlColumnCompletion(CompletionProvider provider, String tabAliasName, String colname, TableInfo tableInfo)
//		{
//			super(provider, fixStrangeNames(colname), (tabAliasName == null ? colname : tabAliasName+"."+colname));
//			_tableInfo = tableInfo;
//
//			TableColumnInfo ci = _tableInfo.getColumnInfo(colname);
//			String colPos = "";
//			if (ci != null)
//				colPos = "pos="+ci._colPos+", ";
//
//			String shortDesc = 
//				"<font color='blue'>"+_tableInfo.getColDdlDesc(colname)+"</font>" +
//				" -- <i><font color='green'>" + colPos + _tableInfo.getColDescription(colname) + "</font></i>";
//			setShortDescription(shortDesc);
//			//setSummary(_tableInfo.toHtmlString(colname));
//		}
//
//		@Override
//		public String getSummary()
//		{
//			if ( ! _tableInfo.isColumnRefreshed() )
//			{
//				CompletionProvider cp = getProvider();
//				if (cp instanceof CompletionProviderAbstract)
//					_tableInfo.refreshColumnInfo(((CompletionProviderAbstract)cp)._connectionProvider);
//			}
//			return _tableInfo.toHtmlString();
//		}
//
//		/**
//		 * Make it HTML aware
//		 */
//		@Override
//		public String toString()
//		{
//			return "<html><body>" + super.toString() + "</body></html>";
//		}
//	}


	
	
	
	

	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	// INFO classes/structures
	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	
	
	
	protected DbInfo getDbInfo(String dbName)
	{
		for (DbInfo di : _dbInfoList)
		{
			if (dbName.equalsIgnoreCase(di._dbName))
				return di;
		}
		return null;
	}

//	/**
//	 * Holds information about databases
//	 */
//	protected static class DbInfo
//	implements Serializable
//	{
//		private static final long serialVersionUID = 1L;
//
//		public String _dbName    = null;
//		public String _dbSize    = null;
//		public int    _dbId      = -1;
//		public String _dbOwner   = null;
//		public String _dbCrDate  = null;
//		public String _dbType    = null;
//		public String _dbRemark  = null;
//		
//		@Override
//		public String toString()
//		{
//			return super.toString() + ": name='"+_dbName+"', size='"+_dbSize+"', id='"+_dbId+"', owner='"+_dbOwner+"', crdate='"+_dbCrDate+"', type='"+_dbType+"', remark='"+_dbRemark+"'";
//		}
//
//		public String toHtmlString()
//		{
//			StringBuilder sb = new StringBuilder();
////			sb.append(_tabType).append(" - <B>").append(_tabName).append("</B>");
//			sb.append("<B>").append(_dbName).append("</B> - <font color='blue'>").append(_dbType).append("</font>");
//			sb.append("<HR>");
//			sb.append("<BR>");
//			sb.append("<B>Description:</B> ").append(StringUtil.isNullOrBlank(_dbRemark) ? "not available" : _dbRemark).append("<BR>");
//			sb.append("<B>Size:</B> ")       .append(StringUtil.isNullOrBlank(_dbSize)   ? "not available" : _dbSize)  .append("<BR>");
//			sb.append("<B>Owner:</B> ")      .append(StringUtil.isNullOrBlank(_dbOwner)  ? "not available" : _dbOwner) .append("<BR>");
//			sb.append("<B>Create Date:</B> ").append(StringUtil.isNullOrBlank(_dbCrDate) ? "not available" : _dbCrDate).append("<BR>");
//			sb.append("<B>dbid:</B> ")       .append(_dbId == -1                         ? "not available" : _dbId)    .append("<BR>");
//			sb.append("<BR>");
//			
//			return sb.toString();
//		}
//	}

//	protected SchemaInfo getSchemaInfo(String schemaName)
//	{
//		for (SchemaInfo si : _schemaInfoList)
//		{
//			if (schemaName.equalsIgnoreCase(si._name))
//				return si;
//		}
//		return null;
//	}

//	/**
//	 * Holds information about databases
//	 */
//	protected static class SchemaInfo
//	implements Serializable
//	{
//		private static final long serialVersionUID = 1L;
//
//		public String _cat  = null;
//		public String _name = null;
//		public String _remark = null;
//
//		@Override
//		public String toString()
//		{
//			return super.toString() + ": name='"+_name+"', catalog='"+_cat+"'";
//		}
//
//		public String toHtmlString()
//		{
//			StringBuilder sb = new StringBuilder();
////			sb.append(_tabType).append(" - <B>").append(_tabName).append("</B>");
//			sb.append("<B>").append(_name).append("</B> - <font color='blue'>").append(_cat).append("</font>");
//			sb.append("<HR>");
//			sb.append("<BR>");
//			sb.append("<B>Description:</B> ").append(StringUtil.isNullOrBlank(_remark) ? "not available" : _remark).append("<BR>");
//			sb.append("<BR>");
//			
//			return sb.toString();
//		}
//	}

//	/**
//	 * Holds information about columns
//	 */
//	protected static class TableColumnInfo
//	implements Serializable
//	{
//		private static final long serialVersionUID = 1L;
//
//		public String _colName       = null;
//		public int    _colPos        = -1;
//		public String _colType       = null;
////		public String _colType2      = null;
//		public int    _colLength     = -1;
//		public int    _colIsNullable = -1;
//		public String _colRemark     = null;
//		public String _colDefault    = null;
////		public int    _colPrec       = -1;
//		public int    _colScale      = -1;
//	}

	protected TableInfo getTableInfo(String tableName)
	{
		return getTableInfo(null, null, tableName, false);
	}
	protected TableInfo getTableInfo(String catName, String schemaName, String tableName, boolean getColInfo)
	{
		for (TableInfo ti : _tableInfoList)
		{
			if (tableName.equalsIgnoreCase(ti._tabName))
			{
				if (StringUtil.hasValue(catName) && ! catName.equalsIgnoreCase(ti._tabCat))
					continue;
				if (StringUtil.hasValue(schemaName) && ! schemaName.equalsIgnoreCase(ti._tabSchema))
					continue;
					
//				if (getColInfo && ! ti.isColumnRefreshed())
//					ti.refreshColumnInfo(_connectionProvider);
				if (getColInfo && ! ti.isColumnRefreshed())
					ti.refreshColumnInfo(getConnection());
					
				return ti;
			}
		}
		return null;
	}

	
	
	
	
	protected FunctionInfo getFunctionInfo(String functionName)
	{
		return getFunctionInfo(null, null, functionName, false);
	}
	protected FunctionInfo getFunctionInfo(String catName, String schemaName, String functionName, boolean getColInfo)
	{
		for (FunctionInfo fi : _functionInfoList)
		{
			if (functionName.equalsIgnoreCase(fi._funcName))
			{
				if (StringUtil.hasValue(catName) && ! catName.equalsIgnoreCase(fi._funcCat))
					continue;
				if (StringUtil.hasValue(schemaName) && ! schemaName.equalsIgnoreCase(fi._funcSchema))
					continue;
					
				if (getColInfo && ! fi.isColumnRefreshed())
					fi.refreshColumnInfo(_connectionProvider);
					
				return fi;
			}
		}
		return null;
	}


//	/**
//	 * Holds information about tables
//	 */
//	protected static class TableInfo
//	implements Serializable
//	{
//		private static final long serialVersionUID = 1L;
//
//		public String _tabCat     = null;
//		public String _tabSchema  = null;
//		public String _tabName    = null;
//		public String _tabType    = null;
//		public String _tabRemark  = null;
//		
//		public boolean _needColumnRefresh = true;
//		public boolean isColumnRefreshed() {return ! _needColumnRefresh;}
//
//		public ArrayList<TableColumnInfo> _columns = new ArrayList<TableColumnInfo>();
//
//		public void addColumn(TableColumnInfo ci)
//		{
//			// If column name already exists, do NOT add it again
//			for (TableColumnInfo existingCi : _columns)
//			{
//				if (existingCi._colName.equals(ci._colName))
//				{
//					//(new Exception("callstack for: addColumn("+ci._colName+") already exists.")).printStackTrace();
//					return;
//				}
//			}
//
//			_columns.add(ci);
//		}
//
//		public void refreshColumnInfo(ConnectionProvider connProvider)
//		{
//			try
//			{
//				final Connection conn = connProvider.getConnection();
//				if (conn == null)
//					return;
//
//				DatabaseMetaData dbmd = conn.getMetaData();
//
//				ResultSet rs = dbmd.getColumns(_tabCat, _tabSchema, _tabName, "%");
//				while(rs.next())
//				{
//					TableColumnInfo ci = new TableColumnInfo();
//					ci._colName       = StringUtils.trim(rs.getString("COLUMN_NAME"));
//					ci._colPos        =                  rs.getInt   ("ORDINAL_POSITION");
//					ci._colType       = StringUtils.trim(rs.getString("TYPE_NAME"));
//					ci._colLength     =                  rs.getInt   ("COLUMN_SIZE");
//					ci._colIsNullable =                  rs.getInt   ("NULLABLE");
//					ci._colRemark     = StringUtils.trim(rs.getString("REMARKS"));
//					ci._colDefault    = StringUtils.trim(rs.getString("COLUMN_DEF"));
//					ci._colScale      =                  rs.getInt   ("DECIMAL_DIGITS");
//					
//					addColumn(ci);
//				}
//				rs.close();
//
//				_needColumnRefresh = false;
//			}
//			catch (SQLException e)
//			{
//				_logger.warn("Problems looking up Column MetaData for table '"+_tabName+"'. Caught: "+e);
//			}
//		}
//
//		@Override
//		public String toString()
//		{
//			return super.toString() + ": cat='"+_tabCat+"', schema='"+_tabSchema+"', name='"+_tabName+"', type='"+_tabType+"', remark='"+_tabRemark+"'";
//		}
//		public String toHtmlString()
//		{
//			StringBuilder sb = new StringBuilder();
////			sb.append(_tabType).append(" - <B>").append(_tabName).append("</B>");
//			sb.append(_tabSchema).append(".<B>").append(_tabName).append("</B> - <font color='blue'>").append(_tabType).append("</font>");
//			sb.append("<HR>");
//			sb.append("<BR>");
//			sb.append("<B>Description:</B> ").append(StringUtil.isNullOrBlank(_tabRemark) ? "not available" : _tabRemark).append("<BR>");
//			sb.append("<BR>");
//			sb.append("<B>Columns:</B> ").append("<BR>");
//			sb.append("<TABLE ALIGN=\"left\" BORDER=0 CELLSPACING=0 CELLPADDING=1\">");
//			sb.append("<TR ALIGN=\"left\" VALIGN=\"top\" BGCOLOR=\"#ffffff\">");
//			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Name")       .append("</B></FONT></TD>");
//			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Datatype")   .append("</B></FONT></TD>");
//			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Length")     .append("</B></FONT></TD>");
//			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Nulls")      .append("</B></FONT></TD>");
//			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Pos")        .append("</B></FONT></TD>");
//			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Description").append("</B></FONT></TD>");
//			sb.append("</TR>");
//			int r=0;
//			for (TableColumnInfo ci : _columns)
//			{
//				r++;
//				if ( (r % 2) == 0 )
//					sb.append("<TR ALIGN=\"left\" VALIGN=\"top\" BGCOLOR=\"#ffffff\">");
//				else
//					sb.append("<TR ALIGN=\"left\" VALIGN=\"top\" BGCOLOR=\"#ffffcc\">");
//				sb.append("	<TD NOWRAP>").append(ci._colName)      .append("</TD>");
//				sb.append("	<TD NOWRAP>").append(ci._colType)      .append("</TD>");
//				sb.append("	<TD NOWRAP>").append(ci._colLength)    .append("</TD>");
//				sb.append("	<TD NOWRAP>").append(ci._colIsNullable).append("</TD>");
//				sb.append("	<TD NOWRAP>").append(ci._colPos)       .append("</TD>");
//				sb.append("	<TD NOWRAP>").append(ci._colRemark != null ? ci._colRemark : "not available").append("</TD>");
//				sb.append("</TR>");
//			}
//			sb.append("</TABLE>");
//			sb.append("<HR>");
//			sb.append("-end-<BR><BR>");
//			
//			return sb.toString();
//		}
//
//		public String toHtmlString(String colname)
//		{
//			TableColumnInfo ci = null;
//			for (TableColumnInfo e : _columns)
//			{
//				if (colname.equalsIgnoreCase(e._colName))
//				{
//					ci = e;
//					break;
//				}
//			}
//			if (ci == null)
//				return "Column name '"+colname+"', was not found in table '"+_tabName+"'.";
//
//			StringBuilder sb = new StringBuilder();
//			sb.append(_tabSchema).append(".<B>").append(_tabName).append(".").append(ci._colName).append("</B> - <font color='blue'>").append(_tabType).append(" - COLUMN").append("</font>");
//			sb.append("<HR>"); // add Horizontal Ruler: ------------------
//			sb.append("<BR>");
//			sb.append("<B>Table Description:</B> ").append(StringUtil.isNullOrBlank(_tabRemark) ? "not available" : _tabRemark).append("<BR>");
//			sb.append("<BR>");
//			sb.append("<B>Column Description:</B> ").append(StringUtil.isNullOrBlank(ci._colRemark) ? "not available" : ci._colRemark).append("<BR>");
//			sb.append("<BR>");
//			sb.append("<B>Name:</B> ")       .append(ci._colName)      .append("<BR>");
//			sb.append("<B>Type:</B> ")       .append(ci._colType)      .append("<BR>");
//			sb.append("<B>Length:</B> ")     .append(ci._colLength)    .append("<BR>");
//			sb.append("<B>Is Nullable:</B> ").append(ci._colIsNullable).append("<BR>");
//			sb.append("<B>Pos:</B> ")        .append(ci._colPos)       .append("<BR>");
//			sb.append("<B>Default:</B> ")    .append(ci._colDefault)   .append("<BR>");
//			sb.append("<HR>");
//			sb.append("-end-<BR><BR>");
//			
//			return sb.toString();
//		}
//
//		public TableColumnInfo getColumnInfo(String colname)
//		{
//			TableColumnInfo ci = null;
//			for (TableColumnInfo e : _columns)
//			{
//				if (colname.equalsIgnoreCase(e._colName))
//				{
//					ci = e;
//					break;
//				}
//			}
//			return ci;
//		}
//
//		public String getColDdlDesc(String colname)
//		{
//			TableColumnInfo ci = getColumnInfo(colname);
//			if (ci == null)
//				return "Column name '"+colname+"', was not found in table '"+_tabName+"'.";
//
//			String nulls    = ci._colIsNullable == DatabaseMetaData.columnNoNulls ? "<b>NOT</b> NULL" : "    NULL";
//			String datatype = ci._colType;
//
//			// Compose data type
//			String dtlower = datatype.toLowerCase();
//			if ( dtlower.equals("char") || dtlower.equals("varchar") )
//				datatype = datatype + "(" + ci._colLength + ")";
//			
//			if ( dtlower.equals("numeric") || dtlower.equals("decimal") )
//				datatype = datatype + "(" + ci._colLength + "," + ci._colScale + ")";
//
//			return datatype + " " + nulls;
//		}
//
//		public String getColDescription(String colname)
//		{
//			TableColumnInfo ci = getColumnInfo(colname);
//			if (ci == null)
//				return "Column name '"+colname+"', was not found in table '"+_tabName+"'.";
//
//			if (StringUtil.isNullOrBlank(ci._colRemark))
////				return "No Description";
//				return "";
//			return ci._colRemark;
//		}
//	}

//	/**
//	 * Holds information about parameters
//	 */
//	protected static class ProcedureParameterInfo
//	implements Serializable
//	{
//		private static final long serialVersionUID = 1L;
//
//		public String _paramName       = null;
//		public int    _paramPos        = -1;
//		public String _paramInOutType  = null;
//		public String _paramType       = null;
////		public String _paramType2      = null;
//		public int    _paramLength     = -1;
//		public int    _paramIsNullable = -1;
//		public String _paramRemark     = null;
//		public String _paramDefault    = null;
////		public int    _paramPrec       = -1;
//		public int    _paramScale      = -1;
//	}

	protected ProcedureInfo getProcedureInfo(String procName)
	{
		return getProcedureInfo(null, null, procName, false);
	}
	protected ProcedureInfo getProcedureInfo(String catName, String schemaName, String procName, boolean getParamInfo)
	{
		for (ProcedureInfo pi : _procedureInfoList)
		{
			if (procName.equalsIgnoreCase(pi._procName))
			{
				if (StringUtil.hasValue(catName) && ! catName.equalsIgnoreCase(pi._procCat))
					continue;
				if (StringUtil.hasValue(schemaName) && ! schemaName.equalsIgnoreCase(pi._procSchema))
					continue;
					
				if (getParamInfo && ! pi.isParamsRefreshed())
					pi.refreshParameterInfo(_connectionProvider);
					
				return pi;
			}
		}
		return null;
	}

	protected ProcedureInfo getSystemProcedureInfo(String procName)
	{
		return getSystemProcedureInfo(null, null, procName, false);
	}
	protected ProcedureInfo getSystemProcedureInfo(String catName, String schemaName, String procName, boolean getParamInfo)
	{
		for (ProcedureInfo pi : _systemProcInfoList)
		{
			if (procName.equalsIgnoreCase(pi._procName))
			{
				if (StringUtil.hasValue(catName) && ! catName.equalsIgnoreCase(pi._procCat))
					continue;
				if (StringUtil.hasValue(schemaName) && ! schemaName.equalsIgnoreCase(pi._procSchema))
					continue;

				if (getParamInfo && ! pi.isParamsRefreshed())
					pi.refreshParameterInfo(_connectionProvider);
					
				return pi;
			}
		}
		return null;
	}



	/**
	 * Creates a CompletionProvider based on what product name the Connection is connected to, 
	 *    using <code>conn.getMetaData().getDatabaseProductName()</code><br>
	 * If we have problems getting the DatabaseProduct return <code>CompletionProviderJdbc</code>
	 * @param conn
	 * @param textPane
	 * @param window
	 * @param connProvider
	 * @return CompletionProviderAbstract
	 */
	public static CompletionProviderAbstract installAutoCompletion(Connection conn, TextEditorPane textPane, RTextScrollPane scroll, ErrorStrip errorStrip, Window window, ConnectionProvider connProvider)
	{
		CompletionProviderAbstract provider = null;

		if (conn == null)
		{
			_logger.info("Installing Completion Provider for JDBC");
			provider = CompletionProviderJdbc.installAutoCompletion(textPane, scroll, errorStrip, window, connProvider);
		}
		else
		{
			try
			{
				DatabaseMetaData md = conn.getMetaData();
				String prodName = md.getDatabaseProductName();
				
				if (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(prodName))
				{
					_logger.info("Installing Completion Provider for Sybase ASE");
					provider = CompletionProviderAse.installAutoCompletion(textPane, scroll, errorStrip, window, connProvider);
				}
				else if (DbUtils.DB_PROD_NAME_SYBASE_RS.equals(prodName))
				{
					_logger.info("Installing Completion Provider for Sybase Replication Server");
					provider = CompletionProviderRepServer.installAutoCompletion(textPane, scroll, errorStrip, window, connProvider);
				}
				else
				{
					_logger.info("Installing Completion Provider for JDBC");
					provider = CompletionProviderJdbc.installAutoCompletion(textPane, scroll, errorStrip, window, connProvider);
				}
			}
			catch (SQLException e)
			{
				_logger.info("Installing Completion Provider for JDBC, problems getting Database Product Name. Caught: "+e);
				provider = CompletionProviderJdbc.installAutoCompletion(textPane, scroll, errorStrip, window, connProvider);
			}
		}

		return provider;
	}

	/**
	 * Called from any ToolTipProvider to check if word can be found in the CompletionProviders dictionary
	 */
	@Override
	public String getToolTipTextForObject(String word, String fullWord)
	{
		if (StringUtil.isNullOrBlank(word))
			return null;

		if (needRefresh())
			refresh();

		SqlObjectName sqlObj = new SqlObjectName(word, _dbProductName, _dbIdentifierQuoteString, _dbStoresUpperCaseIdentifiers, _dbSupportsSchema);

		// For completion, lets not assume "dbo"
		String schemaName = sqlObj.getSchemaName();
		if ("dbo".equals(schemaName))
			schemaName = "";

		DbInfo        dbInfo    = getDbInfo(             sqlObj.getObjectName());
		TableInfo     tabInfo   = getTableInfo(          sqlObj.getCatalogName(), schemaName, sqlObj.getObjectName(), true);
		FunctionInfo  funcInfo  = getFunctionInfo(       sqlObj.getCatalogName(), schemaName, sqlObj.getObjectName(), true);
		ProcedureInfo procInfo  = getProcedureInfo(      sqlObj.getCatalogName(), schemaName, sqlObj.getObjectName(), true);
		ProcedureInfo sProcInfo = getSystemProcedureInfo(sqlObj.getCatalogName(), schemaName, sqlObj.getObjectName(), true);

//System.out.println("dbInfo="+dbInfo);
//System.out.println("tabInfo="+tabInfo);
//System.out.println("procInfo="+procInfo);
//System.out.println("sProcInfo="+sProcInfo);

		StringBuilder sb = new StringBuilder();
		if (dbInfo    != null) sb.append(dbInfo   .toHtmlString());
		if (tabInfo   != null) sb.append(tabInfo  .toHtmlString());
		if (funcInfo  != null) sb.append(funcInfo .toHtmlString());
		if (procInfo  != null) sb.append(procInfo .toHtmlString());
		if (sProcInfo != null) sb.append(sProcInfo.toHtmlString());
		if (sb.length() != 0)
		{
			sb.insert(0, "<html>");
			sb.append("</html>");
			return sb.toString();
		}
		
		return null;
//		return "<html>DUMMY getToolTipTextForObject(word='<b>"+word+"</b>', fullWord='<b>"+fullWord+"</b>'): sqlObj.getObjectName()='"+sqlObj.getObjectName()+"'</html>";
	}

	/**
	 * Cell renderer for SQL Completions
	 * 
	 * @return
	 */
	@Override
	public ListCellRenderer createDefaultCompletionCellRenderer()
	{
		return new SqlCellRenderer();
	}



	public enum GenerateSqlType
	{
		SELECT, SELECT_EXEC, SELECT_EXEC_TOP, INSERT, UPDATE, DELETE
	};

	public enum GenerateJavaType
	{
		SQL_STRING, SQL_STRINGBUILDER
	};

	@Override
	public List<JMenu> createEditorPopupMenuExtention(RTextArea textarea)
	{
		List<JMenu> list = new ArrayList<>();
		
		list.add(createGenerateSqlMenu());
		list.add(createGenerateJavaMenu());
		
		return list;
	}

	/*----------------------------------------------------------------------
	**----------------------------------------------------------------------
	** BEGIN: Generate SQL Menu
	**----------------------------------------------------------------------
	**----------------------------------------------------------------------*/ 
	public JMenu createGenerateSqlMenu()
	{
		JMenu top = new JMenu("Generate SQL, for selected text");

		JMenu select = new JMenu("Select");
		JMenu insert = new JMenu("Insert");
		JMenu update = new JMenu("Update");
		JMenu delete = new JMenu("Delete");
		JMenu ddlGen = new JMenu("DDL");

		select.add(new JMenuItem( new SqlGenerator("To Clipboard",                CmdOutputType.TO_CLIPBOARD, GenerateSqlType.SELECT)          ));
		select.add(new JMenuItem( new SqlGenerator("To Editors Current Location", CmdOutputType.TO_EDITOR,    GenerateSqlType.SELECT)          ));
		select.add(new JMenuItem( new SqlGenerator("To Separate Window",          CmdOutputType.TO_WINDOW,    GenerateSqlType.SELECT)          ));
		select.add(new JMenuItem( new SqlGenerator("Execute 'select ...'",        CmdOutputType.EXECUTE,      GenerateSqlType.SELECT_EXEC)     ));
		select.add(new JMenuItem( new SqlGenerator("Execute 'select top 1000 ...",CmdOutputType.EXECUTE,      GenerateSqlType.SELECT_EXEC_TOP) ));

		insert.add(new JMenuItem( new SqlGenerator("To Clipboard",                CmdOutputType.TO_CLIPBOARD, GenerateSqlType.INSERT)          ));
		insert.add(new JMenuItem( new SqlGenerator("To Editors Current Location", CmdOutputType.TO_EDITOR,    GenerateSqlType.INSERT)          ));
		insert.add(new JMenuItem( new SqlGenerator("To Separate Window",          CmdOutputType.TO_WINDOW,    GenerateSqlType.INSERT)          ));

		update.add(new JMenuItem( new SqlGenerator("To Clipboard",                CmdOutputType.TO_CLIPBOARD, GenerateSqlType.UPDATE)          ));
		update.add(new JMenuItem( new SqlGenerator("To Editors Current Location", CmdOutputType.TO_EDITOR,    GenerateSqlType.UPDATE)          ));
		update.add(new JMenuItem( new SqlGenerator("To Separate Window",          CmdOutputType.TO_WINDOW,    GenerateSqlType.UPDATE)          ));

		delete.add(new JMenuItem( new SqlGenerator("To Clipboard",                CmdOutputType.TO_CLIPBOARD, GenerateSqlType.DELETE)          ));
		delete.add(new JMenuItem( new SqlGenerator("To Editors Current Location", CmdOutputType.TO_EDITOR,    GenerateSqlType.DELETE)          ));
		delete.add(new JMenuItem( new SqlGenerator("To Separate Window",          CmdOutputType.TO_WINDOW,    GenerateSqlType.DELETE)          ));

		ddlGen.add(new JMenuItem( new DdlOutput   ("To Clipboard",                DdlOutputType.TO_CLIPBOARD) ));
		ddlGen.add(new JMenuItem( new DdlOutput   ("To Editors Current Location", DdlOutputType.TO_EDITOR)    ));
		ddlGen.add(new JMenuItem( new DdlOutput   ("To Separate Window",          DdlOutputType.TO_WINDOW)    ));
		
		top.add(select);
		top.add(insert);
		top.add(update);
		top.add(delete);
		top.add(ddlGen);
		
		return top;
	}

	/**
	 * 
	 */
	public String getSqlFor(String word, GenerateSqlType type)
	{
		if (StringUtil.isNullOrBlank(word))
			return null;

		if (needRefresh())
			refresh();

		SqlObjectName sqlObj = new SqlObjectName(word, _dbProductName, _dbIdentifierQuoteString, _dbStoresUpperCaseIdentifiers, _dbSupportsSchema);

		// For completion, lets not assume "dbo"
		String schemaName = sqlObj.getSchemaName();
		if ("dbo".equals(schemaName))
			schemaName = "";

//		DbInfo        dbInfo    = getDbInfo(             sqlObj.getObjectName());
		TableInfo     tabInfo   = getTableInfo(          sqlObj.getCatalogName(), schemaName, sqlObj.getObjectName(), true);
//		FunctionInfo  funcInfo  = getFunctionInfo(       sqlObj.getCatalogName(), schemaName, sqlObj.getObjectName(), true);
//		ProcedureInfo procInfo  = getProcedureInfo(      sqlObj.getCatalogName(), schemaName, sqlObj.getObjectName(), true);
//		ProcedureInfo sProcInfo = getSystemProcedureInfo(sqlObj.getCatalogName(), schemaName, sqlObj.getObjectName(), true);

//System.out.println("dbInfo="+dbInfo);
//System.out.println("tabInfo="+tabInfo);
//System.out.println("procInfo="+procInfo);
//System.out.println("sProcInfo="+sProcInfo);

		StringBuilder sb = new StringBuilder();
//		if (dbInfo    != null) sb.append(dbInfo   .toSelect());
//		if (tabInfo   != null) sb.append(tabInfo  .toSelect());
//		if (funcInfo  != null) sb.append(funcInfo .toSelect());
//		if (procInfo  != null) sb.append(procInfo .toSelect());
//		if (sProcInfo != null) sb.append(sProcInfo.toSelect());

		if (tabInfo != null)
		{
			if      (GenerateSqlType.SELECT         .equals(type)) { sb.append(tabInfo.toSelect()); }
			else if (GenerateSqlType.SELECT_EXEC    .equals(type)) { sb.append(tabInfo.toSelect(true, -1)); }
			else if (GenerateSqlType.SELECT_EXEC_TOP.equals(type)) { sb.append(tabInfo.toSelect(true, 1000)); }
			else if (GenerateSqlType.INSERT         .equals(type)) { sb.append(tabInfo.toInsert()); }
			else if (GenerateSqlType.UPDATE         .equals(type)) { sb.append(tabInfo.toUpdate()); }
			else if (GenerateSqlType.DELETE         .equals(type)) { sb.append(tabInfo.toDelete()); }
		}
		else
		{
			return "-- Sorry: table '"+word+"' was not found in the dictionary.";
		}

//System.out.println("getSqlFor(word='"+word+"', type='"+type+"') tabInfo="+tabInfo+", sb.length()="+sb.length()+", sqlObj"+sqlObj);

		if (sb.length() != 0)
		{
			return sb.toString();
		}
		
		return null;
	}

	/**
	 * Helper method to get the current text to lookup. It can be the text we are currently standing at, or the selected text
	 * @param textComp
	 * @return
	 */
	private String getLookupText(JTextComponent textComp)
	{
		String word;
		String fullWord;
		if (textComp instanceof RTextArea)
		{
			RTextArea rta = (RTextArea) textComp;
			int dot = textComp.getCaretPosition();

			word     = RSyntaxUtilitiesX.getCurrentWord(rta, dot, getCharsAllowedInWordCompletion());
			fullWord = RSyntaxUtilitiesX.getCurrentFullWord(rta, dot);

			String selectedText = textComp.getSelectedText();
			if (selectedText != null)
			{
				word     = selectedText;
				fullWord = selectedText;
			}
		}
		else
		{
			String selectedText = textComp.getSelectedText();
			word     = selectedText;
			fullWord = selectedText;
		}
		
		if (StringUtil.isNullOrBlank(fullWord))
		{
			SwingUtils.showInfoMessage(getGuiOwner(), "Nothing selected", "No text is selected, so nothing to lookup...");
			return null;
		}
		return fullWord;
	}

	//-----------------------------------------------------------------------------
	// SQL GENERATOR
	//-----------------------------------------------------------------------------
	private enum CmdOutputType {TO_CLIPBOARD, TO_EDITOR, TO_WINDOW, EXECUTE};

	private class SqlGenerator extends TextAction
	{
		private static final long serialVersionUID = 1L;
		private GenerateSqlType _generateType;
		private CmdOutputType   _outputType;

		public SqlGenerator(String name, CmdOutputType outputType, GenerateSqlType generateType)
		{
			super(name);
			_outputType   = outputType;
			_generateType = generateType;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			String sql = getSqlFor(getLookupText(getTextComponent(e)), _generateType);

			if (StringUtil.hasValue(sql))
			{
				if (CmdOutputType.TO_CLIPBOARD.equals(_outputType))
				{
					SwingUtils.setClipboardContents(sql);
				}

				if (CmdOutputType.TO_EDITOR.equals(_outputType)) 
				{
					getTextComponent(e).replaceSelection(sql);
				}

				if (CmdOutputType.TO_WINDOW.equals(_outputType))
				{
					SqlTextDialog dialog = new SqlTextDialog(null, sql);
					dialog.setVisible(true);
				}

				if (CmdOutputType.EXECUTE.equals(_outputType))
				{
					ConnectionProvider connProvider = getConnectionProvider();
					if (connProvider instanceof QueryWindow)
						((QueryWindow)connProvider).displayQueryResults(sql, 0, false);
				}
			}
		}
	}

	//-----------------------------------------------------------------------------
	// DDL
	//-----------------------------------------------------------------------------
	private enum DdlOutputType {TO_CLIPBOARD, TO_EDITOR, TO_WINDOW};

	private class DdlOutput extends TextAction
	{
		private static final long serialVersionUID = 1L;
		private DdlOutputType _type;
		public DdlOutput(String name, DdlOutputType type)
		{
			super(name);
			_type = type;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			String lookupText = getLookupText(getTextComponent(e));
			if (lookupText == null)
				return; // message popup is raised in getLookupText()

			String retStr = null;
			DdlGen ddlgen = null;
			try
			{
				// Get default database
				DbxConnection conn = getConnectionProvider().getConnection();
				String dbname = conn.getCatalog();

				ddlgen = DdlGen.create(conn, false, true);
				ddlgen.setDefaultDbname(dbname);

				// Generate the DDL
				retStr = ddlgen.getDdlForType(Type.TABLE, lookupText);
			}
			catch (Throwable ex)
			{
				_logger.warn("Problems when generating DDL Statements: args="+(ddlgen==null?"null":ddlgen.getUsedCommand())+", Caught="+ex, ex);
				SwingUtils.showErrorMessage(null, "Problems generating DDL", 
						"<html>Problems when generating DDL Statements:<br>"
						+ "args="+(ddlgen==null?"null":ddlgen.getUsedCommand())+"<br>"
						+ "<br>"
						+ ex
						+ "</html>", ex);
			}
	
			
			if (StringUtil.hasValue(retStr))
			{
				if (DdlOutputType.TO_CLIPBOARD.equals(_type)) SwingUtils.setClipboardContents(retStr);
				if (DdlOutputType.TO_EDITOR   .equals(_type)) getTextComponent(e).replaceSelection(retStr);
				if (DdlOutputType.TO_WINDOW   .equals(_type))
				{
    				SqlTextDialog dialog = new SqlTextDialog(null, retStr);
    				dialog.setVisible(true);
				}
			}
		}
	}

//	//-----------------------------------------------------------------------------
//	// SELECT
//	//-----------------------------------------------------------------------------
//	private class SelectToClipboard extends TextAction
//	{
//		private static final long serialVersionUID = 1L;
//
//		public SelectToClipboard()
//		{
//			super("To Clipboard");
//		}
//
//		@Override
//		public void actionPerformed(ActionEvent e)
//		{
//			String sql = getSqlFor(getLookupText(getTextComponent(e)), GenerateSqlType.SELECT);
//
//			if (StringUtil.hasValue(sql))
//				SwingUtils.setClipboardContents(sql);
//		}
//	}

	/*----------------------------------------------------------------------
	**----------------------------------------------------------------------
	** END: Generate SQL Menu
	**----------------------------------------------------------------------
	**----------------------------------------------------------------------*/ 	




	/*----------------------------------------------------------------------
	**----------------------------------------------------------------------
	** BEGIN: Generate JAVA Menu
	**----------------------------------------------------------------------
	**----------------------------------------------------------------------*/ 
	public JMenu createGenerateJavaMenu()
	{
		JMenu top = new JMenu("Generate Java Code, for selected text");

		JMenu sqlStr = new JMenu("SQL String");
		JMenu sqlSb  = new JMenu("SQL StringBuilder");

		sqlStr.add(new JMenuItem( new JavaGenerator("To Clipboard",               CmdOutputType.TO_CLIPBOARD, GenerateJavaType.SQL_STRING)          ));
		sqlStr.add(new JMenuItem( new JavaGenerator("To Separate Window",         CmdOutputType.TO_WINDOW,    GenerateJavaType.SQL_STRING)          ));

		sqlSb.add( new JMenuItem( new JavaGenerator("To Clipboard",               CmdOutputType.TO_CLIPBOARD, GenerateJavaType.SQL_STRINGBUILDER)   ));
		sqlSb.add( new JMenuItem( new JavaGenerator("To Separate Window",         CmdOutputType.TO_WINDOW,    GenerateJavaType.SQL_STRINGBUILDER)   ));

		top.add(sqlStr);
		top.add(sqlSb);
		
		return top;
	}

	/**
	 * 
	 */
	public String getJavaFor(String inputStr, GenerateJavaType type)
	{
//System.out.println("getJavaFor: type="+type);
//System.out.println("getJavaFor: inputStr=|"+inputStr+"|");
		if (StringUtil.isNullOrBlank(inputStr))
			return null;

		// Destination
		StringBuilder sb = new StringBuilder();

		// Get a list, one entry for each row
		List<String> lines;
		try { lines = IOUtils.readLines(new StringReader(inputStr)); }
		catch(IOException e) { return null; }

		// Get max width
		int maxStrLen = 0;
		for (String line : lines)
			maxStrLen = Math.max(maxStrLen, line.length());
		
		char qc = '"';

		
		if (GenerateJavaType.SQL_STRING.equals(type)) 
		{
			sb.append("String sql = ").append(qc).append(qc).append("\n");
			for (String str : lines)
			{
				String toPrint = StringUtil.rtrim2(str).replace("\"", "\\\"");
				sb.append("    + ").append(qc).append(toPrint).append(" \\n").append(qc).append("\n");
			}
			sb.append("    + ").append(qc).append(qc).append(";\n");
		}
		else if (GenerateJavaType.SQL_STRINGBUILDER.equals(type))
		{
			sb.append("StringBuilder sb = new StringBuilder();\n");
			for (String str : lines)
			{
				String toPrint = StringUtil.rtrim2(str).replace("\"", "\\\"");
				sb.append("sb.append(").append(qc).append(toPrint).append(" \\n").append(qc).append("); \n");
			}
			sb.append("String sql = sb.toString();\n"); 
		}

		if (sb.length() != 0)
		{
			return sb.toString();
		}
		
		return null;
	}

	//-----------------------------------------------------------------------------
	// JAVA GENERATOR
	//-----------------------------------------------------------------------------
//	private enum CmdOutputType {TO_CLIPBOARD, TO_EDITOR, TO_WINDOW, EXECUTE};

	private class JavaGenerator extends TextAction
	{
		private static final long serialVersionUID = 1L;
		private GenerateJavaType _generateType;
		private CmdOutputType   _outputType;

		public JavaGenerator(String name, CmdOutputType outputType, GenerateJavaType generateType)
		{
			super(name);
			_outputType   = outputType;
			_generateType = generateType;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			String text = getJavaFor(getLookupText(getTextComponent(e)), _generateType);

			if (StringUtil.hasValue(text))
			{
				if (CmdOutputType.TO_CLIPBOARD.equals(_outputType))
				{
					SwingUtils.setClipboardContents(text);
				}

				if (CmdOutputType.TO_EDITOR.equals(_outputType)) 
				{
					getTextComponent(e).replaceSelection(text);
				}

				if (CmdOutputType.TO_WINDOW.equals(_outputType))
				{
					SqlTextDialog dialog = new SqlTextDialog(null, text);
					dialog.setVisible(true);
				}
			}
		}
	}

	/*----------------------------------------------------------------------
	** END: Generate SQL Menu
	**----------------------------------------------------------------------*/ 	
}




//Grabbed from: "C:/Documents and Settings/gorans/My Documents/Downloads/java/RSyntaxTextArea/rsyntaxtextarea_2.0.4.1_Source/src/org/fife/ui/rsyntaxtextarea/MarkOccurrencesSupport.java"
// may be used for parsing....

///**
// * Called after the caret has been moved and a fixed time delay has
// * elapsed.  This locates and highlights all occurrences of the identifier
// * at the caret position, if any.
// *
// * @param e The event.
// */
//public void actionPerformed(ActionEvent e) {
//
//	// Don't do anything if they are selecting text.
//	Caret c = textArea.getCaret();
//	if (c.getDot()!=c.getMark()) {
//		return;
//	}
//
//	RSyntaxDocument doc = (RSyntaxDocument)textArea.getDocument();
//	//long time = System.currentTimeMillis();
//	doc.readLock();
//	try {
//
//		// Get the token at the caret position.
//		int line = textArea.getCaretLineNumber();
//		Token tokenList = textArea.getTokenListForLine(line);
//		int dot = c.getDot();
//		Token t = RSyntaxUtilities.getTokenAtOffset(tokenList, dot);
//		if (t==null /* EOL */ || !isValidType(t) || isNonWordChar(t)) {
//			// Try to the "left" of the caret.
//			dot--;
//			try {
//				if (dot>=textArea.getLineStartOffset(line)) {
//					t = RSyntaxUtilities.getTokenAtOffset(tokenList, dot);
//				}
//			} catch (BadLocationException ble) {
//				ble.printStackTrace(); // Never happens
//			}
//		}
//
//		// Add new highlights if an identifier is selected.
//		if (t!=null && isValidType(t) && !isNonWordChar(t)) {
//			removeHighlights();
//			RSyntaxTextAreaHighlighter h = (RSyntaxTextAreaHighlighter)
//												textArea.getHighlighter();
//			char[] lexeme = t.getLexeme().toCharArray();
//			int type = t.type;
//			for (int i=0; i<textArea.getLineCount(); i++) {
//				Token temp = textArea.getTokenListForLine(i);
//				while (temp!=null && temp.isPaintable()) {
//					if (temp.is(type, lexeme)) {
//						try {
//							int end = temp.offset + temp.textCount;
//							h.addMarkedOccurrenceHighlight(temp.offset, end, p);
//						} catch (BadLocationException ble) {
//							ble.printStackTrace(); // Never happens
//						}
//					}
//					temp = temp.getNextToken();
//				}
//			}
////textArea.repaint();
////TODO: Do a textArea.repaint() instead of repainting each marker as it's added if count is huge
//		}
//
//	} finally {
//		doc.readUnlock();
//		//time = System.currentTimeMillis() - time;
//		//System.out.println("MarkOccurrencesSupport took: " + time + " ms");
//	}
//
//	textArea.fireMarkedOccurrencesChanged();
//
//}

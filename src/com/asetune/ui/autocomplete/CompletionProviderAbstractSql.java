package com.asetune.ui.autocomplete;

import java.awt.Window;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.asetune.gui.ConnectionDialog;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.ui.autocomplete.completions.AbstractCompletionX;
import com.asetune.ui.autocomplete.completions.DbInfo;
import com.asetune.ui.autocomplete.completions.ProcedureInfo;
import com.asetune.ui.autocomplete.completions.ProcedureParameterInfo;
import com.asetune.ui.autocomplete.completions.SchemaInfo;
import com.asetune.ui.autocomplete.completions.ShorthandCompletionX;
import com.asetune.ui.autocomplete.completions.SqlColumnCompletion;
import com.asetune.ui.autocomplete.completions.SqlDbCompletion;
import com.asetune.ui.autocomplete.completions.SqlProcedureCompletion;
import com.asetune.ui.autocomplete.completions.SqlSchemaCompletion;
import com.asetune.ui.autocomplete.completions.SqlTableCompletion;
import com.asetune.ui.autocomplete.completions.TableColumnInfo;
import com.asetune.ui.autocomplete.completions.TableInfo;
import com.asetune.utils.CollectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

public abstract class CompletionProviderAbstractSql
extends CompletionProviderAbstract
{
	private static Logger _logger = Logger.getLogger(CompletionProviderAbstractSql.class);

	public CompletionProviderAbstractSql(Window owner, ConnectionProvider connectionProvider)
	{
		super(owner, connectionProvider);
	}

	protected Set<String>                   _schemaNames         = new HashSet<String>();

	protected List<DbInfo>                  _dbInfoList          = new ArrayList<DbInfo>();
	protected List<SqlDbCompletion>         _dbComplList         = new ArrayList<SqlDbCompletion>();

	protected List<TableInfo>               _tableInfoList       = new ArrayList<TableInfo>();
	protected List<SqlTableCompletion>      _tableComplList      = new ArrayList<SqlTableCompletion>();

	protected List<ProcedureInfo>           _procedureInfoList   = new ArrayList<ProcedureInfo>();
	protected List<SqlProcedureCompletion>  _procedureComplList  = new ArrayList<SqlProcedureCompletion>();

	protected List<ProcedureInfo>           _systemProcInfoList  = new ArrayList<ProcedureInfo>();
	protected List<SqlProcedureCompletion>  _systemProcComplList = new ArrayList<SqlProcedureCompletion>();
	
	/** put quotes around the "tableNames" */
	protected boolean _quoteTableNames = false;
	protected boolean _addSchemaName   = true;

	protected String _dbProductName           = "";
	protected String _dbExtraNameCharacters   = "";
	protected String _dbIdentifierQuoteString = "\"";
	
	protected String _currentCatalog    = null;
	protected String _currentServerName = null;

	@Override
	public void disconnect()
	{
		super.disconnect();
		
		_quoteTableNames         = false;
		_addSchemaName           = true;
		
		_dbProductName           = "";
		_dbExtraNameCharacters   = "";
		_dbIdentifierQuoteString = "\"";
		
		_currentCatalog          = null;
		_currentServerName       = null;
		
		_schemaNames             .clear();
		_dbInfoList              .clear();
		_dbComplList             .clear();
		_tableInfoList           .clear();
		_tableComplList          .clear();
		_procedureInfoList       .clear();
		_procedureComplList      .clear();
		_systemProcInfoList      .clear();
		_systemProcComplList     .clear();
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

	@Override
	public String getDbProductName()
	{
		if (StringUtil.isNullOrBlank(_dbProductName))
		{
			Connection conn = _connectionProvider.getConnection();
			if (conn != null)
			{
				try { _dbProductName = ConnectionDialog.getDatabaseProductName(conn); }
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
			Connection conn = _connectionProvider.getConnection();
			if (conn != null)
				_currentServerName = DbUtils.getDatabaseServerName(conn, getDbProductName());
		}
		return _currentServerName;
	}

//	@Override
	public String getDbCatalogName()
	{
		Connection conn = _connectionProvider.getConnection();
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
	public void loadSavedCacheFromFilePostAction(List<? extends AbstractCompletionX> list)
	{
		for (AbstractCompletionX compl : list)
		{
			if (compl instanceof SqlTableCompletion)
			{
				SqlTableCompletion c = (SqlTableCompletion) compl;
				_schemaNames.add(c._tableInfo._tabSchema);
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
	}


	private void refresh()
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
					String refreshTimeStr = TimeUtils.msToTimeStr("%MM:%SS.%ms", refreshTimeInMs);
					
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

		int needsLookupCount = 0;
		for (Completion c : cList)
		{
			if (c instanceof SqlTableCompletion)
			{
				if ( ((SqlTableCompletion)c)._tableInfo.isColumnRefreshed() )
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

		String allowedChars = "_.*/:%[]\""; // []" will be stripped off when doing comparisons
		setCharsAllowedInWordCompletion(allowedChars);
		if ( ! StringUtil.isNullOrBlank(_dbExtraNameCharacters) )
			setCharsAllowedInWordCompletion( allowedChars + _dbExtraNameCharacters );

//		// Parse 
//		String allText     = comp.getText();
//		String enteredText = getAlreadyEnteredText(comp);

//		String currentWord = getCurrentWord(textArea);

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
//System.out.println("START: enteredText='"+enteredText+"'.");
//System.out.println("START: currentWord='"+currentWord+"'.");

SqlObjectName etId = new SqlObjectName(enteredText, _dbProductName, _dbIdentifierQuoteString);
//SqlObjectName cwId = new SqlObjectName(currentWord);

//System.out.println("START: enteredText IDENTIFIER: "+ etId);
//System.out.println("START: currentWord IDENTIFIER: "+ cwId);

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


		if (needRefresh())
			refresh();

//System.out.println("getCompletionsSql(): _schemaNames="+_schemaNames);		

		//-----------------------------------------------------------
		// Complete DATABASES
		//
		if ("use".equalsIgnoreCase(prevWord1))
		{
//System.out.println(">>> in: USE completion");
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

		//-----------------------------------------------------------
		// :s = show schemas in current database
		//
		if ( enteredText.equalsIgnoreCase(":s") )
		{
//System.out.println(">>> in: :s SCHEMA REPLACEMENT");
			ArrayList<Completion> cList = new ArrayList<Completion>();

			// lets return all schemas/owners
			for (String schemaName : _schemaNames)
				cList.add( new SqlSchemaCompletion(CompletionProviderAbstractSql.this, schemaName) );
			
			return cList;
		}

		//-----------------------------------------------------------
		// Complete STORED PROCS, if the previous word is EXEC
		// if enteredText also starts with "sp_", then do procedure lookup
		//
		if ( "exec".equalsIgnoreCase(prevWord1) || "execute".equalsIgnoreCase(prevWord1) || "call".equalsIgnoreCase(prevWord1) )
		{
//System.out.println(">>> in: Complete STORED PROCS");
//System.out.println("SqlObjectName: "+etId);
			ArrayList<Completion> procList = new ArrayList<Completion>();

			// OTHER_DB_LOOKUP: Procedures in other databases, do lookup "on the fly"
			if (etId.isFullyQualifiedObject()) // CATALOG.SCHEMA.OBJECT
			{
//System.out.println(">>> in: Complete STORED PROCS: isFullyQualifiedObject");
				Connection conn = _connectionProvider.getConnection();
				if (conn == null)
					return null;

				// Add matching procedures in specified database
				// but NOT if specified database is "sybsystemprocs" and input text starts with sp_
				if ( "sybsystemprocs".equalsIgnoreCase(etId._catName) && etId._objName.startsWith("sp_"))
				{
					// do not add sp_* if current working database is sybsystemprocs...
					// if entered text is 'sp_' those procs will be added a bit down (last in this section)
				}
				else
				{
					List<Completion> list = getProcListWithGuiProgress(conn, etId._catName, etId._schName, etId._objName);
					if ( list != null && ! list.isEmpty() )
						procList.addAll(list);
				}

				// System stored procs, which should be executed in a specific database context
				if (etId._objName.startsWith("sp_"))
				{
					// NOTE: _systemProcComplList probably does NOT have a completion with the database name in it.
					for (SqlProcedureCompletion pc : _systemProcComplList)
					{
						ProcedureInfo pi = pc._procInfo;
						if (startsWithIgnoreCaseOrRegExp(pi._procName, etId._objName))
						{
							SqlProcedureCompletion c = new SqlProcedureCompletion(CompletionProviderAbstractSql.this, pi, true, etId._catName, _addSchemaName, _quoteTableNames);
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
//System.out.println(">>> in: Complete STORED PROCS: isSchemaQualifiedObject");
//System.out.println("_dbComplList:    "+_dbComplList);

//				// If the dbname/catalog exists, then do SCHEMA lookup in that database.
//				for (SqlDbCompletion dc : _dbComplList)
//				{
//					DbInfo di = dc._dbInfo;
//System.out.println("etId._schName='"+etId._schName+"', di._dbName='"+di._dbName+"', _currentCatalog='"+_currentCatalog+"'.");
//					if (etId._schName.equalsIgnoreCase(di._dbName))
//					{
//						// use the local schemas (in current database)
//						if (etId._schName.equalsIgnoreCase(_currentCatalog))
//						{
//System.out.println(">>> in: Complete STORED PROCS: isSchemaQualifiedObject: IN CURRENT DATABASE");
//							// lets return all schemas/owners
//							for (String schemaName : _schemaNames)
//								procList.add( new SqlSchemaCompletion(CompletionProviderAbstractSql.this, etId._schName+"."+schemaName) );
//							
//							// Also return all objects... MATCHING the SCHEMA
//							// Add matching procedures in local database
//							for (SqlProcedureCompletion pc : _procedureComplList)
//							{
//								ProcedureInfo pi = pc._procInfo;
//								if (startsWithIgnoreCaseOrRegExp(pi._procName, etId._objName) && etId._schName.equalsIgnoreCase(pi._procSchema)) 
//									procList.add(pc);
//							}
//						}
//						else // Lookup the schemas for the non-local-database do this ON THE FLY (NON CACHED)
//						{
//System.out.println(">>> in: Complete STORED PROCS: isSchemaQualifiedObject: Lookup the schemas for the non-local-database do this ON THE FLY (NON CACHED)");
//							Connection conn = _connectionProvider.getConnection();
//							if (conn == null)
//								return null;
//
//							List<Completion> list = getSchemaListWithGuiProgress(conn, etId._schName, etId._objName);
//							if ( list != null && ! list.isEmpty() )
//								procList.addAll(list);
//						}
//
//						return procList;
//					}
//				}
				
				// Try another way
				return getProcedureCompletionsFromSchema(_procedureComplList, etId._schName, etId._objName);

			} // end: SCHEMA.OBJECT

			// Add matching procedures in local database
			// but NOT if current working database is "sybsystemprocs" and input text starts with sp_
			if ( "sybsystemprocs".equalsIgnoreCase(_currentCatalog) && etId._objName.startsWith("sp_"))
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
					if (startsWithIgnoreCaseOrRegExp(pi._procName, etId._objName))
						procList.add(pc);
				}
			}

			// Add matching databases
			for (SqlDbCompletion dc : _dbComplList)
			{
				DbInfo di = dc._dbInfo;
				if (startsWithIgnoreCaseOrRegExp(di._dbName, etId._objName))
					procList.add(dc);
			}

			// Add matching schemas
			for (String schemaName : _schemaNames)
			{
				if (startsWithIgnoreCaseOrRegExp(schemaName, etId._objName))
					procList.add( new SqlSchemaCompletion(CompletionProviderAbstractSql.this, schemaName) );
			}

			// Add matching 'sp_*' from system procedures
			if (startsWithIgnoreCaseOrRegExp(etId._objName, "sp_"))
			{
//System.out.println("SYSTEM PROC LOOKUP: schName='"+etId._schName+"', objName='"+etId._objName+"'.");

				for (SqlProcedureCompletion pc : _systemProcComplList)
				{
					ProcedureInfo pi = pc._procInfo;
					if (startsWithIgnoreCaseOrRegExp(pi._procName, etId._objName))
						procList.add(pc);
				}
			}
			return procList;
		} // end: exec

		//-----------------------------------------------------------
		// TABLES in other databases, do lookup 'on the fly'
		// Check for database..tab<ctrl+space> 
		//
		if (etId.isFullyQualifiedObject()) // CATALOG.SCHEMA.OBJECT
		{
//System.out.println(">>> in: TABLES in other databases, do lookup 'on the fly'");
			Connection conn = _connectionProvider.getConnection();
			if (conn == null)
				return null;

			return getTableListWithGuiProgress(conn, etId._catName, etId._schName, etId._objName);
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
		//   or a owner/scema name that we should add to completion
		//      for: select * from db1.<ctrl+space>   == show schemas in catalog 'db1'
		//
		if (enteredText.indexOf('.') >= 0)
		{
//System.out.println(">>> in: TAB_ALIAS.COLUMN_NAME or SCHEMA.TAB or SCHEMA completion");

			String text = enteredText;

			int lastDot = text.lastIndexOf('.');
			
			String colName      = text.substring(lastDot+1);
			String tabAliasName = text.substring(0, lastDot);
//System.out.println("1-tabAliasName='"+tabAliasName+"'.");

			while(tabAliasName.indexOf('.') >= 0)
			{
				tabAliasName = tabAliasName.substring(tabAliasName.lastIndexOf('.')+1);
//System.out.println("2-tabAliasName='"+tabAliasName+"'.");
			}

			colName      = SqlObjectName.stripQuote(colName     , _dbIdentifierQuoteString);
			tabAliasName = SqlObjectName.stripQuote(tabAliasName, _dbIdentifierQuoteString);
//System.out.println("3-tabAliasName='"+tabAliasName+"'.");

			// If the "alias" name (word before the dot) is NOT A column, but a SCHEMA name (in the local database, cached in _schemaNames)
			// then continue lookup tables by schema name
//			if ( _schemaNames.contains(tabAliasName) )
			if ( CollectionUtils.containsIgnoreCase(_schemaNames, tabAliasName) )
			{
				String objNamePattern = colName;
//System.out.println("IN LOCAL SCHEMA: schema='"+tabAliasName+"', objNamePattern='"+objNamePattern+"'.");
				// do completion, but only for tables in a specific schema
				return getTableCompletionsFromSchema(completions, tabAliasName, objNamePattern);
			}
			else // alias is NOT in the "locals" schemas (so hopefully it's a column, but we will discover that...)
			{
//System.out.println("XXXX NOT-IN LOCAL SCHEMA: start");
				// Try to figure out what's the "real" table name for this alias
				// If we find it, display columns for the table (alias)
//				String tabName = getTableNameForAlias(comp, tabAliasName, true);
				String        tabName     = getTableNameForAlias(comp, tabAliasName, false);
				SqlObjectName fullTabName = new SqlObjectName( tabName, _dbProductName, _dbIdentifierQuoteString);
//System.out.println("XXXX NOT-IN LOCAL SCHEMA: fullTabName='"+fullTabName+"'.");

				// Columns to show, will end up in here
				ArrayList<Completion> colList = new ArrayList<Completion>();

				// tablename has no Catalog specification, then do local/cached lookup 
				if ( ! fullTabName.hasCatalogName() )
				{
//System.out.println("XXXX NOT-IN LOCAL SCHEMA: -- CACHED LOOKUP ---");
					// Search the cached table information.
					TableInfo ti = getTableInfo(fullTabName._objName);
					if (ti != null)
					{
						for (TableColumnInfo tci : ti._columns)
						{
							if (startsWithIgnoreCaseOrRegExp(tci._colName, colName))
								colList.add( new SqlColumnCompletion(CompletionProviderAbstractSql.this, tabAliasName, tci._colName, ti));
						}
					}
					// If not any columns was found "in cache", for this table, then do "on the fly" lookup
					// column information is optional to get when refreshing tables...
					// For the "non cached lookup" we need the catalog name, so set this...
					if (colList.isEmpty())
					{
//System.out.println("XXXX NOT-IN LOCAL SCHEMA: -- CACHED LOOKUP FAILED, found 0 column entries in cache ---");
						fullTabName.setCatalogName(_currentCatalog);
					}
				}

				// If cached column lookup failed to find any cached entries, the option might be OFF
				// do a on the fly lookup...
				if (colList.isEmpty())
				{
//System.out.println("XXXX NOT-IN LOCAL SCHEMA: -- ON-THE-FLY LOOKUP ---");
					Connection conn = _connectionProvider.getConnection();
					if (conn == null)
						return null;

					List<Completion> tabList = getTableListWithGuiProgress(conn, fullTabName._catName, fullTabName._schName, fullTabName._objName);
//System.out.println("    LOOKUP-RESULT: " + tabList);
//System.out.println("    LOOKUP-RESULT: size="+tabList.size());

					// search the *tables* found when doing lookup (it might return several suggestions)
					// STOP after first *exact* TABLE match
					Completion c = null;
					for (Completion ce : tabList)
					{
//System.out.println("    COMP.getInputText()='"+ce.getInputText()+"'.");
						if (fullTabName._objName.equalsIgnoreCase(ce.getInputText()))
						{
							c = ce;
							break;
						}
					}
//System.out.println("    C = "+c);

					// If we found a TABLE, lets get columns that are matching up to currently entered text
					if (c != null && c instanceof SqlTableCompletion)
					{
						SqlTableCompletion jdbcCompl = (SqlTableCompletion) c;
						for (TableColumnInfo tci : jdbcCompl._tableInfo._columns)
						{
							if (startsWithIgnoreCaseOrRegExp(tci._colName, colName))
								colList.add( new SqlColumnCompletion(CompletionProviderAbstractSql.this, tabAliasName, tci._colName, jdbcCompl._tableInfo));
						}
					}
				}

				//
				// Still no values (no columns were found)
				// ---- Do SCHEMA lookup. ----
				// Example of eneterd text: select * from db1.<ctrl+space>   == show schemas in catalog 'db1'
				//
				if (colList.isEmpty())
				{
					int xdot1 = text.indexOf('.');
					final String catName = SqlObjectName.stripQuote( text.substring(0, xdot1), _dbIdentifierQuoteString);
					final String schName = SqlObjectName.stripQuote( text.substring(xdot1+1) , _dbIdentifierQuoteString);
//System.out.println("XXXX NOT-IN LOCAL SCHEMA: DO SCHEMA-LOOKUP catName='"+catName+"', schName='"+schName+"'.");

					// Serach all known catalogs/databases
					for (SqlDbCompletion dc : _dbComplList)
					{
						DbInfo di = dc._dbInfo;
						if (catName.equalsIgnoreCase(di._dbName))
						{
							// if entered catalog/db name is current working catalog/db
							//   return known schema names in current catalog/db
							// else
							//   get available schemas for the specified catalog/db (NOT cached) 
							if (catName.equalsIgnoreCase(_currentCatalog))
							{
//System.out.println("XXXX NOT-IN LOCAL SCHEMA: DO SCHEMA-LOOKUP... in CURRENT-CATALOG.");
								// lets return all schemas/owners
								for (String schemaName : _schemaNames)
									colList.add( new SqlSchemaCompletion(CompletionProviderAbstractSql.this, catName+"."+schemaName) );
							}
							else // Lookup the schemas for the non-local-database do this ON THE FLY (NON CACHED)
							{
//System.out.println("XXXX NOT-IN LOCAL SCHEMA: DO SCHEMA-LOOKUP... LOOKUP-ON-THE-FLY.");
								Connection conn = _connectionProvider.getConnection();
								if (conn == null)
									return null;

								List<Completion> list = getSchemaListWithGuiProgress(conn, catName, schName);
								if ( list != null && ! list.isEmpty() )
									colList.addAll(list);
							}

							break; // loop: _dbComplList, (after first match)
						}
					}
				}
	
//System.out.println("XXXX NOT-IN LOCAL SCHEMA: RETURN: colList: "+colName);
				return colList;
			}
		} // end ALIAS check: if (enteredText.indexOf('.') >= 0)


		// IF WE GET HERE, I could not figgure out
		// what to deliver as Completion text so
		// LETS return "everything" that has been added as the BASE COMPLETION (catalogs, schemas, tables/views, staticCompletion), but NOT storedProcedures


//		return super.getCompletionsImpl(comp);
		
		// completions is defined in org.fife.ui.autocomplete.AbstractCompletionProvider
		return getCompletionsFrom(completions, enteredText);
	}

	protected List<Completion> getTableCompletionsFromSchema(List<Completion> completions, String schemaName, String lastPart)
	{
		ArrayList<Completion> retComp = new ArrayList<Completion>();
		for (Completion c : completions)
		{
			if (c instanceof SqlTableCompletion)
			{
				SqlTableCompletion tabComp = (SqlTableCompletion) c;
				if (schemaName.equalsIgnoreCase(tabComp._tableInfo._tabSchema))
					retComp.add(c);
			}
		}
		return getCompletionsFrom(retComp, lastPart);
	}
	
	protected List<Completion> getProcedureCompletionsFromSchema(List<SqlProcedureCompletion> ComplList, String schemaName, String lastPart)
	{
		ArrayList<Completion> retComp = new ArrayList<Completion>();
		for (Completion c : ComplList)
		{
			if (c instanceof SqlProcedureCompletion)
			{
				SqlProcedureCompletion procComp = (SqlProcedureCompletion) c;
				if (schemaName.equalsIgnoreCase(procComp._procInfo._procSchema))
					retComp.add(c);
			}
		}
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
		String txt  = comp.getText().toLowerCase().replace('\n', ' ');
		int selectIndex = -1;
		while (true)
		{
			int i = txt.indexOf("select ", selectIndex + 1);
			if (i == -1)
				break;
			if (i >= cp)
				break;

			selectIndex = i;
		}
		int fromIndex = txt.indexOf(" from ", selectIndex);

		String fromStr = null;
		if (fromIndex >= 0)
		{
			fromIndex += " from ".length();
			int stopIndex = -1;
			if (stopIndex == -1) stopIndex = txt.indexOf(" where ",    fromIndex);
			if (stopIndex == -1) stopIndex = txt.indexOf(" group by ", fromIndex);
			if (stopIndex == -1) stopIndex = txt.indexOf(" order by ", fromIndex);
			if (stopIndex == -1) stopIndex = txt.indexOf(" select ",   fromIndex);
			if (stopIndex == -1) stopIndex = txt.indexOf(" update ",   fromIndex);
			if (stopIndex == -1) stopIndex = txt.indexOf(" delete ",   fromIndex);

			// If we can't find any "delimiter char", then try newline 
//			if (stopIndex == -1) stopIndex = otxt.indexOf('\n', fromIndex);
			if (stopIndex == -1) stopIndex = otxt.length();
			
			if (stopIndex >= 0)
				fromStr = otxt.substring(fromIndex, stopIndex);
		}

		// Try to split the from str
		if (fromStr != null)
		{
			int aliasIndex = fromStr.indexOf(" "+alias);
			if (aliasIndex >= 0)
			{
				String[] sa = fromStr.split(",");
			//	System.out.println("sa[]: >"+StringUtil.toCommaStr(sa, "|")+"<");
				for (String s : sa)
				{
					s = s.trim();
					if (StringUtil.isNullOrBlank(s))
						continue;

					String[] sa2 = s.split("\\s+");
				//	System.out.println("sa2[]: >"+StringUtil.toCommaStr(sa2, "|")+"<");
					if (sa2.length >= 2)
					{
						if (sa2[1].equalsIgnoreCase(alias))
						{
							table = sa2[0];
							if (stripPrefix && table.indexOf('.') >= 0)
							{
								table = table.substring( table.lastIndexOf('.') + 1 );
							}
						//	System.out.println("Alias to TabName: alias'"+alias+"', tabname='"+table+"'.");
						}
					}
				}
			}
			//System.out.println("FROM STR='"+fromStr+"'.");
		}

		return table;
	}

//	abstract protected void refreshCompletionForStaticCmds();
//	protected void refreshCompletionForStaticCmds()
//	{
//		_logger.error("CompletionProviderAbstractSql.refreshCompletionForStaticCmds() is called this SHOULD REALLY BE ABSTACT IN HERE...");
//	}

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
		if (waitDialog.wasCancelPressed())
			return;

		// Obtain a DatabaseMetaData object from our current connection        
		DatabaseMetaData dbmd = conn.getMetaData();

		// What PRODUCT are we connected to 
		_dbProductName = dbmd.getDatabaseProductName();

		_currentServerName = DbUtils.getDatabaseServerName(conn, _dbProductName);

		_dbExtraNameCharacters   = dbmd.getExtraNameCharacters();
		_dbIdentifierQuoteString = dbmd.getIdentifierQuoteString();

		_logger.info("JDBC DatabaseMetaData.getDatabaseProductName()   is '"+_dbProductName+"'.");
		_logger.info("JDBC DatabaseMetaData.getExtraNameCharacters()   is '"+_dbExtraNameCharacters+"'.");
		_logger.info("JDBC DatabaseMetaData.getIdentifierQuoteString() is '"+_dbIdentifierQuoteString+"'.");
		
		// get current catalog/dbName
		_currentCatalog = conn.getCatalog();

		if (false || _logger.isDebugEnabled())
		{
			_logger.info("getCatalogSeparator:            "+dbmd.getCatalogSeparator());
			_logger.info("getCatalogTerm:                 "+dbmd.getCatalogTerm());
			_logger.info("getDefaultTransactionIsolation: "+dbmd.getDefaultTransactionIsolation());
			_logger.info("getProcedureTerm:               "+dbmd.getProcedureTerm());
			_logger.info("getSchemaTerm:                  "+dbmd.getSchemaTerm());
			_logger.info("getSearchStringEscape:          "+dbmd.getSearchStringEscape());
			_logger.info("getSQLKeywords:                 "+dbmd.getSQLKeywords());
			_logger.info("getNumericFunctions:            "+dbmd.getNumericFunctions());
			_logger.info("getSQLStateType:                "+dbmd.getSQLStateType());
			_logger.info("getStringFunctions:             "+dbmd.getStringFunctions());
			_logger.info("getSystemFunctions:             "+dbmd.getSystemFunctions());
			_logger.info("getTimeDateFunctions:           "+dbmd.getTimeDateFunctions());
			_logger.info("getURL:                         "+dbmd.getURL());
			_logger.info("getCatalogs\n"             +new ResultSetTableModel(dbmd.getCatalogs(),              "getCatalogs").toTableString());
			_logger.info("getSchemas\n"              +new ResultSetTableModel(dbmd.getSchemas(),               "getSchemas").toTableString());
//			_logger.info("getClientInfoProperties\n" +new ResultSetTableModel(dbmd.getClientInfoProperties(),  "getClientInfoProperties").toTableString());
//			_logger.info("getTableTypes\n"           +new ResultSetTableModel(dbmd.getTableTypes(),            "getTableTypes").toTableString());
//			_logger.info("getTypeInfo\n"             +new ResultSetTableModel(dbmd.getTypeInfo(),              "getTypeInfo").toTableString());
		}		
//_logger.info("getTableTypes\n"           +new ResultSetTableModel(dbmd.getTableTypes(),            "getTableTypes").toTableString());

		if (DbUtils.DB_PROD_NAME_H2.equals(_dbProductName))
		{
			waitDialog.setState("Getting H2 database settings");

			// if 'DATABASE_TO_UPPER' is true, then table names must be quoted
			String sql = "select VALUE from INFORMATION_SCHEMA.SETTINGS where NAME = 'DATABASE_TO_UPPER'";
			try
			{
				Statement stmnt = conn.createStatement();
				ResultSet rs = stmnt.executeQuery(sql);
				while(rs.next())
				{
					String value = rs.getString(1);
					
					_quoteTableNames = value.trim().equalsIgnoreCase("true");
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

			di._dbName = rs.getString("TABLE_CAT");

			dbInfoList.add(di);
		}
		rs.close();

		return dbInfoList;
	}

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
			if ( ! schemaName.endsWith("%") )
				schemaName += "%";
		}

		ResultSet rs = dbmd.getSchemas(catalogName, schemaName);

		while(rs.next())
		{
			SchemaInfo si = new SchemaInfo();

			si._cat  = rs.getString("TABLE_CATALOG");
			si._name = rs.getString("TABLE_SCHEM");

			// On some databases, do not show all the ***_role things, they are not schema or users...
			if (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(_dbProductName) && si._name != null && si._name.endsWith("_role"))
				continue;

			schemaInfoList.add(si);
		}
		rs.close();

		return schemaInfoList;
	}

	@Override
	public TableModel getLookupTableTypesModel()
	{
		Connection conn = _connectionProvider.getConnection();
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
				String type = rs.getString(1).trim();
				
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
				String type = rs.getString(1).trim();
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
			if ( ! schemaName.endsWith("%") )
				schemaName += "%";
		}

		if (tableName == null)
			tableName = "%";
		else
		{
			tableName = tableName.replace('*', '%').trim();
			if ( ! tableName.endsWith("%") )
				tableName += "%";
		}

		// What table types do we want to retrieve
		String[] types = getTableTypes(conn);

		if (_logger.isDebugEnabled())
			_logger.debug("refreshCompletionForTables(): calling dbmd.getTables(catalog='"+catalogName+"', schema=null, table='"+tableName+"', types='"+StringUtil.toCommaStr(types)+"')");

		ResultSet rs = dbmd.getTables(catalogName, schemaName, tableName, types);
		
		int counter = 0;
		while(rs.next())
		{
			counter++;
			if ( (counter % 100) == 0 )
				waitDialog.setState(stateMsg + " (Fetch count "+counter+")");

			TableInfo ti = new TableInfo();
			ti._tabCat     = rs.getString(1);
			ti._tabSchema  = rs.getString(2);
			ti._tabName    = rs.getString(3);
			ti._tabType    = rs.getString(4);
			ti._tabRemark  = rs.getString(5);

			// add schemas... this is a Set so duplicates is ignored
			_schemaNames.add(ti._tabSchema);

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
//			if ( ! catalogName.endsWith("%") )
//				catalogName += "%";
//		}
//
//		// fix schemaName
//		if (schemaName != null)
//		{
//			schemaName = schemaName.replace('*', '%').trim();
//			if ( ! schemaName.endsWith("%") )
//				schemaName += "%";
//		}
//
//		// fix tableName
//		if (tableName == null)
//			tableName = "%";
//		else
//		{
//			tableName = tableName.replace('*', '%').trim();
//			if ( ! tableName.endsWith("%") )
//				tableName += "%";
//		}

		// fix colName
		if (colName == null)
			colName = "%";
		else
		{
			colName = colName.replace('*', '%').trim();
			if ( ! colName.endsWith("%") )
				colName += "%";
		}

		// Obtain a DatabaseMetaData object from our current connection
		ResultSet rs = dbmd.getColumns(catalogName, schemaName, tableName, colName);

		int counter = 0;
		while(rs.next())
		{
			counter++;
			if ( (counter % 100) == 0 )
				waitDialog.setState(stateMsg + " (Fetch count "+counter+")");

			String tabCatalog = rs.getString("TABLE_CAT");
			String tabSchema  = rs.getString("TABLE_SCHEM");
			String tabName    = rs.getString("TABLE_NAME");
			
			TableColumnInfo ci = new TableColumnInfo();
			ci._colName       = rs.getString("COLUMN_NAME");
			ci._colPos        = rs.getInt   ("ORDINAL_POSITION");
			ci._colType       = rs.getString("TYPE_NAME");
			ci._colLength     = rs.getInt   ("COLUMN_SIZE");
			ci._colIsNullable = rs.getInt   ("NULLABLE");
			ci._colRemark     = rs.getString("REMARKS");
			ci._colDefault    = rs.getString("COLUMN_DEF");
			ci._colScale      = rs.getInt   ("DECIMAL_DIGITS");

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

			int counter = 0;
			while(rs.next())
			{
				counter++;
				if ( (counter % 100) == 0 )
					waitDialog.setState(stateMsg + " (Fetch count "+counter+")");

	//			String tabCatalog = rs.getString("TABLE_CAT");
	//			String tabSchema  = rs.getString("TABLE_SCHEM");
				String tabName    = rs.getString("TABLE_NAME");
				
				TableColumnInfo ci = new TableColumnInfo();
				ci._colName       = rs.getString("COLUMN_NAME");
				ci._colPos        = rs.getInt   ("ORDINAL_POSITION");
				ci._colType       = rs.getString("TYPE_NAME");
				ci._colLength     = rs.getInt   ("COLUMN_SIZE");
				ci._colIsNullable = rs.getInt   ("NULLABLE");
				ci._colRemark     = rs.getString("REMARKS");
				ci._colDefault    = rs.getString("COLUMN_DEF");
				ci._colScale      = rs.getInt   ("DECIMAL_DIGITS");
	
				if ( ! prevTabName.equals(tabName) )
				{
					prevTabName = tabName;
					tabInfo = getTableInfo(tabName);
				}
				if (tabInfo == null)
					continue;
	
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
				ti._needColumnRefresh = false;

				ResultSet rs = dbmd.getColumns(ti._tabCat, ti._tabSchema, ti._tabName, "%");
				while(rs.next())
				{
					TableColumnInfo ci = new TableColumnInfo();
					ci._colName       = rs.getString("COLUMN_NAME");
					ci._colPos        = rs.getInt   ("ORDINAL_POSITION");
					ci._colType       = rs.getString("TYPE_NAME");
					ci._colLength     = rs.getInt   ("COLUMN_SIZE");
					ci._colIsNullable = rs.getInt   ("NULLABLE");
					ci._colRemark     = rs.getString("REMARKS");
					ci._colDefault    = rs.getString("COLUMN_DEF");
					ci._colScale      = rs.getInt   ("DECIMAL_DIGITS");
					
//System.out.println("refreshCompletionForTableColumns(): bulkGetColumns="+bulkGetColumns+", ROW="+ci);
					ti.addColumn(ci);
				}
				rs.close();

//				// PK INFO
//				rs = dbmd.getPrimaryKeys(null, null, ti._tabName);
//				ResultSetTableModel rstm = new ResultSetTableModel(rs, "getPrimaryKeys");
//				System.out.println("######### PK("+ti._tabName+"):--------\n" + rstm.toTableString());
//				while(rs.next())
//				{
//					TablePkInfo pk = new TablePkInfo();
//					pk._colName       = rs.getString("COLUMN_NAME");
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
//					index._colName       = rs.getString("COLUMN_NAME");
//					
//					ti.addIndex(index);
//				}
//				rs.close();
			} // end: for (TableInfo ti : tableInfoList)
		} // end: fetch-table-by-table
	}

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
			if ( ! schemaName.endsWith("%") )
				schemaName += "%";
		}

		if (procName == null)
			procName = "%";
		else
		{
			procName = procName.replace('*', '%').trim();
			if ( ! procName.endsWith("%") )
				procName += "%";
		}

		_logger.debug("refreshCompletionForProcedures(): calling dbmd.getProcedures(catalog='"+catalogName+"', schema=null, procName='"+procName+"')");

		ResultSet rs = dbmd.getProcedures(catalogName, schemaName, procName);

		int counter = 0;
		while(rs.next())
		{
			counter++;
			if ( (counter % 100) == 0 )
				waitDialog.setState(stateMsg + " (Fetch count "+counter+")");

			ProcedureInfo pi = new ProcedureInfo();
			pi._procCat          = rs.getString("PROCEDURE_CAT");
			pi._procSchema       = rs.getString("PROCEDURE_SCHEM");
			pi._procName         = rs.getString("PROCEDURE_NAME");
			pi._procType         = decodeProcedureType(rs.getInt("PROCEDURE_TYPE"));
			pi._procRemark       = rs.getString("REMARKS");
//			pi._procSpecificName = rs.getString("SPECIFIC_NAME"); //in HANA = not there...

			procInfoList.add(pi);
			
			if (waitDialog.wasCancelPressed())
				return procInfoList;
		}
		rs.close();
		
		return procInfoList;
	}
	public String decodeProcedureType(int type)
	{
		if      (type == DatabaseMetaData.procedureResultUnknown) return "Procedure (Result-Unknown)";
		else if (type == DatabaseMetaData.procedureNoResult)      return "Procedure (No-Result)";
		else if (type == DatabaseMetaData.procedureReturnsResult) return "Procedure (Returns-Results)";
		else return "Procedure";
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

			int counter = 0;
			while(rs.next())
			{
				counter++;
				if ( (counter % 100) == 0 )
					waitDialog.setState(stateMsg + " (Fetch count "+counter+")");

				colId++;
	//			String procCatalog = rs.getString("PROCEDURE_CAT");
	//			String procSchema  = rs.getString("PROCEDURE_SCHEM");
				String procName    = rs.getString("PROCEDURE_NAME");
				
				ProcedureParameterInfo pi = new ProcedureParameterInfo();
				pi._paramName       = rs.getString("COLUMN_NAME");
				pi._paramPos        = colId;
				pi._paramInOutType  = procInOutDecode(rs.getShort("COLUMN_TYPE")); // IN - OUT - INOUT
				pi._paramType       = rs.getString("TYPE_NAME");
				pi._paramLength     = rs.getInt   ("LENGTH");
				pi._paramIsNullable = rs.getInt   ("NULLABLE");
				pi._paramRemark     = rs.getString("REMARKS");
				pi._paramDefault    = rs.getString("COLUMN_DEF");
				pi._paramScale      = rs.getInt   ("SCALE");
	
				if ( ! prevProcName.equals(procName) )
				{
					colId = 0;
					prevProcName = procName;
					procInfo = getProcedureInfo(procName);
				}
				if (procInfo == null)
					continue;
	
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

				while(rs.next())
				{
					colId++;

					ProcedureParameterInfo ppi = new ProcedureParameterInfo();
					ppi._paramName       = rs.getString("COLUMN_NAME");
					ppi._paramPos        = colId;
					ppi._paramInOutType  = procInOutDecode(rs.getShort("COLUMN_TYPE")); // IN - OUT - INOUT
					ppi._paramType       = rs.getString("TYPE_NAME");
					ppi._paramLength     = rs.getInt   ("LENGTH");
					ppi._paramIsNullable = rs.getInt   ("NULLABLE");
					ppi._paramRemark     = rs.getString("REMARKS");
					ppi._paramDefault    = rs.getString("COLUMN_DEF");
					ppi._paramScale      = rs.getInt   ("SCALE");
					
//System.out.println("refreshCompletionForTableColumns(): bulkMode="+bulkMode+", ROW="+ppi);
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
	protected List<SqlTableCompletion> refreshCompletion()
	{
//System.out.println("SQL: refreshCompletion()");
		final Connection conn = _connectionProvider.getConnection();
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
					getWaitDialog().setState("Creating Mandatory Completions.");
					refreshCompletionForMandatory(conn, getWaitDialog());
					_logger.debug("---------------- Refresh Completion: Mandatory Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));

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
						getWaitDialog().setState("Creating Miscelanious Completions.");
						refreshCompletionForMisc(conn, getWaitDialog());
						_logger.debug("---------------- Refresh Completion: MISC Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));
					}

					//----------------------------------------------------------
					// Get database information
					if (isLookupDb())
					{
						thisStartTime = System.currentTimeMillis();
						//----------------------------------------------------------
						// Get DB information
						_dbInfoList = refreshCompletionForDbs(conn, getWaitDialog());
						_logger.debug("---------------- Refresh Completion: DB Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));

//						// If SQLExceptions has been down graded to SQLWarnings in the jConnect message handler
//						AseConnectionUtils.checkSqlWarningsAndThrowSqlExceptionIfSeverityIsAbove10(conn.getWarnings());
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


					//----------------------------------------------------------
					// Get Table and Columns information
					if (isLookupTableName())
					{
						thisStartTime = System.currentTimeMillis();
						_tableInfoList = refreshCompletionForTables(conn, getWaitDialog());
						enrichCompletionForTables(conn, getWaitDialog());
						_logger.debug("---------------- Refresh Completion: TAB-1 Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));
						if (isLookupTableColumns())
						{
							refreshCompletionForTableColumns(conn, getWaitDialog(), _tableInfoList, true);
							_logger.debug("---------------- Refresh Completion: TAB-2 Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));
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
    						for (SchemaInfo si : allSchemas)
    							_schemaNames.add(si._name);
						}

						// Add all schema names
						for (String schemaName : _schemaNames)
						{
							SqlSchemaCompletion c = new SqlSchemaCompletion(CompletionProviderAbstractSql.this, schemaName);
							completionList.add(c);
						}
					}


					//----------------------------------------------------------
					// Get USER Procedure and Parameters information
					if (isLookupProcedureName())
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

					//----------------------------------------------------------
					// Get SYSTEM Procedure and Parameters information
					// Only do this once
					if (isLookupSystemProcedureName())
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
								SqlProcedureCompletion c = new SqlProcedureCompletion(CompletionProviderAbstractSql.this, pi, false, null, _addSchemaName, _quoteTableNames);
								_systemProcComplList.add(c);
							}
						}
					}

					_logger.debug("Refresh Completion: TOTAL Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-allStartTime));
				}
				catch (SQLException e)
				{
					_logger.info("Problems reading table information for SQL Table code completion.", e);
				}
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
		ArrayList<SqlTableCompletion> list = (ArrayList)wait.execAndWait(doWork);
		if (list == null)
		{
			if (doWork.hasException())
				_logger.error("Problems when refreshing Code Completion. Caught:"+doWork.getException(), doWork.getException());
		}

		return list;
    }

	/**
	 * Lookup TABLES "on the fly", do not cache
	 * 
	 * @param conn
	 * @param catName
	 * @param objName
	 * @return
	 */
	private List<Completion> getTableListWithGuiProgress(final Connection conn, final String catName, final String schemaName, final String objName)
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
					// Get Table and Columns informaation
					List<TableInfo> tableInfoList = refreshCompletionForTables(conn, getWaitDialog(), catName, schemaName, objName);
//					if (isLookupTableColumns())
//						refreshCompletionForTableColumns(conn, getWaitDialog(), tableInfoList);
					if (tableInfoList.size() < 25)
						refreshCompletionForTableColumns(conn, getWaitDialog(), tableInfoList, false);

					// Create completion list
					getWaitDialog().setState("Creating Table Completions.");
					for (TableInfo ti : tableInfoList)
					{
//						SqlTableCompletion c = new SqlTableCompletion(_thisBaseClass, ti, _quoteTableNames);
						SqlTableCompletion c = new SqlTableCompletion(CompletionProviderAbstractSql.this, ti, true, _addSchemaName, _quoteTableNames);
						completionList.add(c);
					}
				}
				catch (SQLException e)
				{
					_logger.info("Problems reading table information for SQL Table code completion.", e);
				}

				return completionList;
			}
		}; // END: new WaitForExecDialog.BgExecutor()
		
		// Execute and WAIT
		ArrayList<Completion> list = (ArrayList)wait.execAndWait(doWork);
		if (list == null)
		{
			if (doWork.hasException())
				_logger.error("Problems when refreshing Code Completion (for getTableListWithGuiProgress). Caught:"+doWork.getException(), doWork.getException());
		}

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
	private List<Completion> getProcListWithGuiProgress(final Connection conn, final String catName, final String schemaName, final String objName)
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
					// Get Table and Columns informaation
					if (isLookupTableName())
					{
						List<ProcedureInfo> procInfoList = refreshCompletionForProcedures(conn, getWaitDialog(), catName, schemaName, objName);
						if (procInfoList.size() < 25)
							refreshCompletionForProcedureParameters(conn, getWaitDialog(), procInfoList, true);
	
						// Create completion list
						getWaitDialog().setState("Creating Table Completions.");
						for (ProcedureInfo pi : procInfoList)
						{
							SqlProcedureCompletion c = new SqlProcedureCompletion(CompletionProviderAbstractSql.this, pi, true, catName, _addSchemaName, _quoteTableNames);
							completionList.add(c);
						}
					}
				}
				catch (SQLException e)
				{
					_logger.info("Problems reading table information for SQL Procedure code completion.", e);
				}

				return completionList;
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
					// Get Table and Columns informaation
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
//				"<font color=\"blue\">"+di._dbType+"</font>" +
////				" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(di._dbRemark) ? "No Description" : di._dbRemark) + "</font></i>";
//				" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(di._dbRemark) ? "" : di._dbRemark) + "</font></i>";
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
//				"<font color=\"blue\">"+schemaName+"</font>" +
//				" -- <i><font color=\"green\">SCHEMA</font></i>";
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
//				"<font color=\"blue\">"+si._name+"</font>" +
//				" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(si._remark) ? "" : si._remark) + "</font></i>";
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
//				"<font color=\"blue\">"+ti._tabType+"</font>" +
////				" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(ti._tabRemark) ? "No Description" : ti._tabRemark) + "</font></i>";
//				" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(ti._tabRemark) ? "" : ti._tabRemark) + "</font></i>";
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
//				"<font color=\"blue\">"+pi._procType+"</font>" +
//				(StringUtil.isNullOrBlank(pi._procSpecificName) ? "" : ", SpecificName="+pi._procSpecificName) +
////				" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(pi._procRemark) ? "No Description" : pi._procRemark) + "</font></i>";
//				" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(pi._procRemark) ? "" : pi._procRemark) + "</font></i>";
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
//				"<font color=\"blue\">"+_tableInfo.getColDdlDesc(colname)+"</font>" +
//				" -- <i><font color=\"green\">" + colPos + _tableInfo.getColDescription(colname) + "</font></i>";
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
//			sb.append("<B>").append(_dbName).append("</B> - <font color=\"blue\">").append(_dbType).append("</font>");
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
//			sb.append("<B>").append(_name).append("</B> - <font color=\"blue\">").append(_cat).append("</font>");
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
					
				if (getColInfo && ! ti.isColumnRefreshed())
					ti.refreshColumnInfo(_connectionProvider);
					
				return ti;
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
//					ci._colName       = rs.getString("COLUMN_NAME");
//					ci._colPos        = rs.getInt   ("ORDINAL_POSITION");
//					ci._colType       = rs.getString("TYPE_NAME");
//					ci._colLength     = rs.getInt   ("COLUMN_SIZE");
//					ci._colIsNullable = rs.getInt   ("NULLABLE");
//					ci._colRemark     = rs.getString("REMARKS");
//					ci._colDefault    = rs.getString("COLUMN_DEF");
//					ci._colScale      = rs.getInt   ("DECIMAL_DIGITS");
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
//			sb.append(_tabSchema).append(".<B>").append(_tabName).append("</B> - <font color=\"blue\">").append(_tabType).append("</font>");
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
//			sb.append(_tabSchema).append(".<B>").append(_tabName).append(".").append(ci._colName).append("</B> - <font color=\"blue\">").append(_tabType).append(" - COLUMN").append("</font>");
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

//	/**
//	 * Holds information about procedures
//	 */
//	protected static class ProcedureInfo
//	implements Serializable
//	{
//		private static final long serialVersionUID = 1L;
//
//		public String _procCat     = null;
//		public String _procSchema  = null;
//		public String _procName    = null;
//		public String _procType    = null;
//		public String _procRemark  = null;
//		public String _procSpecificName = null;
//		
//		public boolean _needParamsRefresh = true;
//		public boolean isParamsRefreshed() {return ! _needParamsRefresh;}
//
//		public ArrayList<ProcedureParameterInfo> _parameters = new ArrayList<ProcedureParameterInfo>();
//
//		public void addParameter(ProcedureParameterInfo ci)
//		{
//			_parameters.add(ci);
//		}
//
//		public void refreshParameterInfo(ConnectionProvider connProvider)
//		{
//			try
//			{
//				final Connection conn = connProvider.getConnection();
//				if (conn == null)
//					return;
//
//				DatabaseMetaData dbmd = conn.getMetaData();
//
//				int colId = 0;
//				ResultSet rs = dbmd.getProcedureColumns(_procCat, _procSchema, _procName, "%");
//
//				while(rs.next())
//				{
//					colId++;
//
//					ProcedureParameterInfo ppi = new ProcedureParameterInfo();
//					ppi._paramName       = rs.getString("COLUMN_NAME");
//					ppi._paramPos        = colId;
//					ppi._paramInOutType  = procInOutDecode(rs.getShort("COLUMN_TYPE")); // IN - OUT - INOUT
//					ppi._paramType       = rs.getString("TYPE_NAME");
//					ppi._paramLength     = rs.getInt   ("LENGTH");
//					ppi._paramIsNullable = rs.getInt   ("NULLABLE");
//					ppi._paramRemark     = rs.getString("REMARKS");
//					ppi._paramDefault    = rs.getString("COLUMN_DEF");
//					ppi._paramScale      = rs.getInt   ("SCALE");
//					
//					addParameter(ppi);
//				}
//				rs.close();
//
//				_needParamsRefresh = false;
//			}
//			catch (SQLException e)
//			{
//				_logger.warn("Problems looking up Parmeter MetaData for procedure '"+_procName+"'. Caught: "+e);
//			}
//		}
//
//		@Override
//		public String toString()
//		{
//			return super.toString() + ": cat='"+_procCat+"', schema='"+_procSchema+"', name='"+_procName+"', type='"+_procType+"', remark='"+_procRemark+"'";
//		}
//
//		public String toHtmlString()
//		{
//			StringBuilder sb = new StringBuilder();
////			sb.append(_tabType).append(" - <B>").append(_tabName).append("</B>");
//			sb.append(_procSchema).append(".<B>").append(_procName).append("</B> - <font color=\"blue\">").append(_procType).append("</font>");
//			sb.append("<HR>");
//			sb.append("<BR>");
//			sb.append("<B>Description:</B> ").append(StringUtil.isNullOrBlank(_procRemark) ? "not available" : _procRemark).append("<BR>");
//			sb.append("<BR>");
//			sb.append("<B>Columns:</B> ").append("<BR>");
//			sb.append("<TABLE ALIGN=\"left\" BORDER=0 CELLSPACING=0 CELLPADDING=1\">");
//			sb.append("<TR ALIGN=\"left\" VALIGN=\"top\" BGCOLOR=\"#ffffff\">");
//			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Name")       .append("</B></FONT></TD>");
//			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("ParamType")  .append("</B></FONT></TD>");
//			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Datatype")   .append("</B></FONT></TD>");
//			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Length")     .append("</B></FONT></TD>");
//			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Nulls")      .append("</B></FONT></TD>");
//			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Pos")        .append("</B></FONT></TD>");
//			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Description").append("</B></FONT></TD>");
//			sb.append("</TR>");
//			int r=0;
//			for (ProcedureParameterInfo pi : _parameters)
//			{
//				r++;
//				if ( (r % 2) == 0 )
//					sb.append("<TR ALIGN=\"left\" VALIGN=\"top\" BGCOLOR=\"#ffffff\">");
//				else
//					sb.append("<TR ALIGN=\"left\" VALIGN=\"top\" BGCOLOR=\"#ffffcc\">");
//				sb.append("	<TD NOWRAP>").append(pi._paramName)      .append("</TD>");
//				sb.append("	<TD NOWRAP>").append(pi._paramInOutType) .append("</TD>");
//				sb.append("	<TD NOWRAP>").append(pi._paramType)      .append("</TD>");
//				sb.append("	<TD NOWRAP>").append(pi._paramLength)    .append("</TD>");
//				sb.append("	<TD NOWRAP>").append(pi._paramIsNullable).append("</TD>");
//				sb.append("	<TD NOWRAP>").append(pi._paramPos)       .append("</TD>");
//				sb.append("	<TD NOWRAP>").append(pi._paramRemark != null ? pi._paramRemark : "not available").append("</TD>");
//				sb.append("</TR>");
//			}
//			sb.append("</TABLE>");
//			sb.append("<HR>");
//			sb.append("-end-<BR><BR>");
//			
//			return sb.toString();
//		}
//
//		public String toHtmlString(String paramName)
//		{
//			ProcedureParameterInfo pi = null;
//			for (ProcedureParameterInfo e : _parameters)
//			{
//				if (paramName.equalsIgnoreCase(e._paramName))
//				{
//					pi = e;
//					break;
//				}
//			}
//			if (pi == null)
//				return "Parameter name '"+paramName+"', was not found in procedure '"+_procName+"'.";
//
//			StringBuilder sb = new StringBuilder();
//			sb.append(_procSchema).append(".<B>").append(_procName).append(".").append(pi._paramName).append("</B> - <font color=\"blue\">").append(_procType).append(" - COLUMN").append("</font>");
//			sb.append("<HR>"); // add Horizontal Ruler: ------------------
//			sb.append("<BR>");
//			sb.append("<B>Procedure Description:</B> ").append(StringUtil.isNullOrBlank(_procRemark) ? "not available" : _procRemark).append("<BR>");
//			sb.append("<BR>");
//			sb.append("<B>Column Description:</B> ").append(StringUtil.isNullOrBlank(pi._paramRemark) ? "not available" : pi._paramRemark).append("<BR>");
//			sb.append("<BR>");
//			sb.append("<B>Name:</B> ")       .append(pi._paramName)      .append("<BR>");
//			sb.append("<B>In/Out:</B> ")     .append(pi._paramInOutType) .append("<BR>");
//			sb.append("<B>Type:</B> ")       .append(pi._paramType)      .append("<BR>");
//			sb.append("<B>Length:</B> ")     .append(pi._paramLength)    .append("<BR>");
//			sb.append("<B>Is Nullable:</B> ").append(pi._paramIsNullable).append("<BR>");
//			sb.append("<B>Pos:</B> ")        .append(pi._paramPos)       .append("<BR>");
//			sb.append("<B>Default:</B> ")    .append(pi._paramDefault)   .append("<BR>");
//			sb.append("<HR>");
//			sb.append("-end-<BR><BR>");
//			
//			return sb.toString();
//		}
//
//		public ProcedureParameterInfo getParameterInfo(String colname)
//		{
//			ProcedureParameterInfo ci = null;
//			for (ProcedureParameterInfo e : _parameters)
//			{
//				if (colname.equalsIgnoreCase(e._paramName))
//				{
//					ci = e;
//					break;
//				}
//			}
//			return ci;
//		}
//
//		public String getParamDdlDesc(String paramName)
//		{
//			ProcedureParameterInfo pi = getParameterInfo(paramName);
//			if (pi == null)
//				return "Parameter name '"+paramName+"', was not found in procedure '"+_procName+"'.";
//
//			String nulls    = pi._paramIsNullable == DatabaseMetaData.columnNoNulls ? "<b>NOT</b> NULL" : "    NULL";
//			String datatype = pi._paramType;
//
//			// Compose data type
//			String dtlower = datatype.toLowerCase();
//			if ( dtlower.equals("char") || dtlower.equals("varchar") )
//				datatype = datatype + "(" + pi._paramLength + ")";
//			
//			if ( dtlower.equals("numeric") || dtlower.equals("decimal") )
//				datatype = datatype + "(" + pi._paramLength + "," + pi._paramScale + ")";
//
//			return datatype + " " + nulls;
//		}
//
//		public String getColDescription(String paramName)
//		{
//			ProcedureParameterInfo pi = getParameterInfo(paramName);
//			if (pi == null)
//				return "Column name '"+paramName+"', was not found in table '"+_procName+"'.";
//
//			if (StringUtil.isNullOrBlank(pi._paramRemark))
////				return "No Description";
//				return "";
//			return pi._paramRemark;
//		}
//	}



//	/**
//	 * Helper class to put in a object name, and get all the individual parts.
//	 * @author gorans
//	 *
//	 */
//	private static class SqlObjectName
//	{
//		public String _fullName  = "";
//		public String _catName   = "";
//		public String _schName   = "";
//		public String _objName   = "";
//		
//		public String _originFullName = "";
//		public String _originCatName  = "";
//		public String _originSchName  = "";
//		public String _originObjName  = "";
//		
//		public String getFullName   ()       { return _fullName; }
//		public String getCatalogName()       { return _catName; }
//		public String getSchemaName ()       { return _schName; }
//		public String getObjectName ()       { return _objName; }
//
//		public String getOriginFullName   () { return _originFullName; }
//		public String getOriginCatalogName() { return _originCatName; }
//		public String getOriginSchemaName () { return _originSchName; }
//		public String getOriginObjectName () { return _originObjName; }
//
//		/** 
//		 * constructor using full name [catalog.][schema.][object] 
//		 */
//		public SqlObjectName(final String name)
//		{
//			setFullName(name);
//		}
//
//		/**
//		 * Set the fullname, which will be parsed to set all the individual parts<br>
//		 * <br>
//		 * Strip out quote characters and square brackets at start/end of the 
//		 * string '"name"' and '[name]' will be 'name' <br>
//		 * <br>
//		 * The "unstriped" names is available in methods getOrigin{Full|Catalog|Schema|Object}Name()
//		 *
//		 * @param name [catalog.][schema.][object]
//		 */
//		public void setFullName   (String name) 
//		{ 
//			// Dont need to continue if it's empty...
//			if (StringUtil.isNullOrBlank(name))
//				return;
//
//			_originFullName = name;
//			_originCatName  = "";
//			_originSchName  = "";
//			_originObjName  = name;
//			
//			int dot1 = name.indexOf('.');
//			if (dot1 >= 0)
//			{
//				_originSchName = name.substring(0, dot1);
//				_originObjName = name.substring(dot1+1);
//
//				int dot2 = name.indexOf('.', dot1+1);
//				if (dot2 >= 0)
//				{
//					_originCatName = name.substring(0, dot1);
//					_originSchName = name.substring(dot1+1, dot2);
//					_originObjName = name.substring(dot2+1);
//				}
//			}
//			
//			// in some cases check schema/owner name
//			if (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(_dbProductName) || DbUtils.DB_PROD_NAME_MSSQL.equals(_dbProductName))
//			{
//				// if empty schema/owner, add 'dbo'
//				if (StringUtil.isNullOrBlank(_originSchName))
//					_originSchName = "dbo";
//			}
//			
//			_fullName = stripQuote( _originFullName );
//			setCatalogName(_originCatName);
//			setSchemaName (_originSchName);
//			setObjectName (_originObjName);
//		}
//
//		/**
//		 * Set the catalog name<br>
//		 * <br>
//		 * Strip out quote characters and square brackets at start/end of the 
//		 * string '"name"' and '[name]' will be 'name' <br>
//		 * <br>
//		 * The "unstriped" names is available in methods getOriginCatalogName()
//		 *
//		 * @param name catalog name
//		 */
//		public void setCatalogName(String name) 
//		{
//			_originCatName = name;
//			_catName       = stripQuote( name  );
//		}
//
//		/**
//		 * Set the schema name<br>
//		 * <br>
//		 * Strip out quote characters and square brackets at start/end of the 
//		 * string '"name"' and '[name]' will be 'name' <br>
//		 * <br>
//		 * The "unstriped" names is available in methods getOriginSchemaName()
//		 *
//		 * @param name schema name
//		 */
//		public void setSchemaName (String name) 
//		{
//			_originSchName = name;
//			_schName       = stripQuote( name  );
//		}
//
//		/**
//		 * Set the object name<br>
//		 * <br>
//		 * Strip out quote characters and square brackets at start/end of the 
//		 * string '"name"' and '[name]' will be 'name' <br>
//		 * <br>
//		 * The "unstriped" names is available in methods getOriginObjectName
//		 *
//		 * @param name object name
//		 */
//		public void setObjectName (String name) 
//		{
//			_originObjName = name;
//			_objName       = stripQuote( name  );
//		}
//
////		/** make: schemaName -> catalaogName and objectName -> schemaName and blank-out objectName */
////		public void shiftLeft()
////		{
////			_originCatName = _originSchName;
////			_originSchName = _originObjName;
////			_originObjName = "";
////
////			_catName = _schName;
////			_schName = _objName;
////			_objName = "";
////		}
//
//		public boolean hasCatalogName() { return ! StringUtil.isNullOrBlank(_catName); }
//		public boolean hasSchemaName()  { return ! StringUtil.isNullOrBlank(_schName); }
//		public boolean hasObjectName()  { return ! StringUtil.isNullOrBlank(_objName); }
//
//		/** true if it has CatalogName and SchemaName and ObjectName
//		 * @return hasCatalogName() && hasScemaName() */
//		public boolean isFullyQualifiedObject()  { return hasCatalogName() && hasSchemaName(); }
//		
//		/** true if it has schemaName and objectName, but NOT catalogName <br>
//		 *  @return !hasCatalogName() && hasScemaName() */
//		public boolean isSchemaQualifiedObject()  { return !hasCatalogName() && hasSchemaName(); }
//		
//		/** true if it has objectName, but NOT catalogName and schemaName <br>
//		 *  @return !hasCatalogName() && !hasScemaName() */
//		public boolean isSimpleQualifiedObject()  { return !hasCatalogName() && !hasSchemaName(); }
//		
//		@Override
//		public String toString() 
//		{
//			return super.toString() + " catName='"+_catName+"', schName='"+_schName+"', objName='"+_objName+"', isFullyQualifiedObject="+isFullyQualifiedObject()+", isSchemaQualifiedObject="+isSchemaQualifiedObject()+", isSimpleQualifiedObject="+isSimpleQualifiedObject()+".";
//		}
//	}


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

		SqlObjectName sqlObj = new SqlObjectName(word, _dbProductName, _dbIdentifierQuoteString);

		// For completion, lets not assume "dbo"
		String schemaName = sqlObj.getSchemaName();
		if ("dbo".equals(schemaName))
			schemaName = "";

		DbInfo        dbInfo    = getDbInfo(             sqlObj.getObjectName());
		TableInfo     tabInfo   = getTableInfo(          sqlObj.getCatalogName(), schemaName, sqlObj.getObjectName(), true);
		ProcedureInfo procInfo  = getProcedureInfo(      sqlObj.getCatalogName(), schemaName, sqlObj.getObjectName(), true);
		ProcedureInfo sProcInfo = getSystemProcedureInfo(sqlObj.getCatalogName(), schemaName, sqlObj.getObjectName(), true);

//System.out.println("dbInfo="+dbInfo);
//System.out.println("tabInfo="+tabInfo);
//System.out.println("procInfo="+procInfo);
//System.out.println("sProcInfo="+sProcInfo);

		StringBuilder sb = new StringBuilder();
		if (dbInfo    != null) sb.append(dbInfo   .toHtmlString());
		if (tabInfo   != null) sb.append(tabInfo  .toHtmlString());
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

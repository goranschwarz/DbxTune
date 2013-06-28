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

import javax.swing.text.JTextComponent;

import org.apache.log4j.Logger;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.autocomplete.Util;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;

import com.asetune.gui.ConnectionDialog;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.StringUtil;

public abstract class CompletionProviderAbstractSql
extends CompletionProviderAbstract
{
	private static Logger _logger = Logger.getLogger(CompletionProviderAbstractSql.class);

	public CompletionProviderAbstractSql(Window owner, ConnectionProvider connectionProvider)
	{
		super(owner, connectionProvider);
	}
	/** List some known DatabaseProductName that we can use here */
	public static String DB_PROD_NAME_ASE = "Adaptive Server Enterprise";
	public static String DB_PROD_NAME_ASA = "SQL Anywhere";
	public static String DB_PROD_NAME_H2  = "H2";

//	private final CompletionProviderAbstractSql _thisBaseClass = this;

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

	public void addSqlCompletion(Completion c)
	{
		super.addCompletion(c);
	}
	public void addSqlCompletions(List<SqlTableCompletion> list)
	{
		super.addCompletions(list);
	}

	/**
	 * Returns whether the specified token is a single non-word char (e.g. not
	 * in <tt>[A-Za-z]</tt>.  This is a HACK to work around the fact that many
	 * standard token makers return things like semicolons and periods as
	 * {@link Token#IDENTIFIER}s just to make the syntax highlighting coloring
	 * look a little better.
	 * 
	 * @param t The token to check.  This cannot be <tt>null</tt>.
	 * @return Whether the token is a single non-word char.
	 */
	private static final boolean isNonWordChar(Token t) 
	{
		return t.textCount==1 && !RSyntaxUtilities.isLetter(t.text[t.textOffset]);
	}
	/**
	 * Returns whether the specified token is a type that we can do a
	 * "mark occurrences" on.
	 *
	 * @param t The token.
	 * @return Whether we should mark all occurrences of this token.
	 */
	private boolean isValidType(RSyntaxTextArea textArea, Token t) 
	{
		TokenMaker tmaker = TokenMakerFactory.getDefaultInstance().getTokenMaker(textArea.getSyntaxEditingStyle());
		return tmaker.getMarkOccurrencesOfTokenType(t.type);
	}


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

	@Override
	protected List<Completion> getCompletionsImpl(JTextComponent comp)
	{
//System.out.println("SQL: getCompletionsImpl()");

		RSyntaxTextArea textArea = (RSyntaxTextArea)comp;

		setCharsAllowedInWordCompletion("_.*/");

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

//		try
//		{
//			SqlDocument doc = SqlDocument.parseDocument(allText);
//			ISqlItem si = doc.getRootItem();
//			System.out.println("xxx="+si);
//			System.out.println("yyy="+si.getSqlItemType());
//			System.out.println("zzz="+si.getSqlText());
//
//			List<ISqlStatement> xxx = doc.getStatementsReadonly();
//			for (ISqlStatement ss : xxx)
//			{
//				System.out.println("SS.sql="+ss.getSqlText());
//				System.out.println("SS.type="+ss.getSqlItemType());
//				System.out.println("SS.GroupType="+ss.getSqlItemGroupType());
//
//				System.out.println("objType="+ss.getClass().getName());
//				if (ss.getSqlItemType() == SqlItemType.SqlSelectStatement)
//				{
//					SqlSelectStatement xx = (SqlSelectStatement) ss;
//					List<SqlTable> tabList = xx.getTablesReadonly();
//					for (SqlTable sqlTable : tabList)
//					{
//						System.out.println("TAB.db="+sqlTable.getDatabase());
//						System.out.println("TAB.name="+sqlTable.getName());
//						System.out.println("TAB.alias="+sqlTable.getAlias());
//						System.out.println("TAB,index="+sqlTable.getIndex());
//						System.out.println("---------------------------------");
//					}
//				}
//				for (SqlAttribute attr : ss.getAttributesReadonly())
//				{
//					System.out.println("ATTR="+attr.getName());
//					System.out.println("ATTR="+attr);
//				}
//			}
//		}
//		catch (SqlParseException pe)
//		{
//			pe.printStackTrace();
//		}
		
//		// Don't do anything if they are selecting text.
//		Caret caret = textArea.getCaret();
////		if (c.getDot()!=c.getMark()) {
////			return;
////		}
//		RSyntaxDocument doc = (RSyntaxDocument)comp.getDocument();
//		//long time = System.currentTimeMillis();
//		doc.readLock();
//		try {
//	
//			// Get the token at the caret position.
//			int line = textArea.getCaretLineNumber();
//			Token tokenList = textArea.getTokenListForLine(line);
//			int dot = caret.getDot();
//			Token t = RSyntaxUtilities.getTokenAtOffset(tokenList, dot);
//System.out.println("0: Line="+line+", dot="+dot+", Token.toString(): '"+(t==null ? "NULL" : t.toString())+"'.");
//			if (t==null /* EOL */ || !isValidType(textArea, t) || isNonWordChar(t)) {
//				// Try to the "left" of the caret.
//				dot--;
//				try {
//					if (dot>=textArea.getLineStartOffset(line)) {
//						t = RSyntaxUtilities.getTokenAtOffset(tokenList, dot);
//					}
//				} catch (BadLocationException ble) {
//					ble.printStackTrace(); // Never happens
//				}
//			}
//			System.out.println("1: Line="+line+", dot="+dot+", Token.toString(): '"+(t==null ? "NULL" : t.toString())+"'.");
//	
//			// Add new highlights if an identifier is selected.
//			if (t!=null && isValidType(textArea, t) && !isNonWordChar(t)) {
////				removeHighlights();
////				RSyntaxTextAreaHighlighter h = (RSyntaxTextAreaHighlighter) textArea.getHighlighter();
//				char[] lexeme = t.getLexeme().toCharArray();
//				int type = t.type;
//				for (int i=0; i<textArea.getLineCount(); i++) {
//					Token temp = textArea.getTokenListForLine(i);
//					System.out.println("ForLine: "+i+", Token.toString(): '"+temp.toString()+"'.");
//					while (temp!=null && temp.isPaintable()) {
//						if (temp.is(type, lexeme)) {
//							System.out.println("xxxxxxx: Token.toString(): '"+temp.toString()+"'.");
////							try {
//								int end = temp.offset + temp.textCount;
////								h.addMarkedOccurrenceHighlight(temp.offset, end, p);
////							} catch (BadLocationException ble) {
////								ble.printStackTrace(); // Never happens
////							}
//						}
//						temp = temp.getNextToken();
//					}
//				}
//	//textArea.repaint();
//	//TODO: Do a textArea.repaint() instead of repainting each marker as it's added if count is huge
//			}
//	
//		} finally {
//			doc.readUnlock();
//			//time = System.currentTimeMillis() - time;
//			//System.out.println("MarkOccurrencesSupport took: " + time + " ms");
//		}

//		System.out.println("XXXXXXXXXX: "+textArea.getLineCount());
//
//		Caret caret = textArea.getCaret();
//		RSyntaxDocument doc = (RSyntaxDocument)comp.getDocument();
//		//long time = System.currentTimeMillis();
//		doc.readLock();
//		try 
//		{
//			for (int row=0; row<textArea.getLineCount(); row++)
//			{
//				System.out.println("###################");
////				for (Token tok = tokenList.getNextToken(); tok != null; tok = tok.getNextToken())
//				for (Token t = textArea.getTokenListForLine(row); t != null; t = t.getNextToken())
//				{
//					if (t.type != TokenTypes.NULL)
//					{
//						System.out.println("Line: "+row+", offs="+t.offset+", type='"+AsetuneTokenMaker.getTokenString(t.type)+"', token.getLexeme='"+t.getLexeme()+"'.");
//					}
//				}
//			}
//	
//		} 
//		finally 
//		{
//			doc.readUnlock();
//		}

//		System.out.println("!!!! PARSE(General SQL Parser) BEGIN !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//		System.out.println("Parser License: "+TGSqlParser.getLicenseType());
//		System.out.println("Parser License Message: "+TGSqlParser.getLicenseMessage());
//
//		TGSqlParser sqlparser = new TGSqlParser(EDbVendor.dbvsybase);
//		sqlparser.setSqltext(textArea.getText());
////		if (textArea instanceof TextEditorPane)
////			sqlparser.setSqlfilename(((TextEditorPane)textArea).getFileFullPath());
//		int parseRc = sqlparser.parse();
//
//		findXxx(sqlparser, caret.getDot());
//        
//		if (parseRc == 0)
//		{
//			System.out.println("parseRc=0: ---OK----");
//            for(int i=0;i<sqlparser.sqlstatements.size();i++)
//                iterateStmt(sqlparser.sqlstatements.get(i));
//
////			sl.get
////			WhereCondition w = new WhereCondition(sqlparser.sqlstatements.get( 0 ).getWhereClause( ).getCondition( ));
////			w.printColumn();
//		}
//		else
//		{
//			System.out.println("parseRc="+parseRc+": ################################### FAIL FAIL FAIL ###################");
//			System.out.println(sqlparser.getErrormessage( ));
//            for(int i=0;i<sqlparser.sqlstatements.size();i++)
//                iterateStmt(sqlparser.sqlstatements.get(i));
//		}
//		System.out.println("!!!! PARSE(General SQL Parser) END !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		
		
//		System.out.println("!!!! PARSE(JSqlParser) BEGIN !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//		CCJSqlParserManager pm = new CCJSqlParserManager();
//		String sql = "SELECT * FROM MY_TABLE1, MY_TABLE2, (SELECT * FROM MY_TABLE3) LEFT OUTER JOIN MY_TABLE4 "+
//		" WHERE ID = (SELECT MAX(ID) FROM MY_TABLE5) AND ID2 IN (SELECT * FROM MY_TABLE6)" ;
//		sql = textArea.getText();
//		try
//		{
//			net.sf.jsqlparser.statement.Statement statement = pm.parse(new StringReader(sql));
//			if (statement instanceof Select) 
//			{
//				Select selectStatement = (Select) statement;
//				TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
//				List<String> tableList = tablesNamesFinder.getTableList(selectStatement);
//				for (String tableName : tableList)
//				{
//					System.out.println(tableName);
//				}
////				for (Iterator iter = tableList.iterator(); iter.hasNext();) {
////					System.out.println(tableName);
////				}
//			}
//		}
//		catch (JSQLParserException e)
//		{
//			e.printStackTrace();
//		}
////		System.out.println("!!!! PARSE(JSqlParser) END !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");


		if (needRefresh())
		{
			// Clear old completions
			clear();
//			super.completions.clear();

			_schemaNames.clear();

			// Restore the "saved" completions
//			for (Completion c : _savedComplitionList)
//				super.addCompletion(c);
//			super.addCompletions(_savedComplitionList);
			super.addCompletions(getStaticCompletions());

			// DO THE REFRESH
			List<SqlTableCompletion> list = refreshCompletion();

			if (list != null && list.size() > 0)
			{
				// Add the completion list
//				for (SqlTableCompletion entry : list)
//					addSqlCompletion(entry);
				addSqlCompletions(list);

				setNeedRefresh(false);
			}
		} // end _needRefresh


		//-----------------------------------------------------------
		// Complete DATABASES
		//
		if ("use".equalsIgnoreCase(prevWord1))
		{
			ArrayList<Completion> dbList = new ArrayList<Completion>();

//System.out.println("getCompletionsImpl(): USE completion...");
			for (SqlDbCompletion dc : _dbComplList)
			{
				DbInfo di = dc._dbInfo;
				if (Util.startsWithIgnoreCase(di._dbName, enteredText))
					dbList.add(dc);
			}
			return dbList;
		}

		//-----------------------------------------------------------
		// Complete STORED PROCS, if the previous word is EXEC
		// if enteredText also starts with "sp_", then do procedure lookup
		//
//		if ( "exec".equalsIgnoreCase(prevWord1) || "execute".equalsIgnoreCase(prevWord1) || enteredText.startsWith("sp_") )
		if ( "exec".equalsIgnoreCase(prevWord1) || "execute".equalsIgnoreCase(prevWord1) )
		{
//System.out.println("getCompletionsImpl(): EXEC completion");
			ArrayList<Completion> procList = new ArrayList<Completion>();

//System.out.println("getCompletionsImpl(): EXEC completion... USER PROC.");
			for (SqlProcedureCompletion pc : _procedureComplList)
			{
				ProcedureInfo pi = pc._procInfo;
				if (startsWithIgnoreCaseOrRegExp(pi._procName, enteredText))
					procList.add(pc);
				
			}
			
//System.out.println("getCompletionsImpl(): EXEC completion... SYSTEM PROC.");
			if (startsWithIgnoreCaseOrRegExp(currentWord, "sp_"))
			{
				for (SqlProcedureCompletion pc : _systemProcComplList)
				{
					ProcedureInfo pi = pc._procInfo;
					if (startsWithIgnoreCaseOrRegExp(pi._procName, enteredText))
						procList.add(pc);
				}
			}
			return procList;
		}

		//-----------------------------------------------------------
		// Check for column name completion
		// This is if the current word has a '.' in it
		//
		if (enteredText.indexOf('.') >= 0)
		{
//System.out.println("getCompletionsImpl(): in COLUMN completion: ENTERED");
			int lastDot = enteredText.lastIndexOf('.');
			String colName      = enteredText.substring(lastDot+1);
			String tabAliasName = enteredText.substring(0, lastDot);
			while(tabAliasName.indexOf('.') >= 0)
				tabAliasName = tabAliasName.substring(tabAliasName.lastIndexOf('.')+1);

//System.out.println("getCompletionsImpl(): in COLUMN completion: colName='"+colName+"', tabAliasName='"+tabAliasName+"'. _schemaNames="+_schemaNames);
			// If the "alias" name (word before the dot) is in any of the schema names
			// then continue with normal completion.
			if ( _schemaNames.contains(tabAliasName) )
			{
//System.out.println("getCompletionsImpl(): in column compl: do normal completion");
				String lastPart = colName;
				// do normal completion
//				return getCompletionsSimple(comp, lastPart);
				return getCompletionsFrom(completions, lastPart);
			}
			else // alias is NOT in the schemas
			{
	//			System.out.println("tabAliasName='"+tabAliasName+"'.");
	//			System.out.println("colName     ='"+colName+"'.");
	
				String tabName = getTableNameForAlias(comp, tabAliasName, true);
	//			System.out.println(" >>> tabName='"+tabName+"'.");

//System.out.println("getCompletionsImpl(): in COLUMN completion: tabName='"+tabName+"'.");
				ArrayList<Completion> colList = new ArrayList<Completion>();

				Completion c = null;
				for (Completion ce : _tableComplList)
				{
					if (tabName.equalsIgnoreCase(ce.getInputText()))
					{
						c = ce;
						break;
					}
				}
				
//				// find table name if this is a alias.
//				Completion c = null;
////				int index = Collections.binarySearch(completions, tabName, comparator);
//				int index = Collections.binarySearch(_tableComplList, tabName, comparator);
//				if (index > 0)
//					c = (Completion) _tableComplList.get(index);
//System.out.println("getCompletionsImpl(): in COLUMN completion: index='"+index+"', c="+c);
//System.out.println("getCompletionsImpl(): in COLUMN completion: TABLE COMPLETIONS");
//for (Completion ccc : _tableComplList)
//{
//	System.out.println("ENTRY: getInputText='"+ccc.getInputText()+"'.");
//}
	
				if (c != null && c instanceof SqlTableCompletion)
				{
					SqlTableCompletion jdbcCompl = (SqlTableCompletion) c;
					for (TableColumnInfo tci : jdbcCompl._tableInfo._columns)
					{
						if (Util.startsWithIgnoreCase(tci._colName, colName))
//							colList.add( new SqlColumnCompletion(_thisBaseClass, tabAliasName, tci._colName, jdbcCompl._tableInfo));
							colList.add( new SqlColumnCompletion(CompletionProviderAbstractSql.this, tabAliasName, tci._colName, jdbcCompl._tableInfo));
					}
				}
	//			else
	//			{
	//				colList.add( new ShorthandCompletion(thisJdbcCompleationProvider, "not_found", "not_found", "Table '"+tabName+"' not found.", "<html>The table name '"+tabName+"' can't be found in the internal lookup dictionary.</html>"));
	//			}
	
				return colList;
			}
		}

//		return super.getCompletionsImpl(comp);
//		return getCompletionsSimple(comp, enteredText);
		
		// completions is defined in org.fife.ui.autocomplete.AbstractCompletionProvider
		return getCompletionsFrom(completions, enteredText);
	}

//	private List getCompletionsSimple(JTextComponent comp, String text)
//	{
//		List retList = new ArrayList();
////		if (text == null)
////			text = getAlreadyEnteredText(comp);
//
//		if ( text != null )
//		{
//			for (Object o : completions)
//			{
//				if (o instanceof Completion)
//				{
//					Completion c = (Completion) o;
//					if (Util.startsWithIgnoreCase(c.getInputText(), text))
//						retList.add(c);
//				}
//			}
//		}
//
//		return retList;
//	}

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
						if (sa2[1].equals(alias))
						{
							table = sa2[0];
							if (table.indexOf('.') >= 0)
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

		// Obtain a DatabaseMetaData object from our current connection        
		DatabaseMetaData dbmd = conn.getMetaData();

		// What PRODUCT are we connected to 
		String dbProductName = dbmd.getDatabaseProductName();
		
		if (DB_PROD_NAME_H2.equals(dbProductName))
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

	protected List<DbInfo> refreshCompletionForDbs(Connection conn, WaitForExecDialog waitDialog)
	throws SQLException
	{
//System.out.println("SQL: refreshCompletionForDbs()");
		waitDialog.setState("Getting Database information");

		ArrayList<DbInfo> dbInfoList = new ArrayList<DbInfo>();

		// Obtain a DatabaseMetaData object from our current connection        
		DatabaseMetaData dbmd = conn.getMetaData();

		ResultSet rs = dbmd.getCatalogs();
		while(rs.next())
		{
			DbInfo di = new DbInfo();

			di._dbName = rs.getString("TABLE_CAT");

			dbInfoList.add(di);
		}
		return dbInfoList;
	}

	protected List<TableInfo> refreshCompletionForTables(Connection conn, WaitForExecDialog waitDialog)
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

		waitDialog.setState("Getting Table information");

		ArrayList<TableInfo> tableInfoList = new ArrayList<TableInfo>();

		if (waitDialog.wasCancelPressed())
			return tableInfoList;

		ResultSet rs = dbmd.getTables(null, null, "%", null);
		while(rs.next())
		{
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

	protected void refreshCompletionForTableColumns(Connection conn, WaitForExecDialog waitDialog, List<TableInfo> tableInfoList)
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

		boolean bulkGetColumns = true;

		if (bulkGetColumns)
		{
			waitDialog.setState("Getting Column information for ALL tables.");
	
			String prevTabName = "";
			TableInfo tabInfo = null;
	
			// Obtain a DatabaseMetaData object from our current connection
			ResultSet rs = dbmd.getColumns(null, null, "%", "%");
			while(rs.next())
			{
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
		
//		// ADD Column information
//		for (TableInfo ti : tableInfoList)
//		{
//			if (waitDialog.wasCancelPressed())
//				return;
//
//			// Retrieves a description of table columns available in the specified catalog. 
//			// Only column descriptions matching the catalog, schema, table and column name criteria are returned. They are ordered by TABLE_CAT,TABLE_SCHEM, TABLE_NAME, and ORDINAL_POSITION. 
//			// 
//			// Each column description has the following columns: 
//			// TABLE_CAT         String => table catalog (may be null) 
//			// TABLE_SCHEM       String => table schema (may be null) 
//			// TABLE_NAME        String => table name 
//			// COLUMN_NAME       String => column name 
//			// DATA_TYPE         int    => SQL type from java.sql.Types 
//			// TYPE_NAME         String => Data source dependent type name, for a UDT the type name is fully qualified 
//			// COLUMN_SIZE       int    => column size. 
//			//                                  BUFFER_LENGTH is not used. 
//			// DECIMAL_DIGITS    int    => the number of fractional digits. Null is returned for data types where DECIMAL_DIGITS is not applicable. 
//			// NUM_PREC_RADIX    int    => Radix (typically either 10 or 2) 
//			// NULLABLE          int    => is NULL allowed. 
//			//                                  columnNoNulls - might not allow NULL values 
//			//                                  columnNullable - definitely allows NULL values 
//			//                                  columnNullableUnknown - nullability unknown 
//			// REMARKS           String => comment describing column (may be null) 
//			// COLUMN_DEF        String => default value for the column, which should be interpreted as a string when the value is enclosed in single quotes (may be null) 
//			// SQL_DATA_TYPE     int    => unused 
//			// SQL_DATETIME_SUB  int    => unused 
//			// CHAR_OCTET_LENGTH int    => for char types the maximum number of bytes in the column 
//			// ORDINAL_POSITION  int    => index of column in table (starting at 1) 
//			// IS_NULLABLE       String => ISO rules are used to determine the nullability for a column. 
//			//                                  YES --- if the parameter can include NULLs 
//			//                                  NO --- if the parameter cannot include NULLs 
//			//                                  empty string --- if the nullability for the parameter is unknown 
//			// SCOPE_CATLOG      String => catalog of table that is the scope of a reference attribute (null if DATA_TYPE isn't REF) 
//			// SCOPE_SCHEMA      String => schema of table that is the scope of a reference attribute (null if the DATA_TYPE isn't REF) 
//			// SCOPE_TABLE       String => table name that this the scope of a reference attribure (null if the DATA_TYPE isn't REF) 
//			// SOURCE_DATA_TYPE  short  => source type of a distinct type or user-generated Ref type, SQL type from java.sql.Types (null if DATA_TYPE isn't DISTINCT or user-generated REF) 
//			// IS_AUTOINCREMENT  String => Indicates whether this column is auto incremented 
//			//                                  YES --- if the column is auto incremented 
//			//                                  NO --- if the column is not auto incremented 
//			//                                  empty string --- if it cannot be determined whether the column is auto incremented parameter is unknown 
//			//                                  The COLUMN_SIZE column the specified column size for the given column. For numeric data, this is the maximum precision. For character data, this is the length in characters. For datetime datatypes, this is the length in characters of the String representation (assuming the maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes. For the ROWID datatype, this is the length in bytes. Null is returned for data types where the column size is not applicable.
//			// 
//			// Parameters:
//			// - catalog a catalog name; must match the catalog name as it is stored in the database; "" retrieves those without a catalog; null means that the catalog name should not be used to narrow the search
//			// - schemaPattern a schema name pattern; must match the schema name as it is stored in the database; "" retrieves those without a schema; null means that the schema name should not be used to narrow the search
//			// - tableNamePattern a table name pattern; must match the table name as it is stored in the database 
//			// - columnNamePattern a column name pattern; must match the column name as it is stored in the database
//
//			waitDialog.setState("Getting Column information for table '"+ti._tabName+"'.");
//
//			ResultSet rs = dbmd.getColumns(null, null, ti._tabName, "%");
//			while(rs.next())
//			{
//				TableColumnInfo ci = new TableColumnInfo();
//				ci._colName       = rs.getString("COLUMN_NAME");
//				ci._colPos        = rs.getInt   ("ORDINAL_POSITION");
//				ci._colType       = rs.getString("TYPE_NAME");
//				ci._colLength     = rs.getInt   ("COLUMN_SIZE");
//				ci._colIsNullable = rs.getInt   ("NULLABLE");
//				ci._colRemark     = rs.getString("REMARKS");
//				ci._colDefault    = rs.getString("COLUMN_DEF");
//				ci._colScale      = rs.getInt   ("DECIMAL_DIGITS");
//				
//				ti.addColumn(ci);
//			}
//			rs.close();
//
//			// PK INFO
////			rs = dbmd.getPrimaryKeys(null, null, ti._tabName);
////			ResultSetTableModel rstm = new ResultSetTableModel(rs);
////			System.out.println("######### PK("+ti._tabName+"):--------\n" + rstm.toTableString());
//////			while(rs.next())
//////			{
//////				TablePkInfo pk = new TablePkInfo();
//////				pk._colName       = rs.getString("COLUMN_NAME");
//////				
//////				ti.addPk(pk);
//////			}
////			rs.close();
//			
//			// INDEX INFO
////			rs = dbmd.getIndexInfo(null, null, ti._tabName, false, false);
////			rstm = new ResultSetTableModel(rs);
////			System.out.println("########## INDEX("+ti._tabName+"):--------\n" + rstm.toTableString());
//////			while(rs.next())
//////			{
//////				TableIndexInfo index = new TableIndexInfo();
//////				index._colName       = rs.getString("COLUMN_NAME");
//////				
//////				ti.addIndex(index);
//////			}
////			rs.close();
//
//		} // end: for (TableInfo ti : tableInfoList)


		
//		// What PRODUCT are we connected to 
//		String dbProductName = dbmd.getDatabaseProductName();
//
//		if (DB_PROD_NAME_ASE.equals(dbProductName))
//		{
//			// if ASE, check if we have monrole
//			boolean hasMonRole = AseConnectionUtils.hasRole(conn, AseConnectionUtils.MON_ROLE);
//			int aseVersionNum  = AseConnectionUtils.getAseVersionNumber(conn);
//
//			if (hasMonRole)
//			{
//				for (TableInfo ti : tableInfoList)
//				{
//					// update DESCRIPTION for MDA Columns
//					// is ASE, db=master, table mon*
//					if (DB_PROD_NAME_ASE.equals(dbProductName) && "master".equals(ti._tabCat) && ti._tabName.startsWith("mon") )
//					{
//						waitDialog.setState("Getting MDA Column Description");
//						
//						//String sql = "select ColumnName, TypeName, Length, Description, Precision, Scale from master.dbo.monTableColumns where TableName = '"+ti._tabName+"'";
//						String sql = "select ColumnName, Description from master.dbo.monTableColumns where TableName = '"+ti._tabName+"'";
//						if (aseVersionNum >= 15700)
//							sql += " and Language = 'en_US' ";
//	
//						try
//						{
//							Statement stmnt = conn.createStatement();
//							ResultSet rs = stmnt.executeQuery(sql);
//							while(rs.next())
//							{
//								String colname = rs.getString(1);
//								String colDesc = rs.getString(2);
//	
//								TableColumnInfo ci = ti.getColumnInfo(colname);
//								if (ci != null)
//									ci._colRemark = colDesc;
//							}
//							rs.close();
//							stmnt.close();
//						}
//						catch (SQLException sqle)
//						{
//							_logger.info("Problems when getting ASE monTableColumns dictionary, skipping this and continuing. Caught: "+sqle);
//						}
//					}
//				}
//			} // end: hasMonRole
//		} // end: is ASE
	}

	protected List<ProcedureInfo> refreshCompletionForProcedures(Connection conn, WaitForExecDialog waitDialog)
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

		waitDialog.setState("Getting Procedure information");

		ArrayList<ProcedureInfo> procInfoList = new ArrayList<ProcedureInfo>();

		if (waitDialog.wasCancelPressed())
			return procInfoList;

		ResultSet rs = dbmd.getProcedures(null, null, "%");
		while(rs.next())
		{
			ProcedureInfo pi = new ProcedureInfo();
			pi._procCat     = rs.getString("PROCEDURE_CAT");
			pi._procSchema  = rs.getString("PROCEDURE_SCHEM");
			pi._procName    = rs.getString("PROCEDURE_NAME");
			pi._procType    = rs.getString("PROCEDURE_TYPE");
			pi._procRemark  = rs.getString("REMARKS");

			procInfoList.add(pi);
			
			if (waitDialog.wasCancelPressed())
				return procInfoList;
		}
		rs.close();
		
		return procInfoList;
	}

	protected void refreshCompletionForProcedureParameters(Connection conn, WaitForExecDialog waitDialog, List<ProcedureInfo> procedureInfoList)
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

		boolean getBulk = true;

		if (getBulk)
		{
			waitDialog.setState("Getting Procedure Parameter information for ALL procedures");

			String prevProcName = "";
			ProcedureInfo procInfo = null;
			int colId = 0;

			// Get params for all procs
			ResultSet rs = dbmd.getProcedureColumns(null, null, "%", "%");
			while(rs.next())
			{
				colId++;
	//			String procCatalog = rs.getString("PROCEDURE_CAT");
	//			String procSchema  = rs.getString("PROCEDURE_SCHEM");
				String procName    = rs.getString("PROCEDURE_NAME");
				
				ProcedureParameterInfo pi = new ProcedureParameterInfo();
				pi._paramName       = rs.getString("COLUMN_NAME");
				pi._paramPos        = colId;
				pi._paramType       = rs.getString("TYPE_NAME");
				pi._paramLength     = rs.getInt   ("LENGTH");
				pi._paramIsNullable = rs.getInt   ("NULLABLE");
//				pi._paramRemark     = rs.getString("REMARKS");
//				pi._paramDefault    = rs.getString("COLUMN_DEF");
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
	}

	protected List<ProcedureInfo> refreshCompletionForSystemProcedures(Connection conn, WaitForExecDialog waitDialog)
	throws SQLException
	{
//System.out.println("ASE: refreshCompletionForSystemProcedures()");
		waitDialog.setState("Getting Procedure information");

		ArrayList<ProcedureInfo> procInfoList = new ArrayList<ProcedureInfo>();

		return procInfoList;
	}

	protected void refreshCompletionForSystemProcedureParameters(Connection conn, WaitForExecDialog waitDialog, List<ProcedureInfo> procedureInfoList)
	throws SQLException
	{
//System.out.println("ASE: refreshCompletionForSystemProcedureParameters()");
		waitDialog.setState("Getting Procedure Parameter information");

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

		WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
		{
			// This is the object that will be returned.
			ArrayList<SqlTableCompletion> completionList = new ArrayList<SqlTableCompletion>();
			
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
					long allStartTime  = System.currentTimeMillis();
					long thisStartTime = System.currentTimeMillis();

					thisStartTime = System.currentTimeMillis();
					//----------------------------------------------------------
					// Get Miscelanious Compleation
					getWaitDialog().setState("Creating Miscelanious Completions.");
					refreshCompletionForMisc(conn, getWaitDialog());
//System.out.println("---------------- Refresh Completion: MISC Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));

					
					thisStartTime = System.currentTimeMillis();
					//----------------------------------------------------------
					// Get DB informaation
					_dbInfoList = refreshCompletionForDbs(conn, getWaitDialog());
//System.out.println("---------------- Refresh Completion: DB Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));

					// Create completion list
					getWaitDialog().setState("Creating Database Completions.");
					for (DbInfo di : _dbInfoList)
					{
//						SqlDbCompletion c = new SqlDbCompletion(_thisBaseClass, di);
						SqlDbCompletion c = new SqlDbCompletion(CompletionProviderAbstractSql.this, di);
						_dbComplList.add(c);
					}

					
					thisStartTime = System.currentTimeMillis();
					//----------------------------------------------------------
					// Get Table and Columns informaation
					_tableInfoList = refreshCompletionForTables(conn, getWaitDialog());
//System.out.println("---------------- Refresh Completion: TAB-1 Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));
					refreshCompletionForTableColumns(conn, getWaitDialog(), _tableInfoList);
//System.out.println("---------------- Refresh Completion: TAB-2 Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));

					// Create completion list
					getWaitDialog().setState("Creating Table Completions.");
					for (TableInfo ti : _tableInfoList)
					{
//						SqlTableCompletion c = new SqlTableCompletion(_thisBaseClass, ti, _quoteTableNames);
						SqlTableCompletion c = new SqlTableCompletion(CompletionProviderAbstractSql.this, ti, _quoteTableNames);
						completionList.add(c);
						_tableComplList.add(c);
					}


					thisStartTime = System.currentTimeMillis();
					//----------------------------------------------------------
					// Get USER Procedure and Parameters information
					_procedureInfoList = refreshCompletionForProcedures(conn, getWaitDialog());
//System.out.println("---------------- Refresh Completion: PROC-1 Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));
					refreshCompletionForProcedureParameters(conn, getWaitDialog(), _procedureInfoList);
//System.out.println("---------------- Refresh Completion: PROC-2 Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));

					getWaitDialog().setState("Creating Procedure Completions.");
					for (ProcedureInfo pi : _procedureInfoList)
					{
//						SqlProcedureCompletion c = new SqlProcedureCompletion(_thisBaseClass, pi);
						SqlProcedureCompletion c = new SqlProcedureCompletion(CompletionProviderAbstractSql.this, pi);
						_procedureComplList.add(c);
					}

					thisStartTime = System.currentTimeMillis();
					//----------------------------------------------------------
					// Get SYSTEM Procedure and Parameters information
					// Only do this once
					if (_systemProcInfoList.size() == 0)
					{
						_systemProcInfoList = refreshCompletionForSystemProcedures(conn, getWaitDialog());
//System.out.println("---------------- Refresh Completion: SYS PROC-1 Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));
						refreshCompletionForSystemProcedureParameters(conn, getWaitDialog(), _systemProcInfoList);
//System.out.println("---------------- Refresh Completion: SYS PROC-2 Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-thisStartTime));

						getWaitDialog().setState("Creating System Procedure Completions.");
						for (ProcedureInfo pi : _systemProcInfoList)
						{
//							SqlProcedureCompletion c = new SqlProcedureCompletion(_thisBaseClass, pi);
							SqlProcedureCompletion c = new SqlProcedureCompletion(CompletionProviderAbstractSql.this, pi);
							_systemProcComplList.add(c);
						}
					}

//System.out.println("Refresh Completion: TOTAL Time: "+TimeUtils.msToTimeStr(System.currentTimeMillis()-allStartTime));
				}
				catch (SQLException e)
				{
					_logger.info("Problems reading table information for SQL Table code completion.", e);
				}

				return completionList;
			}
		}; // END: new WaitForExecDialog.BgExecutor()
		
		// Execute and WAIT
		ArrayList<SqlTableCompletion> list = (ArrayList)wait.execAndWait(doWork);

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
	private static String fixStrangeNames(String name)
	{
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

			// if any other chars, then break and signal "non-normal-char" detected
			normalChars = false;
			break;
		}
		return normalChars ? name : "["+name+"]"; 
	}

    /**
	 * Our own Completion class, which overrides toString() to make it HTML aware
	 */
	protected static class SqlDbCompletion
	extends ShorthandCompletion
	{
		private DbInfo _dbInfo = null;
		
		public SqlDbCompletion(CompletionProviderAbstractSql provider, DbInfo di)
		{
			super(provider, di._dbName, fixStrangeNames(di._dbName));

			_dbInfo = di;

			String shortDesc = 
				"<font color=\"blue\">"+di._dbType+"</font>" +
//				" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(di._dbRemark) ? "No Description" : di._dbRemark) + "</font></i>";
				" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(di._dbRemark) ? "" : di._dbRemark) + "</font></i>";
			setShortDescription(shortDesc);
			setSummary(_dbInfo.toHtmlString());
		}

		/**
		 * Make it HTML aware
		 */
		@Override
		public String toString()
		{
			return "<html><body>" + super.toString() + "</body></html>";
		}
	}

    /**
	 * Our own Completion class, which overrides toString() to make it HTML aware
	 */
	protected static class SqlTableCompletion
	extends ShorthandCompletion
	{
		private TableInfo _tableInfo = null;
		
		public SqlTableCompletion(CompletionProviderAbstractSql provider, TableInfo ti, boolean quoteTableNames)
		{
			super(provider, 
				ti._tabName,
				! quoteTableNames 
					? ti._tabSchema+"."+fixStrangeNames(ti._tabName) 
					: "\""+ti._tabSchema+"\".\""+ti._tabName+"\"");

			_tableInfo = ti;

			String shortDesc = 
				"<font color=\"blue\">"+ti._tabType+"</font>" +
//				" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(ti._tabRemark) ? "No Description" : ti._tabRemark) + "</font></i>";
				" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(ti._tabRemark) ? "" : ti._tabRemark) + "</font></i>";
			setShortDescription(shortDesc);
			setSummary(_tableInfo.toHtmlString());
		}

		/**
		 * Make it HTML aware
		 */
		@Override
		public String toString()
		{
			return "<html><body>" + super.toString() + "</body></html>";
		}
	}

    /**
	 * Our own Completion class, which overrides toString() to make it HTML aware
	 */
	protected static class SqlProcedureCompletion
	extends ShorthandCompletion
	{
		private ProcedureInfo _procInfo = null;
		
		public SqlProcedureCompletion(CompletionProviderAbstractSql provider, ProcedureInfo pi)
		{
			super(provider, 
				pi._procName,
				fixStrangeNames(pi._procName));

			_procInfo = pi;

			String shortDesc = 
				"<font color=\"blue\">"+pi._procType+"</font>" +
//				" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(pi._procRemark) ? "No Description" : pi._procRemark) + "</font></i>";
				" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(pi._procRemark) ? "" : pi._procRemark) + "</font></i>";
			setShortDescription(shortDesc);
			setSummary(_procInfo.toHtmlString());
		}

		/**
		 * Make it HTML aware
		 */
		@Override
		public String toString()
		{
			return "<html><body>" + super.toString() + "</body></html>";
		}
	}

    /**
	 * Our own Completion class, which overrides toString() to make it HTML aware
	 */
	protected static class SqlColumnCompletion
	extends ShorthandCompletion
	{
		private TableInfo _tableInfo = null;
		
		public SqlColumnCompletion(CompletionProvider provider, String tabAliasName, String colname, TableInfo tableInfo)
		{
			super(provider, fixStrangeNames(colname), (tabAliasName == null ? colname : tabAliasName+"."+colname));
			_tableInfo = tableInfo;

			TableColumnInfo ci = _tableInfo.getColumnInfo(colname);
			String colPos = "";
			if (ci != null)
				colPos = "pos="+ci._colPos+", ";

			String shortDesc = 
				"<font color=\"blue\">"+_tableInfo.getColDdlDesc(colname)+"</font>" +
				" -- <i><font color=\"green\">" + colPos + _tableInfo.getColDescription(colname) + "</font></i>";
			setShortDescription(shortDesc);
			setSummary(_tableInfo.toHtmlString(colname));
		}

		/**
		 * Make it HTML aware
		 */
		@Override
		public String toString()
		{
			return "<html><body>" + super.toString() + "</body></html>";
		}
	}


	
	
	
	

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
			if (dbName.equals(di._dbName))
				return di;
		}
		return null;
	}

	/**
	 * Holds information about databases
	 */
	protected static class DbInfo
	{
		public String _dbName    = null;
		public String _dbSize    = null;
		public int    _dbId      = -1;
		public String _dbOwner   = null;
		public String _dbCrDate  = null;
		public String _dbType    = null;
		public String _dbRemark  = null;
		
		public String toHtmlString()
		{
			StringBuilder sb = new StringBuilder();
//			sb.append(_tabType).append(" - <B>").append(_tabName).append("</B>");
			sb.append("<B>").append(_dbName).append("</B> - <font color=\"blue\">").append(_dbType).append("</font>");
			sb.append("<HR>");
			sb.append("<BR>");
			sb.append("<B>Description:</B> ").append(StringUtil.isNullOrBlank(_dbRemark) ? "not available" : _dbRemark).append("<BR>");
			sb.append("<B>Size:</B> ")       .append(StringUtil.isNullOrBlank(_dbSize)   ? "not available" : _dbSize)  .append("<BR>");
			sb.append("<B>Owner:</B> ")      .append(StringUtil.isNullOrBlank(_dbOwner)  ? "not available" : _dbOwner) .append("<BR>");
			sb.append("<B>Create Date:</B> ").append(StringUtil.isNullOrBlank(_dbCrDate) ? "not available" : _dbCrDate).append("<BR>");
			sb.append("<B>dbid:</B> ")       .append(_dbId == -1                         ? "not available" : _dbId)    .append("<BR>");
			sb.append("<BR>");
			
			return sb.toString();
		}
	}

	/**
	 * Holds information about columns
	 */
	protected static class TableColumnInfo
	{
		public String _colName       = null;
		public int    _colPos        = -1;
		public String _colType       = null;
//		public String _colType2      = null;
		public int    _colLength     = -1;
		public int    _colIsNullable = -1;
		public String _colRemark     = null;
		public String _colDefault    = null;
//		public int    _colPrec       = -1;
		public int    _colScale      = -1;
	}

	protected TableInfo getTableInfo(String tableName)
	{
		for (TableInfo ti : _tableInfoList)
		{
			if (tableName.equals(ti._tabName))
				return ti;
		}
		return null;
	}

	/**
	 * Holds information about tables
	 */
	protected static class TableInfo
	{
		public String _tabCat     = null;
		public String _tabSchema  = null;
		public String _tabName    = null;
		public String _tabType    = null;
		public String _tabRemark  = null;
		
		public ArrayList<TableColumnInfo> _columns = new ArrayList<TableColumnInfo>();

		public void addColumn(TableColumnInfo ci)
		{
			_columns.add(ci);
		}

		public String toHtmlString()
		{
			StringBuilder sb = new StringBuilder();
//			sb.append(_tabType).append(" - <B>").append(_tabName).append("</B>");
			sb.append("<B>").append(_tabName).append("</B> - <font color=\"blue\">").append(_tabType).append("</font>");
			sb.append("<HR>");
			sb.append("<BR>");
			sb.append("<B>Description:</B> ").append(StringUtil.isNullOrBlank(_tabRemark) ? "not available" : _tabRemark).append("<BR>");
			sb.append("<BR>");
			sb.append("<B>Columns:</B> ").append("<BR>");
			sb.append("<TABLE ALIGN=\"left\" BORDER=0 CELLSPACING=0 CELLPADDING=1\">");
			sb.append("<TR ALIGN=\"left\" VALIGN=\"top\" BGCOLOR=\"#ffffff\">");
			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Name")       .append("</B></FONT></TD>");
			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Datatype")   .append("</B></FONT></TD>");
			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Length")     .append("</B></FONT></TD>");
			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Nulls")      .append("</B></FONT></TD>");
			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Pos")        .append("</B></FONT></TD>");
			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Description").append("</B></FONT></TD>");
			sb.append("</TR>");
			int r=0;
			for (TableColumnInfo ci : _columns)
			{
				r++;
				if ( (r % 2) == 0 )
					sb.append("<TR ALIGN=\"left\" VALIGN=\"top\" BGCOLOR=\"#ffffff\">");
				else
					sb.append("<TR ALIGN=\"left\" VALIGN=\"top\" BGCOLOR=\"#ffffcc\">");
				sb.append("	<TD NOWRAP>").append(ci._colName)      .append("</TD>");
				sb.append("	<TD NOWRAP>").append(ci._colType)      .append("</TD>");
				sb.append("	<TD NOWRAP>").append(ci._colLength)    .append("</TD>");
				sb.append("	<TD NOWRAP>").append(ci._colIsNullable).append("</TD>");
				sb.append("	<TD NOWRAP>").append(ci._colPos)       .append("</TD>");
				sb.append("	<TD NOWRAP>").append(ci._colRemark != null ? ci._colRemark : "not available").append("</TD>");
				sb.append("</TR>");
			}
			sb.append("</TABLE>");
			sb.append("<HR>");
			sb.append("-end-");
			
			return sb.toString();
		}

		public String toHtmlString(String colname)
		{
			TableColumnInfo ci = null;
			for (TableColumnInfo e : _columns)
			{
				if (colname.equals(e._colName))
				{
					ci = e;
					break;
				}
			}
			if (ci == null)
				return "Column name '"+colname+"', was not found in table '"+_tabName+"'.";

			StringBuilder sb = new StringBuilder();
			sb.append("<B>").append(_tabName).append(".").append(ci._colName).append("</B> - <font color=\"blue\">").append(_tabType).append(" - COLUMN").append("</font>");
			sb.append("<HR>"); // add Horizontal Ruler: ------------------
			sb.append("<BR>");
			sb.append("<B>Table Description:</B> ").append(StringUtil.isNullOrBlank(_tabRemark) ? "not available" : _tabRemark).append("<BR>");
			sb.append("<BR>");
			sb.append("<B>Column Description:</B> ").append(StringUtil.isNullOrBlank(ci._colRemark) ? "not available" : ci._colRemark).append("<BR>");
			sb.append("<BR>");
			sb.append("<B>Name:</B> ")       .append(ci._colName)      .append("<BR>");
			sb.append("<B>Type:</B> ")       .append(ci._colType)      .append("<BR>");
			sb.append("<B>Length:</B> ")     .append(ci._colLength)    .append("<BR>");
			sb.append("<B>Is Nullable:</B> ").append(ci._colIsNullable).append("<BR>");
			sb.append("<B>Pos:</B> ")        .append(ci._colPos)       .append("<BR>");
			sb.append("<B>Default:</B> ")    .append(ci._colDefault)   .append("<BR>");
			sb.append("<HR>");
			sb.append("-end-");
			
			return sb.toString();
		}

		public TableColumnInfo getColumnInfo(String colname)
		{
			TableColumnInfo ci = null;
			for (TableColumnInfo e : _columns)
			{
				if (colname.equals(e._colName))
				{
					ci = e;
					break;
				}
			}
			return ci;
		}

		public String getColDdlDesc(String colname)
		{
			TableColumnInfo ci = getColumnInfo(colname);
			if (ci == null)
				return "Column name '"+colname+"', was not found in table '"+_tabName+"'.";

			String nulls    = ci._colIsNullable == DatabaseMetaData.columnNoNulls ? "<b>NOT</b> NULL" : "    NULL";
			String datatype = ci._colType;

			// Compose data type
			String dtlower = datatype.toLowerCase();
			if ( dtlower.equals("char") || dtlower.equals("varchar") )
				datatype = datatype + "(" + ci._colLength + ")";
			
			if ( dtlower.equals("numeric") || dtlower.equals("decimal") )
				datatype = datatype + "(" + ci._colLength + "," + ci._colScale + ")";

			return datatype + " " + nulls;
		}

		public String getColDescription(String colname)
		{
			TableColumnInfo ci = getColumnInfo(colname);
			if (ci == null)
				return "Column name '"+colname+"', was not found in table '"+_tabName+"'.";

			if (StringUtil.isNullOrBlank(ci._colRemark))
//				return "No Description";
				return "";
			return ci._colRemark;
		}
	}

	/**
	 * Holds information about parameters
	 */
	protected static class ProcedureParameterInfo
	{
		public String _paramName       = null;
		public int    _paramPos        = -1;
		public String _paramType       = null;
//		public String _paramType2      = null;
		public int    _paramLength     = -1;
		public int    _paramIsNullable = -1;
		public String _paramRemark     = null;
		public String _paramDefault    = null;
//		public int    _paramPrec       = -1;
		public int    _paramScale      = -1;
	}

	protected ProcedureInfo getProcedureInfo(String procName)
	{
		for (ProcedureInfo pi : _procedureInfoList)
		{
			if (procName.equals(pi._procName))
				return pi;
		}
		return null;
	}

	protected ProcedureInfo getSystemProcedureInfo(String procName)
	{
		for (ProcedureInfo pi : _systemProcInfoList)
		{
			if (procName.equals(pi._procName))
				return pi;
		}
		return null;
	}

	/**
	 * Holds information about procedures
	 */
	protected static class ProcedureInfo
	{
		public String _procCat     = null;
		public String _procSchema  = null;
		public String _procName    = null;
		public String _procType    = null;
		public String _procRemark  = null;
		
		public ArrayList<ProcedureParameterInfo> _parameters = new ArrayList<ProcedureParameterInfo>();

		public void addParameter(ProcedureParameterInfo ci)
		{
			_parameters.add(ci);
		}

		public String toHtmlString()
		{
			StringBuilder sb = new StringBuilder();
//			sb.append(_tabType).append(" - <B>").append(_tabName).append("</B>");
			sb.append("<B>").append(_procName).append("</B> - <font color=\"blue\">").append(_procType).append("</font>");
			sb.append("<HR>");
			sb.append("<BR>");
			sb.append("<B>Description:</B> ").append(StringUtil.isNullOrBlank(_procRemark) ? "not available" : _procRemark).append("<BR>");
			sb.append("<BR>");
			sb.append("<B>Columns:</B> ").append("<BR>");
			sb.append("<TABLE ALIGN=\"left\" BORDER=0 CELLSPACING=0 CELLPADDING=1\">");
			sb.append("<TR ALIGN=\"left\" VALIGN=\"top\" BGCOLOR=\"#ffffff\">");
			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Name")       .append("</B></FONT></TD>");
			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Datatype")   .append("</B></FONT></TD>");
			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Length")     .append("</B></FONT></TD>");
			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Nulls")      .append("</B></FONT></TD>");
			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Pos")        .append("</B></FONT></TD>");
			sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Description").append("</B></FONT></TD>");
			sb.append("</TR>");
			int r=0;
			for (ProcedureParameterInfo pi : _parameters)
			{
				r++;
				if ( (r % 2) == 0 )
					sb.append("<TR ALIGN=\"left\" VALIGN=\"top\" BGCOLOR=\"#ffffff\">");
				else
					sb.append("<TR ALIGN=\"left\" VALIGN=\"top\" BGCOLOR=\"#ffffcc\">");
				sb.append("	<TD NOWRAP>").append(pi._paramName)      .append("</TD>");
				sb.append("	<TD NOWRAP>").append(pi._paramType)      .append("</TD>");
				sb.append("	<TD NOWRAP>").append(pi._paramLength)    .append("</TD>");
				sb.append("	<TD NOWRAP>").append(pi._paramIsNullable).append("</TD>");
				sb.append("	<TD NOWRAP>").append(pi._paramPos)       .append("</TD>");
				sb.append("	<TD NOWRAP>").append(pi._paramRemark != null ? pi._paramRemark : "not available").append("</TD>");
				sb.append("</TR>");
			}
			sb.append("</TABLE>");
			sb.append("<HR>");
			sb.append("-end-");
			
			return sb.toString();
		}

		public String toHtmlString(String paramName)
		{
			ProcedureParameterInfo pi = null;
			for (ProcedureParameterInfo e : _parameters)
			{
				if (paramName.equals(e._paramName))
				{
					pi = e;
					break;
				}
			}
			if (pi == null)
				return "Parameter name '"+paramName+"', was not found in procedure '"+_procName+"'.";

			StringBuilder sb = new StringBuilder();
			sb.append("<B>").append(_procName).append(".").append(pi._paramName).append("</B> - <font color=\"blue\">").append(_procType).append(" - COLUMN").append("</font>");
			sb.append("<HR>"); // add Horizontal Ruler: ------------------
			sb.append("<BR>");
			sb.append("<B>Procedure Description:</B> ").append(StringUtil.isNullOrBlank(_procRemark) ? "not available" : _procRemark).append("<BR>");
			sb.append("<BR>");
			sb.append("<B>Column Description:</B> ").append(StringUtil.isNullOrBlank(pi._paramRemark) ? "not available" : pi._paramRemark).append("<BR>");
			sb.append("<BR>");
			sb.append("<B>Name:</B> ")       .append(pi._paramName)      .append("<BR>");
			sb.append("<B>Type:</B> ")       .append(pi._paramType)      .append("<BR>");
			sb.append("<B>Length:</B> ")     .append(pi._paramLength)    .append("<BR>");
			sb.append("<B>Is Nullable:</B> ").append(pi._paramIsNullable).append("<BR>");
			sb.append("<B>Pos:</B> ")        .append(pi._paramPos)       .append("<BR>");
			sb.append("<B>Default:</B> ")    .append(pi._paramDefault)   .append("<BR>");
			sb.append("<HR>");
			sb.append("-end-");
			
			return sb.toString();
		}

		public ProcedureParameterInfo getParameterInfo(String colname)
		{
			ProcedureParameterInfo ci = null;
			for (ProcedureParameterInfo e : _parameters)
			{
				if (colname.equals(e._paramName))
				{
					ci = e;
					break;
				}
			}
			return ci;
		}

		public String getParamDdlDesc(String paramName)
		{
			ProcedureParameterInfo pi = getParameterInfo(paramName);
			if (pi == null)
				return "Parameter name '"+paramName+"', was not found in procedure '"+_procName+"'.";

			String nulls    = pi._paramIsNullable == DatabaseMetaData.columnNoNulls ? "<b>NOT</b> NULL" : "    NULL";
			String datatype = pi._paramType;

			// Compose data type
			String dtlower = datatype.toLowerCase();
			if ( dtlower.equals("char") || dtlower.equals("varchar") )
				datatype = datatype + "(" + pi._paramLength + ")";
			
			if ( dtlower.equals("numeric") || dtlower.equals("decimal") )
				datatype = datatype + "(" + pi._paramLength + "," + pi._paramScale + ")";

			return datatype + " " + nulls;
		}

		public String getColDescription(String paramName)
		{
			ProcedureParameterInfo pi = getParameterInfo(paramName);
			if (pi == null)
				return "Column name '"+paramName+"', was not found in table '"+_procName+"'.";

			if (StringUtil.isNullOrBlank(pi._paramRemark))
//				return "No Description";
				return "";
			return pi._paramRemark;
		}
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
	public static CompletionProviderAbstract installAutoCompletion(Connection conn, TextEditorPane textPane, ErrorStrip errorStrip, Window window, ConnectionProvider connProvider)
	{
		if (conn == null)
		{
			_logger.info("Installing Completion Provider for JDBC");
			return CompletionProviderJdbc.installAutoCompletion(textPane, errorStrip, window, connProvider);
		}

		try
		{
			DatabaseMetaData md = conn.getMetaData();
			String prodName = md.getDatabaseProductName();
			
			if (ConnectionDialog.DB_PROD_NAME_SYBASE_ASE.equals(prodName))
			{
				_logger.info("Installing Completion Provider for Sybase ASE");
				return CompletionProviderAse.installAutoCompletion(textPane, errorStrip, window, connProvider);
			}
			else if (ConnectionDialog.DB_PROD_NAME_SYBASE_RS.equals(prodName))
			{
				_logger.info("Installing Completion Provider for Sybase Replication Server");
				return CompletionProviderRepServer.installAutoCompletion(textPane, errorStrip, window, connProvider);
			}
			else
			{
				_logger.info("Installing Completion Provider for JDBC");
				return CompletionProviderJdbc.installAutoCompletion(textPane, errorStrip, window, connProvider);
			}
		}
		catch (SQLException e)
		{
			_logger.info("Installing Completion Provider for JDBC, problems getting Database Product Name. Caught: "+e);
			return CompletionProviderJdbc.installAutoCompletion(textPane, errorStrip, window, connProvider);
		}
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

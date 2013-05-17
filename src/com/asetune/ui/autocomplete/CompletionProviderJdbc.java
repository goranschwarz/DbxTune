package com.asetune.ui.autocomplete;

import java.awt.Window;

import org.apache.log4j.Logger;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;

import com.asetune.utils.ConnectionProvider;

public class CompletionProviderJdbc
extends CompletionProviderAbstractSql
{
	private static Logger _logger = Logger.getLogger(CompletionProviderJdbc.class);

	public static CompletionProviderAbstract installAutoCompletion(TextEditorPane textPane, ErrorStrip errorStrip, Window window, ConnectionProvider connectionProvider)
	{
		_logger.info("Installing Syntax and AutoCompleation for JDBC ("+SyntaxConstants.SYNTAX_STYLE_SQL+").");
		textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);

		CompletionProviderAbstract acProvider = createCompletionProvider(window, connectionProvider);
		AutoCompletion ac = new AutoCompletion(acProvider);
		ac.install(textPane);
		ac.setShowDescWindow(true); // enable the "extra" descriptive window to the right of completion.
//		ac.setChoicesWindowSize(600, 600);
		ac.setDescriptionWindowSize(600, 600);
		
		return acProvider;
	}

	/**
	 * Constructor
	 * @param owner
	 * @param connectionProvider
	 */
	public CompletionProviderJdbc(Window owner, ConnectionProvider connectionProvider)
	{
		super(owner, connectionProvider);
	}

	/**
	 * Create a simple provider that adds some SQL completions.
	 *
	 * @return The completion provider.
	 */
	public static CompletionProviderAbstract createCompletionProvider(Window window, ConnectionProvider connectionProvider)
	{
		_logger.debug("JDBC: createCompletionProvider()");

		// A DefaultCompletionProvider is the simplest concrete implementation
		// of CompletionProvider. This provider has no understanding of
		// language semantics. It simply checks the text entered up to the
		// caret position for a match against known completions. This is all
		// that is needed in the majority of cases.
		CompletionProviderAbstract provider = new CompletionProviderJdbc(window, connectionProvider);

		// Add completions for all SQL keywords. A BasicCompletion is just a straightforward word completion.
		provider.addStaticCompletion(new BasicCompletion(provider, "SELECT * FROM "));
		
		return provider;
	}


//	public void addJdbcCompletion(Completion c)
//	{
//		super.addCompletion(c);
//	}
//	public void addJdbcCompletions(List<JdbcTableCompletion> list)
//	{
//		super.addCompletions(list);
//	}
//
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
//	private static final boolean isNonWordChar(Token t) {
//		return t.textCount==1 &&
//				!RSyntaxUtilities.isLetter(t.text[t.textOffset]);
//	}
//	/**
//	 * Returns whether the specified token is a type that we can do a
//	 * "mark occurrences" on.
//	 *
//	 * @param t The token.
//	 * @return Whether we should mark all occurrences of this token.
//	 */
//	private boolean isValidType(RSyntaxTextArea textArea, Token t) {
//		TokenMaker tmaker = TokenMakerFactory.getDefaultInstance().getTokenMaker(textArea.getSyntaxEditingStyle());
//		return tmaker.getMarkOccurrencesOfTokenType(t.type);
//	}
//
//
//	@Override
//	protected List<Completion> getCompletionsImpl(JTextComponent comp)
//	{
//System.out.println("JDBC: getCompletionsImpl()");
//
//		RSyntaxTextArea textArea = (RSyntaxTextArea)comp;
//
//		setCharsAllowedInWordCompletion("_.");
//
////		// Parse 
////		String allText     = comp.getText();
////		String enteredText = getAlreadyEnteredText(comp);
//
////		String currentWord = getCurrentWord(textArea);
//
//		String enteredText = getAlreadyEnteredText(textArea);
//		String currentWord = getCurrentWord(textArea);
//		String nextWord1   = getRelativeWord(textArea,  1);
//		String nextWord2   = getRelativeWord(textArea,  2);
//		String prevWord1   = getRelativeWord(textArea, -1);
//		String prevWord2   = getRelativeWord(textArea, -2);
//		String prevWord3   = getRelativeWord(textArea, -3);
//		String prevWord4   = getRelativeWord(textArea, -4);
//
//		System.out.println("-----------------------------------");
//		System.out.println("enteredText = '"+enteredText+"'.");
//		System.out.println("currentWord = '"+currentWord+"'.");
//		System.out.println("-----------------------------------");
//		System.out.println("nextWord1   = '"+nextWord1+"'.");
//		System.out.println("nextWord2   = '"+nextWord2+"'.");
//		System.out.println("-----------------------------------");
//		System.out.println("prevWord1   = '"+prevWord1+"'.");
//		System.out.println("prevWord2   = '"+prevWord2+"'.");
//		System.out.println("prevWord3   = '"+prevWord3+"'.");
//		System.out.println("prevWord4   = '"+prevWord4+"'.");
//		System.out.println("-----------------------------------");
//
////		System.out.println("getCurrentWord()='"+currentWord+"'.");
////		System.out.println("getAlreadyEnteredText()='"+enteredText+"'.");
//
////		try
////		{
////			SqlDocument doc = SqlDocument.parseDocument(allText);
////			ISqlItem si = doc.getRootItem();
////			System.out.println("xxx="+si);
////			System.out.println("yyy="+si.getSqlItemType());
////			System.out.println("zzz="+si.getSqlText());
////
////			List<ISqlStatement> xxx = doc.getStatementsReadonly();
////			for (ISqlStatement ss : xxx)
////			{
////				System.out.println("SS.sql="+ss.getSqlText());
////				System.out.println("SS.type="+ss.getSqlItemType());
////				System.out.println("SS.GroupType="+ss.getSqlItemGroupType());
////
////				System.out.println("objType="+ss.getClass().getName());
////				if (ss.getSqlItemType() == SqlItemType.SqlSelectStatement)
////				{
////					SqlSelectStatement xx = (SqlSelectStatement) ss;
////					List<SqlTable> tabList = xx.getTablesReadonly();
////					for (SqlTable sqlTable : tabList)
////					{
////						System.out.println("TAB.db="+sqlTable.getDatabase());
////						System.out.println("TAB.name="+sqlTable.getName());
////						System.out.println("TAB.alias="+sqlTable.getAlias());
////						System.out.println("TAB,index="+sqlTable.getIndex());
////						System.out.println("---------------------------------");
////					}
////				}
////				for (SqlAttribute attr : ss.getAttributesReadonly())
////				{
////					System.out.println("ATTR="+attr.getName());
////					System.out.println("ATTR="+attr);
////				}
////			}
////		}
////		catch (SqlParseException pe)
////		{
////			pe.printStackTrace();
////		}
//		
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
//
//		if (needRefresh())
//		{
//			// Clear old completions
//			clear();
////			super.completions.clear();
//
//			_schemaNames.clear();
//
//			// Restore the "saved" completions
////			for (Completion c : _savedComplitionList)
////				super.addCompletion(c);
//			super.addCompletions(_savedComplitionList);
//
//			// DO THE REFRESH
//			List<JdbcTableCompletion> list = refreshCompletion();
//
//			if (list != null && list.size() > 0)
//			{
//				// Add the completion list
////				for (JdbcTableCompletion entry : list)
////					addJdbcCompletion(entry);
//				addJdbcCompletions(list);
//
//				setNeedRefresh(false);
//			}
//		} // end _needRefresh
//
//		
//		//-----------------------------------------------------------
//		// Complete STORED PROCS
//		//
//		if ("exec".equalsIgnoreCase(prevWord1))
//		{
//System.out.println("getCompletionsImpl(): EXEC completion");
//			ArrayList<Completion> procList = new ArrayList<Completion>();
//
//			for (JdbcProcedureCompletion pc : _procedureComplList)
//			{
//System.out.println("getCompletionsImpl(): EXEC completion... USER PROC.");
//				ProcedureInfo pi = pc._procInfo;
//				if (Util.startsWithIgnoreCase(pi._procName, enteredText))
//					procList.add(pc);
//				
//			}
//			
//			if (Util.startsWithIgnoreCase(currentWord, "sp_"))
//			{
//System.out.println("getCompletionsImpl(): EXEC completion... SYSTEM PROC.");
//				for (JdbcProcedureCompletion pc : _systemProcComplList)
//				{
//					ProcedureInfo pi = pc._procInfo;
//					if (Util.startsWithIgnoreCase(pi._procName, enteredText))
//						procList.add(pc);
//				}
//			}
//			return procList;
//		}
//
//		//-----------------------------------------------------------
//		// Check for column name completion
//		// This is if the current word has a '.' in it
//		//
//		if (enteredText.indexOf('.') >= 0)
//		{
//System.out.println("getCompletionsImpl(): in COLUMN completion: ENTER");
//			int lastDot = enteredText.lastIndexOf('.');
//			String colName      = enteredText.substring(lastDot+1);
//			String tabAliasName = enteredText.substring(0, lastDot);
//			while(tabAliasName.indexOf('.') >= 0)
//				tabAliasName = tabAliasName.substring(tabAliasName.lastIndexOf('.')+1);
//
//			// If the "alias" name (word before the dot) is in any of the schema names
//			// then continue with normal completion.
//			if ( _schemaNames.contains(tabAliasName) )
//			{
//System.out.println("getCompletionsImpl(): in column compl: do normal completion");
//				String lastPart = colName;
//				// do normal completion
//				return getCompletionsSimple(comp, lastPart);
//			}
//			else // alias is NOT in the schemas
//			{
//	//			System.out.println("tabAliasName='"+tabAliasName+"'.");
//	//			System.out.println("colName     ='"+colName+"'.");
//	
//				String tabName = getTableNameForAlias(comp, tabAliasName, true);
//	//			System.out.println(" >>> tabName='"+tabName+"'.");
//				
//				ArrayList<Completion> colList = new ArrayList<Completion>();
//	
//				// find table name if this is a alias.
//				Completion c = null;
//				int index = Collections.binarySearch(completions, tabName, comparator);
//				if (index > 0)
//					c = (Completion) completions.get(index);
//	
//				if (c != null && c instanceof JdbcTableCompletion)
//				{
//					JdbcTableCompletion jdbcCompl = (JdbcTableCompletion) c;
//					for (TableColumnInfo tci : jdbcCompl._tableInfo._columns)
//					{
//						if (Util.startsWithIgnoreCase(tci._colName, colName))
//							colList.add( new JdbcColumnCompletion(thisOuter, tabAliasName, tci._colName, jdbcCompl._tableInfo));
//					}
//				}
//	//			else
//	//			{
//	//				colList.add( new ShorthandCompletion(thisJdbcCompleationProvider, "not_found", "not_found", "Table '"+tabName+"' not found.", "<html>The table name '"+tabName+"' can't be found in the internal lookup dictionary.</html>"));
//	//			}
//	
//				return colList;
//			}
//		}
//
////		return super.getCompletionsImpl(comp);
//		return getCompletionsSimple(comp, enteredText);
//	}
//
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
//
//    /**
//     * An attempt to minimal parse the text to find out what table name a alias has.<br>
//     * This is just a DUMMY, a real parse should be used instead. The problem with various
//     * SQL Parsers is that they throws exception if the syntax is faulty...<br>
//     * But when composing a SQL using completion the statement is "not yet complete" and ready
//     * to send to the server, therefore the syntax will be "off" 
//     *  
//     * @param comp            The text component
//     * @param alias           The alias name to search for a table name
//     * @param stripPrefix     If table name looks like 'dbo.t1' -> strip off all prefixes and return 't1'
//     * @return The Table name, if nothing is found the input alias will be returned.
//     */
//	public String getTableNameForAlias(JTextComponent comp, String alias, boolean stripPrefix)
//	{
//		String table = alias;
//
//		int cp = comp.getCaretPosition();
//		String otxt = comp.getText();
//		String txt  = comp.getText().toLowerCase().replace('\n', ' ');
//		int selectIndex = -1;
//		while (true)
//		{
//			int i = txt.indexOf("select ", selectIndex + 1);
//			if (i == -1)
//				break;
//			if (i >= cp)
//				break;
//
//			selectIndex = i;
//		}
//		int fromIndex = txt.indexOf(" from ", selectIndex);
//
//		String fromStr = null;
//		if (fromIndex >= 0)
//		{
//			fromIndex += " from ".length();
//			int stopIndex = -1;
//			if (stopIndex == -1) stopIndex = txt.indexOf(" where ",    fromIndex);
//			if (stopIndex == -1) stopIndex = txt.indexOf(" group by ", fromIndex);
//			if (stopIndex == -1) stopIndex = txt.indexOf(" order by ", fromIndex);
//			if (stopIndex == -1) stopIndex = txt.indexOf(" select ",   fromIndex);
//			if (stopIndex == -1) stopIndex = txt.indexOf(" update ",   fromIndex);
//			if (stopIndex == -1) stopIndex = txt.indexOf(" delete ",   fromIndex);
//
//			// If we can't find any "delimiter char", then try newline 
////			if (stopIndex == -1) stopIndex = otxt.indexOf('\n', fromIndex);
//			if (stopIndex == -1) stopIndex = otxt.length();
//			
//			if (stopIndex >= 0)
//				fromStr = otxt.substring(fromIndex, stopIndex);
//		}
//
//		// Try to split the from str
//		if (fromStr != null)
//		{
//			int aliasIndex = fromStr.indexOf(" "+alias);
//			if (aliasIndex >= 0)
//			{
//				String[] sa = fromStr.split(",");
//			//	System.out.println("sa[]: >"+StringUtil.toCommaStr(sa, "|")+"<");
//				for (String s : sa)
//				{
//					s = s.trim();
//					if (StringUtil.isNullOrBlank(s))
//						continue;
//
//					String[] sa2 = s.split("\\s+");
//				//	System.out.println("sa2[]: >"+StringUtil.toCommaStr(sa2, "|")+"<");
//					if (sa2.length >= 2)
//					{
//						if (sa2[1].equals(alias))
//						{
//							table = sa2[0];
//							if (table.indexOf('.') >= 0)
//							{
//								table = table.substring( table.lastIndexOf('.') + 1 );
//							}
//						//	System.out.println("Alias to TabName: alias'"+alias+"', tabname='"+table+"'.");
//						}
//					}
//				}
//			}
//			//System.out.println("FROM STR='"+fromStr+"'.");
//		}
//
//		return table;
//	}
//
//	/**
//	 * 
//	 * @param conn
//	 * @param waitDialog
//	 * @return
//	 * @throws SQLException
//	 */
//	protected void refreshCompletionForOthers(Connection conn, WaitForExecDialog waitDialog)
//	throws SQLException
//	{
//System.out.println("JDBC: refreshCompletionForOthers()");
//		if (waitDialog.wasCancelPressed())
//			return;
//
//		// Obtain a DatabaseMetaData object from our current connection        
//		DatabaseMetaData dbmd = conn.getMetaData();
//
//		// What PRODUCT are we connected to 
//		String dbProductName = dbmd.getDatabaseProductName();
//		
//		// if ASE go and get monTables description
////		if (DB_PROD_NAME_ASE.equals(dbProductName) && _aseMonTableDesc == null)
////		{
////			waitDialog.setState("Getting MDA Table information");
////			int aseVersionNum = AseConnectionUtils.getAseVersionNumber(conn);
////
////			_aseMonTableDesc = new HashMap<String, String>();
////			String sql = "select TableName, Description from master.dbo.monTables ";
////			if (aseVersionNum >= 15700)
////				sql += " where Language = 'en_US' ";
////
////			try
////			{
////				Statement stmnt = conn.createStatement();
////				ResultSet rs = stmnt.executeQuery(sql);
////				while(rs.next())
////				{
////					String tabName = rs.getString(1);
////					String tabDesc = rs.getString(2);
//////						System.out.println("_aseMonTableDesc.put('"+tabName+"', '"+tabDesc+"')");
////					_aseMonTableDesc.put(tabName, tabDesc);
////
////					if (waitDialog.wasCancelPressed())
////						return;
////				}
////				rs.close();
////				stmnt.close();
////			}
////			catch (SQLException sqle)
////			{
////				_logger.info("Problems when getting ASE monTables dictionary, skipping this and continuing. Caught: "+sqle);
////			}
////		}
//		if (DB_PROD_NAME_H2.equals(dbProductName))
//		{
//			waitDialog.setState("Getting H2 database settings");
//
//			// if 'DATABASE_TO_UPPER' is true, then table names must be quoted
//			String sql = "select VALUE from INFORMATION_SCHEMA.SETTINGS where NAME = 'DATABASE_TO_UPPER'";
//			try
//			{
//				Statement stmnt = conn.createStatement();
//				ResultSet rs = stmnt.executeQuery(sql);
//				while(rs.next())
//				{
//					String value = rs.getString(1);
//					
//					_quoteTableNames = value.trim().equalsIgnoreCase("true");
//				}
//				rs.close();
//				stmnt.close();
//			}
//			catch (SQLException sqle)
//			{
//				_logger.info("Problems when getting ASE monTables dictionary, skipping this and continuing. Caught: "+sqle);
//			}
//		}
//	}
//
//	protected List<TableInfo> refreshCompletionForTables(Connection conn, WaitForExecDialog waitDialog)
//	throws SQLException
//	{
//System.out.println("JDBC: refreshCompletionForTables()");
//		// Obtain a DatabaseMetaData object from our current connection        
//		DatabaseMetaData dbmd = conn.getMetaData();
//
//		// What PRODUCT are we connected to 
//		String dbProductName = dbmd.getDatabaseProductName();
//		
//		// Each table description has the following columns: 
//		// 1:  TABLE_CAT String                 => table catalog (may be null) 
//		// 2:  TABLE_SCHEM String               => table schema (may be null) 
//		// 3:  TABLE_NAME String                => table name 
//		// 4:  TABLE_TYPE String                => table type. Typical types are "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM". 
//		// 5:  REMARKS String                   => explanatory comment on the table 
//		// 6:  TYPE_CAT String                  => the types catalog (may be null) 
//		// 7:  TYPE_SCHEM String                => the types schema (may be null) 
//		// 8:  TYPE_NAME String                 => type name (may be null) 
//		// 9:  SELF_REFERENCING_COL_NAME String => name of the designated "identifier" column of a typed table (may be null) 
//		// 10: REF_GENERATION String            => specifies how values in SELF_REFERENCING_COL_NAME are created. Values are "SYSTEM", "USER", "DERIVED". (may be null) 
//
//		waitDialog.setState("Getting Table information");
//
//		ArrayList<TableInfo> tableInfoList = new ArrayList<TableInfo>();
//
//		if (waitDialog.wasCancelPressed())
//			return tableInfoList;
//
//		ResultSet rs = dbmd.getTables(null, null, "%", null);
//		while(rs.next())
//		{
//			TableInfo ti = new TableInfo();
//			ti._tabCat     = rs.getString(1);
//			ti._tabSchema  = rs.getString(2);
//			ti._tabName    = rs.getString(3);
//			ti._tabType    = rs.getString(4);
//			ti._tabRemark  = rs.getString(5);
//
//			// add schemas... this is a Set so duplicates is ignored
//			_schemaNames.add(ti._tabSchema);
//
//			// if ASE, dbname=master, tabname=mon*
//			// Replace Remark with description from MDA description
//			if (DB_PROD_NAME_ASE.equals(dbProductName) && _aseMonTableDesc != null)
//			{
//				if ("master".equals(ti._tabCat) && ti._tabName.startsWith("mon"))
//				{
//					ti._tabType   = "MDA Table";
//					ti._tabRemark = _aseMonTableDesc.get(ti._tabName);
//				}
//			}
//
//			tableInfoList.add(ti);
//			
//			if (waitDialog.wasCancelPressed())
//				return tableInfoList;
//		}
//		rs.close();
//		
//		return tableInfoList;
//	}
//
//	protected void refreshCompletionForTableColumns(Connection conn, WaitForExecDialog waitDialog, List<TableInfo> tableInfoList)
//	throws SQLException
//	{
//System.out.println("JDBC: refreshCompletionForTableColumns()");
//		// Obtain a DatabaseMetaData object from our current connection        
//		DatabaseMetaData dbmd = conn.getMetaData();
//
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
//
//
//		
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
//	}
//
//	protected List<ProcedureInfo> refreshCompletionForProcedures(Connection conn, WaitForExecDialog waitDialog)
//	throws SQLException
//	{
//System.out.println("JDBC: refreshCompletionForProcedures()");
//		waitDialog.setState("Getting Procedure information");
//
//		ArrayList<ProcedureInfo> procInfoList = new ArrayList<ProcedureInfo>();
//
//		return procInfoList;
//	}
//
//	protected void refreshCompletionForProcedureParameters(Connection conn, WaitForExecDialog waitDialog, List<ProcedureInfo> procedureInfoList)
//	throws SQLException
//	{
//System.out.println("JDBC: refreshCompletionForProcedureParameters()");
//		waitDialog.setState("Getting Procedure Parameter information");
//
//		// ADD Column information
//		for (ProcedureInfo pi : procedureInfoList)
//		{
//			if (waitDialog.wasCancelPressed())
//				return;
//
//			waitDialog.setState("Getting Parameter information for Procedure '"+pi._procName+"'.");
//		}
//	}
//
//	protected List<ProcedureInfo> refreshCompletionForSystemProcedures(Connection conn, WaitForExecDialog waitDialog)
//	throws SQLException
//	{
//System.out.println("ASE: refreshCompletionForSystemProcedures()");
//		waitDialog.setState("Getting Procedure information");
//
//		ArrayList<ProcedureInfo> procInfoList = new ArrayList<ProcedureInfo>();
//
//		return procInfoList;
//	}
//
//	protected void refreshCompletionForSystemProcedureParameters(Connection conn, WaitForExecDialog waitDialog, List<ProcedureInfo> procedureInfoList)
//	throws SQLException
//	{
//System.out.println("ASE: refreshCompletionForSystemProcedureParameters()");
//		waitDialog.setState("Getting Procedure Parameter information");
//
//		// ADD Column information
//		for (ProcedureInfo pi : procedureInfoList)
//		{
//			if (waitDialog.wasCancelPressed())
//				return;
//
//			waitDialog.setState("Getting Parameter information for Procedure '"+pi._procName+"'.");
//		}
//	}
//
//	/**
//	 * Go and get the Completions for the underlying JDBC Connection
//	 * @return
//	 */
//	protected List<JdbcTableCompletion> refreshCompletion()
//	{
//System.out.println("JDBC: refreshCompletion()");
//		final Connection conn = _connectionProvider.getConnection();
//		if (conn == null)
//			return null;
//
//		// Create a Waitfor Dialog and Executor, then execute it.
//		WaitForExecDialog wait = new WaitForExecDialog(_guiOwner, "Refreshing SQL Completion");
//
//		WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
//		{
//			// This is the object that will be returned.
//			ArrayList<JdbcTableCompletion> completionList = new ArrayList<JdbcTableCompletion>();
//			
//			@Override
//			public boolean canDoCancel() { return true; };
//			
//			@Override
//			public void cancel() 
//			{
//				_logger.info("refreshJdbcCompletion(), CANCEL has been called.");
//				// The WaitForExecDialog: wait.wasCancelPressed() will be true
////				setCanceled(true);
//			};
//			
//			@Override
//			public Object doWork()
//			{
//				try
//				{
////					// Obtain a DatabaseMetaData object from our current connection        
////					DatabaseMetaData dbmd = conn.getMetaData();
////
////					// What PRODUCT are we connected to 
////					String dbProductName = dbmd.getDatabaseProductName();
////					
////					boolean quoteTableNames = false;
////
////					// if ASE go and get monTables description
////					if (DB_PROD_NAME_ASE.equals(dbProductName) && _aseMonTableDesc == null)
////					{
////						wait.setState("Getting MDA Table information");
////						int aseVersionNum = AseConnectionUtils.getAseVersionNumber(conn);
////
////						_aseMonTableDesc = new HashMap<String, String>();
////						String sql = "select TableName, Description from master.dbo.monTables ";
////						if (aseVersionNum >= 15700)
////							sql += " where Language = 'en_US' ";
////
////						try
////						{
////							Statement stmnt = conn.createStatement();
////							ResultSet rs = stmnt.executeQuery(sql);
////							while(rs.next())
////							{
////								String tabName = rs.getString(1);
////								String tabDesc = rs.getString(2);
//////									System.out.println("_aseMonTableDesc.put('"+tabName+"', '"+tabDesc+"')");
////								_aseMonTableDesc.put(tabName, tabDesc);
////
////								if (cancel)
////									return null;
////							}
////							rs.close();
////							stmnt.close();
////						}
////						catch (SQLException sqle)
////						{
////							_logger.info("Problems when getting ASE monTables dictionary, skipping this and continuing. Caught: "+sqle);
////						}
////					}
////					if (DB_PROD_NAME_H2.equals(dbProductName))
////					{
////						wait.setState("Getting H2 database settings");
////
////						// if 'DATABASE_TO_UPPER' is true, then table names must be quoted
////						String sql = "select VALUE from INFORMATION_SCHEMA.SETTINGS where NAME = 'DATABASE_TO_UPPER'";
////						try
////						{
////							Statement stmnt = conn.createStatement();
////							ResultSet rs = stmnt.executeQuery(sql);
////							while(rs.next())
////							{
////								String value = rs.getString(1);
////								
////								quoteTableNames = value.trim().equalsIgnoreCase("true");
////							}
////							rs.close();
////							stmnt.close();
////						}
////						catch (SQLException sqle)
////						{
////							_logger.info("Problems when getting ASE monTables dictionary, skipping this and continuing. Caught: "+sqle);
////						}
////					}
//
////					// Each table description has the following columns: 
////					// 1:  TABLE_CAT String                 => table catalog (may be null) 
////					// 2:  TABLE_SCHEM String               => table schema (may be null) 
////					// 3:  TABLE_NAME String                => table name 
////					// 4:  TABLE_TYPE String                => table type. Typical types are "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM". 
////					// 5:  REMARKS String                   => explanatory comment on the table 
////					// 6:  TYPE_CAT String                  => the types catalog (may be null) 
////					// 7:  TYPE_SCHEM String                => the types schema (may be null) 
////					// 8:  TYPE_NAME String                 => type name (may be null) 
////					// 9:  SELF_REFERENCING_COL_NAME String => name of the designated "identifier" column of a typed table (may be null) 
////					// 10: REF_GENERATION String            => specifies how values in SELF_REFERENCING_COL_NAME are created. Values are "SYSTEM", "USER", "DERIVED". (may be null) 
////
////					getWaitDialog().setState("Getting Table information");
////
////					ArrayList<TableInfo> tableInfoList = new ArrayList<TableInfo>();
////
////					ResultSet rs = dbmd.getTables(null, null, "%", null);
////					while(rs.next())
////					{
////						TableInfo ti = new TableInfo();
////						ti._tabCat     = rs.getString(1);
////						ti._tabSchema  = rs.getString(2);
////						ti._tabName    = rs.getString(3);
////						ti._tabType    = rs.getString(4);
////						ti._tabRemark  = rs.getString(5);
////
////						// add schemas... this is a Set so duplicates is ignored
////						_schemaNames.add(ti._tabSchema);
////
////						// if ASE, dbname=master, tabname=mon*
////						// Replace Remark with description from MDA description
////						if (DB_PROD_NAME_ASE.equals(dbProductName) && _aseMonTableDesc != null)
////						{
////							if ("master".equals(ti._tabCat) && ti._tabName.startsWith("mon"))
////							{
////								ti._tabType   = "MDA Table";
////								ti._tabRemark = _aseMonTableDesc.get(ti._tabName);
////							}
////						}
////
////						tableInfoList.add(ti);
////						
////						if (getWaitDialog().wasCancelPressed())
////							return null;
////					}
////					rs.close();
////
////					// ADD Column information
////					for (TableInfo ti : tableInfoList)
////					{
////						// Retrieves a description of table columns available in the specified catalog. 
////						// Only column descriptions matching the catalog, schema, table and column name criteria are returned. They are ordered by TABLE_CAT,TABLE_SCHEM, TABLE_NAME, and ORDINAL_POSITION. 
////						// 
////						// Each column description has the following columns: 
////						// TABLE_CAT         String => table catalog (may be null) 
////						// TABLE_SCHEM       String => table schema (may be null) 
////						// TABLE_NAME        String => table name 
////						// COLUMN_NAME       String => column name 
////						// DATA_TYPE         int    => SQL type from java.sql.Types 
////						// TYPE_NAME         String => Data source dependent type name, for a UDT the type name is fully qualified 
////						// COLUMN_SIZE       int    => column size. 
////						//                                  BUFFER_LENGTH is not used. 
////						// DECIMAL_DIGITS    int    => the number of fractional digits. Null is returned for data types where DECIMAL_DIGITS is not applicable. 
////						// NUM_PREC_RADIX    int    => Radix (typically either 10 or 2) 
////						// NULLABLE          int    => is NULL allowed. 
////						//                                  columnNoNulls - might not allow NULL values 
////						//                                  columnNullable - definitely allows NULL values 
////						//                                  columnNullableUnknown - nullability unknown 
////						// REMARKS           String => comment describing column (may be null) 
////						// COLUMN_DEF        String => default value for the column, which should be interpreted as a string when the value is enclosed in single quotes (may be null) 
////						// SQL_DATA_TYPE     int    => unused 
////						// SQL_DATETIME_SUB  int    => unused 
////						// CHAR_OCTET_LENGTH int    => for char types the maximum number of bytes in the column 
////						// ORDINAL_POSITION  int    => index of column in table (starting at 1) 
////						// IS_NULLABLE       String => ISO rules are used to determine the nullability for a column. 
////						//                                  YES --- if the parameter can include NULLs 
////						//                                  NO --- if the parameter cannot include NULLs 
////						//                                  empty string --- if the nullability for the parameter is unknown 
////						// SCOPE_CATLOG      String => catalog of table that is the scope of a reference attribute (null if DATA_TYPE isn't REF) 
////						// SCOPE_SCHEMA      String => schema of table that is the scope of a reference attribute (null if the DATA_TYPE isn't REF) 
////						// SCOPE_TABLE       String => table name that this the scope of a reference attribure (null if the DATA_TYPE isn't REF) 
////						// SOURCE_DATA_TYPE  short  => source type of a distinct type or user-generated Ref type, SQL type from java.sql.Types (null if DATA_TYPE isn't DISTINCT or user-generated REF) 
////						// IS_AUTOINCREMENT  String => Indicates whether this column is auto incremented 
////						//                                  YES --- if the column is auto incremented 
////						//                                  NO --- if the column is not auto incremented 
////						//                                  empty string --- if it cannot be determined whether the column is auto incremented parameter is unknown 
////						//                                  The COLUMN_SIZE column the specified column size for the given column. For numeric data, this is the maximum precision. For character data, this is the length in characters. For datetime datatypes, this is the length in characters of the String representation (assuming the maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes. For the ROWID datatype, this is the length in bytes. Null is returned for data types where the column size is not applicable.
////						// 
////						// Parameters:
////						// - catalog a catalog name; must match the catalog name as it is stored in the database; "" retrieves those without a catalog; null means that the catalog name should not be used to narrow the search
////						// - schemaPattern a schema name pattern; must match the schema name as it is stored in the database; "" retrieves those without a schema; null means that the schema name should not be used to narrow the search
////						// - tableNamePattern a table name pattern; must match the table name as it is stored in the database 
////						// - columnNamePattern a column name pattern; must match the column name as it is stored in the database
////
////						getWaitDialog().setState("Getting Column information for table '"+ti._tabName+"'.");
////
////						rs = dbmd.getColumns(null, null, ti._tabName, "%");
////						while(rs.next())
////						{
////							TableColumnInfo ci = new TableColumnInfo();
////							ci._colName       = rs.getString("COLUMN_NAME");
////							ci._colPos        = rs.getInt   ("ORDINAL_POSITION");
////							ci._colType       = rs.getString("TYPE_NAME");
////							ci._colLength     = rs.getInt   ("COLUMN_SIZE");
////							ci._colIsNullable = rs.getInt   ("NULLABLE");
////							ci._colRemark     = rs.getString("REMARKS");
////							ci._colDefault    = rs.getString("COLUMN_DEF");
////							ci._colScale      = rs.getInt   ("DECIMAL_DIGITS");
////							
////							ti.addColumn(ci);
////						}
////						rs.close();
////
////						// PK INFO
//////						rs = dbmd.getPrimaryKeys(null, null, ti._tabName);
//////						ResultSetTableModel rstm = new ResultSetTableModel(rs);
//////						System.out.println("######### PK("+ti._tabName+"):--------\n" + rstm.toTableString());
////////						while(rs.next())
////////						{
////////							TablePkInfo pk = new TablePkInfo();
////////							pk._colName       = rs.getString("COLUMN_NAME");
////////							
////////							ti.addPk(pk);
////////						}
//////						rs.close();
////						
////						// INDEX INFO
//////						rs = dbmd.getIndexInfo(null, null, ti._tabName, false, false);
//////						rstm = new ResultSetTableModel(rs);
//////						System.out.println("########## INDEX("+ti._tabName+"):--------\n" + rstm.toTableString());
////////						while(rs.next())
////////						{
////////							TableIndexInfo index = new TableIndexInfo();
////////							index._colName       = rs.getString("COLUMN_NAME");
////////							
////////							ti.addIndex(index);
////////						}
//////						rs.close();
////
////						
////						// update DESCRIPTION for MDA Columns
////						// is ASE, db=master, table mon*
////						if (DB_PROD_NAME_ASE.equals(dbProductName) && "master".equals(ti._tabCat) && ti._tabName.startsWith("mon") )
////						{
////							getWaitDialog().setState("Getting MDA Column Description");
////							int aseVersionNum = AseConnectionUtils.getAseVersionNumber(conn);
////							
////							//String sql = "select ColumnName, TypeName, Length, Description, Precision, Scale from master.dbo.monTableColumns where TableName = '"+ti._tabName+"'";
////							String sql = "select ColumnName, Description from master.dbo.monTableColumns where TableName = '"+ti._tabName+"'";
////							if (aseVersionNum >= 15700)
////								sql += " and Language = 'en_US' ";
////
////							try
////							{
////								Statement stmnt = conn.createStatement();
////								rs = stmnt.executeQuery(sql);
////								while(rs.next())
////								{
////									String colname = rs.getString(1);
////									String colDesc = rs.getString(2);
////
////									TableColumnInfo ci = ti.getColumnInfo(colname);
////									if (ci != null)
////										ci._colRemark = colDesc;
////								}
////								rs.close();
////								stmnt.close();
////							}
////							catch (SQLException sqle)
////							{
////								_logger.info("Problems when getting ASE monTableColumns dictionary, skipping this and continuing. Caught: "+sqle);
////							}
////						}
////
////						if (getWaitDialog().wasCancelPressed())
////							break;
////
////					} // end: for (TableInfo ti : tableInfoList)
////					
////					getWaitDialog().setState("Creating Code Completions.");
////					for (TableInfo ti : tableInfoList)
////					{
////						JdbcTableCompletion c = new JdbcTableCompletion(thisOuter, ti, _quoteTableNames);
////						completionList.add(c);
////					}
//
//					//----------------------------------------------------------
//					// Get Table and Columns informaation
//					_tableInfoList = refreshCompletionForTables(conn, getWaitDialog());
//					refreshCompletionForTableColumns(conn, getWaitDialog(), _tableInfoList);
//
//					// Create completion list
//					getWaitDialog().setState("Creating Table Completions.");
//					for (TableInfo ti : _tableInfoList)
//					{
//						JdbcTableCompletion c = new JdbcTableCompletion(thisOuter, ti, _quoteTableNames);
//						completionList.add(c);
//						_tableComplList.add(c);
//					}
//
//
//					//----------------------------------------------------------
//					// Get USER Procedure and Parameters information
//					_procedureInfoList = refreshCompletionForProcedures(conn, getWaitDialog());
//					refreshCompletionForProcedureParameters(conn, getWaitDialog(), _procedureInfoList);
//
//					getWaitDialog().setState("Creating Procedure Completions.");
//					for (ProcedureInfo pi : _procedureInfoList)
//					{
//						JdbcProcedureCompletion c = new JdbcProcedureCompletion(thisOuter, pi);
//						_procedureComplList.add(c);
//					}
//
//					//----------------------------------------------------------
//					// Get SYSTEM Procedure and Parameters information
//					// Only do this once
//					if (_systemProcInfoList.size() == 0)
//					{
//						_systemProcInfoList = refreshCompletionForSystemProcedures(conn, getWaitDialog());
//						refreshCompletionForSystemProcedureParameters(conn, getWaitDialog(), _systemProcInfoList);
//
//						getWaitDialog().setState("Creating System Procedure Completions.");
//						for (ProcedureInfo pi : _systemProcInfoList)
//						{
//							JdbcProcedureCompletion c = new JdbcProcedureCompletion(thisOuter, pi);
//							_systemProcComplList.add(c);
//						}
//					}
//
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
//		ArrayList<JdbcTableCompletion> list = (ArrayList)wait.execAndWait(doWork);
//
//		return list;
//    }
//
//    /**
//	 * Our own Completion class, which overrides toString() to make it HTML aware
//	 */
//	protected static class JdbcTableCompletion
//	extends ShorthandCompletion
//	{
//		private TableInfo _tableInfo = null;
//		
//		public JdbcTableCompletion(CompletionProviderJdbc provider, TableInfo ti, boolean quoteTableNames)
//		{
//			super(provider, 
//				ti._tabName,
//				! quoteTableNames 
//					? ti._tabSchema+"."+ti._tabName 
//					: "\""+ti._tabSchema+"\".\""+ti._tabName+"\"");
//
//			_tableInfo = ti;
//
//			String shortDesc = 
//				"<font color=\"blue\">"+ti._tabType+"</font>" +
//				" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(ti._tabRemark) ? "No Description" : ti._tabRemark) + "</font></i>";
//			setShortDescription(shortDesc);
//			setSummary(_tableInfo.toHtmlString());
//		}
//
//		/**
//		 * Make it HTML aware
//		 */
//		@Override
//		public String toString()
//		{
//			return "<html>" + super.toString() + "</html>";
//		}
//	}
//
//    /**
//	 * Our own Completion class, which overrides toString() to make it HTML aware
//	 */
//	protected static class JdbcProcedureCompletion
//	extends ShorthandCompletion
//	{
//		private ProcedureInfo _procInfo = null;
//		
//		public JdbcProcedureCompletion(CompletionProviderJdbc provider, ProcedureInfo pi)
//		{
//			super(provider, 
//				pi._procName,
//				pi._procName);
//
//			_procInfo = pi;
//
//			String shortDesc = 
//				"<font color=\"blue\">"+pi._procType+"</font>" +
//				" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(pi._procRemark) ? "No Description" : pi._procRemark) + "</font></i>";
//			setShortDescription(shortDesc);
//			setSummary(_procInfo.toHtmlString());
//		}
//
//		/**
//		 * Make it HTML aware
//		 */
//		@Override
//		public String toString()
//		{
//			return "<html>" + super.toString() + "</html>";
//		}
//	}
//
//    /**
//	 * Our own Completion class, which overrides toString() to make it HTML aware
//	 */
//	protected static class JdbcColumnCompletion
//	extends ShorthandCompletion
//	{
//		private TableInfo _tableInfo = null;
//		
//		public JdbcColumnCompletion(CompletionProvider provider, String tabAliasName, String colname, TableInfo tableInfo)
//		{
//			super(provider, colname, (tabAliasName == null ? colname : tabAliasName+"."+colname));
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
//			setSummary(_tableInfo.toHtmlString(colname));
//		}
//
//		/**
//		 * Make it HTML aware
//		 */
//		@Override
//		public String toString()
//		{
//			return "<html>" + super.toString() + "</html>";
//		}
//	}
//
//	/**
//	 * Holds information about columns
//	 */
//	protected static class TableColumnInfo
//	{
//		public String _colName       = null;
//		public int    _colPos        = -1;
//		public String _colType       = null;
//		public int    _colLength     = -1;
//		public int    _colIsNullable = -1;
//		public String _colRemark     = null;
//		public String _colDefault    = null;
////		public int    _colPrec       = -1;
//		public int    _colScale      = -1;
//	}
//
//	/**
//	 * Holds information about tables
//	 */
//	protected static class TableInfo
//	{
//		public String _tabCat     = null;
//		public String _tabSchema  = null;
//		public String _tabName    = null;
//		public String _tabType    = null;
//		public String _tabRemark  = null;
//		
//		public ArrayList<TableColumnInfo> _columns = new ArrayList<TableColumnInfo>();
//
//		public void addColumn(TableColumnInfo ci)
//		{
//			_columns.add(ci);
//		}
//
//		public String toHtmlString()
//		{
//			StringBuilder sb = new StringBuilder();
////			sb.append(_tabType).append(" - <B>").append(_tabName).append("</B>");
//			sb.append("<B>").append(_tabName).append("</B> - <font color=\"blue\">").append(_tabType).append("</font>");
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
//			sb.append("-end-");
//			
//			return sb.toString();
//		}
//
//		public String toHtmlString(String colname)
//		{
//			TableColumnInfo ci = null;
//			for (TableColumnInfo e : _columns)
//			{
//				if (colname.equals(e._colName))
//				{
//					ci = e;
//					break;
//				}
//			}
//			if (ci == null)
//				return "Column name '"+colname+"', was not found in table '"+_tabName+"'.";
//
//			StringBuilder sb = new StringBuilder();
//			sb.append("<B>").append(_tabName).append(".").append(ci._colName).append("</B> - <font color=\"blue\">").append(_tabType).append(" - COLUMN").append("</font>");
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
//			sb.append("-end-");
//			
//			return sb.toString();
//		}
//
//		public TableColumnInfo getColumnInfo(String colname)
//		{
//			TableColumnInfo ci = null;
//			for (TableColumnInfo e : _columns)
//			{
//				if (colname.equals(e._colName))
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
//				return "No Description";
//			return ci._colRemark;
//		}
//	}
//
//	/**
//	 * Holds information about parameters
//	 */
//	protected static class ProcedureParameterInfo
//	{
//		public String _paramName       = null;
//		public int    _paramPos        = -1;
//		public String _paramType       = null;
//		public int    _paramLength     = -1;
//		public int    _paramIsNullable = -1;
//		public String _paramRemark     = null;
//		public String _paramDefault    = null;
////		public int    _paramPrec       = -1;
//		public int    _paramScale      = -1;
//	}
//
//	/**
//	 * Holds information about procedures
//	 */
//	protected static class ProcedureInfo
//	{
//		public String _procCat     = null;
//		public String _procSchema  = null;
//		public String _procName    = null;
//		public String _procType    = null;
//		public String _procRemark  = null;
//		
//		public ArrayList<ProcedureParameterInfo> _parameters = new ArrayList<ProcedureParameterInfo>();
//
//		public void addParameter(ProcedureParameterInfo ci)
//		{
//			_parameters.add(ci);
//		}
//
//		public String toHtmlString()
//		{
//			StringBuilder sb = new StringBuilder();
////			sb.append(_tabType).append(" - <B>").append(_tabName).append("</B>");
//			sb.append("<B>").append(_procName).append("</B> - <font color=\"blue\">").append(_procType).append("</font>");
//			sb.append("<HR>");
//			sb.append("<BR>");
//			sb.append("<B>Description:</B> ").append(StringUtil.isNullOrBlank(_procRemark) ? "not available" : _procRemark).append("<BR>");
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
//			for (ProcedureParameterInfo pi : _parameters)
//			{
//				r++;
//				if ( (r % 2) == 0 )
//					sb.append("<TR ALIGN=\"left\" VALIGN=\"top\" BGCOLOR=\"#ffffff\">");
//				else
//					sb.append("<TR ALIGN=\"left\" VALIGN=\"top\" BGCOLOR=\"#ffffcc\">");
//				sb.append("	<TD NOWRAP>").append(pi._paramName)      .append("</TD>");
//				sb.append("	<TD NOWRAP>").append(pi._paramType)      .append("</TD>");
//				sb.append("	<TD NOWRAP>").append(pi._paramLength)    .append("</TD>");
//				sb.append("	<TD NOWRAP>").append(pi._paramIsNullable).append("</TD>");
//				sb.append("	<TD NOWRAP>").append(pi._paramPos)       .append("</TD>");
//				sb.append("	<TD NOWRAP>").append(pi._paramRemark != null ? pi._paramRemark : "not available").append("</TD>");
//				sb.append("</TR>");
//			}
//			sb.append("</TABLE>");
//			sb.append("<HR>");
//			sb.append("-end-");
//			
//			return sb.toString();
//		}
//
//		public String toHtmlString(String paramName)
//		{
//			ProcedureParameterInfo pi = null;
//			for (ProcedureParameterInfo e : _parameters)
//			{
//				if (paramName.equals(e._paramName))
//				{
//					pi = e;
//					break;
//				}
//			}
//			if (pi == null)
//				return "Parameter name '"+paramName+"', was not found in procedure '"+_procName+"'.";
//
//			StringBuilder sb = new StringBuilder();
//			sb.append("<B>").append(_procName).append(".").append(pi._paramName).append("</B> - <font color=\"blue\">").append(_procType).append(" - COLUMN").append("</font>");
//			sb.append("<HR>"); // add Horizontal Ruler: ------------------
//			sb.append("<BR>");
//			sb.append("<B>Procedure Description:</B> ").append(StringUtil.isNullOrBlank(_procRemark) ? "not available" : _procRemark).append("<BR>");
//			sb.append("<BR>");
//			sb.append("<B>Column Description:</B> ").append(StringUtil.isNullOrBlank(pi._paramRemark) ? "not available" : pi._paramRemark).append("<BR>");
//			sb.append("<BR>");
//			sb.append("<B>Name:</B> ")       .append(pi._paramName)      .append("<BR>");
//			sb.append("<B>Type:</B> ")       .append(pi._paramType)      .append("<BR>");
//			sb.append("<B>Length:</B> ")     .append(pi._paramLength)    .append("<BR>");
//			sb.append("<B>Is Nullable:</B> ").append(pi._paramIsNullable).append("<BR>");
//			sb.append("<B>Pos:</B> ")        .append(pi._paramPos)       .append("<BR>");
//			sb.append("<B>Default:</B> ")    .append(pi._paramDefault)   .append("<BR>");
//			sb.append("<HR>");
//			sb.append("-end-");
//			
//			return sb.toString();
//		}
//
//		public ProcedureParameterInfo getParameterInfo(String colname)
//		{
//			ProcedureParameterInfo ci = null;
//			for (ProcedureParameterInfo e : _parameters)
//			{
//				if (colname.equals(e._paramName))
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
//				return "No Description";
//			return pi._paramRemark;
//		}
//	}
}

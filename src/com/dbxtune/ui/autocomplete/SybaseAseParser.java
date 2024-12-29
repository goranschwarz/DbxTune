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
package com.dbxtune.ui.autocomplete;

import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;

public class SybaseAseParser
extends AbstractParser
{
	private DefaultParseResult _result = new DefaultParseResult(this);
	
	
	@Override
	public ParseResult parse(RSyntaxDocument doc, String style)
	{
		_result.clearNotices();
		
		int lineCount = doc.getDefaultRootElement().getElementCount();
		_result.setParsedLines(0, lineCount-1);

		String sqlText = "";
		try
		{
			sqlText = doc.getText(0, doc.getLength());
		}
		catch (BadLocationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("---- SQL --- BEGIN --- ");
		System.out.println(sqlText);
		System.out.println("---- SQL --- END   --- ");
		
		long start = System.currentTimeMillis();

//		// PUT THE PARSER CODE HERE...
//		// create a CharStream that reads from standard input
//		ANTLRInputStream input = null;
//		try
//		{
//			input = new ANTLRInputStream(new StringReader(sqlText));
//		}
//		catch (IOException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		// create a lexer that feeds off of input CharStream
//		SybaseAseLexer lexer = new SybaseAseLexer(input);
//
//		// create a buffer of tokens pulled from the lexer
//		CommonTokenStream tokens = new CommonTokenStream(lexer);
//
//		// create a parser that feeds off the tokens buffer
//		com.dbxtune.parser.sybase.ase.SybaseAseParser parser = new com.dbxtune.parser.sybase.ase.SybaseAseParser(tokens);
//
//		parser.removeErrorListeners(); // remove ConsoleErrorListener
//		parser.addErrorListener(new VerboseListener()); // add ours
//		
//		ParseTree tree = parser.program(); // begin parsing at init rule
//		System.out.println(tree.toStringTree(parser)); // print LISP-style tree

		long time = System.currentTimeMillis() - start;
		_result.setParseTime(time);
		//System.out.println(time + "ms");

		// TODO Auto-generated method stub
		return _result;
	}
	
	
//	private class VerboseListener extends BaseErrorListener
//	{
//		@Override
//		public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
//		{
//			List<String> stack = ((Parser) recognizer).getRuleInvocationStack();
//			Collections.reverse(stack);
//			System.err.println("rule stack: " + stack);
//			System.err.println("line " + line + ":" + charPositionInLine + " at " + offendingSymbol + ": " + msg);
//
//System.out.println("PARSER ERROR: LINE='"+line+"', charPositionInLine='"+charPositionInLine+"', e="+e);
//
//			DefaultParserNotice pn = new DefaultParserNotice(SybaseAseParser.this, msg, line, charPositionInLine, -1);
//			_result.addNotice(pn);
//		}
//	}
}


//--------------------------------------------------------------------
// General SQL Parser
//--------------------------------------------------------------------
//System.out.println("!!!! PARSE(General SQL Parser) BEGIN !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//System.out.println("Parser License: "+TGSqlParser.getLicenseType());
//System.out.println("Parser License Message: "+TGSqlParser.getLicenseMessage());
////
//TGSqlParser sqlparser = new TGSqlParser(EDbVendor.dbvsybase);
//sqlparser.setSqltext(sqlText);
//int parseRc = sqlparser.parse();
//
////findXxx(sqlparser, caret.getDot());
//
//if (parseRc == 0)
//{
//	System.out.println("parseRc=0: ---OK----");
//
//	DefaultParserNotice pn = new DefaultParserNotice(this, "dummy text", 2);
//	_result.addNotice(pn);
//
////    for(int i=0;i<sqlparser.sqlstatements.size();i++)
////        iterateStmt(sqlparser.sqlstatements.get(i));
//
////	sl.get
////	WhereCondition w = new WhereCondition(sqlparser.sqlstatements.get( 0 ).getWhereClause( ).getCondition( ));
////	w.printColumn();
//}
//else
//{
//	System.out.println("parseRc="+parseRc+": ################################### FAIL FAIL FAIL ###################");
//	System.out.println(sqlparser.getErrormessage( ));
////    for(int i=0;i<sqlparser.sqlstatements.size();i++)
////        iterateStmt(sqlparser.sqlstatements.get(i));
//	ArrayList<TSyntaxError> parserErrors = sqlparser.getSyntaxErrors();
//	for (TSyntaxError serr : parserErrors)
//	{
//		DefaultParserNotice pn = new DefaultParserNotice(this, serr.hint, (int)serr.lineNo);
//		_result.addNotice(pn);
//
//		System.out.println("TSyntaxError.columnNo  = " + serr.columnNo);
//		System.out.println("TSyntaxError.errorno   = " + serr.errorno);
//		System.out.println("TSyntaxError.hint      = " + serr.hint);
//		System.out.println("TSyntaxError.lineNo    = " + serr.lineNo);
//		System.out.println("TSyntaxError.tokentext = " + serr.tokentext);
//		System.out.println("TSyntaxError.errortype = " + serr.errortype);
//		System.out.println("TSyntaxError.toString()= " + serr.toString());
//	}
//}
//System.out.println("!!!! PARSE(General SQL Parser) END !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");






//--------------------------------------------------------------------
// http://www.eclipse.org/datatools/project_sqldevtools/sqltools_doc/SQL%20Query%20Parser%20User%20documentation.htm
//--------------------------------------------------------------------
//try
//{
//					// 2.2 Getting a com.dbxtune.parser manager for a specific vendor
//					// The recommended way is to instantiate a com.dbxtune.parser based on the
//					// database vendor information so as to handle vendor specific
//					// variations of SQL. The following code snippet shows how to create
//					// the com.dbxtune.parser based on the vendor information
//					// TODO initialize the variable db with the
//					// org.eclipse.wst.rdb.internal.models.sql.schema.Database intance
//					// obtained from
//					// the database connection
//					// Database db;
//		
//					// TODO get the vendorname and version after the variable is
//					// initialized
//					// String dbName = db.getVendor();
//					// String dbVersion = db.getVersion();
//		
//					// get the best matching com.dbxtune.parser manager depending on what com.dbxtune.parser
//					// extension are plugged in
//				//	SQLQueryParserManager parserManager = SQLQueryParserManagerProvider.getInstance().getParserManager(dbName, dbVersion);
//
//	// Create an instance the Parser Manager
//	// SQLQueryParserManagerProvider.getInstance().getParserManager
//	// returns the best compliant SQLQueryParserManager
//	// supporting the SQL dialect of the database described by the given
//	// database product information. In the code below null is passed
//	// for both the database and version
//	// in which case a generic com.dbxtune.parser is returned
//	SQLQueryParserManager parserManager = SQLQueryParserManagerProvider.getInstance().getParserManager(null, null);
//
//	// Sample query
////	String sql = "SELECT * FROM TABLE1";
//	String sql = sqlText;
//
//	// Parse
//	SQLQueryParseResult parseResult = parserManager.parseQuery(sql);
//
//	// Get the Query Model object from the result
//	QueryStatement resultObject = parseResult.getQueryStatement();
//
//	// Get the SQL text
////	String parsedSQL = resultObject.getSQL();
////	System.out.println(parsedSQL);
//}
//catch (SQLParserException spe)
//{
//	// handle the syntax error
//	System.out.println(spe.getMessage());
//	List syntacticErrors = spe.getErrorInfoList();
//	Iterator itr = syntacticErrors.iterator();
//	while (itr.hasNext())
//	{
//		SQLParseErrorInfo errorInfo = (SQLParseErrorInfo) itr.next();
//		// Example usage of the SQLParseErrorInfo object
//		// the error message
//		String errorMessage = errorInfo.getParserErrorMessage();
//		// the line numbers of error
//		int errorLine = errorInfo.getLineNumberStart();
//		int errorColumn = errorInfo.getColumnNumberStart();
//		
//		System.out.println("SQLParserException: errorLine='"+errorLine+"', errorColumn='"+errorColumn+"', errorMessage='"+errorMessage+"'.");
//
//	}
//}
//catch (SQLParserInternalException spie)
//{
//	// handle the exception
//	System.out.println(spie.getMessage());
//}		

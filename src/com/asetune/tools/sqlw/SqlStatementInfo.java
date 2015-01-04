package com.asetune.tools.sqlw;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SortOrder;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;

import com.asetune.Version;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.tools.sqlw.msg.JAseMessage;
import com.asetune.tools.sqlw.msg.JAseProcRetCode;
import com.asetune.tools.sqlw.msg.JAseProcRetParam;
import com.asetune.tools.sqlw.msg.JPlainResultSet;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;
import com.sybase.jdbcx.SybPreparedStatement;

/*----------------------------------------------------------------------
** BEGIN: class SqlStatementInfo, SqlParam
**----------------------------------------------------------------------*/ 
public class SqlStatementInfo
{
	private static Logger _logger = Logger.getLogger(SqlStatementInfo.class);

	private boolean                         _callableStatement = false;
	private boolean                         _preparedStatement = false;
	private ArrayList<SqlParam> _sqlParams = null;
	private String                          _sqlOrigin = null;
	private String                          _sql       = null;
	private boolean                         _doReturnCode = false;

	// Used when SQL String do NOT start with "exec "
	// Used during ResultSet loop
	private Statement _stmnt = null;

	// Used when SQL String starts with "exec "
	private CallableStatement _cstmnt = null;

	public Statement getStatement()
	{
//System.out.println("SqlStatementInfo.getStatement():" + ((_useCallableStatement) ? "CALLABLE" : "LANGUAGE"));
		return (_callableStatement||_preparedStatement) ? _cstmnt : _stmnt;  
	}
	
	public boolean execute() 
	throws SQLException
	{
//System.out.println("SqlStatementInfo.execute():" + ((_useCallableStatement) ? _sql : _sqlOrigin));
		return (_callableStatement||_preparedStatement) ? _cstmnt.execute() : _stmnt.execute(_sqlOrigin);
	}
	
	public SqlStatementInfo(Connection conn, String sql, String dbProductName, ArrayList<JComponent> resultCompList)
	throws SQLException
	{
		_sqlOrigin = sql;
		_sql = sql.trim();

		String w1 = "";
		String w2 = "";
		// Check if it's a LOCAL command, which starts with: \
		if (_sql.startsWith("\\"))
		{
			// A set of known commands
			String[] knownCommands = {"\\exec", "\\rpc", "\\call", "\\prep"};

			// Get first and seconds word
			StringTokenizer st = new StringTokenizer(_sql);
			int word = 0;
			while (st.hasMoreTokens()) 
			{
				word++;
				if      (word == 1) w1 = st.nextToken();
				else if (word == 2) w2 = st.nextToken();
				else break;
			}

			// UNKNOWN command, give a list of available commands.
			if ( ! StringUtil.arrayContains(knownCommands, w1) || w2.equals(""))
			{
				String msg = 
					  "Unknown Local Command (or no parameters to it): " + w1 + "\n"
					+ "\n"
					+ "Local Commands available: \n"
					+ "    \\exec procName ? ? :(params)               -- exec using Callable Statement\n"
					+ "    \\rpc  procName ? ? :(params)               -- exec using Callable Statement\n"
					+ "    \\call procName ? ? :(params)               -- exec using Callable Statement\n"
					+ "    \\prep insert inti t1 values(? ?) :(params) -- exec using Prepared Statement\n"
					+ "\n"
					+ "param description: \n"
					+ "    Type     Value               java.sql.Types Example: replace question mark(?) with value\n"
					+ "    -------  ------------------- -------------- ----------------------------------------\n"
					+ "    string = 'a string value'    Types.VARCHAR  string='it''s a string', string=null\n"
					+ "    int    = integer             Types.INTEGER  int=99, int=null\n"
					+ "    clob   = 'filename|url'      Types.CLOB     clob='c:\\xxx.txt, clob='http://asetune.com'\n"
					+ "    blob   = 'filename|url'      Types.BLOB     blob='c:\\xxx.jpg, blob='http://www.asemon.se/images/sample3.png'\n"
					+ "    ora_rs                       -10            a ResultSet OUTPUT parameter, from an Oracle Procedure\n"
					+ "                                                ora_rs will simply be treated as a ResultSet for SQL Window.\n"
					+ "Examples: \n"
					+ "    \\call procName1(?,?,?) :(string='a string', int=99, string=null) \n"
					+ "    \\call procName2(?,?)   :(int=99, string=null out) -- calls a procedure where last parameter is an output variable\n"
					+ "    \\call oracleProc(?)    :(ora_rs) -- calls a Oracle procedure which has a SYS_REFCURSOR as output parameter\n"
					+ "\n"
					+ "    \\prep insert into t1 values(?,?,?)     :(int=98, string=null, blob='http://www.asemon.se/images/sample3.png') \n"
					+ "    \\prep insert into t1 values(99,NULL,?) :(blob='http://www.asemon.se/images/sample3.png') \n"
					+ "";
				throw new SQLException(msg);
			}
		}


		// Flag that this is officially a CallableStatement or PreparedStatement
		if      (w1.equals("\\exec")) _callableStatement = true;
		else if (w1.equals("\\rpc" )) _callableStatement = true;
		else if (w1.equals("\\call")) _callableStatement = true;
		else if (w1.equals("\\prep")) _preparedStatement = true;

		// Should we try to make a RPC call to the server
		if (_callableStatement || _preparedStatement)
		{
			// Initialize this to be empty at start
			_sqlParams = new ArrayList<SqlParam>();

			boolean forceRpc = false;
			String sqlParamsStr = "";
			String rpcParamsStr = "";

			// COMMAND: \prep
			if (_preparedStatement)
			{
				int startPos = _sql.indexOf(w2); // first word after '\prep'

				// input: \prep insert into t1 values(?, ?, ?)    :( string='xxx', int=99, int=null ) --- there might be comments here...
				//              ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
				// extract      sqlParamsStr                         rpcParamsStr

				int rpcParamsBeginIdx = _sql.indexOf(":(");
				int rpcParamsEndIdx   = StringUtil.indexOfEndBrace(_sql, rpcParamsBeginIdx + 2, ')');

				sqlParamsStr = _sql.substring(startPos).trim();
				if (rpcParamsBeginIdx >= 0)
				{
					if (rpcParamsEndIdx < 0)
						throw new SQLException("Missing end parentheses of the dynamic parameter specification. I found ':(' at pos "+rpcParamsBeginIdx+", but no end parenthes");

					sqlParamsStr = _sql.substring(startPos, rpcParamsBeginIdx).trim();
					rpcParamsStr = _sql.substring(rpcParamsBeginIdx + 2, rpcParamsEndIdx);
				}
//System.out.println("SqlStatementInfo(): PreparedStmnt.sqlParamsStr=|"+sqlParamsStr+"|.");
//System.out.println("SqlStatementInfo(): PreparedStmnt.rpcParamsStr=|"+rpcParamsStr+"|.");

				_sql = sqlParamsStr;
				if (StringUtil.hasValue(rpcParamsStr))
					_sqlParams = SqlParam.parse(rpcParamsStr);
			}

			// FIXME: make the below code EASIER
			// COMMAND: \exec
			// COMMAND: \rpc
			// COMMAND: \call
			if ( _callableStatement )
			{
				// If we are connected to a server that has return codes
				if (DbUtils.isProductName(dbProductName, DbUtils.DB_PROD_NAME_SYBASE_ASE, DbUtils.DB_PROD_NAME_SYBASE_ASA, DbUtils.DB_PROD_NAME_SYBASE_IQ, DbUtils.DB_PROD_NAME_SYBASE_RS, DbUtils.DB_PROD_NAME_MSSQL))
					_doReturnCode = true;
				
				forceRpc = false;
				if (w1.equals("\\rpc"))
					forceRpc = true;

				
				// ------------------------------------------------------------------------
				// FIRST break up the _sql, into 2 parts: SQL_STATEMENT and DYNAMIC_PARAMETERS_SPEC

				// position of first word after '\exec' or '\rpc' or '\call'
				int startPos = _sql.indexOf(w2); 

				// input: \call procname(?, ?, ?)    :( string='xxx', int=99, int=null ) --- there might be comments here...
				//              ^^^^^^^^^^^^^^^^^       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
				// extract      sqlParamsStr           rpcParamsStr

				int rpcParamsBeginIdx = _sql.indexOf(":(");
				int rpcParamsEndIdx   = StringUtil.indexOfEndBrace(_sql, rpcParamsBeginIdx + 2, ')');

				sqlParamsStr = _sql.substring(startPos).trim();
				if (rpcParamsBeginIdx >= 0)
				{
					if (rpcParamsEndIdx < 0)
						throw new SQLException("Missing end parentheses of the dynamic parameter specification. I found ':(' at pos "+rpcParamsBeginIdx+", but no end parenthes");

					sqlParamsStr = _sql.substring(startPos, rpcParamsBeginIdx).trim();
					rpcParamsStr = _sql.substring(rpcParamsBeginIdx + 2, rpcParamsEndIdx);
				}
//System.out.println("SqlStatementInfo(): CallableStmnt.sqlParamsStr=|"+sqlParamsStr+"|.");
//System.out.println("SqlStatementInfo(): CallableStmnt.rpcParamsStr=|"+rpcParamsStr+"|.");



				// ------------------------------------------------------------------------
				// Now break up the sqlParamStr, into 2 parts: PROCNAME and PROC_PARAMETERS
				// input: \call procname(?, ?, ?)
				//              ^^^^^^^^ ^^^^^^^ 
				// extract      procName sqlParamsStr
				String procName = "";

				int sqlParamStart = sqlParamsStr.indexOf('(');
				if (sqlParamStart >= 0) // ok '(' was found... else we go for ' '
				{
					int endParenthesPos = StringUtil.indexOfEndBrace(sqlParamsStr, sqlParamStart + 1, ')');
					if (endParenthesPos < 0)
						throw new SQLException("Missing end parentheses of the procedure parameters. I found '(' at pos "+sqlParamStart+", but no end parenthes");

					procName     = sqlParamsStr.substring(0, sqlParamStart);
					sqlParamsStr = sqlParamsStr.substring(sqlParamStart+1, endParenthesPos);
				}
				else
				{
					sqlParamStart = sqlParamsStr.indexOf(' ');
					if (sqlParamStart >= 0)
					{
						procName     = sqlParamsStr.substring(0, sqlParamStart);
						sqlParamsStr = sqlParamsStr.substring(sqlParamStart+1);
					}
					else
					{
						procName     = sqlParamsStr;
						sqlParamsStr = "";
					}
				}
				procName = procName.trim();

//System.out.println("SqlStatementInfo(): CallableStmnt.procName    =|"+procName+"|.");
//System.out.println("SqlStatementInfo(): CallableStmnt.sqlParamsStr=|"+sqlParamsStr+"|.");
//System.out.println("SqlStatementInfo(): CallableStmnt.sqlParamsStr=|"+rpcParamsStr+"|.");
				
				// RPC Parameters spec ':({int|string}=val[ out][,...])'
				// \exec procname ?, ?, ? :(int=3,string=val, int=99)
				if (StringUtil.hasValue(rpcParamsStr))
				{
					_sqlParams = SqlParam.parse(rpcParamsStr);
				}
				
				if (_doReturnCode)
					_sql = "{?=call "+procName+"("+sqlParamsStr+")}";
				else
					_sql = "{call "+procName+"("+sqlParamsStr+")}";
				
				_logger.debug("NEW PROC SQL: |"+_sql+"|.");
			} // end: _callableStatement


			//----------------------------------------------------
			// CHECK if question marks matches parameters

			int questionMarkCount = StringUtil.charCount(sqlParamsStr, '?');
//System.out.println("SqlStatementInfo(): questionMarkCount="+questionMarkCount+", sqlParamsStr.length()="+sqlParamsStr.length()+", _sqlParams.size()="+_sqlParams.size()+".");
			if (questionMarkCount == 0 && sqlParamsStr.length() > 0)
			{
				String rpcParamSpec        = ":( {int|bigint|string|numeric|timestamp[(fmt)]|date[(fmt)]|time[(fmt)]|clob|blob} = val [ out] [,...] )";
				String rpcParamSpecExample = ":( int = 99, string = 'abc', int = 999 out, clob='c:\filename.txt', clob='http://google.com' )";
				String rpcfullExample      = "\\exec sp_who ? :( string = '2' )";

				String msg = "EXEC PROCEDURE AS RPC LOGIC: \n" +
				             "Trying to execute a stored procedure via RPC method. The parameter list dosn't contain any Question Marks('?') This will probably *force* the JDBC driver to deliver the call as a 'language' statament to the server. \n" +
				             "Current CallableStatement sql looks like '"+_sql+"'. \n" +
				             "If you want it to be sent as a RPC to the server, you needs to use Question Marks(?) for the parameter(s), and a parameter specification (se below) so that the JDBC driver can issue the call in an aproperiate way. \n" +
				             "Format of the Parameter Specification is '"+rpcParamSpec+"'. \n" +
				             "Example of the Parameter Specification '"+rpcParamSpecExample+"'. \n" +
				             "Example of a SQL that will be translated to a RPC call '"+rpcfullExample+"'.";

				// if issued by '\rpc' then throw an exception
				// otherwise just write a warning message and continue
				if (forceRpc)
					throw new SQLException(msg);

				// logg... but remove all NewLines
				_logger.warn(msg.replace("\n", ""));

				// Add WARNING Message to the result
				resultCompList.add(new JAseMessage(Version.getAppName()+": WARNING - "+msg, _sql));
			}
			if (questionMarkCount != _sqlParams.size())
			{
				String rpcParamSpec        = ":( {int|bigint|string|numeric|timestamp[(fmt)]|date[(fmt)]|time[(fmt)]|clob|blob} = val [ out] [,...] )";
				String rpcParamSpecExample = ":( int = 99, string = 'abc', int = 999 out, clob='c:\filename.txt', clob='http://google.com' )";
				String rpcfullExample      = "\\exec sp_who ? :( string = '2' )";
				
				String msg = "EXEC PROCEDURE AS RPC LOGIC: \n" +
				             "Trying to execute a stored procedure via RPC method. Number of Question Marks('?') doesn't match the parameter specification count (QuestionMarkCount="+questionMarkCount+", paramSpecCount="+_sqlParams.size()+"). \n" +
				             "Current CallableStatement sql looks like '"+_sql+"'. \n" +
				             "ParameterSpecification looks like '"+rpcParamsStr+"'. \n" +
				             "Format of the Parameter Specification is '"+rpcParamSpec+"'. \n" +
				             "Example of the Parameter Specification '"+rpcParamSpecExample+"'. \n" +
				             "Example of a SQL that will be translated to a RPC call '"+rpcfullExample+"'.";
				throw new SQLException(msg);
			}
		} // end: exec or call

		//---------------------------------------------------------------
		// Prepare the call and set parameters
		if (_callableStatement||_preparedStatement)
		{
			// Get a CallableStatement... (will also be used for PreparedStatement, since CallableStatement extends PreparedStatement)
			_cstmnt = conn.prepareCall(_sql);

			// Register the return-status
			if (_doReturnCode)
				_cstmnt.registerOutParameter(1, Types.INTEGER);

			// Set RPC Parameters
			if (_sqlParams != null)
			{
				int pos = _doReturnCode ? 1 : 0;
				for (SqlParam param : _sqlParams)
				{
					pos++; // first pos will be 2, since #1 is used as return status

					if (param.isOutputParam())
						_cstmnt.registerOutParameter(pos, param.getSqlType());
					else
					{
						if (param.getSqlType() == Types.BLOB)
						{
							_cstmnt.setBytes(pos, (byte[])param.getValue());
//							if (param._val instanceof InputStream)
////								_cstmnt.setBlob(pos, (InputStream)param._val);
//								_cstmnt.setBinaryStream(pos, (InputStream)param._val);
//							else
//								throw new SQLException("INTERNAL ERROR: Input parameter "+pos+" must be of InputStream...");
						}
						else if (param.getSqlType() == Types.CLOB)
						{
							_cstmnt.setString(pos, (String)param.getValue());
//							if (param._val instanceof InputStream)
////								_cstmnt.setClob(pos, new InputStreamReader( (InputStream)param._val) );
//								_cstmnt.setAsciiStream(pos, (InputStream)param._val );
//							else
//								throw new SQLException("INTERNAL ERROR: Input parameter "+pos+" must be of InputStream...");
						}
						else if (param.getSqlType() == Types.NUMERIC)
						{
							BigDecimal bd = (BigDecimal) param.getValue();

							// for jConnect we need to set the precision and scale (maybe in later jConnect this works out-of-the-box)
							if (_cstmnt instanceof SybPreparedStatement)
								((SybPreparedStatement)_cstmnt).setBigDecimal(pos, bd, bd.precision(), bd.scale());
							else
								_cstmnt.setBigDecimal(pos, bd);
						}
						else
						{
							_cstmnt.setObject(pos, param.getValue(), param.getSqlType());
						}
					}
				}
			}
		}
		else
		{
			// Get a "regular" Statement...
			_stmnt = conn.createStatement();
		}
	}

	/**
	 * Read output parameters
	 * 
	 * @param resultCompList
	 * @param asPlainText
	 * @throws SQLException
	 */
	public void readRpcReturnCodeAndOutputParameters(ArrayList<JComponent> resultCompList, boolean asPlainText) 
	throws SQLException
	{
		if (_callableStatement||_preparedStatement)
		{
			if (_doReturnCode)
			{
				int returnStatus = _cstmnt.getInt(1);
				resultCompList.add( new JAseProcRetCode(returnStatus, _sql) );
			}
			
			// Set RPC Parameters
			if (_sqlParams != null)
			{
				int pos       = _doReturnCode ? 1 : 0;
				int posAdjust = _doReturnCode ? 1 : 0;
				for (SqlParam param : _sqlParams)
				{
					pos++;
					
					if (param.isOutputParam())
					{
						Object outParamVal = _cstmnt.getObject(pos);

						// If OUTput parameter is ORACLE SYS_REFCURSOR, then read the ResultSet
						if (    outParamVal != null 
						     && outParamVal instanceof ResultSet 
						     && param.getSqlType() == SqlParam.ORACLE_CURSOR_TYPE
						   )
						{
							ResultSet rs = (ResultSet) outParamVal;

							// For the moment, don't support pipe/filters etc... just make this simple
							if (asPlainText)
							{
								ResultSetTableModel rstm = new ResultSetTableModel(rs, true, "Oracle ResultSet Cursor");
								
								resultCompList.add(new JPlainResultSet(rstm));
							}
							else
							{
								// Convert the ResultSet into a TableModel, which fits on a JTable
								ResultSetTableModel rstm = new ResultSetTableModel(rs, true, "Oracle ResultSet Cursor");
			
								// Create the JTable, using the just created TableModel/ResultSet
								JXTable tab = new ResultSetJXTable(rstm);
								tab.setSortable(true);
								tab.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
								tab.packAll(); // set size so that all content in all cells are visible
								tab.setColumnControlVisible(true);
								tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

								// Add the JTable to a list for later use
								resultCompList.add(tab);
							}
						}
						else
						{
							resultCompList.add( new JAseProcRetParam(pos-posAdjust, outParamVal, param.getSqlType(), _sql) );
						}
					} // end: isOutput parameter
				}// end: loop SqlParam
			}
		}
	} // end: method

	
	//###########################################################################################
	//###########################################################################################
	// Some test code
	//###########################################################################################
	//###########################################################################################
	private static void test(int testnum, boolean shouldWork, Connection conn, String sql, String product, ArrayList<JComponent> resultCompList)
	{
		System.out.println(">>>>>>>>> Test number '"+testnum+"'.");
		try
		{
			System.out.println("--------- input '"+sql+"'.");
			new SqlStatementInfo(conn, sql, product, resultCompList);
			
			if (shouldWork)
				System.out.println("--------- Test number '"+testnum+"': OK");
			else
				System.err.println("--------- Test number '"+testnum+"': ----FAILED-----");
		}
		catch (Throwable e)
		{
			if (e instanceof SQLException)
				System.out.println("EXCEPTION: "+e);
			else
				e.printStackTrace();

			if ( ! shouldWork )
				System.out.println("--------- Test number '"+testnum+"': OK");
			else
				System.err.println("--------- Test number '"+testnum+"': ----FAILED-----");
		}
		System.out.println("<<<<<<<<< Test number '"+testnum+"'.");
		System.out.println();
	}
	public static void main(String[] args)
	{
		try
		{
			Connection conn = AseConnectionFactory.getConnection("GORAN_15702_DS", null, "sa", "sybase", "dummy");

			test(1,  true, conn, "select * from xxx", "dbProductName", null);

			test(2,  false, conn, "\\prep insert into t1 values(?,?,?)",                                 "dbProductName", null);
			test(3,  false, conn, "\\prep insert into t1 values (?, ?, ?)",                              "dbProductName", null);
			test(4,  false, conn, "\\prep insert into t1 values ( ?, ?, ? ):(string='',int=99,int=null", "dbProductName", null);
			test(5,  true,  conn, "\\prep insert into t1 values ( ?, ?, ? ):(string='',int=99,int=null)", "dbProductName", null);
			test(6,  true,  conn, "\\prep insert into t1 values ( ?, ?, ? ):(string=' ',int=99,int=null)", "dbProductName", null);

			test(7,  false, conn, "\\call proc1(?,?,?)",                                                 "dbProductName", null);
			test(8,  false, conn, "\\call proc1  (?,?,?",                                                "dbProductName", null);
			test(9,  false, conn, "\\call proc1  (?,?,?)",                                               "dbProductName", null);
			test(10, false, conn, "\\call proc1 ?, ?, ?",                                                "dbProductName", null);
			test(11, false, conn, "\\call proc1 ( ?, ?, ? ):(string='',int=99,int=null", "dbProductName", null);
			test(12, true,  conn, "\\call proc1 ( ?, ?, ? ) :(string = '', int = 99 , string = 'it''s a string')", "dbProductName", null);
			test(13, true,  conn, "\\call proc1 ( ?, ?, ? ) :(string = '', int = 99 , string = \" Note: 5 \"\" wheel.  \")", "dbProductName", null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
} // end: class SqlStatementInfo


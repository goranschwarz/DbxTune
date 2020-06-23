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
package com.asetune.tools.sqlw;

import java.awt.Component;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.JComponent;

import com.asetune.sql.SqlProgressDialog;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.pipe.PipeCommandException;
import com.asetune.utils.StringUtil;

public class SqlStatementFactory
{
	public static SqlStatement create(DbxConnection conn, String sql, String dbProductName, ArrayList<JComponent> resultCompList, SqlProgressDialog progress, Component owner, QueryWindow queryWindow)
	throws SQLException, PipeCommandException
	{
		String sqlOrigin = sql;
		sql = sql.trim();

		String w1 = "";
		String w2 = "";
		// Check if it's a LOCAL command, which starts with: \
		if (sql.startsWith("\\"))
		{
			// A set of known commands
			String[] knownCommands  = {
					"\\exec", 
					"\\rpc", 
					"\\call", 
					"\\prep", 
					"\\loadfile",
					"\\ddlgen", 
					"\\ssh", 
					"\\set", 
					"\\connect", 
					"\\disconnect", 
					"\\use", 
					"\\tabdiff", 
					"\\dbdiff", 
					"\\rsql",
					"\\ccr",
					"\\utf8len"
					};

			String[] mustHaveParams = {
					"\\exec", 
					"\\rpc", 
					"\\call", 
					"\\prep", 
					"\\ssh"
					};

			// Get first and seconds word
			StringTokenizer st = new StringTokenizer(sql);
			int word = 0;
			while (st.hasMoreTokens()) 
			{
				word++;
				if      (word == 1) w1 = st.nextToken();
				else if (word == 2) w2 = st.nextToken();
				else break;
			}

			// UNKNOWN command, give a list of available commands.
			if ( ! StringUtil.arrayContains(knownCommands, w1) || (StringUtil.arrayContains(mustHaveParams, w1) && w2.equals("")) )
			{
				String msg = 
					  "Unknown Local Command (or no parameters to it): " + w1 + "\n"
					+ "\n"
					+ "Local Commands Available: \n"
					+ "    \\loadfile -T tabname filename              -- load a (CSV) file into a table\n"
					+ "    \\ddlgen -h | -t tabname                    -- generate DDL and open editor\n"
					+ "    \\ssh [user@host] cmd                       -- execute a command on the remote host\n"
					+ "    \\rsql                                      -- Execute SQL Statement on a remote DBMS\n"
					+ "    \\set [-u] [var=val]                        -- Set a variable to a value. Varaibles can be used in text as ${varname}\n"
					+ "    \\connect profileName                       -- Connect to any saved connection profile\n"
					+ "    \\disconnect                                -- Disconnect from current DBMS.\n"
					+ "    \\use                                       -- Use/switch db Connection (used with DBMS that do not support cross db access, like Postgres and DB2)\n"
					+ "    \\tabdiff                                   -- Do data 'diff' on two tables (on different servers, or DBMS's)\n"
					+ "    \\dbdiff                                    -- Do data 'diff' on all tables in two databases (on different servers, or DBMS's)\n"
					+ "    \\ccr                                       -- Code Completion Refresh\n"
					+ "    \\utf8len                                   -- Check table content for char/varchar columns with conents wider than they can hold (used for db migrations)\n"
					+ "\n"
					+ "Local JDBC Extention Calls Available: \n"
					+ "    \\exec procName ? ? :(params)               -- exec using Callable Statement\n"
					+ "    \\rpc  procName ? ? :(params)               -- exec using Callable Statement\n"
					+ "    \\call procName ? ? :(params)               -- exec using Callable Statement\n"
					+ "    \\prep insert inti t1 values(? ?) :(params) -- exec using Prepared Statement\n"
					+ "\n"
					+ "param description (for JDBC Extentions): \n"
					+ "    Type        Value               java.sql.Types  Example: replace question mark(?) with value\n"
					+ "    ---------   ------------------- --------------- --------------------------------------------\n"
					+ "    string    = 'a string value'    Types.VARCHAR   string='it''s a string', string=null\n"
					+ "    nstring   = 'a string value'    Types.NVARCHAR  string='it''s a string', string=null\n"
					+ "    int       = integer             Types.INTEGER   int=99, int=null\n"
					+ "    bigint    = long                Types.BIGINT    bigint=9999999999999999999, int=null\n"
					+ "    numeric   = bigdecimal          Types.NUMERIC   numeric=1.12, numeric=null\n"
					+ "    double    = double              Types.DOUBLE    double=1.12, double=null\n"
					+ "    timestamp = 'datetime str'      Types.TIMESTAMP timestamp='2015-01-10 14:20:10', timestamp(dd/MM/yyyy HH.mm)='31/12/2014 14.00', timestamp=null\n"
					+ "    date      = 'date str'          Types.DATE      date='2015-01-10', date(dd/MM/yyyy)='31/12/2014', date=null\n"
					+ "    time      = 'time str'          Types.TIME      time='14:20:10', time(HH.mm)='14.00', time=null\n"
					+ "    nclob     = 'filename|url'      Types.NCLOB     clob='c:\\xxx.txt, clob='http://dbxtune.com'\n"
					+ "    clob      = 'filename|url'      Types.CLOB      clob='c:\\xxx.txt, clob='http://dbxtune.com'\n"
					+ "    blob      = 'filename|url'      Types.BLOB      blob='c:\\xxx.jpg, blob='http://www.dbxtune.com/images/sample3.png'\n"
					+ "    ora_rs                          -10             a ResultSet OUTPUT parameter, from an Oracle Procedure\n"
					+ "                                                    ora_rs will simply be treated as a ResultSet for SQL Window.\n"
					+ "Examples: \n"
					+ "    \\call procName1(?,?,?) :(string='a string', int=99, string=null) \n"
					+ "    \\call procName2(?,?)   :(int=99, string=null out) -- calls a procedure where last parameter is an output variable\n"
					+ "    \\call oracleProc(?)    :(ora_rs) -- calls a Oracle procedure which has a SYS_REFCURSOR as output parameter\n"
					+ "\n"
					+ "    \\prep insert into t1 values(?,?,?)     :(int=98, string=null, blob='http://www.dbxtune.com/images/sample3.png') \n"
					+ "    \\prep insert into t1 values(99,NULL,?) :(blob='http://www.dbxtune.com/images/sample3.png') \n"
					+ "";
				throw new SQLException(msg);
			}
		}

		// Decide what to create
		if      (w1.equals("\\loadfile"))   return new SqlStatementCmdLoadFile        (conn, sqlOrigin, dbProductName, resultCompList, progress, owner, queryWindow);
		if      (w1.equals("\\ddlgen"))     return new SqlStatementCmdDdlGen          (conn, sqlOrigin, dbProductName, resultCompList, progress, owner, queryWindow);
		if      (w1.equals("\\ssh"))        return new SqlStatementCmdSsh             (conn, sqlOrigin, dbProductName, resultCompList, progress, owner, queryWindow);
		if      (w1.equals("\\rsql"))       return new SqlStatementCmdRemoteSql       (conn, sqlOrigin, dbProductName, resultCompList, progress, owner, queryWindow);
		if      (w1.equals("\\set"))        return new SqlStatementCmdSet             (conn, sqlOrigin, dbProductName, resultCompList, progress, owner, queryWindow);
		if      (w1.equals("\\connect"))    return new SqlStatementCmdConnect         (conn, sqlOrigin, dbProductName, resultCompList, progress, owner, queryWindow);
		if      (w1.equals("\\disconnect")) return new SqlStatementCmdDisconnect      (conn, sqlOrigin, dbProductName, resultCompList, progress, owner, queryWindow);
		if      (w1.equals("\\use"))        return new SqlStatementCmdUse             (conn, sqlOrigin, dbProductName, resultCompList, progress, owner, queryWindow);
		if      (w1.equals("\\tabdiff"))    return new SqlStatementCmdTabDiff         (conn, sqlOrigin, dbProductName, resultCompList, progress, owner, queryWindow);
		if      (w1.equals("\\dbdiff"))     return new SqlStatementCmdDbDiff          (conn, sqlOrigin, dbProductName, resultCompList, progress, owner, queryWindow);
		if      (w1.equals("\\ccr"))        return new SqlStatementCmdCodeComplRefresh(conn, sqlOrigin, dbProductName, resultCompList, progress, owner, queryWindow);
		if      (w1.equals("\\utf8len"))    return new SqlStatementCmdUtf8Length      (conn, sqlOrigin, dbProductName, resultCompList, progress, owner, queryWindow);

//		else if (w1.equals("\\exec"))     return new SqlStatementCallPrep(conn, sqlOrigin, dbProductName, resultCompList, progress);
//		else if (w1.equals("\\rpc" ))     return new SqlStatementCallPrep(conn, sqlOrigin, dbProductName, resultCompList, progress);
//		else if (w1.equals("\\call"))     return new SqlStatementCallPrep(conn, sqlOrigin, dbProductName, resultCompList, progress);
//		else if (w1.equals("\\prep"))     return new SqlStatementCallPrep(conn, sqlOrigin, dbProductName, resultCompList, progress);
//		
//		// Everything else
//		return new SqlStatementNormal(conn, sqlOrigin, dbProductName, resultCompList);
		
		return new SqlStatementInfo(conn, sqlOrigin, dbProductName, resultCompList);
	}
}

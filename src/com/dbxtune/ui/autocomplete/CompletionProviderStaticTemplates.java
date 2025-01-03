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

import java.util.ArrayList;
import java.util.List;

import com.dbxtune.Version;
import com.dbxtune.ui.autocomplete.completions.CompletionTemplate;
import com.dbxtune.utils.SwingUtils;

public class CompletionProviderStaticTemplates
{
	public static List<CompletionTemplate> createCompletionTemplates()
	{
		ArrayList<CompletionTemplate> list = new ArrayList<CompletionTemplate>();
		
		// Add completions for all SQL keywords. A BasicCompletion is just a straightforward word completion.
		list.add( new CompletionTemplate("ss", "SELECT * FROM "));
		list.add( new CompletionTemplate(      "SELECT * FROM "));

		list.add( new CompletionTemplate("CASE WHEN x=1 THEN 'x=1' WHEN x=2 THEN 'x=2' ELSE 'not' END"));

		list.add( new CompletionTemplate( "JOIN",
				  "SELECT <select_list> \n"
				+ "FROM Table_A A \n"
				+ "INNER JOIN Table_B B\n"
				+ "        ON A.Key = B.Key", 
				"JOIN: (normal) return all of the records in the left table (table A) that have a matching record in the right table (table B). ",
				SwingUtils.readImageIcon(Version.class, "images/cc_sql_join_inner.png")));

		list.add( new CompletionTemplate( "INNER JOIN",
				  "SELECT <select_list> \n"
				+ "FROM Table_A A \n"
				+ "INNER JOIN Table_B B\n"
				+ "        ON A.Key = B.Key", 
				"INNER JOIN: (normal) return all of the records in the left table (table A) that have a matching record in the right table (table B). ",
				SwingUtils.readImageIcon(Version.class, "images/cc_sql_join_inner.png")));

		list.add( new CompletionTemplate( "LEFT JOIN",
				  "SELECT <select_list> \n"
				+ "FROM Table_A A \n"
				+ "LEFT OUTER JOIN Table_B B\n"
				+ "             ON A.Key = B.Key", 
				"LEFT [OUTER] JOIN: return all of the records in the left table (table A) regardless if any of those records have a match in the right table (table B). It will also return any matching records from the right table",
				SwingUtils.readImageIcon(Version.class, "images/cc_sql_join_left.png")));

		list.add( new CompletionTemplate( "RIGHT JOIN",
				  "SELECT <select_list> \n"
				+ "FROM Table_A A \n"
				+ "RIGHT OUTER JOIN Table_B B\n"
				+ "              ON A.Key = B.Key", 
				"RIGHT [OUTER] JOIN: return all of the records in the right table (table B) regardless if any of those records have a match in the left table (table A). It will also return any matching records from the left table",
				SwingUtils.readImageIcon(Version.class, "images/cc_sql_join_right.png")));

		list.add( new CompletionTemplate( "FULL OUTER JOIN",
				  "SELECT <select_list> \n"
				+ "FROM Table_A A \n"
				+ "FULL OUTER JOIN JOIN Table_B B\n"
				+ "                  ON A.Key = B.Key", 
				"FULL [OUTER] JOIN: return all of the records from both tables, joining records from the left table (table A) that match records from the right table (table B). ",
				SwingUtils.readImageIcon(Version.class, "images/cc_sql_join_outer.png")));

		// \exec  and \rpc templates
		list.add( new CompletionTemplate( "exec",    "\\exec procName ?, ? :( string = '1', int = 99 ) -- make RPC call with 2 parameters", "Execute a Stored Proc using CallableStatement method"));
		list.add( new CompletionTemplate( "\\exec",  "\\exec procName ?, ? :( string = '1', int = 99 ) -- make RPC call with 2 parameters", "Execute a Stored Proc using CallableStatement method"));
		list.add( new CompletionTemplate( "rpc",     "\\rpc procName ?, ? :( string = '1', int = 99 ) -- make RPC call with 2 parameters", "Execute a Stored Proc using CallableStatement method"));
		list.add( new CompletionTemplate( "\\rpc",   "\\rpc procName ?, ? :( string = '1', int = 99 ) -- make RPC call with 2 parameters", "Execute a Stored Proc using CallableStatement method"));
		list.add( new CompletionTemplate( "call",    "\\call procName ?, ? :( string = '1', int = 99 ) -- make RPC call with 2 parameters", "Execute a Stored Proc using CallableStatement method"));
		list.add( new CompletionTemplate( "\\call",  "\\call procName ?, ? :( string = '1', int = 99 ) -- make RPC call with 2 parameters", "Execute a Stored Proc using CallableStatement method"));
		list.add( new CompletionTemplate( "prep",    "\\prep insert into t1 values(?, ?) :( string = '1', int = 99 ) -- Prepared SQL with 2 parameters", "Execute a SQL Statement using java PreparedStatement method"));
		list.add( new CompletionTemplate( "\\prep",  "\\prep insert into t1 values(?, ?) :( string = '1', int = 99 ) -- Prepared SQL with 2 parameters", "Execute a SQL Statement using java PreparedStatement method"));

		list.add( new CompletionTemplate( "loadfile",    "\\loadfile -T tabname filename"));
		list.add( new CompletionTemplate( "\\loadfile",  "\\loadfile -T tabname filename"));

		list.add( new CompletionTemplate( "ddlgen",    "\\ddlgen -t tabname"));
		list.add( new CompletionTemplate( "\\ddlgen",  "\\ddlgen -t tabname"));

		list.add( new CompletionTemplate(":",   ":",   "Show all ':' shorthand completions") );
		list.add( new CompletionTemplate(":s",  ":s",  "Show all schemas") );
		list.add( new CompletionTemplate(":db", ":db", "Show all databases") );
		list.add( new CompletionTemplate(":t",  ":t",  "Show all user tables") );
		list.add( new CompletionTemplate(":v",  ":v",  "Show all user views") );
		list.add( new CompletionTemplate(":st", ":st", "Show all system tables") );
		list.add( new CompletionTemplate(":sv", ":sv", "Show all system views") );
		list.add( new CompletionTemplate(":r",  ":r",  "Refresh the Code Completion") );

		return list;
	}

}

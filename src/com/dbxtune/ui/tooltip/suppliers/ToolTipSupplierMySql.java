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
package com.dbxtune.ui.tooltip.suppliers;

import java.awt.Window;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.dbxtune.ui.autocomplete.CompletionProviderAbstract;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.ConnectionProvider;
import com.dbxtune.utils.StringUtil;

public class ToolTipSupplierMySql
extends ToolTipSupplierAbstractSql
{
	public ToolTipSupplierMySql(Window owner, CompletionProviderAbstract compleationProvider, ConnectionProvider connectionProvider)
	{
		super(owner, compleationProvider, connectionProvider);
	}

	@Override
	public String getName() 
	{
		return "MySql";
	}

	@Override
	public String getToolTipProviderFile() 
	{
		return StringUtil.envVariableSubstitution("${APPL_HOME}/resources/EMPTY_tooltip_provider.xml");
	}

	@Override
	public String getFooter()
	{
		return "<b>Note:</b> The above output was fetched from MySQL table <code>mysql.help_topic, mysql.help_category</code><br>";
	}

	@Override
	public List<TtpEntry> load()
	throws Exception
	{
		// Get parent entries
		List<TtpEntry> list = super.load();

		if (list == null)
			list = new ArrayList<TtpEntry>();
		
		Connection conn = _connectionProvider.getConnection();
		if (conn == null)
			throw new Exception("Connection provider, returned a null connection. Can't continue.");
		
		if ( ! AseConnectionUtils.isConnectionOk(conn, false, null) )
			throw new Exception("Connection provider, returned a connection, which wasn't connected. Can't continue.");

//		RS> Col# Label            JDBC Type Name             Guessed DBMS type
//		RS> ---- ---------------- -------------------------- -----------------
//		RS> 1    help_topic_id    java.sql.Types.INTEGER     INT UNSIGNED     
//		RS> 2    name             java.sql.Types.CHAR        CHAR(64)         
//		RS> 3    help_category_id java.sql.Types.SMALLINT    SMALLINT UNSIGNED
//		RS> 4    description      java.sql.Types.LONGVARCHAR VARCHAR          
//		RS> 5    example          java.sql.Types.LONGVARCHAR VARCHAR          
//		RS> 6    url              java.sql.Types.CHAR        CHAR(128)        		try

		try
		{
//			String sql = "select TOPIC, SECTION, TEXT, SYNTAX from INFORMATION_SCHEMA.HELP";
			String sql = 
				"select c.name, t.name, t.description, t.example, t.url \n" +
				"from   mysql.help_topic t, mysql.help_category c \n" +
				"where  t.help_category_id = c.help_category_id  \n" +
				"";

			Statement stmnt   = conn.createStatement();
			ResultSet rs      = stmnt.executeQuery(sql);

			while (rs.next())
			{
				TtpEntry e = new TtpEntry();
				
				String module      = rs.getString(1);
				String cmdName     = rs.getString(2);
				String description = rs.getString(3);
				String example     = rs.getString(4);
				String sourceUrl   = rs.getString(5);

				e.setModule     (module);
				e.setCmdName    (cmdName);
				e.setDescription("<pre>" + description + "</pre>");
				e.setSourceUrl  (sourceUrl);

				if (example != null && !example.trim().isEmpty())
					e.setExample("<pre>" + example + "</pre>");

				list.add(e);
			}
			rs.close();
			
			return list;
		}
		catch(SQLException ex)
		{
			throw new Exception("Problems getting HELP information from MySQL. Error Number: "+ex.getErrorCode()+", Message: " + ex.getMessage(), ex);
		}
	}

//	@Override
//	public String getToolTipText(RTextArea textArea, MouseEvent e, String word, String fullWord)
//	{
//		if (_connectionProvider != null)
//		{
//			Connection conn = _connectionProvider.getConnection();
//			if (conn == null)
//				return "Not yet Connected";
//			
//			if ( ! AseConnectionUtils.isConnectionOk(conn, false, null) )
//				return "Not Connected";
//
//			StringBuilder sb = new StringBuilder();
//			try
//			{
//				String sql = 
//					"select SECTION, TOPIC, SYNTAX, TEXT " +
//					"from INFORMATION_SCHEMA.HELP \n" +
//					"where UPPER(TOPIC) like UPPER('%"+word+"%')";
//
//				Statement stmnt   = conn.createStatement();
//				ResultSet rs      = stmnt.executeQuery(sql);
//				
//				int rows = 0;
//				while (rs.next())
//				{
//					rows++;
//					if (rows == 1)
//						sb.append("<html>");
//
//					sb.append("<hr>");
//					sb.append("<b>Topic:       </b>").append(rs.getString(2)).append("<br>");
//					sb.append("<b>Section:     </b>").append(rs.getString(1)).append("<br>");
//					sb.append("<b>Description: </b>").append(rs.getString(4)).append("<br>");
//					sb.append("<pre>")               .append(rs.getString(3)).append("</pre><br>");
//					sb.append("\n");
//				}
//				rs.close();
//				if (rows > 0)
//				{
//					sb.append("<hr>");
//					sb.append("<b>Note:</b> The above output was generated by the following SQL Query:<br>");
//					sb.append("<pre>").append(sql).append("</pre><br>");
//					sb.append("</html>");
//				}
//			}
//			catch(SQLException ex)
//			{
//				sb.append("Problems getting current Working Database. Error Number: "+ex.getErrorCode()+", Message: " + ex.getMessage());
//			}
//			return sb.length() == 0 ? null : sb.toString();
//		}
//		return "<html><h2>ToolTipSupplier - H2</h2>"+word+"<br>"+fullWord+"</html>";
//	}
}

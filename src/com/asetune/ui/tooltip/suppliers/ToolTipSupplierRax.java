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
package com.asetune.ui.tooltip.suppliers;

import java.awt.Window;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.asetune.ui.autocomplete.CompletionProviderAbstract;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.ConnectionProvider;

public class ToolTipSupplierRax
extends ToolTipSupplierAbstractSql
{
	public ToolTipSupplierRax(Window owner, CompletionProviderAbstract compleationProvider, ConnectionProvider connectionProvider)
	{
		super(owner, compleationProvider, connectionProvider);
	}

	@Override
	public String getName() 
	{
		return "RAX";
	}

	@Override
	public String getToolTipProviderFile() 
	{
		return null;
	}

	@Override
	public String getFooter()
	{
		return "<b>Note:</b> The above output was fetched from RAX command <code>ra_help</code><br>";
	}

	@Override
	public List<TtpEntry> load()
	throws Exception
	{
		Connection conn = _connectionProvider.getConnection();
		if (conn == null)
			throw new Exception("Connection provider, returned a null connection. Can't continue.");
		
		if ( ! AseConnectionUtils.isConnectionOk(conn, false, null) )
			throw new Exception("Connection provider, returned a connection, which wasn't connected. Can't continue.");

//		1> ra_help
//		RS> Col# Label           JDBC Type Name      Guessed DBMS type
//		RS> ---- --------------- ------------------- -----------------
//		RS> 1    Command Name    java.sql.Types.CHAR char(24)         
//		RS> 2    Parameter Types java.sql.Types.CHAR char(39)         
//		RS> 3    Description     java.sql.Types.CHAR char(234)        

		try
		{
			String sql = "ra_help";

			Statement stmnt   = conn.createStatement();
			ResultSet rs      = stmnt.executeQuery(sql);

			ArrayList<TtpEntry> list = new ArrayList<TtpEntry>();
			while (rs.next())
			{
				String cmd    = rs.getString(1).trim();
				String params = rs.getString(2).trim();
				String desc   = rs.getString(3).trim();
				
				if (params.equals("(none)"))
					params = "";
				else
					params = "("+params+")";
					
				TtpEntry e = new TtpEntry();
				e.setCmdName    (cmd + params);
				e.setModule     ("rax");
				e.setDescription(desc);
//				e.setSyntax     ("<pre>" + syntaxExample + "</pre>" );
				
				list.add(e);
			}
			rs.close();
			
			return list;
		}
		catch(SQLException ex)
		{
			throw new Exception("Problems getting HELP information from RAX. Error Number: "+ex.getErrorCode()+", Message: " + ex.getMessage(), ex);
		}
	}
}

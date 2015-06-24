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

public class ToolTipSupplierH2
extends ToolTipSupplierAbstractSql
{
	public ToolTipSupplierH2(Window owner, CompletionProviderAbstract compleationProvider, ConnectionProvider connectionProvider)
	{
		super(owner, compleationProvider, connectionProvider);
	}

	@Override
	public String getName() 
	{
		return "H2";
	}

	@Override
	public String getToolTipProviderFile() 
	{
		return null;
	}

	@Override
	public String getFooter()
	{
		return "<b>Note:</b> The above output was fetched from H2 table <code>INFORMATION_SCHEMA.HELP</code><br>";
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

		try
		{
			String sql = "select TOPIC, SECTION, TEXT, SYNTAX from INFORMATION_SCHEMA.HELP";

			Statement stmnt   = conn.createStatement();
			ResultSet rs      = stmnt.executeQuery(sql);

			ArrayList<TtpEntry> list = new ArrayList<TtpEntry>();
			while (rs.next())
			{
				TtpEntry e = new TtpEntry();
				e.setCmdName    (rs.getString(1));
				e.setModule     (rs.getString(2));
				e.setDescription(rs.getString(3));
				e.setSyntax     ("<pre>" + rs.getString(4) + "</pre>" );
				
				list.add(e);
			}
			
			return list;
		}
		catch(SQLException ex)
		{
			throw new Exception("Problems getting HELP information from H2. Error Number: "+ex.getErrorCode()+", Message: " + ex.getMessage(), ex);
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

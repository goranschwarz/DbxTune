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
package com.asetune.cm.sqlserver;

import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.asetune.cm.CmToolTipSupplierDefault;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;
import com.asetune.utils.StringUtil;

public class ToolTipSupplierSqlServer
extends CmToolTipSupplierDefault
{
	private static Logger _logger = Logger.getLogger(ToolTipSupplierSqlServer.class);

	public ToolTipSupplierSqlServer(CountersModel cm)
	{
		super(cm);
	}

	@Override
	public String getToolTipTextOnTableColumnHeader(String colName)
	{
		return super.getToolTipTextOnTableColumnHeader(colName);
	}

	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if (_cm == null)
			return null;

		String sql = null;
		
		// Get tip on sql_handle
		if ("sql_handle".equals(colName))
		{
			//Object cellVal = getValueAt(modelRow, modelCol);
			if (cellValue instanceof String)
			{
				if ( _cm.isConnected() )
				{
					if (MainFrame.isOfflineConnected())
					{
					}
					else
					{
						sql = "select text from sys.dm_exec_sql_text("+cellValue+")";
					}
				}
			}
		}

		// Get tip on plan_handle
		if ("plan_handle".equals(colName))
		{
			//Object cellVal = getValueAt(modelRow, modelCol);
			if (cellValue instanceof String)
			{
				if ( _cm.isConnected() )
				{
					if (MainFrame.isOfflineConnected())
					{
					}
					else
					{
						sql = "select query_plan from sys.dm_exec_query_plan("+cellValue+")";
					}
				}
			}
		}

		if (sql != null)
		{
			try
			{
				Connection conn = _cm.getCounterController().getMonConnection();

				StringBuilder sb = new StringBuilder(300);
				sb.append("<html>\n");

				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql);
//				ResultSetMetaData rsmd = rs.getMetaData();
//				int cols = rsmd.getColumnCount();

				sb.append("<pre>\n");
				while (rs.next())
				{
					String c1 = rs.getString(1);
					
					// If it's a XML Showplan
					if (c1 != null && c1.startsWith("<ShowPlanXML"))
					{
//						System.out.println("XML-SHOWPLAN: "+c1);
//						c1 = c1.replace("<", "&lt;").replace(">", "&gt;");
						c1 = StringUtil.xmlFormat(c1);
						c1 = c1.replace("<", "&lt;").replace(">", "&gt;");
					}
					
					// FIXME: translate to a "safe" HTML string 
					sb.append(c1);
				}
				sb.append("<pre>\n");
				sb.append("</html>\n");

				for (SQLWarning sqlw = stmt.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
				{
					// IGNORE: DBCC execution completed. If DBCC printed error messages, contact a user with System Administrator (SA) role.
					if (sqlw.getMessage().startsWith("DBCC execution completed. If DBCC"))
						continue;

					sb = sb.append(sqlw.getMessage()).append("<br>");
				}
				rs.close();
				stmt.close();
				
				return sb.toString();
			}
			catch (SQLException ex)
			{
				_logger.warn("Problems when executing sql for cm='"+_cm.getName()+"', getToolTipTextOnTableCell(colName='"+colName+"', cellValue='"+cellValue+"'): "+sql, ex);
				return "<html>" +  
				       "Trying to get tooltip details for colName='"+colName+"', value='"+cellValue+"'.<br>" +
				       "Problems when executing sql: "+sql+"<br>" +
				       ex.toString() +
				       "</html>";
			}
		}
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
}

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

import java.awt.Desktop;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.List;

import org.apache.log4j.Logger;
import org.fife.ui.autocomplete.Completion;

import com.asetune.Version;
import com.asetune.cm.CmToolTipSupplierDefault;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.showplan.ShowplanHtmlView;
import com.asetune.sql.showplan.ShowplanHtmlView.Type;
import com.asetune.ui.autocomplete.CompletionProviderJdbc;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

public class ToolTipSupplierSqlServer
extends CmToolTipSupplierDefault
{
	private static Logger _logger = Logger.getLogger(ToolTipSupplierSqlServer.class);

	private CompletionProviderJdbc _complProvider;
	
	public ToolTipSupplierSqlServer(CountersModel cm)
	{
		super(cm);

		_complProvider = new CompletionProviderJdbc(MainFrame.getInstance(), MainFrame.getInstance());
		_complProvider.setWildcatdMath(false);
	}

	@Override
	public String getToolTipTextOnTableColumnHeader(String colName)
	{
		return super.getToolTipTextOnTableColumnHeader(colName);
	}

	private String getObjectSchema(DbxConnection conn, String dbname, String objectName)
	{
		String tabOwner = "dbo";
		String sql = "select TABLE_SCHEMA from ["+dbname+"].[INFORMATION_SCHEMA].[TABLES] where [TABLE_CATALOG] = '"+dbname+"' and [TABLE_NAME] = '"+objectName+"'";

		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				tabOwner = rs.getString(1);
		}
		catch (SQLException ex)
		{
			_logger.warn("Problem getting SchemaName from SQL-Server. I will return 'dbo'. SQL='"+sql+"'. Caught="+ex);
			tabOwner = "dbo";
		}
		return tabOwner;
	}
	
	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if (_cm == null)
			return null;

		String sql = null;
		
		// Get tip on sql_handle
		if ("objectName".equalsIgnoreCase(colName))
		{
			//Object cellVal = getValueAt(modelRow, modelCol);
			if (cellValue != null && cellValue instanceof String)
			{
				String objectName = (String)cellValue;

				if ("ObjectName".equalsIgnoreCase(colName))
				{
					DbxConnection conn = _complProvider.getConnectionProvider().getConnection();

					String dbName       = _cm.getAbsString(modelRow, "dbname", false);
//					String tabObjId     = _cm.getAbsString(modelRow, "object_id", false);
					String tabOwnerName = getObjectSchema(conn, dbName, objectName);
					
					List<Completion> list = _complProvider.getTableListWithGuiProgress(conn, dbName, tabOwnerName, objectName);

					if (_logger.isDebugEnabled())
						_logger.debug("ToolTipSupplierSqlServer.getToolTipTextOnTableCell(ObjectName): dbName='"+dbName+"', ownerName='"+tabOwnerName+"', objectName='"+objectName+"', cm='"+_cm.getName()+"'. list.size()="+(list==null?"-null-":list.size())+".");

					if ( list != null )
					{
						if      (list.size() == 0) return "No table information found for table '"+tabOwnerName+"."+objectName+"' in database '"+dbName+"'.";
						else if (list.size() == 1) return list.get(0).getSummary();
						else                       return "Found table information, but I found MORE than 1 table, count="+list.size()+". I can only show info for 1 table. (database='"+dbName+"', table='"+tabOwnerName+"."+objectName+"')";
					}
				}
		}
		}
		
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
						
						StringBuilder qpsb = new StringBuilder(1024);
						Connection conn = _cm.getCounterController().getMonConnection();
						try ( Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql) )
						{
							while(rs.next())
								qpsb.append(rs.getString(1));
						}
						catch(SQLException ex)
						{
							String problem = "Problems getting SQL-Server QueryPlan for '"+cellValue+"', skipping this request. using sql='"+sql+"'"; 
							_logger.warn(problem);
							return problem;
						}

						String queryPlanText = qpsb.toString();
						if (queryPlanText != null && queryPlanText.equalsIgnoreCase("null"))
							queryPlanText = null;

						if (StringUtil.isNullOrBlank(queryPlanText) || queryPlanText.equalsIgnoreCase("null"))
							return "Getting query plan using: sql='"+sql+"' returned NULL. (NO QUERY PLAN was available).";
						
						//-------------------------------------
						// Write the content to a file!
						//-------------------------------------
						File tmpFile = null;
						try
						{
							// put content in a TEMP file
//							tmpFile = createTempFile("ssTune_qp_tooltip_", ".xml", queryPlanText.getBytes()); // NOTE: A Browser is possibly better at reading the XML than any registered app???
							tmpFile = ShowplanHtmlView.createHtmlFile(Type.SQLSERVER, queryPlanText);

							// Compose ToolTip HTML (with content, & a LINK to be opened in "browser")
							String urlStr = ("file:///"+tmpFile);
							try	
							{
								String propName_xmlInline    = Version.getAppName() + ".tooltip.xmlplan.show.inline";
								String propName_autoExternal = Version.getAppName() + ".tooltip.xmlplan.show.auto.externalBrowser";

								boolean showInline     = Configuration.getCombinedConfiguration().getBooleanProperty(propName_xmlInline,    false);
								boolean showAutoExtern = Configuration.getCombinedConfiguration().getBooleanProperty(propName_autoExternal, false);
								
								URL url = new URL(urlStr);
								
								StringBuilder sb = new StringBuilder();
								sb.append("<html>");
								sb.append("<h2>Tooltip for 'SQL-Server Query Plan'</h2>");
								sb.append("<br>");
								sb.append("Using temp file: <code>").append(tmpFile).append("</code><br>");
								sb.append("File Size: <code>").append(StringUtil.bytesToHuman(tmpFile.length(), "#.#")).append("</code><br>");
								sb.append("<a href='").append(OPEN_IN_EXTERNAL_BROWSER + url).append("'>Open in External Browser</a> (registered application for file extention <b>'.html'</b> will be used)<br>");
								sb.append("<br>");
								sb.append("<a href='").append(SET_PROPERTY_TEMP + propName_autoExternal + "=" + (!showAutoExtern) ).append("'>"+(showAutoExtern ? "Disable" : "Enable")+"</a> - Automatically open in Extrnal Browser. (set property <code>"+propName_autoExternal+"="+(!showAutoExtern)+"</code>)<br>");
								sb.append("<a href='").append(SET_PROPERTY_TEMP + propName_xmlInline    + "=" + (!showInline)     ).append("'>"+(showInline     ? "Disable" : "Enable")+"</a> - Show the XML Plan in here. (set property <code>"+propName_xmlInline+"="+(!showInline)+"</code>)<br>");
								if (showAutoExtern)
									sb.append("<h3>Auto open external browser is enabled! (Check the browser for results)</h3>");
								sb.append("<hr>");
								
								if (showInline)
								{
									String formatedQueryPlanText = StringUtil.xmlFormat(queryPlanText);
									formatedQueryPlanText = formatedQueryPlanText.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); 

									sb.append("<pre><code>");
									sb.append(formatedQueryPlanText);
									sb.append("</code></pre>");
								}

								sb.append("</html>");

								if (showAutoExtern)
								{
									if (Desktop.isDesktopSupported())
									{
										Desktop desktop = Desktop.getDesktop();
										if ( desktop.isSupported(Desktop.Action.BROWSE) )
										{
											try
											{
												desktop.browse(tmpFile.toURI());
											}
											catch (Exception ex)
											{
												SwingUtils.showErrorMessage(null, "Problems HTML Showplan", "Problems when open the URL '"+tmpFile+"'.", ex);
											}
										}
									}
								}

								return sb.toString();
							}
							catch (Exception ex) 
							{
								_logger.warn("Problems when open the URL '"+urlStr+"'. Caught: "+ex, ex); 
								return 
									"<html>Problems when open the URL '<code>"+urlStr+"</code>'.<br>"
									+ "Caught: <b>" + ex + "</b><br>"
									+ "<hr>"
									+ "<a href='" + OPEN_IN_EXTERNAL_BROWSER + urlStr + "'>Open tempfile in External Browser</a> (registered application for file extention <b>'.html'</b> will be used)<br>"
									+ "Or copy the above filename, and open it in any application or text editor<br>"
									+ "<html/>";
							}
						}
						catch (Exception ex)
						{
							return "<html>Sorry problems when creating temporary file '"+tmpFile+"'<br>Caught: "+ex+"</html>";
						}
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

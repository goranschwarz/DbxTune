package com.asetune.cm.sqlserver;

import java.awt.event.MouseEvent;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

import com.asetune.cm.CmToolTipSupplierDefault;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;

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
					if (c1.startsWith("<ShowPlanXML"))
					{
//						System.out.println("XML-SHOWPLAN: "+c1);
//						c1 = c1.replace("<", "&lt;").replace(">", "&gt;");
						c1 = XmlFormatter.format(c1);
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

	private static class XmlFormatter
	{
		public static String format(String xml)
		{

			try
			{
				final InputSource src = new InputSource(new StringReader(xml));
				final Node document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src).getDocumentElement();
				final Boolean keepDeclaration = Boolean.valueOf(xml.startsWith("<?xml"));

				// May need this: System.setProperty(DOMImplementationRegistry.PROPERTY,"com.sun.org.apache.xerces.internal.dom.DOMImplementationSourceImpl");

				final DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
				final DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
				final LSSerializer writer = impl.createLSSerializer();

				writer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE); // Set this to true if the output needs to be beautified.
				writer.getDomConfig().setParameter("xml-declaration", keepDeclaration); // Set this to true if the declaration is needed to be outputted.

				return writer.writeToString(document);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}
}

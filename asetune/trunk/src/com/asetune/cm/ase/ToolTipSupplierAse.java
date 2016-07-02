package com.asetune.cm.ase;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;

import javax.swing.Timer;

import org.apache.log4j.Logger;

import com.asetune.cm.CmToolTipSupplierDefault;
import com.asetune.cm.CounterTableModel;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonWaitEventIdDictionary;
import com.asetune.config.dict.RemarkDictionary;
import com.asetune.gui.AsePlanViewer;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.StringUtil;

public class ToolTipSupplierAse 
extends CmToolTipSupplierDefault
{
	private static Logger _logger = Logger.getLogger(ToolTipSupplierAse.class);

//	private long   _lastCalled = 0;
//	private String _currentObjectName = "";
//	Timer          _aseShowplanTimer;

	public ToolTipSupplierAse(CountersModel cm)
	{
		super(cm);
		
//		// Setup timer for not overloading the AseShowplan component
//		_aseShowplanTimer = new Timer(500, new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent paramActionEvent)
//			{
//				_aseShowplanTimer.stop();
//
//System.out.println(">>>>>>>>>>SHOW XML PLAN: objectName='"+_currentObjectName+"'");
//				AsePlanViewer.getInstance().loadXmlFromCache(_currentObjectName);
//			}
//		});
	}

	@Override
	public String getToolTipTextOnTableColumnHeader(String colName)
	{
		return super.getToolTipTextOnTableColumnHeader(colName);
	}

	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
//		_lastCalled = System.currentTimeMillis();

		if (_cm == null)
			return null;
		
		// Get tip on WaitEventID
		if ("WaitEventID".equals(colName))
		{
			//Object cellVal = getValueAt(modelRow, modelCol);
			if (cellValue instanceof Number)
			{
				int waitEventId = ((Number)cellValue).intValue();
				if (waitEventId > 0)
					return MonWaitEventIdDictionary.getInstance().getToolTipText(waitEventId);
			}
		}

		// Get tip on Remark (at least in CMobjectActivity/CM_NAME__OBJECT_ACTIVITY)
		if ("Remark".equals(colName))
		{
			//Object cellVal = getValueAt(modelRow, modelCol);
			if (cellValue instanceof String)
			{
				String key = (String)cellValue;
				if ( ! StringUtil.isNullOrBlank(key) )
					return RemarkDictionary.getInstance().getToolTipText(key);
			}
		}

		// XML ShowPlan
		if ( _cm.isConnected() && ("ObjectName".equalsIgnoreCase(colName) || "procName".equalsIgnoreCase(colName)) )
		{
			//Object cellVal = getValueAt(modelRow, modelCol);
			if (cellValue != null && cellValue instanceof String)
			{
				String objectName = (String)cellValue;
				if (objectName.startsWith("*ss") || objectName.startsWith("*sq"))
				{
					AsePlanViewer.getInstance().loadXmlFromCacheDeferred(objectName);
				}
//System.out.println("colName='"+colName+"', cellValue='"+cellValue+"'");
//				if (objectName.startsWith("*ss") || objectName.startsWith("*sq"))
//				{
//					// No need to show same object again
//					if (objectName.equals(_currentObjectName))
//						return null;
//
//					// Set the new object to show, but show it after 500ms if you move the mouse into a new object the timer restarts
//					_currentObjectName = objectName;
//					if ( ! _aseShowplanTimer.isRunning() )
//						_aseShowplanTimer.start();
//					else
//						_aseShowplanTimer.restart();
//
//System.out.println("SHOW XML PLAN: colName='"+colName+"', objectName='"+objectName+"'");
//				}
			}
		}

		// If we are CONNECTED and we have a USER DEFINED TOOLTIP for this columns
		if (cellValue != null)
		{
			String sql = MainFrame.getUserDefinedToolTip(_cm.getName(), colName);

			// If we reading an offline database, go there to fetch data...
			if ( sql != null && ! _cm.isConnected() )
			{
				// IF SPID, get values from JTable in OFFLINE MODE
				if (    "SPID"          .equalsIgnoreCase(colName) // From a bunch of places
				     || "OldestTranSpid".equalsIgnoreCase(colName) // from CmOpenDatabases
				     || "KPID"          .equalsIgnoreCase(colName) // From a bunch of places
				     || "OwnerPID"      .equalsIgnoreCase(colName) // CmSpinlockActivity
				     || "LastOwnerPID"  .equalsIgnoreCase(colName) // CmSpinlockActivity
				   )
				{
					// Determine the COLUMN name to be used in the search
					String whereColName = "SPID";
					if (    "KPID"          .equalsIgnoreCase(colName) // From a bunch of places
						 || "OwnerPID"      .equalsIgnoreCase(colName) // CmSpinlockActivity
						 || "LastOwnerPID"  .equalsIgnoreCase(colName) // CmSpinlockActivity
					   )
					{
						whereColName = "KPID";
					}
					
					if (MainFrame.isOfflineConnected())
					{
						// FIXME: _counterController is NOT set for UDC Counters (especially when initialized from OfflineStorage)
						CountersModel cm = _cm.getCounterController().getCmByName(CmProcessActivity.CM_NAME);
						TabularCntrPanel tcp = cm.getTabPanel();
						if (tcp != null)
						{
							tcp.tabSelected();
							cm = tcp.getDisplayCm();
							if (cm != null)
							{
								CounterTableModel ctmAbs  = cm.getCounterDataAbs();
								CounterTableModel ctmDiff = cm.getCounterDataDiff();
								CounterTableModel ctmRate = cm.getCounterDataRate();
								if (ctmRate == null)
								{
									return "<html>Counters of type 'rate' was not saved for Performance Counter '"+CmProcessActivity.SHORT_NAME+"'.</html>";
								}
								else
								{
									int cellPidInt = -1;
									if (cellValue instanceof Number)
										cellPidInt = ((Number)cellValue).intValue();
									else
									{
										return "<html>" +
												"Current Cell value '"+cellValue+"' is not a <i>Number</i>.<br>" +
												"The object type is <code>"+cellValue.getClass().getName()+"</code><br>" +
												"It must be of datatype <code>Number</code><br>" +
												"</html>";
									}
										
									int pid_pos = ctmRate.findColumn(whereColName);
									int rowCount = ctmRate.getRowCount();
									for (int r=0; r<rowCount; r++)
									{
										Object rowCellValue = ctmRate.getValueAt(r, pid_pos);
										int rowPidInt = -1;
										if (rowCellValue instanceof Number)
											rowPidInt = ((Number)rowCellValue).intValue();
										else
											continue;
//										System.out.println("CellValue='"+cellValue+"', tableRow="+r+", Value="+ctmRate.getValueAt(r, spid_pos)+", TableObjType="+ctmRate.getValueAt(r, spid_pos).getClass().getName()+", cellValueObjType="+cellValue.getClass().getName());
										
//										if ( cellValue.equals(ctmRate.getValueAt(r, spid_pos)) )
										if ( cellPidInt == rowPidInt )
										{
											StringBuilder sb = new StringBuilder(300);
											sb.append("<html>\n");
											sb.append("<table border=0 cellspacing=0 >\n");
//											sb.append("<table border=1 cellspacing=0 >\n");
//											sb.append("<table BORDER=1 CELLSPACING=0 CELLPADDING=0>\n");

											sb.append("<tr>");
											sb.append("<td nowrap bgcolor=\"#cccccc\"><font color=\"#000000\"><b>").append("Column Name")      .append("</b></font></td>");
											sb.append("<td nowrap bgcolor=\"#cccccc\"><font color=\"#000000\"><b>").append("Absolute Counters").append("</b></font></td>");
											sb.append("<td nowrap bgcolor=\"#cccccc\"><font color=\"#000000\"><b>").append("Diff Counters")    .append("</b></font></td>");
											sb.append("<td nowrap bgcolor=\"#cccccc\"><font color=\"#000000\"><b>").append("Rate Counters")    .append("</b></font></td>");
											sb.append("</tr>\n");

											for (int c=0; c<ctmRate.getColumnCount(); c++)
											{
//												System.out.println("XXXX: colName='"+ctm.getColumnName(c)+"', value='"+ctm.getValueAt(r, c)+"'.");

												if ( (c % 2) == 0 )
													sb.append("<tr bgcolor=\"#ffffff\">"); // white
												else
													sb.append("<tr bgcolor=\"#ffffcc\">"); // light yellow

												sb.append("<td nowrap bgcolor=\"#cccccc\"><font color=\"#000000\"><b>").append(ctmRate.getColumnName(c)).append("</b></font></td>");

												sb.append("<td nowrap>").append(ctmAbs ==null?"":ctmAbs .getValueAt(r, c)).append("</td>");
												sb.append("<td nowrap>").append(ctmDiff==null?"":ctmDiff.getValueAt(r, c)).append("</td>");
												sb.append("<td nowrap>").append(ctmRate==null?"":ctmRate.getValueAt(r, c)).append("</td>");
												sb.append("</tr>\n");
											}
											sb.append("</table>\n");
											sb.append("</html>\n");
											return sb.toString();
										}
									}
								}
//								return "<html>Can't find the SPID '"+cellValue+"' in Performance Counter '"+GetCounters.CM_DESC__PROCESS_ACTIVITY+"'.</html>";
								return "<html>Can't find the "+whereColName+" '"+cellValue+"' in Performance Counter '"+CmProcessActivity.SHORT_NAME+"'.</html>";
							}
						}
					} // end: offline
				} // end: SPID

				return "<html>" +
				       "No runtime tool tip available for '"+colName+"'. <br>" +
				       "Not connected to the monitored server.<br>" +
				       "</html>";
			}

			if (sql != null)
			{
				try
				{
					DbxConnection conn = _cm.getCounterController().getMonConnection();

					StringBuilder sb = new StringBuilder(300);
					sb.append("<html>\n");
//					sb.append("<table BORDER=1 CELLSPACING=0 CELLPADDING=0>\n");
					sb.append("<table border=1>\n");

					PreparedStatement stmt = conn.prepareStatement(sql);
					stmt.setObject(1, cellValue);

					ResultSet rs = stmt.executeQuery();
					ResultSetMetaData rsmd = rs.getMetaData();
					int cols = rsmd.getColumnCount();

					sb.append("<tr>");
					for (int c=1; c<=cols; c++)
						sb.append("<td nowrap>").append(rsmd.getColumnName(c)).append("</td>");
					sb.append("</tr>\n");

					while (rs.next())
					{
						sb.append("<tr>");
						for (int c=1; c<=cols; c++)
							sb.append("<td nowrap>").append(rs.getObject(c)).append("</td>");
						sb.append("</tr>\n");
					}
					sb.append("</table>\n");
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
		}

		return null;
	}
}

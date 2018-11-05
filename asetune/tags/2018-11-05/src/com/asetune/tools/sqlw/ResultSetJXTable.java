package com.asetune.tools.sqlw;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.StringValue;
import org.jdesktop.swingx.table.TableColumnExt;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.focusabletip.FocusableTip;
import com.asetune.gui.swing.DeferredMouseMotionListener;
import com.asetune.utils.Configuration;
import com.asetune.utils.JavaVersion;
import com.asetune.utils.JsonUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
//import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

public class ResultSetJXTable
extends JXTable
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger = Logger.getLogger(ResultSetJXTable.class);

	public final static Color NULL_VALUE_COLOR = new Color(240, 240, 240);

	public static final String  PROPKEY_TABLE_TOOLTIP_SHOW_ALL_COLUMNS = "ResultSetJXTable.table.tooltip.show.all.columns";
	public static final boolean DEFAULT_TABLE_TOOLTIP_SHOW_ALL_COLUMNS = true;

	private Point _lastMouseClick = null;

	private boolean      _tabHeader_useFocusableTips   = true;
	private boolean      _cellContent_useFocusableTips = true;
	private FocusableTip _focusableTip                 = null;

	public Point getLastMouseClickPoint()
	{
		return _lastMouseClick;
	}

	public ResultSetJXTable(TableModel tm)
	{
		super(tm);
		
		// if it's java9 there seems to be some problems with repainting... (if the table is not added to a ScrollPane, which takes upp the whole scroll)
		if (JavaVersion.isJava9orLater())
		{
			_logger.info("For Java-9, add a 'repaint' when the mouse moves. THIS SHOULD BE REMOVED WHEN THE BUG IS FIXED IN SOME JAVA 9 RELEASE.");

			// Add a repaint every 50ms (when the mouse stops moving = no more repaint until we start to move it again)
			// with the second parameter to true: it will only do repaint 50ms after you have stopped moving the mouse.
			addMouseMotionListener(new DeferredMouseMotionListener(50, false)
			{
				@Override
				public void deferredMouseMoved(MouseEvent e)
				{
					//System.out.println("ResultSetJXTable.this.repaint(): "+System.currentTimeMillis());
					ResultSetJXTable.this.repaint();
				}
			});
		}

		addMouseListener(new MouseListener()
		{
			@Override public void mouseReleased(MouseEvent e) {}
			@Override public void mousePressed(MouseEvent e) {}
			@Override public void mouseExited(MouseEvent e) {}
			@Override public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseClicked(MouseEvent e)
			{
				_lastMouseClick = e.getPoint();
			}
		});

		// java.sql.Timestamp format
		@SuppressWarnings("serial")
		StringValue svTimestamp = new StringValue() 
		{
//			DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
			String format = Configuration.getCombinedConfiguration().getProperty("ResultSetJXTable.cellRenderer.format.Timestamp", "yyyy-MM-dd HH:mm:ss.SSS");
			DateFormat df = new SimpleDateFormat(format);
			@Override
			public String getString(Object value) 
			{
				if (value != null && value instanceof java.sql.Timestamp)
				{
    				try 
    				{ 
    					return df.format(value); 
    				}
    				catch(Throwable ignore) 
    				{
    					_logger.warn("Problems in ResultSetJXTable when rendering type '"+value.getClass().getName()+"', object '"+value+"'. returning a toString instead. Caught: "+ignore);
    					return value==null ? ResultSetTableModel.DEFAULT_NULL_REPLACE : value.toString(); 
    				}
				}
				else
				{
					return value==null ? ResultSetTableModel.DEFAULT_NULL_REPLACE : value.toString();
				}
			}
		};
		setDefaultRenderer(java.sql.Timestamp.class, new DefaultTableRenderer(svTimestamp));

		// java.sql.Date format
		@SuppressWarnings("serial")
		StringValue svDate = new StringValue() 
		{
			String format = Configuration.getCombinedConfiguration().getProperty("ResultSetJXTable.cellRenderer.format.Date", "yyyy-MM-dd");
			DateFormat df = new SimpleDateFormat(format);
			@Override
			public String getString(Object value) 
			{
				if (value != null && value instanceof java.sql.Date)
				{
    				try 
    				{ 
    					return df.format(value); 
    				}
    				catch(Throwable ignore) 
    				{
    					_logger.warn("Problems in ResultSetJXTable when rendering type '"+value.getClass().getName()+"', object '"+value+"'. returning a toString instead. Caught: "+ignore);
    					return value==null ? ResultSetTableModel.DEFAULT_NULL_REPLACE : value.toString(); 
    				}
				}
				else
				{
					return value==null ? ResultSetTableModel.DEFAULT_NULL_REPLACE : value.toString();
				}
			}
		};
		setDefaultRenderer(java.sql.Date.class, new DefaultTableRenderer(svDate));

		// java.sql.Time format
		@SuppressWarnings("serial")
		StringValue svTime = new StringValue() 
		{
			String format = Configuration.getCombinedConfiguration().getProperty("ResultSetJXTable.cellRenderer.format.Time", "HH:mm:ss");
			DateFormat df = new SimpleDateFormat(format);
			@Override
			public String getString(Object value) 
			{
				if (value != null && value instanceof java.sql.Time)
				{
    				try 
    				{ 
    					return df.format(value); 
    				}
    				catch(Throwable ignore) 
    				{
    					_logger.warn("Problems in ResultSetJXTable when rendering type '"+value.getClass().getName()+"', object '"+value+"'. returning a toString instead. Caught: "+ignore);
    					return value==null ? ResultSetTableModel.DEFAULT_NULL_REPLACE : value.toString(); 
    				}
				}
				else
				{
					return value==null ? ResultSetTableModel.DEFAULT_NULL_REPLACE : value.toString();
				}
			}
		};
		setDefaultRenderer(java.sql.Time.class, new DefaultTableRenderer(svTime));

		// NULL Values: SET BACKGROUND COLOR
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				// Check NULL value
				String cellValue = adapter.getString();
				if (cellValue == null || ResultSetTableModel.DEFAULT_NULL_REPLACE.equals(cellValue))
					return true;
				
				// Check ROWID Column
				int mcol = adapter.convertColumnIndexToModel(adapter.column);
				String colName = adapter.getColumnName(mcol);
				if (mcol == 0 && ResultSetTableModel.ROW_NUMBER_COLNAME.equals(colName))
					return true;

				return false;
			}
		}, NULL_VALUE_COLOR, null));
	}

	@Override
	public void packAll()
	{
//		super.packAll();
		packAllGrowOnly();
	}
	
	private int _packMaxColWidth = SwingUtils.hiDpiScale(1000);
	public int getPackMaxColWidth() { return _packMaxColWidth; }
	public void setPackMaxColWidth(int maxWidth) { _packMaxColWidth = maxWidth; }

	public void packAllGrowOnly()
	{
		int margin = -1;
		boolean onlyGrowWidth = true;

		for (int c = 0; c < getColumnCount(); c++)
		{
			TableColumnExt ce = getColumnExt(c);

//			int maxWidth = -1;
			int maxWidth = getPackMaxColWidth();
			int beforePackWidth = ce.getPreferredWidth();
			
			packColumn(c, margin, maxWidth);

			if (onlyGrowWidth)
			{
				int afterPackWidth = ce.getPreferredWidth();
				if (afterPackWidth < beforePackWidth)
					ce.setPreferredWidth(beforePackWidth);

				/* Check if the width exceeds the max */
				if (maxWidth != -1 && afterPackWidth > maxWidth)
					ce.setPreferredWidth(maxWidth);
			}
		}
	}

	// 
	// TOOL TIP for: TABLE HEADERS
	//
	@Override
	protected JTableHeader createDefaultTableHeader()
	{
		return new JXTableHeader(getColumnModel())
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getToolTipText(MouseEvent e)
			{
				// Now get the column name, which we point at
				Point p = e.getPoint();
				int index = getColumnModel().getColumnIndexAtX(p.x);
				if ( index < 0 )
					return null;
				
				TableModel tm = getModel();
				if (tm instanceof ResultSetTableModel)
				{
					ResultSetTableModel rstm = (ResultSetTableModel) tm;
					String tooltip = rstm.getToolTipTextForTableHeader(index);

					if (_tabHeader_useFocusableTips)
					{
						if (tooltip != null) 
						{
							if (_focusableTip == null) 
								_focusableTip = new FocusableTip(this);

							_focusableTip.toolTipRequested(e, tooltip);
						}
						// No tool tip text at new location - hide tip window if one is
						// currently visible
						else if (_focusableTip != null) 
						{
							_focusableTip.possiblyDisposeOfTipWindow();
						}
						return null;
					}
					else
						return tooltip;
				}
				return null;
			}
		};
	}

//	// 
//	// TOOL TIP for: CELLS
//	//
//	@Override
//	public String getToolTipText(MouseEvent e)
//	{
//		String tip = null;
//		Point p = e.getPoint();
//		int row = rowAtPoint(p);
//		int col = columnAtPoint(p);
//		if ( row >= 0 && col >= 0 )
//		{
//			col = super.convertColumnIndexToModel(col);
//			row = super.convertRowIndexToModel(row);
//
//			TableModel model = getModel();
//			String colName = model.getColumnName(col);
//			Object cellValue = model.getValueAt(row, col);
//
//			if ( model instanceof ITableTooltip )
//			{
//				ITableTooltip tt = (ITableTooltip) model;
//				tip = tt.getToolTipTextOnTableCell(e, colName, cellValue, row, col);
//
//				// Do we want to use "focusable" tips?
//				if (tip != null) 
//				{
//					if (_focusableTip == null) 
//						_focusableTip = new FocusableTip(this);
//
////						_focusableTip.setImageBase(imageBase);
//					_focusableTip.toolTipRequested(e, tip);
//				}
//				// No tooltip text at new location - hide tip window if one is
//				// currently visible
//				else if (_focusableTip!=null) 
//				{
//					_focusableTip.possiblyDisposeOfTipWindow();
//				}
//				return null;
//			}
//		}
////		if ( tip != null )
////			return tip;
//		return getToolTipText();
//	}
	// 
	// TOOL TIP for: CELL DATA
	//
	@Override
	public String getToolTipText(MouseEvent e)
	{
		String tooltip = null;
		Point p = e.getPoint();
		int row = rowAtPoint(p);
		int col = columnAtPoint(p);
		if ( row >= 0 && col >= 0 )
		{
			col = super.convertColumnIndexToModel(col);
			row = super.convertRowIndexToModel(row);

			TableModel tm = getModel();
			if (tm instanceof ResultSetTableModel)
			{
				ResultSetTableModel rstm = (ResultSetTableModel) tm;
				int sqlType = rstm.getSqlType(col);

				int type = 0;
				if (sqlType == Types.LONGVARBINARY || sqlType == Types.VARBINARY || sqlType == Types.BLOB)
					type = 1;
				else if (sqlType == Types.LONGVARCHAR || sqlType == Types.CLOB)
					type = 2;
				else if (sqlType == Types.CHAR || sqlType == Types.VARCHAR || sqlType == Types.NCHAR || sqlType == Types.NVARCHAR)
					type = 3;
				
				if (type != 0)
//				if (type == 1 || type == 2)
				{
					Object cellValue = tm.getValueAt(row, col);
					if (cellValue == null)
						return null;
					String cellStr   = cellValue.toString();

					if (cellStr.length() >= 100)
					{
						byte[] bytes = (type == 1) ? StringUtil.hexToBytes(cellStr) : cellStr.getBytes();

						tooltip = getContentSpecificToolTipText(cellStr, bytes);
					}
				}
//				else if (type == 3)
//				{
//					Object cellValue = tm.getValueAt(row, col);
//					if (cellValue == null)
//						return null;
//					String cellStr   = cellValue.toString();
//					
//					if (isXml(cellStr))
//					{
//						
//					}
//					else if (isJson(cellStr))
//					{
//						
//					}
//				}
				
				if (tooltip == null)
				{
					boolean useAllColumnsTableTooltip = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_TABLE_TOOLTIP_SHOW_ALL_COLUMNS, DEFAULT_TABLE_TOOLTIP_SHOW_ALL_COLUMNS);
					if (useAllColumnsTableTooltip)
					{
						tooltip = rstm.toHtmlTableString(row, false, true, true); // borders=false, stripedRows=true, addOuterHtmlTags=true
					}
				}

				if (_cellContent_useFocusableTips)
				{
					if (tooltip != null) 
					{
						if (_focusableTip == null) 
							_focusableTip = new FocusableTip(this);

						_focusableTip.toolTipRequested(e, tooltip);
					}
					// No tool tip text at new location - hide tip window if one is currently visible
					else if (_focusableTip != null) 
					{
						_focusableTip.possiblyDisposeOfTipWindow();
					}
					return null;
				}
				else
					return tooltip;

			} // end: ResultSetTableModel
//			else
//			{
//				String colName = tm.getColumnName(col);
//				Object cellValue = tm.getValueAt(row, col);
//			}
		}

		return tooltip;
	}
	
	private String getContentSpecificToolTipText(String cellStr, byte[] bytes)
	{
		if (bytes == null)
			bytes = cellStr.getBytes();

		// Get a MIME type
		ContentInfoUtil util = new ContentInfoUtil();
		ContentInfo info = util.findMatch( bytes );

		// unrecognized MIME Type
		if (info == null)
		{
			// JSON isn't picked up by the ContentInfoUtil
			if (JsonUtils.isPossibleJson(cellStr))
			{
				if (JsonUtils.isJsonValid(cellStr))
				{
					StringBuilder sb = new StringBuilder();
					sb.append("<html>");
					sb.append("Cell content looks like <i>JSON</i>, so displaying it as formated JSON. Origin length="+cellStr.length()+"<br>");
					sb.append("<hr>");
					sb.append("<pre><code>");
					sb.append(JsonUtils.format(cellStr).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
					sb.append("</code></pre>");
					sb.append("</html>");

					return sb.toString();
				}
			}

			// XML that do NOT start with '<?xml ' isn't picked up by the ContentInfoUtil, so lets dig into the String and check if it *might* be a XML content...
			if (StringUtil.isPossibleXml(cellStr))
			{
				StringBuilder sb = new StringBuilder();
				sb.append("<html>");
				sb.append("Cell content looks like <i>XML</i>, so displaying it as formated XML. Origin length="+cellStr.length()+"<br>");
				sb.append("<hr>");
				sb.append("<pre><code>");
				sb.append(StringUtil.xmlFormat(cellStr).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
				sb.append("</code></pre>");
				sb.append("</html>");

				return sb.toString();
			}

			StringBuilder sb = new StringBuilder();
			sb.append("<html>");
			sb.append("Cell content is <i>unknown</i>, so displaying it as raw text. Length="+cellStr.length()+"<br>");
			sb.append("<hr>");
			sb.append("<pre><code>");
			sb.append(cellStr.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
			sb.append("</code></pre>");
			sb.append("</html>");

			return sb.toString();
		}
		else
		{
//			System.out.println("info.getName()           = |" + info.getName()           +"|.");
//			System.out.println("info.getMimeType()       = |" + info.getMimeType()       +"|.");
//			System.out.println("info.getMessage()        = |" + info.getMessage()        +"|.");
//			System.out.println("info.getFileExtensions() = |" + StringUtil.toCommaStr(info.getFileExtensions()) +"|.");

			String mimeType = info.getMimeType();
			if (mimeType != null && mimeType.startsWith("image/"))
			{
				boolean imageToolTipInline = Configuration.getCombinedConfiguration().getBooleanProperty("QueryWindow.tooltip.cellContent.image.inline.", false);
				if (imageToolTipInline)
				{
//					String bytesEncoded = Base64.encode(bytes);
					String bytesEncoded = Base64.encodeBase64String(bytes);
					
					StringBuilder sb = new StringBuilder();
					sb.append("<html>");
					sb.append("Cell content is an image of type: ").append(info).append("<br>");
					sb.append("<hr>");
					sb.append("<img src=\"data:").append(info.getMimeType()).append(";base64,").append(bytesEncoded).append("\" alt=\"").append(info).append("\"/>");
					sb.append("</html>");
//System.out.println("htmlImage: "+sb.toString());

					return sb.toString();
				}
				else
				{
					File tmpFile = null;
					try
					{
						String suffix = null;
						String[] extArr = info.getFileExtensions();
						if (extArr != null && extArr.length > 0)
							suffix = "." + extArr[0];
							
						tmpFile = File.createTempFile("sqlw_image_tooltip_", suffix);
						tmpFile.deleteOnExit();
						FileOutputStream fos = new FileOutputStream(tmpFile);
						fos.write(bytes);
						fos.close();

						boolean imageToolTipInlineLaunchBrowser = Configuration.getCombinedConfiguration().getBooleanProperty("QueryWindow.tooltip.cellContent.image.launchBrowser", false);
						if (imageToolTipInlineLaunchBrowser)
						{
							return openInLocalAppOrBrowser(tmpFile);
						}
						else
						{
							ImageIcon tmpImage = new ImageIcon(bytes);
							int width  = tmpImage.getIconWidth();
							int height = tmpImage.getIconHeight();

							// calculate a new image size max 500x500, but keep image aspect ratio
							Dimension originSize   = new Dimension(width, height);
							Dimension boundarySize = new Dimension(500, 500);
							Dimension newSize      = SwingUtils.getScaledDimension(originSize, boundarySize);

							StringBuilder sb = new StringBuilder();
							sb.append("<html>");
							sb.append("Cell content is an image of type: ").append(info).append("<br>");
							sb.append("Using temp file: <code>").append(tmpFile).append("</code><br>");
							sb.append("Width/Height: <code>").append(originSize.width).append(" x ").append(originSize.height).append("</code><br>");
							sb.append("Size:  <code>").append(StringUtil.bytesToHuman(bytes.length, "#.#")).append("</code><br>");
							sb.append("<hr>");
							sb.append("<img src=\"file:///").append(tmpFile).append("\" alt=\"").append(info).append("\" width=\"").append(newSize.width).append("\" height=\"").append(newSize.height).append("\">");
							sb.append("</html>");

							return sb.toString();
						}
					}
					catch (Exception ex)
					{
						return "<html>Sorry problems when creating temporary file '"+tmpFile+"'<br>Caught: "+ex+"</html>";
					}
				}
			} // end: is "image/"

			else if (info.getName().equals("html"))
			{
				// newer html versions, just use the "default" browser, so create a file, and kick it off
				if (cellStr.startsWith("<!doctype html>"))
				{
					File tmpFile = null;
					try
					{
						tmpFile = File.createTempFile("sqlw_html_tooltip_", ".html");
						tmpFile.deleteOnExit();
						FileOutputStream fos = new FileOutputStream(tmpFile);
						fos.write(bytes);
						fos.close();

						boolean launchBrowserOnHtmlTooltip = Configuration.getCombinedConfiguration().getBooleanProperty("QueryWindow.tooltip.cellContent.html.launchBrowser", true);
						if (launchBrowserOnHtmlTooltip)
						{
							return openInLocalAppOrBrowser(tmpFile);
						}
						else
							return cellStr;
					}
					catch (Exception ex) 
					{
						return "<html>Sorry problems when creating temporary file '"+tmpFile+"'<br>Caught: "+ex+"</html>";
					}
				}
				else
				{
					return cellStr;
				}
			}

			else if (info.getName().equals("xml")) // ?xml version="1.1" encoding="UTF-8"?>  XXXX 
			{
				StringBuilder sb = new StringBuilder();
				sb.append("<html>");
				sb.append("Cell content is <i>XML</i>, so displaying it as formated XML. Origin length="+cellStr.length()+"<br>");
				sb.append("<hr>");
				sb.append("<pre><code>");
				sb.append(StringUtil.xmlFormat(cellStr).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
				sb.append("</code></pre>");
				sb.append("</html>");
                
				return sb.toString();
			}

System.out.println("getContentSpecificToolTipText() unhandled mime type: info.getName()='"+info.getName()+"'.");
			// If "document type" isn't handle above, lets go "generic" and launch a browser with the registered content...
			boolean launchBrowserOnUnknownMimeTypes = Configuration.getCombinedConfiguration().getBooleanProperty("QueryWindow.tooltip.cellContent.unknown.launchBrowser", true);
			if (launchBrowserOnUnknownMimeTypes)
			{
				String[] fileExtentions = info.getFileExtensions();
				String fileExt = "txt";
				if (fileExtentions != null && fileExtentions.length >= 1)
					fileExt = fileExtentions[0];

System.out.println("getContentSpecificToolTipText() unhandled mime type: choosen file extention='"+fileExt+"' all extentions: "+StringUtil.toCommaStr(fileExtentions));
				
				String mimeTypeName = info.getName();
				boolean promptExternalAppForThisMimeType = Configuration.getCombinedConfiguration().getBooleanProperty("QueryWindow.tooltip.cellContent.mimetype."+mimeTypeName+".launchExternalTool.ask", true);
				boolean launchExternalAppForThisMimeType = Configuration.getCombinedConfiguration().getBooleanProperty("QueryWindow.tooltip.cellContent.mimetype."+mimeTypeName+".launchExternalTool", false);
				if (promptExternalAppForThisMimeType)
				{
					String msgHtml = 
							"<html>" +
							   "<h2>Tooltip for MIME Type '"+mimeTypeName+"'</h2>" +
							   "Sorry I have no way to internally show the content type '"+mimeTypeName+"'.<br>" +
							   "Do you want to view the content with any external tool?<br>" +
							   "<ul>" +
							   "  <li><b>Show, This time</b> - Ask me every type if it should be opened in an external tool.</li>" +
							   "  <li><b>Show, Always</b> - Always do this in the future for '"+mimeTypeName+"' mime type (do not show this popup in the future).</li>" +
							   "  <li><b>Never</b> Do NOT show me the content at all (do not show this popup in the future).</li>" +
							   "  <li><b>Cancel</b> Do NOT show me the content this time.</li>" +
							   "</ul>" +
							"</html>";
		
						Object[] options = {
								"Show, This time",
								"Show, Always",
								"Never",
								"Cancel"
								};
						int answer = JOptionPane.showOptionDialog(this, 
							msgHtml,
							"View content in external tool.", // title
							JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE,
							null,     //do not use a custom Icon
							options,  //the titles of buttons
							options[0]); //default button title
		
						if (answer == 0) 
						{
							launchExternalAppForThisMimeType = true;
						}
						else if (answer == 1)
						{
							launchExternalAppForThisMimeType = true;
							Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
							if (conf != null)
							{
								conf.setProperty("QueryWindow.tooltip.cellContent.mimetype."+mimeTypeName+".launchExternalTool.ask", false);
								conf.setProperty("QueryWindow.tooltip.cellContent.mimetype."+mimeTypeName+".launchExternalTool", true);
								conf.save();
							}
						}
						else if (answer == 2)
						{
							launchExternalAppForThisMimeType = false;
							Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
							if (conf != null)
							{
								conf.setProperty("QueryWindow.tooltip.cellContent.mimetype."+mimeTypeName+".launchExternalTool.ask", false);
								conf.setProperty("QueryWindow.tooltip.cellContent.mimetype."+mimeTypeName+".launchExternalTool", false);
								conf.save();
							}
						}
						else
						{
							launchExternalAppForThisMimeType = false;
						}
				}
				if (launchExternalAppForThisMimeType)
				{
					File tmpFile = null;
					try
					{
						tmpFile = File.createTempFile("sqlw_mime_type_"+mimeTypeName+"_tooltip_", "." + fileExt);
						tmpFile.deleteOnExit();
						FileOutputStream fos = new FileOutputStream(tmpFile);
						fos.write(bytes);
						fos.close();

						return openInLocalAppOrBrowser(tmpFile);
					}
					catch (Exception ex) 
					{
						return "<html>Sorry problems when creating temporary file '"+tmpFile+"'<br>Caught: "+ex+"</html>";
					}
				}
				else
				{
					return info.toString();
				}
			}
			else
			{
				return info.toString();
			}
		}
	}

	private String openInLocalAppOrBrowser(File tmpFile)
	{
		// open the default Browser
		if (Desktop.isDesktopSupported())
		{
			Desktop desktop = Desktop.getDesktop();
			if ( desktop.isSupported(Desktop.Action.BROWSE) )
			{
				String urlStr = ("file:///"+tmpFile);
//				String urlStr = ("file:///"+tmpFile).replace('\\', '/');
				try	
				{
					URL url = new URL(urlStr);
					desktop.browse(url.toURI()); 
					return 
						"<html>"
						+ "Opening the contect in the registered application (or browser)<br>"
						+ "The Content were saved in the temporary file: "+tmpFile+"<br>"
						+ "And opened using local application using URL: "+url+"<br>"
						+ "<html/>";
				}
				catch (Exception ex) 
				{
					_logger.warn("Problems when open the URL '"+urlStr+"'. Caught: "+ex); 
					return 
						"<html>Problems when open the URL '"+urlStr+"'.<br>"
						+ "Caught: " + ex + "<br>"
						+ "<html/>";
				}
			}
		}
		return 
			"<html>"
			+ "Desktop browsing is not supported.<br>"
			+ "But the file '"+tmpFile+"' was produced."
			+ "<html/>";
	}

	public enum DmlOperation
	{
		Insert, Update, Delete
	};
	public String getDmlForSelectedRows(DmlOperation dmlOperation)
	{
		int[] selRows = getSelectedRows();

		String tabname = "SOME_TABLE_NAME";
		TableModel tm = getModel();
		if (tm instanceof ResultSetTableModel)
		{
			ResultSetTableModel rstm = (ResultSetTableModel) tm;
			List<String> uniqueTables = rstm.getRsmdReferencedTableNames();
			if (uniqueTables.size() == 1)
				tabname = uniqueTables.get(0);
			else
				for (String tab : uniqueTables)
					tabname = tabname + "/" + tab;
		}
		if (tabname.startsWith("/"))
			tabname = tabname.substring(1); // Remove first "/"
		
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(tabname).append("(");
		for (int c=0; c<getColumnCount(); c++)
			sb.append(getColumnName(c)).append(", ");
		sb.replace(sb.length()-2, sb.length(), ""); // remove last comma
		sb.append(") values(");
		
		String insIntoStr = sb.toString();
		sb.setLength(0);
			
		for (int r : selRows)
		{
			sb.append(insIntoStr);
			for (int c=0; c<getColumnCount(); c++)
			{
				Object val = getValueAt(r, c);
				if (ResultSetTableModel.DEFAULT_NULL_REPLACE.equals(val))
					val = null;
				if (val != null && needsQuotes(r, c, val))
				{
					if (val instanceof String)
					{
						val = val.toString().replace("'", "''");
					}
					sb.append("'").append(val).append("'").append(", ");
				}
				else
					sb.append( val == null ? "NULL" : val).append(", ");
			}
			sb.replace(sb.length()-2, sb.length(), ""); // remove last comma
			sb.append(")\n");
		}
		return sb.toString();
	}

	private boolean needsQuotes(int row, int col, Object val)
	{
		TableModel tm = getModel();
		if (tm instanceof ResultSetTableModel)
		{
			ResultSetTableModel rstm = (ResultSetTableModel) tm;
			int sqlType = rstm.getSqlType(col);

			// Return the "object" via getXXX method for "known" datatypes
			switch (sqlType)
			{
			case java.sql.Types.BIT:           return false;
			case java.sql.Types.TINYINT:       return false;
			case java.sql.Types.SMALLINT:      return false;
			case java.sql.Types.INTEGER:       return false;
			case java.sql.Types.BIGINT:        return false;
			case java.sql.Types.FLOAT:         return false;
			case java.sql.Types.REAL:          return false;
			case java.sql.Types.DOUBLE:        return false;
			case java.sql.Types.NUMERIC:       return false;
			case java.sql.Types.DECIMAL:       return false;
			case java.sql.Types.CHAR:          return true;
			case java.sql.Types.VARCHAR:       return true;
			case java.sql.Types.LONGVARCHAR:   return true;
			case java.sql.Types.DATE:          return true;
			case java.sql.Types.TIME:          return true;
			case java.sql.Types.TIMESTAMP:     return true;
			case java.sql.Types.BINARY:        return false;
			case java.sql.Types.VARBINARY:     return false;
			case java.sql.Types.LONGVARBINARY: return false;
			case java.sql.Types.NULL:          return false;
			case java.sql.Types.OTHER:         return false;
			case java.sql.Types.JAVA_OBJECT:   return false;
			case java.sql.Types.DISTINCT:      return false;
			case java.sql.Types.STRUCT:        return false;
			case java.sql.Types.ARRAY:         return false;
			case java.sql.Types.BLOB:          return false;
			case java.sql.Types.CLOB:          return true;
			case java.sql.Types.REF:           return false;
			case java.sql.Types.DATALINK:      return false;
			case java.sql.Types.BOOLEAN:       return false;

			//------------------------- JDBC 4.0 -----------------------------------
			case java.sql.Types.ROWID:         return false;
			case java.sql.Types.NCHAR:         return true;
			case java.sql.Types.NVARCHAR:      return true;
			case java.sql.Types.LONGNVARCHAR:  return true;
			case java.sql.Types.NCLOB:         return true;
			case java.sql.Types.SQLXML:        return true;

			//------------------------- UNHANDLED TYPES  ---------------------------
			default:
				return false;
			}
		}
		
		if (val instanceof String)
			return true;
		return false;
	}

}
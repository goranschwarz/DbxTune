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

import javax.swing.ImageIcon;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

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
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

public class ResultSetJXTable
extends JXTable
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger = Logger.getLogger(ResultSetJXTable.class);

	public final static Color NULL_VALUE_COLOR = new Color(240, 240, 240);

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
				
				if (type != 0)
				{
					Object cellValue = tm.getValueAt(row, col);
					if (cellValue == null)
						return null;
					String cellStr   = cellValue.toString();

					byte[] bytes = type == 2 ? cellStr.getBytes() : StringUtil.hexToBytes(cellStr);

					tooltip = getContentSpecificToolTipText(cellStr, bytes);
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
					String bytesEncoded = Base64.encode(bytes);
					
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

			return info.toString();
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

}
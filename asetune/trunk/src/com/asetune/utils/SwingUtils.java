/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.utils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

public class SwingUtils
{
	private static Logger _logger = Logger.getLogger(SwingUtils.class);

	public static void printComponents(JComponent c, String text)
	{
		Component[] comp = c.getComponents();
		for (int i=0; i<comp.length; i++)
		{
			System.out.println(text+".comp["+i+"].type="+comp[i].getClass().getName());
//			if (comp[i] instanceof JPanel)
//			{
//				JPanel jp = (JPanel) comp[i]; 
//				System.out.println("JPanel="+jp);
//			}
		}
	}

	public static void printParents(Component c, String text)
	{
		for (int i=0; c!=null; i++)
		{
			Component parent = c.getParent();
			if (parent != null)
			{
				System.out.println(text + " .depth["+i+"].parentClassName="+parent.getClass().getName());
			}
			c = parent;
		}
	}


	// INFO panels
	public static void showInfoMessage(String title, String msg)
	{
		showMessage(null, Level.INFO, title, msg, null);
	}
	public static void showInfoMessage(Component owner, String title, String msg)
	{
		showMessage(owner, Level.INFO, title, msg, null);
	}

	
	// WARN panels
	public static void showWarnMessage(String title, String msg, Throwable exception)
	{
		showMessage(null, Level.WARNING, title, msg, exception);
	}
	public static void showWarnMessage(Component owner, String title, String msg, Throwable exception)
	{
		showMessage(owner, Level.WARNING, title, msg, exception);
	}

	
	// ERROR panels
	public static void showErrorMessage(String title, String msg, Throwable exception)
	{
		showMessage(null, Level.SEVERE, title, msg, exception);
	}
	public static void showErrorMessage(Component owner, String title, String msg, Throwable exception)
	{
		showMessage(owner, Level.SEVERE, title, msg, exception);
	}

	
	// panels
	public static void showMessage(Component owner, final Level errorLevel, final String title, final String msg, final Throwable exception) 
	{
		if (owner != null)
		{
			if ( ! (owner instanceof JFrame) )
			{
				owner = JOptionPane.getFrameForComponent(owner);
			}
		}

		final String category;
		if      (errorLevel.equals(Level.INFO))    category = "Information";
		else if (errorLevel.equals(Level.WARNING)) category = "Warning";
		else                                       category = "Error";

		
		final Component finalOwner = owner;
		Runnable doRun = new Runnable()
		{
			@Override
			public void run()
			{
				JXErrorPane.setDefaultLocale(Locale.ENGLISH);
				JXErrorPane errorPane = new JXErrorPane();
				ErrorInfo info = new ErrorInfo(title, msg, null, category, exception, errorLevel, null);
				errorPane.setErrorInfo(info);
				JDialog dialog = JXErrorPane.createDialog(finalOwner, errorPane);
				dialog.pack();
				dialog.setTitle(title);
				dialog.setVisible(true);
			}
		};
		if (SwingUtils.isEventQueueThread())
			doRun.run();
		else
		{
			try
			{
				SwingUtilities.invokeAndWait(doRun);
			}
			catch (InterruptedException e)
			{
				_logger.error("Problems showing message. Caught: "+e, e);
			}
			catch (InvocationTargetException e)
			{
				_logger.error("Problems showing message. Caught: "+e, e);
			}
		}
	}

	public static void centerWindow(Component frame)
	{
		//Center the window
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = frame.getSize();
		if (frameSize.height > screenSize.height)
		{
			frameSize.height = screenSize.height;
		}
		if (frameSize.width > screenSize.width)
		{
			frameSize.width = screenSize.width;
		}
		frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
	}

	public static void setLocationNearTopLeft(Component parent, Component child)
	{
		setLocationNearTopLeft(parent, child, 20, 40);
	}
	public static void setLocationNearTopLeft(Component parent, Component child, int x, int y)
	{
		if (parent != null)
		{
			Point p = parent.getLocationOnScreen();
			p.x += x;
			p.y += y;
			child.setLocation(p);
		}
		else
			centerWindow(child);
//		child.setLocationRelativeTo(parent);
//		child.setLocationByPlatform(true);
	}

	public static JButton makeToolbarButton(Class<?> clazz, String imageName, String actionCommand, ActionListener al, String toolTipText, String altText)
	{
		// Look for the image.
		String imgLocation = "images/" + imageName;
		URL imageURL = clazz.getResource(imgLocation);

		// Create and initialize the button.
		JButton button = new JButton();
		button.setActionCommand(actionCommand);
		button.setToolTipText(toolTipText);
//		button.addActionListener(this);

		if (al != null)
			button.addActionListener(al);

		if (imageURL != null)
		{ // image found
			button.setIcon(new ImageIcon(imageURL, altText));
		}
		else
		{ // no image found
			button.setText(altText);
			_logger.error("Toolbar Resource not found '"+imgLocation+"', url='"+imageURL+"'.");
		}

		return button;
	}

	public static ImageIcon readImageIcon(Class<?> clazz, String filename)
	{
//		URL url = MainFrame.class.getResource("images/" + filename);
		URL url = clazz.getResource(filename);
//		System.out.println("---->>>>>>>>>>>>>>>>>> Using the icon '"+url+"'.");
		if (url == null)
		{
			_logger.error("Cant find the resource for class='"+clazz+"', filename='"+filename+"'.");
			return null;
		}

		return new ImageIcon(Toolkit.getDefaultToolkit().getImage(url));
	}

	
	/** helper method to create a JPanel */
	public static JPanel createPanel(String title, boolean createBorder) 
	{
		JPanel panel = new JPanel();
		if (createBorder)
		{
			Border border = BorderFactory.createTitledBorder(title);
			panel.setBorder(border);
//			panel.setAlignmentX(110);
//			panel.setAlignmentY(110);
		}
		return panel;
	}

	public static void calcColumnWidths(JTable table)
	{
		calcColumnWidths(table, 0, false);
	}

	public static void calcColumnWidths(JTable table, int onlyXLastRows, boolean onlyIncreaseWith)
	{
		JTableHeader header = table.getTableHeader();
		TableCellRenderer defaultHeaderRenderer = null;
		if (header != null)
			defaultHeaderRenderer = header.getDefaultRenderer();
		TableColumnModel columns = table.getColumnModel();
		TableModel data = table.getModel();
		int margin = columns.getColumnMargin(); // only JDK1.3
		int totalRowCount = data.getRowCount();
		int stopAtRow = 0;
		if (onlyXLastRows > 0)
		{
			stopAtRow = Math.max(stopAtRow, (totalRowCount-onlyXLastRows));
		}
		int totalWidth = 0;
		for (int i = columns.getColumnCount() - 1; i >= 0; --i)
		{
			TableColumn column = columns.getColumn(i);
			int columnIndex = column.getModelIndex();
			int width = -1;
			TableCellRenderer h = column.getHeaderRenderer();
			if (h == null)
				h = defaultHeaderRenderer;
			Component columnHeader = null;
			if (h != null) // Not explicitly impossible
			{
				columnHeader = h.getTableCellRendererComponent(table, 
					column.getHeaderValue(), false, false, -1, i);
				width = columnHeader.getPreferredSize().width;
			}
			for (int row = totalRowCount - 1; row >= stopAtRow; --row)
			{
				TableCellRenderer r = table.getCellRenderer(row, i);
				Component c = r.getTableCellRendererComponent(table, 
					data.getValueAt(row, columnIndex), false, false, row, i);
				width = Math.max(width, c.getPreferredSize().width);
			}
			if (width >= 0)
			{
				column.setPreferredWidth(width + margin); // <1.3: without margin
				// The below didnt seem to work that well
				//if (onlyIncreaseWith && columnHeader != null)
				//{
				//	Dimension dim = columnHeader.getPreferredSize();
				//	dim.width = width + margin;
				//	columnHeader.setPreferredSize(dim);
				//}
			}
			else
				; // ???
			totalWidth += column.getPreferredWidth();
		}
	}
	
	public static void setCaretToLineNumber(JTextArea text, int linenumber) 
	{
		text.setCaretPosition(0);
		if (linenumber<2) 
			return;

		StringTokenizer st = new StringTokenizer(text.getText(),"\n",true);
		int count = 0;
		int countRowAfter = 0;
		while (st.hasMoreTokens() & (linenumber>1))
		{
			String s = st.nextToken();
			count += s.length();
			if (s.equals("\n")) 
				linenumber--;
		}
		// Look for next row aswell, this so we can "mark" the linenumber
		if (st.hasMoreTokens())
		{
			String s = st.nextToken();
			countRowAfter = count + s.length();
		}

		text.setCaretPosition(count);
		text.select(count, countRowAfter);
	}

	
	/**
	 * If current thread is the Event Queue Dispatch thread, return true.
	 * <p>
	 * For the moment I just check for the thread name 'AWT-EventQueue'<br>
	 * But there might be other checks in the future.
	 * 
	 * @return 
	 */
	public static boolean isEventQueueThread()
	{
		String threadName = Thread.currentThread().getName();

		if ( threadName.startsWith("AWT-EventQueue") )
			return true;

//		SwingUtilities.isEventDispatchThread();
		return false;
	}

	
	/**
	 * Parse a String with Color specification and return a Color.
	 * <p>
	 * If the colorStr can't be parsed, the defaultColor is returned.
	 * 
	 * @param colorStr input string in the form:
	 * <ul>
	 *   <li>integer</li>
	 *   <li>rrr.ggg.bbb[.aaa] Red, Green, Blue, Alpha should be in the range on 0-255</li>
	 *   <li>rrr,ggg,bbb[,aaa] Red, Green, Blue, Alpha should be in the range on 0-255</li>
	 *   <li>#rrggbb[aa] Red, Green, Blue, Alpha is in hex</li>
	 *   <li>0xrrggbb[aa] Red, Green, Blue, Alpha is in hex</li>
	 *   <li>java predefined Color: WHITE, LIGHT_GRAY, GRAY, DARK_GRAY, BLACK, RED, PINK, ORANGE, YELLOW, GREEN, MAGENTA, CYAN, BLUE</li>
	 * </ul>
	 * @param defaultColor A color if the input string can't be parsed
	 * @return Color
	 */
	public static Color parseColor(String colorStr, Color defaultColor)
	{
		try
		{
			return parseColor(colorStr);
		}
		catch (ParseException e)
		{
			_logger.debug(e.toString());
			return defaultColor;
		}
	}
	
	/**
	 * Parse a String with Color specification and return a Color.
	 * <p>
	 * If the colorStr can't be parsed, the defaultColor is returned.
	 * 
	 * @param colorStr input string in the form:
	 * <ul>
	 *   <li>integer</li>
	 *   <li>rrr.ggg.bbb[.aaa] Red, Green, Blue, Alpha should be in the range on 0-255</li>
	 *   <li>rrr,ggg,bbb[,aaa] Red, Green, Blue, Alpha should be in the range on 0-255</li>
	 *   <li>#rrggbb[aa] Red, Green, Blue, Alpha is in hex</li>
	 *   <li>0xrrggbb[aa] Red, Green, Blue, Alpha is in hex</li>
	 *   <li>0xrrggbb[aa] Red, Green, Blue, Alpha is in hex</li>
	 *   <li>java predefined Color: WHITE, LIGHT_GRAY, GRAY, DARK_GRAY, BLACK, RED, PINK, ORANGE, YELLOW, GREEN, MAGENTA, CYAN, BLUE</li>
	 * </ul>
	 * @param defaultColor A color if the input string can't be parsed
	 * @return Color
	 * @throws ParseException in case of parsing problems.
	 */
	public static Color parseColor(String colorStr)
	throws ParseException
	{
		if (colorStr == null)
			throw new ParseException("Color string is null.", -1);

		colorStr = colorStr.trim();
		if (colorStr.equals(""))
			throw new ParseException("Color string is empty.", -1);

		// -----------------------------------------
		// try: normal integer 
		// -----------------------------------------
		try	
		{ 
			int colorInt = Integer.parseInt(colorStr); 
			return new Color(colorInt);
		}
		catch (NumberFormatException ignore) {}

		// -----------------------------------------
		// try: rrr.ggg.bbb[.aaa] || rrr,ggg,bbb[,aaa]
		// -----------------------------------------
		if (colorStr.indexOf(".") >= 0 || colorStr.indexOf(",") >= 0)
		{
			String[] sa = null;
			int[]    ia = new int[4];

			if (colorStr.indexOf(".") >= 0)
				sa = colorStr.split("\\.");
			else
				sa = colorStr.split(",");

			if ( ! (sa.length == 3 || sa.length == 4) )
				throw new ParseException("Color string '"+colorStr+"' does not have two or three divider characters of '.' or ','", -1);

			ia[3] = 255; // set alpha to default
			for(int i=0; i<sa.length; i++)
			{
				sa[i] = sa[i].trim();
				try	
				{ 
					ia[i] = Integer.parseInt(sa[i]); 
				}
				catch (NumberFormatException ignore) 
				{
					throw new ParseException("Color string '"+colorStr+"' has a non number in the "+(i+1)+" field.", i);
				}

				if (ia[i] > 255)
					throw new ParseException("Color string '"+colorStr+"' has a 'to big' number in the "+(i+1)+" field. max is 255.", i);
			}
			return new Color(ia[0], ia[1], ia[2], ia[3]);
		}

		// -----------------------------------------
		// try: #rrggbb[aa] || 0xrrggbb[aa]
		// -----------------------------------------
		if (colorStr.startsWith("#") || colorStr.startsWith("0x"))
		{
			if (colorStr.startsWith("#"))
				colorStr = colorStr.substring(1);
			else
				colorStr = colorStr.substring(2);

			if ( ! (colorStr.length() == 6 || colorStr.length() == 8) )
				throw new ParseException("Color string '"+colorStr+"' has to be of the length 6 (or 8 if you want to have alpha).", -1);

			try
			{
				int r = 0, g = 0, b = 0, a = 255;
				r = Integer.parseInt(colorStr.substring(0,2), 16);
				g = Integer.parseInt(colorStr.substring(2,4), 16);
				b = Integer.parseInt(colorStr.substring(4,6), 16);
				if(colorStr.length() > 6)
					a = Integer.parseInt(colorStr.substring(6, 8), 16);
				
				return new Color(r, g, b, a);
			}
			catch (NumberFormatException ignore)
			{
				throw new ParseException("Color string '"+colorStr+"' has a non number in the field.", -1);
			}
		}
		
		// -----------------------------------------
		// try: WHITE, LIGHT_GRAY, GRAY, DARK_GRAY, BLACK, RED, PINK, ORANGE, YELLOW, GREEN, MAGENTA, CYAN, BLUE
		// -----------------------------------------
		if (colorStr.equalsIgnoreCase("WHITE"))      return Color.WHITE;
		if (colorStr.equalsIgnoreCase("LIGHT_GRAY")) return Color.LIGHT_GRAY;
		if (colorStr.equalsIgnoreCase("GRAY"))       return Color.GRAY;
		if (colorStr.equalsIgnoreCase("DARK_GRAY"))  return Color.DARK_GRAY;
		if (colorStr.equalsIgnoreCase("BLACK"))      return Color.BLACK;
		if (colorStr.equalsIgnoreCase("RED"))        return Color.RED;
		if (colorStr.equalsIgnoreCase("PINK"))       return Color.PINK;
		if (colorStr.equalsIgnoreCase("ORANGE"))     return Color.ORANGE;
		if (colorStr.equalsIgnoreCase("YELLOW"))     return Color.YELLOW;
		if (colorStr.equalsIgnoreCase("GREEN"))      return Color.GREEN;
		if (colorStr.equalsIgnoreCase("MAGENTA"))    return Color.MAGENTA;
		if (colorStr.equalsIgnoreCase("CYAN"))       return Color.CYAN;
		if (colorStr.equalsIgnoreCase("BLUE"))       return Color.BLUE;

		// -----------------------------------------
		// OUT of options.
		// -----------------------------------------
		
		throw new ParseException("Color string '"+colorStr+"' can't be parsed. I tried 'int' & 'r,g,b[,a]|r.g.b[.a]' & '#rrggbb[aa]|0xrrggbb[aa]' & 'java colors' (out of parser implementations).", -1);
	}

	public static String tableToString(TableModel model, int justRowNumber)
	{
		return tableToString(model, false, null, null, justRowNumber, justRowNumber+1);
	}
	public static String tableToString(TableModel model)
	{
		return tableToString(model, false, null, null, -1, -1);
	}
	public static String tableToString(TableModel model, boolean stripHtml, String[] prefixColName, Object[] prefixColData)
	{
		return tableToString(model, stripHtml, prefixColName, prefixColData, -1, -1);
	}
	/**
	 * Turn a JTable's TableModel into a String table, can be used for putting into the copy/paste buffer.
	 * 
	 * @param model a JTable's TableModel
	 * @param stripHtml If cell value contains html tags, remove them...
	 * @param prefixColName Optional Column Names to be added as "prefix" columns
	 * @param prefixColData Optional Column Data to be added as "prefix" columns, the value will be repeated for every row
	 * @param firstRow Starting row number in the table. If this is -1, it means start from row 0
	 * @param lastRow Last row number in the table. If this is -1, it means 'to the end of the table'
	 * 
	 * @return a String in a table format
	 * <p>
	 * <pre>
	 * +-------+-------+-------+----------+
	 * |SPID   |KPID   |BatchID|LineNumber|
	 * +-------+-------+-------+----------+
	 * |38     |2687017|2      |1         |
	 * |38     |2687017|2      |1         |
	 * +-------+-------+-------+----------+
	 * Rows 2
	 * </pre>
	 */
	public static String tableToString(TableModel model, boolean stripHtml, String[] prefixColName, Object[] prefixColData, int firstRow, int lastRow)
	{
		String colSepOther = "+";
		String colSepData  = "|";
		String lineSpace   = "-";
		String newLine     = "\n";

		// first copy the information to Array list
		// This was simples to do if we want to add pre/post columns...
		ArrayList<String>       tableHead = new ArrayList<String>();
		ArrayList<List<Object>> tableData = new ArrayList<List<Object>>();

		StringBuilder sb = new StringBuilder();

		boolean doPrefix = false;
		if (prefixColName != null && prefixColData != null)
		{
			if (prefixColName.length != prefixColData.length)
				throw new IllegalArgumentException("tableToString(): prefixColName.length="+prefixColName.length+" is NOT equal prefixColData.length="+prefixColData.length);
			doPrefix = true;
		}

		int cols = model.getColumnCount();
//		int rows = model.getRowCount();
		if (firstRow < 0) firstRow = 0;
		if (lastRow  < 0) lastRow = model.getRowCount();
		int copiedRows = 0;

		//------------------------------------
		// Copy COL NAMES
		//------------------------------------
		if (doPrefix)
			for (int c=0; c<prefixColName.length; c++)
				tableHead.add(prefixColName[c]);

		for (int c=0; c<cols; c++)
			tableHead.add(model.getColumnName(c));

		//------------------------------------
		// Copy ROWS (from firstRow to lastRow)
		//------------------------------------
		for (int r=firstRow; r<lastRow; r++)
		{
			ArrayList<Object> row = new ArrayList<Object>();
			if (doPrefix)
				for (int c=0; c<prefixColData.length; c++)
					row.add(prefixColData[c]);

			for (int c=0; c<cols; c++)
			{
				Object obj = model.getValueAt(r, c);
				
				// Strip of '\n' at the end of Strings
				if (obj != null && obj instanceof String)
				{
					String str = (String) obj;
					if (str.endsWith("\n"))
						obj = str.subSequence(0, str.length()-1);
				}
				row.add(obj);
			}
			
			tableData.add(row);
			copiedRows++;
		}

		// Add prefixColCount to cols
		if (doPrefix)
			cols += prefixColName.length;

		//------------------------------------
		// Get MAX column length and store in colLength[]
		//------------------------------------
		int[] colLength = new int[cols];
		for (int c=0; c<cols; c++)
		{
			int maxLen = 0;

			// ColNames
			String cellName = tableHead.get(c);
			maxLen = Math.max(maxLen, cellName.length());
			
			// All the rows
			for (int r=0; r<copiedRows; r++)
			{
//				ArrayList row = tableData.get(r);
//				Object cellObj = row.get(c);
				
				Object cellObj = tableData.get(r).get(c);
//				Object cellObj = model.getValueAt(r, c);
				String cellStr = cellObj == null ? "" : cellObj.toString();
				maxLen = Math.max(maxLen, cellStr.length());
			}
			
			colLength[c] = maxLen;
		}


		//-------------------------------------------
		// Print the TABLE HEAD
		//-------------------------------------------
		// +------+------+-----+\n
		for (int c=0; c<cols; c++)
		{
			String line = StringUtil.replicate(lineSpace, colLength[c]);
			sb.append(colSepOther).append(line);
		}
		sb.append(colSepOther).append(newLine);

		// |col1|col2   |col3|\n
		for (int c=0; c<cols; c++)
		{
			String cellName = tableHead.get(c);
//			String cellName = model.getColumnName(c);
			String data = StringUtil.fill(cellName, colLength[c]);
			sb.append(colSepData).append(data);
		}
		sb.append(colSepData).append(newLine);

		// +------+------+-----+\n
		for (int c=0; c<cols; c++)
		{
			String line = StringUtil.replicate(lineSpace, colLength[c]);
			sb.append(colSepOther).append(line);
		}
		sb.append(colSepOther).append(newLine);

		
		//-------------------------------------------
		// Print the TABLE DATA
		//-------------------------------------------
		for (int r=0; r<copiedRows; r++)
		{
			// |col1|col2   |col3|\n
			for (int c=0; c<cols; c++)
			{
				Object cellObj = tableData.get(r).get(c);
//				Object cellObj = model.getValueAt(r, c);
				String cellStr = cellObj == null ? "" : cellObj.toString();
				if (stripHtml)
					cellStr = StringUtil.stripHtml(cellStr);

				String data = StringUtil.fill(cellStr, colLength[c]);
				sb.append(colSepData).append(data);
			}
			sb.append(colSepData).append(newLine);
		}

		//-------------------------------------------
		// Print the TABLE FOOTER
		//-------------------------------------------
		// +------+------+-----+\n
		for (int c=0; c<cols; c++)
		{
			String line = StringUtil.replicate(lineSpace, colLength[c]);
			sb.append(colSepOther).append(line);
		}
		sb.append(colSepOther).append(newLine);
		sb.append("Rows ").append(copiedRows).append(newLine);
		
		return sb.toString();
	}

	/**
	 * Convenience method to detect dataChanged table event type.
	 * 
	 * @param e the event to examine.
	 * @return true if the event is of type dataChanged, false else.
	 */
	public static boolean isDataChanged(TableModelEvent e) 
	{
		if (e == null)
			return false;

		return e.getType()     == TableModelEvent.UPDATE 
		    && e.getFirstRow() == 0 
		    && e.getLastRow()  == Integer.MAX_VALUE;
	}

	/**
	 * Convenience method to detect update table event type.
	 * 
	 * @param e the event to examine.
	 * @return true if the event is of type update and not dataChanged, false else.
	 */
	public static boolean isUpdate(TableModelEvent e) 
	{
		if (isStructureChanged(e))
			return false;

		return e.getType()   == TableModelEvent.UPDATE
		    && e.getLastRow() < Integer.MAX_VALUE;
	}

	/**
	 * Convenience method to detect a structureChanged table event type.
	 * 
	 * @param e the event to examine.
	 * @return true if the event is of type structureChanged or null, false
	 *         else.
	 */
	public static boolean isStructureChanged(TableModelEvent e) 
	{
		return e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW;
	}

	/**
	 * Copy the table row by row, column by column.<br>
	 * So this is NOT a shallow copy...
	 * 
	 * @param copyFrom a table model
	 * @return a new DefaultTableModel with all values copied from the input
	 */
	public static DefaultTableModel copyTableModel(TableModel copyFrom)
	{
		if (copyFrom == null)
			throw new RuntimeException("copyTableModel(), Input parameter copyFrom can't be null.");

		DefaultTableModel newModel = new DefaultTableModel();

		newModel.setRowCount(   copyFrom.getRowCount());
		newModel.setColumnCount(copyFrom.getColumnCount());

		// Copy header
		Vector<String> colHeader = new Vector<String>();
		for (int c=0; c<copyFrom.getColumnCount(); c++)
			colHeader.add(copyFrom.getColumnName(c));
		newModel.setColumnIdentifiers(colHeader);

		// Copy the table row by row, column by column
		for (int r=0; r<copyFrom.getRowCount(); r++)
			for (int c=0; c<copyFrom.getColumnCount(); c++)
				newModel.setValueAt(copyFrom.getValueAt(r, c), r, c);

		return newModel;
	}


}

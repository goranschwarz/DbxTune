/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.utils;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

import asemon.gui.MainFrame;

public class SwingUtils
{
	private static Logger _logger = Logger.getLogger(SwingUtils.class);

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
	public static void showMessage(Component owner, Level errorLevel, String title, String msg, Throwable exception) 
	{
		if (owner != null)
		{
			if ( ! (owner instanceof JFrame) )
			{
				owner = JOptionPane.getFrameForComponent(owner);
			}
		}

		String category = null;
		if      (errorLevel.equals(Level.INFO))    category = "Information";
		else if (errorLevel.equals(Level.WARNING)) category = "Warning";
		else                                       category = "Error";

		JXErrorPane.setDefaultLocale(Locale.ENGLISH);
		JXErrorPane errorPane = new JXErrorPane();
		ErrorInfo info = new ErrorInfo(title, msg, null, category, exception, errorLevel, null);
		errorPane.setErrorInfo(info);
		JDialog dialog = JXErrorPane.createDialog(owner, errorPane);
		dialog.setTitle(title);
		dialog.setVisible(true);
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

	public static JButton makeToolbarButton(Class clazz, String imageName, String actionCommand, ActionListener al, String toolTipText, String altText)
	{
		// Look for the image.
		String imgLocation = "images/" + imageName + ".gif";
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

	public static ImageIcon readImageIcon(Class clazz, String filename)
	{
//		URL url = MainFrame.class.getResource("./images/" + filename);
		URL url = clazz.getResource(filename);
//		System.out.println("---->>>>>>>>>>>>>>>>>> Using the icon '"+url+"'.");
		if (url == null)
		{
			_logger.info("Cant find the resource for class='"+clazz+"', filename='"+filename+"'.");
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
}

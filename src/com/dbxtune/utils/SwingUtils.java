/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdesktop.swingx.JXEditorPane;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.swing.GPanel;

import net.java.balloontip.BalloonTip;
import net.java.balloontip.TableCellBalloonTip;
import net.java.balloontip.positioners.LeftAbovePositioner;
import net.java.balloontip.styles.EdgedBalloonStyle;
import net.java.balloontip.utils.TimingUtils;
import net.miginfocom.swing.MigLayout;

public class SwingUtils
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static boolean isHiDpi()
	{
		float scale = getHiDpiScale();
		return scale >= 1.5f;
	}

	public static float getHiDpiScale()
	{
		if (GraphicsEnvironment.isHeadless()) 
			return -1.0f;

		String hiDpiScale = System.getProperty("SwingUtils.hiDpiScale");
		if (hiDpiScale != null)
		{
			try { return Float.parseFloat(hiDpiScale); }
			catch(NumberFormatException e) { _logger.error("Problems parsing the property 'SwingUtils.hiDpiScale', allowed values is any float value (like 2.0 for 200% scaling). Continuing with normal processing inside SwingUtils.getHiDpiScale()"); }
		}
		
		// use system scale factor(s)
		// Swing in Java 9 scales automatically using the system scale factor(s) that the
		// user can change in the system settings (Windows: Control Panel; Mac: System Preferences).
		// Each connected screen may use its own scale factor
		// (e.g. 1.5 for primary 4K 40inch screen and 1.0 for secondary HD screen).
		if (JavaVersion.isJava9orLater())
			return 1.0f;
		
//		Font labelFont = UIManager.getFont("TextArea.font");   // Default is 13, on HiDPI it doesn't seems to be scaled???
//		Font labelFont = UIManager.getFont("TextField.font");  // Default is 11, on HiDPI xx
		Font labelFont = UIManager.getFont("Label.font");      // Default is 11, on HiDPI xx
		float scale = labelFont.getSize() / 11.0f;  
		if (scale < 1.0f)
			scale = 1.0f;

		return scale;
	}
	/**
	 *  convert a "pixel" size into something smaller/bigger by multiplying it with the scaling factor.<br>
	 *  Currently the scaling factor is used based on the Font size of <code>UIManager.getFont("Label.font")</code>
	 */
	public static int hiDpiScale(int val)
	{
		return Math.round( (val*1.0f) * getHiDpiScale() );
	}
	/**
	 *  convert a "pixel" size into something smaller/bigger by multiplying it with the scaling factor.<br>
	 *  Currently the scaling factor is used based on the Font size of <code>UIManager.getFont("Label.font")</code>
	 */
	public static Dimension hiDpiScale(Dimension dim)
	{
		return new Dimension( hiDpiScale(dim.width), hiDpiScale(dim.height) );
	}

	/**
	 * Add an "indicator" color to the icon
	 */
	public static ImageIcon paintIcon(ImageIcon icon, Color indColor, int indSize, boolean indToLeft)
	{
		// well if we do not have an Icon attached here, no need to continue
		if ( icon == null )     return null;
		if ( indColor == null ) return icon;

		BufferedImage im;
		Graphics2D img;

		int stipeSize = SwingUtils.hiDpiScale(indSize);
		im = new BufferedImage(icon.getIconWidth() + stipeSize, icon.getIconHeight(), BufferedImage.TRANSLUCENT);
		img = im.createGraphics();
		img.setColor(indColor);

		if ( indToLeft )
		{
			icon.paintIcon(null, img, stipeSize, 0);
			img.fillRect(0, 1, stipeSize, icon.getIconHeight()-2);
		}
		else
		{
			icon.paintIcon(null, img, 0, 0);
			img.fillRect(icon.getIconWidth(), 1, stipeSize, icon.getIconHeight()-2);
		}

		return new ImageIcon(im);
	}

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

	/**
	 * Should this top menu be visible or not.<br>
	 * If it doesn't have any visible entries, it's hidden.
	 * @param menu
	 */
	public static void hideMenuIfNoneIsVisible(JMenu menu)
	{
		if (menu == null)
			return;
		
		boolean hasVisibleItems = false;
		for(int i=0; i<menu.getItemCount(); i++)
		{
			JMenuItem mi = menu.getItem(i);
			if (mi.isVisible())
			{
				hasVisibleItems = true;
				break;
			}
		}
		menu.setVisible(hasVisibleItems);
	}
	
	/**
	 * Get the String residing on the clipboard.	
	 *
	 * @return any text found on the Clipboard; if none found, return null
	 */
	public static String getClipboardContents() 
	{
		String result = null;
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		//odd: the Object param of getContents is not currently used
		Transferable contents = clipboard.getContents(null);
		boolean hasTransferableText = (contents != null) &&	contents.isDataFlavorSupported(DataFlavor.stringFlavor);
		if ( hasTransferableText ) 
		{
			try 
			{
				result = (String)contents.getTransferData(DataFlavor.stringFlavor);
			}
			catch (UnsupportedFlavorException ex)
			{
				//highly unlikely since we are using a standard DataFlavor
				_logger.error("getClipboardContents(): "+ex, ex);
			}
			catch (IOException ex) 
			{
				_logger.error("getClipboardContents(): "+ex, ex);
			}
		}
		return result;
	}

	/**
	 * Set the String residing on the clipboard.	
	 */
	public static void setClipboardContents(String s)
	{
		StringSelection selection = new StringSelection(s);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
	}

    /**
     * borrowed from BasicErrorPaneUI
     * Converts the incoming string to an escaped output string. This method
     * is far from perfect, only escaping &lt;, &gt; and &amp; characters
     */
    private static String escapeXml(String input) {
        String s = input == null ? "" : input.replace("&", "&amp;");
        s = s.replace("<", "&lt;");
        return s = s.replace(">", "&gt;");
    }
    /**
     * borrowed from BasicErrorPaneUI
     */
	private static String getDetailsAsHTML(ErrorInfo errorInfo)
	{
		if ( errorInfo.getErrorException() != null )
		{
			// convert the stacktrace into a more pleasent bit of HTML
			StringBuffer html = new StringBuffer("<html>");
			html.append("<h2>" + escapeXml(errorInfo.getTitle()) + "</h2>");
			html.append("<HR size='1' noshade>");
			html.append("<div></div>");
			html.append("<b>Message:</b>");
			html.append("<pre>");
			html.append("    " + escapeXml(errorInfo.getErrorException().toString()));
			html.append("</pre>");
			html.append("<b>Level:</b>");
			html.append("<pre>");
			html.append("    " + errorInfo.getErrorLevel());
			html.append("</pre>");
			html.append("<b>Stack Trace:</b>");
			Throwable ex = errorInfo.getErrorException();
			while (ex != null)
			{
				html.append("<h4>" + ex.getMessage() + "</h4>");
				html.append("<pre>");
				for (StackTraceElement el : ex.getStackTrace())
				{
					html.append("    " + el.toString().replace("<init>", "&lt;init&gt;") + "\n");
				}
				html.append("</pre>");
				ex = ex.getCause();
			}
			html.append("</html>");
			return html.toString();
		}
		else
		{
			return null;
		}
	}

	/**
	 * 
	 * @param owner     parent GUI component
	 * @param title     window title
	 * @param msg       the error message to be displayed
	 * @param chkbox    If we want to have a check box, which can have "do not show this next time"
	 * @param throwable any Throwable to be displayed
	 */
	public static void showInfoMessageExt(Component owner, String title, String msg, JCheckBox chkbox, Throwable throwable)
	{
		showMessageExt(Level.INFO, owner, title, msg, chkbox, throwable);
	}
	/**
	 * 
	 * @param owner     parent GUI component
	 * @param title     window title
	 * @param msg       the error message to be displayed
	 * @param chkbox    If we want to have a check box, which can have "do not show this next time"
	 * @param throwable any Throwable to be displayed
	 */
	public static void showWarnMessageExt(Component owner, String title, String msg, JCheckBox chkbox, Throwable throwable)
	{
		showMessageExt(Level.WARNING, owner, title, msg, chkbox, throwable);
	}
	/**
	 * 
	 * @param owner     parent GUI component
	 * @param title     window title
	 * @param msg       the error message to be displayed
	 * @param chkbox    If we want to have a check box, which can have "do not show this next time"
	 * @param throwable any Throwable to be displayed
	 */
	public static void showErrorMessageExt(Component owner, String title, String msg, JCheckBox chkbox, Throwable throwable)
	{
		showMessageExt(Level.SEVERE, owner, title, msg, chkbox, throwable);
	}

	/** just convert the Throwable into a JPanel and call showMessageExt */
	private static void showMessageExt(final Level errorLevel, final Component owner, final String title, final String msg, final JCheckBox chkbox, Throwable throwable)
	{
		JXEditorPane details = new JXEditorPane();
		details.setContentType("text/html");
		JScrollPane detailsScrollPane = new JScrollPane(details);
//		detailsScrollPane.setPreferredSize(new Dimension(10, 250));
		detailsScrollPane.setPreferredSize(hiDpiScale(new Dimension(10, 250)));
		details.setEditable(false);

		ErrorInfo info = new ErrorInfo(title, msg, null, null, throwable, errorLevel, null);
		String htmlText = getDetailsAsHTML(info);
		details.setText(htmlText);
		details.setCaretPosition(0);

		JPanel stackTracePanel = new JPanel();
		stackTracePanel.setLayout(new MigLayout());
		stackTracePanel.add(detailsScrollPane, "push, grow");
		
		showMessageExt(errorLevel, owner, title, msg, chkbox, stackTracePanel);
	}

	/**
	 * 
	 * @param owner     parent GUI component
	 * @param title     window title
	 * @param msg       the error message to be displayed
	 * @param chkbox    If we want to have a check box, which can have "do not show this next time"
	 * @param userPanel If you want to add some extra components in a special panel below the error message
	 */
	public static void showInfoMessageExt(Component owner, String title, String msg, JCheckBox chkbox, JPanel userPanel)
	{
		showMessageExt(Level.INFO, owner, title, msg, chkbox, userPanel);
	}
	/**
	 * 
	 * @param owner     parent GUI component
	 * @param title     window title
	 * @param msg       the error message to be displayed
	 * @param chkbox    If we want to have a check box, which can have "do not show this next time"
	 * @param userPanel If you want to add some extra components in a special panel below the error message
	 */
	public static void showWarnMessageExt(Component owner, String title, String msg, JCheckBox chkbox, JPanel userPanel)
	{
		showMessageExt(Level.WARNING, owner, title, msg, chkbox, userPanel);
	}
	/**
	 * 
	 * @param owner     parent GUI component
	 * @param title     window title
	 * @param msg       the error message to be displayed
	 * @param chkbox    If we want to have a check box, which can have "do not show this next time"
	 * @param userPanel If you want to add some extra components in a special panel below the error message
	 */
	public static void showErrorMessageExt(Component owner, String title, String msg, JCheckBox chkbox, JPanel userPanel)
	{
		showMessageExt(Level.SEVERE, owner, title, msg, chkbox, userPanel);
	}
	/**
	 * 
	 * @param owner     parent GUI component
	 * @param title     window title
	 * @param msg       the error message to be displayed
	 * @param chkbox    If we want to have a check box, which can have "do not show this next time"
	 * @param userPanel If you want to add some extra components in a special panel below the error message
	 */
	private static void showMessageExt(final Level errorLevel, final Component owner, final String title, final String msg, final JCheckBox chkbox, final JPanel userPanel)
	{
		// If not Event Queue Thread, execute it with that
		if ( ! isEventQueueThread() )
		{
			Runnable doRun = new Runnable()
			{
				@Override
				public void run()
				{
					showMessageExt(errorLevel, owner, title, msg, chkbox, userPanel);
				}
			};
			try
			{
				SwingUtilities.invokeAndWait(doRun);
			}
			catch (Exception e)
			{
				_logger.error("Problems doing 'showMessageExt'.", e);
			}
			return;
		}

		//-------------------------------------------------
		// Method *really* start here...
		//-------------------------------------------------
		final JDialog dialog;
		if      (owner instanceof Dialog) dialog = new JDialog( (Dialog) owner );
		else if (owner instanceof Frame)  dialog = new JDialog( (Frame)  owner );
		else if (owner instanceof Window) dialog = new JDialog( (Window) owner );
		else dialog = new JDialog();

		JPanel msgPanel    = new JPanel(new MigLayout());
		JPanel bottomPanel = new JPanel(new MigLayout());

		// Create a close button
		JButton close_but = new JButton("Close");
		close_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				dialog.setVisible(false);
			}
		});

		final String iconProp;
		if      (errorLevel.equals(Level.INFO))    iconProp = "OptionPane.informationIcon";
		else if (errorLevel.equals(Level.WARNING)) iconProp = "OptionPane.warningIcon";
		else                                       iconProp = "OptionPane.errorIcon";

		// Icon to be displayed
		JLabel icon_lbl = new JLabel(UIManager.getIcon(iconProp));

		// Create the error message component
		//JLabel errorMessage  = new JLabel(msg);
		// To get JLabel "selectable" so we can copy paste from it, use JEditorPane
		JEditorPane errorMessage = new JEditorPane(new HTMLEditorKit().getContentType(), msg);
		errorMessage.setText(msg);
		// add a CSS rule to force body tags to use the default label font
		// instead of the value in javax.swing.text.html.default.csss
		Font font = UIManager.getFont("Label.font");
		String bodyRule = "body { font-family: " + font.getFamily() + "; " + "font-size: " + font.getSize() + "pt; }";
		((HTMLDocument)errorMessage.getDocument()).getStyleSheet().addRule(bodyRule);

		errorMessage.setOpaque(false);
		errorMessage.setBorder(null);
		errorMessage.setEditable(false);

		// Add components to the MESSAGE PANEL
		msgPanel.add(icon_lbl,     "gap 20 20 20 20, top, left, split");
//		msgPanel.add(errorMessage, "gap 20 20 20 20, grow, push, wrap");

		// If we add the JEditorPane, using MigLayout, it gets to big...
		JPanel dummyPanel = new JPanel(new BorderLayout());
		dummyPanel.add(errorMessage);
		msgPanel.add(dummyPanel, "gap 20 20 20 0, grow, push, wrap");

		// Add components to the "footer"
		Component comp = chkbox;
		if (comp == null)
			comp = new JLabel("");
		bottomPanel.add(comp,      "split, grow, push");
		bottomPanel.add(close_but, "tag right, wrap");

		// Add all the panels to a base panel, which we add to the dialog
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(msgPanel,    BorderLayout.NORTH);
		panel.add(bottomPanel, BorderLayout.SOUTH);
		if (userPanel != null)
		{
			JPanel tmpPanel = new JPanel(new MigLayout());
			tmpPanel.add(userPanel, "gap 65 20, grow, push, wrap");
			
			panel.add(tmpPanel, BorderLayout.CENTER);
		}

		// set properties to the dialog
		dialog.setContentPane(panel);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.setTitle(title);
		dialog.setModal(true);
		dialog.pack();

		dialog.setLocationRelativeTo(owner);

		// Finally show the dialog
		dialog.setVisible(true);
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
		// If we are NOT in GUI mode, log the error and get out of here
		if (GraphicsEnvironment.isHeadless())
		{
			if      (errorLevel.equals(Level.INFO))    _logger.info (msg, exception);
			else if (errorLevel.equals(Level.WARNING)) _logger.warn (msg, exception);
			else                                       _logger.error(msg, exception);
			return;
		}
		
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
//				info.addHyperlinkListener(this); // the JXErrorPane/ErrorInfo doesn't support Hyperlinks
				final JDialog dialog = JXErrorPane.createDialog(finalOwner, errorPane);
		        dialog.setLocationRelativeTo(finalOwner);
				dialog.pack();
				dialog.setTitle(title);

				// Try to set focus on the "Close" button, execute this *after* the window is visible
				Runnable grabFocus = new Runnable()
				{
					@Override
					public void run()
					{
						dialog.requestFocusInWindow();
					}
				};
				SwingUtilities.invokeLater(grabFocus);
				
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

	/**
	 * If window seems to be outside of the screen
	 * <p>
	 * The usage is: when reading a saved screen location, which seams to be "out of scope"<br>
	 * I'm not sure how this will work on virtual screens or multiple screens
	 * @returns true if the location is out-of-screen
	 */
	public static boolean isOutOfScreen(int winPosX, int winPosY)
	{
		return isOutOfScreen(winPosX, winPosY, 0, 0);
	}
	/**
	 * If window seems to be outside of the screen
	 * <p>
	 * The usage is: when reading a saved screen location, which seams to be "out of scope"<br>
	 * I'm not sure how this will work on virtual screens or multiple screens
	 * @returns true if the location is out-of-screen
	 */
	public static boolean isOutOfScreen(int winPosX, int winPosY, int winWidth, int winHeight)
	{
//		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

		if (    ((winPosX + winWidth)  < 0) || winPosX > screenSize.width 
			 || ((winPosY + winHeight) < 0) || winPosY > screenSize.height 
		   )
		{
			return true;
		}
		return false;
	}

	public static void centerWindow(Component frame)
	{
		//Center the window
//		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
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

	public static void setLocationCenterParentWindow(Component parent, Component child)
	{
		// FIXME not proper implemented
//		if (parent != null)
//		{
//			Point p = parent.getLocationOnScreen();
//			p.x += x;
//			p.y += y;
//			child.setLocation(p);
//		}
//		else
			centerWindow(child);
//		child.setLocationRelativeTo(parent);
//		child.setLocationByPlatform(true);
	}

	public static JButton makeToolbarButton(Class<?> clazz, String imageName, String actionCommand, ActionListener al, String toolTipText, String altText)
	{
		// Get the image
		ImageIcon imageIcon = readImageIcon(clazz, imageName);
		// Look for the image.
//		String imgLocation = "images/" + imageName;
//		URL imageURL = clazz.getResource(imgLocation);

		// Create and initialize the button.
		JButton button = new JButton();
		button.setActionCommand(actionCommand);
		button.setToolTipText(toolTipText);
//		button.addActionListener(this);

		if (al != null)
			button.addActionListener(al);

//		if (imageURL != null)
//		{ // image found
//			button.setIcon(new ImageIcon(imageURL, altText));
//		}
		if (imageIcon != null)
		{ // image found
			imageIcon.setDescription(altText);
			button.setIcon(imageIcon);
		}
		else
		{ // no image found
			button.setText(altText);
//			_logger.error("Toolbar Resource not found '"+imgLocation+"', url='"+imageURL+"'.");
		}

		return button;
	}

	public static ImageIcon readImageIcon(Class<?> clazz, String filename)
	{
		if (StringUtil.isNullOrBlank(filename))
			return null;

//		URL url = MainFrame.class.getResource("images/" + filename);
		URL url = clazz.getResource(filename);
//		System.out.println("---->>>>>>>>>>>>>>>>>> Using the icon '"+url+"'.");
		if (url == null)
		{
			_logger.error("Can't find the resource for class='"+clazz+"', filename='"+filename+"'.");
			return null;
		}

//		return new ImageIcon(Toolkit.getDefaultToolkit().getImage(url));

		ImageIcon iconImage = new ImageIcon(Toolkit.getDefaultToolkit().getImage(url));

		int scaleFactor = 1;
		if (SwingUtils.isHiDpi())
		{
			scaleFactor = 2;
			
			int newWidth  = iconImage.getIconWidth()  * scaleFactor;
			int newHeight = iconImage.getIconHeight() * scaleFactor;
			
			Image image = iconImage.getImage().getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
			iconImage = new ImageIcon(image);
			
			if (_logger.isDebugEnabled())
				_logger.debug("readImageIcon(): isHiDpi()="+isHiDpi()+", getHiDpiScale()="+getHiDpiScale()+", scaleFactor="+scaleFactor+", newWidth="+newWidth+", newHeight="+newHeight+", newWidthAfter="+iconImage.getIconWidth()+", newHeightAfter="+iconImage.getIconHeight()+".");
		}
		return iconImage;

//		Image image = Toolkit.getDefaultToolkit().getImage(url);
////		BufferedImage buffered = ((ToolkitImage) image).getBufferedImage();
//		BufferedImage bigImage = enlarge(toBufferedImage(image), 2);
//		ImageIcon ii = new ImageIcon(bigImage);
////		return new ImageIcon(bigImage);

//		try
//		{
//			return new ImageIcon(enlarge(ImageIO.read(url), 2));
//		}
//		catch (IOException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return null;
//		}

//		return new HiDpiIcon(Toolkit.getDefaultToolkit().getImage(url), Toolkit.getDefaultToolkit().getImage(url));
	}

//	public static BufferedImage enlarge(BufferedImage image, int n)
////	public static BufferedImage enlarge(Image image, int n)
//	{
//
//		int w = n * image.getWidth();
//		int h = n * image.getHeight();
////		int w = n * image.getWidth(null);
////		int h = n * image.getHeight(null);
//
//		BufferedImage enlargedImage = new BufferedImage(w, h, image.getType());
//
//		for (int y = 0; y < h; ++y)
//		{
//			for (int x = 0; x < w; ++x)
//			{
//				enlargedImage.setRGB(x, y, image.getRGB(x / n, y / n));
//			}
//		}
//		return enlargedImage;
//	}
	
	/** helper method to create a JPanel */
	public static JPanel createPanel(String title, boolean createBorder) 
	{
		return createPanel(title, createBorder, null);
	}

	/** helper method to create a JPanel */
	public static JPanel createPanel(String title, boolean createBorder, LayoutManager layoutManager) 
	{
		JPanel panel = new JPanel();
		
		if (layoutManager != null)
			panel.setLayout(layoutManager);

		if (createBorder)
		{
			Border border = BorderFactory.createTitledBorder(title);
			panel.setBorder(border);
//			panel.setAlignmentX(110);
//			panel.setAlignmentY(110);
		}
		return panel;
	}

	/** helper method to create a GPanel */
	public static GPanel createGPanel(String title, boolean createBorder) 
	{
		GPanel panel = new GPanel();
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

	public static String tableToString(TableModel tm)
	{
		return tableToString(tm, true);
	}
	public static String tableToString(TableModel tm, String newLineWhenValueChangesForColName)
	{
		JTable dummyJtable = new JTable(tm);
		return tableToString(dummyJtable, false, null, null, -1, -1, null, true, newLineWhenValueChangesForColName);
	}
	public static String tableToString(TableModel tm, boolean printNumOfRowsAtEnd)
	{
		JTable dummyJtable = new JTable(tm);
		return tableToString(dummyJtable, printNumOfRowsAtEnd);
	}
	
	public static String tableToString(JTable jtable, int[] justRowNumbers)
	{
		int firstRow = justRowNumbers[0];
		int lastRow  = justRowNumbers[justRowNumbers.length-1] + 1;
		return tableToString(jtable, false, null, null, firstRow, lastRow, justRowNumbers, true);
	}
	public static String tableToString(JTable jtable, int justRowNumber)
	{
		return tableToString(jtable, false, null, null, justRowNumber, justRowNumber+1, null, true);
	}
	public static String tableToString(JTable jtable)
	{
		return tableToString(jtable, true);
	}
	public static String tableToString(JTable jtable, boolean printNumOfRowsAtEnd)
	{
		return tableToString(jtable, false, null, null, -1, -1, null, printNumOfRowsAtEnd);
	}
	public static String tableToString(JTable jtable, boolean stripHtml, String[] prefixColName, Object[] prefixColData)
	{
		return tableToString(jtable, stripHtml, prefixColName, prefixColData, -1, -1, null, true);
	}

	/** used in tableToString() */
	private static String REGEXP_NEW_LINE = "\\r?\\n|\\r";
//	private static String REGEXP_NEW_LINE = "[\n\r]";
//	private static String REGEXP_NEW_LINE = "\\r?\\n";
//	private static String REGEXP_NEW_LINE = "\n";
//	private static String REGEXP_NEW_LINE = "[\\r\\n]+";
//	private static String REGEXP_NEW_LINE = "[\\n\\x0B\\f\\r]+";
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
	public static String tableToString(JTable jtable, boolean stripHtml, String[] prefixColName, Object[] prefixColData, int firstRow, int lastRow, int[] justRowNumbers, boolean printNumOfRowsAtEnd)
	{
		return tableToString(jtable, stripHtml, prefixColName, prefixColData, firstRow, lastRow, justRowNumbers, printNumOfRowsAtEnd, null);
	}

	public static String tableToString(JTable jtable, boolean stripHtml, String[] prefixColName, Object[] prefixColData, int firstRow, int lastRow, int[] justRowNumbers, boolean printNumOfRowsAtEnd, String newLineWhenValueChangesForColName)
	{
		String colSepOther = "+";
		String colSepData  = "|";
		String lineSpace   = "-";
		String newLine     = "\n";

		// first copy the information to Array list
		// This was simples to do if we want to add pre/post columns...
		ArrayList<String>       tableHead = new ArrayList<String>();
		ArrayList<List<Object>> tableData = new ArrayList<List<Object>>();

//		int expSize = 0; // Try to calculate an expected size for the whole table so we can exit early if it gets to big
		StringBuilder sb = new StringBuilder();

//		int maxCellStrLen = 1*1024*1024; // 1MB
		int maxCellStrLen = 20*1024; // 20KB

		System.gc();
		try { Thread.sleep(15); } catch(InterruptedException ignore) {} // Sleep a while for Garbage Collector can work...
		long maxStrBufLen  = Runtime.getRuntime().freeMemory() / 3;
//		long maxStrBufLen  = Runtime.getRuntime().maxMemory() / 8;
//		long maxStrBufLen  = 500*1024*1024; // 500MB   or grab free memory / 3
		
		int     truncateCellStrLenCount      = 0; // if cells has been truncated
		boolean truncateOutputBufferHappened = false; // if StringBuilder has been truncated
		
		boolean doPrefix = false;
		if (prefixColName != null && prefixColData != null)
		{
			if (prefixColName.length != prefixColData.length)
				throw new IllegalArgumentException("tableToString(): prefixColName.length="+prefixColName.length+" is NOT equal prefixColData.length="+prefixColData.length);
			doPrefix = true;
		}

		int cols = jtable.getColumnCount();
		if (firstRow < 0) firstRow = 0;
		if (lastRow  < 0) lastRow = jtable.getRowCount();
		int copiedRows = 0;

		//------------------------------------
		// Copy COL NAMES
		//------------------------------------
		if (doPrefix)
			for (int c=0; c<prefixColName.length; c++)
				tableHead.add(prefixColName[c]);

		for (int c=0; c<cols; c++)
			tableHead.add(jtable.getColumnName(c));

		TableModel tm = jtable.getModel();

		// Figgure out if we should do 'newLineWhenValueChangesForColName'
		int pos_newLineWhenValueChangesForColName = -1;
		if (StringUtil.hasValue(newLineWhenValueChangesForColName))
		{
			pos_newLineWhenValueChangesForColName = tableHead.indexOf(newLineWhenValueChangesForColName);
			if (pos_newLineWhenValueChangesForColName == -1)
				throw new RuntimeException("Could not find column: newLineWhenValueChangesForColName='" + newLineWhenValueChangesForColName + "' in table.");
		}
		
		//------------------------------------
		// Copy ROWS (from firstRow to lastRow)
		//------------------------------------
		for (int r=firstRow; r<lastRow; r++)
		{
			if (justRowNumbers != null)
			{
				boolean addThisRow = false;
				for (int a=0; a<justRowNumbers.length; a++)
				{
					if ( r == justRowNumbers[a] )
					{
						addThisRow = true;
						break;
					}
				}
				if ( ! addThisRow )
					continue;
			}

			ArrayList<Object> row = new ArrayList<Object>();
			if (doPrefix)
				for (int c=0; c<prefixColData.length; c++)
					row.add(prefixColData[c]);

			for (int c=0; c<cols; c++)
			{
				Object obj = jtable.getValueAt(r, c);

				// Strip of '\n' at the end of Strings
				if (obj != null && obj instanceof String)
				{
					
					String str = (String) obj;
					
					// strip off HTML chars
					if (stripHtml)
						str = StringUtil.stripHtml(str);

					// if the string ENDS with a newline, remove it
					while (str.endsWith("\r") || str.endsWith("\n"))
						str = str.substring(0, str.length()-1);

					// replace all tab's with 8 spaces
					if (str.indexOf('\t') >= 0)
						str = str.replace("\t", "        ");

					// Truncate if it's to long
					if (str.length() > maxCellStrLen)
					{
						str = str.substring(0, maxCellStrLen) + "... ---WARNING--- String truncated after " + maxCellStrLen + " chars ---WARNING---";
						truncateCellStrLenCount++;
					}
					
					// if we have a "multiple row/line cell"
					if (str.indexOf('\r') >= 0 || str.indexOf('\n') >= 0)
					{
						// RTrim - Remove trailing blanks (if there is a lot of blanks at the end of each rows)
						String[] sa = str.split(REGEXP_NEW_LINE); // object "type" would be String[]
						for (int i=0; i<sa.length; i++)
							sa[i] = StringUtil.rtrim2(sa[i]);

						obj = sa;
					}
					else
						obj = str;
				}
				else
				{
					// if the TableModel is of ResultSetTableModel... let's use that DataType "converter/format" (example for Timestamp values)  
					if (tm instanceof ResultSetTableModel)
					{
						ResultSetTableModel rstm = (ResultSetTableModel) tm;
						
						if (obj != null && obj instanceof Number && rstm.getToStringNumberFormat())
						{
							obj = rstm.toString(obj, null);
						}
						else if (obj != null && obj instanceof Timestamp && StringUtil.hasValue(rstm.getToStringTimestampFormat()))
						{
							obj = rstm.toString(obj, null);
						}
//						else
//						{
//							obj = rstm.toString(obj, null);
//						}
					}
				}
				
				row.add(obj);
				//System.out.println(" 111 [r="+r+",c="+c+"] "+(obj == null ? "-NULL-" : obj.getClass().getSimpleName())+": obj=|"+obj+"|.");
			}
			
			tableData.add(row);
			copiedRows++;
		}

		// Add prefixColCount to cols
		if (doPrefix)
			cols += prefixColName.length;

		NumberFormat nf = NumberFormat.getInstance();
		
		//------------------------------------
		// Get MAX column length and store in colLength[]
		// Get MAX newLines/numberOfRows in each cell...
		//------------------------------------
		boolean tableHasMultiLineCells = false;
		int[]   colLength           = new int[cols];
		int[][] rowColCellLineCount = new int[copiedRows][cols];
//		int[]   rowMaxLineCount     = new int[copiedRows];
		for (int c=0; c<cols; c++)
		{
			int maxLen = 0;

			// ColNames
			String cellName = tableHead.get(c);
			maxLen = Math.max(maxLen, cellName.length());
			
			// All the rows, for this column
			for (int r=0; r<copiedRows; r++)
			{
				Object cellObj = tableData.get(r).get(c);
				String cellStr = cellObj == null ? "" : cellObj.toString();

				if (cellObj != null && cellObj instanceof Number)
					cellStr = nf.format(cellObj);
					
				// Set number of "rows" within the cell
				rowColCellLineCount[r][c] = 0;
				if (cellObj instanceof String[])
				{
					String[] sa = (String[]) cellObj;
					tableHasMultiLineCells = true;

					rowColCellLineCount[r][c] = sa.length;

					for (int l=0; l<sa.length; l++)
						maxLen = Math.max(maxLen, sa[l].length());
				}
				else
				{
					maxLen = Math.max(maxLen, cellStr.length());
				}
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


		// used by: pos_newLineWhenValueChangesForColName
		Object rememberColValue = null;
		
		//-------------------------------------------
		// Print the TABLE DATA
		//-------------------------------------------
		for (int r=0; r<copiedRows; r++)
		{
			// First loop cols on this row and check for any multiple lines in any o the cells
			int maxCellLineCountOnThisRow = 0;
			for (int c=0; c<cols; c++)
			{
				maxCellLineCountOnThisRow = Math.max(maxCellLineCountOnThisRow, rowColCellLineCount[r][c]);
			}

			// Add a extra "row" separator if any cells has multiple lines
			if (tableHasMultiLineCells && r > 0)
			{
				// +------+------+-----+\n
				for (int c=0; c<cols; c++)
				{
					String line = StringUtil.replicate(lineSpace, colLength[c]);
					sb.append(colSepOther).append(line);
				}
				sb.append(colSepOther).append(newLine);
			}
//System.out.println("r="+r+", sb.length="+sb.length()+", maxCellLineCountOnThisRow="+maxCellLineCountOnThisRow);
			// NO multiple lines for any cells on this row
			if (maxCellLineCountOnThisRow == 0)
			{
				// if values has been changed for column: Add a "line-separator" (|------|------|-----|) 
				if (pos_newLineWhenValueChangesForColName != -1)
				{
					Object colValue = tableData.get(r).get(pos_newLineWhenValueChangesForColName);
					if (colValue != null && !colValue.equals(rememberColValue))
					{
						rememberColValue = colValue;
						if (r > 0)
						{
							// print: |------|------|-----|\n
							for (int c=0; c<cols; c++)
							{
								String line = StringUtil.replicate(lineSpace, colLength[c]);
								sb.append(colSepData).append(line);
							}
							sb.append(colSepData).append(newLine);
						}
					}
				}

				// |col1|col2   |col3|\n
				for (int c=0; c<cols; c++)
				{
					Object cellObj = tableData.get(r).get(c);
					String cellStr = cellObj == null ? "" : cellObj.toString();
	
					String data;
					if (cellObj != null && cellObj instanceof Number)
					{
						cellStr = nf.format(cellObj);
						data = StringUtil.right(cellStr, colLength[c]);
						//System.out.println(">>> [r="+r+",c="+c+"] isNumber: data=|"+data+"|.");
					}
					else
					{
						data = StringUtil.fill(cellStr, colLength[c]);
						//System.out.println("  - [r="+r+",c="+c+"] "+(cellObj == null ? "-NULL-" : cellObj.getClass().getSimpleName())+": data=|"+data+"|.");
					}

					sb.append(colSepData).append(data);
				}
				sb.append(colSepData).append(newLine);
			}
			// MULTIPLE line in one or more cells
			else
			{
				for (int l=0; l<maxCellLineCountOnThisRow; l++)
				{
//System.out.println("-------> r="+r+", l="+l+", sb.length="+sb.length()+", maxCellLineCountOnThisRow="+maxCellLineCountOnThisRow);
					// |col1|col2   |col3|\n
					for (int c=0; c<cols; c++)
					{
//System.out.println("r="+r+", c="+c+", l="+l+", sb.length="+sb.length()+", maxCellLineCountOnThisRow="+maxCellLineCountOnThisRow);
						Object cellObj = tableData.get(r).get(c);
						String cellStr = cellObj == null ? "" : cellObj.toString();

						// first line
						if (l == 0)
						{
							// this cell has multiple lines, so just choose first line
							if ( cellObj instanceof String[] )
							{
								String[]sa = (String[]) cellObj;

								cellStr = sa[0];
							}
							String data = StringUtil.fill(cellStr, colLength[c]);
							sb.append(colSepData).append(data);
						}
						else // next of the lines
						{
							// this cell has multiple lines
							if ( cellObj instanceof String[] )
							{
								String[]sa = (String[]) cellObj;

								cellStr = "";
								if (l < sa.length)
									cellStr = sa[l];
							}
							else
							{
								cellStr = "";
							}
							String data = StringUtil.fill(cellStr, colLength[c]);
							sb.append(colSepData).append(data);
						}
					}
					sb.append(colSepData).append(newLine);

					if (sb.length() > maxStrBufLen)
						break;
				}
			}
			
			if (sb.length() > maxStrBufLen)
			{
				truncateOutputBufferHappened = true;

				sb.append(newLine);
				sb.append("At row: ").append(r).append(" WARNING: Output buffer is getting to large... stopping appending rows... String buffer size=").append(maxStrBufLen);
				sb.append(newLine);
				break;
			}
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
		if (printNumOfRowsAtEnd)
			sb.append("Rows ").append(copiedRows).append(newLine);
		
		if (truncateCellStrLenCount > 0)
			sb.append("--- WARNING --- WARNING --- WARNING --- ").append(truncateCellStrLenCount).append(" Cells was truncated because they were to large --- WARNING --- WARNING --- WARNING ---").append(newLine);
		
		if (truncateOutputBufferHappened)
			sb.append("--- WARNING --- WARNING --- WARNING --- ").append(" Output buffer was truncated --- WARNING --- WARNING --- WARNING ---").append(newLine);
		
		return sb.toString();
	}


	public static String tableToHtmlString(TableModel tm)
	{
		return tableToHtmlString(tm, null, -1);
	}
	public static String tableToHtmlString(TableModel tm, int firstXRows)
	{
		return tableToHtmlString(tm, null, firstXRows);
	}
	public static String tableToHtmlString(TableModel tm, String className, int firstXRows)
	{
		if (tm == null)
		{
			_logger.warn("tableToHtmlString(TableModel tm): The passed TableModel is null, returning '' (blank)");
			return "";
		}

		StringBuilder sb = new StringBuilder();
		int rows = tm.getRowCount();
		int cols = tm.getColumnCount();

		if (firstXRows > 0)
			rows = Math.min(rows, firstXRows);
		
		String tableAttr = "";
		if (StringUtil.hasValue(className))
			tableAttr = " class='" + className + "'";

		sb.append("<TABLE" + tableAttr + ">\n");

		// Headers
		sb.append("  <TR>");
		for (int c=0; c<cols; c++) 
			sb.append(" <TH>").append(tm.getColumnName(c)).append("</TH>");
		sb.append(" </TR>\n");

		// Rows
		for (int r=0; r<rows; r++) 
		{
			sb.append("  <TR>");
			for (int c=0; c<cols; c++) 
				sb.append(" <TD>").append(tm.getValueAt(r, c)).append("</TD>");
			sb.append(" </TR>\n");
		}
		sb.append("</TABLE>\n");

		return sb.toString();
	}

	public static String tableToHtmlString(JTable table)
	{
		return tableToHtmlString(table, null, -1);
	}
	public static String tableToHtmlString(JTable table, int firstXRows)
	{
		return tableToHtmlString(table, null, firstXRows);
	}
	public static String tableToHtmlString(JTable table, String className, int firstXRows)
	{
		StringBuilder sb = new StringBuilder();
		int rows = table.getRowCount();
		int cols = table.getColumnCount();

		if (firstXRows > 0)
			rows = Math.min(rows, firstXRows);
		
		String tableAttr = "";
		if (StringUtil.hasValue(className))
			tableAttr = " class='" + className + "'";

		sb.append("<TABLE" + tableAttr + ">\n");

		// Headers
		sb.append("  <TR>");
		for (int c=0; c<cols; c++) 
			sb.append(" <TH>").append(table.getColumnName(c)).append("</TH>");
		sb.append(" </TR>\n");

		// Rows
		for (int r=0; r<rows; r++) 
		{
			sb.append("  <TR>");
			for (int c=0; c<cols; c++) 
				sb.append(" <TD>").append(table.getValueAt(r, c)).append("</TD>");
			sb.append(" </TR>\n");
		}
		sb.append("</TABLE>\n");

		return sb.toString();
	}
	public static String tableToCsvString(JTable table, boolean headers, String colSep, String rowSep, String tabNullValue, String outNullvalue, boolean useRfc4180)
	{
		StringBuilder sb = new StringBuilder();
		int rows = table.getRowCount();
		int cols = table.getColumnCount();

		// Headers
		if (headers)
		{
			for (int c=0; c<cols; c++) 
			{
				sb.append(table.getColumnName(c));

				// Add column separator, but not for last column
				if (c+1 < cols)
					sb.append(colSep);
			}
			sb.append(rowSep);
		}

		// Rows
		for (int r=0; r<rows; r++) 
		{
			for (int c=0; c<cols; c++) 
			{
				// FIXME: We would probably do something like:
				// - get Object, + toString
				// - escape any "new-line" characters
				Object obj = table.getValueAt(r, c);
				if (obj == null)
				{
					obj = outNullvalue;
				}
				else
				{
					if (obj.equals(tabNullValue))
						obj = outNullvalue;
				}
				// Write columns data
				if ( obj instanceof String && useRfc4180 )
					sb.append(StringUtil.toRfc4180String( (String)obj ));
				else
					sb.append(obj);

				// Add column separator, but not for last column
				if (c+1 < cols)
					sb.append(colSep);
			}
			sb.append(rowSep);
		}

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

//	/**
//	 * Get current screen resulution as a String
//	 * @return For example "1024x768"
//	 */
//	public static String getScreenResulutionAsString()
//	{
//		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
//		int width = gd.getDisplayMode().getWidth();
//		int height = gd.getDisplayMode().getHeight();
//		
//		return width + "x" + height;
//	}
	/**
	 * Get current screen resolution as a String<br>
	 * And if you got more than one screen, the screen resolutions will be separated with a semicolon (;)
	 * @return For example "1024x768" or "2560x1440;1024x768"
	 */
	public static String getScreenResulutionAsString()
	{
		// If we cant provide a GUI simply say YES
		if (GraphicsEnvironment.isHeadless()) 
		{
			return "headless";
		}

		String retStr = "";

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] screens = ge.getScreenDevices();
		int n = screens.length;
		for (int i=0; i<n; i++) 
		{
			GraphicsDevice gd = screens[i];
			if (gd == null) 
				continue;
			if (gd.getDisplayMode() == null) 
				continue;

			int width = gd.getDisplayMode().getWidth();
			int height = gd.getDisplayMode().getHeight();

			if (i > 0)
				retStr += ";";
				
			retStr = retStr + width + "x" + height;
		}

		if (_logger.isDebugEnabled())
			_logger.debug("getScreenResulutionAsString(): returns |"+retStr+"|.");

		return retStr;
	}
	/**
	 * If the input parameters are smaller than the screen size then they will be used<br>
	 * Otherwise the screen size will be used
	 * 
	 * @param width
	 * @param height
	 * @param marginPixels Number of pixels to the border of the screen. 0 means no margin space
	 * @return a Dimension
	 */
	public static Dimension getSizeWithingScreenLimit(int width, int height, int marginPixels)
	{
//		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		int screenWidth  = screenSize.width  - (marginPixels * 3); // *3 = right and left margin + some extra
		int screenHeight = screenSize.height - (marginPixels * 2); // *2 = top and bottom margin
		return new Dimension(Math.min(width, screenWidth), Math.min(height, screenHeight));
	}

	/**
	 * If the input Component are smaller than the screen size then they will be used<br>
	 * Otherwise the screen size will be used
	 * 
	 * @param comp Component to set size of
	 * @param marginPixels Number of pixels to the border of the screen. 0 means no margin space
	 */
	public static void setSizeWithingScreenLimit(Component comp, int marginPixels)
	{
		Dimension size = comp.getSize();
		size = SwingUtils.getSizeWithingScreenLimit(size.width, size.height, marginPixels);
		comp.setSize(size);
	}

	/** for documentation see {@link #setWindowMinSize(Window, Container)} */
	public static void setWindowMinSize(JDialog dialog)
	{
		setWindowMinSize(dialog, dialog.getContentPane());
	}
	/** for documentation see {@link #setWindowMinSize(Window, Container)} */
	public static void setWindowMinSize(JFrame frame)
	{
		setWindowMinSize(frame, frame.getContentPane());
	}
	/** for documentation see {@link #setWindowMinSize(Window, Container)} */
	public static void setWindowMinSize(JWindow window)
	{
		setWindowMinSize(window, window.getContentPane());
	}
	/**
	 * Respect the allowed contents minimum size.<br>
	 * This can be used to set the minimum size if setSize() has been done which is to small to fit the content.<br>
	 * While we still wanted to restore the previous saved size of a dialog.
	 * <p>
	 * Typically used when restoring sizes from a old application version, but the new application version has 
	 * extended the minimum size (added some fields), then buttons might not be visible due to that they 
	 * are displayed "outside" of the visible window... 
	 * 
	 * @param win
	 * @param contentPane
	 */
	public static void setWindowMinSize(final Window win, final Container contentPane)
	{
		// Do this later, so all components is resized to it's proper values
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				// Get Window Decoration sizes (windows head and bottom)
			    Insets ins = win.getInsets();

			    int thisWinHeight    = win.getHeight();
				int thisWinWidth     = win.getWidth();
				int calcWinMinHeight = ins.top + ins.bottom + contentPane.getMinimumSize().height;
				int calcWinMinWidth  = ins.left + ins.right + contentPane.getMinimumSize().width;
//				int calcWinMinHeight = ins.top  + ins.bottom + contentPane.getPreferredSize().height;
//				int calcWinMinWidth  = ins.left + ins.right  + contentPane.getMinimumSize()  .width;

				if (_logger.isDebugEnabled())
				{
					_logger.debug("setWindowMinSize(): actWinHeight="+thisWinHeight+", calcWinMinHeight="+calcWinMinHeight+".");
					_logger.debug("setWindowMinSize(): actWinWidth ="+thisWinWidth +", calcWinMinWidth ="+calcWinMinWidth+".");
				}

				if (thisWinHeight < calcWinMinHeight)
				{
					_logger.info("setWindowMinSize(): Adjusting minimum window HEIGHT from '"+thisWinHeight+"' to minimum '"+calcWinMinHeight+"'.");
					win.setSize(win.getWidth(), calcWinMinHeight);
				}
				if (thisWinWidth < calcWinMinWidth)
				{
					_logger.info("setWindowMinSize(): Adjusting minimum window WIDTH from '"+thisWinWidth+"' to minimum '"+calcWinMinWidth+"'.");
					win.setSize(calcWinMinWidth, win.getHeight());
				}
			}
		});
	}

	/**
	 * Enable/disable all components on a JPanel
	 * @param panel
	 * @param enable
	 */
	public static void setEnabled(JPanel panel, boolean enable)
	{
		panel.setEnabled(enable);
		for (int i = 0; i < panel.getComponentCount(); i++)
		{
			Component comp = panel.getComponent(i);
			comp.setEnabled(enable);
			
			if (comp instanceof JPanel)
				setEnabled((JPanel)comp, enable);
		}
	}
	public static void setEnabled(JPanel panel, boolean enable, Component... disregard)
	{
//		if (panel == null)
//			return;

		panel.setEnabled(enable);
		for (int i = 0; i < panel.getComponentCount(); i++)
		{
			Component comp = panel.getComponent(i);

			// check for components that should be "disregarded"
			boolean doAction = true;
			if (disregard != null)
				for (Component dc : disregard)
					if (dc.equals(comp))
						doAction = false;

			if (doAction)
				comp.setEnabled(enable);

			if (comp instanceof JPanel)
				setEnabled((JPanel)comp, enable, disregard);
		}
	}

	/**
	 * Set focus to a good field or button<br>
	 * uses: <code>SwingUtilities.invokeLater()</code>
	 */
	public static void setFocus(final Component compToFocus)
	{
		// The components needs to be visible for the requestFocus()
		// to work, so lets the EventThreda do it for us after the windows is visible.
		Runnable deferredAction = new Runnable()
		{
			@Override
			public void run()
			{
				compToFocus.requestFocus();
			}
		};
		SwingUtilities.invokeLater(deferredAction);
	}


	/**
	 * When pressing ESCAPE, what button should we press
	 */
	public static void installEscapeButton(JDialog window, final JButton pressThisOnEscape)
	{
		@SuppressWarnings("serial")
		AbstractAction escAction = new AbstractAction() 
		{
			@Override
			public void actionPerformed(ActionEvent evt) 
			{
				if (pressThisOnEscape != null) 
					pressThisOnEscape.doClick(20);
	        }
		};

		InputMap iMap = window.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");

		ActionMap aMap = window.getRootPane().getActionMap();
		aMap.put("escape", escAction); 
	}
	/**
	 * When pressing ESCAPE, what button should we press
	 */
	public static void installEscapeButton(JFrame window, final JButton pressThisOnEscape)
	{
		@SuppressWarnings("serial")
		AbstractAction escAction = new AbstractAction() 
		{
			@Override
			public void actionPerformed(ActionEvent evt) 
			{
				if (pressThisOnEscape != null) 
					pressThisOnEscape.doClick(20);
	        }
		};

		InputMap iMap = window.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");

		ActionMap aMap = window.getRootPane().getActionMap();
		aMap.put("escape", escAction); 
	}


	/**
	 * Get a new Dimension object and keep the aspect ratio based on the boundary<br>
	 * So if you want to resize an image or whatever area from a large area to a smaller but wants to
	 * keep the aspect ratio of the area...
	 * 
	 * @param originArea Current size of an image or a area
	 * @param boundary The boundary which the are should fit in
	 * @return a Dimension keeping the aspect ratio of the inputArea
	 */
	public static Dimension getScaledDimension(Dimension originArea, Dimension boundary)
	{

		int originWidth = originArea.width;
		int originHight = originArea.height;
		int boundWidth  = boundary.width;
		int boundHeight = boundary.height;
		int newWidth    = originWidth;
		int newHeight   = originHight;

		// first check if we need to scale width
		if ( originWidth > boundWidth )
		{
			// scale width to fit
			newWidth = boundWidth;
			// scale height to maintain aspect ratio
			newHeight = (newWidth * originHight) / originWidth;
		}

		// then check if we need to scale even with the new height
		if ( newHeight > boundHeight )
		{
			// scale height to fit instead
			newHeight = boundHeight;
			// scale width to maintain aspect ratio
			newWidth = (newHeight * originWidth) / originHight;
		}

		return new Dimension(newWidth, newHeight);
	}	

	/**
	 * Create a byte array of a IconImage
	 * 
	 * @param iconImage
	 * @return a byte[] if problems it will ruturn null
	 */
	public static byte[] toBytArray(ImageIcon iconImage)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(toBufferedImage(iconImage), "jpg", baos );
			return baos.toByteArray();
		}
		catch (IOException ignore) {}
		return null;
	}

	/**
	 * Create a BufferedImage from a ImageIcon
	 * 
	 * @param iconImage
	 * @return
	 */
	public static BufferedImage toBufferedImage(ImageIcon iconImage)
	{
		return toBufferedImage(iconImage.getImage());
	}

	/**
	 * Create a BufferedImage from a image
	 * 
	 * @param image
	 * @return
	 */
	public static BufferedImage toBufferedImage(final Image image)
	{
		if ( image instanceof BufferedImage )
			return (BufferedImage) image;

		BufferedImage bi = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = bi.createGraphics();
		g2.drawImage(image, 0, 0, null);
		return bi;
	}

	public static Image iconToImage(Icon icon)
	{
		if ( icon instanceof ImageIcon )
		{
			return ((ImageIcon) icon).getImage();
		}
		else
		{
			int w = icon.getIconWidth();
			int h = icon.getIconHeight();
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice gd = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gd.getDefaultConfiguration();
			BufferedImage image = gc.createCompatibleImage(w, h);
			Graphics2D g = image.createGraphics();
			icon.paintIcon(null, g, 0, 0);
			g.dispose();
			return image;
		}
	}

	/** 
	 * Just a wrapper to make sure we execute fireTableDataChanged on the Event Dispatch Thread 
	 */
	public static void fireTableDataChanged(final AbstractTableModel tm)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			tm.fireTableDataChanged();
		}
		else
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					tm.fireTableDataChanged();
				}
			});
		}
	}

	/** 
	 * Just a wrapper to make sure we execute fireTableDataChanged on the Event Dispatch Thread 
	 */
	public static void fireTableStructureChanged(final AbstractTableModel tm)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			tm.fireTableStructureChanged();
		}
		else
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					tm.fireTableStructureChanged();
				}
			});
		}
	}

	/** 
	 * Just a wrapper to make sure we execute fireTableRowsInserted on the Event Dispatch Thread 
	 */
	public static void fireTableRowsInserted(final AbstractTableModel tm, final int firstRow, final int lastRow)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			tm.fireTableRowsInserted(firstRow, lastRow);
		}
		else
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					tm.fireTableRowsInserted(firstRow, lastRow);
				}
			});
		}
	}

	/** 
	 * Just a wrapper to make sure we execute fireTableRowsDeleted on the Event Dispatch Thread 
	 */
	public static void fireTableRowsDeleted(final AbstractTableModel tm, final int firstRow, final int lastRow)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			tm.fireTableRowsDeleted(firstRow, lastRow);
		}
		else
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					tm.fireTableRowsDeleted(firstRow, lastRow);
				}
			});
		}
	}

	/** 
	 * Just a wrapper to make sure we execute fireTableRowsUpdated on the Event Dispatch Thread 
	 */
	public static void fireTableRowsUpdated(final AbstractTableModel tm, final int firstRow, final int lastRow)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			tm.fireTableRowsUpdated(firstRow, lastRow);
		}
		else
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					tm.fireTableRowsUpdated(firstRow, lastRow);
				}
			});
		}
	}

	/** 
	 * Just a wrapper to make sure we execute fireTableCellUpdated on the Event Dispatch Thread 
	 */
	public static void fireTableCellUpdated(final AbstractTableModel tm, final int row, final int column)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			tm.fireTableCellUpdated(row, column);
		}
		else
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					tm.fireTableCellUpdated(row, column);
				}
			});
		}
	}

	/** 
	 * Just a wrapper to make sure we execute fireTableChanged on the Event Dispatch Thread 
	 */
	public static void fireTableCellUpdated(final AbstractTableModel tm, final TableModelEvent tme)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			tm.fireTableChanged(tme);
		}
		else
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					tm.fireTableChanged(tme);
				}
			});
		}
	}

	/**
	 * Show a balloontip for X ms with a text.
	 * @param jcomp         A JComponent, or null. null will try to pickup your last focused component...
	 * @param timeoutVal    Number of milliseconds the popup will be visible
	 * @param ping          Sound an alarm
	 * @param msg           The String you want to display.
	 */
	public static void showTimedBalloonTip(JComponent jcomp, int timeoutVal, boolean ping, String msg)
	{
		if (jcomp == null)
		{
			Component comp = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			if (comp instanceof JComponent)
				jcomp = (JComponent) comp;
			else
				throw new RuntimeException("showTimedBalloonTip: jcomp was null and the derived componen was not a JComponent. comp="+comp);
		}

		// Make some noice
		if (ping)
			UIManager.getLookAndFeel().provideErrorFeedback(jcomp);

		// Show the balloon
		BalloonTip balloonTip = new BalloonTip(jcomp,
				new JLabel(msg),
				new EdgedBalloonStyle(new Color(255,253,245), new Color(64,64,64)),
				new LeftAbovePositioner(15, 10), 
				null);
		TimingUtils.showTimedBalloon(balloonTip, timeoutVal);
	}

	/**
	 * Show a balloontip for X ms with a text. in a JTable for row and column
	 * 
	 * @param jcomp         A JComponent, or null. null will try to pickup your last focused component...
	 * @param row           row int the table
	 * @param col           column in the table
	 * @param timeoutVal    Number of milliseconds the popup will be visible
	 * @param ping          Sound an alarm
	 * @param msg           The String you want to display.
	 */
	public static void showTimedBalloonTip(JComponent jcomp, int row, int col, int timeoutVal, boolean ping, String msg)
	{
		if (jcomp == null)
		{
			Component comp = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			if (comp instanceof JComponent)
				jcomp = (JComponent) comp;
			else
				throw new RuntimeException("showTimedBalloonTip: jcomp was null and the derived componen was not a JComponent. comp="+comp);
		}
		
		JTable table;
		if (jcomp instanceof JTable)
			table = (JTable) jcomp;
		else
		{
			table = null; 
			for (Component c = jcomp.getParent(); c!=null; c=c.getParent())
			{
				if (c instanceof JTable)
					table = (JTable) c;
			}

			if (table == null)
			{
				SwingUtils.printParents(jcomp, "showTimedBalloonTip: ");
				throw new RuntimeException("showTimedBalloonTip: component was not a JTable. comp="+jcomp);
			}
		}

		// Make some noice
		if (ping)
			UIManager.getLookAndFeel().provideErrorFeedback(jcomp);

		// Show the balloon
		TableCellBalloonTip balloonTip = new TableCellBalloonTip(table,  
				new JLabel(msg),
				row, col,
				new EdgedBalloonStyle(new Color(255,253,245), new Color(64,64,64)),
				new LeftAbovePositioner(15, 10), 
				null);
		TimingUtils.showTimedBalloon(balloonTip, timeoutVal);
	}

	/**
	 * Show a balloontip for X ms with a text. in a JTable for row and column
	 * 
	 * @param model         Table model from where this origins
	 * @param row           row int the table
	 * @param col           column in the table
	 * @param timeoutVal    Number of milliseconds the popup will be visible
	 * @param ping          Sound an alarm
	 * @param msg           The String you want to display.
	 */
	public static void showTimedBalloonTip(AbstractTableModel model, int row, int col, int timeoutVal, boolean ping, String msg)
	{
		// If we do not have a TableModel...
		if (model == null)
		{
			showTimedBalloonTip((JComponent)null, row, col, timeoutVal, ping, msg);
			return;
		}

		// Try to get the JTable instance... from the Listeners...
		JTable table = getJTableByModel(model);
		showTimedBalloonTip(table, row, col, timeoutVal, ping, msg);
	}

	/**
	 * Get first JTable that the table model is listening on
	 * @param model
	 * @return
	 */
	public static JTable getJTableByModel(AbstractTableModel model)
	{
		JTable table = null;
		for (TableModelListener tml : model.getTableModelListeners())
		{
			if (tml instanceof JTable)
			{
				table = (JTable) tml;
				break;
			}
		}
		return table;
	}
	
	/**
     * Returns the <b>view<b> column position given its name. (not case sensitive)
     *
     * @param columnName string containing name of column to be located
     * @return the view column with <code>columnName</code>, or -1 if not found
     */
	public static int findColumnView(JTable tab, String columnName)
	{
		for (int c=0; c<tab.getColumnCount(); c++)
		{
			if ( columnName.equalsIgnoreCase(tab.getColumnName(c)) )
				return c;
		}
		return -1;
	}

	/**
     * Returns the <b>model<b> column position given its name. (not case sensitive)
     *
     * @param columnName string containing name of column to be located
     * @return the model column with <code>columnName</code>, or -1 if not found
     */
	public static int findColumnModel(JTable tab, String columnName)
	{
		return findColumn(tab.getModel(), columnName);
	}

	/**
     * Returns the <b>model<b> column position given its name. (not case sensitive)
     *
     * @param columnName string containing name of column to be located
     * @return the model column with <code>columnName</code>, or -1 if not found
     */
	public static int findColumn(TableModel tm, String columnName)
	{
		for (int c=0; c<tm.getColumnCount(); c++)
		{
			if ( columnName.equalsIgnoreCase(tm.getColumnName(c)) )
				return c;
		}
		return -1;
	}

	/** 
	 * Get the Window component from a specific Component
	 * 
	 * @param comp
	 * @return null or the Window object...
	 */
	public static Window getParentWindow(Component comp)
	{
		Window window = null;
		for (Component c = comp.getParent(); c!=null; c=c.getParent())
		{
			if (c instanceof Window)
				window = (Window) c;
		}
		return window;
	}
	/** 
	 * Get the Window component, based on the KeyboardFocusManager
	 *  
	 * @param comp
	 * @return null or the Window object...
	 */
	public static Window getParentWindowByFocus()
	{
		// also check:
//		System.out.println("getActiveWindow()          : "+ FocusManager.getCurrentManager().getActiveWindow()          );
//		System.out.println("getCurrentFocusCycleRoot() : "+ FocusManager.getCurrentManager().getCurrentFocusCycleRoot() );
//		System.out.println("getFocusedWindow()         : "+ FocusManager.getCurrentManager().getFocusedWindow()         );
//		System.out.println("getFocusOwner()            : "+ FocusManager.getCurrentManager().getFocusOwner()            );
//		System.out.println("getPermanentFocusOwner()   : "+ FocusManager.getCurrentManager().getPermanentFocusOwner()   );

		return getParentWindow(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
	}

	

	/**
	 * Get a TreePath with the first DefaultMutableTreeNode that matches the "display name"
	 * 
	 * @param tree
	 * @param name
	 * @return
	 */
	public static TreePath findNameInTree(JTree tree, String name) 
	{
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
		return findNameInTree(root, name);
	}

	/**
	 * Get a TreePath with the first DefaultMutableTreeNode that matches the "display name"
	 * 
	 * @param tree
	 * @param name
	 * @return
	 */
	private static TreePath findNameInTree(DefaultMutableTreeNode root, String s) 
	{
		@SuppressWarnings("unchecked")
//		Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
		Enumeration<TreeNode> e = root.depthFirstEnumeration();
		while (e.hasMoreElements()) 
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
			if (node.toString().equalsIgnoreCase(s)) 
			{
				return new TreePath(node.getPath());
			}
		}
		return null;
	}

	/**
	 * Scroll a TreePath to the center of the view<br>
	 * NOTE: This will be deferred executed using SwingUtilities.invokeLater()
	 * @param tree
	 * @param treePath
	 * @param leftJustify
	 */
	public static void scrollToCenter(JTree tree, TreePath treePath, boolean leftJustify)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				if ( !(tree.getParent() instanceof JViewport) )
				{
					tree.scrollPathToVisible(treePath);
					return;
				}

				JViewport viewport = (JViewport) tree.getParent();
				Rectangle rect     = tree.getPathBounds(treePath);

				Rectangle viewRect = viewport.getViewRect();
				rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y);

				int centerX = (viewRect.width - rect.width) / 2;
				int centerY = (viewRect.height - rect.height) / 2;

				if ( rect.x < centerX )
					centerX = -centerX;

				if ( rect.y < centerY )
					centerY = -centerY;

				if (leftJustify)
					centerX = -1000;
				
				rect.translate(centerX, centerY);
				viewport.scrollRectToVisible(rect);
			}
		});
		
	}	
	
	
	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////
	//// TEST CODE
	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////

	public static void main(String[] args)
	{
		String[] columnNames = { "First Name", "Last Name", "Sport", "# of Years", "Vegetarian" };

		Object[][] data = { 
				{ "row 1", "no newlines in this row",      "Snowboarding",                  Integer.valueOf(5),  Boolean.valueOf(false) }, 
				{ "row 2", "2-rows:Ro(ln)wing",            "Ro\r\n\rwing",                      Integer.valueOf(3),  Boolean.valueOf(true) }, 
				{ "row 3", "8-rows,after each char",       "K\nn\ni\nt\nt\ni\nn\ng\n",      Integer.valueOf(2),  Boolean.valueOf(false) }, 
				{ "row 4", "2-rows:Speed(ln) reading",     "Speed\n reading",               Integer.valueOf(20), Boolean.valueOf(true) }, 
				{ "row 5", "Pool(ln)             xxx   -", "Pool\n             xxx   -",    Integer.valueOf(10), Boolean.valueOf(false) }, 
				{ "row 6", "no newlines, 2 lead space",    "  last Row .-.",                 Integer.valueOf(5),  Boolean.valueOf(false) }, 
			};

		JTable jtable = new JTable(data, columnNames);
		
		String xxx = tableToString(jtable);
		System.out.println("##################################");
		System.out.println(xxx);
		System.out.println("##################################");
	}
}

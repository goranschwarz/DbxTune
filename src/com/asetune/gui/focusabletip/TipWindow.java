/*
 * 07/29/2009
 *
 * TipWindow.java - The actual window component representing the tool tip.
 * Copyright (C) 2009 Robert Futrell
 * robert_futrell at users.sourceforge.net
 * http://fifesoft.com/rsyntaxtextarea
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA.
 */
package com.asetune.gui.focusabletip;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;

import com.asetune.utils.Configuration;


/**
 * The actual tool tip component.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class TipWindow extends JWindow implements ActionListener, HyperlinkListener {
	private static final long serialVersionUID = 1L;
	private FocusableTip ft;
	private JEditorPane textArea;
//	private BrowserPane textArea;
	private String text;
	private TipListener tipListener;
	private HyperlinkListener userHyperlinkListener;
	private ToolTipHyperlinkResolver userHyperlinkResolver;

	private static TipWindow visibleInstance;

	private ArrayList<String> historyList  = new ArrayList<String>();
	private int               historyIndex = -1;
	
//	private final static String PRIMARY_JEditorPane_class = System.getProperty("TipWindow.JEditorPane.replacement", "org.fit.cssbox.swingbox.BrowserPane");
	private final static String PRIMARY_JEditorPane_class = System.getProperty("TipWindow.JEditorPane.replacement", "");

	/**
	 * Constructor.
	 *
	 * @param owner The parent window.
	 * @param msg The text of the tool tip.  This can be HTML.
	 */
	public TipWindow(Window owner, FocusableTip ft, String msg) 
	{
		super(owner);
		this.ft = ft;
		this.text = msg;
		tipListener = new TipListener();

		JPanel cp = new JPanel(new BorderLayout());
		cp.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.BLACK), 
			BorderFactory.createEmptyBorder()));
		cp.setBackground(TipUtil.getToolTipBackground());

		try
		{
			if ( ! PRIMARY_JEditorPane_class.trim().equals("") )
			{
				Class<?> clazz = Class.forName(PRIMARY_JEditorPane_class);
				textArea = (JEditorPane) clazz.newInstance();
			}
		}
		catch (Exception e)
		{
			System.out.println("Problems loading HTML Browser replacement/plugin class '"+PRIMARY_JEditorPane_class+"'. Falling back to use the default 'javax.swing.JEditorPane'. Caught: "+e);
		}
		finally
		{
			if (textArea == null)
			{
				textArea = new JEditorPane();
				textArea.setContentType("text/html");
			}
		}
		textArea.setEditable(false);

		// Show the initial text
		showPage(this.text);

		TipUtil.tweakTipEditorPane(textArea);
		if (ft.getImageBase()!=null) // Base URL for images
			if (textArea.getDocument() instanceof HTMLDocument)
				((HTMLDocument)textArea.getDocument()).setBase(ft.getImageBase());
		textArea.addMouseListener(tipListener);
		textArea.addHyperlinkListener(this);
		cp.add(textArea);

		setFocusableWindowState(false);
		setContentPane(cp);
		setBottomPanel(); // Must do after setContentPane()
		pack();

		// InputMap/ActionMap combo doesn't work for JWindows (even when
		// using the JWindow's JRootPane), so we'll resort to KeyListener
		KeyAdapter ka = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode()==KeyEvent.VK_ESCAPE) {
					TipWindow.this.ft.possiblyDisposeOfTipWindow();
				}
			}
		};
		addKeyListener(ka);
		textArea.addKeyListener(ka);

		// Ensure only 1 TipWindow is ever visible.  If the caller does what
		// they're supposed to and only creates these on the EDT, the
		// synchronization isn't necessary, but we'll be extra safe.
		synchronized (TipWindow.class) {
			if (visibleInstance!=null) {
				visibleInstance.dispose();
			}
			visibleInstance = this;
		}

	}


	@Override
	public void actionPerformed(ActionEvent e) 
	{
		if (!getFocusableWindowState()) 
		{
			setFocusableWindowState(true);
			setBottomPanel();
			textArea.removeMouseListener(tipListener);
			pack();
			addWindowFocusListener(new WindowAdapter() 
			{
				@Override
				public void windowLostFocus(WindowEvent e) 
				{
					ft.possiblyDisposeOfTipWindow();
				}
			});
			ft.removeListeners();
			if (e==null) // Didn't get here via our mouseover timer
				requestFocus();
		}
	}


	/**
	 * Disposes of this window.
	 */
	@Override
	public void dispose() 
	{
		//System.out.println("[DEBUG]: Disposing...");
		Container cp = getContentPane();
		for (int i=0; i<cp.getComponentCount(); i++) 
		{
			// Okay if listener is already removed
			cp.getComponent(i).removeMouseListener(tipListener);
		}
		ft.removeListeners();
		super.dispose();
	}


	/**
	 * Workaround for JEditorPane not returning its proper preferred size
	 * when rendering HTML until after layout already done.  See
	 * http://forums.sun.com/thread.jspa?forumID=57&threadID=574810 for a
	 * discussion.
	 */
	void fixSize() 
	{
		Dimension d = textArea.getPreferredSize();
		Rectangle r = null;
		try 
		{
			int docLength = textArea.getDocument().getLength()-1;
			if (docLength < 0)
				docLength = 0;
			r = textArea.modelToView(docLength);
			d.height = r.y + r.height;

			// Ensure the text area doesn't start out too tall or wide.
			d = textArea.getPreferredSize();
//			d.width = Math.min(d.width+25, 320);
//			d.height = Math.min(d.height, 150);

//			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

			if (d.height > (screenSize.height - 80))
				d.height = screenSize.height - 80;
			if (d.width > screenSize.width - 100)
				d.width = screenSize.width - 100;

			textArea.setPreferredSize(d);

		} catch (BadLocationException ble) { // Never happens
			ble.printStackTrace();
		}

		pack(); // Must re-pack to calculate proper size.
	}


	/**
	 * @return the initial text in the tooltip
	 */
	public String getText() 
	{
		return text;
	}


	private void setBottomPanel() 
	{
		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JSeparator(), BorderLayout.NORTH);

		boolean focusable = getFocusableWindowState();
		if (focusable) 
		{
			panel.add(createNavigationPanel(), BorderLayout.LINE_START);

			SizeGrip sg = new SizeGrip();
			sg.applyComponentOrientation(sg.getComponentOrientation()); // Workaround
			panel.add(sg, BorderLayout.LINE_END);
			MouseInputAdapter adapter = new MouseInputAdapter() 
			{
				private Point lastPoint;
				@Override
				public void mouseDragged(MouseEvent e) 
				{
					Point p = e.getPoint();
					SwingUtilities.convertPointToScreen(p, panel);
					if (lastPoint==null) 
					{
						lastPoint = p;
					}
					else 
					{
						int dx = p.x - lastPoint.x;
						int dy = p.y - lastPoint.y;
						setLocation(getX()+dx, getY()+dy);
						lastPoint = p;
					}
				}
				@Override
				public void mousePressed(MouseEvent e) 
				{
					lastPoint = e.getPoint();
					SwingUtilities.convertPointToScreen(lastPoint, panel);
				}
			};
			panel.addMouseListener(adapter);
			panel.addMouseMotionListener(adapter);
			// Don't add tipListener to the panel or SizeGrip
		}
		else 
		{
			panel.setOpaque(false);
			JLabel label = new JLabel(FocusableTip.getString("FocusHotkey"));
			Color fg = UIManager.getColor("Label.disabledForeground");
			Font font = textArea.getFont();
			font = font.deriveFont(font.getSize2D() - 1.0f);
			label.setFont(font);
			if (fg==null) { // Non BasicLookAndFeel-derived Looks
				fg = Color.GRAY;
			}
			label.setOpaque(true);
			Color bg = TipUtil.getToolTipBackground();
			label.setBackground(bg);
			label.setForeground(fg);
			label.setHorizontalAlignment(SwingConstants.TRAILING);
			label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
			panel.add(label);
			panel.addMouseListener(tipListener);
		}

		// Replace the previous SOUTH Component with the new one.
		Container cp = getContentPane();
		if (cp.getComponentCount()==2) // Skip first time through
		{
			Component comp = cp.getComponent(0);
			cp.remove(0);
			JScrollPane sp = new JScrollPane(comp);
			sp.setViewportBorder(BorderFactory.createEmptyBorder());
			sp.setBackground(textArea.getBackground());
			sp.getViewport().setBackground(textArea.getBackground());
			cp.add(sp);
			// What was component 1 is now 0.
			cp.getComponent(0).removeMouseListener(tipListener);
			cp.remove(0);
		}

		cp.add(panel, BorderLayout.SOUTH);
	}


	/**
	 * Sets the listener for hyperlink events in this tip window.
	 *
	 * @param listener The new listener.  The old listener (if any) is
	 *        removed.  A value of <code>null</code> means "no listener."
	 */
	public void setHyperlinkListener(HyperlinkListener listener) 
	{
		// We've added a separate listener, so remove only the user's.
		if (userHyperlinkListener!=null) {
			textArea.removeHyperlinkListener(userHyperlinkListener);
		}
		userHyperlinkListener = listener;
		if (userHyperlinkListener!=null) {
			textArea.addHyperlinkListener(userHyperlinkListener);
		}
	}


	/**
	 * Sets the listener for FucusableTipHyperlinkResolver events in this tip window.
	 *
	 * @param resolver The new resolver.
	 *        removed.  A value of <code>null</code> means "no resolver."
	 */
	public void setHyperlinkResolver(ToolTipHyperlinkResolver resolver) 
	{
		userHyperlinkResolver = resolver;
	}


	/**
	 * Listens for events in this window.
	 */
	private class TipListener extends MouseAdapter 
	{
		public TipListener() 
		{
		}

		@Override
		public void mousePressed(MouseEvent e) 
		{
			actionPerformed(null); // Manually create "real" window
		}

		@Override
		public void mouseExited(MouseEvent e) 
		{
			// Since we registered this listener on the child components of
			// the JWindow, not the JWindow iteself, we have to be careful.
			Component source = (Component)e.getSource();
			Point p = e.getPoint();
			SwingUtilities.convertPointToScreen(p, source);
			if (!TipWindow.this.getBounds().contains(p)) 
			{
				ft.possiblyDisposeOfTipWindow();
			}
		}

	}

	/** Get to next page in the history */
	public void showNextPage()
	{
		showPage(null, null, 1);
	}

	/** Get to previous page in the history */
	public void showPrevPage()
	{
		showPage(null, null, -1);
	}

	/** Show plain HTML page */
	public void showPage(String htmlText)
	{
		showPage(null, htmlText, 0);
	}

	/** Show the specified URL. */
	public void showPage(URL pageUrl)
	{
		showPage(pageUrl, null, 0);
	}

	/**
	 * Display a URL page or a HTML Text
	 * 
	 * @param pageUrl URL to display
	 * @param htmlText HTML TExt to display
	 * @param nextPrevPage (se below)
	 * <ul>
	 *    <li>0 = A new Page</li>
	 *    <li>1 = Next page (url & text can/will be null)</li>
	 *    <li>-1 = Previous page (url & text can/will be null)</li>
	 */
	private void showPage(URL pageUrl, String htmlText, int nextPrevPage)
	{
		if (nextPrevPage == 0 && pageUrl == null && htmlText == null)
			throw new RuntimeException("showPage: both pageUrl and htmlText can't be null");

		//System.out.println("####> showPage(url="+(pageUrl==null?true:false)+", htmlText="+(htmlText==null?true:false)+", nextPrePage="+nextPrevPage+"): ");
		//System.out.println("   #> enter: historyIndex="+historyIndex+", historyList.size()="+historyList.size());

		// Show hour glass cursor while crawling is under way.
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		String listObject = pageUrl != null ? pageUrl.toString() : htmlText; 

		// Add to end of page
		if (nextPrevPage == 0)
		{
			//System.out.println("  ==> Navigating to new page: ");

			// If we have pressed prev/back and klick a new link in that page
			// historyIndex would be less than the history.size()
			// Then we would remove all entries in the stack from historyIndex
			if (historyIndex < historyList.size()-1)
			{
				//System.out.println("   => Truncate history at historyIndex="+(historyIndex+1));

				for (int i=historyIndex+1; i<historyList.size(); i++)
				{
					//System.out.println("      Removing enty["+i+"]="+historyList.get(i));
					historyList.remove(i);
				}
			}

			// Add it to the end of history
			historyList.add(listObject);
			historyIndex++;

			//System.out.println("   => After append history: historyIndex="+historyIndex+", historyList.size()="+historyList.size());
		}
		// Next page
		if (nextPrevPage > 0)
		{
			//System.out.println("  ==> Navigating to NEXT PAGE: ");
			if (historyIndex < historyList.size())
			{
				historyIndex++;
				String entry = historyList.get(historyIndex);
				pageUrl = toUrlEntry(entry);
				if (pageUrl == null)
					htmlText = entry;
				//System.out.println("   => NEXT pageUrl="+pageUrl+", htmlText="+(htmlText!=null?"HAS_HTML_TEXT":null));
			}
		}
		// Previous page
		if (nextPrevPage < 0)
		{
			//System.out.println("  ==> Navigating to PREV PAGE: ");
			if (historyIndex >= 1)
			{
				historyIndex--;
				String entry = historyList.get(historyIndex);
				pageUrl = toUrlEntry(entry);
				if (pageUrl == null)
					htmlText = entry;
				//System.out.println("   => PREV pageUrl="+pageUrl+", htmlText="+(htmlText!=null?"HAS_HTML_TEXT":null));
			}
		}

		if (htmlText != null)
		{
			textArea.setText(htmlText);
			//textArea.setPage((URL)null); // Note this can't be done... but still in here for clarity
//			try
//			{
//				URL tmpUrl = DataURLHandler.createURL(null, "data:text/html,"+htmlText);
//				textArea.setPage(tmpUrl); // Note this can't be done... but still in here for clarity
//				System.out.println("TEXT URL: "+tmpUrl);
//			}
//			catch (MalformedURLException e)
//			{
//				e.printStackTrace();
//				textArea.setText(htmlText);
//			}
//			catch (IOException e)
//			{
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}

			// Reset font etc if it has changed while visiting some page... 
			TipUtil.tweakTipEditorPane(textArea);

			setLocationText(null);
		}
		else if (pageUrl != null)
		{
			URL newUrl = null;
			try
			{
				// Load and display specified page.
				textArea.setPage(pageUrl);
				
				// Get URL of new page being displayed.
				newUrl = textArea.getPage();
				setLocationText(newUrl);		
			}
			catch (Exception e)
			{
				// Remove it from the history 
				historyList.remove(listObject);
				if (nextPrevPage == 0)
					historyIndex--;

				// Show error messsage.
				showError("Unable to load page '"+newUrl+"'", e);
			}
		}
		else
		{
			//System.out.println(" ###> Nothing was made: pageUrl="+pageUrl+", htmlText="+htmlText+".");
		}

		// Return to default cursor.
		setCursor(Cursor.getDefaultCursor());

		// Update buttons based on the page being displayed.
		updateButtons();

		//System.out.println("   #> exit: historyIndex="+historyIndex+", historyList.size()="+historyList.size());
		//System.out.println("   #> history stack, starting at historyIndex "+(historyIndex+1));
		//for (int i=historyIndex+1; i<historyList.size(); i++)
		//	System.out.println("          ["+i+"]="+historyList.get(i));
	}

	/** */
	private URL toUrlEntry(String entry)
	{
		try
		{
			URL url = new URL(entry);
			if ( (url+"").startsWith("data:text/html") )
				return null;
			return url;
		}
		catch(MalformedURLException e)
		{
			// ok no URL, so simply return null, and display it as text.
			return null;
		}
	}



	/** Show dialog box with error message. */
	private void showError(String errorMessage, Exception e)
	{
		if (e != null)
		{
			// Exception To String
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			String stackTrace = sw.toString();

			errorMessage += "\n\n" + stackTrace;
		}

		JOptionPane.showMessageDialog(this, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
	}

	/** Update back and forward buttons based on the page being displayed. */
	private void updateButtons()
	{
		// historyList.size() == 1 : One entry, is empty history
		if (historyList.size() > 1)
		{
			backButton.setEnabled(historyIndex > 0);
			forwardButton.setEnabled(historyIndex < (historyList.size() - 1));
		}
		else
		{
			backButton.setEnabled(false);
			forwardButton.setEnabled(false);
		}
		openInBrowser.setEnabled( ! locationText.getText().trim().equals("") );
	}


	/**
	 * The internal HuperlinkListener, which will act when clicking on links
	 */
	@Override
	public void hyperlinkUpdate(HyperlinkEvent event)
	{
		HyperlinkEvent.EventType eventType = event.getEventType();
		if (eventType == HyperlinkEvent.EventType.ENTERED) 
		{
			setLocationText(event.getURL());
		} 
		else if (eventType == HyperlinkEvent.EventType.EXITED) 
		{
			setLocationText(textArea.getPage());
		}
		else if ( eventType == HyperlinkEvent.EventType.ACTIVATED )
		{
//			if ( event instanceof HTMLFrameHyperlinkEvent )
//			{
//				HTMLFrameHyperlinkEvent linkEvent = (HTMLFrameHyperlinkEvent) event;
//				HTMLDocument document = (HTMLDocument) textArea.getDocument();
//				document.processHTMLFrameHyperlinkEvent(linkEvent);
//			}
//			else
//			{
				if (userHyperlinkResolver != null)
				{
					ResolverReturn rr = userHyperlinkResolver.hyperlinkResolv(event);

					// Just for the sake of it check for null
					if (rr == null)
						return;

					if (rr.getType() == ResolverReturn.Type.OPEN_URL_IN_EXTERNAL_BROWSER)
					{
						if (Desktop.isDesktopSupported())
						{
							Desktop desktop = Desktop.getDesktop();
							if ( desktop.isSupported(Desktop.Action.BROWSE) )
							{
								try
								{
									desktop.browse(rr.getUrl().toURI());

									// Should we close the current tool tip window or not?
									if (rr.isCloseToolTipWindowEnabled())
										ft.possiblyDisposeOfTipWindow();
								}
								catch (Exception ex)
								{
									showError("Problems when open the URL '"+locationText.getText()+"'. Caught: "+ex, ex);
								}
							}
						}
						return;
					}
					
					if (rr.getType() == ResolverReturn.Type.SET_PROPERTY_TEMP)
					{
						Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
						if (conf == null)
							return;

						String str = rr.getStringValue();
						int firstEqual = str.indexOf("=");
						if (firstEqual == -1)
						{
							throw new RuntimeException("ToolTipHyperlinkResolver SET_PROPERTY_TEMP, not a properly formed KeyValString. it should have key=val. Can't find any '=' char in the string '"+str+"'.");
						}
						else
						{
							String propName  = str.substring(0, firstEqual).trim();
							String propValue = str.substring(firstEqual + 1).trim();

							conf.setProperty(propName, propValue);
							conf.save();

							// Should we close the current tool tip window or not? 
							if (rr.isCloseToolTipWindowEnabled())
								ft.possiblyDisposeOfTipWindow();
						}
						
						return;
					}
					
					// OK lets load the requested URL 
					if (rr.hasUrl())
					{
						showPage(rr.getUrl());
					}
					// Or the String returned from the resolver
					else if (rr.hasStingValue())
					{
						showPage(rr.getStringValue());
					}
					// Unknown return type
					else 
					{
						throw new RuntimeException("ToolTipHyperlinkResolver did not have URL nor, HTML Text, not sure what to do here");
					}
				}
				else // Try to load the page if no Resolver was available
				{
					showPage(event.getURL());
				}
//			}
		}
	}

	/** Set the label of what URL we are currently at */
	private void setLocationText(URL url)
	{
		if (url == null)
			locationText.setText("");
		else if (url != null)
			locationText.setText(url.toString());
	}

	/** Helper method to read/create an Image */
	public static ImageIcon readImageIcon(Class<?> clazz, String filename)
	{
		URL url = clazz.getResource(filename);
		if (url == null)
		{
			System.out.println("Can't find the resource for class='"+clazz+"', filename='"+filename+"'.");
			return null;
		}

		return new ImageIcon(Toolkit.getDefaultToolkit().getImage(url));
	}

	// These are the buttons for iterating through the page list.
    private JButton    backButton    = new JButton("<");
    private JButton    forwardButton = new JButton(">");
    private JButton    openInBrowser = new JButton();
	private JTextField locationText  = new JTextField(40);
//	private JTextField locationText  = new JTextField();

	/** creates the navigation panel at the end */
	private JPanel createNavigationPanel()
	{
		// Set up button panel.
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		// backward
		backButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				showPrevPage();
			}
		});
		backButton.setEnabled(false);
		backButton.setText("");
		backButton.setToolTipText("Back");
		backButton.setIcon(readImageIcon(TipWindow.class, "left.png"));
		backButton.setContentAreaFilled(false);
		backButton.setMargin( new Insets(0,0,0,0) );
		gbc.gridx = 0;
		panel.add(backButton, gbc);

		// forward
		forwardButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				showNextPage();
			}
		});
		forwardButton.setEnabled(false);
		forwardButton.setText("");
		forwardButton.setToolTipText("Forward");
		forwardButton.setIcon(readImageIcon(TipWindow.class, "right.png"));
		forwardButton.setContentAreaFilled(false);
		forwardButton.setMargin( new Insets(0,0,0,0) );
		gbc.gridx = 1;
		panel.add(forwardButton, gbc);
		

		// open in browser
		openInBrowser.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (Desktop.isDesktopSupported())
				{
					Desktop desktop = Desktop.getDesktop();
					if ( desktop.isSupported(Desktop.Action.BROWSE) )
					{
						try
						{
							desktop.browse(new URI(locationText.getText()));
						}
						catch (Exception ex)
						{
							showError("Problems when open the URL '"+locationText.getText()+"'. Caught: "+ex, ex);
						}
					}
				}
			}
		});
		openInBrowser.setEnabled(false);
		openInBrowser.setText("");
		openInBrowser.setToolTipText("Open in External Browser");
		openInBrowser.setIcon(readImageIcon(TipWindow.class, "external_browser.png"));
		openInBrowser.setContentAreaFilled(false);
		openInBrowser.setMargin( new Insets(0,0,0,0) );
		gbc.gridx = 2;
		panel.add(openInBrowser, gbc);
		

		// Location
		locationText.setToolTipText("Current URL, or what URL you have the mouse over.");
		locationText.setEditable(false);
		locationText.setBackground(panel.getBackground());
		locationText.setBorder(null);
		gbc.gridx = 3;
//		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(locationText, gbc);
		
		return panel;
	}
}



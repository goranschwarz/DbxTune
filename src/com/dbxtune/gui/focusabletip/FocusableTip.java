/*
 * 07/29/2009
 *
 * FocusableTip.java - A focusable tool tip, like those in Eclipse.
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
package com.dbxtune.gui.focusabletip;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.net.URL;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.MouseInputAdapter;

//import org.fife.ui.rsyntaxtextarea.PopupWindowDecorator;


/**
 * A focusable tool tip, similar to those found in Eclipse.  The user
 * can click in the tip and it becomes a "real," resizable window.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class FocusableTip {

//	private JTextArea textArea;
	private JComponent _comp;
	private TipWindow tipWindow;
	private URL imageBase;
	private TextAreaListener textAreaListener;
	private HyperlinkListener hyperlinkListener;
	private ToolTipHyperlinkResolver hyperlinkResolver;
	private String lastText;

	/**
	 * The screen bounds in which the mouse has to stay for the currently
	 * displayed tip to stay visible.
	 */
	private Rectangle tipVisibleBounds;

	/**
	 * Margin from mouse cursor at which to draw focusable tip.
	 */
//	private static final int X_MARGIN = 18;
	private static final int X_MARGIN = 0;

	/**
	 * Margin from mouse cursor at which to draw focusable tip.
	 */
	private static final int Y_MARGIN = 12;

//	private static final String MSG =
////		"com.dbxtune.gui.focusabletip.FocusableTip";
//		"org.fife.ui.rsyntaxtextarea.focusabletip.FocusableTip";
//	private static final ResourceBundle msg = ResourceBundle.getBundle(MSG);


//	public FocusableTip(JTextArea textArea, HyperlinkListener listener) {
//		setTextArea(textArea);
//		this.hyperlinkListener = listener;
//		textAreaListener = new TextAreaListener();
//		tipVisibleBounds = new Rectangle();
//	}
	public FocusableTip(JComponent comp, HyperlinkListener listener, ToolTipHyperlinkResolver hyperlinkResolver) {
//		setTextArea(comp);
		setComponentArea(comp);
		this.hyperlinkListener = listener;
		this.hyperlinkResolver = hyperlinkResolver;
		textAreaListener = new TextAreaListener();
		tipVisibleBounds = new Rectangle();
	}
	public FocusableTip(JComponent comp, HyperlinkListener listener) {
		this(comp, listener, null);
	}
	public FocusableTip(JComponent comp) {
		this(comp, null, null);
	}

	/**
	 * Compute the bounds in which the user can move the mouse without the
	 * tip window disappearing.
	 */
	private void computeTipVisibleBounds() {
		// Compute area that the mouse can move in without hiding the
		// tip window. Note that Java 1.4 can only detect mouse events
		// in Java windows, not globally.
		Rectangle r = tipWindow.getBounds();
		Point p = r.getLocation();
		SwingUtilities.convertPointFromScreen(p, _comp);
		r.setLocation(p);
		tipVisibleBounds.setBounds(r.x,r.y-15, r.width,r.height+15*2);
	}


	private void createAndShowTipWindow(final MouseEvent e, final String text) {

		Window owner = SwingUtilities.getWindowAncestor(_comp);
		tipWindow = new TipWindow(owner, this, text);
		tipWindow.setHyperlinkListener(hyperlinkListener);
		tipWindow.setHyperlinkResolver(hyperlinkResolver);

		// Give apps a chance to decorate us with drop shadows, etc.
//		PopupWindowDecorator decorator = PopupWindowDecorator.get();
//		if (decorator!=null) {
//			decorator.decorate(tipWindow);
//		}

		// TODO: Position tip window better (handle RTL, edges of screen, etc.).
		// Wrap in an invokeLater() to work around a JEditorPane issue where it
		// doesn't return its proper preferred size until after it is displayed.
		// See http://forums.sun.com/thread.jspa?forumID=57&threadID=574810
		// for a discussion.
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				// If a new FocusableTip is requested while another one is
				// *focused* and visible, the focused tip (i.e. "tipWindow")
				// will be disposed of.  If this Runnable is run after the
				// dispose(), tipWindow will be null.  All of this is done on
				// the EDT so no synchronization should be necessary.
				if (tipWindow==null) {
					return;
				}

				tipWindow.fixSize();
				ComponentOrientation o = _comp.getComponentOrientation();

				Point p = e.getPoint();
				SwingUtilities.convertPointToScreen(p, _comp);

				// Ensure tool tip is in the window bounds.
				// Multi-monitor support - make sure the completion window (and
				// description window, if applicable) both fit in the same
				// window in a multi-monitor environment.  To do this, we decide
				// which monitor the rectangle "p" is in, and use that one.
				Rectangle sb = TipUtil.getScreenBoundsForPoint(p.x, p.y);
				//Dimension ss = tipWindow.getToolkit().getScreenSize();

				// Try putting our stuff "below" the mouse first.
				int y = p.y + Y_MARGIN;
				if (y+tipWindow.getHeight()>=sb.y+sb.height) {
					y = p.y - Y_MARGIN - tipWindow.getHeight();

					// Ensure the top of the tool tip is always visible.
					// The bottom of it may be off-screen if a tool tip is
					// taller than the screen, but that is very unlikely
					if (y < sb.y) {
						y = sb.y + Y_MARGIN;
					}
				}

				// Get x-coordinate of completions.  Try to align left edge
				// with the mouse first (with a slight margin).
				int x = p.x - X_MARGIN; // ltr
				if (!o.isLeftToRight()) {
					x = p.x - tipWindow.getWidth() + X_MARGIN;
				}
				if (x<sb.x) {
					x = sb.x;
				}
				else if (x+tipWindow.getWidth()>sb.x+sb.width) { // completions don't fit
					x = sb.x + sb.width - tipWindow.getWidth();
				}

				//System.out.println("tipWindow.setLocation(x="+x+", y="+y+");");
				// DO NOT position the window above the screen "top"
				if (y < 0)
					y = 10;

				tipWindow.setLocation(x, y);
				tipWindow.setVisible(true);

				computeTipVisibleBounds(); // Do after tip is visible
				textAreaListener.install(_comp);
				lastText = text;

			}
		});

	}


	/**
	 * Returns the base URL to use when loading images in this focusable tip.
	 *
	 * @return The base URL to use.
	 * @see #setImageBase(URL)
	 */
	public URL getImageBase() {
		return imageBase;
	}


	/**
	 * Returns localized text for the given key.
	 *
	 * @param key The key into the resource bundle.
	 * @return The localized text.
	 */
	static String getString(String key) {
//		return msg.getString(key);
		if ("FocusHotkey".equals(key)) return "Press 'F2' for focus";
		return null;
	}


	/**
	 * Disposes of the focusable tip currently displayed, if any.
	 */
	public void possiblyDisposeOfTipWindow() {
		if (tipWindow != null) {
			tipWindow.dispose();
			tipWindow = null;
			textAreaListener.uninstall();
			tipVisibleBounds.setBounds(-1, -1, 0, 0);
			lastText = null;
			_comp.requestFocus();
		}
	}


	void removeListeners() {
		//System.out.println("DEBUG: Removing text area listeners");
		textAreaListener.uninstall();
	}


	/**
	 * Sets the base URL to use when loading images in this focusable tip.
	 * 
	 * @param url The base URL to use.
	 * @see #getImageBase()
	 */
	public void setImageBase(URL url) {
		imageBase = url;
	}


//	private void setTextArea(JTextArea textArea) {
//		this._comp = textArea;
//		// Is okay to do multiple times.
//		ToolTipManager.sharedInstance().registerComponent(textArea);
//	}
	private void setComponentArea(JComponent comp) {
		this._comp = comp;
		// Is okay to do multiple times.
		ToolTipManager.sharedInstance().registerComponent(comp);
	}


	public void toolTipRequested(MouseEvent e, String text) {

		if (text==null || text.length()==0) {
			possiblyDisposeOfTipWindow();
			lastText = text;
			return;
		}

		if (lastText==null || text.length()!=lastText.length()
				|| !text.equals(lastText)) {
			possiblyDisposeOfTipWindow();
			createAndShowTipWindow(e, text);
		}

	}


	private class TextAreaListener extends MouseInputAdapter implements
			CaretListener, ComponentListener, FocusListener, KeyListener {

		@Override
		public void caretUpdate(CaretEvent e) {
			Object source = e.getSource();
			if (source == _comp) {
				possiblyDisposeOfTipWindow();
			}
		}

		@Override
		public void componentHidden(ComponentEvent e) {
			handleComponentEvent(e);
		}

		@Override
		public void componentMoved(ComponentEvent e) {
			handleComponentEvent(e);
		}

		@Override
		public void componentResized(ComponentEvent e) {
			handleComponentEvent(e);
		}

		@Override
		public void componentShown(ComponentEvent e) {
			handleComponentEvent(e);
		}

		@Override
		public void focusGained(FocusEvent e) {
		}

		@Override
		public void focusLost(FocusEvent e) {
			// Only dispose of tip if it wasn't the TipWindow that was clicked
			// "c" can be null, at least on OS X, so we must check that before
			// calling SwingUtilities.getWindowAncestor() to guard against an
			// NPE.
			Component c = e.getOppositeComponent();
			boolean tipClicked = (c instanceof TipWindow) ||
				(c!=null &&
					SwingUtilities.getWindowAncestor(c) instanceof TipWindow);
			if (!tipClicked) {
				possiblyDisposeOfTipWindow();
			}
		}

		private void handleComponentEvent(ComponentEvent e) {
			possiblyDisposeOfTipWindow();
		}

//		public void install(JTextArea textArea) {
//			textArea.addCaretListener(this);
//			textArea.addComponentListener(this);
//			textArea.addFocusListener(this);
//			textArea.addKeyListener(this);
//			textArea.addMouseListener(this);
//			textArea.addMouseMotionListener(this);
//		}
		public void install(JComponent textArea) {
//			textArea.addCaretListener(this);
			textArea.addComponentListener(this);
			textArea.addFocusListener(this);
			textArea.addKeyListener(this);
			textArea.addMouseListener(this);
			textArea.addMouseMotionListener(this);
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode()==KeyEvent.VK_ESCAPE) {
				possiblyDisposeOfTipWindow();
			}
			else if (e.getKeyCode()==KeyEvent.VK_F2) {
				if (tipWindow!=null && !tipWindow.getFocusableWindowState()) {
					tipWindow.actionPerformed(null);
					e.consume(); // Don't do bookmarking stuff
				}
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// possiblyDisposeOfTipWindow();
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			if (tipVisibleBounds==null ||
					!tipVisibleBounds.contains(e.getPoint())) {
				possiblyDisposeOfTipWindow();
			}
		}

		public void uninstall() {
//			_comp.removeCaretListener(this);
			_comp.removeComponentListener(this);
			_comp.removeFocusListener(this);
			_comp.removeKeyListener(this);
			_comp.removeMouseListener(this);
			_comp.removeMouseMotionListener(this);
		}

	}

}
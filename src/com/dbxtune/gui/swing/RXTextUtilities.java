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
package com.dbxtune.gui.swing;

//package com.wordpress.tipsforjava.swing;

import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Utilities;
import javax.swing.text.View;

/**
 * A collection of static methods that provide added functionality for text
 * components (most notably, JTextArea and JTextPane)
 * 
 * See also: javax.swing.text.Utilities
 * 
 * @author Rob Camick
 * @author Darryl Burke
 */
public class RXTextUtilities
{
	/**
	 * Attempt to "center" (or nearer to top) the line containing the caret at the center of the scroll pane.
	 * 
	 * @param component the text component in the scroll pane
	 */
	public static void possiblyMoveLineInScrollPane(JTextComponent component)
	{
		Container container = SwingUtilities.getAncestorOfClass(JViewport.class, component);

		if ( container == null )
			return;

		try
		{
			Rectangle r       = component.modelToView(component.getCaretPosition());
			Rectangle visible = component.getVisibleRect();

			// make slight margin so if next is "near" the top/bottom still move
			visible.height -= 16;
			
			// If the new selection is already in the view, don't scroll,
			// as that is visually jarring.
			if (visible.contains(r)) 
				return;

			visible.x = r.x - (visible.width  - r.width)  / 8;
			visible.y = r.y - (visible.height - r.height) / 8;

			Rectangle bounds = component.getBounds();
			Insets i = component.getInsets();
			bounds.x = i.left;
			bounds.y = i.top;
			bounds.width -= i.left + i.right;
			bounds.height -= i.top + i.bottom;

			if (visible.x < bounds.x) 
				visible.x = bounds.x;

			if (visible.x + visible.width > bounds.x + bounds.width) 
				visible.x = bounds.x + bounds.width - visible.width;

			if (visible.y < bounds.y) 
				visible.y = bounds.y;

			if (visible.y + visible.height > bounds.y + bounds.height) 
				visible.y = bounds.y + bounds.height - visible.height;

			component.scrollRectToVisible(visible);
		}
		catch (BadLocationException ble)
		{
		}
	}

	/**
	 * Attempt to center the line containing the caret at the center of the
	 * scroll pane.
	 * 
	 * @param component the text component in the sroll pane
	 */
	public static void centerLineInScrollPane(JTextComponent component)
	{
		Container container = SwingUtilities.getAncestorOfClass(JViewport.class, component);

		if ( container == null )
			return;

		try
		{
			Rectangle r = component.modelToView(component.getCaretPosition());
			JViewport viewport = (JViewport) container;
			int extentHeight = viewport.getExtentSize().height;
			int viewHeight = viewport.getViewSize().height;

			int y = Math.max(0, r.y - (extentHeight / 2));
			y = Math.min(y, viewHeight - extentHeight);

			viewport.setViewPosition(new Point(0, y));
		}
		catch (BadLocationException ble)
		{
		}
	}

	/**
	 * Return the column number at the Caret position.
	 * 
	 * The column returned will only make sense when using a Monospaced font.
	 */
	public static int getColumnAtCaret(JTextComponent component)
	{
		// Since we assume a monospaced font we can use the width of a single
		// character to represent the width of each character

		FontMetrics fm = component.getFontMetrics(component.getFont());
		int characterWidth = fm.stringWidth("0");
		int column = 0;

		try
		{
			Rectangle r = component.modelToView(component.getCaretPosition());
			int width = r.x - component.getInsets().left;
			column = width / characterWidth;
		}
		catch (BadLocationException ble)
		{
		}

		return column + 1;
	}

	/**
	 * Return the line number at the Caret position.
	 */
	public static int getLineAtCaret(JTextComponent component)
	{
		int caretPosition = component.getCaretPosition();
		Element root = component.getDocument().getDefaultRootElement();

		return root.getElementIndex(caretPosition) + 1;
	}

	/**
	 * Return the number of lines of text in the Document
	 */
	public static int getLines(JTextComponent component)
	{
		Element root = component.getDocument().getDefaultRootElement();
		return root.getElementCount();
	}

	/**
	 * Position the caret at the start of a line.
	 */
	public static void gotoStartOfLine(JTextComponent component, int line)
	{
		Element root = component.getDocument().getDefaultRootElement();
		line = Math.max(line, 1);
		line = Math.min(line, root.getElementCount());
		component.setCaretPosition(root.getElement(line - 1).getStartOffset());
	}

	/**
	 * Position the caret on the first word of a line.
	 */
	public static void gotoFirstWordOnLine(final JTextComponent component, int line)
	{
		gotoStartOfLine(component, line);

		// The following will position the caret at the start of the first word

		try
		{
			int position = component.getCaretPosition();
			String first = component.getDocument().getText(position, 1);

			if ( Character.isWhitespace(first.charAt(0)) )
			{
				component.setCaretPosition(Utilities.getNextWord(component, position));
			}
		}
		catch (Exception e)
		{
		}
	}

	/**
	 * Return the number of lines of text, including wrapped lines.
	 */
	public static int getWrappedLines(JTextArea component)
	{
		View view = component.getUI().getRootView(component).getView(0);
		int preferredHeight = (int) view.getPreferredSpan(View.Y_AXIS);
		int lineHeight = component.getFontMetrics(component.getFont()).getHeight();
		return preferredHeight / lineHeight;
	}

	/**
	 * Return the number of lines of text, including wrapped lines.
	 */
	public static int getWrappedLines(JTextComponent component)
	{
		int lines = 0;

		View view = component.getUI().getRootView(component).getView(0);

		int paragraphs = view.getViewCount();

		for (int i = 0; i < paragraphs; i++)
		{
			lines += view.getView(i).getViewCount();
		}

		return lines;
	}
}

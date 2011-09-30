package com.asetune.gui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

/**
 * A combo box that lets the user choose a color.
 * The appaerance of the combo box is fully customizable: <br>
 * It can show colored lines, rectangles or text:<br>
 * <table align="top">
 * <tr><th></th><th>Line</th><th>Rect</th><th>Text</th></tr>
 * <tr><td>Closed:</td><td><img src="doc-files/cline.png"></td><td><img src="doc-files/crect.png"></td><td><img src="doc-files/ctextonly.png"></td></tr>
 * <tr><td>Open:</td><td><img src="doc-files/line.png"></td><td><img src="doc-files/rect.png"></td><td><img src="doc-files/textonly.png"></td></tr>
 * </table>
 * <br>You can have also lines or rectangles with text:<br> 
 * <table>
 * <tr><th>Line</th><th>Rect</th></tr>
 * <tr><td><img src="doc-files/linetext.png"></td><td><img src="doc-files/recttext.png"></td></tr>
 * </table>
 * <P><DL>
 * <DT><B>License:</B></DT>
 * <DD><pre>
 *  Copyright © 2006, 2007 Roberto Mariottini. All rights reserved.
 *
 *  Permission is granted to anyone to use this software in source and binary forms
 *  for any purpose, with or without modification, including commercial applications,
 *  and to alter it and redistribute it freely, provided that the following conditions
 *  are met:
 *
 *  o  Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  o  The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software
 *     in a product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 *  o  Altered source versions must be plainly marked as such, and must not
 *     be misrepresented as being the original software.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *  FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 *  OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 *  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 * <pre></DD></DL>
 *
 * @version 1.0
 * @author Roberto Mariottini
 * 
 * Goran Schwarz, Changed the setSelectedItem(Object color), which didn't work to 100%
 */
public final class JColorComboBox
    extends JComboBox
{
	private static final long	serialVersionUID	= 9029315930996566434L;
	/** Show the color with a colored line. */
	public static final int	  LINE	             = 0;
	/** Show the color with a colored rectangle. */
	public static final int	  RECT	             = 1;
	/** Show the color with a colored string. */
	public static final int	  TEXT_ONLY	         = 2;

	/** 
	 * Create a color combo box of the specified type. The colors offered are the
	 * predefined colors in the Java class {@link java.awt.Color}. Only graphics
	 * (or only text if type is TEXT_ONLY) is shown.
	 * @param type one of {@link #LINE}, {@link #RECT} or {@link #TEXT_ONLY}.
	 */
	public JColorComboBox(int type)
	{
		init(type, false, new DefaultComboBoxModel(DEFAULT_PAIRS));
	}

	/** 
	 * Create a color combo box of the specified type, optionally showing also
	 * colors names. The colors offered are the
	 * predefined colors in the Java class {@link java.awt.Color}.
	 * @param type one of {@link #LINE}, {@link #RECT} or {@link #TEXT_ONLY}.
	 * @param showText true to show color names also, false to show only color
	 *        graphics (if type is TEXT_ONLY then this is assumed to be true). 
	 */
	public JColorComboBox(int type, boolean showText)
	{
		if (type == TEXT_ONLY && !showText)
		{
			System.out.println("JColorComboBox: constructor called with (type == TEXT_ONLY) and (showText == false), assuming (showText == true).");
		}
		init(type, showText, new DefaultComboBoxModel(DEFAULT_PAIRS));
	}

	/** 
	 * Create a color combo box of the specified type, offering the specified colors. 
	 * @param type one of {@link #LINE} or {@link #RECT} ({@link #TEXT_ONLY} is invalid and ignored).
	 * @param colors the colors used to fill the combo box.
	 */
	public JColorComboBox(int type, Color[] colors)
	{
		if (type == TEXT_ONLY)
		{
			System.out.println("JColorComboBox: constructor called with (type == TEXT_ONLY) and no text info, ignored.");
			type = LINE;
		}
		Pair[] pairs = new Pair[colors.length];
		for (int i = 0; i < pairs.length; ++i)
		{
			pairs[i] = new Pair(colors[i], null);
		}
		init(type, false, new DefaultComboBoxModel(pairs));
	}

	/** 
	 * Create a color combo box of the specified type, offering the specified colors
	 * and color names. 
	 * @param type one of {@link #LINE}, {@link #RECT} or {@link #TEXT_ONLY}.
	 * @param colors the colors used to fill the combo box.
	 * @param colorNames the color names to use.
	 */
	public JColorComboBox(int type, Color[] colors, String[] colorNames)
	{
		Pair[] pairs = new Pair[colors.length];
		for (int i = 0; i < pairs.length; ++i)
		{
			pairs[i] = new Pair(colors[i], colorNames[i]);
		}
		init(type, true, new DefaultComboBoxModel(pairs));
	}

	/** 
	 * Call this function to enable/disable the showing of colors names.
	 * @param showText true to show color names, false to show only color
	 *        graphics (if the type is TEXT_ONLY then this is assumed to be true). 
	 * @see #getShowText
	 */
	public void setShowText(boolean showText)
	{
		if (type == TEXT_ONLY && !showText)
		{
			System.out.println("JColorComboBox: setShowText(false) called with (type == TEXT_ONLY), ignored.");
			return;
		}

		this.showText = showText;
	}

	/** 
	 * Returns the text showing status.
	 * @return true to if names are shown, false otherwise (if the type is TEXT_ONLY
	 *         then this will return true). 
	 * @see #setShowText
	 */
	public boolean getShowText()
	{
		return showText;
	}

	/** 
	 * Set this combo box of the specified type.
	 * This will reset any color thickness, color width and item height settings.
	 * @param type one of {@link #LINE}, {@link #RECT} or {@link #TEXT_ONLY}.
	 */
	public void setType(int type)
	{
		if (type == this.type)
		{
			return;
		}
		if (type == TEXT_ONLY)
		{
			if (getModel().getSize() > 0 && !(((Pair) getModel().getElementAt(0)).name == null))
			{
				System.out.println("JColorComboBox: setType(TEXT_ONLY) called with no text info, ignored.");
				return;
			}
			showText = true;
		}
		this.type = type;
		thickness = (type == LINE ? DEFAULT_LINE_THICKNESS : DEFAULT_RECT_THICKNESS);
		height = DEFAULT_HEIGHT;
		width = DEFAULT_WIDTH;
		setRenderer(new ComboBoxRenderer(this, thickness, DEFAULT_HEIGHT, DEFAULT_WIDTH));
	}

	/** 
	 * Returns the type of this combo box.
	 * @return one of {@link #LINE}, {@link #RECT} or {@link #TEXT_ONLY}.
	 * @see #setType
	 */
	public int getType()
	{
		return type;
	}

	/**
	 * Set the thickness of the color line (if type is {@link #LINE})
	 * or the height of the color rectangle (if type is {@link #RECT}).
	 * The line thickness can't be less than one, and the retangle height
	 * also has an inferior limit. Calling this function will reset the item height.
	 * @param thickness the line thickness or the rectangle height.
	 * @see #setItemHeight
	 */
	public void setColorThickness(int thickness)
	{
		if (thickness == this.thickness)
		{
			return;
		}
		if (thickness < 1)
		{
			thickness = 1;
		}
		int def_thickness = (type == LINE ? DEFAULT_LINE_THICKNESS : DEFAULT_RECT_THICKNESS);
		if (thickness < def_thickness)
		{
			thickness = def_thickness;
		}
		this.thickness = thickness;
		height = thickness + DEFAULT_MARGINS;
		if (height < DEFAULT_HEIGHT)
		{
			height = DEFAULT_HEIGHT;
		}
		setRenderer(new ComboBoxRenderer(this, thickness, height, width));
	}

	/** 
	 * Returns the current color line thickness/rectangle height.
	 * @return the current color line thickness/rectangle height.
	 * @see #setColorThickness
	 */
	public int getColorThickness()
	{
		return thickness;
	}

	/** 
	 * Sets the height of the combo box list item.
	 * This is to leave more space between color lines/rectangles.
	 * The height can't be less than the current thickness.
	 * @param height the list item height
	 */
	public void setItemHeight(int height)
	{
		if (height == this.height)
		{
			return;
		}
		if (height < thickness)
		{
			height = thickness;
		}
		setRenderer(new ComboBoxRenderer(this, thickness, height, width));
	}

	/** 
	 * Returns the current height of the combo box list item.
	 * @return the list item height
	 * @see #setItemHeight
	 */
	public int getItemHeight()
	{
		return height;
	}

	/** 
	 * Sets the width of the color line/rectangle. It can't be less than one. 
	 * @param width the color line/rectangle width. 
	 */
	public void setColorWidth(int width)
	{
		if (width == this.width)
		{
			return;
		}
		if (width < 1)
		{
			width = 1;
		}
		this.width = width;
		setRenderer(new ComboBoxRenderer(this, thickness, height, width));
	}

	/** 
	 * Returns the width of the color line/rectangle.
	 * @return the color line/rectangle width.
	 * @see #setColorWidth
	 */
	public int getColorWidth()
	{
		return width;
	}

	/**
	 * Sets the font to use for this combo box. This will change something only 
	 * if text is enabled.
	 * @param font the font to use to render text
	 * @see #setShowText
	 */
	public void setFont(Font font)
	{
		super.setFont(font);
		setRenderer(new ComboBoxRenderer(this, thickness, height, width));
	}

	/** 
	 * Returns the selected color.
	 * @return the currently selected color
	 */
	public Color getSelectedColor()
	{
		return ((Pair) getSelectedItem()).color;
	}

	/** 
	 * Select a color.
	 * @param color the color to select. If the color is not present in the color
	 *        list, this function has no effect
	 */
	public void setSelectedColor(Color color)
	{
		DefaultComboBoxModel model = (DefaultComboBoxModel) getModel();
		for (int i = 0; i < model.getSize(); ++i)
		{
			Pair p = (Pair) model.getElementAt(i);
			if (p.color.equals(color))
			{
				setSelectedIndex(i);
				break;
			}
		}
	}

	/** 
	 * Select a string that is in the model.
	 * @param str the string to select. If the string is not present in the color
	 *        list, this function has no effect
	 */
	public void setSelectedText(String text)
	{
		DefaultComboBoxModel model = (DefaultComboBoxModel) getModel();
		for (int i = 0; i < model.getSize(); ++i)
		{
			Pair p = (Pair) model.getElementAt(i);
			if (p.name != null && p.name.equals(text))
			{
				setSelectedIndex(i);
				break;
			}
		}
	}

	/** 
	 * Select a color.
	 * @param color the color to select. If the color is not present in the color
	 *        list, this function has no effect
	 * @see #setSelectedColor
	 */
	public void setSelectedItem(Object color)
	{
		System.out.println("JColorComboBox.setSelectedItem(Object): color.getClass='"+color.getClass().getName()+"', color.toString='"+color+"'.");
		if (color instanceof Pair)
		{
			super.setSelectedItem(color);
			return;
		}
		if (color instanceof Color)
		{
			setSelectedColor((Color) color);
			return;
		}
		if (color != null)
			setSelectedText(color.toString());
		super.setSelectedItem(color);
	}

	private static final int	  DEFAULT_LINE_THICKNESS	= 2;
	private static final int	  DEFAULT_RECT_THICKNESS	= 10;
	private static final int	  DEFAULT_HEIGHT	     = 16;
	private static final int	  DEFAULT_MARGINS	     = DEFAULT_HEIGHT - DEFAULT_RECT_THICKNESS;
	private static final int	  DEFAULT_WIDTH	         = 80;

	private static final Color[]	DEFAULT_COLORS	     = { Color.black, Color.blue, Color.cyan, Color.darkGray, Color.gray, Color.green, Color.lightGray, Color.magenta, Color.orange, Color.pink, Color.red, Color.white, Color.yellow, };
	private static final String[]	DEFAULT_COLOR_NAMES	 = { "Black", "Blue", "Cyan", "Dark gray", "Gray", "Green", "Light gray", "Magenta", "Orange", "Pink", "Red", "White", "Yellow", };
	private static final Pair[]	  DEFAULT_PAIRS;
	static
	{
		DEFAULT_PAIRS = new Pair[DEFAULT_COLORS.length];
		for (int i = 0; i < DEFAULT_PAIRS.length; ++i)
		{
			DEFAULT_PAIRS[i] = new Pair(DEFAULT_COLORS[i], DEFAULT_COLOR_NAMES[i]);
		}
	}

	private final static class Pair
	{
		Color	color;
		String	name;

		public Pair(Color color, String name)
		{
			this.color = color;
			this.name = name;
		}

		public String toString()
		{
			return name;
		}
	}

	private int	    type;
	private boolean	showText;
	private int	    thickness;
	private int	    height;
	private int	    width;

	private void init(int type, boolean showText, ComboBoxModel model)
	{
		this.type = type;
		if (type == TEXT_ONLY)
		{
			showText = true;
		}
		this.showText = showText;
		setModel(model);
		setEditable(false);
		thickness = (type == LINE ? DEFAULT_LINE_THICKNESS : DEFAULT_RECT_THICKNESS);
		height = DEFAULT_HEIGHT;
		width = DEFAULT_WIDTH;
		setRenderer(new ComboBoxRenderer(this, thickness, DEFAULT_HEIGHT, DEFAULT_WIDTH));
	}

	private final class ComboBoxRenderer
	    extends JPanel
	    implements ListCellRenderer
	{
		private static final long	serialVersionUID	= 1276567177059308897L;

		private Color		      color;
		private int		          width;
		private int		          thickness;
		private int		          height;
		private int		          disp;

		JPanel		              textPanel;
		JLabel		              text;

		public ComboBoxRenderer(JComboBox combo, int thickness, int height, int width)
		{
			this.thickness = thickness;
			this.width = width;
			this.height = height;
			disp = (height / 2) - (thickness / 2);
			textPanel = new JPanel();
			textPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 0));
			textPanel.add(this);
			text = new JLabel();
			text.setOpaque(true);
			text.setFont(combo.getFont());
			textPanel.add(text);
		}

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			if (isSelected)
			{
				setBackground(list.getSelectionBackground());
			}
			else
			{
				setBackground(JColorComboBox.super.getBackground());
			}

			color = ((Pair) value).color;
			if (showText)
			{
				text.setText(((Pair) value).name);
				text.setForeground(color);
				text.setBackground(getBackground());
				if (type == TEXT_ONLY)
				{
					return text;
				}
				else
				{
					textPanel.setBackground(getBackground());
					return textPanel;
				}
			}

			return this;
		}

		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			g.setColor(color);
			g.fillRect(2, disp, getWidth() - 4, thickness);
		}

		public Dimension getPreferredSize()
		{
			return new Dimension(width, height);
		}
	}
}

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
package com.dbxtune.gui;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JTextArea;

 
/**
 * Draws line numbers next to each line, in the same font as the text.
 * Based upon the comment in {@link #getInsets(Insets) getInsets} maybe the
 * "line numbering" could be a border?
 */
public class LineNumberedPaper extends JTextArea {
 
    private static final long serialVersionUID = -4712451085091820083L;

public static void main(String[] args)
   {
      javax.swing.JFrame frame = new javax.swing.JFrame("Line Numbers...");
      frame.addWindowListener(new java.awt.event.WindowAdapter() {
         @Override
		public void windowClosing(java.awt.event.WindowEvent ev) {
            System.exit(0);
         }
      });
 
      java.awt.Container contentPane = frame.getContentPane();
      contentPane.setLayout(new java.awt.GridLayout(0,2));
 
      javax.swing.JPanel subpanel;
      LineNumberedPaper lnp;
 
      int []justified = { LineNumberedBorder.LEFT_JUSTIFY,
                          LineNumberedBorder.RIGHT_JUSTIFY};
 
      String []labels = { "Left Justified",
                          "Right Justified"};
 
      for (int idx = 0; idx < labels.length; idx++) {
         lnp = new LineNumberedPaper(10,10);
         lnp.setLineNumberJustification(justified[idx]);
 
         subpanel = new javax.swing.JPanel(new java.awt.BorderLayout());
         subpanel.add(new javax.swing.JLabel(labels[idx]), java.awt.BorderLayout.NORTH);
         subpanel.add(new javax.swing.JScrollPane(lnp), java.awt.BorderLayout.CENTER);
         contentPane.add(subpanel);
      }
 
      frame.setSize(800,600);
      //frame.show();
      frame.setVisible(true);
   } // main
 
   /**
    * The line number should be right justified.
    */
   public static int RIGHT_JUSTIFY = 0;
 
   /**
    * The line number should be left justified.
    */
   public static int LEFT_JUSTIFY = 1;
 
   /**
    * Indicates the justification of the text of the line number.
    */
   private int lineNumberJustification = RIGHT_JUSTIFY;
 
   public LineNumberedPaper(int rows, int cols) {
      super(rows, cols);
      setOpaque(false);
      // if this is NOT opaque...then painting is a problem...
      // basically...this draws the line numbers...
      // but...super.paintComponent()...erases the background...and the
      // line numbers...what to do?
      //
      // "workaround": paint the background in this class...
   }
 
   @Override
public Insets getInsets() {
      return getInsets(new Insets(0,0,0,0));
   }
 
   /**
    * This modifies the insets, by adding space for the line number on the
    * left. Should be modified to add space on the right, depending upon
    * Locale.
    */
   @Override
public Insets getInsets(Insets insets) {
      insets = super.getInsets(insets);
      insets.left += lineNumberWidth();
      return insets;
   }
 
   public int getLineNumberJustification() {
      return lineNumberJustification;
   }
 
   public void setLineNumberJustification(int justify) {
      if (justify == RIGHT_JUSTIFY || justify == LEFT_JUSTIFY) {
         lineNumberJustification = justify;
      }
   }
 
   /**
    * Returns the width, in pixels, of the maximum line number, plus a
    * trailing space.
    */
   private int lineNumberWidth() {
      //
      // note: should this be changed to use all nines for the lineCount?
      // for example, if the number of rows is 111...999 could be wider
      // (in pixels) in a proportionally spaced font...
      //
      int lineCount = Math.max(getRows(), getLineCount() + 1);
      return getFontMetrics(getFont()).stringWidth(lineCount + " ");
   }
 
   //
   // NOTE: This method is called every time the cursor blinks...
   //       so...optimize (later and if possible) for speed...
   //
   @Override
public void paintComponent(Graphics g) {
      Insets insets = getInsets();
 
      Rectangle clip = g.getClipBounds();
 
      g.setColor(getBackground()); // see note in constructor about this...
      g.fillRect(clip.x, clip.y, clip.width, clip.height);
 
      // do the line numbers need redrawn?
      if (clip.x < insets.left) {
         FontMetrics fm = g.getFontMetrics();
         int fontHeight = fm.getHeight();
 
         // starting location at the "top" of the page...
         // y is the starting baseline for the font...
         // should "font leading" be applied?
         int y = fm.getAscent() + insets.top;
 
         //
         // now determine if it is the "top" of the page...or somewhere else
         //
         int startingLineNumber = ((clip.y + insets.top) / fontHeight) + 1;
 
         //
         // use any one of the following if's:
         //
         //			if (startingLineNumber != 1)
         if (y < clip.y) {
            //
            // not within the clip rectangle...move it...
            // determine how many fontHeight's there are between
            // y and clip.y...then add that many fontHeights
            //
 
            y = startingLineNumber * fontHeight - (fontHeight - fm.getAscent());
         }
 
         //
         // options:
         // . write the number rows in the document (current)
         // . write the number of existing lines in the document (to do)
         //   see getLineCount()
         //
         // determine which the "drawing" should end...
         // add fontHeight: make sure...part of the line number is drawn
         //
         // could also do this by determining what the last line
         // number to draw.
         // then the "while" loop whould change accordingly.
         //
         int	yend = y + clip.height + fontHeight;
 
         // base x position of the line number
         int lnxstart = insets.left;
         if (lineNumberJustification == LEFT_JUSTIFY) {
            // actual starting location of the string of a left
            // justified string...it's constant...
            // the right justified string "moves"...
            lnxstart -= lineNumberWidth();
         }
 
         g.setColor(getForeground());
         //
         // loop until out of the "visible" region...
         //
         int length = ("" + Math.max(getRows(), getLineCount() + 1)).length();
         while (y < yend) {
            //
            // options:
            // . left justify the line numbers (current)
            // . right justify the line number (to do)
            //
 
            if (lineNumberJustification == LEFT_JUSTIFY) {
               g.drawString(startingLineNumber + " ", lnxstart, y);
            } else { // right justify
               String label = padLabel(startingLineNumber, length, true);
               g.drawString(label, insets.left - fm.stringWidth(label), y);
            }
 
            y += fontHeight;
            startingLineNumber++;
         }
      } // draw line numbers?
 
      super.paintComponent(g);
   } // paintComponent
 
   /**
    * Create the string for the line number.
    * NOTE: The <tt>length</tt> param does not include the
    * <em>optional</em> space added after the line number.
    *
    * @param lineNumber to stringize
    * @param length     the length desired of the string
    * @param length     the length desired of the string
    *
    * @return the line number for drawing
    */
   private String padLabel(int lineNumber, int length, boolean addSpace) {
      StringBuffer buffer = new StringBuffer();
      buffer.append(lineNumber);
      for (int count = (length - buffer.length()); count > 0; count--) {
         buffer.insert(0, ' ');
      }
      if (addSpace) {
         buffer.append(' ');
      }
      return buffer.toString();
   }
} // LineNumberedPaper

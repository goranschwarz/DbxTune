/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.gui;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JTextArea;
import javax.swing.border.AbstractBorder;

/**
 *  Draws line numbers next to each line, in the same font as the text.
 *  Currently, this can only be used with a <tt>JTextArea</tt> , since it relies
 *  on the <tt>getRows()</tt> and <tt>getLineCount()</tt> methods. A possible
 *  extension, create an interface to return this rows/linecount.
 *
 *@author     Administrator
 *@created    January 29, 2002
 */
public class LineNumberedBorder extends AbstractBorder {

	private static final long serialVersionUID = -2029404543137395812L;

public static void main(String[] args) {
      javax.swing.JFrame frame = new javax.swing.JFrame("Line Numbers (as Borders)...");
      frame.addWindowListener(
         new java.awt.event.WindowAdapter() {
            @Override
			public void windowClosing(java.awt.event.WindowEvent ev) {
               System.exit(0);
            }
         });

      java.awt.Container contentPane = frame.getContentPane();
      contentPane.setLayout(new java.awt.GridLayout(0, 2));

      int[] sides = {
            LineNumberedBorder.LEFT_SIDE,
            LineNumberedBorder.LEFT_SIDE,
            LineNumberedBorder.RIGHT_SIDE,
            LineNumberedBorder.RIGHT_SIDE};

      int[] justified = {
            LineNumberedBorder.LEFT_JUSTIFY,
            LineNumberedBorder.RIGHT_JUSTIFY,
            LineNumberedBorder.LEFT_JUSTIFY,
            LineNumberedBorder.RIGHT_JUSTIFY};

      String[] labels = {
            "Left Side/Left Justified",
            "Left Side/Right Justified",
            "Right Side/Left Justified",
            "Right Side/Right Justified"};

      javax.swing.JPanel subpanel;
      JTextArea textArea;

      boolean useMultipleBorders = false;
      if (args.length > 0 && "multiple".equals(args[0])) {
         useMultipleBorders = true;
      }

      for (int idx = 0; idx < labels.length; idx++) {
         textArea = new JTextArea(10, 10);
         LineNumberedBorder lnb = new LineNumberedBorder(sides[idx], justified[idx]);
         if (useMultipleBorders) {
            textArea.setBorder(
                  javax.swing.BorderFactory.createCompoundBorder(
                  javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5),
                  javax.swing.BorderFactory.createCompoundBorder(
                  javax.swing.BorderFactory.createLineBorder(java.awt.Color.red, 1),
                  javax.swing.BorderFactory.createCompoundBorder(
                  lnb,
                  javax.swing.BorderFactory.createLineBorder(java.awt.Color.blue, 1)
                  )
                  )
                  )
                  );
         } else {
            textArea.setBorder(lnb);
         }

         subpanel = new javax.swing.JPanel(new java.awt.BorderLayout());
         subpanel.add(new javax.swing.JLabel(labels[idx]), java.awt.BorderLayout.NORTH);
         subpanel.add(new javax.swing.JScrollPane(textArea), java.awt.BorderLayout.CENTER);
         contentPane.add(subpanel);
      }

      frame.setSize(800, 600);
      //frame.show();
      frame.setVisible(true);
   }
   // main

   /**
    *  The line numbers should be drawn on the left side of the component.
    */
   public static int LEFT_SIDE = -2;

   /**
    *  The line numbers should be drawn on the right side of the component.
    */
   public static int RIGHT_SIDE = -1;

   /**
    *  The line number should be right justified.
    */
   public static int RIGHT_JUSTIFY = 0;

   /**
    *  The line number should be left justified.
    */
   public static int LEFT_JUSTIFY = 1;

   /**
    *  Indicates the justification of the text of the line number.
    */
   private int lineNumberJustification = RIGHT_JUSTIFY;

   /**
    *  Indicates the location of the line numbers, w.r.t. the component.
    */
   private int location = LEFT_SIDE;

   public LineNumberedBorder(int location, int justify) {
      setLocation(location);
      setLineNumberJustification(justify);
   }

   @Override
public Insets getBorderInsets(Component c) {
      return getBorderInsets(c, new Insets(0, 0, 0, 0));
   }

   /**
    *  This modifies the insets, by adding space for the line number on the
    *  left. Should be modified to add space on the right, depending upon
    *  Locale.
    *
    *@param  c       Description of the Parameter
    *@param  insets  Description of the Parameter
    *@return         The borderInsets value
    */
   @Override
public Insets getBorderInsets(Component c, Insets insets) {
      // if c is not a JTextArea...nothing is done...
      if (c instanceof JTextArea) {
         int width = lineNumberWidth((JTextArea) c);
         if (location == LEFT_SIDE) {
            insets.left = width;
         } else {
            insets.right = width;
         }
      }
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

   public int getLocation() {
      return location;
   }

   public void setLocation(int loc) {
      if (loc == RIGHT_SIDE || loc == LEFT_SIDE) {
         location = loc;
      }
   }

   /**
    *  Returns the width, in pixels, of the maximum line number, plus a trailing
    *  space.
    *
    *@param  textArea  Description of the Parameter
    *@return           Description of the Return Value
    */
   private int lineNumberWidth(JTextArea textArea) {
      //
      // note: should this be changed to use all nines for the lineCount?
      // for example, if the number of rows is 111...999 could be wider
      // (in pixels) in a proportionally spaced font...
      //
      int lineCount =
            Math.max(textArea.getRows(), textArea.getLineCount() + 1);
      return textArea.getFontMetrics(
            textArea.getFont()).stringWidth(lineCount + " ");
   }

   //
   // NOTE: This method is called every time the cursor blinks...
   //       so...optimize (later and if possible) for speed...
   //
   @Override
public void paintBorder(Component c, Graphics g, int x, int y,
         int width, int height) {

      java.awt.Rectangle clip = g.getClipBounds();

      FontMetrics fm = g.getFontMetrics();
      int fontHeight = fm.getHeight();

      // starting location at the "top" of the page...
      // y is the starting baseline for the font...
      // should "font leading" be applied?
      int ybaseline = y + fm.getAscent();

      //
      // now determine if it is the "top" of the page...or somewhere else
      //
      int startingLineNumber = (clip.y / fontHeight) + 1;

      //
      // use any one of the following if's:
      //
//		if (startingLineNumber != 1)
      if (ybaseline < clip.y) {
         //
         // not within the clip rectangle...move it...
         // determine how many fontHeight's there are between
         // y and clip.y...then add that many fontHeights
         //
         ybaseline = y + startingLineNumber * fontHeight -
               (fontHeight - fm.getAscent());
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
      //int	yend = y + clip.height + fontHeight;
      //int	yend = ybaseline + height + fontHeight; // original
      int yend = ybaseline + height;
      if (yend > (y + height)) {
         yend = y + height;
      }

      JTextArea jta = (JTextArea) c;
      int lineWidth = lineNumberWidth(jta);

      // base x position of the line number
      int lnxstart = x;
      if (location == LEFT_SIDE) {
         // x (LEFT) or (x + lineWidth) (RIGHT)
         // (depends upon justification)
         if (lineNumberJustification == LEFT_JUSTIFY) {
            lnxstart = x;
         } else {
            // RIGHT JUSTIFY
            lnxstart = x + lineWidth;
         }
      } else {
         // RIGHT SIDE
         // (y + width) - lineWidth (LEFT) or (y + width) (RIGHT)
         // (depends upon justification)
         if (lineNumberJustification == LEFT_JUSTIFY) {
            lnxstart = (y + width) - lineWidth;
         } else {
            // RIGHT JUSTIFY
            lnxstart = (y + width);
         }
      }

      g.setColor(c.getForeground());
      //
      // loop until out of the "visible" region...
      //
      int length = ("" + Math.max(jta.getRows(), jta.getLineCount() + 1)).length();
      while (ybaseline < yend) {
         //
         // options:
         // . left justify the line numbers
         // . right justify the line numbers
         //

         if (lineNumberJustification == LEFT_JUSTIFY) {
            g.drawString(startingLineNumber + " ", lnxstart, ybaseline);
         } else {
            // right justify
            String label = padLabel(startingLineNumber, length, true);
            g.drawString(label, lnxstart - fm.stringWidth(label), ybaseline);
         }

         ybaseline += fontHeight;
         startingLineNumber++;
      }
   }
   // paintComponent

   /**
    *  Create the string for the line number. NOTE: The <tt>length</tt> param
    *  does not include the <em>optional</em> space added after the line number.
    *
    *@param  lineNumber  to stringize
    *@param  length      the length desired of the string
    *@param  addSpace    Description of the Parameter
    *@return             the line number for drawing
    */
   private static String padLabel(int lineNumber, int length, boolean addSpace) {
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
}
// LineNumberedBorder

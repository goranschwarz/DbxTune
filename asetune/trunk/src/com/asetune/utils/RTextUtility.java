package com.asetune.utils;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;

import org.fife.ui.rtextarea.ChangeableHighlightPainter;
import org.fife.ui.rtextarea.SearchEngine;

public class RTextUtility
{
	public static int markAll(JTextArea ta, Color color, String[] toMarkArr)
	{
		return markAll(ta, color, Arrays.asList(toMarkArr));
	}

	public static int markAll(JTextArea ta, Color color, Collection<String> toMarkColl)
	{
		boolean matchCase = true;
		boolean wholeWord = true;
		boolean regex     = false;

		//Color.ORANGE
		ChangeableHighlightPainter markAllHighlightPainter = new ChangeableHighlightPainter(color);
		markAllHighlightPainter.setRoundedEdges(true);

		Highlighter	h = ta.getHighlighter();

		int numMarked = 0;
		for (String toMark : toMarkColl)
		{
			if ( toMark != null && h != null )
			{
				int caretPos = ta.getCaretPosition();

				ta.setCaretPosition(0);
				boolean found = SearchEngine.find(ta, toMark, true, matchCase, wholeWord, regex);
				while (found)
				{
					int start = ta.getSelectionStart();
					int end = ta.getSelectionEnd();

					try 
					{
						h.addHighlight(start, end, markAllHighlightPainter);
					}
					catch (BadLocationException ble) 
					{
						ble.printStackTrace();
					}

					numMarked++;
					found = SearchEngine.find(ta, toMark, true, matchCase, wholeWord, regex);
				}
				ta.setCaretPosition(caretPos);
				ta.repaint();
			}
		}
		return numMarked;
	}

}

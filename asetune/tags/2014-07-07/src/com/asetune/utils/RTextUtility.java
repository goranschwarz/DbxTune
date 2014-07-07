package com.asetune.utils;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;

import org.fife.ui.rtextarea.ChangeableHighlightPainter;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

public class RTextUtility
{
	public static int markAll(JTextArea ta, Color color, String[] toMarkArr)
	{
		return markAll(ta, color, Arrays.asList(toMarkArr));
	}

	public static int markAll(JTextArea ta, Color color, Collection<String> toMarkColl)
	{
//		boolean matchCase = true;
//		boolean wholeWord = true;
//		boolean regex     = false;

		if (toMarkColl == null)
			return 0;
		
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

				SearchContext searchCtx = new SearchContext();
				searchCtx.setSearchFor(toMark);
				searchCtx.setMatchCase(true);
				searchCtx.setWholeWord(true);
				
				ta.setCaretPosition(0);
//				boolean found = SearchEngine.find(ta, toMark, true, matchCase, wholeWord, regex);
//				boolean found = SearchEngine.find(ta, searchCtx);
//				while (found)
				SearchResult found = SearchEngine.find(ta, searchCtx);
				while (found.wasFound())
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
//					found = SearchEngine.find(ta, toMark, true, matchCase, wholeWord, regex);
					found = SearchEngine.find(ta, searchCtx);
				}
				ta.setCaretPosition(caretPos);
				ta.repaint();
			}
		}
		return numMarked;
	}

}

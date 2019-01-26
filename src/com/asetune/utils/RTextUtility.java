/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.utils;

import java.awt.Color;
import java.util.ArrayList;
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
	public static int markAll(JTextArea ta, Color color, String toMark)
	{
		ArrayList<String> list = new ArrayList<String>(1);
		list.add(toMark);
		
		return markAll(ta, color, list);
	}

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
	
	public static void unMarkAll(JTextArea ta)
	{
//		((RTextAreaHighlighter)ta.getHighlighter()).clearMarkAllHighlights();
//		ta.repaint();

		SearchContext context = new SearchContext();
		context.setMarkAll(false);
		SearchEngine.find(ta, context);
	}

}

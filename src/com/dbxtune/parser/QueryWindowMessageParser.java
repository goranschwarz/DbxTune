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
package com.dbxtune.parser;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rtextarea.GutterIconInfo;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.dbxtune.Version;
import com.dbxtune.tools.sqlw.msg.JAseMessage;
import com.dbxtune.utils.SwingUtils;

public class QueryWindowMessageParser
extends AbstractParser
{
	public static final ImageIcon ICON_ERROR = SwingUtils.readImageIcon(Version.class, "images/error_16.png");

	private DefaultParseResult        _result = new DefaultParseResult(this);
	private ArrayList<GutterIconInfo> _addedErrorIcons = new ArrayList<GutterIconInfo>();
	private RTextScrollPane           _scroll = null;

	public QueryWindowMessageParser(RTextScrollPane scroll)
	{
		_scroll = scroll;
	}

	@Override 
	public ParseResult parse(RSyntaxDocument doc, String style)
	{
		// Remove old notices
		_result.clearNotices();

		// Remove old icons on the left hand side
		for (GutterIconInfo gci : _addedErrorIcons)
			_scroll.getGutter().removeTrackingIcon(gci);
		_addedErrorIcons.clear();

		long start = System.currentTimeMillis();

		// The DB_MESSAGES property is set at the end of QueryWindow.displayQueryResults()
		// after all/any messages has been traversed
		@SuppressWarnings("unchecked")
		List<JAseMessage> dbMessages = (List<JAseMessage>) doc.getProperty(ParserProperties.DB_MESSAGES);
		if (dbMessages != null)
		{
			for (JAseMessage msg : dbMessages)
			{
				int line = msg.getScriptRow() - 1;
				int col  = msg.getScriptCol(); // this would be the *offset* not on the line but the whole document

				if (line >= 0)
				{
					int offset = -1;
					int length = -1;
					if (col > 0)
					{
						try {
							offset = getLineStartOffset(doc, line) + col;
							length = getLineEndOffset(doc, line) - col;
						} catch (Throwable t) {
							offset = -1;
						}
					}
//System.out.println("parse(RSyntaxDocument): line="+line+", col="+col+", offset="+offset+", length="+length+"... getLineStartOffset="+getLineStartOffset(doc, line)+", getLineEndOffset="+getLineEndOffset(doc, line));
	
					DefaultParserNotice pn = new DefaultParserNotice(this, msg.getFullMsgTextHtml(), line, offset, length);
					_result.addNotice(pn);
	
					try
					{
						GutterIconInfo gci = _scroll.getGutter().addLineTrackingIcon(line, ICON_ERROR, msg.getFullMsgTextHtml());
						_addedErrorIcons.add(gci);
					}
					catch (BadLocationException ignore) { /* ignore */ }
				}
			}
		}
		
		long time = System.currentTimeMillis() - start;
		_result.setParseTime(time);

		return _result;
	}

	private int getLineStartOffset(RSyntaxDocument doc, int line) 
	{
		Element map = doc.getDefaultRootElement();
		Element lineElem = map.getElement(line);
		return lineElem.getStartOffset();
	}

	private int getLineEndOffset(RSyntaxDocument doc, int line) 
	{
		Element root = doc.getDefaultRootElement();
		Element elem = root.getElement(line);
		int offs = elem.getStartOffset();
		int len = elem.getEndOffset() - offs;
		if ( line == root.getElementCount() - 1 ) 
			len++;

		return len;
	}
}
	
	

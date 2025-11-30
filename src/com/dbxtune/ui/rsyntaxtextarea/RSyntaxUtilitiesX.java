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
package com.dbxtune.ui.rsyntaxtextarea;

import java.awt.Component;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.invoke.MethodHandles;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Utilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.rsta.ui.GoToDialog;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchEvent.Type;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import com.dbxtune.gui.MainFrame;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;


public class RSyntaxUtilitiesX
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	protected static final String EMPTY_STRING = "";

    private static String _charsAllowedInWords = System.getProperty("RSyntaxTextArea.charsAllowedInWords", "_");

	/**
	 * Get characters that can be part of a word for all <code>RSyntaxTextAreaX</code>
	 * 
	 * @param charsAllowedInWords String of characters that should be considered part of words. For Example "_"
	 */
	public static String getCharsAllowedInWords()
	{
		// TODO Auto-generated method stub
		return _charsAllowedInWords;
	}

	/**
	 * Set characters that can be part of a word for all <code>RSyntaxTextAreaX</code>
	 * <p>
	 * This setting can be overridden by setCharsAllowedInWords on any <code>RSyntaxTextAreaX</code>
	 * 
	 * @param charsAllowedInWords String of characters that should be considered part of words. For Example "_"
	 */
	public static void setCharsAllowedInWords(String charsAllowedInWords)
	{
		_charsAllowedInWords = charsAllowedInWords;
		if(_charsAllowedInWords == null)
			_charsAllowedInWords = "";
	}

	/**
	 * Set characters that can be part of a word for a specific <code>RSyntaxTextArea</code>
	 * <p>
	 * Note: this is a workaround to set the allowed words for any classes that 
	 * extends <code>RSyntaxTextArea</code> instead of <code>RSyntaxTextAreaX</code> for example <code>TextEditorPane</code>
	 * 
	 * @param textArea
	 * @param string
	 */
	public static void setCharsAllowedInWords(RSyntaxTextArea textArea, String string)
	{
		RSyntaxTextAreaX.localInit(textArea);
	}


	/**
	 * 
	 * @param textAreaScroll
	 */
	public static void installRightClickMenuExtentions(final RTextScrollPane textAreaScroll, Component owner)
	{
		RTextArea textArea = textAreaScroll.getTextArea();
		installRightClickMenuExtentions(textArea, textAreaScroll, owner);
	}
	/**
	 * 
	 * @param textArea
	 * @param textAreaScroll
	 */
	public static void installRightClickMenuExtentions(final RTextArea textArea, final RTextScrollPane textAreaScroll, Component owner)
	{
		JPopupMenu menu = textArea.getPopupMenu();
		JMenuItem mi;
		JMenu     m;
		
		final RSyntaxTextArea syntaxTextArea = (textArea instanceof RSyntaxTextArea) ? (RSyntaxTextArea) textArea : null;

		//--------------------------------
		menu.addSeparator();

		//--------------------------------
		// Mark Text on Double Click
		if (syntaxTextArea != null)
		{
			mi = new JCheckBoxMenuItem(new RSyntaxTextAreaEditorKitX.MarkWordOnDoubleClickAction(RSyntaxTextAreaX.markAllWordsOnDoubleClick));
			mi.setText("Mark all Words, on Double Click");

			// Set initial value for Context Menu "Mark all Words, on Double Click"... 
			mi.setSelected(Configuration.getCombinedConfiguration().getBooleanProperty(RSyntaxTextAreaX.PROPKEY_IS_HIGLIGHT_WORD_MODE_ENABLED, true));

//			mi = new JCheckBoxMenuItem("Mark all Words, on Double Click", RSyntaxTextAreaX.isHiglightWordModeEnabled(syntaxTextArea));
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
//			mi.addActionListener(new ActionListener()
//			{
//				@Override
//				public void actionPerformed(ActionEvent e)
//				{
//					RSyntaxTextAreaX.setHiglightWordModeEnabled(syntaxTextArea, ! RSyntaxTextAreaX.isHiglightWordModeEnabled(syntaxTextArea) );
//				}
//			});
			menu.add(mi);
		}

		//--------------------------------
		// Format SQL
		mi = new JMenuItem(new RSyntaxTextAreaEditorKitX.FormatSqlAction(RSyntaxTextAreaX.formatSql));
		mi.setText("Format SQL");
		mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK));
		menu.add(mi);
		
		//--------------------------------
		// Change Case
		m = new JMenu("Change Case");
		menu.add(m);

		//--------------------------------
		// Convert Tabs to Spaces
//		mi = new JMenuItem(new RSyntaxTextAreaEditorKitX.FormatSqlAction(RSyntaxTextAreaX.convertTabsToSpaces));
//		mi.setText("Convert Tabs to Spaces");
//		menu.add(mi);
		
		//--------------------------------
		// Convert Spaces to Tabs
//		mi = new JMenuItem(new RSyntaxTextAreaEditorKitX.FormatSqlAction(RSyntaxTextAreaX.convertSpacesToTabs));
//		mi.setText("Convert Spaces to Tabs");
//		menu.add(mi);
		
		//--------------------------------
		// Offline DDL View
		mi = new JMenuItem();
		mi.setText("Offline DDL Viewer...");
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String dbname     = null;
				String objectname = textArea.getSelectedText();
				
				if (StringUtil.isNullOrBlank(objectname))
				{
					SwingUtils.showErrorMessage(textArea, "Select a string", "You must select a text to lookup...", null);
					return;
				}

				MainFrame.getInstance().action_openDdlViewer(dbname, objectname);
			}
		});
		menu.add(mi);

		//-- Upper
		mi = new JMenuItem(new RSyntaxTextAreaEditorKitX.ToUpperCaseAction(RSyntaxTextAreaX.toUpperCase));
		mi.setText("Upper Case");
		mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
		m.add(mi);
		
		//-- Lower
		mi = new JMenuItem(new RSyntaxTextAreaEditorKitX.ToLowerCaseAction(RSyntaxTextAreaX.toLowerCase));
		mi.setText("Lower Case");
		mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
		m.add(mi);
		
		//--------------------------------
		menu.addSeparator();

		//--------------------------------
		// Word Wrap
		if (textArea != null)
		{
			mi = new JCheckBoxMenuItem("Word Wrap", textArea.getLineWrap());
			mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					textArea.setLineWrap( ! textArea.getLineWrap() );
				}
			});
			menu.add(mi);
		}

		//--------------------------------
		// Line Numbers
		if (textAreaScroll != null)
		{
			mi = new JCheckBoxMenuItem("Line Numbers", textAreaScroll.getLineNumbersEnabled());
			mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					textAreaScroll.setLineNumbersEnabled( ! textAreaScroll.getLineNumbersEnabled() );
				}
			});
			menu.add(mi);
		}

		//--------------------------------
		// Current Line Highlight
		if (textArea != null)
		{
			mi = new JCheckBoxMenuItem("Current Line Highlight", textArea.getHighlightCurrentLine());
			mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					textArea.setHighlightCurrentLine( ! textArea.getHighlightCurrentLine() );
				}
			});
			menu.add(mi);
		}

		//--------------------------------
		// Visible Whitespace
		if (syntaxTextArea != null)
		{
			mi = new JCheckBoxMenuItem("Visible Whitespace (space and tabs)", syntaxTextArea.isWhitespaceVisible());
			mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					syntaxTextArea.setWhitespaceVisible( ! syntaxTextArea.isWhitespaceVisible() );
				}
			});
			menu.add(mi);
		}

		//--------------------------------
		// Visible End Of Line Markers
		if (syntaxTextArea != null)
		{
			mi = new JCheckBoxMenuItem("Visible End Of Line Markers", syntaxTextArea.getEOLMarkersVisible());
			mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					syntaxTextArea.setEOLMarkersVisible( ! syntaxTextArea.getEOLMarkersVisible() );
				}
			});
			menu.add(mi);
		}

		//--------------------------------
		// Separator
		menu.addSeparator();

		
		//--------------------------------
		// Set default Font
		if (syntaxTextArea != null)
		{
			mi = new JMenuItem();
			mi.setText("Set Default Font");
			mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
//					Font font = RSyntaxTextArea.getDefaultFont();
					Font font = RSyntaxTextAreaX.getDefaultFont();
					syntaxTextArea.setFont(font);
				}
			});
			menu.add(mi);
		}

		//--------------------------------
		// GOTO LINE...
		menu.add(new JMenuItem(new GoToLineAction(textArea, owner)));

		//--------------------------------
		// Find/Replace...
		menu.add(new JMenuItem(new ReplaceDialogAction(textArea, owner)));
	}

//	/**
//	 * Quick and dirty Find/Replace Action/Dialog, reusing the RSTA dialog.
//	 * But this needs to be replaced be something better.
//	 * Like a mix of Eclipse(layout and "Wrap Search") and TextPad(Mark button)
//	 * Also save/restore the search/replace history 
//	 * 
//	 * @author gorans
//	 */
//	private static class ReplaceDialogAction extends AbstractAction
//	{
//		private static final long serialVersionUID = 1L;
//		private ReplaceDialog _replaceDialog;
//		private RTextArea _textArea;
//		private Component _owner;
//		private JFrame    _frame;
//
//		private static final String NAME = "Find/Replace...";
//
//		public ReplaceDialogAction(RTextArea textArea, Component owner)
//		{
//			super(NAME);
//			_textArea = textArea;
//			_owner = owner; //JOptionPane.getFrameForComponent(_textArea);
//			if (_owner != null && _owner instanceof JFrame)
//				_frame = (JFrame) _owner;
//
//			// Key Mapping
//			int       mask      = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
//			int       key       = KeyEvent.VK_F;
//			KeyStroke keyStroke = KeyStroke.getKeyStroke(key, mask);
//
//			if (_textArea.getActionForKeyStroke(keyStroke) != null)
//			{
//				_logger.warn("Sorry but keyBinding for command '"+NAME+"' using keyStroke '"+keyStroke+"', is already used, ignoring this Key Mapping.");
//			}
//			else
//			{
//				// set it in the popup menu
//				putValue(Action.ACCELERATOR_KEY, keyStroke);
//
//				// But it's not propagated to the TextArea, so this is done extra (don't know if I'm doing something wrong)
//				_textArea.registerKeyboardAction(this, NAME, keyStroke, JComponent.WHEN_FOCUSED);
//				//_textArea.getInputMap().put(keyStroke, NAME); // doesn't work...
//			}
//			_logger.debug("_textArea.getActionForKeyStroke("+keyStroke+"), command("+NAME+"): "+_textArea.getActionForKeyStroke(keyStroke) );
//			
//			_replaceDialog = new ReplaceDialog(_frame, this);
//		}
//
//		@Override
//		public void actionPerformed(ActionEvent e)
//		{
//			String command = e.getActionCommand();
//			_logger.debug("ReplaceDialogAction.actionPerformed(): command = '"+command+"'.");
//
//			// If it's the dialog, open it
//			if ( NAME.equals(command) )
//			{
//				String selectText = _textArea.getSelectedText();
//				if ( selectText != null )
//					_replaceDialog.setSearchString(selectText);
//
//				_replaceDialog.setVisible(true);
//				return;
//			}
//
//			// else it must be any FindDialog Action
//			SearchDialogSearchContext context = _replaceDialog.getSearchContext();
//
//			if ( FindDialog.ACTION_FIND.equals(command) )
//			{
//				if (context.getMarkAll())
//					_textArea.markAll(context.getSearchFor(), context.getMatchCase(), context.getWholeWord(), context.isRegularExpression());
//				else
//					_textArea.clearMarkAllHighlights();
//			}
//
//			if ( ! SearchEngine.find(_textArea, context) )
//			{
//				UIManager.getLookAndFeel().provideErrorFeedback(_textArea);
//			}
//			else if ( ReplaceDialog.ACTION_REPLACE.equals(command) )
//			{
//				if ( ! SearchEngine.replace(_textArea, context) )
//					UIManager.getLookAndFeel().provideErrorFeedback(_textArea);
//			}
//			else if ( ReplaceDialog.ACTION_REPLACE_ALL.equals(command) )
//			{
//				int count = SearchEngine.replaceAll(_textArea, context);
//				JOptionPane.showMessageDialog(null, count + " occurrences replaced.");
//			}
//		}
//
//	}
	/**
	 * Quick and dirty Find/Replace Action/Dialog, reusing the RSTA dialog.
	 * But this needs to be replaced be something better.
	 * Like a mix of Eclipse(layout and "Wrap Search") and TextPad(Mark button)
	 * Also save/restore the search/replace history 
	 * 
	 * @author gorans
	 */
	private static class ReplaceDialogAction extends AbstractAction implements SearchListener
	{
		private static final long serialVersionUID = 1L;
		private ReplaceDialog _replaceDialog;
		private RTextArea _textArea;
		private Component _owner;
		private JFrame    _frame;

		private static final String NAME = "Find/Replace...";

		public ReplaceDialogAction(RTextArea textArea, Component owner)
		{
			super(NAME);
			_textArea = textArea;
			_owner = owner; //JOptionPane.getFrameForComponent(_textArea);
			if (_owner != null && _owner instanceof JFrame)
				_frame = (JFrame) _owner;

			// Key Mapping
			int       mask      = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
			int       key       = KeyEvent.VK_F;
			KeyStroke keyStroke = KeyStroke.getKeyStroke(key, mask);

			if (_textArea.getActionForKeyStroke(keyStroke) != null)
			{
				_logger.warn("Sorry but keyBinding for command '"+NAME+"' using keyStroke '"+keyStroke+"', is already used, ignoring this Key Mapping.");
			}
			else
			{
				// set it in the popup menu
				putValue(Action.ACCELERATOR_KEY, keyStroke);

				// But it's not propagated to the TextArea, so this is done extra (don't know if I'm doing something wrong)
				_textArea.registerKeyboardAction(this, NAME, keyStroke, JComponent.WHEN_FOCUSED);
				//_textArea.getInputMap().put(keyStroke, NAME); // doesn't work...
			}
			_logger.debug("_textArea.getActionForKeyStroke("+keyStroke+"), command("+NAME+"): "+_textArea.getActionForKeyStroke(keyStroke) );
			
			_replaceDialog = new ReplaceDialog(_frame, this);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
//System.out.println("actionPerformed(): e="+e);

			String command = e.getActionCommand();
			_logger.debug("ReplaceDialogAction.actionPerformed(): command = '"+command+"'.");

			// If it's the dialog, open it
			if ( NAME.equals(command) )
			{
				String selectText = _textArea.getSelectedText();
				if ( selectText != null )
					_replaceDialog.setSearchString(selectText);

				_replaceDialog.setVisible(true);
				return;
			}

//			// else it must be any FindDialog Action
//			SearchDialogSearchContext context = _replaceDialog.getSearchContext();
//
//			if ( FindDialog.ACTION_FIND.equals(command) )
//			{
//				if (context.getMarkAll())
//					_textArea.markAll(context.getSearchFor(), context.getMatchCase(), context.getWholeWord(), context.isRegularExpression());
//				else
//					_textArea.clearMarkAllHighlights();
//			}
//
//			if ( ! SearchEngine.find(_textArea, context) )
//			{
//				UIManager.getLookAndFeel().provideErrorFeedback(_textArea);
//			}
//			else if ( ReplaceDialog.ACTION_REPLACE.equals(command) )
//			{
//				if ( ! SearchEngine.replace(_textArea, context) )
//					UIManager.getLookAndFeel().provideErrorFeedback(_textArea);
//			}
//			else if ( ReplaceDialog.ACTION_REPLACE_ALL.equals(command) )
//			{
//				int count = SearchEngine.replaceAll(_textArea, context);
//				JOptionPane.showMessageDialog(null, count + " occurrences replaced.");
//			}
		}

		@Override
		public void searchEvent(SearchEvent e)
		{
			Type type = e.getType();
//System.out.println("searchEvent(): type="+type);
			SearchContext context = _replaceDialog.getSearchContext();

			SearchResult sr = null; 

			if ( type.equals(Type.MARK_ALL) )
			{
				sr = SearchEngine.markAll(_textArea, context);
				if ( sr.getMarkedCount() <= 0 )
					UIManager.getLookAndFeel().provideErrorFeedback(_textArea);
			}
			else if ( type.equals(Type.FIND) )
			{
				sr = SearchEngine.find(_textArea, context);
				if ( ! sr.wasFound() )
					UIManager.getLookAndFeel().provideErrorFeedback(_textArea);
			}
			else if ( type.equals(Type.REPLACE) )
			{
				sr = SearchEngine.replace(_textArea, context);
				if ( ! sr.wasFound() )
					UIManager.getLookAndFeel().provideErrorFeedback(_textArea);
			}
			else if ( type.equals(Type.REPLACE_ALL) )
			{
				sr = SearchEngine.replaceAll(_textArea, context);
				JOptionPane.showMessageDialog(null, sr.getCount() + " occurrences replaced.");
			}
		}

		@Override
		public String getSelectedText()
		{
			String selectText = null;
			
			if (_textArea != null)
				selectText = _textArea.getSelectedText();

			return selectText;
		}

	}

	/**
	 * Quick and dirty GoToLine Action/Dialog, reusing the RSTA dialog.
	 * 
	 * @author gorans
	 */
	private static class GoToLineAction extends AbstractAction
	{
		private static final long serialVersionUID = 1L;
		private RTextArea _textArea;
		private Component _owner;
		private JFrame    _frame;

		private static final String NAME = "Go To Line...";

		public GoToLineAction(RTextArea textArea, Component owner)
		{
			super(NAME);
			_textArea = textArea;
			_owner = owner; //JOptionPane.getFrameForComponent(_textArea);
			if (_owner != null && _owner instanceof JFrame)
				_frame = (JFrame) _owner;

			// Key Mapping
			int       mask      = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
			int       key       = KeyEvent.VK_G;
			KeyStroke keyStroke = KeyStroke.getKeyStroke(key, mask);

			if (_textArea.getActionForKeyStroke(keyStroke) != null)
			{
				_logger.warn("Sorry but keyBinding for command '"+NAME+"' using keyStroke '"+keyStroke+"', is already used, ignoring this Key Mapping.");
			}
			else
			{
				// set it in the popup menu
				putValue(Action.ACCELERATOR_KEY, keyStroke);

				// But it's not propagated to the TextArea, so this is done extra (don't know if I'm doing something wrong)
				_textArea.registerKeyboardAction(this, NAME, keyStroke, JComponent.WHEN_FOCUSED);
				//_textArea.getInputMap().put(keyStroke, NAME); // doesn't work...
			}
			_logger.debug("_textArea.getActionForKeyStroke("+keyStroke+"), command("+NAME+"): "+_textArea.getActionForKeyStroke(keyStroke) );
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			GoToDialog dialog = new GoToDialog(_frame);

			dialog.setMaxLineNumberAllowed(_textArea.getLineCount());
			dialog.setVisible(true);
			int line = dialog.getLineNumber();
			if ( line > 0 )
			{
				try
				{
					_textArea.setCaretPosition(_textArea.getLineStartOffset(line - 1));
				}
				catch (BadLocationException ble)
				{ // Never happens
					UIManager.getLookAndFeel().provideErrorFeedback(_textArea);
					ble.printStackTrace();
				}
			}
		}
	}

	
	/**
	 * Returns the start of the word at the given offset.
	 *
	 * @param textArea The text area.
	 * @param offs The offset into the text area's content.
	 * @return The start offset of the word.
	 * @throws BadLocationException If <code>offs</code> is invalid.
	 * @see #getWordEnd(RSyntaxTextArea, int)
	 */
	public static int getWordStart(RTextArea textArea, int offs)
	throws BadLocationException 
	{
//		String allowChars = textArea.getCharsAllowedInWords();
		String allowChars = getCharsAllowedInWords();
		if (textArea instanceof RSyntaxTextAreaX)
			allowChars = ((RSyntaxTextAreaX)textArea).getCharsAllowedInWords();

		return getWordStart(textArea, offs, allowChars);
	}

	/**
	 * Returns the start of the word at the given offset.
	 *
	 * @param textArea The text area.
	 * @param offs The offset into the text area's content.
	 * @param allowChars Characters that is allowed to be part of any words
	 * @return The start offset of the word.
	 * @throws BadLocationException If <code>offs</code> is invalid.
	 * @see #getWordEnd(RSyntaxTextArea, int)
	 */
	public static int getWordStart(RTextArea textArea, int offs, String allowChars)
	throws BadLocationException 
	{

		Document doc = textArea.getDocument();
		Element line = getLineElem(doc, offs);
		if (line == null) 
			throw new BadLocationException("No word at " + offs, offs);

		int lineStart = line.getStartOffset();
		if (offs==lineStart) // Start of the line.
			return offs;

		int endOffs = Math.min(offs+1, doc.getLength());

		if (allowChars == null)
			allowChars = "";
//System.out.println("X: getWordStart(): allowChars='"+allowChars+"'.");

		String s = doc.getText(lineStart, endOffs-lineStart);
		if(s != null && s.length() > 0) 
		{
			int i = s.length() - 1;
			if (Character.isWhitespace(s.charAt(i))) 
			{
				while (i>0 && Character.isWhitespace(s.charAt(i-1))) 
					i--;
				offs = lineStart + i;
			}
			else if (Character.isLetterOrDigit(s.charAt(i)) || allowChars.indexOf(s.charAt(i)) != -1) 
			{
				while (i>0 && (Character.isLetterOrDigit(s.charAt(i-1)) || allowChars.indexOf(s.charAt(i-1))!= -1))
					i--;
				offs = lineStart + i;
			}
			else // not space or character, lets "skip" same character as current one, so ----- and ===== will be marked
			{
				char c = s.charAt(i);
				while (i>0 && c == s.charAt(i-1))
					i--;
				offs = lineStart + i;
			}
		}

		return offs;
	}

	/**
	 * Returns the end of the word at the given offset.
	 *
	 * @param textArea The text area.
	 * @param offs The offset into the text area's content.
	 * @return The end offset of the word.
	 * @throws BadLocationException If <code>offs</code> is invalid.
	 * @see #getWordStart(RSyntaxTextArea, int)
	 */
	public static int getWordEnd(RTextArea textArea, int offs)
	throws BadLocationException 
	{
//		String allowChars = textArea.getCharsAllowedInWords();
		String allowChars = getCharsAllowedInWords();
		if (textArea instanceof RSyntaxTextAreaX)
			allowChars = ((RSyntaxTextAreaX)textArea).getCharsAllowedInWords();

		return getWordEnd(textArea, offs, allowChars);
	}

	/**
	 * Returns the end of the word at the given offset.
	 *
	 * @param textArea The text area.
	 * @param offs The offset into the text area's content.
	 * @param allowChars Characters that is allowed to be part of any words
	 * @return The end offset of the word.
	 * @throws BadLocationException If <code>offs</code> is invalid.
	 * @see #getWordStart(RSyntaxTextArea, int)
	 */
	public static int getWordEnd(RTextArea textArea, int offs, String allowChars)
	throws BadLocationException 
	{

		Document doc = textArea.getDocument();
//		int endOffs = textArea.getLineEndOffsetOfCurrentLine();
		int endOffs = textArea.getLineEndOffset(textArea.getLineOfOffset(offs));
		int lineEnd = Math.min(endOffs, doc.getLength());
		if (offs == lineEnd)  // End of the line.
			return offs;

//System.out.println("getWordEnd(): lineEnd="+lineEnd+", offs="+offs+" (lineEnd-offs)="+(lineEnd-offs));
		if (allowChars == null)
			allowChars = "";
//System.out.println("X: getWordEnd(): allowChars='"+allowChars+"'.");
		
		String s = doc.getText(offs, lineEnd-offs);
		if (s!=null && s.length()>0) // Should always be true
		{
			int i = 0;
			int count = s.length();
			if (Character.isWhitespace(s.charAt(i))) 
			{
				while (i<count && Character.isWhitespace(s.charAt(i)))
					i++;
			}
			else if (Character.isLetterOrDigit(s.charAt(i)) || allowChars.indexOf(s.charAt(i)) != -1) 
			{
				while (i<count && (Character.isLetterOrDigit(s.charAt(i)) || allowChars.indexOf(s.charAt(i)) != -1))
					i++;
			}
			else // not space or character, lets "skip" same character as current one, so ----- and ===== will be marked
			{
				char c = s.charAt(i);
//System.out.println("getWordEnd() same char, init char='"+c+"'.");
				while (i<count && c == s.charAt(i))
					i++;
			}
			offs += i;
		}

		return offs;
	}

	/**
	 * Returns the start of the word at the given offset.<br>
	 * The word can contain "any" character ! isWhitespace
	 *
	 * @param textArea The text area.
	 * @param offs The offset into the text area's content.
	 * @return The start offset of the word.
	 *
	 * @throws BadLocationException If <code>offs</code> is invalid.
	 * @see #getFullWordEnd(RTextArea, int)
	 */
	public static int getFullWordStart(RTextArea textArea, int offs)
	throws BadLocationException 
	{

		Document doc = textArea.getDocument();
		Element line = getLineElem(doc, offs);
		if (line == null) 
			throw new BadLocationException("No word at " + offs, offs);

		int lineStart = line.getStartOffset();
		if (offs==lineStart) // Start of the line.
			return offs;

		int endOffs = Math.min(offs+1, doc.getLength());

		String s = doc.getText(lineStart, endOffs-lineStart);
		if(s != null && s.length() > 0) 
		{
			int i = s.length() - 1;
			if (Character.isWhitespace(s.charAt(i))) 
			{
				while (i>0 && Character.isWhitespace(s.charAt(i-1))) 
					i--;
				offs = lineStart + i;
			}
			else if ( ! Character.isWhitespace(s.charAt(i)) ) 
			{
				while (i>0 && ! Character.isWhitespace(s.charAt(i-1)) )
					i--;
				offs = lineStart + i;
			}
//			else // not space or character, lets "skip" same character as current one, so ----- and ===== will be marked
//			{
//				char c = s.charAt(i);
//				while (i>0 && c == s.charAt(i-1))
//					i--;
//				offs = lineStart + i;
//			}
		}

		return offs;
	}

	/**
	 * Returns the end of the word at the given offset.<br>
	 * The word can contain "any" character ! isWhitespace
	 *
	 * @param textArea The text area.
	 * @param offs The offset into the text area's content.
	 * @return The end offset of the word.
	 *
	 * @throws BadLocationException If <code>offs</code> is invalid.
	 * @see #getFullWordStart(RTextArea, int)
	 */
	public static int getFullWordEnd(RTextArea textArea, int offs)
	throws BadLocationException 
	{

		Document doc = textArea.getDocument();
//		int endOffs = textArea.getLineEndOffsetOfCurrentLine();
		int endOffs = textArea.getLineEndOffset(textArea.getLineOfOffset(offs));
		int lineEnd = Math.min(endOffs, doc.getLength());
		if (offs == lineEnd)  // End of the line.
			return offs;

		String s = doc.getText(offs, lineEnd-offs);
		if (s!=null && s.length()>0) // Should always be true
		{
			int i = 0;
			int count = s.length();
			if (Character.isWhitespace(s.charAt(i))) 
			{
				while (i<count && Character.isWhitespace(s.charAt(i)))
					i++;
			}
			else if ( ! Character.isWhitespace(s.charAt(i)) ) 
			{
				while (i<count && ! Character.isWhitespace(s.charAt(i)) )
					i++;
			}
//			else // not space or character, lets "skip" same character as current one, so ----- and ===== will be marked
//			{
//				char c = s.charAt(i);
////System.out.println("getWordEnd() same char, init char='"+c+"'.");
//				while (i<count && c == s.charAt(i))
//					i++;
//			}
			offs += i;
		}

		return offs;
	}

	/**
	 * 
	 * @param textArea
	 * @param offs
	 * @return
	 * @throws BadLocationException
	 */
	public static int getNextWord(RTextArea textArea, int offs)
	throws BadLocationException 
	{
		String allowChars = getCharsAllowedInWords();
		if (textArea instanceof RSyntaxTextAreaX)
			allowChars = ((RSyntaxTextAreaX)textArea).getCharsAllowedInWords();

		return getNextWord(textArea, offs, allowChars);
	}
	/**
	 * 
	 * @param textArea
	 * @param offs
	 * @param allowChars
	 * @return
	 * @throws BadLocationException
	 */
	public static int getNextWord(RTextArea textArea, int offs, String allowChars)
	throws BadLocationException 
	{
		if (allowChars == null)
			allowChars = "";

		int endOfWord = getWordEnd(textArea, offs, allowChars);
		int pos = Utilities.getNextWord(textArea, offs);

		// move words until we are after the end of-current-word
		while (pos < endOfWord)
			pos = Utilities.getNextWord(textArea, pos);

		return pos;
	}

	/**
	 * 
	 * @param textArea
	 * @param offs
	 * @return
	 * @throws BadLocationException
	 */
	public static int getPreviousWord(RTextArea textArea, int offs)
	throws BadLocationException 
	{
		String allowChars = getCharsAllowedInWords();
		if (textArea instanceof RSyntaxTextAreaX)
			allowChars = ((RSyntaxTextAreaX)textArea).getCharsAllowedInWords();

		return getPreviousWord(textArea, offs, allowChars);
	}
	/**
	 * 
	 * @param textArea
	 * @param offs
	 * @param allowChars
	 * @return
	 * @throws BadLocationException
	 */
	public static int getPreviousWord(RTextArea textArea, int offs, String allowChars)
	throws BadLocationException 
	{
		if (allowChars == null)
			allowChars = "";

		// move back before getting start of word, otherwise startOfWord will be current word we stand on (or "next" word)
		int pos = Utilities.getPreviousWord(textArea, offs);
		int startOfWord = getWordStart(textArea, pos, allowChars);

		// move words until we are before the start of-current-word
		while (pos > startOfWord)
			pos = Utilities.getPreviousWord(textArea, pos);

		return pos;
	}


	private static final Element getLineElem(Document d, int offs) {
		Element map = d.getDefaultRootElement();
		int index = map.getElementIndex(offs);
		Element elem = map.getElement(index);
		if ((offs>=elem.getStartOffset()) && (offs<elem.getEndOffset())) {
			return elem;
		}
		return null;
	}




	/**
	 * return current word as a String
	 * <p>
	 * Allowed characters are grabbed from 
	 * 
	 * @param textArea   The textarea
	 * @param offs       Offset in the document where to grab the word
	 * @return current word as a String
	 * @throws BadLocationException
	 */
	public static String getCurrentWord(RTextArea textArea, int offs)
	{
		String allowChars = getCharsAllowedInWords();
		if (textArea instanceof RSyntaxTextAreaX)
			allowChars = ((RSyntaxTextAreaX)textArea).getCharsAllowedInWords();

		return getCurrentWord(textArea, offs, allowChars);
	}
	/**
	 * return current word as a String
	 * 
	 * @param textArea   The textarea
	 * @param offs       Offset in the document where to grab the word
	 * @param allowChars Characters allowed within the word (default is just <code>Character.isLetterOrDigit()</code>)
	 * @return current word as a String
	 */
	public static String getCurrentWord(RTextArea textArea, int offs, String allowChars)
	{
		try
		{
			if (allowChars == null)
				allowChars = "";
	
			int start = getWordStart(textArea, offs, allowChars);
			int end   = getWordEnd(textArea, offs, allowChars);
	
			return textArea.getText(start, end-start);
		}
		catch (BadLocationException ble)
		{
			ble.printStackTrace();
			return EMPTY_STRING;
		}
	}

	/**
	 * return current word as a String<br>
	 * The word can contain "any" character ! isWhitespace
	 * 
	 * @param textArea   The textarea
	 * @param offs       Offset in the document where to grab the word
	 * @return current word as a String
	 */
	public static String getCurrentFullWord(RTextArea textArea, int offs)
	{
		try
		{
			int start = getFullWordStart(textArea, offs);
			int end   = getFullWordEnd(textArea, offs);
	
			// hmm... check if this work, or go back and use doc.getText... as in above methods
			return textArea.getText(start, end-start);
		}
		catch (BadLocationException ble)
		{
//			ble.printStackTrace();
			return EMPTY_STRING;
		}
	}

	/**
     * Get Relative word from the current word (-1=previous word, -2=PrevPriv word, 0=currentWord, 1=nextWord... )
	 * 
	 * @param textArea
	 * @param index
	 * @return
	 */
	public static String getRelativeWord(RTextArea textArea, int index)
	{
		String allowChars = getCharsAllowedInWords();
		if (textArea instanceof RSyntaxTextAreaX)
			allowChars = ((RSyntaxTextAreaX)textArea).getCharsAllowedInWords();

		return getRelativeWord(textArea, index, allowChars);
	}

	/**
     * Get Relative word from the current word (-1=previous word, -2=PrevPriv word, 0=currentWord, 1=nextWord... )
	 * 
	 * @param textArea
	 * @param index
	 * @param allowChars
	 * @return
	 */
	public static String getRelativeWord(RTextArea textArea, int index, String allowChars)
	{
		int start = textArea.getCaretPosition();

		// Get current word
		if (index == 0)
			return getCurrentWord(textArea, start, allowChars);

		try
		{
//			int start = textArea.getCaretPosition();
			int end;

			// get words "forward" in the text
			if (index > 0)
			{
				for (int w=0; w<index; w++)
				{
					// position at first char in next word
					start = getNextWord(textArea, start, allowChars);
				}
				end = getWordEnd(textArea, start, allowChars);
			}
			// get words "backwards" in the text
			else 
			{
				int pIndex = Math.abs(index); // convert minus into positive, for simplicity
				
				// if previous char to cursor is not space, add one extra step to previous word, to "rewind" to current word start
				char prevChar= textArea.getText(start-1, 1).charAt(0);
				if ( ! Character.isWhitespace(prevChar) )
					pIndex++;

				for (int w=0; w<pIndex; w++)
				{
					// position at first char in previous word
					start = getPreviousWord(textArea, start, allowChars);
				}
				end = getWordEnd(textArea, start, allowChars);
			}

			int len = end-start;
			String word = EMPTY_STRING;
			if (len > 0)
				word = textArea.getText(start, len);
			return word;
		}
		catch (BadLocationException ble)
		{
//			ble.printStackTrace();
//System.out.println("getRelativeWord(index="+index+"): Caught: "+ble.getMessage());
			return EMPTY_STRING;
		}
	}
}

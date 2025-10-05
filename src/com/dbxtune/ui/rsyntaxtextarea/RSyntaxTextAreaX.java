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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.StyleContext;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rtextarea.FontUtil;
import org.fife.ui.rtextarea.RTextAreaEditorKit;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;
import org.fife.ui.rtextarea.ToolTipSupplier;

import com.dbxtune.gui.focusabletip.FocusableTip;
import com.dbxtune.gui.focusabletip.ToolTipHyperlinkResolver;
import com.dbxtune.gui.swing.ClickListener;
import com.dbxtune.gui.swing.DeferredCaretListener;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKitX.FormatSqlAction;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKitX.MarkWordOnDoubleClickAction;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKitX.NextWordAction;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKitX.PreviousWordAction;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKitX.SelectWordAction;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKitX.ToLowerCaseAction;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKitX.ToUpperCaseAction;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

public class RSyntaxTextAreaX
extends RSyntaxTextArea
{
	private static final long	serialVersionUID	= 1L;

	public static final String  PROPKEY_IS_HIGLIGHT_WORD_MODE_ENABLED = "RSyntaxTextAreaX.isHiglightWordMode.enabled";
	public static final String  PROPKEY_IS_IN_WORD_HIGLIGH_TMODE      = "RSyntaxTextAreaX.isInWordHiglightMode";

	public static final String  PROPKEY_TAB_SIZE                      = "RSyntaxTextAreaX.tabSize";
	public static final int     DEFAULT_TAB_SIZE                      = 4;
	
	public static final String  PROPKEY_TAB_TO_SPACES                 = "RSyntaxTextAreaX.tabToSpaces";
	public static final boolean DEFAULT_TAB_TO_SPACES                 = false;
	
	public static final String  PROPKEY_WINDOWS_FONT_USE_CONSOLAS     = "RSyntaxTextAreaX.windows.default.font.use.Consolas";
	public static final boolean DEFAULT_WINDOWS_FONT_USE_CONSOLAS     = true;

	/**
	 * Constructor.
	 */
	public RSyntaxTextAreaX() 
	{
		super();
		localInit(this);
	}


	/**
	 * Constructor.
	 * 
	 * @param doc The document for the editor.
	 */
	public RSyntaxTextAreaX(RSyntaxDocument doc) 
	{
		super(doc);
		localInit(this);
	}

	/**
	 * Constructor.
	 * 
	 * @param text The initial text to display.
	 */
	public RSyntaxTextAreaX(String text) 
	{
		super(text);
		localInit(this);
	}


	/**
	 * Constructor.
	 * 
	 * @param rows The number of rows to display.
	 * @param cols The number of columns to display.
	 * @throws IllegalArgumentException If either <code>rows</code> or
	 *         <code>cols</code> is negative.
	 */
	public RSyntaxTextAreaX(int rows, int cols) 
	{
		super(rows, cols);
		localInit(this);
	}


	/**
	 * Constructor.
	 * 
	 * @param text The initial text to display.
	 * @param rows The number of rows to display.
	 * @param cols The number of columns to display.
	 * @throws IllegalArgumentException If either <code>rows</code> or
	 *         <code>cols</code> is negative.
	 */
	public RSyntaxTextAreaX(String text, int rows, int cols) 
	{
		super(text, rows, cols);
		localInit(this);
	}


	/**
	 * Constructor.
	 * 
	 * @param doc The document for the editor.
	 * @param text The initial text to display.
	 * @param rows The number of rows to display.
	 * @param cols The number of columns to display.
	 * @throws IllegalArgumentException If either <code>rows</code> or
	 *         <code>cols</code> is negative.
	 */
	public RSyntaxTextAreaX(RSyntaxDocument doc, String text,int rows,int cols) 
	{
		super(doc, text, rows, cols);
		localInit(this);
	}


	/**
	 * Creates a new <code>RSyntaxTextArea</code>.
	 *
	 * @param textMode Either <code>INSERT_MODE</code> or
	 *        <code>OVERWRITE_MODE</code>.
	 */
	public RSyntaxTextAreaX(int textMode) 
	{
		super(textMode);
		localInit(this);
	}

	/**
	 * Returns the default font for text areas.
	 *
	 * @return The default font.
	 */
	public static Font getDefaultFont()
	{
		Font font = FontUtil.getDefaultMonospacedFont();
//System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXX.getDefaultFont(): font=" + font);

		int os = RSyntaxUtilities.getOS();

		if (os == RSyntaxUtilities.OS_MAC_OSX) 
		{
		}
		else if (os == RSyntaxUtilities.OS_WINDOWS) 
		{
			// FROM: org.fife.ui.rtextarea.getDefaultMonospaceFontWindows
			//
			// Cascadia Code was added in later Windows 10/11, default in VS
			// and VS Code. Consolas was added in Vista, used in older VS.
//			Font font = FontUtil.createFont("Cascadia Code", Font.PLAIN, 13);

			if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_WINDOWS_FONT_USE_CONSOLAS, DEFAULT_WINDOWS_FONT_USE_CONSOLAS))
			{
				font = FontUtil.createFont("Consolas", Font.PLAIN, 13);
//System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXX: font=" + font);
				if (font == null)
					font = FontUtil.getDefaultMonospacedFont();
			}
		}
		else if (os == RSyntaxUtilities.OS_LINUX) 
		{
		}

		return font;
	}

	public static final String markAllWordsOnDoubleClick = "mark-all-words-on-double-click";
	public static final String formatSql                 = "format-sql";
//	public static final String convertTabsToSpaces       = "convert-tabs-to-spaces";
//	public static final String convertSpacesToTabs       = "convert-spaces-to-tabs";
	public static final String toUpperCase               = "to-upper-case";
	public static final String toLowerCase               = "to-lower-case";

    /**
	 * initialize this class
	 */
	public static void localInit(final RSyntaxTextArea textArea)
	{
		ActionMap am = textArea.getActionMap();
		am.put(RTextAreaEditorKit.selectWordAction,            new SelectWordAction());
		am.put(RTextAreaEditorKit.nextWordAction,              new NextWordAction(    RTextAreaEditorKit.nextWordAction,          false));
		am.put(RTextAreaEditorKit.selectionNextWordAction,     new NextWordAction(    RTextAreaEditorKit.selectionNextWordAction, true));
		am.put(RTextAreaEditorKit.previousWordAction,          new PreviousWordAction(RTextAreaEditorKit.previousWordAction,          false));
		am.put(RTextAreaEditorKit.selectionPreviousWordAction, new PreviousWordAction(RTextAreaEditorKit.selectionPreviousWordAction, true));

		am.put(RTextAreaEditorKit.rtaIncreaseFontSizeAction,   new RSyntaxTextAreaEditorKit.IncreaseFontSizeAction());
		am.put(RTextAreaEditorKit.rtaDecreaseFontSizeAction,   new RSyntaxTextAreaEditorKit.DecreaseFontSizeAction());

//		am.put(onDoubleClick_markAllOccurrences, new MarkOccurencesAction(onDoubleClick_markAllOccurrences));

		am.put(markAllWordsOnDoubleClick, new MarkWordOnDoubleClickAction(markAllWordsOnDoubleClick));
		am.put(formatSql,                 new FormatSqlAction(formatSql));

//		am.put(convertTabsToSpaces,       new ConvertTabsToSpaces(convertTabsToSpaces));
//		am.put(convertSpacesToTabs,       new ConvertSpacesToTabs(convertSpacesToTabs));
		
		am.put(toUpperCase,               new ToUpperCaseAction(toUpperCase));
		am.put(toLowerCase,               new ToLowerCaseAction(toLowerCase));

		// FIXME: the am.put(), doesn't seems to work... I don't know what the issue is, need to dig into this later
		// Add Ctrl+/ to comment un-comment lines
//		am.put(RSyntaxTextAreaEditorKit.rstaToggleCommentAction, new RSyntaxTextAreaEditorKit.ToggleCommentAction());
		// Key Mapping
		int       mask      = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK;
		int       key       = KeyEvent.VK_7;
//		int       mask      = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
//		int       key       = KeyEvent.VK_SLASH;
		KeyStroke keyStroke = KeyStroke.getKeyStroke(key, mask);

		textArea.registerKeyboardAction(new RSyntaxTextAreaEditorKit.ToggleCommentAction(), RSyntaxTextAreaEditorKit.rstaToggleCommentAction, keyStroke, JComponent.WHEN_FOCUSED);
//		textArea.getInputMap().put(keyStroke, NAME); // doesn't work...

		// Format SQL: Ctrl + Shift + F
		textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK), formatSql);

		// TO LOWER and UPPPER mapping
		textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_U, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), toUpperCase);
		textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), toLowerCase);

		// Increase/Decrease font size with keyboard Ctrl+ and Ctrl-
		textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS,  Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), RTextAreaEditorKit.rtaIncreaseFontSizeAction);
		textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), RTextAreaEditorKit.rtaDecreaseFontSizeAction);

		Configuration conf = Configuration.getCombinedConfiguration();
		
		// I want "tabs", but I want them to be 4 virtual spaces
		int tabSize = conf.getIntProperty(PROPKEY_TAB_SIZE, DEFAULT_TAB_SIZE);
		textArea.setTabSize(tabSize);

		// Insert "blank spaces" instead of a tab
		boolean tabToSpaces = conf.getBooleanProperty(PROPKEY_TAB_TO_SPACES, DEFAULT_TAB_TO_SPACES);
		textArea.setTabsEmulated(tabToSpaces);

//System.out.println("UIManager.getLookAndFeel()="+UIManager.getLookAndFeel());
//com.sun.java.swing.plaf.windows.WindowsLookAndFeel

//System.out.println("XXXXXXX: TextArea.font   = " + UIManager.getFont("TextArea.font"));
//System.out.println("XXXXXXX: TextField.font  = " + UIManager.getFont("TextField.font"));
//System.out.println("XXXXXXX: Label.font      = " + UIManager.getFont("Label.font"));
//System.out.println("XXXXXXX: rstaDefaultFont = " + getDefaultFont());
		
		// DISABLE "smart" insertion of quotes, which just isn't working that great
//		textArea.setInsertPairedCharacters(false);
		// Note: The above didn't work... it introduced "strange" behavior

		// Fix Font
		if (true)
		{
    		Font font = getDefaultFont();
			textArea.setFont( font );

			Component parent = textArea.getParent();
			if (parent instanceof javax.swing.JViewport) 
			{
				parent = parent.getParent();
				if (parent instanceof RTextScrollPane) 
					((RTextScrollPane)parent).getGutter().setLineNumberFont( font );

				if (parent instanceof JScrollPane) 
					parent.repaint();
			}
		}
		
		// Fix font size, use same size as JLabel
		if (SwingUtils.isHiDpi())
		{
//    		Font labelFont = UIManager.getFont("Label.font");
    		Font rstaDefaultFont = getDefaultFont();
    		
//    		int scaledFontSize = labelFont.getSize(); 
//    		int scaledFontSize = SwingUtils.hiDpiScale(13); // 13 is the default size, so lets try to scale that
    		int scaledFontSize = SwingUtils.hiDpiScale(rstaDefaultFont.getSize());
    		
    		Font rstaNewFont = StyleContext.getDefaultStyleContext().getFont(
    				rstaDefaultFont.getFontName(), 
    				rstaDefaultFont.getStyle(), // Font.PLAIN, 
    				scaledFontSize);
    		textArea.setFont( rstaNewFont );
    		
    		// The line numbers in the ScrollBar dosn't adjust...
    		// Try the same hack as in RSyntaxTextAreaEditorKit.IncreaseFontSizeAction()
			Component parent = textArea.getParent();
			if (parent instanceof javax.swing.JViewport) 
			{
				parent = parent.getParent();
				if (parent instanceof JScrollPane) 
					parent.repaint();
			}
		}
		
		// Install a Mouse Wheel Listener, so we can increase decrease the font sizes 
		textArea.addMouseWheelListener(new MouseWheelListener()
		{
			private JScrollPane getParentScrollPane(JComponent component)
			{
				Component parent = component.getParent();
				while (!(parent instanceof JScrollPane) && parent != null)
				{
					parent = parent.getParent();
				}
				if (parent instanceof JScrollPane)
					return (JScrollPane)parent;
				return null;
			}

		    @Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				JComponent component = (JComponent) e.getComponent();
				if ( e.isControlDown() )
				{
					if ( e.getWheelRotation() < 0 )
					{
						//System.out.println("Ctrl-Wheel-UP (IncreaseFontSize)");
						Action action = component.getActionMap().get(RSyntaxTextAreaEditorKit.rtaIncreaseFontSizeAction);
						if ( action != null )
							action.actionPerformed(null);
					}
					else
					{
						//System.out.println("Ctrl-Wheel-DOWN (DecreaseFontSize)");
						Action action = component.getActionMap().get(RSyntaxTextAreaEditorKit.rtaDecreaseFontSizeAction);
						if ( action != null )
							action.actionPerformed(null);
					}
				}
				else // dispatch the event to the parent scroll
				{
					JScrollPane scrollPane = getParentScrollPane(component);
					if (scrollPane != null)
						scrollPane.dispatchEvent(e);
				}
			}
		});

		// Poor-mans version of NotePad++ select a word and we highlight the word everywhere in the doc 
		textArea.addMouseListener(new ClickListener()
		{
			@Override
			public void doubleClick(MouseEvent e)
			{
				if ( ! isHiglightWordModeEnabled(textArea) )
					return;

				String str = textArea.getSelectedText();
				if (StringUtil.hasValue(str) && str.length() > 1)
				{
					SearchContext context = new SearchContext(str.trim());
					context.setMarkAll(true);
					context.setMatchCase(true);
					context.setWholeWord(true);
					SearchEngine.markAll(textArea, context);

					textArea.putClientProperty(PROPKEY_IS_IN_WORD_HIGLIGH_TMODE, true);
				}
			}
			@Override
			public void singleClick(MouseEvent e)
			{
//				if ( ! isHiglightWordModeEnabled(textArea) )
//					return;

				// If we are NOT in highlight mode EXIT
				Object isInWordHiglightMode = textArea.getClientProperty(PROPKEY_IS_IN_WORD_HIGLIGH_TMODE);
				if (isInWordHiglightMode != null && isInWordHiglightMode instanceof Boolean && (Boolean)isInWordHiglightMode == false)
					return;
				
				SearchContext context = new SearchContext();
				context.setMarkAll(true);
				context.setMatchCase(true);
				context.setWholeWord(true);
				SearchEngine.markAll(textArea, context);

				textArea.putClientProperty(PROPKEY_IS_IN_WORD_HIGLIGH_TMODE, false);
			}
		});
		
		// Poor-mans version of NotePad++ select a word and we highlight the word everywhere in the doc 
		textArea.addCaretListener(new DeferredCaretListener()
		{
			@Override
			public void stopMoveWithSelection(CaretEvent e, String selectedText)
			{
				if ( ! isHiglightWordModeEnabled(textArea) )
					return;

				if (StringUtil.isNullOrBlank(selectedText))
					return;
				if (selectedText.length() <= 1)
					return;

				// If we have newline, the it's probably NOT a good idea to mark-the-selected-text
				int nlPos = selectedText.lastIndexOf('\n');
				if (nlPos == -1) // no newlines
				{
    				SearchContext context = new SearchContext(selectedText);
    				context.setMarkAll(true);
    				context.setMatchCase(true);
    				context.setWholeWord(true);
    				SearchResult sr = SearchEngine.markAll(textArea, context);
//System.out.println("caretListener-searchResult: getCount()="+sr.getCount()+", getMarkedCount()="+sr.getMarkedCount());

    				textArea.putClientProperty(PROPKEY_IS_IN_WORD_HIGLIGH_TMODE, true);
				}
			}
		});
		
		// new default: when selecting a variable (or similar) after 350ms, highlight all other variables... (default was 1 second)
		textArea.setMarkOccurrencesDelay(350);
	}
//	private static final Color SELECTION_MARK_COLOR   = new Color(238, 221, 130); // Light Goldenrod

	/** 
	 * String that holds characters allowed in words<br>
	 * If null, grab the String from <code>RSyntaxUtilitiesX.getCharsAllowedInWords()</code>
	 * which holds the string for all <code>RSyntaxTextArea</code> components
	 */
	private static String _charsAllowedInWords = null;

	/**
	 * 
	 * @param rsta
	 * @param noWordSep
	 */
	public void setCharsAllowedInWords(String charsAllowedInWords)
	{
		_charsAllowedInWords = charsAllowedInWords;
	}

	/**
	 * 
	 * @param rsta
	 * @param noWordSep
	 */
	public String getCharsAllowedInWords()
	{
		if (_charsAllowedInWords != null)
			return _charsAllowedInWords;
		
		return RSyntaxUtilitiesX.getCharsAllowedInWords();
	}
	
//	@Override
//	public String getToolTipText(MouseEvent e)
//	{
//		String superToolTip = super.getToolTipText(e);
//		int offset = viewToModel(e.getPoint());
//		System.out.println("getToolTipText(): viewToModel="+offset);
//		
//		return superToolTip;
//	}

	@Override
	public void setToolTipSupplier(ToolTipSupplier supplier)
	{
		super.setToolTipSupplier(supplier);
		if (supplier instanceof ToolTipHyperlinkResolver)
			setToolTipHyperlinkResolver((ToolTipHyperlinkResolver)supplier);
	}
	
	private ToolTipHyperlinkResolver _hyperlinkResolver = null;
	/**
	 * Sets the ToolTipHyperlinkResolver where you can decide what to do when a link is pressed within the ToolTip window
	 * @param hyperlinkResolver
	 */
	public void setToolTipHyperlinkResolver(ToolTipHyperlinkResolver hyperlinkResolver)
	{
		_hyperlinkResolver = hyperlinkResolver;
	}
	/**
	 * Get the installed ToolTipHyperlinkResolver
	 * @return null if not installed, otherwise the Resolver installed
	 */
	public ToolTipHyperlinkResolver getToolTipHyperlinkResolver()
	{
		return _hyperlinkResolver;
	}
	
	private FocusableTip _focusableTip = null;
	/**
	 * Create your own Focusable ToolTip
	 * 
	 * @param textArea The JTextArea
	 * @param listener if you want to replace the HyperlinkListener in the FocusableToolTip window. Note: all the default functionality will be replaced.
	 * 
	 * @return The instance to be used by RSyntaxTextArea
	 */
	public FocusableTip createFocusableTip(JTextArea textArea, HyperlinkListener listener)
	{
		FocusableTip ft = new FocusableTip(textArea, listener, getToolTipHyperlinkResolver());
//System.out.println("###############:createFocusableTip() created: ft="+ft);
		return ft;
	}

	/**
	 * Try to make our own focusableTip, which can handle http links
	 * @param e The mouse event.
	 */
	@Override
	public String getToolTipText(MouseEvent e) 
	{
super.setUseFocusableTips(false);
		String text = super.getToolTipText(e);
//		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXX: getUseFocusableTips()="+getUseFocusableTips()+"super.text="+text);

		// Do we want to use "focusable" tips?
//		if (getUseFocusableTips()) 
		if (true) 
		{
//			System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXX: _focusableTip="+_focusableTip);

			if (text!=null) 
			{
				if (_focusableTip == null) 
					_focusableTip = createFocusableTip(this, null);

//				_focusableTip.setImageBase(imageBase);
				_focusableTip.toolTipRequested(e, text);
			}
			// No tool tip text at new location - hide tip window if one is
			// currently visible
			else if (_focusableTip != null) 
			{
				_focusableTip.possiblyDisposeOfTipWindow();
			}
//			System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXX: after getUseFocusableTips(): returns null");
			return null;
		}

//		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXX: at the end, returns text="+text);
		return text;
	}
	
	public static boolean isHiglightWordModeEnabled(RSyntaxTextArea rsta)
	{
		Object isHiglightWordModeEnabled = rsta.getClientProperty(PROPKEY_IS_HIGLIGHT_WORD_MODE_ENABLED);

		if (isHiglightWordModeEnabled != null && isHiglightWordModeEnabled instanceof Boolean && (Boolean)isHiglightWordModeEnabled == false)
		{
//			System.out.println("isHiglightWordModeEnabled():     <<<<---- return FALSE");
			return false;
		}

		Configuration conf = Configuration.getCombinedConfiguration();
		return conf.getBooleanProperty(PROPKEY_IS_HIGLIGHT_WORD_MODE_ENABLED, true);

	}
	public static void setHiglightWordModeEnabled(RSyntaxTextArea rsta, boolean enable)
	{
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		if (tmpConf != null)
		{
			tmpConf.setProperty(PROPKEY_IS_HIGLIGHT_WORD_MODE_ENABLED, enable);
			tmpConf.save();
		}
		
		rsta.putClientProperty(PROPKEY_IS_HIGLIGHT_WORD_MODE_ENABLED, enable);
	}
}

package com.asetune.ui.rsyntaxtextarea;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import org.fife.ui.rtextarea.RTextAreaEditorKit;

import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKitX.NextWordAction;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKitX.PreviousWordAction;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKitX.SelectWordAction;

public class RSyntaxTextAreaX
extends RSyntaxTextArea
{
	private static final long	serialVersionUID	= 1L;

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
	 * initialize this class
	 */
	public static void localInit(RSyntaxTextArea textArea)
	{
		ActionMap am = textArea.getActionMap();
		am.put(RTextAreaEditorKit.selectWordAction,            new SelectWordAction());
		am.put(RTextAreaEditorKit.nextWordAction,              new NextWordAction(    RTextAreaEditorKit.nextWordAction,          false));
		am.put(RTextAreaEditorKit.selectionNextWordAction,     new NextWordAction(    RTextAreaEditorKit.selectionNextWordAction, true));
		am.put(RTextAreaEditorKit.previousWordAction,          new PreviousWordAction(RTextAreaEditorKit.previousWordAction,          false));
		am.put(RTextAreaEditorKit.selectionPreviousWordAction, new PreviousWordAction(RTextAreaEditorKit.selectionPreviousWordAction, true));

		// FIXME: the am.put(), doesn't seems to work... I don't know what the issue is, need to dig into this later
		// Add Ctrl+/ to comment un-comment lines
//		am.put(RSyntaxTextAreaEditorKit.rstaToggleCommentAction, new RSyntaxTextAreaEditorKit.ToggleCommentAction());
		// Key Mapping
		int       mask      = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_DOWN_MASK;
		int       key       = KeyEvent.VK_7;
//		int       mask      = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
//		int       key       = KeyEvent.VK_SLASH;
		KeyStroke keyStroke = KeyStroke.getKeyStroke(key, mask);

		textArea.registerKeyboardAction(new RSyntaxTextAreaEditorKit.ToggleCommentAction(), RSyntaxTextAreaEditorKit.rstaToggleCommentAction, keyStroke, JComponent.WHEN_FOCUSED);
//		textArea.getInputMap().put(keyStroke, NAME); // doesn't work...
	}

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
}

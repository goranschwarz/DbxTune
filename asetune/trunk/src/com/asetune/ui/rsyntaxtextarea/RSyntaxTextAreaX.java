package com.asetune.ui.rsyntaxtextarea;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.HyperlinkListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import org.fife.ui.rtextarea.RTextAreaEditorKit;

import com.asetune.gui.focusabletip.FocusableTip;
import com.asetune.gui.focusabletip.ToolTipHyperlinkResolver;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKitX.NextWordAction;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKitX.PreviousWordAction;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKitX.SelectWordAction;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKitX.ToLowerCaseAction;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKitX.ToUpperCaseAction;

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

    public static final String toUpperCase = "to-upper-case";
    public static final String toLowerCase = "to-lower-case";

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

		am.put(toUpperCase, new ToUpperCaseAction(toUpperCase));
		am.put(toLowerCase, new ToLowerCaseAction(toLowerCase));

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

		// TO LOWER and UPPPER mapping
		textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_U, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), toUpperCase);
		textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), toLowerCase);
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
System.out.println("###############:createFocusableTip() created: ft="+ft);
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
		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXX: getUseFocusableTips()="+getUseFocusableTips()+"super.text="+text);

		// Do we want to use "focusable" tips?
//		if (getUseFocusableTips()) 
		if (true) 
		{
			System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXX: _focusableTip="+_focusableTip);

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
			System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXX: after getUseFocusableTips(): returns null");
			return null;
		}

		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXX: at the end, returns text="+text);
		return text;
	}
	
}

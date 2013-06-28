package com.asetune.ui.autocomplete;

import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.Util;
import org.fife.ui.rtextarea.RTextArea;

import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.ConnectionProvider;

public abstract class CompletionProviderAbstract
extends DefaultCompletionProvider
{
	/** if the back-end values for completion is needed to be refreshed or not */
	private boolean _needRefresh = true;

	/** GUI Owner, can be null */
	protected Window _guiOwner = null;

	protected ConnectionProvider _connectionProvider = null;

	public CompletionProviderAbstract(Window owner, ConnectionProvider connectionProvider)
	{
		_guiOwner           = owner;
		_connectionProvider = connectionProvider;
	}

//	/** Save completions that is added, since it's reseted if _needRefres */
//	protected List<Completion> _savedComplitionList = new ArrayList<Completion>();
	
	/** Static completions that will be used as a "fallback" */
	protected List<Completion> _staticCompletionList = new ArrayList<Completion>();

	public void addStaticCompletion(Completion c)
	{
		_staticCompletionList.add(c);
//		super.addCompletion(c);
	}
	public List<Completion> getStaticCompletions()
	{
		return _staticCompletionList;
	}

//	@Override
//	public void addCompletion(Completion c)
//	{
//		_savedComplitionList.add(c);
//		super.addCompletion(c);
//	}

	/**
	 * Mark this provider, to do refresh next time it's used
	 */
	public void setNeedRefresh(boolean needRefresh)
	{
		_needRefresh = needRefresh;
	}
	/**
	 * @return true if we need to refresh the back-end completion
	 */
	public boolean needRefresh()
	{
		return _needRefresh;
	}


	/** chars allowed in word completion */
	private String _charsAllowedInWordCompletion = "";

	/**
	 * Get characters that can be part of a words in completion
	 */
	public String getCharsAllowedInWordCompletion()
	{
		// TODO Auto-generated method stub
		return _charsAllowedInWordCompletion;
	}

	/**
	 * Set characters that can be part of a words in completion
	 *
	 * @param charsAllowedInWords String of characters that should be considered part of words. For Example "_"
	 */
	public void setCharsAllowedInWordCompletion(String charsAllowedInWords)
	{
		_charsAllowedInWordCompletion = charsAllowedInWords;
		if(_charsAllowedInWordCompletion == null)
			_charsAllowedInWordCompletion = "";
	}

	
	/**
	 * Returns the text just before the current caret position that could be
	 * the start of something auto-completable.<p>
	 *
	 * This method returns all characters before the caret that are matched
	 * by  {@link #isValidChar(char)}.
	 *
	 * {@inheritDoc}
	 */
	@Override
	public String getAlreadyEnteredText(JTextComponent comp) 
	{
		String allowChars = getCharsAllowedInWordCompletion();

		Document doc = comp.getDocument();

		int dot = comp.getCaretPosition();
		Element root = doc.getDefaultRootElement();
		int index = root.getElementIndex(dot);
		Element elem = root.getElement(index);
		int start = elem.getStartOffset();
		int len = dot-start;
		try {
			doc.getText(start, len, seg);
		} catch (BadLocationException ble) {
			ble.printStackTrace();
			return EMPTY_STRING;
		}

		int segEnd = seg.offset + len;
		start = segEnd - 1;
		while (start>=seg.offset && (isValidChar(seg.array[start]) || allowChars.indexOf(seg.array[start]) != -1)) 
			start--;
		start++;

		len = segEnd - start;
		return len==0 ? EMPTY_STRING : new String(seg.array, start, len);

	}

	/**
	 * Search a completion list for <code>text</code> and returns <code>Completion</code> entries that matches.
	 * 
	 * @param completions
	 * @param text
	 * @return <code>Completion</code> entries that matches <code>text</code>
	 */
	protected List<Completion> getCompletionsFrom(List<Completion> completions, String text)
	{
		List<Completion> retList = new ArrayList<Completion>();

		if ( text == null )
			return retList;

		// search
		for (Completion c : completions)
		{
			if (Util.startsWithIgnoreCase(c.getInputText(), text))
				retList.add(c);
		}

		return retList;
	}


	/**
	 * Get current whole word
	 */
	protected String getCurrentWord(RTextArea textArea)
	{
		String allowChars = getCharsAllowedInWordCompletion();

		return getCurrentWord(textArea, allowChars);
	}
	/**
	 * Get current whole word
	 */
	protected String getCurrentWord(RTextArea textArea, String allowChars)
	{
		int dot = textArea.getCaretPosition();

		return RSyntaxUtilitiesX.getCurrentWord(textArea, dot, allowChars);
	}

	/**
	 * return current word as a String<br>
	 * The word can contain "any" character, meaning except isWhitespace
	 */
	protected String getCurrentFullWord(RTextArea textArea)
	{
		int dot = textArea.getCaretPosition();

		return RSyntaxUtilitiesX.getCurrentFullWord(textArea, dot);
	}

	/**
     * Get Relative word from the current word (-1=previous word, -2=PrevPriv word, 0=currentWord, 1=nextWord... )
     * 
	 * @param textArea
	 * @param index
	 * @return
	 */
	protected String getRelativeWord(RTextArea textArea, int index)
	{
		String allowChars = getCharsAllowedInWordCompletion();

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
	protected String getRelativeWord(RTextArea textArea, int index, String allowChars)
	{
		return RSyntaxUtilitiesX.getRelativeWord(textArea, index, allowChars);
	}

	/**
	 * Returns whether <code>str</code> starts with <code>start</code>, ignoring case.<br>
	 * In case of '*' chars in the <code>start</code> reqexp seach will be used.
	 *
	 * @param str The string to check/search in.
	 * @param start The prefix to check for. (use char '*' for wildcard search )
	 * 
	 * @return Whether <code>str</code> starts with <code>start</code>,
	 *         ignoring case.
	 */
	protected boolean startsWithIgnoreCaseOrRegExp(String str, String start) 
	{
		if (start.indexOf('*') >= 0)
		{
			// (?i) = case-insensitive after this char.
			// and replace '*' with '.*'
			String regexp = "(?i)" + start.replace("*", ".*");
			return str.matches(regexp);
		}
		int startLen = start.length();
		if (str.length()>=startLen) 
		{
			for (int i=0; i<startLen; i++) 
			{
				char c1 = str.charAt(i);
				char c2 = start.charAt(i);
				if (Character.toLowerCase(c1) != Character.toLowerCase(c2)) 
				{
					return false;
				}
			}
			return true;
		}
		return false;
	}
}

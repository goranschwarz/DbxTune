package com.asetune.ui.autocomplete;

import java.awt.Component;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.Util;
import org.fife.ui.rtextarea.RTextArea;

import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;

public abstract class CompletionProviderAbstract
extends DefaultCompletionProvider
{
	public static final String  PROPKEY_CODE_COMP_LOOKUP_STATIC_CMDS_INFO              = "code.completion.lookup.static.cmds.info";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_MISC_INFO                     = "code.completion.lookup.misc.info";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_DATABASE_INFO                 = "code.completion.lookup.database.info";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_TABLE_NAME_INFO               = "code.completion.lookup.table.name.info";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_TABLE_COLUMNS_INFO            = "code.completion.lookup.table.columns.info";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_PROCEDURE_NAME_INFO           = "code.completion.lookup.procedure.name.info";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_PROCEDURE_COLUMNS_INFO        = "code.completion.lookup.procedure.columns.info";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_NAME_INFO    = "code.completion.lookup.system.procedure.name.info";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_COLUMNS_INFO = "code.completion.lookup.system.procedure.columns.info";

	public static final boolean DEFAULT_CODE_COMP_LOOKUP_STATIC_CMDS_INFO              = true;
	public static final boolean DEFAULT_CODE_COMP_LOOKUP_MISC_INFO                     = true;
	public static final boolean DEFAULT_CODE_COMP_LOOKUP_DATABASE_INFO                 = true;
	public static final boolean DEFAULT_CODE_COMP_LOOKUP_TABLE_NAME_INFO               = true;
	public static final boolean DEFAULT_CODE_COMP_LOOKUP_TABLE_COLUMNS_INFO            = true;
	public static final boolean DEFAULT_CODE_COMP_LOOKUP_PROCEDURE_NAME_INFO           = true;
	public static final boolean DEFAULT_CODE_COMP_LOOKUP_PROCEDURE_COLUMNS_INFO        = true;
	public static final boolean DEFAULT_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_NAME_INFO    = true;
	public static final boolean DEFAULT_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_COLUMNS_INFO = true;

	protected static final List<Completion> EMPTY_COMPLETION_LIST = new ArrayList<Completion>();
	
	public boolean isLookupStaticCmds()             { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_STATIC_CMDS_INFO,              DEFAULT_CODE_COMP_LOOKUP_STATIC_CMDS_INFO); }
	public boolean isLookupMisc()                   { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_MISC_INFO,                     DEFAULT_CODE_COMP_LOOKUP_MISC_INFO); }
	public boolean isLookupDb()                     { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_DATABASE_INFO,                 DEFAULT_CODE_COMP_LOOKUP_DATABASE_INFO); }
	public boolean isLookupTableName()              { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_TABLE_NAME_INFO,               DEFAULT_CODE_COMP_LOOKUP_TABLE_NAME_INFO); }
	public boolean isLookupTableColumns()           { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_TABLE_COLUMNS_INFO,            DEFAULT_CODE_COMP_LOOKUP_TABLE_COLUMNS_INFO); }
	public boolean isLookupProcedureName()          { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_PROCEDURE_NAME_INFO,           DEFAULT_CODE_COMP_LOOKUP_PROCEDURE_NAME_INFO); }
	public boolean isLookupProcedureColumns()       { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_PROCEDURE_COLUMNS_INFO,        DEFAULT_CODE_COMP_LOOKUP_PROCEDURE_COLUMNS_INFO); }
	public boolean isLookupSystemProcedureName()    { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_NAME_INFO,    DEFAULT_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_NAME_INFO); }
	public boolean isLookupSystemProcedureColumns() { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_COLUMNS_INFO, DEFAULT_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_COLUMNS_INFO); }

	public void    setLookupStaticCmds            (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_STATIC_CMDS_INFO,              val); tmp.save(); } }
	public void    setLookupMisc                  (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_MISC_INFO,                     val); tmp.save(); } }
	public void    setLookupDb                    (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_DATABASE_INFO,                 val); tmp.save(); } }
	public void    setLookupTableName             (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_TABLE_NAME_INFO,               val); tmp.save(); } }
	public void    setLookupTableColumns          (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_TABLE_COLUMNS_INFO,            val); tmp.save(); } }
	public void    setLookupProcedureName         (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_PROCEDURE_NAME_INFO,           val); tmp.save(); } }
	public void    setLookupProcedureColumns      (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_PROCEDURE_COLUMNS_INFO,        val); tmp.save(); } }
	public void    setLookupSystemProcedureName   (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_NAME_INFO,    val); tmp.save(); } }
	public void    setLookupSystemProcedureColumns(boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_COLUMNS_INFO, val); tmp.save(); } }

	/** if the back-end values for completion is needed to be refreshed or not */
	private boolean _needRefresh = true;

	/** if the back-end values for completion is needed to be refreshed or not */
	private boolean _needRefreshSystemInfo = true;

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
	private List<Completion> _staticCompletionList = new ArrayList<Completion>();

	public void resetStaticCompletion()
	{
		_staticCompletionList = new ArrayList<Completion>();
	}
	public void addStaticCompletion(Completion c)
	{
		if (_staticCompletionList == null)
			_staticCompletionList = new ArrayList<Completion>();
		
		_staticCompletionList.add(c);
//		super.addCompletion(c);
	}
	public List<Completion> getStaticCompletions()
	{
		if (_staticCompletionList == null)
			_staticCompletionList = new ArrayList<Completion>();
		
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

	/**
	 * Mark this provider, to do refresh next time it's used
	 */
	public void setNeedRefreshSystemInfo(boolean needRefresh)
	{
		_needRefreshSystemInfo = needRefresh;
		resetStaticCompletion();
	}
	/**
	 * @return true if we need to refresh the back-end completion
	 */
	public boolean needRefreshSystemInfo()
	{
		return _needRefreshSystemInfo;
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

		boolean useRegExp = text.indexOf('*') >= 0;

		// search
		for (Completion c : completions)
		{
			if (useRegExp)
			{
				if (startsWithIgnoreCaseOrRegExp(c.getInputText(), text))
					retList.add(c);
			}
			else
			{
				if (Util.startsWithIgnoreCase(c.getInputText(), text))
					retList.add(c);
			}
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

			// Add "any" wild card search at the end.
			if  ( ! regexp.endsWith(".*") )
				regexp += ".*";
					
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
	
	/**
	 * Get tooltip text for a specific object<br>
	 * This is most possibly called from any ToolTipSupplier if the word can't be 
	 * resolved within that dictionary...<p>
	 * 
	 * This method should be overridden by any implementors for specific functionality
	 *  
	 * @param word The word we currently has the mouse over
	 * @param fullWord 
	 * @return null if not tip, otherwise hopefully a HTML string which describes a table/procedure or any object... 
	 */
	public String getToolTipTextForObject(String word, String fullWord)
	{
		return null;
	}

	/**
	 * Override this method to create a Completion Provider that delivers static content
	 * @return
	 */
	public DefaultCompletionProvider createTemplateProvider()
	{
		return new DefaultCompletionProvider();
	}

	/**
	 * 
	 * @return
	 */
	abstract public ListCellRenderer createDefaultCompletionCellRenderer();
}

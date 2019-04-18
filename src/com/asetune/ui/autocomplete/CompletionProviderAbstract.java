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
package com.asetune.ui.autocomplete;

import java.awt.Component;
import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;

import org.apache.log4j.Logger;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.Util;
import org.fife.ui.rtextarea.RTextArea;

import com.asetune.AppDir;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.ui.autocomplete.completions.AbstractCompletionX;
import com.asetune.ui.autocomplete.completions.CompletionTemplate;
import com.asetune.ui.autocomplete.completions.SavedCacheCompletionWarning;
import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;

public abstract class CompletionProviderAbstract
extends DefaultCompletionProvider
{
	private static Logger _logger = Logger.getLogger(CompletionProviderAbstract.class);

	public static final String  PROPKEY_CODE_COMP_LOOKUP_STATIC_CMDS_INFO              = "code.completion.lookup.static.cmds.info";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_MISC_INFO                     = "code.completion.lookup.misc.info";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_DATABASE_INFO                 = "code.completion.lookup.database.info";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_TABLE_NAME_INFO               = "code.completion.lookup.table.name.info";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_TABLE_COLUMNS_INFO            = "code.completion.lookup.table.columns.info";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_FUNCTION_NAME_INFO            = "code.completion.lookup.function.name.info";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_FUNCTION_COLUMNS_INFO         = "code.completion.lookup.function.columns.info";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_PROCEDURE_NAME_INFO           = "code.completion.lookup.procedure.name.info";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_PROCEDURE_COLUMNS_INFO        = "code.completion.lookup.procedure.columns.info";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_NAME_INFO    = "code.completion.lookup.system.procedure.name.info";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_COLUMNS_INFO = "code.completion.lookup.system.procedure.columns.info";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_TABLE_TYPES                   = "code.completion.lookup.table.tableTypes.{PRODUCTNAME}";
	public static final String  PROPKEY_CODE_COMP_LOOKUP_SCHEMA_WITH_NO_TABLES         = "code.completion.lookup.schema.withNoTables";
	public static final String  PROPKEY_CODE_COMP_saveCache                            = "code.completion.lookup.cache.save";
	public static final String  PROPKEY_CODE_COMP_saveCacheTimeInMs                    = "code.completion.lookup.cache.save.timeInMs";
	public static final String  PROPKEY_CODE_COMP_saveCacheQuestion                    = "code.completion.lookup.cache.save.popupQuestion";

	public static final boolean DEFAULT_CODE_COMP_LOOKUP_STATIC_CMDS_INFO              = true;
	public static final boolean DEFAULT_CODE_COMP_LOOKUP_MISC_INFO                     = true;
	public static final boolean DEFAULT_CODE_COMP_LOOKUP_DATABASE_INFO                 = true;
	public static final boolean DEFAULT_CODE_COMP_LOOKUP_TABLE_NAME_INFO               = true;
	public static final boolean DEFAULT_CODE_COMP_LOOKUP_TABLE_COLUMNS_INFO            = false;
	public static final boolean DEFAULT_CODE_COMP_LOOKUP_FUNCTION_NAME_INFO            = true;
	public static final boolean DEFAULT_CODE_COMP_LOOKUP_FUNCTION_COLUMNS_INFO         = false;
	public static final boolean DEFAULT_CODE_COMP_LOOKUP_PROCEDURE_NAME_INFO           = true;
	public static final boolean DEFAULT_CODE_COMP_LOOKUP_PROCEDURE_COLUMNS_INFO        = false;
	public static final boolean DEFAULT_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_NAME_INFO    = true;
	public static final boolean DEFAULT_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_COLUMNS_INFO = true;
	public static final boolean DEFAULT_CODE_COMP_LOOKUP_SCHEMA_WITH_NO_TABLES         = true;
	public static final boolean DEFAULT_CODE_COMP_saveCache                            = false;
	public static final int     DEFAULT_CODE_COMP_saveCacheTimeInMs                    = 7000;
	public static final boolean DEFAULT_CODE_COMP_saveCacheQuestion                    = true;

	public static final String  TEMPLATE_CODE_COMP_saveCacheFileName                   = "CompletionProviderCache.{INSTANCE}.jso";

	protected boolean _wilcardMatch                 = true;

	public void    setWildcatdMath(boolean to) { _wilcardMatch = to; }
	public boolean isWildcatdMath()            { return _wilcardMatch; }

	protected static final List<Completion> EMPTY_COMPLETION_LIST = new ArrayList<Completion>();
	
	public boolean isLookupStaticCmds()             { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_STATIC_CMDS_INFO,              DEFAULT_CODE_COMP_LOOKUP_STATIC_CMDS_INFO); }
	public boolean isLookupMisc()                   { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_MISC_INFO,                     DEFAULT_CODE_COMP_LOOKUP_MISC_INFO); }
	public boolean isLookupDb()                     { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_DATABASE_INFO,                 DEFAULT_CODE_COMP_LOOKUP_DATABASE_INFO); }
	public boolean isLookupTableName()              { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_TABLE_NAME_INFO,               DEFAULT_CODE_COMP_LOOKUP_TABLE_NAME_INFO); }
	public boolean isLookupTableColumns()           { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_TABLE_COLUMNS_INFO,            DEFAULT_CODE_COMP_LOOKUP_TABLE_COLUMNS_INFO); }
	public boolean isLookupFunctionName()           { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_FUNCTION_NAME_INFO,            DEFAULT_CODE_COMP_LOOKUP_FUNCTION_NAME_INFO); }
	public boolean isLookupFunctionColumns()        { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_FUNCTION_COLUMNS_INFO,         DEFAULT_CODE_COMP_LOOKUP_FUNCTION_COLUMNS_INFO); }
	public boolean isLookupProcedureName()          { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_PROCEDURE_NAME_INFO,           DEFAULT_CODE_COMP_LOOKUP_PROCEDURE_NAME_INFO); }
	public boolean isLookupProcedureColumns()       { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_PROCEDURE_COLUMNS_INFO,        DEFAULT_CODE_COMP_LOOKUP_PROCEDURE_COLUMNS_INFO); }
	public boolean isLookupSystemProcedureName()    { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_NAME_INFO,    DEFAULT_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_NAME_INFO); }
	public boolean isLookupSystemProcedureColumns() { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_COLUMNS_INFO, DEFAULT_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_COLUMNS_INFO); }
	public boolean isLookupSchemaWithNoTables()     { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_LOOKUP_SCHEMA_WITH_NO_TABLES,         DEFAULT_CODE_COMP_LOOKUP_SCHEMA_WITH_NO_TABLES); }
	public boolean isSaveCacheEnabled()             { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_saveCache,                            DEFAULT_CODE_COMP_saveCache); }
	public int     getSaveCacheTimeInMs()           { return Configuration.getCombinedConfiguration().getIntProperty    (PROPKEY_CODE_COMP_saveCacheTimeInMs,                    DEFAULT_CODE_COMP_saveCacheTimeInMs); }
	public boolean isSaveCacheQuestionEnabled()     { return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CODE_COMP_saveCacheQuestion,                    DEFAULT_CODE_COMP_saveCacheQuestion); }

	public void    setLookupStaticCmds            (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_STATIC_CMDS_INFO,              val); tmp.save(); } }
	public void    setLookupMisc                  (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_MISC_INFO,                     val); tmp.save(); } }
	public void    setLookupDb                    (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_DATABASE_INFO,                 val); tmp.save(); } }
	public void    setLookupTableName             (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_TABLE_NAME_INFO,               val); tmp.save(); } }
	public void    setLookupTableColumns          (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_TABLE_COLUMNS_INFO,            val); tmp.save(); } }
	public void    setLookupFunctionName          (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_FUNCTION_NAME_INFO,            val); tmp.save(); } }
	public void    setLookupFunctionColumns       (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_FUNCTION_COLUMNS_INFO,         val); tmp.save(); } }
	public void    setLookupProcedureName         (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_PROCEDURE_NAME_INFO,           val); tmp.save(); } }
	public void    setLookupProcedureColumns      (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_PROCEDURE_COLUMNS_INFO,        val); tmp.save(); } }
	public void    setLookupSystemProcedureName   (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_NAME_INFO,    val); tmp.save(); } }
	public void    setLookupSystemProcedureColumns(boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_SYSTEM_PROCEDURE_COLUMNS_INFO, val); tmp.save(); } }
	public void    setLookupSchemaWithNoTables    (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_LOOKUP_SCHEMA_WITH_NO_TABLES,         val); tmp.save(); } }
	public void    setSaveCacheEnabled            (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_saveCache,                            val); tmp.save(); } }
	public void    setSaveCacheTimeInMs           (int     val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_saveCacheTimeInMs,                    val); tmp.save(); } }
	public void    setSaveCacheQuestionEnabled    (boolean val) { Configuration tmp = Configuration.getInstance(Configuration.USER_TEMP); if (tmp != null) { tmp.setProperty(PROPKEY_CODE_COMP_saveCacheQuestion,                    val); tmp.save(); } }

	/** if the back-end values for completion is needed to be refreshed or not */
	private boolean _needRefresh = true;

	/** if the back-end values for completion is needed to be refreshed or not */
	private boolean _needRefreshSystemInfo = true;

	/** GUI Owner, can be null */
	protected Window _guiOwner = null;

	protected ConnectionProvider _connectionProvider = null;

	protected List<CompletionTemplate> _completionTemplates = null;

	private boolean       _createLocalConn  = false;  // Set to true if we should create local connection or reuse sqlw conn: true = _connProvider.getNewConnection(), false = _connProvider.getConnection()
	public void    setCreateLocalConnection(boolean b) { _createLocalConn = b; } 
	public boolean isCreateLocalConnection()           { return _createLocalConn; } 
	
	public CompletionProviderAbstract(Window owner, ConnectionProvider connectionProvider)
	{
		_guiOwner           = owner;
		_connectionProvider = connectionProvider;

		// Create Completion templates
		setCompletionTemplates(createCompletionTemplates());
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

	protected void refreshCompletionForStaticCmds()
	{
		resetStaticCompletion();

		for (CompletionTemplate ct : getCompletionTemplates())
			this.addCompletion(ct.createCompletion(this));
	}


	public ConnectionProvider getConnectionProvider()
	{
		return _connectionProvider;
	}

//	@Override
//	public void addCompletion(Completion c)
//	{
//		_savedComplitionList.add(c);
//		super.addCompletion(c);
//	}

	private String _codeCompletionCacheSavedFile = null;
	
	public String getSavedCacheFileName()
	{
		return _codeCompletionCacheSavedFile;
	}

	public void setSavedCacheInstanceName(String instanceName)
	{
		if (instanceName == null)
		{
			_codeCompletionCacheSavedFile = null;
			return;
		}

		// Keep only A-Z, a-z so no strange chars will be part of the filename
		instanceName = instanceName.replaceAll("[^A-Za-z0-9_.-]", "");
		
		String filename = AppDir.getAppStoreDir() + File.separator + TEMPLATE_CODE_COMP_saveCacheFileName.replace("{INSTANCE}", instanceName);
		_codeCompletionCacheSavedFile = filename;
	}

	/**
	 * Clear the saved/serialized cache if any exists
	 */
	public void clearSavedCache()
	{
		if (getSavedCacheFileName() == null)
			return;

		File f = new File(getSavedCacheFileName());
		if (f.exists())
		{
			f.delete();
			_logger.info("Removing saved completion cache. filename '"+getSavedCacheFileName()+"'.");
		}
		setSavedCacheInstanceName(null);
	}

	public void saveCacheToFile(List<? extends Completion> list, String instanceName)
	{
		setSavedCacheInstanceName(instanceName);
		
		if (list == null)
			return;

		_logger.info("Saving "+list.size()+" entries in completion cache using filename '"+getSavedCacheFileName()+"'.");

		try
		{
			FileOutputStream fos = new FileOutputStream(getSavedCacheFileName());
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(list);
			oos.close();
			fos.close();
		}
		catch (IOException e)
		{
			_logger.warn("Problems saving completion cache to the file '"+getSavedCacheFileName()+"'.", e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends AbstractCompletionX> List<T> getSavedCacheFromFile(String instanceName)	
	{
		setSavedCacheInstanceName(instanceName);

		if (getSavedCacheFileName() == null)
			return null;

		final File f = new File(getSavedCacheFileName());
		if ( ! f.exists() )
			return null;

		// Create a Waitfor Dialog and Executor, then execute it.
		WaitForExecDialog wait = new WaitForExecDialog(_guiOwner, "Restoring saved Completions");

		WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
		{

			@Override
			public Object doWork()
			{
				ArrayList<T> list = new ArrayList<T>();
				try
				{
					getWaitDialog().setState("Restoring from file: "+getSavedCacheFileName());

					FileInputStream fis = new FileInputStream(getSavedCacheFileName());
					ObjectInputStream ois = new ObjectInputStream(fis);
					list = (ArrayList<T>) ois.readObject();
					ois.close();
					fis.close();
		
					// Loop and set the provider after it has been restored from File
					for (T compl : list)
						compl.setProvider(CompletionProviderAbstract.this);
					
					loadSavedCacheFromFilePostAction(list, getWaitDialog());
		
					// Add a "WARNING" entry that the list is restored from file (can be old)
					T warningEntry = (T) new SavedCacheCompletionWarning(CompletionProviderAbstract.this, f, list.size());
					warningEntry.setRelevance(Integer.MAX_VALUE);
					list.add(warningEntry);
					
					return list;
				}
				catch (Exception e)
				{
					_logger.warn("Problems restoring completion cache from the file '"+getSavedCacheFileName()+"'.", e);
					return null;
				}
			}
			
		};

//		ArrayList<T> list = new ArrayList<T>();
//		try
//		{
//			FileInputStream fis = new FileInputStream(getSavedCacheFileName());
//			ObjectInputStream ois = new ObjectInputStream(fis);
//			list = (ArrayList<T>) ois.readObject();
//			ois.close();
//			fis.close();
//
//			// Loop and set the provider after it has been restored from File
//			for (T compl : list)
//				compl.setProvider(this);
//			
//			loadSavedCacheFromFilePostAction(list);
//
//			// Add a "WARNING" entry that the list is restored from file (can be old)
//			T warningEntry = (T) new SavedCacheCompletionWarning(this, f, list.size());
//			warningEntry.setRelevance(Integer.MAX_VALUE);
//			list.add(warningEntry);
//		}
//		catch (Exception e)
//		{
//			_logger.warn("Problems restoring completion cache from the file '"+getSavedCacheFileName()+"'.", e);
//			return null;
//		}

		ArrayList<T> list = (ArrayList) wait.execAndWait(doWork);

		if (list == null)
		{
			if (doWork.hasException())
				_logger.error("Problems when restoring Code Completion. Caught:"+doWork.getException(), doWork.getException());
		}
		else
			_logger.info("Restored "+list.size()+" entries into the completion cache from filename '"+getSavedCacheFileName()+"'.");

		return list;
	}
	
	/**
	 * Override this if the implementor needs to do something with the list after it has been loaded
	 * @param list the loaded completions
	 * @param waitForExecDialog 
	 */
	public void loadSavedCacheFromFilePostAction(List<? extends AbstractCompletionX> list, WaitForExecDialog waitForExecDialog)
	{
	}

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
	 * Refresh the Code completion
	 */
	abstract public void refresh();

	
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
			String inputText = c.getInputText();
			if (inputText == null)
				continue;
			
			if (useRegExp)
			{
				if (startsWithIgnoreCaseOrRegExp(inputText, text))
					retList.add(c);
			}
			else
			{
				if (Util.startsWithIgnoreCase(inputText, text))
					retList.add(c);
			}
		}

		return retList;
	}


	/**
	 * Get current whole Line as a String
	 */
	protected String getCurrentLineStr(RTextArea textArea)
	{
		try
		{
			int caretOffset = textArea.getCaretPosition();
			int lineNumber  = textArea.getLineOfOffset(caretOffset);
			int startOffset = textArea.getLineStartOffset(lineNumber);
			int endOffset   = textArea.getLineEndOffset(lineNumber);
			
			return textArea.getText(startOffset, endOffset-startOffset);
		}
		catch (BadLocationException e)
		{
		}
		return "";
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
		if (str == null || start == null)
			return false;

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
//	public DefaultCompletionProvider createTemplateProvider()
//	{
//		return new DefaultCompletionProvider();
//	}
	public DefaultCompletionProvider createTemplateProvider()
	{
		DefaultCompletionProvider provider = new DefaultCompletionProvider();
		
		for (CompletionTemplate ct : getCompletionTemplates())
			provider.addCompletion(ct.createCompletion(provider));

		return provider;
	}

	/**
	 * Override this method to create a Completion Template texts
	 * @return
	 */
	public List<CompletionTemplate> createCompletionTemplates()
	{
		return new ArrayList<CompletionTemplate>();
	}

	public void setCompletionTemplates(List<CompletionTemplate> templates)
	{
		_completionTemplates = templates;
	}

	public List<CompletionTemplate> getCompletionTemplates()
	{
		if (_completionTemplates == null)
			return new ArrayList<CompletionTemplate>();

		return _completionTemplates;
	}

	/**
	 * 
	 * @return
	 */
	abstract public ListCellRenderer createDefaultCompletionCellRenderer();

	/**
	 * Open a configuration dialog
	 */
	public void configure()
	{
		CompletionPropertiesDialog.showDialog(_guiOwner, this);
	}

	public TableModel getLookupTableTypesModel()
	{
		String[] cols = {"Include", "TableType"};
		DefaultTableModel tm = new DefaultTableModel(cols,  0);
		return tm;
	}
	
	public void setLookupTableTypes(List<String> tableTypes)
	{
	}

	public String getDbProductName()
	{
		return "";
	}
	/** Clean up specific stuff when the client does a disconnect */
	public void disconnect()
	{
	}
	
	public Window getGuiOwner()
	{
		return _guiOwner;
	}

//	/**
//	 * Install any entries to the Editors Popup Menu
//	 * @param textarea
//	 */
//	public void installEditorPopupMenuExtention(RTextArea textarea)
//	{
//		JMenu addThis = createEditorPopupMenuExtention(textarea);
//		if (addThis == null)
//			return;
//
//		JPopupMenu pm = textarea.getPopupMenu();
//		if (pm == null)
//			return;
//
//		// Mark this item as a 'MenuExtention'
//		JSeparator separator = new JSeparator();
//		separator.putClientProperty("CompletionProviderAbstract.EditorPopupCcMenuExtention.separator", true);
//
//		// Mark this item as a 'MenuExtention'
//		addThis.putClientProperty("CompletionProviderAbstract.EditorPopupCcMenuExtention", true);
//
//		// Loop current menu items and check if we got any extentions installed, if se they will be removed.
//		JSeparator currentMenuSeparator = null;
//		JMenuItem  currentMenuExtention = null;
//		for (int i=0; i<pm.getComponentCount(); i++)
//		{
//			Component c = pm.getComponent(i);
//			if (c instanceof JSeparator)
//			{
//				Object obj = ((JSeparator) c).getClientProperty("CompletionProviderAbstract.EditorPopupCcMenuExtention.separator");
//				if (obj != null)
//					currentMenuSeparator = (JSeparator) c;
//			}
//			if (c instanceof JMenuItem)
//			{
//				Object obj = ((JMenuItem) c).getClientProperty("CompletionProviderAbstract.EditorPopupCcMenuExtention");
//				if (obj != null)
//					currentMenuExtention = (JMenuItem) c;
//			}
//		}
//		// Remove already existing entries
//		if (currentMenuSeparator != null)
//			pm.remove(currentMenuSeparator);
//		if (currentMenuExtention != null)
//			pm.remove(currentMenuExtention);
//
//		// Finally ADD the entry
//		pm.add(separator);
//		pm.add(addThis);
//	}
	/**
	 * Install any entries to the Editors Popup Menu
	 * @param textarea
	 */
	public void installEditorPopupMenuExtention(RTextArea textarea)
	{
		List<JMenu> addThisList = createEditorPopupMenuExtention(textarea);
		if (addThisList == null)
			return;
		if (addThisList.isEmpty())
			return;

		JPopupMenu pm = textarea.getPopupMenu();
		if (pm == null)
			return;

		// Mark this item as a 'MenuExtention'
		JSeparator separator = new JSeparator();
		separator.putClientProperty("CompletionProviderAbstract.EditorPopupCcMenuExtention.separator", true);

		// Mark this item as a 'MenuExtention'
		for (JMenu jMenu : addThisList)
			jMenu.putClientProperty("CompletionProviderAbstract.EditorPopupCcMenuExtention", true);

		// Loop current menu items and check if we got any extentions installed, if se they will be removed.
		JSeparator currentMenuSeparator = null;
		JMenuItem  currentMenuExtention = null;
		for (int i=0; i<pm.getComponentCount(); i++)
		{
			Component c = pm.getComponent(i);
			if (c instanceof JSeparator)
			{
				Object obj = ((JSeparator) c).getClientProperty("CompletionProviderAbstract.EditorPopupCcMenuExtention.separator");
				if (obj != null)
					currentMenuSeparator = (JSeparator) c;
			}
			if (c instanceof JMenuItem)
			{
				Object obj = ((JMenuItem) c).getClientProperty("CompletionProviderAbstract.EditorPopupCcMenuExtention");
				if (obj != null)
					currentMenuExtention = (JMenuItem) c;
			}
		}
		// Remove already existing entries
		if (currentMenuSeparator != null)
			pm.remove(currentMenuSeparator);
		if (currentMenuExtention != null)
			pm.remove(currentMenuExtention);

		// Finally ADD the entry
		pm.add(separator);
		for (JMenu jMenu : addThisList)
			pm.add(jMenu);
	}

	/**
	 * Create any entries that should go into the Editors Right Click popup menu (at the end)
	 * @param textarea
	 * @return
	 */
	public List<JMenu> createEditorPopupMenuExtention(RTextArea textarea)
	{
		return null;
	}
}

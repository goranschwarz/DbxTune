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
package com.dbxtune.ui.autocomplete.completions;

import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;

/*
 * 01/03/2009
 *
 * BasicCompletion.java - A straightforward Completion implementation.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */


/**
 * A straightforward {@link Completion} implementation.  This implementation
 * can be used if you have a relatively short number of static completions
 * with no (or short) summaries.<p>
 *
 * This implementation uses the replacement text as the input text.  It also
 * includes a "short description" field, which (if non-<code>null</code>), is
 * used in the completion choices list. 
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class BasicCompletionX extends AbstractCompletionX {

	private static final long serialVersionUID = 1L;

	private String replacementText;
	private String shortDesc;
	private String summary;


	/**
	 * Constructor.
	 *
	 * @param provider The parent completion provider.
	 * @param replacementText The text to replace.
	 */
	public BasicCompletionX(CompletionProvider provider, String replacementText){
		this(provider, replacementText, null);
	}


	/**
	 * Constructor.
	 *
	 * @param provider The parent completion provider.
	 * @param replacementText The text to replace.
	 * @param shortDesc A short description of the completion.  This will be
	 *        displayed in the completion list.  This may be <code>null</code>.
	 */
	public BasicCompletionX(CompletionProvider provider, String replacementText,
							String shortDesc) {
		this(provider, replacementText, shortDesc, null);
	}


	/**
	 * Constructor.
	 *
	 * @param provider The parent completion provider.
	 * @param replacementText The text to replace.
	 * @param shortDesc A short description of the completion.  This will be
	 *        displayed in the completion list.  This may be <code>null</code>.
	 * @param summary The summary of this completion.  This should be HTML.
	 *        This may be <code>null</code>.
	 */
	public BasicCompletionX(CompletionProvider provider, String replacementText,
							String shortDesc, String summary) {
		super(provider);
		this.replacementText = replacementText;
		this.shortDesc = shortDesc;
		this.summary = summary;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getReplacementText() {
		return replacementText;
	}


	/**
	 * Returns the short description of this completion, usually used in
	 * the completion choices list.
	 *
	 * @return The short description, or <code>null</code> if there is none.
	 * @see #setShortDescription(String)
	 */
	public String getShortDescription() {
		return shortDesc;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSummary() {
		return summary;
	}


	/**
	 * Sets the short description of this completion.
	 *
	 * @param shortDesc The short description of this completion.
	 * @see #getShortDescription()
	 */
	public void setShortDescription(String shortDesc) {
		this.shortDesc = shortDesc;
	}


	/**
	 * Sets the summary for this completion.
	 *
	 * @param summary The summary for this completion.
	 * @see #getSummary()
	 */
	public void setSummary(String summary) {
		this.summary = summary;
	}


	/**
	 * Returns a string representation of this completion.  If the short
	 * description is not <code>null</code>, this method will return:
	 * 
	 * <code>getInputText() + " - " + shortDesc</code>
	 * 
	 * otherwise, it will return <tt>getInputText()</tt>.
	 *
	 * @return A string representation of this completion.
	 */
	@Override
	public String toString() {
		if (shortDesc==null) {
			return getInputText();
		}
		return getInputText() + " - " + shortDesc;
	}


}

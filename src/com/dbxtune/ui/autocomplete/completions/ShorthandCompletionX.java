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

import org.fife.ui.autocomplete.CompletionProvider;

/*
 * 12/22/2008
 *
 * ShorhandCompletion.java - A completion that is shorthand for some other
 * text.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */


/**
 * A completion where the input text is shorthand for (really, just different
 * than) the actual text to be inserted.  For example, the input text
 * "<code>sysout</code>" could be associated with the completion
 * "<code>System.out.println(</code>" in Java.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class ShorthandCompletionX extends BasicCompletionX {

	/**
	 * The text the user can start typing that will match this completion.
	 */
	private String inputText;


	/**
	 * Constructor.
	 *
	 * @param provider The provider that returns this completion.
	 * @param inputText The text the user inputs to get this completion.
	 * @param replacementText The replacement text of the completion.
	 */
	public ShorthandCompletionX(CompletionProvider provider, String inputText,
								String replacementText) {
		super(provider, replacementText);
		this.inputText = inputText;
	}


	/**
	 * Constructor.
	 *
	 * @param provider The provider that returns this completion.
	 * @param inputText The text the user inputs to get this completion.
	 * @param replacementText The replacement text of the completion.
	 * @param shortDesc A short description of the completion.  This will be
	 *        displayed in the completion list.  This may be <code>null</code>.
	 */
	public ShorthandCompletionX(CompletionProvider provider, String inputText,
								String replacementText, String shortDesc) {
		super(provider, replacementText, shortDesc);
		this.inputText = inputText;
	}


	/**
	 * Constructor.
	 *
	 * @param provider The provider that returns this completion.
	 * @param inputText The text the user inputs to get this completion.
	 * @param replacementText The replacement text of the completion.
	 * @param shortDesc A short description of the completion.  This will be
	 *        displayed in the completion list.  This may be <code>null</code>.
	 * @param summary The summary of this completion.  This should be HTML.
	 *        This may be <code>null</code>.
	 */
	public ShorthandCompletionX(CompletionProvider provider, String inputText,
					String replacementText, String shortDesc, String summary) {
		super(provider, replacementText, shortDesc, summary);
		this.inputText = inputText;
	}


	/**
	 * Returns the text the user must start typing to get this completion.
	 *
	 * @return The text the user must start to input.
	 */
	@Override
	public String getInputText() {
		return inputText;
	}


	/**
	 * If a summary has been set, that summary is returned.  Otherwise, the
	 * replacement text is returned.
	 *
	 * @return A description of this completion (the text that will be
	 *         inserted).
	 * @see #getReplacementText()
	 */
	@Override
	public String getSummary() {
		String summary = super.getSummary();
		return summary!=null ? summary : ("<html><body>" + getSummaryBody());
	}


	/**
	 * Returns the "body" of the HTML returned by {@link #getSummary()} when
	 * no summary text has been set.  This is defined to return the replacement
	 * text in a monospaced font.
	 *
	 * @return The summary text's body, if no other summary has been defined.
	 * @see #getReplacementText()
	 */
	protected String getSummaryBody() {
		return "<code>" + getReplacementText();
	}


}

package com.asetune.ui.autocomplete.completions;

import java.io.Serializable;

import org.fife.ui.autocomplete.CompletionProvider;

public class SqlCompletion
extends ShorthandCompletionX
implements Serializable
{
	private static final long serialVersionUID = 1L;

	public SqlCompletion(CompletionProvider provider, String inputText, String replacementText)
	{
		super(provider, inputText, replacementText);
	}
}

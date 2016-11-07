package com.asetune.ui.autocomplete.completions;

import java.io.Serializable;

import org.fife.ui.autocomplete.CompletionProvider;

import com.asetune.utils.StringUtil;

public class SqlCompletion
extends ShorthandCompletionX
implements Serializable
{
	private static final long serialVersionUID = 1L;

	public SqlCompletion(CompletionProvider provider, String inputText, String replacementText)
	{
		super(provider, inputText, replacementText);
	}

	protected String stripMultiLineHtml(String str)
	{
//		return str;
//		System.out.println("stripMultiLineHtml(): >>>> "+str);
//		System.out.println("stripMultiLineHtml(): <<<< "+StringUtil.stripHtml(str));
		return StringUtil.stripHtml(str);
	}
}

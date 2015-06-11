package com.asetune.ui.autocomplete.completions;

import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;

public class CompletionTemplate
{
	private String _inputText       = "";
	private String _replacementText = null;
	private String _description     = null;
	
	public CompletionTemplate(String inputText)
	{
		this(inputText, inputText, null);
	}

	public CompletionTemplate(String inputText, String replacementText)
	{
		this(inputText, replacementText, null);
	}

	public CompletionTemplate(String inputText, String replacementText, String description)
	{
		_inputText       = inputText;
		_replacementText = replacementText;
		_description     = description;
	}
	
	public Completion createCompletion(CompletionProvider provider)
	{
//		return new ShorthandCompletion(provider, _inputText, _replacementText, _description);
		return new ShorthandCompletionHtml(provider, _inputText, _replacementText, _description);
	}


	private static class ShorthandCompletionHtml
	extends ShorthandCompletion
	{

		public ShorthandCompletionHtml(CompletionProvider provider, String inputText, String replacementText, String shortDesc)
		{
			super(provider, inputText, replacementText, shortDesc);
		}
		@Override
		public String toString()
		{
			String shortDesc = getShortDescription();
			if (shortDesc == null) 
			{
				return getInputText();
			}
			return "<html>" + getInputText() + " -- <i><font color=\"green\">" + shortDesc + "</font></i></html>";
		}
		@Override
		public String getSummary()
		{
			if (getShortDescription() == null)
				return null;
			
			String replacementText = super.getReplacementText();
			return "<html><pre>" + replacementText + "</pre></html>";
		}
	}
}

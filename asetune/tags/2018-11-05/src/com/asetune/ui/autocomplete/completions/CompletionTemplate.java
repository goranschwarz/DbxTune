package com.asetune.ui.autocomplete.completions;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;

public class CompletionTemplate
{
	private String _inputText       = "";
	private String _replacementText = null;
	private String _description     = null;
	
	private ImageIcon _icon = null;
	
	public CompletionTemplate(String inputText)
	{
		this(inputText, inputText, null, null);
	}

	public CompletionTemplate(String inputText, String replacementText)
	{
		this(inputText, replacementText, null, null);
	}

	public CompletionTemplate(String inputText, String replacementText, String description)
	{
		this(inputText, replacementText, description, null);
	}

	public CompletionTemplate(String inputText, String replacementText, String description, ImageIcon icon)
	{
		_inputText       = inputText;
		_replacementText = replacementText;
		_description     = description;
		_icon            = icon;
	}
	
	public Completion createCompletion(CompletionProvider provider)
	{
//		return new ShorthandCompletion(provider, _inputText, _replacementText, _description);
		return new ShorthandCompletionHtml(provider, _inputText, _replacementText, _description, _icon);
	}


	public static class ShorthandCompletionHtml
	extends ShorthandCompletion
	{

		public ShorthandCompletionHtml(CompletionProvider provider, String inputText, String replacementText, String shortDesc, Icon icon)
		{
			super(provider, inputText, replacementText, shortDesc);
			this.setIcon(icon);
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
//		@Override
//		public String getSummary()
//		{
//			if (getShortDescription() == null)
//				return null;
//			
//			String replacementText = super.getReplacementText();
//			return "<html><pre>" + replacementText + "</pre></html>";
//		}
		@Override
		public String getSummary()
		{
			String replacementText  = super.getReplacementText();
			String shortDescription = super.getShortDescription();
			
			if (getShortDescription() == null)
				return null;

			return 
				"<html>"
				+ "<b>" + shortDescription + "</b>" 
				+ "<br>" 
				+ "<br>" 
				+ "<br>" 
				+ "<i>The below text will be inserted in the editor:</i>" 
				+ "<hr>" 
				+ "<pre>" 
				+ replacementText 
				+ "</pre>"
				+ "</html>";
		}
	}


//	public boolean hasIcon()
//	{
//		return _icon != null;
//	}
//
//	public Icon getIcon()
//	{
//		return _icon;
//	}
//
//	public void setIcon(ImageIcon icon)
//	{
//		_icon = icon;
//	}
}

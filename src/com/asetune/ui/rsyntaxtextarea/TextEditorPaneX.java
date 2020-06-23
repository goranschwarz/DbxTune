/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.ui.rsyntaxtextarea;

import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.swing.JTextArea;
import javax.swing.event.HyperlinkListener;

import org.fife.ui.rsyntaxtextarea.FileLocation;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.ToolTipSupplier;

import com.asetune.gui.focusabletip.FocusableTip;
import com.asetune.gui.focusabletip.ToolTipHyperlinkResolver;

/**
 * An extension of {@link org.fife.ui.rsyntaxtextarea.TextEditorPane}
 */
public class TextEditorPaneX extends TextEditorPane
{
	private static final long serialVersionUID = 1L;

	public TextEditorPaneX() 
	{ 
		super(); 
		localInit(this);
	}
	public TextEditorPaneX(int textMode) 
	{ 
		super(textMode); 
		localInit(this);
	}
	public TextEditorPaneX(int textMode, boolean wordWrapEnabled) 
	{ 
		super(textMode, wordWrapEnabled); 
		localInit(this);
	}
	public TextEditorPaneX(int textMode, boolean wordWrapEnabled, FileLocation loc) throws IOException 
	{ 
		super(textMode, wordWrapEnabled, loc); 
		localInit(this);
	}
	public TextEditorPaneX(int textMode, boolean wordWrapEnabled, FileLocation loc, String defaultEnc) throws IOException 
	{ 
		super(textMode, wordWrapEnabled, loc, defaultEnc); 
		localInit(this);
	}



    public static final String formatSql   = "format-sql";
    public static final String toUpperCase = "to-upper-case";
    public static final String toLowerCase = "to-lower-case";

    /**
	 * initialize this class
	 */
	public static void localInit(final RSyntaxTextArea textArea)
	{
		RSyntaxTextAreaX.localInit(textArea);
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
	
	@Override
	public void setToolTipSupplier(ToolTipSupplier supplier)
	{
		super.setToolTipSupplier(supplier);
		if (supplier instanceof ToolTipHyperlinkResolver)
			setToolTipHyperlinkResolver((ToolTipHyperlinkResolver)supplier);
	}
	
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

		// Do we want to use "focusable" tips?
//		if (getUseFocusableTips()) 
		if (true) 
		{
			if (text!=null) 
			{
				if (_focusableTip == null) 
					_focusableTip = createFocusableTip(this, null);

				_focusableTip.toolTipRequested(e, text);
			}
			// No tool tip text at new location - hide tip window if one is
			// currently visible
			else if (_focusableTip != null) 
			{
				_focusableTip.possiblyDisposeOfTipWindow();
			}
			return null;
		}

		return text;
	}
}
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
package com.asetune.gui.focusabletip;

import java.net.URL;

import javax.swing.event.HyperlinkEvent;

/**
 * This class is most likely used in conjunction with the ToolTipHyperlinkResolver
 * <p>
 * It's used to controll what the ToolTip window should do when you click on hyperlinks. 
 * <p>
 * You may want to open the URL in a External Browser (if the ToolTipWindow 
 * doesn't support the content, by default the ToolTipWindow only supports HTML 3.2)<br>
 * Or you can construct your own HTML text output in a string...
 * <p>
 * However if you have set the System Property "TipWindow.JEditorPane.replacement" 
 * to a class name that extends JEditorPane, and that class can render HTML Content 
 * better than JEditorPane, you may choose to always display the URL using 
 * Type.OPEN_URL_IN_TOOLTIP_WINDOW
 * 
 * @author Goran Schwarz
 */
public class ResolverReturn
{
	/** various types the ToolTip Window will be able to handle */
	public enum Type {DO_NOT_OPEN, OPEN_URL_IN_TOOLTIP_WINDOW, OPEN_URL_IN_EXTERNAL_BROWSER, HTML_STRING};

	private HyperlinkEvent _originHyperlinkEvent;
	private Type   _type;
	private URL    _url;
	private String _htmlText;
	
	/**
	 * Internal Constructor, which everybody else are using
	 */
	private ResolverReturn(HyperlinkEvent event, Type type, URL url, String htmlText)
	{
		_originHyperlinkEvent = event;
		_type     = type;
		_url      = url;
		_htmlText = htmlText;
	}

	/**
	 * This sets the URL to event.getURL()<br>
	 * and Type = Type.OPEN_URL_IN_TOOLTIP_WINDOW
	 * 
	 * @param event The event from the HyperlinkListener
	 */
	public ResolverReturn(HyperlinkEvent event)
	{
		// call the private Constructor
		this(event, Type.OPEN_URL_IN_TOOLTIP_WINDOW, event.getURL(), null);
	}

	/**
	 * This sets the URL to event.getURL()<br>
	 * The Type you will have to choose
	 * 
	 * @param event The event from the HyperlinkListener
	 * @param type choose you own Type, probably OPEN_URL_IN_EXTERNAL_BROWSER
	 */
	public ResolverReturn(HyperlinkEvent event, Type type)
	{
		// call the private Constructor
		this(event, type, event.getURL(), null);
	}
	
	/**
	 * Use your own fabricated URL object<br>
	 * Type will be OPEN_URL_IN_TOOLTIP_WINDOW
	 * 
	 * @param event The event from the HyperlinkListener
	 * @param url Your own URL
	 */
	public ResolverReturn(HyperlinkEvent event, URL url)
	{
		// call the private Constructor
		this(event, Type.OPEN_URL_IN_TOOLTIP_WINDOW, url, null);
	}
	
	/**
	 * Use your own fabricated URL object<br>
	 * The Type you will have to choose
	 * 
	 * @param event The event from the HyperlinkListener
	 * @param url Your own URL
	 * @param type choose you own Type, probably OPEN_URL_IN_EXTERNAL_BROWSER
	 */
	public ResolverReturn(HyperlinkEvent event, URL url, Type type)
	{
		// call the private Constructor
		this(event, type, url, null);
	}
	
	/**
	 * Do not use a URL instead display the passed htmlText
	 * Type will be HTML_STRING
	 * 
	 * @param event The event from the HyperlinkListener
	 * @param htmlText HTML text to display
	 */
	public ResolverReturn(HyperlinkEvent event, String htmlText)
	{
		// call the private Constructor
		this(event, Type.HTML_STRING, null, htmlText);
	}

	
	
	
	/** @param type Set the Type you want */
	public void setType(Type type)
	{
		_type = type;
	}
	/** @return current Type */
	public Type getType()
	{
		return _type;
	}

	
	
	/** @return true if If the object has HTML Text assigned */
	public boolean hasHtmlText()
	{
		return _htmlText != null;
	}
	/** Set HTML Text */
	public void setHtmlText(String htmlText)
	{
		_htmlText = htmlText;
	}
	/** @return The current HTML text, null if none */
	public String getHtmlText()
	{
		return _htmlText;
	}

	
	/** @return true if a URL has been assigned */
	public boolean hasUrl()
	{
		return _url != null;
	}
	/** @param url Set a specific URL */
	public void setUrl(URL url)
	{
		_url = url;
	}
	/** @return specified URL, null if nothis is set */
	public URL getUrl()
	{
		return _url;
	}

	
	/** @param event Set the Original Event from the HyperlinkListener */
	public void setOriginHyperlinkEvent(HyperlinkEvent event)
	{
		_originHyperlinkEvent = event;
	}
	/** 
	 * @return The Original Event from the HyperlinkListener, hopefully 
	 * this will never be null except if you use setOriginHyperlinkEvent(null) 
	 */
	public HyperlinkEvent getOriginHyperlinkEvent()
	{
		return _originHyperlinkEvent;
	}
}

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

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.event.HyperlinkEvent;

/**
 * This class is most likely used in conjunction with the ToolTipHyperlinkResolver
 * <p>
 * It's used to control what the ToolTip window should do when you click on hyperlink's. 
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
	public enum Type {DO_NOT_OPEN, OPEN_URL_IN_TOOLTIP_WINDOW, OPEN_URL_IN_EXTERNAL_BROWSER, HTML_STRING, SET_PROPERTY_TEMP};

	private HyperlinkEvent _originHyperlinkEvent;
	private Type    _type;
	private URL     _url;
	private String  _strValue;
	private boolean _closeToolTipWindow = false;

	
	//------------------------------------------------------------------
	// Some Factory methods
	//------------------------------------------------------------------
	
//	public static ResolverReturn createDoNotOpen()
//	{
//		new 
//	}

	/**
	 * This sets the URL to event.getURL()<br>
	 * And opens the URL in THIS tool tip window
	 * 
	 * @param event The event from the HyperlinkListener
	 */
	public static ResolverReturn createOpenInCurrentTooltipWindow(HyperlinkEvent event)
	{
		return new ResolverReturn(event, Type.OPEN_URL_IN_TOOLTIP_WINDOW, event.getURL(), null, false);
	}

	/**
	 * Open the current text in THIS tool tip window
	 * @param event   The event from the HyperlinkListener
	 * @param string  The string that you want to show (this can be in HTML Format "&lt;html&gt;&lt;h1&gt;some text&lt;h1&gt;&lt;/html&gt;")
	 * @return
	 */
	public static ResolverReturn createOpenInCurrentTooltipWindow(HyperlinkEvent event, String htmlText)
	{
		return new ResolverReturn(event, Type.HTML_STRING, null, htmlText, false);
	}



	/**
	 * This sets the URL to event.getURL()<br>
	 * And opens the URL in an external Browser (using the registered application)<br>
	 * Note: Current tool tip Window will be closed, when clicked
	 * 
	 * @param event The event from the HyperlinkListener
	 */
	public static ResolverReturn createOpenInExternalBrowser(HyperlinkEvent event)
	{
		return new ResolverReturn(event, Type.OPEN_URL_IN_EXTERNAL_BROWSER, event.getURL(), null, true);
	}

	/**
	 * This opens the URL in an external Browser (using the registered application)
	 * Note: Current tool tip Window will be closed, when clicked
	 * 
	 * @param event The event from the HyperlinkListener
	 * @param url The desired URL you want to open
	 */
	public static ResolverReturn createOpenInExternalBrowser(HyperlinkEvent event, URL url)
	{
		return new ResolverReturn(event, Type.OPEN_URL_IN_EXTERNAL_BROWSER, url, null, true);
	}

	/**
	 * This opens the URL in an external Browser (using the registered application)
	 * Note: Current tool tip Window will be closed, when clicked
	 * 
	 * @param event The event from the HyperlinkListener
	 * @param urlStr The desired URL you want to open (as a String, so it will make it a URL, and potentially throw MalformedURLException)
	 * @return
	 * @throws MalformedURLException
	 */
	public static ResolverReturn createOpenInExternalBrowser(HyperlinkEvent event, String urlStr) 
	throws MalformedURLException
	{
		URL url = new URL(urlStr);
		return new ResolverReturn(event, Type.OPEN_URL_IN_EXTERNAL_BROWSER, url, null, true);
	}



	/**
	 * Set the property specified in the Configuration TEMP settings
	 * Note: Current tool tip Window will be closed, when clicked
	 * 
	 * @param event  The event from the HyperlinkListener
	 * @param key    Name of the property
	 * @param value  Value of the property
	 * @return
	 */
	public static ResolverReturn createSetProperyTemp(HyperlinkEvent event, String key, String value)
	{
		String str = key + "=" + value;
		return new ResolverReturn(event, Type.SET_PROPERTY_TEMP, null, str, true);		
	}

	/**
	 * Set the property specified in the Configuration TEMP settings
	 * Note: Current tool tip Window will be closed, when clicked
	 * 
	 * @param event  The event from the HyperlinkListener
	 * @param str    Set the property in the format: key=value
	 * @return
	 */
	public static ResolverReturn createSetProperyTemp(HyperlinkEvent event, String str)
	{
		return new ResolverReturn(event, Type.SET_PROPERTY_TEMP, null, str, true);		
	}

	/**
	 * Simply return
	 * @return
	 */
	public static ResolverReturn createDoNothing(HyperlinkEvent event)
	{
		return new ResolverReturn(event, Type.DO_NOT_OPEN, null, null, true);
	}



	/**
	 * Internal Constructor, which everybody else are using
	 */
	private ResolverReturn(HyperlinkEvent event, Type type, URL url, String strValue, boolean closeToolTipWindow)
	{
		_originHyperlinkEvent = event;
		_type                 = type;
		_url                  = url;
		_strValue             = strValue;
		_closeToolTipWindow   = closeToolTipWindow;
	}

//	/**
//	 * This sets the URL to event.getURL()<br>
//	 * and Type = Type.OPEN_URL_IN_TOOLTIP_WINDOW
//	 * 
//	 * @param event The event from the HyperlinkListener
//	 */
//	public ResolverReturn(HyperlinkEvent event)
//	{
//		// call the private Constructor
//		this(event, Type.OPEN_URL_IN_TOOLTIP_WINDOW, event.getURL(), null, false);
//	}

//	/**
//	 * This sets the URL to event.getURL()<br>
//	 * The Type you will have to choose
//	 * 
//	 * @param event The event from the HyperlinkListener
//	 * @param type choose you own Type, probably OPEN_URL_IN_EXTERNAL_BROWSER
//	 */
//	public ResolverReturn(HyperlinkEvent event, Type type)
//	{
//		// call the private Constructor
//		this(event, type, event.getURL(), null, false);
//	}
	
//	/**
//	 * Use your own fabricated URL object<br>
//	 * Type will be OPEN_URL_IN_TOOLTIP_WINDOW
//	 * 
//	 * @param event The event from the HyperlinkListener
//	 * @param url Your own URL
//	 */
//	public ResolverReturn(HyperlinkEvent event, URL url)
//	{
//		// call the private Constructor
//		this(event, Type.OPEN_URL_IN_TOOLTIP_WINDOW, url, null, false);
//	}
	
//	/**
//	 * Use your own fabricated URL object<br>
//	 * The Type you will have to choose
//	 * 
//	 * @param event The event from the HyperlinkListener
//	 * @param url Your own URL
//	 * @param type choose you own Type, probably OPEN_URL_IN_EXTERNAL_BROWSER
//	 */
//	public ResolverReturn(HyperlinkEvent event, URL url, Type type)
//	{
//		// call the private Constructor
//		this(event, type, url, null, false);
//	}
	
//	/**
//	 * Do not use a URL instead display the passed htmlText
//	 * Type will be HTML_STRING
//	 * 
//	 * @param event The event from the HyperlinkListener
//	 * @param htmlText HTML text to display
//	 */
//	public ResolverReturn(HyperlinkEvent event, String htmlText)
//	{
//		// call the private Constructor
//		this(event, Type.HTML_STRING, null, htmlText, false);
//	}

	
	
	
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
	public boolean hasStingValue()
	{
		return _strValue != null;
	}
	/** Set HTML Text */
	public void setStringValue(String strVal)
	{
		_strValue = strVal;
	}
	/** @return The current HTML text, null if none */
	public String getStringValue()
	{
		return _strValue;
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

	/**
	 * Should we close the tool tip window or not?
	 * @return
	 */
	public boolean isCloseToolTipWindowEnabled()
	{
		return _closeToolTipWindow;
	}

	/**
	 * Should we close the tool tip window or not?
	 * @return
	 */
	public void setCloseToolTipWindow(boolean toVal)
	{
		_closeToolTipWindow = toVal;
	}
}

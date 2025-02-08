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
package com.dbxtune.central.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.controllers.HtmlStatic.PageSection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

/**
 * Use this class to extend a DbxCentral Page
 * <p>
 * This means that you will get the "top header" etc 
 * <p>
 * NOTE: This looked like a good idea at start... But it wasn't that useful... So it might be scrapped (lets keep it for a while and evaluate)
 */
public abstract class DbxCentralPageTemplate
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private HttpServletRequest _request;
	private HttpServletResponse _response;
//	private Map<String, String[]> _parameterMap;
	private PrintWriter _writer;
	private Map<String, String> _urlParameterMap = new LinkedHashMap<>();

	private Set<UrlParameterDescription> _urlParameterDescriptions;// = new LinkedHashSet<>();
	
	public HttpServletRequest    getRequest()      { return _request; }
	public HttpServletResponse   getResponse()     { return _response; }
//	public Map<String, String[]> getParameterMap() { return _parameterMap; }
	public PrintWriter           getWriter()       { return _writer; }

	/**
	 * Helper class for URL Parameters
	 */
	public static class UrlParameterDescription
	{
		private String   _name;
		private String   _description;
		private Object   _defaultValue;
		private Class<?> _dataType;
		
		public UrlParameterDescription(String name, String description, Object defaultValue, Class<?> dataType)
		{
			_name         = name;
			_description  = description;
			_defaultValue = defaultValue;
			_dataType     = dataType;
		}
		public String   getName()         { return _name;         }
		public String   getDescription()  { return _description;  }
		public Object   getDefaultValue() { return _defaultValue; }
		public Class<?> getDataType()     { return _dataType;     }
		
		public boolean hasDefaultValue() { return _defaultValue != null; }

		@Override
		public int hashCode()
		{
			return Objects.hash(_name);
		}
	
		@Override
		public boolean equals(Object obj)
		{
			if ( this == obj ) return true;
			if ( obj == null ) return false;
			if ( getClass() != obj.getClass() ) return false;

			UrlParameterDescription other = (UrlParameterDescription) obj;
			return Objects.equals(_name, other._name);
		}

		
	}


	
	/**
	 * Get UrlParameterDescription from a set
	 * 
	 * @param name
	 * @param urlParameterDescriptions
	 * @return
	 * @throws RuntimeException if name is NOT FOUND
	 */
	public UrlParameterDescription getUrlParameterDescriptionEntry(String name)
	{
		for (UrlParameterDescription entry : getUrlParameterDescriptions())
		{
			if (name.equals(entry.getName()))
				return entry;
		}
		throw new RuntimeException("UrlParameterDescription for name '" + name + "' does NOT Exist.");
	}
	
	/** Set parameter from in the URL Parameters Map */
	public void setUrlParameter(String parameterName, Object value)
	{
		String valueStr = value == null ? "" : value.toString();

		getUrlParameters().put(parameterName, valueStr);
	}

	/** Get parameter from the URL Parameters Map, if not found null will be returned  */
	public String getUrlParameter(String parameterName)
	{
		return getUrlParameter(parameterName, null);
	}
	/** Get parameter from the URL Parameters Map, if not found the 'defaultValue' will be returned  */
	public String getUrlParameter(String parameterName, String defaultValue)
	{
//		String value = getUrlParameter(parameterName);
		String value = getUrlParameters().get(parameterName);

		if (StringUtil.isNullOrBlank(value))
			return defaultValue;
		
		if (value.equals("undefined"))
			return defaultValue;
		
		return value;
	}

	/** Get parameter from the URL Parameters Map, if not found use the default value from 'urlParameterDescriptions'  */
	public String getUrlParameter_defaultFromDesc(String parameterName)
	{
		UrlParameterDescription urlParameterDescription = getUrlParameterDescriptionEntry(parameterName);
		return getUrlParameter(parameterName, (String) urlParameterDescription.getDefaultValue());
	}



	/** Get parameter from the URL Parameters Map, if not found the 'defaultValue' will be returned  */
	public int getUrlParameterInt(String parameterName, int defaultValue)
	{
		String value = getUrlParameter(parameterName);
		
		if (StringUtil.isNullOrBlank(value))
			return defaultValue;
		
		if (value.equals("undefined"))
			return defaultValue;
		
		return StringUtil.parseInt(value, defaultValue);
	}
	
	/** Get parameter from the URL Parameters Map, if not found the 'defaultValue' will be returned  */
	public double getUrlParameterDouble(String parameterName, double defaultValue)
	{
		String value = getUrlParameter(parameterName);
		
		if (StringUtil.isNullOrBlank(value))
			return defaultValue;
		
		if (value.equals("undefined"))
			return defaultValue;
		
		return StringUtil.parseDouble(value, defaultValue);
	}
	
	/** Get parameter from the URL Parameters Map, if not found use the default value from 'urlParameterDescriptions'  */
	public int getUrlParameterInt_defaultFromDesc(String parameterName)
	{
		UrlParameterDescription urlParameterDescription = getUrlParameterDescriptionEntry(parameterName);
		return getUrlParameterInt(parameterName, (Integer) urlParameterDescription.getDefaultValue());
	}

	/** Get parameter from the URL Parameters Map, if not found the 'defaultValue' will be returned  */
	public boolean getUrlParameterBoolean(String parameterName, boolean defaultValue)
	{
		String value = getUrlParameter(parameterName);
		
		if (StringUtil.isNullOrBlank(value))
			return defaultValue;
		
		if ("undefined".equals(value))
			return defaultValue;
		
		return StringUtil.parseBoolean(value, defaultValue);
	}

	/** Get parameter from the URL Parameters Map, if not found use the default value from 'urlParameterDescriptions'  */
	public boolean getUrlParameterBoolean_defaultFromDesc(String parameterName)
	{
		UrlParameterDescription urlParameterDescription = getUrlParameterDescriptionEntry(parameterName);
		return getUrlParameterBoolean(parameterName, (boolean) urlParameterDescription.getDefaultValue());
	}



	/** Get parameter from the URL Parameters Map, if not found the 'defaultValue' will be returned  */
	public Timestamp getUrlParameterTs(String parameterName, Timestamp defaultValue)
	{
		String value = getUrlParameter(parameterName);
		
		if (StringUtil.isNullOrBlank(value))
			return defaultValue;
		
		if (value.equals("undefined"))
			return defaultValue;
		
		try 
		{
			return TimeUtils.parseToTimestampX(value);
		} 
		catch (ParseException ex) 
		{
			_logger.warn("Problems in getUrlParameterTs(parameterName='" + parameterName + "', defaultValue='" + defaultValue + "'). returning defaultValue. Caoght: " + ex);
			return defaultValue;
		}
	}

	/** Get parameter from the URL Parameters Map, if not found use the default value from 'urlParameterDescriptions'  */
	public Timestamp getUrlParameterTs_defaultFromDesc(String parameterName)
	{
		UrlParameterDescription urlParameterDescription = getUrlParameterDescriptionEntry(parameterName);
		return getUrlParameterTs(parameterName, (Timestamp) urlParameterDescription.getDefaultValue());
	}



	
	/**
	 * Set a new parameter Map&lt;String, String&gt; (if the assigned one is not "good" or "usable")
	 * @return
	 */
	public void setUrlParameters(Map<String, String> parameterMap)
	{
		_urlParameterMap = parameterMap;
	}

	/**
	 * Get a Map&lt;String, String&gt; of all passed parameters
	 * @return
	 */
	public Map<String, String> getUrlParameters()
	{
		return _urlParameterMap != null ? _urlParameterMap : Collections.emptyMap();
	}
	
	/**
	 * Create a list of known parameters, there descriptions etc... 
	 * <p>
	 * Return <code>null</code> to "skip" checking parameters and allow "anything" 
	 */
	public abstract Set<UrlParameterDescription> createUrlParameterDescription();
	
	/** 
	 * Get a list of known parameters, there descriptions etc... created with: createUrlParameterDescription() 
	 */
	public Set<UrlParameterDescription> getUrlParameterDescriptions()
	{
		return _urlParameterDescriptions;
	}

	/** 
	 * Set a list of known parameters, there descriptions etc... 
	 */
	public void setUrlParameterDescription(Set<UrlParameterDescription> urlParameterDescriptions)
	{
		_urlParameterDescriptions = urlParameterDescriptions;
	}

	/**
	 * In this implementation you can reject parameter values...<br>
	 * Or change them into "other data types"<br>
	 * For example
	 * <ul>
	 *     <li>if parameter 'startTime=TODAY', you may want to "reassign" that to a valid "YYYY-MM-DD hh:mm"</li>
	 *     <li>if parameter 'startTime=-2h', you may want to "reassign" that to a valid "YYYY-MM-DD hh:mm"</li>
	 * </ul>
	 * 
	 * @param urlParameters
	 * @throws Exception
	 */
	protected abstract void checkUrlParameters()
	throws Exception;
	
	
	/**
	 * Servlet: doGet(...)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
	{
		_request = request;
		_response = response;

		// Make the output UTF8
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());

		// Get the writer this must be done AFTER setCharacterEncoding
		_writer = response.getWriter();

		// Set the URL Parameters
		setUrlParameters(Helper.getParameterMap(request));
		
		// Create a set of Parameters
		setUrlParameterDescription(createUrlParameterDescription());
		
		// Check for known input parameters
		String[] knownParams = getKnownParameters();
		if (knownParams != null)
		{
			if (Helper.hasUnKnownParameters(request, response, ArrayUtils.addAll(knownParams)))
				return;
		}

		// Get KNOWN Parameters for the User Defined Content producer
//		udc.setUrlParameters(Helper.getParameterMap(req));
		try
		{
			checkUrlParameters();
		}
		catch (Exception ex)
		{
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
			return;
		}

		try
		{
			init(Configuration.getCombinedConfiguration());
		}
		catch (Exception ex)
		{
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
			return;
		}

		try
		{
			open();
			
			createHtml(getWriter());
		}
		catch (SendResponseErrorException ex)
		{
			response.sendError(ex.getResponceCode(), ex.getMessage());
ex.printStackTrace();
			return;
		}
		finally
		{
			close();
		}
		
		getWriter().flush();
		getWriter().close();
	}


	/**
	 * Create the HTML Page
	 * <br>
	 * This basically just calls:
	 * <pre>
	 *     createHtmlHead(writer);
	 *     createHtmlBody(writer);
	 * </pre>
	 * 
	 * @param writer
	 */
	public void createHtml(PrintWriter writer) 
	{
//		writer.append("<html>\n");
		writer.append("<!DOCTYPE html>  \n");
		writer.append("<html lang='en'> \n");

		try
		{
			createHtmlHead(writer);
			createHtmlBody(writer);
		}
		catch (RuntimeException rte)
		{
			writer.append("<b>Problems creating HTML content</b>, Caught RuntimeException: ").append(rte.toString()).append("<br> \n");
			writer.append("<pre>\n");
			writer.append(StringUtil.exceptionToString(rte));
			writer.append("</pre>\n");
			
			_logger.warn("Problems creating HTML content. Caught: "+rte, rte);
		}

		writer.append("</html> \n");
		writer.flush();
	}
	
	/**
	 * Create the HTML Head section<br>
	 * This will add
	 * <ul>
	 *     <li>Check for refresh, and adds 'auto-refresh' every X second. if <code>getHeadRefreshTime()</code> returns other than 0</li>
	 *     <li>calls method <code>createHtmlHeadContent()</code> to generate code in the header, injected at the <b>TOP</b></li>
	 *     <li>add: JavaScript/CSS for: jquery</li>
	 *     <li>add: JavaScript/CSS for: moment</li>
	 *     <li>add: JavaScript/CSS for: bootstrap</li>
	 *     <li>add: JavaScript/CSS for: dbxcentral</li>
	 *     <li>add: 'your own javascripts' by implementing <code>createHtmlHeadJavaScript()</code> and/or <code>getJavaScriptList()</code></li>
	 *     <li>add: 'your own CSS' by implementing <code>craeteHtmlHeadCss()</code> and/or <code>getCssList()</code></li>
	 * </ul>
	 * You can also add your own JavaScript 
	 * @param writer
	 */
	public void createHtmlHead(PrintWriter writer)
	{
		writer.println("<!-- ################### HEAD BEGIN ################### -->");
		writer.println("<head>");

		writer.println("<title>" + getHeadTitle() + "</title> ");
		writer.println("<meta charset='utf-8'> ");
		
		int refreshTime = getHeadRefreshTime();
		if (refreshTime > 0)
			writer.println("<meta http-equiv='refresh' content='" + refreshTime + "' />");

		createHtmlHeadContent(writer);

		writer.println("<!-- JS: JQuery -->");
		writer.println("<script type='text/javascript' src='/scripts/jquery/jquery-3.7.0.min.js'></script>");
		writer.println();
		writer.println("<!-- JS: JQuery UI -->");
		writer.println("<script type='text/javascript' src='/scripts/jquery/ui/1.13.2/jquery-ui.min.js'></script>");
		writer.println();
		writer.println("<!-- JS: Moment; used by: ChartJs, DateRangePicker -->");
		writer.println("<script type='text/javascript' src='/scripts/moment/moment.js'></script>");
		writer.println("<script type='text/javascript' src='/scripts/moment/moment-duration-format.js'></script>");
		writer.println();
		writer.println("<!-- JS: Bootstrap -->");
		writer.println("<script type='text/javascript' src='/scripts/popper/1.12.9/popper.min.js'></script>");
		writer.println("<script type='text/javascript' src='/scripts/bootstrap/js/bootstrap.min.js'></script>");
		writer.println();
		writer.println("<!-- JS: DbxCentral -->");
		writer.println("<script type='text/javascript' src='/scripts/dbxcentral.utils.js'></script>");
		writer.println();

		createHtmlHeadJavaScript(writer);
		for (String scriptLocation : getJavaScriptList())
		{
			writer.println("<script type='text/javascript' src='" + scriptLocation + "'></script>");
		}

		writer.println("<!-- CSS: DbxCentral -->");
		writer.println("<link rel='stylesheet' href='/scripts/css/dbxcentral.css'>");
		writer.println();
		writer.println("<!-- CSS: JQuery UI -->");
		writer.println("<link rel='stylesheet' href='/scripts/jquery/ui/1.13.2/themes/smoothness/jquery-ui.css'>");
		writer.println();
		writer.println("<!-- CSS: Bootstrap -->");
		writer.println("<link rel='stylesheet' href='/scripts/bootstrap/css/bootstrap.min.css'>");
		writer.println();
		writer.println("<!-- CSS: Font Awsome -->");
		writer.println("<link rel='stylesheet' href='/scripts/font-awesome/4.4.0/css/font-awesome.min.css'>");
		writer.println();

		// Internal CSS needed (a better name could probably be good)
		craeteHtmlHeadCssInternal(writer);
		
		craeteHtmlHeadCssPre(writer);
		for (String cssLocation : getCssList())
		{
			writer.println("<link rel='stylesheet' href='" + cssLocation + "'>");
		}
		craeteHtmlHeadCssPost(writer);

		writer.println("</head>");
		writer.println("<!-- ################### HEAD END ################### -->");
	}

	/**
	 * Get auto refresh (return 0 or negative for NO-auto-refresh)
	 * @return
	 */
	public int getHeadRefreshTime() 
	{
		return getUrlParameterInt("refresh", -1);
	}

	/**
	 * Page title in the head
	 * @return
	 */
	public abstract String getHeadTitle();

	/** implement this to add your own JavaScripts in the header */
	public void createHtmlHeadJavaScript(PrintWriter writer)
	{
	}
	/** implement this to add your own CSS in the header... BEFORE calling getCssList() */
	public void craeteHtmlHeadCssPre(PrintWriter writer)
	{
	}
	/** implement this to add your own CSS in the header... AFTER calling getCssList() */
	public void craeteHtmlHeadCssPost(PrintWriter writer)
	{
	}
	/** implement this to add your own Code in the header, at the TOP */
	public void createHtmlHeadContent(PrintWriter writer)
	{
	}
	/** List of <i>extra</i> CSS we depends on */
	protected List<String> getCssList()
	{
		return Collections.emptyList();
	}

	/** List of <i>extra</i> java script we depends on */
	protected List<String> getJavaScriptList()
	{
		return Collections.emptyList();
	}
	
	/** Some extra internal stuff */
	public void craeteHtmlHeadCssInternal(PrintWriter writer)
	{
		writer.println();
		writer.println("<style type='text/css'>");
		writer.println();
		writer.println("    /* The below data-tooltip is used to show Actual exected SQL Text, as a tooltip where a normalized text is in a table cell */ ");
		writer.println("    [data-tooltip] { ");
		writer.println("        position: relative; ");
		writer.println("    } ");
		writer.println();
		writer.println("    /* 'tooltip' CSS settings for SQL Text... */ ");
		writer.println("    [data-tooltip]:hover::before { ");
		writer.println("        content: attr(data-tooltip);		 ");
//		writer.println("        content: 'Click to Open Text Dialog...'; ");
		writer.println("        position: absolute; ");
		writer.println("        z-index: 103; ");
		writer.println("        top: 20px; ");
		writer.println("        left: 30px; ");
		writer.println("        width: 1000px; ");
		writer.println("        height: 900px; ");
//		writer.println("        width: 220px; ");
		writer.println("        padding: 10px; ");
		writer.println("        background: #454545; ");
		writer.println("        color: #fff; ");
		writer.println("        font-size: 11px; ");
		writer.println("        font-family: Courier; ");
		writer.println("        white-space: pre-wrap; ");
		writer.println("    } ");
		writer.println("    [data-title]:hover::after { ");
		writer.println("        content: ''; ");
		writer.println("        position: absolute; ");
		writer.println("        bottom: -12px; ");
		writer.println("        left: 8px; ");
		writer.println("        border: 8px solid transparent; ");
		writer.println("        border-bottom: 8px solid #000; ");
		writer.println("    } ");
		writer.println();
		writer.println("    /* Parameter descriptions */ ");
		writer.println("    table.parameter-description th, ");
		writer.println("    table.parameter-description td { ");
		writer.println("        border: 1px solid black; ");
		writer.println("        border-collapse: collapse; ");
		writer.println("        padding: 5px; ");
		writer.println("    } ");
		writer.println("</style>");
		writer.println();
	}



	/**
	 * Created the HTML <b>body</b> section
	 * <br>
	 * The default will call:
	 * <ul>
	 *     <li><code>createHtmlBodyNavbar()</code>           -- To create a navigation bar</li>
	 *     <li><code>createHtmlBodyJavaScriptTop()</code>    -- Add some JavaScript at the TOP of the code</li>
	 *     <li><code>createHtmlBodyContent()</code>          -- Add <b>your</b> HTML content. <b>This is probably what you want to implement</b></li>
	 *     <li><code>createHtmlBodyJavaScriptBottom()</code> -- Add some JavaScript at the TOP of the code</li>
	 *     <li><code>createHtmlBodyLoginCheck()</code>       -- Code to check if we are logged in</li>
	 * </ul>
	 * @param writer
	 */
	public void createHtmlBody(PrintWriter writer)
	{
		writer.println("<!-- ################### BODY BEGIN ################### -->");
		writer.println("<body " + getBodyAttributes() + ">");

		// Create navbar
		createHtmlBodyNavbar(writer);

		// Any JavaScript at the top
		createHtmlBodyJavaScriptTop(writer);

		// Page content
		createHtmlBodyContent(writer);

		// Any JavaScript at the bottom (after HTML Content)
		createHtmlBodyJavaScriptBottom(writer);

		// Check if we are logged in
		createHtmlBodyLoginCheck(writer);
		//writer.println( HtmlStatic.getJavaScriptAtEnd(true) );
				
		writer.println("</body>");
		writer.println("<!-- ################### BODY END ################### -->");
	}

	/** 
	 * Any Attributes to the &lt;body getBodyAttributes()&gt; tag 
	 */
	public String getBodyAttributes() 
	{
		return ""; 
	}
	
	/** 
	 * What page section is this page for. What section should be <i>selected</i> in the Navigation bar 
	 */
	public abstract PageSection getPageSection();

	public boolean isLoginEnabled()
	{
		return true;
	}

	public String getNavbarHtmlRightSideHookin()
	{
		return "";
	}
	public String getNavbarJavaScriptHookin()
	{
		return "";
	}
	
	/**
	 * Override this if you want to create a navbar
	 * @param writer
	 */
	public void createHtmlBodyNavbar(PrintWriter writer)
	{
		writer.print(HtmlStatic.getHtmlNavbar(getPageSection(), getNavbarHtmlRightSideHookin(), isLoginEnabled()));
//		writer.println("<br>");

		writer.println(getNavbarJavaScriptHookin());
	}

	/** called from <code>createHtmlBody()</code> */
	public void createHtmlBodyJavaScriptTop(PrintWriter writer)
	{
	}

	/** called from <code>createHtmlBody()</code> */
	public abstract void createHtmlBodyContent(PrintWriter writer);

	/** called from <code>createHtmlBody()</code> */
	public void createHtmlBodyJavaScriptBottom(PrintWriter writer)
	{
	}

	/** called from <code>createHtmlBody()</code> */
	public void createHtmlBodyLoginCheck(PrintWriter writer)
	{
	}


	/** 
	 * What parameters do this JavaServlet expect<br>
	 * This one extract all parameters from: <code>getUrlParameterDescription()</code>
	 */
	public String[] getKnownParameters()
	{
		Set<UrlParameterDescription> parameterDescriptions = getUrlParameterDescriptions();
		if (parameterDescriptions == null)
			return null;

		List<String> knownParams = new ArrayList<>();

		for (UrlParameterDescription paramDesc : parameterDescriptions)
			knownParams.add(paramDesc.getName());

		return knownParams.toArray(new String[0]);
	}

	/**
	 * Create a HTML Table with all parameters and there descriptions 
	 * <p>
	 * It will also 
	 * <ul>
	 *     <li>Display current passed values</li>
	 *     <li>You can change the values and click "reload" to load the page with the new parameters.</li>
	 * </ul>
	 * This table can be "hidden" under a "details" tag, for example:
	 * <pre>
	 * &lt;details&gt;
	 *     &lt;summary&gt;Show Parameters&lt;/summary&gt;
	 *     content of call to 'getParameterDescriptionHtmlTable()' should go here 
	 * &lt;/details&gt;
	 * </pre>
	 * @return
	 */
	public String getParameterDescriptionHtmlTable()
	{
		// TODO: Rewrite this to a "form" so we can change parameters and "re-apply" them from here...
		Set<UrlParameterDescription> paramDescSet = getUrlParameterDescriptions();
		if (paramDescSet == null)
			return "";

		StringBuilder sb = new StringBuilder();

		// START tag
		sb.append("<table class='parameter-description'> \n");

		// TBODY
		sb.append("<tbody> \n");
		for (UrlParameterDescription entry : paramDescSet)
		{
			String   name     = entry.getName();
			String   desc     = entry.getDescription();
			Object   value    = entry.getDefaultValue();
			Class<?> dataType = entry.getDataType();

			if (value == null)
				value = "";
			
			// Override the default value with the passed parameter
			String passedParamVal = getUrlParameter(name);
			if (StringUtil.hasValue(passedParamVal))
				value = passedParamVal;

			String type     = "text";
			String extra    = "";
			String preText  = "";
			String postText = "";
			if (String   .class.equals(dataType)) { type = "text";           extra = "style='width: 80%;'"; } // make text input fields wide
			if (Timestamp.class.equals(dataType)) { type = "datetime-local";  }
			if (Integer  .class.equals(dataType)) { type = "number";  }
			if (Boolean  .class.equals(dataType)) { type = "checkbox";       
			                                        value = value.toString(); 
			                                        extra = "true".equalsIgnoreCase(value.toString()) ? "checked" : ""; // Checkboxes do NOT use 'value=true|false', so lets use "checked" ... 
			                                        preText = name + ": "; // PreString to identify the checkbox
			                                      } 

			desc += "\n<hr>";
			desc += "\n" + preText + "<input id='inputField_" + name + "' name='" + name + "' value='" + value + "' placeholder='" + name + "' type='" + type + "' " + extra + "> " + postText;
			desc += "\n&nbsp;&nbsp;&nbsp; <button onclick='reloadUrlWithInputFieldParameters()' type='button' class='btn btn-secondary btn-s'>Reload</button>";
			
			sb.append("  <tr> \n");
			sb.append("    <td nowrap><b>").append( name ).append("</b></td> \n");
			sb.append("    <td nowrap>")   .append( desc ).append("</td> \n");
			sb.append("  </tr> \n");
		}
		sb.append("</tbody> \n");

		// END tag
		sb.append("</table> \n");
		
		sb.append("\n");
		sb.append("<br> \n");
		sb.append("<button onclick='reloadUrlWithInputFieldParameters()' type='button' class='btn btn-primary'>Reload Page with Above Parameters</button> \n");
		sb.append("<br> \n");

		sb.append("\n");
		sb.append("<!-- Javascript to collect the above fields and reload the page --> \n");
		sb.append("<script> \n");
		sb.append("function reloadUrlWithInputFieldParameters() \n");
		sb.append("{      \n");
		sb.append("    // Modify the current URL  \n");
		sb.append("    const url = new URL(window.location.href);  \n");
		sb.append("\n");
		sb.append("    let fieldValue = ''; \n");
		for (UrlParameterDescription entry : paramDescSet)
		{
			String   name     = entry.getName();
			Object   defVal   = entry.getDefaultValue();
			Class<?> dataType = entry.getDataType();

			if (defVal == null)
				defVal = "";

			String jsValueOp = "value";
			if (Boolean.class.equals(dataType)) 
				jsValueOp = "checked";
			
			sb.append("\n");
			sb.append("    // ------- field: " + name + " \n");
			sb.append("    fieldValue = String( document.getElementById('inputField_" + name + "')." + jsValueOp + " ); \n");  // jsValueOp = value|checked
			sb.append("    console.log('DEBUG: inputFieldFor: " + name + "=|' + fieldValue + '|.'); \n");
			sb.append("    if (fieldValue !== '' && fieldValue !== '" + defVal + "') \n");
			sb.append("    { \n");
			sb.append("        url.searchParams.set('" + name + "', fieldValue); \n");
			sb.append("    } \n");
			sb.append("    else \n");
			sb.append("    { \n");
			sb.append("        url.searchParams.delete('" + name + "'); \n");
			sb.append("    } \n");
		}
		
		sb.append("\n");
		sb.append("    // Reload URL  \n");
		sb.append("    console.log('DEBUG: LOAD NEW URL: |' + url.toString() + '|.', url); \n");
		sb.append("    window.location.href = url.toString();  \n");
		sb.append("\n");
		sb.append("}      \n");
		sb.append("</script> \n");
		
//		document.getElementById('textbox_id').value
		return sb.toString();
	}


	/**
	 * Replaces any single-quote strings (') with \x27
	 * @param str
	 * @return
	 */
	protected String escapeJsQuote(String str)
	{
		if (str == null)
			return str;
		return str.replace("'", "\\x27");
	}

	

	/**
	 * Initialize ...
	 * 
	 * @param conf
	 * @throws Exception
	 */
	public void init(Configuration conf) 
	throws Exception
	{
	}

	/**
	 * Open Any resources needed by any implementor.
	 * <p>
	 * This would typically be a DBMS Connection(s) or similar
	 */
	protected  void open()
	throws SendResponseErrorException
	{
	}

	/**
	 * Close any resources opened by the implementor
	 * <p>
	 * This would typically be a DBMS Connection(s) or similar
	 */
	protected void close()
	{
	}
	
	/**
	 * This could be used by <code>open()</code> to abort a request and send <code>response.sendError(number, message)</code> to the caller.
	 */
	public static class SendResponseErrorException
	extends Exception
	{
		private static final long serialVersionUID = 1L;

		int _responseCode;
//		String _message;
		
		public int    getResponceCode() { return _responseCode; } 
//		public String getMessage()      { return _message; } 

		public SendResponseErrorException(int responceCode, String message)
		{
			super(message);
			_responseCode = responceCode;
//			_message = message;
		}
	}
}

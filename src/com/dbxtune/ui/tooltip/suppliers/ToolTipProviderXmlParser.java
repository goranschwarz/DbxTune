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
package com.dbxtune.ui.tooltip.suppliers;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ToolTipProviderXmlParser
extends DefaultHandler
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private SAXParserFactory _saxFactory      = SAXParserFactory.newInstance();
	private SAXParser        _saxParser       = null;
	private Locator          _locator         = null; // Where are we in the XML file

	private String           _xmlFileName     = null;
	
	private Exception        _lastException   = null;

	public ToolTipProviderXmlParser()
	{
		try
		{
			_lastException = null;
			_saxParser = _saxFactory.newSAXParser();
		}
		catch (SAXException e)
		{
			_logger.warn("Problems Creating ToolTip Provider XML Parser '"+getFileName()+"'. Caught: "+e, e);
			_lastException = e;
		}
		catch (ParserConfigurationException e)
		{
			_logger.warn("Problems Creating ToolTip Provider XML Parser '"+getFileName()+"'. Caught: "+e, e);
			_lastException = e;
		}
	}

	public TtpEntry parseEntry(String entry)
	{
		_lastEntry       = null;
		_entryList       = null;

		try
		{
			_lastException = null;
			_saxParser.parse(new InputSource(new StringReader(entry)), this);
		}
		catch (SAXException e)
		{
			_logger.warn("Problems Parsing ToolTip Provider Entry '"+entry+"'. Caught: "+e, e);
			_lastException = e;
		}
		catch (IOException e)
		{
			_logger.warn("Problems Parsing ToolTip Provider Entry '"+entry+"'. Caught: "+e, e);
			_lastException = e;
		}
		return _lastEntry;
	}

	public void setFileName(String filename)
	{
		_xmlFileName = filename;
	}

	public String getFileName()
	{
		return _xmlFileName;
	}
	

	public ArrayList<TtpEntry> parseFile()
	{
		return parseFile(getFileName());
	}
	
	/**
	 *  Parse a file
	 *  @param filename        Name of the file
	 *  
	 *  @return A ArrayList with TtpEntry
	 */
	public ArrayList<TtpEntry> parseFile(String fileName)
	{
		try
		{
			_lastException = null;
//			_saxParser.parse(new InputSource(new FileReader(fileName)), this);
			_saxParser.parse(new File(fileName), this);
		}
		catch (SAXException e)
		{
			_logger.warn("Problems Parsing ToolTip Provider File '"+fileName+"'. Caught: "+e, e);
			_lastException = e;
		}
		catch (IOException e)
		{
			_logger.warn("Problems Parsing ToolTip Provider File '"+fileName+"'. Caught: "+e, e);
			_lastException = e;
		}
		return _entryList;
	}

	public Exception getException()
	{
		return _lastException;
	}

	@Override
	public void setDocumentLocator(Locator locator)
	{
		super.setDocumentLocator(locator);
//		System.out.println("ToolTipProviderXmlParser:setDocumentLocator() locator="+locator);
		_locator = locator;
	}

	//----------------------------------------------------------
	// START: XML Parsing code
	//----------------------------------------------------------
	private StringBuilder       _xmlTagBuffer = new StringBuilder();
	private TtpEntry            _lastEntry    = null;
	private ArrayList<TtpEntry> _entryList    = null;

	@Override
	public void characters(char[] buffer, int start, int length)
	{
		_xmlTagBuffer.append(buffer, start, length);
//		System.out.println("XML.character: start="+start+", length="+length+", _xmlTagBuffer="+_xmlTagBuffer);
	}

	@Override
	public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) 
	throws SAXException
	{
//System.out.println("BeginTag '"+qName+"'"+getXmlFileDetailes()+"...");
		
		_xmlTagBuffer.setLength(0);
//		System.out.println("SAX: startElement: qName='"+qName+"', attributes="+attributes);
		if (TtpEntry.XML_TAG_ENTRY.equals(qName))
		{
			_lastEntry = new TtpEntry();
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) 
	throws SAXException
	{
//		System.out.println("SAX: endElement: qName='"+qName+"', _xmlTagBuffer="+_xmlTagBuffer);
		if (TtpEntry.XML_TAG_ENTRY.equals(qName))
		{
			if (_entryList == null)
				_entryList = new ArrayList<TtpEntry>();

//System.out.println("_lastEntry="+_lastEntry);
			_entryList.add(_lastEntry);
		}
		else
		{
			if      (TtpEntry.XML_SUBTAG_CMD_NAME    .equals(qName)) _lastEntry.setCmdName    (_xmlTagBuffer.toString().trim());
			else if (TtpEntry.XML_SUBTAG_MODULE      .equals(qName)) _lastEntry.setModule     (_xmlTagBuffer.toString().trim());
			else if (TtpEntry.XML_SUBTAG_SECTION     .equals(qName)) _lastEntry.setSection    (_xmlTagBuffer.toString().trim());
			else if (TtpEntry.XML_SUBTAG_FROM_VERSION.equals(qName)) _lastEntry.setFromVersion(_xmlTagBuffer.toString().trim());
			else if (TtpEntry.XML_SUBTAG_DESCRIPTION .equals(qName)) _lastEntry.setDescription(_xmlTagBuffer.toString().trim());
			else if (TtpEntry.XML_SUBTAG_SYNTAX      .equals(qName)) _lastEntry.setSyntax     (_xmlTagBuffer.toString().trim());
			else if (TtpEntry.XML_SUBTAG_PARAMETERS  .equals(qName)) _lastEntry.setParameters (_xmlTagBuffer.toString().trim());
			else if (TtpEntry.XML_SUBTAG_EXAMPLE     .equals(qName)) _lastEntry.setExample    (_xmlTagBuffer.toString().trim());
			else if (TtpEntry.XML_SUBTAG_USAGE       .equals(qName)) _lastEntry.setUsage      (_xmlTagBuffer.toString().trim());
			else if (TtpEntry.XML_SUBTAG_PERMISSIONS .equals(qName)) _lastEntry.setPermissions(_xmlTagBuffer.toString().trim());
			else if (TtpEntry.XML_SUBTAG_SEE_ALSO    .equals(qName)) _lastEntry.setSeeAlso    (_xmlTagBuffer.toString().trim());
			else if (TtpEntry.XML_SUBTAG_SOURCE_URL  .equals(qName)) _lastEntry.setSourceUrl  (_xmlTagBuffer.toString().trim());
			else if (TtpEntry.XML_TAG_ENTRIES        .equals(qName)) { /* DO NOTHING ON </Entries>*/ }
			else 
			{
				_logger.warn("Found a 'end' tag '"+qName+"'"+getXmlFileDetailes()+", that was not expected, skipping this and continuing...");
			}
		}
		_xmlTagBuffer.setLength(0);
	}
	//----------------------------------------------------------
	// END: XML Parsing code
	//----------------------------------------------------------
	
	private String getXmlFileDetailes()
	{
		String lineSpec = "";
		if (_locator != null)
		{
			int lineNum     = _locator.getLineNumber();
			int colPos      = _locator.getColumnNumber();
			String systemId = _locator.getSystemId();
			lineSpec = ", at line="+lineNum+", col="+colPos+", ID="+systemId;
		}
		return lineSpec;
	}
}

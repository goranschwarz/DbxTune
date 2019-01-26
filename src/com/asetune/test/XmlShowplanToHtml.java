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
package com.asetune.test;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class XmlShowplanToHtml
{
	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args)
	{
		if ( args.length != 3 )
		{
			System.err.println("give command as follows : ");
			System.err.println("XSLTTest data.xml converted.xsl converted.html");
			return;
		}
		String dataXML = args[0];
		String inputXSL = args[1];
		String outputHTML = args[2];

		XmlShowplanToHtml st = new XmlShowplanToHtml();
		try
		{
			st.transform(dataXML, inputXSL, outputHTML);
		}
		catch (TransformerConfigurationException e)
		{
			System.err.println("TransformerConfigurationException");
			System.err.println(e);
		}
		catch (TransformerException e)
		{
			System.err.println("TransformerException");
			System.err.println(e);
		}
	}

	public void transform(String dataXML, String inputXSL, String outputHTML) 
	throws TransformerConfigurationException, TransformerException
	{
		TransformerFactory factory = TransformerFactory.newInstance();
		StreamSource xslStream = new StreamSource(inputXSL);
		Transformer transformer = factory.newTransformer(xslStream);
		StreamSource in = new StreamSource(dataXML);
		StreamResult out = new StreamResult(outputHTML);
		transformer.transform(in, out);
		System.out.println("The generated HTML file is:" + outputHTML);
	}

}

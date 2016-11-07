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

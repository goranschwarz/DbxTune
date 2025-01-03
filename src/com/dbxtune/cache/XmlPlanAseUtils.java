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
package com.dbxtune.cache;

import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.dbxtune.utils.StringUtil;

public class XmlPlanAseUtils
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Get SQL Statement from a ASE XML Showplan
	 * @param xmlPlan
	 * @return
	 */
	public static String getSqlStatement(String xmlPlan)
	{
		if (StringUtil.isNullOrBlank(xmlPlan))
			return xmlPlan;

		int startPos = xmlPlan.indexOf("<![CDATA[");
		int endPos   = xmlPlan.indexOf("]]>");

		if (startPos >= 0 && endPos >= 0)
		{
			startPos += "<![CDATA[".length();
			
			String newSQLText = xmlPlan.substring(startPos, endPos).trim();
			
			if (newSQLText.startsWith("SQL Text:"))
				newSQLText = newSQLText.substring("SQL Text:".length()).trim();

			return newSQLText;
		}
		return null;
	}

	/**
	 * Get Compile and Execution values for parameters as a HTML Table
	 * 
	 * @param xmlPlan
	 * @return empty if nothing was found or we had problems
	 */
	public static String getCompileAndExecParamsAsHtml(String xmlPlan)
	{
		return getCompileAndExecParamsAsHtml(xmlPlan, true, true);
	}

	/**
	 * Get Compile and Execution values for parameters as a HTML Table
	 * 
	 * @param xmlPlan
	 * @param getCompileParams
	 * @param getExecuteParams
	 * @return empty if nothing was found or we had problems
	 */
	public static String getCompileAndExecParamsAsHtml(String xmlPlan, boolean getCompileParams, boolean getExecuteParams)
	{
		LinkedHashMap<String, String>      compileParams = getCompileParams ? new LinkedHashMap<>() : null;
		LinkedHashMap<String, List<String>> execParams   = getExecuteParams ? new LinkedHashMap<>() : null;
		
		// Parse and get the parameters from XML
		getCompileAndExecParams(xmlPlan, compileParams, execParams);
		
		if (compileParams.isEmpty() && execParams.isEmpty())
			return "";

		// Build HTML Table
		StringBuilder sb = new StringBuilder();

		//-----------------------------------
		// COMPILE PARAMS
		if ( compileParams != null && ! compileParams.isEmpty() )
		{
			sb.append("\n");
			sb.append("<table class='ase-showplan-compile-params'> \n");

			sb.append("<thead> \n");
			sb.append("  <tr> \n");
			sb.append("    <th>Compile Parameter Names:</th>\n");
			for (String key : compileParams.keySet())
				sb.append("    <th>").append(key).append("</th>\n");
			sb.append("  </tr> \n");
			sb.append("</thead> \n");

			sb.append("<tbody> \n");
			sb.append("  <tr> \n");
			sb.append("    <td><b>Compile Only ONE parameter:</b></td>\n");
			for (String val : compileParams.values())
				sb.append("    <td>").append(val).append("</th>\n");
			sb.append("  </tr> \n");
			sb.append("</tbody> \n");

			sb.append("</table> \n");
		}

		//-----------------------------------
		// EXECUTE PARAMS
		if ( execParams != null && ! execParams.isEmpty() )
		{
			sb.append("\n");

			// Add extra line if we had compile params
			if ( ! compileParams.isEmpty() )
				sb.append("<br>\n");

			sb.append("<table class='ase-showplan-execute-params'> \n");

			int numExecValues = 0;
			
			sb.append("<thead> \n");
			sb.append("  <tr> \n");
			sb.append("    <th>Execution Parameter Names:</th>\n");
			for (String key : execParams.keySet())
			{
				sb.append("    <th>").append(key).append("</th>\n");
				
				// each parameter has a list of values... get max
				numExecValues = Math.max(numExecValues, execParams.get(key).size());
			}
			sb.append("  </tr> \n");
			sb.append("</thead> \n");

			sb.append("<tbody> \n");
			for (int r=0; r<numExecValues; r++)
			{
				sb.append("  <tr> \n");
				
				sb.append("    <td><b>Execution Parameter Set: ").append(r+1).append("</b></td>\n");

				for (List<String> valueList : execParams.values())
					sb.append("    <td>").append(valueList.get(r)).append("</th>\n");

				sb.append("  </tr> \n");
			}
			sb.append("</tbody> \n");

			sb.append("</table> \n");
		}
		
		return sb.toString();
	}

	/**
	 * Get Compile and Execution values for parameters
	 * 
	 * @param xmlPlan
	 * @param compileMap    a LinkedHashMap which will be filled in! Map(key=ParameterName, value=compileValue)
	 * @param execMap       a LinkedHashMap which will be filled in! Map(key=ParameterName, value=ListOfExecutionValues) List since there can be "many" different execution values
	 */
	public static void getCompileAndExecParams(String xmlPlan, LinkedHashMap<String, String> compileMap, LinkedHashMap<String, List<String>> execMap)
	{
		try
		{
			InputSource source = new InputSource(new StringReader(xmlPlan.trim()));
			
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

			Document doc = dBuilder.parse(source);
			doc.getDocumentElement().normalize();

			XPath xPath = XPathFactory.newInstance().newXPath();

			if (compileMap != null)
			{
				String expression = "/query/plan/compileParameters/parameter";
				NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);

				for (int i = 0; i < nodeList.getLength(); i++) 
				{
					Node nNode = nodeList.item(i);
					if ("parameter".equals(nNode.getNodeName()))
					{
//						System.out.println();
//						System.out.println("Current Element :" + nNode.getNodeName());

						if (nNode.getNodeType() == Node.ELEMENT_NODE) 
						{
							Element eElement = (Element) nNode;
//							System.out.println("name  : " + eElement.getElementsByTagName("name"  ).item(0).getTextContent());
//							System.out.println("number: " + eElement.getElementsByTagName("number").item(0).getTextContent());
//							System.out.println("type  : " + eElement.getElementsByTagName("type"  ).item(0).getTextContent());
//							System.out.println("value : " + eElement.getElementsByTagName("value" ).item(0).getTextContent());

							String name  = eElement.getElementsByTagName("name" ).item(0).getTextContent();
							String value = eElement.getElementsByTagName("value").item(0).getTextContent();

							compileMap.put(name, value);
						}
					}
				}
			}
			if (execMap != null)
			{
				String expression = "/query/plan/execParameters/parameter";
				NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);

				for (int i = 0; i < nodeList.getLength(); i++) 
				{
					Node nNode = nodeList.item(i);
					if ("parameter".equals(nNode.getNodeName()))
					{
//						System.out.println();
//						System.out.println("Current Element :" + nNode.getNodeName());

						if (nNode.getNodeType() == Node.ELEMENT_NODE) 
						{
							Element eElement = (Element) nNode;
//							System.out.println("name  : " + eElement.getElementsByTagName("name"  ).item(0).getTextContent());
//							System.out.println("number: " + eElement.getElementsByTagName("number").item(0).getTextContent());
//							System.out.println("type  : " + eElement.getElementsByTagName("type"  ).item(0).getTextContent());
//							System.out.println("value : " + eElement.getElementsByTagName("value" ).item(0).getTextContent());

							String name  = eElement.getElementsByTagName("name" ).item(0).getTextContent();
							String value = eElement.getElementsByTagName("value").item(0).getTextContent();

							List<String> valueList = execMap.get(name);
							if (valueList == null)
								valueList = new ArrayList<>();

							valueList.add(value);
							execMap.put(name, valueList);
						}
					}
				}
			}
		} 
		catch (Exception ex) 
		{
			_logger.error("Problems when parsing ASE Showplan XML.", ex);
		} 
	}

	public static void main(String[] args)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("\n");
		sb.append("<query>\n");
		sb.append("	<statementId>113887604</statementId>\n");
		sb.append("	<text>\n");
		sb.append("		<![CDATA[\n");
		sb.append("			SQL Text: select T1.M__DATE_, T1.M__ALIAS_, T1.M_REFERENCE, T1.M_SETNAME, T1.M_US_TYPE, T1.M_TYPE,T2.M__DATE_, T2.M__ALIAS_, T2.M_REFERENCE, T2.M_MATCODE from MPX_VOLMAT_DBF T1 ,MPY_VOLMAT_DBF T2 where T1.M_REFERENCE *= T2.M_REFERENCE and T1.M__DATE_ *= T2.M__DATE_ and T1.M__ALIAS_ *= T2.M__ALIAS_ and ((((T1.M__DATE_=@@@V0_VCHAR1) and (T1.M__ALIAS_=@@@V1_VCHAR1)) and ((T1.M_SETNAME = @@@V2_VCHAR1 and T1.M_US_TYPE = @@@V3_INT) and T1.M_TYPE = @@@V4_INT)) and ((T2.M__DATE_=@@@V5_VCHAR1) and (T2.M__ALIAS_=@@@V6_VCHAR1))) order by T1.M_SETNAME asc ,T1.M_US_TYPE asc ,T1.M_TYPE asc(@@@V0_VCHAR1 VARCHAR(64), @@@V1_VCHAR1 VARCHAR(64), @@@V2_VCHAR1 VARCHAR(64), @@@V3_INT INT, @@@V4_INT INT, @@@V5_VCHAR1 VARCHAR(64), @@@V6_VCHAR1 VARCHAR(64)) ]]>\n");
		sb.append("	</text>\n");
		sb.append("	\n");
		sb.append("	<plan>\n");
		sb.append("		<planId>124359611</planId>\n");
		sb.append("		<planStatus> available </planStatus>\n");
		sb.append("		<planSharing> notShareable </planSharing>\n");
		sb.append("		<execCount>1</execCount>\n");
		sb.append("		<maxTime>155758</maxTime>\n");
		sb.append("		<avgTime>155758</avgTime>\n");
		sb.append("		<maxPreQueryTime>7</maxPreQueryTime>\n");
		sb.append("		<avgPreQueryTime>7</avgPreQueryTime>\n");
		sb.append("		<maxExecTime>154919</maxExecTime>\n");
		sb.append("		<avgExecTime>154919</avgExecTime>\n");
		sb.append("		\n");
		sb.append("		<compileParameters>\n");
		sb.append("			<parameter>\n");
		sb.append("				<name>@@@V0_VCHAR1</name>\n");
		sb.append("				<number>1</number>\n");
		sb.append("				<type>VARCHAR</type>\n");
		sb.append("				<value>'20211230'</value>\n");
		sb.append("			</parameter>\n");
		sb.append("			\n");
		sb.append("			<parameter>\n");
		sb.append("				<name>@@@V1_VCHAR1</name>\n");
		sb.append("				<number>2</number>\n");
		sb.append("				<type>VARCHAR</type>\n");
		sb.append("				<value>'BO'</value>\n");
		sb.append("			</parameter>\n");
		sb.append("			\n");
		sb.append("			<parameter>\n");
		sb.append("				<name>@@@V2_VCHAR1</name>\n");
		sb.append("				<number>3</number>\n");
		sb.append("				<type>VARCHAR</type>\n");
		sb.append("				<value>'68338'</value>\n");
		sb.append("			</parameter>\n");
		sb.append("			\n");
		sb.append("			<parameter>\n");
		sb.append("				<name>@@@V3_INT</name>\n");
		sb.append("				<number>4</number>\n");
		sb.append("				<type>INT4</type>\n");
		sb.append("				<value>0</value>\n");
		sb.append("			</parameter>\n");
		sb.append("			\n");
		sb.append("			<parameter>\n");
		sb.append("				<name>@@@V4_INT</name>\n");
		sb.append("				<number>5</number>\n");
		sb.append("				<type>INT4</type>\n");
		sb.append("				<value>0</value>\n");
		sb.append("			</parameter>\n");
		sb.append("			\n");
		sb.append("			<parameter>\n");
		sb.append("				<name>@@@V5_VCHAR1</name>\n");
		sb.append("				<number>6</number>\n");
		sb.append("				<type>VARCHAR</type>\n");
		sb.append("				<value>'20211230'</value>\n");
		sb.append("			</parameter>\n");
		sb.append("			\n");
		sb.append("			<parameter>\n");
		sb.append("				<name>@@@V6_VCHAR1</name>\n");
		sb.append("				<number>7</number>\n");
		sb.append("				<type>VARCHAR</type>\n");
		sb.append("				<value>'BO'</value>\n");
		sb.append("			</parameter>\n");
		sb.append("		\n");
		sb.append("		</compileParameters>\n");
		sb.append("		\n");

		sb.append("		<execParameters>\n");
		sb.append("			<parameter>\n");
		sb.append("				<name>@@@V0_VCHAR1</name>\n");
		sb.append("				<number>1</number>\n");
		sb.append("				<type>VARCHAR</type>\n");
		sb.append("				<value>'20211230'</value>\n");
		sb.append("			</parameter>\n");
		sb.append("			\n");
		sb.append("			<parameter>\n");
		sb.append("				<name>@@@V1_VCHAR1</name>\n");
		sb.append("				<number>2</number>\n");
		sb.append("				<type>VARCHAR</type>\n");
		sb.append("				<value>'BO'</value>\n");
		sb.append("			</parameter>\n");
		sb.append("			\n");
		sb.append("			<parameter>\n");
		sb.append("				<name>@@@V2_VCHAR1</name>\n");
		sb.append("				<number>3</number>\n");
		sb.append("				<type>VARCHAR</type>\n");
		sb.append("				<value>'68338'</value>\n");
		sb.append("			</parameter>\n");
		sb.append("			\n");
		sb.append("			<parameter>\n");
		sb.append("				<name>@@@V3_INT</name>\n");
		sb.append("				<number>4</number>\n");
		sb.append("				<type>INT4</type>\n");
		sb.append("				<value>0</value>\n");
		sb.append("			</parameter>\n");
		sb.append("			\n");
		sb.append("			<parameter>\n");
		sb.append("				<name>@@@V4_INT</name>\n");
		sb.append("				<number>5</number>\n");
		sb.append("				<type>INT4</type>\n");
		sb.append("				<value>0</value>\n");
		sb.append("			</parameter>\n");
		sb.append("			\n");
		sb.append("			<parameter>\n");
		sb.append("				<name>@@@V5_VCHAR1</name>\n");
		sb.append("				<number>6</number>\n");
		sb.append("				<type>VARCHAR</type>\n");
		sb.append("				<value>'20211230'</value>\n");
		sb.append("			</parameter>\n");
		sb.append("			\n");
		sb.append("			<parameter>\n");
		sb.append("				<name>@@@V6_VCHAR1</name>\n");
		sb.append("				<number>7</number>\n");
		sb.append("				<type>VARCHAR</type>\n");
		sb.append("				<value>'BO'</value>\n");
		sb.append("			</parameter>\n");
		sb.append("		\n");
		sb.append("		</execParameters>\n");
		
		
sb.append("		<execParameters>\n");
sb.append("			<parameter>\n");
sb.append("				<name>@@@V0_VCHAR1</name>\n");
sb.append("				<number>1</number>\n");
sb.append("				<type>VARCHAR</type>\n");
sb.append("				<value>'x--20211230'</value>\n");
sb.append("			</parameter>\n");
sb.append("			\n");
sb.append("			<parameter>\n");
sb.append("				<name>@@@V1_VCHAR1</name>\n");
sb.append("				<number>2</number>\n");
sb.append("				<type>VARCHAR</type>\n");
sb.append("				<value>'x--BO'</value>\n");
sb.append("			</parameter>\n");
sb.append("			\n");
sb.append("			<parameter>\n");
sb.append("				<name>@@@V2_VCHAR1</name>\n");
sb.append("				<number>3</number>\n");
sb.append("				<type>VARCHAR</type>\n");
sb.append("				<value>'x--68338'</value>\n");
sb.append("			</parameter>\n");
sb.append("			\n");
sb.append("			<parameter>\n");
sb.append("				<name>@@@V3_INT</name>\n");
sb.append("				<number>4</number>\n");
sb.append("				<type>INT4</type>\n");
sb.append("				<value>x--0</value>\n");
sb.append("			</parameter>\n");
sb.append("			\n");
sb.append("			<parameter>\n");
sb.append("				<name>@@@V4_INT</name>\n");
sb.append("				<number>5</number>\n");
sb.append("				<type>INT4</type>\n");
sb.append("				<value>x--0</value>\n");
sb.append("			</parameter>\n");
sb.append("			\n");
sb.append("			<parameter>\n");
sb.append("				<name>@@@V5_VCHAR1</name>\n");
sb.append("				<number>6</number>\n");
sb.append("				<type>VARCHAR</type>\n");
sb.append("				<value>'x--20211230'</value>\n");
sb.append("			</parameter>\n");
sb.append("			\n");
sb.append("			<parameter>\n");
sb.append("				<name>@@@V6_VCHAR1</name>\n");
sb.append("				<number>7</number>\n");
sb.append("				<type>VARCHAR</type>\n");
sb.append("				<value>'x--BO'</value>\n");
sb.append("			</parameter>\n");
sb.append("		\n");
sb.append("		</execParameters>\n");
		sb.append("		\n");
		sb.append("		<opTree>\n");
		sb.append("			<Emit>\n");
		sb.append("			<VA>10</VA>\n");
		sb.append("			<est>\n");
		sb.append("				<rowCnt>35535.81</rowCnt>\n");
		sb.append("				<lio>0</lio>\n");
		sb.append("				<pio>0</pio>\n");
		sb.append("				<rowSz>66</rowSz>\n");
		sb.append("			</est>\n");
		sb.append("			<act>\n");
		sb.append("				<rowCnt>18</rowCnt>\n");
		sb.append("			</act>\n");
		sb.append("			<arity>1</arity>\n");
		sb.append("				<Restrict>\n");
		sb.append("				<VA>9</VA>\n");
		sb.append("				<est>\n");
		sb.append("					<rowCnt>0</rowCnt>\n");
		sb.append("					<lio>0</lio>\n");
		sb.append("					<pio>0</pio>\n");
		sb.append("					<rowSz>0</rowSz>\n");
		sb.append("				</est>\n");
		sb.append("				<act>\n");
		sb.append("					<rowCnt>18</rowCnt>\n");
		sb.append("				</act>\n");
		sb.append("				<arity>1</arity>\n");
		sb.append("					<Sequencer>\n");
		sb.append("					<VA>8</VA>\n");
		sb.append("					<est>\n");
		sb.append("						<rowCnt>0</rowCnt>\n");
		sb.append("						<lio>0</lio>\n");
		sb.append("						<pio>0</pio>\n");
		sb.append("						<rowSz>0</rowSz>\n");
		sb.append("					</est>\n");
		sb.append("					<act>\n");
		sb.append("						<rowCnt>18</rowCnt>\n");
		sb.append("					</act>\n");
		sb.append("					<arity>2</arity>\n");
		sb.append("						<StoreIndex>\n");
		sb.append("						<VA>3</VA>\n");
		sb.append("						<est>\n");
		sb.append("							<rowCnt>35535.81</rowCnt>\n");
		sb.append("							<lio>390</lio>\n");
		sb.append("							<pio>770</pio>\n");
		sb.append("							<rowSz>37</rowSz>\n");
		sb.append("						</est>\n");
		sb.append("						<act>\n");
		sb.append("							<rowCnt>422</rowCnt>\n");
		sb.append("							<lio>12</lio>\n");
		sb.append("							<pio>2</pio>\n");
		sb.append("						</act>\n");
		sb.append("						<arity>1</arity>\n");
		sb.append("							<Insert>\n");
		sb.append("							<VA>2</VA>\n");
		sb.append("							<est>\n");
		sb.append("								<rowCnt>35535.81</rowCnt>\n");
		sb.append("								<lio>35535.81</lio>\n");
		sb.append("								<pio>323.5298</pio>\n");
		sb.append("								<rowSz>37</rowSz>\n");
		sb.append("							</est>\n");
		sb.append("							<act>\n");
		sb.append("								<rowCnt>422</rowCnt>\n");
		sb.append("								<lio>424</lio>\n");
		sb.append("								<pio>0</pio>\n");
		sb.append("							</act>\n");
		sb.append("							<arity>1</arity>\n");
		sb.append("								<Restrict>\n");
		sb.append("								<VA>1</VA>\n");
		sb.append("								<est>\n");
		sb.append("									<rowCnt>35535.81</rowCnt>\n");
		sb.append("									<lio>0</lio>\n");
		sb.append("									<pio>0</pio>\n");
		sb.append("									<rowSz>37</rowSz>\n");
		sb.append("								</est>\n");
		sb.append("								<act>\n");
		sb.append("									<rowCnt>422</rowCnt>\n");
		sb.append("								</act>\n");
		sb.append("								<arity>1</arity>\n");
		sb.append("									<TableScan>\n");
		sb.append("										<VA>0</VA>\n");
		sb.append("										<est>\n");
		sb.append("											<rowCnt>35535.81</rowCnt>\n");
		sb.append("											<lio>41067</lio>\n");
		sb.append("											<pio>5207</pio>\n");
		sb.append("											<rowSz>37</rowSz>\n");
		sb.append("										</est>\n");
		sb.append("										<act>\n");
		sb.append("											<rowCnt>422</rowCnt>\n");
		sb.append("											<lio>41069</lio>\n");
		sb.append("											<pio>0</pio>\n");
		sb.append("										</act>\n");
		sb.append("										<varNo>0</varNo>\n");
		sb.append("										<objName>MPY_VOLMAT_DBF</objName>\n");
		sb.append("										<corrName>T2</corrName>\n");
		sb.append("										<scanType>TableScan</scanType>\n");
		sb.append("										<scanOrder> ForwardScan </scanOrder>\n");
		sb.append("										<positioning> StartOfTable </positioning>\n");
		sb.append("										<scanCoverage> NonCovered </scanCoverage>\n");
		sb.append("										<dataIOSizeInKB>32</dataIOSizeInKB>\n");
		sb.append("										<dataBufReplStrategy> LRU </dataBufReplStrategy>\n");
		sb.append("									</TableScan>\n");
		sb.append("								</Restrict>\n");
		sb.append("							<objName>Worktable1</objName><updateMode> Direct </updateMode>\n");
		sb.append("							</Insert>\n");
		sb.append("						</StoreIndex>\n");
		sb.append("						<NestLoopJoin>\n");
		sb.append("						<VA>7</VA>\n");
		sb.append("						<est>\n");
		sb.append("							<rowCnt>35535.81</rowCnt>\n");
		sb.append("							<lio>0</lio>\n");
		sb.append("							<pio>0</pio>\n");
		sb.append("							<rowSz>66</rowSz>\n");
		sb.append("						</est>\n");
		sb.append("						<act>\n");
		sb.append("							<rowCnt>18</rowCnt>\n");
		sb.append("						</act>\n");
		sb.append("						<arity>2</arity>\n");
		sb.append("							<Restrict>\n");
		sb.append("							<VA>5</VA>\n");
		sb.append("							<est>\n");
		sb.append("								<rowCnt>2.27341</rowCnt>\n");
		sb.append("								<lio>0</lio>\n");
		sb.append("								<pio>0</pio>\n");
		sb.append("								<rowSz>49</rowSz>\n");
		sb.append("							</est>\n");
		sb.append("							<act>\n");
		sb.append("								<rowCnt>1</rowCnt>\n");
		sb.append("							</act>\n");
		sb.append("							<arity>1</arity>\n");
		sb.append("								<TableScan>\n");
		sb.append("									<VA>4</VA>\n");
		sb.append("									<est>\n");
		sb.append("										<rowCnt>2.27341</rowCnt>\n");
		sb.append("										<lio>3590</lio>\n");
		sb.append("										<pio>438</pio>\n");
		sb.append("										<rowSz>49</rowSz>\n");
		sb.append("									</est>\n");
		sb.append("									<act>\n");
		sb.append("										<rowCnt>1</rowCnt>\n");
		sb.append("										<lio>3590</lio>\n");
		sb.append("										<pio>0</pio>\n");
		sb.append("									</act>\n");
		sb.append("									<varNo>2</varNo>\n");
		sb.append("									<objName>MPX_VOLMAT_DBF</objName>\n");
		sb.append("									<corrName>T1</corrName>\n");
		sb.append("									<scanType>TableScan</scanType>\n");
		sb.append("									<scanOrder> ForwardScan </scanOrder>\n");
		sb.append("									<positioning> StartOfTable </positioning>\n");
		sb.append("									<scanCoverage> NonCovered </scanCoverage>\n");
		sb.append("									<dataIOSizeInKB>32</dataIOSizeInKB>\n");
		sb.append("									<dataBufReplStrategy> LRU </dataBufReplStrategy>\n");
		sb.append("								</TableScan>\n");
		sb.append("							</Restrict>\n");
		sb.append("							<IndexScan>\n");
		sb.append("								<VA>6</VA>\n");
		sb.append("								<est>\n");
		sb.append("									<rowCnt>35535.81</rowCnt>\n");
		sb.append("									<lio>328.0766</lio>\n");
		sb.append("									<pio>40.69122</pio>\n");
		sb.append("									<rowSz>37</rowSz>\n");
		sb.append("								</est>\n");
		sb.append("								<act>\n");
		sb.append("									<rowCnt>18</rowCnt>\n");
		sb.append("									<lio>438</lio>\n");
		sb.append("									<pio>0</pio>\n");
		sb.append("								</act>\n");
		sb.append("								<varNo>1</varNo>\n");
		sb.append("								<objName>WorkTable# 1</objName>\n");
		sb.append("								<scanType>IndexScan</scanType>\n");
		sb.append("								<indId>1</indId>\n");
		sb.append("								<scanOrder> ForwardScan </scanOrder>\n");
		sb.append("								<positioning> ByKey </positioning>\n");
		sb.append("								<scanCoverage> NonCovered </scanCoverage>\n");
		sb.append("								<dataIOSizeInKB>32</dataIOSizeInKB>\n");
		sb.append("								<dataBufReplStrategy> LRU </dataBufReplStrategy>\n");
		sb.append("							</IndexScan>\n");
		sb.append("						</NestLoopJoin>\n");
		sb.append("					</Sequencer>\n");
		sb.append("				</Restrict>\n");
		sb.append("			</Emit>\n");
		sb.append("			<est>\n");
		sb.append("				<totalLio>80910.89</totalLio>\n");
		sb.append("				<totalPio>6779.221</totalPio>\n");
		sb.append("			</est>\n");
		sb.append("			<act>\n");
		sb.append("				<totalLio>45527</totalLio>\n");
		sb.append("				<totalPio>2</totalPio>\n");
		sb.append("			</act>\n");
		sb.append("		\n");
		sb.append("		</opTree>\n");
		sb.append("	\n");
		sb.append("	</plan>\n");
		sb.append("	</query>\n");

		LinkedHashMap<String, String> compileParams = new LinkedHashMap<>();
		LinkedHashMap<String, List<String>> execParams = new LinkedHashMap<>();
		getCompileAndExecParams(sb.toString(), compileParams, execParams);

		System.out.println("Compile Params: " + compileParams);
		System.out.println("   Exec Params: " + execParams);

		System.out.println(getCompileAndExecParamsAsHtml(sb.toString()));
	}
}

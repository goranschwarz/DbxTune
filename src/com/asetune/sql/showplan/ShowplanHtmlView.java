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
package com.asetune.sql.showplan;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.asetune.Version;
import com.asetune.utils.FileUtils;
import com.asetune.utils.SwingUtils;

public abstract class ShowplanHtmlView
{
	private static Logger _logger = Logger.getLogger(ShowplanHtmlView.class);

//	private static final String _baseTempPath = System.getProperty("java.io.tmpdir");
	public static final String TMP_SHOWPLAN_PATH = System.getProperty("java.io.tmpdir") + File.separator + "showplan" + File.separator;
	
	public enum Type
	{
		SQLSERVER,  // Showplan for DBMS 
		ASE         // Showplan for Sybase ASE
	};

	public static void show(Type type, String xmlPlan)
	{
		if (Type.SQLSERVER.equals(type))
		{
			ShowplanSqlServerExternalHtmlView sqlserverPlan = new ShowplanSqlServerExternalHtmlView();
			sqlserverPlan.show(xmlPlan);
		}
		else if (Type.ASE.equals(type))
		{
			System.out.println("ASE-XML-SHOWPLAN: \n" + xmlPlan);
			throw new RuntimeException("Sybase ASE Showplan HTML Viewer has not yet been implemented.");
//			ShowplanAseExternalHtmlView asePlan = new ShowplanAseExternalHtmlView();
//			asePlan.show(xmlPlan);
		}
		else
		{
			throw new IllegalArgumentException("Unknown Showplan type");
		}
	}

	/**
	 * show the HTML Entry in the default HTML Browser<br>
	 * But first, validate that the temp directory exists, and copy all resources/templates to the directory
	 *  
	 * @param xmlPlan The XML ShowPlan content (note: not a file, but the actual XML Content)
	 */
	protected void show(String xmlPlan)
	{
		try
		{
			validateTempDir();
			
			File filename = createHtmlFile(xmlPlan);
			if (filename != null)
				viewHtmlFile(filename);
		}
		catch(Exception e)
		{
			showError("Problems Creating a HTML Showplan", e);
		}
	}

	public static File createHtmlFile(Type type, String xmlPlan)
	{
		if (Type.SQLSERVER.equals(type))
		{
			ShowplanSqlServerExternalHtmlView sqlserverPlan = new ShowplanSqlServerExternalHtmlView();
			return sqlserverPlan.createDestFile(xmlPlan);
		}
		else if (Type.ASE.equals(type))
		{
			System.out.println("ASE-XML-SHOWPLAN: \n" + xmlPlan);
			throw new RuntimeException("Sybase ASE Showplan HTML Viewer has not yet been implemented.");
//			ShowplanAseExternalHtmlView asePlan = new ShowplanAseExternalHtmlView();
//			asePlan.show(xmlPlan);
		}
		else
		{
			throw new IllegalArgumentException("Unknown Showplan type");
		}
	}

	/**
	 * show the HTML Entry in the default HTML Browser<br>
	 * But first, validate that the temp directory exists, and copy all resources/templates to the directory
	 *  
	 * @param xmlPlan The XML ShowPlan content (note: not a file, but the actual XML Content)
	 */
	protected File createDestFile(String xmlPlan)
	{
		try
		{
			validateTempDir();
			
			return createHtmlFile(xmlPlan);
		}
		catch(Exception e)
		{
			showError("Problems Creating a HTML Showplan", e);
			return null;
		}
	}

	/**
	 * @return The base path to where we should copy template files to and also copy the resulting HTML file 
	 */
	protected String getTmpShowplanPath()
	{
		return TMP_SHOWPLAN_PATH;
	}

	/**
	 * @return The name of the XSLT file we should use to translate XML into HTML
	 */
	protected abstract String getXsltFile();

	/**
	 * The template directory (in the current JAR) where various XSLT files and images are located 
	 * @return
	 */
	protected abstract String getTemplateJarDir();

	/**
	 * Validate that the Temp (template) directory exists<br>
	 * If the directory doesn't exists create it and copy all the desired files
	 * @throws IOException 
	 */
	protected void validateTempDir() 
	throws IOException
	{
		File tmpBaseDir  = new File(getTmpShowplanPath());
		File destDir     = new File(getTmpShowplanPath() + getTemplateJarDir());
		File distDir     = new File(getTmpShowplanPath() + getTemplateJarDir() + "/dist");
		File cssDir      = new File(getTmpShowplanPath() + getTemplateJarDir() + "/css");
		File qpCssFile   = new File(getTmpShowplanPath() + getTemplateJarDir() + "/css/qp.css");
		File qpJsFile    = new File(getTmpShowplanPath() + getTemplateJarDir() + "/dist/qp.js");
		File qpJsMinFile = new File(getTmpShowplanPath() + getTemplateJarDir() + "/dist/qp.min.js");

		boolean populate = false;
		
		// Create the BASE TEMP DIR
		if (tmpBaseDir.exists() && tmpBaseDir.isDirectory())
		{
			_logger.info("Already exists: BASE temp directory for DBMS HTML Showplan Viewer. Location '" + tmpBaseDir + "'.");
		}
		else
		{
			if ( ! tmpBaseDir.mkdir() )
				_logger.error("Failed to create BASE temp directory for DBMS HTML Showplan Viewer at Location '" + tmpBaseDir + "'.");

			_logger.info("Created BASE temp directory for DBMS HTML Showplan Viewer at Location '" + tmpBaseDir + "'.");
		}

		// Create the Product(SQL-Server|ASE) TEMP DIR
		if (destDir.exists() && destDir.isDirectory())
		{
			_logger.info("Already exists: temp directory for DBMS HTML Showplan Viewer. Location '" + destDir + "'.");
		}
		else
		{
			if ( ! destDir.mkdir() )
				_logger.error("Failed to create temp directory for DBMS HTML Showplan Viewer at Location '" + destDir + "'.");

			_logger.info("Created temp directory for DBMS HTML Showplan Viewer at Location '" + destDir + "'.");

			populate = true;
		}

		if ( ! distDir.exists() )
		{
			_logger.info("DBMS HTML Showplan Viewer '/dist' directory did NOT exist. Populating it again at Location '" + distDir + "'.");
			populate = true;
		}
		else
		{
			if ( ! qpJsFile.exists() )
			{
				_logger.info("DBMS HTML Showplan Viewer '/dist/qp.js' file did NOT exist. Populating it again at Location '" + distDir + "'.");
				populate = true;
			}
			if ( ! qpJsMinFile.exists() )
			{
				_logger.info("DBMS HTML Showplan Viewer '/dist/qp.min.js' file did NOT exist. Populating it again at Location '" + distDir + "'.");
				populate = true;
			}
		}

		if ( ! cssDir.exists() )
		{
			_logger.info("DBMS HTML Showplan Viewer '/css' directory did NOT exist. Populating it again at Location '" + cssDir + "'.");
			populate = true;
		}
		else
		{
			if ( ! qpCssFile.exists() )
			{
				_logger.info("DBMS HTML Showplan Viewer '/css/qp.css' file did NOT exist. Populating it again at Location '" + cssDir + "'.");
				populate = true;
			}
		}

		// If Dir is to old, recreate it...
		File checkDir = cssDir;
		if (checkDir.exists() && checkDir.isDirectory())
		{
			long ageInHours = System.currentTimeMillis() - checkDir.lastModified() / 1000 / 60 / 60;
			long ageInHoursLimit = 24 * 7;
			if (ageInHours >= ageInHoursLimit)
			{
				_logger.info("DBMS HTML Showplan Viewer. The Showplan dir is OLDER than " + ageInHoursLimit + " hours, Recreating it. Location '" + checkDir + "'.");
				populate = true;
			}
		}
		
		
		if (populate)
		{
			// Copy all desired files from the JAR file
			populateTempDir(tmpBaseDir);
		}

		
		// Delete the directories on exit
//		tmpBaseDir.deleteOnExit();
//		destDir.deleteOnExit();
	}

	/**
	 * Populates all the template file into the temp directory, this is probably called from validateTempDir()
	 * @param tempDir
	 * @throws IOException 
	 */
	protected void populateTempDir(File tempDir) 
	throws IOException
	{
		URL url = this.getClass().getResource(getTemplateJarDir());
		
   		final URLConnection urlConnection = url.openConnection();
   		_logger.debug("populateTempDir: urlConnection=" + urlConnection);
   		
   		if (urlConnection instanceof JarURLConnection) 
   		{
   			copyJarResourcesRecursively(tempDir, (JarURLConnection)urlConnection);
   		}
   		else
   		{
   			copyFilesRecusively(new File(url.getPath()), tempDir);
   		}
//		else
//		{
//			return FileUtils.copyFilesRecusively(new File(originUrl.getPath()), destination);
//		}
	}

	/**
	 * Copy templates from the JAR file
	 * @param destination
	 * @param jarConnection
	 * @throws IOException
	 */
	public void copyJarResourcesRecursively(File destination, JarURLConnection jarConnection) 
	throws IOException
	{
		_logger.debug("copyJarResourcesRecursively(): destination='" + destination + "'.");
		JarFile jarFile = jarConnection.getJarFile();
		for (final Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();)
		{
			final JarEntry entry = e.nextElement();
			if ( entry.getName().startsWith(jarConnection.getEntryName()) )
			{
				String remove = getParent(jarConnection.getEntryName());
				String fileName = StringUtils.removeStart(entry.getName(), remove);

				if ( !entry.isDirectory() )
				{
					InputStream      entryInputStream = null;
					FileOutputStream fileOutputStream = null;
					try
					{
						_logger.debug("COPY JAR entry: destination='" + destination + ", 'filename='" + fileName + "'. entry=" + entry);
						entryInputStream = jarFile.getInputStream(entry);
						fileOutputStream = new FileOutputStream(new File(destination, fileName));
//						FileUtils.copyStream(entryInputStream, fileOutputStream);
						IOUtils.copy(entryInputStream, fileOutputStream);
					}
					finally
					{
						FileUtils.safeClose(entryInputStream);
						FileUtils.safeClose(fileOutputStream);
					}
				}
				else
				{
					_logger.debug("CREATE DIR JAR entry: destination='" + destination + ", 'filename='" + fileName + "'. entry=" + entry);
					FileUtils.ensureDirectoryExists(new File(destination, fileName));
				}
			}
		}
	}
	/** simply strip of last entry */
	private String getParent(String entryName)
	{
		if (entryName == null)
			return null;

		if (entryName.endsWith("/"))
			entryName = entryName.substring(0, entryName.length() - 1);
		
		int lastSlash = entryName.lastIndexOf('/');
		if (lastSlash != -1)
			entryName = entryName.substring(0, lastSlash);
			
		return entryName;
	}

	/**
	 * Copy templates if we are NOT using a JAR file (that is during development)
	 * 
	 * @param srcFile
	 * @param destDir
	 * @return
	 * @throws IOException 
	 */
	public static boolean copyFilesRecusively(final File srcFile, final File destDir) 
	throws IOException
	{
		assert destDir.isDirectory();
		if ( !srcFile.isDirectory() )
		{
			org.apache.commons.io.FileUtils.copyFile( srcFile, new File(destDir, srcFile.getName()) );
		}
		else
		{
			final File newDestDir = new File(destDir, srcFile.getName());
			if ( !newDestDir.exists() && !newDestDir.mkdir() )
			{
				return false;
			}

			for (final File child : srcFile.listFiles())
			{
				if ( !copyFilesRecusively(child, newDestDir) )
				{
					return false;
				}
			}
		}

		return true;
	}

//	/**
//	 * Create a HTML file using the XML plan
//	 * 
//	 * @param xmlPlan
//	 * @return The file name created
//	 * @throws IOException 
//	 * @throws TransformerException 
//	 * @throws TransformerConfigurationException 
//	 * @throws Exception
//	 */
//	protected File createHtmlFile(String xmlPlan) 
//	throws IOException, TransformerConfigurationException, TransformerException 
//	{
//		File destDir = new File(getTmpShowplanPath() + getTemplateJarDir());
//		String xsltFile = getTmpShowplanPath() + getTemplateJarDir() + getXsltFile();
//
//		File outputHTML = File.createTempFile("showplan_", ".html", destDir);
//		outputHTML.deleteOnExit();
//
//		// TODO: This can be done better, maybe: 
//		//       - transform returns a string instead of the File (so we don't have to read the file and then write to it at once)
//		//       - or: write the SQL-Server showplan into a javascript variable, and then:
//		//         <div id="container"></div>
//		//         <script>
//		//             QP.showPlan(document.getElementById("container"), '<ShowPlanXML...');
//		//         </script> 
//		//         like the authur suggest on: https://github.com/JustinPealing/html-query-plan
//		transform(xmlPlan, xsltFile, outputHTML);
//		
//		String fileContent = FileUtils.readFile(outputHTML, null);
//		
//		// Write HTML file
//		BufferedWriter out = null;
//		try
//		{
//			FileWriter fstream = new FileWriter(outputHTML);
//			out = new BufferedWriter(fstream);
//			out.write("<html>                                                                    \n");
//			out.write("                                                                          \n");
//			out.write("<head>                                                                    \n");
//			out.write("    <META http-equiv='Content-Type' content='text/html; charset=UTF-8'>   \n");
//			out.write("    <title>Execution plan</title>                                         \n");
//			out.write("    <link rel='stylesheet' type='text/css' href='css/qp.css'>             \n");
//			out.write("    <script src='lib/qp.js' type='text/javascript'></script>              \n");
//			out.write("</head>                                                                   \n");
//			out.write("                                                                          \n");
//			out.write("<body onload=\"QP.drawLines(document.getElementById('container'));\">     \n");
//			out.write("<div id='container'>                                                      \n");
//
//			out.write(fileContent);
//			
//			out.write("</div>                                                                    \n");
//			out.write("</body>                                                                   \n");
//			out.write("                                                                          \n");
//			out.write("</html>                                                                   \n");
//		}
//		catch (IOException e)
//		{
//			throw e;
//		}
//		finally
//		{
//			if(out != null) 
//				out.close();
//		}
//
//		return outputHTML;
////		File destDir = new File(getTmpShowplanPath() + getJarDir());
////		String xsltFile = getTmpShowplanPath() + getTemplateJarDir() + getXsltFile();
////		try
////		{
////			File outputHTML = File.createTempFile("showplan_", ".html", destDir);
////			outputHTML.deleteOnExit();
////
////			transform(xmlPlan, xsltFile, outputHTML);
////
////			return outputHTML;
////		}
////		catch (TransformerConfigurationException e)
////		{
////			System.err.println("TransformerConfigurationException");
////			System.err.println(e);
////		}
////		catch (TransformerException e)
////		{
////			System.err.println("TransformerException");
////			System.err.println(e);
////		}
////		catch (IOException e)
////		{
////			System.err.println("IOException");
////			e.printStackTrace();
////		}
////		
////		return null;
//	}
	/**
	 * Create a HTML file using the XML plan
	 * 
	 * @param xmlPlan
	 * @return The file name created
	 * @throws IOException 
	 * @throws TransformerException 
	 * @throws TransformerConfigurationException 
	 * @throws Exception
	 */
	protected File createHtmlFile(String xmlPlan) 
	throws IOException, TransformerConfigurationException, TransformerException 
	{
		File destDir = new File(getTmpShowplanPath() + getTemplateJarDir());
		String xsltFile = getTmpShowplanPath() + getTemplateJarDir() + getXsltFile();

		File outputHTML = File.createTempFile("showplan_", ".html", destDir);
		outputHTML.deleteOnExit();

		// TODO: This can be done better, maybe: 
		//       - transform returns a string instead of the File (so we don't have to read the file and then write to it at once)
		//       - or: write the SQL-Server showplan into a javascript variable, and then:
		//         <div id="container"></div>
		//         <script>
		//             QP.showPlan(document.getElementById("container"), '<ShowPlanXML...');
		//         </script> 
		//         like the authur suggest on: https://github.com/JustinPealing/html-query-plan
//		transform(xmlPlan, xsltFile, outputHTML);
		
//		String fileContent = FileUtils.readFile(outputHTML, null);
		
		// Write HTML file
		BufferedWriter out = null;
		try
		{
			FileWriter fstream = new FileWriter(outputHTML);
			out = new BufferedWriter(fstream);
			out.write("<html>                                                                    \n");
			out.write("                                                                          \n");
			out.write("<head>                                                                    \n");
			out.write("    <META http-equiv='Content-Type' content='text/html; charset=UTF-8'>   \n");
			out.write("    <title>Execution plan</title>                                         \n");
			out.write("    <link rel='stylesheet' type='text/css' href='css/qp.css'>             \n");
//			out.write("    <script src='lib/qp.js' type='text/javascript'></script>              \n");
			out.write("    <script src='dist/qp.js' type='text/javascript'></script>             \n");
			out.write("    <script src='https://code.jquery.com/jquery-3.7.1.min.js' integrity='sha256-/JqT3SQfawRcv/BIHPThkBvs0OEvtFFmqPF/lYI/Cxo=' crossorigin='anonymous'></script>  \n");
			out.write("</head>                                                                   \n");
			out.write("                                                                          \n");
			out.write("<body>                                                                    \n");
			out.write("<h2>" + Version.getAppName() + " - Simple Showplan viewer</h2>            \n");
			out.write("The query plan is embedded in the HTML Source text...<br>                 \n");
			out.write("Feel free to copy the showplan xml and past it into some other tool...<br> \n");
			out.write("For example the online tool: <a target='_blank' href='http://www.supratimas.com/'>http://www.supratimas.com/</a><br> \n");
			out.write("<button onclick='copyShowplanToClipboard()'>Copy Showplan</button>        \n");
			out.write("<button onclick='downloadShowplan()'>Download Showplan</button>           \n");
			out.write("<br>                                                                      \n");
			out.write("<button onclick='copyShowplanFormattedToClipboard()'>Copy Showplan (formatted)</button> \n");
			out.write("<button onclick='downloadShowplanFormatted()'>Download Showplan (formatted)</button> \n");
			out.write("<div id='copy-done'></div>                                                \n");
			out.write("<br>                                                                      \n");
			out.write("<hr>                                                                      \n");
			out.write("<br>                                                                      \n");

			out.write("<div id='container'></div>                                                \n");

			// write the below to the file, note the backtick (`) on the "var showplan = ``" which is EcmaScript6 syntax to escape newlines, single/double quotes etc... 
			// <script>
			//     var showplanText = `<ShowPlanXML...`;
			//     QP.showPlan(document.getElementById("container"), showplanText);
			// </script> 

			out.write("<script>                                                                  \n");
			out.write("    var showplanText = `"); // note the backtick char
			out.write(xmlPlan);
			out.write("`;\n");                     // note the backtick char
			out.write("    QP.showPlan(document.getElementById('container'), showplanText);      \n");

//			out.write("                                                                          \n");
//			out.write("function copyShowplanToClipboard()                                        \n");
//			out.write("{                                                                         \n");
//			out.write("    window.prompt('Copy to clipboard: Ctrl+C, Enter', showplanText);      \n");
//			out.write("}                                                                         \n");
			
			out.write("                                                                          \n");
			out.write("function formatXml(xml) {                                                 \n");
			out.write("    var formatted = '';                                                   \n");
			out.write("    var reg = /(>)(<)(\\/*)/g;                                            \n");
			out.write("    xml = xml.replace(reg, '$1\\r\\n$2$3');                               \n");
			out.write("    var pad = 0;                                                          \n");
			out.write("    jQuery.each(xml.split('\\r\\n'), function(index, node) {              \n");
			out.write("        var indent = 0;                                                   \n");
			out.write("        if (node.match( /.+<\\/\\w[^>]*>$/ )) {                           \n");
			out.write("            indent = 0;                                                   \n");
			out.write("        } else if (node.match( /^<\\/\\w/ )) {                            \n");
			out.write("            if (pad != 0) {                                               \n");
			out.write("                pad -= 1;                                                 \n");
			out.write("            }                                                             \n");
			out.write("        } else if (node.match( /^<\\w[^>]*[^\\/]>.*$/ )) {                \n");
			out.write("            indent = 1;                                                   \n");
			out.write("        } else {                                                          \n");
			out.write("            indent = 0;                                                   \n");
			out.write("        }                                                                 \n");
			out.write("                                                                          \n");
			out.write("        var padding = '';                                                 \n");
			out.write("        for (var i = 0; i < pad; i++) {                                   \n");
			out.write("            padding += '  ';                                              \n");
			out.write("        }                                                                 \n");
			out.write("                                                                          \n");
			out.write("        formatted += padding + node + '\\r\\n';                           \n");
			out.write("        pad += indent;                                                    \n");
			out.write("    });                                                                   \n");
			out.write("                                                                          \n");
			out.write("    return formatted;                                                     \n");
			out.write("}                                                                         \n");
			out.write("                                                                          \n");

			// get Query/Plan Hash from XML:   QueryHash="0xE0245C01AFBDBF7E" QueryPlanHash="0xE984CF727B712DF9"
			out.write("                                                                          \n");
			out.write("function getFileNameFromXmlPlan(xml)                                      \n");
			out.write("{                                                                         \n");
			out.write("    let filename = 'unknown';                                             \n");
			out.write("    try                                                                   \n");
			out.write("    {                                                                     \n");
			out.write("        let startPos      = -1;                                           \n");
			out.write("        let endPos        = -1;                                           \n");
			out.write("        let queryHash     = '';                                           \n");
			out.write("        let queryPlanHash = '';                                           \n");
			out.write("                                                                          \n");
			out.write("        startPos = xml.indexOf('QueryHash=\"0x');                         \n");
			out.write("        endPos   = xml.indexOf(' ', startPos);                            \n");
			out.write("        if (startPos != -1 && endPos != -1)                               \n");
			out.write("        {                                                                 \n");
			out.write("            queryHash = xml.substring(startPos, endPos).replaceAll('\"', '').replace('QueryHash=', '');  \n");
			out.write("        }                                                                 \n");
			out.write("                                                                          \n");
			out.write("        startPos = xml.indexOf('QueryPlanHash=\"0x');                     \n");
			out.write("        endPos   = xml.indexOf(' ', startPos);                            \n");
			out.write("        if (startPos != -1 && endPos != -1)                               \n");
			out.write("        {                                                                 \n");
			out.write("            queryPlanHash = xml.substring(startPos, endPos).replaceAll('\"', '').replace('QueryPlanHash=', '');  \n");
			out.write("        }                                                                 \n");
			out.write("                                                                          \n");
			out.write("        if (queryHash.length > 0 && queryPlanHash.length > 0)             \n");
			out.write("        {                                                                 \n");
			out.write("            filename = 'QueryHash=' + queryHash + '__QueryPlanHash=' + queryPlanHash; \n");
			out.write("        }                                                                 \n");
			out.write("                                                                          \n");
//			out.write("        let xmlDoc   = $.parseXML(xml);                                   \n");
//			out.write("                                                                          \n");
//			out.write("        let queryHash        = '';                                        \n");
//			out.write("        let queryHashArr     = [];                                        \n");
//			out.write("        let queryPlanHash    = '';                                        \n");
//			out.write("        let queryPlanHashArr = [];                                        \n");
//			out.write("        $(xmlDoc).find('StmtSimple').each(function(i,e) {                 \n");
//			out.write("            queryHashArr    .push($(e).attr('QueryHash'));                \n");
//			out.write("            queryPlanHashArr.push($(e).attr('QueryPlanHash'));            \n");
//			out.write("        });                                                               \n");
//			out.write("                                                                          \n");
//			out.write("        if (queryPlanHashArr.length > 0)                                  \n");
//			out.write("        {                                                                 \n");
//			out.write("            queryPlanHash = queryPlanHashArr[0];                          \n");
//			out.write("                                                                          \n");
//			out.write("            if (queryHashArr.length > 0)                                  \n");
//			out.write("            {                                                             \n");
//			out.write("                queryHash = queryHashArr[0];                              \n");
//			out.write("            }                                                             \n");
//			out.write("        }                                                                 \n");
//			out.write("                                                                          \n");
//			out.write("        filename = 'QueryHash=' + queryHash + '__QueryPlanHash=' + queryPlanHash; \n");
			out.write("    }                                                                     \n");
			out.write("    catch (error)                                                         \n");
			out.write("    {                                                                     \n");
			out.write("        console.error(error);                                             \n");
			out.write("    }                                                                     \n");
			out.write("                                                                          \n");
			out.write("    return filename;                                                      \n");
			out.write("}                                                                         \n");
			out.write("                                                                          \n");

			out.write("function copyShowplanToClipboard()                                        \n");
			out.write("{                                                                         \n");
			out.write("    var copyFeedback = document.getElementById('copy-done');              \n");
			out.write("                                                                          \n");
			out.write("    // Set feedback                                                       \n");
			out.write("    copyFeedback.innerHTML     = 'The Query Plan was copied to Clipboard';\n");
			out.write("    copyFeedback.style.display = 'block';                                 \n");
			out.write("                                                                          \n");
			out.write("    // Copy to clipboard                                                  \n");
			out.write("    copyStringToClipboard(showplanText);                                  \n");
			out.write("                                                                          \n");
			out.write("    // Hide feedback                                                      \n");
			out.write("    setTimeout( function(){                                               \n");
			out.write("        copyFeedback.style.display = 'none';                              \n");
			out.write("    }, 3000);                                                             \n");
			out.write("}                                                                         \n");
			out.write("                                                                          \n");

			out.write("function copyShowplanFormattedToClipboard()                               \n");
			out.write("{                                                                         \n");
			out.write("    var copyFeedback = document.getElementById('copy-done');              \n");
			out.write("                                                                          \n");
			out.write("    // Set feedback                                                       \n");
			out.write("    copyFeedback.innerHTML     = 'The Query Plan was copied to Clipboard';\n");
			out.write("    copyFeedback.style.display = 'block';                                 \n");
			out.write("                                                                          \n");
			out.write("    // Copy to clipboard                                                  \n");
			out.write("    copyStringToClipboard(formatXml(showplanText));                       \n");
			out.write("                                                                          \n");
			out.write("    // Hide feedback                                                      \n");
			out.write("    setTimeout( function(){                                               \n");
			out.write("        copyFeedback.style.display = 'none';                              \n");
			out.write("    }, 3000);                                                             \n");
			out.write("}                                                                         \n");
			out.write("                                                                          \n");

			out.write("function copyStringToClipboard (string) {                                 \n");
			out.write("    function handler (event)                                              \n");
			out.write("    {                                                                     \n");
			out.write("        event.clipboardData.setData('text/plain', string);                \n");
			out.write("        event.preventDefault();                                           \n");
			out.write("        document.removeEventListener('copy', handler, true);              \n");
			out.write("    }                                                                     \n");
			out.write("                                                                          \n");
			out.write("    document.addEventListener('copy', handler, true);                     \n");
			out.write("    document.execCommand('copy');                                         \n");
			out.write("}                                                                         \n");

			out.write("                                                                          \n");
			out.write("function downloadShowplan()                                               \n");
			out.write("{                                                                         \n");
			out.write("    var textToSave = showplanText;                                        \n");
			out.write("    var hiddenElement = document.createElement('a');                      \n");
			out.write("                                                                          \n");
			out.write("    hiddenElement.href     = 'data:attachment/text,' + encodeURI(textToSave); \n");
			out.write("    hiddenElement.target   = '_blank';                                    \n");
//			out.write("    hiddenElement.download = 'showplan_xxx.xml'                           \n");
			out.write("    hiddenElement.download = 'showplan__' + getFileNameFromXmlPlan(showplanText) + '.xml.sqlplan' \n");
			out.write("    hiddenElement.click();                                                \n");
			out.write("}                                                                         \n"); 

			out.write("                                                                          \n");
			out.write("function downloadShowplanFormatted()                                      \n");
			out.write("{                                                                         \n");
			out.write("    var textToSave = formatXml(showplanText);                             \n");
			out.write("    var hiddenElement = document.createElement('a');                      \n");
			out.write("                                                                          \n");
			out.write("    hiddenElement.href     = 'data:attachment/text,' + encodeURI(textToSave); \n");
			out.write("    hiddenElement.target   = '_blank';                                    \n");
//			out.write("    hiddenElement.download = 'showplan_xxx.xml'                           \n");
			out.write("    hiddenElement.download = 'showplan__' + getFileNameFromXmlPlan(showplanText) + '.xml.sqlplan' \n");
			out.write("    hiddenElement.click();                                                \n");
			out.write("}                                                                         \n"); 
			out.write("</script>                                                                 \n");
			out.write("                                                                          \n");
			out.write("</body>                                                                   \n");
			out.write("                                                                          \n");
			out.write("</html>                                                                   \n");
		}
		catch (IOException e)
		{
			throw e;
		}
		finally
		{
			if(out != null) 
				out.close();
		}

		return outputHTML;
	}


	/**
	 * Transform XML into HTML
	 * 
	 * @param dataXML
	 * @param inputXSL
	 * @param outputHTML
	 * @throws TransformerConfigurationException
	 * @throws TransformerException
	 */
	protected void transform(String dataXML, String inputXSL, File outputHTML) 
	throws TransformerConfigurationException, TransformerException
	{
		TransformerFactory factory = TransformerFactory.newInstance();

		StreamSource xslStream = new StreamSource(inputXSL);
		Transformer transformer = factory.newTransformer(xslStream);

//		StreamSource in = new StreamSource(dataXML);
		StreamSource in = new StreamSource(new StringReader(dataXML));
		StreamResult out = new StreamResult(outputHTML);

		transformer.transform(in, out);
//		System.out.println("The generated HTML file is:" + outputHTML);
	}

	/**
	 * Open the default external HTML Browser
	 * @param filename
	 */
	protected void viewHtmlFile(File filename)
	{
		if (Desktop.isDesktopSupported())
		{
			Desktop desktop = Desktop.getDesktop();
			if ( desktop.isSupported(Desktop.Action.BROWSE) )
			{
				try
				{
					desktop.browse(filename.toURI());
				}
				catch (Exception ex)
				{
					showError("Problems when open the URL '" + filename + "'. Caught: " + ex, ex);
				}
			}
		}
	}

	/** Show dialog box with error message. */
//	protected void showError(String errorMessage, Exception e)
//	{
//		if (e != null)
//		{
//			// Exception To String
//			StringWriter sw = new StringWriter();
//			PrintWriter pw = new PrintWriter(sw);
//			e.printStackTrace(pw);
//			String stackTrace = sw.toString();
//
//			errorMessage += "\n\n" + stackTrace;
//		}
//
//		JOptionPane.showMessageDialog(null, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
//	}
	protected void showError(String errorMessage, Exception e)
	{
		SwingUtils.showErrorMessage(null, "Problems HTML Showplan", errorMessage, e);
	}





	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);
		

		String xmlPlan = 
//			"<?xml version=\"1.1\" encoding=\"UTF-8\"?> \n" +
            "<ShowPlanXML Build=\"12.0.4213.0\" Version=\"1.2\" xmlns=\"http://schemas.microsoft.com/sqlserver/2004/07/showplan\"> \n" +
            "    <BatchSequence> \n" +
            "        <Batch> \n" +
            "            <Statements> \n" +
            "                <StmtSimple RetrievedFromCache=\"false\" StatementCompId=\"1\" StatementId=\"1\" StatementText=\"select getdate()\" StatementType=\"SELECT WITHOUT QUERY\"/> \n" +
            "            </Statements> \n" +
            "        </Batch> \n" +
            "    </BatchSequence> \n" +
            "</ShowPlanXML> \n" +
            "";

		ShowplanHtmlView.show(Type.SQLSERVER, xmlPlan);
		try {Thread.sleep(20*1000);} catch(InterruptedException ignore) {} // To make the file stay fore a few seconds...
	}
}

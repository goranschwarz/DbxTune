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
package com.dbxtune.sql.showplan;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.utils.FileUtils;
import com.dbxtune.utils.SwingUtils;

public abstract class ShowplanHtmlView
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

//	private static final String _baseTempPath = System.getProperty("java.io.tmpdir");
	public static final String TMP_SHOWPLAN_PATH = System.getProperty("java.io.tmpdir") + File.separator + "showplan" + File.separator;
	
	public enum Type
	{
		POSTGRES,   // Showplan for Postgres
		SQLSERVER,  // Showplan for SQL Server 
		ASE         // Showplan for Sybase ASE
	};

	public static void show(Type type, String plantext)
	{
		if (Type.SQLSERVER.equals(type))
		{
			ShowplanSqlServerExternalHtmlView sqlserverPlan = new ShowplanSqlServerExternalHtmlView();
			sqlserverPlan.show(plantext);
		}
		else if (Type.POSTGRES.equals(type))
		{
			ShowplanPostgresExternalHtmlView postgresPlan = new ShowplanPostgresExternalHtmlView();
			postgresPlan.show(plantext);
		}
		else if (Type.ASE.equals(type))
		{
			System.out.println("ASE-XML-SHOWPLAN: \n" + plantext);
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
	protected void show(String plantext)
	{
		try
		{
			validateTempDir();
			
			File filename = createHtmlFile(plantext);
			if (filename != null)
				viewHtmlFile(filename);
		}
		catch(Exception e)
		{
			showError("Problems Creating a HTML Showplan", e);
		}
	}

	public static File createHtmlFile(Type type, String plantext)
	{
		if (Type.SQLSERVER.equals(type))
		{
			ShowplanSqlServerExternalHtmlView sqlserverPlan = new ShowplanSqlServerExternalHtmlView();
			return sqlserverPlan.createDestFile(plantext);
		}
		else if (Type.POSTGRES.equals(type))
		{
			ShowplanPostgresExternalHtmlView postgresPlan = new ShowplanPostgresExternalHtmlView();
			return postgresPlan.createDestFile(plantext);
		}
		else if (Type.ASE.equals(type))
		{
			System.out.println("ASE-XML-SHOWPLAN: \n" + plantext);
			throw new RuntimeException("Sybase ASE Showplan HTML Viewer has not yet been implemented.");
//			ShowplanAseExternalHtmlView asePlan = new ShowplanAseExternalHtmlView();
//			asePlan.show(plantext);
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
	 * @param plantext The XML ShowPlan content (note: not a file, but the actual XML Content)
	 */
	protected File createDestFile(String plantext)
	{
		try
		{
			validateTempDir();
			
			return createHtmlFile(plantext);
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

	
	/**
	 * Create a HTML file using the plan text
	 * 
	 * @param planText
	 * @return The file name created
	 * 
	 * @throws IOException 
	 * @throws TransformerException 
	 * @throws TransformerConfigurationException 
	 * @throws Exception
	 */
	protected abstract File createHtmlFile(String planText)
	throws IOException, TransformerConfigurationException, TransformerException;

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
		// Set Log4j Log Level
//		Configurator.setRootLevel(Level.TRACE);
		

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

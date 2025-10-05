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
package com.dbxtune.utils;

import java.io.File;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.h2.engine.SysProperties;

public class H2UrlHelper
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static String _h2UrlStart = "jdbc:h2:";

	private String _urlType   = "file";

	private String _tcpHost   = null;
	private int    _tcpPort   = -1;

	private String _originUrl         = null;
	private String _urlOptions        = null;
	private String _rawFileName       = null;
	private String _extractedFileName = null;

	private Map<String, String> _urlOptionsMap = new LinkedHashMap<String, String>();
	
	public H2UrlHelper(String url)
	{
		_originUrl = url;
		parse(url);
	}

	private void parse(String url)
	{
		if (url == null)
			return;

		if ( ! url.startsWith(_h2UrlStart) )
			return;

		_urlType = "file";

		// get everything beyond jdbc:h2:
		String urlVal  = url.substring(_h2UrlStart.length());

		// Strip of any options
		if (urlVal.indexOf(';') >= 0)
		{
			int index = urlVal.indexOf(';');
			_urlOptions = urlVal.substring(index);
			_urlOptionsMap = StringUtil.parseCommaStrToMap(_urlOptions, "=", ";");

			urlVal = urlVal.substring(0, index);
		}
		
		// Strip of any prefix
		if (urlVal.startsWith("file:"))
		{
			_urlType = "file";
			urlVal = urlVal.substring("file:".length());
		}
		else if (urlVal.startsWith("mem:"))
		{
			_urlType = "mem";
			urlVal = urlVal.substring("mem:".length());
		}
		else if (urlVal.startsWith("tcp:"))
		{
			_urlType = "tcp";
			urlVal = urlVal.substring("tcp:".length());
		}
		else if (urlVal.startsWith("ssl:"))
		{
			_urlType = "ssl";
			urlVal = urlVal.substring("ssl:".length());
		}
		else if (urlVal.startsWith("zip:"))
		{
			_urlType = "zip";
			urlVal = urlVal.substring("zip:".length());
		}

		// for TCP/SSL get host/port
		if (_urlType.equals("tcp") || _urlType.equals("ssl"))
		{
			String cleanUri = urlVal.replace('\\', '/');
			try
			{
				URI uri = URI.create(cleanUri);

				//System.out.println("H2UrlHelper.parse(url='"+url+"'): cleanURI='"+cleanUri+"', uri.getScheme()='"+uri.getScheme()+"', uri.getHost()='"+uri.getHost()+"', uri.getPort()='"+uri.getPort()+"'.");
				
				_tcpHost = uri.getHost();
				_tcpPort = uri.getPort();

				//p.setHostPortStr( null ); // getHostPortStr(): if _hostPortStr==null -> getHost() + ":" + getPort()
			}
			catch (Throwable ex)
			{
				//System.out.println("H2UrlHelper: Problem parsing the GENERIC URL '"+url+"', cleanUri '"+cleanUri+"'. Caught: "+ex);
			}
		}

		_rawFileName = urlVal.trim();

		// FIX template stuff
		if ("file".equals(_urlType))
		{
			urlVal = urlVal.replace("[<path>]", "");
			urlVal = urlVal.replace("<dbname>", "");
		}

		// replace ~, with the HOME directory
		if (urlVal.startsWith("~"))
		{
			String homeDir = SysProperties.USER_HOME;
//			urlVal = homeDir + SysProperties.FILE_SEPARATOR + urlVal.substring(1);
			urlVal = homeDir + File.separator + urlVal.substring(1);
		}

		_extractedFileName = urlVal.trim();

//		if (urlVal.trim().equals(""))
//		{
//			if (defaultValue == null)
//				return null;
//			return new File(defaultValue);
//		}
//
//		return new File(urlVal);
	}

	/**
	 * Get the origin URL passed into this object
	 * @return
	 */
	public String getOriginUrl()
	{
		return _originUrl;
	}

	/**
	 * Get the a "new" URL Based on all sub components
	 * @return
	 */
	public String getUrl()
	{
		String urlOptions = getUrlOptions();
		if (urlOptions == null)
			urlOptions = "";

		return _h2UrlStart + _urlType + ":" + _rawFileName + urlOptions;
	}

	/**
	 * returns true if the URL is of type "file"
	 * @return
	 */
	public boolean isUrlTypeFile()
	{
		return "file".equals(_urlType);
	}

	/**
	 * returns type "file, mem, tcp, ssl, zip"
	 * @return
	 */
	public String getUrlType()
	{
		return _urlType;
	}

	public String getUrlTcpHostPort()
	{
		//p.setHostPortStr( null ); // getHostPortStr(): if _hostPortStr==null -> getHost() + ":" + getPort()

		if (StringUtil.isNullOrBlank(_tcpHost))
			return null;

		if (_tcpPort != -1)
			return _tcpHost + ":" + _tcpPort;

		return _tcpHost;
	}

	public String getUrlTcpHost()
	{
		return _tcpHost;
	}

	public int getUrlTcpPort()
	{
		if (StringUtil.isNullOrBlank(_tcpHost))
			return -1;

		if (_tcpPort == -1)
			return 9092;

		return _tcpPort;
	}

	/**
	 * Get URL Options as a string
	 * @return
	 */
	public String getUrlOptions()
	{
		return _urlOptions;
	}

	/**
	 * Get URL Options
	 * @return
	 */
	public Map<String, String> getUrlOptionsMap()
	{
		return _urlOptionsMap;
	}

	/**
	 * Set URL Options
	 * @return
	 */
	public void setUrlOptionsMap(Map<String, String> urlOptionsMap)
	{
		_urlOptionsMap = urlOptionsMap;

		String urlOptions = "";
		if (_urlOptionsMap != null && _urlOptionsMap.size() > 0)
		{
			for (Map.Entry<String,String> entry : _urlOptionsMap.entrySet()) 
			{
				String key = entry.getKey();
				String val = entry.getValue();

				urlOptions += ";" + key + "=" + val;
			}
		}
		if (StringUtil.isNullOrBlank(urlOptions))
			_urlOptions = null;
		else
			_urlOptions = urlOptions;
	}

	public void removeUrlOption(String urlOption)
	{
		// Get current map
		Map<String, String> urlMap = getUrlOptionsMap();

		// Remove the option
		urlMap.remove(urlOption);
		
		// This rebuilds the URL String
		setUrlOptionsMap(urlMap);
	}
	
	/**
	 * Get the Filename part passed in the URL.<br>
	 * The format is RAW, meaning ~ is not translated into $HOME, and PATH delimiters is not changed.
	 * @return
	 */
	public String getRawFileString()
	{
		return _rawFileName;
	}

	/**
	 * Get a File object of the parsed file part of the URL
	 * @return
	 */
	public File getFile()
	{
		return (_extractedFileName == null) ? null : new File(_extractedFileName);
	}

	/**
	 * Get name of the parsed file part of the URL<br>
	 * This is the full path, if a full path was specified in the url
	 * null if no file was passed.
	 * @return
	 */
	public String getFilename()
	{
		return _extractedFileName;
	}

	/**
	 * Get FULL name of the parsed file part of the URL<br>
	 * This is the full path including file <b>extention</b>
	 * 
	 * @return null if no file was passed or the file didn't exist.
	 */
	public File getDbFile()
	{
		File f = getFile();
		if (f == null)
			return null;

		if (f.exists())
			return f;

		f = new File(f.getAbsolutePath() + ".mv.db");
		if (f.exists())
			return f;
		
		return null;
	}

	/**
	 * Get FULL name of the parsed file part of the URL<br>
	 * This is the full path including file <b>extention</b>
	 * 
	 * @return null if no file was passed or the file didn't exist.
	 */
	public File getDbFile(boolean returnFileEvenIfTheFileNotYetExists)
	{
		File f = getFile();
		if (f == null)
			return null;

		if (f.exists())
			return f;

		f = new File(f.getAbsolutePath() + ".mv.db");
		if (f.exists())
			return f;
		
		return returnFileEvenIfTheFileNotYetExists ? f : null;
	}

//	/**
//	 * Set file name part of the URL<br>
//	 * This is the full path (or simply the filename, but then it will use the relative path)
//	 */
//	public void setFilename(String filename)
//	{
//		_extractedFileName = filename;
//	}

	/**
	 * Get a File object of what directory the parsed database file exists in 
	 * @return (null if no db file was found after extracting the templates)
	 */
	public File getDir()
	{
		return getDir(null);
	}

	/**
	 * Get a File object of what directory the parsed database file exists in 
	 * @param defaultDir if no db file was found after extracting the templates, this is the default directory returned
	 * @return 
	 */
	public File getDir(String defaultDir)
	{
//		if ("".equals(_extractedFileName))
		if (StringUtil.isNullOrBlank(_extractedFileName))
		{
			if (defaultDir != null)
				return new File(defaultDir);
			return null;
		}
		
		File f = new File(_extractedFileName);
		if ( ! f.isDirectory() )
			f = f.getParentFile();
		return f;
	}

	/**
	 * Replace current database file in the current URL with a new file
	 * <p>
	 * If Templates exists in current URL those will be replaced
	 * 
	 * @param newDbFile The new database file
	 * @return The new URL
	 */
	public String getNewUrl(String newDbFile)
	{
		// REPLACE VARIOUS SUFFIXES
		// ------------------------------------

		// Take away db suffix. ".h2.db"
		if (newDbFile.indexOf(".h2.db") >= 0)
			newDbFile = newDbFile.replace(".h2.db", "");

		// Take away db suffix. ".mv.db"
		if (newDbFile.indexOf(".mv.db") >= 0)
			newDbFile = newDbFile.replace(".mv.db", "");

		// Take away db suffix. ".trace.db"
		if (newDbFile.indexOf(".trace.db") >= 0)
			newDbFile = newDbFile.replace(".trace.db", "");

		// Take away db suffix. ".data.db"
		if (newDbFile.indexOf(".data.db") >= 0)
			newDbFile = newDbFile.replace(".data.db", "");

		// Take away index suffix. ".index.db"
		if (newDbFile.indexOf(".index.db") >= 0)
			newDbFile = newDbFile.replace(".index.db", "");

		// Take away log suffix. ".99.log.db"
		if (newDbFile.matches(".*\\.[0-9]*\\.log\\.db.*"))
			newDbFile = newDbFile.replaceAll("\\.[0-9]*\\.log\\.db", "");

		
		String url = _originUrl;

		// REPLACE VARIOUS TEMPLATES
		// ------------------------------------
		if (url.startsWith("jdbc:h2:zip:") || newDbFile.toLowerCase().endsWith(".zip"))
		{
			File f = new File(newDbFile);
			String dbname = f.getName();
			if (dbname.toLowerCase().endsWith(".zip"))
				dbname = dbname.substring(0, dbname.length()-".zip".length());
			else
				dbname = "offlineDb";

			// If it's a ZIP file, but the URL isn't ZIP, the construct a total new URL 
			if ( ! url.startsWith("jdbc:h2:zip:") && newDbFile.toLowerCase().endsWith(".zip") )
			{
				url = "jdbc:h2:zip:"+newDbFile+"!/"+dbname;
			}
			else
			{	
				// fill in the template
				if ( url.matches(".*<zipFileName>!/<dbname>.*") )
				{
					url = url.replaceFirst("<zipFileName>", newDbFile);
					url = url.replaceFirst("<dbname>", dbname);
				}
				else
				{
					// Replace old file with the new file
					url = url.replace(_rawFileName, newDbFile);
				}
			}
		}
		else
		{
			if (url.indexOf("[<path>]<dbname>") >= 0)
			{
				url = url.replace("[<path>]<dbname>", newDbFile);
			}
			else
			{
				// Replace old file with the new file
				url = url.replace(_rawFileName, newDbFile);
			}
		}
		
		return url;
	}

	public static void main(String[] args)
	{
		// Set Log4j Log Level
//		Configurator.setRootLevel(Level.TRACE);

		test("jdbc:h2:file:C:/projects/dbxtune_recordings_temp/spam_prod_b_2014-09-23.15;IFEXISTS=TRUE;DATABASE_TO_UPPER=false;AUTO_SERVER=TRUE");
		test("jdbc:h2:tcp://192.168.0.112/spam_prod_b_2014-09-23.15");
		test("jdbc:h2:tcp://192.168.0.112/spam_prod_b_2014-09-23.15;IFEXISTS=TRUE;DATABASE_TO_UPPER=false;AUTO_SERVER=TRUE");
		test("jdbc:h2:tcp://192.168.0.111:9092/spam_prod_b_2014-09-23.15;IFEXISTS=TRUE;DATABASE_TO_UPPER=false;AUTO_SERVER=TRUE");
	}
	
	private static void test(String url)
	{
		H2UrlHelper h = new H2UrlHelper(url);
		System.out.println("");
		System.out.println("########################################################");
		System.out.println(" URL="+url);
		System.out.println(" toString="+h);
	}
}

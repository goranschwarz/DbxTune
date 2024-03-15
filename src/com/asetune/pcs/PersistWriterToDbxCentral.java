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
package com.asetune.pcs;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.InputValidator;
import com.asetune.cm.CmSettingsHelper.Type;
import com.asetune.cm.CmSettingsHelper.ValidationException;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class PersistWriterToDbxCentral 
extends PersistWriterToHttpJson
//implements IPersistWriter
{
//NOTE;// This needs major rewrite if we want to rename it to "PersistWriterToDbxCentral"
// Needed actions 
//  * move the toJson() to PersistContainer (or somewhere else, so we it can be reused from "PersistWriterToHttpJson"
//  *
// The idea with the "move" from "PersistWriterToHttpJson" to "PersistWriterToDbxCentral"
// * Give it a more specific name so we can do more "stuff" in here, like:
//   * Write to HTTP or FILE (or both) and possibly other "localizations" later on...

	private static Logger _logger = Logger.getLogger(PersistWriterToDbxCentral.class);

	// What writer type should we use
	private WriterType _writerType = null;
	
	private HttpConfigSlot _httpConfSlot = null; // Found in PersistWriterToHttpJson
	private FileConfigSlot _fileConfSlot = null;

	/** Hold configurations for "local directory/file" destination */
	private class FileConfigSlot
	extends HttpConfigSlot
	{
		private String _dirName;

		// Below is found in HttpConfigSlot
//		boolean _sendAlarmExtDescAsHtml   = DEFAULT_sendAlarmExtDescAsHtml;
//		SendCountersConfig _sendCounters = new SendCountersConfig(false);
//		SendCountersConfig _sendGraphs   = new SendCountersConfig(true);
		
	}


	@Override
	public void saveSample(PersistContainer cont)
	{
		// Implement this (split super stuff into more methods/modules so we can reuse much of the SUPER code)
		String jsonStr = "";

		//------------------------------------------------------------
		// Send to the HTTP config
		//------------------------------------------------------------
		if (_httpConfSlot != null)
		{
			try
			{
				// First send old entries in the error queue (no exception is thrown)
				_httpConfSlot.sendErrorQueue(cont);

				// Construct and send JSON
				jsonStr = toJson(cont, _httpConfSlot);
				sendMessage(jsonStr, _httpConfSlot);
			}
			catch (ConnectException ex) 
			{
				_logger.error("Problems connecting/sending JSON-REST call to '" + _httpConfSlot._url + "'. The entry will be saved in the 'error-queue' and sent later. Caught: " + ex);
				_httpConfSlot.addToErrorQueue(cont, jsonStr);
				
				// if destination has been down for some time: Send Alarm ?
				checkSendAlarm(_httpConfSlot);
			}
			catch (Exception ex)
			{
				_logger.error("Problems creating JSON or sending REST call to '" + _httpConfSlot._url + "'. Caught: " + ex , ex);
			}
		}

		//------------------------------------------------------------
		// Save the JSON file (which will be picked up by DbxCentral, if both Collector and DbxCentral is on same host
		//------------------------------------------------------------
		if (_fileConfSlot != null)
		{
			String destDirName = _fileConfSlot._dirName;

			// If dir do NOT exist, create the dir
			File dir = new File(destDirName);
			if ( ! dir.exists() )
			{
				if ( dir.mkdirs() )
				{
					_logger.info("Created the OUTPUT directory '" + destDirName + "' for '" + this.getClass().getSimpleName() + "'.");
				}
			}

			// init some helper variables
			String ts  = new SimpleDateFormat("yyyy-MM-dd.HH_mm_ss_SSS").format( new Date(System.currentTimeMillis()) );
			String srv = cont.getServerNameOrAlias();

			String tmpFileName  = "PersistWriterToDbxCentral." + ts + "." + srv + ".json.tmp";
			String destFileName = "PersistWriterToDbxCentral." + ts + "." + srv + ".json";
			File   tmpFile  = new File(destDirName, tmpFileName);
			File   destFile = new File(destDirName, destFileName);

			try
			{
				// Construct and send JSON
				jsonStr = toJson(cont, _fileConfSlot);

				// Save to FILE (save it to a "temporary name", and at the end move the file to the REAL Destination file)
				//               this so that any readers don't pick up the file before it's fully written
				FileUtils.write(tmpFile, jsonStr, StandardCharsets.UTF_8);

				// Move file to destination
				tmpFile.renameTo(destFile);
			}
			catch (IOException ex)
			{
				_logger.error("Problems creating JSON or saving it to FILE '" + tmpFile + "'. Caught: " + ex , ex);
			}
		}
	}

	@Override
	public void printConfig()
	{
		int spaces = 55;
		_logger.info("Configuration for Persist Writer Module: " + getName());

		// Writer TYPE
		_logger.info("    " + StringUtil.left(key(PROPKEY_writerType, ""), spaces) + ": " + _writerType);
		
		if (_httpConfSlot != null)
		{
			printConfig(_httpConfSlot);
		}

		if (_fileConfSlot != null)
		{
			_logger.info("    " + StringUtil.left(key(PROPKEY_writerTypeFileDir     , ""), spaces) + ": " + _fileConfSlot._dirName);
			_logger.info("    " + StringUtil.left(key(PROPKEY_sendAlarmExtDescAsHtml, ""), spaces) + ": " + _fileConfSlot._sendAlarmExtDescAsHtml);
			_logger.info("    " + StringUtil.left(key(PROPKEY_sendCounters          , ""), spaces) + ": " + _fileConfSlot._sendCounters);
			_logger.info("    " + StringUtil.left(key(PROPKEY_sendGraphs            , ""), spaces) + ": " + _fileConfSlot._sendGraphs);
		}

	}

	@Override
	public void init(Configuration conf) throws Exception
	{
//System.out.println(getName() + ": INIT.....................................");

		_logger.info("Initializing the PersistWriter component named '" + getName() + "'.");

		// Get what TYPE of writer "method" we should use
		_writerType = WriterType.fromString( conf.getProperty(PROPKEY_writerType, DEFAULT_writerType) );
		
		//--------------------------------------------
		// Read "http" configurations
		//--------------------------------------------
		if (WriterType.HTTP.equals(_writerType))
		{
			_httpConfSlot = new HttpConfigSlot();
			_httpConfSlot._url     = conf.getProperty(key(PROPKEY_url), DEFAULT_url);
			_httpConfSlot._cfgName = "DbxCentral";
    
			// Most is done in the super class: PersistWriterToHttpJson
			_httpConfSlot = initConfigSlot(conf, "DbxCentral", _httpConfSlot);
		}
		
		//--------------------------------------------
		// Read "file" configurations
		//--------------------------------------------
		if (WriterType.FILE.equals(_writerType))
		{
			_fileConfSlot = new FileConfigSlot();

			// Using RAW so we do NOT replace ${tmpDir} with blank... 
			_fileConfSlot._dirName = getResolvedStorageDir();


			// Most is done in the super class: PersistWriterToHttpJson (to get what we should save)
			initConfigSlot(conf, "DbxCentral", _fileConfSlot);
			
			// NOTE: The above 'initConfigSlot', if no URL like HERE, it will do NOTHING...
			//       So we need to initialize "some" stuff:
			//          - What Counters and graphs we want to store in the output files
			String cfgKey = "";
			SendCountersConfig sendCounters = new SendCountersConfig(conf, key(PROPKEY_sendCounters, cfgKey), DEFAULT_sendCounters);
			SendCountersConfig sendGraphs   = new SendCountersConfig(conf, key(PROPKEY_sendGraphs  , cfgKey), DEFAULT_sendGraphs);
			
			// We still need this to be configured (Example: the default is to send CmActiveStatements)
			_fileConfSlot._sendCounters = sendCounters;
			_fileConfSlot._sendGraphs   = sendGraphs;

		}

		//------------------------------------------
		// Check for mandatory parameters
		//------------------------------------------
//		if ( StringUtil.isNullOrBlank(_httpConfSlot._url) )  throw new Exception("The property '" + PROPKEY_url + "' is mandatory for the PersistWriter named '" + getName() + "'.");


		//------------------------------------------
		// Check for valid configuration
		//------------------------------------------
		
		//------------------------------------------
		// Print how we are configured
		//------------------------------------------
		printConfig();
	}

	public static String getResolvedStorageDir()
	{
		// Set System property 'tmpDir', which will be used to replace '${tmpDir}' in the fetched value
//		System.setProperty("tmpDir", System.getProperty("java.io.tmpdir"));
//		String dirName = conf.getProperty(PROPKEY_writerTypeFileDir, DEFAULT_writerTypeFileDir);

		// Using RAW so we do NOT replace ${tmpDir} with blank... 
		String dirName = Configuration.getCombinedConfiguration().getPropertyRaw(PROPKEY_writerTypeFileDir, DEFAULT_writerTypeFileDir);

		// replace '${tmpDir}'
		dirName = dirName.replace("${tmpDir}", System.getProperty("java.io.tmpdir"));

		return dirName;
	}
	
	public enum WriterType
	{
		FILE,
		HTTP;
		
		/** parse the value */
		public static WriterType fromString(String text)
		{
			for (WriterType type : WriterType.values()) 
			{
				// check for upper/lower: 'NAME', 'name'
				if (type.name().equalsIgnoreCase(text))
					return type;
			}
			throw new IllegalArgumentException("No constant with text " + text + " found");
		}
	};

	/** WriterType - Input validator */
	public static class WriterTypeInputValidator
	implements InputValidator
	{
		@Override
		public boolean isValid(CmSettingsHelper sh, String val) throws ValidationException
		{
			if (StringUtil.isNullOrBlank(val))
				throw new ValidationException("Can't be blank or null.");

			if ( ! StringUtil.containsAny(val, "file", "http") )
				throw new ValidationException("Must be 'file' or 'http'");

			return true;
		}
	}

	public static final String  PROPKEY_writerType        = "PersistWriterToDbxCentral.writer.type";
//	public static final String  DEFAULT_writerType        = WriterType.FILE.name().toLowerCase();
	public static final String  DEFAULT_writerType        = WriterType.HTTP.name().toLowerCase();

	public static final String  PROPKEY_writerTypeFileDir = "PersistWriterToDbxCentral.write.type.file.dir";
//	public static final String  DEFAULT_writerTypeFileDir = System.getProperty("java.io.tmpdir") + File.separatorChar + "DbxTune" + File.separatorChar + "PersistWriterToDbxCentral";
//	public static final String  DEFAULT_writerTypeFileDir = "${tmpDir}/DbxTune/PersistWriterToDbxCentral";
	public static final String  DEFAULT_writerTypeFileDir = "${tmpDir}/DbxTune/DbxCentral/file_receiver";

	@Override
	public List<CmSettingsHelper> getAvailableSettings()
	{
		ArrayList<CmSettingsHelper> list = new ArrayList<>();
		
		Configuration conf = Configuration.getCombinedConfiguration();

		list.add( new CmSettingsHelper("WriterType",          Type.MANDATORY,     key(PROPKEY_writerType )       , String .class, conf.getProperty(key(PROPKEY_writerType       ), DEFAULT_writerType)       , DEFAULT_writerType        , "How should we write the messages to DbxCentral. 'file' or 'http'. file = 'Write to local file, which would be picked up by DbxCentral', http= 'Send via a HTTP/REST call to DbxCentral' ", new WriterTypeInputValidator()));
		list.add( new CmSettingsHelper("WriterTypeFileDir",                       key(PROPKEY_writerTypeFileDir ), String .class, conf.getProperty(key(PROPKEY_writerTypeFileDir), DEFAULT_writerTypeFileDir), DEFAULT_writerTypeFileDir , "If we use type='file'. This is the directory where the files are stored."));

		
		// Get ConfigSettings from SUPER, and add the ones we want to keep
		List<CmSettingsHelper> superList = super.getAvailableSettings();
		for (CmSettingsHelper entry : superList)
		{
			// Skip some entries
			if (entry.getPropName().contains(".config.keys"))
				continue;

			// Decide what properties we should prefix description with "something"
			boolean addPrefix = true;
			if (entry.getPropName().contains(".send.alarm.extendedDescAsHtml")) addPrefix = false;
			if (entry.getPropName().contains(".send.counters"))                 addPrefix = false;
			if (entry.getPropName().contains(".send.graphs"))                   addPrefix = false;

			// Prefix description: 
			if (addPrefix)
				entry.setDescription("If we use type='http'. " + entry.getDescription()); 

			// Add to the Settings list
			list.add(entry);
		}

		return list;
	}
}

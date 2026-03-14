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
package com.dbxtune.central.controllers.ud.action;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.controllers.OverviewServlet;
import com.dbxtune.central.controllers.ud.action.IUserDefinedAction.ActionType;
import com.dbxtune.utils.Configuration;

public class UserDefinedActionManager
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

//	private List<IUserDefinedAction> _actions = new ArrayList<>();
	private Map<String, IUserDefinedAction> _actions = new LinkedHashMap<>();

	public static final String USER_DEFINED_FILE_POST = ".ud.action.props";

	private static AtomicBoolean _reInitialize = new AtomicBoolean(true);

	//----------------------------------------------------------------
	// BEGIN: instance
	private static UserDefinedActionManager _instance = null;
	public static synchronized UserDefinedActionManager getInstance()
	{
		if (_reInitialize.get())
		{
			_instance = null;
		}

		if (_instance == null)
		{
			_instance = new UserDefinedActionManager();
			_reInitialize.set(false);

			//throw new RuntimeException("UserDefinedActionManager dosn't have an instance yet, please set with setInstance(instance).");
		}
		return _instance;
	}
//	public static void setInstance(UserDefinedActionManager instance)
//	{
//		_instance = instance;
//	}
//	public static boolean hasInstance()
//	{
//		return _instance != null;
//	}
	// END: instance
	//----------------------------------------------------------------

	//----------------------------------------------------------------
	// BEGIN: Constructors
	public UserDefinedActionManager()
	{
		init();
	}
	// END: Constructors
	//----------------------------------------------------------------

	private void init()
	{
		for (File file : OverviewServlet.getFilesInConfDir())
		{
			if (_logger.isDebugEnabled())
				_logger.debug("***************** examin FILE: " + file);

			if (! file.isFile() )
				continue;

			int errorCount = 0;

			if (file.getName().endsWith(USER_DEFINED_FILE_POST))
			{
				Configuration conf = new Configuration(file.getAbsolutePath());
				String actionType = conf.getProperty("actionType");

				if (_logger.isDebugEnabled())
					_logger.debug("***************** READING FILE='" + file + "', actionType='" + actionType + "'.");

				String name = "-unkown-";
				try 
				{
					IUserDefinedAction action;

					if (ActionType.SQL.equals(ActionType.fromString(actionType)))
					{
						action = new UserDefinedActionSql(conf);
						name = action.getName();
					}
					else if (ActionType.OS_CMD.equals(ActionType.fromString(actionType)))
					{
						action = new UserDefinedActionOsCmd(conf);
						name = action.getName();
					}
					else
					{
						throw new IllegalArgumentException("No ActionType of " + actionType + " could be created. expected values are: " + ActionType.values());
					}

					// Add the above created UserDefinedAction
					IUserDefinedAction prevVal = _actions.put(name, action);
					if (prevVal != null)
					{
						throw new Exception("A User Defined Action has already been added with the Name='" + name + "'.");
					}
					_logger.info("Initialized User Defined Action of type '" + actionType + "', with key/name '" + name + "' in the file '" + file.getAbsolutePath() + "'.");
				}
				catch (Exception ex)
				{
					// Create add a dummy request 'UserDefinedActionError', which holds the error or problem we faced during initialization
					// This so we can read the error(s) from the WebPage
					errorCount++;
					UserDefinedActionError errorAction = new UserDefinedActionError();
					errorAction.setName(name);
					errorAction.setErrorDescription(ex.getMessage());
					errorAction.setConfigFilename(file.getAbsolutePath());
					_actions.put("ERROR[" + errorCount + "]: " + name, errorAction);
					
					_logger.error("Problems initializing User Defined Action from the file '" + file + "'.", ex);
				}
			}
		} 
	}

	/**
	 * Called from {@link Configuration.FileWatcher} when file has been changed.
	 * @param fullPath
	 */
	public void onConfigFileChange(Path fullPath)
	{
		_logger.info("Reinitialize due to: CHANGED - User Defined Action file '" + fullPath + "'.");
		_reInitialize.set(true);

//		_logger.info("Changes to User Defined Action file '" + fullPath + "'.");
//
//		for (IUserDefinedAction action : _actions.values())
//		{
//			Path actionCfgPath = Paths.get(action.getConfigFilename());
//			if (actionCfgPath.equals(fullPath))
//			{
//				action.onConfigFileChange(fullPath);
//			}
//		}
	}
	public void onConfigFileAdd(Path fullPath)
	{
		_logger.info("Reinitialize due to: ADDED - User Defined Action file '" + fullPath + "'.");
		_reInitialize.set(true);
	}
	public void onConfigFileRemove(Path fullPath)
	{
		_logger.info("Reinitialize due to: REMOVED - User Defined Action file '" + fullPath + "'.");
		_reInitialize.set(true);
	}

	
//	public IUserDefinedAction getAction(String name, String srvName)
//	{
//		return _actions.get(name + "|" + srvName);
//	}
	public IUserDefinedAction getAction(String name)
	{
		return _actions.get(name);
	}
	
	public List<IUserDefinedAction> getActions()
	{
		return new ArrayList<>(_actions.values());
	}

	public String getTemplateText()
	{
		String templateFile = "template.props";
		
		try
		{
			URL url = UserDefinedActionManager.class.getResource("template.props");
			if (url == null)
			{
				String msg = "Can't find the resource for class='" + UserDefinedActionManager.class + "', filename='" + templateFile + "'.";
				_logger.error(msg);
				return msg;
			}
			return IOUtils.toString(url, Charset.defaultCharset()); 
		}
		catch (Exception ex) 
		{
			return "Problems reading '" + templateFile + "'. Caught: " + ex;
		}
	}
}

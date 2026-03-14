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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public interface IUserDefinedAction
{
	public enum ActionType
	{
		/** SQL */
		SQL,

		/** OS_CMD */
		OS_CMD,

		/** ERROR - Only used to indicate that the Implementation is ERROR -- Could not be initialized */
		ERROR
		;

		/** parse the value */
		public static ActionType fromString(String text)
		{
			for (ActionType type : ActionType.values()) 
			{
				// check for upper/lower: 'TIMELINE', 'TIMELINE'
				if (type.name().equalsIgnoreCase(text))
					return type;

				// check for camelCase: 'maxOverMinutes', 'maxoverminutes'
				if (type.name().replace("_", "").equalsIgnoreCase(text))
					return type;
			}
			throw new IllegalArgumentException("No ActionType with text " + text + " found");
		}
	};
	
	/**
	 * Get name of a User defined Chart
	 * @return
	 */
	String getName();

	/**
	 * Get description of a User Defined Chart
	 * @return
	 */
	String getDescription();

	/**
	 * Get what type of chart this is
	 * @return
	 */
	ActionType getActionType();

	/**
	 * Get the command used by any Action
	 * @return
	 */
	String getCommand();

//	/**
//	 * Name of the service to get the information from 
//	 * @return
//	 */
//	String getDbmsServerName();

	/**
	 * Name of the Server where the command will be executed 
	 * @return
	 */
	String getOnServerName();

	
	/**
	 * Get suggested URL for this Chart
	 * @return
	 */
	String getUrl();

//	/**
//	 * Produce content, which later can be fetched with getContent()
//	 * @param out 
//	 * @throws Exception on any errors
//	 */
//	void produce(PrintWriter out) throws Exception;

//	/**
//	 * Get Information content for this User Defined Chart
//	 * <p>
//	 * This will be presented "at the top" in a collapse section
//	 */
//	String getInfoContent();
	/**
	 * Create Information content for this User Defined Chart
	 * <p>
	 * This will be presented "at the top" in a collapse section
	 */
	void createInfoContent(PrintWriter out) throws IOException;

//	/**
//	 * Get User Defined Chart HTML Content
//	 * <p>
//	 * This should only return 
//	 * @return
//	 */
//	String getContent();

	/**
	 * Produces output, normally called from a Servlet
	 * @return
	 */
	void produce(PrintWriter out) throws Exception;

	/**
	 * Create User Defined HTML Content
	 * @return
	 */
	void createContent(PrintWriter pageOut, PrintWriter mailOut) throws Exception;

	/**
	 * Get the name of the configuration file
	 * @return
	 */
	String getConfigFilename();

	/**
	 * Is this entry valid, in not check the description method
	 * @return
	 */
	boolean isValid();

	/**
	 * Get a list of CSS locations this module needs to load<br>
	 * Example: list.add("/scripts/bootstrap-table/1.12.1/bootstrap-table.min.css");
	 * @return
	 */
	List<String> getCssList();

	/**
	 * Get a list of JavaScript libraries this module needs to load<br>
	 * Example: list.add("https://www.gstatic.com/charts/loader.js");
	 * @return
	 */
	List<String> getJavaScriptList();

	/**
	 * Called from the responsible Servlet, so we can get parameters, and if there are parameters we dont know about we can throw an Exception
	 * @param map
	 * @throws Exception in case there are parameter problems
	 */
	void checkUrlParameters(Map<String, String> map) throws Exception;

	/** Set the parameters called by the servlet */
	void setUrlParameters(Map<String, String> parameterMap);

	/** Get the parameters passed to the servlet */
	Map<String, String> getUrlParameters();

	/**
	 * What known parameters does this take (used to check if we passed any "unknown" parameters, so we can fail early)
	 */
	String[] getKnownParameters();

	/**
	 * A description of each of the parameters
	 * @return <code>parameterName = description</code> Possibly a LinkedHashMap to preserve the order of the names
	 */
	Map<String, String> getParameterDescription();

//	/**
//	 * If anybody changes the underlying configuration file, this is called (from {@link Configuration.FileWatcher}) 
//	 * @param fullPath
//	 */
//	void onConfigFileChange(Path fullPath);
//NOW: The UserDefinedActionManager: just reinitialize the whole module(s)

	/**
	 * What is the log filename for this Action
	 * @return null = No separate log file
	 */
	String getLogFilename();

	/**
	 * What ROLES are authorized to execute this User Defined Action
	 * @return A list of Role(s)
	 */
	List<String> getAuthorizedRoles();

	/**
	 * What USERS are authorized to execute this User Defined Action
	 * @return A list of User(s)
	 */
	List<String> getAuthorizedUsers();

	void setPageRefreshTime(int refresh);
	int  getPageRefreshTime();

}

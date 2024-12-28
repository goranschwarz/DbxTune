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
package com.asetune.central.controllers.ud.chart;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.asetune.utils.Configuration;

public interface IUserDefinedChart
{
	public enum ChartType
	{
		/**
		 * Timeline is a GANTT look and feel output
		 */
		TIMELINE
		;

		/** parse the value */
		public static ChartType fromString(String text)
		{
			for (ChartType type : ChartType.values()) 
			{
				// check for upper/lower: 'TIMELINE', 'TIMELINE'
				if (type.name().equalsIgnoreCase(text))
					return type;

				// check for camelCase: 'maxOverMinutes', 'maxoverminutes'
				if (type.name().replace("_", "").equalsIgnoreCase(text))
					return type;
			}
			throw new IllegalArgumentException("No ChartType with text " + text + " found");
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
	ChartType getChartType();

	/**
	 * Name of the service to get the information from 
	 * @return
	 */
	String getDbmsServerName();

	/**
	 * Get suggested URL for this Chart
	 * @return
	 */
	String getUrl();

	/**
	 * Produce content, which later can be fetched with getContent()
	 * @throws Exception on any errors
	 */
	void produce() throws Exception;

	/**
	 * Get Information content for this User Defined Chart
	 * <p>
	 * This will be presented "at the top" in a collapse section
	 */
	String getInfoContent();

	/**
	 * Get User Defined Chart HTML Content
	 * <p>
	 * This should only return 
	 * @return
	 */
	String getContent();

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

	/**
	 * If anybody changes the underlying configuration file, this is called (from {@link Configuration.FileWatcher}) 
	 * @param fullPath
	 */
	void onConfigFileChange(Path fullPath);
}

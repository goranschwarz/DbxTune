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
package com.dbxtune.central.controllers.ud.chart;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.dbxtune.central.controllers.OverviewServlet;
import com.dbxtune.central.controllers.ud.chart.IUserDefinedChart.ChartType;
import com.dbxtune.utils.Configuration;

public class UserDefinedChartManager
{
	private static Logger _logger = Logger.getLogger(UserDefinedChartManager.class);

//	private List<IUserDefinedChart> _charts = new ArrayList<>();
	private Map<String, IUserDefinedChart> _charts = new LinkedHashMap<>();

	public static final String USER_DEFINED_FILE_POST = ".ud.content.props";


	//----------------------------------------------------------------
	// BEGIN: instance
	private static UserDefinedChartManager _instance = null;
	public static UserDefinedChartManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new UserDefinedChartManager();
			//throw new RuntimeException("UserDefinedChartManager dosn't have an instance yet, please set with setInstance(instance).");
		}
		return _instance;
	}
//	public static void setInstance(UserDefinedChartManager instance)
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
	public UserDefinedChartManager()
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

			if (file.getName().endsWith(USER_DEFINED_FILE_POST))
			{
				Configuration conf = new Configuration(file.getAbsolutePath());
				String chartType = conf.getProperty("chartType");

				if (_logger.isDebugEnabled())
					_logger.debug("***************** READING FILE='" + file + "', chartType='" + chartType + "'.");

				try 
				{
					if (ChartType.TIMELINE.equals(ChartType.fromString(chartType)))
					{
						UserDefinedTimelineChart chart = new UserDefinedTimelineChart(conf);

//						String key = chart.getName() + "|" + chart.getDbmsServerName();
						String key = chart.getName();
						IUserDefinedChart prevVal = _charts.put(key, chart);
						if (prevVal != null)
						{
//							throw new Exception("A User Defined Chart has already been added with the Name='" + chart.getName() + "' and SrvName='" + chart.getDbmsServerName() + "'.");
							throw new Exception("A User Defined Chart has already been added with the Name='" + chart.getName() + "'.");
						}
						_logger.info("Initialized User Defined Chart of type '" + chartType + "', with key/name '" + key + "' in the file '" + file.getAbsolutePath() + "'.");
					}
					else
					{
						throw new IllegalArgumentException("No ChartType of " + chartType + " could be created. expected values are: " + ChartType.values());
					}
				}
				catch (Exception ex)
				{
					_logger.error("Problems initializing User Defined Chart from the file '" + file + "'.", ex);
				}
			}
		} 
	}

	/**
	 * Called from {@link Configuration.FileWatcher} when file has been changed.
	 * @param fullPath
	 */
	public void onConfigChange(Path fullPath)
	{
		_logger.info("Changes to User Defined Chart file '" + fullPath + "'.");

		for (IUserDefinedChart chart : _charts.values())
		{
			Path chartCfgPath = Paths.get(chart.getConfigFilename());
			if (chartCfgPath.equals(fullPath))
			{
				chart.onConfigFileChange(fullPath);
			}
		}
	}

	
//	public IUserDefinedChart getChart(String name, String srvName)
//	{
//		return _charts.get(name + "|" + srvName);
//	}
	public IUserDefinedChart getChart(String name)
	{
		return _charts.get(name);
	}
	
	public List<IUserDefinedChart> getCharts()
	{
		return new ArrayList<>(_charts.values());
	}

	public String getTemplateText()
	{
		String templateFile = "template.props";
		
		try
		{
			URL url = UserDefinedChartManager.class.getResource("template.props");
			if (url == null)
			{
				String msg = "Can't find the resource for class='" + UserDefinedChartManager.class + "', filename='" + templateFile + "'.";
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

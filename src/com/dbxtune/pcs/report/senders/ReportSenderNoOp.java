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
package com.dbxtune.pcs.report.senders;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.pcs.report.DailySummaryReportFactory;
import com.dbxtune.pcs.report.content.DailySummaryReportContent;
import com.dbxtune.utils.Configuration;

public class ReportSenderNoOp 
extends ReportSenderAbstract
{
	private static Logger _logger = Logger.getLogger(ReportSenderNoOp.class);

	@Override
	public void init() throws Exception
	{
	}

	@Override
	public void send(DailySummaryReportContent reportContent)
	{
		String serverName = reportContent.getServerName();

		boolean saveIsEnabled = Configuration.getCombinedConfiguration().getBooleanProperty(DailySummaryReportFactory.PROPKEY_save   , DailySummaryReportFactory.DEFAULT_save);
		String saveDir        = Configuration.getCombinedConfiguration().getProperty(       DailySummaryReportFactory.PROPKEY_saveDir, DailySummaryReportFactory.DEFAULT_saveDir);

		String saveToStr = " And the Save the Report property '" + DailySummaryReportFactory.PROPKEY_save + "' is NOT enabled. Enable this if you want the Report to be saved in directory '" + saveDir + "'.";
		if (saveIsEnabled)
			saveToStr = " But the property '" + DailySummaryReportFactory.PROPKEY_save + "' is enabled, so the report will be saved to directory '" + saveDir + "'.";

		_logger.info("No Operation Sender: The report for server '"+serverName+"' will NOT be sent anywhere..." + saveToStr);
	}


//	@Override
	public List<CmSettingsHelper> getAvailableSettings()
	{
		ArrayList<CmSettingsHelper> list = new ArrayList<>();
		
		return list;
	}

	@Override
	public void printConfig()
	{
		_logger.info("Configuration for Report Sender Module: "+getName());
		_logger.info("    This module has no configuration.");
	}
}

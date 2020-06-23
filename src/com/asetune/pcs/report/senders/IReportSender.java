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
package com.asetune.pcs.report.senders;

import com.asetune.pcs.report.content.DailySummaryReportContent;

public interface IReportSender
{
	/**
	 * @return Name of this module 
	 */
	String getName();
	
	/**
	 * Initialize whatever
	 * @throws Exception 
	 */
	void init() throws Exception;

	/**
	 * Prints the configuration
	 */
	void printConfig();


	/**
	 * Check if this Sender is enabled for this specific server
	 * 
	 * @param serverName
	 * @return
	 */
	public boolean isEnabledForServer(String serverName);

	/**
	 * Send the report to "whatever"
	 * @param reportContent 
	 */
	void send(DailySummaryReportContent reportContent);

}

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
package com.asetune.pcs.report.content;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public interface IReportEntry
{
	/**
	 * Get Report Message as a text String
	 */
//	String getMsgAsText();

	/**
	 * Get Report Message as a HTML String (do NOT surround with HTML start/end tags)
	 */
	String getMessageText();

	/**
	 * If the ReportEntry has a problem... then print that problem.  
	 * @return
	 */
	String getProblemText();
	
	/**
	 * Text that should be appended "at the end" of each Report Entry
	 * @return
	 */
	String getEndOfReportText();
	
	/**
	 * 
	 */
	String getSubject();

	/**
	 * is this report entry/section enabled or should we skip this...
	 */
	boolean isEnabled();
	
	/** 
	 * Get the configuration 'key' name, for enable/disable this Report Entry 
	 */
	String getIsEnabledConfigKeyName();

	/**
	 * Can this Report entry be enabled/disabled (if true: then we want to write some info about this)
	 * @return
	 */
	boolean canBeDisabled();

	/**
	 * If it's disabled, print the reason for it.<br>
	 * It can for example be:
	 * <ul>
	 *   <li>This report is only supported if DBMS is running on Linux</li>
	 *   <li>This report is only supported if DBMS is running on Windows</li>
	 *   <li>This report is only supported in DBMS Version x.y or above.</li>
	 * </ul>
	 * @return (can be null). The reason Why this entry is disabled.
	 */
	String getDisabledReason();

	
	/**
	 * in what order should the ReportEntries be reported in<br>
	 * return 0 in case the order is not important (then it will be reported in the order the entries are added)
	 */
	default double getReportOrder()
	{
		return 0;
	}

	/**
	 * Get severity (more or less if this entry has something to report, or if the "subject" should contain the NTR - Nothing To Report tag
	 */
//	int getSeverity();
	
	/**
	 * Indicate that this is of NO RELEVEANCE.<br>
	 * For instance a mail Subject will contain: -NTR-, here is an example: 'Daily Report -NTR- for: PROD_A1_ASE'
	 * @return TRUE == Something of interest... FALSE = Nothing To Report
	 */
	boolean hasIssueToReport();

	/**
	 * Create Report entry with data from the database...
	 * @param conn            Connection to the DBMS which stores data
	 * @param srvName         Name of the DBMS Server Instance
	 * @param pcsSavedConf    any configuration saved in the pcs
	 * @param localConf       local configuration (same as <code>Configuration conf = Configuration.getCombinedConfiguration()</code>)

	 * @return
	 */
	void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf);

	/**
	 * Get any of the ResultSetTableModel created in the "create(...)" method
	 * 
	 * @param whatData    if the create() method generated many Results 
	 * @return
	 */
	default ResultSetTableModel getData(String whatData)
	{
		return null;
	}

//	/**
//	 * Create Report entry with data from the database...
//	 * @param conn
//	 * @return
//	 */
//	ResultSetTableModel createData(DbxConnection conn);
//
//	/** 
//	 * Any exceptions when getting data?
//	 */
//	Exception getExecption();
}

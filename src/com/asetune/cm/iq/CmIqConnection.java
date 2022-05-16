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
package com.asetune.cm.iq;

import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */

/**
 * sp_iqconnection procedure
 * Shows information about connections and versions, including which users are using temporary dbspace, which users are keeping versions alive, what the connections are doing inside Sybase IQ, connection status, database version status, and so on.
 * 
 * sp_iqconnection returns a row for each active connection. The columns ConnHandle, Name, Userid, LastReqTime, ReqType, CommLink, NodeAddr, and LastIdle are the connection properties Number, Name, Userid, LastReqTime, ReqType, CommLink, NodeAddr, and LastIdle respectively, and return the same values as the system function sa_conn_info. The additional columns return connection data from the Sybase IQ side of the Sybase IQ engine. Rows are ordered by ConnCreateTime. 
 * The column MPXServerName stores information related to multiplex internode communication (INC)
 * @author I063869
 *
 */
public class CmIqConnection
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminWhoSqm.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIqConnection.class.getSimpleName();
	public static final String   SHORT_NAME       = "connections";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<h4>sp_iqconnection</h4>"
		+ "sp_iqconnection returns a row for each active connection. "
		+ "<br/>The columns ConnHandle, Name, Userid, LastReqTime, ReqType, CommLink, NodeAddr, and LastIdle "
		+ "<br/>are the connection properties Number, Name, Userid, LastReqTime, ReqType, CommLink, NodeAddr, "
		+ "<br/>and LastIdle respectively, and return the same values as the system function sa_conn_info. "
		+ "<br/>The additional columns return connection data from the Sybase IQ side of the Sybase IQ engine. "
		+ "<br/>Rows are ordered by ConnCreateTime."
		+ "<br/>The column MPXServerName stores information related to multiplex internode communication (INC)" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sp_iqconnection"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"IQthreads",
		"TempTableSpaceKB",
		"TempWorkSpaceKB",
		"satoiq_count",
		"iqtosa_count"
	};


	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 60; //CountersModel.DEFAULT_sqlQueryTimeout;
	

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.ALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmIqConnection(counterController, guiController);
	}

	public CmIqConnection(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);
		setBackgroundDataPollingEnabled(true, false);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
	private void addTrendGraphs()
	{
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmRaSysmonPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("sp_iqconnection",  "Shows information about connections and versions, including which users are using temporary dbspace, which users are keeping versions alive, what the connections are doing inside Sybase IQ, connection status, database version status, and so on.");
			
			mtd.addColumn("sp_iqconnection","ConnHandle",
					"<html>The ID number of the connection.</html>");
			mtd.addColumn("sp_iqconnection","Name",
					"<html>The name of the server.</html>");
			mtd.addColumn("sp_iqconnection","Userid",
					"<html>The user ID for the connection.</html>");
			mtd.addColumn("sp_iqconnection","LastReqTime",
					"<html>The time at which the last request for the specified connection started.</html>");
			mtd.addColumn("sp_iqconnection","ReqType",
					"<html>A string for the type of the last request.</html>");
			mtd.addColumn("sp_iqconnection","IQCmdType",
					"<html>The current command executing on the Sybase IQ side, if any. "
					+ "<br/>The command type reflects commands defined at the implementation level of the engine. "
					+ "<br/>These commands consists of transaction commands, DDL and DML commands for data in the "
					+ "<br/>IQ store, internal IQ cursor commands, and special control commands such as OPEN and "
					+ "<br/>CLOSE DB, BACKUP, RESTORE, and others.</html>");
			mtd.addColumn("sp_iqconnection","LastIQCmdTime",
					"<html>The time the last IQ command started or completed on the IQ side of the Sybase IQ "
					+ "<br/>engine on this connection.</html>");
			mtd.addColumn("sp_iqconnection","IQCursors",
					"<html>The number of cursors open in the IQ store on this connection.</html>");
			mtd.addColumn("sp_iqconnection","LowestIQCursorState",
					"<html>The IQ cursor state, if any. If multiple cursors exist on the connection, the state "
					+ "<br/>displayed is the lowest cursor state of all the cursors; that is, the furthest from completion. "
					+ "<br/>Cursor state reflects internal Sybase IQ implementation detail and is subject to change in the future. "
					+ "<br/>For this version, cursor states are: NONE, INITIALIZED, PARSED, DESCRIBED, COSTED, PREPARED, "
					+ "<br/>EXECUTED, FETCHING, END_OF_DATA, CLOSED and COMPLETED. As suggested by the names, "
					+ "<br/>cursor state changes at the end of the operation. A state of PREPARED, for example, "
					+ "<br/>indicates that the cursor is executing.</html>");
			mtd.addColumn("sp_iqconnection","IQthreads",
					"<html>The number of Sybase IQ threads currently assigned to the connection. "
					+ "<br/>Some threads may be assigned but idle. This column can help you determine which connections "
					+ "<br/>are using the most resources.</html>");
			mtd.addColumn("sp_iqconnection","TxnID",
					"<html>The transaction ID of the current transaction on the connection. "
					+ "<br/>This is the same as the transaction ID displayed in the .iqmsg file by the BeginTxn, CmtTxn, and PostCmtTxn "
					+ "<br/>messages, as well as the Txn ID Seq logged when the database is opened.</html>");
			mtd.addColumn("sp_iqconnection","ConnCreateTime",
					"<html>The time the connection was created.</html>");
			mtd.addColumn("sp_iqconnection","TempTableSpaceKB",
					"<html>The number of kilobytes of IQ temporary store space in use by this connection "
					+ "<br/>for data stored in IQ temp tables.</html>");
			mtd.addColumn("sp_iqconnection","TempWorkSpaceKB",
					"<html>The number of kilobytes of IQ temporary store space in use by this connection "
					+ "<br/>for working space such as sorts, hashes, and temporary bitmaps. Space used by bitmaps "
					+ "<br/>or other objects that are part of indexes on Sybase IQ temporary tables are reflected in "
					+ "<br/>TempTableSpaceKB.</html>");
			mtd.addColumn("sp_iqconnection","IQConnID",
					"<html>The 10-digit connection ID displayed as part of all messages in the .iqmsg file. "
					+ "<br/>This is a monotonically increasing integer unique within a server session.</html>");
			mtd.addColumn("sp_iqconnection","satoiq_count",
					"<html>An internal counter used to display the number of crossings from the SQL Anywhere "
					+ "<br/>side to the IQ side of the Sybase IQ engine. This might be occasionally useful in determining "
					+ "<br/>connection activity. Result sets are returned in buffers of rows and do not increment satoiq_"
					+ "<br/>count or iqtosa_count once per row.</html>");
			mtd.addColumn("sp_iqconnection","iqtosa_count",
					"<html>An internal counter used to display the number of crossings from the IQ side to the SQL Anywhere "
					+ "<br/>side of the Sybase IQ engine. You might find this column to be occasionally useful in determining "
					+ "<br/>connection activity.</html>");
			mtd.addColumn("sp_iqconnection","CommLink",
					"<html>The communication link for the connection. This is one of the network protocols supported "
					+ "by Sybase IQ, or is local for a same-machine connection.</html>");
			mtd.addColumn("sp_iqconnection","NodeAddr",
					"<html>The node for the client in a client/server connection.</html>");
			mtd.addColumn("sp_iqconnection","LastIdle",
					"<html>The number of ticks between requests.</html>");
			mtd.addColumn("sp_iqconnection","MPXServerName",
					"<html>If an INC connection, the varchar(128) value contains the name of the multiplex server where "
					+ "<br/>the INC connection originates. NULL if not an INC connection.</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();
		
		pkCols.add("ConnHandle");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = "select * from sp_iqconnection()";

		return sql;
	}
}

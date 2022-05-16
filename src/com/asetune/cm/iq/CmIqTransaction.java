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
import com.asetune.cm.iq.gui.CmIqTransactionPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */

/**
 * sp_iqtransaction procedure
 * Shows information about transactions and versions.
 * sp_iqtransaction returns a row for each transaction control block in the Sybase IQ transaction manager. The columns Name, Userid, and ConnHandle are the connection properties Name, Userid, and Number, respectively. Rows are ordered by TxnID.
 * sp_iqtransaction output does not contain rows for connections that do not have a transaction started. To see all connections, use sp_iqconnection.
 * 
 * 
 * @author I063869
 *
 */
public class CmIqTransaction
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminWhoSqm.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIqTransaction.class.getSimpleName();
	public static final String   SHORT_NAME       = "transaction";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<h4>sp_iqtransaction</h4>" +
		"Shows information about transactions and versions." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sp_iqtransaction"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"MainTableKBCr",
		"MainTableKBDr",
		"TempTableKBCr",
		"TempTableKBDr",
		"TempWorkSpaceKB",
		"CursorCount",
		"SpCount",
		"SpNumber"
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

		return new CmIqTransaction(counterController, guiController);
	}

	public CmIqTransaction(ICounterController counterController, IGuiController guiController)
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

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmIqTransactionPanel(this);
	}

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
			mtd.addTable("sp_iqtransaction",  "FIXME");

			mtd.addColumn("sp_iqtransaction", "Name",            "<html>The name of the application.</html>");
			mtd.addColumn("sp_iqtransaction", "Userid",          "<html>The user ID for the connection. </html>");
			mtd.addColumn("sp_iqtransaction", "TxnID",           "<html>The transaction ID of this transaction control block. The transaction ID is assigned during begin transaction. <br>"
			                                                         + "This is the same as the transaction ID displayed in the .iqmsg file by the BeginTxn, CmtTxn and PostCmtTxn messages as well as the Txn ID Seq logged when the database is opened.</html>");
			mtd.addColumn("sp_iqtransaction", "CmtID",           "<html>The ID assigned by the transaction manager when the transaction commits. It is zero for active transactions.</html>");
			mtd.addColumn("sp_iqtransaction", "VersionID",       "<html>In simplex databases, the VersionID is the same as the TxnID. For the multiplex coordinator, the VersionID is the same as the TxnID of the active transaction and VersionID is the same as the CmtID of a committed transaction. <br>"
			                                                         + "In multiplex secondary servers, the VersionID is the CmtID of the transaction that created the database version on the multiplex coordinator. <br>"
			                                                         + "It is used internally by the Sybase IQ in-memory catalog and the IQ transaction manager to uniquely identify a database version to all nodes within a multiplex database.</html>");
			mtd.addColumn("sp_iqtransaction", "State",           "<html>The state of the transaction control block. This variable reflects internal Sybase IQ implementation detail and is subject to change in the future. <br>"
			                                                         + "At the time of this writing, transaction states are NONE, ACTIVE, ROLLING_BACK, ROLLED_BACK, COMMITTING, COMMITTED, and APPLIED.</html>");
			mtd.addColumn("sp_iqtransaction", "ConnHandle",      "<html>The ID number of the connection.</html>");
			mtd.addColumn("sp_iqtransaction", "IQConnID",        "<html>The ten-digit connection ID displayed as part of all messages in the .iqmsg file. This is a monotonically increasing integer unique within a server session.</html>");
			mtd.addColumn("sp_iqtransaction", "MainTableKBCr",   "<html>The number of kilobytes of IQ store space created by this transaction.</html>");
			mtd.addColumn("sp_iqtransaction", "MainTableKBDr",   "<html>The number of kilobytes of IQ store space dropped by this transaction, but which persist on disk in the store because the space is visible in other database versions or other savepoints of this transaction.</html>");
			mtd.addColumn("sp_iqtransaction", "TempTableKBCr",   "<html>The number of kilobytes of IQ temporary store space created by this transaction for storage of IQ temporary table data.</html>");
			mtd.addColumn("sp_iqtransaction", "TempTableKBDr",   "<html>The number of kilobytes of IQ temporary table space dropped by this transaction, but which persist on disk in the IQ temporary store because the space is visible to IQ cursors or is owned by other savepoints of this transaction.</html>");
			mtd.addColumn("sp_iqtransaction", "TempWorkSpaceKB", "<html>For ACTIVE transactions, this is a snapshot of the work space in use at this instant by this transaction, such as sorts, hashes, and temporary bitmaps. <br>"
			                                                         + "The number varies depending on when you run sp_iqtransaction. For example, the query engine might create 60MB in the temporary cache but release most of it quickly, even though query processing continues. <br>"
			                                                         + "If you run sp_iqtransaction after the query finishes, this column shows a much smaller number. <br>"
			                                                         + "When the transaction is no longer active, this column is zero. <br/>For ACTIVE transactions, this column is the same as the TempWorkSpaceKB column of sp_iqconnection.</html>");
			mtd.addColumn("sp_iqtransaction", "TxnCreateTime",   "<html>The time the transaction began. All Sybase IQ transactions begin implicitly as soon as an active connection is established or when the previous transaction commits or rolls back.</html>");
			mtd.addColumn("sp_iqtransaction", "CursorCount",     "<html>The number of open Sybase IQ cursors that reference this transaction control block. <br>"
			                                                         + "If the transaction is ACTIVE, it indicates the number of open cursors created within the transaction. <br>"
			                                                         + "If the transaction is COMMITTED, it indicates the number of HOLD cursors that reference a database version owned by this transaction control block. <br>"
			                                                         + "The number of open Sybase IQ cursors that reference this transaction control block.<br>"
			                                                         + " If the transaction is ACTIVE, it indicates the number of open cursors created within the transaction. <br>"
			                                                         + "If the transaction is COMMITTED, it indicates the number of HOLD cursors that reference a database version owned by this transaction control block.</html>");
			mtd.addColumn("sp_iqtransaction", "SpCount",         "<html>The number of savepoint structures that exist within the transaction control block. Savepoints may be created and released implicitly. <br>"
			                                                         + "Therefore, this number does not indicate the number of user-created savepoints within the transaction.</html>");
			mtd.addColumn("sp_iqtransaction", "SpNumber",        "<html>The active savepoint number of the transaction. This is an implementation detail and might not reflect a user-created savepoint.</html>");
			mtd.addColumn("sp_iqtransaction", "MPXServerName",   "<html>The value indicates if an active transaction is from an inter-node communication (INC) connection. <br>"
			                                                         + "If from INC connection, the value is the name of the multiplex server where the transaction originates. NULL if not from an INC connection. Always NULL if the transaction is not active.</html>");
			mtd.addColumn("sp_iqtransaction", "GlobalTxnID",     "<html>The value indicates the global transaction ID associated with the current transaction. Zero if there is no associated global transaction.</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("TxnID");      // in the web errorlog we get duplicates... and I dont really know what output is "unique", so I'm just guessing here...
		pkCols.add("VersionID");
		pkCols.add("ConnHandle"); // the ConnHandle could probably be removed... but I haven't got a good test system to test it out on

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = "select * from sp_iqtransaction()";

		return sql;
	}
}

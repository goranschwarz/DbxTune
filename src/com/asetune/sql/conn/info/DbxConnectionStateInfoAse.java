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
package com.asetune.sql.conn.info;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.DbUtils;
import com.asetune.utils.Ver;

public class DbxConnectionStateInfoAse
implements DbxConnectionStateInfo
{
	private static Logger _logger = Logger.getLogger(DbxConnectionStateInfoAse.class);

	/** _current* below is only maintained if we are connected to ASE */
	public String _dbname       = "";
	public int    _spid         = -1;
	public String _username     = "";
	public String _susername    = "";
	public int    _tranState    = -1;
	public int    _tranCount    = -1;
	public int    _tranChained  = -1;
	public int    _lockCount    = -1;
	public int    _hadrModeInt  = -1; // @@hadr_mode
	public String _hadrModeStr  = ""; // hadr_mode()
	public int    _hadrStateInt = 0;  // @@hadr_state
	public String _hadrStateStr = ""; // hadr_state()
	public List<LockRecord> _lockList = new ArrayList<LockRecord>();

	private boolean _isAse = true;

	// Transaction SQL states for DONE flavors
	//
	// 0 Transaction in progress: an explicit or implicit transaction is in effect;
	//   the previous statement executed successfully.
	// 1 Transaction succeeded: the transaction completed and committed its changes.
	// 2 Statement aborted: the previous statement was aborted; no effect on the transaction.
	// 3 Transaction aborted: the transaction aborted and rolled back any changes.
	public static final int TSQL_TRAN_IN_PROGRESS = 0;
	public static final int TSQL_TRAN_SUCCEED = 1;
	public static final int TSQL_STMT_ABORT = 2;
	public static final int TSQL_TRAN_ABORT = 3;
	public static final int TSQL_TRANSTATE_NOT_AVAILABLE = 4; // Possible a MSSQL system

	public static final String[] TSQL_TRANSTATE_NAMES =
	{
//		"TSQL_TRAN_IN_PROGRESS",
//		"TSQL_TRAN_SUCCEED",
//		"TSQL_STMT_ABORT",
//		"TSQL_TRAN_ABORT"
		"TRAN_IN_PROGRESS",
		"TRAN_SUCCEED",
		"STMT_ABORT",
		"TRAN_ABORT",
		"NOT_AVAILABLE"
	};

	public static final String[] TSQL_TRANSTATE_DESCRIPTIONS =
	{
//		"TRAN_IN_PROGRESS = Transaction in progress. A transaction is in effect; \nThe previous statement executed successfully.",
		"TRAN_IN_PROGRESS = Transaction in progress. \nThe previous statement executed successfully.",
		"TRAN_SUCCEED = Last Transaction succeeded. \nThe transaction completed and committed its changes.",
		"STMT_ABORT = Last Statement aborted. \nThe previous statement was aborted; \nNo effect on the transaction.",
		"TRAN_ABORT = Last Transaction aborted. \nThe transaction aborted and rolled back any changes.",
		"NOT_AVAILABLE = Not available in this system."
	};
	

	public DbxConnectionStateInfoAse(DbxConnection conn)
	{
		refresh(conn);
	}
	
	private void refresh(DbxConnection conn)
	{
		_isAse = conn.isDatabaseProduct(DbUtils.DB_PROD_NAME_SYBASE_ASE);

		String sql = "select dbname=db_name(), spid=@@spid, username = user_name(), susername =suser_name(), trancount=@@trancount";
		if (_isAse)
		{
			sql += ", tranchained=@@tranchained, transtate=@@transtate";
			if (conn.getDbmsVersionNumber() >= Ver.ver(16,0,0, 2))
				sql += ", @@hadr_mode, hadr_mode(), @@hadr_state, hadr_state()";
		}
		else
		{	// SQL-Server
			sql += ", tranchained=sign((@@options & 2))"; // MSSQL retired @@transtate in SqlServer2008, SqlServer never implemented @@transtate 
		}

		// Do the work
		try
		{
			Statement stmnt = conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);

			while(rs.next())
			{
				_dbname      = rs.getString(1);
				_spid        = rs.getInt   (2);
				_username    = rs.getString(3);
				_susername   = rs.getString(4);
				_tranCount   = rs.getInt   (5);
				_tranChained = rs.getInt   (6);
				if (_isAse)
				{
					_tranState = rs.getInt(7);

					if (conn.getDbmsVersionNumber() >= Ver.ver(16,0,0, 2)) // 16.0 SP2
					{
    					_hadrModeInt  = rs.getInt   (8);
    					_hadrModeStr  = rs.getString(9);
    					_hadrStateInt = rs.getInt   (10);
    					_hadrStateStr = rs.getString(11);
					}
				}
				else
				{
					_tranState    = TSQL_TRANSTATE_NOT_AVAILABLE;
					_hadrModeInt  = -1;
					_hadrModeStr  = "Not Available";
					_hadrStateInt = 0;
					_hadrStateStr = "Not Available";
				}
			}
			rs.close();

//			sql = "select count(*) from master.dbo.syslocks where spid = @@spid";
//			rs = stmnt.executeQuery(sql);
//			while(rs.next())
//			{
//				_lockCount = rs.getInt(1);
//			}
			if (_isAse)
			{
				sql = "select dbname=db_name(dbid), table_name=object_name(id, dbid), lock_type=type, lock_count=count(*) "
					+ " from master.dbo.syslocks "
					+ " where spid = @@spid	"
					+ " group by dbid, id, type ";

				_lockCount = 0;
				_lockList.clear();

				rs = stmnt.executeQuery(sql);
				while(rs.next())
				{
					String dbname    = rs.getString(1);
					String tableName = rs.getString(2);
					int    lockType  = rs.getInt   (3);
					int    lockCount = rs.getInt   (4);

					_lockCount += lockCount;
					_lockList.add( new LockRecord(dbname, tableName, lockType, lockCount) );
				}

				rs.close();
				stmnt.close();
			}
			else // MS SQL do not have syslocks anymore, so use: sys.dm_tran_locks, and simulate some kind of equal question...
			{    // NOTE: this needs permission 'VIEW SERVER STATE'
				List<String> permissions = conn.getActiveServerRolesOrPermissions();
				if (permissions != null && permissions.contains("VIEW SERVER STATE"))
				{
    				sql = "select dbname=db_name(resource_database_id),	table_name=object_name(resource_associated_entity_id, resource_database_id), lock_type=request_mode, lock_count=request_reference_count "
    				    + " from sys.dm_tran_locks "
    				    + " where request_session_id = @@spid "
    				    + "  and resource_type = 'OBJECT' ";
    
    				_lockCount = 0;
    				_lockList.clear();
    
    				rs = stmnt.executeQuery(sql);
    				while(rs.next())
    				{
    					String dbname    = rs.getString(1);
    					String tableName = rs.getString(2);
    					String lockType  = rs.getString(3);
    					int    lockCount = rs.getInt   (4);
    
    					_lockCount += lockCount;
    					_lockList.add( new LockRecord(dbname, tableName, lockType, lockCount) );
    				}
    
    				rs.close();
    				stmnt.close();
				}
				else
				{
					_lockCount = -999;
				}
			}

		}
		catch (SQLException sqle)
		{
			_logger.error("Error in refresh() problems executing sql='"+sql+"'.", sqle);
		}

//		select count(*) from master.dbo.syslogshold where spid = @@spid

		if (_logger.isDebugEnabled())
			_logger.debug("refresh(): db_name()='"+_dbname+"', @@spid='"+_spid+"', user_name()='"+_username+"', suser_name()='"+_susername+"', @@transtate="+_tranState+", '"+getTranStateStr()+"', @@trancount="+_tranCount+".");
//		return csi;
	}

	@Override
	public boolean isNormalState()
	{
		return isNormalTranState();
	}

	@Override
	public String getWaterMarkText()
	{
		String str = null;
		
		if ( _tranCount > 0 || isNonNormalTranState() )
		{
			if (_tranChained == 1)
			{
				if (_lockCount > 0)
					str = "You are in CHAINED MODE (AutoCommit=false)\n"
						+ "And you are holding "+_lockCount+" locks in the server\n"
						+ "Don't forget to commit or rollback!";
			}
			else
			{
    			if (isTranStateUsed())
    				str = getTranStateDescription() + "\n@@trancount = " + _tranCount + ", @@tranchained = " + _tranChained;
    			else
    				str = "@@trancount = " + _tranCount + ", @@tranchained = " + _tranChained;
			}
		}
		
		return str;
	}

	@Override
	public String getStatusBarText()
	{
		String dbname      = "db=<b>"          + _dbname           + "</b>";
		String spid        = "spid=<b>"        + _spid             + "</b>";
		String username    = "user=<b>"        + _username         + "</b>";
		String susername   = "login=<b>"       + _susername        + "</b>";
		String tranState   = "TranState=<b>"   + getTranStateStr() + "</b>";
		String tranCount   = "TranCount=<b>"   + _tranCount        + "</b>";
		String tranChained = "TranChained=<b>" + _tranChained      + "</b>";
		String lockCount   = "LockCount=<b>"   + _lockCount        + "</b>";
		String hadrInfo    = "";

		if (_tranCount > 0)
			tranCount = "TranCount=<b><font color='red'>" + _tranCount        + "</font></b>";

		if ( isNonNormalTranState() )
			tranState = "TranState=<b><font color='red'>" + getTranStateStr() + "</font></b>";
		
		if (_tranCount > 0)
			tranChained = "TranChained=<b><font color='red'>" + _tranChained    + "</font></b>";

		if (_lockCount > 0)
			lockCount = "LockCount=<b><font color='red'>" + _lockCount    + "</font></b>";

		if (_hadrModeInt >= 0) // -1 == Disabled
		{
			String color="black";
			switch(_hadrModeInt)
			{
			case 0: // 0 == Standby
				color="blue";
				break;

			case 1: // 1 == Primary
				color="green";
				break;

			case 2: // 2 == Unreachable
				color="red";
				break;
			
			case 3: // 3 == Starting
				color="fuchsia";
				break;
			
			default: // unknown
				color="black";
				break;
			}
			hadrInfo  = ", HADR Mode=<font color='"+color+"'>" + _hadrModeInt  + ":<b>" + _hadrModeStr  + "</b></font>, State=<font color='"+color+"'>" + _hadrStateInt + ":<b>" + _hadrStateStr + "</b></font>";
		}

		// status: Normal state
		String text = "<html>" + spid + ", " + dbname + ", " + username + ", " + susername + hadrInfo + "</html>";

		// status: "problem" state
		if (_tranCount > 0 || isNonNormalTranState())
		{
			text = "<html>"
				+ spid      + ", " 
				+ dbname    + ", "
				+ username  + ", " 
				+ susername + ", " 
				+ (isTranStateUsed() ? (tranState + ", ") : "") 
				+ tranCount + ", "
				+ tranChained + ", "
				+ lockCount
				+ hadrInfo
				+ "</html>";
		}
		// If we are in CHAINED mode, and do NOT hold any locks, set state to "normal"
		if (_tranChained == 1 && _lockCount == 0)
		{ // color #B45F04 = dark yellow/orange
			text = "<html><font color='#B45F04'>CHAINED mode</font>, " + spid + ", " + dbname + ", " + username + ", " + susername + hadrInfo + "</html>";
		}

		return text;
	}

	@Override
	public String getStatusBarToolTipText()
	{
		String lockText = "<hr>";
		if (_lockCount > 0)
			lockText = "<hr>Locks held by this SPID:" + getLockListTableAsHtmlTable() + "<hr>";

		String lockCountStr = _lockCount +"";
		if (_lockCount == -999)
			lockCountStr = "<font color='red'> To see lock count/table you need permission 'VIEW SERVER STATE'</font>";

		String hadrTooltip = "";
		if (_hadrModeInt >= 0) // -1 == Disabled
		{
			hadrTooltip = 
                "<tr> <td>HADR Mode:  </td> <td><b>" + _hadrModeInt  + " = " + _hadrModeStr  + "</b> </td> </tr>" +
                "<tr> <td>HADR State: </td> <td><b>" + _hadrStateInt + " = " + _hadrStateStr + "</b> </td> </tr>";
		}
		
		String tooltip = "<html>" +
			"<table border=0 cellspacing=0 cellpadding=1>" +
			                         "<tr> <td>Current DB:    </td> <td><b>" + _dbname           + "</b> </td> </tr>" +
			                         "<tr> <td>SPID:          </td> <td><b>" + _spid             + "</b> </td> </tr>" +
			                         "<tr> <td>Current User:  </td> <td><b>" + _username         + "</b> </td> </tr>" +
			                         "<tr> <td>Current Login: </td> <td><b>" + _susername        + "</b> </td> </tr>" +
			    (isTranStateUsed() ? "<tr> <td>Tran State:    </td> <td><b>" + getTranStateStr() + "</b> </td> </tr>" : "") +
			                         "<tr> <td>Tran Count:    </td> <td><b>" + _tranCount        + "</b> </td> </tr>" +
			                         "<tr> <td>Tran Chained:  </td> <td><b>" + _tranChained      + "</b> </td> </tr>" +
			                         hadrTooltip + 
			                         "<tr> <td>Lock Count:    </td> <td><b>" + lockCountStr      + "</b> </td> </tr>" +
			"</table>" +
			lockText +
			(isTranStateUsed() ? ASE_STATE_INFO_TOOLTIP_BASE : ASE_STATE_INFO_TOOLTIP_BASE_NO_TRANSTATE).replace("<html>", ""); // remove the first/initial <html> tag...

		return tooltip;
	}

	private static final String ASE_STATE_INFO_TOOLTIP_BASE = 
			"<html>" +
			"Various status for the current connection. Are we in a transaction or not.<br>" +
			"<br>" +
			"<code>@@trancount</code> / TranCount Explanation:" +
			"<ul>" +
			"  <li>Simply a counter on <code>begin transaction</code> nesting level<br>" +
			"      Try to issue <code>begin/commit/rollback tran</code> multiple times and see how @@trancount increases/decreases (rollback always resets it to 0)</li>" +
			"</ul>" +
			"<code>@@transtate</code> / TranState Explanation:" +
			"<ul>" +
			"  <li><b>TRAN_IN_PROGRESS:</b> Transaction in progress. A transaction is in effect; The previous statement executed successfully</li>" +
			"  <li><b>TRAN_SUCCEED:    </b> Last Transaction succeeded. The transaction completed and committed its changes.</li>" +
			"  <li><b>STMT_ABORT:      </b> Last Statement aborted. The previous statement was aborted; No effect on the transaction.</li>" +
			"  <li><b>TRAN_ABORT:      </b> Last Transaction aborted. The transaction aborted and rolled back any changes.<br>" +
			"                               To get rid of status 'TRAN_ABORT' simply issue <code>begin tran commit tran</code> to induce a dummy transaction that succeeds...<br>" +
			"      </li>" +
			"</ul>" +
			"<br>" +
			"<code>@@tranchained</code> / TranChained Explanation:" +
			"<ul>" +
			"  <li><b>0</b>: autocommit=true  (<code>set chained off</code>): The default mode, called <b>unchained</b> mode or Transact-SQL mode, requires explicit <b>begin transaction</b> statements paired with <b>commit transaction</b> or <b>rollback transaction</b> statements to complete the transaction.</li>" +
			"  <li><b>1</b>: autocommit=false (<code>set chained on</code>):  <b>chained</b> mode implicitly begins a transaction before any data-retrieval or modification statement: <b>delete</b>, <b>insert</b>, <b>open</b>, <b>fetch</b>, <b>select</b>, and <b>update</b>. You must still explicitly end the transaction with <b>commit transaction</b> or <b>rollback transaction</b></li>" +
			"</ul>" +
			"</html>";
		
	private static final String ASE_STATE_INFO_TOOLTIP_BASE_NO_TRANSTATE = 
			"<html>" +
			"Various status for the current connection. Are we in a transaction or not.<br>" +
			"<br>" +
			"<code>@@trancount</code> / TranCount Explanation:<br>" +
			"<ul>" +
			"  <li>Simply a counter on <code>begin transaction</code> nesting level<br>" +
			"      Try to issue <code>begin/commit/rollback tran</code> multiple times and see how @@trancount increases/decreases (rollback always resets it to 0)</li>" +
			"</ul>" +
			"</html>";











	public boolean isTranStateUsed()
	{
		return _tranState != TSQL_TRANSTATE_NOT_AVAILABLE;
	}

	public boolean isNonNormalTranState()
	{
		return ! isNormalTranState();
	}
	public boolean isNormalTranState()
	{
		if (_tranState == TSQL_TRAN_SUCCEED)            return true;
		if (_tranState == TSQL_TRANSTATE_NOT_AVAILABLE) return true;
		return false;
	}

	public String getTranStateStr()
	{
		return tsqlTranStateToString(_tranState);
	}

	public String getTranStateDescription()
	{
		return tsqlTranStateToDescription(_tranState);
	}

	/**
	 * Get the String name of the transactionState
	 *
	 * @param state
	 * @return
	 */
	protected String tsqlTranStateToString(int state)
	{
		switch (state)
		{
			case TSQL_TRAN_IN_PROGRESS:
				return TSQL_TRANSTATE_NAMES[state];

			case TSQL_TRAN_SUCCEED:
				return TSQL_TRANSTATE_NAMES[state];

			case TSQL_STMT_ABORT:
				return TSQL_TRANSTATE_NAMES[state];

			case TSQL_TRAN_ABORT:
				return TSQL_TRANSTATE_NAMES[state];

			case TSQL_TRANSTATE_NOT_AVAILABLE:
				return TSQL_TRANSTATE_NAMES[state];

			default:
				return "TSQL_UNKNOWN_STATE("+state+")";
		}
	}
	protected String tsqlTranStateToDescription(int state)
	{
		switch (state)
		{
			case TSQL_TRAN_IN_PROGRESS:
				return TSQL_TRANSTATE_DESCRIPTIONS[state];

			case TSQL_TRAN_SUCCEED:
				return TSQL_TRANSTATE_DESCRIPTIONS[state];

			case TSQL_STMT_ABORT:
				return TSQL_TRANSTATE_DESCRIPTIONS[state];

			case TSQL_TRAN_ABORT:
				return TSQL_TRANSTATE_DESCRIPTIONS[state];

			case TSQL_TRANSTATE_NOT_AVAILABLE:
				return TSQL_TRANSTATE_DESCRIPTIONS[state];

			default:
				return "TSQL_UNKNOWN_STATE("+state+")";
		}
	}
	
	/** 
	 * @return "" if no locks, otherwise a HTML TABLE, with the headers: DB, Table, Type, Count
	 */
	public String getLockListTableAsHtmlTable()
	{
		if (_lockList.size() == 0)
			return "";

		StringBuilder sb = new StringBuilder("<TABLE BORDER=1>");
		sb.append("<TR> <TH>DB</TH> <TH>Table</TH> <TH>Type</TH> <TH>Count</TH> </TR>");
		for (LockRecord lr : _lockList)
		{
			sb.append("<TR>");
			sb.append("<TD>").append(lr._dbname   ).append("</TD>");
			sb.append("<TD>").append(lr._tableName).append("</TD>");
			sb.append("<TD>").append(lr._lockType ).append("</TD>");
			sb.append("<TD>").append(lr._lockCount).append("</TD>");
			sb.append("</TR>");
		}
		sb.append("</TABLE>");
		return sb.toString();
	}

	
	
	
	public static class LockRecord
	{
		public String _dbname    = "";
		public String _tableName = "";
		public String _lockType  = "";
		public int    _lockCount = 0;

//		public LockRecord(String dbname, String tableName, String lockType, int lockCount)
//		{
//			_dbname    = dbname;
//			_tableName = tableName;
//			_lockType  = lockType;
//			_lockCount = lockCount;
//		}

		public LockRecord(String dbname, String tableName, int lockType, int lockCount)
		{
			_dbname    = dbname;
			_tableName = tableName;
			_lockType  = getAseLockType(lockType);
			_lockCount = lockCount;
		}
		public LockRecord(String dbname, String tableName, String lockType, int lockCount)
		{
			_dbname    = dbname;
			_tableName = tableName;
			_lockType  = lockType;
			_lockCount = lockCount;
		}
	}


	public static String getAseLockType(int type)
	{
		// below values grabbed from ASE 15.7 SP102: 
		//            select 'case '+convert(char(5),number)+': return "'+name+'";' from master..spt_values where type in ('L') and number != -1
		switch (type)
		{
		case 1   : return "Ex_table";
		case 2   : return "Sh_table";
		case 3   : return "Ex_intent";
		case 4   : return "Sh_intent";
		case 5   : return "Ex_page";
		case 6   : return "Sh_page";
		case 7   : return "Update_page";
		case 8   : return "Ex_row";
		case 9   : return "Sh_row";
		case 10  : return "Update_row";
		case 11  : return "Sh_nextkey";
		case 257 : return "Ex_table-blk";
		case 258 : return "Sh_table-blk";
		case 259 : return "Ex_intent-blk";
		case 260 : return "Sh_intent-blk";
		case 261 : return "Ex_page-blk";
		case 262 : return "Sh_page-blk";
		case 263 : return "Update_page-blk";
		case 264 : return "Ex_row-blk";
		case 265 : return "Sh_row-blk";
		case 266 : return "Update_row-blk";
		case 267 : return "Sh_nextkey-blk";
		case 513 : return "Ex_table-demand";
		case 514 : return "Sh_table-demand";
		case 515 : return "Ex_intent-demand";
		case 516 : return "Sh_intent-demand";
		case 517 : return "Ex_page-demand";
		case 518 : return "Sh_page-demand";
		case 519 : return "Update_page-demand";
		case 520 : return "Ex_row-demand";
		case 521 : return "Sh_row-demand";
		case 522 : return "Update_row-demand";
		case 523 : return "Sh_nextkey-demand";
		case 769 : return "Ex_table-demand-blk";
		case 770 : return "Sh_table-demand-blk";
		case 771 : return "Ex_intent-demand-blk";
		case 772 : return "Sh_intent-demand-blk";
		case 773 : return "Ex_page-demand-blk";
		case 774 : return "Sh_page-demand-blk";
		case 775 : return "Update_page-demand-blk";
		case 776 : return "Ex_row-demand-blk";
		case 777 : return "Sh_row-demand-blk";
		case 778 : return "Update_row-demand-blk";
		case 779 : return "Sh_nextkey-demand-blk";
		case 1025: return "Ex_table-request";
		case 1026: return "Sh_table-request";
		case 1027: return "Ex_intent-request";
		case 1028: return "Sh_intent-request";
		case 1029: return "Ex_page-request";
		case 1030: return "Sh_page-request";
		case 1031: return "Update_page-request";
		case 1032: return "Ex_row-request";
		case 1033: return "Sh_row-request";
		case 1034: return "Update_row-request";
		case 1035: return "Sh_nextkey-request";
		case 1537: return "Ex_table-demand-request";
		case 1538: return "Sh_table-demand-request";
		case 1539: return "Ex_intent-demand-request";
		case 1540: return "Sh_intent-demand-request";
		case 1541: return "Ex_page-demand-request";
		case 1542: return "Sh_page-demand-request";
		case 1543: return "Update_page-demand-request";
		case 1544: return "Ex_row-demand-request";
		case 1545: return "Sh_row-demand-request";
		case 1546: return "Update_row-demand-request";
		case 1547: return "Sh_nextkey-demand-request";
		}
		return "unknown("+type+")";
	}

}

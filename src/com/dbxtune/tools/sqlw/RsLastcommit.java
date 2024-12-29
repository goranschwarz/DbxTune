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
package com.dbxtune.tools.sqlw;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.Logger;

import com.dbxtune.utils.AseConnectionFactory;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.RepServerUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

public class RsLastcommit
{
	private static Logger _logger = Logger.getLogger(RsLastcommit.class);

	private static final String SQL = 
		"select origin, origin_qid, secondary_qid, origin_time, dest_commit_time, sample_time = getdate() " +
		"from rs_lastcommit ";
//		"where origin != 0";

	private HashMap<Integer, RsLastcommitEntry> _map = new HashMap<Integer, RsLastcommitEntry>();

	public Set<Integer> keySet()
	{
		return _map.keySet();
	}
	public Collection<RsLastcommitEntry> values()
	{
		return _map.values();
	}

	/**
	 * Get RsLastcommitEntry object for a specific origin/source
	 * @param origin Origin RepServer DBID
	 * @return RsLastcommitEntry
	 */
	public RsLastcommitEntry getRsLastcommitEntry(int origin)
	{
		return _map.get(origin);
	}

	public String getOriginQid(int origin)
	{
		RsLastcommitEntry entry = _map.get(origin);
		if (entry == null)
			return null;
		
		return entry._origin_qid;
	}
	
	public static class OriginNotFoundException extends Exception
	{
		private static final long serialVersionUID = 1L;
		public OriginNotFoundException(String reason)
		{
			super(reason);
		}
	}
	
	/**
	 * If the passed <code>originQid</code> is less than the value fetched from the rs_lastcommit 
	 * table in the destination database this method will return false
	 * 
	 * @param origin     origin RepServer database id
	 * @param originQid  The Origin QID to check if it has been applied/replicated in the destination server. 
	 *                   This is typically an entry on the RepServer Stable Device Queue System
	 * @param seqNo      Just for DEBUGGING purposes
	 * @param command    Just for DEBUGGING purposes
	 * @return true or false
	 * @throws OriginNotFoundException
	 */
	public boolean hasOqidBeenApplied(int origin, String originQid, String command, int seqNo)
	throws OriginNotFoundException
	{
		if (originQid == null)
			throw new IllegalArgumentException("hasOqidBeenApplied(origin="+origin+", originQid='"+originQid+"'): originQid can't be null");

		RsLastcommitEntry entry = _map.get(origin);
		if (entry == null)
			throw new OriginNotFoundException("Origin "+origin+" can't be found in the Map");

		String rsLcQid = entry._origin_qid;
		
		boolean trustSecondaryQid = false;
		if (entry._secondary_qid != null)
		{
			String secQidStripAllZero = entry._secondary_qid.replace("0", "");
			if (StringUtil.hasValue(secQidStripAllZero))
			{
				trustSecondaryQid = true;
			
				// Should we also use the SecondaryQID as the QID????
				// Do this later after we have tested much more... I dont know how it works if we have several System Transaction after each other...
				//rsLcQid = entry._secondary_qid;
			}
		}

		// 
		// The origin queue ID (OQID) for Sybase Adaptive Server Replication Agent is 36 bytes and contains the following fields:
        // 
		// Bytes     Contents
		// --------- --------------------------------------------------------------------
		// 1-2   (2) Generation number
		// 3-8   (6) Log page timestamp
		// 9-14  (6) Rowid of this tran
		// 15-20 (6) Rowid of begin tran
		// 21-28 (8) Datetime of begin tran
		// 29-30 (2) Reserved for RepAgent to delete orphans
		// 31-32 (2) Unused
		// 33-34 (2) Appended by TD (Transaction Delivery module) for uniqueness
		// 35-36 (2) Appended by MD (Message Delivery module)
		// ------------------------------------------------------------------------------
		// The origin queue ID for Replication Agent 15.0 is 32 bytes
		//
		String modOriginQid = originQid.substring(0, originQid.length()-8); // -8 would be 4 bytes removed at the end (see above, 4 bytes for TD & MD module)
		String modRsLcQid   = rsLcQid  .substring(0, rsLcQid  .length()-8); // -8 would be 4 bytes removed at the end (see above, 4 bytes for TD & MD module)

//		int diff = originQid.compareTo(rsLcQid);
		int diff = modOriginQid.compareTo(modRsLcQid);

		// FIXME: Investigate more on this
		// Lets also look at the _secondary_qid
		// it might be a system transaction
		// when normal transaction: secondary_qid is 0x000000000000000000000000000000000000000000000000000000000000000000000000
//		int diff = originQid.compareTo(entry._secondary_qid);

		// FIXME: how about if this is a NEW Connection and nothing has yet been replicated...
		//        this should probably work for this situation as well

		boolean ret = (diff <= 0);
		if (trustSecondaryQid)
			ret = (diff < 0);
		
		//FIXME: above we need to do more... In some cases the LAST system transaction is SKIPPED from the output
		//       so some more investigations has to be done... (probably that when it's a system transaction it should be EQUAL to OQID (or SecondaryQID) and not smaller...
		//       but when this happened I didn't have a test system available
		
		if (_logger.isDebugEnabled())
		{
			_logger.debug("hasOqidBeenApplied: trustSecondaryQid="+trustSecondaryQid+", returns "+(ret ? "TRUE":"false")+", origin="+origin + "   " + (ret ? "--- DISCARD":" +++ keep") );
			_logger.debug("                         queue OQID = '" + originQid + "'   modQueueQid = '" + modOriginQid + "', CmdTextSeqNo="+seqNo+", CmdText='"+command+"'");
			_logger.debug("                 rs_lastcommit OQID = '" + rsLcQid   + "'   modRsLcQid  = '" + modRsLcQid   + "', CmdTextSeqNo="+seqNo+", CmdText='"+command+"'");
			_logger.debug("");
		}

		return ret;
	}

	public static RsLastcommit getRsLastcommit(String srvname, String hostPortStr, String dbname, String username, String password)
	throws SQLException
	{
		if (StringUtil.isNullOrBlank(hostPortStr))
			hostPortStr = srvname;

		Connection conn = null;
		try
		{
			conn = AseConnectionFactory.getConnection(hostPortStr, dbname, username, password, "getRsLastcommit");
			AseConnectionUtils.useDbname(conn, dbname);

			RsLastcommit rsLastcommit = getRsLastcommit(conn, srvname, dbname, false);

			conn.close();

			return rsLastcommit;
		}
		catch (SQLException e)
		{
			StringBuffer sb = new StringBuffer();
			while (e != null)
			{
				sb.append( "\n" );
				sb.append( e.getMessage() );
				e = e.getNextException();
			}
			String msg = 
				"When trying to login to '"+srvname+"."+dbname+"' with user '"+username+"'.\n" +
				"This was done while get records from the 'rs_lastcommit' table.\n" +
				"\n" +
				"Connection FAILED." +
				"\n" +
				"\n" + 
				sb.toString();
//			JOptionPane.showMessageDialog(null, msg, "getRsLastcommit - connect check", JOptionPane.ERROR_MESSAGE);
			SwingUtils.showErrorMessage(null, "getRsLastcommit - connect check", msg, e);
		}
		catch (Exception e)
		{
			String msg = 
				"When trying to login to '"+srvname+"."+dbname+"' with user '"+username+"'.\n" +
				"This was done while get records from the 'rs_lastcommit' table.\n" +
				"\n" +
				"Connection FAILED." +
				"\n" +
				"\n" + 
				e.toString();
//			JOptionPane.showMessageDialog(null, msg,  "getRsLastcommit - connect check", JOptionPane.ERROR_MESSAGE);
			SwingUtils.showErrorMessage(null, "getRsLastcommit - connect check", msg, e);
		}
		return null;
	}
	public static RsLastcommit getRsLastcommit(Connection conn, String srvname, String dbname)
	throws SQLException
	{
		return getRsLastcommit(conn, srvname, dbname, true);
	}
	/**
	 * 
	 * @param conn    Replication Server Connection
	 * @param srvname Name of destination server
	 * @param dbname  Name of destination database
	 * @return null if failures, RsLastcommit when it has entries
	 */
	public static RsLastcommit getRsLastcommit(Connection conn, String srvname, String dbname, boolean useRsGw)
	throws SQLException
	{
		String cmd = SQL;
		boolean throwInFinally = true;
		try
		{
			// create a object which will be returned.
			RsLastcommit rsLastcommit = new RsLastcommit();

			// Connect to destination via RepServer
			if (useRsGw)
				RepServerUtils.connectGwDb(conn, srvname, dbname);
			
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(cmd);
		
			boolean rows = false;
			while (rs.next())
			{
				rows = true;

				int       origin           = rs.getInt(1);
				String    origin_qid       = rs.getString(2);
				String    secondary_qid    = rs.getString(3);
				Timestamp origin_time      = rs.getTimestamp(4);
				Timestamp dest_commit_time = rs.getTimestamp(5);
				Timestamp sample_time      = rs.getTimestamp(6);

				// Create a new object
				RsLastcommitEntry entry = new RsLastcommitEntry(origin, origin_qid, secondary_qid, origin_time, dest_commit_time, sample_time);
				
				// add it to the list
				rsLastcommit._map.put(origin, entry);
			}
			rs.close();
			stmt.close();

			if ( ! rows )
			{
				_logger.warn("Can't find any rows in the rs_lastcommit in server '"+srvname+"' database '"+dbname+"'.");
				return null;
			}
			return rsLastcommit;
		}
		catch (SQLException sqle)
		{
			String msg = "Problems when executing '"+cmd+"' in Server '"+srvname+"'. ";
			_logger.error(msg + sqle);

			throwInFinally = false;
			throw sqle;
//			return null;
		}
		finally
		{
			if (useRsGw)
			{
				try
				{
					RepServerUtils.disconnectGw(conn);
				}
				catch(SQLException e)
				{
					if (throwInFinally)
						throw e;
				}
			}
		}
	}

}

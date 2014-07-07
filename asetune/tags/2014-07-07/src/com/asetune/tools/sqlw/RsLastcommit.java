package com.asetune.tools.sqlw;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.RepServerUtils;

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
	 * @return true or false
	 * @throws OriginNotFoundException
	 */
	public boolean hasOqidBeenApplied(int origin, String originQid)
	throws OriginNotFoundException
	{
		if (originQid == null)
			throw new IllegalArgumentException("hasOqidBeenApplied(origin="+origin+", originQid='"+originQid+"'): originQid can't be null");

		RsLastcommitEntry entry = _map.get(origin);
		if (entry == null)
			throw new OriginNotFoundException("Origin "+origin+" can't be found in the Map");

		String rsLcQid = entry._origin_qid;
//		if (entry._secondary_qid != null && ! entry._secondary_qid.equals("000000000000000000000000000000000000000000000000000000000000000000000000"))
//			rsLcQid = entry._secondary_qid;
		
		int diff = originQid.compareTo(rsLcQid);

		// FIXME: Investigate more on this
		// Lets also look at the _secondary_qid
		// it might be a system transaction
		// when normal transaction: secondary_qid is 0x000000000000000000000000000000000000000000000000000000000000000000000000
//		int diff = originQid.compareTo(entry._secondary_qid);

		// FIXME: how about if this is a NEW Connection and nothing has yet been replicated...
		//        this should probably work for this situation as well
		
//		System.out.println("hasOqidBeenApplied: returns "+(diff <= 0)+", origin="+origin);
//		System.out.println("                         queue OQID = '"+originQid + "'.");
//		System.out.println("                 rs_lastcommit OQID = '"+entry._origin_qid + "'.");
//		System.out.println();

//		return (diff <= 0);
		return (diff < 0);
	}

	public static RsLastcommit getRsLastcommit(String srvname, String dbname, String username, String password)
	throws SQLException
	{
		Connection conn = null;
		try
		{
			conn = AseConnectionFactory.getConnection(srvname, dbname, username, password, "getRsLastcommit");
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
			JOptionPane.showMessageDialog(null, msg, "getRsLastcommit - connect check", JOptionPane.ERROR_MESSAGE);
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
			JOptionPane.showMessageDialog(null, msg,  "getRsLastcommit - connect check", JOptionPane.ERROR_MESSAGE);
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

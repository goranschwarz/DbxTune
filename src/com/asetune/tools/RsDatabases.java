package com.asetune.tools;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.asetune.utils.RepServerUtils;

public class RsDatabases
{
	private static Logger _logger = Logger.getLogger(RsLastcommit.class);

	private static final String SQL = "select * from rs_databases ";

	private HashMap<Integer, Entry> _map = new HashMap<Integer, Entry>();

	/**
	 * Get RSDatabase Entry object for a specific repserver dbid
	 * @param dbid RepServer DBID
	 * @return Entry
	 */
	public Entry getEntry(int dbid)
	{
		return _map.get(dbid);
	}

	public Entry getEntry(String srvDb)
	{
		int dot = srvDb.indexOf('.');
		String srv = srvDb.substring(0, dot);
		String db  = srvDb.substring(dot+1);

		return getEntry(srv, db);
	}
	public Entry getEntry(String srv, String db)
	{
		for (Entry e : _map.values())
		{
			if (e._dsname.equals(srv) && e._dbname.equals(db))
				return e;
		}
		return null;
	}

	public int getDbid(String srvDb)
	{
		Entry e = getEntry(srvDb);
		if (e == null)
			return -1;
		return e._dbid;
	}
	public int getDbid(String srv, String db)
	{
		Entry e = getEntry(srv, db);
		if (e == null)
			return -1;
		return e._dbid;
	}
	
	public String getSrvDb(int dbid)
	{
		Entry e = getEntry(dbid);
		if (e == null)
			return null;
		return e._dsname + "." + e._dbname;
	}

	/**
	 * 
	 * @param conn    Replication Server Connection
	 * @param srvname Name of destination server
	 * @param dbname  Name of destination database
	 * @return null if failures, RsLastcommit when it has entries
	 */
	public static RsDatabases getRsDatabases(Connection conn)
	{
		String cmd = SQL;
		try
		{
			RsDatabases rsDatabases = new RsDatabases();

			// Connect to destination via RepServer
			RepServerUtils.connectGwRssd(conn);
			
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(cmd);
		
			boolean rows = false;
			while (rs.next())
			{
				rows = true;

				Entry e = new Entry();
				e._dsname          =        rs.getString("dsname");
				e._dbname          =        rs.getString("dbname");
				e._dbid            =        rs.getInt   ("dbid");
				e._dist_status     =        rs.getInt   ("dist_status");
				e._src_status      =        rs.getInt   ("src_status");
				e._attributes      =        rs.getInt   ("attributes");
				e._errorclassid    = "0x" + rs.getString("errorclassid");
				e._funcclassid     = "0x" + rs.getString("funcclassid");
				e._prsid           =        rs.getInt   ("prsid");
				e._rowtype         =        rs.getInt   ("rowtype");
				e._sorto_status    =        rs.getInt   ("sorto_status");
				e._ltype           =        rs.getString("ltype");
				e._ptype           =        rs.getString("ptype");
				e._ldbid           =        rs.getInt   ("ldbid");
				e._enable_seq      =        rs.getInt   ("enable_seq");
				e._rs_errorclassid = "0x" + rs.getString("rs_errorclassid");

				// add it to the list
				rsDatabases._map.put(e._dbid, e);
			}
			rs.close();
			stmt.close();

			if ( ! rows )
			{
				_logger.warn("Can't find any rows in the rs_databases in RSSD server.");
				return null;
			}
			return rsDatabases;
		}
		catch (SQLException sqle)
		{
			String msg = "Problems when executing '"+cmd+"' in RSD Server.";
			_logger.error(msg + sqle);

			return null;
		}
		finally
		{
			try
			{
				RepServerUtils.disconnectGw(conn);
			}
			catch(SQLException e)
			{
			}
		}
	}


	public static class Entry
	{
// sp_help rs_databases
//		 Column_name     Type    Length Prec Scale Nulls Default_name               Rule_name Access_Rule_name Computed_Column_object Identity  
//		 --------------- ------- ------ ---- ----- ----- -------------------------- --------- ---------------- ---------------------- ----------
//		 dsname          varchar 30     NULL NULL  0     NULL                       NULL      NULL             NULL                   0         
//		 dbname          varchar 30     NULL NULL  0     NULL                       NULL      NULL             NULL                   0         
//		 dbid            int     4      NULL NULL  0     NULL                       NULL      NULL             NULL                   0         
//		 dist_status     int     4      NULL NULL  0     NULL                       NULL      NULL             NULL                   0         
//		 src_status      int     4      NULL NULL  0     NULL                       NULL      NULL             NULL                   0         
//		 attributes      tinyint 1      NULL NULL  0     NULL                       NULL      NULL             NULL                   0         
//		 errorclassid    rs_id   8      NULL NULL  0     NULL                       NULL      NULL             NULL                   0         
//		 funcclassid     rs_id   8      NULL NULL  0     NULL                       NULL      NULL             NULL                   0         
//		 prsid           int     4      NULL NULL  0     NULL                       NULL      NULL             NULL                   0         
//		 rowtype         tinyint 1      NULL NULL  0     NULL                       NULL      NULL             NULL                   0         
//		 sorto_status    tinyint 1      NULL NULL  0     NULL                       NULL      NULL             NULL                   0         
//		 ltype           char    1      NULL NULL  0     NULL                       NULL      NULL             NULL                   0         
//		 ptype           char    1      NULL NULL  0     NULL                       NULL      NULL             NULL                   0         
//		 ldbid           int     4      NULL NULL  0     NULL                       NULL      NULL             NULL                   0         
//		 enable_seq      int     4      NULL NULL  0     NULL                       NULL      NULL             NULL                   0         
//		 rs_errorclassid rs_id   8      NULL NULL  0     rs_databas_rs_err_82096302 NULL      NULL             NULL                   0         
// rs_id = binary(8)
		
		public String _dsname          = null;  //   varchar
		public String _dbname          = null;  //   varchar
		public int    _dbid            = 0;     //   int    
		public int    _dist_status     = 0;     //   int    
		public int    _src_status      = 0;     //   int    
		public int    _attributes      = 0;     //   tinyint
		public String _errorclassid    = null;  //   rs_id   (binary 8)
		public String _funcclassid     = null;  //   rs_id   (binary 8)
		public int    _prsid           = 0;     //   int    
		public int    _rowtype         = 0;     //   tinyint
		public int    _sorto_status    = 0;     //   tinyint
		public String _ltype           = null;  //   char   
		public String _ptype           = null;  //   char   
		public int    _ldbid           = 0;     //   int    
		public int    _enable_seq      = 0;     //   int    
		public String _rs_errorclassid = null;  //rs_id (binary 8)

		public String _prsName           = null;    
		public String _errorclassName    = null;
		public String _funcclassName     = null;
		public String _rs_errorclassName = null;
	}
}

package com.asetune.tools.sqlw;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.apache.log4j.Logger;


public class RsLastcommitEntry 
{
	private static Logger _logger          = Logger.getLogger(RsLastcommitEntry.class);

	private static final String _sql = 
		"select origin, origin_qid, secondary_qid, origin_time, dest_commit_time, sample_time = getdate() \n" +
		"from DB_NAME..rs_lastcommit \n" +
		"where origin = RS_DB_ID";

	public int       _origin           = 0;
	public String    _origin_qid       = null;
	public String    _secondary_qid    = null;
	public Timestamp _origin_time      = null;
	public Timestamp _dest_commit_time = null;
	public Timestamp _sample_time      = null;

	public long      _createTime      = System.currentTimeMillis();
	
	// A parsed OQID
	public int       _generationId     = 0;
	public String    _logTimestamp     = null;
	public int       _logPage          = 0;
	public int       _logRow           = 0;

	public RsLastcommitEntry()
	{
	}

	public RsLastcommitEntry(int origin, String origin_qid, String secondary_qid, Timestamp origin_time, Timestamp dest_commit_time, Timestamp sample_time)
	{
		_origin           = origin;
		_origin_qid       = origin_qid;
		_secondary_qid    = secondary_qid;
		_origin_time      = origin_time;
		_dest_commit_time = dest_commit_time;
		_sample_time      = sample_time;

		parseOriginQId();
	}

	/** Give a string representation of the object */
	@Override
	public String toString()
	{
		return "origin='"+_origin+"', originQId='"+_origin_qid+"', secondaryQId='"+_secondary_qid+"', originTime='"+_origin_time+"', destTime='"+_dest_commit_time+"', sampleTime='"+_sample_time+"'. Parsed: genId='"+_generationId+"', logTimestamp='"+_logTimestamp+"', logPage='"+_logPage+"', logRow='"+_logRow+"'.";
	}

	/**
	 * How many seconds is the destination database lagging behand.
	 * The formula is the difference between "origin commit time" and 
	 * "destination commit time", note that this may be wrong due to 
	 * the fact that the clocks on the different machines are <b>not</b> in synch.
	 *   
	 * @return number of seconds between origin and destination commit time
	 */
	public int getLatencyInSeconds()
	{
		if ( _origin_time      == null ) throw new NullPointerException("RsLastCommit._origin_time can't be null.");
		if ( _dest_commit_time == null ) throw new NullPointerException("RsLastCommit._dest_commit_time can't be null.");
		if ( _sample_time      == null ) throw new NullPointerException("RsLastCommit._sample_time can't be null.");

		return (int)( _dest_commit_time.getTime() - _origin_time.getTime() ) / 1000; 
	}
	/** 
	 * How many minutes is the destination database lagging behand. 
	 * @see getLatencyInSeconds() for more info 
	 */
	public int getLatencyInMinutes()
	{
		return getLatencyInSeconds() / 60;
	}

	/** How old data are we looking at in the destination database. */
	public int getOriginDataAgeInSeconds()
	{
		if ( _origin_time      == null ) throw new NullPointerException("RsLastCommit._origin_time can't be null.");
		if ( _dest_commit_time == null ) throw new NullPointerException("RsLastCommit._dest_commit_time can't be null.");
		if ( _sample_time      == null ) throw new NullPointerException("RsLastCommit._sample_time can't be null.");

		return (int)( _sample_time.getTime() - _origin_time.getTime() ) / 1000; 
	}
	/** @see getOriginDataAgeInSeconds() for more info*/
	public int getOriginDataAgeInMinutes()
	{
		return getOriginDataAgeInSeconds() / 60;
	}

	/** How many seconds was it since we last got <b>any</b> data replicated to the destination database. */
	public int getDestinationDataAgeInSeconds()
	{
		if ( _origin_time      == null ) throw new NullPointerException("RsLastCommit._origin_time can't be null.");
		if ( _dest_commit_time == null ) throw new NullPointerException("RsLastCommit._dest_commit_time can't be null.");
		if ( _sample_time      == null ) throw new NullPointerException("RsLastCommit._sample_time can't be null.");

		return (int)( _sample_time.getTime() - _dest_commit_time.getTime() ) / 1000; 
	}
	/** @see getDestinationDataAgeInSeconds() for more info*/
	public int getDestinationDataAgeInMinutes()
	{
		return getDestinationDataAgeInSeconds() / 60;
	}

	/**
	 * Get information from the Replication Servers system table rs_lastcommit stored in the destination database.
	 * From here we can then calculate latency and other various stuff.
	 * It is also used to check for stranded transactions in a source database.
	 * 
	 * @param conn    A connection to the ASE server holding the destination database.
	 * @param dbname  The name of the destination database.
	 * @param dbid    Repserver DBID of the <b>source</b> database
	 * @return
	 * @throws SQLException
	 */
//	public static RsLastcommitEntry getRsLastCommit(ManageDb mdb, String dbname, int dbid)
	public static RsLastcommitEntry getRsLastCommit(Connection conn, String srvname, String dbname, int dbid)
	{
		RsLastcommitEntry out = null;

		//SQL: select origin, origin_qid, secondary_qid, origin_time, dest_commit_time from DB_NAME..rs_lastcommit where origin = RS_DB_ID
		String cmd = _sql;
		cmd = cmd.replaceFirst("DB_NAME", dbname);
		cmd = cmd.replaceFirst("RS_DB_ID", Integer.toString(dbid));
	
		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(cmd);
		
			// Create a new object
			out = new RsLastcommitEntry();

			boolean rows = false;
			while (rs.next())
			{
				rows = true;
				out._origin           = rs.getInt(1);
				out._origin_qid       = rs.getString(2);
				out._secondary_qid    = rs.getString(3);
				out._origin_time      = rs.getTimestamp(4);
				out._dest_commit_time = rs.getTimestamp(5);
				out._sample_time      = rs.getTimestamp(6);
			}
			rs.close();
			stmt.close();

			if ( ! rows )
			{
//				_logger.warn("Can't find any rows in the rs_lastcommit for the '"+mdb.getManagedType()+"' in database '"+mdb.getSrvDbname()+"' for RepServer origin dbid '"+dbid+"'.");
				_logger.warn("Can't find any rows in the rs_lastcommit in database '"+dbname+"' for RepServer origin dbid '"+dbid+"'.");
				return null;
			}

			out.parseOriginQId();
		}
		catch (SQLException sqle)
		{
			String msg = "Problems when executing '"+cmd+"' in Server '"+srvname+"'. ";
			_logger.error(msg + sqle);
		}

		return out;
	}
	
	/** 
	 * Internaly used to parse a origin Queue ID into it's various parts 
	 */
	public void parseOriginQId()
	{
		String genId, logTs, logRowId, logPg, logRow, p4, p5, p6, p7, p8, p9;
		int start;
		int end;

		//System.out.println("_origin_qid='"+_origin_qid+"'.");


		// Database generation number used for recovering after reloading coordinated dumps
		start = 1; end = 2;
		genId = _origin_qid.substring( (start-1)*2, end*2 );

		// Log page timestamp for the current record.
		start = 3; end = 8;
		logTs = _origin_qid.substring( (start-1)*2, end*2 );

		// Row ID of the current row. Row ID = page number (4 bytes) + row number (2 bytes).
		start = 9; end = 14;
		logRowId = _origin_qid.substring( (start-1)*2, end*2 );

		start = 1; end = 4;
		logPg = logRowId.substring( (start-1)*2, end*2 );

		start = 5; end = 6;
		logRow = logRowId.substring( (start-1)*2, end*2 );

		// Row ID of the begin record for the oldest open transaction.
		start = 15; end = 20;
		p4 = _origin_qid.substring( (start-1)*2, end*2 );

		// Date and time of the begin record for the oldest open transaction.
		start = 21; end = 28;
		p5 = _origin_qid.substring( (start-1)*2, end*2 );

		// An extension used by the RepAgent to roll back orphan transactions.
		start = 29; end = 30;
		p6 = _origin_qid.substring( (start-1)*2, end*2 );

		// Unused
		start = 31; end = 32;
		p7 = _origin_qid.substring( (start-1)*2, end*2 );

		p8 = "";
		p9 = "";
		if ( _origin_qid.length() <= 36*2 )
		{
			// Applied by TD for uniquenes
			start = 32; end = 34;
			p8 = _origin_qid.substring( (start-1)*2, end*2 );
	
			// Applied by MD for uniquenes
			start = 35; end = 36;
			p9 = _origin_qid.substring( (start-1)*2, end*2 );
		}

		// Make HEX to INT
		_generationId     = Integer.parseInt(genId,  16);
		_logTimestamp     = logTs;
		_logPage          = Integer.parseInt(logPg,  16);;
		_logRow           = Integer.parseInt(logRow, 16);;
		
		if (_logger.isDebugEnabled())
		{
			_logger.debug("genId='"+genId+"', logTs='"+logTs+"', logRowId='"+logRowId+"'(logPage="+_logPage+",logRow="+_logRow+"), p4='"+p4+"', p5='"+p5+"', p6='"+p6+"', p7='"+p7+"', p8='"+p8+"', p9='"+p9+"'");
		}
	}
}

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
package com.dbxtune.utils;


public class RepServerDumpQueue
{
//	///////////////////////////////////////////////////////////////////////
//	///////////////////////////////////////////////////////////////////////
//	//// dump queue
//	///////////////////////////////////////////////////////////////////////
//	///////////////////////////////////////////////////////////////////////
//	/**
//	 * Dumps the Stable queue for a specific destination.
//	 * <p>
//	 * The returned list is sorted on the transaction start order not the commit order.
//	 * But since a WS is not applying in commit order but "first in first out" this would work...
//	 * But there are room for improvements.
//	 * <p>
//	 * Every row in the output list is a List itself, that list has first row with a "header"
//	 * then the commands comes.
//	 *
//	 * @param lsrv Logical server connaction name
//	 * @param ldb  Logical database connaction name
//	 * @param destLastcommit a RsLastcommitEntry record fetched from the destination database.
//	 * @param onlyXFirstRecords only read the first X rows from the repserver before giving up -1 would be to read all records.
//	 * @return A List of transactions
//	 */
//	public List dumpWsQueue(String lsrv, String ldb, RsLastcommitEntry destLastcommit, int onlyXFirstRecords)
//	throws ManageException
//	{
//		ifClosedThrow();
//
//		if (destLastcommit == null)
//		{
//			_logger.info("Skipping dumpWsQueue, a null pointer was sent in for the destLastcommit.");
//			return new LinkedList();
//		}
//
//		int rsdbid = getRsDbIdForLogicalConnection(lsrv, ldb);
//
//		String cmd = null;
//		try
//		{
//			// sysadmin dump_queue, q_number, q_type, seg, blk, cnt [, RSSD | client]
//			//   seg - blk
//			//    - Setting seg to -1 starts with the first active segment in the queue
//			//    - Setting seg to -2 starts with the first segment in the queue,
//			//      including any inactive segments retained by setting a save interval
//			//    - Setting seg to -1 and blk to -1 starts with the first undeleted block in the queue.
//			//    - Setting seg to -1 and blk to -2 starts with the first unread block in the queue.
//			//   cnt
//			//    - Specifies the number of blocks to dump. This number can span multiple
//			//      segments. If cnt is set to -1, the end of the current segment is the last block
//			//      dumped. If it is set to -2, the end of the queue is the last block dumped.
//			//
//			// so: rsdbid, 1, -1, 1, -1 = Take In queue and first segment read if from start (blk=1) and read only info on this segment (only span 1MB)
//			//
//			// The seems to work better (if last committed row is at block 64, then this hops over to next segment)
//			// = Take "first active segment", "first 'unread' block (whatever that means), read to end of current segment (with the exception if last tran starts at segment 64, it jumps over to next segment"
//			// sysadmin dump_queue, 111, 1, -1, -2, -1, client
//			//
//			cmd = "sysadmin dump_queue, "+rsdbid+", 1, -1, -2, -1, client";
//
//			//--------------------------
//			// Output example
//			//--------------------------
//			// Q Number    Q Type      Segment     Block       Row         Message Len Orgn Siteid Orgn Time                      Orgn Qid                                                                   Orgn User                      Tran Name                      Local Qid                                                                  Status      Tranid                                                                                                                                                                                                                                             Logical Orgn Siteid Version     Command Len Seq No.     Command
//			// ----------- ----------- ----------- ----------- ----------- ----------- ----------- ---------                      --------                                                                   ---------                      ---------                      ---------                                                                  ----------- ------                                                                                                                                                                                                                                             ------------------- ----------- ----------- ----------- -------
//			//        107           1          73           1           0         276         108 Oct  9 2007  9:10PM            0x0000000000025eaf0000f4d200080000f4d20008000099c2015ce2280000000000000000 sa                             _upd                           0x000000000000000000000000000000000000000000000000000000000000004900010000           4 0x0000000000025eaf000847313530676f72616e3100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000                 107        1100          19           0 begin transaction
//			//        107           1          73           1           1         756         108 Jan  1 1900 12:00AM            0x0000000000025eaf0000f4d2000a0000f4d20008000099c2015ce2280000000000000000 NULL                           NULL                           0x000000000000000000000000000000000000000000000000000000000000004900010001     2097152 0x0000000000025eaf000847313530676f72616e3100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000                 107        1100         233           0 update dbo.rsm_heartbeat set lastbeat='20071009 21:10:15:746' where dbid=108 and server='G150' and dbname='goran1' and seconds=10 and seconds_left=10 and spid=25 and lastbeat='20071009 21:09:54:653' and rsm_server='mredMonitor'
//			//        107           1          73           1           2         204         108 Oct  9 2007  9:10PM            0x0000000000025eaf0000f4d2000b0000f4d20008000099c2015ce2280000000000000000 NULL                           NULL                           0x000000000000000000000000000000000000000000000000000000000000004900010002           1 0x0000000000025eaf000847313530676f72616e3100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000                 107        1100          20           0 commit transaction
//			//--------------------------
//
//			_rsMsgHandler.setPrefix("dumpWsQueue(): ");
//
//			Statement stmt = _conn.createStatement();
//			if ( getQueryTimeout() > 0 )
//				stmt.setQueryTimeout( getQueryTimeout() );
//			ResultSet rs = stmt.executeQuery(cmd);
//
//			// To be able to "sort" the transaction and append info to them...
//			// the key would be: tranId, and the object would be a linked list with Strings
//			Hashtable transTable = new Hashtable();
//			// The list below will keep the "order" of transactions.
//			List transOrderedList = new LinkedList();
//
//			RsLastcommitEntry usedToParseOqid = new RsLastcommitEntry();
//
//			while (rs.next())
//			{
//				int       qNumber           = rs.getInt      (1);
//				int       qType             = rs.getInt      (2);
//				int       segment           = rs.getInt      (3);
//				int       block             = rs.getInt      (4);
//				int       row               = rs.getInt      (5);
//				int       messageLen        = rs.getInt      (6);
//				int       orgnSiteid        = rs.getInt      (7);
//				Timestamp orgnTime          = rs.getTimestamp(8);
//				String    orgnQid           = rs.getString   (9);
//				String    orgnUser          = rs.getString   (10);
//				String    tranName          = rs.getString   (11);
//				String    localQid          = rs.getString   (12);
//				int       status            = rs.getInt      (13);
//				String    tranid            = rs.getString   (14);
//				int       logicalOrgnSiteid = rs.getInt      (15);
//				int       version           = rs.getInt      (16);
//				int       commandLen        = rs.getInt      (17);
//				int       seqNo             = rs.getInt      (18);
//				String    command           = rs.getString   (19);
//
//				boolean alreadyAppliedToDest = false;
//				if (destLastcommit._origin_qid.compareTo(orgnQid) > 0)
//				{
//					alreadyAppliedToDest = true;
//				}
//
//				if ( ! alreadyAppliedToDest )
//				{
//					List trans = (List)transTable.get(tranid);
//					if (trans == null)
//					{
//						trans = new LinkedList();
//						transTable.put(tranid, trans);
//						transOrderedList.add(trans);
//
//						// This if we want various parts of the OQID
//						usedToParseOqid._origin_qid = orgnQid;
//						usedToParseOqid.parseOriginQId();
//
//						// Add a first row, which would contain info about the transaction
//						String header
//							="originSiteId='"    + orgnSiteid
//							+"', originTime='"   + orgnTime
//							+"', originUser='"   + orgnUser
//							+"', TranName='"     + tranName
//							+"', generationId='" + usedToParseOqid._generationId
//							+"', logPage='"      + usedToParseOqid._logPage
//							+"', logRow='"       + usedToParseOqid._logRow
//							+"', LogTs='"        + usedToParseOqid._logTimestamp
//							+"'.";
//						trans.add(header);
//					}
//					if (seqNo == 0)
//						trans.add(command);
//					else
//					{
//						int listPos = trans.size()-1;
//						String cmdInList = (String) trans.get( listPos );
//						cmdInList += command;
//						trans.set(listPos, cmdInList);
//					}
//				}
//
//				if ( _logger.isDebugEnabled() )
//				{
//					_logger.debug("dumpWsQueue(alreadyAppliedToDest="+alreadyAppliedToDest+"): qNumber='"+qNumber+"', qType='"+qType+"', segment='"+segment
//						+"', block='"+block+"', row='"+row+"', messageLen='"+messageLen+"', orgnSiteid='"+orgnSiteid
//						+"', orgnTime='"+orgnTime+"', orgnQid='"+orgnQid+"', orgnUser='"+orgnUser+"', tranName='"+tranName
//						+"', localQid='"+localQid+"', status='"+status+"', tranid='"+tranid+"', logicalOrgnSiteid='"+logicalOrgnSiteid
//						+"', version='"+version+"', commandLen='"+commandLen+"', seqNo='"+seqNo+"', command='"+command+"'.");
//				}
//
//				// If negative number, log everything
//				if (onlyXFirstRecords > 0)
//				{
//					onlyXFirstRecords--;
//					if (onlyXFirstRecords == 0)
//						break;
//				}
//			}
//			rs.close();
//			stmt.close();
//
//			return transOrderedList;
//		}
//		catch (SQLException sqle)
//		{
//			checkForProblems(sqle, cmd);
//
//			if (sqle instanceof EedInfo)
//			{
//				EedInfo sybsqle = (EedInfo) sqle;
//				if (sybsqle.getSeverity() == 0)
//				{
//					String msg = "Got SQLException when executing '"+cmd+"' in RepServer '"+getSrvDbname()+"'. But Severity was 0, so I'm going to skip this. More info about the message";
//					_logger.debug(msg + sqlExceptionToString(sqle));
//
//					return null;
//				}
//			}
//
//			// if no queue entries was found... RepServer isn't sending us a Empty ResultSet
//			// so we need to look for: java.sql.SQLException: JZ0R2: No result set for this query.
//			if (sqle.getSQLState().equals("JZ0R2"))
//			{
//				_logger.info("When executing '"+cmd+"' we got a empty resultset, so the queue must be empty...");
//				return new LinkedList();
//			}
//			String msg = "Problems when executing '"+cmd+"' in RepServer '"+getSrvDbname()+"'.";
//			_logger.error(msg + sqlExceptionToString(sqle));
//			throw new ManageException(msg, sqle);
//		}
//	}

}

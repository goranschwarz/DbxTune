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
/* NOTE: remove the package name if you compile/run it from outside the asetune package, this will probably make life easier */
package com.asetune.parser.sybase.iq;

/*-------------------------------------------------------------------------------------------------
 * IF YOU MAKE CHANGES/ENHANCEMENTS TO THE BELOW CODE 
 * PLEASE SEND THE FILE BACK TO ME SO I ALSO CAN MAKE THE SAME ENHANCEMENTS
 * 
 * This is a simple parser that takes a iqmsg file and tries to sort out what user applied
 * data to IQ and on what tables
 * It also gets number of rows affected, and how long the operation took (whole operation, Pass 1 and Pass 2)
 * 
 * The idea behind the parser is to establish what tables are considered as "slow"
 * but also establish a "profile" of what tables gets modified the most
 * 
 * Here is an example output of one connection:
 *      #######################################################################################
 *      Start='12/08 16:36:08', end='12/09 09:12:30', lenth(HH:MM)='16:36'.
 *      User='iq_xxxxx_x_maint', ConnID='0000035309'.
 *      Only top=10 will be presented.
 *      Cancel Requests=2990
 *      Replication Server has worked on this...
 *      iq_xxxxx_x_maint       32948 UPDATE  rs_lastcommit                       307        4       97       47          0,0      32948      32948          1,0         107,3 Only SINGLE ROW
 *      #######################################################################################
 *      User/connid          OpCount Oper    Table                          Seconds  MaxSec   UpdP1Sec UpdP2Sec AvgTimePerOp SumRows    SingleRows AvgRowsPerOp avgRowsPerSec Note
 *      -------------------- ------- ------- ------------------------------ -------- -------- -------- -------- ------------ ---------- ---------- ------------ ------------- -------------------------
 *      iq_xxxxx_x_maint        2978 INSERT  xxxxxxxxxxx                        1635        3        0        0          0,5     120635          0         40,5          73,8 
 *      iq_xxxxx_x_maint       65167 UPDATE  yyyyyyyyyyy                        1462        2     1094      135          0,0      68772      64966          1,1          47,0 
 *      iq_xxxxx_x_maint        4301 INSERT  zzzzzzzzzzzzzzzzz                   374        1        0        0          0,1     247210          0         57,5         661,0 
 *      iq_xxxxx_x_maint         422 INSERT  aaaaaaaaaaaaaaaaaaaaaa              348        7        0        0          0,8      33189          0         78,6          95,4 
 *      iq_xxxxx_x_maint       36482 UPDATE  bbbbbbbbbbbbbbb                     311        2      111       51          0,0     137338      33491          3,8         441,6 
 *      iq_xxxxx_x_maint       32948 UPDATE  rs_lastcommit                       307        4       97       47          0,0      32948      32948          1,0         107,3 Only SINGLE ROW
 *      iq_xxxxx_x_maint        1167 INSERT  cccccccccccccccccccc                251        3        0        0          0,2      27691          0         23,7         110,3 
 *      iq_xxxxx_x_maint        6892 INSERT  dddddddddddddddddd                  217        2        0        0          0,0    1260096          0        182,8        5806,9 
 *      iq_xxxxx_x_maint        8077 UPDATE  eeeeeeeeeeeeeee                     207        2      115       55          0,0       8077       8077          1,0          39,0 Only SINGLE ROW
 *      iq_xxxxx_x_maint       17660 UPDATE  fff                                 131        1       37        4          0,0      22691      17506          1,3         173,2 
 *      ==================== ======= ======= ============================== ======== ======== ======== ======== ============ ========== ========== ============ ============= =========================
 *      iq_xxxxx_x_maint      205475 SUMMARY SummaryForAllTables                5850        0     1615      322          0,0    3652119     169938         17,8         624,3 
 * 
 * goran_schwarz@hotmail.com, goran.schwarz@sap.com 
 *-------------------------------------------------------------------------------------------------*/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IqLogParser
{
	/**
	 * Holds a mapping from a connectionId to a specific UserName
	 * Note: a UserName is only picked up when it connects or receives a cancel requests
	 *       This means that the UserName can be unknown if the file doesn't login/cancel requests
	 *  
	 * Key:   connId
	 * Value: userName
	 */
	private Map<String, String> _connId2User = new HashMap<String, String>();           // Key: connId, Value: userName 
	
	/**
	 * Holds a mapping from a connectionId to gathered details about a specific connection 
	 * The ConnLevelInfo object, holds various information about a specific connId...
	 *   * like a Map of the various Operations performed by this connection id
	 *   * query plans if this is enabled.
	 *   
	 * Key:   connId
	 * Value: class ConnLevelInfo
	 */
	private Map<String, ConnLevelInfo> _connIdMap = new LinkedHashMap<String, ConnLevelInfo>();
	
	/**
	 * Holds Update/Insert Pass 1 and Pass 2 information
	 * Temporary used to store last row(s), and removed as soon as the Operation is added to the ConnLevelInfo object
	 * (this so we can have multiple connections output lines out-of-sequence in the file, meaning different connections print simultaneously to the log)
	 * 
	 * When adding the Statistics for a Update/Insert Operation, the entry is removed.  
	 *  
	 * Key:   connId
	 * Value: class Pass12
	 */
	private Map<String, Pass12> _connPass12Map = new HashMap<String, Pass12>();

	/**
	 * Holds a summary counter for Cancel request made by a specific UserName (note cancel request is also incremented at the ConnLevelInfo for each ConnID)
	 * 
	 * Key:   userName, 
	 * Value: counter that holds number of cancel request
	 */
	private Map<String, Integer> _userCancelReq = new LinkedHashMap<String, Integer>();

	/** What date format does the log have */
	private static SimpleDateFormat _dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss");

	private CmdLineOptions _cmdLineOpt = null;

	private String          _currentFile  = null;
	private Date            _startTime    = null;
	private Date            _endTime      = null;

	private String getUserByConnectionId(String connId)
	{
		String user = _connId2User.get(connId);
		if (user == null)
			return "--" + connId + "--";
		return user;
	}

	public IqLogParser(CmdLineOptions cmdLineOptions)
	{
		_cmdLineOpt = cmdLineOptions;
	}

	public void parse(String file)
	{
		_currentFile = file;

		try
		{
			System.out.println("Opening file: "+file);
			FileReader fileReader = new FileReader(new File(file));
			BufferedReader br = new BufferedReader(fileReader);

			// if no more lines the readLine() returns null
			String line = null;
			int lineNum = 0;
			while ((line = br.readLine()) != null) 
			{
				String[] row = line.split("\\s+");
				lineNum++;

				if ((lineNum % 500000) == 0)
					System.out.format("Processed %d K lines.\n", lineNum/1000);


				if (row.length >= 5)
				{
					String connId = row[3];

					//------------------------------------
					// try to grab Update Pass 1 and Pass 2
					//------------------------------------
					// I. 11/27 02:54:24. 0000000574 Update Started:
					// I. 11/27 02:54:24. 0000000574 income
					// I. 11/27 02:54:24. 0000000574 [20895]: Update Pass 1 completed in 0 seconds.
					// I. 11/27 02:54:24. 0000000574 [20895]: Update Pass 2 completed in 0 seconds.
					// I. 11/27 02:54:24. 0000000574 [20896]: Update for 'income' completed in 0 seconds.  62 rows updated.
					
	                // I. 12/08 16:40:05. 0000035309 Insert Started.
	                // I. 12/08 16:40:05. 0000035309 #rs_uledger_accounts_1237_4
	                // I. 12/08 16:40:05. 0000000085 Txn 1041065594 0 1041065594
	                // I. 12/08 16:40:05. 0000035309 [20895]: Insert Pass 1 completed in 0 seconds.
	                // I. 12/08 16:40:05. 0000035309 [20895]: Insert Pass 2 completed in 0 seconds.
					// I. 12/08 16:40:05. 0000035309 [20896]: Insert for '#rs_uledger_accounts_1237_4' completed in 0 seconds.  52 rows inserted.

					if ( row.length == 6 && "Update".equals(row[4]) && "Started:".equals(row[5]) )
						_connPass12Map.put(connId, new Pass12(OpType.UPDATE) );

					if ( row.length == 6 && "Insert".equals(row[4]) && "Started.".equals(row[5]) ) // NOTE the '.' instead of ':'
						_connPass12Map.put(connId, new Pass12(OpType.INSERT) );

					if ( row.length == 5 && _connPass12Map.containsKey(connId) )
					{
						Pass12 p12 = _connPass12Map.get(connId);
						if (p12 != null)
							p12._tabName = row[4];
					}

					if ( row.length == 12 && _connPass12Map.containsKey(connId) && "Pass".equals(row[6]) && "completed".equals(row[8]) && "in".equals(row[9]) && "seconds.".equals(row[11]))
					{
						Pass12 p12 = _connPass12Map.get(connId);
						if (p12 != null)
						{
							// Just "insanity check"
							if (    "Update".equals(row[5]) && p12._opType == OpType.UPDATE 
							     || "Insert".equals(row[5]) && p12._opType == OpType.INSERT
							   )
							{
								if ("1".equals(row[7])) p12._pass1 = parseInt(row[10], 0);
								if ("2".equals(row[7])) p12._pass2 = parseInt(row[10], 0);
							}
						}
					}
					
					//------------------------------------
					// Grab Query plan information 
					// Start of a query plan has 7 tokens - the last 5th being [20535]:
					// End of the query plan comes when the 5th token is no longer [20535]:
					//------------------------------------
					//	I. 12/11 08:13:28. 0001334736 [20535]: Query Plan:
					//	I. 12/11 08:13:28. 0001334736 [20535]: 0    #20:  Root
					//	I. 12/11 08:13:28. 0001334736 [20535]:         Child Node 1:  #19
					//	I. 12/11 08:13:28. 0001334736 [20535]:         Estimated Result Rows:  8
					//	I. 12/11 08:13:28. 0001334736 [20535]:         User Name:  #szhong   (SA connHandle: 258722  SA connID: 185)
					//	I. 12/11 08:13:28. 0001334736 [20535]:         Est. Temp Space Used (Mb):    15.8
					//	I. 12/11 08:13:28. 0001334736 [20535]:         Requested attributes:  No Scroll Chained 
					//	I. 12/11 08:13:28. 0001334736 [20535]:         Effective Number of Users:  6
					//	I. 12/11 08:13:28. 0001334736 [20535]:         Number of CPUs:  16
					//	I. 12/11 08:13:28. 0001334736 [20535]:         IQ Main Cache Size (Mb):  65000
					//	I. 12/11 08:13:28. 0001334736 [20535]:         IQ Temp Cache Size (Mb):  70000
					//	I. 12/11 08:13:28. 0001334736 [20535]:         Threads used for executing local invariant predicates:  3
					//	I. 12/11 08:13:28. 0001334736 [20535]:         Number of CPUs (actual):  88
					//  .....
					//	I. 12/11 08:13:28. 0001334736 [20535]:                   Column 2     Note:  LF index used by optimizer
					//	I. 12/11 08:13:28. 0001334736 [20535]:                   Column 2     Indexes:  FP(2), LF, HG(U)
					//	I. 12/11 08:13:28. 0001334736 [20535]:                   Maximum Row ID:  504
					//	I. 12/11 08:13:28. 0001334736 [20819]: 

					if (_cmdLineOpt._qplans)
					{
						if ("[20535]:".equals(row[4]))
						{
							if ( "Query".equals(row[5]) && "Plan:".equals(row[6]))
							{
								String date    = row[1];
								String time    = row[2];

								QueryPlan qPlan = new QueryPlan(date, time);

								int startPos = line.indexOf("[20535]:") + "[20535]: ".length();
								qPlan.addLine( line.substring(startPos) );

								addQueryPlan(connId, qPlan);
							}
							else
							{
								QueryPlan qPlan = getQueryPlan(connId);
								
								// NOTE: Just add it to the "last" query plan
								//       But if we havn't received any row with "Query Plan:" then the getQueryPlan() will return with NULL 
								if (qPlan != null)
								{
									int startPos = line.indexOf("[20535]:") + "[20535]: ".length();
									qPlan.addLine( line.substring(startPos) );
								}
							}
						}
					} // end: _cmdLineOpt._qplans
				} // end: row.length >= 5

				if (row.length >= 14)
				{
					//------------------------------------
					// CONNECT Requests
					//------------------------------------
					// I. 12/08 16:40:00. 0000201823 Connect:  SA connHandle: 1000197847  SA connID: 30  IQ connID: 0000201823  User: DBA
					if ("Connect:".equals(row[4]))
					{
						String connId = row[13];
						String user   = row[15];

						if ( _connId2User.containsKey(connId) )
						{
							String prevUser = _connId2User.get(connId);
							if ( ! user.equals(prevUser) )
								System.out.format("WARNING: Resusing ConnectionID=%s, for user=%-20s  this previously used by=%s\n", connId, user, prevUser);
						}
						_connId2User.put(connId, user);
						
//						System.out.format("Connect: id=%s, user=%s\n", connId, user);
					}

					//------------------------------------
					// DISCONNECT Requests
					//------------------------------------
					// I. 12/08 16:39:31. 0000201793 Disconnect:  SA connHandle: 1000197817  SA connID: 40  IQ connID: 0000201793  User: DBA
					if ("Disconnect:".equals(row[4]))
					{
						String connId = row[13];
						String user   = row[15];
						
						// Do some cleanup
						_connId2User.remove(connId);
					}

					//------------------------------------
					// CANCEL Requests
					//------------------------------------
					//I. 12/03 15:16:54. 0000000000 Cancellation request received:  SA connHandle: 99  SA connID: 16  IQ connID: 0000001635  User: iq_micos_n_maint
					if ("Cancellation".equals(row[4]) && "request".equals(row[5]) && "received:".equals(row[6]))
					{
						String connId = row[15];
						String user   = row[17];

						// Update at connection level
						ConnLevelInfo connLvl = _connIdMap.get(connId);
						if (connLvl != null)
						{
							connLvl.incCancelRequests();
							connLvl.setUser(user);
						}
						

						// Update at a "global" level
						Integer cancelReq = _userCancelReq.get(user);
						if (cancelReq == null)
							_userCancelReq.put(user, 1);
						else
							_userCancelReq.put(user, cancelReq+1);


						// Check if the ID->User has a value, otherwise we might use the CANCEL request to set it
						if ( ! _connId2User.containsKey(connId) )
						{
							//String prevUser = _connId2User.get(connId);
							_connId2User.put(connId, user);
						}


//System.out.println("--------- CANCEL on connId='"+connId+"', connLvl="+connLvl);
					}

					//------------------------------------
					// ROWS
					//------------------------------------
					// I. 11/27 18:00:19. 0000000177 [20896]: Update for 'maga_status' completed in 89 seconds. 11 rows updated.
					if (   row[0]  .equals("I.") 
					    && row[6]  .equals("for") 
					    && row[8]  .equals("completed") 
					    && row[9]  .equals("in") 
					    && row[11] .equals("seconds.") 
					    && row[13] .equals("rows") 
					   )
					{
						// I. 11/27 18:00:19. 0000000177 [20896]: Update for 'maga_status' completed in 89 seconds. 11 rows updated.
						// -- ----- --------- ---------- -------- ------ --- ------------- --------- -- -- -------- -- ---- --------
						// 0  1     2         3          4        5      6   7             8         9  10 11       12 13   14
						String logLvl  = row[0];
						String date    = row[1];
						String time    = row[2];
						String connId  = row[3];
						String op      = row[5];
						String tab     = row[7];
						int    seconds = parseInt(row[10], 0);
						int    rows    = parseInt(row[12], 0);

						Pass12 p12 = _connPass12Map.get(connId);

						putStats(connId, tab, op, seconds, rows, p12, date, time, lineNum);

						_connPass12Map.remove(connId);

						double rowPerSec = rows;
						if (seconds > 0)
							rowPerSec = rows / seconds;
						
//						System.out.printf("%s %s %-20s %s for %-30s completed in %3d seconds. %9d rows %10s        rows per sec %f\n", 
//							logLvl, time, getUserByConnectionId(connId), op, tab, seconds, rows, row[14], rowPerSec );
					}
				}
			}
			
			br.close();
			fileReader.close();

			System.out.println("Done Parsing file: "+file);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	
	private QueryPlan getQueryPlan(String connId)
	{
		ConnLevelInfo connLvl = _connIdMap.get(connId);
		if (connLvl != null)
		{
			return connLvl.getLastQueryPlan();
		}

		return null;
	}

	private void addQueryPlan(String connId, QueryPlan qplan)
	{
		if (qplan == null)
			throw new IllegalArgumentException("calling addQueryPlan: QueryPlan is NULL");

		String user = getUserByConnectionId(connId);

		ConnLevelInfo connLvl = _connIdMap.get(connId);
		if (connLvl == null)
		{
			connLvl = new ConnLevelInfo(connId, user, qplan.getDateTime());
			_connIdMap.put(connId, connLvl);
		}

		if (_startTime == null)
 			_startTime = qplan.getDateTime();
 		_endTime = qplan.getDateTime();
		
		connLvl.addQueryPlan(qplan);
	}

	private void putStats(String connId, String tab, String opStr, int seconds, int rows, Pass12 pass12, String date, String time, int lineNum)
	{
		String user = getUserByConnectionId(connId);
		
		tab = tab.replace("'", "");
	
		Date ts = null;
 		try { ts = _dateFormat.parse(date + " " + time); }
		catch (ParseException ignore) {}
 		
 		if (_startTime == null)
 			_startTime = ts;
 		_endTime = ts;
		
		ConnLevelInfo connLvl = _connIdMap.get(connId);
		if (connLvl == null)
		{
			connLvl = new ConnLevelInfo(connId, user, ts);
			_connIdMap.put(connId, connLvl);
		}

		connLvl.addOperation(tab, opStr, seconds, rows, pass12, ts, lineNum);
	}

	public void printStat(PrintStream ps)
	{
		ps.append("=======================================================================================\n");
		ps.append("IQ Log Report:\n");
		ps.append("File:     "+_currentFile).append("\n");
		ps.append("Start:    "+_dateFormat.format(_startTime)).append("\n");
		ps.append("End:      "+_dateFormat.format(_endTime)).append("\n");
		ps.append("Duration: "+msToTimeStr("%HH:%MM", _endTime.getTime() - _startTime.getTime()) + "    (HH:MM)").append("\n");
		ps.append("=======================================================================================\n");
		ps.append("\n");
		
		for (String key : _connIdMap.keySet())
		{
			ConnLevelInfo connLvl = _connIdMap.get(key);

			// If the statistics map is empty, no need to print information.
			if (connLvl._statsMap.isEmpty())
				continue;

			// Get Operations in to a list and sort them
			List<Operation> opList = new ArrayList<Operation>();
			for (Operation op : connLvl._statsMap.values())
				opList.add(op);
			
			Collections.sort(opList);


			StringBuilder section = new StringBuilder();
			section.append("\n");
			section.append("#######################################################################################\n");
			section.append("Start='"+_dateFormat.format(connLvl._startTime)+"', end='"+_dateFormat.format(connLvl._endTime)+"', length(HH:MM)='"+msToTimeStr("%HH:%MM", connLvl._endTime.getTime() - connLvl._startTime.getTime())+"'.\n");
			section.append("User='").append(connLvl.getUser()).append("', ConnID='").append(connLvl._connId).append("'.\n");

			if (_cmdLineOpt._topRows > 0 && _cmdLineOpt._topRows != Integer.MAX_VALUE)
				section.append("Only top=").append(_cmdLineOpt._topRows).append(" will be presented.\n");

			if (connLvl._cancelReqCnt > 0)
				section.append("Cancel Requests="+connLvl._cancelReqCnt+"\n");

			Operation rsLastCommit = null;
			for (Operation op : opList)
			{
				if ("rs_lastcommit".equals(op._table))
				{
					if (rsLastCommit != null)
					{
						section.append("WARNING: we seems to have more than 1 entry for 'rs_lastcommit' for section '"+key+"'.\n");
						section.append("     rsLastCommit: "+rsLastCommit+"\n");
						section.append("               op: "+op+"\n");
					}
					rsLastCommit = op;
				}
			}
			if (rsLastCommit == null)
			{
				section.append("NOTE: no 'rs_lastcommit' entry, so this is most likly NOT a replicated connection\n");
				if (_cmdLineOpt._onlyRs)
					continue; // do next entry from: for (String key : _connMap.keySet())
			}
			else
			{
				section.append("Replication Server has worked on this...\n");
				section.append(rsLastCommit).append("\n");
			}
			section.append("#######################################################################################\n");
			section.append(Operation.getHeader());
						

			Operation sumOp = new Operation(connLvl, "SummaryForAllTables", "summary");

			int top   = _cmdLineOpt._topRows;
			if (top <= 0)
				top = Integer.MAX_VALUE;

			int count = 0;
			for (Operation op : opList)
			{
				count++;
				if (count <= top)
					section.append(op.toString()).append("\n");

				sumOp.summary(op);
			}
			section.append(Operation.getSumLine());
			section.append(sumOp.toString()).append("\n");

			// Finally add this section to the report
			ps.append(section);
		}

		ps.append("\n");
		ps.append("=======================================================\n");
		ps.append("-- Cancel requests statistics:\n");
		ps.append("-------------------------------------------------------\n");
		for (String key : _userCancelReq.keySet())
		{
			ps.append(String.format("%6d Cancel request for user %-20s was received.\n", _userCancelReq.get(key), key));
		}

		if (_cmdLineOpt._qplans)
		{
			ps.append("\n");
			ps.append("=======================================================\n");
			ps.append("-- Query Plan SUMMARY:\n");
			ps.append("-------------------------------------------------------\n");
			ps.append(" User                           ConnID     QueryPlanCount QueryPlansRowSum\n");
			ps.append(" ------------------------------ ---------- -------------- ----------------\n");
			for (String key : _connIdMap.keySet())
			{
				ConnLevelInfo connLvl = _connIdMap.get(key);

				if (connLvl.getQueryPlanCount() == 0)
					continue;

				int qpRowSum = 0;
				for (QueryPlan qplan : connLvl.getQueryPlanList())
					qpRowSum += qplan.getQueryPlanRows();

				String row = String.format(" %-30s %10s %14d %16d\n", connLvl.getUser(), connLvl._connId, connLvl.getQueryPlanCount(), qpRowSum);
				ps.append(row);
			}

			ps.append("\n");
			ps.append("=======================================================\n");
			ps.append("-- Query Plan Output:\n");
			ps.append("-------------------------------------------------------\n");
			for (String key : _connIdMap.keySet())
			{
				ConnLevelInfo connLvl = _connIdMap.get(key);

				if (connLvl.getQueryPlanCount() == 0)
					continue;

				ps.append("\n");
				ps.append("#######################################################################################\n");
				ps.append("User='").append(connLvl.getUser()).append("', ConnID='").append(connLvl._connId).append("', QueryPlanCount=").append(connLvl.getQueryPlanCount()+"").append(".\n");
				ps.append("#######################################################################################\n");
				for (QueryPlan qplan : connLvl.getQueryPlanList())
				{
					ps.append("\n");
					ps.append(qplan.toString("    ")).append("\n");
				}
			}
		}

		ps.append("\n");
		ps.append("=======================================================================================\n");
		ps.append("--END-OF-REPORT--:\n");
		ps.append("=======================================================================================\n");
		ps.append("File:     "+_currentFile).append("\n");
		ps.append("Start:    "+_dateFormat.format(_startTime)).append("\n");
		ps.append("End:      "+_dateFormat.format(_endTime)).append("\n");
		ps.append("Duration: "+msToTimeStr("%HH:%MM", _endTime.getTime() - _startTime.getTime()) + "    (HH:MM)").append("\n");
		ps.append("=======================================================================================\n");
		ps.append("\n");
	}

	/**
	 * Simply do Integer.parseInt(str), but if it fails (NumberFormatException), then return the default value
	 * @param str           String to be converted
	 * @param defaultValue  if "str" can't be converted (NumberFormatException), then return this value
	 * @return a integer value
	 */
	public static int parseInt(String str, int defaultValue)
	{
		try
		{
			return Integer.parseInt(str);
		}
		catch (NumberFormatException nfe)
		{
			return defaultValue;
		}
	}

	/**
	 * Convert a long into a time string 
	 * 
	 * @param format %HH:%MM:%SS.%ms
	 * @param execTime
	 * @return a string of the format description
	 */
	public static String msToTimeStr(String format, long execTime)
	{
		String execTimeHH = "00";
		String execTimeMM = "00";
		String execTimeSS = "00";
		String execTimeMs = "000";

		// MS
		execTimeMs = "000" + execTime % 1000;
		execTimeMs = execTimeMs.substring(execTimeMs.length()-3);
		execTime = execTime / 1000;

		// Seconds
		execTimeSS = "00" + execTime % 60;
		execTimeSS = execTimeSS.substring(execTimeSS.length()-2);
		execTime = execTime / 60;

		// Minutes
		execTimeMM = "00" + execTime % 60;
		execTimeMM = execTimeMM.substring(execTimeMM.length()-2);
		execTime = execTime / 60;

		// Hour
		execTimeHH = "00" + execTime;
		execTimeHH = (execTime < 100) ? execTimeHH.substring(execTimeHH.length()-2) : "" + execTime;
		

		format = format.replaceAll("%HH", execTimeHH);
		format = format.replaceAll("%MM", execTimeMM);
		format = format.replaceAll("%SS", execTimeSS);
		format = format.replaceAll("%ms", execTimeMs);

		return format;
	}

	private static class Pass12
	{
		public Pass12(OpType opType)
		{
			_opType = opType;
		}
		public OpType _opType  = null;
		public String _tabName = null;
		public int    _pass1   = 0;
		public int    _pass2   = 0;
	}

	public enum OpType
	{
		INSERT, UPDATE, DELETE, SUMMARY
	};

	private static class ConnLevelInfo
	{
		private String _user                  = null;//"";
		private String _connId                = "";

		private Map<String, Operation> _statsMap = new LinkedHashMap<String, Operation>();
//		private List<Operation> _opList          = new ArrayList<Operation>();
		private List<QueryPlan> _queryPlanList   = new ArrayList<QueryPlan>();
		private Date            _startTime       = null;
		private Date            _endTime         = null;
		
//		private Operation       _rsLastCommit = null;
//		private Operation       _summary      = null;
		
		private int             _cancelReqCnt = 0;

		public ConnLevelInfo(String connId, String user, Date ts)
		{
			_user      = user;
			_connId    = connId;
			_startTime = ts;
		}

		public QueryPlan getLastQueryPlan()
		{
			if (_queryPlanList.isEmpty())
				return null;
			
			return _queryPlanList.get( _queryPlanList.size() - 1 );
		}

		public void addQueryPlan(QueryPlan qplan)
		{
			_queryPlanList.add(qplan);
			_endTime = qplan.getDateTime();
		}

		public List<QueryPlan> getQueryPlanList()
		{
			return _queryPlanList;
		}

		public int getQueryPlanCount()
		{
			return _queryPlanList.size();
		}

		public String getUser()
		{
			return _user;
		}

		public void setUser(String user)
		{
			if ( _user != null && !_user.equals(user) && !_user.startsWith("--"))
				System.out.println("ConnLevelInfo: _connId='', is already assigned a user with name '"+_user+"', this will be overwritten with the new name '"+user+"'.");
			_user = user;
		}

		public void incCancelRequests()
		{
			_cancelReqCnt++;
		}

		public void addOperation(String tab, String opStr, int seconds, int rows, Pass12 pass12, Date ts, int lineNum)
		{
			if (ts != null)
				_endTime = ts;

			if (pass12 != null)
			{
				if ( ! tab.equals(pass12._tabName))
					pass12 = null;
			}

			String key = tab + ":" + opStr;
			Operation op = _statsMap.get(key);
			if (op == null)
			{
//				op = new Operation(this, _user, tab, opStr);
				op = new Operation(this, tab, opStr);
				_statsMap.put(key, op);
			}

			op.update(seconds, rows, pass12, lineNum);
		}
	}

	private static class QueryPlan
	{
		private Date _datetime = null;
		private String _sqlCmd = null;
		private ArrayList<String> _planTextList = new ArrayList<String>();
		
		
		public QueryPlan(String date, String time)
		{
	 		try { _datetime = _dateFormat.parse(date + " " + time); }
			catch (ParseException ignore) {}
		}

		public Date getDateTime()
		{
			return _datetime;
		}

		public String getSqlCmd()
		{
			return _sqlCmd;
		}
		
		public ArrayList<String> getQueryPlanList()
		{
			return _planTextList;
		}
		
		public int getQueryPlanRows()
		{
			return _planTextList.size();
		}
		
		@Override
		public String toString()
		{
			return toString("");
		}
		
		public String toString(String prefix)
		{
			StringBuilder sb = new StringBuilder();

			sb.append(prefix).append("==== Begin Query Plan ============================================================\n");
			sb.append(prefix).append("Time: ").append(_datetime).append("\n");
			sb.append(prefix).append("SQL: ").append(_sqlCmd != null ? _sqlCmd : "Not available").append("\n");
			sb.append(prefix).append("----------------------------------------------------------------------------------\n");
			for (String str : _planTextList)
				sb.append(prefix).append(str).append("\n");
			sb.append(prefix).append("---- End Query Plan --------------------------------------------------------------\n");
			
			return sb.toString();
		}
		
		public void addLine(String s)
		{
			_planTextList.add(s);
		}
	}
	private static class Operation
	implements Comparable<Operation>
	{
		private ConnLevelInfo     _connLvl     = null;
//		private String        _user        = "";
		private String        _table       = "";
		private OpType        _opType      = null;
		private int           _count       = 0;
		private int           _seconds     = 0;
		private int           _maxSec      = 0;
		private int           _pass1Sec    = 0;
		private int           _pass2Sec    = 0;
		private int           _rows        = 0;
		private int           _singleRow   = 0;
		private List<Integer> _atLine      = new ArrayList<Integer>();

		
		private OpType parseOpType(String opStr)
		{
			if (opStr.equalsIgnoreCase("Insert"))  return OpType.INSERT;
			if (opStr.equalsIgnoreCase("Update"))  return OpType.UPDATE;
			if (opStr.equalsIgnoreCase("Delete"))  return OpType.DELETE;
			if (opStr.equalsIgnoreCase("summary")) return OpType.SUMMARY;

			throw new RuntimeException("Uknown operation '"+opStr+"'.");
		}

		public void summary(Operation op)
		{
			_count       += op._count;
			_seconds     += op._seconds;
			_maxSec      =  Math.max(_maxSec, op._maxSec);
			_pass1Sec    += op._pass1Sec;
			_pass2Sec    += op._pass2Sec;
			_rows        += op._rows;
			_singleRow   += op._singleRow;
		}
		public Operation(ConnLevelInfo connLevel, String tab, String opStr)
		{
			_connLvl = connLevel;
//			_user    = user;
			_table   = tab;
			_opType  = parseOpType(opStr);
		}
		public void update(int seconds, int rows, Pass12 pass12, int lineNum)
		{
			_count++;
			_seconds += seconds;
			_rows    += rows;

			if (seconds > _maxSec )
				_maxSec = seconds;

			if (rows == 1)
				_singleRow++;
			
			if (pass12 != null)
			{
				_pass1Sec += pass12._pass1;
				_pass2Sec += pass12._pass2;
			}

			if (lineNum > 0)
				_atLine.add(lineNum);
		}

		public static String getHeader()
		{
			return "User/connid          OpCount Oper    Table                          Seconds  MaxSec   Pass1Sec Pass2Sec AvgTimePerOp SumRows    SingleRows AvgRowsPerOp avgRowsPerSec Note\n" +
			       "-------------------- ------- ------- ------------------------------ -------- -------- -------- -------- ------------ ---------- ---------- ------------ ------------- -------------------------\n";
		}
		public static String getSumLine()
		{
			return "==================== ======= ======= ============================== ======== ======== ======== ======== ============ ========== ========== ============ ============= =========================\n";
		}
		@Override
		public String toString()
		{
			double avgRowsPerSec = _rows;
			if (_seconds > 0)
				avgRowsPerSec = (double)_rows / (double)_seconds;
			
			double avgRowsPerOp = 0;
			if (_count > 0)
				avgRowsPerOp = (double)_rows / (double)_count;
			
			double avgTimePerOp = 0;
			if (_count > 0)
				avgTimePerOp = (double)_seconds / (double)_count;

			String note = "";
			if (_count == _singleRow)
				note += "Only SINGLE ROW";
			
			String user = "";
			if (_connLvl != null)
				user = _connLvl.getUser(); 

			return String.format(
				"%-20s %7d %-7s %-30s %8d %8d %8d %8d %12.1f %10d %10d %12.1f %13.1f %s",
				//user cnt op   tab   sec max up1 up2 tPerOp rows sro rPerOp rPerS  note
				user, _count, _opType, _table, _seconds, _maxSec, _pass1Sec, _pass2Sec, avgTimePerOp, _rows, _singleRow, avgRowsPerOp, avgRowsPerSec, note);
		}

		@Override
		public int compareTo(Operation o)
		{
			if (o._seconds > this._seconds) return 1;
			if (o._seconds < this._seconds) return -1;
			return 0;
		}
	}

	private static class CmdLineOptions
	{
		public String  _outfile = null;
		public int     _topRows = Integer.MAX_VALUE; 
		public boolean _onlyRs  = false; 
		public boolean _qplans  = false;
	}

	public static void main(String[] args)
	{
		CmdLineOptions cmdLineOptions = new CmdLineOptions();;
		List<String> fileList = new ArrayList<String>();
		
		if (args.length == 0)
		{
			System.out.println("");
			System.out.println("Usage: [-o outfile] [-t top#] [-r] [-q] iq_log_file [iq_log_file...]");
			System.out.println("       -o file  write to this output file, default stdout");
			System.out.println("       -t #     Only show top rows");
			System.out.println("       -r       Only collections with rs_lastcommit in them.");
			System.out.println("       -q       Capture embedded query plans.");
			System.out.println("");
		}
		
		for (int i=0; i<args.length; i++)
		{
			if (args[i].startsWith("-o")) // -o filename | -tfilename
			{
				if (args[i].equals("-o"))
				{
					i++;
					cmdLineOptions._outfile = args[i];
				}
				else
				{
					cmdLineOptions._outfile = args[i].substring(2);
				}
			}
			else if (args[i].startsWith("-t")) // -t 10 | -t10
			{
				if (args[i].equals("-t"))
				{
					i++;
					cmdLineOptions._topRows = parseInt(args[i], Integer.MAX_VALUE);
				}
				else
				{
					cmdLineOptions._topRows = parseInt(args[i].substring(2), Integer.MAX_VALUE);
				}
			}
			else if (args[i].equals("-r"))
			{
				cmdLineOptions._onlyRs = true;
			}
			else if (args[i].equals("-q"))
			{
				cmdLineOptions._qplans = true;
			}
			else
			{
				fileList.add(args[i]);
			}
				
		}

		PrintStream out = System.out;
		if (cmdLineOptions._outfile != null)
		{
			try
			{
				out = new PrintStream(cmdLineOptions._outfile);
			}
			catch (FileNotFoundException e)
			{
				System.err.println("Problems opening the output file '"+cmdLineOptions._outfile+"', Caught: "+e);
				e.printStackTrace();
				return;
			}
		}

		for (String file : fileList)
		{
			IqLogParser p = new IqLogParser(cmdLineOptions);
			p.parse(file);
			System.out.println("Report output is written to: " + (cmdLineOptions._outfile == null ? "stdout" : cmdLineOptions._outfile) );
			p.printStat(out);
		}
	}
}

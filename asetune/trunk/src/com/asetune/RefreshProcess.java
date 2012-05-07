/**
*/


package com.asetune;
import java.awt.Color;
import java.awt.Container;
import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;

import org.apache.log4j.Logger;

import com.asetune.cm.CmSybMessageHandler;
import com.asetune.cm.CountersModel;
import com.asetune.gui.AseConfigMonitoringDialog;
import com.asetune.gui.swing.GTabbedPane;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.TimeUtils;


public class RefreshProcess extends Thread 
{
	private static Logger _logger          = Logger.getLogger(RefreshProcess.class);

	private ProcessDetailFrame pdf;
	private Connection	       _conn;
	private boolean	           refreshProcessFlag;
	private boolean            _paused               = false;
	private int                _refreshInterval      = 1;
	boolean	                   batchCapture	         = false;
	
	boolean	                   stmtPipe	             = false;
	boolean	                   stmtPipeMsg	         = false;
	boolean	                   stmtStat	             = false;
	
	boolean	                   sqlTextSample         = true;
	boolean	                   sqlTextPipe	         = false;
	boolean	                   sqlTextPipeMsg	     = false;
	boolean	                   SQLTextMonitor	     = false;
	
	boolean	                   planTextSample        = true;
	boolean	                   planTextPipe	         = false;
	boolean	                   planTextPipeMsg	     = false;

	boolean	                   procWaitEvents	     = false;
	boolean	                   has_sa_role	         = false;

	private boolean            _firstTimeSample      = true;
	private boolean            _discardPreOpenStmnts = true;

	private Statement	       stmt;
	private int	               kpid;
	private int	               spid;
	private ResultSet	       rs;
	private int	               currentSpid	         = -1;
	private int	               currentKpid	         = -1;
	private int	               currentBatchID	     = -1;
	private int	               currentDBID	         = -1;
	private int	               currentProcedureID	 = -1;
	private int	               currentPlanID	     = -1;
	private int	               currentSqlLine        = -1;
	private String             currentDbname         = "";
	private String             currentProcName       = "";

	// rs.getXXX(pos_xxx), use the below variables when positioning in the resultset
	private int pos_currentSpid        = -1;
	private int pos_currentKpid        = -1;
	private int pos_currentBatchID     = -1;
	private int pos_currentSqlLine     = -1;
	private int pos_currentDbname      = -1;
	private int pos_currentProcName    = -1;
	private int pos_currentPlanID      = -1;
	private int pos_currentDBID        = -1;
	private int pos_currentProcedureID = -1;
	private int pos_currentWaitEventID = -1;
	private int pos_currentWaitEventDesc = -1;

	// rs.getXXX(pos_xxx), use the below variables when positioning in the resultset
	private int pos_capturedSpid        = -1;
	private int pos_capturedKpid        = -1;
	private int pos_capturedBatchID     = -1;
	private int pos_capturedSqlLine     = -1;
	private int pos_capturedDbname      = -1;
	private int pos_capturedProcName    = -1;
	private int pos_capturedPlanID      = -1;
	private int pos_capturedDBID        = -1;
	private int pos_capturedProcedureID = -1;
	
	private Vector	           currentStmtColNames	 = null;
	private StatementsModel	   currentStmtModel;
	private boolean	           currentSMInitialized	 = false;
	private Vector	           currentStmt	         = null;
//	private Vector	           currentStmtRow	     = null;
	private Vector	           currentStmtRows	     = null; // multiple currentStmtRow objects
	private Batch	           currentBatch          = null;

	private Vector	           capturedStmtColNames	 = null;
	private StatementsModel	   capturedStmtModel;
	private boolean	           capturedSMInitialized = false;
	private boolean            capturedTableColmnsIsResized = false;


	private Vector	           newCapturedStatements = null;

	private int	               selectedStatement;

	private Hashtable	       batchHistory          = new Hashtable();
	private Hashtable	       plansHistory          = new Hashtable();
	private Hashtable	       compiledPlansHistory  = new Hashtable();
	private Hashtable	       procedureTextCache    = new Hashtable();

	public CountersModel	   CMProcObjects;
	public CountersModel	   CMProcWaits;
	public CountersModel	   CMLocks;

	private int     _aseVersion       = 0;
	private boolean _isClusterEnabled = false;
	private int     _monTablesVersion = 0;
	 

	private String  _captureRestrictionSql = "";
	private boolean _captureRestrictions   = false;

	private String _activeStatementsSql = "";
	private String _historyStatementsSql = "";

	public String getActiveStatementsSql()  { return _activeStatementsSql; }
	public String getHistoryStatementsSql() { return _historyStatementsSql; }
	
	public void setCaptureRestriction(String str)
	{
		_captureRestrictionSql = str;
		
		if (_captureRestrictionSql != null && _captureRestrictionSql.length() > 0)
			_captureRestrictions = true;
		else
			_captureRestrictions = false;

//		RuntimeException rte =  new RuntimeException("DEBUG exception to get from where this method was called from.");
//		_logger.debug("Setting CaptureRestrictingSql to '"+str+"'.", rte);
		_logger.debug("Setting CaptureRestrictingSql to '"+str+"'.");
	}
	

	public void setSqlTextSample(boolean b)
	{
		sqlTextSample = b;
	}
	public void setPlanTextSample(boolean b)
	{
		planTextSample = b;
	}
	public void setDiscardPreOpenStmnts(boolean b)
	{
		_discardPreOpenStmnts = b;
	}
	

	public RefreshProcess(ProcessDetailFrame aPdf, Connection conn, int kpid) 
	{
		this.pdf = aPdf;
		this._conn = conn;
		this.kpid = kpid;
		refreshProcessFlag=true;
		selectedStatement=-1;
		try 
		{
			stmt = conn.createStatement();
			currentStmt = new Vector();
			
			currentStmtModel = new StatementsModel();
			pdf.currentStatementTable.setModel(currentStmtModel);
			
			capturedStmtModel = new StatementsModel();
			pdf.capturedStatementsTable.setModel(capturedStmtModel);
		}
		catch (Exception e) {}
	}

	private void addBatchHistory(Batch batch)
	{
		String id = batch.getKey();
		if (_logger.isDebugEnabled())
			_logger.debug("Adding SQL text for for id='"+id+"'. text="+batch.getSqlText(false));
		batchHistory.put(id, batch);
	}

	private Batch getBatchHistory(String id)
	{
		Batch batch = (Batch) batchHistory.get(id);
		
		if (batch == null)
			_logger.debug("No SQL text was found for batch  id='"+id+"'.");
		
		return batch;
	}
	private Batch getBatchHistory(int spid, int kpid, int batchId)
	{
		String id = spid + ":" + kpid + ":" + batchId;
		Batch batch = (Batch) batchHistory.get(id);
		
		if (batch == null)
			_logger.debug("No SQL text was found for batch  id='"+id+"', spid='"+spid+"', kpid='"+kpid+"', batchId='"+batchId+"'.");
		
		return batch;
	}


	/** TODO: keep plans for procedures in a specific cache */ 
	private void addPlanHistory(int spid, int kpid, int batchId, int dbId, int procId, Batch batch, StringBuffer planText)
	{
		if (planText == null)
			throw new RuntimeException("Passed StringBuffer 'planText' cant be null.");

		// Get rid of last newline
		int len = planText.length() - 1;
		if ( planText.charAt(len) == '\n')
		{
			planText.setCharAt(len, ' ');
		}

		String id = spid + ":" + kpid + ":" + batchId + ":" + procId;

		Batch existingBatch = (Batch) plansHistory.get(id);
		if (existingBatch != null)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("Appending SHOWPLAN to spid='"+spid+"', kpid='"+kpid+"', batchId='"+batchId+"', procId='"+procId+"', hashtable.key='"+id+"'. text="+planText);
			existingBatch.addShowplanTextLine(planText);
		}
		else
		{
			if (_logger.isDebugEnabled())
				_logger.debug("Adding SHOWPLAN for spid='"+spid+"', kpid='"+kpid+"', batchId='"+batchId+"', procId='"+procId+"', hashtable.key='"+id+"'. text="+planText);
			batch.addShowplanTextLine(planText);
			plansHistory.put(id, batch);
		}
	}
	private void addPlanHistory(Batch batch)
	{
		String id = batch.spid + ":" + batch.kpid + ":" + batch.batchId + ":" + batch.procedureId;

		Batch existingBatch = (Batch) plansHistory.get(id);
		if (existingBatch != null)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("Appending SHOWPLAN to spid='"+batch.spid+"', kpid='"+batch.kpid+"', batchId='"+batch.batchId+"', procId='"+batch.procedureId+"', hashtable.key='"+id+"'. text="+batch.getShowplanText());
			existingBatch.appendShowplanText(batch.getShowplanText());
		}
		else
		{
			if (_logger.isDebugEnabled())
				_logger.debug("Adding SHOWPLAN for spid='"+batch.spid+"', kpid='"+batch.kpid+"', batchId='"+batch.batchId+"', procId='"+batch.procedureId+"', hashtable.key='"+id+"'. text="+batch.getShowplanText());
			//batch.addShowplanTextLine(planText);
			plansHistory.put(id, batch);
		}
		
	}

	/** TODO: keep plans for procedures in a specific cache */ 
	private Batch getPlanHistory(int spid, int kpid, int batchId, int dbId, int procId)
	{
		//String id = batchId + ":" + dbId + ":" + procId;
//		String id = batchId + ":" + procId;
		String id = spid + ":" + kpid + ":" + batchId + ":" + procId;
		Batch batch = (Batch) plansHistory.get(id);
		
		if (batch == null)
			_logger.debug("No SHOWPLAN text was found for spid='"+spid+"', kpid='"+kpid+"', batchId='"+batchId+"', procId='"+procId+"', hashtable.key='"+id+"'.");
			//_logger.debug("No SHOWPLAN text was found for batchId='"+batchId+"', dbId='"+dbId+"', procId='"+procId+"', hashtable.key='"+id+"'.");
		
		return batch;
	}

	private String nullFix(Object o)
	{
		return (o == null) ? "" : o.toString();
	}
	private void refreshStmt() 
	{
		// Used at the end to printout SQL statement, when SQLExceptions are thrown.
		String currentSql = "";
		try
		{
			if (kpid > 0)
			{
				// Refresh process information
				currentSql = 
					"select P.spid, P.enginenum, P.status, P.suid, suser_name(P.suid), P.hostname, P.hostprocess, P.cmd, P.cpu, \n" + 
					"   P.physical_io, P.memusage, LocksHeld, P.blocked, P.dbid, db_name(P.dbid), P.uid, '' /*user_name(P.uid)*/, P.gid, \n" + 
					"   P.tran_name, P.time_blocked, P.network_pktsz, P.fid, P.execlass, P.priority, P.affinity, P.id, object_name(P.id,P.dbid), \n" + 
					"   P.stmtnum, P.linenum, P.origsuid, P.block_xloid, P.clientname, P.clienthostname,  P.clientapplname, P.sys_id, \n" + 
					"   P.ses_id, P.loggedindatetime, P.ipaddr,  program_name=convert(varchar,P.program_name), CPUTime, \n" + 
					"   WaitTime, LogicalReads, PhysicalReads, PagesRead, PhysicalWrites, PagesWritten, MemUsageKB, \n" + 
					"   ScanPgs =  TableAccesses,IdxPgs = IndexAccesses, TmpTbl = TempDbObjects, UlcBytWrite =  ULCBytesWritten, \n" + 
					"   UlcFlush = ULCFlushes, ULCFlushFull, Transactions, Commits, Rollbacks, PacketsSent, PacketsReceived, \n" + 
					"   BytesSent, BytesReceived \n" + 
					"from sysprocesses P , monProcessActivity A, monProcessNetIO N \n" + 
					"where P.kpid=A.KPID \n" +
					"  and N.KPID=P.kpid \n" +
					"  and P.kpid=" + Integer.toString(kpid);
				rs = stmt.executeQuery(currentSql);
	
				rs.next();
				if (rs.getRow() > 0)
				{
					spid = ((Number) rs.getObject(1)).intValue();

					pdf.spidFld            .setText(nullFix(rs.getObject(1)));
					pdf.enginenumFld       .setText(nullFix(rs.getObject(2)));
					pdf.statusFld          .setText(nullFix(rs.getObject(3)));
					pdf.suidFld            .setText(nullFix(rs.getObject(4)));
					pdf.suser_nameFld      .setText(nullFix(rs.getObject(5)));
					pdf.hostnameFld        .setText(nullFix(rs.getObject(6)));
					pdf.hostprocessFld     .setText(nullFix(rs.getObject(7)));
					pdf.cmdFld             .setText(nullFix(rs.getObject(8)));
					pdf.cpuFld             .setText(nullFix(rs.getObject(9)));
					pdf.physical_ioFld     .setText(nullFix(rs.getObject(10)));
					pdf.memusageFld        .setText(nullFix(rs.getObject(11)));
					pdf.LocksHeldFld       .setText(nullFix(rs.getObject(12)));
					pdf.blockedFld         .setText(nullFix(rs.getObject(13)));
					pdf.dbidFld            .setText(nullFix(rs.getObject(14)));
					pdf.db_nameFld         .setText(nullFix(rs.getObject(15)));
					pdf.uidFld             .setText(nullFix(rs.getObject(16)));
					pdf.user_nameFld       .setText(nullFix(rs.getObject(17)));
					pdf.gidFld             .setText(nullFix(rs.getObject(18)));
	
					pdf.tran_nameFld       .setText(nullFix(rs.getObject(19)));
					pdf.time_blockedFld    .setText(nullFix(rs.getObject(20)));
					pdf.network_pktszFld   .setText(nullFix(rs.getObject(21)));
					pdf.fidFld             .setText(nullFix(rs.getObject(22)));
					pdf.execlassFld        .setText(nullFix(rs.getObject(23)));
					pdf.priorityFld        .setText(nullFix(rs.getObject(24)));
					pdf.affinityFld        .setText(nullFix(rs.getObject(25)));
					pdf.procIDFld          .setText(nullFix(rs.getObject(26)));
					pdf.object_nameFld     .setText(nullFix(rs.getObject(27)));
					pdf.stmtnumFld         .setText(nullFix(rs.getObject(28)));
					pdf.linenumFld         .setText(nullFix(rs.getObject(29)));
					pdf.origsuidFld        .setText(nullFix(rs.getObject(30)));
					pdf.block_xloidFld     .setText(nullFix(rs.getObject(31)));
					pdf.clientnameFld      .setText(nullFix(rs.getObject(32)));
					pdf.clienthostnameFld  .setText(nullFix(rs.getObject(33)));
					pdf.clientapplnameFld  .setText(nullFix(rs.getObject(34)));
					pdf.sys_idFld          .setText(nullFix(rs.getObject(35)));
					pdf.ses_idFld          .setText(nullFix(rs.getObject(36)));
					pdf.loggedindatetimeFld.setText(nullFix(rs.getObject(37)));
					pdf.ipaddrFld          .setText(nullFix(rs.getObject(38)));
	
					pdf.program_nameFld    .setText(nullFix(rs.getObject(39)));
					pdf.CPUTimeFld         .setText(nullFix(rs.getObject(40)));
					pdf.WaitTimeFld        .setText(nullFix(rs.getObject(41)));
					pdf.LogicalReadsFld    .setText(nullFix(rs.getObject(42)));
					pdf.PhysicalReadsFld   .setText(nullFix(rs.getObject(43)));
					pdf.PagesReadFld       .setText(nullFix(rs.getObject(44)));
					pdf.PhysicalWritesFld  .setText(nullFix(rs.getObject(45)));
					pdf.PagesWrittenFld    .setText(nullFix(rs.getObject(46)));
					pdf.MemUsageKBFld      .setText(nullFix(rs.getObject(47)));
					pdf.ScanPgsFld         .setText(nullFix(rs.getObject(48)));
					pdf.IdxPgsFld          .setText(nullFix(rs.getObject(49)));
					pdf.TmpTblFld          .setText(nullFix(rs.getObject(50)));
					pdf.UlcBytWriteFld     .setText(nullFix(rs.getObject(51)));
					pdf.UlcFlushFld        .setText(nullFix(rs.getObject(52)));
					pdf.ULCFlushFullFld    .setText(nullFix(rs.getObject(53)));
					pdf.TransactionsFld    .setText(nullFix(rs.getObject(54)));
					pdf.CommitsFld         .setText(nullFix(rs.getObject(55)));
					pdf.RollbacksFld       .setText(nullFix(rs.getObject(56)));
					pdf.PacketsSentFld     .setText(nullFix(rs.getObject(57)));
					pdf.PacketsReceivedFld .setText(nullFix(rs.getObject(58)));
					pdf.BytesSentFld       .setText(nullFix(rs.getObject(59)));
					pdf.BytesReceivedFld   .setText(nullFix(rs.getObject(60)));
				}
				else
				{
					pdf.statusBarLbl.setText("Process disconnected");
					stopRefresh();
				}
			}

			//------------------------------------
			// refresh current statement
			// current statements will be stored in currentStmtRows and aplied to the JTable later
			//------------------------------------
			currentStmtRows               = null;
			boolean currentStmtHasRow     = false;
			boolean currentStmtHasChanged = false;
			if (stmtStat)
			{
				/* 
				 * 12.5.4 version of monProcessStatement
				 * SPID        KPID        DBID        ProcedureID PlanID      BatchID     ContextID   LineNumber  CpuTime     WaitTime    MemUsageKB  PhysicalReads LogicalReads PagesModified PacketsSent PacketsReceived NetworkPacketSize PlansAltered RowsAffected StartTime                      
				 */
				String extraCols = "";
				if (_aseVersion >= 15002 || (_aseVersion >= 12540 && _aseVersion <= 15000) )
				{
					extraCols = "  S.RowsAffected, \n";
				}

//				String sql = 
//					"select  SPID, KPID, BatchID, LineNumber, dbname=db_name(DBID), procname=isnull(object_name(ProcedureID,DBID),''), \n" 
//					+ "  CpuTime, WaitTime, ExecTimeInMs=datediff(ms, StartTime, getdate()), MemUsageKB, PhysicalReads, LogicalReads, \n"
//					+ extraCols
//				    + "  PagesModified, PacketsSent, PacketsReceived, \n" 
//				    + "  NetworkPacketSize, PlansAltered, StartTime, PlanID, DBID, ProcedureID \n" 
//				    + "from monProcessStatement \n";
				String sql = 
					"select  S.SPID, S.KPID, S.BatchID, S.LineNumber, \n"
					+ "  dbname=db_name(S.DBID), procname=isnull(object_name(S.ProcedureID,S.DBID),''), \n"
					+ "  P.Command, S.CpuTime, S.WaitTime, \n"
//					+ "  ExecTimeInMs=datediff(ms, S.StartTime, getdate()), \n"
					+ "  ExecTimeInMs = CASE WHEN datediff(day, S.StartTime, getdate()) > 20 THEN -1 ELSE  datediff(ms, S.StartTime, getdate()) END, \n"  // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
					+ "  S.MemUsageKB, S.PhysicalReads, S.LogicalReads, \n"
					+ extraCols
					+ "  P.Application, P.Login, \n"
					+ "  P.WaitEventID, WaitEventDesc = '', \n"
					+ "  P.SecondsWaiting, P.BlockingSPID, \n"
					+ "  S.PagesModified, S.PacketsSent, S.PacketsReceived, \n"
					+ "  S.NetworkPacketSize, S.PlansAltered, \n"
					+ "  S.StartTime, S.PlanID, S.DBID, S.ProcedureID, \n"
					+ "  P.SecondsConnected, P.EngineNumber, P.NumChildren \n" 
					+ "from monProcessStatement S, monProcess P \n"
					+ "where S.KPID = P.KPID\n";

				Configuration conf = Configuration.getCombinedConfiguration();
				String extraWhere = conf.getProperty(ProcessDetailFrame.PROP_CURRENT_STATEMENT_EXTRA_WHERE);
				String orderBy    = conf.getProperty(ProcessDetailFrame.PROP_CURRENT_STATEMENT_ORDER_BY);

				if (StringUtil.isNullOrBlank(extraWhere))
					extraWhere = " 1 = 1 ";
				if (StringUtil.isNullOrBlank(orderBy))
					orderBy = "S.LogicalReads desc \n";

				if (kpid > 0)
					sql += "  and S.KPID = " + Integer.toString(kpid) + " \n";
				else
					sql += "  and S.SPID != @@spid \n";
				sql += "  and " + extraWhere + " \n";
				sql += "order by " + orderBy;

				currentSql = sql;
				_activeStatementsSql = sql;
				rs = stmt.executeQuery(sql);
				ResultSetMetaData rsmdCurStmt = rs.getMetaData();
				int nbColsCurStmt = rsmdCurStmt.getColumnCount();
				if (currentStmtColNames == null)
				{
					currentStmtColNames = new Vector();
					for (int i = 1; i <= nbColsCurStmt; i++)
					{
						String colName = rsmdCurStmt.getColumnName(i);
						currentStmtColNames.add(colName);

						if (colName.equals("SPID")       ) pos_currentSpid        = i;
						if (colName.equals("KPID")       ) pos_currentKpid        = i;
						if (colName.equals("BatchID")    ) pos_currentBatchID     = i;
						if (colName.equals("LineNumber") ) pos_currentSqlLine     = i;
						if (colName.equals("dbname")     ) pos_currentDbname      = i;
						if (colName.equals("procname")   ) pos_currentProcName    = i;
						if (colName.equals("PlanID")     ) pos_currentPlanID      = i;
						if (colName.equals("DBID")       ) pos_currentDBID        = i;
						if (colName.equals("ProcedureID")) pos_currentProcedureID = i;
						
						if (colName.equals("WaitEventDesc")) pos_currentWaitEventDesc = i;
						if (colName.equals("WaitEventID")) pos_currentWaitEventID = i;
					}
				}

				currentSpid        = -1;
				currentKpid        = -1;
				currentBatchID     = -1;
				currentSqlLine     = -1;
				currentDbname      = null;
				currentProcName    = null;
				currentPlanID      = -1;
				currentDBID        = -1;
				currentProcedureID = -1;
				
				MonTablesDictionary mtd = MonTablesDictionary.getInstance();

				boolean firstRow = true;
				while (rs.next())
				{
					Vector row = new Vector();
					for (int i = 1; i <= nbColsCurStmt; i++)
					{
						Object o = rs.getObject(i);
						
						if (i == pos_currentWaitEventDesc && mtd != null)
						{
							int witEventId = rs.getInt(pos_currentWaitEventID);
							o = mtd.getWaitEventDescription(witEventId);
						}
						row.add(o);
					}
					if (currentStmtRows == null)
					{
						currentStmtRows = new Vector();
					}
					currentStmtRows.add(row);

					// old code - for only 1 row
					//currentStmtRow = new Vector();
					//for (int i = 1; i <= nbColsCurStmt; i++)
					//{
					//	currentStmtRow.add(rs.getObject(i));
					//}

					if (firstRow)
					{
						currentStmtHasRow  = true;
						firstRow           = false;
						currentSpid        = rs.getInt(pos_currentSpid);
						currentKpid        = rs.getInt(pos_currentKpid);
						currentBatchID     = rs.getInt(pos_currentBatchID);
						currentSqlLine     = rs.getInt(pos_currentSqlLine);
						currentDbname      = rs.getString(pos_currentDbname).trim();
						currentProcName    = rs.getString(pos_currentProcName).trim();
						currentPlanID      = rs.getInt(pos_currentPlanID);
						currentDBID        = rs.getInt(pos_currentDBID);
						currentProcedureID = rs.getInt(pos_currentProcedureID);

						if (currentBatch == null)
						{
							currentBatch = new Batch();
						}

						// If the another statement
						if (    currentBatch.spid    != currentSpid 
						     && currentBatch.kpid    != currentKpid 
						     && currentBatch.batchId != currentBatchID
						   )
						{
							currentStmtHasChanged      = true;
							if (_logger.isDebugEnabled())
							{
								_logger.debug("currentStmtHasChanged: currentBatch.spid("+currentBatch.spid+") != currentSpid("+currentSpid+")   &&   currentBatch.kpid("+currentBatch.kpid+") != currentKpid("+currentKpid+")   &&   currentBatch.batchId("+currentBatch.batchId+") != currentBatchID("+currentBatchID+").");
							}

							currentBatch.spid          = currentSpid;
							currentBatch.kpid          = currentKpid;
							currentBatch.batchId       = currentBatchID;
							currentBatch.dbid          = currentDBID;
							currentBatch.dbname        = currentDbname;
							currentBatch.procedureId   = currentProcedureID;
							currentBatch.procedureName = currentProcName;
							currentBatch.planId        = currentPlanID;
						}

						// Always change these values, for first running statement
						currentBatch.lineNumber    = currentSqlLine;
					}
				}
				// Refresh of the associated JTable will be done at the end, in
				// the swing thread
			}
			if ( ! currentStmtHasRow )
			{
				currentBatch = null;
			}

			// If   NO rows from the captured SQL is choosed
			// and  current SQL is empty
			// SET the batch field to empty
			if (selectedStatement == -1  && ! currentStmtHasRow)
				pdf.batchTextArea.setText("");


			
			//------------------------------------
			// Refresh the current SQL text
			// Do only get SQL text for the "first" rows fetched from 
			//   curently executed statements
			//------------------------------------
//			_logger.debug("SQLTextMonitor="+SQLTextMonitor+", batchCapture="+batchCapture+", currentStmtHasRow="+currentStmtHasRow+", currentStmtHasChanged="+currentStmtHasChanged);
			if (    SQLTextMonitor     // is enabled
			     && batchCapture       // is enabled
			     && currentStmtHasRow  // there ARE SQL currently running
			     && currentStmtHasChanged
			   )
			{
				// get this new batch SQL text
				String sql = 
					"select SPID, KPID, BatchID, LineNumber, SQLText \n" 
					+ "from monProcessSQLText \n" 
					+ "where KPID=" + currentBatch.kpid + " \n"
					+ " and  SPID=" + currentBatch.spid + " \n"
					+ " and  BatchID=" + currentBatch.batchId + " \n" 
					+ " order by SPID, KPID, LineNumber, SequenceInLine \n";

				_logger.debug("EXEC SQL: " + sql);

				// -----------------------------------------------------
				// monProcessSQLText and monSysSQLText work in a small different ways
				// with line truncations and newlines...
				//
				// monProcessSQLText, seems to chop (parses for) newlines from the 
				// column SQLText but has the LineNumber column to figgure out if
				// a SQL statement has been wraped over 255 chars... 
				// and "" empty lines will just be skiped (meaning LineNumber hops over one row)
				//SPID        KPID        BatchID     LineNumber  SQLText                                         
				//----------- ----------- ----------- ----------- -------                                         
				//         13     7536755         207           1 select SPID, KPID, BatchID, LineNumber, SQLText,
				//         13     7536755         207           1 4567890:16-1234567890:17-1234567890:18-123456789
				//         13     7536755         207           2 from monProcessSQLText                          
				//         13     7536755         207           3 where SPID = @@spid                             
				//         13     7536755         207           4                                                 
				//         13     7536755         207           5 select * from monSysSQLText                     
				//         13     7536755         207           6 where SPID = @@spid                             
				//
				// monSysSQLText, seems to INCLUDE newlines in the SQLText
				// and fill out the SQLText column to 255 chars...
				// SequenceInBatch is just what 255 chunk we are reading...
				// -----------------------------------------------------

				currentSql = sql;
				rs = stmt.executeQuery(sql);
				int saveLineNumber = 1;
				String sqlText = "";
				while (rs.next())
				{
					// Add a newline, before we "enter" a new LineNumber
					int atLineNum =  rs.getInt(4);
					
					// Add newlines for "" empty SQL lines...
					// For example if the First row from monProcessSQLText
					// has LineNumber 5, then there should be 4 newlines added.
					while (saveLineNumber < atLineNum)
					{
						sqlText += "\n";
						saveLineNumber++;

						// do not end up in a endless loop...
						if (saveLineNumber > 10240) 
							break;
					}

					if (atLineNum == saveLineNumber)
					{
						sqlText += rs.getString(5);
					}
					else
					{
						sqlText += rs.getString(5);
						sqlText += "\n";
					}

					saveLineNumber = atLineNum;
				}
				// Add the composed string
				if ( ! sqlText.equals("") )
					currentBatch.appendSqlText( sqlText );
					
//				int curLine = 0;
//				StringBuffer lineSb = null;
//				while (rs.next())
//				{
//					int lineNum = rs.getInt(4);
//					if (lineNum != curLine)
//					{
//						if (lineSb != null)
//							currentBatch.addSqlTextLine(lineSb); // add previous lineSb to batch
//						curLine = lineNum;
//						lineSb = new StringBuffer();
//					}
//					lineSb.append(rs.getString(5));
//				}
//				if (lineSb != null)
//					currentBatch.addSqlTextLine(lineSb); // add last lineSb to batch

				//------------------------------------
				// Get current plan using sp_showplanfull 
				// if user has sa_role
				//------------------------------------
				if (has_sa_role)
				{
					// if no showplan exists in currentBatch
					// If the plan/object cant be found in compiledPlansCache
					//...
//					if ()
					_logger.debug("EXEC sp_showplanfull " + currentSpid);

					String showplanSql = "sp_showplanfull " + currentSpid; 
					currentSql = showplanSql;
					stmt.executeUpdate(showplanSql);

					StringBuffer planSb = null;
					SQLWarning sqlw = stmt.getWarnings();
					while (true)
					{
						// _logger.debug(sqlw.getErrorCode()+" " +
						// sqlw.getSQLState() +" "+sqlw.getMessage());
						if (planSb == null)
							planSb = new StringBuffer();

						// Ignore "10233 01ZZZ The specified statement
						// number..." message
						if (sqlw.getErrorCode() == 10233)
							break;
						// Ignore "010P4: An output parameter was received and
						// ignored." message
						if (sqlw.getSQLState() == "010P4")
							break;

						planSb = planSb.append(sqlw.getMessage());
						sqlw = sqlw.getNextWarning();
						if (sqlw == null)
							break;
					}
					if (planSb != null)
					{
						currentBatch.addShowplanTextLine(planSb);
					}

					// FIXME
					// Maybe add the PLAN (planId) to 
					// a compiledProcShowplan cache
					if (currentBatch.procedureId > 0)
					{
						//procedureTextCache
						String key = Integer.toString(currentBatch.planId);
//						compiledPlansCache.put(key, currentBatch);
					}
				}

				// save this batch in history
				addBatchHistory(currentBatch);
			}

			// Show this in the GUI, it might takes along time
			// to do the rest of the SQL below
			if (selectedStatement == -1)
			{
				displayCurrentBatch();
			}
			
			
			//------------------------------------
			// Get recent statements
			// store them in newCapturedStatements, which will be moved over to the TableModel later
			//------------------------------------
			Hashtable restrictionsCapturedKeys = new Hashtable(); 
			newCapturedStatements = null;
			if (stmtPipe && stmtPipeMsg)
			{
//				 ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ 
//				 Adaptive Server Enterprise/15.0.3/EBF 17770 ESD#4/P/x86_64/Enterprise Linux/ase1503/2768/64-bit/FBO/Thu Aug 26 09:54:27 2010                                                                                             
//				 
//				 SPID   KPID        DBID        ProcedureID PlanID      BatchID     ContextID   LineNumber  CpuTime     WaitTime    MemUsageKB  PhysicalReads LogicalReads PagesModified PacketsSent PacketsReceived NetworkPacketSize PlansAltered RowsAffected ErrorStatus HashKey     SsqlId      ProcNestLevel StatementNumber DBName                                                       StartTime                  EndTime                    
//				 ------ ----------- ----------- ----------- ----------- ----------- ----------- ----------- ----------- ----------- ----------- ------------- ------------ ------------- ----------- --------------- ----------------- ------------ ------------ ----------- ----------- ----------- ------------- --------------- ------------------------------------------------------------ -------------------------- -------------------------- 

				 // NOTE: do not forget to change:  
				//       SQL table definition file
				//       in method: saveCapturedStatements

				String extraCols = "";
				if (_aseVersion >= 15002 || (_aseVersion >= 12540 && _aseVersion <= 15000) )
				{
					extraCols = "       RowsAffected, ErrorStatus, \n";
				}
				if (_aseVersion >= 15030 )
				{
					extraCols += "      ProcNestLevel, StatementNumber, \n";
				}
				
				
				String sql =
					"select SPID, KPID, BatchID, LineNumber, \n" +
					"       dbname=db_name(DBID), \n" +
					"       procname=isnull(object_name(ProcedureID,DBID),''), \n" +
//					"       Elapsed_ms=datediff(ms,StartTime, EndTime), \n" +
					"       Elapsed_ms = CASE WHEN datediff(day, StartTime, EndTime) > 20 THEN -1 ELSE  datediff(ms, StartTime, EndTime) END, \n" + // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
					"       CpuTime, WaitTime, MemUsageKB, PhysicalReads, LogicalReads, \n" +
					extraCols +
					"       PagesModified, PacketsSent, \n" +
					"       PacketsReceived, NetworkPacketSize, \n" +
					"       PlansAltered, StartTime, EndTime, \n" +
					"       PlanID, DBID, ProcedureID \n" +
					"from monSysStatement \n" +
					"where 1 = 1\n";

				
				if (kpid > 0)
					sql += "and KPID = " + Integer.toString(kpid) + "\n";
				else
					sql += "and SPID != @@spid\n";

				if ( _captureRestrictions )
				{
					sql += "and (" + _captureRestrictionSql + ")\n";
				}

				// EMPTY monSysStatement on first execution.
				if ( _firstTimeSample && _discardPreOpenStmnts )
				{
					// If first time... 
					// discard everything in the transient monSysStatement table
					//sql += "and SPID < 0\n";
					long startTime = System.currentTimeMillis();
					_logger.info("BEGIN: Discarding everything in the transient monSysStatement table in the first sample.");

					currentSql = "select count(*) from monSysStatement";
					rs = stmt.executeQuery(currentSql);
					while(rs.next()) 
					{
						// DO nothing, we are "emptying" the table
					}
					rs.close();
					_logger.info("END:   Discarding everything in the transient monSysStatement table in the first sample. this took "+TimeUtils.msToTimeStr(System.currentTimeMillis()-startTime)+".");
				}

				currentSql = sql;
				_historyStatementsSql = sql;

				rs = stmt.executeQuery(sql);
				ResultSetMetaData rsmd = rs.getMetaData();
				int nbCols = rsmd.getColumnCount();
				if (capturedStmtColNames == null)
				{
					capturedStmtColNames = new Vector();
					for (int i = 1; i <= nbCols; i++)
					{
						String colName = rsmd.getColumnName(i);
						capturedStmtColNames.add(colName);

						if (colName.equals("SPID")       ) pos_capturedSpid        = i;
						if (colName.equals("KPID")       ) pos_capturedKpid        = i;
						if (colName.equals("BatchID")    ) pos_capturedBatchID     = i;
						if (colName.equals("LineNumber") ) pos_capturedSqlLine     = i;
						if (colName.equals("dbname")     ) pos_capturedDbname      = i;
						if (colName.equals("procname")   ) pos_capturedProcName    = i;
						if (colName.equals("PlanID")     ) pos_capturedPlanID      = i;
						if (colName.equals("DBID")       ) pos_capturedDBID        = i;
						if (colName.equals("ProcedureID")) pos_capturedProcedureID = i;
					}
				}
				newCapturedStatements = new Vector();
				while (rs.next())
				{
					// Add KEY elemets to a temporary array, just so we know WHAT
					// WHAT key elements we can KEEP later on
					// This is only valid if _captureRestrictions is set to something
					if (_captureRestrictions)
					{
						int lSpid    = rs.getInt(1);
						int lKpid    = rs.getInt(2);
						int lBatchId = rs.getInt(3);
						
						String key = lSpid + ":" + lKpid + ":" + lBatchId;
						restrictionsCapturedKeys.put(key, "dummy");
					}

					// Add the FULL row to the Vector
					Vector row = new Vector();
					for (int i = 1; i <= nbCols; i++)
					{
						row.add(rs.getObject(i));
					}
					newCapturedStatements.add(row);
				}
			}

			//------------------------------------
			// Get recent SQL TEXT into batchs
			//------------------------------------
			if (    sqlTextSample        // is checkbox "ON"
				 && sqlTextPipe 
				 && sqlTextPipeMsg 
				 && SQLTextMonitor)
			{
				String sql =
					"select SPID, KPID, BatchID, SQLText \n" +
					"from monSysSQLText \n" +
					"where 1=1 \n";

				if (kpid > 0)
					sql += "and KPID=" + Integer.toString(kpid);

				if ( _firstTimeSample && _discardPreOpenStmnts )
				{
//					// If first time... 
//					sql += "and SPID < 0\n";
//					_logger.info("Discarding everything in the transient monSysSQLText table in the first sample.");

					long startTime = System.currentTimeMillis();
					_logger.info("BEGIN: Discarding everything in the transient monSysSQLText table in the first sample.");

					currentSql = "select count(*) from monSysSQLText";
					rs = stmt.executeQuery(currentSql);
					while(rs.next()) 
					{
						// DO nothing, we are "emptying" the table
					}
					rs.close();
					_logger.info("END:   Discarding everything in the transient monSysSQLText table in the first sample. this took "+TimeUtils.msToTimeStr(System.currentTimeMillis()-startTime)+".");
				}

				sql += " order by SPID, KPID, BatchID, SequenceInBatch \n";

				currentSql = sql;
				rs = stmt.executeQuery(sql);
				String       lastKey = "";
				Batch        batch   = null;
				while (rs.next())
				{
					int lSpid       = rs.getInt(1);
					int lKpid       = rs.getInt(2);
					int lBatchId    = rs.getInt(3);
					String lSqlText = rs.getString(4);

					String lKey = lSpid + ":" + lKpid + ":" + lBatchId;

					// OK, new SQL statement found
					if ( ! lastKey.equals(lKey) )
					{
						// Post priviously batches
						if (batch != null)
						{
							addBatchHistory(batch);
							batch = null;
						}
						
						// Can we skip this row?
						// If it's not part of the captured rows in previous section
						boolean addBatch = true;
						if (_captureRestrictions)
						{
							if ( restrictionsCapturedKeys.get(lKey) == null)
							{
								addBatch = false;
							}
						}
						// Create a new one
						if (addBatch)
						{
							batch = new Batch(lSpid, lKpid, lBatchId);
							batch.appendSqlText(lSqlText);
						}
					}
					else // same statement, so add text to it
					{
						if (batch != null)
							batch.appendSqlText(lSqlText);
					}

					// important...
					lastKey = lKey;
				}
				// Add any "leftovers" that wasnt added in the loop above
				if (batch != null)
				{
					addBatchHistory(batch);
				}
			}

			//------------------------------------
			// Get recent plans
			//------------------------------------
			if (planTextSample && planTextPipe && planTextPipeMsg)
			{
				String sql =
					"select SPID, KPID, BatchID, PlanID, PlanText, DBID, ProcedureID \n" + 
					"from monSysPlanText \n" +
					"where 1=1 \n";

				if (kpid > 0)
					sql += " and KPID=" + Integer.toString(kpid);

				if ( _firstTimeSample && _discardPreOpenStmnts )
				{
//					// If first time... 
//					sql += "and SPID < 0\n";
//					_logger.info("Discarding everything in the transient monSysPlanText table in the first sample.");

					long startTime = System.currentTimeMillis();
					_logger.info("BEGIN: Discarding everything in the transient monSysPlanText table in the first sample.");

					rs = stmt.executeQuery("select count(*) from monSysSQLText");
					while(rs.next()) 
					{
						// DO nothing, we are "emptying" the table
					}
					rs.close();
					_logger.info("END:   Discarding everything in the transient monSysPlanText table in the first sample. this took "+TimeUtils.msToTimeStr(System.currentTimeMillis()-startTime)+".");
				}

				sql += " order by SPID, KPID, PlanID, SequenceNumber";

//				rs = stmt.executeQuery(sql);
//				int curSpid    = -1;
//				int curKpid    = -1;
//				int curBatchId = -1;
//				int curPlanId  = -1;
//				int curProcId  = -1;
//				int curDbId    = -1;
//				Batch batch    = null;
//				StringBuffer planSb = null;
//				while (rs.next())
//				{
//					int l_spid  = rs.getInt(1);
//					int l_kpid  = rs.getInt(2);
//					int l_batchID = rs.getInt(3);
//					int l_planID  = rs.getInt(4);
//					int l_dbId    = rs.getInt(6);
//					int l_procId  = rs.getInt(7);
//					// _logger.debug(batchID+"-"+planID + " : " + rs.getString(5));
//					if (l_spid != curSpid || l_kpid != curKpid || l_planID != curPlanId || l_batchID != curBatchId || l_dbId != curDbId || l_procId != curProcId)
//					{
//						if (curPlanId != -1)
//						{
//							// add previous batch
//							addPlanHistory(curSpid, curKpid, curBatchId, curDbId, curProcId, batch, planSb);
//						}
//						curSpid    = l_spid;
//						curKpid    = l_kpid;
//						curBatchId = l_batchID;
//						curPlanId  = l_planID;
//						curDbId    = l_dbId;
//						curProcId  = l_procId;
//						batch      = new Batch(curPlanId);
//						planSb     = new StringBuffer();
//					}
//					planSb.append(rs.getString(5));
//				}
//				if (curPlanId != -1)
//				{
//					// add last batch
//					addPlanHistory(curSpid, curKpid, curBatchId, curDbId, curProcId, batch, planSb);
//				}

// SEMI NEW				
//				rs = stmt.executeQuery(sql);
//				String       lastKey = "";
//				Batch        batch   = null;
//				while (rs.next())
//				{
//					int    lSpid       = rs.getInt(1);
//					int    lKpid       = rs.getInt(2);
//					int    lBatchId    = rs.getInt(3);
//					int    lPlanID     = rs.getInt(4);
//					String lPlanText   = rs.getString(5);
//					int    lDbId       = rs.getInt(6);
//					int    lProcId     = rs.getInt(7);
//
//					String lKey  = lSpid + ":" + lKpid + ":" + lBatchId + ":" + lProcId;
//					String lKeyX = lSpid + ":" + lKpid + ":" + lBatchId;
//
//					// OK, new SQL statement found
//					if ( ! lastKey.equals(lKey) )
//					{
//						// Post priviously batches
//						if (batch != null)
//						{
//							addPlanHistory(batch);
//							batch = null;
//						}
//						
//						// Can we skip his row?
//						// If it's not part of the captured rows in previous section
//						boolean addBatch = true;
//						if (_captureRestrictions)
//						{
//							if ( restrictionsCapturedKeys.get(lKeyX) == null)
//							{
//								addBatch = false;
//							}
//						}
//						// Create a new one
//						if (addBatch)
//						{
//							batch = new Batch(lSpid, lKpid, lBatchId);
//							batch.procedureId = lProcId;
//							batch.appendShowplanText(lPlanText);
//						}
//					}
//					else // same statement, so add text to it
//					{
//						if (batch != null)
//							batch.appendShowplanText(lPlanText);
//					}
//
//					// important...
//					lastKey = lKey;
//				}
//				// Add any "leftovers" that wasnt added in the loop above
//				if (batch != null)
//				{
//					addPlanHistory(batch);
//				}

				
				currentSql = sql;
				rs = stmt.executeQuery(sql);
				String       lastKey = "";
				Batch        batch   = null;
				while (rs.next())
				{
					int    lSpid       = rs.getInt(1);
					int    lKpid       = rs.getInt(2);
					int    lBatchId    = rs.getInt(3);
					int    lPlanID     = rs.getInt(4);
					String lPlanText   = rs.getString(5);
					int    lDbId       = rs.getInt(6);
					int    lProcId     = rs.getInt(7);

					String lKey  = lSpid + ":" + lKpid + ":" + lBatchId;

					// OK, new SQL statement found
					if ( ! lastKey.equals(lKey) )
					{
						// Can we skip his row?
						// If it's not part of the captured rows in previous section
						boolean addBatch = true;
						if (_captureRestrictions)
						{
							if ( restrictionsCapturedKeys.get(lKey) == null)
							{
								addBatch = false;
							}
						}
						// Get previous Batch and start to fill the batch with SHOWPLAN text
						if (addBatch)
						{
							batch = getBatchHistory(lKey);

							if (batch != null)
							{
								batch.dbid        = lDbId;
								batch.procedureId = lProcId;
								batch.planId      = lPlanID;
								batch.appendShowplanText(lPlanText);
							}
						}
					}
					else // same statement, so add text to it
					{
						if (batch != null)
							batch.appendShowplanText(lPlanText);
					}

					// important...
					lastKey = lKey;
				}
			}

//			//------------------------------------
//			// Get current plan if not found in planHistory and if user has sa_role
//			//------------------------------------
//			if (has_sa_role && (currentBatchID != -1))
//			{
//				// Check if plan exists
//				Batch batch = getPlanHistory(currentSpid, currentKpid, currentBatchID, currentDBID, currentProcedureID);
//				if (batch == null)
//				{
//					stmt.executeUpdate("sp_showplanfull " + currentSpid);
//					StringBuffer planSb = null;
//					SQLWarning sqlw = stmt.getWarnings();
//					while (true)
//					{
//						// _logger.debug(sqlw.getErrorCode()+" " +
//						// sqlw.getSQLState() +" "+sqlw.getMessage());
//						if (planSb == null)
//							planSb = new StringBuffer();
//
//						// Ignore "10233 01ZZZ The specified statement
//						// number..." message
//						if (sqlw.getErrorCode() == 10233)
//							break;
//						// Ignore "010P4: An output parameter was received and
//						// ignored." message
//						if (sqlw.getSQLState() == "010P4")
//							break;
//
//						planSb = planSb.append(sqlw.getMessage());
//						sqlw = sqlw.getNextWarning();
//						if (sqlw == null)
//							break;
//					}
//					if (planSb != null)
//					{
//						batch = new Batch(currentBatchID);
//						addPlanHistory(currentSpid, currentKpid, currentBatchID, currentDBID, currentProcedureID, batch, planSb);
//					}
//				}
//			}

			// If CURRENT SQL is selected, show some stuff.
			if (selectedStatement == -1)
			{
				// If anything is running for the moment
//				if (currentBatchID != -1)
//				{
//					// Display current plan
//					displayPlan(currentSpid, currentKpid, currentBatchID, 0, currentDBID, currentProcedureID, currentSqlLine);
//				}
				if (currentBatch != null)
				{
					// Display current plan
					displayPlan(currentBatch.spid, currentBatch.kpid, currentBatch.batchId, 
							currentBatch.planId, 
							currentBatch.dbid, currentBatch.procedureId, 
							currentBatch.lineNumber);
				}
				else
				{
					// Empty the PLAN text area
					pdf.planTextArea.setText("");
				}
			}

			// Update panel
			Runnable updatePanel_inSwingThread = new Runnable()
			{
				public void run()
				{
					updatePanel_code();
				}
			};
			SwingUtilities.invokeLater(updatePanel_inSwingThread);

			//
			_firstTimeSample = false;
		}
		catch (SQLException SQLEx)
		{
			_logger.error("SQL Capture Tool, RefreshProcess: Msg="+SQLEx.getErrorCode()+", caught:" + SQLEx);
			_logger.error("Got SQL error(s) when refreshing information. SQL="+currentSql, SQLEx);

			pdf.setRefreshError(SQLEx);
			
			pdf.statusBarLbl.setForeground(Color.RED);
			pdf.statusBarLbl.setText("Error when executing SQL, check 'Restrictions syntax'. ASE Message '"+SQLEx.getMessage()+"'.");

			if (AseTune.hasGUI())
			{
				String htmlMsg = "<html>" +
					"Problems executing SQL Statement.<br>" +
					"ASE Error Number '"+SQLEx.getErrorCode()+"'<br>" +
					"ASE SQL State '"+SQLEx.getSQLState()+"'<br>" +
					"ASE Message '"+SQLEx.getMessage()+"'<br>" +
					"<br>" +
					"<b>Check the below SQL and see if any of your 'Extra Where Clauses / order by' may cause them.</b><br>" +
					"<i>TIP: Check and Use alias for table names...</i><br>" +
					"<br>" +
					"<b>NOTE: The refresh process has been PAUSED, so fix the problem and resume the refresh process.</b><br>" +
					"<pre>" +
					currentSql +
					"<pre>" +
					"</html>";
				SwingUtils.showErrorMessage("Error when executing SQL", htmlMsg, SQLEx);
				
				setPauseProcess(true);
			}
		}
	}

	public void setTextFix(JTextArea text) 
	{
		StringTokenizer st = new StringTokenizer(text.getText(),"\n",true);
		int linenumber = 0;
		int count = 0;
		while ( st.hasMoreTokens() )
		{
			String s = st.nextToken();
			count += s.length();

			if (s.equals("\n")) 
				linenumber++;
		}
		// set rows
		_logger.debug("setting JTextArea to rows='"+linenumber+"'");
		text.setRows(linenumber);

	}

	public void setSelectionAndMoveToTop(JTextArea text, int start, int end) 
	{
		if (start > 0  && end > 0)
		{
			// MARK the text as "selected" with the text colour red
			text.setSelectedTextColor(Color.RED);
			text.setCaretPosition(start);
			text.setSelectionStart(start);
			text.setSelectionEnd(end);
			// workaround to get the selection visible
			text.getCaret().setSelectionVisible(true);

			// Move the marked text to "top of the text field"
			try
			{
				//Rectangle rectAtPos = text.modelToView(start);
				Point pointAtPos = text.modelToView(start).getLocation();
				pointAtPos.x = 0; // Always point to LEFT in the viewport/scrollbar
				
				_logger.debug("text.modelToView(start): "+ pointAtPos);
				Container parent = text.getParent();
				while ( parent != null )
				{
					// if viewport, move the position to "top of viewport"
					if (parent instanceof JViewport)
					{
						((JViewport)parent).setViewPosition(pointAtPos);
						break;
					}
					// if scrollpane, move the position to "top of viewport"
					if (parent instanceof JScrollPane)
					{
						((JScrollPane)parent).getViewport().setViewPosition(pointAtPos);
						break;
					}
					// Get next parent if it's NOT a JViewport or JScrollPane
					parent = parent.getParent();
				}
			}
			catch (BadLocationException e)
			{
				_logger.debug("text.modelToView(): " + e);
			}
		}
	}
	
	public void setCaretToBatchLine(JTextArea text, int linenumber) 
	{
//		text.setCaretPosition(0);
		if (linenumber <= 0) 
			return;

		StringTokenizer st = new StringTokenizer(text.getText(),"\n",true);
		int count = 0;
		int countRowAfter = 0;
		while (st.hasMoreTokens() && (linenumber>1))
		{
			String s = st.nextToken();
			count += s.length();
			if (s.equals("\n")) 
				linenumber--;
		}
		// Look for next row aswell, this so we can "mark" the linenumber
		if (st.hasMoreTokens())
		{
			String s = st.nextToken();
			countRowAfter = count + s.length();
			countRowAfter += 1;
		}

		setSelectionAndMoveToTop(text, count, countRowAfter);
	}

	public void setCaretToPlanLine(JTextArea text, int sqlLine) 
	{
//		text.setCaretPosition(0);

		int start = text.getText().indexOf("(at line "+sqlLine+")");
		int end = text.getText().indexOf("\n", start);
		end += 1;

		setSelectionAndMoveToTop(text, start, end);
	}

	public void displayProcInBatchWindow(String dbname, String procName, int procLine)
	{
		String sqlStatement = null;
		StringBuffer procTextSb  = null;
		String       procTextStr = null;
		String       fullProcName = dbname+".dbo."+procName;

//		if (1==1)
//		throw new RuntimeException("debug exception, to find out from where it was called.");

		// If the procedure text is already loaded, dont do it again.
		if ( pdf.titledBorderBatch.getTitle().equals(fullProcName) )
		{
			setCaretToBatchLine(pdf.batchTextArea, procLine);
			return;
		}

		// get check if we already has the text for this procedure.
		procTextStr = (String) procedureTextCache.get(fullProcName);

		if (procTextStr != null)
		{
			_logger.debug("Found object '"+fullProcName+"' in procedureTextCache.");
		}
		else
		{
//			// First get the DBNAME and PROCNAME
//			sqlStatement = "select db_name("+dbId+"), object_name("+procId+", "+dbId+")";
//			try
//			{
//				Statement statement = conn.createStatement();
//				ResultSet rs = statement.executeQuery(sqlStatement);
//				while(rs.next())
//				{
//					dbname   = rs.getString(1)();
//					procName = rs.getString(2)();
//				}
//			}
//			catch (Exception e)
//			{
//				JOptionPane.showMessageDialog(null, "Executing SQL command '"+sqlStatement+"'. Found the following error:\n."+e, "Error", JOptionPane.ERROR_MESSAGE);
//			}
//			
//			_logger.debug("Getting object dbid='"+dbId+"', dbname='"+dbname+"', procId='"+procId+"', procName='"+procName+"' from database.");

			_logger.debug("Getting object '"+fullProcName+"' from database.");
//			/*
//			** See if the object is hidden (SYSCOM_TEXT_HIDDEN will be set)
//			*/
//			if exists (select 1
//			        from syscomments where (status & 1 = 
//			 1)
//			        and id = object_id(@objname))
//			begin
//			        /*
//			        ** 18406, "Source text for compiled object %!1
//			        ** (id = %!2) is hidden."
//			        */
//			        select @proc_id = object_id(@objname)
//			        raiserror 18406, @objname, @proc_id
//			       
//			  return (1)
//			end

			sqlStatement = "select c.text, c.status, c.id"
				+ " from "+dbname+"..sysobjects o, "+dbname+"..syscomments c"
				+ " where o.name = '"+procName+"'"
				+ "   and o.id = c.id"
				+ " order by c.number, c.colid2, c.colid";
	
			try
			{
				Statement statement = _conn.createStatement();
				ResultSet rs = statement.executeQuery(sqlStatement);
				while(rs.next())
				{
					String textPart = rs.getString(1);
					int    status   = rs.getInt(2);
					int    id       = rs.getInt(3);
					
					if (procTextSb == null)
						procTextSb = new StringBuffer();
	
					// if status is ASE: SYSCOM_TEXT_HIDDEN
					if ((status & 1) == 1)
					{
						procTextSb.append("ASE StoredProcedure Source text for compiled object '"+dbname+".dbo."+procName+"' (id = "+id+") is hidden.");
						break;
					}

					procTextSb.append(textPart);
				}
				rs.close();
			}
			catch (Exception e)
			{
				JOptionPane.showMessageDialog(null, "Executing SQL command '"+sqlStatement+"'. Found the following error:\n."+e, "Error", JOptionPane.ERROR_MESSAGE);
			}

			if (procTextSb != null)
			{
				procTextStr = procTextSb.toString();

				// Store the procedure cache text in cache
				procedureTextCache.put(fullProcName, procTextStr);
			}
		}

		if (procTextStr != null)
		{
			pdf.titledBorderBatch.setTitle(fullProcName);
			pdf.batchTextArea.setText(procTextStr);
			setCaretToBatchLine(pdf.batchTextArea, procLine);
			//setTextFix(pdf.batchTextArea);
		}
		else
		{
			pdf.batchTextArea.setText("Can't find text for procedure.");
		}
	}

	public void displayCurrentBatch()
	{
		pdf.titledBorderBatch.setTitle("Batch text");
		pdf.batchTextArea.setCaretPosition(0);
		pdf.batchTextArea.setFocusable(true);

		if (currentBatch == null)
		{
			pdf.batchTextArea.setText("");
			return;
		}

		if (    !currentBatch.dbname.equals("") 
			 && !currentBatch.procedureName.equals("") 
			 && !currentBatch.procedureName.startsWith("*")
			 && currentSqlLine > 0
			 && pdf.sqlTextShowProcSrcCheckbox.isSelected() 
		   )
		{
			// Display current running stored proc
			displayProcInBatchWindow(currentBatch.dbname, currentBatch.procedureName, currentBatch.lineNumber);

//			_logger.debug("BATCH SQL: " + currentBatch.getSqlText());
		}
		else
		{
			pdf.batchTextArea.setText( currentBatch.getSqlText() );
			setCaretToBatchLine(pdf.batchTextArea, currentSqlLine);
		}
	}

	public void displayBatch(int spid, int kpid, int batchId, int sqlLine)
	{
		pdf.titledBorderBatch.setTitle("Batch text");
//		pdf.batchTextArea.setCaretPosition(0);
//		pdf.batchTextArea.setFocusable(true);		
		pdf.batchTextArea.setText(null);

		// Find batch
		Batch batch = getBatchHistory(spid, kpid, batchId);
		if (batch == null)
		{
			pdf.batchTextArea.setText("");
			return;
		}
		if (batch.getBatchId() != batchId)
		{
			_logger.debug("AseTune.RefreshProcess : bd batchId, asked " + batchId + " returned " + batch.getBatchId());
		}

		pdf.batchTextArea.setText(batch.getSqlText(false));
		setCaretToBatchLine(pdf.batchTextArea, sqlLine);
		//setTextFix(pdf.batchTextArea);

	}

	public void displayPlan(int spid, int kpid, int batchId, int planId, int dbId, int procId, int sqlLine)
	{
		_logger.debug("displayPlan(): batchId="+batchId+", planId="+planId+", dbId="+dbId+", procId="+procId+", sqlLine="+sqlLine+".");
		pdf.planTextArea.setCaretPosition(0);

		// planID in monSysPlanText is not the same as planID in monSysStatement
		// (why ????), so use DBID and ProcedureID
		// Find batch
//		Batch batch = getPlanHistory(spid, kpid, batchId, dbId, procId);
		Batch batch = getBatchHistory(spid, kpid, batchId);
		if (batch == null)
		{
			pdf.planTextArea.setText("No SHOWPLAN text was found in the plan history.");
			return;
		}

		/*
		 * if (batch.getBatchId() != planId) { _logger.debug
		 * ("AseTune.RefreshProcess : bd batchId, asked "+planId+" returned
		 * "+batch.getBatchId()); }
		 */

		String planStr = batch.getShowplanText();
		pdf.planTextArea.setText(planStr);
		if (sqlLine >= 0)
			setCaretToPlanLine(pdf.planTextArea, sqlLine);
	}

	public void setSelectedStatement(int rowid)
	{
		_logger.debug("setSelectedStatement(rowid="+rowid+")");

		// This method is called *several* times...
		// So only do action when, rowid, chaanges
		// Make shure we only do stuf when the rowid changes
		if (rowid == selectedStatement)
			return;
		selectedStatement = rowid;

		// if rowid is -1, then it's the current running SQL that is choosed
		// if rowid >= 0, the it's the capturred statements we are looking at.
		if (rowid >= 0)
		{
			Object o = capturedStmtModel.getValueAt(rowid, 0);
			if (o == null)
			{
				_logger.error("Row "+rowid+" column 0, does NOT contain any data...");
				return;
			}
//			_logger.debug("setSelectedStatement()"
//					+ ", pos_capturedSpid="        + pos_capturedSpid 
//					+ ", pos_capturedKpid="        + pos_capturedKpid 
//					+ ", pos_capturedBatchID="     + pos_capturedBatchID 
//					+ ", pos_capturedSqlLine="     + pos_capturedSqlLine 
//					+ ", pos_capturedPlanID="      + pos_capturedPlanID
//					+ ", pos_capturedDBID="        + pos_capturedDBID
//					+ ", pos_capturedProcedureID=" + pos_capturedProcedureID
//					+ ", pos_capturedDbname="      + pos_capturedDbname
//					+ ", pos_capturedProcName="    + pos_capturedProcName
//					+ ".");

			int spid    = ((Number) (capturedStmtModel.getValueAt(rowid, pos_capturedSpid       -1))).intValue();
			int kpid    = ((Number) (capturedStmtModel.getValueAt(rowid, pos_capturedKpid       -1))).intValue();
			int batchId = ((Number) (capturedStmtModel.getValueAt(rowid, pos_capturedBatchID    -1))).intValue();
			int sqlLine = ((Number) (capturedStmtModel.getValueAt(rowid, pos_capturedSqlLine    -1))).intValue();
			int planId  = ((Number) (capturedStmtModel.getValueAt(rowid, pos_capturedPlanID     -1))).intValue();
			int dbId    = ((Number) (capturedStmtModel.getValueAt(rowid, pos_capturedDBID       -1))).intValue();
			int procId  = ((Number) (capturedStmtModel.getValueAt(rowid, pos_capturedProcedureID-1))).intValue();

			if (_logger.isDebugEnabled())
				_logger.debug("setSelectedStatement(), batchId='" + batchId + "', planId='" + planId + "', dbId='" + dbId + "', procId='" + procId + "', (sqlLine='"+sqlLine+"')");

			if (procId > 0 && sqlLine > 0 && pdf.sqlTextShowProcSrcCheckbox.isSelected())
			{
				String dbname   = (String) (capturedStmtModel.getValueAt(rowid, pos_capturedDbname  -1));
				String procname = (String) (capturedStmtModel.getValueAt(rowid, pos_capturedProcName-1));

				displayProcInBatchWindow(dbname, procname, sqlLine);
			}
			else
			{
				displayBatch(spid, kpid, batchId, sqlLine);
			}
			displayPlan(spid, kpid, batchId, planId, dbId, procId, sqlLine);
		}
		else // then it's the current running SQL that is choosed
		{
			if (    currentDbname   != null && !currentDbname.equals("")
			     && currentProcName != null && !currentProcName.equals("")
			     && currentProcName != null && !currentProcName.startsWith("*")
			     && currentSqlLine > 0
				 && pdf.sqlTextShowProcSrcCheckbox.isSelected() 
			   )
			{
				// Display current running stored proc
				displayProcInBatchWindow(currentDbname, currentProcName, currentSqlLine);
			}
			else
			{
				// Display current batch
				displayCurrentBatch();
			}
			displayPlan(currentSpid, currentKpid, currentBatchID, 0, currentDBID, currentProcedureID, currentSqlLine);
		}
	}

	public int getSelectedStatement()
	{
		return selectedStatement;
	}

	private void updatePanel_code()
	{
		// Refresh the current statement JTable
		if (!currentSMInitialized)
		{
			currentStmtModel.setDataVector(currentStmt, currentStmtColNames);
			currentSMInitialized = true;
		}
//		if (currentStmtRow != null)
//		{
//			// insert or update in the curStmt JTable
//			if (currentStmtModel.getRowCount() == 0)
//			{
//				currentStmtModel.addRow(currentStmtRow);
//				currentStmtModel.setRowCount(1);
//			}
//			else
//			{
//				for (int i = 0; i < currentStmtRow.size(); i++)
//				{
//					currentStmtModel.setValueAt(currentStmtRow.get(i), 0, i);
//				}
//			}
//		}
//		else if (currentStmtModel.getRowCount() == 1)
//		{
//			currentStmtModel.removeRow(0);
//			currentStmtModel.setRowCount(0);
//		}
		if (currentStmtRows != null)
		{
			// refresh
			currentStmtModel.setDataVector(currentStmtRows, currentStmtColNames);
			
			// resize the columns
			SwingUtils.calcColumnWidths(pdf.currentStatementTable);
		}
		else if (currentStmtModel.getRowCount() > 0)
		{
			// Clear the table
			currentStmtModel.setRowCount(0);
		}

		// Refresh the captured statement JTable
		if (newCapturedStatements != null)
		{
			if (!capturedSMInitialized)
			{
				capturedStmtModel.setDataVector(new Vector(), capturedStmtColNames);
				capturedSMInitialized = true;

				// resize the columns, it's only done when new header
				// It could be done always, but since we loop all rows in the table, 
				//    it might takte to long time to do this
				SwingUtils.calcColumnWidths(pdf.capturedStatementsTable);
				capturedTableColmnsIsResized = false;
			}
			for (int i = 0; i < newCapturedStatements.size(); i++)
			{
				capturedStmtModel.addRow((Vector) newCapturedStatements.get(i));

				// resize, but only check with for newly inserted rows.
				//TabularCntrPanel.calcColumnWidths(pdf.capturedStatementsTable, newCapturedStatements.size(), true);

				// resize, But only do this first time we see data
				if ( ! capturedTableColmnsIsResized )
				{
					capturedTableColmnsIsResized = true;
					SwingUtils.calcColumnWidths(pdf.capturedStatementsTable);
				}
			}
		}
	}

	
	private boolean getAseConfigBoolean(String cfgName)
	throws SQLException
	{
		boolean b = false;
		ResultSet rs = stmt.executeQuery("sp_configure '"+cfgName+"'");
		while ( rs.next() )
		{
			if (rs.getInt(5) > 0) 
				b = true;
		}
		rs.close();
		
		return b;
	}

	
	/**
	 * Cleanup memory structures
	 */
	public void clear()
	{
		_logger.debug("Before clear(), batchHistory had "+batchHistory.size()+" entries.");
		batchHistory.clear();

		_logger.debug("Before clear(), plansHistory had "+plansHistory.size()+" entries.");
		plansHistory.clear();

		_logger.debug("Before clear(), compiledPlansHistory had "+compiledPlansHistory.size()+" entries.");
		compiledPlansHistory.clear();

		_logger.debug("Before clear(), procedureTextCache had "+procedureTextCache.size()+" entries.");
		procedureTextCache.clear();

		_logger.debug("Before clear(), currentStmtModel had "+currentStmtModel.getRowCount()+" entries.");
		currentStmtModel.clear();

		_logger.debug("Before clear(), capturedStmtModel had "+capturedStmtModel.getRowCount()+" entries.");
		capturedStmtModel.clear();

		pdf.batchTextArea.setText("");
		pdf.planTextArea.setText("");
	}

	public void saveCapturedStatementsToFile()
  	{
		saveCapturedStatementsToFile(null, "", "", "");
  	}
	public void saveCapturedStatementsToFile(String saveToDir, String bcpFilename, String txtFilename, String tabDefFilename)
  	{
		// BCP file (open in append mode, write data only)
		BufferedWriter bcpWriter     = null;

		// Write header + data (can be used in Excel or simular)
		BufferedWriter txtWriter        = null;
		boolean        txtWriterNewFile = false;

		// Write SQL Table definition
		BufferedWriter tabWriter     = null;

		// SQL Time stamp when we saved this information
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd H:mm:ss.SSS");
		String sqlSaveTime = sdf.format(new Date());

		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
		String fileTime = sdf2.format(new Date());

		// no directory name was passed
		if (saveToDir == null)
		{
			saveToDir = System.getProperty("ASETUNE_SAVE_DIR");

			if (saveToDir == null)
			{
				saveToDir = System.getProperty("ASETUNE_HOME");

				if (saveToDir == null)
				{
					_logger.error("Directory name was not specified and ASETUNE_SAVE_DIR or ASETUNE_HOME was not set, can't save information about Captured Statements.");
					return;
				}
			}
		}
		
		// no BCP file was passed
		if (bcpFilename != null  && bcpFilename.length() == 0)
		{
			bcpFilename = saveToDir + "/capStmts."+fileTime+".bcp";
		}

		// no TXT file was passed
		if (txtFilename != null  && txtFilename.length() == 0)
		{
//			SimpleDateFormat txtSdf = new SimpleDateFormat("yyyy-MM-dd.H_mm_ss.SSS");
//			String time = txtSdf.format(new Date());
//			
//			txtFilename = saveToDir + "/capStmts."+time+".txt";
			txtFilename = saveToDir + "/capStmts."+fileTime+".txt";
		}

		// no TXT file was passed
		if (tabDefFilename != null  && tabDefFilename.length() == 0)
		{
			tabDefFilename = saveToDir + "/capStmts."+fileTime+".ddl.sql";
		}

		// Open all files
		try
		{
			// Open the BCP file in append mode.
			if (bcpFilename != null)
			{
				bcpWriter     = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(bcpFilename, true)));
			}

			// Open the TXT file in append mode.
			if (txtFilename != null)
			{
				File txtWriterFile = new File(txtFilename);

				if (txtWriterFile.exists())
					txtWriterNewFile = false;
				else
					txtWriterNewFile = true;
					
				txtWriter     = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(txtWriterFile, true)));
			}

			// Open The tabledef in write over
			if (tabDefFilename != null)
			{
				tabWriter     = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tabDefFilename)));
			}
		}
		catch (FileNotFoundException e)
		{
			_logger.warn("Problems opening/creating the a file. "+e);
			return;
		}
		

		//------------------------------
		// Write SQL table definition file
		//------------------------------
		if (tabWriter != null)
		{
			try
			{
				tabWriter.write("create table CapturedStatements \n");
				tabWriter.write("( \n");
				tabWriter.write("    SaveTime           datetime        not null,\n");
				tabWriter.write("    SPID               int             not null,\n");
				tabWriter.write("    KPID               int             not null,\n");
				tabWriter.write("    BatchID            int             not null,\n");
				tabWriter.write("    LineNumber         int             not null,\n");
				tabWriter.write("    dbname             varchar(30)     not null,\n");
				tabWriter.write("    procname           varchar(30)     not null,\n");
				tabWriter.write("    Elapsed_ms         int             not null,\n");
				tabWriter.write("    CpuTime            int             not null,\n");
				tabWriter.write("    WaitTime           int             not null,\n");
				tabWriter.write("    MemUsageKB         int             not null,\n");
				tabWriter.write("    PhysicalReads      int             not null,\n");
				tabWriter.write("    LogicalReads       int             not null,\n");
				if (_aseVersion >= 15002 || (_aseVersion >= 12540 && _aseVersion <= 15000) )
				{
				tabWriter.write("    RowsAffected       int             not null,\n");
				tabWriter.write("    ErrorStatus        int             not null,\n");
				}
				if (_aseVersion >= 15030 )
				{
				tabWriter.write("    ProcNestLevel      int             not null,\n");
				tabWriter.write("    StatementNumber    int             not null,\n");
				}
				tabWriter.write("    PagesModified      int             not null,\n");
				tabWriter.write("    PacketsSent        int             not null,\n");
				tabWriter.write("    PacketsReceived    int             not null,\n");
				tabWriter.write("    NetworkPacketSize  int             not null,\n");
				tabWriter.write("    PlansAltered       int             not null,\n");
				tabWriter.write("    StartTime          datetime        not null,\n");
				tabWriter.write("    EndTime            datetime        not null,\n");
				tabWriter.write("    PlanID             int             not null,\n");
				tabWriter.write("    DBID               int             not null,\n");
				tabWriter.write("    ProcedureID        int             not null,\n");
				tabWriter.write(") \n");
				tabWriter.write("go\n");
				tabWriter.write("\n");
				tabWriter.write("create index CapStmnts_ix1 on CapturedStatements(SaveTime)\n");
				tabWriter.write("go\n");
				tabWriter.write("\n");
				tabWriter.write("create index CapStmnts_ix2 on CapturedStatements(procname, LineNumber)\n");
				tabWriter.write("go\n");
			}
			catch (IOException e)
			{
				_logger.warn("Error writing to file.", e);
			}
		}

		//------------------------------
		// Write to the files
		//------------------------------
		String colSep = "\t";
		String rowSep = "\n";
		
		Object       colObj    = null;
		StringBuffer rowSb     = new StringBuffer();
		StringBuffer headerSb  = new StringBuffer();

		int rows = capturedStmtModel.getRowCount();
		int cols = capturedStmtModel.getColumnCount();

		// Loop all headers, to get name(s)
		headerSb.append("SaveTime");
		headerSb.append(colSep);
		for (int c=0; c<cols; c++)
		{
			String colName = capturedStmtModel.getColumnName(c);
			if (colName != null)
				headerSb.append(colName);
			else
				headerSb.append("");

			headerSb.append(colSep);
		}

		// Loop all rows
		for (int r=0; r<rows; r++)
		{
			// Compose 1 row 
			rowSb.setLength(0);

			// Add sqlSaveTime as the first column
			rowSb.append(sqlSaveTime);
			rowSb.append(colSep);

			// loop all columns
			for (int c=0; c<cols; c++)
			{
				colObj = capturedStmtModel.getValueAt(r, c);
				if (colObj != null)
					rowSb.append(colObj);
				else
					rowSb.append("");
					
				rowSb.append(colSep);
			}
			
			// Write that row
			if (rowSb.length() > 0)
			{
				try
				{
					//--------------------
					// BCP FILE
					//--------------------
					if (bcpWriter != null)
					{
						bcpWriter.write(rowSb.toString());
						bcpWriter.newLine();
					}

					//--------------------
					// TEXT FILE
					//--------------------
					// Write header before first row, if the file is NEW
					if (txtWriter != null)
					{
						//if (txtWriterNewFile && r == 0)
						if (r == 0)
						{
							txtWriter.write(headerSb.toString());
							txtWriter.newLine();
						}
						txtWriter.write(rowSb.toString());
						txtWriter.newLine();
					}
				}
				catch (IOException e)
				{
					_logger.warn("Error writing to file.", e);
				}
			}
		} // end: loop rows

		// Close the files
		try
		{
			if (bcpWriter != null) bcpWriter.close();
			if (txtWriter != null) txtWriter.close();
			if (tabWriter != null) tabWriter.close();
		}
		catch (IOException e)
		{
			_logger.warn("Error closing file.", e);
		}

  	} // end: method
	

	public void createCounters()
	{
//		String   cols1 = null;
//		String   cols2 = null;
//		String   cols3 = null;

//		String   name;
//		String   displayName;
//		String   description;
		int      needVersion   = 0;
		int      needCeVersion = 0;
		String[] monTables;
		String[] needRole;
		String[] needConfig;
		String[] colsCalcDiff;
		String[] colsCalcPCT;
//		List     pkList;

		String groupName = "RefreshProcess";

		//------------------------------------
		//------------------------------------
		// Objects
		//------------------------------------
		//------------------------------------
//		name         = "CMProcObjects";
//		displayName  = "Objects";
//		description  = "What objects are accessed right now.";

		needVersion  = 0;
		monTables    = new String[] { "monProcessObject" };
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring", "per object statistics active"};
		colsCalcDiff = new String[] { "LogicalReads", "PhysicalReads", "PhysicalAPFReads" };
		colsCalcPCT  = new String[] {};
//		pkList       = new LinkedList();
//		     pkList.add("KPID");
//		     pkList.add("DBName");
//		     pkList.add("ObjectID");
//		     pkList.add("IndexID");
//		     pkList.add("OwnerUserID");
//
//		cols1 = cols2 = cols3 = "";
//		cols1 = "SPID, KPID, DBName, ObjectID, OwnerUserID, ObjectName, IndexID, ObjectType, ";
//		cols2 = "LogicalReads, PhysicalReads, PhysicalAPFReads, dupMergeCount=convert(int,0)";
//		cols3 = "";
//		if (_aseVersion >= 12520)
//		{
//			cols3 = ", TableSize";
//		}
//		if (_aseVersion >= 15000)
//		{
//			cols1 += "PartitionID, PartitionName, "; // new cols in 15.0.0
//			cols3 = ", PartitionSize";  // TableSize has changed name to PartitionSize
//
//			pkList.add("PartitionID");
//		}
//
//		String CMProcObjectsSqlBase = 
//			"select " + cols1 + cols2 + cols3 + "\n" +
//		    "from monProcessObject \n" +
//		    "where 1=1 ";

		CMProcObjects = new CountersModel(
				"CMProcObjects", groupName, null, null, 
				colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, true, true)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getSqlForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				String cols1 = "";
				String cols2 = "";
				String cols3 = "";
				cols1 = "SPID, KPID, DBName, ObjectID, OwnerUserID, ObjectName, IndexID, ObjectType, \n";
				cols2 = "LogicalReads, PhysicalReads, PhysicalAPFReads, dupMergeCount=convert(int,0) \n";
				cols3 = "";
				if (srvVersion >= 12520)
				{
					cols3 = ", TableSize";
				}
				if (srvVersion >= 15000)
				{
					cols1 += "PartitionID, PartitionName, "; // new cols in 15.0.0
					cols3 = ", PartitionSize";  // TableSize has changed name to PartitionSize
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monProcessObject \n" +
					"where 1=1 ";

				return sql;
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

//				if (isClusterEnabled)
//					pkCols.add("InstanceID");

				pkCols.add("KPID");
				pkCols.add("DBName");
				pkCols.add("ObjectID");
				pkCols.add("IndexID");
				pkCols.add("OwnerUserID");

				if (srvVersion >= 15000)
					pkCols.add("PartitionID");

				return pkCols;
			}

			@Override
			public boolean isRefreshable()
			{
				boolean refresh = false;

				// Current TAB is visible
				if ( equalsTabPanel(pdf.getActiveTab()) )
					refresh = true;
		
				// Current TAB is un-docked (in it's own window)
				if (getTabPanel() != null)
				{
					JTabbedPane tp = pdf.getTabbedPane();
					if (tp instanceof GTabbedPane)
					{
						GTabbedPane gtp = (GTabbedPane) tp;
						if (gtp.isTabUnDocked(getTabPanel().getPanelName()))
							refresh = true;
					}
				}

				// Background poll is checked
				if ( isBackgroundDataPollingEnabled() )
					refresh = true;

				// NO-REFRESH if data polling is PAUSED
				if ( isDataPollingPaused() )
					refresh = false;

				// Check postpone
				if ( getTimeToNextPostponedRefresh() > 0 )
				{
					_logger.debug("Next refresh for the cm '"+getName()+"' will have to wait '"+TimeUtils.msToTimeStr(getTimeToNextPostponedRefresh())+"'.");
					refresh = false;
				}

				return refresh;
			}
		};

		CMProcObjects.setTabPanel(pdf.processObjectsPanel);
		
		if (kpid > 0)
			CMProcObjects.setSqlWhere(" and KPID = " + kpid);

		
		//------------------------------------
		//------------------------------------
		// Waits
		//------------------------------------
		//------------------------------------
//		name         = "CMProcWaits";
//		displayName  = "Waits";
//		description  = "What is this spid waiting for right now.";

		needVersion  = 0;
		monTables    = new String[] { "monProcessWaits", "monWaitEventInfo", "monWaitClassInfo" };
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring", "process wait events", "wait event timing"};
		colsCalcDiff = new String[] { "WaitTime", "Waits" };
		colsCalcPCT  = new String[] {};
//		pkList       = new LinkedList();
//		     pkList.add("KPID");
//		     pkList.add("WaitEventID");
//
//		String   CMProcWaitsSqlBase = 
//			"select SPID, KPID, Class=C.Description, Event=I.Description, W.WaitEventID, WaitTime, Waits " + 
//		    "from monProcessWaits W, monWaitEventInfo I, monWaitClassInfo C " + 
//		    "where W.WaitEventID=I.WaitEventID " + 
//		    "  and I.WaitClassID=C.WaitClassID ";
//		CMProcWaits = new CountersModel(
//				"CMProcWaits", 
//				CMProcWaitsSqlBase, 
//				pkList,	colsCalcDiff, colsCalcPCT, 
//				monTables, needRole, needConfig, needVersion, needCeVersion, true, true)
//		{
		CMProcWaits = new CountersModel(
				"CMProcWaits", groupName, null, null,
				colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, true, true)
		{
			private static final long	serialVersionUID	= 1L;

			@Override
			protected CmSybMessageHandler createSybMessageHandler()
			{
				CmSybMessageHandler msgHandler = super.createSybMessageHandler();
	
				// If ASE is above 15.0.3 esd#1, and dbcc traceon(3604) is given && 'capture missing stats' is 
				// on the 'CMsysWaitActivity' CM will throw an warning which should NOT be throws...
				//if (getServerVersion() >= 15031) // NOTE this is done early in initialization, so getServerVersion() can't be used
				msgHandler.addDiscardMsgStr("WaitClassID, WaitEventID");
	
				return msgHandler;
			}

			@Override
			public String getSqlForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				String sql = 
					"select SPID, KPID, Class=C.Description, Event=I.Description, W.WaitEventID, WaitTime, Waits \n" + 
					"from monProcessWaits W, monWaitEventInfo I, monWaitClassInfo C \n" + 
					"where W.WaitEventID=I.WaitEventID \n" + 
					"  and I.WaitClassID=C.WaitClassID \n";
				return sql;
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

//				if (isClusterEnabled)
//					pkCols.add("InstanceID");

				pkCols.add("KPID");
				pkCols.add("WaitEventID");

				return pkCols;
			}

			@Override
			public boolean isRefreshable()
			{
				boolean refresh = false;

				// Current TAB is visible
				if ( equalsTabPanel(pdf.getActiveTab()) )
					refresh = true;
		
				// Current TAB is un-docked (in it's own window)
				if (getTabPanel() != null)
				{
					JTabbedPane tp = pdf.getTabbedPane();
					if (tp instanceof GTabbedPane)
					{
						GTabbedPane gtp = (GTabbedPane) tp;
						if (gtp.isTabUnDocked(getTabPanel().getPanelName()))
							refresh = true;
					}
				}

				// Background poll is checked
				if ( isBackgroundDataPollingEnabled() )
					refresh = true;

				// NO-REFRESH if data polling is PAUSED
				if ( isDataPollingPaused() )
					refresh = false;

				// Check postpone
				if ( getTimeToNextPostponedRefresh() > 0 )
				{
					_logger.debug("Next refresh for the cm '"+getName()+"' will have to wait '"+TimeUtils.msToTimeStr(getTimeToNextPostponedRefresh())+"'.");
					refresh = false;
				}

				return refresh;
			}
		};


		CMProcWaits.setTabPanel(pdf.processWaitsPanel);

		if (kpid > 0)
			CMProcWaits.setSqlWhere(" and KPID = " + kpid);


		//------------------------------------
		//------------------------------------
		// Locks
		//------------------------------------
		//------------------------------------
		/*
		** SPID        KPID        DBID        ParentSPID  LockID      Context     ObjectID    LockState            LockType             LockLevel                      WaitTime    PageNumber  RowNumber   
		** ----------- ----------- ----------- ----------- ----------- ----------- ----------- ---------            --------             ---------                      ----------- ----------- ----------- 
		**         221  1222509855          17           0         442           8   757577737 Granted              shared page          PAGE                                  NULL      854653        NULL 
		**         221  1222509855          17           0         442           8  1073438898 Granted              shared page          PAGE                                  NULL      479900        NULL 
		**         405   222298460           4           0         810           0  1768445424 Granted              shared intent        TABLE                                 NULL        NULL        NULL 
		**         405   222298460           4           0         810           0  1560444683 Granted              shared intent        TABLE                                 NULL        NULL        NULL 
		**         405   222298460           4           0         810           0  1592444797 Granted              shared intent        TABLE                                 NULL        NULL        NULL 
		**         405   222298460           4           0         810           0  1736445310 Granted              shared intent        TABLE                                 NULL        NULL        NULL 
		*/
//		name         = "CMProcLocks";
//		displayName  = "Locks";
//		description  = "What locks does this spid hold right now.";

		needVersion  = 0;
		monTables    = new String[] { "monLocks" };
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring"};
		colsCalcDiff = new String[] {};
		colsCalcPCT  = new String[] {};

//		//List pkList_CMLocks = new LinkedList();
//		pkList = null;

//		cols1 = cols2 = cols3 = "";
//		cols1 = "SPID, KPID, DBID, ParentSPID, LockID, Context, ObjectID, ObjectName=object_name(ObjectID, DBID), LockState, LockType, LockLevel, ";
//		cols2 = "";
//		cols3 = "WaitTime, PageNumber, RowNumber";
//		if (_aseVersion >= 15002)
//		{
//			cols2 = "BlockedState, BlockedBy, ";  //
//		}
//		if (_aseVersion >= 15020)
//		{
//			cols3 += ", SourceCodeID";  //
//		}
//		
//		String   CMLocksSqlBase =
//			"select " + cols1 + cols2 + cols3 + "\n" +
//			"from monLocks L " + 
//			"where 1=1 ";

		CMLocks = new CountersModel(
				"CMLocks", groupName, null, null, 
				colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, false, true)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getSqlForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				String cols1 = "";
				String cols2 = "";
				String cols3 = "";

				cols1 = "SPID, KPID, DBID, ParentSPID, LockID, Context, \n" +
						"ObjectID, ObjectName=object_name(ObjectID, DBID), \n" +
						"LockState, LockType, LockLevel, ";
				cols2 = "";
				cols3 = "WaitTime, PageNumber, RowNumber";
				if (srvVersion >= 15002)
				{
					cols2 = "BlockedState, BlockedBy, ";  //
				}
				if (srvVersion >= 15020)
				{
					cols3 += ", SourceCodeID";  //
				}
				
				String sql =
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monLocks L " + 
					"where 1=1 ";

				return sql;
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return null;
			}

			@Override
			public boolean isRefreshable()
			{
				boolean refresh = false;

				// Current TAB is visible
				if ( equalsTabPanel(pdf.getActiveTab()) )
					refresh = true;
		
				// Current TAB is un-docked (in it's own window)
				if (getTabPanel() != null)
				{
					JTabbedPane tp = pdf.getTabbedPane();
					if (tp instanceof GTabbedPane)
					{
						GTabbedPane gtp = (GTabbedPane) tp;
						if (gtp.isTabUnDocked(getTabPanel().getPanelName()))
							refresh = true;
					}
				}

				// Background poll is checked
				if ( isBackgroundDataPollingEnabled() )
					refresh = true;

				// NO-REFRESH if data polling is PAUSED
				if ( isDataPollingPaused() )
					refresh = false;

				// Check postpone
				if ( getTimeToNextPostponedRefresh() > 0 )
				{
					_logger.debug("Next refresh for the cm '"+getName()+"' will have to wait '"+TimeUtils.msToTimeStr(getTimeToNextPostponedRefresh())+"'.");
					refresh = false;
				}

				return refresh;
			}
		};

		CMLocks.setTabPanel(pdf.processLocksPanel);

		if (kpid > 0)
			CMLocks.setSqlWhere(" and KPID = " + kpid);
	}

	private List<String> _activeRoleList    = null;
	private boolean      _isInitialized     = false;
	private boolean      _countersIsCreated = false;

	public void initCounters(Connection conn, boolean hasGui, int aseVersion, boolean isClusterEnabled, int monTablesVersion)
	throws Exception
	{
		if (_isInitialized)
			return;

		if ( ! AseConnectionUtils.isConnectionOk(conn, hasGui, null))
			throw new Exception("Trying to initialize the counters with a connection this seems to be broken.");

			
		if (! _countersIsCreated)
			createCounters();
		
		_logger.info("Capture SQL; Initializing all CM objects, using ASE server version number "+aseVersion+", isClusterEnabled="+isClusterEnabled+" with monTables Install version "+monTablesVersion+".");

		// Get active ASE Roles
		//List<String> activeRoleList = AseConnectionUtils.getActiveRoles(conn);
		_activeRoleList = AseConnectionUtils.getActiveRoles(conn);
		
		// Get active Monitor Configuration
		Map<String,Integer> monitorConfigMap = AseConnectionUtils.getMonitorConfigs(conn);

		// in version 15.0.3.1 compatibility_mode was introduced, this to use 12.5.4 optimizer & exec engine
		// This will hurt performance, especially when querying sysmonitors table, so set this to off
		if (aseVersion >= 15031)
			AseConnectionUtils.setCompatibilityMode(conn, false);

		ArrayList<CountersModel> CMList = new ArrayList<CountersModel>();
		CMList.add(CMProcWaits);
		CMList.add(CMProcObjects);
		CMList.add(CMLocks);

		// initialize all the CM's
		for (CountersModel cm : CMList)
		{
			if (cm != null)
			{
				_logger.debug("Initializing CM named '"+cm.getName()+"', display name '"+cm.getDisplayName()+"', using ASE server version number "+aseVersion+".");

				// set the version
				cm.setServerVersion(aseVersion);
				cm.setClusterEnabled(isClusterEnabled);
				
				// set the active roles, so it can be used in initSql()
				cm.setActiveRoles(_activeRoleList);

				// set the ASE Monitor Configuration, so it can be used in initSql() and elsewhere
				cm.setMonitorConfigs(monitorConfigMap);

				// Now when we are connected to a server, and properties are set in the CM, 
				// mark it as runtime initialized (or late initialization)
				// do NOT do this at the end of this method, because some of the above stuff
				// will be used by the below method calls.
				cm.setRuntimeInitialized(true);

				// Initializes SQL, use getServerVersion to check what we are connected to.
				cm.initSql(conn);

				// Use this method if we need to check anything in the database.
				// for example "deadlock pipe" may not be active...
				// If server version is below 15.0.2 statement cache info should not be VISABLE
				cm.init(conn);
			}
		}
			
		_isInitialized = true;
	}

	
	public void run()
	{
		// Check configured options
		try
		{
			batchCapture    = getAseConfigBoolean("SQL batch capture");
			SQLTextMonitor  = getAseConfigBoolean("max SQL text monitored");
			planTextPipe    = getAseConfigBoolean("plan text pipe active");
			planTextPipeMsg = getAseConfigBoolean("plan text pipe max messages");
			sqlTextPipe     = getAseConfigBoolean("sql text pipe active");
			sqlTextPipeMsg  = getAseConfigBoolean("sql text pipe max messages");
			stmtPipe        = getAseConfigBoolean("statement pipe active");
			stmtPipeMsg     = getAseConfigBoolean("statement pipe max messages");
			stmtStat        = getAseConfigBoolean("statement statistics active");
			procWaitEvents  = getAseConfigBoolean("process wait events");


			// Check if user has sa_role
			ResultSet rs = stmt.executeQuery("sp_activeroles");
			while (rs.next())
			{
				if (rs.getString(1).equals("sa_role"))
					has_sa_role = true;
			}

		}
		catch (SQLException SQLEx)
		{
			_logger.error(Version.getAppName()+" : error in refreshProcess getting options. ", SQLEx);
			// SQLEx.printStackTrace();
		}

		// Display warning if all configuration parameters are not set
		StringBuffer              msg = new StringBuffer();
		if (!stmtStat)            msg = msg.append("       'statement statistics active' to 1\n" );
		if (!batchCapture)        msg = msg.append("       'SQL batch capture' to 1 \n");
		if (!SQLTextMonitor)      msg = msg.append("       'max SQL text monitored' to a value greater than 0 (ex : 4096)\n");
		if (sqlTextSample)
		{
			if (!sqlTextPipe)     msg = msg.append("       'sql text pipe active' to 1\n");
			if (!sqlTextPipeMsg)  msg = msg.append("       'sql text pipe max messages' to a value greater than 0\n");
		}
		if (!stmtPipe)            msg = msg.append("       'statement pipe active' to 1\n");
		if (!stmtPipeMsg)         msg = msg.append("       'statement pipe max messages' to a value greater than 0\n");
		if (planTextSample)
		{
			if (!planTextPipe)    msg = msg.append("       'plan text pipe active' to 1\n");
			if (!planTextPipeMsg) msg = msg.append("       'plan text pipe max messages' to a value greater than 0\n");
		}
		if (!procWaitEvents)      msg = msg.append("       'process wait events' to 1\n");
		if (!has_sa_role) 
		{
			if (msg.length()>0) msg = msg.append(" and ");
				msg = msg.append("user should have 'sa_role'");
		}

		if (msg.length() > 0)
		{
			msg.insert(0, "RECOMMENDATION : for full features, configure the following parameters :\n\n");
			msg = msg.append("\n\nConfigure the ASE and re-open the window.\n");

			// MessageDialog msgDlg = new MessageDialog(pdf, "Warning",
			// msg.toString());
			// new MessageDialog(pdf, "Warning", msg.toString());
			JOptionPane.showMessageDialog(pdf, msg.toString(), Version.getAppName()+" - SPID monitoring", JOptionPane.WARNING_MESSAGE);

			AseConfigMonitoringDialog.showDialog(pdf, _conn, -1);
		}

		// Check if "sp_showplanfull" exists
		if (has_sa_role) 
		{
			try 
			{
				rs=stmt.executeQuery("select name from sybsystemprocs..sysobjects where name ='sp_showplanfull' and type='P'");
				rs.next();
				if (rs.getRow() == 0) 
				{
					// Proc doesn't exist, create it
					stmt.executeUpdate("use sybsystemprocs");
					stmt.executeUpdate(
						"create proc sp_showplanfull    \n" +
						"(   \n" +
						"    @spid int   \n" +
						")   \n" +
						"as   \n" +
						"declare   \n" +
						"     @batchid int,   \n" +
						"     @contextid int,   \n" +
						"     @stmtid int,   \n" +
						"     @return_value int,   \n" +
						"     @procname varchar(30),   \n" +
						"     @procid int,   \n" +
						"     @dbid int   \n" +
						"    \n" +
						"set nocount on   \n" +
						"    \n" +
						"select @return_value = show_plan(@spid, -1, -1, -1)   \n" +
						"if (@return_value < 0)   \n" +
						"begin   \n" +
						"        print 'No plan for spid %1!', @spid   \n" +
						"        goto fin   \n" +
						"end   \n" +
						"else   \n" +
						"begin   \n" +
						"        select @batchid = @return_value   \n" +
						"end   \n" +
						"    \n" +
						"select @return_value = show_plan(@spid, @batchid, -1, -1)   \n" +
						"if (@return_value < 0)   \n" +
						"begin   \n" +
						"        print 'No plan for spid %1!', @spid   \n" +
						"        goto fin   \n" +
						"end   \n" +
						"else   \n" +
						"begin   \n" +
						"        select @contextid = @return_value   \n" +
						"end   \n" +
						"    \n" +
						"select @procname=object_name(id, dbid), @procid=id, @dbid=dbid from master..sysprocesses where spid=@spid   \n" +
						"if @procid>0    \n" +
						"begin   \n" +
						"       print 'Plan for procedure : %1! (id=%2!, dbid=%3!)', @procname, @procid, @dbid   \n" +
						"end   \n" +
						"    \n" +
						"select @stmtid =1   \n" +
						"while (1=1)   \n" +
						"begin   \n" +
						"      select @return_value = show_plan(@spid, @batchid, @contextid , @stmtid)   \n" +
						"      if (@return_value < 0)   \n" +
						"          break   \n" +
						"      else   \n" +
						"          select @stmtid =@stmtid +1   \n" +
						"end   \n" +
						"fin:   \n"  );

					stmt.executeUpdate("use master");
				}
			}
			catch (SQLException sqlex)
			{
				_logger.error("When creating stored proc 'sp_showplanfull'.", sqlex);
			}
		}

		_aseVersion       = MonTablesDictionary.getInstance().aseVersionNum;
		_isClusterEnabled = MonTablesDictionary.getInstance().isClusterEnabled;
		_monTablesVersion = MonTablesDictionary.getInstance().montablesVersionNum;

		try
		{
			initCounters(_conn, true, _aseVersion, _isClusterEnabled, _monTablesVersion);
		}
		catch (Exception ex)
		{
			SwingUtils.showErrorMessage("Problems Initalize CM's", 
					"Problems when initializing some of the Performance Counters", ex);
			return;
		}
		

		// loop
		boolean firstTime = true;
		while (refreshProcessFlag)
		{
			pdf.updateGuiStatus();
			try
			{
				if (firstTime == false && ! _paused)
					Thread.sleep(_refreshInterval * 1000);

				if (_paused)
					Thread.sleep(500);
			}
			catch (InterruptedException ignore) {}

			if (_paused)
			{
				pdf.statusBarLbl.setText("Paused");
				continue;
			}

			firstTime = false;

			refreshProcess();
		}

		try
		{
			_conn.close();
		}
		catch (SQLException sqlex)
		{
			_logger.error("SQL problems", sqlex);
		}
		pdf.statusBarLbl.setText("Process disconnected");
	}

	public void setRefreshInterval(int interval)
	{
		_refreshInterval = interval;
	}

	public boolean isPaused()
	{
		return _paused;
	}

	public void setPauseProcess(boolean pause)
	{
		_paused = pause;
	}

//	public void pauseProcess()
//	{
//		_paused = true;
//	}
//
//	public void resumeProcess()
//	{
//		_paused = false;
//	}

	public synchronized void refreshProcess()
	{
		try
		{
			// Refresh statement panel
			pdf.statusBarLbl.setText("Refreshing");
			pdf.statusBarLbl.setForeground(Color.BLACK);
			refreshStmt();

			if (true)           CMProcObjects.getTabPanel().setWatermark();
			if (procWaitEvents) CMProcWaits  .getTabPanel().setWatermark();
			if (true)           CMLocks      .getTabPanel().setWatermark();

			if (kpid > 0)
			{
				// Refresh process objects
				if (CMProcObjects.isRefreshable())
					CMProcObjects.refresh();
	
				// Refresh process waits
				if (procWaitEvents && CMProcWaits.isRefreshable())
					CMProcWaits.refresh();
	
				// Refresh locks
				if (CMLocks.isRefreshable())
					CMLocks.refresh();
			}
			else
			{
				if (currentBatch != null)
				{
					pdf.kpidFld.setText(currentBatch.kpid+"");
					pdf.kpidFld.setForeground(Color.BLUE);

					pdf.spidFld.setText(currentBatch.spid+"");
					pdf.spidFld.setForeground(Color.BLUE);

					if (true)           CMProcObjects.setSqlWhere(" and KPID = " + currentBatch.kpid);
					if (procWaitEvents) CMProcWaits  .setSqlWhere(" and KPID = " + currentBatch.kpid);
					if (true)           CMLocks      .setSqlWhere(" and KPID = " + currentBatch.kpid);

					if (                  CMProcObjects.isRefreshable()) CMProcObjects.refresh();
					if (procWaitEvents && CMProcWaits.isRefreshable()  ) CMProcWaits  .refresh();
					if (                  CMLocks.isRefreshable()      ) CMLocks      .refresh();
				}
				else
				{
					if (true)           CMProcObjects.clear();
					if (procWaitEvents) CMProcWaits  .clear();
					if (true)           CMLocks      .clear();

					String watermark = "No SQL Statement is executing, the \"Current Statements\" table is empty.";
					if (true)           CMProcObjects.getTabPanel().setWatermarkText(watermark);
					if (procWaitEvents) CMProcWaits  .getTabPanel().setWatermarkText(watermark);
					if (true)           CMLocks      .getTabPanel().setWatermarkText(watermark);
					
					pdf.kpidFld.setText("none");
					pdf.kpidFld.setForeground(Color.BLACK);

					pdf.spidFld.setText("none");
					pdf.spidFld.setForeground(Color.BLACK);
				}
			}
		}
		catch (Exception e)
		{
			_logger.error(Version.getAppName()+" : error in refreshProcess loop. ", e);
			//e.printStackTrace();
		}
		finally
		{
			if ( ! pdf.statusBarLbl.getText().startsWith("Error") )
			{
				pdf.statusBarLbl.setText("");
			}
		}
	}

	public void stopRefresh()
	{
		refreshProcessFlag = false;
	}

	class StatementsModel
	    extends DefaultTableModel
	{
		private static final long	serialVersionUID	= -2280839765834154391L;

		public StatementsModel()
		{
			super();
		}

		public StatementsModel(int row, int col)
		{
			super(row, col);
		}

		public boolean isCellEditable(int row, int col)
		{
			return false;
		}
		
		public void clear()
		{
			int r = getRowCount();
			if (r > 0)
			{
				for (; r>0; r--)
				{
					removeRow( r-1 );
				}
				//setRowCount(0);
			}
		}
	}
}

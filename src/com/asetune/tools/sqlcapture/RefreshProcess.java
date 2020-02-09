/**
*/


package com.asetune.tools.sqlcapture;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.apache.log4j.Logger;

import com.asetune.DbxTune;
import com.asetune.Version;
import com.asetune.cache.XmlPlanCache;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.config.ui.AseConfigMonitoringDialog;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.TimeUtils;
import com.asetune.utils.Ver;


public class RefreshProcess extends Thread 
{
	private static Logger _logger          = Logger.getLogger(RefreshProcess.class);

	private static final String  PROPKEY_showDialogMdaConfig = "CommandHistory.showDialogMdaConfig";
	private static final boolean DEFAULT_showDialogMdaConfig = true;

	private ProcessDetailFrame pdf;
//	private Connection	       _conn;
	private DbxConnection	   _conn;
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
	
	private boolean            _discardAseTuneAppName= true;

	private Statement	       stmt;
	private int	               _kpid;
	private int	               _spid;
	private int	               _activeSpid	         = -1;
	private int	               _activeKpid	         = -1;
	private int	               _activeBatchID	     = -1;
	private int	               _activeDBID	         = -1;
	private int	               _activeObjOwnerID     = -1;
	private int	               _activeProcedureID	 = -1;
	private int	               _activePlanID	     = -1;
	private int	               _activeSqlLine        = -1;
	private String             _activeDbname         = "";
	private String             _activeProcName       = "";

	// rs.getXXX(pos_xxx), use the below variables when positioning in the resultset
	private int pos_activeSpid        = -1;
	private int pos_activeKpid        = -1;
	private int pos_activeBatchID     = -1;
	private int pos_activeSqlLine     = -1;
	private int pos_activeDbname      = -1;
	private int pos_activeProcName    = -1;
	private int pos_activePlanID      = -1;
	private int pos_activeDBID        = -1;
	private int pos_activeObjOwnerID  = -1;
	private int pos_activeProcedureID = -1;
	private int pos_activeWaitEventID = -1;
	private int pos_activeWaitEventDesc = -1;

	// rs.getXXX(pos_xxx), use the below variables when positioning in the resultset
	private int pos_historySpid        = -1;
	private int pos_historyKpid        = -1;
	private int pos_historyBatchID     = -1;
	private int pos_historySqlLine     = -1;
	private int pos_historyDbname      = -1;
	private int pos_historyObjOwnerID  = -1;
	private int pos_historyProcName    = -1;
	private int pos_historyPlanID      = -1;
	private int pos_historyDBID        = -1;
	private int pos_historyProcedureID = -1;
	
	private Vector	           _activeStmtColNames	 = null;
	private StatementsModel	   _activeStmtModel;
	private boolean	           _activeSMInitialized	 = false;
	private Vector	           _activeStmt	         = null;
//	private Vector	           _activeStmtRow	     = null;
	private Vector	           _activeStmtRows	     = null; // multiple currentStmtRow objects
	private Batch	           _activeBatch          = null;

	private Vector	           _historyStmtColNames	 = null;
	private StatementsModel	   _historyStmtModel;
	private boolean	           _historySMInitialized = false;
	private boolean            _historyTableColmnsIsResized = false;


	private Vector	           _newHistoryStatements = null;

	private int	               _selectedStatement;

	private HashMap<String, Batch>  _batchHistory         = new HashMap<String, Batch>();
//	private HashMap	                _plansHistory         = new HashMap();
//	private HashMap	                _compiledPlansHistory = new HashMap();
	private HashMap<String, String>	_procedureTextCache   = new HashMap<String, String>();

	public CountersModel	   _cmProcObjects;
	public CountersModel	   _cmProcWaits;
	public CountersModel	   _cmLocks;

	private long    _srvVersion       = 0;
	private boolean _isClusterEnabled = false;
	private long    _monTablesVersion = 0;
	 

	private String  _historyRestrictionSql = "";
	private boolean _historyRestrictions   = false;

	private String _activeStatementsSql = "";
	private String _historyStatementsSql = "";

	public String getActiveStatementsSql()  { return _activeStatementsSql; }
	public String getHistoryStatementsSql() { return _historyStatementsSql; }
	
	public void setHistoryRestriction(String str)
	{
		_historyRestrictionSql = str;
		
		if (_historyRestrictionSql != null && _historyRestrictionSql.length() > 0)
			_historyRestrictions = true;
		else
			_historyRestrictions = false;

//		RuntimeException rte =  new RuntimeException("DEBUG exception to get from where this method was called from.");
//		_logger.debug("Setting CaptureRestrictingSql to '"+str+"'.", rte);
		_logger.debug("Setting HistoryRestrictingSql to '"+str+"'.");
	}
	

	public void setDiscardAseTuneAppname(boolean b)
	{
		_discardAseTuneAppName = b;
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
	

//	public RefreshProcess(ProcessDetailFrame aPdf, Connection conn, int _spid, int kpid) 
	public RefreshProcess(ProcessDetailFrame aPdf, DbxConnection conn, int spid, int kpid) 
	{
		this.pdf = aPdf;
		this._conn = conn;
		this._spid = spid;
		this._kpid = kpid;
		refreshProcessFlag=true;
		_selectedStatement=-1;
		try 
		{
			stmt = conn.createStatement();
			_activeStmt = new Vector();
			
			_activeStmtModel = new StatementsModel();
			pdf.activeStatementTable.setModel(_activeStmtModel);
			
			_historyStmtModel = new StatementsModel();
			pdf._historyStatementsTable.setModel(_historyStmtModel);
		}
		catch (Exception e) {}
	}

	private void addBatchHistory(Batch batch)
	{
		String id = batch.getKey();
		if (_logger.isDebugEnabled())
			_logger.debug("Adding SQL text for for id='"+id+"'. text="+batch.getSqlText(false));
		_batchHistory.put(id, batch);
	}

	private Batch getBatchHistory(String id)
	{
		Batch batch = _batchHistory.get(id);
		
		if (batch == null)
			_logger.debug("No SQL text was found for batch  id='"+id+"'.");
		
		return batch;
	}
	private Batch getBatchHistory(int spid, int kpid, int batchId)
	{
		String id = spid + ":" + kpid + ":" + batchId;
		Batch batch = _batchHistory.get(id);
		
		if (batch == null)
			_logger.debug("No SQL text was found for batch  id='"+id+"', spid='"+spid+"', kpid='"+kpid+"', batchId='"+batchId+"'.");
		
		return batch;
	}


	/** TODO: keep plans for procedures in a specific cache */ 
//	private void addPlanHistory(int spid, int kpid, int batchId, int dbId, int procId, Batch batch, StringBuilder planText)
//	{
//		if (planText == null)
//			throw new RuntimeException("Passed StringBuilder 'planText' cant be null.");
//
//		// Get rid of last newline
//		int len = planText.length() - 1;
//		if ( planText.charAt(len) == '\n')
//		{
//			planText.setCharAt(len, ' ');
//		}
//
//		String id = spid + ":" + kpid + ":" + batchId + ":" + procId;
//
//		Batch existingBatch = (Batch) _plansHistory.get(id);
//		if (existingBatch != null)
//		{
//			if (_logger.isDebugEnabled())
//				_logger.debug("Appending SHOWPLAN to spid='"+spid+"', kpid='"+kpid+"', batchId='"+batchId+"', procId='"+procId+"', hashtable.key='"+id+"'. text="+planText);
//			existingBatch.addShowplanTextLine(planText);
//		}
//		else
//		{
//			if (_logger.isDebugEnabled())
//				_logger.debug("Adding SHOWPLAN for spid='"+spid+"', kpid='"+kpid+"', batchId='"+batchId+"', procId='"+procId+"', hashtable.key='"+id+"'. text="+planText);
//			batch.addShowplanTextLine(planText);
//			_plansHistory.put(id, batch);
//		}
//	}
//	private void addPlanHistory(Batch batch)
//	{
//		String id = batch.spid + ":" + batch.kpid + ":" + batch.batchId + ":" + batch.procedureId;
//
//		Batch existingBatch = (Batch) _plansHistory.get(id);
//		if (existingBatch != null)
//		{
//			if (_logger.isDebugEnabled())
//				_logger.debug("Appending SHOWPLAN to spid='"+batch.spid+"', kpid='"+batch.kpid+"', batchId='"+batch.batchId+"', procId='"+batch.procedureId+"', hashtable.key='"+id+"'. text="+batch.getShowplanText());
//			existingBatch.appendShowplanText(batch.getShowplanText());
//		}
//		else
//		{
//			if (_logger.isDebugEnabled())
//				_logger.debug("Adding SHOWPLAN for spid='"+batch.spid+"', kpid='"+batch.kpid+"', batchId='"+batch.batchId+"', procId='"+batch.procedureId+"', hashtable.key='"+id+"'. text="+batch.getShowplanText());
//			//batch.addShowplanTextLine(planText);
//			_plansHistory.put(id, batch);
//		}
//		
//	}

	/** TODO: keep plans for procedures in a specific cache */ 
//	private Batch getPlanHistory(int spid, int kpid, int batchId, int dbId, int procId)
//	{
//		//String id = batchId + ":" + dbId + ":" + procId;
////		String id = batchId + ":" + procId;
//		String id = spid + ":" + kpid + ":" + batchId + ":" + procId;
//		Batch batch = (Batch) _plansHistory.get(id);
//		
//		if (batch == null)
//			_logger.debug("No SHOWPLAN text was found for spid='"+spid+"', kpid='"+kpid+"', batchId='"+batchId+"', procId='"+procId+"', hashtable.key='"+id+"'.");
//			//_logger.debug("No SHOWPLAN text was found for batchId='"+batchId+"', dbId='"+dbId+"', procId='"+procId+"', hashtable.key='"+id+"'.");
//		
//		return batch;
//	}

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
			if (_spid > 0 || _kpid > 0)
			{
				String whereSpidKpid = "";
				if (_spid > 0) whereSpidKpid += "  and P.spid=" + _spid + "\n";
				if (_kpid > 0) whereSpidKpid += "  and P.kpid=" + _kpid + "\n";
				
				// Refresh process information
				currentSql = 
					"select P.spid, P.enginenum, P.status, P.suid, suser_name(P.suid), P.hostname, P.hostprocess, P.cmd, P.cpu, \n" + 
					"   P.physical_io, P.memusage, LocksHeld, P.blocked, P.dbid, db_name(P.dbid), P.uid, '' /*user_name(P.uid)*/, P.gid, \n" + 
					"   P.tran_name, P.time_blocked, P.network_pktsz, P.fid, P.execlass, P.priority, P.affinity, P.id, isnull(object_name(P.id,P.dbid),object_name(P.id,2)), \n" + 
					"   P.stmtnum, P.linenum, P.origsuid, P.block_xloid, P.clientname, P.clienthostname,  P.clientapplname, P.sys_id, \n" + 
					"   P.ses_id, P.loggedindatetime, P.ipaddr,  program_name=convert(varchar,P.program_name), CPUTime, \n" + 
					"   WaitTime, LogicalReads, PhysicalReads, PagesRead, PhysicalWrites, PagesWritten, MemUsageKB, \n" + 
					"   ScanPgs =  TableAccesses,IdxPgs = IndexAccesses, TmpTbl = TempDbObjects, UlcBytWrite =  ULCBytesWritten, \n" + 
					"   UlcFlush = ULCFlushes, ULCFlushFull, Transactions, Commits, Rollbacks, PacketsSent, PacketsReceived, \n" + 
					"   BytesSent, BytesReceived \n" + 
					"from sysprocesses P , monProcessActivity A, monProcessNetIO N \n" + 
					"where P.kpid=A.KPID \n" +
					"  and N.KPID=P.kpid \n" +
					whereSpidKpid;
				ResultSet rs = stmt.executeQuery(currentSql);
	
				rs.next();
				if (rs.getRow() > 0)
				{
					_spid = ((Number) rs.getObject(1)).intValue();

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
					pdf.setStatusBar("Process disconnected", false);
					stopRefresh();
				}
			}

			//------------------------------------
			// refresh current statement
			// current statements will be stored in _activeStmtRows and aplied to the JTable later
			//------------------------------------
			_activeStmtRows              = null;
			boolean activeStmtHasRow     = false;
			boolean activeStmtHasChanged = false;
			if (stmtStat)
			{
				pdf.setWatermarkActiveTable("Refreshing...");

				/* 
				 * 12.5.4 version of monProcessStatement
				 * SPID        KPID        DBID        ProcedureID PlanID      BatchID     ContextID   LineNumber  CpuTime     WaitTime    MemUsageKB  PhysicalReads LogicalReads PagesModified PacketsSent PacketsReceived NetworkPacketSize PlansAltered RowsAffected StartTime                      
				 */
				String extraCols = "";
				if (_srvVersion >= Ver.ver(15,0,0,2) || (_srvVersion >= Ver.ver(12,5,4) && _srvVersion < Ver.ver(15,0)) )
				{
					extraCols = "  S.RowsAffected, \n";
				}

				String ObjOwnerID = "ObjOwnerID = convert(int, 0), ";
				if (_srvVersion >= Ver.ver(15,0,3) )
				{
					ObjOwnerID = "ObjOwnerID = object_owner_id(S.ProcedureID, S.DBID), ";
				}
				
				// ASE 16.0 SP3
				String QueryOptimizationTime       = "";
				String ase160_sp3_nl               = "";
				if (_srvVersion >= Ver.ver(16,0,0, 3)) // 16.0 SP3
				{
					QueryOptimizationTime       = "  S.QueryOptimizationTime, ";
					ase160_sp3_nl               = "\n";
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
					+ "  dbname=db_name(S.DBID), \n"
//					+ "  procname=isnull(isnull(object_name(S.ProcedureID,S.DBID),object_name(S.ProcedureID,2)),''), \n"
					+ "  procname=isnull(isnull(isnull(object_name(S.ProcedureID,S.DBID),object_name(S.ProcedureID,2)),object_name(S.ProcedureID,db_id('sybsystemprocs'))),''), \n"
					+ "  P.Command, S.CpuTime, S.WaitTime, \n"
					+ "  ExecTimeInMs = CASE WHEN datediff(day, S.StartTime, getdate()) >= 24 THEN -1 ELSE  datediff(ms, S.StartTime, getdate()) END, \n"  // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
					+ QueryOptimizationTime + ase160_sp3_nl
					+ "  S.MemUsageKB, S.PhysicalReads, S.LogicalReads, \n"
					+ extraCols
					+ "  P.Application, P.Login, \n"
					+ "  P.WaitEventID, WaitEventDesc = '', \n"
					+ "  P.SecondsWaiting, P.BlockingSPID, \n"
					+ "  S.PagesModified, S.PacketsSent, S.PacketsReceived, \n"
					+ "  S.NetworkPacketSize, S.PlansAltered, \n"
					+ "  S.StartTime, S.PlanID, S.DBID, " + ObjOwnerID + "S.ProcedureID, \n"
					+ "  P.SecondsConnected, P.EngineNumber, P.NumChildren \n" 
					+ "from monProcessStatement S, monProcess P \n"
					+ "where S.KPID = P.KPID\n";

				Configuration conf = Configuration.getCombinedConfiguration();
				String extraWhere = conf.getProperty(ProcessDetailFrame.PROP_ACTIVE_STATEMENT_EXTRA_WHERE);
				String orderBy    = conf.getProperty(ProcessDetailFrame.PROP_ACTIVE_STATEMENT_ORDER_BY);

				boolean hide_activeSqlWaitEventId250 = Configuration.getCombinedConfiguration().getBooleanProperty(ProcessDetailFrame.PROPKEY_hide_activeSqlWaitEventId250, ProcessDetailFrame.DEFAULT_hide_activeSqlWaitEventId250);
				

				if (StringUtil.isNullOrBlank(extraWhere))
					extraWhere = " 1 = 1 -- where clause from the 'Where: ' field goes here";
				if (StringUtil.isNullOrBlank(orderBy))
					orderBy = "S.LogicalReads desc \n";

				if (_spid > 0 || _kpid > 0)
				{
					if (_spid > 0) sql += "  and S.SPID = " + _spid + "\n";
					if (_kpid > 0) sql += "  and S.KPID = " + _kpid + "\n";
				}
				else
				{
					sql += "  and S.SPID != @@spid \n";
					if (_discardAseTuneAppName)
						sql += "  and P.Application not like '" + ProcessDetailFrame.DISCARD_APP_NAME + "%' \n";
					
					if (hide_activeSqlWaitEventId250)
						sql += "  and not (P.WaitEventID = 250 and S.WaitTime > 60000) -- get rid of incorrect rows with 'AWAITING COMMAND'... CR###### \n";
				}
				sql += "  and " + extraWhere + " \n";
				sql += "order by " + orderBy;

				currentSql = sql;
				_activeStatementsSql = sql;
				ResultSet rs = stmt.executeQuery(sql);
				ResultSetMetaData rsmdCurStmt = rs.getMetaData();
				int nbColsCurStmt = rsmdCurStmt.getColumnCount();
				if (_activeStmtColNames == null)
				{
					_activeStmtColNames = new Vector();
					for (int i = 1; i <= nbColsCurStmt; i++)
					{
						String colName = rsmdCurStmt.getColumnName(i);
						_activeStmtColNames.add(colName);

						if (colName.equals("SPID")       ) pos_activeSpid        = i;
						if (colName.equals("KPID")       ) pos_activeKpid        = i;
						if (colName.equals("BatchID")    ) pos_activeBatchID     = i;
						if (colName.equals("LineNumber") ) pos_activeSqlLine     = i;
						if (colName.equals("dbname")     ) pos_activeDbname      = i;
						if (colName.equals("procname")   ) pos_activeProcName    = i;
						if (colName.equals("PlanID")     ) pos_activePlanID      = i;
						if (colName.equals("DBID")       ) pos_activeDBID        = i;
						if (colName.equals("ObjOwnerID") ) pos_activeObjOwnerID  = i;
						if (colName.equals("ProcedureID")) pos_activeProcedureID = i;
						
						if (colName.equals("WaitEventDesc")) pos_activeWaitEventDesc = i;
						if (colName.equals("WaitEventID")) pos_activeWaitEventID = i;
					}
				}

				_activeSpid        = -1;
				_activeKpid        = -1;
				_activeBatchID     = -1;
				_activeSqlLine     = -1;
				_activeDbname      = null;
				_activeProcName    = null;
				_activePlanID      = -1;
				_activeDBID        = -1;
				_activeObjOwnerID  = -1;
				_activeProcedureID = -1;
				
				MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

				boolean firstRow = true;
				while (rs.next())
				{
					Vector row = new Vector();
					for (int i = 1; i <= nbColsCurStmt; i++)
					{
						Object o = rs.getObject(i);
						
						if (i == pos_activeWaitEventDesc && mtd != null)
						{
							int witEventId = rs.getInt(pos_activeWaitEventID);
							o = mtd.getWaitEventDescription(witEventId);
						}
						row.add(o);
					}
					if (_activeStmtRows == null)
					{
						_activeStmtRows = new Vector();
					}
					_activeStmtRows.add(row);

					// old code - for only 1 row
					//currentStmtRow = new Vector();
					//for (int i = 1; i <= nbColsCurStmt; i++)
					//{
					//	currentStmtRow.add(rs.getObject(i));
					//}

					if (firstRow)
					{
						activeStmtHasRow  = true;
						firstRow           = false;
						_activeSpid        = rs.getInt(pos_activeSpid);
						_activeKpid        = rs.getInt(pos_activeKpid);
						_activeBatchID     = rs.getInt(pos_activeBatchID);
						_activeSqlLine     = rs.getInt(pos_activeSqlLine);
						_activeDbname      = rs.getString(pos_activeDbname).trim();
						_activeProcName    = rs.getString(pos_activeProcName).trim();
						_activePlanID      = rs.getInt(pos_activePlanID);
						_activeDBID        = rs.getInt(pos_activeDBID);
						_activeObjOwnerID  = rs.getInt(pos_activeObjOwnerID);
						_activeProcedureID = rs.getInt(pos_activeProcedureID);

						if (_activeBatch == null)
						{
							_activeBatch = new Batch();
						}

						// If the another statement
						if (    _activeBatch.spid    != _activeSpid 
						     && _activeBatch.kpid    != _activeKpid 
						     && _activeBatch.batchId != _activeBatchID
						   )
						{
							activeStmtHasChanged      = true;
							if (_logger.isDebugEnabled())
							{
								_logger.debug("currentStmtHasChanged: _activeBatch.spid("+_activeBatch.spid+") != _activeSpid("+_activeSpid+")   &&   _activeBatch.kpid("+_activeBatch.kpid+") != _activeKpid("+_activeKpid+")   &&   _activeBatch.batchId("+_activeBatch.batchId+") != _activeBatchID("+_activeBatchID+").");
							}

							_activeBatch.spid          = _activeSpid;
							_activeBatch.kpid          = _activeKpid;
							_activeBatch.batchId       = _activeBatchID;
							_activeBatch.dbid          = _activeDBID;
							_activeBatch.dbname        = _activeDbname;
							_activeBatch.objectOwnerId = _activeObjOwnerID;
							_activeBatch.procedureId   = _activeProcedureID;
							_activeBatch.procedureName = _activeProcName;
							_activeBatch.planId        = _activePlanID;
						}

						// Always change these values, for first running statement
						_activeBatch.lineNumber    = _activeSqlLine;
					}
				}
				rs.close();
				// Refresh of the associated JTable will be done at the end, in
				// the swing thread

				pdf.setWatermarkActiveTable("");
			}
			else
			{
				// message that SQL Text isn't sampled
				String msg = "Refresh disabled due to:\n";
				if ( ! stmtStat ) msg += "sp_configure 'statement statistics active', 0\n";

				pdf.setWatermarkActiveTable(msg);
			}

			if ( ! activeStmtHasRow )
			{
				_activeBatch = null;
			}

			// If   NO rows from the History SQL is selected
			// and  current SQL is empty
			// SET the batch field to empty
			if (_selectedStatement == -1  && ! activeStmtHasRow)
				pdf.batchTextArea.setText("");


			
			//------------------------------------
			// Refresh the current SQL text
			// Do only get SQL text for the "first" rows fetched from Currently executed statements
			//------------------------------------
//			_logger.debug("SQLTextMonitor="+SQLTextMonitor+", batchCapture="+batchCapture+", currentStmtHasRow="+currentStmtHasRow+", currentStmtHasChanged="+currentStmtHasChanged);
			if (    SQLTextMonitor     // is enabled
			     && batchCapture       // is enabled
			     && activeStmtHasRow  // there ARE SQL currently running
			     && activeStmtHasChanged
			   )
			{
//				pdf.setWatermarkActiveTable("Refreshing...");

				// get this new batch SQL text
				String sql = 
					"select SPID, KPID, BatchID, LineNumber, SQLText \n" 
					+ "from monProcessSQLText \n" 
					+ "where KPID=" + _activeBatch.kpid + " \n"
					+ " and  SPID=" + _activeBatch.spid + " \n"
					+ " and  BatchID=" + _activeBatch.batchId + " \n" 
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
				ResultSet rs = stmt.executeQuery(sql);
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
				rs.close();
				// Add the composed string
				if ( ! sqlText.equals("") )
					_activeBatch.appendSqlText( sqlText );
					
//				int curLine = 0;
//				StringBuilder lineSb = null;
//				while (rs.next())
//				{
//					int lineNum = rs.getInt(4);
//					if (lineNum != curLine)
//					{
//						if (lineSb != null)
//							_activeBatch.addSqlTextLine(lineSb); // add previous lineSb to batch
//						curLine = lineNum;
//						lineSb = new StringBuilder();
//					}
//					lineSb.append(rs.getString(5));
//				}
//				if (lineSb != null)
//					_activeBatch.addSqlTextLine(lineSb); // add last lineSb to batch

				//------------------------------------
				// Get current plan using sp_showplanfull 
				// if user has sa_role
				//------------------------------------
				boolean sample_spShowplanfull        = Configuration.getCombinedConfiguration().getBooleanProperty(ProcessDetailFrame.PROPKEY_sample_spShowplanfull,        ProcessDetailFrame.DEFAULT_sample_spShowplanfull);
//System.out.println("has_sa_role='"+has_sa_role+"', doExecSpShowplanfull='"+doExecSpShowplanfull+"'.");

				if (has_sa_role && sample_spShowplanfull)
				{
	//				FIXME: get xml showplan for *ss#### or *sd####
					
					// if no showplan exists in _activeBatch
					// If the plan/object cant be found in compiledPlansCache
					//...
//					if ()
					_logger.debug("EXEC sp_showplanfull " + _activeSpid);

					String showplanSql = "sp_showplanfull " + _activeSpid; 
//System.out.println("EXEC: '"+showplanSql+"'.");
					currentSql = showplanSql;
					stmt.executeUpdate(showplanSql);

					StringBuilder planSb = null;
					SQLWarning sqlw = stmt.getWarnings();
					while (true)
					{
						// _logger.debug(sqlw.getErrorCode()+" " +
						// sqlw.getSQLState() +" "+sqlw.getMessage());
						if (planSb == null)
							planSb = new StringBuilder();

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
						_activeBatch.addShowplanTextLine(planSb);
					}

					// FIXME
					// Maybe add the PLAN (planId) to 
					// a compiledProcShowplan cache
					if (_activeBatch.procedureId > 0)
					{
						//_procedureTextCache
						String key = Integer.toString(_activeBatch.planId);
//						compiledPlansCache.put(key, _activeBatch);
					}
				}

				// save this batch in history
				addBatchHistory(_activeBatch);
				
//				pdf.setWatermarkActiveTable("");
			}
//			else
//			{
//				// message that SQL Text isn't sampled
//				String msg = "Refresh disabled due to:\n";
//				if ( ! SQLTextMonitor      ) msg += "sp_configure 'max SQL text monitored', 0\n";
//				if ( ! batchCapture        ) msg += "sp_configure 'SQL batch capture', 0\n";
//				if ( ! activeStmtHasRow    ) msg += "activeStmtHasRow = false\n";
//				if ( ! activeStmtHasChanged) msg += "activeStmtHasChanged = false\n";
//
//				pdf.setWatermarkActiveTable(msg);
//			}

			// Show this in the GUI, it might takes along time
			// to do the rest of the SQL below
			if (_selectedStatement == -1)
			{
				displayActiveBatch();
			}
			
			
			//------------------------------------
			// Get recent statements
			// store them in _newHistoryStatements, which will be moved over to the TableModel later
			//------------------------------------
			HashMap<String, String> restrictionsHistoricalKeys = new HashMap<String, String>(); 
			_newHistoryStatements = null;
			if (stmtPipe && stmtPipeMsg)
			{
				pdf.setWatermarkHistoryTable("Refreshing...");

//				 ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ 
//				 Adaptive Server Enterprise/15.0.3/EBF 17770 ESD#4/P/x86_64/Enterprise Linux/ase1503/2768/64-bit/FBO/Thu Aug 26 09:54:27 2010                                                                                             
//				 
//				 SPID   KPID        DBID        ProcedureID PlanID      BatchID     ContextID   LineNumber  CpuTime     WaitTime    MemUsageKB  PhysicalReads LogicalReads PagesModified PacketsSent PacketsReceived NetworkPacketSize PlansAltered RowsAffected ErrorStatus HashKey     SsqlId      ProcNestLevel StatementNumber DBName                                                       StartTime                  EndTime                    
//				 ------ ----------- ----------- ----------- ----------- ----------- ----------- ----------- ----------- ----------- ----------- ------------- ------------ ------------- ----------- --------------- ----------------- ------------ ------------ ----------- ----------- ----------- ------------- --------------- ------------------------------------------------------------ -------------------------- -------------------------- 

				 // NOTE: do not forget to change:  
				//       SQL table definition file
				//       in method: saveCapturedStatements

				String extraCols = "";
				if (_srvVersion >= Ver.ver(15,0,0,2) || (_srvVersion >= Ver.ver(12,5,4) && _srvVersion < Ver.ver(15,0)) )
				{
					extraCols = "       RowsAffected, ErrorStatus, \n";
				}
				if (_srvVersion >= Ver.ver(15,0,3) )
				{
					extraCols += "      ProcNestLevel, StatementNumber, \n";
				}

				String ObjOwnerID = "      ObjOwnerID = convert(int, 0), \n";
				if (_srvVersion >= Ver.ver(15,0,3) )
				{
					ObjOwnerID = "      ObjOwnerID = object_owner_id(ProcedureID, DBID), \n";
				}
				
				// ASE 16.0 SP3
				String QueryOptimizationTime       = "";
				String ase160_sp3_nl               = "";
				if (_srvVersion >= Ver.ver(16,0,0, 3)) // 16.0 SP3
				{
					QueryOptimizationTime       = "      QueryOptimizationTime, ";
					ase160_sp3_nl               = "\n";
				}
				
				
				String sql =
					"select SPID, KPID, BatchID, LineNumber, \n" +
					"       dbname=db_name(DBID), \n" +
//					"       procname=isnull(isnull(object_name(ProcedureID,DBID),object_name(ProcedureID,2)),''), \n" +
					"       procname=isnull(isnull(isnull(object_name(ProcedureID,DBID),object_name(ProcedureID,2)),object_name(ProcedureID,db_id('sybsystemprocs'))),''), \n" +
//					"       Elapsed_ms=datediff(ms,StartTime, EndTime), \n" +
					"       Elapsed_ms = CASE WHEN datediff(day, StartTime, EndTime) >= 24 THEN -1 ELSE  datediff(ms, StartTime, EndTime) END, \n" + // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
					"       CpuTime, WaitTime, MemUsageKB, PhysicalReads, LogicalReads, \n" +
					extraCols +
					QueryOptimizationTime + ase160_sp3_nl +
					"       PagesModified, PacketsSent, \n" +
					"       PacketsReceived, NetworkPacketSize, \n" +
					"       PlansAltered, StartTime, EndTime, \n" +
					"       PlanID, DBID, " + ObjOwnerID + "ProcedureID \n" +
					"from monSysStatement \n" +
					"where 1 = 1\n";

				
				if (_spid > 0 || _kpid > 0)
				{
					if (_spid > 0) sql += "  and SPID = " + _spid + "\n";
					if (_kpid > 0) sql += "  and KPID = " + _kpid + "\n";
				}
				else
				{
					sql += "  and SPID != @@spid \n";
					if (_discardAseTuneAppName)
						sql += "  and SPID not in (select spid from master.dbo.sysprocesses where program_name like '" + ProcessDetailFrame.DISCARD_APP_NAME + "%') \n";
				}

				if ( _historyRestrictions )
				{
					sql += "  and (" + _historyRestrictionSql + ")\n";
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
					ResultSet rs = stmt.executeQuery(currentSql);
					while(rs.next()) 
					{
						// DO nothing, we are "emptying" the table
					}
					rs.close();
					_logger.info("END:   Discarding everything in the transient monSysStatement table in the first sample. this took "+TimeUtils.msToTimeStr(System.currentTimeMillis()-startTime)+".");
				}

				currentSql = sql;
				_historyStatementsSql = sql;

				ResultSet rs = stmt.executeQuery(sql);
				ResultSetMetaData rsmd = rs.getMetaData();
				int nbCols = rsmd.getColumnCount();
				if (_historyStmtColNames == null)
				{
					_historyStmtColNames = new Vector();
					for (int i = 1; i <= nbCols; i++)
					{
						String colName = rsmd.getColumnName(i);
						_historyStmtColNames.add(colName);

						if (colName.equals("SPID")       ) pos_historySpid        = i;
						if (colName.equals("KPID")       ) pos_historyKpid        = i;
						if (colName.equals("BatchID")    ) pos_historyBatchID     = i;
						if (colName.equals("LineNumber") ) pos_historySqlLine     = i;
						if (colName.equals("dbname")     ) pos_historyDbname      = i;
						if (colName.equals("procname")   ) pos_historyProcName    = i;
						if (colName.equals("PlanID")     ) pos_historyPlanID      = i;
						if (colName.equals("DBID")       ) pos_historyDBID        = i;
						if (colName.equals("ObjOwnerID") ) pos_historyObjOwnerID  = i;
						if (colName.equals("ProcedureID")) pos_historyProcedureID = i;
					}
				}
				_newHistoryStatements = new Vector();
				while (rs.next())
				{
					// Add KEY elemets to a temporary array, just so we know WHAT
					// WHAT key elements we can KEEP later on
					// This is only valid if _historyRestrictions is set to something
					if (_historyRestrictions)
					{
						int lSpid    = rs.getInt(1);
						int lKpid    = rs.getInt(2);
						int lBatchId = rs.getInt(3);
						
						String key = lSpid + ":" + lKpid + ":" + lBatchId;
						restrictionsHistoricalKeys.put(key, "dummy");
					}

					// Add the FULL row to the Vector
					Vector row = new Vector();
					for (int i = 1; i <= nbCols; i++)
					{
						row.add(rs.getObject(i));
					}
					_newHistoryStatements.add(row);
				}
				rs.close();

				pdf.setWatermarkHistoryTable("");
			}
			else
			{
				// message that SQL Text isn't sampled
				String msg = "Refresh disabled due to:\n";
				if ( ! stmtPipe    ) msg += "sp_configure 'statement pipe active', 0\n";
				if ( ! stmtPipeMsg ) msg += "sp_configure 'statement pipe max messages', 0\n";

				pdf.setWatermarkHistoryTable(msg);
			}

			//------------------------------------
			// Get recent SQL TEXT into batch
			//------------------------------------
			if (    sqlTextSample        // is checkbox "ON"
				 && sqlTextPipe 
				 && sqlTextPipeMsg 
				 && SQLTextMonitor)
			{
				pdf.setWatermarkSqlText("Refreshing...");

				String sql =
					"select SPID, KPID, BatchID, SQLText \n" +
					"from monSysSQLText \n" +
					"where 1=1 \n";

				if (_spid > 0) sql += "  and SPID = " + _spid + "\n";
				if (_kpid > 0) sql += "  and KPID = " + _kpid + "\n";

				if ( _firstTimeSample && _discardPreOpenStmnts )
				{
//					// If first time... 
//					sql += "and SPID < 0\n";
//					_logger.info("Discarding everything in the transient monSysSQLText table in the first sample.");

					long startTime = System.currentTimeMillis();
					_logger.info("BEGIN: Discarding everything in the transient monSysSQLText table in the first sample.");

					currentSql = "select count(*) from monSysSQLText";
					ResultSet rs = stmt.executeQuery(currentSql);
					while(rs.next()) 
					{
						// DO nothing, we are "emptying" the table
					}
					rs.close();
					_logger.info("END:   Discarding everything in the transient monSysSQLText table in the first sample. this took "+TimeUtils.msToTimeStr(System.currentTimeMillis()-startTime)+".");
				}

				sql += " order by SPID, KPID, BatchID, SequenceInBatch \n";

				currentSql = sql;
				ResultSet rs = stmt.executeQuery(sql);
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
						// Post previously batches
						if (batch != null)
						{
							addBatchHistory(batch);
							batch = null;
						}
						
						// Can we skip this row?
						// If it's not part of the captured rows in previous section
						boolean addBatch = true;
						if (_historyRestrictions)
						{
							if ( restrictionsHistoricalKeys.get(lKey) == null)
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
				rs.close();
				// Add any "leftovers" that wasn't added in the loop above
				if (batch != null)
				{
					addBatchHistory(batch);
				}

				pdf.setWatermarkSqlText("");
			}
			else
			{
				// message that SQL Text isn't sampled
				String msg = "Refresh disabled due to:\n";
				if ( ! sqlTextPipe    ) msg += "sp_configure 'sql text pipe active', 0\n";
				if ( ! sqlTextPipeMsg ) msg += "sp_configure 'sql text pipe max messages', 0\n";
				if ( ! SQLTextMonitor ) msg += "sp_configure 'max SQL text monitored', 0\n";
				if ( ! sqlTextSample  ) msg += "Checkbox 'Sample SQL batch text' is OFF\n";

				pdf.setWatermarkSqlText(msg);
			}

			//------------------------------------
			// Get recent plans
			//------------------------------------
			if (planTextSample && planTextPipe && planTextPipeMsg)
			{
				pdf.setWatermarkPlanText("Refreshing...");

				String sql =
					"select SPID, KPID, BatchID, PlanID, PlanText, DBID, ProcedureID \n" + 
					"from monSysPlanText \n" +
					"where 1=1 \n";

				if (_spid > 0) sql += "  and SPID = " + _spid;
				if (_kpid > 0) sql += "  and KPID = " + _kpid;

				if ( _firstTimeSample && _discardPreOpenStmnts )
				{
//					// If first time... 
//					sql += "and SPID < 0\n";
//					_logger.info("Discarding everything in the transient monSysPlanText table in the first sample.");

					long startTime = System.currentTimeMillis();
					_logger.info("BEGIN: Discarding everything in the transient monSysPlanText table in the first sample.");

					ResultSet rs = stmt.executeQuery("select count(*) from monSysSQLText");
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
//				StringBuilder planSb = null;
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
//						planSb     = new StringBuilder();
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
//						if (_historyRestrictions)
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
				ResultSet rs = stmt.executeQuery(sql);
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
						if (_historyRestrictions)
						{
							if ( restrictionsHistoricalKeys.get(lKey) == null)
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
				rs.close();
				
				pdf.setWatermarkPlanText("");
			}
			else
			{
				// message that SQL Text isn't sampled
				String msg = "Refresh disabled due to:\n";
				if ( ! planTextPipe    ) msg += "sp_configure 'plan text pipe active', 0\n";
				if ( ! planTextPipeMsg ) msg += "sp_configure 'sql text pipe max messages', 0\n";
				if ( ! planTextSample  ) msg += "Checkbox 'Sample showplan text' is OFF\n";
				
				pdf.setWatermarkPlanText(msg);
			}

//			//------------------------------------
//			// Get current plan if not found in planHistory and if user has sa_role
//			//------------------------------------
//			if (has_sa_role && (_activeBatchID != -1))
//			{
//				// Check if plan exists
//				Batch batch = getPlanHistory(_activeSpid, _activeKpid, _activeBatchID, _activeDBID, _activeProcedureID);
//				if (batch == null)
//				{
//					stmt.executeUpdate("sp_showplanfull " + _activeSpid);
//					StringBuilder planSb = null;
//					SQLWarning sqlw = stmt.getWarnings();
//					while (true)
//					{
//						// _logger.debug(sqlw.getErrorCode()+" " +
//						// sqlw.getSQLState() +" "+sqlw.getMessage());
//						if (planSb == null)
//							planSb = new StringBuilder();
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
//						batch = new Batch(_activeBatchID);
//						addPlanHistory(_activeSpid, _activeKpid, _activeBatchID, _activeDBID, _activeProcedureID, batch, planSb);
//					}
//				}
//			}

			// If CURRENT SQL is selected, show some stuff.
			if (_selectedStatement == -1)
			{
				// If anything is running for the moment
//				if (_activeBatchID != -1)
//				{
//					// Display current plan
//					displayPlan(_activeSpid, _activeKpid, _activeBatchID, 0, _activeDBID, _activeProcedureID, _activeSqlLine);
//				}
				if (_activeBatch != null)
				{
					// Display current plan
					displayPlan(_activeBatch.spid, _activeBatch.kpid, _activeBatch.batchId, 
							_activeBatch.planId, 
							_activeBatch.dbid, _activeBatch.procedureId, 
							_activeBatch.lineNumber);
				}
				else
				{
					// Empty the PLAN text area
					pdf.setPlanText("");
				}
			}

			// Update panel
			Runnable updatePanel_inSwingThread = new Runnable()
			{
				@Override
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
			
			pdf.setStatusBar("Error when executing SQL, check 'Restrictions syntax'. ASE Message '"+SQLEx.getMessage()+"'.", true);

			if (DbxTune.hasGui())
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
//		int count = 0;
		while ( st.hasMoreTokens() )
		{
			String s = st.nextToken();
//			count += s.length();

			if (s.equals("\n")) 
				linenumber++;
		}
		// set rows
		_logger.debug("setting JTextArea to rows='"+linenumber+"'");
		text.setRows(linenumber);

	}

	public void setCaretToBatchLine(JTextArea text, int linenumber) 
	{
		if (linenumber <= 0) 
			return;

		text.setCaretPosition(0);

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

		ProcessDetailFrame.setSelectionAndMoveToTop(text, count, countRowAfter);
	}

	/**
	 * 
	 * @param dbname
	 * @param procName
	 * @param procLine
	 * @return 
	 * <ul>
	 * <li>-1 = Text not found</li>
	 * <li>1 = Statement Cache Entry</li>
	 * <li>2 = SQL Batch</li>
	 * </ul>
	 */
	public int displayProcInBatchWindow(String dbname, int objOwnerId, String procName, int procLine, int planId)
	{
		if (objOwnerId <= 0)
			objOwnerId = 1;

		return displayProcInBatchWindow(dbname, objOwnerId+"", procName, procLine, planId);
	}
	public int displayProcInBatchWindow(String dbname, String objOwnerNameOrId, String procName, int procLine, int planId)
	{
		if (_logger.isDebugEnabled())
			_logger.debug("displayProcInBatchWindow(dbname='"+dbname+"', objOwnerNameOrId='"+objOwnerNameOrId+"', procName='"+procName+"', procLine="+procLine+", planId="+planId+").");

		if (StringUtil.isNullOrBlank(procName))
			return -1; // -1 = Text not found

		if (StringUtil.isNullOrBlank(objOwnerNameOrId) || (objOwnerNameOrId != null && (objOwnerNameOrId.equals("-1") || objOwnerNameOrId.equals("0") || objOwnerNameOrId.equals("1"))) )
			objOwnerNameOrId = "dbo";
		
//		String        sqlStatement = null;
//		StringBuilder procTextSb   = null;
		String        procTextStr  = null;
		String        fullProcName = dbname+"."+objOwnerNameOrId+"."+procName;
		boolean       isStatementCacheEntry = false;

		// See if this the procName is a StatementCache or a Dynamic/Prepared statement and if it is: get the text via show_ached_plan_in_xml()
		if (procName.startsWith("*ss") || procName.startsWith("*sq") ) // *sq in ASE 15.7 esd#2, DynamicSQL can/will end up in statement cache
		{
			fullProcName = "StatementCache." + procName;
			isStatementCacheEntry = true;
		}
//System.out.println("displayProcInBatchWindow(dbname='"+dbname+"', objOwnerNameOrId='"+objOwnerNameOrId+"', procName='"+procName+"', procLine="+procLine+", planId="+planId+").");
//System.out.println("displayProcInBatchWindow(): fullProcName='"+fullProcName+"'.");

		// If the text is already loaded, don't do it again.
		if ( pdf.titledBorderBatch.getTitle().equals(fullProcName) )
		{
			setCaretToBatchLine(pdf.batchTextArea, procLine);
			
			if (isStatementCacheEntry)
				return 1; // 1 = Statement Cache Entry
			return 2;     // 2 = SQL Batch
		}

		if (isStatementCacheEntry && _conn.getDbmsVersionNumber() >= Ver.ver(15, 7))
		{
//System.out.println("----GET FROM XmlPlanCache: procName='"+procName+"', planId="+planId+" ------------------------------------");
			procTextStr = XmlPlanCache.getInstance().getPlan(procName, planId);
//System.out.println("    <<<<<<<< XmlPlanCache: procName='"+procName+"', planId="+planId+" Returned: " + (StringUtil.hasValue(procTextStr)?"HAS XML PLAN":"--NO-HIT-IN-PLAN-CACHE-WHICH-NEVER-HAPPENS--"));
		}
		else
		{
			procTextStr = _procedureTextCache.get(fullProcName);
			if (procTextStr == null)
			{
//System.out.println("----GET FROM DB: procTextStr------------------------------------");
//				procTextStr = AseConnectionUtils.cachedPlanInXml(_conn, procName, false);
				procTextStr = AseConnectionUtils.getObjectText(_conn, dbname, procName, objOwnerNameOrId, planId, _conn.getDbmsVersionNumber());

				// Store SQL text in "procText" cache
				_procedureTextCache.put(fullProcName, procTextStr);
//System.out.println("----BEGIN: procTextStr------------------------------------");
//System.out.println(procTextStr);
//System.out.println("------END: procTextStr------------------------------------");
    		}
    		else
    		{
//System.out.println("----USE CACHED entry for procTextStr------------------------------------");
    		}
		}

		
//		String procOrStmntText = AseConnectionUtils.getObjectText(_conn, dbname, procName, objOwnerNameOrId, _conn.getDbmsVersionNumber());
		if (StringUtil.hasValue(procTextStr))
		{
			int textType = -1; // Unknown

			if      (procTextStr.indexOf("SSQL_DESC ")       >= 0) textType = 1; // dbcc prsqlcache(ssqlid, 1) 
			else if (procTextStr.indexOf("<?xml version=\"") >= 0) textType = 2; // select show_cached_plan_in_xml(ssqlid, 0, 0)
			else                                                   textType = 3; // procedure text

//System.out.println("textType="+textType);

			// XML PLAN or DBCC PRSQLCACHE
			if (textType == 1 || textType == 2)
			{
				String xmlPlan = procTextStr;

				String sqlText = "";
				String planText = xmlPlan;

				// XML PLAN
				if (textType == 2 && xmlPlan != null)
				{
					int cDataStart = xmlPlan.indexOf("<![CDATA[");
					int cDataEnd   = xmlPlan.indexOf("]]>");
					if (cDataStart != -1 && cDataEnd != -1)
					{
						cDataStart += "<![CDATA[".length(); // move "pointer" past the "<![CDATA[" string
						
						int startPos = xmlPlan.indexOf("SQL Text:", cDataStart);
						if (startPos != -1)
						{
							startPos += "SQL Text:".length(); // move "pointer" past the "SQL Text:" string
							if (Character.isWhitespace(xmlPlan.charAt(startPos))) // If it is a space after "SQL Text:": move start with 1   
								startPos ++;
						}
						else
						{
							startPos = cDataStart;
						}
						
						sqlText = xmlPlan.substring(startPos, cDataEnd);
					}
				}

				// DBCC PRSQLCACHE
				if (textType == 1 && xmlPlan != null)
				{
					int sqlStart = xmlPlan.indexOf("SQL TEXT: ");
					int sqlEnd   = xmlPlan.indexOf("QUERY PLAN FOR STATEMENT");
					
					if (sqlStart != -1 && sqlEnd != -1)
					{
						sqlStart += "SQL TEXT: ".length(); // move "pointer" past the "SQL TEXT: " string
						
						sqlText = xmlPlan.substring(sqlStart, sqlEnd);
					}
				}
				
				
				// Set SQL Text
				pdf.titledBorderBatch.setTitle(fullProcName);
				pdf.batchTextArea.setText(sqlText);
				setCaretToBatchLine(pdf.batchTextArea, procLine);

				// Set PLAN Text
//				pdf.titledBorderBatch.setTitle(fullProcName);
				pdf.setPlanText(planText, procLine);

				return 1; // 1 = Statement Cache Entry
			}
			
			// Procedure Text
			if (textType == 3)
			{
				pdf.titledBorderBatch.setTitle(fullProcName);
				pdf.batchTextArea.setText(procTextStr);
				setCaretToBatchLine(pdf.batchTextArea, procLine);
				//setTextFix(pdf.batchTextArea);
				return 2; // 2 = SQL Batch
			}
		}

		// 
		pdf.batchTextArea.setText("Can't find text for procedure.");
		return -1; // -1 = Text not found

	} // end: method displayProcInBatchWindow

//FIXME: for the below .... just call: AseConnectionUtils.getObjectText(_conn, dbname, procName, owner, _conn.getDbmsVersionNumber())
//
//if its a 15.x the output will look like:
//
//SSQL_DESC 0x0x15181e090
//ssql_name *ss0399073476_1965424952ss*
//ssql_hashkey 0x0x75260138	ssql_id 399073476
//ssql_suid 1		ssql_uid 1	ssql_dbid 1	ssql_spid 0
//ssql_status 0x0xa0	ssql_parallel_deg 1
//ssql_isolate 1		ssql_tranmode 32
//ssql_keep 0		ssql_usecnt 2	ssql_pgcount 8
//ssql_optgoal allrows_mix	ssql_optlevel ase_default
//opt options bitmap  0080bf972c6181fffb160100008001000000000000000000000000000000
//SQL TEXT: select db_name(), user_name(uid), name from sysobjects where type in('P')
//
//
//QUERY PLAN FOR STATEMENT 1 (at line 1).
//Optimized using Serial Mode
//
//
//    STEP 1
//        The type of query is SELECT.
//
//	2 operator(s) under root
//
//       |ROOT:EMIT Operator (VA = 2)
//       |
//       |   |RESTRICT Operator (VA = 1)(0)(3)(0)(0)(3)
//       |   |
//       |   |   |SCAN Operator (VA = 0)
//       |   |   |  FROM TABLE
//       |   |   |  sysobjects
//       |   |   |  Table Scan.
//       |   |   |  Forward Scan.
//       |   |   |  Positioning at start of table.
//       |   |   |  Using I/O Size 4 Kbytes for data pages.
//       |   |   |  With LRU Buffer Replacement Strategy for data pages.
//
//
//
//
//DBCC execution completed. If DBCC printed error messages, contact a user with System Administrator (SA) role.


//		//--------------------------------------
//		// FIRTS: Check for Statement Cache
//		//--------------------------------------
//		// See if this the procName is a StatementCache or a Dynamic/Prepared statement and if it is: get the text via show_ached_plan_in_xml()
//		if (procName.startsWith("*ss") || procName.startsWith("*sq") ) // *sq in ASE 15.7 esd#2, DynamicSQL can/will end up in statement cache
//		{
//			fullProcName = "stmntcache." + procName;
//			
//			// If the text is already loaded, don't do it again.
//			if ( pdf.titledBorderBatch.getTitle().equals(fullProcName) )
//			{
//				setCaretToBatchLine(pdf.batchTextArea, procLine);
//				return 1;
//			}
//
//			procTextStr = _procedureTextCache.get(fullProcName);
//			if (procTextStr == null)
//			{
//				procTextStr = AseConnectionUtils.cachedPlanInXml(_conn, procName, false);
//
//				// Store SQL text in "procText" cache
//				_procedureTextCache.put(fullProcName, procTextStr);
//			}
//
//			// Yes it can still be null if cachedPlanInXml() fails
//			if (procTextStr != null)
//			{
//				String xmlPlan = procTextStr;
//
//				String sqlText = "";
//				String planText = xmlPlan;
//
//				if (xmlPlan != null)
//				{
//					int cDataStart = xmlPlan.indexOf("<![CDATA[");
//					int cDataEnd   = xmlPlan.indexOf("]]>");
//					if (cDataStart != -1 && cDataEnd != -1)
//					{
//						cDataStart += "<![CDATA[".length(); // move "pointer" past the "<![CDATA[" string
//						
//						int startPos = xmlPlan.indexOf("SQL Text:", cDataStart);
//						if (startPos != -1)
//						{
//							startPos += "SQL Text:".length(); // move "pointer" past the "SQL Text:" string
//							if (Character.isWhitespace(xmlPlan.charAt(startPos))) // If it is a space after "SQL Text:": move start with 1   
//								startPos ++;
//						}
//						else
//						{
//							startPos = cDataStart;
//						}
//						
//						sqlText = xmlPlan.substring(startPos, cDataEnd);
//					}
//				}
//
//				// Set SQL Text
//				pdf.titledBorderBatch.setTitle(fullProcName);
//				pdf.batchTextArea.setText(sqlText);
//				setCaretToBatchLine(pdf.batchTextArea, procLine);
//
//				// Set PLAN Text
////				pdf.titledBorderBatch.setTitle(fullProcName);
//				pdf.planTextArea.setText(planText);
//				setCaretToPlanLine(pdf.planTextArea, procLine);
//
//				return 1;
//			}
//			else
//			{
//				pdf.batchTextArea.setText("Can't find text from Statement Cache.");
//				return -1;
//			}
//		}
//
//
//		//--------------------------------------
//		// NOT Statement Cache logic
//		//--------------------------------------
//		
//		// If the procedure text is already loaded, don't do it again.
//		if ( pdf.titledBorderBatch.getTitle().equals(fullProcName) )
//		{
//			setCaretToBatchLine(pdf.batchTextArea, procLine);
//			return 2;
//		}
//
//		// get check if we already has the text for this procedure.
//		procTextStr = _procedureTextCache.get(fullProcName);
//
//		if (procTextStr != null)
//		{
//			_logger.debug("Found object '"+fullProcName+"' in _procedureTextCache.");
//		}
//		else
//		{
////			// First get the DBNAME and PROCNAME
////			sqlStatement = "select db_name("+dbId+"), object_name("+procId+", "+dbId+")";
////			try
////			{
////				Statement statement = conn.createStatement();
////				ResultSet rs = statement.executeQuery(sqlStatement);
////				while(rs.next())
////				{
////					dbname   = rs.getString(1)();
////					procName = rs.getString(2)();
////				}
//					rs.close();
////			}
////			catch (Exception e)
////			{
////				JOptionPane.showMessageDialog(null, "Executing SQL command '"+sqlStatement+"'. Found the following error:\n."+e, "Error", JOptionPane.ERROR_MESSAGE);
////			}
////			
////			_logger.debug("Getting object dbid='"+dbId+"', dbname='"+dbname+"', procId='"+procId+"', procName='"+procName+"' from database.");
//
//			_logger.debug("Getting object '"+fullProcName+"' from database.");
////			/*
////			** See if the object is hidden (SYSCOM_TEXT_HIDDEN will be set)
////			*/
////			if exists (select 1
////			        from syscomments where (status & 1 = 
////			 1)
////			        and id = object_id(@objname))
////			begin
////			        /*
////			        ** 18406, "Source text for compiled object %!1
////			        ** (id = %!2) is hidden."
////			        */
////			        select @proc_id = object_id(@objname)
////			        raiserror 18406, @objname, @proc_id
////			       
////			  return (1)
////			end
//
//			sqlStatement = "select c.text, c.status, c.id"
//				+ " from "+dbname+"..sysobjects o, "+dbname+"..syscomments c"
//				+ " where o.name = '"+procName+"'"
//				+ "   and o.id = c.id"
//				+ " order by c.number, c.colid2, c.colid";
//	
//			try
//			{
//				Statement statement = _conn.createStatement();
//				ResultSet rs = statement.executeQuery(sqlStatement);
//				while(rs.next())
//				{
//					String textPart = rs.getString(1);
//					int    status   = rs.getInt(2);
//					int    id       = rs.getInt(3);
//					
//					if (procTextSb == null)
//						procTextSb = new StringBuilder();
//	
//					// if status is ASE: SYSCOM_TEXT_HIDDEN
//					if ((status & 1) == 1)
//					{
//						procTextSb.append("ASE StoredProcedure Source text for compiled object '"+dbname+".dbo."+procName+"' (id = "+id+") is hidden.");
//						break;
//					}
//
//					procTextSb.append(textPart);
//				}
//				rs.close();
//			}
//			catch (Exception e)
//			{
//				JOptionPane.showMessageDialog(null, "Executing SQL command '"+sqlStatement+"'. Found the following error:\n."+e, "Error", JOptionPane.ERROR_MESSAGE);
//			}
//
//			if (procTextSb != null)
//			{
//				procTextStr = procTextSb.toString();
//
//				// Store the procedure cache text in cache
//				_procedureTextCache.put(fullProcName, procTextStr);
//			}
//		}
//
//		if (procTextStr != null)
//		{
//			pdf.titledBorderBatch.setTitle(fullProcName);
//			pdf.batchTextArea.setText(procTextStr);
//			setCaretToBatchLine(pdf.batchTextArea, procLine);
//			//setTextFix(pdf.batchTextArea);
//			return 2;
//		}
//		else
//		{
//			pdf.batchTextArea.setText("Can't find text for procedure.");
//			return -1;
//		}
//	}

	public void displayActiveBatch()
	{
		pdf.titledBorderBatch.setTitle("Batch text");
		pdf.batchTextArea.setCaretPosition(0);
		pdf.batchTextArea.setFocusable(true);

		if (_activeBatch == null)
		{
			pdf.batchTextArea.setText("");
			return;
		}

		if (    !_activeBatch.dbname.equals("") 
			 && !_activeBatch.procedureName.equals("") 
			 && !_activeBatch.procedureName.startsWith("*")
			 && _activeSqlLine > 0
			 && pdf.sqlTextShowProcSrcCheckbox.isSelected() 
		   )
		{
			// Display current running stored proc
			displayProcInBatchWindow(_activeBatch.dbname, _activeBatch.objectOwnerId, _activeBatch.procedureName, _activeBatch.lineNumber, _activeBatch.planId);

//			_logger.debug("BATCH SQL: " + _activeBatch.getSqlText());
		}
		else
		{
			pdf.batchTextArea.setText( _activeBatch.getSqlText() );
			setCaretToBatchLine(pdf.batchTextArea, _activeSqlLine);
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
			pdf.setPlanText("No SHOWPLAN text was found in the plan history.");
			return;
		}

		/*
		 * if (batch.getBatchId() != planId) { _logger.debug
		 * ("AseTune.RefreshProcess : bd batchId, asked "+planId+" returned
		 * "+batch.getBatchId()); }
		 */

		String planStr = batch.getShowplanText();
		pdf.setPlanText(planStr, sqlLine);
	}

	public void setSelectedStatement(int rowid)
	{
		_logger.debug("setSelectedStatement(rowid="+rowid+")");

		// This method is called *several* times...
		// So only do action when, rowid, chaanges
		// Make shure we only do stuf when the rowid changes
		if (rowid == _selectedStatement)
			return;
		_selectedStatement = rowid;

		// if rowid is -1, then it's the current running SQL that is selected
		// if rowid >= 0, the it's the captured statements we are looking at.
		if (rowid >= 0)
		{
			Object o = _historyStmtModel.getValueAt(rowid, 0);
			if (o == null)
			{
				_logger.error("Row "+rowid+" column 0, does NOT contain any data...");
				return;
			}
//			_logger.debug("setSelectedStatement()"
//					+ ", pos_historySpid="        + pos_historySpid 
//					+ ", pos_historyKpid="        + pos_historyKpid 
//					+ ", pos_historyBatchID="     + pos_historyBatchID 
//					+ ", pos_historySqlLine="     + pos_historySqlLine 
//					+ ", pos_historyPlanID="      + pos_historyPlanID
//					+ ", pos_historyDBID="        + pos_historyDBID
//					+ ", pos_historyProcedureID=" + pos_historyProcedureID
//					+ ", pos_historyDbname="      + pos_historyDbname
//					+ ", pos_historyProcName="    + pos_historyProcName
//					+ ".");

			int spid       = ((Number) (_historyStmtModel.getValueAt(rowid, pos_historySpid       -1))).intValue();
			int kpid       = ((Number) (_historyStmtModel.getValueAt(rowid, pos_historyKpid       -1))).intValue();
			int batchId    = ((Number) (_historyStmtModel.getValueAt(rowid, pos_historyBatchID    -1))).intValue();
			int sqlLine    = ((Number) (_historyStmtModel.getValueAt(rowid, pos_historySqlLine    -1))).intValue();
			int planId     = ((Number) (_historyStmtModel.getValueAt(rowid, pos_historyPlanID     -1))).intValue();
			int dbId       = ((Number) (_historyStmtModel.getValueAt(rowid, pos_historyDBID       -1))).intValue();
			int objOwnerId = ((Number) (_historyStmtModel.getValueAt(rowid, pos_historyObjOwnerID -1))).intValue();
			int procId     = ((Number) (_historyStmtModel.getValueAt(rowid, pos_historyProcedureID-1))).intValue();

			if (_logger.isDebugEnabled())
				_logger.debug("setSelectedStatement(), batchId='" + batchId + "', planId='" + planId + "', dbId='" + dbId + "', procId='" + procId + "', (sqlLine='"+sqlLine+"')");

			int textType = -1;

			if (procId > 0 && sqlLine > 0 && pdf.sqlTextShowProcSrcCheckbox.isSelected())
			{
				String dbname   = (String) (_historyStmtModel.getValueAt(rowid, pos_historyDbname  -1));
				String procname = (String) (_historyStmtModel.getValueAt(rowid, pos_historyProcName-1));

				textType = displayProcInBatchWindow(dbname, objOwnerId, procname, sqlLine, planId);
			}

			if ( textType < 0 ) // if the ProcText wasn't found, go and get the SQL Batch Text
				displayBatch(spid, kpid, batchId, sqlLine);

			if ( textType != 1 ) // Not for Statement Cache: that is done in displayProcInBatchWindow()
				displayPlan(spid, kpid, batchId, planId, dbId, procId, sqlLine);
		}
		else // then it's the current running SQL that is selected
		{
			int textType = -1;

			if (    _activeDbname   != null && !_activeDbname.equals("")
			     && _activeProcName != null && !_activeProcName.equals("")
			     && _activeProcName != null && !_activeProcName.startsWith("*")
			     && _activeSqlLine > 0
				 && pdf.sqlTextShowProcSrcCheckbox.isSelected() 
			   )
			{
				// Display current running stored proc
				textType = displayProcInBatchWindow(_activeDbname, _activeObjOwnerID, _activeProcName, _activeSqlLine, _activePlanID);
			}

			if ( textType < 0 ) // if the ProcText wasn't found, go and get the SQL Batch Text
				displayActiveBatch();

			if ( textType != 1 ) // Not for Statement Cache: that is done in displayProcInBatchWindow()
				displayPlan(_activeSpid, _activeKpid, _activeBatchID, 0, _activeDBID, _activeProcedureID, _activeSqlLine);
		}
	}

	public int getSelectedStatement()
	{
		return _selectedStatement;
	}

	private void updatePanel_code()
	{
		// Refresh the current statement JTable
		if (!_activeSMInitialized)
		{
			_activeStmtModel.setDataVector(_activeStmt, _activeStmtColNames);
			_activeSMInitialized = true;
		}
//		if (currentStmtRow != null)
//		{
//			// insert or update in the curStmt JTable
//			if (_activeStmtModel.getRowCount() == 0)
//			{
//				_activeStmtModel.addRow(currentStmtRow);
//				_activeStmtModel.setRowCount(1);
//			}
//			else
//			{
//				for (int i = 0; i < currentStmtRow.size(); i++)
//				{
//					_activeStmtModel.setValueAt(currentStmtRow.get(i), 0, i);
//				}
//			}
//		}
//		else if (_activeStmtModel.getRowCount() == 1)
//		{
//			_activeStmtModel.removeRow(0);
//			_activeStmtModel.setRowCount(0);
//		}
		if (_activeStmtRows != null)
		{
			// refresh
			_activeStmtModel.setDataVector(_activeStmtRows, _activeStmtColNames);
			
			// resize the columns
			SwingUtils.calcColumnWidths(pdf.activeStatementTable);
		}
		else if (_activeStmtModel.getRowCount() > 0)
		{
			// Clear the table
			_activeStmtModel.setRowCount(0);
		}

		// Refresh the captured statement JTable
		if (_newHistoryStatements != null)
		{
			if (!_historySMInitialized)
			{
				_historyStmtModel.setDataVector(new Vector(), _historyStmtColNames);
				_historySMInitialized = true;

				// resize the columns, it's only done when new header
				// It could be done always, but since we loop all rows in the table, 
				//    it might takte to long time to do this
				SwingUtils.calcColumnWidths(pdf._historyStatementsTable);
				_historyTableColmnsIsResized = false;
			}
			for (int i=0; _newHistoryStatements!=null && i<_newHistoryStatements.size(); i++)
			{
				_historyStmtModel.addRow((Vector) _newHistoryStatements.get(i));

				// resize, but only check with for newly inserted rows.
				//TabularCntrPanel.calcColumnWidths(pdf.capturedStatementsTable, _newHistoryStatements.size(), true);

				// resize, But only do this first time we see data
				if ( ! _historyTableColmnsIsResized )
				{
					_historyTableColmnsIsResized = true;
					SwingUtils.calcColumnWidths(pdf._historyStatementsTable);
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
		_logger.debug("Before clear(), _batchHistory had "+_batchHistory.size()+" entries.");
		_batchHistory.clear();

//		_logger.debug("Before clear(), _plansHistory had "+_plansHistory.size()+" entries.");
//		_plansHistory.clear();
//
//		_logger.debug("Before clear(), _compiledPlansHistory had "+_compiledPlansHistory.size()+" entries.");
//		_compiledPlansHistory.clear();

		_logger.debug("Before clear(), _procedureTextCache had "+_procedureTextCache.size()+" entries.");
		_procedureTextCache.clear();

		_logger.debug("Before clear(), _activeStmtModel had "+_activeStmtModel.getRowCount()+" entries.");
		_activeStmtModel.clear();

		_logger.debug("Before clear(), _historyStmtModel had "+_historyStmtModel.getRowCount()+" entries.");
		_historyStmtModel.clear();

		pdf.batchTextArea.setText("");
		pdf.setPlanText("");
	}

	/** Called from ProcessDetailFrame outOfMemoryHandler() */
	public void outOfMemoryHandler()
	{
		_logger.info("outOfMemoryHandler(): Clearing Batch History.");
		_batchHistory.clear();

		_logger.info("outOfMemoryHandler(): Clearing Procedure Text Cache.");
		_procedureTextCache.clear();
	}

	/** Called from ProcessDetailFrame memoryConsumption() */
	public void memoryConsumption(int memoryLeftInMB)
	{
	}

	public void saveHistoryStatementsToFile()
  	{
		saveHistoryStatementsToFile(null, "", "", "");
  	}
	public void saveHistoryStatementsToFile(String saveToDir, String bcpFilename, String txtFilename, String tabDefFilename)
  	{
		// BCP file (open in append mode, write data only)
		BufferedWriter bcpWriter     = null;

		// Write header + data (can be used in Excel or simular)
		BufferedWriter txtWriter        = null;
//		boolean        txtWriterNewFile = false;

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
//			String envNameSaveDir = DbxTune.getInstance().getAppSaveDirEnvName();  // ASETUNE_SAVE_DIR
//			String envNameHomeDir = DbxTune.getInstance().getAppHomeEnvName();     // ASETUNE_HOME
			String envNameSaveDir = "DBXTUNE_SAVE_DIR";
			String envNameHomeDir = "DBXTUNE_HOME";

			saveToDir = StringUtil.getEnvVariableValue(envNameSaveDir);

			if (saveToDir == null)
			{
				saveToDir = StringUtil.getEnvVariableValue(envNameHomeDir);

				if (saveToDir == null)
				{
					_logger.error("Directory name was not specified and "+envNameSaveDir+" or "+envNameHomeDir+" was not set, can't save information about Historical Statements.");
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

//				if (txtWriterFile.exists())
//					txtWriterNewFile = false;
//				else
//					txtWriterNewFile = true;
					
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
				tabWriter.write("create table HistoryStatements \n");
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
				if (_srvVersion >= Ver.ver(15,0,0,2) || (_srvVersion >= Ver.ver(12,5,4) && _srvVersion < Ver.ver(15,0)) )
				{
				tabWriter.write("    RowsAffected       int             not null,\n");
				tabWriter.write("    ErrorStatus        int             not null,\n");
				}
				if (_srvVersion >= Ver.ver(15,0,3) )
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
				tabWriter.write("    ObjOwnerID         int             not null,\n");
				tabWriter.write("    ProcedureID        int             not null,\n");
				tabWriter.write(") \n");
				tabWriter.write("go\n");
				tabWriter.write("\n");
				tabWriter.write("create index CapStmnts_ix1 on HistoryStatements(SaveTime)\n");
				tabWriter.write("go\n");
				tabWriter.write("\n");
				tabWriter.write("create index CapStmnts_ix2 on HistoryStatements(procname, LineNumber)\n");
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
//		String rowSep = "\n";
		
		Object       colObj    = null;
		StringBuilder rowSb     = new StringBuilder();
		StringBuilder headerSb  = new StringBuilder();

		int rows = _historyStmtModel.getRowCount();
		int cols = _historyStmtModel.getColumnCount();

		// Loop all headers, to get name(s)
		headerSb.append("SaveTime");
		headerSb.append(colSep);
		for (int c=0; c<cols; c++)
		{
			String colName = _historyStmtModel.getColumnName(c);
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
				colObj = _historyStmtModel.getValueAt(r, c);
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

//	public void createCounters()
//	{
////		String   cols1 = null;
////		String   cols2 = null;
////		String   cols3 = null;
//
////		String   name;
////		String   displayName;
////		String   description;
//		int      needVersion   = 0;
//		int      needCeVersion = 0;
//		String[] monTables;
//		String[] needRole;
//		String[] needConfig;
//		String[] colsCalcDiff;
//		String[] colsCalcPCT;
////		List     pkList;
//
//		String groupName = "RefreshProcess";
//
//		String whereSpidKpid = "";
//		if (_spid > 0) whereSpidKpid += "  and SPID = " + _spid + "\n";
//		if (_kpid > 0) whereSpidKpid += "  and KPID = " + _kpid + "\n";
//
//		
//		//------------------------------------
//		//------------------------------------
//		// Objects
//		//------------------------------------
//		//------------------------------------
////		name         = "_cmProcObjects";
////		displayName  = "Objects";
////		description  = "What objects are accessed right now.";
//
//		needVersion  = 0;
//		monTables    = new String[] { "monProcessObject" };
//		needRole     = new String[] {"mon_role"};
//		needConfig   = new String[] {"enable monitoring", "per object statistics active"};
//		colsCalcDiff = new String[] { "LogicalReads", "PhysicalReads", "PhysicalAPFReads" };
//		colsCalcPCT  = new String[] {};
////		pkList       = new LinkedList();
////		     pkList.add("KPID");
////		     pkList.add("DBName");
////		     pkList.add("ObjectID");
////		     pkList.add("IndexID");
////		     pkList.add("OwnerUserID");
////
////		cols1 = cols2 = cols3 = "";
////		cols1 = "SPID, KPID, DBName, ObjectID, OwnerUserID, ObjectName, IndexID, ObjectType, ";
////		cols2 = "LogicalReads, PhysicalReads, PhysicalAPFReads, dupMergeCount=convert(int,0)";
////		cols3 = "";
//////		if (_srvVersion >= 12520)
//////		if (_srvVersion >= 1252000)
////		if (_srvVersion >= Ver.ver(12,5,2))
////		{
////			cols3 = ", TableSize";
////		}
//////		if (_srvVersion >= 15000)
//////		if (_srvVersion >= 1500000)
////		if (_srvVersion >= Ver.ver(15,0))
////		{
////			cols1 += "PartitionID, PartitionName, "; // new cols in 15.0.0
////			cols3 = ", PartitionSize";  // TableSize has changed name to PartitionSize
////
////			pkList.add("PartitionID");
////		}
////
////		String CMProcObjectsSqlBase = 
////			"select " + cols1 + cols2 + cols3 + "\n" +
////		    "from monProcessObject \n" +
////		    "where 1=1 ";
//
//		_cmProcObjects = new CountersModel(
//				"cmProcObjects", groupName, null, null, 
//				colsCalcDiff, colsCalcPCT, 
//				monTables, needRole, needConfig, needVersion, needCeVersion, true, true)
//		{
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public boolean isConnected()
//			{
//				if (_conn == null)
//					return false;
//				return _conn.isConnectionOk();
//			}
//
//			@Override
//			public ITableTooltip createToolTipSupplier()
//			{
//				//return CounterController.getInstance().createCmToolTipSupplier(this);
//				return null;
//			}
//
//			@Override
//			public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
//			{
//				String cols1 = "";
//				String cols2 = "";
//				String cols3 = "";
//				cols1 = "SPID, KPID, DBName, ObjectID, OwnerUserID, ObjectName, IndexID, ObjectType, \n";
//				cols2 = "LogicalReads, PhysicalReads, PhysicalAPFReads, dupMergeCount=convert(int,0) \n";
//				cols3 = "";
//				if (srvVersion >= Ver.ver(12,5,2))
//				{
//					cols3 = ", TableSize";
//				}
//				if (srvVersion >= Ver.ver(15,0))
//				{
//					cols1 += "PartitionID, PartitionName, "; // new cols in 15.0.0
//					cols3 = ", PartitionSize";  // TableSize has changed name to PartitionSize
//				}
//
//				String sql = 
//					"select " + cols1 + cols2 + cols3 + "\n" +
//					"from monProcessObject \n" +
//					"where 1=1 ";
//
//				return sql;
//			}
//
//			@Override
//			public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
//			{
//				List <String> pkCols = new LinkedList<String>();
//
////				if (isClusterEnabled)
////					pkCols.add("InstanceID");
//
//				pkCols.add("KPID");
//				pkCols.add("DBName");
//				pkCols.add("ObjectID");
//				pkCols.add("IndexID");
//				pkCols.add("OwnerUserID");
//
//				if (srvVersion >= Ver.ver(15,0))
//					pkCols.add("PartitionID");
//
//				return pkCols;
//			}
//
//			@Override
//			public boolean isRefreshable()
//			{
//				boolean refresh = false;
//
//				// Current TAB is visible
//				if ( equalsTabPanel(pdf.getActiveTab()) )
//					refresh = true;
//		
//				// Current TAB is un-docked (in it's own window)
//				if (getTabPanel() != null)
//				{
//					JTabbedPane tp = pdf.getTabbedPane();
//					if (tp instanceof GTabbedPane)
//					{
//						GTabbedPane gtp = (GTabbedPane) tp;
//						if (gtp.isTabUnDocked(getTabPanel().getPanelName()))
//							refresh = true;
//					}
//				}
//
//				// Background poll is checked
//				if ( isBackgroundDataPollingEnabled() )
//					refresh = true;
//
//				// NO-REFRESH if data polling is PAUSED
//				if ( isDataPollingPaused() )
//					refresh = false;
//
//				// Check postpone
//				if ( getTimeToNextPostponedRefresh() > 0 )
//				{
//					_logger.debug("Next refresh for the cm '"+getName()+"' will have to wait '"+TimeUtils.msToTimeStr(getTimeToNextPostponedRefresh())+"'.");
//					refresh = false;
//				}
//
//				return refresh;
//			}
//		};
//
//		_cmProcObjects.setTabPanel(pdf.processObjectsPanel);
//		
//		if (_spid > 0 || _kpid > 0)
//			_cmProcObjects.setSqlWhere(whereSpidKpid);
//
//		
//		//------------------------------------
//		//------------------------------------
//		// Waits
//		//------------------------------------
//		//------------------------------------
////		name         = "_cmProcWaits";
////		displayName  = "Waits";
////		description  = "What is this spid waiting for right now.";
//
//		needVersion  = 0;
//		monTables    = new String[] { "monProcessWaits", "monWaitEventInfo", "monWaitClassInfo" };
//		needRole     = new String[] {"mon_role"};
//		needConfig   = new String[] {"enable monitoring", "process wait events", "wait event timing"};
//		colsCalcDiff = new String[] { "WaitTime", "Waits" };
//		colsCalcPCT  = new String[] {};
////		pkList       = new LinkedList();
////		     pkList.add("KPID");
////		     pkList.add("WaitEventID");
////
////		String   CMProcWaitsSqlBase = 
////			"select SPID, KPID, Class=C.Description, Event=I.Description, W.WaitEventID, WaitTime, Waits " + 
////		    "from monProcessWaits W, monWaitEventInfo I, monWaitClassInfo C " + 
////		    "where W.WaitEventID=I.WaitEventID " + 
////		    "  and I.WaitClassID=C.WaitClassID ";
////		_cmProcWaits = new CountersModel(
////				"_cmProcWaits", 
////				CMProcWaitsSqlBase, 
////				pkList,	colsCalcDiff, colsCalcPCT, 
////				monTables, needRole, needConfig, needVersion, needCeVersion, true, true)
////		{
//		_cmProcWaits = new CountersModel(
//				"cmProcWaits", groupName, null, null,
//				colsCalcDiff, colsCalcPCT, 
//				monTables, needRole, needConfig, needVersion, needCeVersion, true, true)
//		{
//			private static final long	serialVersionUID	= 1L;
//
//			@Override
//			public boolean isConnected()
//			{
//				if (_conn == null)
//					return false;
//				return _conn.isConnectionOk();
//			}
//
//			@Override
//			public ITableTooltip createToolTipSupplier()
//			{
//				//return CounterController.getInstance().createCmToolTipSupplier(this);
//				return null;
//			}
//
//			@Override
//			protected CmSybMessageHandler createSybMessageHandler()
//			{
//				CmSybMessageHandler msgHandler = super.createSybMessageHandler();
//	
//				// If ASE is above 15.0.3 esd#1, and dbcc traceon(3604) is given && 'capture missing stats' is 
//				// on the 'CMsysWaitActivity' CM will throw an warning which should NOT be throws...
//				//if (getServerVersion() >= 15031) // NOTE this is done early in initialization, so getServerVersion() can't be used
//				//if (getServerVersion() >= 1503010) // NOTE this is done early in initialization, so getServerVersion() can't be used
//				//if (getServerVersion() >= Ver.ver(15,0,3,1)) // NOTE this is done early in initialization, so getServerVersion() can't be used
//				msgHandler.addDiscardMsgStr("WaitClassID, WaitEventID");
//	
//				return msgHandler;
//			}
//
//			@Override
//			public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
//			{
//				String sql = 
//					"select SPID, KPID, Class=C.Description, Event=I.Description, W.WaitEventID, WaitTime, Waits \n" + 
//					"from master..monProcessWaits W, master..monWaitEventInfo I, master..monWaitClassInfo C \n" + 
//					"where W.WaitEventID=I.WaitEventID \n" + 
//					"  and I.WaitClassID=C.WaitClassID \n";
//
//				if (srvVersion >= Ver.ver(15,7))
//				{
//					sql += "  and C.Language = 'en_US' \n";
//					sql += "  and I.Language = 'en_US' \n";
//				}
//				return sql;
//			}
//
//			@Override
//			public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
//			{
//				List <String> pkCols = new LinkedList<String>();
//
////				if (isClusterEnabled)
////					pkCols.add("InstanceID");
//
//				pkCols.add("KPID");
//				pkCols.add("WaitEventID");
//
//				return pkCols;
//			}
//
//			@Override
//			public boolean isRefreshable()
//			{
//				boolean refresh = false;
//
//				// Current TAB is visible
//				if ( equalsTabPanel(pdf.getActiveTab()) )
//					refresh = true;
//		
//				// Current TAB is un-docked (in it's own window)
//				if (getTabPanel() != null)
//				{
//					JTabbedPane tp = pdf.getTabbedPane();
//					if (tp instanceof GTabbedPane)
//					{
//						GTabbedPane gtp = (GTabbedPane) tp;
//						if (gtp.isTabUnDocked(getTabPanel().getPanelName()))
//							refresh = true;
//					}
//				}
//
//				// Background poll is checked
//				if ( isBackgroundDataPollingEnabled() )
//					refresh = true;
//
//				// NO-REFRESH if data polling is PAUSED
//				if ( isDataPollingPaused() )
//					refresh = false;
//
//				// Check postpone
//				if ( getTimeToNextPostponedRefresh() > 0 )
//				{
//					_logger.debug("Next refresh for the cm '"+getName()+"' will have to wait '"+TimeUtils.msToTimeStr(getTimeToNextPostponedRefresh())+"'.");
//					refresh = false;
//				}
//
//				return refresh;
//			}
//		};
//
//
//		_cmProcWaits.setTabPanel(pdf.processWaitsPanel);
//
//		if (_spid > 0 || _kpid > 0)
//			_cmProcWaits.setSqlWhere(whereSpidKpid);
//
//
//		//------------------------------------
//		//------------------------------------
//		// Locks
//		//------------------------------------
//		//------------------------------------
//		/*
//		** SPID        KPID        DBID        ParentSPID  LockID      Context     ObjectID    LockState            LockType             LockLevel                      WaitTime    PageNumber  RowNumber   
//		** ----------- ----------- ----------- ----------- ----------- ----------- ----------- ---------            --------             ---------                      ----------- ----------- ----------- 
//		**         221  1222509855          17           0         442           8   757577737 Granted              shared page          PAGE                                  NULL      854653        NULL 
//		**         221  1222509855          17           0         442           8  1073438898 Granted              shared page          PAGE                                  NULL      479900        NULL 
//		**         405   222298460           4           0         810           0  1768445424 Granted              shared intent        TABLE                                 NULL        NULL        NULL 
//		**         405   222298460           4           0         810           0  1560444683 Granted              shared intent        TABLE                                 NULL        NULL        NULL 
//		**         405   222298460           4           0         810           0  1592444797 Granted              shared intent        TABLE                                 NULL        NULL        NULL 
//		**         405   222298460           4           0         810           0  1736445310 Granted              shared intent        TABLE                                 NULL        NULL        NULL 
//		*/
////		name         = "CMProcLocks";
////		displayName  = "Locks";
////		description  = "What locks does this _spid hold right now.";
//
//		needVersion  = 0;
//		monTables    = new String[] { "monLocks" };
//		needRole     = new String[] {"mon_role"};
//		needConfig   = new String[] {"enable monitoring"};
//		colsCalcDiff = new String[] {};
//		colsCalcPCT  = new String[] {};
//
////		//List pkList_CMLocks = new LinkedList();
////		pkList = null;
//
////		cols1 = cols2 = cols3 = "";
////		cols1 = "SPID, KPID, DBID, ParentSPID, LockID, Context, ObjectID, ObjectName=object_name(ObjectID, DBID), LockState, LockType, LockLevel, ";
////		cols2 = "";
////		cols3 = "WaitTime, PageNumber, RowNumber";
//////		if (_srvVersion >= 15002)
//////		if (_srvVersion >= 1500020)
////		if (_srvVersion >= Ver.ver(15,0,0,2))
////		{
////			cols2 = "BlockedState, BlockedBy, ";  //
////		}
//////		if (_srvVersion >= 15020)
//////		if (_srvVersion >= 1502000)
////		if (_srvVersion >= Ver.ver(15,0,2))
////		{
////			cols3 += ", SourceCodeID";  //
////		}
////		
////		String   CMLocksSqlBase =
////			"select " + cols1 + cols2 + cols3 + "\n" +
////			"from monLocks L " + 
////			"where 1=1 ";
//
//		_cmLocks = new CountersModel(
//				"cmLocks", groupName, null, null, 
//				colsCalcDiff, colsCalcPCT, 
//				monTables, needRole, needConfig, needVersion, needCeVersion, false, true)
//		{
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public boolean isConnected()
//			{
//				if (_conn == null)
//					return false;
//				return _conn.isConnectionOk();
//			}
//
//			@Override
//			public ITableTooltip createToolTipSupplier()
//			{
//				//return CounterController.getInstance().createCmToolTipSupplier(this);
//				return null;
//			}
//
//			@Override
//			public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
//			{
//				String cols1 = "";
//				String cols2 = "";
//				String cols3 = "";
//
//				cols1 = "SPID, KPID, DBID, ParentSPID, LockID, Context, \n" +
//						"ObjectID, ObjectName=object_name(ObjectID, DBID), \n" +
//						"LockState, LockType, LockLevel, ";
//				cols2 = "";
//				cols3 = "WaitTime, PageNumber, RowNumber";
//				if (srvVersion >= Ver.ver(15,0,0,2))
//				{
//					cols2 = "BlockedState, BlockedBy, ";  //
//				}
//				if (srvVersion >= Ver.ver(15,0,2))
//				{
//					cols3 += ", SourceCodeID";  //
//				}
//				if (srvVersion >= Ver.ver(16,0))
//				{
//					cols3 += ", PartitionID";  //
//				}
//				
//				String sql =
//					"select " + cols1 + cols2 + cols3 + "\n" +
//					"from monLocks L " + 
//					"where 1=1 ";
//
//				return sql;
//			}
//
//			@Override
//			public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
//			{
//				return null;
//			}
//
//			@Override
//			public boolean isRefreshable()
//			{
//				boolean refresh = false;
//
//				// Current TAB is visible
//				if ( equalsTabPanel(pdf.getActiveTab()) )
//					refresh = true;
//		
//				// Current TAB is un-docked (in it's own window)
//				if (getTabPanel() != null)
//				{
//					JTabbedPane tp = pdf.getTabbedPane();
//					if (tp instanceof GTabbedPane)
//					{
//						GTabbedPane gtp = (GTabbedPane) tp;
//						if (gtp.isTabUnDocked(getTabPanel().getPanelName()))
//							refresh = true;
//					}
//				}
//
//				// Background poll is checked
//				if ( isBackgroundDataPollingEnabled() )
//					refresh = true;
//
//				// NO-REFRESH if data polling is PAUSED
//				if ( isDataPollingPaused() )
//					refresh = false;
//
//				// Check postpone
//				if ( getTimeToNextPostponedRefresh() > 0 )
//				{
//					_logger.debug("Next refresh for the cm '"+getName()+"' will have to wait '"+TimeUtils.msToTimeStr(getTimeToNextPostponedRefresh())+"'.");
//					refresh = false;
//				}
//
//				return refresh;
//			}
//		};
//
//		_cmLocks.setTabPanel(pdf.processLocksPanel);
//
//		if (_spid > 0 || _kpid > 0)
//			_cmLocks.setSqlWhere(whereSpidKpid);
//	}

//	private List<String> _activeRoleList    = null;
//	private boolean      _isInitialized     = false;
//	private boolean      _countersIsCreated = false;
//
//	public void initCounters(Connection conn, boolean hasGui, long srvVersion, boolean isClusterEnabled, long monTablesVersion)
//	throws Exception
//	{
//		if (_isInitialized)
//			return;
//
//		if ( ! AseConnectionUtils.isConnectionOk(conn, hasGui, null))
//			throw new Exception("Trying to initialize the counters with a connection this seems to be broken.");
//
//			
//		if (! _countersIsCreated)
//			createCounters();
//		
//		_logger.info("Capture SQL; Initializing all CM objects, using ASE server version number "+srvVersion+", isClusterEnabled="+isClusterEnabled+" with monTables Install version "+monTablesVersion+".");
//
//		// Get active ASE Roles
//		//List<String> activeRoleList = AseConnectionUtils.getActiveRoles(conn);
//		_activeRoleList = AseConnectionUtils.getActiveSystemRoles(conn);
//		
//		// Get active Monitor Configuration
//		Map<String,Integer> monitorConfigMap = AseConnectionUtils.getMonitorConfigs(conn);
//
//		// in version 15.0.3.1 compatibility_mode was introduced, this to use 12.5.4 optimizer & exec engine
//		// This will hurt performance, especially when querying sysmonitors table, so set this to off
//		if (srvVersion >= Ver.ver(15,0,3,1))
//			AseConnectionUtils.setCompatibilityMode(conn, false);
//
//		ArrayList<CountersModel> CMList = new ArrayList<CountersModel>();
//		CMList.add(_cmProcWaits);
//		CMList.add(_cmProcObjects);
//		CMList.add(_cmLocks);
//
//Create Some Cind of CounterController and set that at the CM level, this so that the Global used in AseTune if called from there isn't messed up...
//		// initialize all the CM's
//		for (CountersModel cm : CMList)
//		{
//			if (cm != null)
//			{
//				_logger.debug("Initializing CM named '"+cm.getName()+"', display name '"+cm.getDisplayName()+"', using ASE server version number "+srvVersion+".");
//
//				// set the version
////				cm.setServerVersion(srvVersion);
//				cm.setServerVersion(monTablesVersion);
//				cm.setClusterEnabled(isClusterEnabled);
//				
//				// set the active roles, so it can be used in initSql()
//				cm.setActiveServerRolesOrPermissions(_activeRoleList);
//
//				// set the ASE Monitor Configuration, so it can be used in initSql() and elsewhere
//				cm.setMonitorConfigs(monitorConfigMap);
//
//				// Now when we are connected to a server, and properties are set in the CM, 
//				// mark it as runtime initialized (or late initialization)
//				// do NOT do this at the end of this method, because some of the above stuff
//				// will be used by the below method calls.
//				cm.setRuntimeInitialized(true);
//
//				// Initializes SQL, use getServerVersion to check what we are connected to.
//				cm.initSql(conn);
//
//				// Use this method if we need to check anything in the database.
//				// for example "deadlock pipe" may not be active...
//				// If server version is below 15.0.2 statement cache info should not be VISABLE
//				cm.init(conn);
//			}
//		}
//			
//		_isInitialized = true;
//	}

	private void refreshAseMonitorConfigs()
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
			ResultSet rs = stmt.executeQuery("sp_activeroles 'expand_down'");
			while (rs.next())
			{
				if (rs.getString(1).equals("sa_role"))
					has_sa_role = true;
			}
			rs.close();
		}
		catch (SQLException SQLEx)
		{
			_logger.error(Version.getAppName()+" : error in refreshProcess getting options. ", SQLEx);
			// SQLEx.printStackTrace();
		}
	}

	@Override
	public void run()
	{
		refreshAseMonitorConfigs();
		
		// Display warning if all configuration parameters are not set
		StringBuilder             msg = new StringBuilder();
		if (!stmtStat)            msg = msg.append("<li>'statement statistics active' to 1 </li>" );
		if (!batchCapture)        msg = msg.append("<li>'SQL batch capture' to 1 </li>");
		if (!SQLTextMonitor)      msg = msg.append("<li>'max SQL text monitored' to a value greater than 0 (ex : 4096) </li>");
		if (sqlTextSample) // Only if the checkbox is true
		{
			if (!sqlTextPipe)     msg = msg.append("<li>'sql text pipe active' to 1 </li>");
			if (!sqlTextPipeMsg)  msg = msg.append("<li>'sql text pipe max messages' to a value greater than 0 </li>");
		}
		if (!stmtPipe)            msg = msg.append("<li>'statement pipe active' to 1 </li>");
		if (!stmtPipeMsg)         msg = msg.append("<li>'statement pipe max messages' to a value greater than 0 </li>");
		if (planTextSample) // Only if the checkbox is true
		{
			if (!planTextPipe)    msg = msg.append("<li>'plan text pipe active' to 1 </li>");
			if (!planTextPipeMsg) msg = msg.append("<li>'plan text pipe max messages' to a value greater than 0 </li>");
		}
		if (!procWaitEvents)      msg = msg.append("<li>'process wait events' to 1 </li>");

		if (!has_sa_role)         msg = msg.append("<li>user should have 'sa_role'</li>");

		if (msg.length() > 0)
		{
			msg.insert(0, "<html><h3>RECOMMENDATION: for full features</h3>\n Configure the following parameters\n <ul>");
			msg = msg.append("</ul><br>");
			msg = msg.append("A window will be opened where you can configure the above parameters.");
			msg = msg.append("</html>");
//			msg = msg.append("\n\nConfigure the ASE and re-open the window.\n");

			// GUI popup with the error, AND a JCheckBox to "not show this message again"
			boolean showInfo = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_showDialogMdaConfig, DEFAULT_showDialogMdaConfig);
			if (showInfo)
			{
				// Create a check box that will be passed to the message
				JCheckBox chk = new JCheckBox("Show this dialog in the future.", showInfo);
				chk.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
						if (conf == null)
							return;
						conf.setProperty(PROPKEY_showDialogMdaConfig, ((JCheckBox)e.getSource()).isSelected());
						conf.save();
					}
				});

				SwingUtils.showInfoMessageExt(pdf, Version.getAppName()+" - SPID monitoring",
						msg.toString(), chk, (JPanel)null);

				AseConfigMonitoringDialog.showDialog(pdf, _conn, -1, true);

				// Refresh this again...
				refreshAseMonitorConfigs();
			}
		}

		// Check if "sp_showplanfull" exists
		if (has_sa_role) 
		{
			try 
			{
				ResultSet rs=stmt.executeQuery("select name from sybsystemprocs..sysobjects where name ='sp_showplanfull' and type='P'");
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
						"        print 'No plan for _spid %1!', @spid   \n" +
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
						"select @procname=isnull(object_name(id,dbid), object_name(id,2)), @procid=id, @dbid=dbid from master..sysprocesses where spid=@spid   \n" +
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
				rs.close();
			}
			catch (SQLException sqlex)
			{
				_logger.error("When creating stored proc 'sp_showplanfull'.", sqlex);
			}
		}

//		try
//		{
    		_srvVersion       = MonTablesDictionaryManager.getInstance().getDbmsExecutableVersionNum();
    		_isClusterEnabled = MonTablesDictionaryManager.getInstance().isClusterEnabled();
    		_monTablesVersion = MonTablesDictionaryManager.getInstance().getDbmsMonTableVersion();
//		}
//		catch(RuntimeException rte) // if MonTablesDictionary hasn't been initialized... java.lang.RuntimeException: No MonTablesDictionary has been set...
//		{
//    		_srvVersion       = _conn.getDbmsVersionNumber();
//    		_isClusterEnabled = false; // _conn.isClusterEnabled();
//    		_monTablesVersion = _srvVersion; // hopefully good enough
//		}

//		try
//		{
//			initCounters(_conn, true, _srvVersion, _isClusterEnabled, _monTablesVersion);
//		}
//		catch (Exception ex)
//		{
//			SwingUtils.showErrorMessage("Problems Initalize CM's", 
//					"Problems when initializing some of the Performance Counters", ex);
//			return;
//		}
		try
		{
			CounterControllerSqlCapture counterController = new CounterControllerSqlCapture(true, this, pdf);
			counterController.setMonConnection(_conn);
			counterController.initCounters(_conn, true, _srvVersion, _isClusterEnabled, _monTablesVersion);
			
			String whereSpidKpid = "";
			if (_spid > 0) whereSpidKpid += "  and SPID = " + _spid + "\n";
			if (_kpid > 0) whereSpidKpid += "  and KPID = " + _kpid + "\n";

			_cmProcObjects = counterController.getCmByName(CmProcessObjects.CM_NAME);
			_cmProcWaits   = counterController.getCmByName(CmProcessWaits  .CM_NAME);
			_cmLocks       = counterController.getCmByName(CmLocks         .CM_NAME);
			
//			_cmProcObjects.setTabPanel(pdf.processObjectsPanel);
//			pdf.mainTabbedPanel.add(_cmProcObjects.getTabPanel(), _cmProcObjects.getDisplayName());
			if (_spid > 0 || _kpid > 0)
				_cmProcObjects.setSqlWhere(whereSpidKpid);

//			_cmProcWaits.setTabPanel(pdf.processWaitsPanel);
//			pdf.mainTabbedPanel.add(_cmProcWaits.getTabPanel(), _cmProcWaits.getDisplayName());
			if (_spid > 0 || _kpid > 0)
				_cmProcWaits.setSqlWhere(whereSpidKpid);

//			_cmLocks.setTabPanel(pdf.processLocksPanel);
//			pdf.mainTabbedPanel.add(_cmLocks.getTabPanel(), _cmLocks.getDisplayName());
			if (_spid > 0 || _kpid > 0)
				_cmLocks.setSqlWhere(whereSpidKpid);

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
				pdf.setStatusBar("Paused", false);
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
		pdf.setStatusBar("Process disconnected", false);
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
			pdf.refreshBegin();
			refreshStmt();

			if (true)           _cmProcObjects.getTabPanel().setWatermark();
			if (procWaitEvents) _cmProcWaits  .getTabPanel().setWatermark();
			if (true)           _cmLocks      .getTabPanel().setWatermark();

			if (_spid > 0 || _kpid > 0)
			{
				// Refresh process objects
				if (_cmProcObjects.isRefreshable())
					_cmProcObjects.refresh();
	
				// Refresh process waits
				if (procWaitEvents && _cmProcWaits.isRefreshable())
					_cmProcWaits.refresh();
	
				// Refresh locks
				if (_cmLocks.isRefreshable())
					_cmLocks.refresh();
			}
			else
			{
				if (_activeBatch != null)
				{
					pdf.kpidFld.setText(_activeBatch.kpid+"");
					pdf.kpidFld.setForeground(Color.BLUE);

					pdf.spidFld.setText(_activeBatch.spid+"");
					pdf.spidFld.setForeground(Color.BLUE);

					if (true)           _cmProcObjects.setSqlWhere(" and KPID = " + _activeBatch.kpid);
					if (procWaitEvents) _cmProcWaits  .setSqlWhere(" and KPID = " + _activeBatch.kpid);
					if (true)           _cmLocks      .setSqlWhere(" and KPID = " + _activeBatch.kpid);

					if (                  _cmProcObjects.isRefreshable()) _cmProcObjects.refresh();
					if (procWaitEvents && _cmProcWaits  .isRefreshable()) _cmProcWaits  .refresh();
					if (                  _cmLocks      .isRefreshable()) _cmLocks      .refresh();
				}
				else
				{
					if (true)           _cmProcObjects.clear();
					if (procWaitEvents) _cmProcWaits  .clear();
					if (true)           _cmLocks      .clear();

					String watermark = "No Active SQL Statement is executing, the tab \"Active Statements\" data table is empty.";
					if (true)           _cmProcObjects.getTabPanel().setWatermarkText(watermark);
					if (procWaitEvents) _cmProcWaits  .getTabPanel().setWatermarkText(watermark);
					if (true)           _cmLocks      .getTabPanel().setWatermarkText(watermark);
					
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
			pdf.refreshEnd();
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

		@Override
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

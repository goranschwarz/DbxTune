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
package com.asetune.cm.iq;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.DbxTune;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CounterTableModel;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.hostmon.HostMonitorMetaData;
import com.asetune.hostmon.OsTable;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.IqConnection;
import com.asetune.ssh.SshConnection;
import com.asetune.utils.FileTail;


public class CmIqMsgLogParser 
extends CountersModel
implements FileTail.TraceListener
{
	/** Log4j logging. */
	private static Logger	   _logger	          = Logger.getLogger(CmIqMsgLogParser.class);
	private static final long	serialVersionUID	= 1L;

	public static final String   CM_NAME          = CmIqMsgLogParser.class.getSimpleName();
	public static final String   SHORT_NAME       = "IQ Msg File Parser";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Parses the IQ Message Log to extract valubale information.<br>" +
		"If the server is located on a Unix or Linux server, then a SSH connection is used to remote 'tail' the file.<br>" +
		"Note: if it's a remote file, you need to enable <b><i>OS Host Monitoring</i></b> when connecting.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
//	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
//	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.LARGE; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmIqMsgLogParser(counterController, guiController);
	}

	public CmIqMsgLogParser(ICounterController counterController, IGuiController guiController)
	{
//		super(CM_NAME, GROUP_NAME, CM_TYPE, null, true);
//		super(CM_NAME, GROUP_NAME, null, null, );

		this(CM_NAME, GROUP_NAME, true);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);
		
		setCounterController(counterController);
		setGuiController(guiController);
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmIqMsgLogParserPanel(this);
//	}

	private Timestamp   _thisSamplingTime = null;

//	private HostMonitor _hostMonitor   = null;
	private OsTable     _osSampleTable = null;
//	private int         _hostMonType   = 0;

//	public static final int HOSTMON_IOSTAT   = 1; 
//	public static final int HOSTMON_VMSTAT   = 2; 
//	public static final int HOSTMON_MPSTAT   = 3; 
//	public static final int HOSTMON_UPTIME   = 4; 
//	public static final int HOSTMON_UD_CLASS = 5;
//	private String _udModuleName = null;
//
//	public static String getTypeExplanation(int type)
//	{
//		if (type == HOSTMON_IOSTAT)   return "HOSTMON_IOSTAT";
//		if (type == HOSTMON_VMSTAT)   return "HOSTMON_VMSTAT";
//		if (type == HOSTMON_MPSTAT)   return "HOSTMON_MPSTAT";
//		if (type == HOSTMON_UPTIME)   return "HOSTMON_UPTIME";
//		if (type == HOSTMON_UD_CLASS) return "HOSTMON_UD_CLASS";
//		return "UNKNOWN TYPE("+type+")";
//	}
	//----------------------------------------------------------------------------
	// BEGIN: Constructors
	//----------------------------------------------------------------------------
	public CmIqMsgLogParser(String name, String groupName, boolean systemCm)
	{
		super.setName(name);
		super.setGroupName(groupName);
		super.setSystemCm(systemCm);

//		_udModuleName = udModuleName;
//		_hostMonType = type;
		
		// Check if name is OK
		checkInConstructor();

		setDataSource(DATA_ABS, false);

		// Load saved properties
		super.loadProps();

		setDataInitialized(false);
	}
	//----------------------------------------------------------------------------
	// END: Constructors
	//----------------------------------------------------------------------------

	/** called from GetCounters.initCounters() */
	@Override
	public void init(Connection conn)
	throws Exception
	{
		localInit();
	}

	/** If we need to restart the tail, do not accept records which we have already received */
	private Timestamp _lastLogTsReceived = null;

	private String  _iqMsgFile = null;
	private boolean _iqMsgFileChanged = false;
	
	private void setIqMsgFile(String name)
	{
		if (name.equals(_iqMsgFile))
		{
			_iqMsgFileChanged = false;
			return;
		}
System.out.println("NOTE: iqMsgFile was changed from '"+_iqMsgFile+"' to '"+name+"'.");
		_iqMsgFileChanged = true;
		_iqMsgFile        = name;
	}
	private String getIqMsgFilename()
	{
		return _iqMsgFile;
	}

	private String  _iqMsgFilePlatform = null;
	private void setPlatform(String platform)
	{
		_iqMsgFilePlatform = platform;
	}

	private boolean _localInit = false;
	private void localInit()
	throws Exception
	{
		if (CounterController.getInstance().isMonConnected())
		{
			DbxConnection dbxConn = CounterController.getInstance().getMonConnection();
			
			if (dbxConn instanceof IqConnection)
			{
				setPlatform(  ((IqConnection)dbxConn).getPlatform() );
				setIqMsgFile( ((IqConnection)dbxConn).getIqMsgFilname() );
				
				startTail();
				_localInit = true;
			}
			else
			{
				throw new Exception("The passed connection is NOT an IqConnection. dbxConn="+dbxConn);
			}
		}

		if (CounterController.getInstance().isHostMonConnected())
		{
			SshConnection sshConn = CounterController.getInstance().getHostMonConnection();
			DbxConnection dbxConn = CounterController.getInstance().getMonConnection();
			
			if (dbxConn instanceof IqConnection)
			{
				setPlatform(  ((IqConnection)dbxConn).getPlatform() );
				setIqMsgFile( ((IqConnection)dbxConn).getIqMsgFilname() );
			}
			else
			{
				throw new Exception("The passed connection is NOT an IqConnection. dbxConn="+dbxConn);
			}

			//System.out.println("CmIqMsgLogParser.init(sqlConn): CREATING MONITOR: "+_hostMonType);

//			// FIXME: Hmm do this in a BETTER way, factoring needs to much maintenance,
//			// it would be better to use some kind of class loading, or similar...
//			if      (_hostMonType == HOSTMON_IOSTAT) _hostMonitor = MonitorIo    .createMonitor(sshConn, false);
//			else if (_hostMonType == HOSTMON_VMSTAT) _hostMonitor = MonitorVmstat.createMonitor(sshConn, false);
//			else if (_hostMonType == HOSTMON_MPSTAT) _hostMonitor = MonitorMpstat.createMonitor(sshConn, false);
//			else if (_hostMonType == HOSTMON_UPTIME) _hostMonitor = MonitorUpTime.createMonitor(sshConn, false);
//			else if (_hostMonType == HOSTMON_UD_CLASS)
//			{
//				Configuration conf = null;
//				if(DbxTune.hasGui())
//					conf = Configuration.getCombinedConfiguration();
//				else
//					conf = Configuration.getInstance(Configuration.PCS);
//
//				if (conf == null)
//					throw new Exception("No Configuration can be found.");
//
//				_hostMonitor = new MonitorUserDefined(conf, _udModuleName, sshConn, false);
//			}
//			else
//				throw new Exception("Unsupported HOSTMON_TYPE of '"+_hostMonType+"'.");

			// set this property so we can reach this object from the TabularControlPanel
//			setClientProperty(HostMonitor.PROPERTY_NAME, _hostMonitor);

			_localInit = true;
		}
	}

	/*---------------------------------------------------
	** BEGIN: implementing FileTail.TraceListener
	**---------------------------------------------------
	*/
	@Override
	public void newTraceRow(String row)
	{
		System.out.println("newTraceRow(): |"+row+"|.");
	}
	/*---------------------------------------------------
	** END: implementing FileTail.TraceListener
	**---------------------------------------------------
	*/
	private FileTail _fileTail      = null;
	private boolean  _tailFromStart = false;
	private int      _tailFromRow   = 10;

	/** Stop all tracing and disconnect SSH connection if any */
	public void stopTail()
	{
		if (_fileTail != null)
		{
			_fileTail.shutdown();

			_fileTail.waitForShutdownToComplete();
			_fileTail.removeTraceListener(this);

			_fileTail = null;
		}
	}

	/** 
	 * START a file tail session<br>
	 * This involves
	 * <ul>
	 *   <li>Connect to the SSH Server if remote tail</li>
	 *   <li>do 'tail' on the file. (this is done via SSH or a local file location)</li>
	 * </ul>
	 */
	public boolean startTail()
	{
		boolean doShh = false;
		SshConnection sshConn = CounterController.getInstance().getHostMonConnection();

		if (doShh)
		{
			// Start the TAIL on the file...
			if (_tailFromStart)
				_fileTail = new FileTail(sshConn, getIqMsgFilename(), true);
			else
				_fileTail = new FileTail(sshConn, getIqMsgFilename(), _tailFromRow);

			if (_fileTail.doFileExist())
			{
				_fileTail.addTraceListener(this);
				_fileTail.start();
			}
			else
			{
				String msg = "The trace file '"+_fileTail.getFilename()+"' was not found. (SSH Access mode)";
				_logger.error(msg);
//				SwingUtils.showErrorMessage("Trace file not found", msg, null);
				stopTail();
				return false;
			}
		}
		else
		{
			// Start the TAIL on the file...
			if (_tailFromStart)
				_fileTail = new FileTail(getIqMsgFilename(), true);
			else
				_fileTail = new FileTail(getIqMsgFilename(), _tailFromRow);

			if (_fileTail.doFileExist())
			{
				_fileTail.addTraceListener(this);
				_fileTail.start();
			}
			else
			{
				String msg = "The trace file '"+_fileTail.getFilename()+"' was not found. (Local Access mode)";
				_logger.error(msg);
//				SwingUtils.showErrorMessage("Trace file not found", msg, null);
				stopTail();
				return false;
			}
		}

		return true;
	}

	/*---------------------------------------------------
	** BEGIN: implementing TableModel or overriding CounterModel/AbstractTableModel
	**---------------------------------------------------
	*/
	@Override
	public int getColumnCount()
	{
		if (_offlineValues != null) 
			return _offlineValues.getColumnCount();

		int c = 0;
		if (isDataInitialized() && _osSampleTable != null)
			c = _osSampleTable.getColumnCount();
		return c;
    }

	@Override
	public String getColumnName(int col)
	{
		if (_offlineValues != null) 
			return _offlineValues.getColumnName(col); 

		if (isDataInitialized() && _osSampleTable != null)
			return _osSampleTable.getColumnName(col); 
		return null;
	}

	@Override
	public int getRowCount()
	{
		if (_offlineValues != null) 
			return _offlineValues.getRowCount();

		int c = 0;
		if (isDataInitialized() && _osSampleTable != null)
			c = _osSampleTable.getRowCount();
		return c;
    }

	@Override
	public Object getValueAt(int row, int col)
	{
		if (_offlineValues != null) 
			return _offlineValues.getValueAt(row, col);

		if (isDataInitialized() && _osSampleTable != null)
			return _osSampleTable.getValueAt(row, col);
		return null;
    }

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return false;
    }

	@Override
	public int findColumn(String colName)
	{
		if (_offlineValues != null) 
			return _offlineValues.findColumn(colName);

		if (isDataInitialized() && _osSampleTable != null)
			return _osSampleTable.findColumn(colName);
		return -1;
	}
	/*---------------------------------------------------
	** END: implementing TableModel or overriding CounterModel/AbstractTableModel
	**---------------------------------------------------
	*/

	
	
	/*---------------------------------------------------
	** BEGIN: used by the Persistent Writer and Reader
	**---------------------------------------------------
	*/
	// FIXME: the below is used in PCS(JDBC STORAGE), maybe change PCS to use TableModel instead 
	@Override
	public List<List<Object>> getDataCollection(int whatData)
	{
		if (_osSampleTable == null)
			return null;

		List<List<Object>> table = new ArrayList<List<Object>>();
		int rowCount = _osSampleTable.getRowCount();
		int colCount = _osSampleTable.getColumnCount();

		for (int r=0; r<rowCount; r++)
		{
			List<Object> row = new ArrayList<Object>();
			for (int c=0; c<colCount; c++)
				row.add(_osSampleTable.getValueAt(r, c));
			table.add(row);
		}


		return table;
	}

	// FIXME: the below is used in PCS(JDBC STORAGE), maybe change PCS to use TableModel instead 
	@Override
	public synchronized List<String> getColNames(int whatData)
	{
		if (_osSampleTable == null)
			return null;

		List<String> data = new ArrayList<String>();
		int colCount = _osSampleTable.getColumnCount();

		for (int c=0; c<colCount; c++)
			data.add(_osSampleTable.getColumnName(c));

		return data;
	}



	/** Used to hold offline data */
	private DefaultTableModel     _offlineValues      = null;
	private HostMonitorMetaData   _offlineMetadata    = null;
	private HostMonitorMetaData[] _offlineMetadataArr = null;

//	/** 
//	 * Used to set off-line counter data 
//	 */
//	@Override
//	public void setOfflineColumnNames(int type, List<String> cols)
//	{
////		System.out.println("setColumnNames(): type="+type+", cols='"+cols+"'.");
//		if (_offlineMetadataArr == null)
//		{
//			if      (_hostMonType == HOSTMON_IOSTAT)   _offlineMetadataArr = MonitorIo    .createOfflineMetaData();
//			else if (_hostMonType == HOSTMON_VMSTAT)   _offlineMetadataArr = MonitorVmstat.createOfflineMetaData();
//			else if (_hostMonType == HOSTMON_MPSTAT)   _offlineMetadataArr = MonitorMpstat.createOfflineMetaData();
//			else if (_hostMonType == HOSTMON_UPTIME)   _offlineMetadataArr = MonitorUpTime.createOfflineMetaData();
//			else if (_hostMonType == HOSTMON_UD_CLASS) _offlineMetadataArr = MonitorUserDefined.createOfflineMetaData(_udModuleName);
//	
//			_offlineMetadata = null;
//			for (HostMonitorMetaData md : _offlineMetadataArr)
//			{
//				if ( cols.containsAll(md.getColumnNames()) )
//					_offlineMetadata = md;
//			}
//			if (_offlineMetadata != null)
//				_logger.info("Setting Offline Meta Data to use '"+_offlineMetadata.getTableName()+"'.");
//			else
//				_logger.info("Can't find a Offline Meta Data structure for columns: "+cols);
//		}
//		_offlineValues = new DefaultTableModel(new Vector<String>(cols), 0);
//	}

	/** 
	 * Used to set off-line counter data 
	 */
	@Override
	public void setOfflineValueAt(int type, Object value, int row, int col)
	{
//		System.out.println("setValueAt(type='"+type+"', value='"+value+"', row='"+row+"', col='"+col+"').");
		if (type != DATA_ABS)
			return;
		if (_offlineValues == null) 
			return;

		if ( row >= _offlineValues.getRowCount())
			_offlineValues.setRowCount(row+1);
		_offlineValues.setValueAt(value, row, col);
	}
	/*---------------------------------------------------
	** END: used by the Persistent Writer and Reader
	**---------------------------------------------------
	*/

	
	@Override
	public String getToolTipTextOnTableColumnHeader(String colname)
	{
		if (_offlineMetadata != null)
			return _offlineMetadata.getDescription(colname);

		if (_osSampleTable != null)
			return _osSampleTable.getMetaData().getDescription(colname);

		return super.getToolTipTextOnTableColumnHeader(colname);
	}

	@Override public boolean discardDiffHighlighterOnAbsTable() { return false; }
	@Override public boolean discardPctHighlighterOnAbsTable()  { return false; }
	@Override public boolean isDiffCalcEnabled() { return false; }
	@Override public boolean isDiffColumn(int index) { return false; }
	@Override public boolean isPctColumn (int index) 
	{
		// MetaData starts a 1, while the index here starts at 0 for col1
		int column = index + 1;

		if (_offlineMetadata != null)
			return _offlineMetadata.isPctColumn(column);

		if (_osSampleTable != null)
			return _osSampleTable.getMetaData().isPctColumn(column);

		return false;
	}

	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
	}
	
	@Override public CounterTableModel getCounterDataAbs()  { return _osSampleTable; }
	@Override public CounterTableModel getCounterDataDiff() { return null; }
	@Override public CounterTableModel getCounterDataRate() { return null; }

	@Override public boolean hasAbsData()  { return _osSampleTable != null; }
	@Override public boolean hasDiffData() { return false; }
	@Override public boolean hasRateData() { return false; }

//	@Override public void    setPersistCountersAbs(boolean b) {}
	@Override public boolean isPersistCountersAbsEnabled() { return true; }
	@Override public boolean isPersistCountersAbsEditable(){ return false; }
	
//	@Override public void    setPersistCountersDiff(boolean b) {}
	@Override public boolean isPersistCountersDiffEnabled() { return false; }
	@Override public boolean isPersistCountersDiffEditable(){ return false; }
	
//	@Override public void    setPersistCountersRate(boolean b) {}
	@Override public boolean isPersistCountersRateEnabled() { return false; }
	@Override public boolean isPersistCountersRateEditable(){ return false; }

	@Override public boolean getDefaultIsPersistCountersEnabled()     { return false; }
	@Override public boolean getDefaultIsPersistCountersAbsEnabled()  { return true; }
	@Override public boolean getDefaultIsPersistCountersDiffEnabled() { return false; }
	@Override public boolean getDefaultIsPersistCountersRateEnabled() { return false; }
	
	@Override public synchronized Timestamp getTimestamp() { return _thisSamplingTime; }

	@Override
	protected int refreshGetData(DbxConnection conn) 
	throws Exception
	{
System.out.println("Entering refresh() method for " + getName());

		if (_logger.isDebugEnabled())
			_logger.debug("Entering refresh() method for " + getName());

		if ( ! _localInit )
			localInit();

		if ( _fileTail == null )
			return -1;

//		if ( _hostMonitor == null )
//			return -1;

//		if ( _hostMonitor.isPaused() )
//		{
//			_logger.info("The Host Monitor module '"+_hostMonitor.getModuleName()+"' is paused.");
//			// do NOT return here, _hostMonitor.getSummaryTable() will return an empty, 0 row result...
//		}

		if (CounterController.getInstance().isMonConnected())
		{
			DbxConnection dbxConn = CounterController.getInstance().getMonConnection();
			if (dbxConn instanceof IqConnection)
				setIqMsgFile( ((IqConnection)dbxConn).getIqMsgFilname() );
			else
				throw new Exception("The passed connection is NOT an IqConnection. dbxConn="+dbxConn);
		}
		if (_iqMsgFileChanged)
		{
			System.out.println("DO SOMETHING: the iqMsgFile has been changed.");
			stopTail();
			startTail();
		}


		
//		if ( ! _hostMonitor.isRunning() && ! _hostMonitor.isPaused() )
//		{
//			if ( _hostMonitor.isOsCommandStreaming() )
//				_hostMonitor.start();
//			// do NOT return here, _hostMonitor.getSummaryTable() will return an empty, 0 row result...
//		}

		// GET THE DATA FROM THE HOST MONITOR
//		OsTable tmpOsSampleTable;
//		if (_hostMonitor.isOsCommandStreaming())
//			tmpOsSampleTable = _hostMonitor.getSummaryTable();
//		else
//			tmpOsSampleTable = _hostMonitor.executeAndParse();
//
//		if (_logger.isDebugEnabled())
//			_logger.debug("OUTPUT Table from '"+_hostMonitor.getModuleName()+"':\n" + tmpOsSampleTable.toTableString());

//		// set MetaData: otherwise the Persistent Counter Storage can't create tables
//		setResultSetMetaData(tmpOsSampleTable.getMetaData());

//		// If the HostMonitor has problems, relay them to the GUI
//		Exception exception = _hostMonitor.getException();
//		if (exception != null)
//			setSampleException(exception);
//			//throw exception;

//		int rowsFetched = tmpOsSampleTable.getRowCount();

		setDataInitialized(true);

		// Update dates on panel
		_thisSamplingTime = new Timestamp(System.currentTimeMillis());
		setSampleTime( _thisSamplingTime );
//		setSampleInterval(tmpOsSampleTable.getSampleSpanTime());

		// NOW apply data to the VIEW
		// in GUI mode this is done preferred by the EventDispathThread, thats why we
		// need to use the temp variable tmpOsSampleTable, which is set to _osSampleTable...
		if ( ! DbxTune.hasGui() )
		{
//			// NO GUI, move structures
//			_osSampleTable = tmpOsSampleTable;
//
//			// if it's the first time sampling...
//			if ( ! isInitialized() )
//			{
//				_logger.debug(getName()+":------doFireTableStructureChanged------");
//				fireTableStructureChanged();
//
//				setInitialized(true);
//			}
//
//			// Calculte what values we should have in the graphs
//			// this has to be after _prevSample, _newSample, _diffData, _rateData has been SET
//			updateGraphData();
		}
		else // HAS GUI
		{
//			// Make them final copies to be used in the doWork/Runnable below
//			final OsTable fTmpOsSampleTable = tmpOsSampleTable;
//
//			Runnable doWork = new Runnable()
//			{
//				@Override
//				public void run()
//				{
//					// IMPORTANT: move datastructure.
//					_osSampleTable = fTmpOsSampleTable;
//
//					beginGuiRefresh();
//
//					if ( ! isInitialized() )
//					{
//						_logger.debug(getName()+":------doFireTableStructureChanged------");
//						fireTableStructureChanged();
//
//						setInitialized(true);
//					}
//
//					if (getTabPanel() != null && !getTabPanel().isTableInitialized())
//					{
//						_logger.debug(getName()+":------doFireTableStructureChanged------");
//						fireTableStructureChanged();
//						getTabPanel().adjustTableColumnWidth();
//					}
//					else
//					{
//						_logger.debug(getName()+":-fireTableDataChanged-");
//						fireTableDataChanged();
//					}
//
//					// Calculte what values we should have in the graphs
//					// this has to be after _prevSample, _newSample, _diffData, _rateData has been SET
//					// since we do this differred in case we use Swing, it has to be done here.
//					updateGraphData();
//
//					endGuiRefresh();
//				}
//			};
//			// Invoke this job on the SWING Event Dispather Thread
//			if ( ! SwingUtilities.isEventDispatchThread() )
//				SwingUtilities.invokeLater(doWork);
//			else
//				doWork.run();
		}
		
//		return rowsFetched;
		return 1;
	}

	
	/**
	 * SQL is not used here, so simply override this method with "nothing"
	 */
	@Override
	public void initSql(Connection conn)
	{
	}

	@Override
	public void clearForRead()
	{
	}

	@Override
	public void close()
	{
		_localInit = false;
		stopTail();
//		if (_hostMonitor != null)
//			_hostMonitor.shutdown();
//		_hostMonitor = null;
//
//		// set this property so we can reach this object from the TabularControlPanel
//		setClientProperty(HostMonitor.PROPERTY_NAME, _hostMonitor);
	}

	@Override
	public void clear()
	{
		clear(100);
	}
	@Override
	public synchronized void clear(int clearLevel)
	{
		setInitialized(false);

		_offlineValues      = null;
		_offlineMetadata    = null;
		_offlineMetadataArr = null;

		setValidSampleData(false);
		_osSampleTable = null;
		_thisSamplingTime = null;

		setTimeInfo(null, null, null, 0);

		// Clear dates on panel
		TabularCntrPanel tabPanel = getTabPanel();
		if (tabPanel != null)
			tabPanel.reset();

//		System.out.println(_name+":------doFireTableStructureChanged------");
		fireTableStructureChanged();
	}

//	/**
//	 * Get basic configuration "html" chars/tags is accepted.<br>
//	 * This would be overrided by modules that does not support Properties by ShowCmPropertiesDialog
//	 * @return
//	 */
//	@Override
//	public String getBasicConfigurationDescription()
//	{
//		String ret = 
//			"Host Monitor has no specific properties.<br>" +
//			"Please view executed command on the right hand side of the Option Panel<br>" +
//			"<br>" +
//			"hostMonType="+getTypeExplanation(_hostMonType)+".<br>";
//
//		if (_hostMonType == HOSTMON_UD_CLASS)
//			ret += "udModuleName='"+_udModuleName+"'<br>";
//
//		if (_hostMonitor != null)
//		{
//			ret += "<br>";
//			ret += "Some Basic info:<br>";
//			ret += "getName: "     + getName()                           + "<br>";
//			ret += "getCommand: "  + _hostMonitor.getCommand()           + "<br>";
//			ret += "getSleepTime: "+ _hostMonitor.getSleepTime()         + "<br>";
//			ret += "isPaused: "    + _hostMonitor.isPaused()             + "<br>";
//			ret += "isRunning: "   + _hostMonitor.isRunning()            + "<br>";
//			ret += "isStreaming: " + _hostMonitor.isOsCommandStreaming() + "<br>";
//		}
//
//		return ret;
//	}
}

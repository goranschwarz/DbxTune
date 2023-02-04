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
package com.asetune.cm;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.DbxTune;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.hostmon.HostMonitor;
import com.asetune.hostmon.HostMonitorConnection;
import com.asetune.hostmon.HostMonitorMetaData;
import com.asetune.hostmon.MonitorDiskSpace;
import com.asetune.hostmon.MonitorIo;
import com.asetune.hostmon.MonitorMeminfo;
import com.asetune.hostmon.MonitorMpstat;
import com.asetune.hostmon.MonitorNwInfo;
import com.asetune.hostmon.MonitorPs;
import com.asetune.hostmon.MonitorUpTime;
import com.asetune.hostmon.MonitorUserDefined;
import com.asetune.hostmon.MonitorVmstat;
import com.asetune.hostmon.OsTable;
import com.asetune.hostmon.OsTableRow;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.ssh.SshConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.NumberUtils;


public class CounterModelHostMonitor 
extends CountersModel
{
	/** Log4j logging. */
	private static Logger	   _logger	          = Logger.getLogger(CounterModelHostMonitor.class);
	private static final long	serialVersionUID	= 1L;

	private Timestamp   _thisSamplingTime = null;

	private int         _hostMonType   = 0;
	private HostMonitor _hostMonitor   = null;
	private OsTable     _osSampleTable = null;
//	private OsTable     _prevSample    = null;  // Contains old raw data (previous sample)                      
//	private OsTable     _newSample     = null;  // Contains new raw data                                        
	private OsTable     _diffData      = null;  // diff between newSample and oldSample data (not filtered)    
	private OsTable     _rateData      = null;  // diffData / sampleInterval                                   

//	private OsTable     _osPrevSampleTable = null; // Contains old raw data (previous sample), but only for non-streaming-modules

	public static final int HOSTMON_IOSTAT    = 1; 
	public static final int HOSTMON_VMSTAT    = 2; 
	public static final int HOSTMON_MPSTAT    = 3; 
	public static final int HOSTMON_UPTIME    = 4; 
	public static final int HOSTMON_MEMINFO   = 5;
	public static final int HOSTMON_NWINFO    = 6;
	public static final int HOSTMON_DISKSPACE = 7;
	public static final int HOSTMON_PS        = 8;
	public static final int HOSTMON_UD_CLASS  = 9;
	private String _udModuleName = null;

	public static String getTypeExplanation(int type)
	{
		if (type == HOSTMON_IOSTAT)    return "HOSTMON_IOSTAT";
		if (type == HOSTMON_VMSTAT)    return "HOSTMON_VMSTAT";
		if (type == HOSTMON_MPSTAT)    return "HOSTMON_MPSTAT";
		if (type == HOSTMON_UPTIME)    return "HOSTMON_UPTIME";
		if (type == HOSTMON_MEMINFO)   return "HOSTMON_MEMINFO";
		if (type == HOSTMON_NWINFO)    return "HOSTMON_NWINFO";
		if (type == HOSTMON_DISKSPACE) return "HOSTMON_DISKSPACE";
		if (type == HOSTMON_PS)        return "HOSTMON_PS";
		if (type == HOSTMON_UD_CLASS)  return "HOSTMON_UD_CLASS";
		return "UNKNOWN TYPE("+type+")";
	}
	//----------------------------------------------------------------------------
	// BEGIN: Constructors
	//----------------------------------------------------------------------------
	public CounterModelHostMonitor(String name,	String groupName, int type, String udModuleName, boolean negativeDiffCountersToZero, boolean systemCm, int defaultPostponeTime)
	{
		super.setName(name);
		super.setGroupName(groupName);
		super.setNegativeDiffCountersToZero(negativeDiffCountersToZero, false);
		super.setSystemCm(systemCm);
		super.setPostponeTime(defaultPostponeTime, false);

		_udModuleName = udModuleName;
		_hostMonType = type;
		
		// Check if name is OK
		checkInConstructor();

		setDataSource(DATA_ABS, false);

		setUserDefinedAlarmInterrogator(createUserDefinedAlarmHandler());

		// Load saved properties
		super.loadProps();

		setDataInitialized(false);

		// Initialize alarms
		initAlarms();

		// Print the alarm configuration.
		printSystemAlarmConfig();
	}
	//----------------------------------------------------------------------------
	// END: Constructors
	//----------------------------------------------------------------------------

//	@Override
//	public CountersModel copyForOfflineRead()
//	{
//System.out.println(this+"::CounterModelHostMonitor.copyForOfflineRead(): this="+this);
//
//System.out.println(this+"::CounterModelHostMonitor.copyForOfflineRead(): _hostMonType   = "+_hostMonType);
//System.out.println(this+"::CounterModelHostMonitor.copyForOfflineRead(): _hostMonitor   = "+_hostMonitor);
//System.out.println(this+"::CounterModelHostMonitor.copyForOfflineRead(): _osSampleTable = "+_osSampleTable);
//System.out.println(this+"::CounterModelHostMonitor.copyForOfflineRead(): _diffData      = "+_diffData);
//System.out.println(this+"::CounterModelHostMonitor.copyForOfflineRead(): _rateData      = "+_rateData);
//System.out.println();
////System.out.println(this+"::CounterModelHostMonitor.copyForOfflineRead(): _offlineMode        = "+_offlineMode);
//System.out.println(this+"::CounterModelHostMonitor.copyForOfflineRead(): _offlineValues      = "+_offlineValues);
//System.out.println(this+"::CounterModelHostMonitor.copyForOfflineRead(): _offlineValuesAbs   = "+_offlineValuesAbs);
//System.out.println(this+"::CounterModelHostMonitor.copyForOfflineRead(): _offlineValuesDiff  = "+_offlineValuesDiff);
//System.out.println(this+"::CounterModelHostMonitor.copyForOfflineRead(): _offlineValuesRate  = "+_offlineValuesRate);
//System.out.println(this+"::CounterModelHostMonitor.copyForOfflineRead(): _offlineMetadata    = "+_offlineMetadata         + ", _offlineMetadata.getColumnNames():" + (_offlineMetadata==null?"-null-":_offlineMetadata.getColumnNames()) );
////System.out.println(this+"::CounterModelHostMonitor.copyForOfflineRead(): _offlineMetadataArr = "+_offlineMetadataArr);
//
//CountersModel cm = super.copyForOfflineRead();
//System.out.println(this+"::CounterModelHostMonitor.copyForOfflineRead(): return cm="+cm);
//		return cm;
//	}

	/** Check if we are connected to a specififc OS Vendor */
	public boolean isConnectedToVendor(HostMonitor.OsVendor vendor)
	{
		if (_hostMonitor == null)
			return false;
		
		return _hostMonitor.isConnectedToVendor(vendor);
	}

	/** Check if we are connected to a specififc OS Vendor */
	public HostMonitor.OsVendor getConnectedToVendor()
	{
		if (_hostMonitor == null)
			return HostMonitor.OsVendor.NotSet;
		
		return _hostMonitor.getConnectedToVendor();
	}

	/** called from GetCounters.initCounters() */
	@Override
	public void init(DbxConnection conn)
	throws Exception
	{
		localInit();

		// init stuff on the GUI part, if we have one...
		if (getTabPanel() != null)
			getTabPanel().onCmInit();
	}

	private boolean _localInit = false;
	private void localInit()
	throws Exception
	{
System.out.println("CounterModelHostMonitor.localInit(): _hostMonType="+_hostMonType+", CounterController.getInstance().isHostMonConnected()="+CounterController.getInstance().isHostMonConnected());
		if (CounterController.getInstance().isHostMonConnected())
		{
//			SshConnection sshConn = CounterController.getInstance().getHostMonConnection();
			HostMonitorConnection hostMonConn = CounterController.getInstance().getHostMonConnection();

			//System.out.println(this+"::CounterModelHostMonitor.init(sqlConn): CREATING MONITOR: "+_hostMonType);

			// FIXME: Hmm do this in a BETTER way, factoring needs to much maintenance,
			// it would be better to use some kind of class loading, or similar...
			if      (_hostMonType == HOSTMON_IOSTAT)    _hostMonitor = MonitorIo       .createMonitor(hostMonConn, false);
			else if (_hostMonType == HOSTMON_VMSTAT)    _hostMonitor = MonitorVmstat   .createMonitor(hostMonConn, false);
			else if (_hostMonType == HOSTMON_MPSTAT)    _hostMonitor = MonitorMpstat   .createMonitor(hostMonConn, false);
			else if (_hostMonType == HOSTMON_UPTIME)    _hostMonitor = MonitorUpTime   .createMonitor(hostMonConn, false);
			else if (_hostMonType == HOSTMON_MEMINFO)   _hostMonitor = MonitorMeminfo  .createMonitor(hostMonConn, false);
			else if (_hostMonType == HOSTMON_NWINFO)    _hostMonitor = MonitorNwInfo   .createMonitor(hostMonConn, false);
			else if (_hostMonType == HOSTMON_DISKSPACE) _hostMonitor = MonitorDiskSpace.createMonitor(hostMonConn, false);
			else if (_hostMonType == HOSTMON_PS)        _hostMonitor = MonitorPs       .createMonitor(hostMonConn, false);
			else if (_hostMonType == HOSTMON_UD_CLASS)
			{
				Configuration conf = null;
				if(DbxTune.hasGui())
//					conf = Configuration.getInstance(Configuration.CONF);
					conf = Configuration.getCombinedConfiguration();
				else
					conf = Configuration.getInstance(Configuration.PCS);

				if (conf == null)
					throw new Exception("No Configuration can be found.");

				_hostMonitor = new MonitorUserDefined(conf, _udModuleName, hostMonConn, false);
			}
			else
				throw new Exception("Unsupported HOSTMON_TYPE of '"+_hostMonType+"'.");

			// set this property so we can reach this object from the TabularControlPanel
			setClientProperty(HostMonitor.PROPERTY_NAME, _hostMonitor);

			_localInit = true;
		}

		// If the OS CM is initialized "late", in the refreshGetData(), then we need to do some extra stuff
		if (_hostMonitor != null)
		{
			// init stuff on the GUI part, if we have one...
			if (getTabPanel() != null)
				getTabPanel().onCmInit();
		}
System.out.println("CounterModelHostMonitor.localInit() end: _hostMonitor="+_hostMonitor);
	}

	/*---------------------------------------------------
	** BEGIN: implementing TableModel or overriding CounterModel/AbstractTableModel
	**---------------------------------------------------
	*/
	@Override
	public int getColumnCount()
	{
//System.out.println(this+"::getColumnCount(): getDataSource="+getDataSource()+", _offlineValues='"+_offlineValues+"'. xxx="+(_offlineValues==null?"super":_offlineValues.getColumnCount()));
		if (_offlineValues != null) 
			return _offlineValues.getColumnCount();

		return super.getColumnCount();
//		int c = 0;
//		if (isDataInitialized() && _osSampleTable != null)
//			c = _osSampleTable.getColumnCount();
//		return c;
    }

	@Override
	public String getColumnName(int col)
	{
//System.out.println(this+"::getColumnName(col="+col+"): getDataSource="+getDataSource()+", _offlineValues='"+_offlineValues+"'. xxx="+(_offlineValues==null?"super":_offlineValues.getColumnName(col)));
		if (_offlineValues != null) 
			return _offlineValues.getColumnName(col); 

		return super.getColumnName(col);
//		if (isDataInitialized() && _osSampleTable != null)
//			return _osSampleTable.getColumnName(col); 
//		return null;
	}

	@Override
	public Class<?> getColumnClass(int col)
	{
//System.out.println(this+"::getColumnClass(col="+col+"): getDataSource="+getDataSource()+", _offlineValues='"+_offlineValues+"'. xxx="+(_offlineValues==null?"super":_offlineValues.getColumnClass(col)));
		if (_offlineValues != null) 
			return _offlineValues.getColumnClass(col); 

		return super.getColumnClass(col);
//		if (isDataInitialized() && _osSampleTable != null)
//			return _osSampleTable.getColumnClass(col); 
//		return null;
	}
	
	@Override
	public int getRowCount()
	{
//System.out.println(this+"::getRowCount(): getDataSource="+getDataSource()+", _offlineValues='"+_offlineValues+"'. xxx="+(_offlineValues==null?"super":_offlineValues.getRowCount()));
		if (_offlineValues != null) 
			return _offlineValues.getRowCount();

		return super.getRowCount();
//		int c = 0;
//		if (isDataInitialized() && _osSampleTable != null)
//			c = _osSampleTable.getRowCount();
//		return c;
    }

	@Override
	public Object getValueAt(int row, int col)
	{
//System.out.println(this+"::getValueAt(row="+row+", col="+col+"): getDataSource="+getDataSource()+", _offlineValues='"+_offlineValues+"'. xxx="+(_offlineValues==null?"super":_offlineValues.getValueAt(row,col)));
		if (_offlineValues != null) 
			return _offlineValues.getValueAt(row, col);

		return super.getValueAt(row, col);
//		if (isDataInitialized() && _osSampleTable != null)
//			return _osSampleTable.getValueAt(row, col);
//		return null;
    }

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return false;
	}

	@Override
	public int findColumn(String colName)
	{
//System.out.println(this+"::findColumn(colName="+colName+"): getDataSource="+getDataSource()+", _offlineValues='"+_offlineValues+"'. xxx="+(_offlineValues==null?"super":_offlineValues.findColumn(colName)));
		if (_offlineValues != null) 
			return _offlineValues.findColumn(colName);

		return super.findColumn(colName);
//		if (isDataInitialized() && _osSampleTable != null)
//			return _osSampleTable.findColumn(colName);
//		return -1;
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
		OsTable data = null;

		if      (whatData == DATA_ABS)  data = _osSampleTable;
		else if (whatData == DATA_DIFF) data = _diffData;
		else if (whatData == DATA_RATE) data = _rateData;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		List<List<Object>> table = new ArrayList<List<Object>>();
		int rowCount = data.getRowCount();
		int colCount = data.getColumnCount();

		for (int r=0; r<rowCount; r++)
		{
			List<Object> row = new ArrayList<Object>();
			for (int c=0; c<colCount; c++)
				row.add(data.getValueAt(r, c));
			table.add(row);
		}

		return table;
	}

	// FIXME: the below is used in PCS(JDBC STORAGE), maybe change PCS to use TableModel instead 
	@Override
	public synchronized List<String> getColNames(int whatData)
	{
		OsTable data = null;

		if      (whatData == DATA_ABS)  data = _osSampleTable;
		else if (whatData == DATA_DIFF) data = _diffData;
		else if (whatData == DATA_RATE) data = _rateData;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available. you passed whatData="+whatData);

		if (data == null)
			return null;

		return data.getColNames();
	}



	/** Used to hold offline data */
	private CounterSample       _offlineValues     = null;
	private CounterSample       _offlineValuesAbs  = null;
	private CounterSample       _offlineValuesDiff = null;
	private CounterSample       _offlineValuesRate = null;
	private HostMonitorMetaData _offlineMetadata   = null;

	@Override
	public void clearForRead()
	{
		super.clearForRead();

		_offlineValues      = null;
		_offlineValuesAbs   = null;
		_offlineValuesDiff  = null;
		_offlineValuesRate  = null;
	}


	/** 
	 * Used to set off-line counter data <br>
	 * NOTE: PersistentReader clones the original object... CounterController.getInstance().getCmByName(name)<br>
	 *       So if we want to make MORE PERMANENT setting we need to do it on the Cm grabbed from the CounterController cm Map
	 */
	@Override
	public void setOfflineColumnNames(int type, List<String> cols, List<Integer> sqlTypes)
	{
//System.out.println(this+"::setColumnNames(): ----------------------------------------------------------------------");
//System.out.println(this+"::setColumnNames(): type="+type+", cols='"+cols+"'.");
//
//System.out.println(this+"::setColumnNames(): _offlineValues     = "+_offlineValues);
//System.out.println(this+"::setColumnNames(): _offlineValuesAbs  = "+_offlineValuesAbs);
//System.out.println(this+"::setColumnNames(): _offlineValuesDiff = "+_offlineValuesDiff);
//System.out.println(this+"::setColumnNames(): _offlineValuesRate = "+_offlineValuesRate);


		if (_offlineMetadata == null)
		{
			HostMonitorMetaData[] offlineMetadataArr = null;
			
			if      (_hostMonType == HOSTMON_IOSTAT)    offlineMetadataArr = MonitorIo         .createOfflineMetaData();
			else if (_hostMonType == HOSTMON_VMSTAT)    offlineMetadataArr = MonitorVmstat     .createOfflineMetaData();
			else if (_hostMonType == HOSTMON_MPSTAT)    offlineMetadataArr = MonitorMpstat     .createOfflineMetaData();
			else if (_hostMonType == HOSTMON_UPTIME)    offlineMetadataArr = MonitorUpTime     .createOfflineMetaData();
			else if (_hostMonType == HOSTMON_MEMINFO)   offlineMetadataArr = MonitorMeminfo    .createOfflineMetaData();
			else if (_hostMonType == HOSTMON_NWINFO)    offlineMetadataArr = MonitorNwInfo     .createOfflineMetaData();
			else if (_hostMonType == HOSTMON_DISKSPACE) offlineMetadataArr = MonitorDiskSpace  .createOfflineMetaData();
			else if (_hostMonType == HOSTMON_PS)        offlineMetadataArr = MonitorPs         .createOfflineMetaData();
			else if (_hostMonType == HOSTMON_UD_CLASS)  offlineMetadataArr = MonitorUserDefined.createOfflineMetaData(_udModuleName);
	
			_offlineMetadata = null;
			for (HostMonitorMetaData md : offlineMetadataArr)
			{
				if ( cols.containsAll(md.getColumnNames()) )
				{
					_offlineMetadata = md;
					
					// Set the value in the CounterController CM-MAP as well
					// NOTE: This since a new CM is created/cloned from the CC-CM-MAP every time a offline read is done...
					//       and in this way we do not need to initialize a new MetaData class on every offline read!
					CounterModelHostMonitor ccCm = (CounterModelHostMonitor) CounterController.getInstance().getCmByName(getName());
					ccCm._offlineMetadata = md;
				}
			}
			if (_offlineMetadata != null)
				_logger.info("Setting Offline Meta Data to use '"+_offlineMetadata.getTableName()+"'.");
			else
				_logger.warn("Can't find a Offline Meta Data structure for columns: "+cols);
//System.out.println(this+"::setColumnNames(): _offlineMetadata="+_offlineMetadata+", this="+this+", getTabPanel()="+getTabPanel());

			// Initialize the GUI
			if (_offlineMetadata != null)
			{
				if (getTabPanel() != null)
				{
//					System.out.println(this+"::CALLING getTabPanel().onCmInit()... from: CounterModelHostMonitor.setColumnNames(int="+type+", cols="+cols+")");
//					getTabPanel().onCmInit();
					getTabPanel().setOptionsOnCmInit(this);
					// NOTE: in PersistReader.loadSessionCm(CmIndicator cmInd, Timestamp sampleTs)
					//       we create a new CM instance using: cm = cm.copyForOfflineRead();
					//       and at: PersistReader.loadSessionCm(CountersModel cm, int type, Timestamp sampleTs): 
					//               we call this method on the new CM...which is later set in the TabularCntrPanel with setDisplayCm()
					//               but this is made after this call... so getDisplayCm() at TabularCntrPanel will return null or faulty CM
					//       so in a nutshell, we can't call getTabPanel().onCmInit();
				}
			}
		}
		CounterSample data = null;
		if      (type == DATA_ABS)  { if (_offlineValuesAbs  == null) {_offlineValuesAbs  = new CounterSample("offline-abs",  false, null, null); data = _offlineValuesAbs ;} else data = _offlineValuesAbs ;}
		else if (type == DATA_DIFF) { if (_offlineValuesDiff == null) {_offlineValuesDiff = new CounterSample("offline-diff", false, null, null); data = _offlineValuesDiff;} else data = _offlineValuesDiff;}
		else if (type == DATA_RATE) { if (_offlineValuesRate == null) {_offlineValuesRate = new CounterSample("offline-rate", false, null, null); data = _offlineValuesRate;} else data = _offlineValuesRate;}
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		data.setColumnNames(cols);
		data.setSqlType(sqlTypes);
	}

	public boolean isOfflineMode()
	{
		return _offlineMetadata != null;
	}

	@Override
	public void setDataSource(int dataSource, boolean saveProps)
	{
//System.out.println(this+"::CounterModelHostMonitor.setDataSource(dataSource="+dataSource+", saveProps="+saveProps+"): isOfflineMode()="+isOfflineMode());
		if (isOfflineMode())
		{
			if      (dataSource == DATA_ABS)  { _offlineValues = _offlineValuesAbs;  }
			else if (dataSource == DATA_DIFF) { _offlineValues = _offlineValuesDiff; }
			else if (dataSource == DATA_RATE) { _offlineValues = _offlineValuesRate; }
		}
		super.setDataSource(dataSource, saveProps);
	}
	/** 
	 * Used to set off-line counter data 
	 */
	@Override
	public void setOfflineValueAt(int type, Object value, int row, int col)
	{
		CounterTableModel data = null;
		if      (type == DATA_ABS)  { if (_offlineValuesAbs  == null) {_offlineValuesAbs  = new CounterSample("offline-abs",  false, null, null); data = _offlineValuesAbs ;} else data = _offlineValuesAbs ;}
		else if (type == DATA_DIFF) { if (_offlineValuesDiff == null) {_offlineValuesDiff = new CounterSample("offline-diff", false, null, null); data = _offlineValuesDiff;} else data = _offlineValuesDiff;}
		else if (type == DATA_RATE) { if (_offlineValuesRate == null) {_offlineValuesRate = new CounterSample("offline-rate", false, null, null); data = _offlineValuesRate;} else data = _offlineValuesRate;}
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		// NOTE: this might suffer from performance problems if: fireTableCellUpdated() on the JTable is fired...
		data.setValueAt(value, row, col);

		if      (type == DATA_ABS)  { _offlineValues = _offlineValuesAbs;  }
		else if (type == DATA_DIFF) { _offlineValues = _offlineValuesDiff; }
		else if (type == DATA_RATE) { _offlineValues = _offlineValuesRate; }
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

	@Override public boolean isDiffCalcEnabled()                   
	{ 
//System.out.println(this+"::isDiffCalcEnabled(): _offlineMetadata="+_offlineMetadata+", _hostMonitor='"+_hostMonitor+"', this="+this+", getTabPanel()="+getTabPanel());
		if (_offlineMetadata != null)
			return _offlineMetadata.isDiffEnabled();

		// _hostMonitor is NOT initialized from start, only after a localInit() has been done. which can be called "late" from refreshGetData()
		// After connect this method is called again from: CounterTablePanel.onCmInit(), or localInit()... where we a second times determen what options should be visible
		if (_hostMonitor == null)
			return false;
		
		return _hostMonitor.getMetaData().isDiffEnabled();
	}

	@Override public boolean isDiffColumn(int index)
	{
//		if (getDataSource() == DATA_ABS)
//			return false;

		// MetaData starts a 1, while the index here starts at 0 for col1
		int column = index + 1;

		HostMonitorMetaData md = null;
		
		if (_offlineMetadata != null) md = _offlineMetadata;
		if (_osSampleTable   != null) md = _osSampleTable.getMetaData();

		if (md != null)
			return md.isDiffColumn(column);

		return false;
	}

	@Override public boolean isPctColumn (int index) 
	{
		// MetaData starts a 1, while the index here starts at 0 for col1
		int column = index + 1;

		HostMonitorMetaData md = null;
		
		if (_offlineMetadata != null) md = _offlineMetadata;
		if (_osSampleTable   != null) md = _osSampleTable.getMetaData();

		if (md != null)
			return md.isPctColumn(column);

		return false;
	}

	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
	}
	
//	@Override public CounterTableModel getCounterDataAbs()  { System.out.println(this+"::getCounterDataAbs():  isOfflineMode()="+isOfflineMode()+", _offlineValuesAbs=" +_offlineValuesAbs +", _osSampleTable="+_osSampleTable); return isOfflineMode() ? _offlineValuesAbs  : _osSampleTable; }
//	@Override public CounterTableModel getCounterDataDiff() { System.out.println(this+"::getCounterDataDiff(): isOfflineMode()="+isOfflineMode()+", _offlineValuesDiff="+_offlineValuesDiff+", _diffData="     +_diffData);      return isOfflineMode() ? _offlineValuesDiff : _diffData; }
//	@Override public CounterTableModel getCounterDataRate() { System.out.println(this+"::getCounterDataRate(): isOfflineMode()="+isOfflineMode()+", _offlineValuesRate="+_offlineValuesRate+", _rateData="     +_rateData);      return isOfflineMode() ? _offlineValuesRate : _rateData; }
//
//	@Override public boolean hasAbsData()  { System.out.println(this+"::hasAbsData():  isOfflineMode()="+isOfflineMode()+", _offlineValuesAbs=" +_offlineValuesAbs +", _osSampleTable="+_osSampleTable); return isOfflineMode() ? _offlineValuesAbs  != null : _osSampleTable != null; }
//	@Override public boolean hasDiffData() { System.out.println(this+"::hasDiffData(): isOfflineMode()="+isOfflineMode()+", _offlineValuesDiff="+_offlineValuesDiff+", _diffData="     +_diffData);      return isOfflineMode() ? _offlineValuesDiff != null : _diffData      != null; }
//	@Override public boolean hasRateData() { System.out.println(this+"::hasRateData(): isOfflineMode()="+isOfflineMode()+", _offlineValuesRate="+_offlineValuesRate+", _rateData="     +_osSampleTable); return isOfflineMode() ? _offlineValuesRate != null : _rateData      != null; }

	@Override public CounterTableModel getCounterDataAbs()  { return isOfflineMode() ? _offlineValuesAbs  : _osSampleTable; }
	@Override public CounterTableModel getCounterDataDiff() { return isOfflineMode() ? _offlineValuesDiff : _diffData; }
	@Override public CounterTableModel getCounterDataRate() { return isOfflineMode() ? _offlineValuesRate : _rateData; }

	@Override public boolean hasAbsData()  { return isOfflineMode() ? _offlineValuesAbs  != null : _osSampleTable != null; }
	@Override public boolean hasDiffData() { return isOfflineMode() ? _offlineValuesDiff != null : _diffData      != null; }
	@Override public boolean hasRateData() { return isOfflineMode() ? _offlineValuesRate != null : _rateData      != null; }

	@Override public synchronized Timestamp getTimestamp() { return _thisSamplingTime; }

	@Override
	protected int refreshGetData(DbxConnection conn) 
	throws Exception
	{
		if (_logger.isDebugEnabled())
			_logger.debug("Entering refresh() method for " + getName());

		if ( ! _localInit )
			localInit();

		if ( _hostMonitor == null )
			return -1;

		if ( _hostMonitor.isPaused() )
		{
			_logger.info("The Host Monitor module '"+_hostMonitor.getModuleName()+"' is paused.");
			// do NOT return here, _hostMonitor.getSummaryTable() will return an empty, 0 row result...
		}

		if ( ! _hostMonitor.isRunning() && ! _hostMonitor.isPaused() )
		{
			if ( _hostMonitor.isOsCommandStreaming() )
				_hostMonitor.start();
			// do NOT return here, _hostMonitor.getSummaryTable() will return an empty, 0 row result...
		}

		final OsTable tmpNewSample;
		OsTable tmpDiffData = null;
		OsTable tmpRateData = null;

		// GET THE DATA FROM THE HOST MONITOR
		if (_hostMonitor.isOsCommandStreaming())
			tmpNewSample = _hostMonitor.getSummaryTable();
		else
			tmpNewSample = _hostMonitor.executeAndParse();

		if (_logger.isDebugEnabled())
			_logger.debug("OUTPUT Table from '"+_hostMonitor.getModuleName()+"':\n" + tmpNewSample.toTableString());

		// set MetaData: otherwise the Persistent Counter Storage can't create tables
		setResultSetMetaData(tmpNewSample.getMetaData());

		// If the HostMonitor has problems, relay them to the GUI
		Exception exception = _hostMonitor.getException();
		if (exception != null)
			setSampleException(exception);
			//throw exception;

		int rowsFetched = tmpNewSample.getRowCount();

		setDataInitialized(true);

		// Update dates on panel
		Timestamp prevSamplingTime = _thisSamplingTime;
		_thisSamplingTime = new Timestamp(System.currentTimeMillis());
		setSampleTime( _thisSamplingTime );
		if (_hostMonitor.isOsCommandStreaming())
			setSampleInterval(tmpNewSample.getSampleSpanTime());
		else
		{
			if (prevSamplingTime != null)
				setSampleInterval(_thisSamplingTime.getTime() - prevSamplingTime.getTime());
		}

		localCalculation(tmpNewSample);

		// Do local calculation with availability to the previous sample
		localCalculation(_osSampleTable, tmpNewSample);

		// Should we do DIFF/RATE calculations
		if ( isDiffCalcEnabled() )
		{
			if (_osSampleTable != null)
			{
				final List<Integer> deletedRows  = null;
				final List<Integer> newDeltaRows = null;

				tmpDiffData = computeDiffCnt(_osSampleTable, tmpNewSample, deletedRows, newDeltaRows);

				// If we got a DIFF
				if (tmpDiffData != null)
				{
					beginLcRefresh();

					// Compute local stuff
					// NOTE: this needs to be done BEFORE computeRatePerSec()
					//       otherwise the PCT columns will still be the DIFF values
					localCalculation(_osSampleTable, tmpNewSample, tmpDiffData);
		
					// we got some data, compute the rates and update the data model
					tmpRateData = computeRatePerSec(tmpDiffData, getSampleInterval());

					// Compute local stuff for RatePerSec, here we can adjust some stuff if needed
					localCalculationRatePerSec(tmpRateData, tmpDiffData);
		
					long secondLcTime = endLcRefresh();
					//setLcRefreshTime(firstLcTime + secondLcTime);
					setLcRefreshTime(secondLcTime);
				}
			}
		}

//if ( _hostMonitor.getMetaData().isDiffEnabled() )
//{
//	System.out.println("");
//	System.out.println("");
//	System.out.println("");
//	
//	if (tmpNewSample != null)
//	{
//		System.out.println("##############################################################");
//		System.out.println("DATA: "+getName());
//		System.out.println("##############################################################");
//		System.out.println(tmpNewSample.toTableString());
//		System.out.println("");
//	}
//
//	if (tmpDiffData != null)
//	{
//		System.out.println("##############################################################");
//		System.out.println("DIFF: "+getName());
//		System.out.println("##############################################################");
//		System.out.println(tmpDiffData.toTableString());
//		System.out.println("");
//	}
//
//	if (tmpRateData != null)
//	{
//		System.out.println("##############################################################");
//		System.out.println("RATE: "+getName());
//		System.out.println("##############################################################");
//		System.out.println(tmpRateData.toTableString());
//		System.out.println("");
//	}
//}
		// NOW apply data to the VIEW
		// in GUI mode this is done preferred by the EventDispathThread, thats why we
		// need to use the temp variable tmpOsSampleTable, which is set to _osSampleTable...
		if ( ! DbxTune.hasGui() )
		{
			// NO GUI, move structures
			_osSampleTable = tmpNewSample;
			_diffData      = tmpDiffData;
			_rateData      = tmpRateData;

			// if it's the first time sampling...
			if ( ! isInitialized() )
			{
				_logger.debug(getName()+":------doFireTableStructureChanged------");
				fireTableStructureChanged();

				setInitialized(true);
			}

			// Calculte what values we should have in the graphs
			// this has to be after _prevSample, _newSample, _diffData, _rateData has been SET
			updateGraphData();

			// Do we want to send an Alarm somewhere, every CM's has to implement this.
			wrapperFor_sendAlarmRequest();
		}
		else // HAS GUI
		{
			final OsTable f_tmpDiffOsSampleTable = tmpDiffData;
			final OsTable f_tmpRateOsSampleTable = tmpRateData;

			Runnable doWork = new Runnable()
			{
				@Override
				public void run()
				{
					// IMPORTANT: move datastructure.
					_osSampleTable = tmpNewSample;
					_diffData      = f_tmpDiffOsSampleTable;
					_rateData      = f_tmpRateOsSampleTable;

					beginGuiRefresh();

					if ( ! isInitialized() )
					{
						_logger.debug(getName()+":------doFireTableStructureChanged------");
						fireTableStructureChanged();

						setInitialized(true);
					}

					if (getTabPanel() != null && !getTabPanel().isTableInitialized())
					{
						_logger.debug(getName()+":------doFireTableStructureChanged------");
						fireTableStructureChanged();
						getTabPanel().adjustTableColumnWidth();
					}
					else
					{
						_logger.debug(getName()+":-fireTableDataChanged-");
						fireTableDataChanged();
					}

					// Calculte what values we should have in the graphs
					// this has to be after _prevSample, _newSample, _diffData, _rateData has been SET
					// since we do this differred in case we use Swing, it has to be done here.
					updateGraphData();

					endGuiRefresh();

					// Do we want to send an Alarm somewhere, every CM's has to implement this.
					wrapperFor_sendAlarmRequest();
				}
			};
			// Invoke this job on the SWING Event Dispather Thread
			if ( ! SwingUtilities.isEventDispatchThread() )
				SwingUtilities.invokeLater(doWork);
			else
				doWork.run();
		}
		
		return rowsFetched;
	}

	/**
	 * Create a DIFF calculated OsTable<br>
	 * NOTE: most of the code is from CounterModel.computeDiffCnt(): so if you change this, make sure you get the same logic in CounterModel
	 * 
	 * @param prevSample
	 * @param thisSample
	 * @return
	 */
	public OsTable computeDiffCnt(OsTable oldSample, OsTable newSample, List<Integer> deletedRows, List<Integer> newDeltaRows)
	{
		// NOT-YET-IMPLEMENTED
//		return null;
		HostMonitorMetaData metaData = newSample.getMetaData();
		
		// Initialize result structure
		OsTable diffCnt  = new OsTable(newSample.getMetaData());
		
//		return CounterSample.computeDiffCnt(prevSample, newSample, deletedRows, pkCols, isDiffCol, isCountersCleared);
		// Initialize result structure
//		CounterSample diffCnt = new CounterSample(newSample, false, newSample._name+"-diff");

//		long newTsMilli      = newSample.getSampleTime().getTime();
//		long oldTsMilli      = oldSample.getSampleTime().getTime();
//		int newTsNano        = newSample.getSampleTime().getNanos();
//		int oldTsNano        = oldSample.getSampleTime().getNanos();
//
//		// Check if TsMilli has really ms precision (not the case before JDK 1.4)
//		if ((newTsMilli - (newTsMilli / 1000) * 1000) == newTsNano / 1000000)
//			// JDK > 1.3.1
//			diffCnt.setSampleInterval(newTsMilli - oldTsMilli);
//		else
//			diffCnt.setSampleInterval(newTsMilli - oldTsMilli + (newTsNano - oldTsNano) / 1000000);

//		List<Object> newRow;
//		List<Object> oldRow;
//		List<Object> diffRow;
		OsTableRow newRow;
		OsTableRow oldRow;
		OsTableRow diffRow;

		boolean isCountersCleared          = false;
//		boolean negativeDiffCountersToZero = true; // diffCnt.getNegativeDiffCountersToZero();
		boolean negativeDiffCountersToZero = super.isNegativeDiffCountersToZero();
		String  newSampleName              = getName() + "-diff";   // newSample.getName()
		
		// Special case, only one row for each sample, no key
		if ( ! metaData.hasPkCols() )
		{
			oldRow = oldSample.getRow(0);
			newRow = newSample.getRow(0);
			diffRow = new OsTableRow(newRow);
			for (int c = 1; c <= newSample.getColumnCount(); c++) // Note col start with 1 in OsTableRow
			{
				// This looks ugly: should really be done using the same logic as below... But this is really never used...
				diffRow.setValue(c, new Integer(((Integer) (newRow.getValue(c))).intValue() - ((Integer) (oldRow.getValue(c))).intValue()));
			}
			diffCnt.addRow(diffRow);
			return diffCnt;
		}

		// Keep a array of what rows that we access of the old values
		// this will help us find out what rows we "deleted" from the previous to the new sample
		// or actually rows that are no longer available in the new sample...
		boolean oldSampleAccessArr[] = new boolean[oldSample.getRowCount()]; // default values: false

//		if (_isNewDeltaOrRateRow == null)
//			_isNewDeltaOrRateRow = new boolean[newSample.getRowCount()+10]; // default values: false... add some extra fields...

		// Loop on all rows from the NEW sample
		for (int newRowId = 0; newRowId < newSample.getRowCount(); newRowId++)
		{
			newRow = newSample.getRow(newRowId);
			diffRow = new OsTableRow(newRow);
			
			// get PK of the new row
			String newPk = newSample.getPkValue(newRowId);

			// Retrieve old same row
			int oldRowId = oldSample.getRowNumberForPkValue(newPk);
			
			// if old Row EXISTS, we can do diff calculation
			if (oldRowId != -1)
			{
				// Mark the row as "not deleted" / or "accessed"
				if (oldRowId >= 0 && oldRowId < oldSampleAccessArr.length)
					oldSampleAccessArr[oldRowId] = true;
				
				// Old row found, compute the diffs
				oldRow = oldSample.getRow(oldRowId);
				for (int c = 1; c <= newSample.getColumnCount(); c++)  // Note col start with 1 in OsTableRow
				{
					if ( ! metaData.isDiffColumn(c) )
						diffRow.setValue(c, newRow.getValue(c));
					else
					{
						//checkType(oldSample, oldRowId, i, newSample, newRowId, i);
						//if ((newRow.get(i)).getClass().toString().equals("class java.math.BigDecimal"))
						Object oldRowObj = oldRow.getValue(c);
						Object newRowObj = newRow.getValue(c);

						String colName = newSample.getColumnName(c);

						if (newRowObj == null)
						{
							diffRow.setValue(c, null);
						}
						else if ( newRowObj instanceof Number )
						{
							Number diffValue = diffColumnValue((Number)oldRowObj, (Number)newRowObj, negativeDiffCountersToZero, newSampleName, colName, isCountersCleared);
							diffRow.setValue(c, diffValue);
						}
						else
						{
							// Try to convert the String into a Number
							try
							{
								Number diffValue = diffColumnValue(NumberUtils.toNumber(oldRowObj), NumberUtils.toNumber(newRowObj), negativeDiffCountersToZero, newSampleName, colName, isCountersCleared);
								diffRow.setValue(c, diffValue);
							}
							catch(NumberFormatException nfe)
							{
								_logger.warn("CounterSampleSetName='"+newSampleName+"', className='"+newRowObj.getClass().getName()+"' columns can't be 'diff' calculated. colName='"+colName+"', key='"+newPk+"', oldObj='"+oldRowObj+"', newObj='"+newRowObj+"'. Trying to convert it to a Number Caught: "+nfe);
								diffRow.setValue(c, newRowObj);
							}
								
//							_logger.warn("CounterSampleSetName='"+newSample._name+"', className='"+newRowObj.getClass().getName()+"' columns can't be 'diff' calculated. colName='"+colName+"', key='"+newPk+"', oldObj='"+oldRowObj+"', newObj='"+newRowObj+"'.");
//							diffRow.setValue(c, newRowObj);
						}
					}
				}

				// Mark this row as 'new row'
				setNewDeltaOrRateRow(newRowId, false);

			} // end: old row was found
			else
			{
				// Row was NOT found in previous sample, which means it's a "new" row for this sample.
				// So we do not need to do DIFF calculation, just add the raw data...
				for (int c = 1; c <= newSample.getColumnCount(); c++)  // Note col start with 1 in OsTableRow
				{
					diffRow.setValue(c, newRow.getValue(c));
				}
				
				if (newDeltaRows != null)
					newDeltaRows.add(newRowId);
				
				// Mark this row as 'new row'
				setNewDeltaOrRateRow(newRowId, true);
			}

			diffCnt.addRow(diffRow);

		} // end: row loop
		
		// What rows was DELETED from previous sample.
		// meaning, rows in the previous sample that was NOT part of the new sample.
		if (deletedRows != null)
		{
			for (int c = 1; c <= oldSampleAccessArr.length; c++) // Note col start with 1 in OsTableRow
			{
				if (oldSampleAccessArr[c] == false)
				{
					deletedRows.add(c);
				}
			}
		}

		return diffCnt;
	}

	/**
	 * Compute Rate per seconds
	 * @param diffData
	 * @param sampleInterval
	 * @return
	 */
	public OsTable computeRatePerSec(OsTable diffData, long sampleInterval)
	{
		// NOT-YET-IMPLEMENTED
//		return null;

		HostMonitorMetaData metaData = diffData.getMetaData();
		
		// Initialize result structure
		OsTable rate  = new OsTable(diffData.getMetaData());

//		int sampleInterval = diffData.getSampleInterval();
		
		// - Loop on all rows in the DIFF structure
		// - Do calculations on them
		// - And add them to the RATE structure
		for (int rowId=0; rowId < diffData.getRowCount(); rowId++) 
		{
			// Get the row from the DIFF structure
			OsTableRow diffRow = diffData.getRow(rowId);

			// Create a new ROW "structure" for each row in the DIFF
			OsTableRow newRow = new OsTableRow(diffRow);

			for (int c = 1; c <= diffData.getColumnCount(); c++) // Note col start with 1 in OsTableRow 
			{
				// Get the RAW object from the DIFF structure
				Object originObject = diffRow.getValue(c);

				// If the below IF statements is not true... keep the same object
				Object newObject = originObject;

				// If PCT column DO nothing.
				if ( metaData.isPctColumn(c) ) 
				{
				}
				// If this is a column that has DIFF calculation.
				else if ( metaData.isDiffColumn(c) ) 
				{
					double val = 0;

					// What to do if we CANT DO DIVISION
					if (sampleInterval == 0)
						newObject = "N/A";

					if (originObject == null)
					{
						newObject = null;
					}
					// Calculate rate
					else if (originObject instanceof Number)
					{
						// Get the object as a Double value
						if ( originObject instanceof Number )
							val = ((Number)originObject).doubleValue();
						else
							val = Double.parseDouble( originObject.toString() );

						// interval is in MilliSec, so val has to be multiplied by 1000
						val = (val * 1000) / sampleInterval;
						BigDecimal newVal = new BigDecimal( val ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

						// Set the new object
						newObject = newVal;
					}
					// Unsupported columns, skip the calculation
					else
					{
						String colName = diffData.getColumnName(c);
//						_logger.warn("CounterSampleSetName='"+diffData._name+"', className='"+originObject.getClass().getName()+"' columns can't be 'rate' calculated. colName='"+colName+"', originObject='"+originObject+"', keeping this object.");
						_logger.warn("OsTable(RateCalc)-CmName='"+getName()+"', className='"+originObject.getClass().getName()+"' columns can't be 'rate' calculated. colName='"+colName+"', originObject='"+originObject+"', keeping this object.");
						newObject = originObject;
					}
				}

				// set the data in the new row
//				newRow.add(newObject);
				newRow.setValue(c, newObject);

			} // end: row loop

			rate.addRow(newRow);

		} // end: all rows loop
		
		return rate;
	}

	/**
	 * Change some values...<br>
	 * For any subclases to implement, if they need the functionality
	 * @param tmpOsSampleTable
	 */
	public void localCalculation(OsTable thisSample)
	{
	}
	
	/** 
	 * Do local calculation with availability to the previous sample<br>
	 * For any subclases to implement, if they need the functionality
	 * 
	 * @param prevOsSampleTable
	 * @param thisOsSampleTable
	 */
	public void localCalculation(OsTable prevSample, OsTable thisSample)
	{
	}

	/** For any subclases to implement, if they need the functionality */
	public void localCalculation(OsTable prevSample, OsTable thisSample, OsTable diffdata)
	{
	}
	
	/** For any subclases to implement, if they need the functionality */
	public void localCalculationRatePerSec(OsTable rateData, OsTable diffData)
	{
	}

	/**
	 * SQL is not used here, so simply override this method with "nothing"
	 */
	@Override
	public void initSql(DbxConnection conn)
	{
	}

	@Override
	public void close()
	{
		_localInit = false;
		if (_hostMonitor != null)
			_hostMonitor.shutdown();
		_hostMonitor = null;

		// set this property so we can reach this object from the TabularControlPanel
		setClientProperty(HostMonitor.PROPERTY_NAME, _hostMonitor);
	}

	@Override
	public void clear()
	{
		clear(100);
	}
	@Override
	public synchronized void clear(int clearLevel)
	{
//System.out.println(this+"::clear##############################################################################################");
//System.out.println(this+"::clear(clearLevel="+clearLevel+")");
//System.out.println(this+"::clear##############################################################################################");

		setInitialized(false);

		_offlineValues      = null;
		_offlineValuesAbs   = null;
		_offlineValuesDiff  = null;
		_offlineValuesRate  = null;
		_offlineMetadata    = null;

		setValidSampleData(false);
		_osSampleTable    = null;
		_diffData         = null;
		_rateData         = null;
		_thisSamplingTime = null;

		setTimeInfo(null, null, null, 0);

		// Clear dates on panel
		TabularCntrPanel tabPanel = getTabPanel();
		if (tabPanel != null)
			tabPanel.reset();

//		System.out.println(_name+":------doFireTableStructureChanged------");
		fireTableStructureChanged();
	}

	/**
	 * Get basic configuration "html" chars/tags is accepted.<br>
	 * This would be overrided by modules that does not support Properties by ShowCmPropertiesDialog
	 * @return
	 */
	@Override
	public String getBasicConfigurationDescription()
	{
		String ret = 
			"Host Monitor has no specific properties.<br>" +
			"Please view executed command on the right hand side of the Option Panel<br>" +
			"<br>" +
			"hostMonType="+getTypeExplanation(_hostMonType)+".<br>";

		if (_hostMonType == HOSTMON_UD_CLASS)
			ret += "udModuleName='"+_udModuleName+"'<br>";

		if (_hostMonitor != null)
		{
			ret += "<br>";
			ret += "Some Basic info:<br>";
			ret += "getName: "     + getName()                           + "<br>";
			ret += "getCommand: "  + _hostMonitor.getCommand()           + "<br>";
			ret += "getSleepTime: "+ _hostMonitor.getSleepTime()         + "<br>";
			ret += "isPaused: "    + _hostMonitor.isPaused()             + "<br>";
			ret += "isRunning: "   + _hostMonitor.isRunning()            + "<br>";
			ret += "isStreaming: " + _hostMonitor.isOsCommandStreaming() + "<br>";
		}

		return ret;
	}
}




/* 
 * ********************************************************************************************
 * NOTE: The below might be a better solution (less moving parts)
 * But it doesnt work, due to the fact that we create/clone a new object every time we read from Offline DB
 * 
 * The below uses/references the super object... which "moves" due to the "clone"...
 * We might be able to use the below in the future...
 * 
 * The below code simply calls "super" in most cases (where we want to read offline storage)
 * But the current code is "fucked up", due to the "clone" to a offlineCm...
 * I need to come up with a better solution for offline handling :)
 * 
 * But that will done "later", and we all know how that is spelled... (most of the time it spelled: n-e-v-e-r)
 * 
 * ********************************************************************************************
 */

///** Used to hold offline data */
////private CounterSample     _offlineValues      = null;
////private CounterSample     _offlineValuesAbs   = null;
////private CounterSample     _offlineValuesDiff  = null;
////private CounterSample     _offlineValuesRate  = null;
//private HostMonitorMetaData   _offlineMetadata    = null;
//
///** 
// * Used to set off-line counter data <br>
// * NOTE: PersistentReader clones the original object... CounterController.getInstance().getCmByName(name)<br>
// *       So if we want to make MORE PERMANENT setting we need to do it on the Cm grabbed from the CounterController cm Map
// */
//@Override
//public void setOfflineColumnNames(int type, List<String> cols)
//{
//	if (_offlineMetadata == null)
//	{
//		HostMonitorMetaData[] offlineMetadataArr = null;
//		
//		if      (_hostMonType == HOSTMON_IOSTAT)   offlineMetadataArr = MonitorIo     .createOfflineMetaData();
//		else if (_hostMonType == HOSTMON_VMSTAT)   offlineMetadataArr = MonitorVmstat .createOfflineMetaData();
//		else if (_hostMonType == HOSTMON_MPSTAT)   offlineMetadataArr = MonitorMpstat .createOfflineMetaData();
//		else if (_hostMonType == HOSTMON_UPTIME)   offlineMetadataArr = MonitorUpTime .createOfflineMetaData();
//		else if (_hostMonType == HOSTMON_MEMINFO)  offlineMetadataArr = MonitorMeminfo.createOfflineMetaData();
//		else if (_hostMonType == HOSTMON_NWINFO)   offlineMetadataArr = MonitorNwInfo .createOfflineMetaData();
//		else if (_hostMonType == HOSTMON_UD_CLASS) offlineMetadataArr = MonitorUserDefined.createOfflineMetaData(_udModuleName);
//
//		_offlineMetadata = null;
//		for (HostMonitorMetaData md : offlineMetadataArr)
//		{
//			if ( cols.containsAll(md.getColumnNames()) )
//			{
//				_offlineMetadata = md;
//				
//				// Set the value in the CounterController CM-MAP as well
//				// NOTE: This since a new CM is created/cloned from the CC-CM-MAP every time a offline read is done...
//				//       and in this way we do not need to initialize a new MetaData class on every offline read!
//				CounterModelHostMonitor ccCm = (CounterModelHostMonitor) CounterController.getInstance().getCmByName(getName());
//				ccCm._offlineMetadata = md;
//			}
//		}
//		if (_offlineMetadata != null)
//			_logger.info("Setting Offline Meta Data to use '"+_offlineMetadata.getTableName()+"'.");
//		else
//			_logger.warn("Can't find a Offline Meta Data structure for columns: "+cols);
//
//		// Initialize the GUI
//		if (_offlineMetadata != null)
//		{
//			if (getTabPanel() != null)
//			{
//				System.out.println(this+"::CALLING getTabPanel().onCmInit()... from: CounterModelHostMonitor.setOfflineColumnNames(int="+type+", cols="+cols+")");
////				getTabPanel().onCmInit();
//				getTabPanel().setOptionsOnCmInit(this);
//				// NOTE: in PersistReader.loadSessionCm(CmIndicator cmInd, Timestamp sampleTs)
//				//       we create a new CM instance using: cm = cm.copyForOfflineRead();
//				//       and at: PersistReader.loadSessionCm(CountersModel cm, int type, Timestamp sampleTs): 
//				//               we call this method on the new CM...which is later set in the TabularCntrPanel with setDisplayCm()
//				//               but this is made after this call... so getDisplayCm() at TabularCntrPanel will return null or faulty CM
//				//       so in a nutshell, we can't call getTabPanel().onCmInit();
//			}
//		}
//	}
//
//	super.setOfflineColumnNames(type, cols);
//}
//
///*---------------------------------------------------
//** END: used by the Persistent Writer and Reader
//**---------------------------------------------------
//*/
//
//
//@Override public CounterTableModel getCounterDataAbs()  { return isOfflineMode() ? super.getCounterDataAbs()  : _osSampleTable; }
//@Override public CounterTableModel getCounterDataDiff() { return isOfflineMode() ? super.getCounterDataDiff() : _diffData; }
//@Override public CounterTableModel getCounterDataRate() { return isOfflineMode() ? super.getCounterDataRate() : _rateData; }
//
//@Override public boolean hasAbsData()  { return isOfflineMode() ? super.hasAbsData()  : _osSampleTable != null; }
//@Override public boolean hasDiffData() { return isOfflineMode() ? super.hasDiffData() : _diffData      != null; }
//@Override public boolean hasRateData() { return isOfflineMode() ? super.hasRateData() : _rateData      != null; }


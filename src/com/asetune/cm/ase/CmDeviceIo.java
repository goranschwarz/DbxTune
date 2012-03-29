package com.asetune.cm.ase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.GetCounters;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.MonTablesDictionary;
import com.asetune.cm.CountersModel;
import com.asetune.cm.SamplingCnt;
import com.asetune.cm.ase.gui.CmDeviceIoPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmDeviceIo
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmDeviceIo.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmDeviceIo.class.getSimpleName();
	public static final String   SHORT_NAME       = "Devices";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>What devices are doing IO's and what's the approximare service time on the disk.</p>" +
		"Do not trust the service time <b>too</b> much...<br>" +
		"<br>" +
		GetCounters.TRANLOG_DISK_IO_TOOLTIP +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monDeviceIO"};
	public static final String[] NEED_ROLES       = new String[] {"sa_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1"};

	public static final String[] PCT_COLUMNS      = new String[] {"ReadsPct", "APFReadsPct", "WritesPct"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"TotalIOs", "Reads", "APFReads", "Writes", "DevSemaphoreRequests", "DevSemaphoreWaits", "IOTime"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmDeviceIo(counterController, guiController);
	}

	public CmDeviceIo(ICounterController counterController, IGuiController guiController)
	{
		super(CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);

		if (getQueryTimeout() == CountersModel.DEFAULT_sqlQueryTimeout)
			setQueryTimeout(DEFAULT_QUERY_TIMEOUT);
		
		addTrendGraphs();
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmDeviceIoPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
			mtd.addColumn("monDeviceIO",  "TotalIOs",     "<html>" +
			                                                   "Total number of IO's issued on this device.<br>" +
			                                                   "<b>Formula</b>: Reads + Writes<br>" +
			                                              "</html>");
			mtd.addColumn("monDeviceIO",  "APFReadsPct",  "<html>" +
			                                                   "Of all the issued Reads, what's the Asynch Prefetch Reads percentage.<br>" +
			                                                   "<b>Formula</b>: APFReads / Reads * 100<br>" +
			                                              "</html>");
			mtd.addColumn("monDeviceIO",  "WritesPct",    "<html>" +
			                                                   "Of all the issued IO's, what's the Write percentage.<br>" +
			                                                   "<b>Formula</b>: Writes / (Reads + Writes) * 100<br>" +
			                                              "</html>");
			mtd.addColumn("monDeviceIO",  "AvgServ_ms",   "<html>" +
			                                                   "Service time on the disk.<br>" +
			                                                   "This is basically the average time it took to make a disk IO on this device.<br>" +
			                                                   "Warning: ASE isn't timing each IO individually, Instead it uses the 'click ticks' to do it... This might change in the future.<br>" +
			                                                   "<b>Formula</b>: IOTime / (Reads + Writes) <br>" +
			                                                   "<b>Note</b>: If there is few I/O's this value might be a bit off, this due to 'click ticks' is 100 ms by default.<br>" +
			                                              "</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("LogicalName");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		if (isClusterEnabled)
		{
			cols1 += "InstanceID, ";
		}

		String TotalIOs = "TotalIOs = (Reads + Writes)";
		if (aseVersion > 15000) 
			TotalIOs = "TotalIOs = convert(bigint,Reads) + convert(bigint,Writes)";

		String DeviceType = "";
		if (aseVersion >= 15020) 
		{
			DeviceType = 
				"DeviceType = CASE \n" +
				"               WHEN getdevicetype(PhysicalName) = 1 THEN 'RAW Device  '\n" +
				"               WHEN getdevicetype(PhysicalName) = 2 THEN 'BLOCK Device'\n" +
				"               WHEN getdevicetype(PhysicalName) = 3 THEN 'File        '\n" +
				"               ELSE '-unknown-'+convert(varchar(5), getdevicetype(PhysicalName))+'-'\n" +
				"             END,\n";
		}
		
		cols1 += "LogicalName, "+TotalIOs+", \n" +
		         "Reads, \n" +
		         "ReadsPct = CASE WHEN Reads + Writes > 0 \n" +
		         "                THEN convert(numeric(10,1), (Reads + 0.0) / (Reads + Writes + 0.0) * 100.0 ) \n" +
		         "                ELSE convert(numeric(10,1), 0.0 ) \n" +
		         "           END, \n" +
		         "APFReads, \n" +
		         "APFReadsPct = CASE WHEN Reads > 0 \n" +
		         "                   THEN convert(numeric(10,1), (APFReads + 0.0) / (Reads + 0.0) * 100.0 ) \n" +
		         "                   ELSE convert(numeric(10,1), 0.0 ) \n" +
		         "              END, \n" +
		         "Writes, \n" +
		         "WritesPct = CASE WHEN Reads + Writes > 0 \n" +
		         "                 THEN convert(numeric(10,1), (Writes + 0.0) / (Reads + Writes + 0.0) * 100.0 ) \n" +
		         "                 ELSE convert(numeric(10,1), 0.0 ) \n" +
		         "            END, \n" +
		         "DevSemaphoreRequests, DevSemaphoreWaits, IOTime, \n";
		cols2 += "AvgServ_ms = CASE \n" +
				 "               WHEN Reads+Writes>0 \n" +
				 "               THEN convert(numeric(10,1), IOTime / convert(numeric(10,0), Reads+Writes)) \n" +
				 "               ELSE convert(numeric(10,1), null) \n" +
				 "             END \n";
		cols3 += ", "+DeviceType+" PhysicalName";
		if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
		{
		}

		String sql = 
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from master..monDeviceIO\n" +
			"order by LogicalName" + (isClusterEnabled ? ", InstanceID" : "") + "\n";

		return sql;
	}

	/** 
	 * Compute the avgServ column, which is IOTime/(Reads+Writes)
	 */
	@Override
	public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
	{
		int AvgServ_msId=-1, ReadsPctId=-1, APFReadsPctId=-1, WritesPctId=-1;

		int Reads,      APFReads,      Writes,      IOTime;
		int ReadsId=-1, APFReadsId=-1, WritesId=-1, IOTimeId=-1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;
		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("Reads"))       ReadsId       = colId;
			else if (colName.equals("APFReads"))    APFReadsId    = colId;
			else if (colName.equals("Writes"))      WritesId      = colId;
			else if (colName.equals("IOTime"))      IOTimeId      = colId;

			else if (colName.equals("ReadsPct"))    ReadsPctId    = colId;
			else if (colName.equals("APFReadsPct")) APFReadsPctId = colId;
			else if (colName.equals("WritesPct"))   WritesPctId   = colId;
			else if (colName.equals("AvgServ_ms"))  AvgServ_msId  = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			Reads    = ((Number) diffData.getValueAt(rowId, ReadsId))   .intValue();
			APFReads = ((Number) diffData.getValueAt(rowId, APFReadsId)).intValue();
			Writes   = ((Number) diffData.getValueAt(rowId, WritesId))  .intValue();
			IOTime   = ((Number) diffData.getValueAt(rowId, IOTimeId))  .intValue();

			//--------------------
			//---- AvgServ_ms
			int totIo = Reads + Writes;
			if (totIo != 0)
			{
				// AvgServ_ms = (IOTime * 1000) / ( totIo);
				double calc = (IOTime + 0.0) / totIo;

				BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);;
				diffData.setValueAt(newVal, rowId, AvgServ_msId);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, AvgServ_msId);

			//--------------------
			//---- ReadsPct
			if (totIo > 0)
			{
				double calc = (Reads + 0.0) / (Reads + Writes + 0.0) * 100.0;

				BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, ReadsPctId);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, ReadsPctId);

			//--------------------
			//---- APFReadsPct
			if (Reads > 0)
			{
				double calc = (APFReads + 0.0) / (Reads + 0.0) * 100.0;

				BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, APFReadsPctId);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, APFReadsPctId);

			//--------------------
			//---- WritesPct
			if (totIo > 0)
			{
				double calc = (Writes + 0.0) / (Reads + Writes + 0.0) * 100.0;

				BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, WritesPctId);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, WritesPctId);
		}
	}
}

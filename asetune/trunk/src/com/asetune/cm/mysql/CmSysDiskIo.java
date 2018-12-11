package com.asetune.cm.mysql;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.mysql.gui.CmSysDiskIoPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSysDiskIo
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmSysIndexStats.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSysDiskIo.class.getSimpleName();
	public static final String   SHORT_NAME       = "Disk IO";
	public static final String   HTML_DESC        = 
		"<html>"
		+ "<h4>Disk IO</h4>"
		+ "Simply: select * from sys.x$io_global_by_file_by_latency"
		+ "</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"x$io_global_by_file_by_latency"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			  "total"         // The total number of I/O events for the file.
			, "total_latency" // The total wait time of timed I/O events for the file.
			, "count_read"    // The total number of read I/O events for the file.
			, "read_latency"  // The total wait time of timed read I/O events for the file.
			, "count_write"   // The total number of write I/O events for the file.
			, "write_latency" // The total wait time of timed write I/O events for the file.
			, "count_misc"    // The total number of other I/O events for the file.
			, "misc_latency"  // The total wait time of timed other I/O events for the file.
			};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.ALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmSysDiskIo(counterController, guiController);
	}

	public CmSysDiskIo(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);
		setBackgroundDataPollingEnabled(true, false);
		
		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX = CM_NAME;

	public static final String  PROPKEY_sample_LatencyInMs = PROP_PREFIX + ".sample.latencyInMs";
	public static final boolean DEFAULT_sample_LatencyInMs = true;

	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmSysDiskIoPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			// https://dev.mysql.com/doc/refman/5.7/en/innodb-buffer-pool-stats-table.html
			
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("x$io_global_by_file_by_latency",  "These views summarize processlist information. They provide more complete information than the SHOW PROCESSLIST statement and the INFORMATION_SCHEMA PROCESSLIST table, and are also nonblocking. By default, rows are sorted by descending process time and descending wait time.");

			mtd.addColumn("x$io_global_by_file_by_latency", "file",          "<html> The file path name.</html>");
			mtd.addColumn("x$io_global_by_file_by_latency", "total",         "<html> The total number of I/O events for the file.</html>");
			mtd.addColumn("x$io_global_by_file_by_latency", "total_latency", "<html> The total wait time of timed I/O events for the file.</html>");
			mtd.addColumn("x$io_global_by_file_by_latency", "count_read",    "<html> The total number of read I/O events for the file.</html>");
			mtd.addColumn("x$io_global_by_file_by_latency", "read_latency",  "<html> The total wait time of timed read I/O events for the file.</html>");
			mtd.addColumn("x$io_global_by_file_by_latency", "count_write",   "<html> The total number of write I/O events for the file.</html>");
			mtd.addColumn("x$io_global_by_file_by_latency", "write_latency", "<html> The total wait time of timed write I/O events for the file.</html>");
			mtd.addColumn("x$io_global_by_file_by_latency", "count_misc",    "<html> The total number of other I/O events for the file.</html>");
			mtd.addColumn("x$io_global_by_file_by_latency", "misc_latency",  "<html> The total wait time of timed other I/O events for the file.</html>");

			// extra columns added in the SQL statement
			mtd.addColumn("x$io_global_by_file_by_latency", "total_lpc",     "<html> Wait time per count <br> lpc = Latency Per Count. <br> Formula: total_latency / total </html>");
			mtd.addColumn("x$io_global_by_file_by_latency", "read_lpc",      "<html> Wait time per count <br> lpc = Latency Per Count. <br> Formula: read_latency / count_read</html>");
			mtd.addColumn("x$io_global_by_file_by_latency", "write_lpc",     "<html> Wait time per count <br> lpc = Latency Per Count. <br> Formula: write_latency / count_write </html>");
			mtd.addColumn("x$io_global_by_file_by_latency", "misc_lpc",      "<html> Wait time per count <br> lpc = Latency Per Count. <br> Formula: misc_latency / count_misc</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("file");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		boolean sampleLatencyInMs = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_LatencyInMs, DEFAULT_sample_LatencyInMs);

//		String sql = "select * from sys.`x$io_global_by_file_by_latency`";

		String total_latency = "total_latency";
		String read_latency  = "read_latency";
		String write_latency = "write_latency";
		String misc_latency  = "misc_latency";

		if (sampleLatencyInMs)
		{
			total_latency = "(total_latency / 1000000)";
			read_latency  = "(read_latency  / 1000000)";
			write_latency = "(write_latency / 1000000)";
			misc_latency  = "(misc_latency  / 1000000)";
		}
		
		String sql = ""
			    + "select \n"
			    + "    file, \n"
			    + "    total, \n"
			    + "    " + total_latency + " as total_latency, \n"
			    + "    count_read, \n"
			    + "    " + read_latency  + " as read_latency, \n"
			    + "    count_write, \n"
			    + "    " + write_latency + " as write_latency, \n"
			    + "    count_misc, \n"
			    + "    " + misc_latency  + " as misc_latency, \n"
			    + "    CASE WHEN total        = 0 THEN 0 ELSE " + total_latency + " / total       END as total_lpc, \n"
			    + "    CASE WHEN read_latency = 0 THEN 0 ELSE " + read_latency  + " / count_read  END as read_lpc, \n"
			    + "    CASE WHEN count_write  = 0 THEN 0 ELSE " + write_latency + " / count_write END as write_lpc, \n"
			    + "    CASE WHEN count_misc   = 0 THEN 0 ELSE " + misc_latency  + " / count_misc  END as misc_lpc \n"
			    + "from sys.`x$io_global_by_file_by_latency` \n"
			    + "";

		return sql;
	}





	/** 
	 * Compute the WaitTimePerWait for diff values
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int total_pos       = -1, total_latency_pos = -1;
		int count_read_pos  = -1, read_latency_pos  = -1;
		int count_write_pos = -1, write_latency_pos = -1;
		int count_misc_pos  = -1, misc_latency_pos  = -1;
		int total_lpc_pos   = -1, read_lpc_pos = -1, write_lpc_pos = -1, misc_lpc_pos = -1;
		
		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("total"))          total_pos         = colId;
			else if (colName.equals("total_latency"))  total_latency_pos = colId;
			else if (colName.equals("count_read"))     count_read_pos    = colId;
			else if (colName.equals("read_latency"))   read_latency_pos  = colId;
			else if (colName.equals("count_write"))    count_write_pos   = colId;
			else if (colName.equals("write_latency"))  write_latency_pos = colId;
			else if (colName.equals("count_misc"))     count_misc_pos    = colId;
			else if (colName.equals("misc_latency"))   misc_latency_pos  = colId;
			
			else if (colName.equals("total_lpc"))      total_lpc_pos     = colId;
			else if (colName.equals("read_lpc"))       read_lpc_pos      = colId;
			else if (colName.equals("write_lpc"))      write_lpc_pos     = colId;
			else if (colName.equals("misc_lpc"))       misc_lpc_pos      = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			calcWaitPerCount(diffData, rowId, total_pos,       total_latency_pos, total_lpc_pos);
			calcWaitPerCount(diffData, rowId, count_read_pos,  read_latency_pos,  read_lpc_pos);
			calcWaitPerCount(diffData, rowId, count_write_pos, write_latency_pos, write_lpc_pos);
			calcWaitPerCount(diffData, rowId, count_misc_pos,  misc_latency_pos,  misc_lpc_pos);
		}
	}
	private void calcWaitPerCount(CounterSample cs, int rowId, int count_pos, int latency_pos, int dest_pos)
	{
		long count   = ((Number)cs.getValueAt(rowId, count_pos  )).longValue();
		long latency = ((Number)cs.getValueAt(rowId, latency_pos)).longValue();
		
		if (count > 0)
		{
			BigDecimal newVal = new BigDecimal( (latency/(count*1.0)) ).setScale(3, BigDecimal.ROUND_HALF_EVEN);;
			cs.setValueAt(newVal, rowId, dest_pos);
		}
		else
			cs.setValueAt(new BigDecimal(0), rowId, dest_pos);
	}
}

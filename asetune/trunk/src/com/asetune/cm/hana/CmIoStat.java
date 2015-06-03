package com.asetune.cm.hana;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmIoStat
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmServiceMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIoStat.class.getSimpleName();
	public static final String   SHORT_NAME       = "IO Perf Stat";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"M_VOLUME_IO_PERFORMANCE_STATISTICS"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
//		"MAX_IO_BUFFER", 								//	BIGINT		Max. IO buffer size
		"READ_SYNC", 									//	BIGINT		Count of synchronous reads
		"WRITE_SYNC", 									//	BIGINT		Count of synchronous writes
		"READ_REQUESTS", 								//	BIGINT		Count of read requests
		"WRITE_REQUESTS", 								//	BIGINT		Count of write requests
		"READ_COMPLETIONS", 							//	BIGINT		Count of read completions
		"FAILED_READS", 								//	BIGINT		Count of failed reads
		"WRITE_COMPLETIONS", 							//	BIGINT		Count of write completions
		"FAILED_WRITES", 								//	BIGINT		Count of failed writes
		"FULL_RETRY_READS", 							//	BIGINT		Count of full retry reads
		"FULL_RETRY_WRITES", 							//	BIGINT		Count of full retry writes
		"SHORT_READS", 									//	BIGINT		Count of short reads
		"SHORT_WRITES", 								//	BIGINT		Count of short writes
		"DELAYED_READ_REQUESTS",	 					//	BIGINT		Count of delayed read requests
		"DELAYED_WRITE_REQUESTS", 						//	BIGINT		Count of delayed write requests
		"DEQUEUED_DELAYED_REQUESTS", 					//	BIGINT		Count of dequeued delayed requests
		"RESUBMITTED_DELAYED_REQUESTS", 				//	BIGINT		Count of resubmitted delayed requests
		"RESUBMITTED_DELAYED_REQUESTS_DELAYED_AGAIN", 	//	BIGINT		Count of resubmitted delayed requests delayed
		"ITEMS_IN_DELAY_QUEUE", 						//	BIGINT		Count of items in delay queue
//		"MAX_ITEMS_IN_DELAY_QUEUE", 					//	BIGINT		Maximum size of delay queue
//		"LAST_READ_SYNC_TIME", 							//	BIGINT		Time for synchronous reads (last)
//		"MAX_READ_SYNC_TIME", 							//	BIGINT		Time for synchronous reads (max)
//		"MIN_READ_SYNC_TIME", 							//	BIGINT		Time for synchronous reads (min)
		"SUM_READ_SYNC_TIME", 							//	BIGINT		Time for synchronous reads (total)
//		"AVG_READ_SYNC_TIME", 							//	BIGINT		Time for synchronous reads (avg)
//		"LAST_WRITE_SYNC_TIME", 						//	BIGINT		Time for synchronous writes (last)
//		"MAX_WRITE_SYNC_TIME", 							//	BIGINT		Time for synchronous writes (max)
//		"MIN_WRITE_SYNC_TIME", 							//	BIGINT		Time for synchronous writes (min)
		"SUM_WRITE_SYNC_TIME", 							//	BIGINT		Time for synchronous writes (total)
//		"AVG_WRITE_SYNC_TIME", 							//	BIGINT		Time for synchronous writes (avg)
//		"LAST_READ_TIME", 								//	BIGINT		Time for read events (last)
//		"MAX_READ_TIME", 								//	BIGINT		Time for read events (max)
//		"MIN_READ_TIME", 								//	BIGINT		Time for read events (min)
		"SUM_READ_TIME", 								//	BIGINT		Time for read events (total)
//		"AVG_READ_TIME", 								//	BIGINT		Time for read events (avg)
//		"LAST_WRITE_TIME", 								//	BIGINT		Time for write events (last)
//		"MAX_WRITE_TIME", 								//	BIGINT		Time for write events (max)
//		"MIN_WRITE_TIME", 								//	BIGINT		Time for write events (min)
		"SUM_WRITE_TIME", 								//	BIGINT		Time for write events (total)
//		"AVG_WRITE_TIME", 								//	BIGINT		Time for write events (avg)
//		"LAST_READ_ENQUEUE_TIME", 						//	BIGINT		Time for enqueuing read I/O events (last)
//		"MAX_READ_ENQUEUE_TIME", 						//	BIGINT		Time for enqueuing read I/O events (max)
//		"MIN_READ_ENQUEUE_TIME", 						//	BIGINT		Time for enqueuing read I/O events (min)
		"SUM_READ_ENQUEUE_TIME", 						//	BIGINT		Time for enqueuing read I/O events (total)
//		"AVG_READ_ENQUEUE_TIME", 						//	BIGINT		Time for enqueuing read I/O events (avg)
//		"LAST_WRITE_ENQUEUE_TIME", 						//	BIGINT		Time for enqueuing write I/O events (last)
//		"MAX_WRITE_ENQUEUE_TIME", 						//	BIGINT		Time for enqueuing write I/O events (max)
//		"MIN_WRITE_ENQUEUE_TIME", 						//	BIGINT		Time for enqueuing write I/O events (min)
		"SUM_WRITE_ENQUEUE_TIME", 						//	BIGINT		Time for enqueuing write I/O events (total)
//		"AVG_WRITE_ENQUEUE_TIME", 						//	BIGINT		Time for enqueuing write I/O events (avg)
//		"LAST_READ_SIZE", 								//	BIGINT		Size of read data (last)
//		"MAX_READ_SIZE", 								//	BIGINT		Size of read data (max)
//		"MIN_READ_SIZE", 								//	BIGINT		Size of read data (min)
		"SUM_READ_SIZE", 								//	BIGINT		Size of read data (total)
//		"AVG_READ_SIZE", 								//	BIGINT		Size of read data (avg)
//		"LAST_WRITE_SIZE", 								//	BIGINT		Size of written data (last)
//		"MAX_WRITE_SIZE", 								//	BIGINT		Size of written data (max)
//		"MIN_WRITE_SIZE", 								//	BIGINT		Size of written data (min)
		"SUM_WRITE_SIZE", 								//	BIGINT		Size of written data (total)
//		"AVG_WRITE_SIZE", 								//	BIGINT		Size of written data (avg)
//		"LAST_READ_SYNC_SIZE", 							//	BIGINT		Size of synchronously read data (last)
//		"MAX_READ_SYNC_SIZE", 							//	BIGINT		Size of synchronously read data (max)
//		"MIN_READ_SYNC_SIZE", 							//	BIGINT		Size of synchronously read data (min)
		"SUM_READ_SYNC_SIZE", 							//	BIGINT		Size of synchronously read data (total)
//		"AVG_READ_SYNC_SIZE", 							//	BIGINT		Size of synchronously read data (avg)
//		"LAST_WRITE_SYNC_SIZE", 						//	BIGINT		Size of synchronously written data (last)
//		"MAX_WRITE_SYNC_SIZE", 							//	BIGINT		Size of synchronously written data (max)
//		"MIN_WRITE_SYNC_SIZE", 							//	BIGINT		Size of synchronously written data (min)
		"SUM_WRITE_SYNC_SIZE", 							//	BIGINT		Size of synchronously written data (total)
//		"AVG_WRITE_SYNC_SIZE", 							//	BIGINT		Size of synchronously written data (avg) 		
		"-last-dummy-col-"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

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

		return new CmIoStat(counterController, guiController);
	}

	public CmIoStat(ICounterController counterController, IGuiController guiController)
	{
		super(CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
	private void addTrendGraphs()
	{
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmRaSysmonPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("HOST");
		pkCols.add("PORT");
		pkCols.add("VOLUME_ID");
		pkCols.add("TYPE");
		pkCols.add("MAX_IO_BUFFER");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = "select * from M_VOLUME_IO_PERFORMANCE_STATISTICS";

		return sql;
	}
}

package com.asetune.cm.sqlserver;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSchedulers
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmSchedulers.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSchedulers.class.getSimpleName();
	public static final String   SHORT_NAME       = "Schedulers";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_os_schedulers"};
	public static final String[] NEED_ROLES       = new String[] {}; // VIEW SERVER STATE
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"preemptive_switches_count",
		"context_switches_count",
		"idle_switches_count",
		"current_tasks_count",
//		"runnable_tasks_count",
		"current_workers_count",
		"active_workers_count",
//		"work_queue_count",
//		"pending_disk_io_count",
//		"load_factor",
		"yield_count",
		"last_timer_activity"
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

		return new CmSchedulers(counterController, guiController);
	}

	public CmSchedulers(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmRaSysmonPanel(this);
//	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
//	Create graph: Sheduler Queue: runnable_tasks_count
//	Create graph: Outstanding IO: pending_disk_io_count
		
	public static final String GRAPH_NAME_RUN_QUEUE_LENGTH_SUM   = "RunQLengthSum";
	public static final String GRAPH_NAME_RUN_QUEUE_LENGTH_ENG   = "RunQLengthEng";
	public static final String GRAPH_NAME_PENDING_IO_SUM         = "PendingIoSum";
	public static final String GRAPH_NAME_PENDING_IO_ENG         = "PendingIoEng";
	

	private void addTrendGraphs()
	{
//		String[] labels_runQueueLength  = new String[] { "Sum: runnable_tasks_count", "Avg: runnable_tasks_count" };
//		String[] labels_pendingIo       = new String[] { "Sum: pending_disk_io_count", "Avg: pending_disk_io_count" };
////		String[] labels_runtimeReplaced = new String[] { "-runtime-replaced-" };
//		String[] labels_runtimeReplaced = TrendGraphDataPoint.RUNTIME_REPLACED_LABELS;
//
//		addTrendGraphData(GRAPH_NAME_RUN_QUEUE_LENGTH_SUM, new TrendGraphDataPoint(GRAPH_NAME_RUN_QUEUE_LENGTH_SUM, labels_runQueueLength,  LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_RUN_QUEUE_LENGTH_ENG, new TrendGraphDataPoint(GRAPH_NAME_RUN_QUEUE_LENGTH_ENG, labels_runtimeReplaced, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_PENDING_IO_SUM,       new TrendGraphDataPoint(GRAPH_NAME_PENDING_IO_SUM,       labels_pendingIo,       LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_PENDING_IO_ENG,       new TrendGraphDataPoint(GRAPH_NAME_PENDING_IO_ENG,       labels_runtimeReplaced, LabelType.Dynamic));

		// GRAPH: Run Queue Length
		addTrendGraph(GRAPH_NAME_RUN_QUEUE_LENGTH_SUM,
			"Runnable Queue Length, Summary", 	                        // Menu CheckBox text
			"Runnable Queue Length, Summary (using dm_os_schedulers.runnable_tasks_count)", // Label 
			new String[] { "Sum: runnable_tasks_count", "Avg: runnable_tasks_count" },
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH: Run Queue Length
		addTrendGraph(GRAPH_NAME_RUN_QUEUE_LENGTH_ENG,
			"Runnable Queue Length, per Scheduler", 	                        // Menu CheckBox text
			"Runnable Queue Length, per Scheduler (using dm_os_schedulers.runnable_tasks_count)", // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH: Outstanding IO's
		addTrendGraph(GRAPH_NAME_PENDING_IO_SUM,
			"Outstanding IO Requests, Summary", 	                        // Menu CheckBox text
			"Outstanding IO Requests, Summary (using dm_os_schedulers.pending_disk_io_count)", // Label 
			new String[] { "Sum: pending_disk_io_count", "Avg: pending_disk_io_count" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH: Outstanding IO's
		addTrendGraph(GRAPH_NAME_PENDING_IO_ENG,
			"Outstanding IO Requests, per Scheduler", 	                        // Menu CheckBox text
			"Outstanding IO Requests, per Scheduler (using dm_os_schedulers.pending_disk_io_count)", // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			TrendGraph tg = null;
//
//			// GRAPH: Run Queue Length
//			tg = new TrendGraph(GRAPH_NAME_RUN_QUEUE_LENGTH_SUM,
//				"Runnable Queue Length, Summary", 	                        // Menu CheckBox text
//				"Runnable Queue Length, Summary (using dm_os_schedulers.runnable_tasks_count)", // Label 
//				labels_runQueueLength, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH: Run Queue Length
//			tg = new TrendGraph(GRAPH_NAME_RUN_QUEUE_LENGTH_ENG,
//				"Runnable Queue Length, per Scheduler", 	                        // Menu CheckBox text
//				"Runnable Queue Length, per Scheduler (using dm_os_schedulers.runnable_tasks_count)", // Label 
//				labels_runQueueLength, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH: Outstanding IO's
//			tg = new TrendGraph(GRAPH_NAME_PENDING_IO_SUM,
//				"Outstanding IO Requests, Summary", 	                        // Menu CheckBox text
//				"Outstanding IO Requests, Summary (using dm_os_schedulers.pending_disk_io_count)", // Label 
//				labels_pendingIo, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH: Outstanding IO's
//			tg = new TrendGraph(GRAPH_NAME_PENDING_IO_ENG,
//				"Outstanding IO Requests, per Scheduler", 	                        // Menu CheckBox text
//				"Outstanding IO Requests, per Scheduler (using dm_os_schedulers.pending_disk_io_count)", // Label 
//				labels_pendingIo, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
	}
		
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
//		long srvVersion = getServerVersion();

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_RUN_QUEUE_LENGTH_SUM.equals(tgdp.getName()))
		{
			int[] rowIds = this.getAbsRowIdsWhere("status", "VISIBLE ONLINE");
			if (rowIds == null)
				_logger.warn("When updateGraphData for '"+tgdp.getName()+"', getAbsRowIdsWhere('status', 'VISIBLE ONLINE'), retuned null, so I can't do more here.");
			else
			{
				Double[] arr = new Double[2];
				arr[0] = this.getAbsValueSum(rowIds, "runnable_tasks_count");
				arr[1] = this.getAbsValueAvg(rowIds, "runnable_tasks_count");

				if (_logger.isDebugEnabled())
					_logger.debug("updateGraphData("+tgdp.getName()+"): runnable_tasks_count(sum)='"+arr[0]+"', runnable_tasks_count(avg)='"+arr[1]+"'.");

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_RUN_QUEUE_LENGTH_ENG.equals(tgdp.getName()))
		{
			// Get a array of rowId's where the column 'status' has the value 'VISIBLE ONLINE'
			int[] rowIds = this.getAbsRowIdsWhere("status", "VISIBLE ONLINE");
			if (rowIds == null)
				_logger.warn("When updateGraphData for '"+tgdp.getName()+"', getAbsRowIdsWhere('status', 'VISIBLE ONLINE'), retuned null, so I can't do more here.");
			else
			{
				Double[] data  = new Double[rowIds.length];
				String[] label = new String[rowIds.length];
				for (int i=0; i<rowIds.length; i++)
				{
					int rowId = rowIds[i];

					// get LABEL
					label[i] = "sch-" + this.getAbsString(rowId, "scheduler_id");

					// get DATA
					data[i]  = this.getAbsValueAsDouble(rowId, "runnable_tasks_count");
				}
				if (_logger.isDebugEnabled())
				{
					String debugStr = "";
					for (int i=0; i<data.length; i++)
						debugStr += label[i] + "='"+data[i]+"', ";
					_logger.debug("updateGraphData("+tgdp.getName()+"): "+debugStr);
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), label, data);
			}
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_PENDING_IO_SUM.equals(tgdp.getName()))
		{	
			int[] rowIds = this.getAbsRowIdsWhere("status", "VISIBLE ONLINE");
			if (rowIds == null)
				_logger.warn("When updateGraphData for '"+tgdp.getName()+"', getAbsRowIdsWhere('status', 'VISIBLE ONLINE'), retuned null, so I can't do more here.");
			else
			{
				Double[] arr = new Double[2];
				arr[0] = this.getAbsValueSum(rowIds, "pending_disk_io_count");
				arr[1] = this.getAbsValueAvg(rowIds, "pending_disk_io_count");

				if (_logger.isDebugEnabled())
					_logger.debug("updateGraphData("+tgdp.getName()+"): pending_disk_io_count(sum)='"+arr[0]+"', pending_disk_io_count(avg)='"+arr[1]+"'.");

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_PENDING_IO_ENG.equals(tgdp.getName()))
		{	
			// Get a array of rowId's where the column 'status' has the value 'VISIBLE ONLINE'
			int[] rowIds = this.getAbsRowIdsWhere("status", "VISIBLE ONLINE");
			if (rowIds == null)
				_logger.warn("When updateGraphData for '"+tgdp.getName()+"', getAbsRowIdsWhere('status', 'VISIBLE ONLINE'), retuned null, so I can't do more here.");
			else
			{
				Double[] data  = new Double[rowIds.length];
				String[] label = new String[rowIds.length];
				for (int i=0; i<rowIds.length; i++)
				{
					int rowId = rowIds[i];

					// get LABEL
					label[i] = "sch-" + this.getAbsString(rowId, "scheduler_id");

					// get DATA
					data[i]  = this.getAbsValueAsDouble(rowId, "pending_disk_io_count");
				}
				if (_logger.isDebugEnabled())
				{
					String debugStr = "";
					for (int i=0; i<data.length; i++)
						debugStr += label[i] + "='"+data[i]+"', ";
					_logger.debug("updateGraphData("+tgdp.getName()+"): "+debugStr);
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), label, data);
			}
		}
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("dm_os_schedulers",  "Returns one row per scheduler in SQL Server where each scheduler is mapped to an individual processor. Use this view to monitor the condition of a scheduler or to identify runaway tasks..");

			mtd.addColumn("dm_os_schedulers", "scheduler_address",          "<html>Memory address of the scheduler. Is not nullable.</html>");
			mtd.addColumn("dm_os_schedulers", "parent_node_id",             "<html>ID of the node that the scheduler belongs to, also known as the parent node. This represents a nonuniform memory access (NUMA) node.</html>");
			mtd.addColumn("dm_os_schedulers", "scheduler_id",               "<html>ID of the scheduler. <br>"
			                                                                  + "All schedulers that are used to run regular queries have ID numbers less than 1048576. <br>"
			                                                                  + "Those schedulers that have IDs greater than or equal to 1048576 are used internally by SQL Server, such as the dedicated administrator connection scheduler.</html>");
			mtd.addColumn("dm_os_schedulers", "cpu_id",                     "<html>CPU ID assigned to the scheduler.<br>"
			                                                                  + "<b>Note:</b> 255 does not indicate no affinity as it did in SQL Server 2005. See sys.dm_os_threads (Transact-SQL) for additional affinity information."
			                                                                  + "</html>");
			mtd.addColumn("dm_os_schedulers", "status",                     "<html>Indicates the status of the scheduler. Can be one of the following values"
			                                                                  + "<ul>"
			                                                                  + "  <li>HIDDEN ONLINE</li>"
			                                                                  + "  <li>HIDDEN OFFLINE</li>"
			                                                                  + "  <li>VISIBLE ONLINE</li>"
			                                                                  + "  <li>VISIBLE OFFLINE</li>"
			                                                                  + "  <li>VISIBLE ONLINE (DAC)</li>"
			                                                                  + "  <li>HOT_ADDED</li>"
			                                                                  + "</ul>"
			                                                                  + "HIDDEN schedulers are used to process requests that are internal to the Database Engine. VISIBLE schedulers are used to process user requests. <br>"
			                                                                  + "<br>"
			                                                                  + "OFFLINE schedulers map to processors that are offline in the affinity mask and are, therefore, not being used to process any requests. ONLINE schedulers map to processors that are online in the affinity mask and are available to process threads.<br>"
			                                                                  + "<br>"
			                                                                  + "DAC indicates the scheduler is running under a dedicated administrator connection.<br>"
			                                                                  + "<br>"
			                                                                  + "HOT ADDED indicates the schedulers were added in response to a hot add CPU event.<br>"
			                                                                  + "<br>"
			                                                                  + "</html>");
			mtd.addColumn("dm_os_schedulers", "is_online",                  "<html>If SQL Server is configured to use only some of the available processors on the server, this configuration can mean that some schedulers are mapped to processors that are not in the affinity mask. <br>"
			                                                                  + "If that is the case, this column returns 0. This value means that the scheduler is not being used to process queries or batches.</html>");
			mtd.addColumn("dm_os_schedulers", "is_idle",                    "<html>1 = Scheduler is idle. No workers are currently running.</html>");
			mtd.addColumn("dm_os_schedulers", "preemptive_switches_count",  "<html>Number of times that workers on this scheduler have switched to the preemptive mode.<br>"
			                                                                  + "<br>"
			                                                                  + "To execute code that is outside SQL Server (for example, extended stored procedures and distributed queries), a thread has to execute outside the control of the non-preemptive scheduler. To do this, a worker switches to preemptive mode. </html>");
			mtd.addColumn("dm_os_schedulers", "context_switches_count",     "<html>Number of context switches that have occurred on this scheduler.<br>"
			                                                                  + "<br>"
			                                                                  + "To allow for other workers to run, the current running worker has to relinquish control of the scheduler or switch context.<br>"
			                                                                  + "<br>"
			                                                                  + "<b>Note:</b> If a worker yields the scheduler and puts itself into the runnable queue and then finds no other workers, the worker will select itself. "
			                                                                  + "In this case, the context_switches_count is not updated, but the yield_count is updated. </html>");
			mtd.addColumn("dm_os_schedulers", "idle_switches_count",        "<html>Number of times the scheduler has been waiting for an event while idle. This column is similar to context_switches_count.</html>");
			mtd.addColumn("dm_os_schedulers", "current_tasks_count",        "<html>Number of current tasks that are associated with this scheduler. This count includes the following:"
			                                                                  + "<ul>"
			                                                                  + "  <li>Tasks that are waiting for a worker to execute them.</li>"
			                                                                  + "  <li>Tasks that are currently waiting or running (in SUSPENDED or RUNNABLE state).</li>"
			                                                                  + "</ul>"
			                                                                  + "When a task is completed, this count is decremented. Is not nullable."
			                                                                  + "</html>");
			mtd.addColumn("dm_os_schedulers", "runnable_tasks_count",       "<html>Number of workers, with tasks assigned to them, that are waiting to be scheduled on the runnable queue. </html>");
			mtd.addColumn("dm_os_schedulers", "current_workers_count",      "<html>Number of workers that are associated with this scheduler. This count includes workers that are not assigned any task.</html>");
			mtd.addColumn("dm_os_schedulers", "active_workers_count",       "<html>Number of workers that are active. An active worker is never preemptive, must have an associated task, and is either running, runnable, or suspended. </html>");
			mtd.addColumn("dm_os_schedulers", "work_queue_count",           "<html>Number of tasks in the pending queue. These tasks are waiting for a worker to pick them up.</html>");
			mtd.addColumn("dm_os_schedulers", "pending_disk_io_count",      "<html>Number of pending I/Os that are waiting to be completed. <br>"
			                                                                  + "Each scheduler has a list of pending I/Os that are checked to determine whether they have been completed every time there is a context switch. <br>"
			                                                                  + "The count is incremented when the request is inserted. This count is decremented when the request is completed. <br>"
			                                                                  + "This number does not indicate the state of the I/Os. </html>");
			mtd.addColumn("dm_os_schedulers", "load_factor",                "<html>Internal value that indicates the perceived load on this scheduler. <br>"
			                                                                  + "This value is used to determine whether a new task should be put on this scheduler or another scheduler. <br>"
			                                                                  + "This value is useful for debugging purposes when it appears that schedulers are not evenly loaded. <br>"
			                                                                  + "The routing decision is made based on the load on the scheduler. <br>"
			                                                                  + "SQL Server also uses a load factor of nodes and schedulers to help determine the best location to acquire resources. <br>"
			                                                                  + "When a task is enqueued, the load factor is increased. When a task is completed, the load factor is decreased. <br>"
			                                                                  + "Using the load factors helps SQL Server OS balance the work load better. </html>");
			mtd.addColumn("dm_os_schedulers", "yield_count",                "<html>Internal value that is used to indicate progress on this scheduler. <br>"
			                                                                  + "This value is used by the Scheduler Monitor to determine whether a worker on the scheduler is not yielding to other workers on time. <br>"
			                                                                  + "This value does not indicate that the worker or task transitioned to a new worker.</html>");
			mtd.addColumn("dm_os_schedulers", "last_timer_activity",        "<html>In CPU ticks, the last time that the scheduler timer queue was checked by the scheduler.</html>");
			mtd.addColumn("dm_os_schedulers", "failed_to_create_worker",    "<html>Set to 1 if a new worker could not be created on this scheduler. This generally occurs because of memory constraints.</html>");
			mtd.addColumn("dm_os_schedulers", "active_worker_address",      "<html>Memory address of the worker that is currently active. Is nullable. For more information, see sys.dm_os_workers (Transact-SQL).</html>");
			mtd.addColumn("dm_os_schedulers", "memory_object_address",      "<html>Memory address of the scheduler memory object</html>");
			mtd.addColumn("dm_os_schedulers", "task_memory_object_address", "<html>Memory address of the task memory object. Is not nullable. For more information, see sys.dm_os_memory_objects (Transact-SQL).</html>");
			mtd.addColumn("dm_os_schedulers", "quantum_length_us",          "<html>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed. Exposes the scheduler quantum used by SQLOS.</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("scheduler_id");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String sql = "select * from sys.dm_os_schedulers";

		return sql;
	}
}

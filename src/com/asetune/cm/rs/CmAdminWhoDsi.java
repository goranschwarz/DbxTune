package com.asetune.cm.rs;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.MonTablesDictionary;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmAdminWhoDsi
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminWhoDsi.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmAdminWhoDsi.class.getSimpleName();
	public static final String   SHORT_NAME       = "DSI";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Stable Queue Manager Statistics</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dsi"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
//		"Xact_retry_times",
//		"Cmd_batch_size",
//		"Xact_group_size",
//		"Max_cmds_to_log",
		"Xacts_read",
		"Xacts_ignored",
		"Xacts_skipped",
		"Xacts_succeeded",
		"Xacts_failed",
		"Xacts_retried",
//		"Current Origin DB",
		"Cmds_read",
		"Cmds_parsed_by_sqt",
		"Xacts_Sec_ignored",
//		"NumThreads",
//		"NumLargeThreads",
//		"LargeThreshold",
//		"CacheSize",
//		"Max_Xacts_in_group",
		"Xacts_retried_blk",
//		"CommitMaxChecks",
//		"CommitLogChecks",
//		"CommitCheckIntvl",
//		"RSTicket"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
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

		return new CmAdminWhoDsi(counterController, guiController);
	}

	public CmAdminWhoDsi(ICounterController counterController, IGuiController guiController)
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
	public void addMonTableDictForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
			mtd.addTable("dsi",  "");

			mtd.addColumn("dsi", "Spid",                  "<html>RepServer internal <i>thread id</i></html>");
			mtd.addColumn("dsi", "State",                 "<html>FIXME: State</html>");
			mtd.addColumn("dsi", "Info",                  "<html>FIXME: Info</html>");
			mtd.addColumn("dsi", "Maintenance User",      "<html>FIXME: Maintenance User     </html>");
			mtd.addColumn("dsi", "Xact_retry_times",      "<html>FIXME: Xact_retry_times     </html>");
			mtd.addColumn("dsi", "Batch",                 "<html>FIXME: Batch                </html>");
			mtd.addColumn("dsi", "Cmd_batch_size",        "<html>FIXME: Cmd_batch_size       </html>");
			mtd.addColumn("dsi", "Xact_group_size",       "<html>FIXME: Xact_group_size      </html>");
			mtd.addColumn("dsi", "Dump_load",             "<html>FIXME: Dump_load            </html>");
			mtd.addColumn("dsi", "Max_cmds_to_log",       "<html>FIXME: Max_cmds_to_log      </html>");
			mtd.addColumn("dsi", "Xacts_read",            "<html>FIXME: Xacts_read           </html>");
			mtd.addColumn("dsi", "Xacts_ignored",         "<html>FIXME: Xacts_ignored        </html>");
			mtd.addColumn("dsi", "Xacts_skipped",         "<html>FIXME: Xacts_skipped        </html>");
			mtd.addColumn("dsi", "Xacts_succeeded",       "<html>FIXME: Xacts_succeeded      </html>");
			mtd.addColumn("dsi", "Xacts_failed",          "<html>FIXME: Xacts_failed         </html>");
			mtd.addColumn("dsi", "Xacts_retried",         "<html>FIXME: Xacts_retried        </html>");
			mtd.addColumn("dsi", "Current Origin DB",     "<html>FIXME: Current Origin DB    </html>");
			mtd.addColumn("dsi", "Current Origin QID",    "<html>FIXME: Current Origin QID   </html>");
			mtd.addColumn("dsi", "Subscription Name",     "<html>FIXME: Subscription Name    </html>");
			mtd.addColumn("dsi", "Sub Command",           "<html>FIXME: Sub Command          </html>");
			mtd.addColumn("dsi", "Current Secondary QID", "<html>FIXME: Current Secondary QID</html>");
			mtd.addColumn("dsi", "Cmds_read",             "<html>FIXME: Cmds_read            </html>");
			mtd.addColumn("dsi", "Cmds_parsed_by_sqt",    "<html>FIXME: Cmds_parsed_by_sqt   </html>");
			mtd.addColumn("dsi", "IgnoringStatus",        "<html>FIXME: IgnoringStatus       </html>");
			mtd.addColumn("dsi", "Xacts_Sec_ignored",     "<html>FIXME: Xacts_Sec_ignored    </html>");
			mtd.addColumn("dsi", "GroupingStatus",        "<html>FIXME: GroupingStatus       </html>");
			mtd.addColumn("dsi", "TriggerStatus",         "<html>FIXME: TriggerStatus        </html>");
			mtd.addColumn("dsi", "ReplStatus",            "<html>FIXME: ReplStatus           </html>");
			mtd.addColumn("dsi", "NumThreads",            "<html>FIXME: NumThreads           </html>");
			mtd.addColumn("dsi", "NumLargeThreads",       "<html>FIXME: NumLargeThreads      </html>");
			mtd.addColumn("dsi", "LargeThreshold",        "<html>FIXME: LargeThreshold       </html>");
			mtd.addColumn("dsi", "CacheSize",             "<html>FIXME: CacheSize            </html>");
			mtd.addColumn("dsi", "Serialization",         "<html>FIXME: Serialization        </html>");
			mtd.addColumn("dsi", "Max_Xacts_in_group",    "<html>FIXME: Max_Xacts_in_group   </html>");
			mtd.addColumn("dsi", "Xacts_retried_blk",     "<html>FIXME: Xacts_retried_blk    </html>");
			mtd.addColumn("dsi", "CommitControl",         "<html>FIXME: CommitControl        </html>");
			mtd.addColumn("dsi", "CommitMaxChecks",       "<html>FIXME: CommitMaxChecks      </html>");
			mtd.addColumn("dsi", "CommitLogChecks",       "<html>FIXME: CommitLogChecks      </html>");
			mtd.addColumn("dsi", "CommitCheckIntvl",      "<html>FIXME: CommitCheckIntvl     </html>");
			mtd.addColumn("dsi", "IsolationLevel",        "<html>FIXME: IsolationLevel       </html>");
			mtd.addColumn("dsi", "dsi_rs_ticket_report",  "<html>FIXME: dsi_rs_ticket_report </html>");
			mtd.addColumn("dsi", "RSTicket",              "<html>FIXME: RSTicket             </html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("Spid");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		String sql = "admin who, dsi, no_trunc ";
		return sql;
	}
}

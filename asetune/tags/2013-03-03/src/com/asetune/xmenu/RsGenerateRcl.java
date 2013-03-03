package com.asetune.xmenu;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.tools.QueryWindow.RclViewer;
import com.asetune.utils.RepServerUtils;
import com.asetune.utils.RepServerUtils.ConfigEntry;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

public class RsGenerateRcl
extends XmenuActionBase 
{
//	private static Logger _logger = Logger.getLogger(RsGenerateRcl.class);

	/**
	 * 
	 */
	public RsGenerateRcl() 
	{
		super();
	}

	/**
	 */
	@Override 
	public void doWork() 
	{
		final Connection conn = getConnection();

		String nameCol = getParamValue(0);
		String infoCol = getParamValue(1);

//		SwingUtils.showInfoMessage(null, "Not yet implemented", "Not yet implemented.");

//		+----+---------+---------+-------------------------------+
//		|Spid|Name     |State    |Info                           |
//		+----+---------+---------+-------------------------------+
//		|    |DSI EXEC |Suspended|122(1) GORAN_1_DS.wsdb1        |
//		|    |DSI      |Suspended|122 GORAN_1_DS.wsdb1           |
//		|    |REP AGENT|Down     |GORAN_2_DS.wsdb2               |
//		|    |DSI EXEC |Down     |116(1) REP_CONNECTOR_1.dest1   |
//		|    |DSI EXEC |Down     |118(1) REP_CONNECTOR_HPUX.dest1|
//		|    |DSI EXEC |Down     |115(1) XE.XE                   |
//		+----+---------+---------+-------------------------------+

		String dsdb      = null;
		// Parse the INFO Columns 
		if ("DSI".equals(nameCol))
		{
			dsdb = infoCol.substring(infoCol.indexOf(" ")).trim();
		}
		else if ("DSI EXEC".equals(nameCol))
		{
			dsdb = infoCol.substring(infoCol.indexOf(" ")).trim();
		}
		else if ("REP AGENT".equals(nameCol))
		{
			dsdb = infoCol;
		}
//		else if ("RSI".equals(nameCol))
//		{
//			queueType = 0;
//		}
		else
		{
			SwingUtils.showInfoMessage(null, "Not yet implemented", "Dump Queue for '"+nameCol+"' can't be done or not yet implemented.");
			return;
		}
		
		final String dsdb_final = dsdb;
		
		// Create a Waitfor Dialog and Executor, then execute it.
		WaitForExecDialog wait = new WaitForExecDialog(getOwner(), "Reading Replication Server Configuration");

		WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
		{
			@Override
			public Object doWork()
			{
				String[] sa = dsdb_final.split("\\.");
				String ds = sa[0];
				String db = sa[1];

				boolean onlyChangedConfigs = false;
				Map<String, String> configDescription = RepServerUtils.getConfigDescriptions(conn);

				getWaitDialog().setState("Getting Physical Connection Configuration, for '"+ds+"."+db+"'.");

				// FIXME: Active/Standby DSI connections are not handled, then we should print the logical connection config
				
				StringBuilder sb = new StringBuilder();
				sb.append("\n");
				sb.append("/* CONNECTION: "+ds+"."+db).append(" */\n");
				boolean printedRecords = false;
				List<ConfigEntry> config = RepServerUtils.getConnectionConfig(conn, ds, db);
				for (ConfigEntry ce : config)
				{
					System.out.println("CE: "+ce);

					if ( (onlyChangedConfigs && ce.isConfigOptionChanged()) || ! onlyChangedConfigs )
					{
						printedRecords = true;

						String prefix = "   ";
						String cfgDesc = configDescription.get(ce.getConfigName());

						if (ce._dynamicConfigOption || ce._isDefaultConfigured)
							prefix = "-- ";

						sb.append(prefix)
							.append("alter connection to ")
							.append(StringUtil.left("\"" + ds + "\".\"" + db + "\"", 40))
							.append(" set ")
							.append(StringUtil.left(ce.getConfigName(), 30, true))
							.append(" to ")
							.append(StringUtil.left(ce.getConfigValue(), 40, true, "'"))
							.append(ce.getComments())
							.append(" Description='").append(cfgDesc).append("'.")
							.append("\n");
					}
				}
				if ( ! printedRecords )
					sb.append("      -- no local configurations").append("\n");

				return sb.toString();
			}
		}; // END: new WaitForExecDialog.BgExecutor()
		
		// Execute and WAIT
		String ddl = (String)wait.execAndWait(doWork);

		RclViewer rclViewer = new RclViewer(ddl);
		rclViewer.setVisible(true);
	}
}

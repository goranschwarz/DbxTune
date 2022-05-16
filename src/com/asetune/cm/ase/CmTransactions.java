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
package com.asetune.cm.ase;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEventLongRunningDetachedTransaction;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmTransactions
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmTransactions.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmTransactions.class.getSimpleName();
	public static final String   SHORT_NAME       = "Transactions";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Current Transactions in the system<br>" +
		"This can be used to check for Distributed Transactions etc...<br>" +
	"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"systransactions"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.LARGE; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmTransactions(counterController, guiController);
	}

	public CmTransactions(ICounterController counterController, IGuiController guiController)
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


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
	private void addTrendGraphs()
	{
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmCmTransactionsPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// no need to have PK, since we are NOT using "diff" counters
		return null;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return 
            "select \n" +
            "        t.spid,  \n" +
            "        t.loid, \n" +
            "        xactkey     = '0x' + bintostr(t.xactkey), \n" +
            "        type        = convert(varchar(11), v3.name), \n" +
            "        coordinator = convert(varchar(10), v4.name), \n" +
            "        t.starttime, \n" +
            "        ageInSec    = datediff(ss, t.starttime, getdate()), \n" +
            "        state       = convert(varchar(17), v1.name), \n" +
            "        connection  = convert(varchar(9),  v2.name), \n" +
            "        dbid        = t.masterdbid,  \n" +
            "        dbname      = db_name(t.masterdbid),  \n" +
            "        failover    = convert(varchar(26), v5.name), \n" +
            "        t.srvname, \n" +
            "        t.namelen, \n" +
            "        t.xactname \n" +
            "from master.dbo.systransactions t, \n" +
            "     master.dbo.spt_values v1, \n" +
            "     master.dbo.spt_values v2, \n" +
            "     master.dbo.spt_values v3, \n" +
            "     master.dbo.spt_values v4, \n" +
            "     master.dbo.spt_values v5  \n" +
            "where t.state       = v1.number and v1.type = 'T1' \n" +
            "  and t.connection  = v2.number and v2.type = 'T2' \n" +
            "  and t.type        = v3.number and v3.type = 'T3' \n" +
            "  and t.coordinator = v4.number and v4.type = 'T4' \n" +
            "  and t.failover    = v5.number and v5.type = 'T5' \n" +
            "order by t.xactkey, t.srvname, t.failover \n" +
            "";
	}
	
	
	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("systransactions",  "contains information about SAP ASE transactions, but it is not a normal table. Portions of the table are built dynamically when queried by a user, while other portions are stored in the master database");

			mtd.addColumn("systransactions", "spid",        "<html>Server process ID, or 0 if the process is detached</html>");
			mtd.addColumn("systransactions", "loid",        "<html>Lock owner ID</html>");
			mtd.addColumn("systransactions", "xactkey",     "<html>Unique SAP ASE transaction key</html>");
			mtd.addColumn("systransactions", "type",        "<html>Value indicating the type of transaction</html>");
			mtd.addColumn("systransactions", "coordinator", "<html>Value indicating the coordination method or protocol</html>");
			mtd.addColumn("systransactions", "starttime",   "<html>Date the transaction started</html>");
			mtd.addColumn("systransactions", "ageInSec",    "<html>Number of seconds since the transaction started</html>");
			mtd.addColumn("systransactions", "state",       "<html>Value indicating the current state of the transaction</html>");
			mtd.addColumn("systransactions", "connection",  "<html>Value indicating the connection state</html>");
			mtd.addColumn("systransactions", "dbid",        "<html>Starting database ID of the transaction</html>");
			mtd.addColumn("systransactions", "dbname",      "<html>Starting database NAME (this may be faulty if not local server) of the transaction</html>");
			mtd.addColumn("systransactions", "failover",    "<html>Value indicating the transaction failover state.</html>");
			mtd.addColumn("systransactions", "srvname",     "<html>Name of the remote server (null for local servers)</html>");
			mtd.addColumn("systransactions", "namelen",     "<html>Length of xactname</html>");
			mtd.addColumn("systransactions", "xactname",    "<html>Transaction name or XID</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}



	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		CountersModel cm = this;

		boolean debugPrint = System.getProperty("sendAlarmRequest.debug", "false").equalsIgnoreCase("true");
		//debugPrint = true;

		for (int r=0; r<cm.getAbsRowCount(); r++)
		{
			String state  = cm.getAbsString(r, "state");
			String dbname = cm.getAbsString(r, "dbname");
			
			if ("Detached".equalsIgnoreCase(state))
			{
				//-------------------------------------------------------
				// state -> Detached -> ageInSec
				//-------------------------------------------------------
				if (isSystemAlarmsForColumnEnabledAndInTimeRange("state[Detached]"))
				{
					Double ageInSec  = cm.getAbsValueAsDouble(r, "ageInSec");
					String xactname  = cm.getAbsString(r,        "xactname");
					
					if (ageInSec != null)
					{
						int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_StateDetached_ageInSec, DEFAULT_alarm_StateDetached_ageInSec);

						if (debugPrint || _logger.isDebugEnabled())
							System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", ageInSec='"+ageInSec+"'.");

						if (ageInSec.intValue() > threshold)
						{
							AlarmHandler.getInstance().addAlarm( 
								new AlarmEventLongRunningDetachedTransaction(cm, threshold, dbname, ageInSec, xactname) );
						}
					}
				}
			}
		}
	} // end: method

	public static final String  PROPKEY_alarm_StateDetached_ageInSec = CM_NAME + ".alarm.system.if.state[Detached].ageInSec.gt";
	public static final int     DEFAULT_alarm_StateDetached_ageInSec = 120;
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("state[Detached]", isAlarmSwitch, PROPKEY_alarm_StateDetached_ageInSec, Integer.class, conf.getIntProperty(PROPKEY_alarm_StateDetached_ageInSec, DEFAULT_alarm_StateDetached_ageInSec), DEFAULT_alarm_StateDetached_ageInSec, "If 'state' is in 'Detached' and '' is greater than X seconds, send 'AlarmEvent'." ));

		return list;
	}

}

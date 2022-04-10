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
package com.asetune.cm.postgres;

import java.awt.event.MouseEvent;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.AlarmHelper;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventLongRunningStatement;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.RegExpInputValidator;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.postgres.gui.CmPgActivityPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.pcs.PcsColumnOptions;
import com.asetune.pcs.PcsColumnOptions.ColumnType;
import com.asetune.sql.ResultSetMetaDataCached;
import com.asetune.sql.ResultSetMetaDataCached.Entry;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgActivity
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmPgActivity.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgActivity.class.getSimpleName();
	public static final String   SHORT_NAME       = "Active Statements";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Statemenets that are currently executing in the ASE." +
		"<br><br>" +
		"Table Background colors:" +
		"<ul>" +
		"    <li>GREEN  - active - The backend is executing a query.</li>" +
//		"    <li>WHITE  - idle - The backend is waiting for a new client command.</li>" +
		"    <li>YELLOW - idle in transaction -  The backend is in a transaction, but is not currently executing a query.</li>" +
		"    <li>PINK   - idle in transaction (aborted) -  This state is similar to idle in transaction, except one of the statements in the transaction caused an error.</li>" +
//		"    <li>XXX    - fastpath function call -  The backend is executing a fast-path function.</li>" +
//		"    <li>XXX    - disabled -  This state is reported if track_activities is disabled in this backend.</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_activity"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmPgActivity(counterController, guiController);
	}

	public CmPgActivity(ICounterController counterController, IGuiController guiController)
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

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmPgActivityPanel(this);
	}
	
	@Override
	public Map<String, PcsColumnOptions> getPcsColumnOptions()
	{
		Map<String, PcsColumnOptions> map = super.getPcsColumnOptions();

		// No settings in the super, create one, and set it at super
		if (map == null)
		{
			map = new HashMap<>();
			map.put("query", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
	}
	
	@Override
	public List<String> getPkForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("pid");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
	{
		return ""
				+ "select \n"
				+ "     * \n"
				+ "    ,CASE WHEN state != 'active' THEN NULL \n"
				+ "          ELSE cast(((EXTRACT('epoch' from CLOCK_TIMESTAMP()) - EXTRACT('epoch' from query_start)) * 1000) as int) \n"
				+ "     END as \"execTimeInMs\" \n"
				+ "from pg_catalog.pg_stat_activity";
	}

	/**
	 * Change data types (or length) for some column  
	 * <p>
	 * We could have done that by converting columns into varchar datatypes, but since we do: "select * from ..." 
	 * for forward/backward compatibility, this is done in the code instead...<br>
	 * When we switch to "column specified" SQL Statement, then we can get rid of this!  
	 */
	@Override
	public ResultSetMetaDataCached createResultSetMetaData(ResultSetMetaData rsmd) throws SQLException
	{
		ResultSetMetaDataCached rsmdc = super.createResultSetMetaData(rsmd);

		if (rsmdc == null)
			return null;
		
		// In PG x.y
		setColumnShorterLength(rsmdc, "application_name" , 60);  // text --> varchar(60)
		setColumnShorterLength(rsmdc, "client_addr"      , 30);  // text --> varchar(30)
		setColumnShorterLength(rsmdc, "client_hostname"  , 60);  // text --> varchar(60)
		setColumnShorterLength(rsmdc, "state"            , 30);  // text --> varchar(30)
//		setColumnShorterLength(rsmdc, "backend_xid"      , 20);  // xid  --- Already set to varchar(30) in com.asetune.sql.ddl.DbmsDdlResolverPostgres
//		setColumnShorterLength(rsmdc, "backend_xmin"     , 20);  // xid  --- Already set to varchar(30) in com.asetune.sql.ddl.DbmsDdlResolverPostgres
		
		// In PG 9.6
		setColumnShorterLength(rsmdc, "wait_event_type"  , 30);  // text --> varchar(30)
		setColumnShorterLength(rsmdc, "wait_event"       , 50);  // text --> varchar(50)

		// In PG 10
		setColumnShorterLength(rsmdc, "backend_type"     , 30);  // text --> varchar(30)
		
		return rsmdc;
	}

	private void setColumnShorterLength(ResultSetMetaDataCached rsmdc, String colName, int newLength)
	{
		int colPos = rsmdc.findColumn(colName);
		
		// return if column wasn't found
		if (colPos == -1)
			return;
		
		Entry colEntry = rsmdc.getEntry(colPos);
		if (colEntry.getPrecision() > newLength)
		{
			colEntry.setColumnType(Types.VARCHAR);
			colEntry.setColumnTypeName("varchar");
			colEntry.setPrecision(newLength);
		}
	}
	
	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		// query
		if ("query".equals(colName))
		{
			return cellValue == null ? null : toHtmlString(cellValue.toString());
		}
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
	/** add HTML around the string, and translate line breaks into <br> */
	private String toHtmlString(String in)
	{
		String str = StringUtil.makeApproxLineBreak(in, 150, 10, "\n");
		str = str.replaceAll("\\n", "<br>");
		if (in.indexOf("<html>")>=0 || in.indexOf("<HTML>")>=0)
			return str;
		return "<html><pre>" + str + "</pre></html>";
	}
	




//	@Override
//	public void sendAlarmRequest()
//	{
//		AlarmHelper.sendAlarmRequestForColumn(this, "application_name");
//		AlarmHelper.sendAlarmRequestForColumn(this, "usename");
//	}
//	
//	@Override
//	public List<CmSettingsHelper> getLocalAlarmSettings()
//	{
//		List<CmSettingsHelper> list = new ArrayList<>();
//		
//		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "application_name") );
//		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "usename") );
//		
//		return list;
//	}

	@Override
	public void sendAlarmRequest()
	{
		AlarmHelper.sendAlarmRequestForColumn(this, "application_name");
		AlarmHelper.sendAlarmRequestForColumn(this, "usename");

		sendAlarmRequestLocal();
	}
	
	public void sendAlarmRequestLocal()
	{
		if ( ! hasDiffData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;
		
		// EXIT EARLY if no alarm properties has been specified (since there can be *many* logins)
		boolean isAnyAlarmEnabled = false;
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("StatementExecInSec")) isAnyAlarmEnabled = true;
//		if (isSystemAlarmsForColumnEnabledAndInTimeRange("xxxxxxxxxxx"      )) isAnyAlarmEnabled = true;

		if (isAnyAlarmEnabled == false)
			return;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		AlarmHandler alarmHandler = AlarmHandler.getInstance();
		
		CountersModel cm = this;

		for (int r=0; r<cm.getDiffRowCount(); r++)
		{
			//-------------------------------------------------------
			// StatementExecInSec 
			// --->>> possibly move/copy this to CmActiveStatements
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("StatementExecInSec"))
			{
				// Only continue is "state" is "active"
				String state = cm.getDiffValue(r, "state") + "";
				if ("active".equals(state))
				{
					int execTimeInMs = -1;

					// get 'execTimeInMs'
					Object o_execTimeInMs = cm.getDiffValue(r, "execTimeInMs");
					if (o_execTimeInMs != null && o_execTimeInMs instanceof Number)
					{
						execTimeInMs = ((Number)o_execTimeInMs).intValue();
					}
//					else
//					{
//						// ok, lets 
//						Object o_query_start = cm.getDiffValue(r, "query_start");
//						if (o_query_start != null && o_query_start instanceof Timestamp)
//						{
//							// Get approximate server time here (when we last refreshed this CM)
//							execTimeInMs = this.getTimestamp().getTime() - ((Timestamp)o_query_start).getTime();
//						}
//					}
					
					int StatementExecInSec = (int)execTimeInMs / 1000;
					
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_StatementExecInSec, DEFAULT_alarm_StatementExecInSec);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", StatementExecInSec='"+StatementExecInSec+"'.");

					if (StatementExecInSec > threshold)
					{
						// Get config 'skip some known values'
						String skipDbnameRegExp   = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipDbname,   DEFAULT_alarm_StatementExecInSecSkipDbname);
						String skipLoginRegExp    = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipLogin,    DEFAULT_alarm_StatementExecInSecSkipLogin);
						String skipCmdRegExp      = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipCmd,      DEFAULT_alarm_StatementExecInSecSkipCmd);
						String skipTranNameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipTranName, DEFAULT_alarm_StatementExecInSecSkipTranName);

						String StatementStartTime = cm.getDiffValue(r, "query_start")   + "";
						String DBName             = cm.getDiffValue(r, "datname")       + "";
						String Login              = cm.getDiffValue(r, "usename")       + "";
						String Command            = cm.getDiffValue(r, "query")         + "";
//						String tran_name          = cm.getDiffValue(r, "transaction_name") + "";
						String tran_name          = "-unknown-";
						
						// note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
						boolean doAlarm = true;

						// The below could have been done with nested if(!skipXxx), if(!skipYyy) doAlarm=true; 
						// Below is more readable, from a variable context point-of-view, but HARDER to understand
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipDbnameRegExp)   || ! DBName   .matches(skipCmdRegExp )));     // NO match in the SKIP Cmd      regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipLoginRegExp)    || ! Login    .matches(skipCmdRegExp )));     // NO match in the SKIP Cmd      regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipCmdRegExp)      || ! Command  .matches(skipCmdRegExp )));     // NO match in the SKIP Cmd      regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipTranNameRegExp) || ! tran_name.matches(skipTranNameRegExp))); // NO match in the SKIP TranName regexp

						// NO match in the SKIP regEx
						if (doAlarm)
						{
							String extendedDescText = cm.toTextTableString(DATA_RATE, r);
							String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
													
							AlarmEvent ae = new AlarmEventLongRunningStatement(cm, threshold, StatementExecInSec, StatementStartTime, DBName, Login, Command, tran_name);
							ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
							alarmHandler.addAlarm( ae );
						}
					} // end: above threshold
				} // end: is number
			} // end: StatementExecInSec
		} // end: loop rows
	}

	public static final String  PROPKEY_alarm_StatementExecInSec             = CM_NAME + ".alarm.system.if.StatementExecInSec.gt";
	public static final int     DEFAULT_alarm_StatementExecInSec             = 3 * 60 * 60; // 3 Hours

	public static final String  PROPKEY_alarm_StatementExecInSecSkipDbname   = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.dbname";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipDbname   = "";

	public static final String  PROPKEY_alarm_StatementExecInSecSkipLogin    = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.login";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipLogin    = "";

	public static final String  PROPKEY_alarm_StatementExecInSecSkipCmd      = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.cmd";
//	public static final String  DEFAULT_alarm_StatementExecInSecSkipCmd      = "^(BACKUP |RESTORE ).*";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipCmd      = "";

	public static final String  PROPKEY_alarm_StatementExecInSecSkipTranName = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.tranName";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipTranName = "";
	

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("StatementExecInSec",           isAlarmSwitch, PROPKEY_alarm_StatementExecInSec            , Integer.class, conf.getIntProperty(PROPKEY_alarm_StatementExecInSec            , DEFAULT_alarm_StatementExecInSec            ), DEFAULT_alarm_StatementExecInSec            , "If any SPID's has been executed a single SQL Statement for more than ## seconds, then send alarm 'AlarmEventLongRunningStatement'." ));
		list.add(new CmSettingsHelper("StatementExecInSec SkipDbs",                  PROPKEY_alarm_StatementExecInSecSkipDbname  , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipDbname  , DEFAULT_alarm_StatementExecInSecSkipDbname  ), DEFAULT_alarm_StatementExecInSecSkipDbname  , "If 'StatementExecInSec' is true; Discard databases listed (regexp is used).", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipLogins",               PROPKEY_alarm_StatementExecInSecSkipLogin   , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipLogin   , DEFAULT_alarm_StatementExecInSecSkipLogin   ), DEFAULT_alarm_StatementExecInSecSkipLogin   , "If 'StatementExecInSec' is true; Discard Logins listed (regexp is used)."   , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipCommands",             PROPKEY_alarm_StatementExecInSecSkipCmd     , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipCmd     , DEFAULT_alarm_StatementExecInSecSkipCmd     ), DEFAULT_alarm_StatementExecInSecSkipCmd     , "If 'StatementExecInSec' is true; Discard Commands listed (regexp is used)." , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipTranNames",            PROPKEY_alarm_StatementExecInSecSkipTranName, String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipTranName, DEFAULT_alarm_StatementExecInSecSkipTranName), DEFAULT_alarm_StatementExecInSecSkipTranName, "If 'StatementExecInSec' is true; Discard TranName listed (regexp is used)." , new RegExpInputValidator()));
		
		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "application_name") );
		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "usename") );

		return list;
	}
}

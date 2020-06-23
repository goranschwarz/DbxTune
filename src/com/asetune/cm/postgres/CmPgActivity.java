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
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHelper;
import com.asetune.cm.CmSettingsHelper;
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
import com.asetune.utils.StringUtil;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgActivity
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmPgActivity.class);
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
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("pid");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return "select * from pg_catalog.pg_stat_activity";
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
	

	@Override
	public void sendAlarmRequest()
	{
		AlarmHelper.sendAlarmRequestForColumn(this, "application_name");
		AlarmHelper.sendAlarmRequestForColumn(this, "usename");
	}
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "application_name") );
		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "usename") );
		
		return list;
	}
}

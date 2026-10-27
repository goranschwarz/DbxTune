/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune.cm.db2;

import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;

/**
 * The utilities that runs right now (MON_GET_UTILITY): BACKUP, RESTORE, REORG, RUNSTATS, LOAD, ...
 *
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmUtilities
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmUtilities.class.getSimpleName();
	public static final String   SHORT_NAME       = "Utilities";
	public static final String   HTML_DESC        =
		"<html>" +
		"<p>The utilities that runs right now (MON_GET_UTILITY): BACKUP, RESTORE, REORG, RUNSTATS, LOAD, ROLLFORWARD, MOVE_TABLE...</p>" +
		"<p>Who started it, when, and on what object. An empty table = no utility runs.</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "MON_GET_UTILITY"};
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
	@Override public Type    getTemplateLevel()                       { return Type.SMALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmUtilities(counterController, guiController);
	}

	public CmUtilities(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null,
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES,
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION,
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);

		setCounterController(counterController);
		setGuiController(guiController);

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("MEMBER");
		pkCols.add("UTILITY_INVOCATION_ID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// NOTE: UTILITY_ID is NULL for some utilities (an INPLACE REORG): UTILITY_INVOCATION_ID is the key
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     u.MEMBER \n"
			+ "    ,HEX(u.UTILITY_INVOCATION_ID)                                  as UTILITY_INVOCATION_ID \n"
			+ "    ,u.UTILITY_ID \n"
			+ "    ,u.UTILITY_TYPE \n"
			+ "    ,u.UTILITY_OPERATION_TYPE \n"
			+ "    ,u.UTILITY_START_TIME \n"
			+ "    ,SECONDS_BETWEEN(CURRENT TIMESTAMP, u.UTILITY_START_TIME)      as RUNNING_SEC \n"
			+ "    ,u.UTILITY_INVOKER_TYPE \n"
			+ "    ,u.UTILITY_PRIORITY \n"
			+ "    ,u.OBJECT_TYPE \n"
			+ "    ,RTRIM(u.OBJECT_SCHEMA)                                        as OBJECT_SCHEMA \n"
			+ "    ,u.OBJECT_NAME \n"
			+ "    ,u.NUM_TBSPS \n"
			+ "    ,CAST(u.TBSP_NAMES AS VARCHAR(2000))                          as TBSP_NAMES \n"
			+ "    ,u.APPLICATION_HANDLE \n"
			+ "    ,u.APPLICATION_NAME \n"
			+ "    ,RTRIM(u.SESSION_AUTH_ID)                                     as SESSION_AUTH_ID \n"
			+ "    ,CAST(u.UTILITY_DETAIL AS VARCHAR(4000))                      as UTILITY_DETAIL \n"
			+ "from table(MON_GET_UTILITY(-2)) u \n"
			+ "order by u.UTILITY_START_TIME \n"
			+ "";

		return sql;
	}

	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return Db2CmUtils.checkFunctionsAreExecutable(this, conn, "MON_GET_UTILITY(-2)");
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(CM_NAME, "The utilities that runs right now (MON_GET_UTILITY).");

			mtd.addColumn(CM_NAME, "MEMBER",                 "<html>The member (0 if this is not pureScale or DPF).</html>");
			mtd.addColumn(CM_NAME, "UTILITY_INVOCATION_ID",  "<html>Identifies this run of the utility (hex).</html>");
			mtd.addColumn(CM_NAME, "UTILITY_ID",             "<html>The id of the utility (what LIST UTILITIES shows). Empty for some utilities, for example an INPLACE REORG.</html>");
			mtd.addColumn(CM_NAME, "UTILITY_TYPE",           "<html>BACKUP, RESTORE, REORG, RUNSTATS, LOAD, ROLLFORWARD, MOVE_TABLE, ...</html>");
			mtd.addColumn(CM_NAME, "UTILITY_OPERATION_TYPE", "<html>For example for BACKUP: F = full, I = incremental, D = delta. For REORG: T = table, I = index...</html>");
			mtd.addColumn(CM_NAME, "UTILITY_START_TIME",     "<html>When it started.</html>");
			mtd.addColumn(CM_NAME, "RUNNING_SEC",            "<html>How long it has run, in seconds.</html>");
			mtd.addColumn(CM_NAME, "UTILITY_INVOKER_TYPE",   "<html>USER = started by a user, AUTO = by automatic maintenance (auto runstats, auto reorg...).</html>");
			mtd.addColumn(CM_NAME, "UTILITY_PRIORITY",       "<html>The throttling priority (0 = not throttled).</html>");
			mtd.addColumn(CM_NAME, "OBJECT_TYPE",            "<html>DATABASE, TABLE, INDEX, TABLESPACE, ...</html>");
			mtd.addColumn(CM_NAME, "OBJECT_SCHEMA",          "<html>The schema of the table or index.</html>");
			mtd.addColumn(CM_NAME, "OBJECT_NAME",            "<html>The table, index or table space.</html>");
			mtd.addColumn(CM_NAME, "NUM_TBSPS",              "<html>Number of table spaces it works on.</html>");
			mtd.addColumn(CM_NAME, "TBSP_NAMES",             "<html>The table spaces it works on.</html>");
			mtd.addColumn(CM_NAME, "APPLICATION_HANDLE",     "<html>The connection that runs it.</html>");
			mtd.addColumn(CM_NAME, "APPLICATION_NAME",       "<html>The program that runs it.</html>");
			mtd.addColumn(CM_NAME, "SESSION_AUTH_ID",        "<html>The user that runs it.</html>");
			mtd.addColumn(CM_NAME, "UTILITY_DETAIL",         "<html>Details, for example the command.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}
}

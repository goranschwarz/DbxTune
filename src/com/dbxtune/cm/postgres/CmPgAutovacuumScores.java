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
package com.dbxtune.cm.postgres;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSampleCatalogIteratorPostgres;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Ver;

/**
 * How urgently autovacuum wants to process each table (pg_stat_autovacuum_scores, Postgres 19 and above), the top 100 per database.
 */
public class CmPgAutovacuumScores
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgAutovacuumScores.class.getSimpleName();
	public static final String   SHORT_NAME       = "Autovacuum Scores";
	public static final String   HTML_DESC        =
		"<html>" +
		"How urgently autovacuum wants to vacuum/analyze each table (Postgres 19 and above): the top 100 tables per database, by score.<br>" +
		"A score of 1.0 or more means that autovacuum would process the table now; the highest score is processed first.<br>" +
		"The part scores tell why: transaction id / multixact age (wraparound), dead rows, inserted rows, or changed rows (analyze).<br>" +
		"<i>A table that stays at a high score is not processed in time: too few autovacuum workers, or a too low cost limit.</i>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(19);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_autovacuum_scores"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {}; // scores are "right now" values

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 60; // the scores change slowly
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
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmPgAutovacuumScores(counterController, guiController);
	}

	public CmPgAutovacuumScores(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null,
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES,
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION,
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

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
		pkCols.add("dbname");
		pkCols.add("schemaname");
		pkCols.add("relname");
		return pkCols;
	}

	/**
	 * The view only shows the tables in the current database: iterate over all databases (as CmPgTables)
	 */
	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		List<String> fallbackDbList = Arrays.asList( new String[]{"postgres"} );
		return new CounterSampleCatalogIteratorPostgres(name, negativeDiffCountersToZero, diffColumns, prevSample, fallbackDbList);
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// Only tables with a score (most tables are at 0 most of the time), the top 100 per database
		return ""
			    + "SELECT \n"
			    + "     current_database() AS dbname \n"
			    + "    ,s.* \n"
			    + "FROM pg_stat_autovacuum_scores s \n"
			    + "WHERE s.score > 0 \n"
			    + "ORDER BY s.score DESC \n"
			    + "LIMIT 100 \n"
			    + "";
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			String tabname = "pg_stat_autovacuum_scores";
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(tabname, "One row per table in the current database: how urgently autovacuum wants to process it (Postgres 19 and above).");

			mtd.addColumn(tabname, "dbname",              "<html>The database (the view only shows the tables in the database it is read in, so every database is read).</html>");
			mtd.addColumn(tabname, "relid",               "<html>OID of the table.</html>");
			mtd.addColumn(tabname, "schemaname",          "<html>Name of the schema the table is in.</html>");
			mtd.addColumn(tabname, "relname",             "<html>Name of the table.</html>");
			mtd.addColumn(tabname, "score",               "<html>The table's autovacuum priority: the highest of the part scores below. 1.0 or more: autovacuum would process the table now (the highest score first).</html>");
			mtd.addColumn(tabname, "xid_score",           "<html>Part score from the age of the table's oldest unfrozen transaction id, compared with <code>autovacuum_freeze_max_age</code> (wraparound).</html>");
			mtd.addColumn(tabname, "mxid_score",          "<html>Part score from the age of the table's oldest multixact id, compared with <code>autovacuum_multixact_freeze_max_age</code> (wraparound).</html>");
			mtd.addColumn(tabname, "vacuum_score",        "<html>Part score from the dead rows, compared with the vacuum threshold (<code>autovacuum_vacuum_threshold + autovacuum_vacuum_scale_factor * reltuples</code>).</html>");
			mtd.addColumn(tabname, "vacuum_insert_score", "<html>Part score from the rows inserted since the last vacuum, compared with the insert threshold (<code>autovacuum_vacuum_insert_threshold</code> / <code>_scale_factor</code>).</html>");
			mtd.addColumn(tabname, "analyze_score",       "<html>Part score from the rows changed since the last analyze, compared with the analyze threshold (<code>autovacuum_analyze_threshold</code> / <code>_scale_factor</code>).</html>");
			mtd.addColumn(tabname, "do_vacuum",           "<html>Autovacuum would vacuum this table now.<br>false with a score of 1.0 or more: autovacuum is turned off for the table (<code>autovacuum_enabled = false</code>), and only a forced anti-wraparound vacuum will run.</html>");
			mtd.addColumn(tabname, "do_analyze",          "<html>Autovacuum would analyze this table now.<br>false with a score of 1.0 or more: autovacuum is turned off for the table (<code>autovacuum_enabled = false</code>).</html>");
			mtd.addColumn(tabname, "for_wraparound",      "<html>The vacuum would be an anti-wraparound vacuum (it can't be cancelled by a lock conflict).</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}
}

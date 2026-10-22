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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.cm.oracle;

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
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmMemoryAdvice
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmMemoryAdvice.class.getSimpleName();
	public static final String   SHORT_NAME       = "Memory Advice";
	public static final String   HTML_DESC        =
		"<html>" +
		"What would happen with more memory, or with less? Oracle's own estimate, from the workload it has seen since the instance was started.<br>" +
		"<br>" +
		"One row for every size that Oracle has an estimate for, for each of:" +
		"<ul>" +
		"  <li><b>MEMORY_TARGET</b> - SGA + PGA together (empty if <code>memory_target</code> is not used).</li>" +
		"  <li><b>SGA_TARGET</b> - the whole SGA (empty if <code>sga_target</code> is not used).</li>" +
		"  <li><b>BUFFER_CACHE</b> - the buffer cache: how many physical reads there would have been.</li>" +
		"  <li><b>SHARED_POOL</b> - the shared pool: how long time it would have taken to load objects into the library cache (parsing).</li>" +
		"  <li><b>PGA_TARGET</b> - <code>pga_aggregate_target</code>: how much of the sorts and hash joins that would fit in memory.</li>" +
		"</ul>" +
		"<b>How to read it</b>: find the row where <code>IS_CURRENT</code> is YES (the size today, <code>SIZE_FACTOR</code> 1). " +
		"Then look at <code>ESTD_FACTOR</code> for the bigger sizes: 1 = as today, 0.8 = 20 percent less of what <code>ESTD_WHAT</code> says, 1.5 = 50 percent more.<br>" +
		"If a bigger size gives almost the same number: more memory will not help. If a smaller size gives almost the same number: the memory can be used for something else.<br>" +
		"For PGA_TARGET: <code>ESTD_OVERALLOC_COUNT</code> above 0 means that the size is too small for the workload.<br>" +
		"<br>" +
		"<b>Note</b>: The numbers are for the whole instance, also in a Container Database. They are only as good as the workload has been: look after a normal day, not right after a restart.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$MEMORY_TARGET_ADVICE", "V$SGA_TARGET_ADVICE", "V$DB_CACHE_ADVICE", "V$SHARED_POOL_ADVICE", "V$PGA_TARGET_ADVICE"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 600;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

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

		return new CmMemoryAdvice(counterController, guiController);
	}

	public CmMemoryAdvice(ICounterController counterController, IGuiController guiController)
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

		pkCols.add("ADVICE");
		pkCols.add("POOL_NAME");
		pkCols.add("SIZE_MB");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$MEMORY_TARGET_ADVICE", "V$SGA_TARGET_ADVICE", "V$DB_CACHE_ADVICE", "V$SHARED_POOL_ADVICE", "V$PGA_TARGET_ADVICE");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// The five advice views, as one table. They are for the whole instance (in a Container Database: CON_ID 0), and the columns used here are the same from 11.2
		// The sizes are in MB in the views, except for the PGA (bytes). The times are in seconds
		// Note: CAST(... as NUMBER(18,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		String sql = ""
			+ "select \n"
			+ "     x.ADVICE \n"
			+ "    ,x.POOL_NAME \n"
			+ "    ,x.SIZE_MB \n"
			+ "    ,x.SIZE_FACTOR \n"
			+ "    ,CAST(case when x.SIZE_FACTOR = 1 then 'YES' else 'NO' end as VARCHAR2(3)) as IS_CURRENT \n"
			+ "    ,x.ESTD_WHAT \n"
			+ "    ,x.ESTD_VALUE \n"
			+ "    ,x.ESTD_FACTOR \n"
			+ "    ,x.ESTD_PHYSICAL_READS \n"
			+ "    ,x.ESTD_OVERALLOC_COUNT \n"
			+ "    ,x.ADVICE_STATUS \n"
			+ "from ( \n"
			+ "    select \n"
			+ "         CAST('MEMORY_TARGET' as VARCHAR2(15))                    as ADVICE \n"
			+ "        ,CAST('-' as VARCHAR2(40))                                as POOL_NAME \n"
			+ "        ,CAST(a.MEMORY_SIZE         as NUMBER(18,1))              as SIZE_MB \n"
			+ "        ,CAST(a.MEMORY_SIZE_FACTOR  as NUMBER(10,4))              as SIZE_FACTOR \n"
			+ "        ,CAST('DB time (seconds)' as VARCHAR2(40))                as ESTD_WHAT \n"
			+ "        ,CAST(a.ESTD_DB_TIME        as NUMBER(20,1))              as ESTD_VALUE \n"
			+ "        ,CAST(a.ESTD_DB_TIME_FACTOR as NUMBER(10,4))              as ESTD_FACTOR \n"
			+ "        ,CAST(NULL as NUMBER(20))                                 as ESTD_PHYSICAL_READS \n"
			+ "        ,CAST(NULL as NUMBER(20))                                 as ESTD_OVERALLOC_COUNT \n"
			+ "        ,CAST(NULL as VARCHAR2(5))                                as ADVICE_STATUS \n"
			+ "    from V$MEMORY_TARGET_ADVICE a \n"
			+ "    union all \n"
			+ "    select \n"
			+ "         CAST('SGA_TARGET' as VARCHAR2(15)) \n"
			+ "        ,CAST('-' as VARCHAR2(40)) \n"
			+ "        ,CAST(a.SGA_SIZE            as NUMBER(18,1)) \n"
			+ "        ,CAST(a.SGA_SIZE_FACTOR     as NUMBER(10,4)) \n"
			+ "        ,CAST('DB time (seconds)' as VARCHAR2(40)) \n"
			+ "        ,CAST(a.ESTD_DB_TIME        as NUMBER(20,1)) \n"
			+ "        ,CAST(a.ESTD_DB_TIME_FACTOR as NUMBER(10,4)) \n"
			+ "        ,CAST(a.ESTD_PHYSICAL_READS as NUMBER(20)) \n"
			+ "        ,CAST(NULL as NUMBER(20)) \n"
			+ "        ,CAST(NULL as VARCHAR2(5)) \n"
			+ "    from V$SGA_TARGET_ADVICE a \n"
			+ "    union all \n"
			+ "    select \n"
			+ "         CAST('BUFFER_CACHE' as VARCHAR2(15)) \n"
			+ "        ,CAST(a.NAME || ' ' || a.BLOCK_SIZE as VARCHAR2(40)) \n"
			+ "        ,CAST(a.SIZE_FOR_ESTIMATE         as NUMBER(18,1)) \n"
			+ "        ,CAST(a.SIZE_FACTOR               as NUMBER(10,4)) \n"
			+ "        ,CAST('physical reads' as VARCHAR2(40)) \n"
			+ "        ,CAST(a.ESTD_PHYSICAL_READS       as NUMBER(20,1)) \n"
			+ "        ,CAST(a.ESTD_PHYSICAL_READ_FACTOR as NUMBER(10,4)) \n"
			+ "        ,CAST(a.ESTD_PHYSICAL_READS       as NUMBER(20)) \n"
			+ "        ,CAST(NULL as NUMBER(20)) \n"
			+ "        ,CAST(a.ADVICE_STATUS as VARCHAR2(5)) \n"
			+ "    from V$DB_CACHE_ADVICE a \n"
			+ "    union all \n"
			+ "    select \n"
			+ "         CAST('SHARED_POOL' as VARCHAR2(15)) \n"
			+ "        ,CAST('-' as VARCHAR2(40)) \n"
			+ "        ,CAST(a.SHARED_POOL_SIZE_FOR_ESTIMATE as NUMBER(18,1)) \n"
			+ "        ,CAST(a.SHARED_POOL_SIZE_FACTOR       as NUMBER(10,4)) \n"
			+ "        ,CAST('library cache load time (seconds)' as VARCHAR2(40)) \n"
			+ "        ,CAST(a.ESTD_LC_LOAD_TIME             as NUMBER(20,1)) \n"
			+ "        ,CAST(a.ESTD_LC_LOAD_TIME_FACTOR      as NUMBER(10,4)) \n"
			+ "        ,CAST(NULL as NUMBER(20)) \n"
			+ "        ,CAST(NULL as NUMBER(20)) \n"
			+ "        ,CAST(NULL as VARCHAR2(5)) \n"
			+ "    from V$SHARED_POOL_ADVICE a \n"
			+ "    union all \n"
			+ "    select \n"
			+ "         CAST('PGA_TARGET' as VARCHAR2(15)) \n"
			+ "        ,CAST('-' as VARCHAR2(40)) \n"
			+ "        ,CAST(a.PGA_TARGET_FOR_ESTIMATE / 1048576 as NUMBER(18,1)) \n"
			+ "        ,CAST(a.PGA_TARGET_FACTOR                 as NUMBER(10,4)) \n"
			+ "        ,CAST('PGA cache hit (percent)' as VARCHAR2(40)) \n"
			+ "        ,CAST(a.ESTD_PGA_CACHE_HIT_PERCENTAGE     as NUMBER(20,1)) \n"
			+ "        ,CAST(NULL as NUMBER(10,4)) \n"
			+ "        ,CAST(NULL as NUMBER(20)) \n"
			+ "        ,CAST(a.ESTD_OVERALLOC_COUNT              as NUMBER(20)) \n"
			+ "        ,CAST(a.ADVICE_STATUS as VARCHAR2(5)) \n"
			+ "    from V$PGA_TARGET_ADVICE a \n"
			+ ") x \n"
			+ "order by decode(x.ADVICE, 'MEMORY_TARGET', 1, 'SGA_TARGET', 2, 'BUFFER_CACHE', 3, 'SHARED_POOL', 4, 5), x.POOL_NAME, x.SIZE_MB \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "What would happen with more memory, or with less? Oracle's own estimate, from the workload it has seen since the instance was started.");

			mtd.addColumn(CM_NAME, "ADVICE",               "<html>What the estimate is for: MEMORY_TARGET (V$MEMORY_TARGET_ADVICE), SGA_TARGET (V$SGA_TARGET_ADVICE), BUFFER_CACHE (V$DB_CACHE_ADVICE), SHARED_POOL (V$SHARED_POOL_ADVICE) or PGA_TARGET (V$PGA_TARGET_ADVICE).</html>");
			mtd.addColumn(CM_NAME, "POOL_NAME",            "<html>For BUFFER_CACHE: the buffer pool and it's block size, like 'DEFAULT 8192'. Otherwise '-'.</html>");
			mtd.addColumn(CM_NAME, "SIZE_MB",              "<html>The size that the row is an estimate for, in MB.</html>");
			mtd.addColumn(CM_NAME, "SIZE_FACTOR",          "<html>SIZE_MB compared to the size today: 1 = the size today, 0.5 = half of it, 2 = twice as big.</html>");
			mtd.addColumn(CM_NAME, "IS_CURRENT",           "<html>YES = this row is the size today (SIZE_FACTOR is 1). Start reading here.</html>");
			mtd.addColumn(CM_NAME, "ESTD_WHAT",            "<html>What ESTD_VALUE is:<ul>"
			                                                 + "<li><b>DB time (seconds)</b> - the time the sessions would have spent in the database.</li>"
			                                                 + "<li><b>physical reads</b> - the number of blocks that would have been read from disk.</li>"
			                                                 + "<li><b>library cache load time (seconds)</b> - the time it would have taken to load (parse) objects into the shared pool.</li>"
			                                                 + "<li><b>PGA cache hit (percent)</b> - how much of the sorts and hash joins that would have been done in memory. 100 = all of it.</li>"
			                                                 + "</ul></html>");
			mtd.addColumn(CM_NAME, "ESTD_VALUE",           "<html>Oracle's estimate for this size. See ESTD_WHAT for what it is.</html>");
			mtd.addColumn(CM_NAME, "ESTD_FACTOR",          "<html>ESTD_VALUE compared to today: 1 = as today, 0.8 = 20 percent less (better), 1.5 = 50 percent more (worse). Empty for PGA_TARGET.</html>");
			mtd.addColumn(CM_NAME, "ESTD_PHYSICAL_READS",  "<html>The number of physical reads that Oracle estimates for this size. Only for SGA_TARGET and BUFFER_CACHE.</html>");
			mtd.addColumn(CM_NAME, "ESTD_OVERALLOC_COUNT", "<html>Only for PGA_TARGET: how many times Oracle would have had to use more PGA than the target. Above 0 = this size is too small for the workload.</html>");
			mtd.addColumn(CM_NAME, "ADVICE_STATUS",        "<html>ON = Oracle collects the estimate. OFF = it does not (the parameter 'db_cache_advice', or 'statistics_level' is BASIC). Only for BUFFER_CACHE and PGA_TARGET.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}
}

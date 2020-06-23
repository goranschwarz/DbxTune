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

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSampleCatalogIteratorPostgres;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.postgres.gui.CmPgSequencesIoPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgSequencesIo
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmPgSequencesIo.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgSequencesIo.class.getSimpleName();
	public static final String   SHORT_NAME       = "Sequences IO Info";
	public static final String   HTML_DESC        = 
		"<html>" +
		"One row for each index in the current database, showing statistics about accesses to that specific sequence." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_statio_all_sequences"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"blks_read",
			"blks_hit"
	};
	
//	RS> Col# Label      JDBC Type Name         Guessed DBMS type Source Table            
//	RS> ---- ---------- ---------------------- ----------------- ------------------------
//	RS> 1    relid      java.sql.Types.BIGINT  oid               pg_statio_user_sequences
//	RS> 2    schemaname java.sql.Types.VARCHAR name(2147483647)  pg_statio_user_sequences
//	RS> 3    relname    java.sql.Types.VARCHAR name(2147483647)  pg_statio_user_sequences
//	RS> 4    blks_read  java.sql.Types.BIGINT  int8              pg_statio_user_sequences
//	RS> 5    blks_hit   java.sql.Types.BIGINT  int8              pg_statio_user_sequences

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

		return new CmPgSequencesIo(counterController, guiController);
	}

	public CmPgSequencesIo(ICounterController counterController, IGuiController guiController)
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
	private static final String  PROP_PREFIX                          = CM_NAME;

	public static final String  PROPKEY_sample_systemTables           = PROP_PREFIX + ".sample.systemTables";
	public static final boolean DEFAULT_sample_systemTables           = false;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_systemTables,           DEFAULT_sample_systemTables);
	}
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Sample System Tables", PROPKEY_sample_systemTables , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemTables  , DEFAULT_sample_systemTables  ), DEFAULT_sample_systemTables, "Sample System Tables" ));

		return list;
	}


	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmPgSequencesIoPanel(this);
	}


	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("dbname");
		pkCols.add("schemaname");
		pkCols.add("relname");

		return pkCols;
	}

	/**
	 * Create a special CounterSample, that will iterate over all databases that we will interrogate
	 */
	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		List<String> fallbackDbList = Arrays.asList( new String[]{"postgres"} );
		return new CounterSampleCatalogIteratorPostgres(name, negativeDiffCountersToZero, diffColumns, prevSample, fallbackDbList);
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String tabName = "pg_catalog.pg_statio_user_sequences";

		// Sample System Tables
		if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_systemTables, DEFAULT_sample_systemTables))
		{
			tabName = "pg_catalog.pg_statio_all_sequences";
		}

		return "select current_database() as dbname, * from "+tabName;
	}
}

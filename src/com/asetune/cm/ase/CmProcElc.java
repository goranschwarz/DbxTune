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

import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmProcElc
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmProcElc.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmProcElc.class.getSimpleName();
	public static final String   SHORT_NAME       = "Engine Local Proc Cache";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Procedur Cache - Engine Local Cache Usage<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(16,0,0, 3,8); // 16.0 SP3 PL8
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monProcELC"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"ChunkCountDiff",
			"Allocs",
			"Frees",
			"Empty",
			"Full",
			"Flushes",
			"_last_dummy_column_which_do_not_exists_in_resultset_"};


	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmProcElc(counterController, guiController);
	}

	public CmProcElc(ICounterController counterController, IGuiController guiController)
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
//		return new ACopyMePanel(this);
//	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addColumn("monProcELC", "EngineID",           "<html>System-defined, numeric identifier of the engine, starting from 0.</html>");
			mtd.addColumn("monProcELC", "ElcNum",             "<html>System-defined, numeric identifier of the ELC. Each engine has four ELCs, whose identifiers range from 0 to 3. <br>Origin column 'ProcELCNum'. </html>");
			mtd.addColumn("monProcELC", "ChunkSize",          "<html>Size of the ELC chunk, in units of 2k pages. For example, a value of 2 means 4k pages for the ELC chunk.       <br>Origin column 'ProcELCChunkSize'. </html>");
			mtd.addColumn("monProcELC", "ChunkSizeInKb",      "<html>Size of the ELC chunk in KB instead of the number found in column 'ProcELCChunkSize'.<br>"
			                                                   + "<b>Formula:</b> ProcELCChunkSize * 2"
			                                                   + "</html>");
			mtd.addColumn("monProcELC", "ChunkCountDiff",     "<html>Total number of chunks stored in the cache.                                    <br>Origin column 'ProcELCCount'. </html>");
			mtd.addColumn("monProcELC", "ChunkCount",         "<html>Total number of chunks stored in the cache.                                    <br>Origin column 'ProcELCCount'. </html>");
			mtd.addColumn("monProcELC", "ChunksInMb",         "<html>Total number of chunks (in MB) stored in the cache. <br>"
			                                                   + "<b>Formula:</b> (ProcELCCount * ProcELCChunkSize * 2) / 1024.0"
			                                                   + "</html>");
			mtd.addColumn("monProcELC", "MaxCount",           "<html>Maximum number of chunks that can be stored in the cache.                      <br>Origin column 'ProcELCMaxCount'. </html>");
			mtd.addColumn("monProcELC", "MaxCountInMb",       "<html>Maximum number of chunks (in MB) that can be stored in the cache. <br>"
			                                                   + "<b>Formula:</b> (ProcELCMaxCount * ProcELCChunkSize * 2) / 1024.0</html>");
			mtd.addColumn("monProcELC", "Allocs",             "<html>Number of allocations done from this ELC.                                      <br>Origin column 'ProcELCAllocs'.  </html>");
			mtd.addColumn("monProcELC", "Frees",              "<html>Number of frees done to this ELC.                                              <br>Origin column 'ProcELCFrees'.   </html>");
			mtd.addColumn("monProcELC", "Empty",              "<html>Number of times this ELC was found empty.                                      <br>Origin column 'ProcELCEmpty'.   </html>");
			mtd.addColumn("monProcELC", "Full",               "<html>Number of times this ELC was found to be full.                                 <br>Origin column 'ProcELCFull'.    </html>");
			mtd.addColumn("monProcELC", "Flushes",            "<html>Number of times this ELC was flushed.                                          <br>Origin column 'ProcELCFlushes'. </html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}
		
	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("EngineID");
		pkCols.add("ElcNum");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
	{
		String sql = ""
			    + "select \n"
			    + "     EngineID \n"
			    + "    ,ElcNum         = ProcELCNum \n"
			    + "    ,ChunkSize      = ProcELCChunkSize \n"
			    + "    ,ChunkSizeInKb  = convert(varchar(5), ProcELCChunkSize * 2) + 'K' \n"
			    + "    ,ChunkCountDiff = ProcELCCount \n"
			    + "    ,ChunkCount     = ProcELCCount \n"
			    + "    ,ChunksInMb     = convert(decimal(12,1), (ProcELCCount * ProcELCChunkSize * 2) / 1024.0) \n"
			    + "    ,MaxCount       = ProcELCMaxCount \n"
			    + "    ,MaxCountInMb   = convert(decimal(12,1), (ProcELCMaxCount * ProcELCChunkSize * 2) / 1024.0) \n"
			    + "    ,Allocs         = ProcELCAllocs \n"
			    + "    ,Frees          = ProcELCFrees \n"
			    + "    ,Empty          = ProcELCEmpty \n"
			    + "    ,Full           = ProcELCFull \n"
			    + "    ,Flushes        = ProcELCFlushes \n"
			    + "from master.dbo.monProcELC \n"
			    + "";
		
		return sql;
	}
}

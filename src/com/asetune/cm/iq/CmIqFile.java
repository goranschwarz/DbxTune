/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.cm.iq;

import java.sql.Connection;
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

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */

/**
 * sp_iqfile procedure
 * Displays detailed information about each dbfile in a dbspace.
 * @author I063869
 *
 */
public class CmIqFile
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminWhoSqm.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIqFile.class.getSimpleName();
	public static final String   SHORT_NAME       = "files";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<h4>sp_iqfile</h4>" + 
		"Displays detailed information about each dbfile in a dbspace." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sp_iqfile"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 60; //CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmIqFile(counterController, guiController);
	}

	public CmIqFile(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);
		
		setShowClearTime(false);
		setBackgroundDataPollingEnabled(false, false);

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
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("sp_iqfile",  "Displays detailed information about each dbfile in a dbspace.");

			mtd.addColumn("sp_iqfile", "DBSpaceName",
					"<html>Name of the dbspace as specified in the CREATE DBSPACE statement. "
					+ "<br/>Dbspace names are case-insensitive for databases created with CASE RESPECT.</html>");
			mtd.addColumn("sp_iqfile", "DBFileName",
					"<html>Logical file name.</html>");
			mtd.addColumn("sp_iqfile", "Path",
					"<html>Location of the physical file or raw partition.</html>");
			mtd.addColumn("sp_iqfile", "SegmentType",
					"<html>Type of dbspace (MAIN or TEMPORARY).</html>");
			mtd.addColumn("sp_iqfile", "RWMode",
					"<html>Mode of the dbspace: read-write (RW) or read-only (RO).</html>");
			mtd.addColumn("sp_iqfile", "Online",
					"<html>T (online) or F (offline).</html>");
			mtd.addColumn("sp_iqfile", "Usage",
					"<html>Percent of dbspace currently in use by this file in the dbspace.</html>");
			mtd.addColumn("sp_iqfile", "DBFileSize",
					"<html>Current size of the file or raw partition. For a raw partition, this size value can be less than the physical size.</html>");
			mtd.addColumn("sp_iqfile", "Reserve",
					"<html>Reserved space that can be added to this file in the dbspace.</html>");
			mtd.addColumn("sp_iqfile", "StripeSize",
					"<html>Amount of data written to the file before moving to the next file, if disk striping is on.</html>");
			mtd.addColumn("sp_iqfile", "BlkTypes",
					"<html>Space used by both user data and internal system structures (see Table 7-28 for identifier values).</html>");
			mtd.addColumn("sp_iqfile", "FirstBlk",
					"<html>First IQ block number assigned to the file.</html>");
			mtd.addColumn("sp_iqfile", "LastBlk",
					"<html>Last IQ block number assigned to the file.</html>");
			mtd.addColumn("sp_iqfile", "OkToDrop",
					"<html>‘Y’ indicates the file can be dropped; otherwise ‘N’.</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return null;
//		List <String> pkCols = new LinkedList<String>();
//
//		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String sql = "select * from sp_iqfile()";

		return sql;
	}
}

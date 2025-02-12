/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.cm.ase;

import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.ase.gui.CmActiveObjectsPanel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSybaseAse;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmActiveObjects
extends CountersModel
{
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmActiveObjects.class.getSimpleName();
	public static final String   SHORT_NAME       = "Active Objects";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Objects that are currently accessed by any active Statements in the ASE.<br>" +
		"<br>" +
		"Table Background colors:" +
		"<ul>" +
		"    <li>ORANGE - An Index.</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monProcessObject"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "per object statistics active=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"LogicalReads", "PhysicalReads", "PhysicalAPFReads"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
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

		return new CmActiveObjects(counterController, guiController);
	}

	public CmActiveObjects(ICounterController counterController, IGuiController guiController)
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
		return new CmActiveObjectsPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addColumn("monProcessObject",  "dupMergeCount", "<html>" +
			                                                       "If more than <b>one</b> row was fetched for this \"Primary Key\".<br>" +
			                                                       "Then this column will hold number of rows merged into this row. 0=No Merges(only one row for this PK), 1=One Merge accurred(two rows was seen for this PK), etc...<br>" +
			                                                       "This means that the non-diff columns will be from the first row fetched,<br>" +
			                                                       "then all columns which is marked for difference calculation will be a summary of all the rows (so it's basically a SQL SUM(colName) operation)." +
			                                                   "</html>");
		}
		catch (NameNotFoundException e) 
		{
		//	_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
			System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("SPID");
		pkCols.add("DBID");
		pkCols.add("OwnerUserID");
		pkCols.add("ObjectID");
		pkCols.add("IndexID");

		if (srvVersion >= Ver.ver(15,0))
			pkCols.add("PartitionID");

		// NOTE: PK is NOT unique, so therefore 'dupMergeCount' column is added to the SQL Query
		//       This can happen for example on self joins (or when the same table is refrerenced more than once in the a SQL Statement)

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

		String cols = "";

		String InstanceID    = ""; // only in ClusterEdition
		String TableSize     = ""; // between 12.5.2 and < 15.0.0
		String ObjectName    = "ObjectName = isnull(object_name(ObjectID, DBID), 'Obj='+ObjectName), \n"; // if user is not a valid user in A.DBID, then object_name() will return null
		String IndexName     = ""; // 15.0.2 or higher
		String PartitionID   = ""; // 15.0.0 or higher
		String PartitionName = ""; // 15.0.0 or higher
		String PartitionSize = ""; // 15.0.0 or higher

		if (isClusterEnabled)
			InstanceID = "InstanceID, ";

		if (srvVersion >= Ver.ver(12,5,2))
			TableSize = "TableSize, \n";

		if (srvVersion >= Ver.ver(15,0))
		{
			TableSize = "";
			PartitionName = "PartitionName, ";
			PartitionSize = "PartitionSize, \n";
			PartitionID   = ", PartitionID";
		}

		if (srvVersion >= Ver.ver(15,0,2))
		{
			IndexName = "IndexName = CASE WHEN IndexID=0 THEN convert(varchar(255),'DATA') \n" +
				        "                 ELSE convert(varchar(255), isnull(index_name(DBID, ObjectID, IndexID), '-unknown-')) \n" +
				        "            END, \n";
		}

		cols = InstanceID + "SPID, KPID, ObjectType, DBName, \n" +
		       ObjectName +
		       "IndexID, \n" + 
		       IndexName + 
		       PartitionName + PartitionSize +
		       TableSize +
		       "OwnerUserID, LogicalReads, PhysicalReads, PhysicalAPFReads, dupMergeCount=convert(int,0), \n" +
		       "DBID, ObjectID" + PartitionID;

		// in 12.5.4 (esd#9) will produce an "empty" resultset using "S.SPID != @@spid"
		//                   so this will be a workaround for those releses below 15.0.0
		String whereSpidNotMe = "SPID != @@spid";
		if (srvVersion < Ver.ver(15,0))
		{
			whereSpidNotMe = "SPID != convert(int,@@spid)";
		}

		String sql = 
			"select " + cols + "\n" +
			"from master..monProcessObject\n" +
			"where "+whereSpidNotMe;

		return sql;
	}

	/** 
	 * Get number of rows to save/request ddl information for 
	 */
	@Override
	public int getMaxNumOfDdlsToPersist()
	{
		return Integer.MAX_VALUE; // Basically ALL Rows
	}
}

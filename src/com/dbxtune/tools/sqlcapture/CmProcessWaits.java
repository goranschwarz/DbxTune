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
package com.dbxtune.tools.sqlcapture;

import java.util.LinkedList;
import java.util.List;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CmSybMessageHandler;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.swing.GTabbedPane;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmProcessWaits
extends CountersModel
{
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmProcessWaits.class.getSimpleName();
	public static final String   SHORT_NAME       = "Waits";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Wait events of the first SPID from 'Active Statement List' in tab 'Statements'." +
		"</html>";

	public static final String   GROUP_NAME       = null;
//	public static final String   GUI_ICON_FILE    = null;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monProcessWaits", "monWaitEventInfo", "monWaitClassInfo"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring", "process wait events", "wait event timing"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"WaitTime", "Waits"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
//	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmProcessWaits(counterController, guiController);
	}

	/**
	 * Constructor
	 */
	public CmProcessWaits(ICounterController counterController, IGuiController guiController)
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
		
//		CounterSetTemplates.register(this);
	}
	
	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
	private void addTrendGraphs()
	{
	}

	@Override
	protected CmSybMessageHandler createSybMessageHandler()
	{
		CmSybMessageHandler msgHandler = super.createSybMessageHandler();

		// If ASE is above 15.0.3 esd#1, and dbcc traceon(3604) is given && 'capture missing stats' is 
		// on the 'CMsysWaitActivity' CM will throw an warning which should NOT be throws...
		//if (getServerVersion() >= 15031) // NOTE this is done early in initialization, so getServerVersion() can't be used
		//if (getServerVersion() >= 1503010) // NOTE this is done early in initialization, so getServerVersion() can't be used
		//if (getServerVersion() >= Ver.ver(15,0,3,1)) // NOTE this is done early in initialization, so getServerVersion() can't be used
		msgHandler.addDiscardMsgStr("WaitClassID, WaitEventID");

		return msgHandler;
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("KPID");
		pkCols.add("WaitEventID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		long srvVersion = versionInfo.getLongVersion();

		String sql = 
			"select SPID, KPID, Class=C.Description, Event=I.Description, W.WaitEventID, WaitTime, Waits \n" + 
			"from master..monProcessWaits W, master..monWaitEventInfo I, master..monWaitClassInfo C \n" + 
			"where W.WaitEventID=I.WaitEventID \n" + 
			"  and I.WaitClassID=C.WaitClassID \n";

		if (srvVersion >= Ver.ver(15,7))
		{
			sql += "  and C.Language = 'en_US' \n";
			sql += "  and I.Language = 'en_US' \n";
		}
		return sql;
	}

	@Override
	public boolean isRefreshable()
	{
		boolean refresh = false;

		// Current TAB is visible
		if ( equalsTabPanel(getGuiController().getActiveTab()) )
			refresh = true;

		// Current TAB is un-docked (in it's own window)
		if (getTabPanel() != null)
		{
			GTabbedPane tp = getGuiController().getTabbedPane();
			if (tp.isTabUnDocked(getTabPanel().getPanelName()))
				refresh = true;
		}

		// Background poll is checked
		if ( isBackgroundDataPollingEnabled() )
			refresh = true;

		// NO-REFRESH if data polling is PAUSED
		if ( isDataPollingPaused() )
			refresh = false;

		// Check postpone
		if ( getTimeToNextPostponedRefresh() > 0 )
		{
//			_logger.debug("Next refresh for the cm '"+getName()+"' will have to wait '"+TimeUtils.msToTimeStr(getTimeToNextPostponedRefresh())+"'.");
			refresh = false;
		}

		return refresh;
	}
}

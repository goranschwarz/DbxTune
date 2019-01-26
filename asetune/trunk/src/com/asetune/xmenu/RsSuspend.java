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
package com.asetune.xmenu;

import java.sql.Connection;

import com.asetune.gui.RsResumeSuspendDialog;
import com.asetune.utils.SwingUtils;

public class RsSuspend
extends XmenuActionBase 
{
//	private static Logger _logger = Logger.getLogger(RsSuspend.class);

	/**
	 * 
	 */
	public RsSuspend() 
	{
		super();
	}

//	/**
//	 */
//	@Override 
//	public void doWork() 
//	{
//		
//		final Connection conn = getConnection();
//
//		String nameCol = getParamValue(0);
//		String infoCol = getParamValue(1);
//
//		SwingUtils.showInfoMessage(null, "Not yet implemented", "Not yet implemented.");
//	}
	/**
	 */
	@Override 
	public void doWork() 
	{
		final Connection conn = getConnection();
//System.out.println("doWork(): conn="+conn);

//		String rcl     = getConfig();
		String nameCol = getParamValue(0);
		String infoCol = getParamValue(1);

//		+----+---------+---------+-------------------------------+
//		|Spid|Name     |State    |Info                           |
//		+----+---------+---------+-------------------------------+
//		|    |DSI EXEC |Suspended|122(1) GORAN_1_DS.wsdb1        |
//		|    |DSI      |Suspended|122 GORAN_1_DS.wsdb1           |
//		|    |REP AGENT|Down     |GORAN_2_DS.wsdb2               |
//		|    |DSI EXEC |Down     |116(1) REP_CONNECTOR_1.dest1   |
//		|    |DSI EXEC |Down     |118(1) REP_CONNECTOR_HPUX.dest1|
//		|    |DSI EXEC |Down     |115(1) XE.XE                   |
//		+----+---------+---------+-------------------------------+

		// Parse the INFO Columns 
		if ("DSI".equals(nameCol) || "DSI EXEC".equals(nameCol))
		{
			String dsdb = infoCol.substring(infoCol.indexOf(" ")).trim();
			
			RsResumeSuspendDialog dialog = new RsResumeSuspendDialog(getOwner(), conn, dsdb, 
					RsResumeSuspendDialog.ActionType.SUSPEND, 
					RsResumeSuspendDialog.RsThreadType.DSI);
			dialog.setVisible(true);
		}
		else if ("REP AGENT".equals(nameCol))
		{
			String dsdb = infoCol;

			RsResumeSuspendDialog dialog = new RsResumeSuspendDialog(getOwner(), conn, dsdb, 
					RsResumeSuspendDialog.ActionType.SUSPEND, 
					RsResumeSuspendDialog.RsThreadType.REP_AGENT);
			dialog.setVisible(true);
		}
		else if ("RSI".equals(nameCol))
		{
			SwingUtils.showInfoMessage(null, "Not yet implemented", "Suspend for RSI is not yet implemented.");
			return;
		}
		else
		{
			SwingUtils.showInfoMessage(null, "Not yet implemented", "Suspend for '"+nameCol+"' is not yet implemented.");
			return;
		}
	}
}

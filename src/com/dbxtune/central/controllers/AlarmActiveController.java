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
package com.dbxtune.central.controllers;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.alarm.AlarmMuteManager;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.central.pcs.objects.DbxAlarmActive;
import com.dbxtune.central.pcs.objects.DbxCentralServerLayout;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AlarmActiveController 
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.setContentType("text/html");
		resp.setCharacterEncoding("UTF-8");
//		resp.setContentType("application/json");
//		resp.setCharacterEncoding("UTF-8");
		ServletOutputStream out = resp.getOutputStream();

		// Check that we have a READER
		if ( ! CentralPersistReader.hasInstance() )
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No PCS Reader to: DBX Central Database.");
			return;
		}
		CentralPersistReader reader = CentralPersistReader.getInstance();


		String payload;
		try
		{
			// Check for known input parameters
			if (Helper.hasUnKnownParameters(req, resp, "srv", "srvName", "group", "groupOfSrv"))
				return;
			
			String srv        = Helper.getParameter(req, new String[] {"srv", "srvName"} );
			String group      = Helper.getParameter(req, new String[] {"group"} );
			String groupOfSrv = Helper.getParameter(req, new String[] {"groupOfSrv"} );

			// Check that "srv" exists
			if (StringUtil.hasValue(srv))
			{
				if ( ! reader.hasServerSession(srv) )
				{
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Server name '"+srv+"' do not exist in the DBX Central Database.");
					return;
				}
			}

			// get Data
			List<DbxAlarmActive> list = reader.getAlarmActive(srv);

			// Enrich each alarm with current mute state from AlarmMuteManager
			AlarmMuteManager muteMgr = AlarmMuteManager.getInstance();
			for (DbxAlarmActive alarm : list)
			{
				String alarmId = alarm.getAlarmId();
				if (alarmId != null && muteMgr.isMuted(alarmId))
				{
					AlarmMuteManager.MuteRecord rec = muteMgr.getMute(alarmId);
					if (rec != null)
					{
						alarm.setIsMuted(true);
						alarm.setMuteReason(rec.reason);
						alarm.setMutedByUser(rec.mutedByUser);
						alarm.setMutedTime(rec.mutedTime);
						alarm.setMuteExpiresAt(rec.expiresAt);
					}
				}
			}

			//------------------------------------------------------------------
			// Stamp every alarm with the SERVER_LIST GROUP its server belongs to.
			// NOTE: This is cached (and invalidated on file change) inside
			//       DbxCentralServerLayout, so it's cheap enough to always do.
			//------------------------------------------------------------------
			Map<String, String> srvToGroupMap = DbxCentralServerLayout.getServerNameToGroupMap(null);
			for (DbxAlarmActive alarm : list)
			{
				alarm.setGroup( srvToGroupMap.get(alarm.getSrvName()) );
			}

			//------------------------------------------------------------------
			// Optionally: filter on GROUP
			//  - 'groupOfSrv=<srvName>' -> resolve which group that server is in, then filter on it
			//  - 'group=<name>[,<name>]' -> filter on the group name(s)
			//------------------------------------------------------------------
			if (StringUtil.hasValue(groupOfSrv))
			{
				// NOTE: If the server is unknown, or not a member of any group, we deliberately
				//       end up with an EMPTY list (and NOT an error). A Collector that isn't in
				//       any group should degrade quietly, not fail.
				group = DbxCentralServerLayout.getGroupNameForServer(groupOfSrv, null);

				if (StringUtil.isNullOrBlank(group))
				{
					if (_logger.isDebugEnabled())
						_logger.debug("AlarmActive: groupOfSrv='" + groupOfSrv + "' is not a member of any GROUP in the SERVER_LIST file. Returning an empty list.");

					list.clear();
				}
			}

			if ( ! list.isEmpty() && StringUtil.hasValue(group) )
			{
				Set<String> srvNamesInGroup = DbxCentralServerLayout.getServerNamesInGroups(group, null);
				list.removeIf( alarm -> ! srvNamesInGroup.contains(alarm.getSrvName()) );
			}

			// to JSON
			ObjectMapper om = Helper.createObjectMapper();
			payload = om.writeValueAsString(list);
		}
		catch (Exception e)
		{
			_logger.info("Problem accessing DBMS or writing JSON, Caught: "+e, e);
			throw new ServletException("Problem accessing db or writing JSON, Caught: "+e, e);
		}
		
		out.println(payload);
		
		out.flush();
		out.close();
	}
}

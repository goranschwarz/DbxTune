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
package com.dbxtune.cm.rs;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;


public class RsStatCounterDictionary
//implements SrvCounterDictionary
{
	private static Logger        _logger          = Logger.getLogger(CmAdminStats.class);
//	private static final long    serialVersionUID = 1L;

	private static RsStatCounterDictionary _instance = null;
//	private boolean _initialized = false;
	
	public static boolean hasInstance()
	{
		return _instance != null;
	}
	public static void setInstance(RsStatCounterDictionary dict)
	{
		_instance = dict;
	}
	public static RsStatCounterDictionary getInstance()
	{
		return _instance;
	}

	
//	public boolean isInitialized()
//	{
//		return _initialized;
//	}
//	public void setInitialized(boolean to)
//	{
//		_initialized = to;
//	}

	Map<Integer, StatCounterEntry> _counterIdMap   = new HashMap<Integer, StatCounterEntry>();
	Map<String,  StatCounterEntry> _counterNameMap = new HashMap<String,  StatCounterEntry>();
	
	public void addCounter(StatCounterEntry c)
	{
		if (_counterIdMap.containsKey(c._counterId))
			System.out.println("XXXXXXXXXXXXXXXX: counter_id="+c._counterId+" alreasy exists in _counterIdMap ?????");

		String key = c._moduleName + ":" + c._displayName;
		if (_counterNameMap.containsKey(key))
//		if (_counterNameMap.containsKey(c._displayName))
		{
			// Hack to workaround some known counter DisplayName duplicates, which is CR# ??????, fixed in RS version ?????
			if      (c._displayName.equals("GroupsClosedDispatch") && c._counterId == 5127) c._displayName = "GroupsClosedSwitchSQT";
			else if (c._displayName.equals("GroupsClosedDispatch") && c._counterId == 5128) c._displayName = "GroupsClosedRsLastCommit";
			else
			{
				StatCounterEntry i = _counterNameMap.get(c._displayName);
				System.out.println("XXXXXXXXXXXXXXXX: '"+c._displayName+"' counter name, already exists in _counterNameMap ?????");
				System.out.println("             CUR: _moduleName="+i._moduleName+", _counterId="+i._counterId);
				System.out.println("             NEW: _moduleName="+c._moduleName+", _counterId="+c._counterId);
			}
			key = c._moduleName + ":" + c._displayName;
		}

		_counterIdMap  .put(c._counterId,   c);
//		_counterNameMap.put(c._displayName, c);
		_counterNameMap.put(key, c);
	}

	public boolean isInitialized()
	{
		return _counterIdMap.size() > 0;
	}
	public StatCounterEntry getCounter(int id)
	{
		return _counterIdMap.get(id);
	}

	public StatCounterEntry getCounter(String instance, String name)
	{
		String module = instance;
		int delimiter = module.indexOf(',');
		if (delimiter != -1)
			module = module.substring(0, delimiter);

		if (module.equals("DSI EXEC"))
		{
			if      (name.startsWith("HQ"))  module = "DSIHQ";
			else if (name.startsWith("DSI")) module = "DSIEXEC";
			else                             module = "DSIEXEC";
		}
		else if (module.equals("REP AGENT"))
		{
			module = "REPAGENT";
		}
		else if (module.equals("dCM"))
		{
			module = "CM";
		}
		
//System.out.println("getCounter(): module='"+module+"'.   name='"+name+"', instance='"+instance+"'.");
		if (name.startsWith("*") || name.startsWith("#") )
		{
			name = name.substring(1);

			// Do it once more
			if (name.startsWith("*") || name.startsWith("#") )
				name = name.substring(1);
		}
		String key = module + ":" + name;
//		return _counterNameMap.get(name);
		return _counterNameMap.get(key);
	}
	
	protected void init(Connection conn)
	{
//		_rsCounterDict = new RsCounterDict();

		String sql = "uninitialized";

		boolean inRssd = false;
		try
		{
			Statement stmnt = conn.createStatement();

			// Connect to the RSSD
			sql = "connect to rssd";
			stmnt.executeUpdate(sql);
			inRssd = true;

			// Get counter info
			sql = "select counter_id, counter_name, module_name, display_name, counter_status, description from rs_statcounters";
			ResultSet rs = stmnt.executeQuery(sql);
			while (rs.next())
			{
				StatCounterEntry c = new StatCounterEntry();
				c._counterId     = rs.getInt   (1);
				c._counterName   = rs.getString(2);
				c._moduleName    = rs.getString(3);
				c._displayName   = rs.getString(4);
				c._counterStatus = rs.getInt   (5);
				c._description   = rs.getString(6);

				addCounter(c);
			}
			rs.close();
			
			// Leave the RSSD
			sql = "disconnect";
			stmnt.executeUpdate(sql);
			inRssd = false;

			stmnt.close();
		}
		catch (SQLException e)
		{
			_logger.error("Problems getting counter information from the RSSD. stillInRssd="+inRssd+", sql="+sql, e);
		}
		finally
		{
			if (inRssd)
			{
				try
				{
					Statement stmnt = conn.createStatement();
					stmnt.executeUpdate("disconnect");
					stmnt.close();
				}
				catch (SQLException e2)
				{
					_logger.error("populateStatCounters(): Problems leaving the RSSD using 'disconnect'");
				}
			}
		}
	}

	
	public static class StatCounterEntry
	{
		int    _counterId;     // ex: 5007
		String _counterName;   // ex: DSI: Transaction groups succeeded
		String _moduleName;    // ex: DSI
		String _displayName;   // ex: DSITranGroupsSucceeded
		int    _counterStatus; // ex: 652
		String _description;   // ex: Transaction groups applied successfully to a target database by a DSI thread. This includes transactions that were successfully committed or rolled back according to their final disposition.

		/**
		 * From the RepServer Reference Manual - System Tables - rs_statcounters
		 * <p>
		 * Counter status. Bit-mask values are:
		 * <ul>
		 *   <li> 0x001 � internal use, does not display </li>
		 *   <li> 0x002 � internal use, does not display </li>
		 *   <li> 0x004 � sysmon (counter flushed as output of admin statistics, sysmon) </li>
		 *   <li> 0x008 � must sample (counter sampled at all times) </li>
		 *   <li> 0x010 � no reset (counter is never reset) </li>
		 *   <li> 0x020 � duration (counter records amount of time to complete an action, usually in .01 seconds) </li>
		 *   <li> 0x040 � internal use, does not display </li>
		 *   <li> 0x080 � keep old (previous value of counter retained, usually to aid calculation during next observation period) </li>
		 *   <li> 0x100 � internal use, does not display </li>
		 *   <li> 0x200 � observer </li>
		 *   <li> 0x400 � monitor </li>
		 *   <li> 0x800 � internal use, does not display </li>
		 * </ul>
		 * 
		 * @return a description
		 */
		public String getStatusDesc()
		{
			return getStatusDesc(", ");
		}
		public String getStatusDesc(String separator)
		{
			String desc = "";
			if ( (_counterStatus & 0x01)  == 0x01)  desc += "0x01='internal use, does not display'" + separator;
			if ( (_counterStatus & 0x02)  == 0x02)  desc += "0x02='internal use, does not display'" + separator;
			if ( (_counterStatus & 0x04)  == 0x04)  desc += "0x04='sysmon (counter flushed as output of admin statistics, sysmon)'" + separator;
			if ( (_counterStatus & 0x08)  == 0x08)  desc += "0x08='must sample (counter sampled at all times)'" + separator;
			if ( (_counterStatus & 0x10)  == 0x10)  desc += "0x10='no reset (counter is never reset)'" + separator;
			if ( (_counterStatus & 0x20)  == 0x20)  desc += "0x20='duration (counter records amount of time to complete an action, usually in .01 seconds)'" + separator;
			if ( (_counterStatus & 0x40)  == 0x40)  desc += "0x40='internal use, does not display', ";
			if ( (_counterStatus & 0x80)  == 0x80)  desc += "0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)'" + separator;
			if ( (_counterStatus & 0x100) == 0x100) desc += "0x100='internal use, does not display'" + separator;
			if ( (_counterStatus & 0x200) == 0x200) desc += "0x200='observer'" + separator;
			if ( (_counterStatus & 0x400) == 0x400) desc += "0x400='monitor'" + separator;
			if ( (_counterStatus & 0x800) == 0x800) desc += "0x800='internal use, does not display'" + separator;
			
			return desc;
		}

		public String getToolTipText()
		{
			StringBuilder sb = new StringBuilder();

			sb.append("<html>");
			sb.append("<table BORDER=0 CELLSPACING=1 CELLPADDING=1>");
			sb.append("  <tr> <td><b>Description: </b></td> <td>").append(_description)         .append("</td> </tr>\n");
			sb.append("  <tr> <td><b>Display Name:</b></td> <td>").append(_displayName)         .append("</td> </tr>\n");
			sb.append("  <tr> <td><b>Module:      </b></td> <td>").append(_moduleName)          .append("</td> </tr>\n");
			sb.append("  <tr> <td><b>Counter Id:  </b></td> <td>").append(_counterId)           .append("</td> </tr>\n");
			sb.append("  <tr> <td><b>Counter Name:</b></td> <td>").append(_counterName)         .append("</td> </tr>\n");
			sb.append("  <tr> <td><b>Status:      </b></td> <td>").append(_counterStatus)       .append("</td> </tr>\n");
			sb.append("  <tr> <td><b>Status Desc: </b></td> <td>").append(getStatusDesc("<br>")).append("</td> </tr>\n");
			sb.append("</table>");
			sb.append("</html>");

			return sb.toString();
		}
	}
}

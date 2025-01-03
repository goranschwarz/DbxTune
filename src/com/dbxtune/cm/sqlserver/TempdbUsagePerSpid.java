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
package com.dbxtune.cm.sqlserver;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.Version;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.NumberUtils;
import com.dbxtune.utils.Ver;

public class TempdbUsagePerSpid
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private String _sql_session = null;
	private String _sql_task    = null;
	
	private int _queryTimeout = 3;
	
	public static final String  PROPKEY_debugOnRefreshPrintInfo = "TempdbUsagePerSpid.debug.on.refresh.print.info";
	public static final boolean DEFAULT_debugOnRefreshPrintInfo = false;
//	public static final boolean DEFAULT_debugOnRefreshPrintInfo = true;
	
//	private HashMap<Integer, TempDbSpaceInfo> _prevMap;                   // If we want to do "delta" calculations
	private HashMap<Integer, TempDbSpaceInfo> _infoMap = new HashMap<>(); // Stores latest information <spid, info>

	private static TempdbUsagePerSpid _instance;
	
	public static TempdbUsagePerSpid getInstance()
	{
		if (_instance == null)
		{
			_instance = new TempdbUsagePerSpid();
		}

		return _instance;
	}

	/**
	 * Close the instance, a new one will be created at next getInstance()
	 */
	public void close()
	{
		_instance = null;
	}
	
	/**
	 * Get SQL for SESSION 
	 * 
	 * @param conn
	 * @return
	 */
	public String getSql_session(DbxConnection conn)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();
		long srvVersion = ssVersionInfo.getLongVersion();

		String dm_db_session_space_usage = "dm_db_session_space_usage";

		if (ssVersionInfo.isAzureSynapseAnalytics())
		{
			dm_db_session_space_usage = "dm_pdw_nodes_db_session_space_usage";
		}

		String user_objects_deferred_dealloc_page_count_1 = "       ,user_objects_deferred_dealloc_page_count = 0 \n";
		String user_objects_deferred_dealloc_page_count_2 = "";
		if (srvVersion >= Ver.ver(2014))
		{
			user_objects_deferred_dealloc_page_count_1 = "       ,user_objects_deferred_dealloc_page_count \n";
			user_objects_deferred_dealloc_page_count_2 = "       OR user_objects_deferred_dealloc_page_count > 0 \n";
		}

		String sql = ""
			    + "SELECT /* " + Version.getAppName() + ":" + this.getClass().getSimpleName() + " */ \n"
			    + "        session_id \n"
			    + "       ,user_objects_alloc_page_count \n"
			    + "       ,user_objects_dealloc_page_count \n"
			    + "       ,internal_objects_alloc_page_count \n"
			    + "       ,internal_objects_dealloc_page_count \n"
			    + user_objects_deferred_dealloc_page_count_1
			    + "FROM tempdb.sys." + dm_db_session_space_usage + " \n"
			    + "WHERE database_id = db_id('tempdb') \n"
			    + "  AND (   user_objects_alloc_page_count       > 0 \n"
			    + "       OR user_objects_dealloc_page_count     > 0 \n"
			    + "       OR internal_objects_alloc_page_count   > 0 \n"
			    + "       OR internal_objects_dealloc_page_count > 0 \n"
			    + user_objects_deferred_dealloc_page_count_2
			    + "      ) \n"
			    + "";

		return sql;
	}

	/**
	 * Get SQL for TASK
	 * 
	 * @param conn
	 * @return
	 */
	public String getSql_task(DbxConnection conn)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();
//		long srvVersion = ssVersionInfo.getLongVersion();

		String dm_db_task_space_usage    = "dm_db_task_space_usage";

		if (ssVersionInfo.isAzureSynapseAnalytics())
		{
			dm_db_task_space_usage    = "dm_pdw_nodes_db_task_space_usage";
		}

		String sql = ""
			    + "SELECT /* " + Version.getAppName() + ":" + this.getClass().getSimpleName() + " */ \n"
			    + "        session_id \n"
			    + "       ,count(*)                                 AS task_count \n"
			    + "       ,sum(user_objects_alloc_page_count      ) AS user_objects_alloc_page_count \n"
			    + "       ,sum(user_objects_dealloc_page_count    ) AS user_objects_dealloc_page_count \n"
			    + "       ,sum(internal_objects_alloc_page_count  ) AS internal_objects_alloc_page_count \n"
			    + "       ,sum(internal_objects_dealloc_page_count) AS internal_objects_dealloc_page_count \n"
			    + "FROM tempdb.sys. " + dm_db_task_space_usage + " \n"
			    + "WHERE database_id = db_id('tempdb') \n"
			    + "  AND (   user_objects_alloc_page_count       > 0 \n"
			    + "       OR user_objects_dealloc_page_count     > 0 \n"
			    + "       OR internal_objects_alloc_page_count   > 0 \n"
			    + "       OR internal_objects_dealloc_page_count > 0 \n"
			    + "      ) \n"
			    + "GROUP BY session_id \n"
			    + "";

		return sql;
	}

	/**
	 * Refresh any counters
	 * 
	 * @param conn
	 */
	public synchronized void refresh(DbxConnection conn)
	{
		if (_sql_session == null) _sql_session = getSql_session(conn);
		if (_sql_task    == null) _sql_task    = getSql_task   (conn);

		String sql = null;
		
		HashMap<Integer, TempDbSpaceInfo> tmpInfoMap = new HashMap<>();

		// SESSION
		sql = _sql_session;
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(_queryTimeout);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
				{
					int  session_id                               = rs.getInt(1);
					long user_objects_alloc_page_count            = rs.getLong(2);
					long user_objects_dealloc_page_count          = rs.getLong(3);
					long internal_objects_alloc_page_count        = rs.getLong(4);
					long internal_objects_dealloc_page_count      = rs.getLong(5);
					long user_objects_deferred_dealloc_page_count = rs.getLong(6);

					// If we havn't got an entry create one, and add it
					TempDbSpaceInfo entry = tmpInfoMap.get(session_id);
					if (entry == null)
					{
						entry = new TempDbSpaceInfo();
						tmpInfoMap.put(session_id, entry);
					}
					
					entry.session_id                                = session_id;
					entry.user_objects_alloc_page_count            += user_objects_alloc_page_count;
					entry.user_objects_dealloc_page_count          += user_objects_dealloc_page_count;
					entry.internal_objects_alloc_page_count        += internal_objects_alloc_page_count;
					entry.internal_objects_dealloc_page_count      += internal_objects_dealloc_page_count;
					entry.user_objects_deferred_dealloc_page_count += user_objects_deferred_dealloc_page_count;
				}
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems refreshing 'tempdb sessions' from 'tempdb.sys.dm_db_session_space_usage' using SQL=" + sql, ex);
		}
		
		// TASK
		sql = _sql_task;
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(_queryTimeout);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
				{
					int  session_id                          = rs.getInt(1);
					int  task_count                          = rs.getInt(2);
					long user_objects_alloc_page_count       = rs.getLong(3);
					long user_objects_dealloc_page_count     = rs.getLong(4);
					long internal_objects_alloc_page_count   = rs.getLong(5);
					long internal_objects_dealloc_page_count = rs.getLong(6);

					// If we havn't got an entry create one, and add it
					TempDbSpaceInfo entry = tmpInfoMap.get(session_id);
					if (entry == null)
					{
						entry = new TempDbSpaceInfo();
						tmpInfoMap.put(session_id, entry);
					}
					
					entry.session_id                                = session_id;
					entry.task_count                               += task_count;
					entry.user_objects_alloc_page_count            += user_objects_alloc_page_count;
					entry.user_objects_dealloc_page_count          += user_objects_dealloc_page_count;
					entry.internal_objects_alloc_page_count        += internal_objects_alloc_page_count;
					entry.internal_objects_dealloc_page_count      += internal_objects_dealloc_page_count;
				}
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems refreshing 'tempdb task' from 'tempdb.sys.dm_db_task_space_usage' using SQL=" + sql, ex);
		}
		
		// Finally assign the map
//		_prevMap = _infoMap;
		_infoMap = tmpInfoMap;
		
		
		// DEBUG: to STDOUT
		boolean debugOnRefreshPrintInfo = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_debugOnRefreshPrintInfo, DEFAULT_debugOnRefreshPrintInfo);
//		if ("goran".equals(System.getProperty("user.name"))) // Always print this if: "I'm running it..."
//			debugOnRefreshPrintInfo = true;

		if (debugOnRefreshPrintInfo)
		{
			System.out.println("---- Tempdb info ----------------------------------------");
			for (TempDbSpaceInfo entry : tmpInfoMap.values())
			{
				System.out.println("Tempdb info for: " + entry);
			}
		}

		// DEBUG: to Logger
		if (_logger.isDebugEnabled())
		{
			_logger.debug("---- Tempdb info ----------------------------------------");
			for (TempDbSpaceInfo entry : tmpInfoMap.values())
			{
				_logger.debug("Tempdb info for: " + entry);
			}
		}
	}


	//--------------------------------------------------------------------
	//-- Some methods for reporting (or create HTML Table on content)
	//--------------------------------------------------------------------
	/**
	 * Get html table with the following content
	 * <ul>
	 *    <li>SPID</li>
	 *    <li>Task/Worker Count</li>
	 *    <li>Total Space Used In Mb</li>
	 *    <li>User Object Space Used In Mb</li>
	 *    <li>Internal Object Space Used In Mb</li>
	 * </ul>
	 * 
	 * Using: <br>
	 *  - borders = true<br>
	 *  - striped Rows = false<br>
	 *  - addOuterHtmlTags = false<br>
	 * 
	 * @param thresholdInMb           Only print SPID's with 'Total Space Used In Mb' above this value
	 * @param borders                 Should the table have borders
	 * @param stripedRows             Stripe the rows
	 * @param addOuterHtmlTags        add outer begin/end <code><b>&lt;html&gt;</b> html-table-tags <b>&lt;/html&gt;</b></code> tags
	 * 
	 * @return A HTML Table (if no SPID's is above the threshold, an empty string "", will be returned)
	 */
	public String toHtmlTableString(double thresholdInMb)
	{
		return toHtmlTableString(thresholdInMb, true, false, false);
	}

	/**
	 * Get html table with the following content
	 * <ul>
	 *    <li>SPID</li>
	 *    <li>Task/Worker Count</li>
	 *    <li>Total Space Used In Mb</li>
	 *    <li>User Object Space Used In Mb</li>
	 *    <li>Internal Object Space Used In Mb</li>
	 * </ul>
	 * 
	 * @param thresholdInMb           Only print SPID's with 'Total Space Used In Mb' above this value
	 * @param borders                 Should the table have borders
	 * @param stripedRows             Stripe the rows
	 * @param addOuterHtmlTags        add outer begin/end <code><b>&lt;html&gt;</b> html-table-tags <b>&lt;/html&gt;</b></code> tags
	 * 
	 * @return A HTML Table (if no SPID's is above the threshold, an empty string "", will be returned)
	 */
	public String toHtmlTableString(double thresholdInMb, boolean borders, boolean stripedRows, boolean addOuterHtmlTags)
	{
		if (_infoMap   == null) return "";
		if (_infoMap.isEmpty()) return "";
		
		StringBuilder sb = new StringBuilder(1024);

		if (addOuterHtmlTags)
			sb.append("<html>\n");

		String border      = borders ? " border=1"      : " border=0";
		String cellPadding = borders ? ""               : " cellpadding=1";
		String cellSpacing = borders ? ""               : " cellspacing=0";

		String stripeColor = "#f2f2f2"; // Light gray;

		int rowCount = 0;

		// Table start & header
		sb.append("The below table contains SPID's with tempdb usage above ").append(thresholdInMb).append(" MB.<br>\n");
		
		
		// Table start & header
		sb.append("<table class='dbx-table-basic'").append(border).append(cellPadding).append(cellSpacing).append(">\n");

		sb.append("<thead>\n");
		sb.append("<tr>\n");
		sb.append("  <th nowrap><b>SPID</b></th>\n");
		sb.append("  <th nowrap><b>Task/Worker Count</b></th>\n");
		sb.append("  <th nowrap><b>Total Space Used In Mb</b></th>\n");
		sb.append("  <th nowrap><b>User Object Space Used In Mb</b></th>\n");
		sb.append("  <th nowrap><b>Internal Object Space Used In Mb</b></th>\n");
		sb.append("</tr>\n");
		sb.append("</thead>\n");
		
		sb.append("<tbody>\n");
		for (TempDbSpaceInfo entry : _infoMap.values())
		{
			// Skip rows if below threshold
			if (entry.getTotalSpaceUsedInMb().doubleValue() < thresholdInMb)
				continue;
			
			rowCount++;

			String stripeTag = "";
			if (stripedRows && ((rowCount % 2) == 0) )
				stripeTag = " bgcolor='" + stripeColor + "'";

			sb.append("<tr").append(stripeTag).append(">\n");
			sb.append("  <td nowrap>").append( entry.getSessionId()                   ).append("</td>\n");
			sb.append("  <td nowrap>").append( entry.getTaskCount()                   ).append("</td>\n");
			sb.append("  <td nowrap>").append( entry.getTotalSpaceUsedInMb()          ).append("</td>\n");
			sb.append("  <td nowrap>").append( entry.getUserObjectSpaceUsedInMb()     ).append("</td>\n");
			sb.append("  <td nowrap>").append( entry.getInternalObjectSpaceUsedInMb() ).append("</td>\n");
			sb.append("</tr>\n");
		}
		sb.append("</tbody>\n");

		sb.append("</table>\n");

		if (addOuterHtmlTags)
			sb.append("</html>\n");

		// No rows: return empty string
		if (rowCount == 0)
			return "";
		
		return sb.toString();
	}
	
	
	//--------------------------------------------------------------------
	//-- SUMMARY methods
	//--------------------------------------------------------------------

//	public double getTotalSpaceUsedInMb()
//	{
//		if (_infoMap == null) { _logger.warn("TempdbUsagePerSpid not yet initialized or refreshed. _infoMap=null"); return 0; }
//
//		double sum = 0;
//		for (TempDbSpaceInfo entry : _infoMap.values())
//		{
//			sum += entry.getTotalSpaceUsedInMb();
//		}
//
//		return NumberUtils.round(sum, 1);
//	}
//
//	public double getUserObjectSpaceUsedInMb()
//	{
//		if (_infoMap == null) { _logger.warn("TempdbUsagePerSpid not yet initialized or refreshed. _infoMap=null"); return 0; }
//
//		double sum = 0;
//		for (TempDbSpaceInfo entry : _infoMap.values())
//		{
//			sum += entry.getUserObjectSpaceUsedInMb();
//		}
//
//		return NumberUtils.round(sum, 1);
//	}
//
//	public double getInternalObjectSpaceUsedInMb()
//	{
//		if (_infoMap == null) { _logger.warn("TempdbUsagePerSpid not yet initialized or refreshed. _infoMap=null"); return 0; }
//
//		double sum = 0;
//		for (TempDbSpaceInfo entry : _infoMap.values())
//		{
//			sum += entry.getInternalObjectSpaceUsedInMb();
//		}
//
//		return NumberUtils.round(sum, 1);
//	}

	public BigDecimal getTotalSpaceUsedInMb()
	{
		if (_infoMap == null) { _logger.warn("TempdbUsagePerSpid not yet initialized or refreshed. _infoMap=null"); return new BigDecimal(0); }

		long sum = 0;
		for (TempDbSpaceInfo entry : _infoMap.values())
		{
			sum += entry.getTotalSpaceUsedInPages();
		}

		return NumberUtils.roundAsBigDecimal(sum / 128.0, 1);
	}

	public BigDecimal getUserObjectSpaceUsedInMb()
	{
		if (_infoMap == null) { _logger.warn("TempdbUsagePerSpid not yet initialized or refreshed. _infoMap=null"); return new BigDecimal(0); }

		long sum = 0;
		for (TempDbSpaceInfo entry : _infoMap.values())
		{
			sum += entry.getUserObjectSpaceUsedInPages();
		}

		return NumberUtils.roundAsBigDecimal(sum / 128.0, 1);
	}

	public BigDecimal getInternalObjectSpaceUsedInMb()
	{
		if (_infoMap == null) { _logger.warn("TempdbUsagePerSpid not yet initialized or refreshed. _infoMap=null"); return new BigDecimal(0); }

		long sum = 0;
		for (TempDbSpaceInfo entry : _infoMap.values())
		{
			sum += entry.getInternalObjectSpaceUsedInPages();
		}

		return NumberUtils.roundAsBigDecimal(sum / 128.0, 1);
	}
	

	//--------------------------------------------------------------------
	//-- SPID methods
	//--------------------------------------------------------------------

	public int getTaskCount(int spid, int defVal)
	{
		if (_infoMap == null) { _logger.warn("TempdbUsagePerSpid not yet initialized or refreshed. _infoMap=null"); return defVal; }

		TempDbSpaceInfo entry = _infoMap.get(spid);
		if (entry == null)
			return defVal;

		return entry.getTaskCount();
	}

	public long getTotalObjectSpaceUsedInPages(int spid, long defVal)
	{
		if (_infoMap == null) { _logger.warn("TempdbUsagePerSpid not yet initialized or refreshed. _infoMap=null"); return defVal; }

		TempDbSpaceInfo entry = _infoMap.get(spid);
		if (entry == null)
			return defVal;

		return entry.getTotalSpaceUsedInPages();
	}

	public long getUserObjectSpaceUsedInPages(int spid, long defVal)
	{
		if (_infoMap == null) { _logger.warn("TempdbUsagePerSpid not yet initialized or refreshed. _infoMap=null"); return defVal; }

		TempDbSpaceInfo entry = _infoMap.get(spid);
		if (entry == null)
			return defVal;
		
		return entry.getUserObjectSpaceUsedInPages();
	}

	public long getInternalObjectSpaceUsedInPages(int spid, long defVal)
	{
		if (_infoMap == null) { _logger.warn("TempdbUsagePerSpid not yet initialized or refreshed. _infoMap=null"); return defVal; }

		TempDbSpaceInfo entry = _infoMap.get(spid);
		if (entry == null)
			return defVal;

		return entry.getInternalObjectSpaceUsedInPages();
	}

	public long getUserObjectSpaceDeferredDeallocInPages(int spid, long defVal)
	{
		if (_infoMap == null) { _logger.warn("TempdbUsagePerSpid not yet initialized or refreshed. _infoMap=null"); return defVal; }

		TempDbSpaceInfo entry = _infoMap.get(spid);
		if (entry == null)
			return defVal;
		
		return entry.getUserObjectSpaceDeferredDeallocInPages();
	}




	public BigDecimal getTotalSpaceUsedInMb(int spid, double defVal)
	{
		if (_infoMap == null) { _logger.warn("TempdbUsagePerSpid not yet initialized or refreshed. _infoMap=null"); return new BigDecimal(defVal); }

		TempDbSpaceInfo entry = _infoMap.get(spid);
		if (entry == null)
			return new BigDecimal(defVal);
		
		return entry.getTotalSpaceUsedInMb();
	}

	public BigDecimal getUserObjectSpaceUsedInMb(int spid, double defVal)
	{
		if (_infoMap == null) { _logger.warn("TempdbUsagePerSpid not yet initialized or refreshed. _infoMap=null"); return new BigDecimal(defVal); }

		TempDbSpaceInfo entry = _infoMap.get(spid);
		if (entry == null)
			return new BigDecimal(defVal);
		
		return entry.getUserObjectSpaceUsedInMb();
	}

	public BigDecimal getInternalObjectSpaceUsedInMb(int spid, double defVal)
	{
		if (_infoMap == null) { _logger.warn("TempdbUsagePerSpid not yet initialized or refreshed. _infoMap=null"); return new BigDecimal(defVal); }

		TempDbSpaceInfo entry = _infoMap.get(spid);
		if (entry == null)
			return new BigDecimal(defVal);
		
		return entry.getInternalObjectSpaceUsedInMb();
	}

	public BigDecimal getUserSpaceDeferredDeallocInMb(int spid, double defVal)
	{
		if (_infoMap == null) { _logger.warn("TempdbUsagePerSpid not yet initialized or refreshed. _infoMap=null"); return new BigDecimal(defVal); }

		TempDbSpaceInfo entry = _infoMap.get(spid);
		if (entry == null)
			return new BigDecimal(defVal);
		
		return entry.getUserSpaceDeferredDeallocInMb();
	}

	
	public TempDbSpaceInfo getEntryForSpid(int spid)
	{
		if (_infoMap == null) { _logger.warn("TempdbUsagePerSpid not yet initialized or refreshed. _infoMap=null"); return null; }

		return _infoMap.get(spid);
	}

	
	/**
	 * Entry class to hold basic values
	 * 
	 */
	public static class TempDbSpaceInfo
	{
		public int  session_id                               = -1;
		public int  task_count                               = 0;
		public long user_objects_alloc_page_count            = 0;
		public long user_objects_dealloc_page_count          = 0;
		public long internal_objects_alloc_page_count        = 0;
		public long internal_objects_dealloc_page_count      = 0;
		public long user_objects_deferred_dealloc_page_count = 0;

		public int getSessionId() { return this.session_id; }
		public int getTaskCount() { return this.task_count; }

		public long getTotalSpaceUsedInPages()                 { return getUserObjectSpaceUsedInPages() + getInternalObjectSpaceUsedInPages(); }
		public long getUserObjectSpaceUsedInPages()            { long retVal = this.user_objects_alloc_page_count     - this.user_objects_dealloc_page_count;     return (retVal < 0) ? 0 : retVal; } // if we have negative values, return 0
		public long getInternalObjectSpaceUsedInPages()        { long retVal = this.internal_objects_alloc_page_count - this.internal_objects_dealloc_page_count; return (retVal < 0) ? 0 : retVal; } // if we have negative values, return 0
		public long getUserObjectSpaceDeferredDeallocInPages() { return this.user_objects_deferred_dealloc_page_count; }

		public BigDecimal getTotalSpaceUsedInMb()                  { return NumberUtils.roundAsBigDecimal(getTotalSpaceUsedInPages()                 / 128.0, 1); }
		public BigDecimal getUserObjectSpaceUsedInMb()             { return NumberUtils.roundAsBigDecimal(getUserObjectSpaceUsedInPages()            / 128.0, 1); }
		public BigDecimal getInternalObjectSpaceUsedInMb()         { return NumberUtils.roundAsBigDecimal(getInternalObjectSpaceUsedInPages()        / 128.0, 1); }
		public BigDecimal getUserSpaceDeferredDeallocInMb()        { return NumberUtils.roundAsBigDecimal(getUserObjectSpaceDeferredDeallocInPages() / 128.0, 1); }

		public BigDecimal get_user_objects_alloc_mb()              { return NumberUtils.roundAsBigDecimal(this.user_objects_alloc_page_count            / 128.0, 1); }
		public BigDecimal get_user_objects_dealloc_mb()            { return NumberUtils.roundAsBigDecimal(this.user_objects_dealloc_page_count          / 128.0, 1); }
		public BigDecimal get_internal_objects_alloc_mb()          { return NumberUtils.roundAsBigDecimal(this.internal_objects_alloc_page_count        / 128.0, 1); }
		public BigDecimal get_internal_objects_dealloc_mb()        { return NumberUtils.roundAsBigDecimal(this.internal_objects_dealloc_page_count      / 128.0, 1); }
		public BigDecimal get_user_objects_deferred_dealloc_mb()   { return NumberUtils.roundAsBigDecimal(this.user_objects_deferred_dealloc_page_count / 128.0, 1); }

		@Override
		public String toString()
		{
			return "spid="                                          + getSessionId() 
					+ ", task_count="                               + getTaskCount()

					+ ", tot_mb="                                   + getTotalSpaceUsedInMb()
					+ ", user_mb="                                  + getUserObjectSpaceUsedInMb()
					+ ", internal_mb="                              + getInternalObjectSpaceUsedInMb()

					+ ", tot_pgs="                                  + getTotalSpaceUsedInPages()
					+ ", user_pgs="                                 + getUserObjectSpaceUsedInPages()
					+ ", internal_pgs="                             + getInternalObjectSpaceUsedInPages()

					+ ", user_objects_alloc_page_count="            + user_objects_alloc_page_count
					+ ", user_objects_dealloc_page_count="          + user_objects_dealloc_page_count

					+ ", internal_objects_alloc_page_count="        + internal_objects_alloc_page_count
					+ ", internal_objects_dealloc_page_count="      + internal_objects_dealloc_page_count
					
					+ ", user_objects_deferred_dealloc_page_count=" + user_objects_deferred_dealloc_page_count
					;
		}
	}
}

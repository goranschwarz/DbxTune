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
package com.dbxtune.cache;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.ConnectionProvider;
import com.dbxtune.utils.TimeUtils;

public class XmlPlanCacheAse 
extends XmlPlanCache
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** how often do we write messages */
	private static long _configRejectPlanTimeThreshold = 1 * 60 * 60 * 1000; // Write Message: Every Hour   

	/** Keep a counter of each plan name */
	private static Map<String, Integer> _lastConfigRejectPlanMap = new HashMap<>();

	/** when was the last message written */
	private static long _lastWriteRejectPlanTime = -1; // System.currentTimeMillis(); 

	public static final String  PROPKEY_xmlPlan_writeRejectTimeThreshold   = "XmlPlanCacheAse.writeRejectTimeThreshold";
	public static final int     DEFAULT_xmlPlan_writeRejectTimeThreshold   = 1 * 60 * 60 * 1000; // Write Message: Every Hour

	public XmlPlanCacheAse(ConnectionProvider connProvider)
	{
		super(connProvider);
		_configRejectPlanTimeThreshold = Configuration.getCombinedConfiguration().getLongProperty(PROPKEY_xmlPlan_writeRejectTimeThreshold, DEFAULT_xmlPlan_writeRejectTimeThreshold);
	}

	/**
	 * Check if the plan is "valid" in some sense, if it's not it wont be cached.
	 * @param xmlPlan
	 * @return
	 */
	public static boolean rejectPlan(String planName, String xmlPlan)
	{
		if (xmlPlan == null)
			return true;

		// If plan isn't really ready...
		if (xmlPlan.indexOf("<planStatus> not executed </planStatus>") >= 0)
		{
//			_logger.info("Rejecting plan named '"+planName+"' due to: <planStatus> not executed </planStatus>");
//			return true;
			
			if (_lastConfigRejectPlanMap == null)
				_lastConfigRejectPlanMap = new HashMap<>();

			// Increment count for this "slot"
			Integer count = _lastConfigRejectPlanMap.get(planName);
			if (count == null)
				count = 0;
			count++;
			_lastConfigRejectPlanMap.put(planName, count);

			// Warning on firt time or every X minute/hour
			if (_lastWriteRejectPlanTime == -1 || TimeUtils.msDiffNow(_lastWriteRejectPlanTime) > _configRejectPlanTimeThreshold)
			{
				_logger.warn("Rejected "+_lastConfigRejectPlanMap.size()+" plan names due to '<planStatus> not executed </planStatus>'. " 
						+ "For the last '"+TimeUtils.msToTimeStr("%HH:%MM", _configRejectPlanTimeThreshold)+"' (HH:MM), "
						+ "The following plans was rejected (planName=count). " + _lastConfigRejectPlanMap);

				// Reset the values, so we can print new message in X minutes/hours
				_lastWriteRejectPlanTime = System.currentTimeMillis();
				_lastConfigRejectPlanMap = new HashMap<>();
			}
			return true;
		}
		
		return false;
	}
	
	@Override
	protected String getPlan(DbxConnection conn, String planName, int planId)
	{
		if (conn == null)
			return null;

		// Get the plan
		String xmlPlan = AseConnectionUtils.getObjectText(conn, null, planName, null, planId, conn.getDbmsVersionNumber());

		// Reject some of the plans...
		if (rejectPlan(planName, xmlPlan))
			return null;

		return xmlPlan;
	}

	@Override
	protected void getPlanBulk(DbxConnection conn, List<String> list)
	{
		String sql = "select objectName = object_name(SSQLID,2), xmlPlan = show_cached_plan_in_xml(SSQLID,0,0) from master.dbo.monCachedStatement";

		try
		{
    		Statement stmnt = conn.createStatement();
    		ResultSet rs = stmnt.executeQuery(sql);
    		while(rs.next())
    		{
    			String planName    = rs.getString(1);
    			String planContent = rs.getString(2);
    
    			// Reject some of the plans...
    			if (rejectPlan(planName, planContent))
    				continue;

    			setPlan(planName, planContent);
    		}
    		rs.close();
    		stmnt.close();
    		
    		_statBulkPhysicalReads++;
		}
		catch(SQLException ex)
		{
			_logger.error("Problems using getPlanBulk(). SQL='"+sql+"', Caught: "+ex);
		}
	}
}

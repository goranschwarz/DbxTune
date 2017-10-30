package com.asetune.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.ConnectionProvider;

public class XmlPlanCacheAse 
extends XmlPlanCache
{
	private static Logger _logger = Logger.getLogger(XmlPlanCacheAse.class);

	public XmlPlanCacheAse(ConnectionProvider connProvider)
	{
		super(connProvider);
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
			_logger.info("Rejecting plan named '"+planName+"' due to: <planStatus> not executed </planStatus>");
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

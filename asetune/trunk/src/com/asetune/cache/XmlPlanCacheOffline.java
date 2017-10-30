package com.asetune.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.pcs.PersistWriterBase;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.ConnectionProvider;

public class XmlPlanCacheOffline
extends XmlPlanCache
{
	private static Logger _logger = Logger.getLogger(XmlPlanCacheAse.class);

	public XmlPlanCacheOffline(ConnectionProvider connProvider)
	{
		super(connProvider);
	}

	@Override
	protected String getPlan(DbxConnection conn, String planName, int planId)
	{
		if (conn == null)
			return null;

		String sql = "select \"extraInfoText\" from " + PersistWriterBase.getTableName(PersistWriterBase.DDL_STORAGE, null, true) + " where \"objectName\" = '"+planName+"' and \"type\" = 'SS'";
		String xmlPlan = null;
		try
		{
    		Statement stmnt = conn.createStatement();
    		ResultSet rs = stmnt.executeQuery(sql);
    		while(rs.next())
    		{
    			xmlPlan = rs.getString(1);
    
    			setPlan(planName, xmlPlan);
    		}
    		rs.close();
    		stmnt.close();
    		
    		_statBulkPhysicalReads++;
		}
		catch(SQLException ex)
		{
			_logger.error("Problems using getPlanBulk(). SQL='"+sql+"', Caught: "+ex);
		}

		return xmlPlan;
	}

	@Override
	protected void getPlanBulk(DbxConnection conn, List<String> list)
	{
		throw new RuntimeException("Not Yet Implemeted");
//		String sql = "select objectName = object_name(SSQLID,2), xmlPlan = show_cached_plan_in_xml(SSQLID,0,0) from master.dbo.monCachedStatement";
//
//		try
//		{
//    		Statement stmnt = conn.createStatement();
//    		ResultSet rs = stmnt.executeQuery(sql);
//    		while(rs.next())
//    		{
//    			String planName    = rs.getString(1);
//    			String planContent = rs.getString(2);
//    
//    			setPlan(planName, planContent);
//    		}
//    		rs.close();
//    		stmnt.close();
//    		
//    		_statBulkPhysicalReads++;
//		}
//		catch(SQLException ex)
//		{
//			_logger.error("Problems using getPlanBulk(). SQL='"+sql+"', Caught: "+ex);
//		}
	}

}

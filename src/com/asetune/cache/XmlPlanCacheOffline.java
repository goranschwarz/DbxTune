/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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

		String schemaName = null;
		
		String sql = "select [extraInfoText] from " + PersistWriterBase.getTableName(conn, schemaName, PersistWriterBase.DDL_STORAGE, null, true) + " where [objectName] = '"+planName+"' and [type] = 'SS'";
		sql = conn.quotifySqlString(sql);
		
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

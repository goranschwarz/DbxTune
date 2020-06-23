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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.config.dict;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.RepServerUtils;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.Ver;


public class MonTablesDictionaryRs
extends MonTablesDictionary
{
    /** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(MonTablesDictionaryRs.class);

	@Override
	public void initialize(DbxConnection conn, boolean hasGui)
	{
		if (conn == null)
			return;

		setGui(hasGui);

		// This may be done from the Connection Dialog as well, but in NO-GUI mode it isn't so lets do it again...
		initializeVersionInfo(conn, hasGui);
		
		// Initialize the JTable Column Header Tooltip with proper values fetched from the DBMS
		initializeMonTabColHelper(conn, false);
		
		// finally MARK it as initialized
		setInitialized(true);
	}


	@Override
	public void initializeVersionInfo(DbxConnection conn, boolean hasGui)
	{
		if (conn == null)
			return;		

		if (isVersionInfoInitialized())
			return;

		String sql = "";
		//------------------------------------
		// - Set the DBMS Servername
		// - Get what Version the DBMS is of
		// - SORT order ID and NAME
		// - Can this possible be a SAP Business Suite System

		try
		{
			setDbmsServerName( RepServerUtils.getServerName(conn) );
		}
		catch(SQLException ex)
		{
			_logger.error("MonTablesDictionaryRs:initializeVersionInfo, RepServerUtils.getServerName(conn)", ex);
			if (hasGui)
				SwingUtils.showErrorMessage("MonTablesDictionary - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when Getting servername:\n\n"+sql, ex);
			return;
		}

		try
		{
			sql = "admin version";
			
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while ( rs.next() )
			{
				String versionStr = rs.getString(1);

				setDbmsExecutableVersionStr( versionStr );
				
				long versionNum = Ver.sybVersionStringToNumber(versionStr);
				setDbmsExecutableVersionNum( versionNum );

//System.out.println("Setting RS VersionNum = "+versionNum);
//System.out.println("Setting RS VersionStr = '"+versionStr+"'.");
//System.out.println("getDbmsExecutableVersionNum = "+getDbmsExecutableVersionNum());
//System.out.println("getDbmsExecutableVersionStr = '"+getDbmsExecutableVersionStr()+"'.");
			}
			rs.close();
		}
		catch (SQLException ex)
		{
			_logger.error("MonTablesDictionaryRs:initializeVersionInfo, sql="+sql, ex);
			if (hasGui)
				SwingUtils.showErrorMessage("MonTablesDictionary - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
			return;
		}

		//------------------------------------
		// SORT order ID and NAME
//		setDbmsSortId  ( id );
		setDbmsSortName( RepServerUtils.getRsSortorder(conn) );

//		setDbmsCharsetId  ( id ) ;
		setDbmsCharsetName( RepServerUtils.getRsCharset(conn) );
		
		
		setVersionInfoInitialized(true);
	}


	/**
	 * NO, save MonTableDictionary in PCS
	 */
	@Override
	public boolean isSaveMonTablesDictionaryInPcsEnabled()
	{
		return false;
	}

	@Override
	public void initializeMonTabColHelper(DbxConnection conn, boolean offline)
	{
		if (conn == null)
			return;

		System.out.println("FIXME: maybe move rs_counters descriptions into here...");

//		HashMap<String,MonTableEntry> monTablesMap = new HashMap<String,MonTableEntry>();
//
//		try
//		{
//			DatabaseMetaData md = conn.getMetaData();
//			
//			List<CountersModel> cmList = CounterController.getInstance().getCmList();
//			for (CountersModel cm : cmList)
//			{
//				String[] sa = cm.getMonTablesInQuery();
//				if (sa == null)
//					continue;
//
//				for (String tableName : sa)
//				{
//					MonTableEntry tEntry = new MonTableEntry();
////					tEntry._tableID      = ;
////					tEntry._columns      = ;
////					tEntry._parameters   = ;
////					tEntry._indicators   = ;
////					tEntry._size         = ;
//					tEntry._tableName    = tableName;
//					tEntry._description  = "";
//					
//					// Create substructure with the columns
//					// This is filled in BELOW (next SQL query)
//					tEntry._monTableColumns = new HashMap<String,MonTableColumnsEntry>();
//	
//					monTablesMap.put(tEntry._tableName, tEntry);
//
////					MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
////					mtd.addTable(tableName,  "");
////
////					ResultSet rs = md.getColumns(null, null, tableName, "%");
////					while(rs.next())
////					{
////						String tName = rs.getString("TABLE_NAME");
////						String cName = rs.getString("COLUMN_NAME");
////						String desc  = rs.getString("REMARKS");
////
////						try 
////						{
////							if (StringUtil.hasValue(desc))
////								mtd.addColumn(tName, cName, "<html>"+desc.replace("\n", "<br>")+"</html>");
////						}
////						catch (NameNotFoundException e) {/*ignore*/ e.printStackTrace();}
////					}
////					rs.close();
//
//					ResultSet rs = md.getColumns(null, null, tableName, "%");
//					while(rs.next())
//					{
//						String tName = rs.getString("TABLE_NAME");
//						String cName = rs.getString("COLUMN_NAME");
//						String desc  = rs.getString("REMARKS");
//
//						MonTableColumnsEntry cEntry = new MonTableColumnsEntry();
//						
////						cEntry._tableID      = ;
////						cEntry._columnID     = ;
////						cEntry._typeID       = ;
////						cEntry._precision    = ;
////						cEntry._scale        = ;
////						cEntry._length       = ;
////						cEntry._indicators   = ;
//						cEntry._tableName    = tName;
//						cEntry._columnName   = cName;
////						cEntry._typeName     = ;
//						cEntry._description  = desc;
//						
//						tEntry._monTableColumns.put(cEntry._columnName, cEntry);
//					}
//					rs.close();
//				}
//			}
//		}
//		catch (SQLException ex)
//		{
//			_logger.error("Problem initializing HANA MonTable Column Tooltip, This simply means that tooltip wont be showed in various places.", ex);
//			return;
//		}
//
//		setMonTablesDictionaryMap(monTablesMap);
	}
}

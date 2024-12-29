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
package com.dbxtune.config.dict;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.dbxtune.sql.conn.DbxConnection;

public class MonTablesDictionaryDefault extends MonTablesDictionary
{
	private static Logger _logger          = Logger.getLogger(MonTablesDictionaryDefault.class);

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


		//------------------------------------
		// - Set the DBMS Servername
		// - Get what Version the DBMS is of
		// - SORT order ID and NAME
		// - Can this possible be a SAP Business Suite System

		try { setDbmsServerName          ( conn.getDbmsServerName()    ); } catch(SQLException ex) { _logger.warn("initializeVersionInfo() problems when getting getDbmsServerName(). Caught: "+ex); }
		try { setDbmsExecutableVersionStr( conn.getDbmsVersionStr()    ); } catch(SQLException ex) { _logger.warn("initializeVersionInfo() problems when getting getDbmsVersionStr(). Caught: "+ex); }
		      setDbmsExecutableVersionNum( conn.getDbmsVersionNumber() ); 

		try { setDbmsSortName            (conn.getDbmsSortOrderName());   } catch(SQLException ex) { _logger.warn("initializeVersionInfo() problems when getting getDbmsSortOrderName(). Caught: "+ex); }
		try { setDbmsSortId              (conn.getDbmsSortOrderId());     } catch(SQLException ex) { _logger.warn("initializeVersionInfo() problems when getting getDbmsSortOrderId().   Caught: "+ex); }

		try { setDbmsCharsetName         (conn.getDbmsCharsetName());     } catch(SQLException ex) { _logger.warn("initializeVersionInfo() problems when getting getDbmsCharsetName(). Caught: "+ex); }
		try { setDbmsCharsetId           (conn.getDbmsCharsetId());       } catch(SQLException ex) { _logger.warn("initializeVersionInfo() problems when getting getDbmsCharsetId().   Caught: "+ex); }

//		//------------------------------------
//		// Can this possible be a SAP Business Suite System
//			setSapSystemInfo(sapSystemInfo);
		
		setVersionInfoInitialized(true);
	}

	/**
	 * NO, do not save MonTableDictionary in PCS
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

//		setMonTablesDictionaryMap(monTablesMap);
	}
}

package com.asetune.config.dict;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.asetune.sql.conn.DbxConnection;

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

	@Override
	public void initializeMonTabColHelper(DbxConnection conn, boolean offline)
	{
		if (conn == null)
			return;

//		setMonTablesDictionaryMap(monTablesMap);
	}
}

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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.cm;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Ver;


public class CountersModelUserDefined
    extends CountersModel
{
    private static final long serialVersionUID = 505823624208175944L;

	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(CountersModelUserDefined.class);

	/** a Map<Integer, String> that holds a Version and a specific SQL statement */
	private Map<Integer,String> _sqlVerStr = null;
	
	private String _sqlInitial = null;

	/** Remember the PK locally, since it's blanked out in CounterModel.clear() method, on disconnect */
	private List<String> _pkList = null;
	
	public CountersModelUserDefined
	(
			ICounterController counterController,
			String              name,             // Name of the Counter Model
			String              groupName,        // Group this belongs to
			String              sql,              // SQL Used to grab a sample from the counter data
			Map<Integer,String> sqlVerStr,        // a Map<Integer, String> that holds a Version and a specific SQL statement
			List<String>        pkList,           // A list of columns that will be used during diff calculations to lookup values in previous samples
			String[]            diffColumns,      // Columns to do diff calculations on
			String[]            pctColumns,       // Columns that is to be considered as Percent calculated columns, (they still need to be apart of diffColumns)
			String[]            monTables,        // What monitor tables are accessed in this query, used for TOOLTIP lookups
			String[]            dependsOnRole,    // what roles do we need
			String[]            dependsOnConfig,  // Check that these configurations are above 0
			int                 dependsOnVersion, // What version of ASE do we need to sample for this CounterModel
			int                 dependsOnCeVersion, // What version of ASE-CE do we need to sample for this CounterModel
			boolean             negativeDiffCountersToZero // if diff calculations is negative, reset the counter to zero.
	)
	{
		super(counterController, name, groupName, sql, pkList, diffColumns, pctColumns, monTables, dependsOnRole, dependsOnConfig, dependsOnVersion, dependsOnCeVersion, negativeDiffCountersToZero, false);
		
		_sqlVerStr  = sqlVerStr;
		_sqlInitial = sql;
		
		_pkList   = pkList;
	}

//	@Override
//	public String getSqlForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
//	{
//		// Treat version specific SQL
//		if (_sqlVerStr != null)
//		{
//			// Check/get version specific SQL strings
//			int     sqlVersionHigh = -1;
//			for (Map.Entry<Integer,String> entry : _sqlVerStr.entrySet()) 
//			{
//				Integer key = entry.getKey();
//				//String val = entry.getValue();
//				//do stuff here
//	
//				int sqlVersionNumInKey = key.intValue();
//	
//				// connected srvVersion, go and get the highest/closest sql version string 
//				if (srvVersion >= sqlVersionNumInKey)
//				{
////					if (sqlVersionNumInKey < 12503)
////					if (sqlVersionNumInKey < 1250030)
//					if (sqlVersionNumInKey < Ver.ver(12,5,0,3))
//					{
//						_logger.warn("Reading User Defined Counter '"+getName()+"' with specialized sql for version number '"+sqlVersionNumInKey+"'. First version number that we support is "+Ver.ver(12,5,0,3)+" (which is Ase Version 12.5.0.3 in a numbered format, ("+Ver.ver(12,5,0,3)+" is new to be able to support ServicePackage, so for version '15.7.0 SP100'="+Ver.ver(15,7,0,100)+", '15.7.0 ESD#4'="+Ver.ver(15,7,0,4)+", '15.7.0 ESD#4.2'="+Ver.ver(12,7,0,4,2)+") ), disregarding this entry.");
//					}
//					else
//					{
//						if (sqlVersionHigh <= sqlVersionNumInKey)
//						{
//							sqlVersionHigh = sqlVersionNumInKey;
//						}
//					}
//				}
//			}
//			if (sqlVersionHigh > 0)
//			{
//				_logger.info("Initializing User Defined Counter '"+getName()+"' with sql using version number '"+sqlVersionHigh+"'.");
//	
//				String val = (String) _sqlVerStr.get( new Integer(sqlVersionHigh) );
//				return val;
//			}
//		}
//		return _sqlInitial;
//	}
	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// Treat version specific SQL
		if (_sqlVerStr != null)
		{
			// Check/get version specific SQL strings
			int     sqlVersionHigh = -1;
			for (Map.Entry<Integer,String> entry : _sqlVerStr.entrySet()) 
			{
				Integer key = entry.getKey();
				//String val = entry.getValue();
				//do stuff here
	
				int sqlVersionNumInKey = key.intValue();
	
				// connected srvVersion, go and get the highest/closest sql version string 
				if (versionInfo.getLongVersion() >= sqlVersionNumInKey)
				{
					if (sqlVersionNumInKey < Ver.ver(12,5,0,3))
					{
						_logger.warn("Reading User Defined Counter '"+getName()+"' with specialized sql for version number '"+sqlVersionNumInKey+"'. First version number that we support is "+Ver.ver(12,5,0,3)+" (which is Ase Version 12.5.0.3 in a numbered format, ("+Ver.ver(12,5,0,3)+" is new to be able to support ServicePackage, so for version '15.7.0 SP100'="+Ver.ver(15,7,0,100)+", '15.7.0 ESD#4'="+Ver.ver(15,7,0,4)+", '15.7.0 ESD#4.2'="+Ver.ver(12,7,0,4,2)+") ), disregarding this entry.");
					}
					else
					{
						if (sqlVersionHigh <= sqlVersionNumInKey)
						{
							sqlVersionHigh = sqlVersionNumInKey;
						}
					}
				}
			}
			if (sqlVersionHigh > 0)
			{
				_logger.info("Initializing User Defined Counter '"+getName()+"' with sql using version number '"+sqlVersionHigh+"'.");
	
				String val = (String) _sqlVerStr.get( new Integer(sqlVersionHigh) );
				return val;
			}
		}
		return _sqlInitial;
	}

//	@Override
//	public String getSqlInitForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
//	{
//		return getSqlInit();
//	}

//	@Override
//	public String getSqlCloseForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
//	{
//		return getSqlClose();
//	}

//	@Override
//	public List<String> getPkForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
//	{
//		return _pkList;
//	}
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return _pkList;
	}

//	@Override
//	public String[] getDependsOnConfigForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
//	{
//		return getDependsOnConfig();
//	}
	
}

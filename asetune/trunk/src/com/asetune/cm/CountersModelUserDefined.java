/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.cm;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.utils.Ver;


public class CountersModelUserDefined
    extends CountersModel
{
    private static final long serialVersionUID = 505823624208175944L;

	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(CountersModelUserDefined.class);

	/** a Map<Integer, String> that holds a Version and a specific SQL statement */
	private Map<Integer,String> _sqlVerStr = null;
	
	private String _sqlInitial = null;


	public CountersModelUserDefined
	(
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
		super(name, groupName, sql, pkList, diffColumns, pctColumns, monTables, dependsOnRole, dependsOnConfig, dependsOnVersion, dependsOnCeVersion, negativeDiffCountersToZero, false);
		
		_sqlVerStr  = sqlVerStr;
		_sqlInitial = sql;
	}

	@Override
	public String getSqlForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
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
	
				// connected aseVersion, go and get the highest/closest sql version string 
				if (srvVersion >= sqlVersionNumInKey)
				{
//					if (sqlVersionNumInKey < 12503)
//					if (sqlVersionNumInKey < 1250030)
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
//	public String getSqlInitForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
//	{
//		return getSqlInit();
//	}

//	@Override
//	public String getSqlCloseForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
//	{
//		return getSqlClose();
//	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return getPk();
	}

//	@Override
//	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
//	{
//		return getDependsOnConfig();
//	}
	
}

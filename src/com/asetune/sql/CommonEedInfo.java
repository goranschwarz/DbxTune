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
package com.asetune.sql;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.microsoft.sqlserver.jdbc.SQLServerError;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import com.microsoft.sqlserver.jdbc.SQLServerWarning;
import com.sybase.jdbcx.EedInfo;

/**
 * 
 */
public class CommonEedInfo
{
//	private static final long serialVersionUID = 1L;

	private SQLException        _sqlex;

	private int					_state;
	private int					_severity;
	private String				_serverName;
	private String				_procName;
	private int					_lineNum;
	private transient ResultSet	_params;
	private int					_tranState;
	private int					_status;

	private boolean				_hasEedInfoProperties = false;
	private boolean             _supportsTranState    = false; 
	private boolean             _supportsEedParams    = false;

	public CommonEedInfo(SQLException sqlex)
	{
		_sqlex = sqlex;

		if (sqlex instanceof EedInfo) // Message from jConnect
		{
			EedInfo e = (EedInfo) sqlex;
			_state      = e.getState();
			_severity   = e.getSeverity();
			_serverName = e.getServerName();
			_procName   = e.getProcedureName();
			_lineNum    = e.getLineNumber();
			_params     = e.getEedParams();
			_tranState  = e.getTranState();
			_status     = e.getStatus();
			
			_supportsTranState = true;
			_supportsEedParams = true;
			
			_hasEedInfoProperties = true;
		}
		else if (sqlex instanceof SQLServerException || sqlex instanceof SQLServerWarning)
		{
			SQLServerError e = null;

			if (sqlex instanceof SQLServerException) e = ((SQLServerException)sqlex).getSQLServerError();
			if (sqlex instanceof SQLServerWarning  ) e = ((SQLServerWarning  )sqlex).getSQLServerError();
			
			if (e != null)
			{
				_state      = e.getErrorState();
				_severity   = e.getErrorSeverity();
				_serverName = e.getServerName();
				_procName   = e.getProcedureName();
//				_lineNum    = e.getLineNumber();  // line number is LONG in Microsoft
				_params     = null; // e.getEedParams();   // MS do not have this
				_tranState  = -1;   // e.getTranState();   // MS do not have this
				_status     = 0;    // e.getStatus();      // MS do not have this

				long lineNumber = e.getLineNumber();
				if (lineNumber < Integer.MAX_VALUE)
					_lineNum = (int) lineNumber;
				else
					throw new RuntimeException("CommonEedInfo.SQLServerException.getLineNumber: has a value above Integer.MAX_VALUE: " + lineNumber);
					
				_supportsTranState = false;
				_supportsEedParams = false;
				
				_hasEedInfoProperties = true;
			}
		}
//		else if (sqlex instanceof JtdsEedInfo) // Message from *modified* JTDS
//		{
//			JtdsEedInfo e = (JtdsEedInfo) sqlex;
//			_state      = e.getState();
//			_severity   = e.getSeverity();
//			_serverName = e.getServerName();
//			_procName   = e.getProcedureName();
//			_lineNum    = e.getLineNumber();
//			_params     = e.getEedParams();
//			_tranState  = e.getTranState();
//			_status     = e.getStatus();
//
//			_hasEedInfoProperties = true;
//		}
//		else if (sqlex instanceof SQLServerEedInfo) // Message from *modified* SQL-Server
//		{
//			SQLServerEedInfo e = (SQLServerEedInfo) sqlex;
//			_state      = e.getState();
//			_severity   = e.getSeverity();
//			_serverName = e.getServerName();
//			_procName   = e.getProcedureName();
//			_lineNum    = e.getLineNumber();
//			_params     = e.getEedParams();
//			_tranState  = e.getTranState();
//			_status     = e.getStatus();
//
//			_hasEedInfoProperties = true;
//		}
	}
	
	public static boolean hasEedInfo(SQLException sqlex)
	{
		if (sqlex instanceof EedInfo) // Message from jConnect
			return true;
		else if (sqlex instanceof SQLServerException) // SQL-Server driver 7.2
			return true;
//		else if (sqlex instanceof JtdsEedInfo) // Message from *modified* JTDS
//			return true;
//		else if (sqlex instanceof SQLServerEedInfo) // Message from *modified* SQL-Server
//			return true;
		else
			return false;
	}

	public boolean hasEedInfo()
	{
		return _hasEedInfoProperties;
	}

	public int getErrorCode()
	{
		return _sqlex.getErrorCode();
	}
	public String getSQLState()
	{
		return _sqlex.getSQLState();
	}
	public String getMessage()
	{
		return _sqlex.getMessage();
	}
	
	
	/**
	 * @return information about the internal source of the error message in the server.
	 */
	public int getState()
	{
		return _state;
	}

	/**
	 * @return the severity of the error message.
	 */
	public int getSeverity()
	{
		return _severity;
	}

	/**
	 * @return the name of the server that generated the message.
	 */
	public String getServerName()
	{
		return _serverName;
	}

	/**
	 * @return the name of the procedure that caused the error message.
	 */
	public String getProcedureName()
	{
		return _procName;
	}

	/**
	 * @return the line number of the stored procedure or query that caused the error message.
	 */
	public int getLineNumber()
	{
		return _lineNum;
	}

	/**
	 * Check if we have EED Params or not
	 * @return
	 */
	public boolean hasEedParams()
	{
		return _params != null;
	}

	/**
	 * Not yet implemented in jTDS
	 * @return a one-row result set containing any parameter values that accompany the error message.
	 */
	public ResultSet getEedParams()
	{
		return _params;
	}

	/**
	 * Not yet implemented in jTDS
	 * @return A map, with, a one-row result set containing any parameter values that accompany the error message.
	 */
	public Map<String, Object> getEedParamsAsMap()
	{
		ResultSet rs = getEedParams();

		if (rs == null)
			return Collections.emptyMap();

		// Add each of the columns to a Map
		LinkedHashMap<String, Object> eedParamsMap = new LinkedHashMap<>();
		try
		{
			if ( ! rs.isClosed() )
			{
    			ResultSetMetaData rsmd = rs.getMetaData();
    			int numCols = rsmd.getColumnCount();
    			while(rs.next())
    			{
    				for (int c=1; c<=numCols; c++)
    				{
    					String name = rsmd.getColumnLabel(c);
    					Object val  = rs.getObject(c);
    
    					eedParamsMap.put(name, val);
    				}
    			}
    		}
		}
		catch(SQLException ex)
		{
			// SQLException: JZ0R0: ResultSet has already been closed.
			if ( ! "JZ0R0".equals(ex.getSQLState()) )
				eedParamsMap.put("SQLException", ex);
		}

		return eedParamsMap;
	}

	/**
	 * Works for jConnect<br>
	 * Not yet implemented in jTDS, SQLServer and will always for now return -1<br>
	 * <p>
	 * This will return one of the following transaction states:<br>
	 * 0 – the connection is currently in an extended transaction.<br>
	 * 1 – the previous transaction committed successfully.<br>
	 * 3 – the previous transaction aborted.<br>
	 * 
	 * @return -1 if not supported otherwise
	 */
	public int getTranState()
	{
		return _tranState;
	}

	public boolean supportsTranState()
	{
		return _supportsTranState;
	}
	public boolean supportsEedParams()
	{
		return _supportsEedParams;
	}
	
	/**
	 * @return a 1 if there are parameter values, and returns a 0 if there are no parameter values in the message.
	 */
	public int getStatus()
	{
		return _status;
	}
}


/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune.cloud;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Normalized Cloud database error-log entry mapped to the 28-column schema of Postgres <code>CmErrorLog</code>.
 */
public class CloudLogEntry
{
	private String    _insertId;
	private String    _rawTimestampIso;

	private Timestamp _logTime;
	private String    _userName;
	private String    _databaseName;
	private Integer   _processId;
	private String    _connectionFrom;
	private String    _sessionId;
	private Long      _sessionLineNum;
	private String    _commandTag;
	private Timestamp _sessionStartTime;
	private String    _virtualTransactionId;
	private Long      _transactionId;
	private String    _errorSeverity;
	private String    _sqlStateCode;
	private String    _message;
	private String    _detail;
	private String    _hint;
	private String    _internalQuery;
	private Integer   _internalQueryPos;
	private String    _context;
	private String    _query;
	private Integer   _queryPos;
	private String    _location;
	private String    _applicationName;
	private String    _backendType;
	private Integer   _leaderPid;
	private Long      _queryId;
	private String    _logfile;
	private Long      _rowId;

	public String    getInsertId()                             { return _insertId; }
	public void      setInsertId(String insertId)              { _insertId = insertId; }

	public String    getRawTimestampIso()                      { return _rawTimestampIso; }
	public void      setRawTimestampIso(String rawIso)         { _rawTimestampIso = rawIso; }

	public Timestamp getLogTime()                              { return _logTime; }
	public void      setLogTime(Timestamp logTime)             { _logTime = logTime; }

	public String    getUserName()                             { return _userName; }
	public void      setUserName(String userName)              { _userName = userName; }

	public String    getDatabaseName()                         { return _databaseName; }
	public void      setDatabaseName(String databaseName)      { _databaseName = databaseName; }

	public Integer   getProcessId()                            { return _processId; }
	public void      setProcessId(Integer processId)           { _processId = processId; }

	public String    getConnectionFrom()                       { return _connectionFrom; }
	public void      setConnectionFrom(String connectionFrom)  { _connectionFrom = connectionFrom; }

	public String    getSessionId()                            { return _sessionId; }
	public void      setSessionId(String sessionId)            { _sessionId = sessionId; }

	public Long      getSessionLineNum()                       { return _sessionLineNum; }
	public void      setSessionLineNum(Long sessionLineNum)    { _sessionLineNum = sessionLineNum; }

	public String    getCommandTag()                           { return _commandTag; }
	public void      setCommandTag(String commandTag)          { _commandTag = commandTag; }

	public Timestamp getSessionStartTime()                     { return _sessionStartTime; }
	public void      setSessionStartTime(Timestamp ts)         { _sessionStartTime = ts; }

	public String    getVirtualTransactionId()                 { return _virtualTransactionId; }
	public void      setVirtualTransactionId(String vxid)      { _virtualTransactionId = vxid; }

	public Long      getTransactionId()                        { return _transactionId; }
	public void      setTransactionId(Long transactionId)      { _transactionId = transactionId; }

	public String    getErrorSeverity()                        { return _errorSeverity; }
	public void      setErrorSeverity(String errorSeverity)    { _errorSeverity = errorSeverity; }

	public String    getSqlStateCode()                         { return _sqlStateCode; }
	public void      setSqlStateCode(String sqlStateCode)      { _sqlStateCode = sqlStateCode; }

	public String    getMessage()                              { return _message; }
	public void      setMessage(String message)                { _message = message; }

	public String    getDetail()                               { return _detail; }
	public void      setDetail(String detail)                  { _detail = detail; }

	public String    getHint()                                 { return _hint; }
	public void      setHint(String hint)                      { _hint = hint; }

	public String    getInternalQuery()                        { return _internalQuery; }
	public void      setInternalQuery(String internalQuery)    { _internalQuery = internalQuery; }

	public Integer   getInternalQueryPos()                     { return _internalQueryPos; }
	public void      setInternalQueryPos(Integer pos)          { _internalQueryPos = pos; }

	public String    getContext()                              { return _context; }
	public void      setContext(String context)                { _context = context; }

	public String    getQuery()                                { return _query; }
	public void      setQuery(String query)                    { _query = query; }

	public Integer   getQueryPos()                             { return _queryPos; }
	public void      setQueryPos(Integer queryPos)             { _queryPos = queryPos; }

	public String    getLocation()                             { return _location; }
	public void      setLocation(String location)              { _location = location; }

	public String    getApplicationName()                      { return _applicationName; }
	public void      setApplicationName(String appName)        { _applicationName = appName; }

	public String    getBackendType()                          { return _backendType; }
	public void      setBackendType(String backendType)        { _backendType = backendType; }

	public Integer   getLeaderPid()                            { return _leaderPid; }
	public void      setLeaderPid(Integer leaderPid)           { _leaderPid = leaderPid; }

	public Long      getQueryId()                              { return _queryId; }
	public void      setQueryId(Long queryId)                  { _queryId = queryId; }

	public String    getLogfile()                              { return _logfile; }
	public String    getLogFileName()                          { return _logfile; }
	public void      setLogfile(String logfile)                { _logfile = logfile; }

	public Long      getRowId()                                { return _rowId; }
	public void      setRowId(Long rowId)                      { _rowId = rowId; }

	/**
	 * Convert this entry into a 28-element row matching the exact column order of Postgres <code>CmErrorLog</code>.
	 */
	public List<Object> toCmErrorLogRow()
	{
		List<Object> row = new ArrayList<>(28);
		row.add(_logTime);              //  1: log_time
		row.add(_userName);             //  2: user_name
		row.add(_databaseName);         //  3: database_name
		row.add(_processId);            //  4: process_id
		row.add(_connectionFrom);       //  5: connection_from
		row.add(_sessionId);            //  6: session_id
		row.add(_sessionLineNum);       //  7: session_line_num
		row.add(_commandTag);           //  8: command_tag
		row.add(_sessionStartTime);     //  9: session_start_time
		row.add(_virtualTransactionId); // 10: virtual_transaction_id
		row.add(_transactionId);        // 11: transaction_id
		row.add(_errorSeverity);        // 12: error_severity
		row.add(_sqlStateCode);         // 13: sql_state_code
		row.add("");                    // 14: sql_state_code_desc (populated in CmErrorLog.localCalculation)
		row.add(_message);              // 15: message
		row.add(_detail);               // 16: detail
		row.add(_hint);                 // 17: hint
		row.add(_internalQuery);        // 18: internal_query
		row.add(_internalQueryPos);     // 19: internal_query_pos
		row.add(_context);              // 20: context
		row.add(_query);                // 21: query
		row.add(_queryPos);             // 22: query_pos
		row.add(_location);             // 23: location
		row.add(_applicationName);      // 24: application_name
		row.add(_backendType);          // 25: backend_type
		row.add(_leaderPid);            // 26: leader_pid
		row.add(_queryId);              // 27: query_id
		row.add(_logfile);              // 28: pg_current_logfile
		return row;
	}
}

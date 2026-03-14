/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;

/**
 * NOTE: THIS IS NOT YET IMPLEMENTED AS IT SHOULD BE
 * <p>
 * WORK IN Progress
 */
public interface SqlScriptExecutorCallback
{
	void onScriptExecuteStart();
	void onScriptExecuteEnd  (int execTimeInMs);

	void onBatchExecuteStart(int batchId, String sqlText);
	void onBatchExecuteEnd  (int batchId, String sqlText, int execTimeInMs);

	void onReadResultSetStart(int batchId, int resultSetId, String sqlText);
	void processResultSet    (int batchId, int resultSetId, String sqlText, ResultSet rs);
	void onReadResultSetEnd  (int batchId, int resultSetId, String sqlText, int rowCount, int readTimeInMs);

	void onUpdateCount(int batchId, String sqlText, int rowCount);
	
	void onMessage         (int batchId, String sqlText, String sqlMsg);
	void onWarningMessage  (int batchId, String sqlText, String sqlMsg, SQLWarning sqlw);
	void onErrorMessage    (int batchId, String sqlText, String sqlMsg, SQLException sqlEx);
}

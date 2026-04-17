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
package com.dbxtune;

/**
 * Types of on-demand (and nightly) data extractions that a {@link ICounterController}
 * implementation may support.
 *
 * <p>Each enum value represents one extraction task that can be triggered either:
 * <ul>
 *   <li>Automatically — during {@code doLastRecordingActionBeforeDatabaseRollover} at midnight</li>
 *   <li>On-demand    — via {@link ICounterController#triggerExtractionOf(ExtractionType)}</li>
 * </ul>
 *
 * <p>Not every DBMS supports every extraction type; use
 * {@link ICounterController#getSupportedExtractionTypes()} to discover what is available
 * for a specific controller before calling {@code triggerExtractionOf}.
 *
 * <p>Adding a new extraction type:
 * <ol>
 *   <li>Add a value here</li>
 *   <li>Override {@code getSupportedExtractionTypes()} in the relevant
 *       {@code CounterController*} subclass</li>
 *   <li>Handle the new case in that subclass's {@code triggerExtractionOf} switch</li>
 * </ol>
 */
public enum ExtractionType
{
	/**
	 * SQL Server — copies Query Store data from the monitored SQL Server databases into
	 * H2 schemas named {@code qs:<DatabaseName>} in the recording database.
	 * Supported by: {@code CounterControllerSqlServer}
	 */
	QUERY_STORE,

	/**
	 * SQL Server — extracts deadlock graph XML captured in Extended Events / system health
	 * into the recording database for reporting and AI analysis.
	 * Supported by: {@code CounterControllerSqlServer}
	 */
	DEADLOCK,

	/**
	 * SQL Server — captures SQL Server Agent job execution history into the recording
	 * database for timeline correlation and performance analysis.
	 * Supported by: {@code CounterControllerSqlServer}
	 */
	JOB_SCHEDULER,

	/**
	 * SQL Server + Sybase ASE — captures database backup history for SLA reporting and
	 * RTO/RPO analysis.
	 * Supported by: {@code CounterControllerSqlServer}, {@code CounterControllerAse}
	 */
	BACKUP_HISTORY,
}

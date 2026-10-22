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
package com.dbxtune.pcs.report.content.mysql;

import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.ReportEntryAbstract;

/**
 * What the MySQL report sections have in common (as OracleAbstract / PostgresAbstract).
 * <p>
 * NOTE: The "Table Information" (what a table and its indexes look like, from the DDL Storage) is not here yet:
 * MySqlTune does not look up any DDL yet (no ObjectLookupInspectorMySql), so there is nothing to read.
 */
public abstract class MySqlAbstract
extends ReportEntryAbstract
{
	public MySqlAbstract(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}
}

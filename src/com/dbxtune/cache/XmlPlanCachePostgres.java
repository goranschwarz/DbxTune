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
package com.dbxtune.cache;

import java.util.List;

import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.ConnectionProvider;

/**
 * Just a dummy implementation (to reuse ASE ... but we just use to hold "some" plans in memory)
 * <p>
 * The idea is to add it from CmErrorLog or the consumer of PCS DDL Storage
 * so we can pick up the entries from CmActiveStatements and/or CmPgStatements
 */
public class XmlPlanCachePostgres 
extends XmlPlanCache
{
//	private static Logger _logger = Logger.getLogger(XmlPlanCachePostgres.class);

	public XmlPlanCachePostgres(ConnectionProvider connProvider)
	{
		super(connProvider);
	}

	@Override
	protected String getPlan(DbxConnection conn, String planName, int planId)
	{
		// not implemented... since we can't get the plan from anywhere at runtime
		return null;
	}

	@Override
	protected void getPlanBulk(DbxConnection conn, List<String> list)
	{
		// not implemented... since we can't get the plan from anywhere at runtime
	}
}

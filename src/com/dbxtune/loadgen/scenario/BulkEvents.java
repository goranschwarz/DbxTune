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
package com.dbxtune.loadgen.scenario;

import java.sql.Timestamp;

import com.dbxtune.loadgen.LgSession;

/**
 * Load 500 session_event rows in one transaction, without updating statistics (they go stale over time).
 */
public class BulkEvents extends Scenario
{
	private static final int ROWS = 500;

	public BulkEvents()
	{
		super("bulk_events", Kind.BATCH, 3, "Insert 500 session_event rows in one transaction, no statistics update");
	}

	@Override
	public void execute(RunContext ctx, LgSession s)
	throws Exception
	{
		// Reserve ROWS ids in one go
		long first;
		s.begin();
		try
		{
			s.update(tag() + "update counter_table set next_val = next_val + ? where name = 'event'", ROWS);
			first = ((Number) s.queryValue(tag() + "select next_val from counter_table where name = 'event'")).longValue() - ROWS;
			s.commit();
		}
		catch (Exception ex)
		{
			s.rollbackQuietly();
			throw ex;
		}

		String[] types = {"PAGE_VIEW", "SEARCH", "CART_ADD"};
		Timestamp now = new Timestamp(System.currentTimeMillis());
		s.begin();
		try
		{
			for (int i=0; i<ROWS && !ctx.isStopping(); i++)
				s.update(tag() + "insert into session_event (event_id, customer_id, event_ts, event_type, payload) values (?, ?, ?, ?, ?)",
						first + i, ctx.randomCustomer(), now, types[ctx.rnd().nextInt(types.length)], "bulk " + "y".repeat(200 + ctx.rnd().nextInt(800)));
			s.commit();
		}
		catch (Exception ex)
		{
			s.rollbackQuietly();
			throw ex;
		}
	}
}

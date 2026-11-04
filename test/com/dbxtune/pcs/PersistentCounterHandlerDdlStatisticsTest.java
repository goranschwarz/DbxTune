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
package com.dbxtune.pcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.dbxtune.pcs.inspection.ObjectLookupInspectorAbstract;
import com.dbxtune.pcs.sqlcapture.SqlCaptureDetails;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * Runs the real handler threads with a fake inspector and a fake writer, and checks every counter in the errorlog line.
 */
public class PersistentCounterHandlerDdlStatisticsTest
{
	/** Only 'orders' exists. The name 'alias_of_orders' finds the same object. */
	public static class FakeInspector
	extends ObjectLookupInspectorAbstract
	{
		@Override public boolean allowInspection(ObjectLookupQueueEntry entry) { return ! "blocked".equals(entry._objectName); }
		@Override public void onConnect(DbxConnection conn) {}
		@Override public DbxConnection createConnection() { return null; }
		@Override public boolean checkConnection(DbxConnection conn) { return true; }

		@Override
		public List<DdlDetails> doObjectInfoLookup(DbxConnection conn, ObjectLookupQueueEntry qe, PersistentCounterHandler pch)
		{
			List<DdlDetails> list = new ArrayList<>();
			if ("missing".equals(qe._objectName))
			{
				pch.markDdlDetailsAsDiscarded(qe._dbname, qe._objectName);
				return list;
			}
			DdlDetails entry = new DdlDetails();
			entry.setDbname          ("shop");
			entry.setOwner           ("dbo");
			entry.setObjectName      ("orders");
			entry.setSearchDbname    (qe._dbname);
			entry.setSearchObjectName(qe._objectName);
			entry.setObjectText      ("some text");
			list.add(entry);
			return list;
		}
	}

	/** Like PersistWriterJdbc: the same object is written only once */
	public static class FakeWriter
	extends PersistWriterBase
	{
		private final Set<String> _stored    = new HashSet<>();
		private final Set<String> _discarded = new HashSet<>();

		@Override public synchronized boolean isDdlDetailsStored       (String dbname, String objectName) { return _stored   .contains(dbname + "." + objectName); }
		@Override public synchronized void    markDdlDetailsAsStored   (String dbname, String objectName) { _stored   .add(dbname + "." + objectName); }
		@Override public synchronized boolean isDdlDetailsDiscarded    (String dbname, String objectName) { return _discarded.contains(dbname + "." + objectName); }
		@Override public synchronized void    markDdlDetailsAsDiscarded(String dbname, String objectName) { _discarded.add(dbname + "." + objectName); }

		@Override
		public synchronized boolean saveDdlDetails(DdlDetails ddlDetails)
		{
			boolean alreadyStored = isDdlDetailsStored("shop", "orders");
			markDdlDetailsAsStored("shop", "orders");
			markDdlDetailsAsStored(ddlDetails.getSearchDbname(), ddlDetails.getSearchObjectName());
			return ! alreadyStored;
		}

		@Override public String        getName()                         { return "FakeWriter"; }
		@Override public String        getTargetInfo()                   { return "FakeWriter"; }
		@Override public String        getConfigStr()                    { return "FakeWriter"; }
		@Override public Configuration getConfig()                       { return null; }
		@Override public void          init(Configuration props)         {}
		@Override public void          close()                           {}
		@Override public void          startServices()                   {}
		@Override public void          stopServices(int maxWaitTimeInMs) {}
		@Override public void          saveSample(PersistContainer cont) {}
		@Override public void          saveSqlCaptureDetails(SqlCaptureDetails details) {}
	}

	@Test
	public void testAllCounters() throws Exception
	{
		Configuration conf = new Configuration();
		conf.setProperty(PersistentCounterHandler.PROPKEY_WriterClass, FakeWriter.class.getName());
		conf.setProperty(PersistentCounterHandler.PROPKEY_ddl_doDdlLookupAndStore, true);
		conf.setProperty(PersistentCounterHandler.PROPKEY_ddl_afterDdlLookupSleepTimeInMs, 0);

		try (Connection h2 = DriverManager.getConnection("jdbc:h2:mem:ddlStatTest"))
		{
			PersistentCounterHandler pch = new PersistentCounterHandler(new FakeInspector(), null);
			pch.init(conf);
			pch.setDdlLookupConnection(DbxConnection.createDbxConnection(h2));
			pch.start();

			request(pch, "orders");           // lookup, saved
			request(pch, "orders");           // alreadyStored
			request(pch, "alias_of_orders");  // lookup, NOT saved: already there under another name
			request(pch, "blocked");          // notAllowed
			request(pch, "missing");          // lookup with no result
			request(pch, "missing");          // discarded by the second check, in the lookup thread

			List<String> lines = pch._ddlLookupStatistics.rollOver(LocalDateTime.now().plusHours(2), true, false, false);
			pch.stop(false, 2000);

			assertEquals(1, lines.size());
			assertTrue(lines.get(0), lines.get(0).contains(
					" requests=6, alreadyQueued=0, alreadyStored=2, notAllowed=1, lookups=3, lookupsNoResult=1, objectsReturned=2, saved=1, notSaved=1,"));
		}
	}

	/** One at a time, so the result of the previous is known */
	private static void request(PersistentCounterHandler pch, String objectName) throws Exception
	{
		pch.addDdl("shop", objectName, "test");
		Thread.sleep(500);
	}
}

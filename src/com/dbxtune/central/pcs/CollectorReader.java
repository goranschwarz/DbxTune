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
package com.dbxtune.central.pcs;

import java.sql.Timestamp;
import java.util.concurrent.ConcurrentHashMap;

import com.dbxtune.sql.conn.DbxConnection;

public class CollectorReader
{
	private static class CollectorDbCacheEntry
	{
		private String    _srvName;
		private String    _srvDate;
		private String    _url;
		private String    _username;
		private String    _password;

		private Timestamp _startTs;
		private Timestamp _lastUsedTs;
		private int       _ttlInSec;
	}
	private static class CollectorDbCache
	{
		private ConcurrentHashMap<String, CollectorDbCacheEntry> _entries;
		
//		public DbxConnection getEntry(...)
	}

	// Abstract Overview:
	// A Controller will get data from this, and send it to the Web Page (which implements, various Collector Tabs -- More or less like in the GUI Tool)
	// Im guessing the controller will have parameters for:
	//   - srvName
	//   - ts       YYYY-HH-MM_hhmmss (or a iso xxx timestamp '.....T....')
	//   - op       {getCm|getSampleDetails|???}
	//   - cntType  {abs|diff|rate}
	// Can the CM have local graphs (like the GUI has???)
	//
	// Can we make this:
	// - Read from "Collector" databases
	// - If it's TODAY's date connect to the "active" recording...
	// - if it's "yesterday" (or NOT today) -- Connect to the *DATE* we sent in that we wanted to see...
	//
	// This should "cache" or hold a Connection Pool -- so we don't have to make a new connection to (a H2) database on every request (in H2 a "single request" means starting/stopping the DB)
	// - After X (10) minutes of "inactivity" close the connection. (for H2 it means stop that database)
	//
	// First GRAB the "overview" of a SessionSampleTime from DbxSessionSampleDetailes (to get what's available in the DB for this SAMPLE) -- Just like we do in the GUI
	// - Then the GUI will read ... 
	//     * (hmmm: can we call the Collectors PCS code to read everything... Meaning "reuse" the code/objects we already have in the GUI)
	//     * or should we simply call cm.toJson()...
	//
}

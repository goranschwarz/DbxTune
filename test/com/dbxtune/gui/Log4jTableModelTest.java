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
package com.dbxtune.gui;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.BeforeClass;
import org.junit.Test;

public class Log4jTableModelTest
{

	@BeforeClass
	public static void init() 
	{
		//System.out.println("@BeforeClass - init():");

//		Configurator.setRootLevel(Level.TRACE);
	}
	
	@Test
	public void testNoGuiSkipMessages() 
	throws SQLException
	{
		Log4jTableModel tm = new Log4jTableModel();
		tm.setNoGuiMode(true);

		tm.addMessage(createMessage("Dummy first message"));

		tm.addMessage( createMessage("When trying to initialize Counters Model 'CmSqlStatement', named 'SQL Statements'. No recording is active, which this CM depends on."));
		tm.addMessage( createMessage("The environment variable 'DBXTUNE_UD_ALARM_SOURCE_DIR' is NOT set. Setting this to 'C:\\Users\\goran\\.dbxtune'."));
		tm.addMessage( createMessage("The environment variable 'DBXTUNE_NORMALIZER_SOURCE_DIR' is NOT set. Setting this to '/home/sybase/.dbxtune'."));
		tm.addMessage( createMessage("Rejected 1 plan names due to '<planStatus> not executed </planStatus>'. For the last '01:00' (HH:MM), The following plans was rejected (planName=count). {*sq1911778937_1029785637ss*=1}"));
		tm.addMessage( createMessage("The persistent queue has 1 entries. The persistent writer might not keep in pace. The current consumer..."));
		tm.addMessage( createMessage("The persistent queue has 2 entries. The persistent writer might not keep in pace. The current consumer..."));
		tm.addMessage( createMessage("The persistent queue has 3 entries. The persistent writer might not keep in pace. The current consumer..."));
		tm.addMessage( createMessage("The persistent queue has 4 entries. The persistent writer might not keep in pace. The current consumer has been active for 00:02:32.279. H2-PerfCounters{sampleTime=00:30.408, OsLoadAvgAdj[1m=0.18, 5m=0.16, 15m=0.17, 30m=0.18, 60m=0.13], FILE_READ[abs=414138, diff=0, rate=0.0], FILE_WRITE[abs=4047, diff=0, rate=0.0], PAGE_COUNT[abs=3639781, diff=0, rate=0.0], H2_FILE_SIZE_KB[abs=2119024, diff=0, rate=0.0], H2_FILE_SIZE_MB[abs=2069, diff=0, rate=0.0], H2_FILE_NAME='INT_ASE_2021-09-14.mv.db'}."));
		tm.addMessage( createMessage("The persistent queue has 5 entries. The persistent writer might not keep in pace. The current consumer..."));
/*keep*/tm.addMessage( createMessage("The persistent queue has 6 entries. The persistent writer might not keep in pace. The current consumer...")); // only up to number 5
		tm.addMessage( createMessage("The configuration 'sql text pipe max messages' might be to low. For the last '01:00' (HH:MM), We have read 11550 rows. On 1 occations. Average read per occation was 11550 rows. And the configuration value for 'sql text pipe max messages' is 10000"));

		assertEquals("Expected 2 rows.", 2, tm.getRowCount() );
	}

	
	private Log4jLogRecord createMessage(String msg)
	{
		LogEvent logEvent = Log4jLogEvent.newBuilder().setLevel(Level.WARN).setMessage( new SimpleMessage(msg) ).build();
		
		Log4jLogRecord m = new Log4jLogRecord(logEvent);
		
//		m.setCategory();
//		m.setLevel(LogLevel.WARN);
//		m.setMessage(msg);

		return m;
	}
	
}

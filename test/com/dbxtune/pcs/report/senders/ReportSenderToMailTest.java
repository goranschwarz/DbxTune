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
package com.dbxtune.pcs.report.senders;

import static org.junit.Assert.assertEquals;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dbxtune.pcs.report.DailySummaryReportFactory;
import com.dbxtune.utils.Configuration;

public class ReportSenderToMailTest
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@BeforeClass
	public static void setupLogger() throws Exception
	{
		// Set Log4j Log Level
		Configurator.setRootLevel(Level.TRACE);
	}
	
	///////////////////////////////////////////////////
	// NOTE: CHECK the below tests
	// NOTE: they are probably not good... but I had to go for vacation...
	// NOTE: more work is needed here (and also in DailySummaryReportFactory & DailySummaryReport{Abstract/Default} & PersistWriterJdbc)
	///////////////////////////////////////////////////
	
	@Test
	public void testToMail_plain()
	{
		_logger.debug("-------------------------------------------------------- " + new Throwable().getStackTrace()[0].getMethodName() );
		Configuration cfg = new Configuration();
		Configuration.setInstance(Configuration.USER_TEMP, cfg);

		// to any server
		cfg.setProperty(ReportSenderToMail.PROPKEY_to, "dummy@dummy.com");
		assertEquals(true, DailySummaryReportFactory.isCreateReportEnabledForServer("PROD_ASE") );
	}

	@Test
	public void testToMail_srvNameRegExp()
	{
		_logger.debug("-------------------------------------------------------- " + new Throwable().getStackTrace()[0].getMethodName() );
		Configuration cfg = new Configuration();
		Configuration.setInstance(Configuration.USER_TEMP, cfg);

		// to any server
		cfg.setProperty(ReportSenderToMail.PROPKEY_to, "[ {\"serverName\":\"XXX\", \"to\":\"u1@acme.com\"}, {\"serverName\":\"PROD_.*\", \"to\":\"u2@acme.com, u3@acme.com\"}]");
		assertEquals(true, DailySummaryReportFactory.isCreateReportEnabledForServer("PROD_ASE") );
	}

	@Test
	public void testToMail_faultyJson_1()
	{
		_logger.debug("-------------------------------------------------------- " + new Throwable().getStackTrace()[0].getMethodName() );
		Configuration cfg = new Configuration();
		Configuration.setInstance(Configuration.USER_TEMP, cfg);

		// to any server
		cfg.setProperty(ReportSenderToMail.PROPKEY_to, "{ serverName : XXX }");
		assertEquals(true, DailySummaryReportFactory.isCreateReportEnabledForServer("PROD_ASE") );
	}

	@Test
	public void testToMail_faultyJson_2()
	{
		_logger.debug("-------------------------------------------------------- " + new Throwable().getStackTrace()[0].getMethodName() );
		Configuration cfg = new Configuration();
		Configuration.setInstance(Configuration.USER_TEMP, cfg);

		// to any server
		cfg.setProperty(ReportSenderToMail.PROPKEY_to, "{ \"serverName\" : \"XXX\" }");
		assertEquals(true, DailySummaryReportFactory.isCreateReportEnabledForServer("PROD_ASE") );
	}

	@Test
	public void testToMail_faultyJson_3()
	{
		_logger.debug("-------------------------------------------------------- " + new Throwable().getStackTrace()[0].getMethodName() );
		Configuration cfg = new Configuration();
		Configuration.setInstance(Configuration.USER_TEMP, cfg);

		// to any server
		cfg.setProperty(ReportSenderToMail.PROPKEY_to, "[ { \"ServerName\" : \"XXX\", \"To\" : \"xxx@xxx.com\"}, { \"serverName\" : \"XXX\" } ]");
		assertEquals(true, DailySummaryReportFactory.isCreateReportEnabledForServer("PROD_ASE") );
	}

	@Test
	public void testToMail_getAllMailToAddress()
	{
		_logger.debug("-------------------------------------------------------- " + new Throwable().getStackTrace()[0].getMethodName() );
		assertEquals(
				Arrays.asList("user1@acme.com", "user2@acme.com", "user3@acme.com"),
				MailHelper.getAllMailToAddress("[ {#serverName#:#xxx#, #to#:#user1@acme.com, user2@acme.com#}, {#serverName#:#yyy#, #to#:#user1@acme.com#}, {#serverName#:#zzz#, #to#:#user3@acme.com#} ]".replace('#', '"')) );

		assertEquals(
				Arrays.asList("user1@acme.com", "user2@acme.com", "user3@acme.com"),
				MailHelper.getAllMailToAddress("user1@acme.com, user2@acme.com, user3@acme.com") );
	}

}

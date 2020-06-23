/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.utils;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.Test;

public class ConfigurationTest
{
	@Before
	public void beforeTest()
	{
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);
	}

	@Test
	public void includeTest()
	throws Exception
	{
		System.out.println("---includeTest-------------------------------------------------------------");

		File f1 = File.createTempFile("deleteme-1-", ".property");
		File f2 = File.createTempFile("deleteme-2-", ".property");
		
		f1.deleteOnExit();
		f2.deleteOnExit();

//		System.out.println("f1='"+f1.getAbsolutePath()+"'.");
//		System.out.println("f2='"+f2.getAbsolutePath()+"'.");
		
		Configuration conf1 = new Configuration("conf1", f1.getAbsolutePath());
		conf1.setProperty("include.test", f2.getAbsolutePath());
		conf1.setProperty("include.do-not-exist", "/tmp/qwerty.should-not-exists."+System.currentTimeMillis());
		conf1.setProperty("test.1.1", "test-1-1");
		conf1.setProperty("test.1.2", "test-1-2");
		conf1.setProperty("test.duplicate", "from-conf1");
		conf1.save(true);
		
		Configuration conf2 = new Configuration("conf2", f2.getAbsolutePath());
		conf2.setProperty("test.2.1", "test-2-1");
		conf2.setProperty("test.2.2", "test-2-2");
		conf2.setProperty("test.duplicate", "from-conf2");
		conf2.save(true);
		
		Thread.sleep(100);

		Configuration conf3 = new Configuration("conf3", f1.getAbsolutePath());
		
//		System.out.println("conf3.size()="+conf3.size());
		assertEquals(5, conf3.size());
		assertEquals("from-conf1", conf3.getProperty("test.duplicate"));
	}

	@Test
	public void noTempConfig()
	throws Exception
	{
		System.out.println("---noTempConfig-------------------------------------------------------------");

		Configuration.setInstance(Configuration.USER_TEMP, new Configuration());
		Configuration.setInstance(Configuration.USER_CONF, new Configuration());
		Configuration.setInstance(Configuration.SYSTEM_CONF, new Configuration());
		Configuration.setInstance(Configuration.PCS, new Configuration());

		Configuration.setSearchOrder(
				Configuration.PCS,          // First
				Configuration.USER_TEMP,    // Second
				Configuration.USER_CONF,    // Third
				Configuration.SYSTEM_CONF); // Forth

		System.out.println("Combined Configuration Search Order '"+StringUtil.toCommaStr(Configuration.getSearchOrder())+"'.");

		Configuration sys = Configuration.getInstance(Configuration.SYSTEM_CONF);
		sys.setProperty("key.test.system.1", "val-test-system-1");
		sys.save(true);

		//------------------------------------------------
		// Set in TEMP and read from COMBINED
		Configuration userTmp = Configuration.getInstance(Configuration.USER_TEMP);
		userTmp.setProperty("tmp.key.1", "tmp-val-1");
		userTmp.save(true);

		// Check for existence
		Configuration combConf = Configuration.getCombinedConfiguration();
		String val = combConf.getProperty("tmp.key.1");
		System.out.println("cConf.getProperty('tmp.key.1')=="+val);
		assertEquals("tmp-val-1", val);
		
		//------------------------------------------------
		// Set in TEMP and read from COMBINED
		userTmp = Configuration.getInstance(Configuration.USER_TEMP);
		userTmp.setProperty("tmp.key.boolean.1", false);
		userTmp.save(true);

		// Check for existence
		combConf = Configuration.getCombinedConfiguration();
		val = combConf.getProperty("tmp.key.boolean.1");
		System.out.println("cConf.getProperty('tmp.key.boolean.1')=="+val);
		assertEquals("false", val);

		boolean bval = combConf.getBooleanProperty("tmp.key.boolean.1", true);
		System.out.println("cConf.getBooleanProperty('tmp.key.boolean.1')=="+bval);
		assertEquals(false, bval);
		
	}
}

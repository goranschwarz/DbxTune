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

		Configuration.setInstance(Configuration.USER_TEMP  , new Configuration());
		Configuration.setInstance(Configuration.USER_CONF  , new Configuration());
		Configuration.setInstance(Configuration.SYSTEM_CONF, new Configuration());
		Configuration.setInstance(Configuration.PCS        , new Configuration());

		Configuration.setSearchOrder(
				Configuration.PCS,          // First
				Configuration.USER_TEMP,    // Second
				Configuration.USER_CONF,    // Third
				Configuration.SYSTEM_CONF); // Forth

		System.out.println("Combined Configuration Search Order '"+StringUtil.toCommaStr(Configuration.getSearchOrder())+"'.");
		System.out.println("Combined Configuration Search Order, With file names: "+StringUtil.toCommaStr(Configuration.getSearchOrder(true)));

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


	@Test
	public void defaultValues_1()
	throws Exception
	{
		System.out.println("---defaultValues_1-------------------------------------------------------------");

		Configuration.setInstance(Configuration.USER_TEMP  , new Configuration());
		Configuration.setInstance(Configuration.USER_CONF  , new Configuration());
		Configuration.setInstance(Configuration.SYSTEM_CONF, new Configuration());
		Configuration.setInstance(Configuration.PCS        , new Configuration());

		Configuration.setSearchOrder(
				Configuration.PCS,          // First
				Configuration.USER_TEMP,    // Second
				Configuration.USER_CONF,    // Third
				Configuration.SYSTEM_CONF); // Forth

		System.out.println("Combined Configuration Search Order '"                  + StringUtil.toCommaStr(Configuration.getSearchOrder()) + "'.");
		System.out.println("Combined Configuration Search Order, With file names: " + StringUtil.toCommaStr(Configuration.getSearchOrder(true)));


		//------------------------------------------------
		Configuration.registerDefaultValue("dummy.string.withDefaultValue", "string-default");
		Configuration.registerDefaultValue("dummy.boolean.withDefaultValue", false);
		Configuration.registerDefaultValue("dummy.int.withDefaultValue",     -1);
		Configuration.registerDefaultValue("dummy.long.withDefaultValue",    -2L);
		Configuration.registerDefaultValue("dummy.double.withDefaultValue",  -3D);

		Configuration conf = Configuration.getCombinedConfiguration();

		// The below should return the default REGISTERED value, since no property exists in "any" of the configurations 
		assertEquals("string-default", conf.getProperty       ("dummy.string.withDefaultValue" , "xxx"));
		assertEquals(false           , conf.getBooleanProperty("dummy.boolean.withDefaultValue", true));
		assertEquals(-1              , conf.getIntProperty    ("dummy.int.withDefaultValue"    , -11));
		assertEquals(-2L             , conf.getLongProperty   ("dummy.long.withDefaultValue"   , -22L));
		assertEquals(-3D             , conf.getDoubleProperty ("dummy.double.withDefaultValue" , -33D), 0D);

		// Remove ALL of the above registered defaults (for other tests to work)
		Configuration.removeAllRegisterDefaultValues();
		
	}

	@Test
	public void defaultValues_2()
	throws Exception
	{
		System.out.println("---defaultValues_2-------------------------------------------------------------");

		Configuration.setInstance(Configuration.USER_TEMP  , new Configuration());
		Configuration.setInstance(Configuration.USER_CONF  , new Configuration());
		Configuration.setInstance(Configuration.SYSTEM_CONF, new Configuration());
		Configuration.setInstance(Configuration.PCS        , new Configuration());

		Configuration.setSearchOrder(
				Configuration.PCS,          // First
				Configuration.USER_TEMP,    // Second
				Configuration.USER_CONF,    // Third
				Configuration.SYSTEM_CONF); // Forth

		System.out.println("Combined Configuration Search Order '"                  + StringUtil.toCommaStr(Configuration.getSearchOrder()) + "'.");
		System.out.println("Combined Configuration Search Order, With file names: " + StringUtil.toCommaStr(Configuration.getSearchOrder(true)));


		//------------------------------------------------

		Configuration conf = Configuration.getCombinedConfiguration();
		Configuration tmp  = Configuration.getInstance(Configuration.USER_TEMP);

		//-----------------------------------------
		// TEST plain 'USE_DEFAULT'
		//-----------------------------------------
		tmp.setProperty("dummy.string.withDefaultValue" , "USE_DEFAULT");
		tmp.setProperty("dummy.boolean.withDefaultValue", "USE_DEFAULT");
		tmp.setProperty("dummy.int.withDefaultValue"    , "USE_DEFAULT");
		tmp.setProperty("dummy.long.withDefaultValue"   , "USE_DEFAULT");
		tmp.setProperty("dummy.double.withDefaultValue" , "USE_DEFAULT");

		// The below should return the default (second parameter to getXxxProperty), since there is NO REGISTERED Value in this test
		assertEquals("xxx", conf.getProperty       ("dummy.string.withDefaultValue" , "xxx"));
		assertEquals(null , conf.getProperty       ("dummy.string.withDefaultValue" , null));
		assertEquals(true , conf.getBooleanProperty("dummy.boolean.withDefaultValue", true));
		assertEquals(-11  , conf.getIntProperty    ("dummy.int.withDefaultValue"    , -11));
		assertEquals(-22L , conf.getLongProperty   ("dummy.long.withDefaultValue"   , -22L));
		assertEquals(-33D , conf.getDoubleProperty ("dummy.double.withDefaultValue" , -33D), 0D);

		
		//-----------------------------------------
		// TEST plain 'USE_DEFAULT: some value'
		//-----------------------------------------
		tmp.setProperty("dummy.string.withDefaultValue" , "USE_DEFAULT: 1");
		tmp.setProperty("dummy.boolean.withDefaultValue", "USE_DEFAULT: 2");
		tmp.setProperty("dummy.int.withDefaultValue"    , "USE_DEFAULT: 3");
		tmp.setProperty("dummy.long.withDefaultValue"   , "USE_DEFAULT: 4");
		tmp.setProperty("dummy.double.withDefaultValue" , "USE_DEFAULT: 5");

		// The below should return the default (second parameter to getXxxProperty), since there is NO REGISTERED Value in this test
		assertEquals("xxx", conf.getProperty       ("dummy.string.withDefaultValue" , "xxx"));
		assertEquals(true , conf.getBooleanProperty("dummy.boolean.withDefaultValue", true));
		assertEquals(-11  , conf.getIntProperty    ("dummy.int.withDefaultValue"    , -11));
		assertEquals(-22L , conf.getLongProperty   ("dummy.long.withDefaultValue"   , -22L));
		assertEquals(-33D , conf.getDoubleProperty ("dummy.double.withDefaultValue" , -33D), 0D);
	}



	@Test
	public void defaultValuesRaw_1()
	throws Exception
	{
		System.out.println("---defaultValuesRaw_1-------------------------------------------------------------");

		Configuration.setInstance(Configuration.USER_TEMP  , new Configuration());
		Configuration.setInstance(Configuration.USER_CONF  , new Configuration());
		Configuration.setInstance(Configuration.SYSTEM_CONF, new Configuration());
		Configuration.setInstance(Configuration.PCS        , new Configuration());

		Configuration.setSearchOrder(
				Configuration.PCS,          // First
				Configuration.USER_TEMP,    // Second
				Configuration.USER_CONF,    // Third
				Configuration.SYSTEM_CONF); // Forth

		System.out.println("Combined Configuration Search Order '"                  + StringUtil.toCommaStr(Configuration.getSearchOrder()) + "'.");
		System.out.println("Combined Configuration Search Order, With file names: " + StringUtil.toCommaStr(Configuration.getSearchOrder(true)));


		//------------------------------------------------
		Configuration.registerDefaultValue("dummy.string.withDefaultValue", "string-default");

		Configuration conf = Configuration.getCombinedConfiguration();
		Configuration tmp  = Configuration.getInstance(Configuration.USER_TEMP);

		// The below should return the default REGISTERED value, since no property exists in "any" of the configurations 
		assertEquals("string-default", tmp .getPropertyRaw("dummy.string.withDefaultValue" , "xxx"));
		assertEquals("string-default", conf.getPropertyRaw("dummy.string.withDefaultValue" , "xxx"));

		// Remove ALL of the above registered defaults (for other tests to work)
		Configuration.removeAllRegisterDefaultValues();
		
	}

	@Test
	public void defaultValuesRaw_2()
	throws Exception
	{
		System.out.println("---defaultValuesRaw_2-------------------------------------------------------------");

		Configuration.setInstance(Configuration.USER_TEMP  , new Configuration());
		Configuration.setInstance(Configuration.USER_CONF  , new Configuration());
		Configuration.setInstance(Configuration.SYSTEM_CONF, new Configuration());
		Configuration.setInstance(Configuration.PCS        , new Configuration());

		Configuration.setSearchOrder(
				Configuration.PCS,          // First
				Configuration.USER_TEMP,    // Second
				Configuration.USER_CONF,    // Third
				Configuration.SYSTEM_CONF); // Forth

		System.out.println("Combined Configuration Search Order '"                  + StringUtil.toCommaStr(Configuration.getSearchOrder()) + "'.");
		System.out.println("Combined Configuration Search Order, With file names: " + StringUtil.toCommaStr(Configuration.getSearchOrder(true)));


		//------------------------------------------------

		Configuration conf = Configuration.getCombinedConfiguration();
		Configuration tmp  = Configuration.getInstance(Configuration.USER_TEMP);

		//-----------------------------------------
		// TEST plain (from: tmp & combinedConf)
		//-----------------------------------------
		tmp.setProperty("dummy.string.dummy" , " xxx ");

		assertEquals("xxx"  , tmp.getPropertyRaw   ("dummy.string.dummy"));
		assertEquals(" xxx ", tmp.getPropertyRawVal("dummy.string.dummy"));

		assertEquals("xxx"  , tmp.getPropertyRaw   ("dummy.string.dummy" , "aaa"));
		assertEquals(" xxx ", tmp.getPropertyRawVal("dummy.string.dummy" , "bbb"));

		assertEquals("xxx"  , conf.getPropertyRaw   ("dummy.string.dummy"));
		assertEquals(" xxx ", conf.getPropertyRawVal("dummy.string.dummy"));

		assertEquals("xxx"  , conf.getPropertyRaw   ("dummy.string.dummy" , "aaa"));
		assertEquals(" xxx ", conf.getPropertyRawVal("dummy.string.dummy" , "bbb"));

		
		//-----------------------------------------
		// TEST plain 'USE_DEFAULT' (from: tmp & combinedConf)
		//-----------------------------------------
		tmp.setProperty("dummy.string.withDefaultValue" , "USE_DEFAULT");

		// The below should return the default (second parameter to getXxxProperty), since there is NO REGISTERED Value in this test
		assertEquals("xxx"  , tmp .getPropertyRaw   ("dummy.string.withDefaultValue" , "xxx"));
		assertEquals(" xxx ", tmp .getPropertyRawVal("dummy.string.withDefaultValue" , " xxx "));

		assertEquals("xxx"  , conf.getPropertyRaw   ("dummy.string.withDefaultValue" , "xxx"));
		assertEquals(" xxx ", conf.getPropertyRawVal("dummy.string.withDefaultValue" , " xxx "));

		
		//-----------------------------------------
		// TEST plain 'USE_DEFAULT: some value' (from: tmp & combinedConf)
		//-----------------------------------------
		tmp.setProperty("dummy.string.withDefaultValue" , "USE_DEFAULT: 1");

		// The below should return the default (second parameter to getXxxProperty), since there is NO REGISTERED Value in this test
		assertEquals("xxx"  , tmp .getPropertyRaw   ("dummy.string.withDefaultValue" , "xxx"));
		assertEquals(" xxx ", tmp .getPropertyRawVal("dummy.string.withDefaultValue" , " xxx "));

		assertEquals("xxx"  , conf.getPropertyRaw   ("dummy.string.withDefaultValue" , "xxx"));
		assertEquals(" xxx ", conf.getPropertyRawVal("dummy.string.withDefaultValue" , " xxx "));
	}
}

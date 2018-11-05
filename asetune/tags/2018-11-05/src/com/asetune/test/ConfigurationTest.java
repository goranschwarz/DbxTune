package com.asetune.test;

import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.asetune.utils.Configuration;

public class ConfigurationTest
{

	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		test1();
		test2();
	}
	
	private static void test1()
	{
		Configuration conf = new Configuration();
		conf.setProperty("dummy", "xxx");
		conf.save();

		String dummy = conf.getProperty("dummy");
		System.out.println("dummy='"+dummy+"'");
		if ( ! "xxx".equals(dummy))
			System.out.println("test 1: ----------------------- FAILED -------------------------------");
	}


	private static void test2()
	{
		Configuration conf = new Configuration();
		conf.setProperty("dummy", "xxx");
		conf.save();

		String dummy = conf.getProperty("dummy");
		System.out.println("dummy='"+dummy+"'");
		if ( ! "xxx".equals(dummy))
			System.out.println("test 1: ----------------------- FAILED -------------------------------");
	}
}

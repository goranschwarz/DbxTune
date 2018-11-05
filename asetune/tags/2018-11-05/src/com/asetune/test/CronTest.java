package com.asetune.test;

import java.util.Locale;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

import it.sauronsoftware.cron4j.SchedulingPattern;

public class CronTest
{

	private static void xxx(String testName, String cronPattern)
	{
		SchedulingPattern sp = new SchedulingPattern(cronPattern);
		boolean matches = sp.match(System.currentTimeMillis());

		String description = "";
		try
		{
			CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.CRON4J);
			CronParser parser = new CronParser(cronDefinition);
			CronDescriptor descriptor = CronDescriptor.instance(Locale.UK);
			description = descriptor.describe(parser.parse(cronPattern));
		}
		catch (UnsupportedClassVersionError ex)
		{
			description = "not supported in this java version";
		}

//		String description = null;
//		try { description = CronExpressionDescriptor.getDescription(cronPattern); }
//		catch (Exception e) {
//			e.printStackTrace();
//		}

		
		System.out.println(testName + " : " + "cron["+cronPattern+"]: == " + matches + " ---> description='"+description+"'.");
	}

	public static void main(String[] args)
	{
		xxx("0", "* * * * *");
		xxx("1", "1 * * * *");
		xxx("2", "2 * * * *");
		xxx("3", "3 * * * *");
		xxx("4", "4 * * * *");
		xxx("5", "5 * * * *");
		xxx("6", "6 * * * *");
		xxx("7", "7 * * * *");
		xxx("8", "8 * * * *");
		xxx("9", "9 * * * *");

		xxx("xx", "0-34,36-59 15 * * *");

//		xxx("fail", "");
	}
}

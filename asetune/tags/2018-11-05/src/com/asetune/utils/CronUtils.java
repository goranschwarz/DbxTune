package com.asetune.utils;

import java.util.Locale;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

public class CronUtils
{
	public static String getCronExpressionDescription(String cronExpr)
	{
		String description = "";
		try
		{
			CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.CRON4J);
			CronParser parser = new CronParser(cronDefinition);
			CronDescriptor descriptor = CronDescriptor.instance(Locale.UK);
			description = descriptor.describe(parser.parse(cronExpr));
		}
		catch (UnsupportedClassVersionError ex)
		{
			description = "not supported in this java version";
		}
		return description;
	}

	public static String getCronExpressionDescriptionForAlarms(String cronPat)
	{
		String cronPatStr = cronPat;
		boolean negation = false;
		if (cronPatStr.startsWith("!"))
		{
			negation = true;
			cronPatStr = cronPatStr.substring(1);
		}

		String description = "";
		if (cronPatStr.trim().equals("* * * * *"))
		{
			description = "any-time";
		}
		else
		{
			try
			{
				CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.CRON4J);
				CronParser parser = new CronParser(cronDefinition);
				CronDescriptor descriptor = CronDescriptor.instance(Locale.UK);
				description = descriptor.describe(parser.parse(cronPatStr));
			}
			catch (UnsupportedClassVersionError ex)
			{
				return "not supported in this java version";
			}
		}
		
		if (negation)
			return "Alarms is NOT allowed '" + description +"'.";
		else
			return "Alarms is allowed '" + description +"'.";
	}
}

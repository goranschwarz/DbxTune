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
package com.dbxtune.utils;

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

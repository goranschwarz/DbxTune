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
package com.dbxtune.test;

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

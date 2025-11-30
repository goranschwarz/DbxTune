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

import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

public class ScriptEngineTest
{
	public static void main(String[] args)
	{
		ScriptEngineManager mgr = new ScriptEngineManager();
		List<ScriptEngineFactory> factories = mgr.getEngineFactories();

		for (ScriptEngineFactory factory : factories)
		{
			System.out.println("ScriptEngineFactory Info");

			String engName = factory.getEngineName();
			String engVersion = factory.getEngineVersion();
			String langName = factory.getLanguageName();
			String langVersion = factory.getLanguageVersion();

			System.out.printf("\tScript Engine: %s (%s)\n", engName, engVersion);

			List<String> engNames = factory.getNames();
			for (String name : engNames)
			{
				System.out.printf("\tEngine Alias: %s\n", name);
			}

			System.out.printf("\tLanguage: %s (%s)\n", langName, langVersion);
		}
		
		waitForNextSample(99);
	}
	
	public static void waitForNextSample(int waitTime)
	{
		ScriptEngine runtime = null;
		StringBuffer javascript = new StringBuffer();

		try {
	        ScriptEngineManager factory = new ScriptEngineManager();
			runtime = factory.getEngineByName("JavaScript");
			javascript = new StringBuffer();

			javascript.append("1 + 1");

			double result = (Double) runtime.eval(javascript.toString());

			System.out.println("Result: " + result);
		}
		catch (Exception ex) 
		{
			System.out.println(ex.getMessage());
		}		
	}
}

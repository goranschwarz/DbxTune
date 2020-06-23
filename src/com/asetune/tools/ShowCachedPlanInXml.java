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
package com.asetune.tools;

import java.io.File;
import java.util.List;

/**
 * THIS DOES NOT YET WORK
 * 
 * @author gorans
 */
public class ShowCachedPlanInXml
{

	public static Entry parse(String xml)
	{
		return null;
	}
	public static Entry parse(File file)
	{
		return null;
	}

	public static List<Entry> parse(List<String> xmlList)
	{
		return null;
	}

	/**
	 * A entry for a XML instance
	 * 
	 * @author gorans
	 */
	public static class Entry
	{
		private int    _planId          = -1;
		private String _planStatus      = null;
		private String _planSharing     = null;
		private long   _execCount       = -1;
		private long   _maxTime         = -1;
		private long   _avgTime         = -1;
		private long   _maxPreQueryTime = -1;
		private long   _avgPreQueryTime = -1;
		private long   _maxExecTime     = -1;
		private long   _avgExecTime     = -1;

		private List<?> _compileParameters = null;
		private List<?> _execParameters    = null;
		private List<?> _opTree            = null;
	}
}

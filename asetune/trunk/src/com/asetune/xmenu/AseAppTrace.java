/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.xmenu;

import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.tools.AseAppTraceDialog;

public class AseAppTrace
extends XmenuActionBase 
{

	/**
	 * 
	 */
	public AseAppTrace() 
	{
		super();
	}

	/**
	 */
	@Override 
	public void doWork() 
	{
		String spidStr  = getParamValue(0);
		int    spid     = Integer.parseInt(spidStr);

		String srvVerStr = MonTablesDictionaryManager.getInstance().getDbmsExecutableVersionStr();
		String srvName   = MonTablesDictionaryManager.getInstance().getDbmsServerName();

		AseAppTraceDialog apptrace = new AseAppTraceDialog(spid, srvName, srvVerStr);
		apptrace.setVisible(true);
	}
}

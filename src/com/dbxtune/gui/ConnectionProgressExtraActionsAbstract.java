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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.gui;

import com.dbxtune.hostmon.HostMonitorConnection;
import com.dbxtune.sql.conn.DbxConnection;

public abstract class ConnectionProgressExtraActionsAbstract 
implements ConnectionProgressExtraActions
{
	protected boolean _doInitializeVersionInfo        = true;
	protected boolean _doCheckMonitorConfig           = true;
	protected boolean _doInitMonitorDictionary        = true;
	protected boolean _doInitDbServerConfigDictionary = true;
	protected boolean _doInitCounterCollector         = true;

	public ConnectionProgressExtraActionsAbstract(boolean doInitializeVersionInfo, boolean doCheckMonitorConfig, boolean doInitMonitorDictionary, boolean doInitDbServerConfigDictionary, boolean doInitCounterCollector)
	{

		_doInitializeVersionInfo        = doInitializeVersionInfo       ;
		_doCheckMonitorConfig           = doCheckMonitorConfig          ;
		_doInitMonitorDictionary        = doInitMonitorDictionary       ;
		_doInitDbServerConfigDictionary = doInitDbServerConfigDictionary;
		_doInitCounterCollector         = doInitCounterCollector        ;
	}

	@Override public abstract boolean initializeVersionInfo(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception;
	@Override public boolean doInitializeVersionInfo()                 { return _doInitializeVersionInfo;	}
	@Override public void setInitializeVersionInfo(boolean val)        {        _doInitializeVersionInfo = val; }

	@Override public abstract boolean checkMonitorConfig(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception;
	@Override public boolean doCheckMonitorConfig()                    { return _doCheckMonitorConfig; }
	@Override public void setCheckMonitorConfig(boolean val)           {        _doCheckMonitorConfig = val; }

	@Override public abstract boolean initMonitorDictionary(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception;
	@Override public boolean doInitMonitorDictionary()                 { return _doInitMonitorDictionary; }
	@Override public void setInitMonitorDictionary(boolean val)        {        _doInitMonitorDictionary = val; }

	@Override public abstract boolean initDbServerConfigDictionary(DbxConnection conn, HostMonitorConnection hostMonConn, ConnectionProgressDialog cpd) throws Exception;
	@Override public boolean doInitDbServerConfigDictionary()          { return _doInitDbServerConfigDictionary; }
	@Override public void setInitDbServerConfigDictionary(boolean val) {        _doInitDbServerConfigDictionary = val; }

	@Override public abstract boolean initCounterCollector(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception;
	@Override public boolean doInitCounterCollector()                  { return _doInitCounterCollector; }
	@Override public void setInitCounterCollector(boolean val)         {        _doInitCounterCollector = val; }

}

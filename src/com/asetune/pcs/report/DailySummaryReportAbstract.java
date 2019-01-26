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
package com.asetune.pcs.report;

import com.asetune.pcs.report.content.DailySummaryReportContent;
import com.asetune.pcs.report.senders.IReportSender;
import com.asetune.sql.conn.DbxConnection;

public abstract class DailySummaryReportAbstract
implements IDailySummaryReport
{
	private DbxConnection _conn = null;
	private IReportSender _sender = null;
	private String        _serverName = null;
	
	private DailySummaryReportContent _reportContent = null; 

	@Override
	public void          setConnection(DbxConnection conn) { _conn = conn; }
	public DbxConnection getConnection()                   { return _conn; }

	@Override
	public void   setServerName(String serverName) { _serverName = serverName; }
	public String getServerName()                  { return _serverName; }

	@Override public void                      setReportContent(DailySummaryReportContent content) { _reportContent = content; }
	@Override public DailySummaryReportContent getReportContent()                                  { return _reportContent; }

	@Override
	public void setReportSender(IReportSender reportSender)
	{
		_sender = reportSender;
	}

	@Override
	public void init()
	throws Exception
	{
		if (_sender == null)
			throw new RuntimeException("Can't send Daily Summary Report. The sender class is null.");

		_sender.init();
		_sender.printConfig();		
	}

	@Override
	public void send()
	{
		if (_sender == null)
			throw new RuntimeException("Can't send Daily Summary Report. The sender class is null.");

		_sender.send(getReportContent());
	}


	
	
	@Override
	public abstract void create();

}

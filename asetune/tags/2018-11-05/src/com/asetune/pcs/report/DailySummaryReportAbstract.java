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

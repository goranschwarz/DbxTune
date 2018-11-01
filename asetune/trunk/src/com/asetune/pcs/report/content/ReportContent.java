package com.asetune.pcs.report.content;

public class ReportContent
{
	private String _serverName  = "";
	private String _contentText = "";
	private String _contentHtml = "";
	private boolean _hasNothingToReport = false;

	public String getServerName()   { return _serverName  == null ? "" : _serverName; }
	public String getReportAsText() { return _contentText == null ? "" : _contentText; }
	public String getReportAsHtml() { return _contentHtml == null ? "" : _contentHtml; }

	public void setServerName(String serverName) { _serverName = serverName; }
	public void setReportAsText(String text)     { _contentText = text; }
	public void setReportAsHtml(String text)     { _contentHtml = text; }

	public void setNothingToReport(boolean b)
	{
		_hasNothingToReport = b;
	}
	public boolean hasNothingToReport()
	{
		return _hasNothingToReport;
	}
}

package com.asetune.pcs.report.senders;

public abstract class ReportSenderAbstract 
implements IReportSender
{
	@Override
	public String getName() 
	{
		return this.getClass().getSimpleName();
	}
}

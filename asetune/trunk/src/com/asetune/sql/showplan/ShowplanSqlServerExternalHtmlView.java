package com.asetune.sql.showplan;

public class ShowplanSqlServerExternalHtmlView
extends ShowplanHtmlView
{
	@Override
	protected String getXsltFile()
	{
		return "src/qp.xslt";
	}

	@Override
	protected String getTemplateJarDir()
	{
		return "sqlserver/";
	}
}

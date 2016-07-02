package com.asetune.sql.showplan;

public class ShowplanAseExternalHtmlView
extends ShowplanHtmlView
{
	@Override
	protected String getXsltFile()
	{
		return "ase_qp_page.xslt";
	}

	@Override
	protected String getTemplateJarDir()
	{
		return "ase/";
	}
}

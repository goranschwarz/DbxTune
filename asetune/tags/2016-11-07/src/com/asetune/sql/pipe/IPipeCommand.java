package com.asetune.sql.pipe;

import com.asetune.sql.SqlProgressDialog;

public interface IPipeCommand
{
	public String getConfig();
	
	public void   open() throws Exception;
	public void   doPipe(Object input) throws Exception;
//	public void   doEndPoint(Object input) throws Exception;
	public void   doEndPoint(Object input, SqlProgressDialog progress) throws Exception;
	public Object getEndPointResult(String type);
	public void   close();

	public String getCmdStr();
	public String getSqlString();
}

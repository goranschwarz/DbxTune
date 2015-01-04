package com.asetune.tools.sqlw.msg;

import com.asetune.utils.ColorUtils;
import com.asetune.utils.DbUtils;

public class JDbmsOuputMessage
extends JAseMessage
{
	private static final long serialVersionUID = 1L;
	
	private static String getDbmsType(String connectedToProductName)
	{
		if (DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_ORACLE)) return "Oracle";
		if (DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_DB2_UX)) return "DB2";
		return "Oracle/DB2";
	}

	public JDbmsOuputMessage(String message, String originSql, String connectedToProductName)
	{
		super(getDbmsType(connectedToProductName)+" DBMS_OUTPUT.GET_LINE(): "+message, originSql);

		setForeground(ColorUtils.VERY_DARK_BLUE);
	}
}

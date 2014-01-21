package com.asetune.ui.tooltip.suppliers;

import java.awt.Window;

import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.StringUtil;

public class ToolTipSupplierJdbc
extends ToolTipSupplierAbstractSql
{
	public ToolTipSupplierJdbc(Window owner, ConnectionProvider connectionProvider)
	{
		super(owner, connectionProvider);
	}

	@Override
	public String getName() 
	{
		return "JDBC";
	}

	@Override
	public String getToolTipProviderFile() 
	{
		return StringUtil.envVariableSubstitution("${APPL_HOME}/resources/JDBC_tooltip_provider.xml");
	}
}

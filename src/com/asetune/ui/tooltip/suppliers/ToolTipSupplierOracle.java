package com.asetune.ui.tooltip.suppliers;

import java.awt.Window;

import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.StringUtil;

public class ToolTipSupplierOracle
extends ToolTipSupplierAbstractSql
{
	public ToolTipSupplierOracle(Window owner, ConnectionProvider connectionProvider)
	{
		super(owner, connectionProvider);
	}

	@Override
	public String getName() 
	{
		return "Oracle";
	}

	@Override
	public String getToolTipProviderFile() 
	{
		return StringUtil.envVariableSubstitution("${APPL_HOME}/resources/ORACLE_tooltip_provider.xml");
	}
}

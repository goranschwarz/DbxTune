package com.asetune.ui.tooltip.suppliers;

import java.awt.Window;

import com.asetune.ui.autocomplete.CompletionProviderAbstract;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.StringUtil;

public class ToolTipSupplierDb2
extends ToolTipSupplierAbstractSql
{
	public ToolTipSupplierDb2(Window owner, CompletionProviderAbstract compleationProvider, ConnectionProvider connectionProvider)
	{
		super(owner, compleationProvider, connectionProvider);
	}

	@Override
	public String getName() 
	{
		return "DB2";
	}

	@Override
	public String getToolTipProviderFile() 
	{
		return StringUtil.envVariableSubstitution("${APPL_HOME}/resources/DB2_tooltip_provider.xml");
	}
}

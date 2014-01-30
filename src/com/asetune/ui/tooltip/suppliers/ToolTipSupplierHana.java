package com.asetune.ui.tooltip.suppliers;

import java.awt.Window;

import com.asetune.ui.autocomplete.CompletionProviderAbstract;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.StringUtil;

public class ToolTipSupplierHana
extends ToolTipSupplierAbstractSql
{
	public ToolTipSupplierHana(Window owner, CompletionProviderAbstract compleationProvider, ConnectionProvider connectionProvider)
	{
		super(owner, compleationProvider, connectionProvider);
	}

	@Override
	public String getName() 
	{
		return "HANA";
	}

	@Override
	public String getToolTipProviderFile() 
	{
		return StringUtil.envVariableSubstitution("${APPL_HOME}/resources/HANA_tooltip_provider.xml");
	}
}

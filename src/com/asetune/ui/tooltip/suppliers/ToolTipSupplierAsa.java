package com.asetune.ui.tooltip.suppliers;

import java.awt.Window;

import com.asetune.ui.autocomplete.CompletionProviderAbstract;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.StringUtil;

public class ToolTipSupplierAsa
extends ToolTipSupplierAbstractSql
{
	public ToolTipSupplierAsa(Window owner, CompletionProviderAbstract compleationProvider, ConnectionProvider connectionProvider)
	{
		super(owner, compleationProvider, connectionProvider);
	}

	@Override
	public String getName() 
	{
		return "ASA";
	}

	@Override
	public String getToolTipProviderFile() 
	{
		return StringUtil.envVariableSubstitution("${APPL_HOME}/resources/ASA_tooltip_provider.xml");
	}
}

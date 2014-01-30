package com.asetune.ui.tooltip.suppliers;

import java.awt.Window;

import com.asetune.ui.autocomplete.CompletionProviderAbstract;
import com.asetune.utils.ConnectionProvider;


public abstract class ToolTipSupplierAbstractSql 
extends ToolTipSupplierAbstract
{
	public ToolTipSupplierAbstractSql(Window owner, CompletionProviderAbstract compleationProvider, ConnectionProvider connectionProvider)
	{
		super(owner, compleationProvider, connectionProvider);
	}

	@Override
	public String getAllowedChars()
	{
		return "_.*";
	}
}

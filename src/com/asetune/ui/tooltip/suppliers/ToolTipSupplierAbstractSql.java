package com.asetune.ui.tooltip.suppliers;

import java.awt.Window;

import com.asetune.utils.ConnectionProvider;


public abstract class ToolTipSupplierAbstractSql 
extends ToolTipSupplierAbstract
{
	public ToolTipSupplierAbstractSql(Window owner, ConnectionProvider connectionProvider)
	{
		super(owner, connectionProvider);
	}

	@Override
	public String getAllowedChars()
	{
		return "_.*";
	}
}

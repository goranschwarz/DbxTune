package com.asetune.ui.tooltip.suppliers;

import java.awt.Window;

import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.StringUtil;

public class ToolTipSupplierRepServer
extends ToolTipSupplierAbstract
{
	public ToolTipSupplierRepServer(Window owner, ConnectionProvider connectionProvider)
	{
		super(owner, connectionProvider);
	}

	@Override
	public String getName() 
	{
		return "RepServer";
	}

	@Override
	public String getToolTipProviderFile() 
	{
		return StringUtil.envVariableSubstitution("${APPL_HOME}/resources/RepServer_tooltip_provider.xml");
	}

	@Override
	public String getAllowedChars()
	{
		return "_.*";
	}
}

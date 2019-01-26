/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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

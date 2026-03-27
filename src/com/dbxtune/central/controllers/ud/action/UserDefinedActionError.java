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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.central.controllers.ud.action;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UserDefinedActionError
implements IUserDefinedAction
{

	private String _name            = "-unknown-";
	private String _errorDescrption = "-unknown-";
	private String _cofigFilename   = "-unknown-";

	public void setName(String name)                    { _name = name; }
	public void setErrorDescription(String message)     { _errorDescrption = message; }
	public void setConfigFilename(String cofigFilename) { _cofigFilename = cofigFilename; }

	
	@Override public String     getName()             { return _name; }
	@Override public String     getDescription()      { return _errorDescrption; }
	@Override public ActionType getActionType()       { return ActionType.ERROR; }
	@Override public String     getCommand()          { return "-unknown-"; }
	@Override public String     getOnServerName()     { return "-unknown-"; }
	@Override public String     getUrl()              { return "-unknown-"; }

	@Override public String     getConfigFilename()   { return _cofigFilename; }
	@Override public String     getLogFilename()      { return null; }
	@Override public boolean    isValid()             { return false; }

	@Override public List<String> getCssList()        { return Collections.emptyList(); }
	@Override public List<String> getJavaScriptList() { return Collections.emptyList(); }

	@Override public void checkUrlParameters(Map<String, String> map) throws Exception {}
	@Override public void setUrlParameters(Map<String, String> parameterMap) {}
	@Override public Map<String, String> getUrlParameters() { return Collections.emptyMap(); };

	@Override public String[]            getKnownParameters()      { return new String[] {}; }
	@Override public Map<String, String> getParameterDescription() { return Collections.emptyMap(); }

	@Override public List<String> getAuthorizedRoles() { return Collections.emptyList(); }
	@Override public List<String> getAuthorizedUsers() { return Collections.emptyList(); }

	@Override public void setPageRefreshTime(int time) {}
	@Override public int  getPageRefreshTime()         { return -1; }

	@Override public void   setExecutedByUser(String username) {}
	@Override public String getExecutedByUser()                { return "-unknown-"; }

	@Override public boolean isReasonMessageRequired()         { return false; }
	@Override public void    setExecutionReason(String reason) {}
	@Override public String  getExecutionReason()              { return null; }

	@Override
	public void createInfoContent(PrintWriter out) throws IOException
	{
	}

	@Override
	public void createContent(PrintWriter pageOut, PrintWriter mailOut) throws Exception
	{
	}

	@Override
	public void produce(PrintWriter pageOut) throws Exception
	{
	}

}

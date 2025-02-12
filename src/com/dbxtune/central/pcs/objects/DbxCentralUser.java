/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.central.pcs.objects;

import java.util.ArrayList;
import java.util.List;

import com.dbxtune.utils.StringUtil;

public class DbxCentralUser
{
	private String       _username = "";
	private String       _password = "";
	private List<String> _roles = new ArrayList<>();
	private String       _email = "";

	public DbxCentralUser()
	{
	}
	
	public DbxCentralUser(String username, String password, String email, String roleCsv)
	{
		_username = username;
		_password = password;
		_email    = password;
		_roles    = StringUtil.commaStrToList(roleCsv);
	}
	
	public String getUsername()
	{
		return _username;
	}

	public String getPassword()
	{
		return _password;
	}

	public String getEmail()
	{
		return _email;
	}

	public String[] getRoles()
	{
		return _roles.toArray(new String[_roles.size()]);
	}

}

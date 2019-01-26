package com.asetune.central.pcs.objects;

import java.util.ArrayList;
import java.util.List;

import com.asetune.utils.StringUtil;

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

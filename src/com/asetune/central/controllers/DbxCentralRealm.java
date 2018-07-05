package com.asetune.central.controllers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

public class DbxCentralRealm
extends AbstractLifeCycle implements LoginService
{
	private IdentityService _identityService = new DefaultIdentityService();
	private final Map<String, Object> users = new ConcurrentHashMap<>();


	// grabbed from: http://senchado.blogspot.se/2012/10/custom-realm-in-jetty.html
	public DbxCentralRealm() 
	{
    }

	@Override
	public String getName()
	{
		return this.getClass().getSimpleName();
	}

	@Override
	public UserIdentity login(String username, Object credentials)
	{
		UserIdentity user = null;

//		boolean validUser = false;
//		if (validUser)
//            user = _identityService.newUserIdentity(subject, principal, new String[] { "admin" });
		
		return user;
	}

	@Override
	public boolean validate(UserIdentity user)
	{
		if (users.containsKey(user.getUserPrincipal().getName()))
			return true;

		return false;
	}

	@Override
	public IdentityService getIdentityService()
	{
		return _identityService;
	}

	@Override
	public void setIdentityService(IdentityService service)
	{
		if (isRunning())
            throw new IllegalStateException("The server is already running");	

		_identityService = service;
	}

	@Override
	public void logout(UserIdentity user)
	{
		// TODO Auto-generated method stub
		
	}

}


// https://www.google.se/search?q=CustomRealm.java

// https://www.javacodegeeks.com/2012/05/apache-shiro-part-1-basics.html

// https://github.com/elastic/shield-custom-realm-example/blob/master/src/main/java/org/elasticsearch/example/realm/CustomRealm.java

// when starting...
//webAppContext.getSecurityHandler().setLoginService(new NuoRealm());

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
package com.dbxtune.central.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

// https://www.javatpoint.com/servlet-http-session-login-and-logout-example
public class LoginServlet extends HttpServlet
{
	private static final long	serialVersionUID = 1L;
	private static final Logger	_logger			 = Logger.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.setContentType("text/html;charset=UTF-8");
		PrintWriter out = resp.getWriter();

		// Check for known input parameters
		if (Helper.hasUnKnownParameters(req, resp, "username", "password"))
			return;

		String username = Helper.getParameter(req, "username");
		String password = Helper.getParameter(req, "password");

//		if ( Validate.checkUser(user, pass) )
		if ( "admin999".equals(username) && "admin999".equals(password) )
		{
			_logger.info("Login SUCCEEDED: username='"+username+"'.");

			RequestDispatcher rs = req.getRequestDispatcher("Welcome");
			rs.forward(req, resp);
			
			HttpSession session=req.getSession();
			session.setAttribute("username",username);  

		}
		else
		{
			_logger.info("Login failed: username='"+username+"', password='"+password+"'.");
			
			out.println("Username or Password incorrect");
			RequestDispatcher rs = req.getRequestDispatcher("index.html");
			rs.include(req, resp);
		}
	}
}

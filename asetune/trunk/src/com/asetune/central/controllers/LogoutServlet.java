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
package com.asetune.central.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

// https://www.javatpoint.com/servlet-http-session-login-and-logout-example
// https://github.com/ProfAndreaPollini/Java-Servlet-Login-Simple-Example/blob/master/src/java/com/ap/logindemo/LoginServlet.java
public class LogoutServlet extends HttpServlet
{
	private static final long	serialVersionUID = 1L;
	private static final Logger	_logger			 = Logger.getLogger(MethodHandles.lookup().lookupClass());

//	@Override
//	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
//	{
//		response.setContentType("text/html;charset=UTF-8");
//		PrintWriter out = response.getWriter();
//
//		request.getRequestDispatcher("index.html").include(request, response);
//		String username = request.getRemoteUser();
//        
//		HttpSession session = request.getSession();
//		if (session != null)
//		{
//			session.invalidate();
//			_logger.info("Logout: username='"+username+"'.");
//		}
//          
//		out.print("You are successfully logged out!");  
//		out.close();  
//	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();

		String username = request.getRemoteUser();
        
		HttpSession session = request.getSession();
		if (session != null)
		{
			session.invalidate();
			_logger.info("Logout: username='"+username+"'.");
		}

//		response.sendRedirect(request.getContextPath() + "/index.html");
		response.sendRedirect(request.getContextPath() + "/");
		
		out.print("You are successfully logged out!");  
		out.close();  
	}
}

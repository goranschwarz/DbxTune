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

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();

		request.getRequestDispatcher("link.html").include(request, response);  
        
		HttpSession session=request.getSession();  
		session.invalidate();  
          
		out.print("You are successfully logged out!");  
          
		out.close();  

//		_logger.info("Logout: username='"+username+"'.");
			
	}
}
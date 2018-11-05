package com.asetune.central.controllers;

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
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();

		String username = request.getParameter("username");
		String password = request.getParameter("password");

//		if ( Validate.checkUser(user, pass) )
		if ( "admin1".equals(username) && "admin1".equals(password) )
		{
			_logger.info("Login SUCCEEDED: username='"+username+"'.");

			RequestDispatcher rs = request.getRequestDispatcher("Welcome");
			rs.forward(request, response);
			
			HttpSession session=request.getSession();
			session.setAttribute("username",username);  

		}
		else
		{
			_logger.info("Login failed: username='"+username+"', password='"+password+"'.");
			
			out.println("Username or Password incorrect");
			RequestDispatcher rs = request.getRequestDispatcher("index.html");
			rs.include(request, response);
		}
	}
}

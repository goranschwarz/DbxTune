package com.asetune.central.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.velocity.runtime.directive.Foreach;

import com.asetune.utils.StringUtil;

// https://www.javatpoint.com/servlet-http-session-login-and-logout-example
// https://github.com/ProfAndreaPollini/Java-Servlet-Login-Simple-Example/blob/master/src/java/com/ap/logindemo/LoginServlet.java
public class LoginCheckServlet extends HttpServlet
{
	private static final long	serialVersionUID = 1L;
	private static final Logger	_logger			 = Logger.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();

		String username = request.getRemoteUser();
//		System.out.println("LoginCheckServlet: username='"+username+"'.");
        
//		HttpSession session = request.getSession();
//		if (session != null)
//		{
//			Enumeration<String> attr = session.getAttributeNames();
//			while (attr.hasMoreElements())
//			{
//				String name = attr.nextElement();
//				Object val = session.getAttribute(name);
//				
//				System.out.println("LoginCheckServlet: session.attr["+name+"]='"+val+"'.");
//			}
//
//			System.out.println("LoginCheckServlet: session.getId()="+session.getId());
////			System.out.println("LoginCheckServlet: session.getId()="+session.);
//		}

		if (StringUtil.hasValue(username))
			out.print("{\"isLoggedIn\":true,\"asUserName\":\""+username+"\"}");  
		else
			out.print("{\"isLoggedIn\":false,\"asUserName\":\"\"}");
		out.close();  
	}
}

package com.asetune.central.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.asetune.Version;
import com.asetune.gui.MainFrame;

public class DbxTuneGuiHttpConnectOfflineHandler 
extends AbstractHandler
{
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public void handle( String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response ) 
	throws IOException, ServletException
	{
//		System.out.println("target='"+target+"'");
//		System.out.println("baseRequest='"+baseRequest+"'");
//		System.out.println("request='"+request+"'");
//		System.out.println("response='"+response+"'");

		String paramUrl = request.getParameter("url");
//		System.out.println("paramUrl='"+paramUrl+"'");
		
		response.setContentType("text/html; charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);

		PrintWriter out = response.getWriter();

		out.println("<html>");
		out.println("<body>");
		
		if (MainFrame.hasInstance())
		{
			out.println("<h1>"+Version.getAppName()+" - 'connect-offline' Request Received</h1>");
			out.println("The application will ask if you really want to do this!");
			out.println("<br><br><br><br><br><br><br><br>");
			out.println("BuildStr: " + Version.getBuildStr());
		}
		else
		{
			out.println("<font color='red'>");
			out.println("<h1>"+Version.getAppName()+" - 'connect-offline' Request FAILED</h1>");
			out.println("No DbxTune Window was found in this context.");
			out.println("<br><br><br><br><br><br><br><br>");
			out.println("BuildStr: " + Version.getBuildStr());
			out.println("</font>");
		}

		out.println("</body>");
		out.println("</html>");

		baseRequest.setHandled(true);
		
		if (MainFrame.hasInstance())
		{
			MainFrame mf = MainFrame.getInstance();
			mf.doExternalOfflineConnectRequest(paramUrl);
		}
	}
}

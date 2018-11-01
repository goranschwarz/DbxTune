package com.asetune.central.controllers;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

//@WebServlet(urlPatterns="/toUpper")
public class DbxTuneLogTailWebSocketServlet
extends WebSocketServlet
{
	private static final long serialVersionUID = 1L;

//	@Override
//	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
//	{
//		// TODO Auto-generated method stub
//		super.doGet(req, resp);
//	}
	
	@Override
    public void configure(WebSocketServletFactory factory) 
    {
          factory.register(DbxTuneLogTailWebSocket.class);
    }
}

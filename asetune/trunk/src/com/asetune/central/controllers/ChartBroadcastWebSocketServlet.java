package com.asetune.central.controllers;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

//@WebServlet(urlPatterns="/toUpper")
public class ChartBroadcastWebSocketServlet
extends WebSocketServlet
{
	private static final long serialVersionUID = 1L;

	@Override
    public void configure(WebSocketServletFactory factory) 
    {
          factory.register(ChartBroadcastWebSocket.class);
    }
}

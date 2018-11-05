package com.asetune.central.controllers;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.asetune.central.pcs.CentralPcsWriterHandler;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

public class PcsQueueInfoController extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		ServletOutputStream out = resp.getOutputStream();
		resp.setContentType("text/html");
		resp.setCharacterEncoding("UTF-8");
//		resp.setContentType("application/json");
//		resp.setCharacterEncoding("UTF-8");
		
		int     pcsQueueSize           = -1;
//		boolean pcsIsBussy             = false;
//		String  pcsCurrentServerName   = null;
//		int     pcsCurrentExecTimeInMs = -1;
//		String  pcsLastServerName      = null;
//		int     pcsLastExecTimeInMs    = -1;
		if (CentralPcsWriterHandler.hasInstance())
		{
			pcsQueueSize = CentralPcsWriterHandler.getInstance().getQueueSize();
		}
		
		// Create JSON writer
		StringWriter sw = new StringWriter();
		JsonFactory jfactory = new JsonFactory();
		JsonGenerator w = jfactory.createGenerator(sw);
		//w.setPrettyPrinter(new DefaultPrettyPrinter());

		// to JSON
		w.writeStartObject();

		w.writeNumberField ("queueSize",           pcsQueueSize);
//		w.writeBooleanField("bussy",               pcsIsBussy);
//		w.writeStringField ("currentServerName",   pcsCurrentServerName);
//		w.writeNumberField ("currentExecTimeInMs", pcsCurrentExecTimeInMs);
//		w.writeStringField ("lastServerName",      pcsLastServerName);
//		w.writeNumberField ("lastExecTimeInMs",    pcsLastExecTimeInMs);

		w.writeEndObject(); // END: this CM
		w.close();

		// make it a String
		String payload = sw.toString();

		// write the JSON to the output
		out.println(payload);
		
		// flush/close
		out.flush();
		out.close();
	}

}

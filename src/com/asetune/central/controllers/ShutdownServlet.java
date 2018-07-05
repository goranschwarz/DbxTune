package com.asetune.central.controllers;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.asetune.central.pcs.CentralPersistWriterJdbc;
import com.asetune.utils.ShutdownHandler;

public class ShutdownServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	/** Log4j logging. */
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

	private String _shutdownToken = "secret";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String currentUsername = "-no-principal-";

		Principal principal = req.getUserPrincipal();
		if (principal != null)
			currentUsername = principal.getName();

		String from = "from getRemoteHost='" + req.getRemoteHost() + "', currentUsername='"+currentUsername+"', by user '" + req.getRemoteUser() + "'.";
		
		if (!hasCorrectSecurityToken(req))
        {
            _logger.warn("Unauthorized shutdown attempt "+from);
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
		
		boolean doRestart = req.getParameter("restart") != null;
		boolean doDefrag  = req.getParameter("defrag")  != null;

		String type = doRestart ? "restart" : "shutdown";
		
		ServletOutputStream out = resp.getOutputStream();
		resp.setContentType("text/html");
		resp.setCharacterEncoding("UTF-8");
//		resp.setContentType("application/json");
//		resp.setCharacterEncoding("UTF-8");
		out.println("Received "+type+" request..."+from);
		out.flush();
		out.close();

		_logger.info("Received shutdown request "+from);
		
		if (doDefrag)
		{
			// The file will be created, the shutdown will look for the file, delete it, and do shutdown defrag...
			_logger.info("Flag 'defrag' was passed, creating file '"+CentralPersistWriterJdbc.H2_SHUTDOWN_WITH_DEFRAG_FILENAME+"'.");
			FileUtils.write(new File(CentralPersistWriterJdbc.H2_SHUTDOWN_WITH_DEFRAG_FILENAME), "this will do: H2 SHUTDOWN DEFRAG");
		}

		String reason = (doRestart ? "Restart" : "Shutdown") + " Requested from WebServlet";
		ShutdownHandler.shutdown(reason, doRestart, null);
	}

	private boolean hasCorrectSecurityToken(HttpServletRequest request)
	{
		return _shutdownToken.equals(request.getParameter("password"));
	}
}

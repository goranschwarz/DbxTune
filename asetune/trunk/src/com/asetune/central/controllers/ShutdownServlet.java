package com.asetune.central.controllers;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.asetune.central.pcs.CentralPersistWriterJdbc.H2ShutdownType;
import com.asetune.utils.Configuration;
import com.asetune.utils.ShutdownHandler;
import com.asetune.utils.StringUtil;

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

		H2ShutdownType h2ShutdownType = H2ShutdownType.IMMEDIATELY;
		try { 
			h2ShutdownType = H2ShutdownType.valueOf( req.getParameter("h2ShutdownType") ); 
		} catch(RuntimeException ex) { 
			_logger.info("Shutdown type '"+req.getParameter("h2ShutdownType")+"' is unknown value, supported values: "+StringUtil.toCommaStr(H2ShutdownType.values())+". ");
		}
		if (H2ShutdownType.DEFRAG.equals(h2ShutdownType))
			doDefrag = true;

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
		
//		if (doDefrag)
//		{
//			// The file will be created, the shutdown will look for the file, delete it, and do shutdown defrag...
//			_logger.info("Flag 'defrag' was passed, creating file '"+CentralPersistWriterJdbc.H2_SHUTDOWN_WITH_DEFRAG_FILENAME+"'.");
//			FileUtils.write(new File(CentralPersistWriterJdbc.H2_SHUTDOWN_WITH_DEFRAG_FILENAME), "from: ShutdownServlet:"+h2ShutdownType, StandardCharsets.UTF_8);
//		}

		Configuration shutdownConfig = new Configuration();
		shutdownConfig.setProperty("h2.shutdown.type", h2ShutdownType.toString());  // IMMEDIATELY, COMPACT, DEFRAG

		String reason = (doRestart ? "Restart" : "Shutdown") + " Requested from WebServlet";
		ShutdownHandler.shutdown(reason, doRestart, shutdownConfig);
	}

	private boolean hasCorrectSecurityToken(HttpServletRequest request)
	{
		return _shutdownToken.equals(request.getParameter("password"));
	}
}

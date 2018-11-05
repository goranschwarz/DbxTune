package com.asetune.central.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.security.Principal;
import java.util.Collections;
import java.util.LinkedList;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

//@WebServlet("/download-xxx")
public class DownloadServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	/** Log4j logging. */
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String currentUsername = "-no-principal-";

		Principal principal = req.getUserPrincipal();
		if (principal != null)
			currentUsername = principal.getName();

		String from = "from getRemoteHost='" + req.getRemoteHost() + "', currentUsername='"+currentUsername+"', by user '" + req.getRemoteUser() + "'.";
		
//		if (!hasCorrectSecurityToken(req))
//        {
//            _logger.warn("Unauthorized download attempt "+from);
//            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
//            return;
//        }
		
//		boolean doRestart = req.getParameter("restart") != null;
//
//		String type = doRestart ? "restart" : "shutdown";
//		
//		ServletOutputStream out = resp.getOutputStream();
//		resp.setContentType("text/html");
//		resp.setCharacterEncoding("UTF-8");
////		resp.setContentType("application/json");
////		resp.setCharacterEncoding("UTF-8");
//		out.println("Received "+type+" request..."+from);
//		out.flush();
//		out.close();
//
//		_logger.info("Received shutdown request "+from);
//
//		String reason = (doRestart ? "Restart" : "Shutdown") + " Requested from WebServlet";
//		ShutdownHandler.shutdown(reason, doRestart);

		File downloadFile = null;

		String fileName = req.getParameter("fileName");
		if(StringUtil.isNullOrBlank(fileName))
		{
			LinkedList<File> dbxTuneFileList = new LinkedList<>();

			String DBXTUNE_LATEST_ZIP = Configuration.getCombinedConfiguration().getProperty("DBXTUNE_LATEST_ZIP");
			String DBXTUNE_HOME       = Configuration.getCombinedConfiguration().getProperty("DBXTUNE_HOME");

			if (StringUtil.hasValue(DBXTUNE_LATEST_ZIP))
			{
				downloadFile = new File(DBXTUNE_LATEST_ZIP);
				
				// FIXME: resolv link names into real names
			}
			else if (StringUtil.hasValue(DBXTUNE_HOME))
			{
				// List any files: asetune_YYYY-MM-DD.zip or dbxtune_YYYY-MM-DD.zip
				File dir = new File(DBXTUNE_HOME);
				if (dir.exists())
				{
					File[] files = dir.listFiles();
					for (int i=0; i<files.length; i++)
					{
						File f = files[i];
						if (f.isFile())
						{
							if ( f.getName().toLowerCase().matches("(ase|dbx)tune_[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]\\.zip") )
							{
								dbxTuneFileList.add(f);
							}
						}
					}
				}
				// Go up one level in the directory structure.
				dir = new File(DBXTUNE_HOME + File.separator + "..");
				if (dir.exists())
				{
					File[] files = dir.listFiles();
					for (int i=0; i<files.length; i++)
					{
						File f = files[i];
						if (f.isFile())
						{
							if ( f.getName().toLowerCase().matches("(ase|dbx)tune_[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]\\.zip") )
							{
								dbxTuneFileList.add(f);
							}
						}
					}
				}

				if ( dbxTuneFileList.isEmpty() )
				{
					throw new ServletException("File Name can't be null or empty");
				}
				
				Collections.sort(dbxTuneFileList);
				downloadFile = dbxTuneFileList.getLast();

				_logger.info("Found the following DbxTune ZIP files " + dbxTuneFileList + ". Choosing '"+downloadFile+"'.");
				
			} // end: DBXTUNE_HOME
			else
			{
				throw new ServletException("File Name can't be null or empty");
			}
		}
		else
		{
			downloadFile = new File(req.getServletContext().getAttribute("FILES_DIR") + File.separator + fileName);
		}

		if ( ! downloadFile.exists() )
		{
			throw new ServletException("File '+"+downloadFile+"+' doesn't exists on server.");
		}
		_logger.info("Download-Start: File location on server '"+downloadFile.getAbsolutePath()+"'. "+from);
		ServletContext ctx = getServletContext();
		InputStream fis = new FileInputStream(downloadFile);
		String mimeType = ctx.getMimeType(downloadFile.getAbsolutePath());
		resp.setContentType(mimeType != null? mimeType:"application/octet-stream");
		resp.setContentLength((int) downloadFile.length());
		resp.setHeader("Content-Disposition", "attachment; filename=\"" + downloadFile.getName() + "\"");
		
		ServletOutputStream os = resp.getOutputStream();
		byte[] bufferData = new byte[4096];
		int read=0;
		while((read = fis.read(bufferData))!= -1){
			os.write(bufferData, 0, read);
		}
		os.flush();
		os.close();
		fis.close();
		
		_logger.info("Download-End: File '"+downloadFile.getName()+"' downloaded to client was successfully. "+from);
	}

/*------ code was "grabbed" from:
 * https://www.journaldev.com/1964/servlet-upload-file-download-example
 */

//	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//		if(!ServletFileUpload.isMultipartContent(request)){
//			throw new ServletException("Content type is not multipart/form-data");
//		}
//		
//		response.setContentType("text/html");
//		PrintWriter out = response.getWriter();
//		out.write("<html><head></head><body>");
//		try {
//			List<FileItem> fileItemsList = uploader.parseRequest(request);
//			Iterator<FileItem> fileItemsIterator = fileItemsList.iterator();
//			while(fileItemsIterator.hasNext()){
//				FileItem fileItem = fileItemsIterator.next();
//				System.out.println("FieldName="+fileItem.getFieldName());
//				System.out.println("FileName="+fileItem.getName());
//				System.out.println("ContentType="+fileItem.getContentType());
//				System.out.println("Size in bytes="+fileItem.getSize());
//				
//				File file = new File(request.getServletContext().getAttribute("FILES_DIR")+File.separator+fileItem.getName());
//				System.out.println("Absolute Path at server="+file.getAbsolutePath());
//				fileItem.write(file);
//				out.write("File "+fileItem.getName()+ " uploaded successfully.");
//				out.write("<br>");
//				out.write("<a href=\"UploadDownloadFileServlet?fileName="+fileItem.getName()+"\">Download "+fileItem.getName()+"</a>");
//			}
//		} catch (FileUploadException e) {
//			out.write("Exception in uploading file.");
//		} catch (Exception e) {
//			out.write("Exception in uploading file.");
//		}
//		out.write("</body></html>");
//	}

}

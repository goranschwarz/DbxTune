/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.central.controllers;

import java.io.PrintWriter;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.dbxtune.central.controllers.HtmlStatic.PageSection;

public class LoadPageProgress
extends DbxCentralPageTemplate
{
	private static final long serialVersionUID = 1L;

	@Override
	public Set<UrlParameterDescription> createUrlParameterDescription()
	{
		// Allow any Parameters, since we use the 'request.getPathInfo()' and 'request.getPathInfo()'  
		return null;
		//return Collections.emptySet();
	}

	@Override
	protected void checkUrlParameters() throws Exception
	{
	}

	@Override
	public String getHeadTitle()
	{
		return "Loading Slow Page";
	}

	@Override
	public PageSection getPageSection()
	{
		return PageSection.None;
	}

	@Override
	public void createHtmlBodyContent(PrintWriter writer)
	{
		writer.println();
		writer.println("	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ");
		writer.println("	- - Modal Dialog: dbx-loading-url-dialog ");
		writer.println("	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - --> ");
		writer.println("	<div class='modal fade' id='dbx-loading-url-dialog' role='dialog' aria-labelledby='dbx-loading-url-dialog' aria-hidden='true'> ");
		writer.println("		<div class='modal-dialog modal-dialog-centered modal-xl' role='document'> ");
		writer.println("			<div class='modal-content'> ");
		writer.println("				<div class='modal-header'> ");
		writer.println("					<h5 class='modal-title' id='dbx-loading-url-dialog-title'><b><span id='dbx-loading-url-label'></span></b> <span id='dbx-loading-url-objectName'></span></h5> ");
		writer.println("					<button type='button' class='close' data-dismiss='modal' aria-label='Close'> ");
		writer.println("						<span aria-hidden='true'>&times;</span> ");
		writer.println("					</button> ");
		writer.println("				</div> ");
		writer.println("				<div class='modal-body' style='overflow-x: auto;'> ");
		writer.println("					<div id='dbx-loading-url-progress' class='progress mb-3'> ");
		writer.println("						<div id='dbx-loading-url-progress-bar' class='progress-bar progress-bar-striped progress-bar-animated' role='progressbar' style='width: 0%;'></div> ");
		writer.println("					</div> ");
		writer.println("					<div id='dbx-loading-url-text' class='text-center'>Loading...</div> ");
		writer.println("<!--					<div id='dbx-loading-url-content' class='mt-3' style='display: none;'></div>--> ");
		writer.println("					<div id='dbx-loading-url-content' style='display: none;'></div> ");
		writer.println("				</div> ");
		writer.println("				<div class='modal-footer'> ");
		writer.println("					<button type='button' class='btn btn-secondary' data-dismiss='modal'>Close</button> ");
		writer.println("				</div> ");
		writer.println("			</div> ");
		writer.println("		</div> ");
		writer.println("	</div> ");
		writer.println();
		
	}
	
	@Override
	public void createHtmlBodyJavaScriptBottom(PrintWriter writer)
	{
		HttpServletRequest request = getRequest();
		String pathInfo = request.getPathInfo();
		String queryString = request.getQueryString();
		String targetUrl = pathInfo + (queryString != null ? "?" + queryString : "");
		
		writer.println();
		writer.println("	<script> ");
		writer.println();
		writer.println("		// When page is loaded, call function... ");
		writer.println("		document.addEventListener('DOMContentLoaded', loadUrlWithProgressBarIntoCurrentTab('" + targetUrl + "', 'Loading Page...')); ");
		writer.println();
		writer.println("		// Function ");
		writer.println("		function loadUrlWithProgressBarIntoCurrentTab(url, title) ");
		writer.println("		{ ");
		writer.println("			console.log('loadUrlWithProgressBarIntoCurrentTab(): url=| + url + |, title=|' + title + '|.'); ");
		writer.println();
		writer.println("			const modalElement  = new bootstrap.Modal(document.getElementById('dbx-loading-url-dialog')); ");
		writer.println("			const progressBar   = document.getElementById('dbx-loading-url-progress-bar'); ");
		writer.println("			const loadingText   = document.getElementById('dbx-loading-url-text'); ");
		writer.println("			const resultContent = document.getElementById('dbx-loading-url-content'); ");
		writer.println("			const urlObjectName = document.getElementById('dbx-loading-url-objectName'); ");
		writer.println();
		writer.println("			// Show the modal ");
		writer.println("			modalElement.show(); ");
		writer.println();
		writer.println("			// Reset UI elements ");
		writer.println("			progressBar.style.width = '0%'; ");
		writer.println("			loadingText.style.display = 'block'; ");
		writer.println("//			loadingText.textContent = 'Loading...'; ");
		writer.println("			loadingText.innerHTML = '<b>Loading URL...</b><br><br>' + url; ");
		writer.println("//			resultContent.innerHTML = ''; ");
		writer.println("//			resultContent.style.display = 'none'; ");
		writer.println();
		writer.println("			urlObjectName.innerHTML = title; ");
		writer.println();
		writer.println("			// Move the progress bar every second ");
		writer.println("			let progress = 0; ");
		writer.println("			const interval = setInterval(() => { ");
		writer.println("				if (progress >= 90) { ");
		writer.println("					clearInterval(interval); ");
		writer.println("				} else { ");
		writer.println("					progress += 10; ");
		writer.println("					progressBar.style.width = progress + '%'; ");
		writer.println("				} ");
		writer.println("			}, 1000); ");
		writer.println();
		writer.println("			// Call URL, Fetch the HTML content, open HTML in new TAB ");
		writer.println("			fetch(url) ");
		writer.println("				.then(response => { ");
		writer.println("					if (!response.ok) { ");
		writer.println("						return response.text().then(errorText => { ");
		writer.println("							throw new Error('<b>Failed to fetch data</b> <br>'  ");
		writer.println("								+ '<br>' ");
		writer.println("								+ '<b>Error Code</b> <br>'  ");
		writer.println("								+ response.status + ' (' + response.statusText + ') <br>' ");
		writer.println("								+ '<br>' ");
		writer.println("								+ '<b>Called URL</b> <br>'  ");
		writer.println("								+ response.url + '<br>' ");
		writer.println("								+ '<br>' ");
		writer.println("								+ '<b>Full Error Text from Server</b> <br>'  ");
		writer.println("								+ '<hr>' ");
		writer.println("								+ errorText + '<br>' ");
		writer.println("								); ");
		writer.println("						}); ");
		writer.println("					} ");
		writer.println("					return response.text(); ");
		writer.println("				}) ");
		writer.println("				.then(html => { ");
		writer.println("					// Stop the progress bar ");
		writer.println("					clearInterval(interval); ");
		writer.println("					progressBar.style.width = '100%'; ");
		writer.println();
		writer.println("					// Close the modal and open the content in a new tab ");
		writer.println("					setTimeout(() => { ");
		writer.println("						modalElement.hide(); ");
		writer.println();
		writer.println("						// Replace current content with the fetched one, including loading everything... ");
		writer.println("						document.open(); ");
		writer.println("						document.write(html); ");
		writer.println("						document.close(); ");
		writer.println();
//		writer.println("						// Replace current content with the fetched one ");
//		writer.println("						document.body.innerHTML = html; ");
//		writer.println();
//		writer.println("						// Open a new tab with the correct URL in the address bar ");
//		writer.println("						const newTab = window.open(url, '_blank'); ");
//		writer.println();
//		writer.println("						// Replace the tab's content with the fetched HTML ");
//		writer.println("						newTab.document.open(); ");
//		writer.println("						newTab.document.write(html); ");
//		writer.println("						newTab.document.close(); ");
//		writer.println();
//		writer.println("						// Set the URL string in the navigation field ");
//		writer.println("						newTab.history.pushState('object or string', 'Title', url); ");
		writer.println("					}, 500); ");
		writer.println("				}) ");
		writer.println("				.catch(error => { ");
		writer.println("					// Handle error ");
		writer.println("					clearInterval(interval); ");
		writer.println("					progressBar.style.width = '0%'; ");
		writer.println("					loadingText.innerHTML = `<span class='text-danger'>${error.message}</span>`; ");
		writer.println("				}); ");
		writer.println("		} ");
		writer.println("	</script> ");
		
	}

	
	
	
}
////@WebServlet("/lpp/*")
//public class LoadPageProgress
//extends HttpServlet
//{
//	private static final long serialVersionUID = 1L;
//
//	@Override
//	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
//	throws ServletException, IOException 
//	{
//		response.setContentType("text/html");
//		PrintWriter out = response.getWriter();
//	        
//		// Get everything after /lpp/
//		String pathInfo = request.getPathInfo();
//		String queryString = request.getQueryString();
//		String targetUrl = pathInfo + (queryString != null ? "?" + queryString : "");
//	        
//		out.println("<!DOCTYPE html> ");
//		out.println("<html><head> ");
//		out.println("<title>Loading Slow Page</title> ");
//		out.println("<style> ");
//		out.println("body { font-family: Arial; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background-color: #f0f0f0; } ");
//		out.println("#loader { text-align: center; } ");
//		out.println("#progress-bar { width: 300px; height: 20px; background-color: #e0e0e0; border-radius: 10px; overflow: hidden; margin: 20px auto; } ");
//		out.println("#progress { width: 0%; height: 100%; background-color: #4CAF50; transition: width 0.5s ease; } ");
//		out.println("#pageUrl { margin-top: 20px; font-size: 12px; color: #666; word-break: break-all; } ");
//		out.println("</style> ");
//		out.println("</head> ");
//		out.println("<body> ");
//		out.println();
//		
//		out.println("<div id='loader'> ");
//		out.println("    <h2>Loading Target Page...</h2> ");
//		out.println("    <div id='progress-bar'> ");
//		out.println("        <div id='progress'></div> ");
//		out.println("    </div> ");
//		out.println("    <p id='status'>Loading page...</p> ");
//		out.println("    <div id='pageUrl'></div> ");
//		out.println("</div> ");
//		out.println();
//		
//		out.println("<script> ");
//		out.println("const targetUrl = '" + targetUrl.replace("'", "\\'") + "'; ");
//		out.println("document.getElementById('pageUrl').textContent = `Target URL: ${targetUrl}`; ");
//		out.println();
//
//		out.println("function simulateLoading() { ");
//		out.println("    return new Promise(resolve => { ");
//		out.println("        let progress = 0; ");
//		out.println("        const interval = setInterval(() => { ");
//		out.println("            progress += 5; ");
//		out.println("            document.getElementById('progress').style.width = progress + '%'; ");
//		out.println("            if (progress >= 100) { ");
//		out.println("                clearInterval(interval); ");
//		out.println("                resolve(); ");
//		out.println("            } ");
//		out.println("        }, 500); ");
//		out.println("    }); ");
//		out.println("} ");
//		out.println("async function startLoading() { ");
//		out.println("    await simulateLoading(); ");
//		out.println("    window.location.href = targetUrl; ");
//		out.println("} ");
//		out.println("startLoading(); ");
//		out.println("</script> ");
//		out.println();
//		
//		out.println("</body> ");
//		out.println("</html> ");
//	}
//}

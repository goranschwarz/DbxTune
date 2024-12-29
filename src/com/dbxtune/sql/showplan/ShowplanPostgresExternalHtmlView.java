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
package com.dbxtune.sql.showplan;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import com.dbxtune.Version;

public class ShowplanPostgresExternalHtmlView
extends ShowplanHtmlView
{
	@Override
	protected String getXsltFile()
	{
		return null;
	}

	@Override
	protected String getTemplateJarDir()
	{
		return "postgres/";
	}

	
	/**
	 * Create a HTML file using the XML plan
	 * 
	 * @param xmlPlan
	 * @return The file name created
	 * @throws IOException 
	 * @throws TransformerException 
	 * @throws TransformerConfigurationException 
	 * @throws Exception
	 */
	@Override
	protected File createHtmlFile(String planText) 
	throws IOException, TransformerConfigurationException, TransformerException 
	{
		File destDir = new File(getTmpShowplanPath() + getTemplateJarDir());
//		String xsltFile = getTmpShowplanPath() + getTemplateJarDir() + getXsltFile();

		File outputHTML = File.createTempFile("pg_showplan_", ".html", destDir);
		outputHTML.deleteOnExit();

		// Write HTML file
		BufferedWriter out = null;
		try
		{
			FileWriter fstream = new FileWriter(outputHTML);
			out = new BufferedWriter(fstream);
			out.write("<html>                                                                    \n");
			out.write("                                                                          \n");
			out.write("<head>                                                                    \n");
			out.write("    <META http-equiv='Content-Type' content='text/html; charset=UTF-8'>   \n");
			out.write("    <title>Postgres Execution plan</title>                                \n");

			out.write("    <script src='https://unpkg.com/vue@3.2.45/dist/vue.global.prod.js'></script>                   \n");
			out.write("    <script src='https://unpkg.com/pev2/dist/pev2.umd.js'></script>                                \n");
			out.write("    <link rel='stylesheet' href='https://unpkg.com/bootstrap@5.3.2/dist/css/bootstrap.min.css' />  \n");
			out.write("    <link rel='stylesheet' href='https://unpkg.com/pev2/dist/style.css' />                         \n");
			
//			out.write("    <script src='lib/qp.js' type='text/javascript'></script>              \n");
//			out.write("    <script src='dist/qp.js' type='text/javascript'></script>             \n");
//			out.write("    <script src='https://code.jquery.com/jquery-3.7.1.min.js' integrity='sha256-/JqT3SQfawRcv/BIHPThkBvs0OEvtFFmqPF/lYI/Cxo=' crossorigin='anonymous'></script>  \n");

			out.write("    <style>                                                                \n");
			out.write("        .grow-to-bottom {                                                  \n");
			out.write("            position: absolute; /* Positioned relative to the nearest positioned ancestor */ \n");
			out.write("            top: 160;           /* Start at the top of its container */     \n");
			out.write("            bottom: 0;          /* Stretch to the bottom */                 \n");
			out.write("            left: 0;            /* Stretch to the full width if needed */   \n");
			out.write("            right: 0;           /* Stretch to the full width if needed */   \n");
			out.write("            overflow: auto;     /* Add scrolling if content overflows */    \n");
			out.write("    }                                                                      \n");
			out.write("    </style>                                                               \n");
			
			
			// NOTE: If you ADD more text, please adjust class 'grow-to-bottom' >> top: 160; << to make room for more rows
			//       There is PROBABLY a better way to do this, but my knowledge in HTML/CSS etc is LIMITED 
			out.write("</head>                                                                   \n");
			out.write("                                                                          \n");
			out.write("<body>                                                                    \n");
			out.write("<h2>" + Version.getAppName() + " - Postgres Execution Plan Viewer</h2>    \n");
			out.write("The query plan is embedded in the HTML Source text...<br>                 \n");
			out.write("Feel free to copy the plan text and past it into some other tool...<br>   \n");
			out.write("For example the online tool: <a target='_blank' href='https://explain.dalibo.com'>https://explain.dalibo.com</a><br> \n");
			out.write("<button onclick='copyShowplanToClipboard()'>Copy Showplan</button>        \n");
//			out.write("<button onclick='downloadShowplan()'>Download Showplan</button>           \n");
			out.write("<br>                                                                      \n");
//			out.write("<button onclick='copyShowplanFormattedToClipboard()'>Copy Showplan (formatted)</button> \n");
//			out.write("<button onclick='downloadShowplanFormatted()'>Download Showplan (formatted)</button> \n");
			out.write("<div id='copy-done'></div>                                                \n");
			out.write("<br>                                                                      \n");
			out.write("<hr>                                                                      \n");
			out.write("<br>                                                                      \n");

//			out.write("<div id='container'></div>                                                \n");
			out.write("<div id='app'>                                                            \n");
			out.write("    <pev2 class='grow-to-bottom' :plan-source='plan' plan-query='' />     \n");
			out.write("</div>                                                                    \n");

			// write the below to the file, note the backtick (`) on the "var showplan = ``" which is EcmaScript6 syntax to escape newlines, single/double quotes etc... 
			// <script>
			//     var showplanText = `<ShowPlanXML...`;
			//     QP.showPlan(document.getElementById("container"), showplanText);
			// </script> 

			out.write("<script>                                                                  \n");
			out.write("  var showplanText = `"); // note the backtick char
			out.write(planText);
			out.write("`;\n");                     // note the backtick char
			out.write("\n");
			out.write("  const { createApp } = Vue;                                            \n");
			out.write("                                                                        \n");
			out.write("  const app = createApp({                                               \n");
			out.write("    data() {                                                            \n");
			out.write("      return {                                                          \n");
			out.write("        plan: showplanText,                                             \n");
			out.write("      }                                                                 \n");
			out.write("    },                                                                  \n");
			out.write("  });                                                                   \n");
			out.write("  app.component('pev2', pev2.Plan);                                     \n");
			out.write("  app.mount('#app');                                                    \n");
			out.write("</script>                                                               \n");


			out.write("\n");
			out.write("<script>                                                                  \n");

			out.write("function copyShowplanToClipboard()                                        \n");
			out.write("{                                                                         \n");
			out.write("    var copyFeedback = document.getElementById('copy-done');              \n");
			out.write("                                                                          \n");
			out.write("    // Set feedback                                                       \n");
			out.write("    copyFeedback.innerHTML     = 'The Query Plan was copied to Clipboard';\n");
			out.write("    copyFeedback.style.display = 'block';                                 \n");
			out.write("                                                                          \n");
			out.write("    // Copy to clipboard                                                  \n");
			out.write("    copyStringToClipboard(showplanText);                                  \n");
			out.write("                                                                          \n");
			out.write("    // Hide feedback                                                      \n");
			out.write("    setTimeout( function(){                                               \n");
			out.write("        copyFeedback.style.display = 'none';                              \n");
			out.write("    }, 3000);                                                             \n");
			out.write("}                                                                         \n");
			out.write("                                                                          \n");

			out.write("\n");
			out.write("function copyStringToClipboard (string) {                                 \n");
			out.write("    function handler (event)                                              \n");
			out.write("    {                                                                     \n");
			out.write("        event.clipboardData.setData('text/plain', string);                \n");
			out.write("        event.preventDefault();                                           \n");
			out.write("        document.removeEventListener('copy', handler, true);              \n");
			out.write("    }                                                                     \n");
			out.write("                                                                          \n");
			out.write("    document.addEventListener('copy', handler, true);                     \n");
			out.write("    document.execCommand('copy');                                         \n");
			out.write("}                                                                         \n");

			out.write("</script>                                                                 \n");

			out.write("                                                                          \n");
			out.write("</body>                                                                   \n");
			out.write("                                                                          \n");
			out.write("</html>                                                                   \n");
		}
		catch (IOException e)
		{
			throw e;
		}
		finally
		{
			if(out != null) 
				out.close();
		}

		return outputHTML;
	}
}

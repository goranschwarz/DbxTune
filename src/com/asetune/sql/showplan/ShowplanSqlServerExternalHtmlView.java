/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.sql.showplan;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import com.asetune.Version;

public class ShowplanSqlServerExternalHtmlView
extends ShowplanHtmlView
{
	@Override
	protected String getXsltFile()
	{
		return "src/qp.xslt";
	}

	@Override
	protected String getTemplateJarDir()
	{
		return "sqlserver/";
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
	protected File createHtmlFile(String xmlPlan) 
	throws IOException, TransformerConfigurationException, TransformerException 
	{
		File destDir = new File(getTmpShowplanPath() + getTemplateJarDir());
		String xsltFile = getTmpShowplanPath() + getTemplateJarDir() + getXsltFile();

		File outputHTML = File.createTempFile("showplan_", ".html", destDir);
		outputHTML.deleteOnExit();

		// TODO: This can be done better, maybe: 
		//       - transform returns a string instead of the File (so we don't have to read the file and then write to it at once)
		//       - or: write the SQL-Server showplan into a javascript variable, and then:
		//         <div id="container"></div>
		//         <script>
		//             QP.showPlan(document.getElementById("container"), '<ShowPlanXML...');
		//         </script> 
		//         like the authur suggest on: https://github.com/JustinPealing/html-query-plan
//		transform(xmlPlan, xsltFile, outputHTML);
		
//		String fileContent = FileUtils.readFile(outputHTML, null);
		
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
			out.write("    <title>Execution plan</title>                                         \n");
			out.write("    <link rel='stylesheet' type='text/css' href='css/qp.css'>             \n");
//			out.write("    <script src='lib/qp.js' type='text/javascript'></script>              \n");
			out.write("    <script src='dist/qp.js' type='text/javascript'></script>             \n");
			out.write("    <script src='https://code.jquery.com/jquery-3.7.1.min.js' integrity='sha256-/JqT3SQfawRcv/BIHPThkBvs0OEvtFFmqPF/lYI/Cxo=' crossorigin='anonymous'></script>  \n");
			out.write("</head>                                                                   \n");
			out.write("                                                                          \n");
			out.write("<body>                                                                    \n");
			out.write("<h2>" + Version.getAppName() + " - Simple Showplan viewer</h2>            \n");
			out.write("The query plan is embedded in the HTML Source text...<br>                 \n");
			out.write("Feel free to copy the showplan xml and past it into some other tool...<br> \n");
			out.write("For example the online tool: <a target='_blank' href='http://www.supratimas.com/'>http://www.supratimas.com/</a><br> \n");
			out.write("<button onclick='copyShowplanToClipboard()'>Copy Showplan</button>        \n");
			out.write("<button onclick='downloadShowplan()'>Download Showplan</button>           \n");
			out.write("<br>                                                                      \n");
			out.write("<button onclick='copyShowplanFormattedToClipboard()'>Copy Showplan (formatted)</button> \n");
			out.write("<button onclick='downloadShowplanFormatted()'>Download Showplan (formatted)</button> \n");
			out.write("<div id='copy-done'></div>                                                \n");
			out.write("<br>                                                                      \n");
			out.write("<hr>                                                                      \n");
			out.write("<br>                                                                      \n");

			out.write("<div id='container'></div>                                                \n");

			// write the below to the file, note the backtick (`) on the "var showplan = ``" which is EcmaScript6 syntax to escape newlines, single/double quotes etc... 
			// <script>
			//     var showplanText = `<ShowPlanXML...`;
			//     QP.showPlan(document.getElementById("container"), showplanText);
			// </script> 

			out.write("<script>                                                                  \n");
			out.write("    var showplanText = `"); // note the backtick char
			out.write(xmlPlan);
			out.write("`;\n");                     // note the backtick char
			out.write("    QP.showPlan(document.getElementById('container'), showplanText);      \n");

//			out.write("                                                                          \n");
//			out.write("function copyShowplanToClipboard()                                        \n");
//			out.write("{                                                                         \n");
//			out.write("    window.prompt('Copy to clipboard: Ctrl+C, Enter', showplanText);      \n");
//			out.write("}                                                                         \n");
			
			out.write("                                                                          \n");
			out.write("function formatXml(xml) {                                                 \n");
			out.write("    var formatted = '';                                                   \n");
			out.write("    var reg = /(>)(<)(\\/*)/g;                                            \n");
			out.write("    xml = xml.replace(reg, '$1\\r\\n$2$3');                               \n");
			out.write("    var pad = 0;                                                          \n");
			out.write("    jQuery.each(xml.split('\\r\\n'), function(index, node) {              \n");
			out.write("        var indent = 0;                                                   \n");
			out.write("        if (node.match( /.+<\\/\\w[^>]*>$/ )) {                           \n");
			out.write("            indent = 0;                                                   \n");
			out.write("        } else if (node.match( /^<\\/\\w/ )) {                            \n");
			out.write("            if (pad != 0) {                                               \n");
			out.write("                pad -= 1;                                                 \n");
			out.write("            }                                                             \n");
			out.write("        } else if (node.match( /^<\\w[^>]*[^\\/]>.*$/ )) {                \n");
			out.write("            indent = 1;                                                   \n");
			out.write("        } else {                                                          \n");
			out.write("            indent = 0;                                                   \n");
			out.write("        }                                                                 \n");
			out.write("                                                                          \n");
			out.write("        var padding = '';                                                 \n");
			out.write("        for (var i = 0; i < pad; i++) {                                   \n");
			out.write("            padding += '  ';                                              \n");
			out.write("        }                                                                 \n");
			out.write("                                                                          \n");
			out.write("        formatted += padding + node + '\\r\\n';                           \n");
			out.write("        pad += indent;                                                    \n");
			out.write("    });                                                                   \n");
			out.write("                                                                          \n");
			out.write("    return formatted;                                                     \n");
			out.write("}                                                                         \n");
			out.write("                                                                          \n");

			// get Query/Plan Hash from XML:   QueryHash="0xE0245C01AFBDBF7E" QueryPlanHash="0xE984CF727B712DF9"
			out.write("                                                                          \n");
			out.write("function getFileNameFromXmlPlan(xml)                                      \n");
			out.write("{                                                                         \n");
			out.write("    let filename = 'unknown';                                             \n");
			out.write("    try                                                                   \n");
			out.write("    {                                                                     \n");
			out.write("        let startPos      = -1;                                           \n");
			out.write("        let endPos        = -1;                                           \n");
			out.write("        let queryHash     = '';                                           \n");
			out.write("        let queryPlanHash = '';                                           \n");
			out.write("                                                                          \n");
			out.write("        startPos = xml.indexOf('QueryHash=\"0x');                         \n");
			out.write("        endPos   = xml.indexOf(' ', startPos);                            \n");
			out.write("        if (startPos != -1 && endPos != -1)                               \n");
			out.write("        {                                                                 \n");
			out.write("            queryHash = xml.substring(startPos, endPos).replaceAll('\"', '').replace('QueryHash=', '');  \n");
			out.write("        }                                                                 \n");
			out.write("                                                                          \n");
			out.write("        startPos = xml.indexOf('QueryPlanHash=\"0x');                     \n");
			out.write("        endPos   = xml.indexOf(' ', startPos);                            \n");
			out.write("        if (startPos != -1 && endPos != -1)                               \n");
			out.write("        {                                                                 \n");
			out.write("            queryPlanHash = xml.substring(startPos, endPos).replaceAll('\"', '').replace('QueryPlanHash=', '');  \n");
			out.write("        }                                                                 \n");
			out.write("                                                                          \n");
			out.write("        if (queryHash.length > 0 && queryPlanHash.length > 0)             \n");
			out.write("        {                                                                 \n");
			out.write("            filename = 'QueryHash=' + queryHash + '__QueryPlanHash=' + queryPlanHash; \n");
			out.write("        }                                                                 \n");
			out.write("                                                                          \n");
//			out.write("        let xmlDoc   = $.parseXML(xml);                                   \n");
//			out.write("                                                                          \n");
//			out.write("        let queryHash        = '';                                        \n");
//			out.write("        let queryHashArr     = [];                                        \n");
//			out.write("        let queryPlanHash    = '';                                        \n");
//			out.write("        let queryPlanHashArr = [];                                        \n");
//			out.write("        $(xmlDoc).find('StmtSimple').each(function(i,e) {                 \n");
//			out.write("            queryHashArr    .push($(e).attr('QueryHash'));                \n");
//			out.write("            queryPlanHashArr.push($(e).attr('QueryPlanHash'));            \n");
//			out.write("        });                                                               \n");
//			out.write("                                                                          \n");
//			out.write("        if (queryPlanHashArr.length > 0)                                  \n");
//			out.write("        {                                                                 \n");
//			out.write("            queryPlanHash = queryPlanHashArr[0];                          \n");
//			out.write("                                                                          \n");
//			out.write("            if (queryHashArr.length > 0)                                  \n");
//			out.write("            {                                                             \n");
//			out.write("                queryHash = queryHashArr[0];                              \n");
//			out.write("            }                                                             \n");
//			out.write("        }                                                                 \n");
//			out.write("                                                                          \n");
//			out.write("        filename = 'QueryHash=' + queryHash + '__QueryPlanHash=' + queryPlanHash; \n");
			out.write("    }                                                                     \n");
			out.write("    catch (error)                                                         \n");
			out.write("    {                                                                     \n");
			out.write("        console.error(error);                                             \n");
			out.write("    }                                                                     \n");
			out.write("                                                                          \n");
			out.write("    return filename;                                                      \n");
			out.write("}                                                                         \n");
			out.write("                                                                          \n");

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

			out.write("function copyShowplanFormattedToClipboard()                               \n");
			out.write("{                                                                         \n");
			out.write("    var copyFeedback = document.getElementById('copy-done');              \n");
			out.write("                                                                          \n");
			out.write("    // Set feedback                                                       \n");
			out.write("    copyFeedback.innerHTML     = 'The Query Plan was copied to Clipboard';\n");
			out.write("    copyFeedback.style.display = 'block';                                 \n");
			out.write("                                                                          \n");
			out.write("    // Copy to clipboard                                                  \n");
			out.write("    copyStringToClipboard(formatXml(showplanText));                       \n");
			out.write("                                                                          \n");
			out.write("    // Hide feedback                                                      \n");
			out.write("    setTimeout( function(){                                               \n");
			out.write("        copyFeedback.style.display = 'none';                              \n");
			out.write("    }, 3000);                                                             \n");
			out.write("}                                                                         \n");
			out.write("                                                                          \n");

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

			out.write("                                                                          \n");
			out.write("function downloadShowplan()                                               \n");
			out.write("{                                                                         \n");
			out.write("    var textToSave = showplanText;                                        \n");
			out.write("    var hiddenElement = document.createElement('a');                      \n");
			out.write("                                                                          \n");
			out.write("    hiddenElement.href     = 'data:attachment/text,' + encodeURI(textToSave); \n");
			out.write("    hiddenElement.target   = '_blank';                                    \n");
//			out.write("    hiddenElement.download = 'showplan_xxx.xml'                           \n");
			out.write("    hiddenElement.download = 'showplan__' + getFileNameFromXmlPlan(showplanText) + '.xml.sqlplan' \n");
			out.write("    hiddenElement.click();                                                \n");
			out.write("}                                                                         \n"); 

			out.write("                                                                          \n");
			out.write("function downloadShowplanFormatted()                                      \n");
			out.write("{                                                                         \n");
			out.write("    var textToSave = formatXml(showplanText);                             \n");
			out.write("    var hiddenElement = document.createElement('a');                      \n");
			out.write("                                                                          \n");
			out.write("    hiddenElement.href     = 'data:attachment/text,' + encodeURI(textToSave); \n");
			out.write("    hiddenElement.target   = '_blank';                                    \n");
//			out.write("    hiddenElement.download = 'showplan_xxx.xml'                           \n");
			out.write("    hiddenElement.download = 'showplan__' + getFileNameFromXmlPlan(showplanText) + '.xml.sqlplan' \n");
			out.write("    hiddenElement.click();                                                \n");
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

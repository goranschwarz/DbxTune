/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

public class HtmlUtils
{
//	public static String createJsScriptTag(String primary, String fallback)
//	{
//		// The below tries to do: <SCRIPT src="/scripts/xxx.js" onerror="document.write('<script src=\'https://xxx.js\'><\/script>')"></SCRIPT>
//		String str = "\n"
//			+ "  <SCRIPT src='" + primary + "'"
//			+           " onerror=\"document.write('<script src=\\'" + fallback + "\\'><\\/script>');"
//			+           " console.log('Failed Loading Primary script |" + primary + "|, fallback to |" + fallback + "|.');\""
//			+    "></SCRIPT> \n"
//			+ "";
//
//		return str;
//	}
	public static String createJsScriptTag(String primary, String... fallbacks)
	{
	    if (fallbacks == null || fallbacks.length == 0)
	    {
	        // No fallbacks provided, just return simple script tag
	        return "\n  <SCRIPT src='" + primary + "'></SCRIPT> \n";
	    }
	    
	    // Build the fallback script tags
	    StringBuilder fallbackScripts = new StringBuilder();
	    for (String fallback : fallbacks)
	    {
	        fallbackScripts.append("<script src=\\'").append(fallback).append("\\'><\\/script>");
	    }
	    
	    String str = "\n"
	        + "  <SCRIPT src='" + primary + "'"
	        +           " onerror=\"document.write('" + fallbackScripts.toString() + "');"
	        +           " console.log('Failed Loading Primary script |" + primary + "|, fallback to |" + String.join(", ", fallbacks) + "|.');\""
	        +    "></SCRIPT> \n"
	        + "";

	    return str;
	}

	public static String createJsScriptTag(StringBuilder sb, String primary, String... fallback)
	{
		String str = createJsScriptTag(primary, fallback);
		sb.append(str);
		return str;
	}

	public static String createJsScriptTag(Writer w, String primary, String... fallback) 
	throws IOException
	{
		String str = createJsScriptTag(primary, fallback);
		w.append(str);
		return str;
	}

	public static String createJsScriptTag(PrintWriter w, String primary, String... fallback) 
	{
		String str = createJsScriptTag(primary, fallback);
		w.append(str);
		return str;
	}
	

//	public static String createCssLinkTag(String primary, String fallback)
//	{
//	    // The below tries to do: <link rel="stylesheet" href="/styles/xxx.css" onerror="this.onerror=null; this.href='https://xxx.css'">
//	    String str = "\n"
//	        + "  <link rel='stylesheet' href='" + primary + "'"
//	        +       " onerror=\"this.onerror=null;"
//	        +       " this.href='" + fallback + "';"
//	        +       " console.log('Failed Loading Primary stylesheet |" + primary + "|, fallback to |" + fallback + "|.');\""
//	        +  "> \n"
//	        + "";
//
//	    return str;
//	}
	public static String createCssLinkTag(String primary, String... fallbacks)
	{
	    if (fallbacks == null || fallbacks.length == 0)
	    {
	        // No fallbacks provided, just return simple link tag
	        return "\n  <link rel='stylesheet' href='" + primary + "'> \n";
	    }
	    
	    // Build the fallback link creation script
	    StringBuilder fallbackScript = new StringBuilder();
	    for (int i = 0; i < fallbacks.length; i++)
	    {
	        fallbackScript.append("var link").append(i).append("=document.createElement('link');");
	        fallbackScript.append("link").append(i).append(".rel='stylesheet';");
	        fallbackScript.append("link").append(i).append(".href='").append(fallbacks[i]).append("';");
	        fallbackScript.append("document.head.appendChild(link").append(i).append(");");
	    }
	    
	    String str = "\n"
	        + "  <link rel='stylesheet' href='" + primary + "'"
	        +       " onerror=\"this.onerror=null;"
	        +       " " + fallbackScript.toString()
	        +       " console.log('Failed Loading Primary stylesheet |" + primary + "|, fallback to |" + String.join(", ", fallbacks) + "|.');\""
	        +  "> \n"
	        + "";

	    return str;
	}

	public static String createCssLinkTag(StringBuilder sb, String primary, String... fallback)
	{
		String str = createCssLinkTag(primary, fallback);
		sb.append(str);
		return str;
	}

	public static String createCssLinkTag(Writer w, String primary, String... fallback) 
	throws IOException
	{
		String str = createCssLinkTag(primary, fallback);
		w.append(str);
		return str;
	}
	
	public static String createCssLinkTag(PrintWriter w, String primary, String... fallback) 
	{
		String str = createCssLinkTag(primary, fallback);
		w.append(str);
		return str;
	}
	
	
//#####################################################################################
//## Possible solution 
//#####################################################################################
//    <script>
//    /**
//     * Loads scripts in order. If the primary fails, it tries the fallback
//     * before moving to the next script in the list.
//     */
//    function loadScriptsSequentially(scripts) 
//    {
//        if (scripts.length === 0) 
//        	return;
//    
//        const current = scripts.shift();
//        const scriptEl = document.createElement('script');
//        
//        // This is the key: async=false ensures execution order 
//        // is preserved even if scripts are added dynamically.
//        scriptEl.async = false;
//        scriptEl.src = current.primary;
//    
//        scriptEl.onload = function() 
//        {
//            console.log(`Successfully loaded: ${current.primary}`);
//            loadScriptsSequentially(scripts);
//        };
//    
//        scriptEl.onerror = function() 
//        {
//            console.error(`Failed: ${current.primary}. Trying fallback...`);
//            const fallbackEl = document.createElement('script');
//            fallbackEl.async = false;
//            fallbackEl.src = current.fallback;
//            
//            fallbackEl.onload = function() {
//                loadScriptsSequentially(scripts);
//            };
//            
//            document.head.appendChild(fallbackEl);
//        };
//    
//        document.head.appendChild(scriptEl);
//    }
//    
//    // Define your library dependencies in order
//    loadScriptsSequentially([
//        { primary: '/scripts/jquery/jquery-3.7.1.min.js',                      fallback: 'https://cdnjs.cloudflare.com' },
//        { primary: '/scripts/sorttable/sorttable.js',                          fallback: 'http://www.dbxtune.com'       },
//        { primary: '/scripts/jquery-sparklines/2.1.2/jquery.sparkline.min.js', fallback: 'https://cdnjs.cloudflare.com' }
//    ]);
//    </script>
//#####################################################################################
}

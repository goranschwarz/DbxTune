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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.gui.wizard;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.netbeans.spi.wizard.Summary;
import org.netbeans.spi.wizard.WizardException;
import org.netbeans.spi.wizard.WizardPage.WizardResultProducer;

import com.dbxtune.CounterControllerAbstract;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;


public class WizardUserDefinedCmResultProducer 
implements WizardResultProducer 
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger          = Logger.getLogger(WizardUserDefinedCmResultProducer.class);

	/**
	 * @see org.netbeans.spi.wizard.WizardPage.WizardResultProducer#cancel(java.util.Map)
	 */
	@SuppressWarnings("rawtypes")
	public boolean cancel(Map arg0) 
	{
		return true;
	}

	/**
	 * @see org.netbeans.spi.wizard.WizardPage.WizardResultProducer#finish(java.util.Map)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object finish(Map wizardData) 
	throws WizardException 
	{
		Boolean  tmp;
		boolean append = false;
		boolean tmpAdd = false;

		String summaryMessage = "";

		// propTexy
		String propText = (String) wizardData.get("propText");

		// append
		tmp = (Boolean) wizardData.get("appendToCfg");
		if (tmp != null) 
			append = tmp.booleanValue();

		// tmpAdd
		tmp = (Boolean) wizardData.get("addTmpToCfg");
		if (tmp != null) 
			tmpAdd = tmp.booleanValue();

		
		// Get configuration
		Configuration conf = Configuration.getInstance(Configuration.USER_CONF);
		String cfgFile = conf.getFilename();

		// APPEND TO FILE		
		if (append)
		{
//			// APPEND TO FILE
			if (propText == null || (propText != null && propText.trim().length() == 0) )
			{
				propText = createPropsStr( wizardData );
			}

			try
			{
				conf.append(propText, this.getClass().getName());
	
				// Reload to config file...
				// This just means that the 'properties' object is refreshed
				// but the Application is not "refreshed"
				conf.reload();
	
				summaryMessage += "\n\n##################################\n";
				summaryMessage += "The below information was appended to config file '" + cfgFile + "'.\n";
				summaryMessage += "--------------------------------------------------------\n";
				summaryMessage += propText;
				summaryMessage += "--------------------------------------------------------\n";
			}
			catch (IOException e)
			{
				summaryMessage += "\n\n##################################\n";
				summaryMessage += "Problems when APPENDING User Defined Counter the to file '"+cfgFile+"'.\n";
				summaryMessage += e.getMessage() + "\n";
				summaryMessage += "Opening the 'log viewer' so you can check other log messages.\n";
			}
		}
		else
		{
			summaryMessage += "\n\n##################################\n";
			summaryMessage += "NOTE: The information was NOT saved to the config file '" + cfgFile + "'.\n";
		}

		// ADD		
		if (tmpAdd)
		{
			try
			{
				Configuration tmpConf = createConf(wizardData);
				int failCount = CounterControllerAbstract.createUserDefinedCounterModels(tmpConf);
				
				if (failCount == 0)
				{
					summaryMessage += "\n\n##################################\n";
					summaryMessage += "ADDED the User Defined Counter to the GUI\n";
				}
				else
				{
					summaryMessage += "\n\n##################################\n";
					summaryMessage += "Problems when ADDING User Defined Counter the to GUI\n";
					summaryMessage += "Opening the 'log viewer' so you can check for problems.\n";
					MainFrame.openLogViewer();
				}
				
			}
			catch(Throwable t)
			{
				summaryMessage += "\n\n##################################\n";
				summaryMessage += "Problems when ADDING User Defined Counter the to GUI\n";
				summaryMessage += t.getMessage() + "\n";
				summaryMessage += StringUtil.stackTraceToString(t) + "\n";
				summaryMessage += "Opening the 'log viewer' so you can check other log messages.\n";
				_logger.error(t.getMessage(), t);

				MainFrame.openLogViewer();
			}
		}

		if (_logger.isDebugEnabled())
		{
			summaryMessage += "\n\n";
			summaryMessage += "--------------------------------------------------------\n";
			summaryMessage += "DEBUG, MAP: \n";
			summaryMessage += "--------------------------------------------------------\n";
			for (Iterator it = wizardData.keySet().iterator(); it.hasNext();)
	        {
		        String key = (String) it.next();
		        String val = (String) wizardData.get(key).toString();
	
		        summaryMessage += key + ": " + val + "\n";
	        }
		}

		Summary summary = Summary.create (summaryMessage, wizardData);

		return summary;
	}


	private static boolean hasVal(String str)
	{
		if (str == null)
			return false;
		if (str.trim().length() == 0)
			return false;
		return true;
	}
	private static String getKey(String key, Map<String,String> wizdata)
	{
		Object o = wizdata.get(key);
		if (o == null)
			return null;
		return o.toString();
	}
	public static String createPropsStr(Map<String,String> wizdata)
	{
		String name             = getKey("name"              , wizdata);
		String displayName      = getKey("displayName"       , wizdata);
		String description      = getKey("description"       , wizdata);
		String sqlInit          = getKey("sqlInit"           , wizdata);
		String sql              = getKey("sql"               , wizdata);
		String sqlClose         = getKey("sqlClose"          , wizdata);
		String needVersion      = getKey("needVersion"       , wizdata);
		String needRole         = getKey("needRole"          , wizdata);
		String needConfig       = getKey("needConfig"        , wizdata);
		String ttMonTab         = getKey("toolTipMonTables"  , wizdata);
		String pk               = getKey("pk"                , wizdata);
		String diff             = getKey("diff"              , wizdata);
		String pct              = getKey("pct"               , wizdata);
		String negDiffCntToZero = getKey("negativeDiffCountersToZero", wizdata);

		String graph            = getKey("graph"             , wizdata);
		String graphType        = getKey("graph.type"        , wizdata);
		String graphName        = getKey("graph.name"        , wizdata);
		String graphLabel       = getKey("graph.label"       , wizdata);
		String graphMenuLabel   = getKey("graph.menuLabel"   , wizdata);
		String graphDataCols    = getKey("graph.data.cols"   , wizdata);
		String graphDataMethods = getKey("graph.data.methods", wizdata);
		String graphDataLabels  = getKey("graph.data.labels" , wizdata);

		if (negDiffCntToZero == null) negDiffCntToZero = "true";

		String out = "";
		String prefix = "udc." + name + ".";

		out  = "\n\n";
		out += "### ================================================================================\n";
		out += "### BEGIN: UDC(User Defined Counter) - " + name + "\n";
		out += "### --------------------------------------------------------------------------------\n";
		
		boolean escapeSpaces = false; // false is more readable, but initial spaces on each row will be lost when reading the property
		
		                         out +=        prefix + "name              = " + name;
		                         out += "\n" + prefix + "displayName       = " + displayName;
		                         out += "\n" + prefix + "description       = " + description;
		if (hasVal(sqlInit))     out += "\n" + prefix + "sqlInit           = " + Configuration.saveConvert(sqlInit,  escapeSpaces, true).replace("\\n", "\\n\\\n");
		                         out += "\n" + prefix + "sql               = " + Configuration.saveConvert(sql,      escapeSpaces, true).replace("\\n", "\\n\\\n");
		if (hasVal(sqlClose))    out += "\n" + prefix + "sqlClose          = " + Configuration.saveConvert(sqlClose, escapeSpaces, true).replace("\\n", "\\n\\\n");
		if (hasVal(needVersion)) out += "\n" + prefix + "needVersion       = " + needVersion;
		if (hasVal(needRole))    out += "\n" + prefix + "needRole          = " + needRole;
		if (hasVal(needConfig))  out += "\n" + prefix + "needConfig        = " + needConfig;
		if (hasVal(ttMonTab))    out += "\n" + prefix + "toolTipMonTables  = " + ttMonTab;
		if (hasVal(pk))          out += "\n" + prefix + "pk                = " + pk;
		if (hasVal(diff))        out += "\n" + prefix + "diff              = " + diff;
		if (hasVal(pct))         out += "\n" + prefix + "pct               = " + pct;
	    out += "\n" + prefix + "negativeDiffCountersToZero = " + negDiffCntToZero;
				

		if ("true".equals(graph))
		{
			out += "\n" + prefix + "graph              = " + graph;
			out += "\n" + prefix + "graph.type         = " + graphType;
			out += "\n" + prefix + "graph.name         = " + graphName;
			out += "\n" + prefix + "graph.label        = " + graphLabel;
			out += "\n" + prefix + "graph.menuLabel    = " + graphMenuLabel;
			out += "\n" + prefix + "graph.data.cols    = " + graphDataCols;
			out += "\n" + prefix + "graph.data.methods = " + graphDataMethods;
			out += "\n" + prefix + "graph.data.labels  = " + graphDataLabels;
		}
		out += "\n"; // new line terminator for last property entry

		out += "### ================================================================================\n";
		out += "### END: UDC(User Defined Counter) - " + name + "\n";
		out += "### --------------------------------------------------------------------------------\n";
		out += "\n";

		return out;
	}
	public static Configuration createConf(Map<String,String> wizdata)
	{
		String name             = getKey("name"              , wizdata);
		String displayName      = getKey("displayName"       , wizdata);
		String description      = getKey("description"       , wizdata);
		String sqlInit          = getKey("sqlInit"           , wizdata);
		String sql              = getKey("sql"               , wizdata);
		String sqlClose         = getKey("sqlClose"          , wizdata);
		String needVersion      = getKey("needVersion"       , wizdata);
		String needRole         = getKey("needRole"          , wizdata);
		String needConfig       = getKey("needConfig"        , wizdata);
		String ttMonTab         = getKey("toolTipMonTables"  , wizdata);
		String pk               = getKey("pk"                , wizdata);
		String diff             = getKey("diff"              , wizdata);
		String pct              = getKey("pct"               , wizdata);
		String negDiffCntToZero = getKey("negativeDiffCountersToZero", wizdata);

		String graph            = getKey("graph"             , wizdata);
		String graphType        = getKey("graph.type"        , wizdata);
		String graphName        = getKey("graph.name"        , wizdata);
		String graphLabel       = getKey("graph.label"       , wizdata);
		String graphMenuLabel   = getKey("graph.menuLabel"   , wizdata);
		String graphDataCols    = getKey("graph.data.cols"   , wizdata);
		String graphDataMethods = getKey("graph.data.methods", wizdata);
		String graphDataLabels  = getKey("graph.data.labels" , wizdata);

		if (negDiffCntToZero == null) negDiffCntToZero = "true";

		Configuration out = new Configuration();

		String prefix = "udc." + name + ".";

		                         out.setProperty(prefix + "name",             name);
		                         out.setProperty(prefix + "displayName",      displayName);
		                         out.setProperty(prefix + "description",      description);
		if (hasVal(sqlInit))     out.setProperty(prefix + "sqlInit",          sqlInit);
		                         out.setProperty(prefix + "sql",              sql);
		if (hasVal(sqlClose))    out.setProperty(prefix + "sqlClose",         sqlClose);
		if (hasVal(needVersion)) out.setProperty(prefix + "needVersion",      needVersion);
		if (hasVal(needRole))    out.setProperty(prefix + "needRole",         needRole);
		if (hasVal(needConfig))  out.setProperty(prefix + "needConfig",       needConfig);
		if (hasVal(ttMonTab))    out.setProperty(prefix + "toolTipMonTables", ttMonTab);
		if (hasVal(pk))          out.setProperty(prefix + "pk",               pk);
		if (hasVal(diff))        out.setProperty(prefix + "diff",             diff);
		if (hasVal(pct))         out.setProperty(prefix + "pct",              pct);
                                 out.setProperty(prefix + "negativeDiffCountersToZero", negDiffCntToZero);
				

		if ("true".equals(graph))
		{
			out.setProperty(prefix + "graph",              graph);
			out.setProperty(prefix + "graph.type",         graphType);
			out.setProperty(prefix + "graph.name",         graphName);
			out.setProperty(prefix + "graph.label",        graphLabel);
			out.setProperty(prefix + "graph.menuLabel",    graphMenuLabel);
			out.setProperty(prefix + "graph.data.cols",    graphDataCols);
			out.setProperty(prefix + "graph.data.methods", graphDataMethods);
			out.setProperty(prefix + "graph.data.labels",  graphDataLabels);
		}

		return out;
	}
	
}

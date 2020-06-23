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
package com.asetune.sql.pipe;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.StringUtil;

/**
 * This one should be able to parse a bunch of things.<br>
 * and also several pipes... cmd | cmd | cmd
 * <p>
 * 
 * ============================================================
 * convert
 * ============================================================
 * ------------------------------------------------------------
 * 
 */
public class PipeCommandGraph
extends PipeCommandAbstract
{
	private String[] _args = null;

	private static class CmdParams
	{
		boolean   _data            = false;

//		String    _graphTypeStr    = null;
		GraphType _graphType       = GraphType.AUTO;
		boolean   _pivot           = false;
		
		String    _titleName       = null;
		String    _labelCategory   = null;
		String    _labelValue      = null;

		int       _rotateCategoryLabels = 0;
		
		boolean   _use3d           = false;

//		List<Integer> _keyCols     = null;
//		List<Integer> _valCols     = null;
		List<String>  _keyCols     = null;
		List<String>  _valCols     = null;

		boolean   _str2num         = false;
		String    _removeRegEx     = null;

		// Mig Layout Constraints 
		String    _widthMLC        = null;
		String    _heightMLC       = null;

		boolean   _showDataValues  = false;
		boolean   _showShapes      = false;
		boolean   _window          = false;
		boolean   _debug           = false;
		
		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();

			sb.append(""  ).append("data                ".trim()).append("=").append(StringUtil.quotify(_data                ));
			sb.append(", ").append("graphType           ".trim()).append("=").append(StringUtil.quotify(_graphType           ));
			sb.append(", ").append("pivot               ".trim()).append("=").append(StringUtil.quotify(_pivot               ));
			sb.append(", ").append("titleName           ".trim()).append("=").append(StringUtil.quotify(_titleName           ));
			sb.append(", ").append("labelCategory       ".trim()).append("=").append(StringUtil.quotify(_labelCategory       ));
			sb.append(", ").append("labelValue          ".trim()).append("=").append(StringUtil.quotify(_labelValue          ));
			sb.append(", ").append("rotateCategoryLabels".trim()).append("=").append(StringUtil.quotify(_rotateCategoryLabels));
			sb.append(", ").append("use3d               ".trim()).append("=").append(StringUtil.quotify(_use3d               ));
			sb.append(", ").append("keyCols             ".trim()).append("=").append(StringUtil.quotify(_keyCols             ));
			sb.append(", ").append("valCols             ".trim()).append("=").append(StringUtil.quotify(_valCols             ));
			sb.append(", ").append("str2num             ".trim()).append("=").append(StringUtil.quotify(_str2num             ));
			sb.append(", ").append("removeRegEx         ".trim()).append("=").append(StringUtil.quotify(_removeRegEx         ));
			sb.append(", ").append("widthMLC            ".trim()).append("=").append(StringUtil.quotify(_widthMLC            ));
			sb.append(", ").append("heightMLC           ".trim()).append("=").append(StringUtil.quotify(_heightMLC           ));
			sb.append(", ").append("showDataValues      ".trim()).append("=").append(StringUtil.quotify(_showDataValues      ));
			sb.append(", ").append("showShapes          ".trim()).append("=").append(StringUtil.quotify(_showShapes          ));
			sb.append(", ").append("window              ".trim()).append("=").append(StringUtil.quotify(_window              ));
			sb.append(", ").append("debug               ".trim()).append("=").append(StringUtil.quotify(_debug               ));
			sb.append(".");

			return sb.toString();
		}
	}
	
	private CmdParams _params = null;

	
	public String        getCmdLineParams()        { return _params.toString();            }
	                                                                                       
	public boolean       isAddDataEnabled()        { return _params._data;                 }
	public GraphType     getGraphType()            { return _params._graphType;            }
	public boolean       isPivotEnabled()          { return _params._pivot;                }
	public String        getGraphTitle()           { return _params._titleName;            }
	public String        getLabelCategory()        { return _params._labelCategory;        }
	public String        getLabelValue()           { return _params._labelValue;           }
	public boolean       is3dEnabled()             { return _params._use3d;                }
//	public List<Integer> getKeyCols()              { return _params._keyCols;              }
//	public List<Integer> getValCols()              { return _params._valCols;              }
	public List<String>  getKeyCols()              { return _params._keyCols;              }
	public List<String>  getValCols()              { return _params._valCols;              }
	public boolean       isStr2NumEnabled()        { return _params._str2num;              }
	public String        getRemoveRegEx()          { return _params._removeRegEx;          }
	public boolean       isShowDataValues()        { return _params._showDataValues;       }
	public boolean       isShowShapes()            { return _params._showShapes;           }
	public boolean       isWindowEnabled()         { return _params._window;               }
	public boolean       isDebugEnabled()          { return _params._debug;                }
	public int           getRotateCategoryLabels() { return _params._rotateCategoryLabels; }

	public void setPivot(boolean b) { _params._pivot = b; }

	
	public String getMigLayoutConstrains()
	{
		String layout = "grow, push";
		
		if (_params._widthMLC != null || _params._heightMLC != null)
		{
			layout = "";

			if (_params._widthMLC != null)
				layout += "width " + _params._widthMLC + ", ";

			if (_params._heightMLC != null)
				layout += "height " + _params._heightMLC + ", ";
		}

		layout = layout.trim();
		if (layout.endsWith(","))
			layout = layout.substring(0, layout.length()-1);
		
		return layout;
	}

	public enum GraphType
	{
		/** Automatically choose the best graph type */
		AUTO, 

		/** AREA Graph */
		AREA, 
		
		/** STACKED AREA Graph */
		SAREA, STACKEDAREA, 
		
		/** BAR Graph */
		BAR, 

		/** STACKED BAR Graph */
		SBAR, STACKEDBAR,

		/** LINE CHART */
		LINE, 

		/** PIE CHART */
		PIE, 

		/** XY */
//		XY, 

		/** TIMESERIES */
		TS, TIMESERIES
		;

		/** parse the value */
		public static GraphType fromString(String text)
		{
			for (GraphType type : GraphType.values()) 
			{
				// check for upper/lower: 
				if (type.name().equalsIgnoreCase(text))
					return type;

				// check for camelCase: 'maxOverMinutes', 'maxoverminutes'
				if (type.name().replace("_", "").equalsIgnoreCase(text))
					return type;
			}
//			if ("ts".equalsIgnoreCase(text))
//				return TIMESERIES;

			throw new IllegalArgumentException("Unknown GraphType '" + text + "' found, possible values: "+StringUtil.toCommaStr(GraphType.values()));
		}
	};
	
	
	public PipeCommandGraph(String input, String sqlString, ConnectionProvider connProvider)
	throws PipeCommandException
	{
		super(input, sqlString, connProvider);
		parse(input);
	}

	public void parse(String input)
	throws PipeCommandException
	{
		if (    input.startsWith("graph ") || input.equals("graph") 
		     || input.startsWith("chart ") || input.equals("chart")
		   )
		{
//			String params = input.substring(input.indexOf(' ') + 1).trim();
//			_args = StringUtil.translateCommandline(params);
			_args = StringUtil.translateCommandline(input, true);

			CommandLine cmdLine = parseCmdLine(_args);
			if (cmdLine.hasOption('d')) _params._data                 = true;
			if (cmdLine.hasOption('t')) _params._graphType            = GraphType.fromString(cmdLine.getOptionValue('t'));
			if (cmdLine.hasOption('p')) _params._pivot                = true;
			if (cmdLine.hasOption('n')) _params._titleName            = cmdLine.getOptionValue('n');
			if (cmdLine.hasOption('3')) _params._use3d                = true;
//			if (cmdLine.hasOption('k')) _params._keyCols              = parseCommaStrToIntArray(cmdLine.getOptionValue('k'));
//			if (cmdLine.hasOption('v')) _params._valCols              = parseCommaStrToIntArray(cmdLine.getOptionValue('v'));
			if (cmdLine.hasOption('k')) _params._keyCols              = StringUtil.commaStrToList(cmdLine.getOptionValue('k'));
			if (cmdLine.hasOption('v')) _params._valCols              = StringUtil.commaStrToList(cmdLine.getOptionValue('v'));
			if (cmdLine.hasOption('c')) _params._str2num              = true;
			if (cmdLine.hasOption('r')) _params._removeRegEx          = cmdLine.getOptionValue('r');
			if (cmdLine.hasOption('l')) _params._labelCategory        = cmdLine.getOptionValue('l');
			if (cmdLine.hasOption('L')) _params._labelValue           = cmdLine.getOptionValue('L');
			if (cmdLine.hasOption('R')) _params._rotateCategoryLabels = StringUtil.parseInt(cmdLine.getOptionValue('R'), 0);
			if (cmdLine.hasOption('w')) _params._widthMLC             = cmdLine.getOptionValue('w');
			if (cmdLine.hasOption('h')) _params._heightMLC            = cmdLine.getOptionValue('h');
			if (cmdLine.hasOption('D')) _params._showDataValues       = true;
			if (cmdLine.hasOption('S')) _params._showShapes           = true;
			if (cmdLine.hasOption('W')) _params._window               = true;
			if (cmdLine.hasOption('x')) _params._debug                = true;
		}
		else
		{
			throw new PipeCommandException("PipeCommand, cmd='"+input+"' is unknown. Available commands is: graph or chart");
		}
		
//		System.out.println("PipeCommandGrep: _optV='"+_optV+"', _optX='"+_optX+"', _type='"+_type+"', _grepStr='"+_grepStr+"'.");
	}

	@Override
	public String getConfig()
	{
		if (_params == null)
			return "graph: ...";
		
		return _params.toString();
	}

	
//	private CommandLine parseCmdLine(String args)
//	throws PipeCommandException
//	{
//		return parseCmdLine(StringUtil.translateCommandline(args));
////		return parseCmdLine(args.split(" "));
//	}
	private CommandLine parseCmdLine(String[] args)
	throws PipeCommandException
	{
		Options options = new Options();

		// Switches       short long Option              hasParam Description (not really used)
		//                ----- ------------------------ -------- ------------------------------------------
		options.addOption( "d", "data",                  false,   "Also add table data to the output" );
		options.addOption( "t", "type",                  true,    "What type of graph do you want to produce." );
		options.addOption( "p", "pivot",                 false,   "" );
		options.addOption( "n", "name",                  true,    "Name of the graph" );
		options.addOption( "3", "3d",                    false,   "Use 3D graphs if possible" );
		options.addOption( "k", "keyCols",               true,    "" );
		options.addOption( "v", "valCols",               true,    "" );
		options.addOption( "c", "str2num",               false,   "Try to convert String columns to numbers." );
		options.addOption( "r", "removeRegEx",           true,    "In combination with '-c', remove some strings using a RegEx" );
		options.addOption( "l", "labelCategory",         true,    "" );
		options.addOption( "L", "labelValue",            true,    "" );
		options.addOption( "R", "rotateCategoryLabels",  true,    "" );
		options.addOption( "w", "width",                 true,    "" );
		options.addOption( "h", "height",                true,    "" );
		options.addOption( "D", "showDataValues",        false,   "" );
		options.addOption( "S", "showShapes",            false,   "" );
		options.addOption( "W", "window",                false,   "" );
		options.addOption( "x", "debug",                 false,   "debug" );

		try
		{
			_params = new CmdParams();
			
			// create the command line com.asetune.parser
			CommandLineParser parser = new DefaultParser();

			// parse the command line arguments
			CommandLine cmd = parser.parse( options, args );

			if ( cmd.getArgs() != null && cmd.getArgs().length > 0 )
			{
				String error = "Unknown options: " + StringUtil.toCommaStr(cmd.getArgs());
				printHelp(options, error);
			}
//			if ( cmd.getArgs() != null && cmd.getArgs().length == 0 )
//			{
//				if (cmd.hasOption('x'))
//					; // if option 'x' we don't need any parameters
//				else
//				{
//					String error = "Missing string to use for 'graph' or 'chart' command.";
//					printHelp(options, error);
//				}
//			}
			if ( cmd.getArgs() != null && cmd.getArgs().length > 1 )
			{
				String error = "To many options: " + StringUtil.toCommaStr(cmd.getArgs());
				printHelp(options, error);
			}
			return cmd;
		}
		catch (ParseException pe)
		{
			String error = "Error: " + pe.getMessage();
			printHelp(options, error);
			return null;
		}	
	}
	private static void printHelp(Options options, String errorStr)
	throws PipeCommandException
	{
		StringBuilder sb = new StringBuilder();

		if (errorStr != null)
		{
			sb.append("\n");
			sb.append(errorStr);
			sb.append("\n");
		}

		sb.append("\n");
		sb.append("usage: graph or chart [-d] [-t <type>] [-p] [-3] [-k <csv>] [-v <csv>] \n");
		sb.append("                      [-n <name>] [-l <label>] [-L <label>] [-c] [-r <regEx>]\n");
		sb.append("                      [-w <width>] [-h <height>] [-D] [-S] [-W] [-x]\n");
		sb.append("\n");
		sb.append("options: \n");
		sb.append("  -d,--data                 Also add table data to the output \n");
		sb.append("  -t,--type        <type>   What type of graph do you want to produce. \n");
		sb.append("                   auto      - Try to figgure out what you want (default)\n");
		sb.append("                   bar       - bar graph. \n");
		sb.append("                   sbar      - stacked bar graph. \n");
		sb.append("                   area      - area graph. \n");
		sb.append("                   sarea     - stacked area graph. \n");
		sb.append("                   line      - line chart. \n");
		sb.append("                   pie       - pie chart. \n");
		sb.append("                   ts        - time series data. \n");
		sb.append("  -p,--pivot                Turn the columns into rows or vice verse (based on graph type)\n");
		sb.append("  -3,--3d                   If possible use 3D graphs/charts. \n");
		sb.append("  -k,--keyCols              Comma separated list of KEY columns to use: ColNames or ColPos (pos starts at 0) \n");
		sb.append("  -v,--valCols              Comma separated list of VALUE columns to use ColNames or ColPos (pos starts at 0) \n");
		sb.append("  -n,--name          name   Name of the graph. (printed on top) \n");
		sb.append("  -l,--labelCategory name   Label for Categories \n");
		sb.append("  -L,--labelValue    name   Label for Values \n");
		sb.append("  -R,--rotateCategoryLabels Rotate Category Labels [1=45_UP, 2=UP_90, 3=DOWN_45, 4=DOWN_90, else=45_UP]\n");
		sb.append("  -c,--str2num              Try to convert String Columns to numbers. \n");
		sb.append("  -r,--removeRegEx   str    In combination with '-c', remove some strings column content using a RegEx \n");
		sb.append("                             - example to remove KB or KB from columns: go | graph -c -r '(KB|MB)'\n");
		sb.append("\n");
		sb.append("  -w,--width         spec   Width  of the graph/chart \n");
		sb.append("  -h,--height        spec   Height of the graph/chart \n");
		sb.append("\n");
		sb.append("  -D,--showDataValues       Show Data Values in graphs (easier to see data values)\n");
		sb.append("  -S,--showShapes           Show Shapes/boxes on data points (easier see data points in smaller datasets) \n");
		sb.append("  -W,--window               Open Graph/Chart in it's own Windows. \n");
		sb.append("  -x,--debug                Debug, print some extra info \n");
		sb.append("  \n");
		sb.append("  \n");
		
		
		throw new PipeCommandException(sb.toString());
	}
}

package com.asetune.sql.pipe;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * This one should be able to parse a bunch of things.<br>
 * and also several pipes... cmd | cmd | cmd
 * <p>
 * 
 * ============================================================
 * grep or egrep
 * ============================================================
 * [rs[:colname|all]|msg|all]{grep|egrep} [-v] stringToGrepFor
 * 
 * rs[:colname|all]  = do the command on a result set. (:onOnlyOneSpecificColumnName, or all columns) DEFAULT = 'all'
 * msg               = do the command on a AseMessage
 * all               = do the command on 'all' the output from the sql batch
 * Default: *rs*
 * ------------------------------------------------------------
 * 
 */
public class PipeCommandGrep
extends PipeCommandAbstract
{
//	public enum Types {All, ResultSet, Message}
	public static final int TYPE_RESULTSET  = 1;
	public static final int TYPE_MSG        = 2;
//	public static final int TYPE_NEXT_TYPEa = 4;
//	public static final int TYPE_NEXT_TYPEb = 8;
//	public static final int TYPE_NEXT_TYPEc = 16;
//	public static final int TYPE_NEXT_TYPEd = 32;

	private String  _grepStr = null;
	private boolean _optV    = false;
	private int     _type    = 0;
	public boolean	_optX    = false;

	public PipeCommandGrep(String input)
	throws PipeCommandException
	{
		parse(input);
	}

//	public void parse(String input)
//	throws PipeCommandException
//	{
////		_type = _type | TYPE_RESULTSET;
////		_type = _type | TYPE_MSG;
//
//		if (input.startsWith("grep ") || input.startsWith("egrep "))
//		{
//			String params = input.substring(input.indexOf(' ') + 1).trim();
//
//			// TODO parse for cmdline switches to grep: like -v etc
//			while (params.startsWith("-v ") || params.startsWith("-x ") || params.startsWith("-r ") || params.startsWith("-m ") )
//			{
//				if (params.startsWith("-v "))
//				{
//					_optV = true;
//					params = params.substring(params.indexOf(' ') + 1).trim();
//				}
//				if (params.startsWith("-x "))
//				{
//					_optX = true;
//					params = params.substring(params.indexOf(' ') + 1).trim();
//				}
//				if (params.startsWith("-r "))
//				{
//					_type = _type | TYPE_RESULTSET;
//					params = params.substring(params.indexOf(' ') + 1).trim();
//				}
//				if (params.startsWith("-m "))
//				{
//					_type = _type | TYPE_MSG;
//					params = params.substring(params.indexOf(' ') + 1).trim();
//				}
//			}
//			// if no options just do grep for ResultSets
//			if (_type == 0)
//				_type = _type | TYPE_RESULTSET;
//
//			_grepStr = params;
//		}
//		else
//		{
//			throw new PipeCommandException("PipeCommand, cmd='"+input+"' is unknown. Available commands is: grep, egrep");
//		}
//		
//		System.out.println("PipeCommandGrep: _optV='"+_optV+"', _optX='"+_optX+"', _type='"+_type+"', _grepStr='"+_grepStr+"'.");
//	}
	public void parse(String input)
	throws PipeCommandException
	{
//		_type = _type | TYPE_RESULTSET;
//		_type = _type | TYPE_MSG;

		if (input.startsWith("grep ") || input.startsWith("egrep "))
		{
			String params = input.substring(input.indexOf(' ') + 1).trim();

			CommandLine cmdLine = parseCmdLine(params);
			if (cmdLine.hasOption('v')) _optV = true; 
			if (cmdLine.hasOption('x')) _optX = true;
			if (cmdLine.hasOption('r')) _type = _type | TYPE_RESULTSET;
			if (cmdLine.hasOption('m')) _type = _type | TYPE_MSG; 
			if (cmdLine.hasOption('e')) _grepStr = cmdLine.getOptionValue('e');

			if ( cmdLine.getArgs() != null && cmdLine.getArgs().length == 1 )
			{
				_grepStr = (String) cmdLine.getArgList().get(0);
			}

			// if no options just do grep for ResultSets
			if (_type == 0)
				_type = _type | TYPE_RESULTSET;

			// Do not do grep, just fix the Msg into ONE line
			if (_optX)
				_type = 0;
		}
		else
		{
			throw new PipeCommandException("PipeCommand, cmd='"+input+"' is unknown. Available commands is: grep, egrep");
		}
		
		System.out.println("PipeCommandGrep: _optV='"+_optV+"', _optX='"+_optX+"', _type='"+_type+"', _grepStr='"+_grepStr+"'.");
	}

	@Override
	public String getConfig()
	{
		return _grepStr;
	}

	public boolean isOptV()
	{
		return _optV;
	}

	public boolean isOptX()
	{
		return _optX;
	}

	public boolean isValidForType(int option)
	{
		return (_type & option) == option;
	}

	/**
	 * Name of the column to grep on
	 * @return null = any column
	 */
	public String getColName()
	{
		return null;
	}



	private CommandLine parseCmdLine(String args)
	throws PipeCommandException
	{
		return parseCmdLine(args.split(" "));
	}
	private CommandLine parseCmdLine(String[] args)
	throws PipeCommandException
	{
		Options options = new Options();

		// create the Options
		options.addOption( "v", null,     false, "Same as -v to grep" );
		options.addOption( "x", null,     false, "Dummy to strip Multi Line Message" );
		options.addOption( "r", "rs",     false, "grep ResultSet (default)" );
		options.addOption( "m", "msg",    false, "grep Messages" );
//		options.addOption( "c", "col",    true,  "Column name in ResultSet" );
//		options.addOption( "e", "regexp", true,  "regexp to search" );

		try
		{
			// create the command line com.asetune.parser
			CommandLineParser parser = new PosixParser();

			// parse the command line arguments
			CommandLine cmd = parser.parse( options, args );

//			if ( cmd.getArgs() != null && cmd.getArgs().length > 0 )
//			{
//				String error = "Unknown options: " + StringUtil.toCommaStr(cmd.getArgs());
//				printHelp(options, error);
//			}
			if ( cmd.getArgs() != null && cmd.getArgs().length == 0 )
			{
				if (cmd.hasOption('x'))
					; // if option 'x' we don't need any parameters
				else
				{
					String error = "Missing string to use for grep";
					printHelp(options, error);
				}
			}
//			if ( cmd.getArgs() != null && cmd.getArgs().length > 1 )
//			{
//				String error = "To many options: " + StringUtil.toCommaStr(cmd.getArgs());
//				printHelp(options, error);
//			}
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

//		sb.append("usage: grep [-v] [-r [-c <col>]] [-m] [-x] {[-e <regexp>] | [str]} \n");
		sb.append("usage: grep [-v] [-r] [-m] [-x] str \n");
		sb.append("\n");
		sb.append("options: \n");
		sb.append("  -v                Username when connecting to server. \n");
		sb.append("  -r,--rs           grep in ResultSet \n");
//		sb.append("  -c,--col <name>   grep in ResultSet, but only in column named. \n");
		sb.append("  -m,--msg          grep in Messages. \n");
		sb.append("  -x,--short        Strip multi line messages into Msg #: str. \n");
//		sb.append("  -e,--regexp <str> Regexp parameter to search for. \n");
		sb.append("  \n");
		
		throw new PipeCommandException(sb.toString());
	}

}
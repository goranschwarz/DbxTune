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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.sql.SQLException;

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
public class PipeCommandConvert
extends PipeCommandAbstract
{
	private static final String DEFAULT_fromCharset = "ISO-8859-1";
	private static final String DEFAULT_toCharset   = "UTF-8";

	private String[] _args = null;

	private String  _fromCharset = DEFAULT_fromCharset;
	private String  _toCharset   = DEFAULT_toCharset;

	public PipeCommandConvert(String input, String sqlString, ConnectionProvider connProvider)
	throws PipeCommandException
	{
		super(input, sqlString, connProvider);
		parse(input);
	}

	public void parse(String input)
	throws PipeCommandException
	{
		if (    input.startsWith("convert ")  || input.equals("convert") 
		     || input.startsWith("iconv ")    || input.equals("iconv")
		   )
		{
//			String params = input.substring(input.indexOf(' ') + 1).trim();
//			_args = StringUtil.translateCommandline(params);
			_args = StringUtil.translateCommandline(input, true);

			CommandLine cmdLine = parseCmdLine(_args);
			if (cmdLine.hasOption('f')) _fromCharset = cmdLine.getOptionValue('f');
			if (cmdLine.hasOption('t')) _toCharset   = cmdLine.getOptionValue('t');

			if (cmdLine.hasOption('l'))
			{
//				String availableCharsets = Charset.availableCharsets().keySet().toString(); 
				String availableCharsets = "";
				for (String encoding : Charset.availableCharsets().keySet())
					availableCharsets += "    " + encoding + ", \n"; 
				
				availableCharsets =StringUtil.removeLastComma(availableCharsets);
				
				throw new PipeCommandException("Available chartsets encoding: \n"+availableCharsets);
			}
		}
		else
		{
			throw new PipeCommandException("PipeCommand, cmd='"+input+"' is unknown. Available commands is: convert, iconv");
		}
		
//		System.out.println("PipeCommandGrep: _optV='"+_optV+"', _optX='"+_optX+"', _type='"+_type+"', _grepStr='"+_grepStr+"'.");
	}

	@Override
	public String getConfig()
	{
		return "convert: from='"+_fromCharset+"', to='"+_toCharset+"'.";
	}

	public String convert(int row, int col, String toBeConverted)
	throws SQLException
	{
		if (toBeConverted == null)
			return null;
		
		// below is a embryo found at: https://stackoverflow.com/questions/6175169/convert-iso8859-string-to-utf8-%C3%84%C3%96%C3%9C-%C3%83%C3%83-why
		// but we need to implement something like PipeCommandGrep, with parameters, so we can specify 
		try
		{
			String readable = new String(toBeConverted.getBytes(_fromCharset), _toCharset);
			return readable;
		}
		catch (UnsupportedEncodingException ex)
		{
			String msg = "Problems converting row="+row+", col="+col+", value '"+toBeConverted+"' from '"+_fromCharset+"' to '"+_toCharset+"'. Caught: "+ex;
			throw new SQLException(msg);
		}
		
	}
//	/**
//	 * Called to check if this "grep" is a valid item<br>
//	 *  
//	 * @param obj Value to check
//	 * @return true if the item/record should be accepted (or sent to next step/pipe)
//	 */
//	public boolean acceptItem(Object obj)
//	{
//		// NOTE: this is not yet implemented... 
//		//       but it could/should be used instead of grabbing _grepRegexp by getConfig()
//		//       This means we can
//		//         * Compile a regexp  and then use find() instead of String.match() , since the match() "adds" ^regExpStr$ ... match() must match the entire row, while find() is more like a normal "grep"
//		//         * implement the -e flag and have several regexp's in a linked list...
//		//         * the -v flag and isOptV() do not have to be exposed...
//		//         * etc, etc...
//		// But on the other hand, I have to rethinks the "pipe" strategy totally, right now it's not working as intended!
//		if (obj instanceof String)
//		{
//		}
//		return true;
//	}

	
	private CommandLine parseCmdLine(String args)
	throws PipeCommandException
	{
		return parseCmdLine(StringUtil.translateCommandline(args, true));
//		return parseCmdLine(StringUtil.translateCommandline(args));
//		return parseCmdLine(args.split(" "));
	}
	private CommandLine parseCmdLine(String[] args)
	throws PipeCommandException
	{
		Options options = new Options();

		// create the Options
		options.addOption( "f", "from-code",     true,  "Convert characters from encoding" );
		options.addOption( "t", "to-code",       true,  "Convert characters to encoding" );
		options.addOption( "l", "list",          false, "List known coded character sets" );

		try
		{
			// create the command line com.asetune.parser
			CommandLineParser parser = new DefaultParser();

			// parse the command line arguments
			CommandLine cmd = parser.parse( options, args );

//			if ( cmd.getArgs() != null && cmd.getArgs().length > 0 )
//			{
//				String error = "Unknown options: " + StringUtil.toCommaStr(cmd.getArgs());
//				printHelp(options, error);
//			}
			if ( cmd.getArgs() != null && cmd.getArgs().length == 0 )
			{
//				if (cmd.hasOption('x'))
//					; // if option 'x' we don't need any parameters
//				else
//				{
//					String error = "Missing string to use for convert|iconv";
//					printHelp(options, error);
//				}
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
		sb.append("usage: convert [-f encoding] [-t encoding] \n");
		sb.append("\n");
		sb.append("options: \n");
		sb.append("  -f,--from-code <encoding>  Convert characters from encoding, default='"+DEFAULT_fromCharset+"'. \n");
		sb.append("  -t,--to-code   <encoding>  Convert characters to encoding,   default='"+DEFAULT_toCharset+"'. \n");
		sb.append("  -l,--list                  List known coded character sets. \n");
		sb.append("  \n");
		sb.append("  convert or iconv tries to convert from one charset to another\n");
		sb.append("  Only columns containing 'Strings' will be converted.\n");
		sb.append("  \n");
		sb.append("  This command might be usefull if someone has stored string values \n");
		sb.append("  in the database with a faulty charset encoding. \n");
		sb.append("  \n");
		sb.append("  For example if someone inserts a UTF-8 encoded swedish char 'ö' into a ISO-8859-1 storage. \n");
		sb.append("  The stored value would probably be 'Ã¶' instead of 'ö'... \n");
		sb.append("  Because the UTF-8 encoded value for 'ö' will be 2 bytes (c3 b6), hence 'Ã¶'... \n");
		sb.append("  Then try: convert -f ISO-8859-1 -t UTF-8 \n");
		sb.append("  \n");
		sb.append("  For java people, here is what's basically done in the code:\n");
		sb.append("      new String(strToBeConverted.getBytes(fromCharset), toCharset)\n");
		sb.append("  \n");
		
		
		throw new PipeCommandException(sb.toString());
	}

}

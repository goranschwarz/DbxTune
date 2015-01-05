package com.asetune.tools.sqlw;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.utils.StringUtil;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;

public class SqlParam
{
	private static Logger _logger = Logger.getLogger(SqlParam.class);

	private int     _sqlType  = Types.OTHER;
	private Object  _val      = null;
	private boolean _isOutput = false;

	public int     getSqlType()    { return _sqlType; }
	public Object  getValue()      { return _val; }
	public boolean isOutputParam() { return _isOutput; }

	public void sqtSqlType(int sqlType) { _sqlType = sqlType; }
	public void setValue  (Object val)  { _val     = val;     }

	/**
	  * The SQL TYPE for an Oracle CURSOR in a Oracle Stored Procedure.
	  * This duplicates the OracleTypes.CURSOR, but with this constant 
	  * we do not need to import com.oracle.* jars into this project.
	  * However this class is still 100% dependent on Oracle at runtime 
	  * and cannot be unit tested without Oracle.
	  */
	public static int ORACLE_CURSOR_TYPE = -10;

	public static SqlParam parseEntry(String entry)
	{
		if (entry == null)
			throw new RuntimeException("Problem parsing RPC Parameter entry '"+entry+"', is NULL.");

		boolean isOracleResultSetOutputParameter = entry.trim().equalsIgnoreCase("ora_rs");

		if (isOracleResultSetOutputParameter)
		{
			SqlParam p = new SqlParam();
			p._isOutput = true;
			p._sqlType  = ORACLE_CURSOR_TYPE; 
			p._val      = null;

			return p;
		}

		int eqPos   = entry.indexOf('=');
		if (eqPos == -1)
			throw new RuntimeException("Problem parsing RPC Parameter entry '"+entry+"', no equal char is found. Expecting: 'int|bigint|string|numeric|timestamp[(fmt)]|date[(fmt)]|time[(fmt)]|clob|blob = value' or 'ora_rs'");
		String type = entry.substring(0, eqPos).trim();
		String val  = entry.substring(eqPos+1).trim();
		
		// timestamp may have a format description: timestamp(yyyy-MM-dd HH:mm:ss)
		String fmt = null;
		if (type != null && type.indexOf('(') >= 0)
		{
			int lpPos = type.indexOf('(');

			fmt  = type.substring(lpPos+1).trim();
			if (fmt.endsWith(")"))
				fmt  = fmt.substring(0, fmt.length()-1).trim();

			type = type.substring(0, lpPos);
		}
		
		SqlParam p = new SqlParam();

		if (val.endsWith(" out") || val.endsWith(" OUT") )
		{
			p._isOutput = true;
			val = val.substring(0, val.length() - " out".length()).trim();
		}
		val = StringUtil.unquote(val);
//		if ( (val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'")) )
//		{
//			val = val.substring(1, val.length() - 1);
//		}
		boolean isNull = val.equalsIgnoreCase("null");

//System.out.println("XXXX: type=|"+type+"|, val=|"+val+"|, isNull="+isNull+", fmt=|"+fmt+"|.");
		// STRING
		if ("string".equals(type) || "str".equals(type) || "varchar".equals(type)) 
		{
			p._sqlType = Types.VARCHAR; 

			if (isNull)
				p._val = null;
			else
			{
				p._val = new String(val);
			}
		}
		// STRING as CHAR
		else if ("char".equals(type)) 
		{
			p._sqlType = Types.CHAR; 

			if (isNull)
				p._val = null;
			else
			{
				p._val = new String(val);
			}
		}
		// INT
		else if ("int".equals(type) || "integer".equals(type)) 
		{
			p._sqlType = Types.INTEGER; 
			if (isNull)
				p._val = null;
			else
			{
				try { p._val = new Integer(val); }
				catch(NumberFormatException e) {throw new RuntimeException(" Problems parsing value '"+val+"' to a Integer.", e);}
			}
		}
		// BIGINT
		else if ("bigint".equals(type) || "biginteger".equals(type) || "long".equals(type)) 
		{
			p._sqlType = Types.BIGINT;  
			if (isNull)
				p._val = null;
			else
			{
				try { p._val = new Long(val); }
				catch(NumberFormatException e) {throw new RuntimeException(" Problems parsing value '"+val+"' to a Long.", e);}
			}
		}
		// NUMERIC
		else if ("numeric".equals(type) || "number".equals(type)) 
		{
			p._sqlType = Types.NUMERIC;
			if (isNull)
				p._val = null;
			else
			{
				try { p._val = new BigDecimal(val); }
				catch(NumberFormatException e) {throw new RuntimeException(" Problems parsing value '"+val+"' to a BigDecimal.", e);}
			}
		}
		// TIMESTAMP
		else if ("timestamp".equals(type)) 
		{
			p._sqlType = Types.TIMESTAMP; 
			if (isNull)
				p._val = null;
			else
			{
				if (fmt == null)
					fmt = "yyyy-MM-dd HH:mm:ss";
				SimpleDateFormat sdf = new SimpleDateFormat(fmt);
				try { p._val = new java.sql.Timestamp( sdf.parse(val).getTime() ); }
				catch (ParseException e) { throw new RuntimeException("Problems parsing value '"+val+"' to a Timestamp using the format '"+fmt+"'. For format see: http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html", e); }
			}
		}
		// DATE
		else if ("date".equals(type)) 
		{
			p._sqlType = Types.DATE; 
			if (isNull)
				p._val = null;
			else
			{
				if (fmt == null)
					fmt = "yyyy-MM-dd";
				SimpleDateFormat sdf = new SimpleDateFormat(fmt);
				try { p._val = new java.sql.Date( sdf.parse(val).getTime() ); }
				catch (ParseException e) { throw new RuntimeException("Problems parsing value '"+val+"' to a Date using the format '"+fmt+"'. For format see: http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html", e); }
			}
		}
		// TIME
		else if ("time".equals(type)) 
		{
			p._sqlType = Types.TIME; 
			if (isNull)
				p._val = null;
			else
			{
				if (fmt == null)
					fmt = "HH:mm:ss";
				SimpleDateFormat sdf = new SimpleDateFormat(fmt);
				try { p._val = new java.sql.Time( sdf.parse(val).getTime() ); }
				catch (ParseException e) { throw new RuntimeException("Problems parsing value '"+val+"' to a Time using the format '"+fmt+"'. For format see: http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html", e); }
			}
		}
		// CLOB
		else if ("clob".equals(type)) 
		{
			p._sqlType = Types.CLOB; 
			p._val = isNull ? null : readCLobValue(val); 
		}
		// BLOB
		else if ("blob".equals(type)) 
		{
			p._sqlType = Types.BLOB; 
			p._val = isNull ? null : readBLobValue(val); 
		}
		// UNKNOWN
		else throw new RuntimeException("Unknown RPC Datatype '"+type+"'. known datatypes 'int|bigint|string|numeric|timestamp[(fmt)]|date[(fmt)]|time[(fmt)]|clob|blob|ora_rs'");

//System.out.println("p._val=|"+p._val+"|, obj=" + (p._val == null ? "-null-" : p._val.getClass().getName()) );
		return p;
	}
	private static String readCLobValue(String urlStr)
	{
		ByteArrayOutputStream buffer = readLobInputStream(urlStr);
		return buffer.toString();
	}
	private static byte[] readBLobValue(String urlStr)
	{
		ByteArrayOutputStream buffer = readLobInputStream(urlStr);
		return buffer.toByteArray();
	}
	private static ByteArrayOutputStream readLobInputStream(String urlStr)
	{
		InputStream is = readLobValue(urlStr);

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data       = new byte[16384];
		byte[] firstChunk = null; // can be used to determine the content

		try
		{
			while ((nRead = is.read(data, 0, data.length)) != -1) 
			{
				buffer.write(data, 0, nRead);
				if (firstChunk == null)
					firstChunk = Arrays.copyOf(data, 1024);
			}
			buffer.flush();
			is.close();

			if (firstChunk != null)
			{
				ContentInfoUtil util = new ContentInfoUtil();
				ContentInfo info = util.findMatch( firstChunk );
				_logger.info("Loaded file or URL '"+urlStr+"' which is of Content '" + (info == null ? "unknown" : info.toString()) + "'.");
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException("Problems reading the InputStream from the URL or file '" + urlStr + "'. Caught: "+e, e);
		}

		return buffer;
	}
	private static InputStream readLobValue(String urlStr)
	{
		try 
		{
			try
			{
				URL url = new URL(urlStr);
				InputStream is = url.openStream();
				return is;
			}
			catch(MalformedURLException e)
			{
//System.out.println("rpc.readFile(urlStr='"+urlStr+"'): problems reading the URL, trying to open it as a file instead. caught: "+e);
				File file = new File(urlStr);
//				if (!file.exists())
//					throw new RuntimeException("File " + urlStr + " cannot be found.");

				FileInputStream fis = new FileInputStream(file);
				return fis;
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException("Problems reading the URL or file '" + urlStr + "'. Caught: "+e, e);
		}
	}

	public static ArrayList<SqlParam> parse(String rpcParamsStr)
	{
		ArrayList<SqlParam> retList = new ArrayList<SqlParam>();

		List<String> tmp = StringUtil.splitOnCommasAllowQuotes(rpcParamsStr, true);
		if ( tmp.size() == 0 && StringUtil.isNullOrBlank(tmp.get(0)) )
			throw new RuntimeException("Problem parsing RPC Parameter String '"+rpcParamsStr+"', it looks like it's empty. Expecting: 'int|bigint|string|numeric|timestamp[(fmt)]|date[(fmt)]|time[(fmt)]|clob|blob = value' or 'ora_rs'.");
		
		for (int i=0; i<tmp.size(); i++)
		{
			String entry = tmp.get(i).trim();
			SqlParam p = parseEntry(entry);
			if (_logger.isDebugEnabled())
				_logger.debug("RPC PARAM "+i+": |"+entry+"|. type=|"+p._sqlType+"|, val=|"+p._val+"|, isOutParam="+p._isOutput+".");
//System.out.println("RPC PARAM "+i+": |"+entry+"|. type=|"+p._sqlType+"|, val=|"+p._val+"|, isOutParam="+p._isOutput+".");
			retList.add(p);
		}
		return retList;

//		String[] tmp = rpcParamsStr.split(",");
//		if ( tmp.length == 1 && StringUtil.isNullOrBlank(tmp[0]) )
//			throw new RuntimeException("Problem parsing RPC Parameter String '"+rpcParamsStr+"', it looks like it's empty. Expecting: 'int|string = value' or 'ora_rs'.");
//
//		for (int i=0; i<tmp.length; i++)
//		{
//			String entry = tmp[i].trim();
//			SqlParam p = parseEntry(entry);
//			if (_logger.isDebugEnabled())
//				_logger.debug("RPC PARAM "+i+": '"+entry+"'. type=|"+p._sqlType+"|, val=|"+p._val+"|, isOutParam="+p._isOutput+".");
//			retList.add(p);
//		}
//		return retList;
	}
}

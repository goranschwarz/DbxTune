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
package com.asetune.central.controllers.ud.chart;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.Configuration;
import com.asetune.utils.OpenSslAesUtil;

public abstract class UserDefinedChartAbstract
implements IUserDefinedChart
{
	private static Logger _logger = Logger.getLogger(UserDefinedChartAbstract.class);

	public static final String PROPKEY_name             = "name";
	public static final String PROPKEY_chartType        = "chartType";
	public static final String PROPKEY_description      = "description";
	public static final String PROPKEY_refresh          = "refresh";
	public static final String PROPKEY_dbms_sql         = "dbms.sql";
	public static final String PROPKEY_dbms_username    = "dbms.username";
	public static final String PROPKEY_dbms_password    = "dbms.password";
	public static final String PROPKEY_dbms_servername  = "dbms.servername";
	public static final String PROPKEY_dbms_dbname      = "dbms.dbname";
	public static final String PROPKEY_dbms_url         = "dbms.url";
	
	private String    _name           ;
	private ChartType _chartType      ;
	private String    _description    ;
	private int       _refresh        ;
	private String    _dbms_sql       ;
	private String    _dbms_username  ;
	private String    _dbms_password  ;
	private String    _dbms_servername;
	private String    _dbms_dbname    ;
	private String    _dbms_url       ;
	
	private Configuration _conf;
	private boolean       _isValid;
	private String        _content;
	private Map<String, String> _urlParameterMap;
	
	@Override
	public boolean isValid()
	{
		return _isValid;
	}

	public Configuration getConfig()
	{
		return _conf;
	}

	@Override public String    getName()           { return _name; }
	@Override public ChartType getChartType()      { return _chartType; }
	@Override public String    getDescription()    { return _description; }
	@Override public String    getConfigFilename() { return _conf.getFilename(); }
	          public int       getRefresh()        { return _refresh; }
	          public String    getDbmsSql()        { return _dbms_sql; }
	          public String    getDbmsUsername()   { return _dbms_username; }
	          public String    getDbmsPassword()   { return _dbms_password; }
	@Override public String    getDbmsServerName() { return _dbms_servername; }
	          public String    getDbmsDbname()     { return _dbms_dbname; }
	          public String    getDbmsUrl()        { return _dbms_url; }

	          public void setName              (String name           ) { _name            = name; }
	          public void setDescription       (String description    ) { _description     = description; }
	          public void setRefresh           (int    refresh        ) { _refresh         = refresh; }
	          public void setDbmsSql           (String dbms_sql       ) { _dbms_sql        = dbms_sql; }
	          public void setDbmsUsername      (String dbms_username  ) { _dbms_username   = dbms_username; }
	          public void setDbmsPassword      (String dbms_password  ) { _dbms_password   = dbms_password; }
	          public void setDbmsServername    (String dbms_servername) { _dbms_servername = dbms_servername; }
	          public void setDbmsDbname        (String dbms_dbname    ) { _dbms_dbname     = dbms_dbname; }
	          public void setDbmsUrl           (String dbms_url       ) { _dbms_url        = dbms_url; }

	

	/**
	 * Constructor
	 */
	public UserDefinedChartAbstract(Configuration conf)
	throws Exception
	{
		_conf = conf;
		init(conf);
	}
		
	
	
	public void init(Configuration conf)
	throws Exception
	{
		_name            = conf.getMandatoryProperty(PROPKEY_name           );
		_chartType       = ChartType.fromString(conf.getMandatoryProperty(PROPKEY_chartType));
		_description     = conf.getProperty         (PROPKEY_description    , null);
		_refresh         = conf.getIntProperty      (PROPKEY_refresh        , -1);
		_dbms_sql        = conf.getMandatoryProperty(PROPKEY_dbms_sql       );
		_dbms_username   = conf.getMandatoryProperty(PROPKEY_dbms_username  );
		_dbms_password   = conf.getProperty         (PROPKEY_dbms_password  , null);
		_dbms_servername = conf.getProperty         (PROPKEY_dbms_servername, null);
		_dbms_dbname     = conf.getProperty         (PROPKEY_dbms_dbname    , null);
		_dbms_url        = conf.getMandatoryProperty(PROPKEY_dbms_url       );

		_isValid = true;

		if (_name != null && _name.equalsIgnoreCase("FROM_FILENAME"))
		{
			_name = getFromFileName(0, conf.getFilename());  // 0 == indexPos, logicalPos is 1 
		}

		if (_dbms_servername != null && _dbms_servername.equalsIgnoreCase("FROM_FILENAME"))
		{
			_dbms_servername = getFromFileName(1, conf.getFilename());  // 1 == indexPos, logicalPos is 2 
		}

		if (_dbms_dbname != null && _dbms_dbname.equalsIgnoreCase("FROM_FILENAME"))
		{
			_dbms_dbname = getFromFileName(2, conf.getFilename());  // 2 == indexPos, logicalPos is 3
		}

		// Get password (if PROPKEY_dbms_servername is specified)
		if (_dbms_password == null)
		{
			// Note: generate a passwd in linux: echo 'thePasswd' | openssl enc -aes-128-cbc -a -salt -pass:sybase
			_dbms_password = OpenSslAesUtil.readPasswdFromFile(_dbms_username, _dbms_servername);
		}
		
		// Replace '${dbname}' from the DBMS URL
		if (_dbms_url.contains("${dbname}"))
		{
			_dbms_url = _dbms_url.replace("${dbname}", _dbms_dbname);
		}

		// Replace '${srvName}' from the DBMS URL
		if (_dbms_url.contains("${srvName}"))
		{
			_dbms_url = _dbms_url.replace("${srvName}", _dbms_servername);
		}

		// Replace '${ifile-hostname}', '${ifile-port}' from the DBMS URL
		if (_dbms_url.contains("${ifile-hostname}") || _dbms_url.contains("${ifile-port}"))
		{
			String iFileHostname = AseConnectionFactory.getIFirstHost(_dbms_servername);
			int    iFilePort     = AseConnectionFactory.getIFirstPort(_dbms_servername);
			
			if (iFileHostname == null)
			{
				String msg = "Cant find the server name '" + _dbms_servername + "' in the Sybase Name Server File '" + AseConnectionFactory.getIFileName() + "'. Can't continue.";
				_logger.error(msg);
				//throw new Exception(msg);
				_description = msg;
				_isValid = false;
			}
			else
			{
				if (_dbms_url.contains("${ifile-hostname}")) _dbms_url = _dbms_url.replace("${ifile-hostname}", iFileHostname);
				if (_dbms_url.contains("${ifile-port}"    )) _dbms_url = _dbms_url.replace("${ifile-port}",     iFilePort + "");
			}
		}
	}

	/**
	 * Split the file name on '.' and get the string from the index position 'pos'  
	 * @param pos       index position (starting at 0)
	 * @param filename
	 * @return 
	 * @throws IndexOutOfBoundsException if we can't find the pos.
	 */
	private String getFromFileName(int pos, final String filename)
	{
		File f = new File(filename);
		String fn = f.getName();
		String[] sa = fn.split("\\.");
		
		if (sa.length < pos)
			throw new IndexOutOfBoundsException("Trying to find array pos " + pos + " on split by '.' from the string '" + filename + "'. The array.length=" + sa.length + " is < pos=" + pos);
		
		return sa[pos];
	}


	@Override
	public List<String> getJaveScriptList()
	{
		return Collections.emptyList();
	}

	/**
	 * Replaces any single-quote strings (') with \x27
	 * @param str
	 * @return
	 */
	protected String escapeJsQuote(String str)
	{
		if (str == null)
			return str;
		return str.replace("'", "\\x27");
	}

	@Override
	public String getContent()
	{
		return _content;
	}

	/**
	 * Set content
	 * 
	 * @param content
	 */
	public void setContent(String content)
	{
		_content = content;
	}

	
	@Override
	public void setUrlParameters(Map<String, String> parameterMap)
	{
		_urlParameterMap = parameterMap;
	}

	@Override
	public Map<String, String> getUrlParameters()
	{
		return _urlParameterMap != null ? _urlParameterMap : Collections.emptyMap();
	}
	
	/**
	 * Connect via JDBC to the configured DBMS URL. using: getDbmsUrl(), getDbmsUsername(), getDbmsPassword()
	 * @return
	 * @throws Exception
	 */
	protected DbxConnection dbmsConnect()
	throws Exception
	{
		DbxConnection conn = null;

		String jdbcUrl  = getDbmsUrl();
		String jdbcUser = getDbmsUsername();
		String jdbcPass = getDbmsPassword();
		
		if (jdbcPass == null)
			jdbcPass = "";

		ConnectionProp cp = new ConnectionProp();
		cp.setUrl(jdbcUrl);
		cp.setUsername(jdbcUser);
		cp.setPassword(jdbcPass);
		cp.setAppName(Version.getAppName());

		_logger.info("User Defined Content: Connecting to URL='" + jdbcUrl + "', username='" + jdbcUser + "'.");

		conn = DbxConnection.connect(null, cp);

		return conn;
	}
}

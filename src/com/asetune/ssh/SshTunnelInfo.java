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
package com.asetune.ssh;

import java.util.LinkedHashMap;
import java.util.Map;

import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class SshTunnelInfo
{
	private boolean _localPortGenerated = false;
	private int     _localPort    = -1;
	private String  _localHost    = "localhost";

	private String  _destHost     = null;
	private int     _destPort     = -1;

	private String  _sshHost      = null;
	private int     _sshPort      = -1;
	private String  _sshUsername  = null;
	private String  _sshPassword  = null;
	private String  _sshKeyFile   = null;
	
	private String  _sshInitOsCmd = null;
	
	private String nullToBlank  (String str) { return str == null ? "" : str; }
	private String nullStrToNull(String str) { return "null".equalsIgnoreCase(str) ? null : str; }

	public boolean isLocalPortGenerated() { return             _localPortGenerated; }
	public int    getLocalPort()          { return             _localPort;     }
	public String getLocalHost()          { return nullToBlank(_localHost);    }
	public String getDestHost()           { return nullToBlank(_destHost);     }
	public int    getDestPort()           { return             _destPort;      }
	public String getSshHost()            { return nullToBlank(_sshHost);      }
	public int    getSshPort()            { return             _sshPort;       }
	public String getSshUsername()        { return nullToBlank(_sshUsername);  }
	public String getSshPassword()        { return nullToBlank(_sshPassword);  }
	public String getSshInitOsCmd()       { return nullToBlank(_sshInitOsCmd); }
	public String getSshKeyFile()         { return nullToBlank(_sshKeyFile  ); }

	public void setLocalPortGenerated(boolean localPortGenerated) { this._localPortGenerated =                localPortGenerated; }
	public void setLocalPort         (int localPort)              { this._localPort          =                localPort;     }
	public void setLocalHost         (String localHost)           { this._localHost          = nullStrToNull( localHost   ); }
	public void setDestHost          (String destHost)            { this._destHost           = nullStrToNull( destHost    ); }
	public void setDestPort          (int destPort)               { this._destPort           =                destPort;      }
	public void setSshHost           (String sshHost)             { this._sshHost            = nullStrToNull( sshHost     ); }
	public void setSshPort           (int sshPort)                { this._sshPort            =                sshPort;       }
	public void setSshUsername       (String sshUsername)         { this._sshUsername        = nullStrToNull( sshUsername ); }
	public void setSshPassword       (String sshPassword)         { this._sshPassword        = nullStrToNull( sshPassword ); }
	public void setSshKeyFile        (String sshKeyFile)          { this._sshKeyFile         = nullStrToNull( sshKeyFile  ); }
	public void setSshInitOsCmd      (String initOsCmd)           { this._sshInitOsCmd       = nullStrToNull( initOsCmd   ); }

	public boolean isValid()
	{
		if (!_localPortGenerated && _localPort <= 0) return false;

		if (StringUtil.isNullOrBlank(_destHost))     return false;
		if (_destPort <= 0)                          return false;

		if (StringUtil.isNullOrBlank(_sshHost))      return false;
		if (_sshPort <= 0)                           return false;
		if (StringUtil.isNullOrBlank(_sshUsername))  return false;
		if (StringUtil.isNullOrBlank(_sshPassword) && StringUtil.isNullOrBlank(_sshKeyFile))  return false;
		
		return true;
	}

	public String getInvalidReason()
	{
		if (!_localPortGenerated && _localPort <= 0) return "Local Port can't be less than 0";

		if (StringUtil.isNullOrBlank(_destHost))     return "Destination Host is blank";
		if (_destPort <= 0)                          return "Destination Port can't be less than 0";

		if (StringUtil.isNullOrBlank(_sshHost))      return "SSH Host is blank";
		if (_sshPort <= 0)                           return "SSH Port can't be less than 0";
		if (StringUtil.isNullOrBlank(_sshUsername))  return "SSH Username is blank";
		if (StringUtil.isNullOrBlank(_sshPassword) && StringUtil.isNullOrBlank(_sshKeyFile))  return "SSH Password AND KeyFile is blank";

		return "";
	}

	public String getInfoString()
	{
//		boolean generateLocalPort = isLocalPortGenerated();
		int    localPort    = getLocalPort();
		String destHost     = getDestHost();
		int    destPort     = getDestPort();
		String sshHost      = getSshHost();
		int    sshPort      = getSshPort();
		String sshUser      = getSshUsername();
//		String sshPass      = getSshPassword();
		String sshKeyFile   = getSshKeyFile();
		String sshInitOsCmd = getSshInitOsCmd();

		return
			"LocalPort="     + localPort      + ", " +
			"DestHost='"     + destHost       + ":" + destPort  + "', " +
			"SshHost='"      + sshHost        + ":" + sshPort   + "', " +
			"SshUser='"      + sshUser        + "', " +
			"SshKeyFile='"   + sshKeyFile     + "', " +
			"SshInitOsCmd='" + sshInitOsCmd   + "'.";
	}
	
	public String getConfigString(boolean hidePasswd)
	{
		return getConfigString(hidePasswd, false);
	}
	public String getConfigString(boolean hidePasswd, boolean passwdInPlainText)
	{
		LinkedHashMap<String, String> cfg = new LinkedHashMap<String, String>();
		
		cfg.put("isLocalPortGenerated", isLocalPortGenerated()+"");
		cfg.put("LocalPort",            (isLocalPortGenerated() ? -1 : getLocalPort())+"");
		cfg.put("LocalHost",            getLocalHost());
		cfg.put("DestHost",             getDestHost());
		cfg.put("DestPort",             getDestPort()+"");
		cfg.put("SshHost",              getSshHost());
		cfg.put("SshPort",              getSshPort()+"");
		cfg.put("SshUsername",          getSshUsername());
		cfg.put("SshPassword",          hidePasswd ? "**secret**" : passwdInPlainText ? getSshPassword() : Configuration.encryptPropertyValue("SshPassword", getSshPassword()));
		cfg.put("SshKeyFile",           getSshKeyFile());
		cfg.put("SshInitOsCmd",         getSshInitOsCmd());
		
		return StringUtil.toCommaStr(cfg);
	}
	
	public static SshTunnelInfo parseConfigString(String cfgStr)
	{
		if (StringUtil.isNullOrBlank(cfgStr))
			return null;

//System.out.println("----");
//System.out.println("SshTunnelInfo.parseConfigString(): cfgStr='"+cfgStr+"'.");
		Map<String, String> cfg = StringUtil.parseCommaStrToMap(cfgStr);
//System.out.println("SshTunnelInfo.parseConfigString(): cfgMap='"+StringUtil.toCommaStr(cfg)+"'.");
		
		SshTunnelInfo ti = new SshTunnelInfo();
		
		     ti.setLocalPortGenerated( "true".equalsIgnoreCase(cfg.get("isLocalPortGenerated")) );
		try{ ti.setLocalPort(Integer.parseInt(cfg.get("LocalPort"))); } catch(NumberFormatException nfe) {}
		     ti.setLocalHost(                 cfg.get("LocalHost") );
		     ti.setDestHost(                  cfg.get("DestHost") );
		try{ ti.setDestPort( Integer.parseInt(cfg.get("DestPort"))); } catch(NumberFormatException nfe) {}
		     ti.setSshHost(                   cfg.get("SshHost") );
		try{ ti.setSshPort(  Integer.parseInt(cfg.get("SshPort"))); } catch(NumberFormatException nfe) {}
		     ti.setSshUsername(               cfg.get("SshUsername") );
		     ti.setSshPassword(               Configuration.decryptPropertyValue("SshPassword", cfg.get("SshPassword")) );
		     ti.setSshKeyFile(                cfg.get("SshKeyFile") );
		     ti.setSshInitOsCmd(              cfg.get("SshInitOsCmd") );
//System.out.println("SshTunnelInfo.parseConfigString(): SshPassword='"+cfg.get("SshPassword")+"'.");
//System.out.println("SshTunnelInfo.parseConfigString(): returns='"+ti.getConfigString(false, true)+"'.");
		
		return ti;
	}
}

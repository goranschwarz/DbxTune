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
	
	private String  _sshInitOsCmd = null;
	

	public boolean isLocalPortGenerated() { return _localPortGenerated; }
	public int    getLocalPort()          { return _localPort;   }
	public String getLocalHost()          { return _localHost;   }
	public String getDestHost()           { return _destHost;    }
	public int    getDestPort()           { return _destPort;    }
	public String getSshHost()            { return _sshHost;     }
	public int    getSshPort()            { return _sshPort;     }
	public String getSshUsername()        { return _sshUsername; }
	public String getSshPassword()        { return _sshPassword; }
	public String getSshInitOsCmd()       { return _sshInitOsCmd;  }
	
	public void setLocalPortGenerated(boolean localPortGenerated) { this._localPortGenerated = localPortGenerated; }
	public void setLocalPort         (int localPort)              { this._localPort          = localPort;   }
	public void setLocalHost         (String localHost)           { this._localHost          = localHost;   }
	public void setDestHost          (String destHost)            { this._destHost           = destHost;    }
	public void setDestPort          (int destPort)               { this._destPort           = destPort;    }
	public void setSshHost           (String sshHost)             { this._sshHost            = sshHost;     }
	public void setSshPort           (int sshPort)                { this._sshPort            = sshPort;     }
	public void setSshUsername       (String sshUsername)         { this._sshUsername        = sshUsername; }
	public void setSshPassword       (String sshPassword)         { this._sshPassword        = sshPassword; }
	public void setSshInitOsCmd      (String initOsCmd)           { this._sshInitOsCmd       = initOsCmd;   }

	public boolean isValid()
	{
		if (!_localPortGenerated && _localPort <= 0) return false;

		if (StringUtil.isNullOrBlank(_destHost))     return false;
		if (_destPort <= 0)                          return false;

		if (StringUtil.isNullOrBlank(_sshHost))      return false;
		if (_sshPort <= 0)                           return false;
		if (StringUtil.isNullOrBlank(_sshUsername))  return false;
		if (StringUtil.isNullOrBlank(_sshPassword))  return false;
		
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
		if (StringUtil.isNullOrBlank(_sshPassword))  return "SSH Password is blank";

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
		String sshInitOsCmd = getSshInitOsCmd();

		return
			"LocalPort="     + localPort      + ", " +
			"DestHost='"     + destHost       + ":" + destPort  + "', " +
			"SshHost='"      + sshHost        + ":" + sshPort   + "', " +
			"SshUser='"      + sshUser        + "', " +
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
		     ti.setSshInitOsCmd(              cfg.get("SshInitOsCmd") );
//System.out.println("SshTunnelInfo.parseConfigString(): SshPassword='"+cfg.get("SshPassword")+"'.");
//System.out.println("SshTunnelInfo.parseConfigString(): returns='"+ti.getConfigString(false, true)+"'.");
		
		return ti;
	}
}

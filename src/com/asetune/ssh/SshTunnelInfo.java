package com.asetune.ssh;

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
	

	public boolean isLocalPortGenerated() { return _localPortGenerated; }
	public int    getLocalPort()          { return _localPort;   }
	public String getLocalHost()          { return _localHost;   }
	public String getDestHost()           { return _destHost;    }
	public int    getDestPort()           { return _destPort;    }
	public String getSshHost()            { return _sshHost;     }
	public int    getSshPort()            { return _sshPort;     }
	public String getSshUsername()        { return _sshUsername; }
	public String getSshPassword()        { return _sshPassword; }
	
	public void setLocalPortGenerated(boolean localPortGenerated) { this._localPortGenerated = localPortGenerated; }
	public void setLocalPort         (int localPort)              { this._localPort          = localPort;   }
	public void setLocalHost         (String localHost)           { this._localHost          = localHost;   }
	public void setDestHost          (String destHost)            { this._destHost           = destHost;    }
	public void setDestPort          (int destPort)               { this._destPort           = destPort;    }
	public void setSshHost           (String sshHost)             { this._sshHost            = sshHost;     }
	public void setSshPort           (int sshPort)                { this._sshPort            = sshPort;     }
	public void setSshUsername       (String sshUsername)         { this._sshUsername        = sshUsername; }
	public void setSshPassword       (String sshPassword)         { this._sshPassword        = sshPassword; }

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
}

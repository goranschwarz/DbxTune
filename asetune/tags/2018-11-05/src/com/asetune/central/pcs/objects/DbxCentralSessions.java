package com.asetune.central.pcs.objects;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"sessionStartTime", "status", "serverName", "onHostname", "productString", "versionString", "buildString", "collectorHostname", "collectorSampleInterval", "collectorCurrentUrl", "collectorInfoFile", "collectorIsLocal", "numOfSamples", "lastSampleTime", "lastSampleAgeInSec", "serverDescription", "serverExtraInfo"}, alphabetic = true)
public class DbxCentralSessions
{
	public static final int ST_DISABLED = 1;
	
	private Timestamp _sessionStartTime        ;
	private int       _status                  ;
	private String    _serverName              ;
	private String    _onHostname              ;
	private String    _productString           ;
	private String    _versionString           ;
	private String    _buildString             ;
	private String    _collectorHostname       ;
	private int       _collectorSampleInterval ;
	private String    _collectorCurrentUrl     ;
	private String    _collectorInfoFile       ;
	private int       _numOfSamples            ;
	private Timestamp _lastSampleTime          ;
	private String    _serverDescription       ;
	private String    _serverExtraInfo         ;
	private List<DbxGraphProperties> _graphProperties = null;

//	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
//	public String getRecid() { return UUID.randomUUID().toString();  }
	public Timestamp getSessionStartTime()       { return _sessionStartTime;        }
	public int    getStatus()                    { return _status;                  }
	public String getServerName()                { return _serverName;              }
	public String getOnHostname()                { return _onHostname;              }
	public String getProductString()             { return _productString;           }
	public String getVersionString()             { return _versionString;           }
	public String getBuildString()               { return _buildString;             }
	public String getCollectorHostname()         { return _collectorHostname;       }
	public int    getCollectorSampleInterval()   { return _collectorSampleInterval; }
	public String getCollectorCurrentUrl()       { return _collectorCurrentUrl;     }
	public String getCollectorInfoFile()         { return _collectorInfoFile;       }
	public int    getNumOfSamples()              { return _numOfSamples;            }
//	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
	public Timestamp getLastSampleTime()         { return _lastSampleTime;          }
	public String getServerDescription()         { return _serverDescription;       }
	public String getServerExtraInfo()           { return _serverExtraInfo;         }
//	public List<DbxGraphProperties> getGraphProperties() { return _graphProperties; }
	public List<DbxGraphProperties> getData()    { return _graphProperties;         }
	
	public boolean getCollectorIsLocal()
	{
		String thisHostname = "-unknown-";
		try
		{
			InetAddress addr = InetAddress.getLocalHost();
			thisHostname = addr.getCanonicalHostName();
		}
		catch (UnknownHostException ignore) {}

		return thisHostname.equalsIgnoreCase(getCollectorHostname());
	}

	public long getLastSampleAgeInSec()
	{
		if (_lastSampleTime == null)
			return -1;

		long age = (System.currentTimeMillis() - _lastSampleTime.getTime() ) / 1000;
		if (age < -1)
			age = Math.abs(age);

		return age;
	}

	/** 
	 * Check if we have a specific status enabled.
	 * @param status
	 * @return
	 */
	public boolean hasStatus(int status)
	{
		return (_status & status) == status;
	}
	
	public void setSessionStartTime       (Timestamp sessionStartTime       ) { _sessionStartTime        = sessionStartTime;  }
	public void setStatus                 (int       status                 ) { _status                  = status;            }
	public void setServerName             (String    serverName             ) { _serverName              = serverName;        }
	public void setOnHostname             (String    onHostname             ) { _onHostname              = onHostname;        }
	public void setProductString          (String    productString          ) { _productString           = productString;     }
	public void setVersionString          (String    versionString          ) { _versionString           = versionString;     }
	public void setBuildString            (String    buildString            ) { _buildString             = buildString;       }
	public void setCollectorHostname      (String    collectorHostname      ) { _collectorHostname       = collectorHostname; }
	public void setCollectorSampleInterval(int       collectorSampleInterval) { _collectorSampleInterval = collectorSampleInterval; }
	public void setCollectorCurrentUrl    (String    collectorCurrentUrl    ) { _collectorCurrentUrl     = collectorCurrentUrl; }
	public void setCollectorInfoFile      (String    collectorInfoFile      ) { _collectorInfoFile       = collectorInfoFile; }
	public void setNumOfSamples           (int       numOfSamples           ) { _numOfSamples            = numOfSamples;      }
	public void setLastSampleTime         (Timestamp lastSampleTime         ) { _lastSampleTime          = lastSampleTime;    }
	public void setServerDescription      (String    serverDescription      ) { _serverDescription       = serverDescription; }
	public void setServerExtraInfo        (String    serverExtraInfo        ) { _serverExtraInfo         = serverExtraInfo;   }
	public void setGraphProperties        (List<DbxGraphProperties> gp      ) { _graphProperties         = gp;                }

	public DbxCentralSessions(Timestamp sessionStartTime, int status, String serverName, String onHostname, String productString, String versionString, String buildString, String collectorHostname, int collectorSampleInterval, String collectorCurrentUrl, String collectorInfoFile, int numOfSamples, Timestamp lastSampleTime, String serverDescription, String serverExtraInfo, List<DbxGraphProperties> graphProperties)
	{
		super();

		_sessionStartTime        = sessionStartTime;
		_status                  = status;
		_serverName              = serverName;
		_onHostname              = onHostname;
		_productString           = productString;
		_versionString           = versionString;
		_buildString             = buildString;
		_collectorHostname       = collectorHostname;
		_collectorSampleInterval = (collectorSampleInterval <= 0) ? -1 : collectorSampleInterval;
		_collectorCurrentUrl     = collectorCurrentUrl;
		_collectorInfoFile       = collectorInfoFile;
		_numOfSamples            = numOfSamples;
		_lastSampleTime          = lastSampleTime;
		_graphProperties         = graphProperties;
		_serverDescription       = serverDescription;
		_serverExtraInfo         = serverExtraInfo;  
	}
}

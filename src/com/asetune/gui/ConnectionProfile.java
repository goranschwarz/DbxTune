package com.asetune.gui;

import org.apache.log4j.Logger;

import com.asetune.ssh.SshTunnelInfo;
import com.asetune.utils.PropPropEntry;

public class ConnectionProfile
{
	private static Logger _logger = Logger.getLogger(ConnectionProfile.class);
	private static final long serialVersionUID = 1L;

	// ASE, IQ, SA, OpenServer... any connection info using jConnect, which uses TDS Tabular Data Stream
	private String        _tdsUsername      = null;
	private String        _tdsPassword      = null;
	private boolean       _tdsSavePassword  = true; // Should this be here???
	private String        _tdsServer        = null;
	private String        _tdsHost          = null;
	private int           _tdsPort          = -1;
	private String        _tdsDbname        = null;
	private int           _tdsLoginTimout   = -1;
	private SshTunnelInfo _tdsShhTunnelInfo = null;
	private String        _tdsSqlInit       = null;
	private String        _tdsUrlOptions    = null;
	private boolean       _tdsUseUrl        = false;
	private String        _tdsUseUrlStr     = null; // actual URL if above is true

	// AseTune options
	private boolean       _asetuneOptRecordSession          = false;
	private boolean       _asetuneOptOsMonitoring           = false;
	private boolean       _asetuneOptConnectAtStartup       = false; // should the be in here???
	private boolean       _asetuneOptReconnectOnLostConn    = true;
	private boolean       _asetuneOptConnectLater           = false; // should the be in here???
	private String        _asetuneOptConnectLaterHour       = null;  // should the be in here???
	private String        _asetuneOptConnectLaterMinute     = null;  // should the be in here???
	private boolean       _asetuneOptDissConnectLater       = false; // should the be in here???
	private String        _asetuneOptDissConnectLaterHour   = null;  // should the be in here???
	private String        _asetuneOptDissConnectLaterMinute = null;  // should the be in here???

	
	// HostMonitor
	private String        _osMonUsername      = null;
	private String        _osMonPassword      = null;
	private boolean       _osMonSavePassword  = true; // Should this be here???
	private String        _osMonHost          = null;
	private int           _osMonPort          = -1;

	// Recordings
	private String        _pcsWriterClass        = null;
	private String        _pcsWriterDriver            = null;
	private String        _pcsWriterUrl               = null;
	private String        _pcsWriterUsername          = null;
	private String        _pcsWriterPassword          = null;
	private boolean       _pcsWriterStartH2asNwServer = true;
	private boolean       _pcsWriterDdlLookup         = true;
	private boolean       _pcsWriterDdlStoreDependantObjects = true;
	private int           _pcsWriterDdlLookupSleepTime       = -1;
	private PropPropEntry _pcsWriterCounterDetailes          = null;

	// Load Recordings
	private String        _pcsReaderDriver             = null;
	private String        _pcsReaderUrl                = null;
	private String        _pcsReaderUsername           = null;
	private String        _pcsReaderPassword           = null;
	private boolean       _pcsReaderCheckForNewSamples = true;
	private boolean       _pcsReaderStartH2asNwServer  = true;
	
	// JDBC
	private String        _jdbcDriver             = null;
	private String        _jdbcUrl                = null;
	private String        _jdbcUsername           = null;
	private String        _jdbcPassword           = null;
	private String        _jdbcSqlInit            = null;
	private String        _jdbcUrlOptions         = null; // should the be in here???
	private SshTunnelInfo _jdbcShhTunnelInfo      = null; // NOT YET IMPLEMENTED

	//---------------------------------------------------------------
	// BEGIN: Constructors
	//---------------------------------------------------------------
	public ConnectionProfile()
	{
	}
	//---------------------------------------------------------------
	// END: Constructors
	//---------------------------------------------------------------

	
}

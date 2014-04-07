package com.asetune.gui;

import org.apache.log4j.Logger;

import com.asetune.ssh.SshTunnelInfo;
import com.asetune.utils.PropPropEntry;

public class ConnectionProfile
{
	private static Logger _logger = Logger.getLogger(ConnectionProfile.class);
	private static final long serialVersionUID = 1L;

	public enum Type
	{
		TDS, JDBC
	};

	private String    _name      = null;
	private Type      _type      = null;

	private TdsEntry  _tdsEntry  = null;
	private JdbcEntry _jdbcEntry = null;
	
	//---------------------------------------------------------------
	// BEGIN: Constructors
	//---------------------------------------------------------------
	public ConnectionProfile()
	{
	}
	//---------------------------------------------------------------
	// END: Constructors
	//---------------------------------------------------------------


	public String toHtmlString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html>");
		sb.append("</html>");
		
		return sb.toString();
	}

	
	//---------------------------------------------------------------
	// BEGIN: subclasses
	//---------------------------------------------------------------
	public static class TdsEntry
	{
		// ASE, IQ, SA, OpenServer... any connection info using jConnect, which uses TDS Tabular Data Stream
		public String        _tdsUsername      = null;
		public String        _tdsPassword      = null;
		public boolean       _tdsSavePassword  = true; // Should this be here???
		public String        _tdsServer        = null;
//		public String        _tdsHost          = null;
//		public int           _tdsPort          = -1;
		public String        _tdsHosts         = null;
		public String        _tdsPorts         = null;
		public String        _tdsDbname        = null;
		public int           _tdsLoginTimout   = -1;
		public SshTunnelInfo _tdsShhTunnelInfo = null;
		public String        _tdsClientCharset = null;
		public String        _tdsSqlInit       = null;
		public String        _tdsUrlOptions    = null;
		public boolean       _tdsUseUrl        = false;
		public String        _tdsUseUrlStr     = null; // actual URL if above is true

		// AseTune options
		public boolean       _asetuneOptRecordSession          = false;
		public boolean       _asetuneOptOsMonitoring           = false;
		public boolean       _asetuneOptConnectAtStartup       = false; // should the be in here???
		public boolean       _asetuneOptReconnectOnLostConn    = true;
		public boolean       _asetuneOptConnectLater           = false; // should the be in here???
		public String        _asetuneOptConnectLaterHour       = null;  // should the be in here???
		public String        _asetuneOptConnectLaterMinute     = null;  // should the be in here???
		public boolean       _asetuneOptDissConnectLater       = false; // should the be in here???
		public String        _asetuneOptDissConnectLaterHour   = null;  // should the be in here???
		public String        _asetuneOptDissConnectLaterMinute = null;  // should the be in here???

		
		// HostMonitor
		public String        _osMonUsername      = null;
		public String        _osMonPassword      = null;
		public boolean       _osMonSavePassword  = true; // Should this be here???
		public String        _osMonHost          = null;
		public int           _osMonPort          = -1;

		// Recordings
		public String        _pcsWriterClass        = null;
		public String        _pcsWriterDriver            = null;
		public String        _pcsWriterUrl               = null;
		public String        _pcsWriterUsername          = null;
		public String        _pcsWriterPassword          = null;
		public boolean       _pcsWriterStartH2asNwServer = true;
		public boolean       _pcsWriterDdlLookup         = true;
		public boolean       _pcsWriterDdlStoreDependantObjects = true;
		public int           _pcsWriterDdlLookupSleepTime       = -1;
		public PropPropEntry _pcsWriterCounterDetailes          = null;

		// Load Recordings
		public String        _pcsReaderDriver             = null;
		public String        _pcsReaderUrl                = null;
		public String        _pcsReaderUsername           = null;
		public String        _pcsReaderPassword           = null;
		public boolean       _pcsReaderCheckForNewSamples = true;
		public boolean       _pcsReaderStartH2asNwServer  = true;
		
	}

	public static class JdbcEntry
	{
		// JDBC
		public String        _jdbcDriver             = null;
		public String        _jdbcUrl                = null;
		public String        _jdbcUsername           = null;
		public String        _jdbcPassword           = null;
		public String        _jdbcSqlInit            = null;
		public String        _jdbcUrlOptions         = null; // should the be in here???
		public SshTunnelInfo _jdbcShhTunnelInfo      = null; // NOT YET IMPLEMENTED

	}

	public static class OfflineEntry
	{
		public String        _jdbcDriver             = null;
		public String        _jdbcUrl                = null;
		public String        _jdbcUsername           = null;
		public String        _jdbcPassword           = null;

		public boolean       _checkForNewSessions    = false;
		public boolean       _H2Option_startH2NwSrv  = true;
	}

	//---------------------------------------------------------------
	// END: subclasses
	//---------------------------------------------------------------


}

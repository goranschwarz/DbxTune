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
	public static interface ConnProfileEntry
	{
		public String toXml();
	}
	
	public static class TdsEntry
	implements ConnProfileEntry
	{
		// ASE, IQ, SA, OpenServer... any connection info using jConnect, which uses TDS Tabular Data Stream
		public String        _tdsIfile         = null;
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

		@Override
		public String toXml()
		{
			StringBuilder sb = new StringBuilder();

			return sb.toString();
		}
	}
	//---------------------------------------------------
	// Below is XML tags
	//---------------------------------------------------
	private static final String       XML_CONN_PROF_ENTRIES = "ConnectionProfileEntries";
	
	private static final String       XML_TDS_ENTRY         = "TdsEntry";
	private static final String       XML_JDBC_ENTRY        = "JdbcEntry";
	private static final String       XML_OFFLINE_ENTRY     = "OfflineEntry";

	private static final String       XML_TDS_USERNAME          = "Username";
	private static final String       XML_TDS_PASSWORD          = "Password";
	private static final String       XML_TDS_SAVE_PASSWORD     = "SavePassword";

	private static final String       XML_TDS_SERVER_NAME       = "ServerName";
	private static final String       XML_TDS_HOST_LIST         = "HostList";
	private static final String       XML_TDS_PORT_LIST         = "PortList";
	private static final String       XML_TDS_Dbname            = "Dbname";      //??????
	private static final String       XML_TDS_LoginTimout       = "LoginTimout";
	private static final String       XML_TDS_ShhTunnelInfo     = "ShhTunnelInfo";
	private static final String       XML_TDS_ClientCharset     = "ClientCharset";
	private static final String       XML_TDS_SqlInit           = "SqlInit";
	private static final String       XML_TDS_UrlOptions        = "UrlOptions";
	private static final String       XML_TDS_UseRawUrl         = "UseRawUrl";
	private static final String       XML_TDS_UseRawUrlStr      = "UseRawUrlStr";
	
//	private static final String       XML_TDS_XXXXXX            = "Xxxxxx";
//	private static final String       XML_TDS_XXXXXX            = "Xxxxxx";
//	private static final String       XML_TDS_XXXXXX            = "Xxxxxx";

	private static final String       XML_TDS_ASETUNE_OptRecordSession            = "OptRecordSession";
	private static final String       XML_TDS_ASETUNE_OptOsMonitoring            = "OptOsMonitoring";
	private static final String       XML_TDS_ASETUNE_OptConnectAtStartup            = "OptConnectAtStartup";
	private static final String       XML_TDS_ASETUNE_OptReconnectOnLostConn            = "OptReconnectOnLostConn";
	private static final String       XML_TDS_ASETUNE_OptConnectLater            = "OptConnectLater";
	private static final String       XML_TDS_ASETUNE_OptConnectLaterHour            = "OptConnectLaterHour";
	private static final String       XML_TDS_ASETUNE_OptConnectLaterMinute            = "OptConnectLaterMinute";
	private static final String       XML_TDS_ASETUNE_OptDissConnectLater            = "OptDissConnectLater";
	private static final String       XML_TDS_ASETUNE_OptDissConnectLaterHour            = "OptDissConnectLaterHour";
	private static final String       XML_TDS_ASETUNE_OptDissConnectLaterMinute            = "OptDissConnectLaterMinute";

	private static final String       XML_TDS_ASETUNE_osMonUsername            = "osMonUsername";
	private static final String       XML_TDS_ASETUNE_osMonPassword            = "osMonPassword";
	private static final String       XML_TDS_ASETUNE_osMonSavePassword            = "osMonSavePassword";
	private static final String       XML_TDS_ASETUNE_osMonHost            = "osMonHost";
	private static final String       XML_TDS_ASETUNE_osMonPort            = "osMonPort";
	private static final String       XML_TDS_ASETUNE_            = "Xxxxxx";

	private static final String       XML_TAG_COMMAND_HISTORY_LIST  = "CommandHistoryList";
	private static final String       XML_TAG_COMMAND_HISTORY_ENTRY = "CommandHistoryEntry";
	private static final String       XML_SUBTAG_SERVER_NAME        = "ServerName";
	private static final String       XML_SUBTAG_USER_NAME          = "UserName";
	private static final String       XML_SUBTAG_DB_NAME            = "DbName";
	private static final String       XML_SUBTAG_EXEC_TIME          = "ExecTime";
	private static final String       XML_SUBTAG_UUID               = "UUID";
	private static final String       XML_SUBTAG_SOURCE             = "Source";
	private static final String       XML_SUBTAG_COMMAND            = "Command";


	public static class JdbcEntry
	implements ConnProfileEntry
	{
		// JDBC
		public String        _jdbcDriver             = null;
		public String        _jdbcUrl                = null;
		public String        _jdbcUsername           = null;
		public String        _jdbcPassword           = null;
		public String        _jdbcSqlInit            = null;
		public String        _jdbcUrlOptions         = null; // should the be in here???
		public SshTunnelInfo _jdbcShhTunnelInfo      = null; // NOT YET IMPLEMENTED

		@Override
		public String toXml()
		{
			StringBuilder sb = new StringBuilder();

			return sb.toString();
		}
	}

	public static class OfflineEntry
	implements ConnProfileEntry
	{
		public String        _jdbcDriver             = null;
		public String        _jdbcUrl                = null;
		public String        _jdbcUsername           = null;
		public String        _jdbcPassword           = null;

		public boolean       _checkForNewSessions    = false;
		public boolean       _H2Option_startH2NwSrv  = true;

		@Override
		public String toXml()
		{
			StringBuilder sb = new StringBuilder();

			return sb.toString();
		}
	}

	//---------------------------------------------------------------
	// END: subclasses
	//---------------------------------------------------------------


}

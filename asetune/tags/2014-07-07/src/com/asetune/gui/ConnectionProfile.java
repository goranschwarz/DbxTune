package com.asetune.gui;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.ssh.SshTunnelInfo;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;

public class ConnectionProfile
{
	private static Logger _logger = Logger.getLogger(ConnectionProfile.class);
	public enum Type
	{
		TDS, 
		JDBC, 
		OFFLINE
	};

	public enum SrvType 
	{ 
		TDS_ASE, 
		TDS_ASA, 
		TDS_IQ, 
		TDS_RS, 
		TDS_OTHER, 
		TDS_UNDEFINED, // if profile was saved without ever have been connected to

		JDBC_HANA,
		JDBC_H2,
		JDBC_HSQL,
		JDBC_MSSQL,
		JDBC_ORACLE,
		JDBC_DB2_UX,
		JDBC_DB2_ZOS,
		JDBC_MYSQL,
		JDBC_DERBY,
		JDBC_OTHER,
		JDBC_UNDEFINED; // if profile was saved without ever have been connected to
	}

	private String       _name         = null;
	private Type         _type         = null;
	private SrvType      _srvType      = null;

	private TdsEntry     _tdsEntry     = null;
	private JdbcEntry    _jdbcEntry    = null;
	private OfflineEntry _offlineEntry = null;
	
	public static SrvType getServerType(Type type, String productName)
	{
		if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DB2_UX    )) return SrvType.JDBC_DB2_UX;
		if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DB2_ZOS   )) return SrvType.JDBC_DB2_ZOS;
		if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DERBY     )) return SrvType.JDBC_DERBY;
		if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_H2        )) return SrvType.JDBC_H2;
		if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_HANA      )) return SrvType.JDBC_HANA;
		if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_HSQL      )) return SrvType.JDBC_HSQL;
		if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MSSQL     )) return SrvType.JDBC_MSSQL;
		if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MYSQL     )) return SrvType.JDBC_MYSQL;
		if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_ORACLE    )) return SrvType.JDBC_ORACLE;
		
		if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_ASA)) return SrvType.TDS_ASA;
		if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_ASE)) return SrvType.TDS_ASE;
		if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_IQ )) return SrvType.TDS_IQ;
		if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_RS )) return SrvType.TDS_RS;

		if (Type.TDS.equals(type))
		{
//			if (StringUtil.isNullOrBlank(productName)) 
//					return SrvType.TDS_UNDEFINED;
			return SrvType.TDS_OTHER;
		}

//		if (StringUtil.isNullOrBlank(productName))
//			return SrvType.JDBC_UNDEFINED;
		return SrvType.JDBC_OTHER;
	}


	//---------------------------------------------------------------
	// BEGIN: Constructors
	//---------------------------------------------------------------
	public ConnectionProfile(String name, Type type, SrvType srvType, ConnProfileEntry entry)
	{
		_name    = name;
		_type    = type;
		_srvType = srvType;
		
		if      (entry instanceof TdsEntry)     _tdsEntry     = (TdsEntry) entry;
		else if (entry instanceof JdbcEntry)    _jdbcEntry    = (JdbcEntry) entry;
		else if (entry instanceof OfflineEntry) _offlineEntry = (OfflineEntry) entry;
		else
			throw new RuntimeException("Unknown ConnectionProfile Entry Object. entry="+entry.getClass().getName());
	}

	public ConnectionProfile(String name, String type, String srvType, ConnProfileEntry entry)
	{
		_name    = name;
		_type    = Type.valueOf(type);
		_srvType = SrvType.valueOf(srvType);
		
		if      (entry instanceof TdsEntry)     _tdsEntry     = (TdsEntry) entry;
		else if (entry instanceof JdbcEntry)    _jdbcEntry    = (JdbcEntry) entry;
		else if (entry instanceof OfflineEntry) _offlineEntry = (OfflineEntry) entry;
		else
			throw new RuntimeException("Unknown ConnectionProfile Entry Object. entry="+entry.getClass().getName());
	}

	public ConnectionProfile(String name, String productName, ConnProfileEntry entry)
	{
		_name    = name;
		
		if      (entry instanceof TdsEntry)     { _type = Type.TDS;     _tdsEntry     = (TdsEntry) entry; }
		else if (entry instanceof JdbcEntry)    { _type = Type.JDBC;    _jdbcEntry    = (JdbcEntry) entry; }
		else if (entry instanceof OfflineEntry) { _type = Type.OFFLINE; _offlineEntry = (OfflineEntry) entry; }
		else
			throw new RuntimeException("Unknown ConnectionProfile Entry Object. entry="+entry.getClass().getName());

		_srvType = getServerType(_type, productName);
	}


	public ConnectionProfile copy(ConnectionProfile connProfile, String newName)
	{
		return new ConnectionProfile(newName, connProfile.getType(), connProfile.getSrvType(), connProfile.getEntry());
	}

	//---------------------------------------------------------------
	// END: Constructors
	//---------------------------------------------------------------

	public void setName(String name) { _name = name; }

	public String  getKey()     { return getEntry().getKey(); }
	public String  getName()    { return _name; }
	public Type    getType()    { return _type; }
	public SrvType getSrvType() { return _srvType; }

	public boolean isType   (Type    type)    { return type.equals(_type); }
	public boolean isSrvType(SrvType srvType) { return srvType.equals(_srvType); }


	public void setSrvTypeUnknown()
	{
		if (_type == Type.TDS)
			_srvType = SrvType.TDS_UNDEFINED;
		else
			_srvType = SrvType.JDBC_UNDEFINED;
	}

	public void setSrvType(String productName)
	{
		_srvType = getServerType(_type, productName);
	}


	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof ConnProfileEntry)
			return getDifference( (ConnProfileEntry) obj ) == null;
		return super.equals(obj);
	}

	public String getDifference(ConnProfileEntry cpe)
	{
		if (cpe instanceof TdsEntry && _tdsEntry != null)
		{
			return _tdsEntry.getDifference(cpe);
		}
		else if (cpe instanceof JdbcEntry && _jdbcEntry != null)
		{
			return _jdbcEntry.getDifference(cpe);
		}
		else if (cpe instanceof OfflineEntry && _offlineEntry != null)
		{
			return _offlineEntry.getDifference(cpe);
		}
		return null;
	}

	
	
	public TdsEntry getTdsEntry()         { return _tdsEntry; }
	public JdbcEntry getJdbcEntry()       { return _jdbcEntry; }
	public OfflineEntry getOfflineEntry() { return _offlineEntry; }
	
	public ConnProfileEntry getEntry()
	{
		if (_tdsEntry     != null) return _tdsEntry;
		if (_jdbcEntry    != null) return _jdbcEntry;
		if (_offlineEntry != null) return _offlineEntry;

		throw new IllegalStateException("No Entry has been assigned. _tdsEntry==null, _jdbcEntry==null, _offlineEntry==null");
	}

	public void setEntry(ConnProfileEntry entry)
	{
//		throw new IllegalStateException("No Entry has been assigned. _tdsEntry==null, _jdbcEntry==null, _offlineEntry==null");

		if      (entry instanceof TdsEntry    ) { if (_type != Type.TDS    ) new IllegalStateException("Passed entry is TdsEntry, but current Type is '"+_type+"'."); }
		else if (entry instanceof JdbcEntry   ) { if (_type != Type.JDBC   ) new IllegalStateException("Passed entry is JdbcEntry, but current Type is '"+_type+"'."); }
		else if (entry instanceof OfflineEntry) { if (_type != Type.OFFLINE) new IllegalStateException("Passed entry is OfflineEntry, but current Type is '"+_type+"'."); }
		else
			throw new RuntimeException("Unknown ConnectionProfile Entry Object. entry="+entry.getClass().getName());

		if      (entry instanceof TdsEntry)     _tdsEntry     = (TdsEntry) entry;
		else if (entry instanceof JdbcEntry)    _jdbcEntry    = (JdbcEntry) entry;
		else if (entry instanceof OfflineEntry) _offlineEntry = (OfflineEntry) entry;
	}

	public String toXml()
	{
		if (_tdsEntry     != null) return _tdsEntry    .toXml(_name, _type, _srvType);
		if (_jdbcEntry    != null) return _jdbcEntry   .toXml(_name, _type, _srvType);
		if (_offlineEntry != null) return _offlineEntry.toXml(_name, _type, _srvType);
		return "";
	}

	
	@Override
	public String toString()
	{
		return getName();
	}

	
	public String toHtmlString()
	{
		return getEntry().toHtml(_name, _type, _srvType);
	}

	public String getToolTipText()
	{
		return toHtmlString();
	}

	
	//---------------------------------------------------
	// Below is XML tags
	//---------------------------------------------------
	public static final String       XML_CONN_PROF_ENTRIES  = "ConnectionProfileManager";
	
	public static final String       XML_CONN_PROF_ATTR_NAME     = "name";
	public static final String       XML_CONN_PROF_ATTR_TYPE     = "type";
	public static final String       XML_CONN_PROF_ATTR_SRV_TYPE = "srvType";

	public static final String       XML_TDS_ENTRY          = "TdsEntry";
	public static final String       XML_JDBC_ENTRY         = "JdbcEntry";
	public static final String       XML_OFFLINE_ENTRY      = "OfflineEntry";

	//-------------------------------------------------------------------------------------------------------------------
	// TDS
	public static final String       XML_TDS_USERNAME                            = "Username";
	public static final String       XML_TDS_PASSWORD                            = "Password";
	public static final String       XML_TDS_SAVE_PASSWORD                       = "SavePassword";
	public static final String       XML_TDS_NW_ENCRYPT_PASSWORD                 = "EncryptPasswordOverNetwork";

	public static final String       XML_TDS_IFILE                               = "InterfacesFile";
	public static final String       XML_TDS_SERVER_NAME                         = "ServerName";
	public static final String       XML_TDS_HOST_LIST                           = "HostList";
	public static final String       XML_TDS_PORT_LIST                           = "PortList";
	public static final String       XML_TDS_Dbname                              = "Dbname";      //??????
	public static final String       XML_TDS_LoginTimout                         = "LoginTimout";
	public static final String       XML_TDS_ShhTunnelUse                        = "ShhTunnelUse";
	public static final String       XML_TDS_ShhTunnelInfo                       = "ShhTunnelInfo";
	public static final String       XML_TDS_ClientCharset                       = "ClientCharset";
	public static final String       XML_TDS_SqlInit                             = "SqlInit";
	public static final String       XML_TDS_UrlOptions                          = "UrlOptions";
	public static final String       XML_TDS_UseRawUrl                           = "UseRawUrl";
	public static final String       XML_TDS_UseRawUrlStr                        = "UseRawUrlStr";

	public static final String       XML_TDS_IsAseTuneParamsValid                = "IsAseTuneParamsValid";

	public static final String       XML_TDS_ASETUNE_UseTemplate                 = "UseTemplate";
	public static final String       XML_TDS_ASETUNE_UseTemplateName             = "UseTemplateName";

	public static final String       XML_TDS_ASETUNE_OptRecordSession            = "OptRecordSession";
	public static final String       XML_TDS_ASETUNE_OptOsMonitoring             = "OptOsMonitoring";
	public static final String       XML_TDS_ASETUNE_OptConnectAtStartup         = "OptConnectAtStartup";
	public static final String       XML_TDS_ASETUNE_OptReconnectOnLostConn      = "OptReconnectOnLostConn";
	public static final String       XML_TDS_ASETUNE_OptConnectLater             = "OptConnectLater";
	public static final String       XML_TDS_ASETUNE_OptConnectLaterHour         = "OptConnectLaterHour";
	public static final String       XML_TDS_ASETUNE_OptConnectLaterMinute       = "OptConnectLaterMinute";
	public static final String       XML_TDS_ASETUNE_OptDissConnectLater         = "OptDissConnectLater";
	public static final String       XML_TDS_ASETUNE_OptDissConnectLaterHour     = "OptDissConnectLaterHour";
	public static final String       XML_TDS_ASETUNE_OptDissConnectLaterMinute   = "OptDissConnectLaterMinute";

	public static final String       XML_TDS_ASETUNE_osMonUsername               = "osMonUsername";
	public static final String       XML_TDS_ASETUNE_osMonPassword               = "osMonPassword";
	public static final String       XML_TDS_ASETUNE_osMonSavePassword           = "osMonSavePassword";
	public static final String       XML_TDS_ASETUNE_osMonHost                   = "osMonHost";
	public static final String       XML_TDS_ASETUNE_osMonPort                   = "osMonPort";

	public static final String       XML_TDS_ASETUNE_pcsWriterClass                    = "pcsWriterClass";
	public static final String       XML_TDS_ASETUNE_pcsWriterDriver                   = "pcsWriterDriver";
	public static final String       XML_TDS_ASETUNE_pcsWriterUrl                      = "pcsWriterUrl";
	public static final String       XML_TDS_ASETUNE_pcsWriterUsername                 = "pcsWriterUsername";
	public static final String       XML_TDS_ASETUNE_pcsWriterPassword                 = "pcsWriterPassword";
	public static final String       XML_TDS_ASETUNE_pcsWriterSavePassword             = "pcsWriterSavePassword";
	public static final String       XML_TDS_ASETUNE_pcsWriterStartH2asNwServer        = "pcsWriterStartH2asNwServer";
	public static final String       XML_TDS_ASETUNE_pcsWriterDdlLookup                = "pcsWriterDdlLookup";
	public static final String       XML_TDS_ASETUNE_pcsWriterDdlStoreDependantObjects = "pcsWriterDdlStoreDependantObjects";
	public static final String       XML_TDS_ASETUNE_pcsWriterDdlLookupSleepTime       = "pcsWriterDdlLookupSleepTime";
	public static final String       XML_TDS_ASETUNE_pcsWriterCounterDetailes          = "pcsWriterCounterDetailes";
	
//	public static final String       XML_TDS_ASETUNE_pcsReaderDriver             = "pcsReaderDriver";
//	public static final String       XML_TDS_ASETUNE_pcsReaderUrl                = "pcsReaderUrl";
//	public static final String       XML_TDS_ASETUNE_pcsReaderUsername           = "pcsReaderUsername";
//	public static final String       XML_TDS_ASETUNE_pcsReaderPassword           = "pcsReaderPassword";
//	public static final String       XML_TDS_ASETUNE_pcsReaderCheckForNewSamples = "pcsReaderCheckForNewSamples";
//	public static final String       XML_TDS_ASETUNE_pcsReaderStartH2asNwServer  = "pcsReaderStartH2asNwServer";


	//-------------------------------------------------------------------------------------------------------------------
	// JDBC
	public static final String       XML_JDBC_DRIVER                              = "Driver";
	public static final String       XML_JDBC_URL                                 = "URL";
	public static final String       XML_JDBC_USERNAME                            = "Username";
	public static final String       XML_JDBC_PASSWORD                            = "Password";
	public static final String       XML_JDBC_SAVE_PASSWORD                       = "SavePassword";

	public static final String       XML_JDBC_SqlInit                             = "SqlInit";
	public static final String       XML_JDBC_UrlOptions                          = "UrlOptions";
	public static final String       XML_JDBC_ShhTunnelInfo                       = "ShhTunnelInfo";

	
	//-------------------------------------------------------------------------------------------------------------------
	// OFFLINE
	public static final String       XML_OFFLINE_DRIVER                           = "Driver";
	public static final String       XML_OFFLINE_URL                              = "URL";
	public static final String       XML_OFFLINE_USERNAME                         = "Username";
	public static final String       XML_OFFLINE_PASSWORD                         = "Password";
	public static final String       XML_OFFLINE_SAVE_PASSWORD                    = "SavePassword";

	public static final String       XML_OFFLINE_checkForNewSessions              = "checkForNewSessions";
	public static final String       XML_OFFLINE_H2Option_startH2NwSrv            = "H2Option_startH2NwSrv";

	
	//---------------------------------------------------------------
	// BEGIN: subclasses
	//---------------------------------------------------------------
	public static interface ConnProfileEntry
	{
		public String toXml (String name, Type type, SrvType srvType);
		public String toHtml(String name, Type type, SrvType srvType);
		public String getDifference(ConnProfileEntry entry);
		public String getKey();
	}
	
	public static class TdsEntry
	implements ConnProfileEntry
	{
		public String        _key              = null;

		// ASE, IQ, SA, OpenServer... any connection info using jConnect, which uses TDS Tabular Data Stream
		public String        _tdsIfile         = null;
		public String        _tdsUsername      = null;
		public String        _tdsPassword      = null;
		public boolean       _tdsSavePassword  = true;
		public boolean       _tdsNwEncryptPasswd = true;
		public String        _tdsServer        = null;
		public String        _tdsHosts         = null;
		public String        _tdsPorts         = null;
		public String        _tdsDbname        = null;
		public int           _tdsLoginTimout   = AseConnectionFactory.DEFAULT_LOGINTIMEOUT;
		public boolean       _tdsShhTunnelUse  = false;
		public SshTunnelInfo _tdsShhTunnelInfo = null;
		public String        _tdsClientCharset = null;
		public String        _tdsSqlInit       = null;
		public String        _tdsUrlOptions    = null;
		public boolean       _tdsUseUrl        = false;
		public String        _tdsUseUrlStr     = null; // actual URL if above is true

		// If below parameters is valid and should be read written to the XML file
		public boolean       _isAseTuneParamsValid   = false; 

		public boolean       _asetuneUseTemplate     = false;
		public String        _asetuneUseTemplateName = null;

		// AseTune options
		public boolean       _asetuneOptRecordSession          = false;
		public boolean       _asetuneOptOsMonitoring           = false;
		public boolean       _asetuneOptConnectAtStartup       = false;
		public boolean       _asetuneOptReconnectOnLostConn    = true;
		public boolean       _asetuneOptConnectLater           = false;
		public String        _asetuneOptConnectLaterHour       = null; 
		public String        _asetuneOptConnectLaterMinute     = null; 
		public boolean       _asetuneOptDissConnectLater       = false;
		public String        _asetuneOptDissConnectLaterHour   = null; 
		public String        _asetuneOptDissConnectLaterMinute = null; 

		// HostMonitor
		public String        _osMonUsername      = null;
		public String        _osMonPassword      = null;
		public boolean       _osMonSavePassword  = true;
		public String        _osMonHost          = null;
		public int           _osMonPort          = 22;

		// Recordings
		public String        _pcsWriterClass             = null;
		public String        _pcsWriterDriver            = null;
		public String        _pcsWriterUrl               = null;
		public String        _pcsWriterUsername          = null;
		public String        _pcsWriterPassword          = null;
		public boolean       _pcsWriterSavePassword      = true;
		public boolean       _pcsWriterStartH2asNwServer = true;
		public boolean       _pcsWriterDdlLookup                = PersistentCounterHandler.DEFAULT_ddl_doDdlLookupAndStore;
		public boolean       _pcsWriterDdlStoreDependantObjects = PersistentCounterHandler.DEFAULT_ddl_addDependantObjectsToDdlInQueue;
		public int           _pcsWriterDdlLookupSleepTime       = PersistentCounterHandler.DEFAULT_ddl_afterDdlLookupSleepTimeInMs;
//		public PropPropEntry _pcsWriterCounterDetailes          = null;


		public TdsEntry(String key)
		{
			_key = key;
		}

		@Override
		public String getKey()
		{
			return _key;
		}

		/**
		 * if the previously stored profile IS an AseTune Profile<br>
		 * and we are using SQLWindow, we need to copy the "hidden" fields into the passed ConnectionProfileEntry<br>
		 * otherwise we will lose that data the next time we use the entry from AseTune
		 * 
		 * @param fromEntry entry to copy from
		 */
		public void copyAseTuneParams(TdsEntry fromEntry)
		{
//System.out.println("copyAseTuneParams: from '"+fromEntry+"', to this '"+this+"'.");
			_isAseTuneParamsValid              = fromEntry._isAseTuneParamsValid; 

			_asetuneUseTemplate                = fromEntry._asetuneUseTemplate;
			_asetuneUseTemplateName            = fromEntry._asetuneUseTemplateName;

			// AseTune options
			_asetuneOptRecordSession           = fromEntry._asetuneOptRecordSession;
			_asetuneOptOsMonitoring            = fromEntry._asetuneOptOsMonitoring;
			_asetuneOptConnectAtStartup        = fromEntry._asetuneOptConnectAtStartup;
			_asetuneOptReconnectOnLostConn     = fromEntry._asetuneOptReconnectOnLostConn;
			_asetuneOptConnectLater            = fromEntry._asetuneOptConnectLater;
			_asetuneOptConnectLaterHour        = fromEntry._asetuneOptConnectLaterHour; 
			_asetuneOptConnectLaterMinute      = fromEntry._asetuneOptConnectLaterMinute; 
			_asetuneOptDissConnectLater        = fromEntry._asetuneOptDissConnectLater;
			_asetuneOptDissConnectLaterHour    = fromEntry._asetuneOptDissConnectLaterHour; 
			_asetuneOptDissConnectLaterMinute  = fromEntry._asetuneOptDissConnectLaterMinute; 

			// HostMonitor
			_osMonUsername                     = fromEntry._osMonUsername;
			_osMonPassword                     = fromEntry._osMonPassword;
			_osMonSavePassword                 = fromEntry._osMonSavePassword;
			_osMonHost                         = fromEntry._osMonHost;
			_osMonPort                         = fromEntry._osMonPort;

			// Recordings
			_pcsWriterClass                    = fromEntry._pcsWriterClass;
			_pcsWriterDriver                   = fromEntry._pcsWriterDriver;
			_pcsWriterUrl                      = fromEntry._pcsWriterUrl;
			_pcsWriterUsername                 = fromEntry._pcsWriterUsername;
			_pcsWriterPassword                 = fromEntry._pcsWriterPassword;
			_pcsWriterSavePassword             = fromEntry._pcsWriterSavePassword;
			_pcsWriterStartH2asNwServer        = fromEntry._pcsWriterStartH2asNwServer;
			_pcsWriterDdlLookup                = fromEntry._pcsWriterDdlLookup;
			_pcsWriterDdlStoreDependantObjects = fromEntry._pcsWriterDdlStoreDependantObjects;
			_pcsWriterDdlLookupSleepTime       = fromEntry._pcsWriterDdlLookupSleepTime;
			
		}

		//		@Override
//		public boolean equals(Object obj)
//		{
//			// FIXME: implement this BETTER
//			if (obj instanceof TdsEntry)
//			{
////				String thisXml =            this.toXml("equals-dummy", Type.TDS, SrvType.TDS_OTHER);
////				String objXml  = ((TdsEntry)obj).toXml("equals-dummy", Type.TDS, SrvType.TDS_OTHER);
////				return thisXml.equals(objXml);
//				
//				TdsEntry entry = ((TdsEntry)obj);
//				String diffStr = getDifference(entry);
//				return diffStr == null;
//			}
//
//			return super.equals(obj);
//		}

		@Override
		public String getDifference(ConnProfileEntry profileEntry)
		{
			TdsEntry entry = (TdsEntry) profileEntry;
			
			StringBuilder sb = new StringBuilder();
			
			htmlTabRowIfChanged(sb, XML_TDS_IFILE              , _tdsIfile          , entry._tdsIfile);
			htmlTabRowIfChanged(sb, XML_TDS_USERNAME           , _tdsUsername       , entry._tdsUsername);
			if (_tdsSavePassword || entry._tdsSavePassword)
				htmlTabRowIfChangedPwd(sb, XML_TDS_PASSWORD    , _tdsPassword       , entry._tdsPassword);
			htmlTabRowIfChanged(sb, XML_TDS_SAVE_PASSWORD      , _tdsSavePassword   , entry._tdsSavePassword);
			htmlTabRowIfChanged(sb, XML_TDS_NW_ENCRYPT_PASSWORD, _tdsNwEncryptPasswd, entry._tdsNwEncryptPasswd);
			htmlTabRowIfChanged(sb, XML_TDS_SERVER_NAME        , _tdsServer         , entry._tdsServer);
			htmlTabRowIfChanged(sb, XML_TDS_HOST_LIST          , _tdsHosts          , entry._tdsHosts);
			htmlTabRowIfChanged(sb, XML_TDS_PORT_LIST          , _tdsPorts          , entry._tdsPorts);
			htmlTabRowIfChanged(sb, XML_TDS_Dbname             , _tdsDbname         , entry._tdsDbname);
			htmlTabRowIfChanged(sb, XML_TDS_LoginTimout        , _tdsLoginTimout    , entry._tdsLoginTimout);
			htmlTabRowIfChanged(sb, XML_TDS_ShhTunnelUse       , _tdsShhTunnelUse   , entry._tdsShhTunnelUse);
			htmlTabRowIfChanged(sb, XML_TDS_ShhTunnelInfo      , _tdsShhTunnelInfo == null ? "" : _tdsShhTunnelInfo.getConfigString(false),  entry._tdsShhTunnelInfo == null ? "" : entry._tdsShhTunnelInfo.getConfigString(false));
			htmlTabRowIfChanged(sb, XML_TDS_ClientCharset      , _tdsClientCharset  , entry._tdsClientCharset);
			htmlTabRowIfChanged(sb, XML_TDS_SqlInit            , _tdsSqlInit        , entry._tdsSqlInit);
			htmlTabRowIfChanged(sb, XML_TDS_UrlOptions         , _tdsUrlOptions     , entry._tdsUrlOptions);
			htmlTabRowIfChanged(sb, XML_TDS_UseRawUrl          , _tdsUseUrl         , entry._tdsUseUrl);
			if (_tdsUseUrl || entry._tdsUseUrl)
				htmlTabRowIfChanged(sb, XML_TDS_UseRawUrlStr   , _tdsUseUrlStr      , entry._tdsUseUrlStr);

			if (_isAseTuneParamsValid || entry._isAseTuneParamsValid)
			{
				htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_UseTemplate              , _asetuneUseTemplate              , entry._asetuneUseTemplate);
				if (_asetuneUseTemplate || entry._asetuneUseTemplate)
					htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_UseTemplateName      , _asetuneUseTemplateName          , entry._asetuneUseTemplateName);
				htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_OptRecordSession         , _asetuneOptRecordSession         , entry._asetuneOptRecordSession);
				htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_OptOsMonitoring          , _asetuneOptOsMonitoring          , entry._asetuneOptOsMonitoring);
				htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_OptConnectAtStartup      , _asetuneOptConnectAtStartup      , entry._asetuneOptConnectAtStartup);
				htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_OptReconnectOnLostConn   , _asetuneOptReconnectOnLostConn   , entry._asetuneOptReconnectOnLostConn);
				htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_OptConnectLater          , _asetuneOptConnectLater          , entry._asetuneOptConnectLater);
				htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_OptConnectLaterHour      , _asetuneOptConnectLaterHour      , entry._asetuneOptConnectLaterHour);
				htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_OptConnectLaterMinute    , _asetuneOptConnectLaterMinute    , entry._asetuneOptConnectLaterMinute);
				htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_OptDissConnectLater      , _asetuneOptDissConnectLater      , entry._asetuneOptDissConnectLater);
				htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_OptDissConnectLaterHour  , _asetuneOptDissConnectLaterHour  , entry._asetuneOptDissConnectLaterHour);
				htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_OptDissConnectLaterMinute, _asetuneOptDissConnectLaterMinute, entry._asetuneOptDissConnectLaterMinute);

				if (_asetuneOptRecordSession || entry._asetuneOptRecordSession)
				{
					htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_pcsWriterClass                   , _pcsWriterClass   , entry._pcsWriterClass);
					htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_pcsWriterDriver                  , _pcsWriterDriver  , entry._pcsWriterDriver);
					htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_pcsWriterUrl                     , _pcsWriterUrl     , entry._pcsWriterUrl);
					htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_pcsWriterUsername                , _pcsWriterUsername, entry._pcsWriterUsername);
					if (_pcsWriterSavePassword)
						htmlTabRowIfChangedPwd(sb, XML_TDS_ASETUNE_pcsWriterPassword         , _pcsWriterPassword                , entry._pcsWriterPassword);
					htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_pcsWriterSavePassword            , _pcsWriterSavePassword            , entry._pcsWriterSavePassword);
					htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_pcsWriterStartH2asNwServer       , _pcsWriterStartH2asNwServer       , entry._pcsWriterStartH2asNwServer);
					htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_pcsWriterDdlLookup               , _pcsWriterDdlLookup               , entry._pcsWriterDdlLookup);
					htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_pcsWriterDdlStoreDependantObjects, _pcsWriterDdlStoreDependantObjects, entry._pcsWriterDdlStoreDependantObjects);
					htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_pcsWriterDdlLookupSleepTime      , _pcsWriterDdlLookupSleepTime      , entry._pcsWriterDdlLookupSleepTime);
				}

				if (_asetuneOptOsMonitoring || entry._asetuneOptOsMonitoring)
				{
					htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_osMonUsername    , _osMonUsername    , entry._osMonUsername);
					if (_osMonSavePassword || entry._osMonSavePassword)
						htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_osMonPassword, _osMonPassword    , entry._osMonPassword);
					htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_osMonSavePassword, _osMonSavePassword, entry._osMonSavePassword);
					htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_osMonHost        , _osMonHost        , entry._osMonHost);
					htmlTabRowIfChanged(sb, XML_TDS_ASETUNE_osMonPort        , _osMonPort        , entry._osMonPort);
				}
			}

			if (sb.length() > 0)
			{
				sb.insert(0, "<tr> <td nowrap bgcolor=\"#848484\"><font color=\"white\"><b>Attribute</b></font></td> <td nowrap bgcolor=\"#848484\"><font color=\"white\"><b>New Value</b></font></td> <td nowrap bgcolor=\"#848484\"><font color=\"white\"><b>Old Value</b></font></td> </tr>");
				sb.insert(0, "<table border=1 cellspacing=0 cellpadding=1>");
				sb.append(   "</table>");

				return sb.toString();
			}
			return null;
		}

		@Override
		public String toHtml(String name, Type type, SrvType srvType)
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append("<html>");

			sb.append("Connection Profile:<br>");
			sb.append("<table border=0 cellspacing=1 cellpadding=1>");
			htmlTabRow(sb, "Name:",        name);
			htmlTabRow(sb, "Type:",        type);
			htmlTabRow(sb, "Server Type:", srvType);
			sb.append("</table>");
			sb.append("<hr>");

			sb.append("<table border=0 cellspacing=1 cellpadding=1>");
			htmlTabRow(sb, XML_TDS_IFILE              , _tdsIfile);
			htmlTabRow(sb, XML_TDS_USERNAME           , _tdsUsername);
			if (_tdsSavePassword)
				htmlTabRow(sb, XML_TDS_PASSWORD       , "**secret**");
			htmlTabRow(sb, XML_TDS_SAVE_PASSWORD      , _tdsSavePassword);
			htmlTabRow(sb, XML_TDS_NW_ENCRYPT_PASSWORD, _tdsNwEncryptPasswd);
			htmlTabRow(sb, XML_TDS_SERVER_NAME        , _tdsServer);
			htmlTabRow(sb, XML_TDS_HOST_LIST          , _tdsHosts);
			htmlTabRow(sb, XML_TDS_PORT_LIST          , _tdsPorts);
			htmlTabRow(sb, XML_TDS_Dbname             , _tdsDbname);
			htmlTabRow(sb, XML_TDS_LoginTimout        , _tdsLoginTimout);
			htmlTabRow(sb, XML_TDS_ShhTunnelUse       , _tdsShhTunnelUse);
			htmlTabRow(sb, XML_TDS_ShhTunnelInfo      , _tdsShhTunnelInfo == null ? "" : _tdsShhTunnelInfo.getConfigString(true));
			htmlTabRow(sb, XML_TDS_ClientCharset      , _tdsClientCharset);
			htmlTabRow(sb, XML_TDS_SqlInit            , _tdsSqlInit);
			htmlTabRow(sb, XML_TDS_UrlOptions         , _tdsUrlOptions);
			htmlTabRow(sb, XML_TDS_UseRawUrl          , _tdsUseUrl);
			if (_tdsUseUrl)
				htmlTabRow(sb, XML_TDS_UseRawUrlStr   , _tdsUseUrlStr);
			sb.append("</table>");

			if (_isAseTuneParamsValid)
			{
				sb.append("<h3>AseTune specific settings</h3>");
				sb.append("<hr>");
				sb.append("<table border=0 cellspacing=1 cellpadding=1>");
				htmlTabRow(sb, XML_TDS_ASETUNE_UseTemplate, _asetuneUseTemplate   );
				if (_asetuneUseTemplate)
					htmlTabRow(sb, XML_TDS_ASETUNE_UseTemplateName      , _asetuneUseTemplateName);
				htmlTabRow(sb, XML_TDS_ASETUNE_OptRecordSession         , _asetuneOptRecordSession);
				htmlTabRow(sb, XML_TDS_ASETUNE_OptOsMonitoring          , _asetuneOptOsMonitoring);
				htmlTabRow(sb, XML_TDS_ASETUNE_OptConnectAtStartup      , _asetuneOptConnectAtStartup);
				htmlTabRow(sb, XML_TDS_ASETUNE_OptReconnectOnLostConn   , _asetuneOptReconnectOnLostConn);
				htmlTabRow(sb, XML_TDS_ASETUNE_OptConnectLater          , _asetuneOptConnectLater);
				htmlTabRow(sb, XML_TDS_ASETUNE_OptConnectLaterHour      , _asetuneOptConnectLaterHour);
				htmlTabRow(sb, XML_TDS_ASETUNE_OptConnectLaterMinute    , _asetuneOptConnectLaterMinute);
				htmlTabRow(sb, XML_TDS_ASETUNE_OptDissConnectLater      , _asetuneOptDissConnectLater);
				htmlTabRow(sb, XML_TDS_ASETUNE_OptDissConnectLaterHour  , _asetuneOptDissConnectLaterHour);
				htmlTabRow(sb, XML_TDS_ASETUNE_OptDissConnectLaterMinute, _asetuneOptDissConnectLaterMinute);
				sb.append("</table>");

				if (_asetuneOptRecordSession)
				{
					sb.append("<h3>Recording Database details</h3>");
					sb.append("<hr>");
					sb.append("<table border=0 cellspacing=1 cellpadding=1>");
					htmlTabRow(sb, XML_TDS_ASETUNE_pcsWriterClass                   , _pcsWriterClass);
					htmlTabRow(sb, XML_TDS_ASETUNE_pcsWriterDriver                  , _pcsWriterDriver);
					htmlTabRow(sb, XML_TDS_ASETUNE_pcsWriterUrl                     , _pcsWriterUrl);
					htmlTabRow(sb, XML_TDS_ASETUNE_pcsWriterUsername                , _pcsWriterUsername);
					if (_pcsWriterSavePassword)
						htmlTabRow(sb, XML_TDS_ASETUNE_pcsWriterPassword            , "**secret**");
					htmlTabRow(sb, XML_TDS_ASETUNE_pcsWriterSavePassword            , _pcsWriterSavePassword);
					htmlTabRow(sb, XML_TDS_ASETUNE_pcsWriterStartH2asNwServer       , _pcsWriterStartH2asNwServer);
					htmlTabRow(sb, XML_TDS_ASETUNE_pcsWriterDdlLookup               , _pcsWriterDdlLookup);
					htmlTabRow(sb, XML_TDS_ASETUNE_pcsWriterDdlStoreDependantObjects, _pcsWriterDdlStoreDependantObjects);
					htmlTabRow(sb, XML_TDS_ASETUNE_pcsWriterDdlLookupSleepTime      , _pcsWriterDdlLookupSleepTime);
					sb.append("</table>");
				}

				if (_asetuneOptOsMonitoring)
				{
					sb.append("<h3>Operating System Monitoring details</h3>");
					sb.append("<hr>");
					sb.append("<table border=0 cellspacing=1 cellpadding=1>");
					htmlTabRow(sb, XML_TDS_ASETUNE_osMonUsername    , _osMonUsername);
					if (_osMonSavePassword)
						htmlTabRow(sb, XML_TDS_ASETUNE_osMonPassword, "**secret**");
					htmlTabRow(sb, XML_TDS_ASETUNE_osMonSavePassword, _osMonSavePassword);
					htmlTabRow(sb, XML_TDS_ASETUNE_osMonHost        , _osMonHost);
					htmlTabRow(sb, XML_TDS_ASETUNE_osMonPort        , _osMonPort);
					sb.append("</table>");
				}
			}

			sb.append("</html>");
			
			return sb.toString();
		}

		@Override
		public String toXml(String name, Type type, SrvType srvType)
		{
			StringBuilder sb = new StringBuilder();

			StringUtil.xmlBeginTag(sb, 4, XML_TDS_ENTRY, 
					XML_CONN_PROF_ATTR_NAME,     name,
					XML_CONN_PROF_ATTR_TYPE,     type.toString(),
					XML_CONN_PROF_ATTR_SRV_TYPE, srvType.toString()
					);

			StringUtil.xmlTag(sb, 8, XML_TDS_IFILE,               _tdsIfile);
			StringUtil.xmlTag(sb, 8, XML_TDS_USERNAME,            _tdsUsername);
			if (_tdsSavePassword)
				StringUtil.xmlTag(sb, 8, XML_TDS_PASSWORD,        Configuration.encryptPropertyValue(XML_TDS_PASSWORD, _tdsPassword));
			StringUtil.xmlTag(sb, 8, XML_TDS_SAVE_PASSWORD,       _tdsSavePassword);
			StringUtil.xmlTag(sb, 8, XML_TDS_NW_ENCRYPT_PASSWORD, _tdsNwEncryptPasswd);
			StringUtil.xmlTag(sb, 8, XML_TDS_SERVER_NAME,         _tdsServer);
			StringUtil.xmlTag(sb, 8, XML_TDS_HOST_LIST,           _tdsHosts);
			StringUtil.xmlTag(sb, 8, XML_TDS_PORT_LIST,           _tdsPorts);
			StringUtil.xmlTag(sb, 8, XML_TDS_Dbname,              _tdsDbname);
			StringUtil.xmlTag(sb, 8, XML_TDS_LoginTimout,         _tdsLoginTimout);
			StringUtil.xmlTag(sb, 8, XML_TDS_ShhTunnelUse,        _tdsShhTunnelUse);
			StringUtil.xmlTag(sb, 8, XML_TDS_ShhTunnelInfo,       _tdsShhTunnelInfo == null ? "" : _tdsShhTunnelInfo.getConfigString(false));
			StringUtil.xmlTag(sb, 8, XML_TDS_ClientCharset,       _tdsClientCharset);
			StringUtil.xmlTag(sb, 8, XML_TDS_SqlInit,             _tdsSqlInit);
			StringUtil.xmlTag(sb, 8, XML_TDS_UrlOptions,          _tdsUrlOptions);
			StringUtil.xmlTag(sb, 8, XML_TDS_UseRawUrl,           _tdsUseUrl);
			if (_tdsUseUrl)
				StringUtil.xmlTag(sb, 8, XML_TDS_UseRawUrlStr,    _tdsUseUrlStr);

			// AseTune params
			StringUtil.xmlTag(sb, 8, XML_TDS_IsAseTuneParamsValid, _isAseTuneParamsValid);
			if (_isAseTuneParamsValid)
			{
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_UseTemplate,                 _asetuneUseTemplate);
				if (_asetuneUseTemplate)
					StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_UseTemplateName,         _asetuneUseTemplateName);
			
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_OptRecordSession,            _asetuneOptRecordSession);
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_OptOsMonitoring,             _asetuneOptOsMonitoring);
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_OptConnectAtStartup,         _asetuneOptConnectAtStartup);
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_OptReconnectOnLostConn,      _asetuneOptReconnectOnLostConn);
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_OptConnectLater,             _asetuneOptConnectLater);
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_OptConnectLaterHour,         _asetuneOptConnectLaterHour);
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_OptConnectLaterMinute,       _asetuneOptConnectLaterMinute);
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_OptDissConnectLater,         _asetuneOptDissConnectLater);
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_OptDissConnectLaterHour,     _asetuneOptDissConnectLaterHour);
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_OptDissConnectLaterMinute,   _asetuneOptDissConnectLaterMinute);
			
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_osMonUsername,               _osMonUsername);
				if (_osMonSavePassword)
					StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_osMonPassword,           Configuration.encryptPropertyValue(XML_TDS_ASETUNE_osMonPassword, _osMonPassword));
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_osMonSavePassword,           _osMonSavePassword);
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_osMonHost,                   _osMonHost);
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_osMonPort,                   _osMonPort);
			
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_pcsWriterClass,                    _pcsWriterClass);
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_pcsWriterDriver,                   _pcsWriterDriver);
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_pcsWriterUrl,                      _pcsWriterUrl);
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_pcsWriterUsername,                 _pcsWriterUsername);
				if (_pcsWriterSavePassword)
					StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_pcsWriterPassword,              _pcsWriterPassword);
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_pcsWriterSavePassword,             _pcsWriterSavePassword);
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_pcsWriterStartH2asNwServer,        _pcsWriterStartH2asNwServer);
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_pcsWriterDdlLookup,                _pcsWriterDdlLookup);
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_pcsWriterDdlStoreDependantObjects, _pcsWriterDdlStoreDependantObjects);
				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_pcsWriterDdlLookupSleepTime,       _pcsWriterDdlLookupSleepTime);
//				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_pcsWriterCounterDetailes,          _pcsWriterCounterDetailes);

//				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_pcsReaderDriver,             _pcsReaderDriver);
//				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_pcsReaderUrl,                _pcsReaderUrl);
//				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_pcsReaderUsername,           _pcsReaderUsername);
//				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_pcsReaderPassword,           Configuration.encryptPropertyValue(XML_TDS_ASETUNE_pcsReaderPassword, _pcsReaderPassword));
//				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_pcsReaderCheckForNewSamples, _pcsReaderCheckForNewSamples);
//				StringUtil.xmlTag(sb, 8, XML_TDS_ASETUNE_pcsReaderStartH2asNwServer,  _pcsReaderStartH2asNwServer);
			}
			
			StringUtil.xmlEndTag(sb, 4, XML_TDS_ENTRY);
			return sb.toString();
		}

		public static ConnectionProfile parseXml(Element element)
		{
			// Attributes of the Element XML_TDS_ENTRY
			String name    = element.getAttribute(ConnectionProfile.XML_CONN_PROF_ATTR_NAME);
			String type    = element.getAttribute(ConnectionProfile.XML_CONN_PROF_ATTR_TYPE);
			String srvType = element.getAttribute(ConnectionProfile.XML_CONN_PROF_ATTR_SRV_TYPE);

			// Tags in the XML_TDS_ENTRY

			String hostList = getValue(element, XML_TDS_HOST_LIST);
			String portList = getValue(element, XML_TDS_PORT_LIST);
			String key = AseConnectionFactory.toHostPortStr(hostList, portList);
			TdsEntry entry = new TdsEntry(key);
			
			entry._tdsIfile           = getValue(element, XML_TDS_IFILE,               entry._tdsIfile);
			entry._tdsUsername        = getValue(element, XML_TDS_USERNAME,            entry._tdsUsername);
			entry._tdsSavePassword    = getValue(element, XML_TDS_SAVE_PASSWORD,       entry._tdsSavePassword);
			if (entry._tdsSavePassword)
				entry._tdsPassword    = getValue(element, XML_TDS_PASSWORD,            entry._tdsPassword);
			entry._tdsNwEncryptPasswd = getValue(element, XML_TDS_NW_ENCRYPT_PASSWORD, entry._tdsNwEncryptPasswd);
			entry._tdsServer          = getValue(element, XML_TDS_SERVER_NAME,         entry._tdsServer);
			entry._tdsHosts           = getValue(element, XML_TDS_HOST_LIST,           entry._tdsHosts);
			entry._tdsPorts           = getValue(element, XML_TDS_PORT_LIST,           entry._tdsPorts);
			entry._tdsDbname          = getValue(element, XML_TDS_Dbname,              entry._tdsDbname);
			entry._tdsLoginTimout     = getValue(element, XML_TDS_LoginTimout,         entry._tdsLoginTimout);
			entry._tdsShhTunnelUse    = getValue(element, XML_TDS_ShhTunnelUse,        entry._tdsShhTunnelUse);
			entry._tdsShhTunnelInfo   = SshTunnelInfo.parseConfigString(getValue(element, XML_TDS_ShhTunnelInfo));
			entry._tdsClientCharset   = getValue(element, XML_TDS_ClientCharset,       entry._tdsClientCharset);
			entry._tdsSqlInit         = getValue(element, XML_TDS_SqlInit,             entry._tdsSqlInit);
			entry._tdsUrlOptions      = getValue(element, XML_TDS_UrlOptions,          entry._tdsUrlOptions);
			entry._tdsUseUrl          = getValue(element, XML_TDS_UseRawUrl,           entry._tdsUseUrl);
			if (entry._tdsUseUrl)
				entry._tdsUseUrlStr   = getValue(element, XML_TDS_UseRawUrlStr,        entry._tdsUseUrlStr);

			entry._isAseTuneParamsValid = getValue(element, XML_TDS_IsAseTuneParamsValid, entry._isAseTuneParamsValid);
			if (entry._isAseTuneParamsValid)
			{
				entry._asetuneUseTemplate                = getValue(element, XML_TDS_ASETUNE_UseTemplate,     entry._asetuneUseTemplate);
				entry._asetuneUseTemplateName            = getValue(element, XML_TDS_ASETUNE_UseTemplateName, entry._asetuneUseTemplateName);
	
				// AseTune options
				entry._asetuneOptRecordSession           = getValue(element, XML_TDS_ASETUNE_OptRecordSession,          entry._asetuneOptRecordSession);
				entry._asetuneOptOsMonitoring            = getValue(element, XML_TDS_ASETUNE_OptOsMonitoring,           entry._asetuneOptOsMonitoring);
				entry._asetuneOptConnectAtStartup        = getValue(element, XML_TDS_ASETUNE_OptConnectAtStartup,       entry._asetuneOptConnectAtStartup); // should the be in here???
				entry._asetuneOptReconnectOnLostConn     = getValue(element, XML_TDS_ASETUNE_OptReconnectOnLostConn,    entry._asetuneOptReconnectOnLostConn);
				entry._asetuneOptConnectLater            = getValue(element, XML_TDS_ASETUNE_OptConnectLater,           entry._asetuneOptConnectLater); // should the be in here???
				entry._asetuneOptConnectLaterHour        = getValue(element, XML_TDS_ASETUNE_OptConnectLaterHour,       entry._asetuneOptConnectLaterHour);  // should the be in here???
				entry._asetuneOptConnectLaterMinute      = getValue(element, XML_TDS_ASETUNE_OptConnectLaterMinute,     entry._asetuneOptConnectLaterMinute);  // should the be in here???
				entry._asetuneOptDissConnectLater        = getValue(element, XML_TDS_ASETUNE_OptDissConnectLater,       entry._asetuneOptDissConnectLater); // should the be in here???
				entry._asetuneOptDissConnectLaterHour    = getValue(element, XML_TDS_ASETUNE_OptDissConnectLaterHour,   entry._asetuneOptDissConnectLaterHour);  // should the be in here???
				entry._asetuneOptDissConnectLaterMinute  = getValue(element, XML_TDS_ASETUNE_OptDissConnectLaterMinute, entry._asetuneOptDissConnectLaterMinute);  // should the be in here???
	
				// HostMonitor
				if (entry._asetuneOptOsMonitoring)
				{
					entry._osMonUsername                 = getValue(element, XML_TDS_ASETUNE_osMonUsername,     entry._osMonUsername);
					entry._osMonSavePassword             = getValue(element, XML_TDS_ASETUNE_osMonSavePassword, entry._osMonSavePassword); // Should this be here???
					if (entry._osMonSavePassword)
						entry._osMonPassword             = getValue(element, XML_TDS_ASETUNE_osMonPassword,     entry._osMonPassword);
					entry._osMonHost                     = getValue(element, XML_TDS_ASETUNE_osMonHost,         entry._osMonHost);
					entry._osMonPort                     = getValue(element, XML_TDS_ASETUNE_osMonPort,         entry._osMonPort);
				}
	
				// Recordings
				if (entry._asetuneOptRecordSession)
				{
					entry._pcsWriterClass                    = getValue(element, XML_TDS_ASETUNE_pcsWriterClass,                    entry._pcsWriterClass);
					entry._pcsWriterDriver                   = getValue(element, XML_TDS_ASETUNE_pcsWriterDriver,                   entry._pcsWriterDriver);
					entry._pcsWriterUrl                      = getValue(element, XML_TDS_ASETUNE_pcsWriterUrl,                      entry._pcsWriterUrl);
					entry._pcsWriterUsername                 = getValue(element, XML_TDS_ASETUNE_pcsWriterUsername,                 entry._pcsWriterUsername);
					entry._pcsWriterSavePassword             = getValue(element, XML_TDS_ASETUNE_pcsWriterSavePassword,             entry._pcsWriterSavePassword);
					if (entry._pcsWriterSavePassword)
						entry._pcsWriterPassword             = getValue(element, XML_TDS_ASETUNE_pcsWriterPassword,                 entry._pcsWriterPassword);
					entry._pcsWriterStartH2asNwServer        = getValue(element, XML_TDS_ASETUNE_pcsWriterStartH2asNwServer,        entry._pcsWriterStartH2asNwServer);
					entry._pcsWriterDdlLookup                = getValue(element, XML_TDS_ASETUNE_pcsWriterDdlLookup,                entry._pcsWriterDdlLookup);
					entry._pcsWriterDdlStoreDependantObjects = getValue(element, XML_TDS_ASETUNE_pcsWriterDdlStoreDependantObjects, entry._pcsWriterDdlStoreDependantObjects);
					entry._pcsWriterDdlLookupSleepTime       = getValue(element, XML_TDS_ASETUNE_pcsWriterDdlLookupSleepTime,       entry._pcsWriterDdlLookupSleepTime);
	//				entry._pcsWriterCounterDetailes          = getValue(element, XML_TDS_ASETUNE_pcsWriterCounterDetailes,          entry._pcsWriterCounterDetailes);
				}
				// Load Recordings
	//			entry._pcsReaderDriver                   = getValue(element, XML_TDS_ASETUNE_pcsReaderDriver,             entry._pcsReaderDriver);
	//			entry._pcsReaderUrl                      = getValue(element, XML_TDS_ASETUNE_pcsReaderUrl,                entry._pcsReaderUrl);
	//			entry._pcsReaderUsername                 = getValue(element, XML_TDS_ASETUNE_pcsReaderUsername,           entry._pcsReaderUsername);
	//			entry._pcsReaderPassword                 = getValue(element, XML_TDS_ASETUNE_pcsReaderPassword,           entry._pcsReaderPassword);
	//			entry._pcsReaderCheckForNewSamples       = getValue(element, XML_TDS_ASETUNE_pcsReaderCheckForNewSamples, entry._pcsReaderCheckForNewSamples);
	//			entry._pcsReaderStartH2asNwServer        = getValue(element, XML_TDS_ASETUNE_pcsReaderStartH2asNwServer,  entry._pcsReaderStartH2asNwServer);
			}

			ConnectionProfile connProfile = new ConnectionProfile(name, type, srvType, entry);
			return connProfile;
		}
	}

	public static class JdbcEntry
	implements ConnProfileEntry
	{
		// JDBC
		public String        _jdbcDriver             = null;
		public String        _jdbcUrl                = null;
		public String        _jdbcUsername           = null;
		public String        _jdbcPassword           = null;
		public boolean       _jdbcSavePassword       = true;
		public String        _jdbcSqlInit            = null;
		public String        _jdbcUrlOptions         = null; // should the be in here???
		public SshTunnelInfo _jdbcShhTunnelInfo      = null; // NOT YET IMPLEMENTED


		@Override
		public String getKey()
		{
			// TODO Auto-generated method stub
			return _jdbcUrl;
		}

//		@Override
//		public boolean equals(Object obj)
//		{
//			// FIXME: implement this BETTER
//			if (obj instanceof JdbcEntry)
//			{
//				String thisXml =             this.toXml("equals-dummy", Type.JDBC, SrvType.JDBC_OTHER);
//				String objXml  = ((JdbcEntry)obj).toXml("equals-dummy", Type.JDBC, SrvType.JDBC_OTHER);
//				return thisXml.equals(objXml);
//			}
//
//			return super.equals(obj);
//		}

		@Override
		public String getDifference(ConnProfileEntry profileEntry)
		{
			JdbcEntry entry = (JdbcEntry) profileEntry;
			
			StringBuilder sb = new StringBuilder();
			
			htmlTabRowIfChanged(sb, XML_JDBC_DRIVER         , _jdbcDriver,        entry._jdbcDriver);
			htmlTabRowIfChanged(sb, XML_JDBC_URL            , _jdbcUrl,           entry._jdbcUrl);
			htmlTabRowIfChanged(sb, XML_JDBC_USERNAME       , _jdbcUsername,      entry._jdbcUsername);
			if (_jdbcSavePassword)
				htmlTabRowIfChangedPwd(sb, XML_JDBC_PASSWORD, _jdbcPassword,   entry._jdbcPassword);
			htmlTabRowIfChanged(sb, XML_JDBC_SAVE_PASSWORD  , _jdbcSavePassword,  entry._jdbcSavePassword);
			htmlTabRowIfChanged(sb, XML_JDBC_SqlInit        , _jdbcSqlInit,       entry._jdbcSqlInit);
			htmlTabRowIfChanged(sb, XML_JDBC_UrlOptions     , _jdbcUrlOptions,    entry._jdbcUrlOptions);
			htmlTabRowIfChanged(sb, XML_JDBC_ShhTunnelInfo  , _jdbcShhTunnelInfo, entry._jdbcShhTunnelInfo);

			if (sb.length() > 0)
			{
				sb.insert(0, "<tr> <td nowrap bgcolor=\"#848484\"><font color=\"white\"><b>Attribute</b></font></td> <td nowrap bgcolor=\"#848484\"><font color=\"white\"><b>New Value</b></font></td> <td nowrap bgcolor=\"#848484\"><font color=\"white\"><b>Old Value</b></font></td> </tr>");
				sb.insert(0, "<table border=1 cellspacing=0 cellpadding=1>");
				sb.append(   "</table>");

				return sb.toString();
			}
			return null;
		}

		@Override
		public String toHtml(String name, Type type, SrvType srvType)
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append("<html>");

			sb.append("Connection Profile:<br>");
			sb.append("<table border=0 cellspacing=1 cellpadding=1>");
			htmlTabRow(sb, "Name:",        name);
			htmlTabRow(sb, "Type:",        type);
			htmlTabRow(sb, "Server Type:", srvType);
			sb.append("</table>");
			sb.append("<hr>");

			sb.append("<table border=0 cellspacing=1 cellpadding=1>");
			htmlTabRow(sb, XML_JDBC_DRIVER       , _jdbcDriver);
			htmlTabRow(sb, XML_JDBC_URL          , _jdbcUrl);
			htmlTabRow(sb, XML_JDBC_USERNAME     , _jdbcUsername);
			if (_jdbcSavePassword)
			htmlTabRow(sb, XML_JDBC_PASSWORD     , "**secret**");
			htmlTabRow(sb, XML_JDBC_SAVE_PASSWORD, _jdbcSavePassword);
			htmlTabRow(sb, XML_JDBC_SqlInit      , _jdbcSqlInit);
			htmlTabRow(sb, XML_JDBC_UrlOptions   , _jdbcUrlOptions);
			htmlTabRow(sb, XML_JDBC_ShhTunnelInfo, _jdbcShhTunnelInfo);
			sb.append("</table>");

			sb.append("</html>");
			
			return sb.toString();
		}

		@Override
		public String toXml(String name, Type type, SrvType srvType)
		{
			StringBuilder sb = new StringBuilder();

			StringUtil.xmlBeginTag(sb, 4, XML_JDBC_ENTRY, 
					XML_CONN_PROF_ATTR_NAME,     name,
					XML_CONN_PROF_ATTR_TYPE,     type.toString(),
					XML_CONN_PROF_ATTR_SRV_TYPE, srvType.toString()
					);

			StringUtil.xmlTag(sb, 8, XML_JDBC_DRIVER,          _jdbcDriver);
			StringUtil.xmlTag(sb, 8, XML_JDBC_URL,             _jdbcUrl);
			StringUtil.xmlTag(sb, 8, XML_JDBC_USERNAME,        _jdbcUsername);
			if (_jdbcSavePassword)
				StringUtil.xmlTag(sb, 8, XML_JDBC_PASSWORD,    Configuration.encryptPropertyValue(XML_JDBC_PASSWORD, _jdbcPassword));
			StringUtil.xmlTag(sb, 8, XML_JDBC_SAVE_PASSWORD,   _jdbcSavePassword);

			StringUtil.xmlTag(sb, 8, XML_JDBC_SqlInit,         _jdbcSqlInit);
			StringUtil.xmlTag(sb, 8, XML_JDBC_UrlOptions,      _jdbcUrlOptions);
			StringUtil.xmlTag(sb, 8, XML_JDBC_ShhTunnelInfo,   _jdbcShhTunnelInfo == null ? "" : _jdbcShhTunnelInfo.getConfigString(false));
			
			StringUtil.xmlEndTag(sb, 4, XML_JDBC_ENTRY);
			return sb.toString();
		}

		public static ConnectionProfile parseXml(Element element)
		{
			// Attributes of the Element XML_JDBC_ENTRY
			String name    = element.getAttribute(ConnectionProfile.XML_CONN_PROF_ATTR_NAME);
			String type    = element.getAttribute(ConnectionProfile.XML_CONN_PROF_ATTR_TYPE);
			String srvType = element.getAttribute(ConnectionProfile.XML_CONN_PROF_ATTR_SRV_TYPE);

			// Tags in the XML_JDBC_ENTRY
			JdbcEntry entry = new JdbcEntry();
			entry._jdbcDriver        = getValue(element, XML_JDBC_DRIVER,        entry._jdbcDriver);
			entry._jdbcUrl           = getValue(element, XML_JDBC_URL,           entry._jdbcUrl);
			entry._jdbcUsername      = getValue(element, XML_JDBC_USERNAME,      entry._jdbcUsername);
			entry._jdbcSavePassword  = getValue(element, XML_JDBC_SAVE_PASSWORD, entry._jdbcSavePassword);
			if (entry._jdbcSavePassword)
				entry._jdbcPassword  = getValue(element, XML_JDBC_PASSWORD,      entry._jdbcPassword);
			entry._jdbcSqlInit       = getValue(element, XML_JDBC_SqlInit,       entry._jdbcSqlInit);
			entry._jdbcUrlOptions    = getValue(element, XML_JDBC_ShhTunnelInfo, entry._jdbcUrlOptions);
			entry._jdbcShhTunnelInfo = SshTunnelInfo.parseConfigString(getValue(element, XML_JDBC_ShhTunnelInfo));

			ConnectionProfile connProfile = new ConnectionProfile(name, type, srvType, entry);
			return connProfile;
		}
	}

	public static class OfflineEntry
	implements ConnProfileEntry
	{
		public String        _jdbcDriver             = null;
		public String        _jdbcUrl                = null;
		public String        _jdbcUsername           = null;
		public String        _jdbcPassword           = null;
		public boolean       _jdbcSavePassword       = true;

		public boolean       _checkForNewSessions    = false;
		public boolean       _H2Option_startH2NwSrv  = true;


		@Override
		public String getKey()
		{
			return _jdbcUrl;
		}

//		@Override
//		public boolean equals(Object obj)
//		{
//			// FIXME: implement this BETTER
//			if (obj instanceof OfflineEntry)
//			{
//				String thisXml =                this.toXml("equals-dummy", Type.OFFLINE, SrvType.JDBC_OTHER);
//				String objXml  = ((OfflineEntry)obj).toXml("equals-dummy", Type.OFFLINE, SrvType.JDBC_OTHER);
//				return thisXml.equals(objXml);
//			}
//
//			return super.equals(obj);
//		}

		@Override
		public String getDifference(ConnProfileEntry profileEntry)
		{
			OfflineEntry entry = (OfflineEntry) profileEntry;
			
			StringBuilder sb = new StringBuilder();
			
			htmlTabRowIfChanged(sb, XML_OFFLINE_DRIVER               , _jdbcDriver,            entry._jdbcDriver);
			htmlTabRowIfChanged(sb, XML_OFFLINE_URL                  , _jdbcUrl,               entry._jdbcUrl);
			htmlTabRowIfChanged(sb, XML_OFFLINE_USERNAME             , _jdbcUsername,          entry._jdbcUsername);
			if (_jdbcSavePassword)
				htmlTabRowIfChangedPwd(sb, XML_OFFLINE_PASSWORD      , _jdbcPassword,          entry._jdbcPassword);
			htmlTabRowIfChanged(sb, XML_OFFLINE_SAVE_PASSWORD        , _jdbcSavePassword,      entry._jdbcSavePassword);
			htmlTabRowIfChanged(sb, XML_OFFLINE_checkForNewSessions  , _checkForNewSessions,   entry._checkForNewSessions);
			htmlTabRowIfChanged(sb, XML_OFFLINE_H2Option_startH2NwSrv, _H2Option_startH2NwSrv, entry._H2Option_startH2NwSrv);

			if (sb.length() > 0)
			{
				sb.insert(0, "<tr> <td nowrap bgcolor=\"#848484\"><font color=\"white\"><b>Attribute</b></font></td> <td nowrap bgcolor=\"#848484\"><font color=\"white\"><b>New Value</b></font></td> <td nowrap bgcolor=\"#848484\"><font color=\"white\"><b>Old Value</b></font></td> </tr>");
				sb.insert(0, "<table border=1 cellspacing=0 cellpadding=1>");
				sb.append(   "</table>");

				return sb.toString();
			}
			return null;
		}

		@Override
		public String toHtml(String name, Type type, SrvType srvType)
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append("<html>");

			sb.append("Connection Profile:<br>");
			sb.append("<table border=0 cellspacing=1 cellpadding=1>");
			htmlTabRow(sb, "Name:",        name);
			htmlTabRow(sb, "Type:",        type);
			htmlTabRow(sb, "Server Type:", srvType);
			sb.append("</table>");
			sb.append("<hr>");

			sb.append("<table border=0 cellspacing=1 cellpadding=1>");
			htmlTabRow(sb, XML_OFFLINE_DRIVER               , _jdbcDriver);
			htmlTabRow(sb, XML_OFFLINE_URL                  , _jdbcUrl);
			htmlTabRow(sb, XML_OFFLINE_USERNAME             , _jdbcUsername);
			if (_jdbcSavePassword)
			htmlTabRow(sb, XML_OFFLINE_PASSWORD             , "**secret**");
			htmlTabRow(sb, XML_OFFLINE_SAVE_PASSWORD        , _jdbcSavePassword);
			htmlTabRow(sb, XML_OFFLINE_checkForNewSessions  , _checkForNewSessions);
			htmlTabRow(sb, XML_OFFLINE_H2Option_startH2NwSrv, _H2Option_startH2NwSrv);
			sb.append("</table>");

			sb.append("</html>");
			
			return sb.toString();
		}

		@Override
		public String toXml(String name, Type type, SrvType srvType)
		{
			StringBuilder sb = new StringBuilder();

			StringUtil.xmlBeginTag(sb, 4, XML_OFFLINE_ENTRY, 
					XML_CONN_PROF_ATTR_NAME,     name,
					XML_CONN_PROF_ATTR_TYPE,     type.toString(),
					XML_CONN_PROF_ATTR_SRV_TYPE, srvType.toString()
					);

			StringUtil.xmlTag(sb, 8, XML_OFFLINE_DRIVER,          _jdbcDriver);
			StringUtil.xmlTag(sb, 8, XML_OFFLINE_URL,             _jdbcUrl);
			StringUtil.xmlTag(sb, 8, XML_OFFLINE_USERNAME,        _jdbcUsername);
			if (_jdbcSavePassword)
				StringUtil.xmlTag(sb, 8, XML_OFFLINE_PASSWORD,    Configuration.encryptPropertyValue(XML_OFFLINE_PASSWORD, _jdbcPassword));
			StringUtil.xmlTag(sb, 8, XML_OFFLINE_SAVE_PASSWORD,   _jdbcSavePassword);

			StringUtil.xmlTag(sb, 8, XML_OFFLINE_checkForNewSessions,   _checkForNewSessions);
			StringUtil.xmlTag(sb, 8, XML_OFFLINE_H2Option_startH2NwSrv, _H2Option_startH2NwSrv);
			
			StringUtil.xmlEndTag(sb, 4, XML_OFFLINE_ENTRY);
			return sb.toString();
		}

		public static ConnectionProfile parseXml(Element element)
		{
			// Attributes of the Element XML_OFFLINE_ENTRY
			String name    = element.getAttribute(ConnectionProfile.XML_CONN_PROF_ATTR_NAME);
			String type    = element.getAttribute(ConnectionProfile.XML_CONN_PROF_ATTR_TYPE);
			String srvType = element.getAttribute(ConnectionProfile.XML_CONN_PROF_ATTR_SRV_TYPE);

			OfflineEntry entry = new OfflineEntry();
			entry._jdbcDriver            = getValue(element, XML_OFFLINE_DRIVER,                entry._jdbcDriver);
			entry._jdbcUrl               = getValue(element, XML_OFFLINE_URL,                   entry._jdbcUrl);
			entry._jdbcUsername          = getValue(element, XML_OFFLINE_USERNAME,              entry._jdbcUsername);
			entry._jdbcSavePassword      = getValue(element, XML_OFFLINE_SAVE_PASSWORD,         entry._jdbcSavePassword);
			if (entry._jdbcSavePassword)
				entry._jdbcPassword      = getValue(element, XML_OFFLINE_PASSWORD,              entry._jdbcPassword);

			entry._checkForNewSessions   = getValue(element, XML_OFFLINE_checkForNewSessions,   entry._checkForNewSessions);
			entry._H2Option_startH2NwSrv = getValue(element, XML_OFFLINE_H2Option_startH2NwSrv, entry._H2Option_startH2NwSrv);

			ConnectionProfile connProfile = new ConnectionProfile(name, type, srvType, entry);
			return connProfile;
		}
	}

	//---------------------------------------------------------------
	// END: subclasses
	//---------------------------------------------------------------

	private static StringBuilder htmlTabRow(StringBuilder sb, String key, Object val)
	{
		if (val == null)
			return sb;

		sb.append("<tr> <td><b>").append(key).append("</b></td> <td nowrap>").append(val).append("</td> </tr>");
		return sb;
	}
	
	private static StringBuilder htmlTabRowIfChangedPwd(StringBuilder sb, String key, Object oldValue, Object newValue)
	{
		return htmlTabRowIfChanged(sb, key, Configuration.encryptPropertyValue(key, oldValue+""), Configuration.encryptPropertyValue(key, newValue+""));
	}
	private static StringBuilder htmlTabRowIfChanged(StringBuilder sb, String key, Object oldValue, Object newValue)
	{
		boolean changed = false;
		if (oldValue == null) oldValue = "";
		if (newValue == null) newValue = "";

		changed = ! oldValue.equals(newValue);

//		if (thisVal != null)
//			changed = ! thisVal.equals(changedValue);
//
//		if (changedValue != null)
//			changed = ! changedValue.equals(thisVal);

		if (changed)
			sb.append("<tr> <td><b>").append(key).append("</b></td> <td nowrap>").append(newValue).append("</td> <td nowrap>").append(oldValue).append("</td> </tr>");

		return sb;
	}
	
	private static String getValue(Element element, String tagName)
	{
		return getValue(element, tagName, null);
	}
	private static String getValue(Element element, String tagName, String defaultVal)
	{
		String retValue = null;
		NodeList nodeList = element.getElementsByTagName(tagName);
		if (nodeList.getLength() > 0)
			retValue = nodeList.item(0).getTextContent();
		
		if (StringUtil.hasValue(retValue))
		{
			if (Configuration.isEncryptedValue(retValue))
					retValue = Configuration.decryptPropertyValue(tagName, retValue);
			return retValue;
		}

		return defaultVal;
	}

	private static int getValue(Element element, String tagName, int defaultVal)
	{
		String retValue = null;
		NodeList nodeList = element.getElementsByTagName(tagName);
		if (nodeList.getLength() > 0)
			retValue = nodeList.item(0).getTextContent();
		
		if (StringUtil.hasValue(retValue))
		{
			try 
			{ 
				return Integer.parseInt(retValue); 
			}
			catch (NumberFormatException nfe)
			{
				_logger.error("getIntValue(): XML tagName='"+tagName+"', value='"+retValue+"' is not a number, using default value "+defaultVal+" instead.");
			}
		}

		return defaultVal;
	}

	private static boolean getValue(Element element, String tagName, boolean defaultVal)
	{
		String retValue = null;
		NodeList nodeList = element.getElementsByTagName(tagName);
		if (nodeList.getLength() > 0)
			retValue = nodeList.item(0).getTextContent();
		
		if (StringUtil.hasValue(retValue))
			return "true".equalsIgnoreCase(retValue);

		return defaultVal;
	}
}

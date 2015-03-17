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

	private static final DbxTuneParams DBXTUNE_PARAMS_EMPTY = new DbxTuneParams();
	
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
		JDBC_MAXDB,
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
		if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MAXDB     )) return SrvType.JDBC_MAXDB;
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

	public boolean isSrvType(String productName) 
	{
		SrvType srvType = getServerType(_type, productName);
		return srvType.equals(_srvType);
	}

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
	
	public DbxTuneParams getDbxTuneParams()
	{
		if (_tdsEntry     != null) return _tdsEntry ._dbxtuneParams == null ? DBXTUNE_PARAMS_EMPTY : _tdsEntry ._dbxtuneParams;
		if (_jdbcEntry    != null) return _jdbcEntry._dbxtuneParams == null ? DBXTUNE_PARAMS_EMPTY : _jdbcEntry._dbxtuneParams;
		if (_offlineEntry != null) return DBXTUNE_PARAMS_EMPTY;

		throw new IllegalStateException("No Entry has been assigned. _tdsEntry==null, _jdbcEntry==null, _offlineEntry==null");
	}

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

	public String getDbUserName()
	{
		if (_tdsEntry     != null) return _tdsEntry    ._tdsUsername;
		if (_jdbcEntry    != null) return _jdbcEntry   ._jdbcUsername;
		if (_offlineEntry != null) return _offlineEntry._jdbcUsername;
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

	public static final String       XML_TDS_IsAseTuneParamsValid                = "IsAseTuneParamsValid"; // original name for the tag 'IsDbxTuneParamsValid', maybe we can be backward compatible when reading the XML file
	public static final String       XML_TDS_IsDbxTuneParamsValid                = "IsDbxTuneParamsValid";

	public static final String       XML_TDS_DBXTUNE_UseTemplate                 = "UseTemplate";
	public static final String       XML_TDS_DBXTUNE_UseTemplateName             = "UseTemplateName";

	public static final String       XML_TDS_DBXTUNE_OptRecordSession            = "OptRecordSession";
	public static final String       XML_TDS_DBXTUNE_OptOsMonitoring             = "OptOsMonitoring";
	public static final String       XML_TDS_DBXTUNE_OptConnectAtStartup         = "OptConnectAtStartup";
	public static final String       XML_TDS_DBXTUNE_OptReconnectOnLostConn      = "OptReconnectOnLostConn";
	public static final String       XML_TDS_DBXTUNE_OptConnectLater             = "OptConnectLater";
	public static final String       XML_TDS_DBXTUNE_OptConnectLaterHour         = "OptConnectLaterHour";
	public static final String       XML_TDS_DBXTUNE_OptConnectLaterMinute       = "OptConnectLaterMinute";
	public static final String       XML_TDS_DBXTUNE_OptDissConnectLater         = "OptDissConnectLater";
	public static final String       XML_TDS_DBXTUNE_OptDissConnectLaterHour     = "OptDissConnectLaterHour";
	public static final String       XML_TDS_DBXTUNE_OptDissConnectLaterMinute   = "OptDissConnectLaterMinute";

	public static final String       XML_TDS_DBXTUNE_osMonUsername               = "osMonUsername";
	public static final String       XML_TDS_DBXTUNE_osMonPassword               = "osMonPassword";
	public static final String       XML_TDS_DBXTUNE_osMonSavePassword           = "osMonSavePassword";
	public static final String       XML_TDS_DBXTUNE_osMonHost                   = "osMonHost";
	public static final String       XML_TDS_DBXTUNE_osMonPort                   = "osMonPort";

	public static final String       XML_TDS_DBXTUNE_pcsWriterClass                    = "pcsWriterClass";
	public static final String       XML_TDS_DBXTUNE_pcsWriterDriver                   = "pcsWriterDriver";
	public static final String       XML_TDS_DBXTUNE_pcsWriterUrl                      = "pcsWriterUrl";
	public static final String       XML_TDS_DBXTUNE_pcsWriterUsername                 = "pcsWriterUsername";
	public static final String       XML_TDS_DBXTUNE_pcsWriterPassword                 = "pcsWriterPassword";
	public static final String       XML_TDS_DBXTUNE_pcsWriterSavePassword             = "pcsWriterSavePassword";
	public static final String       XML_TDS_DBXTUNE_pcsWriterStartH2asNwServer        = "pcsWriterStartH2asNwServer";
	public static final String       XML_TDS_DBXTUNE_pcsWriterDdlLookup                = "pcsWriterDdlLookup";
	public static final String       XML_TDS_DBXTUNE_pcsWriterDdlStoreDependantObjects = "pcsWriterDdlStoreDependantObjects";
	public static final String       XML_TDS_DBXTUNE_pcsWriterDdlLookupSleepTime       = "pcsWriterDdlLookupSleepTime";
	public static final String       XML_TDS_DBXTUNE_pcsWriterCounterDetailes          = "pcsWriterCounterDetailes";
	
//	public static final String       XML_TDS_DBXTUNE_pcsReaderDriver             = "pcsReaderDriver";
//	public static final String       XML_TDS_DBXTUNE_pcsReaderUrl                = "pcsReaderUrl";
//	public static final String       XML_TDS_DBXTUNE_pcsReaderUsername           = "pcsReaderUsername";
//	public static final String       XML_TDS_DBXTUNE_pcsReaderPassword           = "pcsReaderPassword";
//	public static final String       XML_TDS_DBXTUNE_pcsReaderCheckForNewSamples = "pcsReaderCheckForNewSamples";
//	public static final String       XML_TDS_DBXTUNE_pcsReaderStartH2asNwServer  = "pcsReaderStartH2asNwServer";


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

	public static class DbxTuneParams
	{
		public boolean       _dbxtuneUseTemplate     = false;
		public String        _dbxtuneUseTemplateName = null;

		// DbxTune options
		public boolean       _dbxtuneOptRecordSession          = false;
		public boolean       _dbxtuneOptOsMonitoring           = false;
		public boolean       _dbxtuneOptConnectAtStartup       = false;
		public boolean       _dbxtuneOptReconnectOnLostConn    = true;
		public boolean       _dbxtuneOptConnectLater           = false;
		public String        _dbxtuneOptConnectLaterHour       = null; 
		public String        _dbxtuneOptConnectLaterMinute     = null; 
		public boolean       _dbxtuneOptDissConnectLater       = false;
		public String        _dbxtuneOptDissConnectLaterHour   = null; 
		public String        _dbxtuneOptDissConnectLaterMinute = null; 

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

		/** default constructor */
		public DbxTuneParams()
		{
		}

		/** Copy another object */
		public DbxTuneParams(DbxTuneParams fromEntry)
		{
			if (fromEntry == null)
				return;

			_dbxtuneUseTemplate                = fromEntry._dbxtuneUseTemplate;
			_dbxtuneUseTemplateName            = fromEntry._dbxtuneUseTemplateName;

			// DbxTune options
			_dbxtuneOptRecordSession           = fromEntry._dbxtuneOptRecordSession;
			_dbxtuneOptOsMonitoring            = fromEntry._dbxtuneOptOsMonitoring;
			_dbxtuneOptConnectAtStartup        = fromEntry._dbxtuneOptConnectAtStartup;
			_dbxtuneOptReconnectOnLostConn     = fromEntry._dbxtuneOptReconnectOnLostConn;
			_dbxtuneOptConnectLater            = fromEntry._dbxtuneOptConnectLater;
			_dbxtuneOptConnectLaterHour        = fromEntry._dbxtuneOptConnectLaterHour; 
			_dbxtuneOptConnectLaterMinute      = fromEntry._dbxtuneOptConnectLaterMinute; 
			_dbxtuneOptDissConnectLater        = fromEntry._dbxtuneOptDissConnectLater;
			_dbxtuneOptDissConnectLaterHour    = fromEntry._dbxtuneOptDissConnectLaterHour; 
			_dbxtuneOptDissConnectLaterMinute  = fromEntry._dbxtuneOptDissConnectLaterMinute; 

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

//		public PropPropEntry _pcsWriterCounterDetailes          = null;

		public String getDifference(DbxTuneParams entry)
		{
			if (entry == null)
				entry = DBXTUNE_PARAMS_EMPTY;

			StringBuilder sb = new StringBuilder();
			
			htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_UseTemplate              , _dbxtuneUseTemplate              , entry._dbxtuneUseTemplate);
			if (_dbxtuneUseTemplate || entry._dbxtuneUseTemplate)
				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_UseTemplateName      , _dbxtuneUseTemplateName          , entry._dbxtuneUseTemplateName);
			htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptRecordSession         , _dbxtuneOptRecordSession         , entry._dbxtuneOptRecordSession);
			htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptOsMonitoring          , _dbxtuneOptOsMonitoring          , entry._dbxtuneOptOsMonitoring);
			htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptConnectAtStartup      , _dbxtuneOptConnectAtStartup      , entry._dbxtuneOptConnectAtStartup);
			htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptReconnectOnLostConn   , _dbxtuneOptReconnectOnLostConn   , entry._dbxtuneOptReconnectOnLostConn);
			htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptConnectLater          , _dbxtuneOptConnectLater          , entry._dbxtuneOptConnectLater);
			htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptConnectLaterHour      , _dbxtuneOptConnectLaterHour      , entry._dbxtuneOptConnectLaterHour);
			htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptConnectLaterMinute    , _dbxtuneOptConnectLaterMinute    , entry._dbxtuneOptConnectLaterMinute);
			htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptDissConnectLater      , _dbxtuneOptDissConnectLater      , entry._dbxtuneOptDissConnectLater);
			htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptDissConnectLaterHour  , _dbxtuneOptDissConnectLaterHour  , entry._dbxtuneOptDissConnectLaterHour);
			htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptDissConnectLaterMinute, _dbxtuneOptDissConnectLaterMinute, entry._dbxtuneOptDissConnectLaterMinute);

			if (_dbxtuneOptRecordSession || entry._dbxtuneOptRecordSession)
			{
				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_pcsWriterClass                   , _pcsWriterClass   , entry._pcsWriterClass);
				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_pcsWriterDriver                  , _pcsWriterDriver  , entry._pcsWriterDriver);
				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_pcsWriterUrl                     , _pcsWriterUrl     , entry._pcsWriterUrl);
				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_pcsWriterUsername                , _pcsWriterUsername, entry._pcsWriterUsername);
				if (_pcsWriterSavePassword)
					htmlTabRowIfChangedPwd(sb, XML_TDS_DBXTUNE_pcsWriterPassword         , _pcsWriterPassword                , entry._pcsWriterPassword);
				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_pcsWriterSavePassword            , _pcsWriterSavePassword            , entry._pcsWriterSavePassword);
				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_pcsWriterStartH2asNwServer       , _pcsWriterStartH2asNwServer       , entry._pcsWriterStartH2asNwServer);
				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_pcsWriterDdlLookup               , _pcsWriterDdlLookup               , entry._pcsWriterDdlLookup);
				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_pcsWriterDdlStoreDependantObjects, _pcsWriterDdlStoreDependantObjects, entry._pcsWriterDdlStoreDependantObjects);
				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_pcsWriterDdlLookupSleepTime      , _pcsWriterDdlLookupSleepTime      , entry._pcsWriterDdlLookupSleepTime);
			}

			if (_dbxtuneOptOsMonitoring || entry._dbxtuneOptOsMonitoring)
			{
				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_osMonUsername    , _osMonUsername    , entry._osMonUsername);
				if (_osMonSavePassword || entry._osMonSavePassword)
					htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_osMonPassword, _osMonPassword    , entry._osMonPassword);
				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_osMonSavePassword, _osMonSavePassword, entry._osMonSavePassword);
				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_osMonHost        , _osMonHost        , entry._osMonHost);
				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_osMonPort        , _osMonPort        , entry._osMonPort);
			}

			return sb.toString();
		}

		public String toHtml()
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append("<h3>DbxTune specific settings</h3>");
			sb.append("<hr>");
			sb.append("<table border=0 cellspacing=1 cellpadding=1>");
			htmlTabRow(sb, XML_TDS_DBXTUNE_UseTemplate, _dbxtuneUseTemplate   );
			if (_dbxtuneUseTemplate)
				htmlTabRow(sb, XML_TDS_DBXTUNE_UseTemplateName      , _dbxtuneUseTemplateName);
			htmlTabRow(sb, XML_TDS_DBXTUNE_OptRecordSession         , _dbxtuneOptRecordSession);
			htmlTabRow(sb, XML_TDS_DBXTUNE_OptOsMonitoring          , _dbxtuneOptOsMonitoring);
			htmlTabRow(sb, XML_TDS_DBXTUNE_OptConnectAtStartup      , _dbxtuneOptConnectAtStartup);
			htmlTabRow(sb, XML_TDS_DBXTUNE_OptReconnectOnLostConn   , _dbxtuneOptReconnectOnLostConn);
			htmlTabRow(sb, XML_TDS_DBXTUNE_OptConnectLater          , _dbxtuneOptConnectLater);
			htmlTabRow(sb, XML_TDS_DBXTUNE_OptConnectLaterHour      , _dbxtuneOptConnectLaterHour);
			htmlTabRow(sb, XML_TDS_DBXTUNE_OptConnectLaterMinute    , _dbxtuneOptConnectLaterMinute);
			htmlTabRow(sb, XML_TDS_DBXTUNE_OptDissConnectLater      , _dbxtuneOptDissConnectLater);
			htmlTabRow(sb, XML_TDS_DBXTUNE_OptDissConnectLaterHour  , _dbxtuneOptDissConnectLaterHour);
			htmlTabRow(sb, XML_TDS_DBXTUNE_OptDissConnectLaterMinute, _dbxtuneOptDissConnectLaterMinute);
			sb.append("</table>");

			if (_dbxtuneOptRecordSession)
			{
				sb.append("<h3>Recording Database details</h3>");
				sb.append("<hr>");
				sb.append("<table border=0 cellspacing=1 cellpadding=1>");
				htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterClass                   , _pcsWriterClass);
				htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterDriver                  , _pcsWriterDriver);
				htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterUrl                     , _pcsWriterUrl);
				htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterUsername                , _pcsWriterUsername);
				if (_pcsWriterSavePassword)
					htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterPassword            , !_logger.isDebugEnabled() ? "**secret**" : _pcsWriterPassword);
				htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterSavePassword            , _pcsWriterSavePassword);
				htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterStartH2asNwServer       , _pcsWriterStartH2asNwServer);
				htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterDdlLookup               , _pcsWriterDdlLookup);
				htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterDdlStoreDependantObjects, _pcsWriterDdlStoreDependantObjects);
				htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterDdlLookupSleepTime      , _pcsWriterDdlLookupSleepTime);
				sb.append("</table>");
			}

			if (_dbxtuneOptOsMonitoring)
			{
				sb.append("<h3>Operating System Monitoring details</h3>");
				sb.append("<hr>");
				sb.append("<table border=0 cellspacing=1 cellpadding=1>");
				htmlTabRow(sb, XML_TDS_DBXTUNE_osMonUsername    , _osMonUsername);
				if (_osMonSavePassword)
					htmlTabRow(sb, XML_TDS_DBXTUNE_osMonPassword, !_logger.isDebugEnabled() ? "**secret**" : _osMonPassword);
				htmlTabRow(sb, XML_TDS_DBXTUNE_osMonSavePassword, _osMonSavePassword);
				htmlTabRow(sb, XML_TDS_DBXTUNE_osMonHost        , _osMonHost);
				htmlTabRow(sb, XML_TDS_DBXTUNE_osMonPort        , _osMonPort);
				sb.append("</table>");
			}

			return sb.toString();
		}

		public String toXml()
		{
			StringBuilder sb = new StringBuilder();

			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_UseTemplate,                 _dbxtuneUseTemplate);
			if (_dbxtuneUseTemplate)
				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_UseTemplateName,         _dbxtuneUseTemplateName);
		
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptRecordSession,            _dbxtuneOptRecordSession);
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptOsMonitoring,             _dbxtuneOptOsMonitoring);
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptConnectAtStartup,         _dbxtuneOptConnectAtStartup);
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptReconnectOnLostConn,      _dbxtuneOptReconnectOnLostConn);
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptConnectLater,             _dbxtuneOptConnectLater);
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptConnectLaterHour,         _dbxtuneOptConnectLaterHour);
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptConnectLaterMinute,       _dbxtuneOptConnectLaterMinute);
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptDissConnectLater,         _dbxtuneOptDissConnectLater);
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptDissConnectLaterHour,     _dbxtuneOptDissConnectLaterHour);
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptDissConnectLaterMinute,   _dbxtuneOptDissConnectLaterMinute);
		
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_osMonUsername,               _osMonUsername);
			if (_osMonSavePassword)
				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_osMonPassword,           Configuration.encryptPropertyValue(XML_TDS_DBXTUNE_osMonPassword, _osMonPassword));
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_osMonSavePassword,           _osMonSavePassword);
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_osMonHost,                   _osMonHost);
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_osMonPort,                   _osMonPort);
		
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterClass,                    _pcsWriterClass);
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterDriver,                   _pcsWriterDriver);
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterUrl,                      _pcsWriterUrl);
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterUsername,                 _pcsWriterUsername);
			if (_pcsWriterSavePassword)
				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterPassword,              _pcsWriterPassword);
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterSavePassword,             _pcsWriterSavePassword);
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterStartH2asNwServer,        _pcsWriterStartH2asNwServer);
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterDdlLookup,                _pcsWriterDdlLookup);
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterDdlStoreDependantObjects, _pcsWriterDdlStoreDependantObjects);
			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterDdlLookupSleepTime,       _pcsWriterDdlLookupSleepTime);
//			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterCounterDetailes,          _pcsWriterCounterDetailes);

//			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsReaderDriver,             _pcsReaderDriver);
//			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsReaderUrl,                _pcsReaderUrl);
//			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsReaderUsername,           _pcsReaderUsername);
//			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsReaderPassword,           Configuration.encryptPropertyValue(XML_TDS_DBXTUNE_pcsReaderPassword, _pcsReaderPassword));
//			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsReaderCheckForNewSamples, _pcsReaderCheckForNewSamples);
//			StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsReaderStartH2asNwServer,  _pcsReaderStartH2asNwServer);

			return sb.toString();
		}

		public static DbxTuneParams parseXml(Element element)
		{
			DbxTuneParams entry = new DbxTuneParams();

			entry._dbxtuneUseTemplate                = getValue(element, XML_TDS_DBXTUNE_UseTemplate,     entry._dbxtuneUseTemplate);
			entry._dbxtuneUseTemplateName            = getValue(element, XML_TDS_DBXTUNE_UseTemplateName, entry._dbxtuneUseTemplateName);

			// DbxTune options
			entry._dbxtuneOptRecordSession           = getValue(element, XML_TDS_DBXTUNE_OptRecordSession,          entry._dbxtuneOptRecordSession);
			entry._dbxtuneOptOsMonitoring            = getValue(element, XML_TDS_DBXTUNE_OptOsMonitoring,           entry._dbxtuneOptOsMonitoring);
			entry._dbxtuneOptConnectAtStartup        = getValue(element, XML_TDS_DBXTUNE_OptConnectAtStartup,       entry._dbxtuneOptConnectAtStartup); // should the be in here???
			entry._dbxtuneOptReconnectOnLostConn     = getValue(element, XML_TDS_DBXTUNE_OptReconnectOnLostConn,    entry._dbxtuneOptReconnectOnLostConn);
			entry._dbxtuneOptConnectLater            = getValue(element, XML_TDS_DBXTUNE_OptConnectLater,           entry._dbxtuneOptConnectLater); // should the be in here???
			entry._dbxtuneOptConnectLaterHour        = getValue(element, XML_TDS_DBXTUNE_OptConnectLaterHour,       entry._dbxtuneOptConnectLaterHour);  // should the be in here???
			entry._dbxtuneOptConnectLaterMinute      = getValue(element, XML_TDS_DBXTUNE_OptConnectLaterMinute,     entry._dbxtuneOptConnectLaterMinute);  // should the be in here???
			entry._dbxtuneOptDissConnectLater        = getValue(element, XML_TDS_DBXTUNE_OptDissConnectLater,       entry._dbxtuneOptDissConnectLater); // should the be in here???
			entry._dbxtuneOptDissConnectLaterHour    = getValue(element, XML_TDS_DBXTUNE_OptDissConnectLaterHour,   entry._dbxtuneOptDissConnectLaterHour);  // should the be in here???
			entry._dbxtuneOptDissConnectLaterMinute  = getValue(element, XML_TDS_DBXTUNE_OptDissConnectLaterMinute, entry._dbxtuneOptDissConnectLaterMinute);  // should the be in here???

			// HostMonitor
			if (entry._dbxtuneOptOsMonitoring)
			{
				entry._osMonUsername                 = getValue(element, XML_TDS_DBXTUNE_osMonUsername,     entry._osMonUsername);
				entry._osMonSavePassword             = getValue(element, XML_TDS_DBXTUNE_osMonSavePassword, entry._osMonSavePassword); // Should this be here???
				if (entry._osMonSavePassword)
					entry._osMonPassword             = getValue(element, XML_TDS_DBXTUNE_osMonPassword,     entry._osMonPassword);
				entry._osMonHost                     = getValue(element, XML_TDS_DBXTUNE_osMonHost,         entry._osMonHost);
				entry._osMonPort                     = getValue(element, XML_TDS_DBXTUNE_osMonPort,         entry._osMonPort);
			}

			// Recordings
			if (entry._dbxtuneOptRecordSession)
			{
				entry._pcsWriterClass                    = getValue(element, XML_TDS_DBXTUNE_pcsWriterClass,                    entry._pcsWriterClass);
				entry._pcsWriterDriver                   = getValue(element, XML_TDS_DBXTUNE_pcsWriterDriver,                   entry._pcsWriterDriver);
				entry._pcsWriterUrl                      = getValue(element, XML_TDS_DBXTUNE_pcsWriterUrl,                      entry._pcsWriterUrl);
				entry._pcsWriterUsername                 = getValue(element, XML_TDS_DBXTUNE_pcsWriterUsername,                 entry._pcsWriterUsername);
				entry._pcsWriterSavePassword             = getValue(element, XML_TDS_DBXTUNE_pcsWriterSavePassword,             entry._pcsWriterSavePassword);
				if (entry._pcsWriterSavePassword)
					entry._pcsWriterPassword             = getValue(element, XML_TDS_DBXTUNE_pcsWriterPassword,                 entry._pcsWriterPassword);
				entry._pcsWriterStartH2asNwServer        = getValue(element, XML_TDS_DBXTUNE_pcsWriterStartH2asNwServer,        entry._pcsWriterStartH2asNwServer);
				entry._pcsWriterDdlLookup                = getValue(element, XML_TDS_DBXTUNE_pcsWriterDdlLookup,                entry._pcsWriterDdlLookup);
				entry._pcsWriterDdlStoreDependantObjects = getValue(element, XML_TDS_DBXTUNE_pcsWriterDdlStoreDependantObjects, entry._pcsWriterDdlStoreDependantObjects);
				entry._pcsWriterDdlLookupSleepTime       = getValue(element, XML_TDS_DBXTUNE_pcsWriterDdlLookupSleepTime,       entry._pcsWriterDdlLookupSleepTime);
//				entry._pcsWriterCounterDetailes          = getValue(element, XML_TDS_DBXTUNE_pcsWriterCounterDetailes,          entry._pcsWriterCounterDetailes);
			}
			// Load Recordings
//			entry._pcsReaderDriver                   = getValue(element, XML_TDS_DBXTUNE_pcsReaderDriver,             entry._pcsReaderDriver);
//			entry._pcsReaderUrl                      = getValue(element, XML_TDS_DBXTUNE_pcsReaderUrl,                entry._pcsReaderUrl);
//			entry._pcsReaderUsername                 = getValue(element, XML_TDS_DBXTUNE_pcsReaderUsername,           entry._pcsReaderUsername);
//			entry._pcsReaderPassword                 = getValue(element, XML_TDS_DBXTUNE_pcsReaderPassword,           entry._pcsReaderPassword);
//			entry._pcsReaderCheckForNewSamples       = getValue(element, XML_TDS_DBXTUNE_pcsReaderCheckForNewSamples, entry._pcsReaderCheckForNewSamples);
//			entry._pcsReaderStartH2asNwServer        = getValue(element, XML_TDS_DBXTUNE_pcsReaderStartH2asNwServer,  entry._pcsReaderStartH2asNwServer);
			
			return entry;
		}
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
		public boolean       _isDbxTuneParamsValid   = false; 
		public DbxTuneParams _dbxtuneParams    = null; 

//		public boolean       _dbxtuneUseTemplate     = false;
//		public String        _dbxtuneUseTemplateName = null;
//
//		// DbxTune options
//		public boolean       _dbxtuneOptRecordSession          = false;
//		public boolean       _dbxtuneOptOsMonitoring           = false;
//		public boolean       _dbxtuneOptConnectAtStartup       = false;
//		public boolean       _dbxtuneOptReconnectOnLostConn    = true;
//		public boolean       _dbxtuneOptConnectLater           = false;
//		public String        _dbxtuneOptConnectLaterHour       = null; 
//		public String        _dbxtuneOptConnectLaterMinute     = null; 
//		public boolean       _dbxtuneOptDissConnectLater       = false;
//		public String        _dbxtuneOptDissConnectLaterHour   = null; 
//		public String        _dbxtuneOptDissConnectLaterMinute = null; 
//
//		// HostMonitor
//		public String        _osMonUsername      = null;
//		public String        _osMonPassword      = null;
//		public boolean       _osMonSavePassword  = true;
//		public String        _osMonHost          = null;
//		public int           _osMonPort          = 22;
//
//		// Recordings
//		public String        _pcsWriterClass             = null;
//		public String        _pcsWriterDriver            = null;
//		public String        _pcsWriterUrl               = null;
//		public String        _pcsWriterUsername          = null;
//		public String        _pcsWriterPassword          = null;
//		public boolean       _pcsWriterSavePassword      = true;
//		public boolean       _pcsWriterStartH2asNwServer = true;
//		public boolean       _pcsWriterDdlLookup                = PersistentCounterHandler.DEFAULT_ddl_doDdlLookupAndStore;
//		public boolean       _pcsWriterDdlStoreDependantObjects = PersistentCounterHandler.DEFAULT_ddl_addDependantObjectsToDdlInQueue;
//		public int           _pcsWriterDdlLookupSleepTime       = PersistentCounterHandler.DEFAULT_ddl_afterDdlLookupSleepTimeInMs;
////		public PropPropEntry _pcsWriterCounterDetailes          = null;


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
		 * if the previously stored profile IS an DbxTune Profile<br>
		 * and we are using SQLWindow, we need to copy the "hidden" fields into the passed ConnectionProfileEntry<br>
		 * otherwise we will lose that data the next time we use the entry from DbxTune
		 * 
		 * @param fromEntry entry to copy from
		 */
		public void copyDbxTuneParams(TdsEntry fromEntry)
		{
//System.out.println("copyDbxTuneParams: from '"+fromEntry+"', to this '"+this+"'.");
			_isDbxTuneParamsValid              = fromEntry._isDbxTuneParamsValid; 

			_dbxtuneParams                     = new DbxTuneParams(fromEntry._dbxtuneParams); // we probably need to create a NEW object here

//			_dbxtuneUseTemplate                = fromEntry._dbxtuneUseTemplate;
//			_dbxtuneUseTemplateName            = fromEntry._dbxtuneUseTemplateName;
//
//			// DbxTune options
//			_dbxtuneOptRecordSession           = fromEntry._dbxtuneOptRecordSession;
//			_dbxtuneOptOsMonitoring            = fromEntry._dbxtuneOptOsMonitoring;
//			_dbxtuneOptConnectAtStartup        = fromEntry._dbxtuneOptConnectAtStartup;
//			_dbxtuneOptReconnectOnLostConn     = fromEntry._dbxtuneOptReconnectOnLostConn;
//			_dbxtuneOptConnectLater            = fromEntry._dbxtuneOptConnectLater;
//			_dbxtuneOptConnectLaterHour        = fromEntry._dbxtuneOptConnectLaterHour; 
//			_dbxtuneOptConnectLaterMinute      = fromEntry._dbxtuneOptConnectLaterMinute; 
//			_dbxtuneOptDissConnectLater        = fromEntry._dbxtuneOptDissConnectLater;
//			_dbxtuneOptDissConnectLaterHour    = fromEntry._dbxtuneOptDissConnectLaterHour; 
//			_dbxtuneOptDissConnectLaterMinute  = fromEntry._dbxtuneOptDissConnectLaterMinute; 
//
//			// HostMonitor
//			_osMonUsername                     = fromEntry._osMonUsername;
//			_osMonPassword                     = fromEntry._osMonPassword;
//			_osMonSavePassword                 = fromEntry._osMonSavePassword;
//			_osMonHost                         = fromEntry._osMonHost;
//			_osMonPort                         = fromEntry._osMonPort;
//
//			// Recordings
//			_pcsWriterClass                    = fromEntry._pcsWriterClass;
//			_pcsWriterDriver                   = fromEntry._pcsWriterDriver;
//			_pcsWriterUrl                      = fromEntry._pcsWriterUrl;
//			_pcsWriterUsername                 = fromEntry._pcsWriterUsername;
//			_pcsWriterPassword                 = fromEntry._pcsWriterPassword;
//			_pcsWriterSavePassword             = fromEntry._pcsWriterSavePassword;
//			_pcsWriterStartH2asNwServer        = fromEntry._pcsWriterStartH2asNwServer;
//			_pcsWriterDdlLookup                = fromEntry._pcsWriterDdlLookup;
//			_pcsWriterDdlStoreDependantObjects = fromEntry._pcsWriterDdlStoreDependantObjects;
//			_pcsWriterDdlLookupSleepTime       = fromEntry._pcsWriterDdlLookupSleepTime;
			
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

			if (_isDbxTuneParamsValid || entry._isDbxTuneParamsValid)
			{
				if (_dbxtuneParams == null)
					_dbxtuneParams = DBXTUNE_PARAMS_EMPTY;

				sb.append(_dbxtuneParams.getDifference(entry._dbxtuneParams));

//				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_UseTemplate              , _dbxtuneUseTemplate              , entry._dbxtuneUseTemplate);
//				if (_dbxtuneUseTemplate || entry._dbxtuneUseTemplate)
//					htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_UseTemplateName      , _dbxtuneUseTemplateName          , entry._dbxtuneUseTemplateName);
//				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptRecordSession         , _dbxtuneOptRecordSession         , entry._dbxtuneOptRecordSession);
//				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptOsMonitoring          , _dbxtuneOptOsMonitoring          , entry._dbxtuneOptOsMonitoring);
//				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptConnectAtStartup      , _dbxtuneOptConnectAtStartup      , entry._dbxtuneOptConnectAtStartup);
//				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptReconnectOnLostConn   , _dbxtuneOptReconnectOnLostConn   , entry._dbxtuneOptReconnectOnLostConn);
//				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptConnectLater          , _dbxtuneOptConnectLater          , entry._dbxtuneOptConnectLater);
//				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptConnectLaterHour      , _dbxtuneOptConnectLaterHour      , entry._dbxtuneOptConnectLaterHour);
//				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptConnectLaterMinute    , _dbxtuneOptConnectLaterMinute    , entry._dbxtuneOptConnectLaterMinute);
//				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptDissConnectLater      , _dbxtuneOptDissConnectLater      , entry._dbxtuneOptDissConnectLater);
//				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptDissConnectLaterHour  , _dbxtuneOptDissConnectLaterHour  , entry._dbxtuneOptDissConnectLaterHour);
//				htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_OptDissConnectLaterMinute, _dbxtuneOptDissConnectLaterMinute, entry._dbxtuneOptDissConnectLaterMinute);
//
//				if (_dbxtuneOptRecordSession || entry._dbxtuneOptRecordSession)
//				{
//					htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_pcsWriterClass                   , _pcsWriterClass   , entry._pcsWriterClass);
//					htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_pcsWriterDriver                  , _pcsWriterDriver  , entry._pcsWriterDriver);
//					htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_pcsWriterUrl                     , _pcsWriterUrl     , entry._pcsWriterUrl);
//					htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_pcsWriterUsername                , _pcsWriterUsername, entry._pcsWriterUsername);
//					if (_pcsWriterSavePassword)
//						htmlTabRowIfChangedPwd(sb, XML_TDS_DBXTUNE_pcsWriterPassword         , _pcsWriterPassword                , entry._pcsWriterPassword);
//					htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_pcsWriterSavePassword            , _pcsWriterSavePassword            , entry._pcsWriterSavePassword);
//					htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_pcsWriterStartH2asNwServer       , _pcsWriterStartH2asNwServer       , entry._pcsWriterStartH2asNwServer);
//					htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_pcsWriterDdlLookup               , _pcsWriterDdlLookup               , entry._pcsWriterDdlLookup);
//					htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_pcsWriterDdlStoreDependantObjects, _pcsWriterDdlStoreDependantObjects, entry._pcsWriterDdlStoreDependantObjects);
//					htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_pcsWriterDdlLookupSleepTime      , _pcsWriterDdlLookupSleepTime      , entry._pcsWriterDdlLookupSleepTime);
//				}
//
//				if (_dbxtuneOptOsMonitoring || entry._dbxtuneOptOsMonitoring)
//				{
//					htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_osMonUsername    , _osMonUsername    , entry._osMonUsername);
//					if (_osMonSavePassword || entry._osMonSavePassword)
//						htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_osMonPassword, _osMonPassword    , entry._osMonPassword);
//					htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_osMonSavePassword, _osMonSavePassword, entry._osMonSavePassword);
//					htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_osMonHost        , _osMonHost        , entry._osMonHost);
//					htmlTabRowIfChanged(sb, XML_TDS_DBXTUNE_osMonPort        , _osMonPort        , entry._osMonPort);
//				}
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
				htmlTabRow(sb, XML_TDS_PASSWORD       , !_logger.isDebugEnabled() ? "**secret**" : _tdsPassword);
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

			if (_isDbxTuneParamsValid)
			{
				if (_dbxtuneParams != null)
					sb.append(_dbxtuneParams.toHtml());
				
//				sb.append("<h3>DbxTune specific settings</h3>");
//				sb.append("<hr>");
//				sb.append("<table border=0 cellspacing=1 cellpadding=1>");
//				htmlTabRow(sb, XML_TDS_DBXTUNE_UseTemplate, _dbxtuneUseTemplate   );
//				if (_dbxtuneUseTemplate)
//					htmlTabRow(sb, XML_TDS_DBXTUNE_UseTemplateName      , _dbxtuneUseTemplateName);
//				htmlTabRow(sb, XML_TDS_DBXTUNE_OptRecordSession         , _dbxtuneOptRecordSession);
//				htmlTabRow(sb, XML_TDS_DBXTUNE_OptOsMonitoring          , _dbxtuneOptOsMonitoring);
//				htmlTabRow(sb, XML_TDS_DBXTUNE_OptConnectAtStartup      , _dbxtuneOptConnectAtStartup);
//				htmlTabRow(sb, XML_TDS_DBXTUNE_OptReconnectOnLostConn   , _dbxtuneOptReconnectOnLostConn);
//				htmlTabRow(sb, XML_TDS_DBXTUNE_OptConnectLater          , _dbxtuneOptConnectLater);
//				htmlTabRow(sb, XML_TDS_DBXTUNE_OptConnectLaterHour      , _dbxtuneOptConnectLaterHour);
//				htmlTabRow(sb, XML_TDS_DBXTUNE_OptConnectLaterMinute    , _dbxtuneOptConnectLaterMinute);
//				htmlTabRow(sb, XML_TDS_DBXTUNE_OptDissConnectLater      , _dbxtuneOptDissConnectLater);
//				htmlTabRow(sb, XML_TDS_DBXTUNE_OptDissConnectLaterHour  , _dbxtuneOptDissConnectLaterHour);
//				htmlTabRow(sb, XML_TDS_DBXTUNE_OptDissConnectLaterMinute, _dbxtuneOptDissConnectLaterMinute);
//				sb.append("</table>");
//
//				if (_dbxtuneOptRecordSession)
//				{
//					sb.append("<h3>Recording Database details</h3>");
//					sb.append("<hr>");
//					sb.append("<table border=0 cellspacing=1 cellpadding=1>");
//					htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterClass                   , _pcsWriterClass);
//					htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterDriver                  , _pcsWriterDriver);
//					htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterUrl                     , _pcsWriterUrl);
//					htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterUsername                , _pcsWriterUsername);
//					if (_pcsWriterSavePassword)
//						htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterPassword            , !_logger.isDebugEnabled() ? "**secret**" : _pcsWriterPassword);
//					htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterSavePassword            , _pcsWriterSavePassword);
//					htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterStartH2asNwServer       , _pcsWriterStartH2asNwServer);
//					htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterDdlLookup               , _pcsWriterDdlLookup);
//					htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterDdlStoreDependantObjects, _pcsWriterDdlStoreDependantObjects);
//					htmlTabRow(sb, XML_TDS_DBXTUNE_pcsWriterDdlLookupSleepTime      , _pcsWriterDdlLookupSleepTime);
//					sb.append("</table>");
//				}
//
//				if (_dbxtuneOptOsMonitoring)
//				{
//					sb.append("<h3>Operating System Monitoring details</h3>");
//					sb.append("<hr>");
//					sb.append("<table border=0 cellspacing=1 cellpadding=1>");
//					htmlTabRow(sb, XML_TDS_DBXTUNE_osMonUsername    , _osMonUsername);
//					if (_osMonSavePassword)
//						htmlTabRow(sb, XML_TDS_DBXTUNE_osMonPassword, !_logger.isDebugEnabled() ? "**secret**" : _osMonPassword);
//					htmlTabRow(sb, XML_TDS_DBXTUNE_osMonSavePassword, _osMonSavePassword);
//					htmlTabRow(sb, XML_TDS_DBXTUNE_osMonHost        , _osMonHost);
//					htmlTabRow(sb, XML_TDS_DBXTUNE_osMonPort        , _osMonPort);
//					sb.append("</table>");
//				}
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

			// DbxTune params
			StringUtil.xmlTag(sb, 8, XML_TDS_IsDbxTuneParamsValid, _isDbxTuneParamsValid);
			if (_isDbxTuneParamsValid)
			{
				if (_dbxtuneParams != null)
					sb.append(_dbxtuneParams.toXml());

//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_UseTemplate,                 _dbxtuneUseTemplate);
//				if (_dbxtuneUseTemplate)
//					StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_UseTemplateName,         _dbxtuneUseTemplateName);
//			
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptRecordSession,            _dbxtuneOptRecordSession);
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptOsMonitoring,             _dbxtuneOptOsMonitoring);
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptConnectAtStartup,         _dbxtuneOptConnectAtStartup);
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptReconnectOnLostConn,      _dbxtuneOptReconnectOnLostConn);
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptConnectLater,             _dbxtuneOptConnectLater);
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptConnectLaterHour,         _dbxtuneOptConnectLaterHour);
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptConnectLaterMinute,       _dbxtuneOptConnectLaterMinute);
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptDissConnectLater,         _dbxtuneOptDissConnectLater);
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptDissConnectLaterHour,     _dbxtuneOptDissConnectLaterHour);
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_OptDissConnectLaterMinute,   _dbxtuneOptDissConnectLaterMinute);
//			
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_osMonUsername,               _osMonUsername);
//				if (_osMonSavePassword)
//					StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_osMonPassword,           Configuration.encryptPropertyValue(XML_TDS_DBXTUNE_osMonPassword, _osMonPassword));
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_osMonSavePassword,           _osMonSavePassword);
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_osMonHost,                   _osMonHost);
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_osMonPort,                   _osMonPort);
//			
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterClass,                    _pcsWriterClass);
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterDriver,                   _pcsWriterDriver);
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterUrl,                      _pcsWriterUrl);
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterUsername,                 _pcsWriterUsername);
//				if (_pcsWriterSavePassword)
//					StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterPassword,              _pcsWriterPassword);
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterSavePassword,             _pcsWriterSavePassword);
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterStartH2asNwServer,        _pcsWriterStartH2asNwServer);
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterDdlLookup,                _pcsWriterDdlLookup);
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterDdlStoreDependantObjects, _pcsWriterDdlStoreDependantObjects);
//				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterDdlLookupSleepTime,       _pcsWriterDdlLookupSleepTime);
////				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsWriterCounterDetailes,          _pcsWriterCounterDetailes);
//
////				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsReaderDriver,             _pcsReaderDriver);
////				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsReaderUrl,                _pcsReaderUrl);
////				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsReaderUsername,           _pcsReaderUsername);
////				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsReaderPassword,           Configuration.encryptPropertyValue(XML_TDS_DBXTUNE_pcsReaderPassword, _pcsReaderPassword));
////				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsReaderCheckForNewSamples, _pcsReaderCheckForNewSamples);
////				StringUtil.xmlTag(sb, 8, XML_TDS_DBXTUNE_pcsReaderStartH2asNwServer,  _pcsReaderStartH2asNwServer);
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

			entry._isDbxTuneParamsValid = getValue(element, XML_TDS_IsDbxTuneParamsValid, XML_TDS_IsAseTuneParamsValid, entry._isDbxTuneParamsValid); // XML_TDS_IsAseTuneParamsValid is backward compatibility
			if (entry._isDbxTuneParamsValid)
			{
				entry._dbxtuneParams = DbxTuneParams.parseXml(element);

//				entry._dbxtuneUseTemplate                = getValue(element, XML_TDS_DBXTUNE_UseTemplate,     entry._dbxtuneUseTemplate);
//				entry._dbxtuneUseTemplateName            = getValue(element, XML_TDS_DBXTUNE_UseTemplateName, entry._dbxtuneUseTemplateName);
//	
//				// DbxTune options
//				entry._dbxtuneOptRecordSession           = getValue(element, XML_TDS_DBXTUNE_OptRecordSession,          entry._dbxtuneOptRecordSession);
//				entry._dbxtuneOptOsMonitoring            = getValue(element, XML_TDS_DBXTUNE_OptOsMonitoring,           entry._dbxtuneOptOsMonitoring);
//				entry._dbxtuneOptConnectAtStartup        = getValue(element, XML_TDS_DBXTUNE_OptConnectAtStartup,       entry._dbxtuneOptConnectAtStartup); // should the be in here???
//				entry._dbxtuneOptReconnectOnLostConn     = getValue(element, XML_TDS_DBXTUNE_OptReconnectOnLostConn,    entry._dbxtuneOptReconnectOnLostConn);
//				entry._dbxtuneOptConnectLater            = getValue(element, XML_TDS_DBXTUNE_OptConnectLater,           entry._dbxtuneOptConnectLater); // should the be in here???
//				entry._dbxtuneOptConnectLaterHour        = getValue(element, XML_TDS_DBXTUNE_OptConnectLaterHour,       entry._dbxtuneOptConnectLaterHour);  // should the be in here???
//				entry._dbxtuneOptConnectLaterMinute      = getValue(element, XML_TDS_DBXTUNE_OptConnectLaterMinute,     entry._dbxtuneOptConnectLaterMinute);  // should the be in here???
//				entry._dbxtuneOptDissConnectLater        = getValue(element, XML_TDS_DBXTUNE_OptDissConnectLater,       entry._dbxtuneOptDissConnectLater); // should the be in here???
//				entry._dbxtuneOptDissConnectLaterHour    = getValue(element, XML_TDS_DBXTUNE_OptDissConnectLaterHour,   entry._dbxtuneOptDissConnectLaterHour);  // should the be in here???
//				entry._dbxtuneOptDissConnectLaterMinute  = getValue(element, XML_TDS_DBXTUNE_OptDissConnectLaterMinute, entry._dbxtuneOptDissConnectLaterMinute);  // should the be in here???
//	
//				// HostMonitor
//				if (entry._dbxtuneOptOsMonitoring)
//				{
//					entry._osMonUsername                 = getValue(element, XML_TDS_DBXTUNE_osMonUsername,     entry._osMonUsername);
//					entry._osMonSavePassword             = getValue(element, XML_TDS_DBXTUNE_osMonSavePassword, entry._osMonSavePassword); // Should this be here???
//					if (entry._osMonSavePassword)
//						entry._osMonPassword             = getValue(element, XML_TDS_DBXTUNE_osMonPassword,     entry._osMonPassword);
//					entry._osMonHost                     = getValue(element, XML_TDS_DBXTUNE_osMonHost,         entry._osMonHost);
//					entry._osMonPort                     = getValue(element, XML_TDS_DBXTUNE_osMonPort,         entry._osMonPort);
//				}
//	
//				// Recordings
//				if (entry._dbxtuneOptRecordSession)
//				{
//					entry._pcsWriterClass                    = getValue(element, XML_TDS_DBXTUNE_pcsWriterClass,                    entry._pcsWriterClass);
//					entry._pcsWriterDriver                   = getValue(element, XML_TDS_DBXTUNE_pcsWriterDriver,                   entry._pcsWriterDriver);
//					entry._pcsWriterUrl                      = getValue(element, XML_TDS_DBXTUNE_pcsWriterUrl,                      entry._pcsWriterUrl);
//					entry._pcsWriterUsername                 = getValue(element, XML_TDS_DBXTUNE_pcsWriterUsername,                 entry._pcsWriterUsername);
//					entry._pcsWriterSavePassword             = getValue(element, XML_TDS_DBXTUNE_pcsWriterSavePassword,             entry._pcsWriterSavePassword);
//					if (entry._pcsWriterSavePassword)
//						entry._pcsWriterPassword             = getValue(element, XML_TDS_DBXTUNE_pcsWriterPassword,                 entry._pcsWriterPassword);
//					entry._pcsWriterStartH2asNwServer        = getValue(element, XML_TDS_DBXTUNE_pcsWriterStartH2asNwServer,        entry._pcsWriterStartH2asNwServer);
//					entry._pcsWriterDdlLookup                = getValue(element, XML_TDS_DBXTUNE_pcsWriterDdlLookup,                entry._pcsWriterDdlLookup);
//					entry._pcsWriterDdlStoreDependantObjects = getValue(element, XML_TDS_DBXTUNE_pcsWriterDdlStoreDependantObjects, entry._pcsWriterDdlStoreDependantObjects);
//					entry._pcsWriterDdlLookupSleepTime       = getValue(element, XML_TDS_DBXTUNE_pcsWriterDdlLookupSleepTime,       entry._pcsWriterDdlLookupSleepTime);
//	//				entry._pcsWriterCounterDetailes          = getValue(element, XML_TDS_DBXTUNE_pcsWriterCounterDetailes,          entry._pcsWriterCounterDetailes);
//				}
//				// Load Recordings
//	//			entry._pcsReaderDriver                   = getValue(element, XML_TDS_DBXTUNE_pcsReaderDriver,             entry._pcsReaderDriver);
//	//			entry._pcsReaderUrl                      = getValue(element, XML_TDS_DBXTUNE_pcsReaderUrl,                entry._pcsReaderUrl);
//	//			entry._pcsReaderUsername                 = getValue(element, XML_TDS_DBXTUNE_pcsReaderUsername,           entry._pcsReaderUsername);
//	//			entry._pcsReaderPassword                 = getValue(element, XML_TDS_DBXTUNE_pcsReaderPassword,           entry._pcsReaderPassword);
//	//			entry._pcsReaderCheckForNewSamples       = getValue(element, XML_TDS_DBXTUNE_pcsReaderCheckForNewSamples, entry._pcsReaderCheckForNewSamples);
//	//			entry._pcsReaderStartH2asNwServer        = getValue(element, XML_TDS_DBXTUNE_pcsReaderStartH2asNwServer,  entry._pcsReaderStartH2asNwServer);
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

		public boolean       _isDbxTuneParamsValid   = false; 
		public DbxTuneParams _dbxtuneParams          = null; 


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

		/**
		 * if the previously stored profile IS an DbxTune Profile<br>
		 * and we are using SQLWindow, we need to copy the "hidden" fields into the passed ConnectionProfileEntry<br>
		 * otherwise we will lose that data the next time we use the entry from DbxTune
		 * 
		 * @param storedJdbcEntry entry to copy from
		 */
		public void copyDbxTuneParams(JdbcEntry storedJdbcEntry)
		{
			//System.out.println("copyDbxTuneParams: from '"+fromEntry+"', to this '"+this+"'.");
			_isDbxTuneParamsValid              = storedJdbcEntry._isDbxTuneParamsValid; 
			_dbxtuneParams                     = new DbxTuneParams(storedJdbcEntry._dbxtuneParams); // we probably need to create a NEW object here
		}

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

			if (_isDbxTuneParamsValid || entry._isDbxTuneParamsValid)
			{
				if (_dbxtuneParams == null)
					_dbxtuneParams = DBXTUNE_PARAMS_EMPTY;

				sb.append(_dbxtuneParams.getDifference(entry._dbxtuneParams));
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
			htmlTabRow(sb, XML_JDBC_DRIVER       , _jdbcDriver);
			htmlTabRow(sb, XML_JDBC_URL          , _jdbcUrl);
			htmlTabRow(sb, XML_JDBC_USERNAME     , _jdbcUsername);
			if (_jdbcSavePassword)
			htmlTabRow(sb, XML_JDBC_PASSWORD     , !_logger.isDebugEnabled() ? "**secret**" : _jdbcPassword);
			htmlTabRow(sb, XML_JDBC_SAVE_PASSWORD, _jdbcSavePassword);
			htmlTabRow(sb, XML_JDBC_SqlInit      , _jdbcSqlInit);
			htmlTabRow(sb, XML_JDBC_UrlOptions   , _jdbcUrlOptions);
			htmlTabRow(sb, XML_JDBC_ShhTunnelInfo, _jdbcShhTunnelInfo);
			sb.append("</table>");

			if (_isDbxTuneParamsValid)
			{
				if (_dbxtuneParams != null)
					sb.append(_dbxtuneParams.toHtml());
			}

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
			
			StringUtil.xmlTag(sb, 8, XML_TDS_IsDbxTuneParamsValid, _isDbxTuneParamsValid);
			if (_isDbxTuneParamsValid)
			{
				if (_dbxtuneParams != null)
					sb.append(_dbxtuneParams.toXml());
			}

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

			entry._isDbxTuneParamsValid = getValue(element, XML_TDS_IsDbxTuneParamsValid, entry._isDbxTuneParamsValid);
			if (entry._isDbxTuneParamsValid)
			{
				entry._dbxtuneParams = DbxTuneParams.parseXml(element);
			}

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
			htmlTabRow(sb, XML_OFFLINE_PASSWORD             , !_logger.isDebugEnabled() ? "**secret**" : _jdbcPassword);
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

//	private static boolean getValue(Element element, String tagName, boolean defaultVal)
//	{
//		String retValue = null;
//		NodeList nodeList = element.getElementsByTagName(tagName);
//		if (nodeList.getLength() > 0)
//			retValue = nodeList.item(0).getTextContent();
//		
//		if (StringUtil.hasValue(retValue))
//			return "true".equalsIgnoreCase(retValue);
//
//		return defaultVal;
//	}
	private static boolean getValue(Element element, String tagName, boolean defaultVal)
	{
		return getValue(element, tagName, null, defaultVal);
	}
	private static boolean getValue(Element element, String tagName, String oldTagName, boolean defaultVal)
	{
		String retValue = null;
		NodeList nodeList = element.getElementsByTagName(tagName);
		if (nodeList.getLength() > 0)
		{
			retValue = nodeList.item(0).getTextContent();
		}
		else if (StringUtil.hasValue(oldTagName)) // if the TAG wasn't found: Check backward compatibility TAGNAME
		{
			nodeList = element.getElementsByTagName(oldTagName);
			if (nodeList.getLength() > 0)
				retValue = nodeList.item(0).getTextContent();
		}
			
		
		if (StringUtil.hasValue(retValue))
			return "true".equalsIgnoreCase(retValue);

		return defaultVal;
	}
}

package com.asetune.gui;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.swing.ImageIcon;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.utils.DbUtils;
import com.asetune.utils.SwingUtils;

public class ConnectionProfileManager
{
	private static Logger _logger = Logger.getLogger(ConnectionProfileManager.class);
	private static final long serialVersionUID = 1L;

	/** */
	private LinkedHashMap<String, ConnectionProfile> _profileMap = new LinkedHashMap<String, ConnectionProfile>();

	/** Singleton */
	private static ConnectionProfileManager _instance = null;

	/** default file name where the connection profiles are stored */
	private final static String DEFAULT_STORAGE_FILE = Version.APP_STORE_DIR + File.separator + "ConnectionProfiles.xml";

	
	public enum SrvType 
	{ 
		TDS_ASE, 
		TDS_ASA, 
		TDS_IQ, 
		TDS_RS, 
		TDS_OTHER, 

		JDBC_HANA,
		JDBC_H2,
		JDBC_HSQL,
		JDBC_MSSQL,
		JDBC_ORACLE,
		JDBC_DB2_UX,
		JDBC_DB2_ZOS,
		JDBC_MYSQL,
		JDBC_DERBY,
		JDBC_OTHER;
	}

	// Sybase products
	public static final ImageIcon ICON_DB_PROD_NAME_SYBASE_ASE   = SwingUtils.readImageIcon(Version.class, "images/conn_profile_ase_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_SYBASE_ASA   = SwingUtils.readImageIcon(Version.class, "images/conn_profile_asa_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_SYBASE_IQ    = SwingUtils.readImageIcon(Version.class, "images/conn_profile_iq_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_SYBASE_RS    = SwingUtils.readImageIcon(Version.class, "images/conn_profile_rs_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_SYBASE_OTHER = SwingUtils.readImageIcon(Version.class, "images/conn_profile_tds_other_16.png");

	// SAP products
	public static final ImageIcon ICON_DB_PROD_NAME_HANA         = SwingUtils.readImageIcon(Version.class, "images/conn_profile_hana_16.png");

	// JDBC products
	public static final ImageIcon ICON_DB_PROD_NAME_H2           = SwingUtils.readImageIcon(Version.class, "images/conn_profile_h2_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_HSQL         = SwingUtils.readImageIcon(Version.class, "images/conn_profile_hsql_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_MSSQL        = SwingUtils.readImageIcon(Version.class, "images/conn_profile_mssql_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_ORACLE       = SwingUtils.readImageIcon(Version.class, "images/conn_profile_oracle_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_DB2_UX       = SwingUtils.readImageIcon(Version.class, "images/conn_profile_db2_ux_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_DB2_ZOS      = SwingUtils.readImageIcon(Version.class, "images/conn_profile_db2_zos_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_MYSQL        = SwingUtils.readImageIcon(Version.class, "images/conn_profile_mysql_16.png");
	public static final ImageIcon ICON_DB_PROD_NAME_DERBY        = SwingUtils.readImageIcon(Version.class, "images/conn_profile_derby_16.png");

	public static final ImageIcon ICON_DB_PROD_NAME_OTHER        = SwingUtils.readImageIcon(Version.class, "images/conn_profile_unknown_vendor_16.png");
	
	
	//---------------------------------------------------------------
	// BEGIN: Constructors
	//---------------------------------------------------------------
	public ConnectionProfileManager(String file)
	{
	}
	//---------------------------------------------------------------
	// END: Constructors
	//---------------------------------------------------------------

	//---------------------------------------------------------------
	// BEGIN: Singleton
	//---------------------------------------------------------------
	public static ConnectionProfileManager getInstance()
	{
		if ( _instance == null )
		{
			_logger.debug("No Connection Profile has yet been initiated, creating one now using file '"+DEFAULT_STORAGE_FILE+"'.");
			_instance = new ConnectionProfileManager(DEFAULT_STORAGE_FILE);
		}
		return _instance;
	}

	public static boolean hasInstance(String confName)
	{
		return _instance != null;
	}

	public static void setInstance(ConnectionProfileManager connProfile)
	{
		_instance = connProfile;
	}
	//---------------------------------------------------------------
	// END: Singleton
	//---------------------------------------------------------------

	/**
	 * A ConnectionProfile object, associated to the name
	 * @param name name of the profile we want to get
	 * @return A connection object, if not found null will be returned
	 */
	public ConnectionProfile getProfile(String name)
	{
		return _profileMap.get(name);
	}
	
	/**
	 * Get all saved ConnectionProfile objects
	 * @return a LinkedHasMap of <name,ConnectionProfile> (will never be null, if none found it will be an empty list)
	 */
	public LinkedHashMap<String, ConnectionProfile> getProfiles()
	{
		return _profileMap;
	}

	/**
	 * Get saved Connection Profiles
	 * @return a Set of Strings (will never be null, if none found it will be an empty list)
	 */
	public Set<String> getProfileNames()
	{
		return _profileMap.keySet();
	}

	public static ImageIcon getIcon(String productName)
	{
		if      (DbUtils.DB_PROD_NAME_DB2_UX    .equals(productName)) return ICON_DB_PROD_NAME_DB2_UX;
		else if (DbUtils.DB_PROD_NAME_DB2_ZOS   .equals(productName)) return ICON_DB_PROD_NAME_DB2_ZOS;
		else if (DbUtils.DB_PROD_NAME_DERBY     .equals(productName)) return ICON_DB_PROD_NAME_DERBY;
		else if (DbUtils.DB_PROD_NAME_H2        .equals(productName)) return ICON_DB_PROD_NAME_H2;
		else if (DbUtils.DB_PROD_NAME_HANA      .equals(productName)) return ICON_DB_PROD_NAME_HANA;
		else if (DbUtils.DB_PROD_NAME_HSQL      .equals(productName)) return ICON_DB_PROD_NAME_HSQL;
		else if (DbUtils.DB_PROD_NAME_MSSQL        .equals(productName)) return ICON_DB_PROD_NAME_MSSQL;
		else if (DbUtils.DB_PROD_NAME_MYSQL     .equals(productName)) return ICON_DB_PROD_NAME_MYSQL;
		else if (DbUtils.DB_PROD_NAME_ORACLE    .equals(productName)) return ICON_DB_PROD_NAME_ORACLE;
		
		else if (DbUtils.DB_PROD_NAME_SYBASE_ASA.equals(productName)) return ICON_DB_PROD_NAME_SYBASE_ASA;
		else if (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(productName)) return ICON_DB_PROD_NAME_SYBASE_ASE;
		else if (DbUtils.DB_PROD_NAME_SYBASE_IQ .equals(productName)) return ICON_DB_PROD_NAME_SYBASE_IQ;
		else if (DbUtils.DB_PROD_NAME_SYBASE_RS .equals(productName)) return ICON_DB_PROD_NAME_SYBASE_RS;

		return ICON_DB_PROD_NAME_OTHER;
	}
	public static ImageIcon getIcon(SrvType srvType)
	{
		if      (SrvType.JDBC_DB2_UX .equals(srvType)) return ICON_DB_PROD_NAME_DB2_UX;
		else if (SrvType.JDBC_DB2_ZOS.equals(srvType)) return ICON_DB_PROD_NAME_DB2_ZOS;
		else if (SrvType.JDBC_DERBY  .equals(srvType)) return ICON_DB_PROD_NAME_DERBY;
		else if (SrvType.JDBC_H2     .equals(srvType)) return ICON_DB_PROD_NAME_H2;
		else if (SrvType.JDBC_HANA   .equals(srvType)) return ICON_DB_PROD_NAME_HANA;
		else if (SrvType.JDBC_HSQL   .equals(srvType)) return ICON_DB_PROD_NAME_HSQL;
		else if (SrvType.JDBC_MSSQL  .equals(srvType)) return ICON_DB_PROD_NAME_MSSQL;
		else if (SrvType.JDBC_MYSQL  .equals(srvType)) return ICON_DB_PROD_NAME_MYSQL;
		else if (SrvType.JDBC_ORACLE .equals(srvType)) return ICON_DB_PROD_NAME_ORACLE;

		else if (SrvType.TDS_ASA     .equals(srvType)) return ICON_DB_PROD_NAME_SYBASE_ASA;
		else if (SrvType.TDS_ASE     .equals(srvType)) return ICON_DB_PROD_NAME_SYBASE_ASE;
		else if (SrvType.TDS_IQ      .equals(srvType)) return ICON_DB_PROD_NAME_SYBASE_IQ;
		else if (SrvType.TDS_RS      .equals(srvType)) return ICON_DB_PROD_NAME_SYBASE_RS;
		else if (SrvType.TDS_OTHER   .equals(srvType)) return ICON_DB_PROD_NAME_SYBASE_OTHER;

		return ICON_DB_PROD_NAME_OTHER;
	}

}

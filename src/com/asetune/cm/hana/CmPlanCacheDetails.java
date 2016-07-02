package com.asetune.cm.hana;

import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.hana.gui.CmPlanCacheDetailsPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPlanCacheDetails
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmPlanCacheOverview.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPlanCacheDetails.class.getSimpleName();
	public static final String   SHORT_NAME       = "Statement Cache Details";
	public static final String   HTML_DESC        = 
		"<html>" +
			"Get detailed statistics about each SQL statement in the cache.<br>" +
			"This also includes the SQL statement itself<br>" +
			"And the execution plan (showplan) for the SQL statement<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"M_SQL_PLAN_CACHE"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
//		"HOST",                                    // java.sql.Types.VARCHAR   VARCHAR(64)      
//		"PORT",                                    // java.sql.Types.INTEGER   INTEGER          
//		"VOLUME_ID",                               // java.sql.Types.INTEGER   INTEGER          
//		"STATEMENT_STRING",                        // java.sql.Types.NCLOB     NCLOB            
//		"STATEMENT_HASH",                          // java.sql.Types.VARCHAR   VARCHAR(32)      
//		"USER_NAME",                               // java.sql.Types.NVARCHAR  NVARCHAR(256)    
//		"SESSION_USER_NAME",                       // java.sql.Types.NVARCHAR  NVARCHAR(256)    
//		"SCHEMA_NAME",                             // java.sql.Types.NVARCHAR  NVARCHAR(256)    
//		"IS_VALID",                                // java.sql.Types.VARCHAR   VARCHAR(5)       
//		"IS_INTERNAL",                             // java.sql.Types.VARCHAR   VARCHAR(5)       
//		"IS_DISTRIBUTED_EXECUTION",                // java.sql.Types.VARCHAR   VARCHAR(5)       
//		"IS_PINNED_PLAN",                          // java.sql.Types.VARCHAR   VARCHAR(5)       
//		"PINNED_PLAN_ID",                          // java.sql.Types.BIGINT    BIGINT           
//		"ABAP_VARCHAR_MODE",                       // java.sql.Types.VARCHAR   VARCHAR(5)       
//		"ACCESSED_TABLES",                         // java.sql.Types.VARCHAR   VARCHAR(5000)    
//		"ACCESSED_TABLE_NAMES",                    // java.sql.Types.NVARCHAR  NVARCHAR(5000)   
//		"ACCESSED_OBJECTS",                        // java.sql.Types.VARCHAR   VARCHAR(5000)    
//		"ACCESSED_OBJECT_NAMES",                   // java.sql.Types.NVARCHAR  NVARCHAR(5000)   
//		"TABLE_LOCATIONS",                         // java.sql.Types.VARCHAR   VARCHAR(2000)    
//		"TABLE_TYPES",                             // java.sql.Types.VARCHAR   VARCHAR(128)     
//		"PLAN_SHARING_TYPE",                       // java.sql.Types.VARCHAR   VARCHAR(128)     
//		"OWNER_CONNECTION_ID",                     // java.sql.Types.INTEGER   INTEGER          
//		"PLAN_ID",                                 // java.sql.Types.BIGINT    BIGINT           
//		"PLAN_MEMORY_SIZE",                        // java.sql.Types.BIGINT    BIGINT           
		"REFERENCE_COUNT",                         // java.sql.Types.BIGINT    BIGINT           
//		"PARAMETER_COUNT",                         // java.sql.Types.BIGINT    BIGINT           
//		"UPDATED_TABLE_OID",                       // java.sql.Types.BIGINT    BIGINT           
		"EXECUTION_COUNT",                         // java.sql.Types.BIGINT    BIGINT           
		"EXECUTION_COUNT_BY_ROUTING",              // java.sql.Types.BIGINT    BIGINT           
		"TOTAL_CURSOR_DURATION",                   // java.sql.Types.BIGINT    BIGINT           
//		"AVG_CURSOR_DURATION",                     // java.sql.Types.BIGINT    BIGINT           
//		"MIN_CURSOR_DURATION",                     // java.sql.Types.BIGINT    BIGINT           
//		"MAX_CURSOR_DURATION",                     // java.sql.Types.BIGINT    BIGINT           
		"TOTAL_EXECUTION_TIME",                    // java.sql.Types.BIGINT    BIGINT           
//		"AVG_EXECUTION_TIME",                      // java.sql.Types.BIGINT    BIGINT           
//		"MIN_EXECUTION_TIME",                      // java.sql.Types.BIGINT    BIGINT           
//		"MAX_EXECUTION_TIME",                      // java.sql.Types.BIGINT    BIGINT           
		"TOTAL_EXECUTION_OPEN_TIME",               // java.sql.Types.BIGINT    BIGINT           
//		"AVG_EXECUTION_OPEN_TIME",                 // java.sql.Types.BIGINT    BIGINT           
//		"MIN_EXECUTION_OPEN_TIME",                 // java.sql.Types.BIGINT    BIGINT           
//		"MAX_EXECUTION_OPEN_TIME",                 // java.sql.Types.BIGINT    BIGINT           
		"TOTAL_EXECUTION_FETCH_TIME",              // java.sql.Types.BIGINT    BIGINT           
//		"AVG_EXECUTION_FETCH_TIME",                // java.sql.Types.BIGINT    BIGINT           
//		"MIN_EXECUTION_FETCH_TIME",                // java.sql.Types.BIGINT    BIGINT           
//		"MAX_EXECUTION_FETCH_TIME",                // java.sql.Types.BIGINT    BIGINT           
		"TOTAL_EXECUTION_CLOSE_TIME",              // java.sql.Types.BIGINT    BIGINT           
//		"AVG_EXECUTION_CLOSE_TIME",                // java.sql.Types.BIGINT    BIGINT           
//		"MIN_EXECUTION_CLOSE_TIME",                // java.sql.Types.BIGINT    BIGINT           
//		"MAX_EXECUTION_CLOSE_TIME",                // java.sql.Types.BIGINT    BIGINT           
		"TOTAL_METADATA_CACHE_MISS_COUNT",         // java.sql.Types.BIGINT    BIGINT           
		"TOTAL_TABLE_LOAD_TIME_DURING_PREPARATION",// java.sql.Types.BIGINT    BIGINT           
		"AVG_TABLE_LOAD_TIME_DURING_PREPARATION",  // java.sql.Types.BIGINT    BIGINT           
		"MIN_TABLE_LOAD_TIME_DURING_PREPARATION",  // java.sql.Types.BIGINT    BIGINT           
		"MAX_TABLE_LOAD_TIME_DURING_PREPARATION",  // java.sql.Types.BIGINT    BIGINT           
		"PREPARATION_COUNT",                       // java.sql.Types.BIGINT    BIGINT           
		"TOTAL_PREPARATION_TIME",                  // java.sql.Types.BIGINT    BIGINT           
//		"AVG_PREPARATION_TIME",                    // java.sql.Types.BIGINT    BIGINT           
//		"MIN_PREPARATION_TIME",                    // java.sql.Types.BIGINT    BIGINT           
//		"MAX_PREPARATION_TIME",                    // java.sql.Types.BIGINT    BIGINT           
		"TOTAL_RESULT_RECORD_COUNT",               // java.sql.Types.BIGINT    BIGINT           
		"TOTAL_LOCK_WAIT_COUNT",                   // java.sql.Types.BIGINT    BIGINT           
		"TOTAL_LOCK_WAIT_DURATION",                // java.sql.Types.BIGINT    BIGINT           
//		"LAST_CONNECTION_ID",                      // java.sql.Types.INTEGER   INTEGER          
//		"LAST_EXECUTION_TIMESTAMP",                // java.sql.Types.TIMESTAMP TIMESTAMP        
//		"LAST_PREPARATION_TIMESTAMP",              // java.sql.Types.TIMESTAMP TIMESTAMP        
		"-last-dummy-col-"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.LARGE; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmPlanCacheDetails(counterController, guiController);
	}

	public CmPlanCacheDetails(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                      = CM_NAME;

	public static final String  PROPKEY_sample_extraWhereClause   = PROP_PREFIX + ".sample.extraWhereClause";
	public static final String  DEFAULT_sample_extraWhereClause   = "";

	public static final String  PROPKEY_sample_afterPrevSample    = PROP_PREFIX + ".sample.afterPrevSample";
	public static final boolean DEFAULT_sample_afterPrevSample    = false;

	public static final String  PROPKEY_sample_lastXminutes       = PROP_PREFIX + ".sample.lastXminutes";
	public static final boolean DEFAULT_sample_lastXminutes       = true;

	public static final String  PROPKEY_sample_lastXminutesTime   = PROP_PREFIX + ".sample.lastXminutes.time";
	public static final int     DEFAULT_sample_lastXminutesTime   = 10;


	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_extraWhereClause, DEFAULT_sample_extraWhereClause);
		Configuration.registerDefaultValue(PROPKEY_sample_afterPrevSample,  DEFAULT_sample_afterPrevSample);
		Configuration.registerDefaultValue(PROPKEY_sample_lastXminutes,     DEFAULT_sample_lastXminutes);
		Configuration.registerDefaultValue(PROPKEY_sample_lastXminutesTime, DEFAULT_sample_lastXminutesTime);
	}


	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public Configuration getLocalConfiguration()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		Configuration lc = new Configuration();

		lc.setProperty(PROPKEY_sample_extraWhereClause, conf.getProperty       (PROPKEY_sample_extraWhereClause, DEFAULT_sample_extraWhereClause));
		lc.setProperty(PROPKEY_sample_afterPrevSample,  conf.getBooleanProperty(PROPKEY_sample_afterPrevSample,  DEFAULT_sample_afterPrevSample));
		lc.setProperty(PROPKEY_sample_lastXminutes,     conf.getBooleanProperty(PROPKEY_sample_lastXminutes,     DEFAULT_sample_lastXminutes));
		lc.setProperty(PROPKEY_sample_lastXminutesTime, conf.getIntProperty    (PROPKEY_sample_lastXminutesTime, DEFAULT_sample_lastXminutesTime));
		
		return lc;
	}

	@Override
	public String getLocalConfigurationDescription(String propName)
	{
		if (propName.equals(PROPKEY_sample_extraWhereClause)) return CmPlanCacheDetailsPanel.TOOLTIP_sample_extraWhereClause;
		if (propName.equals(PROPKEY_sample_afterPrevSample))  return CmPlanCacheDetailsPanel.TOOLTIP_sample_afterPrevSample;
		if (propName.equals(PROPKEY_sample_lastXminutes))     return CmPlanCacheDetailsPanel.TOOLTIP_sample_lastXminutes;
		if (propName.equals(PROPKEY_sample_lastXminutesTime)) return CmPlanCacheDetailsPanel.TOOLTIP_sample_lastXminutesTime;
	
		return "";
	}
	@Override
	public String getLocalConfigurationDataType(String propName)
	{
		if (propName.equals(PROPKEY_sample_extraWhereClause)) return String .class.getSimpleName();
		if (propName.equals(PROPKEY_sample_afterPrevSample))  return Boolean.class.getSimpleName();
		if (propName.equals(PROPKEY_sample_lastXminutes))     return Boolean.class.getSimpleName();
		if (propName.equals(PROPKEY_sample_lastXminutesTime)) return Integer.class.getSimpleName();

		return "";
	}

	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmPlanCacheDetailsPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

//	@Override
//	public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
//	{
//	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("HOST");
		pkCols.add("PORT");
		pkCols.add("PLAN_ID");
//		pkCols.add("STATEMENT_HASH");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String  sample_extraWhereClause   = conf.getProperty(PROPKEY_sample_extraWhereClause, DEFAULT_sample_extraWhereClause);
		boolean sample_lastXminutes     = conf.getBooleanProperty(PROPKEY_sample_lastXminutes,     DEFAULT_sample_lastXminutes);
		int     sample_lastXminutesTime = conf.getIntProperty(    PROPKEY_sample_lastXminutesTime, DEFAULT_sample_lastXminutesTime);

		// Do we have extra where clauses
		String sql_sample_extraWhereClause = "  -- Extra where clauses will go here. (it will look like: AND the_extra_where_clause) \n";
		if ( ! StringUtil.isNullOrBlank(sample_extraWhereClause) )
			sql_sample_extraWhereClause = "  AND " + sample_extraWhereClause + "\n";

		String sql_sample_lastXminutes = "";
		if (sample_lastXminutes)
			sql_sample_lastXminutes = "  AND LAST_EXECUTION_TIMESTAMP > ADD_SECONDS(current_timestamp, -("+sample_lastXminutesTime+"*60))\n";

		String sql = 
			"select \n" +
			"    HOST, \n" +                                         // java.sql.Types.VARCHAR   VARCHAR(64)    
			"    PORT, \n" +                                         // java.sql.Types.INTEGER   INTEGER        
			"    PLAN_ID, \n" +                                      // java.sql.Types.BIGINT    BIGINT         
			"    VOLUME_ID, \n" +                                    // java.sql.Types.INTEGER   INTEGER        
			"    USER_NAME, \n" +                                    // java.sql.Types.NVARCHAR  NVARCHAR(256)  
			"    SESSION_USER_NAME, \n" +                            // java.sql.Types.NVARCHAR  NVARCHAR(256)  
			"    SCHEMA_NAME, \n" +                                  // java.sql.Types.NVARCHAR  NVARCHAR(256)  
			"    IS_VALID, \n" +                                     // java.sql.Types.VARCHAR   VARCHAR(5)     
			"    IS_INTERNAL, \n" +                                  // java.sql.Types.VARCHAR   VARCHAR(5)     
			"    IS_DISTRIBUTED_EXECUTION, \n" +                     // java.sql.Types.VARCHAR   VARCHAR(5)     
			"    TABLE_TYPES, \n" +                                  // java.sql.Types.VARCHAR   VARCHAR(128)   
			
			"    TOTAL_EXECUTION_TIME, \n" +                         // java.sql.Types.BIGINT    BIGINT         
			"    AVG_EXECUTION_TIME, \n" +                           // java.sql.Types.BIGINT    BIGINT         
			"    MIN_EXECUTION_TIME, \n" +                           // java.sql.Types.BIGINT    BIGINT         
			"    MAX_EXECUTION_TIME, \n" +                           // java.sql.Types.BIGINT    BIGINT         

			"    LAST_EXECUTION_TIMESTAMP, \n" +                     // java.sql.Types.TIMESTAMP TIMESTAMP      
			"    SECONDS_BETWEEN(IFNULL(LAST_EXECUTION_TIMESTAMP,LAST_PREPARATION_TIMESTAMP), CURRENT_TIMESTAMP) as LAST_EXECUTION_TIMESTAMP_SS, \n" +
			"    EXECUTION_COUNT, \n" +                              // java.sql.Types.BIGINT    BIGINT         
			"    STATEMENT_STRING, \n" +                             // java.sql.Types.NCLOB     NCLOB          

			"    IS_PINNED_PLAN, \n" +                               // java.sql.Types.VARCHAR   VARCHAR(5)     
			"    PINNED_PLAN_ID, \n" +                               // java.sql.Types.BIGINT    BIGINT         
			"    ABAP_VARCHAR_MODE, \n" +                            // java.sql.Types.VARCHAR   VARCHAR(5)     
			"    PLAN_SHARING_TYPE, \n" +                            // java.sql.Types.VARCHAR   VARCHAR(128)   
			"    OWNER_CONNECTION_ID, \n" +                          // java.sql.Types.INTEGER   INTEGER        
			"    PLAN_MEMORY_SIZE, \n" +                             // java.sql.Types.BIGINT    BIGINT         
			"    REFERENCE_COUNT, \n" +                              // java.sql.Types.BIGINT    BIGINT         
			"    PARAMETER_COUNT, \n" +                              // java.sql.Types.BIGINT    BIGINT         
			"    UPDATED_TABLE_OID, \n" +                            // java.sql.Types.BIGINT    BIGINT         
			"    EXECUTION_COUNT_BY_ROUTING, \n" +                   // java.sql.Types.BIGINT    BIGINT         
			"    TOTAL_CURSOR_DURATION, \n" +                        // java.sql.Types.BIGINT    BIGINT         
			"    AVG_CURSOR_DURATION, \n" +                          // java.sql.Types.BIGINT    BIGINT         
			"    MIN_CURSOR_DURATION, \n" +                          // java.sql.Types.BIGINT    BIGINT         
			"    MAX_CURSOR_DURATION, \n" +                          // java.sql.Types.BIGINT    BIGINT         
			"    TOTAL_EXECUTION_OPEN_TIME, \n" +                    // java.sql.Types.BIGINT    BIGINT         
			"    AVG_EXECUTION_OPEN_TIME, \n" +                      // java.sql.Types.BIGINT    BIGINT         
			"    MIN_EXECUTION_OPEN_TIME, \n" +                      // java.sql.Types.BIGINT    BIGINT         
			"    MAX_EXECUTION_OPEN_TIME, \n" +                      // java.sql.Types.BIGINT    BIGINT         
			"    TOTAL_EXECUTION_FETCH_TIME, \n" +                   // java.sql.Types.BIGINT    BIGINT         
			"    AVG_EXECUTION_FETCH_TIME, \n" +                     // java.sql.Types.BIGINT    BIGINT         
			"    MIN_EXECUTION_FETCH_TIME, \n" +                     // java.sql.Types.BIGINT    BIGINT         
			"    MAX_EXECUTION_FETCH_TIME, \n" +                     // java.sql.Types.BIGINT    BIGINT         
			"    TOTAL_EXECUTION_CLOSE_TIME, \n" +                   // java.sql.Types.BIGINT    BIGINT         
			"    AVG_EXECUTION_CLOSE_TIME, \n" +                     // java.sql.Types.BIGINT    BIGINT         
			"    MIN_EXECUTION_CLOSE_TIME, \n" +                     // java.sql.Types.BIGINT    BIGINT         
			"    MAX_EXECUTION_CLOSE_TIME, \n" +                     // java.sql.Types.BIGINT    BIGINT         
			"    TOTAL_METADATA_CACHE_MISS_COUNT, \n" +              // java.sql.Types.BIGINT    BIGINT         
			"    TOTAL_TABLE_LOAD_TIME_DURING_PREPARATION, \n" +     // java.sql.Types.BIGINT    BIGINT         
			"    AVG_TABLE_LOAD_TIME_DURING_PREPARATION, \n" +       // java.sql.Types.BIGINT    BIGINT         
			"    MIN_TABLE_LOAD_TIME_DURING_PREPARATION, \n" +       // java.sql.Types.BIGINT    BIGINT         
			"    MAX_TABLE_LOAD_TIME_DURING_PREPARATION, \n" +       // java.sql.Types.BIGINT    BIGINT         
			"    PREPARATION_COUNT, \n" +                            // java.sql.Types.BIGINT    BIGINT         
			"    TOTAL_PREPARATION_TIME, \n" +                       // java.sql.Types.BIGINT    BIGINT         
			"    AVG_PREPARATION_TIME, \n" +                         // java.sql.Types.BIGINT    BIGINT         
			"    MIN_PREPARATION_TIME, \n" +                         // java.sql.Types.BIGINT    BIGINT         
			"    MAX_PREPARATION_TIME, \n" +                         // java.sql.Types.BIGINT    BIGINT         
			"    TOTAL_RESULT_RECORD_COUNT, \n" +                    // java.sql.Types.BIGINT    BIGINT         
			"    TOTAL_LOCK_WAIT_COUNT, \n" +                        // java.sql.Types.BIGINT    BIGINT         
			"    TOTAL_LOCK_WAIT_DURATION, \n" +                     // java.sql.Types.BIGINT    BIGINT         
			"    LAST_CONNECTION_ID, \n" +                           // java.sql.Types.INTEGER   INTEGER        
			"    LAST_PREPARATION_TIMESTAMP, \n" +                   // java.sql.Types.TIMESTAMP TIMESTAMP      
			"    SECONDS_BETWEEN(LAST_PREPARATION_TIMESTAMP, CURRENT_TIMESTAMP) as LAST_PREPARATION_TIMESTAMP_SS, \n" +
			"    TABLE_LOCATIONS, \n" +                              // java.sql.Types.VARCHAR   VARCHAR(2000)  
			"    ACCESSED_TABLES, \n" +                              // java.sql.Types.VARCHAR   VARCHAR(5000)  
			"    ACCESSED_TABLE_NAMES, \n" +                         // java.sql.Types.NVARCHAR  NVARCHAR(5000) 
			"    ACCESSED_OBJECTS, \n" +                             // java.sql.Types.VARCHAR   VARCHAR(5000)  
			"    ACCESSED_OBJECT_NAMES,  \n" +                       // java.sql.Types.NVARCHAR  NVARCHAR(5000) 
			"    STATEMENT_HASH \n" +                                // java.sql.Types.VARCHAR   VARCHAR(32)    
			"FROM M_SQL_PLAN_CACHE S \n" +
			"WHERE 1 = 1 -- to make extra where clauses easier \n" +
			sql_sample_extraWhereClause +
			sql_sample_lastXminutes;

		// maybe: where last_execution_time > ${prevSampleTime} -- this to just get what has been executed since last "poll" 
		// Or top ### order by LAST_PREPARATION_TIMESTAMP desc
		return sql;
	}

	@Override
	public String getSql()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sample_afterPrevSample = conf.getBooleanProperty(PROPKEY_sample_afterPrevSample, DEFAULT_sample_afterPrevSample);

		if (sample_afterPrevSample)
		{
    		Timestamp prevSample = getPreviousSampleTime();
    		if (prevSample == null)
    			setSqlWhere("AND 1=0"); // do not get any rows for the first sample...
    		else
    			setSqlWhere("AND LAST_EXECUTION_TIMESTAMP > '"+prevSample+"' "); 
		}
		else
			setSqlWhere("");

		// Now get the SQL from super method...
		return super.getSql();
	}
	
	
	
	
	
	
	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
//     TABLE_LOCATIONS, 
//     STATEMENT_STRING, 
//     ACCESSED_TABLES, 
//     ACCESSED_TABLE_NAMES, 
//     ACCESSED_OBJECTS, 
//     ACCESSED_OBJECT_NAMES,  	

		// MON SQL TEXT
		if ("STATEMENT_STRING".equals(colName))
		{
			if (cellValue instanceof String)
			{
				return toHtmlString((String) cellValue);
			}
		}

		if (    "TABLE_LOCATIONS"      .equals(colName)
		     || "ACCESSED_TABLES"      .equals(colName)
		     || "ACCESSED_TABLE_NAMES" .equals(colName)
		     || "ACCESSED_OBJECTS"     .equals(colName)
		     || "ACCESSED_OBJECT_NAMES".equals(colName)
		   )
		{
			if (cellValue != null && cellValue instanceof String)
			{
				return toHtmlString( cellValue.toString().replace(",", "<br>"));
			}
		}
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
	/** add HTML around the string, and translate linebreaks into <br> */
	private String toHtmlString(String in)
	{
		String str = StringUtil.makeApproxLineBreak(in, 150, 10, "\n");
		str = str.replaceAll("\\n", "<br>");
		if (in.indexOf("<html>")>=0 || in.indexOf("<HTML>")>=0)
			return str;
		return "<html><pre>" + str + "</pre></html>";
	}

}
//----------------------------------------------------------
// select * from M_SQL_PLAN_CACHE
//----------------------------------------------------------
//	RS> Col# Label                                    JDBC Type Name           Guessed DBMS type
//	RS> ---- ---------------------------------------- ------------------------ -----------------
//	RS> 1    HOST                                     java.sql.Types.VARCHAR   VARCHAR(64)      
//	RS> 2    PORT                                     java.sql.Types.INTEGER   INTEGER          
//	RS> 3    VOLUME_ID                                java.sql.Types.INTEGER   INTEGER          
//	RS> 4    STATEMENT_STRING                         java.sql.Types.NCLOB     NCLOB            
//	RS> 5    STATEMENT_HASH                           java.sql.Types.VARCHAR   VARCHAR(32)      
//	RS> 6    USER_NAME                                java.sql.Types.NVARCHAR  NVARCHAR(256)    
//	RS> 7    SESSION_USER_NAME                        java.sql.Types.NVARCHAR  NVARCHAR(256)    
//	RS> 8    SCHEMA_NAME                              java.sql.Types.NVARCHAR  NVARCHAR(256)    
//	RS> 9    IS_VALID                                 java.sql.Types.VARCHAR   VARCHAR(5)       
//	RS> 10   IS_INTERNAL                              java.sql.Types.VARCHAR   VARCHAR(5)       
//	RS> 11   IS_DISTRIBUTED_EXECUTION                 java.sql.Types.VARCHAR   VARCHAR(5)       
//	RS> 12   IS_PINNED_PLAN                           java.sql.Types.VARCHAR   VARCHAR(5)       
//	RS> 13   PINNED_PLAN_ID                           java.sql.Types.BIGINT    BIGINT           
//	RS> 14   ABAP_VARCHAR_MODE                        java.sql.Types.VARCHAR   VARCHAR(5)       
//	RS> 15   ACCESSED_TABLES                          java.sql.Types.VARCHAR   VARCHAR(5000)    
//	RS> 16   ACCESSED_TABLE_NAMES                     java.sql.Types.NVARCHAR  NVARCHAR(5000)   
//	RS> 17   ACCESSED_OBJECTS                         java.sql.Types.VARCHAR   VARCHAR(5000)    
//	RS> 18   ACCESSED_OBJECT_NAMES                    java.sql.Types.NVARCHAR  NVARCHAR(5000)   
//	RS> 19   TABLE_LOCATIONS                          java.sql.Types.VARCHAR   VARCHAR(2000)    
//	RS> 20   TABLE_TYPES                              java.sql.Types.VARCHAR   VARCHAR(128)     
//	RS> 21   PLAN_SHARING_TYPE                        java.sql.Types.VARCHAR   VARCHAR(128)     
//	RS> 22   OWNER_CONNECTION_ID                      java.sql.Types.INTEGER   INTEGER          
//	RS> 23   PLAN_ID                                  java.sql.Types.BIGINT    BIGINT           
//	RS> 24   PLAN_MEMORY_SIZE                         java.sql.Types.BIGINT    BIGINT           
//	RS> 25   REFERENCE_COUNT                          java.sql.Types.BIGINT    BIGINT           
//	RS> 26   PARAMETER_COUNT                          java.sql.Types.BIGINT    BIGINT           
//	RS> 27   UPDATED_TABLE_OID                        java.sql.Types.BIGINT    BIGINT           
//	RS> 28   EXECUTION_COUNT                          java.sql.Types.BIGINT    BIGINT           
//	RS> 29   EXECUTION_COUNT_BY_ROUTING               java.sql.Types.BIGINT    BIGINT           
//	RS> 30   TOTAL_CURSOR_DURATION                    java.sql.Types.BIGINT    BIGINT           
//	RS> 31   AVG_CURSOR_DURATION                      java.sql.Types.BIGINT    BIGINT           
//	RS> 32   MIN_CURSOR_DURATION                      java.sql.Types.BIGINT    BIGINT           
//	RS> 33   MAX_CURSOR_DURATION                      java.sql.Types.BIGINT    BIGINT           
//	RS> 34   TOTAL_EXECUTION_TIME                     java.sql.Types.BIGINT    BIGINT           
//	RS> 35   AVG_EXECUTION_TIME                       java.sql.Types.BIGINT    BIGINT           
//	RS> 36   MIN_EXECUTION_TIME                       java.sql.Types.BIGINT    BIGINT           
//	RS> 37   MAX_EXECUTION_TIME                       java.sql.Types.BIGINT    BIGINT           
//	RS> 38   TOTAL_EXECUTION_OPEN_TIME                java.sql.Types.BIGINT    BIGINT           
//	RS> 39   AVG_EXECUTION_OPEN_TIME                  java.sql.Types.BIGINT    BIGINT           
//	RS> 40   MIN_EXECUTION_OPEN_TIME                  java.sql.Types.BIGINT    BIGINT           
//	RS> 41   MAX_EXECUTION_OPEN_TIME                  java.sql.Types.BIGINT    BIGINT           
//	RS> 42   TOTAL_EXECUTION_FETCH_TIME               java.sql.Types.BIGINT    BIGINT           
//	RS> 43   AVG_EXECUTION_FETCH_TIME                 java.sql.Types.BIGINT    BIGINT           
//	RS> 44   MIN_EXECUTION_FETCH_TIME                 java.sql.Types.BIGINT    BIGINT           
//	RS> 45   MAX_EXECUTION_FETCH_TIME                 java.sql.Types.BIGINT    BIGINT           
//	RS> 46   TOTAL_EXECUTION_CLOSE_TIME               java.sql.Types.BIGINT    BIGINT           
//	RS> 47   AVG_EXECUTION_CLOSE_TIME                 java.sql.Types.BIGINT    BIGINT           
//	RS> 48   MIN_EXECUTION_CLOSE_TIME                 java.sql.Types.BIGINT    BIGINT           
//	RS> 49   MAX_EXECUTION_CLOSE_TIME                 java.sql.Types.BIGINT    BIGINT           
//	RS> 50   TOTAL_METADATA_CACHE_MISS_COUNT          java.sql.Types.BIGINT    BIGINT           
//	RS> 51   TOTAL_TABLE_LOAD_TIME_DURING_PREPARATION java.sql.Types.BIGINT    BIGINT           
//	RS> 52   AVG_TABLE_LOAD_TIME_DURING_PREPARATION   java.sql.Types.BIGINT    BIGINT           
//	RS> 53   MIN_TABLE_LOAD_TIME_DURING_PREPARATION   java.sql.Types.BIGINT    BIGINT           
//	RS> 54   MAX_TABLE_LOAD_TIME_DURING_PREPARATION   java.sql.Types.BIGINT    BIGINT           
//	RS> 55   PREPARATION_COUNT                        java.sql.Types.BIGINT    BIGINT           
//	RS> 56   TOTAL_PREPARATION_TIME                   java.sql.Types.BIGINT    BIGINT           
//	RS> 57   AVG_PREPARATION_TIME                     java.sql.Types.BIGINT    BIGINT           
//	RS> 58   MIN_PREPARATION_TIME                     java.sql.Types.BIGINT    BIGINT           
//	RS> 59   MAX_PREPARATION_TIME                     java.sql.Types.BIGINT    BIGINT           
//	RS> 60   TOTAL_RESULT_RECORD_COUNT                java.sql.Types.BIGINT    BIGINT           
//	RS> 61   TOTAL_LOCK_WAIT_COUNT                    java.sql.Types.BIGINT    BIGINT           
//	RS> 62   TOTAL_LOCK_WAIT_DURATION                 java.sql.Types.BIGINT    BIGINT           
//	RS> 63   LAST_CONNECTION_ID                       java.sql.Types.INTEGER   INTEGER          
//	RS> 64   LAST_EXECUTION_TIMESTAMP                 java.sql.Types.TIMESTAMP TIMESTAMP        
//	RS> 65   LAST_PREPARATION_TIMESTAMP               java.sql.Types.TIMESTAMP TIMESTAMP        


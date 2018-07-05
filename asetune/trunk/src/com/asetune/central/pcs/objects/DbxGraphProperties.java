package com.asetune.central.pcs.objects;

import java.sql.Timestamp;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

// select * from GORAN_UB3_DS.DbxGraphProperties

//RS> Col# Label            JDBC Type Name           Guessed DBMS type Source Table                                      
//RS> ---- ---------------- ------------------------ ----------------- --------------------------------------------------
//RS> 1    SessionStartTime java.sql.Types.TIMESTAMP TIMESTAMP         DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxGraphProperties
//RS> 2    CmName           java.sql.Types.VARCHAR   VARCHAR(30)       DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxGraphProperties
//RS> 3    GraphName        java.sql.Types.VARCHAR   VARCHAR(30)       DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxGraphProperties
//RS> 4    TableName        java.sql.Types.VARCHAR   VARCHAR(128)      DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxGraphProperties
//RS> 5    GraphLabel       java.sql.Types.VARCHAR   VARCHAR(255)      DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxGraphProperties
//RS> 6    isPercentGraph   java.sql.Types.INTEGER   INTEGER           DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxGraphProperties

@JsonPropertyOrder(value = {"sessionStartTime", "serverName", "cmName", "graphName", "tableName", "graphLabel", "graphCategory", "isPercentGraph", "visibleAtStartup", "initialOrder"}, alphabetic = true)
public class DbxGraphProperties
{
	private Timestamp _sessionStartTime;
	private String    _serverName      ;
	private String    _cmName          ;
	private String    _graphName       ;
	private String    _tableName       ;
	private String    _graphLabel      ;
	private String    _graphCategory   ;
	private boolean   _isPercentGraph  ;
	private boolean   _visibleAtStartup;
	private int       _initialOrder;

	public String    getRecid()            { return UUID.randomUUID().toString();  }
	public Timestamp getSessionStartTime() { return _sessionStartTime;  }
	public String    getServerName      () { return _serverName      ;  }
	public String    getCmName          () { return _cmName          ;  }
	public String    getGraphName       () { return _graphName       ;  }
	public String    getTableName       () { return _tableName       ;  }
	public String    getGraphLabel      () { return _graphLabel      ;  }
	public String    getGraphCategory   () { return _graphCategory   ;  }
	public boolean   isPercentGraph     () { return _isPercentGraph  ;  }
	public boolean   isVisibleAtStartup () { return _visibleAtStartup;  }
	public int       getInitialOrder    () { return _initialOrder    ;  }

	public void setSessionStartTime(Timestamp sessionStartTime ) { _sessionStartTime  = sessionStartTime; }
	public void setServerName      (String     serverName      ) { _serverName        = serverName      ; }
	public void setCmName          (String     cmName          ) { _cmName            = cmName          ; }
	public void setGraphName       (String     graphName       ) { _graphName         = graphName       ; }
	public void setTableName       (String     tableName       ) { _tableName         = tableName       ; }
	public void setGraphLabel      (String     graphLabel      ) { _graphLabel        = graphLabel      ; }
	public void setGraphCategory   (String     graphCategory   ) { _graphCategory     = graphCategory       ; }
	public void setIsPercentGraph  (boolean    isPercentGraph  ) { _isPercentGraph    = isPercentGraph  ; }
	public void setVisibleAtStartup(boolean    visibleAtStartup) { _visibleAtStartup  = visibleAtStartup; }
	public void setInitialOrder    (int        initialOrder    ) { _initialOrder      = initialOrder    ; }

	public DbxGraphProperties(Timestamp sessionStartTime, String serverName, String cmName, String graphName, String tableName, String graphLabel, String graphCategory, boolean isPercentGraph, boolean visibleAtStartup, int initialOrder)
	{
		super();

		_sessionStartTime  = sessionStartTime;
		_serverName        = serverName      ;
		_cmName            = cmName          ;
		_graphName         = graphName       ;
		_tableName         = tableName       ;
		_graphLabel        = graphLabel      ;
		_graphCategory     = graphCategory   ;
		_isPercentGraph    = isPercentGraph  ;
		_visibleAtStartup  = visibleAtStartup;
		_initialOrder      = initialOrder    ;
	}
}

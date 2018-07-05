package com.asetune.central.pcs.objects;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

//1> select * from GORAN_UB3_DS.DbxAlarmActive
//RS> Col# Label                   JDBC Type Name           Guessed DBMS type Source Table                                  
//RS> ---- ----------------------- ------------------------ ----------------- ----------------------------------------------
//RS> 1    alarmClass              java.sql.Types.VARCHAR   VARCHAR(80)       DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive
//RS> 2    serviceType             java.sql.Types.VARCHAR   VARCHAR(80)       DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive
//RS> 3    serviceName             java.sql.Types.VARCHAR   VARCHAR(30)       DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive
//RS> 4    serviceInfo             java.sql.Types.VARCHAR   VARCHAR(80)       DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive
//RS> 5    extraInfo               java.sql.Types.VARCHAR   VARCHAR(80)       DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive
//RS> 6    category                java.sql.Types.VARCHAR   VARCHAR(20)       DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive
//RS> 7    severity                java.sql.Types.VARCHAR   VARCHAR(10)       DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive
//RS> 8    state                   java.sql.Types.VARCHAR   VARCHAR(10)       DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive
//RS> 9    repeatCnt               java.sql.Types.INTEGER   INTEGER           DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive
//RS> 10   duration                java.sql.Types.VARCHAR   VARCHAR(10)       DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive
//RS> 11   createTime              java.sql.Types.TIMESTAMP TIMESTAMP         DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive
//RS> 12   cancelTime              java.sql.Types.TIMESTAMP TIMESTAMP         DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive
//RS> 13   timeToLive              java.sql.Types.INTEGER   INTEGER           DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive
//RS> 14   threshold               java.sql.Types.VARCHAR   VARCHAR(15)       DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive
//RS> 15   data                    java.sql.Types.VARCHAR   VARCHAR(80)       DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive
//RS> 16   lastData                java.sql.Types.VARCHAR   VARCHAR(80)       DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive
//RS> 17   description             java.sql.Types.VARCHAR   VARCHAR(512)      DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive
//RS> 18   lastDescription         java.sql.Types.VARCHAR   VARCHAR(512)      DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive
//RS> 19   extendedDescription     java.sql.Types.CLOB      CLOB              DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive
//RS> 20   lastExtendedDescription java.sql.Types.CLOB      CLOB              DBXTUNE_CENTRAL_DB.GORAN_UB3_DS.DbxAlarmActive

@JsonPropertyOrder(value = {"srvName", "alarmClass", "serviceType", "serviceName", "serviceInfo", "extraInfo", "category", "severity", "state", "repeatCnt", "duration", "createTime", "cancelTime", "timeToLive", "threshold", "data", "lastData", "description", "lastDescription", "extendedDescription", "lastExtendedDescription"}, alphabetic = true)
public class DbxAlarmActive
{
	private String    _srvName                ;
	private String    _alarmClass             ;
	private String    _serviceType            ;
	private String    _serviceName            ;
	private String    _serviceInfo            ;
	private String    _extraInfo              ;
	private String    _category               ;
	private String    _severity               ;
	private String    _state                  ;
	private int       _repeatCnt              ;
	private String    _duration               ;
	private Timestamp _createTime             ;
	private Timestamp _cancelTime             ;
	private int       _timeToLive             ;
	private String    _threshold              ;
	private String    _data                   ;
	private String    _lastData               ;
	private String    _description            ;
	private String    _lastDescription        ;
	private String    _extendedDescription    ;
	private String    _lastExtendedDescription;
	
	public String    getSrvName                () { return _srvName                ; }
	public String    getAlarmClass             () { return _alarmClass             ; }
	public String    getServiceType            () { return _serviceType            ; }
	public String    getServiceName            () { return _serviceName            ; }
	public String    getServiceInfo            () { return _serviceInfo            ; }
	public String    getExtraInfo              () { return _extraInfo              ; }
	public String    getCategory               () { return _category               ; }
	public String    getSeverity               () { return _severity               ; }
	public String    getState                  () { return _state                  ; }
	public int       getRepeatCnt              () { return _repeatCnt              ; }
	public String    getDuration               () { return _duration               ; }
	public Timestamp getCreateTime             () { return _createTime             ; }
	public Timestamp getCancelTime             () { return _cancelTime             ; }
	public int       getTimeToLive             () { return _timeToLive             ; }
	public String    getThreshold              () { return _threshold              ; }
	public String    getData                   () { return _data                   ; }
	public String    getLastData               () { return _lastData               ; }
	public String    getDescription            () { return _description            ; }
	public String    getLastDescription        () { return _lastDescription        ; }
	public String    getExtendedDescription    () { return _extendedDescription    ; }
	public String    getLastExtendedDescription() { return _lastExtendedDescription; }
	
	public void setSrvName                (String    srvName                ) { _srvName                 = srvName                ; }
	public void setAlarmClass             (String    alarmClass             ) { _alarmClass              = alarmClass             ; }
	public void setServiceType            (String    serviceType            ) { _serviceType             = serviceType            ; }
	public void setServiceName            (String    serviceName            ) { _serviceName             = serviceName            ; }
	public void setServiceInfo            (String    serviceInfo            ) { _serviceInfo             = serviceInfo            ; }
	public void setExtraInfo              (String    extraInfo              ) { _extraInfo               = extraInfo              ; }
	public void setCategory               (String    category               ) { _category                = category               ; }
	public void setSeverity               (String    severity               ) { _severity                = severity               ; }
	public void setState                  (String    state                  ) { _state                   = state                  ; }
	public void setRepeatCnt              (int       repeatCnt              ) { _repeatCnt               = repeatCnt              ; }
	public void setDuration               (String    duration               ) { _duration                = duration               ; }
	public void setCreateTime             (Timestamp createTime             ) { _createTime              = createTime             ; }
	public void setCancelTime             (Timestamp cancelTime             ) { _cancelTime              = cancelTime             ; }
	public void setTimeToLive             (int       timeToLive             ) { _timeToLive              = timeToLive             ; }
	public void setThreshold              (String    threshold              ) { _threshold               = threshold              ; }
	public void setData                   (String    data                   ) { _data                    = data                   ; }
	public void setLastData               (String    lastData               ) { _lastData                = lastData               ; }
	public void setDescription            (String    description            ) { _description             = description            ; }
	public void setLastDescription        (String    lastDescription        ) { _lastDescription         = lastDescription        ; }
	public void setExtendedDescription    (String    extendedDescription    ) { _extendedDescription     = extendedDescription    ; }
	public void setLastExtendedDescription(String    lastExtendedDescription) { _lastExtendedDescription = lastExtendedDescription; }
	
	public DbxAlarmActive(String srvName, String alarmClass, String serviceType, String serviceName, String serviceInfo, String extraInfo, String category, String severity, String state, int repeatCnt, String duration, Timestamp createTime, Timestamp cancelTime, int timeToLive, String threshold, String data, String lastData, String description, String lastDescription, String extendedDescription, String lastExtendedDescription)
	{
		_srvName                 = srvName;
		_alarmClass              = alarmClass;
		_serviceType             = serviceType;
		_serviceName             = serviceName;
		_serviceInfo             = serviceInfo;
		_extraInfo               = extraInfo;
		_category                = category;
		_severity                = severity;
		_state                   = state;
		_repeatCnt               = repeatCnt;
		_duration                = duration;
		_createTime              = createTime;
		_cancelTime              = cancelTime;
		_timeToLive              = timeToLive;
		_threshold               = threshold;
		_data                    = data;
		_lastData                = lastData;
		_description             = description;
		_lastDescription         = lastDescription;
		_extendedDescription     = extendedDescription;
		_lastExtendedDescription = lastExtendedDescription;
	}
}

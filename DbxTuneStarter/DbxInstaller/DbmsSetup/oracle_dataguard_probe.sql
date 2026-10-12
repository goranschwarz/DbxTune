-- --------------------------------------------------------------------------------------------
-- oracle_dataguard_probe.sql
--
-- READ ONLY: this script only does SELECT. It changes nothing.
--
-- WHY
--   The OracleTune tab 'Data Guard' (CmDataGuard) was built WITHOUT a real standby database to test on.
--   It was tested with made up rows, so the calculations are right, but nobody has seen what a real
--   Data Guard configuration puts in the views that it reads. That is why its alarms are off by default.
--
--   This script prints what the tab reads. If you have a Data Guard configuration:
--   run it on the PRIMARY (and, if the standby is open, on the STANDBY) and compare with what you know
--   about your standby. If the numbers in section 7 are right, the alarms can be turned on:
--       CmDataGuard.alarm.system.untested.enabled=true
--
-- HOW
--   sqlplus dbxtune@//hostname:1521/SERVICE_NAME @oracle_dataguard_probe.sql
--   (any login with SELECT_CATALOG_ROLE or SELECT ANY DICTIONARY)
--
-- WHAT TO LOOK FOR (the assumptions that could not be tested)
--   A) Section 3: for a standby that is in step, is ARCHIVED_SEQ# = the last complete log on the primary
--      (section 4: the sequence of the CURRENT log, minus 1)?
--   B) Section 3: is APPLIED_SEQ# filled in (not 0)? With real time apply: is it the last complete log,
--      or is it one behind?
--   C) Section 5: do the logs that the standby has (section 3) have a row here, with a NEXT_TIME?
--   D) Section 6 (on the standby): do 'apply lag' and 'transport lag' look like '+00 00:00:00'?
--   E) Section 7: are the gaps 0 and the lags 0 when you know that the standby is in step?
--      Stop the apply on the standby for a while (or look after a busy period): do they go up?
-- --------------------------------------------------------------------------------------------
set linesize 250
set pagesize 200
set trimspool on
set tab off
column NAME                   format a30
column VALUE                  format a40
column DEST_NAME              format a20
column DESTINATION            format a30
column DB_UNIQUE_NAME         format a16
column ERROR                  format a50
column GAP_STATUS             format a24
column SYNCHRONIZATION_STATUS format a22
column RECOVERY_MODE          format a25
column DATABASE_MODE          format a16
column UNIT                   format a30
column TIME_COMPUTED          format a20
column DATUM_TIME             format a20

prompt
prompt ======================================================================
prompt 1: Role and protection (V$DATABASE)
prompt ======================================================================
select DB_UNIQUE_NAME, DATABASE_ROLE, OPEN_MODE, PROTECTION_MODE, PROTECTION_LEVEL, SWITCHOVER_STATUS, DATAGUARD_BROKER, FORCE_LOGGING, to_char(SYSDATE, 'YYYY-MM-DD HH24:MI:SS') as NOW
from V$DATABASE;

prompt
prompt ======================================================================
prompt 2: The standby destinations (V$ARCHIVE_DEST)
prompt ======================================================================
select DEST_ID, DEST_NAME, STATUS, TARGET, DESTINATION, DB_UNIQUE_NAME, TRANSMIT_MODE, AFFIRM, DELAY_MINS, ERROR
from V$ARCHIVE_DEST
where TARGET = 'STANDBY' and STATUS <> 'INACTIVE'
order by DEST_ID;

prompt
prompt ======================================================================
prompt 3: What the primary knows about each standby (V$ARCHIVE_DEST_STATUS)
prompt ======================================================================
select DEST_ID, STATUS, TYPE, DATABASE_MODE, RECOVERY_MODE, PROTECTION_MODE, ARCHIVED_THREAD#, ARCHIVED_SEQ#, APPLIED_THREAD#, APPLIED_SEQ#, SYNCHRONIZATION_STATUS, GAP_STATUS
from V$ARCHIVE_DEST_STATUS
where DEST_ID in (select DEST_ID from V$ARCHIVE_DEST where TARGET = 'STANDBY' and STATUS <> 'INACTIVE')
order by DEST_ID;

prompt
prompt ======================================================================
prompt 4: The online redo logs of the primary (V$LOG)
prompt    The last complete log = the SEQUENCE# of the CURRENT one, minus 1
prompt ======================================================================
select THREAD#, GROUP#, SEQUENCE#, STATUS, ARCHIVED, to_char(FIRST_TIME, 'YYYY-MM-DD HH24:MI:SS') as FIRST_TIME
from V$LOG
order by THREAD#, SEQUENCE#;

prompt
prompt ======================================================================
prompt 5: The last 10 log sequences, per destination (V$ARCHIVED_LOG)
prompt    NEXT_TIME = when the log ended: the lag is counted from that
prompt ======================================================================
select THREAD#, SEQUENCE#, DEST_ID, STANDBY_DEST, ARCHIVED, APPLIED, to_char(FIRST_TIME, 'YYYY-MM-DD HH24:MI:SS') as FIRST_TIME, to_char(NEXT_TIME, 'YYYY-MM-DD HH24:MI:SS') as NEXT_TIME, to_char(COMPLETION_TIME, 'YYYY-MM-DD HH24:MI:SS') as COMPLETION_TIME
from V$ARCHIVED_LOG
where RESETLOGS_CHANGE# = (select RESETLOGS_CHANGE# from V$DATABASE)
  and SEQUENCE# > (select max(SEQUENCE#) - 10 from V$ARCHIVED_LOG where RESETLOGS_CHANGE# = (select RESETLOGS_CHANGE# from V$DATABASE))
order by THREAD#, SEQUENCE#, DEST_ID;

prompt
prompt ======================================================================
prompt 6: Oracle's own lag numbers (V$DATAGUARD_STATS)
prompt    Only has rows on a STANDBY. This is what the tab shows when OracleTune is connected to a standby
prompt ======================================================================
select NAME, VALUE, UNIT, TIME_COMPUTED, DATUM_TIME
from V$DATAGUARD_STATS;

prompt
prompt ======================================================================
prompt 7: What the tab 'Data Guard' shows on a PRIMARY (the SQL of CmDataGuard)
prompt    TRANSPORT_SEQ_GAP / APPLY_SEQ_GAP = complete logs that the standby has not received / not applied
prompt    TRANSPORT_LAG_SEC / APPLY_LAG_SEC = how old the newest redo is that it has received / applied
prompt    Empty = the primary does not know (ARCHIVED_SEQ# or APPLIED_SEQ# is 0 in section 3)
prompt ======================================================================
select
     y.DEST_ID
    ,y.DB_UNIQUE_NAME
    ,y.STATUS
    ,y.GAP_STATUS
    ,y.PRIMARY_SEQ
    ,y.SHIPPED_SEQ
    ,y.APPLIED_SEQ
    ,GREATEST(y.PRIMARY_SEQ - y.SHIPPED_SEQ, 0)                                    as TRANSPORT_SEQ_GAP
    ,GREATEST(y.PRIMARY_SEQ - y.APPLIED_SEQ, 0)                                    as APPLY_SEQ_GAP
    ,CAST(case when y.SHIPPED_SEQ is null                then NULL
               when y.PRIMARY_SEQ - y.SHIPPED_SEQ <= 0   then 0
               else (SYSDATE - y.SHIPPED_REDO_TIME) * 86400 end as NUMBER(18))     as TRANSPORT_LAG_SEC
    ,CAST(case when y.APPLIED_SEQ is null                then NULL
               when y.PRIMARY_SEQ - y.APPLIED_SEQ <= 0   then 0
               else (SYSDATE - y.APPLIED_REDO_TIME) * 86400 end as NUMBER(18))     as APPLY_LAG_SEC
    ,y.THREAD_NUM
    ,y.RECOVERY_MODE
from (select x.*
            ,(select max(l.SEQUENCE#) - 1 from V$LOG l          where l.THREAD# = x.THREAD_NUM and l.STATUS = 'CURRENT')                                                         as PRIMARY_SEQ
            ,(select max(a.NEXT_TIME)     from V$ARCHIVED_LOG a where a.THREAD# = x.THREAD_NUM and a.SEQUENCE# = x.SHIPPED_SEQ and a.RESETLOGS_CHANGE# = x.RESETLOGS_CHANGE_NUM) as SHIPPED_REDO_TIME
            ,(select max(a.NEXT_TIME)     from V$ARCHIVED_LOG a where a.THREAD# = x.THREAD_NUM and a.SEQUENCE# = x.APPLIED_SEQ and a.RESETLOGS_CHANGE# = x.RESETLOGS_CHANGE_NUM) as APPLIED_REDO_TIME
      from (select d.DEST_ID
                  ,d.DB_UNIQUE_NAME
                  ,d.STATUS
                  ,s.GAP_STATUS
                  ,NVL(NULLIF(s.ARCHIVED_THREAD#, 0), 1)                 as THREAD_NUM
                  ,NULLIF(s.ARCHIVED_SEQ#, 0)                            as SHIPPED_SEQ
                  ,NULLIF(s.APPLIED_SEQ#,  0)                            as APPLIED_SEQ
                  ,(select db.RESETLOGS_CHANGE# from V$DATABASE db)      as RESETLOGS_CHANGE_NUM
                  ,s.RECOVERY_MODE
            from V$ARCHIVE_DEST d
            left join V$ARCHIVE_DEST_STATUS s on s.DEST_ID = d.DEST_ID
            where d.TARGET = 'STANDBY'
              and d.STATUS <> 'INACTIVE'
           ) x
     ) y
order by y.DEST_ID;

prompt
prompt Done. Nothing was changed.

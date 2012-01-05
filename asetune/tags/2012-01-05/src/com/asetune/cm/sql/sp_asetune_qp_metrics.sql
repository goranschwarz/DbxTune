use sybsystemprocs
go
set nocount on
go

-------------------------------------------------------------
--- NOTE -- NOTE -- NOTE -- NOTE -- NOTE -- NOTE -- NOTE ---- 
-------------------------------------------------------------
--
-- If you change anything in here, do NOT forget to change
-- the field 'SP_ASETUNE_QP_METRICS_CR_STR' in 'com.asetune.cm.sql.VersionInfo'
--
-- Otherwise it will NOT be recreated... when asetune starts...
--
-------------------------------------------------------------







-------------------------------------------------------------
-- Make sure we are NOT in master database
-------------------------------------------------------------
if ( (select db_name()) = "master" )
begin
	print "WRONG DATABASE: you should be in a USER DATABASE when creating this procedure."
end
go
if ( (select db_name()) = "master" )
begin
	select syb_quit()
end
go





-------------------------------------------------------------
-- Procedure: sp_asetune_qp_metrics_in_db
-------------------------------------------------------------
if (select object_id('sp_asetune_qp_metrics_in_db')) is not null
begin
	drop procedure sp_asetune_qp_metrics_in_db
	print "Dropping procedure sp_asetune_qp_metrics_in_db"
end
go

declare @dbname varchar(255)
select @dbname = db_name()
print "Creating procedure '%1!.%2!.%3!'.", @dbname, "dbo", "sp_asetune_qp_metrics_in_db"
go

create procedure sp_asetune_qp_metrics_in_db
(
	@cmd  varchar(20)  = null,
	@cmd2 varchar(255) = null
)
as
begin
	declare @sql    varchar(1024),
		@dbid   int,
		@dbname varchar(255)

	select @dbid   = db_id(),
	       @dbname = db_name()

	/*
	** SHOW
	*/
	if (@cmd = "show")
	begin
		select @sql = "SELECT "
		            + " DBName    = db_name(), "
	--	            + " UserName  = user_name(uid), "
	--	            + " showplan1 = show_plan(-1,id,-1,-1), "
	--	            + " showplan2 = show_plan(-1,hashkey,-1,-1), "
		            + " * "
		            + " from sysquerymetrics "
		            + " where gid = 1 "

		if (@cmd2 is not NULL)
		begin
			select @sql = @sql + " AND " + @cmd2
		end

		print "DEBUG: %1!..sp_asetune_qp_metrics_in_db 'show'", @dbname
		print "DEBUG: extra where clause: '%1!'", @cmd2
		print "DEBUG: SQL: %1!", @sql

		exec(@sql)
	end
	/*
	** DROP
	*/
	else if (@cmd = "drop")
	begin
		print "DEBUG: %1!..sp_asetune_qp_metrics_in_db 'drop'", @dbname
		if exists (select 1 from sysquerymetrics where gid = 1)
		begin
			exec sp_metrics 'drop', '1'
		end
	end
	/*
	** FILTER
	*/
	else if (@cmd = "filter")
	begin
		print "DEBUG: %1!..sp_asetune_qp_metrics_in_db 'filter', '%2!'", @dbname, @cmd2
		if exists (select 1 from sysquerymetrics where gid = 1)
		begin
			exec sp_metrics 'filter', '1', @cmd2
		end
	end
	else
	begin
		raiserror 99999 "Unknown command '%1!'.", @cmd
	end
end
go

grant exec on sp_asetune_qp_metrics_in_db to public
go





-------------------------------------------------------------
-- Procedure: sp_asetune_qp_metrics
-------------------------------------------------------------
if (select object_id('sp_asetune_qp_metrics')) is not null
begin
	drop procedure sp_asetune_qp_metrics
	print "Dropping procedure sp_asetune_qp_metrics"
end
go

declare @dbname varchar(255)
select @dbname = db_name()
print "Creating procedure '%1!.%2!.%3!'.", @dbname, "dbo", "sp_asetune_qp_metrics"
go

create procedure sp_asetune_qp_metrics
(
	@cmd  varchar(20)  = null,
	@cmd2 varchar(255) = null
)
as
begin
	declare @c_dbname varchar(30), @c_dbid int
	declare @x        int
	declare @execProc varchar(255)

	/*
	** Check input parameters
	*/
	if (@cmd is null)
		select @cmd = "show"
		
	if      (@cmd = "show")   select @x = 0
	else if (@cmd = "drop")   select @x = 0
	else if (@cmd = "filter") select @x = 0
	else
	begin
		print "Usage: sp_asetune_qp_metrics show|drop|filter"
		return 0
	end
	
	/*
	** First flush the metrics from inmemory structures to system tables
	** OR: do I need to issue this in every database?
	** CHECK: dbcc flushmetrics, to check for the above
	*/
	exec sp_metrics 'flush'
	
	-- master..sysdatabases.status
	-- 32 = Database created with for load option, or crashed while loading database, instructs recovery not to proceed
	-- 256 = Database suspect | Not recovered | Cannot be opened or used | Can be dropped only with dbcc dbrepair

	declare db_curs cursor for 
		select name, dbid 
		from master..sysdatabases 
		where 1 = 1
--		  and name not in('master', 'tempdb', 'model', 'sybsystemdb', 'sybsystemprocs', 'sybpcidb', 'sybmgmtdb', 'sybpcidb')
--		  and name not in(          'tempdb', 'model', 'sybsystemdb', 'sybsystemprocs', 'sybpcidb', 'sybmgmtdb', 'sybpcidb')
		  and (status & 32 != 32) and (status & 256 != 256) 
		order by dbid
		for read only
	open db_curs
	fetch db_curs into @c_dbname, @c_dbid

	while (@@sqlstatus != 2)
	begin
		if (@@sqlstatus = 1)
		begin
			print "Error in fetch from the cursor"
			return 1
		end


--		print "DBID=%1!, DBNAME='%2!'", @c_dbid, @c_dbname

--		exec(@c_dbname+"..sp_asetune_qp_metrics_in_db '"+@cmd+"', '"+@cmd2+"'")
		select @execProc = @c_dbname+"..sp_asetune_qp_metrics_in_db"
		exec @execProc @cmd, @cmd2

		fetch db_curs into @c_dbname, @c_dbid
	end
	close db_curs
	return 0
end
go

grant exec on sp_asetune_qp_metrics to public
go

use master
go
--sp_asetune_qp_metrics
go



use sybsystemprocs
go
set nocount on
go

-------------------------------------------------------------
--- NOTE -- NOTE -- NOTE -- NOTE -- NOTE -- NOTE -- NOTE ---- 
-------------------------------------------------------------
--
-- If you change anything in here, do NOT forget to change
-- the field 'SP_MISSING_STATS_CR_STR' in 'asemon.cm.sql.VersionInfo'
--
-- Otherwise it will NOT be recreated... when asemon starts...
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
-- Procedure: sp_asemon_ra_stats
-------------------------------------------------------------
if (select object_id('sp_asemon_ra_stats')) is not null
begin
	drop procedure sp_asemon_ra_stats
	print "Dropping procedure sp_asemon_ra_stats"
end
go

declare @dbname varchar(255)
select @dbname = db_name()
print "Creating procedure '%1!.%2!.%3!'.", @dbname, "dbo", "sp_asemon_ra_stats"
go

create procedure sp_asemon_ra_stats
(
	@resultSetType int = 2  -- 0=High(OneRowPerCounter), 1=Wide(OneColumnPerCounter), 2=DynamicWide
)
as
begin

	declare @skip_status		int	/* Holds db status to be skipped from the sysdatabases select */
	declare @tempdb_mask		int	/* Mask indicating this is a temporary database */

	-- CURSOR variables
	declare @rowid  int  -- just a counter of what row I'm at (starting at zero)
	declare @c_dbid int

	-- List of names: 'repagent_4', 'repagent_6', 'repagent_7'
	declare @inRaNames   varchar(1024)
	declare @firstDbName varchar(30)

	/*
	** Initialize the variable with the status to be skipped from the
	** select from sysdatabases:
	**
	**      status3 = 8    : Database is being shutdown
	**      status3 = 4096 : Shutdown of the database is complete
	*/
	select @skip_status = 8 | 4096
	select @tempdb_mask = number
	from master.dbo.spt_values
	where   type = "D3" and name = "TEMPDB STATUS MASK"

	select @skip_status =  @skip_status |  @tempdb_mask


	/*
	** fill the variable @inRaNames, with the replicated databases...
	*/
	select @rowid = 0
	declare repdb_cur cursor for
		select dbid
		from master.dbo.sysdatabases
		where 1 = (case when ((status3 & @skip_status) = 0) then is_rep_agent_enabled(dbid) else 0 end)

	OPEN repdb_cur
	FETCH NEXT FROM repdb_cur INTO @c_dbid
	WHILE (@@fetch_status <> -1)
	BEGIN

		IF (@@fetch_status <> -2)
		BEGIN
			if (@rowid = 0)
				select @firstDbName = db_name(@c_dbid)
			else
				select @inRaNames = @inRaNames + ", " -- add ',' after first append

			-- Append
			select @inRaNames = @inRaNames + "'repagent_" + convert(varchar(10), @c_dbid) + "'"
		END
		FETCH NEXT FROM repdb_cur INTO @c_dbid
		select @rowid = @rowid + 1
	END
	CLOSE repdb_cur
	DEALLOCATE repdb_cur

	/*
	** No databases had a RepAgent
	*/
	if (@rowid = 0)
	begin
		print "ERROR: Found NO database that was marked for replication, or had a Replication Agent."
/*-->*/		return 1
	end

	/*
	** Now execute the query...
	*/
	if (@resultSetType = 0)
	begin
		exec ("select dbname=db_name(convert(int,substring(group_name, 10,3))), field_name, field_id, value from master..sysmonitors  where group_name in ("+@inRaNames+")")
	end
	else if (@resultSetType = 1)
	begin
		-- Create #rep_agent_stats1
		select dbname = convert(varchar(50), ''), field_name, field_id, value 
		into #rep_agent_stats1 
		from master..sysmonitors 
		where 1=2

		-- Create #rep_agent_dbnames1
		select dbname = db_name(dbid), repserver=convert(varchar(50),rep_agent_config(dbid, "config", "rs servername"))
		into #rep_agent_dbnames1
		from master.dbo.sysdatabases
		where 1 = (case when ((status3 & @skip_status) = 0) then is_rep_agent_enabled(dbid) else 0 end)
		

		-- fill table: #rep_agent_stats1
		exec ("insert into #rep_agent_stats1 select dbname=db_name(convert(int,substring(group_name, 10,3))), field_name, field_id, value from master..sysmonitors where group_name in ("+@inRaNames+")")

		select
			R.dbname,
			R.repserver,

			log_waits                 = (select value from #rep_agent_stats1 S where field_name = 'ra_log_waits            ' and S.dbname = R.dbname),
			sum_log_wait              = (select value from #rep_agent_stats1 S where field_name = 'ra_sum_log_wait         ' and S.dbname = R.dbname),
			longest_log_wait          = (select value from #rep_agent_stats1 S where field_name = 'ra_longest_log_wait     ' and S.dbname = R.dbname),
			truncpt_moved             = (select value from #rep_agent_stats1 S where field_name = 'ra_truncpt_moved        ' and S.dbname = R.dbname),
			truncpt_gotten            = (select value from #rep_agent_stats1 S where field_name = 'ra_truncpt_gotten       ' and S.dbname = R.dbname),
			rs_connect                = (select value from #rep_agent_stats1 S where field_name = 'ra_rs_connect           ' and S.dbname = R.dbname),
			fail_rs_connect           = (select value from #rep_agent_stats1 S where field_name = 'ra_fail_rs_connect      ' and S.dbname = R.dbname),
			io_send                   = (select value from #rep_agent_stats1 S where field_name = 'ra_io_send              ' and S.dbname = R.dbname),
			sum_io_send_wait          = (select value from #rep_agent_stats1 S where field_name = 'ra_sum_io_send_wait     ' and S.dbname = R.dbname),
			longest_io_send_wait      = (select value from #rep_agent_stats1 S where field_name = 'ra_longest_io_send_wait ' and S.dbname = R.dbname),
			io_recv                   = (select value from #rep_agent_stats1 S where field_name = 'ra_io_recv              ' and S.dbname = R.dbname),
			sum_io_recv_wait          = (select value from #rep_agent_stats1 S where field_name = 'ra_sum_io_recv_wait     ' and S.dbname = R.dbname),
			longest_io_recv_wait      = (select value from #rep_agent_stats1 S where field_name = 'ra_longest_io_recv_wait ' and S.dbname = R.dbname),
			packets_sent              = (select value from #rep_agent_stats1 S where field_name = 'ra_packets_sent         ' and S.dbname = R.dbname),
			full_packets_sent         = (select value from #rep_agent_stats1 S where field_name = 'ra_full_packets_sent    ' and S.dbname = R.dbname),
			sum_packet                = (select value from #rep_agent_stats1 S where field_name = 'ra_sum_packet           ' and S.dbname = R.dbname),
			largest_packet            = (select value from #rep_agent_stats1 S where field_name = 'ra_largest_packet       ' and S.dbname = R.dbname),
			log_records_scanned       = (select value from #rep_agent_stats1 S where field_name = 'ra_log_records_scanned  ' and S.dbname = R.dbname),
			log_records_processed     = (select value from #rep_agent_stats1 S where field_name = 'ra_log_records_processed' and S.dbname = R.dbname),
			log_scans                 = (select value from #rep_agent_stats1 S where field_name = 'ra_log_scans            ' and S.dbname = R.dbname),
			sum_log_scan              = (select value from #rep_agent_stats1 S where field_name = 'ra_sum_log_scan         ' and S.dbname = R.dbname),
			longest_log_scan          = (select value from #rep_agent_stats1 S where field_name = 'ra_longest_log_scan     ' and S.dbname = R.dbname),
			open_xact                 = (select value from #rep_agent_stats1 S where field_name = 'ra_open_xact            ' and S.dbname = R.dbname),
			maintuser_xact            = (select value from #rep_agent_stats1 S where field_name = 'ra_maintuser_xact       ' and S.dbname = R.dbname),
			commit_xact               = (select value from #rep_agent_stats1 S where field_name = 'ra_commit_xact          ' and S.dbname = R.dbname),
			abort_xact                = (select value from #rep_agent_stats1 S where field_name = 'ra_abort_xact           ' and S.dbname = R.dbname),
			prepare_xact              = (select value from #rep_agent_stats1 S where field_name = 'ra_prepare_xact         ' and S.dbname = R.dbname),
			xupdate_processed         = (select value from #rep_agent_stats1 S where field_name = 'ra_xupdate_processed    ' and S.dbname = R.dbname),
			xinsert_processed         = (select value from #rep_agent_stats1 S where field_name = 'ra_xinsert_processed    ' and S.dbname = R.dbname),
			xdelete_processed         = (select value from #rep_agent_stats1 S where field_name = 'ra_xdelete_processed    ' and S.dbname = R.dbname),
			xexec_processed           = (select value from #rep_agent_stats1 S where field_name = 'ra_xexec_processed      ' and S.dbname = R.dbname),
			xcmdtext_processed        = (select value from #rep_agent_stats1 S where field_name = 'ra_xcmdtext_processed   ' and S.dbname = R.dbname),
			xwrtext_processed         = (select value from #rep_agent_stats1 S where field_name = 'ra_xwrtext_processed    ' and S.dbname = R.dbname),
			xrowimage_processed       = (select value from #rep_agent_stats1 S where field_name = 'ra_xrowimage_processed  ' and S.dbname = R.dbname),
			xclr_processed            = (select value from #rep_agent_stats1 S where field_name = 'ra_xclr_processed       ' and S.dbname = R.dbname),
			xckpt_processed           = (select value from #rep_agent_stats1 S where field_name = 'ra_xckpt_processed      ' and S.dbname = R.dbname),
			xckpt_genxactpurge        = (select value from #rep_agent_stats1 S where field_name = 'ra_xckpt_genxactpurge   ' and S.dbname = R.dbname),
			sqldml_processed          = (select value from #rep_agent_stats1 S where field_name = 'ra_sqldml_processed     ' and S.dbname = R.dbname),
			bckward_schema            = (select value from #rep_agent_stats1 S where field_name = 'ra_bckward_schema       ' and S.dbname = R.dbname),
			sum_bckward_wait          = (select value from #rep_agent_stats1 S where field_name = 'ra_sum_bckward_wait     ' and S.dbname = R.dbname),
			longest_bckward_wait      = (select value from #rep_agent_stats1 S where field_name = 'ra_longest_bckward_wait ' and S.dbname = R.dbname),
			forward_schema            = (select value from #rep_agent_stats1 S where field_name = 'ra_forward_schema       ' and S.dbname = R.dbname),
			sum_forward_wait          = (select value from #rep_agent_stats1 S where field_name = 'ra_sum_forward_wait     ' and S.dbname = R.dbname),
			longest_forward_wait      = (select value from #rep_agent_stats1 S where field_name = 'ra_longest_forward_wait ' and S.dbname = R.dbname),
			delayed_commit_xact       = (select value from #rep_agent_stats1 S where field_name = 'ra_delayed_commit_xact  ' and S.dbname = R.dbname),
			schema_reuse              = (select value from #rep_agent_stats1 S where field_name = 'ra_schema_reuse         ' and S.dbname = R.dbname)
		from #rep_agent_dbnames1 R

		drop table #rep_agent_stats1
		drop table #rep_agent_dbnames1
	end
	else if (@resultSetType = 2)
	begin
		declare @sql     varchar(16000)
		declare @colName varchar(100)

		declare @c_field_name varchar(100)

		-- Create #rep_agent_stats2
		select dbname = convert(varchar(50), ''), field_name, field_id, value 
		into #rep_agent_stats2 
		from master..sysmonitors 
		where 1=2

		-- Create #rep_agent_dbnames2
		select dbname = db_name(dbid), repserver=convert(varchar(50),rep_agent_config(dbid, "config", "rs servername"))
		into #rep_agent_dbnames2
		from master.dbo.sysdatabases
		where 1 = (case when ((status3 & @skip_status) = 0) then is_rep_agent_enabled(dbid) else 0 end)
		

		-- fill table: #rep_agent_stats2
		exec ("insert into #rep_agent_stats2 select dbname=db_name(convert(int,substring(group_name, 10,3))), field_name, field_id, value from master..sysmonitors where group_name in ("+@inRaNames+")")

		/*
		** Build a SQL statement that will be executed
		** This is if counter types changes alot, then we have to be dynamic...
		*/
		select @sql = 'select R.dbname, R.repserver'

		select @rowid = 0
		declare raFieldName_cur cursor for
			select rtrim(field_name)
			from #rep_agent_stats2
			where dbname = @firstDbName
			order by field_id

		OPEN raFieldName_cur
		FETCH NEXT FROM raFieldName_cur INTO @c_field_name
		WHILE (@@fetch_status <> -1)
		BEGIN

			IF (@@fetch_status <> -2)
			BEGIN
				select @colName = @c_field_name
				if (@colName like "ra[_]%")
					select @colName = substring(@colName, 4, 100) -- remove the 'ra_' part
				-- Append
				select @sql = @sql + ", "+@colName+" = (select value from #rep_agent_stats2 S where field_name = '"+@c_field_name+"' and S.dbname = R.dbname) "
			END
			FETCH NEXT FROM raFieldName_cur INTO @c_field_name
			select @rowid = @rowid + 1
		END
		CLOSE raFieldName_cur
		DEALLOCATE raFieldName_cur

		select @sql = @sql + " from #rep_agent_dbnames2 R "

--print "@rowid=%1!, SQL: %2!", @rowid, @sql
		/*
		** Now execute the dynamic built SQl which should look something like...
		** select R.dbname,
		**       ,counter_name1 = (select value from #rep_agent_stats2 S where field_name = 'counter_name1' and S.dbname = R.dbname),
		**       ,counter_name1 = (select value from #rep_agent_stats2 S where field_name = 'counter_name2' and S.dbname = R.dbname),
		**       ...
		** from #rep_agent_dbnames2 R
		*/
		exec(@sql)

		drop table #rep_agent_stats2
		drop table #rep_agent_dbnames2
	end

	/*
	** NORMAL Exit point
	*/
/*-->*/	return 0
end
go

grant exec on sp_asemon_ra_stats to public
go


--set statistics io on
go
--exec sp_asemon_ra_stats 1
go
--exec sp_asemon_ra_stats 2
go

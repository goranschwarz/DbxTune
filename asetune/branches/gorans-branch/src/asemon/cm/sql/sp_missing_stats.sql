use sybsystemprocs
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
-- Function: decodeColIdArray
-------------------------------------------------------------
if (select object_id('decodeColIdArray')) is not null
begin
	drop function decodeColIdArray
	print "Dropping function decodeColIdArray"
end
go

declare @dbname varchar(255)
select @dbname = db_name()
print "Creating function '%1!.%2!.%3!'.", @dbname, "dbo", "decodeColIdArray"
go

create function decodeColIdArray(@colIdArray varbinary(100), @tabId int, @dbId int)
returns varchar(1024)
as
begin
	declare @outStr varchar(1024)

	if (@dbId is null)
		select @dbId = db_id()

	while(@colIdArray != null)
	begin
		-- Get the colname
		select  @outStr = @outStr + col_name(@tabId, convert(smallint, left(@colIdArray, 2)), @dbId)

		-- get NEXT colId from the array, if empty get out of there
		select @colIdArray = substring(@colIdArray, 3, 255)
		if (@colIdArray = null)
			break

		-- Append ", "
		select  @outStr = @outStr + ", "
	end
	return @outStr
end
go

grant exec on decodeColIdArray to public
go




-------------------------------------------------------------
-- Procedure: sp_missing_stats_in_db
-------------------------------------------------------------
if (select object_id('sp_missing_stats_in_db')) is not null
begin
	drop procedure sp_missing_stats_in_db
	print "Dropping procedure sp_missing_stats_in_db"
end
go

declare @dbname varchar(255)
select @dbname = db_name()
print "Creating procedure '%1!.%2!.%3!'.", @dbname, "dbo", "sp_missing_stats_in_db"
go

create procedure sp_missing_stats_in_db
as
begin
	declare @dbid int
	select @dbid = db_id()

	select	
		DBName          = db_name(), 
		ObjectName      = object_name(s.id, @dbid), 
		colList         = sybsystemprocs.dbo.decodeColIdArray(s.colidarray, s.id, @dbid), 
		miss_counter    = convert(int, c0), 
		tableRowCount   = row_count(@dbid, s.id),
		tableDataPages  = data_pages(@dbid, s.id),
		rowsPerDataPage = case
		                    when data_pages(@dbid, s.id) = 0 then convert(numeric(3,1), 0)
		                    else convert(numeric(3,1), row_count(@dbid, s.id) / data_pages(@dbid, s.id))
		                  end,
		s.partitionid, 
		s.indid, 
		datachange      = datachange(o.name, null, null),
		s.moddate
	from sysstatistics s, sysobjects o
	where s.id = o.id
	  and o.type = 'U'
--	  and o.type in('U', 'S')
	  and not (o.sysstat2 & 1024 = 1024 or o.sysstat2 & 2048 = 2048)
	  and s.formatid = 110
end
go

grant exec on sp_missing_stats_in_db to public
go





-------------------------------------------------------------
-- Procedure: sp_missing_stats
-------------------------------------------------------------
if (select object_id('sp_missing_stats')) is not null
begin
	drop procedure sp_missing_stats
	print "Dropping procedure sp_missing_stats"
end
go

declare @dbname varchar(255)
select @dbname = db_name()
print "Creating procedure '%1!.%2!.%3!'.", @dbname, "dbo", "sp_missing_stats"
go

create procedure sp_missing_stats
as
begin
	declare @c_dbname varchar(30), @c_dbid int

	declare db_curs cursor for 
		select name, dbid 
		from master..sysdatabases 
--		where name not in('master', 'tempdb', 'model', 'sybsystemdb', 'sybsystemprocs', 'sybpcidb', 'sybmgmtdb', 'sybpcidb')
		where name not in(          'tempdb', 'model', 'sybsystemdb', 'sybsystemprocs', 'sybpcidb', 'sybmgmtdb', 'sybpcidb')
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

		exec(@c_dbname+"..sp_missing_stats_in_db")

		fetch db_curs into @c_dbname, @c_dbid
	end
	close db_curs
	return 0
end
go

grant exec on sp_missing_stats to public
go

use master
go
--sp_missing_stats
go



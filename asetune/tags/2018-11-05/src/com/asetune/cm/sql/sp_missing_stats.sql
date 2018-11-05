/*
** Started to use Rob Verschoor function/procedure for this
** My original code is still in here, but as comments
**
** Rob Verschoor article/code can be found at:
** http://blogs.sybase.com/database/2010/05/making-good-on-my-exercise-for-the-reader/
*/
use sybsystemprocs
go
set nocount on
go

-------------------------------------------------------------
--- NOTE -- NOTE -- NOTE -- NOTE -- NOTE -- NOTE -- NOTE ---- 
-------------------------------------------------------------
--
-- If you change anything in here, do NOT forget to change
-- the field 'SP_MISSING_STATS_CR_STR' in 'com.asetune.cm.sql.VersionInfo'
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
-- Function: decodeColIdArray
-------------------------------------------------------------
--if (select object_id('decodeColIdArray')) is not null
--begin
--	drop function decodeColIdArray
--	print "Dropping function decodeColIdArray"
--end
go

--declare @dbname varchar(255)
--select @dbname = db_name()
--print "Creating function '%1!.%2!.%3!'.", @dbname, "dbo", "decodeColIdArray"
go

--create function decodeColIdArray(@colIdArray varbinary(100), @tabId int, @dbId int)
--returns varchar(1024)
--as
--begin
--	declare @outStr varchar(1024)
--
--	if (@dbId is null)
--		select @dbId = db_id()
--
--	while(@colIdArray != null)
--	begin
--		-- Get the colname
--		select  @outStr = @outStr + col_name(@tabId, convert(smallint, left(@colIdArray, 2)), @dbId)
--
--		-- get NEXT colId from the array, if empty get out of there
--		select @colIdArray = substring(@colIdArray, 3, 255)
--		if (@colIdArray = null)
--			break
--
--		-- Append ", "
--		select  @outStr = @outStr + ", "
--	end
--	return @outStr
--end
go

--grant exec on decodeColIdArray to public
go




-------------------------------------------------------------
-- Function: sp_decode_colidarray
-------------------------------------------------------------
create table #ixcols
(
	id int,
	ixname longsysname,
	colname longsysname,
	n int)
go

if (select object_id('sp_decode_colidarray')) is not null
begin
	drop function sp_decode_colidarray
	print "Dropping function sp_decode_colidarray"
end
go

declare @dbname varchar(255)
select @dbname = db_name()
print "Creating function '%1!.%2!.%3!'.", @dbname, "dbo", "sp_decode_colidarray"
go

create function sp_decode_colidarray(@colidarray varbinary(100), @id int)
	returns varchar (1500)
as
begin
	declare @s varchar (1500) -- assuming this is long enough for the result
	declare @len int, @colid int, @colname longsysname
	declare @ixname longsysname, @n int, @ixcol longsysname

	if datalength(@colidarray)%2 = 1
	begin
	    set @colidarray = @colidarray + 0x00
	end

	set @len = 1
	while @len < datalength(@colidarray)
	begin
		set @colid = convert(smallint, substring(@colidarray, @len, 2))
		set @colname = col_name(@id, @colid)

		-- determine which index this column is part of
		select top 1 @n = n, @ixname = ixname
		from #ixcols
		where id = @id
		and colname = @colname
		order by id, colname, n

		if @@rowcount = 0
		   set @ixcol = "not indexed!"
		else
		   set @ixcol = @ixname + ':col#' + convert(varchar,@n)

		set @s = @s + case @s when NULL then NULL else ', ' end
		     + @colname + '(' + @ixcol + ')'
		set @len = @len + 2
	end
	return @s
end
go

grant exec on sp_decode_colidarray to public
go

drop table #ixcols
go

declare @dbname varchar(255)
select @dbname = db_name()
if ((select object_id('sp_decode_colidarray')) is not null)
	print "Creating function '%1!.%2!.%3!'. SUCCEEDED", @dbname, "dbo", "sp_decode_colidarray"
else
	print "Creating function '%1!.%2!.%3!'. FAILED", @dbname, "dbo", "sp_decode_colidarray"
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

--create procedure sp_missing_stats_in_db
--as
--begin
--	declare @dbid int
--	select @dbid = db_id()
--
--	select	
--		DBName          = db_name(), 
--		ObjectName      = object_name(s.id, @dbid), 
--		LockScheme      = lockscheme(s.id, @dbid),
--		colList         = sybsystemprocs.dbo.decodeColIdArray(s.colidarray, s.id, @dbid), 
--		miss_counter    = convert(int, c0), 
--		tableRowCount   = row_count(@dbid, s.id),
--		tableDataPages  = data_pages(@dbid, s.id),
--		rowsPerDataPage = case
--		                    when data_pages(@dbid, s.id) = 0 then convert(numeric(16,1), 0)
--		                    else convert(numeric(16,1), row_count(@dbid, s.id) / data_pages(@dbid, s.id))
--		                  end,
--		s.partitionid, 
--		s.indid, 
--		datachange      = datachange(o.name, null, null),
--		s.moddate
--	from sysstatistics s, sysobjects o
--	where s.id = o.id
--	  and o.type = 'U'
----	  and o.type in('U', 'S')
--	  and not (o.sysstat2 & 1024 = 1024 or o.sysstat2 & 2048 = 2048)
--	  and s.formatid = 110
--end
go

create proc sp_missing_stats_in_db
(
	@tabname varchar (100) = '%'
)
as
begin
	declare @n int

	select top 31 n=identity(int) into #n from syscolumns

	-- Get a list of all indexed columns for the qualifiying table(s);
	-- this list will be used inside the SQL function 'sp_decode_colidarray'
	-- Note how a Cartesian product is deliberately used for table #n
	select
		id=o.id,
		ixname=i.name,
		colname=index_col(o.name, i.indid, #n.n, o.uid),
		#n.n
	into #ixcols
	from sysindexes i, #n, sysstatistics s, sysobjects o
	where s.id = o.id
	  and o.id = i.id
	  and o.type = "U"
	  and i.indid > 0 and i.indid < 255   /* this line updated June 23rd */
	  and not (o.sysstat2 & 1024 = 1024 or o.sysstat2 & 2048 = 2048) -- proxy tables /* gorans, added 2011-10-10 */
	  and s.formatid = 110                                                           /* gorans, added 2011-10-10 */
	  and datalength(s.colidarray) > 0                                               /* gorans, added 2011-10-10 */

--	select
--		DBname     = db_name(),
--		Tabname    = object_name(s.id),
--		NrRows     = row_count(db_id(), s.id),
--		ColumnList = dbo.sp_decode_colidarray(colidarray, s.id),
--		Captured   = moddate,
--		Occurs     = convert (smallint,c0)
--	into #missing
--	from sysstatistics s, sysobjects o
--	where s.id = o.id
--	and o.name like @tabname
--	and formatid = 110
--	and datalength(colidarray) > 0
--
--	exec sp_autoformat #missing,@orderby='order by 2'

	declare @dbid int
	select @dbid = db_id()

	select	
		DBName          = db_name(), 
		ObjectName      = object_name(s.id, @dbid), 
		LockScheme      = lockscheme(s.id, @dbid),
--		colList         = sybsystemprocs.dbo.decodeColIdArray(s.colidarray, s.id, @dbid), 
		colList         = dbo.sp_decode_colidarray(s.colidarray, s.id),
		miss_counter    = convert(int, c0), 
		tableRowCount   = row_count(@dbid, s.id),
		tableDataPages  = data_pages(@dbid, s.id),
		rowsPerDataPage = case
		                    when data_pages(@dbid, s.id) = 0 then convert(numeric(16,1), 0)
		                    else convert(numeric(16,1), row_count(@dbid, s.id) / data_pages(@dbid, s.id))
		                  end,
		s.partitionid, 
		s.indid, 
		datachange      = datachange(o.name, null, null)--,
--		updStatDate     = isnull((select convert(varchar(30),s2.moddate,109) from sysstatistics s2 where s2.id = s.id and s2.formatid = 100),'not found') -- formatid = 100 seems to be 'update statistics info'
	from sysstatistics s, sysobjects o
	where s.id = o.id
	  and o.name like @tabname
	  and o.type = 'U'
--	  and o.type in('U', 'S')
	  and not (o.sysstat2 & 1024 = 1024 or o.sysstat2 & 2048 = 2048) -- proxy tables
	  and s.formatid = 110
	  and datalength(s.colidarray) > 0

end
go

grant exec on sp_missing_stats_in_db to public
go

declare @dbname varchar(255)
select @dbname = db_name()
if ((select object_id('sp_missing_stats_in_db')) is not null)
	print "Creating procedure '%1!.%2!.%3!'. SUCCEEDED", @dbname, "dbo", "sp_missing_stats_in_db"
else
	print "Creating procedure '%1!.%2!.%3!'. FAILED", @dbname, "dbo", "sp_missing_stats_in_db"
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

	-- master..sysdatabases.status
	-- 32 = Database created with for load option, or crashed while loading database, instructs recovery not to proceed
	-- 256 = Database suspect | Not recovered | Cannot be opened or used | Can be dropped only with dbcc dbrepair

	declare db_curs cursor for 
		select name, dbid 
		from master..sysdatabases 
--		where name not in('master', 'tempdb', 'model', 'sybsystemdb', 'sybsystemprocs', 'sybpcidb', 'sybmgmtdb', 'sybpcidb')
		where name not in(          'tempdb', 'model', 'sybsystemdb', 'sybsystemprocs', 'sybpcidb', 'sybmgmtdb', 'sybpcidb')
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

		exec(@c_dbname+"..sp_missing_stats_in_db")

		fetch db_curs into @c_dbname, @c_dbid
	end
	close db_curs
	return 0
end
go

grant exec on sp_missing_stats to public
go

declare @dbname varchar(255)
select @dbname = db_name()
if ((select object_id('sp_missing_stats')) is not null)
	print "Creating procedure '%1!.%2!.%3!'. SUCCEEDED", @dbname, "dbo", "sp_missing_stats"
else
	print "Creating procedure '%1!.%2!.%3!'. FAILED", @dbname, "dbo", "sp_missing_stats"
go


use master
go
--sp_missing_stats
go




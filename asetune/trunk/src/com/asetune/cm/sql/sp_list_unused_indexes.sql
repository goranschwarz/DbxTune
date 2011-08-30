use sybsystemprocs
go
set nocount on
go

-------------------------------------------------------------
--- NOTE -- NOTE -- NOTE -- NOTE -- NOTE -- NOTE -- NOTE ---- 
-------------------------------------------------------------
--
-- If you change anything in here, do NOT forget to change
-- the field 'SP_LIST_UNUSED_INDEXES_CR_STR' in 'com.asetune.cm.sql.VersionInfo'
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
-- Function: getIndexColumns
-------------------------------------------------------------
--if (select object_id('getIndexColumns')) is not null
--begin
--	drop function getIndexColumns
--	print "Dropping function getIndexColumns"
--end
go

--declare @dbname varchar(255)
--select @dbname = db_name()
--print "Creating function '%1!.%2!.%3!'.", @dbname, "dbo", "getIndexColumns"
go

--create function getIndexColumns(@tabId int, @indid int, @dbId int)
--returns varchar(1024)
--as
--begin
--	declare @keys    varchar(1024)
--	declare @objname varchar(255)
--	declare @i       int
--	declare @thiskey varchar(255)
--	declare @sorder  char(4)
--
--	set nocount on
--
--	if (@dbId is null)
--		select @dbId = db_id()
--
--	select @objname = object_name(@tabId, @dbId)
--	select @keys = "", @i = 1
--	select @keys = @objname+":"+convert(varchar(10),@indid)+";", @i = 1
--
--	while @i <= 31
--	begin
--		select @thiskey = index_col(@objname, @indid, @i)
--
--		select @keys = @keys + convert(varchar(10),@i)+"="+@thiskey
--		if (@thiskey is NULL) 
--		begin
--			break
--		end
--
--		if @i > 1
--		begin
--			select @keys = @keys + ", " 
--		end
--
--		select @keys = @keys + @thiskey
--
--		/*
--		** Get the sort order of the column using index_colorder()
--		** This support is added for handling descending keys.
--		*/
--		select @sorder = index_colorder(@objname, @indid, @i)
--		if (@sorder = "DESC")
--			select @keys = @keys + " " + @sorder
--
--		/*
--		**  Increment @i so it will check for the next key.
--		*/
--		select @i = @i + 1
--	end
--	set nocount off
--
--	return @keys
--end
go

--grant exec on getIndexColumns to public
go




-------------------------------------------------------------
-- Function: decodeIndexKeyArray
-------------------------------------------------------------
if (select object_id('decodeIndexKeyArray')) is not null
begin
	drop function decodeIndexKeyArray
	print "Dropping function decodeIndexKeyArray"
end
go

declare @dbname varchar(255)
select @dbname = db_name()
print "Creating function '%1!.%2!.%3!'.", @dbname, "dbo", "decodeIndexKeyArray"
go

/*
 * Hmm... wonder if this will work on all platforms...
 */
create function decodeIndexKeyArray(@keyArray1 varbinary(255), @keyArray2 varbinary(255), @tabId int, @indid int, @dbId int)
returns varchar(1024)
as
begin
	declare @keyArray   varbinary(512)
	declare @outStr     varchar(1024)
	declare @colid      smallint

	select @keyArray = @keyArray1 + @keyArray2
	if (@dbId is null)
		select @dbId = db_id()

	-- If it's a TEXT/IMAGE column, just get the column name
	if (@indid >= 255)
	begin
		--select @outStr = col_name(@tabId, @colid, @dbId)
		return @outStr
	end

	while(@keyArray != null)
	begin
		-- Get the colname
		select @colid = convert(smallint, substring(@keyArray, 5, 2))
		if (@colid = null)
			break

		--select @outStr = @outStr + "id="+convert(varchar(20),@colid)+":"
		select  @outStr = @outStr + col_name(@tabId, @colid, @dbId)

		-- get NEXT colId from the array, if empty get out of there
		select @keyArray = substring(@keyArray, 17, 512)
		if (@keyArray = null OR datalength(@keyArray) < 10)
			break

		-- Append ", "
		select  @outStr = @outStr + ", "
	end
	-- Take away last comma string, if there is any
	if (@outStr like '%, ')
		select  @outStr = substring(@outStr, 1, char_length(@outStr)-2)

	return @outStr
end
go

grant exec on decodeIndexKeyArray to public
go





-------------------------------------------------------------
-- Procedure: sp_list_unused_indexes_in_db
-------------------------------------------------------------
if (select object_id('sp_list_unused_indexes_in_db')) is not null
begin
	drop procedure sp_list_unused_indexes_in_db
	print "Dropping procedure sp_list_unused_indexes_in_db"
end
go

declare @dbname varchar(255)
select @dbname = db_name()
print "Creating procedure '%1!.%2!.%3!'.", @dbname, "dbo", "sp_list_unused_indexes_in_db"
go

create procedure sp_list_unused_indexes_in_db
as
begin
	declare @dbid int
	select @dbid = db_id()

	select	
		DBName          = db_name(), 
		TableName       = object_name(i.id, @dbid), 
		TableLockScheme = lockscheme(i.id, @dbid),
		IndexId         = i.indid, 
		IndexName       = i.name,
		IndexType       = case
		                    when i.indid >= 255 then "TEXT/IMAGE"
		                    when i.indid = 1    then "clustered"
		                    else                     "nonclustered"
		                  end,
		IndexDesciption = case
		                    when (i.status & 32768) = 32768 then "WARNING: SUSPECT"
		                    when (i.status & 2) = 2         then "unique"
		                    else                                 "nonunique"
		                  end,
		IndexColumns    = sybsystemprocs.dbo.decodeIndexKeyArray(i.keys1, i.keys2, i.id, i.indid, @dbid), 
		tableRowCount   = row_count(@dbid, i.id),
		tableDataPages  = data_pages(@dbid, i.id),
		rowsPerDataPage = case
		                    when data_pages(@dbid, i.id) = 0 then convert(numeric(16,1), 0)
		                    else convert(numeric(16,1), row_count(@dbid, i.id) / data_pages(@dbid, i.id))
		                  end,
		IndexPages      = data_pages(@dbid, i.id, i.indid),
		rowsPerIndexPage= case
		                    when data_pages(@dbid, i.id, i.indid) = 0 then convert(numeric(16,1), 0)
		                    else convert(numeric(16,1), row_count(@dbid, i.id) / data_pages(@dbid, i.id, i.indid))
		                  end,
--		i.partitionid, 
--		i.indid, 
--		datachange      = datachange(o.name, null, null),
--		s.moddate
		CreationDate    = i.crdate
	from master..monOpenObjectActivity a, sysindexes i
	where i.id    *= a.ObjectID 
	  and i.indid *= a.IndexID
	  and (a.UsedCount = 0 or a.UsedCount is NULL)
	  and i.indid > 0
	  and i.id > 99 -- No system tables
end
go

grant exec on sp_list_unused_indexes_in_db to public
go





-------------------------------------------------------------
-- Procedure: sp_missing_stats
-------------------------------------------------------------
if (select object_id('sp_list_unused_indexes')) is not null
begin
	drop procedure sp_list_unused_indexes
	print "Dropping procedure sp_list_unused_indexes"
end
go

declare @dbname varchar(255)
select @dbname = db_name()
print "Creating procedure '%1!.%2!.%3!'.", @dbname, "dbo", "sp_list_unused_indexes"
go

create procedure sp_list_unused_indexes( @dbname varchar(30) = null )
as
begin
	declare @c_dbname varchar(30), @c_dbid int

	if (@dbname is not null)
	begin
		exec(@dbname+"..sp_list_unused_indexes_in_db")
		return 0
	end

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

		exec(@c_dbname+"..sp_list_unused_indexes_in_db")

		fetch db_curs into @c_dbname, @c_dbid
	end
	close db_curs
	return 0
end
go

grant exec on sp_list_unused_indexes to public
go

use master
go
--sp_list_unused_indexes
go



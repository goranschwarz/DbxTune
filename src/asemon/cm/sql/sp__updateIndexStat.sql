-------------------------------------------------------------------------
--- NOT YET READY -- NOT YET READY -- NOT YET READY -- NOT YET READY ---- 
-------------------------------------------------------------------------
print "PLEASE DO NOT USE THIS PRC YET, IT'S UNDER DEVELOPMENT"
go
exit
go


-------------------------------------------------------------
--- NOTE -- NOTE -- NOTE -- NOTE -- NOTE -- NOTE -- NOTE ---- 
-------------------------------------------------------------
--
-- If you change anything in here, do NOT forget to change
-- the field 'SP_XXX_CR_STR' in 'asemon.cm.sql.VersionInfo'
--
-- Otherwise it will NOT be recreated... when asemon starts...
--
-------------------------------------------------------------
set nocount on
go

-------------------------------------------------------------
-- Drop in master
-------------------------------------------------------------
use master
go
if ((select object_id('sp__updateIndexStat')) is not null)
begin
	print "  drop procedure: sp__updateIndexStat      in: master"
	drop procedure sp__updateIndexStat
end
go
if ((select object_id('sp__updateIndexStatAllDB')) is not null)
begin
	print "  drop procedure: sp__updateIndexStatAllDB in: master"
	drop procedure sp__updateIndexStatAllDB
end
go


-------------------------------------------------------------
-- Drop in sybsystemprocs
-------------------------------------------------------------
use sybsystemprocs
go
if ((select object_id('sp__updateIndexStat')) is not null)
begin
	print "  drop procedure: sp__updateIndexStat      in: sybsystemprocs"
	drop procedure sp__updateIndexStat
end
go
if ((select object_id('sp__updateIndexStatAllDB')) is not null)
begin
	print "  drop procedure: sp__updateIndexStatAllDB in: sybsystemprocs"
	drop procedure sp__updateIndexStatAllDB
end
go


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
-- Procedure: sp__updateIndexStat
-------------------------------------------------------------
declare @dbname varchar(255)
select @dbname = db_name()
print "create procedure '%1!.%2!.%3!'.", @dbname, "dbo", "sp__updateIndexStat"
go

/*=====================================================================**
** PROCEDURE: sp__updateIndexStat
**---------------------------------------------------------------------**
** Description:
**
** Run 'update index statistics' for all table
** where more than 10% of rows (with columns in a index is changed)
**
**  If table has more than 1mill rows I add 'with sampling = 20'
**  Could also add 'with consumers = 20'
**  
** There are other possiblities for enhancements
**
**---------------------------------------------------------------------**
** Input parameters:
**
** @limit      datachange() must be above this value
** @makeScript Make a script that can be executed at a later time
**
**---------------------------------------------------------------------**
** output parameters:
**
**---------------------------------------------------------------------**
** output select:
**
**---------------------------------------------------------------------**
** Return codes:
**
** 0	- ok.
** 1 	- error when accessing the database
**
**---------------------------------------------------------------------**
** Error codes:
**
**---------------------------------------------------------------------**
** History:
**
** 2008-mm-dd  1.0.1  Henrik H. Larsen, Sybase Denmark
**                    Created
** 2011-04-14  1.0.1  Goran Schwarz, Sybase Sweden
**                    Just name and format changes
**---------------------------------------------------------------------*/

create proc sp__updateIndexStat
(
	@limit      int = 5,
	@makeScript int = 1
)
as 
begin
	declare @Id      int
	declare @indId   int
	declare @keyCnt  int
	declare @i       int
	declare @mustRun int
	declare @col     varchar(255)
	declare @table   varchar(255)
	declare @index   varchar(255)
	declare @cmd     varchar(4096)
	declare @tsStamp varchar(20)
	declare @maxRows numeric(12)
	declare @datachange int

	if (@makeScript > 0)
	begin
		declare @dbname varchar(30)
		select @dbname = db_name()
		print '------------------------------------------------------'
		print 'set flushmessage on'
		print 'set nocount on'
		print 'go'
		print 'use %1!', @dbname
		print 'go'
		print '------------------------------------------------------'
		print 'go'
	end

	DECLARE inxColCursor cursor for
	   SELECT si.id, si.name, si.indid 
	     FROM sysindexes si join sysobjects so on (si.id = so.id)
	    WHERE so.type = 'U'
	      AND si.keycnt between 1 and 250
	FOR READ ONLY

	OPEN inxColCursor
	FETCH inxColCursor INTO @Id, @index, @indId

	while (@@sqlstatus = 0) 
	begin
		set @mustRun = 0
		set @i = 1
		set @table = object_name(@Id)
 
		while (@mustRun = 0 AND @i <= 126)  -- way over max(31), but should be future proof
		begin

			set @col = index_col (@table, @indId, @i)
			if (@col is NULL)
				break

			select @datachange = datachange (@table, NULL,  @col)
			if (@datachange >= @limit)
				set @mustRun = 1

			set @i = @i + 1

		end

		if (@mustRun = 1) 
		begin
			set @tsStamp = convert(varchar(20), getdate(), 117)
			set @cmd = 'update index statistics '+@table +' '+@index

			select @maxRows = max(rowcnt) from systabstats where id = @Id 

			if (@maxRows is not NULL AND @maxRows > 1000000) 
			begin
				set @cmd = @cmd + ' with sampling = ' + 
					(case 
					when @maxRows > 250000000   then '  1 '
					when @maxRows > 150000000   then '  2 '
					when @maxRows > 100000000   then '  5 '
					when @maxRows >  50000000   then '  7 '
					when @maxRows >  10000000   then ' 10 '
					when @maxRows >   5000000   then ' 25 '
					when @maxRows >   1000000   then ' 50 '
					else ' 100 '
					end)
			end

			if (@makeScript > 0)
			begin
				declare @msg varchar(1024)

				-- Print logic for PRE printing
				print         "        ----------------------------------------------------------------------"
				print         "        -- datachange() was %1! for table '%2!', index '%3!'", @datachange, @table, @index
				print         "        ----------------------------------------------------------------------"
				select @msg = "        declare @str varchar(30), @dt datetime, @exeTime int "
				print '%1!', @msg
				select @msg = "        select @dt = getdate(), @str = convert(varchar(30),getdate(),116) "
				print '%1!', @msg
				select @msg = "        print '    >> %1!, EXECUTING: "+@cmd+"', @str"
				print '%1!', @msg

				-- Print the command to execute
				print '--------------------------------------------------------------------------'
				print '%1!', @cmd
			--	print 'waitfor delay "00:00:10"'
				print '--------------------------------------------------------------------------'

				-- Print logic for POST printing
				select @msg = "        select @exeTime = datediff(mi, @dt, getdate()) "
				print '%1!', @msg
				select @msg = "        print '    << This took %1! minutes', @exeTime"
				print '%1!', @msg
				select @msg = "        print ''"
				print '%1!', @msg
				print 'go'
				print ''
			end
			else
			begin
				print '%1! : ** Runs : %2!',@tsStamp, @cmd
				exec(@cmd)
			end
		end

		FETCH inxColCursor INTO @Id, @index, @indId
	end

	close inxColCursor
	deallocate cursor inxColCursor
end
go
grant exec on sp__updateIndexStat to oper_role
go


-------------------------------------------------------------
-- Procedure: sp__updateIndexStatAllDB
-------------------------------------------------------------
declare @dbname varchar(255)
select @dbname = db_name()
print "create procedure '%1!.%2!.%3!'.", @dbname, "dbo", "sp__updateIndexStatAllDB"
go

/*=====================================================================**
** PROCEDURE: sp__updateIndexStatAllDB
**---------------------------------------------------------------------**
** Description:
**
** Run 'sp__updateIndexStat ' for all user databases
** when more than 10% of rows (with columns in a index is changed)
**
** There are posiblities for enhancements
**
**---------------------------------------------------------------------**
** Input parameters:
**
** @limit      datachange() must be above this value
** @makeScript Make a script that can be executed at a later time
**
**---------------------------------------------------------------------**
** output parameters:
**
**---------------------------------------------------------------------**
** output select:
**
**---------------------------------------------------------------------**
** Return codes:
**
** 0	- ok.
** 1 	- error when accessing the database
**
**---------------------------------------------------------------------**
** Error codes:
**
**---------------------------------------------------------------------**
** History:
**
** 2008-mm-dd  1.0.1  Henrik H. Larsen, Sybase Denmark
**                    Created
** 2011-04-14  1.0.1  Goran Schwarz, Sybase Sweden
**                    Just name and format changes
**---------------------------------------------------------------------*/

create proc sp__updateIndexStatAllDB
(
	@limit      int = 5,
	@makeScript int = 1
)
as
begin
	declare @dbName varchar(255)
	declare @tsStamp varchar(20)

	DECLARE dbCursor cursor for
	   SELECT name +'..sp__updateIndexStat'
	     FROM master..sysdatabases
	    WHERE name not like 'tempdb%'
	      AND name not in ('master', 'sybsecurity', 'sybsystemdb', 'sybsystemprocs','model', 'tempdb')
	      AND (status & 32 = 0) 
	      AND (status2 & 256 = 0)
	      AND (status3 & 256 = 0) -- Ignore user tempdb
	FOR READ ONLY

	open dbCursor
	fetch dbCursor into @dbName

	while (@@sqlstatus = 0) 
	begin
		set @tsStamp = convert(varchar(20), getdate(), 117)
		print '------------- >>> %1! : in database : %2! <<< -------------',@tsStamp, @dbName
		exec @dbName @limit, @makeScript
		fetch dbCursor into @dbName
	end
	close dbCursor
	deallocate cursor dbCursor

end
go

grant exec on sp__updateIndexStatAllDB to oper_role
go



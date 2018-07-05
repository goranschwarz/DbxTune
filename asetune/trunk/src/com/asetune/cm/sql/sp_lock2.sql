-------------------------------------------------------------
--- NOTE -- NOTE -- NOTE -- NOTE -- NOTE -- NOTE -- NOTE ---- 
-------------------------------------------------------------
--
-- If you change anything in here, do NOT forget to change
-- the field 'SP_XXX_CR_STR' in 'com.asetune.cm.sql.VersionInfo'
--
-- Otherwise it will NOT be recreated... when asetune starts...
--
-------------------------------------------------------------
set nocount on
go

-------------------------------------------------------------
-- Drop in master
-------------------------------------------------------------
use master
go
if ((select object_id('sp_lock2')) is not null)
begin
	print '  drop procedure: sp_lock2      in: master'
	drop procedure sp_lock2
end
go


-------------------------------------------------------------
-- Drop in sybsystemprocs
-------------------------------------------------------------
use sybsystemprocs
go
if ((select object_id('sp_lock2')) is not null)
begin
	print '  drop procedure: sp_lock2      in: sybsystemprocs'
	drop procedure sp_lock2
end
go


-------------------------------------------------------------
-- Make sure we are NOT in master database
-------------------------------------------------------------
if ( (select db_name()) = 'master' )
begin
	print 'WRONG DATABASE: you should be in a USER DATABASE when creating this procedure.'
end
go
if ( (select db_name()) = 'master' )
begin
	select syb_quit()
end
go






-------------------------------------------------------------
-- Procedure: sp_lock2
-------------------------------------------------------------
declare @dbname varchar(255)
select @dbname = db_name()
print 'create procedure ''%1!.%2!.%3!''.', @dbname, 'dbo', 'sp_lock2'
go

/* Sccsid = "%Z% generic/sproc/%M% %I% %G%" */
/*	4.8	1.1	06/14/90	sproc/src/lock */
/*
** Messages for "sp_lock2"
**
** 18052, "The class column will display the cursor name for locks associated
**         with a cursor for the current user and the cursor id for other 
**         users."
*/


create procedure sp_lock2
@spid1 int = NULL,		/* server process id to check for locks */
@spid2 int = NULL		/* other process id to check for locks */
as

declare @length int
declare @msg varchar(250)


if @@trancount = 0
begin
	set chained off
end

set transaction isolation level 1

/*  Print warning message about cursor lock info:
**  18052, "The class column will display the cursor name for locks associated
**          with a cursor for the current user and the cursor id for other 
**          users."
*/
exec sp_getmessage 18052, @msg out
print @msg

/*
**  Show the locks for both parameters.
*/
if @spid1 is not NULL
begin
    select @length = max(datalength(db_name(dbid)))
	from master..syslocks
		where spid in (@spid1, @spid2)

    if (@length > 15)

	select fid, spid, locktype = v1.name, table_id = convert(varchar(30),object_name(id,dbid)), page, row,
		dbname = db_name(dbid), class, context=v2.name
	from master..syslocks l, master..spt_values v1, master..spt_values v2
		where l.type = v1.number
			and v1.type = 'L'
			and (l.context+2049) = v2.number
			and v2.type = 'L2'
			and spid in (@spid1, @spid2)
    else
	select fid, spid, locktype = v1.name, table_id = convert(varchar(30),object_name(id,dbid)), page, row,
		dbname = convert(char(15), db_name(dbid)), class,context=v2.name
	from master..syslocks l, master..spt_values v1, master..spt_values v2
		where l.type = v1.number
			and v1.type = 'L'
			and (l.context+2049) = v2.number
			and v2.type = 'L2'
			and spid in (@spid1, @spid2)
end

/*
**  No parameters, so show all the locks.
*/
else
begin
    select @length = max(datalength(db_name(dbid)))
	from master..syslocks

    if (@length > 15)
	select fid, spid, locktype = v1.name, table_id = convert(varchar(30),object_name(id,dbid)), page, row,
		dbname = db_name(dbid), class, context=v2.name
	from master..syslocks l, master..spt_values v1, master..spt_values v2
		where l.type = v1.number
			and v1.type = 'L'
			and (l.context+2049) = v2.number
			and v2.type = 'L2'
	order by fid, spid, dbname, table_id, locktype, page, row
    else
	select fid, spid, locktype = v1.name, table_id = convert(varchar(30),object_name(id,dbid)), page, row,
		dbname = convert(char(15), db_name(dbid)), class, context=v2.name
	from master..syslocks l, master..spt_values v1, master..spt_values v2
		where l.type = v1.number
			and v1.type = 'L'
			and (l.context+2049) = v2.number
			and v2.type = 'L2'
	order by fid, spid, dbname, table_id, locktype, page, row
end

return (0)
go


exec sp_procxmode 'sp_lock2', 'anymode'
go
grant exec on sp_lock2 to public
go 

declare @dbname varchar(255)
select @dbname = db_name()
if ((select object_id('sp_lock2')) is not null)
	print 'create procedure ''%1!.%2!.%3!''. SUCCEEDED', @dbname, 'dbo', 'sp_lock2'
else
	print 'create procedure ''%1!.%2!.%3!''. FAILED', @dbname, 'dbo', 'sp_lock2'
go

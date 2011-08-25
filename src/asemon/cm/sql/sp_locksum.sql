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
if ((select object_id('sp_locksum')) is not null)
begin
	print "  drop procedure: sp_locksum      in: master"
	drop procedure sp_locksum
end
go


-------------------------------------------------------------
-- Drop in sybsystemprocs
-------------------------------------------------------------
use sybsystemprocs
go
if ((select object_id('sp_locksum')) is not null)
begin
	print "  drop procedure: sp_locksum      in: sybsystemprocs"
	drop procedure sp_locksum
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
-- Procedure: sp_locksum
-------------------------------------------------------------
declare @dbname varchar(255)
select @dbname = db_name()
print "create procedure '%1!.%2!.%3!'.", @dbname, "dbo", "sp_locksum"
go

/* Sccsid = "%Z% generic/sproc/%M% %I% %G%" */
/*	4.8	1.1	06/14/90	sproc/src/lock */
/*
** Messages for "sp_locksum"
**
** 18052, "The class column will display the cursor name for locks associated
**         with a cursor for the current user and the cursor id for other 
**         users."
*/


create procedure sp_locksum
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

	select dbname = db_name(dbid), table_name = convert(varchar(30),object_name(id,dbid)), locktype = v1.name, context=v2.name, spid, num_of_locks = count(*)
	from master..syslocks l, master..spt_values v1, master..spt_values v2
		where l.type = v1.number
			and v1.type = "L"
			and (l.context+2049) = v2.number
			and v2.type = "L2"
	group by db_name(dbid), object_name(id,dbid), v1.name, v2.name, spid
	order by 1, 2, 3, 4, 5

return (0)
go


exec sp_procxmode 'sp_locksum', 'anymode'
go
grant exec on sp_locksum to public
go 

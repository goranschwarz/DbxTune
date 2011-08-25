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
if ((select object_id('sp_opentran')) is not null)
begin
	print "  drop procedure: sp_opentran      in: master"
	drop procedure sp_opentran
end
go


-------------------------------------------------------------
-- Drop in sybsystemprocs
-------------------------------------------------------------
use sybsystemprocs
go
if ((select object_id('sp_opentran')) is not null)
begin
	print "  drop procedure: sp_opentran      in: sybsystemprocs"
	drop procedure sp_opentran
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
-- Procedure: sp_opentran
-------------------------------------------------------------
declare @dbname varchar(255)
select @dbname = db_name()
print "create procedure '%1!.%2!.%3!'.", @dbname, "dbo", "sp_opentran"
go

/*=====================================================================**
** PROCEDURE: sp_opentran
**---------------------------------------------------------------------**
** Description:
**
** Get info about open transactions (based on syslogshold)
**
**---------------------------------------------------------------------**
** Input parameters:
**
** @block_max   show users with block time larger than this
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
** yyyymmdd 1.0.0  Goran Schwarz, Sybase Sweden
**                 Created
**---------------------------------------------------------------------*/

create procedure sp_opentran
(
	@block_max int = 1	/* show users with block time larger than this */
)
as
	set nocount on

	/*
	** Show only the first rows, this is a indication...
	*/
	set rowcount 200

	/*
	** Get information about the user who has
	** started a operation which holds an OPEN transaction
	*/
	print "User information on oldest open transaction."

	select
		h.spid,
		min_opened	= datediff(mi, h.starttime, getdate()),
		starttime	= convert(varchar(30), h.starttime, 109),
		user_name	= suser_name(p.suid),
		dbname		= db_name(h.dbid),
		p.hostname,
		p.hostprocess,
		p.program_name,
		p.cmd,
		p.status,
		h.reserved,
		h.page,
		h.xactid,
		h.masterxactid,
		h.name
	from 	master..syslogshold h,
		master..sysprocesses p
	where h.spid = p.spid
	  and datediff(mi,starttime, getdate()) > @block_max


	/*
	** Get information about the users who are blocked
	*/
	print "What tables does this user has locks in."

	select
		p.fid,
		p.spid,
		object_name	= convert(varchar(30), object_name(l.id, l.dbid)),
		l.page,
		l.row,
		locktype	= v.name
	from    master..sysprocesses p,
		master..syslocks l,
		master..spt_values v
	where p.spid = l.spid
	  and l.type = v.number
	  and v.type = 'L'
	  and p.spid in ( select spid
			from master..syslogshold
			where datediff(mi,starttime, getdate()) > @block_max)
	order by 1,2,3,4,5
go

--print "Granting procedure: sp_opentran execution to public"
go
exec sp_procxmode 'sp_opentran', 'anymode'
go
grant exec on sp_opentran to public
go

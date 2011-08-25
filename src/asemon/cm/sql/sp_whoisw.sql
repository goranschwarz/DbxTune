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
if ((select object_id('sp_whoisw')) is not null)
begin
	print "  drop procedure: sp_whoisw      in: master"
	drop procedure sp_whoisw
end
go


-------------------------------------------------------------
-- Drop in sybsystemprocs
-------------------------------------------------------------
use sybsystemprocs
go
if ((select object_id('sp_whoisw')) is not null)
begin
	print "  drop procedure: sp_whoisw      in: sybsystemprocs"
	drop procedure sp_whoisw
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
-- Procedure: sp_whoisw
-------------------------------------------------------------
declare @dbname varchar(255)
select @dbname = db_name()
print "create procedure '%1!.%2!.%3!'.", @dbname, "dbo", "sp_whoisw"
go

/*=====================================================================**
** PROCEDURE: sp_whoisw
**---------------------------------------------------------------------**
** Description:
**
** Get information about WHO IS Working in the database (version 2)
**
** User defined @orderby and @where clauses will be implemeted any time...
**
**---------------------------------------------------------------------**
** Input parameters:
**
** @short   Fit the output on a 80 char wide terminal
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
** 20070206 1.0.1  Goran Schwarz, Sybase Sweden
**                 Changed column layout
**---------------------------------------------------------------------*/

create procedure sp_whoisw
(
	@short    int         = 0
)
as
begin
	declare @msg varchar(250)


	if @@trancount = 0
	begin
		set transaction isolation level 1
		set chained off
	end

	if (@short = 1)
	begin
		select
			spid   = convert(char(5),  spid),
			blk    = convert(char(5),  blocked),
			timeb  = convert(char(5),  time_blocked),
			status = convert(char(9),  status),
			cmd    = convert(char(14), cmd),
			proc_name = CASE
			                WHEN spid = @@spid THEN convert(varchar(33), "sp_whoisw"        +"("+convert(varchar(5),linenum)+")" )
			                ELSE                    convert(varchar(33), object_name(id,dbid)+"("+convert(varchar(5),linenum)+")" )
			            END
		from master..sysprocesses
		where spid in (select spid from master..syslocks)
		or (    cmd    != "AWATING COMMAND"
		    and status != "recv sleep"
		    and suid   != 0
		   )
		order by fid, spid
	end
	else
	begin
		select
			fid,
			spid,
			blk=convert(char(5),blocked),
			time_blocked,
			status,
			cmd,
			procName = CASE
			                WHEN spid = @@spid THEN convert(varchar(30), "sp_whoisw"         )
			                ELSE                    convert(varchar(30), object_name(id,dbid) )
			            END,
			linenum,
			stmtnum,
			cpu,
			physical_io,
			tran_name,
			loginame=suser_name(suid),
			hostname,
			program_name,
			DBName=db_name(dbid),
			loggedindatetime,
			ipaddr
		from master..sysprocesses
		where spid in (select spid from master..syslocks)
		or (    cmd    != "AWATING COMMAND"
		    and status != "recv sleep"
		    and suid   != 0
		   )
		order by fid, spid
	end

	select status, "Number of spids in this status" = count(*)
	from master..sysprocesses
	where suid != 0
	group by status
	order by status

	select cmd, "Number of spids in this CMD" = count(*)
	from master..sysprocesses
	where suid != 0
	group by cmd
	order by cmd

	select status, cmd, "Number of spids in this status" = count(*)
	from master..sysprocesses
	where suid != 0
	group by status, cmd
	order by status, cmd

	select status, cmd, "SYSTEM spids in this status" = count(*)
	from master..sysprocesses
	where suid = 0
	group by status, cmd
	order by status, cmd

	select "Total users loged in" = count(*)
	from master..sysprocesses
	where suid != 0

	return (0)
end
go

exec sp_procxmode 'sp_whoisw', 'anymode'
go
grant exec on sp_whoisw to public
go

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
if ((select object_id('sp_whoisb')) is not null)
begin
	print "  drop procedure: sp_whoisb      in: master"
	drop procedure sp_whoisb
end
go
if ((select object_id('sp_whoisb_sub_blocks')) is not null)
begin
	print "  drop procedure: sp_whoisb_sub_blocks in: master"
	drop procedure sp_whoisb_sub_blocks
end
go


-------------------------------------------------------------
-- Drop in sybsystemprocs
-------------------------------------------------------------
use sybsystemprocs
go
if ((select object_id('sp_whoisb')) is not null)
begin
	print "  drop procedure: sp_whoisb      in: sybsystemprocs"
	drop procedure sp_whoisb
end
go
if ((select object_id('sp_whoisb_sub_blocks')) is not null)
begin
	print "  drop procedure: sp_whoisb_sub_blocks in: sybsystemprocs"
	drop procedure sp_whoisb_sub_blocks
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






/* DUMMY proc... will be recreated later */
create procedure sp_whoisb_sub_blocks as select "not yet implemented"
go


-------------------------------------------------------------
-- Procedure: sp_whoisb
-------------------------------------------------------------
declare @dbname varchar(255)
select @dbname = db_name()
print "create procedure '%1!.%2!.%3!'.", @dbname, "dbo", "sp_whoisb"
go

/*=====================================================================**
** PROCEDURE: sp_whoisb
**---------------------------------------------------------------------**
** Description:
**
**---------------------------------------------------------------------**
** History:
**
** 2003-03-07 1.0.0  Goran Schwarz, Sybase Sweden
**                   Created
**---------------------------------------------------------------------*/
create procedure sp_whoisb
(
	@block_time	int	= 0,
	@verbose	int	= 1
)
as
begin
	set nocount on

	declare @spid_c int
	declare @counter int
	declare @num int

	select @counter = 0
	select @num = 0

	/*
	** Take a snapshot of the sysprocesses table
	** only for spids that holds any locks.
	*/
	select spid, blocked, time_blocked
	into #block_work
	from master..sysprocesses p 
	where spid in (select spid from master..syslocks)
	   or	(     status != 'recv sleep' 
		  and cmd != "AWATING COMMAND"
		  and suid > 0
		)

	/*
	** Declare the Cursors select statement
	*/
	DECLARE blockers_c cursor
		for
			select spid
			from #block_work
			where blocked = 0 
		for read only

	/*
	** Open and fetch the first two rows
	*/
	OPEN blockers_c
	FETCH blockers_c INTO @spid_c

	/*
	** Loop all rows in the cursor.
	*/
	WHILE (@@sqlstatus = 0)
	BEGIN
		-- print "CHECKING spid %1! for BLOCKS", @spid_c

		if exists (select * from #block_work 
				where blocked = @spid_c 
				  and time_blocked >= @block_time)
		begin
			select @counter = @counter + 1

			print ""
			print ""
			print "=========================================================================="
			print "spid %1! IS BLOCKING OTHER THREADS in server %2!", @spid_c, @@servername
			print "--------------------------------------------------------------------------"

			print ""
			print "---------------------------------------------"
			print " This is the SQL text the client executed"
			print "---------------------------------------------"
--			print " Not implemented in version 11.5.x"
--			print " If you run a later version edit the procedure"
			dbcc traceon(3604)
			dbcc sqltext(@spid_c)
			print "---------------------------------------------"
			
			print ""
			print "---------------------------------------------"
			print " Try to get a SHOPLAN output of the command."
			print "---------------------------------------------"
			exec sp_showplan @spid_c, NULL, NULL, NULL
			print "---------------------------------------------"

			/*
			** Some info about the blocking spid.
			*/
			print ""
			print "---------------------------------------------"
			print " Info from sysprocesses."
			print "---------------------------------------------"
			select 
				fid,
				spid,
				status,
				loginame=suser_name(suid),
				hostname,
				program_name,
				blk=convert(char(5),blocked),
				time_blocked,
				dbname=db_name(dbid),
				cmd, 
				cpu,
				physical_io,
				proc_name = convert(varchar(30), object_name(id,dbid)),
				stmtnum,
				linenum,
				loggedindatetime,
				ipaddr
			from master..sysprocesses
			where spid = @spid_c
			print "---------------------------------------------"

			/*
			** Lock info (sp_lock info)
			*/
			print ""
			print "---------------------------------------------"
			print " Info about locks that the statement takes."
			print "---------------------------------------------"
			select 	
				fid, 
				spid, 
				locktype = v1.name, 
				table_name = convert(varchar(30), object_name(id,dbid)), 
				page, 
				row,
				dbname = convert(char(15), db_name(dbid)), 
				class, 
				context=v2.name
			from master..syslocks l, 
				master..spt_values v1, 
				master..spt_values v2
			where l.type = v1.number
				and v1.type = "L"
				and (l.context+2049) = v2.number
				and v2.type = "L2"
				and l.spid = @spid_c
			order by fid, spid, dbname, table_name, locktype, page, row
			print "---------------------------------------------"

			if (@verbose != 0 )
			begin
				print ""
				print "---------------------------------------------"
				print " What other spids are we blocking with this user."
				print "---------------------------------------------"
				exec sp_whoisb_sub_blocks 
					@spid = @spid_c, 
					@level = 0,
					@num = @num OUTput
				print "---------------------------------------------"
				print "spid %1! was blocking %2! other threads.", @spid_c, @num
				print "---------------------------------------------"
			end
		end

		FETCH blockers_c INTO @spid_c
	END
	CLOSE blockers_c
	DEALLOCATE CURSOR blockers_c


	if ( @counter = 0 )
	begin
		print ""
		print ""
		print "=========================================================================="
		print "NO spids are blocking others from working in server %1!.", @@servername
		print "--------------------------------------------------------------------------"
	end


	/*
	** Normal exit point
	*/
/*-->*/	return(0)
end
go
exec sp_procxmode 'sp_whoisb', 'anymode'
go
grant exec on sp_whoisb to public
go











/*
** Create this temporary, otherwise the below proc will not create.
*/
select spid, blocked, time_blocked
into #block_work
from master..sysprocesses p 
where 1=2
go


/* DROP is done at the START + create a dummy stored proc */
drop procedure sp_whoisb_sub_blocks
go
print "create procedure: sp_whoisb_sub_blocks"
go
/*=====================================================================**
** PROCEDURE: PROCNAME
**---------------------------------------------------------------------**
** Description:
**
**---------------------------------------------------------------------*/
create procedure sp_whoisb_sub_blocks
(
	 @spid		int
	,@level		int
	,@num		int	OUT
)
as
begin
	set nocount on

	declare @spid_c int
	declare @preStr varchar(100)
	declare @printStr varchar(255)
	declare @fullStr varchar(255)

	select @level = @level + 1
	select @preStr = replicate('> ', @level)	

	if ( @level >=15 )
	begin
		print "%1!STOP HERE, neasting level is reached.", @preStr
		return(1)
	end

	--select @num = count(*) from #block_work where blocked = @spid

	/*
	** Declare the Cursors select statement
	*/
	DECLARE x_c cursor
		for
			select spid
			from #block_work
			where blocked = @spid
		for read only

	/*
	** Open and fetch the first two rows
	*/
	OPEN x_c
	FETCH x_c INTO @spid_c

	/*
	** Loop all rows in the cursor.
	*/
	WHILE (@@sqlstatus = 0)
	BEGIN

		print "%1!%2! blocks spid %3!", @preStr, @spid, @spid_c

--		select @printStr = @preStr + "blocks " + convert(char(4), @spid_c)
--		select @fullStr = convert(char(4), @spid) + @printStr
--		print @fullStr

		exec sp_whoisb_sub_blocks 
			@spid = @spid_c, 
			@level = @level,
			@num = @num OUTput

		select @num = @num + 1

		FETCH x_c INTO @spid_c
	END
	CLOSE x_c
	DEALLOCATE CURSOR x_c


	/*
	** Normal exit point
	*/
/*-->*/	return(0)
end
go
exec sp_procxmode 'sp_whoisb_sub_blocks', 'anymode'
go
grant exec on sp_whoisb_sub_blocks to public
go

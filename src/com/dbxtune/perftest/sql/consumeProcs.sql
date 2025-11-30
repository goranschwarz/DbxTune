--use perfdemo
go


set nocount on
go


/*
** If not in correct database, terminate
*/
declare @dbname varchar(30)
select @dbname = db_name()
if (@dbname = "master")
begin
	print "ERROR: Sorry you are in the wrong database, you should not install into the 'master' database."
	select syb_quit()
end
go

declare @dbname varchar(30)
select @dbname = db_name()
print 'Installing into dbname ''%1!'', at server ''%2!''', @dbname, @@servername
go



-----------------------------------------------------------
if ((select object_id('consumeDest1')) is not null)
begin
	print "  drop proc: consumeDest1"
	drop proc consumeDest1
end
go
print "create proc: consumeDest1"
go
/*=====================================================================**
** PROCEDURE: consumeDest1
**---------------------------------------------------------------------**
** Description:
**
** fixme
**
**---------------------------------------------------------------------**
** History:
**
** 2010-12-01	1.0.0	Goran Schwarz, Sybase Sweden
**		Created
**---------------------------------------------------------------------*/
create procedure consumeDest1
(
	@pk      varchar(30),
	@count   int
)
as
begin
	declare @rowc int
	--waitfor delay "00:00:00.100"

	--------------------------------------
	-- make a dummy select on a "large" table with no index on it
	-- this to emulate some "in memory" table scans
	--------------------------------------
	select @rowc = count(*) 
	from DummyLookupTab1 
	where intCol1 = (@count % 100) 

	--------------------------------------
	-- INSERT into destination table
	--------------------------------------
	insert into VDestTab1(pk, intCol1, vcCol1, visible)
	select @pk, @count, @pk, (@count % 2)


	--print "exiting the proc 'consumeDest1'."
	
	return 0
end
go



-----------------------------------------------------------
if ((select object_id('consumeDest2')) is not null)
begin
	print "  drop proc: consumeDest2"
	drop proc consumeDest2
end
go
print "create proc: consumeDest2"
go
/*=====================================================================**
** PROCEDURE: consumeDest2
**---------------------------------------------------------------------**
** Description:
**
** fixme
**
**---------------------------------------------------------------------**
** History:
**
** 2010-12-01	1.0.0	Goran Schwarz, Sybase Sweden
**		Created
**---------------------------------------------------------------------*/
create procedure consumeDest2
(
	@pk      varchar(30),
	@count   int
)
as
begin
	declare @rowc int
	--waitfor delay "00:00:00.100"

	--------------------------------------
	-- make a dummy select on a "large" table with no index on it
	-- this to emulate some "in memory" table scans
	--------------------------------------
	select @rowc = count(*) 
	from DummyLookupTab2 
	where intCol1 = (@count % 100) --- 'intCol1' contains approx 110000 rows, with values between 0-99, so we would hit alot of records here even if we had a index

	--------------------------------------
	-- INSERT into destination table
	--------------------------------------
	insert into VDestTab2(pk, intCol1, vcCol1, visible)
	select @pk, @count, @pk, (@count % 2)


	--print "exiting the proc 'consumeDest2'."
	
	return 0
end
go



-----------------------------------------------------------
if ((select object_id('consumeDest3')) is not null)
begin
	print "  drop proc: consumeDest3"
	drop proc consumeDest3
end
go
print "create proc: consumeDest3"
go
/*=====================================================================**
** PROCEDURE: consumeDest3
**---------------------------------------------------------------------**
** Description:
**
** fixme
**
**---------------------------------------------------------------------**
** History:
**
** 2010-12-01	1.0.0	Goran Schwarz, Sybase Sweden
**		Created
**---------------------------------------------------------------------*/
create procedure consumeDest3
(
	@pk      varchar(30),
	@count   int
)
as
begin
	declare @rowc int
	--waitfor delay "00:00:00.100"

	--------------------------------------
	-- INSERT into destination table
	--------------------------------------
	insert into VDestTab3(pk, intCol1, vcCol1, visible)
	select @pk, @count, @pk, (@count % 2)


	--print "exiting the proc 'consumeDest3'."
	
	return 0
end
go



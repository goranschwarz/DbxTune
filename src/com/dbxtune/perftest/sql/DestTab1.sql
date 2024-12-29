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

--------------------------------------------------------------------------------
--- INDEX: In this file you have
--------------------------------------------------------------------------------
-- * DestTab1        - The DESTINATION table
-- * VDestTab1       - A view on to of the destination table... (which would be the "public access point" for some applications)
-- * DestTab1Hist    - A History table (keeps: ins, del, upd-before, upd-after immage values, so we can track changes and who made them...
-- * DestTab1Hist_tr - A Trigger on the history, just there so we can "slow things down", and also see Procedure Call Stack
-- * DestTab1_trH_p  - Procedure used in destination table trigger to feed the history tables
-- * DestTab1_tr     - Trigger on the destination table, with a cursor that loops the inserted/deleted table
--------------------------------------------------------------------------------






/* ----------------------------------------------------------------
** TABLE: DestTab1
*/
if ((select object_id('DestTab1')) is not null)
begin
	print "  drop table: DestTab1"
	drop table DestTab1
end
go
print "create table: DestTab1"
go
create table DestTab1
(
	pk		varchar(36)			not null,
	intCol1		int				not null,
	vcCol1		varchar(30)			not null,
	visible		int		default 1	not null,

	primary key(pk)
)
go


/* ----------------------------------------------------------------
** VIEW: VDestTab1
*/
if ((select object_id('VDestTab1')) is not null)
begin
	print "  drop view: VDestTab1"
	drop view VDestTab1
end
go
print "create view: VDestTab1"
go
create view VDestTab1
as
	select * 
	from DestTab1
	where visible = 1
go


/* ----------------------------------------------------------------
** TABLE: DestTab1Hist
*/
if ((select object_id('DestTab1Hist')) is not null)
begin
	print "  drop table: DestTab1Hist"
	drop table DestTab1Hist
end
go
print "create table: DestTab1Hist"
go
create table DestTab1Hist
(
	actionOp	char(10)			not null,
	actionDate	datetime			not null,
	actionUser	varchar(30)			not null,

	pk		varchar(36)			not null,
	intCol1		int				not null,
	vcCol1		varchar(30)			not null,
	visible		int		default 1	not null,

	primary key(pk, actionOp, actionDate)
)
go

/* ----------------------------------------------------------------
** TRIGGER: DestTab1Hist_tr
*/
print "create trigger: DestTab1Hist_tr"
go
create trigger DestTab1Hist_tr
on DestTab1Hist
for insert, update, delete
as
begin
	waitfor delay '00:00:00.500'
end
go






/* ----------------------------------------------------------------
** PROC: DestTab1_trH_p
*/
if ((select object_id('DestTab1_trH_p')) is not null)
begin
	print "  drop table: DestTab1_trH_p"
	drop procedure DestTab1_trH_p
end
go
print "create procedure: DestTab1_trH_p"
go
create procedure DestTab1_trH_p
(
	@operation      varchar(10),
	@execTime       datetime,
	@execUser       varchar(30),

 	@pk		varchar(36),
	@intCol1	int,
	@vcCol1	varchar(30),
	@visible	int
)
as
begin
	insert into DestTab1Hist
	(
		actionOp,
		actionDate,
		actionUser,

		pk,
		intCol1,
		vcCol1,
		visible
	)
	values
	(
		@operation,
		@execTime,
		@execUser,

		@pk,
		@intCol1,
		@vcCol1,
		@visible
	)
end
go








--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
--- TRIGGER: DestTab1
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------

print "create trigger: DestTab1_tr"
go

-- *****************************************************
create trigger DestTab1_tr
on DestTab1 for insert, update, delete 
as 
 
-- *****************************************************
-- Declarative Part
-- *****************************************************
declare
	@del_rows 	int,
	@ins_rows 	int,
 	@ins_pk		varchar(36),
	@ins_intCol1	int,
	@ins_vcCol1	varchar(30),
	@ins_visible	int,
 	@del_pk		varchar(36),
	@del_intCol1	int,
	@del_vcCol1	varchar(30),
	@del_visible	int
declare
	@execTime       datetime,
	@execUser       varchar(30)

select	@execTime = getdate(),
	@execUser = suser_name()
 
-- *****************************************************
-- if no records are changed
-- *****************************************************
if @@rowcount = 0
   return
 
-- *****************************************************
-- Calculate number of inserted and deleted rows
-- *****************************************************
select @ins_rows = count(*) from inserted
select @del_rows = count(*) from deleted
 
-- *****************************************************
-- INSERT-PART
-- *****************************************************
if @del_rows < @ins_rows
begin
	declare inserted_cursor cursor for 
	select 
	   pk,
	   intCol1,
	   vcCol1,
	   visible
	from inserted

	open inserted_cursor

	fetch inserted_cursor into 
	   @ins_pk,
	   @ins_intCol1,
	   @ins_vcCol1,
	   @ins_visible

	while @@sqlstatus <> 2
	begin

		waitfor delay '00:00:00' 
		
		exec DestTab1_trH_p
		   "INSERT",
		   @execTime,
		   @execUser,
		   @ins_pk,
		   @ins_intCol1,
		   @ins_vcCol1,
		   @ins_visible

		fetch inserted_cursor into 
		   @ins_pk,
		   @ins_intCol1,
		   @ins_vcCol1,
		   @ins_visible

	end

	close inserted_cursor
	deallocate cursor inserted_cursor

	return
end
 
-- *****************************************************
-- UPDATE-PART
-- *****************************************************
if @del_rows = @ins_rows
begin

	declare inserted_cursor cursor for 
	select 
	   pk,
	   intCol1,
	   vcCol1,
	   visible
	from inserted

	declare deleted_cursor cursor for 
	select 
	   pk,
	   intCol1,
	   vcCol1,
	   visible
	from deleted
 
	open inserted_cursor

	open deleted_cursor
 
	fetch inserted_cursor into 
	   @ins_pk,
	   @ins_intCol1,
	   @ins_vcCol1,
	   @ins_visible

	fetch deleted_cursor into 
	   @ins_pk,
	   @ins_intCol1,
	   @ins_vcCol1,
	   @ins_visible
 
	while @@sqlstatus <> 2
	begin

		waitfor delay '00:00:00' 

		exec DestTab1_trH_p
		   "UPD-AFTER",
		   @execTime,
		   @execUser,
		   @ins_pk,
		   @ins_intCol1,
		   @ins_vcCol1,
		   @ins_visible

		exec DestTab1_trH_p
		   "UPD-BEFORE",
		   @execTime,
		   @execUser,
		   @del_pk,
		   @del_intCol1,
		   @del_vcCol1,
		   @del_visible


		fetch inserted_cursor into 
		   @ins_pk,
		   @ins_intCol1,
		   @ins_vcCol1,
		   @ins_visible

		fetch deleted_cursor into 
		   @del_pk,
		   @del_intCol1,
		   @del_vcCol1,
		   @del_visible

	end

	close inserted_cursor
	deallocate cursor inserted_cursor

	close deleted_cursor
	deallocate cursor deleted_cursor

	return
end
 
-- *****************************************************
-- DELETE-PART
-- *****************************************************
if @del_rows > @ins_rows
begin
 
	declare deleted_cursor cursor for 
	select 
	   pk,
	   intCol1,
	   vcCol1,
	   visible
	from deleted
 
	open deleted_cursor
 
	fetch deleted_cursor into 
	   @del_pk,
	   @del_intCol1,
	   @del_vcCol1,
	   @del_visible
 
	while @@sqlstatus <> 2
	begin

		waitfor delay '00:00:00' 

		exec DestTab1_trH_p
		   "DELETE",
		   @execTime,
		   @execUser,
		   @del_pk,
		   @del_intCol1,
		   @del_vcCol1,
		   @del_visible

		fetch deleted_cursor into 
		   @del_pk,
		   @del_intCol1,
		   @del_vcCol1,
		   @del_visible
	end
 
	close deleted_cursor
	deallocate cursor deleted_cursor
 
	return
end
-- *****************************************************
go

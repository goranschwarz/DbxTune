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





/* ----------------------------------------------------------------
** TABLE: 
*/
if ((select object_id('DestTab3')) is not null)
begin
	print "  drop table: DestTab3"
	drop table DestTab3
end
go
print "create table: DestTab3"
go
create table DestTab3
(
	pk		varchar(36)			not null,
	intCol1		int				not null,
	vcCol1		varchar(30)			not null,
	visible		int		default 1	not null,

	primary key(pk)
)
go

/* ----------------------------------------------------------------
** VIEW: 
*/
if ((select object_id('VDestTab3')) is not null)
begin
	print "  drop view: VDestTab3"
	drop view VDestTab3
end
go
print "create view: VDestTab3"
go
create view VDestTab3
as
	select * 
	from DestTab3
	where visible = 1
go

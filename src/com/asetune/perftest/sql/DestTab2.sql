use perfdemo
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





/* ----------------------------------------------------------------
** TABLE: 
*/
if ((select object_id('DestTab2')) is not null)
begin
	print "  drop table: DestTab2"
	drop table DestTab2
end
go
print "create table: DestTab2"
go
create table DestTab2
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
if ((select object_id('VDestTab2')) is not null)
begin
	print "  drop view: VDestTab2"
	drop view VDestTab2
end
go
print "create view: VDestTab2"
go
create view VDestTab2
as
	select * 
	from DestTab2
	where visible = 1
go


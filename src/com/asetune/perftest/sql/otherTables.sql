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





----------------------------------------------------------
----------------------------------------------------------
----------------------------------------------------------
----------------------------------------------------------
-- Do some "dummy" lookup tables
-- NO INDEX on it
----------------------------------------------------------
----------------------------------------------------------
----------------------------------------------------------
----------------------------------------------------------
/*
** TABLE: 
*/
if ((select object_id('DummyLookupTab1')) is not null)
begin
	print "  drop table: DummyLookupTab1"
	drop table DummyLookupTab1
end
go
print "create table: DummyLookupTab1"
go
create table DummyLookupTab1
(
	intCol1		int				not null,
	charCol1	char(255)			not null,
	charCol2	char(255)			not null,
	charCol3	char(255)			not null,
	charCol4	char(255)			not null,
	charCol5	char(255)			not null,
	charCol6	char(255)			not null,
	charCol7	char(255)			not null,
)
go
-- in "demo 3": Find long running SQL Statement
-- It might be stupid to have "another" table scan problem...
-- If you want to have the problem, COMMENT OUT the index creation below
create nonclustered index DummyLookupTab1_ix1 on DummyLookupTab1(intCol1)
go

print "Inserting data into 'DummyLookupTab1': approx 18000 rows, which would be 36 MB data"
print "The column 'intCol1', containes values between 0-99999, so index on that col would be almost unique..."
go
----------------------
-- Insert XXX rows in the table
----------------------
insert into DummyLookupTab1
select 
	convert(int,rand2()*100000), 
	newid(1) + ' : and a new newid(1) : ' + newid(1) + ' : and a new newid(1) : ' + newid(1) + ' : and a new newid(1) : ' + newid(1) + ' :end-of-row:',
	newid(1) + ' : and a new newid(1) : ' + newid(1) + ' : and a new newid(1) : ' + newid(1) + ' : and a new newid(1) : ' + newid(1) + ' :end-of-row:',
	newid(1) + ' : and a new newid(1) : ' + newid(1) + ' : and a new newid(1) : ' + newid(1) + ' : and a new newid(1) : ' + newid(1) + ' :end-of-row:',
	newid(1) + ' : and a new newid(1) : ' + newid(1) + ' : and a new newid(1) : ' + newid(1) + ' : and a new newid(1) : ' + newid(1) + ' :end-of-row:',
	newid(1) + ' : and a new newid(1) : ' + newid(1) + ' : and a new newid(1) : ' + newid(1) + ' : and a new newid(1) : ' + newid(1) + ' :end-of-row:',
	newid(1) + ' : and a new newid(1) : ' + newid(1) + ' : and a new newid(1) : ' + newid(1) + ' : and a new newid(1) : ' + newid(1) + ' :end-of-row:',
	newid(1) + ' : and a new newid(1) : ' + newid(1) + ' : and a new newid(1) : ' + newid(1) + ' : and a new newid(1) : ' + newid(1) + ' :end-of-row:'
from sybsystemprocs..syscomments
go
sp_spaceused DummyLookupTab1
go




/*
** TABLE: 
*/
if ((select object_id('DummyLookupTab2')) is not null)
begin
	print "  drop table: DummyLookupTab2"
	drop table DummyLookupTab2
end
go
print "create table: DummyLookupTab2"
go
create table DummyLookupTab2
(
	intCol1		int				not null,
	charCol1	char(36)			not null,
	charCol2	char(255)			not null,
	charCol3	char(255)			not null
)
go

print "Inserting data into 'DummyLookupTab2': approx 72000 rows, which would be 42 MB data"
print "The column 'intCol1', containes values between 0-99, so index on that col would have many duplicates..."
go
----------------------
-- Insert XXX rows in the table
----------------------
insert into DummyLookupTab2
select 
	convert(int,rand2()*100), -- This will generate values between 0 and 99
	newid(1),
	newid(1) + ' : and a new newid(1) : ' + newid(1) + ' : and a new newid(1) : ' + newid(1) + ' : and a new newid(1) : ' + newid(1) + ' :end-of-row:',
	newid(1) + ' : and a new newid(1) : ' + newid(1) + ' : and a new newid(1) : ' + newid(1) + ' : and a new newid(1) : ' + newid(1) + ' :end-of-row:'
from sybsystemprocs..syscomments
go 4
sp_spaceused DummyLookupTab2
go









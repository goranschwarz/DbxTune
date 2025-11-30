use master
go

drop database perfdemo
go

create database perfdemo
    on data1 = 700
log on log1  = 300
go

sp_dboption perfdemo, "select into", true
go

--sp_dboption perfdemo, "trunc log on chkpt", true
go

sp_helpdb perfdemo
go


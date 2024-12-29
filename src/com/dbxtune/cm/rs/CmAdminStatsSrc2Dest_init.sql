---------------------------------------------
-- Get information about databases etc...
-- Used by: CmAdminStatsSrcToDest
--
-- Stuff values in the below temptable, and get it at the very end
---------------------------------------------

create table #rsTuneInfo
(
	type              varchar(10)   not null, 
	local_rsid        int           not null, 
	local_rsname      varchar(30)   not null, 
	src_server        varchar(30)   not null, 
	src_database      varchar(30)   not null, 
	src_dbid          int           not null, 
	src_connection    varchar(80)   not null, 
	src_rsid          int           not null, 
	src_rsname        varchar(30)   not null, 
	is_origin         bit           not null, 
	dest_server       varchar(30)   not null, 
	dest_database     varchar(30)   not null, 
	dest_dbid         int           not null, 
	dest_connection   varchar(80)   not null, 
	dest_rsid         int           not null, 
	dest_rsname       varchar(30)   not null, 
	-- only for TABLE
	num_repdefs       int               null, 
	num_tables        int               null, 
	num_table_subscr  int               null,
	-- only for TABLE
	dbrepid           int               null, 
	db_repdef_name    varchar(30)       null, 
	db_subscr_name    varchar(30)       null, 
	num_subsets       int               null,
	-- only for WS
	logical_conn      varchar(80)       null, 
	ldbid             int               null
)

---------------------------------------------
-- The below code is mostly borrowed from Jeff Tallman RS MC Package
-- proc: rs_perf_datadistro_summary
------------
declare @rs_id int
declare @rs_name varchar(30)
select @rs_name=c.charvalue from dbo.rs_config c where c.optionname = 'oserver'
select @rs_id=id from dbo.rs_sites where name = @rs_name

	declare	@cur_rsname		varchar(30),
			@src_rsname		varchar(30),
			@src_connection	varchar(61),
			@dest_connection	varchar(61),
			@logical_connection	varchar(61),
			@dest_rsname		varchar(30), 
			@num_tables		int, 
			@num_repdefs		int, 
			@num_table_subscr	int,
			@print_line		varchar(255)

	select dsname, dbname, dbid, connid, 
		connection_name=(case
						when ltype='L' and ptype='L' then dsname+'.'+dbname+' ('+convert(varchar(7),connid)+')'+' (WS)'
						else dsname+'.'+dbname+' ('+convert(varchar(7),connid)+')'
					end)
	  into #thisRS_dbs
	  from rs_databases
	  where prsid=@rs_id
	    and (dbname not like '%RSSD' or rowtype!=0)

	select dsname, dbname, dbid, connid, prsid, 
			connection_name=(case
						when ltype='L' and ptype='L' then dsname+'.'+dbname+' ('+convert(varchar(7),connid)+')'+' (WS)'
						else dsname+'.'+dbname+' ('+convert(varchar(7),connid)+')'
					end)
	  into #all_dbs
	  from rs_databases
	  where (dbname not like '%RSSD' or rowtype!=0)
	union
	select dsname, dbname, dbid, dbid as connid, controllerid as prsid,
			connection_name=dsname+'.'+dbname+' ('+convert(varchar(7),dbid)+')'
	  from rs_repdbs
	  where dbid not in (select dbid from rs_databases)
--select 'THIS_RS_DBS', * from #thisRS_dbs
--select 'ALL_DBS', * from #all_dbs

	select @cur_rsname=name from rs_sites where id=@rs_id
	
	-- we need a list of all publications (repdefs & dbrepdefs) from databases in the above list
	-- plus a list of all subscriptions to databases in the above list...
	--
	-- we start with repdefs from a site - since MPR may have multiple repdefs, let's first get the ones for 
	-- the default connection
	select d1.dsname as src_server, d1.dbname as src_database, d1.dbid as src_dbid, d1.connection_name as src_connection,
		@rs_id as src_rsid, @cur_rsname as src_rsname, is_origin=convert(bit,1), 
		o.objid, o.objname as repdef_name, o.phys_tablename, o.phys_objowner,
		o.repl_objowner, o.deliver_as_name, s.subid, s.subname, d2.dsname as dest_server, d2.dbname as dest_database,
		d2.connid as dest_dbid, d2.connection_name as dest_connection,
		r.id as dest_rsid, r.name as dest_rsname
	  into #pubs_subs
	  from #thisRS_dbs d1, rs_objects o, rs_subscriptions s, #all_dbs d2, rs_sites r
	  where d1.dbid=o.dbid
	    and o.objid=s.objid
	    and s.dbid=d2.connid
	    and d1.dbid=d1.connid
	    and d2.prsid=r.id
	-- union in the ones on specific connections
	-- union all  --> ASE 15.7 seems to not like this - perhaps due to join complexity...so we will do insert/select
	insert into #pubs_subs (src_server, src_database, src_dbid, src_connection,
				src_rsid, src_rsname, is_origin,
				objid, repdef_name, phys_tablename, phys_objowner,
				repl_objowner, deliver_as_name, subid, subname, dest_server, dest_database,
				dest_dbid, dest_connection,
				dest_rsid, dest_rsname)
		select d1.dsname as src_server, d1.dbname as src_database, d1.connid as src_dbid, d1.connection_name as src_connection,
			@rs_id as src_rsid, @cur_rsname as src_rsname, is_origin=convert(bit,1), 
			o.objid, o.objname as repdef_name, o.phys_tablename, o.phys_objowner,
			o.repl_objowner, o.deliver_as_name, s.subid, s.subname, d2.dsname as dest_server, d2.dbname as dest_database,
			d2.connid as dest_dbid, d2.connection_name as dest_connection,
			r.id as dest_rsid, r.name as dest_rsname
		  from #thisRS_dbs d1, rs_objects o, rs_subscriptions s, #all_dbs d2, rs_sites r
		  where d1.connid=o.dbid
		    and o.objid=s.objid
		    and s.dbid=d2.connid
		    and d1.dbid!=d1.connid
		    and d2.prsid=r.id
	-- add in all the current replicate database subscriptions where parent connection is repdef
	--union all
	insert into #pubs_subs (src_server, src_database, src_dbid, src_connection,
				src_rsid, src_rsname, is_origin,
				objid, repdef_name, phys_tablename, phys_objowner,
				repl_objowner, deliver_as_name, subid, subname, dest_server, dest_database,
				dest_dbid, dest_connection,
				dest_rsid, dest_rsname)
		select d1.dsname as src_server, d1.dbname as src_database, d1.dbid as src_dbid, 
			d1.connection_name as src_connection,
			r.id as src_rsid, r.name as src_rsname, is_origin=convert(bit,0), 
			o.objid, o.objname as repdef_name, o.phys_tablename, o.phys_objowner,
			o.repl_objowner, o.deliver_as_name, s.subid, s.subname, d2.dsname as dest_server, d2.dbname as dest_database,
			d2.connid as dest_dbid, d2.connection_name as dest_connection,
			@rs_id as dest_rsid, @cur_rsname as dest_rsname
		  from #thisRS_dbs d2, rs_objects o, rs_subscriptions s, #all_dbs d1, rs_sites r
		  where d1.dbid=o.dbid
		    and o.objid=s.objid
		    and s.dbid=d2.connid
		    and d1.dbid=d1.connid
		    and d1.prsid=r.id
	-- add in all the current replicate database subscriptions where MPR connection is repdef
	-- union all
	insert into #pubs_subs (src_server, src_database, src_dbid, src_connection,
				src_rsid, src_rsname, is_origin,
				objid, repdef_name, phys_tablename, phys_objowner,
				repl_objowner, deliver_as_name, subid, subname, dest_server, dest_database,
				dest_dbid, dest_connection,
				dest_rsid, dest_rsname)
		select d1.dsname as src_server, d1.dbname as src_database, d1.connid as src_dbid, 
			d1.connection_name as src_connection,
			r.id as src_rsid, r.name as src_rsname, is_origin=convert(bit,0), 
			o.objid, o.objname as repdef_name, o.phys_tablename, o.phys_objowner,
			o.repl_objowner, o.deliver_as_name, s.subid, s.subname, d2.dsname as dest_server, d2.dbname as dest_database,
			d2.connid as dest_dbid, d2.connection_name as dest_connection,
			@rs_id as dest_rsid, @cur_rsname as dest_rsname
		  from #thisRS_dbs d2, rs_objects o, rs_subscriptions s, #all_dbs d1, rs_sites r
		  where d1.dbid=o.dbid
		    and o.objid=s.objid
		    and s.dbid=d2.connid
		    and d1.dbid!=d1.connid
		    and d1.prsid=r.id
	    
	--select * from #pubs_subs where is_origin=1
	--select * from #pubs_subs where is_origin=0
		
	select src_server, src_database, src_dbid, src_connection, src_rsid, src_rsname, is_origin,
		dest_server, dest_database, dest_dbid, dest_connection, dest_rsid, dest_rsname,
		num_repdefs=count(distinct repdef_name),
		num_tables=count(distinct phys_objowner+'.'+phys_tablename),
		num_table_subscr=count(*)
	  into #num_table_subscr
	  from #pubs_subs
	  group by src_server, src_database, src_dbid, src_connection, src_rsid, src_rsname, is_origin,
		dest_server, dest_database, dest_dbid, dest_connection, dest_rsid, dest_rsname

--select 'pubs_subs', * from #pubs_subs
--select 'num_table_subscr', * from #num_table_subscr
	drop table #pubs_subs

--select 'TABLE' as type, src_server, src_database, src_dbid, src_connection, src_rsid, src_rsname, is_origin, dest_server, dest_database, dest_dbid, dest_connection, dest_rsid, dest_rsname, num_repdefs, num_tables, num_table_subscr  
--from #num_table_subscr
insert into #rsTuneInfo
select 'TABLE' as type, @rs_id, @rs_name,
	src_server,  src_database,  src_dbid,  src_connection,  src_rsid,  src_rsname, is_origin, 
	dest_server, dest_database, dest_dbid, dest_connection, dest_rsid, dest_rsname, 
	num_repdefs, num_tables, num_table_subscr, -- TABLE specific columns
	null, null, null, null,                    -- null values for MSA specific columns
	null, null                                 -- null values for WS specific columns
from #num_table_subscr


--	print ' '
--	print ' '
--	print ' '
--	print '**************************************************************'
--	print '*                                                            *'
--	print '* Data Distribution & Routing Report Sections                *'
--	print '*                                                            *'
--	print '**************************************************************'
--	print ' '
--	
--	print ' '
--	print ' '
--	print 'Databases in this Replication Server publish data to the following connections using standard table repdefs and table subscriptions:'
--	print ' '
--	print 'source database                                      destination database                                 RRS Name                         #Tables   #Repdef   #Subscr'
--	print '--------------------------------------------------   --------------------------------------------------   ------------------------------   -------   -------   -------'
--	print ' '
--	
--	declare src_table_cursor cursor for
--		select src_connection, dest_connection, dest_rsname, num_tables, num_repdefs, num_table_subscr
--		  from #num_table_subscr
--		  where is_origin=1
--		  order by src_connection, dest_connection
--
--	open src_table_cursor
--	fetch src_table_cursor into @src_connection, @dest_connection, @dest_rsname, @num_tables, @num_repdefs, @num_table_subscr
--
--	if @@sqlstatus!=0 print '   (no table repdef/subscriptions from this RS) '
--
--	while @@sqlstatus=0
--	begin
--		if @dest_rsname=@cur_rsname
--			select @print_line=@src_connection
--					+space(50-char_length(@src_connection))
--					+space(3)+@dest_connection
--					+space(50-char_length(@dest_connection))
--					+space(3)+' (local) '
--					+space(30-char_length(' (local) '))
--					+space(3)+str(@num_tables,7,0)
--					+space(3)+str(@num_repdefs,7,0)
--					+space(3)+str(@num_table_subscr,7,0)
--		else
--			select @print_line=@src_connection
--					+space(50-char_length(@src_connection))
--					+space(3)+@dest_connection
--					+space(50-char_length(@dest_connection))
--					+space(3)+@dest_rsname
--					+space(30-char_length(@dest_rsname))
--					+space(3)+str(@num_tables,7,0)
--					+space(3)+str(@num_repdefs,7,0)
--					+space(3)+str(@num_table_subscr,7,0)
--		
--		print '%1!', @print_line
--			
--		fetch src_table_cursor into @src_connection, @dest_connection, @dest_rsname, @num_tables, @num_repdefs, @num_table_subscr
--
--	end
--
--	close src_table_cursor
--	deallocate cursor src_table_cursor
--
--
--	print ' '
--	print ' '
--	print 'Databases in this Replication Server subscribe to data from the following connections using standard table repdefs and table subscriptions:'
--	print ' '
--	print 'destination database                                 source database                                      PRS Name                         #Tables   #Repdef   #Subscr'
--	print '--------------------------------------------------   --------------------------------------------------   ------------------------------   -------   -------   -------'
--	print ' '
--	
--	declare dest_table_cursor cursor for
--		select src_connection, src_rsname, dest_connection, num_tables, num_repdefs, num_table_subscr
--		  from #num_table_subscr
--		  where is_origin=0
--		  order by src_connection, dest_connection
--
--	open dest_table_cursor
--	fetch dest_table_cursor into @src_connection, @src_rsname, @dest_connection, @num_tables, @num_repdefs, @num_table_subscr
--
--	if @@sqlstatus!=0 print '   (no table repdef/subscriptions to this RS) '
--
--	while @@sqlstatus=0
--	begin
--		if @src_rsname=@cur_rsname
--			select @print_line=@dest_connection
--					+space(50-char_length(@dest_connection))
--					+space(3)+@src_connection
--					+space(50-char_length(@src_connection))
--					+space(3)+' (local) '
--					+space(30-char_length(' (local) '))
--					+space(3)+str(@num_tables,7,0)
--					+space(3)+str(@num_repdefs,7,0)
--					+space(3)+str(@num_table_subscr,7,0)
--		else
--			select @print_line=@dest_connection
--					+space(50-char_length(@dest_connection))
--					+space(3)+@src_connection
--					+space(50-char_length(@src_connection))
--					+space(3)+@src_rsname
--					+space(30-char_length(@src_rsname))
--					+space(3)+str(@num_tables,7,0)
--					+space(3)+str(@num_repdefs,7,0)
--					+space(3)+str(@num_table_subscr,7,0)
--		
--		print '%1!', @print_line
--			
--		fetch dest_table_cursor into @src_connection, @src_rsname, @dest_connection, @num_tables, @num_repdefs, @num_table_subscr
--
--	end
--
--	close dest_table_cursor
--	deallocate cursor dest_table_cursor
--	
--	drop table #num_table_subscr
--	
--	print ' '
--	print ' '	





	/*
	**
	** Now we need to look for database repdefs and database subscriptions involving databases in this RS
	**
	*/
	
	
	select d1.dsname as src_server, d1.dbname as src_database, d1.dbid as src_dbid, 
			d1.connection_name as src_connection,
		@rs_id as src_rsid, @cur_rsname as src_rsname, is_origin=convert(bit,1), 
		r.dbrepid, r.dbrepname as db_repdef_name, s.subname as db_subscr_name,  
		num_subsets=convert(int,0),
		d2.dsname as dest_server, d2.dbname as dest_database, d2.connid as dest_dbid, 
		d2.dsname+'.'+d2.dbname+' ('+convert(varchar(7),d2.connid)+')' as dest_connection,
		d2.prsid as dest_rsid, t.name as dest_rsname
	into #db_repdefs
	from #thisRS_dbs d1, rs_dbreps r, rs_subscriptions s, #all_dbs d2, rs_sites t
	where d1.dbid=r.dbid
	  and s.objid=r.dbrepid
	  and s.dbid=d2.connid
	  and d2.prsid=t.id
	union all
	select d1.dsname as src_server, d1.dbname as src_database, d1.dbid as src_dbid, 
		d1.dsname+'.'+d1.dbname+' ('+convert(varchar(7),d1.dbid)+')' as src_connection,
		d1.prsid as src_rsid, t.name as src_rsname, is_origin=convert(bit,0), 
		r.dbrepid, r.dbrepname as db_repdef_name, s.subname as db_subscr_name,  
		num_subsets=convert(int,0),
		d2.dsname as dest_server, d2.dbname as dest_database, d2.connid as dest_dbid,
		d2.connection_name as dest_connection, 
		@rs_id as dest_rsid, @cur_rsname as dest_rsname
	from #thisRS_dbs d2, rs_dbreps r, rs_subscriptions s, #all_dbs d1, rs_sites t
	where d1.dbid=r.dbid
	  and s.objid=r.dbrepid
	  and s.dbid=d2.connid
	  and d1.prsid!=@rs_id
	  and d1.prsid=t.id
	  
	select dbrepid, count(*) as num_subsets
	   into #db_subsets
	   from rs_dbsubsets
	   group by dbrepid
	
	update #db_repdefs
		set num_subsets=s.num_subsets
		from #db_repdefs r, #db_subsets s
		where r.dbrepid=s.dbrepid


--select 'db_repdefs', * from #db_repdefs

--select 'MSA'   as type, src_server, src_database, src_dbid, src_connection, src_rsid, src_rsname, is_origin, dest_server, dest_database, dest_dbid, dest_connection, dest_rsid, dest_rsname, dbrepid, db_repdef_name, db_subscr_name, num_subsets
--from #db_repdefs
insert into #rsTuneInfo
select 'MSA'   as type,  @rs_id, @rs_name,
	src_server,  src_database,  src_dbid,  src_connection,  src_rsid,  src_rsname, is_origin, 
	dest_server, dest_database, dest_dbid, dest_connection, dest_rsid, dest_rsname, 
	null, null, null,                                     -- null values for TABLE specific columns
	dbrepid, db_repdef_name, db_subscr_name, num_subsets, -- MSA specific columns
	null, null                                            -- null values for WS specific columns
from #db_repdefs

--	print ' '
--	print ' '
--	print 'Databases in this Replication Server publish data to the following databases using database repdef & database subcriptions, possibly using MSA or database subsets:'
--	print ' '
--	print 'source database                                      destination database                                 RRS Name                         #subsets'
--	print '--------------------------------------------------   --------------------------------------------------   ------------------------------   --------'
--	print ' '
--
--	declare src_dbrep_cursor cursor for
--		select src_connection, dest_connection, dest_rsname, num_subsets
--		  from #db_repdefs
--		  where is_origin=1
--		  order by src_connection, dest_connection
--
--	open src_dbrep_cursor
--	fetch src_dbrep_cursor into @src_connection, @dest_connection, @dest_rsname, @num_tables
--
--	if @@sqlstatus!=0 print '   (no database repdefs/subscriptions from this RS) '
--
--	while @@sqlstatus=0
--	begin
--		if @dest_rsname=@cur_rsname
--			select @print_line=@src_connection
--					+space(50-char_length(@src_connection))
--					+space(3)+@dest_connection
--					+space(50-char_length(@dest_connection))
--					+space(3)+' (local) '
--					+space(30-char_length(' (local) '))
--					+space(3)+str(@num_tables,8,0)
--		else
--			select @print_line=@src_connection
--					+space(50-char_length(@src_connection))
--					+space(3)+@dest_connection
--					+space(50-char_length(@dest_connection))
--					+space(3)+@dest_rsname
--					+space(30-char_length(@dest_rsname))
--					+space(3)+str(@num_tables,8,0)
--		
--		print '%1!', @print_line
--			
--		fetch src_dbrep_cursor into @src_connection, @dest_connection, @dest_rsname, @num_tables
--
--	end
--
--	close src_dbrep_cursor
--	deallocate cursor src_dbrep_cursor
--
--
--
--	print ' '
--	print ' '
--	print 'Databases in this Replication Server subscribe to data from the following databases using database repdef & database subcriptions, possibly using MSA or database subsets:'
--	print ' '
--	print 'source database                                      PRS Name                         destination database                                 #subsets'
--	print '--------------------------------------------------   ------------------------------   --------------------------------------------------   --------'
--	print ' '
--
--	declare dest_dbrep_cursor cursor for
--		select src_connection, src_rsname, dest_connection, num_subsets
--		  from #db_repdefs
--		  where is_origin=0
--		  order by src_connection, dest_connection
--
--	open dest_dbrep_cursor
--	fetch dest_dbrep_cursor into @src_connection, @src_rsname, @dest_connection, @num_tables
--
--	if @@sqlstatus!=0 print '   (no database repdefs/subscriptions to this RS) '
--
--	while @@sqlstatus=0
--	begin
--		if @src_rsname=@cur_rsname
--			select @print_line=@src_connection
--					+space(50-char_length(@src_connection))
--					+space(3)+' (local) '
--					+space(30-char_length(' (local) '))
--					+space(3)+@dest_connection
--					+space(50-char_length(@dest_connection))
--					+space(3)+str(@num_tables,8,0)
--		else
--			select @print_line=@src_connection
--					+space(50-char_length(@src_connection))
--					+space(3)+@src_rsname
--					+space(30-char_length(@src_rsname))
--					+space(3)+@dest_connection
--					+space(50-char_length(@dest_connection))
--					+space(3)+str(@num_tables,8,0)
--		
--		print '%1!', @print_line
--			
--		fetch dest_dbrep_cursor into @src_connection, @src_rsname, @dest_connection, @num_tables
--
--	end
--
--	close dest_dbrep_cursor
--	deallocate cursor dest_dbrep_cursor
--
--
--	print ' '
--	print ' '	

	drop table #db_repdefs
	drop table #thisRS_dbs
	drop table #all_dbs




	/*
	**
	** Now we need to look for legacy style Warm-Standby Connections
	**
	*/


	select logical_conn=convert(varchar(40),l.dsname+'.'+l.dbname+' ('+convert(varchar(7),l.ldbid)+')')+' (WS)',
		active_dbid=a.dbid,
		active_db=convert(varchar(40),(case
					when a.dbname is null then '(n/a)'
					else a.dsname+'.'+a.dbname+' ('+convert(varchar(7),a.dbid)+')'
					end)),
		standby_dbid=s.dbid,
		standby_db=convert(varchar(40),(case
					when s.dbname is null then '(n/a)'
					else s.dsname+'.'+s.dbname+' ('+convert(varchar(7),s.dbid)+')'
					end)),
		l.ldbid,
		src_dbid=a.dbid,
		src_server=a.dsname,
		src_database=a.dbname,
		dest_dbid=s.dbid,
		dest_server=s.dsname,
		dest_database=s.dbname
	  into #legacy_ws
	  from rs_databases l, rs_databases a, rs_databases s
	  where l.ltype='L'
	    and l.ptype='L'
	    and l.prsid=@rs_id
	    and l.ldbid*=a.ldbid
	    and a.ltype='P'
	    and a.ptype='A'
	    and a.prsid=@rs_id
	    and l.ldbid*=s.ldbid
	    and s.ltype='P'
	    and s.ptype='S'
	    and s.prsid=@rs_id

--select 'legacy_ws', * from #legacy_ws
--select 'WS'    as type, 
--	src_server,  src_database,  src_dbid,  active_db as src_connection,   @rs_id as src_rsid,  @rs_name as src_rsname,  convert(bit,1) as is_origin, 
--	dest_server, dest_database, dest_dbid, standby_db as dest_connection, @rs_id as dest_rsid, @rs_name as dest_rsname,
--	logical_conn, ldbid as logical_dbid
--from #legacy_ws
insert into #rsTuneInfo
select 'WS'    as type,   @rs_id, @rs_name,
	src_server,  src_database,  src_dbid,  active_db as src_connection,   @rs_id as src_rsid,  @rs_name as src_rsname,  convert(bit,1) as is_origin, 
	dest_server, dest_database, dest_dbid, standby_db as dest_connection, @rs_id as dest_rsid, @rs_name as dest_rsname,
	null, null, null,      -- null values for TABLE specific columns
	null, null, null, null, -- null values for MSA specific columns
	logical_conn, ldbid     -- WS specific columns
from #legacy_ws

--	print ' '
--	print ' '
--	print 'This Replication Server has the following legacy-style (non-MSA) Warm Standby Logical Connections defined:'
--	print ' '
--	print 'Logical Connection                       Active Database                          Standby Database'
--	print '--------------------------------------   --------------------------------------   --------------------------------------'
--
--	declare logical_cursor cursor for
--		select logical_conn, active_db, standby_db
--		  from #legacy_ws
--		  order by logical_conn
--
--	open logical_cursor
--	fetch logical_cursor into @logical_connection, @src_connection, @dest_connection
--
--	if @@sqlstatus!=0 print '   (no legacy style (non-MSA) Warm-Standby Logical Connections exist) '
--
--	while @@sqlstatus=0
--	begin
--		select @print_line=substring(@logical_connection+space(40),1,40)
--				+space(1)+substring(@src_connection+space(40),1,40)
--				+space(1)+substring(@dest_connection+space(40),1,40)
--		print '%1!', @print_line
--		fetch logical_cursor into @logical_connection, @src_connection, @dest_connection
--
--	end
--
--	close logical_cursor
--	deallocate cursor logical_cursor
--
--	print ' '
--	print ' '


	  
	drop table #legacy_ws	


select * from #rsTuneInfo where is_origin = 1
drop table #rsTuneInfo	
go

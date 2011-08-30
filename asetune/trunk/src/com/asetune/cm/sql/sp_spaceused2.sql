-------------------------------------------------------------
--- NOTE -- NOTE -- NOTE -- NOTE -- NOTE -- NOTE -- NOTE ---- 
-------------------------------------------------------------
--
-- If you change anything in here, do NOT forget to change
-- the field 'SP_XXX_CR_STR' in 'com.asetune.cm.sql.VersionInfo'
--
-- Otherwise it will NOT be recreated... when asetune starts...
--
-------------------------------------------------------------
set nocount on
go

-------------------------------------------------------------
-- Drop in master
-------------------------------------------------------------
use master
go
if ((select object_id('sp_spaceused2')) is not null)
begin
	print "  drop procedure: sp_spaceused2      in: master"
	drop procedure sp_spaceused2
end
go


-------------------------------------------------------------
-- Drop in sybsystemprocs
-------------------------------------------------------------
use sybsystemprocs
go
if ((select object_id('sp_spaceused2')) is not null)
begin
	print "  drop procedure: sp_spaceused2      in: sybsystemprocs"
	drop procedure sp_spaceused2
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
-- Procedure: sp_spaceused2
-------------------------------------------------------------
declare @dbname varchar(255)
select @dbname = db_name()
print "create procedure '%1!.%2!.%3!'.", @dbname, "dbo", "sp_spaceused2"
go

/*=====================================================================**
** PROCEDURE: sp_spaceused2
**---------------------------------------------------------------------**
** Description:
**
**  Aditional information to sp_spaceused.
**  All tables are listed, with spaceusage, and rowcount etc...
**
** User defined @orderby and @where clauses is also implemented...
**
**---------------------------------------------------------------------**
** Input parameters:
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
** 20070531 1.0.0  Goran Schwarz, Sybase Sweden
**                 Created
**---------------------------------------------------------------------*/

/* Sccsid = "%Z% generic/sproc/%M% %I% %G%" */
/*	4.8	1.1	06/14/90	sproc/src/spaceused */
 
/*
** Messages for "sp_spaceused"          17830
**
** 17460, "Object must be in the current database." 
** 17461, "Object does not exist in this database."
** 17830, "Object is stored in 'sysprocedures' and
** 	   has no space allocated directly."
** 17831, "Views don't have space allocated."
** 17832, "Not avail."
*/

/*
** IMPORTANT NOTE:
** This stored procedure uses the built-in function object_id() in the
** where clause of a select query. If you intend to change this query
** or use the object_id() or db_id() builtin in this procedure, please read the
** READ.ME file in the $DBMS/generic/sproc directory to ensure that the rules
** pertaining to object-id's and db-id's outlined there, are followed.
*/
create proc sp_spaceused2
@objname varchar(317) = null,		/* the object we want size on */
@list_indices int = 0			/* don't sum all indices, list each */
	/*** BEGIN: ADDED BY: gorans@sybase.com */
,@orderby varchar(256) = NULL
,@where   varchar(256) = NULL
,@index   int          = NULL
	/*** END: ADDED BY: gorans@sybase.com */
as

declare @type	smallint		/* the object type */
declare @msg	varchar(1024)		/* message output */
declare @dbname varchar(255)             /* database name */
declare @tabname varchar(255)            /* table name */
declare @length	int


if @@trancount = 0
begin
	set chained off
end

set transaction isolation level 1

/*
**  Check to see that the objname is local.
*/
if @objname is not null
begin
	/*
        ** Get the dbname and ensure that the object is in the
        ** current database. Also get the table name - this is later
        ** needed to see if information is being requested for syslogs.
        */
        execute sp_namecrack @objname,
                             @db = @dbname output,
                             @object = @tabname output
        if @dbname is not NULL
	begin
		/*
		** 17460, "Object must be in the current database." 
		*/
		if (@dbname != db_name())
		begin
			raiserror 17460
			return (1)
		end
	end

	/*
	**  Does the object exist?
	*/
	if not exists (select *
                        from sysobjects
                                where id = object_id(@objname))
	begin
		/*
		** 17461, "Object does not exist in this database."
		*/
		raiserror 17461
		return (1)
	end

	/* Get the object type */
        select @type = sysstat & 15 
                from sysobjects
                        where id = object_id(@objname)
	/*
	**  See if it's a space object.
	**  types are:
	**	0 - trigger
	**	1 - system table
	**	2 - view
	**	3 - user table
	**	4 - sproc
	**	6 - default
	**	7 - rule
	*/
	if not exists (select *
			from sysindexes
				where id = object_id(@objname)
					and indid < 2)
	begin
		if @type not in (1,2,3)
		begin
			/*
			** 17830, "Object is stored in 'sysprocedures' and
			** 	   has no space allocated directly."
			*/
			raiserror 17830
			return (1)
		end

		if @type = 2
		begin
			/*
			** 17831, "Views don't have space allocated."
			*/
			raiserror 17831
			return (1)
		end
	end

end

/*
**  If @objname is null, then we want summary data.
*/
set nocount on
if @objname is null
begin
	declare @slog_res_pgs numeric(20, 9),  	/* number of reserved pgs. in syslogs */
		@slog_dpgs numeric(20, 9) 	/* number of data pages in syslogs */
	





	/*** BEGIN: ADDED BY: gorans@sybase.com */

	declare @sql        varchar(4096),
		@maxTabName int,
		@maxIndName int

	if (@index is NOT NULL)
	begin
		select @list_indices = @index
	end

	/*
	** Get a list of all tables and there usage
	*/
	select  name       = o.name,
		iname      = i.name,
		i.id,
		i.indid,
		low        = d.low,
		row_count  = 0,
		indexcount = 0,
		partcount  = 0,
		reserved   = convert(numeric(20, 9), 0),
		data       = convert(numeric(20, 9), 0),
		index_size = convert(numeric(20, 9), 0),
		unused     = convert(numeric(20, 9), 0)
	into #pagecountsGorans
	from sysobjects o, sysindexes i, master.dbo.spt_values d
	where 1=1
	  and o.id = i.id
	  and d.number = 1
	  and d.type = "E"
	  and o.type = 'U'


--12.5
--data_pages(db_id(), id, indid)
--	update #pagecountsGorans
--	   set
--		row_count  = row_count(db_id(), id),
--		reserved   = convert(numeric(20, 9), (reserved_pgs(id, data) + reserved_pgs(id, index_size))),
--		data       = convert(numeric(20, 9), data_pgs(id, data)),
--		index_size = convert(numeric(20, 9), data_pgs(id, index_size)),
--		unused     = convert(numeric(20, 9), ((reserved_pgs(id, data) +	reserved_pgs(id, index_size)) - (data_pgs(id, data) +	data_pgs(id, index_size)))),
--		indexcount = (select count(*) from sysindexes i    where #pagecountsGorans.id = i.id  AND  i.indid > 0  AND i.indid < 255),
--		partcount  = (select count(*) from syspartitions p where #pagecountsGorans.id = p.id  AND  #pagecountsGorans.indid in (0,1) )

	/* 
	** Calculate the reserved pages, data pages, index pages and
	** unused pages. Note that we take care of the special case
	** of indid = 1, 0 in later steps. For indid = 1 case we need
	** to get the data pages and index pages in separate steps.
	*/

	/* Get info for nonclustered indexes */
	update #pagecountsGorans 
	   set
		reserved   = reserved_pages(db_id(), id, indid),
		index_size = convert(numeric(20, 9), data_pages(db_id(), id, indid)),
		unused     = convert(numeric(20, 9), (reserved_pages(db_id(), id, indid) - data_pages(db_id(), id, indid)))
	where indid  > 1

	/* get the data pages for indid = 0 */
	update #pagecountsGorans 
	   set
		row_count  = row_count(db_id(), id),
		indexcount = (select count(*) from sysindexes i    where #pagecountsGorans.id = i.id  AND  i.indid > 0  AND i.indid < 255),
		reserved   = reserved_pages(db_id(), id, indid),
		data       = convert(numeric(20, 9), data_pages(db_id(), id, indid)),
		unused     = convert(numeric(20, 9), (reserved_pages(db_id(), id, indid) - data_pages(db_id(), id, indid)))
	where indid = 0


	/* 
	** For the clustered index case calculate the data and reserved pages
	** by passing in indid of 0 to the builtins. Note, for indid = 1
	** the data pages are accounted for in ipgs. 
	*/
	update #pagecountsGorans 
	   set
		row_count  = row_count(db_id(), id),
		indexcount = (select count(*) from sysindexes i    where #pagecountsGorans.id = i.id  AND  i.indid > 0  AND i.indid < 255),
		index_size = index_size + convert(numeric(20, 9), data_pages(db_id(), id, indid)),
		data       = data       + convert(numeric(20, 9), data_pages(db_id(), id, 0)),
		reserved   = reserved   + reserved_pages(db_id(), id, 0) + reserved_pages(db_id(), id, indid)
	where indid = 1

	/* Calculate the unused count for the special case of indid  = 1 */
	update #pagecountsGorans 
	   set
		unused = convert(numeric(20, 9), (reserved - data - index_size))
	where indid = 1


	/* set partition count */
	update #pagecountsGorans 
	   set
		partcount  = (select count(*) from syspartitions p where #pagecountsGorans.id = p.id  AND  #pagecountsGorans.indid = p.indid )



	/*
	** Build a SQL that will be executed soon
	*/
	select @maxTabName = max(char_length(name))  from #pagecountsGorans
	select @maxIndName = max(char_length(iname)) from #pagecountsGorans

	select @sql = ''
		+ ' select name = convert(varchar('+convert(varchar(10),@maxTabName)+'), name),'
		+ '	indexcount,'
		+ '	partcount,'

	if (@list_indices = 1)
	begin
		select @sql = @sql
		+ '	indid, '
		+ '	type = '
		+ '	  case '
		+ '	    when indid = 0    then "DATA" '
		+ '	    when indid = 1    then "DATA+CL_IX" '
		+ '	    when indid >= 255 then "BLOB_COL" '
		+ '	    else                   "IX" '
		+ '	  end, '
	end

	select @sql = @sql
		+ '	row_count,'
		+ '	data_KB       = convert(int, data       * (low / 1024)),'
		+ '	data_MB       = convert(int, data       * (low / 1024)/1024),'
		+ '	reserved_KB   = convert(int, reserved   * (low / 1024)),'
		+ '	reserved_MB   = convert(int, reserved   * (low / 1024)/1024),'

	if (@list_indices = 1)
	begin
		select @sql = @sql
		+ '	iname = CASE WHEN indid = 0 THEN "--no_clust_index--data_only--" ELSE convert(varchar('+convert(varchar(10),@maxIndName)+'), iname) END, '
		+ '	index_size_KB = convert(int, index_size * (low / 1024)),'
	end

	select @sql = @sql
		+ '	unused_KB     = convert(int, unused     * (low / 1024))'
		+ ' from #pagecountsGorans'
		+ ' where 1=1'

	if (@list_indices = 0)
	begin
		select @sql = @sql
		+ ' and indid <= 1'
	end

	if (@orderby is NULL)
	begin
		if (@list_indices = 0)
		begin
			select @orderby = "row_count, name"
		end
		else
		begin
			select @orderby = "name, indid"
		end
	end

	if (@where is not NULL)
	begin
		select @sql = @sql + " and " + @where
	end

	select @sql = @sql
		+ ' order by ' + @orderby

	/*
	** Now execute the SQL
	*/
	--print @sql
	exec(@sql)

	/*** END: ADDED BY: gorans@sybase.com */








	select distinct database_name = db_name(), database_size =
		ltrim(str(sum(size) / (1048576 / d.low), 10, 1)) + " MB"
		into #spaceused1result
		from master.dbo.sysusages, master.dbo.spt_values d
			where dbid = db_id()
				and d.number = 1
				and d.type = "E"
			having dbid = db_id()
				and d.number = 1
				and d.type = "E"
	exec sp_autoformat #spaceused1result
	drop table #spaceused1result
	/*
	** Obtain the page count for syslogs table. 
	** 
	** The syslogs system table has only data (no index does exist).
	** Built-in functions reserved_pages and data_pages will always 
	** return the same value for syslogs.
	** This is due to the fact that syslogs pages are allocated an extent
	** worth at a time and all log pages in this extent are set as in use.
	** This is why we aren't able to determine the amount of unused 
	** syslogs pages by simply doing reserved_pages - data_pages.
	**
	** Also note that syslogs table doesn't have OAM pages.  However,
	** builtin functions reserved_pages() and data_pages() handle syslogs
	** as a special case.
	*/
	select @slog_res_pgs = convert(numeric(20, 9),
	      reserved_pages(db_id(), 8)),
	      @slog_dpgs = convert(numeric(20, 9),
	      data_pages(db_id(), 8))

	/*
	** Obtain the page count for all the objects in the current
	** database; except for 'syslogs' (id = 8). Store the results
	** in a temp. table (#pgcounts).
	**
	** Note that we first retrieve the needed information from
	** sysindexes and we only then apply the OAM builtin system
	** functions on that data.  The reason being we want to relax
	** keeping the sh_int table lock on sysindexes for the duration
	** of the command.
	*/
	select distinct
		s.name,
		s.id,
		s.indid,
		res_pgs = 0,
		low = d.low,
		dpgs = convert(numeric(20, 9), 0),
		ipgs = convert(numeric(20, 9), 0),
		unused = convert(numeric(20, 9), 0)
	into #pgcounts 
	from sysindexes s, master.dbo.spt_values d
		where s.id != 8
			and d.number = 1 
			and d.type = "E" 
		having d.number = 1
			and d.type = "E"

	/* Calculate the reserved pages, data pages, index pages and
	** unused pages. Note that we take care of the special case
	** of indid = 1, 0 in later steps. For indid = 1 case we need
	** to get the data pages and index pages in separate steps.
	*/
	update #pgcounts set
		res_pgs = reserved_pages(db_id(), id, indid),
                ipgs = convert(numeric(20, 9), data_pages(db_id(), id, indid)),
		unused = convert(numeric(20, 9),
			  (reserved_pages(db_id(), id, indid) 
			  - data_pages(db_id(), id, indid)))
	where indid  > 1

	/* get the data pages for indid = 0 */
	update #pgcounts set
		res_pgs = reserved_pages(db_id(), id, indid),
		dpgs = convert(numeric(20, 9), data_pages(db_id(), id, indid)),
		unused = convert(numeric(20, 9),
			   (reserved_pages(db_id(), id, indid)
			   - data_pages(db_id(), id, indid)))
	where indid = 0
		
	
	/* For the clustered index case calculate the data and reserved pages
	** by passing in indid of 0 to the builtins. Note, for indid = 1
	** the data pages are accounted for in ipgs. 
	*/
	update #pgcounts set
		ipgs = ipgs 
		  + convert(numeric(20, 9), data_pages(db_id(), id, indid)),
		dpgs = dpgs
		  + convert(numeric(20, 9), data_pages(db_id(), id, 0)),
                res_pgs = res_pgs 
			+ reserved_pages(db_id(), id, 0)
			+ reserved_pages(db_id(), id, indid)
        where indid = 1

	/* Calculate the unused count for the special case of indid  = 1 */
	update #pgcounts set
		unused = convert(numeric(20, 9), (res_pgs - dpgs - ipgs))
	where indid = 1

	/*
	** Compute the summary results by adding page counts from
	** individual data objects. Add to the count the count of 
	** pages for 'syslogs'.  Convert the total pages to space
	** used in Kilo bytes.
	*/
	select distinct reserved = convert(char(15), convert(varchar(11),
		convert(numeric(11, 0), (sum(res_pgs) + @slog_res_pgs) *
			(low / 1024))) + " " + "KB"),
		data = convert(char(15), convert(varchar(11),
			convert(numeric(11, 0), (sum(dpgs) + @slog_dpgs) *
			(low / 1024))) + " " + "KB"),
		index_size = convert(char(15), convert(varchar(11),
			convert(numeric(11, 0),  sum(ipgs) * (low / 1024)))
			+ " " + "KB"),
		unused = convert(char(15), convert(varchar(11),
			convert(numeric(11, 0), sum(unused) * (low / 1024)))
			+ " " + "KB")
	into #fmtpgcnts
	from #pgcounts

	exec sp_autoformat #fmtpgcnts
	drop table #fmtpgcnts
end

/*
**  We want a particular object.
*/
else
begin
	if (@tabname = "syslogs") /* syslogs */
	begin
		declare @free_pages	int, /* log free space in pages */
			@clr_pages	int, /* log space reserved for CLRs */
			@total_pages	int, /* total allocatable log space */
			@used_pages	int, /* allocated log space */
			@ismixedlog	int  /* mixed log & data database ? */

		select @ismixedlog = status2 & 32768
			from master.dbo.sysdatabases where dbid = db_id()

		select @clr_pages = lct_admin("reserved_for_rollbacks", 
						db_id())
		select @free_pages = lct_admin("logsegment_freepages", db_id())
				     - @clr_pages

		select @total_pages = sum(u.size)
		from master.dbo.sysusages u
		where u.segmap & 4 = 4
		and u.dbid = db_id()

		if(@ismixedlog = 32768)
		begin
			/* 
			** For a mixed log and data database, we cannot
			** deduce the log used space from the total space
			** as it is mixed with data. So we take the expensive
			** way by scanning syslogs.
			*/
			select @used_pages = lct_admin("num_logpages", db_id())

			/* Account allocation pages as used pages */
			select @used_pages = @used_pages + (@total_pages / 256)
		end
		else
		begin
			/* Dedicated log database */
			select @used_pages = @total_pages - @free_pages 
					   - @clr_pages
		end

		select	name = convert(char(15), @tabname),
			total_pages = convert(char(15), @total_pages),
			free_pages = convert(char(15), @free_pages),
			used_pages = convert(char(15), @used_pages),
			reserved_pages = convert(char(15), @clr_pages)
	end
	else
	begin
		/*
		** Obtain the page count for the target object in the current
		** database and store them in the temp table #pagecounts.
		**
		** Note that we first retrieve the needed information from
		** sysindexes and we only then apply the OAM builtin system
		** functions on that data.  The reason being we want to relax
		** keeping the sh_int table lock on sysindexes for the duration
		** of the command.
		*/
		select  name = o.name,
			tabid = i.id,
			iname = i.name, 
			indid = i.indid,
			low = d.low,
			rowtotal = convert(numeric(18,0), 0),
			reserved = convert(numeric(20, 9), 0),
			data = convert(numeric(20, 9), 0),
			index_size = convert(numeric(20, 9), 0),
			unused = convert(numeric(20, 9), 0)
		into #pagecounts
		from sysobjects o, sysindexes i, master.dbo.spt_values d
				where i.id = object_id(@objname)
					and o.id = i.id
					and d.number = 1
					and d.type = "E"
		
		/* perform the row counts */
		update #pagecounts
			set rowtotal = row_count(db_id(), tabid)
		where indid <= 1

		/* calculate the counts for indid > 1
		** case of indid = 1, 0 are special cases done later
		*/
		update #pagecounts set
			reserved = convert(numeric(20, 9),
			    reserved_pages(db_id(), tabid, indid)),
			index_size =  convert(numeric(20, 9),
			    data_pages(db_id(), tabid, indid)),
			unused = convert(numeric(20, 9),
				 ((reserved_pages(db_id(), tabid, indid) -
				  (data_pages(db_id(), tabid, indid)))))
		where indid > 1


		/* calculate for case where indid = 0 */
		update #pagecounts set
                        reserved = convert(numeric(20, 9),
                            reserved_pages(db_id(), tabid, indid)),
                        data = convert(numeric(20, 9),
                            data_pages(db_id(), tabid, indid)),
                        unused = convert(numeric(20, 9),
                                 ((reserved_pages(db_id(), tabid, indid) -
                                  (data_pages(db_id(), tabid, indid)))))
                where indid = 0


		/* handle the case where indid = 1, since we need
		** to take care of the data and index pages. 
		*/
		update #pagecounts set
			reserved = convert(numeric(20, 9),
			             reserved_pages(db_id(), tabid, 0)) 
			          +  convert(numeric(20, 9),
			             reserved_pages(db_id(), tabid, indid)),
			index_size = convert(numeric(20, 9),
				     data_pages(db_id(), tabid, indid)),
		        data = convert(numeric(20, 9),
				       data_pages(db_id(), tabid, 0))
		where indid = 1

		/* calculate the unused count for indid = 1 case.*/
		update #pagecounts set
			unused = convert(numeric(20, 9), 
				     reserved - data - index_size)
		where indid = 1


	    if (@list_indices = 1)
	    begin
        	select  index_name = iname,
			size = convert(char(10), convert(varchar(11),
	    	       	       convert(numeric(11, 0),
				       index_size / 1024 *
		        			low)) + " " + "KB"),
	    		reserved = convert(char(10), 
				   convert(varchar(11),
	    	       	   	   convert(numeric(11, 0),
					   reserved / 1024 * 
		       			   low)) + " " + "KB"),
	    		unused = convert(char(10), convert(varchar(11),
	    		 	 convert(numeric(11, 0), unused / 1024 *
					 low)) + " " + "KB")
		into #formatpgcounts
		from #pagecounts
		where indid > 0

		exec sp_autoformat #formatpgcounts
		drop table #formatpgcounts
	    end

	    select distinct name,
	    	rowtotal = convert(char(15), sum(rowtotal)),
		reserved = convert(char(15), convert(varchar(11),
		           convert(numeric(11, 0), sum(reserved) *
			   (low / 1024))) + " " + "KB"),
		data = convert(char(15), convert(varchar(11),
		       convert(numeric(11, 0), sum(data) * (low / 1024)))
		       + " " + "KB"),
		index_size = convert(char(15), convert(varchar(11),
			     convert(numeric(11, 0), sum(index_size) *
			     (low / 1024))) + " " + "KB"),
		unused = convert(char(15), convert(varchar(11),
		    	 convert(numeric(11, 0), sum(unused) *
			 (low / 1024))) + " " + "KB")
		into #fmtpgcounts
	        from #pagecounts

		exec sp_autoformat #fmtpgcounts
		drop table #fmtpgcounts
	end
end
go

exec sp_procxmode 'sp_spaceused2', 'anymode'
go
grant execute on sp_spaceused2 to public
go

use sybsystemprocs
go
IF EXISTS (SELECT 1 FROM sysobjects
           WHERE name = 'sp_sysmon_dcachestats'
             AND id = object_id('sp_sysmon_dcachestats')
             AND type = 'P')
	DROP PROCEDURE sp_sysmon_dcachestats
go

create or replace procedure sp_sysmon_dcachestats
  @top_n_objs 	varchar(14),		/* top 'n' objects to report on */
  @cachename	varchar(255),		/* cache name */
  @seconds	int,
  @Reco_option	char(1)			/* print recommendations */
as

declare	@blankline		char(80)
declare	@separator_line		char(80)
declare	@rpt_line		char(80)	/* formatted line for print */
declare	@exec_str		char(1250)	/* used in exec immediate */
declare	@ldspc			char(1)
declare	@col_sep		char(1)
declare	@psign			char(1)		/* percent sign */
declare	@ret			int
declare	@cache_size_in_KB	bigint
declare	@top_n			int		/* int value of top_n_objs */

declare	@cache_sz_str		varchar(13)
declare	@cache_hit_per		varchar(6)
declare	@cache_usage_per	varchar(6)
declare	@cache_sz_used		real
declare	@logical_reads		int
declare	@physical_reads		int
declare	@spinlock_contention	float
declare	@cache_partitions	int
declare	@cache_id		int

declare	@db_name		varchar(255)
declare	@obj_dbid		int
declare	@tab_id			int
declare	@obj_ind_id		int
declare	@obj_size		int
declare	@obj_lreads		int
declare	@obj_preads		int
declare	@obj_cached_KB		int	
declare	@cache_occp_by_obj_per	varchar(6)
declare	@obj_hit_per		varchar(6)
declare @display_order		int		/* 1 - desc, 0 - asc */
declare	@begin_time		datetime	/* values were cleared */
declare	@end_time		datetime	/* values are reported */
declare	@cut_off_lreads		int		/* value that decides rows
						** selection */
declare @max_objname_len	int		/* auto format object name */
declare	@tmpval			int
declare	@reco			varchar(255)	/* recommendation string */
declare	@max_reco_len		int
declare	@num_engines		int
declare	@numKBperpg		int
declare	@tmp_int		bigint
declare	@iter_cnt		int
declare	@tmp_cntr		varchar(35)
declare	@tmp_grp		varchar(18)	/* group id in sysmonitors */

declare @LReads_col		varchar(6)
declare @PReads_col		varchar(6)
declare @LReads_rename_col	varchar(6)
declare @PReads_rename_col	varchar(6)
declare	@Usage_Per_col		varchar(6)
declare @Run_Size_col		varchar(8)
declare @Cache_Partitions_col	varchar(16)
declare	@Spin_Contention_col	varchar(20)
declare	@Hit_Per_col		varchar(5)
declare @IO_Size_col		varchar(7)
declare	@Wash_col		varchar(9)
declare	@APF_Per_col		varchar(5)
declare	@APF_Eff_Per_col	varchar(9)
declare	@Object_col		varchar(6)
declare	@Obj_Size_col		varchar(8)
declare	@Size_In_Cache_col	varchar(13)
declare	@Obj_Cached_Per_col	varchar(12)
declare	@Cache_Occupied_Per_col	varchar(12)

declare @tempdbname		varchar(30)	/* assigned tempdb */

select	@blankline	= " "
select	@rpt_line	= " "
select	@separator_line = replicate("-", 80)
select	@ldspc	= " "
select	@col_sep = ":"
select	@psign = "%"
select	@numKBperpg = @@maxpagesize/1024

select @LReads_col = "LReads"
select @PReads_col = "PReads"
select @LReads_rename_col = "LR/sec"
select @PReads_rename_col = "PR/sec"
select @Usage_Per_col = "Usage%"
select @Run_Size_col = "Run Size"
select @Cache_Partitions_col = "Cache Partitions"
select @Spin_Contention_col = "Spinlock Contention%"
select @Hit_Per_col = "Hit%"
select @IO_Size_col = "IO Size"
select @Wash_col = "Wash Size"
select @APF_Per_col = "APF%"
select @APF_Eff_Per_col = "APF-Eff%"
select @Object_col = "Object"
select @Obj_Size_col = "Obj Size"
select @Size_In_Cache_col = "Size in Cache"
select @Obj_Cached_Per_col = "Obj_Cached%"
select @Cache_Occupied_Per_col = "Cache_Occp%"

create table #obj_details(dbid int, owner varchar(30), objid int,
				indid int, index_name varchar(30),
				size_KB int)

create table #recommendations_tab(reco_col varchar(255))

select 	co.config, parent, convert(char(30), co.name) name,
	convert(char(30),co.comment) comment, cu.value run_size,
	memory_used wash_size, apf_percent apf_value
into 	#syscacheconfig
from 	master.dbo.sysconfigures co, master.dbo.syscurconfigs cu
where 	parent = 19
and 	co.config = cu.config
and 	co.name = cu.comment
order by name, config

select	@begin_time = min(InsertTime),
	@end_time = max(InsertTime)
from	tempdb.dbo.tempcachestats

select @top_n = convert(int, @top_n_objs)
select @display_order = sign(@top_n)
if @display_order = 0
begin
	select @display_order = 1
end
select @top_n = abs(@top_n)

set nocount on

/* Create the diff tables */
select 1 as "PrintOrder", cs2.CacheName, cs2.CachePartitions, cs2.CacheID,
	cs2.LogicalReads - cs1.LogicalReads as LogicalReads,
	cs2.PhysicalReads - cs1.PhysicalReads as PhysicalReads
into	#tempcachestats
from	tempdb.dbo.tempcachestats cs1,
	tempdb.dbo.tempcachestats cs2
where	cs2.CacheName = cs1.CacheName
and	cs2.InsertTime = @end_time
and	cs1.InsertTime = @begin_time

update	#tempcachestats
set	PrintOrder = 0
where	CacheName = 'default data cache'

select	bs2.CacheName,
	bs2.IOBufferSize,
	bs2.AllocatedKB,
	bs2.PagesTouched * convert(numeric, @@maxpagesize / 1024) as PoolUsed,
	bs2.PhysicalReads - bs1.PhysicalReads as PhysicalReads,
	tm1.value + tm2.value as LReads,
	tm2.value as PReads,
	tm3.value as APFs_Used
into	#tempbufpoolstats
from	tempdb.dbo.tempbufpoolstats bs2,
	tempdb.dbo.tempbufpoolstats bs1,
	#tempmonitors tm1,
	#tempmonitors tm2,
	#tempmonitors tm3
where	bs2.InsertTime = @end_time
and	bs1.InsertTime = @begin_time
and	bs2.CacheName = bs1.CacheName
and	bs2.IOBufferSize = bs1.IOBufferSize
and	tm1.group_name = "buffer_"
		+ convert(varchar(4), bs2.CacheID) 
and	tm2.group_name = tm1.group_name
and	tm3.group_name = tm1.group_name
and	tm1.field_name = "bufsearch_finds_"
			+ convert(varchar(3), bs2.IOBufferSize / 1024) + "k"
and	tm2.field_name = "bufread_"
			+ convert(varchar(3), bs2.IOBufferSize / 1024) + "k"
and	tm3.field_name = "apf_ios_used_"
			+ convert(varchar(3), bs2.IOBufferSize / 1024) + "k"


select InsertTime, sum(CachedKB) as CachedKB, CacheName, 
	DBID, ObjectID, IndexID
into #tempcachedobjstats 
from tempdb.dbo.tempcachedobjstats
group by InsertTime,ObjectID,IndexID

select	co1.CacheName,
	os2.LogicalReads - os1.LogicalReads as LogicalReads,
	os2.PhysicalReads - os1.PhysicalReads as PhysicalReads,
	os2.DBID, os2.ObjectID, os2.IndexID,
	0 as CachedKB
into	#tempobjstats
from	tempdb.dbo.tempobjstats os1,
	tempdb.dbo.tempobjstats os2,
	#tempcachedobjstats co1
where	os1.DBID *= co1.DBID
and	os1.ObjectID *= co1.ObjectID
and	os1.IndexID *= co1.IndexID
and	os2.DBID = os1.DBID
and	os2.ObjectID = os1.ObjectID
and	os2.IndexID = os1.IndexID
and	os2.InsertTime = @end_time
and	os1.InsertTime = @begin_time
and	co1.InsertTime = @end_time
union
select	co.CacheName,
	os.LogicalReads,
	os.PhysicalReads,
	os.DBID, os.ObjectID, os.IndexID,
	co.CachedKB
from	tempdb.dbo.tempobjstats os,
	#tempcachedobjstats co
where	os.DBID = co.DBID
and	os.ObjectID = co.ObjectID
and	os.IndexID = co.IndexID
and	os.InsertTime = @end_time
and	co.InsertTime = @end_time
and	not exists
			(select ObjectID from tempdb.dbo.tempobjstats
			where	ObjectID = os.ObjectID
			and	ObjectID = co.ObjectID
			and	DBID = os.DBID
			and	IndexID = os.IndexID
			and	InsertTime = @begin_time)

/* Objects that have 0 as LogicalReads are removed */
delete	#tempobjstats
where	LogicalReads = 0

update	#tempobjstats
set	os.CachedKB = co.CachedKB,
	os.CacheName = co.CacheName
from	#tempobjstats os,
	#tempcachedobjstats co
where	os.DBID = co.DBID
and	os.ObjectID = co.ObjectID
and	os.IndexID = co.IndexID
and	co.InsertTime = @end_time

/*
** create cursor to process cache rows
*/
if (@cachename = "NULL")
begin
	select @cachename = "%"
end

declare named_cache_cursor cursor for
	select 	CacheName,
		CachePartitions,
		CacheID,
		LogicalReads,
		PhysicalReads
	from 	#tempcachestats
	where CacheName like @cachename
	order by PrintOrder, CacheName
	for read only

/*
** Declare cursor for storing sampled object stats
*/
declare objstats_cursor cursor for
	select 	LogicalReads, PhysicalReads,
		DBID, ObjectID, IndexID , CachedKB
	from #tempobjstats
	where CacheName like @cachename
	and 
	(	(@display_order = 1 and LogicalReads >= @cut_off_lreads)
					or
		(@display_order = -1 and LogicalReads <= @cut_off_lreads)
	)
	order by PhysicalReads desc, (LogicalReads * @display_order) desc
for read only

open named_cache_cursor
fetch named_cache_cursor into @cachename, @cache_partitions, @cache_id,
	@logical_reads, @physical_reads

dump tran tempdb with truncate_only

/*
** If assigned temporary database is different than system tempdb, then
** dump that one as well.
*/
select @tempdbname = db_name(@@tempdbid)
if (@tempdbname != "tempdb")
begin
	dump tran @tempdbname with truncate_only
end

select @num_engines = count(*) from #tempmonitors
	where field_name="clock_ticks"
	and group_name like "engine_%"
	and value > 0

/*
** Print Report Header
*/
select @rpt_line= replicate("=", 80)
print @rpt_line

select @rpt_line = space(34) + "Cache Wizard" + space(34)
print @rpt_line

select @rpt_line= replicate("=", 80)
print @rpt_line

while (@@sqlstatus = 0)
begin
	print @blankline
	select @cachename = ltrim(rtrim(@cachename))
	select @rpt_line = replicate('-', datalength(@cachename))
	print @rpt_line
	
	print @cachename
	
	print @rpt_line
	
	select @cache_size_in_KB = run_size from #syscacheconfig
	where 	config=19
	and 	parent=19
	and	rtrim(name)=@cachename

	select @cache_sz_str = str(@cache_size_in_KB * 1.0 / 1024, 9, 2) + " Mb"
	
	select 	@cache_sz_used = sum(PoolUsed),
		@logical_reads = sum(LReads),
		@physical_reads = sum(PReads)
	from #tempbufpoolstats
	where CacheName like @cachename
	
	if (@logical_reads = 0)
	begin
		select @cache_hit_per = "   n/a"
	end
	else
	begin
		select @cache_hit_per = str(((@logical_reads - @physical_reads)
						* 100.0
						/ @logical_reads), 6, 2)
	end

	select @cache_usage_per = str(@cache_sz_used * 100.0
						/ @cache_size_in_KB, 6, 2)
	
	/* P.value is grabs and W.value is waits */
	select @spinlock_contention = isnull(100.0*sum(convert(float,W.value))/sum(convert(float,P.value)), 0)
			from #tempmonitors P,
			     #tempmonitors W
			where P.field_name = @cachename
			and P.group_name = "spinlock_p"
			and W.group_name = "spinlock_w"
			and P.field_id = W.field_id
			and P.value > 0
	
	select @rpt_line = @Run_Size_col + @ldspc + @ldspc
			+ @ldspc + @ldspc + @ldspc + @ldspc + @ldspc
			+ @ldspc + @col_sep
			+ @cache_sz_str + @ldspc + @ldspc + @ldspc
			+ @Usage_Per_col + @psign + @ldspc + @ldspc
			+ @ldspc + @ldspc + @ldspc + @ldspc + @ldspc
			 + @ldspc + @ldspc + @ldspc + @ldspc 
			+ @ldspc + @ldspc + @ldspc
		 	+ @col_sep + @ldspc + @ldspc + @ldspc + @ldspc
			+ @cache_usage_per
	print @rpt_line

	/* If the sampled period is less than 1 sec then @seconds will be '0'.*/
	/* Round @seconds to '1' if it is '0' to avoid devide by zero problems */

	if (@seconds = 0) select @seconds = 1
	
	select @rpt_line = @LReads_rename_col + @ldspc + @ldspc
			+ @ldspc + @ldspc + @ldspc + @ldspc + @ldspc
			+ @ldspc + @ldspc + @ldspc
			+ @col_sep
			+ str(@logical_reads * 1.0 / @seconds, 9, 2)
			+ @ldspc + @ldspc + @ldspc + @ldspc
			+ @ldspc + @ldspc
			+ @PReads_rename_col + @ldspc + @ldspc + @ldspc
			+ @ldspc + @ldspc + @ldspc + @ldspc + @ldspc
			+ @ldspc + @ldspc + @ldspc
			+ @ldspc + @ldspc + @ldspc
			+ @col_sep + @ldspc
			+ str(@physical_reads * 1.0 / @seconds, 9, 2)
			+ @ldspc + @ldspc
			+ @ldspc + @Hit_Per_col + @psign + @col_sep + @ldspc
			+ @cache_hit_per
	print @rpt_line

	select @rpt_line = @Cache_Partitions_col + @col_sep
			+ str(@cache_partitions, 9) + @ldspc  + @ldspc
			+ @ldspc + @ldspc + @ldspc + @ldspc
			+ @Spin_Contention_col + @psign
			+ @col_sep + @ldspc + @ldspc + @ldspc + @ldspc
			+ str(@spinlock_contention, 6, 2)
	print @rpt_line

	print @blankline
	
	/*
	** Print the Buffer Pool Stats
	*/
	select @rpt_line = "Buffer Pool Information"
	print @rpt_line
	
	print @separator_line

	select @exec_str = "select str(IOBufferSize / 1024, 3) + ' Kb' as '"
			+ @IO_Size_col
			+ "', str(wash_size, 7) + ' Kb' as '"
			+ @Wash_col
			+ "', str(AllocatedKB * 1.0 / 1024, 8, 2) + ' Mb' as '"
			+ @Run_Size_col
			+ "', str(apf_value, 6, 2) as '"
			+ @APF_Per_col
			+ "', str(" + @LReads_col + " * 1.0 / "
				+ convert(varchar(6), @seconds)
			+  ", 8, 2) as '" + @LReads_rename_col
			+ "', str(" + @PReads_col + " * 1.0 / "
				+ convert(varchar(6), @seconds)
			+ ", 8, 2) as '" + @PReads_rename_col
			+ "', case when ("
				+ @LReads_col 
				+ " = 0) then '   n/a' else str(("
				+ @LReads_col + " - " + @PReads_col
				+ ") * 100.0 / ("
				+ @LReads_col
				+ "), 6, 2) end as '" + @Hit_Per_col
			+ "', case when (PhysicalReads - "
				+ @PReads_col + " <= 0) then '   n/a' else str("
				+ "APFs_Used * 100.0 / (PhysicalReads - "
				+ @PReads_col + "), 6, 2) end as '"
				+ @APF_Eff_Per_col
			+ "', str(PoolUsed * 100.0 / AllocatedKB, 6, 2) as '"
			+ @Usage_Per_col
			+ "' from #syscacheconfig, #tempbufpoolstats "
			+ "where config = (19 + floor(1.45 * "
				+ "log(IOBufferSize / 1024))) and parent=19 "
				+ " and rtrim(name)='" + @cachename
				+ "' and CacheName like '" + @cachename
				+ "' order by IOBufferSize desc" 
	exec(@exec_str)
	print @blankline
	
	if @Reco_option = 'Y'
	begin
		if (@cache_sz_used * 100.0 / @cache_size_in_KB < 5.0)
		begin
			select @reco = @Usage_Per_col + " for '"
					+ @cachename + "'"
					+ " is low (< 5%)"
			insert into #recommendations_tab(reco_col)
				values(@reco)
		end
	
		select @exec_str = "insert into #recommendations_tab "
				+ " select '" + @Usage_Per_col + "'"
				+ " + ' for '"
				+ " + str(IOBufferSize/1024, 2) "
				+ " + 'k buffer pool in cache:' + CacheName"
				+ " + ' is low (< 5%)' "
				+ "from #tempbufpoolstats "
				+ "where PoolUsed * 100.0 / AllocatedKB < 5.0"
				+ " and CacheName like '" + @cachename + "'"
				+ " order by IOBufferSize desc"
		exec(@exec_str)
	
		select @tmp_grp = "buffer_"
				+ convert(varchar(4), @cache_id)

		if (@num_engines > 1 and (@spinlock_contention >= 10 or @spinlock_contention < 0))
		begin /* { */
			select @reco = "Consider using Named Caches or creating more cache partitions for '" +  @cachename + "' or both."
			insert into #recommendations_tab(reco_col)
							values(@reco)
		end /* } */

		/* recommendations for pool wash size */
		select @tmp_int = SUM(convert(bigint, value))
			from #tempmonitors
			where group_name = @tmp_grp
			and field_name like "bufgrab_dirty_%"

		if (@tmp_int is not null)
		begin /* { */
			select @iter_cnt = @numKBperpg
			while (@iter_cnt <= 8*@numKBperpg)
			begin    /* { */
				/*
				**  build pool specific counter name,
				**  bufgrab_Nk (ie bufgrab_16k)
				*/
				select @tmp_cntr = "bufgrab_dirty_"
					+ convert(varchar(3), @iter_cnt) + "k"

				select @tmp_int = value
					from #tempmonitors
					where group_name = @tmp_grp and
					field_name = @tmp_cntr

				if @tmp_int != 0
				begin /* { */
					/*
					** If We grabbed a buffer that was
					** dirty from this pool consider
					** increasing the wash size for
					** this buffer pool
					*/
					select @reco =
						"Consider increasing the 'wash size' of the "+ ltrim(str(@iter_cnt,3)) + "k pool for '" + @cachename + "'"
					insert into #recommendations_tab(reco_col) values(@reco)

				end /* } */
				/* get next pool size (power of 2) */
				select @iter_cnt = @iter_cnt * 2
			end /* } */

		end /* } */
			
		/* recommendations for pool addition */
		if (select value from #tempmonitors
			where group_name = @tmp_grp
			and field_name like "bufopt_lrgmass_reqd") > 0
		begin /* { */
			select @reco =
				"Consider adding a large I/O pool for '"
							+ @cachename + "'"
			insert into #recommendations_tab(reco_col)
							values(@reco)
		end /* } */

		insert into #recommendations_tab(reco_col) values(@blankline)
	end
	print @blankline

	/*
	** We want to order the objects by LogicalReads and then while printing
	** order them by PhysicalReads. The ordering being 'desc' or 'ascending'
	** is decided by @display_order. To achieve this, we will select the
	** cut-off
	** value from the ordered table, which will be the value at row position
	** @top_n when @top_n > 0. In case of 'asc', we will delete all values
	** greater than this value and in case of 'desc', delete all values
	** lesser than this value.
	*/
	set rowcount @top_n
	select 	@cut_off_lreads = LogicalReads,
		@tmpval = LogicalReads * @display_order
	from #tempobjstats
	where CacheName = @cachename
	order by 2 desc

	set rowcount 0
	
	open objstats_cursor
	fetch objstats_cursor into @obj_lreads,
				@obj_preads, @obj_dbid,
				@tab_id, @obj_ind_id,
				@obj_cached_KB
	/*
	** Printing object stats
	*/
	select @rpt_line = "Object Statistics"
	print @rpt_line
	
	print @separator_line
	print @blankline

	while(@@sqlstatus = 0)
	begin
		select @db_name = db_name(@obj_dbid)

		select @exec_str = "exec " + @db_name +
				"..sp_dcachestats_obj_details "
				+ str(@tab_id)
				+ ", "
				+ str(@obj_ind_id)
		exec(@exec_str)
		
		fetch objstats_cursor into @obj_lreads,
				@obj_preads, @obj_dbid,
				@tab_id, @obj_ind_id,
				@obj_cached_KB

	end
	
	close objstats_cursor
	
	select	@max_objname_len = max(datalength(db_name(dbid))
					+ datalength(owner)
					+ datalength(object_name(objid, dbid))
					+ datalength(index_name))
					+ 3
	from	#obj_details
	
	/* Print only when objects are present */
	if @max_objname_len is not NULL
	begin
		select @exec_str = " select convert(varchar("
			+ convert(varchar(3), @max_objname_len)
			+ "), db_name(o.dbid) + '.' + owner + '.' + "
			+ "isnull(object_name(o.objid, o.dbid),"
			+ "convert(varchar(10), o.objid)) + case when "
			+ " o.indid != 0 then '.' + o.index_name end) as "
			+ @Object_col
			+ ", str(LogicalReads * 1.0 / "
				+ convert(varchar(6), @seconds)
			+ ", 7, 2) as '" + @LReads_rename_col
			+ "', str(PhysicalReads * 1.0 / "
				+ convert(varchar(6), @seconds)
			+ ", 7, 2) as '" + @PReads_rename_col
			+ "', case when LogicalReads = 0 then '   n/a' else "
				+ " str(100.0 * (LogicalReads - PhysicalReads)"
					+ " / LogicalReads, 6, 2) end as '"
			+ @Hit_Per_col
			+ "', case when size_KB = 0 then '        n/a' else "
				+ "str(CachedKB * 100.0 / size_KB, 11, 2) end "
			+ "as '" + @Obj_Cached_Per_col
			+ "', str(CachedKB * 100.0 / "
			+ convert(varchar(19), @cache_size_in_KB)
			+ ", 11, 2) as '"
			+ @Cache_Occupied_Per_col
			+ "' from #tempobjstats t, #obj_details o "
			+ " where t.DBID = o.dbid and t.ObjectID = o.objid "
			+ " and t.IndexID = o.indid "
			+ " order by PhysicalReads desc, (LogicalReads * "
			+ convert(varchar(2), @display_order)
			+ ") desc"

		exec(@exec_str)

		print @blankline

		select @exec_str = " select convert(varchar("
			+ convert(varchar(3), @max_objname_len)
			+ "), db_name(o.dbid) + '.' + owner + '.' + "
			+ " isnull(object_name(o.objid, o.dbid),"
			+ "convert(varchar(10), objid)) + case when "
			+ " o.indid != 0 then '.' + index_name end) as "
			+ @Object_col
			+ ", str(size_KB, 8) + ' Kb' as '"
			+ @Obj_Size_col
			+ "', str(CachedKB, 8) + ' Kb' as '"
			+ @Size_In_Cache_col
			+ "' from #tempobjstats t, #obj_details o "
			+ " where t.DBID = o.dbid and t.ObjectID = o.objid "
			+ " and t.IndexID = o.indid"
			+ " order by PhysicalReads desc, (LogicalReads * "
			+ convert(varchar(2), @display_order)
			+ ") desc"
		exec(@exec_str)
	
		/* 
		** truncate #obj_details so that the output is not repeated
		** for the previous cache' objects
		*/
		truncate table #obj_details
	end
	else
	begin
		print "No Activity for objects in this interval"
	end

	fetch named_cache_cursor into @cachename, @cache_partitions, @cache_id,
			@logical_reads, @physical_reads
end
print @blankline
select @max_reco_len = isnull(max(datalength(reco_col)), 1)
					from #recommendations_tab
select @exec_str = "if (select count(*) from #recommendations_tab "
		+ "where reco_col != '" + @blankline
		+ "' ) > 0 " + " begin select convert(varchar("
		+ convert(varchar(3), @max_reco_len)
		+ "), reco_col) as 'TUNING RECOMMENDATIONS' "
		+ "from #recommendations_tab end"
exec(@exec_str)

print @blankline
print "LEGEND"
select @rpt_line = replicate('-', 6)
print @rpt_line
print "%1!		- number of logical reads per second, i.e. sum of cache & disk reads",
							@LReads_rename_col
print "%1!		- number of physical reads per second i.e. disk reads",
							@PReads_rename_col
print "%1!	- size of cache or buffer pool in Kilobytes",
							@Run_Size_col
print "%1!	- number of cache partitions", @Cache_Partitions_col
print "%1!	- Percentage spinlock contention for the cache", @Spin_Contention_col
print "%1!		- ratio of hits to total searches", @Hit_Per_col
print "%1!		- ratio of pages referenced to Run Size", @Usage_Per_col
print @blankline
print "%1!	- wash size of buffer pool in Kilobytes", @Wash_col
print "%1!		- asynchronous prefetch %% for this buffer pool",
								@APF_Per_col
print "%1!	- Ratio of buffers found in cache and brought in because",
								@APF_Eff_Per_col
print "			  of APF to the number of APF disk reads performed"
print @blankline
print "%1!		- combination of db, owner, object and index name",
								@Object_col
print "%1!	- size of the object in Kilobytes", @Obj_Size_col
print "%1!	- size occupied in cache in Kilobytes at the end of sample",
								@Size_In_Cache_col
print "%1!	- Ratio of 'Size in Cache' to 'Obj Size'", @Obj_Cached_Per_col
print "%1!	- Ratio of 'Size in Cache' to 'Run Size' of cache",
							@Cache_Occupied_Per_col
close named_cache_cursor
deallocate cursor named_cache_cursor
deallocate cursor objstats_cursor
return 0
go

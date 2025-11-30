use sybsystemprocs
go
IF EXISTS (SELECT 1 FROM sysobjects
           WHERE name = 'sp_sysmon_dcache_dtl'
             AND id = object_id('sp_sysmon_dcache_dtl')
             AND type = 'P')
	DROP PROCEDURE sp_sysmon_dcache_dtl
go


create or replace procedure sp_sysmon_dcache_dtl
        @NumEngines smallint,    /* number of engines online */
        @NumElapsedMs int,      /* for "per Elapsed second" calculations */
        @NumXacts int,          /* for per transactions calculations */
        @Reco   char(1)         /* Flag for recommendations             */
as
/* --------- declare local variables --------- */
declare @CacheName varchar(255)  /* Cache Name from cache id lookup */
declare @CacheID smallint       /* Cache ID to map to buffer_N group */
declare @NumCaches smallint     /* Number of Caches to Report On */
declare @TotalSearches bigint   /* Total Cache Searches on All Caches */
declare @j smallint             /* loop index to iterate through multi-counter
                                ** counters (pool...) */
declare @lrgpool tinyint        /* boolean (0=No, 1=yes) logic to print
                                ** "Lrg Pool Not Used" Msg
                                */
declare @gtlogpgszpool tinyint    /* Boolean set while looking for a pool 
                                **  > logical pagesize in current cache
                                **      0 : Did not find pool
                                **      1 : Did find a pool of current size.
                                */
declare @tmp_grp varchar(25)    /* temp var for build group_name's
                                ** ie. engine_N, disk_N */
declare @cfg_repl varchar(24)   /* configured value of replacement policy */
declare @run_repl varchar(24)   /* run value of replacement policy */
declare @c_status varchar(24)   /* Cache status */
declare @c_type   varchar(24)   /* Cache type */
declare @tmp_cntr varchar(35)   /* temp var for build field_name's
                                ** ie. bufgrab_Nk */
				/* temp var for building fieldname for
				** searches found in buffer pool */
declare @srchfound_cntr varchar(35)
declare @tmp_int bigint         /* temp var for integer storage */
declare @tmp_int2 int           /* temp var for integer storage */
declare @tmp_elc int            /* temp var for integer storage */
declare @tmp_lockless int       /* temp var for integer storage */
declare @tmp_wash int           /* temp var for integer storage */
declare @tmp_int3 int           /* temp var for integer storage used to read 
                                ** value of counter 'prefetch_kept_bp' */
declare @tmp_int4 int           /* temp var for integer storage used to read 
                                ** value of counter 'prefetch_cached_bp' */
declare @tmp_int_sum int        /* temp var for integer storage
                                ** @tmp_int_sum = @tmp_int3 + @tmp_int4 */
declare @tmp_total bigint       /* temp var for summing 'total #s' data */
declare @tmp_total_send bigint  /* temp var for summing 'total #s' data */
declare @tmp_total_recv bigint  /* temp var for summing 'total #s' data */
declare @tmp_float float        /* temp var for float storage */
declare @tmp_float2 float       /* temp var for float storage */
declare @numKBperpg int		/* number of kilobytes per logical page */
declare @subsection char(80)    /* string to delimit subsections on printout */
declare @sum1line char(80)      /* string to delimit total lines without
                                ** percent calc on printout */
declare @sum2line char(67)      /* string to delimit total lines with
                                ** percent calc on printout */
declare @blankline char(1)      /* to print blank line */
declare @psign char(3)          /* hold a percent sign (%) for print out */
declare @na_str char(3)         /* holds 'n/a' for 'not applicable' strings */
declare @zero_str char(80)      /* hold an output string for zero "  0.0" for
                                ** printing zero "% of total" */
declare @rptline varchar(530)    /* formatted stats line for print statement */
declare @section char(80)       /* string to delimit sections on printout */
declare @totalrequested bigint	/* total no of physical locks requested */
declare @totalretained bigint	/* total no of physical locks retained */
declare @totaldiskreads bigint	/* total no of diskreads completed for physical
				** lock acquisition.*/
declare @totaldeadlocks bigint  /* total no of deadlocks occured while taking
				** physical lock. */
declare @totaltransfers bigint	/* total no. of buffers transferred to this
				** instance. */
declare @totalwaits bigint	/* totan no. of times tasks had to wait for a
				** transfer to this instance. */
declare @totaldiskwrites bigint	/* total no of disk writes when a transfer
				** is requested.*/
declare @totallockmgrcalls bigint	/* total no. of times lock manager was 
				** consulted for physical lock. */
declare @NumElapsedSec real	/* No. of elapsed seconds */

/* ------------- Variables for Tuning Recommendations ------------*/
declare @recotxt varchar(300)
declare @recoline char(80)
declare @reco_hdr_prn bit
declare @spinlock_contention float

/* --------- Setup Environment --------- */
set nocount on			/* disable row counts being sent to client */

select @subsection = "-------------------------------------------------------------------------------"
select @sum1line   = "  -------------------------  ------------  ------------  ----------  ----------"
select @sum2line   = "  -------------------------  ------------  ------------  ----------"
select @blankline  = " "
select @psign      = " %%"              /* extra % symbol because '%' is escape char in print statement */
select @na_str     = "n/a"
select @zero_str   = "                                      0.0           0.0           0       n/a"
select @section = "==============================================================================="

/*
** Declare cursor to walk temp cache table in cache name
** order to print cache-specific statistics
*/
select @TotalSearches = SUM(convert(bigint, value))
  from #tempmonitors
  where group_name like "buffer_%" and
                field_name = "bufsearch_calls"

declare cache_info cursor for
	select cid, name, group_name, cache_type,
		ltrim(rtrim(config_replacement)),ltrim(rtrim(run_replacement))
	  from #cachemap
	  order by name
	  for read only

open cache_info
fetch cache_info into @CacheID, @CacheName, @tmp_grp, @c_type, @cfg_repl,@run_repl

/* 
** Get all Spinlock related counters and cache names
** for printing potential spinlock contention.
*/
select  P.field_name as name,
	P.value as grabs,
	W.value as waits,
	S.value as spins into #foo
from #tempmonitors P, #tempmonitors W, #tempmonitors S
where
	    P.group_name = "spinlock_p"
	and W.group_name = "spinlock_w"
	and S.group_name = "spinlock_s"
	and P.field_id = W.field_id
	and P.field_id = S.field_id
	and P.field_name in ( select name from #cachemap )

/* Create a #temp table to store information on currently configured
** pools for each cache.
*/
 
/* Initilize some variables to avoid divide by zero error */
if @NumElapsedMs = 0
begin
	select @NumElapsedMs = 1
end

if @NumXacts = 0
begin
	select @NumXacts = 1
end

select @NumElapsedSec = @NumElapsedMs / 1000.0

while (@@sqlstatus = 0) /* { */
  begin
	print @subsection
	select @rptline = space(2) + "Cache: " + @CacheName
	print @rptline
	print "                                  per sec      per xact       count  %% of total"
	print @sum1line

	/* Print spinlock contention */

	select @spinlock_contention =
			isnull(100.0*(sum(convert(float,waits))/sum
				(case when grabs >= 0 then convert(float,grabs)
				else (power(2.0,32) + convert(float,grabs)) end)), 0)
	  from #foo
	 where name = @CacheName
	   and grabs != 0
 
	select @rptline = "    Spinlock Contention" + space(15) +
						@na_str + space(11) +
						@na_str + space(9) +
						@na_str + space(5) +
						str( @spinlock_contention,5,1) 
							+ @psign
	print @rptline
	print @blankline

	if @TotalSearches != 0
	  begin
		select @tmp_int = value
		  from #tempmonitors
		  where group_name = @tmp_grp and
				field_name = "bufsearch_calls"

		select @rptline = "    Utilization " + space(22) +
						@na_str + space(11) +
						@na_str + space(9) +
						@na_str + space(5) +
						str(100.0 * @tmp_int / 
							@TotalSearches,5,1) + 
						@psign

		print @rptline
		print @blankline
	  end

	print "    Cache Searches"

	select @tmp_total = value
	  from #tempmonitors
	  where group_name = @tmp_grp and
			field_name = "bufsearch_calls"

	if @tmp_total = 0		/* Avoid Divide by Zero Errors */
 	begin
		select @rptline = "      Total Cache Searches            0.0           0.0           0       n/a"
  		print @rptline
 	end
	else
	  begin

		select @tmp_int = value
		  from #tempmonitors
		  where group_name = @tmp_grp and
				field_name = "bufsearch_finds"

		select @rptline = "      Cache Hits" + space(13) +
				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
				space(2) +
				str(@tmp_int / convert(real, @NumXacts),12,1) +
				space(1) +
				str(@tmp_int, 11) + space(5) +
				str(100.0 * @tmp_int / @tmp_total,5,1) + 
				@psign
		print @rptline

		/* save hits for wash % and missed calc */
		select @tmp_int2 = @tmp_int

		select @tmp_int = value
		  from #tempmonitors
		  where group_name = @tmp_grp and
				field_name = "bufsearch_finds_in_wash"
	
		if @tmp_int2 != 0
		begin
			select @rptline = "         Found in Wash" + space(7) +
				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
				space(2) +
				str(@tmp_int / convert(real, @NumXacts),12,1) +
				space(1) +
				str(@tmp_int, 11) + space(5) +
				str(100.0 * @tmp_int / @tmp_int2, 5, 1) + 
				@psign
			print @rptline
		end

		/*
		** Print the split between all the cache hits only for the case of 
		** Lockless data caches
		*/
		if @c_type = "Lockless Data Cache"
		begin
			select @tmp_wash = @tmp_int
	
			select @tmp_int = value
			  from #tempmonitors
			  where group_name = @tmp_grp and
				field_name = "bufsearch_finds_in_elc"
	
			if @tmp_int2 != 0
			begin
				select @rptline = "                   ELC" + space(7) +
					str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
					space(2) +
					str(@tmp_int / convert(real, @NumXacts),12,1) +
					space(1) +
					str(@tmp_int, 11) + space(5) +
					str(100.0 * @tmp_int / @tmp_int2, 5, 1) + 
					@psign
				print @rptline
			end

			select @tmp_elc = @tmp_int

			select @tmp_int = value
			  from #tempmonitors
		  	where group_name = @tmp_grp and
				field_name = "buf_cachesearch_lockless_finds"
	
			if @tmp_int2 != 0
			begin
				select @rptline = "      w/o spinlockheld" + space(7) +
					str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
					space(2) +
					str(@tmp_int / convert(real, @NumXacts),12,1) +
					space(1) +
					str(@tmp_int, 11) + space(5) +
					str(100.0 * @tmp_int / @tmp_int2, 5, 1) + 
					@psign
				print @rptline
			end

			select @tmp_lockless = @tmp_int

			if @tmp_int2 != 0
			begin
				select @tmp_int = @tmp_int2 - @tmp_lockless - @tmp_elc - @tmp_wash
				select @rptline = "     with spinlockheld" + space(7) +
					str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
					space(2) +
					str(@tmp_int / convert(real, @NumXacts),12,1) +
					space(1) +
					str(@tmp_int, 11) + space(5) +
					str(100.0 * @tmp_int / @tmp_int2, 5, 1) + 
					@psign
				print @rptline
			end
		end
		select @tmp_int = @tmp_total - @tmp_int2  /* missed searches */

		select @rptline = "      Cache Misses" + space(11) +
				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
				space(2) +
				str(@tmp_int / convert(real, @NumXacts),12,1) +
				space(1) +
				str(@tmp_int, 11) + space(5) +
				str(100.0 * @tmp_int / @tmp_total,5,1) + 
				@psign
		print @rptline
      end	/* else @tmp_total = 0 */

	print @sum2line
	select @rptline = "    Total Cache Searches" + space(5) +
			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
			space(2) +
			str(@tmp_total / convert(real, @NumXacts),12,1) + 
			space(2) +
			str(@tmp_total,10)
	print @rptline

	print @blankline


	select @tmp_total = SUM(convert(bigint, value))
	  from #tempmonitors
	  where group_name = @tmp_grp and
			field_name like "bufgrab_%k" and field_name not like "bufgrab_ref%k"

	select @numKBperpg = @@maxpagesize/1024
	if @tmp_total = 0
 	begin
		select @rptline = "    Pool Turnover                     0.0           0.0           0       n/a"
  		print @rptline
 	end
	else
          begin /* { */
		print "    Pool Turnover"
		/* init loop ctr to loop through all pool sizes */
		select @j = @numKBperpg
                while (@j <= 8*@numKBperpg)        /* { */
		  begin

			/* 
			**  build pool specific counter name, 
			**  bufgrab_Nk (ie bufgrab_16k) 
			*/
			select @tmp_cntr = "bufgrab_" + rtrim(convert(varchar(3), @j)) + "k"

			select @tmp_int = value
			  from #tempmonitors
			  where group_name = @tmp_grp and
					field_name = @tmp_cntr

                        if @tmp_int != 0        /* { */
			  begin

				select @rptline = space(6) + 
					convert(char(3),@j) + "Kb Pool"
				print @rptline

				select @rptline = "          LRU Buffer Grab" +
					space(4) +
					str(@tmp_int / (@NumElapsedMs / 
						1000.0),12,1) + 
					space(2) +
					str(@tmp_int / 
						convert(real, @NumXacts),12,1)+
					space(1) +
					str(@tmp_int, 11) + space(5) +
					str(100.0 * @tmp_int / @tmp_total,5,1)+
					@psign
				print @rptline

				select @tmp_cntr = "bufgrab_locked_" + 
					convert(varchar(3), @j) + "k"

				select @tmp_int2 = value
				  from #tempmonitors
				  where group_name = @tmp_grp and
						field_name = @tmp_cntr

				select @rptline = space(12) + "Grabbed Locked Buffer" +
					space(1) +
					str(@tmp_int2 / (@NumElapsedMs / 
						1000.0),7,1) + 
					space(2) +
					str(@tmp_int2 / 
						convert(real, @NumXacts),12,1)+
					space(1) +
					str(@tmp_int2, 11) + space(5) +
					str(100.0 * @tmp_int2 / @tmp_int,5,1) +
					@psign
				print @rptline

				select @tmp_cntr = "bufgrab_dirty_" + 
					convert(varchar(3), @j) + "k"

				select @tmp_int2 = value
				  from #tempmonitors
				  where group_name = @tmp_grp and
						field_name = @tmp_cntr

				select @rptline = space(12) + "Grabbed Dirty" +
					space(4) +
					str(@tmp_int2 / (@NumElapsedMs / 
						1000.0),12,1) + 
					space(2) +
					str(@tmp_int2 / 
						convert(real, @NumXacts),12,1)+
					space(1) +
					str(@tmp_int2, 11) + space(5) +
					str(100.0 * @tmp_int2 / @tmp_int,5,1) +
					@psign
				print @rptline

                          end   /* } if @tmp_int != 0 */
			  
			/* get next pool size (power of 2) */
			select @j = @j * 2 

                end             /* } while */


		print @sum2line		/* calc cache turnover percent of all caches */
		select @rptline = "    Total Cache Turnover" + space(5) +
			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
			space(2) +
			str(@tmp_total / convert(real, @NumXacts),12,1) + 
			space(2) +
			str(@tmp_total,10)
		print @rptline
          end   /* } else @tmp_total != 0 */
	print @blankline

	print "    Cluster Cache Behavior"

	select @totalrequested = SUM(convert(bigint, value))
		from #tempmonitors
		where group_name = @tmp_grp and
			field_name like "physical_lock_acquisition"

	select @totalretained = SUM(convert(bigint, value))
		from #tempmonitors
		where group_name = @tmp_grp and
				field_name = "physical_lock_retented"

	select @totaldeadlocks = SUM(convert(bigint, value))
		from #tempmonitors
		where group_name = @tmp_grp and
				field_name = "physical_lock_deadlock"

	select @totalwaits = SUM(convert(bigint, value))
		from #tempmonitors
		where group_name = @tmp_grp and
				field_name = "waited_on_tx"

	select @totaltransfers = SUM(convert(bigint, value))
		from #tempmonitors
		where group_name = @tmp_grp and
			field_name = "physical_lock_txrecv"

	select @totaldiskreads = SUM(convert(bigint, value))
		from #tempmonitors
		where group_name = @tmp_grp and
				field_name = "diskread_lockmgr"

	select @totaldiskwrites = SUM(convert(bigint, value))
		from #tempmonitors
		where group_name = @tmp_grp and
				field_name = "diskwrite"

	select @totallockmgrcalls = SUM(convert(bigint, value))
		from #tempmonitors
		where group_name = @tmp_grp and
			field_name = "physical_lock_lockmgr"

	if @totalrequested = 0
	begin
		print "      No physical locks are acquired on buffers in this cache"
	end
	else
	begin
		select @rptline = "      Total Lock Requests    " +
			str(@totalrequested / @NumElapsedSec,12,1) +
			space(2) +
			str(@totalrequested / convert(real, @NumXacts),12,1) +
			space(1) +
			str(@totalrequested, 11) + space(5) +
			str(100.0 * @totalrequested / @totalrequested,5,1) +
			@psign
		print @rptline
		
		select @rptline = "      Retained Locks         " +
			str(@totalretained / @NumElapsedSec,12,1) +
			space(2) +
			str(@totalretained / convert(real, @NumXacts),12,1) +
			space(1) +
			str(@totalretained, 11) + space(5) +
			str(100.0 * @totalretained / @totalrequested,5,1) +
			@psign
		print @rptline
		
		select @rptline = "      Non-retained Locks     " +
			str((@totalrequested - @totalretained) / 
					@NumElapsedSec,12,1) +
			space(2) +
			str((@totalrequested - @totalretained) / 
					convert(real, @NumXacts),12,1) +
			space(1) +
			str((@totalrequested - @totalretained), 11) + space(5) +
			str(100.0 * (@totalrequested - @totalretained) / 
					@totalrequested,5,1) +
			@psign
		print @rptline
		
		select @rptline = "      Data Read from Disk    " +
			str(@totaldiskreads / @NumElapsedSec,12,1) +
			space(2) +
			str(@totaldiskreads / convert(real, @NumXacts),12,1) +
			space(1) +
			str(@totaldiskreads, 11) + space(5) +
			str(100.0 * @totaldiskreads / @totalrequested,5,1) +
			@psign
		print @rptline
		
		select @tmp_total_recv = SUM(convert(bigint, value))
			from #tempmonitors
			where group_name like "buffer_%" and
				field_name = "physical_lock_txrecv"

		if @tmp_total_recv > 0
		begin
			select @rptline = "      Transfers Received     " +
				str(@totaltransfers / @NumElapsedSec,12,1) +
				space(2) +
				str(@totaltransfers / 
					convert(real, @NumXacts),12,1) +
				space(1) +
				str(@totaltransfers, 11) + space(5) +
				str(100.0 * @totaltransfers / 
					@totalrequested,5,1) + 
				@psign
			print @rptline
		end
		else
		begin
			select @rptline = "      Transfers Received     " +
				str(0.0,12,1) +
				space(2) +
				str(0.0,12,1) +
				space(1) +
				str(0.0, 11) + space(5) + 
				str(0.0,5,1) + @psign
			print @rptline
		end

		select @rptline = "      Waited for Data Transfer" +
			str(@totalwaits / @NumElapsedSec,11,1) +
			space(2) +
			str(@totalwaits / convert(real, @NumXacts),12,1) +
			space(1) +
			str(@totalwaits, 11) + space(5) +
			str(100.0 * @totalwaits / @totalrequested,5,1) +
			@psign
		print @rptline
		
		select @rptline = "      Deadlocks              " +
			str(@totaldeadlocks / @NumElapsedSec,12,1) +
			space(2) +
			str(@totaldeadlocks / convert(real, @NumXacts),12,1) +
			space(1) +
			str(@totaldeadlocks, 11) + space(5) +
			str(100.0 * @totaldeadlocks / @totalrequested,5,1) +
			@psign
		print @rptline
		
		select @rptline = "      Data Write to Disk     " +
			str(@totaldiskwrites / @NumElapsedSec,12,1) +
			space(2) +
			str(@totaldiskwrites / convert(real, @NumXacts),12,1) +
			space(1) +
			str(@totaldiskwrites, 11) + space(5) + "  n/a "
		print @rptline
		
		select @tmp_total_send = SUM(convert(bigint, value))
			from #tempmonitors
			where group_name like "buffer_%" and
				field_name = "physical_lock_txsend"

		if @tmp_total_send > 0
		begin
			select @tmp_int = SUM(convert(bigint, value))
				from #tempmonitors
				where group_name = @tmp_grp and
					field_name = "physical_lock_txsend"

			select @rptline = "      Transfers Sent         " +
				str(@tmp_int / @NumElapsedSec,12,1) +
				space(2) +
				str(@tmp_int / convert(real, @NumXacts),12,1) +
				space(1) +
				str(@tmp_int, 11) + space(5) + "  n/a "
			print @rptline
		end
		else
		begin
			select @rptline = "      Transfers Sent         " +
				str(0.0,12,1) +
				space(2) +
				str(0.0,12,1) +
				space(1) +
				str(0.0, 11) + space(5) + "  n/a "
			print @rptline
		end

		if (@totallockmgrcalls) = 0
		begin
			select @rptline = "      Data Location Efficiency " +
					space(7) +
					@na_str + space(11) +
					@na_str + space(9) +
					@na_str + space(5) +
					str(100.0,5,1) +
					@psign
		end
		else
		begin
			select @rptline = "      Data Location Efficiency " +
					space(7) +
					@na_str + space(11) +
					@na_str + space(9) +
					@na_str + space(5) +
					str(100.0 - (100.0 * @totaltransfers /
					(@totallockmgrcalls)),5,1) +
					@psign
		end
		print @rptline
	end

	print @blankline

	print "    Buffer Wash Behavior"

	select @tmp_total = value
	  from #tempmonitors
	  where group_name = @tmp_grp and
			field_name = "bufwash_throughput"

	if @tmp_total != 0	/* any buffers move through wash yet? */
	  begin

		select @tmp_int = value
		  from #tempmonitors
		  where group_name = @tmp_grp and
				field_name = "bufwash_pass_clean"

		select @rptline = "      Buffers Passed Clean" + space(3) +
				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
				space(2) +
				str(@tmp_int / convert(real, @NumXacts),12,1) +
				space(1) +
				str(@tmp_int, 11) + space(5) +
				str(100.0 * @tmp_int / @tmp_total,5,1) + 
				@psign
		print @rptline

		select @tmp_int = value
		  from #tempmonitors
		  where group_name = @tmp_grp and
				field_name = "bufwash_pass_writing"

		select @rptline = "      Buffers Already in I/O " +
				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
				space(2) +
				str(@tmp_int / convert(real, @NumXacts),12,1) +
				space(1) +
				str(@tmp_int, 11) + space(5) +
				str(100.0 * @tmp_int / @tmp_total,5,1) + 
				@psign
		print @rptline

		select @tmp_int = value
		  from #tempmonitors
		  where group_name = @tmp_grp and
				field_name = "bufwash_write_dirty"

		select @rptline = "      Buffers Washed Dirty   " +
				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
				space(2) +
				str(@tmp_int / convert(real, @NumXacts),12,1) +
				space(1) +
				str(@tmp_int, 11) + space(5) +
				str(100.0 * @tmp_int / @tmp_total,5,1) + 
				@psign
		print @rptline
	  end
	else
		print "      Statistics Not Available - No Buffers Entered Wash Section Yet"

	print @blankline
	
	print "    Cache Strategy"

	/* 
	** Sum all buf unkeeps to look at % of buffers following 
	** MRU vs Discard Strategy 
	*/
	select @tmp_total = SUM(convert(bigint, value))
	  from #tempmonitors
	  where group_name = @tmp_grp and
			field_name IN ("bufunkeep_lru", "bufunkeep_mru")

	if @tmp_total != 0
	  begin
		select @tmp_int = value
		  from #tempmonitors
		  where group_name = @tmp_grp and
				field_name = "bufunkeep_lru"

		select @rptline = "      Cached (LRU) Buffers   " +
				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
				space(2) +
				str(@tmp_int / convert(real, @NumXacts),12,1) +
				space(1) +
				str(@tmp_int, 11) + space(5) +
				str(100.0 * @tmp_int / @tmp_total,5,1) + 
				@psign
		print @rptline

		select @tmp_int = value
		  from #tempmonitors
		  where group_name = @tmp_grp and
				field_name = "bufunkeep_mru"

		select @rptline = "      Discarded (MRU) Buffers"+
				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
				space(2) +
				str(@tmp_int / convert(real, @NumXacts),12,1) +
				space(1) +
				str(@tmp_int, 11) + space(5) +
				str(100.0 * @tmp_int / @tmp_total,5,1) + 
				@psign
		print @rptline
	  end
	else
		print "      Statistics Not Available - No Buffers Displaced Yet"

	print @blankline

	print "    Large I/O Usage"	

	select @tmp_total = value
	  from #tempmonitors
	  where group_name = @tmp_grp and
			field_name = "prefetch_req"

	if @tmp_total = 0
 	begin
		select @rptline = "      Total Large I/O Requests        0.0           0.0           0       n/a"
  		print @rptline
 	end
	else
	  begin

		select @tmp_int = SUM(convert(bigint, value))
		  from #tempmonitors
		  where group_name = @tmp_grp and field_name IN
			 ("prefetch_as_requested", "prefetch_page_realign", "prefetch_increase")

		select @rptline = "      Large I/Os Performed" + space(3) +
				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
				space(2) +
				str(@tmp_int / convert(real, @NumXacts),12,1) +
				space(1) +
				str(@tmp_int, 11) + space(5) +
				str(100.0 * @tmp_int / @tmp_total,5,1) + 
				@psign
		print @rptline
		print @blankline
		
		select @rptline="      Large I/Os Denied due to"
		print @rptline

		select @tmp_int = value
		  from #tempmonitors
		  where group_name = @tmp_grp and
			field_name = "prefetch_decrease"

		select @tmp_int3 = value
		  from #tempmonitors
		  where group_name = @tmp_grp and
			field_name = "prefetch_kept_bp"

		select @tmp_int4 = value
		  from #tempmonitors
		  where group_name = @tmp_grp and
			field_name = "prefetch_cached_bp"

		select @tmp_int_sum = @tmp_int3 + @tmp_int4
		select @tmp_int = @tmp_int - @tmp_int_sum

		select @rptline = "        Pool < Prefetch Size" + space(1) + 
				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
				space(2) +
				str(@tmp_int / convert(real, @NumXacts),12,1) +
				space(1) +
				str(@tmp_int, 11) + space(5) +
				str(100.0 * @tmp_int / @tmp_total,5,1) + 
				@psign
		print @rptline

		select @rptline = "        Pages Requested"
		print @rptline
		select @rptline = "        Reside in Another"
		print @rptline

		select @rptline = "        Buffer Pool" + space(10) + 
				str(@tmp_int_sum / 
					(@NumElapsedMs / 	
					1000.0),12,1) +
					space(2) +
					str(@tmp_int_sum / 
					convert(real, @NumXacts),12,1) +
					space(2) +
					str(@tmp_int_sum, 10) + 
						space(5) +
					str(100.0 * 
						@tmp_int_sum/ 
						@tmp_total,5,1) + 
						@psign
		print @rptline
		print @sum2line 
		select @rptline = "    Total Large I/O Requests " +
			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) +
			space(2) +
			str(@tmp_total / convert(real, @NumXacts),12,1) +
			space(2) +
			str(@tmp_total,10)
		print @rptline
	  end	/* else */

	print @blankline

	print "    Large I/O Detail"

	/*
	**  default to NO large pools found for this cache 
	*/
	select @lrgpool = 0	
	/* 
	** init loop counter to loop through all large I/O pool 
	*/
	select @j = @numKBperpg*2
        while (@j <= 8*@numKBperpg)        /* { */
	  begin

          /* Check that the current cache has a pool configured of size @j */
          select @gtlogpgszpool = count(*)
          from #pool_detail_per_cache pd
          where pd.io_size = convert(varchar(8), @j)
	  and name = @CacheName	
 
          if (@gtlogpgszpool > 0)
          begin                 /* { */
 
                /* Remember that we _did_ find a large I/O pool */
                select @lrgpool = 1

		/* 
		** build pool specific counter name, 
		** bufgrab_Nk (ie bufgrab_16k) 
		*/
		select @tmp_cntr = "bufgrab_" + convert(varchar(3), @j) + "k"

		select @tmp_total = value
		  from #tempmonitors
		  where group_name = @tmp_grp and
				field_name = @tmp_cntr

                select @rptline = space(5) + convert(char(4),@j) +
			"Kb Pool"
		print @rptline

                if @tmp_total = 0
                begin
                        select @rptline = "        Pages Cached" + space(18) +
                                          ltrim(@zero_str)
                        print @rptline
 
                        select @rptline = "        Pages Used" + space(20) +
                                          ltrim(@zero_str)
                        print @rptline
                end
 
                else
                  begin

			/* turn # of masses into # of logical pages */
			select @tmp_total = @tmp_total * (@j / @numKBperpg)	

			select @rptline = "        Pages Cached" + space(9) +
				str(@tmp_total / 
					(@NumElapsedMs / 1000.0),12,1) + 
				space(2) +
				str(@tmp_total / 
					convert(real, @NumXacts),12,1) + 
				space(1) +
				str(@tmp_total,11) + space(7) +
				@na_str
			print @rptline

			select @tmp_cntr = "bufgrab_ref_" + 
				convert(varchar(3), @j) + "K"

			select @tmp_int = value
			  from #tempmonitors
			  where group_name = @tmp_grp and
					field_name = @tmp_cntr

			select @rptline = "        Pages Used" + space(11) +
				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
				space(2) +
				str(@tmp_int / convert(real, @NumXacts),12,1) +
				space(1) +
				str(@tmp_int, 11) + space(5) +
				str(100.0 * @tmp_int / @tmp_total,5,1) + @psign
			print @rptline
		  end

                end     /* } if @gtlogpgszpool > 0 */

		select @j = @j * 2	/* get next pool size */
	  end /* } @j <= 8*@numKBperpg */

	if @lrgpool = 0		/* No large pools in this cache */
	  begin
		print "      No Large Pool(s) In This Cache"
	  end

        print @blankline

	print "    Dirty Read Behavior"
	
	select @tmp_total = value
	  from #tempmonitors
	  where group_name = @tmp_grp and
			field_name = "level0_bufpredirty"

	select @rptline = "	  Page Requests" + space(6) +
			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
			space(2) +
			str(@tmp_total / convert(real, @NumXacts),12,1) + 
			space(1) +
			str(@tmp_total,11) + space(7) +
			@na_str
	print @rptline
	print @blankline

	if @Reco = 'Y'
 	begin /* { */
		select @recotxt =   "    Tuning Recommendations for Data cache : "+@CacheName
		select @recoline =  "    -------------------------------------"
       	 	select @reco_hdr_prn = 0

		/* recommendations for cache replacement policy */

		select @tmp_float = convert(int, (100.0*a.value/b.value))
		  from #tempmonitors a, #tempmonitors b
		  where a.group_name = @tmp_grp and
		  	b.group_name = @tmp_grp and
			a.group_name = b.group_name and
			a.field_name = "bufsearch_finds" and
			b.field_name = "bufsearch_calls" and
			b.value != 0
		  if (@tmp_float is not null)
		  begin /* { */
			select @tmp_float2 = 0
			select @tmp_float2 = (100.0*a.value)/b.value
	  		 from #tempmonitors a, #tempmonitors b
	  		 where a.group_name = @tmp_grp and
		  		b.group_name = @tmp_grp and
				a.group_name = b.group_name and
				a.field_name = "bufwash_write_dirty" and
				b.field_name = "bufwash_throughput" and
				b.value != 0

			if (@tmp_float2 is null)
				select @tmp_float2 = 0

			/* 
			** If the Cache Hit Rate is greater than 95% and
			** the replacement is less than 5% and if the
			** existing replacement policy is "strict LRU"
			** then consider using "relaxed lru replacement"
			** policy for this cache.
			*/
			if ((@tmp_float >= 95.0 and @tmp_float2 <= 5.0) and
				@NumEngines > 1)
         		begin /* { */
				if (@run_repl = "strict LRU")
				 begin /* { */
                			if (@reco_hdr_prn = 0)
                			begin /* { */
                       				print @recotxt
						print @recoline
                        			select @reco_hdr_prn = 1
                			end /* } */
                			print "    - Consider using 'relaxed LRU replacement policy'"
                			print "      for this cache." 
					print @blankline
				 end /* } */
         		end /* } */
			else
         		begin /* { */
			/* 
			** If the Cache Hit Rate is less than 95% and
			** the replacement is greater than 5% and if the
			** existing replacement policy is "relaxed LRU"
			** then consider using "Strict lru replacement"
			** policy for this cache.
			*/
				if (@run_repl = "relaxed LRU")
			 	begin /* { */
                			if (@reco_hdr_prn = 0)
                			begin /* { */
                       				print @recotxt
						print @recoline
                        			select @reco_hdr_prn = 1
                			end /* } */
                			print "    - Consider using 'strict LRU replacement policy'."
                			print "      for this cache." 
					print @blankline
			 	end /* } */
         		end /* } */
 		end /* } */

		/* recommendations for pool wash size */

		select @tmp_int = SUM(convert(bigint, value))
	  		from #tempmonitors
	 		 where group_name = @tmp_grp  and
			field_name like "bufgrab_dirty_%"

		  if (@tmp_int is not null)
		  begin /* { */
			select @j = @numKBperpg
                	while (@j <= 8*@numKBperpg)    
		  	begin    /* { */

				/* 
				**  build pool specific counter name, 
				**  bufgrab_Nk (ie bufgrab_16k) 
				*/
				select @tmp_cntr = "bufgrab_dirty_" + 
						convert(varchar(3), @j) + "k"

				select @tmp_int = value
			  		from #tempmonitors
			  		where group_name = @tmp_grp and
					field_name = @tmp_cntr

                        	if @tmp_int != 0  
				begin /* { */
                			if (@reco_hdr_prn = 0)
                			begin /* { */
                       				print @recotxt
						print @recoline
                        			select @reco_hdr_prn = 1
                			end /* } */
					/*
					** If We grabbed a buffer that was
					** dirty from this pool consider increasing
					** the wash size for this buffer pool
					*/
                			select @rptline = "    - Consider increasing the 'wash size' of the "+ltrim(str(@j,3))+"k pool for this cache."
					print @rptline
					print @blankline

				end /* } */
				/* get next pool size (power of 2) */
				select @j = @j * 2 
			end /* } */	
				
		  end /* } */

		/* recommendations for pool addition */
		if (select value from #tempmonitors
	 		 where group_name = @tmp_grp  and
			field_name like "bufopt_lrgmass_reqd") > 0
                begin /* { */
                        if (@reco_hdr_prn = 0)
                        begin /* { */
                                print @recotxt
				print @recoline
                                select @reco_hdr_prn = 1
                        end /* } */
			/*
			** If the optimizer wanted to do large I/O but could
			** not find a buffer pool configured to be able
			** to do this large I/O consider having a large I/O
			** pool for this cache
			*/
                        print "    - Consider adding a large I/O pool for this cache."
			print @blankline
                end /* } */


		/* recommendations for pool removal */
		select @j = @numKBperpg*2
		select @tmp_cntr = "bufgrab_" + 
					convert(varchar(3), @j) + "k"
		select @srchfound_cntr = "bufsearch_finds_" +
					convert(varchar(3), @j) + "k"

		/*
		** The recommendation to remove a large buffer pool will be
		** printed only when the bufgrabs and bufsearch_finds are 0
		** for the buffer pool. This is to avoid
		** printing this message when the data is entirely cached
		** in the buffer pool and hence not having any grabs.
		*/
		if ((select value from #tempmonitors
	 		 where group_name = @tmp_grp  and
			field_name like @tmp_cntr) = 0
		   and (select value from #tempmonitors
			where group_name = @tmp_grp  and
			field_name like @srchfound_cntr) = 0
         	   and exists (select * from #pool_detail_per_cache 
          		where io_size = convert(varchar(8), @j)
          		and name = @CacheName))
		begin /* { */
			if (@reco_hdr_prn = 0)
			begin /* { */
				print @recotxt
				print @recoline
				select @reco_hdr_prn = 1
			end /* } */
			/*
			** If there are no grabs for this buffer pool
			** consider removing this buffer pool.
			*/
                	select @rptline = "    - Consider removing the "+ltrim(str(@j,3))+"k pool for this cache."
			print @rptline
			print @blankline
		end /* } */

		select @j = @j*2
		select @tmp_cntr = "bufgrab_" + convert(varchar(3), @j) + "k"
		select @srchfound_cntr = "bufsearch_finds_" +
					convert(varchar(3), @j) + "k"

		if ((select value from #tempmonitors
	 		 where group_name = @tmp_grp  and
			field_name like @tmp_cntr) = 0
		   and (select value from #tempmonitors
			where group_name = @tmp_grp  and
			field_name like @srchfound_cntr) = 0
         	   and exists (select * from #pool_detail_per_cache 
          		where io_size = convert(varchar(8), @j)
          		and name = @CacheName))
		begin /* { */
			if (@reco_hdr_prn = 0)
			begin /* { */
				print @recotxt
				print @recoline
				select @reco_hdr_prn = 1
			end /* } */
			/*
			** If there are no grabs for this buffer pool
			** consider removing this buffer pool.
			*/
                	select @rptline = "    - Consider removing the "+ltrim(str(@j,3))+"k pool for this cache."
			print @rptline
			print @blankline
		end /* } */

		select @j = @j*2
		select @tmp_cntr = "bufgrab_" + convert(varchar(3), @j) + "k"
		select @srchfound_cntr = "bufsearch_finds_" +
					convert(varchar(3), @j) + "k"

		if ((select value from #tempmonitors
	 		 where group_name = @tmp_grp  and
			field_name like @tmp_cntr) = 0
		   and (select value from #tempmonitors
			where group_name = @tmp_grp  and
			field_name like @srchfound_cntr) = 0
         	   and exists (select * from #pool_detail_per_cache 
          		where io_size = convert(varchar(8), @j)
          		and name = @CacheName))
		begin /* { */
			if (@reco_hdr_prn = 0)
			begin /* { */
				print @recotxt
				print @recoline
				select @reco_hdr_prn = 1
			end /* } */
			/*
			** If there are no grabs for this buffer pool
			** consider removing this buffer pool.
			*/
                	select @rptline = "    - Consider removing the "+ltrim(str(@j,3))+"k pool for this cache."
			print @rptline
			print @blankline
		end /* } */

		/* recommendations for cache splitting  */
	
		/*
		** If the number of engines is > 1 
		** and if the contention on the buffer
		** manager spinlock is > 10%
		** consider using cache partitions or named caches
		** or both
		** Also there are potential conditions where the waits or
		** grabs might go negative because of the counter overflowing
		** what an integer can hold; Cover for those cases as well.
		*/	
		if (@NumEngines > 1 and (@spinlock_contention >= 10  or
					@spinlock_contention < 0))
		 begin /* { */
                	if (@reco_hdr_prn = 0)
                	begin /* { */
                    		print @recotxt
				print @recoline
                     		select @reco_hdr_prn = 1
               		end /* } */
            		print "    - Consider using Named Caches or Cache partitions or both."
			print @blankline
		 end /* } */
	end /* } */

	fetch cache_info into @CacheID, @CacheName, @tmp_grp, @c_type, @cfg_repl, @run_repl
  end  /* } while @@sqlstatus */

close cache_info
deallocate cursor cache_info

return 0
go

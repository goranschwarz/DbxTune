use sybsystemprocs
go
IF EXISTS (SELECT 1 FROM sysobjects
           WHERE name = 'sp_sysmon_dcache_sum'
             AND id = object_id('sp_sysmon_dcache_sum')
             AND type = 'P')
	DROP PROCEDURE sp_sysmon_dcache_sum
go

create or replace procedure sp_sysmon_dcache_sum
        @NumEngines smallint,    /* number of engines online */
        @NumElapsedMs int,      /* for "per Elapsed second" calculations */
        @NumXacts int,          /* for per transactions calculations */
        @Reco   char(1),        /* Flag for recommendations             */
	@instid smallint = NULL	/* optional SDC instance id */

as

/* --------- declare local variables --------- */
declare @TotalSearches bigint	/* Total Cache Searches on All Caches */
declare @j smallint		/* loop index to iterate through multi-counter 
				** counters (pool...) */
declare @tmp_cntr varchar(35)	/* temp var for build field_name's 
				** ie. bufgrab_Nk */
declare @tmp_int bigint		/* temp var for integer storage */
declare @tmp_int2 int		/* temp var for integer storage */
declare @tmp_int3 int           /* temp var for integer storage used to read 
                                ** value of counter 'prefetch_kept_bp' */
declare @tmp_int4 int           /* temp var for integer storage used to read
                                ** value of counter 'prefetch_cached_bp' */
declare @tmp_int_sum int        /* temp var for integer storage
                                ** @tmp_int_sum = @tmp_int3 + @tmp_int4 */
declare @tmp_total bigint	/* temp var for summing 'total #s' data */
declare @tmp_float float        /* temp var for float storage */
declare @tmp_float2 float       /* temp var for float storage */
declare @sum2line char(67)	/* string to delimit total lines with 
				** percent calc on printout */
declare @numKBperpg int		/* number of kilobytes per logical page */
declare @blankline char(1)	/* to print blank line */
declare @psign char(3)		/* hold a percent sign (%) for print out */
declare @na_str char(3)		/* holds 'n/a' for 'not applicable' strings */
declare @zero_str char(80)	/* hold an output string for zero "  0.0" for 
				** printing zero "% of total" */
declare @rptline char(80)	/* formatted stats line for print statement */
declare @section char(80)	/* string to delimit sections on printout */

/* ------------- Variables for Tuning Recommendations ------------*/
declare @recotxt char(80)
declare @recoline char(80)
declare @reco_hdr_prn bit
declare	@ecache_size	int	/* configured size of extended cache */
declare @ecache_searches int	/* total number of searches in ecache */

/* --------- Setup Environment --------- */
set nocount on			/* disable row counts being sent to client */

select @sum2line   = "  -------------------------  ------------  ------------  ----------"
select @blankline  = " "
select @psign      = " %%"		/* extra % symbol because '%' is escape char in print statement */
select @na_str     = "n/a"
select @zero_str   = "                                      0.0           0.0           0       n/a"
select @section = "==============================================================================="

print @section
print @blankline
print "Data Cache Management"
print "---------------------"
print @blankline
print "  Cache Statistics Summary (All Caches)"
print "  -------------------------------------"
print "                                  per sec      per xact       count  %% of total"
print "                             ------------  ------------  ----------  ----------"
print @blankline
print "    Cache Search Summary"

/*
** get total cache searches on all caches 
*/
select @TotalSearches = SUM(convert(bigint, value))	
  from #tempmonitors
  where group_name like "buffer_%" and
		field_name = "bufsearch_calls" 

select @tmp_int = SUM(convert(bigint, value))		/* get cache hits on all caches */
  from #tempmonitors
  where group_name like "buffer_%" and
		field_name = "bufsearch_finds"

select @tmp_int2 = @TotalSearches - @tmp_int  /* calc total cache misses */

/* Initilize some variables to avoid divide by zero error */
if @NumElapsedMs = 0
begin
	select @NumElapsedMs = 1
end

if @NumXacts = 0
begin
	select @NumXacts = 1
end

if @TotalSearches = 0			/* Avoid Divide by Zero Errors */
	print @zero_str
else
  begin

	select @rptline = "      Total Cache Hits" + space(7) +
			str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
			space(2) +
			str(@tmp_int / convert(real, @NumXacts),12,1) +
			space(1) +
			str(@tmp_int, 11) + space(5) +
			str(100.0 * @tmp_int / @TotalSearches,5,1) + 
			@psign
	print @rptline

	select @rptline="      Total Cache Misses" + space(5) +
			str(@tmp_int2 / (@NumElapsedMs / 1000.0),12,1) +
			space(2) +
			str(@tmp_int2 / convert(real, @NumXacts),12,1) +
			space(1) +
			str(@tmp_int2, 11) + space(5) +
			str(100.0 * @tmp_int2 / @TotalSearches,5,1) + 
			@psign
	print @rptline

	print @sum2line 
	select @rptline = "    Total Cache Searches" + space(5) +
			str(@TotalSearches / (@NumElapsedMs / 1000.0),12,1) + 
			space(2) +
			str(@TotalSearches / convert(real, @NumXacts),12,1) + 
			space(2) +
			str(@TotalSearches, 10)
	print @rptline
  end

/* Print extended cache statistics if extended cache is configured */
if (@instid is NULL)
	select @ecache_size = value
		from master.dbo.sysconfigures
		where comment = 'extended cache size'
else
begin
	select @ecache_size = value
		from master.dbo.sysconfigures
		where comment = 'extended cache size'


	if (@ecache_size is NULL)
		select @ecache_size = value
			from master.dbo.sysconfigures
			where comment = 'extended cache size'

end

if @ecache_size > 0
begin /* { */
	print @blankline
	print "    Secondary Cache Search Summary"

	/*
	** Ecache hit % = (ecache_read / ecache_srchcalls) * 100
	*/

	/* extended cache search calls */	
	select @ecache_searches = value
	  from #tempmonitors
	  where group_name = "ecache" and
			field_name = "ecache_srchcalls"

	/* extended cache search hits */	
	select @tmp_int = value
	  from #tempmonitors
	  where group_name = "ecache" and
			field_name = "ecache_read"

	/* extended cache misses */	
	select @tmp_int2 = @ecache_searches - @tmp_int

	if @ecache_searches = 0		/* Avoid Divide by Zero Errors */
		print @zero_str
	else
	  begin
		select @rptline="      Total Cache Hits" + space(7) +
				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
				space(2) +
				str(@tmp_int / convert(real, @NumXacts),12,1) +
				space(1) +
				str(@tmp_int, 11) + space(5) +
				str(100.0 * @tmp_int / @ecache_searches,5,1) + 
				@psign
		print @rptline

		select @rptline="      Total Cache Misses" + space(5) +
				str(@tmp_int2 / (@NumElapsedMs / 1000.0),12,1) +
				space(2) +
				str(@tmp_int2 / convert(real, @NumXacts),12,1) +
				space(1) +
				str(@tmp_int2, 11) + space(5) +
				str(100.0 * @tmp_int2 / @ecache_searches,5,1) + 
				@psign
		print @rptline

		print @sum2line 
		select @rptline="    Total Cache Searches" + space(5) +
				str(@ecache_searches / (@NumElapsedMs / 1000.0),12,1) + 
				space(2) +
				str(@ecache_searches / convert(real, @NumXacts),12,1) + 
				space(2) +
				str(@ecache_searches, 10)
		print @rptline
	  end
end /* } */
print @blankline

print "    Cache Turnover"

select @tmp_total = SUM(convert(bigint, value))
  from #tempmonitors
  where group_name like "buffer_%" and
		field_name like "bufgrab_%k" and
		field_name not like "bufgrab_ref%k"

select @rptline = "      Buffers Grabbed" + space(8) +
			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
			space(2) +
			str(@tmp_total / convert(real, @NumXacts),12,1) + 
			space(1) +
			str(@tmp_total, 11) + space(7) +
			@na_str
print @rptline

if @tmp_total != 0			/* Avoid Divide by Zero Errors */
  begin

	select @tmp_int = SUM(convert(bigint, value))
	  from #tempmonitors
	  where group_name like "buffer_%" and
			field_name like "bufgrab_dirty_%"

	select @rptline = "      Buffers Grabbed Dirty" + space(2) +
				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
				space(2) +
				str(@tmp_int / convert(real, @NumXacts),12,1) +
				space(1) +
				str(@tmp_int, 11) + space(5) +
				str(100.0 * @tmp_int / @tmp_total,5,1) + @psign
	print @rptline
  end
print @blankline

print "    Cache Strategy Summary"

/*
** Sum all buf unkeeps to look at % of buffers following 
** MRU vs Discard Strategy 
*/

select @tmp_total = SUM(convert(bigint, value))
  from #tempmonitors
  where group_name like "buffer_%" and
		field_name IN ("bufunkeep_lru", "bufunkeep_mru")

if @tmp_total = 0			/* Avoid Divide by Zero Errors */
	print @zero_str
else
  begin

	select @tmp_int = SUM(convert(bigint, value))
	  from #tempmonitors
	  where group_name like "buffer_%" and
			field_name = "bufunkeep_lru"

	select @rptline = "      Cached (LRU) Buffers" + space(3) +
				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
				space(2) +
				str(@tmp_int / convert(real, @NumXacts),12,1) +
				space(1) +
				str(@tmp_int, 11) + space(5) +
				str(100.0 * @tmp_int / @tmp_total,5,1) + 
				@psign
	print @rptline

	select @tmp_int = SUM(convert(bigint, value))
	  from #tempmonitors
	  where group_name like "buffer_%" and
			field_name = "bufunkeep_mru"

	select @rptline = "      Discarded (MRU) Buffers" +
				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
				space(2) +
				str(@tmp_int / convert(real, @NumXacts),12,1) +
				space(1) +
				str(@tmp_int, 11) + space(5) +
				str(100.0 * @tmp_int / @tmp_total,5,1) + 
				@psign
	print @rptline
  end
print @blankline

print "    Large I/O Usage"

select @tmp_total = SUM(convert(bigint, value))
  from #tempmonitors
  where group_name like "buffer_%" and
		field_name = "prefetch_req"

if @tmp_total = 0			/* Avoid Divide by Zero Errors */
	print @zero_str
else
  begin

	select @tmp_int = SUM(convert(bigint, value))
	  from #tempmonitors
	  where group_name like "buffer_%" and field_name IN
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

	select @tmp_int = SUM(convert(bigint, value))
	  from #tempmonitors
	  where group_name like "buffer_%" and
		field_name = "prefetch_decrease"

        select @tmp_int3 = SUM(convert(bigint, value))
          from #tempmonitors
          where group_name like "buffer_%" and
                field_name = "prefetch_kept_bp"

        select @tmp_int4 = SUM(convert(bigint, value))
          from #tempmonitors
                where group_name like "buffer_%" and
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
					@tmp_int_sum / 	
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
  end

print @blankline

print "    Large I/O Effectiveness"

/*
**  calc total # of (logical) pages brought into all caches from all 
**  large I/Os ( > logical pagesize)
*/

/*
**  init loop var's to loop through all possible pool sizes 
*/
select @numKBperpg = @@maxpagesize/1024
select @tmp_total = 0, @j = 2*@numKBperpg

while (@j <= 8*@numKBperpg)
  begin

	/* build pool specific counter name, bufgrab_Nk (ie bufgrab_16k) */
	select @tmp_cntr = "bufgrab_" + convert(varchar(3), @j) + "k"

	select @tmp_total = @tmp_total + (SUM(convert(bigint, value)) * (@j / @numKBperpg))
	  from #tempmonitors
	  where group_name like "buffer_%" and
			field_name = @tmp_cntr

	select @j = @j * 2
  end
  
select @rptline = "      Pages by Lrg I/O Cached" +
			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
			space(2) +
			str(@tmp_total / convert(real, @NumXacts),12,1) + 
			space(1) +
			str(@tmp_total,11) + space(7) +
			@na_str
print @rptline

select @tmp_cntr = "bufgrab_ref_" + convert(varchar(3), @numKBperpg) + "K"

if @tmp_total != 0	/* Avoid Divide by Zero Errors after printout */
  begin
	select @tmp_int = SUM(convert(bigint, value))
	  from #tempmonitors
	  where group_name like "buffer_%" and
			field_name like "bufgrab_ref_%K" and
			field_name != @tmp_cntr

	select @rptline = "      Pages by Lrg I/O Used" + space(2) +
				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
				space(2) +
				str(@tmp_int / convert(real, @NumXacts),12,1) +
				space(1) +
				str(@tmp_int, 11) + space(5) +
				str(100.0 * @tmp_int / @tmp_total,5,1) + 
				@psign
	print @rptline
  end
print @blankline


if exists(select *
                from #tempmonitors
                where group_name = "access" and
                field_name like "apf%")
begin /*{*/


		print "    Asynchronous Prefetch Activity"

		select @tmp_total= SUM(convert(bigint, value))
		  from #tempmonitors
		  where group_name = "access" and
				field_name in ("apf_IOs_issued", "apf_could_not_start_IO_immediately",
						"apf_configured_limit_exceeded", "apf_unused_read_penalty",
						"apf_found_in_cache_with_spinlock", "apf_found_in_cache_wo_spinlock")

		if @tmp_total = 0                       /* Avoid Divide by Zero Errors */
			print @zero_str
		else
		  begin
			select @tmp_int = value
			  from #tempmonitors
			  where group_name = "access"
			  and field_name = "apf_IOs_issued"

			select @rptline = "      APFs Issued" + space(12) +
						str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
						space(2) +
						str(@tmp_int / convert(real, @NumXacts),12,1) +
						space(1) +
						str(@tmp_int, 11) + space(5) +
						str(100.0 * @tmp_int / @tmp_total,5,1) +
						@psign
			print @rptline

			select @rptline = "      APFs Denied Due To" 
			print @rptline


			select @tmp_int = value
			  from #tempmonitors
			  where group_name = "access"
			  and field_name = "apf_could_not_start_IO_immediately"
			
			select @rptline = "        APF I/O Overloads " + space(3) +
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
			  where group_name = "access"
			  and field_name = "apf_configured_limit_exceeded"

			select @rptline = "        APF Limit Overloads " + space(1) +
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
			  where group_name = "access"
			  and field_name = "apf_unused_read_penalty"
			
			select @rptline = "        APF Reused Overloads " + 
					       str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
					       space(2) +
					       str(@tmp_int / convert(real, @NumXacts),12,1) +
					       space(1) +
					       str(@tmp_int, 11) + space(5) +
					       str(100.0 * @tmp_int / @tmp_total,5,1) +
					       @psign
			print @rptline

			select @rptline = "      APF Buffers Found in Cache" 
			print @rptline

			select @tmp_int = value
			  from #tempmonitors
			  where group_name = "access"
			  and field_name = "apf_found_in_cache_with_spinlock"
			
			select @rptline = "        With Spinlock Held" + space(3) +
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
			  where group_name = "access"
			  and field_name = "apf_found_in_cache_wo_spinlock"
			
			select @rptline = "        W/o Spinlock Held " + space(3) +
					       str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
					       space(2) +
					       str(@tmp_int / convert(real, @NumXacts),12,1) +
					       space(1) +
					       str(@tmp_int, 11) + space(5) +
					       str(100.0 * @tmp_int / @tmp_total,5,1) +
					       @psign
			print @rptline

			print @sum2line
			select @rptline = "    Total APFs Requested " + space(4) +
					str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) +
					space(2) +
					str(@tmp_total / convert(real, @NumXacts),12,1) +
					space(2) +
					str(@tmp_total,10)
			print @rptline
		  end

		print @blankline

		print "    Other Asynchronous Prefetch Statistics"

		select @tmp_int = value
			  from #tempmonitors
			  where group_name = "access"
			  and field_name = "apf_IOs_used"

		select @rptline = "      APFs Used" + space(14) +
						str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
						space(2) +
						str(@tmp_int / convert(real, @NumXacts),12,1) +
						space(1) +
						str(@tmp_int, 11) + space(7) +
						@na_str
		print @rptline

		select @tmp_int = value
			from #tempmonitors
			where group_name = "access"
			and field_name = "apf_waited_for_IO_to_complete"

		select @rptline = "      APF Waits for I/O" + space(6) +
			       str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
			       space(2) +
			       str(@tmp_int / convert(real, @NumXacts),12,1) +
			       space(1) +
			       str(@tmp_int, 11) + space(7) +
			       @na_str
		print @rptline

		select @tmp_int = SUM(convert(bigint, value))
			from #tempmonitors
			where field_name like "apf%discard%"

		select @rptline = "      APF Discards" + space(11) +
			       str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
			       space(2) +
			       str(@tmp_int / convert(real, @NumXacts),12,1) +
			       space(1) +
			       str(@tmp_int, 11) + space(7) +
			       @na_str
		print @rptline
end /*}*/
print @blankline


print "    Dirty Read Behavior"
	
select @tmp_total = SUM(convert(bigint, value))
  from #tempmonitors
  where group_name like "buffer_%" and
		field_name = "level0_bufpredirty"

select @rptline = "      Page Requests" + space(10) +
			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
			space(2) +
			str(@tmp_total / convert(real, @NumXacts),12,1) + 
			space(1) +
			str(@tmp_total,11) + space(7) +
			@na_str
print @rptline

if @tmp_total != 0	/* Avoid Divide by Zero Errors after printout */
  begin

	select @tmp_int = value
	  from #tempmonitors
	  where group_name = "access" and
			field_name = "dirty_read_restarts"

	if @tmp_int != 0
	 begin
		select @rptline = "      Re-Starts" + space(10) +
				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
				space(2) +
				str(@tmp_int / convert(real, @NumXacts),12,1) +
				space(1) +
				str(@tmp_int, 11) + space(5) +
				str(100.0 * @tmp_int / @tmp_total,5,1) + 
				@psign
		print @rptline
	 end
  end
print @blankline

/*
** If requested, print global cache recommendations (if any)
*/

if @Reco = 'Y'
 begin
	select @recotxt =     "  Tuning Recommendations for All Caches"
	select @recoline = "  -------------------------------------"
        select @reco_hdr_prn = 0
        /* recommendations for apf */
        select @tmp_float = convert(int, (100.0*a.value/b.value))
        	from #tempmonitors a, #tempmonitors b
        	where   a.group_name = "access"
        	and     a.field_name = "apf_IOs_issued"
        	and     b.group_name = "access"
        	and     b.field_name in ("apf_IOs_issued", "apf_could_not_start_IO_immediately",
                                        "apf_configured_limit_exceeded", "apf_unused_read_penalty",
                                        "apf_found_in_cache_with_spinlock", "apf_found_in_cache_wo_spinlock")
		and 	b.value != 0 
	 if (@tmp_float is not null)
	 begin
        	select @tmp_int = value
        	from #tempmonitors
        	where   group_name = "access"
        	and     field_name = "apf_configured_limit_exceeded"

		/*
		** If the number of APF I/O's issued is greater
		** than 80% and if the APF configured limit 
		** exceeded 0 during the sampling interval
		** consider increasing the apf limits for the
		** pool or globally for all the pools
		*/
        	if (@tmp_float > 80.0 and @tmp_int > 0)
         	begin
               	 	if (@reco_hdr_prn = 0)
               	 	begin
                       		 print @recotxt
                       		 print @recoline
                       	 	 select @reco_hdr_prn = 1
                	end
                	print "  - Consider increasing the 'global asynchronous prefetch limit' parameter"
                	print "    (and the 'local asynchronous prefetch limit' parameter for each pool " 
                	print "    for which this was overidden) by 10%%." 
                	print @blankline
         	end
 	 end
 end

return 0
go

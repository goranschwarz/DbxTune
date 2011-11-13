use sybsystemprocs
go
set nocount on
go

-------------------------------------------------------------
--- NOTE -- NOTE -- NOTE -- NOTE -- NOTE -- NOTE -- NOTE ---- 
-------------------------------------------------------------
--
-- If you change anything in here, do NOT forget to change
-- the field 'SP__OPTDIAG_CR_STR' in 'com.asetune.cm.sql.VersionInfo'
--
-- Otherwise it will NOT be recreated... when asetune starts...
--
-------------------------------------------------------------







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
-- Procedure: sp__optdiag
-------------------------------------------------------------
if (select object_id('sp__optdiag')) is not null
begin
	drop procedure sp__optdiag
	print "Dropping procedure sp__optdiag"
end
go

declare @dbname varchar(255)
select @dbname = db_name()
print "Creating procedure '%1!.%2!.%3!'.", @dbname, "dbo", "sp__optdiag"
go

-----------------------------------------------------------------
-- After this line nothing has been changed from the files
-- downloaded at: http://www.teamsybase.net/kevin.sherlock/sps/mine/
-- also att the end, added some grant
-----------------------------------------------------------------
go
create procedure sp__optdiag
        @tabname        varchar(62) = null,  /* user table name */
        @colname        varchar(30) = null,     /* column name */
        @option         varchar(60) = null              /* output format */
 , @proc_version varchar(78) = "sp__optdiag/1.9.4/0/P/KJS/AnyPlat/AnyOS/G/Mon Feb 02 14:48:15 2004"

as
/*************************************************************************************************
**
**      Description: Format opdiag info from stored procedure
**
**      Options:  NULL - default
**
**                "V/?/HELP/H" - will print the current version string of this proc
**
**      Future Info:  Other options can be added in the future
**               using the @option parameter.  I'm thinking of putting in "simulate" functionality.
**
**      Dependencies:  This proc relies on the object_id built-in
**                    and sp_namecrack
**
**      Errors:
**
**      Version:  This proc is for ASE 11.9.x and beyond
**
**      Usage:  exec <dbname>..sp__optdiag <tabname>, <colname>, <opt>
**
**      Author:  Kevin Sherlock - original version.  email:ksherlo@qwest.com
**               For distribution to newsgroups, list-server, et. al.
**
** Disclaimer:
**      - Please note this is not intended as a "production reliable" tool, but rather as a
**        development tool or reference utility.
**      - This stored procedure has been tested on ASE versions 11.9.2 through 12.5.0.3, but only
**        on HP-UX, Sun, and NT.
**      - There may a performance impact when running this procedure, because it uses a #temp
**        table.  It may not be a good idea to run this on columns that have thousands of cells
**        that don't have enough capacity in tempdb. A future version of this procedure may
**        eliminate the need for the #temp table.
**
**      History: 10/31/2000 (ksherlock) 0.1
**                  Original
**               11/14/2000 (ksherlock) 0.2
**                  Fixed bug to handle binary histograms and handle user defined types
**               12/20/2000 (ksherlock) 0.3
**                  Fixed bug with column groups not being retrieved in col_cursor
**               01/05/2001 (ksherlock) 0.4
**                  Bug fix version which handles numeric decimals correctly
**               03/02/2001 (ksherlock) 0.5
**                  Fixed to be compatible with ASE 12.5 where syscolumns.colid is 2-bytes (smallint)
**                  Fixed minor bug in printing histograms of 10 digit negative INT values
**               05/11/2001 (ksherlock) 0.6
**                  Allows system tables as arguments
**               08/31/2001 (ksherlock) 0.7
**                  - Fixed bug related to creating the "column group" text using the length of colid
**               10/01/2001 (ksherlock) 0.8
**                  - Compatible with "little-endian" systems
**               01/11/2002 (ksherlock) 0.9
**                  - Adds optdiag 12.0 like output with two new derived values for
**                                "Space utilization" and "Large io efficiency"
**                                "Large io efficiency" calculation is not available yet however
**                  - Adds support for Larger Page sizes (available starting with ASE 12.5)
**                  - Cleaned up the histogram cursor code to take up less space
**                    (and be more readable?) :)
**                  - Added disclaimers to comments in header
**               02/27/2002 (ksherlock) 0.9.1
**                  - Fixed bug when processing "sysgams" table, which also affected null tablename
**                    option to produce output for all tables in a database
**                  - When specifying NULL for tablename, only produce stats for user tables
**                    note: Previous functionality can be obtained by using a wildcard for tablename
**                    IE: sp__optdiag null - will print out all user tables only
**                        sp__optdiag "%"  - will print out all user and system tables
**               07/10/2002 (ksherlock) 0.9.2
**                  - Fixed bug in cursor nostats_cursor to accomodate "little-endian"
**                    systems.
**                  - Fixed bug in "column group" SQL to better and more accurately walk
**                    the colidarray structure to produce a column group listing.
**
**               06/17/2003 (ksherlock) 0.9.3
**                  - Checks for "read_only" database before attempting to flushstats
**                  - Bug fix to handle negative numerics in histogram
**
**               03/27/2003 (ksherlock) 1.9.3
**                  - Added functionality to use derived_stat() function, for all cluster ratios,
**                    Large io efficiency, and Space Utilization calculations.  For ASE 12.5.0.3
**                    and up!
**
**               02/02/2004 (ksherlock) 1.9.4
**                  - Compatibility with ASE 12.5 new datatypes "date" and "time"
**
*************************************************************************************************/

declare
        @colid int  /* Variable to hold colid from syscolumns, int will allow up to 4 byte colids */
      , @tabid int  /* Variable to hold object_id from sysobjects */
      , @tabtype char(2)  /* Variable to hold type from sysobjects */
      , @s_dbname varchar(30)
      , @s_tabowner varchar(30)
      , @s_tabname varchar(30)
      , @u_tabname varchar(30)
      , @u_tabowner varchar(30)
      , @colgroup_name varchar(255)
      , @u_dbname varchar(30)
      , @u_dbid int
      , @colidarray varbinary(200)
      , @colidarray_len smallint
      , @indid int
      , @index_cols varchar(254)
      , @index_name varchar(30)
      , @keycnt int
      , @dol_clustered int
      , @clustered int
      , @last_updt varchar(28)
      , @c1stat int
      , @statid smallint
      , @used_count int
      , @rownum int
      , @coltype int
      , @typename varchar(30)
      , @collength varchar(10)
      , @precision varchar(3)
      , @scale varchar(3)
      , @rc_density varchar(24)
      , @tot_density varchar(24)
      , @r_sel varchar(24)
      , @between_sel varchar(24)
      , @freq_cell smallint
      , @steps_act int
      , @steps_req int
      , @step char(9)
      , @weight char(10)
      , @prev_step char(9)
      , @prev_weight char(10)
      , @value_raw varbinary(255)
      , @value_c varchar(255)
      , @leafcnt varchar(32) -- int
      , @pagecnt varchar(32) -- int
      , @emptypgcnt varchar(32) -- int
      , @rowcnt varchar(32)
      , @forwrowcnt varchar(32)
      , @delrowcnt varchar(32)
      , @dpagecrcnt varchar(32)
      , @dpagecr varchar(32)
      , @ipagecrcnt varchar(32)
      , @ipagecr varchar(32)
      , @drowcrcnt varchar(32)
      , @drowcr varchar(32)
      , @oamapgcnt varchar(32) -- int
      , @extent0pgcnt varchar(32)
      , @datarowsize varchar(32)
      , @leafrowsize varchar(32)
      , @indexheight varchar(32) -- int
      , @spare1 varchar(32) -- int
      , @spare2 varchar(32)
      , @spc_utl varchar(32)
      , @dpcr varchar(32)
      , @ipcr varchar(32)
      , @drcr varchar(32)
      , @lrgioeff varchar(32)
      , @ptn_data_pgs int
      , @seq int
      , @colid_len tinyint
      , @big_endian tinyint
      , @pg_hdr tinyint

if @@trancount = 0
begin
        set chained off
end

set transaction isolation level 1
set nocount on
set flushmessage on

if ( (select lower(@option)) in ("v","version","?","h","help") )
begin
   print "%1!",@proc_version
   return 0
end

exec sp_namecrack @tabname, " ", @s_dbname out, @s_tabowner out, @s_tabname out
select @s_dbname = isnull(@s_dbname,db_name())
        /* 0.5, determine length of syscolumns.colid which changed in 12.5 */
      ,@colid_len = col_length('syscolumns','colid')
        /* 0.8, are we big-endian or little-endian? Yes it matters :) */
      ,@big_endian = sign(convert(tinyint,substring(convert(varbinary,convert(smallint,255)),2,1)))

declare object_cursor cursor for
select  id,
        db_name(),
        db_id(),
        user_name(uid),
        name,
        /* 0.9, pg_hdr = 32 for APL, 44 plus 2 bytes for timestamp for DOL */
        pg_hdr = 32 +  ( abs(sign(sysstat2 & 49152)) * (12 + 2) )
from sysobjects
where user_name(uid) like isnull(@s_tabowner,"%")
and   name like isnull(@s_tabname,"%")
and type in ("U", case
                  when @s_tabname is null then null
                  else "S"
                  end )
order by user_name(uid), name
for read only

declare index_cursor cursor for
select  st.indid
      , si.name
      , abs(sign(si.status2 & 512)) /* DOL clustered index */
      , abs(sign(si.status & 16)) /* clustered bit */
      , si.keycnt
from systabstats st, sysindexes si
where st.id = @tabid
  and si.id = @tabid
  and st.id = si.id
  and st.indid = si.indid
order by st.indid
for read only

declare col_cursor cursor for
select  sc.colid,
        ss.colidarray,
        datalength(ss.colidarray),
        sc.name,
        ss.statid,
        convert(int,ss.c1),
        convert(varchar,ss.moddate,109),
        ltrim(str(round(convert(double precision,ss.c2),16),24,16)),
        ltrim(str(round(convert(double precision,ss.c3),16),24,16)),
        convert(int,ss.c4),
        convert(int,ss.c5),
        st.name,
        ltrim(str(convert(int,ss.c7),5)),
        ltrim(str(convert(int,ss.c8),3)),
        ltrim(str(convert(int,ss.c9),3)),
        ltrim(str(round(convert(double precision,ss.c10),16),24,16)),
        ltrim(str(round(convert(double precision,ss.c11),16),24,16))
from syscolumns sc, sysstatistics ss, systypes st
where sc.id = @tabid
and   sc.name like isnull(@colname,"%")
and   ss.id = sc.id
and   convert(int,ss.c6) *= st.type
and   st.name not in ("timestamp","sysname", "nchar", "nvarchar")
and   st.usertype < 100
and   substring(ss.colidarray,1,@colid_len) = convert(varbinary,sc.colid)
and   ss.formatid = 100
order by sc.id, sc.name, ss.colidarray
for read only

declare nostats_cursor cursor for
select sc.name
from syscolumns sc,
 sysstatistics ss
where ss.id =* sc.id
and  sc.id = @tabid
and  ss.formatid = 100
and  ss.statid = 0
and  ss.sequence = 1
and  convert(varbinary,sc.colid) *= ss.colidarray
and  datalength(ss.colidarray) <= @colid_len
group by sc.name
having count(ss.id) = 0
order by sc.name
for read only

create table #cells(seq int,colnum int)

/** DO NOT FOLD, SPINDAL, OR MUTILATE (unless its sysstatistics) **/
/** OK, bear with me, here we go... **/

declare histogram_cursor cursor for
select
 /** Here is the step number **/
 str( ((c.seq-1)*80 + c.colnum ) ,9),

 /** And here is the Weight of the cell **/
 str( isnull(convert(real, case (c.colnum - 1)
      when 0 then s.c0 when 1 then s.c1 when 2 then s.c2 when 3 then s.c3
      when 4 then s.c4 when 5 then s.c5 when 6 then s.c6 when 7 then s.c7
      when 8 then s.c8 when 9 then s.c9 when 10 then s.c10 when 11 then s.c11
      when 12 then s.c12 when 13 then s.c13 when 14 then s.c14 when 15 then s.c15
      when 16 then s.c16 when 17 then s.c17 when 18 then s.c18 when 19 then s.c19
      when 20 then s.c20 when 21 then s.c21 when 22 then s.c22 when 23 then s.c23
      when 24 then s.c24 when 25 then s.c25 when 26 then s.c26 when 27 then s.c27
      when 28 then s.c28 when 29 then s.c29 when 30 then s.c30 when 31 then s.c31
      when 32 then s.c32 when 33 then s.c33 when 34 then s.c34 when 35 then s.c35
      when 36 then s.c36 when 37 then s.c37 when 38 then s.c38 when 39 then s.c39
      when 40 then s.c40 when 41 then s.c41 when 42 then s.c42 when 43 then s.c43
      when 44 then s.c44 when 45 then s.c45 when 46 then s.c46 when 47 then s.c47
      when 48 then s.c48 when 49 then s.c49 when 50 then s.c50 when 51 then s.c51
      when 52 then s.c52 when 53 then s.c53 when 54 then s.c54 when 55 then s.c55
      when 56 then s.c56 when 57 then s.c57 when 58 then s.c58 when 59 then s.c59
      when 60 then s.c60 when 61 then s.c61 when 62 then s.c62 when 63 then s.c63
      when 64 then s.c64 when 65 then s.c65 when 66 then s.c66 when 67 then s.c67
      when 68 then s.c68 when 69 then s.c69 when 70 then s.c70 when 71 then s.c71
      when 72 then s.c72 when 73 then s.c73 when 74 then s.c74 when 75 then s.c75
      when 76 then s.c76 when 77 then s.c77 when 78 then s.c78 when 79 then s.c79
      else convert(varbinary(255),0)
      end /* case */
                     ),0) /* convert(real, isnull( */
     ,10,8) /* str( */,

 /** And finally, here is the Value of the cell **/
 convert(varbinary(255), case (c.colnum - 1)
      when 0 then v.c0 when 1 then v.c1 when 2 then v.c2 when 3 then v.c3
      when 4 then v.c4 when 5 then v.c5 when 6 then v.c6 when 7 then v.c7
      when 8 then v.c8 when 9 then v.c9 when 10 then v.c10 when 11 then v.c11
      when 12 then v.c12 when 13 then v.c13 when 14 then v.c14 when 15 then v.c15
      when 16 then v.c16 when 17 then v.c17 when 18 then v.c18 when 19 then v.c19
      when 20 then v.c20 when 21 then v.c21 when 22 then v.c22 when 23 then v.c23
      when 24 then v.c24 when 25 then v.c25 when 26 then v.c26 when 27 then v.c27
      when 28 then v.c28 when 29 then v.c29 when 30 then v.c30 when 31 then v.c31
      when 32 then v.c32 when 33 then v.c33 when 34 then v.c34 when 35 then v.c35
      when 36 then v.c36 when 37 then v.c37 when 38 then v.c38 when 39 then v.c39
      when 40 then v.c40 when 41 then v.c41 when 42 then v.c42 when 43 then v.c43
      when 44 then v.c44 when 45 then v.c45 when 46 then v.c46 when 47 then v.c47
      when 48 then v.c48 when 49 then v.c49 when 50 then v.c50 when 51 then v.c51
      when 52 then v.c52 when 53 then v.c53 when 54 then v.c54 when 55 then v.c55
      when 56 then v.c56 when 57 then v.c57 when 58 then v.c58 when 59 then v.c59
      when 60 then v.c60 when 61 then v.c61 when 62 then v.c62 when 63 then v.c63
      when 64 then v.c64 when 65 then v.c65 when 66 then v.c66 when 67 then v.c67
      when 68 then v.c68 when 69 then v.c69 when 70 then v.c70 when 71 then v.c71
      when 72 then v.c72 when 73 then v.c73 when 74 then v.c74 when 75 then v.c75
      when 76 then v.c76 when 77 then v.c77 when 78 then v.c78 when 79 then v.c79
      end
    ) /* convert */

from #cells c, sysstatistics s, sysstatistics v
where s.id = @tabid
--- Here, we have to account for either 1byte or 2byte colids
and s.colidarray = substring(convert(varbinary,convert(int,@colid)),(@big_endian * (4 - @colid_len)) + 1,@colid_len)
and s.formatid = 104
and v.id =* s.id
and v.colidarray =* s.colidarray
and v.statid =* s.statid
and v.sequence =* s.sequence
and v.formatid = 102
and c.seq = s.sequence
for read only

/** Wow, I'm glad that's over **/
/** Let's get on with the business at hand **/

print "%1!",@proc_version
print "%1!",@@version
print ''

/** Standard optdiag output **/
begin
   print 'Server name:                            "%1!"',@@servername
   print ''
   print 'Specified database:                     "%1!"',@s_dbname
   if (@s_tabowner is null)
     print 'Specified table owner:                  not specified'
   else
     print 'Specified table owner:                  "%1!"',@s_tabowner
   if (@s_tabname is null)
     print 'Specified table:                        not specified'
   else
     print 'Specified table:                        "%1!"',@s_tabname
   if (@colname is null)
     print 'Specified column:                       not specified'
   else
     print 'Specified column:                       "%1!"',@colname
   print ''

/*
** Check to see if the @tabname is in sysobjects.
*/

   open object_cursor

   fetch object_cursor into
      @tabid, @u_dbname, @u_dbid,
      @u_tabowner, @u_tabname, @pg_hdr

while (@@sqlstatus = 0)
begin
   print 'Table owner:                            "%1!"',@u_tabowner
   print 'Table name:                             "%1!"',@u_tabname
   print ''

   /* v0.9.3 - check for read_only database before flushstats */
   if not exists (
                  select 1
                  from master..sysdatabases d, master..spt_values v
                  where d.status & v.number = v.number
                  and   v.type = "D"
                  and v.name = "read only"
                  and d.dbid = @u_dbid
                 )
                 dbcc flushstats(@u_dbid, @tabid)

   select @ptn_data_pgs = convert(int, max(ptn_data_pgs(@tabid, partitionid)))
   from syspartitions
   where id = @tabid

   ---------------------
   -- Work on Indexes --
   ---------------------
   open index_cursor
   fetch index_cursor into
              @indid ,@index_name ,@dol_clustered, @clustered, @keycnt

   while (@@sqlstatus = 0)
   begin
      select @keycnt = @keycnt - isnull(abs(sign(@clustered - 1)),0)
            ,@index_cols = null
      while (@keycnt > 0)
      begin
         select @index_cols = substring(', ' ,abs(sign(@keycnt - 1)),2)
                            + '"' + index_col(@u_tabname, @indid, @keycnt, user_id(@u_tabowner)) + '"'
                            + @index_cols
         select @keycnt = @keycnt - 1
      end
      select @leafcnt = ltrim(convert(varchar(32),convert(int,i.leafcnt))),
             @pagecnt = ltrim(convert(varchar(32),convert(int,i.pagecnt))),
             @emptypgcnt = ltrim(convert(varchar(32),convert(int,i.emptypgcnt))),
             @rowcnt = ltrim(convert(varchar(32),str(round(convert(double precision,i.rowcnt),16),32,16))),
             @forwrowcnt = ltrim(convert(varchar(32),str(round(convert(double precision,i.forwrowcnt),16),32,16))),
             @delrowcnt = ltrim(convert(varchar(32),str(round(convert(double precision,i.delrowcnt),16),32,16))),
             @dpagecrcnt = ltrim(convert(varchar(32),str(round(convert(double precision,i.dpagecrcnt),16),32,16))),
             @dpagecr = ltrim(convert(varchar(32),str(round(convert(double precision,i.dpagecrcnt),16),32,16))),
             @ipagecrcnt = ltrim(convert(varchar(32),str(round(convert(double precision,i.ipagecrcnt),16),32,16))),
             @ipagecr = ltrim(convert(varchar(32),str(round(convert(double precision,i.ipagecrcnt),16),32,16))),
             @drowcrcnt = ltrim(convert(varchar(32),str(round(convert(double precision,i.drowcrcnt),16),32,16))),
             @drowcr = ltrim(convert(varchar(32),str(round(convert(double precision,i.drowcrcnt),16),32,16))),
             @oamapgcnt = ltrim(convert(varchar(32),convert(int,i.oamapgcnt))),
             @extent0pgcnt = ltrim(convert(varchar(32),convert(int,i.extent0pgcnt))),
             @datarowsize = ltrim(convert(varchar(32),str(round(convert(double precision,i.datarowsize),16),32,16))),
             @leafrowsize = ltrim(convert(varchar(32),str(round(convert(double precision,i.leafrowsize),16),32,16))),
             @indexheight = ltrim(convert(varchar(32),convert(smallint,i.indexheight))),
             @spare1 = ltrim(convert(varchar(32),convert(int,i.spare1))),
             @spare2 = ltrim(convert(varchar(32),str(round(convert(double precision,i.spare2),16),32,16))),
             @spc_utl = ltrim(convert(varchar(32),
               str(round(convert(double precision,derived_stat(i.id,i.indid,"space utilization")),16),32,16))),
             @dpcr = ltrim(convert(varchar(32),
               str(round(convert(double precision,derived_stat(i.id,i.indid,"data page cluster ratio")),16),32,16))),
             @ipcr = ltrim(convert(varchar(32),
               str(round(convert(double precision,derived_stat(i.id,i.indid,"index page cluster ratio")),16),32,16))),
             @drcr = ltrim(convert(varchar(32),
               str(round(convert(double precision,derived_stat(i.id,i.indid,"data row cluster ratio")),16),32,16))),
             @lrgioeff = ltrim(convert(varchar(32),
               str(round(convert(double precision,derived_stat(i.id,i.indid,"large io efficiency")),16),32,16)))
      from systabstats d,
           systabstats i,
           master..spt_values v
      where i.id = @tabid and i.indid = @indid
        and i.id = d.id
        and d.indid between 0 and 1
        and v.number = 1
        and v.type = "E"

      ----------------------
      -- print index info --
      ----------------------

      if (@indid = 0)
         print 'Statistics for table:                   "%1!"',@index_name
      else if (1 in (@clustered,@dol_clustered))
         print 'Statistics for index:                   "%1!" (clustered)',@index_name
      else
         print 'Statistics for index:                   "%1!" (nonclustered)',@index_name
      if (@indid > 0)
         print 'Index column list:                      %1!',@index_cols
      else
         print ''
      if (@clustered = 1 or @indid = 0)
         print '     Data page count:                   %1!',@pagecnt
      else
         print '     Leaf count:                        %1!',@leafcnt

      if (1 in (@clustered,@dol_clustered) or @indid = 0)
         print '     Empty data page count:             %1!',@emptypgcnt
      else
         print '     Empty leaf page count:             %1!',@emptypgcnt

      if (@clustered = 1 or @indid = 0)
      begin
         print '     Data row count:                    %1!',@rowcnt
         print '     Forwarded row count:               %1!',@forwrowcnt
         print '     Deleted row count:                 %1!',@delrowcnt
      end

      print '     Data page CR count:                %1!',@dpagecrcnt
      if ((@clustered = 0 or @dol_clustered = 1) and @indid > 0)
      begin
         print '     Index page CR count:               %1!',@ipagecrcnt
         print '     Data row CR count:                 %1!',@drowcrcnt
      end

      if (@clustered = 1 or @indid = 0)
         print '     OAM + allocation page count:       %1!',@oamapgcnt

      if (@indid = 0)
         print '     First extent data pages:           %1!',@extent0pgcnt
      else
         print '     First extent leaf pages:           %1!',@extent0pgcnt
      if (@clustered = 1 or @indid = 0)
         print '     Data row size:                     %1!',@datarowsize
      else
         print '     Leaf row size:                     %1!',@leafrowsize
      if (@indid > 0)
         print '     Index height:                      %1!',@indexheight
      if ((@clustered = 1 or @indid = 0) and @ptn_data_pgs is not null)
         print '     Pages in largest partition:        %1!',@ptn_data_pgs

      print ''
      print '  Derived statistics:'
      print '     Data page cluster ratio:           %1!',@dpcr
      if ((@clustered = 0 or @dol_clustered = 1) and @indid > 0)
      begin
         print '     Index page cluster ratio:          %1!',@ipcr
         print '     Data row cluster ratio:            %1!',@drcr
      end
/* Space utilization */
      print '     Space utilization:                 %1!',@spc_utl
/* Large IO efficiency */
      print '     Large I/O efficiency:              %1!',@lrgioeff
      print ''

      fetch index_cursor into
                 @indid ,@index_name ,@dol_clustered ,@clustered, @keycnt
   end
   close index_cursor

   ---------------------
   -- Work on Columns --
   ---------------------
      open col_cursor
      fetch col_cursor into
         @colid, @colidarray, @colidarray_len, @colname, @statid, @c1stat, @last_updt, @rc_density, @tot_density
        ,@steps_act, @steps_req, @typename, @collength, @precision, @scale, @r_sel, @between_sel

      while (@@sqlstatus = 0)
      begin
         if (@steps_act is not null)
            print 'Statistics for column:                  "%1!"',@colname
         else
         begin   -- BUILD A COLUMN GROUP NAME
            select @colgroup_name = null
                     /* move to the left most byte on big-endians when colid_len != 1 */
                  ,@colidarray_len = @colidarray_len - (@big_endian * (abs(sign(@colid_len - 1))))
            while (@colidarray_len > 0)
            begin
               select @colgroup_name =
                                substring(', ' ,1-sign(1-sign(@colidarray_len - @colid_len)),2)
                              + '"' + name + '"'
                              + @colgroup_name
               from syscolumns
               where id = @tabid
                 and convert(varbinary,colid) = convert(varbinary,substring(@colidarray,@colidarray_len,@colid_len))
               select @colidarray_len = @colidarray_len - @colid_len
            end
            print 'Statistics for column group:            %1!',@colgroup_name
         end
         print 'Last update of column statistics:       %1!',@last_updt
         if (@c1stat & 2 = 2)
            print 'Statistics loaded from Optdiag.'
         print ''
         print '     Range cell density:                %1!',@rc_density
         print '     Total density:                     %1!',@tot_density
         if (@r_sel is not null)
            print '     Range selectivity:                 %1!',@r_sel
         else
            print '     Range selectivity:                 default used (0.33)'
         if (@between_sel is not null)
            print '     In between selectivity:            %1!',@between_sel
         else
            print '     In between selectivity:            default used (0.25)'
         print ''
         if (@steps_act is not null) /** Print a Histogram **/
         begin
            truncate table #cells
            select @freq_cell = 0, @seq = 1
            select @used_count = isnull(sum(usedcount),0)
            from sysstatistics
            where id = @tabid
             and  statid = @statid
             and  colidarray = substring(convert(varbinary,convert(int,@colid)),(@big_endian * (4 - @colid_len)) + 1,@colid_len)
             and  formatid = 104
             and  sequence = @seq
            while (@used_count > 0)
            begin
               select @rownum = 1
               while (@rownum <= @used_count)
               begin
                  insert into #cells(seq,colnum) values (@seq,@rownum)
                  select @rownum = @rownum + 1
               end
               select @seq = @seq + 1
               select @used_count = isnull(sum(usedcount),0)
               from sysstatistics
               where id = @tabid
                and  statid = @statid
                and  colidarray = substring(convert(varbinary,convert(int,@colid)),(@big_endian * (4 - @colid_len)) + 1,@colid_len)
                and  formatid = 104
                and  sequence = @seq
            end

            print 'Histogram for column:                   "%1!"',@colname
            if (@typename in ("int","intn"))
               select @typename = "integer"
            if (@typename = "float" and @collength = "4")
               select @typename = "real"
            if (@typename = "float" and @collength = "8")
               select @typename = "double precision"
            if (@typename in ("varchar","nvarchar","char","nchar","binary","varbinary","float","floatn"))
               print 'Column datatype:                        %1!(%2!)',@typename,@collength
            else if (@typename in ("numeric","decimal","numericn","decimaln"))
               print 'Column datatype:                        %1!(%2!,%3!)',@typename,@precision,@scale
            else
               print 'Column datatype:                        %1!',@typename
            print 'Requested step count:                   %1!',@steps_req
            print 'Actual step count:                      %1!',@steps_act
            print ''
            print '     Step     Weight                    Value'
            print ''

            open histogram_cursor
            fetch histogram_cursor into
               @step, @weight, @value_raw
            while (@@sqlstatus = 0)
            begin
               select
                 @value_c =
                     CASE
                      WHEN @typename in ("varchar","nvarchar","char","nchar")
                       THEN '"' + convert(varchar(255),@value_raw) + '"'

                      WHEN @typename in ("int","intn","integer")
                       THEN str(convert(int,@value_raw),11)

                      WHEN @typename in ("smallint")
                       THEN str(convert(smallint,@value_raw),11)

                      WHEN @typename in ("tinyint")
                       THEN str(convert(tinyint,@value_raw),11)

                      /** Oh, oh, a scaled numeric, where does the decimal place go??? **/
                      WHEN (@typename in ("numeric","decimal","numericn","decimaln") and convert(smallint,@scale) > 0)
                       THEN str(convert(numeric(38),substring(@value_raw,1,1)
                                    + right(replicate(0x00,255-convert(int,@collength))
                                    + right(@value_raw,convert(int,@collength) - 1),16))
                              /* move over @scale decimal places please */
                                /power(convert(numeric,10),convert(smallint,@scale))
                              /* make room for @precision, minus, and decimal signs */
                               , convert(smallint,@precision)+2,convert(smallint,@scale))

                      WHEN (@typename in ("numeric","decimal","numericn","decimaln") and @scale = "0")
                       THEN str(convert(numeric(38),substring(@value_raw,1,1)
                                    + right(replicate(0x00,255-convert(int,@collength))
                                    + right(@value_raw,convert(int,@collength) - 1),16))
                                , convert(smallint,@precision))

                      WHEN (@typename in ("float","floatn","real") and @collength = "4")
                       THEN str(convert(real,@value_raw),40,8)

                      WHEN (@typename in ("float","floatn","double precision") and @collength = "8")
                       THEN str(convert(double precision,@value_raw),40,16)

                      WHEN @typename in ("money","moneyn","smallmoney")
                       THEN str(convert(money,@value_raw),22,2)

                      WHEN @typename in ("datetime","datetimn")
                       THEN '"' + convert(varchar(255),convert(datetime,@value_raw),109) + '"'

                      WHEN @typename in ("smalldatetime")
                       THEN '"' + convert(varchar(255),convert(smalldatetime,@value_raw),100) + '"'

                      WHEN @typename in ("date","daten")
                       THEN '"' + substring(convert(varchar(255),convert(datetime,@value_raw+0x00000000),101),1,11) + '"'

                      WHEN @typename in ("time","timen")
                       THEN '"' + substring(convert(varchar(255),convert(datetime,0x00000000+@value_raw)),12,8) + '"'

                      ELSE @value_raw
                     END

               if (@value_raw is null)
                 select @freq_cell =1 , @prev_step = @step, @prev_weight = @weight, @value_c = "null"
               else
               begin
                 select @value_c = ltrim(@value_c)
                 if (@freq_cell = 1)
                 begin /* Printing a frequency cell */
                    if (@typename in ("binary","varbinary","timestamp"))
                    begin
                       print '%1!     %2!        <       %3!',@prev_step,@prev_weight,@value_raw
                       print '%1!     %2!        =       %3!',@step,@weight,@value_raw
                    end
                    else
                    begin
                       print '%1!     %2!        <       %3!',@prev_step,@prev_weight,@value_c
                       print '%1!     %2!        =       %3!',@step,@weight,@value_c
                    end
                 end
                 else /* NOT printing a frequency cell */
                 begin
                    if (@typename in ("binary","varbinary","timestamp"))
                       print '%1!     %2!       <=       %3!',@step,@weight,@value_raw
                    else
                       print '%1!     %2!       <=       %3!',@step,@weight,@value_c
                 end
                 select @freq_cell = 0
               end

               fetch histogram_cursor into
                  @step, @weight, @value_raw
            end
            close histogram_cursor
            /* Is there only one cell (a freqency cell) */
            if (@freq_cell = 1)
                    print '%1!     %2!        =       %3!',@prev_step,@prev_weight,@value_c
            print ''
         end /* histogram print */

      fetch col_cursor into
         @colid, @colidarray, @colidarray_len,  @colname, @statid, @c1stat, @last_updt, @rc_density, @tot_density
        ,@steps_act, @steps_req, @typename, @collength, @precision, @scale, @r_sel, @between_sel
      end
      close col_cursor
      -----------------------
      -- Done with columns --
      -----------------------

      ------------------------------
      -- print cols with no stats --
      ------------------------------
      select @keycnt = 0
      open nostats_cursor
      fetch nostats_cursor into @colname
      while (@@sqlstatus = 0)
      begin
         select @keycnt = @keycnt + 1
         if (@keycnt = 1)
            print 'No statistics for remaining columns:    "%1!"',@colname
         else if (@keycnt = 2)
            print '(default values used)                   "%1!"',@colname
         else
            print '                                        "%1!"',@colname
         fetch nostats_cursor into @colname
      end
      close nostats_cursor
      if (@keycnt = 1)
         print '(default values used)'

      print ''

      fetch object_cursor into
         @tabid, @u_dbname, @u_dbid,
         @u_tabowner, @u_tabname, @pg_hdr
   end
   close object_cursor
-----------------------
-- Done with Objects --
-----------------------
end
go


-------------------------------------------------------------
-- Added to/after the original script
-------------------------------------------------------------
grant exec on sp__optdiag to public
go

use master
go

/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.config.dict;

// Below is from Whitepaper: 
// Managing Workloads with ASE: Techniques for OLTP Scaling and Performance Management with Large SMP and Shared Disk Clusters
// http://www.sybase.com/detail?id=1091630
//
// FROM PAGE: 71
//u 54:  waiting for write of the last log page to complete   Comment: Waiting for log semaphore
//u 55:  wait for i/o to finish after writing last log page   Comment: Waiting for log writes to disk
//- 89:  wait for space in ULC to preallocate CLR:            Comment: Rare
//- 90:  wait after error in ULC while logging CLR:           Comment: Rare
//- 122: wait for space in ULC to log commit record:          Comment: Rare
//- 123: wait after error logging commit record in ULC        Comment: Rare
//- 140: wait to grab spinlock                                Comment: Possibly a cache spinlock - note that this is rare as a spinlock is a true spinlock and process doesn�t sleep. You will only see this event if a process was forced to sleep (timeslice) while still trying to get a spinlock.
//u 259: waiting until last chance threshold is cleared       Comment: Typical log suspend issue due to log full
//- 271: waiting for log space during ULC flush               Comment: (rare) likely cause for this was that there was a cache stall in the data cache buffer pool containing the log. Coupled with WaitEventID=140 (above), this should help explain what a dedicated log cache is important for any high volume OLTP database
//u 272: waiting for lock on ULC                              Comment: Mostly seen in high volume OLTP systems with a lot of write activity - usually a small value. Can be tuned via sp_configure �user log cache spinlock ratio� if it becomes a problem
//
//FROM PAGE: 117
//  ID  WaitEvent Description                                 Most Common Cause
//  --- ----------------------------------------------------- ----------------------------------------------
//u 29  waiting for regular buffer read to complete           Physical read from disk. These are page-sized reads (1x buffers). See Event #124.
//u 30  wait to write MASS while MASS is changing             Checkpoint or HK is waiting to flush a buffer to disk but needs to wait on another SPID that is currently modifying a page in one of the buffers
//u 31  waiting for buf write to complete before writing      HK , checkpoint or wash induced IO is waiting for another SPID to finish a logical write before flushing to disk. This is the opposite of 52.
//u 32  waiting for an APF buffer read to complete 
//u 33  waiting for buffer read to complete                   Waiting for a logical read - possibly because cache spinlock is held or timeslice expired.
//u 34  waiting for buffer write to complete                  Waiting to modify a page in memory - possibly because the cache spinlock is held
//u 35  waiting for buffer validation to complete 
//u 36  waiting for MASS to finish writing before changing    A SPID is waiting to make a change to one of the pages in the buffer, but first has to wait for the checkpoint, HK or another SPID that is flushing the buffer to disk.
//u 37  wait for MASS to finish changing before changing      A SPID is waiting to make changes to a page header; but must wait until another spid finishes its changes
//- 38  wait for mass to be validated or finish changing 
//u 46  wait for buf write to finish getting buf from LRU     MRU->LRU relinking due to rows being added to a table causing a new page to be allocated - such as an insert/select; select/into or an update that causes a row expansion (page split or row forwarding). LRU page may be dirty (cache stall) hence the wait.
//- 47  wait for buf read to finish getting buf from LRU      Cache stall when attempting to read a new page into memory.
//u 51  waiting for last i/o on MASS to complete              Current SPID data modification waiting due to blocking IO on last statement (such as index tree maintenance/rebalance)
//u 52  waiting for i/o on MASS initiated by another task     Current SPID data modification waiting for physical write initiated by another task (checkpoint, housekeeper, etc.)
//u 124 wait for mass read to finish when getting page        Waiting on a large I/O read, most likely an APF
//- 125 wait for read to finish when getting cached page      Waiting on a physical read issued by another SPID for the same page but not yet in cache.

import java.util.HashMap;

import com.dbxtune.utils.StringUtil;


public class MonWaitEventIdDictionary
{
    /** Log4j logging. */

	/** Instance variable */
	private static MonWaitEventIdDictionary _instance = null;

	public static int LINE_BREAK_AFTER = 100;
	public static int WORD_HYSTERESIS  = 2;
	
	/** hashtable with MonWaitEventIdRecord */
	private HashMap<Integer,WaitEventIdRecord> _monWaitEwentIds = new HashMap<Integer,WaitEventIdRecord>();

	public class WaitEventIdRecord
	{
		private int      _id              = -1;
		private String   _name            = null;
		private String   _slogan          = null;
		private String[] _description     = null;
		private String[] _action          = null;
		private String   _txtSource       = null;
		private String[] _comment         = null;
		private String[] _mostCommonCause = null;

		public WaitEventIdRecord(int id, String name, String slogan, String[] description, String[] action, String txtSource, String[] comment, String[] mostCommonCause)
		{
			_id          = id;
			_name        = name;
			_slogan      = slogan;
			_description = description;
			_action      = action;
			_txtSource   = txtSource;
			
			_comment         = comment;
			_mostCommonCause = mostCommonCause;

			if (_name != null){} // dummy to make "never read locally" warnings go away...
		}
		
		/**
		 * For the String array lets constrct the following
		 * strArr[0]:     <p>first  row in the array</p>
		 * strArr[1]: <br><p>second row in the array</p>
		 * strArr[2]: <br><p>third  row in the array</p>
		 * 
		 * Also if current string in the array is longer than "about" ### character add a <BR>
		 * @param strArr
		 * @return
		 */
		private String makeHtmlParagraph(String[] strArr)
		{
			StringBuffer sb = new StringBuffer();

			for (int i=0; i<strArr.length; i++)
			{
				if ( ! strArr[i].startsWith("<") )
				{
					if (i > 0)
						sb.append("<br>");

					sb.append("<p>");
					sb.append( StringUtil.makeApproxLineBreak(strArr[i], LINE_BREAK_AFTER, WORD_HYSTERESIS, "<BR>") );
					sb.append("</p>");
				}
				else
				{
					sb.append( StringUtil.makeApproxLineBreak(strArr[i], LINE_BREAK_AFTER, WORD_HYSTERESIS, "<BR>") );
				}
			}

			return sb.toString();
		}

		@Override
		public String toString()
		{
			StringBuffer sb = new StringBuffer();

			sb.append("<html>");

			sb.append("<h2>Event ").append(_id).append(": ").append(_slogan).append("</h2>");

			if (_comment != null)
			{
				sb.append("<h3>Comment</h3>");
				sb.append(makeHtmlParagraph(_comment));
			}

			if (_mostCommonCause != null)
			{
				sb.append("<h3>Most Common Cause</h3>");
				sb.append(makeHtmlParagraph(_mostCommonCause));
			}

			if (_description != null)
			{
				sb.append("<h3>Description</h3>");
				sb.append(makeHtmlParagraph(_description));
			}

			if (_action != null)
			{
				sb.append("<h3>Action</h3>");
				sb.append(makeHtmlParagraph(_action));
			}

			sb.append(getSourceText());;
			sb.append("</html>");

			return sb.toString();
		}

		public String getSourceText()
		{
			if (_txtSource != null)
				return StringUtil.makeApproxLineBreak("<BR><HR><B>Text Source:</B> "+ _txtSource, LINE_BREAK_AFTER, WORD_HYSTERESIS, "<BR>");
			else
			{
				String str = "";
				if (_description != null || _action != null)
				{
					str += "<BR><HR><B>Text Source:</B>  for 'Description' and 'Action'<BR>" +
						"<B>Sybase Manual:</B> <I>Performance and Tuning Series: Monitoring Tables</I><BR>" +
						"<B>Document ID:</B> DC00848-01-1570-01<BR>" +
						"<B>Last Revised:</B> September 2011";
				}
				if (_comment != null || _mostCommonCause != null)
				{
					str += "<BR><HR><B>Text Source:</B> for 'Comment' and 'Most Common Cause'<BR>" +
						"<B>Sybase Whitepaper:</B> <I>Managing Workloads with ASE: Techniques for OLTP Scaling and <BR>Performance Management with Large SMP and Shared Disk Clusters</I><BR>" +
						"<B>Last Revised:</B> Version 1.0 - November 2010<BR>" +
						"<B>Download here:</B> http://www.sybase.com/detail?id=1091630";
				}
				return str;
			}
		}
	}

	public static String getEmpty(int id)
	{
		StringBuffer sb = new StringBuffer();

		sb.append("<html>");

//		sb.append("<h2>Event ").append(id).append(": No writeup is available for this Event.</h2>");
		sb.append("<h2>No writeup is available for Event ").append(id).append("</h2>");

		sb.append("<h3>Description</h3>");
		sb.append("If you want to write one, please do.");

		sb.append("<h3>Action</h3>");
		sb.append("Email the Action and Description to <A HREF=\"mailto:goran_schwarz@hotmail.com\">goran_schwarz@hotmail.com");

		sb.append("</html>");

		return sb.toString();
	}

	public MonWaitEventIdDictionary()
	{
		init();
	}

	public static MonWaitEventIdDictionary getInstance()
	{
		if (_instance == null)
			_instance = new MonWaitEventIdDictionary();
		return _instance;
	}

	private void add(WaitEventIdRecord rec)
	{
		_monWaitEwentIds.put(Integer.valueOf(rec._id), rec);
	}

	public String getToolTipText(int waitEventId)
	{
		WaitEventIdRecord rec = _monWaitEwentIds.get(Integer.valueOf(waitEventId));
		if (rec != null)
			return rec.toString();

		// Compose an empty one
		return getEmpty(waitEventId);
	}

	private void init()
	{
		int      event  = -1;
		String   name   = null;
		String   slogan = null;
		String[] desc   = null;
		String[] action = null;
		String   txtsrc = null;

		String[] comment= null;
		String[] mcc    = null; // Most Common Cause


		event  = 19;
		slogan = "xact coord: pause during idle loop";
		desc   = new String[] {"The Adaptive Server transaction coordinator (ASTC) sleeps, waiting for an alarm or a server task to wake it (ASTC handles transactions involving multiple database servers). If the server does not perform many distributed transactions, the <CODE>time per wait</CODE> for this event is close to 60 seconds."};
		action = new String[] {"No action necessary. Even with high values for <CODE>WaitTime</CODE>, event 19 does not affect overall performance."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 29;
		slogan = "waiting for regular buffer read to complete";
		desc   = new String[] {"A wait caused by a physical read (most likely a cache miss) which occurs when Adaptive Server does not find a page in the data cache and must read it from disk. The number of <CODE>Waits</CODE> is the number of physical reads that occurred because of a cache miss. Use the <CODE>monSysWaits.WaitTime</CODE> value to derive I/O response times"};
		action = new String[] {"Because this event�s value for <CODE>monSysWaits.WaitTime</CODE> is measured in seconds, the value for <CODE>WaitTime</CODE> for this event should be much less than the value for <CODE>Waits</CODE> (an average physical read should be 2-6 milliseconds; more than 10 milliseconds is considered slow). A high average physical read value may indicate poor disk throughput performance. Query <CODE>monIOQueue</CODE> and <CODE>monDeviceIO</CODE> to identify slow or overloaded disks.",
		                       "A high value for <CODE>Waits</CODE>, regardless of the value for <CODE>WaitTime</CODE>, may indicate that query plans are not as effective as they could be. If you encounter a high value for <CODE>Waits</CODE>, a table scan or Cartesian product may have occurred, or the optimizer may have selected a bad plan, due to bad, stale, or missing statistics. Consider adding an index on specific columns to the table on which this occurred.",
		                       "A high value for <CODE>Waits</CODE> can also indicate the data caches are too small, with active pages first pushed out and then reread. Query <CODE>monOpenObjectActivity</CODE>, <CODE>monProcessActivity</CODE>, <CODE>monDataCache</CODE>, <CODE>monCachPool</CODE>, and <CODE>monProcessObject</CODE> to determine how to proceed."};
		comment= null;
		mcc    = new String[] {"Physical read from disk. These are page-sized reads (1x buffers). See Event #124."};
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 30;
		slogan = "wait to write MASS while MASS is changing";
		desc   = new String[] {"Adaptive Server is attempting to write to a MASS (Memory Address Space Segment, <I>aka Extent IO</I>). A MASS is one or more contiguous pages Adaptive Server keeps in a data cache. However, in this event, the status of the MASS is \"changing,\" meaning another spid is updating the MASS. The spid initiating the write cannot write to the MASS until the MASS is no longer in use.",
		                       "A high value for <CODE>WaitTime</CODE> for event 30 may indicate that the data cache is too small, causing pages in the data cache to reach the wash area frequently, forcing the <CODE>checkpoint</CODE> process to perform more writes than necessary.",
		                       "<i>MASS write to disk is delayed because someone is updating a page in the MASS</i>"};
		action = new String[] {"You may be able to reduce high wait times by:",
		                       "<UL>",
		                       "<LI>Increasing the size of the data cache",
		                       "<LI>Using cache partitions or named caches to separate memory-intensive objects",
		                       "<LI>Tuning the housekeeper, washmarker position, or schema implications (such as sequential key tables)",
		                       "<LI>Positioning the washmarker",
		                       "<LI>Adjusting the schema (such as sequential key tables)",
		                       "</UL>"};
		comment= null;
		mcc    = new String[] {"Checkpoint or HK is waiting to flush a buffer to disk but needs to wait on another SPID that is currently modifying a page in one of the buffers"};
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 31;
		slogan = "waiting for buf write to complete before writing";
		desc   = new String[] {"A server process responsible for writing data pages to the disk (for example, a checkpoint) has determined that it must write a MASS (Memory Address Space Segment, <I>aka Extent IO</I>). However, an earlier <CODE>write</CODE> operation involving the same page has not finished, so the second process must wait until the first <CODE>write</CODE> completes before initiating its <CODE>write</CODE> operation."};
		action = new String[] {"Generally, the value for <CODE>WaitTime</CODE> for event 31 should be less than the value for <CODE>Waits</CODE>. High values for <CODE>WaitTime</CODE> may indicate disk contention or slow performance. Query <CODE>monIOQueue</CODE> and <CODE>monDeviceIO</CODE> to identify overloaded or slow disks.",
		                       "A high value for <CODE>WaitTime</CODE> for event 31 may also indicate that the data cache is too small, causing pages in the data cache to reach the wash area frequently and forcing the <CODE>checkpoint</CODE> process to perform more writes than necessary."};
		comment= null;
		mcc    = new String[] {"HK , checkpoint or wash induced IO is waiting for another SPID to finish a logical write before flushing to disk. This is the opposite of 52."};
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 32;
		slogan = "waiting for an APF buffer read to complete";
		desc   = new String[] {"When Adaptive Server issues an asynchronous prefetch (APF) on a page, another process is reading the MASS (Memory Address Space Segment, <I>aka Extent IO</I>) to which this page belongs. Adaptive Server must wait for the read to complete before continuing."};
		action = new String[] {"A high value for <CODE>Waits</CODE> may indicate that Adaptive Server is using asynchronous prefetch too often. Tuning the local APF limit for cache pools may reduce contention for APF pages.",
		                       "Since Adaptive Server often uses APF for table scans, contention involving APF reads may indicate that an application is performing too many table scans because of factors such as missing indexes."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 33;
		slogan = "waiting for buffer read to complete";
		desc   = new String[] {"Logical read."};
		action = new String[] {""};
		comment= null;
		mcc    = new String[] {"Waiting for a logical read - possibly because cache spinlock is held or timeslice expired."};
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 34;
		slogan = "waiting for buffer write to complete";
		desc   = new String[] {"Logical write."};
		action = new String[] {""};
		comment= null;
		mcc    = new String[] {"Waiting to modify a page in memory - possibly because the cache spinlock is held"};
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 35;
		slogan = "waiting for buffer validation to complete";
		desc   = new String[] {"Indicates that a process is attempting to read data in a page that another process has read into cache. After reading a page into a data cache, Adaptive Server validates the success of the <CODE>read</CODE> operation. Because Adaptive Server is validating whether the <CODE>read</CODE> was successful, the second process must wait for this to complete before accessing the data.",
		                       "This event commonly occurs during periods of high physical reads."};
		action = new String[] {"The value for <CODE>WaitTime</CODE> for event 35 should be quite small. If the value is large, many processes are accessing the same page at the same time, or there is CPU contention. Query <CODE>monEngine</CODE> to determine if the engines are overloaded, and run system-level utilities to determine if there is overall CPU contention."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 36;
		slogan = "waiting for MASS to finish writing before changing";
		desc   = new String[] {"A spid must make changes to a MASS (Memory Address Space Segment, <I>aka Extent IO</I>), but another spid is currently writing the MASS. The second spid must wait until the write completes.",
		                       "<i>A SPID wants to change MASS, but has to wait because it is still being flushed to disk.  Could also be: Last page contention with other users, Including space (memory) allocation contention in tempdb.</i>"};
		action = new String[] {"A high value for <CODE>WaitTime</CODE> indicates that some condition may be causing diminished I/O or data cache manager performance. Normally, the value for <CODE>Waits</CODE> should be higher than the value for <CODE>WaitTime</CODE>. Query <CODE>monIOQueue</CODE> and <CODE>monDeviceIO</CODE> to determine if a disk device is slow or overloaded.",
		                       "<HR><B>NOTE:</B> If event 36 occurs because of an update to a page, partitioning th cache has not effect. However, if event 36 occurs when page updates are not taking place, partitioning the cache may expedite the writes."};
		comment= null;
		mcc    = new String[] {"A SPID is waiting to make a change to one of the pages in the buffer, but first has to wait for the checkpoint, HK or another SPID that is flushing the buffer to disk."};
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 37;
		slogan = "wait for MASS to finish changing before changing";
		desc   = new String[] {"A spid attempts to make changes to the MASS (Memory Address Space Segment, <I>aka Extent IO</I>), but another spid is currently changing the MASS. The first spid must wait until the changes are complete before it can make changes to the MASS.",
				               "<i>A SPID wants to change MASS, but has to wait because someone else is changing it. Could also be: Last page contention with other users, Including space (memory) allocation contention in tempdb.</i>"};
		action = new String[] {"Typically, the values for <CODE>Waits</CODE> for event 37 should be much higher than the values for <CODE>WaitTime</CODE>. If the values are not higher for <CODE>Waits</CODE>, either many processes are accessing the same MASS at once, or there is CPU contention. Query <CODE>monEngine</CODE> to determine if the engines are overloaded. Run system-level utilities to determine if there is overall CPU contention."};
		comment= null;
		mcc    = new String[] {"A SPID is waiting to make changes to a page header; but must wait until another spid finishes its changes"};
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 41;
		slogan = "wait to acquire latch";
		desc   = new String[] {"Event 41 often indicates that multiple processes are simultaneously attempting to update rows on a single page.",
		                       "Adaptive Server uses a latch as a transient lock to guarantee a page�s contents are not changed while another process reads or writes data. Adaptive Server typically uses latches in data-only locked tables to protect the contents of the page when multiple processes are simultaneously reading or updating rows on the same page. If one process attempts to acquire a latch while another process already holds the latch, the first process may need to wait. If event 41 occurs frequently, it may indicate a high level of contention for data on a single physical page within an index or table.",
		                       "Reduce contention by:",
		                       "<UL>",
		                       "<LI>Introducing an index with a sort order that distributes data differently across pages, so it spreads the rows that are causing contention",
		                       "<LI>Changing your application so that such contention does not occur",
		                       "</UL>"};
		action = new String[] {"Consider reducing contention for pages by changing index definitions in a way that alters the physical distribution of data across the data and index pages within your table, or modifying your application to reduce contention.",
		                       "If the average value for <CODE>WaitTime</CODE> is high, event 41 may occur because of an Adaptive Server resource shortage, resulting from:",
		                       "<UL>",
		                       "<LI>A hash table that is too small for a lock, resulting in very long hash chains that Adaptive Server must search.",
		                       "<LI>An operating system issue during which calls that should be quick are a bottle neck (for example, starting asynchronous I/O, which should return immediately, blocks because of operating system resource limitations.",
		                       "<LI>Extremely high inserts and expanding updates. Page allocations take place frequently, and contention for the allocation page latch results in a high number of <CODE>Waits</CODE>. Use <CODE>dbcc tune(des_greedyalloc)</CODE> to reduce this contention. For information about latch contention, see <I>Performance and Tuning Series: Monitoring Adaptive Server with sp_sysmon</I>.",
		                       "</UL>"};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 46;
		slogan = "wait for buf write to finish getting buf from LRU";
		desc   = new String[] {"A spid attempts to acquire a buffer from the least recently used (LRU) chain. However, the buffer has an outstanding write that must finish before Adaptive Server can use the buffer for a different page."};
		action = new String[] {"Event 46 may indicate that:",
		                       "<UL>",
		                       "<LI>A cache is so busy that the buffer at the end of the LRU chain is still processing. Query <CODE>monDataCache</CODE> and <CODE>monCachePool</CODE> to determine which cache is busy. Possible resolutions include: increasing the size of the cache, using <CODE>sp_poolconfig</CODE> to increase the wash size, and increasing the housekeeper activity by retuning <CODE>enable housekeeper GC</CODE>.",
		                       "<LI>Disk writes are taking a long time to complete. Query <CODE>monIOQueue</CODE> and <CODE>monDeviceIO</CODE> to determine if there is a slow or overloaded disk device.",
		                       "</UL>"};
		comment= null;
		mcc    = new String[] {"MRU->LRU relinking due to rows being added to a table causing a new page to be allocated - such as an insert/select; select/into or an update that causes a row expansion (page split or row forwarding). LRU page may be dirty (cache stall) hence the wait."};
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 47;
		slogan = "wait for buf read to finish getting buf from LRU";
		desc   = null;
		action = null;
		comment= null;
		mcc    = new String[] {"Cache stall when attempting to read a new page into memory."};
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 51;
		slogan = "waiting for last i/o on MASS to complete";
		desc   = new String[] {"Occurs when a process is writing a range of pages for an object to disk because of a change to the object or because the object is removed from the metadata cache. Because it is important to complete the I/O operations on some pages before other pages are written, the process must wait until it is notified that the I/O that was initiated has completed its task. MASS=(Memory Address Space Segment, <I>aka Extent IO</I>)",
				               "<i>A lot of I/O likely coming from same process OR a page split caused a synchronous IO</i>"};
		action = new String[] {"A high value for <CODE>WaitTime</CODE> indicates that writes may be taking a long time to complete. Typically, the value for <CODE>Waits</CODE> should be much higher than the value for <CODE>WaitTime</CODE>. Query <CODE>monIOQueue</CODE> and <CODE>monDeviceIO</CODE> to determine if there is a slow or overloaded disk device."};
		comment= null;
		mcc    = new String[] {"Current SPID data modification waiting due to blocking IO on last statement (such as index tree maintenance/rebalance)"};
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 52;
		slogan = "waiting for i/o on MASS initiated by another task";
		desc   = new String[] {"A process writes a range of pages for an object to disk because of a change to the object, or because the object was removed from the metadata cache. However, another spid has an I/O outstanding on the MASS (Memory Address Space Segment, <I>aka Extent IO</I>), and the second process must sleep until the first process�s finish writing.",
				               "<i>Could also be: Last page contention with other users, Including space (memory) allocation contention in tempdb.</i>"};
		action = new String[] {"A high value for <CODE>WaitTime</CODE> for this event indicates that writes may be taking too long to complete. Typically, the value for <CODE>Waits</CODE> should be much higher than the value for <CODE>WaitTime</CODE>. Query <CODE>monIOQueue</CODE> and <CODE>monDeviceIO</CODE> to determine if there is a slow or overloaded disk device."};
		comment= null;
		mcc    = new String[] {"Current SPID data modification waiting for physical write initiated by another task (checkpoint, housekeeper, etc.)"};
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 53;
		slogan = "waiting for MASS to finish changing to start i/o";
		desc   = new String[] {"A spid attempts to write to a MASS (Memory Address Space Segment, <I>aka Extent IO</I>), but another spid is already changing the MASS, so the first spid must wait until the changes are complete.",
		                       "Adaptive Server minimizes the number of disk I/O operations it performs. If a process responsible for writing pages (for example, the checkpoint process) needs to modify a page but determines that another process is modifying the page, the second process waits until the first process completes so the page write includes the page modification.",
		                       "<i>MASS write to disk is delayed because someone is updating a page in the MASS</i>"};
		action = new String[] {"Normally, the value for <CODE>Waits</CODE> for event 53 should be higher than the value for <CODE>WaitTime</CODE>. If it is not higher, either many processes are simultaneously accessing the same MASS, or there is CPU contention. Query <CODE>monEngine</CODE> to determine if the engines are overloaded. Run system-level utilities to determine if there is overall CPU contention."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 54;
		slogan = "waiting for write of the last log page to complete";
		desc   = new String[] {"Event 54 occurs when a process is about to initiate a write of the last log page but discovers another process is already scheduled to perform <CODE>write</CODE>. The second process waits until the first process finishes its I/O, but the second process does not initiate the I/O operation.",
		                       "Because Adaptive Server frequently updates the last page of the transaction log, Adaptive Server avoids performing physical writes of the last log page. This reduces the amount of I/O the server performs and increases the last log page�s availability to other processes that need to perform an update, and thereby impoving performance."};
		action = new String[] {"A high average value for <CODE>WaitTime</CODE> for event 54 indicates that writes are taking a long time to complete. Typically, the value for <CODE>Waits</CODE> should be much higher than the value for <CODE>WaitTime</CODE>. Query <CODE>monIOQueue</CODE> and <CODE>monDeviceIO</CODE> to determine if there is a slow or overloaded disk device.",
		                       "High values for <CODE>Waits</CODE>, regardless of the average time, may indicate contention for the last log page. Increase the size of the user log cache to reduce contention, or group operations for applications to avoid committing every row."};
		comment= new String[] {"Waiting for log semaphore"};
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 55;
		slogan = "wait for i/o to finish after writing last log page";
		desc   = new String[] {"Indicates a process has initiated a <CODE>write</CODE> operation on the last page of the transaction log, and must sleep until the I/O completes. A high value for the <CODE>Waits</CODE> column for event 55 indicates that Adaptive Server is making a large number of updates to the transaction log because of committed transactions or other operations that requiring writing the transasction log to disk."};
		action = new String[] {"A high value for <CODE>WaitTime</CODE> for event 55 indicates that writes may be taking a long time to complete. Typically, the value for <CODE>Waits</CODE> should be much higher than the value for <CODE>WaitTime</CODE>"};
		comment= new String[] {"Waiting for log writes to disk"};
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 57;
		slogan = "checkpoint process idle loop";
		desc   = new String[] {"The checkpoint process sleeps between runs to prevent the checkpoint from monopolizing CPU time."};
		action = new String[] {"Event 57 may accumulate large amounts of time since the checkpoint process starts when the server starts. However, you need not perform any actions based on this event."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));


		event  = 61;
		slogan = "hk: pause for some time";
		desc   = new String[] {"The housekeeper pauses occasionally to keep housekeeper functions from monopolizing CPU time."};
		action = new String[] {"Event 61 is expected, and may show large values on servers that have run for along time. Typically, you need not perform any actions based on this event."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 70;
		slogan = "waiting for device semaphore";
		desc   = new String[] {"If you are using Adaptive Server mirroring (that is, <CODE>disable disk mirroring</CODE> is set to 0), each disk device access must first hold the semaphore for that device. Event 70 measures the time spent waiting for that semaphore and can occur if disk I/O structures are too low."};
		action = new String[] {"If you are not using Adaptive Server mirroring, set <CODE>disable disk mirroring</CODE> to 1. If you are using mirroring, high values for <CODE>WaitTime</CODE> may indicate a loss of performance from device contention. Query <CODE>monIOQueue</CODE> and <CODE>monDeviceIO</CODE> to determine if there is a slow or overloaded disk device. Evaluate the results to determine if you can shift some of the load to other devices."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 83;
		slogan = "wait for DES state is changing";
		desc   = new String[] {"A object descriptor (called a \"DES\") is allocated for every open object (temporary tables, cached query plans and statement cache, stored procedures, triggers, defaults, rules, tables, and so on). Event 83 occurs when Adaptive Server is releasing an allocated descriptor, which typically happens when Adaptive Server is dropping an object."};
		action = new String[] {"A high value for <CODE>Waits</CODE> for event 83 may indicate a shortage of object descriptors. You may need to increase the number of open objects."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 84;
		slogan = "wait for checkpoint to complete";
		desc   = new String[] {"Adaptive Server is dropping a DES, which typically occurs when Adaptive Server is dropping an object. Event 84 indicates that the drop must wait for a checkpoint to complete on the database."};
		action = new String[] {"Although it is unlikely that the <CODE>Waits</CODE> value is high for event 84, a high value may indicate that many drops are occurring simultaneously, or that the checkpoint process is taking a long time. If the checkpoints are running for an excessive amount of time, try decreasing the recovery interval (in minutes)."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 85;
		slogan = "wait for flusher to queue full DFLPIECE";
		desc   = new String[] {"When Adaptive Server runs <CODE>dump database</CODE>, it uses the \"flusher\" process to create lists of pages (which includes a structure called DFLPIECE) that are in a data cache and have been changed. Adaptive Server sends the Backup Server a list of pages to include in the dump.",
		                       "Event 85 measures the time the dump process spends waiting for the flusher process to fill and queue DFLPIECE."};
		action = new String[] {"This event is normal during a <CODE>dump database</CODE>. If the average value for <CODE>WaitTime</CODE> is exceptionally high (higher than 2), check other events to determine what is slowing down the flusher processes."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 89;
		slogan = "wait for space in ULC to preallocate CLR";
		desc   = null;
		action = null;
		comment= new String[] {"Rare"};
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 90;
		slogan = "wait after error in ULC while logging CLR";
		desc   = null;
		action = null;
		comment= new String[] {"Rare"};
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 91;
		slogan = "waiting for disk buffer manager i/o to complete";
		desc   = new String[] {"When Adaptive Server runs <CODE>load database</CODE>, it may require the load process to verify that a disk I/O has completed before continuing. Event 91 measures the time Adaptive Server spends waiting for verification."};
		action = new String[] {"Generally, the value for <CODE>WaitTime</CODE> for event 91 should be much lower than the value for <CODE>Waits</CODE>. High values for <CODE>WaitTime</CODE> indicate possible disk contention or slowness. Query <CODE>monIOQueue</CODE> and <CODE>monDeviceIO</CODE> to determine if there is a slow or overloaded disk device."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 99;
		slogan = "wait for data from client";
		desc   = new String[] {"When a process uses a site handler to connect to a remote server, it must occasionally wait for the server to return the data. Event 99 measures the time the process must wait.",
		                       "A site handler is a method for transmitting RPCs from a local server to a remote server. A site handler establishes a single physical connection between local and remote servers and multiple logical connections, as required by RPCs."};
		action = new String[] {"A high average value for <CODE>WaitTime</CODE> for event 99 indicates slow communication with the remote server. This may be due to complex RPC calls that take a long time to complete, performance issues in the remote server, or a slow or overloaded network."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 104;
		slogan = "wait until an engine has been offlined";
		desc   = new String[] {"Adaptive Server has a service process that cleans up issues after an engine goes offline. This process typically sleeps, waking up every 30 seconds to check for work to do. Event 10 4 measures that sleep time."};
		action = new String[] {"The average value for <CODE>WaitTime</CODE> for event 104 should be very close to 30. If engines are frequently taken offline, this value may be slightly lower. If the average value for <CODE>WaitTime</CODE> is significantly higher or lower than 30, contact Sybase Technical Support."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 122;
		slogan = "wait for space in ULC to log commit record";
		desc   = null;
		action = null;
		comment= new String[] {"Rare"};
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 123;
		slogan = "wait after error logging commit record in ULC";
		desc   = null;
		action = null;
		comment= new String[] {"Rare"};
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 124;
		slogan = "wait for mass read to finish when getting page";
		desc   = new String[] {"Event 124 occurs when a process attempts to perform a physical read but another process has already performed the read request (this also counts as a \"cache miss\")."};
		action = new String[] {"The value for <CODE>WaitTime</CODE> for event 124 should be much lower than the value for <CODE>Waits</CODE>. The average value for <CODE>WaitTime</CODE> is high if disk performance is poor. Query <CODE>monIOQueue</CODE> and <CODE>monDeviceIO</CODE> to determine if there is a slow or overloaded disk device."};
		comment= null;
		mcc    = new String[] {"Waiting on a large I/O read, most likely an APF"};
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 125;
		slogan = "wait for read to finish when getting cached page";
		desc   = null;
		action = null;
		comment= null;
		mcc    = new String[] {"Waiting on a physical read issued by another SPID for the same page but not yet in cache."};
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 140;
		slogan = "wait to grab spinlock";
		desc   = null;
		action = null;
		comment= new String[] {"Possibly a cache spinlock - note that this is rare as a spinlock is a true spinlock and process doesn�t sleep. You will only see this event if a process was forced to sleep (timeslice) while still trying to get a spinlock."};
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 142;
		slogan = "wait for logical connection to free up";
		desc   = new String[] {"When Adaptive Server executes an RPC on a remote serer using the site handler mechanism, it creates logical connections.",
		                       "Event 142 occurs when Adaptive Server must close a logical connection but finds another process using it. Adaptive Server must wait until the logical connection is no longer in use before closing the logical connection."};
		action = new String[] {"Event 142 should normally have a very low average value for <CODE>WaitTime</CODE>. A high value for <CODE>WaitTime</CODE> may indicate there is a problem communicating with the remote server."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 143;
		slogan = "pause to synchronise with site manager";
		desc   = new String[] {"Adaptive Server communicates with a remote server using a site manager, but of another process is attempting to connect to that remote server. Event 143 measures the amount of time Adaptive Server waits to establish the connection to the remote server."};
		action = new String[] {"A high average value for <CODE>WaitTime</CODE> for event 143 may indicate performance issues on the remote server or a slow or overloaded network. Query <CODE>monProcessWaits</CODE> for <CODE>WaitEventID</CODE> 143 to determine which spids have high wait times."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 150;
		slogan = "waiting for a lock";
		desc   = new String[] {"A process attempts to obtain a logical lock on an object but another process is already holding a conflicting lock on this object. Event 150 is a common event that occurs when Adaptive Server performs an operation that requires locks to protect data that is being read or updated. The locks involved may be at various levels, including table, page, or row.",
		                       "After all conflicting locks are released, Adaptive Server wakes the waiting process and grants it access to the object."};
		action = new String[] {"The value for <CODE>WaitTime</CODE> for this event can be high if there is contention for a particular table or page (such as a high number of heap inserts). Query <CODE>monLocks</CODE> and <CODE>monOpenObjectActivity</CODE> to identify objects that are experiencing heavy lock contention.",
		                       "In some situations, you can reduce the amount of lock contention by changing the table�s locking scheme from allpages locking to data-only locking. Application or database design typically causes lock contention; evaluate your application design to determine the best method to reduce lock contention, while still considering other application requirements."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 157;
		slogan = "wait for object to be returned to pool";
		desc   = new String[] {"The Adaptive Server memory manager allocates memory for storing data describing a wide range of internal objects from separate memory \"pools\". When a pool�s available memory is low, requests for additional memory may be delayed until another operation returns memory to the pool. When this occurs, the requesting process must wait until more memory is available.",
		                       "Event 157 occurs when a process most wait for memory to become available before allocating the object�s data."};
		action = new String[] {"If the average value for <CODE>WaitTime</CODE> for event 157 is low, performance may not noticeably degrade. However, any <CODE>Waits</CODE> on this event indicate a condition you can correct by increasing the configured number of structures for which Adaptive Server is waiting. Use <CODE>sp_countmetadata</CODE> and <CODE>sp_monitorconfig</CODE> to identify which structures are using the maximum configuration to determine which resources you should increase."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 169;
		slogan = "wait for message";
		desc   = new String[] {"Some Adaptive Server processes (for example, worker threads, auditing, disk mirroring, and so on) use a structure called a \"mailbox\" to pass messages. Event 169 measures the time Adaptive Server spends waiting for a message in a mailbox."};
		action = new String[] {"Typically, the average value for <CODE>WaitTime</CODE> for event 169 is very small. However, if the value for <CODE>WaitTime</CODE> is large, query <CODE>monProcessWaits</CODE> for rows with <CODE>WaitEventID</CODE> value of 169 to determine which jobs have long wait times for this event."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 171;
		slogan = "wait for CTLIB event to complete";
		desc   = new String[] {"Indicates that Adaptive Server is waiting for the remote server to respond. Event 171 appears if you use Component Integration Services (CIS) for proxy tables and RPC calls."};
		action = new String[] {"A high average value for <CODE>WaitTime</CODE> for this event may indicate remote CIS server performance issues or a slow or overloaded network. Query <CODE>monProcessWaits</CODE> for <CODE>WaitEventID</CODE> 171 to determine which spids have high wait times for this event."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 178;
		slogan = "waiting while allocating new client socket";
		desc   = new String[] {"A network listener is an Adaptive Server process that handles a client�s incoming connection requests. Event 178 measures the time Adaptive Server spends waiting for new connection requests."};
		action = new String[] {"You need not perform any actions based on event 178. However, you can use some of its information for analysis. The value for <CODE>WaitTime</CODE> is roughly equivalent to the amount of time the server has been running. The values for <CODE>Waits</CODE> is a measure of how many connection attempts have been made since the server started."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 179;
		slogan = "waiting while no network read or write is required";
		desc   = new String[] {"The Adaptive Server network task sleeps on event 179 if there is no network I/O the server must send or receive. When there is network activity, the server task wakes, handles the requests, and then goes back to sleep."};
		action = new String[] {"High values for event 179 indicate high levels of network activity. If the network activity is unexpectedly high, query other monitoring tables - such as <CODE>monNetworkIO</CODE> and <CODE>monProcessNetIO</CODE> - to determine which jobs are slowing network performance.",
		                       "A high value for the <CODE>Waits</CODE> column for event 179 may indicate that <CODE>dbcc checkstorage</CODE> identified a large number of possible consistency faults. Check the reports from <CODE>dbcc checkstorage</CODE> for more information."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 197;
		slogan = "waiting for read to complete in parallel dbcc";
		desc   = new String[] {"When you run <CODE>dbcc checkstorage</CODE>, Adaptive Server must occasionally perform asynchronous I/O on the workspace to read or write a single reserved buffer. Event 197 measures the time Adaptive Server waits for those disk I/Os."};
		action = new String[] {"Generally, the value for <CODE>WaitTime</CODE> for event 197 should be much lower than the value for <CODE>Waits</CODE>. A high average value for <CODE>WaitTime</CODE> may indicate poor disk throughput performance. Query <CODE>monIOQueue</CODE> and <CODE>monDeviceIO</CODE> to determine if there is a slow or overloaded disk device."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 200;
		slogan = "waiting for page reads in parallel dbcc";
		desc   = new String[] {"Event 200 occurs when you run <CODE>dbcc checkstorage</CODE> using multiple worker processes. This event measures the time spent waiting for reads to complete on pages that <CODE>dbcc</CODE> checks."};
		action = new String[] {"Generally, the value for <CODE>WaitTime</CODE> for event 200 should be much lower than the value for <CODE>Waits</CODE>. A high average value for <CODE>WaitTime</CODE> may indicate poor disk throughput performance. Query <CODE>monIOQueue</CODE> and <CODE>monDeviceIO</CODE> to determine if there is a slow or overloaded disk device."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 201;
		slogan = "waiting for disk read in parallel dbcc";
		desc   = new String[] {"When you run <CODE>dbcc checkverify</CODE>, Adaptive Server performs a disk read to verify whether a potential fault exists in the disk copy of a page; event 201 measures the time spent waiting for those reads to complete."};
		action = new String[] {"Generally, the value for <CODE>WaitTime</CODE> for event 201 should be much lower than the value for <CODE>Waits</CODE>. A high average value for <CODE>WaitTime</CODE> may indicate poor disk throughput. Query <CODE>monIOQueue</CODE> and <CODE>monDeviceIO</CODE> to determine if there is a slow or overloaded disk device."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 202;
		slogan = "waiting to re-read page in parallel";
		desc   = new String[] {"When you run <CODE>dbcc checkstorage</CODE>, Adaptive Server determines whether it needs to perform a disk read to verify whether a potential fault exists in the disk copy of a page; event 202 measures the time spent waiting for those reads to complete."};
		action = new String[] {"Generally, the value for <CODE>WaitTime</CODE> for event 202 should be much lower than the value for <CODE>Waits</CODE>. A high average value for <CODE>WaitTime</CODE> may indicate poor disk throughput. Query <CODE>monIOQueue</CODE> and <CODE>monDeviceIO</CODE> to determine if there is a slow or overloaded disk device."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 203;
		slogan = "waiting on MASS_READING bit in parallel dbcc";
		desc   = new String[] {"When you run <CODE>dbcc checkstorage</CODE>, Adaptive Server determines whether it needs to perform a disk read to verify whether a fault exists in the disk copy of the MASS (Memory Address Space Segment, <I>aka Extent IO</I>). However, another process may have already started that read. Event 203 measures the time spent waiting for those reads to complete."};
		action = new String[] {"Generally, the value for <CODE>WaitTime</CODE> for event 203 should be much lower than the value for <CODE>Waits</CODE>. A high average value for <CODE>WaitTime</CODE> may indicate poor disk throughput. Query <CODE>monIOQueue</CODE> and <CODE>monDeviceIO</CODE> to determine if there is a slow or overloaded disk device."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 205;
		slogan = "waiting on TPT lock in parallel dbcc";
		desc   = new String[] {"When you run <CODE>dbcc checkstorage</CODE> to check text and image pages, Adaptive Server must hold a lock to prevent multiple worker threads from accessing the page links at the same time. Event 205 measures the time spent waiting for those locks."};
		action = new String[] {"The frequency of event 205 depends on how many text and image columns are contained in the tables you are checking. An exceptionally high average value for <CODE>WaitTime</CODE> may indicate some resource contention for the worker thread holding the lock. Check CPU and disk metrics to determine if there is contention."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 207;
		slogan = "waiting sending fault msg to parent in PLL dbcc";
		desc   = new String[] {"When you run <CODE>dbcc checkstorage</CODE>, each worker process reports possible faults to the parent process by queuing messages to the parent spid. If the mailbox of the parent process is full, the worker process must wait for more room in the mailbox before it can queue the next message. Event 207 measures the time the worker process spends waiting."};
		action = new String[] {"Event 207 is typically caused by Adaptive Server reporting a large number of faults. You need not take any actions for this event, other than to follow the normal process of running <CODE>dbcc checkverify</CODE> to verify and analyze the faults."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 209;
		slogan = "waiting for a pipe buffer to read";
		desc   = new String[] {"When Adaptive Server performs a sort in parallel (for example, <CODE>create index</CODE> that specifies a <CODE>consumers</CODE> clause), it uses an internal mechanism to send data between the various tasks. Event 209 measures the amount of time the tasks spend waiting for other tasks to add data to a pipe."};
		action = new String[] {"The average value for <CODE>WaitTime</CODE> for event 209 should be very low. High average values for <CODE>WaitTime</CODE> may indicate that the sort manager producer processes cannot generate data fast enough to keep the consumer processes busy. Check the overall system performance to determine if Adaptive Server has sufficient CPU and I\\O bandwidth."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 210;
		slogan = "waiting for free buffer in pipe manager";
		desc   = new String[] {"When Adaptive Server performs a sort in parallel (for example, <CODE>create index</CODE> that specifies a <CODE>consumers</CODE> clause), it uses an internal mechanism, called a pipe, to send data between the various tasks. Event 210 measures the amount of time a process waits for Adaptive Server to allocate a free pipe buffer."};
		action = new String[] {"The average value for <CODE>WaitTime</CODE> for event 210 should be very low. High average values for <CODE>WaitTime</CODE> may indicate that Adaptive Server has some resource contention. Run <CODE>sp_monitor</CODE> or <CODE>sp_sysmon</CODE>, or query <CODE>monEngine</CODE> to determine if Adaptive Server has sufficient CPU resources."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 214;
		slogan = "waiting on run queue after yield";
		desc   = new String[] {"Event 214 measures the amount of time a process waits on the run queue after yielding to allow other processes to run. This process is \"runnable\", not waiting on a lock, physical I/O, or any other wait condition. This event may be caused by insufficient CPU (that is, the server is CPU bound) or table scans in memory.",
		                       "Event 214 differs from event 215 by indicating a process is performing a CPU-intesive task that exceeded the CPU time allocated by <CODE>time slice</CODE>: the process yields the CPU voluntarily and is placed in a runnable state while it waits for the Adaptive Server scheduler to allocate more CPU time. When this occurs, the process continues with the activity it was performing before it yielded the CPU.",
		                       "Event 215 also indicates that a process is in a runnable state, but for event 214, the process entered this state not because it exceeded the CPU time, but because it encountered a condition that required it to wait for a resource, such as disk or network I/O or a logical lock, before it continues performing its task."};
		action = new String[] {"Busy servers typically have high values for <CODE>Waits</CODE>. However, high values for <CODE>WaitTime</CODE> or the <CODE>time slice</CODE> setting may indicate that Adaptive Server has a large number of spids waiting to execute, or that is has spids running which were heavily CPU bound and are not readily yielding their CPU. Query <CODE>monProcessActivity</CODE> to identify jobs that have high <CODE>CPUTime</CODE>."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 215;
		slogan = "waiting on run queue after sleep";
		desc   = new String[] {"Event 215 occurs when a process is no longer waiting for another wait event (for example, a logical lock, disk I/O, or another wait event) and is placed on the server�s runnable queue. The process must wait until the scheduler allocates CPU time before continuing its task.",
		                       "See the description for event 214 for differences between event 214 and 215."};
		action = new String[] {"Event 215 is a common wait event. The value for <CODE>Waits</CODE> for event 215 is typically large. Busy servers have high values for <CODE>WaitTime</CODE> because processes are waiting for the Adaptive Server runnable queue for a long time. Reduce the value for <CODE>time slice</CODE> to allow more processes to access CPU (this also reduces the average time some processes spend in the CPU) or, if there are sufficient CPUs available on the host machine, increase the number of online engines."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 222;
		slogan = "replication agent sleeping during flush";
		desc   = new String[] {"If Adaptive Server is a primary server performing replication, the RepAgent process sleeps, waiting for work to do (for example, when rows are added to the log for a database). Event 222 measures the amount of time RepAgent spends asleep."};
		action = new String[] {"Depending on the level of activity within a replicated database, event 222 may typically have high values for <CODE>WaitTime</CODE>. Typically, you need not perform any actions for this event."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 250;
		slogan = "waiting for incoming network data";
		desc   = new String[] {"This event measures the time that application processes are active, but waiting for the next request from a client (that is, when jobs are in the <CODE>AWAITING COMMAND</CODE> state).",
		                       "Event 250 typically occurs when the application remains connected to the Adaptive Server but is idle."};
		action = new String[] {"Because event 250 occurs before Adaptive Server processes each command from a client, the number of <CODE>Waits</CODE> and <CODE>WaitTime</CODE> may typically be high.",
		                       "You can use event 250 to estimate how many requests the server has handled from clients.",
		                       "A high <CODE>WaitTime</CODE> value for this event can indicate a large number of idle client connections, or that some client connections remain idle for a long period of time. This wait event can occur between batches or commands sent by the client application, so the <CODE>Waits</CODE> value may be high if applications submit a large number of separate commands or batches."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 251;
		slogan = "waiting for network send to complete";
		desc   = new String[] {"Event 251 measures the amount of time a job waits while sending a reply packet back to a client."};
		action = new String[] {"Event 251 may indicate that Adaptive Server is sending large reply sets to clients, or it may indicate a slow or overloaded network. Check the average packet size in the <CODE>monNetworkIO</CODE> and <CODE>monProcessNetIO</CODE> tables. In each of these tables, the average size is:",
		                       "<CODE>(BytesSent) / (PacketsSent)</CODE>",
		                       "Increasing the client application�s network packet size may improve network performance."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 259;
		slogan = "waiting until last chance threshold is cleared";
		desc   = new String[] {"When Adaptive Server crosses a last-chance threshold for a database log, every process trying to allocate more log space receives message 7415, and is put to sleep, or suspended, while it waits for available log space. Event 259 measures the amount of time the process waits for this space."};
		action = new String[] {"A high value for <CODE>Waits</CODE> for this event may indicate that some databases need larger log segments. A high value for the average <CODE>WaitTime</CODE> may indicate that you have not defined a threshold procedure, or that a procedure is taking a long time to free log space.",
		                       "Increasing the frequency of transaction dumps on the database or allocating more space to the log segment may reduce the value for <CODE>WaitTime</CODE>."};
		comment= new String[] {"Typical log suspend issue due to log full"};
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 260;
		slogan = "waiting for date or time in waitfor command";
		desc   = new String[] {"Event 260 is normal and expected when processes use the <CODE>waitfor</CODE> command."};
		action = new String[] {"When a process uses a <CODE>waitfor</CODE> command, Adaptive Server puts it to sleep until the requested time expires. Event 260 measures this amount of sleep time."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 266;
		slogan = "waiting for message in worker thread mailbox";
		desc   = new String[] {"Adaptive Server worker threads communicate with each other and the parent spid through an internal Adaptive Server mechanism called a mailbox. Event 266 measures the amount of time a worker process spends waiting for its mailbox to add a message."};
		action = new String[] {"To evaluate event 266, determine the number of parallel queries that were run from <CODE>monSysWorkerThread.ParallelQueries</CODE>. If the value for <CODE>WaitTime</CODE> is high per query, Adaptive Server may have a resource shortage (generally, CPU time). A high <CODE>WaitTime</CODE> value may also indicate unbalanced partitions on objects, causing some worker threads to wait for others to complete."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 271;
		slogan = "waiting for log space during ULC flush";
		desc   = null;
		action = null;
		comment= new String[] {"(rare) likely cause for this was that there was a cache stall in the data cache buffer pool containing the log. Coupled with WaitEventID=140 (above), this should help explain what a dedicated log cache is important for any high volume OLTP database"};
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 272;
		slogan = "waiting for lock on ULC";
		desc   = new String[] {"Each process allocates a user log cache (ULC) area, which is used to reduce contention on the last log page. Adaptive Server uses a lock to protect the ULC since more than one process can access the records of a ULC and force a flush. Event 272 measures the time the ULC spends waiting for that lock."};
		action = new String[] {"Typically, the average value for <CODE>WaitTime</CODE> for event 272 is quite low. A high value for average <CODE>WaitTime</CODE> may indicate high wait times for other events, forcing the ULC lock holder to wait. You can analyze other wait events to determine what is causing these waits."};
		comment= new String[] {"Mostly seen in high volume OLTP systems with a lot of write activity - usually a small value. Can be tuned via sp_configure �user log cache spinlock ratio� if it becomes a problem"};
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 334;
		slogan = "waiting for Lava pipe buffer for write";
		desc   = new String[] {"Adaptive Server version 15.0 introduced the lava query execution engine. When this engine executes a parallel query, it uses an internal structure called a \"pipe buffer\" to pass data between the worker processes. Event 334 measures the amount of time Adaptive Server spends waiting for a pipe buffer to be available."};
		action = new String[] {"The value for <CODE>WaitTime</CODE> should be low when processes execute properly. If this is not the case, contact Sybase Technical Support."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 374;
		slogan = "wait for lock pending/data pending to be cleared";
		desc   = new String[] {"A lock request is blocked because a required lock is unavailable at the local node and a currently pending request."};
		action = new String[] {"A high number of conflicting requests for the same lock across different nodes indicates that the application is issuing conflicting lock requests on the same object from multiple nodes. The lock emanates from the object coherency manager, which manages the metadata describing database objects across all of the instances in the cluster.",
		                       "You may improve performance by directing most requests for the object in question to a single node."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 375;
		slogan = "OCM wait for finishing BAST handling";
		desc   = new String[] {"One or more <code>ocm_lock</code> requests are blocked by a conflicting <code>ocm_lock</code> request from a different node."};
		action = new String[] {"A high value for this wait event means that different nodes are sending a high number of conflicting requests for the same <code>ocm_lock</code>. The application is probably issuing these conflicting lock requests from multiple nodes. You may improve performance by directing most requests for the <code>ocm_lock</code> to a single node."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 380;
		slogan = "lock/data pending to reset when OCM_ERR_DIDNTWAIT";
		desc   = new String[] {"If the cluster lock manager cannot immediately grant a lock request, it returns a <code>LOCK_DIDNTWAIT</code> message, which Adaptive Server translates as <code>OCM_ERR_DIDNT_WAIT</code>. The <code>ocm_lock</code> request goes to sleep until the AST returns from the master database with a response for the lock-request."};
		action = new String[] {"A high value for this wait event indicates there are numerous conflicting requests for the same <code>ocm_lock</code> across different nodes. This may be caused by the application issuing conflicting lock requests on the same object frommultiple nodes. You may improve performance by directing requests for the object in question to a single node."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 389;
		slogan = "OCM wait for pushing data flag to be cleared";
		desc   = new String[] {"Adaptive Server cannot process one or more <code>ocm_lock</code> requests because:",
		                       "<UL>",
		                       "<LI>Another lock request is holding the lock<BR>" +
		                         "   In this situation, a high value indicates there are numerous exclusive lock requests that need to perform heavy operations without losing the <code>ocm_lock</code>. This is an internal issue. <BR>" +
		                         "   To improve performance, determine why Adaptive Server is holding the lock for so long.<BR>" +
		                         "</LI>",
		                       "<LI>Another request is pushing the data. <BR>" +
		                         "   In this situation, a high value may indicate that data resides only in memory and Adaptive Server must regularly push this data to other nodes to avoid loosing data in the case of node failure. This adversely affects performance because Adaptive Server performs no useful work while it is pushing the data to other nodes. This is an internal isssue. <BR>" +
		                         "   To improve performance, determine why Adaptive Server is holding the lock or pushing the data for so long.<BR>" +
		                         "</LI>",
		                       "<LI>A canceled transaction is pending (the requested data is probably not being delivered, and the OCM on this node is taking corrective action).<BR>" +
		                         "   This wait event is rare (usually when an instance is expecting data from a node that just crashed). A high value for this wait event may result from a high rate of node or hardware failure. <BR>" +
		                         "</LI>",
		                       "</UL>"};
		action = null;
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 442;
		slogan = "waiting for cluster physical lock acquisition";
		desc   = new String[] {"To fix this situation:",
		                       "<UL>",
		                       "<LI>Check the CIPC send and receive queues - the wait time required for look-ups might increase if messages are stuck in the queues and not immediately processed.</LI>",
		                       "<LI>Check the network - congested networks can slow message transmission.</LI>",
		                       "<LI>Avoid the badly partitioned setting - enable trace flag 15815 for clusters that have more than two instances if the setting is well partitioned. This trace flag forces the affinity mode for master assignment, so the lock requester and the lock master are on the same instance and the lock request is managed by the current instance.</LI>",
		                       "</UL>"};
		action = null;
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 444;
		slogan = "waiting for cluster physical lock lookup";
		desc   = new String[] {"To fix this situation:",
		                       "<UL>",
		                       "<LI>Physical lock hash bucket size - large hash buckets may increase the amount of time required to look up locks.</LI>",
		                       "<LI>CIPC send and receive queues - the wait time required for look-ups might increase if messages are stuck in the queues and not immediately processed.</LI>",
		                       "<LI>Network - congested networks can slow message transmission.</LI>",
		                       "</UL>"};
		action = null;
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 449;
		slogan = "waiting for cluster object lock lookup";
		desc   = new String[] {"To solve the situation, check the:",
		                       "<UL>",
		                       "<LI>Object lock hash bucket size - large hash buckets may increase the amount of time required to look up locks.</LI>",
		                       "<LI>CIPC send and receive queues - the wait time required for look-ups might increase if messages are stuck in the queues and not immediately processed.</LI>",
		                       "<LI>Network - congested networks can slow message transmission.</LI>",
		                       "</UL>"};
		action = null;
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 454;
		slogan = "waiting for cluster logical lock lookup";
		desc   = new String[] {"To solve the situation, check the:",
		                       "<UL>",
		                       "<LI>Logical lock (row, page, and table) hash bucket size - large hash buckets may increase the amount of time required to look up locks.</LI>",
		                       "<LI>CIPC send and receive queues - the wait time required for look-ups might increase if messages are stuck in the queues and not immediately processed.</LI>",
		                       "<LI>Network - congested networks can slow message transmission.</LI>",
		                       "</UL>"};
		action = null;
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));



		event  = 483;
		slogan = "Waiting for ack of a multicast synchronous message";
		desc   = new String[] {"Although this wait event occurs with some frequency, it is a result of normal activity."};
		action = new String[] {"No action required."};
		comment= null;
		mcc    = null;
		add(new WaitEventIdRecord(event, name, slogan, desc, action, txtsrc, comment, mcc));
	}
}

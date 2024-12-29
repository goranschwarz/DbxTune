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
package com.dbxtune.config.dict;

import java.util.HashMap;

import com.dbxtune.utils.StringUtil;

public class WindowsPerformanceCountersDictionary
{
	/** Instance variable */
	private static WindowsPerformanceCountersDictionary _instance = null;

	private HashMap<String, Record> _map = new HashMap<>();
//	private HashMap<String, Record> _counterNameMap = new HashMap<>();

	public static class Record
	{
		private String _srvType         = null;
		private String _section         = null;
		private String _field           = null;
		private String _description     = null;

		public Record(String srvType, String section, String field, String description)
		{
			_srvType     = srvType;
			_section     = section;
			_field       = field;
			_description = description;
		}
		
		@Override
		public String toString()
		{
			return "srvType='"+_srvType+"', section='"+_section+"', field='"+_field+"', description='"+_description+"'.";
//			return StringUtil.left(_id, 50) + " - " + _description;
		}
	}


	public WindowsPerformanceCountersDictionary()
	{
		init();
	}

	public static WindowsPerformanceCountersDictionary getInstance()
	{
		if (_instance == null)
			_instance = new WindowsPerformanceCountersDictionary();
		return _instance;
	}

	/**
	 * Strips out all HTML and return it as a "plain" text
	 * @param waitName
	 * @return
	 */
	public String getDescriptionPlain(String section, String field)
	{
		Record rec = _map.get(section+"|"+field);
		if (rec != null)
			return StringUtil.stripHtml(rec._description);

		// Compose an empty one
		return "";
//		return "WaitName '"+waitName+"' not found in dictionary.";
	}

//	/**
//	 * Strips out all HTML and return it as a "plain" text
//	 * @param waitName
//	 * @return
//	 */
//	public String getDescriptionPlain(String counterName)
//	{
//		Record rec = _counterNameMap.get(counterName);
//		if (rec != null)
//			return StringUtil.stripHtml(rec._description);
//
//		// Compose an empty one
//		return "";
////		return "WaitName '"+waitName+"' not found in dictionary.";
//	}


//	public String getDescriptionHtml(String section, String field, String instance, String calculated_value)
//	{
//		Record rec = _map.get(section+"|"+field);
//		if (rec != null)
//		{
//			StringBuilder sb = new StringBuilder();
//			sb.append("<html>");
//			sb.append("<table cellpadding='0'>");
//			sb.append("    <tr> <td>    <b>Section               </b></td> <td>"    ).append(section)         .append("</td></tr>");
//			sb.append("    <tr> <td>    <b>Field                 </b></td> <td>"    ).append(field)           .append("</td></tr>");
//			if (StringUtil.hasValue(instance)) 
//				sb.append("<tr> <td>    <b>Instance              </b></td> <td>"    ).append(instance)        .append("</td></tr>");
//			sb.append("    <tr> <td><br><b>Description           </b></td> <td><br>").append(rec._description).append("</td></tr>");
//			sb.append("    <tr> <td>    <b>Calculated Value&nbsp;</b></td> <td>"    ).append(calculated_value).append("</td></tr>");
//			sb.append("</table>");
//			sb.append("<br>");
//			sb.append("<hr>");
//			sb.append("The above information was found at:<br>");
//			sb.append("<a href='https://docs.microsoft.com/en-us/sql/relational-databases/performance-monitor/use-sql-server-objects'>https://docs.microsoft.com/en-us/sql/relational-databases/performance-monitor/use-sql-server-objects</a>");
//			sb.append("</html>");
//			return sb.toString();
//		}
//
//		// Compose an empty one
//		return "<html><code>" + section + " - " + field + "</code> <br><b>not found in dictionary.</b></html>";
//	}


	private void set(Record rec)
	{
		String pk = rec._section+"|"+rec._field;
		
		Record old = _map.get(pk);
		if ( old != null)
		{
			System.out.println("Field '"+pk+"' already exists. It will be overwritten.");
			System.out.println("      >>> new record: "+rec);
			System.out.println("      >>> old record: "+old);
		}

		_map.put(pk, rec);
	}

//	private void setCounterName(Record rec)
//	{
//		String pk = rec._field;
//		
//		Record old = _counterNameMap.get(pk);
//		if ( old != null)
//		{
//			System.out.println("Field '"+pk+"' already exists. It will be overwritten.");
//			System.out.println("      >>> new record: "+rec);
//			System.out.println("      >>> old record: "+old);
//		}
//
//		_counterNameMap.put(pk, rec);
//	}

	private void add(String srvType, String section, String field, String desc)
	{
		Record rec = new Record(srvType, section, field, desc);
		set(rec);
//		setCounterName(rec);
	}

	private void init()
	{
		// The below descriptions was fetched from:
		// https://docs.microsoft.com/en-us/previous-versions/windows/it-pro/windows-server-2003/cc776376(v=ws.10)
		
		// ################################################################
		// Physical Disk
		// ################################################################
		add("OS", "PhysicalDisk", "% Disk Read Time", "Shows the percentage of time that the selected disk drive was busy servicing read requests.");
		add("OS", "PhysicalDisk", "% Disk Time", "Shows the percentage of elapsed time that the selected disk drive was busy servicing read or write requests.");
		add("OS", "PhysicalDisk", "% Disk Write Time", "Shows the percentage of elapsed time that the selected disk drive was busy servicing write requests.");
		add("OS", "PhysicalDisk", "% Idle Time", "Shows the percentage of elapsed time during the sample interval that the selected disk drive was idle.");
		add("OS", "PhysicalDisk", "Avg. Disk Bytes/Read", "Shows the average number of bytes that were transferred from the disk during read operations.");
		add("OS", "PhysicalDisk", "Avg. Disk Bytes/Transfer", "Shows the average number of bytes that were transferred to or from the disk during write or read operations.");
		add("OS", "PhysicalDisk", "Avg. Disk Bytes/Write", "Shows the average number of bytes that were transferred to the disk during write operations.");
		add("OS", "PhysicalDisk", "Avg. Disk Queue Length", "Shows the average number of both read and write requests that were queued for the selected disk during the sample interval.");
		add("OS", "PhysicalDisk", "Avg. Disk Read Queue Length", "Shows the average number of read requests that were queued for the selected disk during the sample interval.");
		add("OS", "PhysicalDisk", "Avg. Disk sec/Read", "Shows the average time, in seconds, of a read of data from the disk.");
		add("OS", "PhysicalDisk", "Avg. Disk sec/Transfer", "Shows the average time, in seconds, of a disk transfer.");
		add("OS", "PhysicalDisk", "Avg. Disk sec/Write", "Shows the average time, in seconds, of a write of data to the disk.");
		add("OS", "PhysicalDisk", "Avg. Disk Write Queue Length", "Shows the average number of write requests that were queued for the selected disk during the sample interval.");
		add("OS", "PhysicalDisk", "Current Disk Queue Length", "Shows the number of requests that were outstanding on the disk at the time that the performance data was collected. This is a snapshot, not an average over the time interval. It includes requests in service at the time of the collection. Multispindle disk devices can have multiple requests active at one time, but other concurrent requests are awaiting service. This counter might reflect a transitory high or low queue length, but if this counter is consistently high, then it is likely that there is a sustained load on the disk drive. Requests experience delays proportional to the length of this queue, minus the number of spindles on the disks. This difference should average less than two.");
		add("OS", "PhysicalDisk", "Disk Bytes/sec", "Shows the rate, in incidents per second, at which bytes were transferred to or from the disk during write or read operations.");
		add("OS", "PhysicalDisk", "Disk Read Bytes/sec", "Shows the rate, in incidents per second, at which bytes were transferred from the disk during read operations.");
		add("OS", "PhysicalDisk", "Disk Reads/sec", "Shows the rate, in incidents per second, at which read operations were performed on the disk.");
		add("OS", "PhysicalDisk", "Disk Transfers/sec", "Shows the rate, in incidents per second, at which read and write operations were performed on the disk.");
		add("OS", "PhysicalDisk", "Disk Write Bytes/sec", "Shows the rate, in incidents per second, at which bytes were transferred to the disk during write operations.");
		add("OS", "PhysicalDisk", "Disk Writes/sec", "Shows the rate, in incidents per second, at which write operations were performed on the disk.");
		add("OS", "PhysicalDisk", "Split IO/sec", "Shows the rate, in incidents per second, at which input/output (I/O) requests to the disk were split into multiple requests. A split I/O might result from requesting data in a size that is too large to fit into a single I/O, or from a fragmented disk subsystem.");
		add("OS", "PhysicalDisk", "Split IO/Sec", "Shows the rate, in incidents per second, at which input/output (I/O) requests to the disk were split into multiple requests. A split I/O might result from requesting data in a size that is too large to fit into a single I/O, or from a fragmented disk subsystem.");
		// I added "Split IO/Sec", this is a duplicate of "Split IO/sec"

		// ################################################################
		// Processor
		// ################################################################
		add("OS", "Processor", "% C1 Time", "Shows the percentage of time that the processor spent in the C1 low-power idle state. % C1 Time is a subset of the total processor idle time. C1 low-power idle state enables the processor to maintain its entire context and quickly return to the running state. Not all systems support the C1 state.");
		add("OS", "Processor", "% C2 Time", "Shows the percentage of time that the processor spent in the C2 low-power idle state. % C2 Time is a subset of the total processor idle time. C2 low-power idle state enables the processor to maintain the context of the system caches. The C2 power state is a lower power and higher exit latency state than C1. Not all systems support the C2 state.");
		add("OS", "Processor", "% C3 Time", "Shows the percentage of time that the processor spent in the C3 low-power idle state. % C3 Time is a subset of the total processor idle time. When the processor is in the C3 low-power idle state it is unable to maintain the coherency of its caches. The C3 power state is a lower power and higher exit latency state than C2. Not all systems support the C3 state.");
		add("OS", "Processor", "% DPC Time", "Shows the percentage of time that the processor spent receiving and servicing deferred procedure calls (DPCs) during the sample interval. DPCs are interrupts that run at a lower priority than standard interrupts. % DPC Time is a component of % Privileged Time because DPCs are executed in privileged mode. They are counted separately and are not a component of the interrupt counters. This counter displays the average busy time as a percentage of the sample time.");
		add("OS", "Processor", "% Idle Time", "Shows the time that the processor was idle during the sample interval.");
		add("OS", "Processor", "% Interrupt Time", "Shows the percentage of time that the processor spent receiving and servicing hardware interrupts during the sample interval. This value is an indirect indicator of the activity of devices that generate interrupts, such as the system clock, the mouse, disk drivers, data communication lines, network interface cards and other peripheral devices. These devices normally interrupt the processor when they have completed a task or require attention. Normal thread execution is suspended during interrupts. Most system clocks interrupt the processor every 10 milliseconds, creating a background of interrupt activity. This counter displays the average busy time as a percentage of the sample time.");
		add("OS", "Processor", "% Privileged Time", "Shows the percentage of elapsed time that this thread spent executing code in privileged mode. When a Windows Server 2003 family operating system service is called, the service often runs in privileged mode in order to gain access to system-private data. Such data is protected from access by threads executing in user mode. Calls to the system can be explicit or implicit, such as page faults and interrupts. Unlike some early operating systems, Windows Server 2003 uses process boundaries for subsystem protection in addition to the traditional protection of user and privileged modes. These subsystem processes provide additional protection. Therefore, some work done by Windows Server 2003 on behalf of your application might appear in other subsystem processes in addition to the privileged time in your process.");
		add("OS", "Processor", "% Processor Time", "Shows the percentage of elapsed time that this thread used the processor to execute instructions. An instruction is the basic unit of execution in a processor, and a thread is the object that executes instructions. Code executed to handle some hardware interrupts and trap conditions is included in this count.");
		add("OS", "Processor", "% User Time", "Shows the percentage of elapsed time that this thread spent executing code in user mode. Applications, environment subsystems, and integral subsystems execute in user mode. Code executing in user mode cannot damage the integrity of the Windows Server 2003 Executive, Kernel, and device drivers. Unlike some early operating systems, Windows Server 2003 uses process boundaries for subsystem protection in addition to the traditional protection of user and privileged modes. These subsystem processes provide additional protection. Therefore, some work done by Windows Server 2003 on behalf of your application might appear in other subsystem processes in addition to the privileged time in your process.");
		add("OS", "Processor", "C1 Transitions/sec", "Shows the rate, in incidents per second, at which the CPU entered the C1 low-power idle state. The CPU enters the C1 state when it is sufficiently idle, and exits this state on any interrupt. This counter displays the difference between the values observed in the last two samples, divided by the duration of the sample interval.");
		add("OS", "Processor", "C2 Transitions/sec", "Shows the rate, in incidents per second, at which the CPU entered the C2 low-power idle state. The CPU enters the C2 state when it is sufficiently idle, and exits this state on any interrupt. This counter displays the difference between the values observed in the last two samples, divided by the duration of the sample interval.");
		add("OS", "Processor", "C3 Transitions/sec", "Shows the rate, in incidents per second, at which the CPU entered the C3 low-power idle state. The CPU enters the C3 state when it is sufficiently idle, and exits this state on any interrupt. This counter displays the difference between the values observed in the last two samples, divided by the duration of the sample interval.");
		add("OS", "Processor", "DPC Rate", "Shows the rate at which deferred procedure calls (DPCs) were added to the processor's DPC queue between the timer ticks of the processor clock. DPCs are interrupts that run at a lower priority than standard interrupts. Each processor has its own DPC queue. This counter measures the rate at which DPCs were added to the queue, not the number of DPCs in the queue. This counter displays the last observed value only; it is not an average.");
		add("OS", "Processor", "DPCs Queued/sec", "Shows the average rate, in incidents per second, at which deferred procedure calls (DPCs) were added to the processor's DPC queue. DPCs are interrupts that run at a lower priority than standard interrupts. Each processor has its own DPC queue. This counter measures the rate at which DPCs were added to the queue, not the number of DPCs in the queue. This counter displays the difference between the values observed in the last two samples, divided by the duration of the sample interval.");
		add("OS", "Processor", "Interrupts/sec", "Shows the rate, in incidents per second, at which the processor received and serviced hardware interrupts. It does not include deferred procedure calls (DPCs), which are counted separately. This value is an indirect indicator of the activity of devices that generate interrupts, such as the system clock, the mouse, disk drivers, data communication lines, network interface cards, and other peripheral devices. These devices normally interrupt the processor when they have completed a task or require attention. Normal thread execution is suspended during interrupts. Most system clocks interrupt the processor every 10 milliseconds, creating a background of interrupt activity. This counter displays the difference between the values observed in the last two samples, divided by the duration of the sample interval.");


		// ################################################################
		// Paging File
		// ################################################################
		add("OS", "Paging File", "Usage Peak",   "Shows the amount of the page file instance in use.");
		add("OS", "Paging File", "% Usage Peak", "Shows the greatest percentage of the paging file that was in use during the sample interval.");


		// ################################################################
		// System
		// ################################################################
		add("OS", "System", "% Registry Quota In Use"     ,"Shows the percentage of the Total Registry Quota Allowed that was being used by the system.");
		add("OS", "System", "Alignment Fixups/sec"        ,"Shows the rate, in incidents per second, at which alignment faults were fixed by the system.");
		add("OS", "System", "Context Switches/sec"        ,"Shows the combined rate, in incidents per second, at which all processors on the computer were switched from one thread to another. It is the sum of the values of Thread Context Switches/sec for each thread running on all processors on the computer, and is measured in numbers of switches. Context switches occur when a running thread voluntarily relinquishes the processor, or is preempted by a higher priority, ready thread.");
		add("OS", "System", "Exception Dispatches/sec"    ,"Shows the rate, in incidents per second, at which exceptions were dispatched by the system.");
		add("OS", "System", "File Control Bytes/sec"      ,"Shows the overall rate, in incidents per second, at which bytes were transferred for all file system operations that were neither read nor write operations, such as file system control requests and requests for information about device characteristics or status.");
		add("OS", "System", "File Control Operations/sec" ,"Shows the combined rate, in incidents per second, of file system operations that were neither read nor write operations, such as file system control requests and requests for information about device characteristics or status. This is the inverse of File Data Operations/sec.");
		add("OS", "System", "File Data Operations/sec"    ,"Shows the combined rate, in incidents per second, of read and write operations on disks, serial, or parallel devices. This is the inverse of File Control Operations/sec.");
		add("OS", "System", "File Read Bytes/sec"         ,"Shows the overall rate, in incidents per second, at which bytes were read to satisfy file system read requests to all devices on the computer, including read operations from the file system cache.");
		add("OS", "System", "File Read Operations/sec"    ,"Shows the combined rate, in incidents per second, of file system read requests to all devices on the computer, including requests to read from the file system cache.");
		add("OS", "System", "File Write Bytes/sec"        ,"Shows the overall rate, in incidents per second, at which bytes were written to satisfy file system write requests to all devices on the computer, including write operations to the file system cache.");
		add("OS", "System", "File Write Operations/sec"   ,"Shows the combined rate, in incidents per second, of file system write requests to all devices on the computer, including requests to write to data in the file system cache.");
		add("OS", "System", "Floating Emulations/sec"     ,"Shows the rate, in incidents per second, of floating emulations performed by the system.");
		add("OS", "System", "Processes"                   ,"Shows the number of processes in the computer at the time of data collection. This is an instantaneous count, not an average over the time interval. Each process represents a program that is running.");
		add("OS", "System", "Processor Queue Length"      ,"Shows the number of threads in the processor queue. Unlike the disk counters, this counter shows ready threads only, not threads that are running. There is a single queue for processor time, even on computers with multiple processors. Therefore, if a computer has multiple processors, you need to divide this value by the number of processors servicing the workload. A sustained processor queue of greater than two threads generally indicates processor congestion.");
		add("OS", "System", "System Calls/sec"            ,"Shows the combined rate, in incidents per second, of calls to operating system service routines by all processes running on the computer. These routines perform all of the basic scheduling and synchronization of activities on the computer, and provide access to nongraphic devices, memory management, and name space management.");
		add("OS", "System", "System Up Time"              ,"Shows the total time, in seconds, that the computer has been operational since it was last started.");
		add("OS", "System", "Threads"                     ,"Shows the number of threads in the computer at the time of data collection. This is an instantaneous count, not an average over the time interval. A thread is the basic executable entity that can execute instructions in a processor.");

		
		// ################################################################
		// Memory
		// ################################################################
		add("OS", "Memory", "% Committed Bytes In Use"        ,"Shows the ratio of Committed Bytes to the Commit Limit. Committed memory is physical memory in use, for which space has been reserved in the paging file(s) so that it can be written to disk. The commit limit is determined by the size of the paging file. If the paging file is enlarged, the commit limit increases, and the ratio is reduced.");
		add("OS", "Memory", "Available Bytes"                 ,"Shows the amount of physical memory, in bytes, immediately available for allocation to a process or for system use. It is equal to the sum of memory assigned to the standby (cached), free, and zero page lists.");
		add("OS", "Memory", "Available KBytes"                ,"Shows the amount of physical memory, in Kilobytes, immediately available for allocation to a process or for system use. It is equal to the sum of memory assigned to the standby (cached), free, and zero page lists.");
		add("OS", "Memory", "Available MBytes"                ,"Shows the amount of physical memory, in Megabytes, immediately available for allocation to a process or for system use. It is equal to the sum of memory assigned to the standby (cached), free, and zero page lists.");
		add("OS", "Memory", "Cache Bytes"                     ,"Shows the sum of the values of System Cache Resident Bytes, System Driver Resident Bytes, System Code Resident Bytes, and Pool Paged Resident Bytes.");
		add("OS", "Memory", "Cache Bytes Peak"                ,"Shows the maximum number of bytes used by the file system cache since the system was last started. This might be larger than the current size of the cache.");
		add("OS", "Memory", "Cache Faults/sec"                ,"Shows the rate, in incidents per second, at which faults occured when a page that was sought in the file system cache was not found and was be retrieved either from elsewhere in memory (a soft fault) or from disk (a hard fault). This counter shows the total number of faults, without regard for the number of pages faulted in each operation.");
		add("OS", "Memory", "Commit Limit"                    ,"Shows the amount of virtual memory, in bytes, that can be committed without having to extend the paging file(s). Committed memory is physical memory that has space reserved on the disk paging file(s). There can be one or more paging files on each physical drive. If the paging file(s) are expanded, this limit increases accordingly.");
		add("OS", "Memory", "Committed Bytes"                 ,"Shows the amount of committed virtual memory, in bytes.");
		add("OS", "Memory", "Demand Zero Faults/sec"          ,"Shows the average rate, in incidents per second, at which page faults required a zeroed page to satisfy the fault. This counter displays the difference between the values observed in the last two samples, divided by the duration of the sample interval. Zeroed pages (pages emptied of previously stored data and filled with zeroes) prevent processes from seeing data stored by earlier processes that used the same memory space. This counter displays the number of faults, without regard to the number of pages retrieved to satisfy the fault.");
		add("OS", "Memory", "Free System Page Table Entries"  ,"Shows the number of page table entries not in use by the system.");
		add("OS", "Memory", "Page Faults/sec"                 ,"Shows the average number of pages faulted per second, which is equal to the number of page fault operations because only one page is faulted in each fault operation. This counter includes both hard faults (those that require disk access) and soft faults (where the faulted page is found elsewhere in physical memory). Most processors can handle large numbers of soft faults without significant consequence. However, hard faults can cause delaysbecause they require disk access..");
		add("OS", "Memory", "Page Reads/sec"                  ,"Shows the rate, in incidents per second, at which the disk was read to resolve hard page faults. This counter shows numbers of read operations, without regard to the number of pages retrieved in each operation. Hard page faults occur when a process references a page in virtual memory that must be retrieved from disk because it is not in its working set or elsewhere in physical memory. This counter is a primary indicator for the kinds of faults that cause system-wide delays. It includes read operations to satisfy faults in the file system cache (usually requested by applications) and in noncached mapped memory files. Compare the value of Page Reads/sec to the value of Pages Input/sec to find an average of how many pages were read during each read operation.");
		add("OS", "Memory", "Page Writes/sec"                 ,"Shows the rate, in incidents per second, at which pages were written to disk to free up space in physical memory. Pages are written to disk only if they are changed while in physical memory, so they are likely to hold data, not code. This counter shows write operations, without regard to the number of pages written in each operation.");
		add("OS", "Memory", "Pages Input/sec"                 ,"Shows the rate, in incidents per second, at which pages were read from disk to resolve hard page faults. Hard page faults occur when a process refers to a page in virtual memory that must be retrieved from disk because it is not in its working set or elsewhere in physical memory. When a page is faulted, the system tries to read multiple contiguous pages into memory to maximize the benefit of the read operation. Compare Pages Input/sec to Page Reads/sec to find the average number of pages read into memory during each read operation");
		add("OS", "Memory", "Pages Output/sec"                ,"Shows the rate, in incidents per second, at which pages were written to disk to free up space in physical memory. A high rate of pages output might indicate a memory shortage. The Windows Server 2003 family writes more pages back to disk to free up space when physical memory is in short supply. This counter shows numbers of pages, and can be compared to other counts of pages without conversion.");
		add("OS", "Memory", "Pages/sec"                       ,"Shows the rate, in incidents per second, at which pages were read from or written to disk to resolve hard page faults. This counter is a primary indicator for the kinds of faults that cause system-wide delays. It is the sum of Pages Input/sec and Pages Output/sec. It is counted in numbers of pages, so it can be directly compared to other counts of pages such as Page Faults/sec. It includes pages retrieved to satisfy faults in the file system cache (usually requested by applications) and noncached mapped memory files.");
		add("OS", "Memory", "Pool Nonpaged Allocs"            ,"Shows the number of calls to allocate space in the nonpaged pool. This counter is measured in numbers of calls to allocate space, regardless of the amount of space allocated in each call.");
		add("OS", "Memory", "Pool Nonpaged Bytes"             ,"Shows the size, in bytes, of the nonpaged pool. Pool Nonpaged Bytes is calculated differently than Process\\Pool Nonpaged Bytes, so it might not equal Process(_Total )\\Pool Nonpaged Bytes.");
		add("OS", "Memory", "Pool Paged Allocs"               ,"Shows the number of calls to allocate space in the paged pool. This counter is measured in numbers of calls to allocate space, regardless of the amount of space allocated in each call.");
		add("OS", "Memory", "Pool Paged Bytes"                ,"Shows the size, in bytes, of the paged pool. Pool Paged Bytes is calculated differently than Process\\Pool Paged Bytes, so it might not equal Process(_Total )\\Pool Paged Bytes.");
		add("OS", "Memory", "Pool Paged Resident Bytes"       ,"Shows the size, in bytes, of the paged pool. Space used by the paged and nonpaged pools is taken from physical memory, so a pool that is too large denies memory space to processes.");
		add("OS", "Memory", "System Cache Resident Bytes"     ,"Shows the size, in bytes, of pageable operating system code in the file system cache. This value includes only current physical pages and does not include any virtual memory pages that are not currently resident. It does not equal the System Cache value shown in Task Manager. As a result, this value may be smaller than the actual amount of virtual memory in use by the file system cache. This value is a component of System Code Resident Bytes that represents all pageable operating system code that is currently in physical memory.");
		add("OS", "Memory", "System Code Resident Bytes"      ,"Shows the size, in bytes, of operating system code currently in physical memory that can be written to disk when not in use. This value is a component of System Code Total Bytes, which also includes operating system code on disk. System Code Resident Bytes (and System Code Total Bytes) does not include code that must remain in physical memory.");
		add("OS", "Memory", "System Code Total Bytes"         ,"Shows the size, in bytes, of pageable operating system code currently in virtual memory. It is a measure of the amount of physical memory being used by the operating system that can be written to disk when not in use. This value is calculated by adding the bytes in Ntoskrnl.exe, Hal.dll, the boot drivers, and file systems loaded by Ntldr/osloader. This counter does not include code that must remain in physical memory.");
		add("OS", "Memory", "System Driver Resident Bytes"    ,"Shows the size, in bytes, of pageable physical memory being used by device drivers. The counter is the working set (physical memory area) of the drivers. This value is a component of System Driver Total Bytes, which also includes driver memory that has been written to disk. Neither System Driver Resident Bytes nor System Driver Total Bytes includes memory that cannot be written to disk.");
		add("OS", "Memory", "System Driver Total Bytes"       ,"Shows the size, in bytes, of pageable virtual memory currently being used by device drivers. Pageable memory can be written to disk when it is not being used. It includes physical memory (System Driver Resident Bytes) and code and data written to disk. This counter is a component of System Code Total Bytes.");
		add("OS", "Memory", "Transition Faults/sec"           ,"Shows the rate, in incidents per second, at which page faults were resolved by recovering pages without additional disk activity, including pages that were being used by another process sharing the page, or that were on the modified page list or the standby list, or that were being written to disk at the time of the page fault. This counter is also equal to the number of pages faulted because only one page is faulted in each operation.");
		add("OS", "Memory", "Write Copies/sec"                ,"Shows the rate, in incidents per second, at which page faults were caused by attempts to write that were satisfied by copying the page from elsewhere in physical memory. This is an economical way of sharing data since pages are only copied when they are written to; otherwise, the page is shared. This counter shows the number of copies, without regard to the number of pages copied in each operation.");

		// counters merged from "Paging File" -->> "Memory"
		add("OS", "Memory", "Paging File(_Total) - % Usage"      ,"The percentage of the system page file that is currently in use. This is not directly related to performance, but you can run into serious application issues if the page file does become completely full and additional memory is still being requested by applications.");
		add("OS", "Memory", "Paging File(_Total) - % Usage Peak" ,"Shows the greatest percentage of the paging file that was in use during the sample interval.");
		
		
		// ################################################################
		// Cache
		// ################################################################
		add("OS", "Cache", "Async Copy Reads/sec", "Shows the rate at which read operations from pages of the file system cache involve a memory copy of the data from the cache to the application's buffer. The application regains control immediately, even if the disk must be accessed to retrieve the page.");
		add("OS", "Cache", "Async Data Maps/sec", "Shows the rate at which an application that uses a file system (such as the NTFS file system) to map a page of a file into the file system cache does not wait for the page to be retrieved, if the page is not in main memory.");
		add("OS", "Cache", "Async Fast Reads/sec", "Shows the rate at which read operations from the file system cache bypass the installed file system and retrieve the data directly from the cache. Normally, file I/O requests invoke the file system to retrieve data from a file, but this path permits data to be retrieved from the cache directly (without file system involvement) if the data is in the cache. Even if the data is not in the cache, one invocation of the file system is avoided. If the data is not in the cache, the request does not wait until the data has been retrieved from the disk, but gets control immediately.");
		add("OS", "Cache", "Async MDL Reads/sec", "Shows the rate at which read operations from the file system cache use a memory descriptor list (MDL) to access the pages. The MDL contains the physical address of each page in the transfer, thus permitting direct memory access (DMA) of the pages. If the accessed pages are not in main memory, the calling application program does not wait for the pages to be retrieved from disk through a page fault.");
		add("OS", "Cache", "Async Pin Reads/sec", "Shows the rate at which data is read into the file system cache before writing the data back to disk. Pages read in this fashion are \"pinned\" in memory at the completion of the read. Pinned pages are those that are read into the file system cache before the system writes data back to the disk at the completion of the read. The file system regains control immediately, even if the disk must be accessed to retrieve the page. While pinned, a page's physical address is not altered.");
		add("OS", "Cache", "Copy Read Hits %", "Shows the percentage of cache copy read requests that did not require a disk read to access the page in the cache. A copy read is a file read operation that is satisfied by a memory copy from a page in the cache to the application's buffer. The LAN redirector uses this method for retrieving information from the cache, as does the LAN server for small transfers. This method is also used by the disk file systems.");
		add("OS", "Cache", "Copy Reads/sec", "Shows the rate at which read operations from pages of the file system cache involve a copy read.");
		add("OS", "Cache", "Data Flush Pages/sec", "Shows the rate at which the file system cache has flushed its contents to disk in response to a request to flush, or to satisfy a write-through file write request. More than one page can be transferred on each flush operation.");
		add("OS", "Cache", "Data Flushes/sec", "Shows the rate at which the file system cache has flushed its contents to disk in response to a request to flush, or to satisfy a write-through file write request.");
		add("OS", "Cache", "Data Map Hits %", "Shows the percentage of data maps in the file system cache that can be resolved without having to retrieve a page from the disk, because the page is already in physical memory.");
		add("OS", "Cache", "Data Map Pins/sec", "Shows the rate at which data maps in the file system cache resulted in pinning a page in main memory.");
		add("OS", "Cache", "Data Maps/sec", "Shows the rate at which a file system, such as NTFS, maps a page of a file into the file system cache to read the page.");
		add("OS", "Cache", "Fast Read Not Possibles/sec", "Shows the rate at which an application programming interface (API) function call attempts to bypass the file system to get to data in the file system cache that required invoking the file system.");
		add("OS", "Cache", "Fast Read Resource Misses/sec", "Shows the rate at which cache misses occur because there are not enough resources to satisfy the request.");
		add("OS", "Cache", "Fast Reads/sec", "Shows the rate at which read operations from the file system cache bypass the installed file system and retrieve the data directly from the cache.");
		add("OS", "Cache", "Lazy Write Flushes/sec", "Shows the rate at which the Lazy Writer thread writes to disk.");
		add("OS", "Cache", "Lazy Write Pages/sec", "Shows the rate at which the Lazy Writer thread has written to disk.");
		add("OS", "Cache", "MDL Read Hits %", "Shows the percentage of MDL read requests to the file system cache that did not have to access the disk to provide memory access to the page or pages in the cache.");
		add("OS", "Cache", "MDL Reads/sec", "Shows the rate at which read operations from the file system cache use an MDL to access the data. The LAN server uses this method for large transfers out of the server.");
		add("OS", "Cache", "Pin Read Hits %", "Shows the percentage of pin read requests that did not have to access the disk to provide access to the page in the file system cache.");
		add("OS", "Cache", "Pin Reads/sec", "Shows the rate at which data is read into the file system cache before writing the data back to disk. Pages read in this fashion are pinned in memory at the completion of the read.");
		add("OS", "Cache", "Read Aheads/sec", "Shows the rate at which read operations from the file system cache detect sequential access to a file. Read aheads permit data to be transferred in larger blocks than those being requested by the application, reducing the overhead per access.");
		add("OS", "Cache", "Sync Copy Reads/sec", "Shows the rate at which read operations from pages of the file system cache involve a memory copy of the data from the cache to the application's buffer. The file system does not regain control until the copy operation is complete, even if the disk must be accessed to retrieve the page.");
		add("OS", "Cache", "Sync Data Maps/sec", "Shows the rate at which a file system (such as NTFS) maps a page of a file into the file system cache to read the page, and — if the page is not in main memory — waits for it to be retrieved.");
		add("OS", "Cache", "Sync Fast Reads/sec", "Shows the rate at which read operations from the file system cache bypass the installed file system and retrieve the data directly from the cache.");
		add("OS", "Cache", "Sync MDL Reads/sec", "Shows the rate at which read operations from the file system cache use an MDL to access the pages.");
		add("OS", "Cache", "Sync Pin Reads/sec", "Shows the rate at which data is read into the file system cache before it is written back to disk. Pages read in this fashion are pinned in memory at the completion of the read.");

	
		// ################################################################
		// LogicalDisk
		// ################################################################
		add("OS", "LogicalDisk", "% Disk Time", "Shows the percentage of time that the selected disk drive was busy servicing read or write requests.");
		add("OS", "LogicalDisk", "% Disk Read Time", "Shows the percentage of time that the selected disk drive was busy servicing read requests.");
		add("OS", "LogicalDisk", "% Disk Write Time", "Shows the percentage of time that the selected disk drive was busy servicing write requests.");
		add("OS", "LogicalDisk", "% Free Space", "Shows the percentage of the total usable space on the selected logical disk drive that was free.");
		add("OS", "LogicalDisk", "Avg. Disk Bytes/Read", "Shows the average number of bytes transferred from the disk during read operations.");
		add("OS", "LogicalDisk", "Avg. Disk Bytes/Transfer", "Shows the average number of bytes transferred to or from the disk during write or read operations.");
		add("OS", "LogicalDisk", "Avg. Disk Bytes/Write", "Shows the average number of bytes transferred to the disk during write operations.");
		add("OS", "LogicalDisk", "Avg. Disk Queue Length", "Shows the average number of both read and write requests that were queued for the selected disk during the sample interval.");
		add("OS", "LogicalDisk", "Avg. Disk Read Queue Length", "Shows the average number of read requests that were queued for the selected disk during the sample interval.");
		add("OS", "LogicalDisk", "Avg. Disk sec/Read", "Shows the average time, in seconds, of a read operation from the disk.");
		add("OS", "LogicalDisk", "Avg. Disk sec/Transfer", "Shows the time, in seconds, of the average disk transfer.");
		add("OS", "LogicalDisk", "Avg. Disk sec/Write", "Shows the average time, in seconds, of a write operation to the disk.");
		add("OS", "LogicalDisk", "Avg. Disk Write Queue Length", "Shows the average number of write requests that were queued for the selected disk during the sample interval.");
		add("OS", "LogicalDisk", "Current Disk Queue Length", "Shows the number of requests outstanding on the disk at the time that the performance data is collected. It also includes requests in service at the time of the collection. This is a snapshot, not an average over a time interval. Multispindle disk devices can have multiple requests that are active at one time, but other concurrent requests are awaiting service. This counter might reflect a transitory high or low queue length, but if there is a sustained load on the disk drive, it is likely that this will be consistently high. Requests experience delays that are proportional to the length of this queue, minus the number of spindles on the disks. For good performance, this difference should average less than two.");
		add("OS", "LogicalDisk", "Disk Bytes/sec", "Shows the rate, in incidents per second, at which bytes were transferred to or from the disk during write or read operations.");
		add("OS", "LogicalDisk", "Disk Read Bytes/sec", "Shows the rate, in incidents per second, at which bytes were transferred from the disk during read operations.");
		add("OS", "LogicalDisk", "Disk Reads/sec", "Shows the rate, in incidents per second, at which read operations were performed on the disk.");
		add("OS", "LogicalDisk", "Disk Transfers/sec", "Shows the rate, in incidents per second, at which read and write operations were performed on the disk.");
		add("OS", "LogicalDisk", "Disk Write Bytes/sec", "Shows the rate, in incidents per second, at which bytes were transferred to the disk during write operations.");
		add("OS", "LogicalDisk", "Disk Writes/sec", "Shows the rate, in incidents per second, at which write operations were performed on the disk.");
		add("OS", "LogicalDisk", "Free Megabytes", "Shows the unallocated space, in megabytes, on the disk drive. One megabyte is equal to 1,048,576 bytes.");

		
		// ################################################################
		// Objects
		// ################################################################
		add("OS", "Objects", "Events", "Shows the number of events in the computer at the time of data collection. An event occurs when two or more threads try to synchronize execution. This is an instantaneous count, not an average over time.");
		add("OS", "Objects", "Mutexes", "Shows the number of mutexes in the computer at the time of data collection. Mutexes are executive dispatcher objects that ensure threads are synchronized. Mutexes are used by threads to assure that only one thread is executing a particular section of code. This is an instantaneous count, not an average over time.");
		add("OS", "Objects", "Processes", "Shows the number of processes in the computer at the time of data collection. Each process represents the running of a program. This is an instantaneous count, not an average over time.");
		add("OS", "Objects", "Sections", "Shows the number of sections in the computer at the time of data collection. A section is a portion of virtual memory created by a process for storing data. A process can share sections with other processes. This is an instantaneous count, not an average over time.");
		add("OS", "Objects", "Semaphores", "Shows the number of semaphores in the computer at the time of data collection. Threads use semaphores to obtain exclusive access to data structures that they share with other threads. This is an instantaneous count, not an average over time.");
		add("OS", "Objects", "Threads", "Shows the number of threads in the computer at the time of data collection. A thread is the basic executable entity that can execute instructions in a processor. This is an instantaneous count, not an average over time.");

		
		// ################################################################
		// Process
		// ################################################################
		add("OS", "Process", "% Privileged Time", "Shows the percentage of non-idle processor time spent executing code in privileged mode. Privileged mode is a processing mode designed for operating system components and hardware-manipulating drivers. It allows direct access to hardware and memory. The alternative, user mode, is a restricted processing mode designed for applications, environmental subsystems, and integral subsystems. The operating system switches application threads to privileged mode to access operating system services. % Privileged Time includes time servicing interrupts and DPCs. A high rate of privileged time might be attributed to a large number of interrupts generated by a failing device. When a Windows system service is called, the service often runs in privileged mode to gain access to system-private data. Such data is protected from access by threads executing in user mode. Calls to the system can be explicit or implicit, such as page faults or interrupts. Unlike some earlier Windows operating systems, Windows 2000 and the Windows Server 2003 family use process boundaries for subsystem protection in addition to the traditional protection of user and privileged modes. These subsystem processes provide additional protection. Therefore, some work done by Windows 2000 and Windows Server 2003 on behalf of your application might appear in other subsystem processes in addition to the privileged time in your process.");
		add("OS", "Process", "% Processor Time", "Shows the percentage of time that the processor spent executing a non-idle thread. It is calculated by measuring the duration that the idle thread is active during the sample interval, and subtracting that time from 100 %. (Each processor has an idle thread that consumes cycles when no other threads are ready to run.) This counter is the primary indicator of processor activity, and displays the average percentage of busy time observed during the sample interval. Code executed to handle some hardware interrupts and trap conditions are included in this count.");
		add("OS", "Process", "% User Time", "Shows the percentage of time that the processor spent executing code in user mode. Applications, environment subsystems, and integral subsystems execute in user mode. Code executing in user mode cannot damage the integrity of the Windows Executive, kernel, and/or device drivers. Unlike some earlier Windows operating systems, Windows 2000 and Windows Server 2003 use process boundaries for subsystem protection in addition to the traditional protection of user and privileged modes. These subsystem processes provide additional protection. Therefore, some work done by Windows 2000 and Windows Server 2003 on behalf of your application might appear in other subsystem processes in addition to the privileged time in your process.");
		add("OS", "Process", "Page Faults/sec", "Shows the rate, in incidents per second, at which page faults were handled by the processor. A page fault occurs when a process requires code or data that is not in its working set. This counter includes both hard faults (those that require disk access) and soft faults (those where the faulted page is found elsewhere in physical memory). Most processors can handle large numbers of soft faults without consequence. However, hard faults can cause significant delays.");
		add("OS", "Process", "Page File Bytes", "Shows the the current amount of virtual memory, in bytes, that a process has reserved for use in the paging file(s). Paging files are used to store pages of memory used by the process. Paging files are shared by all processes, and the lack of space in paging files can prevent other processes from allocating memory. If there is no paging file, this counter reflects the current amount of virtual memory that the process has reserved for use in physical memory.");
		add("OS", "Process", "Page File Bytes Peak", "Shows the maximum amount of virtual memory, in bytes, that a process has reserved for use in the paging file(s). Paging files are used to store pages of memory used by the process. Paging files are shared by all processes, and the lack of space in paging files can prevent other processes from allocating memory. If there is no paging file, this counter reflects the maximum amount of virtual memory that the process has reserved for use in physical memory.");
		add("OS", "Process", "Priority Base", "Shows the current base priority of this process. Threads within a process can raise and lower their own base priority relative to the process's base priority.");
		add("OS", "Process", "Private Bytes", "Shows the size, in bytes, that this process has allocated that cannot be shared with other processes.");
		add("OS", "Process", "Working Set", "Shows the size, in bytes, in the working set of this process. The working set is the set of memory pages that were touched recently by the threads in the process. If free memory in the computer is above a threshold, pages are left in the working set of a process, even if they are not in use. When free memory falls below a threshold, pages are trimmed from working sets. If the pages are needed, they will be soft-faulted back into the working set before leaving main memory.");
		add("OS", "Process", "Creating Process ID", "Shows the identifier of the process that created the current process. Note that the creating process might have terminated since the current process was created, and so the current Creating Process ID counter may no longer identify a running process.");
		add("OS", "Process", "Elapsed Time", "Shows the time, in seconds, that this process has been running.");
		add("OS", "Process", "Handle Count", "Shows the total number of handles currently open by this process. This number is the equal to the sum of the handles currently open by each thread in this process.");
		add("OS", "Process", "ID Process", "Shows the unique identifier of this process. ID Process numbers are reused, so they only identify a process for the lifetime of that process.");
		add("OS", "Process", "IO Data Bytes/sec", "Shows the rate, in incidents per second, at which the process was reading and writing bytes in I/O operations. It counts all I/O activity generated by the process including file, network, and device I/Os.");
		add("OS", "Process", "IO Data Operations/sec", "Shows the rate, in incidents per second, at which the process was issuing read and write I/O operations. It counts all I/O activity generated by the process including file, network, and device I/Os.");
		add("OS", "Process", "IO Other Bytes/sec", "Shows the rate, in incidents per second, at which the process was issuing bytes to I/O operations that do not involve data such as control operations. It counts all I/O activity generated by the process including file, network, and device I/Os.");
		add("OS", "Process", "IO Other Operations/sec", "Shows the rate, in incidents per second, at which the process was issuing I/O operations that were neither read nor write operations (for example, a control function). It counts all I/O activity generated by the process including file, network, and device I/Os.");
		add("OS", "Process", "IO Read Bytes/sec", "Shows the rate, in incidents per second, at which the process was reading bytes from I/O operations. It counts all I/O activity generated by the process including file, network, and device I/Os.");
		add("OS", "Process", "IO Read Operations/sec", "Shows the rate, in incidents per second, at which the process was issuing read I/O operations. It counts all I/O activity generated by the process including file, network, and device I/Os.");
		add("OS", "Process", "IO Write Bytes/sec", "Shows the rate, in incidents per second, at which the process was writing bytes to I/O operations. It counts all I/O activity generated by the process including file, network, and device I/Os.");
		add("OS", "Process", "IO Write Operations/sec", "Shows the rate, in incidents per second, at which the process was issuing write I/O operations. It counts all I/O activity generated by the process including file, network, and device I/Os.");
		add("OS", "Process", "Pool Nonpaged Bytes", "Shows the number of bytes in the nonpaged pool, an area of system memory (physical memory used by the operating system) for objects that cannot be written to disk. Nonpaged pool pages cannot be paged out to the paging file, but instead remain in main memory as long as they are allocated.");
		add("OS", "Process", "Pool Paged Bytes", "Shows the number of bytes in the paged pool, an area of system memory (physical memory used by the operating system) for objects that can be written to disk. Paged pool pages can be paged out to the paging file when not accessed by the system for sustained periods of time. Memory\\Pool Paged Bytes and Process\\Pool Paged Bytes are calculated differently than this counter, so the counters might not display the same amounts.");
		add("OS", "Process", "Thread Count", "Shows the number of threads that were active in this process. A thread is the object that executes instructions, which are the basic unit of execution in a processor. Every running process has at least one thread.");
		add("OS", "Process", "Virtual Bytes", "Shows the size, in bytes, of the virtual address space that the process is using. Use of virtual address space does not necessarily imply corresponding use of either disk or main memory pages. Virtual space is finite, and by using too much, the process can limit its ability to load libraries.");
		add("OS", "Process", "Virtual Bytes Peak", "Shows the maximum size, in bytes, of virtual address space that the process has used at any one time. Use of virtual address space does not necessarily imply corresponding use of either disk or main memory pages. However, virtual space is finite, and the process might limit its ability to load libraries by using too much.");
		add("OS", "Process", "Working Set Peak", "Shows the maximum size, in bytes, in the working set of this process. The working set is the set of memory pages that were touched recently by the threads in the process. If free memory in the computer is above a certain threshold, pages are left in the working set of a process, even if they are not in use. When free memory falls below a certain threshold, pages are trimmed from working sets. If the pages are needed, they will be soft-faulted back into the working set before leaving main memory.");

		
		// ################################################################
		// TCP
		// ################################################################
		add("OS", "TCP", "Connection Failures", "Shows the number of times that TCP connections have made a direct transition to the CLOSED state from the SYN-SENT or SYN-RCVD state, plus the number of times TCP connections have made a direct transition to the LISTEN state from the SYN-RCVD state, since the server was last started.");
		add("OS", "TCP", "Connections Active", "Shows the number of times TCP connections have made a direct transition to the SYN-SENT state from the CLOSED state, since the server was last started.");
		add("OS", "TCP", "Connections Established", "Shows the number of TCP connections for which the state was either ESTABLISHED or CLOSE-WAIT, since the server was last started.");
		add("OS", "TCP", "Connections Passive", "Shows the number of times that TCP connections have made a direct transition to the SYN-RCVD state from the LISTEN state, since the server was last started.");
		add("OS", "TCP", "Connections Reset", "Shows the number of times that TCP connections have made a direct transition to the CLOSED state from either the ESTABLISHED or CLOSE-WAIT state, since the server was last started.");
		add("OS", "TCP", "Segments Received/sec", "Shows the rate, in incidents per second, at which segments were received, including those received in error. This count includes segments received on currently established connections. Segments Received/sec is a subset of Segments/sec.");
		add("OS", "TCP", "Segments Retransmitted/sec", "Shows the rate, in incidents per second, at which segments containing one or more previously transmitted bytes were retransmitted.");
		add("OS", "TCP", "Segments Sent/sec", "Shows the rate, in incidents per second, at which segments were sent. This value includes those on current connections, but excludes those containing only retransmitted bytes. Segments Sent/sec is a subset of Segments/sec.");
		add("OS", "TCP", "Segments/sec", "Shows the rate, in incidents per second, at which TCP segments were sent or received using the TCP protocol. Segments/sec is the sum of the values of Segments Received/sec and Segments Sent/sec.");

		
		// ################################################################
		// Thread
		// ################################################################
		add("OS", "Thread", "% Privileged Time", "Shows the percentage of non-idle processor time spent executing code in privileged mode. Privileged mode is a processing mode designed for operating system components and hardware-manipulating drivers. It allows direct access to hardware and memory. The alternative, user mode, is a restricted processing mode designed for applications, environmental subsystems, and integral subsystems. The operating system switches application threads to privileged mode to access operating system services. % Privileged Time includes time servicing interrupts and DPCs. A high rate of privileged time might be attributed to a large number of interrupts generated by a failing device. When a Windows system service is called, the service often runs in privileged mode to gain access to system-private data. Such data is protected from access by threads executing in user mode. Calls to the system can be explicit or implicit, such as page faults or interrupts. Unlike some earlier Windows operating systems, Windows 2000 and the Windows Server 2003 family use process boundaries for subsystem protection in addition to the traditional protection of user and privileged modes. These subsystem processes provide additional protection. Therefore, some work done by Windows 2000 and Windows Server 2003 on behalf of your application might appear in other subsystem processes in addition to the privileged time in your process.");
		add("OS", "Thread", "% Processor Time", "Shows the percentage of time that the processor spent executing a non-idle thread. It is calculated by measuring the duration that the idle thread is active during the sample interval, and subtracting that time from 100 %. (Each processor has an idle thread that consumes cycles when no other threads are ready to run.) This counter is the primary indicator of processor activity, and displays the average percentage of busy time observed during the sample interval. Code executed to handle some hardware interrupts and trap conditions are included in this count.");
		add("OS", "Thread", "% User Time", "Shows the percentage of time that the processor spent executing code in user mode. Applications, environment subsystems, and integral subsystems execute in user mode. Code executing in user mode cannot damage the integrity of the Windows Executive, kernel, and/or device drivers. Unlike some earlier Windows operating systems, Windows 2000 and Windows Server 2003 use process boundaries for subsystem protection in addition to the traditional protection of user and privileged modes. These subsystem processes provide additional protection. Therefore, some work done by Windows 2000 and Windows Server 2003 on behalf of your application might appear in other subsystem processes in addition to the privileged time in your process.");
		add("OS", "Thread", "Context Switches/sec", "Shows the combined rate, in incidents per second, at which all processors on the computer were switched from one thread to another Context switches occur when a running thread voluntarily relinquishes the processor, or is preempted by a higher priority, ready thread.");
		add("OS", "Thread", "Elapsed Time", "Shows the time, in seconds, that this process has been running.");
		add("OS", "Thread", "ID Process", "Shows the unique identifier of this process. ID Process numbers are reused, so they only identify a process for the lifetime of that process.");
		add("OS", "Thread", "ID Thread", "Shows the unique identifier of this thread. ID Thread numbers are reused, so they only identify a thread for the lifetime of that thread.");
		add("OS", "Thread", "Priority Base", "Shows the current base priority of this process. Threads within a process can raise and lower their own base priority relative to the process's base priority.");
		add("OS", "Thread", "Priority Current", "Shows the current dynamic priority of this thread. The system can raise the dynamic priority of the thread above the base priority if the thread is handling user input, or lower it toward the base priority if the thread becomes compute bound.");
		add("OS", "Thread", "Start Address", "Shows the starting virtual address for this thread.");
		add("OS", "Thread", "Thread State", "Shows the current state of the thread. Valid values are: 0 Initialized 1 Ready 2 Running 3 Standby 4 Terminated 5 Waiting 6 Transition 7 Unknown");
		add("OS", "Thread", "Thread Wait Reason", "Shows the reason that a thread was in the Wait state. This counter is only applicable when the thread is in the Wait state, which is indicated by a Thread State value of 5. Values for this counter are as follows: 0 Waiting for a component of the Windows NT Executive 1 Waiting for a page to be freed 2 Waiting for a page to be mapped or copied 3 Waiting for space to be allocated in the paged or nonpaged pool 4 Waiting for an Execution Delay to be resolved 5 Suspended 6 Waiting for a user request 7 Waiting for a component of the Windows NT Executive 8 Waiting for a page to be freed 9 Waiting for a page to be mapped or copied 10 Waiting for space to be allocated in the paged or nonpaged pool 11 Waiting for an Execution Delay to be resolved 12 Suspended 13 Waiting for a user request 14 Waiting for an event pair high 15 Waiting for an event pair low 16 Waiting for an LPC Receive notice 17 Waiting for an LPC Reply notice 18 Waiting for virtual memory to be allocated 19 Waiting for a page to be written to disk 20+ (Reserved for future use)");

		
		// ################################################################
		// IP
		// ################################################################
		add("OS", "IP", "Datagrams Forwarded/sec", "Shows the rate, in incidents per second, at which attempts were made to find routes to forward input datagrams to their final destination, because the local server was not the final IP destination. In servers that do not act as IP gateways, this rate includes only packets that were source-routed via this entity where the source-route option processing was successful.");
		add("OS", "IP", "Datagrams Outbound No Route", "Shows the number of IP datagrams that were discarded because no route could be found to transmit them to their destination. This counter includes any packets counted in Datagrams Forwarded/sec that meet this \"no route\" criterion.");
		add("OS", "IP", "Datagrams Outbound Discarded", "Shows the number of output IP datagrams that were discarded even though no problems were encountered to prevent their transmission to their destination (for example, lack of buffer space). This counter includes datagrams counted in Datagrams Forwarded/sec that meet this criterion.");
		add("OS", "IP", "Datagrams Received Address Errors", "Shows the number of input IP datagrams that were discarded because the IP address in their IP header destination field was not valid for the computer. This count includes invalid addresses (for example, 0.0.0.0) and addresses of unsupported Classes (for example, Class E). For entities that are not IP gateways and do not forward datagrams, this counter includes datagrams that were discarded because the destination was not a local address.");
		add("OS", "IP", "Datagrams Received Delivered/sec", "Shows the rate, in incidents per second, at which input IP datagrams were successfully delivered to IP user-protocols, including Internet Control Message Protocol (ICMP).");
		add("OS", "IP", "Datagrams Received Discarded", "Shows the number of input IP datagrams that were discarded even though problems prevented their continued processing (for example, lack of buffer space). This counter does not include any datagrams discarded while awaiting reassembly.");
		add("OS", "IP", "Datagrams Received Header Errors", "Shows the number of input IP datagrams that were discarded due to errors in the IP headers, including bad checksums, version number mismatch, other format errors, time-to-live exceeded, errors discovered while processing their IP options, etc.");
		add("OS", "IP", "Datagrams Received Unknown Protocol", "Shows the number of locally-addressed datagrams that were successfully received but were discarded because of an unknown or unsupported protocol.");
		add("OS", "IP", "Datagrams Received/sec", "Shows the rate, in incidents per second, at which IP datagrams were received from the interfaces, including those in error. Datagrams Received/sec is a subset of Datagrams/sec.");
		add("OS", "IP", "Datagrams Sent/sec", "Shows the rate, in incidents per second, at which IP datagrams were supplied for transmission by local IP user-protocols (including ICMP). This counter does not include datagrams counted in Datagrams Forwarded/sec. Datagrams Sent/sec is a subset of Datagrams/sec.");
		add("OS", "IP", "Datagrams/sec", "Shows the rate, in incidents per second, at which IP datagrams were received from or sent to the interfaces, including those in error. Forwarded datagrams are not included in this rate.");
		add("OS", "IP", "Fragment Reassembly Failures", "Shows the number of failures detected by the IP reassembly algorithm, such as time outs, errors, etc. This is not necessarily a count of discarded IP fragments since some algorithms (notably RFC 815) lose track of the number of fragments by combining them as they are received.");
		add("OS", "IP", "Fragmentation Failures", "Shows the number of IP datagrams that were discarded because they needed to be fragmented but could not be (for example, because the \"Don't Fragment\" flag was set).");
		add("OS", "IP", "Fragmented Datagrams/sec", "Shows the rate, in incidents per second, at which datagrams were successfully fragmented.");
		add("OS", "IP", "Fragments Created/sec", "Shows the rate, in incidents per second, at which IP datagram fragments were generated as a result of fragmentation.");
		add("OS", "IP", "Fragments Reassembled/sec", "Shows the rate, in incidents per second, at which IP fragments were successfully reassembled.");
		add("OS", "IP", "Fragments Received/sec", "Shows the rate, in incidents per second, at which IP fragments that need to be reassembled at this entity were received.");

		
		// ################################################################
		// DUPTILE1_Network Interface
		// ################################################################
		add("OS", "DUPTILE1_Network Interface", "Bytes Received/sec", "Shows the rate, in incidents per second, at which bytes were received over each network adapter. The counted bytes include framing characters. Bytes Received/sec is a subset of Bytes Total/sec.");
		add("OS", "DUPTILE1_Network Interface", "Bytes Sent/sec", "Shows the rate, in incidents per second, at which bytes were sent over each network adapter. The counted bytes include framing characters. Bytes Sent/sec is a subset of Bytes Total/sec.");
		add("OS", "DUPTILE1_Network Interface", "Bytes Total/sec", "Shows the rate, in incidents per second, at which bytes were sent and received on the network interface, including framing characters. Bytes Total/sec is the sum of the values of Bytes Received/sec and Bytes Sent/sec.");
		add("OS", "DUPTILE1_Network Interface", "Current Bandwidth", "Shows an estimate of the current bandwidth of the network interface in bits per second (bps). For interfaces that do not vary in bandwidth, or for those where no accurate estimation can be made, this value is the nominal bandwidth.");
		add("OS", "DUPTILE1_Network Interface", "Output Queue Length", "Shows the length, in number of packets, of the output packet queue. If this is longer than two packets, it indicates that there are delays, and if possible the bottleneck should be found and eliminated. Since the requests are queued by Network Driver Interface Specification (NDIS) in this implementation, this value is always 0.");
		add("OS", "DUPTILE1_Network Interface", "Packets Outbound Discarded", "Shows the number of outbound packets to be discarded, even though no errors were detected to prevent transmission. One possible reason for discarding such a packet could be to free up buffer space.");
		add("OS", "DUPTILE1_Network Interface", "Packets Outbound Errors", "Shows the number of outbound packets that could not be transmitted because of errors.");
		add("OS", "DUPTILE1_Network Interface", "Packets Received Discarded", "Shows the number of inbound packets that were discarded, even though no errors were detected to prevent their being delivered to a higher-layer protocol. One possible reason for discarding such a packet could be to free up buffer space.");
		add("OS", "DUPTILE1_Network Interface", "Packets Received Errors", "Shows the number of inbound packets that contained errors that prevented them from being delivered to a higher-layer protocol.");
		add("OS", "DUPTILE1_Network Interface", "Packets Received Non-Unicast/sec", "Shows the rate, in incidents per second, at which non-unicast (subnet broadcast or subnet multicast) packets were delivered to a higher-layer protocol.");
		add("OS", "DUPTILE1_Network Interface", "Packets Received Unicast/sec", "Shows the rate, in incidents per second, at which subnet-unicast packets were delivered to a higher-layer protocol.");
		add("OS", "DUPTILE1_Network Interface", "Packets Received Unknown", "Shows the number of packets received through the interface that were discarded because of an unknown or unsupported protocol.");
		add("OS", "DUPTILE1_Network Interface", "Packets Received/sec", "Shows the rate, in incidents per second, at which packets were received on the network interface.");
		add("OS", "DUPTILE1_Network Interface", "Packets Sent/sec", "Shows the rate, in incidents per second, at which packets were sent on the network interface.");
		add("OS", "DUPTILE1_Network Interface", "Packets Sent Non-Unicast/sec", "Shows the rate, in incidents per second, at which packets were requested to be transmitted to non-unicast (subnet broadcast or subnet multicast) addresses by higher-level protocols. This counter includes packets that were discarded or not sent.");
		add("OS", "DUPTILE1_Network Interface", "Packets Sent Unicast/sec", "Shows the rate, in incidents per second, at which packets were requested to be transmitted to subnet-unicast addresses by higher-level protocols. This counter includes the packets that were discarded or not sent.");

		// ################################################################
		// Network Interface
		// NOTE: This is a copy of 'DUPTILE1_Network Interface' because I couldn't find 'Network Interface'
		// ################################################################
		add("OS", "Network Interface", "Bytes Received/sec", "Shows the rate, in incidents per second, at which bytes were received over each network adapter. The counted bytes include framing characters. Bytes Received/sec is a subset of Bytes Total/sec.");
		add("OS", "Network Interface", "Bytes Sent/sec", "Shows the rate, in incidents per second, at which bytes were sent over each network adapter. The counted bytes include framing characters. Bytes Sent/sec is a subset of Bytes Total/sec.");
		add("OS", "Network Interface", "Bytes Total/sec", "Shows the rate, in incidents per second, at which bytes were sent and received on the network interface, including framing characters. Bytes Total/sec is the sum of the values of Bytes Received/sec and Bytes Sent/sec.");
		add("OS", "Network Interface", "Current Bandwidth", "Shows an estimate of the current bandwidth of the network interface in bits per second (bps). For interfaces that do not vary in bandwidth, or for those where no accurate estimation can be made, this value is the nominal bandwidth.");
		add("OS", "Network Interface", "Output Queue Length", "Shows the length, in number of packets, of the output packet queue. If this is longer than two packets, it indicates that there are delays, and if possible the bottleneck should be found and eliminated. Since the requests are queued by Network Driver Interface Specification (NDIS) in this implementation, this value is always 0.");
		add("OS", "Network Interface", "Packets Outbound Discarded", "Shows the number of outbound packets to be discarded, even though no errors were detected to prevent transmission. One possible reason for discarding such a packet could be to free up buffer space.");
		add("OS", "Network Interface", "Packets Outbound Errors", "Shows the number of outbound packets that could not be transmitted because of errors.");
		add("OS", "Network Interface", "Packets Received Discarded", "Shows the number of inbound packets that were discarded, even though no errors were detected to prevent their being delivered to a higher-layer protocol. One possible reason for discarding such a packet could be to free up buffer space.");
		add("OS", "Network Interface", "Packets Received Errors", "Shows the number of inbound packets that contained errors that prevented them from being delivered to a higher-layer protocol.");
		add("OS", "Network Interface", "Packets Received Non-Unicast/sec", "Shows the rate, in incidents per second, at which non-unicast (subnet broadcast or subnet multicast) packets were delivered to a higher-layer protocol.");
		add("OS", "Network Interface", "Packets Received Unicast/sec", "Shows the rate, in incidents per second, at which subnet-unicast packets were delivered to a higher-layer protocol.");
		add("OS", "Network Interface", "Packets Received Unknown", "Shows the number of packets received through the interface that were discarded because of an unknown or unsupported protocol.");
		add("OS", "Network Interface", "Packets Received/sec", "Shows the rate, in incidents per second, at which packets were received on the network interface.");
		add("OS", "Network Interface", "Packets Sent/sec", "Shows the rate, in incidents per second, at which packets were sent on the network interface.");
		add("OS", "Network Interface", "Packets Sent Non-Unicast/sec", "Shows the rate, in incidents per second, at which packets were requested to be transmitted to non-unicast (subnet broadcast or subnet multicast) addresses by higher-level protocols. This counter includes packets that were discarded or not sent.");
		add("OS", "Network Interface", "Packets Sent Unicast/sec", "Shows the rate, in incidents per second, at which packets were requested to be transmitted to subnet-unicast addresses by higher-level protocols. This counter includes the packets that were discarded or not sent.");
	}
}

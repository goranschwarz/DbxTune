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
package com.dbxtune.hostmon;

import com.dbxtune.cm.os.CmOsPs;
import com.dbxtune.ssh.SshConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.VersionShort;

public class MonitorPsLinux
extends HostMonitor
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public MonitorPsLinux()
	{
		this(-1, null);
	}
	public MonitorPsLinux(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorPsLinux";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		
		if (StringUtil.hasValue(cmd))
			return cmd;
		
		int top = Configuration.getCombinedConfiguration().getIntProperty(CmOsPs.PROPKEY_top, CmOsPs.DEFAULT_top);
		
		String topProcs = " | head -n " + top;

		if (top <= 0)
		{
			topProcs = "";
		}


		// NOTE: 'etimes' is not available in RHEL 6.6 (possible version 6)
		//       'etimes' was introduced in version 3.3 according to -- https://abi-laboratory.pro/?view=changelog&l=procps-ng&v=3.3.12
		if ( getUtilVersion() < VersionShort.toInt(3,3,0))
		{
			return "ps -e -ww --format pid,ppid,euser,tty,vsz,rss,cputime,pmem,pcpu,args --sort=-pcpu" + topProcs;
		}
		else
		{
			return "ps -e -ww --format pid,ppid,euser,tty,vsz,rss,etimes,cputime,pmem,pcpu,args --sort=-pcpu" + topProcs;
		}

	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		if ( utilVersion < VersionShort.toInt(3,3,0))
		{
			// NOTE: 'etimes' was introduced in version 3.3 (so it's skipped here)
			md.addIntColumn ("pid",         1,  1,  false,        "a number representing the process ID");
			md.addIntColumn ("ppid",        2,  2,  false,        "parent process ID");
			md.addStrColumn ("user",        3,  3,  false, 20,    "effective user name.  This will be the textual user ID, if it can be obtained and the field width permits, or a decimal representation otherwise.");
			md.addStrColumn ("tty",         4,  4,  false, 10,    "controlling tty (terminal)");
			md.addIntColumn ("vsize",       5,  5,  false,        "virtual memory size of the process in KiB (1024-byte units).  Device mappings are currently excluded; this is subject to change.");
			md.addIntColumn ("rss",         6,  6,  false,        "resident set size, the non-swapped physical memory that a task has used (in kiloBytes).");
			md.addStrColumn ("cputime",     7,  7,  false, 15,    "cumulative CPU time, '[DD-]hh:mm:ss' format.");
			md.addDecColumn ("%mem",        8,  8,  false, 5, 1,  "ratio of the process's resident set size  to the physical memory on the machine, expressed as a percentage.");
			md.addDecColumn ("%cpu",        9,  9,  false, 5, 1,  "cpu utilization of the process in ##.# format. Currently, it is the CPU time used divided by the time the process has been running (cputime/realtime ratio), expressed as a percentage.  It will not add up to 100% unless you are lucky.");
			md.addStrColumn ("command",     10, 10, false, 4096,  "command with all its arguments as a string. Modifications to the arguments may be shown.  The output in this column may contain spaces.  A process marked <defunct> is partly dead, waiting to be fully destroyed by its parent.  Sometimes the process args will be unavailable; when this happens, ps will instead print the executable name in brackets.  (alias cmd, command).  See also the comm format keyword, the -f option, and the c option.");
		}
		else
		{
			md.addIntColumn ("pid",         1,  1,  false,        "a number representing the process ID");
			md.addIntColumn ("ppid",        2,  2,  false,        "parent process ID");
			md.addStrColumn ("user",        3,  3,  false, 20,    "effective user name.  This will be the textual user ID, if it can be obtained and the field width permits, or a decimal representation otherwise.");
			md.addStrColumn ("tty",         4,  4,  false, 10,    "controlling tty (terminal)");
			md.addIntColumn ("vsize",       5,  5,  false,        "virtual memory size of the process in KiB (1024-byte units).  Device mappings are currently excluded; this is subject to change.");
			md.addIntColumn ("rss",         6,  6,  false,        "resident set size, the non-swapped physical memory that a task has used (in kiloBytes).");
			md.addIntColumn ("etimes",      7,  7,  false,        "elapsed time since the process was started, in seconds."); // 'etimes' was introduced in version 3.3
			md.addStrColumn ("cputime",     8,  8,  false, 15,    "cumulative CPU time, '[DD-]hh:mm:ss' format.");
			md.addDecColumn ("%mem",        9,  9,  false, 5, 1,  "ratio of the process's resident set size  to the physical memory on the machine, expressed as a percentage.");
			md.addDecColumn ("%cpu",        10, 10, false, 5, 1,  "cpu utilization of the process in ##.# format. Currently, it is the CPU time used divided by the time the process has been running (cputime/realtime ratio), expressed as a percentage.  It will not add up to 100% unless you are lucky.");
			md.addStrColumn ("command",     11, 11, false, 4096,  "command with all its arguments as a string. Modifications to the arguments may be shown.  The output in this column may contain spaces.  A process marked <defunct> is partly dead, waiting to be fully destroyed by its parent.  Sometimes the process args will be unavailable; when this happens, ps will instead print the executable name in brackets.  (alias cmd, command).  See also the comm format keyword, the -f option, and the c option.");
		}


		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		md.setOsCommandStreaming(false);
		
		md.setPercentCol("%mem", "%cpu");

		md.setPkCol("pid");
		
//		md.setDiffCol( "Used-KB", "Available-KB", "Used-MB", "Available-MB" );

		// Skip the header line
		md.addSkipRows("pid", "PID");

//		// Get SKIP and ALLOW from the Configuration
//		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
//		md.setSkipAndAllowRows("hostmon.MonitorVmstat.", Configuration.getCombinedConfiguration());

		return md;
	}

	/**
	 * The last column "cmd/args" may have *many* arguments... so try to "wrap up" everything in the column "cmd/args" as a single field 
	 */
	@Override
	public String[] parseRow(HostMonitorMetaData md, String row, String[] preParsed, int type)
	{
		if (type == SshConnection.STDERR_DATA)
			return null;
//System.out.println("XXX row=|" + row + "|");
		
		// Now concatenate everything for the "cmd/args" field (if there are parameters)
		if (preParsed.length > md.getParseColumnCount())
		{
//System.out.println("  -- preParsed.length=" + preParsed.length + ", md.getParseColumnCount()=" + md.getParseColumnCount());
			String[] allCols = new String[md.getParseColumnCount()];

			// Copy everything 
			String cmdArgs = "";
			for (int i=md.getParseColumnCount()-1; i<preParsed.length; i++)
			{
				cmdArgs += preParsed[i] + " ";
//				System.out.println("    ++ preParsed[" + i + "]=|" + preParsed[i] + "|, cmdArgs=|" + cmdArgs + "|");
			}
			
			// Copy all "pre" columns (or columns to left of "cmd/args")
			for (int i=0; i<allCols.length; i++)
				allCols[i] = preParsed[i];

			// Then set all the concatenated columns into "cmd/args"
			allCols[allCols.length-1] = cmdArgs.trim();
			
			// Assign the preParsed to the "above fix"
			preParsed = allCols;
		}

		// Let "super" do it's intended work
		String[] data = super.parseRow(md, row, preParsed, type);
		if (data == null)
			return null;

		return data;
	}

//	@Override
//	public String[] parseRow(HostMonitorMetaData md, String row, String[] preParsed, int type)
//	{
//		if (type == SshConnection.STDERR_DATA)
//			return null;
//
//		// Let "super" do it's intended work
//		String[] data = super.parseRow(md, row, preParsed, type);
//		if (data == null)
//			return null;
//
//		// Strip off *trailing* '%' characters in 'UsedPct' fields
//		int usedPct_pos = 4;
//		if (usedPct_pos < data.length)
//		{
//			if (data[usedPct_pos].endsWith("%"))
//				data[usedPct_pos] = data[usedPct_pos].substring(0, data[usedPct_pos].length()-1);
//		}
//
//		return data;
//	}
}

/*
A alternate way to *better* get CPU Usage
  - Get raw values on the Linux side... from: /proc/...
  - Parse the values (received over the SSH STDOUT stream) into Java Objects
  - Calculate various values (like CPU Usage)... Data still needs to be stored in memory for "previous sample"
  - etc...
  
Right now this might be "overkill"... But I have the following, which we might implement in the future

1: A Python Script that scans /proc/... and get desired fields
   Presented as a *single* row per PID

2: Java code that parses etc

See ChatGPT History 'Top vs Ps for Stats', where we talk about:
 * why PS (on it's own) isn't good enough, because we can't remember "stuff" from previous execution
 * Various implementations (how other tools like top, htop, atop... does this)
 * Should we extract all /proc/... values to the client
 * Should we execute some (stateless script) on the server side, that just returns desired counters
 * OR: Should we have some (stetefull script) on the server side, that "does it all"... (but this leads to more problems than solutions)

The end game was:
 * Keep it stateless (remember previous sample at Java Client Side)
 * Have a Python Script that extracts the desired data

One downside of this is:
 * We need to "transfer" the script onto the "remote host" when connecting (place it in /tmp/xxxx.sh with chmod 777 so "everyone" can write/replace it)

The Python script:
--------------------------------------------------------------------------------
#!/usr/bin/env python3
import os

def safe_read(path, binary=False):
    try:
        mode = 'rb' if binary else 'r'
        with open(path, mode) as f:
            return f.read()
    except (FileNotFoundError, PermissionError):
        return None

def parse_kv_block(text):
    d = {}
    for line in text.splitlines():
        if ':' in line:
            k, v = line.split(':', 1)
            d[k.strip()] = v.strip()
    return d

# total CPU
stat = safe_read("/proc/stat")
if stat:
    for line in stat.splitlines():
        if line.startswith("cpu "):
            print("CPU", line)
            break

for pid in os.listdir("/proc"):
    if not pid.isdigit():
        continue

    base = f"/proc/{pid}"

    stat = safe_read(f"{base}/stat")
    if not stat:
        continue

    # --- parse /proc/PID/stat ---
    s = stat.strip()
    open_p = s.find('(')
    close_p = s.rfind(')')
    rest = s[close_p+2:].split()

    utime = rest[11]
    stime = rest[12]
    start = rest[19]
    rss   = rest[21]

    # --- cmdline ---
    cmd = safe_read(f"{base}/cmdline", binary=True)
    if cmd:
        cmd = cmd.replace(b'\0', b' ').decode(errors="ignore").strip()
    else:
        cmd = ""

    # --- IO ---
    io = safe_read(f"{base}/io")
    io_kv = parse_kv_block(io) if io else {}

    # --- context switches ---
    status = safe_read(f"{base}/status")
    ctx = {}
    if status:
        for line in status.splitlines():
            if "ctxt_switches" in line:
                k, v = line.split(':', 1)
                ctx[k.strip()] = v.strip()

    # --- emit SINGLE LINE ---
    fields = [
        f"PID={pid}",
        f"UTIME={utime}",
        f"STIME={stime}",
        f"START={start}",
        f"RSS_PAGES={rss}",
    ]

    for k in ("rchar","wchar","syscr","syscw",
              "read_bytes","write_bytes","cancelled_write_bytes"):
        if k in io_kv:
            fields.append(f"{k.upper()}={io_kv[k]}")

    if "voluntary_ctxt_switches" in ctx:
        fields.append(f"VCS={ctx['voluntary_ctxt_switches']}")
    if "nonvoluntary_ctxt_switches" in ctx:
        fields.append(f"NVCS={ctx['nonvoluntary_ctxt_switches']}")

    fields.append(f"CMD={cmd}")

    print(" ".join(fields))
--------------------------------------------------------------------------------
Example of output (for one row)
PID=88825 UTIME=12345 STIME=6789 START=456789 RSS_PAGES=1234 RCHAR=2099330 WCHAR=3471 SYSCR=4564 SYSCW=451 RBYTES=28647424 WBYTES=0 VCS=123 NVCS=45 CMD=java -Xmx2g com.foo.Main
--------------------------------------------------------------------------------



The Java Code:
--------------------------------------------------------------------------------
// Process key (PID reuse safe)
record ProcKey(int pid, long startTime) {} // OR make this into a class in Java 11, records was introduced in Java 17 I think

// Raw snapshot from server
class ProcSample 
{
    int pid;
    long startTime;
    long utime;
    long stime;
    long rssPages;
    long rbytes;
    long wbytes;
    long vcs;
    long nvcs;
    String cmd;

    long totalCpu() 
    {
        return utime + stime;
    }
}

// Stored previous state
class PrevSample 
{
    long totalCpu;
    long rbytes;
    long wbytes;
}

// Parsing logic (fast, allocation-light)
static ProcSample parseLine(String line) 
{
    ProcSample p = new ProcSample();

    int i = 0;
    while (i < line.length()) 
    {
        int eq = line.indexOf('=', i);
        if (eq < 0) break;

        String key = line.substring(i, eq);
        int next = line.indexOf(' ', eq + 1);
        if (next < 0) next = line.length();

        String val = line.substring(eq + 1, next);

        switch (key) 
        {
            case "PID"        -> p.pid = Integer.parseInt(val);
            case "START"      -> p.startTime = Long.parseLong(val);
            case "UTIME"      -> p.utime = Long.parseLong(val);
            case "STIME"      -> p.stime = Long.parseLong(val);
            case "RSS_PAGES"  -> p.rssPages = Long.parseLong(val);
            case "RBYTES"     -> p.rbytes = Long.parseLong(val);
            case "WBYTES"     -> p.wbytes = Long.parseLong(val);
            case "VCS"        -> p.vcs = Long.parseLong(val);
            case "NVCS"       -> p.nvcs = Long.parseLong(val);
            case "CMD"        -> 
            {
                p.cmd = line.substring(eq + 1);
                return p; // CMD is last field
            }
        }
        i = next + 1;
    }
    return p;
}


// Delta engine + top-N selection
import java.util.*;
import java.util.stream.*;

public class TopNCollector 
{
    private final Map<ProcKey, PrevSample> prev = new HashMap<>();

    public List<Result> processSnapshot(List<String> lines, int topN) 
    {
        Map<ProcKey, PrevSample> nextPrev = new HashMap<>();
        List<Result> results = new ArrayList<>();

        for (String line : lines) 
        {
            if (!line.startsWith("PID=")) continue;

            ProcSample cur = parseLine(line);
            ProcKey key = new ProcKey(cur.pid, cur.startTime);

            PrevSample old = prev.get(key);
            if (old != null) 
            {
                long dCpu = cur.totalCpu() - old.totalCpu;
                long dRead = cur.rbytes - old.rbytes;
                long dWrite = cur.wbytes - old.wbytes;

                if (dCpu > 0) 
                {
                    results.add(new Result(cur, dCpu, dRead, dWrite));
                }
            }

            PrevSample ps = new PrevSample();
            ps.totalCpu = cur.totalCpu();
            ps.rbytes = cur.rbytes;
            ps.wbytes = cur.wbytes;
            nextPrev.put(key, ps);
        }

        prev.clear();
        prev.putAll(nextPrev);

        return results.stream()
                .sorted(Comparator.comparingLong(Result::deltaCpu).reversed())
                .limit(topN)
                .toList();
    }
}

// Result object (what your UI / exporter uses)
record Result(
        ProcSample proc,
        long deltaCpu,
        long deltaReadBytes,
        long deltaWriteBytes
) {}

// Usage example
TopNCollector collector = new TopNCollector();

while (true) 
{
    List<String> snapshot = readFromSSH(); // one snapshot
    List<Result> top = collector.processSnapshot(snapshot, 20);

    for (Result r : top) {
        System.out.printf(
            "PID=%d CPU=%d RSS=%d CMD=%s%n",
            r.proc.pid,
            r.deltaCpu,
            r.proc.rssPages,
            r.proc.cmd
        );
    }

    Thread.sleep(5000);
}


--------------------------------------------------------------------------------

*/
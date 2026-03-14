#!/usr/bin/env python
# -*- coding: utf-8 -*-
#############################################################################
## Used by DbxTune collector MonitorPs to get various Process Information
## NOTE: Make sure the line endings are <NL> and not <CR><LF>
#############################################################################

import os
import time
import json
import pwd
import sys

# --- VERSION CHECK ---
MIN_PY2 = (2, 7)
MIN_PY3 = (3, 4)

def check_version():
    current = sys.version_info
    is_valid = False
    if current.major == 2 and current >= MIN_PY2:
        is_valid = True
    elif current.major == 3 and current >= MIN_PY3:
        is_valid = True
        
    if not is_valid:
        error_res = [{
            "error": "Python version too low",
            "detected": "{}.{}.{}".format(current.major, current.minor, current.micro),
            "required": "2.7+ or 3.4+"
        }]
        sys.stdout.write(json.dumps(error_res) + '\n')
        sys.exit(1)

def get_boot_time():
    """Get system boot time in seconds since epoch from /proc/stat."""
    try:
        with open('/proc/stat', 'r') as f:
            for line in f:
                if line.startswith('btime'):
                    return int(line.split()[1])
    except Exception:
        pass
    # Fallback to uptime calculation if btime is unavailable
    try:
        with open('/proc/uptime', 'r') as f:
            uptime_seconds = float(f.read().split()[0])
            return int(time.time() - uptime_seconds)
    except Exception:
        return int(time.time())

def get_total_memory_kb():
    """Get total physical RAM in KB from /proc/meminfo to calculate pmem."""
    try:
        with open('/proc/meminfo', 'r') as f:
            for line in f:
                if line.startswith('MemTotal:'):
                    return int(line.split()[1])
    except Exception:
        pass
    return 1 # Avoid division by zero

def get_process_stats():
    check_version()
    processes = []
    # Get clock ticks per second to convert ticks to milliseconds
        clk_tck = os.sysconf(os.sysconf_names['SC_CLK_TCK'])
    except (AttributeError, KeyError):
        clk_tck = 100 # Default for most Linux systems
        
    boot_time_sec = get_boot_time()
    total_system_mem_kb = get_total_memory_kb()
    
    for pid_dir in os.listdir('/proc'):
        if not pid_dir.isdigit():
            continue
        
        path = os.path.join('/proc', pid_dir)
        try:
            # Basic process ownership
            stat_info = os.stat(path)
            user_name = pwd.getpwuid(stat_info.st_uid).pw_name

            # CPU, PPID & Start Time (/proc/[pid]/stat)
            with open(os.path.join(path, 'stat'), 'r') as f:
                raw_stat = f.read()
                # Split from the right of the last ')' to handle process names with spaces/parentheses
                after_comm = raw_stat.split(')')[-1].split()
                
                # Mapping relative to the split after command name:
                ppid = int(after_comm[1])
                utime_ms = (int(after_comm[11]) * 1000) // clk_tck
                stime_ms = (int(after_comm[12]) * 1000) // clk_tck
                io_wait_ms = (int(after_comm[39]) * 1000) // clk_tck # delayacct_blkio_ticks
                
                start_ticks = int(after_comm[19])
                # Use float for clk_tck to ensure precise division in Python 2
                start_time_millis = int((boot_time_sec + (start_ticks / float(clk_tck))) * 1000)

            # Command Line (/proc/[pid]/cmdline)
            # Binary read to handle null-bytes correctly across Python versions
            try:
                with open(os.path.join(path, 'cmdline'), 'rb') as f:
                    content = f.read()
                    # Python 2 b'\0' works, Python 3 needs b'\0' to replace in bytes
                    cmdline = content.replace(b'\0', b' ').decode('utf-8', 'ignore').strip()
            except Exception:
                cmdline = ""
                
            if not cmdline: # Fallback for kernel threads or permission issues
                try:
                    with open(os.path.join(path, 'comm'), 'r') as f:
                        cmdline = "[" + f.read().strip() + "]"
                except Exception:
                    cmdline = "[unknown]"

            # Memory & Threads (/proc/[pid]/status)
            ctx_v   = 0
            ctx_inv = 0
            rss_kb  = 0
            threads = 0
            with open(os.path.join(path, 'status'), 'r') as f:
                for line in f:
                    parts = line.split()
                    if len(parts) < 2: continue
                    if line.startswith('voluntary_ctxt_switches:'): ctx_v = int(parts[1])
                    elif line.startswith('nonvoluntary_ctxt_switches:'): ctx_inv = int(parts[1])
                    elif line.startswith('VmRSS:'): rss_kb = int(parts[1])
                    elif line.startswith('Threads:'): threads = int(parts[1])

            pmem = (rss_kb * 100.0) / total_system_mem_kb

            # Disk I/O (/proc/[pid]/io)
            io_ok    = 0  # Permission denied for non-root users on some processes
            io_r     = 0
            io_w     = 0
            io_syscr = 0  # Read  IOPS
            io_syscw = 0  # Write IOPS
            try:
                with open(os.path.join(path, 'io'), 'r') as f:
                    for line in f:
                        p = line.split()
                        if line.startswith('read_bytes:'): io_r = int(p[1])
                        elif line.startswith('write_bytes:'): io_w = int(p[1])
                        elif line.startswith('syscr:'): io_syscr = int(p[1])
                        elif line.startswith('syscw:'): io_syscw = int(p[1])
                    io_ok = 1  # Permission OK
            except (IOError, OSError):
                pass
            # Final JSON structure matching Java CamelCase fields
            processes.append({
                "pid": int(pid_dir),
                "ppid": ppid,
                "startTimeMillis": start_time_millis,
                "user": user_name,
                "cmd": cmdline,
                "threads": threads,
                "cores": num_cores,
                "utimeMs": utime_ms,
                "stimeMs": stime_ms,
                "ioWaitMs": io_wait_ms,
                "rssKb": rss_kb,
                "memPct": round(pmem, 2),
                "ioOk": io_ok,
                "ioReadBytes": io_r,
                "ioWriteBytes": io_w,
                "ioSyscr": io_syscr,
                "ioSyscw": io_syscw,
                "ctxVoluntary": ctx_v,
                "ctxInvoluntary": ctx_inv,
                "ts": int(time.time() * 1000)
            })
            
        except (FileNotFoundError, PermissionError, ProcessLookupError, IndexError):
            # Process died or insufficient permissions during reading
            continue

    return json.dumps(processes)

if __name__ == "__main__":
    # Output raw JSON to stdout for Java to capture via SSH
    print(get_process_stats())

#############################################################################
## END-OF-SCRIPT --- NOTE: Make sure we have a newline AFTER THIS ROW
#############################################################################

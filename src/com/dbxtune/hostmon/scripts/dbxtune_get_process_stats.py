#!/usr/bin/env python3
#############################################################################
## Used by DbxTune collector MonitorPs to get various Process Information
## NOTE: Make sure the line endings are <NL> and not <CR><LF>
#############################################################################

import os
import time
import json
import pwd

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
    processes = []
    # Get clock ticks per second to convert ticks to milliseconds
    clk_tck = os.sysconf(os.sysconf_names['SC_CLK_TCK'])
    boot_time_sec = get_boot_time()
    total_system_mem_kb = get_total_memory_kb()
    num_cores = os.cpu_count() or 1

    for pid_dir in os.listdir('/proc'):
        if not pid_dir.isdigit():
            continue

        pid_str = pid_dir
        path = os.path.join('/proc', pid_str)

        try:
            # 1. Basic process ownership
            stat_info = os.stat(path)
            uid = stat_info.st_uid
            user_name = pwd.getpwuid(uid).pw_name

            # 2. CPU, PPID & Start Time (/proc/[pid]/stat)
            with open(os.path.join(path, 'stat'), 'r') as f:
                raw_stat = f.read()
                # Split from the right of the last ')' to handle process names with spaces/parentheses
                after_comm = raw_stat.split(')')[-1].split()

                # Mapping relative to the split after command name:
                ppid = int(after_comm[1])

                # utime(11) and stime(12) are at index 11 and 12 in the split list
                utime_ms = (int(after_comm[11]) * 1000) // clk_tck
                stime_ms = (int(after_comm[12]) * 1000) // clk_tck

                # starttime(19) is at index 19 in the split list
                start_ticks = int(after_comm[19])
                start_time_seconds = boot_time_sec + (start_ticks / clk_tck)
                start_time_millis = int(start_time_seconds * 1000)

                # after_comm[39] corresponds to delayacct_blkio_ticks (field 42 in man proc)
                iowait_ms = (int(after_comm[39]) * 1000) // clk_tck

            # 3. Full Command Line
            try:
                with open(os.path.join(path, 'cmdline'), 'r') as f:
                    # cmdline is null-byte separated
                    cmdline = f.read().replace('\0', ' ').strip()
            except:
                cmdline = ""

            if not cmdline: # Fallback for kernel threads or permission issues
                try:
                    with open(os.path.join(path, 'comm'), 'r') as f:
                        cmdline = "[" + f.read().strip() + "]"
                except:
                    cmdline = "[unknown]"

            # 4. Memory & Context Switches (/proc/[pid]/status)
            ctx_v   = 0
            ctx_inv = 0
            rss_kb  = 0
            with open(os.path.join(path, 'status'), 'r') as f:
                for line in f:
                    if line.startswith('Threads:'):
                        threads = int(line.split()[1])
                    elif line.startswith('voluntary_ctxt_switches:'):
                        ctx_v = int(line.split()[1])
                    elif line.startswith('nonvoluntary_ctxt_switches:'):
                        ctx_inv = int(line.split()[1])
                    elif line.startswith('VmRSS:'):
                        rss_kb = int(line.split()[1])

            # Calculate pmem (Percentage of total physical memory)
            pmem = (rss_kb * 100.0) / total_system_mem_kb

            # 5. Disk I/O (/proc/[pid]/io)
            io_ok = 1
            io_r = 0
            io_w = 0
            io_syscr = 0  # Write IOPS
            io_syscw = 0  # Write IOPS
            try:
                with open(os.path.join(path, 'io'), 'r') as f:
                    for line in f:
                        if line.startswith('read_bytes:'):
                            io_r = int(line.split()[1])
                        elif line.startswith('write_bytes:'):
                            io_w = int(line.split()[1])
                        elif line.startswith('syscr:'):
                            io_syscr = int(line.split()[1])
                        elif line.startswith('syscw:'):
                            io_syscw = int(line.split()[1])

            except (IOError, PermissionError):
                # Permission denied for non-root users on some processes
                io_ok = 0

            # Final JSON structure matching Java CamelCase fields
            processes.append({
                "pid": int(pid_str),
                "ppid": ppid,
                "startTimeMillis": start_time_millis,
                "user": user_name,
                "cmd": cmdline,
                "threads": threads,
                "cores": num_cores,
                "utimeMs": utime_ms,
                "stimeMs": stime_ms,
                "ioWaitMs": iowait_ms,
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

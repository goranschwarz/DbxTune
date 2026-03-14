#!/usr/bin/env python
#############################################################################
## Used by DbxTune collector MonitorPs to get various Process Information
## NOTE: Make sure the line endings are <NL> and not <CR><LF>
#############################################################################

import os
import time
import json
import pwd
import sys

def check_version():
    """Ensure we are running on at least Python 2.7."""
    if sys.version_info < (2, 7):
        error_res = [{
            "error": "Python version too low",
            "detected": "{}.{}".format(sys.version_info.major, sys.version_info.minor),
            "required": "2.7 or higher"
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
    try:
        with open('/proc/uptime', 'r') as f:
            uptime_seconds = float(f.read().split()[0])
            return int(time.time() - uptime_seconds)
    except Exception:
        return int(time.time())

def get_total_memory_kb():
    """Get total physical RAM in KB from /proc/meminfo."""
    try:
        with open('/proc/meminfo', 'r') as f:
            for line in f:
                if line.startswith('MemTotal:'):
                    return int(line.split()[1])
    except Exception:
        pass
    return 1

def get_core_count():
    """Get number of CPU cores. os.cpu_count() is Python 3.4+ only."""
    try:
        # Python 3.4+
        import multiprocessing
        return multiprocessing.cpu_count()
    except (ImportError, AttributeError):
        # Fallback for Python 2.7
        try:
            return os.sysconf('SC_NPROCESSORS_ONLN')
        except (ValueError, OSError, AttributeError):
            return 1

def get_process_stats():
    check_version()
    processes = []
    
    # Get clock ticks per second
    try:
        clk_tck = os.sysconf(os.sysconf_names['SC_CLK_TCK'])
    except (AttributeError, KeyError):
        clk_tck = 100.0
        
    boot_time_sec = get_boot_time()
    total_system_mem_kb = get_total_memory_kb()
    num_cores = get_core_count()

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
                after_comm = raw_stat.split(')')[-1].split()

                ppid = int(after_comm[1])
                utime_ms = (int(after_comm[11]) * 1000) // clk_tck
                stime_ms = (int(after_comm[12]) * 1000) // clk_tck

                start_ticks = int(after_comm[19])
                # In Python 2, we must use float(clk_tck) to avoid integer division
                start_time_seconds = boot_time_sec + (start_ticks / float(clk_tck))
                start_time_millis = int(start_time_seconds * 1000)

                iowait_ms = (int(after_comm[39]) * 1000) // clk_tck

            # 3. Full Command Line
            try:
                # Open as binary to handle null-bytes consistently in Py2/Py3
                with open(os.path.join(path, 'cmdline'), 'rb') as f:
                    content = f.read()
                    # Python 2 requires b'\0' to be safe
                    cmdline = content.replace(b'\0', b' ').decode('utf-8', 'ignore').strip()
            except Exception:
                cmdline = ""

            if not cmdline:
                try:
                    with open(os.path.join(path, 'comm'), 'r') as f:
                        cmdline = "[" + f.read().strip() + "]"
                except Exception:
                    cmdline = "[unknown]"

            # 4. Memory & Context Switches (/proc/[pid]/status)
            ctx_v, ctx_inv, rss_kb, threads = 0, 0, 0, 0
            with open(os.path.join(path, 'status'), 'r') as f:
                for line in f:
                    parts = line.split()
                    if len(parts) < 2: continue
                    if line.startswith('Threads:'):
                        threads = int(parts[1])
                    elif line.startswith('voluntary_ctxt_switches:'):
                        ctx_v = int(parts[1])
                    elif line.startswith('nonvoluntary_ctxt_switches:'):
                        ctx_inv = int(parts[1])
                    elif line.startswith('VmRSS:'):
                        rss_kb = int(parts[1])

            pmem = (rss_kb * 100.0) / total_system_mem_kb

            # 5. Disk I/O (/proc/[pid]/io)
            io_ok, io_r, io_w, io_syscr, io_syscw = 1, 0, 0, 0, 0
            try:
                with open(os.path.join(path, 'io'), 'r') as f:
                    for line in f:
                        p = line.split()
                        if line.startswith('read_bytes:'):    io_r = int(p[1])
                        elif line.startswith('write_bytes:'): io_w = int(p[1])
                        elif line.startswith('syscr:'):       io_syscr = int(p[1])
                        elif line.startswith('syscw:'):       io_syscw = int(p[1])
                io_ok = 1
            except (IOError, OSError):
                io_ok = 0

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

        except (IOError, OSError, KeyError, IndexError, ValueError):
            # Python 2 uses IOError/OSError instead of FileNotFoundError
            continue

    return json.dumps(processes)

if __name__ == "__main__":
    # Use sys.stdout.write for better cross-version output control
    sys.stdout.write(get_process_stats() + '\n')

#############################################################################
## END-OF-SCRIPT --- NOTE: Make sure we have a newline AFTER THIS ROW
#############################################################################

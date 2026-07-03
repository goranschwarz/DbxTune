# DbxTuneStarter

DbxTuneStarter is a companion suite for [DbxTune](https://dbxtune.com/), a database performance monitoring platform. 
It installs DbxTune, runs its database server monitors as a background service, and provides a desktop GUI for watching and controlling that service. 
The solution (`DbxStarter.sln`) consists of three .NET 10 projects:

- **DbxInstaller** — setup wizard that installs, upgrades, or removes DbxTune and the two components below.
- **DbxStarterService** — background service that starts, stops, and monitors the DbxTune server processes.
- **DbxStarterClient** — desktop GUI used to control and observe the service day to day.

Together: DbxInstaller sets everything up, DbxStarterService runs in the background managing the actual DbxTune 
server processes, and DbxStarterClient is the tray app used to keep an eye on things.

## DbxInstaller

A WPF setup wizard (`DbxInstaller.exe`) that runs elevated (admin) and guides an administrator through installing, upgrading, or removing DbxTune on a Windows machine. It:

- Checks prerequisites, such as required ports being free and Java 17+ being available.
- Creates/configures the Windows service account and grants it the necessary security privileges (symlink creation, service logon, debug).
- Downloads (or uses a local copy of) the DbxTune package, extracts it, and sets up the `DBXTUNE_*` environment variables.
- Walks through DBMS-specific configuration for the databases DbxTune should monitor (SQL Server, PostgreSQL, Sybase ASE/RepServer, MySQL/MariaDB, Oracle, DB2, Sybase IQ, SAP HANA, RepAgentX).
- Deploys and registers `DbxStarterService` as a Windows service, and deploys `DbxStarterClient`.
- Configures Windows Firewall rules for the service's web UI/ports.

Run `DbxInstaller.exe` to install/upgrade, or `DbxInstaller.exe --remove` to uninstall.

## DbxStarterService

A cross-platform background service (Windows Service or Linux systemd) that manages the lifecycle of the actual DbxTune server processes. It:

- Reads a `SERVER_LIST` configuration file describing which DbxTune servers to manage, and starts, stops, restarts, and monitors those Java processes, capturing their console output to log files.
- Exposes a web dashboard/API (default `http://localhost:8055`) for status and control, e.g. `GET /api/status`, `POST /api/servers/{name}/start|stop|restart`, log-level control, and log/file reading.
- Exposes a named pipe (`DbxStarterServicePipe`) for local IPC, used by DbxStarterClient.
- Is configured via `DbxStarterService.json` and the `DBXTUNE_*` environment variables, and logs via Serilog.

## DbxStarterClient

A WPF desktop application that acts as the visual control center for DbxStarterService. It:

- Shows a grid of managed DbxTune servers with status, PID, start time, and paths to their logs, config files, and start scripts.
- Lets an administrator start, stop, or restart individual servers, or all of them at once.
- Provides log and config file viewing (with syntax highlighting) and lets you change the service's log level.
- Talks to DbxStarterService over the `DbxStarterServicePipe` named pipe.
- Runs from the system tray, with dark/light theme support and an option to start automatically with Windows.

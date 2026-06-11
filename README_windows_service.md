# Running DbxTune as a Windows Service

DbxTune collectors and DbxCentral can run as Windows services using [Apache Commons Daemon](https://commons.apache.org/proper/commons-daemon/) (prunsrv.exe). This provides automatic startup, proper shutdown handling, and integration with Windows service management.

## Prerequisites

1. **Java 11+** installed and accessible (via `JAVA_HOME` or `PATH`)
2. **Apache Commons Daemon** — download the Windows binaries from:
   https://commons.apache.org/proper/commons-daemon/download_daemon.cgi

   Place `prunsrv.exe` (and optionally `prunmgr.exe` for the GUI monitor) in one of:
   - `%DBXTUNE_HOME%\bin\`
   - Any directory on your `PATH`

   Use the 64-bit version (`amd64/prunsrv.exe`) for 64-bit Java.

3. **`DBXTUNE_HOME`** environment variable set to the DbxTune installation directory (the script will try to auto-detect from its own location if not set)

## Installing a Service

### Collector service (e.g., ASE, SQL Server, PostgreSQL)

```batch
dbxtune_service.bat install <serviceName> <toolName> [collector arguments...]
```

**Examples:**

```batch
rem ASE collector for production server
dbxtune_service.bat install DbxTune__PROD_ASE ase -n my_ase_config.conf -SPROD_ASE -Usa -Psecret

rem SQL Server collector
dbxtune_service.bat install DbxTune__PROD_MSSQL sqlserver -n mssql_config.conf -SPROD_MSSQL

rem PostgreSQL collector
dbxtune_service.bat install DbxTune__PROD_PG postgres -n pg_config.conf -SPROD_PG
```

### DbxCentral service

```batch
dbxtune_service.bat install DbxTune__Central central -C dbxcentral.conf
```

### Available tool names

`ase`, `iq`, `rs`, `rax`, `hana`, `sqlserver`, `oracle`, `postgres`, `mysql`, `db2`, `central`

## Managing Services

```batch
rem Start a service
dbxtune_service.bat start DbxTune__PROD_ASE

rem Stop a service (graceful shutdown, up to 180 second timeout)
dbxtune_service.bat stop DbxTune__PROD_ASE

rem Check service status
dbxtune_service.bat status DbxTune__PROD_ASE

rem Remove a service (stop it first)
dbxtune_service.bat stop DbxTune__PROD_ASE
dbxtune_service.bat uninstall DbxTune__PROD_ASE
```

You can also manage the services through the standard Windows Services console (`services.msc`) or with `sc.exe`.

## Log File Locations

Service log files are written to `%DBXTUNE_SAVE_DIR%\log\` (defaults to `%USERPROFILE%\.dbxtune\log\`):

| File | Contents |
|------|----------|
| `<serviceName>-stdout.log` | Standard output from the DbxTune process |
| `<serviceName>-stderr.log` | Standard error output |
| `<serviceName>.YYYY-MM-DD.log` | Procrun service wrapper log |

DbxTune's own log files (Log4j) are written to the standard DbxTune log location (`%DBXTUNE_SAVE_DIR%\log\`).

## Service Account Considerations

By default, services are installed to run as `LocalSystem`. For production use, consider:

- **Dedicated service account**: Create a Windows user for DbxTune and configure the service to run as that user via `services.msc` or by modifying the `--ServiceUser` parameter in the install script.
- **File permissions**: The service account needs read/write access to:
  - `%DBXTUNE_HOME%` (installation directory)
  - `%DBXTUNE_SAVE_DIR%` (data/log directory, defaults to `%USERPROFILE%\.dbxtune`)
- **Network access**: If connecting to remote DBMS instances, ensure the service account has appropriate network access.
- **SQL Server Windows Authentication**: If using Windows Authentication for SQL Server, the service must run as a domain account with access to the SQL Server instance.

## Customizing JVM Options

The default JVM settings are:
- `-Xmx4096m -Xms64m` (memory)
- `-XX:+HeapDumpOnOutOfMemoryError` (OOM diagnostics)
- `-Djava.awt.headless=true` (no GUI)

To override memory settings, set the `DBXTUNE_JVM_MEMORY_PARAMS` environment variable before running the install command:

```batch
set DBXTUNE_JVM_MEMORY_PARAMS=-Xmx8192m -Xms256m
dbxtune_service.bat install DbxTune__Central central -C dbxcentral.conf
```

To modify JVM options after installation, use `prunmgr.exe` (the Procrun GUI monitor) or `prunsrv.exe //US//<serviceName>`.

## Setting Automatic Startup

By default, services are installed with manual startup. To enable automatic startup:

```batch
sc config DbxTune__PROD_ASE start= auto
```

Or set it via the Windows Services console (`services.msc`).

## Troubleshooting

1. **"Access denied" errors**: Run the command prompt as Administrator.
2. **Service fails to start**: Check the log files in `%DBXTUNE_SAVE_DIR%\log\` for error details.
3. **Service stops immediately**: Verify the collector arguments are correct by first testing with `dbxtune.bat` in a regular command prompt.
4. **JVM not found**: Ensure `JAVA_HOME` or `DBXTUNE_JAVA_HOME` is set correctly, or that `java` is on the system `PATH`.

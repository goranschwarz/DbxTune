# PCS Writer & DdlStorage — Architecture & Troubleshooting Guide

## Overview

The PCS (Persistent Counter Store) is responsible for collecting performance counter samples and persisting them to a local H2 database (or other JDBC target). A secondary responsibility — the focus of this document — is to **look up and store DDL/object definitions** for database objects referenced in counter data, execution plans, and Query Store queries. This is stored in the `MonDdlStorage` table.

---

## Component Map

```
Counter Models (CMs)              (one per DBMS feature area, e.g. CmExecQueryStats)
       │   sendDdlDetailsRequest() called at end of each sample cycle
       │   ├─ Phase 1: top N abs rows
       │   └─ Phase 2: top N per sort column (getDdlDetailsSortOnColName())
       │
       │  Also: SqlServerQueryStoreDdlExtractor (cron), PcsAddDdlObjectDialog (manual),
       │         SqlCaptureBrokerAse (SQL capture), CmObjectActivityPanel (GUI sort)
       │
       ▼
PersistentCounterHandler.addDdl() (coordinator: orchestrates all DDL lookup & storage)
       │
       ├── _ddlInputQueue         (BlockingQueue — incoming DDL lookup requests)
       │         │
       │         ▼
       │   DdlLookupQueueHandler  (thread: dequeues requests, calls Inspector)
       │         │
       │         ▼
       │   ObjectLookupInspectorSqlServer  (fetches DDL from monitored SQL Server)
       │         │
       │         ▼
       │   _ddlStoreQueue         (BlockingQueue — lookup results awaiting storage)
       │         │
       │         ▼
       │   DdlStorageConsumer     (thread: dequeues results, calls all Writers)
       │
       └── Writers (IPersistWriter implementations)
               ├── PersistWriterJdbc / PersistWriterJdbcH2   (primary: H2 or JDBC)
               ├── PersistWriterToBcpFiles
               ├── PersistWriterToDbxCentral
               ├── PersistWriterToHttpJson
               └── PersistWriterToInfluxDb
```

Key source files:

| File | Role |
|------|------|
| `src/com/dbxtune/pcs/PersistentCounterHandler.java` | Central coordinator |
| `src/com/dbxtune/pcs/PersistWriterBase.java` | Table DDL, base cache methods |
| `src/com/dbxtune/pcs/PersistWriterJdbc.java` | Primary writer, saveDdlDetails |
| `src/com/dbxtune/pcs/DdlDetails.java` | Data transfer object for one DDL entry |
| `src/com/dbxtune/pcs/ObjectLookupInspectorAbstract.java` | Base inspector |
| `src/com/dbxtune/pcs/inspection/ObjectLookupInspectorSqlServer.java` | SQL Server DDL fetcher |
| `src/com/dbxtune/pcs/SqlServerQueryStoreDdlExtractor.java` | Scheduled task: sends top-query tables to DDL queue |
| `src/com/dbxtune/pcs/SqlServerQueryStoreExtractor.java` | Scheduled task: copies full Query Store tables to `qs:<dbname>` schema |
| `src/com/dbxtune/cm/CountersModel.java` | Base CM class; `sendDdlDetailsRequest()`, `getDdlDetailsSortOnColName()` |

---

## MonDdlStorage Table

Defined in `PersistWriterBase.java`:

```sql
CREATE TABLE MonDdlStorage (
    dbname          VARCHAR(30)   NOT NULL,   -- database name
    owner           VARCHAR(30)   NOT NULL,   -- schema (e.g. "dbo")
    objectName      VARCHAR(255)  NOT NULL,   -- unqualified object name
    type            VARCHAR(20)   NOT NULL,   -- U=table, P=proc, V=view, TR=trigger, SS=stmt plan, ...
    crdate          TIMESTAMP,                -- object creation date
    sampleTime      TIMESTAMP     NOT NULL,   -- when DDL was captured
    source          VARCHAR(255),             -- who triggered the lookup
    dependParent    VARCHAR(255),             -- parent object (for dependency chains)
    dependLevel     INTEGER       NOT NULL,   -- 0=first, 1=second-level dependency, ...
    dependList      VARCHAR(1500),            -- comma-separated child/dependent objects
    objectText      CLOB,                     -- DDL text or plan analysis JSON
    dependsText     CLOB,                     -- sp_depends output
    optdiagText     CLOB,                     -- reserved, currently always NULL
    extraInfoText   CLOB,                     -- raw XML plan, sp_help output, or index DDL

    PRIMARY KEY (dbname, owner, objectName)
)
```

**Important**: The primary key is `(dbname, owner, objectName)`. `owner` is the schema name. A bare table lookup for `MyTable` (without schema) and a qualified `dbo.MyTable` lookup can end up as **different rows** if the schema resolution differs between lookup attempts.

---

## Writer Lifecycle

### Session Start

When a monitoring session starts:

1. `PersistWriterJdbc.open()` is called via `beginOfSample(PersistContainer)`.
2. The `MonDdlStorage` table is created if it does not exist.
3. `populateDdlDetailesCache()` reads all `(dbname, owner, objectName)` rows from `MonDdlStorage` and loads them into the in-memory cache — preventing redundant lookups for objects already stored in previous sessions.

### Per-Sample Hooks

- `beginOfSample()` → `open()` — opens or reuses connection; handles H2 date-rollover.
- `endOfSample()` → `close()` — closes connection only if `_keepConnOpen=false` (default: stays open).
- DDL storage is **not** part of the per-sample commit; it runs on its own consumer thread independently.

---

## How Counter Models Feed the DDL Store

Every Counter Model (CM) that wants DDL captured for its objects participates through a standard template-method pattern defined in `CountersModel.java`.

### `sendDdlDetailsRequest()` — the driver

Called automatically at the end of every sample cycle, inside `CountersModel.refreshGetData()`, after all diff/rate calculations are complete:

```
refreshGetData()
   ├─ execute counter SQL
   ├─ localCalculation()
   ├─ computeDiffCnt() → diffData
   ├─ computeRatePerSec() → rateData
   └─► sendDdlDetailsRequest(absData, diffData, rateData)   ← here
```

The method runs in **two phases**:

**Phase 1 — Absolute data (all visible rows)**

Iterates the first `getMaxNumOfDdlsToPersist()` rows of the absolute sample in order. For each row where `sendDdlDetailsRequestForSpecificRow()` returns `true`, calls:

```java
pch.addDdl(dbname, objectName, "CmName.abs, row=R");
```

The `source` string recorded in `MonDdlStorage.source` will look like `"CmExecQueryStats.abs, row=3"`.

**Phase 2 — Hot objects from diff/rate data**

Uses the column names returned by `getDdlDetailsSortOnColName()`. For each sort column:

1. Takes a copy of the diff data.
2. Sorts it **descending** by that column (numeric comparison).
3. Sends the first `getMaxNumOfDdlsToPersist()` rows, **skipping any row where the sort-column value is zero** (nothing happened in this sample interval).

```java
pch.addDdl(dbname, objectName, "CmName.diff.sortCol.LogicalReads, row=2");
```

This means each sort column contributes an independent "top N" submission per sample cycle. A CM returning 4 sort columns will submit up to `4 × N` DDL requests per sample.

**Phase 2 is skipped entirely** if `getDdlDetailsSortOnColName()` returns `null` (the base implementation).

### Hook methods CMs override

| Method | Default | Purpose |
|--------|---------|---------|
| `getMaxNumOfDdlsToPersist()` | `0` (disabled) | How many rows per phase. Must be overridden to enable DDL for a CM. |
| `getDdlDetailsColNames()` | `{"DBName", "ObjectName"}` | Names of the columns in the counter data that hold the db name and object name. |
| `getDdlDetailsSortOnColName()` | `null` (no Phase 2) | Column name(s) to sort diff data on for hot-object extraction. |
| `sendDdlDetailsRequestForSpecificRow()` | `return true` | Row-level filter; return `false` to skip a specific row (e.g. skip system objects). |

### SQL Server CMs using `getDdlDetailsSortOnColName()`

| Counter Model | Sort Columns | What Gets DDL-Stored |
|---|---|---|
| `CmExecQueryStats` | `execution_count`, `total_worker_time`, `total_logical_reads`, `total_elapsed_time` | Stored procedures / ad-hoc SQL plans from `sys.dm_exec_query_stats` |
| `CmExecProcedureStats` | `execution_count`, `total_logical_reads`, `total_elapsed_time` | Stored procedures from `sys.dm_exec_procedure_stats` |
| `CmExecFunctionStats` | `execution_count`, `total_logical_reads`, `total_elapsed_time` | Functions from `sys.dm_exec_function_stats` |
| `CmExecTriggerStats` | `execution_count`, `total_logical_reads`, `total_elapsed_time` | Triggers from `sys.dm_exec_trigger_stats` |
| `CmIndexUsage` | `user_seeks`, `user_scans`, `user_lookups`, `user_updates` | Tables/indexes from `sys.dm_db_index_usage_stats` |
| `CmIndexOpStat` | `row_lock_count`, `row_lock_wait_count`, `page_lock_count`, `page_lock_wait_count`, `page_latch_wait_count` | Tables/indexes with lock/latch contention from `sys.dm_db_index_operational_stats` |

### ASE CMs using `getDdlDetailsSortOnColName()`

| Counter Model | Sort Columns | What Gets DDL-Stored |
|---|---|---|
| `CmObjectActivity` | `LogicalReads`, `APFReads`, `PhysicalReads`, `LockWaits` | Tables from `monTableActivity` |
| `CmCachedProcs` | `RequestCntDiff` (ASE 15.5+ only) | Stored procedures from `monCachedProcedures` |
| `CmCachedProcsSum` | `RequestCntDiff` (ASE 15.5+ only) | Stored procedures (summary) |
| `CmStmntCacheDetails` | `UseCount`, `UseCountDiff`, `AvgLIO`, `AvgElapsedTime`, `TotalEstWaitTime` | Statement cache entries from `monStatementCache` |

---

## All DDL Feed Sources

`PersistentCounterHandler.addDdl()` is the single entry point for all DDL lookup requests. The table below lists every source that calls it:

| Source | Trigger | `source` string in MonDdlStorage |
|--------|---------|----------------------------------|
| `CountersModel.sendDdlDetailsRequest()` | Every sample cycle, Phase 1 (abs rows) | `"CmName.abs, row=N"` |
| `CountersModel.sendDdlDetailsRequest()` | Every sample cycle, Phase 2 (hot objects) | `"CmName.diff.sortCol.ColName, row=N"` |
| `SqlServerQueryStoreDdlExtractor` | Cron schedule (default 23:35 daily) | `"QueryStoreDdlExtractor.parsedTables"` |
| `CmObjectActivityPanel` | User sorts GUI table (ASE) | `"CmObjectActivityPanel.guiSorted, row=N"` |
| `PcsAddDdlObjectDialog` | Manual user dialog | `"PcsAddDdlObjectDialog"` |
| `ObjectLookupInspectorSqlServer` | Recursive dependency resolution | `"ObjectLookupInspectorSqlServer.resolve.view"` (etc.) |
| `ObjectLookupInspectorAse` | Recursive dependency resolution | `"ObjectLookupInspectorAse.resolve.*"` |
| `ObjectLookupInspectorPostgres` | Recursive dependency resolution | `"ObjectLookupInspectorPostgres.resolve.*"` |
| `SqlCaptureBrokerAse` | SQL Capture threshold hit | `"SqlCapture.gt.threshold"` |
| `CmPgStatements` | Postgres statement sampling | direct `addDdl()` call |

**Dependency recursion**: when the inspector stores a view or procedure and discovers it references other objects (via `sp_depends` or equivalent), it calls `addDdl()` again with `dependParent` and `dependLevel+1` set. This fills in child objects one level at a time up to a configured depth limit.

---

## DDL Lookup & Storage Flow

### Step 1 — Request Entry (`addDdl`)

Any component can trigger a DDL lookup by calling:

```java
PersistentCounterHandler.getInstance().addDdl(dbname, objectName, source);
```

Sources seen in practice: `"StatementCache"`, `"QueryStoreDdlExtractor.parsedTables"`, `"PcsAddDdlObjectDialog"`, `"CmSqlServerPlanCache"`.

### Step 2 — Early-Exit Filtering in `addDdl`

The request is **silently dropped** if any of these conditions hold:

| # | Condition | Reason |
|---|-----------|--------|
| 1 | `_doDdlLookupAndStore == false` | Global feature flag disabled |
| 2 | No writers configured | `_writerClasses.size() == 0` |
| 3 | dbname or objectName is blank | null/empty check |
| 4 | Same entry already in `_ddlInputQueue` | Duplicate detection on queue |
| 5 | `_objectLookupInspector.allowInspection(entry) == false` | Inspector veto (e.g. temp tables, sys schema) |
| 6 | Feature sub-type mismatch | StatementCache or DatabaseObjects toggle disabled |
| 7 | All writers already have the DDL stored | `isDdlDetailsStored()` returns true for ALL writers |

If the request passes all checks, it is added to `_ddlInputQueue`.

### Step 3 — Inspector Veto Rules (SQL Server)

`ObjectLookupInspectorSqlServer` skips (discards) objects matching any of:

- `dbname == "32767"` → tempdb
- Schema name == `"sys"` → system schema
- Object name starts with `"sys"` or `"SYS"` → system objects
- Object name starts with `"#"` → temporary tables

Discarded objects are added to `_ddlDetailsDiscardCache`, preventing future re-lookup attempts for the lifetime of the process.

### Step 4 — DDL Lookup (`DdlLookupQueueHandler`)

A dedicated background thread polls `_ddlInputQueue` (1-second timeout). For each entry, it calls `ObjectLookupInspectorSqlServer.doObjectInfoLookup()`, which:

1. Re-checks storage cache (last-chance guard).
2. Re-checks discard cache.
3. Queries `[dbname].sys.objects` to find the object's `object_id`, `schema_id`, `type`, `create_date`.
4. **If not found in `sys.objects`**: marks as discarded, returns empty result — **no DDL stored**.
5. **If found**: executes type-specific queries:

| Type | objectText | extraInfoText | dependsText |
|------|-----------|---------------|-------------|
| SS (execution plan) | JSON analysis (warnings, missing indexes, implicit converts, etc.) | Raw XML query plan | — |
| U (user table) | — | `sp_help` output + CREATE INDEX DDL from `sys.dm_db_partition_stats` / `sys.indexes` | child object names |
| P, TR, V, ... | Source text from `sys.syscomments` | — | `sp_depends` output |

Note: `optdiagText` is reserved but **never populated** for SQL Server (see Known Issues).

### Step 5 — Storage (`DdlStorageConsumer`)

A second background thread polls `_ddlStoreQueue`. For each `DdlDetails` result:

1. Checks `ddlDetails.isEmpty()` — if empty, skips.
2. Calls `pw.saveDdlDetails(ddlDetails)` on **all** configured writers.

### Step 6 — `saveDdlDetails` in `PersistWriterJdbc`

Additional silent-exit conditions inside the writer:

| # | Condition | Effect |
|---|-----------|--------|
| 1 | DDL storage connection is null | Returns silently (rate-limited log) |
| 2 | `isShutdownWithNoWait()` | Returns silently (rate-limited log) |
| 3 | `!isSessionStarted()` | Returns silently — table may not exist yet |
| 4 | Cache already contains key | Returns silently |
| 5 | SQLException during INSERT | Logs WARN, returns silently — **not retried** |

If all conditions pass, executes a plain `INSERT INTO MonDdlStorage (...)` — **no UPSERT**. On success, marks four cache key variants as stored:

```
searchDbname:objectName          (bare name, search db)
dbname:objectName                (bare name, resolved db)
searchDbname:searchObjectName    (original qualified name, search db)
dbname:schema.objectName         (schema-qualified, resolved db)
```

Cache keys are **case-insensitive** (both sides `.toLowerCase()`).

---

## In-Memory Cache

```java
// PersistWriterJdbc
Set<String> _ddlDetailsCache        // objects stored (or confirmed already in DB)
Set<String> _ddlDetailsDiscardCache // objects not found (skip future lookups)
```

Both are `Collections.synchronizedSet(new HashSet<>())` — thread-safe but **in-memory only**.

**Cache key format**: `dbname.toLowerCase() + ":" + objectName.toLowerCase()`

**Populated at startup** by `populateDdlDetailesCache()` which loads all rows from `MonDdlStorage` as both bare (`dbname:objectName`) and qualified (`dbname:schema.objectName`) keys.

---

## QueryStore Integration

There are **two separate SQL Server QueryStore components** that interact with DDL storage differently:

### A. `SqlServerQueryStoreDdlExtractor` — feeds MonDdlStorage

- Runs on a cron schedule (default: `35 23 * * *` — 23:35 daily).
- Queries `sys.query_store_runtime_stats` for the top N (default: 40) plans by `avg_duration`.
- Fetches the SQL text of those plans from `sys.query_store_query_text`.
- Parses each SQL text with `SqlParserUtils.getTables()` to extract referenced table names.
- Calls `PersistentCounterHandler.addDdl(dbname, tableName, "QueryStoreDdlExtractor.parsedTables")` for each table.
- Result: table DDLs for frequently-queried tables end up in `MonDdlStorage`.

**Limitation**: Only the top N queries are processed. If a query isn't in the top N by `avg_duration`, its tables won't be picked up. Also, `SqlParserUtils.getTables()` may miss tables in complex SQL (CTEs, dynamic SQL, three-part names).

### B. `SqlServerQueryStoreExtractor` — raw data copy, does NOT feed MonDdlStorage

- Copies all Query Store system tables into a separate `qs:<dbname>` schema in the PCS database.
- Tables copied: `query_store_query_text`, `query_store_query`, `query_store_plan`, `query_store_runtime_stats`, `query_store_wait_stats`, and 9 others (varies by SQL Server version).
- **Does not call `addDdl()`** — the code for that was written but is commented out.
- DDL definitions for Query Store objects are **not** automatically stored in `MonDdlStorage` by this extractor.

---

## Known Issues & Missing SQL Server Data

### Issue 1 — INSERT-Only, No UPSERT

If an object's DDL changes (e.g., table altered, index added), `saveDdlDetails` will never update the existing row because:
- The in-memory cache marks the object as already stored, so the write is skipped.
- Even if the cache were bypassed, a duplicate INSERT against the primary key would raise a constraint violation, which is silently swallowed (WARN log only, no retry).

**Consequence**: Stale DDL survives indefinitely once stored.

### Issue 2 — Silent Loss During Startup Race or Connection Failure

During startup, `populateDdlDetailesCache()` loads existing entries. However:
- If a DDL lookup result arrives **before** the session is fully started (`!isSessionStarted()`), it is silently dropped and never retried.
- If the connection fails at the exact moment of INSERT, the result is silently dropped. The cache does **not** mark it as stored (mark-as-stored is inside the success path), so it would be retried if re-queued — but that only happens if the same object is requested again, which may not occur.

### Issue 3 — QueryStore DDL Extractor Limited to Top 40 by Duration

Only the 40 most expensive queries (by `avg_duration`) contribute table names to `MonDdlStorage`. Tables referenced only by fast or infrequent queries are never stored. **This is the most common reason for missing table DDL in SQL Server environments using Query Store.**

### Issue 4 — SQL Parser May Miss Tables

`SqlParserUtils.getTables()` parses SQL text for table references. It can miss tables in:
- Dynamic SQL (`EXEC(@sql)`)
- Three-part names (`server.db.schema.table`)
- Complex CTEs
- Subqueries wrapped in unusual syntax

Tables missed here are never sent to `addDdl()`.

### Issue 5 — `optdiagText` Is Always Null for SQL Server

The field is reserved but the code path to populate it was never implemented (there is a `// TODO` comment at line 538 of `ObjectLookupInspectorSqlServer.java`). Reports or consumers expecting optimizer diagnostics here will always find null.

### Issue 6 — `QueryStoreExtractor` DDL Code Is Commented Out

`SqlServerQueryStoreExtractor` had logic to extract table names from top queries and call `addDdl()`, but this code is entirely commented out. Consequently the full QueryStore extraction (`qs:<dbname>` schema population) and the `MonDdlStorage` DDL population are **fully disconnected**.

### Issue 7 — Discard Cache Is Permanent for Process Lifetime

If an object lookup fails (object not in `sys.objects` at lookup time), it is added to `_ddlDetailsDiscardCache`. Even if the object is later created in the same session, it won't be re-looked up until the process restarts. This can cause freshly-created tables to be permanently missing from `MonDdlStorage` within a running session.

---

## Suggestions for Improvement

### 1. Implement UPSERT (Merge) for DDL Updates

Replace the INSERT-only `saveDdlDetails` with a MERGE / INSERT-OR-REPLACE pattern so that DDL changes (schema alterations, index changes) are reflected without requiring a restart or manual cache purge. The cache should be updated on successful UPSERT too.

### 2. Expand QueryStore DDL Coverage

Two improvements to `SqlServerQueryStoreDdlExtractor`:
- **Increase top-N, or diversify ranking criteria**: Top 40 by `avg_duration` is narrow. Consider also ranking by `total_duration`, `execution_count`, or `avg_logical_io_reads` to capture a broader set of tables.
- **Re-enable and complete the commented-out code** in `SqlServerQueryStoreExtractor.extractTopTablesForDdlStorage()`: when the full Query Store copy completes, parse the copied SQL texts to extract table names and call `addDdl()`. This would make DDL extraction proportional to the full query population, not just the nightly top-N.

### 3. Connect `SqlServerQueryStoreExtractor` to `MonDdlStorage`

After copying `qs:<dbname>` tables, iterate `query_store_query_text` in the **local copy** and call `addDdl()` for all referenced tables. This is more reliable than parsing from live SQL Server because the text is already local and the query runs against H2, not the monitored server.

### 4. TTL on Discard Cache

Add a configurable TTL (e.g., 1 hour) to `_ddlDetailsDiscardCache` entries. This allows recently-created objects to be picked up without requiring a process restart.

### 5. Retry Failed DDL Saves

When `saveDdlDetails` fails due to a connection error (as opposed to a duplicate key violation), add the entry back to `_ddlStoreQueue` for retry rather than silently discarding it.

### 6. Add UPSERT Source Tracking

The `source` column in `MonDdlStorage` records which component requested the lookup. When an UPSERT updates an existing row, also update `source` and `sampleTime` so operators can see what triggered the latest refresh.

### 7. Populate `optdiagText` for SQL Server

Implement the TODO at `ObjectLookupInspectorSqlServer.java:538` — populate `optdiagText` with output from `sys.dm_db_missing_index_details` or `sys.dm_exec_query_optimizer_info` to surface optimizer-level information alongside the stored DDL.

### 8. Surface DDL Miss Metrics in `PersistWriterStatistics`

Add a counter for DDL saves that were silently dropped — one counter per silent-exit path in `saveDdlDetails` (no connection, shutdown, session not started, cache hit, SQL exception). This would make missing-DDL problems observable in the UI without requiring log analysis.

---

## Appendix: Full Data Flow Diagram

```
┌──────────────────────── DDL Feed Sources ────────────────────────────┐
│                                                                       │
│  CountersModel.refreshGetData()  (every sample interval)             │
│    └─► sendDdlDetailsRequest(absData, diffData, rateData)            │
│          │                                                            │
│          ├─ Phase 1: first N rows of absData (in order)              │
│          │     addDdl(db, obj, "CmName.abs, row=R")                  │
│          │                                                            │
│          └─ Phase 2: for each column in getDdlDetailsSortOnColName() │
│                sort diffData DESC by column                          │
│                addDdl(db, obj, "CmName.diff.sortCol.COL, row=R")     │
│                (rows with zero diff value are skipped)               │
│                                                                       │
│  SqlServerQueryStoreDdlExtractor (cron @ 23:35 daily)                │
│    └─► parse top-N Query Store SQL texts → addDdl() per table        │
│                                                                       │
│  CmObjectActivityPanel  (user GUI sort, ASE)                         │
│    └─► addDdl() for visible sorted rows                              │
│                                                                       │
│  PcsAddDdlObjectDialog  (manual user entry)                          │
│    └─► addDdl()                                                      │
│                                                                       │
│  SqlCaptureBrokerAse  (SQL Capture threshold)                        │
│    └─► addDdl() for captured SQL objects                             │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
            │
            ▼
  PersistentCounterHandler.addDdl(dbname, objectName, source)
            │
       ┌────┴────────────────────────────────────────────────┐
       │  Early-exit checks (7 conditions — see Step 2)      │
       └────┬────────────────────────────────────────────────┘
            │ (passes all checks)
            ▼
    _ddlInputQueue ──(BlockingQueue)──► DdlLookupQueueHandler (thread)
                                                │
                                                ▼
                                 ObjectLookupInspectorSqlServer
                                   .doObjectInfoLookup()
                                         │
                              ┌──────────┴──────────────────┐
                              │ Inspector veto?              │
                              │ (sys schema, #temp,          │
                              │  not found in sys.objects)   │
                              └──────┬───────────────────────┘
                                     │ (not vetoed)
                                     ▼
                              Fetch DDL from SQL Server
                              (sp_help, sys.syscomments,
                               sys.indexes, XML plan, etc.)
                                     │
                                     ├──► If object has dependencies (views, procs):
                                     │        addDdl(depObj, dependParent, level+1)
                                     │        (re-enters the pipeline recursively)
                                     │
                                     ▼
                    _ddlStoreQueue ──(BlockingQueue)──► DdlStorageConsumer (thread)
                                                                │
                                                    ┌───────────┴──────────────┐
                                                    │  isEmpty check           │
                                                    │  connection check        │
                                                    │  session started check   │
                                                    │  cache duplicate check   │
                                                    └───────────┬──────────────┘
                                                                │ (passes)
                                                                ▼
                                                   INSERT INTO MonDdlStorage
                                                   mark 4 cache key variants as stored


SqlServerQueryStoreExtractor (separate cron, independent of above)
            │
            ▼
   Copies sys.query_store_* tables
   ──────────────────────────────► qs:<dbname> schema tables in PCS DB
   (NOT connected to MonDdlStorage — the DDL extraction code is commented out)
```

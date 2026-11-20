# GCP Cloud SQL Monitoring & ErrorLog — Administrator Handout & Setup Guide

This document describes what is required in **Google Cloud Platform (GCP)** and **Cloud SQL for PostgreSQL** so that **DbxTune (`PostgresTune`)** can collect:
1. **PostgreSQL internal metrics** (via standard JDBC/SQL connection on port `5432`).
2. **OS / Host metrics** (`CmOsMpstat`, `CmOsMeminfo`, `CmOsIostat`, `CmOsDiskSpace`, `CmOsNwInfo`, `CmOsUptime`) via the **Google Cloud Monitoring API**.
3. **PostgreSQL Server ErrorLog & `auto_explain` plans** (`CmErrorLog`) via the **Google Cloud Logging API**.

---

## 1. Architecture & Frequently Asked Questions

### Q1: Do we need one "global" Service Account (`dbxtune`) or one per Postgres instance?
* **Recommended: One single "global" Service Account** (e.g., `dbxtune-monitor@<host-project>.iam.gserviceaccount.com`).
* A single `dbxtune-monitor` Service Account can monitor **all** Cloud SQL instances in a project — and even across **multiple GCP projects**!
  * **Single Project:** Grant the IAM roles once at the **Project** level. The Service Account automatically has read-only access to metrics and logs for all Cloud SQL instances in that project.
  * **Multiple Projects:** Do **not** create separate Service Accounts or key files per project if you don't want to. Simply grant the same `dbxtune-monitor@...` Service Account email the required read-only roles on each target GCP project (or once at the **Folder / Organization** level). DbxTune passes the target `project_id` and `instance_id` in each API request while authenticating as that single Service Account.

### Q2: How does authentication differ between a GCP VM vs. an On-Prem / External VM?
| Feature | Scenario A: Running on a GCP Compute Engine VM | Scenario B: Running on an On-Premises VM or Another Cloud |
| :--- | :--- | :--- |
| **How DbxTune Authenticates** | **Attached VM Service Account** (via GCE Metadata Server `169.254.169.254` / Application Default Credentials). | **Service Account JSON Key File** (or Workload Identity Federation JSON / `gcloud auth application-default login`). |
| **JSON Key File Needed?** | **No** (Zero keys to manage or rotate!). | **Yes** — place a read-only JSON key file on the collector VM (e.g., `/etc/dbxtune/gcp-dbxtune-sa.json`, `chmod 600`). |
| **VM Access Scope** | VM must have OAuth scope `https://www.googleapis.com/auth/cloud-platform` (standard when assigning a custom Service Account). | N/A (scope is embedded automatically by DbxTune when using the JSON key file). |

---

## 2. Handout for the GCP Administrator

> [!IMPORTANT]
> Copy or hand the section below to your GCP Administrator. Every permission listed below is strictly **read-only**.

### Part A: Enable Required GCP APIs (in each monitored GCP Project)
Ensure the following APIs are enabled in the project(s) hosting the Cloud SQL instances:
1. **Cloud Monitoring API** (`monitoring.googleapis.com`) — *Host/OS metrics*
2. **Cloud Logging API** (`logging.googleapis.com`) — *Postgres ErrorLog & `auto_explain` plans*
3. **Cloud Trace API** (`cloudtrace.googleapis.com`) — *Optional: GCP Query Insights execution plans*

```bash
gcloud services enable monitoring.googleapis.com logging.googleapis.com cloudtrace.googleapis.com \
  --project=<TARGET_PROJECT_ID>
```

---

### Part B: Create the Service Account & Grant Read-Only IAM Roles

Create **one** Service Account named `dbxtune-monitor`:

```bash
gcloud iam service-accounts create dbxtune-monitor \
  --display-name="DbxTune Read-Only Monitoring & Log Reader" \
  --project=<ADMIN_OR_TARGET_PROJECT_ID>
```

Grant the following **read-only** IAM roles on every GCP project that contains Cloud SQL instances to be monitored:

| IAM Role | Role ID | Why DbxTune Needs It |
| :--- | :--- | :--- |
| **Monitoring Viewer** *(Mandatory)* | `roles/monitoring.viewer` | Reads `cloudsql.googleapis.com/database/*` time-series metrics (CPU, Memory, Disk I/O, Disk Space, Network, Uptime). |
| **Logs Viewer** *(Mandatory)* | `roles/logging.viewer` | Reads `cloudsql.googleapis.com/postgres.log` entries for `CmErrorLog`, deadlock/error alarms, and Daily Summary Report analytics. |
| **Cloud Trace User** *(Recommended)* | `roles/cloudtrace.user` | Reads sampled execution plans captured by **Cloud SQL Query Insights** (`cloudtrace.traces.list` / `cloudtrace.traces.get`). |
| **Cloud SQL Instance User** *(For IAM DB Login)* | `roles/cloudsql.instanceUser` | Required **only if** using the `dbxtune-monitor` Service Account to log into PostgreSQL directly without a SQL password (Cloud SQL IAM Database Auth). |
| **Cloud SQL Client** *(For IAM DB Login / Proxy)* | `roles/cloudsql.client` | Required alongside `roles/cloudsql.instanceUser` for IAM DB authentication or Cloud SQL Auth Proxy. |

```bash
SA_EMAIL="dbxtune-monitor@<ADMIN_OR_TARGET_PROJECT_ID>.iam.gserviceaccount.com"

# Repeat for each <TARGET_PROJECT_ID> (or grant once at the Folder/Org level):
gcloud projects add-iam-policy-binding <TARGET_PROJECT_ID> \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/monitoring.viewer"

gcloud projects add-iam-policy-binding <TARGET_PROJECT_ID> \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/logging.viewer"

gcloud projects add-iam-policy-binding <TARGET_PROJECT_ID> \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/cloudtrace.user"

# If also using 'dbxtune-monitor' for passwordless PostgreSQL login (Cloud SQL IAM DB Auth):
gcloud projects add-iam-policy-binding <TARGET_PROJECT_ID> \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/cloudsql.instanceUser"

gcloud projects add-iam-policy-binding <TARGET_PROJECT_ID> \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/cloudsql.client"

# And register the Service Account as an IAM user on each Cloud SQL instance
# (Note: Cloud SQL automatically omits '.gserviceaccount.com' from the Postgres role name):
gcloud sql users create dbxtune-monitor@<ADMIN_OR_TARGET_PROJECT_ID>.iam \
  --instance=<INSTANCE_ID> \
  --type=CLOUD_IAM_SERVICE_ACCOUNT \
  --project=<TARGET_PROJECT_ID>
```

---

### Part C: Attach Credentials to the DbxTune Collector VM

#### Option 1 — DbxTune Runs on a **GCP Compute Engine VM** *(Recommended)*
1. **Attach the Service Account to the GCP VM** (no JSON key file needed):
   * In the VM configuration, set **Service Account** = `dbxtune-monitor@...`
   * Set **Access Scopes** = `Allow full access to all Cloud APIs` (`https://www.googleapis.com/auth/cloud-platform`) — IAM roles still restrict the VM strictly to `monitoring.viewer` and `logging.viewer`.
   ```bash
   gcloud compute instances set-service-account <DBXTUNE_VM_NAME> \
     --zone=<ZONE> \
     --service-account="${SA_EMAIL}" \
     --scopes="https://www.googleapis.com/auth/cloud-platform"
   ```
2. **Outbound Network from the GCP VM:**
   * **To Cloud SQL:** TCP port `5432` to the Cloud SQL Private IP (via VPC / Shared VPC / Peering) or Authorized Public IP.
   * **To Google APIs (`monitoring.googleapis.com` & `logging.googleapis.com`):** Outbound HTTPS (`443`). If the GCP VM does **not** have an external IP and has no Cloud NAT, enable **Private Google Access** on the VM's subnet.

#### Option 2 — DbxTune Runs on a **Plain VM (On-Premises or Another Cloud)**
1. **Generate a Service Account JSON Key File** (or Workload Identity Federation credential config):
   ```bash
   gcloud iam service-accounts keys create gcp-dbxtune-sa.json \
     --iam-account="${SA_EMAIL}"
   ```
2. Copy `gcp-dbxtune-sa.json` to the DbxTune collector host (e.g. `/etc/dbxtune/gcp-dbxtune-sa.json`) and restrict permissions:
   ```bash
   chmod 600 /etc/dbxtune/gcp-dbxtune-sa.json
   ```
3. **Outbound Network from the On-Prem / External VM:**
   * **To Cloud SQL:** TCP port `5432` (via Cloud VPN / Cloud Interconnect to Private IP, Cloud SQL Auth Proxy, or Authorized Public IP).
   * **To Google APIs:** Outbound HTTPS (`443`) to:
     * `oauth2.googleapis.com` (token refresh)
     * `monitoring.googleapis.com` (metrics)
     * `logging.googleapis.com` (errorlog)

---

### Part D: SQL Commands & Database Flags Inside Cloud SQL for PostgreSQL

*(Also see [postgres.txt](postgres.txt) for the general Postgres setup reference.)*

#### 1. SQL Commands (Run as `postgres` / `cloudsqlsuperuser`)

Connect to the Cloud SQL instance as `postgres` (or another login with `cloudsqlsuperuser`) and execute:

```sql
-- =====================================================================
-- 1. REQUIRED: Grant 'pg_monitor' to the monitoring login
--    (Sees all sessions, SQL text, settings, and stats views,
--     but CANNOT read any user table data or modify anything)
--    Choose EITHER Way 1A (IAM Service Account) OR Way 1B (SQL Password):
-- =====================================================================

-- Way 1A (Recommended on Cloud SQL): Use the 'dbxtune-monitor' GCP Service Account (NO SQL password!)
--   Prerequisite: 'cloudsql.iam_authentication = on' and 'gcloud sql users create ... --type=CLOUD_IAM_SERVICE_ACCOUNT'
GRANT pg_monitor TO "dbxtune-monitor@<PROJECT_ID>.iam";

-- Way 1B (Alternative): Built-in PostgreSQL login with a password
-- CREATE ROLE dbxtune LOGIN PASSWORD 'the-long-and-arbitrary-password';
-- GRANT pg_monitor TO dbxtune;

-- =====================================================================
-- 2. RECOMMENDED EXTENSIONS (in the 'postgres' database)
--    Note: On Cloud SQL, 'pg_stat_statements' is already in
--    shared_preload_libraries by default, so no restart is needed.
-- =====================================================================
\c postgres

-- Top SQL statements ('Statements' tab, DSR 'Top SQL' & index analysis):
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Optional: Buffer cache inspection ('Buffer Cache' tabs):
CREATE EXTENSION IF NOT EXISTS pg_buffercache;

-- Optional: Wait event sampling ('Server Wait' & 'PID Wait' tabs)
-- Only if database flag 'cloudsql.enable_pg_wait_sampling = on' is set:
-- CREATE EXTENSION IF NOT EXISTS pg_wait_sampling;
-- GRANT EXECUTE ON FUNCTION pg_wait_sampling_reset_profile() TO "dbxtune-monitor@<PROJECT_ID>.iam"; -- (or TO dbxtune)

-- =====================================================================
-- 3. RECOMMENDED IN *EVERY* USER DATABASE: 'pgstattuple'
--    Measures actual table bloat ('Table Bloat' tab, TableBloat alarm,
--    and DSR 'Table Bloat'). 'pg_monitor' already includes permission
--    to call pgstattuple_approx(), which only scans non-all-visible pages.
-- =====================================================================
\c <your_app_database_1>
CREATE EXTENSION IF NOT EXISTS pgstattuple;

\c <your_app_database_2>
CREATE EXTENSION IF NOT EXISTS pgstattuple;

-- =====================================================================
-- 4. OPTIONAL (Only on a Primary instance that has Read Replicas):
--    Allows PostgresTune to measure replica replay lag even when the
--    primary is otherwise idle (updated once every 5 minutes).
-- =====================================================================
\c postgres
CREATE TABLE IF NOT EXISTS public.postgrestune_ha_dummy_update (
    id                varchar(36),
    primaryServerName varchar(80),
    ts                timestamptz,
    PRIMARY KEY (id)
);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.postgrestune_ha_dummy_update TO "dbxtune-monitor@<PROJECT_ID>.iam"; -- (or TO dbxtune)
```

> [!NOTE]
> **What is NOT needed on GCP Cloud SQL:**
> - You do **not** need `GRANT pg_read_server_files TO dbxtune;` or `CREATE EXTENSION file_fdw;` on Cloud SQL, because `CmErrorLog` reads the server log via the **GCP Cloud Logging API** (`roles/logging.viewer`).
> - You do **not** need `pg_read_all_data` or `cloudsqlsuperuser` for the `dbxtune` user once the extensions above are created by an admin.
> - *(Note on On-Premises / Self-Managed Postgres VM: If monitoring a self-managed Postgres server via SQL instead of Cloud Logging, also run `GRANT pg_read_server_files TO dbxtune;` and set `logging_collector = on` and `log_destination = 'stderr,jsonlog'`.)*

---

#### 2. Recommended Cloud SQL Database Flags (in GCP Console / `gcloud` / Terraform)

On Cloud SQL, `ALTER SYSTEM` is not allowed; server parameters are configured via **Cloud SQL Database Flags**:

| Category | Cloud SQL Database Flag | Recommended Value | What It Enables in DbxTune |
| :--- | :--- | :--- | :--- |
| **IAM DB Auth** *(if using Way 1A)* | `cloudsql.iam_authentication` | `on` | Allows `dbxtune-monitor@<PROJECT_ID>.iam` to log into PostgreSQL using a GCP OAuth2 token instead of a password. |
| **SQL & I/O Stats** | `pg_stat_statements.max` | `10000` | Tracks up to 10,000 distinct statements (default 5,000). |
| **SQL & I/O Stats** | `pg_stat_statements.track` | `top` *(or `all`)* | Tracks top-level statements (`all` also tracks statements inside PL/pgSQL functions). |
| **SQL & I/O Stats** | `track_io_timing` | `on` | **Critical:** Measures read/write I/O time per statement, table, and database (otherwise I/O times are `0`). |
| **SQL & I/O Stats** | `track_wal_io_timing` | `on` | Measures WAL write/sync I/O times (Postgres 14+). |
| **SQL & I/O Stats** | `track_functions` | `pl` | Populates the `Function Info` (`CmPgFunctions`) tab. |
| **SQL & I/O Stats** | `track_activity_query_size` | `4096` | Prevents active SQL text in `pg_stat_activity` from being truncated at 1024 bytes *(requires restart)*. |
| **Wait Sampling** *(optional)* | `cloudsql.enable_pg_wait_sampling` | `on` | Loads `pg_wait_sampling` on Cloud SQL *(requires restart, then `CREATE EXTENSION pg_wait_sampling;`)*. |
| **Wait Sampling** *(optional)* | `pg_wait_sampling.profile_queries` | `none` | Reduces `pg_wait_sampling` memory/CPU overhead on servers with many short connections. |
| **ErrorLog Analytics** | `log_error_verbosity` | `verbose` | Emits exact **SQLSTATE codes** (e.g., `40P01`, `23505`) in every log line. |
| **ErrorLog Analytics** | `log_checkpoints` | `on` | Logs checkpoint buffer writes and `fsync` latency spikes. |
| **ErrorLog Analytics** | `log_autovacuum_min_duration` | `0` *(or `1000` ms)* | Logs autovacuum/autoanalyze runs, wraparound vacuums, I/O rates, and unremovable dead tuples. |
| **ErrorLog Analytics** | `log_temp_files` | `0` | Logs queries that spill temporary files to disk. |
| **ErrorLog Analytics** | `log_lock_waits` | `on` | Logs lock waits exceeding `deadlock_timeout` with blocking process details. |
| **ErrorLog Analytics** | `log_min_duration_statement` | e.g. `1000` (1s) | Logs slow statements for top-slow-query aggregation in DSR. |
| **Execution Plans** *(optional)* | `session_preload_libraries` | `auto_explain` | With `auto_explain.log_min_duration = 1000` and `auto_explain.log_analyze = on`, captures execution plans into DbxTune's Plan Cache! |

---

#### 3. Verify the `dbxtune` SQL Login

Connect as `dbxtune` and run these 5 checks:

```sql
psql --username dbxtune --host <cloudsql-ip> --port 5432 --dbname postgres

SELECT pg_has_role('pg_monitor', 'member');                                      -- Expected: t
SELECT count(*) FROM pg_stat_activity WHERE query = '<insufficient privilege>';  -- Expected: 0 (all SQL text visible)
SELECT count(*) FROM pg_stat_statements;                                         -- Expected: > 0
SELECT count(*) FROM pg_extension WHERE extname = 'pgstattuple';                 -- Expected: 1 (check in each database)
```

---

## 3. How to Configure DbxTune (`PostgresTune`)

### Option 1: Unattended / CLI Launch with `-u gcp` *(Works Just Like On-Prem `-u osUserName`!)*
When starting `PostgresTune` in No-GUI mode, you can simply pass **`-u gcp`** (or `conn.sshUsername = gcp`), and DbxTune automatically resolves the Cloud SQL **Instance ID** and **GCP Project ID**:
* **Cloud SQL Instance ID is resolved automatically from:**
  1. The hostname in `-S` / the JDBC URL (e.g. `-S pg-orders-01.corp.internal:5432` → `pg-orders-01`), OR
  2. If `-S` is a raw IP address (e.g. `-S 10.20.30.40:5432`), it falls back to `-A <serverAlias>` (e.g. `-A pg-orders-01`)!
* **GCP Project ID is resolved automatically from:**
  1. The IAM database username (`-U dbxtune-monitor@<project-id>.iam` → `<project-id>`), OR
  2. The Service Account JSON key file (`-k /etc/dbxtune/gcp-dbxtune-sa.json`), OR
  3. The GCP Compute Engine VM Metadata Server (when running on a GCP VM) or `GOOGLE_CLOUD_PROJECT`!

```bash
# Example A: On a GCP VM (DNS hostname in -S, attached VM Service Account -> zero passwords, zero key files):
postgrestune.sh -n /path/to/conf -S pg-orders-01.internal:5432 -U dbxtune-monitor@my-prod-project.iam -u gcp

# Example B: Connecting via raw Private IP (-S 10.20.30.40:5432) where -A holds the Cloud SQL instance name:
postgrestune.sh -n /path/to/conf -S 10.20.30.40:5432 -A pg-orders-01 -U dbxtune-monitor@my-prod-project.iam -u gcp

# Example C: On an On-Premises VM (passing the Service Account JSON key file via -k):
postgrestune.sh -n /path/to/conf -S 10.20.30.40:5432 -A pg-orders-01 -U dbxtune-monitor@my-prod-project.iam -u gcp -k /etc/dbxtune/gcp-dbxtune-sa.json
```

---

### Option 2: Interactive GUI (`ConnectionDialog`)
1. **JDBC Tab (PostgreSQL SQL Connection):**
   * **URL:** `jdbc:postgresql://pg-orders-01.internal:5432/postgres` *(or IP address)*
   * **Username:** `dbxtune-monitor@<project-id>.iam` *(or `dbxtune` if using a SQL password)*
   * **Password:** *(leave empty for IAM DB Auth, or enter SQL password)*
2. **Host Monitor (SSH) Tab (Cloud Metrics & ErrorLog):**
   * Check **Monitor Host**.
   * **Username:** `gcp`
   * **Host:** Auto-filled from the JDBC URL (e.g. `pg-orders-01.internal`), or if your JDBC URL uses a raw IP address, enter `<instance-id>` (or `gcp://<project-id>:<region>:<instance-id>`).
   * **Password:** *(leave empty)*
   * **Key File:** *(leave empty on a GCP VM, or select `/etc/dbxtune/gcp-dbxtune-sa.json` on an On-Prem VM)*

---

### Option 3: Explicit `.conf` Properties (Alternative to CLI switches)

```ini
conn.dbmsUsername                  = dbxtune-monitor@my-prod-project.iam
conn.sshUsername                   = gcp
## Optional if not auto-detected from -S / -A / IAM username / GCP VM metadata:
#conn.hostmon.cloud.project         = my-prod-project
#conn.hostmon.cloud.instance        = pg-orders-01
#conn.hostmon.cloud.credentialsFile = /etc/dbxtune/gcp-dbxtune-sa.json
```

---

## 4. Quick Verification Commands (Before Starting DbxTune)

Your admin (or you) can verify that the VM has working credentials, network access, and IAM permissions directly from the shell on the collector VM:

```bash
# If on an On-Prem VM with a key file, activate or point to it first:
# export GOOGLE_APPLICATION_CREDENTIALS=/etc/dbxtune/gcp-dbxtune-sa.json

# 1. Verify Cloud Monitoring API access (should return timeSeries JSON, not 403):
TOKEN=$(gcloud auth application-default print-access-token)
curl -s -H "Authorization: Bearer ${TOKEN}" \
  "https://monitoring.googleapis.com/v3/projects/<PROJECT_ID>/timeSeries?filter=metric.type%3D%22cloudsql.googleapis.com%2Fdatabase%2Fcpu%2Futilization%22&interval.endTime=$(date -u +%Y-%m-%dT%H:%M:%SZ)&interval.startTime=$(date -u -d '10 minutes ago' +%Y-%m-%dT%H:%M:%SZ)" | head -n 25

# 2. Verify Cloud Logging API access (should return log entries JSON, not 403):
curl -s -X POST -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  "https://logging.googleapis.com/v2/entries:list" \
  -d "{
    \"resourceNames\": [\"projects/<PROJECT_ID>\"],
    \"filter\": \"resource.type=\\\"cloudsql_database\\\" AND resource.labels.database_id=\\\"<PROJECT_ID>:<INSTANCE_ID>\\\" AND logName=\\\"projects/<PROJECT_ID>/logs/cloudsql.googleapis.com%2Fpostgres.log\\\"\",
    \"pageSize\": 5,
    \"orderBy\": \"timestamp desc\"
  }" | head -n 30
```

-----------------------------------------------------------------------
-- DbxLoadGen - MySQL - tables
-- Executed as the admin (root) in the load generator database. InnoDB, utf8mb4.
-- Types: datetime(6) for the timestamps (not TIMESTAMP: no 2038 limit and no time zone conversion), text = mediumtext.
-- Batches are separated with 'go'.
-- A comment line '-- FLAW: text' inside a batch is written to the journal as a deliberate design flaw.
-----------------------------------------------------------------------

create table dbxt_loadgen_info (k varchar(60) not null primary key, v varchar(400) null)
go
create table dbxt_loadgen_runlog
(
    run_id        varchar(40)   not null primary key,
    start_ts      datetime(6)   not null,
    end_ts        datetime(6)   null,
    server_name   varchar(100)  null,
    intensity     varchar(10)   null,
    threads       int           null,
    exec_count    bigint        null,
    error_count   bigint        null,
    summary       text          null
)
go

-----------------------------------------------------------------------
-- OLTP "shop"
-----------------------------------------------------------------------
create table category (category_id int not null primary key, name varchar(60) not null, parent_id int null)
go
-- FLAW: no index on email (CustomerSearch looks customers up by e-mail: a scan)
create table customer
(
    customer_id   int           not null primary key,
    first_name    varchar(40)   not null,
    last_name     varchar(40)   not null,
    email         varchar(80)   not null,
    phone         varchar(20)   not null,
    city          varchar(40)   not null,
    country_code  char(2)       not null,
    segment       varchar(10)   not null,
    vip_flag      char(1)       null,
    created_ts    datetime(6)   not null,
    notes         varchar(400)  null
)
go
create table product
(
    product_id    int           not null primary key,
    category_id   int           not null,
    name          varchar(80)   not null,
    description   varchar(400)  null,
    price         decimal(10,2) not null,
    status        varchar(10)   not null,
    created_ts    datetime(6)   not null
)
go
create table warehouse (warehouse_id int not null primary key, name varchar(40) not null, city varchar(40) not null)
go
-- FLAW: the primary key starts with warehouse_id, so lookups by product_id alone scan the table
create table stock
(
    warehouse_id  int           not null,
    product_id    int           not null,
    qty           int           not null,
    reserved      int           not null,
    last_upd_ts   datetime(6)   not null,
    primary key (warehouse_id, product_id)
)
go
create table orders
(
    order_id      int           not null primary key,
    customer_id   int           not null,
    order_ts      datetime(6)   not null,
    status        varchar(10)   not null,
    total_amount  decimal(12,2) not null,
    warehouse_id  int           not null,
    ship_city     varchar(40)   not null,
    comment_txt   varchar(200)  null
)
go
-- FLAW: no index on product_id (a foreign key column), lookups by product scan the table
create table order_line
(
    order_id      int           not null,
    line_no       int           not null,
    product_id    int           not null,
    qty           int           not null,
    price         decimal(10,2) not null,
    primary key (order_id, line_no)
)
go
create table payment
(
    payment_id    int           not null primary key,
    order_id      int           not null,
    pay_ts        datetime(6)   not null,
    amount        decimal(12,2) not null,
    method        varchar(10)   not null
)
go
create table shipment
(
    order_id      int           not null primary key,
    ship_ts       datetime(6)   not null,
    carrier       varchar(20)   not null,
    status        varchar(10)   not null
)
go
-- FLAW: no primary key (InnoDB adds a hidden one), append only (and a big DELETE now and then)
create table audit_log
(
    log_ts        datetime(6)   not null,
    login_name    varchar(30)   not null,
    action        varchar(20)   not null,
    object_id     int           null,
    details       varchar(400)  null
)
go
-- FLAW: a single row per counter that every transaction updates (hot spot)
create table counter_table (name varchar(30) not null primary key, next_val bigint not null)
go
create table session_event
(
    event_id      bigint        not null primary key,
    customer_id   int           not null,
    event_ts      datetime(6)   not null,
    event_type    varchar(20)   not null,
    payload       mediumtext    null
)
go

-----------------------------------------------------------------------
-- DW star schema
-----------------------------------------------------------------------
create table dim_date
(
    date_key      int           not null primary key,
    cal_date      date          not null,
    cal_year      int           not null,
    cal_quarter   int           not null,
    cal_month     int           not null,
    month_name    varchar(10)   not null,
    weekday       int           not null
)
go
create table dim_customer (customer_key int not null primary key, segment varchar(10) not null, country_code char(2) not null, city varchar(40) not null)
go
create table dim_product (product_key int not null primary key, category_name varchar(60) not null, product_name varchar(80) not null)
go
-- FLAW: no primary key, loaded by the ETL scenario
create table fact_sales
(
    order_id      int           not null,
    line_no       int           not null,
    date_key      int           not null,
    customer_key  int           not null,
    product_key   int           not null,
    qty           int           not null,
    amount        decimal(12,2) not null
)
go
create table etl_watermark (name varchar(30) not null primary key, last_order_id int not null)
go

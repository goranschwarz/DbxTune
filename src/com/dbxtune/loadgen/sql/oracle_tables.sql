-----------------------------------------------------------------------
-- DbxLoadGen - Oracle - tables
-- Executed as the admin (system) with current_schema = the schema owner (DBXT_LOADGEN): the tables are created in that
-- schema, in its default tablespace (DBXT_LOADGEN).
-- Types: int = number(10), bigint = number(19), timestamp = timestamp (not DATE: a java.sql.Timestamp bind against a DATE
-- column converts the column, and no index is used), text = clob.
-- Batches are separated with 'go' (no ';' after a SQL statement, PL/SQL ends with 'end;').
-- A comment line '-- FLAW: text' inside a batch is written to the journal as a deliberate design flaw.
-----------------------------------------------------------------------

create table dbxt_loadgen_info (k varchar2(60) not null primary key, v varchar2(400) null)
go
create table dbxt_loadgen_runlog
(
    run_id        varchar2(40)   not null primary key,
    start_ts      timestamp      not null,
    end_ts        timestamp      null,
    server_name   varchar2(100)  null,
    intensity     varchar2(10)   null,
    threads       number(10)     null,
    exec_count    number(19)     null,
    error_count   number(19)     null,
    summary       varchar2(4000) null
)
go

-----------------------------------------------------------------------
-- OLTP "shop"
-----------------------------------------------------------------------
create table category (category_id number(10) not null primary key, name varchar2(60) not null, parent_id number(10) null)
go
-- FLAW: no index on email (CustomerSearch looks customers up by e-mail: a scan)
create table customer
(
    customer_id   number(10)    not null primary key,
    first_name    varchar2(40)  not null,
    last_name     varchar2(40)  not null,
    email         varchar2(80)  not null,
    phone         varchar2(20)  not null,
    city          varchar2(40)  not null,
    country_code  char(2)       not null,
    segment       varchar2(10)  not null,
    vip_flag      char(1)       null,
    created_ts    timestamp     not null,
    notes         varchar2(400) null
)
go
create table product
(
    product_id    number(10)    not null primary key,
    category_id   number(10)    not null,
    name          varchar2(80)  not null,
    description   varchar2(400) null,
    price         number(10,2)  not null,
    status        varchar2(10)  not null,
    created_ts    timestamp     not null
)
go
create table warehouse (warehouse_id number(10) not null primary key, name varchar2(40) not null, city varchar2(40) not null)
go
-- FLAW: the primary key starts with warehouse_id, so lookups by product_id alone scan the table (or skip scan the index)
create table stock
(
    warehouse_id  number(10)    not null,
    product_id    number(10)    not null,
    qty           number(10)    not null,
    reserved      number(10)    not null,
    last_upd_ts   timestamp     not null,
    primary key (warehouse_id, product_id)
)
go
create table orders
(
    order_id      number(10)    not null primary key,
    customer_id   number(10)    not null,
    order_ts      timestamp     not null,
    status        varchar2(10)  not null,
    total_amount  number(12,2)  not null,
    warehouse_id  number(10)    not null,
    ship_city     varchar2(40)  not null,
    comment_txt   varchar2(200) null
)
go
-- FLAW: no index on product_id (a foreign key column), lookups by product scan the table
create table order_line
(
    order_id      number(10)    not null,
    line_no       number(10)    not null,
    product_id    number(10)    not null,
    qty           number(10)    not null,
    price         number(10,2)  not null,
    primary key (order_id, line_no)
)
go
create table payment
(
    payment_id    number(10)    not null primary key,
    order_id      number(10)    not null,
    pay_ts        timestamp     not null,
    amount        number(12,2)  not null,
    method        varchar2(10)  not null
)
go
create table shipment
(
    order_id      number(10)    not null primary key,
    ship_ts       timestamp     not null,
    carrier       varchar2(20)  not null,
    status        varchar2(10)  not null
)
go
-- FLAW: no primary key, append only (and a big DELETE now and then)
create table audit_log
(
    log_ts        timestamp     not null,
    login_name    varchar2(30)  not null,
    action        varchar2(20)  not null,
    object_id     number(10)    null,
    details       varchar2(400) null
)
go
-- FLAW: a single row per counter that every transaction updates (hot spot)
create table counter_table (name varchar2(30) not null primary key, next_val number(19) not null)
go
create table session_event
(
    event_id      number(19)    not null primary key,
    customer_id   number(10)    not null,
    event_ts      timestamp     not null,
    event_type    varchar2(20)  not null,
    payload       clob          null
)
go

-----------------------------------------------------------------------
-- DW star schema
-----------------------------------------------------------------------
create table dim_date
(
    date_key      number(10)    not null primary key,
    cal_date      date          not null,
    cal_year      number(10)    not null,
    cal_quarter   number(10)    not null,
    cal_month     number(10)    not null,
    month_name    varchar2(10)  not null,
    weekday       number(10)    not null
)
go
create table dim_customer (customer_key number(10) not null primary key, segment varchar2(10) not null, country_code char(2) not null, city varchar2(40) not null)
go
create table dim_product (product_key number(10) not null primary key, category_name varchar2(60) not null, product_name varchar2(80) not null)
go
-- FLAW: no primary key, loaded by the ETL scenario
create table fact_sales
(
    order_id      number(10)    not null,
    line_no       number(10)    not null,
    date_key      number(10)    not null,
    customer_key  number(10)    not null,
    product_key   number(10)    not null,
    qty           number(10)    not null,
    amount        number(12,2)  not null
)
go
create table etl_watermark (name varchar2(30) not null primary key, last_order_id number(10) not null)
go

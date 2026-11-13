-----------------------------------------------------------------------
-- DbxLoadGen - DB2 LUW - indexes and procedures (created after the data is loaded)
-- Executed as the admin with CURRENT SCHEMA = the load generator schema (DBXT_LOADGEN).
-- SQL PL procedures; each is sent as one statement by JDBC (no terminator character needed).
-- A comment line '-- FLAW: text' inside a batch is written to the journal as a deliberate design flaw.
-----------------------------------------------------------------------

create index ix_orders_cust     on orders(customer_id)
go
-- FLAW: duplicate of ix_orders_cust (DB2 refuses an exact duplicate, SQL0605W only for unique; the primary key column is added: still redundant)
create index ix_orders_cust_dup on orders(customer_id, order_id)
go
-- FLAW: overlapping, ix_orders_cust is a left prefix of this index
create index ix_orders_cust_ts  on orders(customer_id, order_ts)
go
-- FLAW: never used by the workload
create index ix_orders_ship_city on orders(ship_city)
go
-- (status, order_ts): delivery reads the oldest PAID orders (3% of all, all recent)
create index ix_orders_status   on orders(status, order_ts)
go
create index ix_orders_ts       on orders(order_ts)
go
create index ix_payment_order   on payment(order_id)
go
-- FLAW: VARCHAR column, the non-sargable scenario searches it with a BIGINT: the column is cast, no index use
create index ix_customer_phone  on customer(phone)
go
create index ix_customer_lname  on customer(last_name)
go
-- FLAW: 99% NULL
create index ix_customer_vip    on customer(vip_flag)
go
-- FLAW: 99% of the rows have the same value ('ACTIVE')
create index ix_product_status  on product(status)
go
create index ix_product_cat     on product(category_id)
go
create index ix_fact_sales_date on fact_sales(date_key)
go
create index ix_session_event_cust on session_event(customer_id)
go

-----------------------------------------------------------------------
-- Procedures
-----------------------------------------------------------------------
-- FLAW: customer 1 owns ~30% of the orders, all other customers a few each (one static plan for both)
create procedure p_order_status(in p_customer_id int)
dynamic result sets 1
language sql
begin
    declare c cursor with return to client for
        select o.order_id, o.order_ts, o.status, o.total_amount, count(*) as line_count
        from orders o
        join order_line ol on ol.order_id = o.order_id
        where o.customer_id = p_customer_id
        group by o.order_id, o.order_ts, o.status, o.total_amount
        order by o.order_ts desc;
    open c;
end
go

-- FLAW: row by row processing with a cursor (and a stock lookup by product_id that has no index)
-- each row scans stock: p_max_rows limits the work (the scenario scales it with the size)
create procedure p_rbar_price_adjust(in p_category_id int, in p_max_rows int)
language sql
begin
    declare v_cnt int default 0;
    rows_loop:
    for r as c cursor for select product_id from product where category_id = p_category_id do
        if v_cnt >= p_max_rows then
            leave rows_loop;
        end if;
        update product set price = price * 1.0001        where product_id = r.product_id;
        update stock   set last_upd_ts = current timestamp where product_id = r.product_id;
        set v_cnt = v_cnt + 1;
    end for rows_loop;
end
go

-- FLAW: a "temp table" declared on every call (a T-SQL habit): a declared global temporary table (needs a USER temporary
-- tablespace) - the statements on it are compiled at run time (incremental bind) on every call
create procedure p_temp_report(in p_customer_id int)
language sql
begin
    declare v_total decimal(14,2);
    declare global temporary table session.tmp_o (order_id int, total_amount decimal(12,2)) with replace on commit preserve rows not logged;
    insert into session.tmp_o select order_id, total_amount from orders where customer_id = p_customer_id;
    select sum(total_amount) into v_total from session.tmp_o where total_amount > 100;
    drop table session.tmp_o;
end
go

-----------------------------------------------------------------------
-- DbxLoadGen - Oracle - indexes and procedures (created after the data is loaded)
-- Executed as the admin (system) with current_schema = the schema owner (DBXT_LOADGEN).
-- Procedures are definer's rights: they run with the schema owner's privileges, the dbxt_user_# users only get EXECUTE (via the role).
-- A comment line '-- FLAW: text' inside a batch is written to the journal as a deliberate design flaw.
-----------------------------------------------------------------------

create index ix_orders_cust     on orders(customer_id)
go
-- FLAW: duplicate of ix_orders_cust (Oracle refuses an exact duplicate column list, ORA-01408, so the primary key column is added: still redundant)
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
-- FLAW: VARCHAR2 column, the non-sargable scenario searches it with a NUMBER bind: TO_NUMBER(phone) = :1, no index use
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
-- FLAW: parameter sniffing (bind peeking) - customer 1 owns ~30% of the orders, all other customers a few each
-- The rows are returned as an implicit result (DBMS_SQL.RETURN_RESULT, 12c+), like a T-SQL procedure's result set
create procedure p_order_status(p_customer_id in number)
as
    c sys_refcursor;
begin
    open c for
        select o.order_id, o.order_ts, o.status, o.total_amount, count(*) as line_count
        from orders o
        join order_line ol on ol.order_id = o.order_id
        where o.customer_id = p_customer_id
        group by o.order_id, o.order_ts, o.status, o.total_amount
        order by o.order_ts desc;
    dbms_sql.return_result(c);
end;
go

-- FLAW: row by row processing with a cursor (and a stock lookup by product_id that has no index)
-- each row scans stock: p_max_rows limits the work (the scenario scales it with the size)
create procedure p_rbar_price_adjust(p_category_id in number, p_max_rows in number default 100)
as
    cursor c is select product_id from product where category_id = p_category_id;
    v_product_id number;
    cnt          number := 0;
begin
    open c;
    loop
        fetch c into v_product_id;
        exit when c%notfound;
        update product set price = price * 1.0001     where product_id = v_product_id;
        update stock   set last_upd_ts = systimestamp where product_id = v_product_id;
        cnt := cnt + 1;
        exit when cnt >= p_max_rows;
    end loop;
    close c;
end;
go

-- FLAW: a "temp table" created on every call (a T-SQL habit): a private temporary table (18c+) is created, filled and
-- read with dynamic SQL, and dropped at commit - DDL and new cursors (hard parses) on every call
create procedure p_temp_report(p_customer_id in number)
as
    total number;
begin
    execute immediate 'create private temporary table ora$ptt_tmp_o (order_id number(10), total_amount number(12,2)) on commit drop definition';
    execute immediate 'insert into ora$ptt_tmp_o select order_id, total_amount from orders where customer_id = :1' using p_customer_id;
    execute immediate 'select sum(total_amount) from ora$ptt_tmp_o where total_amount > 100' into total;
    commit; -- drops the table
exception
    when others then
        rollback; -- drops the table too
        raise;
end;
go

-- Statistics for one table of the schema (the update_stats scenario): definer's rights, so the dbxt_user_# users need no ANALYZE ANY
create procedure p_gather_stats(p_table in varchar2)
as
begin
    dbms_stats.gather_table_stats(ownname => sys_context('USERENV', 'CURRENT_SCHEMA'), tabname => p_table);
end;
go

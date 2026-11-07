-----------------------------------------------------------------------
-- DbxLoadGen - Postgres - indexes and procedures (created after the data is loaded)
-- A comment line '-- FLAW: text' inside a batch is written to the journal as a deliberate design flaw.
-----------------------------------------------------------------------

create index ix_orders_cust     on orders(customer_id)
go
-- FLAW: exact duplicate of ix_orders_cust
create index ix_orders_cust_dup on orders(customer_id)
go
-- FLAW: overlapping, ix_orders_cust is a left prefix of this index
create index ix_orders_cust_ts  on orders(customer_id, order_ts)
go
-- FLAW: never used by the workload
create index ix_orders_ship_city on orders(ship_city)
go
-- (status, order_ts): delivery reads the oldest PAID orders (3% of all, all recent). With only (status) the optimizer walked
-- ix_orders_ts from the oldest order: ~10 M logical reads per call at size large (seen 2026-11-07)
create index ix_orders_status   on orders(status, order_ts)
go
create index ix_orders_ts       on orders(order_ts)
go
create index ix_payment_order   on payment(order_id)
go
-- FLAW: VARCHAR column, the non-sargable scenario searches it with a cast to a number (no index use)
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
-- Procedures (functions)
-----------------------------------------------------------------------
-- FLAW: parameter sniffing (generic plan after 5 calls) - customer 1 owns ~30% of the orders, all other customers a few each
create function p_order_status(p_customer_id int)
returns table(order_id int, order_ts timestamp, status varchar, total_amount numeric, line_count bigint)
language plpgsql stable
as $$
begin
    return query
    select o.order_id, o.order_ts, o.status, o.total_amount, count(*) as line_count
    from orders o
    join order_line ol on ol.order_id = o.order_id
    where o.customer_id = p_customer_id
    group by o.order_id, o.order_ts, o.status, o.total_amount
    order by o.order_ts desc;
end
$$
go

-- FLAW: row by row processing in a loop (and a stock lookup by product_id that has no index)
-- each row scans stock: p_max_rows limits the work (the scenario scales it with the size)
create function p_rbar_price_adjust(p_category_id int, p_max_rows int default 100)
returns int
language plpgsql
as $$
declare
    r   record;
    cnt int := 0;
begin
    for r in select product_id from product where category_id = p_category_id loop
        update product set price = price * 1.0001 where product_id = r.product_id;
        update stock   set last_upd_ts = now()     where product_id = r.product_id;
        cnt := cnt + 1;
        exit when cnt >= p_max_rows;
    end loop;
    return cnt;
end
$$
go

-- FLAW: temp table created and dropped on every call (catalog churn)
create function p_temp_report(p_customer_id int)
returns numeric
language plpgsql
as $$
declare
    total numeric;
begin
    create temp table tmp_o (order_id int not null primary key, total_amount numeric(12,2) not null) on commit drop;
    insert into tmp_o select order_id, total_amount from orders where customer_id = p_customer_id;
    create index ix_tmp_amount on tmp_o(total_amount);
    select sum(total_amount) into total from tmp_o where total_amount > 100;
    drop table tmp_o;
    return total;
end
$$
go

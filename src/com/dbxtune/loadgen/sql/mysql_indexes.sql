-----------------------------------------------------------------------
-- DbxLoadGen - MySQL - indexes and procedures (created after the data is loaded)
-- Executed as the admin (root) in the load generator database.
-- Procedures are SQL SECURITY DEFINER (the default): they run with the admin's privileges; the dbxt_user_# users have
-- EXECUTE through the role (all privileges on the database).
-- No DELIMITER: each procedure is sent as one statement by JDBC.
-- A comment line '-- FLAW: text' inside a batch is written to the journal as a deliberate design flaw.
-----------------------------------------------------------------------

create index ix_orders_cust     on orders(customer_id)
go
-- FLAW: duplicate of ix_orders_cust (MySQL allows it, with a warning)
create index ix_orders_cust_dup on orders(customer_id)
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
-- FLAW: VARCHAR column, the non-sargable scenario searches it with a number: the column is converted, no index use
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
-- FLAW: customer 1 owns ~30% of the orders, all other customers a few each (one plan for both)
create procedure p_order_status(in p_customer_id int)
begin
    select o.order_id, o.order_ts, o.status, o.total_amount, count(*) as line_count
    from orders o
    join order_line ol on ol.order_id = o.order_id
    where o.customer_id = p_customer_id
    group by o.order_id, o.order_ts, o.status, o.total_amount
    order by o.order_ts desc;
end
go

-- FLAW: row by row processing with a cursor (and a stock lookup by product_id that has no index)
-- each row scans stock: p_max_rows limits the work (the scenario scales it with the size)
create procedure p_rbar_price_adjust(in p_category_id int, in p_max_rows int)
begin
    declare v_product_id int;
    declare v_done       int default 0;
    declare v_cnt        int default 0;
    declare c cursor for select product_id from product where category_id = p_category_id;
    declare continue handler for not found set v_done = 1;
    open c;
    read_loop: loop
        fetch c into v_product_id;
        if v_done = 1 or v_cnt >= p_max_rows then
            leave read_loop;
        end if;
        update product set price = price * 1.0001          where product_id = v_product_id;
        update stock   set last_upd_ts = current_timestamp(6) where product_id = v_product_id;
        set v_cnt = v_cnt + 1;
    end loop;
    close c;
end
go

-- FLAW: a temp table created and dropped on every call (a T-SQL habit): Created_tmp_tables, and the statements on it
-- are re-prepared every call
create procedure p_temp_report(in p_customer_id int)
begin
    declare v_total decimal(14,2);
    drop temporary table if exists tmp_o;
    create temporary table tmp_o (order_id int, total_amount decimal(12,2));
    insert into tmp_o select order_id, total_amount from orders where customer_id = p_customer_id;
    select sum(total_amount) into v_total from tmp_o where total_amount > 100;
    drop temporary table tmp_o;
end
go

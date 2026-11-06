-----------------------------------------------------------------------
-- DbxLoadGen - Sybase ASE - indexes and procedures (created after the data is loaded)
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
create index ix_orders_status   on orders(status)
go
create index ix_orders_ts       on orders(order_ts)
go
create index ix_payment_order   on payment(order_id)
go
-- FLAW: VARCHAR column, the non-sargable scenario searches it with an NVARCHAR parameter (implicit conversion)
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
-- FLAW: parameter sniffing - customer 1 owns ~30% of the orders, all other customers a few each
create procedure p_order_status
    @customer_id int
as
begin
    set nocount on
    select o.order_id, o.order_ts, o.status, o.total_amount, count(*) as line_count
    from orders o, order_line ol
    where ol.order_id = o.order_id
      and o.customer_id = @customer_id
    group by o.order_id, o.order_ts, o.status, o.total_amount
    order by o.order_ts desc
end
go

-- FLAW: row by row processing with a cursor (and a stock lookup by product_id that has no index)
create procedure p_rbar_price_adjust
    @category_id int
as
begin
    set nocount on
    declare @product_id int
    declare c_prod cursor for
        select product_id from product where category_id = @category_id
        for read only

    open c_prod
    fetch c_prod into @product_id
    while @@sqlstatus = 0
    begin
        update product set price = price * 1.0001 where product_id = @product_id
        update stock   set last_upd_ts = getdate() where product_id = @product_id
        fetch c_prod into @product_id
    end
    close c_prod
    deallocate cursor c_prod
end
go

-- FLAW: temp table + DDL after DML inside the procedure (recompiles)
create procedure p_temp_report
    @customer_id int
as
begin
    set nocount on
    create table #o (order_id int not null primary key, total_amount decimal(12,2) not null)
    insert into #o select order_id, total_amount from orders where customer_id = @customer_id
    create index ix_tmp_amount on #o(total_amount)
    select count(*), sum(total_amount) from #o where total_amount > 100
    drop table #o
end
go

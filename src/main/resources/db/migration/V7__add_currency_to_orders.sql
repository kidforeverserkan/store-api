-- The currency an order was charged in (DKK or EUR). Order amounts
-- (orders.total_price, order_items.unit_price / total_price) are recorded
-- in this currency. Every order that existed before multi-currency support
-- was charged in DKK, so the default backfills them as DKK.
alter table orders
    add currency varchar(3) default 'DKK' not null;

alter table orders
    add constraint orders_currency_check
        check (currency in ('DKK', 'EUR'));

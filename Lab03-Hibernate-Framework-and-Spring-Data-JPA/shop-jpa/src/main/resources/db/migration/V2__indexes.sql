-- Task 5: indexes for the queries the application runs.
-- V1__init.sql is never edited after it was applied; schema changes go into a new version.
CREATE INDEX idx_orders_customer_placed  ON orders (customer_id, placed_at DESC);
CREATE INDEX idx_order_line_order        ON order_line (order_id);
CREATE INDEX idx_product_category_amount ON product (category, amount);

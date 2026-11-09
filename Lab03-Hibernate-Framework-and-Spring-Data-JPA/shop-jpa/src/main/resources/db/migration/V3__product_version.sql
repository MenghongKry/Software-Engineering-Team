-- Challenge: optimistic locking on Product (@Version) for the stock-reservation comparison.
-- A new migration, because V1 and V2 are already applied and must never be edited.
ALTER TABLE product ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

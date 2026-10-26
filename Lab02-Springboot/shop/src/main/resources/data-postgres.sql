INSERT INTO product(name, price, stock) VALUES ('Keyboard', 25.00, 10) ON CONFLICT (name) DO NOTHING;
INSERT INTO product(name, price, stock) VALUES ('Mouse', 12.50, 20) ON CONFLICT (name) DO NOTHING;
INSERT INTO product(name, price, stock) VALUES ('Monitor', 189.99, 5) ON CONFLICT (name) DO NOTHING;
INSERT INTO product(name, price, stock) VALUES ('USB cable', 3.75, 50) ON CONFLICT (name) DO NOTHING;
INSERT INTO product(name, price, stock) VALUES ('Headset', 45.00, 8) ON CONFLICT (name) DO NOTHING;

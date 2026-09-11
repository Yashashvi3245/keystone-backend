-- ============================================================
-- V2 - KEYSTONE seed / reference data
-- NOTE: User passwords are inserted by DataInitializer.java
--       (Spring bean that runs after Flyway, BCrypt-encodes passwords)
-- ============================================================

-- -------------------------------------------------------
-- Seed customers
-- -------------------------------------------------------
INSERT INTO customers (company_name, contact_email)
VALUES
    ('Meridian Corporate HQ',   'hq@meridian.example.com'),
    ('Greenfield Industries',   'contact@greenfield.example.com')
ON CONFLICT (contact_email) DO NOTHING;

-- -------------------------------------------------------
-- Seed sites
-- -------------------------------------------------------
INSERT INTO sites (name, address, city, state, postal_code, customer_id)
SELECT 'Meridian North Tower', '101 Business Park Dr', 'Chicago',   'IL', '60601', id
FROM   customers WHERE contact_email = 'hq@meridian.example.com'
ON CONFLICT DO NOTHING;

INSERT INTO sites (name, address, city, state, postal_code, customer_id)
SELECT 'Meridian South Depot', '202 Industrial Ave',  'Chicago',   'IL', '60609', id
FROM   customers WHERE contact_email = 'hq@meridian.example.com'
ON CONFLICT DO NOTHING;

INSERT INTO sites (name, address, city, state, postal_code, customer_id)
SELECT 'Greenfield Plant A',   '55 Factory Rd',       'Milwaukee', 'WI', '53202', id
FROM   customers WHERE contact_email = 'contact@greenfield.example.com'
ON CONFLICT DO NOTHING;

-- -------------------------------------------------------
-- Seed parts
-- -------------------------------------------------------
INSERT INTO parts (name, stock_quantity, unit_price)
VALUES
    ('Air Filter',          50, 1500),
    ('HVAC Compressor',      8, 45000),
    ('Electrical Breaker',  25, 3200),
    ('Pipe Fitting 2"',    100,  800),
    ('Thermostat Unit',     15, 7500),
    ('Circuit Board',       12, 12000)
ON CONFLICT (name) DO NOTHING;

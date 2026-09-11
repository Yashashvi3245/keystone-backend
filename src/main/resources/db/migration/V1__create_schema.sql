-- ============================================================
-- V1 - KEYSTONE initial schema
-- All tables owned by Flyway; Hibernate ddl-auto=validate only
-- ============================================================

-- -------------------------------------------------------
-- customers
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    id            BIGSERIAL    PRIMARY KEY,
    company_name  VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255) NOT NULL UNIQUE
);

-- -------------------------------------------------------
-- users
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL
                    CHECK (role IN ('DISPATCHER','TECHNICIAN','MANAGER','CUSTOMER')),
    customer_id BIGINT       REFERENCES customers(id) ON DELETE SET NULL
);

-- -------------------------------------------------------
-- sites
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS sites (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    address     VARCHAR(255) NOT NULL,
    city        VARCHAR(255) NOT NULL,
    state       VARCHAR(255) NOT NULL,
    postal_code VARCHAR(20)  NOT NULL,
    customer_id BIGINT       NOT NULL REFERENCES customers(id) ON DELETE CASCADE
);

-- -------------------------------------------------------
-- parts
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS parts (
    id             BIGSERIAL    PRIMARY KEY,
    name           VARCHAR(255) NOT NULL UNIQUE,
    stock_quantity INTEGER      NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    unit_price     INTEGER      NOT NULL DEFAULT 0 CHECK (unit_price >= 0)
);

-- -------------------------------------------------------
-- work_orders
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS work_orders (
    id           BIGSERIAL    PRIMARY KEY,
    code         VARCHAR(20)  NOT NULL UNIQUE,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    priority     VARCHAR(20)  NOT NULL
                     CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
    status       VARCHAR(20)  NOT NULL
                     CHECK (status IN ('NEW','ASSIGNED','IN_PROGRESS','ON_HOLD','COMPLETED','CLOSED','CANCELLED')),
    sla_due_date TIMESTAMP    NOT NULL,
    customer_id  BIGINT       NOT NULL REFERENCES customers(id),
    site_id      BIGINT       NOT NULL REFERENCES sites(id),
    assignee_id  BIGINT       REFERENCES users(id) ON DELETE SET NULL
);

-- -------------------------------------------------------
-- work_order_history  (append-only audit)
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS work_order_history (
    id             BIGSERIAL   PRIMARY KEY,
    work_order_id  BIGINT      NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    from_status    VARCHAR(20),
    to_status      VARCHAR(20) NOT NULL,
    changed_by     BIGINT      REFERENCES users(id) ON DELETE SET NULL,
    changed_at     TIMESTAMP   NOT NULL,
    note           TEXT
);

-- -------------------------------------------------------
-- work_order_parts
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS work_order_parts (
    id            BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT    NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    part_id       BIGINT    NOT NULL REFERENCES parts(id),
    quantity      INTEGER   NOT NULL CHECK (quantity > 0),
    unit_price    INTEGER   NOT NULL CHECK (unit_price >= 0)
);

-- -------------------------------------------------------
-- time_logs
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS time_logs (
    id            BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT    NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    technician_id BIGINT    NOT NULL REFERENCES users(id),
    minutes       INTEGER   NOT NULL CHECK (minutes > 0),
    note          TEXT,
    logged_at     TIMESTAMP NOT NULL
);

-- -------------------------------------------------------
-- notifications
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message    TEXT        NOT NULL,
    read       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL
);

-- -------------------------------------------------------
-- Indexes for common query patterns
-- -------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_work_orders_status      ON work_orders(status);
CREATE INDEX IF NOT EXISTS idx_work_orders_customer_id ON work_orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_work_orders_assignee_id ON work_orders(assignee_id);
CREATE INDEX IF NOT EXISTS idx_work_orders_sla_due     ON work_orders(sla_due_date);
CREATE INDEX IF NOT EXISTS idx_work_order_history_wo   ON work_order_history(work_order_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_id   ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_users_email             ON users(email);

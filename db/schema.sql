-- ============================================================
-- CANTEEN ORDER MANAGEMENT SYSTEM — schema.sql
-- Target: MySQL 8. Run:  mysql -u root -p < db/schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS canteen_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE canteen_db;

-- ------------------------------------------------------------
-- Users: customers and admins. password_hash is SHA-256 hex.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  full_name     VARCHAR(100) NOT NULL,
  email         VARCHAR(150) NOT NULL UNIQUE,
  password_hash CHAR(64)     NOT NULL,
  role          ENUM('CUSTOMER','ADMIN') NOT NULL DEFAULT 'CUSTOMER',
  is_active     TINYINT(1)   NOT NULL DEFAULT 1,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Menu categories (Breakfast, Meals, Snacks, Beverages ...)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
  id   INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(60) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Menu items.
-- NOTE: stock_qty is a small, documented extension of the base
-- schema so the app can demonstrate synchronized stock updates
-- (Unit II). Everything else matches the required columns.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS menu_items (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  category_id  INT NOT NULL,
  name         VARCHAR(120) NOT NULL,
  description  VARCHAR(255) NOT NULL DEFAULT '',
  price        DECIMAL(10,2) NOT NULL,
  is_veg       TINYINT(1) NOT NULL DEFAULT 1,
  is_available TINYINT(1) NOT NULL DEFAULT 1,
  stock_qty    INT NOT NULL DEFAULT 50,
  image_path   VARCHAR(255) NULL,
  CONSTRAINT fk_menu_category FOREIGN KEY (category_id)
    REFERENCES categories(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Orders. Prices are ALWAYS recomputed server-side (service
-- layer) from menu_items — never trusted from the UI.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  user_id         INT NOT NULL,
  status          ENUM('PLACED','CONFIRMED','PREPARING',
                       'READY_FOR_PICKUP','COMPLETED','CANCELLED')
                  NOT NULL DEFAULT 'PLACED',
  payment_method  ENUM('CASH_ON_PICKUP','UPI_SIMULATED') NOT NULL,
  subtotal        DECIMAL(10,2) NOT NULL,
  charges         DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  total           DECIMAL(10,2) NOT NULL,
  pickup_notes    VARCHAR(255) NOT NULL DEFAULT '',
  idempotency_key CHAR(36) NOT NULL UNIQUE,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_order_user FOREIGN KEY (user_id)
    REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS order_items (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  order_id     INT NOT NULL,
  menu_item_id INT NOT NULL,
  quantity     INT NOT NULL,
  unit_price   DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_oi_order FOREIGN KEY (order_id)
    REFERENCES orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_oi_item FOREIGN KEY (menu_item_id)
    REFERENCES menu_items(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Append-only audit of every status change.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_status_history (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  order_id   INT NOT NULL,
  from_status VARCHAR(20) NULL,
  to_status   VARCHAR(20) NOT NULL,
  note        VARCHAR(255) NOT NULL DEFAULT '',
  changed_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_hist_order FOREIGN KEY (order_id)
    REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_orders_user    ON orders(user_id);
CREATE INDEX idx_orders_status  ON orders(status);
CREATE INDEX idx_orders_created ON orders(created_at);
CREATE INDEX idx_items_order    ON order_items(order_id);

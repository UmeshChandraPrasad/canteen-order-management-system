-- ============================================================
-- CANTEEN ORDER MANAGEMENT SYSTEM — seed.sql
-- Run after schema.sql:  mysql -u root -p canteen_db < db/seed.sql
-- Default admin login: admin@canteen.local / admin123
-- (password_hash below is SHA-256 of "admin123")
-- ============================================================
USE canteen_db;

INSERT INTO users (full_name, email, password_hash, role)
VALUES ('Canteen Admin', 'admin@canteen.local',
        '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
        'ADMIN')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

INSERT INTO categories (name) VALUES
  ('Breakfast'), ('Meals'), ('Snacks'), ('Beverages')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ~16 Indian menu items across the 4 categories.
-- (category ids 1..4 assumed from the insert order above)
INSERT INTO menu_items
  (category_id, name, description, price, is_veg, is_available, stock_qty)
VALUES
  (1, 'Masala Dosa',        'Crisp dosa with potato masala, sambar & chutneys', 60.00, 1, 1, 50),
  (1, 'Idli Sambar (4 pc)', 'Soft steamed idlis served with hot sambar',         40.00, 1, 1, 60),
  (1, 'Poha',               'Flattened rice with peanuts, curry leaves & lemon', 35.00, 1, 1, 60),
  (1, 'Aloo Paratha (2 pc)','Stuffed parathas with curd & pickle',               55.00, 1, 1, 40),
  (2, 'Veg Biryani',        'Fragrant basmati biryani with raita & salan',       90.00, 1, 1, 40),
  (2, 'Rajma Chawal',       'Red kidney bean curry with steamed rice',           80.00, 1, 1, 40),
  (2, 'Paneer Butter Masala + 2 Roti', 'Cottage cheese curry with tandoori rotis', 110.00, 1, 1, 30),
  (2, 'Chicken Biryani',    'Hyderabadi chicken dum biryani with raita',        130.00, 0, 1, 30),
  (2, 'Egg Fried Rice',     'Smoky wok-tossed fried rice with egg',              85.00, 0, 1, 35),
  (3, 'Samosa (2 pc)',      'Crispy punjabi samosas with tamarind chutney',      30.00, 1, 1, 80),
  (3, 'Vada Pav',           'Mumbai style vada pav with garlic chutney',         35.00, 1, 1, 60),
  (3, 'Veg Sandwich',       'Grilled triple-layer vegetable sandwich',           50.00, 1, 1, 40),
  (3, 'Gulab Jamun (4 pc)', 'Warm gulab jamuns in rose syrup',                   45.00, 1, 1, 50),
  (4, 'Masala Chai',        'Cutting chai brewed with ginger & spices',          20.00, 1, 1, 100),
  (4, 'Filter Coffee',      'South Indian degree filter coffee',                 30.00, 1, 1, 80),
  (4, 'Mango Lassi',        'Thick mango lassi topped with malai',               55.00, 1, 1, 50);

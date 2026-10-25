# Canteen Order Management System (Java Desktop)

A complete, runnable **Java 17 Swing desktop application** for a college
canteen: customers browse the menu, build a cart and check out; admins run
the kitchen queue, manage the menu, and view sales reports. Built **only**
from the Java course syllabus — no Spring, no Hibernate, no web
frameworks. The only external library is **MySQL Connector/J**.

## Project layout

```
canteen-desktop/
├── src/com/canteen/
│   ├── CanteenApp entry .../ui/CanteenApp.java
│   ├── model/      MenuItem, Order, OrderItem, Cart, CartLine, Category,
│   │               User (+ Customer, Admin), Payment (+ CashOnPickupPayment,
│   │               SimulatedUpiPayment), OrderStatus, PaymentMethod, Role
│   ├── exception/  ItemUnavailableException, EmptyCartException,
│   │               PaymentFailedException, OrderNotFoundException
│   ├── dao/        UserDAO, CategoryDAO, MenuItemDAO, OrderDAO (interfaces)
│   │   └── impl/   Jdbc*Dao — PreparedStatement JDBC implementations
│   ├── service/    AuthService, MenuService, OrderService, ReportService
│   ├── ui/         LoginFrame, RegisterDialog, MainFrame, MenuPanel,
│   │               CartPanel, OrdersPanel, DashboardPanel, KitchenPanel,
│   │               MenuAdminPanel, ReportsPanel, BarChartPanel
│   └── util/       Config, Db, Passwords, Ui
├── db/
│   ├── schema.sql  all tables (MySQL 8 / InnoDB)
│   └── seed.sql    admin user + 16 Indian menu items in 4 categories
├── lib/
│   └── mysql-connector-j.jar
├── config.properties   DB credentials (NOT hardcoded in sources)
├── compile.sh / run.sh / compile.bat / run.bat
└── docs/testing-report.md
```

## Prerequisites

* **Java 17+** (`java -version` should say 17 or higher)
* **MySQL 8** server running locally
* `lib/mysql-connector-j.jar` (already included)

## 1. Database setup (one time)

```bash
# create the schema and tables
mysql -u root -p < db/schema.sql

# create the admin user + menu data
mysql -u root -p canteen_db < db/seed.sql

# create the app login (or reuse root — then edit config.properties)
mysql -u root -p -e "CREATE USER IF NOT EXISTS 'canteen'@'localhost' \
  IDENTIFIED BY 'canteen123'; \
  GRANT ALL PRIVILEGES ON canteen_db.* TO 'canteen'@'localhost'; \
  FLUSH PRIVILEGES;"
```

Check/update `config.properties` so `db.url`, `db.user`, `db.password`
match your MySQL setup. The Java code never hardcodes credentials.

## 2. Compile

Linux / macOS:

```bash
./compile.sh        # or: chmod +x compile.sh first
```

Windows:

```bat
compile.bat
```

This runs `javac` over every file in `src/` with the connector on the
classpath and writes classes to `out/`.

## 3. Run

Linux / macOS: `./run.sh` &nbsp;&nbsp; Windows: `run.bat`

## Default logins

| Role     | Email               | Password   |
|----------|---------------------|------------|
| Admin    | admin@canteen.local | admin123   |
| Customer | —                   | (register in the app) |

## What each screen does

* **Login / Register** — validated forms, SHA-256 password hashing.
* **Menu** (customer) — text search, category pills, veg-only filter,
  sort by name/price; quantity stepper + Add to cart.
* **Cart → Checkout** — step 1 reviews the cart (editable quantities);
  step 2 picks the payment method and pickup notes, then places the order.
* **My Orders** (customer) — live order statuses, auto-refreshed.
* **Dashboard** (admin) — today's revenue, today's order count, live
  orders by status, 7-day revenue bar chart.
* **Manage Orders** (admin/kitchen) — advance orders
  PLACED → CONFIRMED → PREPARING → READY FOR PICKUP → COMPLETED;
  illegal jumps are rejected. Auto-refreshes.
* **Manage Menu** (admin) — add/edit/delete items, availability toggle.
* **Reports** (admin) — today's revenue per item, top-5 sellers,
  CSV export.

## Rules enforced by the app

* Prices are recomputed on the **service layer from DB values** — the UI
  only ever sends item ids and quantities.
* **Cash on Pickup is fully working.** UPI is **SIMULATED ONLY** — every
  UPI control carries the visible label
  *"SIMULATED / DEMO — no real money"* and no gateway is ever contacted.
* All input is validated; failures show friendly `JOptionPane` dialogs,
  never raw stack traces.

## Syllabus mapping (where each unit is demonstrated)

**Unit I — OOP & Java**
* Encapsulation: `model/MenuItem.java`, `Order.java`, `OrderItem.java`,
  `Cart.java`, `CartLine.java`, `Category.java` (all fields private,
  getters/setters).
* Inheritance: `model/User.java` → `Customer.java`, `Admin.java`.
* Abstraction + polymorphism: `model/Payment.java` (abstract `pay()`)
  → `CashOnPickupPayment.java`, `SimulatedUpiPayment.java`;
  `OrderService.placeOrder()` calls only the abstract type.
* Interfaces as abstraction: `dao/UserDAO.java`, `MenuItemDAO.java`,
  `OrderDAO.java`, `CategoryDAO.java` with `dao/impl/Jdbc*Dao`
  implementations.

**Unit II — Exception Handling & Multithreading**
* Custom exceptions: `exception/ItemUnavailableException.java`,
  `EmptyCartException.java`, `PaymentFailedException.java`,
  `OrderNotFoundException.java` — thrown by `Cart`,
  `OrderService`, `MenuService` and shown as friendly dialogs.
* `try-catch-finally` / `try-with-resources`: every DAO method
  (`dao/impl/*`), `Db.rollbackQuietly`/`closeQuietly` in `util/Db.java`,
  transaction handling in `service/OrderService.java`,
  file writing in `ui/ReportsPanel.java`.
* `SwingWorker` for all DB calls: `ui/LoginFrame.java`,
  `RegisterDialog.java`, `MenuPanel.java`, `CartPanel.java`,
  `OrdersPanel.java`, `KitchenPanel.java`, `DashboardPanel.java`,
  `MenuAdminPanel.java`, `ReportsPanel.java` — the UI never freezes.
* Background auto-refresh threads: `ui/OrdersPanel.java` (20 s) and
  `ui/KitchenPanel.java` (15 s) poll the DB on daemon threads and post
  results to the EDT via `SwingWorker`.
* Synchronized stock updates: `service/OrderService.placeOrder()` is
  guarded by `stockLock`, and rows are locked with
  `SELECT ... FOR UPDATE` inside the transaction
  (`dao/impl/JdbcMenuItemDao.findByIdForUpdate`,
  `JdbcOrderDao.lockStatus`).

**Unit III — Collections & Streams**
* `ArrayList`/`HashMap` storage: `model/Cart.java` (HashMap of lines),
  DAO result lists, `service/ReportService.java`.
* Veg-only filter: `service/MenuService.browseMenu()` (`.filter`).
* Sort by price/name: `MenuService.SortOrder` comparators (`.sorted`).
* Bill total via `mapToDouble().sum()`: `model/Cart.getSubtotal()`,
  `service/ReportService.revenueOn()`.
* Sales report via `groupingBy`: `ReportService.revenueByItemOn()`,
  `countsByStatus()`.
* Top-5 selling items: `ReportService.topSellingItems()` (sorted +
  limit over a stream).

**Unit IV — GUI & Event Handling (delegation event model)**
* `ActionListener` on every button, menu item, combo and checkbox:
  `ui/LoginFrame.java`, `MenuPanel.java` (incl. category pills),
  `CartPanel.java`, `KitchenPanel.java`, `MenuAdminPanel.java`, etc.
* `WindowListener` (`WindowAdapter.windowClosing`) confirms before exit:
  `ui/LoginFrame.java`, `ui/MainFrame.java`.
* Screens: Login/Register, Menu Browser, 2-step Cart & Checkout,
  My Orders (live), Admin Dashboard (bar chart = hand-drawn
  `ui/BarChartPanel.java`), Manage Orders, Manage Menu, Sales Reports.

**Unit V — JDBC**
* `DriverManager` connections: `util/Db.getConnection()`
  (`util/Db.java`).
* `PreparedStatement` for **every** query — no string-concatenated SQL:
  `dao/impl/JdbcUserDao.java`, `JdbcCategoryDao.java`,
  `JdbcMenuItemDao.java`, `JdbcOrderDao.java`.
* `ResultSet`-to-object mapping: `mapRow`/`mapOrder`/`mapItem` in the
  `Jdbc*Dao` classes.
* One-transaction checkout (commit/rollback):
  `service/OrderService.placeOrder()` and `advanceStatus()`.
* Credentials from `config.properties` via `util/Config.java` — nothing
  hardcoded.

## Notes & honest limits

* Password hashing is demo-grade SHA-256 (no salt) — fine for coursework,
  not for production.
* `menu_items.stock_qty` is a small documented extension of the required
  schema so synchronized stock updates (Unit II) are demonstrable.
* UPI is simulated by design (see rules above).
* See `docs/testing-report.md` for exactly what was tested and what was
  not.

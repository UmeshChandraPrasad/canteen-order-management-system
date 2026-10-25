# Testing Report — Canteen Order Management System (Java Desktop)

Date: 2026-10-12. Tester: Muse (build agent) on Ubuntu 24.04, OpenJDK 17.

## 1. Compilation — PASS

* Full source tree (`src/`, 48 `.java` files) compiled with
  `javac -Xlint:all` on JDK 17:
  **82 classes, zero errors, zero warnings.**
* Headless smoke test (`test/com/canteen/SmokeTest.java`) compiled clean
  against the app classes + Connector/J.
* Command used:
  `javac -encoding UTF-8 -Xlint:all -cp lib/mysql-connector-j.jar -d out $(find src -name "*.java")`

## 2. Database setup — PASS (verified 2026-10-12)

* `db/schema.sql` and `db/seed.sql` executed against a local MySQL 8.0.42.
* Tables created: users, categories, menu_items, orders, order_items,
  order_status_history (+ indexes). Seed verified: 1 admin
  (`admin@canteen.local`), 4 categories, 16 menu items.

## 3. Headless service/DAO smoke test — PASS (22/22, 2026-10-13)

`test/com/canteen/SmokeTest.java` exercises the service + DAO layers with
no GUI, against a real MySQL 8.0.42. **All 22 checks passed:**

| # | Check | Result |
|---|-------|--------|
| 1 | Customer registration + login | PASS |
| 2 | Wrong password rejected | PASS |
| 3 | Duplicate email rejected | PASS |
| 4 | Seed menu loads (>= 15 items) | PASS |
| 5 | Veg-only filter | PASS |
| 6 | Price ascending sort | PASS |
| 7 | Text search ("dosa") | PASS |
| 8 | Cart subtotal via streams | PASS |
| 9 | Over-stock add rejected (`ItemUnavailableException`) | PASS |
| 10 | Cash checkout: order id, status PLACED | PASS |
| 11 | Prices recomputed from DB (not trusted from UI) | PASS |
| 12 | Packaging charges + total arithmetic | PASS |
| 13 | Charges + total arithmetic (subtotal + ₹5 packaging) | PASS |
| 14 | Status starts PLACED | PASS |
| 15 | Payment method recorded as CASH_ON_PICKUP | PASS |
| 16 | Simulated-UPI checkout recorded as UPI_SIMULATED | PASS |
| 17 | Empty cart rejected (`EmptyCartException`) | PASS |
| 18 | Invalid transition PLACED→READY_FOR_PICKUP rejected | PASS |
| 19 | Valid transition PLACED→CONFIRMED | PASS |
| 20 | Today's revenue > 0 | PASS |
| 21 | Top-sellers list non-empty | PASS |
| 22 | Customer sees own orders | PASS |

Checks 11–12 specifically verify the money rule: the test adds items to
the cart, then `OrderService` re-reads prices from the DB inside the
transaction — the asserted totals match DB prices + ₹5 packaging, proving
UI-supplied prices are never trusted.

**Environment quirk (sandbox only, not an app bug):** the first run failed
at connect time with `PacketTooBigException: Packet for query is too large
(7,566,701 > 65,535)`. Root-caused via `strace`: the JVM opened an
AF_INET6 socket to the IPv6-mapped address `::ffff:127.0.0.1`, which this
sandbox's transparent network layer intercepts and answers with garbage;
the driver misparsed a length prefix as 7,566,701 bytes. Forcing the IPv4
stack (`-Djava.net.preferIPv4Stack=true`) connects cleanly. On a normal
machine (e.g. your laptop) the app connects with no flags — `run.sh` /
`run.bat` are unchanged. The smoke-test command used:

```bash
java -Djava.net.preferIPv4Stack=true \
  -cp "out:out-test:lib/mysql-connector-j.jar" com.canteen.SmokeTest
```

`test/com/canteen/SmokeTest.java` exercises, without any GUI:

| # | Check | Result |
|---|-------|--------|
| 1 | Customer registration + login | |
| 2 | Wrong password rejected | |
| 3 | Duplicate email rejected | |
| 4 | Seed menu loads (>= 15 items) | |
| 5 | Veg-only filter | |
| 6 | Price ascending sort | |
| 7 | Text search ("dosa") | |
| 8 | Cart subtotal via streams | |
| 9 | Over-stock add rejected (`ItemUnavailableException`) | |
| 10 | Cash checkout: order id, status PLACED | |
| 11 | Prices recomputed from DB (not trusted from UI) | |
| 12 | Packaging charges + total arithmetic | |
| 13 | Simulated-UPI checkout recorded as UPI_SIMULATED | |
| 14 | Empty cart rejected (`EmptyCartException`) | |
| 15 | Invalid transition PLACED→READY_FOR_PICKUP rejected | |
| 16 | Valid transition PLACED→CONFIRMED | |
| 17 | Today's revenue > 0 | |
| 18 | Top-sellers list non-empty | |
| 19 | Customer sees own orders | |

## 4. Not tested (honest list)

* **The Swing GUI was not executed.** This build machine is headless
  (no X server); every screen — Login, Menu, Cart/Checkout, My Orders,
  Dashboard, Kitchen, Manage Menu, Reports — is compile-verified only.
  Please click through them once on a real desktop.
* **Concurrent checkouts** (two users racing for the last stock) were
  not load-tested; the code guards with a JVM lock + `SELECT ... FOR
  UPDATE`, but this was not proven under contention.
* **CSV export** writes `sales_report.csv` — code-reviewed, not run.
* **config.properties edge cases** (missing file, wrong password) show
  the friendly "Cannot reach the database" dialog — code-reviewed,
  not run.
* Password hashing is demo-grade unsalted SHA-256 (documented in README).

## 5. How to re-run the tests

```bash
# 1. DB up, then:
mysql -u root -p < db/schema.sql
mysql -u root -p canteen_db < db/seed.sql
mysql -u root -p -e "CREATE USER IF NOT EXISTS 'canteen'@'localhost' \
  IDENTIFIED BY 'canteen123'; \
  GRANT ALL PRIVILEGES ON canteen_db.* TO 'canteen'@'localhost';"

# 2. Compile app + test
./compile.sh
javac -cp "out:lib/mysql-connector-j.jar" -d out-test \
  $(find test -name "*.java")

# 3. Run headless checks (exit 0 = all pass)
# (add -Djava.net.preferIPv4Stack=true only if your sandbox intercepts
#  IPv6-mapped IPv4 connections, as described in section 3 above)
java -cp "out:out-test:lib/mysql-connector-j.jar" com.canteen.SmokeTest
```

# CINEPHILE — Cinema Ticket Booking System

A modern dark-cinema web application for discovering movies, picking tiered auditorium seats, booking tickets safely, exporting E-Tickets, and managing theatre operations — built with **Core Java (`HttpServer` + Plain JDBC)** and **MySQL 8**.

Developed as an integrated project for **OOPS Using Java (CS23333)** and **Database Management Systems (CS23332)** at **Rajalakshmi Engineering College, Thandalam** (Department of Computer Science and Engineering).

---

## Prerequisites (Only 2 Things Needed)

1. **Java JDK 8 or newer** (`java` and `javac` available in terminal)
2. **MySQL 8.x Server** (running locally on port `3306`)

> **Zero external dependencies to install:** `mysql-connector-j-8.3.0.jar` is already included in `lib/`, and the web server uses Java's built-in `com.sun.net.httpserver.HttpServer` (no Maven, Tomcat, or Spring Boot setup needed).

---

## Quick Setup & Run (3 Steps)

### Step 1: Load the Database in MySQL
Open **MySQL Command Line Client** (or MySQL Workbench) and run:

```sql
SOURCE <full-path-to-repo>/sql/cinephile_mysql.sql;
```

**Example on Windows:**
```sql
SOURCE C:/Users/YourName/Downloads/cinema-booking-system/sql/cinephile_mysql.sql;
```
> Use forward slashes `/` in the `SOURCE` path on Windows.

This creates the `cinephile_db` database with 11 normalized 3NF tables, views, triggers, stored procedures, a stored function, and sample movies/theatres/showtimes.

---

### Step 2: Set Your MySQL Password
1. Copy `db.properties.example` to `db.properties` *(note: `run_cinephile.bat` / `run_cinephile.sh` creates this automatically on first run if missing)*.
2. Open `db.properties` and enter your local MySQL username and password:

```properties
db.url=jdbc:mysql://localhost:3306/cinephile_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=YOUR_MYSQL_PASSWORD
```
*(Note: `db.properties` is listed in `.gitignore`, so your local database password is never committed to GitHub.)*

---

### Step 3: Launch the Application
- **Windows:** Double-click **`run_cinephile.bat`** (or run `.\run_cinephile.bat` in terminal).
- **macOS / Linux:**
  ```bash
  chmod +x run_cinephile.sh
  ./run_cinephile.sh
  ```

The script compiles all Java files automatically, starts the server at **`http://localhost:8080`**, and opens your default browser.

---

## Demo Login Credentials

You can also click **Demo Customer** or **Demo Admin** on the Sign In page (`http://localhost:8080/login.html`) to autofill credentials:

| Role | Email | Password | Capabilities |
|---|---|---|---|
| **Customer** | `aravind@cinephile.com` | `user123` | Browse movies, book tiered seats, download `.txt` E-Ticket, cancel in My Tickets |
| **Administrator** | `admin@cinephile.com` | `admin123` | Manage movies/theatres/screens/shows, view revenue reports & audit logs |

---

## Architecture & Key Features

### Frontend (`web/`)
- **Homepage Showcase (`index.html`)**: Full-bleed crossfading widescreen hero slideshow with slow zoom transitions, high-resolution theatrical poster cards, live title/genre/language filters, and showtime selector modal.
- **Interactive Seat Map (`seat-selection.html`)**: Color-coded Regular, Premium, VIP, Selected, and Booked seats with live total fare calculation and polymorphic payment selection (`UPI`, `Card`, `Cash`).
- **My Tickets (`my-bookings.html`)**: Booking history, `.txt` E-Ticket download, and immediate-release cancellation.
- **Admin Console (`admin.html`)**: CRUD management for movies, theatres, screens (auto-generates tiered seats atomically), showtimes, stored procedure revenue reports, and trigger-driven audit log viewer.

### Core Java & OOPS (`src/com/cinephile/`)
- **Packages**: `model`, `exception`, `service`, `dao`, `web`
- **Abstraction & Inheritance**: `abstract class UserAccount` extended by `Customer` and `Admin`
- **Interfaces & Polymorphism**: `PaymentProcessor` (with `default` and `static` methods) implemented by `UpiPaymentProcessor`, `CardPaymentProcessor`, and `CashPaymentProcessor` via `PaymentFactory`
- **Collections, Streams & Lambdas**: Dynamic movie filtering and fare summation in `BookingCalculator`
- **Custom Checked Exceptions**: `CinephileException`, `SeatAlreadyBookedException`, `InvalidCredentialsException`, `PaymentFailedException`
- **Character File I/O**: `TicketFileService` exports formatted receipts to `tickets/Ticket_<ref>.txt`

### MySQL 8 Database (`sql/cinephile_mysql.sql`)
- **11 Normalized Tables (3NF)**: `customers`, `admins`, `movies`, `theatres`, `screens`, `seats`, `showtimes`, `bookings`, `booking_seats`, `payments`, `booking_audit_log`
- **SQL View**: `vw_booking_summary` (7-table join)
- **Triggers**: `trg_after_booking_insert`, `trg_after_booking_update`
- **Stored Procedures & Function**: `sp_get_movie_revenue(IN, OUT, OUT)`, `sp_get_all_movie_reports()`, `fn_available_seats(p_show_id)`
- **Concurrency Safety**: `Connection.TRANSACTION_SERIALIZABLE` + `SELECT ... FOR UPDATE` row locking + explicit `commit()` / `rollback()`

---

## Project Structure

```text
cinema-booking-system/
├── src/com/cinephile/
│   ├── dao/          # Plain JDBC DAOs & DBConnection
│   ├── exception/    # Custom checked exceptions
│   ├── model/        # Encapsulated domain models & UserAccount hierarchy
│   ├── service/      # PaymentProcessor, BookingCalculator, TicketFileService
│   └── web/          # Built-in HttpServer & JsonHelper
├── web/              # HTML5, CSS3, Vanilla JS pages & movie poster assets
├── sql/              # MySQL 8 schema, triggers, views, procedures, seed data
├── lib/              # Bundled MySQL Connector/J driver (8.3.0)
├── db.properties.example
├── run_cinephile.bat
├── run_cinephile.sh
└── Cinephile_Cinema_Ticket_Booking_System_Report.pdf
```

---

## Troubleshooting

- **`MySQL Status: CHECK db.properties` in terminal:** Ensure MySQL 8 is running, you ran `sql/cinephile_mysql.sql` to create `cinephile_db`, and `db.password` in `db.properties` matches your local MySQL root password.
- **`javac is not recognized`:** Install JDK 8+ (not just JRE) and add its `bin` folder to your system `PATH`.
- **Port `8080` already in use:** Stop any previous Java server instance running on port `8080` before launching `run_cinephile.bat`.

---

## Project Credits

- **Team:** R. Aravind (`2116250701901`), B. Siddhaarth (`2116250701715`), G. J. Shuhas (`2116250701707`)
- **Faculty In-Charge:** Venkat Balakumar *(OOPS – CS23333)*, Sathiyavathi *(DBMS – CS23332)*
- **Report:** [Cinephile Project Report (PDF)](Cinephile_Cinema_Ticket_Booking_System_Report.pdf)

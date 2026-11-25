-- ============================================================================
-- CINEPHILE - Cinema Ticket Booking System (MySQL 8 Database Script)
-- Aligned with CS23332 (DBMS) & CS23333 (OOPS Using Java)
-- Includes: 11 Normalized Tables (3NF), Index, View, 2 Triggers, 2 Stored Procedures, 1 Function
-- ============================================================================

CREATE DATABASE IF NOT EXISTS cinephile_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE cinephile_db;

-- Clean drop for idempotent re-runs
DROP VIEW IF EXISTS vw_booking_summary;
DROP TRIGGER IF EXISTS trg_after_booking_insert;
DROP TRIGGER IF EXISTS trg_after_booking_update;
DROP PROCEDURE IF EXISTS sp_get_movie_revenue;
DROP PROCEDURE IF EXISTS sp_get_all_movie_reports;
DROP FUNCTION IF EXISTS fn_available_seats;

DROP TABLE IF EXISTS booking_audit_log;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS booking_seats;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS showtimes;
DROP TABLE IF EXISTS seats;
DROP TABLE IF EXISTS screens;
DROP TABLE IF EXISTS theatres;
DROP TABLE IF EXISTS movies;
DROP TABLE IF EXISTS admins;
DROP TABLE IF EXISTS customers;

-- ============================================================================
-- 1. TABLES (Normalized to 3NF)
-- ============================================================================

CREATE TABLE customers (
    customer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    phone       VARCHAR(20),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE admins (
    admin_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE movies (
    movie_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    genre            VARCHAR(80)  NOT NULL,
    language         VARCHAR(50)  NOT NULL,
    duration_minutes INT          NOT NULL CHECK (duration_minutes > 0),
    description      TEXT,
    release_date     DATE         NOT NULL
);

CREATE TABLE theatres (
    theatre_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    city       VARCHAR(100) NOT NULL,
    address    VARCHAR(255) NOT NULL
);

CREATE TABLE screens (
    screen_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    theatre_id BIGINT       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    total_rows INT          NOT NULL CHECK (total_rows > 0),
    total_cols INT          NOT NULL CHECK (total_cols > 0),
    CONSTRAINT fk_screen_theatre
        FOREIGN KEY (theatre_id) REFERENCES theatres(theatre_id) ON DELETE CASCADE
);

CREATE TABLE seats (
    seat_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    screen_id BIGINT         NOT NULL,
    row_num   INT            NOT NULL,
    col_num   INT            NOT NULL,
    seat_type VARCHAR(20)    NOT NULL DEFAULT 'REGULAR',
    price     DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    CONSTRAINT fk_seat_screen
        FOREIGN KEY (screen_id) REFERENCES screens(screen_id) ON DELETE CASCADE,
    CONSTRAINT uq_screen_row_col
        UNIQUE (screen_id, row_num, col_num)
);

CREATE TABLE showtimes (
    show_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    movie_id  BIGINT      NOT NULL,
    screen_id BIGINT      NOT NULL,
    show_date DATE        NOT NULL,
    show_time TIME        NOT NULL,
    status    VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    CONSTRAINT fk_show_movie
        FOREIGN KEY (movie_id) REFERENCES movies(movie_id) ON DELETE CASCADE,
    CONSTRAINT fk_show_screen
        FOREIGN KEY (screen_id) REFERENCES screens(screen_id) ON DELETE CASCADE
);

CREATE INDEX idx_showtimes_movie_date ON showtimes(movie_id, show_date);

CREATE TABLE bookings (
    booking_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id  BIGINT         NOT NULL,
    show_id      BIGINT         NOT NULL,
    booking_ref  VARCHAR(30)    NOT NULL UNIQUE,
    total_amount DECIMAL(10, 2) NOT NULL CHECK (total_amount >= 0),
    status       VARCHAR(20)    NOT NULL DEFAULT 'CONFIRMED',
    booked_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_show
        FOREIGN KEY (show_id) REFERENCES showtimes(show_id) ON DELETE CASCADE
);

CREATE TABLE booking_seats (
    booking_seat_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id      BIGINT NOT NULL,
    seat_id         BIGINT NOT NULL,
    CONSTRAINT fk_bs_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    CONSTRAINT fk_bs_seat
        FOREIGN KEY (seat_id) REFERENCES seats(seat_id) ON DELETE CASCADE,
    CONSTRAINT uq_booking_seat
        UNIQUE (booking_id, seat_id)
);

CREATE TABLE payments (
    payment_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id     BIGINT         NOT NULL UNIQUE,
    payment_method VARCHAR(30)    NOT NULL,
    amount         DECIMAL(10, 2) NOT NULL CHECK (amount >= 0),
    payment_status VARCHAR(20)    NOT NULL DEFAULT 'PAID',
    paid_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
);

-- Audit table automatically populated by Triggers
CREATE TABLE booking_audit_log (
    audit_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id  BIGINT       NOT NULL,
    action_type VARCHAR(40)  NOT NULL,
    old_status  VARCHAR(20),
    new_status  VARCHAR(20),
    details     VARCHAR(255),
    logged_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 2. SQL VIEW (CS23332 Unit I - Views & Joins)
-- ============================================================================

CREATE VIEW vw_booking_summary AS
SELECT
    b.booking_id,
    b.booking_ref,
    c.customer_id,
    c.name AS customer_name,
    c.email AS customer_email,
    m.movie_id,
    m.title AS movie_title,
    t.name AS theatre_name,
    t.city AS theatre_city,
    sc.name AS screen_name,
    sh.show_date,
    sh.show_time,
    b.total_amount,
    b.status AS booking_status,
    COALESCE(p.payment_method, 'N/A') AS payment_method,
    COALESCE(p.payment_status, 'N/A') AS payment_status,
    b.booked_at
FROM bookings b
JOIN customers c  ON b.customer_id = c.customer_id
JOIN showtimes sh ON b.show_id = sh.show_id
JOIN movies m     ON sh.movie_id = m.movie_id
JOIN screens sc   ON sh.screen_id = sc.screen_id
JOIN theatres t   ON sc.theatre_id = t.theatre_id
LEFT JOIN payments p ON p.booking_id = b.booking_id;

-- ============================================================================
-- 3. TRIGGERS (CS23332 Unit II & Expt. 13 Mandatory Requirement)
-- ============================================================================

DELIMITER $$

CREATE TRIGGER trg_after_booking_insert
AFTER INSERT ON bookings
FOR EACH ROW
BEGIN
    INSERT INTO booking_audit_log (booking_id, action_type, old_status, new_status, details)
    VALUES (
        NEW.booking_id,
        'BOOKING_CREATED',
        NULL,
        NEW.status,
        CONCAT('New booking ', NEW.booking_ref, ' created for amount Rs.', NEW.total_amount)
    );
END$$

CREATE TRIGGER trg_after_booking_update
AFTER UPDATE ON bookings
FOR EACH ROW
BEGIN
    IF OLD.status <> NEW.status THEN
        INSERT INTO booking_audit_log (booking_id, action_type, old_status, new_status, details)
        VALUES (
            NEW.booking_id,
            'BOOKING_STATUS_CHANGED',
            OLD.status,
            NEW.status,
            CONCAT('Booking ', NEW.booking_ref, ' changed from ', OLD.status, ' to ', NEW.status)
        );
    END IF;
END$$

-- ============================================================================
-- 4. STORED PROCEDURES & FUNCTION (CS23332 Unit II & Expt. 13 Mandatory)
-- ============================================================================

CREATE FUNCTION fn_available_seats(p_show_id BIGINT)
RETURNS INT
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_total INT DEFAULT 0;
    DECLARE v_booked INT DEFAULT 0;

    SELECT COUNT(*) INTO v_total
    FROM seats s
    JOIN showtimes sh ON s.screen_id = sh.screen_id
    WHERE sh.show_id = p_show_id;

    SELECT COUNT(*) INTO v_booked
    FROM booking_seats bs
    JOIN bookings b ON bs.booking_id = b.booking_id
    WHERE b.show_id = p_show_id AND b.status = 'CONFIRMED';

    RETURN (v_total - v_booked);
END$$

-- Stored procedure with IN and OUT parameters (called via JDBC CallableStatement)
CREATE PROCEDURE sp_get_movie_revenue(
    IN  p_movie_id      BIGINT,
    OUT p_total_bookings INT,
    OUT p_total_revenue  DECIMAL(10, 2)
)
BEGIN
    SELECT
        COUNT(DISTINCT b.booking_id),
        COALESCE(SUM(b.total_amount), 0.00)
    INTO p_total_bookings, p_total_revenue
    FROM bookings b
    JOIN showtimes sh ON b.show_id = sh.show_id
    WHERE sh.movie_id = p_movie_id
      AND b.status = 'CONFIRMED';
END$$

-- Stored procedure returning full movie-wise booking & revenue summary
CREATE PROCEDURE sp_get_all_movie_reports()
BEGIN
    SELECT
        m.movie_id,
        m.title,
        m.genre,
        COUNT(DISTINCT CASE WHEN b.status = 'CONFIRMED' THEN b.booking_id END) AS confirmed_bookings,
        COUNT(CASE WHEN b.status = 'CONFIRMED' THEN bs.booking_seat_id END) AS tickets_sold,
        COALESCE(SUM(CASE WHEN b.status = 'CONFIRMED' THEN s.price ELSE 0 END), 0.00) AS ticket_revenue
    FROM movies m
    LEFT JOIN showtimes sh    ON m.movie_id = sh.movie_id
    LEFT JOIN bookings b      ON sh.show_id = b.show_id
    LEFT JOIN booking_seats bs ON b.booking_id = bs.booking_id
    LEFT JOIN seats s         ON bs.seat_id = s.seat_id
    GROUP BY m.movie_id, m.title, m.genre
    ORDER BY ticket_revenue DESC, m.movie_id ASC;
END$$

DELIMITER ;

-- ============================================================================
-- 5. SEED DATA
-- ============================================================================

INSERT INTO admins (name, email, password) VALUES
('Cinema Admin', 'admin@cinephile.com', 'admin123');

INSERT INTO customers (name, email, password, phone) VALUES
('R. Aravind', 'aravind@cinephile.com', 'user123', '9876543210'),
('Jane Doe', 'user@cinephile.com', 'user123', '9876501234');

INSERT INTO movies (title, genre, language, duration_minutes, description, release_date) VALUES
('Inception', 'Sci-Fi', 'English', 148,
 'A skilled thief who extracts secrets from within the subconscious is given a final chance at redemption through inception.',
 '2024-07-16'),
('Interstellar', 'Adventure', 'English', 169,
 'A team of explorers travels through a wormhole in space in an attempt to ensure humanity survival.',
 '2024-11-07'),
('The Dark Knight', 'Action', 'English', 152,
 'Batman faces his greatest psychological and physical test when the Joker unleashes chaos on Gotham.',
 '2024-07-18'),
('Leo', 'Action', 'Tamil', 164,
 'A mild-mannered cafe owner in Himachal Pradesh draws the attention of a dangerous cartel from his past.',
 '2025-10-19');

INSERT INTO theatres (name, city, address) VALUES
('Grand Cineplex Downtown', 'Chennai', '123 Anna Salai, Teynampet'),
('Starlight IMAX Cinema', 'Chennai', '456 OMR Road, Thandalam');

INSERT INTO screens (theatre_id, name, total_rows, total_cols) VALUES
(1, 'Screen 1 (IMAX Laser)', 5, 8),
(1, 'Screen 2 (Dolby Atmos)', 4, 6),
(2, 'Main Auditorium', 5, 8);

-- Screen 1 Seats (5 x 8 = 40 seats: Rows 1-2 REGULAR Rs.150, Rows 3-4 PREMIUM Rs.220, Row 5 VIP Rs.320)
INSERT INTO seats (screen_id, row_num, col_num, seat_type, price) VALUES
(1,1,1,'REGULAR',150.00),(1,1,2,'REGULAR',150.00),(1,1,3,'REGULAR',150.00),(1,1,4,'REGULAR',150.00),
(1,1,5,'REGULAR',150.00),(1,1,6,'REGULAR',150.00),(1,1,7,'REGULAR',150.00),(1,1,8,'REGULAR',150.00),
(1,2,1,'REGULAR',150.00),(1,2,2,'REGULAR',150.00),(1,2,3,'REGULAR',150.00),(1,2,4,'REGULAR',150.00),
(1,2,5,'REGULAR',150.00),(1,2,6,'REGULAR',150.00),(1,2,7,'REGULAR',150.00),(1,2,8,'REGULAR',150.00),
(1,3,1,'PREMIUM',220.00),(1,3,2,'PREMIUM',220.00),(1,3,3,'PREMIUM',220.00),(1,3,4,'PREMIUM',220.00),
(1,3,5,'PREMIUM',220.00),(1,3,6,'PREMIUM',220.00),(1,3,7,'PREMIUM',220.00),(1,3,8,'PREMIUM',220.00),
(1,4,1,'PREMIUM',220.00),(1,4,2,'PREMIUM',220.00),(1,4,3,'PREMIUM',220.00),(1,4,4,'PREMIUM',220.00),
(1,4,5,'PREMIUM',220.00),(1,4,6,'PREMIUM',220.00),(1,4,7,'PREMIUM',220.00),(1,4,8,'PREMIUM',220.00),
(1,5,1,'VIP',320.00),(1,5,2,'VIP',320.00),(1,5,3,'VIP',320.00),(1,5,4,'VIP',320.00),
(1,5,5,'VIP',320.00),(1,5,6,'VIP',320.00),(1,5,7,'VIP',320.00),(1,5,8,'VIP',320.00);

-- Screen 2 Seats (4 x 6 = 24 seats: Rows 1-2 REGULAR Rs.140, Row 3 PREMIUM Rs.200, Row 4 VIP Rs.300)
INSERT INTO seats (screen_id, row_num, col_num, seat_type, price) VALUES
(2,1,1,'REGULAR',140.00),(2,1,2,'REGULAR',140.00),(2,1,3,'REGULAR',140.00),(2,1,4,'REGULAR',140.00),(2,1,5,'REGULAR',140.00),(2,1,6,'REGULAR',140.00),
(2,2,1,'REGULAR',140.00),(2,2,2,'REGULAR',140.00),(2,2,3,'REGULAR',140.00),(2,2,4,'REGULAR',140.00),(2,2,5,'REGULAR',140.00),(2,2,6,'REGULAR',140.00),
(2,3,1,'PREMIUM',200.00),(2,3,2,'PREMIUM',200.00),(2,3,3,'PREMIUM',200.00),(2,3,4,'PREMIUM',200.00),(2,3,5,'PREMIUM',200.00),(2,3,6,'PREMIUM',200.00),
(2,4,1,'VIP',300.00),(2,4,2,'VIP',300.00),(2,4,3,'VIP',300.00),(2,4,4,'VIP',300.00),(2,4,5,'VIP',300.00),(2,4,6,'VIP',300.00);

-- Screen 3 Seats (5 x 8 = 40 seats)
INSERT INTO seats (screen_id, row_num, col_num, seat_type, price) VALUES
(3,1,1,'REGULAR',150.00),(3,1,2,'REGULAR',150.00),(3,1,3,'REGULAR',150.00),(3,1,4,'REGULAR',150.00),
(3,1,5,'REGULAR',150.00),(3,1,6,'REGULAR',150.00),(3,1,7,'REGULAR',150.00),(3,1,8,'REGULAR',150.00),
(3,2,1,'REGULAR',150.00),(3,2,2,'REGULAR',150.00),(3,2,3,'REGULAR',150.00),(3,2,4,'REGULAR',150.00),
(3,2,5,'REGULAR',150.00),(3,2,6,'REGULAR',150.00),(3,2,7,'REGULAR',150.00),(3,2,8,'REGULAR',150.00),
(3,3,1,'PREMIUM',220.00),(3,3,2,'PREMIUM',220.00),(3,3,3,'PREMIUM',220.00),(3,3,4,'PREMIUM',220.00),
(3,3,5,'PREMIUM',220.00),(3,3,6,'PREMIUM',220.00),(3,3,7,'PREMIUM',220.00),(3,3,8,'PREMIUM',220.00),
(3,4,1,'PREMIUM',220.00),(3,4,2,'PREMIUM',220.00),(3,4,3,'PREMIUM',220.00),(3,4,4,'PREMIUM',220.00),
(3,4,5,'PREMIUM',220.00),(3,4,6,'PREMIUM',220.00),(3,4,7,'PREMIUM',220.00),(3,4,8,'PREMIUM',220.00),
(3,5,1,'VIP',320.00),(3,5,2,'VIP',320.00),(3,5,3,'VIP',320.00),(3,5,4,'VIP',320.00),
(3,5,5,'VIP',320.00),(3,5,6,'VIP',320.00),(3,5,7,'VIP',320.00),(3,5,8,'VIP',320.00);

INSERT INTO showtimes (movie_id, screen_id, show_date, show_time, status) VALUES
(1, 1, CURDATE(), '14:30:00', 'UPCOMING'),
(1, 1, CURDATE(), '18:30:00', 'UPCOMING'),
(2, 2, CURDATE(), '16:00:00', 'UPCOMING'),
(2, 3, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '19:30:00', 'UPCOMING'),
(3, 3, CURDATE(), '21:00:00', 'UPCOMING'),
(4, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '18:00:00', 'UPCOMING');

-- Sample initial booking (also triggers trg_after_booking_insert automatically!)
INSERT INTO bookings (customer_id, show_id, booking_ref, total_amount, status) VALUES
(1, 1, 'CPH-1001', 300.00, 'CONFIRMED');

INSERT INTO booking_seats (booking_id, seat_id) VALUES
(1, 1),
(1, 2);

INSERT INTO payments (booking_id, payment_method, amount, payment_status) VALUES
(1, 'UPI', 300.00, 'PAID');

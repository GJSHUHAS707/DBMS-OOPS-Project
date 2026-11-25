package com.cinephile.dao;

import com.cinephile.exception.CinephileException;
import com.cinephile.exception.SeatAlreadyBookedException;
import com.cinephile.model.AuditLogEntry;
import com.cinephile.model.BookingSummary;
import com.cinephile.model.MovieReportRow;
import com.cinephile.model.Seat;
import com.cinephile.service.BookingCalculator;
import com.cinephile.service.PaymentProcessor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles Booking Transactions, SQL View reads, Trigger Audit Logs, and Stored Procedures.
 * Demonstrates CS23332 Unit IV (ACID Transactions) & CS23333 Unit V (JDBC PreparedStatement & CallableStatement).
 */
public class BookingDAO {

    public BookingSummary createBooking(long customerId, long showId, List<Seat> selectedSeats,
                                        PaymentProcessor paymentProcessor)
            throws SeatAlreadyBookedException, CinephileException {
        if (selectedSeats == null || selectedSeats.isEmpty()) {
            throw new CinephileException("Please select at least one seat.");
        }

        double totalAmount = BookingCalculator.calculateTotalAmount(selectedSeats);
        paymentProcessor.processPayment(totalAmount);

        String checkSql = "SELECT s.row_num, s.col_num FROM booking_seats bs " +
                          "JOIN bookings b ON bs.booking_id = b.booking_id " +
                          "JOIN seats s ON bs.seat_id = s.seat_id " +
                          "WHERE b.show_id = ? AND b.status = 'CONFIRMED' AND bs.seat_id = ? FOR UPDATE";
        String insertBookingSql = "INSERT INTO bookings (customer_id, show_id, booking_ref, total_amount, status) " +
                                  "VALUES (?, ?, ?, ?, 'CONFIRMED')";
        String insertSeatSql = "INSERT INTO booking_seats (booking_id, seat_id) VALUES (?, ?)";
        String insertPaySql = "INSERT INTO payments (booking_id, payment_method, amount, payment_status) " +
                              "VALUES (?, ?, ?, 'PAID')";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            conn.setAutoCommit(false);
            try {
                // 1. Concurrency check inside transaction
                for (Seat seat : selectedSeats) {
                    try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                        checkPs.setLong(1, showId);
                        checkPs.setLong(2, seat.getSeatId());
                        try (ResultSet rs = checkPs.executeQuery()) {
                            if (rs.next()) {
                                throw new SeatAlreadyBookedException(
                                        "Seat " + seat.getLabel() + " was just booked by another user.");
                            }
                        }
                    }
                }

                // 2. Insert booking (fires trg_after_booking_insert automatically!)
                String bookingRef = "CPH-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                long bookingId;
                try (PreparedStatement bPs = conn.prepareStatement(insertBookingSql, Statement.RETURN_GENERATED_KEYS)) {
                    bPs.setLong(1, customerId);
                    bPs.setLong(2, showId);
                    bPs.setString(3, bookingRef);
                    bPs.setDouble(4, totalAmount);
                    bPs.executeUpdate();
                    try (ResultSet keys = bPs.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Could not retrieve generated booking_id.");
                        }
                        bookingId = keys.getLong(1);
                    }
                }

                // 3. Insert booking_seats
                try (PreparedStatement bsPs = conn.prepareStatement(insertSeatSql)) {
                    for (Seat seat : selectedSeats) {
                        bsPs.setLong(1, bookingId);
                        bsPs.setLong(2, seat.getSeatId());
                        bsPs.addBatch();
                    }
                    bsPs.executeBatch();
                }

                // 4. Insert payment record
                try (PreparedStatement payPs = conn.prepareStatement(insertPaySql)) {
                    payPs.setLong(1, bookingId);
                    payPs.setString(2, paymentProcessor.getMethodName());
                    payPs.setDouble(3, totalAmount);
                    payPs.executeUpdate();
                }

                conn.commit();
                return getBookingById(bookingId);
            } catch (SeatAlreadyBookedException ex) {
                conn.rollback();
                throw ex;
            } catch (SQLException ex) {
                conn.rollback();
                throw new CinephileException("Booking transaction rolled back: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new CinephileException("Database connection error: " + e.getMessage(), e);
        }
    }

    public void cancelBooking(long bookingId) throws CinephileException {
        // Updating status to CANCELLED automatically fires MySQL trigger trg_after_booking_update!
        String sql = "UPDATE bookings SET status = 'CANCELLED' WHERE booking_id = ? AND status = 'CONFIRMED'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new CinephileException("Booking is already cancelled or does not exist.");
            }
        } catch (SQLException e) {
            throw new CinephileException("Failed to cancel booking: " + e.getMessage(), e);
        }
    }

    public BookingSummary getBookingById(long bookingId) throws CinephileException {
        String sql = "SELECT * FROM vw_booking_summary WHERE booking_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSummaryRow(rs);
                }
            }
            throw new CinephileException("Booking not found: " + bookingId);
        } catch (SQLException e) {
            throw new CinephileException("Failed to load booking: " + e.getMessage(), e);
        }
    }

    public List<BookingSummary> getCustomerBookings(long customerId) throws CinephileException {
        return fetchBookingsFromView("SELECT * FROM vw_booking_summary WHERE customer_id = ? ORDER BY booked_at DESC", customerId);
    }

    public List<BookingSummary> getAllBookings() throws CinephileException {
        return fetchBookingsFromView("SELECT * FROM vw_booking_summary ORDER BY booked_at DESC", null);
    }

    private List<BookingSummary> fetchBookingsFromView(String sql, Long customerId) throws CinephileException {
        List<BookingSummary> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (customerId != null) {
                ps.setLong(1, customerId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSummaryRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new CinephileException("Failed to query vw_booking_summary: " + e.getMessage(), e);
        }
    }

    private BookingSummary mapSummaryRow(ResultSet rs) throws SQLException {
        return new BookingSummary(
                rs.getLong("booking_id"),
                rs.getString("booking_ref"),
                rs.getLong("customer_id"),
                rs.getString("customer_name"),
                rs.getString("customer_email"),
                rs.getString("movie_title"),
                rs.getString("theatre_name"),
                rs.getString("theatre_city"),
                rs.getString("screen_name"),
                rs.getDate("show_date").toLocalDate(),
                rs.getTime("show_time").toLocalTime(),
                rs.getDouble("total_amount"),
                rs.getString("booking_status"),
                rs.getString("payment_method"),
                rs.getString("payment_status"),
                rs.getTimestamp("booked_at")
        );
    }

    public List<String> getSeatLabelsForBooking(long bookingId) throws CinephileException {
        List<String> labels = new ArrayList<>();
        String sql = "SELECT s.row_num, s.col_num, s.seat_type FROM booking_seats bs " +
                     "JOIN seats s ON bs.seat_id = s.seat_id " +
                     "WHERE bs.booking_id = ? ORDER BY s.row_num, s.col_num";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    char rowChar = (char) ('A' + rs.getInt("row_num") - 1);
                    labels.add(rowChar + String.valueOf(rs.getInt("col_num")) + " (" + rs.getString("seat_type") + ")");
                }
            }
            return labels;
        } catch (SQLException e) {
            throw new CinephileException("Failed to fetch seat labels: " + e.getMessage(), e);
        }
    }

    /**
     * Calls MySQL Stored Procedure sp_get_movie_revenue(IN, OUT, OUT) via JDBC CallableStatement.
     */
    public String getMovieRevenueViaProcedure(long movieId, String movieTitle) throws CinephileException {
        String call = "{CALL sp_get_movie_revenue(?, ?, ?)}";
        try (Connection conn = DBConnection.getConnection();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setLong(1, movieId);
            cs.registerOutParameter(2, Types.INTEGER);
            cs.registerOutParameter(3, Types.DECIMAL);
            cs.execute();
            int count = cs.getInt(2);
            double rev = cs.getDouble(3);
            return "Stored Procedure Result for '" + movieTitle + "': "
                    + count + " Confirmed Booking(s), Total Revenue = Rs." + String.format("%.2f", rev);
        } catch (SQLException e) {
            throw new CinephileException("Failed calling sp_get_movie_revenue: " + e.getMessage(), e);
        }
    }

    /**
     * Calls MySQL Stored Procedure sp_get_all_movie_reports() via JDBC CallableStatement.
     */
    public List<MovieReportRow> getAllMovieReportsViaProcedure() throws CinephileException {
        List<MovieReportRow> rows = new ArrayList<>();
        String call = "{CALL sp_get_all_movie_reports()}";
        try (Connection conn = DBConnection.getConnection();
             CallableStatement cs = conn.prepareCall(call);
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                rows.add(new MovieReportRow(
                        rs.getLong("movie_id"),
                        rs.getString("title"),
                        rs.getString("genre"),
                        rs.getInt("confirmed_bookings"),
                        rs.getInt("tickets_sold"),
                        rs.getDouble("ticket_revenue")
                ));
            }
            return rows;
        } catch (SQLException e) {
            throw new CinephileException("Failed calling sp_get_all_movie_reports: " + e.getMessage(), e);
        }
    }

    /**
     * Reads booking_audit_log rows created automatically by MySQL Triggers.
     */
    public List<AuditLogEntry> getAuditLogs() throws CinephileException {
        List<AuditLogEntry> logs = new ArrayList<>();
        String sql = "SELECT audit_id, booking_id, action_type, old_status, new_status, details, logged_at " +
                     "FROM booking_audit_log ORDER BY audit_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                logs.add(new AuditLogEntry(
                        rs.getLong("audit_id"),
                        rs.getLong("booking_id"),
                        rs.getString("action_type"),
                        rs.getString("old_status"),
                        rs.getString("new_status"),
                        rs.getString("details"),
                        rs.getTimestamp("logged_at")
                ));
            }
            return logs;
        } catch (SQLException e) {
            throw new CinephileException("Failed to fetch trigger audit logs: " + e.getMessage(), e);
        }
    }
}

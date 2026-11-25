package com.cinephile.dao;

import com.cinephile.exception.CinephileException;
import com.cinephile.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class MovieShowDAO {

    public List<Movie> getAllMovies() throws CinephileException {
        List<Movie> list = new ArrayList<>();
        String sql = "SELECT movie_id, title, genre, language, duration_minutes, description, release_date " +
                     "FROM movies ORDER BY movie_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Date rel = rs.getDate("release_date");
                list.add(new Movie(
                        rs.getLong("movie_id"),
                        rs.getString("title"),
                        rs.getString("genre"),
                        rs.getString("language"),
                        rs.getInt("duration_minutes"),
                        rs.getString("description"),
                        rel != null ? rel.toLocalDate() : LocalDate.now()
                ));
            }
            return list;
        } catch (SQLException e) {
            throw new CinephileException("Failed to load movies: " + e.getMessage(), e);
        }
    }

    public void addMovie(String title, String genre, String language, int duration,
                         String description, LocalDate releaseDate) throws CinephileException {
        String sql = "INSERT INTO movies (title, genre, language, duration_minutes, description, release_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title.trim());
            ps.setString(2, genre.trim());
            ps.setString(3, language.trim());
            ps.setInt(4, duration);
            ps.setString(5, description.trim());
            ps.setDate(6, Date.valueOf(releaseDate));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new CinephileException("Failed to add movie: " + e.getMessage(), e);
        }
    }

    public void deleteMovie(long movieId) throws CinephileException {
        String sql = "DELETE FROM movies WHERE movie_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, movieId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new CinephileException("Failed to delete movie: " + e.getMessage(), e);
        }
    }

    public List<Theatre> getAllTheatres() throws CinephileException {
        List<Theatre> list = new ArrayList<>();
        String sql = "SELECT theatre_id, name, city, address FROM theatres ORDER BY theatre_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Theatre(
                        rs.getLong("theatre_id"),
                        rs.getString("name"),
                        rs.getString("city"),
                        rs.getString("address")
                ));
            }
            return list;
        } catch (SQLException e) {
            throw new CinephileException("Failed to load theatres: " + e.getMessage(), e);
        }
    }

    public void addTheatre(String name, String city, String address) throws CinephileException {
        String sql = "INSERT INTO theatres (name, city, address) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, city.trim());
            ps.setString(3, address.trim());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new CinephileException("Failed to add theatre: " + e.getMessage(), e);
        }
    }

    public List<Screen> getAllScreens() throws CinephileException {
        List<Screen> list = new ArrayList<>();
        String sql = "SELECT sc.screen_id, sc.theatre_id, t.name AS theatre_name, sc.name, sc.total_rows, sc.total_cols " +
                     "FROM screens sc JOIN theatres t ON sc.theatre_id = t.theatre_id ORDER BY sc.screen_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Screen(
                        rs.getLong("screen_id"),
                        rs.getLong("theatre_id"),
                        rs.getString("theatre_name"),
                        rs.getString("name"),
                        rs.getInt("total_rows"),
                        rs.getInt("total_cols")
                ));
            }
            return list;
        } catch (SQLException e) {
            throw new CinephileException("Failed to load screens: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a screen AND auto-generates its Regular/Premium/VIP seats inside a JDBC transaction.
     */
    public void addScreenWithSeats(long theatreId, String name, int rows, int cols,
                                   double regPrice, double premPrice, double vipPrice) throws CinephileException {
        String screenSql = "INSERT INTO screens (theatre_id, name, total_rows, total_cols) VALUES (?, ?, ?, ?)";
        String seatSql = "INSERT INTO seats (screen_id, row_num, col_num, seat_type, price) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long screenId;
                try (PreparedStatement ps = conn.prepareStatement(screenSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, theatreId);
                    ps.setString(2, name.trim());
                    ps.setInt(3, rows);
                    ps.setInt(4, cols);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Failed to obtain generated screen_id");
                        }
                        screenId = keys.getLong(1);
                    }
                }

                int premStart = (int) Math.ceil(rows * 0.4) + 1;
                int vipStart = (int) Math.ceil(rows * 0.8) + 1;

                try (PreparedStatement seatPs = conn.prepareStatement(seatSql)) {
                    for (int r = 1; r <= rows; r++) {
                        String type = (r >= vipStart) ? "VIP" : (r >= premStart ? "PREMIUM" : "REGULAR");
                        double price = (r >= vipStart) ? vipPrice : (r >= premStart ? premPrice : regPrice);
                        for (int c = 1; c <= cols; c++) {
                            seatPs.setLong(1, screenId);
                            seatPs.setInt(2, r);
                            seatPs.setInt(3, c);
                            seatPs.setString(4, type);
                            seatPs.setDouble(5, price);
                            seatPs.addBatch();
                        }
                    }
                    seatPs.executeBatch();
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new CinephileException("Failed to create screen & seats: " + e.getMessage(), e);
        }
    }

    public List<Showtime> getShowtimesByMovie(Long movieIdFilter) throws CinephileException {
        List<Showtime> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT sh.show_id, sh.movie_id, m.title AS movie_title, " +
                "sh.screen_id, sc.name AS screen_name, t.name AS theatre_name, t.city AS theatre_city, " +
                "sh.show_date, sh.show_time, sh.status, fn_available_seats(sh.show_id) AS avail_seats " +
                "FROM showtimes sh " +
                "JOIN movies m ON sh.movie_id = m.movie_id " +
                "JOIN screens sc ON sh.screen_id = sc.screen_id " +
                "JOIN theatres t ON sc.theatre_id = t.theatre_id ");
        if (movieIdFilter != null) {
            sql.append("WHERE sh.movie_id = ? ");
        }
        sql.append("ORDER BY sh.show_date ASC, sh.show_time ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (movieIdFilter != null) {
                ps.setLong(1, movieIdFilter);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Showtime(
                            rs.getLong("show_id"),
                            rs.getLong("movie_id"),
                            rs.getString("movie_title"),
                            rs.getLong("screen_id"),
                            rs.getString("screen_name"),
                            rs.getString("theatre_name"),
                            rs.getString("theatre_city"),
                            rs.getDate("show_date").toLocalDate(),
                            rs.getTime("show_time").toLocalTime(),
                            rs.getString("status"),
                            rs.getInt("avail_seats")
                    ));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new CinephileException("Failed to load showtimes: " + e.getMessage(), e);
        }
    }

    public void addShowtime(long movieId, long screenId, LocalDate showDate, LocalTime showTime)
            throws CinephileException {
        String sql = "INSERT INTO showtimes (movie_id, screen_id, show_date, show_time, status) " +
                     "VALUES (?, ?, ?, ?, 'UPCOMING')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, movieId);
            ps.setLong(2, screenId);
            ps.setDate(3, Date.valueOf(showDate));
            ps.setTime(4, Time.valueOf(showTime));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new CinephileException("Failed to schedule showtime: " + e.getMessage(), e);
        }
    }

    public List<Seat> getSeatsForShow(long showId) throws CinephileException {
        List<Seat> list = new ArrayList<>();
        String sql = "SELECT s.seat_id, s.screen_id, s.row_num, s.col_num, s.seat_type, s.price, " +
                     "CASE WHEN EXISTS (" +
                     "  SELECT 1 FROM booking_seats bs " +
                     "  JOIN bookings b ON bs.booking_id = b.booking_id " +
                     "  WHERE bs.seat_id = s.seat_id AND b.show_id = ? AND b.status = 'CONFIRMED'" +
                     ") THEN 1 ELSE 0 END AS is_booked " +
                     "FROM seats s " +
                     "JOIN showtimes sh ON s.screen_id = sh.screen_id " +
                     "WHERE sh.show_id = ? " +
                     "ORDER BY s.row_num ASC, s.col_num ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, showId);
            ps.setLong(2, showId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Seat(
                            rs.getLong("seat_id"),
                            rs.getLong("screen_id"),
                            rs.getInt("row_num"),
                            rs.getInt("col_num"),
                            rs.getString("seat_type"),
                            rs.getDouble("price"),
                            rs.getInt("is_booked") == 1
                    ));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new CinephileException("Failed to load seats: " + e.getMessage(), e);
        }
    }
}

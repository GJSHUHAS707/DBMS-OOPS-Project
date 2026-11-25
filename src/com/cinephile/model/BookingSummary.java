package com.cinephile.model;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;

public class BookingSummary {
    private long bookingId;
    private String bookingRef;
    private long customerId;
    private String customerName;
    private String customerEmail;
    private String movieTitle;
    private String theatreName;
    private String theatreCity;
    private String screenName;
    private LocalDate showDate;
    private LocalTime showTime;
    private double totalAmount;
    private String bookingStatus;
    private String paymentMethod;
    private String paymentStatus;
    private Timestamp bookedAt;

    public BookingSummary(long bookingId, String bookingRef, long customerId, String customerName,
                          String customerEmail, String movieTitle, String theatreName, String theatreCity,
                          String screenName, LocalDate showDate, LocalTime showTime,
                          double totalAmount, String bookingStatus, String paymentMethod,
                          String paymentStatus, Timestamp bookedAt) {
        this.bookingId = bookingId;
        this.bookingRef = bookingRef;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.movieTitle = movieTitle;
        this.theatreName = theatreName;
        this.theatreCity = theatreCity;
        this.screenName = screenName;
        this.showDate = showDate;
        this.showTime = showTime;
        this.totalAmount = totalAmount;
        this.bookingStatus = bookingStatus;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.bookedAt = bookedAt;
    }

    public long getBookingId() { return bookingId; }
    public String getBookingRef() { return bookingRef; }
    public long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public String getMovieTitle() { return movieTitle; }
    public String getTheatreName() { return theatreName; }
    public String getTheatreCity() { return theatreCity; }
    public String getScreenName() { return screenName; }
    public LocalDate getShowDate() { return showDate; }
    public LocalTime getShowTime() { return showTime; }
    public double getTotalAmount() { return totalAmount; }
    public String getBookingStatus() { return bookingStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public Timestamp getBookedAt() { return bookedAt; }
}

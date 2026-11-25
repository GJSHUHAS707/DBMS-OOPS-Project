package com.cinephile.model;

public class MovieReportRow {
    private long movieId;
    private String title;
    private String genre;
    private int confirmedBookings;
    private int ticketsSold;
    private double ticketRevenue;

    public MovieReportRow(long movieId, String title, String genre,
                          int confirmedBookings, int ticketsSold, double ticketRevenue) {
        this.movieId = movieId;
        this.title = title;
        this.genre = genre;
        this.confirmedBookings = confirmedBookings;
        this.ticketsSold = ticketsSold;
        this.ticketRevenue = ticketRevenue;
    }

    public long getMovieId() { return movieId; }
    public String getTitle() { return title; }
    public String getGenre() { return genre; }
    public int getConfirmedBookings() { return confirmedBookings; }
    public int getTicketsSold() { return ticketsSold; }
    public double getTicketRevenue() { return ticketRevenue; }
}

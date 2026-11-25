package com.cinephile.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Showtime {
    private long showId;
    private long movieId;
    private String movieTitle;
    private long screenId;
    private String screenName;
    private String theatreName;
    private String theatreCity;
    private LocalDate showDate;
    private LocalTime showTime;
    private String status;
    private int availableSeats;

    public Showtime(long showId, long movieId, String movieTitle, long screenId,
                    String screenName, String theatreName, String theatreCity,
                    LocalDate showDate, LocalTime showTime, String status, int availableSeats) {
        this.showId = showId;
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.screenId = screenId;
        this.screenName = screenName;
        this.theatreName = theatreName;
        this.theatreCity = theatreCity;
        this.showDate = showDate;
        this.showTime = showTime;
        this.status = status;
        this.availableSeats = availableSeats;
    }

    public long getShowId() { return showId; }
    public long getMovieId() { return movieId; }
    public String getMovieTitle() { return movieTitle; }
    public long getScreenId() { return screenId; }
    public String getScreenName() { return screenName; }
    public String getTheatreName() { return theatreName; }
    public String getTheatreCity() { return theatreCity; }
    public LocalDate getShowDate() { return showDate; }
    public LocalTime getShowTime() { return showTime; }
    public String getStatus() { return status; }
    public int getAvailableSeats() { return availableSeats; }
}

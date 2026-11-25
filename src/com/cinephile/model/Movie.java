package com.cinephile.model;

import java.time.LocalDate;

public class Movie {
    private long movieId;
    private String title;
    private String genre;
    private String language;
    private int durationMinutes;
    private String description;
    private LocalDate releaseDate;

    public Movie(long movieId, String title, String genre, String language,
                 int durationMinutes, String description, LocalDate releaseDate) {
        this.movieId = movieId;
        this.title = title;
        this.genre = genre;
        this.language = language;
        this.durationMinutes = durationMinutes;
        this.description = description;
        this.releaseDate = releaseDate;
    }

    public long getMovieId() { return movieId; }
    public String getTitle() { return title; }
    public String getGenre() { return genre; }
    public String getLanguage() { return language; }
    public int getDurationMinutes() { return durationMinutes; }
    public String getDescription() { return description; }
    public LocalDate getReleaseDate() { return releaseDate; }

    @Override
    public String toString() {
        return title + " (" + language + " | " + genre + ")";
    }
}

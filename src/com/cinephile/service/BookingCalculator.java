package com.cinephile.service;

import com.cinephile.model.Movie;
import com.cinephile.model.Seat;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Utility class demonstrating CS23333 Unit IV & V:
 * Collections (List, Set), Streams API, Method References, and Lambda Expressions.
 */
public class BookingCalculator {

    public static double calculateTotalAmount(Collection<Seat> selectedSeats) {
        return selectedSeats.stream()
                .mapToDouble(Seat::getPrice)
                .sum();
    }

    public static String formatSelectedSeatLabels(Collection<Seat> selectedSeats) {
        return selectedSeats.stream()
                .map(Seat::getLabel)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    public static List<Movie> filterMovies(List<Movie> movies, String searchText, String genre, String language) {
        String query = searchText == null ? "" : searchText.trim().toLowerCase();
        return movies.stream()
                .filter(m -> query.isEmpty() || m.getTitle().toLowerCase().contains(query))
                .filter(m -> genre == null || "All Genres".equals(genre) || m.getGenre().equalsIgnoreCase(genre))
                .filter(m -> language == null || "All Languages".equals(language) || m.getLanguage().equalsIgnoreCase(language))
                .collect(Collectors.toList());
    }

    public static Set<String> extractGenres(List<Movie> movies) {
        Set<String> genres = new TreeSet<>();
        movies.forEach(m -> genres.add(m.getGenre()));
        return genres;
    }

    public static Set<String> extractLanguages(List<Movie> movies) {
        Set<String> langs = new TreeSet<>();
        movies.forEach(m -> langs.add(m.getLanguage()));
        return langs;
    }
}

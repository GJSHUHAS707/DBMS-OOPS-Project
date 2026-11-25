package com.cinephile.web;

import com.cinephile.model.*;

import java.util.*;

/**
 * Simple, readable JSON serialization and parsing helper using pure Core Java.
 * Avoids heavy external JSON libraries so all code remains easy to study and explain.
 */
public class JsonHelper {

    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    public static String q(String s) {
        return "\"" + esc(s) + "\"";
    }

    /**
     * Parses a flat JSON object like {"email":"a@b.com","showId":1,"seatIds":[1,2]}
     * into a string key -> raw string value map.
     */
    public static Map<String, String> parseObject(String body) {
        Map<String, String> map = new LinkedHashMap<>();
        if (body == null) return map;
        String trimmed = body.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        if (trimmed.isEmpty()) return map;

        int i = 0;
        int n = trimmed.length();
        while (i < n) {
            while (i < n && (trimmed.charAt(i) == ',' || Character.isWhitespace(trimmed.charAt(i)))) i++;
            if (i >= n) break;

            // Key
            if (trimmed.charAt(i) != '"') break;
            int keyEnd = findClosingQuote(trimmed, i + 1);
            String key = unescape(trimmed.substring(i + 1, keyEnd));
            i = keyEnd + 1;

            while (i < n && trimmed.charAt(i) != ':') i++;
            i++; // skip ':'
            while (i < n && Character.isWhitespace(trimmed.charAt(i))) i++;

            // Value
            String val;
            char ch = trimmed.charAt(i);
            if (ch == '"') {
                int valEnd = findClosingQuote(trimmed, i + 1);
                val = unescape(trimmed.substring(i + 1, valEnd));
                i = valEnd + 1;
            } else if (ch == '[') {
                int arrEnd = trimmed.indexOf(']', i);
                val = trimmed.substring(i, arrEnd + 1);
                i = arrEnd + 1;
            } else {
                int valEnd = i;
                while (valEnd < n && trimmed.charAt(valEnd) != ',') valEnd++;
                val = trimmed.substring(i, valEnd).trim();
                i = valEnd;
            }
            map.put(key, val);
        }
        return map;
    }

    public static List<Long> parseLongArray(String rawArray) {
        List<Long> list = new ArrayList<>();
        if (rawArray == null) return list;
        String inner = rawArray.trim();
        if (inner.startsWith("[")) inner = inner.substring(1);
        if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);
        for (String part : inner.split(",")) {
            String p = part.trim();
            if (!p.isEmpty()) {
                list.add(Long.parseLong(p));
            }
        }
        return list;
    }

    private static int findClosingQuote(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '"' && s.charAt(i - 1) != '\\') {
                return i;
            }
        }
        return s.length() - 1;
    }

    private static String unescape(String s) {
        return s.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    public static String userJson(UserAccount u, String token) {
        return "{"
                + "\"token\":" + q(token) + ","
                + "\"id\":" + u.getId() + ","
                + "\"name\":" + q(u.getName()) + ","
                + "\"email\":" + q(u.getEmail()) + ","
                + "\"role\":" + q(u.getRoleName()) + ","
                + "\"permissions\":" + q(u.getPermissionsSummary())
                + "}";
    }

    public static String moviesJson(List<Movie> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Movie m = list.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"movieId\":").append(m.getMovieId()).append(",")
              .append("\"title\":").append(q(m.getTitle())).append(",")
              .append("\"genre\":").append(q(m.getGenre())).append(",")
              .append("\"language\":").append(q(m.getLanguage())).append(",")
              .append("\"durationMinutes\":").append(m.getDurationMinutes()).append(",")
              .append("\"description\":").append(q(m.getDescription())).append(",")
              .append("\"releaseDate\":").append(q(m.getReleaseDate().toString()))
              .append("}");
        }
        return sb.append("]").toString();
    }

    public static String showsJson(List<Showtime> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Showtime s = list.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"showId\":").append(s.getShowId()).append(",")
              .append("\"movieId\":").append(s.getMovieId()).append(",")
              .append("\"movieTitle\":").append(q(s.getMovieTitle())).append(",")
              .append("\"screenId\":").append(s.getScreenId()).append(",")
              .append("\"screenName\":").append(q(s.getScreenName())).append(",")
              .append("\"theatreName\":").append(q(s.getTheatreName())).append(",")
              .append("\"theatreCity\":").append(q(s.getTheatreCity())).append(",")
              .append("\"showDate\":").append(q(s.getShowDate().toString())).append(",")
              .append("\"showTime\":").append(q(s.getShowTime().toString())).append(",")
              .append("\"status\":").append(q(s.getStatus())).append(",")
              .append("\"availableSeats\":").append(s.getAvailableSeats())
              .append("}");
        }
        return sb.append("]").toString();
    }

    public static String seatsJson(List<Seat> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Seat s = list.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"seatId\":").append(s.getSeatId()).append(",")
              .append("\"screenId\":").append(s.getScreenId()).append(",")
              .append("\"rowNum\":").append(s.getRowNum()).append(",")
              .append("\"colNum\":").append(s.getColNum()).append(",")
              .append("\"label\":").append(q(s.getLabel())).append(",")
              .append("\"seatType\":").append(q(s.getSeatType())).append(",")
              .append("\"price\":").append(s.getPrice()).append(",")
              .append("\"booked\":").append(s.isBooked())
              .append("}");
        }
        return sb.append("]").toString();
    }

    public static String bookingJson(BookingSummary b, List<String> seatLabels) {
        StringBuilder seatsArr = new StringBuilder("[");
        if (seatLabels != null) {
            for (int i = 0; i < seatLabels.size(); i++) {
                if (i > 0) seatsArr.append(",");
                seatsArr.append(q(seatLabels.get(i)));
            }
        }
        seatsArr.append("]");
        return "{"
                + "\"bookingId\":" + b.getBookingId() + ","
                + "\"bookingRef\":" + q(b.getBookingRef()) + ","
                + "\"customerName\":" + q(b.getCustomerName()) + ","
                + "\"customerEmail\":" + q(b.getCustomerEmail()) + ","
                + "\"movieTitle\":" + q(b.getMovieTitle()) + ","
                + "\"theatreName\":" + q(b.getTheatreName()) + ","
                + "\"theatreCity\":" + q(b.getTheatreCity()) + ","
                + "\"screenName\":" + q(b.getScreenName()) + ","
                + "\"showDate\":" + q(b.getShowDate().toString()) + ","
                + "\"showTime\":" + q(b.getShowTime().toString()) + ","
                + "\"totalAmount\":" + b.getTotalAmount() + ","
                + "\"bookingStatus\":" + q(b.getBookingStatus()) + ","
                + "\"paymentMethod\":" + q(b.getPaymentMethod()) + ","
                + "\"paymentStatus\":" + q(b.getPaymentStatus()) + ","
                + "\"bookedAt\":" + q(String.valueOf(b.getBookedAt())) + ","
                + "\"seats\":" + seatsArr
                + "}";
    }

    public static String theatresJson(List<Theatre> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Theatre t = list.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"theatreId\":").append(t.getTheatreId()).append(",")
              .append("\"name\":").append(q(t.getName())).append(",")
              .append("\"city\":").append(q(t.getCity())).append(",")
              .append("\"address\":").append(q(t.getAddress()))
              .append("}");
        }
        return sb.append("]").toString();
    }

    public static String screensJson(List<Screen> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Screen s = list.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"screenId\":").append(s.getScreenId()).append(",")
              .append("\"theatreId\":").append(s.getTheatreId()).append(",")
              .append("\"theatreName\":").append(q(s.getTheatreName())).append(",")
              .append("\"name\":").append(q(s.getName())).append(",")
              .append("\"totalRows\":").append(s.getTotalRows()).append(",")
              .append("\"totalCols\":").append(s.getTotalCols())
              .append("}");
        }
        return sb.append("]").toString();
    }

    public static String reportsJson(List<MovieReportRow> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            MovieReportRow r = list.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"movieId\":").append(r.getMovieId()).append(",")
              .append("\"title\":").append(q(r.getTitle())).append(",")
              .append("\"genre\":").append(q(r.getGenre())).append(",")
              .append("\"confirmedBookings\":").append(r.getConfirmedBookings()).append(",")
              .append("\"ticketsSold\":").append(r.getTicketsSold()).append(",")
              .append("\"ticketRevenue\":").append(r.getTicketRevenue())
              .append("}");
        }
        return sb.append("]").toString();
    }

    public static String auditLogsJson(List<AuditLogEntry> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            AuditLogEntry a = list.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"auditId\":").append(a.getAuditId()).append(",")
              .append("\"bookingId\":").append(a.getBookingId()).append(",")
              .append("\"actionType\":").append(q(a.getActionType())).append(",")
              .append("\"oldStatus\":").append(q(a.getOldStatus() == null ? "-" : a.getOldStatus())).append(",")
              .append("\"newStatus\":").append(q(a.getNewStatus())).append(",")
              .append("\"details\":").append(q(a.getDetails())).append(",")
              .append("\"loggedAt\":").append(q(String.valueOf(a.getLoggedAt())))
              .append("}");
        }
        return sb.append("]").toString();
    }
}

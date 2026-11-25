package com.cinephile.service;

import com.cinephile.model.BookingSummary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Saves E-Ticket passes to local text files.
 * Demonstrates CS23333 Unit IV: Character I/O Streams (FileWriter & BufferedWriter).
 */
public class TicketFileService {

    public static File exportTicket(BookingSummary booking, List<String> seatLabels) throws IOException {
        File dir = new File("tickets");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, "Ticket_" + booking.getBookingRef() + ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("====================================================");
            writer.newLine();
            writer.write("           CINEPHILE - DIGITAL E-TICKET             ");
            writer.newLine();
            writer.write("====================================================");
            writer.newLine();
            writer.write("Booking Ref    : " + booking.getBookingRef());
            writer.newLine();
            writer.write("Status         : " + booking.getBookingStatus());
            writer.newLine();
            writer.write("Customer       : " + booking.getCustomerName() + " (" + booking.getCustomerEmail() + ")");
            writer.newLine();
            writer.write("Movie          : " + booking.getMovieTitle());
            writer.newLine();
            writer.write("Theatre        : " + booking.getTheatreName() + ", " + booking.getTheatreCity());
            writer.newLine();
            writer.write("Screen         : " + booking.getScreenName());
            writer.newLine();
            writer.write("Date & Time    : " + booking.getShowDate() + " at " + booking.getShowTime());
            writer.newLine();
            writer.write("Seats          : " + String.join(", ", seatLabels));
            writer.newLine();
            writer.write("Payment        : " + booking.getPaymentMethod() + " (" + booking.getPaymentStatus() + ")");
            writer.newLine();
            writer.write("Total Amount   : Rs." + String.format("%.2f", booking.getTotalAmount()));
            writer.newLine();
            writer.write("====================================================");
            writer.newLine();
        }
        return file;
    }
}

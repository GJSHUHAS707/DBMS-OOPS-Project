package com.cinephile.model;

public class Seat {
    private long seatId;
    private long screenId;
    private int rowNum;
    private int colNum;
    private String seatType;
    private double price;
    private boolean booked;

    public Seat(long seatId, long screenId, int rowNum, int colNum,
                String seatType, double price, boolean booked) {
        this.seatId = seatId;
        this.screenId = screenId;
        this.rowNum = rowNum;
        this.colNum = colNum;
        this.seatType = seatType;
        this.price = price;
        this.booked = booked;
    }

    public long getSeatId() { return seatId; }
    public long getScreenId() { return screenId; }
    public int getRowNum() { return rowNum; }
    public int getColNum() { return colNum; }
    public String getSeatType() { return seatType; }
    public double getPrice() { return price; }
    public boolean isBooked() { return booked; }

    public String getLabel() {
        char rowChar = (char) ('A' + rowNum - 1);
        return rowChar + String.valueOf(colNum);
    }

    @Override
    public String toString() {
        return getLabel() + " (" + seatType + " - Rs." + String.format("%.2f", price) + ")";
    }
}

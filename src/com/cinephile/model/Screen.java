package com.cinephile.model;

public class Screen {
    private long screenId;
    private long theatreId;
    private String theatreName;
    private String name;
    private int totalRows;
    private int totalCols;

    public Screen(long screenId, long theatreId, String theatreName, String name, int totalRows, int totalCols) {
        this.screenId = screenId;
        this.theatreId = theatreId;
        this.theatreName = theatreName;
        this.name = name;
        this.totalRows = totalRows;
        this.totalCols = totalCols;
    }

    public long getScreenId() { return screenId; }
    public long getTheatreId() { return theatreId; }
    public String getTheatreName() { return theatreName; }
    public String getName() { return name; }
    public int getTotalRows() { return totalRows; }
    public int getTotalCols() { return totalCols; }

    @Override
    public String toString() {
        return theatreName + " - " + name + " (" + (totalRows * totalCols) + " seats)";
    }
}

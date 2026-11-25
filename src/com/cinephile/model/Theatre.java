package com.cinephile.model;

public class Theatre {
    private long theatreId;
    private String name;
    private String city;
    private String address;

    public Theatre(long theatreId, String name, String city, String address) {
        this.theatreId = theatreId;
        this.name = name;
        this.city = city;
        this.address = address;
    }

    public long getTheatreId() { return theatreId; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public String getAddress() { return address; }

    @Override
    public String toString() {
        return name + " (" + city + ")";
    }
}

package com.altf4.figuremortis;

public class DisplayItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_DEATH = 1;

    private int type;
    private String year;
    private Death death;

    public DisplayItem(int type, String year) {
        this.type = type;
        this.year = year;
    }

    public DisplayItem(int type, Death death) {
        this.type = type;
        this.death = death;
    }

    public int getType() {
        return type;
    }

    public String getYear() {
        return year;
    }

    public Death getDeath() {
        return death;
    }
}
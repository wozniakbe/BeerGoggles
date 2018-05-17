package edu.msoe.wozniakbe.beergoggles.src;

/**
 * Created by ben on 5/9/18.
 */

public class Beer {

    private String name;
    private int ibu;
    private int abv;

    public Beer(){} // Default required for firebase

    public Beer(String name, int ibu, int abv) {
        this.name = name;
        this.ibu = ibu;
        this.abv = abv;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
/*
    public int getIbu() {
        return ibu;
    }

    public void setIbu(int ibu) {
        this.ibu = ibu;
    }

    public double getAbv() {
        return abv;
    }

    public void setAbv(double abv) {
        this.abv = abv;
    }*/
}

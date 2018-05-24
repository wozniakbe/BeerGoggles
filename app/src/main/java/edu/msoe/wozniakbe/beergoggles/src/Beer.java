package edu.msoe.wozniakbe.beergoggles.src;

/**
 * Author: Ben Wozniak (wozniakbe@msoe.edu)
 * This class represents a type of beer.
 * Information includes name, international bitterness unit, and alcohol by volume.
 */

public class Beer {

    private String name;
    private String ibu;
    private String abv;

    public Beer(){} // Default required for firebase

    public Beer(String name, String ibu, String abv) {
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

    public String getIbu() {
        return ibu;
    }

    public void setIbu(String ibu) {
        this.ibu = ibu;
    }

    public String getAbv() {
        return abv;
    }

    public void setAbv(String abv) {
        this.abv = abv;
    }
}
